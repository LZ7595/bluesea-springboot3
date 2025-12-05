package com.example.backend.Service;

import com.alibaba.fastjson.JSONObject;
import com.example.backend.Dao.MessageMapper;
import com.example.backend.Dao.UserMapper;
import com.example.backend.Model.Dto.PageResult;
import com.example.backend.Model.Entity.User;
import com.example.backend.Model.WebSocket.Message;
import com.example.backend.Model.WebSocket.MessageForm;
import com.example.backend.Model.WebSocket.WebSocket;
import com.example.backend.Utils.AssertUtils;
import com.example.backend.Utils.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageService {

    @Autowired
    private WebSocketUtil webSocketUtil;
    // 限制聊天记录数量
    final Integer limitMessagesLength = 6000;

    @Value("${image.storage.directory}")
    private String imageStorageDirectory;

    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;

    // 发送信息的逻辑
    public void sendMessage(Message message) throws Exception {
        AssertUtils.isError(message.getSend_user() == null, "发送用户不能为空!");
        AssertUtils.isError(message.getReceive_user() == null, "接收用户不能为空!");
        AssertUtils.isError(StringUtils.isEmpty(message.getContent()), "发送信息不能为空!");

        User sendUser = userMapper.selectbyUserId(message.getSend_user());
        AssertUtils.isError(sendUser == null, "发送用户不存在,发送信息失败!");
        AssertUtils.isError(!sendUser.isStatus(),
                "发送用户:" + message.getSend_user() + "状态已冻结,无法发送信息!");

        User receiveUser = userMapper.selectbyUserId(message.getReceive_user());
        AssertUtils.isError(receiveUser == null, "接收用户不存在,发送信息失败!");
        AssertUtils.isError(!receiveUser.isStatus(),
                "接收用户:" + message.getReceive_user() + "状态已冻结,无法接收信息!");

        message.setHandle(UUID.randomUUID().toString());
        message.setIs_read("0");
        message.setCreate_time(LocalDateTime.now());

        // 设置消息类型，如果前端未传递，默认设为 text
        if (message.getType() == null) {
            message.setType("text");
        }

        // 处理图片消息（超过5MB自动压缩至5MB以内，保留原格式）
        if ("image".equals(message.getType())) {
            String imageBase64 = message.getContent();
            // 1. 校验Base64非空
            if (StringUtils.isEmpty(imageBase64)) {
                throw new Exception("图片Base64数据为空!");
            }

            // -------------------------- 新增：提取原图片格式 --------------------------
            String originalFormat = "jpg"; // 默认格式（防止提取失败）
            if (imageBase64.startsWith("data:image")) {
                // 从前缀中提取格式（如 "data:image/png;base64," → 提取 "png"）
                int slashIndex = imageBase64.indexOf('/');
                int semicolonIndex = imageBase64.indexOf(';', slashIndex);
                if (slashIndex != -1 && semicolonIndex != -1) {
                    originalFormat = imageBase64.substring(slashIndex + 1, semicolonIndex);
                    // 校验合法格式（仅支持常见图片格式）
                    Set<String> validFormats = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "bmp"));
                    if (!validFormats.contains(originalFormat.toLowerCase())) {
                        throw new Exception("不支持的图片格式：" + originalFormat + "，仅支持jpg/png/gif/bmp");
                    }
                    // 统一jpeg为jpg（格式兼容）
                    if ("jpeg".equalsIgnoreCase(originalFormat)) {
                        originalFormat = "jpg";
                    }
                } else {
                    throw new Exception("图片Base64格式错误，无法提取图片类型（正确格式：data:image/xxx;base64,...）");
                }
            } else {
                log.warn("图片Base64无格式前缀，默认按JPG处理");
            }

            // 2. 截取Base64数据（仅去除前缀）
            int commaIndex = imageBase64.indexOf(',');
            if (commaIndex == -1) {
                throw new Exception("图片Base64格式错误，无分隔符!");
            }
            imageBase64 = imageBase64.substring(commaIndex + 1);

            try {
                // 3. 解码Base64为字节数组
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64.getBytes(StandardCharsets.UTF_8));

                // 4. 检查大小，超过5MB则压缩（传入原格式）
                final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
                if (imageBytes.length > MAX_SIZE) {
                    imageBytes = compressImage(imageBytes, MAX_SIZE, originalFormat); // 修复：按原格式压缩
                    if (imageBytes == null) {
                        throw new Exception("图片压缩失败，无法将图片处理至5MB以内!");
                    }
                }

                // 5. 校验是否为有效图片（压缩后再次确认）
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                    BufferedImage image = ImageIO.read(bais);
                    if (image == null) {
                        throw new Exception("无效的图片格式，无法识别!");
                    }
                }

                // 6. 按原格式保存（不再强制转为JPG）
                String fileName = UUID.randomUUID().toString() + "." + originalFormat; // 文件名带原格式后缀
                Path storagePath = Paths.get(imageStorageDirectory + "/chats");
                if (!Files.exists(storagePath)) {
                    Files.createDirectories(storagePath);
                }
                File imageFile = new File(storagePath.toFile(), fileName);

                // 7. 写入文件（按原格式写入）
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                     FileOutputStream fos = new FileOutputStream(imageFile)) {
                    BufferedImage image = ImageIO.read(bais);
                    // 按原格式写入（如png/gif，避免格式转换损坏）
                    if (!ImageIO.write(image, originalFormat, fos)) {
                        throw new Exception("图片写入失败，不支持的格式：" + originalFormat);
                    }
                }

                // 8. 存储相对路径（包含原格式后缀）
                String relativePath = "/chats/" + fileName;
                message.setContent(relativePath);

            } catch (IllegalArgumentException e) {
                throw new Exception("图片Base64解码失败，数据非法!");
            } catch (IOException e) {
                throw new Exception("图片处理/保存失败: " + e.getMessage(), e);
            }
        }

        System.out.println(message);

        // 插入消息到数据库
        messageMapper.insert(message);

        // 通过 WebSocket 发送消息给接收方
        webSocketUtil.sendMessageTo(com.alibaba.fastjson.JSONObject.toJSONString(message), message.getReceive_user());
    }

    // 获取两个人的聊天记录
    public List<Message> findMessageBySendUserAndReceiveUser(int sendUserId, int receiveUserId) throws Exception {
        AssertUtils.isError(sendUserId == 0, "发送用户为空!");
        AssertUtils.isError(receiveUserId == 0, "接收用户为空!");

        User sendUser = userMapper.selectbyUserId(sendUserId);
        AssertUtils.isError(sendUser == null, "发送用户不存在,发送信息失败!");

        User receiveUser = userMapper.selectbyUserId(receiveUserId);
        AssertUtils.isError(receiveUser == null, "接收用户不存在,发送信息失败!");

        // 获取对方发送的信息
        List<Message> receiveMessageList = messageMapper.selectBySendUserAndReceiveUserLimitLength(
                sendUserId, receiveUserId, limitMessagesLength);
        // 获取发送给对方的信息
        List<Message> sendMessageList = messageMapper.selectBySendUserAndReceiveUserLimitLength(
                receiveUserId, sendUserId, limitMessagesLength);

        List<Message> allMessageList = new ArrayList<>();
        allMessageList.addAll(receiveMessageList);
        allMessageList.addAll(sendMessageList);

        List<Message> sortedMessageList = allMessageList.stream()
                .sorted(Comparator.comparing(Message::getCreate_time))
                .collect(Collectors.toList());

        // 设置已读
        List<Message> noReadMessageList = receiveMessageList.stream()
                .filter(o -> "0".equals(o.getIs_read()))
                .peek(message -> message.setIs_read("1"))
                .collect(Collectors.toList());
        if (!noReadMessageList.isEmpty()) {
            for (Message message : noReadMessageList) {
                messageMapper.update(message.getHandle());
            }
        }
        return sortedMessageList;
    }

    // 获取所有数据
    public List<MessageForm> findAllMessageForm(int userId) throws Exception {
        AssertUtils.isError(userId == 0, "用户编号不能为空!");
        User loginUser = userMapper.selectbyUserId(userId);
        AssertUtils.isError(loginUser == null, "用户编号:" + userId + "不存在!");

        Map<Integer, WebSocket> users = webSocketUtil.getUsers();
        Set<Integer> ids = users.keySet();

        List<MessageForm> messageFormList = new ArrayList<>();
        messageFormList.addAll(findAllMessageChatDataWithLoginUserId(userId));

        // 判断ids是否在messageFormList的sendUser的Id中，不是则获取新的数据到messageFormList
        for (Integer id : ids) {
            if (!messageFormList.stream()
                    .map(o -> o.getSend_user().getId())
                    .map(String::valueOf)
                    .collect(Collectors.toList())
                    .contains(id)) {
                MessageForm messageForm = new MessageForm();
                User sendUserData = userMapper.selectbyUserId(id);
                if (sendUserData == null) {
                    continue;
                }
                List<Message> allMessageList = findBothMessages(userId, id,
                        limitMessagesLength, messageMapper);
                messageForm.setMessages(allMessageList);
                messageForm.setSend_user(sendUserData);
                messageForm.setReceive_user(loginUser);
                messageForm.setIs_online(true);
                messageForm.setNo_read_message_length(0);
                messageForm.setLast_message("");
                messageFormList.add(messageForm);
            }
        }

        // 获取所有messageFormList的sendUser的userId
        List<Integer> sendUserIds = messageFormList.stream()
                .map(MessageForm::getSend_user)
                .map(User::getId)
                .collect(Collectors.toList());

        // 按照在线状态为true，有聊天记录的优先展示
        messageFormList.sort((o1, o2) -> {
            if (o1.getIs_online() && o2.getIs_online()) {
                return o2.getMessages().size() - o1.getMessages().size();
            } else if (o1.getIs_online()) {
                return -1;
            } else if (o2.getIs_online()) {
                return 1;
            } else {
                return o2.getMessages().size() - o1.getMessages().size();
            }
        });
        return messageFormList;
    }

    // 获取登录用户所有聊过天的记录数据
    // 获取登录用户所有聊过天的记录数据（修改后）
    public List<MessageForm> findAllMessageChatDataWithLoginUserId(int userId) throws Exception {
        AssertUtils.isError(userId == 0, "用户编号不能为空!");
        User loginUser = userMapper.selectbyUserId(userId);
        AssertUtils.isError(loginUser == null, "用户编号:" + userId + "不存在!");

        Map<Integer, WebSocket> onlineUsers = webSocketUtil.getUsers();
        Set<Integer> onlineUserIds = onlineUsers.keySet();

        // 核心：用Map存储“唯一会话Key→MessageForm”，自动去重
        Map<String, MessageForm> sessionMap = new ConcurrentHashMap<>();

        // -------------------------- 步骤1：处理“接收过消息的用户”（别人发给登录用户） --------------------------
        List<Integer> sendUserIds = messageMapper.selectByReceiveUser(userId).stream()
                .map(Message::getSend_user)
                .distinct()
                .collect(Collectors.toList());

        for (Integer sendUserId : sendUserIds) {
            User sendUser = userMapper.selectbyUserId(sendUserId);
            if (sendUser == null) {
                continue;
            }

            // 生成唯一会话Key（登录用户 ↔ 发送消息用户）
            String sessionKey = getUniqueSessionKey(userId, sendUserId);

            // 1.1 查双向消息（登录用户→发送用户 + 发送用户→登录用户）
            List<Message> sendToUserMessages = messageMapper.selectBySendUserAndReceiveUserLimitLength(
                    userId, sendUserId, limitMessagesLength);
            List<Message> receiveFromUserMessages = messageMapper.selectBySendUserAndReceiveUserLimitLength(
                    sendUserId, userId, limitMessagesLength);

            // 1.2 合并消息并按时间排序
            List<Message> allMessages = new ArrayList<>();
            allMessages.addAll(sendToUserMessages);
            allMessages.addAll(receiveFromUserMessages);
            allMessages = allMessages.stream()
                    .sorted(Comparator.comparing(Message::getCreate_time))
                    .collect(Collectors.toList());

            // 1.3 构造会话对象
            MessageForm session = new MessageForm();
            session.setSend_user(sendUser); // 对方用户（非登录用户）
            session.setReceive_user(loginUser); // 登录用户
            session.setMessages(allMessages);
            session.setIs_online(onlineUserIds.contains(sendUserId)); // 对方是否在线
            // 未读消息数：仅统计“对方发给登录用户且未读”的消息
            session.setNo_read_message_length((int) receiveFromUserMessages.stream()
                    .filter(msg -> "0".equals(msg.getIs_read()))
                    .count());
            // 最后一条消息（区分文本/图片）
            if (!allMessages.isEmpty()) {
                Message lastMsg = allMessages.get(allMessages.size() - 1);
                session.setLast_message("image".equals(lastMsg.getType()) ? "[图片]" : lastMsg.getContent());
            } else {
                session.setLast_message("");
            }

            // 1.4 放入Map（重复Key会自动覆盖，保留最新会话）
            sessionMap.put(sessionKey, session);
        }

        // -------------------------- 步骤2：处理“仅登录用户发送过消息，未收到回复”的用户 --------------------------
        List<Integer> receiveUserIds = messageMapper.selectBySendUser(userId).stream()
                .map(Message::getReceive_user)
                .distinct()
                .collect(Collectors.toList());

        for (Integer receiveUserId : receiveUserIds) {
            // 生成唯一会话Key（登录用户 ↔ 接收消息用户）
            String sessionKey = getUniqueSessionKey(userId, receiveUserId);

            // 跳过已存在的会话（避免重复添加）
            if (sessionMap.containsKey(sessionKey)) {
                continue;
            }

            User receiveUser = userMapper.selectbyUserId(receiveUserId);
            if (receiveUser == null) {
                continue;
            }

            // 2.1 查登录用户发给对方的消息（无回复，仅单向消息）
            List<Message> sendMessages = messageMapper.selectBySendUserAndReceiveUserLimitLength(
                    userId, receiveUserId, limitMessagesLength);
            sendMessages = sendMessages.stream()
                    .sorted(Comparator.comparing(Message::getCreate_time))
                    .collect(Collectors.toList());

            // 2.2 构造会话对象
            MessageForm session = new MessageForm();
            session.setSend_user(receiveUser); // 对方用户（非登录用户）
            session.setReceive_user(loginUser); // 登录用户
            session.setMessages(sendMessages);
            session.setIs_online(onlineUserIds.contains(receiveUserId));
            session.setNo_read_message_length(0); // 无接收消息，未读为0
            // 最后一条消息（仅登录用户发送的消息）
            session.setLast_message(sendMessages.isEmpty() ? "" :
                    ("image".equals(sendMessages.get(sendMessages.size() - 1).getType())
                            ? "[图片]" : sendMessages.get(sendMessages.size() - 1).getContent()));

            // 2.3 放入Map（无重复，新增会话）
            sessionMap.put(sessionKey, session);
        }

        // -------------------------- 步骤3：过滤无效会话 + 转换为List --------------------------
        return sessionMap.values().stream()
                // 过滤无效会话：消息为空且最后一条消息为空
                .filter(session -> !session.getMessages().isEmpty() || StringUtils.isNotEmpty(session.getLast_message()))
                // 排序：在线会话优先 → 消息数多的优先 → 最后一条消息新的优先
                .sorted((s1, s2) -> {
                    // 在线状态：在线在前
                    if (s1.getIs_online() != s2.getIs_online()) {
                        return s1.getIs_online() ? -1 : 1;
                    }
                    // 消息数：多的在前
                    if (s1.getMessages().size() != s2.getMessages().size()) {
                        return Integer.compare(s2.getMessages().size(), s1.getMessages().size());
                    }
                    // 最后一条消息时间：新的在前（无消息则按空字符串排序）
                    if (!s1.getMessages().isEmpty() && !s2.getMessages().isEmpty()) {
                        LocalDateTime time1 = s1.getMessages().get(s1.getMessages().size() - 1).getCreate_time();
                        LocalDateTime time2 = s2.getMessages().get(s2.getMessages().size() - 1).getCreate_time();
                        return time2.compareTo(time1);
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    // 用户区查到的数据，有用户名就查用户名对应用户数据和聊天记录（修改后）
    public List<MessageForm> searchUserForForm(int userId, String username) throws Exception {
        AssertUtils.isError(userId == 0, "登录用户不能为空!");
        User loginUser = userMapper.selectbyUserId(userId);
        AssertUtils.isError(loginUser == null, "登录用户不存在!");

        // 核心：用Map去重（搜索结果可能有重复用户）
        Map<String, MessageForm> sessionMap = new ConcurrentHashMap<>();

        if (StringUtils.isNotEmpty(username)) {
            List<User> userList = userMapper.selectByUserName(username);
            Map<Integer, WebSocket> onlineUsers = webSocketUtil.getUsers();

            for (User targetUser : userList) {
                if (targetUser == null) {
                    continue;
                }

                String sessionKey = getUniqueSessionKey(userId, targetUser.getId());
                // 跳过已存在的会话（避免同一用户多次出现）
                if (sessionMap.containsKey(sessionKey)) {
                    continue;
                }

                // 查双向消息
                List<Message> sendToTarget = messageMapper.selectBySendUserAndReceiveUserLimitLength(
                        userId, targetUser.getId(), limitMessagesLength);
                List<Message> receiveFromTarget = messageMapper.selectBySendUserAndReceiveUserLimitLength(
                        targetUser.getId(), userId, limitMessagesLength);

                List<Message> allMessages = new ArrayList<>();
                allMessages.addAll(sendToTarget);
                allMessages.addAll(receiveFromTarget);
                allMessages.sort(Comparator.comparing(Message::getCreate_time));

                // 构造会话
                MessageForm session = new MessageForm();
                session.setSend_user(targetUser);
                session.setReceive_user(loginUser);
                session.setIs_online(onlineUsers.containsKey(targetUser.getId()));
                session.setMessages(allMessages);
                // 未读消息数：仅对方发给登录用户的未读消息
                session.setNo_read_message_length((int) receiveFromTarget.stream()
                        .filter(msg -> "0".equals(msg.getIs_read()))
                        .count());
                // 最后一条消息
                if (!allMessages.isEmpty()) {
                    Message lastMsg = allMessages.get(allMessages.size() - 1);
                    session.setLast_message("image".equals(lastMsg.getType()) ? "[图片]" : lastMsg.getContent());
                } else {
                    session.setLast_message("");
                }

                sessionMap.put(sessionKey, session);
            }
        } else {
            // 无用户名时，直接调用已去重的findAllMessageForm
            List<MessageForm> allSessions = findAllMessageForm(userId);
            for (MessageForm session : allSessions) {
                String sessionKey = getUniqueSessionKey(userId, session.getSend_user().getId());
                sessionMap.put(sessionKey, session);
            }
        }

        // 转换为List并返回（无需再次排序，findAllMessageForm已排序）
        return new ArrayList<>(sessionMap.values());
    }

    private List<Message> findBothMessages(int sendUserId, int receiveUserId,
                                           Integer limitMessageLength,
                                           MessageMapper messageMapper) {
        List<Message> receiveMessageList = messageMapper.selectBySendUserAndReceiveUserLimitLength(
                receiveUserId, sendUserId, limitMessageLength);
        List<Message> sendMessageList = messageMapper.selectBySendUserAndReceiveUserLimitLength(
                sendUserId, receiveUserId, limitMessageLength);

        List<Message> allMessageList = new ArrayList<>();
        allMessageList.addAll(receiveMessageList);
        allMessageList.addAll(sendMessageList);

        return allMessageList.stream()
                .sorted(Comparator.comparing(Message::getCreate_time))
                .collect(Collectors.toList());
    }
    // -------------------------- 新增：图片压缩工具方法 --------------------------
    /**
     * 按原图片格式压缩（修复：不强制转JPG）
     * @param imageBytes 原图字节数组
     * @param maxSize 最大允许大小（字节）
     * @param originalFormat 原图片格式（如 "png"、"gif"、"jpg"）
     * @return 压缩后的字节数组（null表示压缩失败）
     */
    private byte[] compressImage(byte[] imageBytes, long maxSize, String originalFormat) {
        try {
            // 1. 读取原图（原有逻辑保留）
            BufferedImage originalImage;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                originalImage = ImageIO.read(bais);
                if (originalImage == null) {
                    return null;
                }
            }

            // 2. 初始压缩参数（原有逻辑保留）
            float quality = 0.8f;
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            byte[] compressedBytes = null;

            // 3. 循环压缩（修复：按原格式压缩）
            while (true) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    // 3.1 调整尺寸（原有逻辑保留）
                    BufferedImage scaledImage = resizeImage(originalImage, width, height);

                    // 3.2 按原格式压缩（核心修复：不强制转JPG）
                    // 注意：GIF格式不支持质量压缩，仅支持尺寸压缩；PNG为无损压缩，质量参数无效
                    if ("gif".equalsIgnoreCase(originalFormat)) {
                        // GIF格式：仅尺寸压缩，无质量压缩
                        ImageIO.write(scaledImage, originalFormat, baos);
                    } else if ("png".equalsIgnoreCase(originalFormat)) {
                        // PNG格式：无损压缩，设置压缩级别（0-9，9为最高压缩率）
                        ImageWriteParam param = ImageIO.getImageWritersByFormatName(originalFormat).next().getDefaultWriteParam();
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(quality); // PNG质量参数仅影响压缩率，不影响画质
                        ImageIO.write(scaledImage, originalFormat, baos);
                    } else {
                        // JPG/BMP格式：按质量压缩（原有逻辑保留）
                        ImageWriteParam param = ImageIO.getImageWritersByFormatName(originalFormat).next().getDefaultWriteParam();
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(quality);
                        ImageIO.write(scaledImage, originalFormat, baos);
                    }

                    compressedBytes = baos.toByteArray();
                }

                // 3.3 检查大小（原有逻辑保留）
                if (compressedBytes.length <= maxSize) {
                    break;
                }

                // 3.4 调整压缩参数（原有逻辑保留）
                if (quality > 0.3f) {
                    quality -= 0.1f;
                } else {
                    width = (int) (width * 0.9);
                    height = (int) (height * 0.9);
                    if (width < 100 || height < 100) {
                        return null;
                    }
                }
            }

            return compressedBytes;
        } catch (IOException e) {
            log.error("图片压缩失败（格式：{}）", originalFormat, e);
            return null;
        }
    }
    /**
     * 调整图片尺寸
     * @param originalImage 原图
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     * @return 调整后的图片
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        // 创建目标尺寸的缓冲图片
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        // 绘制缩放后的图片（使用平滑缩放算法）
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return resizedImage;
    }
    // -------------------------- 新增：用户发送消息给客服 --------------------------
    /**
     * 用户发送消息给客服（Operator角色）
     * @param userId 普通用户ID（必须是Role.User）
     * @param content 消息内容（文本或图片Base64）
     * @param type 消息类型（text/image）
     * @param targetServiceId 可选：指定客服ID（必须是Role.Operator）
     */
    public void sendToService(Integer userId, String content, String type, Integer targetServiceId) throws Exception {
        // 1. 校验发送用户合法性
        User user = userMapper.selectbyUserId(userId);
        AssertUtils.isError(user == null, "用户不存在！");
        AssertUtils.isError(!User.Role.User.equals(user.getRole()), "仅普通用户可发起客服对话！");
        AssertUtils.isError(!user.isStatus(), "用户已冻结，无法发送消息！");

        // 2. 确定客服ID（指定或自动分配）
        Integer serviceId;
        if (targetServiceId != null) {
            // 校验指定客服是否为Operator且在线
            User service = userMapper.selectbyUserId(targetServiceId);
            log.info(webSocketUtil.getUsers().toString());
            AssertUtils.isError(service == null, "指定客服不存在！");
            AssertUtils.isError(!User.Role.Operator.equals(service.getRole()), "指定用户不是客服！");
            AssertUtils.isError(!webSocketUtil.getUsers().containsKey(targetServiceId), "指定客服不在线！");
            serviceId = targetServiceId;
        } else {
            // 自动分配在线客服
            serviceId = webSocketUtil.assignRandomService();
            AssertUtils.isError(serviceId == null, "当前无在线客服，请稍后再试！");
        }

        // 3. 构造消息
        Message message = new Message();
        message.setSend_user(userId);
        message.setReceive_user(serviceId);
        message.setContent(content);
        message.setType(StringUtils.isEmpty(type) ? "text" : type);
        message.setHandle(UUID.randomUUID().toString());
        message.setIs_read("0");
        message.setCreate_time(LocalDateTime.now());

        // 4. 处理图片消息（修复：保留原格式，不强制转JPG）
        if ("image".equals(type)) {
            String imageBase64 = content;
            if (StringUtils.isEmpty(imageBase64)) {
                throw new Exception("图片数据为空！");
            }

            // -------------------------- 新增：提取原图片格式 --------------------------
            String originalFormat = "jpg"; // 默认格式（防止提取失败）
            // 从Base64前缀提取格式（如 "data:image/png;base64," → 提取 "png"）
            if (imageBase64.startsWith("data:image")) {
                // 1. 截取 "data:image/xxx;" 部分（如 "image/png"）
                int slashIndex = imageBase64.indexOf('/');
                int semicolonIndex = imageBase64.indexOf(';', slashIndex);
                if (slashIndex != -1 && semicolonIndex != -1) {
                    originalFormat = imageBase64.substring(slashIndex + 1, semicolonIndex);
                    // 2. 校验合法格式（仅支持常见图片格式，避免恶意格式）
                    Set<String> validFormats = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "bmp"));
                    if (!validFormats.contains(originalFormat.toLowerCase())) {
                        throw new Exception("不支持的图片格式：" + originalFormat + "，仅支持jpg/png/gif/bmp");
                    }
                    // 统一格式名（如 "jpeg" 统一为 "jpg"，避免后缀不一致）
                    if ("jpeg".equalsIgnoreCase(originalFormat)) {
                        originalFormat = "jpg";
                    }
                } else {
                    throw new Exception("图片Base64格式错误，无法提取图片类型（格式应为 data:image/xxx;base64,...）");
                }
            } else {
                // 无Base64前缀时，默认按JPG处理（兼容前端未传前缀的场景）
                log.warn("图片Base64无格式前缀，默认按JPG处理");
                originalFormat = "jpg";
            }

            // -------------------------- 截取Base64数据（原有逻辑保留） --------------------------
            int commaIndex = imageBase64.indexOf(',');
            if (commaIndex == -1) {
                throw new Exception("图片Base64格式错误，无分隔符（格式应为 data:image/xxx;base64,...）");
            }
            imageBase64 = imageBase64.substring(commaIndex + 1);

            try {
                // -------------------------- Base64解码与压缩（原有逻辑保留） --------------------------
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64.getBytes(StandardCharsets.UTF_8));
                final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
                if (imageBytes.length > MAX_SIZE) {
                    imageBytes = compressImage(imageBytes, MAX_SIZE, originalFormat); // 修复：传入原格式，按原格式压缩
                    if (imageBytes == null) {
                        throw new Exception("图片压缩失败！");
                    }
                }

                // -------------------------- 校验有效图片（原有逻辑保留） --------------------------
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                    BufferedImage image = ImageIO.read(bais);
                    if (image == null) {
                        throw new Exception("无效图片格式！可能是损坏的图片或非图片数据");
                    }
                }

                // -------------------------- 按原格式保存图片（核心修复：后缀与原格式一致） --------------------------
                // 文件名：UUID + 原格式后缀（如 "a1b2c3d4-1234-5678-90ab-cdef12345678.png"）
                String fileName = UUID.randomUUID().toString() + "." + originalFormat;
                Path storagePath = Paths.get(imageStorageDirectory + "/chats");

                // 确保目录存在（原有逻辑保留）
                if (!Files.exists(storagePath)) {
                    Files.createDirectories(storagePath);
                }
                File imageFile = new File(storagePath.toFile(), fileName);

                // -------------------------- 按原格式写入文件（修复：不强制转JPG） --------------------------
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                     FileOutputStream fos = new FileOutputStream(imageFile)) {
                    BufferedImage image = ImageIO.read(bais);
                    // 按原格式写入（如 originalFormat为"png"，则写为PNG格式）
                    if (!ImageIO.write(image, originalFormat, fos)) {
                        throw new Exception("图片写入失败，不支持的格式：" + originalFormat);
                    }
                }

                // -------------------------- 存储相对路径（后缀与原格式一致） --------------------------
                message.setContent("/chats/" + fileName); // 如 "/chats/a1b2c3d4-1234-5678-90ab-cdef12345678.png"

            } catch (IllegalArgumentException e) {
                throw new Exception("图片Base64解码失败，数据非法！");
            } catch (Exception e) {
                throw new Exception("图片处理失败：" + e.getMessage());
            }
        }

        // 5. 持久化并推送
        messageMapper.insert(message);
        webSocketUtil.sendMessageTo(JSONObject.toJSONString(message), serviceId);
        log.info("用户{}发送消息给客服{}: {}", userId, serviceId, content);
    }

    /**
     * 分页获取两人的聊天记录
     * @param sendUserId 发送方用户ID
     * @param receiveUserId 接收方用户ID
     * @param currentPage 当前页码（从1开始）
     * @param pageSize 每页记录数
     * @return 分页结果（含数据列表、总记录数、总页数等）
     */
    public PageResult<Message> findMessageByPage(int sendUserId, int receiveUserId, int currentPage, int pageSize) throws Exception {
        // 1. 基础参数校验
        AssertUtils.isError(sendUserId == 0, "发送用户不能为空!");
        AssertUtils.isError(receiveUserId == 0, "接收用户不能为空!");
        AssertUtils.isError(currentPage < 1, "当前页码必须≥1!");
        AssertUtils.isError(pageSize < 1 || pageSize > 200, "每页记录数必须在1-200之间!"); // 限制最大页大小，避免性能问题

        // 2. 校验用户合法性
        User sendUser = userMapper.selectbyUserId(sendUserId);
        AssertUtils.isError(sendUser == null, "发送用户不存在!");
        User receiveUser = userMapper.selectbyUserId(receiveUserId);
        AssertUtils.isError(receiveUser == null, "接收用户不存在!");

        // 3. 计算分页偏移量（SQL LIMIT 偏移量, 条数）
        int offset = (currentPage - 1) * pageSize;

        // 4. 分页查询消息列表（核心：调用Mapper的分页查询方法）
        List<Message> messageList = messageMapper.selectBySendReceivePage(
                sendUserId, receiveUserId, offset, pageSize);
        // 按时间排序（升序：旧消息在前，新消息在后）
        messageList.sort(Comparator.comparing(Message::getCreate_time));

        List<Message> unreadMessages = messageList.stream()
                .filter(msg -> receiveUserId == msg.getReceive_user()  // 确保是“当前用户接收”的消息
                        && "0".equals(msg.getIs_read()))              // 未读状态
                .collect(Collectors.toList());

        if (!unreadMessages.isEmpty()) {
            for (Message msg : unreadMessages) {
                messageMapper.update(msg.getHandle()); // 调用原有更新已读状态的方法
            }
        }

        // 5. 查询总记录数（用于计算总页数）
        int total = messageMapper.countBySendReceive(sendUserId, receiveUserId);

        // 6. 组装分页结果（自动计算总页数：向上取整）
        int totalPages = (total + pageSize - 1) / pageSize;
        return new PageResult<>(
                messageList,    // 当前页数据列表
                total,          // 总记录数
                currentPage,    // 当前页码
                pageSize,       // 每页记录数
                totalPages      // 总页数
        );
    }

    /**
     * 生成两个用户间的唯一会话标识（A↔B 与 B↔A 生成相同Key）
     * @param userId1 用户1ID
     * @param userId2 用户2ID
     * @return 唯一会话Key（格式：minId_maxId）
     */
    private String getUniqueSessionKey(Integer userId1, Integer userId2) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        // 取较小ID在前，较大ID在后，确保Key唯一
        int minId = Math.min(userId1, userId2);
        int maxId = Math.max(userId1, userId2);
        return minId + "_" + maxId;
    }
}
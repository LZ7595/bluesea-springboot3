package com.example.backend.Service;

import com.example.backend.Dao.MessageDao;
import com.example.backend.Dao.UserDao;
import com.example.backend.Entity.User;
import com.example.backend.Entity.WebSocket.Message;
import com.example.backend.Entity.WebSocket.MessageForm;
import com.example.backend.Entity.WebSocket.WebSocket;
import com.example.backend.Utils.AssertUtils;
import com.example.backend.Utils.WebSocketUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private WebSocketUtil webSocketUtil;
    // 限制聊天记录数量
    final Integer limitMessagesLength = 6000;
    // 限制用户数量
    final Integer limitUserLength = 300;

    @Value("${image.storage.directory}")
    private String imageStorageDirectory;

    @Autowired
    private MessageDao messageDao;
    @Autowired
    private UserDao userDao;

    // 获取未读的接收信息
    public Integer findNoReadMessageLength(Integer userId) throws Exception {
        AssertUtils.isError(userId == null, "用户编号不能为空!");
        User user = userDao.selectbyUserId(userId);
        AssertUtils.isError(user == null, "用户编号:" + userId + "不存在!");

        // 为防止发送人特别多导致信息未获取，这里多设置一些拿信息数据
        List<Message> messages = messageDao.selectByReceiveUserLimitLength(userId, limitMessagesLength);
        Map<Integer, List<Message>> messageBySendUserMap = messages.stream()
                .collect(Collectors.groupingBy(Message::getSend_user));

        int total = 0;
        for (Integer sendUser : messageBySendUserMap.keySet()) {
            List<Message> receiveMessageList = messageBySendUserMap.get(sendUser);
            int noReadSize = (int) receiveMessageList.stream()
                    .filter(o -> "0".equals(o.getIs_read()))
                    .count();
            total += noReadSize;
        }
        return total;
    }

    // 发送信息的逻辑
    public void sendMessage(Message message) throws Exception {
        AssertUtils.isError(message.getSend_user() == null, "发送用户不能为空!");
        AssertUtils.isError(message.getReceive_user() == null, "接收用户不能为空!");
        AssertUtils.isError(StringUtils.isEmpty(message.getContent()), "发送信息不能为空!");

        User sendUser = userDao.selectbyUserId(message.getSend_user());
        AssertUtils.isError(sendUser == null, "发送用户不存在,发送信息失败!");
        AssertUtils.isError(!sendUser.isStatus(),
                "发送用户:" + message.getSend_user() + "状态已冻结,无法发送信息!");

        User receiveUser = userDao.selectbyUserId(message.getReceive_user());
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

        // 处理图片消息
        if ("image".equals(message.getType())) {
            String imageBase64 = message.getContent();
            // 去除 Base64 编码的前缀
            if (imageBase64.startsWith("data:image")) {
                int commaIndex = imageBase64.indexOf(',');
                if (commaIndex != -1) {
                    imageBase64 = imageBase64.substring(commaIndex + 1);
                }
            }
            try {
                // 生成唯一的文件名
                String fileName = UUID.randomUUID().toString() + ".jpg";
                Path storagePath = Paths.get(imageStorageDirectory);
                if (!Files.exists(storagePath)) {
                    Files.createDirectories(storagePath);
                }
                File imageFile = new File(storagePath.toFile(), fileName);
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    byte[] imageBytes = Base64.getDecoder().decode(imageBase64.getBytes(StandardCharsets.UTF_8));
                    fos.write(imageBytes);
                }

                // 提取相对路径
                String relativePath = "/chats/" + fileName;
                // 将图片相对存储路径保存到消息内容中
                message.setContent(relativePath);

            } catch (IOException e) {
                throw new Exception("图片保存失败: " + e.getMessage(), e);
            }
        }

        System.out.println(message);

        // 插入消息到数据库
        messageDao.insert(message);

        // 通过 WebSocket 发送消息给接收方
        webSocketUtil.sendMessageTo(com.alibaba.fastjson.JSONObject.toJSONString(message), message.getReceive_user());
    }

    // 获取两个人的聊天记录
    public List<Message> findMessageBySendUserAndReceiveUser(int sendUserId, int receiveUserId) throws Exception {
        AssertUtils.isError(sendUserId == 0, "发送用户为空!");
        AssertUtils.isError(receiveUserId == 0, "接收用户为空!");

        User sendUser = userDao.selectbyUserId(sendUserId);
        AssertUtils.isError(sendUser == null, "发送用户不存在,发送信息失败!");

        User receiveUser = userDao.selectbyUserId(receiveUserId);
        AssertUtils.isError(receiveUser == null, "接收用户不存在,发送信息失败!");

        // 获取对方发送的信息
        List<Message> receiveMessageList = messageDao.selectBySendUserAndReceiveUserLimitLength(
                sendUserId, receiveUserId, limitMessagesLength);
        // 获取发送给对方的信息
        List<Message> sendMessageList = messageDao.selectBySendUserAndReceiveUserLimitLength(
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
                messageDao.update(message.getHandle());
            }
        }
        return sortedMessageList;
    }

    // 获取所有数据
    public List<MessageForm> findAllMessageForm(int userId) throws Exception {
        AssertUtils.isError(userId == 0, "用户编号不能为空!");
        User loginUser = userDao.selectbyUserId(userId);
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
                User sendUserData = userDao.selectbyUserId(id);
                if (sendUserData == null) {
                    continue;
                }
                List<Message> allMessageList = findBothMessages(userId, id,
                        limitMessagesLength, messageDao);
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

//        // 如果获取到的用户少于100个，则补齐剩余数量的用户数据,这里补齐的是从没和自己聊天过的用户
//        if (sendUserIds.size() < limitUserLength) {
//            int leaveCount = limitUserLength - sendUserIds.size();
//            List<User> userList = userDao.selectByWhere("id", sendUserIds.toArray(), leaveCount);
//            if (userList != null && !userList.isEmpty()) {
//                for (User user : userList) {
//                    MessageForm messageForm = new MessageForm();
//                    messageForm.setSend_user(user);
//                    messageForm.setReceive_user(loginUser);
//                    messageForm.setIs_online(false);
//                    messageForm.setNo_read_message_length(0);
//                    messageForm.setLast_message("");
//                    messageFormList.add(messageForm);
//                }
//            }
//        }

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
    public List<MessageForm> findAllMessageChatDataWithLoginUserId(int userId) throws Exception {
        AssertUtils.isError(userId == 0, "用户编号不能为空!");
        User loginUser = userDao.selectbyUserId(userId);
        AssertUtils.isError(loginUser == null, "用户编号:" + userId + "不存在!");

        Map<Integer, WebSocket> users = webSocketUtil.getUsers();
        Set<Integer> ids = users.keySet();

        List<MessageForm> messageFormList = new ArrayList<>();

        // 获取所有有发送信息给自己聊天的用户
        List<Integer> allSendUsers = messageDao.selectByReceiveUser(userId).stream()
                .map(Message::getSend_user)
                .distinct()
                .collect(Collectors.toList());

        for (Integer sendUser : allSendUsers) {
            // 发送人的用户信息
            MessageForm messageForm = new MessageForm();
            User sendUserData = userDao.selectbyUserId(sendUser);
            if (sendUserData == null) {
                continue;
            }
            // 获取对方发送的信息
            List<Message> receiveMessageList = messageDao.selectBySendUserAndReceiveUserLimitLength(
                    sendUser, userId, limitMessagesLength);
            // 获取发送给对方的信息
            List<Message> sendMessageList = messageDao.selectBySendUserAndReceiveUserLimitLength(
                    userId, sendUser, limitMessagesLength);

            List<Message> allMessageList = new ArrayList<>();
            allMessageList.addAll(receiveMessageList);
            allMessageList.addAll(sendMessageList);

            List<Message> sortedMessageList = allMessageList.stream()
                    .sorted(Comparator.comparing(Message::getCreate_time))
                    .collect(Collectors.toList());

            // 赋值最新消息
            messageForm.setLast_message(sortedMessageList.isEmpty() ? "" : sortedMessageList.get(sortedMessageList.size() - 1).getContent());
            // 赋值聊天记录
            messageForm.setMessages(sortedMessageList);
            // 赋值未读消息
            messageForm.setNo_read_message_length((int) receiveMessageList.stream()
                    .filter(o -> "0".equals(o.getIs_read()))
                    .count());
            // 赋值发送人
            messageForm.setSend_user(sendUserData);
            // 赋值接收人
            messageForm.setReceive_user(loginUser);
            // 赋值是否在线
            messageForm.setIs_online(ids.contains(sendUser));
            messageFormList.add(messageForm);
        }

        // 获取只有自己发送信息给别人的记录的用户
        List<Integer> allSendToUsers = messageDao.selectBySendUser(userId).stream()
                .map(Message::getReceive_user)
                .distinct()
                .collect(Collectors.toList());

        for (Integer receiveUser : allSendToUsers) {
            // 判断messageFormList的sendUser的userId是否包含receiveUser，有则说明已经存在了
            if (messageFormList.stream()
                    .anyMatch(o -> o.getSend_user().getId().toString().equals(receiveUser))) {
                continue;
            }
            MessageForm messageForm = new MessageForm();
            User receiveUserData = userDao.selectbyUserId(receiveUser);
            if (receiveUserData == null) {
                continue;
            }
            messageForm.setReceive_user(loginUser);
            messageForm.setSend_user(receiveUserData);
            messageForm.setLast_message("");
            messageForm.setNo_read_message_length(0);
            List<Message> sendMessageList = messageDao.selectBySendUserAndReceiveUserLimitLength(userId,
                    receiveUser, limitMessagesLength);
            // 按照CreateTime排序从小到大
            messageForm.setMessages(sendMessageList.stream()
                    .sorted(Comparator.comparing(Message::getCreate_time))
                    .collect(Collectors.toList()));
            messageForm.setIs_online(ids.contains(receiveUser));
            messageFormList.add(messageForm);
        }
        return messageFormList;
    }

    // 用户区查到的数据，有用户名就查用户名对应用户数据和聊天记录
    public List<MessageForm> searchUserForForm(int userId, String username) throws Exception {
        AssertUtils.isError(userId == 0, "登录用户不能为空!");
        User loginUser = userDao.selectbyUserId(userId);
        AssertUtils.isError(loginUser == null, "登录用户不存在!");

        List<MessageForm> messageFormList = new ArrayList<>();
        if (StringUtils.isNotEmpty(username)) {
            List<User> userList = userDao.selectByUserName(username);
            Map<Integer, WebSocket> users = webSocketUtil.getUsers();
            Set<Integer> ids = users.keySet();

            for (User user : userList) {
                MessageForm messageForm = new MessageForm();
                messageForm.setSend_user(user);
                messageForm.setReceive_user(loginUser);
                messageForm.setIs_online(ids.contains(user.getId()));

                // 获取对方发送的信息
                List<Message> receiveMessageList = messageDao.selectBySendUserAndReceiveUserLimitLength(
                        user.getId(), loginUser.getId(), limitMessagesLength);
                // 获取发送给对方的信息
                List<Message> sendMessageList = messageDao.selectBySendUserAndReceiveUserLimitLength(
                        loginUser.getId(), user.getId(), limitMessagesLength);

                List<Message> allMessages = new ArrayList<>();
                allMessages.addAll(receiveMessageList);
                allMessages.addAll(sendMessageList);

                allMessages.sort(Comparator.comparing(Message::getCreate_time));

                // 设置最新消息内容，根据消息类型处理显示
                if (!allMessages.isEmpty()) {
                    Message lastMessage = allMessages.get(allMessages.size() - 1);
                    if ("image".equals(lastMessage.getType())) {
                        messageForm.setLast_message("[图片]");
                    } else {
                        messageForm.setLast_message(lastMessage.getContent());
                    }
                } else {
                    messageForm.setLast_message("");
                }

                messageForm.setMessages(allMessages);
                // 获取allMessages中isRead为0的数量
                messageForm.setNo_read_message_length((int) allMessages.stream()
                        .filter(message -> "0".equals(message.getIs_read()))
                        .count());
                messageFormList.add(messageForm);
            }
        } else {
            messageFormList.addAll(findAllMessageForm(userId));
        }
        return messageFormList;
    }

    private List<Message> findBothMessages(int sendUserId, int receiveUserId,
                                           Integer limitMessageLength,
                                           MessageDao messageDao) {
        List<Message> receiveMessageList = messageDao.selectBySendUserAndReceiveUserLimitLength(
                receiveUserId, sendUserId, limitMessageLength);
        List<Message> sendMessageList = messageDao.selectBySendUserAndReceiveUserLimitLength(
                sendUserId, receiveUserId, limitMessageLength);

        List<Message> allMessageList = new ArrayList<>();
        allMessageList.addAll(receiveMessageList);
        allMessageList.addAll(sendMessageList);

        return allMessageList.stream()
                .sorted(Comparator.comparing(Message::getCreate_time))
                .collect(Collectors.toList());
    }
}
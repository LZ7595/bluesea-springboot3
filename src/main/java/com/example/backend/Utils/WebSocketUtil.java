package com.example.backend.Utils;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.example.backend.Dao.UserMapper;
import com.example.backend.Model.Entity.User;
import com.example.backend.Model.WebSocket.WebSocket;
import com.example.backend.Model.WebSocket.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket工具类（单用户单连接+心跳检测）
 */
@Component
@ServerEndpoint(value = "/webSocket/{userId}")
@Slf4j
public class WebSocketUtil {

    /**
     * 核心：静态线程安全集合（所有WebSocket实例共享，确保单用户单连接）
     */
    private static final AtomicInteger loginCount = new AtomicInteger(0); // 在线人数（原子计数）
    private static final ConcurrentHashMap<Integer, WebSocket> userMap = new ConcurrentHashMap<>(); // userId -> WebSocket
    private static final ConcurrentHashMap<Session, Long> lastHeartbeatTime = new ConcurrentHashMap<>(); // 会话最后心跳时间
    private static final ConcurrentHashMap<Session, Integer> sessionToUserId = new ConcurrentHashMap<>(); // 会话->用户ID映射
    private static final ConcurrentHashMap<Integer, Boolean> userRoleCache = new ConcurrentHashMap<>(); // 用户角色缓存（客服/普通用户）

    /**
     * 心跳检测线程池（守护线程，避免阻塞应用关闭）
     */
    private static final ScheduledExecutorService HEARTBEAT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("websocket-heartbeat-thread");
        return thread;
    });

    /**
     * 超时配置（区分角色）
     */
    private static final long NORMAL_USER_TIMEOUT = 30 * 1000; // 普通用户超时30秒
    private static final long SERVICE_USER_TIMEOUT = 5 * 60 * 1000; // 客服超时5分钟

    /**
     * 静态注入UserMapper（解决WebSocket实例化与Spring注入冲突）
     */
    private static UserMapper userMapper;
    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketUtil.userMapper = userMapper;
    }

    /**
     * 显式无参构造函数（WebSocket容器必须）
     */
    public WebSocketUtil() {
    }

    /**
     * 静态初始化：启动心跳检测（仅执行一次）
     */
    static {
        HEARTBEAT_EXECUTOR.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            // 遍历副本避免ConcurrentModificationException
            for (Map.Entry<Session, Long> entry : new ArrayList<>(lastHeartbeatTime.entrySet())) {
                Session session = entry.getKey();
                Long lastTime = entry.getValue();
                Integer userId = sessionToUserId.get(session);

                // 1. 会话已关闭：清理资源
                if (!session.isOpen()) {
                    cleanSession(session, userId);
                    continue;
                }

                // 2. 无用户ID：无效连接，强制清理
                if (userId == null) {
                    log.warn("会话[{}]无对应用户ID，强制清理", session.getId());
                    cleanSession(session, userId);
                    continue;
                }

                // 3. 获取用户角色（缓存优先）
                Boolean isService = userRoleCache.get(userId);
                if (isService == null) {
                    isService = checkUserIsServiceStatic(userId);
                    userRoleCache.put(userId, isService);
                }

                // 4. 计算超时阈值
                long timeout = isService ? SERVICE_USER_TIMEOUT : NORMAL_USER_TIMEOUT;

                // 5. 超时清理
                if (currentTime - lastTime > timeout) {
                    log.info("清理超时会话：用户[{}]，会话[{}]，超时时间={}秒（阈值={}秒）",
                            userId, session.getId(), (currentTime - lastTime) / 1000, timeout / 1000);
                    cleanSession(session, userId);
                }
            }
        }, 0, 10, TimeUnit.SECONDS); // 每10秒检测一次
    }


    /**
     * 收到消息触发事件（处理业务消息和心跳消息）
     */
    @OnMessage
    public void onMessage(String messageInfo, Session session) {
        if (StringUtils.isBlank(messageInfo)) {
            return;
        }

        // 1. 处理心跳消息（前端约定为"heartbeat"）
        String trimMsg = messageInfo.trim();
        if ("heartbeat".equals(trimMsg)) {
            lastHeartbeatTime.put(session, System.currentTimeMillis());
            log.debug("收到用户[{}]心跳包，会话[{}]", sessionToUserId.get(session), session.getId());
            return;
        }

        // 2. 处理业务消息
        Integer senderUserId = sessionToUserId.get(session);
        if (senderUserId == null) {
            log.error("会话[{}]无对应用户ID，丢弃消息：{}", session.getId(), messageInfo);
            return;
        }

        log.info("收到用户[{}]业务消息：{}", senderUserId, messageInfo);
        try {
            JSONObject jsonObject = JSONObject.parseObject(messageInfo);
            Integer receiverUserId = jsonObject.getInteger("receive_user");
            if (receiverUserId == null) {
                log.error("消息缺少接收方用户ID，用户[{}]消息：{}", senderUserId, messageInfo);
                return;
            }

            // 发送消息给接收方（检查会话有效性）
            WebSocket receiverWebSocket = userMap.get(receiverUserId);
            log.info(String.valueOf(receiverWebSocket.getSession().isOpen()));
            if (receiverWebSocket != null && receiverWebSocket.getSession().isOpen()) {
                receiverWebSocket.getSession().getAsyncRemote().sendText(messageInfo);
                log.info("消息已发送给用户[{}]，会话[{}]", receiverUserId, receiverWebSocket.getSession().getId());
            } else {
                log.info("用户[{}]不在线或会话已关闭，消息暂存：{}", receiverUserId, messageInfo);
                // TODO: 此处可添加消息持久化逻辑（如存入数据库）
            }
        } catch (Exception e) {
            log.error("用户[{}]消息解析失败：{}", senderUserId, e.getMessage(), e);
        }
    }

    /**
     * 打开连接触发事件（初始化会话+缓存角色，确保1用户1连接）
     */
    @OnOpen
    public void onOpen(@PathParam("userId") Integer userId, Session session) {
        log.info("用户[{}]发起WebSocket连接，会话[{}]", userId, session.getId());

        // 1. 基础校验
        if (userId == null) {
            log.error("连接失败：用户ID为空，会话[{}]", session.getId());
            closeSession(session, 1007, "用户ID为空"); // 1007=无效负载
            return;
        }
        if (!session.isOpen()) {
            log.error("用户[{}]会话未打开，连接失败", userId);
            return;
        }

        // 2. 处理重复连接（关闭旧连接，保留新连接）
        WebSocket newWebSocket = new WebSocket();
        newWebSocket.setUserId(userId);
        newWebSocket.setSession(session);

        // 原子操作：尝试放入新连接，若已存在则返回旧连接
        WebSocket oldWebSocket = userMap.putIfAbsent(userId, newWebSocket);
        if (oldWebSocket != null) {
            // 关闭旧连接
            if (oldWebSocket.getSession().isOpen()) {
                log.warn("用户[{}]已存在连接，关闭旧会话[{}]，保留新会话[{}]",
                        userId, oldWebSocket.getSession().getId(), session.getId());
                closeSession(oldWebSocket.getSession(), 1008, "重复连接，关闭旧会话"); // 1008=政策违规
                // 清理旧会话资源（关键：确保计数准确）
                cleanSession(oldWebSocket.getSession(), userId);
            } else {
                log.info("用户[{}]旧会话已关闭，清理无效映射", userId);
                userMap.remove(userId);
                // 重新放入新连接
                userMap.put(userId, newWebSocket);
                loginCount.incrementAndGet(); // 新连接计数+1
            }
        } else {
            // 新连接，计数+1
            loginCount.incrementAndGet();
        }

        // 3. 初始化会话映射
        lastHeartbeatTime.put(session, System.currentTimeMillis());
        sessionToUserId.put(session, userId);

        // 4. 缓存用户角色（避免后续心跳检测查库）
        boolean isService = checkUserIsService(userId);
        userRoleCache.put(userId, isService);
        log.info("用户[{}]角色：{}（{}秒超时）",
                userId, isService ? "客服" : "普通用户",
                isService ? SERVICE_USER_TIMEOUT / 1000 : NORMAL_USER_TIMEOUT / 1000);

        // 5. 连接成功日志
        log.info("用户[{}]连接成功，会话[{}]，当前在线人数：{}",
                userId, session.getId(), loginCount.get());

        // 6. 广播连接消息（可选）
        broadcastOnlineMsg(userId, true);
    }

    /**
     * 关闭连接触发事件（清理会话+角色缓存）
     */
    @OnClose
    public void onClose(@PathParam("userId") Integer userId, Session session, CloseReason closeReason) {
        cleanSession(session, userId);
        log.info("用户[{}]断开连接，会话[{}]，原因：{}，当前在线人数：{}",
                userId, session.getId(), closeReason.getReasonPhrase(), loginCount.get());

        // 广播断开消息（可选）
        broadcastOnlineMsg(userId, false);
    }

    /**
     * 传输消息错误触发事件（主动清理异常连接）
     */
    @OnError
    public void onError(Session session, Throwable error) {
        Integer userId = sessionToUserId.get(session);
        log.error("用户[{}]WebSocket错误，会话[{}]：{}",
                userId, session.getId(), error.getMessage(), error);

        // 错误时主动清理连接
        closeSession(session, 1011, "传输错误"); // 1011=服务器错误
        cleanSession(session, userId);
    }

    /**
     * 发送消息给指定用户
     */
    public void sendMessageTo(String message, Integer userId) {
        if (userId == null || StringUtils.isBlank(message)) {
            log.error("发送消息失败：用户ID为空或消息为空");
            return;
        }

        WebSocket receiverWebSocket = userMap.get(userId);
//        if (receiverWebSocket == null) {
//            log.info("用户[{}]不在线，无法发送消息", userId);
//            return;
//        }

        Session session = receiverWebSocket.getSession();
        if (session.isOpen()) {
            try {
                session.getAsyncRemote().sendText(message);
                log.info("用户[{}]收到消息，会话[{}]：{}", userId, session.getId(), message);
            } catch (Exception e) {
                log.error("发送消息给用户[{}]失败，会话[{}]", userId, session.getId(), e);
                cleanSession(session, userId); // 发送失败，清理无效会话
            }
        } else {
            log.info("用户[{}]会话[{}]已关闭，移除无效连接", userId, session.getId());
            cleanSession(session, userId);
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public void sendMessageAll(String message) {
        if (StringUtils.isBlank(message)) {
            log.error("广播消息失败：消息为空");
            return;
        }

        // 遍历副本避免ConcurrentModificationException
        for (WebSocket webSocket : new ArrayList<>(userMap.values())) {
            Integer userId = webSocket.getUserId();
            Session session = webSocket.getSession();

            if (session.isOpen()) {
                try {
                    session.getAsyncRemote().sendText(message);
                    log.debug("广播消息给用户[{}]，会话[{}]", userId, session.getId());
                } catch (Exception e) {
                    log.error("广播消息给用户[{}]失败，会话[{}]", userId, session.getId(), e);
                    cleanSession(session, userId);
                }
            } else {
                log.info("用户[{}]会话[{}]已关闭，移除无效连接", userId, session.getId());
                cleanSession(session, userId);
            }
        }
    }

    /**
     * 获取在线用户映射（返回副本避免外部修改）
     */
    public Map<Integer, WebSocket> getUsers() {
        return new ConcurrentHashMap<>(userMap);
    }

    /**
     * 应用关闭时释放资源（需在Spring Bean销毁时调用）
     */
    public void destroy() {
        HEARTBEAT_EXECUTOR.shutdown();
        // 清理所有会话
        for (Session session : new ArrayList<>(lastHeartbeatTime.keySet())) {
            cleanSession(session, sessionToUserId.get(session));
        }
        log.info("WebSocket资源已释放，心跳线程池关闭，剩余在线人数：{}", loginCount.get());
    }

    /**
     * 获取所有在线客服（筛选角色为Operator的用户）
     */
    public List<Integer> getOnlineServiceIds() {
        List<Integer> onlineServiceIds = new ArrayList<>();
        // 遍历用户映射，筛选客服角色
        for (Map.Entry<Integer, WebSocket> entry : userMap.entrySet()) {
            Integer userId = entry.getKey();
            WebSocket webSocket = entry.getValue();

            // 会话有效且角色为客服
            if (webSocket.getSession().isOpen()) {
                Boolean isService = userRoleCache.get(userId);
                if (isService == null) {
                    isService = checkUserIsService(userId);
                    userRoleCache.put(userId, isService);
                }
                if (isService) {
                    onlineServiceIds.add(userId);
                }
            } else {
                log.info("客服[{}]会话已关闭，移除无效连接", userId);
                cleanSession(webSocket.getSession(), userId);
            }
        }
        log.info("当前在线客服数量：{}，IDs：{}", onlineServiceIds.size(), onlineServiceIds);
        return onlineServiceIds;
    }

    /**
     * 随机分配在线客服
     */
    public Integer assignRandomService() {
        List<Integer> onlineServices = getOnlineServiceIds();
        if (onlineServices.isEmpty()) {
            log.warn("当前无在线客服（Operator）");
            return null;
        }
        // 随机选择一个客服
        Integer assignedServiceId = onlineServices.get(new Random().nextInt(onlineServices.size()));
        log.info("随机分配客服：{}，在线客服列表：{}", assignedServiceId, onlineServices);
        return assignedServiceId;
    }

    // -------------------------- 工具方法 --------------------------

    /**
     * 非静态方法：检查用户是否为客服（Operator角色）（供实例方法调用）
     */
    private boolean checkUserIsService(Integer userId) {
        try {
            if (userMapper == null) {
                log.error("UserMapper未注入，无法检查用户[{}]角色", userId);
                return false;
            }
            User user = userMapper.selectbyUserId(userId);
            return user != null && User.Role.Operator.equals(user.getRole());
        } catch (Exception e) {
            log.error("检查用户[{}]角色失败", userId, e);
            return false;
        }
    }

    /**
     * 静态方法：检查用户是否为客服（供静态上下文调用）
     */
    private static boolean checkUserIsServiceStatic(Integer userId) {
        try {
            if (userMapper == null) {
                log.error("UserMapper未注入，无法检查用户[{}]角色", userId);
                return false;
            }
            User user = userMapper.selectbyUserId(userId);
            return user != null && User.Role.Operator.equals(user.getRole());
        } catch (Exception e) {
            log.error("检查用户[{}]角色失败", userId, e);
            return false;
        }
    }

    /**
     * 关闭会话（封装关闭逻辑）
     */
    private void closeSession(Session session, int closeCode, String reason) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.valueOf(String.valueOf(closeCode)), reason));
            log.debug("会话[{}]已关闭，原因：{}（代码：{}）", session.getId(), reason, closeCode);
        } catch (Exception e) {
            log.error("关闭会话[{}]失败，原因：{}（代码：{}）", session.getId(), reason, closeCode, e);
        }
    }

    /**
     * 广播用户上下线消息
     */
    private void broadcastOnlineMsg(Integer userId, boolean isOnline) {
        Message broadcastMsg = new Message();
        broadcastMsg.setContent(String.format("用户[%s]已%s线", userId, isOnline ? "上" : "下"));
        broadcastMsg.setCreate_time(new Date()); // 匹配LocalDateTime类型
        sendMessageAll(JSONObject.toJSONString(broadcastMsg));
    }

    /**
     * 清理会话相关资源（核心：确保计数准确+资源不残留）
     */
    private static void cleanSession(Session session, Integer userId) {
        try {
            // 1. 清理用户映射和在线人数（原子操作）
            if (userId != null) {
                WebSocket webSocket = userMap.get(userId);
                // 仅清理当前会话对应的用户映射（避免误删新连接）
                if (webSocket != null && webSocket.getSession().equals(session)) {
                    userMap.remove(userId);
                    loginCount.decrementAndGet(); // 原子减计数，确保准确
                    log.debug("清理用户[{}]映射，当前在线人数：{}", userId, loginCount.get());
                }
            }

            // 2. 关闭会话（双重检查）
            if (session != null && session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "会话清理"));
            }
        } catch (Exception e) {
            log.error("清理会话[{}]失败，用户[{}]", session != null ? session.getId() : "null", userId, e);
        } finally {
            // 3. 清理所有关联缓存（无论是否异常，确保资源不残留）
            if (session != null) {
                lastHeartbeatTime.remove(session);
                sessionToUserId.remove(session);
            }
            if (userId != null) {
                userRoleCache.remove(userId);
            }
        }
    }
}
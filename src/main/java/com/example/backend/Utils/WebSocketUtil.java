package com.example.backend.Utils;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.example.backend.Entity.WebSocket.WebSocket;
import com.example.backend.Entity.WebSocket.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过这个类进行连接WebSocket的，默认发信息就进入onMessage解析
 */
@Component
@ServerEndpoint(value = "/webSocket/{userId}")
@Slf4j
public class WebSocketUtil {

    /**
     * 登录连接数 应该也是线程安全的
     */
    private static int loginCount = 0;
    /**
     * user 线程安全的
     */
    private static final Map<Integer, WebSocket> userMap = new ConcurrentHashMap<>();

    /**
     * 收到消息触发事件，这个消息是连接人发送的消息
     * @param messageInfo 消息内容
     * @param session 会话信息
     */
    @OnMessage
    public void onMessage(String messageInfo, Session session) {
        if (StringUtils.isBlank(messageInfo)) {
            return;
        }
        Integer senderUserId = Integer.parseInt(session.getPathParameters().get("userId"));
        log.info("onMessage:{}", messageInfo);
        System.out.println("接收信息:" + messageInfo + "," + senderUserId);
        try {
            JSONObject jsonObject = JSONObject.parseObject(messageInfo);
            Integer receiverUserId = jsonObject.getInteger("receive_user");
            if (receiverUserId == null) {
                log.error("消息中未包含接收方用户ID，消息内容: {}", messageInfo);
                return;
            }

            // 查找接收方的 WebSocket 会话
            WebSocket receiverWebSocket = userMap.get(receiverUserId);
            if (receiverWebSocket != null) {
                try {
                    // 发送消息给接收方
                    receiverWebSocket.getSession().getAsyncRemote().sendText(messageInfo);
                    log.info("消息已发送给用户 {}，消息内容: {}", receiverUserId, messageInfo);
                } catch (Exception e) {
                    log.error("发送消息给用户 {} 失败: {}", receiverUserId, e.getMessage(), e);
                }
            } else {
                log.info("用户 {} 不在线，消息暂不处理或存储", receiverUserId);
            }
        } catch (Exception e) {
            log.error("消息解析失败: {}", e.getMessage());
        }
        log.info(DateUtil.now() + " | " + senderUserId + " 私人消息-> " + messageInfo);
    }

    /**
     * 打开连接触发事件
     * @param userId 用户 ID
     * @param session 会话信息
     */
    @OnOpen
    public void onOpen(@PathParam("userId") Integer userId, Session session) {
        log.info("onOpen:{}", userId);
        WebSocket webSocket = new WebSocket();
        webSocket.setUserId(userId);
        webSocket.setSession(session);
        if (!userMap.containsKey(userId)) {
            // 添加登录用户数量
            addLoginCount();
            userMap.put(userId, webSocket);
        }
        log.info("打开连接触发事件!已连接用户: " + userId);
        log.info("当前在线人数: " + loginCount);
        Message message = new Message();
        message.setContent(userId + " 已连接");
        sendMessageAll(JSONObject.toJSONString(message));
    }

    /**
     * 关闭连接触发事件
     * @param userId 用户 ID
     * @param session 会话信息
     * @param closeReason 关闭原因
     */
    @OnClose
    public void onClose(@PathParam("userId") Integer userId, Session session, CloseReason closeReason) {
        if (userMap.containsKey(userId)) {
            // 删除map中用户
            userMap.remove(userId);
            // 减少断开连接的用户
            reduceLoginCount();
        }
        log.info("关闭连接触发事件!已断开用户: " + userId);
        log.info("当前在线人数: " + loginCount);
        Message message = new Message();
        message.setContent(userId + " 已断开");
        sendMessageAll(JSONObject.toJSONString(message));
    }

    /**
     * 传输消息错误触发事件
     * @param error 错误信息
     */
    @OnError
    public void onError(Throwable error) {
        log.error("onError: {}", error.getMessage(), error);
    }

    /**
     * 发送指定用户信息
     * @param message 信息内容
     * @param userId 用户 ID
     */
    public void sendMessageTo(String message, Integer userId) {
        for (WebSocket user : userMap.values()) {
            if (user.getUserId().equals(userId)) {
                try {
                    System.out.println("用户:" + userId + "收到信息:" + message);
                    user.getSession().getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    log.error("发送消息给用户 {} 失败: {}", userId, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 发给所有人
     * @param message 信息内容
     */
    public void sendMessageAll(String message) {
        for (WebSocket item : userMap.values()) {
            try {
                item.getSession().getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error("广播消息失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 连接登录数增加
     */
    public static synchronized void addLoginCount() {
        loginCount++;
    }

    /**
     * 连接登录数减少
     */
    public static synchronized void reduceLoginCount() {
        loginCount--;
    }

    /**
     * 获取用户
     * @return 用户映射
     */
    public synchronized Map<Integer, WebSocket> getUsers() {
        return userMap;
    }
}
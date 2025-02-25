package com.example.backend.Entity.WebSocket;

import java.util.ArrayList;
import java.util.List;

import com.example.backend.Entity.User;
import lombok.Data;

@Data
public class MessageForm {

    // 发送用户和接收用户完整聊天消息列表
    private List<Message> messages = new ArrayList<>();
    // 未读消息数量
    private Integer no_read_message_length;
    // 在线标识
    private Boolean is_online;
    // 发送信息用户
    private User send_user;
    // 接收信息用户,偏向于赋值登录人员用户信息
    private User receive_user;
    // 最新一条聊天记录
    private String last_message;
}

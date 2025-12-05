package com.example.backend.Model.WebSocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    private String handle;

    private Integer send_user;

    private Integer receive_user;

    private String content;

    private String is_read;

    private Date create_time;

    private String type; // 新增消息类型字段
}

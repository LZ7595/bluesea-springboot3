package com.example.backend.Entity.WebSocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    private String handle;

    private Integer send_user;

    private Integer receive_user;

    private String content;

    private String is_read;

    private LocalDateTime create_time;
}

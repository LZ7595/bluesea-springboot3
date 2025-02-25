package com.example.backend.Entity.WebSocket;


import jakarta.websocket.Session;
import lombok.Data;

@Data
public class WebSocket {

    private Session session;
    private Integer userId;
}


package com.example.backend.Model.WebSocket;


import jakarta.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocket {

    private Session session;
    private Integer userId;
}


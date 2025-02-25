package com.example.backend.Controller;

import com.example.backend.Entity.WebSocket.Message;
import com.example.backend.Entity.WebSocket.MessageForm;
import com.example.backend.Service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {

    // 逻辑层
    @Autowired
    private MessageService messageService;

    // 查找未读数量
    @GetMapping("/findNoReadMessageLength")
    public ResponseEntity<?> findNoReadMessage(@RequestParam("userId") int userId) {
        try {
            Integer total = messageService.findNoReadMessageLength(userId);
            return new ResponseEntity<>(total, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 查找两个人的聊天记录
    @GetMapping("/findMessageBySendUserAndReceiveUser")
    public ResponseEntity<?> findMessageBySendUserAndReceiveUser(
            @RequestParam("sendUserId") int sendUserId,
            @RequestParam("receiveUserId") int receiveUserId) {
        try {
            List<Message> messages = messageService.findMessageBySendUserAndReceiveUser(sendUserId, receiveUserId);
            return new ResponseEntity<>(messages, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 发送信息
    @PostMapping("/sendMessage")
    public ResponseEntity<?> sendMessage(@RequestBody Message message) {
        System.out.println(message);
        try {
            messageService.sendMessage(message);
            return new ResponseEntity<>("发送成功!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 查找我的用户信息数据
    @GetMapping("/searchUserForForm")
    public ResponseEntity<?> searchUserForForm(
            @RequestParam("loginUserId") int loginUserId,
            @RequestParam("searchUserName") String searchUserName) {
        try {
            List<MessageForm> messages = messageService.searchUserForForm(loginUserId, searchUserName);
            return new ResponseEntity<>(messages, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
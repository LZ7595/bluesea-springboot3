package com.example.backend.Controller;

import com.example.backend.Entity.UserInfo;
import com.example.backend.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    // 根据用户 ID 查询用户信息
    @GetMapping("/{userId}")
    public Optional<UserInfo> getUserInfo(@PathVariable Integer userId) {
        return userService.getUserInfoById(userId);
    }

    // 修改用户名
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserInfo(@PathVariable Integer id, @RequestParam String name, @RequestParam String gender, @DateTimeFormat(pattern = "yyyy-MM-dd") Date birthday) {
        return userService.updateUserInfo(id, name, gender, birthday);
    }

    @GetMapping("/security/{id}")
    public ResponseEntity<?> getSecurityInfo(@PathVariable Integer id) {
        return userService.getSecurityInfo(id);
    }

    @PostMapping("/sendCode")
    public ResponseEntity<?> sendVerificationCode(@RequestParam String info, @RequestParam String type, @RequestParam Integer num) {
        return userService.sendVerificationCode(info, type, num);
    }

    @PostMapping("/ConfirmChange")
    public ResponseEntity<?> confirmChange(@RequestParam Integer userId, @RequestParam String info, @RequestParam String code, @RequestParam String type, @RequestParam Integer num) {
        return userService.confirmChange(userId, info, code, type, num);
    }
}


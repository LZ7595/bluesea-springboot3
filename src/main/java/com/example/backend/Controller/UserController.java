package com.example.backend.Controller;

import com.example.backend.Impl.UserServiceImpl;
import com.example.backend.Model.Vo.UserInfo;
import com.example.backend.Service.UserService;

import com.example.backend.Utils.Common;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private Common common;

    // 根据用户 ID 查询用户信息
    @GetMapping("/{userId}")
    public Optional<UserInfo> getUserInfo(@PathVariable Integer userId) {
        return userService.getUserInfoById(userId);
    }

    // 修改用户信息
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserInfo(@PathVariable Integer id, @RequestParam String name, @RequestParam String gender, @DateTimeFormat(pattern = "yyyy-MM-dd") Date birthday, @RequestParam String avatar, HttpServletRequest request) {
        return userService.updateUserInfo(id, name, gender, birthday, avatar,request);
    }

    @GetMapping("/security/{id}")
    public ResponseEntity<?> getSecurityInfo(@PathVariable Integer id) {
        return userService.getSecurityInfo(id);
    }
    
    @GetMapping("/searchUserByUserId")
    public ResponseEntity<?> searchUserByUserId(@RequestParam Integer userId) {
        return userService.searchUserByUserId(userId);
    }

    @PostMapping("/getUserInfo")
    public UserInfo getUserInfo(HttpServletRequest request) {
        String accessToken = common.extractAccessTokenByClient(request);
        System.out.println("accessToken113: " + accessToken);
        return userService.getUserInfo(request);
    }
}

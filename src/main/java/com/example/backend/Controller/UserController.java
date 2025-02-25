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

    // 确认旧密码修改密码
    @PostMapping("/confirmChangePasswordByOldPassword")
    public ResponseEntity<?> confirmChangePasswordByOldPassword(@RequestParam Integer userId, @RequestParam String oldPassword) {
        return userService.confirmChangePasswordByOldPassword(userId, oldPassword);
    }

    // 确认邮箱验证修改密码
    @PostMapping("/confirmChangePasswordByEmail")
    public ResponseEntity<?> confirmChangePasswordByEmail(@RequestParam Integer userId, @RequestParam String email, @RequestParam String code) {
        return userService.confirmChangePasswordByEmail(userId, email, code);
    }

    // 确认手机验证修改密码
    @PostMapping("/confirmChangePasswordByPhone")
    public ResponseEntity<?> confirmChangePasswordByPhone(@RequestParam Integer userId, @RequestParam String phone, @RequestParam String code) {
        return userService.confirmChangePasswordByPhone(userId, phone, code);
    }

    // 旧密码修改密码
    @PostMapping("/changePasswordByOldPassword")
    public ResponseEntity<?> changePasswordByOldPassword(@RequestParam Integer userId, @RequestParam String oldPassword, @RequestParam String newPassword) {
        return userService.changePasswordByOldPassword(userId, oldPassword, newPassword);
    }

    // 邮箱验证修改密码
    @PostMapping("/changePasswordByEmail")
    public ResponseEntity<?> changePasswordByEmail(@RequestParam Integer userId, @RequestParam String email, @RequestParam String code, @RequestParam String newPassword) {
        return userService.changePasswordByEmail(userId, email, code, newPassword);
    }

    // 手机验证修改密码
    @PostMapping("/changePasswordByPhone")
    public ResponseEntity<?> changePasswordByPhone(@RequestParam Integer userId, @RequestParam String phone, @RequestParam String code, @RequestParam String newPassword) {
        return userService.changePasswordByPhone(userId, phone, code, newPassword);
    }

    @GetMapping("/searchUserByUserId")
    public ResponseEntity<?> searchUserByUserId(@RequestParam Integer userId) {
        return userService.searchUserByUserId(userId);
    }
}

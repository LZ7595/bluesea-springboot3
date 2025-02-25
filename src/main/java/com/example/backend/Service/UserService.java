package com.example.backend.Service;

import com.example.backend.Entity.UserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.Optional;

public interface UserService {
    Optional<UserInfo> getUserInfoById(Integer userId);

    ResponseEntity<?> updateUserInfo(Integer userId, String username, String gender, Date birthday);

    ResponseEntity<?> getSecurityInfo(Integer userId);

    ResponseEntity<?> sendVerificationCode(String info, String type, Integer num);

    ResponseEntity<?> confirmChange(Integer userId, String info, String code, String type, Integer num);
    boolean validateCode(String email, String code);

    // 确认旧密码修改密码
    ResponseEntity<?> confirmChangePasswordByOldPassword(Integer userId, String oldPassword);

    // 确认邮箱验证修改密码
    ResponseEntity<?> confirmChangePasswordByEmail(Integer userId, String email, String code);

    // 确认手机验证修改密码
    ResponseEntity<?> confirmChangePasswordByPhone(Integer userId, String phone, String code);

    // 旧密码修改密码
    ResponseEntity<?> changePasswordByOldPassword(Integer userId, String oldPassword, String newPassword);

    // 邮箱验证修改密码
    ResponseEntity<?> changePasswordByEmail(Integer userId, String email, String code, String newPassword);

    // 手机验证修改密码
    ResponseEntity<?> changePasswordByPhone(Integer userId, String phone, String code, String newPassword);

    ResponseEntity<?> searchUserByUserId(Integer userId);
}

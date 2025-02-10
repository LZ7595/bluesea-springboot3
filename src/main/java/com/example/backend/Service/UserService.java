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
}

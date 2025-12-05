package com.example.backend.Service;

import com.example.backend.Impl.UserServiceImpl;
import com.example.backend.Model.Vo.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.Optional;

public interface UserService {
    Optional<UserInfo> getUserInfoById(Integer userId);

    ResponseEntity<?> updateUserInfo(Integer userId, String username, String gender, Date birthday, String avatar, HttpServletRequest request);

    ResponseEntity<?> getSecurityInfo(Integer userId);

    ResponseEntity<?> searchUserByUserId(Integer userId);

    UserInfo getUserInfo(HttpServletRequest request);
}

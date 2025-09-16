package com.example.backend.Service;

import com.example.backend.Entity.Enum.LoginType;
import com.example.backend.Entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;


public interface AuthService {
    ResponseEntity<?> sendVerificationCode(String email, int type);
    boolean validateCode(String email, String code);
    ResponseEntity<?> registerUser(User user);
    ResponseEntity<?> loginUser(User user, LoginType type, HttpServletRequest request);


    ResponseEntity<?> logoutUser(HttpServletRequest request);
    boolean isUsernameUsed(String username);
    boolean isEmailUsed(String email);

    String refreshToken(String refreshToken);
}

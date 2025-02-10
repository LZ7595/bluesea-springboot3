package com.example.backend.Service;

import com.example.backend.Entity.Enum.LoginType;
import com.example.backend.Entity.User;
import org.springframework.http.ResponseEntity;


public interface AuthService {
    ResponseEntity<?> sendVerificationCode(String email, int type);
    boolean validateCode(String email, String code);
    ResponseEntity<?> registerUser(User user);
    ResponseEntity<?> loginUser(User user, LoginType type);

    ResponseEntity<?> logoutUser();
    boolean isUsernameUsed(String username);
    boolean isEmailUsed(String email);
}

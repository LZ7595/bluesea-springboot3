package com.example.backend.Service;

import com.example.backend.Model.Enum.LoginType;
import com.example.backend.Model.Entity.User;
import com.example.backend.Model.Vo.IdentityVerifyRequest;
import com.example.backend.Model.Vo.OperationExecuteRequest;
import com.example.backend.Model.Vo.SendCodeRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;


public interface AuthService {
    boolean validateCode(String email, String code);
    ResponseEntity<?> registerUser(User user);

    ResponseEntity<?> verifyIdentity(IdentityVerifyRequest request, HttpServletRequest servletRequest);
    ResponseEntity<?> loginUser(User user, LoginType type, HttpServletRequest request);

    ResponseEntity<?> executeOperation(OperationExecuteRequest request, HttpServletRequest servletRequest);
    ResponseEntity<?> sendCode(SendCodeRequest request, HttpServletRequest servletRequest);
    ResponseEntity<?> confirmChange(Integer userId, String info, String code, String type, Integer num);
    ResponseEntity<?> logoutUser(HttpServletRequest request);
    boolean isUsernameUsed(String username);
    boolean isEmailUsed(String email);

    String refreshToken(String refreshToken);
}

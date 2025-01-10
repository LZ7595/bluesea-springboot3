package com.example.backend.Service;

import com.example.backend.Entity.User;
import com.example.backend.Entity.Result;

import java.util.List;

public interface UserService {
    Result selectUser(String username);
    User getUserById(Long id);
    User updateUser(User user);
    void deleteUser(Long id);
}

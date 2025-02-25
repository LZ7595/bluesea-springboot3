package com.example.backend.Service.back;

import com.example.backend.Entity.back.UserDetailsBack;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserBackService {

    ResponseEntity<?> SearchUserList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    ResponseEntity<UserDetailsBack> getUserDetailsBack(Long id);

    int updateUser(UserDetailsBack user);
    String uploadFile(org.springframework.web.multipart.MultipartFile file);

    int addUser(UserDetailsBack user);
    int deleteUser(Long user_id);
    int deleteUserMore(List<Long> userIdList);
}

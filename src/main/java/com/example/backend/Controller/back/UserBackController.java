package com.example.backend.Controller.back;

import com.example.backend.Entity.back.UserDetailsBack;
import com.example.backend.Service.back.UserBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/back/user")
public class UserBackController {
    @Autowired
    private UserBackService userBackService;
    @GetMapping("/search")
    public ResponseEntity<?> SearchUserList(
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return userBackService.SearchUserList(searchKeyword, sortField, sortOrder, currentPage, pageSize);
    }

    @GetMapping("/details")
    public ResponseEntity<?> DetailBack(@RequestParam Long id) {
        return userBackService.getUserDetailsBack(id);
    }

    @PutMapping("/update")
    public String updateUser(@RequestBody UserDetailsBack user) {
        System.out.println(user);
        int result = userBackService.updateUser(user);
        if (result > 0) {
            return "用户信息修改成功";
        } else {
            return "用户信息修改失败";
        }
    }

    @PostMapping("/add")
    public String addUser(@RequestBody UserDetailsBack user) {
        int result = userBackService.addUser(user);
        if (result > 0) {
            return "用户信息添加成功";
        } else {
            return "用户信息添加失败";
        }
    }

    @PostMapping("/upload")
    public String uploadFiles(@RequestParam("file") MultipartFile files) {
        System.out.println("Received files: " + files);
            String filePath = userBackService.uploadFile(files);
        return filePath;
    }

    @DeleteMapping("/deleteone")
    public String deleteUser(@RequestParam Long id) {
        int result = userBackService.deleteUser(id);
        if (result > 0) {
            return "用户信息删除成功";
        } else {
            return "用户信息删除失败";
        }
    }
    @DeleteMapping("/deletemore")
    public String deleteUserMore(@RequestBody List<Long> userIdList) {
        try {
            // 调用服务层方法处理删除逻辑
            userBackService.deleteUserMore(userIdList);
            return "用户删除成功";
        } catch (Exception e) {
            return "用户删除失败: " + e.getMessage();
        }
    }
}

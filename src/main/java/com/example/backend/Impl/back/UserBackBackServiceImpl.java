package com.example.backend.Impl.back;

import com.example.backend.Dao.back.UserBackMapper;

import com.example.backend.Entity.back.UserDetailsBack;
import com.example.backend.Entity.back.UserResponsePageResultBack;

import com.example.backend.Service.back.UserBackService;

import com.example.backend.Utils.Encryption;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class UserBackBackServiceImpl implements UserBackService {
    @Autowired
    private UserBackMapper userBackMapper;
    public ResponseEntity<?> SearchUserList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize) {
        try {
            System.out.println(searchKeyword);
            Map<String, Object> params = new HashMap<>();
            params.put("searchKeyword", searchKeyword);
            params.put("sortField", sortField);
            params.put("sortOrder", sortOrder);
            params.put("offset", (currentPage - 1) * pageSize);
            params.put("pageSize", pageSize);
            List<UserDetailsBack> userList = userBackMapper.SearchUserList(params);
            Map<String, Object> countParams = new HashMap<>();
            countParams.put("searchKeyword", searchKeyword);
            int total = userBackMapper.getSearchUserTotal(countParams);
            System.out.println(userList);
            if (userList != null) {
                UserResponsePageResultBack userResponsePageResultBack = new UserResponsePageResultBack(userList, total);
                return ResponseEntity.ok().body(userResponsePageResultBack);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e);
        }
        return ResponseEntity.status(404).body(null);
    }

    public ResponseEntity<UserDetailsBack> getUserDetailsBack(Long userId) {
        try {
            UserDetailsBack userDetail = userBackMapper.getUserDetailsBackById(userId);
            System.out.println(userDetail);
            if (userDetail != null) {
                return ResponseEntity.ok().body(userDetail);
            } else {
                return ResponseEntity.status(404).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public int updateUser(UserDetailsBack user) {
        try {
            // 查询原记录
            UserDetailsBack originalUser = userBackMapper.getUserDetailsBackById(user.getId());
            if (originalUser == null) {
                System.out.println("未找到用户记录，无法更新");
                return 0;
            }

            String originalPassword = originalUser.getPassword();
            String newPassword = user.getPassword();

            // 判断传入的密码是否为加密密码
            boolean isNewPasswordEncrypted = Encryption.isEncrypted(newPassword);

            if (isNewPasswordEncrypted) {
                // 如果传入的是加密密码，直接比较两个加密密码是否相等
                if (newPassword.equals(originalPassword)) {
                    // 密码未改变，不做处理
                    System.out.println("密码未改变，无需更新");
                } else {
                    // 新的加密密码和原加密密码不同，可能存在异常，这里简单记录日志，可根据需求处理
                    System.out.println("接收到不同的加密密码，可能存在异常");
                    user.setPassword(newPassword);
                }
            } else {
                // 如果传入的是未加密密码，进行验证和加密
                if (!Encryption.verifyPassword(newPassword, originalPassword)) {
                    // 密码有变化，进行加密
                    newPassword = Encryption.encryptPassword(newPassword);
                    user.setPassword(newPassword);
                }
            }

            // 更新 user 表记录
            int result = userBackMapper.updateUser(user);
            System.out.println("更新用户信息，影响行数: " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            // 可以根据不同的异常类型进行不同的处理
            if (e instanceof org.springframework.dao.DataAccessException) {
                System.out.println("数据库访问异常: " + e.getMessage());
            } else {
                System.out.println("其他异常: " + e.getMessage());
            }
            return 0;
        }
    }


    @Override
    public int addUser(UserDetailsBack user) {
        try {
             String encryptPassword = Encryption.encryptPassword(user.getPassword());
            user.setPassword(encryptPassword);
            System.out.println(user);
            // 插入 user 表记录
            int userResult = userBackMapper.addUser(user);
            System.out.println(userResult);
            if (userResult > 0) {
                // 插入 user_details 表记录
                int detailsResult = userBackMapper.addUserDetails(user);
                System.out.println(detailsResult);
                System.out.println(user);
                if (detailsResult > 0) {
                    return userResult + detailsResult;
                } else {
                    throw new RuntimeException("插入 user_details 表失败");
                }
            } else {
                throw new RuntimeException("插入 user 表失败");
            }
        } catch (Exception e) {
            // 事务会自动回滚
            throw new RuntimeException("添加用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteUser(Long userId) {
        return userBackMapper.deleteUser(userId);
    }

    @Override
    public int deleteUserMore(List<Long> userIdList) {
        return userBackMapper.deleteUserMore(userIdList);
    }
}

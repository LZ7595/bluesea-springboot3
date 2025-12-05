package com.example.backend.Dao;


import com.example.backend.Model.Entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface AuthMapper {

    @Select("SELECT COUNT(*) FROM user WHERE email = '${email}'")
    int selectEmail(String email);

    @Select("SELECT COUNT(*) FROM user WHERE username = #{username}")
    int selectUsername(String username);

    @Select("<script>SELECT id FROM user WHERE username = #{info} OR email = #{info} OR phone = #{info} </script>")
    User LoginVerification(String info);

    @Insert("INSERT INTO user (username, password, email, phone, status, register_time) " +
            "VALUES (#{username}, #{password}, #{email}, #{phone}, #{status}, #{registerTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Boolean insert(User user);

    @Update("<script>UPDATE user SET last_login_time = #{now}, last_login_type = #{LoginType} WHERE username = #{info} OR email = #{info} OR phone = #{info}</script>")
    void updateLastLoginTime(@Param("info") String info, @Param("now") LocalDateTime now, @Param("LoginType") String LoginType);

    /**
     * 根据用户 ID 查询头像链接
     *
     * @param userId 用户 ID
     * @return 用户头像链接
     */
    @Select("SELECT avatar FROM user_details WHERE user_id = #{userId}")
    String getImageUrlsByUserId(Integer userId);


    @Select("SELECT u.*, ud.avatar " +
            "FROM user u " +
            "LEFT JOIN user_details ud ON u.id = ud.user_id " +
            "WHERE u.id = #{userId}")
    User getUserById(Integer userId);

    /**
     * 清空用户邮箱（设置为null或空字符串）
     */
    @Update("UPDATE user SET email = NULL WHERE id = #{userId}")
    int clearEmail(@Param("userId") Integer userId);

    /**
     * 清空用户手机号（设置为null或空字符串）
     */
    @Update("UPDATE user SET phone = NULL WHERE id = #{userId}")
    int clearPhone(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM user WHERE phone = #{phone}")
    int selectPhone(String phone);

    @Update("UPDATE user SET phone = #{phone} WHERE id = #{userId}")
    int updatePhone(Integer userId, String phone);

    @Update("UPDATE user SET email = #{email} WHERE id = #{userId}")
    int updateEmail(Integer userId, String email);

    // 修改用户密码
    @Update("UPDATE user SET password = #{newPassword} WHERE id = #{userId}")
    int updatePassword(Integer userId, String newPassword);

    // 在AuthMapper中添加方法
    @Select("SELECT * FROM user WHERE email = #{email} AND status = 1")
    User LoginVerificationByEmail(@Param("email") String email);

    @Select("SELECT * FROM user WHERE phone = #{phone} AND status = 1")
    User LoginVerificationByPhone(@Param("phone") String phone);

    @Select("SELECT * FROM user WHERE username = #{username} AND status = 1")
    User LoginVerificationByUsername(@Param("username") String username);

    @Select("SELECT password FROM user WHERE id = #{userId} AND status = 1")
    String getPasswordByUserId(Integer userId);

    @Select("SELECT COUNT(*) FROM user WHERE email = #{email} AND status = 1")
    int getEmailCount(String email);

@Select("SELECT COUNT(*) FROM user WHERE phone = #{phone} AND status = 1")
    int getPhoneCount(String phone);
}
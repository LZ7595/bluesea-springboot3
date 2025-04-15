package com.example.backend.Dao;

import com.example.backend.Entity.User;
import com.example.backend.Entity.UserInfo;
import com.example.backend.Entity.UserSecurity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(int id);

    @Select("SELECT u.username, ud.birthday, ud.gender, ud.avatar, u.role " +
            "FROM user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "WHERE u.id = #{userId}")
    Optional<UserInfo> getUserInfoById(Integer userId);

    // 修改用户名
    @Update("UPDATE user SET username = #{username} WHERE id = #{userId}")
    int updateUsername(Integer userId, String username);

    // 修改生日
    @Update("UPDATE user_details SET birthday = #{birthday} WHERE user_id = #{userId}")
    int updateBirthday(Integer userId, java.sql.Date birthday);

    // 修改性别
    @Update("UPDATE user_details SET gender = #{gender} WHERE user_id = #{userId}")
    int updateGender(Integer userId, String gender);

    // 修改头像
    @Update("UPDATE user_details SET avatar = #{avatar} WHERE user_id = #{userId}")
    int updateAvatar(Integer userId, String avatar);

    @Select("SELECT id , username , phone , email FROM user WHERE id = #{userId}")
    UserSecurity getSecurityInfo(Integer userId);

    @Update("UPDATE user SET code = NULL, codeExpiration = NULL WHERE id = #{userId}")
    void clearCode(Integer userId);

    @Update("UPDATE user SET code = NULL, codeExpiration = NULL WHERE codeExpiration < #{now}")
    void deleteExpiredCodes(LocalDateTime now);

    @Update("UPDATE user SET code = #{code}, codeExpiration = #{expirationTime} WHERE email = #{email}")
    int updateEmailCode(String code, LocalDateTime expirationTime, String email);

    @Update("UPDATE user SET code = #{code}, codeExpiration = #{expirationTime} WHERE phone = #{phone}")
    int updatePhoneCode(String code, LocalDateTime expirationTime, String phone);

    @Select("SELECT COUNT(*) FROM user WHERE email = #{email}")
    int selectEmail(String email);

    @Select("SELECT COUNT(*) FROM user WHERE phone = #{phone}")
    int selectPhone(String phone);

    @Select("SELECT code FROM user WHERE email = #{email}")
    String selectEmailCode(String email);

    @Select("SELECT code FROM user WHERE phone = #{phone}")
    String selectPhoneCode(String phone);

    @Update("UPDATE user SET phone = #{phone} WHERE id = #{userId}")
    int updatePhone(Integer userId, String phone);

    @Update("UPDATE user SET email = #{email} WHERE id = #{userId}")
    int updateEmail(Integer userId, String email);

    // 获取用户密码
    @Select("SELECT password FROM user WHERE id = #{userId}")
    String getPasswordById(Integer userId);

    // 修改用户密码
    @Update("UPDATE user SET password = #{newPassword} WHERE id = #{userId}")
    int updatePassword(Integer userId, String newPassword);

    @Select("SELECT * FROM user WHERE id = #{userId}")
    User searchUserByUserId(Integer userId);

    // 根据用户 ID 查询用户信息
    @Select("SELECT u.* , ud.*  FROM user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "WHERE u.id = #{userId}")
    User selectbyUserId(@Param("userId") int userId);

    // 根据用户名查询用户信息
    @Select("SELECT u.* , ud.*  FROM user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            " WHERE username LIKE CONCAT('%', #{username}, '%')")
    List<User> selectByUserName(@Param("username") String username);


}
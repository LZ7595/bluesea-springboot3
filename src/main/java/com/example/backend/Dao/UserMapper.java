package com.example.backend.Dao;

import com.example.backend.Entity.UserInfo;
import com.example.backend.Entity.UserSecurity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface UserMapper {

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

    @Select("SELECT id , username , phone , email FROM user WHERE id = #{userId}")
    UserSecurity getSecurityInfo(Integer userId);

    @Update("UPDATE user SET code = NULL, codeExpiration = NULL WHERE id = '${userId}'")
    void clearCode(Integer userId);

    @Update("UPDATE user SET code = NULL, codeExpiration = NULL WHERE codeExpiration < '${now}'")
    void deleteExpiredCodes(LocalDateTime now);

    @Update("UPDATE user SET code = #{code}, codeExpiration = #{expirationTime} WHERE email = '${email}'")
    int updateEmailCode(String code, LocalDateTime expirationTime, String email);

    @Update("UPDATE user SET code = #{code}, codeExpiration = #{expirationTime} WHERE phone = '${phone}'")
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
}
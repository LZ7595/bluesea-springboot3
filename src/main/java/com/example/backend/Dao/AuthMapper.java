package com.example.backend.Dao;


import com.example.backend.Entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface AuthMapper {

    @Select("SELECT COUNT(*) FROM user WHERE email = '${email}'")
    int selectEmail(String email);

    @Select("SELECT COUNT(*) FROM user WHERE username = #{username}")
    int selectUsername(String username);

    @Select("<script>SELECT username , password , role , id FROM user WHERE username = #{info} OR email = #{info}</script>")
    User LoginVerification(String info);

    @Insert("INSERT INTO user (email, password, username) VALUES (#{email}, #{password}, #{username})")
    Boolean insert(User user);

    @Update("UPDATE user SET code = #{code}, codeExpiration = #{expirationTime} WHERE email = '${email}'")
    int updateCode(String code, LocalDateTime expirationTime, String email);

    @Update("<script>UPDATE user SET last_login_time = #{now}, last_login_type = #{LoginType} WHERE username = #{info} OR email = #{info}</script>")
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
}
package com.example.backend.Dao;

import com.example.backend.Model.Entity.User;
import com.example.backend.Model.Vo.UserInfo;
import com.example.backend.Model.Vo.UserSecurity;
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



    @Select("SELECT * FROM user WHERE id = #{userId}")
    User searchUserByUserId(Integer userId);

    // 根据用户 ID 查询用户信息
    @Select("SELECT u.* , ud.*  FROM user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "WHERE u.id = #{userId}")
    User selectbyUserId(@Param("userId") int userId);

    @Select("SELECT u.* , ud.*  FROM user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "WHERE u.id = #{userId}")
    UserInfo getUserInfo(@Param("userId") int userId);

    // 根据用户名查询用户信息
    @Select("SELECT u.* , ud.*  FROM user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            " WHERE username LIKE CONCAT('%', #{username}, '%')")
    List<User> selectByUserName(@Param("username") String username);


    @Select("SELECT u.* , ud.avatar FROM user u JOIN user_details ud ON u.id = ud.user_id WHERE u.role = #{role} AND u.status = 1 ORDER BY id ASC LIMIT #{limit}")
    List<User> selectByRoleWithLimit(
            @Param("role") String role,
            @Param("limit") Integer limit);

    @Select("SELECT u.* , ud.avatar FROM user u JOIN user_details ud ON u.id = ud.user_id WHERE u.role = #{role} AND u.status = 1 AND u.id = #{id}")
    User selectByRoleAndId(@Param("role") String role, @Param("id") Integer id);
}
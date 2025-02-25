package com.example.backend.Dao.back;

import com.example.backend.Entity.User;
import com.example.backend.Entity.back.UserDetailsBack;
import com.example.backend.Entity.back.UserDetailsBack;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper

public interface UserBackMapper {

    @Select("<script>" +
            "SELECT u.* , ud.*  FROM user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (u.username LIKE CONCAT('%', #{searchKeyword}, '%') OR u.id LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<UserDetailsBack> SearchUserList(Map<String, Object> params);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "<where> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (u.username LIKE CONCAT('%', #{searchKeyword}, '%') OR u.id LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "</script>")
    int getSearchUserTotal(Map<String, Object> params);

    @Select("SELECT u.* , ud.* FROM user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "WHERE u.id = #{id} ")
    UserDetailsBack getUserDetailsBackById(Long id);

    @Update("UPDATE user u " +
            "JOIN user_details ud ON u.id = ud.user_id " +
            "SET u.username = #{username}, " +
            "    u.password = #{password}, " +
            "    u.email = '${email}' , " +
            "    u.phone = #{phone}, " +
            "    ud.birthday = #{birthday}, " +
            "    u.role = #{role}, " +
            "    u.status = #{status}, " +
            "    ud.gender = #{gender}, " +
            "    ud.avatar = #{avatar} " +
            "WHERE u.id = #{id}")
    int updateUser(UserDetailsBack user);

    /**
     * 插入用户主表信息
     *
     * @param user 用户信息对象
     * @return 插入成功的行数
     */
    @Insert("INSERT INTO user (username, status, password, phone, email, role) " +
            "VALUES (#{user.username}, #{user.status}, '${user.password}', #{user.phone}, #{user.email}, #{user.role})")
    @Options(useGeneratedKeys = true, keyProperty = "user.id", keyColumn = "id")
    int addUser(@Param("user") UserDetailsBack user);

    /**
     * 插入用户详情表信息
     *
     * @param user 用户信息对象
     * @return 插入成功的行数
     */
    @Update(" UPDATE user_details SET user_id = #{user.id}, birthday = #{user.birthday}, gender = #{user.gender}, avatar = #{user.avatar} WHERE user_id = #{user.id}")
    int addUserDetails(@Param("user") UserDetailsBack user);

    @Delete("DELETE FROM user WHERE id = #{userId}")
    int deleteUser(Long userId);


    @Delete("<script>" +
            "DELETE FROM user " +
            "WHERE id IN " +
            "<foreach item='item' index='index' collection='userIdList' " +
            "open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int deleteUserMore(@Param("userIdList") List<Long> userIdList);

    /**
     * 根据用户 ID 查询头像链接
     *
     * @param userId 用户 ID
     * @return 用户头像链接
     */
    @Select("SELECT avatar FROM user_details WHERE user_id = #{userId}")
    String getImageUrlsByUserId(Long userId);

    /**
     * 修改头像
     *
     * @param avatar 头像链接
     * @param userId 用户 ID
     */
    @Update("UPDATE user_details SET avatar = #{avatar} WHERE user_id = #{userId}")
    void updateUserAvatar(@Param("avatar") String avatar, @Param("userId") Long userId);
}

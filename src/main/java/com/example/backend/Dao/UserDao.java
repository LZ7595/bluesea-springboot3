package com.example.backend.Dao;

import com.example.backend.Entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserDao {

    // 根据用户 ID 查询用户信息
    @Select("SELECT * FROM user WHERE id = #{userId}")
    User selectbyUserId(@Param("userId") int userId);

    // 根据用户名查询用户信息
    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{username}, '%')")
    List<User> selectByUserName(@Param("username") String username);

    // 根据条件查询用户信息
    // 根据条件查询用户信息
    @Select("<script>" +
            "SELECT * FROM user " +
            "<where>" +
            "<if test='notInColumn!= null and notInValues!= null and notInValues.length > 0'>" +
            "${notInColumn} NOT IN " +
            "<foreach item='item' index='index' collection='notInValues' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</if>" +
            "</where>" +
            "<if test='limit!= null'>" +
            " LIMIT #{limit}" +
            "</if>" +
            "</script>")
    List<User> selectByWhere(@Param("notInColumn") String notInColumn, @Param("notInValues") Object[] notInValues, @Param("limit") Integer limit);
}

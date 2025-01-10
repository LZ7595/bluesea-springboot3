package com.example.backend.Dao;


import com.example.backend.Entity.Result;
import com.example.backend.Entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    //根据用户名查询用户
    @Select("select * from user where username=#{username}")
    Result selectUserByUsername(String username);
}
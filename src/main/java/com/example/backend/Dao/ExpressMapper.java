package com.example.backend.Dao;


import com.example.backend.Model.Entity.Express;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExpressMapper {
    @Select("select * from express where id = #{id}")
    Express getExpressById(int id);
}

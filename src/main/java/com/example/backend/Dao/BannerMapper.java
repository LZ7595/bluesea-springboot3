package com.example.backend.Dao;

import com.example.backend.Entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BannerMapper {
    @Select("SELECT * FROM banner WHERE status = true")
    List<Banner> getBanners();
}

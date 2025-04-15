package com.example.backend.Dao.back;

import com.example.backend.Entity.Banner;
import com.example.backend.Entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface BannerBackMapper {
    @Delete("DELETE FROM banner WHERE id = #{id}")
    int deleteBanner(Long id);

    @Delete("<script>" +
            "DELETE FROM banner " +
            "WHERE id IN " +
            "<foreach item='item' index='index' collection='ids' " +
            "open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int deleteBanners(List<String> ids);

    @Insert("INSERT INTO banner (image, product_id, status, create_time, update_time) VALUES (#{image}, #{product_id}, #{status}, #{create_time}, #{update_time})")
    int insertBanner(Banner banner);

    @Update("UPDATE banner SET image = #{image}, product_id = #{product_id}, status = #{status}, update_time = #{update_time} WHERE id = #{id}")
    int updateBanner(Banner banner);

    @Update("UPDATE banner SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateBannerStatus(Long id, boolean status);

    @Select("SELECT b.*, p.product_name " +
            "FROM banner b " +
            "LEFT JOIN product p ON p.product_id = b.product_id " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{pageSize}")
    List<Banner> getBannerList(Map<String, Object> params);

    @Select("SELECT COUNT(*) FROM banner")
    int countBanners();
}

package com.example.backend.Dao;

import com.example.backend.Model.Vo.BrandVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper
public interface CategoryBrandMapper {
    @Select({
            "<script>",
            "SELECT b.brand_id, b.brand_name, r.category_id , b.logo  ",
            "FROM brand_category_relation r ",
            "JOIN brand b ON r.brand_id = b.brand_id ",
            "WHERE r.category_id IN ",
            "<foreach collection='categoryIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "AND r.status = 1 AND b.status = 1 ",
            "ORDER BY b.order_num",
            "</script>"
    })
    List<BrandVO> selectBrandsByCategoryIds(@Param("categoryIds") List<Integer> categoryIds);

    @Select({
            "<script>",
            "SELECT b.brand_id, b.brand_name, r.category_id, b.logo ",
            "FROM brand_category_relation r ",
            "JOIN brand b ON r.brand_id = b.brand_id ",
            "WHERE r.category_id IN",
            "<foreach item='id' collection='categoryIds' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "AND r.status = 1 ",
            "AND b.status = 1 ",
            "<if test='excludeName != null and excludeName != \"\"'>",
            "  AND b.brand_name != #{excludeName}",
            "</if>",
            "ORDER BY b.order_num",
            "</script>"
    })
    List<BrandVO> selectBrandsByCategoryId(@Param("categoryIds") List<Long> categoryIds, @Param("excludeName") String excludeName);

    default Map<Integer, List<BrandVO>> selectBrandMapByCategoryIds(List<Integer> categoryIds) {
        List<BrandVO> allBrands = selectBrandsByCategoryIds(categoryIds);
        System.out.println(allBrands);
        return allBrands.stream().collect(Collectors.groupingBy(BrandVO::getCategory_id));
    }
}

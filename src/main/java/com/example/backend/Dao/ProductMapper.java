package com.example.backend.Dao;

import com.example.backend.Entity.Product;
import com.example.backend.Entity.ProductPromotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper {
    @Select("SELECT * FROM product")
    List<Product> getAllProducts();

    @Select("SELECT * FROM product WHERE product_id = #{productId}")
    Product getProductById(Long productId);


    @Select("SELECT pp.*, p.product_name, p.price " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE pp.product_id = #{id} " +
            "AND NOW() BETWEEN pp.start_time AND pp.end_time ")
    List<ProductPromotion> getFlashSaleByProductId(Long id);

    @Select("SELECT * FROM product ORDER BY create_time DESC LIMIT #{num}")
    List<Product> getNewList(int num);

    /**
     * 根据分类和品牌名称获取产品列表，并支持分页和排序
     *
     * @param categoryName 产品分类名称
     * @param brandName    产品品牌名称
     * @param offset       偏移量，用于分页
     * @param limit        每页显示的记录数
     * @param sortField    排序字段
     * @param sortOrder    排序顺序（ASC 或 DESC）
     * @return 符合条件的产品列表
     */
    @Select("SELECT p.* FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "JOIN brand b ON p.brand_id = b.brand_id " +
            "WHERE c.category_name = #{categoryName} AND b.brand_name = #{brandName} " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{limit}")
    List<Product> getProductList(@Param("categoryName") String categoryName, @Param("brandName") String brandName,
                                 @Param("offset") int offset, @Param("limit") int limit,
                                 @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);

    @Select("SELECT p.* FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "JOIN brand b ON p.brand_id = b.brand_id " +
            "WHERE c.category_name = #{categoryName} AND b.brand_name != '苹果（Apple）' " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{limit}")
    List<Product> getOtherPhoneProductList(@Param("categoryName") String categoryName,
                                           @Param("offset") int offset, @Param("limit") int limit,
                                           @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);

    @Select("<script>" +
            "SELECT p.* FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "JOIN brand b ON p.brand_id = b.brand_id " +
            "WHERE c.category_name IN " +
            "<foreach item='categoryName' collection='categoryNames' open='(' separator=',' close=')'>" +
            "#{categoryName}" +
            "</foreach> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Product> getProductListByCategoryName(@Param("categoryNames") List<String> categoryNames,
                                               @Param("offset") int offset, @Param("limit") int limit,
                                               @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);

    @Select("<script>" +
            "SELECT p.* FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "JOIN brand b ON p.brand_id = b.brand_id " +
            "<where> " +
            "<if test='selectedCategory != null and selectedCategory != \"\"'> " +
            "AND c.category_name = #{selectedCategory} " +
            "</if> " +
            "<if test='selectedBrand != null and selectedBrand != \"\"'> " +
            "AND b.brand_name = #{selectedBrand} " +
            "</if> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR p.product_description LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Product> SearchProductList(Map<String, Object> params);

    @Select("SELECT COUNT(*) FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "JOIN brand b ON p.brand_id = b.brand_id " +
            "WHERE c.category_name = #{categoryName} AND b.brand_name != '苹果（Apple）'")
    int getOtherPhoneProductTotal(@Param("categoryName") String categoryName);

    @Select("SELECT COUNT(*) FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "JOIN brand b ON p.brand_id = b.brand_id " +
            "WHERE c.category_name = #{categoryName} AND b.brand_name = #{brandName}")
    int getProductTotal(@Param("categoryName") String categoryName, @Param("brandName") String brandName);

    @Select("<script>" +
            "SELECT COUNT(*) FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "WHERE c.category_name IN " +
            "<foreach item='categoryName' collection='categoryNames' open='(' separator=',' close=')'>" +
            "#{categoryName}" +
            "</foreach> " +
            "</script>")
    int getProductTotalByCategoryName(@Param("categoryNames") List<String> categoryNames);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "JOIN brand b ON p.brand_id = b.brand_id " +
            "<where> " +
            "<if test='selectedCategory != null and selectedCategory != \"\"'> " +
            "AND c.category_name = #{selectedCategory} " +
            "</if> " +
            "<if test='selectedBrand != null and selectedBrand != \"\"'> " +
            "AND b.brand_name = #{selectedBrand} " +
            "</if> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND (p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR p.product_description LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "</script>")
    int getSearchProductTotal(Map<String, Object> params);
}

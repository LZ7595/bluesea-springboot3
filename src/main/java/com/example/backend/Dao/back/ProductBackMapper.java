package com.example.backend.Dao.back;

import com.example.backend.Entity.Product;
import com.example.backend.Entity.back.ProductDetailsBack;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper

public interface ProductBackMapper {

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
            "AND (p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR p.product_description LIKE CONCAT('%', #{searchKeyword}, '%') OR p.product_id LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Product> SearchProductList(Map<String, Object> params);

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
            "AND (p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR p.product_description LIKE CONCAT('%', #{searchKeyword}, '%') OR p.product_id LIKE CONCAT('%', #{searchKeyword}, '%')) " +
            "</if> " +
            "</where> " +
            "</script>")
    int getSearchProductTotal(Map<String, Object> params);

    @Select("SELECT p.* , c.category_name ,b.brand_name FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "JOIN brand b ON p.brand_id = b.brand_id " +
            "WHERE product_id = #{productId} ")
    ProductDetailsBack getProductDetailsBackById(Long productId);

    @Update("UPDATE product p " +
            "JOIN category c ON c.category_name = #{product.category_name} " +
            "JOIN brand b ON b.brand_name = #{product.brand_name} " +
            "SET p.product_name = #{product.product_name}, " +
            "    p.status = #{product.status}, " +
            "    p.category_id = c.category_id, " +
            "    p.brand_id = b.brand_id, " +
            "    p.product_description = #{product.product_description}, " +
            "    p.price = #{product.price}, " +
            "    p.quality = #{product.quality}, " +
            "    p.stock = #{product.stock}, " +
            "    p.recommended = #{product.recommended}, " +
            "    p.sales_volume = #{product.sales_volume}, " +
            "    p.update_time = NOW() " +
            "WHERE p.product_id = #{product.product_id}")
    int updateProduct(@Param("product") ProductDetailsBack product);



    @Insert("INSERT INTO product (product_name, status, category_id, brand_id, quality, product_description, price, stock, recommended, sales_volume, create_time, update_time) " +
            "SELECT #{product.product_name}, #{product.status}, c.category_id, b.brand_id, #{product.quality}, #{product.product_description}, #{product.price}, #{product.stock}, #{product.recommended} , #{product.sales_volume}, NOW(), NOW() " +
            "FROM category c " +
            "JOIN brand b ON b.brand_name = #{product.brand_name} " +
            "WHERE c.category_name = #{product.category_name} ")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "product_id", keyColumn = "product_id")
    int addProduct(@Param("product") ProductDetailsBack product);

    @Delete("DELETE FROM product WHERE product_id = #{productId}")
    int deleteProduct(Long productId);


    @Delete("<script>" +
            "DELETE FROM product " +
            "WHERE product_id IN " +
            "<foreach item='item' index='index' collection='productIdList' " +
            "open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int deleteProductMore(@Param("productIdList") List<Long> productIdList);
    /**
     * 根据产品 ID 查询所有图片链接
     *
     * @param productId 产品 ID
     * @return 图片链接列表
     */
    @Select("SELECT image_url FROM productimage WHERE product_id = #{productId}")
    List<String> getImageUrlsByProductId(Long productId);

    /**
     * 插入新的图片链接
     *
     * @param imageUrl  图片链接
     * @param productId 产品 ID
     */
    @Insert("INSERT INTO productimage (product_id, image_url) VALUES (#{productId}, #{imageUrl})")
    void insertProductImage(@Param("imageUrl") String imageUrl, @Param("productId") Long productId);

    /**
     * 根据产品 ID 和图片链接删除记录
     *
     * @param imageUrl  图片链接
     * @param productId 产品 ID
     */
    @Delete("DELETE FROM productimage WHERE product_id = #{productId} AND image_url = #{imageUrl}")
    void deleteProductImage(@Param("imageUrl") String imageUrl, @Param("productId") Long productId);
}

package com.example.backend.Dao;

import com.example.backend.Entity.Product;
import com.example.backend.Entity.ProductDetails;
import com.example.backend.Entity.ProductPayInfo;
import com.example.backend.Entity.ProductPromotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper {
    @Select("SELECT * FROM product")
    List<Product> getAllProducts();

    @Select("SELECT * FROM product WHERE product_id = #{productId}")
    Product getProductById(Long productId);

    @Update("UPDATE product SET stock = #{stock}, sales_volume = #{sales_volume} WHERE product_id = #{product_id}")
    void updateProductStockAndSales(Product product);


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

    /**
     * 查询商品基本信息
     * @param productId 商品 ID
     * @return 商品详情
     */
    @Select("SELECT p.product_id, p.product_name, p.price " +
            "FROM product p " +
            "WHERE p.product_id = #{productId}")
    ProductPayInfo getProductDetails(@Param("productId") Long productId);

    /**
     * 查询用户之前使用过的商品促销 ID
     * @param userId 用户 ID
     * @param productId 商品 ID
     * @return 促销 ID 列表
     */
    @Select("SELECT oi.promotion_id " +
            "FROM order_item oi " +
            "JOIN `order` o ON oi.order_id = o.order_id " +
            "WHERE o.user_id = #{userId} AND oi.product_id = #{productId} AND o.order_status != 'CANCELLED'")
    List<Integer> getUserUsedPromotions(@Param("userId") Integer userId, @Param("productId") Long productId);

    /**
     * 查询商品当前可用的促销信息
     * @param userId 用户 ID
     * @param productId 商品 ID
     * @return 可用促销信息列表
     */
    @Select("SELECT * " +
            "FROM productpromotion " +
            "WHERE product_id = #{productId} " +
            "  AND start_time <= NOW() " +
            "  AND (end_time IS NULL OR end_time >= NOW()) " +
            "  AND promotion_stock > 0 " +
            "  AND promotion_id NOT IN (SELECT oi.promotion_id " +
            "                           FROM order_item oi " +
            "                           JOIN `order` o ON oi.order_id = o.order_id " +
            "                           WHERE o.user_id = #{userId} AND oi.product_id = #{productId} AND o.order_status != 'CANCELLED')")
    List<ProductPromotion> getAvailablePromotions(@Param("userId") Integer userId, @Param("productId") Long productId);

}

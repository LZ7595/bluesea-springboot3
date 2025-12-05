package com.example.backend.Dao;

import com.example.backend.Model.Entity.Product;
import com.example.backend.Model.Vo.ProductPayInfo;
import com.example.backend.Model.Entity.ProductPromotion;
import com.example.backend.Model.Dto.PromotionUsedCountDTO;
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


    // 修复：<= 转义为 &lt;=
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
            "AND c.category_id = #{selectedCategory} " +
            "</if> " +
            "<if test='selectedBrand != null and selectedBrand != \"\"'> " +
            "AND b.brand_id = #{selectedBrand} " +
            "</if> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND ( " +
            // 原始完整关键词搜索
            "p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR " +
            "p.product_description LIKE CONCAT('%', #{searchKeyword}, '%') OR " +
            // 拆分单个字符搜索
            "<foreach collection='searchKeyword.split(\"\")' item='char' separator='OR'> " +
            "(p.product_name LIKE CONCAT('%', #{char}, '%') OR p.product_description LIKE CONCAT('%', #{char}, '%')) " +
            "</foreach> " +
            // 如果需要按词语拆分搜索，可以使用特定分隔符(如空格)
            "<if test='searchKeyword.contains(\" \")'> " +
            "<foreach collection='searchKeyword.split(\" \")' item='word' separator='OR'> " +
            "(p.product_name LIKE CONCAT('%', #{word}, '%') OR p.product_description LIKE CONCAT('%', #{word}, '%')) " +
            "</foreach> " +
            "</if> " +
            ") " +
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
            "SELECT COUNT(*) FROM product p " +
            "JOIN category c ON p.category_id = c.category_id " +
            "JOIN brand b ON p.brand_id = b.brand_id " +
            "<where> " +
            "<if test='selectedCategory != null and selectedCategory != \"\"'> " +
            "AND c.category_id = #{selectedCategory} " +
            "</if> " +
            "<if test='selectedBrand != null and selectedBrand != \"\"'> " +
            "AND b.brand_id = #{selectedBrand} " +
            "</if> " +
            "<if test='searchKeyword != null and searchKeyword != \"\"'> " +
            "AND ( " +
            // 原始完整关键词搜索
            "p.product_name LIKE CONCAT('%', #{searchKeyword}, '%') OR " +
            "p.product_description LIKE CONCAT('%', #{searchKeyword}, '%') OR " +
            // 拆分单个字符搜索
            "<foreach collection='searchKeyword.split(\"\")' item='char' separator='OR'> " +
            "(p.product_name LIKE CONCAT('%', #{char}, '%') OR p.product_description LIKE CONCAT('%', #{char}, '%')) " +
            "</foreach> " +
            // 如果需要按词语拆分搜索，可以使用特定分隔符(如空格)
            "<if test='searchKeyword.contains(\" \")'> " +
            "<foreach collection='searchKeyword.split(\" \")' item='word' separator='OR'> " +
            "(p.product_name LIKE CONCAT('%', #{word}, '%') OR p.product_description LIKE CONCAT('%', #{word}, '%')) " +
            "</foreach> " +
            "</if> " +
            ") " +
            "</if> " +
            "</where> " +
            "</script>")
    int getSearchProductTotal(Map<String, Object> params);

    /**
     * 查询商品基本信息
     *
     * @param productId 商品 ID
     * @return 商品详情
     */
    @Select("SELECT p.product_id, p.product_name, p.price " +
            "FROM product p " +
            "WHERE p.product_id = #{productId}")
    ProductPayInfo getProductDetails(@Param("productId") Long productId);

    /**
     * 查询用户之前使用过的商品促销 ID
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 促销 ID 列表
     */
    @Select("SELECT oi.promotion_id " +
            "FROM order_item oi " +
            "JOIN `order` o ON oi.order_id = o.order_id " +
            "WHERE o.user_id = #{userId} AND oi.product_id = #{productId} AND o.order_status != 'CANCELED'")
    List<Long> getUserUsedPromotions(@Param("userId") Integer userId, @Param("productId") Long productId);

    /**
     * 查询商品当前可用的促销信息（修复：<= 和 >= 转义）
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 可用促销信息列表
     */
    @Select("SELECT pp.*, p.price " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE pp.product_id = #{productId} " +
            " AND pp.start_time <= NOW() " +
            "  AND (pp.end_time IS NULL OR pp.end_time >= NOW())")
    List<ProductPromotion> getAvailablePromotions(
            @Param("userId") Integer userId,
            @Param("productId") Long productId);

    /**
     * 查询用户之前使用过的商品促销 ID（批量版）
     *
     * @param userId     用户 ID
     * @param productIds 商品 ID 列表
     * @return 促销 ID 列表
     */
    @Select("<script>" +
            "SELECT oi.promotion_id " +
            "FROM order_item oi " +
            "JOIN `order` o ON oi.order_id = o.order_id " +
            "WHERE o.user_id = #{userId} " +
            "  AND oi.product_id IN " +
            "    <foreach collection='productIds' item='item' open='(' separator=',' close=')'>" +
            "      #{item}" +
            "    </foreach> " +
            "  AND o.order_status != 'CANCELED'" +
            "</script>")
    List<Long> getUserUsedPromotionsBatch(
            @Param("userId") Integer userId,
            @Param("productIds") List<Long> productIds);

    /**
     * 查询商品当前可用的促销信息（批量版，含商品基础库存）（修复：<= 和 >= 转义）
     *
     * @param userId     用户ID
     * @param productIds 商品ID列表
     * @return 可用促销信息列表（含商品基础库存）
     */
    @Select("<script>" +
            "SELECT pp.*, p.price, p.stock AS product_stock " + // 新增 p.stock：商品基础库存
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE pp.product_id IN " +
            "    <foreach collection='productIds' item='item' open='(' separator=',' close=')'>" +
            "      #{item}" +
            "    </foreach> " +
            "  AND pp.start_time &lt;= NOW() " + // 修复：<= 转义为 &lt;=
            "  AND (pp.end_time IS NULL OR pp.end_time &gt;= NOW()) " + // 修复：>= 转义为 &gt;=
            "  AND pp.promotion_stock > 0 " + // 活动库存充足
            "  AND p.stock > 0 " + // 商品基础库存充足（新增）
            "</script>")
    // 移除原 NOT IN 子查询，改在Java代码中判断限购
    List<ProductPromotion> getAvailablePromotionsBatch(
            @Param("userId") Integer userId,
            @Param("productIds") List<Long> productIds);

    /**
     * 查询用户对每个促销的已使用次数（批量版）
     * @param userId     用户ID
     * @param productIds 商品ID列表（过滤范围）
     * @return Map<Long, Integer>  key：promotion_id（促销ID），value：已使用次数
     */
    /**
     * 按用户和商品ID，统计各优惠的使用次数（分组查询）
     *
     * @param userId     用户ID
     * @param productIds 商品ID列表
     * @return 分组结果列表（每个元素对应一个优惠的统计数据）
     */
    @Select("<script>" +
            "SELECT oi.promotion_id AS promotionId, COUNT(oi.promotion_id) AS usedCount " +
            "FROM order_item oi " +
            "JOIN `order` o ON oi.order_id = o.order_id " +
            "WHERE o.user_id = #{userId} " +
            "  AND oi.product_id IN " +
            "    <foreach collection='productIds' item='item' open='(' separator=',' close=')'>" +
            "      #{item}" +
            "    </foreach> " +
            "  AND o.order_status != 'CANCELED' " +
            "  AND oi.promotion_id IS NOT NULL " + // 过滤无促销的订单
            "GROUP BY oi.promotion_id " + // 按促销ID分组统计
            "</script>")
    List<PromotionUsedCountDTO> getPromotionUsedCountByUser(
            @Param("userId") Integer userId,
            @Param("productIds") List<Long> productIds);

    /**
     * 批量查询商品基础信息（含基础库存）
     *
     * @param productIds 商品ID列表
     * @return 商品列表
     */
    @Select("<script>" +
            "SELECT product_id, price, stock " +
            "FROM product " +
            "WHERE product_id IN " +
            "    <foreach collection='productIds' item='item' open='(' separator=',' close=')'>" +
            "      #{item}" +
            "    </foreach> " +
            "</script>")
    List<Product> getProductBaseInfoBatch(@Param("productIds") List<Long> productIds);

    /**
     * 未登录用户查询可用促销（修复：<= 和 >= 转义）
     */
    @Select("SELECT pp.*, p.price " +
            "FROM productpromotion pp " +
            "JOIN product p ON pp.product_id = p.product_id " +
            "WHERE pp.product_id = #{productId} " +
            "  AND pp.start_time <= NOW() " +
            "  AND (pp.end_time IS NULL OR pp.end_time >= NOW()) " +
            "  AND pp.promotion_stock > 0 " +
            "  AND p.stock > 0")
    List<ProductPromotion> getAvailablePromotionsForAnonymous(@Param("productId") Long productId);

    @Update("UPDATE product SET stock = stock + #{quantity} WHERE product_id = #{productId}")
    void updateProductStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
package com.example.backend.Dao;

import com.example.backend.Entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    // 添加商品到购物车
    @Insert("INSERT INTO shoppingcarts (user_id, product_id, quantity, is_selected) VALUES (#{user_id}, #{product_id}, #{quantity}, #{is_selected})")
    @Options(useGeneratedKeys = true, keyProperty = "cart_id")
    void addToCart(ShoppingCart shoppingCart);

    // 根据用户 ID 获取购物车列表
    @Select("SELECT s.* , p.product_name , p.price , p.stock " +
            "FROM shoppingcarts s " +
            "JOIN product p ON s.product_id = p.product_id " +
            "WHERE user_id = #{userId}")
    List<ShoppingCart> getCartListByUserId(Integer userId);

    // 根据购物车 ID 删除商品
    @Delete("DELETE FROM shoppingcarts WHERE cart_id = #{cartId}")
    void deleteFromCart(Integer cartId);

    // 更新购物车中商品的数量
    @Update("UPDATE shoppingcarts SET quantity = #{quantity} WHERE cart_id = #{cartId}")
    void updateQuantity(@Param("cartId") Integer cartId, @Param("quantity") Integer quantity);

    // 更新购物车中商品的选中状态
    @Update("UPDATE shoppingcarts SET is_selected = #{isSelected} WHERE cart_id = #{cartId}")
    void updateSelectedStatus(@Param("cartId") Integer cartId, @Param("isSelected") Integer isSelected);

    // 根据用户 ID 和商品 ID 获取购物车中的商品;
    @Select("SELECT * FROM shoppingcarts WHERE user_id = #{userId} AND product_id = #{productId}")
    Integer getCartByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
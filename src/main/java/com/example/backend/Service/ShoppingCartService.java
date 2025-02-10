package com.example.backend.Service;

import com.example.backend.Entity.Selected;
import com.example.backend.Entity.ShoppingCart;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ShoppingCartService {
    // 添加商品到购物车
    ResponseEntity<?> addToCart(ShoppingCart shoppingCart);

    // 根据用户 ID 获取购物车列表
    ResponseEntity<?> getCartListByUserId(Integer userId);

    // 根据购物车 ID 删除商品
    ResponseEntity<?> deleteFromCart(Integer cartId);

    // 更新购物车中商品的数量
    void updateQuantity(Integer cartId, Integer quantity);

    // 更新购物车中商品的选中状态
    void updateSelectedStatus(Integer cartId, Integer isSelected);

    void updateSelectedListStatus(List<Selected> SelectedList);
}

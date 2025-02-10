package com.example.backend.Controller;

import com.example.backend.Entity.Selected;
import com.example.backend.Entity.ShoppingCart;
import com.example.backend.Service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    // 添加商品到购物车
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody ShoppingCart shoppingCart) {
        return shoppingCartService.addToCart(shoppingCart);
    }

    // 根据用户 ID 获取购物车列表
    @GetMapping("/list/{userId}")
    public ResponseEntity<?> getCartListByUserId(@PathVariable Integer userId) {
        return shoppingCartService.getCartListByUserId(userId);
    }

    // 根据购物车 ID 删除商品
    @DeleteMapping("/delete/{cartId}")
    public ResponseEntity<?> deleteFromCart(@PathVariable Integer cartId) {
       return shoppingCartService.deleteFromCart(cartId);
    }

    // 更新购物车中商品的数量
    @PutMapping("/updateQuantity")
    public void updateQuantity(@RequestParam Integer cartId, @RequestParam Integer quantity) {
        shoppingCartService.updateQuantity(cartId, quantity);
    }

    // 更新购物车中商品的选中状态
    @PutMapping("/updateSelectedStatus")
    public void updateSelectedStatus(@RequestParam Integer cartId, @RequestParam Integer isSelected) {
        shoppingCartService.updateSelectedStatus(cartId, isSelected);
    }

    @PutMapping("/updateSelectedListStatus")
    public void updateSelectedListStatus(@RequestBody List<Selected> SelectedList) {
        shoppingCartService.updateSelectedListStatus(SelectedList);
    }
}
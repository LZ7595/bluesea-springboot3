package com.example.backend.Impl;

import com.example.backend.Dao.ProductImageMapper;
import com.example.backend.Dao.ProductMapper;
import com.example.backend.Dao.ShoppingCartMapper;
import com.example.backend.Entity.Enum.ErrorType;
import com.example.backend.Entity.ProductImage;
import com.example.backend.Entity.ProductPromotion;
import com.example.backend.Entity.Selected;
import com.example.backend.Entity.ShoppingCart;
import com.example.backend.Service.ShoppingCartService;
import com.example.backend.Utils.PromotionDiscountCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ProductMapper productMapper;
    @Override
    public ResponseEntity<?> addToCart(ShoppingCart shoppingCart) {
        Optional<Integer> result = Optional.ofNullable(shoppingCartMapper.getCartByUserIdAndProductId(shoppingCart.getUser_id(), shoppingCart.getProduct_id()));
        if (result.isPresent() && result.get() > 0) {
                return ResponseEntity.status(409).body(ErrorType.ITEM_ALREADY_IN_CART.toErrorResponse());
        } else if (!result.isPresent()) {
            shoppingCartMapper.addToCart(shoppingCart);
            return ResponseEntity.ok().body("添加成功");
        } else {
            return ResponseEntity.status(400).body(ErrorType.CART_ADD_FAILED.toErrorResponse());
        }
    }


    @Override
    public ResponseEntity<?> getCartListByUserId(Integer userId) {
        try {
            List<ShoppingCart> shoppingCartList = shoppingCartMapper.getCartListByUserId(userId);
            List<ShoppingCart> newShoppingCartList = shoppingCartList.stream().map(
                    cartProduct -> {
                        Long productId = cartProduct.getProduct_id();
                        ProductImage mainImage = productImageMapper.getProductMainImageByProductId(productId);
                        cartProduct.setProduct_main_image(mainImage.getImage_url());

                        ProductPromotion flashSale = productMapper.getFlashSaleByProductId(productId);
                        if (flashSale != null) {
                            BigDecimal discountPrice = PromotionDiscountCalculator.calculateDiscountPrice(flashSale);
                            cartProduct.setPrice(discountPrice);
                        }

                        return cartProduct;
                    }
            ).collect(Collectors.toList());
            return ResponseEntity.ok(newShoppingCartList);
        }catch (Exception e){
            return ResponseEntity.status(400).body(ErrorType.CART_SELECT_FAILED.toErrorResponse());
        }
    }

    @Override
    public ResponseEntity<?> deleteFromCart(Integer cartId) {
        shoppingCartMapper.deleteFromCart(cartId);
        return ResponseEntity.ok().body("删除成功");
    }

    @Override
    public void updateQuantity(Integer cartId, Integer quantity) {
        shoppingCartMapper.updateQuantity(cartId, quantity);
    }

    @Override
    public void updateSelectedStatus(Integer cartId, Integer isSelected) {
        shoppingCartMapper.updateSelectedStatus(cartId, isSelected);
    }

    @Override
    public void updateSelectedListStatus(List<Selected> SelectedList){
        for (Selected selected : SelectedList) {
            shoppingCartMapper.updateSelectedStatus(selected.getCartId(), selected.getIsSelected());
        }
    }
}

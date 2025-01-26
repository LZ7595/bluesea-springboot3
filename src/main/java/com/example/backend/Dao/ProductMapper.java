package com.example.backend.Dao;

import com.example.backend.Entity.FlashSale;
import com.example.backend.Entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {
    @Select("SELECT * FROM products")
    List<Product> getAllProducts();
    @Select("SELECT * FROM products WHERE product_id = #{id}")
    Product getProductById(Long id);

    @Select("SELECT * FROM flash_sales WHERE product_id = #{id} AND start_time <= NOW() AND end_time > NOW()")
    FlashSale getFlashSaleByProductId(Long id);

    @Select("SELECT * FROM products ORDER BY created_at DESC LIMIT #{num}")
    List<Product> getNewList(int num);

    @Select("SELECT fs.flash_sale_id, fs.product_id, p.product_name, p.price, fs.discount_price, fs.max_quantity, fs.sold_quantity, p.image_url, p.quality\n" +
            "FROM flash_sales fs\n" +
            "JOIN products p ON fs.product_id = p.product_id\n" +
            "WHERE fs.start_time <= NOW() AND fs.end_time >= NOW()\n" +
            "LIMIT #{num};\n")
    List<FlashSale> getFlashSalesList(int num);

    @Select("SELECT * FROM products WHERE category = '手机' AND brand = '苹果' ORDER BY created_at DESC LIMIT #{num}")
    List<Product> getApplePhoneProductList(int num);

    @Select("SELECT * FROM products WHERE category = '手机' AND brand != '苹果' ORDER BY created_at DESC LIMIT #{num}")
    List<Product> getOtherPhoneProductList(int num);
}

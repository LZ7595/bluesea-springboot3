package com.example.backend.Dao;

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

}

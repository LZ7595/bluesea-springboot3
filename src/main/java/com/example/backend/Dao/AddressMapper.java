package com.example.backend.Dao;

import com.example.backend.Entity.Address;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressMapper {
    @Insert("INSERT INTO addresses (user_id, recipient_name, phone, province, city, district, addressCascader, street_address, is_default) " +
            "VALUES (#{user_id}, #{recipient_name}, #{phone}, #{province}, #{city}, #{district}, #{addressCascader}, #{street_address}, #{is_default})")
    int insertAddress(Address address);

    @Select("SELECT * FROM addresses WHERE user_id = #{userId}")
    List<Address> selectAddress(int userId);

    @Select("SELECT COUNT(*) FROM addresses WHERE user_id = #{userId}")
    int countAddress(int userId);

    @Update("UPDATE addresses SET is_default = false WHERE user_id = #{userId}")
    int updateDefaultFalse(int userId);

    @Update("UPDATE addresses SET is_default = true WHERE address_id = #{addressId}")
    int updateDefaultTrue(int addressId);

    @Delete("DELETE FROM addresses WHERE address_id = #{addressId}")
    int deleteAddress(int addressId);

    @Select("SELECT * FROM addresses WHERE address_id = #{addressId}")
    Address selectAddressById(int addressId);

    @Update("UPDATE addresses SET recipient_name = #{recipient_name}, phone = #{phone}, province = #{province}, city = #{city}, district = #{district}, addressCascader = #{addressCascader}, street_address = #{street_address}, is_default = #{is_default} WHERE address_id = #{address_id}")
    int updateAddress(Address address);

    @Select("SELECT a.* FROM addresses a JOIN `order` o ON a.address_id = o.address_id WHERE o.order_id = #{orderId}")
    Address getAddressByOrderId(Long orderId);
}
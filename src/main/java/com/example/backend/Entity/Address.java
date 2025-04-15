package com.example.backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private Integer address_id;
    private Integer user_id;
    private String recipient_name;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String addressCascader;
    private String street_address;
    private Boolean is_default;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

}

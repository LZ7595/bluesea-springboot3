package com.example.backend.Entity.back;

import com.example.backend.Entity.Banner;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BannerPageBack {
    List<Banner> banners;
    int total;
}

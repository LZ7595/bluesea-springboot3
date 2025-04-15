package com.example.backend.Controller;

import com.example.backend.Dao.BannerMapper;
import com.example.backend.Entity.Banner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/banner")
public class BannerController {

    @Autowired
    private BannerMapper bannerMapper;
    @GetMapping
    public List<Banner> getBanners() {
        return bannerMapper.getBanners();
    }
}

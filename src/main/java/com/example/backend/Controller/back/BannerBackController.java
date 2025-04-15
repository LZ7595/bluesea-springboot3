package com.example.backend.Controller.back;

import com.example.backend.Entity.Banner;
import com.example.backend.Service.back.BannerBackService;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/back/banner")
public class BannerBackController {
    @Autowired
    private BannerBackService bannerBackService;


    @PostMapping("/add")
    public String addBanner(@RequestBody Banner banner) {
        try {
            int result = bannerBackService.addBanner(banner);
            if (result > 0) {
                return "轮播图添加成功";
            } else {
                return "轮播图添加失败";
            }
        } catch (Exception e) {
            return "轮播图添加失败: " + e.getMessage();
        }
    }
    @GetMapping("/list")
    public ResponseEntity<?> getBannerList(@RequestParam(defaultValue = "create_time") String sortField,
                                           @RequestParam(defaultValue = "DESC") String sortOrder,
                                           @RequestParam(defaultValue = "1") int currentPage,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        return bannerBackService.getBannerList(sortField, sortOrder, currentPage, pageSize);
    }

    @Update("/changestatus")
    public String updateBannerStatus(@RequestParam Long id, @RequestParam boolean status) {
        try {
            int result = bannerBackService.updateBannerStatus(id, status);
            if (result > 0) {
                return "轮播图状态更新成功";
            } else {
                return "轮播图状态更新失败";
            }
        } catch (Exception e) {
            return "轮播图状态更新失败: " + e.getMessage();
        }
    }

    @PutMapping("/update")
    public String updateBanner(@RequestBody Banner banner) {
        try {
            int result = bannerBackService.updateBanner(banner);
            if (result > 0) {
                return "轮播图更新成功";
            } else {
                return "轮播图更新失败";
            }
        } catch (Exception e) {
            return "轮播图更新失败: " + e.getMessage();
        }
    }

    @DeleteMapping("/delete")
    public String deleteBanner(@RequestParam Long id) {
        try {
            int result = bannerBackService.deleteBanner(id);
            if (result > 0) {
                return "轮播图删除成功";
            } else {
                return "轮播图删除失败";
            }
        } catch (Exception e) {
            return "轮播图删除失败: " + e.getMessage();
        }
    }

    @DeleteMapping("/deletemore")
    public String deleteBanners(@RequestBody List<String> ids) {
        try {
            int result = bannerBackService.deleteBanners(ids);
            if (result > 0) {
                return "轮播图删除成功";
            } else {
                return "轮播图删除失败";
            }
        } catch (Exception e) {
            return "轮播图删除失败: " + e.getMessage();
        }
    }
}

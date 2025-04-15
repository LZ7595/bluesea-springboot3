package com.example.backend.Impl.back;

import com.example.backend.Dao.back.BannerBackMapper;
import com.example.backend.Entity.Banner;
import com.example.backend.Entity.back.BannerPageBack;
import com.example.backend.Service.back.BannerBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BannerBackServiceImpl implements BannerBackService {
    @Autowired
    private BannerBackMapper bannerBackMapper;

    @Override
    public int addBanner(Banner banner) {
        banner.setCreate_time(new Date());
        banner.setUpdate_time(new Date());
        return bannerBackMapper.insertBanner(banner);
    }

    @Override
    public ResponseEntity<?> getBannerList(String sortField, String sortOrder, int currentPage, int pageSize) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("sortField", sortField);
            params.put("sortOrder", sortOrder);
            params.put("offset", (currentPage - 1) * pageSize);
            params.put("pageSize", pageSize);
            List<Banner> bannerList = bannerBackMapper.getBannerList(params);
            int total = bannerBackMapper.countBanners();
            return ResponseEntity.ok().body(new BannerPageBack(bannerList, total));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取轮播图列表失败");
        }
    }

    @Override
    public int updateBannerStatus(Long id, boolean status) {
        return bannerBackMapper.updateBannerStatus(id, status);
    }

    @Override
    public int updateBanner(Banner banner) {
        banner.setUpdate_time(new Date());
        return bannerBackMapper.updateBanner(banner);
    }

    @Override
    public int deleteBanner(Long id) {
        return bannerBackMapper.deleteBanner(id);
    }

    @Override
    public int deleteBanners(List<String> ids) {
        return bannerBackMapper.deleteBanners(ids);
    }


}

package com.example.backend.Service.back;

import com.example.backend.Entity.Banner;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface BannerBackService {

    int addBanner(Banner banner);
    ResponseEntity<?> getBannerList(String sortField, String sortOrder, int currentPage, int pageSize);

    int updateBannerStatus(Long id, boolean status);

    int updateBanner(Banner banner);

    int deleteBanner(Long id);

    int deleteBanners(List<String> ids);
}

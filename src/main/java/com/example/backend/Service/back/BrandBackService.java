package com.example.backend.Service.back;

import com.example.backend.Entity.Brand;
import com.example.backend.Entity.BrandList;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface BrandBackService {

    Map<String, Object> SearchBrandList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    int updateBrand(Brand brand);

    int addBrand(Brand brand);

    int deleteBrand(Long brand_id);

    int deleteBrandMore(List<Long> brandIdList);

//    List<BrandList> getSelectList(String keyword);

    List<BrandList> getSelectList(Long categoryId, String keyword);
}

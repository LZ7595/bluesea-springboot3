package com.example.backend.Service.back;

import com.example.backend.Model.Entity.Brand;
import com.example.backend.Model.Vo.LabelList;

import java.util.List;
import java.util.Map;

public interface BrandBackService {

    Map<String, Object> SearchBrandList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize);

    int updateBrand(Brand brand);

    int addBrand(Brand brand);

    int deleteBrand(Long brand_id);

    int deleteBrandMore(List<Long> brandIdList);

//    List<BrandList> getSelectList(String keyword);

    List<LabelList> getSelectList(Long categoryId, String keyword);
}

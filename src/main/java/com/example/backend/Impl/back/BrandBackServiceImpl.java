package com.example.backend.Impl.back;

import com.example.backend.Dao.back.BrandBackMapper;
import com.example.backend.Entity.Brand;
import com.example.backend.Entity.BrandList;
import com.example.backend.Service.back.BrandBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class BrandBackServiceImpl implements BrandBackService {
    @Autowired
    private BrandBackMapper brandBackMapper;

    public Map<String, Object> SearchBrandList(String searchKeyword, String sortField, String sortOrder, int currentPage, int pageSize) {
        System.out.println(searchKeyword);
        Map<String, Object> params = new HashMap<>();
        params.put("searchKeyword", searchKeyword);
        params.put("sortField", sortField);
        params.put("sortOrder", sortOrder);
        params.put("offset", (currentPage - 1) * pageSize);
        params.put("pageSize", pageSize);
        List<Brand> brandList = brandBackMapper.SearchBrandList(params);
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("searchKeyword", searchKeyword);
        int total = brandBackMapper.getSearchBrandTotal(countParams);
        Map<String, Object> result = new HashMap<>();
        result.put("list", brandList);
        result.put("total", total);
        return result;
    }

    @Override
    public int updateBrand(Brand brand) {
        try {
            int rowsAffected = brandBackMapper.updateBrand(brand);
            System.out.println("更新产品信息，受影响的行数: " + rowsAffected);

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            handleException(e);
            return 0;
        }
    }

    @Override
    public int addBrand(Brand brand) {
        try {
            // 插入商品记录
            int result = brandBackMapper.addBrand(brand);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int deleteBrand(Long brand_id) {
        return brandBackMapper.deleteBrand(brand_id);
    }

    @Override
    public int deleteBrandMore(List<Long> brandIdList) {
        return brandBackMapper.deleteBrandMore(brandIdList);
    }

//    @Override
//    public List<BrandList> getSelectList(String keyword) {
//        return brandBackMapper.getBrandList(keyword);
//    }

    @Override
    public List<BrandList> getSelectList(Long categoryId, String keyword){
        return brandBackMapper.getBrandList(keyword,categoryId);
    }

    private void handleException(Exception e) {
        if (e instanceof org.springframework.dao.DataAccessException) {
            System.out.println("数据库访问异常: " + e.getMessage());
        } else {
            System.out.println("其他异常: " + e.getMessage());
        }
    }
}

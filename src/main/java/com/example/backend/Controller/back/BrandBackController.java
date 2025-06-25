package com.example.backend.Controller.back;

import com.example.backend.Entity.Brand;
import com.example.backend.Entity.BrandList;
import com.example.backend.Service.back.BrandBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/back/brand")
public class BrandBackController {

    @Autowired
    private BrandBackService brandBackService;
    @GetMapping("/search")
    public Map<String, Object> SearchBrandList(
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "create_time") String sortField,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return brandBackService.SearchBrandList(searchKeyword, sortField, sortOrder, currentPage, pageSize);
    }

    @PutMapping("/update")
    public String updateProduct(@RequestBody Brand brand) {
        int result = brandBackService.updateBrand(brand);
        if (result > 0) {
            return "品牌信息修改成功";
        } else {
            return "品牌信息修改失败";
        }
    }

    @PostMapping("/add")
    public String addBrand(@RequestBody Brand brand) {
        int result = brandBackService.addBrand(brand);
        if (result > 0) {
            return "品牌信息添加成功";
        } else {
            return "品牌信息添加失败";
        }
    }

    @DeleteMapping("/deleteone")
    public String deleteBrand(@RequestParam Long brand_id) {
        int result = brandBackService.deleteBrand(brand_id);
        if (result > 0) {
            return "品牌信息删除成功";
        } else {
            return "品牌信息删除失败";
        }
    }
    @DeleteMapping("/deletemore")
    public String deleteBrandMore(@RequestBody List<Long> brandIdList) {
        try {
            // 调用服务层方法处理删除逻辑
            brandBackService.deleteBrandMore(brandIdList);
            return "品牌删除成功";
        } catch (Exception e) {
            return "品牌删除失败: " + e.getMessage();
        }
    }
    @GetMapping("/list")
    public List<BrandList> getSelectList(@RequestParam(required = false) String keyword) {
        try {
            return brandBackService.getSelectList(keyword); // 服务层方法新增参数
        } catch (Exception e) {
            throw new RuntimeException("获取品牌列表失败: " + e.getMessage());
        }
    }
}

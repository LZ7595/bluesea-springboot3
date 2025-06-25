package com.example.backend.Controller.back;

import com.example.backend.Entity.CategoryBrand;
import com.example.backend.Service.back.CategoryBrandBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping( "/back/categoryBrand")
public class CategoryBrandBackController {
@Autowired
    private CategoryBrandBackService categoryBrandBackService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> findByParams(
            @RequestParam(required = false) Long category_id,
            @RequestParam(required = false) Long brand_id,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "") String sortField,
            @RequestParam(defaultValue = "") String sortOrder) {
        Map<String, Object> result = categoryBrandBackService.Search(category_id,brand_id, currentPage, pageSize, sortField, sortOrder);
        return ResponseEntity.ok(result);
    }
    @PutMapping("/deleteone")
    public String deleteBrand(@RequestParam Long id) {
        int result = categoryBrandBackService.deleteOne(id);
        if (result > 0) {
            return "关联信息删除成功";
        } else {
            return "关联信息删除失败";
        }
    }
    @PutMapping("/deletemore")
    public String deleteBrandMore(@RequestBody List<Long> idList) {
        try {
            // 调用服务层方法处理删除逻辑
            categoryBrandBackService.deleteMore(idList);
            return "关联删除成功";
        } catch (Exception e) {
            return "关联删除失败: " + e.getMessage();
        }
    }

    @PutMapping("/update")
    public String updateProduct(@RequestBody CategoryBrand relation) {
        int result = categoryBrandBackService.update(relation);
        if (result > 0) {
            return "关联信息修改成功";
        } else {
            return "关联信息修改失败";
        }
    }

    @PostMapping("/add")
    public String addBrand(@RequestBody CategoryBrand relation) {
        int result = categoryBrandBackService.add(relation);
        if (result > 0) {
            return "关联信息添加成功";
        } else {
            return "关联信息添加失败";
        }
    }

}

package com.example.backend.Controller.back;

import com.example.backend.Model.Vo.LabelList;
import com.example.backend.Model.Entity.Category;
import com.example.backend.Service.back.CategoryBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/back/category")
public class CategoryBackController {
    @Autowired
    private CategoryBackService categoryBackService;

    @GetMapping("/findByParentId/{parentId}")
    public ResponseEntity<Map<String, Object>> findByParentId(@PathVariable Long parentId,
                                                              @RequestParam(defaultValue = "1") int currentPage,
                                                              @RequestParam(defaultValue = "20") int pageSize,
                                                              @RequestParam(defaultValue = "") String sortField,
                                                              @RequestParam(defaultValue = "") String sortOrder) {
        Map<String, Object> result = categoryBackService.findByParentId(parentId, currentPage, pageSize, sortField, sortOrder);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addCategory(@RequestBody Category category) {
        String savedCategory = categoryBackService.addCategory(category);
        return ResponseEntity.ok(savedCategory);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateCategory(@RequestBody Category category) {
        String updatedCategory = categoryBackService.updateCategory(category);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/deleteone")
    public String deleteCategory(@RequestParam Long category_id) {
        int result = categoryBackService.deleteCategory(category_id);
        if (result > 0) {
            return "分类信息删除成功";
        } else {
            return "分类信息删除失败";
        }
    }

    @DeleteMapping("/deletemore")
    public String deleteCategoryMore(@RequestBody List<Long> categoryIdList) {
        try {
            // 调用服务层方法处理删除逻辑
            categoryBackService.deleteCategoryMore(categoryIdList);
            return "分类删除成功";
        } catch (Exception e) {
            return "分类删除失败: " + e.getMessage();
        }
    }
    @GetMapping("/list")
    public List<LabelList> getBrandList(@RequestParam(required = false) String keyword) {
        try {
            return categoryBackService.getSelectList(keyword, null); // 服务层方法新增参数
        } catch (Exception e) {
            throw new RuntimeException("获取分类列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/list/{brandId}")
    public List<LabelList> getBrandListByBrandId(@RequestParam(required = false) String keyword, @PathVariable Long brandId) {
        try {
            return categoryBackService.getSelectList(keyword, brandId); // 服务层方法新增参数
        } catch (Exception e) {
            throw new RuntimeException("获取分类列表失败: " + e.getMessage());
        }
    }
}

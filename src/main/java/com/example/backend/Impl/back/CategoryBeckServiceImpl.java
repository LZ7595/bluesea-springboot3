package com.example.backend.Impl.back;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.example.backend.Dao.back.CategoryBackMapper;
import com.example.backend.Entity.BrandList;
import com.example.backend.Entity.Category;
import com.example.backend.Service.back.CategoryBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryBeckServiceImpl implements CategoryBackService {

    @Autowired
    private CategoryBackMapper categoryBackMapper ;

    @Override
    public Map<String, Object> findByParentId(Long parentId, int currentPage, int pageSize, String sortField, String sortOrder) {
        int offset = (currentPage - 1) * pageSize;
        List<Category> categoryList = categoryBackMapper.selectByParentId(parentId, offset, pageSize, sortField, sortOrder);
        if (!CollectionUtils.isEmpty(categoryList)) {
            categoryList.forEach(item -> {
                int count = categoryBackMapper.countByParentId(item.getCategory_id());
                if (count > 0) {
                    item.setHasChildren(true);
                } else {
                    item.setHasChildren(false);
                }
            });
        }
        int total = categoryBackMapper.countAllByParentId(parentId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", categoryList);
        result.put("total", total);
        return result;
    }

    public String addCategory(Category category) {
        if (categoryBackMapper.insert(category) > 0) {
            return "添加成功";
        } else {
            return "添加失败";
        }
    }

    public String updateCategory(Category category) {
        if (categoryBackMapper.update(category) > 0) {
            return "更新成功";
        } else {
            return "更新失败";
        }
    }

    @Override
    public int deleteCategory(Long category_id) {
        return categoryBackMapper.deleteCategory(category_id);
    }

    @Override
    public int deleteCategoryMore(List<Long> categoryIdList) {
        return categoryBackMapper.deleteCategoryMore(categoryIdList);
    }

    @Override
    public List<BrandList> getSelectList(String keyword, Long categoryId) {
        return categoryBackMapper.getCategoryList(keyword, categoryId);
    }
}

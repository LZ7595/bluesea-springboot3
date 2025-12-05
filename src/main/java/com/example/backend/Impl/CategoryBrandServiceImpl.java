package com.example.backend.Impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.example.backend.Dao.CategoryBrandMapper;
import com.example.backend.Dao.CategoryMapper;
import com.example.backend.Model.Vo.BrandVO;
import com.example.backend.Model.Vo.CategoryBrandVO;
import com.example.backend.Service.CategoryBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class CategoryBrandServiceImpl implements CategoryBrandService {
    private static final Integer MORE_BRAND_ID = 0;
    private static final String MORE_BRAND_NAME = "更多";
    private static final String MORE_BRAND_LOGO = "/icon/more.png";
    private static final Integer MORE_CATEGORY_ID = 0;
    private static final int DEFAULT_LIMIT = 7; // type=1时默认截取7个品牌

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;

    @Override
    public List<CategoryBrandVO> getCategoryBrandList() {
        // 查询所有分类
        List<CategoryBrandVO> categoryList = categoryMapper.selectAll();

        System.out.println(categoryList.stream().map(CategoryBrandVO::getCategory_id).collect(Collectors.toList()));

        // 查询所有关联关系
        Map<Integer, List<BrandVO>> brandMap = categoryBrandMapper.selectBrandMapByCategoryIds(
                categoryList.stream().map(CategoryBrandVO::getCategory_id).collect(Collectors.toList())
        );

        // 填充品牌信息
        for (CategoryBrandVO category : categoryList) {
            List<BrandVO> brands = brandMap.getOrDefault(category.getCategory_id(), Collections.emptyList());
            category.setBrands(brands);
        }

        return categoryList;
    }

    @Override
    public CategoryBrandVO getBrandListByCategoryName(String categoryName, Integer limit, Integer type) {
        // 1. 核心参数校验：分类名称不能为空
        if (Objects.isNull(categoryName) || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空（categoryName）");
        }
        // 2. 初始化变量：目标分类列表、需排除的品牌名
        List<String> targetCategoryNames = new ArrayList<>();
        String excludeName = null;
        // 3. 处理limit默认值（仅type=1时生效）
        Integer finalLimit = (type == 1) ? Optional.ofNullable(limit).orElse(DEFAULT_LIMIT) : null;

        try {
            // -------------------------- 第一步：确定目标分类列表 --------------------------
            if (type == 1) {
                // type=1：按业务规则映射分类（如"国产"→"手机"，"生活"→多子分类）
                switch (categoryName.trim()) {
                    case "国产":
                        targetCategoryNames = Collections.singletonList("手机");
                        excludeName = "苹果"; // 国产分类排除"苹果"品牌
                        break;
                    case "生活":
                        targetCategoryNames = Arrays.asList("运动户外", "穿戴", "摄影摄像");
                        break;
                    default:
                        targetCategoryNames = Collections.singletonList(categoryName.trim());
                        break;
                }
            } else if (type == 2) {
                // type=2：直接使用传入的分类名称，不做映射，不排除品牌
                targetCategoryNames = Collections.singletonList(categoryName.trim());
            } else {
                // 未知type：默认按"不截取、不映射"处理（可根据业务调整）
                targetCategoryNames = Collections.singletonList(categoryName.trim());
                finalLimit = null; // 未知type时不截取
            }

            // 日志：打印当前查询规则（方便调试）
            System.out.printf(
                    "查询规则：type=%d，目标分类=%s，排除品牌=%s，截取数量=%s%n",
                    type, targetCategoryNames, excludeName, (finalLimit == null ? "不截取" : finalLimit)
            );

            // -------------------------- 第二步：查询分类ID --------------------------
            if (CollectionUtils.isEmpty(targetCategoryNames)) {
                System.err.println("目标分类列表为空，无法查询分类ID");
                return buildEmptyResultVO(categoryName); // 返回空结果VO（避免null）
            }
            List<Long> categoryIds = categoryMapper.getCategoryByName(targetCategoryNames);
            if (CollectionUtils.isEmpty(categoryIds)) {
                System.err.printf("未找到分类[%s]对应的ID%n", targetCategoryNames);
                return buildEmptyResultVO(categoryName);
            }

            // -------------------------- 第三步：查询品牌列表 --------------------------
            List<BrandVO> brandList = categoryBrandMapper.selectBrandsByCategoryId(categoryIds, excludeName);
            if (CollectionUtils.isEmpty(brandList)) {
                System.out.println("该分类下无可用品牌");
                return new CategoryBrandVO(categoryIds.get(0).intValue(), categoryName, Collections.emptyList());
            }

            // -------------------------- 第四步：品牌列表去重（统一去重，避免重复） --------------------------
            List<BrandVO> distinctBrandList = brandList.stream()
                    .filter(distinctByKey(BrandVO::getBrand_id)) // 按brand_id去重（同一品牌不重复出现）
                    .collect(Collectors.toList());

            // -------------------------- 第五步：按type处理品牌列表（核心逻辑） --------------------------
            List<BrandVO> resultBrandList;
            if (type == 1) {
                // type=1：截取前finalLimit个品牌 + 达标时加"更多"
                // 截取逻辑：最多取finalLimit个（不足则取全部）
                resultBrandList = distinctBrandList.stream()
                        .limit(finalLimit)
                        .collect(Collectors.toList());

                // 若截取后数量正好等于finalLimit，添加"更多"选项
                if (resultBrandList.size() == finalLimit) {
                    BrandVO moreBrand = new BrandVO();
                    moreBrand.setBrand_id(MORE_BRAND_ID);
                    moreBrand.setBrand_name(MORE_BRAND_NAME);
                    moreBrand.setLogo(MORE_BRAND_LOGO);
                    moreBrand.setCategory_id(MORE_CATEGORY_ID);
                    resultBrandList.add(moreBrand);
                }
            } else {
                // type=2 或 未知type：不截取，返回全部去重后的品牌（无"更多"选项）
                resultBrandList = distinctBrandList;
            }

            // -------------------------- 第六步：返回最终结果 --------------------------
            return new CategoryBrandVO(
                    categoryIds.get(0).intValue(), // 取第一个分类ID（多分类场景需确认业务逻辑）
                    categoryName,
                    resultBrandList
            );

        } catch (IllegalArgumentException e) {
            // 处理参数异常（如分类名称为空）：抛出自定义异常，让调用方感知
            System.err.println("参数错误：" + e.getMessage());
            throw e;
        } catch (Exception e) {
            // 处理业务异常（如数据库查询失败）：返回错误标记的VO
            System.err.println("查询品牌列表失败：" + e.getMessage());
            e.printStackTrace();
            return new CategoryBrandVO(-1, categoryName, Collections.emptyList()); // categoryId=-1标记失败
        }
    }


    // 辅助方法：根据指定字段去重
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
    private CategoryBrandVO buildEmptyResultVO(String categoryName) {
        return new CategoryBrandVO(-2, categoryName, Collections.emptyList()); // categoryId=-2标记"无分类ID"
    }

    public List<CategoryBrandVO> getCategoryList(){
        return categoryMapper.selectAll();
    }
}

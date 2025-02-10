package com.example.backend.Entity.ccc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.backend.Entity.ccc.BrandsMapper;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BrandsService {
    @Autowired
    private BrandsMapper brandsMapper;

    public Map<String, List<String>> getBrandsByCategory() {
        List<String> categoryNames = brandsMapper.getAllCategoryNames();
        Map<String, List<String>> result = new LinkedHashMap<>();

        for (String categoryName : categoryNames) {
            List<Brands> brands = brandsMapper.getBrandsByCategoryName(categoryName);
            List<String> brandNames = brands.stream().map(Brands::getBrand_name).collect(Collectors.toList());
            result.put(categoryName, brandNames);
        }

        return result;
    }
}

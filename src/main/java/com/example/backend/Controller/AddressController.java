package com.example.backend.Controller;

import com.example.backend.Entity.Address;
import com.example.backend.Service.AddressService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Value("${amap.key}")
    private String amapKey;

    @Value("${amap.api-url}")
    private String amapApi;

    // Redis缓存键
    private static final String REGION_DATA_CACHE_KEY = "region:data";
    // 缓存过期时间（24小时）
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Autowired
    private AddressService addressService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/addAddress")
    public String addAddress(@RequestBody Address address) {
        System.out.println("addAddress: " + address);
        boolean success = addressService.addAddress(address);
        return success ? "添加成功" : "添加失败";
    }

    @GetMapping("/getAddress/{userId}")
    public List<Address> getAddress(@PathVariable int userId) {
        return addressService.getAddress(userId);
    }

    @PutMapping("/setDefaultAddress")
    public String setDefaultAddress(@RequestBody Address address) {
        System.out.println("setDefaultAddress: " + address);
        boolean success = addressService.setDefaultAddress(address);
        return success ? "设置成功" : "设置失败";
    }

    @DeleteMapping("/deleteAddress/{addressId}")
    public String deleteAddress(@PathVariable int addressId) {
        boolean success = addressService.deleteAddress(addressId);
        return success ? "删除成功" : "删除失败";
    }

    @GetMapping("/getAddressDetail/{addressId}")
    public Address getAddressDetail(@PathVariable int addressId) {
        return addressService.getAddressDetail(addressId);
    }

    @PutMapping("/updateAddress")
    public String updateAddress(@RequestBody Address address) {
        boolean success = addressService.updateAddress(address);
        return success ? "更新成功" : "更新失败";
    }

    @GetMapping("/getRegionData")
    public ResponseEntity<?> getRegionData() {
        try {
            // 1. 先从Redis缓存获取数据
            String cachedRegionData = stringRedisTemplate.opsForValue().get(REGION_DATA_CACHE_KEY);
            if (cachedRegionData != null && !cachedRegionData.isEmpty()) {
                // 缓存命中，直接返回缓存数据
                return ResponseEntity.ok(cachedRegionData);
            }

            // 2. 缓存未命中，调用高德地图API获取数据
            RestTemplate restTemplate = new RestTemplate();
            Map<String, String> params = new HashMap<>();
            params.put("key", amapKey);
            params.put("subdistrict", "3");
            params.put("extensions", "base");

            ResponseEntity<String> response = restTemplate.getForEntity(
                    amapApi + "?key={key}&subdistrict={subdistrict}&extensions={extensions}",
                    String.class,
                    params
            );

            // 3. 将获取到的数据存入Redis，并设置过期时间
            if (response.getBody() != null) {
                stringRedisTemplate.opsForValue().set(
                        REGION_DATA_CACHE_KEY,
                        response.getBody(),
                        CACHE_EXPIRE_HOURS,
                        TimeUnit.HOURS
                );
            }

            // 4. 返回高德API的响应数据
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            // 异常处理：如果缓存和API都失败，尝试返回缓存（如果存在）
            String cachedData = stringRedisTemplate.opsForValue().get(REGION_DATA_CACHE_KEY);
            if (cachedData != null) {
                return ResponseEntity.ok(cachedData);
            }

            // 完全失败时返回错误信息
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "0");
            errorResponse.put("info", "获取地区数据失败：" + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}

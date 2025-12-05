package com.example.backend.Consumer;

import com.example.backend.Config.RabbitConfig;
import com.example.backend.Dao.ProductPromotionMapper;
import com.example.backend.Utils.RedisConstants;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ProductCacheConsumer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductPromotionMapper productPromotionMapper;  // 用于查询促销关联的商品


    // 监听商品缓存更新队列，处理缓存删除
    @RabbitListener(queues = RabbitConfig.PRODUCT_CACHE_UPDATE_QUEUE)
    public void handleProductCacheUpdate(Long id) {  // id可能是商品ID或促销ID
        if (id == null) {
            return;
        }

        // 情况1：如果是商品ID（直接删除该商品的缓存）
        // 商品基础信息缓存键
        String productCacheKey = RedisConstants.PRODUCT_DETAILS_KEY + id;
        stringRedisTemplate.delete(productCacheKey);
        // 商品促销信息缓存键（匹配所有用户和匿名用户）
        String promotionCachePattern = RedisConstants.PRODUCT_PROMOTIONS_KEY + id + ":*";
        Set<String> promotionCacheKeys = stringRedisTemplate.keys(promotionCachePattern);
        if (promotionCacheKeys != null && !promotionCacheKeys.isEmpty()) {
            stringRedisTemplate.delete(promotionCacheKeys);
        }
        System.out.println("已删除商品ID=" + id + "的缓存");


        // 情况2：如果是促销ID（查询关联商品并删除缓存）
        // 注：此处通过"是否存在关联商品"判断id类型（实际可通过消息体区分类型）
        List<Long> productIds = productPromotionMapper.getProductIdsByPromotionId(id);
        if (productIds != null && !productIds.isEmpty()) {
            for (Long productId : productIds) {
                stringRedisTemplate.delete(RedisConstants.PRODUCT_DETAILS_KEY + productId);
                Set<String> keys = stringRedisTemplate.keys(RedisConstants.PRODUCT_PROMOTIONS_KEY + productId + ":*");
                if (keys != null) {
                    stringRedisTemplate.delete(keys);
                }
                System.out.println("已删除促销ID=" + id + "关联的商品ID=" + productId + "的缓存");
            }
        }
    }
}
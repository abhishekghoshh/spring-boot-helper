package com.commercesphere.cart.service;
import com.commercesphere.cart.dto.WishlistItemDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class WishlistService {
    private final RedisTemplate<String, Object> redisTemplate;
    public WishlistService(RedisTemplate<String, Object> redisTemplate) { this.redisTemplate = redisTemplate; }

    @SuppressWarnings("unchecked")
    public List<WishlistItemDto> getWishlist(String userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("wishlist:" + userId);
        return entries.values().stream().map(v -> {
            Map<String, Object> m = (Map<String, Object>) v;
            return new WishlistItemDto((String) m.get("productId"), (String) m.get("productName"),
                    new BigDecimal(m.get("price").toString()), (String) m.get("imageUrl"));
        }).collect(Collectors.toList());
    }

    public void addItem(String userId, WishlistItemDto item) {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", item.productId()); map.put("productName", item.productName());
        map.put("price", item.price().doubleValue()); map.put("imageUrl", item.imageUrl());
        redisTemplate.opsForHash().put("wishlist:" + userId, item.productId(), map);
        redisTemplate.expire("wishlist:" + userId, 30, TimeUnit.DAYS);
    }

    public void removeItem(String userId, String productId) {
        redisTemplate.opsForHash().delete("wishlist:" + userId, productId);
    }

    public boolean isInWishlist(String userId, String productId) {
        return redisTemplate.opsForHash().hasKey("wishlist:" + userId, productId);
    }
}

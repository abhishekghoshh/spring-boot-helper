package com.commercesphere.cart.service;
import com.commercesphere.cart.dto.*;
import com.commercesphere.cart.entity.CartHistory;
import com.commercesphere.cart.repository.CartHistoryRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CartService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final CartHistoryRepository cartHistoryRepository;
    private final long cartTtlSeconds;
    private final ReentrantLock lock = new ReentrantLock();

    public CartService(RedisTemplate<String, Object> redisTemplate,
                       CartHistoryRepository cartHistoryRepository,
                       @org.springframework.beans.factory.annotation.Value("${app.cart.ttl-seconds:604800}") long cartTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.cartHistoryRepository = cartHistoryRepository;
        this.cartTtlSeconds = cartTtlSeconds;
    }

    public CartDto getCart(String cartId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("cart:" + cartId);
        List<CartItemDto> items = new ArrayList<>();
        entries.forEach((k, v) -> {
            if (v instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) v;
                items.add(new CartItemDto(
                        (String) itemMap.get("productId"), (String) itemMap.get("productName"),
                        new BigDecimal(itemMap.get("price").toString()), ((Number) itemMap.get("quantity")).intValue(),
                        (String) itemMap.get("imageUrl")));
            }
        });
        BigDecimal total = items.stream().map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = items.stream().mapToInt(CartItemDto::quantity).sum();
        return new CartDto(cartId, items, total, totalItems);
    }

    public CartDto addItem(String cartId, CartItemDto item) {
        lock.lock();
        try {
            String key = "cart:" + cartId;
            Map<String, Object> existing = new HashMap<>();
            Object existingItem = redisTemplate.opsForHash().get(key, item.productId());
            int qty = item.quantity();
            if (existingItem instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> e = (Map<String, Object>) existingItem;
                qty += ((Number) e.get("quantity")).intValue();
            }
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productId", item.productId()); itemMap.put("productName", item.productName());
            itemMap.put("price", item.price().doubleValue()); itemMap.put("quantity", qty); itemMap.put("imageUrl", item.imageUrl());
            redisTemplate.opsForHash().put(key, item.productId(), itemMap);
            redisTemplate.expire(key, cartTtlSeconds, TimeUnit.SECONDS);
        } finally { lock.unlock(); }
        return getCart(cartId);
    }

    public CartDto removeItem(String cartId, String productId) {
        redisTemplate.opsForHash().delete("cart:" + cartId, productId);
        return getCart(cartId);
    }

    public CartDto updateQuantity(String cartId, String productId, int quantity) {
        String key = "cart:" + cartId;
        Object obj = redisTemplate.opsForHash().get(key, productId);
        if (obj instanceof Map && quantity > 0) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = new HashMap<>((Map<String, Object>) obj);
            itemMap.put("quantity", quantity);
            redisTemplate.opsForHash().put(key, productId, itemMap);
        } else if (quantity <= 0) {
            redisTemplate.opsForHash().delete(key, productId);
        }
        return getCart(cartId);
    }

    public void clearCart(String cartId) {
        redisTemplate.delete("cart:" + cartId);
    }

    public CartDto mergeGuestCart(String guestId, String userId) {
        CartDto guestCart = getCart(guestId);
        for (CartItemDto item : guestCart.items()) {
            addItem(userId, item);
        }
        clearCart(guestId);
        return getCart(userId);
    }

    public void saveCartToHistory(String userId) {
        CartDto cart = getCart(userId);
        if (!cart.items().isEmpty()) {
            List<CartHistory.CartItem> items = cart.items().stream().map(i ->
                    CartHistory.CartItem.builder().productId(i.productId()).productName(i.productName())
                            .price(i.price()).quantity(i.quantity()).build()).toList();
            cartHistoryRepository.save(CartHistory.builder().userId(userId).items(items)
                    .totalAmount(cart.totalAmount()).createdAt(Instant.now()).build());
        }
    }
}

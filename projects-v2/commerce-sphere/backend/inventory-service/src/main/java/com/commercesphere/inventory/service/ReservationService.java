package com.commercesphere.inventory.service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Service
public class ReservationService {
    private final RedisTemplate<String, Object> redis; private final long ttl;
    public ReservationService(RedisTemplate<String, Object> redis, @org.springframework.beans.factory.annotation.Value("${app.reservation.ttl-seconds:1800}") long ttl) { this.redis = redis; this.ttl = ttl; }
    public String createReservation(String productId, String orderId, int quantity) {
        String id = UUID.randomUUID().toString();
        redis.opsForValue().set("reservation:" + id, productId + ":" + orderId + ":" + quantity, ttl, TimeUnit.SECONDS);
        return id;
    }
    public void expireReservation(String reservationId) { redis.delete("reservation:" + reservationId); }
}

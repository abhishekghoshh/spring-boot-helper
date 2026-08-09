package com.commercesphere.identity.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final long refreshExpirationMs;

    public RefreshTokenService(RedisTemplate<String, String> redisTemplate,
                               @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.redisTemplate = redisTemplate;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String createRefreshToken(String userId, String jwtRefreshToken) {
        String tokenId = UUID.randomUUID().toString();
        String key = "refresh_token:" + tokenId;
        redisTemplate.opsForValue().set(key, userId + ":" + jwtRefreshToken,
                refreshExpirationMs, TimeUnit.MILLISECONDS);
        return tokenId;
    }

    public boolean validateRefreshToken(String tokenId, String jwtRefreshToken) {
        String key = "refresh_token:" + tokenId;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) return false;
        String[] parts = stored.split(":", 2);
        return parts.length == 2 && parts[1].equals(jwtRefreshToken);
    }

    public void revokeRefreshToken(String tokenId) {
        String key = "refresh_token:" + tokenId;
        redisTemplate.delete(key);
    }

    public String getUserIdFromTokenId(String tokenId) {
        String key = "refresh_token:" + tokenId;
        String stored = redisTemplate.opsForValue().get(key);
        return stored != null ? stored.split(":", 2)[0] : null;
    }
}

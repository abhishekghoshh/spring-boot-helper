package com.commercesphere.order.config;
import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
@Configuration
public class RedisConfig {
    @Bean public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory f) {
        RedisTemplate<String, String> t = new RedisTemplate<>(); t.setConnectionFactory(f);
        t.setKeySerializer(new StringRedisSerializer()); t.setValueSerializer(new StringRedisSerializer());
        return t;
    }
}

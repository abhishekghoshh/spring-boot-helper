package io.github.abhishekghosh.user.repository;

import io.github.abhishekghosh.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.redis.core.RedisHash;

@RedisHash
public interface CacheableUserRepository extends MongoRepository<User, String> {
}

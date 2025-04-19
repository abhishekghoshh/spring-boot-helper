package io.github.abhishekghosh.user.repository;

import io.github.abhishekghosh.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // Custom query methods (if needed) can be defined here
}
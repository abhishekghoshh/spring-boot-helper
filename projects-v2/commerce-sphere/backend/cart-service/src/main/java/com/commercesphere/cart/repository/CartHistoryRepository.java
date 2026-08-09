package com.commercesphere.cart.repository;
import com.commercesphere.cart.entity.CartHistory;
import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface CartHistoryRepository extends MongoRepository<CartHistory, String> {
    Page<CartHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}

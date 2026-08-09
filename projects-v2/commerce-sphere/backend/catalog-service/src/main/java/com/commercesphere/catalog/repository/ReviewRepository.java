package com.commercesphere.catalog.repository;

import com.commercesphere.catalog.document.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    Page<Review> findByProductId(String productId, Pageable pageable);
    List<Review> findByUserId(String userId);
    Optional<Review> findByProductIdAndUserId(String productId, String userId);
    long countByProductId(String productId);
}

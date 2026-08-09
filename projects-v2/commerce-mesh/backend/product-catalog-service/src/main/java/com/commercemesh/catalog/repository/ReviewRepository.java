package com.commercemesh.catalog.repository;

import com.commercemesh.catalog.document.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    Page<Review> findByProductId(String productId, Pageable pageable);

    List<Review> findByProductId(String productId);

    List<Review> findByUserId(String userId);

    long countByProductId(String productId);
}

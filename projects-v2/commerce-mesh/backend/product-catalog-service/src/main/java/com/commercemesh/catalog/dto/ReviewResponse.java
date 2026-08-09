package com.commercemesh.catalog.dto;

import com.commercemesh.catalog.document.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        String id,
        String productId,
        String userId,
        String username,
        int rating,
        String title,
        String comment,
        boolean isVerifiedPurchase,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                review.getUsername(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.isVerifiedPurchase(),
                review.getCreatedAt()
        );
    }
}

package com.commercemesh.catalog.dto;

import com.commercemesh.catalog.document.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ProductResponse(
        String id,
        String name,
        String description,
        String brand,
        String sku,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String currency,
        String categoryId,
        List<String> images,
        List<String> tags,
        Map<String, String> attributes,
        double averageRating,
        int reviewCount,
        boolean isActive,
        boolean isFeatured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getSku(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getCurrency() != null ? product.getCurrency() : "USD",
                product.getCategoryId(),
                product.getImages(),
                product.getTags(),
                product.getAttributes(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.isActive(),
                product.isFeatured(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

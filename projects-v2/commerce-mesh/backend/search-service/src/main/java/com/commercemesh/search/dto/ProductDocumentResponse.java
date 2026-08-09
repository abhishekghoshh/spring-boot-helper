package com.commercemesh.search.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductDocumentResponse(
        String id,
        String name,
        String description,
        String brand,
        String category,
        BigDecimal price,
        String image,
        double averageRating,
        int reviewCount,
        List<String> tags,
        boolean isActive,
        Instant createdAt,
        Float textScore
) {
}

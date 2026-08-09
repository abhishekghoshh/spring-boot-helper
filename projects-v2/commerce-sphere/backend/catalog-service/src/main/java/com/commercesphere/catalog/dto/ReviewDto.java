package com.commercesphere.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewDto(
        String id, String productId, String userId, String userName,
        @Min(1) @Max(5) int rating, @NotBlank String comment,
        java.time.Instant createdAt
) {}

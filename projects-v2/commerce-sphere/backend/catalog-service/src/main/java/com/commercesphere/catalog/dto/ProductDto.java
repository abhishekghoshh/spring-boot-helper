package com.commercesphere.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDto(
        String id, String sku, String name, String description,
        BigDecimal price, String currency, String categoryId,
        String categoryName, String brandId, String brandName,
        List<String> imageUrls, boolean active, boolean featured,
        java.time.Instant createdAt, java.time.Instant updatedAt
) {}

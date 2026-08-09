package com.commercesphere.catalog.dto;

import java.math.BigDecimal;

public record ProductSearchRequest(
        String query, String categoryId, String brandId,
        BigDecimal minPrice, BigDecimal maxPrice,
        Boolean active, Boolean featured,
        int page, int size, String sortBy, String sortDir
) {
    public ProductSearchRequest {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (sortBy == null) sortBy = "createdAt";
        if (sortDir == null) sortDir = "desc";
    }
}

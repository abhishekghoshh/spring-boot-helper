package com.commercemesh.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SearchRequest(
        String keyword,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        @Min(0) Double minRating,
        SortBy sortBy,
        @Min(0) int page,
        @Min(1) @Max(100) int size
) {
    public SearchRequest {
        if (sortBy == null) {
            sortBy = SortBy.RELEVANCE;
        }
        if (size == 0) {
            size = 20;
        }
    }

    public enum SortBy {
        RELEVANCE,
        PRICE_ASC,
        PRICE_DESC,
        RATING,
        NEWEST
    }
}

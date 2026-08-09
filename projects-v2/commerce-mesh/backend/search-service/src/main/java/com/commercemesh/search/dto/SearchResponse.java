package com.commercemesh.search.dto;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        List<ProductDocumentResponse> results,
        long totalResults,
        int page,
        int size,
        int totalPages,
        Map<String, Long> facets,
        Map<String, Long> priceRanges
) {
}

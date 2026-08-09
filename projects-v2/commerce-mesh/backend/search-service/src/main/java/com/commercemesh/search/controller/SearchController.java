package com.commercemesh.search.controller;

import com.commercemesh.search.document.ProductDocument;
import com.commercemesh.search.dto.AutoCompleteResponse;
import com.commercemesh.search.dto.SearchRequest;
import com.commercemesh.search.dto.SearchResponse;
import com.commercemesh.search.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/v1/search — public, full-text search with optional filters.
     */
    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SearchRequest.SortBy sort = parseSortBy(sortBy);

        SearchRequest request = new SearchRequest(
                keyword, category, minPrice, maxPrice, minRating, sort, page, size
        );

        return ResponseEntity.ok(searchService.search(request));
    }

    /**
     * GET /api/v1/search/autocomplete — public, returns top 10 name suggestions.
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<AutoCompleteResponse> autocomplete(@RequestParam String keyword) {
        return ResponseEntity.ok(searchService.autocomplete(keyword));
    }

    /**
     * POST /api/v1/search/reindex — ADMIN only, triggers full reindex of product data.
     */
    @PostMapping("/reindex")
    public ResponseEntity<Void> reindexAll(@Valid @RequestBody List<ProductDocument> products) {
        searchService.reindexAll(products);
        return ResponseEntity.ok().build();
    }

    private SearchRequest.SortBy parseSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return SearchRequest.SortBy.RELEVANCE;
        }
        return switch (sortBy.toLowerCase()) {
            case "price_asc" -> SearchRequest.SortBy.PRICE_ASC;
            case "price_desc" -> SearchRequest.SortBy.PRICE_DESC;
            case "rating" -> SearchRequest.SortBy.RATING;
            case "newest" -> SearchRequest.SortBy.NEWEST;
            default -> SearchRequest.SortBy.RELEVANCE;
        };
    }
}

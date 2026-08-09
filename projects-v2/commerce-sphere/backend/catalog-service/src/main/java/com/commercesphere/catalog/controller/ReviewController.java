package com.commercesphere.catalog.controller;

import com.commercesphere.catalog.dto.PagedResponse;
import com.commercesphere.catalog.dto.ReviewDto;
import com.commercesphere.catalog.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/reviews")
@Tag(name = "Reviews", description = "Product review endpoints")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get reviews for a product")
    public ResponseEntity<PagedResponse<ReviewDto>> getByProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getByProduct(productId, page, size));
    }

    @PostMapping
    @Operation(summary = "Create a review")
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody ReviewDto dto) {
        return ResponseEntity.ok(reviewService.create(dto));
    }
}

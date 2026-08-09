package com.commercemesh.catalog.controller;

import com.commercemesh.catalog.dto.PageResponse;
import com.commercemesh.catalog.dto.ReviewRequest;
import com.commercemesh.catalog.dto.ReviewResponse;
import com.commercemesh.catalog.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/v1/products/{productId}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, page, size));
    }

    @PostMapping("/api/v1/products/{productId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable String productId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        String username = authentication.getName();
        ReviewResponse response = reviewService.addReview(productId, request, userId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/v1/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(reviewService.updateReview(id, request, userId));
    }

    @DeleteMapping("/api/v1/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(@PathVariable String id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_SUPER_ADMIN"));

        if (isAdmin) {
            reviewService.deleteReviewAsAdmin(id);
        } else {
            String userId = authentication.getName();
            reviewService.deleteReview(id, userId);
        }
        return ResponseEntity.noContent().build();
    }
}

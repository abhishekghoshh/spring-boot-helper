package com.commercesphere.catalog.service;

import com.commercesphere.catalog.document.Review;
import com.commercesphere.catalog.dto.PagedResponse;
import com.commercesphere.catalog.dto.ReviewDto;
import com.commercesphere.catalog.repository.ProductRepository;
import com.commercesphere.catalog.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    public PagedResponse<ReviewDto> getByProduct(String productId, int page, int size) {
        Page<Review> result = reviewRepository.findByProductId(productId, PageRequest.of(page, size));
        return new PagedResponse<>(result.getContent().stream().map(this::toDto).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    public ReviewDto create(ReviewDto dto) {
        productRepository.findById(dto.productId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        reviewRepository.findByProductIdAndUserId(dto.productId(), dto.userId())
                .ifPresent(r -> { throw new RuntimeException("Already reviewed"); });

        Review review = Review.builder()
                .productId(dto.productId()).userId(dto.userId()).userName(dto.userName())
                .rating(dto.rating()).comment(dto.comment())
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        return toDto(reviewRepository.save(review));
    }

    private ReviewDto toDto(Review r) {
        return new ReviewDto(r.getId(), r.getProductId(), r.getUserId(), r.getUserName(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}

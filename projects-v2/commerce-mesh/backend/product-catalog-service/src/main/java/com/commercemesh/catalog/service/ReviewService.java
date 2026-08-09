package com.commercemesh.catalog.service;

import com.commercemesh.catalog.document.Product;
import com.commercemesh.catalog.document.Review;
import com.commercemesh.catalog.dto.PageResponse;
import com.commercemesh.catalog.dto.ReviewRequest;
import com.commercemesh.catalog.dto.ReviewResponse;
import com.commercemesh.catalog.exception.ResourceNotFoundException;
import com.commercemesh.catalog.repository.ProductRepository;
import com.commercemesh.catalog.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    public ReviewResponse addReview(String productId, ReviewRequest request, String userId, String username) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Review review = new Review();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setUsername(username);
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment());

        Review saved = reviewRepository.save(review);

        recalculateAverageRating(productId);

        return ReviewResponse.from(saved);
    }

    public PageResponse<ReviewResponse> getReviewsByProduct(String productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByProductId(productId, pageable);

        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(ReviewResponse::from)
                .toList();

        return new PageResponse<>(
                responses,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages()
        );
    }

    public ReviewResponse updateReview(String id, ReviewRequest request, String userId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("You can only update your own reviews");
        }

        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment());

        Review saved = reviewRepository.save(review);

        recalculateAverageRating(review.getProductId());

        return ReviewResponse.from(saved);
    }

    public ReviewResponse getReviewById(String id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        return ReviewResponse.from(review);
    }

    public void deleteReview(String id, String userId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("You can only delete your own reviews");
        }

        String productId = review.getProductId();
        reviewRepository.delete(review);

        recalculateAverageRating(productId);
    }

    public void deleteReviewAsAdmin(String id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        String productId = review.getProductId();
        reviewRepository.delete(review);

        recalculateAverageRating(productId);
    }

    private void recalculateAverageRating(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        List<Review> reviews = reviewRepository.findByProductId(productId);
        int count = reviews.size();

        if (count == 0) {
            product.setAverageRating(0.0);
            product.setReviewCount(0);
        } else {
            double sum = reviews.stream().mapToInt(Review::getRating).sum();
            double avg = sum / count;
            // Round to 1 decimal place
            avg = Math.round(avg * 10.0) / 10.0;
            product.setAverageRating(avg);
            product.setReviewCount(count);
        }

        productRepository.save(product);
    }
}

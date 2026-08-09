package com.commercemesh.cart.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.commercemesh.cart.dto.WishlistItemResponse;
import com.commercemesh.cart.dto.WishlistResponse;
import com.commercemesh.cart.model.Wishlist;
import com.commercemesh.cart.model.WishlistItem;
import com.commercemesh.cart.repository.WishlistRepository;

@Service
public class WishlistService {

    private static final Logger log = LoggerFactory.getLogger(WishlistService.class);

    private final WishlistRepository wishlistRepository;

    public WishlistService(WishlistRepository wishlistRepository) {
        this.wishlistRepository = wishlistRepository;
    }

    /**
     * Returns the wishlist for a given user. Creates one if it does not exist.
     */
    public WishlistResponse getWishlist(String userId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> wishlistRepository.save(new Wishlist(userId)));
        return toResponse(wishlist);
    }

    /**
     * Adds an item to the wishlist. Does nothing if the product is already in the wishlist.
     */
    public WishlistResponse addToWishlist(String userId, String productId, String name,
                                          java.math.BigDecimal price, String image) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> new Wishlist(userId));

        boolean alreadyPresent = wishlist.getItems().stream()
                .anyMatch(i -> i.getProductId().equals(productId));

        if (!alreadyPresent) {
            wishlist.getItems().add(new WishlistItem(productId, name, price, image, Instant.now()));
            wishlist.setUpdatedAt(Instant.now());
            wishlist = wishlistRepository.save(wishlist);
            log.info("Added product {} to wishlist for user {}", productId, userId);
        }

        return toResponse(wishlist);
    }

    /**
     * Removes an item from the wishlist by productId.
     */
    public WishlistResponse removeFromWishlist(String userId, String productId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseGet(() -> new Wishlist(userId));

        wishlist.getItems().removeIf(i -> i.getProductId().equals(productId));
        wishlist.setUpdatedAt(Instant.now());
        wishlist = wishlistRepository.save(wishlist);
        return toResponse(wishlist);
    }

    /**
     * Checks whether a product is in the user's wishlist.
     */
    public boolean isInWishlist(String userId, String productId) {
        return wishlistRepository.findByUserId(userId)
                .map(w -> w.getItems().stream().anyMatch(i -> i.getProductId().equals(productId)))
                .orElse(false);
    }

    private WishlistResponse toResponse(Wishlist wishlist) {
        return new WishlistResponse(
                wishlist.getId(),
                wishlist.getUserId(),
                wishlist.getItems().stream()
                        .map(i -> new WishlistItemResponse(i.getProductId(), i.getName(), i.getPrice(), i.getImage(), i.getAddedAt()))
                        .toList()
        );
    }
}

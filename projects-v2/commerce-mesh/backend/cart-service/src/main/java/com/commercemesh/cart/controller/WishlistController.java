package com.commercemesh.cart.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.commercemesh.cart.dto.WishlistResponse;
import com.commercemesh.cart.service.WishlistService;

@RestController
@RequestMapping("/api/v1/wishlist")
@PreAuthorize("isAuthenticated()")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist() {
        String userId = currentUserId();
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistResponse> addItem(@RequestParam String productId,
                                                    @RequestParam String name,
                                                    @RequestParam BigDecimal price,
                                                    @RequestParam(required = false, defaultValue = "") String image) {
        String userId = currentUserId();
        return ResponseEntity.ok(wishlistService.addToWishlist(userId, productId, name, price, image));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<WishlistResponse> removeItem(@PathVariable String productId) {
        String userId = currentUserId();
        return ResponseEntity.ok(wishlistService.removeFromWishlist(userId, productId));
    }

    @GetMapping("/items/{productId}")
    public ResponseEntity<Boolean> isInWishlist(@PathVariable String productId) {
        String userId = currentUserId();
        return ResponseEntity.ok(wishlistService.isInWishlist(userId, productId));
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

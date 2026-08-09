package com.commercemesh.cart.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commercemesh.cart.dto.AddToCartRequest;
import com.commercemesh.cart.dto.ApplyCouponRequest;
import com.commercemesh.cart.dto.CartResponse;
import com.commercemesh.cart.dto.UpdateCartItemRequest;
import com.commercemesh.cart.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        String userId = currentUserId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddToCartRequest request) {
        String userId = currentUserId();
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable String productId,
                                                   @Valid @RequestBody UpdateCartItemRequest request) {
        String userId = currentUserId();
        return ResponseEntity.ok(cartService.updateCartItem(userId, productId, request.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable String productId) {
        String userId = currentUserId();
        return ResponseEntity.ok(cartService.removeFromCart(userId, productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        String userId = currentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/coupon")
    public ResponseEntity<CartResponse> applyCoupon(@Valid @RequestBody ApplyCouponRequest request) {
        String userId = currentUserId();
        return ResponseEntity.ok(cartService.applyCoupon(userId, request.couponCode()));
    }

    /**
     * Extracts the authenticated user's id from the SecurityContext.
     * Assumes the JWT subject claim holds the userId.
     */
    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

package com.commercemesh.cart.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.commercemesh.cart.dto.AddToCartRequest;
import com.commercemesh.cart.dto.CartItemResponse;
import com.commercemesh.cart.dto.CartResponse;
import com.commercemesh.cart.exception.ResourceNotFoundException;
import com.commercemesh.cart.model.Cart;
import com.commercemesh.cart.model.CartItem;
import com.commercemesh.cart.repository.CartRepository;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    /**
     * Mock coupon discounts — key is coupon code (uppercased), value is discount percentage (0..100).
     */
    private static final Map<String, Integer> COUPONS = Map.of(
            "SAVE10", 10,
            "SAVE20", 20
    );

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    // -----------------------------------------------------------------------
    //  getCart
    // -----------------------------------------------------------------------

    @Cacheable(value = "carts", key = "#userId")
    public CartResponse getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
        return toResponse(cart);
    }

    // -----------------------------------------------------------------------
    //  addToCart
    // -----------------------------------------------------------------------

    @CacheEvict(value = "carts", key = "#userId")
    public CartResponse addToCart(String userId, AddToCartRequest request) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);

        if (cart == null) {
            cart = new Cart(userId);
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.productId()))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.quantity());
            item.setPrice(request.price());   // always update to latest price
            item.setName(request.name());
            item.setImage(request.image());
        } else {
            cart.getItems().add(new CartItem(
                    request.productId(),
                    request.name(),
                    request.price(),
                    request.quantity(),
                    request.image()
            ));
        }

        // Re-apply coupon discount if one is active (coupon was cleared by price changes)
        if (cart.getCouponCode() != null && !cart.getCouponCode().isBlank()) {
            applyDiscount(cart, cart.getCouponCode());
        }

        cart.recalculate();
        cart = cartRepository.save(cart);
        log.info("Cart updated for user {}: {} items", userId, cart.getItems().size());
        return toResponse(cart);
    }

    // -----------------------------------------------------------------------
    //  updateCartItem
    // -----------------------------------------------------------------------

    @CacheEvict(value = "carts", key = "#userId")
    public CartResponse updateCartItem(String userId, String productId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        if (quantity == 0) {
            cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        } else {
            CartItem item = cart.getItems().stream()
                    .filter(i -> i.getProductId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Item " + productId + " not found in cart for user: " + userId));
            item.setQuantity(quantity);
        }

        cart.recalculate();
        cart = cartRepository.save(cart);
        return toResponse(cart);
    }

    // -----------------------------------------------------------------------
    //  removeFromCart
    // -----------------------------------------------------------------------

    @CacheEvict(value = "carts", key = "#userId")
    public CartResponse removeFromCart(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        cart.recalculate();
        cart = cartRepository.save(cart);
        return toResponse(cart);
    }

    // -----------------------------------------------------------------------
    //  clearCart
    // -----------------------------------------------------------------------

    @CacheEvict(value = "carts", key = "#userId")
    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
        cart.getItems().clear();
        cart.setCouponCode(null);
        cart.setDiscount(BigDecimal.ZERO);
        cart.recalculate();
        cartRepository.save(cart);
    }

    // -----------------------------------------------------------------------
    //  applyCoupon
    // -----------------------------------------------------------------------

    @CacheEvict(value = "carts", key = "#userId")
    public CartResponse applyCoupon(String userId, String couponCode) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        if (couponCode == null || couponCode.isBlank()) {
            // remove coupon
            cart.setCouponCode(null);
            cart.setDiscount(BigDecimal.ZERO);
        } else {
            Integer pct = COUPONS.get(couponCode.strip().toUpperCase());
            if (pct == null) {
                throw new IllegalArgumentException("Invalid or unknown coupon code: " + couponCode);
            }
            applyDiscount(cart, couponCode.strip().toUpperCase());
        }

        cart.recalculate();
        cart = cartRepository.save(cart);
        return toResponse(cart);
    }

    // -----------------------------------------------------------------------
    //  helpers
    // -----------------------------------------------------------------------

    private void applyDiscount(Cart cart, String code) {
        cart.setCouponCode(code);
        Integer pct = COUPONS.get(code);
        if (pct == null) return;

        // Recalculate subtotal first so we discount the latest prices
        cart.recalculate();
        BigDecimal multiplier = BigDecimal.valueOf(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        cart.setDiscount(cart.getSubtotal().multiply(multiplier).setScale(2, RoundingMode.HALF_UP));
    }

    private CartResponse toResponse(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getItems().stream()
                        .map(i -> new CartItemResponse(i.getProductId(), i.getName(), i.getPrice(), i.getQuantity(), i.getImage()))
                        .toList(),
                cart.getCouponCode(),
                cart.getDiscount(),
                cart.getSubtotal(),
                cart.getTotal()
        );
    }
}

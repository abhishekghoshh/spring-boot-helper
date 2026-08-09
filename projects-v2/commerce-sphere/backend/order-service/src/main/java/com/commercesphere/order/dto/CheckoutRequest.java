package com.commercesphere.order.dto;
import jakarta.validation.constraints.NotBlank;
public record CheckoutRequest(@NotBlank String cartId, @NotBlank String userId, @NotBlank String userEmail, @NotBlank String shippingAddress) {}

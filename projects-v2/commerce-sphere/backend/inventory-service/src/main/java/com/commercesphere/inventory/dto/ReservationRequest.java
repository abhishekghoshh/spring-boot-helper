package com.commercesphere.inventory.dto;
import jakarta.validation.constraints.Min;import jakarta.validation.constraints.NotBlank;
public record ReservationRequest(@NotBlank String productId, @NotBlank String orderId, @Min(1) int quantity) {}

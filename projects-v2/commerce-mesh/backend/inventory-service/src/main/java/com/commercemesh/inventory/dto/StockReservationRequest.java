package com.commercemesh.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockReservationRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Warehouse ID is required")
        Long warehouseId,

        @NotNull(message = "Order ID is required")
        Long orderId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {}

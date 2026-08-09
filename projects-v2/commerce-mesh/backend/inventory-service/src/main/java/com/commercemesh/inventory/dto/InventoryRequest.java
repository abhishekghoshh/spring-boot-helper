package com.commercemesh.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Warehouse ID is required")
        Long warehouseId,

        @Min(value = 0, message = "Quantity must be non-negative")
        int quantity,

        @Min(value = 0, message = "Reorder level must be non-negative")
        int reorderLevel,

        @Min(value = 0, message = "Reorder quantity must be non-negative")
        int reorderQuantity
) {}

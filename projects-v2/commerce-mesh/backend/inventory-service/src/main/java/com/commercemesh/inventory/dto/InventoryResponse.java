package com.commercemesh.inventory.dto;

import java.time.Instant;
import java.time.LocalDate;

public record InventoryResponse(
        Long id,
        Long productId,
        Long warehouseId,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        int reorderLevel,
        int reorderQuantity,
        LocalDate lastRestockDate,
        Instant createdAt,
        Instant updatedAt
) {}

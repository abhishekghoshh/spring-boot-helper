package com.commercemesh.inventory.dto;

import com.commercemesh.inventory.entity.ReservationStatus;
import java.time.Instant;

public record StockReservationResponse(
        Long id,
        Long productId,
        Long warehouseId,
        Long orderId,
        int quantity,
        ReservationStatus status,
        Instant reservedUntil,
        Instant createdAt
) {}

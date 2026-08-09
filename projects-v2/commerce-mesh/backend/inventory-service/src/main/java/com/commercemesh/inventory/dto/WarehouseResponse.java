package com.commercemesh.inventory.dto;

import java.time.Instant;

public record WarehouseResponse(
        Long id,
        String name,
        String location,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}

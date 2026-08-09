package com.commercemesh.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record WarehouseRequest(
        @NotBlank(message = "Warehouse name is required")
        String name,

        @NotBlank(message = "Warehouse location is required")
        String location,

        boolean isActive
) {}

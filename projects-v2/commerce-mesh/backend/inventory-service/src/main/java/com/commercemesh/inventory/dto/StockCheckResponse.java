package com.commercemesh.inventory.dto;

public record StockCheckResponse(
        boolean available,
        String message
) {}

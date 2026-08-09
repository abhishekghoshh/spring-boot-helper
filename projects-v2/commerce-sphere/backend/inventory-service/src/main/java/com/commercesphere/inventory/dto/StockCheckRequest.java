package com.commercesphere.inventory.dto;
import jakarta.validation.constraints.Min;import jakarta.validation.constraints.NotBlank;
public record StockCheckRequest(@NotBlank String productId, @Min(1) int quantity) {}

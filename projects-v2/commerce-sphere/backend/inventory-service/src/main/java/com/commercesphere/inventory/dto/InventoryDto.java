package com.commercesphere.inventory.dto;
public record InventoryDto(String id, String productId, String productName, String warehouseId, String warehouseName, int quantity, int reservedQuantity, int availableQuantity) {}

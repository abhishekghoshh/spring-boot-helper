package com.commercemesh.order.exception;

public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(Long productId, int requested, int available) {
        super(String.format("Insufficient stock for product %d: requested %d, available %d",
                productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public InsufficientStockException(Long productId, int requested) {
        super(String.format("Insufficient stock for product %d: requested %d", productId, requested));
        this.productId = productId;
        this.requested = requested;
        this.available = 0;
    }

    public Long getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}

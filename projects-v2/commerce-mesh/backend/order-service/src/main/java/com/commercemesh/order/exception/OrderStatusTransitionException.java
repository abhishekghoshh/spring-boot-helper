package com.commercemesh.order.exception;

import com.commercemesh.order.model.OrderStatus;

public class OrderStatusTransitionException extends RuntimeException {

    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    public OrderStatusTransitionException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super(String.format("Invalid order status transition from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public OrderStatusTransitionException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getTargetStatus() {
        return targetStatus;
    }
}

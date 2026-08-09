package com.commercesphere.order.event;
import com.commercesphere.order.document.Order;
public record OrderCancelledEvent(Order order) {}

package com.commercesphere.order.event;
import com.commercesphere.order.document.Order;
public record OrderPlacedEvent(Order order) {}

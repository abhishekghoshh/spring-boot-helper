package com.commercesphere.order.listener;
import com.commercesphere.order.event.*;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    @Async @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) { log.info("Order placed: {}", event.order().getOrderNumber()); }
    @Async @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) { log.info("Order cancelled: {}", event.order().getOrderNumber()); }
}

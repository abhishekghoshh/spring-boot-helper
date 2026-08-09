package com.commercemesh.notification.consumer;

import com.commercemesh.notification.enumeration.NotificationChannel;
import com.commercemesh.notification.enumeration.NotificationType;
import com.commercemesh.notification.event.*;
import com.commercemesh.notification.service.NotificationService;
import com.commercemesh.notification.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final NotificationService notificationService;
    private final TemplateService templateService;

    public OrderEventConsumer(NotificationService notificationService, TemplateService templateService) {
        this.notificationService = notificationService;
        this.templateService = templateService;
    }

    @RabbitListener(queues = "order.notifications")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent: orderId={}, userId={}", event.orderId(), event.userId());
        if (isDuplicate(event.userId(), event.orderId(), NotificationType.ORDER_CONFIRMED)) {
            return;
        }

        Map<String, String> vars = Map.of(
                "orderNumber", event.orderNumber(),
                "customerName", event.customerName(),
                "totalAmount", formatCurrency(event.totalAmount())
        );

        String subject = templateService.buildSubject(NotificationType.ORDER_CONFIRMED, vars);
        String body = templateService.buildBody(NotificationType.ORDER_CONFIRMED, vars);

        notificationService.sendNotification(
                event.userId(), event.orderId(), NotificationType.ORDER_CONFIRMED,
                NotificationChannel.EMAIL, event.customerEmail(), subject, body);
    }

    @RabbitListener(queues = "order.notifications")
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received OrderConfirmedEvent: orderId={}, userId={}", event.orderId(), event.userId());
        if (isDuplicate(event.userId(), event.orderId(), NotificationType.ORDER_CONFIRMED)) {
            return;
        }

        Map<String, String> vars = Map.of(
                "orderNumber", event.orderNumber(),
                "customerName", event.customerName(),
                "totalAmount", formatCurrency(event.totalAmount())
        );

        String subject = templateService.buildSubject(NotificationType.ORDER_CONFIRMED, vars);
        String body = templateService.buildBody(NotificationType.ORDER_CONFIRMED, vars);

        notificationService.sendNotification(
                event.userId(), event.orderId(), NotificationType.ORDER_CONFIRMED,
                NotificationChannel.EMAIL, event.customerEmail(), subject, body);
    }

    @RabbitListener(queues = "order.notifications")
    public void handleOrderShipped(OrderShippedEvent event) {
        log.info("Received OrderShippedEvent: orderId={}, userId={}", event.orderId(), event.userId());
        if (isDuplicate(event.userId(), event.orderId(), NotificationType.ORDER_SHIPPED)) {
            return;
        }

        Map<String, String> vars = Map.of(
                "orderNumber", event.orderNumber(),
                "customerName", event.customerName(),
                "totalAmount", formatCurrency(event.totalAmount()),
                "trackingNumber", event.trackingNumber() != null ? event.trackingNumber() : "N/A",
                "carrier", event.carrier() != null ? event.carrier() : "N/A",
                "estimatedDelivery", event.estimatedDelivery() != null ? event.estimatedDelivery().toString() : "N/A"
        );

        String subject = templateService.buildSubject(NotificationType.ORDER_SHIPPED, vars);
        String body = templateService.buildBody(NotificationType.ORDER_SHIPPED, vars);

        notificationService.sendNotification(
                event.userId(), event.orderId(), NotificationType.ORDER_SHIPPED,
                NotificationChannel.EMAIL, event.customerEmail(), subject, body);
    }

    @RabbitListener(queues = "order.notifications")
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        log.info("Received OrderDeliveredEvent: orderId={}, userId={}", event.orderId(), event.userId());
        if (isDuplicate(event.userId(), event.orderId(), NotificationType.ORDER_DELIVERED)) {
            return;
        }

        Map<String, String> vars = Map.of(
                "orderNumber", event.orderNumber(),
                "customerName", event.customerName(),
                "totalAmount", formatCurrency(event.totalAmount())
        );

        String subject = templateService.buildSubject(NotificationType.ORDER_DELIVERED, vars);
        String body = templateService.buildBody(NotificationType.ORDER_DELIVERED, vars);

        notificationService.sendNotification(
                event.userId(), event.orderId(), NotificationType.ORDER_DELIVERED,
                NotificationChannel.EMAIL, event.customerEmail(), subject, body);
    }

    @RabbitListener(queues = "order.notifications")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: orderId={}, userId={}", event.orderId(), event.userId());
        if (isDuplicate(event.userId(), event.orderId(), NotificationType.ORDER_CANCELLED)) {
            return;
        }

        Map<String, String> vars = Map.of(
                "orderNumber", event.orderNumber(),
                "customerName", event.customerName(),
                "totalAmount", formatCurrency(event.totalAmount()),
                "cancellationReason", event.cancellationReason() != null ? event.cancellationReason() : "No reason provided"
        );

        String subject = templateService.buildSubject(NotificationType.ORDER_CANCELLED, vars);
        String body = templateService.buildBody(NotificationType.ORDER_CANCELLED, vars);

        notificationService.sendNotification(
                event.userId(), event.orderId(), NotificationType.ORDER_CANCELLED,
                NotificationChannel.EMAIL, event.customerEmail(), subject, body);
    }

    private boolean isDuplicate(String userId, String orderId, NotificationType type) {
        boolean exists = notificationService.existsByUserOrderAndType(userId, orderId, type);
        if (exists) {
            log.warn("Duplicate notification detected — skipping: userId={}, orderId={}, type={}",
                    userId, orderId, type);
        }
        return exists;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}

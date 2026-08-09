package com.commercemesh.notification.consumer;

import com.commercemesh.notification.enumeration.NotificationChannel;
import com.commercemesh.notification.enumeration.NotificationType;
import com.commercemesh.notification.event.PaymentCompletedEvent;
import com.commercemesh.notification.event.RefundProcessedEvent;
import com.commercemesh.notification.service.NotificationService;
import com.commercemesh.notification.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final NotificationService notificationService;
    private final TemplateService templateService;

    public PaymentEventConsumer(NotificationService notificationService, TemplateService templateService) {
        this.notificationService = notificationService;
        this.templateService = templateService;
    }

    @RabbitListener(queues = "order.notifications")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: orderId={}, paymentId={}", event.orderId(), event.paymentId());
        if (isDuplicate(event.userId(), event.orderId(), NotificationType.PAYMENT_RECEIVED)) {
            return;
        }

        Map<String, String> vars = Map.of(
                "orderNumber", event.orderNumber(),
                "customerName", event.customerName(),
                "amount", formatCurrency(event.amount()),
                "paymentMethod", event.paymentMethod() != null ? event.paymentMethod() : "N/A",
                "paymentId", event.paymentId() != null ? event.paymentId() : "N/A"
        );

        String subject = templateService.buildSubject(NotificationType.PAYMENT_RECEIVED, vars);
        String body = templateService.buildBody(NotificationType.PAYMENT_RECEIVED, vars);

        notificationService.sendNotification(
                event.userId(), event.orderId(), NotificationType.PAYMENT_RECEIVED,
                NotificationChannel.EMAIL, event.customerEmail(), subject, body);
    }

    @RabbitListener(queues = "order.notifications")
    public void handleRefundProcessed(RefundProcessedEvent event) {
        log.info("Received RefundProcessedEvent: orderId={}, refundId={}", event.orderId(), event.refundId());
        if (isDuplicate(event.userId(), event.orderId(), NotificationType.REFUND_PROCESSED)) {
            return;
        }

        Map<String, String> vars = Map.of(
                "orderNumber", event.orderNumber(),
                "customerName", event.customerName(),
                "refundAmount", formatCurrency(event.refundAmount()),
                "refundReason", event.refundReason() != null ? event.refundReason() : "No reason provided",
                "refundId", event.refundId() != null ? event.refundId() : "N/A"
        );

        String subject = templateService.buildSubject(NotificationType.REFUND_PROCESSED, vars);
        String body = templateService.buildBody(NotificationType.REFUND_PROCESSED, vars);

        notificationService.sendNotification(
                event.userId(), event.orderId(), NotificationType.REFUND_PROCESSED,
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

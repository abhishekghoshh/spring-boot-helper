package com.commercemesh.order.service;

import com.commercemesh.order.model.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String EXCHANGE = "order.events";

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(Long orderId, String orderNumber, Long userId) {
        String routingKey = "order.created";
        String payload = buildPayload(orderId, orderNumber, userId, "ORDER_CREATED");
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        log.info("Published event {} for order {} [{}]", routingKey, orderId, orderNumber);
    }

    public void publishOrderConfirmed(Long orderId, String orderNumber, Long userId) {
        String routingKey = "order.confirmed";
        String payload = buildPayload(orderId, orderNumber, userId, "ORDER_CONFIRMED");
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        log.info("Published event {} for order {} [{}]", routingKey, orderId, orderNumber);
    }

    public void publishOrderPaid(Long orderId, String orderNumber, Long userId, String paymentId) {
        String routingKey = "order.paid";
        String payload = buildPayloadWithExtra(orderId, orderNumber, userId, "ORDER_PAID",
                Map.of("paymentId", paymentId));
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        log.info("Published event {} for order {} [{}]", routingKey, orderId, orderNumber);
    }

    public void publishOrderShipped(Long orderId, String orderNumber, Long userId, String trackingNumber) {
        String routingKey = "order.shipped";
        String payload = buildPayloadWithExtra(orderId, orderNumber, userId, "ORDER_SHIPPED",
                Map.of("trackingNumber", trackingNumber != null ? trackingNumber : ""));
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        log.info("Published event {} for order {} [{}]", routingKey, orderId, orderNumber);
    }

    public void publishOrderDelivered(Long orderId, String orderNumber, Long userId) {
        String routingKey = "order.delivered";
        String payload = buildPayload(orderId, orderNumber, userId, "ORDER_DELIVERED");
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        log.info("Published event {} for order {} [{}]", routingKey, orderId, orderNumber);
    }

    public void publishOrderCancelled(Long orderId, String orderNumber, Long userId) {
        String routingKey = "order.cancelled";
        String payload = buildPayload(orderId, orderNumber, userId, "ORDER_CANCELLED");
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        log.info("Published event {} for order {} [{}]", routingKey, orderId, orderNumber);
    }

    private String buildPayload(Long orderId, String orderNumber, Long userId, String eventType) {
        return buildPayloadWithExtra(orderId, orderNumber, userId, eventType, Map.of());
    }

    private String buildPayloadWithExtra(Long orderId, String orderNumber, Long userId,
                                          String eventType, Map<String, String> extras) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"orderId\":").append(orderId).append(",");
        sb.append("\"orderNumber\":\"").append(escapeJson(orderNumber)).append("\",");
        sb.append("\"userId\":").append(userId).append(",");
        sb.append("\"eventType\":\"").append(eventType).append("\",");
        sb.append("\"timestamp\":\"").append(Instant.now().toString()).append("\"");
        for (Map.Entry<String, String> entry : extras.entrySet()) {
            sb.append(",\"").append(entry.getKey()).append("\":\"").append(escapeJson(entry.getValue())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}

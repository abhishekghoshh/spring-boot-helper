# Messaging & Event Architecture

## Overview

CommerceMesh uses **RabbitMQ 4** for asynchronous, event-driven communication between microservices. The messaging layer decouples services, enables eventual consistency, and provides reliable message delivery with dead-letter handling and retry policies.

---

## Why RabbitMQ?

| Feature | Benefit for CommerceMesh |
|---------|-------------------------|
| **AMQP 0-9-1 protocol** | Industry standard, Spring AMQP native support |
| **Exchanges & Queues** | Flexible routing patterns (topic, direct, fanout) |
| **Message Acknowledgments** | Guaranteed delivery — messages not lost on consumer crash |
| **Dead Letter Queues** | Failed messages isolated for inspection and replay |
| **Delayed Messages** | Retry with exponential backoff via `x-delayed-message` plugin |
| **Management UI** | Built-in dashboard on port 15672 for monitoring queues/messages |
| **Clustering** | Production HA with mirrored queues across nodes |

---

## Exchange & Queue Topology

```
                          ┌─────────────────────┐
                          │  order.events       │
                          │  (topic exchange)   │
                          └─────────┬───────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
     routing: order.*        routing: order.placed   routing: order.*
              │                     │                     │
    ┌─────────▼─────────┐  ┌───────▼───────────┐  ┌─────▼─────────────┐
    │ order.            │  │ order.inventory   │  │ order.analytics   │
    │ notifications     │  │                   │  │                   │
    │ (notification-svc)│  │ (inventory-svc)   │  │ (analytics-svc)   │
    └─────────┬─────────┘  └───────────────────┘  └───────────────────┘
              │
    ┌─────────▼─────────┐
    │ order.notifications│
    │   .dlq            │
    │ (Dead Letter)     │
    └───────────────────┘
```

### Topic Exchange: `order.events`

| Routing Key | Queue | Consumer | Description |
|-------------|-------|----------|-------------|
| `order.placed` | `order.notifications` | notification-service | Send order confirmation email |
| `order.placed` | `order.inventory` | inventory-service | Reserve stock |
| `order.shipped` | `order.notifications` | notification-service | Send shipping notification |
| `order.delivered` | `order.notifications` | notification-service | Send delivery confirmation + review request |
| `order.cancelled` | `order.notifications` | notification-service | Send cancellation email |
| `order.cancelled` | `order.inventory` | inventory-service | Release reserved stock |
| `order.*` | `order.analytics` | (future) | Feed analytics pipeline |

### Dead Letter Queue

Every queue has a corresponding DLQ:

```
order.notifications ──── (max 3 retries) ──── order.notifications.dlq
order.inventory ──────── (max 3 retries) ──── order.inventory.dlq
```

Failed messages in DLQ are:
1. Logged with full message body and error details
2. Exposed as a metric (`rabbitmq_queue_messages{queue="order.notifications.dlq"}`)
3. Manually inspected via RabbitMQ Management UI or admin API
4. Can be replayed using a DLQ replay admin endpoint

---

## Event Schema

All events follow a consistent envelope:

```json
{
  "eventId": "evt_a1b2c3d4",
  "eventType": "OrderPlaced",
  "timestamp": "2026-07-25T15:30:00Z",
  "source": "order-service",
  "correlationId": "corr_x1y2z3",
  "payload": {
    "orderId": "ord_w4x5y6",
    "userId": "usr_m7n8o9",
    "total": 129.99,
    "items": [
      { "productId": "prd_p1q2r3", "name": "Wireless Headphones", "quantity": 1, "price": 129.99 }
    ]
  }
}
```

### Standard Fields

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | String (UUID) | Unique event identifier for idempotency |
| `eventType` | String | PascalCase event name (`OrderPlaced`, `OrderShipped`) |
| `timestamp` | ISO 8601 | When the event was published |
| `source` | String | Originating service name |
| `correlationId` | String | Trace correlation ID (links to distributed trace) |
| `payload` | Object | Event-specific data (varies by eventType) |

### Event Catalog

| Event Type | Publisher | Payload | Consumers |
|-----------|-----------|---------|-----------|
| `OrderPlaced` | order-service | orderId, userId, total, items[], shippingAddress | notification-svc, inventory-svc |
| `OrderShipped` | order-service | orderId, trackingNumber, carrier | notification-svc |
| `OrderDelivered` | order-service | orderId, deliveredAt | notification-svc |
| `OrderCancelled` | order-service | orderId, reason | notification-svc, inventory-svc |
| `PaymentProcessed` | payment-service | orderId, paymentId, amount, method | order-svc |
| `PaymentFailed` | payment-service | orderId, reason | order-svc |
| `UserRegistered` | authentication-service | userId, email, username | notification-svc |
| `PasswordChanged` | authentication-service | userId | notification-svc |
| `InventoryLow` | inventory-service | productId, warehouseId, currentQuantity | notification-svc (alert admin) |

---

## Publisher Configuration (order-service)

```java
@Component
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = OrderPlacedEvent.from(order);
        publish("order.events", "order.placed", event);
    }

    private void publish(String exchange, String routingKey, Object event) {
        rabbitTemplate.convertAndSend(
            exchange,
            routingKey,
            event,
            message -> {
                message.getMessageProperties().setMessageId(event.getEventId());
                message.getMessageProperties().setCorrelationId(MDC.get("traceId"));
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return message;
            }
        );
    }
}
```

**Publisher Settings:**
- `spring.rabbitmq.publisher-confirm-type: correlated` — wait for broker acknowledgment
- `spring.rabbitmq.publisher-returns: true` — handle unroutable messages
- Messages marked `PERSISTENT` (written to disk before acknowledgment)

---

## Consumer Configuration (notification-service)

```java
@Component
public class OrderEventConsumer {

    @RabbitListener(
        queues = "order.notifications",
        containerFactory = "retryContainerFactory"
    )
    public void handleOrderEvent(OrderEvent event) {
        switch (event.getEventType()) {
            case "OrderPlaced" -> handleOrderPlaced((OrderPlacedEvent) event);
            case "OrderShipped" -> handleOrderShipped((OrderShippedEvent) event);
            case "OrderDelivered" -> handleOrderDelivered((OrderDeliveredEvent) event);
            case "OrderCancelled" -> handleOrderCancelled((OrderCancelledEvent) event);
        }
    }

    private void handleOrderPlaced(OrderPlacedEvent event) {
        // Idempotency: check if notification already sent for this eventId
        if (notificationRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate event received: {}", event.getEventId());
            return;
        }

        User user = userClient.getUser(event.getUserId());
        emailService.sendOrderConfirmation(user.getEmail(), event);
        notificationRepository.save(Notification.from(event));
    }
}
```

**Consumer Settings:**
- `spring.rabbitmq.listener.simple.acknowledge-mode: manual` — explicit acknowledgment
- `spring.rabbitmq.listener.simple.prefetch: 10` — process up to 10 messages concurrently
- `spring.rabbitmq.listener.simple.concurrency: 3` — 3 consumer threads per queue

---

## Retry & Dead Letter Policy

### Retry Configuration

```java
@Bean
public RetryOperationsInterceptor retryInterceptor() {
    return RetryInterceptorBuilder.stateless()
        .maxAttempts(3)
        .backOffOptions(
            1000,           // initial interval: 1 second
            2.0,            // multiplier: 2x each retry
            10000           // max interval: 10 seconds
        )   // Retries at: 1s, 2s, 4s
        .recoverer(new RepublishMessageRecoverer(
            rabbitTemplate,
            "order.events",
            "order.notifications.dlq"
        ))
        .build();
}
```

### Dead Letter Queue Flow

```
Attempt 1: FAIL → wait 1s
Attempt 2: FAIL → wait 2s
Attempt 3: FAIL → republish to DLQ
                     │
                     ▼
              order.notifications.dlq
                     │
        ┌────────────┼────────────┐
        │            │            │
   Alert logged   Metric updated  Manual inspection
                                   via RabbitMQ UI
        │
   Admin decides:
   - Fix bug, replay messages
   - Discard (acknowledge)
```

---

## Idempotency

Consumers **must** be idempotent — the same event may be delivered more than once (at-least-once delivery).

**Strategy:** Store processed `eventId` values in a database table with a unique constraint:

```sql
CREATE TABLE processed_events (
    event_id VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

The consumer checks this table before processing. If the `eventId` already exists, the message is acknowledged and skipped.

**Alternative strategies:**
- MongoDB: Check document existence by `eventId` field
- Redis: `SETNX event:{eventId} 1 EX 86400` (24h TTL, 24-hour idempotency window)

---

## RabbitMQ Infrastructure

### Docker Compose

```yaml
rabbitmq:
  image: rabbitmq:4-management-alpine
  ports:
    - "5672:5672"     # AMQP
    - "15672:15672"   # Management UI
  environment:
    RABBITMQ_DEFAULT_USER: admin
    RABBITMQ_DEFAULT_PASS: admin123
  volumes:
    - rabbitmq_data:/var/lib/rabbitmq
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
```

### Spring Boot Configuration

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:admin}
    password: ${RABBITMQ_PASSWORD:admin123}
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        concurrency: 3
        max-concurrency: 10
        retry:
          enabled: false        # Use manual RetryOperationsInterceptor instead
    publisher-confirm-type: correlated
    publisher-returns: true
```

### Declarative Queue Setup

```java
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.events", true, false);
    }

    @Bean
    public Queue orderNotificationsQueue() {
        return QueueBuilder.durable("order.notifications")
            .deadLetterExchange("order.events")
            .deadLetterRoutingKey("order.notifications.dlq")
            .build();
    }

    @Bean
    public Queue orderNotificationsDLQ() {
        return new Queue("order.notifications.dlq", true);
    }

    @Bean
    public Binding orderNotificationsBinding() {
        return BindingBuilder
            .bind(orderNotificationsQueue())
            .to(orderExchange())
            .with("order.*");
    }
}
```

---

## Monitoring RabbitMQ

### Prometheus Metrics

Enable the built-in Prometheus plugin:

```bash
rabbitmq-plugins enable rabbitmq_prometheus
```

Key metrics:

| Metric | Description |
|--------|-------------|
| `rabbitmq_queue_messages_ready` | Messages waiting in queue |
| `rabbitmq_queue_messages_unacknowledged` | Messages delivered but not yet acked |
| `rabbitmq_queue_messages_published_total` | Total messages published |
| `rabbitmq_connections` | Active connections |
| `rabbitmq_channels` | Active channels |
| `rabbitmq_queue_messages{queue="*.dlq"}` | Dead letter queue depth |

### Alert Rules (Prometheus)

```yaml
groups:
  - name: rabbitmq_alerts
    rules:
      - alert: RabbitMQQueueDepth
        expr: rabbitmq_queue_messages_ready > 1000
        for: 5m
        annotations:
          summary: "Queue {{ $labels.queue }} has {{ $value }} pending messages"

      - alert: RabbitMQDLQDepth
        expr: rabbitmq_queue_messages{queue=~".*dlq"} > 0
        for: 1m
        annotations:
          summary: "Dead letter queue has unprocessed messages"
```

---

## Production Considerations

1. **Clustering:** Deploy RabbitMQ as a cluster (3 nodes minimum for quorum queues)
2. **Quorum Queues:** Use quorum queues instead of classic mirrored queues (RabbitMQ 4 default)
3. **Persistent Messages:** All messages use `delivery_mode=2` (persistent to disk)
4. **Connection Pooling:** Each service uses a shared `CachingConnectionFactory` (default Spring behavior)
5. **Network Policies:** Only order-service and notification-service have egress to RabbitMQ in K8s
6. **Resource Limits:** RabbitMQ pods get 512Mi-1Gi memory, 500m-1 CPU based on throughput
7. **Backup:** RabbitMQ definitions exported via `rabbitmqctl export_definitions` for disaster recovery

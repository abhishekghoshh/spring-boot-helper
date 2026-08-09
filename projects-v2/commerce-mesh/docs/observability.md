# Observability

## Overview

CommerceMesh implements the **three pillars of observability** — metrics, tracing, and logging — using a fully open-source stack. Every microservice is instrumented out of the box with **Micrometer** (metrics), **OpenTelemetry** (tracing), and **structured JSON logging** (logs). All telemetry data flows into **Prometheus**, **Jaeger**, and **Loki**, with **Grafana** as the unified visualization layer.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        OBSERVABILITY STACK                          │
│                                                                     │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐      │
│  │Prometheus│    │  Jaeger  │    │   Loki   │    │ Grafana  │      │
│  │  :9090   │    │  :16686  │    │  :3100   │    │  :3001   │      │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘      │
│       │               │               │               │            │
│  ┌────▼───────────────▼───────────────▼───────────────▼────┐       │
│  │                    Telemetry Pipeline                    │       │
│  │  /actuator/prometheus  │  OTLP gRPC  │  Docker logs     │       │
│  └────────────────────┬───┴──────┬──────┴───┬──────────────┘       │
│                       │          │          │                      │
│  ┌────────────────────▼──────────▼──────────▼──────────────────┐   │
│  │              All 12 Microservices                           │   │
│  │  Micrometer  │  OpenTelemetry Agent  │  JSON Logger          │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Metrics (Micrometer + Prometheus + Grafana)

### Metrics Pipeline

```
Application → Micrometer → /actuator/prometheus → Prometheus (pull) → Grafana
                                              ↘ AlertManager (push)
```

### Dependency

Every service includes:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,loggers
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:dev}
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
```

### Built-in Metrics (Auto-collected)

| Category | Metrics |
|----------|---------|
| **JVM** | `jvm_memory_used_bytes`, `jvm_gc_pause_seconds`, `jvm_threads_live`, `jvm_classes_loaded` |
| **HTTP** | `http_server_requests_seconds_count`, `http_server_requests_seconds_sum`, `http_server_requests_seconds_max` |
| **DataSource** | `hikaricp_connections_active`, `hikaricp_connections_pending`, `hikaricp_connections_timeout_total` |
| **Cache** | `cache_gets_total`, `cache_puts_total`, `cache_evictions_total` |
| **MongoDB** | `mongodb_driver_commands_seconds`, `mongodb_driver_pool_checkedout_count` |
| **RabbitMQ** | `spring_rabbitmq_connections`, `spring_rabbitmq_published_total` |
| **Tomcat** | `tomcat_threads_busy`, `tomcat_threads_current`, `tomcat_connections_current` |

### Custom Business Metrics

```java
@Service
public class OrderService {

    private final MeterRegistry meterRegistry;

    // Counter: total orders placed
    private final Counter ordersPlacedCounter;

    // Timer: checkout duration
    private final Timer checkoutTimer;

    // Gauge: active carts
    private final AtomicInteger activeCartsGauge;

    public OrderService(MeterRegistry meterRegistry) {
        this.ordersPlacedCounter = Counter.builder("orders.placed.total")
            .description("Total number of orders placed")
            .tag("service", "order-service")
            .register(meterRegistry);

        this.checkoutTimer = Timer.builder("orders.checkout.duration")
            .description("Time to complete checkout")
            .register(meterRegistry);

        this.activeCartsGauge = meterRegistry.gauge("carts.active",
            new AtomicInteger(0));
    }

    @Transactional
    public OrderDto placeOrder(PlaceOrderRequest request) {
        return checkoutTimer.record(() -> {
            Order order = processOrder(request);
            ordersPlacedCounter.increment();
            return orderMapper.toDto(order);
        });
    }
}
```

### Custom Metric Naming Convention

| Prefix | Type | Example |
|--------|------|---------|
| `orders.*` | Order-related | `orders.placed.total`, `orders.checkout.duration` |
| `products.*` | Product-related | `products.search.duration`, `products.views.total` |
| `payments.*` | Payment-related | `payments.processed.total`, `payments.failed.total` |
| `users.*` | User-related | `users.registered.total`, `users.login.total` |

### Prometheus Scrape Configuration

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-actuator'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'discovery-server:8761'
          - 'config-server:8888'
          - 'api-gateway:8080'
          - 'authentication-service:8081'
          - 'user-service:8082'
          - 'product-catalog-service:8083'
          - 'cart-service:8084'
          - 'inventory-service:8085'
          - 'order-service:8086'
          - 'payment-service:8087'
          - 'notification-service:8088'
          - 'search-service:8089'
```

### Grafana Dashboards

Dashboards are provisioned automatically from `monitoring/grafana/`.

#### Dashboard: JVM Overview

| Panel | Metric |
|-------|--------|
| Heap Memory Usage | `jvm_memory_used_bytes{area="heap"}` |
| Non-Heap Memory | `jvm_memory_used_bytes{area="nonheap"}` |
| GC Pause Time | `rate(jvm_gc_pause_seconds_sum[1m])` |
| Thread Count | `jvm_threads_live_threads` |
| CPU Usage | `process_cpu_usage` |
| Uptime | `process_uptime_seconds` |

#### Dashboard: HTTP Overview

| Panel | Metric |
|-------|--------|
| Request Rate | `rate(http_server_requests_seconds_count[1m])` |
| Error Rate (5xx) | `rate(http_server_requests_seconds_count{status=~"5.."}[1m])` |
| Latency P50/P95/P99 | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))` |
| Top Endpoints | `topk(5, rate(http_server_requests_seconds_count[5m])) by (uri)` |

#### Dashboard: Database Overview

| Panel | Metric |
|-------|--------|
| HikariCP Active Connections | `hikaricp_connections_active` |
| MongoDB Command Duration | `rate(mongodb_driver_commands_seconds_sum[1m])` |
| Mongo Connection Pool | `mongodb_driver_pool_checkedout_count` |

#### Dashboard: Redis Overview

| Panel | Metric |
|-------|--------|
| Cache Hit Rate | `rate(cache_gets_total{result="hit"}[1m]) / rate(cache_gets_total[1m])` |
| Cache Miss Rate | `rate(cache_gets_total{result="miss"}[1m])` |

#### Dashboard: RabbitMQ Overview

| Panel | Metric |
|-------|--------|
| Queue Depth | `rabbitmq_queue_messages_ready` |
| Unacknowledged Messages | `rabbitmq_queue_messages_unacknowledged` |
| DLQ Depth | `rabbitmq_queue_messages{queue=~".*dlq"}` |
| Publish Rate | `rate(spring_rabbitmq_published_total[1m])` |

---

## Distributed Tracing (OpenTelemetry + Jaeger)

### Tracing Pipeline

```
Application → OpenTelemetry Agent → OTLP gRPC (:4317) → Jaeger Collector → Jaeger UI (:16686)
```

### Dependencies

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### Configuration

```yaml
management:
  tracing:
    sampling:
      probability: 1.0    # 100% in dev, 10-25% in production
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

### What Gets Traced (Auto-instrumented)

Spring Boot auto-instruments:

- **HTTP requests:** Every REST endpoint call — spans include method, path, status code, duration
- **Feign clients:** Outbound service-to-service calls — spans include target service name, method, duration
- **Database calls:** JPA repository queries, MongoDB operations — spans include query type (SELECT/INSERT), duration
- **Messaging:** RabbitMQ publish and consume — spans include exchange, routing key, queue
- **Redis:** Cache get/put operations

### Trace Example

```
TRACE: checkout-abc123 (1.2s total)

├── Gateway: POST /api/v1/orders              [1.2s]
│   ├── order-service: POST /api/v1/orders    [1.1s]
│   │   ├── PostgreSQL: SELECT stock          [15ms]
│   │   ├── Feign: inventory-service          [200ms]
│   │   │   └── inventory: POST /reserve      [180ms]
│   │   │       └── PostgreSQL: UPDATE stock  [80ms]
│   │   ├── Feign: payment-service            [500ms]
│   │   │   └── payment: POST /payments       [480ms]
│   │   │       └── PostgreSQL: INSERT payment [30ms]
│   │   ├── PostgreSQL: INSERT order          [20ms]
│   │   └── RabbitMQ: publish OrderPlaced     [10ms]
│   └── notification-service: consume         [200ms]
│       ├── MongoDB: INSERT notification      [15ms]
│       └── SMTP: send email                  [180ms]
```

### Viewing Traces

- **Jaeger UI:** `http://localhost:16686`
- Search by service name, operation, tags, or duration
- Waterfall view shows timing breakdown
- Click on spans to see tags, logs, and events

### Trace Propagation

Traces automatically propagate across:

1. **HTTP headers:** `traceparent`, `tracestate` (W3C Trace Context)
2. **Feign clients:** Automatic — no code change needed
3. **RabbitMQ messages:** Message headers carry trace context
4. **@Async methods:** Thread-local context preserved

---

## Centralized Logging (Loki + Promtail + Grafana)

### Logging Pipeline

```
Application → stdout (JSON) → Docker log driver → Promtail → Loki → Grafana
```

### Structured Logging Configuration

Every service logs in JSON format using Logback with a JSON encoder:

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>requestId</includeMdcKeyName>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### Log Format

```json
{
  "@timestamp": "2026-07-25T15:30:00.123Z",
  "level": "INFO",
  "logger": "com.commercemesh.order.service.OrderService",
  "message": "Order placed successfully",
  "traceId": "abc123def456",
  "spanId": "def456",
  "userId": "user-456",
  "orderId": "ord-789",
  "service": "order-service",
  "thread": "http-nio-8086-exec-3"
}
```

### Loki Configuration

```yaml
auth_enabled: false

server:
  http_listen_port: 3100

common:
  ring:
    kvstore:
      store: inmemory
  replication_factor: 1
  path_prefix: /loki

schema_config:
  configs:
    - from: 2024-01-01
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h

storage_config:
  filesystem:
    directory: /loki/chunks
```

### Promtail Configuration

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: docker
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        regex: '/(.*)'
        target_label: 'container'
```

### LogQL Queries (From Grafana)

```logql
# All logs from order-service
{container="cm-order-service"}

# Error logs from the last hour
{container=~"cm-.*"} | json | level = "ERROR"

# Logs for a specific trace
{container=~"cm-.*"} | json | traceId = "abc123def456"

# Logs containing "OutOfStockException"
{container=~"cm-.*"} |= "OutOfStockException"

# Rate of errors per service over 5 minutes
rate({container=~"cm-.*"} | json | level = "ERROR" [5m]) by (container)
```

---

## Alert Rules

### Prometheus AlertManager Rules

```yaml
groups:
  - name: commerce_mesh_alerts
    rules:

      # Service Down
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "{{ $labels.job }} is down"
          description: "Service has been down for more than 1 minute"

      # High Error Rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.service }} error rate is {{ $value | humanizePercentage }}"

      # High Response Time
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.service }} P95 latency is {{ $value }}s"

      # Circuit Breaker Open
      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{state="open"} == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Circuit breaker {{ $labels.name }} is OPEN"

      # High JVM Memory
      - alert: HighJvmMemory
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.application }} heap usage at {{ $value | humanizePercentage }}"

      # DLQ Messages Present
      - alert: DeadLetterQueueDepth
        expr: rabbitmq_queue_messages{queue=~".*dlq"} > 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Dead letter queue {{ $labels.queue }} has {{ $value }} messages"

      # Database Connection Pool Exhaustion
      - alert: ConnectionPoolExhaustion
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.pool }} connection pool at {{ $value | humanizePercentage }}"

      # Disk Space Low
      - alert: LowDiskSpace
        expr: node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes < 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Disk space below 10% on {{ $labels.instance }}"
```

---

## Spring Boot Actuator Endpoints

Every service exposes:

| Endpoint | Description | Production |
|----------|-------------|-----------|
| `/actuator/health` | Overall health status | ✓ |
| `/actuator/health/liveness` | Container liveness probe | ✓ |
| `/actuator/health/readiness` | Traffic readiness | ✓ |
| `/actuator/info` | Build info, git commit | ✓ |
| `/actuator/metrics` | All Micrometer metrics | ✓ |
| `/actuator/prometheus` | Prometheus scrape endpoint | ✓ |
| `/actuator/env` | Environment properties | ✗ (disabled) |
| `/actuator/loggers` | Dynamic log level changes | ✓ (admin only) |
| `/actuator/threaddump` | Thread dump | ✗ (disabled) |
| `/actuator/heapdump` | Heap dump | ✗ (disabled) |

---

## Production Tuning

### Sampling Rate

- **Development:** `management.tracing.sampling.probability=1.0` (100%)
- **Staging:** `0.25` (25%)
- **Production:** `0.10` (10%) — reduce cost while maintaining statistical significance

### Metric Retention

- **Prometheus:** 15 days of raw data (configurable via `--storage.tsdb.retention.time=15d`)
- **Loki:** 30 days of logs
- **Jaeger:** 7 days of traces

### Log Levels

Default: `INFO` for all packages. Override specific packages:

```yaml
logging:
  level:
    com.commercemesh: DEBUG          # Application code
    org.springframework: INFO        # Framework
    com.zaxxer.hikari: WARN          # Connection pool
    org.mongodb.driver: WARN         # MongoDB driver
    io.lettuce.core: WARN            # Redis client
```

Dynamic log level changes via Actuator (no restart needed):

```bash
curl -X POST http://localhost:8081/actuator/loggers/com.commercemesh.auth \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## Observability Stack Summary

| Tool | Port | Purpose | Data Store |
|------|------|---------|------------|
| Prometheus | 9090 | Metrics collection & query | TSDB on disk |
| Grafana | 3001 | Unified dashboards & alerting | SQLite (internal) |
| Jaeger | 16686 (UI), 4317 (OTLP), 4318 (HTTP) | Distributed tracing | BadgerDB on disk (all-in-one) or Elasticsearch/Cassandra |
| Loki | 3100 | Log aggregation | Filesystem (dev) / S3 (prod) |
| Promtail | — | Log collection from Docker/K8s | Ships to Loki |
| AlertManager | 9093 | Alert routing (Slack, PagerDuty, email) | — |

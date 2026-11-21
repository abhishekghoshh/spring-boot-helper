# Observability with Spring Boot

> **Primer:** Spring Boot 3 observability = Micrometer (metrics + tracing) with
> an OTel/Zipkin bridge, Logback MDC trace ids in logs, and Actuator exposing
> `/actuator/prometheus` + `/actuator/health`. Grafana stack (Prometheus +
> Tempo + Loki) turns the three pillars — metrics, traces, logs — into one
> correlated view keyed by `traceId`.

## Real-life use case

Checkout latency spikes to p99 4s: the Prometheus `http.server.requests` histogram
shows the `/charge` route regressed; clicking an exemplar traceId jumps to the
Tempo waterfall (fraud-check span = 3.8s); the Loki logs for that traceId show
the downstream timeout — root cause in 3 clicks.

## Mental model

```text
  App (Micrometer + OTel bridge)
    ├─ metrics → /actuator/prometheus → Prometheus → Grafana dashboards
    ├─ traces  → OTLP → Tempo (waterfall per traceId)
    └─ logs    → MDC traceId/spanId in every line → Loki (filter by traceId)

  Correlation key: traceId flows HTTP → Kafka headers → DB comments
```

## Minimal example

```yaml
management:
  tracing:
    sampling:
      probability: 0.1          # 10% head sampling in prod; 1.0 in dev
  zipkin:
    tracing:
      endpoint: http://tempo:9411/api/v2/spans
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

```xml
<!-- logback: print traceId/spanId on every line -->
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{HH:mm:ss} [%thread] %-5level %logger{36} [trace=%X{traceId:-} span=%X{spanId:-}] - %msg%n</pattern>
    </encoder>
</appender>
```

```java
// Custom business metric (Micrometer, with imports)
package com.example.orders;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class OrderMetrics {
    private final Counter placed;
    public OrderMetrics(MeterRegistry registry) {
        this.placed = Counter.builder("orders.placed.total")
                .description("Orders placed").register(registry);
    }
    public void markPlaced() { placed.increment(); }
}
```

## Pitfalls

- 100% trace sampling in prod drowns Tempo and doubles egress — use head sampling + tail-based rules for errors.
- High-cardinality labels (user id, order id in metric tags) explode Prometheus — keep those in traces/logs, not metrics.
- Missing MDC in `@Async`/reactor threads breaks correlation — use context propagation (`Hooks.enableAutomaticContextPropagation()`).

## Interview Q&A

**Q1. Metrics vs traces vs logs — what question does each answer?**
A: Metrics ("is something wrong?" — rates/latencies over time), traces
("where exactly is it slow?" — per-request waterfall), logs ("why did this
instance fail?" — detailed events for one traceId). You need all three,
correlated by traceId.

**Q2. Why is `traceId` in every log line non-negotiable?**
A: Without it, finding the 50 log lines for one failed request among millions
is a grep lottery. With MDC trace ids + Loki/ELK, one click from a trace or
alert filters to exactly that request's logs across every service.

---


## Youtube

- [Spring Boot 3 Observability with Grafana Stack](https://www.youtube.com/watch?v=PT2yZTBnUwQ)
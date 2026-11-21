# Splunk Integration with Spring Boot

> **Primer:** Splunk ingests your Spring Boot logs over HTTP Event Collector
> (HEC) — JSON events with `sourcetype`, `index`, and fields — then lets you
> search (`index=prod traceId=abc123`), alert (error-rate spike → PagerDuty),
> and dashboard (p99 by route) across every service from one query bar.

## Real-life use case

Payment outage at 2am: Splunk alert fires (`error rate > 1% for 5m`), the
on-call searches `index=payments status=500` grouped by `traceId`, finds all
failures share `downstream=fraud-check`, and the dashboard shows the fraud
deploy timestamp lines up exactly — rollback in 10 minutes.

## Mental model

```text
  App (Logback JSON + MDC traceId)
        │  HTTPS POST /services/collector/event (HEC token)
        v
  Splunk index=prod ──┬── search: index=prod level=ERROR | stats count by service
                      ├── alert: error-rate > threshold → PagerDuty/Slack
                      └── dashboard: p99 latency, error %, throughput by route
```

## Minimal example

```xml
<!-- logback-spring.xml: JSON to console (collected by forwarder) or HEC appender -->
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
        <customFields>{"service":"payments","env":"${ENV:-local}"}</customFields>
    </encoder>
</appender>
```

```yaml
logging:
  level:
    root: INFO
    com.example.payments: DEBUG
```

```spl
index=prod service=payments level=ERROR
| stats count by exception, downstream
| sort -count
```

## Pitfalls

- Logging full payloads/PII (cards, tokens) into Splunk = compliance incident — mask at the encoder.
- Unbounded `DEBUG` in prod explodes license ingestion — default INFO, flip packages temporarily via Actuator loggers endpoint.
- Missing `traceId` in MDC makes cross-service search impossible — propagate it (see observability page).

## Interview Q&A

**Q1. HEC vs file-forwarder — when is each right?**
A: HEC (app POSTs JSON directly) is simplest for containers/K8s with no agent
access. Universal Forwarder (tails log files) suits VMs and adds local
buffering when Splunk is unreachable. Both land in the same index.

**Q2. How do you keep Splunk costs from exploding?**
A: Log JSON at INFO by default, sample DEBUG via dynamic log levels, drop
health-check noise with filters, set per-index retention, and alert on
ingestion anomalies — log volume is a budget line, not free.

---



## Youtube

- [Spring Boot With Splunk Integration | Realtime logs analysis using Splunk | HEC | JavaTechie](https://www.youtube.com/watch?v=VO20SgiTTOU)
- [Spring Boot - Splunk Alerts ! 🚨 | Creating and Scheduling Alerts | JavaTechie](https://www.youtube.com/watch?v=eCAabhr5UNA)
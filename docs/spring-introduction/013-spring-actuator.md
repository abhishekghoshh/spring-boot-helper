# Spring Actuator

## 1. What is Spring Actuator?

**Spring Boot Actuator** is a sub-project of Spring Boot that exposes a set of **production-ready HTTP endpoints** (and JMX beans) to monitor and manage a running Spring Boot application **without writing any custom code**.

It gives operational teams and developers visibility into the internals of the application — health, metrics, environment, beans, threads, HTTP traces, and much more — directly over HTTP.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Application                              │
│                                                                              │
│  ┌──────────────────────┐      ┌───────────────────────────────────────────┐ │
│  │   Business Layer     │      │         Spring Boot Actuator              │ │
│  │  (Controllers,       │      │                                           │ │
│  │   Services, Repos)   │      │  /actuator/health    → App health status  │ │
│  │                      │      │  /actuator/info      → App metadata       │ │
│  │                      │      │  /actuator/metrics   → JVM, HTTP stats    │ │
│  │                      │      │  /actuator/env       → Config properties  │ │
│  │                      │      │  /actuator/beans     → All Spring beans   │ │
│  │                      │      │  /actuator/loggers   → Log level control  │ │
│  │                      │      │  /actuator/threaddump→ JVM thread dump    │ │
│  │                      │      │  /actuator/httptrace → Recent HTTP calls  │ │
│  └──────────────────────┘      └───────────────────────────────────────────┘ │
│                                          │                                   │
└──────────────────────────────────────────┼───────────────────────────────────┘
                                           │  HTTP / JMX
                          ┌────────────────▼─────────────────┐
                          │   Monitoring Tools / DevOps Team  │
                          │  (Prometheus, Grafana, k8s, etc.) │
                          └──────────────────────────────────┘
```

---

## 2. How to Add Spring Actuator

Add the dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Or with Gradle (`build.gradle`):

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

Once added, the `/actuator/health` endpoint is available **out of the box** with no extra configuration.

---

## 3. What is the Usage of Spring Actuator?

### 3.1 Built-in Endpoints

| Endpoint | HTTP Method | Description |
|---|---|---|
| `/actuator/health` | GET | Overall health of the application |
| `/actuator/info` | GET | Arbitrary application info (version, git, build) |
| `/actuator/metrics` | GET | JVM, CPU, memory, HTTP request stats |
| `/actuator/metrics/{name}` | GET | Specific metric (e.g., `jvm.memory.used`) |
| `/actuator/env` | GET | Full environment including all properties |
| `/actuator/beans` | GET | All Spring beans registered in the context |
| `/actuator/mappings` | GET | All `@RequestMapping` routes |
| `/actuator/loggers` | GET | Current log levels per package |
| `/actuator/loggers/{name}` | POST | Change a log level at runtime |
| `/actuator/threaddump` | GET | Current JVM thread dump |
| `/actuator/heapdump` | GET | Binary heap dump (download) |
| `/actuator/shutdown` | POST | Gracefully shut down the app (disabled by default) |
| `/actuator/httptrace` | GET | Last 100 HTTP request/response pairs |
| `/actuator/scheduledtasks` | GET | Configured scheduled tasks |
| `/actuator/caches` | GET | Available caches and their contents |
| `/actuator/flyway` | GET | Flyway migration history |
| `/actuator/liquibase` | GET | Liquibase changelog status |

### 3.2 Enabling and Exposing Endpoints

By default, only `health` and `info` are exposed over HTTP. Configure in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"          # expose all endpoints
        # include: health,info,metrics,loggers
  endpoint:
    health:
      show-details: always    # show full health details
    shutdown:
      enabled: true           # enable shutdown endpoint (careful in prod!)
  server:
    port: 8081                # run actuator on a separate port (recommended)
```

### 3.3 Health Checks

Spring Actuator integrates with common components automatically:

```
GET /actuator/health

{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": { "database": "PostgreSQL", "validationQuery": "isValid()" }
    },
    "diskSpace": {
      "status": "UP",
      "details": { "total": 499963174912, "free": 91152891904, "threshold": 10485760 }
    },
    "redis": {
      "status": "UP",
      "details": { "version": "7.0.12" }
    }
  }
}
```

**Built-in health indicators:**
- `DataSourceHealthIndicator` (JDBC)
- `RedisHealthIndicator`
- `MongoHealthIndicator`
- `KafkaHealthIndicator`
- `ElasticsearchHealthIndicator`
- `DiskSpaceHealthIndicator`
- `MailHealthIndicator`

### 3.4 Custom Health Indicator

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    private final ExternalApiClient client;

    public ExternalApiHealthIndicator(ExternalApiClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            boolean reachable = client.ping();
            if (reachable) {
                return Health.up()
                             .withDetail("url", client.getBaseUrl())
                             .build();
            }
            return Health.down()
                         .withDetail("reason", "External API not reachable")
                         .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
```

### 3.5 Custom Metrics with Micrometer

Actuator integrates with **Micrometer** (the metrics facade) to publish metrics to Prometheus, Datadog, InfluxDB, etc.

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final Counter orderCounter;

    public OrderService(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.created")
                                   .description("Total orders created")
                                   .tag("region", "us-east")
                                   .register(registry);
    }

    public void createOrder(Order order) {
        // business logic ...
        orderCounter.increment();
    }
}
```

Query via Actuator:
```
GET /actuator/metrics/orders.created

{
  "name": "orders.created",
  "description": "Total orders created",
  "measurements": [{ "statistic": "COUNT", "value": 1042.0 }],
  "availableTags": [{ "tag": "region", "values": ["us-east"] }]
}
```

### 3.6 Custom Info Endpoint

```yaml
# application.yml
info:
  app:
    name: Order Service
    version: "@project.version@"
    description: Manages all order lifecycle events
  team:
    name: Platform Engineering
    contact: platform@company.com
```

```
GET /actuator/info

{
  "app": {
    "name": "Order Service",
    "version": "1.4.2",
    "description": "Manages all order lifecycle events"
  },
  "team": {
    "name": "Platform Engineering",
    "contact": "platform@company.com"
  }
}
```

### 3.7 Dynamic Log Level Change at Runtime

```bash
# Check current log level
GET /actuator/loggers/com.example.service

{ "configuredLevel": "INFO", "effectiveLevel": "INFO" }

# Change to DEBUG without restarting the application
POST /actuator/loggers/com.example.service
Content-Type: application/json

{ "configuredLevel": "DEBUG" }
```

### 3.8 Custom Actuator Endpoint

```java
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "feature-flags")
public class FeatureFlagEndpoint {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagEndpoint(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @ReadOperation
    public Map<String, Boolean> getFlags() {
        return featureFlagService.getAllFlags();
    }

    @WriteOperation
    public void setFlag(@Selector String name, boolean enabled) {
        featureFlagService.setFlag(name, enabled);
    }
}
```

Accessible at: `GET /actuator/feature-flags`

---

## 4. Why Should We Use Spring Actuator?

```
┌─────────────────────────────────────────────────────────────────┐
│                  Why Use Spring Actuator?                        │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────┐ │
│  │  Observability │  │  Operations    │  │  Zero Extra Code   │ │
│  │                │  │                │  │                    │ │
│  │ • Health       │  │ • Graceful     │  │ • Auto-configured  │ │
│  │   status for   │  │   shutdown     │  │   by Spring Boot   │ │
│  │   load balancer│  │ • Log level    │  │ • Works with       │ │
│  │ • JVM metrics  │  │   tuning at    │  │   Prometheus,      │ │
│  │   for Grafana  │  │   runtime      │  │   Datadog, etc.    │ │
│  │ • Thread dump  │  │ • Cache evict  │  │ • One dependency   │ │
│  │   for debugging│  │   at runtime   │  │   away             │ │
│  └────────────────┘  └────────────────┘  └────────────────────┘ │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐                         │
│  │  Kubernetes    │  │  Compliance    │                         │
│  │  Integration   │  │  & Auditing    │                         │
│  │                │  │                │                         │
│  │ • /health used │  │ • Env snapshot │                         │
│  │   for liveness │  │ • Bean listing │                         │
│  │   & readiness  │  │ • Flyway audit │                         │
│  │   probes       │  │   trail        │                         │
│  └────────────────┘  └────────────────┘                         │
└─────────────────────────────────────────────────────────────────┘
```

### Key reasons:

1. **Kubernetes liveness and readiness probes** — Spring Boot auto-configures `/actuator/health/liveness` and `/actuator/health/readiness` for Kubernetes.
2. **Prometheus + Grafana integration** — Add `micrometer-registry-prometheus` and scrape metrics automatically.
3. **No instrumentation boilerplate** — Metrics for JVM, GC, Tomcat, HTTP requests are registered automatically.
4. **Runtime management** — Change log levels, evict caches, or trigger graceful shutdown without a redeploy.
5. **Debugging in production** — Thread dump and heap dump endpoints let you diagnose production issues without SSH access.

### Kubernetes Health Probe Configuration

```yaml
# application.yml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

```yaml
# k8s deployment.yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5
```

### Prometheus Integration

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info
```

Prometheus scrapes `GET /actuator/prometheus` and Grafana dashboards are available immediately.

---

## 5. Advantages and Disadvantages

### Advantages

| Advantage | Detail |
|---|---|
| **Zero boilerplate** | Auto-configured health, metrics, and info out of the box |
| **Production-grade observability** | JVM, GC, Tomcat, DataSource, Redis metrics without custom code |
| **Kubernetes-native** | Built-in liveness and readiness probes |
| **Micrometer integration** | Vendor-neutral metrics API — swap backends without code changes |
| **Runtime management** | Adjust log levels, evict caches, trigger shutdown without restart |
| **Extensible** | Custom endpoints, custom health indicators, custom metrics |
| **Security integration** | Endpoints can be secured with Spring Security role-based access |
| **Dashboard-ready** | Works out of the box with Prometheus, Grafana, Datadog, New Relic |
| **Debugging support** | Thread dump and heap dump without needing SSH or JVM tools |

### Disadvantages

| Disadvantage | Detail |
|---|---|
| **Security risk if misconfigured** | Exposing all endpoints publicly leaks sensitive data (env vars, beans, heap dumps) |
| **Performance overhead** | Metrics collection adds minor CPU/memory overhead; mitigated but non-zero |
| **Sensitive data exposure** | `/actuator/env` can expose secrets if property masking is not configured |
| **Complexity for large apps** | Too many metrics can overwhelm Prometheus/Grafana without proper filtering |
| **Heap dump size** | `/actuator/heapdump` can generate multi-GB files on production JVMs |
| **Shutdown endpoint danger** | `/actuator/shutdown` can bring down the app if accidentally exposed |

### Security Best Practices

```yaml
management:
  server:
    port: 8081                  # Actuator on internal port, not exposed externally

# application.yml (Spring Security)
spring:
  security:
    user:
      name: actuator-user
      password: ${ACTUATOR_PASSWORD}   # from environment variable, never hardcoded
```

```java
@Configuration
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests(auth -> auth
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
                    .permitAll()
                .anyRequest()
                    .hasRole("ACTUATOR_ADMIN")
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```

Masking sensitive environment properties:

```yaml
management:
  endpoint:
    env:
      keys-to-sanitize: "password,secret,key,token,credentials,vcap_services"
```

---

## 6. Architecture: Request Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                     Actuator Request Flow                            │
│                                                                      │
│  External Caller (Prometheus / k8s / DevOps)                        │
│        │                                                             │
│        │  GET /actuator/health                                       │
│        ▼                                                             │
│  ┌─────────────────────┐                                            │
│  │   Security Filter   │  ← Spring Security (role check)           │
│  └──────────┬──────────┘                                            │
│             │                                                        │
│             ▼                                                        │
│  ┌─────────────────────┐                                            │
│  │  WebMvcEndpointHandlerMapping  │  ← maps /actuator/* routes     │
│  └──────────┬──────────┘                                            │
│             │                                                        │
│             ▼                                                        │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │               Endpoint Implementation                        │   │
│  │                                                             │   │
│  │  HealthEndpoint                                             │   │
│  │    │                                                        │   │
│  │    ├── DataSourceHealthIndicator  → checks DB connectivity  │   │
│  │    ├── RedisHealthIndicator       → pings Redis             │   │
│  │    ├── DiskSpaceHealthIndicator   → checks free disk        │   │
│  │    └── ExternalApiHealthIndicator → custom check            │   │
│  │                                                             │   │
│  │  Aggregated status: UP / DOWN / OUT_OF_SERVICE / UNKNOWN    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│             │                                                        │
│             ▼                                                        │
│  JSON response returned to caller                                   │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 7. Summary

| Concern | Spring Actuator Answer |
|---|---|
| **What is it?** | A Spring Boot module exposing management/monitoring HTTP endpoints |
| **Core value** | Instant production observability without custom code |
| **Key endpoints** | `/health`, `/metrics`, `/info`, `/env`, `/loggers`, `/threaddump` |
| **Integration** | Prometheus, Grafana, Datadog, Kubernetes, New Relic |
| **Extensibility** | Custom endpoints, custom health indicators, Micrometer custom metrics |
| **Security** | Always restrict with Spring Security + run on a separate internal port |

---

## 8. Real-Life Use Cases with Code Examples

### Use Case 1: E-Commerce Order Service — Health + Metrics Dashboard

**Scenario:** Your order service connects to PostgreSQL, Redis (for cart), and a third-party payment gateway. The DevOps team uses Prometheus + Grafana and Kubernetes.

```
┌────────────────────────────────────────────────────────────────────────┐
│                      Order Service — Actuator Setup                    │
│                                                                        │
│  Order Service (port 8080)        Actuator (port 8081)                 │
│  ┌──────────────────────┐        ┌──────────────────────────────────┐  │
│  │  POST /orders        │        │  /actuator/health                │  │
│  │  GET  /orders/{id}   │        │    ├── db (PostgreSQL)           │  │
│  │  DELETE /orders/{id} │        │    ├── redis                     │  │
│  └──────────────────────┘        │    └── paymentGateway (custom)   │  │
│                                  │  /actuator/metrics/orders.placed │  │
│                                  │  /actuator/prometheus            │  │
│                                  └──────────────────────────────────┘  │
│                                           │                            │
│                                  ┌────────▼──────────┐                 │
│                                  │  Prometheus scrape │                 │
│                                  └────────┬──────────┘                 │
│                                  ┌────────▼──────────┐                 │
│                                  │  Grafana Dashboard │                 │
│                                  └───────────────────┘                 │
└────────────────────────────────────────────────────────────────────────┘
```

**Dependencies:**

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

**application.yml:**

```yaml
server:
  port: 8080

management:
  server:
    port: 8081
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,metrics,prometheus,loggers
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    tags:
      application: order-service
      environment: production
```

**Custom Health Indicator for payment gateway:**

```java
@Component("paymentGateway")
public class PaymentGatewayHealthIndicator implements HealthIndicator {

    private final PaymentGatewayClient gatewayClient;

    public PaymentGatewayHealthIndicator(PaymentGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @Override
    public Health health() {
        try {
            PingResponse response = gatewayClient.ping();
            if (response.isOk()) {
                return Health.up()
                        .withDetail("provider", "Stripe")
                        .withDetail("latencyMs", response.getLatencyMs())
                        .build();
            }
            return Health.down()
                        .withDetail("reason", "Gateway returned: " + response.getStatus())
                        .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

**Business metrics on the order service:**

```java
@Service
public class OrderService {

    private final Counter ordersPlaced;
    private final Counter ordersFailed;
    private final Timer  orderProcessingTime;

    public OrderService(MeterRegistry registry) {
        this.ordersPlaced = Counter.builder("orders.placed")
                .description("Total orders successfully placed")
                .tag("region", "us-east-1")
                .register(registry);

        this.ordersFailed = Counter.builder("orders.failed")
                .description("Total orders that failed")
                .register(registry);

        this.orderProcessingTime = Timer.builder("orders.processing.time")
                .description("Time taken to process an order")
                .register(registry);
    }

    public Order placeOrder(OrderRequest request) {
        return orderProcessingTime.record(() -> {
            try {
                Order order = processOrder(request);
                ordersPlaced.increment();
                return order;
            } catch (Exception e) {
                ordersFailed.increment();
                throw e;
            }
        });
    }
}
```

**Querying metrics:**

```bash
# List all metric names
GET http://localhost:8081/actuator/metrics

# Get total orders placed
GET http://localhost:8081/actuator/metrics/orders.placed

# Response:
# {
#   "name": "orders.placed",
#   "measurements": [{ "statistic": "COUNT", "value": 4821.0 }],
#   "availableTags": [{ "tag": "region", "values": ["us-east-1"] }]
# }

# Filter by tag
GET http://localhost:8081/actuator/metrics/orders.placed?tag=region:us-east-1
```

---

### Use Case 2: Debug Slow Endpoint in Production Without Restart

**Problem:** Requests to `/api/reports` are slow in production. You need DEBUG logging for the reporting package.

```bash
# Step 1 — Check current log level (no restart needed)
GET http://localhost:8081/actuator/loggers/com.example.reports

# Response:
# { "configuredLevel": null, "effectiveLevel": "INFO" }

# Step 2 — Enable DEBUG logging live
POST http://localhost:8081/actuator/loggers/com.example.reports
Content-Type: application/json

{ "configuredLevel": "DEBUG" }

# Step 3 — Reproduce the slow call, collect logs, then reset
POST http://localhost:8081/actuator/loggers/com.example.reports
Content-Type: application/json

{ "configuredLevel": "INFO" }
```

---

### Use Case 3: Kubernetes Liveness / Readiness Probe

**Scenario:** Your service uses a database connection pool. During startup the pool is not ready — Kubernetes must not route traffic until it is.

```java
// Signal "not ready" during startup, then flip to ready
@Component
public class DatabaseReadinessListener implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationContext context;

    public DatabaseReadinessListener(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
    }
}
```

```yaml
# k8s deployment snippet
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 20
  periodSeconds: 5
  failureThreshold: 2
```

---

## 9. `management.endpoints.web.base-path`

### What it is

`management.endpoints.web.base-path` defines the **URL prefix** under which all Actuator endpoints are served. The default value is `/actuator`.

```
base-path = /actuator

  → /actuator/health
  → /actuator/metrics
  → /actuator/info
  → ...
```

### Possible Values

| Value | Resulting URLs | When to use |
|---|---|---|
| `/actuator` *(default)* | `/actuator/health`, `/actuator/metrics` | Standard setup |
| `/manage` | `/manage/health`, `/manage/metrics` | Avoid conflict with `/actuator` path |
| `/ops` | `/ops/health`, `/ops/metrics` | Organisation-specific naming convention |
| `/` | `/health`, `/metrics` | Minimal path (risky — conflicts likely) |
| `/internal/actuator` | `/internal/actuator/health` | Deep nesting for proxy/gateway routing |

### Configuration

```yaml
management:
  endpoints:
    web:
      base-path: /manage          # change from /actuator to /manage
```

```yaml
management:
  server:
    port: 8081
  endpoints:
    web:
      base-path: /ops             # full URL: http://host:8081/ops/health
```

> **Important:** When `management.server.port` is different from `server.port`, the `base-path` applies to that separate management port only.

---

## 10. `management.endpoints.web.exposure.include`

### What it is

Controls **which endpoints are reachable over HTTP**. Being _enabled_ (existing) and being _exposed_ (reachable via HTTP) are two separate concepts in Actuator.

```
┌─────────────────────────────────────────────────────────┐
│   Endpoint Lifecycle                                     │
│                                                          │
│  enabled=true          exposed=true                      │
│  ┌─────────────┐       ┌──────────────────────────────┐ │
│  │  Endpoint   │──────►│  Reachable via HTTP / JMX    │ │
│  │  registered │       │  GET /actuator/health        │ │
│  └─────────────┘       └──────────────────────────────┘ │
│                                                          │
│  enabled=true          exposed=false (default for most) │
│  ┌─────────────┐       ┌──────────────────────────────┐ │
│  │  Endpoint   │──────►│  Exists but NOT reachable    │ │
│  │  registered │       │  via HTTP (404)              │ │
│  └─────────────┘       └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Default behaviour

| Endpoint | Exposed by default (HTTP) |
|---|---|
| `health` | Yes |
| `info` | Yes |
| All others | **No** |

### Possible Values

| Value | Meaning |
|---|---|
| `"*"` | Expose **all** enabled endpoints |
| `health` | Only `/actuator/health` |
| `health,info` | Only health and info |
| `health,info,metrics,prometheus,loggers` | Common production set |
| `health,info,metrics,env,beans,mappings` | Common dev/debug set |

### Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"              # all endpoints — only safe on internal port
        exclude: heapdump,env     # exclude sensitive ones even from "all"
```

```yaml
# Minimal production setup (recommended)
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

```yaml
# Developer / staging environment
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers,env,beans,mappings,scheduledtasks
```

> **Rule of thumb:** On the public-facing port use only `health` and `info`. On the internal management port (`management.server.port`) you can safely expose more.

---

## 11. All Built-in Actuator Endpoints — Complete Reference

```
┌──────────────────────────────────────────────────────────────────────────────┐
│             All Spring Boot Actuator Endpoints                               │
│                                                                              │
│  TIER 1 — Always safe to expose publicly                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  GET  /actuator/health            Overall health (UP/DOWN)          │    │
│  │  GET  /actuator/health/liveness   Kubernetes liveness probe        │    │
│  │  GET  /actuator/health/readiness  Kubernetes readiness probe       │    │
│  │  GET  /actuator/info              App name, version, git info      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  TIER 2 — Safe on internal/management port                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  GET  /actuator/metrics           List all metric names            │    │
│  │  GET  /actuator/metrics/{name}    Value of a specific metric       │    │
│  │  GET  /actuator/prometheus        Prometheus scrape format         │    │
│  │  GET  /actuator/loggers           All logger levels                │    │
│  │  POST /actuator/loggers/{name}    Change a logger level at runtime │    │
│  │  GET  /actuator/mappings          All @RequestMapping routes       │    │
│  │  GET  /actuator/beans             All Spring beans in context      │    │
│  │  GET  /actuator/scheduledtasks    Configured @Scheduled tasks      │    │
│  │  GET  /actuator/caches            Cache names and contents         │    │
│  │  DELETE /actuator/caches/{name}   Evict a specific cache           │    │
│  │  GET  /actuator/conditions        Auto-config evaluation report    │    │
│  │  GET  /actuator/configprops       All @ConfigurationProperties     │    │
│  │  GET  /actuator/flyway            Flyway migration history         │    │
│  │  GET  /actuator/liquibase         Liquibase changelog status       │    │
│  │  GET  /actuator/integrationgraph  Spring Integration flow graph    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  TIER 3 — Sensitive, restrict carefully                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  GET  /actuator/env               All env properties (may leak     │    │
│  │                                   secrets even with masking)       │    │
│  │  GET  /actuator/env/{toMatch}     Single property lookup           │    │
│  │  GET  /actuator/threaddump        JVM thread dump (plain text/JSON)│    │
│  │  GET  /actuator/heapdump          Binary JVM heap dump file        │    │
│  │  GET  /actuator/httptrace         Last 100 HTTP exchanges          │    │
│  │  POST /actuator/shutdown          Gracefully stop the application  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Detailed endpoint breakdown

#### `/actuator/health`

```bash
GET /actuator/health

# Minimal response (default, show-details=never)
{ "status": "UP" }

# Full response (show-details=always)
{
  "status": "UP",
  "components": {
    "db":        { "status": "UP",   "details": { "database": "PostgreSQL" } },
    "redis":     { "status": "UP",   "details": { "version": "7.0.12" } },
    "diskSpace": { "status": "UP",   "details": { "free": 91152891904 } }
  }
}
```

#### `/actuator/metrics` and `/actuator/metrics/{name}`

```bash
# List all available metric names
GET /actuator/metrics

{
  "names": [
    "jvm.memory.used",
    "jvm.gc.pause",
    "http.server.requests",
    "tomcat.threads.busy",
    "orders.placed"
  ]
}

# Get a specific metric
GET /actuator/metrics/http.server.requests

{
  "name": "http.server.requests",
  "measurements": [
    { "statistic": "COUNT",       "value": 8243.0 },
    { "statistic": "TOTAL_TIME",  "value": 412.5 },
    { "statistic": "MAX",         "value": 1.23 }
  ],
  "availableTags": [
    { "tag": "method", "values": ["GET", "POST"] },
    { "tag": "status", "values": ["200", "404", "500"] },
    { "tag": "uri",    "values": ["/api/orders", "/api/users"] }
  ]
}

# Filter by tag — only GET requests that returned 200
GET /actuator/metrics/http.server.requests?tag=method:GET&tag=status:200
```

#### `/actuator/threaddump`

```bash
GET /actuator/threaddump

{
  "threads": [
    {
      "threadName": "http-nio-8080-exec-1",
      "threadState": "WAITING",
      "stackTrace": [
        { "className": "sun.misc.Unsafe", "methodName": "park" },
        { "className": "java.util.concurrent.locks.LockSupport", "methodName": "park" }
      ]
    }
  ]
}
```

Useful to detect: thread starvation, deadlocks, threads stuck on I/O.

#### `/actuator/heapdump`

```bash
# Downloads a binary .hprof file — open with VisualVM, Eclipse MAT, or JProfiler
GET /actuator/heapdump
```

> This endpoint can produce files of several gigabytes on a production JVM. Protect it with authentication.

#### `/actuator/shutdown`

```bash
# Disabled by default — must explicitly enable
POST /actuator/shutdown

{ "message": "Shutting down, bye..." }
```

```yaml
management:
  endpoint:
    shutdown:
      enabled: true   # enable only if you have an authenticated internal port
```

---

## 12. Enabling Endpoints Selectively

### Two-phase control: _enable_ vs _expose_

```
management.endpoint.<id>.enabled=true/false   → Does the endpoint exist at all?
management.endpoints.web.exposure.include=...  → Is it reachable over HTTP?
```

### Pattern 1 — Disable all, then opt-in (most secure)

```yaml
management:
  endpoints:
    enabled-by-default: false          # disable every endpoint
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      enabled: true
    info:
      enabled: true
    prometheus:
      enabled: true
```

### Pattern 2 — Enable all, then opt-out

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
        exclude: env,beans,heapdump,shutdown   # explicitly block sensitive ones
  endpoint:
    shutdown:
      enabled: false    # disabled entirely, not just unexposed
    heapdump:
      enabled: false
```

### Pattern 3 — Per-environment profiles

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus

---
# application-dev.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    shutdown:
      enabled: false
```

### Pattern 4 — Separate port for sensitive endpoints

```yaml
management:
  server:
    port: 8081          # internal port — not exposed through the load balancer
  endpoints:
    web:
      exposure:
        include: "*"    # safe because port 8081 is firewalled

server:
  port: 8080            # public port — NO actuator endpoints here
```

```
┌───────────────────────────────────────────────────────────────────┐
│  Network boundary                                                  │
│                                                                    │
│  Internet / Load Balancer                                          │
│       │                                                            │
│       ▼  port 8080                                                 │
│  ┌─────────────┐      ┌──────────────────────────────────────┐    │
│  │  App (REST) │      │  Internal Network Only               │    │
│  │  /api/**    │      │  port 8081                           │    │
│  └─────────────┘      │  /actuator/health                    │    │
│                        │  /actuator/metrics                   │    │
│                        │  /actuator/heapdump   (restricted)  │    │
│                        └──────────────────────────────────────┘   │
│                                   ▲                                │
│                        Prometheus / k8s / DevOps tools             │
└───────────────────────────────────────────────────────────────────┘
```

---

## 13. All Built-in Actuator Endpoints — Category Reference

| Category | Endpoint | Default Enabled | Default Exposed (HTTP) | Description |
|---|---|---|---|---|
| **Health** | `health` | Yes | Yes | Aggregated app health |
| **Health** | `health/liveness` | Yes (k8s) | Yes (k8s) | Kubernetes liveness |
| **Health** | `health/readiness` | Yes (k8s) | Yes (k8s) | Kubernetes readiness |
| **Info** | `info` | Yes | Yes | App metadata |
| **Metrics** | `metrics` | Yes | No | List all metric names |
| **Metrics** | `metrics/{name}` | Yes | No | Single metric value |
| **Metrics** | `prometheus` | Yes* | No | Prometheus scrape format |
| **Logging** | `loggers` | Yes | No | View/change log levels |
| **Environment** | `env` | Yes | No | All config properties |
| **Environment** | `env/{toMatch}` | Yes | No | Single property lookup |
| **Context** | `beans` | Yes | No | All Spring beans |
| **Context** | `conditions` | Yes | No | Auto-config report |
| **Context** | `configprops` | Yes | No | `@ConfigurationProperties` |
| **Web** | `mappings` | Yes | No | All request mappings |
| **Web** | `httptrace` | Yes | No | Last 100 HTTP traces |
| **Threads** | `threaddump` | Yes | No | JVM thread dump |
| **Memory** | `heapdump` | Yes | No | JVM heap dump |
| **Tasks** | `scheduledtasks` | Yes | No | `@Scheduled` tasks |
| **Cache** | `caches` | Yes | No | Cache contents |
| **Flyway** | `flyway` | Yes* | No | Migration history |
| **Liquibase** | `liquibase` | Yes* | No | Changelog status |
| **Lifecycle** | `shutdown` | **No** | No | Graceful stop |
| **Integration** | `integrationgraph` | Yes* | No | Spring Integration graph |

> \* Only auto-configured when the relevant library (Prometheus, Flyway, Liquibase, Spring Integration) is on the classpath.

---

## 14. Custom Health Indicators — Cache, DB, and Downstream Services

### How the Health Endpoint Works Internally

```
┌─────────────────────────────────────────────────────────────────────────┐
│                   /actuator/health — Aggregation Flow                   │
│                                                                          │
│  GET /actuator/health                                                    │
│         │                                                                │
│         ▼                                                                │
│  ┌─────────────────────┐                                                 │
│  │   HealthEndpoint    │  collects all HealthIndicator beans             │
│  └──────────┬──────────┘                                                 │
│             │                                                            │
│    ┌────────┴─────────────────────────────────────────┐                 │
│    │                                                   │                │
│    ▼                                                   ▼                │
│  ┌──────────────────┐   ┌──────────────┐   ┌──────────────────────┐    │
│  │ DataSourceHealth │   │ RedisHealth  │   │ CustomCacheHealth    │    │
│  │ Indicator (auto) │   │ Indicator    │   │ Indicator (yours)    │    │
│  └──────────────────┘   └──────────────┘   └──────────────────────┘    │
│                                                                          │
│  StatusAggregator: if ANY is DOWN → overall status = DOWN               │
│                    if ALL are UP  → overall status = UP                 │
└─────────────────────────────────────────────────────────────────────────┘
```

### 14.1 Custom Database Health Indicator

Spring auto-configures `DataSourceHealthIndicator` for any `DataSource` bean. To add **additional named DB checks** (e.g., a read-replica):

```java
@Component("readReplicaDb")
public class ReadReplicaHealthIndicator implements HealthIndicator {

    private final DataSource readReplicaDataSource;

    public ReadReplicaHealthIndicator(
            @Qualifier("readReplicaDataSource") DataSource readReplicaDataSource) {
        this.readReplicaDataSource = readReplicaDataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = readReplicaDataSource.getConnection()) {
            boolean valid = conn.isValid(2);   // 2-second timeout
            if (valid) {
                return Health.up()
                        .withDetail("database", "PostgreSQL read-replica")
                        .withDetail("url", conn.getMetaData().getURL())
                        .build();
            }
            return Health.down().withDetail("reason", "Connection invalid").build();
        } catch (SQLException e) {
            return Health.down(e)
                        .withDetail("reason", "Cannot connect to read-replica")
                        .build();
        }
    }
}
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db":            { "status": "UP", "details": { "database": "PostgreSQL" } },
    "readReplicaDb": { "status": "UP", "details": { "database": "PostgreSQL read-replica" } }
  }
}
```

### 14.2 Custom Cache Health Indicator

```java
@Component("cache")
public class CacheHealthIndicator implements HealthIndicator {

    private final CacheManager cacheManager;

    public CacheHealthIndicator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Health health() {
        Collection<String> cacheNames = cacheManager.getCacheNames();

        if (cacheNames.isEmpty()) {
            return Health.unknown()
                        .withDetail("reason", "No caches configured")
                        .build();
        }

        Map<String, Object> details = new LinkedHashMap<>();
        boolean allHealthy = true;

        for (String name : cacheNames) {
            Cache cache = cacheManager.getCache(name);
            if (cache == null) {
                details.put(name, "MISSING");
                allHealthy = false;
            } else {
                // For Redis-backed caches, try a test put/get
                try {
                    cache.put("__health_check__", "ok");
                    String result = cache.get("__health_check__", String.class);
                    cache.evict("__health_check__");
                    details.put(name, "UP (" + result + ")");
                } catch (Exception e) {
                    details.put(name, "DOWN: " + e.getMessage());
                    allHealthy = false;
                }
            }
        }

        Health.Builder builder = allHealthy ? Health.up() : Health.down();
        details.forEach(builder::withDetail);
        return builder.build();
    }
}
```

### 14.3 Custom Downstream Service Health Indicator

```java
@Component("paymentService")
public class PaymentServiceHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;
    private final String       paymentServiceUrl;

    public PaymentServiceHealthIndicator(
            RestTemplate restTemplate,
            @Value("${payment.service.url}") String paymentServiceUrl) {
        this.restTemplate     = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Override
    public Health health() {
        String pingUrl = paymentServiceUrl + "/health";
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(pingUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                        .withDetail("url",    pingUrl)
                        .withDetail("status", response.getStatusCode().value())
                        .build();
            }
            return Health.down()
                        .withDetail("url",    pingUrl)
                        .withDetail("status", response.getStatusCode().value())
                        .build();
        } catch (ResourceAccessException e) {
            return Health.down()
                        .withDetail("url",    pingUrl)
                        .withDetail("reason", "Connection refused / timeout")
                        .build();
        }
    }
}
```

### 14.4 Composite Health Indicator — Group multiple checks

```java
@Configuration
public class HealthConfig {

    @Bean
    public CompositeHealthContributor externalServices(
            PaymentServiceHealthIndicator payment,
            ShippingServiceHealthIndicator shipping,
            NotificationServiceHealthIndicator notifications) {

        Map<String, HealthIndicator> map = new LinkedHashMap<>();
        map.put("payment",       payment);
        map.put("shipping",      shipping);
        map.put("notifications", notifications);

        return CompositeHealthContributor.fromMap(map);
    }
}
```

**Response:**
```json
{
  "status": "DOWN",
  "components": {
    "externalServices": {
      "status": "DOWN",
      "components": {
        "payment":       { "status": "UP"   },
        "shipping":      { "status": "DOWN", "details": { "reason": "Connection refused" } },
        "notifications": { "status": "UP"   }
      }
    }
  }
}
```

### 14.5 Health Groups (Spring Boot 2.3+)

Group related indicators and expose them independently (e.g., for Kubernetes probes vs full health):

```yaml
management:
  endpoint:
    health:
      group:
        readiness:
          include: db, redis, readReplicaDb   # only these checked for readiness
        liveness:
          include: ping                        # very fast check for liveness
        downstream:
          include: paymentService, shippingService
          show-details: always
```

```bash
GET /actuator/health/readiness    → { "status": "UP" }
GET /actuator/health/downstream   → full downstream component details
```

---

## 15. `health.show-details` — Configuration and Usage

### What it Controls

`management.endpoint.health.show-details` controls **who can see the component-level details** inside the health response.

```
show-details = never   →  { "status": "UP" }

show-details = always  →  {
                              "status": "UP",
                              "components": {
                                "db":        { "status": "UP", "details": {...} },
                                "redis":     { "status": "UP", "details": {...} },
                                "diskSpace": { "status": "UP", "details": {...} }
                              }
                            }
```

### All Possible Values

| Value | Behaviour | When to Use |
|---|---|---|
| `never` *(default)* | Only returns `{ "status": "UP/DOWN" }` — no component details | Public-facing health check (load balancer, uptime monitor) |
| `always` | Returns full component details to **everyone** | Internal management port, development |
| `when-authorized` | Returns details only to authenticated users with the `ACTUATOR` role | Production with Spring Security |

### Configuration Examples

```yaml
# application.yml

# Option 1: Always show details (dev/internal only)
management:
  endpoint:
    health:
      show-details: always

# Option 2: Hide from public, show to authorised users
management:
  endpoint:
    health:
      show-details: when-authorized
      roles: ACTUATOR_ADMIN           # custom role name

# Option 3: Separate behaviour per group
management:
  endpoint:
    health:
      show-details: never             # default for /actuator/health
      group:
        internal:
          include: "*"
          show-details: always        # /actuator/health/internal always detailed
        liveness:
          include: ping
          show-details: never
```

### `show-components` — Hide component names

```yaml
management:
  endpoint:
    health:
      show-components: never   # hides even the component names (not just details)
```

### Full Response Comparison

```bash
# show-details: never (default)
GET /actuator/health
→ { "status": "UP" }

# show-details: always
GET /actuator/health
→ {
    "status": "UP",
    "components": {
      "db": {
        "status": "UP",
        "details": {
          "database": "PostgreSQL",
          "validationQuery": "isValid()"
        }
      },
      "redis": {
        "status": "UP",
        "details": {
          "version": "7.0.12"
        }
      },
      "diskSpace": {
        "status": "UP",
        "details": {
          "total": 499963174912,
          "free":  91152891904,
          "threshold": 10485760,
          "path": "/app/."
        }
      },
      "paymentService": {
        "status": "UP",
        "details": {
          "url": "https://payments.internal/health",
          "status": 200
        }
      }
    }
  }
```

---

## 16. All Built-in Metrics — Complete Reference Table

Spring Boot Actuator auto-registers metrics through **Micrometer**. Below is the full set grouped by category.

### 16.1 JVM Memory Metrics

| Metric Name | Tags | Description |
|---|---|---|
| `jvm.memory.used` | `area` (heap/nonheap), `id` (pool name) | Bytes currently used |
| `jvm.memory.committed` | `area`, `id` | Bytes committed to the OS |
| `jvm.memory.max` | `area`, `id` | Max bytes available (`-1` if unlimited) |
| `jvm.buffer.count` | `id` | Estimated number of buffers |
| `jvm.buffer.memory.used` | `id` | Estimated memory used by buffers |
| `jvm.buffer.total.capacity` | `id` | Total buffer capacity |

```bash
GET /actuator/metrics/jvm.memory.used?tag=area:heap
# Shows heap usage only
```

### 16.2 JVM Garbage Collection Metrics

| Metric Name | Tags | Description |
|---|---|---|
| `jvm.gc.pause` | `action` (end of minor/major GC), `cause` | GC pause duration (Timer) |
| `jvm.gc.memory.promoted` | — | Bytes promoted from young → old gen |
| `jvm.gc.memory.allocated` | — | Bytes allocated in young gen since last GC |
| `jvm.gc.max.data.size` | — | Max size of old gen memory pool |
| `jvm.gc.live.data.size` | — | Size of long-lived object data after GC |
| `jvm.gc.overhead` | — | Percentage of time spent in GC |

```bash
GET /actuator/metrics/jvm.gc.pause
# Shows COUNT, TOTAL_TIME, MAX of GC pauses
```

### 16.3 JVM Thread Metrics

| Metric Name | Description |
|---|---|
| `jvm.threads.live` | Current number of live threads |
| `jvm.threads.daemon` | Current number of daemon threads |
| `jvm.threads.peak` | Peak number of live threads since JVM start |
| `jvm.threads.started` | Total threads ever created |
| `jvm.threads.states` | Thread count per state (NEW, RUNNABLE, BLOCKED, WAITING, etc.) |

### 16.4 JVM Class Loading Metrics

| Metric Name | Description |
|---|---|
| `jvm.classes.loaded` | Number of classes currently loaded |
| `jvm.classes.unloaded` | Total classes unloaded since start |

### 16.5 System / OS Metrics

| Metric Name | Description |
|---|---|
| `system.cpu.count` | Number of available CPU cores |
| `system.cpu.usage` | Recent CPU usage of the whole system (0.0–1.0) |
| `process.cpu.usage` | Recent CPU usage of the JVM process (0.0–1.0) |
| `process.uptime` | Uptime of the JVM process in seconds |
| `process.start.time` | Start time of the JVM (epoch seconds) |
| `process.files.open` | Open file descriptors (Linux/macOS) |
| `process.files.max` | Maximum allowed open file descriptors |

### 16.6 HTTP Server Metrics (Spring MVC / WebFlux)

| Metric Name | Tags | Description |
|---|---|---|
| `http.server.requests` | `method`, `uri`, `status`, `exception`, `outcome` | All HTTP request stats (Counter + Timer) |
| `http.server.requests.active` | `method`, `uri` | Currently active requests (Gauge) |

```bash
# Average response time for POST /api/orders returning 200
GET /actuator/metrics/http.server.requests?tag=method:POST&tag=uri:/api/orders&tag=status:200

{
  "measurements": [
    { "statistic": "COUNT",      "value": 4821.0 },
    { "statistic": "TOTAL_TIME", "value": 963.4  },
    { "statistic": "MAX",        "value": 2.31   }
  ]
}
# Average = TOTAL_TIME / COUNT = 963.4 / 4821 ≈ 0.2s per request
```

### 16.7 Tomcat Metrics

| Metric Name | Tags | Description |
|---|---|---|
| `tomcat.sessions.active.current` | — | Currently active HTTP sessions |
| `tomcat.sessions.active.max` | — | Maximum concurrent sessions ever |
| `tomcat.sessions.created` | — | Total sessions created |
| `tomcat.sessions.expired` | — | Total sessions expired |
| `tomcat.sessions.rejected` | — | Sessions rejected (max limit reached) |
| `tomcat.threads.busy` | `name` (thread pool) | Threads currently handling requests |
| `tomcat.threads.current` | `name` | Total threads in the pool |
| `tomcat.threads.config.max` | `name` | Configured max threads |
| `tomcat.global.request` | `name` | Total request time and count |
| `tomcat.global.received` | `name` | Total bytes received |
| `tomcat.global.sent` | `name` | Total bytes sent |
| `tomcat.global.error` | `name` | Total error count |
| `tomcat.connections.current` | `name` | Current open connections |
| `tomcat.connections.keepalive.current` | `name` | Current keep-alive connections |
| `tomcat.connections.config.max` | `name` | Configured max connections |

### 16.8 JDBC / DataSource Metrics

| Metric Name | Tags | Description |
|---|---|---|
| `jdbc.connections.active` | `name` (datasource bean) | Connections currently in use |
| `jdbc.connections.idle` | `name` | Idle connections in the pool |
| `jdbc.connections.min` | `name` | Minimum pool size |
| `jdbc.connections.max` | `name` | Maximum pool size |
| `hikaricp.connections` | `pool` | Total connections (HikariCP specific) |
| `hikaricp.connections.active` | `pool` | Active HikariCP connections |
| `hikaricp.connections.idle` | `pool` | Idle HikariCP connections |
| `hikaricp.connections.pending` | `pool` | Threads awaiting a connection |
| `hikaricp.connections.acquire` | `pool` | Time to acquire a connection (Timer) |
| `hikaricp.connections.usage` | `pool` | Time connection is held (Timer) |
| `hikaricp.connections.creation` | `pool` | Time to create a new connection (Timer) |
| `hikaricp.connections.timeout.total` | `pool` | Total connection timeout exceptions |

### 16.9 Cache Metrics (Spring Cache / Caffeine / Redis)

| Metric Name | Tags | Description |
|---|---|---|
| `cache.gets` | `name`, `result` (hit/miss), `cache.manager` | Cache lookup count |
| `cache.puts` | `name`, `cache.manager` | Cache put count |
| `cache.evictions` | `name`, `cache.manager` | Cache eviction count |
| `cache.size` | `name`, `cache.manager` | Current cache size (if supported) |

### 16.10 Scheduler / Task Metrics

| Metric Name | Tags | Description |
|---|---|---|
| `executor.active` | `name` | Active threads in an executor |
| `executor.completed` | `name` | Completed tasks |
| `executor.pool.size` | `name` | Current pool size |
| `executor.pool.max` | `name` | Max pool size |
| `executor.pool.core` | `name` | Core pool size |
| `executor.queued` | `name` | Tasks queued |
| `executor.queue.remaining` | `name` | Remaining queue capacity |
| `scheduled.tasks` | `name` | Count of scheduled task executions |

### 16.11 Logback Metrics

| Metric Name | Tags | Description |
|---|---|---|
| `logback.events` | `level` (trace/debug/info/warn/error) | Log events per level |

```bash
# See how many ERROR logs have been emitted
GET /actuator/metrics/logback.events?tag=level:error
```

### 16.12 Summary Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    Metrics Available in /actuator/metrics                │
│                                                                          │
│  JVM Memory        jvm.memory.used / committed / max                    │
│  JVM GC            jvm.gc.pause / promoted / allocated / overhead       │
│  JVM Threads       jvm.threads.live / daemon / peak / states            │
│  JVM Classes       jvm.classes.loaded / unloaded                        │
│  System            system.cpu.usage / process.cpu.usage / uptime        │
│  HTTP              http.server.requests (count, time, max) per route    │
│  Tomcat            threads.busy / connections / sessions / bytes        │
│  JDBC/HikariCP     connections.active / idle / pending / acquire time   │
│  Cache             gets (hit/miss) / puts / evictions / size            │
│  Executor          pool.size / active / queued / completed              │
│  Logback           events per log level                                 │
│  Custom            any Counter / Gauge / Timer / Summary you register  │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 17. Why `heapdump` and `shutdown` Are Blocked by Default — and How to Secure Them

### 17.1 Why They Are Blocked

```
┌─────────────────────────────────────────────────────────────────────────┐
│              Security Risk Analysis                                      │
│                                                                          │
│  /actuator/heapdump                                                      │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ What it exposes:                                                 │   │
│  │  • Full JVM heap snapshot (binary .hprof file)                  │   │
│  │  • ALL objects in memory: Strings, byte[], POJOs                │   │
│  │  • Secrets in plaintext: passwords, API keys, JWT tokens,       │   │
│  │    database credentials still buffered in memory                │   │
│  │  • Customer PII: names, emails, credit card data in-flight      │   │
│  │                                                                  │   │
│  │ Extra concern: files can be 512 MB – 4+ GB in production        │   │
│  │ → DoS risk from a single unauthenticated request                │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  /actuator/shutdown                                                      │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ What it does:                                                    │   │
│  │  • Calls System.exit() / context.close() on the running JVM     │   │
│  │  • Takes down the entire application immediately                │   │
│  │  • One unauthenticated POST = instant outage                    │   │
│  │  • In k8s: pod crashes → restartPolicy reschedules it, but      │   │
│  │    all in-flight requests are aborted                           │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  Default state:                                                          │
│    heapdump  → enabled=true,  exposed=false  (404 without config)       │
│    shutdown  → enabled=false, exposed=false  (404 + no-op)              │
└─────────────────────────────────────────────────────────────────────────┘
```

### 17.2 Step-by-Step: Enabling `heapdump` Safely

#### Step 1 — Move Actuator to a separate internal port

```yaml
management:
  server:
    port: 8081        # This port must NOT be exposed through the load balancer
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,heapdump
```

#### Step 2 — Add Spring Security to the management server

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

#### Step 3 — Secure with role-based access

```java
@Configuration
@Order(1)
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(auth -> auth
                // Public: health and info only
                .requestMatchers(EndpointRequest.to(
                        HealthEndpoint.class,
                        InfoEndpoint.class))
                    .permitAll()
                // Heapdump: only ADMIN role
                .requestMatchers(EndpointRequest.to(HeapDumpWebEndpoint.class))
                    .hasRole("HEAPDUMP_ADMIN")
                // Everything else: ACTUATOR role
                .anyRequest()
                    .hasRole("ACTUATOR")
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    EndpointRequest.toAnyEndpoint()));  // actuator uses non-browser clients
        return http.build();
    }
}
```

#### Step 4 — Store credentials in environment variables

```yaml
spring:
  security:
    user:
      name: ${ACTUATOR_USER}             # from environment / k8s secret
      password: ${ACTUATOR_PASSWORD}
      roles: ACTUATOR
```

For production with multiple roles, use an `InMemoryUserDetailsManager` or integrate with your identity provider.

```java
@Bean
public UserDetailsService actuatorUsers() {
    UserDetails actuatorAdmin = User.withDefaultPasswordEncoder()
            .username(actuatorAdminUser)
            .password(actuatorAdminPassword)
            .roles("ACTUATOR", "HEAPDUMP_ADMIN")
            .build();

    UserDetails monitorUser = User.withDefaultPasswordEncoder()
            .username(monitorUser)
            .password(monitorPassword)
            .roles("ACTUATOR")
            .build();

    return new InMemoryUserDetailsManager(actuatorAdmin, monitorUser);
}
```

> **Never use `withDefaultPasswordEncoder()` in production** — it uses BCrypt but logs a deprecation warning. Use `PasswordEncoderFactories.createDelegatingPasswordEncoder()` with externally stored credentials.

### 17.3 Step-by-Step: Enabling `shutdown` Safely

`shutdown` is **disabled at the endpoint level** by default (not just unexposed). Two steps are required:

```yaml
# Step 1 — Enable the endpoint itself
management:
  endpoint:
    shutdown:
      enabled: true

# Step 2 — Expose it (only on the internal management port)
  endpoints:
    web:
      exposure:
        include: shutdown
  server:
    port: 8081
```

```java
// Step 3 — Require a dedicated role for shutdown
.requestMatchers(EndpointRequest.to(ShutdownEndpoint.class))
    .hasRole("SHUTDOWN_ADMIN")
```

```bash
# Only works with valid credentials
POST http://localhost:8081/actuator/shutdown
Authorization: Basic c2h1dGRvd25hZG1pbjpTM2NyZXQ=

{ "message": "Shutting down, bye..." }
```

### 17.4 Production Security Configuration — Complete Example

```yaml
# application-prod.yml
management:
  server:
    port: 8081
    ssl:
      enabled: true                     # mTLS on the management port (optional)
  endpoints:
    enabled-by-default: false           # start with nothing
    web:
      base-path: /actuator
      exposure:
        include: health,info,prometheus  # only these are HTTP-reachable
  endpoint:
    health:
      enabled: true
      show-details: when-authorized
    info:
      enabled: true
    prometheus:
      enabled: true
    heapdump:
      enabled: false                    # off in production
    shutdown:
      enabled: false                    # off in production
```

```java
@Configuration
public class ProductionActuatorSecurity {

    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(EndpointRequest.to(
                        HealthEndpoint.class, InfoEndpoint.class))
                    .permitAll()
                .requestMatchers(EndpointRequest.to(PrometheusScrapeEndpoint.class))
                    .hasIpAddress("10.0.0.0/8")   // only allow internal Prometheus IP range
                .anyRequest()
                    .hasRole("ACTUATOR_ADMIN")
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(EndpointRequest.toAnyEndpoint()));
        return http.build();
    }
}
```

### 17.5 Summary: heapdump and shutdown Access Matrix

| Scenario | heapdump | shutdown |
|---|---|---|
| Default (no config) | 404 — unexposed | 404 — disabled + unexposed |
| `exposure.include=*` only | Accessible with no auth | 404 — still disabled |
| `endpoint.shutdown.enabled=true` + exposed | — | Accessible with no auth (dangerous!) |
| Internal port + Spring Security `ROLE_ADMIN` | Requires auth | Requires auth |
| Recommended production | `enabled: false` | `enabled: false` |

---

## 18. Custom Actuator Endpoints

### 18.1 Why Create Custom Endpoints?

Built-in Actuator endpoints cover JVM, HTTP, and infrastructure. Custom endpoints let you expose **domain-specific operational data** — feature flags, circuit-breaker states, cache stats, or application-specific commands — through the same secure Actuator infrastructure.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                   Custom Endpoint Anatomy                                │
│                                                                          │
│  @Component                                                              │
│  @Endpoint(id = "feature-flags")          ← registers as /actuator/feature-flags
│  class FeatureFlagEndpoint {                                             │
│                                                                          │
│    @ReadOperation    → HTTP GET    → safe, no side effects               │
│    @WriteOperation   → HTTP POST   → mutates state, needs auth           │
│    @DeleteOperation  → HTTP DELETE → removes state, needs auth           │
│                                                                          │
│    @Selector         → path variable  /actuator/feature-flags/{name}    │
│  }                                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### 18.2 Core Annotations

| Annotation | HTTP Method | Purpose |
|---|---|---|
| `@Endpoint(id)` | — | Declares the class as an Actuator endpoint; `id` becomes the URL segment |
| `@ReadOperation` | GET | Read-only query; safe to call repeatedly |
| `@WriteOperation` | POST | Mutates application state |
| `@DeleteOperation` | DELETE | Removes/resets state |
| `@Selector` | — | Binds a method parameter to a path segment |
| `@WebEndpoint` | — | Variant of `@Endpoint` that is HTTP-only (not JMX) |
| `@JmxEndpoint` | — | Variant that is JMX-only |

---

### 18.3 `@Endpoint(id)` — Declaring the Endpoint

`id` must be:
- lowercase letters and hyphens only
- unique across all endpoints
- mapped to `/actuator/{id}` by Actuator automatically

```java
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "feature-flags")   // accessible at GET /actuator/feature-flags
public class FeatureFlagEndpoint {
    // operations go here
}
```

To expose it:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,feature-flags
```

---

### 18.4 `@ReadOperation` — HTTP GET

Maps to **HTTP GET**. Should have no side effects. Can return any serialisable object — Actuator converts it to JSON automatically.

```java
@Component
@Endpoint(id = "feature-flags")
public class FeatureFlagEndpoint {

    // In-memory store (replace with DB/config server in production)
    private final Map<String, Boolean> flags = new ConcurrentHashMap<>(Map.of(
            "new-checkout",   false,
            "dark-mode",      true,
            "beta-dashboard", false
    ));

    // GET /actuator/feature-flags
    // Returns all flags
    @ReadOperation
    public Map<String, Boolean> getAllFlags() {
        return Collections.unmodifiableMap(flags);
    }

    // GET /actuator/feature-flags/new-checkout
    // Returns a single flag by name
    @ReadOperation
    public FlagDetail getFlag(@Selector String name) {
        Boolean enabled = flags.get(name);
        if (enabled == null) {
            return null;   // Actuator returns 404 when null is returned
        }
        return new FlagDetail(name, enabled);
    }

    public record FlagDetail(String name, boolean enabled) {}
}
```

**Calling the endpoint:**

```bash
# All flags
GET /actuator/feature-flags
→ { "new-checkout": false, "dark-mode": true, "beta-dashboard": false }

# Single flag
GET /actuator/feature-flags/dark-mode
→ { "name": "dark-mode", "enabled": true }

# Non-existent flag → 404
GET /actuator/feature-flags/unknown
→ 404 Not Found
```

---

### 18.5 `@WriteOperation` — HTTP POST

Maps to **HTTP POST**. Used to **mutate** application state. Parameters are read from the **request body** (JSON) — not query params or path variables (except `@Selector` which is a path segment).

```java
@Component
@Endpoint(id = "feature-flags")
public class FeatureFlagEndpoint {

    private final Map<String, Boolean> flags = new ConcurrentHashMap<>();

    // POST /actuator/feature-flags/dark-mode
    // Body: { "enabled": true }
    @WriteOperation
    public WriteResult setFlag(@Selector String name, boolean enabled) {
        flags.put(name, enabled);
        return new WriteResult("updated", name, enabled);
    }

    // POST /actuator/feature-flags
    // Body: { "flags": { "dark-mode": true, "new-checkout": false } }
    @WriteOperation
    public WriteResult bulkUpdate(Map<String, Boolean> flags) {
        this.flags.putAll(flags);
        return new WriteResult("bulk-updated", null, null);
    }

    public record WriteResult(String action, String name, Boolean enabled) {}
}
```

**Calling the endpoint:**

```bash
POST /actuator/feature-flags/dark-mode
Content-Type: application/vnd.spring-boot.actuator.v3+json

{ "enabled": false }

→ { "action": "updated", "name": "dark-mode", "enabled": false }
```

> **Note:** Actuator uses the content type `application/vnd.spring-boot.actuator.v3+json` for write operations. Most HTTP clients also accept plain `application/json`.

---

### 18.6 `@DeleteOperation` — HTTP DELETE

Maps to **HTTP DELETE**. Used to **remove or reset** a piece of state. Typically combined with `@Selector` to target a specific resource.

```java
@Component
@Endpoint(id = "feature-flags")
public class FeatureFlagEndpoint {

    private final Map<String, Boolean> flags = new ConcurrentHashMap<>();

    // DELETE /actuator/feature-flags/new-checkout
    // Removes the flag entirely
    @DeleteOperation
    public DeleteResult removeFlag(@Selector String name) {
        Boolean previous = flags.remove(name);
        if (previous == null) {
            return new DeleteResult("not-found", name);
        }
        return new DeleteResult("removed", name);
    }

    // DELETE /actuator/feature-flags
    // Resets ALL flags to defaults
    @DeleteOperation
    public DeleteResult resetAll() {
        flags.clear();
        return new DeleteResult("reset-all", null);
    }

    public record DeleteResult(String action, String name) {}
}
```

**Calling the endpoint:**

```bash
DELETE /actuator/feature-flags/new-checkout
→ { "action": "removed", "name": "new-checkout" }

DELETE /actuator/feature-flags
→ { "action": "reset-all", "name": null }
```

---

### 18.7 Complete Custom Endpoint — Feature Flags with All Operations

```java
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Endpoint(id = "feature-flags")
public class FeatureFlagEndpoint {

    private final Map<String, Boolean> flags = new ConcurrentHashMap<>(Map.of(
            "new-checkout",   false,
            "dark-mode",      true,
            "beta-dashboard", false
    ));

    // ── READ ───────────────────────────────────────────────────────────────

    @ReadOperation                          // GET /actuator/feature-flags
    public Map<String, Boolean> getAll() {
        return Collections.unmodifiableMap(flags);
    }

    @ReadOperation                          // GET /actuator/feature-flags/{name}
    public FlagDetail get(@Selector String name) {
        Boolean enabled = flags.get(name);
        return (enabled != null) ? new FlagDetail(name, enabled) : null;
    }

    // ── WRITE ──────────────────────────────────────────────────────────────

    @WriteOperation                         // POST /actuator/feature-flags/{name}
    public FlagDetail set(@Selector String name, boolean enabled) {
        flags.put(name, enabled);
        return new FlagDetail(name, enabled);
    }

    // ── DELETE ─────────────────────────────────────────────────────────────

    @DeleteOperation                        // DELETE /actuator/feature-flags/{name}
    public ActionResult remove(@Selector String name) {
        return flags.remove(name) != null
                ? new ActionResult("removed", name)
                : new ActionResult("not-found", name);
    }

    @DeleteOperation                        // DELETE /actuator/feature-flags
    public ActionResult reset() {
        flags.clear();
        return new ActionResult("reset", "all");
    }

    // ── Records ────────────────────────────────────────────────────────────

    public record FlagDetail(String name, boolean enabled) {}
    public record ActionResult(String action, String target) {}
}
```

**HTTP mapping summary:**

```
GET    /actuator/feature-flags            → getAllFlags()
GET    /actuator/feature-flags/{name}     → getFlag(name)
POST   /actuator/feature-flags/{name}     → setFlag(name, enabled)
DELETE /actuator/feature-flags/{name}     → removeFlag(name)
DELETE /actuator/feature-flags            → resetAll()
```

---

### 18.8 Real-Life Use Cases for Custom Endpoints

#### Use Case A — Circuit Breaker State Inspector

```java
@Component
@Endpoint(id = "circuit-breakers")
public class CircuitBreakerEndpoint {

    private final CircuitBreakerRegistry registry;

    public CircuitBreakerEndpoint(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @ReadOperation
    public Map<String, Object> getAllStates() {
        Map<String, Object> result = new LinkedHashMap<>();
        registry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.Metrics m = cb.getMetrics();
            result.put(cb.getName(), Map.of(
                    "state",               cb.getState().name(),
                    "failureRate",         m.getFailureRate(),
                    "slowCallRate",        m.getSlowCallRate(),
                    "bufferedCalls",       m.getNumberOfBufferedCalls(),
                    "failedCalls",         m.getNumberOfFailedCalls(),
                    "successfulCalls",     m.getNumberOfSuccessfulCalls()
            ));
        });
        return result;
    }

    @WriteOperation   // Force-reset a specific circuit breaker to CLOSED state
    public Map<String, String> reset(@Selector String name) {
        CircuitBreaker cb = registry.circuitBreaker(name);
        cb.reset();
        return Map.of("name", name, "newState", cb.getState().name());
    }
}
```

```bash
GET /actuator/circuit-breakers
→ {
    "paymentService": { "state": "OPEN", "failureRate": 75.0, ... },
    "inventoryService": { "state": "CLOSED", "failureRate": 0.0, ... }
  }

POST /actuator/circuit-breakers/paymentService    (no body needed)
→ { "name": "paymentService", "newState": "CLOSED" }
```

#### Use Case B — Cache Management Endpoint

```java
@Component
@Endpoint(id = "cache-manager")
public class CacheManagerEndpoint {

    private final CacheManager cacheManager;

    public CacheManagerEndpoint(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @ReadOperation
    public Collection<String> listCaches() {
        return cacheManager.getCacheNames();
    }

    @DeleteOperation                          // DELETE /actuator/cache-manager/{cacheName}
    public ActionResult evictCache(@Selector String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return new ActionResult("not-found", cacheName);
        cache.clear();
        return new ActionResult("evicted", cacheName);
    }

    @DeleteOperation                          // DELETE /actuator/cache-manager
    public ActionResult evictAll() {
        cacheManager.getCacheNames()
                    .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
        return new ActionResult("evicted-all", "all caches");
    }

    public record ActionResult(String action, String target) {}
}
```

---

### 18.9 Why `@WriteOperation` and `@DeleteOperation` Require Authentication

```
┌──────────────────────────────────────────────────────────────────────────┐
│           Why Write/Delete Operations Must Be Protected                  │
│                                                                          │
│  @ReadOperation  (GET)                                                   │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ • No side effects — reads state only                              │  │
│  │ • Worst case: information disclosure (manage via role)            │  │
│  │ • Safe for Prometheus, k8s probes, monitoring dashboards          │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  @WriteOperation (POST)  /  @DeleteOperation (DELETE)                    │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ • Mutates live application state                                  │  │
│  │ • Examples of what an attacker can do without auth:              │  │
│  │    → Enable a feature flag that bypasses payment validation       │  │
│  │    → Force-close a circuit breaker hiding cascading failures      │  │
│  │    → Evict all caches → database gets hammered with cold queries  │  │
│  │    → Reset rate-limiter counters → bypass throttling              │  │
│  │    → Change log level to TRACE → disk fills, DoS                 │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

Even the **built-in** `@WriteOperation` endpoints follow this — `POST /actuator/loggers/{name}` and `DELETE /actuator/caches/{name}` require authentication for the same reason.

---

### 18.10 Adding Authentication for Custom Endpoints

#### Option 1 — Role-based access for write/delete operations only

```java
@Configuration
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(auth -> auth
                // Public read access to health and info
                .requestMatchers(EndpointRequest.to(
                        HealthEndpoint.class, InfoEndpoint.class))
                    .permitAll()

                // Custom endpoint READ (GET) — allow monitoring role
                .requestMatchers(HttpMethod.GET, "/actuator/feature-flags/**")
                    .hasAnyRole("MONITORING", "OPS_ADMIN")

                // Custom endpoint WRITE/DELETE — ops admin only
                .requestMatchers(HttpMethod.POST,   "/actuator/feature-flags/**")
                    .hasRole("OPS_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/actuator/feature-flags/**")
                    .hasRole("OPS_ADMIN")

                // Everything else — require at least ACTUATOR role
                .anyRequest()
                    .hasRole("ACTUATOR")
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    EndpointRequest.toAnyEndpoint()));
        return http.build();
    }
}
```

#### Option 2 — Method-level security on the endpoint operations

```java
@Component
@Endpoint(id = "feature-flags")
public class FeatureFlagEndpoint {

    @ReadOperation
    public Map<String, Boolean> getAll() {
        // No security annotation needed — read is generally safe
        return flags;
    }

    @WriteOperation
    @PreAuthorize("hasRole('OPS_ADMIN')")   // Spring Method Security
    public FlagDetail set(@Selector String name, boolean enabled) {
        flags.put(name, enabled);
        return new FlagDetail(name, enabled);
    }

    @DeleteOperation
    @PreAuthorize("hasRole('OPS_ADMIN')")
    public ActionResult remove(@Selector String name) {
        flags.remove(name);
        return new ActionResult("removed", name);
    }
}
```

Enable method security in config:

```java
@Configuration
@EnableMethodSecurity   // enables @PreAuthorize, @PostAuthorize
public class MethodSecurityConfig { }
```

#### Option 3 — Audit write/delete operations

```java
@Component
@Endpoint(id = "feature-flags")
public class FeatureFlagEndpoint {

    private final AuditEventRepository auditRepository;

    @WriteOperation
    @PreAuthorize("hasRole('OPS_ADMIN')")
    public FlagDetail set(@Selector String name, boolean enabled,
                          Authentication authentication) {  // Spring injects caller identity
        flags.put(name, enabled);

        // Record who changed what and when
        auditRepository.add(new AuditEvent(
                authentication.getName(),
                "FEATURE_FLAG_CHANGED",
                Map.of("flag", name, "enabled", enabled)
        ));

        return new FlagDetail(name, enabled);
    }
}
```

#### Option 4 — Full security configuration with user store

```yaml
# application.yml
management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: health,info,feature-flags,circuit-breakers
```

```java
@Configuration
public class ActuatorUserConfig {

    @Value("${actuator.monitoring.password}")
    private String monitoringPassword;

    @Value("${actuator.ops.password}")
    private String opsPassword;

    @Bean
    public UserDetailsService actuatorUsers(PasswordEncoder encoder) {
        UserDetails monitoring = User.builder()
                .username("monitoring")
                .password(encoder.encode(monitoringPassword))
                .roles("MONITORING")
                .build();

        UserDetails opsAdmin = User.builder()
                .username("ops-admin")
                .password(encoder.encode(opsPassword))
                .roles("MONITORING", "OPS_ADMIN")
                .build();

        return new InMemoryUserDetailsManager(monitoring, opsAdmin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

```yaml
# k8s secret (base64 encoded values)
apiVersion: v1
kind: Secret
metadata:
  name: actuator-credentials
type: Opaque
data:
  monitoring-password: bW9uaXRvcmluZ1BAc3N3b3Jk
  ops-password: T3BzQWRtaW5QQHNzd29yZA==
```

---

### 18.11 Custom Endpoint — HTTP Method Mapping Summary

```
┌──────────────────────────────────────────────────────────────────────────┐
│          @Endpoint Annotation → HTTP Method → Auth Requirement           │
│                                                                          │
│  @ReadOperation                                                          │
│    Method:  GET                                                          │
│    URL:     /actuator/{id}           (no @Selector)                     │
│             /actuator/{id}/{value}   (with @Selector)                   │
│    Auth:    Optional — use MONITORING role for sensitive reads           │
│    Use for: querying state, listing resources                           │
│                                                                          │
│  @WriteOperation                                                         │
│    Method:  POST                                                         │
│    URL:     /actuator/{id}           (no @Selector)                     │
│             /actuator/{id}/{value}   (with @Selector)                   │
│    Body:    JSON request body (parameters mapped by name)               │
│    Auth:    REQUIRED — always protect with OPS_ADMIN role               │
│    Use for: updating state, enabling/disabling features                 │
│                                                                          │
│  @DeleteOperation                                                        │
│    Method:  DELETE                                                       │
│    URL:     /actuator/{id}           (no @Selector)                     │
│             /actuator/{id}/{value}   (with @Selector)                   │
│  @DeleteOperation                                                        │
│    Method:  DELETE                                                       │
│    URL:     /actuator/{id}           (no @Selector)                     │
│             /actuator/{id}/{value}   (with @Selector)                   │
│    Auth:    REQUIRED — always protect with OPS_ADMIN role               │
│    Use for: removing state, evicting caches, resetting counters         │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 19. Pushing Actuator Metrics to Prometheus and Grafana

### 19.1 Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Spring Boot → Prometheus → Grafana Pipeline               │
│                                                                              │
│  Spring Boot App                                                             │
│  ┌───────────────────────────────┐                                           │
│  │  Micrometer                   │                                           │
│  │  (metrics registry)           │                                           │
│  │   ↓                           │                                           │
│  │  PrometheusMeterRegistry      │                                           │
│  │   ↓                           │                                           │
│  │  GET /actuator/prometheus     │◄─── Prometheus scrapes every 15s         │
│  └───────────────────────────────┘                                           │
│                                          │                                   │
│                               ┌──────────▼───────────┐                      │
│                               │     Prometheus        │                      │
│                               │  (time-series DB)     │                      │
│                               │  stores & queries     │                      │
│                               │  metrics with PromQL  │                      │
│                               └──────────┬───────────┘                      │
│                                          │                                   │
│                               ┌──────────▼───────────┐                      │
│                               │      Grafana          │                      │
│                               │  (visualisation)      │                      │
│                               │  dashboards, alerts   │                      │
│                               └───────────────────────┘                     │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Key concept — Prometheus uses a _pull_ model:**
- Prometheus calls `GET /actuator/prometheus` on your app at a configured interval.
- Your app does NOT push metrics to Prometheus.
- The `/actuator/prometheus` endpoint exposes all registered Micrometer meters in the Prometheus text format.

---

### 19.2 Step 1 — Add Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus registry — converts Micrometer meters to Prometheus format -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Gradle:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

---

### 19.3 Step 2 — Configure Spring Boot

```yaml
# application.yml
management:
  server:
    port: 8081                        # Actuator on a separate internal port
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        descriptions: true            # include HELP text in output
        step: 1m                      # how often counters are reset for rate calculations
    tags:
      application: ${spring.application.name}   # global tag on every metric
      environment: production
      region: us-east-1
```

**Verify the endpoint:**

```bash
GET http://localhost:8081/actuator/prometheus

# Expected output (Prometheus text format):
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{application="order-service",area="heap",id="G1 Eden Space",} 2.7262976E7
jvm_memory_used_bytes{application="order-service",area="nonheap",id="Metaspace",} 6.7108864E7
# HELP http_server_requests_seconds Duration of HTTP server request handling
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{application="order-service",method="GET",status="200",uri="/api/orders",} 4821.0
http_server_requests_seconds_sum{application="order-service",method="GET",status="200",uri="/api/orders",} 963.4
```

---

### 19.4 Step 3 — Configure Prometheus to Scrape Your App

#### Static configuration (`prometheus.yml`)

```yaml
# prometheus.yml
global:
  scrape_interval: 15s       # scrape all targets every 15 seconds
  evaluation_interval: 15s   # evaluate alerting rules every 15 seconds

scrape_configs:

  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'order-service:8081'    # host:management-port
        labels:
          service: order-service
          env: production

  - job_name: 'payment-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'payment-service:8081'
        labels:
          service: payment-service
          env: production
```

#### Kubernetes ServiceMonitor (Prometheus Operator)

```yaml
# service-monitor.yaml — used when running Prometheus Operator in k8s
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: order-service-monitor
  namespace: monitoring
  labels:
    release: prometheus          # must match the Prometheus Operator selector
spec:
  selector:
    matchLabels:
      app: order-service
  endpoints:
    - port: management           # the named port in your Service
      path: /actuator/prometheus
      interval: 15s
      scheme: http
```

```yaml
# k8s Service — expose management port
apiVersion: v1
kind: Service
metadata:
  name: order-service
  labels:
    app: order-service
spec:
  ports:
    - name: http
      port: 8080
      targetPort: 8080
    - name: management           # named port referenced by ServiceMonitor
      port: 8081
      targetPort: 8081
  selector:
    app: order-service
```

---

### 19.5 Step 4 — Run with Docker Compose (Local Development)

```yaml
# docker-compose.yml
version: '3.9'

services:

  order-service:
    image: order-service:latest
    ports:
      - "8080:8080"
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: dev
    networks:
      - monitoring

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=15d'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}   # from .env file
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
    depends_on:
      - prometheus
    networks:
      - monitoring

networks:
  monitoring:
    driver: bridge

volumes:
  grafana-data:
```

```yaml
# prometheus.yml for docker-compose
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['order-service:8081']
```

---

### 19.6 Step 5 — Add Grafana Data Source

Grafana must be told where Prometheus is. Either via UI or provisioning file:

```yaml
# grafana/provisioning/datasources/prometheus.yml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

---

### 19.7 Querying Metrics in Prometheus (PromQL)

PromQL is the query language Prometheus uses. Metrics that Spring Boot exposes as `jvm_memory_used_bytes` (dots replaced with underscores, plus `_total` suffix for counters).

**Micrometer → Prometheus name conversion:**

| Micrometer name | Prometheus name |
|---|---|
| `jvm.memory.used` | `jvm_memory_used_bytes` |
| `http.server.requests` | `http_server_requests_seconds_count` + `_sum` + `_max` |
| `orders.placed` (Counter) | `orders_placed_total` |
| `orders.processing.time` (Timer) | `orders_processing_time_seconds_count` + `_sum` + `_max` |
| `hikaricp.connections.active` | `hikaricp_connections_active` |

#### Common PromQL Queries

**JVM heap usage in MB:**
```promql
jvm_memory_used_bytes{application="order-service", area="heap"} / 1024 / 1024
```

**JVM heap usage as percentage of max:**
```promql
jvm_memory_used_bytes{area="heap"}
  / jvm_memory_max_bytes{area="heap"} * 100
```

**HTTP request rate (requests per second, 5-minute window):**
```promql
rate(http_server_requests_seconds_count{application="order-service"}[5m])
```

**HTTP error rate (5xx responses per second):**
```promql
rate(http_server_requests_seconds_count{
  application="order-service",
  status=~"5.."
}[5m])
```

**Average HTTP response time in milliseconds:**
```promql
rate(http_server_requests_seconds_sum{
  application="order-service",
  uri="/api/orders"
}[5m])
/
rate(http_server_requests_seconds_count{
  application="order-service",
  uri="/api/orders"
}[5m])
* 1000
```

**95th percentile response time (requires histogram, not summary):**
```promql
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{
    application="order-service"
  }[5m])
)
```

> To get histogram buckets instead of summaries, configure Micrometer:
> ```yaml
> management:
>   metrics:
>     distribution:
>       percentiles-histogram:
>         http.server.requests: true
>       slo:
>         http.server.requests: 50ms, 100ms, 200ms, 500ms, 1s
> ```

**HikariCP active connections:**
```promql
hikaricp_connections_active{application="order-service"}
```

**HikariCP connection pool saturation:**
```promql
hikaricp_connections_active / hikaricp_connections_max
```

**Active Tomcat threads:**
```promql
tomcat_threads_busy_threads{application="order-service"}
```

**GC pause total time per minute:**
```promql
rate(jvm_gc_pause_seconds_sum{application="order-service"}[1m])
```

**Custom business metric — orders per minute:**
```promql
rate(orders_placed_total{application="order-service"}[1m]) * 60
```

**Custom business metric — order failure rate:**
```promql
rate(orders_failed_total[5m])
/
rate(orders_placed_total[5m])
* 100
```

---

### 19.8 Grafana Dashboard Setup

#### Provisioning a Dashboard via JSON

Grafana dashboards can be auto-provisioned from files:

```yaml
# grafana/provisioning/dashboards/dashboard.yml
apiVersion: 1
providers:
  - name: spring-boot
    folder: Spring Boot
    type: file
    options:
      path: /etc/grafana/dashboards
```

```
grafana/
  provisioning/
    datasources/
      prometheus.yml
    dashboards/
      dashboard.yml
  dashboards/
    spring-boot-overview.json    ← dashboard JSON goes here
```

#### Recommended Dashboard Panels

| Panel Title | Visualization | PromQL |
|---|---|---|
| **Heap Memory Used** | Gauge / Time series | `jvm_memory_used_bytes{area="heap"}` |
| **GC Pause Rate** | Time series | `rate(jvm_gc_pause_seconds_sum[1m])` |
| **HTTP Request Rate** | Time series | `rate(http_server_requests_seconds_count[1m])` |
| **HTTP Error Rate** | Time series | `rate(http_server_requests_seconds_count{status=~"5.."}[1m])` |
| **Avg Response Time** | Time series | `rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m]) * 1000` |
| **P95 Response Time** | Time series | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))` |
| **Active DB Connections** | Gauge | `hikaricp_connections_active` |
| **Pending DB Connections** | Gauge | `hikaricp_connections_pending` |
| **Active Tomcat Threads** | Gauge | `tomcat_threads_busy_threads` |
| **CPU Usage** | Time series | `process_cpu_usage * 100` |
| **JVM Uptime** | Stat | `process_uptime_seconds` |
| **Orders Per Minute** | Time series | `rate(orders_placed_total[1m]) * 60` |
| **Order Failure %** | Time series | `rate(orders_failed_total[5m]) / rate(orders_placed_total[5m]) * 100` |

> **Ready-made dashboards:** Import Grafana dashboard ID **4701** (JVM Micrometer) or **12900** (Spring Boot Statistics) from grafana.com/dashboards directly in the Grafana UI.

---

### 19.9 Setting Up Alerts in Prometheus

```yaml
# alerts.yml
groups:
  - name: spring-boot-alerts
    rules:

      - alert: HighHeapUsage
        expr: >
          jvm_memory_used_bytes{area="heap"}
          / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High heap usage on {{ $labels.application }}"
          description: "Heap is {{ $value | humanizePercentage }} full"

      - alert: HighHttpErrorRate
        expr: >
          rate(http_server_requests_seconds_count{status=~"5.."}[5m])
          / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High HTTP 5xx error rate on {{ $labels.application }}"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: DatabaseConnectionPoolExhausted
        expr: >
          hikaricp_connections_pending{application="order-service"} > 5
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "DB connection pool near exhaustion"
          description: "{{ $value }} threads waiting for a connection"

      - alert: SlowResponseTime
        expr: >
          rate(http_server_requests_seconds_sum[5m])
          / rate(http_server_requests_seconds_count[5m]) > 1.0
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "Average response time > 1s on {{ $labels.application }}"
```

Reference alerts in `prometheus.yml`:

```yaml
rule_files:
  - "alerts.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

---

### 19.10 Enabling Histogram for Percentiles (P95, P99)

By default Micrometer publishes summaries (pre-calculated percentiles in-process). For accurate server-side percentile calculation in Prometheus, enable histograms:

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true      # enables bucket histograms
        orders.processing.time: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99    # also publish client-side percentiles
      slo:
        http.server.requests: 50ms, 100ms, 200ms, 500ms, 1s   # SLO buckets
      minimum-expected-value:
        http.server.requests: 1ms
      maximum-expected-value:
        http.server.requests: 10s
```

With histograms enabled:

```promql
# P95 latency across all instances (aggregatable across pods)
histogram_quantile(0.95,
  sum by (le, uri) (
    rate(http_server_requests_seconds_bucket{application="order-service"}[5m])
  )
)
```

---

### 19.11 Complete Reference: Metric Name Mapping

```
┌──────────────────────────────────────────────────────────────────────────────┐
│              Micrometer → Prometheus Name Conversion Rules                   │
│                                                                              │
│  1. Dots (.) replaced with underscores (_)                                  │
│  2. Counters get _total suffix                                               │
│  3. Timers produce three series:                                             │
│       {name}_seconds_count  — number of calls                               │
│       {name}_seconds_sum    — total time                                     │
│       {name}_seconds_max    — max time in current interval                  │
│  4. With histograms:                                                         │
│       {name}_seconds_bucket{le="0.1"}  — calls under 100ms                 │
│  5. Gauge: {name} directly (no suffix)                                      │
│                                                                              │
│  Examples:                                                                   │
│  jvm.memory.used          → jvm_memory_used_bytes       (Gauge)             │
│  http.server.requests     → http_server_requests_seconds_count (Counter)    │
│  jvm.gc.pause             → jvm_gc_pause_seconds_count  (Timer)             │
│  orders.placed            → orders_placed_total          (Counter)           │
│  orders.processing.time   → orders_processing_time_seconds_count (Timer)    │
└──────────────────────────────────────────────────────────────────────────────┘
```
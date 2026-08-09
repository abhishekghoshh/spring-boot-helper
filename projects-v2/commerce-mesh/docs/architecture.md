# CommerceMesh Architecture

## Overview

CommerceMesh is an enterprise distributed e-commerce platform built on Spring Boot 4.1, Spring Cloud 2025.1, Java 25, and multiple persistence technologies. The system follows a **microservices architecture** with the **API Gateway** pattern as the single entry point, **Eureka** for service discovery, **Spring Cloud Config** for centralized configuration, and **RabbitMQ** for asynchronous event-driven communication.

Every service is independently deployable, owns its own data, and communicates through well-defined REST APIs or asynchronous events.

---

## High-Level Architecture Diagram

```
                         ┌──────────────────────┐
                         │   NGINX Ingress       │
                         │   (Production only)   │
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │   API Gateway         │
                         │   (Spring Cloud)      │
                         │   Port: 8080          │
                         └──────────┬───────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
            ┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
            │ Config Server│ │  Discovery  │ │  External  │
            │   Port: 8888 │ │  Port: 8761 │ │   Services │
            └──────────────┘ └─────────────┘ └────────────┘
                                    │
     ┌──────────────────────────────┼──────────────────────────────┐
     │              │               │              │               │
┌────▼────┐  ┌──────▼──────┐ ┌─────▼─────┐ ┌──────▼──────┐ ┌─────▼─────┐
│  Auth   │  │    User     │ │  Product  │ │    Cart     │ │ Inventory │
│ 8081    │  │    8082     │ │  Catalog  │ │    8084     │ │   8085    │
└────┬────┘  └──────┬──────┘ └─────┬─────┘ └──────┬──────┘ └─────┬─────┘
     │              │               │              │               │
┌────▼────┐  ┌──────▼──────┐ ┌─────▼─────┐ ┌──────▼──────┐ ┌─────▼─────┐
│  Order  │  │  Payment    │ │Notificat. │ │   Search    │ │    UI     │
│  8086   │  │   8087      │ │   8088    │ │    8089     │ │   3000    │
└────┬────┘  └─────────────┘ └─────┬─────┘ └─────────────┘ └───────────┘
     │                             │
     └─────────┬───────────────────┘
               │
        ┌──────▼──────┐
        │  RabbitMQ   │
        │ 5672/15672  │
        └─────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                        DATA LAYER                                │
│  PostgreSQL 17  │  MongoDB 8  │  Redis 7                         │
│  Port: 5432     │  Port: 27017│  Port: 6379                      │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                     OBSERVABILITY STACK                          │
│  Prometheus:9090  │  Grafana:3001  │  Jaeger:16686  │  Loki:3100│
└──────────────────────────────────────────────────────────────────┘
```

---

## Service Catalog

### Infrastructure Services

| Service | Port | Technology | Purpose |
|---------|------|------------|---------|
| **discovery-server** | 8761 | Netflix Eureka | Service registry — all services register at startup. Enables client-side load balancing via Spring Cloud LoadBalancer. |
| **config-server** | 8888 | Spring Cloud Config | Centralized configuration management. Serves `application.yml` to all services. Supports native, Git, and Vault backends. |
| **api-gateway** | 8080 | Spring Cloud Gateway | Single entry point. Handles JWT validation, rate limiting, path rewriting, CORS, and routing to downstream services. Built on reactive Netty for non-blocking I/O. |

### Business Microservices

| Service | Port | Database | Message Broker | Primary Responsibility |
|---------|------|----------|----------------|----------------------|
| **authentication-service** | 8081 | PostgreSQL | — | Registration, login, logout, JWT issuance, refresh tokens, password reset, email verification |
| **user-service** | 8082 | PostgreSQL | — | User profiles, addresses, role assignments, preferences |
| **product-catalog-service** | 8083 | MongoDB | — | Products, categories, brands, reviews, ratings, product search |
| **cart-service** | 8084 | MongoDB + Redis | — | Shopping cart (MongoDB persisted, Redis cached), wishlist, coupon management |
| **inventory-service** | 8085 | PostgreSQL | — | Warehouse management, stock tracking, stock reservation for orders |
| **order-service** | 8086 | PostgreSQL | RabbitMQ (publisher) | Order lifecycle (created → confirmed → shipped → delivered), checkout orchestration |
| **payment-service** | 8087 | PostgreSQL | — | Payment processing, refunds, invoice generation, retry/circuit breaker patterns |
| **notification-service** | 8088 | PostgreSQL | RabbitMQ (consumer) | Email, SMS, push notification dispatch. Consumes events from order-service |
| **search-service** | 8089 | MongoDB | — | Full-text product search, auto-complete, search suggestions, faceted search |

### No Shared Libraries

This project follows a **no shared libraries** architecture. Each service is a fully independent Spring Boot project with its own `pom.xml`, DTOs, exception classes, security configuration, and event/message contracts. Services communicate only over the network (REST/Feign/RabbitMQ), never via shared JARs.

Common patterns that would typically be extracted into shared libraries are instead implemented locally in each service:
- **API responses:** Each service defines its own `ApiResponse<T>` wrapper and pagination DTOs
- **Security:** Each service has its own `SecurityFilterChain`, `JwtTokenProvider`, and security configuration  
- **Messaging:** Each service defines its own RabbitMQ event DTOs, exchanges, queues, and bindings
- **Error handling:** Each service has its own `GlobalExceptionHandler` and exception class hierarchy
- **Domain types:** Each service owns its entity/value object types, enums, and mappers

This design maximizes service independence — each service can evolve its internal contracts without impacting others.

---

## Architecture Decisions (ADR)

### ADR-001: Microservices with Spring Cloud
**Context:** The platform must handle independent scaling of catalog reads vs. order writes, and allow different teams to work on different domains.  
**Decision:** Each business domain (auth, products, orders, etc.) is a separate Spring Boot microservice connected via Spring Cloud.  
**Consequences:** Increased operational complexity (monitoring, tracing, deployment). Mitigated by the comprehensive observability stack built into every service.

### ADR-002: Polyglot Persistence
**Context:** Orders and users require ACID transactions and relational integrity; products and carts are document-oriented with flexible schemas; session and cache data needs sub-millisecond access.  
**Decision:** PostgreSQL for transactional data (auth, users, orders, payments, inventory, notifications), MongoDB for document data (products, categories, carts, search), Redis for caching and transient state.  
**Consequences:** Operations must manage three database technologies. Each service only connects to the databases it needs.

### ADR-003: Event-Driven Order Fulfillment
**Context:** Order placement triggers notifications (email confirmation) without blocking the checkout response.  
**Decision:** order-service publishes `OrderPlaced`, `OrderShipped`, `OrderDelivered` events to RabbitMQ. notification-service consumes these events asynchronously.  
**Consequences:** Eventual consistency between order status and notification dispatch. DLQ and retry policies handle transient failures.

### ADR-004: Package-by-Feature
**Context:** Traditional layer-based packaging (`controller/`, `service/`, `repository/`) scatters related code across packages.  
**Decision:** Each feature domain gets its own package containing all layers:
```
auth/
├── controller/    → AuthController.java
├── service/       → AuthService.java, TokenService.java
├── repository/    → UserRepository.java, RefreshTokenRepository.java
├── model/         → User.java, Role.java, Permission.java
├── dto/           → LoginRequest.java, RegisterRequest.java, TokenResponse.java
├── config/        → SecurityConfig.java, JwtConfig.java
├── exception/     → AuthenticationException.java
└── mapper/        → UserMapper.java
```
**Consequences:** Better cohesion, easier refactoring, clear domain boundaries.

### ADR-005: Stateless JWT Authentication
**Context:** The platform must scale horizontally without sticky sessions.  
**Decision:** Opaque server-side sessions replaced with stateless JWT access tokens (15min expiry) + refresh tokens (7 days). API Gateway validates JWTs; services trust the gateway and use `@PreAuthorize` for fine-grained authorization.  
**Consequences:** Token revocation requires a Redis blacklist. Refresh token rotation mitigates theft risk.

### ADR-006: Resilience4j for Fault Tolerance
**Context:** Payment service calls an external payment gateway that may be slow or unavailable. Order service depends on inventory service to reserve stock.  
**Decision:** Implement Resilience4j Circuit Breaker, Retry, Bulkhead, and Timeout on all Feign client calls. payment-service has a 3-retry policy with exponential backoff; order-service uses a circuit breaker for inventory calls.  
**Consequences:** Graceful degradation — if inventory is down, orders are accepted but stock is reserved asynchronously when inventory recovers.

### ADR-007: API Versioning via URL Path
**Context:** APIs will evolve and breaking changes need coexisting versions.  
**Decision:** Version in the URL path: `/api/v1/`, `/api/v2/`. API Gateway strips the version prefix and routes to the correct service instance.  
**Consequences:** Cleaner than header-based versioning. Requires careful route management in the Gateway.

### ADR-008: Containerized Development Environment
**Context:** Developers need identical local environments regardless of OS.  
**Decision:** Every service has a multi-stage Dockerfile. docker-compose.yaml at the root starts all infrastructure (DBs, message broker, monitoring) as containers while allowing services to run on the host for hot-reload during development.  
**Consequences:** Developers need Docker installed. The production build pipeline uses the same Dockerfiles.

---

## Service Communication Patterns

### Synchronous (Request-Response)

Used when the caller needs an immediate response:

```
API Gateway ──▶ authentication-service  (JWT validation, login)
API Gateway ──▶ product-catalog-service (product listing)
order-service ──Feign──▶ inventory-service (stock check/reservation)
order-service ──Feign──▶ payment-service  (payment processing)
cart-service   ──Feign──▶ product-catalog-service (price validation)
```

**Technology:** OpenFeign declarative clients with `@FeignClient`  
**Fault Tolerance:** Resilience4j Circuit Breaker, Retry (3 attempts, 1s wait), Timeout (5s per call), Bulkhead (max 10 concurrent calls)

### Asynchronous (Event-Driven)

Used when the caller does not need an immediate response and services should be decoupled:

```
order-service ──publish──▶ exchange: order.events
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
             queue: order.   queue: order.  queue: order.
             notifications  inventory     analytics
                    │
                    ▼
          notification-service (email/SMS)
```

**Exchanges and Queues:**
| Exchange | Queue | Binding Key | Consumer |
|----------|-------|-------------|----------|
| `order.events` (topic) | `order.notifications` | `order.*` | notification-service |
| `order.events` (topic) | `order.inventory` | `order.placed` | inventory-service |
| `order.events` (topic) | `order.analytics` | `order.*` | (future: analytics-service) |

**Reliability:** Publisher confirms enabled. Dead Letter Queue (`order.dlq`) catches failed messages after 3 retries. Idempotent consumers prevent duplicate processing.

### Service Discovery Flow

```
1. Service starts → registers with Eureka (hostname:port + health URL)
2. Eureka heartbeats every 30 seconds
3. Feign client looks up service by name via Eureka + Spring Cloud LoadBalancer
4. Load-balanced request to any healthy instance
5. Service deregisters on graceful shutdown
```

---

## Authentication & Authorization Flow

### Token Issuance

```
┌──────────┐          ┌──────────────┐          ┌──────────────────────┐
│ React UI │          │ API Gateway  │          │ Authentication Svc   │
└────┬─────┘          └──────┬───────┘          └──────────┬───────────┘
     │                       │                             │
     │ POST /api/v1/auth/login                             │
     │ {username, password}  │                             │
     │──────────────────────▶│                             │
     │                       │ Route to auth-service       │
     │                       │─────────────────────────────▶
     │                       │                             │ Validate credentials
     │                       │                             │ Generate JWT (15min)
     │                       │                             │ Generate Refresh Token
     │                       │  {accessToken, refreshToken}│
     │                       │◀─────────────────────────────
     │  200 {accessToken,    │
     │       refreshToken}   │
     │◀──────────────────────│
     │                       │
     │ Store tokens in       │
     │ localStorage          │
```

### Authenticated Request

```
┌──────────┐          ┌──────────────┐          ┌──────────────────────┐
│ React UI │          │ API Gateway  │          │  Any Microservice    │
└────┬─────┘          └──────┬───────┘          └──────────┬───────────┘
     │                       │                             │
     │ GET /api/v1/products  │                             │
     │ Authorization: Bearer │                             │
     │   <accessToken>       │                             │
     │──────────────────────▶│                             │
     │                       │ Validate JWT signature      │
     │                       │ Check blacklist (Redis)     │
     │                       │ Extract roles/permissions   │
     │                       │ Forward with X-User-Id,     │
     │                       │ X-User-Roles headers        │
     │                       │─────────────────────────────▶
     │                       │                             │ @PreAuthorize check
     │                       │                             │ Process request
     │                       │◀─────────────────────────────
     │◀──────────────────────│
```

**Roles:** `SUPER_ADMIN`, `ADMIN`, `PRODUCT_MANAGER`, `ORDER_MANAGER`, `CUSTOMER_SUPPORT`, `CUSTOMER`  
**Default Admin:** username `admin`, password `Admin@123`

---

## Data Architecture

### Database Per Service

Each service owns its database schema/collection — no shared tables across services.

```
authentication-service ──▶ PostgreSQL (schema: auth)
user-service           ──▶ PostgreSQL (schema: users)
inventory-service      ──▶ PostgreSQL (schema: inventory)
order-service          ──▶ PostgreSQL (schema: orders)
payment-service        ──▶ PostgreSQL (schema: payments)
notification-service   ──▶ PostgreSQL (schema: notifications)
product-catalog-service──▶ MongoDB (database: commercemesh)
cart-service           ──▶ MongoDB (database: commercemesh) + Redis
search-service         ──▶ MongoDB (database: commercemesh)
```

### Caching Strategy

| Cache Key Pattern | TTL | Eviction Policy | Populated By |
|-------------------|-----|-----------------|-------------|
| `product:{id}` | 1 hour | TTL + explicit on update | product-catalog-service |
| `cart:{userId}` | 2 hours | TTL + explicit on checkout | cart-service |
| `user:{id}` | 30 min | TTL | authentication-service |
| `dashboard:stats` | 5 min | TTL | aggregation job |
| `trending:products` | 15 min | TTL | analytics pipeline |

**Cache Invalidation:** Write-through for product updates. Cart cache is the source of truth with MongoDB as persistent backup.

---

## Observability Architecture

Every service exposes:
- **Metrics:** `/actuator/prometheus` (Micrometer → Prometheus)
- **Health:** `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`
- **Tracing:** OpenTelemetry auto-instrumentation (propagated trace context across Feign, RabbitMQ, DB calls)
- **Logs:** Structured JSON logging to stdout → Promtail → Loki

### Metrics Pipeline

```
Service ──▶ Micrometer ──▶ /actuator/prometheus ──▶ Prometheus ──▶ Grafana
                                                       │
                                                  AlertManager
                                                  (Slack/PagerDuty)
```

### Tracing Pipeline

```
Service ──▶ OpenTelemetry Agent ──▶ Jaeger Collector (OTLP gRPC :4317)
                                          │
                                     Jaeger UI (:16686)
```

### Logging Pipeline

```
Service ──▶ stdout (JSON) ──▶ Promtail (Docker logs) ──▶ Loki (:3100) ──▶ Grafana
```

---

## Deployment Architecture

### Development (Docker Compose)

- Infrastructure containers: PostgreSQL, MongoDB, Redis, RabbitMQ
- Spring Cloud: Config Server, Eureka, API Gateway as containers or host processes
- Microservices: Can run as host processes (`./mvnw spring-boot:run`) for hot-reload, or as containers
- All containers on a single Docker network (`cm-network`)

### Production (Kubernetes)

```
┌─────────────────────────────────────────────────────────────┐
│  NGINX Ingress Controller                                   │
│  commercemesh.local → api-gateway:8080                      │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│  Namespace: commercemesh                                    │
│                                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ Auth Svc │ │ User Svc │ │ Catalog  │ │ Cart Svc │       │
│  │ 2 pods   │ │ 2 pods   │ │ 3 pods   │ │ 2 pods   │       │
│  │ HPA 2-5  │ │ HPA 2-5  │ │ HPA 2-10 │ │ HPA 2-5  │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
│                                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │Inventory │ │ Order    │ │ Payment  │ │Notif.Svc │       │
│  │ 2 pods   │ │ 3 pods   │ │ 2 pods   │ │ 2 pods   │       │
│  │ HPA 2-10 │ │ HPA 2-10 │ │ HPA 2-5  │ │ HPA 2-5  │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
│                                                             │
│  ┌──────────┐                                               │
│  │Search Svc│  ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ 2 pods   │  │ Gateway  │ │ Discovery│ │ Config   │      │
│  │ HPA 2-5  │  │ 3 pods   │ │ 2 pods   │ │ 2 pods   │      │
│  └──────────┘  └──────────┘ └──────────┘ └──────────┘      │
└─────────────────────────────────────────────────────────────┘
```

Each pod includes:
- **Resource:** requests (256Mi mem, 100m CPU), limits (512Mi mem, 500m CPU)
- **Probes:** liveness, readiness, startup (all via `/actuator/health/*`)
- **Security:** non-root user (1001), read-only root filesystem, dropped capabilities
- **Affinity:** pod anti-affinity (preferred, spreads across nodes)
- **Auto-scaling:** HPA based on CPU utilization (target 80%)

---

## Technology Stack Summary

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.5 |
| Cloud | Spring Cloud | 2024.0.1 |
| Service Discovery | Netflix Eureka | via Spring Cloud |
| API Gateway | Spring Cloud Gateway | via Spring Cloud |
| Config | Spring Cloud Config | via Spring Cloud |
| RPC | OpenFeign | via Spring Cloud |
| Resilience | Resilience4j | 2.3.0 |
| Security | Spring Security + JJWT | 6.x / 0.12.6 |
| Relational DB | PostgreSQL | 17 |
| Document DB | MongoDB | 8 |
| Cache | Redis | 7 |
| Messaging | RabbitMQ | 4 |
| Metrics | Micrometer + Prometheus | latest |
| Tracing | OpenTelemetry + Jaeger | 1.47.0 |
| Logging | Loki + Promtail | latest |
| Dashboards | Grafana | latest |
| API Docs | SpringDoc OpenAPI | 2.8.6 |
| Object Mapping | MapStruct | 1.6.3 |
| Frontend | React + TypeScript + Vite | 18 / 5 |
| UI Framework | Material UI | 6 |
| Containers | Docker + Compose | latest |
| Orchestration | Kubernetes + Helm | 1.30+ / 3+ |
| Testing | JUnit 5 + Testcontainers + ArchUnit | latest |

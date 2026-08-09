# Code Architecture & Exploration Guide

## Overview

This guide explains the **internal code architecture** of CommerceMesh — how the code is organized, how the layers connect, and the recommended path for exploring and understanding the codebase from scratch.

---

## Project at a Glance

```
commerce-mesh/
│
├── docker-compose.yaml              ← 25-container local dev environment
├── README.md                        ← Project overview
├── AGENT.md                         ← AI agent instructions & project spec

├── backend/                         ← 12 Spring Boot microservices
│   ├── discovery-server/            ← Eureka Service Registry
│   ├── config-server/               ← Centralized Config Server
│   ├── api-gateway/                 ← API Gateway (single entry point)
│   ├── authentication-service/      ← Login, JWT, RBAC
│   ├── user-service/                ← Profiles, addresses
│   ├── product-catalog-service/     ← Products, categories, reviews
│   ├── cart-service/                ← Shopping cart, wishlist
│   ├── inventory-service/           ← Stock, warehouses
│   ├── order-service/               ← Orders, checkout, events
│   ├── payment-service/             ← Payments, refunds
│   ├── notification-service/        ← Email/SMS, event consumer
│   └── search-service/              ← Full-text product search
│
├── ui/                               ← 2 React frontends
│   ├── admin-console/                ← Admin portal (React 19 + TypeScript + MUI)
│   └── customer-console/             ← Customer storefront (React 19 + TypeScript + MUI)
│
├── monitoring/                      ← Observability configs
│   ├── prometheus/prometheus.yml
│   ├── grafana/datasources.yml
│   ├── loki/loki-config.yaml
│   └── promtail/promtail-config.yaml
│
├── charts/                          ← Kubernetes Helm umbrella chart
│   └── commerce-mesh/
│
├── scripts/                         ← Convenience scripts
│   ├── init-db.sql
│   ├── init-mongo.js
│   ├── start.sh
│   └── stop.sh
│
└── docs/                            ← All documentation
```

---

## Layers of the Architecture

CommerceMesh is built in **4 architectural layers**. Understanding these layers is the key to navigating any part of the code.

### Layer 1: Root (Docker Compose)

**Files:** `docker-compose.yaml`, `.env`, `.gitignore`

There is no root POM — each service under `backend/` has its own standalone `pom.xml` with Spring Boot 4.1 as parent, Java 25, and Spring Cloud 2025.1.2. Services are fully independent with no shared parent or shared libraries.

Key versions across all services:
- Java 25 (LTS)
- Spring Boot 4.1.0
- Spring Cloud 2025.1.2
- Virtual threads enabled on every service
- Services communicate over the network only (REST/Feign/RabbitMQ)

Each backend `pom.xml` extends Spring Boot 4.1 as a standalone parent:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
    <relativePath/>
</parent>
```

The `docker-compose.yaml` defines the entire local runtime — 26 containers including all databases, all services, both frontends (admin + customer), and the full observability stack.

**Entry point for understanding:** Read `docker-compose.yaml` to see how everything connects at runtime.

---

### Layer 2: Infrastructure Services (`backend/` — discovery, config, gateway)

Three services form the **Spring Cloud backbone**. They are lightweight — mostly configuration, little business logic.

#### discovery-server
- **Package:** `com.commercemesh.discovery`
- **Key file:** `DiscoveryServerApplication.java` — `@EnableEurekaServer`
- **Config:** `application.yml` — disables self-registration
- **Role:** All other services register here on startup. Enables service discovery by name instead of hardcoded URLs.

#### config-server
- **Package:** `com.commercemesh.config`
- **Key file:** `ConfigServerApplication.java` — `@EnableConfigServer`
- **Config:** `application.yml` — uses `native` profile (reads from classpath)
- **Role:** Serves `application.yml` to all services. In production, backs onto a Git repository.

#### api-gateway
- **Package:** `com.commercemesh.gateway`
- **Key file:** `ApiGatewayApplication.java` — `@EnableDiscoveryClient`
- **Config:** `application.yml` — defines route predicates for every microservice
- **Role:** Single entry point. Validates JWT, handles CORS, routes to downstream services, enforces rate limits.

**Entry point for understanding:** Read in this order: discovery-server → config-server → api-gateway. Each builds on the previous.

---

### Layer 4: Business Microservices (`backend/` — auth, user, catalog, etc.)

Each business service follows the **exact same internal pattern**:

```
backend/<service-name>/
├── pom.xml                                ← Maven build (extends root parent)
├── Dockerfile                             ← Multi-stage Docker build
├── helm/                                  ← Kubernetes Helm chart
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/                         ← 9 K8s manifest templates
└── src/
    ├── main/
    │   ├── java/com/commercemesh/<pkg>/
    │   │   ├── <Service>Application.java  ← @SpringBootApplication entry point
    │   │   ├── config/                    ← SecurityConfig, CORSConfig, SwaggerConfig, CacheConfig
    │   │   ├── controller/                ← @RestController classes
    │   │   ├── service/                   ← @Service business logic
    │   │   ├── repository/                ← @Repository (JPA or Mongo)
    │   │   ├── model/                     ← @Entity JPA entities / @Document Mongo documents
    │   │   ├── dto/                       ← Request/Response DTOs (Java records)
    │   │   ├── mapper/                    ← MapStruct interfaces (entity ↔ DTO)
    │   │   └── exception/                ← Service-specific exceptions
    │   └── resources/
    │       ├── application.yml            ← Service configuration
    │       └── db/migration/              ← Flyway migrations (PostgreSQL services)
    └── test/
        └── java/com/commercemesh/<pkg>/
            ├── controller/                ← @WebMvcTest
            ├── service/                   ← Unit tests (Mockito)
            ├── repository/                ← @DataJpaTest / @DataMongoTest
            └── integration/               ← Testcontainers integration tests
```

**Request flow within a service:**

```
HTTP Request → Controller → Service → Repository → Database
                    │            │
                    ▼            ▼
                  Mapper       Mapper
                  (DTO←→Entity) (Entity←→DTO)
                    │
                    ▼
                HTTP Response
```

Each service has 8 package directories created:
- `controller/` — handles HTTP, delegates to services
- `service/` — business logic, transactions, calls repositories
- `repository/` — Spring Data interfaces (`JpaRepository`, `MongoRepository`)
- `model/` — JPA entities (`@Entity`) or MongoDB documents (`@Document`)
- `dto/` — request/response objects, validation annotations
- `mapper/` — MapStruct interfaces for entity-DTO conversion
- `config/` — Spring configuration classes
- `exception/` — custom exceptions

**Entry point for understanding:** Start with **authentication-service** — it's the most self-contained and introduces JWT, PostgreSQL, Redis, and Spring Security. Then move to **product-catalog-service** (MongoDB, no messaging) and finally **order-service** (PostgreSQL + RabbitMQ + Feign).

---

## Data Storage Mapping

| Service | Database | Java Annotation | Repository Interface |
|---------|----------|----------------|---------------------|
| authentication-service | PostgreSQL | `@Entity` + `@Table` | `JpaRepository` |
| user-service | PostgreSQL | `@Entity` + `@Table` | `JpaRepository` |
| inventory-service | PostgreSQL | `@Entity` + `@Table` | `JpaRepository` |
| order-service | PostgreSQL | `@Entity` + `@Table` | `JpaRepository` |
| payment-service | PostgreSQL | `@Entity` + `@Table` | `JpaRepository` |
| notification-service | PostgreSQL | `@Entity` + `@Table` | `JpaRepository` |
| product-catalog-service | MongoDB | `@Document` | `MongoRepository` |
| cart-service | MongoDB + Redis | `@Document` + `@RedisHash` | `MongoRepository` + `RedisTemplate` |
| search-service | MongoDB | `@Document` | `MongoRepository` |

---

## Service Communication Map

```
                    ┌─────────────────────────────────────────────┐
                    │              api-gateway (8080)              │
                    │              JWT validation                 │
                    │              Rate limiting                  │
                    └───┬─────┬─────┬─────┬─────┬─────┬─────┬─────┘
                        │     │     │     │     │     │     │
        ┌───────────────┤     │     │     │     │     │     └───────────────┐
        │               │     │     │     │     │     │                     │
   ┌────▼────┐   ┌──────▼──┐ ┌▼─────┐ ┌▼─────┐ ┌▼─────┐ ┌▼─────┐     ┌─────▼────┐
   │  auth   │   │  user   │ │catalog│ │ cart │ │invntr│ │ paymt│     │  search  │
   │  8081   │   │  8082   │ │ 8083  │ │ 8084 │ │ 8085 │ │ 8087 │     │  8089    │
   └─────────┘   └─────────┘ └───────┘ └───────┘ └──┬───┘ └───────┘     └──────────┘
                                                     │
                                                     │ Feign (sync)
                                                     ▼
                                              ┌──────────────┐
                                              │ order-service│
                                              │    8086      │
                                              └──────┬───────┘
                                                     │
                                                     │ RabbitMQ (async)
                                                     ▼
                                              ┌──────────────┐
                                              │notification  │
                                              │    8088      │
                                              └──────────────┘
```

- **Solid lines (→):** Synchronous via API Gateway routing
- **Feign lines:** Synchronous service-to-service calls (order → inventory, order → payment)
- **RabbitMQ lines:** Asynchronous events (order → notification)
- **Shared libraries:** Used by all services for consistency

---

## Recommended Code Exploration Path

If you're new to this codebase, explore in this order. Each step builds on the previous ones.

### Step 1: Root Structure (15 minutes)

Read these files in order:

1. **`README.md`** — what the project is, service list, tech stack
2. **`AGENT.md`** — the full project specification, learning objectives, architecture requirements
3. **`pom.xml`** — all 17 Maven modules, dependency versions, build plugins
4. **`docker-compose.yaml`** — scan the 25 services, note the ports and dependencies
5. **`.env`** — all environment variables and default credentials

**Goal:** Understand the scope — 12 services, 5 libraries, React frontend, observability stack, and how they connect at runtime.

### Step 2: Infrastructure Foundation (20 minutes)

Read the three infrastructure services:

1. **`backend/discovery-server/src/main/java/com/commercemesh/discovery/DiscoveryServerApplication.java`**
   - One annotation (`@EnableEurekaServer`) makes this the service registry
   - Read `application.yml` — note `register-with-eureka: false`

2. **`backend/config-server/src/main/java/com/commercemesh/config/ConfigServerApplication.java`**
   - `@EnableConfigServer` — centralized config
   - Read `application.yml` — `native` profile, serves from classpath

3. **`backend/api-gateway/src/main/java/com/commercemesh/gateway/ApiGatewayApplication.java`**
   - `@EnableDiscoveryClient` — registers with Eureka
   - Read `application.yml` — route table for all 9 microservices

**Goal:** Understand how services find each other (Eureka) and how external traffic enters the system (Gateway).

### Step 3: First Service Deep Dive — Authentication (30 minutes)

The authentication service is the best starting point because it's self-contained and introduces 4 key Spring technologies.

Read these files:

1. **`backend/authentication-service/pom.xml`**
   - Note dependencies: `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-data-redis`
   - This pattern repeats across all services — each service declares only the deps it needs

2. **`backend/authentication-service/src/main/resources/application.yml`**
   - PostgreSQL datasource config (`ddl-auto: update` for dev)
   - Redis config
   - Eureka client config
   - JWT secret and expiry
   - SpringDoc OpenAPI config
   - Actuator + Prometheus + tracing config

3. **`backend/authentication-service/src/main/java/com/commercemesh/auth/AuthenticationServiceApplication.java`**
   - `@SpringBootApplication`, `@EnableDiscoveryClient`, `@EnableFeignClients`
   - This is the standard entry point pattern for ALL services

4. **`backend/authentication-service/Dockerfile`**
   - Multi-stage: `eclipse-temurin:21-jdk-alpine` (build) → `eclipse-temurin:21-jre-alpine` (runtime)
   - Non-root user (1001), healthcheck
   - This Dockerfile pattern is identical across all 12 services

5. **`backend/authentication-service/helm/`**
   - 9 template files: deployment, service, configmap, secret, ingress, hpa, serviceaccount, networkpolicy, NOTES.txt
   - Standard K8s deployment — liveness/readiness/startup probes, pod anti-affinity
   - This Helm pattern is identical across all 12 services

**Goal:** Understand the anatomy of a service — pom.xml dependencies, application config, entry point, Docker build, Helm chart.

### Step 4: Cross-Cutting Concerns (20 minutes)

Each service owns its own DTOs, exception handlers, security configuration, and event types. There are no shared libraries — services communicate only over the network (REST/Feign/RabbitMQ).

- **API responses:** Each service defines its own `ApiResponse<T>` wrapper and pagination DTOs
- **Security:** Each service has its own `SecurityFilterChain`, `JwtTokenProvider`, and security configuration
- **Events:** Each service defines its own RabbitMQ event DTOs, exchanges, and queues
- **Exceptions:** Each service has its own `GlobalExceptionHandler` and exception class hierarchy

**Goal:** Understand that this project follows a no-shared-libraries architecture. Each service is fully independent with internal DTOs, mappers, and configuration.

### Step 5: Database Mapping (30 minutes)

Pick one PostgreSQL service and one MongoDB service:

**PostgreSQL example — order-service:**
- Read `docs/database-schema.md` → Schema: `orders` section (find the DDL)
- This is the `model/` package — JPA entities mapping to these tables
- `repository/` extends `JpaRepository<Order, UUID>` — Spring Data auto-implements CRUD

**MongoDB example — product-catalog-service:**
- Read `docs/database-schema.md` → Collection: `products` section (find the JSON schema)
- This is the `model/` package — POJOs with `@Document` annotation
- `repository/` extends `MongoRepository<Product, String>` — same pattern, different backend

**Goal:** Understand the two persistence patterns. PostgreSQL = `@Entity` + `JpaRepository`. MongoDB = `@Document` + `MongoRepository`.

### Step 6: Service Communication (30 minutes)

1. **Synchronous (Feign):**
   - Read `docs/architecture.md` → Service Communication Patterns → Synchronous
   - order-service `service/OrderService.java` calls `InventoryClient.reserveStock()` (a Feign interface)
   - inventory-service `controller/InventoryController.java` exposes the endpoint
   - `docs/api-guidelines.md` lists the `/inventory/reserve` endpoint

2. **Asynchronous (RabbitMQ):**
   - Read `docs/messaging.md` — entire document covers this
   - order-service publishes `OrderPlacedEvent` to `order.events` exchange
   - notification-service consumes from `order.notifications` queue
   - Event types are defined within each service's own DTO package

**Goal:** Understand the two communication patterns. Feign = synchronous REST calls. RabbitMQ = async events.

### Step 7: Security Architecture (30 minutes)

1. Read `docs/security.md` — covers the complete security model
2. API Gateway validates JWT on every request (read `api-gateway` `config/` package — planned)
3. Downstream services trust forwarded `X-User-Id`, `X-User-Roles` headers
4. `@PreAuthorize("hasRole('ADMIN')")` on service controllers for authorization

**Goal:** Understand JWT → Gateway validation → downstream trust model.

### Step 8: Frontend — Admin Portal (15 minutes)

1. **`ui/admin-console/src/main.tsx`** — entry point, renders `<App />`
2. **`ui/admin-console/src/App.tsx`** — MUI theme + TanStack Query + React Router setup
3. **`ui/admin-console/src/context/AuthContext.tsx`** — login flow, token storage
4. **`ui/admin-console/src/api/apiClient.ts`** — Axios instance with JWT interceptor
5. **`ui/admin-console/src/layouts/MainLayout.tsx`** — sidebar navigation, `Outlet` for pages
6. **`ui/admin-console/src/pages/LoginPage.tsx`** — example of auth flow
7. **`ui/admin-console/src/pages/DashboardPage.tsx`** — example of a data page

**Goal:** Understand the admin React component tree, routing, auth flow, and how the frontend connects to the backend via `apiClient.ts`.

### Step 9: Frontend — Customer Storefront (15 minutes)

1. **`ui/customer-console/src/App.tsx`** — e-commerce theme, routes for Home, Products, Cart, Checkout, Orders, Profile, Wishlist
2. **`ui/customer-console/src/context/AuthContext.tsx`** — customer login/register flow
3. **`ui/customer-console/src/context/CartContext.tsx`** — cart state (add, remove, update quantity, totals)
4. **`ui/customer-console/src/layouts/CustomerLayout.tsx`** — header with search bar, cart badge, user dropdown, category menu
5. **`ui/customer-console/src/pages/ProductDetailPage.tsx`** — product images, description, add to cart
6. **`ui/customer-console/src/pages/CheckoutPage.tsx`** — multi-step checkout (shipping → payment → review)
7. **`ui/customer-console/src/pages/CartPage.tsx`** — cart with quantity controls, subtotals, checkout CTA

**Goal:** Understand the customer-facing shopping flow — browse → detail → cart → checkout → order history.

### Step 10: Observability (20 minutes)

1. Read `docs/observability.md` — the full observability architecture
2. **`monitoring/prometheus/prometheus.yml`** — scrape targets for all 12 services
3. **`monitoring/grafana/datasources.yml`** — Prometheus + Loki as data sources
4. **`monitoring/loki/loki-config.yaml`** — log aggregation config
5. In any service `application.yml`: `management.endpoints.web.exposure.include` — Actuator config

**Goal:** Understand how metrics, traces, and logs flow from services → Prometheus/Jaeger/Loki → Grafana.

### Step 11: Kubernetes & Deployment (20 minutes)

1. **`backend/<any-service>/helm/`** — per-service Helm chart
   - `templates/deployment.yaml` — pod spec with resource limits, probes, security context
   - `templates/service.yaml` — ClusterIP service
   - `templates/hpa.yaml` — autoscaling config
   - `templates/networkpolicy.yaml` — restricts ingress to API Gateway
2. **`charts/commerce-mesh/`** — umbrella chart for the entire platform
3. Read `docs/deployment.md` — full deployment guide

**Goal:** Understand K8s deployment pattern — each service gets the same 9-template Helm chart.

---

## Quick Reference: Where to Find Things

| Question | Look Here |
|----------|-----------|
| How does a service start? | `backend/<service>/src/main/java/.../<Service>Application.java` |
| What dependencies does a service use? | `backend/<service>/pom.xml` |
| How is a service configured? | `backend/<service>/src/main/resources/application.yml` |
| What are the database tables? | `docs/database-schema.md` → find the service's schema section |
| What REST endpoints exist? | `docs/api-guidelines.md` → Endpoint Catalog section |
| How do services talk to each other? | `docs/architecture.md` → Service Communication Patterns |
| How does auth work? | `docs/security.md` |
| How does messaging work? | `docs/messaging.md` |
| How is monitoring set up? | `docs/observability.md` + `monitoring/` directory |
| How to deploy? | `docs/deployment.md` + `helm/` directories |
| How to run locally? | `docs/development.md` |
| Shared code? | Each service owns its own — no shared libraries |
| Admin frontend? | `ui/admin-console/src/` → follow Step 8 above |
| Customer frontend? | `ui/customer-console/src/` → follow Step 9 above |

---

## Technology Decision Tree

When reading service code, this decision tree helps identify what tech is in play:

```
Is the service...
│
├── Using PostgreSQL? → Look for @Entity, JpaRepository, Flyway migrations
├── Using MongoDB?   → Look for @Document, MongoRepository, JSON document model
├── Using Redis?     → Look for @Cacheable, RedisTemplate, cache config in application.yml
├── Using RabbitMQ?  → Look for RabbitTemplate (publisher), @RabbitListener (consumer)
├── Calling another service? → Look for @FeignClient interfaces
├── Handling auth?   → Look for SecurityFilterChain, @PreAuthorize
├── Exposing APIs?   → Look for @RestController, OpenAPI annotations
│
└── All services share: Actuator, Micrometer, Prometheus, OpenTelemetry tracing
```

---

## File Count Summary

| Layer | Files | Key Types |
|-------|-------|-----------|
| Root | 4 | pom.xml, docker-compose.yaml, .env |
| Shared Libraries | 5 pom.xml + 5 package-info.java | Maven modules |
| Infrastructure Services | 3 × (pom.xml + Application.java + app.yml + Dockerfile + 10 Helm files) | Spring Cloud |
| Business Microservices | 9 × (pom.xml + Application.java + app.yml + Dockerfile + 10 Helm files) | Spring Boot |
| Frontend | 20 source files | React + TypeScript |
| Monitoring | 4 config files | Prometheus, Grafana, Loki, Promtail |
| Docs | 12 markdown files | Architecture, API, DB, Security, Messaging, Dev, Deploy, Obs, Code Arch |

**Total:** ~250 files across the entire project scaffold.

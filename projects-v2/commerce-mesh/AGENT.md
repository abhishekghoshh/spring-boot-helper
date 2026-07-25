# AGENT.md

## CommerceMesh

### Enterprise Distributed E-Commerce Platform

#### Spring Boot • Spring Cloud • Spring Security • PostgreSQL • MongoDB • Redis • React • Kubernetes • Observability

---

## Objective

Build a **production-grade distributed E-Commerce platform** that demonstrates the complete Spring ecosystem and cloud-native development.

The repository should be designed as an **enterprise learning project**, where each microservice teaches different areas of Spring Boot, Spring Cloud, distributed systems, security, persistence, messaging, observability and frontend development.

Every project should be independently deployable and production-ready.

The AI Agent should generate code that follows:

- Spring Boot Best Practices
- Clean Architecture
- SOLID Principles
- Twelve-Factor App methodology
- Cloud Native principles
- Kubernetes Best Practices
- Secure Coding Standards
- Enterprise-grade project organisation

The goal is to understand how large organisations build scalable Spring applications.

---

## Primary Learning Objectives

The repository should intentionally cover nearly every major Spring ecosystem project.

---

### Spring Boot

- Spring Boot Fundamentals
- Spring MVC
- Spring Validation
- Spring Scheduling
- Spring Cache
- Spring Async
- Spring Events
- Spring Profiles
- Spring Configuration
- Spring Mail
- Spring Retry
- OpenAPI / Swagger
- Actuator

---

### Spring Security

- Security Filter Chain
- JWT Authentication
- Refresh Tokens
- Role Based Access Control (RBAC)
- Permission Based Security
- Method Security
- BCrypt Password Encryption
- Authentication Manager
- UserDetailsService
- Custom Authentication Provider
- Session Management
- CSRF
- CORS
- Password Reset
- Email Verification
- Remember Me (optional)

---

### Spring Cloud

#### Spring Cloud Gateway

- API Gateway
- Single Digital Entry Point
- JWT Validation
- Route Predicates
- Filters
- Header Manipulation
- Path Rewriting
- Global Filters
- Rate Limiting

---

#### Eureka

- Service Discovery
- Service Registration
- Health Registration

---

#### Spring Cloud Config

- Centralised Configuration
- Environment Profiles
- Shared Configuration
- Config Refresh

---

#### OpenFeign

- Declarative REST Clients
- Request Interceptors
- Error Decoder
- Retry
- Timeouts

---

#### Spring Cloud LoadBalancer

- Client-side Load Balancing

---

#### Resilience4j

- Circuit Breaker
- Retry
- Bulkhead
- Rate Limiter
- Timeout
- Fallback

---

### Spring Data

#### PostgreSQL

Store relational data:

- Users
- Roles
- Permissions
- Orders
- Payments
- Addresses
- Shipping
- Audit

---

#### MongoDB

Store document-oriented data:

- Products
- Categories
- Brands
- Reviews
- Shopping Cart
- Wishlist
- Product Images
- Product Metadata

---

#### Redis

Store high-speed transient data:

- Sessions
- Product Cache
- Shopping Cart Cache
- OTP
- JWT Blacklist
- Trending Products
- Dashboard Cache
- Rate Limiting
- User Cache

---

### Messaging

Include

- RabbitMQ

Demonstrate

- Publisher
- Consumer
- Dead Letter Queue
- Retry Queue
- Event Driven Architecture

---

### Observability

The project **must** include a complete observability stack.

---

#### Spring Actuator

Demonstrate

- Health
- Info
- Metrics
- Environment
- Thread Dump
- Beans
- Loggers

---

#### Micrometer

- JVM Metrics
- Custom Metrics
- Business Metrics

---

#### Prometheus

Collect metrics from all services.

---

#### Grafana

Dashboards should include

- JVM Metrics
- HTTP Requests
- Response Time
- Error Rate
- Cache Metrics
- Database Metrics
- Redis Metrics
- RabbitMQ Metrics
- Kubernetes Metrics

---

#### OpenTelemetry

Instrument

- REST APIs
- Feign Clients
- Database Calls
- RabbitMQ
- Redis

---

#### Jaeger

Trace

- User Login
- Product Search
- Checkout
- Order Placement

---

#### Loki

Centralised Logging

---

#### Promtail

Log Collection

---

### Frontend

React

- React 19
- TypeScript
- Vite
- Material UI
- React Router
- TanStack Query
- Axios
- React Hook Form
- Zod
- Recharts

---

### Infrastructure

- Docker
- Docker Compose
- Helm
- Kubernetes
- NGINX Ingress

---

## Repository Structure

```
commerce-mesh/
├── charts/
├── docker-compose.yaml
├── docs/
├── scripts/
├── ui-admin-console/
├── discovery-server/
├── config-server/
├── api-gateway/
├── authentication-service/
├── user-service/
├── product-catalog-service/
├── cart-service/
├── order-service/
├── inventory-service/
├── payment-service/
├── notification-service/
├── search-service/
├── common-security-library/
├── common-api-library/
├── common-domain-library/
├── common-event-library/
├── common-exception-library/
├── monitoring/
├── README.md
└── AGENT.md
```

---

## High-Level Architecture

```mermaid
flowchart TD
    UI[React Admin Portal] --> Ingress[NGINX Ingress]
    Ingress --> Gateway[Spring Cloud Gateway]
    Gateway --> Discovery[Eureka Discovery]

    Gateway --> Auth[Authentication Service]
    Gateway --> User[User Service]
    Gateway --> Catalog[Product Catalog Service]
    Gateway --> Cart[Cart Service]
    Gateway --> Inventory[Inventory Service]
    Gateway --> Order[Order Service]
    Gateway --> Payment[Payment Service]
    Gateway --> Notification[Notification Service]
    Gateway --> Search[Search Service]

    Auth --> DataStores[(PostgreSQL + MongoDB + Redis)]
    User --> DataStores
    Catalog --> DataStores
    Cart --> DataStores
    Inventory --> DataStores
    Order --> DataStores
    Payment --> DataStores
    Notification --> DataStores
    Search --> DataStores

    Order --> Queue[(RabbitMQ)]
    Notification --> Queue

    DataStores --> Observability[Prometheus + Grafana + Jaeger + Loki + Promtail]
```

---

## Root Structure

```
charts/
docker-compose.yaml
docs/
scripts/
monitoring/
ui-admin-console/
backend/
├── discovery-server/
├── config-server/
├── api-gateway/
├── authentication-service/
├── user-service/
├── product-catalog-service/
├── cart-service/
├── inventory-service/
├── payment-service/
├── order-service/
├── notification-service/
└── search-service/
libraries/
├── common-api-library/
├── common-security-library/
├── common-events-library/
├── common-model-library/
└── common-exception-library/
```

---

## Backend Services

### 1. Discovery Server

**Topics**

- Eureka Server

---

### 2. Config Server

**Topics**

- Centralised Configuration

---

### 3. API Gateway

**Topics**

- Gateway
- JWT Validation
- Routing
- Filters
- Rate Limiting

---

### 4. Authentication Service

**Responsibilities**

- Registration
- Login
- Logout
- Refresh Token
- Password Reset
- Email Verification

**Database: PostgreSQL**

**Spring Topics**

- Spring Security
- JWT
- BCrypt

---

### 5. User Service

**Responsibilities**

- Profile
- Address
- Roles
- Preferences

**Database: PostgreSQL**

**Topics**

- Spring Data JPA
- Pageable
- Specifications

---

### 6. Product Catalog Service

**Responsibilities**

- Products
- Categories
- Brands
- Reviews
- Search

**Database: MongoDB**

**Topics**

- Spring Data MongoDB
- Aggregation
- Pagination
- Indexing

---

### 7. Cart Service

**Responsibilities**

- Shopping Cart
- Wishlist
- Coupons

**Database: MongoDB**

**Cache: Redis**

**Topics**

- Spring Cache
- RedisTemplate
- TTL
- Cache Eviction

---

### 8. Inventory Service

**Responsibilities**

- Warehouse
- Inventory
- Stock Reservation

**Database: PostgreSQL**

---

### 9. Order Service

**Responsibilities**

- Checkout
- Orders
- History

**Database: PostgreSQL**

**Topics**

- Transactions
- RabbitMQ

---

### 10. Payment Service

**Responsibilities**

- Payments
- Refunds
- Invoices

**Topics**

- Retry
- Circuit Breaker

---

### 11. Notification Service

**Responsibilities**

- Email
- SMS
- Push Notifications

**Topics**

- Async
- RabbitMQ Consumers

---

### 12. Search Service

**Responsibilities**

- Product Search
- Auto Complete
- Search Suggestions

**Database: MongoDB**

---

## React UI

Repository: `ui-admin-console`

---

### Pages

- Login
- Dashboard
- Products
- Categories
- Inventory
- Orders
- Cart
- Payments
- Customers
- Users
- Roles
- Permissions
- Reports
- Monitoring
- Settings

---

### Components

- Navbar
- Sidebar
- Header
- Footer
- ProductTable
- OrderTable
- UserTable
- DashboardCards
- Charts
- Dialogs
- Forms
- Pagination
- Filters
- Snackbar
- Loading
- ProtectedRoute

---

### Hooks

- `useAuth()`
- `useProducts()`
- `useOrders()`
- `useInventory()`
- `usePayments()`
- `useUsers()`
- `useCart()`

---

### API Services

- `authApi.ts`
- `userApi.ts`
- `productApi.ts`
- `cartApi.ts`
- `orderApi.ts`
- `paymentApi.ts`
- `inventoryApi.ts`

---

## Authentication

Implement

- JWT
- Refresh Tokens
- BCrypt
- RBAC
- Method Security

**Roles**

- `SUPER_ADMIN`
- `ADMIN`
- `PRODUCT_MANAGER`
- `ORDER_MANAGER`
- `CUSTOMER_SUPPORT`
- `CUSTOMER`

**Default Administrator**

- **Username:** `admin`
- **Password:** `Admin@123`

---

## Admin Dashboard

Include widgets for:

- Active Users
- Orders Today
- Revenue
- Products
- Inventory Status
- Low Stock
- Shopping Carts
- Top Customers
- Top Products
- Failed Payments
- RabbitMQ Queue Status
- Cache Statistics
- JVM Statistics
- API Metrics
- Request Rate
- Error Rate

---

## Docker

Each backend should include

- Multi-stage Dockerfile
- Non-root User
- Healthcheck
- Small Runtime Image

---

## Docker Compose

The root compose should start:

**Infrastructure**

- PostgreSQL
- pgAdmin
- MongoDB
- Mongo Express
- Redis
- Redis Insight
- RabbitMQ
- RabbitMQ Management

**Spring Cloud**

- Config Server
- Eureka Server
- API Gateway

**Microservices**

- Authentication Service
- User Service
- Product Service
- Cart Service
- Inventory Service
- Order Service
- Payment Service
- Notification Service
- Search Service

**Frontend**

- React UI

**Observability**

- Prometheus
- Grafana
- Loki
- Promtail
- Jaeger

---

## Helm

Each project must contain

```
helm/
├── Chart.yaml
├── values.yaml
├── NOTES.txt
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    ├── secret.yaml
    ├── ingress.yaml
    ├── serviceaccount.yaml
    ├── networkpolicy.yaml
    └── hpa.yaml
```

---

## Kubernetes Best Practices

Every service should include

- Resource Requests
- Resource Limits
- Liveness Probe
- Readiness Probe
- Startup Probe
- Rolling Updates
- ConfigMaps
- Secrets
- Anti Affinity
- Pod Disruption Budget
- HPA
- ServiceAccount
- SecurityContext
- Non-root Containers

---

## Ingress

Create one NGINX Ingress.

**Routes**

- `/`
- `api/auth`
- `api/users`
- `api/products`
- `api/cart`
- `api/orders`
- `api/payments`
- `api/inventory`
- `api/search`

---

## Testing

Every backend must include

### Unit Tests

- Controllers
- Services
- Repositories
- Security
- Validators
- Mappers
- Cache
- RabbitMQ Publishers
- RabbitMQ Consumers

Coverage: **90%+**

---

### Integration Tests

**Use**

- Spring Boot Test
- Testcontainers
- PostgreSQL
- MongoDB
- Redis
- RabbitMQ

**Test**

- Authentication
- Authorization
- Gateway
- Eureka Registration
- Feign Clients
- Circuit Breakers
- Cache
- Messaging
- Database
- REST APIs

---

## Frontend Tests

**Use**

- Vitest
- React Testing Library

**Test**

- Components
- Hooks
- API Clients
- Forms
- Dashboard
- Protected Routes

---

## Spring Boot & Spring Cloud Best Practices

The AI Agent **must** always follow:

### Architecture

- Package-by-feature organisation.
- Clean Architecture.
- SOLID principles.
- DRY and KISS.
- Constructor injection only.
- Thin controllers with business logic confined to services.
- DTOs for all API contracts.
- MapStruct for object mapping.

### Cloud

- Gateway as the sole public entry point.
- Eureka for service discovery.
- Spring Cloud Config for centralised configuration.
- OpenFeign for service-to-service communication.
- Resilience4j for fault tolerance.

### Security

- Explicit `SecurityFilterChain`.
- Stateless JWT authentication.
- BCrypt password hashing.
- Method-level security (`@PreAuthorize`).
- Fine-grained RBAC and permissions.
- Secure CORS and CSRF configuration.

### Persistence

- PostgreSQL for transactional data.
- MongoDB for catalogue and cart documents.
- Redis for caching, sessions and transient state.

### Messaging

- RabbitMQ for asynchronous communication.
- Idempotent consumers.
- Dead-letter queues and retry policies.

### Observability

- Spring Boot Actuator on all services.
- Micrometer metrics.
- Prometheus scraping.
- Grafana dashboards.
- OpenTelemetry instrumentation.
- Jaeger distributed tracing.
- Loki centralised logging.

### API Design

- Versioned REST APIs.
- Consistent response models.
- Standard HTTP status codes.
- OpenAPI documentation.

### Testing

- Unit tests for all business logic.
- Integration tests for persistence, messaging and cloud components.
- Maintain at least **90%** code coverage.

---

## Development Workflow

For every feature, the AI Agent should:

1. Design the domain model.
2. Create database schemas and MongoDB collections.
3. Configure Redis caching.
4. Implement repositories.
5. Build business services.
6. Secure APIs with Spring Security.
7. Register services with Eureka.
8. Configure Gateway routes.
9. Add Feign clients.
10. Configure Resilience4j.
11. Publish or consume RabbitMQ events where appropriate.
12. Instrument with Actuator, Micrometer and OpenTelemetry.
13. Build or update the React UI.
14. Write unit tests.
15. Write integration tests.
16. Build Docker images.
17. Update Docker Compose.
18. Create or update Helm charts.
19. Configure Kubernetes Ingress.
20. Update project documentation.

---

## Expected Deliverable

The completed repository should resemble a real-world enterprise e-commerce platform and demonstrate:

- Spring Boot 3
- Spring Cloud Gateway
- Eureka Service Discovery
- Spring Cloud Config
- OpenFeign
- Resilience4j
- Spring Security with JWT
- PostgreSQL, MongoDB and Redis integration
- RabbitMQ event-driven communication
- Complete observability with Actuator, Micrometer, Prometheus, Grafana, OpenTelemetry, Jaeger, Loki and Promtail
- React administration portal
- Docker-based local development
- Kubernetes deployment with Helm and NGINX Ingress
- Production-quality architecture, testing and documentation

The repository should serve as a comprehensive reference implementation for building cloud-native, enterprise-scale Spring Boot microservices.
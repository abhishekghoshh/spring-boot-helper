# AGENT.md

## Event-Driven Real-Time Order Processing System

*A Complete Spring Boot + React + Kafka (KRaft) Learning Repository*

---

## Objective

Build a production-grade, enterprise-style event-driven platform using modern Spring Boot practices.

This repository is designed to teach every major area of Spring Boot through realistic microservices while also covering frontend development with React, containerisation, Kubernetes deployment and modern software architecture.

The agent should build the project incrementally while following clean architecture and production-ready coding standards.

---

## High-Level Architecture

```mermaid
flowchart TD
    UI[React Admin UI] -->|REST APIs| Order[Order API]
    UI -->|REST APIs| Inventory[Inventory API]
    UI -->|REST APIs| Payment[Payment API]
    UI -->|REST APIs| Shipping[Shipping API]
    UI -->|REST APIs| Notification[Notification API]

    Order --> Kafka[[Kafka - KRaft]]
    Inventory --> Kafka
    Payment --> Kafka
    Shipping --> Kafka
    Notification --> Kafka

    Kafka --> EDS[Event Driven System]
```

Every backend communicates through Kafka events.

REST should only be used by the frontend.

No backend service should directly call another backend unless absolutely necessary.

---

## Technology Stack

### Backend

- Java 21
- Spring Boot 3.x
- Spring MVC
- Spring WebFlux (where appropriate)
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Cache
- Spring Scheduling
- Spring Events
- Spring Retry
- Spring Actuator
- Spring Kafka
- Spring Cloud Stream (optional advanced implementation)
- PostgreSQL
- Flyway
- MapStruct
- Lombok
- OpenAPI / Swagger
- Testcontainers
- JUnit 5
- Mockito
- Awaitility
- ArchUnit

---

### Messaging

- Apache Kafka
- Kafka KRaft Mode
- Dead Letter Topics
- Retry Topics
- Event Versioning
- Event Schema
- Idempotent Consumers

---

### UI

- React
- Vite
- React Router
- TypeScript
- TanStack Query
- Axios
- Material UI
- React Hook Form
- Zod
- Recharts

---

### Infrastructure

- Docker
- Docker Compose
- Helm
- Kubernetes
- Ingress
- NGINX Ingress Controller

---

## Repository Structure

```
event-driven-order-system/
├── charts/
├── docker-compose.yaml
├── docs/
├── scripts/
├── ui-admin-portal/
├── order-command-service/
├── inventory-service/
├── payment-service/
├── shipping-service/
├── notification-service/
├── common-event-library/
├── common-security-library/
├── common-api-library/
├── common-exception-library/
└── README.md
```

---

## Common Structure for Every Spring Boot Service

```
service-name/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── mapper/
│   │   │   ├── event/
│   │   │   ├── consumer/
│   │   │   ├── producer/
│   │   │   ├── scheduler/
│   │   │   ├── validator/
│   │   │   ├── security/
│   │   │   ├── exception/
│   │   │   ├── util/
│   │   │   ├── constant/
│   │   │   ├── configuration/
│   │   │   ├── health/
│   │   │   ├── filter/
│   │   │   └── advice/
│   │   └── resources/
│   └── test/
├── Dockerfile
├── pom.xml
├── README.md
└── helm/
```

---

## Common Components Required in Every Service

Every backend must include:

- Spring Security
- JWT Authentication
- Role Based Access Control
- Global Exception Handler
- Validation
- DTO Layer
- Entity Layer
- Repository Layer
- Service Layer
- Controller Layer
- Mapper Layer
- Kafka Producer
- Kafka Consumer
- Unit Tests
- Integration Tests
- Dockerfile
- Helm Chart
- Kubernetes Deployment
- Kubernetes Service
- Ingress
- ConfigMap
- Secrets
- Health Checks
- Graceful Shutdown
- Swagger Documentation

---

## Authentication

Every project must include:

### Admin User

- **Username:** `admin`
- **Password:** `Admin@123`

Admin should be seeded automatically.

---

## Roles

- ADMIN
- CUSTOMER

---

## Admin Dashboard

The React application must include

- Dashboard
- Login
- User Management
- Orders
- Inventory
- Payments
- Shipping
- Notifications
- Kafka Event Monitor
- System Health
- Audit Logs

---

## Projects

### Project 1: Order Command Service

**Purpose**

Receives customer orders.

**Responsibilities**

- Create Order
- Cancel Order
- Update Order
- Publish OrderCreated Event
- Publish OrderCancelled Event

**Spring Topics**

- REST API
- Validation
- Spring Data JPA
- Transactions
- DTO Mapping
- OpenAPI

**Kafka Topics**

- `order-created`
- `order-cancelled`

---

### Project 2: Inventory Service

**Responsibilities**

- Reserve Inventory
- Release Inventory
- Stock Management

**Consumes**

- `order-created`

**Produces**

- `inventory-reserved`
- `inventory-failed`

**Spring Topics**

- Kafka Consumer
- Kafka Producer
- Retry
- Dead Letter Queue

---

### Project 3: Payment Service

**Responsibilities**

- Payment Processing
- Refund
- Payment Validation

**Consumes**

- `inventory-reserved`

**Produces**

- `payment-success`
- `payment-failed`

**Spring Topics**

- Spring Retry
- Transactions
- Idempotency

---

### Project 4: Shipping Service

**Responsibilities**

- Shipment Creation
- Shipment Tracking

**Consumes**

- `payment-success`

**Produces**

- `shipment-created`

**Spring Topics**

- Scheduling
- Async Processing

---

### Project 5: Notification Service

**Responsibilities**

- Email Notifications
- SMS Notifications
- Push Notifications

**Consumes**

All Events

**Produces**

None

**Spring Topics**

- Async
- Scheduling
- Event Listeners

---

## Shared Libraries

### common-event-library

Contains

- Kafka Events
- Event Versions
- Shared DTOs

---

### common-security-library

Contains

- JWT
- Security Filters
- Authentication
- Authorization

---

### common-api-library

Contains

- API Response
- Pagination
- Utilities

---

### common-exception-library

Contains

- Exception Classes
- Global Error Models

---

## React Application

Folder: `ui-admin-portal`

**Pages**

- Login
- Dashboard
- Orders
- Inventory
- Payments
- Shipping
- Notifications
- Users
- Settings

**Components**

- Navbar
- Sidebar
- Header
- Footer
- DataTable
- Charts
- Cards
- Dialogs
- Forms
- Loading
- Snackbar
- ProtectedRoute

**Hooks**

- `useOrders`
- `useInventory`
- `usePayments`
- `useShipping`
- `useNotifications`
- `useUsers`

**Services**

- `orderApi.ts`
- `inventoryApi.ts`
- `paymentApi.ts`
- `shippingApi.ts`
- `notificationApi.ts`
- `authApi.ts`

---

## Docker

Every backend should include

- Multi-stage Dockerfile
- Non-root user
- Small image
- Health Check
- Environment Variables
- Build Cache Optimisation

The root `docker-compose.yaml` should start:

- PostgreSQL
- Kafka (KRaft)
- Kafka UI
- Order Service
- Inventory Service
- Payment Service
- Shipping Service
- Notification Service
- React UI

---

## Helm

Each service should have

```
helm/
├── Chart.yaml
├── values.yaml
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    ├── secret.yaml
    ├── ingress.yaml
    ├── hpa.yaml
    ├── serviceaccount.yaml
    └── _helpers.tpl
```

Best practices

- Resource limits and requests
- Readiness probes
- Liveness probes
- Rolling updates
- ConfigMaps
- Secrets
- Labels
- Selectors
- Values-driven configuration
- Separate values for local and production

---

## Ingress

Configure a single NGINX Ingress.

Example routes:

- `/api/orders`
- `/api/inventory`
- `/api/payment`
- `/api/shipping`
- `/api/notification`
- `/`

The React application should be served from `/`.

---

## Testing Requirements

Every service must include:

### Unit Tests

- Service Layer
- Controller Layer
- Mapper Layer
- Validators
- Utility Classes

Target coverage: **> 90%**

---

### Integration Tests

- REST APIs
- Repository Layer
- Kafka Producers
- Kafka Consumers
- PostgreSQL
- Testcontainers

---

### Frontend Testing

Use

- Vitest
- React Testing Library

Include tests for

- Components
- Hooks
- Forms
- API Clients
- Protected Routes

---

## Spring Boot Best Practices

The agent must always follow modern Spring Boot best practices:

- Follow SOLID principles.
- Prefer constructor injection over field injection.
- Keep controllers thin and move business logic to services.
- Use DTOs for API contracts; never expose JPA entities directly.
- Use MapStruct for object mapping.
- Apply validation with Jakarta Bean Validation.
- Centralise exception handling with `@ControllerAdvice`.
- Keep configuration externalised using profiles and environment variables.
- Use immutable records for request/response DTOs where appropriate.
- Design idempotent Kafka consumers.
- Implement retries and Dead Letter Topics for failed event processing.
- Use transactional boundaries carefully to avoid partial updates.
- Follow package-by-feature where it improves cohesion.
- Maintain clear separation between domain, application and infrastructure concerns.
- Write meaningful logs without exposing sensitive information.
- Keep code readable, modular and testable.
- Use Flyway for database migrations.
- Generate OpenAPI documentation for every REST endpoint.
- Ensure all code passes static analysis and formatting checks.
- Document public APIs and complex business logic.

---

## Development Workflow

For every feature, the agent should:

1. Design the domain model.
2. Create database migrations.
3. Implement entities and repositories.
4. Build the service layer.
5. Expose REST APIs.
6. Add Kafka producers or consumers.
7. Implement React UI changes.
8. Write unit tests.
9. Write integration tests.
10. Create Docker configuration.
11. Update Docker Compose.
12. Create or update Helm charts.
13. Verify deployment through Ingress.
14. Update project documentation.

---

## Explicit Exclusions

Do **not** include the following in this repository:

- Observability (Prometheus, Grafana, OpenTelemetry, Jaeger, Zipkin, etc.)
- Service Mesh
- API Gateway
- Distributed Tracing
- Monitoring or Logging stacks beyond basic application logging

The focus is on mastering Spring Boot, Kafka, React, containerisation, Kubernetes deployment and clean, production-ready application architecture.
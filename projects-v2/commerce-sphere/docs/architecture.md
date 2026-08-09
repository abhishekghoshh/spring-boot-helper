# Architecture

## Overview

CommerceSphere is built as five independent Spring Boot microservices, each with its own database connectivity, following a no-shared-library, no-monorepo-tooling philosophy. Services communicate only over REST APIs.

## Service Architecture

Each service follows a clean layered architecture:

```
Controller → Service → Repository → MongoDB/Redis
     ↓          ↓
   DTOs      Mappers
```

### Design Principles

- **Package by feature** — Each service owns its domain model, DTOs, mappers, and repositories
- **Constructor injection** — All dependencies injected via constructor
- **No shared libraries** — Each service has its own `pom.xml` and all its own classes
- **Virtual Threads** — Every service runs on virtual threads (`spring.threads.virtual.enabled=true`)
- **Global exception handling** — `@ControllerAdvice` in every service returns consistent error responses

## Data Storage Strategy

### MongoDB (Primary Data Store)
- Products, Categories, Brands (Catalog Service)
- Users, Roles (Identity Service)
- Orders (Order Service)
- Inventory, Warehouses (Inventory Service)
- Cart History (Cart Service)

### Redis (Cache + Transient Data)
- Shopping Cart (Cart Service — 7 day TTL)
- Wishlist (Cart Service — 30 day TTL)
- Refresh Tokens (Identity Service — 30 day TTL)
- JWT Blacklist (Identity Service)
- Product Cache (Catalog Service — 10 min TTL)
- Stock Cache (Inventory Service)
- Checkout Sessions (Order Service — 30 min TTL)
- Rate Limiting (Identity Service)

## Authentication Flow

```
1. User registers/logs in → Identity Service
2. Identity Service returns JWT access token + refresh token
3. Client includes JWT in Authorization header for all requests
4. Identity Service JwtAuthFilter validates token on each request
5. Refresh tokens stored in Redis with TTL
6. Other services can validate tokens by calling Identity Service
```

## Inter-Service Communication

Services communicate via REST using environment-configured URLs:

```
Order Service → Cart Service: Get cart contents during checkout
Order Service → Inventory Service: Check and reserve stock
Frontend → All Services: Direct REST API calls through nginx proxy
```

In Docker Compose, service URLs use Docker service names (e.g., `http://cart-service:8083`).

## API Design

- RESTful with JSON request/response bodies
- Consistent error response format: `{ "error": "ERROR_CODE", "message": "Human readable", "status": 400 }`
- OpenAPI/Swagger documentation on every service
- Pagination using Spring Data `Page` objects
- DTOs separate from MongoDB documents

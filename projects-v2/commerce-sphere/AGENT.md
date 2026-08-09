# AGENT.md

## Project: CommerceSphere

### E-Commerce Product Catalog with Hybrid Storage (Spring Boot + MongoDB + Redis + React)

> **Objective**
>
> Build a production-grade e-commerce platform that demonstrates how to use **Spring Boot** with **MongoDB** and **Redis** in a real-world hybrid storage architecture.
>
> The platform should teach core Spring Boot concepts through multiple independent microservices while following enterprise software architecture and modern Spring Boot best practices.
>
> The AI Agent should generate production-ready, modular, testable code with a clean architecture. Every service must include a React-based UI, comprehensive testing, Docker support, Helm charts, Kubernetes manifests and NGINX Ingress configuration.
>
> **Do not implement observability (Prometheus, Grafana, OpenTelemetry, Jaeger, ELK, etc.).**

---

## Technology Baseline

- **Java 25 (LTS)** for every backend service, with **Spring Boot 4.1** (latest GA) as the baseline, paired with matching current-generation Spring Security, Spring Data MongoDB/Redis, MapStruct and springdoc-openapi versions compatible with it.
- **Virtual Threads** must be enabled on every service (`spring.threads.virtual.enabled=true`); Tomcat, MongoDB/Redis blocking calls and `@Async`/scheduled work should run on virtual threads instead of platform thread pools. Avoid `synchronized` blocks; prefer `ReentrantLock` where locking is required.
- **React 19.2** (latest GA) as the baseline for both frontend apps, paired with the current stable TypeScript, Vite, TanStack Query, React Router, Material UI, React Hook Form and Zod versions compatible with it.
- Beyond these pinned baselines, always prefer the **latest stable release** of every other library in the stack at implementation time instead of pinning to older majors.
- Prefer modern patterns (React Compiler-friendly code, function components, hooks, no legacy class components).

---

## No Shared Libraries / No Monorepo Tooling

This repository is a collection of **fully independent Spring Boot projects**, not a monorepo with shared code.

- There is **no parent POM** and **no shared/common library** (no `common-api-library`, `common-security-library`, `common-model-library`, `common-exception-library`).
- Every service under `backend/` owns **its own** `pom.xml` (or Gradle build), its own DTOs, mappers, exception classes and security config — duplication across services is expected and acceptable.
- Do not introduce cross-module Maven/Gradle dependencies between backend services. Services only communicate over the network (REST), never via shared JARs.
- Do not extract multi-module Maven reactor builds, BOMs, or Gradle composite builds to "deduplicate" services — each service must build and deploy on its own.
- Each React app under `ui/` has its own `package.json`, its own components/hooks/API clients — no shared npm workspace or shared component library between `ui/admin-console` and `ui/storefront`.

---

## Implementation Expectations

When asked to generate a service or feature, the AI Agent must produce **actual working implementation code**, not placeholders or scaffolding:

- Real controllers, services, repositories, documents, DTOs, mappers, security config, and exception handlers with full method bodies — not `// TODO` stubs or empty classes.
- Real React components, hooks and API service files with working logic (state, effects, API calls, form validation) — not empty JSX shells or placeholder components.
- Configuration files (`application.yml`, `.env`, Dockerfiles, Helm values) should contain concrete, usable values consistent with the rest of the stack, not generic placeholders left for the user to fill in.

---

## Objectives

The repository should help developers understand:

- Spring Boot Fundamentals
- Spring MVC
- Spring Data MongoDB
- Spring Data Redis
- Spring Security
- JWT Authentication
- Role Based Access Control (RBAC)
- Bean Validation
- Global Exception Handling
- File Upload
- Spring Events
- Async Processing
- Scheduling
- Caching
- Pagination
- Search
- MongoDB Aggregation
- MongoDB Indexing
- Redis Caching
- Redis Session Management
- Docker
- Docker Compose
- Kubernetes
- Helm
- NGINX Ingress
- Unit Testing
- Integration Testing
- React + TypeScript

---

## Technology Stack

### Backend

- Java 25 (virtual threads enabled)
- Spring Boot 4.1
- Spring Security
- Spring MVC
- Spring Validation
- Spring Data MongoDB
- Spring Data Redis
- Spring Cache
- Spring Scheduling
- Spring Events
- Spring Mail
- MapStruct
- Lombok
- OpenAPI / Swagger

---

### Databases

#### MongoDB

Use MongoDB for

- Products
- Categories
- Brands
- Reviews
- Product Specifications
- Product Images

#### Redis

Use Redis for

- Shopping Cart
- User Session
- JWT Blacklist
- Frequently Accessed Products Cache
- Wishlist Cache
- OTP Storage
- Rate Limiting

---

### Frontend

- React 19.2
- TypeScript
- Vite
- React Router
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
- NGINX Ingress

---

## Repository Structure

```
commerce-sphere/
├── charts/
├── docker-compose.yaml
├── docs/
├── scripts/
├── ui/
│   ├── admin-console/
│   └── storefront/
├── backend/
│   ├── identity-service/
│   ├── catalog-service/
│   ├── cart-service/
│   ├── inventory-service/
│   └── order-service/
├── README.md
└── AGENT.md
```

Each directory under `backend/` and `ui/` is a standalone, independently buildable project (its own build file, no shared parent, no shared library module).

---

## High-Level Architecture

```mermaid
flowchart TD
    AdminUI[React Admin Console] -->|REST APIs| Identity[Identity Service]
    AdminUI -->|REST APIs| Catalog[Catalog Service]
    AdminUI -->|REST APIs| Cart[Cart Service]
    AdminUI -->|REST APIs| Inventory[Inventory Service]
    AdminUI -->|REST APIs| Order[Order Service]

    StorefrontUI[React Storefront] -->|REST APIs| Identity
    StorefrontUI -->|REST APIs| Catalog
    StorefrontUI -->|REST APIs| Cart
    StorefrontUI -->|REST APIs| Order

    Identity --> DataStores[(MongoDB + Redis)]
    Catalog --> DataStores
    Cart --> DataStores
    Inventory --> DataStores
    Order --> DataStores
```

---

## Root Components

- `charts/`
- `docker-compose.yaml`
- `docs/`
- `scripts/`
- `ui/admin-console/`
- `ui/storefront/`
- `backend/identity-service/`
- `backend/catalog-service/`
- `backend/cart-service/`
- `backend/inventory-service/`
- `backend/order-service/`

No `common-*` library modules and no parent/aggregator POM — every entry above is a fully independent, self-contained project.

---

## Common Structure for Every Backend Service

```
backend/service-name/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── config/
│   │   │   ├── configuration/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── mapper/
│   │   │   ├── validation/
│   │   │   ├── exception/
│   │   │   ├── advice/
│   │   │   ├── security/
│   │   │   ├── cache/
│   │   │   ├── event/
│   │   │   ├── listener/
│   │   │   ├── scheduler/
│   │   │   ├── util/
│   │   │   └── constant/
│   │   └── resources/
│   └── test/
├── Dockerfile
├── README.md
└── helm/
```

---

## Project 1: Identity Service

Repository: `backend/identity-service`

**Responsibilities**

- User Registration
- Login
- JWT Authentication
- Refresh Token
- User Profile
- Password Change
- Role Management

**MongoDB Collections**

- `users`
- `roles`

**Redis**

- Sessions
- Refresh Tokens
- Login Attempts
- Blacklisted Tokens

**Spring Topics**

- Spring Security
- JWT
- Validation
- Filters
- Authentication
- RBAC

---

## Project 2: Catalog Service

Repository: `backend/catalog-service`

**Responsibilities**

- Product Management
- Category Management
- Brand Management
- Product Search
- Product Filtering
- Product Reviews
- Product Images

**MongoDB Collections**

- `products`
- `categories`
- `brands`
- `reviews`

**Redis**

- Product Cache
- Category Cache
- Featured Products
- Trending Products

**Spring Topics**

- MongoRepository
- Aggregation Pipeline
- Text Search
- Pagination
- Sorting
- Indexing
- Caching

---

## Project 3: Cart Service

Repository: `backend/cart-service`

**Responsibilities**

- Add Product
- Remove Product
- Update Quantity
- Save Cart
- Merge Guest Cart
- Wishlist

**Redis**

- `shopping-cart`
- `wishlist`
- `recently-viewed`
- `coupon-cache`

**MongoDB**

- Cart History

**Spring Topics**

- RedisTemplate
- Spring Cache
- Serialization
- Session Management
- TTL

---

## Project 4: Inventory Service

Repository: `backend/inventory-service`

**Responsibilities**

- Stock Management
- Warehouse Management
- Product Availability
- Inventory Reservation

**MongoDB**

- `inventory`
- `warehouse`
- `stock-movement`

**Redis**

- `stock-cache`

**Spring Topics**

- Transactions
- Caching
- Scheduling

---

## Project 5: Order Service

Repository: `backend/order-service`

**Responsibilities**

- Checkout
- Place Order
- Order History
- Cancel Order
- Track Order

**MongoDB**

- `orders`

**Redis**

- `checkout-cache`
- `payment-session`
- `recent-orders`

**Spring Topics**

- Validation
- Events
- Scheduling
- Async

---

## React Project: Admin Console

Repository: `ui/admin-console`

### Pages

- Login
- Dashboard
- Products
- Categories
- Brands
- Inventory
- Orders
- Customers
- Shopping Cart
- Wishlist
- Analytics
- Settings
- Users

---

### Components

- Navbar
- Sidebar
- Header
- Footer
- Cards
- ProductTable
- CategoryTable
- InventoryTable
- OrderTable
- UserTable
- CartTable
- Charts
- Dialogs
- Forms
- Pagination
- SearchBar
- Filters
- Loading
- Snackbar
- ProtectedRoute

---

### Hooks

- `useAuth`
- `useProducts`
- `useCategories`
- `useBrands`
- `useInventory`
- `useOrders`
- `useCart`
- `useWishlist`
- `useUsers`

---

### Services

- `authApi.ts`
- `productApi.ts`
- `categoryApi.ts`
- `brandApi.ts`
- `cartApi.ts`
- `inventoryApi.ts`
- `orderApi.ts`
- `userApi.ts`

---

## React Project: Customer Storefront

Repository: `ui/storefront`

A customer-facing storefront for the normal (non-admin) end user to browse, search and purchase products.

### Pages

- Home
- Product Listing
- Product Detail
- Category Landing
- Search Results
- Shopping Cart
- Wishlist
- Checkout
- Order Confirmation
- Order History
- Order Detail
- Login
- Register
- Forgot Password
- My Account / Profile
- Address Book
- Not Found (404)

---

### Components

- Navbar
- Footer
- HeroBanner
- ProductCard
- ProductGrid
- ProductGallery
- ProductReviews
- CategoryMenu
- Breadcrumbs
- CartDrawer
- CartItem
- WishlistButton
- CheckoutSteps
- AddressForm
- PaymentForm
- OrderSummary
- SearchBar
- Filters
- SortDropdown
- Pagination
- RatingStars
- Loading
- Snackbar
- ProtectedRoute

---

### Hooks

- `useAuth`
- `useProducts`
- `useProductDetail`
- `useCategories`
- `useCart`
- `useWishlist`
- `useCheckout`
- `useOrders`
- `useSearch`
- `useReviews`

---

### Services

- `authApi.ts`
- `productApi.ts`
- `categoryApi.ts`
- `cartApi.ts`
- `wishlistApi.ts`
- `orderApi.ts`
- `reviewApi.ts`

---

### Access

- Publicly browsable (Home, Product Listing, Product Detail, Search, Category pages) without authentication.
- Authentication (via Identity Service) required for Checkout, Order History, Wishlist persistence and Account pages.
- Guest cart support with merge-on-login (backed by Cart Service).

---

## Admin Dashboard

Every project must expose an administration dashboard.

**Default Administrator**

- **Username:** `admin`
- **Password:** `Admin@123`

**Dashboard Widgets**

- Total Products
- Total Categories
- Total Orders
- Active Users
- Current Carts
- Inventory Levels
- Top Selling Products
- Recently Added Products
- Low Stock Products
- Monthly Orders

---

## Docker

Every backend service must include

- Multi-stage Dockerfile
- Non-root User
- Small Runtime Image
- Environment Variables
- Health Check
- Build Cache Optimisation

---

## Docker Compose

The root compose file should start

- MongoDB
- Mongo Express
- Redis
- Redis Insight
- Identity Service
- Catalog Service
- Cart Service
- Inventory Service
- Order Service
- React Admin Console
- React Storefront

---

## Helm

Each service should include

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
    ├── serviceaccount.yaml
    └── _helpers.tpl
```

---

## Kubernetes Best Practices

Every deployment should include

- Resource Requests
- Resource Limits
- ConfigMaps
- Secrets
- Readiness Probe
- Liveness Probe
- Rolling Update Strategy
- Environment Variables
- Labels
- Selectors
- Graceful Shutdown

---

## Ingress

Create a single NGINX Ingress.

**Routes**

- `/`
- `/admin`
- `api/auth`
- `api/catalog`
- `api/cart`
- `api/orders`
- `api/inventory`

The React Storefront application should be available at `/`, and the React Admin Console should be available at `/admin`.

---

## Unit Tests

Every backend service must include unit tests for

- Controllers
- Services
- Repositories
- Security
- Validators
- Mappers
- Cache Components
- Utility Classes

Coverage Target: **90%+**

---

## Integration Tests

Use

- Spring Boot Test
- Testcontainers
- MongoDB Container
- Redis Container

Test

- CRUD APIs
- MongoDB Repositories
- Redis Operations
- Cache Behaviour
- Security
- Authentication
- Pagination
- Search
- Shopping Cart

---

## Frontend Testing

Use

- Vitest
- React Testing Library

Test

- Components
- Hooks
- Forms
- API Clients
- Protected Routes
- Dashboard Widgets

---

## Spring Boot Best Practices

The AI Agent must always follow these principles:

### Architecture

- Use a layered architecture with clear separation of concerns.
- Prefer package-by-feature for better modularity.
- Keep controllers thin; delegate business logic to services.
- Encapsulate persistence logic within repositories.
- Separate domain models from API DTOs.

### Dependency Injection

- Use constructor injection only.
- Avoid field injection.

### API Design

- Build RESTful APIs with consistent versioning.
- Standardise request and response models.
- Use appropriate HTTP status codes.
- Generate OpenAPI documentation for all endpoints.

### Validation & Error Handling

- Validate all incoming requests with Jakarta Bean Validation.
- Implement global exception handling using `@ControllerAdvice`.
- Return consistent error responses.

### MongoDB

- Design documents for query efficiency.
- Create indexes for frequently queried fields.
- Use aggregation pipelines for analytics and reporting.
- Avoid unnecessary document nesting.
- Model relationships appropriately using embedding or references.

### Redis

- Use Redis only for transient and high-speed data.
- Configure TTL values for temporary data.
- Cache frequently accessed catalog data.
- Store shopping carts and user sessions in Redis.
- Implement cache eviction where appropriate.

### Security

- Use Spring Security with JWT.
- Implement RBAC.
- Encrypt passwords with BCrypt.
- Configure CORS appropriately.
- Protect sensitive endpoints with method-level security.

### Code Quality

- Follow SOLID, DRY and KISS principles.
- Write clean, readable and maintainable code.
- Use immutable DTOs (`record`) where appropriate.
- Keep methods small and focused.
- Document public APIs and complex business logic.

### Testing

- Write unit tests for all business logic.
- Add integration tests for MongoDB and Redis interactions.
- Ensure each service achieves at least 90% test coverage.

---

## Development Workflow

For every feature, the AI Agent should:

1. Design the domain model.
2. Create MongoDB document models.
3. Design Redis key structures.
4. Implement repositories.
5. Develop business services.
6. Expose REST APIs.
7. Secure endpoints with Spring Security.
8. Build or update the React Storefront UI and Admin Console UI.
9. Write unit tests.
10. Write integration tests.
11. Build Docker images.
12. Update Docker Compose.
13. Create or update Helm charts.
14. Configure NGINX Ingress.
15. Update project documentation.

---

## Deliverable

The final repository should represent a production-style e-commerce platform that demonstrates:

- Java 25 with virtual threads enabled across all services
- Spring Boot 4.1 backend services and a React 19.2 frontend, with all other libraries kept at their latest compatible stable releases
- Independent, non-monorepo services with no shared libraries or parent POM
- Spring Boot microservices
- MongoDB document modelling
- Redis caching and session management
- Hybrid storage architecture
- Secure JWT authentication
- React-based administration portal
- Dockerised local development
- Kubernetes deployment using Helm
- NGINX Ingress routing
- Comprehensive unit and integration testing
- Modern Spring Boot development best practices

The codebase should be modular, maintainable, educational and extensible, making it suitable for learning enterprise Spring Boot development with MongoDB and Redis.
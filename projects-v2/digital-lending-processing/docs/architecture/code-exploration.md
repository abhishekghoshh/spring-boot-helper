# Code Architecture & Exploration Guide

## How to Navigate This Codebase

This guide is for developers who want to understand the codebase structure, module dependencies, and the best way to explore the code.

---

## High-Level Code Architecture

### Module Dependency Graph

```
                    ┌──────────────────────┐
                    │     pom.xml (root)     │
                    │  Spring Boot 4.0.7     │
                    │  Spring Cloud 2025.1.2 │
                    └──────────┬────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ common-api-     │  │ common-domain-  │  │ common-exception│
│ library         │  │ library         │  │ -library        │
│ (no deps)       │  │ → spring-data   │  │ → spring-web    │
│                 │  │   mongodb       │  │   spring-security│
│ DTOs, Enums     │  │                 │  │                 │
│ ApiResponse     │  │ BaseDocument    │  │ GlobalException │
│ ApiPaths        │  │ BaseEntity      │  │ Handler         │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │ common-security-    │
                    │ library             │
                    │ → all 3 libs above  │
                    │ → spring-security   │
                    │ → nimbus-jose-jwt   │
                    │                     │
                    │ JwtTokenProvider    │
                    │ JwtAuthFilter       │
                    │ @CurrentUser        │
                    └──────────┬──────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
┌────────────────┐   ┌────────────────┐   ┌────────────────┐
│ config-server  │   │ discovery-     │   │ api-gateway    │
│                │   │ server         │   │                │
│ port 8888      │   │ port 8761      │   │ port 8080      │
│                │   │                │   │                │
│ Eureka client  │   │ Standalone     │   │ Gateway routes │
│ No common libs │   │ No common libs │   │ Eureka client  │
└────────────────┘   └────────────────┘   └────────────────┘

         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
┌────────────────┐   ┌────────────────┐   ┌────────────────┐
│ authentication │   │ customer-      │   │ loan-offer     │
│ -service       │   │ profile-service│   │ -service       │
│                │   │                │   │                │
│ → all 4 libs   │   │ → all 4 libs   │   │ → all 4 libs   │
│ → security     │   │ → feign        │   │ → redis        │
│ → mongodb      │   │ → mapstruct    │   │ → resilience4j │
│ → redis        │   │ → mongodb      │   │ → mongodb      │
│ → mail         │   │                │   │                │
│                │   │                │   │                │
│ port 8081      │   │ port 8082      │   │ port 8083      │
└────────────────┘   └────────────────┘   └────────────────┘

┌────────────────┐   ┌────────────────┐   ┌────────────────┐
│ emi-calc       │   │ loan-          │   │ loan-          │
│ -service       │   │ application    │   │ processing     │
│                │   │ -service       │   │ -service       │
│ → api + excptn │   │                │   │                │
│   + security   │   │ → all 4 libs   │   │ → all 4 libs   │
│                │   │ → mongodb      │   │ → mongodb      │
│ (stateless)    │   │ → feign        │   │ → feign        │
│                │   │                │   │ → resilience4j │
│ port 8084      │   │ port 8085      │   │ port 8086      │
└────────────────┘   └────────────────┘   └────────────────┘

┌────────────────┐   ┌────────────────┐
│ notification   │   │ document       │
│ -service       │   │ -service       │
│                │   │                │
│ → all 4 libs   │   │ → all 4 libs   │
│ → mongodb      │   │ → mongodb      │
│ → mail         │   │ → gridfs       │
│ → @Async       │   │                │
│                │   │                │
│ port 8087      │   │ port 8088      │
└────────────────┘   └────────────────┘
```

### Dependency Direction

```
Common Libraries  ◄──  Infrastructure Services  ◄──  Business Services
 (no Spring Boot)      (cloud components)           (full stack)
```

**Key Insight**: Libraries flow inward. Infrastructure services depend on libraries. Business services depend on everything. No service depends on another service's code — inter-service communication is strictly via REST APIs.

---

## Recommended Exploration Path

If you're new to this codebase, follow this exact order. Each step builds on the previous one.

### Step 1: Root Configuration

**File**: `pom.xml`

Understand the foundation. This file defines:
- The Spring Boot version (4.0.7) and Java version (25) used everywhere
- The Spring Cloud BOM (2025.1.2) that manages cloud dependency versions
- All 15 modules declared in `<modules>`

**Key takeaway**: Every module inherits from this parent. Changing a version here affects all services.

---

### Step 2: Common API Library

**Module**: `common-api-library/`

This is the simplest module — no external dependencies except Jackson. Read these files:

1. `src/main/java/com/loansphere/common/api/ApiResponse.java` — The universal response envelope. Every controller in every service wraps responses in this. Notice it uses a Java `record` (immutable DTO).

2. `src/main/java/com/loansphere/common/api/ApiPaths.java` — All API path constants. Services use these instead of hardcoded strings.

3. `src/main/java/com/loansphere/common/api/enums/Role.java` — The six user roles used for RBAC across the platform.

**Key insight**: This library defines the **vocabulary** of the platform. It has zero dependencies, so it can be included anywhere.

---

### Step 3: Common Domain Library

**Module**: `common-domain-library/`

Read these files:

1. `src/main/java/com/loansphere/common/domain/BaseDocument.java` — Every MongoDB entity across all services extends this. It provides `id`, `createdAt`, `updatedAt`, and optimistic locking via `@Version`.

**Key insight**: Every `@Document` class in every service extends `BaseDocument`. This ensures consistent audit fields and versioning everywhere.

---

### Step 4: Common Exception Library

**Module**: `common-exception-library/`

Read these files:

1. `src/main/java/com/loansphere/common/exception/ErrorResponse.java` — Structured error record. Notice the nested `ValidationError` record.

2. `src/main/java/com/loansphere/common/exception/GlobalExceptionHandler.java` — A single `@ControllerAdvice` that handles all exception types. Every service imports this and gets centralized error handling automatically.

3. Browse the four exception classes: `ResourceNotFoundException`, `BadRequestException`, `UnauthorizedException`, `ForbiddenException`.

**Key insight**: Services throw these custom exceptions. The global handler catches them and transforms them into consistent `ErrorResponse` JSON. Services don't write their own exception handling code.

---

### Step 5: Common Security Library

**Module**: `common-security-library/`

This is the most important library. Read in this order:

1. `src/main/java/com/loansphere/common/security/JwtTokenProvider.java` — JWT validation, user ID extraction, role extraction from tokens. Built on Nimbus JOSE library.

2. `src/main/java/com/loansphere/common/security/JwtAuthenticationFilter.java` — A `OncePerRequestFilter` that intercepts every HTTP request, extracts the Bearer token, validates it, and sets the Spring Security context.

3. `src/main/java/com/loansphere/common/security/CurrentUser.java` — A custom annotation that lets controllers inject the current user ID directly.

4. `src/main/java/com/loansphere/common/security/CurrentUserArgumentResolver.java` — Spring MVC argument resolver that reads the authenticated user from the security context.

**Key insight**: Every service includes this library and gets JWT authentication automatically. The filter runs on every request. Controllers use `@CurrentUser String userId` to get the authenticated user.

---

### Step 6: Infrastructure Services

Now understand the plumbing that connects everything.

**Start with Discovery Server** (`discovery-server/`):
- `DiscoveryServerApplication.java` — One annotation: `@EnableEurekaServer`. That's it.
- `application.yml` — Port 8761, disables self-registration.

Then **Config Server** (`config-server/`):
- `ConfigServerApplication.java` — `@EnableConfigServer` with native profile.
- `application.yml` — Reads config from classpath, registers with Eureka.

Finally **API Gateway** (`api-gateway/`):
- `application.yml` — **This is the most important config file**. Study each route definition. See how `lb://service-name` resolves via Eureka. Notice CORS is configured here, not in individual services.

**Key insight**: The gateway is the single entry point. All client requests go through it. It doesn't contain business logic — it routes, authenticates, and rate-limits.

---

### Step 7: Authentication Service (First Business Service)

**Module**: `authentication-service/`

This is the best service to study first because it demonstrates the full stack. Read in this order:

1. `AuthenticationServiceApplication.java` — Notice `@ComponentScan` scanning both `com.loansphere.common` and `com.loansphere.auth`. This is how common library beans are discovered.

2. `model/User.java` — Extends `BaseDocument`. Uses `@Indexed` for unique username/email constraints. Manual getters/setters (we avoided Lombok for Java 25 compatibility).

3. `repository/UserRepository.java` — Extends `MongoRepository<User, String>`. Spring Data auto-generates queries from method names.

4. `dto/` — Seven record DTOs, each in its own file. Notice `TokenResponse` has a convenience constructor.

5. `service/AuthService.java` — The core business logic: registration (BCrypt hashing, role assignment), JWT generation (Nimbus `MACSigner` with HS256), refresh token management (Redis), token blacklisting, password reset tokens.

6. `controller/AuthController.java` — Thin controller. Delegates everything to `AuthService`. Uses `@CurrentUser` for the `/me` endpoint. Login validates credentials and returns token pairs.

7. `config/SecurityConfig.java` — The security filter chain. Notice which endpoints are public (`/register`, `/login`, `/refresh-token`). JWT filter added before `UsernamePasswordAuthenticationFilter`. CSRF disabled (stateless API).

8. `config/DataInitializer.java` — `CommandLineRunner` that creates the default admin user if none exists.

**Key insight**: Study the **flow**: Client → Controller → Service → Repository → MongoDB. Tokens flow back: Controller ← Service (generates JWT) ← Redis (stores refresh token).

---

### Step 8: Customer Profile Service (CRUD Pattern)

**Module**: `customer-profile-service/`

This demonstrates the standard CRUD service pattern:

1. `CustomerProfileApplication.java` — Adds `@EnableFeignClients` (needed for cross-service calls).
2. `model/CustomerProfile.java` — Rich domain entity with employment, income, identity fields.
3. `repository/CustomerProfileRepository.java` — Custom query `findByUserId`.
4. `service/CustomerProfileService.java` — Standard create/read/update pattern.
5. `controller/CustomerProfileController.java` — Uses `@PreAuthorize` for role-based access. `@CurrentUser` extracts the authenticated user.
6. `config/SecurityConfig.java` — Also configures `WebMvcConfigurer` for the `@CurrentUser` argument resolver.

**Key insight**: This service demonstrates the `@PreAuthorize` pattern. Controllers declare who can access what. The JWT filter already set the roles in the security context, so `@PreAuthorize` just checks them.

---

### Step 9: Loan Offer Service (Caching + Resilience)

**Module**: `loan-offer-service/`

This demonstrates Redis caching and Resilience4j:

1. `LoanOfferApplication.java` — Adds `@EnableCaching` (Redis-backed).
2. `service/LoanOfferService.java` — Notice `@Cacheable("activeOffers")` and `@CacheEvict`. The Redis cache key is `activeOffers::all`. Cache invalidates on create/update.
3. `application.yml` — Resilience4j circuit breaker and retry config for `customerProfileService`. The circuit opens at 50% failure rate, retries up to 3 times.

**Key insight**: The caching layer sits between the controller and the database. When `getActiveOffers()` is called, Spring checks Redis first. Only on cache miss does it query MongoDB. On offer creation/update, the cache is evicted.

---

### Step 10: EMI Calculation Service (Stateless Computation)

**Module**: `emi-calculation-service/`

The simplest service — no database, no external calls:

1. `controller/EmiCalculationController.java` — Pure computation endpoints. Three formulas: EMI, amortization schedule, prepayment impact. All stateless `GET` endpoints with query parameters.

**Key insight**: Not every microservice needs a database. This service just does math. It can scale horizontally with zero coordination.

---

### Step 11: Loan Application Service (Events)

**Module**: `loan-application-service/`

This demonstrates Spring Application Events:

1. `service/LoanApplicationService.java` — Notice the inner record classes for events: `LoanSubmittedEvent`, `LoanApprovedEvent`, `LoanRejectedEvent`. When status changes, events are published via `ApplicationEventPublisher`. These events are consumed by the notification and processing services.

2. `model/LoanApplication.java` — Status field with lifecycle: `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED`.

**Key insight**: Services communicate asynchronously via events. The application service doesn't know who listens. It just publishes events. This is loose coupling.

---

### Step 12: Notification Service (Async Processing)

**Module**: `notification-service/`

This demonstrates `@Async` and `@Scheduled`:

1. `NotificationApplication.java` — `@EnableAsync` and `@EnableScheduling`.
2. `service/NotificationService.java` — `@Async` on `sendEmail()`. The caller returns immediately; email sending happens on a virtual thread. Mailpit catches emails in development.
3. `application.yml` — Virtual threads enabled. The async executor automatically uses virtual threads instead of a thread pool.

**Key insight**: With virtual threads, you don't need a thread pool. Each `@Async` method gets its own virtual thread, which is cheap and managed by the JVM.

---

### Step 13: Document Service (File Storage)

**Module**: `document-service/`

This demonstrates MongoDB GridFS:

1. `model/DocumentMetadata.java` — Metadata-only document. Actual file bytes live in GridFS collections.
2. `service/DocumentService.java` — `GridFSBucket` usage pattern: `uploadFromStream` stores file chunks; `downloadToStream` retrieves them. Files > 10 MB are rejected.

**Key insight**: Metadata and file bytes are separated. Queries for file listings are fast (metadata only). File download streams from GridFS chunks.

---

### Step 14: Loan Processing Service (Staff Operations)

**Module**: `loan-processing-service/`

This demonstrates role-restricted endpoints:

1. `controller/LoanProcessingController.java` — All endpoints use `@PreAuthorize` with staff roles. Records for request bodies (`ApprovalRequest`, `RejectionRequest`).

**Key insight**: This service would use Feign clients to call other services for credit assessment and document verification. The skeleton is ready; the Feign client implementations are the next step for expansion.

---

### Step 15: Customer Portal (React)

**Module**: `ui-loan-portal/`

Read in this order:

1. `vite.config.ts` — Vite proxy config forwards `/api` to the gateway at port 8080.
2. `src/api/client.ts` — Axios instance with JWT interceptor. Automatically attaches tokens, handles 401s with refresh logic.
3. `src/api/index.ts` — API functions grouped by domain: `authApi`, `customerApi`, `offerApi`, etc.
4. `src/contexts/AuthContext.tsx` — React context that manages auth state. `login()`, `register()`, `logout()`. Calls `authApi` and manages localStorage.
5. `src/App.tsx` — Router setup with `ProtectedRoute` and `PublicRoute` wrappers.
6. `src/pages/AppLayout.tsx` — Sidebar navigation, top bar with user info and logout.
7. `src/pages/LoginPage.tsx` — Form with Material UI TextFields, calls `useAuth().login()`.

**Key insight**: The frontend doesn't know about individual services. All API calls go to `/api/v1/...` which the Vite proxy forwards to the gateway at `http://localhost:8080`.

---

### Step 16: Admin Console (React)

**Module**: `ui-admin-console/`

Read in this order:

1. `vite.config.ts` — Vite proxy on port 5174, same gateway proxy pattern.
2. `src/api/client.ts` — Separate token storage (`adminAccessToken` / `adminRefreshToken`) to avoid conflicts with the customer portal.
3. `src/api/index.ts` — Admin-specific API functions: `authApi`, `offerApi`, `customerApi`, `processingApi`, `dashboardApi`.
4. `src/contexts/AuthContext.tsx` — Enhanced auth context with `isAdmin` flag. Validates that the logged-in user has an admin role (`SUPER_ADMIN`, `BANK_ADMIN`, `LOAN_MANAGER`, `UNDERWRITER`). Non-admin users are redirected.
5. `src/App.tsx` — Router with `ProtectedRoute` (checks `isAdmin`) and `PublicRoute`.
6. `src/pages/AdminLayout.tsx` — Dark-themed sidebar (navy) with admin sections: Dashboard, Users, Loan Offers, Applications, Approvals, Settings. Top bar shows user role chips.
7. `src/pages/DashboardPage.tsx` — 6 stat cards (Customers, Pending, Active Offers, Approved, Rejected, Today's Applications) and a recent applications table.
8. `src/pages/UsersPage.tsx` — User management table with create/edit/delete actions.
9. `src/pages/ApprovalsPage.tsx` — Review queue with approve/reject/request-info action buttons.
10. `src/pages/SettingsPage.tsx` — Platform configuration: JWT settings, feature flags, rate limiting toggles.

**Key insight**: The admin console uses separate localStorage keys (`adminAccessToken`, `adminRefreshToken`) so a user can be logged into both the customer portal and admin console simultaneously in different browser tabs without token collision.

---

## Cross-Cutting Architecture Patterns

### Pattern 1: Component Scanning

Every service scans both its own package and `com.loansphere.common`:

```java
@ComponentScan(basePackages = {"com.loansphere.common", "com.loansphere.auth"})
```

This is how common library beans (`JwtTokenProvider`, `GlobalExceptionHandler`, etc.) are discovered in each service.

### Pattern 2: Constructor Injection

All services use constructor injection. No `@Autowired` on fields.

### Pattern 3: Record DTOs

All data transfer objects are Java records. Immutable, concise, and serializable by Jackson automatically.

### Pattern 4: Package-by-Feature

```java
com.loansphere.auth/
├── config/          // SecurityConfig, DataInitializer
├── controller/      // REST endpoints
├── service/         // Business logic
├── repository/      // Data access
├── model/           // Entities
└── dto/             // Request/Response records
```

### Pattern 5: Stateless Services

Services are stateless. Session state is in the JWT (carried by the client) and in Redis (refresh tokens, cache). A service instance can be killed and replaced without losing state.

---

## Quick Reference: Key Files to Understand the Platform

| File | Why |
|---|---|
| `pom.xml` | All versions, modules, dependency management |
| `common-api-library/.../ApiResponse.java` | Universal response format |
| `common-api-library/.../ApiPaths.java` | All API routes |
| `common-security-library/.../JwtAuthenticationFilter.java` | How authentication works |
| `common-security-library/.../JwtTokenProvider.java` | JWT parsing and validation |
| `common-exception-library/.../GlobalExceptionHandler.java` | Centralized error handling |
| `api-gateway/src/main/resources/application.yml` | All route definitions |
| `authentication-service/.../service/AuthService.java` | JWT generation, refresh, blacklist |
| `authentication-service/.../config/SecurityConfig.java` | Security filter chain pattern |
| `ui-loan-portal/src/api/client.ts` | Axios interceptor with token refresh |
| `ui-loan-portal/src/contexts/AuthContext.tsx` | Auth state management |
| `docker-compose.yaml` | Full stack orchestration |

---

## What to Explore Next

After understanding the codebase, here are natural extensions:

1. **Add Feign clients** to `loan-offer-service` to call `customer-profile-service` for eligibility checks
2. **Add event listeners** in `notification-service` for `LoanSubmittedEvent`, `LoanApprovedEvent`, `LoanRejectedEvent`
3. **Add the @Scheduled method** in `notification-service` to send reminder emails for pending applications
4. **Add real credit assessment logic** in `loan-processing-service`
5. **Add an admin dashboard** in the React frontend with charts and tables
6. **Add integration tests** with Testcontainers for end-to-end flows
7. **Implement rate limiting** at the gateway using the Redis-backed `RequestRateLimiter`
8. **Add OAuth2/OpenID Connect** support for social login

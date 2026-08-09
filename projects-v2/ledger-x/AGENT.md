# AGENT.md

## LedgerX

### Banking Ledger & High-Concurrency Transaction Engine

> **Objective**
>
> Build a production-grade **Banking Ledger & High-Concurrency Transaction Engine** using **Spring Boot 4.1**, **Spring Data JPA**, **PostgreSQL**, **Redis**, and **React 19.2**.
>
> This repository is intended to teach advanced Spring Boot concepts through realistic banking microservices while following modern enterprise architecture and production-ready engineering practices.
>
> The AI Agent should always generate clean, modular, testable, secure and maintainable code following current Spring Boot best practices.
>
> **Observability (Prometheus, Grafana, OpenTelemetry, Jaeger, ELK, etc.) must NOT be included.**

---

## Technology Baseline

- **Java 25 (LTS)** for every backend service, with **Spring Boot 4.1** (latest GA) as the baseline, paired with matching current-generation Spring Data JPA, Hibernate, MapStruct, Resilience4j and springdoc-openapi versions compatible with it.
- **Virtual Threads** must be enabled on every service (`spring.threads.virtual.enabled=true`); Tomcat, JDBC/Hibernate calls, Redis operations and `@Async`/scheduled work should run on virtual threads instead of platform thread pools. Avoid `synchronized` blocks; prefer `ReentrantLock` where locking is required.
- **React 19.2** (latest GA) as the baseline for both frontend apps, paired with the current stable TypeScript, Vite, TanStack Query, React Router, Material UI, React Hook Form and Zod versions compatible with it.
- Beyond these pinned baselines, always prefer the **latest stable release** of every other library in the stack at implementation time instead of pinning to older majors.
- Prefer modern patterns (React Compiler-friendly code, function components, hooks, no legacy class components).

---

## No Shared Libraries / No Monorepo Tooling

This repository is a collection of **fully independent Spring Boot projects**, not a monorepo with shared code.

- There is **no parent POM** and **no shared/common library** (no `common-api-library`, `common-security-library`, `common-domain-library`, `common-exception-library`).
- Every service under `backend/` owns **its own** `pom.xml` (or Gradle build), its own DTOs, mappers, exception classes and security config — duplication across services is expected and acceptable.
- Do not introduce cross-module Maven/Gradle dependencies between backend services. Services only communicate over the network (REST), never via shared JARs.
- Do not extract multi-module Maven reactor builds, BOMs, or Gradle composite builds to "deduplicate" services — each service must build and deploy on its own.
- Each React app under `ui/` has its own `package.json`, its own components/hooks/API clients — no shared npm workspace or shared component library between `ui/admin-portal` and `ui/customer-portal`.

---

## Implementation Expectations

When asked to generate a service or feature, the AI Agent must produce **actual working implementation code**, not placeholders or scaffolding:

- Real controllers, services, repositories, entities, DTOs, mappers, security config, and exception handlers with full method bodies — not `// TODO` stubs or empty classes.
- Real React components, hooks and API service files with working logic (state, effects, API calls, form validation) — not empty JSX shells or placeholder components.
- Configuration files (`application.yml`, `.env`, Dockerfiles, Helm values) should contain concrete, usable values consistent with the rest of the stack, not generic placeholders left for the user to fill in.

---

## Learning Objectives

The repository should demonstrate:

### Spring Boot

- Spring Boot Fundamentals
- Spring MVC
- Spring Security
- Spring Validation
- Spring Scheduling
- Spring Events
- Spring Cache
- Async Processing
- Spring Profiles
- Configuration Properties
- Global Exception Handling
- Spring Retry
- OpenAPI / Swagger

---

### Spring Data JPA

- Entity Relationships
- OneToOne
- OneToMany
- ManyToOne
- ManyToMany
- Composite Keys
- Embedded Objects
- Optimistic Locking
- Pessimistic Locking
- Versioning (`@Version`)
- Transactions
- Transaction Propagation
- Isolation Levels
- Native Queries
- JPQL
- Criteria API
- Specifications
- Pageable
- Sorting
- Entity Graph
- Fetch Join
- Batch Fetching
- Lazy vs Eager Loading
- Cascade Types
- Auditing
- Soft Delete
- Custom Repository
- Projection Interfaces
- DTO Projection
- Stored Procedures (optional)
- Flyway Database Migration

---

### PostgreSQL

Use PostgreSQL for

- Accounts
- Customers
- Transactions
- Ledger Entries
- Audit Records
- Branches
- Users
- Roles

---

### Redis

Use Redis for

- Frequently Accessed Accounts
- Customer Cache
- Session Cache
- Authentication Cache
- Dashboard Cache
- OTP Storage
- Transaction Idempotency Keys
- Rate Limiting
- Distributed Locking
- Short-lived Business Cache

---

### Frontend

- React 19.2
- TypeScript
- Vite
- Material UI
- React Router
- Axios
- TanStack Query
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
ledgerx-platform/
├── charts/
├── docker-compose.yaml
├── docs/
├── scripts/
├── ui/
│   ├── admin-portal/
│   └── customer-portal/
├── backend/
│   ├── customer-service/
│   ├── account-service/
│   ├── ledger-service/
│   ├── transaction-service/
│   └── reporting-service/
├── README.md
└── AGENT.md
```

Each directory under `backend/` and `ui/` is a standalone, independently buildable project (its own build file, no shared parent, no shared library module).

---

## High-Level Architecture

```mermaid
flowchart TD
    AdminUI[React Admin Portal] -->|REST APIs| Customer[Customer Service]
    AdminUI -->|REST APIs| Account[Account Service]
    AdminUI -->|REST APIs| Transaction[Transaction Service]
    AdminUI -->|REST APIs| Ledger[Ledger Service]
    AdminUI -->|REST APIs| Reporting[Reporting Service]

    CustomerUI[React Customer Portal] -->|REST APIs| Customer
    CustomerUI -->|REST APIs| Account
    CustomerUI -->|REST APIs| Transaction
    CustomerUI -->|REST APIs| Reporting

    Customer --> DataStores[(PostgreSQL + Redis)]
    Account --> DataStores
    Transaction --> DataStores
    Ledger --> DataStores
    Reporting --> DataStores
```

---

## Root Components

- `charts/`
- `docker-compose.yaml`
- `docs/`
- `scripts/`
- `ui/admin-portal/`
- `ui/customer-portal/`
- `backend/customer-service/`
- `backend/account-service/`
- `backend/transaction-service/`
- `backend/ledger-service/`
- `backend/reporting-service/`

No `common-*` library modules and no parent/aggregator POM — every entry above is a fully independent, self-contained project.

---

## Standard Structure for Every Backend

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
│   │   │   ├── specification/
│   │   │   ├── projection/
│   │   │   ├── service/
│   │   │   ├── mapper/
│   │   │   ├── validation/
│   │   │   ├── advice/
│   │   │   ├── exception/
│   │   │   ├── cache/
│   │   │   ├── security/
│   │   │   ├── event/
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

## Project 1: Customer Service

Repository: `backend/customer-service`

**Responsibilities**

- Customer Registration
- Customer Search
- Customer Update
- Customer Address
- Customer Documents
- KYC Status

**Database**

- `customers`
- `addresses`
- `customer_documents`

**Spring Topics**

- Validation
- Pageable
- Specifications
- DTO Mapping
- Auditing
- Entity Graph

**Redis**

- Customer Cache

---

## Project 2: Account Service

Repository: `backend/account-service`

**Responsibilities**

- Create Account
- Close Account
- Freeze Account
- Unfreeze Account
- Account Summary
- Balance Enquiry

**Database**

- `accounts`
- `account_types`
- `branches`

**Advanced JPA Topics**

- Optimistic Locking
- Pessimistic Locking
- Composite Keys
- Fetch Join
- Projections
- JPQL
- Criteria API

**Redis**

- Account Cache

---

## Project 3: Transaction Service

Repository: `backend/transaction-service`

**Responsibilities**

- Deposit
- Withdraw
- Transfer
- Reverse Transaction
- Scheduled Transfer
- Transaction Validation

**Database**

- `transactions`

**Advanced Spring Topics**

- Transaction Management
- Isolation Levels
- Propagation
- Retry
- Async Processing

**Advanced JPA**

- `@Version`
- Pessimistic Write Lock
- Batch Updates

**Redis**

- Idempotency Keys
- Transaction Cache
- Distributed Lock

---

## Project 4: Ledger Service

Repository: `backend/ledger-service`

**Responsibilities**

- Double Entry Accounting
- Ledger Posting
- Journal Entries
- Balance Verification
- End of Day Closing

**Database**

- `ledger_entries`
- `journals`

**Spring Topics**

- Events
- Scheduling
- Batch Processing

**JPA Topics**

- Batch Inserts
- Cascade
- Entity Relationships

**Redis**

- Ledger Cache

---

## Project 5: Reporting Service

Repository: `backend/reporting-service`

**Responsibilities**

- Customer Reports
- Transaction Reports
- Ledger Reports
- Daily Summary
- Branch Summary

**Advanced JPA**

- Pageable
- Sorting
- Specifications
- Native Queries
- DTO Projection
- Aggregation Queries

**Redis**

- Dashboard Cache

---

## React Application: Admin Portal

Repository: `ui/admin-portal`

### Pages

- Login
- Dashboard
- Customers
- Accounts
- Transactions
- Ledger
- Reports
- Branches
- Users
- Settings

---

### Components

- Navbar
- Sidebar
- Header
- Footer
- DashboardCards
- CustomerTable
- AccountTable
- TransactionTable
- LedgerTable
- ReportsTable
- Charts
- Dialogs
- Pagination
- SearchBar
- Filters
- Forms
- Snackbar
- Loading
- ProtectedRoute

---

### Hooks

- `useAuth()`
- `useCustomers()`
- `useAccounts()`
- `useTransactions()`
- `useLedger()`
- `useReports()`
- `useBranches()`

---

### API Clients

- `authApi.ts`
- `customerApi.ts`
- `accountApi.ts`
- `transactionApi.ts`
- `ledgerApi.ts`
- `reportApi.ts`

---

## React Application: Customer Portal

Repository: `ui/customer-portal`

A self-service banking portal for normal (non-admin) bank customers to manage their own accounts, distinct from the internal admin/branch-staff portal.

### Pages

- Login
- Register
- Forgot Password
- Reset Password
- Home / My Dashboard
- Accounts Overview
- Account Details
- Transaction History
- Transfer Money
- Scheduled Transfers
- Statements & Reports
- Profile
- Security Settings
- Support

---

### Components

- Navbar
- Footer
- DashboardCards
- AccountCard
- AccountSummary
- TransactionTable
- TransferForm
- ScheduledTransferForm
- StatementDownload
- BalanceChart
- ProfileForm
- SecurityPanel
- Dialogs
- Pagination
- Filters
- Snackbar
- Loading
- ProtectedRoute

---

### Hooks

- `useAuth()`
- `useAccounts()`
- `useAccountDetails()`
- `useTransactions()`
- `useTransfer()`
- `useStatements()`
- `useProfile()`

---

### API Clients

- `authApi.ts`
- `accountApi.ts`
- `transactionApi.ts`
- `transferApi.ts`
- `reportApi.ts`
- `profileApi.ts`

---

### Access

- Restricted to the `CUSTOMER` role, scoped to the authenticated customer's own accounts only.
- No access to other customers' data, branch administration, ledger postings or system reports.
- Uses the same Authentication Service (JWT, refresh tokens) as the Admin Portal.

---

## Authentication

Spring Security with

- JWT
- Refresh Token
- BCrypt
- RBAC
- Method Security

---

## Roles

- `SUPER_ADMIN`
- `BANK_ADMIN`
- `BRANCH_MANAGER`
- `TELLER`
- `AUDITOR`
- `CUSTOMER_SUPPORT`
- `READ_ONLY`
- `CUSTOMER`

---

## Default Administrator

Automatically seed:

- **Username:** `admin`
- **Password:** `Admin@123`

---

## Default Customer

Automatically seed a sample bank customer for the Customer Portal:

- **Username:** `customer`
- **Password:** `Customer@123`

---

## Admin Dashboard

Display

- Total Customers
- Active Accounts
- Total Transactions
- Today's Transactions
- Pending Transfers
- Ledger Balance
- Branch Statistics
- Cached Objects
- Daily Deposits
- Daily Withdrawals

---

## Customer Dashboard

Display

- Total Balance Across Accounts
- Linked Accounts
- Recent Transactions
- Upcoming Scheduled Transfers
- Monthly Spending Summary
- Quick Transfer

---

## Docker

Every backend service must include

- Multi-stage Dockerfile
- Eclipse Temurin JRE image
- Non-root user
- Healthcheck
- Environment variables
- Build cache optimisation

---

## Docker Compose

The root compose file should start

- PostgreSQL
- pgAdmin
- Redis
- Redis Insight
- Customer Service
- Account Service
- Transaction Service
- Ledger Service
- Reporting Service
- React Admin Portal
- React Customer Portal

---

## Helm Structure

Every service should include

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
    └── serviceaccount.yaml
```

---

## Kubernetes Best Practices

Every deployment should implement

- Rolling Updates
- Resource Requests
- Resource Limits
- ConfigMaps
- Secrets
- Readiness Probe
- Liveness Probe
- Startup Probe
- Graceful Shutdown
- Labels
- Selectors
- Anti-affinity (optional)
- Horizontal Pod Autoscaler template (disabled by default)

---

## NGINX Ingress

**Routes**

- `/`
- `/admin`
- `api/customers`
- `api/accounts`
- `api/transactions`
- `api/ledger`
- `api/reports`

The React Customer Portal should be available at `/`, and the React Admin Portal should be available at `/admin`.

TLS-ready configuration should be included.

---

## Redis Best Practices

Demonstrate

- Spring Cache Abstraction
- `@Cacheable`
- `@CachePut`
- `@CacheEvict`
- Cache Manager
- RedisTemplate
- TTL
- Cache Namespaces
- Distributed Locks
- Idempotency Keys

---

## Advanced Spring Data JPA Topics to Demonstrate

The AI Agent must intentionally include practical examples of:

- `JpaRepository`
- `PagingAndSortingRepository`
- `JpaSpecificationExecutor`
- Custom Repository Implementations
- `@EntityGraph`
- `@NamedEntityGraph`
- `@Query`
- Native SQL Queries
- JPQL
- Criteria API
- Dynamic Specifications
- Interface Projections
- DTO Projections
- Batch Processing
- Fetch Joins
- Pagination
- Sorting
- Auditing (`@CreatedDate`, `@LastModifiedDate`)
- Optimistic Locking
- Pessimistic Locking
- Transaction Isolation Levels
- Lazy vs Eager Loading
- Soft Deletes
- Flyway Migrations

---

## Unit Testing

Every backend must include unit tests for

- Controllers
- Services
- Repositories
- Specifications
- Security
- Validators
- Mappers
- Cache Services
- Utility Classes

Minimum coverage target: **90%+**

---

## Integration Testing

Use

- Spring Boot Test
- Testcontainers
- PostgreSQL Container
- Redis Container

Test

- Repository Operations
- Pageable Queries
- Specifications
- Cache Behaviour
- Transactions
- Concurrency
- Locking
- Authentication
- REST APIs

---

## Frontend Testing

Use

- Vitest
- React Testing Library

Test

- Components
- Forms
- Hooks
- API Clients
- Protected Routes
- Dashboard Widgets
- Tables
- Pagination
- Filters

---

## Spring Boot Best Practices

The AI Agent must always follow these engineering practices:

### General

- Use Java 25 features where appropriate.
- Follow SOLID, DRY and KISS principles.
- Prefer composition over inheritance.
- Keep methods small and focused.
- Follow package-by-feature architecture.

### Dependency Injection

- Use constructor injection exclusively.
- Avoid field injection.

### REST APIs

- Build versioned REST APIs.
- Return consistent response models.
- Use proper HTTP status codes.
- Generate OpenAPI documentation for all endpoints.

### Persistence

- Never expose JPA entities directly.
- Use DTOs for API contracts.
- Use MapStruct for object mapping.
- Optimise queries to avoid the N+1 problem.
- Use projections when entities are unnecessary.
- Choose lazy loading by default and fetch eagerly only when required.
- Use pagination for list endpoints.
- Index frequently queried database columns.
- Keep transactions short and focused.

### Caching

- Cache only data that benefits from reuse.
- Define appropriate TTL values.
- Evict caches after updates.
- Prevent cache stampedes where appropriate.
- Use Redis for transient, high-throughput data rather than long-term persistence.

### Security

- Encrypt passwords using BCrypt.
- Apply role-based and method-level security.
- Configure CORS correctly.
- Validate all incoming requests.
- Protect sensitive endpoints.

### Error Handling

- Centralise exception handling with `@ControllerAdvice`.
- Return structured error responses.
- Avoid exposing internal implementation details.

### Testing

- Unit test all business logic.
- Integration test all persistence and cache interactions.
- Include concurrency tests for transaction and ledger operations.

---

## Development Workflow

For every feature, the AI Agent should:

1. Design the domain model.
2. Create or update Flyway migrations.
3. Implement entities and repositories.
4. Implement business services.
5. Apply JPA best practices.
6. Add Redis caching where appropriate.
7. Expose REST APIs.
8. Secure endpoints with Spring Security.
9. Update the React Admin Portal and React Customer Portal.
10. Write unit tests.
11. Write integration tests.
12. Build Docker images.
13. Update Docker Compose.
14. Create or update Helm charts.
15. Configure Kubernetes Ingress.
16. Update project documentation.

---

## Expected Outcome

The completed repository should resemble a production-quality banking platform demonstrating:

- Java 25 with virtual threads enabled across all services
- Spring Boot 4.1 backend services and a React 19.2 frontend, with all other libraries kept at their latest compatible stable releases
- Independent, non-monorepo services with no shared libraries or parent POM
- Advanced Spring Boot architecture
- Enterprise Spring Data JPA
- High-concurrency transaction processing
- Double-entry ledger implementation
- PostgreSQL best practices
- Redis caching strategies
- Advanced pagination and specifications
- Secure JWT authentication
- React administration portal
- Docker-based local development
- Kubernetes deployment with Helm
- Production-ready code organisation
- Comprehensive unit and integration testing

The repository should serve as a comprehensive learning reference for intermediate to advanced Spring Boot developers and showcase enterprise-grade coding standards throughout.
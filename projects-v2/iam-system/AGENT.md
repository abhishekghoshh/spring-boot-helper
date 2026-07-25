# AGENT.md

## Enterprise Single Sign-On (SSO) & Multi-Tenant User Management Platform

> **Goal**
>
> Build a production-grade Enterprise Identity and Access Management (IAM) platform using **Spring Boot 3**, **Spring Security 6**, **React**, **Keycloak**, **Google OAuth2**, **GitHub OAuth2**, **JWT**, and **Multi-Tenant Architecture**.
>
> This repository is intended as a learning project that covers most areas of Spring Boot while following modern enterprise architecture and best practices.
>
> The AI Agent should always favour **clean architecture**, **SOLID principles**, **production-ready implementations**, and **modern Spring Boot recommendations**.

---

## Project Goals

The repository should teach the following Spring Boot concepts:

- Spring Boot Fundamentals
- Spring MVC
- Spring Security
- OAuth2 Login
- OpenID Connect
- JWT Authentication
- Role Based Access Control (RBAC)
- Multi-Tenant Architecture
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Spring Validation
- Spring Events
- Spring Cache
- Async Processing
- Scheduling
- Email
- File Upload
- API Documentation
- Docker
- Docker Compose
- Helm
- Kubernetes
- Ingress
- Testing

**Do NOT include**

- Prometheus
- Grafana
- Jaeger
- Zipkin
- OpenTelemetry
- ELK
- Loki
- Monitoring Stack
- Service Mesh

---

## Technology Stack

### Backend

- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Authorization Server (where appropriate)
- Spring OAuth2 Client
- Spring OAuth2 Resource Server
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- MapStruct
- Lombok
- Spring Validation
- Spring Cache
- Spring Scheduling
- Spring Events
- Spring Mail
- Spring Actuator (health only)
- OpenAPI / Swagger

---

### Authentication

The application must support

- Local Login
- Google Login
- GitHub Login
- Local Keycloak Login
- JWT Authentication
- Refresh Tokens
- Logout
- Password Reset
- Forgot Password
- Email Verification

---

### Frontend

- React
- Vite
- TypeScript
- React Router
- Material UI
- TanStack Query
- React Hook Form
- Axios
- Zod

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
enterprise-sso-platform/
├── charts/
├── docker-compose.yaml
├── docs/
├── scripts/
├── ui-admin-console/
├── authentication-service/
├── tenant-management-service/
├── user-management-service/
├── notification-service/
├── file-storage-service/
├── common-security-library/
├── common-auth-library/
├── common-api-library/
├── common-exception-library/
├── README.md
└── AGENT.md
```

---

## Overall Architecture

```mermaid
flowchart TD
    UI[React Admin Console] -->|REST APIs| Auth[Authentication Service]
    UI -->|REST APIs| Tenant[Tenant Service]
    UI -->|REST APIs| User[User Service]
    UI -->|REST APIs| Notification[Notification Service]

    Auth --> DB[(PostgreSQL)]
    Tenant --> DB

    Auth --> Keycloak[Local Keycloak]
    Keycloak --> Google[Google OAuth]
    Keycloak --> GitHub[GitHub OAuth]
```

---

## Root Components

- `charts/`
- `docker-compose.yaml`
- `docs/`
- `scripts/`
- `ui-admin-console/`
- `authentication-service/`
- `tenant-management-service/`
- `user-management-service/`
- `notification-service/`
- `file-storage-service/`
- `common-security-library/`
- `common-auth-library/`
- `common-api-library/`
- `common-exception-library/`

---

## Common Service Structure

Every Spring Boot service should follow

```
src/
├── main/
│   ├── java/
│   │   ├── config/
│   │   ├── configuration/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── mapper/
│   │   ├── security/
│   │   ├── tenant/
│   │   ├── validation/
│   │   ├── advice/
│   │   ├── exception/
│   │   ├── util/
│   │   ├── constant/
│   │   ├── event/
│   │   ├── listener/
│   │   ├── scheduler/
│   │   └── filter/
│   │
│   └── resources/
│
└── test/

Dockerfile
README.md
helm/
```

---

## Project 1: Authentication Service

Repository: `authentication-service`

**Responsibilities**

- Username Password Login
- Google OAuth Login
- GitHub OAuth Login
- Local Keycloak Login
- JWT Token Generation
- Refresh Token
- Logout
- Session Management
- Email Verification
- Password Reset
- Forgot Password

**Spring Topics**

- Spring Security
- OAuth2 Client
- OpenID Connect
- JWT
- Cookie Security
- Authentication Providers
- Filters
- Security Configuration

---

## Project 2: Tenant Management Service

Repository: `tenant-management-service`

**Responsibilities**

- Tenant Registration
- Tenant Provisioning
- Tenant Activation
- Tenant Suspension
- Tenant Configuration
- Tenant Branding
- Tenant Domain Mapping

**Spring Topics**

- Multi-Tenant Architecture
- Hibernate Multi-Tenant
- Filters
- Interceptors
- Request Context
- Database Routing
- Tenant Resolver

---

## Project 3: User Management Service

Repository: `user-management-service`

**Responsibilities**

- User CRUD
- Invite User
- Disable User
- Assign Roles
- Assign Permissions
- User Profile
- User Preferences

**Spring Topics**

- JPA
- Validation
- Transactions
- Pagination
- Specifications
- Entity Graphs

---

## Project 4: Notification Service

**Responsibilities**

- Welcome Email
- Password Reset Email
- Verification Email
- Invitation Email

**Spring Topics**

- Spring Mail
- Async
- Scheduling

---

## Project 5: File Storage Service

**Responsibilities**

- User Avatar Upload
- Tenant Logo Upload
- Document Upload

**Spring Topics**

- Multipart Upload
- Validation
- File Storage
- Static Resource Serving

---

## Multi-Tenant Design

The platform must support

- Multiple Organisations
- Tenant Isolation
- Tenant Admin
- Tenant Users
- Tenant Configuration
- Tenant Branding

Tenant information should be resolved using a header (`X-Tenant-ID`) or a subdomain (`tenant.example.com`).

The implementation should be modular enough to allow

- Database per Tenant
- Schema per Tenant

without changing business logic.

---

## Roles

- `SUPER_ADMIN`
- `TENANT_ADMIN`
- `MANAGER`
- `USER`
- `VIEWER`

Permissions should be database driven.

---

## Default Admin User

Automatically seed:

- **Username:** `admin`
- **Password:** `Admin@123`

Default tenant: `master`

---

## React Project

Repository: `ui-admin-console`

**Pages**

- Login
- Dashboard
- Tenants
- Users
- Roles
- Permissions
- Profile
- Settings
- Audit
- File Upload
- Administration

---

## React Components

- Navbar
- Sidebar
- Header
- Footer
- ProtectedRoute
- UserTable
- TenantTable
- RoleTable
- PermissionTable
- LoginForm
- RegistrationForm
- ForgotPassword
- ResetPassword
- AvatarUpload
- Dialogs
- Cards
- Charts
- Loading
- Snackbar

---

## React Hooks

- `useAuth`
- `useUser`
- `useTenant`
- `useRole`
- `usePermission`
- `useProfile`
- `useFileUpload`

---

## React Services

- `authApi.ts`
- `tenantApi.ts`
- `userApi.ts`
- `roleApi.ts`
- `permissionApi.ts`
- `profileApi.ts`
- `uploadApi.ts`

---

## Admin Dashboard

The dashboard should display

- Total Tenants
- Total Users
- Active Users
- Inactive Users
- OAuth Login Statistics
- Local Login Statistics
- Tenant Overview
- User Activity
- Recent Registrations
- Role Distribution

---

## Security Best Practices

Always implement

- Stateless Authentication
- JWT
- Refresh Tokens
- Password Hashing (BCrypt)
- CSRF Protection where applicable
- CORS Configuration
- Role Based Access Control
- Method Level Security
- Secure Cookies
- OAuth2 Best Practices
- OpenID Connect Best Practices
- Password Policy
- Account Locking
- Brute Force Protection
- Session Revocation

---

## Docker

Each service should contain

- Multi-stage Dockerfile
- Non-root User
- Small Runtime Image
- Health Check
- Environment Variables
- Build Cache Optimisation

---

## Docker Compose

The root compose file should start

- PostgreSQL
- Keycloak
- Authentication Service
- Tenant Service
- User Service
- Notification Service
- File Storage Service
- React UI

---

## Helm Structure

Each service must contain

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

Every deployment must include

- Resource Requests
- Resource Limits
- Liveness Probe
- Readiness Probe
- Rolling Updates
- ConfigMaps
- Secrets
- Labels
- Selectors
- Environment Variables
- Graceful Shutdown

---

## Ingress

Create one NGINX Ingress.

**Routes**

- `/`
- `api/auth`
- `api/users`
- `api/tenants`
- `api/files`
- `api/notifications`

TLS-ready configuration should be supported.

---

## Testing

Every backend service must include

### Unit Tests

- Controller Tests
- Service Tests
- Repository Tests
- Security Tests
- Validation Tests
- Mapper Tests
- Utility Tests

Target: **90%+ coverage**

---

### Integration Tests

Use

- Spring Boot Test
- Testcontainers
- PostgreSQL
- Mock OAuth Providers where appropriate

Test

- Login
- Registration
- OAuth
- JWT
- Tenant Resolution
- CRUD APIs
- Security Rules
- Database Access

---

## Frontend Tests

Use

- Vitest
- React Testing Library

Test

- Components
- Hooks
- API Clients
- Authentication Flow
- Protected Routes
- Forms

---

## Spring Boot Best Practices

The agent must always follow modern enterprise Spring Boot practices:

- Use constructor injection exclusively.
- Follow package-by-feature where practical.
- Keep controllers thin and delegate business logic to services.
- Never expose JPA entities directly; use DTOs.
- Use MapStruct for mapping.
- Apply Bean Validation to all incoming requests.
- Centralise exception handling with `@ControllerAdvice`.
- Externalise configuration using profiles and environment variables.
- Use immutable DTOs (`record`) where appropriate.
- Design reusable security components.
- Keep authentication and authorisation separate.
- Make all APIs versionable.
- Use Flyway for all schema changes.
- Document APIs with OpenAPI.
- Prefer composition over inheritance.
- Follow SOLID, DRY and KISS principles.
- Write meaningful unit and integration tests for every feature.
- Ensure code is modular, readable and maintainable.

---

## Development Workflow

For every feature, the AI Agent should:

1. Design the domain model.
2. Create or update Flyway migrations.
3. Implement entities and repositories.
4. Develop business services.
5. Expose REST APIs.
6. Secure endpoints with Spring Security.
7. Update the React UI.
8. Add unit tests.
9. Add integration tests.
10. Build Docker images.
11. Update Docker Compose.
12. Create or update Helm charts.
13. Configure Kubernetes Ingress.
14. Update documentation.

---

## Deliverable

The final repository should resemble a real-world enterprise Identity & Access Management platform that demonstrates:

- Enterprise authentication and authorisation
- Google OAuth2 integration
- GitHub OAuth2 integration
- Local Keycloak integration
- Multi-tenant architecture
- React administration portal
- Dockerised development
- Kubernetes deployment with Helm
- Clean, production-ready Spring Boot architecture
- Comprehensive testing
- Secure coding practices

The implementation should be educational, modular and extensible, enabling additional identity providers (such as Microsoft Entra ID, Okta or Auth0) to be integrated with minimal changes in the future.
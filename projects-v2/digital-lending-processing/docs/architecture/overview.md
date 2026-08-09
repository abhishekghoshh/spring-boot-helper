# Architecture Overview

## System Architecture

LoanSphere follows a microservices architecture with a single entry point through the API Gateway. All services register with Eureka for dynamic service discovery.

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         External Clients                         │
│                     (Browser, Mobile, API)                       │
└─────────────────────────────┬───────────────────────────────────┘
                              │ HTTPS
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Spring Cloud Gateway (:8080)                    │
│  ┌──────────┐  ┌───────────┐  ┌────────────┐  ┌─────────────┐  │
│  │ JWT Auth │  │ CORS      │  │ Rate Limit │  │ Path Rewrite│  │
│  │ Filter   │  │ Filter    │  │ Filter     │  │ Filter      │  │
│  └──────────┘  └───────────┘  └────────────┘  └─────────────┘  │
└──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬─────────┘
       │      │      │      │      │      │      │      │
       ▼      ▼      ▼      ▼      ▼      ▼      ▼      ▼
┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐
│ Auth ││ Cust ││Offer ││ EMI  ││ App  ││ Proc ││Notif ││ Doc  │
│ :8081││ :8082││ :8083││ :8084││ :8085││ :8086││ :8087││ :8088│
└──┬───┘└──┬───┘└──┬───┘└──────┘└──┬───┘└──┬───┘└──┬───┘└──┬───┘
   │       │       │               │       │       │       │
   ▼       ▼       ▼               ▼       ▼       ▼       ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Data Layer                                │
│  ┌──────────────┐  ┌──────────────┐                              │
│  │ MongoDB 8    │  │  Redis 7     │                              │
│  │ ┌──────────┐ │  │ ┌──────────┐ │                              │
│  │ │auth_db   │ │  │ │Sessions  │ │                              │
│  │ │cust_db   │ │  │ │Cache     │ │                              │
│  │ │offer_db  │ │  │ │OTP       │ │                              │
│  │ │app_db    │ │  │ │Blacklist │ │                              │
│  │ │proc_db   │ │  │ └──────────┘ │                              │
│  │ │notif_db  │ │  └──────────────┘                              │
│  │ │doc_db    │ │                                                │
│  │ └──────────┘ │                                                │
│  └──────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

## Component Interactions

### Request Flow

1. **Client** sends HTTP request to the API Gateway
2. **Gateway** validates JWT token (extracts claims, checks expiration)
3. **Gateway** resolves service location via Eureka
4. **Gateway** forwards request to the target service with client-side load balancing
5. **Service** re-validates JWT via `JwtAuthenticationFilter`
6. **Service** processes the request and returns response
7. **Gateway** returns response to the client

### Service-to-Service Communication

All inter-service calls use **OpenFeign** declarative REST clients:

```
Loan Processing Service
    ├── Feign → Customer Profile Service (check customer data)
    ├── Feign → Loan Offer Service (validate offer terms)
    └── Feign → Document Service (verify submitted documents)
```

Each Feign client is protected with **Resilience4j**:
- **Circuit Breaker**: Opens after 50% failure rate in a 10-request window
- **Retry**: Up to 3 attempts with 500ms wait
- **Fallback**: Graceful degradation when downstream is unavailable

### JWT Authentication Flow

```
    Client                    Gateway                 Auth Service
      │                         │                         │
      │  POST /api/v1/auth/login│                        │
      │────────────────────────►│                         │
      │                         │  Route to auth-service  │
      │                         │────────────────────────►│
      │                         │                         │
      │                         │  {access, refresh} JWT  │
      │                         │◄────────────────────────│
      │   200 + tokens          │                         │
      │◄────────────────────────│                         │
      │                         │                         │
      │  GET /api/v1/customers/profile                   │
      │  Authorization: Bearer <access_token>            │
      │────────────────────────►│                         │
      │                         │  Validate JWT           │
      │                         │  Extract userId, roles  │
      │                         │                         │
      │                         │  Route to customer-svc  │
      │   200 + profile data    │                         │
      │◄────────────────────────│                         │
```

### Event-Driven Flow

The Loan Application service publishes Spring Application Events:

```
LoanApplicationService
    │
    ├── LoanSubmittedEvent ──► NotificationService (send confirmation)
    │                      ──► LoanProcessingService (add to review queue)
    │
    ├── LoanApprovedEvent ──► NotificationService (send approval notice)
    │
    └── LoanRejectedEvent ──► NotificationService (send rejection notice)
```

## Technology Mapping

| Concern | Technology | Configuration |
|---|---|---|
| Service Discovery | Netflix Eureka | Standalone mode, service `defaultZone` |
| API Gateway | Spring Cloud Gateway | Route predicates, global filters |
| Central Config | Spring Cloud Config | Native profile, classpath config |
| Inter-Service Calls | OpenFeign | Declarative clients, error decoders |
| Resilience | Resilience4j | Circuit breaker, retry, bulkhead |
| Load Balancing | Spring Cloud LoadBalancer | Client-side, round-robin |
| Authentication | Spring Security + JWT | Nimbus JOSE, BCrypt, stateless sessions |
| Data Persistence | MongoDB | Per-service database isolation |
| Caching | Redis | Cache annotations, TTL-based eviction |
| Async Processing | @Async + Virtual Threads | `spring.threads.virtual.enabled=true` |
| File Storage | MongoDB GridFS | Metadata in collections, files in chunks |
| Scheduling | @Scheduled | Fixed-rate and cron expressions |
| Email (Dev) | Mailpit | Local SMTP testing on port 1025 |

## Scalability Patterns

1. **Horizontal Scaling** — Each service can run multiple instances behind the load balancer
2. **Database Isolation** — No shared databases; each service scales independently
3. **Cache-First** — Redis caching for hot data (offers, user sessions)
4. **Circuit Breaking** — Prevents cascading failures across services
5. **Async Processing** — Virtual threads for non-blocking I/O operations
6. **Stateless Design** — JWT tokens carry session state; services are stateless

# Service Catalog

Complete specifications for each microservice.

---

## 1. Config Server

| Property | Value |
|---|---|
| Artifact | `config-server` |
| Port | `8888` |
| Main Class | `com.loansphere.config.ConfigServerApplication` |
| Spring Cloud | `@EnableConfigServer` |
| Profile | `native` |
| Config Location | `classpath:/config` |

**Purpose**: Centralized configuration management. All services fetch configuration on startup.

---

## 2. Discovery Server

| Property | Value |
|---|---|
| Artifact | `discovery-server` |
| Port | `8761` |
| Main Class | `com.loansphere.discovery.DiscoveryServerApplication` |
| Spring Cloud | `@EnableEurekaServer` |
| Mode | Standalone (no peer replication) |

**Purpose**: Service registry. All services register themselves and discover peers via Eureka.

---

## 3. API Gateway

| Property | Value |
|---|---|
| Artifact | `api-gateway` |
| Port | `8080` |
| Main Class | `com.loansphere.gateway.ApiGatewayApplication` |
| Server | WebFlux (Netty) |
| Dependencies | Gateway Server, Eureka Client, LoadBalancer, Redis Reactive |

**Routes**:

| Route ID | Path | Target Service |
|---|---|---|
| `authentication-service` | `/api/v1/auth/**` | `lb://authentication-service` |
| `customer-profile-service` | `/api/v1/customers/**` | `lb://customer-profile-service` |
| `loan-offer-service` | `/api/v1/offers/**` | `lb://loan-offer-service` |
| `emi-calculation-service` | `/api/v1/emi/**` | `lb://emi-calculation-service` |
| `loan-application-service` | `/api/v1/applications/**` | `lb://loan-application-service` |
| `loan-processing-service` | `/api/v1/processing/**` | `lb://loan-processing-service` |
| `notification-service` | `/api/v1/notifications/**` | `lb://notification-service` |
| `document-service` | `/api/v1/documents/**` | `lb://document-service` |

**Global Filters**: CORS, request logging, JWT validation, rate limiting.

---

## 4. Authentication Service

| Property | Value |
|---|---|
| Artifact | `authentication-service` |
| Port | `8081` |
| Main Class | `com.loansphere.auth.AuthenticationServiceApplication` |
| MongoDB | `auth_db` |
| Redis Usage | Sessions, JWT blacklist, OTP, password reset tokens |

**Collections**:

| Collection | Purpose |
|---|---|
| `users` | User accounts, credentials, roles |

**Endpoints**:

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register new user |
| `POST` | `/api/v1/auth/login` | Public | Login, returns JWT pair |
| `POST` | `/api/v1/auth/refresh-token` | Public | Refresh access token |
| `POST` | `/api/v1/auth/logout` | Authenticated | Blacklist current token |
| `POST` | `/api/v1/auth/forgot-password` | Public | Request password reset |
| `POST` | `/api/v1/auth/reset-password` | Public | Reset password with token |
| `GET` | `/api/v1/auth/me` | Authenticated | Get current user profile |

**Security Features**:
- BCrypt password encoding (strength 10)
- JWT access tokens (RS256, configurable expiry)
- Refresh tokens stored in Redis with TTL
- Token blacklisting on logout
- Rate-limited login attempts

---

## 5. Customer Profile Service

| Property | Value |
|---|---|
| Artifact | `customer-profile-service` |
| Port | `8082` |
| Main Class | `com.loansphere.customer.CustomerProfileApplication` |
| MongoDB | `customer_db` |

**Collections**:

| Collection | Purpose |
|---|---|
| `customer_profiles` | Complete customer biodata |

**Endpoints**:

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/customers/profile` | `ROLE_CUSTOMER` | Create profile |
| `GET` | `/api/v1/customers/profile` | Authenticated | Get own profile |
| `PUT` | `/api/v1/customers/profile` | `ROLE_CUSTOMER` | Update profile |

**Profile Fields**: userId, firstName, lastName, email, phone, dateOfBirth, gender, panNumber, aadhaarNumber, annualIncome, employmentType, employerName, creditScore.

---

## 6. Loan Offer Service

| Property | Value |
|---|---|
| Artifact | `loan-offer-service` |
| Port | `8083` |
| Main Class | `com.loansphere.offer.LoanOfferApplication` |
| MongoDB | `offer_db` |
| Redis | Cache for active offers |

**Collections**:

| Collection | Purpose |
|---|---|
| `loan_offers` | Loan product definitions |

**Endpoints**:

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/offers` | Public | List active offers (cached) |
| `GET` | `/api/v1/offers/{id}` | Public | Get offer details |
| `POST` | `/api/v1/offers` | Admin/Manager | Create new offer |
| `PUT` | `/api/v1/offers/{id}` | Admin/Manager | Update offer |
| `GET` | `/api/v1/offers/{id}/emi` | Authenticated | Calculate EMI for offer |

**Resilience4j**:
- Circuit breaker on `customerProfileService` (Feign calls)
- Retry with exponential backoff
- Cache eviction on offer create/update

---

## 7. EMI Calculation Service

| Property | Value |
|---|---|
| Artifact | `emi-calculation-service` |
| Port | `8084` |
| Main Class | `com.loansphere.emi.EmiCalculationApplication` |
| State | Stateless (no persistence) |

**Endpoints**:

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/emi/calculate` | Authenticated | Simple EMI calculation |
| `GET` | `/api/v1/emi/amortization-schedule` | Authenticated | Full amortization table |
| `GET` | `/api/v1/emi/prepayment` | Authenticated | Prepayment impact analysis |

**Formulas**:
- **EMI**: `P × r × (1+r)^n / ((1+r)^n - 1)`
- **Interest Component**: `Remaining Principal × r`
- **Principal Component**: `EMI - Interest`

---

## 8. Loan Application Service

| Property | Value |
|---|---|
| Artifact | `loan-application-service` |
| Port | `8085` |
| Main Class | `com.loansphere.application.LoanApplicationService` |
| MongoDB | `application_db` |

**Collections**:

| Collection | Purpose |
|---|---|
| `loan_applications` | Submitted loan applications |

**Endpoints**:

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/applications` | `ROLE_CUSTOMER` | Submit application |
| `GET` | `/api/v1/applications` | Authenticated | List my applications |
| `GET` | `/api/v1/applications/{id}` | Authenticated | Get application details |

**Status Flow**: `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED`

**Events Published**: `LoanSubmittedEvent`, `LoanApprovedEvent`, `LoanRejectedEvent`

---

## 9. Loan Processing Service

| Property | Value |
|---|---|
| Artifact | `loan-processing-service` |
| Port | `8086` |
| Main Class | `com.loansphere.processing.LoanProcessingApplication` |
| MongoDB | `processing_db` |

**Endpoints**:

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/processing/reviews` | Staff | Get review queue |
| `POST` | `/api/v1/processing/reviews/{id}/approve` | Staff | Approve application |
| `POST` | `/api/v1/processing/reviews/{id}/reject` | Staff | Reject application |
| `POST` | `/api/v1/processing/reviews/{id}/request-info` | Staff | Request more info |

**Feign Clients**: Customer Profile, Loan Offer, Document services.

---

## 10. Notification Service

| Property | Value |
|---|---|
| Artifact | `notification-service` |
| Port | `8087` |
| Main Class | `com.loansphere.notification.NotificationApplication` |
| MongoDB | `notification_db` |

**Collections**:

| Collection | Purpose |
|---|---|
| `notifications` | Notification records |

**Endpoints**:

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | Authenticated | Get my notifications |
| `PUT` | `/api/v1/notifications/{id}/read` | Authenticated | Mark as read |

**Async Processing**: `@Async` + virtual threads for email sending.

---

## 11. Document Service

| Property | Value |
|---|---|
| Artifact | `document-service` |
| Port | `8088` |
| Main Class | `com.loansphere.document.DocumentApplication` |
| MongoDB | `document_db` |
| File Storage | MongoDB GridFS |

**Collections**:

| Collection | Purpose |
|---|---|
| `documents` | File metadata (GridFS reference) |
| `fs.files` | GridFS file metadata (auto) |
| `fs.chunks` | GridFS file chunks (auto) |

**Endpoints**:

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/documents/upload` | Authenticated | Upload file (max 10 MB) |
| `GET` | `/api/v1/documents` | Authenticated | List my documents |
| `GET` | `/api/v1/documents/{id}` | Authenticated | Get metadata |
| `GET` | `/api/v1/documents/{id}/download` | Authenticated | Download file |
| `DELETE` | `/api/v1/documents/{id}` | Authenticated | Delete document |

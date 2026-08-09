# Security Guide

## Authentication Architecture

LoanSphere uses a **stateless JWT-based authentication** model with Spring Security.

```
┌─────────┐     ┌──────────────┐     ┌──────────────┐
│  Client  │────►│  API Gateway │────►│  Services     │
│  (React) │     │  (Validate)  │     │  (Re-validate)│
└─────────┘     └──────────────┘     └──────────────┘
     │                                        │
     │  1. POST /auth/login                   │
     │────────────────────────────────────────►
     │                                         │
     │  2. { accessToken, refreshToken }       │
     │◄────────────────────────────────────────
     │                                         │
     │  3. GET /customers/profile              │
     │     Authorization: Bearer <accessToken> │
     │────────────────────────────────────────►
     │                                         │
     │  4. 200 + profile data                  │
     │◄────────────────────────────────────────
```

## JWT Token Details

### Access Token

- **Algorithm**: HMAC-SHA256 (HS256)
- **Signing Key**: Configurable via `JWT_SECRET` (minimum 256 bits)
- **Expiry**: 1 hour (configurable via `JWT_EXPIRATION_MS`)
- **Claims**:
  - `sub`: User ID
  - `username`: Username
  - `roles`: Array of role strings
  - `iat`: Issued at timestamp
  - `exp`: Expiration timestamp
  - `jti`: Unique token ID

### Refresh Token

- **Format**: UUID v4
- **Storage**: Redis key `refresh:<uuid>` → User ID
- **Expiry**: 24 hours (configurable via `JWT_REFRESH_EXPIRATION_MS`)
- **Rotation**: New refresh token issued on each use; old token invalidated

### Token Flow

1. Client authenticates → receives access + refresh tokens
2. Access token used for API calls in `Authorization: Bearer <token>` header
3. When access token expires, client sends refresh token
4. Server validates refresh token (Redis lookup), issues new pair
5. On logout, access token added to Redis blacklist with TTL

### Token Blacklisting

```java
// On logout
redisTemplate.opsForValue().set(
    "blacklist:" + accessToken, 
    "revoked", 
    Duration.ofMillis(remainingExpiryMs)
);

// On each request
if (redisTemplate.hasKey("blacklist:" + token)) {
    throw new UnauthorizedException("Token has been revoked");
}
```

## Role-Based Access Control (RBAC)

### Roles

| Role | Access Level |
|---|---|
| `SUPER_ADMIN` | Full system access |
| `BANK_ADMIN` | Offer management, reporting |
| `LOAN_MANAGER` | Offer creation, review queue |
| `UNDERWRITER` | Application review, approval |
| `CUSTOMER` | Self-service operations |
| `SUPPORT` | Read-only access |

### Method-Level Security

```java
// Controller-level
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN', 'LOAN_MANAGER')")
public ResponseEntity<?> createOffer(@RequestBody LoanOffer offer) { ... }

// Service-level
@PreAuthorize("hasRole('SUPER_ADMIN')")
public void deleteUser(String userId) { ... }
```

### Security Filter Chain

Each service configures a security filter chain:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())                          // Stateless API
        .cors(cors -> cors.configurationSource(corsConfig()))   // Allow frontend
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/login", 
                             "/api/v1/auth/register").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthFilter, 
                         UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

## Password Security

- **Algorithm**: BCrypt with strength factor 10
- **Storage**: Hashed passwords only — never stored in plaintext
- **Validation**: `passwordEncoder.matches(rawPassword, hashedPassword)`
- **Reset flow**: Time-limited reset token (15 min) stored in Redis

## Cross-Origin Resource Sharing (CORS)

Configured at the API Gateway for centralized management:

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          "[/**]":
            allowedOrigins: "http://localhost:5173"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

## Service-to-Service Authentication

When services communicate via OpenFeign, the JWT token is propagated:

```java
@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
            if (auth != null && auth.getCredentials() instanceof String token) {
                requestTemplate.header("Authorization", "Bearer " + token);
            }
        };
    }
}
```

## Security Best Practices

1. **HTTPS Everywhere** — Terminate TLS at the load balancer / ingress
2. **Secret Rotation** — Rotate `JWT_SECRET` periodically
3. **Token Expiry** — Keep access token TTL short (≤ 1 hour)
4. **Rate Limiting** — Implement at gateway for auth endpoints
5. **Input Validation** — All DTOs validated with Jakarta Bean Validation
6. **No Sensitive Data in Logs** — Sanitize before logging
7. **Principle of Least Privilege** — Users get minimum required roles
8. **Audit Logging** — Track sensitive operations
9. **Dependency Scanning** — Regular updates for security patches
10. **Container Security** — Non-root users, read-only filesystems where possible

## User Management

### Default Administrator

The system creates a default admin user on first startup via `DataInitializer`:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `Admin@123` |
| Email | `admin@loansphere.com` |
| Role | `SUPER_ADMIN` |

This user is created automatically when no user with username `admin` exists in the database. To change the default credentials, update `backend/authentication-service/src/main/java/com/loansphere/auth/config/DataInitializer.java`.

### Creating a Normal User (Customer)

**Via REST API:**

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "John@123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Via Gateway (once gateway is configured):**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"John@123","firstName":"John","lastName":"Doe"}'
```

Newly registered users default to the `CUSTOMER` role.

### Creating Admin/Staff Users

Admin users must be created with an explicit role. Use the API Gateway to register, then update the user's role via direct MongoDB access or an admin endpoint.

**Option 1 — Via MongoDB directly:**

```bash
docker exec loansphere-mongodb mongosh -u root -p rootroot --authenticationDatabase admin <<'EOF'
use auth_db
db.users.updateOne(
  { username: "operator" },
  { $set: { roles: ["LOAN_MANAGER"] } }
)
EOF
```

**Option 2 — Create with role via registration if extended:**

If you add a `role` field to the registration payload and modify `AuthService.register()` to accept it, you can create staff users directly:

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "manager",
    "email": "manager@loansphere.com",
    "password": "Manager@123",
    "firstName": "Loan",
    "lastName": "Manager",
    "roles": ["LOAN_MANAGER"]
  }'
```

### Verifying a User's Role

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

The response JWT contains a `roles` claim. Decode it:

```bash
# Extract token from login response, then:
echo "<token>" | cut -d. -f2 | base64 -d | python3 -m json.tool
```

### Available Roles

| Role | Description |
|---|---|
| `SUPER_ADMIN` | Full system access — manage users, all loans, all settings |
| `BANK_ADMIN` | Manage loan offers, reporting, dashboard |
| `LOAN_MANAGER` | Create offers, review applications |
| `UNDERWRITER` | Review and approve/reject loan applications |
| `CUSTOMER` | Self-service — apply for loans, view own profile |
| `SUPPORT` | Read-only access to customer data |

### Password Requirements

- Minimum 6 characters (enforced by `@Size` annotation on DTO)
- Passwords are BCrypt-hashed before storage
- Default admin password should be changed in production

### Password Reset

```bash
# Request reset token
curl -X POST http://localhost:8081/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@loansphere.com"}'

# Reset password with token (sent via email in production)
curl -X POST http://localhost:8081/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token":"<reset-token-from-email>","newPassword":"NewPass@456"}'
```

## Rate Limiting

The API Gateway can enforce rate limits using Redis:

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: authentication-service
        filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20
```

## Security Headers

Recommended for production:

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
```

Add via Spring Security or a Gateway filter:

```java
http.headers(headers -> headers
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))
    .contentTypeOptions(Customizer.withDefaults())
    .frameOptions(frame -> frame.deny()));
```

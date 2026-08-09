# Security Architecture

## Overview

CommerceMesh implements defense-in-depth security with **Spring Security 6**, **JWT-based stateless authentication**, **RBAC with fine-grained permissions**, and **secure service-to-service communication**. Every layer — from the API Gateway to individual microservices — enforces security policies.

---

## Authentication Flow

### JWT Token Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                    Access Token (JWT)                         │
│  Header: { "alg": "HS256", "typ": "JWT" }                   │
│  Payload: {                                                  │
│    "sub": "user-uuid",                                       │
│    "username": "admin",                                      │
│    "roles": ["ADMIN"],                                       │
│    "permissions": ["product:read", "product:write", ...],     │
│    "iat": 1753459200,                                        │
│    "exp": 1753460100,     ← 15 minutes                       │
│    "jti": "unique-token-id"                                  │
│  }                                                           │
│  Signature: HMAC-SHA256(header.payload, secret)               │
└───────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────┐
│                   Refresh Token                               │
│  - 256-bit random string                                     │
│  - Hashed (SHA-256) before storage                           │
│  - Stored in PostgreSQL (auth.refresh_tokens)                │
│  - 7-day expiry                                              │
│  - Rotated on each use (old token invalidated)               │
│  - Revocable (set revoked = true)                            │
└───────────────────────────────────────────────────────────────┘
```

### Login Sequence

```
1. Client → POST /api/v1/auth/login { username, password }
2. API Gateway → Routes to authentication-service
3. Auth Service:
   a. Load user by username via UserDetailsService
   b. BCrypt.verify(password, storedHash)
   c. Load user's roles and permissions
   d. Generate access token (JWT, 15min)
   e. Generate refresh token (random string)
   f. Hash and store refresh token in DB
   g. Return { accessToken, refreshToken } to client
4. Client stores tokens in localStorage/httpOnly cookie
5. Client includes access token in Authorization header for API calls
```

### Token Refresh Sequence

```
1. Client detects 401 response (expired access token)
2. Client → POST /api/v1/auth/refresh { refreshToken }
3. Auth Service:
   a. Hash the provided refresh token
   b. Look up hash in DB, verify not revoked, not expired
   c. Mark old token as revoked
   d. Generate new access token + new refresh token
   e. Return to client
4. If refresh fails (expired/revoked) → client redirects to login
```

### Logout Sequence

```
1. Client → POST /api/v1/auth/logout (with access token)
2. Auth Service:
   a. Extract JWT ID (jti) from access token
   b. Add jti to Redis blacklist with TTL = remaining token lifetime
   c. Mark all user's refresh tokens as revoked in DB
3. Client clears local storage
```

### Token Validation (Per-Request)

```
API Gateway interceptor:
1. Extract Bearer token from Authorization header
2. Parse and verify JWT signature against shared secret
3. Check token expiry
4. Check if jti is in Redis blacklist
5. Extract user ID, roles, permissions
6. Add headers: X-User-Id, X-User-Roles, X-User-Permissions
7. Forward to downstream service

Downstream service:
1. Extract X-User-Id, X-User-Roles headers
2. Set SecurityContext with roles/permissions
3. @PreAuthorize annotations check against SecurityContext
```

---

## Role-Based Access Control (RBAC)

### Role Hierarchy

```
                    SUPER_ADMIN
                         │
                    ┌────┴────┐
                    │         │
                  ADMIN   PRODUCT_MANAGER
                    │
            ┌───────┼───────┐
            │       │       │
    ORDER_MANAGER  CUSTOMER_SUPPORT
            │
        CUSTOMER
```

- `SUPER_ADMIN` inherits all permissions — can manage roles, users, and system configuration
- `ADMIN` inherits `PRODUCT_MANAGER`, `ORDER_MANAGER`, `CUSTOMER_SUPPORT` — full platform administration
- `PRODUCT_MANAGER` — manage products, categories, brands, inventory
- `ORDER_MANAGER` — view/manage orders, process refunds
- `CUSTOMER_SUPPORT` — view users, view orders, view payments (read-only)
- `CUSTOMER` — basic authenticated user — shop, manage own profile/orders

### Permission Matrix

| Permission | SUPER_ADMIN | ADMIN | PRODUCT_MANAGER | ORDER_MANAGER | CUSTOMER_SUPPORT | CUSTOMER |
|-----------|:-----------:|:-----:|:---------------:|:-------------:|:----------------:|:--------:|
| `user:read` | ✓ | ✓ | | | ✓ | |
| `user:write` | ✓ | ✓ | | | | |
| `user:delete` | ✓ | | | | | |
| `role:manage` | ✓ | | | | | |
| `product:read` | ✓ | ✓ | ✓ | | ✓ | ✓ |
| `product:write` | ✓ | ✓ | ✓ | | | |
| `product:delete` | ✓ | ✓ | ✓ | | | |
| `category:write` | ✓ | ✓ | ✓ | | | |
| `brand:write` | ✓ | ✓ | ✓ | | | |
| `inventory:read` | ✓ | ✓ | ✓ | | ✓ | |
| `inventory:write` | ✓ | ✓ | ✓ | | | |
| `order:read` | ✓ | ✓ | | ✓ | ✓ | ✓ |
| `order:write` | ✓ | ✓ | | ✓ | | |
| `order:status` | ✓ | ✓ | | ✓ | | |
| `payment:read` | ✓ | ✓ | | | ✓ | ✓ |
| `payment:refund` | ✓ | ✓ | | ✓ | | |
| `notification:send` | ✓ | ✓ | | | | |
| `cart:read` | ✓ | ✓ | | | ✓ | ✓ |
| `cart:write` | ✓ | ✓ | | | ✓ | ✓ |

### Implementing RBAC

**Role check (coarse-grained):**
```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/users")
public Page<UserDto> listUsers(Pageable pageable) { ... }
```

**Permission check (fine-grained):**
```java
@PreAuthorize("hasAuthority('product:write')")
@PostMapping("/products")
public ProductDto createProduct(@Valid @RequestBody CreateProductRequest request) { ... }
```

**Ownership check:**
```java
@PreAuthorize("hasRole('CUSTOMER') and #userId == authentication.principal.id")
@GetMapping("/users/{userId}/orders")
public Page<OrderDto> getMyOrders(@PathVariable String userId) { ... }
```

---

## Security Configuration Per Service

### API Gateway Security

The API Gateway is the only service that directly validates JWTs. Its `SecurityFilterChain`:

```java
@Bean
public SecurityFilterChain gatewaySecurityFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/api/v1/auth/login", "/api/v1/auth/register",
                          "/api/v1/auth/refresh", "/api/v1/auth/forgot-password",
                          "/api/v1/auth/reset-password", "/api/v1/auth/verify-email",
                          "/actuator/health/**", "/swagger-ui/**", "/v3/api-docs/**")
                .permitAll()
            .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .anyExchange().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
        )
        .build();
}
```

### Microservice Security (Trusted Subsystem Pattern)

Downstream services trust the gateway's JWT validation. They extract user context from forwarded headers:

```java
@Bean
public SecurityFilterChain serviceSecurityFilterChain(HttpSecurity http) {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health/**").permitAll()
            .requestMatchers("/api/v1/**/internal/**").hasRole("SYSTEM") // service-to-service
            .anyRequest().authenticated()
        )
        .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

The `UserContextFilter` reads `X-User-Id`, `X-User-Roles`, `X-User-Permissions` headers and populates the `SecurityContext`.

### Service-to-Service Authentication

Internal service calls use Feign clients with an interceptor that propagates user context:

```java
@Bean
public RequestInterceptor userContextInterceptor() {
    return requestTemplate -> {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            requestTemplate.header("X-User-Id", auth.getPrincipal().toString());
            requestTemplate.header("X-User-Roles",
                auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(",")));
        }
    };
}
```

---

## Password Security

### Password Policy

- Minimum 8 characters
- At least 1 uppercase, 1 lowercase, 1 digit, 1 special character
- Not in common password list (haveibeenpwned API)
- Maximum 128 characters (bcrypt input limit)

### Storage

```java
// Registration
String hashedPassword = passwordEncoder.encode(rawPassword);
user.setPasswordHash(hashedPassword);
// $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
//  ↑↑  ↑↑                    ↑↑
//  BCrypt rounds=10      22-char salt + 31-char hash
```

BCrypt with cost factor 10 (configurable). Each hash is salted uniquely — two identical passwords produce different hashes.

### Password Reset Flow

```
1. User requests reset → POST /auth/forgot-password { email }
2. System generates reset token (UUID, 15-min expiry)
3. Token hashed and stored in password_resets table
4. Email sent with reset link containing raw token
5. User clicks link → sends POST /auth/reset-password { token, newPassword }
6. System validates token hash, expiry, not-used status
7. Updates password, marks token as used
```

---

## CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000")); // dev
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
    config.setExposedHeaders(List.of("X-Request-Id", "X-RateLimit-Remaining"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L); // preflight cache

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

- CORS is only configured at the API Gateway
- Allowed origins configured per environment via Spring profiles
- Preflight cached for 1 hour

---

## CSRF Protection

CSRF is **disabled** across all services because:
1. The platform uses stateless JWT tokens (no cookies for auth)
2. The React frontend sends tokens via `Authorization` header (not cookies)
3. CSRF attacks rely on browser auto-attaching cookies, which doesn't apply here

If cookie-based auth were added (e.g., Remember Me), CSRF protection would be re-enabled with a `CsrfTokenRepository` using HttpOnly cookies.

---

## Secrets Management

### Development
- Secrets stored in `.env` file (gitignored)
- Referenced via `${ENV_VAR}` placeholders in `application.yml`
- Defaults provided for local development with Docker Compose

### Production
- Spring Cloud Config backed by HashiCorp Vault
- Kubernetes Secrets injected as environment variables
- Never commit secrets to Git
- Rotate JWT signing key on a schedule (every 90 days)

### JWT Secret

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}  # Minimum 256 bits (32 characters)
    access-token-expiration: 900000    # 15 minutes in ms
    refresh-token-expiration: 604800000 # 7 days in ms
```

---

## Security Headers

API Gateway adds security headers to all responses:

```java
@Bean
public GlobalFilter securityHeadersFilter() {
    return (exchange, chain) -> {
        exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
        exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
        exchange.getResponse().getHeaders().add("X-XSS-Protection", "0");
        exchange.getResponse().getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        exchange.getResponse().getHeaders().add("Cache-Control", "no-store");
        exchange.getResponse().getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
        return chain.filter(exchange);
    };
}
```

---

## Security Monitoring

- Failed login attempts logged and rate-limited (5 attempts per IP per 15 min via Redis)
- Suspicious activities (multiple 403s) trigger alerts
- JWT blacklist size monitored
- Security events (`LOGIN_SUCCESS`, `LOGIN_FAILURE`, `TOKEN_REFRESH`, `PASSWORD_CHANGE`) published as audit events

---

## OWASP Top 10 Mitigations

| Vulnerability | Mitigation |
|--------------|-----------|
| **A01: Broken Access Control** | `@PreAuthorize` on all endpoints, ownership checks, role hierarchy |
| **A02: Cryptographic Failures** | BCrypt for passwords, HS256 for JWTs, TLS everywhere (production) |
| **A03: Injection** | Spring Data JPA (parameterized queries), Bean Validation on all input |
| **A04: Insecure Design** | Threat modeling per service, security review in PR checklist |
| **A05: Security Misconfiguration** | Security headers, CORS whitelist, disabled actuators in prod |
| **A06: Vulnerable Components** | Dependabot/Renovate for dependency updates, OWASP Dependency Check |
| **A07: Auth Failures** | Rate-limited login, strong password policy, refresh token rotation |
| **A08: Software & Data Integrity** | Docker image signing, Helm chart provenance, checksum verification |
| **A09: Logging & Monitoring** | Structured audit logs, security event alerting, centralized Loki logging |
| **A10: SSRF** | Input validation on URLs, network policies restricting egress |

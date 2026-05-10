### 19. ★ JWT Authentication with Custom Security Filters — The Spring Security Way

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ JWT AUTHENTICATION WITH CUSTOM SECURITY FILTERS — THE SPRING SECURITY WAY                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Spring Security does NOT provide any default implementation for JWT token generation.       │
│  Unlike form-based login or Basic auth where Spring handles everything, with JWT we          │
│  must build custom filters, custom authentication tokens, and custom authentication          │
│  providers — plugged into Spring Security's existing architecture.                           │
│                                                                                              │
│  ★ The KEY principle: DO NOT put token logic in controllers!                                │
│  ★ Everything goes through the Security Filter Chain, just like form login does.            │
│  ★ We create SEPARATE filters for each concern: login, validation, refresh.                 │
│                                                                                              │
│                                                                                              │
│  ── THE 4 STEPS OF JWT AUTHENTICATION ───────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Step 1: USER CREATION                                                       │           │
│  │  → Register user with username, password, roles                             │           │
│  │  → Password hashed with BCrypt, stored in DB                                │           │
│  │  → This is a normal endpoint, no special filter needed                      │           │
│  │  → But we still secure it (e.g., permitAll or ADMIN-only)                  │           │
│  │                                                                               │           │
│  │  Step 2: TOKEN GENERATION (Login)                                            │           │
│  │  → User sends username + password                                           │           │
│  │  → Custom filter intercepts POST /auth/login                                │           │
│  │  → Filter creates Authentication object → AuthenticationManager             │           │
│  │  → AuthenticationManager finds the right AuthenticationProvider             │           │
│  │  → If valid → Filter generates Access Token + Refresh Token                │           │
│  │  → Returns tokens directly from the filter (no controller!)                │           │
│  │                                                                               │           │
│  │  Step 3: TOKEN VALIDATION (Protected API access)                             │           │
│  │  → User sends request with "Authorization: Bearer <accessToken>"            │           │
│  │  → Custom filter extracts token from header                                 │           │
│  │  → Filter creates custom JwtAuthenticationToken → AuthenticationManager     │           │
│  │  → Custom JwtAuthenticationProvider validates the token                     │           │
│  │  → If valid → set Authentication in SecurityContextHolder                  │           │
│  │  → Request continues to AuthorizationFilter → Controller                   │           │
│  │                                                                               │           │
│  │  Step 4: REFRESH TOKEN (Get new Access Token)                                │           │
│  │  → User sends refresh token to POST /auth/refresh                           │           │
│  │  → Custom filter validates the refresh token via AuthenticationManager      │           │
│  │  → If valid → generates NEW access token (keep same refresh token)         │           │
│  │  → Returns new access token directly from the filter                       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 19.1 ★ How Spring Security's ProviderManager Works Internally — The `supports()` Method

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW SPRING SECURITY'S PROVIDER MANAGER WORKS INTERNALLY                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  When a filter creates an Authentication object and calls                                    │
│  authenticationManager.authenticate(authRequest), the AuthenticationManager                  │
│  (which is actually a ProviderManager) does NOT know which provider can handle it.           │
│  It iterates through ALL registered providers and asks each one:                             │
│  "Can you handle this type of Authentication object?"                                        │
│                                                                                              │
│                                                                                              │
│  ── THE FLOW ────────────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Filter                    ProviderManager             AuthenticationProviders│           │
│  │  ──────                    ───────────────             ──────────────────────│           │
│  │    │                            │                            │               │           │
│  │    │  authenticate(authReq)     │                            │               │           │
│  │    │───────────────────────────>│                            │               │           │
│  │    │                            │                            │               │           │
│  │    │                            │  for each provider:        │               │           │
│  │    │                            │                            │               │           │
│  │    │                            │  provider.supports(        │               │           │
│  │    │                            │    authReq.getClass())?    │               │           │
│  │    │                            │───────────────────────────>│               │           │
│  │    │                            │                            │               │           │
│  │    │                            │  Provider 1 (Dao):         │               │           │
│  │    │                            │  supports(JwtAuthToken)?   │               │           │
│  │    │                            │  → NO (I only support      │               │           │
│  │    │                            │    UsernamePasswordAuth)   │               │           │
│  │    │                            │                            │               │           │
│  │    │                            │  Provider 2 (Jwt):         │               │           │
│  │    │                            │  supports(JwtAuthToken)?   │               │           │
│  │    │                            │  → YES! I can handle this  │               │           │
│  │    │                            │                            │               │           │
│  │    │                            │  provider2.authenticate(   │               │           │
│  │    │                            │    authReq)                │               │           │
│  │    │                            │───────────────────────────>│               │           │
│  │    │                            │                            │               │           │
│  │    │                            │  return Authentication     │               │           │
│  │    │                            │  (authenticated=true)      │               │           │
│  │    │                            │<───────────────────────────│               │           │
│  │    │                            │                            │               │           │
│  │    │  return Authentication     │                            │               │           │
│  │    │<───────────────────────────│                            │               │           │
│  │    │                            │                            │               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── BUILT-IN PROVIDER SUPPORT MAPPING ───────────────────────────────────────               │
│                                                                                              │
│  ┌────────────────────────────────────┬──────────────────────────────────────┐              │
│  │  Authentication Token Class         │  Supported By Provider              │              │
│  ├────────────────────────────────────┼──────────────────────────────────────┤              │
│  │  UsernamePasswordAuthenticationToken│  DaoAuthenticationProvider          │              │
│  ├────────────────────────────────────┼──────────────────────────────────────┤              │
│  │  RememberMeAuthenticationToken     │  RememberMeAuthenticationProvider   │              │
│  ├────────────────────────────────────┼──────────────────────────────────────┤              │
│  │  AnonymousAuthenticationToken      │  AnonymousAuthenticationProvider    │              │
│  ├────────────────────────────────────┼──────────────────────────────────────┤              │
│  │  PreAuthenticatedAuthenticationToken│  PreAuthenticatedAuthProvider      │              │
│  ├────────────────────────────────────┼──────────────────────────────────────┤              │
│  │  JwtAuthenticationToken (custom!)  │  JwtAuthenticationProvider (custom!)│              │
│  │  → We will create this!            │  → We will create this!             │              │
│  └────────────────────────────────────┴──────────────────────────────────────┘              │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**★ Spring Security Internal Code — ProviderManager.authenticate()**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SPRING SECURITY INTERNAL CODE — ProviderManager (simplified)
//  Package: org.springframework.security.authentication
//  
//  This is the ACTUAL code inside Spring Security that iterates through
//  all registered AuthenticationProviders and finds the one that can 
//  handle the given Authentication object.
// ═══════════════════════════════════════════════════════════════════════════════

public class ProviderManager implements AuthenticationManager {

    // ★ List of ALL registered AuthenticationProviders
    // When you create a custom provider and register it, it gets ADDED to this list
    private List<AuthenticationProvider> providers;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        
        Class<? extends Authentication> toTest = authentication.getClass();
        
        AuthenticationException lastException = null;
        Authentication result = null;

        // ★ ITERATE through ALL providers
        for (AuthenticationProvider provider : getProviders()) {
            
            // ★ Ask each provider: "Can you handle THIS type of Authentication?"
            // This is where supports() is called!
            if (!provider.supports(toTest)) {
                continue;  // This provider can't handle it, try the next one
            }

            // ★ Found a provider that supports this Authentication type!
            try {
                // Delegate to the matching provider
                result = provider.authenticate(authentication);

                if (result != null) {
                    // ★ Copy details from the original authentication to the result
                    copyDetails(authentication, result);
                    break;  // Authentication successful, stop iterating
                }
            } catch (AuthenticationException ex) {
                lastException = ex;
                // Provider threw an exception (bad credentials, etc.)
                // Continue to next provider or throw at the end
            }
        }

        if (result != null) {
            // ★ Authentication was successful!
            // Erase credentials for security (remove password from memory)
            if (eraseCredentialsAfterAuthentication && (result instanceof CredentialsContainer)) {
                ((CredentialsContainer) result).eraseCredentials();
            }
            
            // Publish authentication success event
            eventPublisher.publishAuthenticationSuccess(result);
            return result;
        }

        // ★ NO provider could authenticate → throw exception
        if (lastException != null) {
            throw lastException;
        }
        throw new ProviderNotFoundException(
            "No AuthenticationProvider found for " + toTest.getName()
        );
    }
}
```

**★ Spring Security Internal Code — AuthenticationProvider.supports()**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SPRING SECURITY INTERNAL CODE — DaoAuthenticationProvider.supports()
//  
//  This shows HOW a provider declares which Authentication token type
//  it can handle. DaoAuthenticationProvider only supports
//  UsernamePasswordAuthenticationToken.
// ═══════════════════════════════════════════════════════════════════════════════

public class DaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    // From AbstractUserDetailsAuthenticationProvider:
    @Override
    public boolean supports(Class<?> authentication) {
        // ★ "I only support UsernamePasswordAuthenticationToken!"
        // If a filter sends a JwtAuthenticationToken, this returns FALSE
        // and ProviderManager skips this provider and moves to the next one.
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected void additionalAuthenticationChecks(
            UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        
        if (authentication.getCredentials() == null) {
            throw new BadCredentialsException("Bad credentials");
        }
        
        String presentedPassword = authentication.getCredentials().toString();
        
        // ★ Compares the password from the request with the hashed password from DB
        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ KEY INSIGHT — WHY WE NEED CUSTOM PROVIDERS                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  When we create a JwtAuthenticationToken (custom), and pass it to                            │
│  authenticationManager.authenticate(jwtAuthToken):                                           │
│                                                                                              │
│  ProviderManager iterates:                                                                   │
│    → DaoAuthenticationProvider.supports(JwtAuthenticationToken.class) → FALSE ❌             │
│    → AnonymousAuthenticationProvider.supports(JwtAuthenticationToken.class) → FALSE ❌       │
│    → RememberMeAuthenticationProvider.supports(JwtAuthenticationToken.class) → FALSE ❌      │
│    → ★ NO provider supports it! → ProviderNotFoundException!                                │
│                                                                                              │
│  SOLUTION: Create JwtAuthenticationProvider that returns TRUE for JwtAuthenticationToken     │
│  and register it with the ProviderManager. Then:                                             │
│                                                                                              │
│    → DaoAuthenticationProvider.supports(JwtAuthenticationToken.class) → FALSE ❌             │
│    → JwtAuthenticationProvider.supports(JwtAuthenticationToken.class) → TRUE ✅              │
│    → JwtAuthenticationProvider.authenticate(jwtAuthToken) → returns Authentication!          │
│                                                                                              │
│  ★ ALTERNATIVE: If we use UsernamePasswordAuthenticationToken (for login), we don't         │
│    need a custom provider — DaoAuthenticationProvider already supports it!                   │
│    But for token VALIDATION, we MUST use a custom token + custom provider.                  │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 19.2 ★ Complete Architecture — All Filters, Tokens, and Providers

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE ARCHITECTURE — FILTERS, TOKENS, AND PROVIDERS                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── SECURITY FILTER CHAIN ORDER ─────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  HTTP Request                                                                │           │
│  │      │                                                                       │           │
│  │      ▼                                                                       │           │
│  │  ┌─────────────────────────────────────────────────────────────────┐        │           │
│  │  │  DelegatingFilterProxy                                         │        │           │
│  │  │  └─ FilterChainProxy                                           │        │           │
│  │  │     └─ SecurityFilterChain                                     │        │           │
│  │  │                                                                 │        │           │
│  │  │  ┌─────────────────────────────────────────────────────────┐   │        │           │
│  │  │  │  Filter 1: SecurityContextPersistenceFilter             │   │        │           │
│  │  │  │  (Creates empty SecurityContext)                         │   │        │           │
│  │  │  └─────────────────────────────────────────────────────────┘   │        │           │
│  │  │      │                                                         │        │           │
│  │  │      ▼                                                         │        │           │
│  │  │  ┌─────────────────────────────────────────────────────────┐   │        │           │
│  │  │  │  Filter 2: CsrfFilter (disabled for JWT APIs)           │   │        │           │
│  │  │  └─────────────────────────────────────────────────────────┘   │        │           │
│  │  │      │                                                         │        │           │
│  │  │      ▼                                                         │        │           │
│  │  │  ┌─────────────────────────────────────────────────────────┐   │        │           │
│  │  │  │  ★ Filter 3: JwtTokenGenerationFilter (CUSTOM)          │   │        │           │
│  │  │  │  → Intercepts POST /auth/login                          │   │        │           │
│  │  │  │  → Uses UsernamePasswordAuthenticationToken              │   │        │           │
│  │  │  │  → DaoAuthenticationProvider handles it                  │   │        │           │
│  │  │  │  → Generates Access Token + Refresh Token                │   │        │           │
│  │  │  │  → Returns tokens in response (no controller!)           │   │        │           │
│  │  │  │  Position: addFilterAt(UsernamePasswordAuthFilter.class)  │   │        │           │
│  │  │  └─────────────────────────────────────────────────────────┘   │        │           │
│  │  │      │                                                         │        │           │
│  │  │      ▼                                                         │        │           │
│  │  │  ┌─────────────────────────────────────────────────────────┐   │        │           │
│  │  │  │  ★ Filter 4: JwtTokenValidationFilter (CUSTOM)          │   │        │           │
│  │  │  │  → Intercepts ALL requests with "Authorization: Bearer" │   │        │           │
│  │  │  │  → Creates JwtAuthenticationToken (CUSTOM)               │   │        │           │
│  │  │  │  → JwtAuthenticationProvider (CUSTOM) handles it         │   │        │           │
│  │  │  │  → Sets SecurityContextHolder if valid                   │   │        │           │
│  │  │  │  → Returns 401 if invalid                                │   │        │           │
│  │  │  │  Position: addFilterBefore(                               │   │        │           │
│  │  │  │              UsernamePasswordAuthFilter.class)            │   │        │           │
│  │  │  └─────────────────────────────────────────────────────────┘   │        │           │
│  │  │      │                                                         │        │           │
│  │  │      ▼                                                         │        │           │
│  │  │  ┌─────────────────────────────────────────────────────────┐   │        │           │
│  │  │  │  ★ Filter 5: JwtRefreshTokenFilter (CUSTOM)             │   │        │           │
│  │  │  │  → Intercepts POST /auth/refresh                         │   │        │           │
│  │  │  │  → Creates JwtAuthenticationToken with refresh token     │   │        │           │
│  │  │  │  → JwtAuthenticationProvider validates it                │   │        │           │
│  │  │  │  → Generates NEW access token (same refresh token)       │   │        │           │
│  │  │  │  → Returns new access token in response                  │   │        │           │
│  │  │  │  Position: addFilterAfter(                                │   │        │           │
│  │  │  │              JwtTokenValidationFilter.class)              │   │        │           │
│  │  │  └─────────────────────────────────────────────────────────┘   │        │           │
│  │  │      │                                                         │        │           │
│  │  │      ▼                                                         │        │           │
│  │  │  ┌─────────────────────────────────────────────────────────┐   │        │           │
│  │  │  │  Filter 6: AuthorizationFilter                           │   │        │           │
│  │  │  │  → hasRole("ADMIN"), hasAnyRole("USER", "MANAGER")     │   │        │           │
│  │  │  │  → Same as form-based login! No JWT-specific changes!   │   │        │           │
│  │  │  └─────────────────────────────────────────────────────────┘   │        │           │
│  │  │      │                                                         │        │           │
│  │  │      ▼                                                         │        │           │
│  │  │  ┌─────────────────────────────────────────────────────────┐   │        │           │
│  │  │  │  Controller                                              │   │        │           │
│  │  │  └─────────────────────────────────────────────────────────┘   │        │           │
│  │  │                                                                 │        │           │
│  │  └─────────────────────────────────────────────────────────────────┘        │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHAT WE CREATE vs WHAT SPRING PROVIDES ──────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────┬──────────────────────────────────────────┐            │
│  │  WE CREATE (Custom)              │  SPRING PROVIDES (Built-in)              │            │
│  ├──────────────────────────────────┼──────────────────────────────────────────┤            │
│  │  JwtTokenGenerationFilter        │  AuthenticationManager (ProviderManager) │            │
│  │  JwtTokenValidationFilter        │  DaoAuthenticationProvider               │            │
│  │  JwtRefreshTokenFilter           │  UserDetailsService                      │            │
│  │  JwtAuthenticationToken          │  UsernamePasswordAuthenticationToken     │            │
│  │  JwtAuthenticationProvider       │  PasswordEncoder (BCrypt)                │            │
│  │  JwtService (token util)         │  SecurityContextHolder                   │            │
│  │  SecurityConfig                  │  AuthorizationFilter (hasRole etc.)      │            │
│  └──────────────────────────────────┴──────────────────────────────────────────┘            │
│                                                                                              │
│                                                                                              │
│  ── WHICH TOKEN TYPE → WHICH PROVIDER ───────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  FILTER                        TOKEN TYPE                PROVIDER             │           │
│  │  ──────                        ──────────                ────────             │           │
│  │                                                                               │           │
│  │  JwtTokenGenerationFilter  →  UsernamePasswordAuth   →  DaoAuthProvider      │           │
│  │  (Login: username+password)    Token (Spring built-in)   (Spring built-in)    │           │
│  │                                                                               │           │
│  │  ★ We REUSE Spring's existing token + provider for login!                    │           │
│  │  ★ Username/password → UsernamePasswordAuthenticationToken                   │           │
│  │  ★ DaoAuthenticationProvider already supports this token                     │           │
│  │  ★ After successful auth, WE generate the JWT tokens                         │           │
│  │                                                                               │           │
│  │  ───────────────────────────────────────────────────────────────────────      │           │
│  │                                                                               │           │
│  │  JwtTokenValidationFilter  →  JwtAuthenticationToken →  JwtAuthProvider      │           │
│  │  (API access: Bearer token)   (CUSTOM — we create!)     (CUSTOM — we create!)│           │
│  │                                                                               │           │
│  │  ★ JWT token cannot be handled by DaoAuthenticationProvider                  │           │
│  │  ★ DaoAuthenticationProvider.supports(JwtAuthToken) → FALSE                  │           │
│  │  ★ We MUST create JwtAuthenticationProvider that returns TRUE                │           │
│  │                                                                               │           │
│  │  ───────────────────────────────────────────────────────────────────────      │           │
│  │                                                                               │           │
│  │  JwtRefreshTokenFilter     →  JwtAuthenticationToken →  JwtAuthProvider      │           │
│  │  (Refresh: refresh token)     (CUSTOM — same as above)  (CUSTOM — same!)     │           │
│  │                                                                               │           │
│  │  ★ Refresh token is also a JWT → same JwtAuthenticationProvider              │           │
│  │  ★ Provider validates the token signature and expiry                         │           │
│  │  ★ After validation, filter generates NEW access token only                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 19.3 ★ Step-by-Step Implementation

##### 19.3.1 JwtService — Token Creation and Parsing Utility

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  JwtService — Utility for creating and validating JWT tokens
//  
//  This is NOT a filter or provider — it's a utility service used BY
//  the filters and providers to create/parse/validate JWT tokens.
//  
//  Dependencies: io.jsonwebtoken:jjwt-api, jjwt-impl, jjwt-jackson
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;  // e.g., 1800000 (30 minutes in ms)

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // e.g., 604800000 (7 days in ms)

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Generate Access Token ─────────────────────────────────────────
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // ★ Extract roles from the Authentication object
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)                    // ★ Roles in access token
                .claim("type", "access")                  // ★ Token type identifier
                .id(UUID.randomUUID().toString())          // ★ jti for blacklisting
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Generate Refresh Token ────────────────────────────────────────
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("type", "refresh")                 // ★ Marked as refresh token
                .id(UUID.randomUUID().toString())          // ★ jti for revocation
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Parse and Validate Token ──────────────────────────────────────
    // ★ This method THROWS an exception if the token is invalid or expired
    // Jwts.parser() handles signature verification AND expiry check automatically
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractAllClaims(token).get("roles", List.class);
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }
}
```

---

##### 19.3.2 ★ JwtAuthenticationToken — Custom Authentication Token

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  JwtAuthenticationToken — Custom Authentication Token for JWT
//
//  ★ WHY WE NEED THIS:
//  When a request comes with "Authorization: Bearer <token>", we need to
//  wrap the JWT string into an Authentication object to pass to
//  authenticationManager.authenticate().
//
//  ★ DaoAuthenticationProvider does NOT support this token type.
//  DaoAuthenticationProvider.supports(JwtAuthenticationToken.class) → FALSE
//  
//  ★ We MUST create JwtAuthenticationProvider that returns TRUE for this.
//
//  This class extends AbstractAuthenticationToken which implements
//  the Authentication interface.
// ═══════════════════════════════════════════════════════════════════════════════

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;      // The raw JWT string
    private final Object principal;  // Username (after authentication)

    // ── Constructor for UNAUTHENTICATED token ─────────────────────────
    // ★ Used by the FILTER when it first extracts the JWT from the request
    // At this point we only have the raw token, not yet validated
    public JwtAuthenticationToken(String token) {
        super(null);                    // No authorities yet
        this.token = token;
        this.principal = null;          // Unknown principal
        setAuthenticated(false);        // ★ NOT yet authenticated!
    }

    // ── Constructor for AUTHENTICATED token ───────────────────────────
    // ★ Used by the PROVIDER after successful token validation
    // Now we know the username and roles
    public JwtAuthenticationToken(Object principal, String token,
                                   Collection<? extends GrantedAuthority> authorities) {
        super(authorities);             // Set the authorities (roles)
        this.token = token;
        this.principal = principal;      // Username from JWT claims
        setAuthenticated(true);          // ★ Authenticated!
    }

    @Override
    public Object getCredentials() {
        return token;                    // The JWT token IS the credential
    }

    @Override
    public Object getPrincipal() {
        return principal;                // Username
    }

    public String getToken() {
        return token;
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW JwtAuthenticationToken FLOWS THROUGH THE SYSTEM                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Step 1: Filter creates UNAUTHENTICATED token                                │           │
│  │  ┌──────────────────────────────────────┐                                    │           │
│  │  │  JwtAuthenticationToken              │                                    │           │
│  │  │  ├── token: "eyJhbGciOi..."          │                                    │           │
│  │  │  ├── principal: null                 │                                    │           │
│  │  │  ├── authorities: null               │                                    │           │
│  │  │  └── authenticated: FALSE            │                                    │           │
│  │  └──────────────────────────────────────┘                                    │           │
│  │      │                                                                       │           │
│  │      │  authenticationManager.authenticate(unauthToken)                     │           │
│  │      ▼                                                                       │           │
│  │  Step 2: ProviderManager finds JwtAuthenticationProvider                     │           │
│  │      │                                                                       │           │
│  │      ▼                                                                       │           │
│  │  Step 3: Provider validates token and returns AUTHENTICATED token            │           │
│  │  ┌──────────────────────────────────────┐                                    │           │
│  │  │  JwtAuthenticationToken              │                                    │           │
│  │  │  ├── token: "eyJhbGciOi..."          │                                    │           │
│  │  │  ├── principal: "john@example.com"   │                                    │           │
│  │  │  ├── authorities: [ROLE_USER]        │                                    │           │
│  │  │  └── authenticated: TRUE             │                                    │           │
│  │  └──────────────────────────────────────┘                                    │           │
│  │      │                                                                       │           │
│  │      ▼                                                                       │           │
│  │  Step 4: Filter sets SecurityContextHolder.getContext()                      │           │
│  │          .setAuthentication(authenticatedToken)                              │           │
│  │      │                                                                       │           │
│  │      ▼                                                                       │           │
│  │  Step 5: AuthorizationFilter checks hasRole("ADMIN") etc.                   │           │
│  │          using the authorities from the Authentication object               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 19.3.3 ★ JwtAuthenticationProvider — Custom Authentication Provider

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  JwtAuthenticationProvider — Custom Provider for JWT Token Validation
//
//  ★ WHY WE NEED THIS:
//  No existing Spring provider supports JwtAuthenticationToken.
//  DaoAuthenticationProvider only supports UsernamePasswordAuthenticationToken.
//
//  ★ This provider is added to the ProviderManager's list of providers.
//  When ProviderManager.authenticate() iterates through providers:
//    → DaoAuthenticationProvider.supports(JwtAuthenticationToken) → FALSE
//    → JwtAuthenticationProvider.supports(JwtAuthenticationToken) → TRUE ✅
//    → JwtAuthenticationProvider.authenticate(token) is called
//
//  ★ This provider validates BOTH access tokens AND refresh tokens.
//  The filter decides what to DO with the result (set context vs return new token).
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationProvider(JwtService jwtService, 
                                     UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    // ── supports() — Tell ProviderManager "I handle JwtAuthenticationToken" ──
    // ★ This is the method ProviderManager calls on EACH provider!
    // ★ If this returns TRUE, ProviderManager calls authenticate() on this provider
    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }

    // ── authenticate() — Validate the JWT and return authenticated token ──────
    @Override
    public Authentication authenticate(Authentication authentication) 
            throws AuthenticationException {
        
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        String token = jwtAuth.getToken();

        try {
            // ★ Step 1: Parse and validate the JWT (signature + expiry)
            // jwtService.extractAllClaims() throws JwtException if invalid/expired
            String username = jwtService.extractUsername(token);

            // ★ Step 2: Load user from database to verify user still exists
            // and hasn't been banned/deleted since token was issued
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (userDetails == null || !userDetails.isEnabled()) {
                throw new BadCredentialsException("User is disabled or not found");
            }

            // ★ Step 3: Extract roles from the JWT token
            List<String> roles = jwtService.extractRoles(token);
            
            // If roles are null (e.g., refresh token has no roles), 
            // use the roles from the database
            List<SimpleGrantedAuthority> authorities;
            if (roles != null && !roles.isEmpty()) {
                authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            } else {
                authorities = userDetails.getAuthorities().stream()
                        .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                        .collect(Collectors.toList());
            }

            // ★ Step 4: Create and return AUTHENTICATED token
            // This token has principal, authorities, and authenticated=true
            return new JwtAuthenticationToken(userDetails, token, authorities);

        } catch (ExpiredJwtException ex) {
            throw new CredentialsExpiredException("JWT token has expired", ex);
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid JWT token", ex);
        }
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW JwtAuthenticationProvider FITS INTO ProviderManager                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ProviderManager (AuthenticationManager)                                     │           │
│  │  ├── providers: List<AuthenticationProvider>                                 │           │
│  │  │   ├── [0] DaoAuthenticationProvider (Spring built-in)                    │           │
│  │  │   │       supports: UsernamePasswordAuthenticationToken ✅               │           │
│  │  │   │       supports: JwtAuthenticationToken ❌                             │           │
│  │  │   │                                                                       │           │
│  │  │   ├── [1] AnonymousAuthenticationProvider (Spring built-in)              │           │
│  │  │   │       supports: AnonymousAuthenticationToken ✅                      │           │
│  │  │   │       supports: JwtAuthenticationToken ❌                             │           │
│  │  │   │                                                                       │           │
│  │  │   └── [2] ★ JwtAuthenticationProvider (OUR CUSTOM PROVIDER)             │           │
│  │  │           supports: JwtAuthenticationToken ✅                             │           │
│  │  │           supports: UsernamePasswordAuthenticationToken ❌                │           │
│  │  │                                                                           │           │
│  │  │                                                                           │           │
│  │  │  When filter sends UsernamePasswordAuthenticationToken:                  │           │
│  │  │  → [0] DaoAuthenticationProvider handles it (for LOGIN)                  │           │
│  │  │                                                                           │           │
│  │  │  When filter sends JwtAuthenticationToken:                               │           │
│  │  │  → [0] DaoAuthenticationProvider → supports? NO, skip                    │           │
│  │  │  → [1] AnonymousAuthenticationProvider → supports? NO, skip              │           │
│  │  │  → [2] JwtAuthenticationProvider → supports? YES! authenticate!          │           │
│  │  │                                                                           │           │
│  │  └──────────────────────────────────────────────────────────────             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 19.3.4 ★ Filter 1: JwtTokenGenerationFilter — Login & Token Creation

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  JwtTokenGenerationFilter — Handles POST /auth/login
//
//  ★ WHY THIS IS A FILTER AND NOT A CONTROLLER:
//  Spring Security's architecture processes authentication in FILTERS.
//  Form-based login uses UsernamePasswordAuthenticationFilter.
//  We are doing the SAME thing, but instead of redirecting to a page,
//  we return JWT tokens in the response body.
//
//  ★ FLOW:
//  1. User sends POST /auth/login with { "username": "...", "password": "..." }
//  2. This filter intercepts the request
//  3. Creates UsernamePasswordAuthenticationToken (Spring's built-in!)
//  4. Calls authenticationManager.authenticate(usernamePasswordToken)
//  5. ProviderManager finds DaoAuthenticationProvider (supports it!)
//  6. DaoAuthenticationProvider validates username/password against DB
//  7. Returns Authentication object with principal + authorities
//  8. ★ Filter uses Authentication to generate Access Token + Refresh Token
//  9. Returns tokens in the HTTP response (no controller needed!)
//
//  ★ We use UsernamePasswordAuthenticationToken (not custom) because
//  DaoAuthenticationProvider already supports it — no need to reinvent!
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class JwtTokenGenerationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtTokenGenerationFilter(AuthenticationManager authenticationManager,
                                     JwtService jwtService,
                                     ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) 
            throws ServletException, IOException {
        
        // ★ Only intercept POST /auth/login — skip everything else
        if (!"/auth/login".equals(request.getServletPath()) 
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);  // Pass to next filter
            return;
        }

        try {
            // ★ Step 1: Extract username and password from request body
            LoginRequest loginRequest = objectMapper.readValue(
                    request.getInputStream(), LoginRequest.class);

            // ★ Step 2: Create UsernamePasswordAuthenticationToken
            // This is Spring's BUILT-IN token — DaoAuthenticationProvider supports it!
            UsernamePasswordAuthenticationToken authRequest = 
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), 
                            loginRequest.getPassword()
                    );
            // ★ At this point: authenticated = false, authorities = null

            // ★ Step 3: Delegate to AuthenticationManager
            // ProviderManager iterates providers:
            //   → DaoAuthenticationProvider.supports(UsernamePasswordAuth...) → TRUE ✅
            //   → DaoAuthenticationProvider.authenticate() is called
            //   → Loads UserDetails from DB via UserDetailsService
            //   → Compares password with BCryptPasswordEncoder
            //   → If valid → returns Authentication with authenticated=true
            //   → If invalid → throws BadCredentialsException
            Authentication authentication = authenticationManager.authenticate(authRequest);
            // ★ At this point: authenticated = true, principal = UserDetails, 
            //                   authorities = [ROLE_USER, ROLE_ADMIN, etc.]

            // ★ Step 4: Generate tokens from the Authentication object
            String accessToken = jwtService.generateAccessToken(authentication);
            String refreshToken = jwtService.generateRefreshToken(authentication);

            // ★ Step 5: Return tokens in response — NO CONTROLLER NEEDED!
            // The response is sent directly from the filter
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            tokens.put("tokenType", "Bearer");

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            objectMapper.writeValue(response.getOutputStream(), tokens);

            // ★ DO NOT call filterChain.doFilter()!
            // We are done — response is already written
            // No need to go to any controller

        } catch (BadCredentialsException ex) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, String> error = Map.of(
                    "error", "Authentication failed",
                    "message", "Invalid username or password"
            );
            objectMapper.writeValue(response.getOutputStream(), error);
        }
    }
}

// ── LoginRequest DTO ──────────────────────────────────────────────────────
public class LoginRequest {
    private String username;
    private String password;
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ TOKEN GENERATION FLOW — STEP BY STEP                                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Client                 JwtTokenGenFilter     ProviderManager      DaoAuthProv│           │
│  │  ──────                 ─────────────────     ──────────────      ───────────│           │
│  │    │                         │                      │                  │      │           │
│  │    │  POST /auth/login       │                      │                  │      │           │
│  │    │  {user, pass}           │                      │                  │      │           │
│  │    │────────────────────────>│                      │                  │      │           │
│  │    │                         │                      │                  │      │           │
│  │    │                         │  Create               │                  │      │           │
│  │    │                         │  UsernamePassword     │                  │      │           │
│  │    │                         │  AuthenticationToken  │                  │      │           │
│  │    │                         │  (user, pass)         │                  │      │           │
│  │    │                         │  authenticated=false  │                  │      │           │
│  │    │                         │                      │                  │      │           │
│  │    │                         │  authenticate(token)  │                  │      │           │
│  │    │                         │─────────────────────>│                  │      │           │
│  │    │                         │                      │                  │      │           │
│  │    │                         │                      │  supports()?     │      │           │
│  │    │                         │                      │─────────────────>│      │           │
│  │    │                         │                      │  YES ✅          │      │           │
│  │    │                         │                      │<─────────────────│      │           │
│  │    │                         │                      │                  │      │           │
│  │    │                         │                      │  authenticate()  │      │           │
│  │    │                         │                      │─────────────────>│      │           │
│  │    │                         │                      │  Load user from  │      │           │
│  │    │                         │                      │  DB, check pass  │      │           │
│  │    │                         │                      │  (BCrypt)        │      │           │
│  │    │                         │                      │                  │      │           │
│  │    │                         │                      │  Authentication  │      │           │
│  │    │                         │                      │  (authenticated  │      │           │
│  │    │                         │                      │   =true, roles)  │      │           │
│  │    │                         │                      │<─────────────────│      │           │
│  │    │                         │                      │                  │      │           │
│  │    │                         │  Authentication       │                  │      │           │
│  │    │                         │<─────────────────────│                  │      │           │
│  │    │                         │                      │                  │      │           │
│  │    │                         │  Generate tokens     │                  │      │           │
│  │    │                         │  from Authentication  │                  │      │           │
│  │    │                         │  (jwtService)         │                  │      │           │
│  │    │                         │                      │                  │      │           │
│  │    │  {                      │                      │                  │      │           │
│  │    │   accessToken,          │                      │                  │      │           │
│  │    │   refreshToken,         │                      │                  │      │           │
│  │    │   tokenType: "Bearer"   │                      │                  │      │           │
│  │    │  }                      │                      │                  │      │           │
│  │    │<────────────────────────│                      │                  │      │           │
│  │    │                         │                      │                  │      │           │
│  │    │  ★ Response sent directly from filter!                            │      │           │
│  │    │  ★ No controller involved!                                        │      │           │
│  │    │  ★ filterChain.doFilter() is NOT called!                          │      │           │
│  │    │                         │                      │                  │      │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 19.3.5 ★ Filter 2: JwtTokenValidationFilter — Validate Access Token on Every Request

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  JwtTokenValidationFilter — Validates JWT on protected API requests
//
//  ★ FLOW:
//  1. Request comes with "Authorization: Bearer <accessToken>"
//  2. This filter extracts the JWT from the header
//  3. Creates JwtAuthenticationToken (UNAUTHENTICATED — our custom token!)
//  4. Calls authenticationManager.authenticate(jwtAuthToken)
//  5. ProviderManager iterates:
//     → DaoAuthenticationProvider.supports(JwtAuthenticationToken) → FALSE ❌
//     → JwtAuthenticationProvider.supports(JwtAuthenticationToken) → TRUE ✅
//  6. JwtAuthenticationProvider validates token (signature, expiry, user exists)
//  7. Returns AUTHENTICATED JwtAuthenticationToken with principal + authorities
//  8. Filter sets Authentication in SecurityContextHolder
//  9. Request continues to AuthorizationFilter → Controller
//
//  ★ If token is invalid/expired → return 401 immediately (no controller)
//  ★ If no token in header → skip this filter (let other filters handle it)
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class JwtTokenValidationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;

    public JwtTokenValidationFilter(AuthenticationManager authenticationManager,
                                     ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) 
            throws ServletException, IOException {

        // ★ Skip login, refresh, and user registration endpoints
        String path = request.getServletPath();
        if ("/auth/login".equals(path) || "/auth/refresh".equals(path) 
                || "/auth/register".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ★ Step 1: Extract JWT from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token → pass to next filter (might be a public endpoint)
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);  // Remove "Bearer " prefix

        try {
            // ★ Step 2: Create UNAUTHENTICATED JwtAuthenticationToken
            // This is our CUSTOM token — no existing provider supports it!
            JwtAuthenticationToken jwtAuthToken = new JwtAuthenticationToken(jwt);
            // At this point: authenticated=false, principal=null, authorities=null

            // ★ Step 3: Delegate to AuthenticationManager
            // ProviderManager iterates through providers:
            //   → DaoAuthenticationProvider.supports(JwtAuthenticationToken) → FALSE ❌
            //   → AnonymousAuthenticationProvider.supports(...) → FALSE ❌
            //   → JwtAuthenticationProvider.supports(JwtAuthenticationToken) → TRUE ✅
            //   → JwtAuthenticationProvider.authenticate() is called
            //   → Validates signature, expiry, loads user from DB
            //   → Returns AUTHENTICATED JwtAuthenticationToken
            Authentication authentication = authenticationManager.authenticate(jwtAuthToken);
            // At this point: authenticated=true, principal=UserDetails, 
            //                authorities=[ROLE_USER, ROLE_ADMIN, etc.]

            // ★ Step 4: Set Authentication in SecurityContextHolder
            // This is the SAME thing form-based login does!
            // AuthorizationFilter will use this to check hasRole("ADMIN") etc.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // ★ Step 5: Continue to next filter (AuthorizationFilter → Controller)
            filterChain.doFilter(request, response);

        } catch (AuthenticationException ex) {
            // ★ Token is invalid or expired → return 401 immediately
            SecurityContextHolder.clearContext();
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, String> error = Map.of(
                    "error", "Authentication failed",
                    "message", ex.getMessage()
            );
            objectMapper.writeValue(response.getOutputStream(), error);
            // ★ DO NOT call filterChain.doFilter() — we're done
        }
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ TOKEN VALIDATION FLOW — STEP BY STEP                                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Client              JwtValidationFilter  ProviderManager   JwtAuthProvider  │           │
│  │  ──────              ───────────────────  ──────────────   ────────────────  │           │
│  │    │                       │                    │                │            │           │
│  │    │  GET /api/orders      │                    │                │            │           │
│  │    │  Authorization:       │                    │                │            │           │
│  │    │  Bearer eyJhbG...     │                    │                │            │           │
│  │    │──────────────────────>│                    │                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │  Create             │                │            │           │
│  │    │                       │  JwtAuthToken       │                │            │           │
│  │    │                       │  (unauthenticated)  │                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │  authenticate()    │                │            │           │
│  │    │                       │───────────────────>│                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │                    │  supports()?   │            │           │
│  │    │                       │                    │  DaoAuth → NO  │            │           │
│  │    │                       │                    │  JwtAuth → YES │            │           │
│  │    │                       │                    │───────────────>│            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │                    │  authenticate()│            │           │
│  │    │                       │                    │───────────────>│            │           │
│  │    │                       │                    │  Verify sig    │            │           │
│  │    │                       │                    │  Check expiry  │            │           │
│  │    │                       │                    │  Load user DB  │            │           │
│  │    │                       │                    │  Extract roles │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │                    │  Authenticated │            │           │
│  │    │                       │                    │  JwtAuthToken  │            │           │
│  │    │                       │                    │<───────────────│            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │  Authentication    │                │            │           │
│  │    │                       │<───────────────────│                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │  SecurityContext    │                │            │           │
│  │    │                       │  .setAuthentication │                │            │           │
│  │    │                       │  (authResult)      │                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │  filterChain       │                │            │           │
│  │    │                       │  .doFilter()       │                │            │           │
│  │    │                       │──────>             │                │            │           │
│  │    │                       │  AuthorizationFilter                │            │           │
│  │    │                       │  hasRole("USER")? ✅                │            │           │
│  │    │                       │──────>             │                │            │           │
│  │    │                       │  Controller        │                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │  200 OK [{data}]      │                    │                │            │           │
│  │    │<──────────────────────│                    │                │            │           │
│  │    │                       │                    │                │            │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ★ Role checking (hasRole, hasAnyRole) works EXACTLY the same as form-based login!          │
│  ★ AuthorizationFilter reads authorities from SecurityContextHolder                         │
│  ★ The authorities were set by JwtAuthenticationProvider from the JWT claims                │
│  ★ No JWT-specific changes needed for authorization — Spring handles it!                    │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 19.3.6 ★ Filter 3: JwtRefreshTokenFilter — Issue New Access Token

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  JwtRefreshTokenFilter — Handles POST /auth/refresh
//
//  ★ FLOW:
//  1. Client sends POST /auth/refresh with { "refreshToken": "eyJhbG..." }
//  2. This filter intercepts the request
//  3. Creates JwtAuthenticationToken with the refresh token (SAME custom token!)
//  4. Calls authenticationManager.authenticate(jwtAuthToken)
//  5. JwtAuthenticationProvider validates the refresh token:
//     → Signature valid? Expiry valid? User still exists?
//  6. If valid → Filter generates NEW access token (from the Authentication object)
//  7. Returns NEW access token + SAME refresh token
//  8. ★ Refresh token is NOT regenerated — it stays the same until it expires
//
//  ★ If refresh token is invalid or expired → return 401
//  ★ Client must re-login when refresh token expires
//
//  ★ We use the SAME JwtAuthenticationProvider as the validation filter!
//  Both access tokens and refresh tokens are JWTs — same validation logic.
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class JwtRefreshTokenFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtRefreshTokenFilter(AuthenticationManager authenticationManager,
                                  JwtService jwtService,
                                  ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        // ★ Only intercept POST /auth/refresh — skip everything else
        if (!"/auth/refresh".equals(request.getServletPath())
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // ★ Step 1: Extract refresh token from request body
            RefreshRequest refreshRequest = objectMapper.readValue(
                    request.getInputStream(), RefreshRequest.class);
            String refreshToken = refreshRequest.getRefreshToken();

            // ★ Step 2: Verify this is actually a REFRESH token (not an access token)
            String tokenType = jwtService.extractTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                throw new BadCredentialsException("Invalid token type. Expected refresh token.");
            }

            // ★ Step 3: Create UNAUTHENTICATED JwtAuthenticationToken
            // Same custom token type as the validation filter!
            JwtAuthenticationToken jwtAuthToken = new JwtAuthenticationToken(refreshToken);

            // ★ Step 4: Delegate to AuthenticationManager
            // ProviderManager → JwtAuthenticationProvider validates the refresh token
            // (same provider as for access token validation!)
            Authentication authentication = authenticationManager.authenticate(jwtAuthToken);
            // At this point: authenticated=true, principal=UserDetails

            // ★ Step 5: Generate NEW access token from the Authentication object
            // The Authentication contains the user's current roles from DB
            // (JwtAuthenticationProvider loaded fresh UserDetails from DB)
            String newAccessToken = jwtService.generateAccessToken(authentication);

            // ★ Step 6: Return new access token + SAME refresh token
            // We do NOT generate a new refresh token!
            // The original refresh token continues to be valid until its own expiry
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", refreshToken);  // ★ Same refresh token!
            tokens.put("tokenType", "Bearer");

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            objectMapper.writeValue(response.getOutputStream(), tokens);

            // ★ DO NOT call filterChain.doFilter() — response already sent

        } catch (AuthenticationException ex) {
            // ★ Refresh token is invalid or expired
            // Client MUST re-login (POST /auth/login with username + password)
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, String> error = Map.of(
                    "error", "Refresh token invalid or expired",
                    "message", ex.getMessage()
            );
            objectMapper.writeValue(response.getOutputStream(), error);
        }
    }
}

// ── RefreshRequest DTO ────────────────────────────────────────────────────
public class RefreshRequest {
    private String refreshToken;
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ REFRESH TOKEN FLOW — STEP BY STEP                                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Client              JwtRefreshFilter     ProviderManager   JwtAuthProvider  │           │
│  │  ──────              ────────────────     ──────────────   ────────────────  │           │
│  │    │                       │                    │                │            │           │
│  │    │  POST /auth/refresh   │                    │                │            │           │
│  │    │  {refreshToken:       │                    │                │            │           │
│  │    │   "eyJhbG..."}        │                    │                │            │           │
│  │    │──────────────────────>│                    │                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │  Verify type       │                │            │           │
│  │    │                       │  == "refresh" ✅   │                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │  Create             │                │            │           │
│  │    │                       │  JwtAuthToken       │                │            │           │
│  │    │                       │  (unauthenticated)  │                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │  authenticate()    │                │            │           │
│  │    │                       │───────────────────>│                │            │           │
│  │    │                       │                    │  supports()?   │            │           │
│  │    │                       │                    │  JwtAuth → YES │            │           │
│  │    │                       │                    │───────────────>│            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │                    │  Validate JWT  │            │           │
│  │    │                       │                    │  signature +   │            │           │
│  │    │                       │                    │  expiry +      │            │           │
│  │    │                       │                    │  user exists   │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │                    │  Authenticated │            │           │
│  │    │                       │  Authentication    │<───────────────│            │           │
│  │    │                       │<───────────────────│                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │                       │  Generate NEW       │                │            │           │
│  │    │                       │  access token       │                │            │           │
│  │    │                       │  from Authentication│                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │  {                    │                    │                │            │           │
│  │    │   accessToken: NEW,   │                    │                │            │           │
│  │    │   refreshToken: SAME, │  ★ Refresh token NOT regenerated!  │            │           │
│  │    │   tokenType: "Bearer" │                    │                │            │           │
│  │    │  }                    │                    │                │            │           │
│  │    │<──────────────────────│                    │                │            │           │
│  │    │                       │                    │                │            │           │
│  │    │  ★ If refresh token expired:               │                │            │           │
│  │    │  → 401 { "error": "Refresh token expired" }│                │            │           │
│  │    │  → Client MUST re-login with username+password              │            │           │
│  │    │                       │                    │                │            │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 19.3.7 ★ SecurityConfig — Wiring Everything Together

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig — Register all custom filters, providers, and endpoints
//
//  ★ THIS IS WHERE EVERYTHING COMES TOGETHER:
//  1. Register JwtAuthenticationProvider into ProviderManager
//  2. Expose AuthenticationManager as a Bean
//  3. Add custom filters at the correct positions in the filter chain
//  4. Configure endpoint security (permitAll, hasRole, etc.)
//  5. Disable CSRF, sessions (stateless for JWT)
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @Secured, etc.
public class SecurityConfig {

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationProvider jwtAuthenticationProvider,
                          JwtService jwtService,
                          UserDetailsService userDetailsService) {
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    // ── Expose AuthenticationManager as a Bean ────────────────────────
    // ★ We need this because our custom filters require AuthenticationManager
    // ★ We configure it with BOTH DaoAuthenticationProvider AND JwtAuthenticationProvider
    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http) throws Exception {

        AuthenticationManagerBuilder authBuilder = 
                http.getSharedObject(AuthenticationManagerBuilder.class);

        // ★ Register DaoAuthenticationProvider (for username/password login)
        // DaoAuthenticationProvider is configured automatically when we provide
        // UserDetailsService and PasswordEncoder beans
        authBuilder.userDetailsService(userDetailsService)
                   .passwordEncoder(passwordEncoder());

        // ★ Register our CUSTOM JwtAuthenticationProvider
        // This gets ADDED to the ProviderManager's list of providers!
        // Now ProviderManager has:
        //   [0] DaoAuthenticationProvider (supports UsernamePasswordAuth)
        //   [1] JwtAuthenticationProvider (supports JwtAuthenticationToken)
        authBuilder.authenticationProvider(jwtAuthenticationProvider);

        return authBuilder.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── SecurityFilterChain — The Main Configuration ──────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    AuthenticationManager authenticationManager) 
            throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        // ★ Create filter instances with the AuthenticationManager
        JwtTokenGenerationFilter tokenGenFilter = 
                new JwtTokenGenerationFilter(authenticationManager, jwtService, objectMapper);

        JwtTokenValidationFilter tokenValidationFilter = 
                new JwtTokenValidationFilter(authenticationManager, objectMapper);

        JwtRefreshTokenFilter refreshTokenFilter = 
                new JwtRefreshTokenFilter(authenticationManager, jwtService, objectMapper);

        http
            // ★ Disable CSRF — JWT is immune to CSRF (no cookies used)
            .csrf(csrf -> csrf.disable())

            // ★ Stateless session — JWT is stateless, no HttpSession needed
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ★ Endpoint security — role-based authorization
            // Works EXACTLY the same as form-based login!
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no authentication needed
                .requestMatchers("/auth/login", "/auth/refresh", "/auth/register").permitAll()

                // ★ Role-based access — same as form login!
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN", "MANAGER")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // ★ Add custom filters at the correct positions in the chain
            // ORDER MATTERS! Validation must come before generation
            // so that already-authenticated requests don't hit the login filter

            // Filter 1: Token Validation — runs on EVERY request with Bearer token
            // Placed BEFORE UsernamePasswordAuthenticationFilter position
            .addFilterBefore(tokenValidationFilter, 
                             UsernamePasswordAuthenticationFilter.class)

            // Filter 2: Token Generation — only for POST /auth/login
            // Placed AT the UsernamePasswordAuthenticationFilter position
            .addFilterAt(tokenGenFilter, 
                         UsernamePasswordAuthenticationFilter.class)

            // Filter 3: Refresh Token — only for POST /auth/refresh
            // Placed AFTER UsernamePasswordAuthenticationFilter position
            .addFilterAfter(refreshTokenFilter, 
                            UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SECURITY FILTER CHAIN — FINAL ORDER AFTER CONFIGURATION                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── REGISTERED PROVIDERS IN ProviderManager ──                               │           │
│  │                                                                               │           │
│  │  ProviderManager.providers = [                                               │           │
│  │    DaoAuthenticationProvider          ← handles UsernamePasswordAuth          │           │
│  │    JwtAuthenticationProvider          ← handles JwtAuthenticationToken        │           │
│  │  ]                                                                            │           │
│  │                                                                               │           │
│  │  ── FILTER CHAIN ORDER ──                                                    │           │
│  │                                                                               │           │
│  │  Request                                                                      │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  SecurityContextPersistenceFilter  (Spring)                                  │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  CorsFilter  (Spring)                                                        │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  ★ JwtTokenValidationFilter  (CUSTOM — addFilterBefore)                     │           │
│  │    │  → Extracts Bearer token from Authorization header                     │           │
│  │    │  → Creates JwtAuthenticationToken → JwtAuthenticationProvider           │           │
│  │    │  → Sets SecurityContext if valid                                        │           │
│  │    │  → Skips /auth/login, /auth/refresh, /auth/register                    │           │
│  │    ▼                                                                          │           │
│  │  ★ JwtTokenGenerationFilter  (CUSTOM — addFilterAt)                         │           │
│  │    │  → Only intercepts POST /auth/login                                    │           │
│  │    │  → Creates UsernamePasswordAuth → DaoAuthenticationProvider            │           │
│  │    │  → Returns access + refresh tokens in response                         │           │
│  │    ▼                                                                          │           │
│  │  ★ JwtRefreshTokenFilter  (CUSTOM — addFilterAfter)                         │           │
│  │    │  → Only intercepts POST /auth/refresh                                  │           │
│  │    │  → Creates JwtAuthenticationToken → JwtAuthenticationProvider           │           │
│  │    │  → Returns NEW access token + SAME refresh token                       │           │
│  │    ▼                                                                          │           │
│  │  AuthorizationFilter  (Spring)                                               │           │
│  │    │  → hasRole("ADMIN"), hasAnyRole("USER", "MANAGER")                     │           │
│  │    │  → Reads authorities from SecurityContextHolder                        │           │
│  │    │  → ★ SAME as form-based login! No JWT changes needed!                 │           │
│  │    ▼                                                                          │           │
│  │  Controller (DispatcherServlet)                                               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 19.3.8 UserDetailsService and User Entity — User Creation

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  User Entity — Stored in database
// ═══════════════════════════════════════════════════════════════════════════════

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;  // BCrypt hashed

    @Column(nullable = false)
    private String roles;     // Comma-separated: "ROLE_USER,ROLE_ADMIN"

    private boolean enabled = true;

    // Getters, setters, constructors...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  CustomUserDetailsService — Loads user from DB for DaoAuthenticationProvider
//
//  ★ DaoAuthenticationProvider uses this service to load user details
//  during login (token generation). When authenticationManager.authenticate()
//  is called with UsernamePasswordAuthenticationToken, DaoAuthenticationProvider
//  calls this service's loadUserByUsername() method.
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));

        // Convert comma-separated roles to GrantedAuthority list
        List<SimpleGrantedAuthority> authorities = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,   // accountNonExpired, credentialsNonExpired, accountNonLocked
                authorities
        );
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  UserRepository
// ═══════════════════════════════════════════════════════════════════════════════

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AuthController — Only for User Registration (Step 1)
//  
//  ★ NOTE: Login and Refresh are handled by FILTERS, not controllers!
//  This controller only handles user registration which is a simple
//  CRUD operation, not an authentication concern.
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, 
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── User Registration (Step 1) ────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already exists"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // ★ BCrypt hash!
        user.setRoles(request.getRoles() != null ? request.getRoles() : "ROLE_USER");
        user.setEnabled(true);

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully"));
    }
}

// ── RegisterRequest DTO ───────────────────────────────────────────────
public class RegisterRequest {
    private String username;
    private String password;
    private String roles;
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
}
```

---

##### 19.3.9 application.yml Configuration

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — JWT Configuration
# ═══════════════════════════════════════════════════════════════════════════════

jwt:
  # ★ Secret key for HMAC signing (HS256)
  # In production: use environment variable or vault!
  # Generate: openssl rand -base64 64
  secret: "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBIUzI1NiBhbGdvcml0aG0..."
  
  # Access token: 30 minutes (in milliseconds)
  access-token-expiration: 1800000
  
  # Refresh token: 7 days (in milliseconds)
  refresh-token-expiration: 604800000

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

---

##### 19.3.10 Dependencies (pom.xml)

```xml
<!-- ═══════════════════════════════════════════════════════════════════════ -->
<!--  Required Dependencies for JWT Authentication                         -->
<!-- ═══════════════════════════════════════════════════════════════════════ -->

<dependencies>
    <!-- Spring Boot Starter Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Starter Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- ★ JJWT — JSON Web Token library (io.jsonwebtoken) -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

---

#### 19.4 ★ Complete Request Flow — All 4 Steps End-to-End

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE END-TO-END FLOW — ALL 4 STEPS                                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── STEP 1: USER REGISTRATION ───────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  POST /auth/register                                                         │           │
│  │  {                                                                            │           │
│  │    "username": "john",                                                       │           │
│  │    "password": "secret123",                                                  │           │
│  │    "roles": "ROLE_USER"                                                      │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  → SecurityFilterChain: /auth/register is permitAll()                       │           │
│  │  → No authentication needed                                                 │           │
│  │  → AuthController.register() handles it                                     │           │
│  │  → Password hashed: BCrypt("secret123") → "$2a$10$..."                     │           │
│  │  → Saved to database                                                         │           │
│  │                                                                               │           │
│  │  Response: 201 Created                                                       │           │
│  │  { "message": "User registered successfully" }                              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── STEP 2: LOGIN (TOKEN GENERATION) ────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  POST /auth/login                                                            │           │
│  │  { "username": "john", "password": "secret123" }                            │           │
│  │                                                                               │           │
│  │  → JwtTokenGenerationFilter intercepts                                      │           │
│  │  → Creates UsernamePasswordAuthenticationToken("john", "secret123")         │           │
│  │  → authenticationManager.authenticate()                                     │           │
│  │    → ProviderManager: DaoAuthenticationProvider.supports()? YES ✅          │           │
│  │    → DaoAuthenticationProvider.authenticate()                               │           │
│  │      → UserDetailsService.loadUserByUsername("john")                        │           │
│  │      → BCrypt.matches("secret123", "$2a$10$...") → TRUE ✅                 │           │
│  │      → Returns Authentication(principal=john, authorities=[ROLE_USER])      │           │
│  │  → Filter generates access token + refresh token                            │           │
│  │  → Returns directly from filter (no controller!)                            │           │
│  │                                                                               │           │
│  │  Response: 200 OK                                                            │           │
│  │  {                                                                            │           │
│  │    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",   ← 30 min expiry            │           │
│  │    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",  ← 7 day expiry             │           │
│  │    "tokenType": "Bearer"                                                     │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── STEP 3: ACCESS PROTECTED API ────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  GET /api/orders                                                              │           │
│  │  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...                              │           │
│  │                                                                               │           │
│  │  → JwtTokenValidationFilter intercepts                                      │           │
│  │  → Extracts JWT from "Authorization: Bearer ..." header                     │           │
│  │  → Creates JwtAuthenticationToken("eyJhbG...")  (UNAUTHENTICATED)           │           │
│  │  → authenticationManager.authenticate()                                     │           │
│  │    → ProviderManager: DaoAuthenticationProvider.supports()? NO ❌            │           │
│  │    → ProviderManager: JwtAuthenticationProvider.supports()? YES ✅           │           │
│  │    → JwtAuthenticationProvider.authenticate()                               │           │
│  │      → Verify signature (HMAC-SHA256) ✅                                    │           │
│  │      → Check expiry (not expired) ✅                                        │           │
│  │      → Load user from DB (still exists, not banned) ✅                      │           │
│  │      → Extract roles from token claims                                      │           │
│  │      → Return AUTHENTICATED JwtAuthenticationToken                          │           │
│  │  → Filter: SecurityContextHolder.setAuthentication(authResult)              │           │
│  │  → filterChain.doFilter() → continues to next filter                        │           │
│  │  → AuthorizationFilter: hasAnyRole("USER")? ✅ (from SecurityContext)       │           │
│  │  → Controller: returns data                                                  │           │
│  │                                                                               │           │
│  │  Response: 200 OK                                                            │           │
│  │  [{"orderId": 1, "item": "Laptop"}, ...]                                   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── STEP 4: REFRESH TOKEN (After Access Token Expires) ──────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ··· 30 minutes later ··· access token expired!                             │           │
│  │                                                                               │           │
│  │  POST /auth/refresh                                                          │           │
│  │  { "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }                             │           │
│  │                                                                               │           │
│  │  → JwtRefreshTokenFilter intercepts                                         │           │
│  │  → Extracts refresh token from body                                         │           │
│  │  → Verifies type == "refresh" ✅                                            │           │
│  │  → Creates JwtAuthenticationToken("eyJhbG...")  (UNAUTHENTICATED)           │           │
│  │  → authenticationManager.authenticate()                                     │           │
│  │    → ProviderManager: JwtAuthenticationProvider.supports()? YES ✅           │           │
│  │    → JwtAuthenticationProvider.authenticate()                               │           │
│  │      → Verify signature ✅                                                   │           │
│  │      → Check expiry (refresh token still valid — 7 day expiry) ✅           │           │
│  │      → Load user from DB ✅                                                  │           │
│  │      → Return AUTHENTICATED token                                           │           │
│  │  → Filter: Generate NEW access token from Authentication object             │           │
│  │  → Return NEW access token + SAME refresh token                             │           │
│  │                                                                               │           │
│  │  Response: 200 OK                                                            │           │
│  │  {                                                                            │           │
│  │    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",   ← NEW access token!         │           │
│  │    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",  ← SAME refresh token        │           │
│  │    "tokenType": "Bearer"                                                     │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ User never had to re-enter password!                                     │           │
│  │  ★ When refresh token expires (7 days) → must login again                   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 19.5 ★ Role-Based Authorization — Same as Form Login!

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ROLE-BASED AUTHORIZATION WITH JWT — SAME AS FORM LOGIN!                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ★ Key insight: Once you set the Authentication in SecurityContextHolder,                   │
│  Spring Security's authorization layer works EXACTLY the same regardless                    │
│  of whether authentication was done via form login, basic auth, or JWT.                     │
│                                                                                              │
│  The AuthorizationFilter reads the authorities from SecurityContext.                        │
│  It doesn't care HOW those authorities got there!                                            │
│                                                                                              │
│                                                                                              │
│  ── SecurityConfig: URL-Based Authorization ─────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  .authorizeHttpRequests(auth -> auth                                         │           │
│  │      .requestMatchers("/auth/**").permitAll()                                │           │
│  │      .requestMatchers("/api/admin/**").hasRole("ADMIN")                     │           │
│  │      .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")     │           │
│  │      .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN", "MANAGER")     │           │
│  │      .anyRequest().authenticated()                                           │           │
│  │  )                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── Method-Level Authorization with @PreAuthorize ───────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  @RestController                                                             │           │
│  │  @RequestMapping("/api")                                                     │           │
│  │  public class OrderController {                                              │           │
│  │                                                                               │           │
│  │      // Any authenticated user with USER role                                │           │
│  │      @GetMapping("/orders")                                                  │           │
│  │      @PreAuthorize("hasRole('USER')")                                       │           │
│  │      public List<Order> getOrders() { ... }                                 │           │
│  │                                                                               │           │
│  │      // Only ADMIN role                                                      │           │
│  │      @DeleteMapping("/orders/{id}")                                          │           │
│  │      @PreAuthorize("hasRole('ADMIN')")                                      │           │
│  │      public void deleteOrder(@PathVariable Long id) { ... }                 │           │
│  │                                                                               │           │
│  │      // ADMIN or MANAGER role                                                │           │
│  │      @GetMapping("/reports")                                                 │           │
│  │      @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")                        │           │
│  │      public List<Report> getReports() { ... }                               │           │
│  │                                                                               │           │
│  │      // Access current user from SecurityContext                             │           │
│  │      @GetMapping("/profile")                                                 │           │
│  │      public UserDetails getProfile(Authentication authentication) {         │           │
│  │          // ★ This Authentication object is the SAME one that was set       │           │
│  │          // by JwtTokenValidationFilter in SecurityContextHolder!            │           │
│  │          return (UserDetails) authentication.getPrincipal();                 │           │
│  │      }                                                                       │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── HOW ROLES FLOW FROM JWT TO AUTHORIZATION ────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  JWT Token Claims:                                                           │           │
│  │  { "sub": "john", "roles": ["ROLE_USER", "ROLE_ADMIN"], "type": "access" }  │           │
│  │                          │                                                    │           │
│  │                          │  JwtAuthenticationProvider extracts roles          │           │
│  │                          ▼                                                    │           │
│  │  Authentication Object:                                                      │           │
│  │  JwtAuthenticationToken {                                                    │           │
│  │    principal: UserDetails("john"),                                           │           │
│  │    authorities: [ROLE_USER, ROLE_ADMIN],  ← from JWT claims                 │           │
│  │    authenticated: true                                                       │           │
│  │  }                                                                            │           │
│  │                          │                                                    │           │
│  │                          │  JwtTokenValidationFilter sets context             │           │
│  │                          ▼                                                    │           │
│  │  SecurityContextHolder.getContext().setAuthentication(auth);                 │           │
│  │                          │                                                    │           │
│  │                          │  AuthorizationFilter reads authorities             │           │
│  │                          ▼                                                    │           │
│  │  hasRole("ADMIN") → checks if authorities contain "ROLE_ADMIN" → ✅ YES     │           │
│  │  hasRole("MANAGER") → checks if authorities contain "ROLE_MANAGER" → ❌ NO  │           │
│  │                                                                               │           │
│  │  ★ IDENTICAL to how form-based login works!                                  │           │
│  │  ★ AuthorizationFilter doesn't know or care about JWT!                      │           │
│  │  ★ It only checks SecurityContextHolder.getContext().getAuthentication()     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 19.6 ★ Two Approaches Compared — Custom Token vs UsernamePasswordAuthenticationToken

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ TWO APPROACHES — WHEN TO USE WHICH                                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── APPROACH 1: USE UsernamePasswordAuthenticationToken FOR LOGIN ────────────               │
│  (This is what we did in JwtTokenGenerationFilter)                                          │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ★ WHY this works for LOGIN:                                                 │           │
│  │  → Login sends username + password in the request body                      │           │
│  │  → UsernamePasswordAuthenticationToken wraps username + password             │           │
│  │  → DaoAuthenticationProvider already supports this token type ✅             │           │
│  │  → DaoAuthenticationProvider already validates against DB ✅                 │           │
│  │  → No custom provider needed for login!                                     │           │
│  │                                                                               │           │
│  │  Code:                                                                       │           │
│  │  UsernamePasswordAuthenticationToken authRequest =                           │           │
│  │      new UsernamePasswordAuthenticationToken(username, password);            │           │
│  │  Authentication auth = authenticationManager.authenticate(authRequest);      │           │
│  │  // → DaoAuthenticationProvider handles this!                                │           │
│  │  // → auth.isAuthenticated() == true                                         │           │
│  │  // → Generate JWT tokens from this auth object                              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── APPROACH 2: USE JwtAuthenticationToken FOR VALIDATION & REFRESH ─────────               │
│  (This is what we did in JwtTokenValidationFilter and JwtRefreshTokenFilter)                │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ★ WHY this is needed for VALIDATION/REFRESH:                                │           │
│  │  → Request has a JWT token, NOT username + password                         │           │
│  │  → UsernamePasswordAuthenticationToken expects (username, password)          │           │
│  │  → We can't fake it — DaoAuthenticationProvider would try to BCrypt-match   │           │
│  │    a JWT string against the stored password hash → FAIL!                    │           │
│  │  → We NEED a custom token type that carries the JWT string                  │           │
│  │  → And a custom provider that knows how to validate JWT signatures          │           │
│  │                                                                               │           │
│  │  Code:                                                                       │           │
│  │  JwtAuthenticationToken jwtAuthToken = new JwtAuthenticationToken(jwt);     │           │
│  │  Authentication auth = authenticationManager.authenticate(jwtAuthToken);    │           │
│  │  // → DaoAuthenticationProvider.supports(JwtAuthToken) → FALSE ❌            │           │
│  │  // → JwtAuthenticationProvider.supports(JwtAuthToken) → TRUE ✅             │           │
│  │  // → JwtAuthenticationProvider validates JWT signature + expiry            │           │
│  │  // → auth.isAuthenticated() == true (if token is valid)                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── SUMMARY TABLE ───────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────┬──────────────────────────┬─────────────────────────────┐              │
│  │  Use Case          │  Authentication Token    │  Authentication Provider    │              │
│  ├──────────────────┼──────────────────────────┼─────────────────────────────┤              │
│  │  Login             │  UsernamePasswordAuth    │  DaoAuthenticationProvider  │              │
│  │  (username+pass)   │  Token (Spring built-in) │  (Spring built-in)          │              │
│  ├──────────────────┼──────────────────────────┼─────────────────────────────┤              │
│  │  Token Validation  │  JwtAuthenticationToken  │  JwtAuthenticationProvider  │              │
│  │  (Bearer token)    │  (Custom — we create!)   │  (Custom — we create!)      │              │
│  ├──────────────────┼──────────────────────────┼─────────────────────────────┤              │
│  │  Refresh Token     │  JwtAuthenticationToken  │  JwtAuthenticationProvider  │              │
│  │  (refresh JWT)     │  (Custom — same!)        │  (Custom — same!)           │              │
│  └──────────────────┴──────────────────────────┴─────────────────────────────┘              │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 19.7 Summary — Section 19 Key Takeaways

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SUMMARY — SECTION 19 KEY TAKEAWAYS                                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. SPRING DOESN'T PROVIDE JWT TOKEN GENERATION                             │           │
│  │     • No default filter for JWT — we build custom filters                   │           │
│  │     • Token logic goes in FILTERS, NOT controllers                          │           │
│  │     • Follows the same architecture as form-based login                     │           │
│  │                                                                               │           │
│  │  2. THREE CUSTOM FILTERS IN THE SECURITY FILTER CHAIN                       │           │
│  │     • JwtTokenGenerationFilter: POST /auth/login → returns tokens           │           │
│  │     • JwtTokenValidationFilter: validates Bearer token on every request     │           │
│  │     • JwtRefreshTokenFilter: POST /auth/refresh → returns new access token  │           │
│  │     • Order: Validation → Generation → Refresh                              │           │
│  │                                                                               │           │
│  │  3. HOW ProviderManager WORKS (INTERNAL CODE)                               │           │
│  │     • Iterates through ALL registered AuthenticationProviders               │           │
│  │     • Calls supports(authToken.getClass()) on EACH provider                 │           │
│  │     • First provider that returns TRUE handles the authentication           │           │
│  │     • If NO provider supports the token → ProviderNotFoundException         │           │
│  │                                                                               │           │
│  │  4. CUSTOM TOKEN + CUSTOM PROVIDER                                           │           │
│  │     • JwtAuthenticationToken: wraps JWT string, extends AbstractAuthToken    │           │
│  │     • JwtAuthenticationProvider: supports JwtAuthenticationToken             │           │
│  │     • Added to ProviderManager's list via SecurityConfig                    │           │
│  │     • ProviderManager now has: [DaoAuthProvider, JwtAuthProvider]           │           │
│  │                                                                               │           │
│  │  5. TWO APPROACHES FOR LOGIN                                                 │           │
│  │     • Option A: Use UsernamePasswordAuthenticationToken → reuse              │           │
│  │       DaoAuthenticationProvider (RECOMMENDED for login)                     │           │
│  │     • Option B: Create custom token → custom provider for everything        │           │
│  │     • For validation/refresh: MUST use custom token + provider              │           │
│  │                                                                               │           │
│  │  6. ROLE-BASED AUTHORIZATION — SAME AS FORM LOGIN                           │           │
│  │     • hasRole("ADMIN"), hasAnyRole("USER", "MANAGER") — no changes!        │           │
│  │     • @PreAuthorize("hasRole('ADMIN')") — works the same!                  │           │
│  │     • AuthorizationFilter reads authorities from SecurityContextHolder      │           │
│  │     • Doesn't care if auth came from form login, basic auth, or JWT        │           │
│  │                                                                               │           │
│  │  7. FILTER RESPONSIBILITIES                                                  │           │
│  │     • Login filter: creates tokens, returns in response (no controller!)   │           │
│  │     • Validation filter: sets SecurityContext, calls filterChain.doFilter() │           │
│  │     • Refresh filter: creates new access token, returns in response         │           │
│  │     • ★ Login + Refresh filters do NOT call filterChain.doFilter()          │           │
│  │     • ★ Validation filter DOES call filterChain.doFilter()                  │           │
│  │                                                                               │           │
│  │  8. SECURITY CONFIG WIRING                                                   │           │
│  │     • AuthenticationManagerBuilder: register both providers                 │           │
│  │     • addFilterBefore: validation filter before UsernamePasswordAuth        │           │
│  │     • addFilterAt: generation filter at UsernamePasswordAuth position       │           │
│  │     • addFilterAfter: refresh filter after UsernamePasswordAuth             │           │
│  │     • CSRF disabled, sessions STATELESS for JWT                             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 19.8 ★ Is HttpSession Created for JWT? How Does Authentication Reach the Controller?

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ IS HttpSession CREATED FOR JWT? — NO! AND HERE'S WHY                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── THE SHORT ANSWER ────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ★ NO! HttpSession is NOT created for JWT authentication.                    │           │
│  │                                                                               │           │
│  │  We explicitly disabled it in SecurityConfig:                                │           │
│  │                                                                               │           │
│  │  .sessionManagement(session ->                                               │           │
│  │      session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))        │           │
│  │                                                                               │           │
│  │  SessionCreationPolicy.STATELESS means:                                      │           │
│  │  → Spring Security will NEVER create an HttpSession                         │           │
│  │  → Spring Security will NEVER use an HttpSession to get SecurityContext     │           │
│  │  → No JSESSIONID cookie is sent to the client                              │           │
│  │  → Each request is completely independent (truly stateless!)               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── SO HOW DOES AUTHENTICATION REACH THE CONTROLLER? ────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The answer is: SecurityContextHolder — a ThreadLocal-based in-memory       │           │
│  │  storage that lives ONLY for the duration of a single HTTP request.         │           │
│  │                                                                               │           │
│  │  ★ It does NOT use HttpSession.                                              │           │
│  │  ★ It does NOT persist between requests.                                     │           │
│  │  ★ It exists ONLY in the current thread's memory.                            │           │
│  │  ★ It is cleared after the response is sent.                                 │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── THE COMPLETE FLOW: FROM FILTER TO CONTROLLER ────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ★ Understanding: Each HTTP request creates a NEW thread in Tomcat.          │           │
│  │  SecurityContextHolder uses ThreadLocal to store the Authentication          │           │
│  │  object for that specific thread (= that specific request).                  │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  Thread-1 (Request: GET /api/orders, Bearer eyJhbG...)                      │           │
│  │  ─────────────────────────────────────────────────────                       │           │
│  │                                                                               │           │
│  │  Step 1: Request enters Tomcat → new Thread-1 created                       │           │
│  │          ThreadLocal for Thread-1: { securityContext: null }                 │           │
│  │                                                                               │           │
│  │  Step 2: SecurityContextHolderFilter runs                                   │           │
│  │          → Creates empty SecurityContext                                     │           │
│  │          → Stores in ThreadLocal: { securityContext: { auth: null } }        │           │
│  │          → ★ With STATELESS policy, it does NOT check HttpSession!          │           │
│  │          → ★ With STATEFUL (form login), it WOULD load from HttpSession    │           │
│  │                                                                               │           │
│  │  Step 3: JwtTokenValidationFilter runs                                      │           │
│  │          → Extracts JWT from header                                         │           │
│  │          → Validates via authenticationManager.authenticate()               │           │
│  │          → Gets authenticated JwtAuthenticationToken back                   │           │
│  │          → ★★★ THIS IS THE KEY LINE: ★★★                                  │           │
│  │                                                                               │           │
│  │          SecurityContextHolder.getContext().setAuthentication(authResult);   │           │
│  │                                                                               │           │
│  │          → ThreadLocal for Thread-1:                                         │           │
│  │            { securityContext: { auth: JwtAuthToken(john, [ROLE_USER]) } }   │           │
│  │          → Calls filterChain.doFilter() → passes to next filter             │           │
│  │                                                                               │           │
│  │  Step 4: AuthorizationFilter runs                                            │           │
│  │          → Reads from SAME ThreadLocal:                                     │           │
│  │            SecurityContextHolder.getContext().getAuthentication()            │           │
│  │          → Gets JwtAuthToken(john, [ROLE_USER])                             │           │
│  │          → hasRole("USER")? checks authorities → YES ✅                     │           │
│  │          → Passes to next filter                                             │           │
│  │                                                                               │           │
│  │  Step 5: DispatcherServlet → Controller method                              │           │
│  │          → Spring MVC resolves the Authentication parameter:                │           │
│  │                                                                               │           │
│  │          @GetMapping("/orders")                                              │           │
│  │          public List<Order> getOrders(Authentication authentication) {       │           │
│  │              // ★ WHERE does this Authentication object come from?          │           │
│  │              // Answer: SecurityContextHolder.getContext()                   │           │
│  │              //                       .getAuthentication()                   │           │
│  │              // It reads from the SAME ThreadLocal!                          │           │
│  │              String username = authentication.getName(); // "john"           │           │
│  │          }                                                                    │           │
│  │                                                                               │           │
│  │  Step 6: Response sent → SecurityContextHolderFilter clears ThreadLocal     │           │
│  │          → SecurityContextHolder.clearContext()                              │           │
│  │          → ThreadLocal for Thread-1: { securityContext: null }               │           │
│  │          → Thread returns to Tomcat thread pool                              │           │
│  │          → ★ NOTHING is saved to HttpSession! (STATELESS policy)            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**★ Spring Security Internal Code — SecurityContextHolder (ThreadLocal)**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SPRING SECURITY INTERNAL CODE — SecurityContextHolder (simplified)
//  Package: org.springframework.security.core.context
//
//  This is how Spring Security stores the Authentication object in memory
//  for the current thread. It uses ThreadLocal — each thread has its OWN
//  copy of the SecurityContext. No HttpSession involved!
// ═══════════════════════════════════════════════════════════════════════════════

public class SecurityContextHolder {

    // ★ ThreadLocal — each thread gets its own SecurityContext
    // Thread-1 (Request A): SecurityContext { auth: JwtAuthToken(john) }
    // Thread-2 (Request B): SecurityContext { auth: JwtAuthToken(jane) }
    // Thread-3 (Request C): SecurityContext { auth: null (unauthenticated) }
    // → Each request is isolated! No cross-contamination!
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

    // ★ Get the SecurityContext for the CURRENT thread
    public static SecurityContext getContext() {
        SecurityContext ctx = contextHolder.get();
        if (ctx == null) {
            ctx = createEmptyContext();
            contextHolder.set(ctx);
        }
        return ctx;
    }

    // ★ Set the SecurityContext for the CURRENT thread
    // This is what JwtTokenValidationFilter calls!
    public static void setContext(SecurityContext context) {
        contextHolder.set(context);
    }

    // ★ Clear the SecurityContext for the CURRENT thread
    // Called after response is sent — prevents memory leaks
    // and prevents the next request on this thread from seeing old data
    public static void clearContext() {
        contextHolder.remove();
    }

    public static SecurityContext createEmptyContext() {
        return new SecurityContextImpl();
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SPRING SECURITY INTERNAL CODE — SecurityContextImpl
//  This is the actual SecurityContext object stored in ThreadLocal
// ═══════════════════════════════════════════════════════════════════════════════

public class SecurityContextImpl implements SecurityContext {

    private Authentication authentication;  // ★ The Authentication object!

    @Override
    public Authentication getAuthentication() {
        return this.authentication;
    }

    @Override
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }
}
```

**★ Spring MVC Internal Code — How Authentication Parameter is Resolved in Controller**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SPRING SECURITY INTERNAL CODE — ServletRequestMethodArgumentResolver
//  Package: org.springframework.security.web.method.annotation
//
//  When you write a controller method with an Authentication parameter,
//  Spring MVC uses this resolver to inject the Authentication object.
//  It reads directly from SecurityContextHolder — NOT from HttpSession!
// ═══════════════════════════════════════════════════════════════════════════════

// ★ This is the ACTUAL resolver Spring uses for the Authentication parameter:
public class AuthenticationPrincipalArgumentResolver 
        implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // Supports Authentication type or @AuthenticationPrincipal annotation
        return Authentication.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ...) {
        // ★★★ THIS IS WHERE IT READS THE AUTHENTICATION! ★★★
        // It reads from SecurityContextHolder (ThreadLocal)
        // NOT from HttpSession, NOT from request attribute, NOT from anywhere else!
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication;
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  So when you write this controller:
//
//  @GetMapping("/orders")
//  public List<Order> getOrders(Authentication authentication) {
//      String user = authentication.getName();  // "john"
//      ...
//  }
//
//  Spring MVC calls:
//  SecurityContextHolder.getContext().getAuthentication()
//  → Returns the JwtAuthenticationToken that was set by JwtTokenValidationFilter
//  → Same ThreadLocal, same thread, same request!
// ═══════════════════════════════════════════════════════════════════════════════
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPARISON: SESSION-BASED vs JWT — HOW AUTHENTICATION REACHES THE CONTROLLER             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── SESSION-BASED (Form Login) ──────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Request 1 (Login):                                                          │           │
│  │  POST /login { username, password }                                          │           │
│  │  → UsernamePasswordAuthenticationFilter authenticates                       │           │
│  │  → Sets SecurityContextHolder.getContext().setAuthentication(auth)          │           │
│  │  → ★ SecurityContextPersistenceFilter SAVES SecurityContext TO HttpSession  │           │
│  │  → Response: Set-Cookie: JSESSIONID=abc123                                  │           │
│  │                                                                               │           │
│  │  Request 2 (API call):                                                       │           │
│  │  GET /api/orders  Cookie: JSESSIONID=abc123                                 │           │
│  │  → ★ SecurityContextPersistenceFilter LOADS SecurityContext FROM HttpSession│           │
│  │  → Sets SecurityContextHolder.getContext().setAuthentication(savedAuth)     │           │
│  │  → AuthorizationFilter reads from SecurityContextHolder ✅                  │           │
│  │  → Controller gets Authentication from SecurityContextHolder ✅             │           │
│  │  → Response sent → SecurityContext SAVED back to HttpSession               │           │
│  │                                                                               │           │
│  │  ★ HttpSession is the BRIDGE between requests!                              │           │
│  │  ★ SecurityContext is saved to HttpSession after Request 1                  │           │
│  │  ★ SecurityContext is loaded from HttpSession at Request 2                  │           │
│  │  ★ The server must STORE the session somewhere (memory/Redis/DB)            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── JWT-BASED (Stateless) ───────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Request 1 (Login):                                                          │           │
│  │  POST /auth/login { username, password }                                    │           │
│  │  → JwtTokenGenerationFilter authenticates                                   │           │
│  │  → Returns { accessToken, refreshToken } in response body                  │           │
│  │  → ★ NO SecurityContext saved to HttpSession!                               │           │
│  │  → ★ NO JSESSIONID cookie!                                                  │           │
│  │  → ★ Response is just JSON with tokens!                                     │           │
│  │                                                                               │           │
│  │  Request 2 (API call):                                                       │           │
│  │  GET /api/orders  Authorization: Bearer eyJhbGci...                         │           │
│  │  → ★ NO HttpSession to load from! SecurityContext starts EMPTY!             │           │
│  │  → JwtTokenValidationFilter extracts JWT from header                        │           │
│  │  → Validates JWT (signature + expiry + user exists)                         │           │
│  │  → Creates Authentication from JWT claims                                   │           │
│  │  → Sets SecurityContextHolder.getContext().setAuthentication(auth)          │           │
│  │  → AuthorizationFilter reads from SecurityContextHolder ✅                  │           │
│  │  → Controller gets Authentication from SecurityContextHolder ✅             │           │
│  │  → Response sent → SecurityContext CLEARED (not saved anywhere!)            │           │
│  │                                                                               │           │
│  │  ★ JWT itself IS the "session"! It carries all the data!                    │           │
│  │  ★ No server-side storage needed (stateless!)                               │           │
│  │  ★ Every request must carry the JWT and be validated fresh                  │           │
│  │  ★ SecurityContextHolder is populated AND cleared within the SAME request   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── VISUAL: ThreadLocal LIFECYCLE WITHIN ONE REQUEST ────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Thread-1 Timeline (single request lifecycle):                               │           │
│  │                                                                               │           │
│  │  ─────────────────────────────────────────────────────────────> time          │           │
│  │                                                                               │           │
│  │  │ Request    │ SecurityContext │ JWT Validation  │ Authorization │ Controller │           │
│  │  │ Arrives    │ Filter          │ Filter          │ Filter        │            │           │
│  │  │            │                 │                 │               │            │           │
│  │  │            │ ThreadLocal:    │ ThreadLocal:    │ ThreadLocal:  │ ThreadLocal│           │
│  │  │            │ { auth: null }  │ { auth:         │ { auth:       │ { auth:    │           │
│  │  │            │                 │   JwtAuthToken  │   JwtAuthToken│  JwtAuth   │           │
│  │  │            │                 │   (john,        │   (john,      │  Token     │           │
│  │  │            │                 │    [ROLE_USER]) │    [ROLE_USER])│ (john,    │           │
│  │  │            │                 │ }               │ }             │ [ROLE_     │           │
│  │  │            │                 │                 │               │  USER])}   │           │
│  │  │            │                 │                 │               │            │           │
│  │  │            │                 │ ★ setAuth()     │ ★ getAuth()   │ ★ getAuth()│           │
│  │  │            │                 │   populates     │   checks      │   injects  │           │
│  │  │            │                 │   ThreadLocal   │   roles       │   into     │           │
│  │  │            │                 │                 │               │   method   │           │
│  │  │            │                 │                 │               │   param    │           │
│  │  │                                                                            │           │
│  │                                                                               │           │
│  │  │ Response   │ SecurityContext │                                             │           │
│  │  │ Sent       │ Filter          │                                             │           │
│  │  │            │ clearContext()  │                                             │           │
│  │  │            │ ThreadLocal:    │                                             │           │
│  │  │            │ { auth: null }  │  ★ CLEARED! Thread returns to pool.       │           │
│  │  │            │                 │  ★ Next request on this thread starts      │           │
│  │  │            │                 │    with empty SecurityContext              │           │
│  │  │                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ALL THE WAYS TO ACCESS AUTHENTICATION IN A CONTROLLER                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  // ── Method 1: Authentication parameter ─────────────────────────         │           │
│  │  // Spring MVC injects it via SecurityContextHolder.getContext()             │           │
│  │  //                                         .getAuthentication()             │           │
│  │  @GetMapping("/orders")                                                      │           │
│  │  public List<Order> getOrders(Authentication authentication) {              │           │
│  │      String username = authentication.getName();          // "john"         │           │
│  │      Collection<?> roles = authentication.getAuthorities(); // [ROLE_USER]  │           │
│  │      boolean isAuth = authentication.isAuthenticated();   // true           │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  // ── Method 2: @AuthenticationPrincipal annotation ───────────────        │           │
│  │  // Extracts just the Principal (UserDetails) from Authentication            │           │
│  │  @GetMapping("/profile")                                                     │           │
│  │  public String getProfile(                                                   │           │
│  │          @AuthenticationPrincipal UserDetails userDetails) {                 │           │
│  │      String username = userDetails.getUsername();                            │           │
│  │      // ★ Internally: SecurityContextHolder.getContext()                    │           │
│  │      //                .getAuthentication().getPrincipal()                   │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  // ── Method 3: SecurityContextHolder directly ────────────────────        │           │
│  │  // Can be called from ANYWHERE — service, repository, utility class        │           │
│  │  @GetMapping("/data")                                                        │           │
│  │  public String getData() {                                                   │           │
│  │      Authentication auth = SecurityContextHolder.getContext()               │           │
│  │                                   .getAuthentication();                      │           │
│  │      String username = auth.getName();                                       │           │
│  │      // ★ Works because it reads from ThreadLocal                           │           │
│  │      // ★ Same thread = same SecurityContext = same Authentication          │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  // ── Method 4: Principal parameter (less detailed) ───────────────        │           │
│  │  @GetMapping("/info")                                                        │           │
│  │  public String getInfo(Principal principal) {                                │           │
│  │      String username = principal.getName();                                  │           │
│  │      // ★ Principal is the parent interface of Authentication                │           │
│  │      // ★ Less information than Authentication (no authorities access)      │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ★ ALL 4 methods read from the SAME place: SecurityContextHolder             │           │
│  │  ★ SecurityContextHolder reads from ThreadLocal (NOT HttpSession!)           │           │
│  │  ★ The Authentication was put there by JwtTokenValidationFilter              │           │
│  │  ★ No HttpSession is involved at any point!                                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SessionCreationPolicy OPTIONS — WHAT EACH ONE DOES                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌───────────────────────┬───────────────────────────────────────────────────────┐          │
│  │  Policy                │  Behavior                                            │          │
│  ├───────────────────────┼───────────────────────────────────────────────────────┤          │
│  │  ALWAYS                │  Always creates an HttpSession                       │          │
│  │                        │  Always saves SecurityContext to session             │          │
│  │                        │  ★ Used by: form-based login                        │          │
│  ├───────────────────────┼───────────────────────────────────────────────────────┤          │
│  │  IF_REQUIRED (default) │  Creates HttpSession only if needed                  │          │
│  │                        │  Saves SecurityContext to session if session exists  │          │
│  │                        │  ★ Used by: form-based login (default Spring)       │          │
│  ├───────────────────────┼───────────────────────────────────────────────────────┤          │
│  │  NEVER                 │  Never CREATES an HttpSession                        │          │
│  │                        │  BUT will USE an existing session if one exists      │          │
│  │                        │  ⚠️ NOT truly stateless! Another component might    │          │
│  │                        │  create a session                                    │          │
│  ├───────────────────────┼───────────────────────────────────────────────────────┤          │
│  │  ★ STATELESS          │  Never creates, never uses HttpSession               │          │
│  │                        │  SecurityContext is NEVER saved/loaded from session  │          │
│  │                        │  Every request starts with empty SecurityContext     │          │
│  │                        │  ★ Used by: JWT authentication!                     │          │
│  │                        │  ★ This is what we use!                              │          │
│  └───────────────────────┴───────────────────────────────────────────────────────┘          │
│                                                                                              │
│                                                                                              │
│  ── WHAT HAPPENS INTERNALLY WITH STATELESS POLICY ───────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  With SessionCreationPolicy.STATELESS, Spring Security replaces              │           │
│  │  the default SecurityContextRepository with                                  │           │
│  │  NullSecurityContextRepository:                                              │           │
│  │                                                                               │           │
│  │  // Default (STATEFUL) — saves/loads from HttpSession:                      │           │
│  │  SecurityContextRepository repo = new HttpSessionSecurityContextRepository();│           │
│  │  repo.saveContext(context, request, response);  // saves to session ✅       │           │
│  │  repo.loadContext(request);  // loads from session ✅                        │           │
│  │                                                                               │           │
│  │  // STATELESS — does NOTHING:                                                │           │
│  │  SecurityContextRepository repo = new NullSecurityContextRepository();      │           │
│  │  repo.saveContext(context, request, response);  // does NOTHING! No-op!     │           │
│  │  repo.loadContext(request);  // returns EMPTY SecurityContext every time!    │           │
│  │                                                                               │           │
│  │  ★ This is why every JWT request must re-authenticate!                      │           │
│  │  ★ The SecurityContext is populated by JwtTokenValidationFilter,            │           │
│  │    used during the request, then DISCARDED — never saved anywhere.          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── SUMMARY ─────────────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Form Login (Stateful):                                                      │           │
│  │  Request 1 → authenticate → save to HttpSession                             │           │
│  │  Request 2 → load from HttpSession → already authenticated!                 │           │
│  │  ★ Authentication persists across requests via HttpSession                  │           │
│  │                                                                               │           │
│  │  JWT (Stateless):                                                            │           │
│  │  Request 1 → validate JWT → set SecurityContextHolder → use → clear         │           │
│  │  Request 2 → validate JWT → set SecurityContextHolder → use → clear         │           │
│  │  Request 3 → validate JWT → set SecurityContextHolder → use → clear         │           │
│  │  ★ Every request authenticates fresh! Nothing persists!                     │           │
│  │  ★ SecurityContextHolder (ThreadLocal) is the ONLY transport mechanism     │           │
│  │  ★ It carries auth from filter → authorization → controller within         │           │
│  │    the SAME request, then gets cleared                                      │           │
│  │  ★ HttpSession is never created, never used, never needed!                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```



### 4. Spring Security Architecture — Complete Deep Dive

---

#### 4.1 Maven Dependencies

```xml
<!-- pom.xml -->
<dependencies>

    <!-- Core Spring Security (authentication, authorization, filter chain) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Spring Web (DispatcherServlet, Controllers, Filters) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- OAuth2 Client (Google, GitHub, Keycloak login) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>

    <!-- OAuth2 Resource Server (JWT token validation) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <!-- JJWT (for custom JWT creation/validation) -->
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

    <!-- Spring Data JPA (for JdbcUserDetailsManager / DB-backed users) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Spring Security Test (for testing) -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>

</dependencies>
```

**What `spring-boot-starter-security` brings in:**

```
spring-boot-starter-security
├── spring-security-core          (Core authentication/authorization)
├── spring-security-config        (@EnableWebSecurity, SecurityFilterChain bean)
├── spring-security-web           (Filters, DelegatingFilterProxy)
└── spring-aop                    (Method-level security: @PreAuthorize)
```

---

#### 4.2 High-Level Architecture — Where Security Filter Chain Fits

When you add `spring-boot-starter-security`, Spring Boot auto-configures a **Security Filter Chain** that plugs into the Servlet Container's existing filter chain via a special bridge called `DelegatingFilterProxy`.

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│              WHERE SPRING SECURITY FITS IN THE REQUEST FLOW                          │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  ┌──────────┐                                                                        │
│  │  CLIENT   │  HTTP Request                                                         │
│  └────┬─────┘                                                                        │
│       │                                                                              │
│       ▼                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐    │
│  │                    SERVLET CONTAINER (Tomcat)                                 │    │
│  │                                                                              │    │
│  │  ┌────────────────────────────────────────────────────────────────────────┐  │    │
│  │  │              SERVLET FILTER CHAIN (Container-level)                    │  │    │
│  │  │                                                                        │  │    │
│  │  │  ┌──────────────────┐  ┌──────────────────────┐  ┌─────────────────┐  │  │    │
│  │  │  │ CharacterEncoding│  │ DelegatingFilterProxy │  │ Other Servlet   │  │  │    │
│  │  │  │ Filter           │─►│ (springSecurityFilter │─►│ Filters         │  │  │    │
│  │  │  │                  │  │  Chain)                │  │                 │  │  │    │
│  │  │  └──────────────────┘  └──────────┬───────────┘  └─────────────────┘  │  │    │
│  │  │                                   │                                    │  │    │
│  │  └───────────────────────────────────┼────────────────────────────────────┘  │    │
│  │                                      │                                       │    │
│  │          ┌───────────────────────────┼─────────────────────────────┐         │    │
│  │          │                           ▼                             │         │    │
│  │          │  ┌─────────────────────────────────────────────────┐   │         │    │
│  │          │  │         FilterChainProxy                        │   │         │    │
│  │          │  │    (Manages SecurityFilterChain beans)          │   │         │    │
│  │          │  │                                                 │   │         │    │
│  │          │  │   ┌─ SecurityFilterChain 0 (/api/**)           │   │         │    │
│  │          │  │   │  [Filter1] → [Filter2] → ... → [FilterN]  │   │         │    │
│  │          │  │   │                                             │   │         │    │
│  │          │  │   ┌─ SecurityFilterChain 1 (/admin/**)         │   │         │    │
│  │          │  │   │  [Filter1] → [Filter2] → ... → [FilterN]  │   │         │    │
│  │          │  │   │                                             │   │         │    │
│  │          │  │   ┌─ SecurityFilterChain 2 (/**)  ← fallback  │   │         │    │
│  │          │  │   │  [Filter1] → [Filter2] → ... → [FilterN]  │   │         │    │
│  │          │  │                                                 │   │         │    │
│  │          │  └─────────────────────────────────────────────────┘   │         │    │
│  │          │              SPRING SECURITY LAYER                     │         │    │
│  │          └───────────────────────────┬───────────────────────────┘         │    │
│  │                                      │                                     │    │
│  │                                      ▼                                     │    │
│  │  ┌───────────────────────────────────────────────────────────────────┐    │    │
│  │  │                    DISPATCHER SERVLET                              │    │    │
│  │  │                    (Front Controller)                              │    │    │
│  │  │                          │                                         │    │    │
│  │  │               ┌─────────┼──────────┐                              │    │    │
│  │  │               ▼         ▼          ▼                              │    │    │
│  │  │         HandlerMapping  Interceptors  Controller                  │    │    │
│  │  │                                                                    │    │    │
│  │  └───────────────────────────────────────────────────────────────────┘    │    │
│  │                                                                           │    │
│  └───────────────────────────────────────────────────────────────────────────┘    │
│                                                                                    │
│  KEY COMPONENTS:                                                                   │
│  • DelegatingFilterProxy   — Servlet Filter that bridges to Spring's bean         │
│  • FilterChainProxy        — The Spring bean that holds SecurityFilterChain(s)    │
│  • SecurityFilterChain     — Your @Bean with the list of security filters         │
│                                                                                    │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

**How the bridge works internally:**

```java
// 1. DelegatingFilterProxy (Servlet Container Filter)
// Registered by Spring Boot auto-configuration with name "springSecurityFilterChain"
// It delegates to a Spring bean of the same name → which is FilterChainProxy

public class DelegatingFilterProxy extends GenericFilterBean {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        // Looks up the Spring bean "springSecurityFilterChain" → FilterChainProxy
        Filter delegate = this.applicationContext.getBean("springSecurityFilterChain", Filter.class);
        delegate.doFilter(req, res, chain);
    }
}

// 2. FilterChainProxy (Spring Bean)
// Holds multiple SecurityFilterChain instances and picks the right one based on URL
public class FilterChainProxy extends GenericFilterBean {
    private List<SecurityFilterChain> filterChains;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;

        // Find the FIRST matching SecurityFilterChain
        for (SecurityFilterChain securityFilterChain : this.filterChains) {
            if (securityFilterChain.matches(request)) {
                List<Filter> filters = securityFilterChain.getFilters();
                // Execute all filters in this chain
                VirtualFilterChain virtualChain = new VirtualFilterChain(chain, filters);
                virtualChain.doFilter(req, res);
                return;
            }
        }
        // No match → continue without security
        chain.doFilter(req, res);
    }
}
```

**Configuring multiple SecurityFilterChains:**

```java
@Configuration
@EnableWebSecurity
public class MultiChainSecurityConfig {

    // Chain 1: API endpoints — stateless JWT
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    // Chain 2: Admin panel — form login with sessions
    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form.loginPage("/admin/login").permitAll())
            .sessionManagement(s -> s.maximumSessions(1));
        return http.build();
    }

    // Chain 3: Everything else — default
    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
```

---

#### 4.3 Complete List of Security Filters (In Execution Order)

Each `SecurityFilterChain` contains an ordered list of security filters. Here is the **complete list** of all built-in Spring Security filters in their default execution order:

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│         COMPLETE SPRING SECURITY FILTER CHAIN (All Filters — Execution Order)       │
├────┬─────────────────────────────────────────────┬───────────────────────────────────┤
│ #  │ Filter Name                                  │ Purpose                           │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│  1 │ DisableEncodeUrlFilter                       │ Prevents session ID in URLs       │
│    │                                              │ (security best practice)          │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│  2 │ WebAsyncManagerIntegrationFilter             │ Integrates SecurityContext with   │
│    │                                              │ Spring async (Callable, etc.)     │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│  3 │ SecurityContextHolderFilter                  │ Sets up SecurityContextHolder     │
│    │ (replaces deprecated                        │ for the request, loads any saved  │
│    │  SecurityContextPersistenceFilter)           │ context from SecurityContext-     │
│    │                                              │ Repository                        │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│  4 │ HeaderWriterFilter                           │ Adds security response headers:  │
│    │                                              │ X-Content-Type-Options,          │
│    │                                              │ X-Frame-Options,                 │
│    │                                              │ Strict-Transport-Security, etc.  │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│  5 │ CorsFilter                                   │ Handles CORS preflight and       │
│    │                                              │ adds CORS response headers       │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│  6 │ CsrfFilter                                   │ Validates CSRF tokens on         │
│    │                                              │ state-changing requests           │
│    │                                              │ (POST, PUT, DELETE)              │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│  7 │ LogoutFilter                                 │ Intercepts /logout, invalidates  │
│    │                                              │ session, clears SecurityContext,  │
│    │                                              │ deletes cookies                  │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│  8 │ OAuth2AuthorizationRequestRedirectFilter     │ Redirects user to OAuth2 provider│
│    │                                              │ (Google/GitHub login page)        │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│  9 │ OAuth2LoginAuthenticationFilter              │ Handles OAuth2 callback           │
│    │                                              │ (/login/oauth2/code/*)           │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 10 │ UsernamePasswordAuthenticationFilter         │ Handles form login POST           │
│    │                                              │ (/login with username+password)  │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 11 │ DefaultLoginPageGeneratingFilter             │ Generates default login page     │
│    │                                              │ when no custom login configured  │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 12 │ DefaultLogoutPageGeneratingFilter            │ Generates default logout page    │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 13 │ BasicAuthenticationFilter                    │ Handles HTTP Basic Auth           │
│    │                                              │ (Authorization: Basic base64)    │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 14 │ BearerTokenAuthenticationFilter              │ Handles Bearer token auth         │
│    │                                              │ (Authorization: Bearer jwt)      │
│    │                                              │ Used by OAuth2 Resource Server   │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 15 │ RequestCacheAwareFilter                      │ Restores the original request    │
│    │                                              │ saved before auth redirect       │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 16 │ SecurityContextHolderAwareRequestFilter      │ Wraps request to provide         │
│    │                                              │ request.isUserInRole(), etc.     │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 17 │ AnonymousAuthenticationFilter                │ If no authentication yet, sets   │
│    │                                              │ an AnonymousAuthentication token │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 18 │ SessionManagementFilter                      │ Controls session fixation,       │
│    │                                              │ concurrent sessions, etc.        │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 19 │ ExceptionTranslationFilter                   │ Catches AuthenticationException  │
│    │                                              │ & AccessDeniedException and      │
│    │                                              │ triggers auth entry point or     │
│    │                                              │ access denied handler            │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│ 20 │ AuthorizationFilter                          │ Final check: evaluates           │
│    │ (replaces deprecated FilterSecurity-        │ authorizeHttpRequests() rules    │
│    │  Interceptor)                                │ (permitAll, hasRole, etc.)       │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│    │                                              │                                   │
│    │ ADDITIONAL FILTERS (added for specific      │                                   │
│    │ features when configured):                   │                                   │
│    │                                              │                                   │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│    │ ConcurrentSessionFilter                      │ Checks if session has expired    │
│    │                                              │ due to concurrent session limit  │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│    │ DigestAuthenticationFilter                   │ HTTP Digest authentication       │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│    │ RememberMeAuthenticationFilter               │ Authenticates via remember-me    │
│    │                                              │ cookie (persistent login)        │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│    │ X509AuthenticationFilter                     │ Client certificate auth          │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│    │ SwitchUserFilter                             │ "Login as another user" feature  │
│    │                                              │ (for admin impersonation)        │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│    │ OidcLogoutFilter                             │ Handles OIDC back-channel and    │
│    │                                              │ RP-initiated logout              │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│    │ Saml2WebSsoAuthenticationFilter              │ SAML 2.0 SSO authentication      │
├────┼─────────────────────────────────────────────┼───────────────────────────────────┤
│    │ Saml2LogoutRequestFilter                     │ SAML 2.0 logout handling         │
└────┴─────────────────────────────────────────────┴───────────────────────────────────┘
```

**NOTE:** `SecurityContextPersistenceFilter` is **deprecated** in Spring Security 6.x. It is replaced by `SecurityContextHolderFilter` + explicit `SecurityContextRepository` save. Similarly `FilterSecurityInterceptor` is replaced by `AuthorizationFilter`.

**How to see which filters are active in your app:**

```yaml
# application.yml — enable debug logging
logging:
  level:
    org.springframework.security: DEBUG
    # Or even more detailed:
    org.springframework.security.web.FilterChainProxy: TRACE
```

This prints the ordered list of filters on startup:

```
Security filter chain: [
  DisableEncodeUrlFilter
  WebAsyncManagerIntegrationFilter
  SecurityContextHolderFilter
  HeaderWriterFilter
  CsrfFilter
  LogoutFilter
  UsernamePasswordAuthenticationFilter
  BasicAuthenticationFilter
  RequestCacheAwareFilter
  SecurityContextHolderAwareRequestFilter
  AnonymousAuthenticationFilter
  SessionManagementFilter
  ExceptionTranslationFilter
  AuthorizationFilter
]
```

---

#### 4.4 Authentication Flow — AuthenticationManager → ProviderManager → AuthenticationProvider

This is the **core authentication architecture** of Spring Security.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│                  SPRING SECURITY AUTHENTICATION ARCHITECTURE (Complete)                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────┐   POST /login                                                                 │
│  │  Client   │ ────────────┐                                                                 │
│  └──────────┘              │                                                                 │
│                            ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐                             │
│  │           AUTHENTICATION FILTER                              │                             │
│  │  (e.g., UsernamePasswordAuthenticationFilter)                │                             │
│  │                                                              │                             │
│  │  1. Extracts credentials from request                        │                             │
│  │  2. Creates an UNAUTHENTICATED Authentication token:         │                             │
│  │     UsernamePasswordAuthenticationToken(username, password)  │                             │
│  │     → authenticated = false                                  │                             │
│  │  3. Passes to AuthenticationManager                          │                             │
│  └──────────────────────────┬───────────────────────────────────┘                             │
│                             │                                                                 │
│                             ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐     │
│  │                                                                                     │     │
│  │   AuthenticationManager (Interface)                                                 │     │
│  │   └── authenticate(Authentication auth) : Authentication                            │     │
│  │                                                                                     │     │
│  │   Default Implementation: ProviderManager                                           │     │
│  │                                                                                     │     │
│  │   ┌─────────────────────────────────────────────────────────────────────────────┐   │     │
│  │   │                     ProviderManager                                         │   │     │
│  │   │                                                                             │   │     │
│  │   │   Has a List<AuthenticationProvider> providers                              │   │     │
│  │   │                                                                             │   │     │
│  │   │   for (AuthenticationProvider provider : providers) {                       │   │     │
│  │   │       if (provider.supports(authentication.getClass())) {                   │   │     │
│  │   │           result = provider.authenticate(authentication);                   │   │     │
│  │   │           if (result != null) return result;  // SUCCESS                    │   │     │
│  │   │       }                                                                     │   │     │
│  │   │   }                                                                         │   │     │
│  │   │                                                                             │   │     │
│  │   │   // If no provider handled it, try parent AuthenticationManager            │   │     │
│  │   │   if (parent != null) return parent.authenticate(authentication);           │   │     │
│  │   │                                                                             │   │     │
│  │   │   throw ProviderNotFoundException("No provider found");                     │   │     │
│  │   │                                                                             │   │     │
│  │   └─────────────────────────────────────────────────────────────────────────────┘   │     │
│  │       │                        │                         │                          │     │
│  │       ▼                        ▼                         ▼                          │     │
│  │  ┌────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐            │     │
│  │  │ DaoAuthProvider │  │ JwtAuthProvider       │  │ OAuth2LoginAuthProv  │            │     │
│  │  │                 │  │                       │  │                      │            │     │
│  │  │ supports:       │  │ supports:             │  │ supports:            │            │     │
│  │  │ UsernamePassword│  │ BearerTokenAuth       │  │ OAuth2LoginAuth      │            │     │
│  │  │ AuthToken.class │  │ Token.class           │  │ Token.class          │            │     │
│  │  └────────┬────────┘  └───────────────────────┘  └──────────────────────┘            │     │
│  │           │                                                                          │     │
│  │           │  DaoAuthenticationProvider details:                                      │     │
│  │           │                                                                          │     │
│  │           ▼                                                                          │     │
│  │  ┌──────────────────────────────────────────────────────────┐                       │     │
│  │  │  1. UserDetailsService.loadUserByUsername(username)       │                       │     │
│  │  │     → returns UserDetails (from DB, LDAP, memory, etc.) │                       │     │
│  │  │                                                          │                       │     │
│  │  │  2. PasswordEncoder.matches(rawPassword, encodedPassword)│                       │     │
│  │  │     → returns true/false                                 │                       │     │
│  │  │                                                          │                       │     │
│  │  │  If match → return AUTHENTICATED token                   │                       │     │
│  │  │  If no match → throw BadCredentialsException             │                       │     │
│  │  └──────────────────────────────────────────────────────────┘                       │     │
│  │                                                                                     │     │
│  └─────────────────────────────────────────────────────────────────────────────────────┘     │
│                             │                                                                 │
│                             ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐                  │
│  │  On SUCCESS:                                                            │                  │
│  │  • Returns AUTHENTICATED Authentication object                         │                  │
│  │    (authenticated = true, principal = UserDetails, authorities = roles) │                  │
│  │  • SecurityContextHolder.getContext().setAuthentication(authResult)     │                  │
│  │  • SecurityContextRepository.saveContext() (for session-based apps)     │                  │
│  │  • AuthenticationSuccessHandler called (redirect, return JWT, etc.)     │                  │
│  │                                                                         │                  │
│  │  On FAILURE:                                                            │                  │
│  │  • AuthenticationException thrown                                       │                  │
│  │  • SecurityContextHolder.clearContext()                                 │                  │
│  │  • AuthenticationFailureHandler called (return 401, redirect to login)  │                  │
│  └─────────────────────────────────────────────────────────────────────────┘                  │
│                             │                                                                 │
│                             ▼                                                                 │
│                 Request continues to DispatcherServlet                                        │
│                 → Interceptors → Controller                                                   │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 4.5 Complete List of AuthenticationProvider Implementations

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│              ALL AUTHENTICATION PROVIDER IMPLEMENTATIONS                             │
├──────────────────────────────────────┬───────────────────────────────────────────────┤
│ AuthenticationProvider               │ Purpose                                       │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ DaoAuthenticationProvider            │ Username/password via UserDetailsService +    │
│                                      │ PasswordEncoder. Most common provider.        │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ JwtAuthenticationProvider            │ Validates JWT Bearer tokens using             │
│                                      │ JwtDecoder. Used by OAuth2 Resource Server.   │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ OpaqueTokenAuthenticationProvider    │ Validates opaque tokens via introspection     │
│                                      │ endpoint (OAuth2 token introspection).        │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ OAuth2LoginAuthenticationProvider    │ Handles OAuth2 authorization code exchange    │
│                                      │ (after user returns from Google/GitHub).      │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ OidcAuthorizationCodeAuthentication  │ Like OAuth2Login but also handles OpenID      │
│ Provider                             │ Connect ID tokens and userinfo endpoint.      │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ OAuth2AuthorizationCodeAuthentication│ Pure authorization code grant without OIDC.   │
│ Provider                             │                                               │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ LdapAuthenticationProvider           │ Authenticates against LDAP/Active Directory.  │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ ActiveDirectoryLdapAuthentication    │ Specialized LDAP for Microsoft AD with AD-    │
│ Provider                             │ specific error code handling.                 │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ PreAuthenticatedAuthenticationProv   │ For pre-authenticated scenarios (e.g., X.509  │
│                                      │ client certs, SSO headers from reverse proxy).│
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ RememberMeAuthenticationProvider     │ Authenticates using remember-me cookies.      │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ AnonymousAuthenticationProvider      │ Provides anonymous Authentication for         │
│                                      │ unauthenticated users.                        │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ RunAsImplAuthenticationProvider      │ Authenticates RunAs replacement tokens        │
│                                      │ (used for method-level RunAs).               │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ JaasAuthenticationProvider           │ Delegates to Java Authentication and          │
│                                      │ Authorization Service (JAAS).                │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ TestingAuthenticationProvider        │ For testing purposes only.                    │
├──────────────────────────────────────┼───────────────────────────────────────────────┤
│ Saml2AuthenticationTokenConverter    │ SAML 2.0 response assertion validation.       │
│ (via OpenSaml4AuthenticationProvider)│                                               │
└──────────────────────────────────────┴───────────────────────────────────────────────┘
```

---

#### 4.6 DaoAuthenticationProvider Deep Dive — UserDetailsService + PasswordEncoder

```
┌────────────────────────────────────────────────────────────────────────────────────────────┐
│              DaoAuthenticationProvider — INTERNAL FLOW                                      │
├────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                            │
│  UsernamePasswordAuthenticationToken(username="john", password="secret")                   │
│       │                                                                                    │
│       ▼                                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────────┐          │
│  │  DaoAuthenticationProvider.authenticate()                                    │          │
│  │                                                                              │          │
│  │  Step 1: Load user from UserDetailsService                                   │          │
│  │  ──────────────────────────────────────────                                  │          │
│  │  UserDetails user = userDetailsService.loadUserByUsername("john");            │          │
│  │                                                                              │          │
│  │  UserDetailsService (Interface)                                              │          │
│  │  ├── InMemoryUserDetailsManager    (stores users in HashMap — dev/testing)   │          │
│  │  ├── JdbcUserDetailsManager        (queries DB via JdbcTemplate — built-in)  │          │
│  │  ├── LdapUserDetailsManager        (queries LDAP directory)                  │          │
│  │  ├── CachingUserDetailsService     (wraps another UDS with cache)            │          │
│  │  └── CustomUserDetailsService      (YOUR implementation — most common)       │          │
│  │                                                                              │          │
│  │  Step 2: Pre-authentication checks                                           │          │
│  │  ─────────────────────────────────                                           │          │
│  │  • isAccountNonLocked()    → throw LockedException                           │          │
│  │  • isEnabled()             → throw DisabledException                          │          │
│  │  • isAccountNonExpired()   → throw AccountExpiredException                   │          │
│  │                                                                              │          │
│  │  Step 3: Password verification                                               │          │
│  │  ─────────────────────────                                                   │          │
│  │  boolean matches = passwordEncoder.matches("secret", user.getPassword());    │          │
│  │  if (!matches) throw BadCredentialsException("Bad credentials");             │          │
│  │                                                                              │          │
│  │  PasswordEncoder (Interface)                                                 │          │
│  │  ├── BCryptPasswordEncoder          (recommended — uses BCrypt hash)         │          │
│  │  ├── Argon2PasswordEncoder          (newer, memory-hard — very secure)       │          │
│  │  ├── SCryptPasswordEncoder          (CPU + memory hard)                      │          │
│  │  ├── Pbkdf2PasswordEncoder          (PBKDF2 hash)                           │          │
│  │  ├── DelegatingPasswordEncoder     (supports multiple encoders via {id})     │          │
│  │  └── NoOpPasswordEncoder           (plain text — NEVER in production!)       │          │
│  │                                                                              │          │
│  │  Step 4: Post-authentication checks                                          │          │
│  │  ──────────────────────────────────                                          │          │
│  │  • isCredentialsNonExpired() → throw CredentialsExpiredException             │          │
│  │                                                                              │          │
│  │  Step 5: Create AUTHENTICATED token                                          │          │
│  │  ──────────────────────────────────                                          │          │
│  │  return new UsernamePasswordAuthenticationToken(                              │          │
│  │      userDetails,           // principal                                     │          │
│  │      null,                  // credentials (erased for security)             │          │
│  │      userDetails.getAuthorities()  // ROLE_USER, ROLE_ADMIN, etc.           │          │
│  │  );  // authenticated = true                                                 │          │
│  │                                                                              │          │
│  └──────────────────────────────────────────────────────────────────────────────┘          │
│                                                                                            │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

**UserDetailsService Implementations — Code:**

**1. InMemoryUserDetailsManager (for dev/testing)**

```java
@Configuration
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN", "USER")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

**2. JdbcUserDetailsManager (built-in DB schema)**

```java
@Configuration
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);

        // Uses default schema: users(username, password, enabled)
        //                       authorities(username, authority)
        // You can customize queries:
        manager.setUsersByUsernameQuery(
            "SELECT username, password, enabled FROM app_users WHERE username = ?");
        manager.setAuthoritiesByUsernameQuery(
            "SELECT username, authority FROM app_authorities WHERE username = ?");

        return manager;
    }
}
```

```sql
-- Default Spring Security schema
CREATE TABLE users (
    username VARCHAR(50)  NOT NULL PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE authorities (
    username  VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users(username)
);
CREATE UNIQUE INDEX ix_auth_username ON authorities (username, authority);
```

**3. Custom UserDetailsService (most common in production)**

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                !user.isLocked(),  // accountNonLocked
                user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList())
        );
    }
}
```

**DelegatingPasswordEncoder (supports migration between encoders):**

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // Creates encoder that reads the {id} prefix to determine which encoder to use
    // Stored passwords look like: {bcrypt}$2a$12$...  or {argon2}$argon2id$...
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    // Default encoding = bcrypt
    // Supports: bcrypt, argon2, scrypt, pbkdf2, sha256, ldap, MD5, noop
}
```

---

#### 4.7 SecurityContext — Where Authentication is Stored

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                  SECURITY CONTEXT STORAGE                                      │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│  SecurityContextHolder                                                         │
│  ├── Stores SecurityContext using a strategy:                                 │
│  │   ├── MODE_THREADLOCAL (default) — per-thread storage                     │
│  │   ├── MODE_INHERITABLETHREADLOCAL — inherited by child threads            │
│  │   └── MODE_GLOBAL — single context for entire JVM (rare)                  │
│  │                                                                            │
│  └── SecurityContext                                                          │
│      └── Authentication                                                       │
│          ├── principal      → UserDetails (who the user is)                  │
│          ├── credentials    → usually null after auth (erased)               │
│          ├── authorities    → Collection<GrantedAuthority> (roles/perms)     │
│          ├── authenticated  → true/false                                     │
│          └── details        → WebAuthenticationDetails (IP, session ID)      │
│                                                                                │
│                                                                                │
│  Access in your code:                                                          │
│                                                                                │
│  // Option 1: Static access                                                    │
│  Authentication auth = SecurityContextHolder.getContext().getAuthentication();  │
│  String username = auth.getName();                                             │
│  Collection<? extends GrantedAuthority> roles = auth.getAuthorities();         │
│                                                                                │
│  // Option 2: Method parameter injection                                       │
│  @GetMapping("/me")                                                            │
│  public UserInfo getCurrentUser(@AuthenticationPrincipal UserDetails user) {   │
│      return new UserInfo(user.getUsername(), user.getAuthorities());            │
│  }                                                                             │
│                                                                                │
│  // Option 3: In SpEL expressions                                              │
│  @PreAuthorize("authentication.principal.username == #username")               │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
```

**SecurityContextRepository (how context persists across requests):**

```java
// For session-based apps (stateful):
// HttpSessionSecurityContextRepository saves SecurityContext in the HTTP session

// For stateless apps (JWT):
// NullSecurityContextRepository — nothing is saved between requests
// Each request must re-authenticate (via JWT filter)

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Stateless — no SecurityContext persistence
        .securityContext(context -> context
            .securityContextRepository(new RequestAttributeSecurityContextRepository())
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
    return http.build();
}
```

---

#### 4.8 Complete End-to-End Flow — From Request to Controller

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│   COMPLETE FLOW: POST /api/login {username: "john", password: "secret"}                 │
├──────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌──────────┐                                                                            │
│  │  CLIENT   │  POST /api/login                                                          │
│  │           │  Content-Type: application/json                                            │
│  │           │  {"username": "john", "password": "secret"}                               │
│  └────┬─────┘                                                                            │
│       │                                                                                  │
│       │  STEP 1: Tomcat receives request                                                 │
│       │  ─────────────────────────────                                                   │
│       ▼                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐                    │
│  │  SERVLET CONTAINER (Tomcat)                                      │                    │
│  │  • Assigns thread from pool                                      │                    │
│  │  • Creates HttpServletRequest + HttpServletResponse              │                    │
│  └──────────────────────────────┬───────────────────────────────────┘                    │
│                                 │                                                        │
│       │  STEP 2: Servlet Filter Chain                                                    │
│       ▼                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐                    │
│  │  CharacterEncodingFilter → sets UTF-8                            │                    │
│  │  DelegatingFilterProxy → delegates to FilterChainProxy           │                    │
│  └──────────────────────────────┬───────────────────────────────────┘                    │
│                                 │                                                        │
│       │  STEP 3: Spring Security Filter Chain                                            │
│       ▼                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐                    │
│  │  SecurityContextHolderFilter                                     │                    │
│  │  → Loads empty SecurityContext (new request)                     │                    │
│  │                                                                  │                    │
│  │  HeaderWriterFilter                                              │                    │
│  │  → Prepares security headers for response                       │                    │
│  │                                                                  │                    │
│  │  CorsFilter → handles CORS if cross-origin                      │                    │
│  │                                                                  │                    │
│  │  CsrfFilter → skipped (CSRF disabled for stateless API)         │                    │
│  │                                                                  │                    │
│  │  LogoutFilter → not /logout, skipped                             │                    │
│  │                                                                  │                    │
│  │  UsernamePasswordAuthenticationFilter                            │                    │
│  │  → Not /login form POST, skipped (we use custom controller)     │                    │
│  │                                                                  │                    │
│  │  JwtAuthenticationFilter (our custom filter)                     │                    │
│  │  → No Bearer token in header, skipped                           │                    │
│  │                                                                  │                    │
│  │  AnonymousAuthenticationFilter                                   │                    │
│  │  → Sets AnonymousAuthenticationToken                             │                    │
│  │                                                                  │                    │
│  │  ExceptionTranslationFilter → wraps downstream exceptions       │                    │
│  │                                                                  │                    │
│  │  AuthorizationFilter                                             │                    │
│  │  → /api/login is permitAll() → ALLOWED                          │                    │
│  └──────────────────────────────┬───────────────────────────────────┘                    │
│                                 │                                                        │
│       │  STEP 4: DispatcherServlet                                                       │
│       ▼                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐                    │
│  │  DispatcherServlet.doDispatch()                                  │                    │
│  │  • HandlerMapping → finds AuthController.login()                 │                    │
│  │  • HandlerAdapter → prepares to invoke method                    │                    │
│  └──────────────────────────────┬───────────────────────────────────┘                    │
│                                 │                                                        │
│       │  STEP 5: Interceptors                                                            │
│       ▼                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐                    │
│  │  Interceptor.preHandle() → logging, rate limiting                │                    │
│  └──────────────────────────────┬───────────────────────────────────┘                    │
│                                 │                                                        │
│       │  STEP 6: Controller                                                              │
│       ▼                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐                    │
│  │  AuthController.login(LoginRequest)                              │                    │
│  │  │                                                               │                    │
│  │  │  // Trigger Spring Security authentication                    │                    │
│  │  │  authenticationManager.authenticate(                          │                    │
│  │  │      new UsernamePasswordAuthenticationToken(                 │                    │
│  │  │          "john", "secret")                                    │                    │
│  │  │  );                                                           │                    │
│  │  │       │                                                       │                    │
│  │  │       ▼                                                       │                    │
│  │  │  ProviderManager                                              │                    │
│  │  │  → iterates providers                                         │                    │
│  │  │  → DaoAuthenticationProvider.supports(UsernamePassword...) ✓  │                    │
│  │  │       │                                                       │                    │
│  │  │       ▼                                                       │                    │
│  │  │  DaoAuthenticationProvider.authenticate()                     │                    │
│  │  │  → userDetailsService.loadUserByUsername("john")              │                    │
│  │  │  → finds User(john, $2a$12$..., ROLE_USER)                   │                    │
│  │  │  → passwordEncoder.matches("secret", "$2a$12$...") → true ✓  │                    │
│  │  │  → returns AUTHENTICATED token                                │                    │
│  │  │       │                                                       │                    │
│  │  │       ▼                                                       │                    │
│  │  │  String jwt = jwtService.generateToken(userDetails);          │                    │
│  │  │  return ResponseEntity.ok(new AuthResponse(jwt));             │                    │
│  │  │                                                               │                    │
│  └──────────────────────────────┬───────────────────────────────────┘                    │
│                                 │                                                        │
│       │  STEP 7: Response flows back                                                     │
│       ▼                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────┐                    │
│  │  Interceptor.postHandle()                                        │                    │
│  │  Interceptor.afterCompletion()                                   │                    │
│  │  MappingJackson2HttpMessageConverter → serializes to JSON        │                    │
│  │  Filter Chain (reverse) → adds security headers                  │                    │
│  │  Tomcat → sends HTTP response                                    │                    │
│  └──────────────────────────────────────────────────────────────────┘                    │
│                                 │                                                        │
│       │  STEP 8: Client receives response                                                │
│       ▼                                                                                  │
│  ┌──────────┐                                                                            │
│  │  CLIENT   │  HTTP/1.1 200 OK                                                          │
│  │           │  {"token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIi..."}                │
│  └──────────┘                                                                            │
│                                                                                          │
│  ════════════════════════════════════════════════════════════════════                     │
│  SUBSEQUENT AUTHENTICATED REQUEST:  GET /api/users                                       │
│  ════════════════════════════════════════════════════════════════════                     │
│                                                                                          │
│  ┌──────────┐  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...                            │
│  │  CLIENT   │ ──────────────────────┐                                                   │
│  └──────────┘                        │                                                   │
│                                      ▼                                                   │
│  Security Filter Chain:                                                                  │
│  │                                                                                       │
│  │  JwtAuthenticationFilter (our custom filter)                                          │
│  │  → Extracts JWT from "Authorization: Bearer ..." header                               │
│  │  → jwtService.extractUsername(jwt) → "john"                                           │
│  │  → userDetailsService.loadUserByUsername("john") → UserDetails                        │
│  │  → jwtService.isTokenValid(jwt, userDetails) → true ✓                                │
│  │  → Creates UsernamePasswordAuthenticationToken (authenticated)                        │
│  │  → SecurityContextHolder.getContext().setAuthentication(token)                        │
│  │                                                                                       │
│  │  AuthorizationFilter                                                                  │
│  │  → /api/users requires ROLE_USER → user has ROLE_USER → ALLOWED ✓                   │
│  │                                                                                       │
│  │  DispatcherServlet → Interceptors → UserController.getAllUsers()                      │
│  │  → Returns user data                                                                  │
│  │                                                                                       │
│  │  SecurityContextHolder.clearContext()  (stateless — cleared after request)             │
│  │                                                                                       │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 4.9 Internal Source Code — Key Classes

**AuthenticationManager Interface:**

```java
// org.springframework.security.authentication.AuthenticationManager
public interface AuthenticationManager {
    // The ONE method — takes unauthenticated token, returns authenticated token
    // or throws AuthenticationException
    Authentication authenticate(Authentication authentication) throws AuthenticationException;
}
```

**ProviderManager (Default Implementation):**

```java
// org.springframework.security.authentication.ProviderManager (simplified)
public class ProviderManager implements AuthenticationManager {

    private List<AuthenticationProvider> providers;
    private AuthenticationManager parent;  // fallback parent manager

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        AuthenticationException lastException = null;

        for (AuthenticationProvider provider : providers) {
            // Check if this provider can handle this type of token
            if (!provider.supports(authentication.getClass())) {
                continue;
            }

            try {
                // Attempt authentication
                Authentication result = provider.authenticate(authentication);
                if (result != null) {
                    // Erase credentials from the result for security
                    if (result instanceof CredentialsContainer) {
                        ((CredentialsContainer) result).eraseCredentials();
                    }
                    return result;  // SUCCESS!
                }
            } catch (AuthenticationException ex) {
                lastException = ex;
            }
        }

        // No provider succeeded — try parent
        if (parent != null) {
            try {
                return parent.authenticate(authentication);
            } catch (AuthenticationException ex) {
                lastException = ex;
            }
        }

        throw lastException;  // All providers failed
    }
}
```

**AuthenticationProvider Interface:**

```java
// org.springframework.security.authentication.AuthenticationProvider
public interface AuthenticationProvider {

    // Perform authentication — return authenticated token or throw exception
    Authentication authenticate(Authentication authentication) throws AuthenticationException;

    // Does this provider support the given Authentication token type?
    boolean supports(Class<?> authentication);
}
```

**DaoAuthenticationProvider (simplified internal code):**

```java
// org.springframework.security.authentication.dao.DaoAuthenticationProvider (simplified)
public class DaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    private UserDetailsService userDetailsService;
    private PasswordEncoder passwordEncoder;

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected UserDetails retrieveUser(String username,
            UsernamePasswordAuthenticationToken authentication) {
        // Step 1: Load user
        UserDetails loadedUser = userDetailsService.loadUserByUsername(username);
        if (loadedUser == null) {
            throw new InternalAuthenticationServiceException(
                "UserDetailsService returned null");
        }
        return loadedUser;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication) {
        // Step 2: Verify password
        String presentedPassword = authentication.getCredentials().toString();

        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
    }

    // AbstractUserDetailsAuthenticationProvider.authenticate() orchestrates:
    // 1. retrieveUser()         — load from DB
    // 2. preAuthenticationChecks  — locked? disabled? expired?
    // 3. additionalAuthenticationChecks() — password match
    // 4. postAuthenticationChecks — credentials expired?
    // 5. createSuccessAuthentication() — return authenticated token
}
```

**UsernamePasswordAuthenticationFilter (simplified):**

```java
// What happens when user submits login form
public class UsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    // Default: processes POST /login
    public UsernamePasswordAuthenticationFilter() {
        super(new AntPathRequestMatcher("/login", "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException {

        // Extract credentials from form parameters
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Create unauthenticated token
        UsernamePasswordAuthenticationToken authRequest =
            UsernamePasswordAuthenticationToken.unauthenticated(username, password);

        // Delegate to AuthenticationManager (→ ProviderManager → Providers)
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}

// AbstractAuthenticationProcessingFilter handles:
// On SUCCESS → SecurityContextHolder.setAuthentication(result)
//            → AuthenticationSuccessHandler.onAuthenticationSuccess()
// On FAILURE → SecurityContextHolder.clearContext()
//            → AuthenticationFailureHandler.onAuthenticationFailure()
```

**Authentication Object (the token that flows through the system):**

```java
// org.springframework.security.authentication.UsernamePasswordAuthenticationToken

// BEFORE authentication (unauthenticated):
UsernamePasswordAuthenticationToken token =
    UsernamePasswordAuthenticationToken.unauthenticated("john", "secret");
// token.isAuthenticated() → false
// token.getPrincipal()    → "john" (just the username string)
// token.getCredentials()  → "secret"
// token.getAuthorities()  → empty

// AFTER authentication (authenticated):
UsernamePasswordAuthenticationToken token =
    UsernamePasswordAuthenticationToken.authenticated(
        userDetails,                    // principal (UserDetails object)
        null,                           // credentials (erased)
        userDetails.getAuthorities()    // [ROLE_USER, ROLE_ADMIN]
    );
// token.isAuthenticated() → true
// token.getPrincipal()    → UserDetails object
// token.getCredentials()  → null (erased for security)
// token.getAuthorities()  → [ROLE_USER, ROLE_ADMIN]
```

---

#### 4.10 Industry Use Case: E-commerce with Multiple Auth Methods

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  E-COMMERCE PLATFORM — MULTIPLE AUTH PROVIDERS                                       │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  SecurityFilterChain 1: /api/**  (REST API — JWT)                                   │
│  ┌──────────────────────────────────────────────────────────────────────────────┐    │
│  │  Filters: SecurityContextHolder → CORS → JwtAuthFilter → AuthorizationFilter│    │
│  │                                                                              │    │
│  │  Mobile App ──► Bearer Token ──► JwtAuthenticationProvider ──► validated ✓   │    │
│  └──────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                      │
│  SecurityFilterChain 2: /admin/**  (Admin Panel — Form Login + MFA)                 │
│  ┌──────────────────────────────────────────────────────────────────────────────┐    │
│  │  Filters: SecurityContextHolder → CSRF → UsernamePasswordFilter → Session   │    │
│  │                                                                              │    │
│  │  Admin ──► Form Login ──► DaoAuthenticationProvider ──► authenticated ✓      │    │
│  │                           (UserDetailsService + BCrypt)                       │    │
│  └──────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                      │
│  SecurityFilterChain 3: /oauth2/**  (Social Login — OAuth2)                         │
│  ┌──────────────────────────────────────────────────────────────────────────────┐    │
│  │  Filters: OAuth2AuthorizationRequestRedirect → OAuth2LoginFilter            │    │
│  │                                                                              │    │
│  │  Customer ──► "Login with Google" ──► OAuth2LoginAuthenticationProvider      │    │
│  │           ──► Google Auth Server ──► callback ──► user profile ──► JWT ✓     │    │
│  └──────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                      │
│  ProviderManager:                                                                    │
│  ├── DaoAuthenticationProvider     (admin login)                                    │
│  ├── JwtAuthenticationProvider     (API authentication)                              │
│  ├── OAuth2LoginAuthProvider       (social login)                                   │
│  └── RememberMeAuthProvider        (persistent login cookie)                        │
│                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class EcommerceSecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    // REST API chain — JWT-based, stateless
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authenticationProvider(daoAuthProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }

    // Admin panel — form login with sessions
    @Bean
    @Order(2)
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login").permitAll()
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .defaultSuccessUrl("/admin/dashboard")
            )
            .sessionManagement(s -> s.maximumSessions(1))
            .rememberMe(r -> r.tokenValiditySeconds(86400));
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```



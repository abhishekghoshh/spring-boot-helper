### 15. Form Login Defaults, SessionManagement, SessionCreationPolicy & Why Custom Session Management Is Overhead

---

#### 15.1 Form Login Is ON By Default — SpringBootWebSecurityConfiguration Internals

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SPRING BOOT AUTO-CONFIGURATION — DEFAULT SECURITY FILTER CHAIN                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  When you add spring-boot-starter-security to your project, Spring Boot                     │
│  AUTO-CONFIGURES a SecurityFilterChain bean for you. You don't need to write                │
│  ANY security configuration at all — everything works out of the box.                       │
│                                                                                              │
│                                                                                              │
│  ── WHERE DOES THIS DEFAULT COME FROM? ──────────────────────────────────────               │
│                                                                                              │
│  Class: org.springframework.boot.autoconfigure.security.servlet                              │
│         .SpringBootWebSecurityConfiguration                                                  │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  @Configuration(proxyBeanMethods = false)                                     │           │
│  │  @ConditionalOnDefaultWebSecurity                                             │           │
│  │  // ↑ This condition is TRUE only when:                                      │           │
│  │  //   1. Spring Security is on the classpath                                 │           │
│  │  //   2. You have NOT defined your own SecurityFilterChain bean              │           │
│  │  //   As soon as you define your own, this class BACKS OFF!                  │           │
│  │                                                                               │           │
│  │  static class SecurityFilterChainConfiguration {                             │           │
│  │                                                                               │           │
│  │      @Bean                                                                    │           │
│  │      @Order(SecurityProperties.BASIC_AUTH_ORDER)                             │           │
│  │      SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)        │           │
│  │              throws Exception {                                               │           │
│  │                                                                               │           │
│  │          http                                                                 │           │
│  │              .authorizeHttpRequests(                                           │           │
│  │                  (requests) -> requests.anyRequest().authenticated()          │           │
│  │                  // ↑ ALL endpoints require authentication                   │           │
│  │                  // ↑ No role-based restrictions — just "must be logged in"  │           │
│  │              )                                                                │           │
│  │              .formLogin(withDefaults())                                        │           │
│  │              // ↑ Enables form-based login with ALL defaults:                │           │
│  │              //   • Login page at GET /login (auto-generated HTML page)      │           │
│  │              //   • Login processing at POST /login                          │           │
│  │              //   • Default success URL: / (root)                            │           │
│  │              //   • Default failure URL: /login?error                        │           │
│  │              //   • Logout at POST /logout                                   │           │
│  │              //   • UsernamePasswordAuthenticationFilter registered          │           │
│  │              //   • DefaultLoginPageGeneratingFilter registered              │           │
│  │              //   • DefaultLogoutPageGeneratingFilter registered             │           │
│  │                                                                               │           │
│  │              .httpBasic(withDefaults());                                       │           │
│  │              // ↑ Enables HTTP Basic authentication with defaults:           │           │
│  │              //   • BasicAuthenticationFilter registered                     │           │
│  │              //   • Realm: "Spring Security Application"                     │           │
│  │              //   • 401 WWW-Authenticate header on failure                   │           │
│  │                                                                               │           │
│  │          return http.build();                                                 │           │
│  │      }                                                                        │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHAT THIS DEFAULT GIVES YOU ─────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. ALL endpoints require authentication                                     │           │
│  │     → Cannot access ANY URL without logging in                               │           │
│  │                                                                               │           │
│  │  2. Form Login with auto-generated login page                                │           │
│  │     → Visit any URL → redirected to /login                                  │           │
│  │     → Spring generates an HTML login page automatically                      │           │
│  │     → Submit username/password via POST /login                               │           │
│  │                                                                               │           │
│  │  3. HTTP Basic authentication also enabled                                   │           │
│  │     → Can send credentials via Authorization: Basic header                  │           │
│  │     → Useful for REST API testing (curl, Postman)                           │           │
│  │                                                                               │           │
│  │  4. CSRF protection enabled                                                  │           │
│  │     → POST/PUT/DELETE requests require CSRF token                           │           │
│  │                                                                               │           │
│  │  5. Session management with default settings                                 │           │
│  │     → SessionCreationPolicy.IF_REQUIRED (default)                           │           │
│  │     → Session created when needed (after successful authentication)         │           │
│  │                                                                               │           │
│  │  6. Logout endpoint at POST /logout                                          │           │
│  │     → Invalidates session, clears SecurityContext, redirects to /login?logout│           │
│  │                                                                               │           │
│  │  7. A random password is generated and printed to console:                   │           │
│  │     "Using generated security password: a1b2c3d4-e5f6-..."                  │           │
│  │     → Default username: "user"                                               │           │
│  │     → Override in application.properties:                                    │           │
│  │        spring.security.user.name=admin                                       │           │
│  │        spring.security.user.password=secret                                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── VISUAL: WHAT HAPPENS ON FIRST REQUEST ───────────────────────────────────               │
│                                                                                              │
│  Browser: GET /dashboard                                                                    │
│       │                                                                                      │
│       ▼                                                                                      │
│  SecurityFilterChain (default)                                                               │
│       │                                                                                      │
│       ├── SecurityContextHolderFilter: no session → empty SecurityContext                   │
│       ├── UsernamePasswordAuthenticationFilter: SKIPPED (not POST /login)                   │
│       ├── AnonymousAuthenticationFilter: sets AnonymousAuthenticationToken                  │
│       ├── AuthorizationFilter: .anyRequest().authenticated()                                │
│       │      → User is anonymous, NOT authenticated                                        │
│       │      → AccessDeniedException                                                        │
│       │                                                                                      │
│       ├── ExceptionTranslationFilter catches it:                                            │
│       │      → User is anonymous → redirect to login page                                  │
│       │      → Save original URL (/dashboard) to RequestCache                              │
│       │                                                                                      │
│       ▼                                                                                      │
│  HTTP 302 Found                                                                              │
│  Location: /login                                                                            │
│       │                                                                                      │
│       ▼                                                                                      │
│  Browser: GET /login                                                                        │
│       │                                                                                      │
│       ▼                                                                                      │
│  DefaultLoginPageGeneratingFilter:                                                           │
│  → Generates an HTML login page with username/password fields                               │
│  → Returns 200 OK with the auto-generated HTML form                                        │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 15.2 @ConditionalOnDefaultWebSecurity — When the Default Backs Off

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  CONDITIONAL ACTIVATION — WHEN DOES THE DEFAULT APPLY?                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  @ConditionalOnDefaultWebSecurity resolves to TRUE only when BOTH conditions met:           │
│                                                                                              │
│  1. Spring Security is on the classpath (spring-boot-starter-security)                      │
│  2. No SecurityFilterChain bean is defined by the developer                                 │
│                                                                                              │
│  As soon as you define your own SecurityFilterChain:                                        │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  @Configuration                                                               │           │
│  │  @EnableWebSecurity                                                           │           │
│  │  public class SecurityConfig {                                                │           │
│  │                                                                               │           │
│  │      @Bean                                                                    │           │
│  │      SecurityFilterChain filterChain(HttpSecurity http) throws Exception {   │           │
│  │          // YOUR custom configuration here                                   │           │
│  │          // ...                                                               │           │
│  │          return http.build();                                                 │           │
│  │      }                                                                        │           │
│  │  }                                                                            │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  → Spring Boot's SpringBootWebSecurityConfiguration BACKS OFF completely!                   │
│  → The default SecurityFilterChain bean is NOT created.                                     │
│  → ONLY your SecurityFilterChain is used.                                                   │
│                                                                                              │
│  ⚠️ IMPORTANT: When you define your own SecurityFilterChain, you lose ALL                   │
│     defaults! If you want form login or HTTP basic, you must explicitly                     │
│     configure them in your own bean. Nothing is "inherited" from the default.               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  // Common mistake — this config has NO form login, NO basic auth!          │           │
│  │  // Because you replaced the defaults and didn't re-add them.               │           │
│  │                                                                               │           │
│  │  @Bean                                                                        │           │
│  │  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {       │           │
│  │      http                                                                     │           │
│  │          .authorizeHttpRequests(auth -> auth                                  │           │
│  │              .requestMatchers("/admin/**").hasRole("ADMIN")                   │           │
│  │              .anyRequest().authenticated()                                    │           │
│  │          );                                                                   │           │
│  │      // ⚠️ NO .formLogin() → no login page! Users get 403 instead of       │           │
│  │      //    being redirected to a login form.                                 │           │
│  │      // ⚠️ NO .httpBasic() → no Basic auth support!                         │           │
│  │      return http.build();                                                     │           │
│  │  }                                                                            │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  // Correct — explicitly re-add what you need                               │           │
│  │                                                                               │           │
│  │  @Bean                                                                        │           │
│  │  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {       │           │
│  │      http                                                                     │           │
│  │          .authorizeHttpRequests(auth -> auth                                  │           │
│  │              .requestMatchers("/admin/**").hasRole("ADMIN")                   │           │
│  │              .anyRequest().authenticated()                                    │           │
│  │          )                                                                    │           │
│  │          .formLogin(Customizer.withDefaults())   // ★ Re-add form login     │           │
│  │          .httpBasic(Customizer.withDefaults());   // ★ Re-add basic auth    │           │
│  │      return http.build();                                                     │           │
│  │  }                                                                            │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 15.3 Spring Session — Automatic Session Table Creation in Database

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SPRING SESSION — AUTO-CREATED DATABASE TABLES                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  When you use Spring Session with JDBC (spring-session-jdbc), Spring                        │
│  automatically creates two database tables to store session data:                            │
│                                                                                              │
│                                                                                              │
│  ── DEPENDENCY ──────────────────────────────────────────────────────────────               │
│                                                                                              │
│  <!-- pom.xml -->                                                                            │
│  <dependency>                                                                                │
│      <groupId>org.springframework.session</groupId>                                         │
│      <artifactId>spring-session-jdbc</artifactId>                                           │
│  </dependency>                                                                               │
│                                                                                              │
│  # application.properties                                                                    │
│  spring.session.store-type=jdbc                                                              │
│  # Spring Boot auto-creates the session tables (DDL script)                                 │
│  spring.session.jdbc.initialize-schema=always                                                │
│                                                                                              │
│                                                                                              │
│  ── TABLE 1: SPRING_SESSION ─────────────────────────────────────────────────               │
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐            │
│  │  CREATE TABLE SPRING_SESSION (                                              │            │
│  │      PRIMARY_ID            CHAR(36) NOT NULL,    -- UUID session ID        │            │
│  │      SESSION_ID            CHAR(36) NOT NULL,    -- Maps to JSESSIONID     │            │
│  │      CREATION_TIME         BIGINT NOT NULL,      -- Epoch millis           │            │
│  │      LAST_ACCESS_TIME      BIGINT NOT NULL,      -- Last activity time     │            │
│  │      MAX_INACTIVE_INTERVAL INT NOT NULL,          -- Timeout in seconds    │            │
│  │      EXPIRY_TIME           BIGINT NOT NULL,       -- When session expires  │            │
│  │      PRINCIPAL_NAME        VARCHAR(100),           -- Username (if set)    │            │
│  │      CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)                 │            │
│  │  );                                                                         │            │
│  │                                                                              │            │
│  │  -- Index for fast session lookup by SESSION_ID (JSESSIONID cookie value)  │            │
│  │  CREATE UNIQUE INDEX SPRING_SESSION_IX1                                     │            │
│  │      ON SPRING_SESSION (SESSION_ID);                                        │            │
│  │                                                                              │            │
│  │  -- Index for cleanup of expired sessions                                   │            │
│  │  CREATE INDEX SPRING_SESSION_IX2                                            │            │
│  │      ON SPRING_SESSION (EXPIRY_TIME);                                       │            │
│  │                                                                              │            │
│  │  -- Index for finding sessions by username                                  │            │
│  │  CREATE INDEX SPRING_SESSION_IX3                                            │            │
│  │      ON SPRING_SESSION (PRINCIPAL_NAME);                                    │            │
│  └─────────────────────────────────────────────────────────────────────────────┘            │
│                                                                                              │
│                                                                                              │
│  ── TABLE 2: SPRING_SESSION_ATTRIBUTES ──────────────────────────────────────               │
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐            │
│  │  CREATE TABLE SPRING_SESSION_ATTRIBUTES (                                   │            │
│  │      SESSION_PRIMARY_ID  CHAR(36) NOT NULL,  -- FK to SPRING_SESSION       │            │
│  │      ATTRIBUTE_NAME      VARCHAR(200) NOT NULL,                             │            │
│  │      ATTRIBUTE_BYTES     BYTEA NOT NULL,     -- Serialized Java object     │            │
│  │                                                                              │            │
│  │      CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK                                │            │
│  │          PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),                  │            │
│  │      CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK                                │            │
│  │          FOREIGN KEY (SESSION_PRIMARY_ID)                                   │            │
│  │          REFERENCES SPRING_SESSION(PRIMARY_ID)                              │            │
│  │          ON DELETE CASCADE                                                   │            │
│  │  );                                                                          │            │
│  │                                                                              │            │
│  │  -- This table stores the session attributes, including:                    │            │
│  │  -- • "SPRING_SECURITY_CONTEXT" → the serialized SecurityContext            │            │
│  │  -- • Any custom attributes you set: session.setAttribute("cart", cart)     │            │
│  │  -- The SecurityContext (with Authentication object) is serialized          │            │
│  │  -- using Java serialization and stored as raw bytes.                       │            │
│  └─────────────────────────────────────────────────────────────────────────────┘            │
│                                                                                              │
│                                                                                              │
│  ── HOW SESSION FLOWS WITH JDBC STORAGE ─────────────────────────────────────               │
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐            │
│  │                                                                              │            │
│  │  Login (POST /login)                                                         │            │
│  │       │                                                                      │            │
│  │       ▼                                                                      │            │
│  │  Authentication succeeds                                                     │            │
│  │       │                                                                      │            │
│  │       ▼                                                                      │            │
│  │  Spring Session creates a new session:                                       │            │
│  │  → INSERT INTO SPRING_SESSION (PRIMARY_ID, SESSION_ID, ...)                 │            │
│  │  → INSERT INTO SPRING_SESSION_ATTRIBUTES                                    │            │
│  │    (SESSION_PRIMARY_ID, "SPRING_SECURITY_CONTEXT", <serialized bytes>)      │            │
│  │       │                                                                      │            │
│  │       ▼                                                                      │            │
│  │  Set-Cookie: SESSION=<SESSION_ID>                                            │            │
│  │  (Note: Spring Session uses "SESSION" cookie, not "JSESSIONID")             │            │
│  │                                                                              │            │
│  │                                                                              │            │
│  │  Subsequent Request (GET /api/orders)                                        │            │
│  │       │                                                                      │            │
│  │       ▼                                                                      │            │
│  │  Cookie: SESSION=abc123                                                      │            │
│  │       │                                                                      │            │
│  │       ▼                                                                      │            │
│  │  Spring Session intercepts:                                                  │            │
│  │  → SELECT * FROM SPRING_SESSION WHERE SESSION_ID = 'abc123'                 │            │
│  │  → SELECT * FROM SPRING_SESSION_ATTRIBUTES                                  │            │
│  │    WHERE SESSION_PRIMARY_ID = <primary_id>                                  │            │
│  │  → Deserialize SPRING_SECURITY_CONTEXT → SecurityContext object             │            │
│  │       │                                                                      │            │
│  │       ▼                                                                      │            │
│  │  SecurityContextHolderFilter sets it in ThreadLocal                          │            │
│  │  → User is authenticated, request continues...                              │            │
│  │                                                                              │            │
│  └─────────────────────────────────────────────────────────────────────────────┘            │
│                                                                                              │
│                                                                                              │
│  ── OVERRIDING SESSION TABLE NAMES ──────────────────────────────────────────               │
│                                                                                              │
│  You CAN override the table names, but this is NOT widely used anymore:                     │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  # application.properties                                                     │           │
│  │  spring.session.jdbc.table-name=CUSTOM_SESSION                                │           │
│  │  # → Tables become: CUSTOM_SESSION and CUSTOM_SESSION_ATTRIBUTES             │           │
│  │                                                                               │           │
│  │  # Or in Java config:                                                         │           │
│  │  @Configuration                                                               │           │
│  │  @EnableJdbcHttpSession(tableName = "CUSTOM_SESSION")                        │           │
│  │  public class SessionConfig { }                                               │           │
│  │                                                                               │           │
│  │  ⚠️ NOTE: JDBC-based sessions are NOT widely used in modern production.     │           │
│  │  Modern architectures prefer:                                                 │           │
│  │  • Redis (spring-session-data-redis) — fastest, most popular                │           │
│  │  • JWT tokens — stateless, no server-side session at all                    │           │
│  │  • JDBC sessions are okay for small/monolithic apps where Redis             │           │
│  │    is not available, but they have scalability limitations (see 15.7)        │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 15.4 SessionManagement Configuration — All Options Explained

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SESSION MANAGEMENT — CONFIGURATION OPTIONS                                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Spring Security provides session management configuration through the                      │
│  .sessionManagement() DSL in the SecurityFilterChain. This controls HOW                     │
│  sessions are created, tracked, and protected.                                              │
│                                                                                              │
│                                                                                              │
│  ── BASIC SESSION MANAGEMENT CONFIGURATION ──────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  @Bean                                                                        │           │
│  │  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {       │           │
│  │      http                                                                     │           │
│  │          .sessionManagement(session -> session                                │           │
│  │                                                                               │           │
│  │              // ── 1. Session Creation Policy ────────────────────────       │           │
│  │              .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)       │           │
│  │                                                                               │           │
│  │              // ── 2. Invalid Session Strategy ───────────────────────       │           │
│  │              .invalidSessionUrl("/login?expired")                             │           │
│  │              // ↑ Where to redirect if session is invalid/expired            │           │
│  │                                                                               │           │
│  │              // ── 3. Session Fixation Protection ────────────────────       │           │
│  │              .sessionFixation(fixation -> fixation                            │           │
│  │                  .newSession()                                                 │           │
│  │                  // Options: .migrateSession() (default in 6.x)              │           │
│  │                  //          .newSession()                                    │           │
│  │                  //          .changeSessionId() (default in 5.x)             │           │
│  │                  //          .none() ← ⚠️ INSECURE! Never use in production│           │
│  │              )                                                                │           │
│  │                                                                               │           │
│  │              // ── 4. Maximum Sessions per User ──────────────────────       │           │
│  │              .maximumSessions(1)                                              │           │
│  │              // ↑ Only ONE session per user allowed                          │           │
│  │              // ↑ If user logs in again, previous session is invalidated     │           │
│  │                                                                               │           │
│  │              // ── 5. Expired Session URL (when max sessions exceeded) ──    │           │
│  │              .expiredUrl("/login?maxSessionsExceeded")                        │           │
│  │                                                                               │           │
│  │              // ── 6. Prevent Login When Max Sessions Reached ────────      │           │
│  │              .maxSessionsPreventsLogin(true)                                  │           │
│  │              // ↑ If true: new login BLOCKED, existing session stays         │           │
│  │              // ↑ If false (default): old session invalidated, new login OK  │           │
│  │          )                                                                    │           │
│  │          .authorizeHttpRequests(auth -> auth                                  │           │
│  │              .anyRequest().authenticated()                                    │           │
│  │          )                                                                    │           │
│  │          .formLogin(Customizer.withDefaults());                               │           │
│  │      return http.build();                                                     │           │
│  │  }                                                                            │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── ALL CONFIGURABLE OPTIONS TABLE ──────────────────────────────────────────               │
│                                                                                              │
│  ┌────────────────────────────────┬──────────────────────────────────────────────────────┐  │
│  │  Option                        │  Description                                          │  │
│  ├────────────────────────────────┼──────────────────────────────────────────────────────┤  │
│  │  .sessionCreationPolicy()      │  Controls WHEN sessions are created                   │  │
│  │                                │  (see Section 15.5 for full detail)                   │  │
│  ├────────────────────────────────┼──────────────────────────────────────────────────────┤  │
│  │  .invalidSessionUrl(url)       │  Redirect URL when session is invalid/expired        │  │
│  ├────────────────────────────────┼──────────────────────────────────────────────────────┤  │
│  │  .invalidSessionStrategy()     │  Custom InvalidSessionStrategy implementation        │  │
│  ├────────────────────────────────┼──────────────────────────────────────────────────────┤  │
│  │  .sessionFixation()            │  Protection against session fixation attacks          │  │
│  │                                │  • migrateSession() — copy attributes to new session │  │
│  │                                │  • newSession() — new session, no attribute copy     │  │
│  │                                │  • changeSessionId() — change ID, keep session       │  │
│  │                                │  • none() — ⚠️ INSECURE, no protection!              │  │
│  ├────────────────────────────────┼──────────────────────────────────────────────────────┤  │
│  │  .maximumSessions(n)           │  Max concurrent sessions per user                     │  │
│  │                                │  Default: unlimited                                   │  │
│  ├────────────────────────────────┼──────────────────────────────────────────────────────┤  │
│  │  .maxSessionsPreventsLogin()   │  true = block new login if max reached               │  │
│  │                                │  false = evict oldest session (default)               │  │
│  ├────────────────────────────────┼──────────────────────────────────────────────────────┤  │
│  │  .expiredUrl(url)              │  Redirect URL when session is evicted by concurrency │  │
│  ├────────────────────────────────┼──────────────────────────────────────────────────────┤  │
│  │  .sessionAuthenticationStrategy │  Custom SessionAuthenticationStrategy               │  │
│  │                                │  (advanced — rarely needed)                           │  │
│  └────────────────────────────────┴──────────────────────────────────────────────────────┘  │
│                                                                                              │
│                                                                                              │
│  ── SESSION FIXATION PROTECTION — VISUAL ────────────────────────────────────               │
│                                                                                              │
│  Session fixation attack without protection:                                                │
│                                                                                              │
│  ┌─── ATTACKER ───────────────────────────────────────────────────────────┐                │
│  │                                                                        │                │
│  │  1. Attacker visits site → gets JSESSIONID=EVIL123                    │                │
│  │  2. Attacker tricks victim into using JSESSIONID=EVIL123              │                │
│  │     (via URL rewriting or injected cookie)                            │                │
│  │  3. Victim logs in with JSESSIONID=EVIL123                            │                │
│  │  4. Server authenticates and stores SecurityContext in EVIL123 session │                │
│  │  5. Attacker uses JSESSIONID=EVIL123 → has victim's authenticated     │                │
│  │     session! → COMPROMISED!                                           │                │
│  │                                                                        │                │
│  └────────────────────────────────────────────────────────────────────────┘                │
│                                                                                              │
│  With migrateSession() protection (DEFAULT in Spring Security 6.x):                        │
│                                                                                              │
│  ┌─── PROTECTED ──────────────────────────────────────────────────────────┐                │
│  │                                                                        │                │
│  │  1. Victim logs in with JSESSIONID=EVIL123                            │                │
│  │  2. Spring Security DETECTS login event                               │                │
│  │  3. Creates NEW session: JSESSIONID=SAFE456                           │                │
│  │  4. Copies all attributes from old session to new session             │                │
│  │  5. Invalidates old session EVIL123                                   │                │
│  │  6. Attacker's EVIL123 is now INVALID → attack FAILS!                │                │
│  │                                                                        │                │
│  └────────────────────────────────────────────────────────────────────────┘                │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 15.5 ★ SessionCreationPolicy — All 4 Options Explained with Use Cases

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SessionCreationPolicy — THE 4 OPTIONS                                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  SessionCreationPolicy is an enum that controls WHEN Spring Security creates                │
│  an HttpSession. This is one of the MOST IMPORTANT configuration decisions                  │
│  you'll make in your SecurityConfig.                                                        │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  OPTION 1: SessionCreationPolicy.IF_REQUIRED  ← DEFAULT                                    │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  .sessionManagement(session -> session                                        │           │
│  │      .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)               │           │
│  │  )                                                                            │           │
│  │                                                                               │           │
│  │  BEHAVIOR:                                                                    │           │
│  │  • Spring Security will create a session ONLY WHEN IT NEEDS ONE             │           │
│  │  • Session is created after successful authentication (to store              │           │
│  │    SecurityContext in HttpSession)                                            │           │
│  │  • If the request doesn't need a session (e.g., stateless API call         │           │
│  │    with a token), no session is created                                      │           │
│  │                                                                               │           │
│  │  WHEN TO USE:                                                                 │           │
│  │  ✅ Traditional web applications with form login                              │           │
│  │  ✅ Server-rendered pages (Thymeleaf, JSP)                                    │           │
│  │  ✅ Admin dashboards, CMS, internal tools                                     │           │
│  │  ✅ Applications where you WANT session-based authentication                  │           │
│  │  ✅ Default — use this if you're not sure                                     │           │
│  │                                                                               │           │
│  │  FLOW:                                                                        │           │
│  │  POST /login → authenticate → SUCCESS → create session → store context     │           │
│  │  GET /page   → load session  → SecurityContext available → serve page       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  OPTION 2: SessionCreationPolicy.ALWAYS                                                     │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  .sessionManagement(session -> session                                        │           │
│  │      .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)                    │           │
│  │  )                                                                            │           │
│  │                                                                               │           │
│  │  BEHAVIOR:                                                                    │           │
│  │  • Spring Security will ALWAYS create a session, even if one doesn't exist  │           │
│  │  • Session is created on EVERY request if no session is present              │           │
│  │  • Even for unauthenticated users, a session is created                     │           │
│  │                                                                               │           │
│  │  WHEN TO USE:                                                                 │           │
│  │  ✅ When you need to track unauthenticated users (shopping carts, analytics) │           │
│  │  ✅ When you need session-scoped beans (Spring's @SessionScope)              │           │
│  │  ✅ Applications that store pre-login data in the session                    │           │
│  │  ⚠️ CAUTION: Creates sessions even for bots, crawlers, health checks!       │           │
│  │     This can lead to session table bloat if you're using DB-backed sessions │           │
│  │                                                                               │           │
│  │  FLOW:                                                                        │           │
│  │  GET /page → no session? → CREATE session immediately → serve page          │           │
│  │  GET /page → session exists? → USE existing session → serve page            │           │
│  │                                                                               │           │
│  │  ── DIFFERENCE FROM IF_REQUIRED ──                                           │           │
│  │  IF_REQUIRED: GET /public/page (no auth needed) → NO session created        │           │
│  │  ALWAYS:      GET /public/page (no auth needed) → session IS created        │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  OPTION 3: SessionCreationPolicy.NEVER                                                      │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  .sessionManagement(session -> session                                        │           │
│  │      .sessionCreationPolicy(SessionCreationPolicy.NEVER)                     │           │
│  │  )                                                                            │           │
│  │                                                                               │           │
│  │  BEHAVIOR:                                                                    │           │
│  │  • Spring Security will NEVER create a session itself                        │           │
│  │  • BUT — if a session ALREADY EXISTS (created by application code or        │           │
│  │    another component), Spring Security WILL USE IT                           │           │
│  │  • Spring Security will read SecurityContext from an existing session        │           │
│  │  • Spring Security just won't be the one to CREATE the session              │           │
│  │                                                                               │           │
│  │  WHEN TO USE:                                                                 │           │
│  │  ✅ When another part of your application manages sessions                    │           │
│  │  ✅ When a front-end framework or gateway creates sessions                   │           │
│  │  ✅ When you want session awareness but don't want Spring Security           │           │
│  │     to create sessions on its own                                            │           │
│  │  ✅ Microservice that sits behind an API gateway that manages sessions       │           │
│  │                                                                               │           │
│  │  FLOW:                                                                        │           │
│  │  POST /login → authenticate → SUCCESS → ❌ Spring Security does NOT create  │           │
│  │               session! (If app code does request.getSession(true), then     │           │
│  │               that session will be used by Spring Security on next request) │           │
│  │                                                                               │           │
│  │  ── DIFFERENCE FROM IF_REQUIRED AND STATELESS ──                             │           │
│  │  IF_REQUIRED: Creates session when needed (e.g., after login)               │           │
│  │  NEVER:       Won't create, but WILL USE an existing session                │           │
│  │  STATELESS:   Won't create AND won't use — completely ignores sessions      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  OPTION 4: SessionCreationPolicy.STATELESS  ← MOST IMPORTANT FOR REST APIs                 │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  .sessionManagement(session -> session                                        │           │
│  │      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)                 │           │
│  │  )                                                                            │           │
│  │                                                                               │           │
│  │  BEHAVIOR:                                                                    │           │
│  │  • Spring Security will NEVER create a session                               │           │
│  │  • Spring Security will NEVER use an existing session                        │           │
│  │  • SecurityContextHolderFilter will NOT look up sessions at all             │           │
│  │  • HttpSessionSecurityContextRepository is replaced with                     │           │
│  │    NullSecurityContextRepository (does nothing)                              │           │
│  │  • Every request is treated as completely independent — no server-side state │           │
│  │  • Authentication must be provided on EVERY request (via JWT, API key, etc.)│           │
│  │                                                                               │           │
│  │  WHEN TO USE:                                                                 │           │
│  │  ✅ REST APIs (the most common use case!)                                     │           │
│  │  ✅ JWT-based authentication                                                  │           │
│  │  ✅ OAuth2 Resource Servers                                                   │           │
│  │  ✅ Microservices that authenticate via tokens on every request               │           │
│  │  ✅ Mobile app backends                                                       │           │
│  │  ✅ Serverless / horizontally scaled applications                             │           │
│  │  ❌ NOT for form login — login won't persist without a session!              │           │
│  │                                                                               │           │
│  │  FLOW:                                                                        │           │
│  │  GET /api/orders                                                              │           │
│  │  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...                              │           │
│  │  → JwtAuthenticationFilter extracts and validates JWT                        │           │
│  │  → Sets Authentication in SecurityContextHolder (ThreadLocal only!)          │           │
│  │  → AuthorizationFilter checks access → GRANTED                              │           │
│  │  → Controller processes request → returns response                          │           │
│  │  → SecurityContextHolder CLEARED                                             │           │
│  │  → ❌ NO session created, nothing stored server-side!                        │           │
│  │  → Next request must include JWT again                                      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── COMPARISON TABLE — ALL 4 POLICIES ───────────────────────────────────────               │
│                                                                                              │
│  ┌────────────────────┬───────────────┬───────────────┬───────────────────────────────────┐ │
│  │  Policy            │ Creates       │ Uses Existing │ Best For                           │ │
│  │                    │ Session?      │ Session?      │                                    │ │
│  ├────────────────────┼───────────────┼───────────────┼───────────────────────────────────┤ │
│  │  ALWAYS            │ ✅ Always      │ ✅ Yes         │ Pre-login tracking, session-scoped│ │
│  │                    │ (every req)   │               │ beans, shopping carts              │ │
│  ├────────────────────┼───────────────┼───────────────┼───────────────────────────────────┤ │
│  │  IF_REQUIRED       │ ✅ When needed │ ✅ Yes         │ Form login, traditional web apps  │ │
│  │  (DEFAULT)         │ (after auth)  │               │ server-rendered pages             │ │
│  ├────────────────────┼───────────────┼───────────────┼───────────────────────────────────┤ │
│  │  NEVER             │ ❌ Never       │ ✅ Yes         │ Gateway-managed sessions,         │ │
│  │                    │               │               │ delegated session creation        │ │
│  ├────────────────────┼───────────────┼───────────────┼───────────────────────────────────┤ │
│  │  STATELESS         │ ❌ Never       │ ❌ Never       │ REST APIs, JWT, OAuth2,           │ │
│  │                    │               │               │ microservices, mobile backends    │ │
│  └────────────────────┴───────────────┴───────────────┴───────────────────────────────────┘ │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 15.6 Complete Code — SessionCreationPolicy for Different Application Types

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  EXAMPLE 1: Traditional Web App (Form Login + Session)
//  → Use IF_REQUIRED (default)
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class WebAppSecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                // ↑ Session created after successful login
                // ↑ This is the DEFAULT — you can omit this line

                .sessionFixation(fixation -> fixation.migrateSession())
                // ↑ Protect against session fixation attacks (default)

                .maximumSessions(1)
                // ↑ Only one session per user (prevent concurrent logins)

                .expiredUrl("/login?expired")
                // ↑ Where to redirect if session is evicted by new login
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/css/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")                  // Custom login page
                .defaultSuccessUrl("/dashboard")      // Where to go after login
                .failureUrl("/login?error")           // Where to go on failure
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)          // ★ Destroy session on logout
                .deleteCookies("JSESSIONID")          // ★ Delete session cookie
            );
        return http.build();
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  EXAMPLE 2: REST API with JWT (Stateless)
//  → Use STATELESS — no sessions at all!
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class RestApiSecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // ↑ NO sessions created or used!
                // ↑ Every request must carry its own authentication (JWT)
            )
            .csrf(csrf -> csrf.disable())
            // ↑ CSRF can be disabled for stateless APIs
            // ↑ CSRF protection is session-based — no session = no CSRF needed
            // ↑ Tokens (JWT) are not automatically sent by browsers

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // ↑ Custom JWT filter that validates token on every request
            // ↑ No form login needed — authentication via Authorization header

            .formLogin(AbstractHttpConfigurer::disable)
            // ↑ Explicitly disable form login (not needed for APIs)

            .httpBasic(AbstractHttpConfigurer::disable);
            // ↑ Disable basic auth (using JWT instead)

        return http.build();
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  EXAMPLE 3: Hybrid — Web pages (session) + API endpoints (stateless)
//  → Two SecurityFilterChain beans with different session policies!
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class HybridSecurityConfig {

    // ── Chain 1: REST API — Stateless ──────────────────────────────────────
    @Bean
    @Order(1)  // ★ Higher priority (lower number = checked first)
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")  // ★ Only applies to /api/** URLs
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // ── Chain 2: Web Pages — Session-based ─────────────────────────────────
    @Bean
    @Order(2)  // ★ Lower priority (checked second, for non-API URLs)
    SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
```

---

#### 15.7 ★ Why Custom Session Management Is a Big Overhead — Scalability Issues

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHY SERVER-SIDE SESSION MANAGEMENT IS OVERHEAD IN DISTRIBUTED SYSTEMS                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Server-side sessions (where session data is stored on the server — in memory,              │
│  in a database, or in Redis) introduce significant overhead as your system grows.           │
│  This section explains WHY and WHEN it becomes a problem.                                   │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PROBLEM 1: SESSION AFFINITY (STICKY SESSIONS) — Single Instance                           │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  By default, sessions are stored IN MEMORY of the application server (Tomcat).              │
│  The session lives ONLY in the JVM where it was created.                                    │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  SINGLE INSTANCE — No problem:                                               │           │
│  │                                                                               │           │
│  │  Browser ──── JSESSIONID=ABC ────→ [ Server 1 ]                             │           │
│  │                                     Session store:                            │           │
│  │                                     { ABC: { john, ROLE_USER } }             │           │
│  │                                     ✅ Works fine!                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  MULTIPLE INSTANCES — BROKEN without sticky sessions:                        │           │
│  │                                                                               │           │
│  │  Browser ──── JSESSIONID=ABC ────→ [ Load Balancer ]                        │           │
│  │                                          │                                    │           │
│  │                          ┌───────────────┼───────────────┐                   │           │
│  │                          ▼               ▼               ▼                   │           │
│  │                    [ Server 1 ]    [ Server 2 ]    [ Server 3 ]             │           │
│  │                    Session: ABC    Session: ???    Session: ???              │           │
│  │                    { john }        EMPTY!          EMPTY!                    │           │
│  │                                                                               │           │
│  │  Request 1 → Server 1 ✅ (session found)                                    │           │
│  │  Request 2 → Server 2 ❌ (session NOT found → redirect to login!)           │           │
│  │  Request 3 → Server 1 ✅ (session found)                                    │           │
│  │  Request 4 → Server 3 ❌ (session NOT found → redirect to login!)           │           │
│  │                                                                               │           │
│  │  ★ User keeps getting logged out randomly!                                   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  Solution: "Sticky Sessions" — load balancer always routes same user to same server.       │
│  But this DEFEATS the purpose of load balancing and creates hot spots!                      │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  Problems with Sticky Sessions:                                               │           │
│  │                                                                               │           │
│  │  1. UNEVEN LOAD — Some servers get more users than others                    │           │
│  │  2. SERVER FAILURE — If Server 1 crashes, ALL its sessions are LOST          │           │
│  │     → All users on Server 1 are logged out instantly                         │           │
│  │  3. SCALING DOWN — Can't remove a server without losing its sessions        │           │
│  │  4. DEPLOYMENT — Rolling updates become complex (active sessions!)          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PROBLEM 2: DATABASE-BACKED SESSIONS — DB Load & Latency                                   │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  To solve the sticky session problem, you can store sessions in a shared                    │
│  database (Spring Session JDBC). But this creates NEW problems:                             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌─────────┐         ┌──────────────┐         ┌──────────────────────┐      │           │
│  │  │ Browser │────────→│ Load Balancer│────────→│  Server 1, 2, or 3  │      │           │
│  │  └─────────┘         └──────────────┘         └──────────┬───────────┘      │           │
│  │                                                           │                  │           │
│  │                                                           ▼                  │           │
│  │                                                   ┌──────────────┐          │           │
│  │                                                   │  Database     │          │           │
│  │                                                   │  (PostgreSQL) │          │           │
│  │                                                   │              │          │           │
│  │                                                   │ SPRING_SESSION│          │           │
│  │                                                   │ SPRING_SESSION│          │           │
│  │                                                   │ _ATTRIBUTES   │          │           │
│  │                                                   └──────────────┘          │           │
│  │                                                                               │           │
│  │  ✅ All servers share the same session store                                 │           │
│  │  ✅ Any server can handle any request                                        │           │
│  │  ✅ Server failure doesn't lose sessions                                     │           │
│  │                                                                               │           │
│  │  BUT...                                                                       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  ★ THE DATABASE BECOMES A BOTTLENECK:                                        │           │
│  │                                                                               │           │
│  │  On EVERY request, Spring Session must:                                      │           │
│  │                                                                               │           │
│  │  1. SELECT from SPRING_SESSION (lookup by session ID)                        │           │
│  │  2. SELECT from SPRING_SESSION_ATTRIBUTES (get SecurityContext)              │           │
│  │  3. DESERIALIZE the SecurityContext from bytes                                │           │
│  │  4. At end of request: UPDATE LAST_ACCESS_TIME                               │           │
│  │  5. If attributes changed: UPDATE SPRING_SESSION_ATTRIBUTES                  │           │
│  │                                                                               │           │
│  │  → That's 2-4 SQL queries PER REQUEST, EVERY request!                       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── SCALE CALCULATION ────────────────────────────────────────────────       │           │
│  │                                                                               │           │
│  │  1,000 concurrent users × 5 requests/second = 5,000 requests/second         │           │
│  │  5,000 requests × 3 SQL queries/request = 15,000 SQL queries/second         │           │
│  │  ...JUST FOR SESSION MANAGEMENT!                                              │           │
│  │                                                                               │           │
│  │  10,000 users → 150,000 queries/second for sessions alone!                  │           │
│  │                                                                               │           │
│  │  These queries COMPETE with your actual business queries                     │           │
│  │  (user lookups, order queries, product searches, etc.)                       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  SPECIFIC PROBLEMS WITH DB-BACKED SESSIONS:                                  │           │
│  │                                                                               │           │
│  │  1. LATENCY PER REQUEST                                                      │           │
│  │     • In-memory session lookup: ~0.001ms (nanoseconds)                       │           │
│  │     • Database session lookup: ~1-10ms (network + query + deserialization)   │           │
│  │     • That's 1,000x-10,000x SLOWER per request                              │           │
│  │     • Adds 2-20ms of latency to EVERY request                               │           │
│  │                                                                               │           │
│  │  2. DATABASE CONNECTION POOL EXHAUSTION                                      │           │
│  │     • Default HikariCP pool: 10 connections                                  │           │
│  │     • Session queries consume connections that your business logic needs     │           │
│  │     • Under load: "Connection is not available" errors                       │           │
│  │     • Must increase pool size → more DB connections → more DB memory         │           │
│  │                                                                               │           │
│  │  3. SERIALIZATION / DESERIALIZATION OVERHEAD                                 │           │
│  │     • SecurityContext must be serialized to bytes (Java Serialization)       │           │
│  │     • On read: bytes must be deserialized back to Java objects               │           │
│  │     • CPU-intensive, especially with large session attributes                │           │
│  │     • Java Serialization is SLOW and produces large payloads                 │           │
│  │                                                                               │           │
│  │  4. TABLE SIZE GROWTH                                                        │           │
│  │     • 10,000 active sessions × ~2KB per session = ~20MB                     │           │
│  │     • 1,000,000 sessions (if cleanup is delayed) = ~2GB                     │           │
│  │     • Expired session cleanup requires periodic DELETE queries              │           │
│  │     • Large tables → slower index lookups → cascading performance issues    │           │
│  │                                                                               │           │
│  │  5. SINGLE POINT OF FAILURE                                                  │           │
│  │     • Database goes down → ALL sessions are unavailable                     │           │
│  │     • ALL users are effectively logged out                                   │           │
│  │     • Must set up DB replication/failover for high availability             │           │
│  │                                                                               │           │
│  │  6. LOCK CONTENTION                                                          │           │
│  │     • Concurrent requests from the same user may attempt to UPDATE the      │           │
│  │       same session row simultaneously → row-level locks → contention        │           │
│  │     • Particularly problematic with AJAX-heavy SPAs                         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PROBLEM 3: EVEN REDIS-BACKED SESSIONS ADD OVERHEAD                                        │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  Redis is MUCH faster than a database for session storage, but it's not free:              │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Redis session lookup: ~0.1-1ms (vs ~1-10ms for database)                   │           │
│  │  ✅ Much faster than DB                                                      │           │
│  │  ✅ No SQL parsing, no disk I/O (in-memory)                                  │           │
│  │  ✅ Built-in TTL expiry (no cleanup queries needed)                          │           │
│  │                                                                               │           │
│  │  BUT STILL:                                                                   │           │
│  │  ❌ Network round-trip on every request (app server ↔ Redis)                 │           │
│  │  ❌ Serialization/deserialization overhead still exists                       │           │
│  │  ❌ Redis memory cost (all sessions in RAM)                                  │           │
│  │  ❌ Redis becomes a dependency — Redis down = sessions lost                  │           │
│  │  ❌ Must manage Redis cluster for high availability                          │           │
│  │  ❌ Still 2 network calls per request (read session + write last access)     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  THE ALTERNATIVE: STATELESS AUTHENTICATION (JWT / OAuth2 Tokens)                           │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  With JWT (JSON Web Token) authentication:                                   │           │
│  │                                                                               │           │
│  │  • NO server-side session at all                                             │           │
│  │  • Authentication info is embedded IN the token itself                       │           │
│  │  • Token is sent on every request via Authorization header                  │           │
│  │  • Server validates the token's signature — no DB/Redis lookup needed!      │           │
│  │  • SessionCreationPolicy.STATELESS                                           │           │
│  │                                                                               │           │
│  │  ┌─────────┐                          ┌──────────────────────┐              │           │
│  │  │ Browser │──── Authorization: ────→│  Server 1, 2, or 3  │              │           │
│  │  │ / App   │    Bearer eyJhbGci...    │                      │              │           │
│  │  └─────────┘                          │  1. Validate JWT     │              │           │
│  │                                       │     signature        │              │           │
│  │                                       │  2. Extract claims   │              │           │
│  │                                       │     { sub: "john",   │              │           │
│  │                                       │       roles: [USER] }│              │           │
│  │                                       │  3. Create Auth obj  │              │           │
│  │                                       │  4. Set in           │              │           │
│  │                                       │     SecurityContext   │              │           │
│  │                                       │  5. Process request  │              │           │
│  │                                       │                      │              │           │
│  │                                       │  ❌ NO session!      │              │           │
│  │                                       │  ❌ NO DB query!     │              │           │
│  │                                       │  ❌ NO Redis call!   │              │           │
│  │                                       └──────────────────────┘              │           │
│  │                                                                               │           │
│  │  ✅ Horizontal scaling: add servers freely, no shared state                  │           │
│  │  ✅ No session affinity needed: any server handles any request               │           │
│  │  ✅ No session store dependency: no DB or Redis needed for auth             │           │
│  │  ✅ Zero server-side storage per user                                        │           │
│  │  ✅ Sub-millisecond auth (just signature verification)                       │           │
│  │  ✅ Perfect for microservices and distributed systems                        │           │
│  │                                                                               │           │
│  │  ⚠️ Trade-offs of JWT:                                                       │           │
│  │  • Cannot revoke a JWT before expiry (no server-side session to invalidate) │           │
│  │    → Mitigation: short-lived tokens + refresh token rotation               │           │
│  │  • Token size is larger than a session cookie (~800 bytes vs ~36 bytes)     │           │
│  │  • Must handle token refresh logic (refresh tokens, token rotation)         │           │
│  │  • Sensitive data in JWT payload is visible (base64-encoded, NOT encrypted) │           │
│  │    → Never put passwords, PII, or secrets in JWT claims                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 15.8 Session Storage Comparison — In-Memory vs Database vs Redis vs Stateless

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SESSION STORAGE COMPARISON TABLE                                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────┬──────────────┬──────────────┬──────────────┬──────────────────────┐   │
│  │ Aspect           │ In-Memory    │ Database     │ Redis        │ Stateless (JWT)      │   │
│  │                  │ (Tomcat)     │ (JDBC)       │              │                      │   │
│  ├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────────────┤   │
│  │ Lookup Speed     │ ~0.001ms     │ ~1-10ms      │ ~0.1-1ms     │ ~0.01ms              │   │
│  │                  │ (fastest)    │ (slowest)    │ (fast)       │ (signature verify)   │   │
│  ├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────────────┤   │
│  │ Scales with      │ ❌ No        │ ✅ Yes (shared│ ✅ Yes (shared│ ✅ Yes (no shared     │   │
│  │ multiple servers │ (sticky sess)│ store)       │ store)       │ state at all)        │   │
│  ├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────────────┤   │
│  │ Server failure   │ ❌ Sessions   │ ✅ Sessions   │ ✅ Sessions   │ ✅ No sessions to     │   │
│  │ impact           │ LOST         │ survive      │ survive      │ lose                 │   │
│  ├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────────────┤   │
│  │ Additional infra │ None         │ Database     │ Redis cluster│ None                 │   │
│  │ required         │              │ (already     │              │                      │   │
│  │                  │              │ have one?)   │              │                      │   │
│  ├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────────────┤   │
│  │ Memory usage     │ JVM heap     │ DB disk +    │ Redis RAM    │ Zero server-side     │   │
│  │                  │ (~1-5KB/sess)│ connection   │ (~1-5KB/sess)│                      │   │
│  │                  │              │ pool         │              │                      │   │
│  ├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────────────┤   │
│  │ DB load          │ None         │ HIGH         │ None (on DB) │ None                 │   │
│  │                  │              │ (3-5 queries │              │                      │   │
│  │                  │              │ per request) │              │                      │   │
│  ├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────────────┤   │
│  │ Revocability     │ ✅ Instant    │ ✅ Instant    │ ✅ Instant    │ ❌ Cannot revoke      │   │
│  │                  │ (invalidate) │ (delete row) │ (delete key) │ until expiry         │   │
│  ├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────────────┤   │
│  │ Best for         │ Single       │ Small apps,  │ Distributed  │ REST APIs,           │   │
│  │                  │ instance,    │ monoliths,   │ web apps     │ microservices,       │   │
│  │                  │ development  │ low traffic  │ needing      │ mobile backends,     │   │
│  │                  │              │              │ sessions     │ high scalability     │   │
│  ├──────────────────┼──────────────┼──────────────┼──────────────┼──────────────────────┤   │
│  │ Spring config    │ Default      │ spring-      │ spring-      │ SessionCreation      │   │
│  │                  │ (no extra)   │ session-jdbc │ session-data │ Policy.STATELESS     │   │
│  │                  │              │              │ -redis       │ + JWT filter         │   │
│  └──────────────────┴──────────────┴──────────────┴──────────────┴──────────────────────┘   │
│                                                                                              │
│                                                                                              │
│  ── DECISION FLOWCHART — WHICH SESSION STRATEGY SHOULD I USE? ───────────────               │
│                                                                                              │
│  Is your app a REST API or microservice?                                                    │
│       │                                                                                      │
│    ┌──┴──┐                                                                                   │
│    │     │                                                                                   │
│  YES    NO                                                                                   │
│    │     │                                                                                   │
│    ▼     │                                                                                   │
│  Use     Is it a traditional web app with server-rendered pages?                            │
│  STATELESS  │                                                                                │
│  (JWT)   ┌──┴──┐                                                                             │
│          │     │                                                                             │
│        YES    NO (hybrid / SPA)                                                              │
│          │     │                                                                             │
│          ▼     ▼                                                                             │
│          │   Consider STATELESS (JWT) for API + session for web pages                       │
│          │   (see hybrid example in 15.6)                                                   │
│          │                                                                                   │
│          Will you have multiple server instances?                                            │
│          │                                                                                   │
│       ┌──┴──┐                                                                                │
│       │     │                                                                                │
│     YES    NO                                                                                │
│       │     │                                                                                │
│       │     ▼                                                                                │
│       │   In-Memory sessions (default Tomcat) — simplest!                                   │
│       │                                                                                      │
│       Do you already have Redis?                                                            │
│       │                                                                                      │
│    ┌──┴──┐                                                                                   │
│    │     │                                                                                   │
│  YES    NO                                                                                   │
│    │     │                                                                                   │
│    ▼     ▼                                                                                   │
│  Use     Use JDBC sessions (spring-session-jdbc)                                            │
│  Redis   ⚠️ But consider switching to Redis or JWT                                         │
│  sessions   if traffic grows beyond a few hundred concurrent users                         │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 15.9 Summary — Defaults, Session Policies & Distributed Overhead

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SUMMARY — SECTION 15 KEY TAKEAWAYS                                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. SPRING BOOT DEFAULT SECURITY                                             │           │
│  │     • SpringBootWebSecurityConfiguration creates a default SecurityFilter-   │           │
│  │       Chain with form login + HTTP basic + .anyRequest().authenticated()     │           │
│  │     • This bean is created ONLY when you don't define your own               │           │
│  │     • @ConditionalOnDefaultWebSecurity → backs off when you define yours    │           │
│  │     • When you define your own, ALL defaults are lost — re-add what you need │           │
│  │                                                                               │           │
│  │  2. SPRING SESSION DATABASE TABLES                                           │           │
│  │     • spring-session-jdbc auto-creates SPRING_SESSION and                    │           │
│  │       SPRING_SESSION_ATTRIBUTES tables                                       │           │
│  │     • Session cookie name changes from JSESSIONID to SESSION                 │           │
│  │     • Table names can be overridden but this is rarely done                  │           │
│  │     • JDBC sessions are NOT recommended for high-traffic production systems  │           │
│  │                                                                               │           │
│  │  3. SESSION CREATION POLICIES                                                │           │
│  │     • IF_REQUIRED (default): creates session only when needed (after login)  │           │
│  │     • ALWAYS: creates session on every request (even unauthenticated)       │           │
│  │     • NEVER: won't create, but will use existing sessions                   │           │
│  │     • STATELESS: never creates or uses sessions (for REST APIs + JWT)       │           │
│  │                                                                               │           │
│  │  4. WHY SERVER-SIDE SESSIONS ARE OVERHEAD                                    │           │
│  │     • In-memory sessions: don't scale across multiple instances             │           │
│  │     • DB-backed sessions: 3-5 extra SQL queries per request                 │           │
│  │       → connection pool exhaustion, increased latency, DB load              │           │
│  │     • Redis-backed: faster but adds infrastructure dependency               │           │
│  │     • All server-side sessions require serialization/deserialization         │           │
│  │                                                                               │           │
│  │  5. MODERN RECOMMENDATION                                                    │           │
│  │     • REST APIs / Microservices → STATELESS + JWT                           │           │
│  │     • Traditional web apps → IF_REQUIRED + Redis (if multi-instance)        │           │
│  │     • Hybrid (web + API) → two SecurityFilterChain beans with different     │           │
│  │       session policies                                                       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```


---


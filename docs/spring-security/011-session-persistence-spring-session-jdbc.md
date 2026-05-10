### 11. Session Persistence — Storing HttpSession & JSESSIONID in Database (Spring Session JDBC)

---

#### 11.1 The Problem — Why Store Sessions in a Database?

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  THE PROBLEM WITH IN-MEMORY SESSIONS (Default Tomcat Behavior)                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  By default, Tomcat stores HttpSession objects in a ConcurrentHashMap in JVM HEAP memory:   │
│                                                                                              │
│  ┌─────────────────────────────────────────────┐                                            │
│  │  Tomcat JVM (Heap Memory)                    │                                            │
│  │  ┌───────────────────────────────────────┐  │                                            │
│  │  │  SessionManager (ConcurrentHashMap)    │  │                                            │
│  │  │  ┌─────────────┬────────────────────┐ │  │                                            │
│  │  │  │ JSESSIONID  │ HttpSession Object  │ │  │                                            │
│  │  │  ├─────────────┼────────────────────┤ │  │                                            │
│  │  │  │ ABC123      │ {john, ROLE_USER}   │ │  │                                            │
│  │  │  │ DEF456      │ {jane, ROLE_ADMIN}  │ │  │                                            │
│  │  │  │ GHI789      │ {bob, ROLE_USER}    │ │  │                                            │
│  │  │  └─────────────┴────────────────────┘ │  │                                            │
│  │  └───────────────────────────────────────┘  │                                            │
│  └─────────────────────────────────────────────┘                                            │
│                                                                                              │
│  PROBLEMS with in-memory sessions:                                                          │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  1. SERVER RESTART = ALL SESSIONS LOST                                       │           │
│  │     JVM restarts (deploy, crash, scaling) → HashMap wiped → all users       │           │
│  │     are logged out immediately. They must re-login.                          │           │
│  │                                                                               │           │
│  │  2. NO HORIZONTAL SCALING                                                     │           │
│  │     If you have 3 server instances behind a load balancer:                   │           │
│  │     User logs in on Server 1 → session stored in Server 1's memory          │           │
│  │     Next request hits Server 2 → Server 2 doesn't have the session → 403!  │           │
│  │                                                                               │           │
│  │  3. MEMORY LIMITS                                                             │           │
│  │     10,000 active sessions × ~5KB each = ~50MB of heap memory               │           │
│  │     More users = more memory = risk of OutOfMemoryError                      │           │
│  │                                                                               │           │
│  │  4. NO AUDIT TRAIL                                                            │           │
│  │     Cannot query "how many active sessions?" from a database                 │           │
│  │     Cannot expire specific sessions administratively                         │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  SOLUTION: Store sessions in a SHARED external store (Database or Redis)                    │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 11.2 What is Spring Session JDBC?

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SPRING SESSION JDBC — OVERVIEW                                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Spring Session is a project that REPLACES the servlet container's (Tomcat's) default       │
│  HttpSession implementation with a custom one that stores session data in an EXTERNAL       │
│  store instead of JVM memory.                                                                │
│                                                                                              │
│  Spring Session JDBC specifically stores sessions in a RELATIONAL DATABASE                   │
│  (MySQL, PostgreSQL, H2, Oracle, SQL Server, etc.)                                          │
│                                                                                              │
│  Maven Dependency:                                                                           │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  <dependency>                                                                │           │
│  │      <groupId>org.springframework.session</groupId>                          │           │
│  │      <artifactId>spring-session-jdbc</artifactId>                            │           │
│  │  </dependency>                                                               │           │
│  │  <!-- Version is managed by Spring Boot BOM — no version needed -->          │           │
│  │                                                                               │           │
│  │  <!-- You also need a JDBC driver + Spring Data JDBC/JPA -->                 │           │
│  │  <dependency>                                                                │           │
│  │      <groupId>org.springframework.boot</groupId>                             │           │
│  │      <artifactId>spring-boot-starter-jdbc</artifactId>                       │           │
│  │  </dependency>                                                               │           │
│  │  <dependency>                                                                │           │
│  │      <groupId>org.postgresql</groupId>                                       │           │
│  │      <artifactId>postgresql</artifactId>                                     │           │
│  │      <scope>runtime</scope>                                                  │           │
│  │  </dependency>                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  Gradle:                                                                                     │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  implementation 'org.springframework.session:spring-session-jdbc'             │           │
│  │  implementation 'org.springframework.boot:spring-boot-starter-jdbc'           │           │
│  │  runtimeOnly 'org.postgresql:postgresql'                                      │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  HOW IT WORKS (Internally):                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. Spring Session registers a Servlet Filter: SessionRepositoryFilter        │           │
│  │     This filter runs BEFORE all Spring Security filters.                     │           │
│  │                                                                               │           │
│  │  2. SessionRepositoryFilter WRAPS the HttpServletRequest with a custom       │           │
│  │     wrapper: SessionRepositoryRequestWrapper                                  │           │
│  │                                                                               │           │
│  │  3. When any code calls request.getSession():                                │           │
│  │     Instead of Tomcat's in-memory session → it creates/loads from DATABASE   │           │
│  │                                                                               │           │
│  │  4. JdbcIndexedSessionRepository handles all DB operations:                  │           │
│  │     - createSession()  → INSERT INTO SPRING_SESSION                          │           │
│  │     - findById()       → SELECT FROM SPRING_SESSION WHERE SESSION_ID = ?     │           │
│  │     - save()           → UPDATE SPRING_SESSION                               │           │
│  │     - deleteById()     → DELETE FROM SPRING_SESSION WHERE SESSION_ID = ?     │           │
│  │                                                                               │           │
│  │  5. Spring Security works EXACTLY the same — it doesn't even know            │           │
│  │     that sessions are now in a database. It still calls:                     │           │
│  │     session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext)          │           │
│  │     But now this goes to PostgreSQL instead of a HashMap.                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 11.3 Architecture Diagram — Before & After Spring Session JDBC

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  BEFORE: Default Tomcat Sessions (In-Memory)                                                 │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Browser ──Cookie: JSESSIONID=ABC──▶ Tomcat ──▶ JVM Heap Memory (HashMap)                  │
│                                                  ┌─────────────────────────┐                │
│                                                  │  ABC → {john, ROLE_USER}│                │
│                                                  │  DEF → {jane, ROLE_ADMIN}│               │
│                                                  └─────────────────────────┘                │
│                                                                                              │
│  ⚠️ Server restart = HashMap cleared = ALL sessions LOST!                                   │
│                                                                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│  AFTER: Spring Session JDBC (Database-Backed)                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                  SessionRepositoryFilter                                    │
│                                  (replaces Tomcat's session handling)                        │
│                                         │                                                    │
│  Browser ──Cookie: SESSION=ABC──▶ Spring Boot ──▶ JdbcIndexedSessionRepository             │
│                                                         │                                    │
│                                                         ▼                                    │
│                                                  ┌──────────────┐                           │
│                                                  │  PostgreSQL   │                           │
│                                                  │  ┌──────────────────────────────┐       │
│                                                  │  │  SPRING_SESSION              │       │
│                                                  │  │  ┌──────────┬──────────┐    │       │
│                                                  │  │  │SESSION_ID│ATTRIBUTES│    │       │
│                                                  │  │  ├──────────┼──────────┤    │       │
│                                                  │  │  │ABC       │{john,...}│    │       │
│                                                  │  │  │DEF       │{jane,...}│    │       │
│                                                  │  │  └──────────┴──────────┘    │       │
│                                                  │  └──────────────────────────────┘       │
│                                                  │  ┌──────────────────────────────┐       │
│                                                  │  │  SPRING_SESSION_ATTRIBUTES   │       │
│                                                  │  │  ┌──────────┬──────┬──────┐ │       │
│                                                  │  │  │SESSION_ID│ KEY  │VALUE │ │       │
│                                                  │  │  ├──────────┼──────┼──────┤ │       │
│                                                  │  │  │ABC       │SEC_CTX│blob │ │       │
│                                                  │  │  │DEF       │SEC_CTX│blob │ │       │
│                                                  │  │  └──────────┴──────┴──────┘ │       │
│                                                  │  └──────────────────────────────┘       │
│                                                  └──────────────┘                           │
│                                                                                              │
│  ✅ Server restart = sessions SURVIVE! Users stay logged in!                                │
│  ✅ Cookie name changes from JSESSIONID to SESSION (Spring Session default)                 │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 11.4 Database Tables — What Spring Session JDBC Creates

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  DATABASE TABLES CREATED BY SPRING SESSION JDBC                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Spring Session auto-creates TWO tables in your database:                                   │
│                                                                                              │
│  TABLE 1: SPRING_SESSION                                                                    │
│  ┌─────────────────────┬───────────┬──────────────────────────────────────────┐             │
│  │  Column              │  Type     │  Description                             │             │
│  ├─────────────────────┼───────────┼──────────────────────────────────────────┤             │
│  │  PRIMARY_ID          │  CHAR(36) │  UUID primary key                        │             │
│  │  SESSION_ID          │  CHAR(36) │  The session ID sent as cookie value     │             │
│  │  CREATION_TIME       │  BIGINT   │  Epoch millis when session was created   │             │
│  │  LAST_ACCESS_TIME    │  BIGINT   │  Epoch millis of last request            │             │
│  │  MAX_INACTIVE_INTERVAL│ INT      │  Timeout in seconds (e.g., 2700 = 45m)  │             │
│  │  EXPIRY_TIME         │  BIGINT   │  Epoch millis when session expires       │             │
│  │  PRINCIPAL_NAME      │  VARCHAR  │  Username (for querying sessions by user)│             │
│  └─────────────────────┴───────────┴──────────────────────────────────────────┘             │
│                                                                                              │
│  TABLE 2: SPRING_SESSION_ATTRIBUTES                                                         │
│  ┌─────────────────────┬───────────┬──────────────────────────────────────────┐             │
│  │  Column              │  Type     │  Description                             │             │
│  ├─────────────────────┼───────────┼──────────────────────────────────────────┤             │
│  │  SESSION_PRIMARY_ID  │  CHAR(36) │  FK → SPRING_SESSION.PRIMARY_ID          │             │
│  │  ATTRIBUTE_NAME      │  VARCHAR  │  "SPRING_SECURITY_CONTEXT" or custom     │             │
│  │  ATTRIBUTE_BYTES     │  BYTEA    │  Serialized Java object (SecurityContext)│             │
│  └─────────────────────┴───────────┴──────────────────────────────────────────┘             │
│                                                                                              │
│  Example data after login:                                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  SPRING_SESSION:                                                             │           │
│  │  PRIMARY_ID  = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"                       │           │
│  │  SESSION_ID  = "x9y8z7w6-v5u4-t3s2-r1q0-p9o8n7m6l5k4"                      │           │
│  │  CREATION_TIME = 1745920800000 (2026-04-29T10:00:00Z)                        │           │
│  │  LAST_ACCESS_TIME = 1745921100000 (2026-04-29T10:05:00Z)                     │           │
│  │  MAX_INACTIVE_INTERVAL = 2700 (45 minutes)                                   │           │
│  │  EXPIRY_TIME = 1745923500000 (2026-04-29T10:45:00Z)                          │           │
│  │  PRINCIPAL_NAME = "john"                                                      │           │
│  │                                                                               │           │
│  │  SPRING_SESSION_ATTRIBUTES:                                                   │           │
│  │  SESSION_PRIMARY_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"                │           │
│  │  ATTRIBUTE_NAME = "SPRING_SECURITY_CONTEXT"                                   │           │
│  │  ATTRIBUTE_BYTES = 0xACED0005... (serialized SecurityContext with             │           │
│  │                     Authentication { principal: john, authorities: ROLE_USER})│           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  NOTE: ATTRIBUTE_BYTES stores Java-serialized objects.                                      │
│  All classes stored in the session MUST implement java.io.Serializable.                     │
│  This includes your UserAuthEntity class!                                                   │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 11.5 Complete Implementation — Spring Session JDBC

**Step 1: `pom.xml` — Add Dependencies**

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Spring Data JPA (for UserAuthEntity) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- ★ Spring Session JDBC — THIS IS THE KEY DEPENDENCY ★ -->
    <dependency>
        <groupId>org.springframework.session</groupId>
        <artifactId>spring-session-jdbc</artifactId>
    </dependency>

    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Thymeleaf (for custom login page) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

**Step 2: `application.properties` — Configure Spring Session JDBC**

```properties
# ═══════════════════════════════════════════════════════════════════════════════
#  DATABASE CONNECTION
# ═══════════════════════════════════════════════════════════════════════════════
spring.datasource.url=jdbc:postgresql://localhost:5432/myapp
spring.datasource.username=postgres
spring.datasource.password=secret
spring.datasource.driver-class-name=org.postgresql.Driver

# ═══════════════════════════════════════════════════════════════════════════════
#  JPA / HIBERNATE
# ═══════════════════════════════════════════════════════════════════════════════
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# ═══════════════════════════════════════════════════════════════════════════════
#  ★ SPRING SESSION JDBC CONFIGURATION ★
# ═══════════════════════════════════════════════════════════════════════════════

# Tell Spring Session to use JDBC as the session store
# Options: jdbc, redis, hazelcast, mongodb, none
spring.session.store-type=jdbc

# Auto-create the SPRING_SESSION and SPRING_SESSION_ATTRIBUTES tables
# Options: always, never, embedded
# "always" = create tables on every startup (safe — uses CREATE TABLE IF NOT EXISTS)
# "embedded" = only for embedded databases (H2, HSQLDB)
# "never" = you must create tables manually using the SQL scripts
spring.session.jdbc.initialize-schema=always

# Table name prefix (default: SPRING_SESSION)
# Change if you want custom table names: MYAPP_SESSION, MYAPP_SESSION_ATTRIBUTES
spring.session.jdbc.table-name=SPRING_SESSION

# Session timeout (overrides server.servlet.session.timeout)
# Spring Session uses this value, NOT Tomcat's timeout
server.servlet.session.timeout=45m

# Cleanup cron — Spring Session JDBC runs a scheduled task to delete expired sessions
# Default: runs every minute
spring.session.jdbc.cleanup-cron=0 */1 * * * *

# ═══════════════════════════════════════════════════════════════════════════════
#  SESSION COOKIE CONFIGURATION
# ═══════════════════════════════════════════════════════════════════════════════
# NOTE: Spring Session changes the default cookie name from JSESSIONID to SESSION
# You can override this:
server.servlet.session.cookie.name=SESSION
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=Lax
```

**Step 3: `UserAuthEntity` — MUST implement Serializable**

```java
@Entity
@Table(name = "users")
public class UserAuthEntity implements UserDetails, Serializable {
    //                                              ↑ CRITICAL!
    // Spring Session JDBC serializes session attributes to ATTRIBUTE_BYTES
    // SecurityContext → Authentication → Principal (UserAuthEntity)
    // If UserAuthEntity is NOT Serializable → NotSerializableException!

    private static final long serialVersionUID = 1L;
    //                                          ↑ Recommended: add serialVersionUID

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;  // e.g., "ROLE_USER", "ROLE_ADMIN"

    // ... getters, setters, UserDetails methods (same as before)

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return true; }
}
```

**Step 4: `SecurityConfig` — No changes needed for Spring Session!**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // ★ IMPORTANT: Your SecurityConfig does NOT change at all!
    // Spring Session JDBC transparently replaces Tomcat's session management.
    // Spring Security still calls session.setAttribute("SPRING_SECURITY_CONTEXT", ctx)
    // But now it goes to PostgreSQL instead of a HashMap.

    // ... same SecurityConfig as Section 10.1.1 above ...
    // .formLogin(), .logout(), .sessionManagement() — all work exactly the same!
}
```

---

#### 11.6 How It Works — Internal Flow with Spring Session JDBC

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  INTERNAL FLOW — SPRING SESSION JDBC (What Happens Under the Hood)                          │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ── FILTER ORDER (Spring Session filter runs FIRST!) ─────────────────────────              │
│                                                                                              │
│  Request arrives                                                                             │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐    │
│  │ 0. SessionRepositoryFilter (from Spring Session — runs BEFORE Spring Security!)     │    │
│  │    • Wraps HttpServletRequest with SessionRepositoryRequestWrapper                  │    │
│  │    • From this point, any call to request.getSession() goes to DATABASE             │    │
│  │    • Tomcat's in-memory session management is COMPLETELY BYPASSED                   │    │
│  └─────────────────────────────────────────────────────────────────────────────────────┘    │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐    │
│  │ 1. SecurityContextHolderFilter (Spring Security)                                     │    │
│  │    • Calls request.getSession() → goes to SessionRepositoryRequestWrapper           │    │
│  │    • → JdbcIndexedSessionRepository.findById(sessionId)                              │    │
│  │    • → SQL: SELECT * FROM SPRING_SESSION s                                           │    │
│  │             JOIN SPRING_SESSION_ATTRIBUTES a ON s.PRIMARY_ID = a.SESSION_PRIMARY_ID │    │
│  │             WHERE s.SESSION_ID = ?                                                   │    │
│  │    • Deserializes ATTRIBUTE_BYTES → SecurityContext                                  │    │
│  │    • Sets in SecurityContextHolder                                                   │    │
│  └─────────────────────────────────────────────────────────────────────────────────────┘    │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐    │
│  │ 2-9. All other Spring Security filters (same as before)                              │    │
│  │    CsrfFilter → UsernamePasswordAuthFilter → AuthorizationFilter → etc.             │    │
│  └─────────────────────────────────────────────────────────────────────────────────────┘    │
│       │                                                                                      │
│       ▼                                                                                      │
│  Controller handles request                                                                  │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐    │
│  │ Response Phase: SessionRepositoryFilter commits session                              │    │
│  │    • Serializes SecurityContext to bytes                                              │    │
│  │    • SQL: UPDATE SPRING_SESSION SET LAST_ACCESS_TIME = ? WHERE SESSION_ID = ?       │    │
│  │    • SQL: UPDATE SPRING_SESSION_ATTRIBUTES SET ATTRIBUTE_BYTES = ?                  │    │
│  │           WHERE SESSION_PRIMARY_ID = ? AND ATTRIBUTE_NAME = ?                       │    │
│  └─────────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                              │
│                                                                                              │
│  ── LOGIN FLOW (New session created in database) ─────────────────────────────              │
│                                                                                              │
│  POST /login → Authentication success                                                       │
│       │                                                                                      │
│       ▼                                                                                      │
│  JdbcIndexedSessionRepository.createSession()                                                │
│  → SQL: INSERT INTO SPRING_SESSION                                                          │
│         (PRIMARY_ID, SESSION_ID, CREATION_TIME, LAST_ACCESS_TIME,                           │
│          MAX_INACTIVE_INTERVAL, EXPIRY_TIME, PRINCIPAL_NAME)                                │
│         VALUES (?, ?, ?, ?, 2700, ?, 'john')                                                │
│                                                                                              │
│  → SQL: INSERT INTO SPRING_SESSION_ATTRIBUTES                                               │
│         (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES)                               │
│         VALUES (?, 'SPRING_SECURITY_CONTEXT', ?)                                            │
│                                                                                              │
│  Response: Set-Cookie: SESSION=x9y8z7w6...; Path=/; HttpOnly; Secure                       │
│  (NOTE: cookie name is "SESSION" not "JSESSIONID" — Spring Session's default)               │
│                                                                                              │
│                                                                                              │
│  ── SUBSEQUENT REQUEST (Session loaded from database) ────────────────────────              │
│                                                                                              │
│  GET /dashboard                                                                              │
│  Cookie: SESSION=x9y8z7w6...                                                                │
│       │                                                                                      │
│       ▼                                                                                      │
│  JdbcIndexedSessionRepository.findById("x9y8z7w6...")                                       │
│  → SQL: SELECT * FROM SPRING_SESSION WHERE SESSION_ID = 'x9y8z7w6...'                      │
│  → Check: EXPIRY_TIME > now()? Yes → session is valid                                      │
│  → Load attributes: SELECT * FROM SPRING_SESSION_ATTRIBUTES                                 │
│    WHERE SESSION_PRIMARY_ID = ?                                                              │
│  → Deserialize ATTRIBUTE_BYTES → SecurityContext { john, ROLE_USER }                        │
│  → SecurityContextHolder.setContext(ctx)                                                     │
│  → User is authenticated! ✓                                                                │
│                                                                                              │
│                                                                                              │
│  ── LOGOUT (Session deleted from database) ────────────────────────────────                 │
│                                                                                              │
│  POST /logout                                                                                │
│       │                                                                                      │
│       ▼                                                                                      │
│  JdbcIndexedSessionRepository.deleteById("x9y8z7w6...")                                     │
│  → SQL: DELETE FROM SPRING_SESSION_ATTRIBUTES WHERE SESSION_PRIMARY_ID = ?                  │
│  → SQL: DELETE FROM SPRING_SESSION WHERE PRIMARY_ID = ?                                     │
│                                                                                              │
│  Response: Set-Cookie: SESSION=; Path=/; HttpOnly; Max-Age=0                                │
│                                                                                              │
│                                                                                              │
│  ── EXPIRED SESSION CLEANUP (Background Job) ──────────────────────────────                 │
│                                                                                              │
│  Spring Session runs a scheduled cron job (default: every 1 minute):                        │
│  → SQL: DELETE FROM SPRING_SESSION WHERE EXPIRY_TIME < ?                                    │
│  This deletes all expired sessions from the database automatically.                         │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 11.7 Verify It's Working — Query the Database

```sql
-- See all active sessions:
SELECT 
    SESSION_ID, 
    PRINCIPAL_NAME, 
    TO_TIMESTAMP(CREATION_TIME / 1000) AS created_at,
    TO_TIMESTAMP(LAST_ACCESS_TIME / 1000) AS last_accessed,
    TO_TIMESTAMP(EXPIRY_TIME / 1000) AS expires_at,
    MAX_INACTIVE_INTERVAL / 60 AS timeout_minutes
FROM SPRING_SESSION
ORDER BY LAST_ACCESS_TIME DESC;

-- See all sessions for a specific user:
SELECT * FROM SPRING_SESSION WHERE PRINCIPAL_NAME = 'john';

-- See session attributes (SecurityContext is stored here):
SELECT 
    a.SESSION_PRIMARY_ID,
    a.ATTRIBUTE_NAME,
    LENGTH(a.ATTRIBUTE_BYTES) AS bytes_size
FROM SPRING_SESSION_ATTRIBUTES a
JOIN SPRING_SESSION s ON s.PRIMARY_ID = a.SESSION_PRIMARY_ID;

-- Manually expire/delete a specific user's session (admin force-logout):
DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = 'john';

-- Count active sessions:
SELECT COUNT(*) AS active_sessions FROM SPRING_SESSION WHERE EXPIRY_TIME > EXTRACT(EPOCH FROM NOW()) * 1000;
```


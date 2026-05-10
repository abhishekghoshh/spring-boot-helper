### 12. Distributed Sessions with Redis — Scaling Form Login Across Multiple Server Instances

---

#### 12.1 The Problem — Sessions in Multi-Instance Deployments

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  THE STICKY SESSION PROBLEM (Why In-Memory Sessions Break with Load Balancers)               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Consider: 3 instances of your Spring Boot app behind a load balancer (Nginx, AWS ALB)      │
│                                                                                              │
│  ── SCENARIO: In-Memory Sessions (DEFAULT — BROKEN!) ────────────────────────               │
│                                                                                              │
│                          ┌─────────────────────┐                                            │
│                          │   Load Balancer       │                                            │
│                          │  (Nginx / AWS ALB)    │                                            │
│                          └────────┬──────────────┘                                            │
│                       ┌───────────┼───────────┐                                              │
│                       │           │           │                                              │
│                       ▼           ▼           ▼                                              │
│              ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                                │
│              │  Server 1    │ │  Server 2    │ │  Server 3    │                                │
│              │  (JVM Heap)  │ │  (JVM Heap)  │ │  (JVM Heap)  │                                │
│              │ ┌──────────┐│ │ ┌──────────┐│ │ ┌──────────┐│                                │
│              │ │Sessions: ││ │ │Sessions: ││ │ │Sessions: ││                                │
│              │ │ABC→john  ││ │ │(empty)   ││ │ │(empty)   ││                                │
│              │ └──────────┘│ │ └──────────┘│ │ └──────────┘│                                │
│              └─────────────┘ └─────────────┘ └─────────────┘                                │
│                                                                                              │
│  WHAT HAPPENS:                                                                               │
│                                                                                              │
│  1. User logs in → Request hits Server 1                                                    │
│     → Session ABC created in Server 1's heap memory                                         │
│     → Response: Set-Cookie: JSESSIONID=ABC                                                  │
│                                                                                              │
│  2. Next request → Load balancer sends to Server 2 (round-robin)                            │
│     → Cookie: JSESSIONID=ABC                                                                │
│     → Server 2 checks its heap: "ABC"? → NOT FOUND!                                        │
│     → SecurityContext is empty → user appears unauthenticated                               │
│     → 302 Redirect to /login 😱                                                             │
│     → User is confused — they just logged in!                                               │
│                                                                                              │
│  3. User logs in again → Request hits Server 3                                              │
│     → New session DEF created in Server 3's heap                                            │
│     → Next request hits Server 1 → "DEF"? NOT FOUND! → redirect to /login again 😱        │
│                                                                                              │
│  THE USER IS STUCK IN AN INFINITE LOGIN LOOP!                                               │
│                                                                                              │
│                                                                                              │
│  ── WORKAROUND 1: Sticky Sessions (NOT recommended) ─────────────────────────               │
│                                                                                              │
│  Configure load balancer to always route a user to the SAME server:                         │
│  User john → ALWAYS goes to Server 1                                                        │
│  User jane → ALWAYS goes to Server 2                                                        │
│                                                                                              │
│  PROBLEMS with sticky sessions:                                                              │
│  ├── If Server 1 crashes → john's session is LOST → must re-login                         │
│  ├── Uneven load distribution (one server may get all the heavy users)                     │
│  ├── Cannot do rolling deployments easily (restarting a server loses sessions)             │
│  └── Defeats the purpose of having multiple servers                                         │
│                                                                                              │
│                                                                                              │
│  ── SOLUTION: Centralized Session Store (Redis) ──────────────────────────────              │
│                                                                                              │
│  Store sessions in a SHARED Redis server that ALL instances can access:                     │
│                                                                                              │
│                          ┌─────────────────────┐                                            │
│                          │   Load Balancer       │                                            │
│                          └────────┬──────────────┘                                            │
│                       ┌───────────┼───────────┐                                              │
│                       │           │           │                                              │
│                       ▼           ▼           ▼                                              │
│              ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                                │
│              │  Server 1    │ │  Server 2    │ │  Server 3    │                                │
│              │  (no local   │ │  (no local   │ │  (no local   │                                │
│              │   sessions)  │ │   sessions)  │ │   sessions)  │                                │
│              └──────┬───────┘ └──────┬───────┘ └──────┬───────┘                                │
│                     │               │               │                                        │
│                     └───────────────┼───────────────┘                                        │
│                                     │                                                        │
│                                     ▼                                                        │
│                          ┌─────────────────────┐                                            │
│                          │   Redis Server        │                                            │
│                          │  ┌─────────────────┐ │                                            │
│                          │  │ ABC → {john,...} │ │                                            │
│                          │  │ DEF → {jane,...} │ │                                            │
│                          │  │ GHI → {bob,...}  │ │                                            │
│                          │  └─────────────────┘ │                                            │
│                          └─────────────────────┘                                            │
│                                                                                              │
│  NOW:                                                                                        │
│  1. User logs in → hits Server 1 → session ABC stored in Redis                             │
│  2. Next request → hits Server 2 → reads ABC from Redis → john authenticated ✓            │
│  3. Next request → hits Server 3 → reads ABC from Redis → john authenticated ✓            │
│  4. Server 1 crashes → Server 2 still finds ABC in Redis → user stays logged in ✅        │
│                                                                                              │
│  ANY server can handle ANY request because ALL sessions are in ONE shared Redis!            │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 12.2 What is Spring Session Redis?

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SPRING SESSION REDIS — HOW IT WORKS                                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Spring Session Data Redis replaces Tomcat's in-memory session store with Redis.            │
│  It works EXACTLY the same way as Spring Session JDBC, but instead of SQL queries           │
│  it uses Redis commands (SET, GET, DEL, EXPIRE).                                            │
│                                                                                              │
│  WHY REDIS instead of a SQL Database?                                                       │
│  ┌──────────────────────────┬──────────────────────────────────────────────┐               │
│  │  Feature                  │  JDBC (PostgreSQL)  vs  Redis                │               │
│  ├──────────────────────────┼──────────────────────────────────────────────┤               │
│  │  Read speed               │  ~1-5ms (disk I/O)  vs  ~0.1-0.5ms (RAM)   │               │
│  │  Write speed              │  ~2-10ms (disk)     vs  ~0.1-0.5ms (RAM)   │               │
│  │  EVERY request hits it    │  Heavy on DB        vs  Built for this     │               │
│  │  Built-in TTL/expiry      │  Manual cleanup     vs  Native EXPIRE      │               │
│  │  Data structure            │  Tables + SQL       vs  Key-Value (Hash)   │               │
│  │  Horizontal scaling        │  Limited             vs  Redis Cluster     │               │
│  │  Persistence               │  Always durable     vs  Optional (RDB/AOF)│               │
│  │  Session data queries      │  SQL (flexible)     vs  Limited            │               │
│  │  Ideal for                │  Audit + durability  vs  Speed + scale     │               │
│  └──────────────────────────┴──────────────────────────────────────────────┘               │
│                                                                                              │
│  Redis is the INDUSTRY STANDARD for distributed session storage because:                    │
│  • Sessions are accessed on EVERY HTTP request (read-heavy)                                 │
│  • Redis stores everything in RAM — sub-millisecond reads                                   │
│  • Redis has built-in key expiration (TTL) — sessions auto-expire                          │
│  • No tables, no schema, no migrations — just key-value pairs                              │
│  • Redis Cluster supports horizontal scaling for millions of sessions                       │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 12.3 How Redis Stores Spring Sessions

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  REDIS DATA STRUCTURE FOR SPRING SESSIONS                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Spring Session stores each session as a Redis HASH with multiple fields:                   │
│                                                                                              │
│  Key: spring:session:sessions:<SESSION_ID>                                                  │
│  Type: HASH                                                                                  │
│  TTL: 2700 seconds (45 minutes) — auto-expires!                                             │
│                                                                                              │
│  redis> HGETALL spring:session:sessions:x9y8z7w6-v5u4-t3s2-r1q0                            │
│  ┌────────────────────────────────────────────────────────────────────────────────────┐     │
│  │  Field                                │  Value                                     │     │
│  ├────────────────────────────────────────┼───────────────────────────────────────────┤     │
│  │  creationTime                          │  1745920800000                              │     │
│  │  lastAccessedTime                      │  1745921100000                              │     │
│  │  maxInactiveInterval                   │  2700                                       │     │
│  │  sessionAttr:SPRING_SECURITY_CONTEXT   │  \xAC\xED\x00\x05... (serialized bytes)   │     │
│  │  principalName                         │  john                                       │     │
│  └────────────────────────────────────────┴───────────────────────────────────────────┘     │
│                                                                                              │
│  Additional keys Spring Session creates:                                                    │
│  ┌────────────────────────────────────────────────────────────────────────────────────┐     │
│  │  spring:session:sessions:expires:<SESSION_ID>     → empty string, with TTL        │     │
│  │  spring:session:expirations:<EXPIRE_TIME_BUCKET>  → set of session IDs expiring   │     │
│  │  spring:session:index:...:findByPrincipalName:john → set of john's session IDs    │     │
│  └────────────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                              │
│  Redis commands used by Spring Session:                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────────────┐     │
│  │  CREATE session:  HMSET spring:session:sessions:<id> field1 val1 field2 val2 ...  │     │
│  │                   EXPIRE spring:session:sessions:<id> 2700                        │     │
│  │                                                                                    │     │
│  │  READ session:    HGETALL spring:session:sessions:<id>                            │     │
│  │                                                                                    │     │
│  │  UPDATE session:  HMSET spring:session:sessions:<id> lastAccessedTime <now>       │     │
│  │                   EXPIRE spring:session:sessions:<id> 2700 (reset TTL)            │     │
│  │                                                                                    │     │
│  │  DELETE session:  DEL spring:session:sessions:<id>                                │     │
│  │                   DEL spring:session:sessions:expires:<id>                        │     │
│  │                                                                                    │     │
│  │  EXPIRATION:      Redis automatically deletes the key when TTL reaches 0!         │     │
│  │                   No cleanup cron job needed (unlike JDBC)!                       │     │
│  └────────────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 12.4 Complete Implementation — Spring Session Redis

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

    <!-- ★ Spring Session Data Redis — THE KEY DEPENDENCY ★ -->
    <dependency>
        <groupId>org.springframework.session</groupId>
        <artifactId>spring-session-data-redis</artifactId>
    </dependency>

    <!-- ★ Spring Boot Starter Data Redis (includes Lettuce client) ★ -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- PostgreSQL Driver (for user data — sessions go to Redis) -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

**Step 2: `application.properties` — Configure Spring Session Redis**

```properties
# ═══════════════════════════════════════════════════════════════════════════════
#  DATABASE CONNECTION (for user data — UserAuthEntity, etc.)
# ═══════════════════════════════════════════════════════════════════════════════
spring.datasource.url=jdbc:postgresql://localhost:5432/myapp
spring.datasource.username=postgres
spring.datasource.password=secret

# ═══════════════════════════════════════════════════════════════════════════════
#  ★ REDIS CONNECTION (for session storage) ★
# ═══════════════════════════════════════════════════════════════════════════════
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=            # Empty if no password (development)
# spring.data.redis.password=myRedisPassword   # Production: always set a password!
spring.data.redis.database=0           # Redis DB index (0-15)
spring.data.redis.timeout=2000ms       # Connection timeout
spring.data.redis.lettuce.pool.max-active=8    # Max connections in pool
spring.data.redis.lettuce.pool.max-idle=8      # Max idle connections
spring.data.redis.lettuce.pool.min-idle=2      # Min idle connections

# For Redis Sentinel (high availability):
# spring.data.redis.sentinel.master=mymaster
# spring.data.redis.sentinel.nodes=host1:26379,host2:26379,host3:26379

# For Redis Cluster:
# spring.data.redis.cluster.nodes=host1:6379,host2:6379,host3:6379

# ═══════════════════════════════════════════════════════════════════════════════
#  ★ SPRING SESSION REDIS CONFIGURATION ★
# ═══════════════════════════════════════════════════════════════════════════════

# Tell Spring Session to use Redis as the session store
spring.session.store-type=redis

# Redis key namespace (prefix for all session keys)
# Default: "spring:session"
spring.session.redis.namespace=spring:session

# Flush mode: when to write session changes to Redis
# ON_SAVE = write when response is committed (default — batches changes)
# IMMEDIATE = write immediately on every setAttribute() call
spring.session.redis.flush-mode=on-save

# Session timeout
server.servlet.session.timeout=45m

# ═══════════════════════════════════════════════════════════════════════════════
#  SESSION COOKIE CONFIGURATION
# ═══════════════════════════════════════════════════════════════════════════════
server.servlet.session.cookie.name=SESSION
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=Lax
```

**Step 3: `UserAuthEntity` — MUST implement Serializable (same as JDBC)**

```java
@Entity
@Table(name = "users")
public class UserAuthEntity implements UserDetails, Serializable {
    // Same as Section 11.5 — Serializable is required for Redis serialization too!
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    // ... same UserDetails methods ...
}
```

**Step 4: `SecurityConfig` — No changes needed! (Same as always)**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // ★ Your SecurityConfig is IDENTICAL whether you use:
    //   - In-memory sessions (default Tomcat)
    //   - Spring Session JDBC (PostgreSQL)
    //   - Spring Session Redis
    //
    // Spring Session transparently replaces the session layer.
    // Spring Security doesn't know or care WHERE sessions are stored.

    // ... exact same config as Section 10.1.1 ...
}
```

**Step 5: `docker-compose.yaml` — Redis + PostgreSQL for Development**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: myapp-postgres
    environment:
      POSTGRES_DB: myapp
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: myapp-redis
    ports:
      - "6379:6379"
    # Production: add password
    # command: redis-server --requirepass myRedisPassword
    volumes:
      - redis-data:/data

volumes:
  postgres-data:
  redis-data:
```

---

#### 12.5 Complete Architecture Diagram — Multi-Instance with Redis

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  PRODUCTION ARCHITECTURE — SPRING SESSION REDIS WITH LOAD BALANCER                           │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│           ┌──────────┐                                                                      │
│           │  Browser  │                                                                      │
│           │  Cookie:  │                                                                      │
│           │  SESSION= │                                                                      │
│           │  x9y8z7.. │                                                                      │
│           └─────┬─────┘                                                                      │
│                 │                                                                             │
│                 ▼                                                                             │
│    ┌──────────────────────────┐                                                             │
│    │   Load Balancer           │                                                             │
│    │   (Nginx / AWS ALB)       │                                                             │
│    │   Round-Robin / Least     │                                                             │
│    │   Connections             │                                                             │
│    │                           │                                                             │
│    │   NO sticky sessions      │  ← Not needed! Any server can handle any request           │
│    │   needed!                 │                                                             │
│    └──────┬──────┬──────┬──────┘                                                             │
│           │      │      │                                                                    │
│           ▼      ▼      ▼                                                                    │
│    ┌──────────┐ ┌──────────┐ ┌──────────┐                                                  │
│    │Server 1  │ │Server 2  │ │Server 3  │                                                  │
│    │(Spring   │ │(Spring   │ │(Spring   │                                                  │
│    │ Boot)    │ │ Boot)    │ │ Boot)    │                                                  │
│    │          │ │          │ │          │                                                  │
│    │ Filters: │ │ Filters: │ │ Filters: │                                                  │
│    │ Session  │ │ Session  │ │ Session  │                                                  │
│    │ Repo     │ │ Repo     │ │ Repo     │                                                  │
│    │ Filter   │ │ Filter   │ │ Filter   │                                                  │
│    │ ↓        │ │ ↓        │ │ ↓        │                                                  │
│    │ Security │ │ Security │ │ Security │                                                  │
│    │ Filters  │ │ Filters  │ │ Filters  │                                                  │
│    │          │ │          │ │          │                                                  │
│    │ NO local │ │ NO local │ │ NO local │                                                  │
│    │ sessions!│ │ sessions!│ │ sessions!│                                                  │
│    └────┬─────┘ └────┬─────┘ └────┬─────┘                                                  │
│         │            │            │                                                          │
│         │     ALL connect to     │                                                          │
│         │     the SAME Redis     │                                                          │
│         └────────────┼────────────┘                                                          │
│                      │                                                                       │
│              ┌───────▼───────┐                                                              │
│              │               │                                                              │
│         ┌────▼────┐    ┌────▼────┐                                                          │
│         │  Redis   │    │ Postgres │                                                          │
│         │ (Sessions│    │ (Users,  │                                                          │
│         │  ONLY)   │    │  App     │                                                          │
│         │          │    │  Data)   │                                                          │
│         │ Keys:    │    │          │                                                          │
│         │ spring:  │    │ Tables:  │                                                          │
│         │ session: │    │ users    │                                                          │
│         │ sessions:│    │ orders   │                                                          │
│         │ x9y8z7.. │    │ products │                                                          │
│         │ → {john} │    │          │                                                          │
│         │          │    │          │                                                          │
│         │ TTL:2700s│    │          │                                                          │
│         └──────────┘    └──────────┘                                                          │
│                                                                                              │
│                                                                                              │
│  FLOW:                                                                                       │
│  1. User logs in → hits Server 1                                                            │
│     → Server 1 authenticates (DB query for user)                                            │
│     → Server 1 stores session in Redis: SET spring:session:sessions:x9y8z7 {...}            │
│     → Response: Set-Cookie: SESSION=x9y8z7                                                  │
│                                                                                              │
│  2. Next request → load balancer sends to Server 2                                          │
│     → Cookie: SESSION=x9y8z7                                                                │
│     → Server 2's SessionRepositoryFilter:                                                   │
│       → Redis: HGETALL spring:session:sessions:x9y8z7                                      │
│       → Returns: {john, ROLE_USER, authenticated=true}                                      │
│     → SecurityContextHolder.setContext(ctx) → john is authenticated ✓                      │
│     → Server 2 handles request normally!                                                    │
│                                                                                              │
│  3. Server 1 crashes → load balancer routes to Server 2 or 3                                │
│     → Session x9y8z7 is still in Redis → user stays logged in ✅                           │
│                                                                                              │
│  4. Deploy new version: restart servers one by one                                           │
│     → Sessions survive in Redis → zero downtime for users ✅                                │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 12.6 Verify Redis Sessions — Redis CLI Commands

```bash
# Connect to Redis
redis-cli

# List all session keys
KEYS spring:session:*

# View a specific session
HGETALL spring:session:sessions:x9y8z7w6-v5u4-t3s2-r1q0

# Check session TTL (time to live in seconds)
TTL spring:session:sessions:x9y8z7w6-v5u4-t3s2-r1q0
# Returns: 2650 (means ~44 minutes left before expiry)

# Find all sessions for a user
SMEMBERS spring:session:index:org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME:john

# Manually delete a session (force logout)
DEL spring:session:sessions:x9y8z7w6-v5u4-t3s2-r1q0

# Count active sessions
KEYS spring:session:sessions:* | wc -l
# Or better:
SCAN 0 MATCH spring:session:sessions:* COUNT 100

# Monitor Redis commands in real-time (see session reads/writes)
MONITOR
# Then make HTTP requests to your app — you'll see HGETALL, HMSET, EXPIRE commands
```

---

#### 12.7 Comparison — In-Memory vs JDBC vs Redis

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SESSION STORAGE COMPARISON — WHICH ONE TO USE?                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────┬───────────────────┬───────────────────┬─────────────────────────────┐│
│  │  Feature          │  In-Memory        │  JDBC (PostgreSQL)│  Redis                       ││
│  │                   │  (Default Tomcat)  │                   │                               ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Dependency       │  None (built-in)   │  spring-session-  │  spring-session-data-redis  ││
│  │                   │                    │  jdbc              │  + spring-boot-starter-     ││
│  │                   │                    │                   │    data-redis               ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Storage          │  JVM heap memory   │  SQL tables        │  Redis in-memory store      ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Read latency     │  ~0.001ms          │  ~1-5ms            │  ~0.1-0.5ms                 ││
│  │                   │  (HashMap.get())   │  (SELECT query)   │  (HGETALL)                  ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Survives restart │  ❌ NO             │  ✅ YES            │  ✅ YES (with RDB/AOF)      ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Multi-instance   │  ❌ NO             │  ✅ YES            │  ✅ YES                      ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Session expiry   │  Background thread │  Cron job (1 min) │  Native TTL (automatic)    ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Queryable         │  ❌ NO             │  ✅ SQL queries    │  Limited (KEYS/SCAN)       ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Audit trail       │  ❌ NO             │  ✅ YES            │  ❌ NO (keys expire)         ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Extra infra       │  None              │  Already have DB   │  Need Redis server         ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Serializable req  │  ❌ NO             │  ✅ YES            │  ✅ YES                      ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Cookie name       │  JSESSIONID        │  SESSION           │  SESSION                    ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  Config property   │  (default)         │  spring.session.   │  spring.session.            ││
│  │                   │                    │  store-type=jdbc   │  store-type=redis           ││
│  ├──────────────────┼───────────────────┼───────────────────┼─────────────────────────────┤│
│  │  BEST FOR          │  Development,      │  Single server +   │  Multi-server production,  ││
│  │                   │  Single server,    │  Need audit trail, │  Microservices,             ││
│  │                   │  Prototyping       │  Already have DB   │  Cloud-native apps          ││
│  └──────────────────┴───────────────────┴───────────────────┴─────────────────────────────┘│
│                                                                                              │
│                                                                                              │
│  DECISION GUIDE:                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Single server, simple app?                                                   │           │
│  │  └── Use DEFAULT in-memory sessions (zero config)                            │           │
│  │                                                                               │           │
│  │  Single server, need sessions to survive restarts?                           │           │
│  │  └── Use Spring Session JDBC (you already have a database)                   │           │
│  │                                                                               │           │
│  │  Multiple servers behind a load balancer?                                    │           │
│  │  └── Use Spring Session Redis (industry standard)                            │           │
│  │                                                                               │           │
│  │  Multiple servers + need audit trail of sessions?                            │           │
│  │  └── Use Spring Session JDBC (queryable with SQL)                            │           │
│  │                                                                               │           │
│  │  High traffic (10K+ concurrent users)?                                       │           │
│  │  └── Use Spring Session Redis (sub-ms reads, native TTL)                     │           │
│  │                                                                               │           │
│  │  Microservices with shared auth?                                             │           │
│  │  └── Consider JWT instead of sessions (truly stateless)                      │           │
│  │      OR use Spring Session Redis if you need session revocation              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```
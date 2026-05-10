### What is Second-Level Caching

The second-level (L2) cache is a **SessionFactory-scoped** cache shared across all `EntityManager` / `Session` instances in the application. Unlike the first-level cache (which dies with the EntityManager), the L2 cache survives across transactions.

```text
em.find(User.class, 1L)
         │
         v
┌─────────────────────────────────────────────────────────────────────┐
│  1st-Level Cache (Persistence Context)                              │
│  Scoped to: this EntityManager / this transaction                   │
│                                                                     │
│  User(id=1) found?                                                  │
│       │                                                             │
│    YES → return immediately (same object reference)                 │
│       │                                                             │
│    NO ↓                                                             │
├─────────────────────────────────────────────────────────────────────┤
│  2nd-Level Cache (SessionFactory-level)                             │
│  Scoped to: entire application, shared across all EntityManagers    │
│                                                                     │
│  User(id=1) found?                                                  │
│       │                                                             │
│    YES → hydrate entity from cached data, put in L1 cache, return  │
│       │                                                             │
│    NO ↓                                                             │
├─────────────────────────────────────────────────────────────────────┤
│  DATABASE                                                           │
│  SELECT * FROM users WHERE id = 1                                   │
│  → put result in L2 cache AND L1 cache, return entity               │
└─────────────────────────────────────────────────────────────────────┘
```

**Why use it?**

- Reduces database load for **read-heavy, rarely-changing** data (countries, categories, config, roles).
- Avoids repeated SELECT statements across different transactions/requests for the same entity.
- The L1 cache only helps within a single transaction. The L2 cache helps across all transactions.

**When to use it?**

| Good fit | Bad fit |
|---|---|
| Reference data (countries, currencies, roles) | Frequently updated data (orders, inventory) |
| Read-heavy, write-rare entities | Data that changes per request |
| Single-instance or single-DB apps | Multi-instance apps without distributed cache |
| Entities loaded by primary key often | Complex queries (L2 cache is entity-level, not query-level by default) |

**Code example**

```java
// Transaction A
@Transactional
public void txnA() {
    // L1 miss, L2 miss → SELECT from DB → stored in L2 and L1
    User user = entityManager.find(User.class, 1L);
}
// EntityManager A closes → L1 cache destroyed, L2 cache still has User(id=1)

// Transaction B (different EntityManager, maybe different thread)
@Transactional
public void txnB() {
    // L1 miss (new EM), L2 HIT → no SQL, entity hydrated from L2 cache
    User user = entityManager.find(User.class, 1L);
}
```

---

### How to Use It — Maven Dependencies

The L2 cache requires three layers:

```text
+------------------+      +-------------------+      +---------------------+
| Hibernate L2     | ---> | JCache API        | ---> | Cache Provider      |
| Cache SPI        |      | (JSR-107)         |      | (EhCache/Caffeine/  |
|                  |      |                   |      |  Hazelcast)         |
| hibernate-jcache |      | cache-api         |      | ehcache / caffeine  |
+------------------+      +-------------------+      +---------------------+
```

**Maven dependencies**

```xml
<!-- 1. Hibernate JCache integration — bridges Hibernate's L2 cache SPI to JCache API -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-jcache</artifactId>
</dependency>

<!-- 2. JCache API (JSR-107) — standard caching interface -->
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
</dependency>

<!-- 3. Pick ONE cache provider implementation -->

<!-- Option A: EhCache (most popular with Hibernate, feature-rich, XML-configurable) -->
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
    <classifier>jakarta</classifier>
</dependency>

<!-- Option B: Caffeine (lightweight, high-performance, in-memory only) -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>jcache</artifactId>
</dependency>

<!-- Option C: Hazelcast (distributed cache, works across multiple app instances) -->
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast</artifactId>
</dependency>
```

### Purpose of Each Import

| Dependency | What it does | Why you need it |
|---|---|---|
| `hibernate-jcache` | Hibernate's adapter that plugs into the JCache API | Tells Hibernate "use JCache as the L2 cache backend" |
| `cache-api` (JSR-107) | Standard Java caching interfaces (`CacheManager`, `Cache`, `CacheConfiguration`) | Common API so you can swap providers without changing code |
| `ehcache` | Actual cache implementation — stores data in heap/off-heap/disk | The engine that holds cached entities in memory |
| `caffeine` | Lightweight in-memory cache with high hit rates | Alternative to EhCache, best for single-instance apps |
| `hazelcast` | Distributed in-memory cache with cluster sync | Use when multiple app instances need to share cache state |

```text
Hibernate ──> hibernate-jcache ──> JCache API (cache-api) ──> EhCache / Caffeine / Hazelcast
              (bridge)              (standard interface)       (actual storage engine)
```

---

### Configuration in application.yml

**Using EhCache**

```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Enable L2 cache
        cache:
          use_second_level_cache: true
          use_query_cache: true          # optional: also cache JPQL/HQL query results
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        # Point to EhCache config file
        javax:
          cache:
            provider: org.ehcache.jsr107.EhcacheCachingProvider
            uri: classpath:ehcache.xml
```

**ehcache.xml** (in `src/main/resources/`)

```xml
<config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://www.ehcache.org/v3"
        xsi:noNamespaceSchemaLocation="http://www.ehcache.org/schema/ehcache-core-3.0.xsd">

    <!-- Default cache template -->
    <cache-template name="default">
        <expiry>
            <ttl unit="minutes">30</ttl>
        </expiry>
        <heap unit="entries">1000</heap>
    </cache-template>

    <!-- Entity-specific cache region -->
    <cache alias="com.example.entity.User" uses-template="default">
        <heap unit="entries">500</heap>
    </cache>

    <cache alias="com.example.entity.Country" uses-template="default">
        <expiry>
            <ttl unit="hours">24</ttl>
        </expiry>
        <heap unit="entries">300</heap>
    </cache>
</config>
```

**Using Caffeine (simpler, no XML)**

```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        javax:
          cache:
            provider: com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
```

---

### Annotations and Usage

**Entity-level annotations**

```java
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "users")
@Cacheable                                                     // JPA standard: marks entity as cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE,           // Hibernate-specific: concurrency strategy
       region = "com.example.entity.User")                     // cache region name (maps to ehcache.xml alias)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToMany(mappedBy = "user")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)        // cache the collection too
    private List<Address> addresses;
}
```

| Annotation | Source | Purpose |
|---|---|---|
| `@Cacheable` | JPA (`jakarta.persistence`) | Marks the entity as eligible for L2 caching |
| `@Cache` | Hibernate (`org.hibernate.annotations`) | Configures the concurrency strategy and cache region |
| `usage` | — | Defines how concurrent reads/writes to cached data are handled |
| `region` | — | Maps to a named cache in ehcache.xml. Defaults to the fully qualified class name |

---

### CacheConcurrencyStrategy — All Types Explained

```text
+─────────────────+───────────────+─────────────────+──────────────────────────────────────────────+
│ Strategy        │ Read Safe     │ Write Safe      │ Use Case                                      │
+─────────────────+───────────────+─────────────────+──────────────────────────────────────────────+
│ READ_ONLY       │ ✅            │ ❌ (never write) │ Truly immutable data: countries, zip codes     │
│ NONSTRICT_      │ ✅            │ ⚠️ eventual     │ Rarely updated, stale OK: user profiles        │
│ READ_WRITE      │               │                 │                                                │
│ READ_WRITE      │ ✅            │ ✅ (soft lock)   │ Most entities: users, products, categories     │
│ TRANSACTIONAL   │ ✅            │ ✅ (XA/JTA)      │ Distributed txn with JTA: banking, payments    │
+─────────────────+───────────────+─────────────────+──────────────────────────────────────────────+
```

**1. READ_ONLY**

- Entity is **never updated** after initial insert.
- Cache never needs invalidation for writes.
- Highest performance, lowest overhead.

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Country {
    @Id private String code;    // "US", "IN", "DE"
    private String name;        // never changes
}
```

**Business context**: Country lists, currency codes, static configuration, enum-like lookup tables.

**2. NONSTRICT_READ_WRITE**

- Reads are cached, writes invalidate the cache **after** commit.
- Between the write commit and cache invalidation, other transactions may see **stale** data (brief window).
- No locking, so highest write throughput among writable strategies.

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserProfile {
    @Id private Long id;
    private String bio;         // rarely updated, stale for a moment is OK
    private String avatarUrl;
}
```

**Business context**: User profiles, blog posts, settings that update infrequently and brief staleness is acceptable.

**3. READ_WRITE**

- Uses **soft locks** to prevent stale reads during writes.
- When a transaction updates an entity, the cache entry is locked. Other transactions see a cache miss and go to the DB until the lock is released on commit.
- **No stale reads**, but slightly more overhead than NONSTRICT.

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {
    @Id private Long id;
    private String name;
    private BigDecimal price;   // price updates must be immediately visible
}
```

```text
Write flow with READ_WRITE:

Thread A: UPDATE Product(id=1) price = 99.99
     │
     ├─ Soft-lock cache entry for Product(id=1)
     ├─ Execute UPDATE SQL
     ├─ Commit transaction
     └─ Release soft-lock, update cache with new value

Thread B: find(Product.class, 1L) during soft-lock
     │
     ├─ L2 cache: entry is soft-locked → treat as cache MISS
     ├─ Go to DB → gets latest value
     └─ (no stale read)
```

**Business context**: Products, inventory, employee records, anything that updates sometimes and stale reads are unacceptable.

**4. TRANSACTIONAL**

- Uses **full XA/JTA transactions** to coordinate cache and database.
- Cache participates in the same distributed transaction as the DB.
- Requires a JTA-capable cache provider (e.g., EhCache with JTA, Infinispan).
- Heaviest overhead, strongest consistency.

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class BankAccount {
    @Id private Long id;
    private BigDecimal balance;  // absolute consistency required
}
```

**Business context**: Financial systems, banking, payment processing where cache and DB must always be in sync.

### Why READ_WRITE is Used for Most Use Cases

```text
                      READ_ONLY   NONSTRICT_RW   READ_WRITE   TRANSACTIONAL
───────────────────   ─────────   ────────────   ──────────   ─────────────
Performance           ★★★★★       ★★★★           ★★★          ★★
Consistency           immutable   eventual       strong       strongest
Write support         ❌           ✅              ✅            ✅
Stale reads           N/A         possible       prevented    prevented
Infrastructure        minimal     minimal        minimal      JTA required
───────────────────   ─────────   ────────────   ──────────   ─────────────
```

- Most entities **do** get updated occasionally, so `READ_ONLY` is too restrictive.
- Stale data is usually unacceptable in business apps, so `NONSTRICT_READ_WRITE` is risky.
- `TRANSACTIONAL` requires JTA infrastructure, which most Spring Boot apps do not use.
- `READ_WRITE` offers the **best balance**: strong consistency with soft locks, no JTA required, works with standard RESOURCE_LOCAL transactions.

---

### How Update and Cache Invalidation Happens

**Update flow (READ_WRITE strategy)**

```text
Step 1: @Transactional method starts
        EntityManager opened, L1 cache empty

Step 2: user = em.find(User.class, 1L)
        L1 miss → L2 hit → entity hydrated into L1 as MANAGED

Step 3: user.setName("Bob")
        L1 dirty checking detects change

Step 4: Method returns → flush
        ├─ Hibernate generates: UPDATE users SET name='Bob' WHERE id=1
        ├─ BEFORE transaction commit: soft-lock L2 cache entry for User(id=1)
        │     (other threads see cache miss during lock)
        ├─ JDBC commit
        └─ AFTER commit: update L2 cache entry with new data, release soft-lock

Step 5: Next transaction calls find(User.class, 1L)
        L2 cache has updated value → cache HIT with fresh data
```

```text
Timeline:
─────────────────────────────────────────────────────
  T1: begin          flush       commit      after-commit
       │               │           │              │
       │               │        soft-lock      update L2
       │               │        L2 entry       release lock
       │               │           │              │
  T2:                       find(1L)
                            L2 locked → cache MISS → go to DB
                                                      │
  T3:                                              find(1L)
                                                   L2 updated → cache HIT (fresh data)
```

**Delete flow**

```text
em.remove(user)  →  flush  →  DELETE FROM users WHERE id=1  →  commit  →  EVICT from L2 cache
```

**Manual cache eviction**

```java
// Evict a specific entity from L2 cache
sessionFactory.getCache().evict(User.class, userId);

// Evict all User entities from L2 cache
sessionFactory.getCache().evict(User.class);

// Evict everything from L2 cache
sessionFactory.getCache().evictAllRegions();
```

---

### Multi-Instance: L2 Cache vs Redis with @Cacheable

In a multi-instance environment (multiple Spring Boot instances behind a load balancer, all connecting to the same PostgreSQL), the choice depends on the architecture.

```text
Scenario: 3 Spring Boot instances → 1 PostgreSQL

┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Instance A   │    │ Instance B   │    │ Instance C   │
│              │    │              │    │              │
│ L2 Cache (A) │    │ L2 Cache (B) │    │ L2 Cache (C) │
│ (in-memory)  │    │ (in-memory)  │    │ (in-memory)  │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────┴──────┐
                    │ PostgreSQL  │
                    └─────────────┘

Problem: Instance A updates User(id=1).
         Instance A's L2 cache is updated.
         Instance B and C still have STALE data in their L2 caches!
```

**Option 1: Hibernate L2 Cache with distributed provider (Hazelcast/Infinispan)**

```text
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Instance A   │    │ Instance B   │    │ Instance C   │
│              │    │              │    │              │
│ L2 Cache ────┼────┼── L2 Cache ──┼────┼── L2 Cache   │
│ (Hazelcast   │    │ (Hazelcast   │    │ (Hazelcast   │
│  cluster)    │    │  cluster)    │    │  cluster)    │
└──────────────┘    └──────────────┘    └──────────────┘
     Hazelcast syncs cache across all instances automatically
```

- Works, but adds complexity (Hazelcast cluster management, network overhead).
- Only works for entity-level caching (by primary key).

**Option 2: Redis with Spring @Cacheable (recommended for multi-instance)**

```text
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Instance A   │    │ Instance B   │    │ Instance C   │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────┴──────┐
                    │    Redis    │    ← single source of truth for cache
                    │  (shared)   │
                    └──────┬──────┘
                           │
                    ┌──────┴──────┐
                    │ PostgreSQL  │
                    └─────────────┘

Instance A updates User(id=1) → updates Redis → all instances see fresh data
```

**Redis + @Cacheable code example**

```yaml
# application.yml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Cacheable(value = "users", key = "#id")           // cache on read
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    @CachePut(value = "users", key = "#user.id")       // update cache on write
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#id")           // evict from cache on delete
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @CacheEvict(value = "users", allEntries = true)     // clear ALL user cache
    public void clearUserCache() {}
}
```

**Comparison table**

| | Hibernate L2 Cache (EhCache) | Hibernate L2 Cache (Hazelcast) | Redis + @Cacheable |
|---|---|---|---|
| **Scope** | Single JVM | Distributed (cluster) | Centralized (shared server) |
| **Multi-instance safe** | ❌ stale data | ✅ cluster sync | ✅ single source |
| **Cache level** | Entity-level (by PK) | Entity-level (by PK) | Any level (method result, DTO, custom key) |
| **Integration** | Deep Hibernate integration, automatic | Deep Hibernate integration, auto-sync | Spring Cache abstraction, manual annotations |
| **Flexibility** | Only works with `em.find()` by PK | Only works with `em.find()` by PK | Cache any method result, any key pattern |
| **Infrastructure** | None (in-process) | Hazelcast cluster | Redis server |
| **Best for** | Single-instance apps | Multi-instance, entity-heavy | Multi-instance, flexible caching |

**Recommendation for multi-instance**

```text
Single instance app               → Hibernate L2 Cache with EhCache/Caffeine
                                     (simple, zero infrastructure, automatic)

Multi-instance + entity PK lookups → Hibernate L2 Cache with Hazelcast
                                     (if you want transparent ORM-level caching)

Multi-instance + flexible caching  → Redis + @Cacheable / @CachePut / @CacheEvict
                                     (recommended — works for any method, any key,
                                      DTOs, aggregated results, not just entities)

Multi-instance + both needs        → Redis for application-level caching (@Cacheable)
                                     + Hibernate L2 with Hazelcast for entity-level
                                     (maximum performance, highest complexity)
```

For most production multi-instance Spring Boot apps connecting to PostgreSQL: **use Redis with `@Cacheable`**. It gives you a single shared cache, works for any type of result (not just JPA entities), and is simpler to reason about than distributed Hibernate L2 caching.

---


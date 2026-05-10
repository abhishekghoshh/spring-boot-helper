### What Is a Native Query?

A **Native Query** is a raw SQL query that you write directly in the database's SQL dialect (MySQL, PostgreSQL, Oracle, etc.) instead of JPQL. It bypasses Hibernate's entity-based query translation and sends the SQL **directly to the database**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JPQL vs Native Query — The Key Difference:                                      │
│                                                                                  │
│  JPQL:                                                                           │
│    @Query("SELECT e FROM Employee e WHERE e.salary > :sal")                      │
│    → Works on ENTITY names and FIELD names                                       │
│    → Hibernate translates to SQL for YOUR database dialect                       │
│    → DB-independent: same query works on MySQL, PostgreSQL, Oracle               │
│    → Goes through Hibernate's query parser → entity model → SQL generator        │
│                                                                                  │
│  Native Query:                                                                   │
│    @Query(value = "SELECT * FROM employees WHERE salary > :sal",                 │
│           nativeQuery = true)                                                    │
│    → Works on TABLE names and COLUMN names                                       │
│    → SQL goes DIRECTLY to the database — no Hibernate translation               │
│    → DB-DEPENDENT: query may break if you switch databases                      │
│    → Bypasses Hibernate's query parser — you write the exact SQL                 │
│                                                                                  │
│  Flow comparison:                                                                │
│                                                                                  │
│  JPQL:                                                                           │
│    @Query("SELECT e FROM Employee e")                                            │
│       │                                                                          │
│       v                                                                          │
│    Hibernate JPQL Parser → reads entity metadata → generates SQL                │
│       │                                                                          │
│       v                                                                          │
│    SQL: SELECT e.id, e.emp_name, e.salary FROM employees e                       │
│       │                                                                          │
│       v                                                                          │
│    JDBC → Database                                                               │
│                                                                                  │
│  Native Query:                                                                   │
│    @Query(value = "SELECT * FROM employees", nativeQuery = true)                 │
│       │                                                                          │
│       v                                                                          │
│    Spring sends SQL DIRECTLY to JDBC (NO Hibernate JPQL parsing)                │
│       │                                                                          │
│       v                                                                          │
│    JDBC → Database                                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Native Query Is Plain SQL — Database Dependent

Since native queries use raw SQL with actual table and column names, they are **tied to the specific database**. If you switch from MySQL to PostgreSQL (or vice versa), your native queries may break because SQL dialects differ.

**Entity and Database Table:**

```java
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "active")
    private Boolean active;

    // getters, setters, constructors
}
```

```text
Database Table — user_details:

  ┌────┬───────────┬──────────────────┬────────────────┬────────┐
  │ id │ user_name │ email            │ phone          │ active │
  ├────┼───────────┼──────────────────┼────────────────┼────────┤
  │  1 │ Alice     │ alice@ex.com     │ +1-555-0101    │ true   │
  │  2 │ Bob       │ bob@ex.com       │ +1-555-0102    │ true   │
  │  3 │ Charlie   │ charlie@ex.com   │ +1-555-0103    │ false  │
  │  4 │ Diana     │ diana@ex.com     │ +1-555-0104    │ true   │
  └────┴───────────┴──────────────────┴────────────────┴────────┘
```

```java
// Repository — Native Query uses TABLE and COLUMN names
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // JPQL — uses entity field name "name" (Java field)
    @Query("SELECT u FROM UserDetails u WHERE u.name = :name")
    List<UserDetails> findByNameJpql(@Param("name") String name);
    // Hibernate generates: SELECT u.id, u.user_name, u.email, u.phone, u.active
    //                      FROM user_details u WHERE u.user_name = ?

    // Native Query — uses column name "user_name" (database column)
    @Query(value = "SELECT * FROM user_details WHERE user_name = :name",
           nativeQuery = true)
    List<UserDetails> findByNameNative(@Param("name") String name);
    // SQL sent directly: SELECT * FROM user_details WHERE user_name = ?
    // NO Hibernate translation — this IS the final SQL.
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why Native Query is DB-dependent — real examples:                                │
│                                                                                  │
│  MySQL:                                                                          │
│    SELECT * FROM user_details LIMIT 10 OFFSET 0                                  │
│    SELECT IF(active, 'Yes', 'No') FROM user_details                              │
│    SELECT JSON_EXTRACT(metadata, '$.key') FROM user_details                      │
│                                                                                  │
│  PostgreSQL:                                                                     │
│    SELECT * FROM user_details LIMIT 10 OFFSET 0  (same — lucky!)                │
│    SELECT CASE WHEN active THEN 'Yes' ELSE 'No' END FROM user_details            │
│    SELECT metadata->>'key' FROM user_details   (JSONB operator)                  │
│                                                                                  │
│  Oracle:                                                                         │
│    SELECT * FROM user_details WHERE ROWNUM <= 10  (no LIMIT!)                    │
│    SELECT DECODE(active, 1, 'Yes', 'No') FROM user_details                      │
│    SELECT JSON_VALUE(metadata, '$.key') FROM user_details                        │
│                                                                                  │
│  If you wrote a MySQL native query with LIMIT and later switched to Oracle:      │
│  → Query BREAKS at runtime. Oracle doesn't have LIMIT.                           │
│                                                                                  │
│  JPQL doesn't have this problem:                                                 │
│    JPQL: SELECT u FROM UserDetails u  (with Pageable)                            │
│    → MySQL:  ... LIMIT 10 OFFSET 0                                               │
│    → Oracle: ... FETCH FIRST 10 ROWS ONLY                                        │
│    → Hibernate handles the dialect difference automatically.                     │
│                                                                                  │
│  RULE: If your application might switch databases → prefer JPQL.                 │
│        If your application is locked to one DB → native query is fine.           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### No Caching, Lazy Loading, or Entity Lifecycle Management

When you use a native query, Hibernate's **Persistence Context** features are limited. The result may be mapped to entities, but certain behaviors are bypassed or weakened.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Native Query BYPASSES:                                                     │
│                                                                                  │
│  1. NO JPQL QUERY CACHE:                                                         │
│     JPQL queries can be cached by Hibernate's query cache (2nd-level cache).     │
│     Native queries are NOT eligible for JPQL query caching.                      │
│     Each call hits the database directly.                                        │
│                                                                                  │
│  2. LAZY LOADING MAY NOT WORK AS EXPECTED:                                       │
│     If your native SELECT returns only some columns:                             │
│       SELECT user_name, phone FROM user_details                                  │
│     The result is NOT a managed entity — it's raw data.                          │
│     Relationships like @OneToMany on UserDetails WON'T be lazy-loadable.        │
│     There's no proxy — just raw column values.                                   │
│                                                                                  │
│     If SELECT * is used and mapped to entity → entity IS managed.               │
│     Lazy loading works on managed entities returned from native queries.        │
│                                                                                  │
│  3. NO DIRTY CHECKING for partial results:                                       │
│     If you return a DTO or Object[] from native query → no entity managed.      │
│     Hibernate won't track changes. No automatic UPDATE on flush.                │
│     If you return a full entity (SELECT *) → dirty checking works.              │
│                                                                                  │
│  4. NO AUTOMATIC SQL TRANSLATION:                                                │
│     JPQL: Hibernate reads @Column(name="user_name") and maps field → column.    │
│     Native: YOU must use the correct column names in the SQL.                    │
│     If you rename a column in DB, JPQL auto-adjusts. Native breaks.             │
│                                                                                  │
│  5. NO ENTITY LIFECYCLE EVENTS for non-entity results:                           │
│     @PrePersist, @PostLoad, @PreUpdate etc. fire for managed entities.           │
│     If native query returns Object[] or DTO → no lifecycle events.              │
│     If native query returns full entity (SELECT *) → @PostLoad fires.           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Demonstration — entity lifecycle with native vs JPQL
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "active")
    private Boolean active;

    @OneToMany(mappedBy = "userDetails", fetch = FetchType.LAZY)
    private List<UserAddress> userAddressList;

    @PostLoad
    public void onLoad() {
        System.out.println("Entity loaded: " + this.name);
    }

    // getters, setters
}
```

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Native — SELECT * → full entity → managed → lazy loading works, @PostLoad fires
    @Query(value = "SELECT * FROM user_details WHERE active = true", nativeQuery = true)
    List<UserDetails> findActiveNative();
    // Returns managed entities. Lazy loading of userAddressList works.
    // @PostLoad fires for each entity.

    // Native — partial columns → NOT a managed entity → no lazy loading
    @Query(value = "SELECT user_name, phone FROM user_details WHERE active = true",
           nativeQuery = true)
    List<Object[]> findNameAndPhoneNative();
    // Returns Object[] — NOT entities.
    // No lazy loading, no dirty checking, no @PostLoad.
    // Just raw data: Object[0] = "Alice", Object[1] = "+1-555-0101"
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JPQL vs Native — Feature Comparison:                                            │
│                                                                                  │
│  ┌────────────────────────────────┬──────────────────┬──────────────────────────┐│
│  │ Feature                        │ JPQL             │ Native Query             ││
│  ├────────────────────────────────┼──────────────────┼──────────────────────────┤│
│  │ Query cache (2nd level)        │ ✓ Supported      │ ✗ Not supported          ││
│  │ Lazy loading on full entity    │ ✓ Works          │ ✓ Works (SELECT *)       ││
│  │ Lazy loading on partial result │ N/A              │ ✗ No proxy               ││
│  │ Dirty checking (full entity)   │ ✓ Entity managed │ ✓ Entity managed         ││
│  │ Dirty checking (partial cols)  │ N/A              │ ✗ Not managed            ││
│  │ @PostLoad lifecycle event      │ ✓ Fires          │ ✓ Only for SELECT *      ││
│  │ Auto column name resolution    │ ✓ From @Column   │ ✗ You write column names ││
│  │ DB independence                │ ✓ Dialect handles│ ✗ DB-specific SQL        ││
│  └────────────────────────────────┴──────────────────┴──────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### When to Use Native Query Over JPQL

#### 1. Database-Specific Features (JSONB, Full-Text Search, Window Functions)

JPQL doesn't support database-specific operators like PostgreSQL's JSONB operators, MySQL's `MATCH AGAINST`, or SQL window functions. For these, you need native queries.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // PostgreSQL JSONB — not possible in JPQL
    @Query(value = "SELECT * FROM user_details WHERE metadata @> '{\"role\": \"admin\"}'::jsonb",
           nativeQuery = true)
    List<UserDetails> findAdminsByJsonMetadata();
    // SQL: SELECT * FROM user_details WHERE metadata @> '{"role": "admin"}'::jsonb
    // The @> operator (contains) and ::jsonb cast are PostgreSQL-specific.
    // JPQL has NO equivalent for JSONB operators.

    // PostgreSQL full-text search — not possible in JPQL
    @Query(value = "SELECT * FROM user_details WHERE to_tsvector('english', user_name) @@ to_tsquery(:term)",
           nativeQuery = true)
    List<UserDetails> fullTextSearch(@Param("term") String term);
    // SQL: SELECT * FROM user_details
    //      WHERE to_tsvector('english', user_name) @@ to_tsquery('alice')

    // SQL Window Function — not possible in JPQL
    @Query(value = "SELECT *, ROW_NUMBER() OVER (ORDER BY user_name) as row_num FROM user_details",
           nativeQuery = true)
    List<Object[]> findWithRowNumber();
    // SQL: SELECT *, ROW_NUMBER() OVER (ORDER BY user_name) as row_num FROM user_details
    // Window functions (ROW_NUMBER, RANK, DENSE_RANK, LAG, LEAD) are not in JPQL.

    // MySQL-specific — MATCH AGAINST for full-text search
    @Query(value = "SELECT * FROM user_details WHERE MATCH(user_name) AGAINST(:keyword IN BOOLEAN MODE)",
           nativeQuery = true)
    List<UserDetails> mysqlFullTextSearch(@Param("keyword") String keyword);
}
```

```text
Database:

  user_details
  ┌────┬───────────┬──────────────────┬────────────────┬────────┬──────────────────────────┐
  │ id │ user_name │ email            │ phone          │ active │ metadata (JSONB)         │
  ├────┼───────────┼──────────────────┼────────────────┼────────┼──────────────────────────┤
  │  1 │ Alice     │ alice@ex.com     │ +1-555-0101    │ true   │ {"role":"admin","lvl":5}  │
  │  2 │ Bob       │ bob@ex.com       │ +1-555-0102    │ true   │ {"role":"user","lvl":2}   │
  │  3 │ Charlie   │ charlie@ex.com   │ +1-555-0103    │ false  │ {"role":"admin","lvl":3}  │
  └────┴───────────┴──────────────────┴────────────────┴────────┴──────────────────────────┘

  findAdminsByJsonMetadata() → returns Alice (id=1) and Charlie (id=3)
  because their metadata contains {"role": "admin"}
```

#### 2. Non-Entity Results — COUNT(*), JOINs Without Relationships

JPQL can only query entities and their mapped relationships. If you need to JOIN two tables that have **no JPA relationship** (no `@OneToMany`, no `@ManyToOne`), or if you need aggregate queries that don't map to any entity, you need native queries.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // COUNT(*) with GROUP BY — returns non-entity result
    @Query(value = "SELECT active, COUNT(*) as total FROM user_details GROUP BY active",
           nativeQuery = true)
    List<Object[]> countByActiveStatus();
    // SQL: SELECT active, COUNT(*) as total FROM user_details GROUP BY active
    //
    // Result:
    //   Object[0] = { true, 3 }   → 3 active users
    //   Object[1] = { false, 1 }  → 1 inactive user
    //
    // This COUNT(*) with GROUP BY returning raw columns doesn't map to an entity.

    // JOIN tables WITHOUT any @Entity relationship
    // Suppose "audit_logs" table exists but has NO @Entity or JPA relationship to user_details.
    @Query(value = "SELECT u.user_name, a.action, a.timestamp " +
                   "FROM user_details u " +
                   "JOIN audit_logs a ON u.id = a.user_id " +
                   "WHERE a.action = :action",
           nativeQuery = true)
    List<Object[]> findUserActionsFromAuditLog(@Param("action") String action);
    // SQL: SELECT u.user_name, a.action, a.timestamp
    //      FROM user_details u
    //      JOIN audit_logs a ON u.id = a.user_id
    //      WHERE a.action = 'LOGIN'
    //
    // JPQL CANNOT do this JOIN because there is no @ManyToOne or @OneToMany
    // between UserDetails and AuditLog entities.
    // Native query JOINs on raw foreign keys — no relationship mapping needed.

    // Subquery with NOT EXISTS — complex logic
    @Query(value = "SELECT u.* FROM user_details u " +
                   "WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id)",
           nativeQuery = true)
    List<UserDetails> findUsersWithNoOrders();
    // Returns users who have never placed an order.
    // "orders" table may not have a JPA entity at all.
}
```

```text
Database tables (no JPA relationship between them):

  user_details                           audit_logs (NO @Entity)
  ┌────┬───────────┬──────────────────┐  ┌────┬─────────┬────────────┬─────────────────────┐
  │ id │ user_name │ email            │  │ id │ user_id │ action     │ timestamp           │
  ├────┼───────────┼──────────────────┤  ├────┼─────────┼────────────┼─────────────────────┤
  │  1 │ Alice     │ alice@ex.com     │  │ 10 │    1    │ LOGIN      │ 2026-04-18 10:00:00 │
  │  2 │ Bob       │ bob@ex.com       │  │ 11 │    1    │ LOGOUT     │ 2026-04-18 11:00:00 │
  │  3 │ Charlie   │ charlie@ex.com   │  │ 12 │    2    │ LOGIN      │ 2026-04-18 09:00:00 │
  └────┴───────────┴──────────────────┘  └────┴─────────┴────────────┴─────────────────────┘

  findUserActionsFromAuditLog("LOGIN") returns:
    Object[] { "Alice",   "LOGIN", "2026-04-18 10:00:00" }
    Object[] { "Bob",     "LOGIN", "2026-04-18 09:00:00" }

  JPQL cannot express: FROM UserDetails u JOIN audit_logs a ON u.id = a.user_id
  because audit_logs is not mapped as a JPA relationship on UserDetails entity.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why JPQL can't do this:                                                         │
│                                                                                  │
│  JPQL JOIN requires a mapped relationship:                                       │
│    "SELECT u FROM UserDetails u JOIN u.addresses a"                              │
│                                      ↑                                           │
│                                u.addresses must be a @OneToMany or               │
│                                @ManyToMany field on the UserDetails entity.       │
│                                                                                  │
│  You CANNOT write in JPQL:                                                       │
│    "SELECT u FROM UserDetails u JOIN AuditLog a ON u.id = a.userId"              │
│    → Compilation error: "unexpected token: ON"                                   │
│    → JPQL doesn't support arbitrary ON clauses for unrelated entities            │
│      (JPA 2.1+ supports ON for additional conditions, but the entities           │
│       must still be related via a mapped association)                             │
│                                                                                  │
│  Native query has no such restriction:                                           │
│    "SELECT u.*, a.* FROM user_details u JOIN audit_logs a ON u.id = a.user_id"   │
│    → Works! SQL can JOIN any tables by any column.                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 3. Query Efficiency — Bulk Operations Bypass Persistence Context

JPQL `@Modifying` queries also bypass the entity lifecycle, but native queries are even more efficient for bulk operations because they skip JPQL parsing entirely. Additionally, JPQL keeps the Persistence Context updated (with `flushAutomatically`/`clearAutomatically`), adding overhead. Native queries go straight to the database.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Native bulk UPDATE — fastest possible, no JPQL parsing, no entity overhead
    @Modifying
    @Query(value = "UPDATE user_details SET active = false WHERE id IN (:ids)",
           nativeQuery = true)
    int bulkDeactivateNative(@Param("ids") List<Long> ids);
    // SQL: UPDATE user_details SET active = false WHERE id IN (1, 2, 3, ...)
    // → Sent directly to DB. No JPQL parsing. No Hibernate entity involvement.
    // → Returns int = number of rows updated.

    // Native bulk DELETE
    @Modifying
    @Query(value = "DELETE FROM user_details WHERE active = false AND " +
                   "id NOT IN (SELECT user_id FROM orders)",
           nativeQuery = true)
    int deleteInactiveUsersWithNoOrders();
    // SQL: DELETE FROM user_details WHERE active = false
    //      AND id NOT IN (SELECT user_id FROM orders)
    // → Complex subquery in DELETE — easier in native than JPQL.

    // Native INSERT (JPQL doesn't support INSERT...VALUES at all)
    @Modifying
    @Query(value = "INSERT INTO user_details (user_name, email, phone, active) " +
                   "VALUES (:name, :email, :phone, true)",
           nativeQuery = true)
    int insertUser(@Param("name") String name,
                   @Param("email") String email,
                   @Param("phone") String phone);
    // SQL: INSERT INTO user_details (user_name, email, phone, active)
    //      VALUES ('Alice', 'alice@ex.com', '+1-555-0101', true)
    // JPQL does NOT support INSERT...VALUES. Only INSERT...SELECT.
}
```

```java
// Service — @Transactional required for @Modifying
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional
    public int deactivateUsers(List<Long> ids) {
        return userDetailsRepository.bulkDeactivateNative(ids);
        // Fastest path: SQL goes directly to DB.
        // No JPQL parsing, no entity loading, no Persistence Context involvement.
        // WARNING: L1 cache is STALE after this. Use clearAutomatically = true
        // on @Modifying if you read these entities afterwards.
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Performance: JPQL @Modifying vs Native @Modifying:                              │
│                                                                                  │
│  JPQL @Modifying:                                                                │
│    @Modifying                                                                    │
│    @Query("UPDATE UserDetails u SET u.active = false WHERE u.id IN :ids")        │
│       │                                                                          │
│       v                                                                          │
│    1. JPQL Parser → parses "UPDATE UserDetails u SET..."                         │
│    2. Entity Metadata → resolves UserDetails → user_details table                │
│    3. SQL Generator → generates: UPDATE user_details SET active = false ...       │
│    4. (optional) flush() if flushAutomatically = true                            │
│    5. JDBC → executes SQL on DB                                                  │
│    6. (optional) clear() if clearAutomatically = true                            │
│    → Overhead: JPQL parsing + entity metadata lookup + SQL generation            │
│                                                                                  │
│  Native @Modifying:                                                              │
│    @Modifying                                                                    │
│    @Query(value = "UPDATE user_details SET active = false WHERE id IN (:ids)",   │
│           nativeQuery = true)                                                    │
│       │                                                                          │
│       v                                                                          │
│    1. JDBC → executes SQL on DB directly                                         │
│    → No JPQL parsing, no entity metadata lookup, no SQL generation               │
│    → Fastest possible path                                                       │
│                                                                                  │
│  For single queries, the difference is negligible.                               │
│  For bulk operations on millions of rows, native is noticeably faster.           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### How to Use Native Query — @Query(value = "...", nativeQuery = true)

The syntax is simple: add `nativeQuery = true` to the `@Query` annotation.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Basic native query — find all active users
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           nativeQuery = true)
    List<UserDetails> findAllActiveNative();
    // SQL sent to DB: SELECT * FROM user_details WHERE active = true
    // Result: mapped to List<UserDetails> entities

    // Native query with named parameter
    @Query(value = "SELECT * FROM user_details WHERE user_name = :name AND active = :active",
           nativeQuery = true)
    List<UserDetails> findByNameAndActiveNative(@Param("name") String name,
                                                @Param("active") Boolean active);
    // SQL: SELECT * FROM user_details WHERE user_name = 'Alice' AND active = true

    // Native query with positional parameter
    @Query(value = "SELECT * FROM user_details WHERE user_name = ?1 AND email = ?2",
           nativeQuery = true)
    UserDetails findByNameAndEmailNative(String name, String email);
    // SQL: SELECT * FROM user_details WHERE user_name = 'Alice' AND email = 'alice@ex.com'

    // Native query with LIKE
    @Query(value = "SELECT * FROM user_details WHERE user_name LIKE %:keyword%",
           nativeQuery = true)
    List<UserDetails> searchByNameNative(@Param("keyword") String keyword);
    // SQL: SELECT * FROM user_details WHERE user_name LIKE '%ali%'

    // Native query returning single value
    @Query(value = "SELECT COUNT(*) FROM user_details WHERE active = true",
           nativeQuery = true)
    long countActiveUsersNative();
    // SQL: SELECT COUNT(*) FROM user_details WHERE active = true
    // Returns: 3
}
```

```java
// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public List<UserDetails> getActiveUsers() {
        return userDetailsRepository.findAllActiveNative();
    }

    public List<UserDetails> searchUsers(String keyword) {
        return userDetailsRepository.searchByNameNative(keyword);
    }

    public long getActiveCount() {
        return userDetailsRepository.countActiveUsersNative();
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/active")
    public List<UserDetails> getActive() {
        return userService.getActiveUsers();
    }

    @GetMapping("/search")
    public List<UserDetails> search(@RequestParam String keyword) {
        return userService.searchUsers(keyword);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Query(nativeQuery = true) — How Spring processes it:                           │
│                                                                                  │
│  @Query(value = "SELECT * FROM user_details WHERE active = true",                │
│         nativeQuery = true)                                                      │
│  List<UserDetails> findAllActiveNative();                                        │
│       │                                                                          │
│       v                                                                          │
│  Spring Proxy:                                                                   │
│    sees nativeQuery = true                                                       │
│    → Does NOT parse as JPQL                                                      │
│    → Creates a NativeQuery via entityManager.createNativeQuery(sql, class)       │
│       │                                                                          │
│       v                                                                          │
│  Hibernate:                                                                      │
│    → Does NOT translate entity names → table names (already raw SQL)             │
│    → Sends SQL directly to JDBC PreparedStatement                                │
│       │                                                                          │
│       v                                                                          │
│  JDBC executes: SELECT * FROM user_details WHERE active = true                   │
│       │                                                                          │
│       v                                                                          │
│  ResultSet returned → Hibernate maps columns to entity fields:                   │
│    id → UserDetails.id                                                           │
│    user_name → UserDetails.name (via @Column(name="user_name"))                  │
│    email → UserDetails.email                                                     │
│    phone → UserDetails.phone                                                     │
│    active → UserDetails.active                                                   │
│       │                                                                          │
│       v                                                                          │
│  Returns List<UserDetails> — managed entities in Persistence Context             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### SELECT * — How Native Query Maps to Entity

When you write `SELECT * FROM user_details` in a native query and the return type is `List<UserDetails>`, Hibernate uses the entity's `@Column` annotations to map each column from the ResultSet to the corresponding Java field.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(value = "SELECT * FROM user_details", nativeQuery = true)
    List<UserDetails> findAllNative();
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  SELECT * FROM user_details — Column to Entity mapping:                          │
│                                                                                  │
│  SQL Result (from SELECT *):                                                     │
│  ┌────┬───────────┬──────────────────┬────────────────┬────────┐                 │
│  │ id │ user_name │ email            │ phone          │ active │                 │
│  ├────┼───────────┼──────────────────┼────────────────┼────────┤                 │
│  │  1 │ Alice     │ alice@ex.com     │ +1-555-0101    │ true   │                 │
│  │  2 │ Bob       │ bob@ex.com       │ +1-555-0102    │ true   │                 │
│  └────┴───────────┴──────────────────┴────────────────┴────────┘                 │
│                                                                                  │
│  Hibernate reads the ResultSet column names and matches with @Column:            │
│                                                                                  │
│    ResultSet column    @Column annotation          Java field                    │
│    ───────────────     ─────────────────────       ──────────                    │
│    id              →   @Id                     →   Long id                       │
│    user_name       →   @Column(name="user_name") → String name                  │
│    email           →   @Column(name="email")   →   String email                 │
│    phone           →   @Column(name="phone")   →   String phone                 │
│    active          →   @Column(name="active")  →   Boolean active               │
│                                                                                  │
│  Result: Fully populated UserDetails entities.                                   │
│  Entities are MANAGED in Persistence Context (L1 cache).                         │
│  Dirty checking works. Lazy loading of relationships works.                      │
│  @PostLoad lifecycle event fires.                                                │
│                                                                                  │
│  This is identical to what JPQL "SELECT u FROM UserDetails u" produces,          │
│  except the SQL was written by YOU instead of generated by Hibernate.            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Service — entity is fully managed
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional
    public void demonstrateNativeSelectStar() {
        List<UserDetails> users = userDetailsRepository.findAllNative();
        // SELECT * returns all columns → full entity mapping

        UserDetails alice = users.get(0);
        System.out.println(alice.getName());   // "Alice" — field populated
        System.out.println(alice.getEmail());   // "alice@ex.com" — field populated
        System.out.println(alice.getPhone());   // "+1-555-0101" — field populated

        // Entity is MANAGED → dirty checking works
        alice.setName("Alice Updated");
        // At transaction commit: Hibernate detects the change
        // → SQL: UPDATE user_details SET user_name = 'Alice Updated' WHERE id = 1
        // This happens automatically — same as with JPQL.

        // Lazy loading of relationships also works
        // alice.getUserAddressList() → triggers lazy SQL query
    }
}
```

---

### Partial SELECT — Will It Map to Entity?

If you write `SELECT user_name, phone FROM user_details` (only some columns, not all), it **CANNOT** be directly mapped to the `UserDetails` entity. Hibernate expects ALL columns to populate the entity. Missing columns cause an error or null values depending on the approach.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // THIS WILL FAIL — partial columns, return type is entity
    @Query(value = "SELECT user_name, phone FROM user_details", nativeQuery = true)
    List<UserDetails> findNameAndPhoneAsEntity();  // ← RUNTIME ERROR!
    // Hibernate tries to map ResultSet to UserDetails entity.
    // ResultSet has only 2 columns: user_name, phone
    // UserDetails needs: id, user_name, email, phone, active
    // → Missing columns (id, email, active)
    // → Exception: "Could not read entity" or "Column 'id' not found"
    //
    // Hibernate CANNOT create a managed entity without the @Id column (id).
    // Even if other columns were nullable, the primary key is mandatory.
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Partial SELECT → Entity mapping — WHY it fails:                                 │
│                                                                                  │
│  SELECT user_name, phone FROM user_details                                       │
│                                                                                  │
│  ResultSet:                                                                      │
│  ┌───────────┬────────────────┐                                                  │
│  │ user_name │ phone          │                                                  │
│  ├───────────┼────────────────┤                                                  │
│  │ Alice     │ +1-555-0101    │                                                  │
│  │ Bob       │ +1-555-0102    │                                                  │
│  └───────────┴────────────────┘                                                  │
│                                                                                  │
│  Hibernate tries to build UserDetails:                                           │
│    id     = ??? → NOT in ResultSet → ERROR! @Id is mandatory                     │
│    name   = "Alice" → mapped from user_name column ✓                             │
│    email  = ??? → NOT in ResultSet → null or error                               │
│    phone  = "+1-555-0101" → mapped from phone column ✓                           │
│    active = ??? → NOT in ResultSet → null or error                               │
│                                                                                  │
│  Without the @Id (primary key), Hibernate CANNOT:                                │
│    - Register the entity in the Persistence Context                              │
│    - Track changes (dirty checking)                                              │
│    - Maintain identity (two entities with same id are the same object)           │
│                                                                                  │
│  RESULT: RuntimeException at query execution time.                               │
│                                                                                  │
│  Solutions for partial SELECT:                                                   │
│    1. Use List<Object[]> as return type                                          │
│    2. Use @SqlResultSetMapping + @NamedNativeQuery → DTO mapping                 │
│    3. Use interface-based projection (Spring Data)                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Partial SELECT → DTO Using @SqlResultSetMapping + @NamedNativeQuery

When you need to map partial columns to a DTO class, you can use the JPA standard approach: `@SqlResultSetMapping` with `@ConstructorResult` on the entity class, combined with `@NamedNativeQuery` that references the mapping.

**Step 1: Create the DTO class:**

```java
// DTO — plain POJO, NOT an @Entity
public class UserPhoneDTO {
    private String userName;
    private String phone;

    // Constructor — parameter order and types must match @ColumnResult declarations
    public UserPhoneDTO(String userName, String phone) {
        this.userName = userName;
        this.phone = phone;
    }

    // getters
    public String getUserName() { return userName; }
    public String getPhone() { return phone; }
}
```

**Step 2: Add @SqlResultSetMapping and @NamedNativeQuery on the entity:**

```java
@Entity
@Table(name = "user_details")
@SqlResultSetMapping(
    name = "UserPhoneDTOMapping",                          // ← mapping name (referenced by @NamedNativeQuery)
    classes = @ConstructorResult(
        targetClass = UserPhoneDTO.class,                  // ← DTO class to construct
        columns = {
            @ColumnResult(name = "user_name", type = String.class),  // ← 1st constructor param
            @ColumnResult(name = "phone", type = String.class)       // ← 2nd constructor param
        }
    )
)
@NamedNativeQuery(
    name = "UserDetails.findUserPhone",                     // ← query name (Entity.methodName convention)
    query = "SELECT user_name, phone FROM user_details",    // ← raw SQL
    resultSetMapping = "UserPhoneDTOMapping"                // ← links to @SqlResultSetMapping above
)
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "active")
    private Boolean active;

    // getters, setters
}
```

**Step 3: Repository method matches @NamedNativeQuery name:**

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Spring matches "UserDetails.findUserPhone" → @NamedNativeQuery on entity
    List<UserPhoneDTO> findUserPhone();
    // The method name "findUserPhone" + entity name "UserDetails"
    // → looks for @NamedNativeQuery(name = "UserDetails.findUserPhone")
    // → executes: SELECT user_name, phone FROM user_details
    // → maps result using @SqlResultSetMapping("UserPhoneDTOMapping")
    // → calls: new UserPhoneDTO(resultSet.getString("user_name"), resultSet.getString("phone"))
    // → returns List<UserPhoneDTO>

    // With WHERE condition — add another @NamedNativeQuery
    // (need to add on entity: @NamedNativeQuery(name="UserDetails.findUserPhoneByActive", ...))
    List<UserPhoneDTO> findUserPhoneByActive(@Param("active") Boolean active);
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @SqlResultSetMapping + @NamedNativeQuery — Flow:                                │
│                                                                                  │
│  Entity class (UserDetails):                                                     │
│    @SqlResultSetMapping(                                                         │
│      name = "UserPhoneDTOMapping",                                               │
│      classes = @ConstructorResult(                                                │
│        targetClass = UserPhoneDTO.class,                                         │
│        columns = { @ColumnResult(name="user_name"), @ColumnResult(name="phone") }│
│      )                                                                           │
│    )                                                                             │
│    @NamedNativeQuery(                                                            │
│      name = "UserDetails.findUserPhone",                                         │
│      query = "SELECT user_name, phone FROM user_details",                        │
│      resultSetMapping = "UserPhoneDTOMapping"                                    │
│    )                                                                             │
│                                                                                  │
│  Repository: List<UserPhoneDTO> findUserPhone();                                 │
│       │                                                                          │
│       v                                                                          │
│  Spring sees method "findUserPhone" on UserDetails repository                    │
│    → Looks for @NamedNativeQuery(name = "UserDetails.findUserPhone") ✓           │
│       │                                                                          │
│       v                                                                          │
│  Executes: SELECT user_name, phone FROM user_details                             │
│       │                                                                          │
│       v                                                                          │
│  ResultSet:                                                                      │
│    ┌───────────┬────────────────┐                                                │
│    │ user_name │ phone          │                                                │
│    ├───────────┼────────────────┤                                                │
│    │ Alice     │ +1-555-0101    │                                                │
│    │ Bob       │ +1-555-0102    │                                                │
│    └───────────┴────────────────┘                                                │
│       │                                                                          │
│       v                                                                          │
│  @ConstructorResult mapping:                                                     │
│    new UserPhoneDTO("Alice", "+1-555-0101")    → UserPhoneDTO[0]                 │
│    new UserPhoneDTO("Bob",   "+1-555-0102")    → UserPhoneDTO[1]                 │
│       │                                                                          │
│       v                                                                          │
│  Returns: List<UserPhoneDTO> with 2 elements                                    │
│                                                                                  │
│  Column matching:                                                                │
│    @ColumnResult(name = "user_name") → ResultSet column "user_name" → 1st param  │
│    @ColumnResult(name = "phone")     → ResultSet column "phone"     → 2nd param  │
│    ORDER in @ColumnResult = ORDER of constructor parameters                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**With a WHERE condition — multiple @NamedNativeQuery on the same entity:**

```java
@Entity
@Table(name = "user_details")
@SqlResultSetMapping(
    name = "UserPhoneDTOMapping",
    classes = @ConstructorResult(
        targetClass = UserPhoneDTO.class,
        columns = {
            @ColumnResult(name = "user_name", type = String.class),
            @ColumnResult(name = "phone", type = String.class)
        }
    )
)
@NamedNativeQuery(
    name = "UserDetails.findUserPhone",
    query = "SELECT user_name, phone FROM user_details",
    resultSetMapping = "UserPhoneDTOMapping"
)
@NamedNativeQuery(
    name = "UserDetails.findUserPhoneByActive",
    query = "SELECT user_name, phone FROM user_details WHERE active = :active",
    resultSetMapping = "UserPhoneDTOMapping"
)
public class UserDetails {
    // ... fields same as above
}
```

```java
// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public List<UserPhoneDTO> getAllUserPhones() {
        List<UserPhoneDTO> results = userDetailsRepository.findUserPhone();
        // SQL: SELECT user_name, phone FROM user_details
        // → List<UserPhoneDTO> — type-safe, no manual casting

        for (UserPhoneDTO dto : results) {
            System.out.println(dto.getUserName() + " → " + dto.getPhone());
            // Alice → +1-555-0101
            // Bob → +1-555-0102
        }
        return results;
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/phones")
    public List<UserPhoneDTO> getPhones() {
        return userService.getAllUserPhones();
        // JSON: [{"userName":"Alice","phone":"+1-555-0101"}, {"userName":"Bob","phone":"+1-555-0102"}]
    }
}
```

---

### Partial SELECT → List<Object[]> and Manual DTO Conversion

A simpler (but less type-safe) approach is to return `List<Object[]>` from the repository and convert to the DTO manually in the service layer.

```java
// Repository — returns raw Object[]
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(value = "SELECT user_name, phone FROM user_details",
           nativeQuery = true)
    List<Object[]> findUserNameAndPhoneRaw();
    // SQL: SELECT user_name, phone FROM user_details
    //
    // Result: List<Object[]>
    //   Object[0] = { "Alice",   "+1-555-0101" }
    //   Object[1] = { "Bob",     "+1-555-0102" }
    //   Object[2] = { "Charlie", "+1-555-0103" }

    @Query(value = "SELECT user_name, phone FROM user_details WHERE active = :active",
           nativeQuery = true)
    List<Object[]> findUserNameAndPhoneByActiveRaw(@Param("active") Boolean active);
}
```

```java
// DTO class — same as before
public class UserPhoneDTO {
    private String userName;
    private String phone;

    public UserPhoneDTO(String userName, String phone) {
        this.userName = userName;
        this.phone = phone;
    }

    public String getUserName() { return userName; }
    public String getPhone() { return phone; }
}
```

```java
// Service — conversion happens HERE in the service layer
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public List<UserPhoneDTO> getAllUserPhones() {
        List<Object[]> rawResults = userDetailsRepository.findUserNameAndPhoneRaw();
        // SQL: SELECT user_name, phone FROM user_details
        //
        // rawResults:
        //   [0] = Object[] { "Alice",   "+1-555-0101" }
        //   [1] = Object[] { "Bob",     "+1-555-0102" }

        // Convert Object[] → UserPhoneDTO manually
        List<UserPhoneDTO> dtos = rawResults.stream()
            .map(row -> new UserPhoneDTO(
                (String) row[0],    // user_name (index 0 matches SELECT order)
                (String) row[1]     // phone     (index 1 matches SELECT order)
            ))
            .collect(Collectors.toList());

        // dtos:
        //   [0] = UserPhoneDTO { userName="Alice",   phone="+1-555-0101" }
        //   [1] = UserPhoneDTO { userName="Bob",     phone="+1-555-0102" }

        return dtos;
    }

    // Alternative — using a loop instead of stream
    public List<UserPhoneDTO> getAllUserPhonesLoop() {
        List<Object[]> rawResults = userDetailsRepository.findUserNameAndPhoneRaw();
        List<UserPhoneDTO> dtos = new ArrayList<>();

        for (Object[] row : rawResults) {
            String userName = (String) row[0];
            String phone = (String) row[1];
            dtos.add(new UserPhoneDTO(userName, phone));
        }

        return dtos;
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/phones")
    public List<UserPhoneDTO> getPhones() {
        return userService.getAllUserPhones();
        // JSON: [{"userName":"Alice","phone":"+1-555-0101"}, ...]
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Object[] mapping — positional (same as JPQL Object[]):                          │
│                                                                                  │
│  SELECT user_name, phone FROM user_details                                       │
│         ↑ index 0   ↑ index 1                                                    │
│                                                                                  │
│  Object[] row = { "Alice", "+1-555-0101" }                                       │
│                    row[0]   row[1]                                                │
│                                                                                  │
│  Conversion location: SERVICE LAYER                                              │
│    Repository → returns List<Object[]>                                           │
│    Service → converts Object[] → UserPhoneDTO                                    │
│    Controller → receives List<UserPhoneDTO>                                      │
│                                                                                  │
│  Flow:                                                                           │
│  ┌───────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │ Repository │    │  Service     │    │  Service     │    │ Controller   │      │
│  │ returns    │ →  │  receives    │ →  │  converts    │ →  │ receives     │      │
│  │ Object[]   │    │  Object[]    │    │  to DTO      │    │ DTO list     │      │
│  └───────────┘    └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                                  │
│  Drawbacks of Object[] approach (same as with JPQL):                             │
│  - No compile-time type safety                                                   │
│  - Manual casting: (String) row[0]                                               │
│  - Index-based, fragile: if SELECT order changes, all indexes break              │
│  - Not self-documenting: what is row[0]?                                         │
│                                                                                  │
│  When to use Object[] vs @SqlResultSetMapping:                                   │
│  ┌─────────────────────────────────┬───────────────────────────────────────────┐ │
│  │ List<Object[]>                  │ @SqlResultSetMapping + @NamedNativeQuery  │ │
│  ├─────────────────────────────────┼───────────────────────────────────────────┤ │
│  │ Quick, simple                   │ More setup, more annotations              │ │
│  │ No extra annotations on entity  │ Annotations on entity class               │ │
│  │ Manual casting in service       │ Automatic DTO construction                │ │
│  │ Fragile (index-based)           │ Robust (column-name based)                │ │
│  │ Good for prototyping            │ Good for production                       │ │
│  └─────────────────────────────────┴───────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Pagination and Sorting in Native Queries

Native queries support both `Pageable` (pagination) and `Sort` (sorting), but with important differences from JPQL.

**Pagination with PageRequest:**

For pagination, you must provide a `countQuery` parameter because Spring **cannot auto-generate** a count query from raw SQL (it can for JPQL because it understands the entity model).

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Paginated native query — countQuery is REQUIRED for Page<T> return type
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           countQuery = "SELECT COUNT(*) FROM user_details WHERE active = true",
           nativeQuery = true)
    Page<UserDetails> findActiveUsersPaginated(Pageable pageable);
    // SQL 1 (data):  SELECT * FROM user_details WHERE active = true LIMIT 10 OFFSET 0
    // SQL 2 (count): SELECT COUNT(*) FROM user_details WHERE active = true
    //
    // countQuery is mandatory for Page<T>.
    // Without it: Spring cannot derive COUNT from raw SQL → exception.

    // With WHERE condition and parameters
    @Query(value = "SELECT * FROM user_details WHERE user_name LIKE %:keyword%",
           countQuery = "SELECT COUNT(*) FROM user_details WHERE user_name LIKE %:keyword%",
           nativeQuery = true)
    Page<UserDetails> searchUsersPaginated(@Param("keyword") String keyword, Pageable pageable);

    // Slice — no countQuery needed (Slice doesn't need total count)
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           nativeQuery = true)
    Slice<UserDetails> findActiveUsersSliced(Pageable pageable);
    // SQL: SELECT * FROM user_details WHERE active = true LIMIT 11 OFFSET 0
    //      (fetches size+1 rows to determine hasNext)

    // List — no countQuery needed
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           nativeQuery = true)
    List<UserDetails> findActiveUsersList(Pageable pageable);
    // SQL: SELECT * FROM user_details WHERE active = true LIMIT 10 OFFSET 0
}
```

**Sorting in Native Queries:**

Sorting with the `Sort` parameter object does **NOT work** with native queries in most Spring Data JPA versions. Spring cannot validate sort properties against entity metadata for raw SQL. You must include `ORDER BY` directly in the SQL string.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // SORTING — include ORDER BY directly in the SQL (NOT via Sort parameter)
    @Query(value = "SELECT * FROM user_details WHERE active = true ORDER BY user_name ASC",
           nativeQuery = true)
    List<UserDetails> findActiveUsersSortedByName();
    // SQL: SELECT * FROM user_details WHERE active = true ORDER BY user_name ASC
    // Sort is HARDCODED in the SQL. Not dynamic.

    // Dynamic sorting — use Sort via Pageable (works for simple column names)
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           countQuery = "SELECT COUNT(*) FROM user_details WHERE active = true",
           nativeQuery = true)
    Page<UserDetails> findActiveUsersSorted(Pageable pageable);
    // When Pageable contains Sort info, Spring appends ORDER BY to the SQL.
    // BUT: Sort property names must match COLUMN names (not entity field names).
    //
    // Sort.by("user_name") → works (user_name is the actual column name)
    // Sort.by("name") → FAILS (name is the Java field, not the column name)
    //
    // This is the opposite of JPQL where you use Java field names for sorting.

    // For complex sorting that can't be expressed via Pageable:
    @Query(value = "SELECT * FROM user_details WHERE active = true " +
                   "ORDER BY active DESC, user_name ASC",
           nativeQuery = true)
    List<UserDetails> findWithComplexSort();
}
```

```java
// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    // Pagination only
    public Page<UserDetails> getActiveUsersPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userDetailsRepository.findActiveUsersPaginated(pageable);
        // SQL 1: SELECT * FROM user_details WHERE active = true LIMIT 10 OFFSET 0
        // SQL 2: SELECT COUNT(*) FROM user_details WHERE active = true
    }

    // Pagination + Sorting (dynamic via Pageable)
    public Page<UserDetails> getActiveUsersPaginatedAndSorted(int page, int size) {
        // IMPORTANT: Use COLUMN names for Sort, not Java field names!
        Sort sort = Sort.by(Sort.Direction.DESC, "user_name");  // ← column name
        Pageable pageable = PageRequest.of(page, size, sort);
        return userDetailsRepository.findActiveUsersSorted(pageable);
        // SQL 1: SELECT * FROM user_details WHERE active = true
        //        ORDER BY user_name DESC
        //        LIMIT 10 OFFSET 0
        // SQL 2: SELECT COUNT(*) FROM user_details WHERE active = true
    }

    // Multiple sort columns
    public Page<UserDetails> getActiveUsersMultiSort(int page, int size) {
        Sort sort = Sort.by(
            Sort.Order.asc("user_name"),   // ← column name (not "name")
            Sort.Order.desc("id")           // ← column name
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        return userDetailsRepository.findActiveUsersSorted(pageable);
        // SQL: SELECT * FROM user_details WHERE active = true
        //      ORDER BY user_name ASC, id DESC
        //      LIMIT 10 OFFSET 0
    }

    // Pagination with Object[] result (partial columns)
    public List<Object[]> getUserPhonesPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userDetailsRepository.findUserNameAndPhonePaginated(pageable);
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/active")
    public Page<UserDetails> getActiveUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "user_name") String sortBy,
        @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        // NOTE: sortBy must be a COLUMN name for native queries
        Pageable pageable = PageRequest.of(page, size, sort);
        return userService.getActiveUsersPaginatedAndSorted(page, size);
    }
}
```

```java
// Pagination with partial columns (Object[]) — needs countQuery too
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(value = "SELECT user_name, phone FROM user_details WHERE active = true",
           countQuery = "SELECT COUNT(*) FROM user_details WHERE active = true",
           nativeQuery = true)
    Page<Object[]> findUserNameAndPhonePaginated(Pageable pageable);
    // SQL 1: SELECT user_name, phone FROM user_details WHERE active = true LIMIT 10 OFFSET 0
    // SQL 2: SELECT COUNT(*) FROM user_details WHERE active = true
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pagination + Sorting in Native Query — Key Differences from JPQL:               │
│                                                                                  │
│  ┌──────────────────────────────────┬────────────────────────────────────────────┐│
│  │ Feature                          │ JPQL                │ Native Query         ││
│  ├──────────────────────────────────┼─────────────────────┼──────────────────────┤│
│  │ Pageable parameter               │ ✓ Works             │ ✓ Works              ││
│  │ countQuery                       │ Auto-generated      │ MUST be provided     ││
│  │                                  │ (or custom)         │ for Page<T>          ││
│  │ Sort via Pageable                │ Uses Java FIELD     │ Uses DB COLUMN       ││
│  │                                  │ names               │ names                ││
│  │ Sort via Sort parameter alone    │ ✓ Works             │ ✗ Often fails        ││
│  │ ORDER BY in query string         │ ✓ Possible          │ ✓ Recommended for    ││
│  │                                  │                     │ complex sorts        ││
│  │ LIMIT/OFFSET generation          │ Auto from Pageable  │ Auto from Pageable   ││
│  │ Slice<T> (no count)              │ ✓ Works             │ ✓ Works              ││
│  │ Page<T> (with count)             │ ✓ Auto count        │ ✓ Needs countQuery   ││
│  └──────────────────────────────────┴─────────────────────┴──────────────────────┘│
│                                                                                  │
│  CRITICAL DIFFERENCE — Sort property names:                                      │
│                                                                                  │
│  JPQL:   Sort.by("name")      → works (Java field name)                         │
│  Native: Sort.by("name")      → FAILS (no column called "name")                 │
│  Native: Sort.by("user_name") → works (actual column name)                      │
│                                                                                  │
│  CRITICAL DIFFERENCE — countQuery:                                               │
│                                                                                  │
│  JPQL:                                                                           │
│    @Query("SELECT u FROM UserDetails u WHERE u.active = true")                   │
│    Page<UserDetails> findActive(Pageable pageable);                              │
│    → Spring auto-generates: SELECT COUNT(u) FROM UserDetails u WHERE ...         │
│                                                                                  │
│  Native:                                                                         │
│    @Query(value = "SELECT * FROM user_details WHERE active = true",              │
│           nativeQuery = true)                                                    │
│    Page<UserDetails> findActive(Pageable pageable);                              │
│    → Spring CANNOT auto-generate count from raw SQL → EXCEPTION                  │
│    → You MUST provide: countQuery = "SELECT COUNT(*) FROM user_details ..."      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Complete flow — Native Query with Pagination + Sorting:

  Client: GET /api/users/active?page=1&size=5&sortBy=user_name&direction=DESC
     │
     v
  Controller: extracts params → creates Sort and Pageable
    Sort sort = Sort.by(Sort.Direction.DESC, "user_name");   ← COLUMN name!
    Pageable pageable = PageRequest.of(1, 5, sort);
     │
     v
  Service: calls repository
    userDetailsRepository.findActiveUsersSorted(pageable);
     │
     v
  Spring Proxy:
    sees @Query(nativeQuery = true) → treats as raw SQL
    Original SQL: SELECT * FROM user_details WHERE active = true
    Appends Sort → ORDER BY user_name DESC
    Appends Pagination → LIMIT 5 OFFSET 5
     │
     v
  SQL 1 (data):
    SELECT * FROM user_details
    WHERE active = true
    ORDER BY user_name DESC
    LIMIT 5 OFFSET 5
     │
  SQL 2 (count):
    SELECT COUNT(*) FROM user_details WHERE active = true
     │
     v
  Results:
    Page<UserDetails> {
      content: [user6, user7, user8, user9, user10],  ← page 1 (0-indexed)
      pageNumber: 1,
      pageSize: 5,
      totalElements: 12,
      totalPages: 3,
      sort: Sort { orders: [Order(user_name, DESC)] }
    }
     │
     v
  Controller returns → Jackson serializes to JSON → Client receives response
```

```text
Summary — When to use Native Query vs JPQL vs Derived Query:

  ┌──────────────────────────────────┬──────────────────────────────────────────────┐
  │ Scenario                         │ Best Approach                                │
  ├──────────────────────────────────┼──────────────────────────────────────────────┤
  │ Simple CRUD / findBy conditions  │ Derived Query (method name)                  │
  │ Complex WHERE, JOINs on entities │ JPQL (@Query)                                │
  │ DB-specific features (JSONB,     │ Native Query (@Query nativeQuery=true)        │
  │   full-text, window functions)   │                                              │
  │ JOIN tables without relationship │ Native Query                                 │
  │ Bulk INSERT...VALUES             │ Native Query (JPQL can't do INSERT VALUES)   │
  │ Bulk UPDATE/DELETE               │ Either (Native slightly faster)              │
  │ Aggregates with GROUP BY         │ Native Query or JPQL (both work)             │
  │ Partial columns → DTO            │ JPQL (NEW constructor) or Native (Object[],  │
  │                                  │   @SqlResultSetMapping)                      │
  │ Database portability needed      │ JPQL (DB-independent)                         │
  │ Maximum performance              │ Native Query (no JPQL parsing overhead)       │
  └──────────────────────────────────┴──────────────────────────────────────────────┘
```

---


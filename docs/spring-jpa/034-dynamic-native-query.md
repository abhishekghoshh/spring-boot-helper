### What Is a Dynamic Native Query?

A **dynamic native query** is a raw SQL query that is built **at runtime** in the service layer using `EntityManager`, rather than being defined statically in a repository with `@Query`. This lets you construct the SQL string conditionally based on method arguments — adding or removing WHERE clauses, JOIN conditions, ORDER BY, and LIMIT/OFFSET dynamically.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Static @Query (Repository) vs Dynamic Query (EntityManager):                    │
│                                                                                  │
│  Static — @Query on Repository:                                                  │
│    @Query(value = "SELECT * FROM user_details WHERE active = true",              │
│           nativeQuery = true)                                                    │
│    List<UserDetails> findActive();                                               │
│    → Query is FIXED at compile time. Cannot add/remove conditions dynamically.   │
│    → If you need 10 different filter combinations, you need 10 methods.          │
│                                                                                  │
│  Dynamic — EntityManager in Service:                                             │
│    String sql = "SELECT * FROM user_details WHERE 1=1";                          │
│    if (name != null) sql += " AND user_name = :name";                            │
│    if (active != null) sql += " AND active = :active";                           │
│    Query query = entityManager.createNativeQuery(sql, UserDetails.class);        │
│    → Query is BUILT at runtime based on which parameters are provided.           │
│    → One method handles ANY combination of filters.                              │
│                                                                                  │
│  When to use Dynamic Native Query:                                               │
│    - Search/filter APIs with many optional parameters                            │
│    - Dynamic report generation where columns/conditions change                   │
│    - Admin dashboards with configurable filters                                  │
│    - Any scenario where the number of WHERE conditions is unknown at compile time│
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Database Table — user_details (used in all examples below):

  ┌────┬───────────┬──────────────────┬────────────────┬────────┐
  │ id │ user_name │ email            │ phone          │ active │
  ├────┼───────────┼──────────────────┼────────────────┼────────┤
  │  1 │ Alice     │ alice@ex.com     │ +1-555-0101    │ true   │
  │  2 │ Bob       │ bob@ex.com       │ +1-555-0102    │ true   │
  │  3 │ Charlie   │ charlie@ex.com   │ +1-555-0103    │ false  │
  │  4 │ Diana     │ diana@ex.com     │ +1-555-0104    │ true   │
  │  5 │ Eve       │ eve@ex.com       │ +1-555-0105    │ false  │
  │  6 │ Frank     │ frank@ex.com     │ +1-555-0106    │ true   │
  │  7 │ Grace     │ grace@ex.com     │ +1-555-0107    │ true   │
  │  8 │ Hank      │ hank@ex.com      │ +1-555-0108    │ false  │
  └────┴───────────┴──────────────────┴────────────────┴────────┘
```

---

### @PersistenceContext and EntityManager

`@PersistenceContext` injects the JPA `EntityManager` — the core JPA interface for interacting with the Persistence Context. You use it in service classes to create and execute queries programmatically.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @PersistenceContext vs @Autowired for EntityManager:                             │
│                                                                                  │
│  @PersistenceContext                                                             │
│  private EntityManager entityManager;                                            │
│  → JPA standard annotation.                                                      │
│  → Injects a PROXY that is bound to the current transaction's Persistence Context│
│  → Thread-safe: each request gets its own Persistence Context.                   │
│  → RECOMMENDED for EntityManager injection.                                      │
│                                                                                  │
│  @Autowired                                                                      │
│  private EntityManager entityManager;                                            │
│  → Spring-specific. Also works because Spring registers EntityManager as a bean. │
│  → Same proxy behavior in practice.                                              │
│  → But @PersistenceContext is the JPA-standard way.                              │
│                                                                                  │
│  Key EntityManager methods for native queries:                                   │
│    entityManager.createNativeQuery(sql)                                          │
│      → returns Query object for raw SQL                                          │
│    entityManager.createNativeQuery(sql, EntityClass.class)                       │
│      → returns Query object that maps results to an entity                       │
│    query.setParameter("name", value)                                             │
│      → binds a named parameter                                                   │
│    query.setParameter(1, value)                                                  │
│      → binds a positional parameter                                              │
│    query.getResultList()                                                         │
│      → executes SELECT, returns List                                             │
│    query.getSingleResult()                                                       │
│      → executes SELECT, returns single object                                    │
│    query.executeUpdate()                                                         │
│      → executes INSERT/UPDATE/DELETE, returns int (affected rows)                │
│    query.setFirstResult(offset)                                                  │
│      → sets the starting row (OFFSET)                                            │
│    query.setMaxResults(limit)                                                    │
│      → sets max rows to return (LIMIT)                                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Basic Dynamic Native Query — createNativeQuery + setParameter

**Building a query dynamically based on which parameters are non-null:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic search — only filters that are non-null are added to the query.
     * Any combination of name, email, active can be provided.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<UserDetails> searchUsers(String name, String email, Boolean active) {

        // Step 1: Start building the SQL string
        StringBuilder sql = new StringBuilder("SELECT * FROM user_details WHERE 1=1");
        //                                                                    ↑
        //  "WHERE 1=1" is a trick so every subsequent condition can use "AND ..."
        //  Without it, the first condition would need "WHERE" and others "AND".
        //  1=1 is always true, so it doesn't affect results.

        // Step 2: Conditionally add WHERE clauses
        if (name != null && !name.isEmpty()) {
            sql.append(" AND user_name = :name");
        }
        if (email != null && !email.isEmpty()) {
            sql.append(" AND email = :email");
        }
        if (active != null) {
            sql.append(" AND active = :active");
        }

        // Step 3: Create the native query with entity class mapping
        Query query = entityManager.createNativeQuery(sql.toString(), UserDetails.class);
        //                                             ↑ raw SQL       ↑ map to entity

        // Step 4: Bind parameters (only the ones we added to the SQL)
        if (name != null && !name.isEmpty()) {
            query.setParameter("name", name);
        }
        if (email != null && !email.isEmpty()) {
            query.setParameter("email", email);
        }
        if (active != null) {
            query.setParameter("active", active);
        }

        // Step 5: Execute and return
        return query.getResultList();
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

    // GET /api/users/search?name=Alice
    // GET /api/users/search?active=true
    // GET /api/users/search?name=Alice&active=true
    // GET /api/users/search?name=Alice&email=alice@ex.com&active=true
    // All combinations work with the SAME endpoint!
    @GetMapping("/search")
    public List<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Boolean active
    ) {
        return userService.searchUsers(name, email, active);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dynamic Query Building — Example Scenarios:                                     │
│                                                                                  │
│  Scenario 1: searchUsers("Alice", null, null)                                    │
│    Built SQL: SELECT * FROM user_details WHERE 1=1 AND user_name = :name         │
│    Bound:     :name = "Alice"                                                    │
│    Final SQL: SELECT * FROM user_details WHERE 1=1 AND user_name = 'Alice'       │
│    Result:    [UserDetails(id=1, name="Alice", ...)]                             │
│                                                                                  │
│  Scenario 2: searchUsers(null, null, true)                                       │
│    Built SQL: SELECT * FROM user_details WHERE 1=1 AND active = :active          │
│    Bound:     :active = true                                                     │
│    Final SQL: SELECT * FROM user_details WHERE 1=1 AND active = true             │
│    Result:    [Alice, Bob, Diana, Frank, Grace] — 5 active users                 │
│                                                                                  │
│  Scenario 3: searchUsers("Bob", "bob@ex.com", true)                              │
│    Built SQL: SELECT * FROM user_details WHERE 1=1                               │
│               AND user_name = :name AND email = :email AND active = :active      │
│    Bound:     :name = "Bob", :email = "bob@ex.com", :active = true               │
│    Final SQL: SELECT * FROM user_details WHERE 1=1                               │
│               AND user_name = 'Bob' AND email = 'bob@ex.com' AND active = true   │
│    Result:    [UserDetails(id=2, name="Bob", ...)]                               │
│                                                                                  │
│  Scenario 4: searchUsers(null, null, null)                                       │
│    Built SQL: SELECT * FROM user_details WHERE 1=1                               │
│    Final SQL: SELECT * FROM user_details WHERE 1=1                               │
│    Result:    All 8 users (no filters applied)                                   │
│                                                                                  │
│  ONE method handles ALL combinations — no need for multiple @Query methods!      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Flow — Dynamic Native Query:                                                    │
│                                                                                  │
│  Controller: GET /api/users/search?name=Alice&active=true                        │
│       │                                                                          │
│       v                                                                          │
│  Service: searchUsers("Alice", null, true)                                       │
│       │                                                                          │
│       v                                                                          │
│  Build SQL string dynamically:                                                   │
│    "SELECT * FROM user_details WHERE 1=1"                                        │
│     + " AND user_name = :name"      ← name is non-null                           │
│     (email is null → skipped)                                                    │
│     + " AND active = :active"       ← active is non-null                         │
│       │                                                                          │
│       v                                                                          │
│  entityManager.createNativeQuery(sql, UserDetails.class)                         │
│       │                                                                          │
│       v                                                                          │
│  query.setParameter("name", "Alice")                                             │
│  query.setParameter("active", true)                                              │
│       │                                                                          │
│       v                                                                          │
│  query.getResultList()                                                           │
│       │                                                                          │
│       v                                                                          │
│  Hibernate sends to DB:                                                          │
│    SELECT * FROM user_details                                                    │
│    WHERE 1=1 AND user_name = 'Alice' AND active = true                           │
│       │                                                                          │
│       v                                                                          │
│  ResultSet → mapped to UserDetails entity (SELECT * → full entity)               │
│       │                                                                          │
│       v                                                                          │
│  Returns List<UserDetails> → Controller → JSON response                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Returning Object[] instead of Entity (partial columns):**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<UserPhoneDTO> searchUserPhones(String name, Boolean active) {

        // Select only specific columns — returns Object[], NOT entity
        StringBuilder sql = new StringBuilder("SELECT user_name, phone FROM user_details WHERE 1=1");

        if (name != null) {
            sql.append(" AND user_name LIKE :name");
        }
        if (active != null) {
            sql.append(" AND active = :active");
        }

        // No entity class → returns List<Object[]>
        Query query = entityManager.createNativeQuery(sql.toString());
        //                                             ↑ no second parameter = Object[] result

        if (name != null) {
            query.setParameter("name", "%" + name + "%");
        }
        if (active != null) {
            query.setParameter("active", active);
        }

        List<Object[]> results = query.getResultList();

        // Convert Object[] → DTO in service layer
        return results.stream()
            .map(row -> new UserPhoneDTO((String) row[0], (String) row[1]))
            .collect(Collectors.toList());
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  createNativeQuery — Two Overloads:                                              │
│                                                                                  │
│  1. entityManager.createNativeQuery(sql, UserDetails.class)                      │
│     → SQL must SELECT all entity columns (SELECT *)                              │
│     → ResultSet mapped to entity automatically                                   │
│     → Returns managed entities (dirty checking, lazy loading work)               │
│     → getResultList() returns List<UserDetails>                                  │
│                                                                                  │
│  2. entityManager.createNativeQuery(sql)                                         │
│     → SQL can select ANY columns                                                 │
│     → ResultSet returned as raw Object[]                                         │
│     → NOT managed entities — just raw data                                       │
│     → getResultList() returns List<Object[]>                                     │
│     → You convert to DTO manually in service layer                               │
│                                                                                  │
│  IMPORTANT — SQL Injection Protection:                                           │
│    ALWAYS use setParameter() for user inputs.                                    │
│    NEVER concatenate user values directly into SQL:                              │
│                                                                                  │
│    ✗ WRONG (SQL injection):                                                      │
│      sql.append(" AND user_name = '" + name + "'");                              │
│      → If name = "'; DROP TABLE user_details; --"                                │
│      → SQL: ... AND user_name = ''; DROP TABLE user_details; --'                 │
│      → TABLE DELETED!                                                            │
│                                                                                  │
│    ✓ CORRECT (parameterized):                                                    │
│      sql.append(" AND user_name = :name");                                       │
│      query.setParameter("name", name);                                           │
│      → JDBC escapes the value. SQL injection impossible.                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Dynamic Native Query with Pagination and Sorting

`EntityManager` provides `setFirstResult(offset)` and `setMaxResults(limit)` for pagination. For sorting, you append `ORDER BY` to the SQL string dynamically.

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic search with pagination and sorting.
     * @param name     optional filter
     * @param active   optional filter
     * @param page     0-indexed page number
     * @param size     page size
     * @param sortBy   column name to sort by (e.g., "user_name", "id", "email")
     * @param sortDir  sort direction ("ASC" or "DESC")
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Page<UserDetails> searchUsersPaginated(String name, Boolean active,
                                                   int page, int size,
                                                   String sortBy, String sortDir) {

        // === DATA QUERY ===
        StringBuilder dataSql = new StringBuilder("SELECT * FROM user_details WHERE 1=1");

        // === COUNT QUERY (for Page metadata) ===
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM user_details WHERE 1=1");

        // Conditionally add WHERE clauses to BOTH queries
        if (name != null && !name.isEmpty()) {
            dataSql.append(" AND user_name LIKE :name");
            countSql.append(" AND user_name LIKE :name");
        }
        if (active != null) {
            dataSql.append(" AND active = :active");
            countSql.append(" AND active = :active");
        }

        // Add ORDER BY (sorting) — only to data query, NOT count query
        // IMPORTANT: Validate sortBy against a whitelist to prevent SQL injection!
        List<String> allowedSortColumns = List.of("id", "user_name", "email", "phone", "active");
        if (sortBy != null && allowedSortColumns.contains(sortBy)) {
            String direction = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
            dataSql.append(" ORDER BY ").append(sortBy).append(" ").append(direction);
        } else {
            dataSql.append(" ORDER BY id ASC");  // default sort
        }

        // Create data query
        Query dataQuery = entityManager.createNativeQuery(dataSql.toString(), UserDetails.class);

        // Create count query
        Query countQuery = entityManager.createNativeQuery(countSql.toString());

        // Bind parameters to BOTH queries
        if (name != null && !name.isEmpty()) {
            dataQuery.setParameter("name", "%" + name + "%");
            countQuery.setParameter("name", "%" + name + "%");
        }
        if (active != null) {
            dataQuery.setParameter("active", active);
            countQuery.setParameter("active", active);
        }

        // Apply pagination — OFFSET and LIMIT
        dataQuery.setFirstResult(page * size);   // OFFSET = page * size
        dataQuery.setMaxResults(size);            // LIMIT = size

        // Execute both queries
        List<UserDetails> content = dataQuery.getResultList();
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        // Build and return Page object
        Pageable pageable = PageRequest.of(page, size, Sort.by(
            "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy != null ? sortBy : "id"
        ));
        return new PageImpl<>(content, pageable, totalElements);
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

    // GET /api/users/search?active=true&page=0&size=3&sortBy=user_name&sortDir=DESC
    @GetMapping("/search")
    public Page<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        return userService.searchUsersPaginated(name, active, page, size, sortBy, sortDir);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dynamic Query with Pagination + Sorting — Scenario:                             │
│                                                                                  │
│  Request: GET /api/users/search?active=true&page=1&size=2&sortBy=user_name       │
│                                                      &sortDir=ASC                │
│                                                                                  │
│  Built data SQL:                                                                 │
│    SELECT * FROM user_details                                                    │
│    WHERE 1=1 AND active = :active                                                │
│    ORDER BY user_name ASC                                                        │
│                                                                                  │
│  After setParameter + pagination:                                                │
│    SELECT * FROM user_details                                                    │
│    WHERE 1=1 AND active = true                                                   │
│    ORDER BY user_name ASC                                                        │
│    LIMIT 2 OFFSET 2                                                              │
│         ↑ size    ↑ page(1) × size(2)                                            │
│                                                                                  │
│  Built count SQL:                                                                │
│    SELECT COUNT(*) FROM user_details                                             │
│    WHERE 1=1 AND active = true                                                   │
│    → Returns: 5 (Alice, Bob, Diana, Frank, Grace)                                │
│                                                                                  │
│  Active users sorted by name: Alice(1), Bob(2), Diana(4), Frank(6), Grace(7)    │
│  Page 0 (OFFSET 0, LIMIT 2): [Alice, Bob]                                       │
│  Page 1 (OFFSET 2, LIMIT 2): [Diana, Frank]  ← THIS page                        │
│  Page 2 (OFFSET 4, LIMIT 2): [Grace]                                            │
│                                                                                  │
│  Response:                                                                       │
│    Page<UserDetails> {                                                           │
│      content: [Diana, Frank],                                                    │
│      pageNumber: 1,                                                              │
│      pageSize: 2,                                                                │
│      totalElements: 5,                                                           │
│      totalPages: 3                                                               │
│    }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pagination Methods — setFirstResult / setMaxResults:                            │
│                                                                                  │
│  query.setFirstResult(offset)                                                    │
│    → Translates to SQL OFFSET                                                    │
│    → Skips the first N rows                                                      │
│    → Page 0: setFirstResult(0)   → OFFSET 0                                     │
│    → Page 1: setFirstResult(10)  → OFFSET 10  (if size = 10)                    │
│    → Page 2: setFirstResult(20)  → OFFSET 20                                    │
│    → Formula: offset = page × size                                               │
│                                                                                  │
│  query.setMaxResults(limit)                                                      │
│    → Translates to SQL LIMIT                                                     │
│    → Returns at most N rows                                                      │
│    → setMaxResults(10) → LIMIT 10                                                │
│                                                                                  │
│  Combined:                                                                       │
│    query.setFirstResult(page * size);                                            │
│    query.setMaxResults(size);                                                    │
│    → SQL: ... LIMIT size OFFSET (page * size)                                    │
│                                                                                  │
│  Hibernate translates to DB-specific syntax:                                     │
│    MySQL/PostgreSQL: LIMIT 10 OFFSET 20                                          │
│    Oracle: OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY                               │
│    SQL Server: OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY                           │
│                                                                                  │
│  NOTE: For COUNT query, do NOT set pagination — you want the TOTAL count.        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Sorting — SQL Injection Protection for ORDER BY:                                │
│                                                                                  │
│  setParameter() CANNOT be used for ORDER BY column names:                        │
│    query.setParameter("sort", "user_name")                                       │
│    SQL: ... ORDER BY :sort → becomes ORDER BY 'user_name' → ERROR!               │
│    Parameterized values are treated as STRING LITERALS, not column identifiers.  │
│                                                                                  │
│  You MUST concatenate column name into the SQL string:                            │
│    sql.append(" ORDER BY ").append(sortBy).append(" ASC")                        │
│                                                                                  │
│  But this opens up SQL injection if sortBy comes from user input!                │
│    sortBy = "user_name; DROP TABLE user_details; --"                             │
│    SQL: ... ORDER BY user_name; DROP TABLE user_details; -- ASC                  │
│                                                                                  │
│  SOLUTION: Validate sortBy against a WHITELIST of allowed column names:          │
│    List<String> allowed = List.of("id", "user_name", "email", "phone");          │
│    if (allowed.contains(sortBy)) {                                               │
│        sql.append(" ORDER BY ").append(sortBy);                                  │
│    }                                                                             │
│    → Only pre-approved column names can be used.                                 │
│    → Unknown values are rejected.                                                │
│    → SQL injection impossible.                                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Complex Queries Using Dynamic Native Query

Dynamic native queries shine when you need complex search logic with optional JOINs, subqueries, GROUP BY, HAVING, or database-specific features — all built conditionally.

**Example 1: Multi-table search with optional JOINs:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Complex search: filter by user fields AND optionally by address fields.
     * If no address filters are provided, no JOIN is added (faster query).
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<UserDetails> advancedSearch(String name, Boolean active,
                                             String city, String country) {

        StringBuilder sql = new StringBuilder("SELECT DISTINCT u.* FROM user_details u");

        // Only JOIN addresses if address filters are provided
        boolean needsAddressJoin = (city != null && !city.isEmpty())
                                || (country != null && !country.isEmpty());
        if (needsAddressJoin) {
            sql.append(" JOIN user_addresses a ON u.id = a.user_id");
        }

        sql.append(" WHERE 1=1");

        if (name != null && !name.isEmpty()) {
            sql.append(" AND u.user_name LIKE :name");
        }
        if (active != null) {
            sql.append(" AND u.active = :active");
        }
        if (city != null && !city.isEmpty()) {
            sql.append(" AND a.city = :city");
        }
        if (country != null && !country.isEmpty()) {
            sql.append(" AND a.country = :country");
        }

        Query query = entityManager.createNativeQuery(sql.toString(), UserDetails.class);

        if (name != null && !name.isEmpty()) {
            query.setParameter("name", "%" + name + "%");
        }
        if (active != null) {
            query.setParameter("active", active);
        }
        if (city != null && !city.isEmpty()) {
            query.setParameter("city", city);
        }
        if (country != null && !country.isEmpty()) {
            query.setParameter("country", country);
        }

        return query.getResultList();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Complex Query — Dynamic JOIN Scenarios:                                         │
│                                                                                  │
│  Database Tables:                                                                │
│                                                                                  │
│  user_details                         user_addresses                             │
│  ┌────┬───────────┬────────┐          ┌────┬──────────────┬──────────┬─────────┐ │
│  │ id │ user_name │ active │          │ id │ street       │ city     │ country │ │
│  ├────┼───────────┼────────┤          ├────┼──────────────┼──────────┼─────────┤ │
│  │  1 │ Alice     │ true   │          │ 10 │ 123 Main St  │ New York │ USA     │ │
│  │  2 │ Bob       │ true   │          │ 11 │ 456 Oak Ave  │ London   │ UK      │ │
│  │  3 │ Charlie   │ false  │          │ 12 │ 789 Pine Rd  │ Mumbai   │ India   │ │
│  └────┴───────────┴────────┘          └────┴──────────────┴──────────┴─────────┘ │
│                                                                                  │
│  Scenario 1: advancedSearch("Alice", null, null, null)                           │
│    SQL: SELECT DISTINCT u.* FROM user_details u                                  │
│         WHERE 1=1 AND u.user_name LIKE '%Alice%'                                 │
│    → NO JOIN (address filters not provided → faster query)                       │
│                                                                                  │
│  Scenario 2: advancedSearch(null, true, "London", null)                          │
│    SQL: SELECT DISTINCT u.* FROM user_details u                                  │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│         WHERE 1=1 AND u.active = true AND a.city = 'London'                      │
│    → JOIN added because city filter is present                                   │
│                                                                                  │
│  Scenario 3: advancedSearch("Bob", true, "London", "UK")                         │
│    SQL: SELECT DISTINCT u.* FROM user_details u                                  │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│         WHERE 1=1 AND u.user_name LIKE '%Bob%'                                   │
│         AND u.active = true AND a.city = 'London' AND a.country = 'UK'           │
│    → All filters applied, JOIN included                                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 2: Aggregation with GROUP BY and HAVING:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic report: count users per city, with optional filters.
     * Returns aggregated data — not entities.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Object[]> getUserCountByCity(Boolean active, Integer minCount) {

        StringBuilder sql = new StringBuilder(
            "SELECT a.city, a.country, COUNT(DISTINCT u.id) as user_count " +
            "FROM user_details u " +
            "JOIN user_addresses a ON u.id = a.user_id " +
            "WHERE 1=1"
        );

        if (active != null) {
            sql.append(" AND u.active = :active");
        }

        sql.append(" GROUP BY a.city, a.country");

        if (minCount != null && minCount > 0) {
            sql.append(" HAVING COUNT(DISTINCT u.id) >= :minCount");
        }

        sql.append(" ORDER BY user_count DESC");

        Query query = entityManager.createNativeQuery(sql.toString());

        if (active != null) {
            query.setParameter("active", active);
        }
        if (minCount != null && minCount > 0) {
            query.setParameter("minCount", minCount);
        }

        return query.getResultList();
        // Returns: List<Object[]>
        //   Object[] { "New York", "USA", 15 }
        //   Object[] { "London",   "UK",  12 }
        //   Object[] { "Mumbai",   "India", 8 }
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Aggregation Query — Scenarios:                                                  │
│                                                                                  │
│  Scenario 1: getUserCountByCity(true, null)                                      │
│    SQL: SELECT a.city, a.country, COUNT(DISTINCT u.id) as user_count             │
│         FROM user_details u                                                      │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│         WHERE 1=1 AND u.active = true                                            │
│         GROUP BY a.city, a.country                                               │
│         ORDER BY user_count DESC                                                 │
│    → Counts active users per city, no minimum threshold                          │
│                                                                                  │
│  Scenario 2: getUserCountByCity(null, 10)                                        │
│    SQL: SELECT a.city, a.country, COUNT(DISTINCT u.id) as user_count             │
│         FROM user_details u                                                      │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│         WHERE 1=1                                                                │
│         GROUP BY a.city, a.country                                               │
│         HAVING COUNT(DISTINCT u.id) >= 10                                        │
│         ORDER BY user_count DESC                                                 │
│    → All users, only cities with 10+ users                                       │
│                                                                                  │
│  Scenario 3: getUserCountByCity(true, 5)                                         │
│    SQL: ... WHERE 1=1 AND u.active = true                                        │
│         GROUP BY ... HAVING COUNT(...) >= 5                                       │
│         ORDER BY user_count DESC                                                 │
│    → Active users, only cities with 5+ active users                              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 3: Dynamic UPDATE with EntityManager:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic bulk update — only SET the fields that are non-null.
     */
    @Transactional
    public int dynamicUpdate(Long id, String newName, String newEmail, Boolean newActive) {

        StringBuilder sql = new StringBuilder("UPDATE user_details SET ");
        List<String> setClauses = new ArrayList<>();

        if (newName != null) {
            setClauses.add("user_name = :name");
        }
        if (newEmail != null) {
            setClauses.add("email = :email");
        }
        if (newActive != null) {
            setClauses.add("active = :active");
        }

        if (setClauses.isEmpty()) {
            return 0;  // nothing to update
        }

        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE id = :id");

        Query query = entityManager.createNativeQuery(sql.toString());

        if (newName != null) {
            query.setParameter("name", newName);
        }
        if (newEmail != null) {
            query.setParameter("email", newEmail);
        }
        if (newActive != null) {
            query.setParameter("active", newActive);
        }
        query.setParameter("id", id);

        return query.executeUpdate();
        // Returns int = number of rows affected (0 or 1)
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dynamic UPDATE — Scenarios:                                                     │
│                                                                                  │
│  Scenario 1: dynamicUpdate(1, "Alice Updated", null, null)                       │
│    SQL: UPDATE user_details SET user_name = :name WHERE id = :id                 │
│    Final: UPDATE user_details SET user_name = 'Alice Updated' WHERE id = 1       │
│    → Only name updated. Email and active unchanged.                              │
│                                                                                  │
│  Scenario 2: dynamicUpdate(2, null, "bob.new@ex.com", false)                     │
│    SQL: UPDATE user_details SET email = :email, active = :active WHERE id = :id  │
│    Final: UPDATE user_details SET email = 'bob.new@ex.com', active = false       │
│           WHERE id = 2                                                           │
│    → Email and active updated. Name unchanged.                                   │
│                                                                                  │
│  Scenario 3: dynamicUpdate(3, "Chuck", "chuck@ex.com", true)                     │
│    SQL: UPDATE user_details SET user_name = :name, email = :email,               │
│         active = :active WHERE id = :id                                          │
│    Final: UPDATE user_details SET user_name = 'Chuck',                           │
│           email = 'chuck@ex.com', active = true WHERE id = 3                     │
│    → All three fields updated.                                                   │
│                                                                                  │
│  NOTE: executeUpdate() returns int, not entities.                                │
│  The Persistence Context is NOT updated — cached entities are STALE.             │
│  If you need fresh data after update, call entityManager.clear()                 │
│  or entityManager.refresh(entity).                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Summary — Dynamic Native Query Patterns

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dynamic Native Query — Complete API:                                            │
│                                                                                  │
│  @PersistenceContext                                                             │
│  private EntityManager entityManager;                                            │
│                                                                                  │
│  Creating:                                                                       │
│    entityManager.createNativeQuery(sql)                  → List<Object[]>        │
│    entityManager.createNativeQuery(sql, Entity.class)    → List<Entity>          │
│                                                                                  │
│  Binding parameters:                                                             │
│    query.setParameter("name", value)     → named :name                           │
│    query.setParameter(1, value)          → positional ?1                         │
│                                                                                  │
│  Pagination:                                                                     │
│    query.setFirstResult(page * size)     → OFFSET                                │
│    query.setMaxResults(size)             → LIMIT                                 │
│                                                                                  │
│  Executing:                                                                      │
│    query.getResultList()                 → SELECT → List                          │
│    query.getSingleResult()               → SELECT → single Object                │
│    query.executeUpdate()                 → INSERT/UPDATE/DELETE → int             │
│                                                                                  │
│  Building dynamic SQL:                                                           │
│    StringBuilder sql = new StringBuilder("SELECT ... WHERE 1=1");                │
│    if (param != null) sql.append(" AND column = :param");                        │
│    → Only non-null filters are added to the WHERE clause                        │
│    → Always use setParameter() for values (prevents SQL injection)               │
│    → Whitelist validate column names for ORDER BY (can't parameterize)           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
When to use which query approach — Complete Decision Table:

  ┌─────────────────────────────────────┬──────────────────────────────────────────┐
  │ Scenario                            │ Best Approach                            │
  ├─────────────────────────────────────┼──────────────────────────────────────────┤
  │ Simple findBy with 1-2 conditions   │ Derived Query (method name)              │
  │ Fixed complex query on entities     │ JPQL @Query on Repository                │
  │ Fixed complex query on tables       │ Native @Query on Repository              │
  │ DB-specific features (JSONB, etc.)  │ Native @Query on Repository              │
  │ Optional filters (search/filter API)│ Dynamic Query with EntityManager         │
  │ Dynamic ORDER BY from user input    │ Dynamic Query with EntityManager         │
  │ Dynamic JOINs (conditional)         │ Dynamic Query with EntityManager         │
  │ Dynamic GROUP BY / HAVING           │ Dynamic Query with EntityManager         │
  │ Dynamic UPDATE (partial fields)     │ Dynamic Query with EntityManager         │
  │ Report queries with many variations │ Dynamic Query with EntityManager         │
  └─────────────────────────────────────┴──────────────────────────────────────────┘
```

---


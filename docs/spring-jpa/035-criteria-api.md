### What Is the Criteria API?

The **Criteria API** is a programmatic, type-safe way to build JPA queries using Java objects instead of writing query strings (JPQL or raw SQL). It is part of the **JPA specification** (javax.persistence.criteria / jakarta.persistence.criteria) and is implemented by Hibernate.

Instead of writing `"SELECT u FROM UserDetails u WHERE u.active = true"` as a string, you build the same query using method calls on `CriteriaBuilder`, `CriteriaQuery`, `Root`, `Predicate`, etc. The result is:
- **Type-safe**: compile-time checks catch errors (no typos in field names if using metamodel)
- **Database-independent**: uses JPA abstraction — Hibernate generates the correct SQL dialect
- **Dynamic**: build queries programmatically, adding/removing conditions at runtime

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Query Approaches — Evolution:                                                   │
│                                                                                  │
│  1. Derived Query (method name)                                                  │
│     findByActiveTrue()                                                           │
│     → Simplest. Fixed at compile time. Spring parses the method name.            │
│                                                                                  │
│  2. JPQL (@Query)                                                                │
│     @Query("SELECT u FROM UserDetails u WHERE u.active = :active")               │
│     → String-based. Entity field names. DB-independent. Fixed query.             │
│                                                                                  │
│  3. Native Query (@Query nativeQuery=true)                                       │
│     @Query(value = "SELECT * FROM user_details WHERE active = true",             │
│            nativeQuery = true)                                                   │
│     → String-based. DB column names. DB-dependent. Fixed query.                  │
│                                                                                  │
│  4. Dynamic Native Query (EntityManager + createNativeQuery)                     │
│     StringBuilder sql = "SELECT * FROM user_details WHERE 1=1";                  │
│     if (name != null) sql.append(" AND user_name = :name");                      │
│     → String-based. DB column names. DB-dependent. DYNAMIC query.               │
│                                                                                  │
│  5. Criteria API (CriteriaBuilder + CriteriaQuery)          ← THIS SECTION      │
│     CriteriaBuilder cb = entityManager.getCriteriaBuilder();                     │
│     CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);           │
│     Root<UserDetails> root = cq.from(UserDetails.class);                         │
│     cq.select(root).where(cb.equal(root.get("active"), true));                   │
│     → Object-based. Java field names. DB-independent. DYNAMIC query.            │
│     → Type-safe (especially with Metamodel). No raw SQL strings at all.          │
│                                                                                  │
│  KEY INSIGHT:                                                                    │
│  Criteria API = Dynamic queries (like EntityManager native) + DB independence    │
│                 (like JPQL) + Type safety (no strings at all with Metamodel)      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Database Tables (used in all examples below):

  user_details                                          user_addresses
  ┌────┬───────────┬──────────────────┬────────┐        ┌────┬─────────┬──────────┬─────────┬────────────┐
  │ id │ user_name │ email            │ active │        │ id │ street  │ city     │ country │ user_id(FK)│
  ├────┼───────────┼──────────────────┼────────┤        ├────┼─────────┼──────────┼─────────┼────────────┤
  │  1 │ Alice     │ alice@ex.com     │ true   │        │ 10 │ Main St │ New York │ USA     │     1      │
  │  2 │ Bob       │ bob@ex.com       │ true   │        │ 11 │ Oak Ave │ London   │ UK      │     2      │
  │  3 │ Charlie   │ charlie@ex.com   │ false  │        │ 12 │ Pine Rd │ Mumbai   │ India   │     1      │
  │  4 │ Diana     │ diana@ex.com     │ true   │        │ 13 │ Elm St  │ Paris    │ France  │     3      │
  │  5 │ Eve       │ eve@ex.com       │ false  │        │ 14 │ Bay Dr  │ New York │ USA     │     4      │
  │  6 │ Frank     │ frank@ex.com     │ true   │        └────┴─────────┴──────────┴─────────┴────────────┘
  │  7 │ Grace     │ grace@ex.com     │ true   │
  │  8 │ Hank      │ hank@ex.com      │ false  │
  └────┴───────────┴──────────────────┴────────┘

  Entity classes:

  @Entity
  @Table(name = "user_details")
  public class UserDetails {
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(name = "user_name")
      private String name;

      @Column(name = "email")
      private String email;

      @Column(name = "active")
      private Boolean active;

      @OneToMany(mappedBy = "userDetails", fetch = FetchType.LAZY)
      private List<UserAddress> addresses;
      // getters, setters
  }

  @Entity
  @Table(name = "user_addresses")
  public class UserAddress {
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(name = "street")
      private String street;

      @Column(name = "city")
      private String city;

      @Column(name = "country")
      private String country;

      @ManyToOne(fetch = FetchType.LAZY)
      @JoinColumn(name = "user_id")
      private UserDetails userDetails;
      // getters, setters
  }
```

---

### Native Query vs Criteria API — DB Dependency vs JPA Abstraction

Dynamic Native Queries (using `EntityManager.createNativeQuery()`) let you build queries at runtime, but they are **database-dependent** — you write raw SQL with DB-specific column names, functions, and syntax. The Criteria API achieves the same dynamic query building but through **JPA abstraction** — you use Java field names, and Hibernate translates to the correct SQL dialect for your database.

```java
// APPROACH 1: Dynamic Native Query — database dependent
@Service
public class UserServiceNative {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<UserDetails> searchUsers(String name, Boolean active) {

        // Raw SQL — uses DB column names (user_name, not "name")
        StringBuilder sql = new StringBuilder("SELECT * FROM user_details WHERE 1=1");

        if (name != null) {
            sql.append(" AND user_name LIKE :name");   // ← DB column name "user_name"
        }
        if (active != null) {
            sql.append(" AND active = :active");       // ← DB column name "active"
        }

        Query query = entityManager.createNativeQuery(sql.toString(), UserDetails.class);

        if (name != null) {
            query.setParameter("name", "%" + name + "%");
        }
        if (active != null) {
            query.setParameter("active", active);
        }

        return query.getResultList();
    }
    // Generated SQL (MySQL):
    //   SELECT * FROM user_details WHERE 1=1 AND user_name LIKE '%Alice%' AND active = true
    //
    // If you switch to Oracle → still works (basic SQL is universal)
    // But if you used MySQL-specific functions (LIMIT, IFNULL, etc.) → BREAKS on Oracle
}
```

```java
// APPROACH 2: Criteria API — database independent
@Service
public class UserServiceCriteria {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<UserDetails> searchUsers(String name, Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        // Build predicates dynamically — uses JAVA FIELD names
        List<Predicate> predicates = new ArrayList<>();

        if (name != null) {
            predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            //                          ↑ Java field name "name", NOT DB column "user_name"
        }
        if (active != null) {
            predicates.add(cb.equal(root.get("active"), active));
            //                           ↑ Java field name "active"
        }

        cq.select(root).where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getResultList();
    }
    // Generated SQL (MySQL dialect):
    //   SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active
    //   FROM user_details u1_0
    //   WHERE u1_0.user_name LIKE '%Alice%' AND u1_0.active = true
    //
    // Generated SQL (Oracle dialect):
    //   SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active
    //   FROM user_details u1_0
    //   WHERE u1_0.user_name LIKE '%Alice%' AND u1_0.active = 1
    //                                                        ↑ Oracle uses 1/0 for boolean
    //
    // Hibernate handles the dialect difference automatically!
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Native Query vs Criteria API — Side by Side:                                    │
│                                                                                  │
│  ┌───────────────────────────────┬──────────────────────┬────────────────────────┐│
│  │ Aspect                        │ Dynamic Native Query │ Criteria API           ││
│  ├───────────────────────────────┼──────────────────────┼────────────────────────┤│
│  │ Query language                │ Raw SQL strings      │ Java method calls      ││
│  │ Column/field references       │ DB column names      │ Java field names       ││
│  │                               │ ("user_name")        │ ("name")               ││
│  │ DB independence               │ ✗ DB-specific SQL    │ ✓ Hibernate generates  ││
│  │                               │                      │   dialect-specific SQL ││
│  │ Type safety                   │ ✗ String-based       │ ✓ Compile-time checks  ││
│  │                               │   (runtime errors)   │   (with Metamodel)     ││
│  │ Dynamic query building        │ ✓ StringBuilder      │ ✓ Predicate list       ││
│  │ SQL injection risk            │ ✗ Must use           │ ✓ Parameterized by     ││
│  │                               │   setParameter()     │   design (no raw SQL)  ││
│  │ DB-specific features          │ ✓ Full access        │ ✗ Limited to JPA spec  ││
│  │ (JSONB, window functions)     │                      │                        ││
│  │ Performance                   │ Slightly faster      │ Slightly slower        ││
│  │                               │ (no JPQL parsing)    │ (builds AST → SQL)     ││
│  │ Readability                   │ ✓ Familiar SQL       │ ✗ Verbose Java code    ││
│  │ Maintainability               │ ✗ String fragile     │ ✓ Refactor-safe        ││
│  └───────────────────────────────┴──────────────────────┴────────────────────────┘│
│                                                                                  │
│  WHEN TO USE CRITERIA API over Dynamic Native Query:                             │
│    - Application must support multiple databases (MySQL + Oracle + PostgreSQL)   │
│    - You want compile-time safety (catch typos in field names early)             │
│    - You want to avoid SQL injection risk entirely (no string concatenation)     │
│    - You are building complex dynamic queries with many optional conditions      │
│                                                                                  │
│  WHEN TO USE DYNAMIC NATIVE QUERY over Criteria API:                             │
│    - You need DB-specific features (JSONB, full-text search, window functions)   │
│    - You need maximum query performance (skip Criteria → JPQL → SQL translation)│
│    - Your team prefers reading SQL over verbose Criteria API code                │
│    - Application is locked to a single database                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  DB Portability — Criteria API vs Native Query:                                  │
│                                                                                  │
│  Scenario: Boolean column "active" — switching from MySQL to Oracle              │
│                                                                                  │
│  Native Query:                                                                   │
│    MySQL:  "SELECT * FROM user_details WHERE active = true"   ← works            │
│    Oracle: "SELECT * FROM user_details WHERE active = true"   ← ERROR!           │
│            Oracle has no BOOLEAN type. Uses NUMBER(1): 0 or 1.                   │
│    Fix:    "SELECT * FROM user_details WHERE active = 1"      ← Oracle-specific  │
│    → You must maintain different SQL strings per database!                        │
│                                                                                  │
│  Criteria API:                                                                   │
│    cb.equal(root.get("active"), true)                                            │
│    MySQL:  → WHERE active = true      (Hibernate MySQL dialect)                  │
│    Oracle: → WHERE active = 1         (Hibernate Oracle dialect)                 │
│    → Same Java code. Hibernate handles the translation automatically.            │
│                                                                                  │
│  Scenario: Pagination — switching from MySQL to Oracle                           │
│                                                                                  │
│  Native Query:                                                                   │
│    MySQL:  "SELECT * FROM user_details LIMIT 10 OFFSET 20"                       │
│    Oracle: "SELECT * FROM user_details OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"  │
│    → Different syntax! Must rewrite.                                             │
│                                                                                  │
│  Criteria API:                                                                   │
│    query.setFirstResult(20);                                                     │
│    query.setMaxResults(10);                                                      │
│    MySQL:  → LIMIT 10 OFFSET 20                                                  │
│    Oracle: → OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY                             │
│    → Same Java code. Hibernate generates dialect-specific pagination.            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### How Criteria API Works — JPA Abstraction and Type Safety

The Criteria API operates entirely at the **JPA entity level**, not the database level. You reference Java field names, Java types, and entity relationships. Hibernate's Criteria engine builds an internal AST (Abstract Syntax Tree), then translates it to SQL using the configured dialect.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API — How it generates SQL (internal flow):                            │
│                                                                                  │
│  Your Java code:                                                                 │
│    CriteriaBuilder cb = entityManager.getCriteriaBuilder();                      │
│    CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);            │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    cq.select(root).where(cb.equal(root.get("name"), "Alice"));                   │
│    TypedQuery<UserDetails> query = entityManager.createQuery(cq);                │
│    List<UserDetails> result = query.getResultList();                              │
│       │                                                                          │
│       v                                                                          │
│  Step 1: CriteriaBuilder builds an AST (Abstract Syntax Tree)                   │
│    SelectStatement                                                               │
│      ├─ FROM: UserDetails (entity) → table "user_details"                        │
│      ├─ SELECT: root (all fields)                                                │
│      └─ WHERE: EqualPredicate                                                    │
│                  ├─ left: root.get("name") → @Column(name="user_name")           │
│                  └─ right: "Alice" (literal)                                     │
│       │                                                                          │
│       v                                                                          │
│  Step 2: Hibernate reads entity metadata                                         │
│    UserDetails.class:                                                            │
│      @Table(name = "user_details")                                               │
│      field "name" → @Column(name = "user_name")                                  │
│      field "email" → @Column(name = "email")                                     │
│      field "active" → @Column(name = "active")                                   │
│       │                                                                          │
│       v                                                                          │
│  Step 3: SQL Generator (uses configured Dialect)                                 │
│    Dialect = MySQL8Dialect                                                        │
│    → SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name = 'Alice'                                              │
│       │                                                                          │
│       v                                                                          │
│  Step 4: JDBC PreparedStatement → executed on DB → ResultSet                     │
│       │                                                                          │
│       v                                                                          │
│  Step 5: ResultSet → mapped to UserDetails entities (managed in L1 cache)        │
│                                                                                  │
│  KEY: You wrote "name" (Java field). Hibernate resolved it to "user_name" (DB).  │
│  KEY: If you rename the DB column, only @Column annotation changes.              │
│       Your Criteria code stays the same.                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Type Safety — String-based vs Metamodel:**

The Criteria API supports two ways to reference entity fields:

```java
// Option 1: String-based field reference (not fully type-safe)
root.get("name")       // field name as String — typo causes runtime error
root.get("naem")       // ← typo! Compiles fine. Fails at RUNTIME.

// Option 2: JPA Metamodel (fully type-safe)
root.get(UserDetails_.name)     // field reference as generated class constant
root.get(UserDetails_.naem)     // ← typo! COMPILE ERROR. Caught immediately.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JPA Metamodel — Static type safety:                                             │
│                                                                                  │
│  Hibernate can generate a "metamodel" class for each entity at compile time.     │
│  This class has static fields for every entity attribute.                        │
│                                                                                  │
│  Entity:                                          Generated Metamodel:           │
│  @Entity                                          @StaticMetamodel(UserDetails)  │
│  public class UserDetails {                       public class UserDetails_ {    │
│      @Id                                              public static volatile     │
│      private Long id;                                   SingularAttribute<       │
│      @Column(name="user_name")                            UserDetails, Long> id; │
│      private String name;                             public static volatile     │
│      @Column(name="email")                              SingularAttribute<       │
│      private String email;                                UserDetails,String>name│
│      @Column(name="active")                           public static volatile     │
│      private Boolean active;                            SingularAttribute<       │
│  }                                                        UserDetails,String>    │
│                                                           email;                 │
│                                                       public static volatile     │
│                                                         SingularAttribute<       │
│                                                           UserDetails,Boolean>   │
│                                                           active;                │
│                                                   }                              │
│                                                                                  │
│  Usage with Metamodel:                                                           │
│    cb.equal(root.get(UserDetails_.name), "Alice")                                │
│    cb.greaterThan(root.get(UserDetails_.id), 5L)                                 │
│                                                                                  │
│  Benefits:                                                                       │
│    - Typo in field name → COMPILE ERROR (not runtime)                            │
│    - Type mismatch → COMPILE ERROR                                               │
│      e.g., cb.equal(root.get(UserDetails_.name), 42) → error (String != int)    │
│    - IDE autocomplete works — root.get(UserDetails_. → shows all fields          │
│    - When entity field is renamed → refactoring updates Metamodel too            │
│                                                                                  │
│  NOTE: In examples below, we use String-based root.get("name") for simplicity.  │
│  In production, prefer the Metamodel approach for type safety.                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Type Safety Comparison:                                                         │
│                                                                                  │
│  ┌─────────────────────────────────┬──────────────────┬──────────────────────────┐│
│  │ Approach                        │ Field Reference   │ Typo Detection           ││
│  ├─────────────────────────────────┼──────────────────┼──────────────────────────┤│
│  │ Native Query (raw SQL)          │ "user_name"      │ Runtime only             ││
│  │ JPQL (@Query string)            │ "u.name"         │ Startup (Spring parses)  ││
│  │ Criteria API (String-based)     │ root.get("name") │ Runtime (query exec)     ││
│  │ Criteria API (Metamodel)        │ root.get(U_.name)│ COMPILE TIME             ││
│  └─────────────────────────────────┴──────────────────┴──────────────────────────┘│
│                                                                                  │
│  Criteria API with Metamodel = SAFEST approach for dynamic queries               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### CriteriaBuilder → CriteriaQuery → TypedQuery — The Three Steps

Every Criteria API query follows this pattern:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API — Three Step Pattern:                                              │
│                                                                                  │
│  Step 1: Get CriteriaBuilder from EntityManager                                  │
│    CriteriaBuilder cb = entityManager.getCriteriaBuilder();                      │
│    → CriteriaBuilder is a FACTORY for creating query components:                 │
│      predicates (WHERE conditions), expressions, orderings, etc.                 │
│                                                                                  │
│  Step 2: Build CriteriaQuery using CriteriaBuilder                               │
│    CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);            │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    cq.select(root).where(cb.equal(root.get("active"), true));                    │
│    → CriteriaQuery defines the STRUCTURE of the query:                           │
│      what to SELECT, FROM which entity, WHERE conditions, ORDER BY, GROUP BY     │
│                                                                                  │
│  Step 3: Create TypedQuery from CriteriaQuery and execute                        │
│    TypedQuery<UserDetails> query = entityManager.createQuery(cq);                │
│    List<UserDetails> results = query.getResultList();                             │
│    → TypedQuery is the EXECUTABLE form — translates to SQL and runs it           │
│                                                                                  │
│  Flow diagram:                                                                   │
│                                                                                  │
│  EntityManager                                                                   │
│       │                                                                          │
│       ├─ .getCriteriaBuilder() ──→ CriteriaBuilder (factory)                     │
│       │                                │                                         │
│       │                                ├─ .createQuery(Class) → CriteriaQuery    │
│       │                                ├─ .equal()    → Predicate                │
│       │                                ├─ .like()     → Predicate                │
│       │                                ├─ .and()      → Predicate                │
│       │                                ├─ .or()       → Predicate                │
│       │                                ├─ .asc()      → Order                    │
│       │                                ├─ .desc()     → Order                    │
│       │                                ├─ .count()    → Expression               │
│       │                                └─ .sum()      → Expression               │
│       │                                                                          │
│       │   CriteriaQuery                                                          │
│       │       │                                                                  │
│       │       ├─ .from(Entity.class) → Root<Entity>                              │
│       │       ├─ .select(root)                                                   │
│       │       ├─ .where(predicates)                                              │
│       │       ├─ .orderBy(orders)                                                │
│       │       ├─ .groupBy(expressions)                                           │
│       │       └─ .having(predicate)                                              │
│       │                                                                          │
│       └─ .createQuery(criteriaQuery) ──→ TypedQuery<T>                           │
│                                              │                                   │
│                                              ├─ .getResultList() → List<T>       │
│                                              ├─ .getSingleResult() → T           │
│                                              ├─ .setFirstResult(offset)          │
│                                              ├─ .setMaxResults(limit)            │
│                                              └─ .setParameter(name, value)       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### CriteriaBuilder — All Comparison and Logical Operators

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  CriteriaBuilder — Comparison Operators:                                         │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ CriteriaBuilder Method                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ cb.equal(expr, value)                    │ column = value                    ││
│  │ cb.notEqual(expr, value)                 │ column != value (column <> value) ││
│  │ cb.greaterThan(expr, value)              │ column > value                    ││
│  │ cb.greaterThanOrEqualTo(expr, value)     │ column >= value                   ││
│  │ cb.lessThan(expr, value)                 │ column < value                    ││
│  │ cb.lessThanOrEqualTo(expr, value)        │ column <= value                   ││
│  │ cb.between(expr, low, high)              │ column BETWEEN low AND high       ││
│  │ cb.like(expr, pattern)                   │ column LIKE pattern               ││
│  │ cb.notLike(expr, pattern)                │ column NOT LIKE pattern           ││
│  │ cb.isNull(expr)                          │ column IS NULL                    ││
│  │ cb.isNotNull(expr)                       │ column IS NOT NULL                ││
│  │ cb.in(expr)  or  expr.in(collection)     │ column IN (val1, val2, ...)       ││
│  │ cb.isTrue(expr)                          │ column = true (or = 1)            ││
│  │ cb.isFalse(expr)                         │ column = false (or = 0)           ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  CriteriaBuilder — Logical Operators:                                            │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ CriteriaBuilder Method                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ cb.and(predicate1, predicate2)           │ cond1 AND cond2                   ││
│  │ cb.or(predicate1, predicate2)            │ cond1 OR cond2                    ││
│  │ cb.not(predicate)                        │ NOT cond                          ││
│  │ cb.conjunction()                         │ 1=1 (always true — AND identity)  ││
│  │ cb.disjunction()                         │ 1=0 (always false — OR identity)  ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  CriteriaBuilder — Aggregate Functions:                                          │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ CriteriaBuilder Method                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ cb.count(expr)                           │ COUNT(column)                     ││
│  │ cb.countDistinct(expr)                   │ COUNT(DISTINCT column)            ││
│  │ cb.sum(expr)                             │ SUM(column)                       ││
│  │ cb.avg(expr)                             │ AVG(column)                       ││
│  │ cb.max(expr)                             │ MAX(column)                       ││
│  │ cb.min(expr)                             │ MIN(column)                       ││
│  │ cb.greatest(expr)                        │ GREATEST(column) (like max)       ││
│  │ cb.least(expr)                           │ LEAST(column) (like min)          ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  CriteriaBuilder — String Functions:                                             │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ CriteriaBuilder Method                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ cb.upper(expr)                           │ UPPER(column)                     ││
│  │ cb.lower(expr)                           │ LOWER(column)                     ││
│  │ cb.concat(expr1, expr2)                  │ CONCAT(col1, col2)               ││
│  │ cb.substring(expr, start, len)           │ SUBSTRING(column, start, len)    ││
│  │ cb.trim(expr)                            │ TRIM(column)                     ││
│  │ cb.length(expr)                          │ LENGTH(column)                   ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  TypedQuery — Execution Methods:                                                 │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ TypedQuery Method                        │ Meaning                           ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ query.getResultList()                    │ Execute SELECT → List<T>          ││
│  │ query.getSingleResult()                  │ Execute SELECT → single T         ││
│  │                                          │ (throws if 0 or 2+ results)      ││
│  │ query.setFirstResult(offset)             │ OFFSET (for pagination)           ││
│  │ query.setMaxResults(limit)               │ LIMIT (for pagination)            ││
│  │ query.setParameter(name, value)          │ Bind named parameter              ││
│  │ query.setParameter(position, value)      │ Bind positional parameter         ││
│  │ query.setHint(name, value)               │ Set query hint (cache, timeout)   ││
│  │ query.setLockMode(LockModeType)          │ Set lock mode (PESSIMISTIC, etc.) ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Complete Service + Controller Example — Dynamic Search with Criteria API

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic search using Criteria API.
     * Filters are optional — only non-null parameters become WHERE conditions.
     */
    @Transactional(readOnly = true)
    public List<UserDetails> searchUsers(String name, String email, Boolean active) {

        // Step 1: Get CriteriaBuilder
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Step 2: Create CriteriaQuery for UserDetails entity
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        //                                 ↑ result type = UserDetails

        // Step 3: Define the FROM clause — the root entity
        Root<UserDetails> root = cq.from(UserDetails.class);
        //  root represents the "user_details" table
        //  root.get("name") = reference to "user_name" column
        //  root.get("email") = reference to "email" column

        // Step 4: Build predicates (WHERE conditions) dynamically
        List<Predicate> predicates = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            // SQL: user_name LIKE '%Alice%'
        }

        if (email != null && !email.isEmpty()) {
            predicates.add(cb.equal(root.get("email"), email));
            // SQL: email = 'alice@ex.com'
        }

        if (active != null) {
            predicates.add(cb.equal(root.get("active"), active));
            // SQL: active = true
        }

        // Step 5: Apply SELECT and WHERE
        cq.select(root).where(predicates.toArray(new Predicate[0]));
        //  .where() accepts Predicate[] (varargs)
        //  Multiple predicates are combined with AND by default.

        // Step 6: Create TypedQuery and execute
        TypedQuery<UserDetails> query = entityManager.createQuery(cq);
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
    // GET /api/users/search?name=Alice&email=alice@ex.com&active=true
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
│  Criteria API — Generated SQL for each scenario:                                 │
│                                                                                  │
│  Scenario 1: searchUsers("Alice", null, null)                                    │
│    Predicates: [cb.like(root.get("name"), "%Alice%")]                            │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Alice%'                                         │
│    Result: [UserDetails(id=1, name="Alice", ...)]                                │
│                                                                                  │
│  Scenario 2: searchUsers(null, null, true)                                       │
│    Predicates: [cb.equal(root.get("active"), true)]                              │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.active = true                                                    │
│    Result: [Alice, Bob, Diana, Frank, Grace] — 5 active users                    │
│                                                                                  │
│  Scenario 3: searchUsers("Bob", "bob@ex.com", true)                              │
│    Predicates: [like, equal(email), equal(active)]                               │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Bob%'                                           │
│        AND u1_0.email = 'bob@ex.com'                                             │
│        AND u1_0.active = true                                                    │
│    Result: [UserDetails(id=2, name="Bob", ...)]                                  │
│                                                                                  │
│  Scenario 4: searchUsers(null, null, null)                                       │
│    Predicates: [] (empty)                                                        │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│    Result: All 8 users (no WHERE clause at all)                                  │
│                                                                                  │
│  NOTE: When predicates list is empty, .where() with empty array = no WHERE.      │
│  This is the Criteria API equivalent of the "WHERE 1=1" trick.                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Using OR, NOT, and complex logic:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Complex search with OR and NOT logic:
     * Find users where (name LIKE keyword OR email LIKE keyword) AND active = true
     */
    @Transactional(readOnly = true)
    public List<UserDetails> complexSearch(String keyword, Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        List<Predicate> predicates = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            // OR condition: name LIKE keyword OR email LIKE keyword
            Predicate nameLike = cb.like(root.get("name"), "%" + keyword + "%");
            Predicate emailLike = cb.like(root.get("email"), "%" + keyword + "%");
            predicates.add(cb.or(nameLike, emailLike));
            // SQL: (user_name LIKE '%keyword%' OR email LIKE '%keyword%')
        }

        if (active != null) {
            predicates.add(cb.equal(root.get("active"), active));
        }

        // All predicates combined with AND
        cq.select(root).where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getResultList();
    }

    /**
     * Using IN, BETWEEN, IS NOT NULL:
     */
    @Transactional(readOnly = true)
    public List<UserDetails> advancedSearch(List<Long> ids, Long minId, Long maxId) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        List<Predicate> predicates = new ArrayList<>();

        if (ids != null && !ids.isEmpty()) {
            predicates.add(root.get("id").in(ids));
            // SQL: id IN (1, 2, 5)
        }

        if (minId != null && maxId != null) {
            predicates.add(cb.between(root.get("id"), minId, maxId));
            // SQL: id BETWEEN 3 AND 7
        }

        predicates.add(cb.isNotNull(root.get("email")));
        // SQL: email IS NOT NULL

        cq.select(root).where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getResultList();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Complex Logic — Generated SQL:                                                  │
│                                                                                  │
│  complexSearch("ali", true):                                                     │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE (u1_0.user_name LIKE '%ali%' OR u1_0.email LIKE '%ali%')                │
│      AND u1_0.active = true                                                      │
│    Result: [Alice] — name matches "ali", active = true                           │
│                                                                                  │
│  advancedSearch([1, 2, 5], null, null):                                          │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.id IN (1, 2, 5)                                                    │
│      AND u1_0.email IS NOT NULL                                                  │
│    Result: [Alice, Bob, Eve]                                                     │
│                                                                                  │
│  advancedSearch(null, 3L, 7L):                                                   │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.id BETWEEN 3 AND 7                                                 │
│      AND u1_0.email IS NOT NULL                                                  │
│    Result: [Charlie, Diana, Eve, Frank, Grace]                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Multiselect — Selecting Multiple Fields as List<Object[]>

When you don't need the full entity but only specific fields, use `cq.multiselect()` to select individual columns. The result type is `Object[]` — each array element corresponds to one selected field.

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Select only name and email — returns List<Object[]>
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUserNamesAndEmails(Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Result type = Object[] (not entity — because we select partial fields)
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<UserDetails> root = cq.from(UserDetails.class);

        // multiselect — select specific fields
        cq.multiselect(
            root.get("name"),     // index 0 → user_name column
            root.get("email")     // index 1 → email column
        );

        if (active != null) {
            cq.where(cb.equal(root.get("active"), active));
        }

        TypedQuery<Object[]> query = entityManager.createQuery(cq);
        return query.getResultList();
    }
}
```

```java
// Service — converting Object[] to DTO
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<UserEmailDTO> getUserEmails(Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        cq.multiselect(
            root.get("name"),
            root.get("email")
        );

        if (active != null) {
            cq.where(cb.equal(root.get("active"), active));
        }

        List<Object[]> results = entityManager.createQuery(cq).getResultList();

        // Convert Object[] to DTO
        return results.stream()
            .map(row -> new UserEmailDTO((String) row[0], (String) row[1]))
            .collect(Collectors.toList());
    }
}

// DTO
public class UserEmailDTO {
    private String name;
    private String email;

    public UserEmailDTO(String name, String email) {
        this.name = name;
        this.email = email;
    }
    // getters
    public String getName() { return name; }
    public String getEmail() { return email; }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/emails")
    public List<UserEmailDTO> getEmails(@RequestParam(required = false) Boolean active) {
        return userService.getUserEmails(active);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  multiselect — How it works:                                                     │
│                                                                                  │
│  Java code:                                                                      │
│    cq.multiselect(root.get("name"), root.get("email"));                          │
│                                                                                  │
│  Generated SQL:                                                                  │
│    SELECT u1_0.user_name, u1_0.email                                             │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.active = true                                                      │
│                                                                                  │
│  ResultSet:                                                                      │
│    ┌───────────┬──────────────────┐                                              │
│    │ user_name │ email            │                                              │
│    ├───────────┼──────────────────┤                                              │
│    │ Alice     │ alice@ex.com     │   → Object[] { "Alice", "alice@ex.com" }     │
│    │ Bob       │ bob@ex.com       │   → Object[] { "Bob",   "bob@ex.com" }       │
│    │ Diana     │ diana@ex.com     │   → Object[] { "Diana", "diana@ex.com" }     │
│    │ Frank     │ frank@ex.com     │   → Object[] { "Frank", "frank@ex.com" }     │
│    │ Grace     │ grace@ex.com     │   → Object[] { "Grace", "grace@ex.com" }     │
│    └───────────┴──────────────────┘                                              │
│                                                                                  │
│  Object[] index mapping:                                                         │
│    row[0] → "name"  (first in multiselect)  → user_name column                  │
│    row[1] → "email" (second in multiselect) → email column                       │
│                                                                                  │
│  Alternative — Direct DTO construction (no Object[] step):                       │
│    CriteriaQuery<UserEmailDTO> cq = cb.createQuery(UserEmailDTO.class);          │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    cq.select(cb.construct(UserEmailDTO.class,                                    │
│        root.get("name"),                                                         │
│        root.get("email")                                                         │
│    ));                                                                            │
│    → Hibernate calls new UserEmailDTO(name, email) directly!                     │
│    → No manual Object[] conversion needed.                                       │
│    → DTO constructor parameter order must match select order.                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Using cb.construct() for direct DTO mapping (cleaner approach):**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<UserEmailDTO> getUserEmailsDirect(Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Result type = DTO class directly
        CriteriaQuery<UserEmailDTO> cq = cb.createQuery(UserEmailDTO.class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        // cb.construct() — calls the DTO constructor directly
        cq.select(cb.construct(UserEmailDTO.class,
            root.get("name"),      // → 1st constructor parameter (String name)
            root.get("email")      // → 2nd constructor parameter (String email)
        ));

        if (active != null) {
            cq.where(cb.equal(root.get("active"), active));
        }

        return entityManager.createQuery(cq).getResultList();
        // Returns List<UserEmailDTO> directly — no Object[] conversion!
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  multiselect vs cb.construct — Comparison:                                       │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ cq.multiselect() → List<Object[]>        │ cb.construct() → List<DTO>        ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ Returns raw Object[]                     │ Returns DTO instances directly    ││
│  │ Manual casting: (String) row[0]          │ No casting needed                 ││
│  │ Index-based (fragile)                    │ Constructor-based (robust)        ││
│  │ Flexible — any column combination        │ Needs matching DTO constructor    ││
│  │ Good for prototyping/ad-hoc queries      │ Good for production code          ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  Both generate the same SQL:                                                     │
│    SELECT u1_0.user_name, u1_0.email FROM user_details u1_0 WHERE ...            │
│                                                                                  │
│  The difference is only in how the ResultSet is mapped to Java objects.           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Multiselect with aggregation:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Count users by active status — GROUP BY + aggregate
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUserCountByStatus() {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        cq.multiselect(
            root.get("active"),              // GROUP BY column
            cb.count(root.get("id"))         // COUNT(id)
        );

        cq.groupBy(root.get("active"));

        return entityManager.createQuery(cq).getResultList();
    }
    // Generated SQL:
    //   SELECT u1_0.active, COUNT(u1_0.id)
    //   FROM user_details u1_0
    //   GROUP BY u1_0.active
    //
    // Result:
    //   Object[] { true,  5 }   — 5 active users
    //   Object[] { false, 3 }   — 3 inactive users
}
```

---

### JOIN Two Entities Using Criteria API

The Criteria API supports JOINs through `Root.join()`, which follows JPA entity relationships (`@OneToMany`, `@ManyToOne`, etc.). Unlike native queries, you don't write `JOIN ... ON ...` — you reference the Java field that holds the relationship.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API JOIN — How it works:                                               │
│                                                                                  │
│  Entity relationship:                                                            │
│    UserDetails ───@OneToMany(mappedBy="userDetails")──→ List<UserAddress>        │
│    UserAddress ───@ManyToOne──→ UserDetails                                      │
│                                                                                  │
│  Criteria API:                                                                   │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    Join<UserDetails, UserAddress> addressJoin = root.join("addresses");           │
│    //          ↑ parent entity    ↑ child entity       ↑ Java field name         │
│    //  "addresses" is the field on UserDetails that holds List<UserAddress>       │
│                                                                                  │
│  Hibernate reads the relationship:                                               │
│    @OneToMany(mappedBy = "userDetails")                                          │
│    private List<UserAddress> addresses;                                           │
│    → Knows that user_addresses.user_id = user_details.id                         │
│                                                                                  │
│  Generated SQL:                                                                  │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                            │
│    //                          ↑ Hibernate generates the ON clause automatically │
│                                                                                  │
│  JOIN types:                                                                     │
│    root.join("addresses")                         → INNER JOIN (default)         │
│    root.join("addresses", JoinType.INNER)         → INNER JOIN                   │
│    root.join("addresses", JoinType.LEFT)          → LEFT OUTER JOIN              │
│    root.join("addresses", JoinType.RIGHT)         → RIGHT OUTER JOIN             │
│                                                                                  │
│  IMPORTANT: Unlike native query, you CANNOT join unrelated tables.               │
│  The entities MUST have a mapped JPA relationship (@OneToMany, @ManyToOne, etc.) │
│  For unrelated tables, use native query.                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 1: JOIN + multiselect — user name with city:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Search users by city — JOIN UserDetails with UserAddress.
     * Returns user name + city as Object[].
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUsersWithCity(String city) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        // FROM user_details
        Root<UserDetails> userRoot = cq.from(UserDetails.class);

        // JOIN user_addresses ON user_details.id = user_addresses.user_id
        Join<UserDetails, UserAddress> addressJoin = userRoot.join("addresses");
        //                                                    ↑ field name on UserDetails entity

        // SELECT user_name, city
        cq.multiselect(
            userRoot.get("name"),         // user_details.user_name
            addressJoin.get("city")       // user_addresses.city
        );

        // WHERE city = :city (optional)
        if (city != null && !city.isEmpty()) {
            cq.where(cb.equal(addressJoin.get("city"), city));
        }

        return entityManager.createQuery(cq).getResultList();
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

    // GET /api/users/by-city?city=New York
    @GetMapping("/by-city")
    public List<Object[]> getUsersByCity(@RequestParam(required = false) String city) {
        return userService.getUsersWithCity(city);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JOIN + multiselect — Scenarios:                                                 │
│                                                                                  │
│  Scenario 1: getUsersWithCity("New York")                                        │
│    Generated SQL:                                                                │
│      SELECT u1_0.user_name, a1_0.city                                            │
│      FROM user_details u1_0                                                      │
│      JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                          │
│      WHERE a1_0.city = 'New York'                                                │
│                                                                                  │
│    Result:                                                                       │
│      ┌───────────┬──────────┐                                                    │
│      │ user_name │ city     │                                                    │
│      ├───────────┼──────────┤                                                    │
│      │ Alice     │ New York │  → Object[] { "Alice", "New York" }                │
│      │ Diana     │ New York │  → Object[] { "Diana", "New York" }                │
│      └───────────┴──────────┘                                                    │
│    (Alice has address id=10 in New York, Diana has address id=14 in New York)    │
│                                                                                  │
│  Scenario 2: getUsersWithCity(null) — no filter                                  │
│    Generated SQL:                                                                │
│      SELECT u1_0.user_name, a1_0.city                                            │
│      FROM user_details u1_0                                                      │
│      JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                          │
│                                                                                  │
│    Result:                                                                       │
│      ┌───────────┬──────────┐                                                    │
│      │ user_name │ city     │                                                    │
│      ├───────────┼──────────┤                                                    │
│      │ Alice     │ New York │  → from address id=10                              │
│      │ Alice     │ Mumbai   │  → from address id=12 (Alice has 2 addresses)      │
│      │ Bob       │ London   │  → from address id=11                              │
│      │ Charlie   │ Paris    │  → from address id=13                              │
│      │ Diana     │ New York │  → from address id=14                              │
│      └───────────┴──────────┘                                                    │
│    NOTE: Alice appears TWICE because she has 2 addresses (INNER JOIN behavior)   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 2: JOIN + multiselect + dynamic conditions on both entities:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Advanced search across user_details and user_addresses.
     * Dynamic conditions on BOTH entities.
     */
    @Transactional(readOnly = true)
    public List<Object[]> advancedJoinSearch(String name, Boolean active,
                                              String city, String country) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<UserDetails> userRoot = cq.from(UserDetails.class);
        Join<UserDetails, UserAddress> addrJoin = userRoot.join("addresses", JoinType.LEFT);
        //                                                                   ↑ LEFT JOIN
        //  LEFT JOIN ensures users without addresses are still returned

        // SELECT user fields + address fields
        cq.multiselect(
            userRoot.get("id"),
            userRoot.get("name"),
            userRoot.get("active"),
            addrJoin.get("city"),
            addrJoin.get("country")
        );

        // Dynamic WHERE conditions
        List<Predicate> predicates = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            predicates.add(cb.like(userRoot.get("name"), "%" + name + "%"));
        }
        if (active != null) {
            predicates.add(cb.equal(userRoot.get("active"), active));
        }
        if (city != null && !city.isEmpty()) {
            predicates.add(cb.equal(addrJoin.get("city"), city));
        }
        if (country != null && !country.isEmpty()) {
            predicates.add(cb.equal(addrJoin.get("country"), country));
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        return entityManager.createQuery(cq).getResultList();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Advanced JOIN — Generated SQL:                                                  │
│                                                                                  │
│  advancedJoinSearch(null, true, "New York", "USA"):                              │
│    SELECT u1_0.id, u1_0.user_name, u1_0.active, a1_0.city, a1_0.country         │
│    FROM user_details u1_0                                                        │
│    LEFT JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                       │
│    WHERE u1_0.active = true AND a1_0.city = 'New York' AND a1_0.country = 'USA'  │
│                                                                                  │
│    Result:                                                                       │
│      ┌────┬───────────┬────────┬──────────┬─────────┐                            │
│      │ id │ user_name │ active │ city     │ country │                            │
│      ├────┼───────────┼────────┼──────────┼─────────┤                            │
│      │  1 │ Alice     │ true   │ New York │ USA     │                            │
│      │  4 │ Diana     │ true   │ New York │ USA     │                            │
│      └────┴───────────┴────────┴──────────┴─────────┘                            │
│                                                                                  │
│  advancedJoinSearch("Alice", null, null, null):                                  │
│    SELECT u1_0.id, u1_0.user_name, u1_0.active, a1_0.city, a1_0.country         │
│    FROM user_details u1_0                                                        │
│    LEFT JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                       │
│    WHERE u1_0.user_name LIKE '%Alice%'                                           │
│                                                                                  │
│    Result:                                                                       │
│      ┌────┬───────────┬────────┬──────────┬─────────┐                            │
│      │ id │ user_name │ active │ city     │ country │                            │
│      ├────┼───────────┼────────┼──────────┼─────────┤                            │
│      │  1 │ Alice     │ true   │ New York │ USA     │ ← address id=10            │
│      │  1 │ Alice     │ true   │ Mumbai   │ India   │ ← address id=12            │
│      └────┴───────────┴────────┴──────────┴─────────┘                            │
│    Alice appears twice (she has 2 addresses)                                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 3: JOIN with GROUP BY and HAVING — count addresses per user:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Count addresses per user, optionally filter by minimum count.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUsersWithAddressCount(Integer minAddresses) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<UserDetails> userRoot = cq.from(UserDetails.class);
        Join<UserDetails, UserAddress> addrJoin = userRoot.join("addresses", JoinType.LEFT);

        cq.multiselect(
            userRoot.get("name"),
            cb.count(addrJoin.get("id"))      // COUNT(user_addresses.id)
        );

        cq.groupBy(userRoot.get("id"), userRoot.get("name"));

        if (minAddresses != null && minAddresses > 0) {
            cq.having(cb.ge(cb.count(addrJoin.get("id")), minAddresses));
            // HAVING COUNT(address.id) >= minAddresses
        }

        cq.orderBy(cb.desc(cb.count(addrJoin.get("id"))));

        return entityManager.createQuery(cq).getResultList();
    }
    // Generated SQL (minAddresses = 2):
    //   SELECT u1_0.user_name, COUNT(a1_0.id)
    //   FROM user_details u1_0
    //   LEFT JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id
    //   GROUP BY u1_0.id, u1_0.user_name
    //   HAVING COUNT(a1_0.id) >= 2
    //   ORDER BY COUNT(a1_0.id) DESC
    //
    // Result:
    //   Object[] { "Alice", 2 }   — Alice has 2 addresses (New York + Mumbai)
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JOIN Variations — Criteria API:                                                 │
│                                                                                  │
│  INNER JOIN (default — only matching rows):                                      │
│    root.join("addresses")                                                        │
│    root.join("addresses", JoinType.INNER)                                        │
│    SQL: ... JOIN user_addresses a ON u.id = a.user_id                            │
│    → Users WITHOUT addresses are EXCLUDED                                        │
│                                                                                  │
│  LEFT JOIN (all parent rows, null for missing child):                            │
│    root.join("addresses", JoinType.LEFT)                                         │
│    SQL: ... LEFT JOIN user_addresses a ON u.id = a.user_id                       │
│    → Users WITHOUT addresses are INCLUDED (address columns = null)               │
│                                                                                  │
│  FETCH JOIN (load relationship eagerly to avoid N+1):                            │
│    root.fetch("addresses")                                                       │
│    root.fetch("addresses", JoinType.LEFT)                                        │
│    SQL: ... LEFT JOIN user_addresses a ON u.id = a.user_id                       │
│    → Same SQL as LEFT JOIN                                                       │
│    → PLUS: loaded addresses are populated into entity's addresses field          │
│    → Used with entity results, NOT with multiselect/Object[]                     │
│                                                                                  │
│  ┌────────────────────────┬──────────────────────────────────────────────────────┐│
│  │ Method                 │ Use when                                             ││
│  ├────────────────────────┼──────────────────────────────────────────────────────┤│
│  │ root.join()            │ Filtering/selecting by child fields + multiselect   ││
│  │ root.fetch()           │ Loading full entities with relationships (avoid N+1)││
│  └────────────────────────┴──────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Pagination and Sorting with Criteria API

Pagination uses `TypedQuery.setFirstResult(offset)` and `TypedQuery.setMaxResults(limit)` — same methods as with native queries. Sorting uses `CriteriaQuery.orderBy()` with `CriteriaBuilder.asc()` or `CriteriaBuilder.desc()`.

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Full-featured dynamic search with Criteria API:
     * - Optional filters
     * - Pagination (page + size)
     * - Dynamic sorting (sortBy field name + direction)
     * Returns Page<UserDetails>
     */
    @Transactional(readOnly = true)
    public Page<UserDetails> searchUsersPaginated(String name, Boolean active,
                                                   int page, int size,
                                                   String sortBy, String sortDir) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // === DATA QUERY ===
        CriteriaQuery<UserDetails> dataCq = cb.createQuery(UserDetails.class);
        Root<UserDetails> dataRoot = dataCq.from(UserDetails.class);

        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        if (name != null && !name.isEmpty()) {
            predicates.add(cb.like(dataRoot.get("name"), "%" + name + "%"));
        }
        if (active != null) {
            predicates.add(cb.equal(dataRoot.get("active"), active));
        }

        dataCq.select(dataRoot);
        if (!predicates.isEmpty()) {
            dataCq.where(predicates.toArray(new Predicate[0]));
        }

        // Sorting — dynamic ORDER BY using Java field names
        List<String> allowedSortFields = List.of("id", "name", "email", "active");
        if (sortBy != null && allowedSortFields.contains(sortBy)) {
            if ("DESC".equalsIgnoreCase(sortDir)) {
                dataCq.orderBy(cb.desc(dataRoot.get(sortBy)));
            } else {
                dataCq.orderBy(cb.asc(dataRoot.get(sortBy)));
            }
        } else {
            dataCq.orderBy(cb.asc(dataRoot.get("id")));  // default sort
        }

        // Apply pagination
        TypedQuery<UserDetails> dataQuery = entityManager.createQuery(dataCq);
        dataQuery.setFirstResult(page * size);   // OFFSET
        dataQuery.setMaxResults(size);            // LIMIT

        List<UserDetails> content = dataQuery.getResultList();

        // === COUNT QUERY ===
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<UserDetails> countRoot = countCq.from(UserDetails.class);

        countCq.select(cb.count(countRoot));

        // Rebuild predicates for count query (same conditions, different Root)
        List<Predicate> countPredicates = new ArrayList<>();
        if (name != null && !name.isEmpty()) {
            countPredicates.add(cb.like(countRoot.get("name"), "%" + name + "%"));
        }
        if (active != null) {
            countPredicates.add(cb.equal(countRoot.get("active"), active));
        }
        if (!countPredicates.isEmpty()) {
            countCq.where(countPredicates.toArray(new Predicate[0]));
        }

        Long totalElements = entityManager.createQuery(countCq).getSingleResult();

        // Build Page object
        Pageable pageable = PageRequest.of(page, size, Sort.by(
            "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy != null && allowedSortFields.contains(sortBy) ? sortBy : "id"
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

    // GET /api/users/search?active=true&page=0&size=3&sortBy=name&sortDir=DESC
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
│  Criteria API Pagination + Sorting — Scenario:                                   │
│                                                                                  │
│  Request: GET /api/users/search?active=true&page=1&size=2&sortBy=name&sortDir=ASC│
│                                                                                  │
│  Data query generated SQL:                                                       │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.active = true                                                      │
│    ORDER BY u1_0.user_name ASC                                                   │
│    LIMIT 2 OFFSET 2                                                              │
│         ↑ size   ↑ page(1) × size(2)                                             │
│                                                                                  │
│  Count query generated SQL:                                                      │
│    SELECT COUNT(u1_0.id)                                                         │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.active = true                                                      │
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
│                                                                                  │
│  KEY DIFFERENCE from Native Query pagination:                                    │
│  - sortBy = "name" (JAVA FIELD name, not DB column "user_name")                 │
│  - Hibernate translates "name" → "user_name" via @Column annotation              │
│  - No whitelist needed for SQL injection (no string concatenation in SQL)        │
│  - But whitelist is still good practice to reject unexpected field names          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Multi-column sorting:**

```java
// Sort by active DESC, then by name ASC
dataCq.orderBy(
    cb.desc(dataRoot.get("active")),    // active users first
    cb.asc(dataRoot.get("name"))        // then alphabetical by name
);

// Generated SQL:
//   ... ORDER BY u1_0.active DESC, u1_0.user_name ASC
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API Sorting vs Other Approaches:                                       │
│                                                                                  │
│  ┌────────────────────────────────────────┬──────────────────────────────────────┐│
│  │ Approach                               │ Sorting Mechanism                   ││
│  ├────────────────────────────────────────┼──────────────────────────────────────┤│
│  │ Derived Query                          │ findByActiveOrderByNameAsc()        ││
│  │                                        │ → Fixed in method name              ││
│  │ JPQL                                   │ "... ORDER BY u.name ASC"           ││
│  │                                        │ → String (Java field names)         ││
│  │ Native Query                           │ "... ORDER BY user_name ASC"        ││
│  │                                        │ → String (DB column names)          ││
│  │ Dynamic Native (EntityManager)         │ sql.append(" ORDER BY " + col)      ││
│  │                                        │ → String concat (injection risk!)   ││
│  │ Criteria API                           │ cb.asc(root.get("name"))            ││
│  │                                        │ → Method call (type-safe, no SQL)   ││
│  └────────────────────────────────────────┴──────────────────────────────────────┘│
│                                                                                  │
│  Criteria API sorting is the safest:                                             │
│    - No SQL string concatenation → no SQL injection possible                    │
│    - Uses Java field names → Hibernate resolves to DB column names               │
│    - Compile-time safe with Metamodel: cb.asc(root.get(UserDetails_.name))       │
│    - Dynamic: can add/remove orderBy at runtime based on parameters              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Summary — Criteria API

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API — Complete Pattern:                                                │
│                                                                                  │
│  @PersistenceContext                                                             │
│  private EntityManager entityManager;                                            │
│                                                                                  │
│  // 1. Get builder                                                               │
│  CriteriaBuilder cb = entityManager.getCriteriaBuilder();                        │
│                                                                                  │
│  // 2. Create query                                                              │
│  CriteriaQuery<Entity> cq = cb.createQuery(Entity.class);     // entity result   │
│  CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class); // partial cols    │
│  CriteriaQuery<Long> cq = cb.createQuery(Long.class);         // count           │
│  CriteriaQuery<DTO> cq = cb.createQuery(DTO.class);           // DTO (construct) │
│                                                                                  │
│  // 3. Define FROM                                                               │
│  Root<Entity> root = cq.from(Entity.class);                                      │
│                                                                                  │
│  // 4. Optional JOIN                                                             │
│  Join<Parent, Child> join = root.join("fieldName");                               │
│  Join<Parent, Child> join = root.join("fieldName", JoinType.LEFT);               │
│                                                                                  │
│  // 5. SELECT                                                                    │
│  cq.select(root);                                  // full entity                │
│  cq.multiselect(root.get("a"), root.get("b"));     // partial → Object[]        │
│  cq.select(cb.construct(DTO.class, ...));           // direct DTO                │
│  cq.select(cb.count(root));                         // aggregate                 │
│                                                                                  │
│  // 6. WHERE (dynamic predicates)                                                │
│  List<Predicate> preds = new ArrayList<>();                                      │
│  preds.add(cb.equal(...));  preds.add(cb.like(...));                             │
│  cq.where(preds.toArray(new Predicate[0]));                                      │
│                                                                                  │
│  // 7. ORDER BY                                                                  │
│  cq.orderBy(cb.asc(root.get("field")));                                          │
│                                                                                  │
│  // 8. GROUP BY + HAVING                                                         │
│  cq.groupBy(root.get("field"));                                                  │
│  cq.having(cb.ge(cb.count(root), 5));                                            │
│                                                                                  │
│  // 9. Execute with pagination                                                   │
│  TypedQuery<T> query = entityManager.createQuery(cq);                            │
│  query.setFirstResult(page * size);                                              │
│  query.setMaxResults(size);                                                      │
│  List<T> results = query.getResultList();                                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Complete Decision Table — All Query Approaches:

  ┌─────────────────────────────────────┬──────────────────────────────────────────┐
  │ Scenario                            │ Best Approach                            │
  ├─────────────────────────────────────┼──────────────────────────────────────────┤
  │ Simple findBy with 1-2 conditions   │ Derived Query (method name)              │
  │ Fixed complex query on entities     │ JPQL @Query on Repository                │
  │ Fixed complex query on tables       │ Native @Query on Repository              │
  │ DB-specific features (JSONB, etc.)  │ Native @Query on Repository              │
  │ Dynamic query, DB-locked            │ Dynamic Native Query (EntityManager)     │
  │ Dynamic query, DB-independent       │ Criteria API                             │
  │ Dynamic query, type-safe            │ Criteria API + Metamodel                 │
  │ Dynamic JOINs (related entities)    │ Criteria API                             │
  │ Dynamic JOINs (unrelated tables)    │ Dynamic Native Query (EntityManager)     │
  │ Dynamic GROUP BY / HAVING           │ Criteria API or Dynamic Native Query     │
  │ Dynamic UPDATE (partial fields)     │ Dynamic Native Query (EntityManager)     │
  │ Maximum readability                 │ JPQL or Native Query (SQL is familiar)   │
  │ Maximum type safety                 │ Criteria API + Metamodel                 │
  │ Maximum DB portability              │ Criteria API or JPQL                     │
  └─────────────────────────────────────┴──────────────────────────────────────────┘
```

---


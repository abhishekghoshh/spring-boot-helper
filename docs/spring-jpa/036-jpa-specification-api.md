### JPA Specification API

#### What Is the Specification API?

The **Specification API** is a Spring Data JPA abstraction built **on top of** the Criteria API. It solves two major problems with raw Criteria API usage:

1. **Code Duplicity** — the same predicate logic (e.g., "filter by active = true") gets copy-pasted across multiple service methods
2. **Boilerplate** — every Criteria query needs the same `CriteriaBuilder` → `CriteriaQuery` → `Root` → `TypedQuery` setup code

The Specification API encapsulates each WHERE condition as a **reusable, composable** `Specification` object. Spring Data's `JpaSpecificationExecutor` then handles all the CriteriaBuilder/CriteriaQuery/TypedQuery boilerplate for you.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Evolution — from Criteria API to Specification API:                             │
│                                                                                  │
│  Criteria API (manual):                                                          │
│    @Service                                                                      │
│    public class UserService {                                                    │
│        @PersistenceContext                                                        │
│        private EntityManager entityManager;                                      │
│                                                                                  │
│        public List<UserDetails> search(String name, Boolean active) {            │
│            CriteriaBuilder cb = entityManager.getCriteriaBuilder();       // ┐   │
│            CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);//│  │
│            Root<UserDetails> root = cq.from(UserDetails.class);           // │   │
│            List<Predicate> preds = new ArrayList<>();                     // │   │
│            if (name != null) preds.add(cb.like(root.get("name"), ...));   // │   │
│            if (active != null) preds.add(cb.equal(root.get("active"),    // ├ B  │
│                                                    active));             // │ O  │
│            cq.select(root).where(preds.toArray(new Predicate[0]));       // │ I  │
│            TypedQuery<UserDetails> query = entityManager.createQuery(cq); // │ L  │
│            return query.getResultList();                                  // │ E  │
│        }                                                                  // │ R  │
│        // EVERY method repeats this boilerplate!                          // ┘ P  │
│    }                                                                             │
│                                                                                  │
│  Specification API (Spring Data):                                                │
│    // Repository — just extend JpaSpecificationExecutor                          │
│    public interface UserDetailsRepository extends JpaRepository<UserDetails, Long>│
│                                                , JpaSpecificationExecutor<        │
│                                                    UserDetails> { }              │
│                                                                                  │
│    // Specification — reusable predicate                                         │
│    public class UserSpecs {                                                      │
│        public static Specification<UserDetails> hasName(String name) {           │
│            return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%"│
│            );                                                                    │
│        }                                                                         │
│        public static Specification<UserDetails> isActive() {                     │
│            return (root, query, cb) -> cb.equal(root.get("active"), true);       │
│        }                                                                         │
│    }                                                                             │
│                                                                                  │
│    // Service — NO boilerplate!                                                  │
│    @Service                                                                      │
│    public class UserService {                                                    │
│        @Autowired                                                                │
│        private UserDetailsRepository repository;                                 │
│                                                                                  │
│        public List<UserDetails> search(String name, Boolean active) {            │
│            Specification<UserDetails> spec = Specification.where(null);           │
│            if (name != null) spec = spec.and(UserSpecs.hasName(name));            │
│            if (active != null) spec = spec.and(UserSpecs.isActive());            │
│            return repository.findAll(spec);                                      │
│        }                                                                         │
│    }                                                                             │
│                                                                                  │
│  RESULT:                                                                         │
│    - No EntityManager, CriteriaBuilder, CriteriaQuery, Root, TypedQuery          │
│    - Each predicate defined ONCE, reused everywhere                              │
│    - Predicates composed with .and(), .or(), .not()                              │
│    - Spring handles all the Criteria API plumbing                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Database Tables (same as Criteria API section):

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
```

---

#### Problem 1: Code Duplicity in Criteria API — Solved by Specification.toPredicate

**The Problem:** With raw Criteria API, the same predicate logic is duplicated across multiple service methods. If the "active = true" condition appears in 10 different search methods, you write `cb.equal(root.get("active"), true)` in all 10 places. When the business rule changes (e.g., "active" is renamed to "enabled"), you must update all 10 methods.

```java
// PROBLEM — Criteria API code duplicity:
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    // Method 1: search by name + active
    public List<UserDetails> searchByNameAndActive(String name, Boolean active) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);
        List<Predicate> preds = new ArrayList<>();
        if (name != null) {
            preds.add(cb.like(root.get("name"), "%" + name + "%"));  // ← DUPLICATED
        }
        if (active != null) {
            preds.add(cb.equal(root.get("active"), active));         // ← DUPLICATED
        }
        cq.select(root).where(preds.toArray(new Predicate[0]));
        return entityManager.createQuery(cq).getResultList();
    }

    // Method 2: search by email + active — SAME active predicate!
    public List<UserDetails> searchByEmailAndActive(String email, Boolean active) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);
        List<Predicate> preds = new ArrayList<>();
        if (email != null) {
            preds.add(cb.equal(root.get("email"), email));
        }
        if (active != null) {
            preds.add(cb.equal(root.get("active"), active));         // ← SAME CODE AGAIN
        }
        cq.select(root).where(preds.toArray(new Predicate[0]));
        return entityManager.createQuery(cq).getResultList();
    }

    // Method 3: count active users — SAME active predicate AGAIN!
    public long countActiveUsers() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<UserDetails> root = cq.from(UserDetails.class);
        cq.select(cb.count(root));
        cq.where(cb.equal(root.get("active"), true));               // ← SAME CODE AGAIN
        return entityManager.createQuery(cq).getSingleResult();
    }

    // The "active" predicate is written 3 times.
    // If "active" field is renamed to "enabled" → must update ALL 3 methods.
    // If the active check changes (e.g., check active AND NOT deleted) → update ALL.
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Code Duplicity Problem — Visual:                                                │
│                                                                                  │
│  searchByNameAndActive():                                                        │
│    cb.like(root.get("name"), ...)              ← unique to this method           │
│    cb.equal(root.get("active"), active)        ← DUPLICATED                      │
│                                                                                  │
│  searchByEmailAndActive():                                                       │
│    cb.equal(root.get("email"), email)          ← unique to this method           │
│    cb.equal(root.get("active"), active)        ← SAME predicate, copied again    │
│                                                                                  │
│  countActiveUsers():                                                             │
│    cb.equal(root.get("active"), true)          ← SAME predicate, copied AGAIN    │
│                                                                                  │
│  searchByNameInCity():                                                           │
│    cb.like(root.get("name"), ...)              ← copied from method 1            │
│    cb.equal(root.get("active"), active)        ← copied AGAIN (4th time)         │
│    cb.equal(addrJoin.get("city"), city)        ← unique to this method           │
│                                                                                  │
│  As the application grows:                                                       │
│    10 methods × same "active" check = 10 copies of the same code                │
│    Bug in one? Must fix all 10.                                                  │
│    Rename field? Must update all 10.                                             │
│    DRY principle violated!                                                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**The Solution: `Specification<T>` — Encapsulate Each Predicate:**

The `Specification<T>` interface has a single method:

```java
// The Specification interface (from Spring Data JPA):
@FunctionalInterface
public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);
}
```

Each `Specification` wraps ONE predicate (WHERE condition). You define it once and reuse it everywhere.

```java
// SOLUTION — Define each predicate as a reusable Specification:
public class UserSpecs {

    // Specification for: WHERE user_name LIKE '%name%'
    public static Specification<UserDetails> hasName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
        //      ↑ Root   ↑ CriteriaQuery  ↑ CriteriaBuilder
        //      These are provided by Spring when the Specification is executed.
        //      You just define WHAT predicate to create.
    }

    // Specification for: WHERE email = :email
    public static Specification<UserDetails> hasEmail(String email) {
        return (root, query, cb) -> cb.equal(root.get("email"), email);
    }

    // Specification for: WHERE active = :active
    public static Specification<UserDetails> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    // Specification for: WHERE active = true (no parameter)
    public static Specification<UserDetails> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }

    // Specification for: WHERE id IN (:ids)
    public static Specification<UserDetails> hasIdIn(List<Long> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }

    // Specification for: WHERE id BETWEEN :min AND :max
    public static Specification<UserDetails> hasIdBetween(Long min, Long max) {
        return (root, query, cb) -> cb.between(root.get("id"), min, max);
    }

    // Each predicate is defined ONCE. Reused across ALL service methods.
    // If "active" is renamed to "enabled" → change ONE place (isActive method).
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification.toPredicate — How it works:                                       │
│                                                                                  │
│  Specification<UserDetails> spec = UserSpecs.hasName("Alice");                   │
│                                                                                  │
│  What hasName("Alice") returns:                                                  │
│    A lambda: (root, query, cb) -> cb.like(root.get("name"), "%Alice%")           │
│                                                                                  │
│  This lambda is NOT executed immediately.                                        │
│  It's a "recipe" for creating a Predicate.                                       │
│                                                                                  │
│  When Spring executes the Specification:                                         │
│    1. Spring creates CriteriaBuilder, CriteriaQuery, Root internally             │
│    2. Spring calls spec.toPredicate(root, query, cb)                             │
│    3. The lambda executes: cb.like(root.get("name"), "%Alice%")                  │
│    4. Returns a Predicate                                                        │
│    5. Spring adds the Predicate to the CriteriaQuery's WHERE clause              │
│    6. Spring creates TypedQuery, executes, returns results                        │
│                                                                                  │
│  You provide the WHAT (predicate logic).                                         │
│  Spring handles the HOW (CriteriaBuilder, CriteriaQuery, execution).            │
│                                                                                  │
│  Defined ONCE:                               Reused EVERYWHERE:                  │
│  ┌─────────────────────────┐                 ┌─────────────────────────────────┐ │
│  │ UserSpecs.hasName(name) │ ────────────→   │ searchByNameAndActive()         │ │
│  │ UserSpecs.isActive()    │ ──┬──────────→  │ searchByEmailAndActive()        │ │
│  │ UserSpecs.hasEmail(e)   │   ├──────────→  │ countActiveUsers()              │ │
│  └─────────────────────────┘   └──────────→  │ searchByNameInCity()            │ │
│                                              └─────────────────────────────────┘ │
│                                                                                  │
│  Change "active" → "enabled"? Fix ONLY isActive(). All methods auto-updated.    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Problem 2: Boilerplate in Criteria API — Solved by JpaSpecificationExecutor

**The Problem:** Even after extracting predicates into `Specification` objects, you still need to write the `CriteriaBuilder` → `CriteriaQuery` → `Root` → `TypedQuery` boilerplate in every service method to execute them.

**The Solution:** `JpaSpecificationExecutor<T>` is a Spring Data interface that provides pre-built methods for executing Specifications. You just extend it on your repository — no `EntityManager` needed.

```java
// Repository — extend JpaSpecificationExecutor
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long>,
                                               JpaSpecificationExecutor<UserDetails> {
    // That's it! No method declarations needed.
    // JpaSpecificationExecutor provides:
    //   List<T> findAll(Specification<T> spec)
    //   Page<T> findAll(Specification<T> spec, Pageable pageable)
    //   List<T> findAll(Specification<T> spec, Sort sort)
    //   Optional<T> findOne(Specification<T> spec)
    //   long count(Specification<T> spec)
    //   boolean exists(Specification<T> spec)
    //   void delete(Specification<T> spec)
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JpaSpecificationExecutor — Methods provided:                                    │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────┬─────────────────────────┐│
│  │ Method                                              │ SQL Equivalent          ││
│  ├─────────────────────────────────────────────────────┼─────────────────────────┤│
│  │ findAll(Specification<T> spec)                      │ SELECT * WHERE ...      ││
│  │ findAll(Specification<T> spec, Sort sort)           │ SELECT * WHERE ...      ││
│  │                                                     │   ORDER BY ...          ││
│  │ findAll(Specification<T> spec, Pageable pageable)   │ SELECT * WHERE ...      ││
│  │                                                     │   ORDER BY ... LIMIT .. ││
│  │ findOne(Specification<T> spec)                      │ SELECT * WHERE ... (1)  ││
│  │ count(Specification<T> spec)                        │ SELECT COUNT(*) WHERE ..││
│  │ exists(Specification<T> spec)                       │ SELECT EXISTS(...)      ││
│  │ delete(Specification<T> spec)                       │ DELETE WHERE ...        ││
│  └─────────────────────────────────────────────────────┴─────────────────────────┘│
│                                                                                  │
│  ALL of these methods internally:                                                │
│    1. Get CriteriaBuilder from EntityManager                                     │
│    2. Create CriteriaQuery                                                       │
│    3. Create Root                                                                │
│    4. Call spec.toPredicate(root, query, cb) to get your Predicate               │
│    5. Apply the Predicate to WHERE clause                                        │
│    6. Create TypedQuery and execute                                              │
│    7. Return results                                                             │
│                                                                                  │
│  You NEVER write this boilerplate yourself!                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// BEFORE — Criteria API (with boilerplate):
@Service
public class UserServiceBefore {

    @PersistenceContext
    private EntityManager entityManager;   // ← need EntityManager

    public List<UserDetails> searchByNameAndActive(String name, Boolean active) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();              // boilerplate
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);   // boilerplate
        Root<UserDetails> root = cq.from(UserDetails.class);                 // boilerplate
        List<Predicate> preds = new ArrayList<>();                           // boilerplate
        if (name != null) preds.add(cb.like(root.get("name"), "%" + name + "%"));
        if (active != null) preds.add(cb.equal(root.get("active"), active));
        cq.select(root).where(preds.toArray(new Predicate[0]));             // boilerplate
        TypedQuery<UserDetails> query = entityManager.createQuery(cq);       // boilerplate
        return query.getResultList();                                        // boilerplate
    }
    // 9 lines of boilerplate for every method!
}
```

```java
// AFTER — Specification API (no boilerplate):
@Service
public class UserServiceAfter {

    @Autowired
    private UserDetailsRepository repository;  // ← just the repository

    public List<UserDetails> searchByNameAndActive(String name, Boolean active) {
        Specification<UserDetails> spec = Specification.where(null);  // start with no condition

        if (name != null) {
            spec = spec.and(UserSpecs.hasName(name));
        }
        if (active != null) {
            spec = spec.and(UserSpecs.isActive(active));
        }

        return repository.findAll(spec);  // Spring handles ALL the Criteria API plumbing
    }
    // ZERO boilerplate! Just compose Specifications and call findAll().
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Boilerplate Comparison:                                                         │
│                                                                                  │
│  Criteria API (per method):                    Specification API (per method):    │
│  ─────────────────────────                     ──────────────────────────────     │
│  CriteriaBuilder cb = ...;                     Specification<T> spec = where(null│
│  CriteriaQuery<T> cq = ...;                    );                                │
│  Root<T> root = ...;                           spec = spec.and(hasName(name));    │
│  List<Predicate> preds = ...;                  spec = spec.and(isActive(active)); │
│  if (...) preds.add(cb.like(...));             return repo.findAll(spec);         │
│  if (...) preds.add(cb.equal(...));                                              │
│  cq.select(root).where(...);                   TOTAL: 4 lines                    │
│  TypedQuery<T> query = ...;                                                      │
│  return query.getResultList();                                                   │
│  TOTAL: 9 lines                                                                  │
│                                                                                  │
│  With 10 search methods:                                                         │
│    Criteria API: 10 × 9 = 90 lines of boilerplate                               │
│    Specification: 10 × 4 = 40 lines total + ~20 lines for Specification defs     │
│    = 60 lines total (33% less) + predicates are REUSABLE                         │
│                                                                                  │
│  The real win is reusability:                                                    │
│    If 10 methods use "isActive" → defined ONCE, not 10 times                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Flow — Specification API execution:                                             │
│                                                                                  │
│  Controller: GET /api/users/search?name=Alice&active=true                        │
│       │                                                                          │
│       v                                                                          │
│  Service: searchByNameAndActive("Alice", true)                                   │
│       │                                                                          │
│       ├─ Specification.where(null)          → empty spec (no condition)           │
│       ├─ spec.and(UserSpecs.hasName("Alice"))  → adds name LIKE '%Alice%'        │
│       ├─ spec.and(UserSpecs.isActive(true))    → adds active = true              │
│       │                                                                          │
│       v                                                                          │
│  repository.findAll(spec)                                                        │
│       │                                                                          │
│       v                                                                          │
│  Spring Data JPA internally:                                                     │
│    CriteriaBuilder cb = entityManager.getCriteriaBuilder();                      │
│    CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);            │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    Predicate p = spec.toPredicate(root, cq, cb);                                │
│    //  → cb.and(                                                                 │
│    //      cb.like(root.get("name"), "%Alice%"),                                 │
│    //      cb.equal(root.get("active"), true)                                    │
│    //    )                                                                       │
│    cq.where(p);                                                                  │
│    TypedQuery<UserDetails> query = entityManager.createQuery(cq);                │
│    return query.getResultList();                                                 │
│       │                                                                          │
│       v                                                                          │
│  Generated SQL:                                                                  │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.user_name LIKE '%Alice%' AND u1_0.active = true                    │
│       │                                                                          │
│       v                                                                          │
│  Returns List<UserDetails> → Controller → JSON response                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Complete Setup — Specifications + Repository + Service + Controller

**Step 1: Specification class (define reusable predicates):**

```java
// UserSpecs.java — all specifications for UserDetails entity
public class UserSpecs {

    public static Specification<UserDetails> hasName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<UserDetails> hasEmail(String email) {
        return (root, query, cb) -> cb.equal(root.get("email"), email);
    }

    public static Specification<UserDetails> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<UserDetails> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }

    public static Specification<UserDetails> hasIdIn(List<Long> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }

    public static Specification<UserDetails> emailContains(String keyword) {
        return (root, query, cb) -> cb.like(root.get("email"), "%" + keyword + "%");
    }

    public static Specification<UserDetails> hasIdGreaterThan(Long id) {
        return (root, query, cb) -> cb.greaterThan(root.get("id"), id);
    }
}
```

**Step 2: Repository (extend JpaSpecificationExecutor):**

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long>,
                                               JpaSpecificationExecutor<UserDetails> {
    // No custom methods needed for Specification-based queries.
    // All findAll(spec), findAll(spec, pageable), count(spec), etc. are inherited.
}
```

**Step 3: Service (compose Specifications dynamically):**

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Dynamic search — any combination of filters.
     */
    public List<UserDetails> searchUsers(String name, String email, Boolean active) {

        Specification<UserDetails> spec = Specification.where(null);
        //  Specification.where(null) starts with NO condition (returns all rows).
        //  Equivalent to "WHERE 1=1" in dynamic native queries.

        if (name != null && !name.isEmpty()) {
            spec = spec.and(UserSpecs.hasName(name));
        }
        if (email != null && !email.isEmpty()) {
            spec = spec.and(UserSpecs.hasEmail(email));
        }
        if (active != null) {
            spec = spec.and(UserSpecs.isActive(active));
        }

        return repository.findAll(spec);
    }

    /**
     * Count active users — reuses the same isActive() Specification
     */
    public long countActiveUsers() {
        return repository.count(UserSpecs.isActive());
        //  Spring generates: SELECT COUNT(*) FROM user_details WHERE active = true
    }

    /**
     * Check if user exists — reuses hasEmail() Specification
     */
    public boolean existsByEmail(String email) {
        return repository.exists(UserSpecs.hasEmail(email));
        //  Spring generates: SELECT 1 FROM user_details WHERE email = 'alice@ex.com' LIMIT 1
    }
}
```

**Step 4: Controller:**

```java
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

    @GetMapping("/count-active")
    public long countActive() {
        return userService.countActiveUsers();
    }

    @GetMapping("/exists")
    public boolean exists(@RequestParam String email) {
        return userService.existsByEmail(email);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification API — Generated SQL for each scenario:                            │
│                                                                                  │
│  Scenario 1: searchUsers("Alice", null, null)                                    │
│    spec = where(null).and(hasName("Alice"))                                      │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Alice%'                                         │
│    Result: [UserDetails(id=1, name="Alice", ...)]                                │
│                                                                                  │
│  Scenario 2: searchUsers(null, null, true)                                       │
│    spec = where(null).and(isActive(true))                                        │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.active = true                                                    │
│    Result: [Alice, Bob, Diana, Frank, Grace] — 5 active users                    │
│                                                                                  │
│  Scenario 3: searchUsers("Bob", "bob@ex.com", true)                              │
│    spec = where(null).and(hasName("Bob")).and(hasEmail("bob@ex.com"))             │
│                       .and(isActive(true))                                       │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Bob%'                                           │
│        AND u1_0.email = 'bob@ex.com'                                             │
│        AND u1_0.active = true                                                    │
│    Result: [UserDetails(id=2, name="Bob", ...)]                                  │
│                                                                                  │
│  Scenario 4: searchUsers(null, null, null)                                       │
│    spec = where(null) — no .and() called                                         │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│    Result: All 8 users (no WHERE clause)                                         │
│                                                                                  │
│  Scenario 5: countActiveUsers()                                                  │
│    spec = isActive()                                                             │
│    Generated SQL:                                                                │
│      SELECT COUNT(u1_0.id) FROM user_details u1_0 WHERE u1_0.active = true       │
│    Result: 5                                                                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### AND, OR, NOT, and Complex Predicates with Specification API

Specifications support composition using `.and()`, `.or()`, and `Specification.not()`.

```java
// UserSpecs.java — reusable specifications
public class UserSpecs {

    public static Specification<UserDetails> hasName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<UserDetails> hasEmail(String email) {
        return (root, query, cb) -> cb.equal(root.get("email"), email);
    }

    public static Specification<UserDetails> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<UserDetails> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }

    public static Specification<UserDetails> emailContains(String keyword) {
        return (root, query, cb) -> cb.like(root.get("email"), "%" + keyword + "%");
    }

    public static Specification<UserDetails> hasIdGreaterThan(Long id) {
        return (root, query, cb) -> cb.greaterThan(root.get("id"), id);
    }

    public static Specification<UserDetails> hasIdBetween(Long min, Long max) {
        return (root, query, cb) -> cb.between(root.get("id"), min, max);
    }
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * AND — all conditions must match
     * Find active users whose name contains "a"
     */
    public List<UserDetails> findActiveByName(String name) {
        Specification<UserDetails> spec = Specification
            .where(UserSpecs.hasName(name))
            .and(UserSpecs.isActive());
        return repository.findAll(spec);
        // SQL: WHERE user_name LIKE '%a%' AND active = true
    }

    /**
     * OR — any condition can match
     * Find users where name contains keyword OR email contains keyword
     */
    public List<UserDetails> searchByKeyword(String keyword) {
        Specification<UserDetails> spec = Specification
            .where(UserSpecs.hasName(keyword))
            .or(UserSpecs.emailContains(keyword));
        return repository.findAll(spec);
        // SQL: WHERE user_name LIKE '%keyword%' OR email LIKE '%keyword%'
    }

    /**
     * NOT — negate a specification
     * Find inactive users (NOT active)
     */
    public List<UserDetails> findInactiveUsers() {
        Specification<UserDetails> spec = Specification.not(UserSpecs.isActive());
        return repository.findAll(spec);
        // SQL: WHERE NOT (active = true)
        // Same as: WHERE active = false
    }

    /**
     * Complex: (name LIKE keyword OR email LIKE keyword) AND active = true AND id > 2
     */
    public List<UserDetails> complexSearch(String keyword, Long minId) {

        // Build OR condition: name LIKE keyword OR email LIKE keyword
        Specification<UserDetails> keywordSpec = Specification
            .where(UserSpecs.hasName(keyword))
            .or(UserSpecs.emailContains(keyword));

        // Combine with AND
        Specification<UserDetails> spec = Specification
            .where(keywordSpec)                            // (name LIKE OR email LIKE)
            .and(UserSpecs.isActive())                     // AND active = true
            .and(UserSpecs.hasIdGreaterThan(minId));       // AND id > minId

        return repository.findAll(spec);
    }

    /**
     * Dynamic AND + OR with optional parameters
     */
    public List<UserDetails> dynamicSearch(String name, String email,
                                            Boolean active, String keyword) {

        Specification<UserDetails> spec = Specification.where(null);

        if (name != null) {
            spec = spec.and(UserSpecs.hasName(name));
        }
        if (email != null) {
            spec = spec.and(UserSpecs.hasEmail(email));
        }
        if (active != null) {
            spec = spec.and(UserSpecs.isActive(active));
        }
        if (keyword != null) {
            // OR within the keyword filter, AND with the rest
            Specification<UserDetails> keywordSpec = Specification
                .where(UserSpecs.hasName(keyword))
                .or(UserSpecs.emailContains(keyword));
            spec = spec.and(keywordSpec);
        }

        return repository.findAll(spec);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification Composition — Generated SQL:                                      │
│                                                                                  │
│  AND:                                                                            │
│    where(hasName("a")).and(isActive())                                            │
│    SQL: WHERE user_name LIKE '%a%' AND active = true                             │
│                                                                                  │
│  OR:                                                                             │
│    where(hasName("ali")).or(emailContains("ali"))                                 │
│    SQL: WHERE user_name LIKE '%ali%' OR email LIKE '%ali%'                       │
│                                                                                  │
│  NOT:                                                                            │
│    Specification.not(isActive())                                                 │
│    SQL: WHERE NOT (active = true)                                                │
│                                                                                  │
│  Complex: (OR group) AND active AND id > 2                                       │
│    where(                                                                        │
│      where(hasName("ali")).or(emailContains("ali"))                               │
│    ).and(isActive()).and(hasIdGreaterThan(2L))                                    │
│    SQL: WHERE (user_name LIKE '%ali%' OR email LIKE '%ali%')                     │
│           AND active = true                                                      │
│           AND id > 2                                                             │
│    Result from DB:                                                               │
│      ┌────┬───────────┬──────────────────┬────────┐                              │
│      │ id │ user_name │ email            │ active │                              │
│      ├────┼───────────┼──────────────────┼────────┤                              │
│      │  4 │ Diana     │ diana@ex.com     │ true   │ ← id > 2, active, name has a│
│      │  6 │ Frank     │ frank@ex.com     │ true   │ ← id > 2, active, name has a│
│      │  7 │ Grace     │ grace@ex.com     │ true   │ ← id > 2, active, name has a│
│      └────┴───────────┴──────────────────┴────────┘                              │
│    (Alice matches "ali" but id=1, not > 2)                                       │
│                                                                                  │
│  Dynamic: name="Bob" + keyword="bob"                                             │
│    spec = where(null)                                                            │
│           .and(hasName("Bob"))                                                   │
│           .and( where(hasName("bob")).or(emailContains("bob")) )                  │
│    SQL: WHERE user_name LIKE '%Bob%'                                             │
│           AND (user_name LIKE '%bob%' OR email LIKE '%bob%')                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification Composition Methods:                                              │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ Method                                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ Specification.where(spec)                │ Start a chain (initial condition) ││
│  │ Specification.where(null)                │ No initial condition (1=1)        ││
│  │ spec.and(otherSpec)                      │ cond1 AND cond2                   ││
│  │ spec.or(otherSpec)                       │ cond1 OR cond2                    ││
│  │ Specification.not(spec)                  │ NOT (cond)                        ││
│  │ spec.and(spec2.or(spec3))               │ cond1 AND (cond2 OR cond3)        ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  Note on .where(null):                                                           │
│    Specification.where(null) returns a Specification that produces NO predicate.  │
│    When Spring sees a null Specification, it skips the WHERE clause entirely.    │
│    This is how you handle the "no filters" case cleanly.                         │
│                                                                                  │
│    spec.and(null) is also safe — the null Specification is ignored.              │
│    This means you can write:                                                     │
│      spec = spec.and(name != null ? UserSpecs.hasName(name) : null);             │
│    → If name is null, the .and(null) is a no-op.                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Cleaner dynamic search using ternary with null:**

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Clean dynamic search using ternary null pattern
     */
    public List<UserDetails> searchUsers(String name, String email, Boolean active) {

        Specification<UserDetails> spec = Specification
            .where(name != null ? UserSpecs.hasName(name) : null)
            .and(email != null ? UserSpecs.hasEmail(email) : null)
            .and(active != null ? UserSpecs.isActive(active) : null);
        //  null Specifications are ignored in .and() and .where()!

        return repository.findAll(spec);
    }
    // If name="Alice", email=null, active=true:
    //   where(hasName("Alice"))       ← name is non-null
    //   .and(null)                    ← email is null → ignored
    //   .and(isActive(true))          ← active is non-null
    //   SQL: WHERE user_name LIKE '%Alice%' AND active = true
}
```

---

#### JOIN and Multiselect with Specification API

The Specification API's `toPredicate` method receives `Root`, `CriteriaQuery`, and `CriteriaBuilder` — the same objects used in the Criteria API. So you can perform JOINs inside a Specification. However, `JpaSpecificationExecutor.findAll()` always returns entities (not `Object[]`), so multiselect requires using EntityManager directly with the Specifications providing predicates.

**JOIN inside a Specification — filtering by child entity fields:**

```java
// UserSpecs.java — JOIN specifications
public class UserSpecs {

    // Existing specs...
    public static Specification<UserDetails> hasName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<UserDetails> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }

    // JOIN spec — filter by address city
    public static Specification<UserDetails> hasCity(String city) {
        return (root, query, cb) -> {
            // JOIN user_addresses ON user_details.id = user_addresses.user_id
            Join<UserDetails, UserAddress> addressJoin = root.join("addresses");
            //                                                ↑ Java field name on UserDetails

            // Prevent duplicate results from JOIN (one user may have multiple addresses)
            query.distinct(true);

            return cb.equal(addressJoin.get("city"), city);
            // SQL: ... JOIN user_addresses a ON u.id = a.user_id WHERE a.city = :city
        };
    }

    // JOIN spec — filter by address country
    public static Specification<UserDetails> hasCountry(String country) {
        return (root, query, cb) -> {
            Join<UserDetails, UserAddress> addressJoin = root.join("addresses");
            query.distinct(true);
            return cb.equal(addressJoin.get("country"), country);
        };
    }

    // LEFT JOIN spec — include users without addresses
    public static Specification<UserDetails> hasCityOptional(String city) {
        return (root, query, cb) -> {
            Join<UserDetails, UserAddress> addressJoin = root.join("addresses", JoinType.LEFT);
            query.distinct(true);
            return cb.equal(addressJoin.get("city"), city);
        };
    }
}
```

```java
// Service — composing JOIN specifications with other specifications
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Find active users in a specific city
     */
    public List<UserDetails> findActiveUsersInCity(String city) {
        Specification<UserDetails> spec = Specification
            .where(UserSpecs.isActive())
            .and(UserSpecs.hasCity(city));
        return repository.findAll(spec);
    }

    /**
     * Dynamic search with optional city filter
     */
    public List<UserDetails> search(String name, Boolean active, String city, String country) {
        Specification<UserDetails> spec = Specification
            .where(name != null ? UserSpecs.hasName(name) : null)
            .and(active != null ? UserSpecs.isActive() : null)
            .and(city != null ? UserSpecs.hasCity(city) : null)
            .and(country != null ? UserSpecs.hasCountry(country) : null);
        return repository.findAll(spec);
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

    // GET /api/users/search?name=Alice&city=New York
    // GET /api/users/search?active=true&country=USA
    @GetMapping("/search")
    public List<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String country
    ) {
        return userService.search(name, active, city, country);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification JOIN — Generated SQL:                                             │
│                                                                                  │
│  findActiveUsersInCity("New York"):                                              │
│    spec = where(isActive()).and(hasCity("New York"))                              │
│    Generated SQL:                                                                │
│      SELECT DISTINCT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active           │
│      FROM user_details u1_0                                                      │
│      JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                          │
│      WHERE u1_0.active = true AND a1_0.city = 'New York'                         │
│                                                                                  │
│    Result:                                                                       │
│      ┌────┬───────────┬──────────────────┬────────┐                              │
│      │ id │ user_name │ email            │ active │                              │
│      ├────┼───────────┼──────────────────┼────────┤                              │
│      │  1 │ Alice     │ alice@ex.com     │ true   │ ← address in New York        │
│      │  4 │ Diana     │ diana@ex.com     │ true   │ ← address in New York        │
│      └────┴───────────┴──────────────────┴────────┘                              │
│                                                                                  │
│  search("Alice", null, null, null):                                              │
│    spec = where(hasName("Alice")).and(null).and(null).and(null)                   │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Alice%'                                         │
│    → No JOIN (city and country are null → JOIN specs not added)                   │
│                                                                                  │
│  search(null, true, "London", "UK"):                                             │
│    spec = where(null).and(isActive()).and(hasCity("London"))                      │
│                       .and(hasCountry("UK"))                                     │
│    Generated SQL:                                                                │
│      SELECT DISTINCT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active           │
│      FROM user_details u1_0                                                      │
│      JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                          │
│      WHERE u1_0.active = true AND a1_0.city = 'London' AND a1_0.country = 'UK'  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Multiselect with Specification API — using EntityManager for Object[] results:**

`JpaSpecificationExecutor.findAll()` always returns full entities. For partial column selection (multiselect/Object[]), you can use Specifications as predicate providers inside an EntityManager-based approach:

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Multiselect (specific columns) — uses Specification as predicate provider
     * but executes via EntityManager for Object[] result.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUserNamesWithCity(String city, Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        Join<UserDetails, UserAddress> addrJoin = root.join("addresses");

        cq.multiselect(root.get("name"), addrJoin.get("city"));

        // Build WHERE using Specification objects
        List<Predicate> predicates = new ArrayList<>();
        if (city != null) {
            // Call Specification.toPredicate manually
            predicates.add(
                UserSpecs.hasCity(city).toPredicate(root, cq, cb)
            );
        }
        if (active != null) {
            predicates.add(
                UserSpecs.isActive(active).toPredicate(root, cq, cb)
            );
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        return entityManager.createQuery(cq).getResultList();
    }
    // Generated SQL:
    //   SELECT u1_0.user_name, a1_0.city
    //   FROM user_details u1_0
    //   JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id
    //   WHERE a1_0.city = 'New York' AND u1_0.active = true
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification API + Multiselect — Two Approaches:                               │
│                                                                                  │
│  Approach 1: JpaSpecificationExecutor.findAll(spec)                              │
│    → Always returns full entities (List<UserDetails>)                            │
│    → Handles all boilerplate                                                     │
│    → CANNOT do multiselect / Object[]                                            │
│    → Use for 90% of queries                                                      │
│                                                                                  │
│  Approach 2: EntityManager + spec.toPredicate() manually                         │
│    → Can do multiselect → Object[]                                               │
│    → Still reuses Specification predicate logic (no code duplication)             │
│    → Requires some boilerplate (CriteriaBuilder, Root, etc.)                     │
│    → Use for the 10% of queries that need partial columns                        │
│                                                                                  │
│  In Approach 2, you call spec.toPredicate(root, query, cb) manually              │
│  to extract the Predicate from a Specification and apply it to your custom query.│
│  The predicate logic is still defined once in UserSpecs.                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  NOTE — Duplicate JOIN issue:                                                    │
│                                                                                  │
│  If two Specifications both call root.join("addresses"), two separate JOINs      │
│  are created:                                                                    │
│    hasCity("New York")  → root.join("addresses") → JOIN #1                       │
│    hasCountry("USA")    → root.join("addresses") → JOIN #2                       │
│    SQL: ... JOIN user_addresses a1 ON ... JOIN user_addresses a2 ON ...           │
│    → Two JOINs on the same table! Inefficient but correct.                       │
│                                                                                  │
│  To avoid duplicate JOINs, either:                                               │
│  1. Combine conditions into one Specification:                                   │
│     public static Specification<UserDetails> hasAddress(String city, String co) { │
│         return (root, query, cb) -> {                                            │
│             Join<UserDetails, UserAddress> join = root.join("addresses");         │
│             query.distinct(true);                                                │
│             List<Predicate> preds = new ArrayList<>();                            │
│             if (city != null) preds.add(cb.equal(join.get("city"), city));        │
│             if (co != null) preds.add(cb.equal(join.get("country"), co));         │
│             return cb.and(preds.toArray(new Predicate[0]));                       │
│         };                                                                       │
│     }                                                                            │
│                                                                                  │
│  2. Reuse existing JOINs by checking root.getJoins():                            │
│     Join<?, ?> join = root.getJoins().stream()                                   │
│         .filter(j -> j.getAttribute().getName().equals("addresses"))              │
│         .findFirst()                                                             │
│         .orElseGet(() -> root.join("addresses"));                                │
│     → Uses existing JOIN if already present, creates new one if not.             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Pagination and Sorting with Specification API

`JpaSpecificationExecutor` provides `findAll(Specification, Pageable)` which handles pagination and sorting automatically. No manual `setFirstResult`/`setMaxResults` or `orderBy` needed.

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Dynamic search with pagination and sorting.
     */
    public Page<UserDetails> searchUsersPaginated(String name, String email,
                                                   Boolean active,
                                                   int page, int size,
                                                   String sortBy, String sortDir) {

        // Build Specification dynamically
        Specification<UserDetails> spec = Specification
            .where(name != null ? UserSpecs.hasName(name) : null)
            .and(email != null ? UserSpecs.hasEmail(email) : null)
            .and(active != null ? UserSpecs.isActive(active) : null);

        // Build Sort
        Sort sort;
        List<String> allowedSortFields = List.of("id", "name", "email", "active");
        if (sortBy != null && allowedSortFields.contains(sortBy)) {
            sort = Sort.by(
                "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
            );
        } else {
            sort = Sort.by(Sort.Direction.ASC, "id");  // default sort
        }

        // Build Pageable (page number + page size + sort)
        Pageable pageable = PageRequest.of(page, size, sort);

        // Execute — Spring handles EVERYTHING:
        //   1. Creates CriteriaBuilder, CriteriaQuery, Root
        //   2. Calls spec.toPredicate() for WHERE clause
        //   3. Adds ORDER BY from Sort
        //   4. Adds LIMIT/OFFSET from Pageable
        //   5. Executes DATA query
        //   6. Executes COUNT query (for totalElements)
        //   7. Returns Page<UserDetails>
        return repository.findAll(spec, pageable);
    }

    /**
     * Search with sorting only (no pagination — returns List, not Page)
     */
    public List<UserDetails> searchUsersSorted(Boolean active, String sortBy) {
        Specification<UserDetails> spec = active != null ? UserSpecs.isActive(active) : null;

        Sort sort = Sort.by(Sort.Direction.ASC, sortBy != null ? sortBy : "id");

        return repository.findAll(spec, sort);
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
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        return userService.searchUsersPaginated(name, email, active,
                                                 page, size, sortBy, sortDir);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification + Pagination — Scenario:                                          │
│                                                                                  │
│  Request: GET /api/users/search?active=true&page=1&size=2&sortBy=name&sortDir=ASC│
│                                                                                  │
│  spec = where(null).and(null).and(isActive(true))                                │
│  pageable = PageRequest.of(1, 2, Sort.by(ASC, "name"))                           │
│                                                                                  │
│  Spring internally generates TWO queries:                                        │
│                                                                                  │
│  Data query:                                                                     │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.active = true                                                      │
│    ORDER BY u1_0.user_name ASC                                                   │
│    LIMIT 2 OFFSET 2                                                              │
│                                                                                  │
│  Count query:                                                                    │
│    SELECT COUNT(u1_0.id) FROM user_details u1_0 WHERE u1_0.active = true         │
│    → Returns: 5                                                                  │
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
│  KEY ADVANTAGE over Criteria API pagination:                                     │
│    - No manual setFirstResult/setMaxResults                                      │
│    - No manual count query (Spring generates it automatically!)                  │
│    - No manual PageImpl construction                                             │
│    - Just: repository.findAll(spec, pageable) → Page<T>                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Pagination with JOIN Specifications:**

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Paginated search with JOIN filter
     */
    public Page<UserDetails> searchInCity(String city, Boolean active,
                                           int page, int size) {

        Specification<UserDetails> spec = Specification
            .where(city != null ? UserSpecs.hasCity(city) : null)
            .and(active != null ? UserSpecs.isActive(active) : null);

        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));

        return repository.findAll(spec, pageable);
    }
    // SQL (city="New York", active=true, page=0, size=2):
    //   SELECT DISTINCT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active
    //   FROM user_details u1_0
    //   JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id
    //   WHERE a1_0.city = 'New York' AND u1_0.active = true
    //   ORDER BY u1_0.user_name ASC
    //   LIMIT 2 OFFSET 0
    //
    // Count SQL (auto-generated):
    //   SELECT COUNT(DISTINCT u1_0.id)
    //   FROM user_details u1_0
    //   JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id
    //   WHERE a1_0.city = 'New York' AND u1_0.active = true
}
```

**Multi-column sorting:**

```java
// Sort by active DESC, then by name ASC
Sort sort = Sort.by(
    Sort.Order.desc("active"),
    Sort.Order.asc("name")
);
Pageable pageable = PageRequest.of(0, 10, sort);

// SQL: ... ORDER BY u1_0.active DESC, u1_0.user_name ASC LIMIT 10 OFFSET 0
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification Pagination vs Criteria API Pagination:                            │
│                                                                                  │
│  Criteria API:                                                                   │
│    // Data query                                                                 │
│    CriteriaBuilder cb = entityManager.getCriteriaBuilder();                      │
│    CriteriaQuery<UserDetails> dataCq = cb.createQuery(UserDetails.class);        │
│    Root<UserDetails> dataRoot = dataCq.from(UserDetails.class);                  │
│    dataCq.select(dataRoot).where(predicates);                                    │
│    dataCq.orderBy(cb.asc(dataRoot.get("name")));                                 │
│    TypedQuery<UserDetails> dataQuery = entityManager.createQuery(dataCq);        │
│    dataQuery.setFirstResult(page * size);                                        │
│    dataQuery.setMaxResults(size);                                                │
│    List<UserDetails> content = dataQuery.getResultList();                         │
│    // Count query (separate!)                                                    │
│    CriteriaQuery<Long> countCq = cb.createQuery(Long.class);                     │
│    Root<UserDetails> countRoot = countCq.from(UserDetails.class);                │
│    countCq.select(cb.count(countRoot)).where(countPredicates);                   │
│    Long total = entityManager.createQuery(countCq).getSingleResult();            │
│    return new PageImpl<>(content, pageable, total);                               │
│    → 14 lines of boilerplate!                                                    │
│                                                                                  │
│  Specification API:                                                              │
│    Pageable pageable = PageRequest.of(page, size, Sort.by("name"));              │
│    return repository.findAll(spec, pageable);                                    │
│    → 2 lines! Spring handles data query, count query, and PageImpl.              │
│                                                                                  │
│  Specification API eliminates:                                                   │
│    ✓ Manual CriteriaBuilder/CriteriaQuery/Root setup                             │
│    ✓ Manual orderBy with cb.asc()/cb.desc()                                      │
│    ✓ Manual setFirstResult/setMaxResults                                         │
│    ✓ Manual count query creation and execution                                   │
│    ✓ Manual PageImpl construction                                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Summary — Specification API

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification API — Complete Pattern:                                           │
│                                                                                  │
│  1. Define Specifications (once):                                                │
│     public class UserSpecs {                                                     │
│         static Specification<UserDetails> hasName(String name) {                 │
│             return (root, query, cb) -> cb.like(root.get("name"), "%" +name+ "%"│
│             );                                                                   │
│         }                                                                        │
│         static Specification<UserDetails> isActive() {                           │
│             return (root, query, cb) -> cb.equal(root.get("active"), true);      │
│         }                                                                        │
│     }                                                                            │
│                                                                                  │
│  2. Repository (extend JpaSpecificationExecutor):                                │
│     interface UserDetailsRepository                                              │
│         extends JpaRepository<UserDetails, Long>,                                │
│                 JpaSpecificationExecutor<UserDetails> { }                         │
│                                                                                  │
│  3. Compose and Execute (in service):                                            │
│     Specification<UserDetails> spec = Specification                              │
│         .where(name != null ? hasName(name) : null)                              │
│         .and(active != null ? isActive() : null);                                │
│     repository.findAll(spec);                     // List<T>                     │
│     repository.findAll(spec, pageable);           // Page<T>                     │
│     repository.findAll(spec, sort);               // List<T> sorted              │
│     repository.count(spec);                       // long                        │
│     repository.exists(spec);                      // boolean                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Complete Decision Table — All Query Approaches (updated):

  ┌─────────────────────────────────────┬──────────────────────────────────────────┐
  │ Scenario                            │ Best Approach                            │
  ├─────────────────────────────────────┼──────────────────────────────────────────┤
  │ Simple findBy with 1-2 conditions   │ Derived Query (method name)              │
  │ Fixed complex query on entities     │ JPQL @Query on Repository                │
  │ Fixed complex query on tables       │ Native @Query on Repository              │
  │ DB-specific features (JSONB, etc.)  │ Native @Query on Repository              │
  │ Dynamic query, DB-locked            │ Dynamic Native Query (EntityManager)     │
  │ Dynamic query, DB-independent       │ Criteria API or Specification API        │
  │ Dynamic query, type-safe            │ Criteria API + Metamodel                 │
  │ Reusable dynamic predicates         │ Specification API                        │
  │ Dynamic query + minimal boilerplate │ Specification API                        │
  │ Dynamic JOINs (related entities)    │ Specification API or Criteria API        │
  │ Dynamic JOINs (unrelated tables)    │ Dynamic Native Query (EntityManager)     │
  │ Multiselect (partial columns)       │ Criteria API (multiselect)               │
  │ Pagination + sorting + dynamic WHERE│ Specification API (simplest)             │
  │ Dynamic GROUP BY / HAVING           │ Criteria API or Dynamic Native Query     │
  │ Dynamic UPDATE (partial fields)     │ Dynamic Native Query (EntityManager)     │
  │ Maximum readability                 │ JPQL or Native Query (SQL is familiar)   │
  │ Maximum type safety                 │ Criteria API + Metamodel                 │
  │ Maximum DB portability              │ Specification API, Criteria API, or JPQL │
  │ Maximum reusability                 │ Specification API                        │
  └─────────────────────────────────────┴──────────────────────────────────────────┘
```


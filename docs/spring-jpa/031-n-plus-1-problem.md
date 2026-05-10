### The N+1 Problem in JPA, What Is the N+1 Problem?

The N+1 problem is a **performance anti-pattern** where loading N parent entities triggers N additional SQL queries to load their child entities — resulting in **1 + N total queries** instead of a single query.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 Problem — The Core Issue:                                                   │
│                                                                                  │
│  You want: "Give me all users and their addresses"                               │
│                                                                                  │
│  What SHOULD happen (1 query):                                                   │
│    SQL: SELECT u.*, a.* FROM user_details u                                      │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│    → 1 query, all data loaded                                                    │
│                                                                                  │
│  What ACTUALLY happens with N+1 (1 + N queries):                                 │
│    SQL 1: SELECT u.* FROM user_details               ← 1 query for parents      │
│           → Returns 5 users                                                      │
│                                                                                  │
│    SQL 2: SELECT a.* FROM user_addresses WHERE user_id = 1  ← child for user 1  │
│    SQL 3: SELECT a.* FROM user_addresses WHERE user_id = 2  ← child for user 2  │
│    SQL 4: SELECT a.* FROM user_addresses WHERE user_id = 3  ← child for user 3  │
│    SQL 5: SELECT a.* FROM user_addresses WHERE user_id = 4  ← child for user 4  │
│    SQL 6: SELECT a.* FROM user_addresses WHERE user_id = 5  ← child for user 5  │
│           → 5 additional queries (one per parent)                                │
│                                                                                  │
│    Total: 1 + 5 = 6 queries instead of 1!                                       │
│                                                                                  │
│  If N = 1000 users → 1001 queries!                                               │
│  If N = 10000 users → 10001 queries!                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**When does it happen and in which mapping?**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 happens in ANY relationship mapping with collections:                       │
│                                                                                  │
│  @OneToMany  → MOST COMMON. Parent has List<Child>.                              │
│  @ManyToMany → Parent has Set<Child> via join table.                             │
│  @ManyToOne  → Less common, but possible with eager fetch on the "many" side.    │
│  @OneToOne   → Possible with lazy proxy, but rare (only 1 extra query per row).  │
│                                                                                  │
│  The problem is worst with @OneToMany and @ManyToMany because each parent        │
│  has a COLLECTION of children — each collection triggers a separate SQL query.   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Does it happen on EAGER or LAZY?**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 can happen with BOTH FetchType.EAGER and FetchType.LAZY:                    │
│                                                                                  │
│  EAGER (FetchType.EAGER):                                                        │
│    N+1 happens IMMEDIATELY when you load the parent.                             │
│    Hibernate loads the parent → then fires N queries for children automatically. │
│    You can't avoid it — children are loaded whether you need them or not.        │
│    N+1 is HIDDEN — you don't explicitly call getChildren(), but queries fire.    │
│                                                                                  │
│  LAZY (FetchType.LAZY) — default for @OneToMany / @ManyToMany:                  │
│    N+1 happens WHEN YOU ACCESS the children collection.                          │
│    Loading the parent fires 1 query. Children are proxied (not loaded).          │
│    When you call user.getAddresses() for each user → N queries fire.            │
│    N+1 is VISIBLE — you can see it when you iterate and access children.        │
│                                                                                  │
│  KEY POINT: Lazy doesn't SOLVE N+1. It only DEFERS it.                          │
│  If you eventually access all children, you still get N+1 queries.              │
│  Lazy is better ONLY if you DON'T always need the children.                     │
│                                                                                  │
│  ┌──────────────┬──────────────────────────┬────────────────────────────────┐    │
│  │ FetchType    │ When N+1 fires           │ Can you avoid the N queries?  │    │
│  ├──────────────┼──────────────────────────┼────────────────────────────────┤    │
│  │ EAGER        │ Immediately on load      │ No — always fires N queries   │    │
│  │ LAZY         │ When you access children │ Yes — if you never access     │    │
│  │              │                          │ children, no extra queries    │    │
│  └──────────────┴──────────────────────────┴────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Performance impact:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Performance Impact of N+1:                                                      │
│                                                                                  │
│  1. DATABASE ROUND-TRIPS:                                                        │
│     Each query = 1 network round-trip to the database.                           │
│     1001 queries = 1001 round-trips over the network.                            │
│     Even if each query takes 1ms → total = 1+ second just in network latency.   │
│                                                                                  │
│  2. CONNECTION POOL EXHAUSTION:                                                   │
│     Each query holds a DB connection.                                            │
│     With many concurrent requests, the connection pool drains.                   │
│     Other requests wait → timeouts → application becomes unresponsive.           │
│                                                                                  │
│  3. DATABASE CPU/IO:                                                             │
│     1001 separate queries = 1001 query plans, 1001 index lookups.               │
│     1 JOIN query = 1 query plan, 1 optimized scan.                              │
│     The database does far more work with N+1.                                    │
│                                                                                  │
│  4. MEMORY PRESSURE:                                                             │
│     With EAGER, all children loaded into memory even if not needed.              │
│     1000 parents × 10 children each = 10,000 entities in L1 cache.              │
│                                                                                  │
│  5. LATENCY IN API RESPONSE:                                                     │
│     REST API that returns paginated users with addresses:                        │
│     Without N+1 fix: 200ms+ response time.                                      │
│     With N+1 fix (JOIN FETCH): 5-20ms response time.                            │
│     10x-100x performance difference!                                             │
│                                                                                  │
│  Example:                                                                        │
│    N = 100 parents, each with 5 children                                         │
│    Without fix: 1 + 100 = 101 SQL queries                                        │
│    With JOIN FETCH: 1 SQL query                                                  │
│    With @BatchSize(25): 1 + 4 = 5 SQL queries                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### N+1 with Eager Loading — Multiple Parents with Child Collections

When you load multiple parent entities that have `FetchType.EAGER` on a `@OneToMany` relationship, Hibernate fires 1 query for the parents and then N additional queries — one per parent — to load the children.

**Entities:**

```java
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToMany(mappedBy = "userDetails", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<UserAddress> userAddressList = new ArrayList<>();
    //                                              ↑ EAGER = load children IMMEDIATELY

    // getters, setters, constructors
}

@Entity
@Table(name = "user_addresses")
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserDetails userDetails;

    // getters, setters, constructors
}
```

```text
Database:

  user_details                           user_addresses
  ┌────┬────────┬──────────────┐         ┌────┬──────────────┬──────────┬─────────┬─────────┐
  │ id │ name   │ email        │         │ id │ street       │ city     │ country │ user_id │
  ├────┼────────┼──────────────┤         ├────┼──────────────┼──────────┼─────────┼─────────┤
  │  1 │ Alice  │ alice@ex.com │         │ 10 │ 123 Main St  │ New York │ USA     │    1    │
  │  2 │ Bob    │ bob@ex.com   │         │ 11 │ 456 Oak Ave  │ Boston   │ USA     │    1    │
  │  3 │ Charlie│ char@ex.com  │         │ 12 │ 789 Pine Rd  │ London   │ UK      │    2    │
  └────┴────────┴──────────────┘         │ 13 │ 321 Elm St   │ Mumbai   │ India   │    3    │
                                          │ 14 │ 654 Birch Dr │ Delhi    │ India   │    3    │
                                          └────┴──────────────┴──────────┴─────────┴─────────┘
```

```java
// Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {
    // Using built-in findAll()
}

// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public List<UserDetails> getAllUsers() {
        return userDetailsRepository.findAll();
        // You expect 1 query. You get 1 + 3 = 4 queries!
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 with FetchType.EAGER — What Hibernate does:                                 │
│                                                                                  │
│  You call: userDetailsRepository.findAll()                                       │
│                                                                                  │
│  QUERY 1 (the "1" in N+1) — Load all parents:                                   │
│    SQL: SELECT ud.id, ud.name, ud.email                                          │
│         FROM user_details ud                                                     │
│    → Returns 3 users: [Alice(1), Bob(2), Charlie(3)]                             │
│                                                                                  │
│  Now Hibernate sees FetchType.EAGER on userAddressList.                          │
│  It MUST load addresses NOW, before returning results.                           │
│                                                                                  │
│  QUERY 2 (N=1) — Load addresses for Alice (user_id = 1):                        │
│    SQL: SELECT ua.id, ua.street, ua.city, ua.country, ua.user_id                 │
│         FROM user_addresses ua                                                   │
│         WHERE ua.user_id = 1                                                     │
│    → Returns 2 addresses [10, 11]                                                │
│                                                                                  │
│  QUERY 3 (N=2) — Load addresses for Bob (user_id = 2):                          │
│    SQL: SELECT ua.id, ua.street, ua.city, ua.country, ua.user_id                 │
│         FROM user_addresses ua                                                   │
│         WHERE ua.user_id = 2                                                     │
│    → Returns 1 address [12]                                                      │
│                                                                                  │
│  QUERY 4 (N=3) — Load addresses for Charlie (user_id = 3):                      │
│    SQL: SELECT ua.id, ua.street, ua.city, ua.country, ua.user_id                 │
│         FROM user_addresses ua                                                   │
│         WHERE ua.user_id = 3                                                     │
│    → Returns 2 addresses [13, 14]                                                │
│                                                                                  │
│  TOTAL: 1 + 3 = 4 queries for 3 parents.                                        │
│  If you had 1000 users → 1 + 1000 = 1001 queries!                               │
│                                                                                  │
│  Timeline:                                                                       │
│  ┌────────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐                      │
│  │ SELECT     │  │ SELECT   │  │ SELECT   │  │ SELECT   │                      │
│  │ all users  │→│ addr for │→│ addr for │→│ addr for │                      │
│  │ (1 query)  │  │ Alice    │  │ Bob      │  │ Charlie  │                      │
│  └────────────┘  └──────────┘  └──────────┘  └──────────┘                      │
│       "1"             N=1           N=2           N=3                            │
│                                                                                  │
│  This happens AUTOMATICALLY with EAGER. You don't call getAddresses().          │
│  Hibernate fires N queries silently behind the scenes.                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### N+1 with JPQL JOIN — UserDetails → UserAddress (@OneToMany, Lazy)

Now let's use `FetchType.LAZY` (default for @OneToMany) and a JPQL `JOIN` query. The N+1 still happens — just **deferred** to when you access the collection.

**Entities (with LAZY this time):**

```java
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToMany(mappedBy = "userDetails", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAddress> userAddressList = new ArrayList<>();
    //                                              ↑ LAZY = don't load until accessed

    // getters, setters
}
```

```java
// Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query("SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad WHERE ud.name = :userFirstName")
    List<UserDetails> findUserDetailsWithAddress(@Param("userFirstName") String userFirstName);
}
```

```java
// Service — N+1 happens here
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional(readOnly = true)
    public List<UserDetails> getUsersWithAddresses(String name) {
        List<UserDetails> users = userDetailsRepository.findUserDetailsWithAddress(name);
        // QUERY 1: SELECT ud.* FROM user_details ud
        //          JOIN user_addresses ad ON ud.id = ad.user_id
        //          WHERE ud.name = 'Alice'
        // → Returns 1 user (Alice) — addresses NOT loaded yet (LAZY proxy)

        // Now we iterate and access the addresses:
        for (UserDetails user : users) {
            List<UserAddress> addresses = user.getUserAddressList();
            // ↑ LAZY proxy triggered! Hibernate fires a query NOW.
            System.out.println(user.getName() + " has " + addresses.size() + " addresses");
        }
        // If findUserDetailsWithAddress returned 5 users:
        //   QUERY 2: SELECT * FROM user_addresses WHERE user_id = 1
        //   QUERY 3: SELECT * FROM user_addresses WHERE user_id = 2
        //   QUERY 4: SELECT * FROM user_addresses WHERE user_id = 3
        //   QUERY 5: SELECT * FROM user_addresses WHERE user_id = 4
        //   QUERY 6: SELECT * FROM user_addresses WHERE user_id = 5
        //   → 5 extra queries! (N+1 problem)

        return users;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 with JPQL JOIN (FetchType.LAZY):                                            │
│                                                                                  │
│  JPQL: SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad                  │
│        WHERE ud.name = :userFirstName                                            │
│                                                                                  │
│  NOTE: JPQL "JOIN" is used ONLY for filtering (WHERE clause).                    │
│        It does NOT load the child collection!                                    │
│        The addresses are still LAZY proxies after this query.                    │
│                                                                                  │
│  QUERY 1 (the "1"):                                                              │
│    SQL: SELECT ud.id, ud.name, ud.email                                          │
│         FROM user_details ud                                                     │
│         INNER JOIN user_addresses ad ON ud.id = ad.user_id                       │
│         WHERE ud.name = 'Alice'                                                  │
│    → Returns UserDetails(Alice) with userAddressList = LAZY PROXY (not loaded)   │
│                                                                                  │
│  When you call: user.getUserAddressList().size()                                 │
│                                                                                  │
│  QUERY 2 (N=1):                                                                 │
│    SQL: SELECT ua.id, ua.street, ua.city, ua.country, ua.user_id                 │
│         FROM user_addresses ua                                                   │
│         WHERE ua.user_id = 1                                                     │
│    → Loads addresses for Alice                                                   │
│                                                                                  │
│  If the query returned multiple users (e.g., 3 users named "Alice"):            │
│                                                                                  │
│  QUERY 3 (N=2): SELECT ... FROM user_addresses WHERE user_id = 2                │
│  QUERY 4 (N=3): SELECT ... FROM user_addresses WHERE user_id = 3                │
│                                                                                  │
│  TOTAL: 1 + 3 = 4 queries.                                                      │
│                                                                                  │
│  Flow:                                                                           │
│  ┌─────────────────┐      ┌──────────────────┐                                  │
│  │ JPQL JOIN query  │      │ Returns users    │                                  │
│  │ (1 SQL query)    │ ──>  │ with LAZY proxy  │                                  │
│  │                  │      │ for addresses    │                                  │
│  └─────────────────┘      └────────┬─────────┘                                  │
│                                     │                                            │
│                            for each user:                                        │
│                            user.getUserAddressList()                              │
│                                     │                                            │
│                        ┌────────────┼────────────┐                               │
│                        v            v            v                                │
│                  ┌──────────┐ ┌──────────┐ ┌──────────┐                          │
│                  │ SELECT   │ │ SELECT   │ │ SELECT   │                          │
│                  │ addr for │ │ addr for │ │ addr for │                          │
│                  │ user 1   │ │ user 2   │ │ user 3   │                          │
│                  └──────────┘ └──────────┘ └──────────┘                          │
│                      N=1          N=2          N=3                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Solution 1: JOIN FETCH — Load Everything in One Query

`JOIN FETCH` tells Hibernate: "Not only JOIN for filtering, but also **FETCH** (load) the child collection into memory in the SAME query." This eliminates all N extra queries.

```java
// Repository — using JOIN FETCH
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query("SELECT ud FROM UserDetails ud JOIN FETCH ud.userAddressList ad WHERE ud.name = :userFirstName")
    List<UserDetails> findUserDetailsWithAddress(@Param("userFirstName") String userFirstName);
    //                                    ↑ FETCH keyword added
}
```

```java
// Service — NO more N+1!
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional(readOnly = true)
    public List<UserDetails> getUsersWithAddresses(String name) {
        List<UserDetails> users = userDetailsRepository.findUserDetailsWithAddress(name);
        // ONLY 1 QUERY! All addresses loaded in the same query.

        for (UserDetails user : users) {
            List<UserAddress> addresses = user.getUserAddressList();
            // ↑ NO lazy proxy trigger! Addresses are ALREADY loaded.
            // NO additional SQL query fired here.
            System.out.println(user.getName() + " has " + addresses.size() + " addresses");
        }

        return users;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JOIN FETCH — How it solves N+1:                                                 │
│                                                                                  │
│  JPQL: SELECT ud FROM UserDetails ud                                             │
│        JOIN FETCH ud.userAddressList ad                                          │
│        WHERE ud.name = :userFirstName                                            │
│                                                                                  │
│  SINGLE SQL QUERY generated:                                                     │
│    SELECT ud.id, ud.name, ud.email,                                              │
│           ad.id, ad.street, ad.city, ad.country, ad.user_id                      │
│    FROM user_details ud                                                          │
│    INNER JOIN user_addresses ad ON ud.id = ad.user_id                            │
│    WHERE ud.name = 'Alice'                                                       │
│                                                                                  │
│  Result set (raw SQL rows):                                                      │
│  ┌──────┬───────┬──────────────┬──────┬──────────────┬──────────┬─────────┐      │
│  │ud.id │ud.name│ ud.email     │ ad.id│ ad.street    │ ad.city  │ad.country│     │
│  ├──────┼───────┼──────────────┼──────┼──────────────┼──────────┼─────────┤      │
│  │  1   │ Alice │ alice@ex.com │  10  │ 123 Main St  │ New York │ USA     │      │
│  │  1   │ Alice │ alice@ex.com │  11  │ 456 Oak Ave  │ Boston   │ USA     │      │
│  └──────┴───────┴──────────────┴──────┴──────────────┴──────────┴─────────┘      │
│                                                                                  │
│  Hibernate de-duplicates and builds:                                             │
│    UserDetails(id=1, name="Alice") {                                             │
│      userAddressList: [                                                          │
│        UserAddress(id=10, street="123 Main St", city="New York"),                │
│        UserAddress(id=11, street="456 Oak Ave", city="Boston")                   │
│      ]                                                                           │
│    }                                                                             │
│                                                                                  │
│  TOTAL QUERIES: 1  (instead of 1 + N)                                            │
│                                                                                  │
│  Before (JOIN):       1 + N queries    │   After (JOIN FETCH):  1 query          │
│  ┌──────────────┐     ┌──────────┐     │   ┌─────────────────────────┐           │
│  │ SELECT users │ ──> │ SELECT   │×N   │   │ SELECT users + addresses│           │
│  └──────────────┘     │ addresses│     │   │ in ONE query            │           │
│                       └──────────┘     │   └─────────────────────────┘           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
JOIN vs JOIN FETCH — Comparison:

  ┌──────────────────────────────────────┬────────────────────────────────────────┐
  │ JOIN (regular)                       │ JOIN FETCH                             │
  ├──────────────────────────────────────┼────────────────────────────────────────┤
  │ Used for filtering (WHERE clause)    │ Used for filtering AND loading         │
  │ Children are NOT loaded              │ Children ARE loaded in the same query  │
  │ Children remain LAZY proxies         │ Children are fully initialized         │
  │ Accessing children = N extra queries │ Accessing children = 0 extra queries   │
  │ SELECT returns parent columns only   │ SELECT returns parent + child columns  │
  │ N+1 problem remains                  │ N+1 problem SOLVED                     │
  └──────────────────────────────────────┴────────────────────────────────────────┘

  NOTE: JOIN FETCH cannot be used with Pageable directly.
  Hibernate warns: "firstResult/maxResults specified with collection fetch; applying in memory!"
  For pagination + JOIN FETCH, use a two-query approach or @EntityGraph.
```

---

### Solution 2: @BatchSize — Batch the N Queries Into Fewer Queries

`@BatchSize(size = N)` tells Hibernate: "Instead of loading children one parent at a time, load children for N parents in a single query using `IN (?, ?, ?, ...)`."

This doesn't reduce to 1 query like JOIN FETCH, but it reduces from N to ⌈N/batchSize⌉.

```java
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToMany(mappedBy = "userDetails", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)   // ← load addresses for 10 users at a time
    private List<UserAddress> userAddressList = new ArrayList<>();

    // getters, setters
}
```

```java
// Repository — regular JPQL JOIN (NOT JOIN FETCH)
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query("SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad WHERE ud.name = :userFirstName")
    List<UserDetails> findUserDetailsWithAddress(@Param("userFirstName") String userFirstName);
}
```

```java
// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional(readOnly = true)
    public List<UserDetails> getUsersWithAddresses(String name) {
        List<UserDetails> users = userDetailsRepository.findUserDetailsWithAddress(name);
        // QUERY 1: SELECT ud.* FROM user_details ud
        //          JOIN user_addresses ad ON ud.id = ad.user_id
        //          WHERE ud.name = 'Alice'
        // → Returns users (addresses are still LAZY proxies)

        for (UserDetails user : users) {
            user.getUserAddressList().size();  // triggers batch load
        }
        // With @BatchSize(size = 10):
        // If 25 users returned, instead of 25 individual queries:
        //   QUERY 2: SELECT * FROM user_addresses WHERE user_id IN (1,2,3,4,5,6,7,8,9,10)
        //   QUERY 3: SELECT * FROM user_addresses WHERE user_id IN (11,12,13,14,15,16,17,18,19,20)
        //   QUERY 4: SELECT * FROM user_addresses WHERE user_id IN (21,22,23,24,25)
        // Total: 1 + 3 = 4 queries instead of 1 + 25 = 26!

        return users;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @BatchSize(size = 10) — How it works:                                           │
│                                                                                  │
│  Without @BatchSize (N+1):                                                       │
│    Query 1: SELECT users → 25 users                                              │
│    Query 2: SELECT addresses WHERE user_id = 1                                   │
│    Query 3: SELECT addresses WHERE user_id = 2                                   │
│    ...                                                                           │
│    Query 26: SELECT addresses WHERE user_id = 25                                 │
│    TOTAL: 26 queries                                                             │
│                                                                                  │
│  With @BatchSize(size = 10):                                                     │
│    Query 1: SELECT users → 25 users                                              │
│    Query 2: SELECT addresses WHERE user_id IN (1,2,3,4,5,6,7,8,9,10)            │
│    Query 3: SELECT addresses WHERE user_id IN (11,12,13,14,15,16,17,18,19,20)    │
│    Query 4: SELECT addresses WHERE user_id IN (21,22,23,24,25)                   │
│    TOTAL: 4 queries                                                              │
│                                                                                  │
│  Reduction: from 26 queries to 4 queries!                                        │
│                                                                                  │
│  Formula: Total queries = 1 + ⌈N / batchSize⌉                                   │
│    N = 25, batchSize = 10 → 1 + ⌈25/10⌉ = 1 + 3 = 4                            │
│    N = 100, batchSize = 25 → 1 + ⌈100/25⌉ = 1 + 4 = 5                          │
│    N = 1000, batchSize = 50 → 1 + ⌈1000/50⌉ = 1 + 20 = 21                      │
│                                                                                  │
│  Timeline comparison:                                                            │
│                                                                                  │
│  Without @BatchSize (25 users):                                                  │
│  ┌──────┐ ┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐...┌──┐                │
│  │users │ │a1││a2││a3││a4││a5││a6││a7││a8││a9││10││11│   │25│                │
│  └──────┘ └──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘...└──┘                │
│  26 queries total                                                                │
│                                                                                  │
│  With @BatchSize(10):                                                            │
│  ┌──────┐ ┌─────────────────┐ ┌─────────────────┐ ┌──────────┐                  │
│  │users │ │ addr IN(1..10)  │ │ addr IN(11..20) │ │IN(21..25)│                  │
│  └──────┘ └─────────────────┘ └─────────────────┘ └──────────┘                  │
│  4 queries total                                                                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Does @BatchSize work with FetchType.EAGER?**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @BatchSize + FetchType.EAGER — YES, it works!                                   │
│                                                                                  │
│  @OneToMany(mappedBy = "userDetails", fetch = FetchType.EAGER)                   │
│  @BatchSize(size = 10)                                                           │
│  private List<UserAddress> userAddressList;                                      │
│                                                                                  │
│  With EAGER, Hibernate loads children IMMEDIATELY after loading parents.         │
│  Without @BatchSize: fires N individual queries (1 per parent).                  │
│  With @BatchSize(10): batches the eager loading into chunks of 10.               │
│                                                                                  │
│  findAll() → 25 users                                                            │
│                                                                                  │
│  Without @BatchSize (EAGER):                                                     │
│    Query 1: SELECT users                                                         │
│    Query 2-26: SELECT addresses WHERE user_id = ? (× 25)                         │
│    Total: 26                                                                     │
│                                                                                  │
│  With @BatchSize(10) + EAGER:                                                    │
│    Query 1: SELECT users                                                         │
│    Query 2: SELECT addresses WHERE user_id IN (1,2,...,10)   ← auto-triggered    │
│    Query 3: SELECT addresses WHERE user_id IN (11,12,...,20) ← auto-triggered    │
│    Query 4: SELECT addresses WHERE user_id IN (21,...,25)    ← auto-triggered    │
│    Total: 4                                                                      │
│                                                                                  │
│  The difference from LAZY:                                                       │
│  - EAGER + @BatchSize: batched queries fire IMMEDIATELY (no access needed)       │
│  - LAZY + @BatchSize: batched queries fire when you ACCESS the collection        │
│  - The SQL is the same — only the TIMING differs                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Solution 3: @EntityGraph — Override Fetch Strategy Per Query

`@EntityGraph(attributePaths = "userAddressList")` tells JPA: "For this specific query, fetch `userAddressList` eagerly using a LEFT JOIN — regardless of the entity's FetchType annotation."

This is placed on **repository methods** (both derived queries and @Query methods).

```java
// Repository — using @EntityGraph on a derived query
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Derived query with @EntityGraph — eagerly fetches addresses in ONE query
    @EntityGraph(attributePaths = "userAddressList")
    List<UserDetails> findByName(String name);
    // Without @EntityGraph:
    //   SQL 1: SELECT ud.* FROM user_details ud WHERE ud.name = ?
    //   SQL 2-N: SELECT ua.* FROM user_addresses ua WHERE ua.user_id = ?  (per user)
    //
    // With @EntityGraph:
    //   SQL: SELECT ud.id, ud.name, ud.email,
    //               ua.id, ua.street, ua.city, ua.country, ua.user_id
    //        FROM user_details ud
    //        LEFT JOIN user_addresses ua ON ud.id = ua.user_id
    //        WHERE ud.name = ?
    //   → 1 query! Addresses loaded via LEFT JOIN.

    // @EntityGraph on findAll (override the built-in method)
    @EntityGraph(attributePaths = "userAddressList")
    @Override
    List<UserDetails> findAll();
    // SQL: SELECT ud.*, ua.*
    //      FROM user_details ud
    //      LEFT JOIN user_addresses ua ON ud.id = ua.user_id

    // @EntityGraph on a @Query method — also works!
    @EntityGraph(attributePaths = "userAddressList")
    @Query("SELECT ud FROM UserDetails ud WHERE ud.name = :name")
    List<UserDetails> findUsersByName(@Param("name") String name);
    // SQL: SELECT ud.*, ua.*
    //      FROM user_details ud
    //      LEFT JOIN user_addresses ua ON ud.id = ua.user_id
    //      WHERE ud.name = ?

    // Multiple attribute paths — fetch multiple collections/associations
    @EntityGraph(attributePaths = {"userAddressList", "orders"})
    List<UserDetails> findByEmail(String email);
    // SQL: SELECT ud.*, ua.*, o.*
    //      FROM user_details ud
    //      LEFT JOIN user_addresses ua ON ud.id = ua.user_id
    //      LEFT JOIN orders o ON ud.id = o.user_id
    //      WHERE ud.email = ?
}
```

```java
// Service — uses @EntityGraph method
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional(readOnly = true)
    public List<UserDetails> getUsersByName(String name) {
        List<UserDetails> users = userDetailsRepository.findByName(name);
        // ONLY 1 SQL query executed (LEFT JOIN includes addresses)

        for (UserDetails user : users) {
            // No lazy proxy trigger — addresses already loaded
            System.out.println(user.getName() + " has "
                + user.getUserAddressList().size() + " addresses");
            // NO additional SQL! Everything loaded in the initial query.
        }

        return users;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @EntityGraph — How it works:                                                    │
│                                                                                  │
│  @EntityGraph(attributePaths = "userAddressList")                                │
│  List<UserDetails> findByName(String name);                                      │
│                                                                                  │
│  At query time, JPA sees the @EntityGraph and:                                   │
│    1. Ignores the entity's FetchType (LAZY or EAGER — doesn't matter)            │
│    2. Adds a LEFT JOIN for "userAddressList" to the generated SQL                │
│    3. Fetches parent + child data in ONE query                                   │
│                                                                                  │
│  Generated SQL:                                                                  │
│    SELECT ud.id, ud.name, ud.email,                                              │
│           ua.id, ua.street, ua.city, ua.country, ua.user_id                      │
│    FROM user_details ud                                                          │
│    LEFT JOIN user_addresses ua ON ud.id = ua.user_id                             │
│    WHERE ud.name = 'Alice'                                                       │
│                                                                                  │
│  NOTE: @EntityGraph uses LEFT JOIN (not INNER JOIN)                               │
│  This means users WITHOUT addresses are also returned (with null address list).  │
│  JOIN FETCH uses INNER JOIN by default — users without addresses are excluded.   │
│                                                                                  │
│  ┌─────────────────────────────┬───────────────────────────────────┐             │
│  │ Without @EntityGraph        │ With @EntityGraph                  │             │
│  ├─────────────────────────────┼───────────────────────────────────┤             │
│  │ SQL 1: SELECT users         │ SQL 1: SELECT users LEFT JOIN     │             │
│  │ SQL 2: SELECT addr user 1   │        addresses                  │             │
│  │ SQL 3: SELECT addr user 2   │ (1 query total)                   │             │
│  │ SQL 4: SELECT addr user 3   │                                   │             │
│  │ (4 queries total)           │                                   │             │
│  └─────────────────────────────┴───────────────────────────────────┘             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Why JPA Tries to Optimize and What Happens with JOIN FETCH / @BatchSize / @EntityGraph

JPA (Hibernate) defaults to **lazy loading** as an optimization strategy: don't load data you don't need. This is the right default for most cases. But when you DO need the related data, lazy loading backfires into the N+1 problem.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why JPA defaults to LAZY (optimization):                                        │
│                                                                                  │
│  Scenario: Employee entity has 5 relationships:                                  │
│    @OneToMany  addresses                                                         │
│    @OneToMany  orders                                                            │
│    @ManyToMany projects                                                          │
│    @ManyToOne  department                                                        │
│    @OneToOne   profilePicture                                                    │
│                                                                                  │
│  If ALL were EAGER:                                                              │
│    findById(1) would fire:                                                       │
│      SQL 1: SELECT employee WHERE id = 1                                         │
│      SQL 2: SELECT addresses WHERE employee_id = 1                               │
│      SQL 3: SELECT orders WHERE employee_id = 1                                  │
│      SQL 4: SELECT projects via join table WHERE employee_id = 1                 │
│      SQL 5: SELECT department WHERE id = ?                                       │
│      SQL 6: SELECT profilePicture WHERE employee_id = 1                          │
│    → 6 queries every time, even if you only need the employee's name!            │
│                                                                                  │
│  With LAZY (default for collections):                                            │
│    findById(1) fires only:                                                       │
│      SQL 1: SELECT employee WHERE id = 1                                         │
│    → 1 query. Other data loaded ONLY when accessed.                              │
│    → If API only needs employee name → no wasted queries.                        │
│                                                                                  │
│  JPA's philosophy: "Load the minimum. Fetch more on demand."                     │
│  This is optimal for SINGLE entity access.                                       │
│  It becomes a problem for BULK access (loading many parents + children).         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**What each solution does to override JPA's default:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What happens when you add JOIN FETCH / @BatchSize / @EntityGraph:               │
│                                                                                  │
│  JPA's default behavior (LAZY):                                                  │
│    "Load parent. Create proxy for children. Load children only on access."       │
│    → Minimal initial load, but N+1 on bulk access.                               │
│                                                                                  │
│  JOIN FETCH:                                                                     │
│    "Override LAZY. Load parent AND children in ONE SQL with INNER JOIN."          │
│    → Maximum efficiency for known data needs.                                    │
│    → But: INNER JOIN excludes parents without children.                          │
│    → But: Can't use with Pageable (pagination done in memory).                   │
│    → But: Multiple JOIN FETCH on collections = Cartesian product.               │
│                                                                                  │
│  @BatchSize(size = N):                                                           │
│    "Keep LAZY, but when children are accessed, load them in batches of N."       │
│    → Doesn't change to 1 query, but reduces from N to ⌈N/batchSize⌉.            │
│    → Works with LAZY and EAGER both.                                             │
│    → Works with Pageable (no in-memory pagination issue).                        │
│    → Transparent — no change to repository methods.                              │
│    → But: Still more than 1 query.                                               │
│                                                                                  │
│  @EntityGraph(attributePaths = "..."):                                           │
│    "Override LAZY for THIS specific query. Fetch specified paths via LEFT JOIN." │
│    → Per-method control. Same entity, different fetch strategies per use case.   │
│    → Uses LEFT JOIN (includes parents without children).                         │
│    → Works with derived queries and @Query.                                      │
│    → Works with Pageable.                                                        │
│    → But: LEFT JOIN may return duplicate parents (use DISTINCT).                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code comparison — all three solutions side by side:**

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // PROBLEM — N+1 with regular JOIN (children not loaded)
    @Query("SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad WHERE ud.name = :name")
    List<UserDetails> findWithN1Problem(@Param("name") String name);
    // SQL 1: SELECT ud.* ... INNER JOIN ... WHERE name = ?      (1 query)
    // SQL 2..N: SELECT ua.* WHERE user_id = ?                    (N queries on access)

    // SOLUTION 1: JOIN FETCH — 1 query total
    @Query("SELECT ud FROM UserDetails ud JOIN FETCH ud.userAddressList WHERE ud.name = :name")
    List<UserDetails> findWithJoinFetch(@Param("name") String name);
    // SQL: SELECT ud.*, ua.* ... INNER JOIN ... WHERE name = ?   (1 query)

    // SOLUTION 2: @BatchSize — defined on entity, no repo change needed
    // Just use the same query as the N+1 version.
    // @BatchSize(size=10) on the entity field handles the batching.

    // SOLUTION 3: @EntityGraph — 1 query total (LEFT JOIN)
    @EntityGraph(attributePaths = "userAddressList")
    @Query("SELECT ud FROM UserDetails ud WHERE ud.name = :name")
    List<UserDetails> findWithEntityGraph(@Param("name") String name);
    // SQL: SELECT ud.*, ua.* ... LEFT JOIN ... WHERE name = ?    (1 query)
}
```

```text
Comparison table — all N+1 solutions:

  Scenario: 100 parents, each with children, @BatchSize(size = 25)

  ┌──────────────────────┬──────────┬────────────┬──────────────────────────────────┐
  │ Approach             │ Queries  │ JOIN type  │ Trade-offs                        │
  ├──────────────────────┼──────────┼────────────┼──────────────────────────────────┤
  │ No fix (N+1)         │ 101      │ —          │ Worst performance                │
  ├──────────────────────┼──────────┼────────────┼──────────────────────────────────┤
  │ JOIN FETCH           │ 1        │ INNER JOIN │ Best performance.                │
  │                      │          │            │ No Pageable support.             │
  │                      │          │            │ Excludes parents w/o children.   │
  │                      │          │            │ Cartesian product with multiple  │
  │                      │          │            │ collection fetches.              │
  ├──────────────────────┼──────────┼────────────┼──────────────────────────────────┤
  │ @BatchSize(25)       │ 5        │ —          │ Good performance.                │
  │                      │          │ (IN clause)│ Works with Pageable.             │
  │                      │          │            │ Transparent (entity-level).      │
  │                      │          │            │ Still multiple queries.           │
  ├──────────────────────┼──────────┼────────────┼──────────────────────────────────┤
  │ @EntityGraph         │ 1        │ LEFT JOIN  │ Best per-method control.         │
  │                      │          │            │ Works with Pageable.             │
  │                      │          │            │ Includes parents w/o children.   │
  │                      │          │            │ May need DISTINCT for dupes.     │
  └──────────────────────┴──────────┴────────────┴──────────────────────────────────┘

  When to use which:

  ┌──────────────────────────────────────────┬──────────────────────────────────────┐
  │ Use Case                                 │ Best Solution                        │
  ├──────────────────────────────────────────┼──────────────────────────────────────┤
  │ Single collection, no pagination needed  │ JOIN FETCH                           │
  │ Need pagination + eager child loading    │ @EntityGraph                         │
  │ Global optimization, minimal code change │ @BatchSize                           │
  │ Multiple collections on same entity      │ @BatchSize (avoid Cartesian product) │
  │ Different fetch strategies per use case  │ @EntityGraph (per-method control)    │
  └──────────────────────────────────────────┴──────────────────────────────────────┘
```

```text
How JPA's optimization philosophy changes with each solution:

  DEFAULT (LAZY — JPA's optimization):
  ┌──────────────────────────────────────────────────────────────────┐
  │  "I'll load the MINIMUM. If you need children, ask me later."   │
  │                                                                  │
  │  findAll() → SELECT users only                                   │
  │  user.getAddresses() → SELECT addresses WHERE user_id = ?       │
  │  user.getOrders() → SELECT orders WHERE user_id = ?             │
  │                                                                  │
  │  RESULT: Many small queries. Efficient if you don't need all    │
  │  relationships. Terrible if you do.                              │
  └──────────────────────────────────────────────────────────────────┘

  JOIN FETCH — "Override LAZY, load everything NOW in ONE query":
  ┌──────────────────────────────────────────────────────────────────┐
  │  SELECT u.*, a.* FROM users u                                    │
  │  INNER JOIN addresses a ON u.id = a.user_id                      │
  │                                                                  │
  │  You tell JPA: "I KNOW I need the addresses. Load them now."    │
  │  JPA abandons its lazy strategy for this query.                  │
  │  ONE big query instead of many small ones.                       │
  └──────────────────────────────────────────────────────────────────┘

  @BatchSize — "Keep LAZY, but be smarter about batch loading":
  ┌──────────────────────────────────────────────────────────────────┐
  │  SELECT users → 100 users loaded (addresses still lazy)          │
  │  user[0].getAddresses() triggered →                              │
  │    Instead of: SELECT WHERE user_id = 1 (just this one)         │
  │    Hibernate: SELECT WHERE user_id IN (1,2,...,25) (batch of 25)│
  │                                                                  │
  │  You tell JPA: "Stay lazy, but when you DO load, do it in bulk."│
  │  JPA optimizes the lazy loading itself.                          │
  └──────────────────────────────────────────────────────────────────┘

  @EntityGraph — "Override LAZY for THIS specific method":
  ┌──────────────────────────────────────────────────────────────────┐
  │  @EntityGraph(attributePaths = "userAddressList")                 │
  │  List<UserDetails> findByName(name);                             │
  │                                                                  │
  │  SELECT u.*, a.* FROM users u                                    │
  │  LEFT JOIN addresses a ON u.id = a.user_id                       │
  │  WHERE u.name = ?                                                │
  │                                                                  │
  │  You tell JPA: "For findByName, I need addresses. Fetch them."  │
  │  Other methods (findById, findAll) still use LAZY.               │
  │  Per-method override of fetch strategy.                          │
  └──────────────────────────────────────────────────────────────────┘
```

---


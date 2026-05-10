### Eager and Lazy Loading

**Does the child entity always load with the parent?**

No — it depends on the **fetch type**. The `@OneToOne` annotation has a `fetch` attribute that controls when the child is loaded.

```java
@OneToOne(fetch = FetchType.EAGER)   // child loaded immediately WITH parent (DEFAULT for @OneToOne)
@OneToOne(fetch = FetchType.LAZY)    // child loaded ONLY when accessed
```

---

### What is Eager Loading

With eager loading, when you load the parent entity, Hibernate **immediately** loads the child entity too, in the **same SQL query** (using a JOIN).

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)   // EAGER = default for @OneToOne
    @JoinColumn(name = "address_id")
    private Address address;
}
```

```java
// Load user
User user = userRepository.findById(1L).orElseThrow();
// At this point, user.address is ALREADY loaded — no extra query
System.out.println(user.getAddress().getCity());   // works, no additional SQL
```

```text
SQL generated (single query with LEFT JOIN):

SELECT u.id, u.name, u.address_id,
       a.id, a.city, a.zip_code
FROM users u
LEFT JOIN addresses a ON u.address_id = a.id
WHERE u.id = 1;
```

---

### What is Lazy Loading

With lazy loading, when you load the parent entity, Hibernate does **NOT** load the child entity. It creates a **proxy object** instead. The real data is fetched from the database **only when you access** the child's fields.

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;       // not loaded until accessed
}
```

```java
@Transactional
public void lazyLoadingExample() {
    // SQL: SELECT u.id, u.name, u.address_id FROM users u WHERE u.id = 1
    // (NO JOIN — address is NOT loaded)
    User user = userRepository.findById(1L).orElseThrow();

    // user.address is a Hibernate PROXY at this point (not real Address)
    // Accessing a field on the proxy triggers the second query:
    // SQL: SELECT a.id, a.city, a.zip_code FROM addresses a WHERE a.id = 1
    String city = user.getAddress().getCity();
}
```

```text
Eager loading (1 query):
  SELECT u.*, a.* FROM users u LEFT JOIN addresses a ON u.address_id = a.id WHERE u.id = 1;

Lazy loading (2 queries, second only if accessed):
  Query 1: SELECT u.id, u.name, u.address_id FROM users u WHERE u.id = 1;
  Query 2: SELECT a.id, a.city, a.zip_code FROM addresses a WHERE a.id = 1;   ← only when getAddress().getCity() called
```

---

### Difference Between Eager and Lazy Loading

| | Eager (FetchType.EAGER) | Lazy (FetchType.LAZY) |
|---|---|---|
| **When child loads** | Immediately with parent (same query) | Only when child field is accessed |
| **SQL generated** | JOIN query (1 query) | Separate SELECT for child (2 queries) |
| **Memory usage** | Higher — always loads full graph | Lower — loads only what you need |
| **Performance risk** | Loads unnecessary data, especially for large graphs | N+1 problem if iterating over parent list |
| **Proxy** | No proxy — real object | Hibernate proxy until accessed |
| **Outside transaction** | Always works (data already loaded) | Fails outside transaction (`LazyInitializationException`) |

**Advantages of Eager**

- Simple — child data always available, no extra queries.
- No `LazyInitializationException`.
- Good when you **always** need the child with the parent.

**Disadvantages of Eager**

- Loads child even when you only need the parent.
- If entity graph is deep (User → Address → Country → Continent), all levels load eagerly.
- Wastes memory and DB resources for unused data.

**Advantages of Lazy**

- Only loads data you actually use.
- Better performance for large or complex entity graphs.
- Reduces memory footprint.

**Disadvantages of Lazy**

- Requires an open persistence context (active transaction) to access the child.
- `LazyInitializationException` if accessed outside a transaction.
- N+1 query problem when iterating over a list of parents.

**Use cases**

| Use case | Recommended fetch type |
|---|---|
| User → Profile (almost always needed together) | EAGER |
| User → Address (needed on user detail page only) | LAZY |
| Order → OrderItems (large collection) | LAZY |
| Product → Category (small lookup, always displayed) | EAGER |

---

### Why Eager is Default for @OneToOne and Lazy for @ManyToMany

```text
@OneToOne   → default: FetchType.EAGER
@ManyToOne  → default: FetchType.EAGER
@OneToMany  → default: FetchType.LAZY
@ManyToMany → default: FetchType.LAZY
```

**Why @OneToOne defaults to EAGER**

- `@OneToOne` loads **exactly one** related entity — very low cost.
- In most cases, if you load a User, you also need its Address or Profile.
- One extra JOIN adds negligible overhead.

**Why @ManyToMany defaults to LAZY**

- `@ManyToMany` can load **hundreds or thousands** of related entities.
- Loading all students for a course, or all courses for a student, eagerly would cause massive data transfer.
- Lazy loading prevents this by only fetching the collection when explicitly accessed.

**Can we override it?**

Yes — you override the default by specifying `fetch` explicitly:

```java
// Override @OneToOne EAGER default → make it LAZY
@OneToOne(fetch = FetchType.LAZY)
private Address address;

// Override @ManyToMany LAZY default → make it EAGER (not recommended)
@ManyToMany(fetch = FetchType.EAGER)
private Set<Course> courses;
```

---

### Why Hibernate Uses LEFT JOIN for @OneToOne

Hibernate generates a **LEFT JOIN** (not INNER JOIN) to load the parent and child together.

```text
SELECT u.id, u.name, u.address_id,
       a.id, a.city, a.zip_code
FROM users u
LEFT JOIN addresses a ON u.address_id = a.id
WHERE u.id = 1;
```

**Why LEFT JOIN instead of INNER JOIN?**

- A `LEFT JOIN` ensures the parent row is **always returned**, even if the FK column is `NULL` (no child exists).
- If the `address_id` is `NULL` in the `users` table, an `INNER JOIN` would return **zero rows**, and you could not load the User at all.
- Since JPA allows `@OneToOne` to be optional (nullable FK), LEFT JOIN is the safe choice.

```text
INNER JOIN:
  User(id=1, address_id=NULL)  →  NOT returned (no matching address row)
  User lost!

LEFT JOIN:
  User(id=1, address_id=NULL)  →  Returned with address columns as NULL
  User preserved, address = null in Java
```

**What LEFT JOIN does in eager vs lazy loading**

```text
Eager loading (FetchType.EAGER):
  SELECT u.*, a.* FROM users u LEFT JOIN addresses a ON u.address_id = a.id WHERE u.id = ?
  → Parent AND child data in ONE query. If address_id is NULL, address columns are NULL → user.address = null.

Lazy loading (FetchType.LAZY):
  Query 1: SELECT u.id, u.name, u.address_id FROM users u WHERE u.id = ?
  → Only parent loaded. No JOIN at all.

  Query 2 (triggered on access): SELECT a.* FROM addresses a WHERE a.id = ?
  → Simple SELECT by PK. No LEFT JOIN needed because the FK value is already known.
```

If you make the FK `NOT NULL` (`@JoinColumn(nullable = false)`), Hibernate knows the child always exists and **may** optimize to use `INNER JOIN` instead of `LEFT JOIN`:

```java
@OneToOne(fetch = FetchType.EAGER, optional = false)
@JoinColumn(name = "address_id", nullable = false)
private Address address;
// Hibernate may use INNER JOIN since address is guaranteed to exist
```

---

### Lazy Loading and Jackson Serialization Exception

When you use lazy loading and return the **entity directly** as a REST response (not a DTO), Jackson encounters the Hibernate **proxy** object and fails to serialize it.

**The problem**

```java
@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElseThrow();
        // Returns User entity directly as JSON
    }
}
```

```java
@Entity
public class User {
    @Id
    private Long id;
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;         // Hibernate proxy — not real Address
}
```

```text
What happens:

1. @Transactional (in repository) loads User from DB
2. Transaction commits → persistence context closes → EntityManager closed
3. user.address is still a Hibernate PROXY (not initialized)
4. Controller returns the User object
5. Jackson (HttpMessageConverter) tries to serialize the User to JSON
6. Jackson calls user.getAddress() → triggers proxy initialization
7. But the persistence context is CLOSED → no active Session
8. EXCEPTION:

   com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
   No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor
   
   OR
   
   org.hibernate.LazyInitializationException:
   could not initialize proxy — no Session
```

```text
Timeline:

 @Transactional        Transaction ends       Jackson serializes
      │                     │                       │
  findById()            PC closed              user.getAddress()
  user loaded          address is              proxy init fails!
  address = proxy      still a proxy           → EXCEPTION
      │                     │                       │
  EM open ──────────── EM closed ──────────── no EM available
```

---

**How @JsonIgnore helps (and its limitation)**

`@JsonIgnore` tells Jackson to **skip** the field entirely during serialization.

```java
@Entity
public class User {
    @Id
    private Long id;
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    @JsonIgnore                           // Jackson will skip this field
    private Address address;
}
```

```text
Response JSON:
{
    "id": 1,
    "name": "Alice"
    // address is completely absent — @JsonIgnore skipped it
}
```

**The limitation**: `@JsonIgnore` **always** ignores the field — even when the address IS loaded (e.g., you explicitly fetched it via a JOIN query or accessed it within the transaction). You can never include the address in the response.

```java
@Transactional
public User getUserWithAddress(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    user.getAddress().getCity();   // force initialization inside transaction
    return user;
}
// Even though address IS loaded, @JsonIgnore still excludes it from JSON!
```

---

**Why DTO is the Right Solution (and Recommended)**

A **DTO (Data Transfer Object)** decouples the API response from the JPA entity. You control exactly what data is included.

```java
public class UserDTO {
    private Long id;
    private String name;
    private String city;        // flat field from Address, only what the API needs
    private String zipCode;

    // constructors, getters, setters
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow();

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());

        if (user.getAddress() != null) {
            dto.setCity(user.getAddress().getCity());       // accessed inside transaction → works
            dto.setZipCode(user.getAddress().getZipCode());
        }

        return dto;   // safe to serialize — plain POJO, no proxy
    }
}
```

```java
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/users/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.getUserById(id);   // returns DTO, not entity
    }
}
```

**Why DTO is recommended over returning Entity**

| Concern | Returning Entity | Returning DTO |
|---|---|---|
| Lazy loading exceptions | Risk of `LazyInitializationException` | No proxy — plain POJO |
| API response shape | Coupled to entity structure | Decoupled — API can change independently |
| Security | May expose sensitive fields (password, internal IDs) | Only expose what you choose |
| Hibernate proxy in JSON | `ByteBuddyInterceptor` errors | No proxy — clean serialization |
| Versioning | Entity changes break API | DTO versioned separately |
| Circular references | Infinite recursion (bidirectional) | No cycles — flat structure |

---

### When Does Lazy Loading Actually Fetch the Child

The child entity is fetched **when you access any field** on the proxy object, and the persistence context must still be open.

```java
@Transactional
public void lazyLoadTiming() {
    User user = userRepository.findById(1L).orElseThrow();
    // SQL: SELECT u.id, u.name, u.address_id FROM users u WHERE u.id = 1
    // user.address = Hibernate PROXY (not initialized)

    // This DOES NOT trigger loading — just returns the proxy reference:
    Address addressProxy = user.getAddress();

    // This TRIGGERS the actual SQL:
    String city = addressProxy.getCity();
    // SQL: SELECT a.id, a.city, a.zip_code FROM addresses a WHERE a.id = 1
    // Now the proxy is "initialized" and has real data

    // Subsequent calls do NOT trigger SQL — proxy is already initialized:
    String zip = addressProxy.getZipCode();   // no SQL
}
```

```text
Access sequence:

user.getAddress()              → returns PROXY reference (no SQL)
user.getAddress().getCity()    → TRIGGERS SQL (proxy initialized)
user.getAddress().getZipCode() → uses initialized proxy (no SQL)
user.getAddress().getId()      → may NOT trigger SQL (ID is in the FK column, already known)
```

**Key rules**:
- Accessing `getAddress()` alone returns the proxy — no SQL.
- Accessing any **non-ID field** on the proxy (like `getCity()`) triggers the query.
- Accessing `getId()` on the proxy may **not** trigger a query because the ID is already stored in the parent's FK column.
- Once initialized, subsequent field accesses use the loaded data — no repeated queries.

---

### Why No Exception When Inserting Parent with Child via Lazy Loading

When you **save** (insert) a parent with its child, even with lazy loading, there is no `LazyInitializationException`. Here is why:

```java
@Transactional
public User createUserWithAddress() {
    Address address = new Address();
    address.setCity("Bangalore");
    address.setZipCode("560001");

    User user = new User();
    user.setName("Alice");
    user.setAddress(address);       // setting real Address object (not a proxy)

    return userRepository.save(user);
    // Works fine — no lazy loading exception
}
```

**Why it works**

```text
During INSERT:

1. user.address = actual Address object (you created it with "new")
   It is NOT a Hibernate proxy — it is a real object you built in Java.

2. The persistence context is OPEN (inside @Transactional).

3. CascadeType.ALL → Hibernate persists Address first, gets its ID,
   then persists User with the FK pointing to Address.

4. No proxy involved, no lazy loading triggered.
   The Address object is already in memory — you just created it!
```

```text
During SELECT with lazy loading (where the problem occurs):

1. Hibernate loads User from DB.
2. Hibernate creates a PROXY for Address (does NOT load it).
3. Transaction ends → persistence context closes.
4. Later, you access user.getAddress().getCity().
5. Proxy tries to initialize → NO persistence context → EXCEPTION.
```

**Does the persistence context help?**

Yes — the persistence context is critical. As long as it is open, lazy proxy initialization works:

```text
@Transactional method:
  ├─ Persistence context OPEN
  ├─ Load user (lazy address = proxy)
  ├─ user.getAddress().getCity()     ← proxy initializes → SQL executes → WORKS
  └─ Persistence context CLOSES

Outside @Transactional:
  ├─ Persistence context CLOSED
  ├─ user.getAddress().getCity()     ← proxy tries to initialize → FAILS
  └─ LazyInitializationException
```

During **insert**, you are always inside a `@Transactional` method, and you are passing real objects (not proxies). During **read**, if you exit the transactional boundary before accessing the lazy proxy, you get the exception.

---


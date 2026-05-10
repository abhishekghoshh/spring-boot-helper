### OneToMany Unidirectional Mapping

In a `@OneToMany` unidirectional mapping, one parent entity is associated with **multiple** child entities. The reference exists only in the parent — the child has no knowledge of the parent.

**Real-life use case**: A `User` can place many `Order`s. The User entity has a list of Orders. The Order entity does NOT have a reference back to User.

```text
Java Objects:                              

┌───────────────────────┐                  
│  User                 │                  
│                       │                  
│  id: Long             │    @OneToMany    
│  name: String         │                  
│  orders: List<Order>  ├─────────────────> ┌────────────────────┐
│                       │                   │  Order             │
│                       │                   │                    │
└───────────────────────┘                   │  id: Long          │
                                            │  product: String   │
           1 User  ───────>  N Orders       │  amount: BigDecimal│
           (parent)          (children)     │                    │
                                            │  (NO ref to User)  │
                                            └────────────────────┘

Direction: User ──────> Orders   (one way only)
           User knows its Orders.   Order does NOT know its User.
```

**Code example**

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private BigDecimal amount;

    // NO reference to User — this is unidirectional
    // constructors, getters, setters
}
```

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();

    // constructors, getters, setters
}
```

**Usage**

```java
@Transactional
public User createUserWithOrders() {
    Order order1 = new Order();
    order1.setProduct("Laptop");
    order1.setAmount(new BigDecimal("999.99"));

    Order order2 = new Order();
    order2.setProduct("Mouse");
    order2.setAmount(new BigDecimal("29.99"));

    User user = new User();
    user.setName("Alice");
    user.getOrders().add(order1);
    user.getOrders().add(order2);

    return userRepository.save(user);
}
```

---

### How OneToMany Unidirectional is Stored in DB — The Join Table

By default, when you use `@OneToMany` **without** `@JoinColumn`, Hibernate creates a **third join table** to link the parent and child tables. This is because in a unidirectional `@OneToMany`, the child table has no FK column to the parent — so Hibernate needs a separate table to hold the relationship.

```text
DEFAULT behavior (@OneToMany without @JoinColumn):

┌──────────────┐        ┌────────────────────────┐        ┌──────────────────────┐
│ users         │        │ users_orders            │        │ orders               │
├──────────────┤        │ (AUTO-GENERATED         │        ├──────────────────────┤
│ id (PK)      │───────>│  JOIN TABLE)            │<───────│ id (PK)              │
│ name         │        ├────────────────────────┤        │ product              │
│              │        │ user_id (FK) ──> users  │        │ amount               │
│              │        │ orders_id (FK) ──> orders│        │                      │
│              │        │                        │        │ (NO user_id here!)    │
└──────────────┘        └────────────────────────┘        └──────────────────────┘
```

**Generated DDL (3 tables)**

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product VARCHAR(255),
    amount DECIMAL(19,2)
);

-- Hibernate auto-creates this join table:
CREATE TABLE users_orders (
    user_id BIGINT NOT NULL,
    orders_id BIGINT NOT NULL UNIQUE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (orders_id) REFERENCES orders(id)
);
```

**SQL generated when saving User with Orders**

```text
INSERT INTO orders (product, amount) VALUES ('Laptop', 999.99);          ← Order 1 saved
INSERT INTO orders (product, amount) VALUES ('Mouse', 29.99);            ← Order 2 saved
INSERT INTO users (name) VALUES ('Alice');                                ← User saved
INSERT INTO users_orders (user_id, orders_id) VALUES (1, 1);             ← link User to Order 1
INSERT INTO users_orders (user_id, orders_id) VALUES (1, 2);             ← link User to Order 2
```

**5 SQL statements** for a simple save — this is **inefficient**.

```text
Why does Hibernate create a join table by default?

1. @OneToMany is on the PARENT (User).
2. The CHILD (Order) has NO reference to User and NO FK column.
3. Hibernate cannot add a FK to Order because Order doesn't "know" about User.
4. So Hibernate creates a middle table to store the relationship.
```

---

### @JoinColumn — Avoiding the Join Table (Industry Standard)

By adding `@JoinColumn` to the `@OneToMany`, you tell Hibernate: **"Put the foreign key column directly in the child table. Do NOT create a join table."**

```text
WITH @JoinColumn (@OneToMany + @JoinColumn):

┌──────────────┐                              ┌──────────────────────┐
│ users         │                              │ orders               │
├──────────────┤                              ├──────────────────────┤
│ id (PK)      │──────────────────────────────>│ id (PK)              │
│ name         │                              │ product              │
│              │                              │ amount               │
│              │                              │ user_id (FK)  ←──────│── FK added to child table
│              │                              │                      │
└──────────────┘                              └──────────────────────┘

NO join table!   FK lives in the CHILD table (orders).
Only 2 tables.
```

**Code example**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")                // FK "user_id" goes into the orders table
    private List<Order> orders = new ArrayList<>();
}
```

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private BigDecimal amount;

    // Still NO reference to User — still unidirectional
    // The "user_id" column is managed by Hibernate, not by Order entity
}
```

**Generated DDL (2 tables only)**

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product VARCHAR(255),
    amount DECIMAL(19,2),
    user_id BIGINT,                                -- FK added by @JoinColumn
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**SQL generated when saving**

```text
INSERT INTO users (name) VALUES ('Alice');                                       ← User saved first
INSERT INTO orders (product, amount) VALUES ('Laptop', 999.99);                  ← Order 1 saved (user_id is NULL initially)
INSERT INTO orders (product, amount) VALUES ('Mouse', 29.99);                    ← Order 2 saved (user_id is NULL initially)
UPDATE orders SET user_id = 1 WHERE id = 1;                                      ← FK set via UPDATE
UPDATE orders SET user_id = 1 WHERE id = 2;                                      ← FK set via UPDATE
```

Note: Hibernate inserts the child rows with `user_id = NULL` first, then issues **UPDATE** statements to set the FK. This is because in unidirectional `@OneToMany`, the child entity does not know about the FK — the parent manages it.

**Is @JoinColumn the industry standard?**

Yes. Using `@JoinColumn` with `@OneToMany` is the **standard approach** in production code.

```text
Without @JoinColumn:
  - 3 tables (extra join table)
  - 5+ SQL statements for a simple save
  - Extra JOINs needed for every query
  - Harder to understand schema

With @JoinColumn:
  - 2 tables (clean, normalized schema)
  - Fewer SQL statements
  - Standard FK relationship
  - Simpler queries
```

**Will it make queries faster?**

```text
Without @JoinColumn (join table):
  SELECT u.*, o.*
  FROM users u
  JOIN users_orders uo ON u.id = uo.user_id       ← extra join through middle table
  JOIN orders o ON uo.orders_id = o.id
  WHERE u.id = 1;

With @JoinColumn (direct FK):
  SELECT u.*, o.*
  FROM users u
  LEFT JOIN orders o ON u.id = o.user_id           ← direct join, one less table
  WHERE u.id = 1;

Direct FK = one fewer JOIN = faster queries, simpler execution plan.
```

---

### Lazy Loading in OneToMany (Default)

`@OneToMany` defaults to `FetchType.LAZY`. The child collection is **not loaded** when the parent is fetched. It is loaded only when you **access** the collection.

**Entity**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)   // LAZY is default for @OneToMany
    @JoinColumn(name = "user_id")
    private List<Order> orders = new ArrayList<>();
}
```

**Controller + Service**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
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
        // SQL #1: SELECT u.id, u.name FROM users u WHERE u.id = 1
        // At this point, user.orders is a Hibernate PersistentBag PROXY (not loaded)

        List<OrderDTO> orderDTOs = user.getOrders().stream()     // ← TRIGGERS SQL #2
            .map(o -> new OrderDTO(o.getId(), o.getProduct(), o.getAmount()))
            .toList();
        // SQL #2: SELECT o.id, o.product, o.amount, o.user_id FROM orders o WHERE o.user_id = 1
        // Orders loaded NOW because we accessed the collection

        return new UserDTO(user.getId(), user.getName(), orderDTOs);
    }
}
```

```text
SQL generated (lazy loading):

Query 1 (load parent):
  SELECT u.id, u.name FROM users u WHERE u.id = 1;

Query 2 (load children — triggered by user.getOrders()):
  SELECT o.id, o.product, o.amount, o.user_id
  FROM orders o
  WHERE o.user_id = 1;

Total: 2 queries (second one only if you access the orders)
```

```text
Timeline:

findById(1L)                    user.getOrders().stream()
     │                                   │
     v                                   v
SELECT users ...              SELECT orders WHERE user_id = 1
(orders = PROXY)              (orders loaded into real list)
     │                                   │
     └───── orders NOT loaded ───────────┘
                                    loaded here
```

---

### Returning Entity Directly — Jackson Exception

If you return the **entity** directly as a REST response with lazy loading, you **will** get a Jackson serialization exception for `@OneToMany`:

```java
// DANGEROUS — returning entity directly
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
    // Transaction ends here (if no @Transactional on controller)
    // user.orders = Hibernate PersistentBag proxy (not initialized)
    // Jackson tries to serialize user.orders → EXCEPTION
}
```

```text
What happens:

1. Repository loads User inside its own transaction
2. Transaction commits → persistence context closes
3. user.orders = PersistentBag proxy (NOT initialized)
4. Jackson tries to call user.getOrders() to serialize the list
5. Proxy tries to initialize → NO active Session
6. EXCEPTION:

   com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
   No serializer found for class org.hibernate.collection.spi.PersistentBag

   OR

   org.hibernate.LazyInitializationException:
   failed to lazily initialize a collection of role: User.orders — could not initialize proxy — no Session
```

**Will the child entities be loaded automatically?** No. Unlike `@OneToOne` with eager default, `@OneToMany` defaults to LAZY. Jackson will NOT trigger the load — it will fail because the session is closed.

**With OSIV (Open Session In View) enabled** (`spring.jpa.open-in-view=true`, the default): Jackson CAN trigger the lazy load because the EntityManager is still open during view rendering. But this is considered a **bad practice** because:
- It hides the N+1 problem.
- It couples the view layer to the persistence layer.
- It keeps the database connection open longer than necessary.

**The right solution is to use DTOs** (as shown in the lazy loading example above).

---

### Eager Loading in OneToMany

With eager loading, Hibernate loads the parent **and all children** in a single JOIN query when the parent is fetched.

**Entity**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)   // EAGER — load children immediately
    @JoinColumn(name = "user_id")
    private List<Order> orders = new ArrayList<>();
}
```

**Controller + Service**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
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
        // SQL: SELECT u.*, o.* FROM users u LEFT JOIN orders o ON u.id = o.user_id WHERE u.id = 1
        // Orders are ALREADY loaded — no proxy, real list

        List<OrderDTO> orderDTOs = user.getOrders().stream()     // NO extra SQL — already loaded
            .map(o -> new OrderDTO(o.getId(), o.getProduct(), o.getAmount()))
            .toList();

        return new UserDTO(user.getId(), user.getName(), orderDTOs);
    }
}
```

```text
SQL generated (eager loading — single query):

SELECT u.id, u.name,
       o.id, o.product, o.amount, o.user_id
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.id = 1;

Total: 1 query (all data loaded at once)
```

```text
Timeline:

findById(1L)
     │
     v
SELECT users LEFT JOIN orders ...
(user + ALL orders loaded in one shot)
     │
     └───── orders are REAL objects, not proxies
            no second query needed
```

**Why EAGER is dangerous for @OneToMany**

```text
Scenario: findAll() with EAGER @OneToMany

userRepository.findAll();
→ SELECT * FROM users;                                    ← loads 100 users
→ For EACH user: SELECT * FROM orders WHERE user_id = ?   ← 100 more queries!

Total: 101 queries (N+1 problem)
This is why @OneToMany defaults to LAZY.
```

Eager loading is only safe for `@OneToMany` when:
- You always need the children with the parent.
- The collection is small and bounded.
- You are loading a single parent entity (not a list).

---

### Cascade Types in OneToMany Unidirectional

**CascadeType.PERSIST — Saving parent auto-saves children**

```java
@OneToMany(cascade = CascadeType.PERSIST)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public User createUserWithOrders() {
    Order order1 = new Order("Laptop", new BigDecimal("999.99"));
    Order order2 = new Order("Mouse", new BigDecimal("29.99"));

    User user = new User("Alice");
    user.getOrders().add(order1);
    user.getOrders().add(order2);

    return userRepository.save(user);   // saves User AND both Orders
}
```

```text
SQL:
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount) VALUES ('Laptop', 999.99);
  INSERT INTO orders (product, amount) VALUES ('Mouse', 29.99);
  UPDATE orders SET user_id = 1 WHERE id = 1;
  UPDATE orders SET user_id = 1 WHERE id = 2;
```

```text
persist(user)
     │
     ├─ INSERT User
     ├─ CASCADE PERSIST → INSERT Order 1
     ├─ CASCADE PERSIST → INSERT Order 2
     └─ UPDATE Orders to set FK
```

---

**CascadeType.MERGE — Updating parent auto-updates children**

```java
@OneToMany(cascade = CascadeType.MERGE)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public User updateUserAndOrders(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    user.setName("Bob");
    user.getOrders().get(0).setProduct("Gaming Laptop");   // update first order

    return userRepository.save(user);
}
```

```text
SQL:
  UPDATE users SET name = 'Bob' WHERE id = 1;
  UPDATE orders SET product = 'Gaming Laptop' WHERE id = 1;
```

```text
merge(user)
     │
     ├─ UPDATE User (name changed)
     └─ CASCADE MERGE → UPDATE Order 1 (product changed)
```

---

**CascadeType.REMOVE — Deleting parent auto-deletes children**

```java
@OneToMany(cascade = CascadeType.REMOVE)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public void deleteUser(Long userId) {
    userRepository.deleteById(userId);
}
```

```text
SQL:
  SELECT u.* FROM users u WHERE u.id = 1;                    ← load user
  SELECT o.* FROM orders o WHERE o.user_id = 1;              ← load orders (for cascade)
  DELETE FROM orders WHERE id = 1;                            ← cascade delete order 1
  DELETE FROM orders WHERE id = 2;                            ← cascade delete order 2
  DELETE FROM users WHERE id = 1;                             ← delete user
```

```text
remove(user)
     │
     ├─ CASCADE REMOVE → DELETE Order 1
     ├─ CASCADE REMOVE → DELETE Order 2
     └─ DELETE User
```

---

**CascadeType.REFRESH — Refreshing parent auto-refreshes children**

```java
@OneToMany(cascade = CascadeType.REFRESH)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public void refreshUser(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();

    // Someone changed the order amount directly in DB to 1500.00
    user.getOrders().get(0).setAmount(new BigDecimal("0.00"));  // in-memory change

    entityManager.refresh(user);
    // Hibernate re-reads user AND all orders from DB
    // in-memory change is DISCARDED
    // order.amount is back to 1500.00 (from DB)
}
```

```text
SQL:
  SELECT u.* FROM users u WHERE u.id = 1;                    ← refresh user
  SELECT o.* FROM orders o WHERE o.user_id = 1;              ← cascade refresh all orders
```

---

**CascadeType.DETACH — Detaching parent auto-detaches children**

```java
@OneToMany(cascade = CascadeType.DETACH)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public void detachExample(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    user.getOrders().size();    // initialize lazy collection

    entityManager.detach(user);
    // user AND all orders are now DETACHED
    // changes to user or orders will NOT be auto-detected by dirty checking

    user.setName("Changed");                           // NOT tracked
    user.getOrders().get(0).setProduct("Changed");     // NOT tracked
    // No UPDATE SQL will be generated
}
```

---

**CascadeType.ALL — All operations cascaded (most common for owned collections)**

```java
@OneToMany(cascade = CascadeType.ALL)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

Equivalent to `{PERSIST, MERGE, REMOVE, REFRESH, DETACH}`.

```text
Cascade usage summary for @OneToMany:

┌──────────────────┬────────────────────────────────────────────────────┐
│ Cascade Type     │ Propagates                                        │
├──────────────────┼────────────────────────────────────────────────────┤
│ PERSIST          │ save(parent) → saves all children                 │
│ MERGE            │ save(existing parent) → updates all children      │
│ REMOVE           │ delete(parent) → deletes all children             │
│ REFRESH          │ refresh(parent) → re-reads all children from DB   │
│ DETACH           │ detach(parent) → detaches all children            │
│ ALL              │ All of the above                                  │
└──────────────────┴────────────────────────────────────────────────────┘

Industry standard for @OneToMany:
  - Owned collections (User → Orders):     CascadeType.ALL
  - Shared references (Order → Product):   NO cascade or PERSIST + MERGE only
```

---

### Orphan Child Problem — When Updating the Child List

An **orphan** is a child entity that was **removed from the parent's collection** but still exists in the database. This happens when you update the parent's list by replacing or removing children — the old children are disconnected from the parent but NOT deleted.

**The problem**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private List<Order> orders = new ArrayList<>();
}
```

**API + Service that causes orphans**

```java
// DTO for the update request
public class UpdateUserOrdersRequest {
    private String name;
    private List<OrderDTO> orders;    // new list of orders from the client
}
```

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/{id}/orders")
    public UserDTO updateUserOrders(@PathVariable Long id,
                                     @RequestBody UpdateUserOrdersRequest request) {
        return userService.updateUserOrders(id, request);
    }
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserDTO updateUserOrders(Long userId, UpdateUserOrdersRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        // user.orders = [Order(id=1, "Laptop"), Order(id=2, "Mouse")]

        // Client sends a NEW list of orders — replaces old ones
        user.getOrders().clear();    // remove old orders from the collection

        for (OrderDTO dto : request.getOrders()) {
            Order newOrder = new Order(dto.getProduct(), dto.getAmount());
            user.getOrders().add(newOrder);
        }

        userRepository.save(user);
        // ...
    }
}
```

```text
What happens WITHOUT orphanRemoval:

Before update:
┌──────────────┐      ┌──────────────────────────────┐
│ users         │      │ orders                       │
├──────────────┤      ├──────────────────────────────┤
│ id=1 "Alice" │      │ id=1, "Laptop", user_id=1    │
│              │      │ id=2, "Mouse",  user_id=1    │
└──────────────┘      └──────────────────────────────┘

After user.getOrders().clear() + add new orders:
┌──────────────┐      ┌──────────────────────────────┐
│ users         │      │ orders                       │
├──────────────┤      ├──────────────────────────────┤
│ id=1 "Alice" │      │ id=1, "Laptop", user_id=NULL │ ← ORPHAN! (FK set to NULL, row NOT deleted)
│              │      │ id=2, "Mouse",  user_id=NULL │ ← ORPHAN! (FK set to NULL, row NOT deleted)
│              │      │ id=3, "Keyboard", user_id=1  │ ← new order
│              │      │ id=4, "Monitor",  user_id=1  │ ← new order
└──────────────┘      └──────────────────────────────┘
```

**SQL generated (orphans created)**

```text
UPDATE orders SET user_id = NULL WHERE id = 1;     ← FK set to NULL (not deleted!)
UPDATE orders SET user_id = NULL WHERE id = 2;     ← FK set to NULL (not deleted!)
INSERT INTO orders (product, amount) VALUES ('Keyboard', 49.99);
INSERT INTO orders (product, amount) VALUES ('Monitor', 299.99);
UPDATE orders SET user_id = 1 WHERE id = 3;
UPDATE orders SET user_id = 1 WHERE id = 4;
```

The old orders (id=1, id=2) are now **orphans** — they exist in the database with `user_id = NULL`, not connected to any user, wasting space and causing data inconsistency.

---

### Solving with orphanRemoval = true

`orphanRemoval = true` tells Hibernate: **"If a child entity is removed from the parent's collection, DELETE it from the database."**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)   // ← orphanRemoval added
    @JoinColumn(name = "user_id")
    private List<Order> orders = new ArrayList<>();
}
```

**Same service code, different result**

```java
@Transactional
public UserDTO updateUserOrders(Long userId, UpdateUserOrdersRequest request) {
    User user = userRepository.findById(userId).orElseThrow();
    // user.orders = [Order(id=1, "Laptop"), Order(id=2, "Mouse")]

    user.getOrders().clear();    // remove old orders from the collection

    for (OrderDTO dto : request.getOrders()) {
        Order newOrder = new Order(dto.getProduct(), dto.getAmount());
        user.getOrders().add(newOrder);
    }

    userRepository.save(user);
    // Now orphans are DELETED, not just disconnected
}
```

```text
What happens WITH orphanRemoval = true:

Before update:
┌──────────────┐      ┌──────────────────────────────┐
│ users         │      │ orders                       │
├──────────────┤      ├──────────────────────────────┤
│ id=1 "Alice" │      │ id=1, "Laptop", user_id=1    │
│              │      │ id=2, "Mouse",  user_id=1    │
└──────────────┘      └──────────────────────────────┘

After user.getOrders().clear() + add new orders:
┌──────────────┐      ┌──────────────────────────────┐
│ users         │      │ orders                       │
├──────────────┤      ├──────────────────────────────┤
│ id=1 "Alice" │      │ id=3, "Keyboard", user_id=1  │ ← new order
│              │      │ id=4, "Monitor",  user_id=1  │ ← new order
└──────────────┘      └──────────────────────────────┘

Orders id=1 and id=2 are DELETED from the database — no orphans!
```

**SQL generated (orphans deleted)**

```text
DELETE FROM orders WHERE id = 1;                       ← orphan removed!
DELETE FROM orders WHERE id = 2;                       ← orphan removed!
INSERT INTO orders (product, amount) VALUES ('Keyboard', 49.99);
INSERT INTO orders (product, amount) VALUES ('Monitor', 299.99);
UPDATE orders SET user_id = 1 WHERE id = 3;
UPDATE orders SET user_id = 1 WHERE id = 4;
```

```text
Comparison:

WITHOUT orphanRemoval:
  user.getOrders().clear()  →  UPDATE orders SET user_id = NULL   (orphans remain)

WITH orphanRemoval = true:
  user.getOrders().clear()  →  DELETE FROM orders WHERE id = ?    (orphans deleted)
```

**When does orphanRemoval trigger?**

```text
user.getOrders().remove(order1);        → DELETE order1 from DB
user.getOrders().clear();               → DELETE ALL orders from DB
user.setOrders(newList);                → DELETE old orders, INSERT new ones

It triggers whenever a child is REMOVED from the parent's collection,
regardless of whether the parent is being deleted.
```

---

### Orphan Removal vs Cascade Delete

These are **different operations** that solve different problems.

```text
┌─────────────────────────┬───────────────────────────────────────────────────────┐
│                         │ What triggers deletion                               │
├─────────────────────────┼───────────────────────────────────────────────────────┤
│ CascadeType.REMOVE      │ Parent entity is DELETED                             │
│                         │ → all children are also deleted                      │
│                         │                                                      │
│ orphanRemoval = true    │ Child is REMOVED FROM THE COLLECTION                 │
│                         │ → that specific child is deleted from DB             │
│                         │ (parent is NOT deleted — it still exists)            │
└─────────────────────────┴───────────────────────────────────────────────────────┘
```

**CascadeType.REMOVE scenario — parent is deleted**

```java
@OneToMany(cascade = CascadeType.REMOVE)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
userRepository.deleteById(1L);
// DELETE FROM orders WHERE user_id = 1;     ← all orders deleted
// DELETE FROM users WHERE id = 1;           ← user deleted
```

```text
CASCADE REMOVE:
  Delete parent → children are also deleted

  user deleted
     │
     ├─ Order 1 deleted  (because parent was deleted)
     └─ Order 2 deleted  (because parent was deleted)
```

**orphanRemoval scenario — child is removed from collection, parent still exists**

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public void removeOneOrder(Long userId, Long orderId) {
    User user = userRepository.findById(userId).orElseThrow();
    user.getOrders().removeIf(o -> o.getId().equals(orderId));
    // orphanRemoval triggers: DELETE FROM orders WHERE id = ?
    // User is NOT deleted — only the removed order is deleted
}
```

```text
ORPHAN REMOVAL:
  Remove child from collection → only that child is deleted
  Parent still exists

  user still exists
     │
     ├─ Order 1 removed from list → DELETED from DB
     └─ Order 2 still in list → still in DB
```

**Side-by-side comparison**

| | CascadeType.REMOVE | orphanRemoval = true |
|---|---|---|
| **Trigger** | `delete(parent)` | `collection.remove(child)` or `collection.clear()` |
| **Parent deleted?** | Yes | No — parent still exists |
| **Which children deleted?** | ALL children | Only the removed child(ren) |
| **Use case** | Delete user → delete all their orders | Update user's orders → remove old, add new |
| **Without the other** | Cannot clean orphans on collection update | Does not auto-delete children when parent is deleted |
| **Together** | `cascade = ALL, orphanRemoval = true` covers both scenarios |

**Best practice for owned @OneToMany collections**

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```text
cascade = CascadeType.ALL    → handles: save, update, delete parent → propagates to children
orphanRemoval = true         → handles: remove child from collection → deletes it from DB

Together they cover ALL lifecycle scenarios:
  ✅ Save parent with children
  ✅ Update parent and children
  ✅ Delete parent → children deleted
  ✅ Remove child from list → child deleted from DB (no orphans)
```


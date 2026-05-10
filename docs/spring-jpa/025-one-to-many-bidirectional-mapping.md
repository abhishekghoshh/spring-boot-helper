### OneToMany Bidirectional Mapping

In a bidirectional `@OneToMany` / `@ManyToOne` mapping, **both** the parent and child entities have references to each other. The parent has a collection of children, and each child has a reference back to the parent.

**Real-life use case**: A `User` has many `Order`s (parent → children). Each `Order` belongs to one `User` (child → parent). You can navigate both directions: get all orders for a user, or get the user who placed an order.

```text
Java Objects:                              Database Tables:

┌───────────────────────┐                  ┌──────────────────────────┐
│  User (PARENT)        │                  │  users                   │
│                       │                  ├──────────────────────────┤
│  id: Long             │  @OneToMany      │  id (PK, BIGINT)         │
│  name: String         │  (mappedBy)      │  name (VARCHAR)          │
│  orders: List<Order>  ├────────────>     │                          │
│                       │                  └──────────────────────────┘
│                       │<────────────┐
└───────────────────────┘             │
                                      │    ┌──────────────────────────┐
┌───────────────────────┐             │    │  orders                  │
│  Order (CHILD)        │             │    ├──────────────────────────┤
│                       │  @ManyToOne │    │  id (PK, BIGINT)         │
│  id: Long             │  @JoinColumn│    │  product (VARCHAR)       │
│  product: String      │             │    │  amount (DECIMAL)        │
│  amount: BigDecimal   ├─────────────┘    │  user_id (FK, BIGINT) ───┼──> users.id
│  user: User           │                  │                          │
│                       │                  │  FK lives in CHILD table │
└───────────────────────┘                  └──────────────────────────┘

Direction: User <──────────> Order   (both ways)
           User knows its Orders.   Order knows its User.
```

---

### Owning Side vs Inverse Side

In every bidirectional relationship, JPA requires you to define:

- **Owning side**: The entity whose table holds the **foreign key**. This side **controls** the relationship — JPA reads the owning side to decide what SQL to generate.
- **Inverse side**: The entity that **mirrors** the relationship using `mappedBy`. It does NOT control the FK — it is just a convenience for navigation.

**Is the owning side always the parent?**

**No — it is the opposite.** In a `@OneToMany` / `@ManyToOne` bidirectional mapping, the **child** (the `@ManyToOne` side) is **always the owning side** because the FK naturally lives in the child table.

```text
┌────────────────────────────────┐        ┌────────────────────────────────┐
│  User (INVERSE SIDE)           │        │  Order (OWNING SIDE)           │
│                                │        │                                │
│  @OneToMany(mappedBy = "user") │        │  @ManyToOne                    │
│  private List<Order> orders;   │        │  @JoinColumn(name = "user_id") │
│                                │        │  private User user;            │
│  "I'm just the mirror.        │        │                                │
│   I do NOT control the FK."   │        │  "I control the FK.            │
│                                │        │   user_id is in MY table."     │
│  → NO @JoinColumn here        │        │  → @JoinColumn here            │
└────────────────────────────────┘        └────────────────────────────────┘
```

```text
Why the child is the owning side:

In relational databases, a "many" side always holds the FK:
  - One User has many Orders
  - The FK (user_id) goes in the Orders table
  - Each Order row points to its User via user_id
  - You can't put a "list of order IDs" inside a single user row

Therefore:
  Child table (orders) has the FK → Child entity is the owning side
  Parent table (users) has NO FK → Parent entity is the inverse side
```

**Which side contains the FK relationship?** The **child** (owning side).

**Where do we put @JoinColumn?** On the **child** entity (the `@ManyToOne` side).

---

### Child Entity Holds the Foreign Key by Default

In a bidirectional `@OneToMany` / `@ManyToOne`, the FK **always** lives in the child table. This is a fundamental rule of relational databases — the "many" side holds the FK to the "one" side.

**Code example**

```java
// CHILD — OWNING SIDE (holds the FK)
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")                // FK column in orders table
    private User user;                            // reference to parent

    // constructors, getters, setters
}
```

```java
// PARENT — INVERSE SIDE (mirrors the relationship)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // constructors, getters, setters
}
```

**Generated DDL**

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product VARCHAR(255),
    amount DECIMAL(19,2),
    user_id BIGINT,                                -- FK in CHILD table
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

```text
DB Tables:

┌──────────────────┐                    ┌──────────────────────────────┐
│ users             │                    │ orders                       │
├──────────────────┤                    ├──────────────────────────────┤
│ id=1, "Alice"    │◄───────────────────│ id=1, "Laptop", user_id=1   │
│                  │◄───────────────────│ id=2, "Mouse",  user_id=1   │
│                  │                    │                              │
│ id=2, "Bob"      │◄───────────────────│ id=3, "Phone",  user_id=2   │
└──────────────────┘                    └──────────────────────────────┘

FK (user_id) is in the orders table.
No FK in the users table.
Only 2 tables — no join table needed.
```

**SQL for saving**

```java
@Transactional
public User createUserWithOrders() {
    User user = new User();
    user.setName("Alice");

    Order order1 = new Order();
    order1.setProduct("Laptop");
    order1.setAmount(new BigDecimal("999.99"));
    order1.setUser(user);                     // ← SET PARENT IN CHILD (critical!)

    Order order2 = new Order();
    order2.setProduct("Mouse");
    order2.setAmount(new BigDecimal("29.99"));
    order2.setUser(user);                     // ← SET PARENT IN CHILD (critical!)

    user.getOrders().add(order1);
    user.getOrders().add(order2);

    return userRepository.save(user);
}
```

```text
SQL generated:
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, 1);    ← FK set directly!
  INSERT INTO orders (product, amount, user_id) VALUES ('Mouse', 29.99, 1);      ← FK set directly!
```

Notice: Unlike unidirectional `@OneToMany` (which does INSERT with NULL FK + UPDATE), bidirectional sets the FK **directly** in the INSERT — more efficient!

---

### mappedBy on the @OneToMany (Inverse/Parent Side)

`mappedBy` tells JPA: "I am NOT the owner of this relationship. The owner is the field named in `mappedBy` on the other entity. Do NOT create a FK or join table for me."

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> orders = new ArrayList<>();
```

The value `"user"` refers to the **field name** in the `Order` entity:

```java
// In Order.java
@ManyToOne
@JoinColumn(name = "user_id")
private User user;              // ← this is the field "user" that mappedBy refers to
```

```text
@OneToMany(mappedBy = "user")  ─── refers to ──→  Order.user field
                                                        │
                                                  @ManyToOne
                                                  @JoinColumn(name = "user_id")
                                                  The REAL FK owner
```

**What happens WITHOUT mappedBy?**

```text
WITHOUT mappedBy on @OneToMany:

@Entity User:
    @OneToMany(cascade = CascadeType.ALL)
    private List<Order> orders;

@Entity Order:
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

Result: JPA sees TWO independent relationships!
  1. User → Orders  (creates a users_orders join table or separate FK management)
  2. Order → User   (creates user_id FK in orders table)

  ┌──────────────┐     ┌──────────────────────┐     ┌─────────────────────┐
  │ users         │     │ users_orders          │     │ orders              │
  │ id (PK)      │────>│ user_id, orders_id    │<────│ id (PK)             │
  │ name         │     │                      │     │ product             │
  │              │     └──────────────────────┘     │ amount              │
  │              │                                  │ user_id (FK) ───────│──> users.id
  └──────────────┘                                  └─────────────────────┘

  3 tables!  Duplicate relationship!  Extra INSERTs and UPDATEs!
```

```text
WITH mappedBy = "user" on @OneToMany:

@Entity User:
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;

@Entity Order:
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

Result: JPA sees ONE relationship managed by Order.user:
  ┌──────────────┐                              ┌─────────────────────┐
  │ users         │                              │ orders              │
  │ id (PK)      │                              │ id (PK)             │
  │ name         │                              │ product             │
  │              │                              │ amount              │
  │              │◄──────────────────────────────│ user_id (FK)        │
  └──────────────┘                              └─────────────────────┘

  2 tables!  Single FK!  Clean and efficient!
```

**Generated SQL comparison**

```text
WITHOUT mappedBy (3 tables, extra SQL):
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, 1);
  INSERT INTO users_orders (user_id, orders_id) VALUES (1, 1);     ← unnecessary!

WITH mappedBy (2 tables, clean SQL):
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, 1);
  (no join table inserts)
```

---

### What Happens If You Only Set the Child List on the Parent (Not the Parent on Child)

Since JPA **only reads the owning side** (`Order.user`) to manage the FK, if you only add Orders to User's list but do NOT set `order.setUser(user)`, the FK column will be **NULL**.

**The problem**

```java
@Transactional
public User createUserWithOrders_WRONG() {
    User user = new User();
    user.setName("Alice");

    Order order1 = new Order();
    order1.setProduct("Laptop");
    order1.setAmount(new BigDecimal("999.99"));
    // order1.setUser(user);     ← NOT SET! Missing!

    Order order2 = new Order();
    order2.setProduct("Mouse");
    order2.setAmount(new BigDecimal("29.99"));
    // order2.setUser(user);     ← NOT SET! Missing!

    user.getOrders().add(order1);      // only adding to parent's list
    user.getOrders().add(order2);      // only adding to parent's list

    return userRepository.save(user);
}
```

```text
SQL generated:
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, NULL);    ← user_id is NULL!
  INSERT INTO orders (product, amount, user_id) VALUES ('Mouse', 29.99, NULL);      ← user_id is NULL!
```

```text
DB after save:

┌──────────────────┐                    ┌──────────────────────────────────┐
│ users             │                    │ orders                           │
├──────────────────┤                    ├──────────────────────────────────┤
│ id=1, "Alice"    │    NO connection!  │ id=1, "Laptop", user_id=NULL    │ ← orphan!
│                  │                    │ id=2, "Mouse",  user_id=NULL    │ ← orphan!
└──────────────────┘                    └──────────────────────────────────┘

The orders exist in the DB but have NO connection to the user!
```

**Why does this happen?**

```text
JPA relationship management rules:

1. JPA only looks at the OWNING SIDE to determine FK values.
2. The owning side is Order.user (annotated with @ManyToOne + @JoinColumn).
3. We never called order.setUser(user) → Order.user = null → user_id = NULL.
4. Adding orders to user.getOrders() is for Java navigation only.
   JPA IGNORES the inverse side (User.orders) when generating INSERT SQL.

  User.orders = [order1, order2]    ← JPA ignores this for FK
  Order.user = null                 ← JPA reads THIS for FK → NULL
```

**The correct way — always set BOTH sides**

```java
@Transactional
public User createUserWithOrders_CORRECT() {
    User user = new User();
    user.setName("Alice");

    Order order1 = new Order();
    order1.setProduct("Laptop");
    order1.setAmount(new BigDecimal("999.99"));
    order1.setUser(user);                     // ← SET owning side (controls FK)

    Order order2 = new Order();
    order2.setProduct("Mouse");
    order2.setAmount(new BigDecimal("29.99"));
    order2.setUser(user);                     // ← SET owning side (controls FK)

    user.getOrders().add(order1);             // ← SET inverse side (for Java navigation)
    user.getOrders().add(order2);             // ← SET inverse side (for Java navigation)

    return userRepository.save(user);
}
```

```text
SQL generated:
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, 1);    ← FK set correctly!
  INSERT INTO orders (product, amount, user_id) VALUES ('Mouse', 29.99, 1);      ← FK set correctly!
```

**Best practice — add a helper method to the parent to keep both sides in sync**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // Helper method — sets BOTH sides of the relationship
    public void addOrder(Order order) {
        orders.add(order);          // inverse side (Java navigation)
        order.setUser(this);        // owning side (controls FK)
    }

    public void removeOrder(Order order) {
        orders.remove(order);       // inverse side
        order.setUser(null);        // owning side (nullifies FK)
    }
}
```

```java
// Now usage is simple and safe:
User user = new User();
user.setName("Alice");
user.addOrder(order1);    // sets both sides automatically
user.addOrder(order2);    // sets both sides automatically
userRepository.save(user);
```

---

### @JsonIgnore on the Parent Reference in Child Entity

Yes — you **should** put `@JsonIgnore` on the parent reference in the child entity to prevent infinite recursion when serializing bidirectional relationships.

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore                           // ← prevents infinite recursion
    private User user;
}
```

**Why it is needed**

Without `@JsonIgnore`, Jackson serializes User → orders → [Order → user → User → orders → ...] infinitely:

```text
WITHOUT @JsonIgnore:

{
  "id": 1,
  "name": "Alice",
  "orders": [
    {
      "id": 1,
      "product": "Laptop",
      "user": {                          ← serializes User AGAIN
        "id": 1,
        "name": "Alice",
        "orders": [                      ← serializes orders AGAIN
          {
            "user": {                    ← INFINITE LOOP → StackOverflowError
              ...
```

```text
WITH @JsonIgnore on Order.user:

{
  "id": 1,
  "name": "Alice",
  "orders": [
    {
      "id": 1,
      "product": "Laptop",
      "amount": 999.99
    },
    {
      "id": 2,
      "product": "Mouse",
      "amount": 29.99
    }
  ]
}

Clean response — no infinite recursion.
"user" field in each Order is skipped by Jackson.
```

**Limitation**: `@JsonIgnore` **always** hides the `user` field in Order's JSON. If you need to show the user when fetching an Order individually, you have the same alternatives as `@OneToOne`:

| Solution | Behavior |
|---|---|
| `@JsonIgnore` on `Order.user` | Always hides user in Order JSON. Simplest. |
| `@JsonManagedReference` on `User.orders` + `@JsonBackReference` on `Order.user` | User shows orders, Order hides user. |
| `@JsonIdentityInfo` on both entities | Both directions visible, second occurrence replaced with ID. |
| **DTO (recommended)** | Full control over response shape. No annotations on entities. |

**In practice**, for `@OneToMany` bidirectional, using `@JsonIgnore` on the child's parent reference is the most common approach because:
- When fetching User, you want orders included → User.orders is serialized.
- When fetching Order, you rarely need the full User object embedded → `@JsonIgnore` on `Order.user` is acceptable.
- For cases where you need User info in the Order response, you create a DTO with a flat `userId` or `userName` field.

---


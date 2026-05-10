### ManyToMany Unidirectional Mapping

In a `@ManyToMany` relationship, **multiple entities on one side** are associated with **multiple entities on the other side**. There is no parent-child hierarchy — both entities are **independent** and equal.

**Real-life use case**: An `Order` can contain many `Product`s. A `Product` can appear in many `Order`s. Neither owns the other — they are independent business entities.

```text
Java Objects:

┌───────────────────────┐                  ┌───────────────────────┐
│  Order                │    @ManyToMany   │  Product              │
│                       │                  │                       │
│  id: Long             │                  │  id: Long             │
│  orderDate: LocalDate │                  │  name: String         │
│  products: Set<Product├─────────────────>│  price: BigDecimal    │
│                       │                  │                       │
│  (OWNING entity)      │                  │  (NO ref to Order)    │
└───────────────────────┘                  └───────────────────────┘

Direction: Order ──────> Products   (one way only, unidirectional)
           Order knows its Products.   Product does NOT know its Orders.
```

---

### No Parent-Child Concept in ManyToMany

In `@OneToMany` / `@ManyToOne`, there is a clear parent-child hierarchy: User (parent) owns Orders (children), the child cannot exist without the parent. In `@ManyToMany`, **neither entity owns the other**.

```text
@OneToMany (parent-child):                  @ManyToMany (peers):

  User (PARENT)                               Order ←──────→ Product
    │                                         (independent)   (independent)
    └── Order (CHILD, depends on User)
                                              Neither is parent.
  Parent deleted → child deleted              Neither depends on the other.
  Child has FK to parent                      Deleting Order does NOT delete Product.
                                              Deleting Product does NOT delete Order.
```

**Who is the owning entity in unidirectional @ManyToMany?**

The entity that has the `@ManyToMany` annotation with `@JoinTable` is the **owning entity**. In unidirectional, there is no inverse entity — only one side knows about the relationship.

```text
Order (OWNING):
  - Has @ManyToMany
  - Has @JoinTable
  - JPA manages the join table through this entity

Product (NOT AWARE):
  - No @ManyToMany
  - No reference to Order
  - Completely independent
```

Since there is no `mappedBy` (only the owning side exists in unidirectional), JPA **fully manages** the join table lifecycle through the owning entity. You do not need to create a join table entity — JPA handles it.

---

### @ManyToMany with @JoinTable — Code Example

**Entity code**

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    // NO reference to Order — unidirectional
    // constructors, getters, setters
}
```

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "order_products",                                    // join table name
        joinColumns = @JoinColumn(name = "order_id"),               // FK to THIS entity (Order)
        inverseJoinColumns = @JoinColumn(name = "product_id")       // FK to the OTHER entity (Product)
    )
    private Set<Product> products = new HashSet<>();

    // constructors, getters, setters

    public void addProduct(Product product) {
        products.add(product);
    }

    public void removeProduct(Product product) {
        products.remove(product);
    }
}
```

**@JoinTable properties explained**

```text
@JoinTable(
    name = "order_products",                                  ← name of the join table in DB
    joinColumns = @JoinColumn(name = "order_id"),             ← FK column pointing to the OWNING entity (Order)
    inverseJoinColumns = @JoinColumn(name = "product_id")     ← FK column pointing to the OTHER entity (Product)
)
```

```text
┌─────────────────────┐
│  @JoinTable          │
│                      │
│  name                │── "order_products" (the join table name)
│                      │
│  joinColumns         │── FK to the entity WHERE @JoinTable is declared (Order)
│    @JoinColumn       │      name = "order_id" → references orders.id
│                      │
│  inverseJoinColumns  │── FK to the OTHER entity (Product)
│    @JoinColumn       │      name = "product_id" → references products.id
│                      │
└─────────────────────┘
```

**Yes — it creates a new join table**

```text
┌──────────────────┐       ┌─────────────────────┐       ┌──────────────────┐
│ orders            │       │ order_products       │       │ products          │
├──────────────────┤       │ (JOIN TABLE)         │       ├──────────────────┤
│ id (PK)          │──────>│ order_id (FK)        │       │ id (PK)          │
│ order_date       │       │ product_id (FK) ─────┼──────>│ name             │
│                  │       │                     │       │ price            │
│                  │       │ PK(order_id,        │       │                  │
│                  │       │    product_id)       │       │                  │
└──────────────────┘       └─────────────────────┘       └──────────────────┘

3 tables: orders, products, and the auto-generated order_products join table.
The join table has a composite PK of (order_id, product_id) to prevent duplicates.
```

**Generated DDL**

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    price DECIMAL(19,2)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_date DATE
);

-- JPA auto-creates this join table:
CREATE TABLE order_products (
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (order_id, product_id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

**API + Service code**

```java
// DTO for creating an order
public class CreateOrderRequest {
    private LocalDate orderDate;
    private List<Long> productIds;      // IDs of existing products

    // getters, setters
}
```

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public OrderDTO createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
```

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Fetch existing products by IDs
        List<Product> products = productRepository.findAllById(request.getProductIds());

        Order order = new Order();
        order.setOrderDate(request.getOrderDate());

        for (Product product : products) {
            order.addProduct(product);       // add to the Set
        }

        Order saved = orderRepository.save(order);
        // JPA inserts into orders table + order_products join table automatically

        return toDTO(saved);
    }

    private OrderDTO toDTO(Order order) {
        List<ProductDTO> productDTOs = order.getProducts().stream()
            .map(p -> new ProductDTO(p.getId(), p.getName(), p.getPrice()))
            .toList();
        return new OrderDTO(order.getId(), order.getOrderDate(), productDTOs);
    }
}
```

**Generated SQL for POST /api/orders**

```text
Request body:
{
    "orderDate": "2026-04-16",
    "productIds": [1, 3, 5]
}

SQL executed:
  SELECT p.* FROM products p WHERE p.id IN (1, 3, 5);              ← fetch existing products

  INSERT INTO orders (order_date) VALUES ('2026-04-16');             ← insert order (gets id=10)

  INSERT INTO order_products (order_id, product_id) VALUES (10, 1); ← link order to product 1
  INSERT INTO order_products (order_id, product_id) VALUES (10, 3); ← link order to product 3
  INSERT INTO order_products (order_id, product_id) VALUES (10, 5); ← link order to product 5
```

```text
DB after save:

orders:
  id=10, order_date='2026-04-16'

order_products (join table):
  order_id=10, product_id=1
  order_id=10, product_id=3
  order_id=10, product_id=5

products (unchanged — they already existed):
  id=1, "Laptop", 999.99
  id=3, "Keyboard", 49.99
  id=5, "Monitor", 299.99
```

**JPA manages the join table — you do not**

Since the owning entity (Order) has `@ManyToMany` + `@JoinTable` without `mappedBy`, JPA fully manages the `order_products` table:

```text
What JPA manages automatically:
  ✅ Creating the join table at startup (if ddl-auto = update/create)
  ✅ Inserting rows when you add products to order.products
  ✅ Deleting rows when you remove products from order.products
  ✅ Deleting all join rows when the order is deleted

What you do NOT need to do:
  ❌ No @Entity for order_products
  ❌ No Repository for order_products
  ❌ No manual INSERT/DELETE on the join table
  ❌ No mappedBy (unidirectional — only one side manages it)
```

**Why CascadeType.PERSIST + MERGE (not ALL) for @ManyToMany?**

```text
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})

Why NOT CascadeType.ALL (which includes REMOVE)?

  - Products are INDEPENDENT entities shared across many Orders.
  - If you delete Order #10, you do NOT want to delete "Laptop" from products table.
  - Other orders also reference "Laptop".
  - CascadeType.REMOVE on @ManyToMany would delete the Product entity itself,
    breaking all other orders that reference it.

  CascadeType.PERSIST  → auto-save new products when saving an order (if needed)
  CascadeType.MERGE    → auto-update product changes when updating an order
  NO REMOVE            → deleting order only removes join table rows, not products
```

---


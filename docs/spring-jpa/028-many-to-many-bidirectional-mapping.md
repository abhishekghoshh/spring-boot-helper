### ManyToMany Bidirectional Mapping

In a bidirectional `@ManyToMany`, **both** entities have a reference to each other. Both can navigate to the other side.

**Real-life use case**: An `Order` has many `Product`s, and a `Product` can be in many `Order`s. You want to navigate both ways: get products for an order, AND get all orders for a product.

```text
Java Objects:

┌───────────────────────┐    @ManyToMany    ┌───────────────────────┐
│  Order                │    @JoinTable     │  Product              │
│                       │                   │                       │
│  id: Long             │                   │  id: Long             │
│  orderDate: LocalDate ├──────────────────>│  name: String         │
│  products: Set<Product│                   │  price: BigDecimal    │
│                       │<──────────────────┤  orders: Set<Order>   │
│  (OWNING side)        │    @ManyToMany    │                       │
│                       │    (mappedBy)     │  (INVERSE side)       │
└───────────────────────┘                   └───────────────────────┘

Direction: Order <──────────> Product   (both ways)
```

---

### Anyone Can Be Owning or Inverse Side

In `@ManyToMany`, since both entities are **peers** (no parent-child), you can choose either as the owning side. The choice is a **design decision**, not a database constraint.

```text
Option A: Order is owning side (more common)
  Order has @ManyToMany + @JoinTable
  Product has @ManyToMany(mappedBy = "products")

Option B: Product is owning side
  Product has @ManyToMany + @JoinTable
  Order has @ManyToMany(mappedBy = "orders")

Both produce the SAME join table and SAME DB structure.
The difference: JPA reads the OWNING side to manage the join table.
So you should make the entity you UPDATE MORE OFTEN the owning side.
```

**Convention**: The entity from which you **add/remove relationships most often** should be the owning side. Typically, when placing an Order you add products to it — so Order is the owning side.

---

### DB Structure — Same as Unidirectional

The database structure is **identical** to unidirectional `@ManyToMany`. Adding bidirectional navigation does NOT change the tables — it only adds a Java-side reference.

```text
DB Tables (SAME for unidirectional and bidirectional):

┌──────────────────┐       ┌─────────────────────┐       ┌──────────────────┐
│ orders            │       │ order_products       │       │ products          │
├──────────────────┤       │ (JOIN TABLE)         │       ├──────────────────┤
│ id (PK)          │──────>│ order_id (FK)        │       │ id (PK)          │
│ order_date       │       │ product_id (FK) ─────┼──────>│ name             │
│                  │       │                     │       │ price            │
└──────────────────┘       │ PK(order_id,        │       └──────────────────┘
                           │    product_id)       │
                           └─────────────────────┘

Still 3 tables.  Still a join table.
Neither entity table holds FKs to the other.
ALL relationship data is in the join table.
```

```text
Why a join table (not FKs in entity tables)?

  - Cannot put "list of product IDs" in a single orders row    → violates 1NF
  - Cannot put "list of order IDs" in a single products row    → violates 1NF
  - A separate join table is the ONLY way to represent M:N in relational DBs

  Unidirectional:  Same 3 tables, only Order knows about Products in Java.
  Bidirectional:   Same 3 tables, both Order and Product know about each other in Java.
```

---

### Code Example — Bidirectional @ManyToMany

**Owning side — Order (has @ManyToMany + @JoinTable)**

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
        name = "order_products",
        joinColumns = @JoinColumn(name = "order_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> products = new HashSet<>();

    // Helper methods — keep both sides in sync
    public void addProduct(Product product) {
        products.add(product);
        product.getOrders().add(this);      // sync inverse side
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.getOrders().remove(this);   // sync inverse side
    }

    // constructors, getters, setters
}
```

**Inverse side — Product (has @ManyToMany with mappedBy)**

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    @ManyToMany(mappedBy = "products")       // "products" = field name in Order
    @JsonIgnore                               // prevent infinite recursion
    private Set<Order> orders = new HashSet<>();

    // constructors, getters, setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();     // stable hashCode for Sets
    }
}
```

**Why `equals()` and `hashCode()` on entities used in Sets?**

```text
@ManyToMany uses Set<Product> and Set<Order>.
Sets rely on equals()/hashCode() to determine membership.

Without proper equals()/hashCode():
  - set.add(product) and set.remove(product) may not work correctly.
  - Duplicate entries can appear in the Set.
  - Hibernate may generate unnecessary INSERT/DELETE on the join table.

Rule: Use the @Id field for equals(), and a stable constant for hashCode().
```

---

### API + Service Code — Bidirectional

**DTOs**

```java
public class CreateOrderRequest {
    private LocalDate orderDate;
    private List<Long> productIds;
}

public class OrderDTO {
    private Long id;
    private LocalDate orderDate;
    private List<ProductDTO> products;
}

public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
}

public class ProductWithOrdersDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private List<OrderSummaryDTO> orders;
}

public class OrderSummaryDTO {
    private Long id;
    private LocalDate orderDate;
}
```

**Controller**

```java
@RestController
@RequestMapping("/api")
public class OrderProductController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    // Create order with products (owning side)
    @PostMapping("/orders")
    public OrderDTO createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    // Get all orders for a product (inverse side navigation)
    @GetMapping("/products/{id}/orders")
    public ProductWithOrdersDTO getProductWithOrders(@PathVariable Long id) {
        return productService.getProductWithOrders(id);
    }
}
```

**OrderService — owning side operations**

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        List<Product> products = productRepository.findAllById(request.getProductIds());

        Order order = new Order();
        order.setOrderDate(request.getOrderDate());

        for (Product product : products) {
            order.addProduct(product);    // sets BOTH sides (owning + inverse)
        }

        Order saved = orderRepository.save(order);
        // JPA manages join table via the OWNING side (Order)

        return toDTO(saved);
    }

    @Transactional
    public OrderDTO removeProductFromOrder(Long orderId, Long productId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();

        order.removeProduct(product);   // removes from BOTH sides
        // JPA deletes the join table row automatically

        return toDTO(order);
    }

    private OrderDTO toDTO(Order order) {
        List<ProductDTO> productDTOs = order.getProducts().stream()
            .map(p -> new ProductDTO(p.getId(), p.getName(), p.getPrice()))
            .toList();
        return new OrderDTO(order.getId(), order.getOrderDate(), productDTOs);
    }
}
```

**ProductService — inverse side navigation**

```java
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductWithOrdersDTO getProductWithOrders(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow();

        // Navigate from inverse side: Product → Orders
        List<OrderSummaryDTO> orderDTOs = product.getOrders().stream()
            .map(o -> new OrderSummaryDTO(o.getId(), o.getOrderDate()))
            .toList();

        return new ProductWithOrdersDTO(
            product.getId(), product.getName(), product.getPrice(), orderDTOs
        );
    }
}
```

**Generated SQL for POST /api/orders**

```text
Request:
{
    "orderDate": "2026-04-16",
    "productIds": [1, 3, 5]
}

SQL:
  SELECT p.* FROM products p WHERE p.id IN (1, 3, 5);              ← fetch products

  INSERT INTO orders (order_date) VALUES ('2026-04-16');             ← insert order (id=10)

  INSERT INTO order_products (order_id, product_id) VALUES (10, 1); ← join table row
  INSERT INTO order_products (order_id, product_id) VALUES (10, 3); ← join table row
  INSERT INTO order_products (order_id, product_id) VALUES (10, 5); ← join table row
```

**Generated SQL for GET /api/products/1/orders**

```text
SQL:
  SELECT p.* FROM products p WHERE p.id = 1;                                     ← load product

  SELECT o.* FROM orders o                                                        ← load orders
  JOIN order_products op ON o.id = op.order_id                                     via join table
  WHERE op.product_id = 1;
```

**Generated SQL for removing a product from an order**

```text
SQL:
  DELETE FROM order_products WHERE order_id = 10 AND product_id = 3;   ← only join table row deleted
                                                                        ← Product(id=3) still exists
                                                                        ← Order(id=10) still exists
```

---

### JPA Manages Everything from the Owning Side

JPA only reads the **owning side** to decide what to do with the join table. The inverse side (`mappedBy`) is purely for Java navigation.

```text
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  OWNING SIDE (Order):                                               │
│    order.addProduct(product)  → JPA inserts into order_products     │
│    order.removeProduct(product) → JPA deletes from order_products   │
│    orderRepository.delete(order) → JPA deletes join rows + order    │
│                                                                     │
│  INVERSE SIDE (Product):                                            │
│    product.getOrders().add(order)  → JPA does NOTHING               │
│    product.getOrders().remove(order) → JPA does NOTHING             │
│    productRepository.delete(product) → deletes product only,        │
│                                         NOT join table rows!        │
│                                         (may cause FK violation)    │
│                                                                     │
│  Rule: ALWAYS manage the relationship through the OWNING side.      │
│        The inverse side is just a mirror for navigation.            │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**What happens if you only modify the inverse side?**

```java
// WRONG — modifying only the INVERSE side
@Transactional
public void addOrderToProduct_WRONG(Long productId, Long orderId) {
    Product product = productRepository.findById(productId).orElseThrow();
    Order order = orderRepository.findById(orderId).orElseThrow();

    product.getOrders().add(order);     // inverse side only
    // JPA generates NO INSERT into order_products!
    // The relationship is NOT saved!
}
```

```java
// CORRECT — modify the OWNING side (or use helper that syncs both)
@Transactional
public void addProductToOrder_CORRECT(Long orderId, Long productId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    Product product = productRepository.findById(productId).orElseThrow();

    order.addProduct(product);          // owning side + syncs inverse
    // JPA generates INSERT into order_products
}
```

**Summary — @ManyToMany bidirectional**

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│  @ManyToMany Bidirectional                                                   │
│                                                                              │
│  DB structure: SAME as unidirectional (3 tables: orders, products, join)     │
│  Java difference: Product now has Set<Order> orders (back reference)         │
│                                                                              │
│  Owning side (Order):                                                        │
│    @ManyToMany + @JoinTable(name, joinColumns, inverseJoinColumns)           │
│    → JPA manages join table through this side                                │
│                                                                              │
│  Inverse side (Product):                                                     │
│    @ManyToMany(mappedBy = "products")                                        │
│    → Just for navigation, JPA ignores changes here                           │
│                                                                              │
│  Always modify the OWNING side to persist relationship changes.              │
│  Use helper methods (addProduct/removeProduct) to sync both sides.           │
│  Use Set (not List) for @ManyToMany to avoid Hibernate bag issues.           │
│  Use CascadeType.PERSIST + MERGE (never ALL/REMOVE for shared entities).    │
│  Use @JsonIgnore on inverse side to prevent infinite recursion.              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

### How JPA Internally Manages the Join Table from the Owning Entity

JPA treats the owning side as the **single source of truth** for the `@ManyToMany` relationship. Every operation on the join table — INSERT, DELETE, full re-sync — is derived by comparing the owning entity's collection state **before and after** the transaction.

```text
┌─────────────────────────────────────────────────────────────────────────────────┐
│  JPA's Internal Flow for @ManyToMany (Owning Side = Order)                     │
│                                                                                 │
│  @Transactional method begins                                                   │
│       │                                                                         │
│       v                                                                         │
│  1. Load Order (owning entity)                                                  │
│     → Hibernate takes a SNAPSHOT of order.products = {P1, P3, P5}               │
│                                                                                 │
│  2. You modify the collection:                                                  │
│     order.removeProduct(P3);       → order.products = {P1, P5}                  │
│     order.addProduct(P7);          → order.products = {P1, P5, P7}              │
│                                                                                 │
│  3. Flush / Commit                                                              │
│     → Hibernate compares SNAPSHOT vs CURRENT:                                   │
│                                                                                 │
│     Snapshot:  {P1, P3, P5}                                                     │
│     Current:   {P1, P5, P7}                                                     │
│                                                                                 │
│     Diff:                                                                       │
│       REMOVED: P3  → DELETE FROM order_products WHERE order_id=10 AND product_id=3│
│       ADDED:   P7  → INSERT INTO order_products (order_id, product_id) VALUES (10,7)│
│       KEPT:    P1, P5 → no SQL (already in join table)                          │
│                                                                                 │
│  4. Join table is now in sync with the owning collection.                       │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Step-by-step code example showing exactly what JPA does**

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public void updateOrderProducts(Long orderId) {

        // STEP 1: Load owning entity — JPA snapshots the collection
        Order order = orderRepository.findById(orderId).orElseThrow();
        // SQL: SELECT o.* FROM orders o WHERE o.id = 10
        // SQL: SELECT p.* FROM products p
        //        JOIN order_products op ON p.id = op.product_id
        //        WHERE op.order_id = 10
        //
        // Snapshot taken: order.products = {Product(1), Product(3), Product(5)}

        // STEP 2: Modify the collection on the OWNING side
        Product p3 = order.getProducts().stream()
            .filter(p -> p.getId().equals(3L)).findFirst().orElseThrow();
        order.removeProduct(p3);              // remove P3

        Product p7 = productRepository.findById(7L).orElseThrow();
        order.addProduct(p7);                 // add P7

        // At this point IN MEMORY:
        //   order.products = {Product(1), Product(5), Product(7)}
        //   No SQL yet — changes are in Persistence Context only

        // STEP 3: Transaction commits → flush
        // JPA compares snapshot vs current and generates:
    }
    // After @Transactional method returns:
    // SQL: DELETE FROM order_products WHERE order_id = 10 AND product_id = 3
    // SQL: INSERT INTO order_products (order_id, product_id) VALUES (10, 7)
}
```

```text
Join table state:

BEFORE:                                   AFTER:
┌──────────┬────────────┐                ┌──────────┬────────────┐
│ order_id │ product_id │                │ order_id │ product_id │
├──────────┼────────────┤                ├──────────┼────────────┤
│    10    │     1      │   (kept)       │    10    │     1      │
│    10    │     3      │   (removed)    │    10    │     5      │
│    10    │     5      │   (kept)       │    10    │     7      │  (added)
└──────────┴────────────┘                └──────────┴────────────┘
```

---

**What JPA manages for each operation on the owning entity**

```text
┌─────────────────────────────────────┬──────────────────────────────────────────────────────┐
│ Operation on Owning Entity (Order)  │ What JPA does to the join table                      │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ order.addProduct(product)           │ INSERT INTO order_products (order_id, product_id)     │
│                                     │ VALUES (?, ?)                                        │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ order.removeProduct(product)        │ DELETE FROM order_products                            │
│                                     │ WHERE order_id = ? AND product_id = ?                │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ order.getProducts().clear()         │ DELETE FROM order_products WHERE order_id = ?         │
│                                     │ (all rows for this order removed)                    │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ order.setProducts(newSet)           │ DELETE all old rows + INSERT all new rows             │
│                                     │ (full re-sync of join table for this order)          │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ orderRepository.save(newOrder)      │ INSERT INTO orders + INSERT all join rows             │
│ (with products added)               │                                                      │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ orderRepository.delete(order)       │ DELETE FROM order_products WHERE order_id = ?         │
│                                     │ DELETE FROM orders WHERE id = ?                      │
│                                     │ (join rows deleted FIRST to avoid FK violation)      │
│                                     │ Products are NOT deleted (independent entities)      │
└─────────────────────────────────────┴──────────────────────────────────────────────────────┘
```

---

**What JPA does NOT manage (inverse side changes)**

```java
@Transactional
public void inverseSideDemo(Long productId, Long orderId) {
    Product product = productRepository.findById(productId).orElseThrow();
    Order order = orderRepository.findById(orderId).orElseThrow();

    // Modifying INVERSE side only:
    product.getOrders().add(order);

    // Flush / Commit:
    // SQL generated: NOTHING!
    // JPA does not read the inverse side. The join table is UNCHANGED.
}
```

```text
Why JPA ignores the inverse side:

  mappedBy = "products" means:
    "The field 'products' in Order is the REAL owner."
    "I (Product.orders) am just a MIRROR."
    "Don't read me for persistence decisions."

  JPA's dirty checking for @ManyToMany ONLY compares:
    owning collection snapshot  vs  owning collection current state

  It NEVER compares the inverse collection.

  ┌───────────────────────────────┐     ┌───────────────────────────────┐
  │  Order.products (OWNING)      │     │  Product.orders (INVERSE)     │
  │                               │     │                               │
  │  Changed? → JPA generates SQL │     │  Changed? → JPA ignores it    │
  │  Snapshot tracked? → YES      │     │  Snapshot tracked? → NO       │
  │  Drives join table? → YES     │     │  Drives join table? → NO      │
  └───────────────────────────────┘     └───────────────────────────────┘
```

---

**Complete lifecycle managed by JPA from the owning entity**

```java
@Service
public class OrderLifecycleService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;

    // CREATE — JPA inserts order + join rows
    @Transactional
    public Order createOrderWithProducts(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        Order order = new Order();
        order.setOrderDate(LocalDate.now());
        products.forEach(order::addProduct);
        return orderRepository.save(order);
        // SQL: INSERT INTO orders ...
        // SQL: INSERT INTO order_products (order_id, product_id) VALUES (?, ?) × N
    }

    // READ — JPA loads order and lazily loads join + products
    @Transactional(readOnly = true)
    public Order getOrderWithProducts(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.getProducts().size();    // trigger lazy load
        return order;
        // SQL: SELECT o.* FROM orders WHERE id = ?
        // SQL: SELECT p.* FROM products JOIN order_products ON ... WHERE order_id = ?
    }

    // UPDATE — JPA diffs the collection and syncs join table
    @Transactional
    public Order replaceProducts(Long orderId, List<Long> newProductIds) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        // Snapshot: order.products = {P1, P3, P5}

        // Clear old (JPA will diff)
        order.getProducts().forEach(p -> p.getOrders().remove(order));
        order.getProducts().clear();

        // Add new
        List<Product> newProducts = productRepository.findAllById(newProductIds);
        newProducts.forEach(order::addProduct);
        // Current: order.products = {P2, P4}

        return order;
        // SQL: DELETE FROM order_products WHERE order_id = 10 AND product_id = 1
        // SQL: DELETE FROM order_products WHERE order_id = 10 AND product_id = 3
        // SQL: DELETE FROM order_products WHERE order_id = 10 AND product_id = 5
        // SQL: INSERT INTO order_products (order_id, product_id) VALUES (10, 2)
        // SQL: INSERT INTO order_products (order_id, product_id) VALUES (10, 4)
    }

    // DELETE — JPA removes join rows first, then order
    @Transactional
    public void deleteOrder(Long orderId) {
        orderRepository.deleteById(orderId);
        // SQL: DELETE FROM order_products WHERE order_id = 10  ← join rows first
        // SQL: DELETE FROM orders WHERE id = 10                ← then the order
        // Products are NOT deleted — they are independent entities
    }
}
```

```text
Full CRUD lifecycle — all managed by JPA from the owning entity:

  CREATE:  save(order with products)  → INSERT orders + INSERT join rows
  READ:    findById + access products → SELECT orders + SELECT via join table
  UPDATE:  modify products set        → DELETE old join rows + INSERT new join rows
  DELETE:  deleteById(orderId)        → DELETE join rows + DELETE order
                                         (products untouched)

  You never write SQL for the join table.
  You never create an @Entity for the join table.
  You never create a Repository for the join table.
  JPA handles it all — as long as you modify the OWNING side.
```


---


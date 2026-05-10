### What is @Transactional? Why and How Should We Use It?

`@Transactional` is a **declarative annotation** provided by Spring that wraps a method (or all methods in a class) inside a **database transaction**. It ensures that all database operations within the annotated method either **all succeed (COMMIT)** or **all fail (ROLLBACK)**.

**Why use it?**

- Without it, each `repository.save()` is an independent auto-committed operation
- If step 2 of 3 fails, step 1 is already committed — data is left in an inconsistent state
- `@Transactional` groups all operations into a single atomic unit

**Industry Example — E-Commerce Order Placement:**

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}
```

```java
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentGateway paymentGateway;

    @Transactional
    public Order placeOrder(OrderRequest request) {

        // Step 1: Create order
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        // Step 2: Deduct inventory
        Inventory inventory = inventoryRepository
                .findByProductId(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        if (inventory.getQuantity() < request.getQuantity()) {
            throw new OutOfStockException("Insufficient stock"); // triggers ROLLBACK
        }
        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        inventoryRepository.save(inventory);

        // Step 3: Process payment
        PaymentResult result = paymentGateway.charge(request.getPaymentDetails());
        if (!result.isSuccess()) {
            throw new PaymentFailedException("Payment declined"); // triggers ROLLBACK
        }

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setAmount(request.getTotalAmount());
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        // Step 4: Confirm order
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        return order;
        // If ALL steps succeed → COMMIT
        // If ANY step throws exception → ROLLBACK all 4 steps
    }
}
```

**Generated SQL (with `spring.jpa.show-sql=true`):**

```sql
-- Step 1: Create order
Hibernate: insert into orders (customer_id, total_amount, status, created_at)
           values (?, ?, ?, ?)
-- Parameters: [101, 2499.99, 'PENDING', '2026-04-26T10:30:00']

-- Step 2: Read & update inventory
Hibernate: select i.id, i.product_id, i.quantity from inventory i
           where i.product_id=?
-- Parameters: [501]

Hibernate: update inventory set quantity=? where id=?
-- Parameters: [47, 12]

-- Step 3: Insert payment
Hibernate: insert into payments (order_id, amount, status)
           values (?, ?, ?)
-- Parameters: [1001, 2499.99, 'COMPLETED']

-- Step 4: Update order status
Hibernate: update orders set status=? where id=?
-- Parameters: ['CONFIRMED', 1001]

-- ✅ ALL succeeded → COMMIT
-- Transaction committed via: connection.commit()

-- ❌ If Step 3 threw PaymentFailedException:
-- ROLLBACK → Step 1 (order INSERT), Step 2 (inventory UPDATE) all undone
-- Transaction rolled back via: connection.rollback()
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Transactional Flow — placeOrder():                                             │
│                                                                                  │
│  Client Request                                                                  │
│       │                                                                          │
│       v                                                                          │
│  ┌──────────────────┐   Spring AOP creates a Proxy around OrderService           │
│  │  AOP Proxy        │                                                            │
│  │  ┌──────────────┐ │                                                            │
│  │  │ BEGIN         │ │  connection.setAutoCommit(false);                          │
│  │  │ TRANSACTION   │ │                                                            │
│  │  └──────┬───────┘ │                                                            │
│  │         │         │                                                            │
│  │         v         │                                                            │
│  │  ┌──────────────┐ │  INSERT INTO orders ...                                    │
│  │  │ 1.Create     │ │                                                            │
│  │  │   Order      │ │                                                            │
│  │  └──────┬───────┘ │                                                            │
│  │         │         │                                                            │
│  │         v         │                                                            │
│  │  ┌──────────────┐ │  SELECT ... FROM inventory WHERE product_id=?              │
│  │  │ 2.Deduct     │ │  UPDATE inventory SET quantity=? WHERE id=?                │
│  │  │   Inventory  │ │                                                            │
│  │  └──────┬───────┘ │                                                            │
│  │         │         │                                                            │
│  │         v         │                                                            │
│  │  ┌──────────────┐ │  INSERT INTO payments ...                                  │
│  │  │ 3.Process    │ │                                                            │
│  │  │   Payment    │ │                                                            │
│  │  └──────┬───────┘ │                                                            │
│  │         │         │                                                            │
│  │         v         │                                                            │
│  │  ┌──────────────┐ │  UPDATE orders SET status='CONFIRMED' WHERE id=?           │
│  │  │ 4.Confirm    │ │                                                            │
│  │  │   Order      │ │                                                            │
│  │  └──────┬───────┘ │                                                            │
│  │         │         │                                                            │
│  │    ┌────┴────┐    │                                                            │
│  │    │ Success?│    │                                                            │
│  │    └────┬────┘    │                                                            │
│  │    YES/ \NO       │                                                            │
│  │      /   \        │                                                            │
│  │  ┌──┐   ┌──────┐ │                                                            │
│  │  │✅│   │❌    │ │                                                            │
│  │  │CO│   │ROLL  │ │  connection.rollback();                                     │
│  │  │MM│   │BACK  │ │  // All 4 steps undone                                     │
│  │  │IT│   │      │ │                                                            │
│  │  └──┘   └──────┘ │                                                            │
│  └──────────────────┘                                                            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


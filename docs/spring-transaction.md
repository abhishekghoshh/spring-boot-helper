# Spring Transaction


## Blogs and Websites

- [Spring Transaction Management: @Transactional In-Depth](https://www.marcobehler.com/guides/spring-transaction-management-transactional-in-depth)



## Youtube

- [Spring Transactions Tutorial | Spring Boot - Daily Code Buffer](https://www.youtube.com/watch?v=raWaxW_clqo)
- [Concept && Coding - by Shrayansh](https://www.youtube.com/@ConceptAndCodingByShrayansh)
    - [Spring boot @Transactional Annotation - Part1](https://www.youtube.com/watch?v=Kf-gAW8hGQA)
    - [Spring boot @Transactional Annotation - Part2 | Declarative, Programmatic Approach and Propagation](https://www.youtube.com/watch?v=u4kRFypRmHA)
    - [Spring boot @Transactional Annotation - Part3 | Isolation Level and its different types](https://www.youtube.com/watch?v=W1YSG-MrX1c)

- [Spring Transactions](https://www.youtube.com/playlist?list=PL-bgVzzRdaPimI4ERQ9gOtUKLEIALmoFL)



## Theory

### Prerequisites

Before diving into Spring Transactions, you need to understand two foundational concepts:

1. **Concurrency Control** — How multiple threads/users accessing the same data simultaneously is managed
2. **Spring AOP** — How Spring wraps your beans with proxies to add cross-cutting behavior (see [Spring AOP](spring-aop.md))

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why These Prerequisites Matter:                                                 │
│                                                                                  │
│  Concurrency Control                    Spring AOP                               │
│  ┌─────────────────────┐               ┌──────────────────────┐                  │
│  │ Multiple users/      │               │ @Transactional works │                  │
│  │ threads accessing     │               │ via AOP Proxy that   │                  │
│  │ same bank account     │               │ wraps your service   │                  │
│  │ simultaneously        │               │ bean with transaction │                  │
│  │                       │               │ begin/commit/rollback│                  │
│  │ → Dirty Reads         │               │                      │                  │
│  │ → Lost Updates        │               │ → @Around advice     │                  │
│  │ → Phantom Reads       │               │ → Proxy pattern      │                  │
│  └─────────┬─────────────┘               └──────────┬───────────┘                  │
│            │                                        │                             │
│            └──────────────┬─────────────────────────┘                             │
│                           │                                                       │
│                           v                                                       │
│              ┌──────────────────────────┐                                         │
│              │  Spring @Transactional    │                                         │
│              │  Solves both:             │                                         │
│              │  1. Data consistency      │                                         │
│              │  2. Isolation between     │                                         │
│              │     concurrent access     │                                         │
│              └──────────────────────────┘                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### What is a Critical Section?

A **critical section** is a code segment where **shared resources** (database rows, account balances, inventory counts) are being **accessed and modified** by multiple threads/requests concurrently. Without proper protection, concurrent access leads to data corruption.

**Industry Example — Bank Fund Transfer:**

```java
@Service
public class BankService {

    @Autowired
    private AccountRepository accountRepository;

    // ❌ WITHOUT TRANSACTION — This is an UNPROTECTED critical section
    public void transferFunds(Long fromAccountId, Long toAccountId, BigDecimal amount) {

        // --- CRITICAL SECTION START ---
        // Multiple threads can read the SAME balance simultaneously

        Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow();
        Account toAccount = accountRepository.findById(toAccountId).orElseThrow();

        // Thread A reads balance = 1000
        // Thread B reads balance = 1000 (SAME stale value!)

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount)); // debit
        toAccount.setBalance(toAccount.getBalance().add(amount));           // credit

        accountRepository.save(fromAccount);
        // ⚠️ CRASH HERE — money debited but NOT credited! DATA INCONSISTENCY!
        accountRepository.save(toAccount);

        // --- CRITICAL SECTION END ---
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  PROBLEM: Concurrent Access Without Transaction Protection                       │
│                                                                                  │
│  Account A: Balance = ₹1000                                                      │
│  Account B: Balance = ₹500                                                       │
│                                                                                  │
│  Thread 1 (Transfer ₹800 A→B)          Thread 2 (Transfer ₹500 A→B)             │
│  ─────────────────────────────          ─────────────────────────────             │
│  t1: READ A.balance = ₹1000                                                     │
│                                         t2: READ A.balance = ₹1000 (stale!)     │
│  t3: CHECK ₹1000 >= ₹800 ✓                                                      │
│                                         t4: CHECK ₹1000 >= ₹500 ✓ (wrong!)      │
│  t5: A.balance = ₹1000 - ₹800 = ₹200                                           │
│  t6: SAVE A (balance = ₹200)                                                    │
│                                         t7: A.balance = ₹1000 - ₹500 = ₹500    │
│                                         t8: SAVE A (balance = ₹500) ← OVERWRITES│
│  t9: B.balance = ₹500 + ₹800 = ₹1300                                           │
│  t10: SAVE B (balance = ₹1300)                                                  │
│                                         t11: B.balance = ₹1300 + ₹500 = ₹1800  │
│                                         t12: SAVE B (balance = ₹1800)           │
│                                                                                  │
│  RESULT:                                                                         │
│    A started with ₹1000, now has ₹500 (should be -₹300, impossible!)            │
│    B started with ₹500, now has ₹1800                                            │
│    Total BEFORE: ₹1500 → Total AFTER: ₹2300 — ₹800 CREATED from nothing!       │
│                                                                                  │
│  This is the LOST UPDATE problem — Thread 2 overwrote Thread 1's write.          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**The Fix — Using `@Transactional`:**

```java
@Service
public class BankService {

    @Autowired
    private AccountRepository accountRepository;

    // ✅ WITH @Transactional — critical section is now PROTECTED
    @Transactional
    public void transferFunds(Long fromAccountId, Long toAccountId, BigDecimal amount) {

        Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow();
        Account toAccount = accountRepository.findById(toAccountId).orElseThrow();

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        // If ANY exception occurs, BOTH saves are rolled back — all or nothing!
    }
}
```

---

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

### ACID Properties with @Transactional

**ACID** defines the four guarantees every database transaction must provide:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ACID Properties:                                                                │
│                                                                                  │
│  ┌─────────────────┬─────────────────────────────────────────────────────────┐    │
│  │ Property        │ Meaning                                                │    │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤    │
│  │ A — Atomicity   │ All operations succeed OR all fail. No partial state.  │    │
│  │                 │ Like a light switch — ON or OFF, never halfway.        │    │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤    │
│  │ C — Consistency │ Transaction moves DB from one valid state to another.  │    │
│  │                 │ All constraints, rules, triggers are respected.        │    │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤    │
│  │ I — Isolation   │ Concurrent transactions don't interfere with each      │    │
│  │                 │ other. Each transaction sees a consistent snapshot.    │    │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤    │
│  │ D — Durability  │ Once committed, data survives crashes, power failures. │    │
│  │                 │ Written to disk/WAL (Write-Ahead Log).                 │    │
│  └─────────────────┴─────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**How @Transactional achieves each ACID property:**

```java
@Service
public class BankService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionLogRepository transactionLogRepository;

    /**
     * ATOMICITY — @Transactional ensures ALL or NOTHING.
     * If debit succeeds but credit fails → entire transaction rolls back.
     */
    @Transactional
    public void transferFunds(Long fromId, Long toId, BigDecimal amount) {

        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();

        from.setBalance(from.getBalance().subtract(amount));   // debit
        to.setBalance(to.getBalance().add(amount));             // credit

        accountRepository.save(from);
        accountRepository.save(to);

        // CONSISTENCY — If balance goes negative, DB constraint CHECK(balance >= 0)
        // will cause exception → transaction rolls back → DB stays consistent

        TransactionLog log = new TransactionLog(fromId, toId, amount, LocalDateTime.now());
        transactionLogRepository.save(log);
    }

    /**
     * ISOLATION — Controls what concurrent transactions can see.
     * READ_COMMITTED: Can only read data that has been committed by other txns.
     * Prevents dirty reads.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BigDecimal getAccountBalance(Long accountId) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        return account.getBalance();
    }

    /**
     * ISOLATION — SERIALIZABLE: Strongest isolation. 
     * Transactions execute as if they were sequential.
     * Prevents: dirty reads, non-repeatable reads, phantom reads.
     * Trade-off: lowest concurrency / highest consistency.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transferWithFullIsolation(Long fromId, Long toId, BigDecimal amount) {
        // Same logic, but with strictest isolation
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);
    }

    // DURABILITY — Handled by the database engine (e.g., MySQL InnoDB WAL,
    // PostgreSQL WAL). Once Spring calls connection.commit(), the DB guarantees
    // the data is persisted to disk. @Transactional doesn't control this directly —
    // it's a DB-level guarantee.
}
```

**Generated SQL showing ACID in action:**

```sql
-- ATOMICITY + CONSISTENCY — Transfer ₹500 from Account 1 to Account 2

-- Spring AOP Proxy does this BEFORE your method:
SET autocommit = 0;
START TRANSACTION;

-- Your method's SQL:
SELECT a.id, a.balance FROM accounts a WHERE a.id = 1;
-- Result: {id=1, balance=5000.00}

SELECT a.id, a.balance FROM accounts a WHERE a.id = 2;
-- Result: {id=2, balance=3000.00}

UPDATE accounts SET balance = 4500.00 WHERE id = 1;   -- debit ₹500
UPDATE accounts SET balance = 3500.00 WHERE id = 2;   -- credit ₹500

INSERT INTO transaction_log (from_id, to_id, amount, created_at)
       VALUES (1, 2, 500.00, '2026-04-26 10:30:00');

-- Spring AOP Proxy does this AFTER your method returns successfully:
COMMIT;

-- IF any exception occurred:
ROLLBACK;   -- ALL updates and inserts are undone


-- ISOLATION — READ_COMMITTED example:
-- Transaction A (transferring):
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
UPDATE accounts SET balance = 4500.00 WHERE id = 1;   -- not yet committed

-- Transaction B (reading balance) — with READ_COMMITTED:
SELECT balance FROM accounts WHERE id = 1;
-- Result: 5000.00 ← sees OLD value because A hasn't committed yet
-- (dirty read PREVENTED)

-- After Transaction A commits:
COMMIT;

-- Now Transaction B reads:
SELECT balance FROM accounts WHERE id = 1;
-- Result: 4500.00 ← sees NEW committed value
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ACID with @Transactional — Summary:                                             │
│                                                                                  │
│  @Transactional                                                                  │
│  ├── Atomicity ──→ Spring wraps in BEGIN/COMMIT or ROLLBACK                      │
│  │                 Achieved by: connection.setAutoCommit(false)                   │
│  │                              connection.commit() / connection.rollback()       │
│  │                                                                               │
│  ├── Consistency → Your code + DB constraints ensure valid state transitions     │
│  │                 Achieved by: CHECK constraints, FK constraints,               │
│  │                              NOT NULL, UNIQUE + exception → rollback          │
│  │                                                                               │
│  ├── Isolation ──→ @Transactional(isolation = Isolation.XXXX)                    │
│  │                 Achieved by: SET TRANSACTION ISOLATION LEVEL ...              │
│  │                                                                               │
│  │   ┌────────────────────┬────────────┬──────────────────┬──────────────┐       │
│  │   │ Isolation Level    │ Dirty Read │ Non-Repeatable   │ Phantom Read │       │
│  │   │                    │            │ Read             │              │       │
│  │   ├────────────────────┼────────────┼──────────────────┼──────────────┤       │
│  │   │ READ_UNCOMMITTED   │ Possible   │ Possible         │ Possible     │       │
│  │   │ READ_COMMITTED     │ Prevented  │ Possible         │ Possible     │       │
│  │   │ REPEATABLE_READ    │ Prevented  │ Prevented        │ Possible     │       │
│  │   │ SERIALIZABLE       │ Prevented  │ Prevented        │ Prevented    │       │
│  │   └────────────────────┴────────────┴──────────────────┴──────────────┘       │
│  │                                                                               │
│  └── Durability ─→ Handled by DB engine (WAL / redo logs)                        │
│                    Once COMMIT returns, data is on disk                           │
│                    @Transactional doesn't control this — DB guarantees it         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Imports for RDBMS vs NoSQL and YAML Configuration

**For RDBMS (JPA + Hibernate):**

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Data JPA (includes Hibernate as default JPA provider) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Database Drivers — choose ONE -->
    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- H2 (in-memory for testing) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

```java
// Java imports for RDBMS Transactions
import org.springframework.transaction.annotation.Transactional;       // ← The annotation
import org.springframework.transaction.annotation.Isolation;           // ← Isolation levels
import org.springframework.transaction.annotation.Propagation;         // ← Propagation types

// These come from spring-boot-starter-data-jpa:
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.Entity;           // Jakarta EE 9+ (Spring Boot 3.x)
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
```

```yaml
# application.yml — RDBMS (MySQL example)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

# PostgreSQL example:
# spring:
#   datasource:
#     url: jdbc:postgresql://localhost:5432/mydb
#     username: postgres
#     password: secret
#     driver-class-name: org.postgresql.Driver
#   jpa:
#     properties:
#       hibernate:
#         dialect: org.hibernate.dialect.PostgreSQLDialect
```

**For NoSQL (MongoDB):**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
<!-- MongoDB driver is included transitively -->
```

```java
// Java imports for MongoDB Transactions
import org.springframework.transaction.annotation.Transactional;       // ← Same annotation!
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;       // ← MongoDB-specific TxManager
import org.springframework.data.annotation.Id;                         // ← Not jakarta.persistence!
import org.springframework.data.mongodb.core.mapping.Document;
```

```yaml
# application.yml — MongoDB
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb
      # For transactions, MongoDB requires a Replica Set:
      # uri: mongodb://localhost:27017,localhost:27018,localhost:27019/mydb?replicaSet=rs0
```

```java
// ⚠️ MongoDB requires explicit TransactionManager bean configuration
@Configuration
public class MongoConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
// Note: MongoDB transactions require a Replica Set (not standalone).
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  RDBMS vs NoSQL — Transaction Support Comparison:                                │
│                                                                                  │
│  ┌──────────────────┬──────────────────────────┬────────────────────────────┐     │
│  │                  │ RDBMS (JPA/Hibernate)    │ NoSQL (MongoDB)            │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ Starter          │ spring-boot-starter-     │ spring-boot-starter-       │     │
│  │                  │ data-jpa                 │ data-mongodb               │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ Driver           │ mysql-connector-j /      │ Included in starter        │     │
│  │                  │ postgresql               │                            │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ TxManager        │ JpaTransactionManager    │ MongoTransactionManager    │     │
│  │                  │ (auto-configured)        │ (manual bean required)     │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ @Transactional   │ Same annotation          │ Same annotation            │     │
│  │                  │ (spring-tx)              │ (spring-tx)                │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ Entity Annot.    │ @Entity, @Table          │ @Document                  │     │
│  │                  │ (jakarta.persistence)    │ (spring-data-mongodb)      │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ Requirement      │ Any RDBMS                │ Replica Set required       │     │
│  │                  │                          │ (standalone won't work)    │     │
│  └──────────────────┴──────────────────────────┴────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### What is @EnableTransactionManagement? Is it Compulsory?

`@EnableTransactionManagement` tells Spring to **enable annotation-driven transaction management** — it activates the infrastructure that detects `@Transactional` annotations and creates AOP proxies.

```java
// Explicit usage:
@Configuration
@EnableTransactionManagement
public class AppConfig {
    // ...
}
```

**Is it compulsory? NO — in Spring Boot, it is NOT required.**

Spring Boot's `TransactionAutoConfiguration` (from `spring-boot-autoconfigure`) **automatically enables** `@EnableTransactionManagement` when it detects a `PlatformTransactionManager` bean on the classpath. Adding `spring-boot-starter-data-jpa` or `spring-boot-starter-data-mongodb` triggers this auto-configuration.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @EnableTransactionManagement — When to Use:                                     │
│                                                                                  │
│  ┌───────────────────────────────┬───────────────────────────────────────────┐    │
│  │ Scenario                      │ @EnableTransactionManagement Needed?      │    │
│  ├───────────────────────────────┼───────────────────────────────────────────┤    │
│  │ Spring Boot + starter-data-*  │ ❌ NO — auto-configured                   │    │
│  │ Plain Spring (no Spring Boot) │ ✅ YES — must add explicitly              │    │
│  │ Custom TransactionManager     │ ❌ NO — Spring Boot still auto-detects    │    │
│  │ Want to customize proxy mode  │ ✅ YES — to set proxyTargetClass, mode    │    │
│  └───────────────────────────────┴───────────────────────────────────────────┘    │
│                                                                                  │
│  // Customization example (rare):                                                │
│  @EnableTransactionManagement(                                                   │
│      proxyTargetClass = true,    // force CGLIB proxies instead of JDK dynamic   │
│      mode = AdviceMode.ASPECTJ   // use AspectJ weaving instead of Spring AOP    │
│  )                                                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Method-Level vs Class-Level @Transactional

**Method-Level** — Applied to specific methods. More granular control.

```java
@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    // ✅ Method-level: Only THIS method runs inside a transaction
    @Transactional
    public Product createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product = productRepository.save(product);

        AuditLog log = new AuditLog("PRODUCT_CREATED", product.getId());
        auditLogRepository.save(log);

        return product;
    }

    // ❌ No @Transactional — runs with auto-commit (each query is independent)
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    // ✅ Method-level with readOnly optimization
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContaining(keyword);
    }
}
```

**Generated SQL — Method-Level:**

```sql
-- createProduct() — has @Transactional
SET autocommit = 0;
START TRANSACTION;
Hibernate: insert into products (name, price) values (?, ?)
           -- Parameters: ['MacBook Pro', 2499.99]
Hibernate: insert into audit_log (action, entity_id) values (?, ?)
           -- Parameters: ['PRODUCT_CREATED', 1]
COMMIT;

-- getProduct() — no @Transactional → auto-commit mode
Hibernate: select p.id, p.name, p.price from products p where p.id=?
           -- Parameters: [1]
-- Each query auto-commits independently. No explicit BEGIN/COMMIT.

-- searchProducts() — @Transactional(readOnly = true)
SET autocommit = 0;
SET TRANSACTION READ ONLY;   -- ← Hibernate optimization hint
START TRANSACTION;
Hibernate: select p.id, p.name, p.price from products p
           where p.name like ?
           -- Parameters: ['%MacBook%']
COMMIT;
-- readOnly = true → Hibernate skips dirty checking (performance boost)
```

**Class-Level** — All public methods in the class run inside a transaction. Individual methods can override.

```java
@Service
@Transactional  // ✅ Class-level: ALL public methods are transactional
public class InventoryService {

    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    // Inherits class-level @Transactional (readOnly = false, default isolation)
    public void addStock(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        inv.setQuantity(inv.getQuantity() + quantity);
        inventoryRepository.save(inv);

        StockMovement movement = new StockMovement(productId, quantity, "IN");
        stockMovementRepository.save(movement);
    }

    // Inherits class-level @Transactional
    public void removeStock(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        if (inv.getQuantity() < quantity) {
            throw new OutOfStockException("Not enough stock");
        }
        inv.setQuantity(inv.getQuantity() - quantity);
        inventoryRepository.save(inv);

        StockMovement movement = new StockMovement(productId, -quantity, "OUT");
        stockMovementRepository.save(movement);
    }

    // ✅ OVERRIDES class-level: readOnly = true for better performance
    @Transactional(readOnly = true)
    public Inventory getStock(Long productId) {
        return inventoryRepository.findByProductId(productId).orElseThrow();
    }

    // ✅ OVERRIDES class-level: SERIALIZABLE isolation for critical operation
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transferStock(Long fromProductId, Long toProductId, int quantity) {
        removeStock(fromProductId, quantity);
        addStock(toProductId, quantity);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Class-Level vs Method-Level @Transactional:                                     │
│                                                                                  │
│  @Service                                                                        │
│  @Transactional            ← Class-Level (DEFAULT for all public methods)        │
│  public class InventoryService {                                                 │
│                                                                                  │
│      public void addStock() { ... }                                              │
│      │  └──→ Uses class-level defaults: readOnly=false, isolation=DEFAULT        │
│      │                                                                           │
│      public void removeStock() { ... }                                           │
│      │  └──→ Uses class-level defaults: readOnly=false, isolation=DEFAULT        │
│      │                                                                           │
│      @Transactional(readOnly = true)   ← Method-Level OVERRIDES class-level     │
│      public Inventory getStock() { ... }                                         │
│      │  └──→ Uses: readOnly=true (overridden)                                   │
│      │                                                                           │
│      @Transactional(isolation = Isolation.SERIALIZABLE)   ← OVERRIDES           │
│      public void transferStock() { ... }                                         │
│         └──→ Uses: isolation=SERIALIZABLE (overridden), readOnly=false (default)│
│  }                                                                               │
│                                                                                  │
│  Rule: Method-level @Transactional ALWAYS takes priority over class-level.       │
│  Tip: Use class-level for common defaults, override at method-level for          │
│       special cases (readOnly queries, stricter isolation).                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### How @Transactional Internally Uses Spring AOP

`@Transactional` is the **most common real-world use case of Spring AOP**. When you annotate a method with `@Transactional`, Spring does NOT modify your code. Instead, it creates a **proxy object** that wraps your bean and intercepts method calls to add transaction begin/commit/rollback logic.

**The Internal Architecture:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How Spring Creates the Transactional Proxy at Startup:                          │
│                                                                                  │
│  1. Application starts → Spring scans for beans                                  │
│  2. Spring finds OrderService with @Transactional methods                        │
│  3. BeanPostProcessor (InfrastructureAdvisorAutoProxyCreator) kicks in           │
│  4. It checks: Does any Advisor match this bean?                                 │
│  5. BeanFactoryTransactionAttributeSourceAdvisor says YES:                       │
│     - Its Pointcut (TransactionAttributeSourcePointcut) checks:                  │
│       "Does this class/method have @Transactional?"                              │
│     - Match found → create a PROXY                                               │
│  6. Spring creates a CGLIB proxy (or JDK dynamic proxy) around OrderService      │
│  7. The proxy contains TransactionInterceptor as the advice                      │
│  8. The proxy is registered in ApplicationContext INSTEAD of the real bean        │
│                                                                                  │
│  ┌──────────────┐     ┌──────────────────────────────────────────────┐            │
│  │ Spring IoC    │     │ ApplicationContext                           │            │
│  │ Container     │────→│                                              │            │
│  │               │     │  "orderService" → Proxy$$OrderService (CGLIB)│            │
│  │               │     │                    │                         │            │
│  │               │     │                    ├── TransactionInterceptor │            │
│  │               │     │                    └── Real OrderService      │            │
│  └──────────────┘     └──────────────────────────────────────────────┘            │
│                                                                                  │
│  When you @Autowired OrderService → you get the PROXY, not the real bean!        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Runtime Execution Flow:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Runtime: What Happens When You Call a @Transactional Method                     │
│                                                                                  │
│  Controller calls: orderService.placeOrder(request)                              │
│       │                                                                          │
│       │ (orderService is actually Proxy$$OrderService)                           │
│       v                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐               │
│  │  CGLIB Proxy$$OrderService                                     │               │
│  │                                                                │               │
│  │  intercept(obj, method, args, methodProxy)                     │               │
│  │       │                                                        │               │
│  │       v                                                        │               │
│  │  ┌──────────────────────────────────────────────────────────┐  │               │
│  │  │  TransactionInterceptor.invoke(MethodInvocation)         │  │               │
│  │  │       │                                                  │  │               │
│  │  │       v                                                  │  │               │
│  │  │  invokeWithinTransaction(method, targetClass, invocation)│  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 1. Get TransactionAttribute                      │  │               │
│  │  │       │    (reads @Transactional properties:             │  │               │
│  │  │       │     isolation, propagation, readOnly, etc.)      │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 2. Get PlatformTransactionManager                │  │               │
│  │  │       │    (JpaTransactionManager for JPA)               │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 3. createTransactionIfNecessary()                │  │               │
│  │  │       │    → txManager.getTransaction(definition)        │  │               │
│  │  │       │    → DataSource.getConnection()                  │  │               │
│  │  │       │    → connection.setAutoCommit(false)  ← BEGIN    │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 4. invocation.proceed()                          │  │               │
│  │  │       │    → calls REAL OrderService.placeOrder()        │  │               │
│  │  │       │    → your business logic runs here               │  │               │
│  │  │       │    → Hibernate generates SQL                     │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 5a. SUCCESS → commitTransactionAfterReturning()  │  │               │
│  │  │       │     → txManager.commit(status)                   │  │               │
│  │  │       │     → connection.commit()             ← COMMIT   │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 5b. EXCEPTION → completeTransactionAfterThrowing()│ │               │
│  │  │       │     → txManager.rollback(status)                 │  │               │
│  │  │       │     → connection.rollback()           ← ROLLBACK │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 6. cleanupTransactionInfo()                      │  │               │
│  │  │       │    → restore previous transaction (if nested)    │  │               │
│  │  └───────┴──────────────────────────────────────────────────┘  │               │
│  └────────────────────────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### AOP Annotations Used Internally — Pointcut and @Around Advice in Detail

Spring's transaction infrastructure uses AOP concepts internally, though it's implemented via `Advisor`/`Interceptor` pattern (not `@Aspect` annotations). Here's how each AOP concept maps:

**1. Pointcut — How Spring Finds @Transactional Methods:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut: TransactionAttributeSourcePointcut                                    │
│                                                                                  │
│  Equivalent @Aspect pointcut expression would be:                                │
│                                                                                  │
│    @Pointcut("@within(org.springframework.transaction.annotation.Transactional)  │
│            || @annotation(org.springframework.transaction.annotation.Transactional│
│    )")                                                                            │
│                                                                                  │
│  @within → matches ALL methods of a class annotated with @Transactional          │
│  @annotation → matches specific methods annotated with @Transactional            │
│                                                                                  │
│  Actual Spring Internal Code (simplified):                                       │
│  ─────────────────────────────────────────                                        │
│  // TransactionAttributeSourcePointcut.java                                      │
│  public class TransactionAttributeSourcePointcut extends StaticMethodMatcherPoint│
│  cut {                                                                            │
│                                                                                  │
│      @Override                                                                   │
│      public boolean matches(Method method, Class<?> targetClass) {               │
│          TransactionAttributeSource tas = getTransactionAttributeSource();        │
│          // Checks if method or class has @Transactional annotation              │
│          return (tas == null || tas.getTransactionAttribute(method, targetClass)  │
│                  != null);                                                        │
│      }                                                                           │
│  }                                                                               │
│                                                                                  │
│  // AnnotationTransactionAttributeSource.java                                    │
│  // Internally calls:                                                            │
│  //   AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class)    │
│  //   AnnotatedElementUtils.findMergedAnnotation(targetClass, Transactional.class│
│  //                                                                              │
│  // Priority: Method-level > Class-level > Interface-level                       │
│                                                                                  │
│  Scanning Flow:                                                                  │
│  ┌──────────────┐    ┌────────────────────────┐    ┌───────────────────┐          │
│  │ Spring finds  │    │ Pointcut checks:       │    │ Match found?     │          │
│  │ bean          │───→│ Has @Transactional on  │───→│ YES → create     │          │
│  │ OrderService  │    │ class or any method?   │    │      AOP Proxy   │          │
│  └──────────────┘    └────────────────────────┘    │ NO  → use as-is  │          │
│                                                     └───────────────────┘          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**2. @Around Advice — TransactionInterceptor:**

The `TransactionInterceptor` is the **@Around advice** equivalent. It wraps the target method execution with transaction begin/commit/rollback.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// SPRING INTERNAL CODE (simplified from TransactionInterceptor.java 
// and TransactionAspectSupport.java)
// ═══════════════════════════════════════════════════════════════════════════════

// TransactionInterceptor.java — The @Around Advice
public class TransactionInterceptor extends TransactionAspectSupport
        implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // Get the target class
        Class<?> targetClass = (invocation.getThis() != null
                ? AopUtils.getTargetClass(invocation.getThis()) : null);

        // Delegate to the @Around logic in parent class
        return invokeWithinTransaction(
                invocation.getMethod(),
                targetClass,
                new CoroutinesInvocationCallback() {
                    @Override
                    public Object proceedWithInvocation() throws Throwable {
                        return invocation.proceed();  // ← calls the REAL method
                    }
                }
        );
    }
}

// TransactionAspectSupport.java — The actual @Around logic
public abstract class TransactionAspectSupport {

    // This is the CORE method — equivalent to @Around advice body
    protected Object invokeWithinTransaction(Method method, Class<?> targetClass,
            InvocationCallback invocation) throws Throwable {

        // ─── STEP 1: Read @Transactional attributes ───
        TransactionAttributeSource tas = getTransactionAttributeSource();
        TransactionAttribute txAttr = tas.getTransactionAttribute(method, targetClass);
        // txAttr contains: propagation, isolation, readOnly, timeout,
        //                  rollbackFor, noRollbackFor

        // ─── STEP 2: Get the appropriate TransactionManager ───
        PlatformTransactionManager ptm = determineTransactionManager(txAttr);

        // ─── STEP 3: BEGIN TRANSACTION ───
        TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, methodId);
        // Internally calls:
        //   ptm.getTransaction(definition)
        //   → DataSourceTransactionManager.doBegin()
        //   → connection = dataSource.getConnection()
        //   → connection.setAutoCommit(false)  ← THIS IS WHERE TX BEGINS

        Object retVal;
        try {
            // ─── STEP 4: EXECUTE THE REAL METHOD ───
            retVal = invocation.proceedWithInvocation();
            // ↑ This calls YOUR actual service method
            // Your code runs here, Hibernate generates SQL
        }
        catch (Throwable ex) {
            // ─── STEP 5b: EXCEPTION → ROLLBACK ───
            completeTransactionAfterThrowing(txInfo, ex);
            // Internally:
            //   if (txAttr.rollbackOn(ex))    ← checks rollback rules
            //       txManager.rollback(status)
            //       → connection.rollback()    ← ROLLBACK SQL
            //   else
            //       txManager.commit(status)   ← commit even on checked exception
            throw ex;
        }
        finally {
            // ─── STEP 6: CLEANUP ───
            cleanupTransactionInfo(txInfo);
        }

        // ─── STEP 5a: SUCCESS → COMMIT ───
        commitTransactionAfterReturning(txInfo);
        // Internally:
        //   txManager.commit(status)
        //   → connection.commit()              ← COMMIT SQL

        return retVal;
    }
}
```

**How it maps to AOP concepts:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Mapping Spring Transaction Infrastructure to AOP Terminology:                   │
│                                                                                  │
│  ┌────────────────────┬────────────────────────────────────────────────────────┐  │
│  │ AOP Concept        │ Spring Transaction Equivalent                         │  │
│  ├────────────────────┼────────────────────────────────────────────────────────┤  │
│  │ @Aspect            │ BeanFactoryTransactionAttributeSourceAdvisor          │  │
│  │                    │ (combines Pointcut + Advice into one Advisor)         │  │
│  ├────────────────────┼────────────────────────────────────────────────────────┤  │
│  │ @Pointcut          │ TransactionAttributeSourcePointcut                    │  │
│  │                    │ Matches: @within(Transactional) ||                    │  │
│  │                    │          @annotation(Transactional)                   │  │
│  ├────────────────────┼────────────────────────────────────────────────────────┤  │
│  │ @Around            │ TransactionInterceptor.invoke()                       │  │
│  │                    │ → calls invokeWithinTransaction()                     │  │
│  ├────────────────────┼────────────────────────────────────────────────────────┤  │
│  │ JoinPoint          │ MethodInvocation (the intercepted method call)        │  │
│  ├────────────────────┼────────────────────────────────────────────────────────┤  │
│  │ proceed()          │ invocation.proceedWithInvocation()                    │  │
│  │                    │ → executes the real @Transactional method             │  │
│  ├────────────────────┼────────────────────────────────────────────────────────┤  │
│  │ Proxy              │ CGLIB Proxy (default) or JDK Dynamic Proxy            │  │
│  │                    │ Created by AutoProxyCreator at startup                │  │
│  ├────────────────────┼────────────────────────────────────────────────────────┤  │
│  │ Weaving            │ Runtime weaving via BeanPostProcessor                 │  │
│  │                    │ (InfrastructureAdvisorAutoProxyCreator)               │  │
│  └────────────────────┴────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  If written as a custom @Aspect, it would look like:                             │
│                                                                                  │
│  @Aspect                                                                         │
│  @Component                                                                      │
│  public class TransactionAspect {                                                │
│                                                                                  │
│      @Pointcut("@within(org.springframework.transaction.annotation.Transactional)│
│              || @annotation(org.springframework.transaction.annotation.Transactio │
│      nal)")                                                                       │
│      public void transactionalMethods() {}                                       │
│                                                                                  │
│      @Around("transactionalMethods()")                                           │
│      public Object invokeWithinTransaction(ProceedingJoinPoint pjp)              │
│              throws Throwable {                                                  │
│          // 1. Read @Transactional attributes                                    │
│          // 2. Get TransactionManager                                            │
│          // 3. connection.setAutoCommit(false) — BEGIN                            │
│          try {                                                                   │
│              Object result = pjp.proceed();  // 4. Call real method              │
│              // 5a. connection.commit() — COMMIT                                 │
│              return result;                                                       │
│          } catch (Throwable ex) {                                                │
│              // 5b. connection.rollback() — ROLLBACK                             │
│              throw ex;                                                           │
│          }                                                                       │
│      }                                                                           │
│  }                                                                               │
│                                                                                  │
│  ⚠️ Spring does NOT use @Aspect annotation. It uses the Advisor/Interceptor      │
│  pattern directly for performance. But the LOGIC is identical.                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Complete execution with generated SQL:**

```java
// Controller
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;  // ← This is actually Proxy$$OrderService!

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        Order order = orderService.placeOrder(request);  // calls PROXY, not real bean
        return ResponseEntity.ok(order);
    }
}
```

```sql
-- What happens at the JDBC/SQL level when orderService.placeOrder() is called:

-- Step 3: TransactionInterceptor → createTransactionIfNecessary()
-- Internally: connection.setAutoCommit(false)
SET autocommit = 0;
START TRANSACTION;

-- Step 4: invocation.proceed() → Real OrderService.placeOrder() runs
-- Hibernate generates:
Hibernate: insert into orders (customer_id, total_amount, status, created_at)
           values (?, ?, ?, ?)
           -- Binding: [101, 2499.99, 'PENDING', '2026-04-26T10:30:00']

Hibernate: select i.id, i.product_id, i.quantity
           from inventory i where i.product_id = ?
           -- Binding: [501]

Hibernate: update inventory set quantity = ? where id = ?
           -- Binding: [47, 12]

Hibernate: insert into payments (order_id, amount, status) values (?, ?, ?)
           -- Binding: [1001, 2499.99, 'COMPLETED']

Hibernate: update orders set status = ? where id = ?
           -- Binding: ['CONFIRMED', 1001]

-- Step 5a: commitTransactionAfterReturning() → txManager.commit()
-- Internally: connection.commit()
COMMIT;
```

---

### How Rollback Happens Internally

Rollback is the mechanism that **undoes all database changes** made within a transaction when an exception occurs. Spring's `TransactionInterceptor` catches exceptions and decides whether to rollback or commit based on configurable rules.

**Default Rollback Rules:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Default Rollback Behavior:                                                      │
│                                                                                  │
│  ┌───────────────────────────────┬───────────────────────────────────────────┐    │
│  │ Exception Type                │ Default Behavior                          │    │
│  ├───────────────────────────────┼───────────────────────────────────────────┤    │
│  │ RuntimeException (unchecked)  │ ✅ ROLLBACK                               │    │
│  │ Error                         │ ✅ ROLLBACK                               │    │
│  │ Checked Exception             │ ❌ COMMIT (not rollback!)                 │    │
│  └───────────────────────────────┴───────────────────────────────────────────┘    │
│                                                                                  │
│  ⚠️ COMMON PITFALL: Checked exceptions DO NOT trigger rollback by default!       │
│  You must explicitly configure: @Transactional(rollbackFor = Exception.class)    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Internal Rollback Code Flow:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// SPRING INTERNAL CODE (simplified from TransactionAspectSupport.java)
// ═══════════════════════════════════════════════════════════════════════════════

// When exception is caught in invokeWithinTransaction():
protected void completeTransactionAfterThrowing(TransactionInfo txInfo, Throwable ex) {

    if (txInfo != null && txInfo.getTransactionStatus() != null) {
        TransactionAttribute txAttr = txInfo.getTransactionAttribute();

        // ─── DECISION POINT: Should we rollback? ───
        if (txAttr.rollbackOn(ex)) {
            // YES → Rollback
            try {
                txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
                // Internally calls:
                //   AbstractPlatformTransactionManager.rollback(status)
                //   → DataSourceTransactionManager.doRollback(status)
                //   → connection.rollback()         ← ACTUAL SQL ROLLBACK
                //   → connection.setAutoCommit(true) ← restore auto-commit
            }
            catch (TransactionSystemException ex2) {
                ex2.initApplicationException(ex);
                throw ex2;
            }
        }
        else {
            // NO → Commit even though exception occurred (checked exception!)
            try {
                txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
            }
            catch (TransactionSystemException ex2) {
                ex2.initApplicationException(ex);
                throw ex2;
            }
        }
    }
}

// ─── How rollbackOn() determines rollback ───
// DefaultTransactionAttribute.java:
@Override
public boolean rollbackOn(Throwable ex) {
    return (ex instanceof RuntimeException || ex instanceof Error);
    // ↑ This is why only unchecked exceptions trigger rollback by default
}

// RuleBasedTransactionAttribute.java (when you specify rollbackFor):
@Override
public boolean rollbackOn(Throwable ex) {
    // Check custom rules first
    RollbackRuleAttribute winner = null;
    int deepest = Integer.MAX_VALUE;

    // Iterate through rollback rules (from @Transactional attributes)
    for (RollbackRuleAttribute rule : this.rollbackRules) {
        int depth = rule.getDepth(ex);
        if (depth >= 0 && depth < deepest) {
            deepest = depth;
            winner = rule;
        }
    }

    if (winner == null) {
        // No custom rule matched → fall back to default behavior
        return super.rollbackOn(ex);
    }

    // Winner found → rollback unless it's a NoRollbackRuleAttribute
    return !(winner instanceof NoRollbackRuleAttribute);
}
```

**Industry Example — Rollback Scenarios:**

```java
@Service
public class PaymentService {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private NotificationService notificationService;

    // ─── SCENARIO 1: Default rollback on RuntimeException ───
    @Transactional
    public Payment processPayment(PaymentRequest request) {
        Account account = accountRepository.findById(request.getAccountId()).orElseThrow();

        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);                // ← INSERT executed

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);                 // ← UPDATE executed

        if (request.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            // RuntimeException → triggers ROLLBACK
            throw new PaymentLimitExceededException("Amount exceeds daily limit");
            // ↑ Both INSERT and UPDATE are ROLLED BACK
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);
        return payment;
    }

    // ─── SCENARIO 2: Checked Exception does NOT rollback by default ───
    @Transactional
    public Payment processWithChecked(PaymentRequest request) throws InsufficientFundsException {
        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        paymentRepository.save(payment);                 // ← INSERT executed

        if (someCondition) {
            // ⚠️ Checked exception → COMMITS by default! INSERT is NOT rolled back!
            throw new InsufficientFundsException("Not enough funds");
        }
        return payment;
    }

    // ─── SCENARIO 3: Fix — rollbackFor includes checked exceptions ───
    @Transactional(rollbackFor = Exception.class)  // ← catches ALL exceptions
    public Payment processWithRollbackOnAll(PaymentRequest request) throws Exception {
        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        paymentRepository.save(payment);

        if (someCondition) {
            // Now checked exception ALSO triggers rollback ✅
            throw new InsufficientFundsException("Not enough funds");
        }
        return payment;
    }

    // ─── SCENARIO 4: noRollbackFor — prevent rollback on specific exceptions ───
    @Transactional(noRollbackFor = NotificationFailedException.class)
    public Payment processWithSelectiveRollback(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        paymentRepository.save(payment);

        try {
            notificationService.sendConfirmation(payment);
        } catch (NotificationFailedException e) {
            // This exception does NOT trigger rollback — payment is still saved
            log.warn("Notification failed, but payment committed", e);
            throw e;  // re-thrown but won't cause rollback due to noRollbackFor
        }
        return payment;
    }
}
```

**Generated SQL — Rollback in Action:**

```sql
-- SCENARIO 1: RuntimeException → ROLLBACK

SET autocommit = 0;
START TRANSACTION;

Hibernate: insert into payments (amount, status) values (?, ?)
           -- Binding: [15000.00, 'PROCESSING']

Hibernate: update accounts set balance = ? where id = ?
           -- Binding: [5000.00, 1]

-- PaymentLimitExceededException thrown! (RuntimeException)
-- TransactionInterceptor catches it:
--   completeTransactionAfterThrowing()
--   txAttr.rollbackOn(PaymentLimitExceededException) → true (RuntimeException)
--   txManager.rollback(status)

ROLLBACK;    -- ← Both INSERT and UPDATE are undone!

-- Database state: UNCHANGED (as if nothing happened)


-- SCENARIO 2: Checked Exception → COMMIT (BUG!)

SET autocommit = 0;
START TRANSACTION;

Hibernate: insert into payments (amount, status) values (?, ?)
           -- Binding: [500.00, 'PROCESSING']

-- InsufficientFundsException thrown! (Checked Exception)
-- TransactionInterceptor catches it:
--   completeTransactionAfterThrowing()
--   txAttr.rollbackOn(InsufficientFundsException) → false (Checked Exception!)
--   txManager.commit(status)

COMMIT;    -- ← INSERT is COMMITTED even though exception occurred!

-- Database state: Payment record EXISTS in DB with status 'PROCESSING' ← DATA INCONSISTENCY!


-- SCENARIO 3: rollbackFor = Exception.class → ROLLBACK on checked exception

SET autocommit = 0;
START TRANSACTION;

Hibernate: insert into payments (amount, status) values (?, ?)
           -- Binding: [500.00, 'PROCESSING']

-- InsufficientFundsException thrown! (Checked Exception)
-- TransactionInterceptor catches it:
--   completeTransactionAfterThrowing()
--   txAttr.rollbackOn(InsufficientFundsException) → true (matches rollbackFor rule!)
--   txManager.rollback(status)

ROLLBACK;    -- ← INSERT is rolled back correctly ✅
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Rollback Decision Flow (Internal):                                              │
│                                                                                  │
│  Exception thrown in @Transactional method                                        │
│       │                                                                          │
│       v                                                                          │
│  TransactionInterceptor catches exception                                        │
│       │                                                                          │
│       v                                                                          │
│  completeTransactionAfterThrowing(txInfo, ex)                                    │
│       │                                                                          │
│       v                                                                          │
│  txAttr.rollbackOn(ex) ─── checks rules in order:                               │
│       │                                                                          │
│       ├──→ Has @Transactional(rollbackFor = ...) rules?                          │
│       │         │                                                                │
│       │    YES: Match exception class against rollbackFor list                   │
│       │         Also check noRollbackFor list                                    │
│       │         Pick the rule with deepest match (closest in hierarchy)          │
│       │         │                                                                │
│       │         ├── rollbackFor matched → ROLLBACK                               │
│       │         └── noRollbackFor matched → COMMIT                               │
│       │                                                                          │
│       └──→ No custom rules? Use defaults:                                        │
│                 │                                                                │
│                 ├── ex instanceof RuntimeException → ROLLBACK                    │
│                 ├── ex instanceof Error → ROLLBACK                               │
│                 └── Checked Exception → COMMIT (⚠️ surprising default!)          │
│                                                                                  │
│  After decision:                                                                 │
│       │                                                                          │
│       ├── ROLLBACK path:                                                         │
│       │   txManager.rollback(status)                                             │
│       │   → AbstractPlatformTransactionManager.processRollback()                 │
│       │   → DataSourceTransactionManager.doRollback(status)                      │
│       │   → status.getTransaction().getConnectionHolder()                        │
│       │     .getConnection().rollback()          ← JDBC connection.rollback()    │
│       │   → connection.setAutoCommit(true)       ← restore auto-commit          │
│       │   → Release connection back to pool                                      │
│       │                                                                          │
│       └── COMMIT path:                                                           │
│           txManager.commit(status)                                               │
│           → AbstractPlatformTransactionManager.processCommit()                   │
│           → DataSourceTransactionManager.doCommit(status)                        │
│           → connection.commit()                  ← JDBC connection.commit()      │
│           → connection.setAutoCommit(true)                                       │
│           → Release connection back to pool                                      │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │ BEST PRACTICE:                                                             │  │
│  │                                                                            │  │
│  │ Always use: @Transactional(rollbackFor = Exception.class)                  │  │
│  │                                                                            │  │
│  │ This ensures ALL exceptions (checked + unchecked) trigger rollback.        │  │
│  │ The default behavior of committing on checked exceptions is a common       │  │
│  │ source of bugs in production systems.                                      │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Hierarchy of TransactionManager

Spring's transaction management is built on a well-designed **interface hierarchy** that follows the Template Method design pattern. Each level adds more specific behavior.

**Complete Hierarchy Diagram:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  TransactionManager Hierarchy:                                                   │
│                                                                                  │
│  TransactionManager (empty marker interface)                                     │
│  └── PlatformTransactionManager (3 methods: getTransaction, commit, rollback)    │
│      └── AbstractPlatformTransactionManager (template method implementations)    │
│          ├── DataSourceTransactionManager (plain JDBC / MyBatis)                 │
│          │   └── JdbcTransactionManager (Spring 5.3+ — same as parent + logging)│
│          ├── JpaTransactionManager (JPA / Hibernate via EntityManager)           │
│          ├── HibernateTransactionManager (native Hibernate SessionFactory)       │
│          └── JtaTransactionManager (distributed / XA transactions)              │
│                                                                                  │
│  Also:                                                                           │
│  TransactionManager                                                              │
│  └── ReactiveTransactionManager (for WebFlux / reactive stack)                   │
│      └── R2dbcTransactionManager                                                 │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  LOCAL Transaction Managers          │  DISTRIBUTED Transaction Manager    │  │
│  │  (single datasource/resource)        │  (multiple datasources/resources)   │  │
│  │                                      │                                     │  │
│  │  • DataSourceTransactionManager      │  • JtaTransactionManager            │  │
│  │  • JdbcTransactionManager            │    (uses XA protocol / 2PC)         │  │
│  │  • JpaTransactionManager             │                                     │  │
│  │  • HibernateTransactionManager       │                                     │  │
│  └──────────────────────────────────────┴─────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 1. TransactionManager — The Empty Marker Interface

`TransactionManager` is a **marker interface** with no methods. It exists purely as a **common type** so that both imperative (`PlatformTransactionManager`) and reactive (`ReactiveTransactionManager`) transaction managers share a common root.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: org.springframework.transaction.TransactionManager
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction;

/**
 * Marker interface for transaction manager implementations.
 * 
 * This is the SUPER-INTERFACE for:
 *   - PlatformTransactionManager (imperative / servlet stack)
 *   - ReactiveTransactionManager (reactive / WebFlux stack)
 *
 * @since 5.2
 */
public interface TransactionManager {
    // No methods — just a marker/tag interface
    // Purpose: common type for dependency injection and type checks
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why Does an Empty Interface Exist?                                              │
│                                                                                  │
│  Before Spring 5.2:                                                              │
│    PlatformTransactionManager was the root → only imperative (servlet) stack     │
│                                                                                  │
│  After Spring 5.2 (WebFlux introduced):                                          │
│    TransactionManager                                                            │
│    ├── PlatformTransactionManager    ← imperative (Servlet, MVC)                │
│    └── ReactiveTransactionManager    ← reactive (WebFlux, R2DBC)                │
│                                                                                  │
│  Having a common root allows:                                                    │
│    1. @Transactional to work with BOTH imperative and reactive managers          │
│    2. TransactionInterceptor to accept either type                               │
│    3. Spring to auto-detect any transaction manager bean in the context          │
│                                                                                  │
│  Usage:                                                                          │
│    @Bean                                                                         │
│    TransactionManager txManager() {  // ← Can return either type                │
│        return new JpaTransactionManager(emf);       // imperative                │
│        // return new R2dbcTransactionManager(cf);   // reactive                  │
│    }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 2. PlatformTransactionManager — The Core Contract (3 Methods)

`PlatformTransactionManager` defines the **3 fundamental operations** of transaction management. Every imperative (non-reactive) transaction manager must implement these.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: org.springframework.transaction.PlatformTransactionManager
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction;

public interface PlatformTransactionManager extends TransactionManager {

    /**
     * 1. BEGIN or JOIN a transaction based on propagation behavior.
     *
     * If no transaction exists → create a NEW one
     * If transaction already exists → join/suspend/nest based on propagation
     *
     * @param definition - contains isolation, propagation, timeout, readOnly
     *                     (parsed from @Transactional attributes)
     * @return TransactionStatus - handle to the current transaction
     *         (used later for commit/rollback)
     */
    TransactionStatus getTransaction(TransactionDefinition definition)
            throws TransactionException;

    /**
     * 2. COMMIT the transaction.
     *
     * Flushes all pending changes to the database and makes them permanent.
     * After this call, other transactions can see the changes.
     *
     * @param status - the handle returned by getTransaction()
     */
    void commit(TransactionStatus status) throws TransactionException;

    /**
     * 3. ROLLBACK the transaction.
     *
     * Undoes ALL changes made since getTransaction() was called.
     * Database returns to the state before the transaction began.
     *
     * @param status - the handle returned by getTransaction()
     */
    void rollback(TransactionStatus status) throws TransactionException;
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  PlatformTransactionManager — 3 Methods Lifecycle:                               │
│                                                                                  │
│  @Transactional method called                                                    │
│       │                                                                          │
│       v                                                                          │
│  ┌─────────────────────────────────────────────────┐                             │
│  │ 1. getTransaction(TransactionDefinition)        │                             │
│  │    ├── Reads: isolation, propagation, readOnly  │                             │
│  │    ├── Gets JDBC Connection from DataSource     │                             │
│  │    ├── connection.setAutoCommit(false)           │                             │
│  │    └── Returns: TransactionStatus               │                             │
│  │         (holds reference to connection & state)  │                             │
│  └──────────────────────┬──────────────────────────┘                             │
│                         │                                                        │
│                         v                                                        │
│            ┌────────────────────────┐                                            │
│            │  Your Business Logic   │                                            │
│            │  runs here             │                                            │
│            │  (SQL queries execute) │                                            │
│            └────────────┬───────────┘                                            │
│                         │                                                        │
│                    ┌────┴────┐                                                   │
│                    │ Success?│                                                   │
│                    └────┬────┘                                                   │
│               YES /          \ NO (exception)                                    │
│                 /              \                                                  │
│  ┌─────────────────────┐  ┌──────────────────────┐                               │
│  │ 2. commit(status)   │  │ 3. rollback(status)  │                               │
│  │    connection.commit│  │    connection.rollback│                               │
│  │    release conn     │  │    release conn       │                               │
│  └─────────────────────┘  └──────────────────────┘                               │
│                                                                                  │
│  TransactionDefinition (input) contains:                                         │
│    • propagation: REQUIRED, REQUIRES_NEW, NESTED, etc.                           │
│    • isolation: READ_COMMITTED, REPEATABLE_READ, etc.                            │
│    • timeout: max seconds before auto-rollback                                   │
│    • readOnly: optimization hint (skip dirty checking)                           │
│    • name: transaction name (for monitoring)                                     │
│                                                                                  │
│  TransactionStatus (output) contains:                                            │
│    • isNewTransaction(): was a new TX created or did we join existing?            │
│    • hasSavepoint(): does this TX have a savepoint (for NESTED)?                 │
│    • setRollbackOnly(): mark TX for rollback (even if no exception)              │
│    • isRollbackOnly(): has TX been marked for rollback?                           │
│    • isCompleted(): has commit/rollback already been called?                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 3. AbstractPlatformTransactionManager — Template Method Pattern

`AbstractPlatformTransactionManager` is an **abstract class** that provides the **default implementation** of `getTransaction()`, `commit()`, and `rollback()` using the **Template Method design pattern**. It handles all the common logic (propagation behavior, savepoints, status tracking) and delegates resource-specific operations to subclasses via **abstract hook methods**.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: AbstractPlatformTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction.support;

public abstract class AbstractPlatformTransactionManager
        implements PlatformTransactionManager, Serializable {

    // ─── getTransaction() — handles propagation logic ───
    @Override
    public final TransactionStatus getTransaction(TransactionDefinition definition)
            throws TransactionException {

        TransactionDefinition def = (definition != null ?
                definition : TransactionDefinition.withDefaults());

        // STEP 1: Ask subclass for current transaction object (if any)
        Object transaction = doGetTransaction();
        // ↑ ABSTRACT — subclass returns its resource holder
        //   e.g., DataSourceTransactionManager returns ConnectionHolder

        // STEP 2: Check if transaction already exists
        if (isExistingTransaction(transaction)) {
            // Handle propagation for EXISTING transaction:
            //   REQUIRED → join existing
            //   REQUIRES_NEW → suspend existing, create new
            //   NESTED → create savepoint
            //   NOT_SUPPORTED → suspend existing, run non-transactional
            //   NEVER → throw exception
            return handleExistingTransaction(def, transaction, debugEnabled);
        }

        // STEP 3: No existing transaction — check propagation
        if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
            throw new IllegalTransactionStateException(
                    "No existing transaction found for MANDATORY propagation");
        }

        if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {

            // STEP 4: Suspend any synchronization (no actual TX to suspend)
            SuspendedResourcesHolder suspendedResources = suspend(null);

            try {
                // STEP 5: Ask subclass to BEGIN the actual transaction
                doBegin(transaction, def);
                // ↑ ABSTRACT — subclass opens connection, sets autoCommit=false

                // STEP 6: Register synchronization
                prepareSynchronization(status, def);

                return status;
            }
            catch (RuntimeException | Error ex) {
                resume(null, suspendedResources);
                throw ex;
            }
        }

        // PROPAGATION_SUPPORTS, NOT_SUPPORTED, NEVER → no transaction
        return prepareTransactionStatus(def, null, true, ...);
    }

    // ─── commit() — handles commit with status checks ───
    @Override
    public final void commit(TransactionStatus status) throws TransactionException {

        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction already completed");
        }

        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;

        // Check if someone called setRollbackOnly()
        if (defStatus.isLocalRollbackOnly()) {
            processRollback(defStatus, false);
            return;
        }

        // Check global rollback-only (set by inner transaction participation)
        if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
            processRollback(defStatus, true);
            return;
        }

        // All checks passed → actually commit
        processCommit(defStatus);
    }

    private void processCommit(DefaultTransactionStatus status) throws TransactionException {
        try {
            if (status.hasSavepoint()) {
                // NESTED → release savepoint
                status.releaseHeldSavepoint();
            }
            else if (status.isNewTransaction()) {
                // REQUIRED / REQUIRES_NEW → actual commit
                doCommit(status);
                // ↑ ABSTRACT — subclass calls connection.commit()
            }
            // else: participating in existing TX → don't commit yet
        }
        finally {
            cleanupAfterCompletion(status);
        }
    }

    // ─── rollback() — handles rollback ───
    @Override
    public final void rollback(TransactionStatus status) throws TransactionException {
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction already completed");
        }
        processRollback((DefaultTransactionStatus) status, false);
    }

    private void processRollback(DefaultTransactionStatus status, boolean unexpected) {
        try {
            if (status.hasSavepoint()) {
                // NESTED → rollback to savepoint
                status.rollbackToHeldSavepoint();
            }
            else if (status.isNewTransaction()) {
                // We own the transaction → do actual rollback
                doRollback(status);
                // ↑ ABSTRACT — subclass calls connection.rollback()
            }
            else {
                // Participating in larger TX → mark rollback-only
                if (status.hasTransaction()) {
                    doSetRollbackOnly(status);
                }
            }
        }
        finally {
            cleanupAfterCompletion(status);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ABSTRACT HOOK METHODS — Subclasses MUST implement these:
    // ═══════════════════════════════════════════════════════════════════════

    /** Return current transaction object (e.g., ConnectionHolder) */
    protected abstract Object doGetTransaction();

    /** Begin a new transaction with the given definition */
    protected abstract void doBegin(Object transaction, TransactionDefinition definition);

    /** Perform actual commit on the underlying resource */
    protected abstract void doCommit(DefaultTransactionStatus status);

    /** Perform actual rollback on the underlying resource */
    protected abstract void doRollback(DefaultTransactionStatus status);

    /** Check if the transaction object represents an existing transaction */
    protected boolean isExistingTransaction(Object transaction) {
        return false; // subclasses override
    }

    /** Mark the transaction as rollback-only */
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        throw new IllegalTransactionStateException("Not supported");
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Template Method Pattern in AbstractPlatformTransactionManager:                  │
│                                                                                  │
│  AbstractPlatformTransactionManager                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐      │
│  │                                                                         │      │
│  │  getTransaction() ──── FINAL (cannot override)                          │      │
│  │  ├── handles propagation logic (REQUIRED, REQUIRES_NEW, etc.)           │      │
│  │  ├── calls doGetTransaction()     ← ABSTRACT (subclass provides)        │      │
│  │  ├── calls isExistingTransaction()← OVERRIDABLE                         │      │
│  │  └── calls doBegin()              ← ABSTRACT (subclass provides)        │      │
│  │                                                                         │      │
│  │  commit() ────────── FINAL                                              │      │
│  │  ├── checks rollbackOnly flags                                          │      │
│  │  ├── handles savepoints                                                 │      │
│  │  └── calls doCommit()             ← ABSTRACT (subclass provides)        │      │
│  │                                                                         │      │
│  │  rollback() ─────── FINAL                                               │      │
│  │  ├── handles savepoints                                                 │      │
│  │  ├── handles participant vs owner                                       │      │
│  │  └── calls doRollback()           ← ABSTRACT (subclass provides)        │      │
│  │                                                                         │      │
│  └─────────────────────────────────────────────────────────────────────────┘      │
│                                                                                  │
│  Subclasses only implement the resource-specific parts:                          │
│                                                                                  │
│  DataSourceTransactionManager:                                                   │
│    doGetTransaction() → return ConnectionHolder from ThreadLocal                 │
│    doBegin()          → dataSource.getConnection(); conn.setAutoCommit(false)    │
│    doCommit()         → connection.commit()                                      │
│    doRollback()       → connection.rollback()                                    │
│                                                                                  │
│  JpaTransactionManager:                                                          │
│    doGetTransaction() → return EntityManager + ConnectionHolder                  │
│    doBegin()          → emf.createEntityManager(); get JDBC connection           │
│    doCommit()         → entityManager.getTransaction().commit()                  │
│    doRollback()       → entityManager.getTransaction().rollback()                │
│                                                                                  │
│  The COMMON logic (propagation, savepoints, status tracking) is written          │
│  ONCE in AbstractPlatformTransactionManager. Subclasses only handle              │
│  the database-specific connection/commit/rollback operations.                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 4. Concrete Implementations of AbstractPlatformTransactionManager

##### 4a. DataSourceTransactionManager (Plain JDBC / MyBatis)

Manages transactions for a single **JDBC DataSource**. Works directly with `java.sql.Connection`. Used when you use plain JDBC, Spring `JdbcTemplate`, or MyBatis.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: DataSourceTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.jdbc.datasource;

public class DataSourceTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager, InitializingBean {

    private DataSource dataSource;

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected Object doGetTransaction() {
        DataSourceTransactionObject txObject = new DataSourceTransactionObject();
        // Check if there's already a connection bound to current thread
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager
                .getResource(this.dataSource);
        txObject.setConnectionHolder(conHolder, false);
        return txObject;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

        Connection con = null;
        try {
            // Get a new connection from the DataSource (connection pool)
            if (!txObject.hasConnectionHolder()) {
                Connection newCon = obtainDataSource().getConnection();
                txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
            }

            con = txObject.getConnectionHolder().getConnection();

            // Apply isolation level
            Integer previousIsolationLevel = DataSourceUtils
                    .prepareConnectionForTransaction(con, definition);
            // ↑ Internally calls: con.setTransactionIsolation(isolationLevel)

            txObject.setPreviousIsolationLevel(previousIsolationLevel);
            txObject.setReadOnly(definition.isReadOnly());

            // Switch to manual commit — THIS IS WHERE THE TRANSACTION BEGINS
            if (con.getAutoCommit()) {
                txObject.setMustRestoreAutoCommit(true);
                con.setAutoCommit(false);
                // ↑ Equivalent to: START TRANSACTION
            }

            prepareTransactionalConnection(con, definition);
            // ↑ Sets connection to read-only if @Transactional(readOnly=true)

            // Bind connection to current thread (ThreadLocal)
            if (txObject.isNewConnectionHolder()) {
                TransactionSynchronizationManager
                        .bindResource(obtainDataSource(), txObject.getConnectionHolder());
            }
        }
        catch (Throwable ex) {
            if (txObject.isNewConnectionHolder()) {
                DataSourceUtils.releaseConnection(con, obtainDataSource());
            }
            throw new CannotCreateTransactionException("Could not open connection", ex);
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        DataSourceTransactionObject txObject =
                (DataSourceTransactionObject) status.getTransaction();
        Connection con = txObject.getConnectionHolder().getConnection();
        try {
            con.commit();   // ← ACTUAL SQL COMMIT
        }
        catch (SQLException ex) {
            throw translateException("JDBC commit", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        DataSourceTransactionObject txObject =
                (DataSourceTransactionObject) status.getTransaction();
        Connection con = txObject.getConnectionHolder().getConnection();
        try {
            con.rollback();  // ← ACTUAL SQL ROLLBACK
        }
        catch (SQLException ex) {
            throw translateException("JDBC rollback", ex);
        }
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        // Unbind connection from ThreadLocal
        TransactionSynchronizationManager.unbindResource(obtainDataSource());
        // Restore auto-commit
        Connection con = txObject.getConnectionHolder().getConnection();
        if (txObject.isMustRestoreAutoCommit()) {
            con.setAutoCommit(true);
        }
        // Release connection back to pool
        DataSourceUtils.releaseConnection(con, obtainDataSource());
    }
}
```

##### 4b. JdbcTransactionManager (Spring 5.3+)

`JdbcTransactionManager` extends `DataSourceTransactionManager` — it's essentially the same but adds **better exception translation** (converts JDBC `SQLException` to Spring's `DataAccessException` hierarchy using `SQLExceptionTranslator`).

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: JdbcTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.jdbc.support;

/**
 * @since 5.3
 * Recommended replacement for DataSourceTransactionManager.
 * Adds proper JDBC 4 exception translation.
 */
public class JdbcTransactionManager extends DataSourceTransactionManager {

    public JdbcTransactionManager(DataSource dataSource) {
        super(dataSource);
    }

    // Overrides exception translation to use SQLExceptionTranslator
    // instead of generic TransactionSystemException
    @Override
    protected RuntimeException translateException(String task, SQLException ex) {
        // Uses SQLErrorCodeSQLExceptionTranslator to provide
        // database-specific exceptions like:
        //   DuplicateKeyException, DataIntegrityViolationException, etc.
        return getExceptionTranslator().translate(task, null, ex);
    }
}
```

##### 4c. JpaTransactionManager (JPA / Hibernate via EntityManager)

Manages transactions through **JPA's `EntityManager`** and `EntityManagerFactory`. This is the **most commonly used** transaction manager in Spring Boot applications with `spring-boot-starter-data-jpa`. It manages both the JPA `EntityManager` lifecycle AND the underlying JDBC connection.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: JpaTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.orm.jpa;

public class JpaTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

    private EntityManagerFactory entityManagerFactory;
    private DataSource dataSource;   // extracted from EMF for JDBC-level binding

    @Override
    protected Object doGetTransaction() {
        JpaTransactionObject txObject = new JpaTransactionObject();

        // Check for existing EntityManager bound to thread
        EntityManagerHolder emHolder = (EntityManagerHolder)
                TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
        if (emHolder != null) {
            txObject.setEntityManagerHolder(emHolder, false);
        }

        // Also check for existing JDBC connection
        if (getDataSource() != null) {
            ConnectionHolder conHolder = (ConnectionHolder)
                    TransactionSynchronizationManager.getResource(getDataSource());
            txObject.setConnectionHolder(conHolder);
        }

        return txObject;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        JpaTransactionObject txObject = (JpaTransactionObject) transaction;

        try {
            // STEP 1: Create a new EntityManager if needed
            if (!txObject.hasEntityManagerHolder()) {
                EntityManager em = createEntityManagerForTransaction();
                // ↑ Internally: entityManagerFactory.createEntityManager()
                txObject.setEntityManagerHolder(
                        new EntityManagerHolder(em), true);
            }

            EntityManager em = txObject.getEntityManagerHolder().getEntityManager();

            // STEP 2: Begin JPA transaction
            EntityTransaction etx = em.getTransaction();
            etx.begin();
            // ↑ Internally: Hibernate calls connection.setAutoCommit(false)

            // STEP 3: Get the underlying JDBC connection for isolation/readOnly
            // Hibernate exposes it via Session.doWork()
            Object rawConnection = em.unwrap(java.sql.Connection.class);
            // Set isolation level, readOnly etc. on the JDBC connection

            // STEP 4: Bind EntityManager to current thread
            if (txObject.isNewEntityManagerHolder()) {
                TransactionSynchronizationManager.bindResource(
                        obtainEntityManagerFactory(),
                        txObject.getEntityManagerHolder());
            }

            // STEP 5: Also bind JDBC connection to thread
            // (so JdbcTemplate can participate in the same transaction)
            if (getDataSource() != null) {
                ConnectionHolder conHolder = new ConnectionHolder(
                        (Connection) rawConnection);
                TransactionSynchronizationManager.bindResource(
                        getDataSource(), conHolder);
                txObject.setConnectionHolder(conHolder);
            }
        }
        catch (TransactionException ex) {
            closeEntityManagerAfterFailedBegin(txObject);
            throw ex;
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        JpaTransactionObject txObject =
                (JpaTransactionObject) status.getTransaction();
        EntityTransaction etx =
                txObject.getEntityManagerHolder().getEntityManager().getTransaction();
        try {
            etx.commit();
            // ↑ Hibernate internally:
            //   1. session.flush() → generates and executes pending SQL
            //   2. connection.commit() → commits to database
        }
        catch (RollbackException ex) {
            throw new UnexpectedRollbackException("JPA commit failed", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        JpaTransactionObject txObject =
                (JpaTransactionObject) status.getTransaction();
        EntityTransaction etx =
                txObject.getEntityManagerHolder().getEntityManager().getTransaction();
        try {
            etx.rollback();
            // ↑ Hibernate internally:
            //   1. session.clear() → discards all pending changes
            //   2. connection.rollback() → rollbacks to database
        }
        catch (PersistenceException ex) {
            throw new TransactionSystemException("JPA rollback failed", ex);
        }
    }
}
```

##### 4d. HibernateTransactionManager (Native Hibernate Session)

Manages transactions through **Hibernate's native `SessionFactory`** (not JPA `EntityManagerFactory`). Used in legacy applications or when you need Hibernate-specific features not available through JPA.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: HibernateTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.orm.hibernate5;

public class HibernateTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

    private SessionFactory sessionFactory;

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;

        Session session = obtainSessionFactory().openSession();
        // ↑ Opens a Hibernate Session (wraps a JDBC Connection)

        txObject.setSession(session);

        session.beginTransaction();
        // ↑ Internally: connection.setAutoCommit(false)

        // Bind session to thread
        TransactionSynchronizationManager.bindResource(
                obtainSessionFactory(), new SessionHolder(session));
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        HibernateTransactionObject txObject =
                (HibernateTransactionObject) status.getTransaction();
        Transaction hibernateTx = txObject.getSession().getTransaction();

        hibernateTx.commit();
        // ↑ Internally: session.flush() → connection.commit()
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        HibernateTransactionObject txObject =
                (HibernateTransactionObject) status.getTransaction();
        Transaction hibernateTx = txObject.getSession().getTransaction();

        hibernateTx.rollback();
        // ↑ Internally: session.clear() → connection.rollback()
    }
}
```

##### 4e. JtaTransactionManager (Distributed / XA Transactions)

Manages **distributed transactions** across multiple resources (databases, message queues, etc.) using the **JTA (Java Transaction API)** and **XA protocol with Two-Phase Commit (2PC)**.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: JtaTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction.jta;

public class JtaTransactionManager extends AbstractPlatformTransactionManager
        implements TransactionFactory, InitializingBean, Serializable {

    // JTA interfaces — provided by app server or standalone JTA impl (Atomikos, Narayana)
    private transient UserTransaction userTransaction;
    private transient TransactionManager transactionManager;

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        JtaTransactionObject txObject = (JtaTransactionObject) transaction;
        try {
            // Set timeout if specified
            if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
                userTransaction.setTransactionTimeout(definition.getTimeout());
            }

            // Begin distributed transaction
            userTransaction.begin();
            // ↑ JTA coordinates ALL enlisted resources (databases, JMS, etc.)
            //   Each resource gets an XA transaction branch
        }
        catch (NotSupportedException | SystemException ex) {
            throw new CannotCreateTransactionException("JTA begin failed", ex);
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        try {
            userTransaction.commit();
            // ↑ TWO-PHASE COMMIT:
            //   Phase 1 (PREPARE): Ask ALL resources — "Can you commit?"
            //     DB1: "Yes, prepared"
            //     DB2: "Yes, prepared"
            //     JMS: "Yes, prepared"
            //   Phase 2 (COMMIT): Tell ALL resources — "Commit now"
            //     DB1: COMMIT
            //     DB2: COMMIT
            //     JMS: COMMIT
        }
        catch (RollbackException | HeuristicMixedException |
               HeuristicRollbackException | SystemException ex) {
            throw new TransactionSystemException("JTA commit failed", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        try {
            userTransaction.rollback();
            // ↑ ALL enlisted resources roll back
        }
        catch (SystemException ex) {
            throw new TransactionSystemException("JTA rollback failed", ex);
        }
    }
}
```

**Comparison of All Implementations:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Comparison of TransactionManager Implementations:                               │
│                                                                                  │
│  ┌──────────────────────┬────────────────┬──────────────────┬───────────────┐     │
│  │ TransactionManager   │ Manages        │ Used With        │ Auto-Config   │     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ DataSource            │ JDBC           │ JdbcTemplate,    │ starter-jdbc  │     │
│  │ TransactionManager    │ Connection     │ MyBatis          │               │     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ Jdbc                  │ JDBC           │ Same as above +  │ starter-jdbc  │     │
│  │ TransactionManager    │ Connection     │ better exceptions│ (Spring 5.3+)│     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ Jpa                   │ EntityManager  │ Spring Data JPA, │ starter-      │     │
│  │ TransactionManager    │ + Connection   │ Hibernate (JPA)  │ data-jpa     │     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ Hibernate              │ Hibernate      │ Native Hibernate │ Manual        │     │
│  │ TransactionManager    │ Session        │ SessionFactory   │ config        │     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ Jta                   │ JTA            │ Multiple DBs,    │ starter-jta-  │     │
│  │ TransactionManager    │ UserTransaction│ DB + JMS, XA     │ atomikos      │     │
│  └──────────────────────┴────────────────┴──────────────────┴───────────────┘     │
│                                                                                  │
│  Resource Flow:                                                                  │
│                                                                                  │
│  DataSourceTxManager:  TxManager → DataSource → Connection → SQL                │
│  JpaTxManager:         TxManager → EMF → EntityManager → Session → Conn → SQL   │
│  HibernateTxManager:   TxManager → SessionFactory → Session → Conn → SQL        │
│  JtaTxManager:         TxManager → UserTransaction → XA Resources → SQL/JMS     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5. Local vs Distributed Transaction Manager

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  LOCAL Transaction Manager:                                                      │
│                                                                                  │
│  Manages transactions for a SINGLE resource (one database, one connection).      │
│  Uses native resource-level transaction APIs (JDBC commit/rollback).             │
│                                                                                  │
│  ┌────────────────┐      ┌──────────────┐      ┌──────────────┐                  │
│  │ Application     │─────→│ Local         │─────→│ Single       │                  │
│  │ @Transactional  │      │ TxManager     │      │ Database     │                  │
│  └────────────────┘      │ (JPA/JDBC)    │      │ (MySQL)      │                  │
│                          └──────────────┘      └──────────────┘                  │
│                                                                                  │
│  Examples:                                                                       │
│  • DataSourceTransactionManager  → 1 DataSource → 1 DB                          │
│  • JdbcTransactionManager        → 1 DataSource → 1 DB                          │
│  • JpaTransactionManager         → 1 EntityManagerFactory → 1 DB                │
│  • HibernateTransactionManager   → 1 SessionFactory → 1 DB                      │
│                                                                                  │
│  Pros: Simple, fast, no overhead                                                 │
│  Cons: Cannot span multiple databases or message queues                          │
│                                                                                  │
│──────────────────────────────────────────────────────────────────────────────────│
│                                                                                  │
│  DISTRIBUTED Transaction Manager (JTA):                                          │
│                                                                                  │
│  Manages transactions across MULTIPLE resources using Two-Phase Commit (2PC).    │
│  Coordinates XA-capable resources via JTA UserTransaction.                       │
│                                                                                  │
│  ┌────────────────┐      ┌──────────────┐      ┌──────────────┐                  │
│  │ Application     │─────→│ JTA           │──┬──→│ Database 1   │                  │
│  │ @Transactional  │      │ TxManager     │  │   │ (MySQL)      │                  │
│  └────────────────┘      │ (Atomikos/    │  │   └──────────────┘                  │
│                          │  Narayana)    │  │                                    │
│                          └──────────────┘  │   ┌──────────────┐                  │
│                                             ├──→│ Database 2   │                  │
│                                             │   │ (PostgreSQL) │                  │
│                                             │   └──────────────┘                  │
│                                             │                                    │
│                                             │   ┌──────────────┐                  │
│                                             └──→│ Message Queue│                  │
│                                                  │ (ActiveMQ)   │                  │
│                                                  └──────────────┘                  │
│                                                                                  │
│  Two-Phase Commit (2PC) Protocol:                                                │
│                                                                                  │
│  ┌─────────────┐                                                                 │
│  │ TxManager    │                                                                │
│  │ (coordinator)│                                                                │
│  └──────┬──────┘                                                                 │
│         │                                                                        │
│    PHASE 1: PREPARE                                                              │
│         │──── "Can you commit?" ───→  DB1: ✅ PREPARED                           │
│         │──── "Can you commit?" ───→  DB2: ✅ PREPARED                           │
│         │──── "Can you commit?" ───→  JMS: ✅ PREPARED                           │
│         │                                                                        │
│         │  ALL said YES?                                                         │
│         │                                                                        │
│    PHASE 2: COMMIT                                                               │
│         │──── "COMMIT now" ────────→  DB1: COMMITTED ✅                          │
│         │──── "COMMIT now" ────────→  DB2: COMMITTED ✅                          │
│         │──── "COMMIT now" ────────→  JMS: COMMITTED ✅                          │
│         │                                                                        │
│    If ANY resource said NO in Phase 1:                                           │
│    PHASE 2: ROLLBACK                                                             │
│         │──── "ROLLBACK" ──────────→  DB1: ROLLED BACK                           │
│         │──── "ROLLBACK" ──────────→  DB2: ROLLED BACK                           │
│         │──── "ROLLBACK" ──────────→  JMS: ROLLED BACK                           │
│                                                                                  │
│  Examples:                                                                       │
│  • JtaTransactionManager (Spring) + Atomikos / Narayana / Bitronix              │
│  • App server managed: WebLogic, WildFly, WebSphere                              │
│                                                                                  │
│  Pros: ACID across multiple resources                                            │
│  Cons: Complex, slower (2PC overhead), requires XA-capable drivers               │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │ WHEN TO USE:                                                               │  │
│  │                                                                            │  │
│  │ • Single DB → use LOCAL (JpaTransactionManager)                            │  │
│  │ • Multiple DBs in one transaction → use JTA (distributed)                  │  │
│  │ • DB + JMS in one transaction → use JTA (distributed)                      │  │
│  │                                                                            │  │
│  │ Modern alternative to JTA:                                                 │  │
│  │ • Saga Pattern (eventual consistency, no 2PC overhead)                     │  │
│  │ • Outbox Pattern (reliable event publishing)                               │  │
│  │ • These are preferred in microservices architectures                       │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 6. Configuring and Using Specific Transaction Managers

When you have **multiple data sources** or need different transaction managers for different operations, you can define named transaction managers and reference them from `@Transactional`.

**Scenario: Application with MySQL (primary) + PostgreSQL (analytics) + MongoDB:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// STEP 1: Configuration — Define multiple TransactionManagers
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
public class DataSourceConfig {

    // ─── Primary DataSource (MySQL) ───
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    // ─── Analytics DataSource (PostgreSQL) ───
    @Bean
    @ConfigurationProperties("spring.datasource.analytics")
    public DataSource analyticsDataSource() {
        return DataSourceBuilder.create().build();
    }
}

@Configuration
public class TransactionManagerConfig {

    // ─── Primary JPA TransactionManager (MySQL) ───
    @Bean
    @Primary   // ← Default when @Transactional doesn't specify a manager
    public PlatformTransactionManager transactionManager(
            EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // ─── Analytics JDBC TransactionManager (PostgreSQL) ───
    @Bean("analyticsTransactionManager")   // ← Named bean
    public PlatformTransactionManager analyticsTransactionManager(
            @Qualifier("analyticsDataSource") DataSource analyticsDataSource) {
        return new DataSourceTransactionManager(analyticsDataSource);
    }

    // ─── MongoDB TransactionManager ───
    @Bean("mongoTransactionManager")   // ← Named bean
    public PlatformTransactionManager mongoTransactionManager(
            MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTransactionManager(mongoDatabaseFactory);
    }
}
```

```yaml
# application.yml — Multiple DataSources
spring:
  datasource:
    primary:
      url: jdbc:mysql://localhost:3306/orders_db
      username: root
      password: secret
      driver-class-name: com.mysql.cj.jdbc.Driver
    analytics:
      url: jdbc:postgresql://localhost:5432/analytics_db
      username: postgres
      password: secret
      driver-class-name: org.postgresql.Driver
  data:
    mongodb:
      uri: mongodb://localhost:27017/audit_db?replicaSet=rs0
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
// STEP 2: Service — Reference specific TransactionManager in @Transactional
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private AnalyticsJdbcTemplate analyticsJdbcTemplate;
    @Autowired private AuditMongoRepository auditMongoRepository;

    // ─── Uses @Primary (default) TransactionManager → JpaTransactionManager (MySQL)
    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order(request);
        return orderRepository.save(order);
        // SQL runs against MySQL via JPA EntityManager
    }

    // ─── Uses named TransactionManager → analyticsTransactionManager (PostgreSQL)
    @Transactional(transactionManager = "analyticsTransactionManager")
    public void recordAnalytics(AnalyticsEvent event) {
        analyticsJdbcTemplate.update(
                "INSERT INTO events (type, data, created_at) VALUES (?, ?, ?)",
                event.getType(), event.getData(), LocalDateTime.now()
        );
        // SQL runs against PostgreSQL via JDBC DataSource
    }

    // ─── Uses named TransactionManager → mongoTransactionManager (MongoDB)
    @Transactional("mongoTransactionManager")   // shorthand (value = transactionManager)
    public void createAuditLog(AuditEntry entry) {
        auditMongoRepository.save(entry);
        // Operations run against MongoDB replica set
    }

    // ─── Using BOTH primary + analytics in one method ───
    // ⚠️ This does NOT create a distributed transaction!
    // Each @Transactional only manages ONE resource.
    // For true distributed transactions, use JtaTransactionManager.
    @Transactional  // manages MySQL
    public Order createOrderWithAnalytics(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));

        // Analytics runs in a SEPARATE transaction (different DB)
        analyticsService.recordAnalytics(new AnalyticsEvent("ORDER_CREATED", order.getId()));

        return order;
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
// ALTERNATIVE: Using @Qualifier to inject specific TransactionManager
// (useful in programmatic transaction management)
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class ReportService {

    private final TransactionTemplate primaryTxTemplate;
    private final TransactionTemplate analyticsTxTemplate;

    public ReportService(
            PlatformTransactionManager transactionManager,   // @Primary → JPA
            @Qualifier("analyticsTransactionManager")
            PlatformTransactionManager analyticsTxManager) {

        this.primaryTxTemplate = new TransactionTemplate(transactionManager);
        this.analyticsTxTemplate = new TransactionTemplate(analyticsTxManager);
    }

    // Programmatic transaction management with specific managers:
    public void generateReport(Long orderId) {
        // Read from MySQL (primary)
        Order order = primaryTxTemplate.execute(status -> {
            return orderRepository.findById(orderId).orElseThrow();
        });

        // Write to PostgreSQL (analytics)
        analyticsTxTemplate.execute(status -> {
            analyticsJdbcTemplate.update(
                    "INSERT INTO reports (order_id, generated_at) VALUES (?, ?)",
                    orderId, LocalDateTime.now());
            return null;
        });
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How @Transactional Resolves the TransactionManager:                             │
│                                                                                  │
│  @Transactional                                                                  │
│  public void someMethod() { ... }                                                │
│       │                                                                          │
│       v                                                                          │
│  TransactionInterceptor.determineTransactionManager(txAttr)                      │
│       │                                                                          │
│       ├── Is transactionManager specified in @Transactional?                     │
│       │   │                                                                      │
│       │   ├── YES: @Transactional("analyticsTransactionManager")                 │
│       │   │        → Look up bean by name: "analyticsTransactionManager"         │
│       │   │        → Return DataSourceTransactionManager (PostgreSQL)            │
│       │   │                                                                      │
│       │   └── NO:  @Transactional (no name specified)                            │
│       │            → Look up @Primary PlatformTransactionManager bean            │
│       │            → Return JpaTransactionManager (MySQL)                        │
│       │                                                                          │
│       v                                                                          │
│  TransactionManager resolved → used for getTransaction/commit/rollback           │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  SUMMARY:                                                                  │  │
│  │                                                                            │  │
│  │  @Transactional                          → uses @Primary TxManager         │  │
│  │  @Transactional("myTxManager")           → uses bean named "myTxManager"   │  │
│  │  @Transactional(transactionManager =     │                                 │  │
│  │       "analyticsTransactionManager")     → uses bean named "analytics..."  │  │
│  │                                                                            │  │
│  │  Spring Boot auto-configures:                                              │  │
│  │    • JpaTransactionManager    if spring-boot-starter-data-jpa present      │  │
│  │    • DataSourceTransactionManager if only spring-boot-starter-jdbc present │  │
│  │    • MongoTransactionManager  MUST be configured manually                  │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### How Many Ways Can We Manage Transactions?

Spring provides **two approaches** to manage transactions:

1. **Declarative Transaction Management** — using `@Transactional` annotation (AOP-driven)
2. **Programmatic Transaction Management** — using `TransactionTemplate` or `PlatformTransactionManager` directly in code

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Two Ways to Manage Transactions in Spring:                                      │
│                                                                                  │
│  ┌─────────────────────────────────┐    ┌─────────────────────────────────────┐   │
│  │  1. DECLARATIVE                 │    │  2. PROGRAMMATIC                    │   │
│  │     (@Transactional)            │    │     (TransactionTemplate /          │   │
│  │                                 │    │      PlatformTransactionManager)    │   │
│  ├─────────────────────────────────┤    ├─────────────────────────────────────┤   │
│  │                                 │    │                                     │   │
│  │  @Transactional                 │    │  transactionTemplate.execute(       │   │
│  │  public Order placeOrder() {    │    │      status -> {                    │   │
│  │      // just business logic     │    │          // business logic          │   │
│  │      orderRepo.save(order);     │    │          orderRepo.save(order);     │   │
│  │      return order;              │    │          return order;              │   │
│  │  }                              │    │      }                              │   │
│  │                                 │    │  );                                 │   │
│  │  TX managed by AOP Proxy        │    │  TX managed by YOUR code            │   │
│  │  (invisible to developer)       │    │  (explicit control)                 │   │
│  │                                 │    │                                     │   │
│  ├─────────────────────────────────┤    ├─────────────────────────────────────┤   │
│  │  ✅ 95% of use cases            │    │  ✅ 5% — when you need fine-grained │   │
│  │  ✅ Clean, non-invasive          │    │     control over TX boundaries     │   │
│  │  ✅ Separation of concerns       │    │  ✅ Conditional commit/rollback     │   │
│  │  ❌ All-or-nothing per method    │    │  ✅ Partial transactions            │   │
│  │  ❌ No partial TX control        │    │  ❌ Boilerplate code                │   │
│  └─────────────────────────────────┘    └─────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 1. Declarative Transaction Management (@Transactional)

Declarative transactions use the `@Transactional` annotation. Spring's AOP proxy **automatically** wraps the method with `BEGIN → your code → COMMIT/ROLLBACK`. You write **zero transaction management code** — just annotate.

**Where it is exactly required:**
- Standard CRUD service methods (create, update, delete)
- Any method that performs **multiple database operations** that must be atomic
- Read-only queries that benefit from `readOnly=true` optimization
- **95% of all transaction use cases** in production applications

**Industry Example — User Registration Service:**

```java
@Service
public class UserRegistrationService {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private EmailVerificationRepository emailVerificationRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    /**
     * DECLARATIVE — @Transactional handles everything.
     * 
     * If wallet creation fails, user record is also rolled back.
     * If email verification insert fails, both user and wallet are rolled back.
     * Developer writes ONLY business logic.
     */
    @Transactional(rollbackFor = Exception.class)
    public User registerUser(RegistrationRequest request) {

        // Step 1: Create user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user = userRepository.save(user);

        // Step 2: Create default wallet with zero balance
        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("INR");
        walletRepository.save(wallet);

        // Step 3: Create email verification token
        EmailVerification verification = new EmailVerification();
        verification.setUserId(user.getId());
        verification.setToken(UUID.randomUUID().toString());
        verification.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationRepository.save(verification);

        // Step 4: Audit log
        auditLogRepository.save(new AuditLog("USER_REGISTERED", user.getId()));

        return user;
        // ALL 4 steps succeed → COMMIT
        // ANY step fails → ROLLBACK all 4
    }

    @Transactional(readOnly = true)
    public User getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
```

**Generated SQL:**

```sql
-- Declarative: Spring AOP Proxy handles TX boundaries automatically

-- PROXY does: connection.setAutoCommit(false)
SET autocommit = 0;
START TRANSACTION;

-- Step 1:
Hibernate: insert into users (email, name, password_hash, status)
           values (?, ?, ?, ?)
           -- Binding: ['john@example.com', 'John Doe', '$2a$10$...', 'PENDING_VERIFICATION']

-- Step 2:
Hibernate: insert into wallets (user_id, balance, currency)
           values (?, ?, ?)
           -- Binding: [1, 0.00, 'INR']

-- Step 3:
Hibernate: insert into email_verifications (user_id, token, expires_at)
           values (?, ?, ?)
           -- Binding: [1, 'a1b2c3d4-...', '2026-04-27T10:30:00']

-- Step 4:
Hibernate: insert into audit_log (action, entity_id)
           values (?, ?)
           -- Binding: ['USER_REGISTERED', 1]

-- PROXY does: connection.commit()
COMMIT;
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Declarative Transaction — How It Works Under the Hood:                          │
│                                                                                  │
│  Controller                                                                      │
│     │                                                                            │
│     │ calls userRegistrationService.registerUser(request)                        │
│     v                                                                            │
│  ┌─────────────────────────────────────────────────────────────┐                  │
│  │  AOP Proxy (generated by Spring at startup)                 │                  │
│  │                                                             │                  │
│  │  ┌─────────────────────────────────────────────────────┐    │                  │
│  │  │ TransactionInterceptor.invoke()                     │    │                  │
│  │  │                                                     │    │                  │
│  │  │  1. Read @Transactional attributes                  │    │                  │
│  │  │     rollbackFor = Exception.class                   │    │                  │
│  │  │                                                     │    │                  │
│  │  │  2. txManager.getTransaction(definition)            │    │                  │
│  │  │     → connection.setAutoCommit(false)               │    │                  │
│  │  │                                                     │    │                  │
│  │  │  3. invocation.proceed()                            │    │                  │
│  │  │     ┌─────────────────────────────────────────┐     │    │                  │
│  │  │     │ REAL UserRegistrationService             │     │    │                  │
│  │  │     │   registerUser(request)                  │     │    │                  │
│  │  │     │   → INSERT user                          │     │    │                  │
│  │  │     │   → INSERT wallet                        │     │    │                  │
│  │  │     │   → INSERT email_verification            │     │    │                  │
│  │  │     │   → INSERT audit_log                     │     │    │                  │
│  │  │     └─────────────────────────────────────────┘     │    │                  │
│  │  │                                                     │    │                  │
│  │  │  4a. Success → connection.commit()                  │    │                  │
│  │  │  4b. Exception → connection.rollback()              │    │                  │
│  │  └─────────────────────────────────────────────────────┘    │                  │
│  └─────────────────────────────────────────────────────────────┘                  │
│                                                                                  │
│  Developer writes: ONLY the 4 business logic steps                               │
│  Spring handles:   BEGIN, COMMIT, ROLLBACK, connection management                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 2. Programmatic Transaction Management

Programmatic transactions give you **explicit control** over transaction boundaries in your code. You decide **exactly** when to begin, commit, or rollback — and can even commit/rollback **part** of a method.

**Two ways to do programmatic transactions:**

1. **`TransactionTemplate`** — recommended, handles boilerplate (simpler)
2. **`PlatformTransactionManager`** directly — most flexible, most verbose

**Where it is exactly required:**
- When only **part** of a method should be transactional
- When you need **conditional commit/rollback** based on business logic (not exceptions)
- When you need **multiple independent transactions** in one method
- Batch processing — commit every N records instead of all-or-nothing
- When `@Transactional` limitations apply (self-invocation, private methods)

**Why it is flexible:**
- You control exact transaction boundaries (not limited to method boundaries)
- You can commit partway through and start a new transaction
- You can programmatically call `setRollbackOnly()` without throwing an exception
- Works inside private methods, static methods, or any code block
- No AOP proxy needed — no self-invocation pitfall

---

##### 2a. Programmatic Transactions with PlatformTransactionManager

This is the **lowest-level API** — you manually call `getTransaction()`, `commit()`, and `rollback()`.

**Industry Example — Batch Import with Partial Commits:**

```java
@Service
public class BatchImportService {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ImportLogRepository importLogRepository;

    /**
     * PROGRAMMATIC with PlatformTransactionManager.
     * 
     * Use case: Import 10,000 products from CSV.
     * Commit every 500 records so that if row 7,501 fails,
     * the first 7,500 are already committed (not rolled back).
     * 
     * @Transactional would roll back ALL 10,000 on any failure — not acceptable.
     */
    public ImportResult importProducts(List<ProductDTO> products) {
        int batchSize = 500;
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < products.size(); i += batchSize) {
            List<ProductDTO> batch = products.subList(
                    i, Math.min(i + batchSize, products.size()));

            // Manually define transaction properties
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            def.setTimeout(30); // 30 seconds per batch

            // BEGIN TRANSACTION
            TransactionStatus status = transactionManager.getTransaction(def);

            try {
                for (ProductDTO dto : batch) {
                    Product product = new Product();
                    product.setSku(dto.getSku());
                    product.setName(dto.getName());
                    product.setPrice(dto.getPrice());
                    productRepository.save(product);
                }

                // COMMIT this batch — these 500 records are now permanent
                transactionManager.commit(status);
                successCount += batch.size();

            } catch (Exception e) {
                // ROLLBACK only this batch of 500 — previous batches are safe
                transactionManager.rollback(status);
                failCount += batch.size();
                errors.add("Batch starting at index " + i + " failed: " + e.getMessage());
            }
        }

        // Log the overall result (in its own transaction)
        DefaultTransactionDefinition logDef = new DefaultTransactionDefinition();
        TransactionStatus logStatus = transactionManager.getTransaction(logDef);
        try {
            importLogRepository.save(new ImportLog(successCount, failCount, errors));
            transactionManager.commit(logStatus);
        } catch (Exception e) {
            transactionManager.rollback(logStatus);
        }

        return new ImportResult(successCount, failCount, errors);
    }
}
```

**Generated SQL — Batch Import with Partial Commits:**

```sql
-- Batch 1 (rows 0-499): SUCCESS
SET autocommit = 0;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
Hibernate: insert into products (sku, name, price) values (?, ?, ?)  -- row 0
Hibernate: insert into products (sku, name, price) values (?, ?, ?)  -- row 1
-- ... 498 more inserts ...
Hibernate: insert into products (sku, name, price) values (?, ?, ?)  -- row 499
COMMIT;   -- ✅ 500 rows permanently saved

-- Batch 2 (rows 500-999): SUCCESS
SET autocommit = 0;
START TRANSACTION;
Hibernate: insert into products (sku, name, price) values (?, ?, ?)  -- row 500
-- ... 499 more inserts ...
COMMIT;   -- ✅ 500 more rows permanently saved

-- Batch 3 (rows 1000-1499): FAILURE at row 1234
SET autocommit = 0;
START TRANSACTION;
Hibernate: insert into products (sku, name, price) values (?, ?, ?)  -- row 1000
-- ... rows 1001-1233 ...
-- Row 1234: DataIntegrityViolationException (duplicate SKU)
ROLLBACK; -- ❌ Only rows 1000-1234 rolled back. Rows 0-999 are SAFE!

-- Import log:
START TRANSACTION;
Hibernate: insert into import_log (success_count, fail_count, errors)
           values (?, ?, ?)
           -- Binding: [1000, 500, '[Batch starting at index 1000 failed: ...]']
COMMIT;
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  PlatformTransactionManager — Batch Import Flow:                                 │
│                                                                                  │
│  10,000 Products to Import (batchSize = 500)                                     │
│                                                                                  │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐         ┌─────────┐         │
│  │ Batch 1  │  │ Batch 2  │  │ Batch 3  │  │ Batch 4  │  ...  │ Batch 20│         │
│  │ 0-499    │  │ 500-999  │  │1000-1499 │  │1500-1999 │       │9500-9999│         │
│  │          │  │          │  │          │  │          │       │         │         │
│  │ BEGIN    │  │ BEGIN    │  │ BEGIN    │  │ BEGIN    │       │ BEGIN   │         │
│  │ 500 INS  │  │ 500 INS  │  │ 234 INS  │  │ 500 INS  │       │ 500 INS │         │
│  │ COMMIT ✅│  │ COMMIT ✅│  │ ERROR ❌ │  │ COMMIT ✅│       │COMMIT ✅│         │
│  │          │  │          │  │ ROLLBACK │  │          │       │         │         │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘         └─────────┘         │
│                                                                                  │
│  Each batch is an INDEPENDENT transaction.                                       │
│  Batch 3 failure does NOT affect Batch 1, 2, 4, ..., 20.                         │
│                                                                                  │
│  With @Transactional: ALL 10,000 or NOTHING — Batch 3 failure rolls back ALL.   │
│  With PlatformTransactionManager: Only Batch 3 is rolled back (500 rows).        │
│  Result: 9,500 imported successfully, 500 failed.                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 2b. Programmatic Transactions with TransactionTemplate

`TransactionTemplate` is a **convenience wrapper** around `PlatformTransactionManager` that eliminates the boilerplate of `try/catch/commit/rollback`. It follows the **template pattern** (like `JdbcTemplate`, `RestTemplate`).

**Industry Example — Conditional Transaction Logic:**

```java
@Service
public class PaymentProcessingService {

    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate readOnlyTxTemplate;

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FraudDetectionService fraudDetectionService;

    // Inject TransactionTemplate via constructor
    public PaymentProcessingService(PlatformTransactionManager transactionManager) {
        // Default TransactionTemplate (read-write)
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.transactionTemplate.setTimeout(10); // 10 seconds

        // Read-only TransactionTemplate (for queries)
        this.readOnlyTxTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTxTemplate.setReadOnly(true);
    }

    /**
     * Use case: Process payment with conditional rollback.
     * 
     * We want to:
     *  1. Save the payment record (always — even if payment fails)
     *  2. Deduct balance ONLY if fraud check passes
     *  3. If fraud detected → rollback the deduction but KEEP the payment record
     * 
     * This is IMPOSSIBLE with a single @Transactional method because
     * @Transactional is all-or-nothing for the entire method.
     */
    public PaymentResult processPayment(PaymentRequest request) {

        // TX 1: Save payment record (always committed, even on fraud)
        Payment payment = transactionTemplate.execute(status -> {
            Payment p = new Payment();
            p.setAmount(request.getAmount());
            p.setAccountId(request.getAccountId());
            p.setStatus(PaymentStatus.INITIATED);
            p.setCreatedAt(LocalDateTime.now());
            return paymentRepository.save(p);
        });

        // Fraud check (outside any transaction)
        boolean isFraudulent = fraudDetectionService.check(request);

        if (isFraudulent) {
            // TX 2: Mark payment as REJECTED (separate transaction)
            transactionTemplate.execute(status -> {
                payment.setStatus(PaymentStatus.REJECTED);
                payment.setRejectionReason("FRAUD_DETECTED");
                paymentRepository.save(payment);
                return null;
            });
            return new PaymentResult(payment, false, "Fraud detected");
        }

        // TX 3: Deduct balance and confirm payment
        return transactionTemplate.execute(status -> {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow();

            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                // Programmatic rollback WITHOUT throwing exception
                status.setRollbackOnly();
                payment.setStatus(PaymentStatus.FAILED);
                payment.setRejectionReason("INSUFFICIENT_FUNDS");
                // This save will also be rolled back since we set rollbackOnly
                return new PaymentResult(payment, false, "Insufficient funds");
            }

            account.setBalance(account.getBalance().subtract(request.getAmount()));
            accountRepository.save(account);

            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            return new PaymentResult(payment, true, "Success");
        });
    }

    /**
     * Read-only query using readOnly TransactionTemplate
     */
    public AccountSummary getAccountSummary(Long accountId) {
        return readOnlyTxTemplate.execute(status -> {
            Account account = accountRepository.findById(accountId).orElseThrow();
            List<Payment> recentPayments = paymentRepository
                    .findTop10ByAccountIdOrderByCreatedAtDesc(accountId);
            return new AccountSummary(account, recentPayments);
        });
    }
}
```

**Generated SQL — TransactionTemplate with Multiple Independent Transactions:**

```sql
-- TX 1: Save payment record (always committed)
SET autocommit = 0;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
Hibernate: insert into payments (amount, account_id, status, created_at)
           values (?, ?, ?, ?)
           -- Binding: [5000.00, 42, 'INITIATED', '2026-04-26T11:00:00']
COMMIT;   -- ✅ Payment record saved permanently

-- Fraud check happens (no DB transaction)

-- SCENARIO A: Fraud detected
-- TX 2: Mark as rejected
SET autocommit = 0;
START TRANSACTION;
Hibernate: update payments set status = ?, rejection_reason = ? where id = ?
           -- Binding: ['REJECTED', 'FRAUD_DETECTED', 1001]
COMMIT;   -- ✅ Payment marked as rejected
-- Balance NOT deducted. Payment record preserved for audit.


-- SCENARIO B: No fraud, sufficient balance
-- TX 3: Deduct and confirm
SET autocommit = 0;
START TRANSACTION;
Hibernate: select a.id, a.balance from accounts a where a.id = ?
           -- Binding: [42]
           -- Result: {id=42, balance=10000.00}

Hibernate: update accounts set balance = ? where id = ?
           -- Binding: [5000.00, 42]

Hibernate: update payments set status = ? where id = ?
           -- Binding: ['COMPLETED', 1001]
COMMIT;   -- ✅ Balance deducted + payment confirmed


-- SCENARIO C: No fraud, insufficient balance
-- TX 3: status.setRollbackOnly() called
SET autocommit = 0;
START TRANSACTION;
Hibernate: select a.id, a.balance from accounts a where a.id = ?
           -- Binding: [42]
           -- Result: {id=42, balance=100.00}  ← not enough!

-- status.setRollbackOnly() called — NO exception thrown!
ROLLBACK;  -- ← Rolled back due to setRollbackOnly(), NOT due to exception
-- Payment record from TX 1 still exists with 'INITIATED' status
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  TransactionTemplate — Multiple Independent Transactions in One Method:          │
│                                                                                  │
│  processPayment(request)                                                         │
│       │                                                                          │
│       v                                                                          │
│  ┌────────────────────┐                                                          │
│  │ TX 1: Save Payment │  transactionTemplate.execute(status -> { ... })          │
│  │  INSERT payment     │  → ALWAYS committed (audit trail)                       │
│  │  COMMIT ✅          │                                                          │
│  └────────┬───────────┘                                                          │
│           │                                                                      │
│           v                                                                      │
│  ┌────────────────────┐                                                          │
│  │ Fraud Check        │  No transaction (pure computation)                       │
│  │ (no DB operations) │                                                          │
│  └────────┬───────────┘                                                          │
│           │                                                                      │
│      ┌────┴────┐                                                                 │
│      │ Fraud?  │                                                                 │
│      └────┬────┘                                                                 │
│      YES /    \ NO                                                               │
│        /        \                                                                │
│  ┌─────────────┐  ┌──────────────────────┐                                       │
│  │ TX 2: Mark   │  │ TX 3: Deduct Balance │                                      │
│  │ REJECTED     │  │                      │                                      │
│  │ UPDATE pmt   │  │ Has enough balance?  │                                      │
│  │ COMMIT ✅    │  │ YES → UPDATE account │                                      │
│  │              │  │        UPDATE payment│                                      │
│  │ Balance NOT  │  │        COMMIT ✅     │                                      │
│  │ touched!     │  │                      │                                      │
│  └─────────────┘  │ NO → setRollbackOnly │                                      │
│                    │      ROLLBACK ❌      │                                      │
│                    │      (no exception!)  │                                      │
│                    └──────────────────────┘                                       │
│                                                                                  │
│  KEY: Each execute() block is a SEPARATE, INDEPENDENT transaction.               │
│  This is impossible with a single @Transactional method.                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### TransactionTemplate Internal Code — TransactionCallback and TransactionStatus

**TransactionTemplate internals:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: TransactionTemplate (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction.support;

public class TransactionTemplate extends DefaultTransactionDefinition
        implements TransactionOperations, InitializingBean {

    private PlatformTransactionManager transactionManager;

    public TransactionTemplate(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * The core execute method.
     * Accepts a TransactionCallback<T> — your business logic wrapped in a lambda.
     * Returns whatever your callback returns (or null).
     */
    @Override
    public <T> T execute(TransactionCallback<T> action) throws TransactionException {

        // STEP 1: BEGIN TRANSACTION
        // Uses 'this' as TransactionDefinition (inherits isolation, propagation, etc.)
        TransactionStatus status = this.transactionManager.getTransaction(this);
        // ↑ Internally: connection.setAutoCommit(false)

        T result;
        try {
            // STEP 2: EXECUTE YOUR CALLBACK
            result = action.doInTransaction(status);
            // ↑ YOUR lambda runs here. 'status' lets you call setRollbackOnly()
        }
        catch (RuntimeException | Error ex) {
            // STEP 3a: EXCEPTION → ROLLBACK
            rollbackOnException(status, ex);
            throw ex;
        }

        // STEP 3b: SUCCESS → COMMIT (unless rollbackOnly was set)
        this.transactionManager.commit(status);
        // ↑ If status.isRollbackOnly() == true, commit() internally calls rollback()
        //   This is why setRollbackOnly() works without exceptions

        return result;
    }

    private void rollbackOnException(TransactionStatus status, Throwable ex) {
        try {
            this.transactionManager.rollback(status);
        }
        catch (TransactionSystemException ex2) {
            ex2.initApplicationException(ex);
            throw ex2;
        }
    }
}
```

**TransactionCallback — The Functional Interface:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: TransactionCallback
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction.support;

/**
 * Functional interface for transactional code.
 * 
 * T = the return type of your transactional operation.
 * Use TransactionCallbackWithoutResult if you don't need a return value.
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    /**
     * @param status — TransactionStatus: lets you query and control the transaction
     *                 • status.setRollbackOnly()  → force rollback without exception
     *                 • status.isRollbackOnly()   → check if marked for rollback
     *                 • status.isNewTransaction()  → is this a new TX or joining existing?
     *                 • status.hasSavepoint()      → does this TX have a savepoint?
     *                 • status.isCompleted()       → has TX already committed/rolled back?
     *
     * @return T — your result (Order, Payment, void, etc.)
     */
    T doInTransaction(TransactionStatus status);
}

// For void operations (no return value):
public abstract class TransactionCallbackWithoutResult implements TransactionCallback<Object> {

    @Override
    public final Object doInTransaction(TransactionStatus status) {
        doInTransactionWithoutResult(status);
        return null;
    }

    protected abstract void doInTransactionWithoutResult(TransactionStatus status);
}
```

**TransactionStatus — What You Can Do With It:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: TransactionStatus
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction;

public interface TransactionStatus extends TransactionExecution, SavepointManager {

    // From TransactionExecution:
    boolean isNewTransaction();     // Did WE create this TX, or are we joining?
    void setRollbackOnly();         // Mark TX for rollback (no exception needed!)
    boolean isRollbackOnly();       // Has someone marked this TX for rollback?
    boolean isCompleted();          // Has commit/rollback been called?

    // From SavepointManager:
    Object createSavepoint();       // Create a savepoint (for NESTED propagation)
    void rollbackToSavepoint(Object savepoint);  // Rollback to savepoint
    void releaseSavepoint(Object savepoint);      // Release savepoint

    // TransactionStatus specific:
    boolean hasSavepoint();         // Does this TX have an active savepoint?
    void flush();                   // Flush underlying session (JPA/Hibernate)
}
```

**Industry Example — Using TransactionStatus for Fine-Grained Control:**

```java
@Service
public class OrderFulfillmentService {

    private final TransactionTemplate txTemplate;

    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private ShippingService shippingService;

    public OrderFulfillmentService(PlatformTransactionManager txManager) {
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /**
     * Fulfill order: deduct inventory + update order status.
     * 
     * Use case for TransactionStatus.setRollbackOnly():
     * If inventory is low (< 5 remaining), we want to ROLLBACK the deduction
     * but NOT throw an exception — instead, return a "backorder" response
     * so the caller can handle it gracefully.
     */
    public FulfillmentResult fulfillOrder(Long orderId) {

        return txTemplate.execute(status -> {
            Order order = orderRepository.findById(orderId).orElseThrow();
            Inventory inventory = inventoryRepository
                    .findByProductId(order.getProductId()).orElseThrow();

            // Deduct inventory
            int remaining = inventory.getQuantity() - order.getQuantity();
            inventory.setQuantity(remaining);
            inventoryRepository.save(inventory);

            // Business rule: if remaining stock drops below threshold,
            // rollback and backorder instead of fulfilling
            if (remaining < 5) {
                status.setRollbackOnly();
                // ↑ Transaction WILL be rolled back when execute() returns
                //   But NO exception is thrown — we return a result gracefully
                return new FulfillmentResult(order, FulfillmentStatus.BACKORDERED,
                        "Low stock: " + remaining + " remaining after fulfillment");
            }

            order.setStatus(OrderStatus.SHIPPED);
            orderRepository.save(order);

            return new FulfillmentResult(order, FulfillmentStatus.SHIPPED, "Success");
        });
        // If setRollbackOnly() was called:
        //   TransactionTemplate.execute() → transactionManager.commit(status)
        //   → commit() sees isRollbackOnly() == true
        //   → internally calls rollback() instead of commit()
        //   → inventory deduction is UNDONE
        //   → but FulfillmentResult is still returned to caller (no exception!)
    }
}
```

**Generated SQL — setRollbackOnly() vs Normal Commit:**

```sql
-- SCENARIO A: Sufficient stock (remaining >= 5) → COMMIT
SET autocommit = 0;
START TRANSACTION;

Hibernate: select o.id, o.product_id, o.quantity, o.status
           from orders o where o.id = ?
           -- Binding: [101]

Hibernate: select i.id, i.product_id, i.quantity
           from inventory i where i.product_id = ?
           -- Binding: [501]
           -- Result: {quantity=50}

Hibernate: update inventory set quantity = ? where id = ?
           -- Binding: [45, 12]     -- 50 - 5 = 45 (remaining >= 5, OK!)

Hibernate: update orders set status = ? where id = ?
           -- Binding: ['SHIPPED', 101]

COMMIT;    -- ✅ Normal commit. Inventory deducted, order shipped.


-- SCENARIO B: Low stock (remaining < 5) → setRollbackOnly() → ROLLBACK
SET autocommit = 0;
START TRANSACTION;

Hibernate: select o.id, o.product_id, o.quantity, o.status
           from orders o where o.id = ?
           -- Binding: [101]

Hibernate: select i.id, i.product_id, i.quantity
           from inventory i where i.product_id = ?
           -- Binding: [501]
           -- Result: {quantity=6}

Hibernate: update inventory set quantity = ? where id = ?
           -- Binding: [1, 12]      -- 6 - 5 = 1 (remaining < 5!)

-- status.setRollbackOnly() called — marks TX for rollback
-- TransactionTemplate calls transactionManager.commit(status)
-- commit() checks: status.isRollbackOnly() → true
-- commit() internally delegates to rollback()

ROLLBACK;  -- ❌ Inventory update UNDONE. No exception thrown.
           --    Caller receives FulfillmentResult with BACKORDERED status.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  TransactionTemplate.execute() Internal Flow:                                    │
│                                                                                  │
│  transactionTemplate.execute(callback)                                           │
│       │                                                                          │
│       v                                                                          │
│  ┌─────────────────────────────────────────────────────┐                         │
│  │ 1. transactionManager.getTransaction(this)          │                         │
│  │    → connection.setAutoCommit(false)                 │                         │
│  │    → returns TransactionStatus                       │                         │
│  └──────────────────────┬──────────────────────────────┘                         │
│                         │                                                        │
│                         v                                                        │
│  ┌─────────────────────────────────────────────────────┐                         │
│  │ 2. callback.doInTransaction(status)                  │                         │
│  │    → YOUR code runs here                             │                         │
│  │    → status.setRollbackOnly() available              │                         │
│  └──────────────────────┬──────────────────────────────┘                         │
│                         │                                                        │
│                    ┌────┴──────┐                                                 │
│                    │ Exception?│                                                 │
│                    └────┬──────┘                                                 │
│               NO /            \ YES                                              │
│                /                \                                                │
│  ┌────────────────────┐  ┌──────────────────────┐                                │
│  │ 3b. commit(status) │  │ 3a. rollback(status) │                                │
│  │                     │  │    → connection       │                                │
│  │  isRollbackOnly()?  │  │      .rollback()     │                                │
│  │  ├── YES → rollback │  │    → throw exception │                                │
│  │  └── NO  → commit   │  └──────────────────────┘                                │
│  │     connection       │                                                         │
│  │     .commit()        │                                                         │
│  └────────────────────┘                                                          │
│                                                                                  │
│  KEY INSIGHT:                                                                    │
│  • RuntimeException/Error → automatic rollback + exception re-thrown              │
│  • Checked Exception → NOT caught by TransactionTemplate (propagates as-is,      │
│    transaction commits by default — same as @Transactional behavior)             │
│  • setRollbackOnly() → rollback when commit() is called, NO exception            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 3. Comparison — Declarative vs Programmatic

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Declarative (@Transactional) vs Programmatic (TransactionTemplate / TxManager)  │
│                                                                                  │
│  ┌──────────────────────┬──────────────────────────┬────────────────────────────┐ │
│  │ Aspect               │ Declarative              │ Programmatic               │ │
│  │                      │ (@Transactional)         │ (TransactionTemplate /     │ │
│  │                      │                          │  PlatformTransactionManager)│ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ How it works          │ AOP Proxy wraps method   │ You call begin/commit/     │ │
│  │                      │ automatically             │ rollback explicitly        │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ TX boundary           │ Entire method            │ Any code block             │ │
│  │                      │ (method = transaction)    │ (fine-grained)             │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ Code invasiveness    │ Non-invasive             │ Invasive (TX code mixed    │ │
│  │                      │ (just an annotation)      │ with business logic)       │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ Rollback trigger     │ Exception-based only     │ Exception OR               │ │
│  │                      │                          │ setRollbackOnly()          │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ Multiple TXs in      │ ❌ Not possible           │ ✅ Yes — each execute()    │ │
│  │ one method            │ (1 method = 1 TX)        │ is independent             │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ Partial commit       │ ❌ All or nothing         │ ✅ Commit batch by batch   │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ Self-invocation      │ ❌ Broken (bypasses       │ ✅ Works (no proxy needed) │ │
│  │ (this.method())      │ proxy — no TX!)           │                            │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ Private methods       │ ❌ Not supported          │ ✅ Works anywhere          │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ Testability          │ ✅ Easy (just annotate)   │ Needs TransactionManager   │ │
│  │                      │                          │ mock/stub                  │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ Readability          │ ✅ Clean, minimal code    │ ❌ More boilerplate         │ │
│  ├──────────────────────┼──────────────────────────┼────────────────────────────┤ │
│  │ When to use          │ 95% — standard services  │ 5% — batch processing,     │ │
│  │                      │ CRUD, business methods    │ conditional TX, partial    │ │
│  │                      │                          │ commits, self-invocation   │ │
│  └──────────────────────┴──────────────────────────┴────────────────────────────┘ │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  ADVANTAGES — Declarative:                                                 │  │
│  │  ✅ Clean separation of concerns (business logic free from TX code)         │  │
│  │  ✅ Easy to change TX behavior (just change annotation attributes)          │  │
│  │  ✅ Consistent — every @Transactional method follows same pattern           │  │
│  │  ✅ Less error-prone (Spring handles commit/rollback correctly)             │  │
│  │                                                                            │  │
│  │  DISADVANTAGES — Declarative:                                              │  │
│  │  ❌ Self-invocation pitfall (this.method() bypasses proxy)                  │  │
│  │  ❌ Cannot do partial commits within one method                             │  │
│  │  ❌ Rollback only via exceptions (no programmatic setRollbackOnly)          │  │
│  │  ❌ Method = transaction boundary (can't scope transaction smaller)         │  │
│  │  ❌ Private methods not supported                                           │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  ADVANTAGES — Programmatic:                                                │  │
│  │  ✅ Fine-grained control over transaction scope                             │  │
│  │  ✅ Multiple transactions in one method                                     │  │
│  │  ✅ Conditional commit/rollback without exceptions                          │  │
│  │  ✅ Works with self-invocation, private methods, static methods             │  │
│  │  ✅ Batch processing with partial commits                                   │  │
│  │                                                                            │  │
│  │  DISADVANTAGES — Programmatic:                                             │  │
│  │  ❌ TX code mixed with business logic (SRP violation)                       │  │
│  │  ❌ More boilerplate code                                                   │  │
│  │  ❌ Easy to forget commit/rollback (with PlatformTransactionManager)        │  │
│  │  ❌ Harder to read — business logic buried in TX management code            │  │
│  │  ❌ Changing TX behavior requires code changes (not just annotation attrs)  │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  BEST PRACTICE:                                                            │  │
│  │                                                                            │  │
│  │  • Default to @Transactional (declarative) for everything                  │  │
│  │  • Switch to TransactionTemplate ONLY when you need:                       │  │
│  │    - Partial commits (batch processing)                                    │  │
│  │    - Multiple independent TXs in one method                                │  │
│  │    - Conditional rollback without exceptions                               │  │
│  │    - Workaround for self-invocation                                        │  │
│  │  • Avoid raw PlatformTransactionManager unless TransactionTemplate         │  │
│  │    doesn't meet your needs (very rare)                                     │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Side-by-Side Code Comparison:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// SAME operation implemented THREE ways:
// ═══════════════════════════════════════════════════════════════════════════════

// ─── Way 1: DECLARATIVE (@Transactional) ───
@Service
public class OrderServiceDeclarative {

    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(OrderRequest request) {
        Order order = new Order(request);
        order = orderRepository.save(order);
        inventoryService.deductStock(order.getProductId(), order.getQuantity());
        return order;
    }
}

// ─── Way 2: PROGRAMMATIC (TransactionTemplate) ───
@Service
public class OrderServiceTemplate {

    private final TransactionTemplate txTemplate;

    public OrderServiceTemplate(PlatformTransactionManager txManager) {
        this.txTemplate = new TransactionTemplate(txManager);
    }

    public Order createOrder(OrderRequest request) {
        return txTemplate.execute(status -> {
            Order order = new Order(request);
            order = orderRepository.save(order);
            inventoryService.deductStock(order.getProductId(), order.getQuantity());
            return order;
        });
    }
}

// ─── Way 3: PROGRAMMATIC (PlatformTransactionManager directly) ───
@Service
public class OrderServiceManual {

    @Autowired
    private PlatformTransactionManager txManager;

    public Order createOrder(OrderRequest request) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = txManager.getTransaction(def);

        try {
            Order order = new Order(request);
            order = orderRepository.save(order);
            inventoryService.deductStock(order.getProductId(), order.getQuantity());
            txManager.commit(status);
            return order;
        } catch (Exception e) {
            txManager.rollback(status);
            throw e;
        }
    }
}

// All three produce IDENTICAL SQL:
// SET autocommit = 0;
// START TRANSACTION;
// INSERT INTO orders (...) VALUES (...);
// UPDATE inventory SET quantity = ? WHERE product_id = ?;
// COMMIT;   (or ROLLBACK on failure)
```


---

### What is Transaction Propagation?

Transaction Propagation defines **what should happen when a `@Transactional` method calls another `@Transactional` method**. Should the inner method join the existing transaction? Create a new one? Run without any transaction?

When `AbstractPlatformTransactionManager.getTransaction()` is called, the **first thing it checks** is the propagation value. This tells it whether to:
1. **Join** the existing transaction
2. **Create a new** transaction (suspending the existing one)
3. **Run without** any transaction
4. **Throw an exception** if conditions aren't met

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Transaction Propagation — The Core Question:                                    │
│                                                                                  │
│  ServiceA                          ServiceB                                      │
│  ┌─────────────────────────┐       ┌─────────────────────────┐                   │
│  │ @Transactional           │       │ @Transactional(          │                   │
│  │ public void methodA() {  │       │   propagation = ???)     │                   │
│  │                          │       │ public void methodB() {  │                   │
│  │   // some DB operations  │──────→│   // more DB operations  │                   │
│  │   serviceB.methodB();    │       │ }                        │                   │
│  │                          │       └─────────────────────────┘                   │
│  │ }                        │                                                    │
│  └─────────────────────────┘       QUESTION: Does methodB()...                   │
│                                     • JOIN methodA's transaction?                │
│                                     • CREATE a NEW transaction?                  │
│                                     • RUN WITHOUT any transaction?               │
│                                     • THROW an exception?                        │
│                                                                                  │
│  The PROPAGATION value on methodB's @Transactional answers this.                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Internal Code — How Propagation is Checked:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// AbstractPlatformTransactionManager.getTransaction() — Propagation Logic
// (simplified from Spring source)
// ═══════════════════════════════════════════════════════════════════════════════

public final TransactionStatus getTransaction(TransactionDefinition definition) {

    Object transaction = doGetTransaction();  // get current TX state from ThreadLocal

    // ─── CASE 1: A transaction ALREADY EXISTS ───
    if (isExistingTransaction(transaction)) {
        return handleExistingTransaction(definition, transaction);
        // Propagation decides what to do:
        //   REQUIRED     → join existing TX
        //   REQUIRES_NEW → suspend existing, create NEW TX
        //   SUPPORTS     → join existing TX
        //   NOT_SUPPORTED→ suspend existing, run non-transactional
        //   MANDATORY    → join existing TX
        //   NEVER        → THROW exception (TX exists but shouldn't!)
        //   NESTED       → create savepoint within existing TX
    }

    // ─── CASE 2: NO transaction exists ───
    switch (definition.getPropagationBehavior()) {

        case PROPAGATION_REQUIRED:
        case PROPAGATION_REQUIRES_NEW:
        case PROPAGATION_NESTED:
            // Create a NEW transaction
            doBegin(transaction, definition);  // connection.setAutoCommit(false)
            return newTransactionStatus(definition, transaction, true);

        case PROPAGATION_SUPPORTS:
        case PROPAGATION_NOT_SUPPORTED:
            // Run WITHOUT a transaction (non-transactional)
            return emptyTransactionStatus();

        case PROPAGATION_MANDATORY:
            // THROW exception — a TX MUST already exist!
            throw new IllegalTransactionStateException(
                    "No existing transaction found for MANDATORY propagation");

        case PROPAGATION_NEVER:
            // No TX exists, and NEVER doesn't want one → OK, run non-transactional
            return emptyTransactionStatus();
    }
}
```

**Summary Table:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  All 7 Propagation Types at a Glance:                                            │
│                                                                                  │
│  ┌─────────────────┬───────────────────────────┬─────────────────────────────┐    │
│  │ Propagation     │ TX Already Exists          │ No TX Exists                │    │
│  ├─────────────────┼───────────────────────────┼─────────────────────────────┤    │
│  │ REQUIRED        │ Join existing TX           │ Create NEW TX              │    │
│  │ (default)       │                           │                             │    │
│  ├─────────────────┼───────────────────────────┼─────────────────────────────┤    │
│  │ REQUIRES_NEW    │ Suspend existing,          │ Create NEW TX              │    │
│  │                 │ create NEW TX              │                             │    │
│  ├─────────────────┼───────────────────────────┼─────────────────────────────┤    │
│  │ SUPPORTS        │ Join existing TX           │ Run non-transactional      │    │
│  ├─────────────────┼───────────────────────────┼─────────────────────────────┤    │
│  │ NOT_SUPPORTED   │ Suspend existing,          │ Run non-transactional      │    │
│  │                 │ run non-transactional      │                             │    │
│  ├─────────────────┼───────────────────────────┼─────────────────────────────┤    │
│  │ MANDATORY       │ Join existing TX           │ ❌ THROW exception          │    │
│  ├─────────────────┼───────────────────────────┼─────────────────────────────┤    │
│  │ NEVER           │ ❌ THROW exception          │ Run non-transactional      │    │
│  ├─────────────────┼───────────────────────────┼─────────────────────────────┤    │
│  │ NESTED          │ Create SAVEPOINT within    │ Create NEW TX              │    │
│  │                 │ existing TX                │ (same as REQUIRED)          │    │
│  └─────────────────┴───────────────────────────┴─────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 1. Propagation.REQUIRED (Default)

**Behavior:** If a transaction exists → **join it**. If no transaction exists → **create a new one**.

This is the **default** propagation. If you don't specify propagation, you get `REQUIRED`.

**Industry Use Case:** An order service calls a payment service. Both should participate in the **same transaction** — if payment fails, the order is also rolled back.

```java
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentService paymentService;

    @Transactional  // propagation = REQUIRED (default)
    public Order placeOrder(OrderRequest request) {
        // TX-1 created here (no existing TX)
        Order order = new Order(request);
        order.setStatus(OrderStatus.PENDING);
        order = orderRepository.save(order);

        // Calls PaymentService — which also has @Transactional(REQUIRED)
        paymentService.processPayment(order.getId(), request.getAmount());
        // ↑ PaymentService JOINS TX-1 (does NOT create a new TX)

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        return order;
    }
}

@Service
public class PaymentService {

    @Autowired private PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRED)  // default
    public void processPayment(Long orderId, BigDecimal amount) {
        // TX already exists (from OrderService) → JOIN it
        Payment payment = new Payment(orderId, amount, PaymentStatus.COMPLETED);
        paymentRepository.save(payment);
        // If this throws → BOTH order and payment are rolled back
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Propagation.REQUIRED — Flow:                                                    │
│                                                                                  │
│  Controller → orderService.placeOrder()                                          │
│                    │                                                             │
│                    │  No TX exists → CREATE TX-1                                 │
│                    │  connection.setAutoCommit(false)                             │
│                    v                                                             │
│             ┌──────────────────────────────────────────────┐                     │
│             │  TX-1                                         │                     │
│             │                                              │                     │
│             │  INSERT INTO orders (...)                     │                     │
│             │                                              │                     │
│             │  paymentService.processPayment()              │                     │
│             │       │                                      │                     │
│             │       │  TX exists → JOIN TX-1                │                     │
│             │       │  (no new TX created!)                 │                     │
│             │       v                                      │                     │
│             │  INSERT INTO payments (...)                   │                     │
│             │                                              │                     │
│             │  UPDATE orders SET status = 'CONFIRMED'      │                     │
│             │                                              │                     │
│             │  ────────────────────────────────────────     │                     │
│             │  ALL succeed → COMMIT TX-1                    │                     │
│             │  ANY fails → ROLLBACK TX-1 (both tables)     │                     │
│             └──────────────────────────────────────────────┘                     │
│                                                                                  │
│  Key: BOTH OrderService and PaymentService share the SAME connection,            │
│  the SAME transaction. One COMMIT or ROLLBACK covers everything.                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Generated SQL:**

```sql
-- TX-1 created by OrderService.placeOrder()
SET autocommit = 0;
START TRANSACTION;

-- OrderService:
Hibernate: insert into orders (customer_id, status, total_amount) values (?, ?, ?)
           -- Binding: [101, 'PENDING', 2499.99]

-- PaymentService.processPayment() → JOINS TX-1 (no new BEGIN)
Hibernate: insert into payments (order_id, amount, status) values (?, ?, ?)
           -- Binding: [1, 2499.99, 'COMPLETED']

-- Back in OrderService:
Hibernate: update orders set status = ? where id = ?
           -- Binding: ['CONFIRMED', 1]

-- TX-1 committed by OrderService (the TX owner)
COMMIT;

-- If PaymentService threw exception:
-- ROLLBACK;  ← Both INSERT orders and INSERT payments are undone
```

---

#### 2. Propagation.REQUIRES_NEW

**Behavior:** **Always create a NEW transaction**. If a transaction already exists → **suspend** it until the new one completes.

**Industry Use Case:** An audit log must be saved **regardless** of whether the main transaction succeeds or fails. If the order fails, the audit log should still be committed.

```java
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private AuditService auditService;

    @Transactional
    public Order placeOrder(OrderRequest request) {
        // TX-1 created
        Order order = new Order(request);
        order = orderRepository.save(order);

        // Audit runs in its OWN transaction (TX-2)
        auditService.logAction("ORDER_CREATED", order.getId());
        // ↑ TX-1 is SUSPENDED while TX-2 runs
        // TX-2 commits independently
        // TX-1 resumes

        // If this fails → TX-1 rolls back, but audit log (TX-2) is ALREADY committed
        processRiskyOperation(order);

        return order;
    }
}

@Service
public class AuditService {

    @Autowired private AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, Long entityId) {
        // ALWAYS creates TX-2 (suspends TX-1 if it exists)
        AuditLog log = new AuditLog(action, entityId, LocalDateTime.now());
        auditLogRepository.save(log);
        // TX-2 commits here, even if TX-1 later fails
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Propagation.REQUIRES_NEW — Flow:                                                │
│                                                                                  │
│  Controller → orderService.placeOrder()                                          │
│                    │                                                             │
│                    │  No TX → CREATE TX-1                                        │
│                    v                                                             │
│             ┌──────────────────────────────────────────────┐                     │
│             │  TX-1 (OrderService)                          │                     │
│             │                                              │                     │
│             │  INSERT INTO orders (...)                     │                     │
│             │                                              │                     │
│             │  auditService.logAction()                     │                     │
│             │       │                                      │                     │
│             │       │  TX-1 SUSPENDED ⏸️                    │                     │
│             │       v                                      │                     │
│             │  ┌──────────────────────────────────┐        │                     │
│             │  │  TX-2 (AuditService) — NEW TX     │        │                     │
│             │  │                                   │        │                     │
│             │  │  INSERT INTO audit_log (...)       │        │                     │
│             │  │                                   │        │                     │
│             │  │  COMMIT TX-2 ✅                    │        │                     │
│             │  └──────────────────────────────────┘        │                     │
│             │       │                                      │                     │
│             │       │  TX-1 RESUMED ▶️                      │                     │
│             │       v                                      │                     │
│             │  processRiskyOperation() → EXCEPTION!         │                     │
│             │                                              │                     │
│             │  ROLLBACK TX-1 ❌                              │                     │
│             └──────────────────────────────────────────────┘                     │
│                                                                                  │
│  Result:                                                                         │
│    orders table:    INSERT rolled back (order NOT saved)                          │
│    audit_log table: INSERT committed (audit log IS saved) ✅                      │
│                                                                                  │
│  TX-2 is completely INDEPENDENT of TX-1.                                         │
│  TX-1's rollback does NOT affect TX-2's already-committed data.                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Generated SQL:**

```sql
-- TX-1 created by OrderService
SET autocommit = 0;
START TRANSACTION;    -- TX-1 BEGIN

Hibernate: insert into orders (customer_id, status) values (?, ?)
           -- Binding: [101, 'PENDING']

-- TX-1 SUSPENDED — AuditService.logAction() creates TX-2
-- (Spring internally unbinds TX-1's connection from ThreadLocal,
--  gets a NEW connection from the pool for TX-2)

SET autocommit = 0;
START TRANSACTION;    -- TX-2 BEGIN (different connection!)

Hibernate: insert into audit_log (action, entity_id, created_at) values (?, ?, ?)
           -- Binding: ['ORDER_CREATED', 1, '2026-04-26T12:00:00']

COMMIT;               -- TX-2 COMMIT ✅ (audit log saved permanently)

-- TX-1 RESUMED (original connection re-bound to ThreadLocal)
-- processRiskyOperation() throws exception

ROLLBACK;             -- TX-1 ROLLBACK ❌ (order insert undone)
                      -- audit_log is UNAFFECTED (already committed in TX-2)
```

---

#### 3. Propagation.SUPPORTS

**Behavior:** If a transaction exists → **join it**. If no transaction exists → **run non-transactionally** (auto-commit mode).

**Industry Use Case:** A read-only helper method that can work inside or outside a transaction. If called from a transactional context, it benefits from the transaction's isolation; if called standalone, it just reads with auto-commit.

```java
@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private PriceCalculator priceCalculator;

    @Transactional
    public Product updatePrice(Long productId, BigDecimal newPrice) {
        // TX-1 exists
        Product product = productRepository.findById(productId).orElseThrow();
        product.setPrice(newPrice);

        // SUPPORTS: joins TX-1 → reads within same TX isolation
        BigDecimal finalPrice = priceCalculator.calculateWithTax(product);
        product.setFinalPrice(finalPrice);

        return productRepository.save(product);
    }
}

@Service
public class PriceCalculator {

    @Autowired private TaxRateRepository taxRateRepository;

    @Transactional(propagation = Propagation.SUPPORTS)
    public BigDecimal calculateWithTax(Product product) {
        // If TX exists → join it (consistent reads within same TX)
        // If no TX → run non-transactional (auto-commit, each query independent)
        TaxRate rate = taxRateRepository.findByCategory(product.getCategory());
        return product.getPrice().multiply(BigDecimal.ONE.add(rate.getRate()));
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Propagation.SUPPORTS — Two Scenarios:                                           │
│                                                                                  │
│  SCENARIO A: Called FROM a transactional method                                   │
│  ──────────────────────────────────────────────                                   │
│  updatePrice() [TX-1]                                                            │
│       │                                                                          │
│       └──→ priceCalculator.calculateWithTax() → JOINS TX-1                       │
│            (reads within TX-1's isolation level)                                  │
│                                                                                  │
│  SQL:                                                                            │
│  SET autocommit = 0;                                                             │
│  START TRANSACTION;                                                              │
│  SELECT ... FROM products WHERE id = ?                                           │
│  SELECT ... FROM tax_rates WHERE category = ?  ← inside TX-1                     │
│  UPDATE products SET price = ?, final_price = ? WHERE id = ?                     │
│  COMMIT;                                                                         │
│                                                                                  │
│  ──────────────────────────────────────────────                                   │
│  SCENARIO B: Called DIRECTLY (no existing TX)                                     │
│  ──────────────────────────────────────────────                                   │
│  controller → priceCalculator.calculateWithTax() → NO TX, run in auto-commit     │
│                                                                                  │
│  SQL:                                                                            │
│  -- No START TRANSACTION                                                         │
│  SELECT ... FROM tax_rates WHERE category = ?  ← auto-commit (independent query) │
│  -- No COMMIT needed                                                             │
│                                                                                  │
│  SUPPORTS is like saying: "I don't care about transactions, but if one exists,   │
│  I'll participate."                                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 4. Propagation.NOT_SUPPORTED

**Behavior:** **Always run non-transactionally**. If a transaction exists → **suspend** it.

**Industry Use Case:** A heavy reporting query that reads millions of rows. You don't want it to hold a transaction lock, and you don't want it to be affected by the caller's transaction timeout.

```java
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ReportService reportService;

    @Transactional
    public Order placeOrder(OrderRequest request) {
        // TX-1 exists
        Order order = new Order(request);
        order = orderRepository.save(order);

        // Generate report OUTSIDE the transaction (TX-1 suspended)
        reportService.generateDailyReport();
        // ↑ TX-1 suspended, report runs non-transactionally
        // TX-1 resumes after report completes

        order.setStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }
}

@Service
public class ReportService {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateDailyReport() {
        // TX-1 is SUSPENDED — this runs in auto-commit mode
        // Long-running query won't hold TX-1's locks or be killed by TX-1's timeout
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT category, SUM(amount) as total " +
                "FROM orders WHERE created_at >= CURDATE() " +
                "GROUP BY category"
        );
        // Process results... write to report file, etc.
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Propagation.NOT_SUPPORTED — Flow:                                               │
│                                                                                  │
│  orderService.placeOrder()                                                       │
│       │                                                                          │
│       │  TX-1 exists                                                             │
│       v                                                                          │
│  ┌──────────────────────────────────────────────┐                                │
│  │  TX-1                                         │                                │
│  │  INSERT INTO orders (...)                     │                                │
│  │                                              │                                │
│  │  reportService.generateDailyReport()          │                                │
│  │       │                                      │                                │
│  │       │  TX-1 SUSPENDED ⏸️                    │                                │
│  │       v                                      │                                │
│  │  ┌──────────────────────────────────┐        │                                │
│  │  │  NO TRANSACTION                   │        │                                │
│  │  │  (auto-commit mode)               │        │                                │
│  │  │                                   │        │                                │
│  │  │  SELECT ... GROUP BY category     │        │                                │
│  │  │  (each query auto-commits)        │        │                                │
│  │  │                                   │        │                                │
│  │  └──────────────────────────────────┘        │                                │
│  │       │                                      │                                │
│  │       │  TX-1 RESUMED ▶️                      │                                │
│  │       v                                      │                                │
│  │  UPDATE orders SET status = 'CONFIRMED'      │                                │
│  │                                              │                                │
│  │  COMMIT TX-1                                  │                                │
│  └──────────────────────────────────────────────┘                                │
│                                                                                  │
│  The report query runs WITHOUT holding any transaction locks.                    │
│  If the report query takes 30 seconds, it won't cause TX-1 to timeout.           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Generated SQL:**

```sql
-- TX-1 created by OrderService
SET autocommit = 0;
START TRANSACTION;

Hibernate: insert into orders (customer_id, status) values (?, ?)

-- TX-1 SUSPENDED — reportService runs non-transactionally
-- (Spring unbinds TX-1's connection, report uses auto-commit connection)

-- Report query runs in auto-commit mode:
SELECT category, SUM(amount) as total
FROM orders WHERE created_at >= CURDATE()
GROUP BY category;
-- Auto-committed immediately (no explicit BEGIN/COMMIT)

-- TX-1 RESUMED
Hibernate: update orders set status = ? where id = ?
           -- Binding: ['CONFIRMED', 1]

COMMIT;    -- TX-1 commits
```

---

#### 5. Propagation.MANDATORY

**Behavior:** A transaction **MUST already exist**. If no transaction exists → **throw exception**. If transaction exists → **join it**.

**Industry Use Case:** A repository-level method that should NEVER be called outside a transactional context. It enforces that callers always provide a transaction.

```java
@Service
public class WalletService {

    @Autowired private WalletRepository walletRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deductBalance(Long walletId, BigDecimal amount) {
        // MANDATORY: A TX MUST already exist from the caller
        // If no TX → throws IllegalTransactionStateException
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Not enough balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
    }
}

// ─── SCENARIO A: Called from transactional context → WORKS ✅ ───
@Service
public class PaymentService {

    @Autowired private WalletService walletService;
    @Autowired private PaymentRepository paymentRepository;

    @Transactional
    public void processPayment(Long walletId, BigDecimal amount) {
        // TX-1 exists
        walletService.deductBalance(walletId, amount);
        // ↑ MANDATORY: TX exists → joins TX-1 ✅

        paymentRepository.save(new Payment(walletId, amount));
    }
}

// ─── SCENARIO B: Called WITHOUT transaction → EXCEPTION ❌ ───
@RestController
public class WalletController {

    @Autowired private WalletService walletService;

    @PostMapping("/deduct")
    public void deduct(@RequestParam Long walletId, @RequestParam BigDecimal amount) {
        // No @Transactional on this controller method → no TX exists
        walletService.deductBalance(walletId, amount);
        // ↑ MANDATORY: No TX exists → throws IllegalTransactionStateException ❌
        // "No existing transaction found for transaction marked with propagation 'mandatory'"
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Propagation.MANDATORY — Two Scenarios:                                          │
│                                                                                  │
│  SCENARIO A: Called from @Transactional method → ✅ JOINS                        │
│                                                                                  │
│  PaymentService.processPayment() [TX-1]                                          │
│       │                                                                          │
│       └──→ walletService.deductBalance() [MANDATORY]                             │
│            TX exists → JOINS TX-1 ✅                                             │
│            SELECT ... UPDATE ... (within TX-1)                                   │
│                                                                                  │
│  ──────────────────────────────────────────────                                   │
│  SCENARIO B: Called directly (no TX) → ❌ EXCEPTION                              │
│                                                                                  │
│  WalletController.deduct() [no TX]                                               │
│       │                                                                          │
│       └──→ walletService.deductBalance() [MANDATORY]                             │
│            No TX exists → ❌ IllegalTransactionStateException                    │
│            "No existing transaction found for propagation 'mandatory'"            │
│                                                                                  │
│  No SQL executed. Exception thrown BEFORE method body runs.                       │
│                                                                                  │
│  MANDATORY is a safety net: "This method must NEVER run outside a transaction."  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Generated SQL:**

```sql
-- SCENARIO A: Called from PaymentService (TX exists)
SET autocommit = 0;
START TRANSACTION;    -- TX-1 (from PaymentService)

-- walletService.deductBalance() JOINS TX-1
Hibernate: select w.id, w.balance from wallets w where w.id = ?
           -- Binding: [42]
Hibernate: update wallets set balance = ? where id = ?
           -- Binding: [4500.00, 42]

Hibernate: insert into payments (wallet_id, amount) values (?, ?)
           -- Binding: [42, 500.00]

COMMIT;    -- TX-1 commits everything


-- SCENARIO B: Called directly (no TX)
-- NO SQL executed at all!
-- Spring throws IllegalTransactionStateException BEFORE entering the method.
```

---

#### 6. Propagation.NEVER

**Behavior:** Must **NOT** run inside a transaction. If a transaction exists → **throw exception**. If no transaction → **run non-transactionally**.

The **opposite of MANDATORY**.

**Industry Use Case:** A method that calls an external API and should NEVER hold a database transaction open during the call (to prevent connection pool exhaustion).

```java
@Service
public class NotificationService {

    @Autowired private ExternalEmailClient emailClient;

    @Transactional(propagation = Propagation.NEVER)
    public void sendEmail(String to, String subject, String body) {
        // NEVER: If a TX exists → throw exception
        // This ensures we NEVER hold a DB connection while waiting for
        // an external HTTP call (which could take 5-30 seconds)
        emailClient.send(to, subject, body);
        // External API call — we don't want a DB transaction here
    }
}

// ─── SCENARIO A: Called without transaction → WORKS ✅ ───
@RestController
public class NotificationController {

    @Autowired private NotificationService notificationService;

    @PostMapping("/notify")
    public void notify(@RequestBody NotificationRequest req) {
        // No @Transactional → no TX exists
        notificationService.sendEmail(req.getTo(), req.getSubject(), req.getBody());
        // ↑ NEVER: No TX → runs fine ✅
    }
}

// ─── SCENARIO B: Called from transactional method → EXCEPTION ❌ ───
@Service
public class OrderService {

    @Autowired private NotificationService notificationService;

    @Transactional
    public Order placeOrder(OrderRequest request) {
        // TX-1 exists
        Order order = orderRepository.save(new Order(request));

        notificationService.sendEmail(order.getCustomerEmail(),
                "Order Placed", "Your order #" + order.getId());
        // ↑ NEVER: TX exists → throws IllegalTransactionStateException ❌
        // "Existing transaction found for transaction marked with propagation 'never'"

        return order;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Propagation.NEVER — Two Scenarios:                                              │
│                                                                                  │
│  SCENARIO A: No TX exists → ✅ Runs non-transactionally                         │
│                                                                                  │
│  NotificationController.notify() [no TX]                                         │
│       │                                                                          │
│       └──→ notificationService.sendEmail() [NEVER]                               │
│            No TX exists → OK ✅                                                  │
│            Sends email (no DB transaction)                                        │
│                                                                                  │
│  ──────────────────────────────────────────────                                   │
│  SCENARIO B: TX exists → ❌ EXCEPTION                                            │
│                                                                                  │
│  OrderService.placeOrder() [TX-1]                                                │
│       │                                                                          │
│       └──→ notificationService.sendEmail() [NEVER]                               │
│            TX exists → ❌ IllegalTransactionStateException                       │
│            "Existing transaction found for propagation 'never'"                  │
│                                                                                  │
│  NEVER says: "I refuse to run inside any transaction. If you try, I'll fail."    │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐   │
│  │  MANDATORY vs NEVER — Opposites:                                          │   │
│  │                                                                           │   │
│  │  MANDATORY: "A TX MUST exist, or I throw"                                 │   │
│  │  NEVER:     "A TX MUST NOT exist, or I throw"                             │   │
│  └────────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 7. Propagation with PlatformTransactionManager (Programmatic)

You set propagation on a `DefaultTransactionDefinition` and pass it to `transactionManager.getTransaction()`.

**Industry Example — Batch Processing with REQUIRES_NEW per Batch:**

```java
@Service
public class DataMigrationService {

    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private UserRepository userRepository;
    @Autowired private MigrationLogRepository migrationLogRepository;

    public MigrationResult migrateUsers(List<UserDTO> users) {
        int successCount = 0;
        int failCount = 0;

        for (UserDTO dto : users) {
            // Each user migrated in its OWN transaction (REQUIRES_NEW equivalent)
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            def.setTimeout(5);

            TransactionStatus status = transactionManager.getTransaction(def);

            try {
                User user = new User();
                user.setEmail(dto.getEmail());
                user.setName(dto.getName());
                user.setMigratedAt(LocalDateTime.now());
                userRepository.save(user);

                transactionManager.commit(status);
                successCount++;
            } catch (Exception e) {
                transactionManager.rollback(status);
                failCount++;
            }
        }

        // Log result in a MANDATORY-like check:
        // We expect a transaction from the caller, but since we're programmatic
        // we can check ourselves
        DefaultTransactionDefinition logDef = new DefaultTransactionDefinition();
        logDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionStatus logStatus = transactionManager.getTransaction(logDef);
        try {
            migrationLogRepository.save(
                    new MigrationLog(successCount, failCount, LocalDateTime.now()));
            transactionManager.commit(logStatus);
        } catch (Exception e) {
            transactionManager.rollback(logStatus);
        }

        return new MigrationResult(successCount, failCount);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Programmatic Propagation with PlatformTransactionManager:                       │
│                                                                                  │
│  DefaultTransactionDefinition def = new DefaultTransactionDefinition();           │
│                                                                                  │
│  // Set propagation:                                                             │
│  def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);         │
│  def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);     │
│  def.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);         │
│  def.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);    │
│  def.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);        │
│  def.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);            │
│  def.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);           │
│                                                                                  │
│  // Also set other properties:                                                   │
│  def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);          │
│  def.setTimeout(10);                                                             │
│  def.setReadOnly(true);                                                          │
│                                                                                  │
│  // Begin transaction with these settings:                                       │
│  TransactionStatus status = transactionManager.getTransaction(def);              │
│                                                                                  │
│  // ... business logic ...                                                       │
│                                                                                  │
│  transactionManager.commit(status);  // or .rollback(status)                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Generated SQL — Per-User Migration:**

```sql
-- User 1: REQUIRES_NEW → new TX
SET autocommit = 0;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
Hibernate: insert into users (email, name, migrated_at) values (?, ?, ?)
           -- Binding: ['alice@example.com', 'Alice', '2026-04-26T12:00:00']
COMMIT;    -- ✅ User 1 saved

-- User 2: REQUIRES_NEW → another new TX
SET autocommit = 0;
START TRANSACTION;
Hibernate: insert into users (email, name, migrated_at) values (?, ?, ?)
           -- Binding: ['bob@example.com', 'Bob', '2026-04-26T12:00:01']
COMMIT;    -- ✅ User 2 saved

-- User 3: REQUIRES_NEW → fails
SET autocommit = 0;
START TRANSACTION;
Hibernate: insert into users (email, name, migrated_at) values (?, ?, ?)
           -- ERROR: Duplicate email constraint violation!
ROLLBACK;  -- ❌ Only User 3 rolled back. Users 1 & 2 are safe.

-- Migration log: REQUIRED → new TX
START TRANSACTION;
Hibernate: insert into migration_log (success_count, fail_count, created_at)
           values (?, ?, ?)
           -- Binding: [2, 1, '2026-04-26T12:00:02']
COMMIT;
```

---

#### 8. Propagation with TransactionTemplate (Programmatic)

`TransactionTemplate` extends `DefaultTransactionDefinition`, so you can set propagation directly on it. You can create **multiple TransactionTemplate instances** with different propagation settings.

**Industry Example — Order with Independent Audit and Notification:**

```java
@Service
public class OrderProcessingService {

    private final TransactionTemplate requiredTxTemplate;
    private final TransactionTemplate requiresNewTxTemplate;
    private final TransactionTemplate notSupportedTxTemplate;

    @Autowired private OrderRepository orderRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ExternalNotificationClient notificationClient;

    public OrderProcessingService(PlatformTransactionManager txManager) {

        // REQUIRED — joins or creates TX (for main business logic)
        this.requiredTxTemplate = new TransactionTemplate(txManager);
        this.requiredTxTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRED);

        // REQUIRES_NEW — always creates independent TX (for audit)
        this.requiresNewTxTemplate = new TransactionTemplate(txManager);
        this.requiresNewTxTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // NOT_SUPPORTED — no TX (for external API calls)
        this.notSupportedTxTemplate = new TransactionTemplate(txManager);
        this.notSupportedTxTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }

    public OrderResult processOrder(OrderRequest request) {

        // TX-1: REQUIRED — Create order (main business transaction)
        Order order = requiredTxTemplate.execute(status -> {
            Order o = new Order(request);
            o.setStatus(OrderStatus.PENDING);
            return orderRepository.save(o);
        });

        // TX-2: REQUIRES_NEW — Audit log (independent, always committed)
        requiresNewTxTemplate.execute(status -> {
            auditLogRepository.save(new AuditLog("ORDER_CREATED", order.getId()));
            return null;
        });

        // NO TX: NOT_SUPPORTED — External notification (no DB TX held open)
        notSupportedTxTemplate.execute(status -> {
            // No transaction here — safe to make slow external API calls
            notificationClient.sendOrderConfirmation(order.getCustomerEmail());
            return null;
        });

        // TX-3: REQUIRED — Confirm order
        requiredTxTemplate.execute(status -> {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            return null;
        });

        return new OrderResult(order, true);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  TransactionTemplate with Different Propagations — Flow:                         │
│                                                                                  │
│  processOrder(request)                                                           │
│       │                                                                          │
│       v                                                                          │
│  ┌────────────────────────────────────┐                                          │
│  │ TX-1: REQUIRED                      │  requiredTxTemplate.execute(...)         │
│  │  INSERT INTO orders (...)           │                                          │
│  │  COMMIT ✅                          │                                          │
│  └────────────────┬───────────────────┘                                          │
│                   │                                                              │
│                   v                                                              │
│  ┌────────────────────────────────────┐                                          │
│  │ TX-2: REQUIRES_NEW                  │  requiresNewTxTemplate.execute(...)      │
│  │  INSERT INTO audit_log (...)        │  (independent TX)                        │
│  │  COMMIT ✅                          │                                          │
│  └────────────────┬───────────────────┘                                          │
│                   │                                                              │
│                   v                                                              │
│  ┌────────────────────────────────────┐                                          │
│  │ NO TX: NOT_SUPPORTED                │  notSupportedTxTemplate.execute(...)     │
│  │  External API call                  │  (no DB transaction held)                │
│  │  (auto-commit if any DB ops)        │                                          │
│  └────────────────┬───────────────────┘                                          │
│                   │                                                              │
│                   v                                                              │
│  ┌────────────────────────────────────┐                                          │
│  │ TX-3: REQUIRED                      │  requiredTxTemplate.execute(...)         │
│  │  UPDATE orders SET status =         │                                          │
│  │    'CONFIRMED'                      │                                          │
│  │  COMMIT ✅                          │                                          │
│  └────────────────────────────────────┘                                          │
│                                                                                  │
│  4 steps, 3 independent transactions + 1 non-transactional block.                │
│  If notification fails, order and audit log are already committed.               │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  TransactionTemplate Propagation Setup:                                    │  │
│  │                                                                            │  │
│  │  TransactionTemplate tt = new TransactionTemplate(txManager);              │  │
│  │                                                                            │  │
│  │  tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);    │  │
│  │  tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);│  │
│  │  tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);    │  │
│  │  tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED)│  │
│  │  tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);   │  │
│  │  tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);       │  │
│  │  tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);      │  │
│  │                                                                            │  │
│  │  // Also settable:                                                         │  │
│  │  tt.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);     │  │
│  │  tt.setTimeout(10);                                                        │  │
│  │  tt.setReadOnly(true);                                                     │  │
│  │                                                                            │  │
│  │  // Execute:                                                               │  │
│  │  Result r = tt.execute(status -> { ... return result; });                  │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Generated SQL:**

```sql
-- TX-1: REQUIRED (create order)
SET autocommit = 0;
START TRANSACTION;
Hibernate: insert into orders (customer_id, status, total_amount) values (?, ?, ?)
           -- Binding: [101, 'PENDING', 2499.99]
COMMIT;

-- TX-2: REQUIRES_NEW (audit log — independent)
SET autocommit = 0;
START TRANSACTION;
Hibernate: insert into audit_log (action, entity_id, created_at) values (?, ?, ?)
           -- Binding: ['ORDER_CREATED', 1, '2026-04-26T12:00:00']
COMMIT;

-- NO TX: NOT_SUPPORTED (external notification)
-- No SQL generated. External HTTP call only.
-- If there were DB queries, they'd run in auto-commit mode.

-- TX-3: REQUIRED (confirm order)
SET autocommit = 0;
START TRANSACTION;
Hibernate: update orders set status = ? where id = ?
           -- Binding: ['CONFIRMED', 1]
COMMIT;
```

---

#### Propagation — Quick Reference with Declarative vs Programmatic

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Declarative (@Transactional) vs Programmatic — Setting Propagation:             │
│                                                                                  │
│  ┌───────────────────────────────────────┬───────────────────────────────────┐    │
│  │ Declarative                           │ Programmatic                      │    │
│  ├───────────────────────────────────────┼───────────────────────────────────┤    │
│  │ @Transactional(propagation =          │ def.setPropagationBehavior(       │    │
│  │   Propagation.REQUIRED)               │   TransactionDefinition          │    │
│  │                                       │     .PROPAGATION_REQUIRED);      │    │
│  ├───────────────────────────────────────┼───────────────────────────────────┤    │
│  │ @Transactional(propagation =          │ def.setPropagationBehavior(       │    │
│  │   Propagation.REQUIRES_NEW)           │   TransactionDefinition          │    │
│  │                                       │     .PROPAGATION_REQUIRES_NEW);  │    │
│  ├───────────────────────────────────────┼───────────────────────────────────┤    │
│  │ @Transactional(propagation =          │ def.setPropagationBehavior(       │    │
│  │   Propagation.SUPPORTS)               │   TransactionDefinition          │    │
│  │                                       │     .PROPAGATION_SUPPORTS);      │    │
│  ├───────────────────────────────────────┼───────────────────────────────────┤    │
│  │ @Transactional(propagation =          │ def.setPropagationBehavior(       │    │
│  │   Propagation.NOT_SUPPORTED)          │   TransactionDefinition          │    │
│  │                                       │     .PROPAGATION_NOT_SUPPORTED); │    │
│  ├───────────────────────────────────────┼───────────────────────────────────┤    │
│  │ @Transactional(propagation =          │ def.setPropagationBehavior(       │    │
│  │   Propagation.MANDATORY)              │   TransactionDefinition          │    │
│  │                                       │     .PROPAGATION_MANDATORY);     │    │
│  ├───────────────────────────────────────┼───────────────────────────────────┤    │
│  │ @Transactional(propagation =          │ def.setPropagationBehavior(       │    │
│  │   Propagation.NEVER)                  │   TransactionDefinition          │    │
│  │                                       │     .PROPAGATION_NEVER);         │    │
│  ├───────────────────────────────────────┼───────────────────────────────────┤    │
│  │ @Transactional(propagation =          │ def.setPropagationBehavior(       │    │
│  │   Propagation.NESTED)                 │   TransactionDefinition          │    │
│  │                                       │     .PROPAGATION_NESTED);        │    │
│  └───────────────────────────────────────┴───────────────────────────────────┘    │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  WHEN TO USE WHICH PROPAGATION:                                            │  │
│  │                                                                            │  │
│  │  REQUIRED      → Default. 95% of methods. "Join or create."               │  │
│  │  REQUIRES_NEW  → Audit logs, notification logs, metrics — must survive     │  │
│  │                  even if caller rolls back.                                │  │
│  │  SUPPORTS      → Read-only helpers that work with or without TX.          │  │
│  │  NOT_SUPPORTED → Heavy reports, external API calls — don't hold TX locks. │  │
│  │  MANDATORY     → Safety: enforce that caller provides a TX.               │  │
│  │  NEVER         → Safety: enforce that caller does NOT have a TX.          │  │
│  │  NESTED        → Partial rollback within a larger TX (savepoints).        │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### What is Isolation Level?

Isolation Level controls **how the changes made by one transaction are visible to other transactions running concurrently**. It answers the question: "If Transaction A modifies a row, can Transaction B see that modification before A commits?"

In ACID, the **I (Isolation)** defines the degree to which one transaction must be isolated from the effects of other concurrent transactions. Higher isolation = more safety but less concurrency. Lower isolation = more concurrency but more anomalies.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Isolation Level — The Core Problem:                                             │
│                                                                                  │
│  Thread-1 (TX-A)                     Thread-2 (TX-B)                             │
│  ┌────────────────────────┐          ┌────────────────────────┐                  │
│  │ BEGIN;                  │          │ BEGIN;                  │                  │
│  │                         │          │                         │                  │
│  │ UPDATE accounts         │          │ SELECT balance          │                  │
│  │ SET balance = 500       │          │ FROM accounts           │                  │
│  │ WHERE id = 1;           │          │ WHERE id = 1;           │                  │
│  │                         │          │                         │                  │
│  │ -- balance was 1000     │          │ -- What does TX-B see?  │                  │
│  │ -- now changed to 500   │          │ -- 1000 (old value)?    │                  │
│  │ -- but NOT committed    │          │ -- 500 (uncommitted)?   │                  │
│  │                         │          │                         │                  │
│  │ COMMIT;  (or ROLLBACK?) │          │ COMMIT;                 │                  │
│  └────────────────────────┘          └────────────────────────┘                  │
│                                                                                  │
│  The ISOLATION LEVEL answers: "What does TX-B see?"                              │
│                                                                                  │
│  READ_UNCOMMITTED → TX-B sees 500 (uncommitted!) — DANGEROUS                    │
│  READ_COMMITTED   → TX-B sees 1000 (only committed data) — SAFE                 │
│  REPEATABLE_READ  → TX-B sees 1000 (and will ALWAYS see 1000) — SAFER           │
│  SERIALIZABLE     → TX-B WAITS until TX-A commits/rolls back — SAFEST           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Internal Code — How Spring Sets Isolation Level:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: DataSourceTransactionManager.doBegin() — sets isolation
// (simplified from Spring source)
// ═══════════════════════════════════════════════════════════════════════════════

@Override
protected void doBegin(Object transaction, TransactionDefinition definition) {

    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

    Connection con = obtainDataSource().getConnection();
    txObject.setConnectionHolder(new ConnectionHolder(con));

    // ─── SET ISOLATION LEVEL ───
    Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(
            con, definition);
    // Internally this calls:
    //   if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
    //       previousIsolationLevel = con.getTransactionIsolation();
    //       con.setTransactionIsolation(definition.getIsolationLevel());
    //       // ↑ Maps to JDBC: connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
    //       // ↑ Which sends to DB: SET TRANSACTION ISOLATION LEVEL READ COMMITTED
    //   }

    txObject.setPreviousIsolationLevel(previousIsolationLevel);

    con.setAutoCommit(false);  // BEGIN TRANSACTION

    // ... bind connection to ThreadLocal via TransactionSynchronizationManager
}

// ═══════════════════════════════════════════════════════════════════════════════
// How @Transactional(isolation = ...) maps to JDBC:
// ═══════════════════════════════════════════════════════════════════════════════

// @Transactional(isolation = Isolation.READ_UNCOMMITTED)
//   → con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED)   // value = 1
//   → SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED

// @Transactional(isolation = Isolation.READ_COMMITTED)
//   → con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)     // value = 2
//   → SET TRANSACTION ISOLATION LEVEL READ COMMITTED

// @Transactional(isolation = Isolation.REPEATABLE_READ)
//   → con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)    // value = 4
//   → SET TRANSACTION ISOLATION LEVEL REPEATABLE READ

// @Transactional(isolation = Isolation.SERIALIZABLE)
//   → con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)       // value = 8
//   → SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
```

**Industry Use Case — E-Commerce Inventory:**

```java
@Service
public class InventoryService {

    @Autowired private ProductRepository productRepository;

    // Default isolation (DB default) — for most operations
    @Transactional
    public void updateStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    // READ_COMMITTED — for balance checks (don't read dirty data)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int getStock(Long productId) {
        return productRepository.findById(productId)
                .map(Product::getStock)
                .orElse(0);
    }

    // REPEATABLE_READ — for price calculations (price must not change mid-TX)
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BigDecimal calculateOrderTotal(List<Long> productIds) {
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : productIds) {
            Product p = productRepository.findById(id).orElseThrow();
            total = total.add(p.getPrice());
            // Even if another TX changes the price, THIS TX will see
            // the same price it saw the first time (repeatable read)
        }
        return total;
    }

    // SERIALIZABLE — for critical financial operations
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transferStock(Long fromProductId, Long toProductId, int qty) {
        Product from = productRepository.findById(fromProductId).orElseThrow();
        Product to = productRepository.findById(toProductId).orElseThrow();
        from.setStock(from.getStock() - qty);
        to.setStock(to.getStock() + qty);
        productRepository.save(from);
        productRepository.save(to);
        // SERIALIZABLE ensures no other TX can read or write these rows
        // until this TX completes — as if TXs run one after another
    }
}
```

---

#### 1. Dirty Read Problem

A **Dirty Read** occurs when Transaction B reads data that Transaction A has **modified but NOT yet committed**. If Transaction A later **rolls back**, Transaction B has read data that **never actually existed** in the database.

**Real-World Use Case — Bank Account:**
A customer has $1000 in their account. Transaction A (a transfer) deducts $500 but hasn't committed yet. Transaction B (balance check) reads the balance as $500. Then Transaction A rolls back (transfer failed). The customer's real balance is $1000, but Transaction B reported $500 — a **dirty read**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dirty Read — Timeline:                                                          │
│                                                                                  │
│  Time    TX-A (Transfer)                    TX-B (Balance Check)                 │
│  ─────   ─────────────────────────────      ──────────────────────────────       │
│                                                                                  │
│  T1      BEGIN;                                                                  │
│          SELECT balance FROM accounts                                            │
│          WHERE id = 1;                                                           │
│          -- Result: balance = 1000                                               │
│                                                                                  │
│  T2      UPDATE accounts                                                         │
│          SET balance = 500                                                       │
│          WHERE id = 1;                                                           │
│          -- balance changed to 500                                               │
│          -- but NOT COMMITTED yet!                                               │
│                                                                                  │
│  T3                                         BEGIN;                               │
│                                             SELECT balance FROM accounts         │
│                                             WHERE id = 1;                        │
│                                                                                  │
│                                             ┌─────────────────────────────┐      │
│                                             │ 🔴 DIRTY READ!              │      │
│                                             │ Result: balance = 500       │      │
│                                             │ (reads UNCOMMITTED data!)   │      │
│                                             └─────────────────────────────┘      │
│                                                                                  │
│  T4      ROLLBACK;                                                               │
│          -- Transfer failed!                                                     │
│          -- balance is back to 1000                                              │
│                                                                                  │
│  T5                                         -- TX-B used balance = 500           │
│                                             -- but real balance is 1000!         │
│                                             -- TX-B made decisions based on      │
│                                             -- data that NEVER existed!          │
│                                             COMMIT;                              │
│                                                                                  │
│  Concurrency Level: HIGHEST (no locks, no waits)                                 │
│  Data Integrity:    LOWEST  (reading phantom/ghost data)                         │
│                                                                                  │
│  ✅ SOLUTION: Use READ_COMMITTED or higher isolation level.                      │
│  At READ_COMMITTED, TX-B would see balance = 1000 (only committed data).         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example — Dirty Read Happening:**

```java
// ─── TX-A: Transfer (runs on Thread-1) ───
@Service
public class TransferService {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void transfer(Long fromAccountId, BigDecimal amount) {
        // T1: Deduct from sender
        jdbcTemplate.update(
                "UPDATE accounts SET balance = balance - ? WHERE id = ?",
                amount, fromAccountId);
        // balance changed in DB buffer — NOT committed yet

        // T2: Some slow validation...
        validateTransfer(fromAccountId, amount);  // takes 5 seconds

        // T3: If validation fails → ROLLBACK
        // But TX-B may have already read the deducted balance!
    }
}

// ─── TX-B: Balance Check (runs on Thread-2) ───
@Service
public class BalanceService {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public BigDecimal getBalance(Long accountId) {
        // 🔴 DIRTY READ: Reads TX-A's uncommitted UPDATE!
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?",
                BigDecimal.class, accountId);
        // Returns 500 instead of 1000!
    }
}
```

```sql
-- T1: TX-A begins and deducts
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
START TRANSACTION;  -- TX-A
UPDATE accounts SET balance = balance - 500 WHERE id = 1;
-- balance in buffer: 500 (was 1000). NOT committed.

-- T3: TX-B reads UNCOMMITTED data (DIRTY READ!)
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
START TRANSACTION;  -- TX-B
SELECT balance FROM accounts WHERE id = 1;
-- Returns: 500  ← 🔴 DIRTY! This is uncommitted data from TX-A!
COMMIT;  -- TX-B

-- T4: TX-A rolls back
ROLLBACK;  -- TX-A
-- balance reverts to 1000
-- But TX-B already used 500! DATA INCONSISTENCY!
```

**How to Solve:** Use `Isolation.READ_COMMITTED` or higher:

```java
@Transactional(isolation = Isolation.READ_COMMITTED)  // ✅ Prevents dirty reads
public BigDecimal getBalance(Long accountId) {
    // Now TX-B will ONLY see committed data
    // It will see balance = 1000 (the last committed value)
    return jdbcTemplate.queryForObject(
            "SELECT balance FROM accounts WHERE id = ?",
            BigDecimal.class, accountId);
}
```

---

#### 2. Non-Repeatable Read Problem

A **Non-Repeatable Read** occurs when Transaction B reads the **same row twice** and gets **different values** because Transaction A **committed an UPDATE** between the two reads.

**Real-World Use Case — Flight Ticket Pricing:**
A customer checks a flight price ($300), adds it to cart, then proceeds to checkout. Between the price check and checkout, another transaction changes the price to $450. When the checkout reads the price again, it's $450 — the price **changed between two reads in the same transaction**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Non-Repeatable Read — Timeline:                                                 │
│                                                                                  │
│  Time    TX-A (Price Update)                TX-B (Booking Flow)                  │
│  ─────   ─────────────────────────────      ──────────────────────────────       │
│                                                                                  │
│  T1                                         BEGIN;                               │
│                                             SELECT price FROM flights            │
│                                             WHERE flight_id = 'AA101';           │
│                                             -- Result: price = 300               │
│                                             -- (shows $300 to customer)          │
│                                                                                  │
│  T2      BEGIN;                                                                  │
│          UPDATE flights                                                          │
│          SET price = 450                                                         │
│          WHERE flight_id = 'AA101';                                              │
│          COMMIT;                                                                 │
│          -- ✅ Committed! Price is now 450.                                      │
│                                                                                  │
│  T3                                         -- Customer clicks "Checkout"        │
│                                             -- System reads price AGAIN:         │
│                                             SELECT price FROM flights            │
│                                             WHERE flight_id = 'AA101';           │
│                                                                                  │
│                                             ┌─────────────────────────────┐      │
│                                             │ 🟡 NON-REPEATABLE READ!     │      │
│                                             │ Result: price = 450         │      │
│                                             │ (was 300 at T1, now 450!)   │      │
│                                             │ Same row, different value!  │      │
│                                             └─────────────────────────────┘      │
│                                                                                  │
│  T4                                         -- Charges customer $450             │
│                                             -- Customer expected $300!           │
│                                             COMMIT;                              │
│                                                                                  │
│  Concurrency Level: HIGH (only committed reads, but rows can change)             │
│  Data Integrity:    MEDIUM (no dirty reads, but same query returns diff results) │
│                                                                                  │
│  ✅ SOLUTION: Use REPEATABLE_READ or higher isolation level.                     │
│  At REPEATABLE_READ, TX-B's second SELECT would still return 300.                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example:**

```java
// ─── TX-A: Airline updates price (Thread-1) ───
@Service
public class FlightPricingService {

    @Transactional
    public void updatePrice(String flightId, BigDecimal newPrice) {
        // T2: Update and commit
        flightRepository.updatePrice(flightId, newPrice);
        // After commit, price = 450 in DB
    }
}

// ─── TX-B: Customer booking (Thread-2) ───
@Service
public class BookingService {

    @Autowired private FlightRepository flightRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)  // allows non-repeatable reads!
    public Booking bookFlight(String flightId, Long customerId) {
        // T1: First read — price = 300
        Flight flight = flightRepository.findById(flightId).orElseThrow();
        BigDecimal displayPrice = flight.getPrice();  // 300

        // ... customer reviews booking details, takes 10 seconds ...
        processCustomerReview(customerId);

        // T3: Second read — price = 450 (TX-A committed between T1 and T3!)
        flight = flightRepository.findById(flightId).orElseThrow();
        BigDecimal checkoutPrice = flight.getPrice();  // 🟡 450! Non-repeatable read!

        if (!displayPrice.equals(checkoutPrice)) {
            throw new PriceChangedException("Price changed from " +
                    displayPrice + " to " + checkoutPrice);
        }

        return createBooking(flight, customerId, checkoutPrice);
    }
}
```

```sql
-- T1: TX-B first read
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;  -- TX-B
SELECT price FROM flights WHERE flight_id = 'AA101';
-- Returns: 300

-- T2: TX-A updates and commits
START TRANSACTION;  -- TX-A
UPDATE flights SET price = 450 WHERE flight_id = 'AA101';
COMMIT;  -- TX-A committed ✅

-- T3: TX-B second read — SAME query, DIFFERENT result!
SELECT price FROM flights WHERE flight_id = 'AA101';
-- Returns: 450  ← 🟡 NON-REPEATABLE READ!
-- Was 300 at T1, now 450 at T3 (because TX-A committed in between)

COMMIT;  -- TX-B
```

**How to Solve:** Use `Isolation.REPEATABLE_READ`:

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)  // ✅ Prevents non-repeatable reads
public Booking bookFlight(String flightId, Long customerId) {
    // T1: First read — price = 300
    Flight flight = flightRepository.findById(flightId).orElseThrow();
    // T3: Second read — STILL price = 300 (even though TX-A committed 450!)
    // REPEATABLE_READ guarantees the SAME value for the same row within a TX
    flight = flightRepository.findById(flightId).orElseThrow();
    // price is still 300 ✅
}
```

---

#### 3. Phantom Read Problem

A **Phantom Read** occurs when Transaction B executes the **same range query twice** and gets **different sets of rows** because Transaction A **INSERT**ed or **DELETE**d rows that match the query between the two reads.

The key difference from non-repeatable read: **non-repeatable read** is about the same row returning different values; **phantom read** is about **new/deleted rows appearing/disappearing**.

**Real-World Use Case — Seat Availability:**
A booking system counts available seats on a flight (10 seats). While the customer is confirming, another transaction books 3 seats. The second count shows only 7 seats — **3 phantom rows disappeared**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Phantom Read — Timeline:                                                        │
│                                                                                  │
│  Time    TX-A (New Bookings)                TX-B (Seat Count)                    │
│  ─────   ─────────────────────────────      ──────────────────────────────       │
│                                                                                  │
│  T1                                         BEGIN;                               │
│                                             SELECT COUNT(*) FROM bookings        │
│                                             WHERE flight_id = 'AA101'            │
│                                             AND status = 'CONFIRMED';            │
│                                             -- Result: 40 bookings               │
│                                             -- Available = 200 - 40 = 160 seats  │
│                                                                                  │
│  T2      BEGIN;                                                                  │
│          INSERT INTO bookings                                                    │
│            (flight_id, customer_id, status)                                      │
│          VALUES ('AA101', 501, 'CONFIRMED'),                                     │
│                 ('AA101', 502, 'CONFIRMED'),                                     │
│                 ('AA101', 503, 'CONFIRMED');                                     │
│          COMMIT;  ✅                                                             │
│          -- 3 NEW rows added to bookings table                                   │
│                                                                                  │
│  T3                                         -- System re-counts for validation:  │
│                                             SELECT COUNT(*) FROM bookings        │
│                                             WHERE flight_id = 'AA101'            │
│                                             AND status = 'CONFIRMED';            │
│                                                                                  │
│                                             ┌─────────────────────────────┐      │
│                                             │ 👻 PHANTOM READ!            │      │
│                                             │ Result: 43 bookings         │      │
│                                             │ (was 40 at T1, now 43!)     │      │
│                                             │ 3 "phantom" rows appeared!  │      │
│                                             └─────────────────────────────┘      │
│                                                                                  │
│  T4                                         -- Available = 200 - 43 = 157 seats  │
│                                             -- But we told customer 160!         │
│                                             COMMIT;                              │
│                                                                                  │
│  Concurrency Level: MEDIUM (existing rows locked, but range not locked)          │
│  Data Integrity:    MEDIUM-HIGH (rows can appear/disappear in range queries)     │
│                                                                                  │
│  ✅ SOLUTION: Use SERIALIZABLE isolation level.                                  │
│  At SERIALIZABLE, TX-A's INSERT would BLOCK until TX-B completes.                │
│  (or TX-B would see a consistent snapshot — depending on DB implementation)      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example:**

```java
// ─── TX-A: New bookings (Thread-1) ───
@Service
public class BookingService {

    @Transactional
    public void bulkBook(String flightId, List<Long> customerIds) {
        // T2: Insert new bookings and commit
        for (Long customerId : customerIds) {
            bookingRepository.save(new Booking(flightId, customerId, "CONFIRMED"));
        }
        // After commit: 3 new rows in bookings table
    }
}

// ─── TX-B: Availability check (Thread-2) ───
@Service
public class AvailabilityService {

    @Autowired private BookingRepository bookingRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ)  // still allows phantom reads!
    public int getAvailableSeats(String flightId, int totalCapacity) {
        // T1: First count
        int bookedCount = bookingRepository.countByFlightIdAndStatus(
                flightId, "CONFIRMED");
        // Result: 40

        // ... processing time ...
        calculatePricing(flightId);

        // T3: Second count — PHANTOM READ!
        int recount = bookingRepository.countByFlightIdAndStatus(
                flightId, "CONFIRMED");
        // Result: 43  ← 👻 3 phantom rows appeared!
        // REPEATABLE_READ prevents existing rows from changing,
        // but CANNOT prevent NEW rows from appearing in range queries

        return totalCapacity - recount;
    }
}
```

```sql
-- T1: TX-B counts bookings
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;  -- TX-B
SELECT COUNT(*) FROM bookings
WHERE flight_id = 'AA101' AND status = 'CONFIRMED';
-- Returns: 40

-- T2: TX-A inserts new bookings and commits
START TRANSACTION;  -- TX-A
INSERT INTO bookings (flight_id, customer_id, status)
VALUES ('AA101', 501, 'CONFIRMED'),
       ('AA101', 502, 'CONFIRMED'),
       ('AA101', 503, 'CONFIRMED');
COMMIT;  -- TX-A ✅

-- T3: TX-B re-counts — same query, different row count!
SELECT COUNT(*) FROM bookings
WHERE flight_id = 'AA101' AND status = 'CONFIRMED';
-- Returns: 43  ← 👻 PHANTOM READ! 3 new rows appeared!

COMMIT;  -- TX-B

-- NOTE: In MySQL/InnoDB with REPEATABLE_READ, phantom reads are actually
-- prevented by "gap locks" (next-key locking). This is MySQL-specific.
-- In PostgreSQL and standard SQL, REPEATABLE_READ does NOT prevent phantoms.
```

**How to Solve:** Use `Isolation.SERIALIZABLE`:

```java
@Transactional(isolation = Isolation.SERIALIZABLE)  // ✅ Prevents phantom reads
public int getAvailableSeats(String flightId, int totalCapacity) {
    // T1: First count = 40
    int count1 = bookingRepository.countByFlightIdAndStatus(flightId, "CONFIRMED");
    // T3: Second count = 40 (TX-A's INSERT is BLOCKED or invisible)
    int count2 = bookingRepository.countByFlightIdAndStatus(flightId, "CONFIRMED");
    // Both counts are 40 ✅ — no phantom rows
    return totalCapacity - count2;
}
```

---

#### 4. Database Locking — Shared Lock (Read Lock) and Exclusive Lock (Write Lock)

Database locks are the **mechanism** that isolation levels use to prevent concurrency anomalies. There are two fundamental lock types:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Two Types of Locks:                                                             │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐     │
│  │ SHARED LOCK (S-Lock) — Read Lock                                        │     │
│  │                                                                         │     │
│  │ • Acquired when a transaction READS a row (SELECT)                      │     │
│  │ • Multiple transactions can hold S-Locks on the SAME row simultaneously │     │
│  │ • Allows OTHER transactions to also READ (acquire S-Lock)               │     │
│  │ • BLOCKS other transactions from WRITING (acquiring X-Lock)             │     │
│  │ • Purpose: "I'm reading this — don't modify it while I'm reading"      │     │
│  │                                                                         │     │
│  │ SQL: SELECT ... LOCK IN SHARE MODE  (MySQL)                             │     │
│  │      SELECT ... FOR SHARE           (PostgreSQL)                        │     │
│  └──────────────────────────────────────────────────────────────────────────┘     │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐     │
│  │ EXCLUSIVE LOCK (X-Lock) — Write Lock                                    │     │
│  │                                                                         │     │
│  │ • Acquired when a transaction WRITES a row (INSERT/UPDATE/DELETE)        │     │
│  │ • Only ONE transaction can hold an X-Lock on a row at a time            │     │
│  │ • BLOCKS all other transactions from READING (S-Lock) and WRITING       │     │
│  │ • Purpose: "I'm modifying this — nobody can read or write it"           │     │
│  │                                                                         │     │
│  │ SQL: SELECT ... FOR UPDATE          (MySQL & PostgreSQL)                │     │
│  │      UPDATE / DELETE / INSERT       (automatically acquires X-Lock)     │     │
│  └──────────────────────────────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Lock Compatibility Matrix:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Lock Compatibility Matrix:                                                      │
│                                                                                  │
│  "If TX-A holds Lock X on a row, can TX-B acquire Lock Y on the SAME row?"      │
│                                                                                  │
│  ┌──────────────────────┬─────────────────────┬─────────────────────┐            │
│  │ TX-A holds \  TX-B   │ S-Lock (Read)       │ X-Lock (Write)      │            │
│  │ wants →               │                     │                     │            │
│  ├──────────────────────┼─────────────────────┼─────────────────────┤            │
│  │ S-Lock (Read)         │ ✅ YES (compatible)  │ ❌ NO (blocks/waits) │            │
│  │                      │ Both can read       │ TX-B must WAIT      │            │
│  ├──────────────────────┼─────────────────────┼─────────────────────┤            │
│  │ X-Lock (Write)        │ ❌ NO (blocks/waits) │ ❌ NO (blocks/waits) │            │
│  │                      │ TX-B must WAIT      │ TX-B must WAIT      │            │
│  └──────────────────────┴─────────────────────┴─────────────────────┘            │
│                                                                                  │
│  Summary:                                                                        │
│  • S + S = ✅ (multiple readers allowed)                                         │
│  • S + X = ❌ (reader blocks writer)                                             │
│  • X + S = ❌ (writer blocks reader)                                             │
│  • X + X = ❌ (writer blocks writer)                                             │
│                                                                                  │
│  Key Insight: EXCLUSIVE lock is truly exclusive — it blocks EVERYTHING.           │
│  SHARED lock only blocks writes, not other reads.                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Multithreading Example — Two Transactions Competing for Locks:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Two Transactions — Lock Interaction:                                            │
│                                                                                  │
│  SCENARIO 1: Both READ (S-Lock + S-Lock) → ✅ No blocking                       │
│  ──────────────────────────────────────────────                                   │
│  Time   Thread-1 (TX-A)                    Thread-2 (TX-B)                       │
│  T1     SELECT ... WHERE id=1              SELECT ... WHERE id=1                 │
│         Acquires S-Lock on row 1 ✅         Acquires S-Lock on row 1 ✅           │
│         (reads balance = 1000)             (reads balance = 1000)                │
│  T2     -- Both proceed. No blocking.      -- Both proceed.                      │
│                                                                                  │
│  ──────────────────────────────────────────────                                   │
│  SCENARIO 2: One READS, One WRITES (S-Lock + X-Lock) → ❌ Blocking              │
│  ──────────────────────────────────────────────                                   │
│  Time   Thread-1 (TX-A)                    Thread-2 (TX-B)                       │
│  T1     SELECT ... WHERE id=1                                                    │
│         Acquires S-Lock on row 1 ✅                                               │
│  T2                                        UPDATE ... WHERE id=1                 │
│                                            Wants X-Lock on row 1                 │
│                                            ❌ BLOCKED! (S-Lock held by TX-A)     │
│                                            ⏳ Waiting...                          │
│  T3     COMMIT;                                                                  │
│         Releases S-Lock ✅                                                        │
│  T4                                        X-Lock granted ✅                      │
│                                            UPDATE proceeds.                      │
│                                                                                  │
│  ──────────────────────────────────────────────                                   │
│  SCENARIO 3: Both WRITE (X-Lock + X-Lock) → ❌ Blocking                         │
│  ──────────────────────────────────────────────                                   │
│  Time   Thread-1 (TX-A)                    Thread-2 (TX-B)                       │
│  T1     UPDATE ... SET balance=500         UPDATE ... SET balance=300            │
│         WHERE id=1                         WHERE id=1                            │
│         Acquires X-Lock on row 1 ✅         Wants X-Lock on row 1                │
│                                            ❌ BLOCKED! (X-Lock held by TX-A)     │
│  T2     -- TX-A does more work...          ⏳ Waiting...                          │
│  T3     COMMIT;                                                                  │
│         Releases X-Lock ✅                                                        │
│  T4                                        X-Lock granted ✅                      │
│                                            UPDATE proceeds.                      │
│                                            (balance becomes 300, not 500)        │
│                                                                                  │
│  ──────────────────────────────────────────────                                   │
│  DEADLOCK SCENARIO: Both want what the other has → 💀                            │
│  ──────────────────────────────────────────────                                   │
│  Time   Thread-1 (TX-A)                    Thread-2 (TX-B)                       │
│  T1     X-Lock on row 1 ✅                  X-Lock on row 2 ✅                    │
│  T2     Wants X-Lock on row 2              Wants X-Lock on row 1                 │
│         ❌ BLOCKED (TX-B holds row 2)       ❌ BLOCKED (TX-A holds row 1)         │
│         ⏳ Waiting for TX-B...              ⏳ Waiting for TX-A...                 │
│                                                                                  │
│         💀 DEADLOCK! Both waiting for each other forever!                        │
│         DB detects → kills one TX with "Deadlock found" error                    │
│         Killed TX gets rolled back; surviving TX proceeds.                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**When Locks Are Released — Depends on Isolation Level:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  When Are Locks Released?                                                        │
│                                                                                  │
│  ┌──────────────────────┬──────────────────────────────────────────────────┐      │
│  │ Isolation Level       │ When S-Lock (Read Lock) is Released              │      │
│  ├──────────────────────┼──────────────────────────────────────────────────┤      │
│  │ READ_UNCOMMITTED     │ No S-Lock acquired at all (reads without lock)   │      │
│  ├──────────────────────┼──────────────────────────────────────────────────┤      │
│  │ READ_COMMITTED       │ Released IMMEDIATELY after the SELECT completes  │      │
│  │                      │ (short-duration lock)                            │      │
│  ├──────────────────────┼──────────────────────────────────────────────────┤      │
│  │ REPEATABLE_READ      │ Held until END of transaction (COMMIT/ROLLBACK)  │      │
│  │                      │ (long-duration lock)                             │      │
│  ├──────────────────────┼──────────────────────────────────────────────────┤      │
│  │ SERIALIZABLE         │ Held until END of transaction + RANGE LOCKS on   │      │
│  │                      │ index ranges (prevents phantom inserts)          │      │
│  └──────────────────────┴──────────────────────────────────────────────────┘      │
│                                                                                  │
│  X-Lock (Write Lock) is ALWAYS held until COMMIT/ROLLBACK at all levels.         │
│                                                                                  │
│  Key Insight: The difference between isolation levels is primarily about          │
│  HOW LONG shared (read) locks are held.                                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5. Isolation Levels — Values, Defaults, and Anomaly Matrix

**Spring `Isolation` enum values and their JDBC constants:**

```java
// Spring's Isolation enum:
public enum Isolation {
    DEFAULT(-1),                // Use the DB's default isolation
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),   // 1
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),       // 2
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),     // 4
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);           // 8
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Isolation Levels — Anomaly Matrix:                                              │
│                                                                                  │
│  ┌──────────────────────┬────────────┬──────────────────┬──────────────┬────────┐│
│  │ Isolation Level       │ Dirty Read │ Non-Repeatable   │ Phantom Read │ Concur-││
│  │                      │            │ Read             │              │ rency  ││
│  ├──────────────────────┼────────────┼──────────────────┼──────────────┼────────┤│
│  │ READ_UNCOMMITTED     │ ✅ Possible │ ✅ Possible       │ ✅ Possible   │ ⬆⬆⬆⬆   ││
│  │                      │            │                  │              │ Highest││
│  ├──────────────────────┼────────────┼──────────────────┼──────────────┼────────┤│
│  │ READ_COMMITTED       │ ❌ Prevented│ ✅ Possible       │ ✅ Possible   │ ⬆⬆⬆    ││
│  │                      │            │                  │              │ High   ││
│  ├──────────────────────┼────────────┼──────────────────┼──────────────┼────────┤│
│  │ REPEATABLE_READ      │ ❌ Prevented│ ❌ Prevented      │ ✅ Possible*  │ ⬆⬆     ││
│  │                      │            │                  │              │ Medium ││
│  ├──────────────────────┼────────────┼──────────────────┼──────────────┼────────┤│
│  │ SERIALIZABLE         │ ❌ Prevented│ ❌ Prevented      │ ❌ Prevented  │ ⬆      ││
│  │                      │            │                  │              │ Lowest ││
│  └──────────────────────┴────────────┴──────────────────┴──────────────┴────────┘│
│                                                                                  │
│  * MySQL/InnoDB's REPEATABLE_READ also prevents phantoms via gap locks.           │
│    Standard SQL and PostgreSQL's REPEATABLE_READ do NOT prevent phantoms.         │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────────┐ │
│  │                   Safety ──────────────────────► Higher                     │ │
│  │  READ_UNCOMMITTED → READ_COMMITTED → REPEATABLE_READ → SERIALIZABLE        │ │
│  │                   Concurrency ─────────────────► Lower                     │ │
│  └──────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Default Isolation Level Per Database:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Default Isolation Level Per Database:                                            │
│                                                                                  │
│  ┌────────────────────────────┬────────────────────────────────────────────────┐  │
│  │ Database                   │ Default Isolation Level                        │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ MySQL (InnoDB)             │ REPEATABLE_READ                               │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ PostgreSQL                 │ READ_COMMITTED                                │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ Oracle                     │ READ_COMMITTED                                │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ SQL Server                 │ READ_COMMITTED                                │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ H2 (in-memory)            │ READ_COMMITTED                                │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ SQLite                     │ SERIALIZABLE                                  │  │
│  └────────────────────────────┴────────────────────────────────────────────────┘  │
│                                                                                  │
│  Important: When Spring uses @Transactional(isolation = Isolation.DEFAULT),       │
│  it does NOT set any isolation level — it lets the DB use its own default.        │
│                                                                                  │
│  To check current isolation in your DB:                                           │
│  MySQL:      SELECT @@transaction_isolation;                                     │
│  PostgreSQL: SHOW default_transaction_isolation;                                 │
│  Oracle:     SELECT * FROM V$TRANSACTION;                                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 6. Isolation.READ_UNCOMMITTED

**Behavior:** The **lowest** isolation level. Transactions can read data that has been **modified but NOT yet committed** by other transactions.

**Prevents:** Nothing.
**Allows:** Dirty reads ✅, Non-repeatable reads ✅, Phantom reads ✅

**Concurrency Level:** **HIGHEST** — almost no locking, maximum throughput.

**Locking Strategy:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  READ_UNCOMMITTED — Locking Behavior:                                            │
│                                                                                  │
│  READ operations:                                                                │
│    • NO shared (S) locks acquired at all                                         │
│    • Reads directly from the buffer/dirty pages                                  │
│    • Ignores other transactions' exclusive (X) locks for reading                 │
│    • Can see uncommitted changes in real-time                                    │
│                                                                                  │
│  WRITE operations:                                                               │
│    • X-Lock acquired (same as all isolation levels)                              │
│    • Held until COMMIT/ROLLBACK                                                  │
│                                                                                  │
│  Lock Timeline:                                                                  │
│  TX-B: SELECT balance FROM accounts WHERE id = 1;                                │
│        │                                                                         │
│        ├── No S-Lock acquired                                                    │
│        ├── Reads directly from data page (even if modified by TX-A)              │
│        └── Done. Nothing to release.                                             │
│                                                                                  │
│  Because no read locks are taken:                                                │
│    • Writers are NEVER blocked by readers                                        │
│    • Readers are NEVER blocked by writers                                        │
│    • Maximum concurrency, minimum safety                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why Does READ_UNCOMMITTED Exist?**

1. **Approximate analytics/reporting** — When you need rough counts or sums and absolute accuracy doesn't matter
2. **Monitoring dashboards** — Real-time metrics where a 0.01% error is acceptable
3. **Large table scans** — Reading millions of rows without blocking any writers
4. **Queue length estimation** — "Approximately how many jobs are pending?"

**Industry Use Case — Real-Time Dashboard Metrics:**

```java
@Service
public class DashboardService {

    @Autowired private JdbcTemplate jdbcTemplate;

    // READ_UNCOMMITTED: We want FAST approximate counts for a dashboard.
    // It's OK if the numbers are slightly off — they refresh every 5 seconds.
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
    public DashboardMetrics getLiveMetrics() {

        // These queries run WITHOUT acquiring any shared locks.
        // They can read uncommitted data — but that's fine for a dashboard.

        long totalOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE created_at >= CURDATE()",
                Long.class);

        BigDecimal totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payments " +
                "WHERE status = 'COMPLETED' AND created_at >= CURDATE()",
                BigDecimal.class);

        long pendingShipments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status = 'PENDING_SHIPMENT'",
                Long.class);

        return new DashboardMetrics(totalOrders, totalRevenue, pendingShipments);
    }
}
```

**Internal Code — How Spring Sets READ_UNCOMMITTED:**

```java
// @Transactional(isolation = Isolation.READ_UNCOMMITTED) triggers:

// Step 1: TransactionInterceptor reads the isolation attribute
TransactionAttribute txAttr = ...;  // isolation = READ_UNCOMMITTED

// Step 2: DataSourceTransactionManager.doBegin()
Connection con = dataSource.getConnection();
con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);  // value = 1
// ↑ This sends to MySQL:
// SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;

con.setAutoCommit(false);  // BEGIN TRANSACTION
```

**Generated Raw SQL:**

```sql
-- Spring internally executes:
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SET autocommit = 0;
START TRANSACTION;

-- TX-B: Dashboard query (no S-locks acquired!)
SELECT COUNT(*) FROM orders WHERE created_at >= CURDATE();
-- Returns: 1547 (includes 3 uncommitted INSERTs from TX-A)
-- ↑ 🔴 Dirty read! But acceptable for dashboard metrics.

SELECT COALESCE(SUM(amount), 0) FROM payments
WHERE status = 'COMPLETED' AND created_at >= CURDATE();
-- Returns: 45230.50 (includes an uncommitted UPDATE from TX-C that changed
-- a payment from 100 to 200 — adds an extra 100)
-- ↑ 🔴 Dirty read! But we're showing approximate revenue anyway.

SELECT COUNT(*) FROM orders WHERE status = 'PENDING_SHIPMENT';
-- Returns: 23

COMMIT;
-- No locks held at any point during these reads.
-- Writers (TX-A, TX-C) were NEVER blocked.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  READ_UNCOMMITTED — Dirty Read in Action:                                        │
│                                                                                  │
│  Time   TX-A (Order Creation)              TX-B (Dashboard — READ_UNCOMMITTED)   │
│  ─────  ──────────────────────             ──────────────────────────────────     │
│                                                                                  │
│  T1     BEGIN;                                                                   │
│         INSERT INTO orders (...)                                                 │
│         VALUES (..., 'PENDING');                                                 │
│         -- NOT committed yet!                                                    │
│                                                                                  │
│  T2                                        BEGIN;                                │
│                                            SELECT COUNT(*) FROM orders           │
│                                            WHERE created_at >= CURDATE();        │
│                                            -- 🔴 INCLUDES TX-A's uncommitted row!│
│                                            -- Returns: 1548 (instead of 1547)    │
│                                                                                  │
│  T3     ROLLBACK;                          -- TX-B used count 1548               │
│         -- Order insert undone!            -- Real count was 1547                 │
│                                            -- Dashboard showed 1548              │
│                                            -- Refreshes in 5 sec anyway ¯\_(ツ)_/¯│
│                                            COMMIT;                               │
│                                                                                  │
│  For dashboards, this is ACCEPTABLE.                                             │
│  For financial transactions, this is DANGEROUS.                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 7. Isolation.READ_COMMITTED

**Behavior:** A transaction can only read data that has been **committed** by other transactions. Uncommitted (dirty) data is invisible.

**Prevents:** Dirty reads ❌
**Allows:** Non-repeatable reads ✅, Phantom reads ✅

**Concurrency Level:** **HIGH** — short-lived read locks, good throughput.

This is the **default isolation level** for PostgreSQL, Oracle, SQL Server, and H2.

**Locking Strategy:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  READ_COMMITTED — Locking Behavior:                                              │
│                                                                                  │
│  READ operations:                                                                │
│    • S-Lock acquired at the START of the SELECT                                  │
│    • S-Lock released IMMEDIATELY after the SELECT completes                      │
│    • This means: between two SELECTs in the same TX,                             │
│      another TX can UPDATE and COMMIT the same row                               │
│    • Result: non-repeatable reads are possible                                   │
│                                                                                  │
│  WRITE operations:                                                               │
│    • X-Lock acquired (same as all levels)                                        │
│    • Held until COMMIT/ROLLBACK                                                  │
│                                                                                  │
│  Lock Timeline:                                                                  │
│                                                                                  │
│  TX-B: SELECT #1              SELECT #2                                          │
│        ├──S-Lock──┤           ├──S-Lock──┤                                       │
│        Acquire  Release       Acquire  Release                                   │
│                    ↑                                                             │
│                    │ Gap! No lock held here.                                     │
│                    │ TX-A can UPDATE and COMMIT in this gap!                      │
│                    │ → Non-repeatable read!                                       │
│                                                                                  │
│  PostgreSQL/Oracle approach (MVCC — no S-locks at all):                          │
│    • Instead of S-Locks, reads see a SNAPSHOT of committed data                  │
│    • Each SELECT sees the latest COMMITTED version at the time of that SELECT    │
│    • Two SELECTs in the same TX can see different committed versions              │
│    • Result: same effect — non-repeatable reads possible, but no lock contention │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why Does READ_COMMITTED Exist?**

1. **Best balance** for most OLTP applications — prevents dirty reads without hurting concurrency
2. **Default for PostgreSQL/Oracle** — industry standard for transactional applications
3. **Sufficient for most CRUD** — rarely do you read the same row twice in one transaction
4. **Low lock contention** — S-locks are released immediately, writers rarely blocked

**Industry Use Case — E-Commerce Order Processing:**

```java
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryRepository inventoryRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order placeOrder(Long productId, int quantity) {

        // Read 1: Check inventory (S-Lock acquired and RELEASED immediately)
        Product product = inventoryRepository.findById(productId).orElseThrow();
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Not enough stock");
        }
        // S-Lock released! Another TX could update stock RIGHT NOW.

        // ... build order, validate address, calculate shipping (takes time) ...
        Order order = buildOrder(product, quantity);

        // Read 2: Re-check inventory (new S-Lock, new read)
        product = inventoryRepository.findById(productId).orElseThrow();
        // 🟡 NON-REPEATABLE READ possible here!
        // Stock might have changed if another TX committed an UPDATE between
        // Read 1 and Read 2.

        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Stock changed — not enough");
        }

        // Deduct stock (X-Lock acquired, held until COMMIT)
        product.setStock(product.getStock() - quantity);
        inventoryRepository.save(product);
        order.setStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }
}
```

**Internal Code — How Spring Sets READ_COMMITTED:**

```java
// @Transactional(isolation = Isolation.READ_COMMITTED) triggers:

Connection con = dataSource.getConnection();
con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);  // value = 2

// MySQL:      SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
// PostgreSQL: SET TRANSACTION ISOLATION LEVEL READ COMMITTED;  (this is the default)
// Oracle:     (this is the default, no SET needed)

con.setAutoCommit(false);  // BEGIN TRANSACTION
```

**Generated Raw SQL:**

```sql
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SET autocommit = 0;
START TRANSACTION;  -- TX-B

-- Read 1: Check inventory (S-Lock acquired and released immediately)
Hibernate: select p.id, p.name, p.stock, p.price
           from products p where p.id = ?
           -- Binding: [42]
           -- Result: {id=42, name='Laptop', stock=10, price=999.99}
           -- S-Lock RELEASED immediately after SELECT completes

-- ... time passes (order building, validation) ...

-- Meanwhile, TX-A runs:
-- START TRANSACTION;
-- UPDATE products SET stock = 3 WHERE id = 42;  -- X-Lock acquired
-- COMMIT;  -- stock is now 3, committed ✅

-- Read 2: Re-check inventory (new S-Lock, new committed snapshot)
Hibernate: select p.id, p.name, p.stock, p.price
           from products p where p.id = ?
           -- Binding: [42]
           -- Result: {id=42, name='Laptop', stock=3, price=999.99}
           -- 🟡 stock was 10 at Read 1, now 3 at Read 2! (Non-repeatable read)
           -- But this is COMMITTED data — NOT dirty. TX-A committed its update.

-- Deduct stock (X-Lock held until COMMIT)
Hibernate: update products set stock = ? where id = ?
           -- Binding: [1, 42]  -- 3 - 2 = 1

Hibernate: insert into orders (product_id, quantity, status) values (?, ?, ?)
           -- Binding: [42, 2, 'CONFIRMED']

COMMIT;  -- TX-B (releases X-Lock)
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  READ_COMMITTED — Non-Repeatable Read in Action:                                 │
│                                                                                  │
│  Time   TX-A (Stock Update)                TX-B (Order — READ_COMMITTED)         │
│  ─────  ──────────────────────             ──────────────────────────────         │
│                                                                                  │
│  T1                                        BEGIN;                                │
│                                            SELECT stock FROM products            │
│                                            WHERE id = 42;                        │
│                                            -- stock = 10 ✅ (committed data)     │
│                                            -- S-Lock acquired and RELEASED       │
│                                                                                  │
│  T2     BEGIN;                                                                   │
│         UPDATE products SET stock = 3                                            │
│         WHERE id = 42;                                                           │
│         -- X-Lock acquired                                                       │
│         COMMIT; ✅                                                               │
│         -- stock is now 3 in DB                                                  │
│                                                                                  │
│  T3                                        SELECT stock FROM products            │
│                                            WHERE id = 42;                        │
│                                            -- stock = 3 🟡 (non-repeatable!)     │
│                                            -- But it IS committed data ✅        │
│                                            -- No dirty read, just changed value  │
│                                                                                  │
│  T4                                        UPDATE products SET stock = 1          │
│                                            WHERE id = 42;                        │
│                                            COMMIT;                               │
│                                                                                  │
│  ❌ Dirty Read:          PREVENTED (TX-B never sees uncommitted data)            │
│  🟡 Non-Repeatable Read: POSSIBLE  (stock changed from 10 to 3 between reads)   │
│  👻 Phantom Read:        POSSIBLE  (new rows can appear in range queries)        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8. Isolation.REPEATABLE_READ

**Behavior:** Once a transaction reads a row, it will **always see the same value** for that row throughout the transaction, even if other transactions commit updates to it. The read is "repeatable."

**Prevents:** Dirty reads ❌, Non-repeatable reads ❌
**Allows:** Phantom reads ✅ (except in MySQL InnoDB which uses gap locks to prevent phantoms too)

**Concurrency Level:** **MEDIUM** — read locks held longer, more blocking.

This is the **default isolation level** for MySQL (InnoDB).

**Locking Strategy:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  REPEATABLE_READ — Locking Behavior:                                             │
│                                                                                  │
│  READ operations:                                                                │
│    • S-Lock acquired at the START of the SELECT                                  │
│    • S-Lock held until END of transaction (COMMIT/ROLLBACK)                      │
│    • No other TX can UPDATE or DELETE this row while S-Lock is held              │
│    • Result: reading the same row always returns the same value ✅               │
│                                                                                  │
│  WRITE operations:                                                               │
│    • X-Lock acquired (same as all levels)                                        │
│    • Held until COMMIT/ROLLBACK                                                  │
│                                                                                  │
│  Lock Timeline:                                                                  │
│                                                                                  │
│  TX-B: SELECT #1              SELECT #2                    COMMIT                │
│        ├────────── S-Lock held for entire transaction ──────────┤                │
│                                                                                  │
│        TX-A tries UPDATE in between?  ❌ BLOCKED until TX-B commits!             │
│        → Same value guaranteed on second SELECT ✅                               │
│                                                                                  │
│  Compare with READ_COMMITTED:                                                    │
│  TX-B: SELECT #1   SELECT #2                                                    │
│        ├──S──┤      ├──S──┤     (locks released between reads)                   │
│               ↑                 (TX-A can UPDATE in the gap!)                    │
│                                                                                  │
│  MySQL InnoDB approach (MVCC + Gap Locks):                                       │
│    • Uses CONSISTENT SNAPSHOT from the FIRST read                                │
│    • All subsequent reads see data as of that snapshot                            │
│    • Gap locks prevent INSERTs in index ranges → prevents phantoms too            │
│    • No S-locks needed for reads → better concurrency than locking approach       │
│                                                                                  │
│  PostgreSQL approach (MVCC Snapshot):                                             │
│    • Snapshot taken at FIRST statement (not at BEGIN)                             │
│    • All reads see data as of that snapshot                                       │
│    • Does NOT use gap locks → phantoms still possible in edge cases              │
│    • On write conflict → throws serialization error, TX must retry               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why Does REPEATABLE_READ Exist?**

1. **Financial calculations** — When you read account balance multiple times and need the same value every time
2. **Report generation** — When a report reads data multiple times for cross-validation
3. **Inventory reservation** — Ensure the stock count doesn't change while you're processing
4. **MySQL default** — Deemed a good balance between safety and performance for InnoDB

**Industry Use Case — Financial Report Generation:**

```java
@Service
public class FinancialReportService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public FinancialReport generateMonthlyReport(Long accountId, YearMonth month) {

        // Read 1: Get account balance
        Account account = accountRepository.findById(accountId).orElseThrow();
        BigDecimal currentBalance = account.getBalance();  // 10,000

        // Read transactions for the month
        List<Transaction> txns = transactionRepository
                .findByAccountIdAndMonth(accountId, month);
        BigDecimal totalDebits = txns.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = txns.stream()
                .filter(t -> t.getType() == TransactionType.CREDIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Read 2: Re-read balance for cross-validation
        account = accountRepository.findById(accountId).orElseThrow();
        BigDecimal rereadBalance = account.getBalance();

        // With REPEATABLE_READ: currentBalance == rereadBalance ALWAYS ✅
        // Even if another TX committed a balance change between Read 1 and Read 2
        assert currentBalance.equals(rereadBalance);

        BigDecimal openingBalance = currentBalance
                .subtract(totalCredits)
                .add(totalDebits);

        return new FinancialReport(accountId, month,
                openingBalance, totalDebits, totalCredits, currentBalance);
    }
}
```

**Internal Code — How Spring Sets REPEATABLE_READ:**

```java
// @Transactional(isolation = Isolation.REPEATABLE_READ) triggers:

Connection con = dataSource.getConnection();
con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);  // value = 4

// MySQL:      SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;  (this is the default)
// PostgreSQL: SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;

con.setAutoCommit(false);  // BEGIN TRANSACTION
// MySQL InnoDB: first SELECT establishes the CONSISTENT SNAPSHOT
```

**Generated Raw SQL:**

```sql
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SET autocommit = 0;
START TRANSACTION;  -- TX-B (REPEATABLE_READ)

-- Read 1: Get account balance
-- (MySQL: establishes CONSISTENT SNAPSHOT at this point)
Hibernate: select a.id, a.balance, a.name
           from accounts a where a.id = ?
           -- Binding: [101]
           -- Result: {id=101, balance=10000.00, name='Alice'}
           -- S-Lock on row held until COMMIT (or snapshot established)

-- Read transactions
Hibernate: select t.id, t.account_id, t.amount, t.type, t.created_at
           from transactions t
           where t.account_id = ? and t.created_at between ? and ?
           -- Binding: [101, '2026-04-01', '2026-04-30']

-- Meanwhile, TX-A runs:
-- START TRANSACTION;
-- UPDATE accounts SET balance = 12000 WHERE id = 101;
-- COMMIT; ✅
-- But TX-B will NOT see this change!

-- Read 2: Re-read balance (REPEATABLE READ guarantees same value!)
Hibernate: select a.id, a.balance, a.name
           from accounts a where a.id = ?
           -- Binding: [101]
           -- Result: {id=101, balance=10000.00, name='Alice'}
           --                          ↑ STILL 10000! ✅
           -- Even though TX-A committed balance=12000, TX-B's snapshot
           -- was established at T1 and doesn't see TX-A's change.

COMMIT;  -- TX-B
-- After COMMIT, the NEXT transaction will see balance=12000.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  REPEATABLE_READ — Consistent Reads:                                             │
│                                                                                  │
│  Time   TX-A (Balance Update)              TX-B (Report — REPEATABLE_READ)       │
│  ─────  ──────────────────────             ──────────────────────────────         │
│                                                                                  │
│  T1                                        BEGIN;                                │
│                                            SELECT balance FROM accounts          │
│                                            WHERE id = 101;                       │
│                                            -- balance = 10000 ✅                 │
│                                            -- (snapshot established)             │
│                                                                                  │
│  T2     BEGIN;                                                                   │
│         UPDATE accounts SET balance = 12000                                      │
│         WHERE id = 101;                                                          │
│         COMMIT; ✅                                                               │
│         -- Committed! Real balance is 12000.                                     │
│                                                                                  │
│  T3                                        SELECT balance FROM accounts          │
│                                            WHERE id = 101;                       │
│                                            -- balance = 10000 ✅                 │
│                                            -- STILL 10000! Repeatable! ✅        │
│                                            -- TX-B reads from its snapshot       │
│                                                                                  │
│  T4                                        COMMIT;                               │
│                                            -- Snapshot released.                 │
│                                            -- Next TX will see 12000.            │
│                                                                                  │
│  ❌ Dirty Read:          PREVENTED                                               │
│  ❌ Non-Repeatable Read: PREVENTED (balance = 10000 in both reads) ✅            │
│  👻 Phantom Read:        POSSIBLE* (new rows can appear in range queries)        │
│                                                                                  │
│  * MySQL InnoDB: Also prevents phantoms via gap locks.                           │
│  * PostgreSQL:   Does NOT prevent phantoms.                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 9. Isolation.SERIALIZABLE

**Behavior:** The **highest** isolation level. Transactions execute as if they were running **one after another** (serially), even though they may actually run concurrently. Completely eliminates all concurrency anomalies.

**Prevents:** Dirty reads ❌, Non-repeatable reads ❌, Phantom reads ❌
**Allows:** Nothing — all anomalies prevented.

**Concurrency Level:** **LOWEST** — heavy locking, transactions block each other frequently.

**Locking Strategy:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  SERIALIZABLE — Locking Behavior:                                                │
│                                                                                  │
│  READ operations:                                                                │
│    • S-Lock on ALL read rows held until COMMIT/ROLLBACK                          │
│    • RANGE LOCKS (Gap Locks) on index ranges — prevents INSERTs in ranges        │
│    • SELECT ... WHERE price > 100  →  locks the INDEX RANGE for price > 100     │
│    • No new rows matching the WHERE clause can be inserted by other TXs          │
│                                                                                  │
│  WRITE operations:                                                               │
│    • X-Lock acquired (same as all levels)                                        │
│    • Held until COMMIT/ROLLBACK                                                  │
│                                                                                  │
│  Lock Types Used:                                                                │
│    • Row-level S-Locks and X-Locks (same as REPEATABLE_READ)                     │
│    • Range Locks / Gap Locks (NEW — blocks INSERTs in index gaps)                │
│    • Next-Key Locks (combination of row lock + gap lock)                         │
│    • Predicate Locks (lock based on WHERE clause predicate)                      │
│                                                                                  │
│  Lock Timeline:                                                                  │
│                                                                                  │
│  TX-B: SELECT WHERE category='electronics'                      COMMIT           │
│        ├──── S-Lock on ALL matching rows ────────────────────────┤               │
│        ├──── Gap Lock on index range for category='electronics' ─┤               │
│                                                                                  │
│        TX-A tries INSERT INTO products (category='electronics')?                 │
│        ❌ BLOCKED! Gap lock prevents insert until TX-B commits.                  │
│        → No phantom rows can appear ✅                                           │
│                                                                                  │
│  PostgreSQL approach (SSI — Serializable Snapshot Isolation):                     │
│    • Uses MVCC snapshots (like REPEATABLE_READ) + dependency tracking            │
│    • Detects "dangerous structures" (read-write conflicts between TXs)           │
│    • If conflict detected → throws serialization error, TX must retry            │
│    • More optimistic — less blocking, but more rollbacks                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why Does SERIALIZABLE Exist?**

1. **Financial transfers** — Double-spending prevention, account-to-account transfers
2. **Seat booking** — Ensuring no overbooking (exactly N seats, never N+1 bookings)
3. **Inventory management** — Stock can never go negative, even under extreme concurrency
4. **Regulatory compliance** — Banking systems that require zero anomalies by law

**Industry Use Case — Bank Account Transfer (Double-Spend Prevention):**

```java
@Service
public class BankTransferService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransferLogRepository transferLogRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransferResult transfer(Long fromAccountId, Long toAccountId,
                                   BigDecimal amount) {

        // SERIALIZABLE: These reads lock the rows AND the index ranges
        // No other TX can read, update, or insert conflicting data

        Account from = accountRepository.findById(fromAccountId).orElseThrow();
        Account to = accountRepository.findById(toAccountId).orElseThrow();

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Balance " + from.getBalance() + " < transfer " + amount);
        }

        // Deduct and credit
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);

        // Log the transfer
        TransferLog log = new TransferLog(fromAccountId, toAccountId,
                amount, LocalDateTime.now());
        transferLogRepository.save(log);

        // At SERIALIZABLE, if another TX tried to transfer from the same
        // account concurrently, it would be BLOCKED until this TX completes.
        // This prevents double-spending: only ONE TX at a time can
        // deduct from the same account.

        return new TransferResult(true, from.getBalance(), to.getBalance());
    }
}
```

**Internal Code — How Spring Sets SERIALIZABLE:**

```java
// @Transactional(isolation = Isolation.SERIALIZABLE) triggers:

Connection con = dataSource.getConnection();
con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);  // value = 8

// MySQL:      SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
// PostgreSQL: SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

con.setAutoCommit(false);  // BEGIN TRANSACTION
```

**Generated Raw SQL:**

```sql
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SET autocommit = 0;
START TRANSACTION;  -- TX-B (SERIALIZABLE)

-- Read from-account (S-Lock + Range Lock on account row)
Hibernate: select a.id, a.balance, a.name
           from accounts a where a.id = ?
           -- Binding: [101]
           -- Result: {id=101, balance=10000.00, name='Alice'}
           -- S-Lock on row id=101 — held until COMMIT

-- Read to-account (S-Lock + Range Lock on account row)
Hibernate: select a.id, a.balance, a.name
           from accounts a where a.id = ?
           -- Binding: [202]
           -- Result: {id=202, balance=5000.00, name='Bob'}
           -- S-Lock on row id=202 — held until COMMIT

-- If TX-A tries to read/update account 101 or 202 RIGHT NOW:
-- TX-A: UPDATE accounts SET balance = 9000 WHERE id = 101;
-- ❌ BLOCKED! TX-B holds S-Lock on row 101.
-- TX-A must WAIT until TX-B commits or rolls back.

-- Deduct from sender
Hibernate: update accounts set balance = ? where id = ?
           -- Binding: [7000.00, 101]  -- 10000 - 3000
           -- S-Lock upgraded to X-Lock on row 101

-- Credit to receiver
Hibernate: update accounts set balance = ? where id = ?
           -- Binding: [8000.00, 202]  -- 5000 + 3000
           -- S-Lock upgraded to X-Lock on row 202

-- Log the transfer
Hibernate: insert into transfer_log (from_account, to_account, amount, created_at)
           values (?, ?, ?, ?)
           -- Binding: [101, 202, 3000.00, '2026-04-26T14:30:00']

COMMIT;  -- TX-B
-- ALL locks released. TX-A can now proceed with its update.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  SERIALIZABLE — Complete Protection (No Anomalies):                              │
│                                                                                  │
│  Time   TX-A (Another Transfer)            TX-B (Transfer — SERIALIZABLE)        │
│  ─────  ──────────────────────             ──────────────────────────────         │
│                                                                                  │
│  T1                                        BEGIN;                                │
│                                            SELECT ... FROM accounts              │
│                                            WHERE id = 101;                       │
│                                            -- balance = 10000                    │
│                                            -- S-Lock on row 101 ✅               │
│                                                                                  │
│  T2     BEGIN;                                                                   │
│         UPDATE accounts                                                          │
│         SET balance = balance - 5000                                             │
│         WHERE id = 101;                                                          │
│         ❌ BLOCKED! TX-B holds S-Lock on row 101!                                │
│         ⏳ Waiting for TX-B to commit/rollback...                                 │
│                                                                                  │
│  T3                                        UPDATE accounts                       │
│                                            SET balance = 7000                    │
│                                            WHERE id = 101;  -- 10000 - 3000     │
│                                            -- X-Lock on row 101                  │
│                                                                                  │
│  T4                                        UPDATE accounts                       │
│                                            SET balance = 8000                    │
│                                            WHERE id = 202;  -- 5000 + 3000      │
│                                                                                  │
│  T5                                        COMMIT; ✅                            │
│                                            -- All locks released                 │
│                                                                                  │
│  T6     -- TX-A UNBLOCKED!                                                       │
│         -- TX-A reads balance = 7000 (TX-B's committed value)                    │
│         -- TX-A: 7000 - 5000 = 2000 ✅ (correct!)                               │
│         UPDATE accounts SET balance = 2000 WHERE id = 101;                       │
│         COMMIT;                                                                  │
│                                                                                  │
│  WITHOUT SERIALIZABLE:                                                           │
│    TX-A might have read balance = 10000 (before TX-B committed)                  │
│    TX-A: 10000 - 5000 = 5000                                                    │
│    TX-B: 10000 - 3000 = 7000                                                    │
│    Last writer wins: balance = 5000 or 7000 (one transfer LOST!)                 │
│    This is the "lost update" problem. SERIALIZABLE prevents it.                  │
│                                                                                  │
│  ❌ Dirty Read:          PREVENTED                                               │
│  ❌ Non-Repeatable Read: PREVENTED                                               │
│  ❌ Phantom Read:        PREVENTED (range/gap locks block inserts)               │
│  ✅ Full Serial Execution Guarantee                                              │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  PERFORMANCE WARNING:                                                      │  │
│  │                                                                            │  │
│  │  SERIALIZABLE dramatically reduces concurrency:                            │  │
│  │  • More lock contention → more waiting                                     │  │
│  │  • Higher chance of deadlocks                                              │  │
│  │  • Transactions may be rolled back due to serialization failures           │  │
│  │  • Should be used ONLY for critical operations (transfers, bookings)       │  │
│  │  • For 95% of operations, READ_COMMITTED is sufficient                     │  │
│  │                                                                            │  │
│  │  Rule of Thumb:                                                            │  │
│  │  • READ_COMMITTED:   General CRUD, most business operations                │  │
│  │  • REPEATABLE_READ:  Reports, calculations that read same data twice       │  │
│  │  • SERIALIZABLE:     Financial transfers, booking systems, critical writes │  │
│  │  • READ_UNCOMMITTED: Dashboard metrics, approximate analytics              │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```
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


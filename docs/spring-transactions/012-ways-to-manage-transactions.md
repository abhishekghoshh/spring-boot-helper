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


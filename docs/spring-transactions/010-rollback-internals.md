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


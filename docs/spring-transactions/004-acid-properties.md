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


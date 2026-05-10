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


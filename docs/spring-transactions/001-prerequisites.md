### Prerequisites

Before diving into Spring Transactions, you need to understand two foundational concepts:

1. **Concurrency Control** — How multiple threads/users accessing the same data simultaneously is managed
2. **Spring AOP** — How Spring wraps your beans with proxies to add cross-cutting behavior (see [Spring AOP](../spring-aop/001-what-is-aop.md))

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

**Real-life use case — why you need both prerequisites:** a bank transfer
`debit(A) + credit(B)` is a critical section (shared balances) that must be
atomic. Spring implements that atomicity with an AOP proxy around your
service — that is exactly what `@Transactional` does.

**Teaser — where this is going (`@Transactional` preview):**

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class BankService {
    @Autowired private AccountRepository accounts;

    @Transactional // BEGIN before, COMMIT on success / ROLLBACK on failure
    public void transfer(Long fromId, Long toId, java.math.BigDecimal amt) {
        accounts.debit(fromId, amt);
        accounts.credit(toId, amt);
    }
}
```

**SQL preview — one atomic unit:**

```sql
BEGIN;
UPDATE accounts SET balance = balance - 500 WHERE id = 1;
UPDATE accounts SET balance = balance + 500 WHERE id = 2;
COMMIT;
-- Any exception before COMMIT -> ROLLBACK (both updates undone)
```

**Common pitfall:** thinking `@Transactional` alone fixes lost updates under
`READ_COMMITTED` — concurrent threads can still read the same stale balance.
You also need a lock (`SELECT ... FOR UPDATE` / `@Version`). Details in `014`.

**Interview Q&A:**
- Q: Why do you need AOP to understand `@Transactional`?
  A: Because `@Transactional` is implemented as an `@Around` advice in a proxy;
  self-invocation bypasses the proxy so the transaction never starts.
- Q: Why do you need concurrency control to understand transactions?
  A: Because transactions exist to protect critical sections from dirty reads,
  lost updates, and phantom reads.

**Next:**
- [002 — Critical Section](002-critical-section.md)
- [Spring AOP — What is AOP](../spring-aop/001-what-is-aop.md)

---


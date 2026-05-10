### Why @PersistenceContext Instead of @Autowired for EntityManager

```java
// CORRECT
@PersistenceContext
private EntityManager entityManager;

// WRONG (but compiles)
@Autowired
private EntityManager entityManager;
```

| | `@PersistenceContext` | `@Autowired` |
|---|---|---|
| **What it injects** | A **transaction-scoped proxy** that delegates to the correct EntityManager for the current transaction | A single shared EntityManager bean instance |
| **Thread safety** | Safe — each thread/transaction gets its own EntityManager behind the proxy | Unsafe — same instance shared across threads |
| **Lifecycle** | New EntityManager per transaction, auto-closes when transaction ends | One instance for the entire application |
| **Standard** | JPA standard annotation | Spring-specific |

```text
Thread A (txn 1)  ──>  @PersistenceContext proxy  ──>  EntityManager A (txn 1's PC)
Thread B (txn 2)  ──>  @PersistenceContext proxy  ──>  EntityManager B (txn 2's PC)

vs.

Thread A  ──>  @Autowired  ──>  same EntityManager instance  <──  Thread B
                                  (SHARED = data corruption!)
```

`@PersistenceContext` creates a **shared proxy** that internally uses a `ThreadLocal` to route each call to the transaction-bound `EntityManager`. This is why it is thread-safe.

---


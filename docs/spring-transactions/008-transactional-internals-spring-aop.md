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

**Real-life use case — why this internal matters:** `OrderService.placeOrder()`
inserts an order, deducts inventory, and records payment. Without the proxy
(shown above) each step auto-commits independently; with the proxy all three
share one connection with `autoCommit=false`, so a payment failure rolls back
the order + inventory writes too.

**Simplified `TransactionInterceptor` stub (labeled simplified — not real code):**

```java
// SIMPLIFIED — omits propagation/isolation/readOnly handling for clarity
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TransactionInterceptorStub implements MethodInterceptor {
    @Override public Object invoke(MethodInvocation inv) throws Throwable {
        var tx = txManager.getTransaction(txDefinition); // setAutoCommit(false)
        try {
            Object result = inv.proceed(); // real OrderService method
            txManager.commit(tx);          // connection.commit()
            return result;
        } catch (RuntimeException | Error ex) {
            txManager.rollback(tx);        // connection.rollback()
            throw ex;
        }
    }
}
```

**SQL log — `autocommit` / `COMMIT` / `ROLLBACK`:**

```sql
-- Proxy binds one connection, disables auto-commit (= BEGIN)
SET autocommit = 0; -- connection.setAutoCommit(false)
Hibernate: insert into orders (customer_id, total_amount) values (?, ?) -- [101, 2499.99]
Hibernate: update inventory set quantity=? where id=? -- [47, 12]
COMMIT;    -- success path: connection.commit()
-- failure path (RuntimeException): ROLLBACK; -- connection.rollback()
SET autocommit = 1; -- connection restored + returned to pool
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Self-invocation pitfall (why internals matter):                                 │
│                                                                                  │
│  orderService.placeOrder() ──proxy──> BEGIN → proceed() → COMMIT ✅              │
│  orderService.checkout() ──this.placeOrder()──> NO proxy → NO transaction ❌     │
│  Fix: call through the injected proxy / separate bean / exposeProxy = true.      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Pitfalls:**
- Self-invocation bypasses the proxy — `@Transactional` silently ignored.
- `private`/`final` methods are never intercepted (CGLIB cannot override them).
- Catching + swallowing the exception inside the method causes a COMMIT, not
  a ROLLBACK — rethrow or call `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`.

**Interview Q&A:**
- Q: How does `@Transactional` use Spring AOP internally?
  A: `InfrastructureAdvisorAutoProxyCreator` wraps the bean in a JDK/CGLIB proxy
  whose `TransactionInterceptor` (`@Around` advice) opens, commits, or rolls
  back the transaction around `invocation.proceed()`.
- Q: Why does self-invocation break `@Transactional`?
  A: `this.foo()` never passes through the proxy, so the interceptor never runs.
- Q: Where is the actual `COMMIT` executed?
  A: `TransactionInterceptor → PlatformTransactionManager.commit()` →
  `DataSourceTransactionManager.doCommit()` → `connection.commit()`.

**Next:**
- [Spring AOP 015 — How AOP Works Internally](../spring-aop/015-how-aop-works-internally.md)
- [009 — AOP Annotations, Pointcut & `@Around`](009-aop-annotations-pointcut-around.md)
- [010 — Rollback Internals](010-rollback-internals.md)

---


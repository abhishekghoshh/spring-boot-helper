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


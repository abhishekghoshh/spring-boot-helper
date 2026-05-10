### What is a Pointcut?

A **Pointcut** is a **predicate** (a true/false condition) that tells Spring AOP **which method calls should be intercepted** by an advice. Think of it as a **filter** or a **selector** — it defines the **WHERE** of AOP.

Without a pointcut, an advice wouldn't know which methods to apply to. The pointcut is the glue between the **advice** (what to do) and the **join point** (where to do it).

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut — The "WHERE" of AOP:                                                  │
│                                                                                  │
│  AOP has 3 questions:                                                            │
│    1. WHAT to do?     → Advice  (the code inside @Before, @Around, etc.)         │
│    2. WHERE to do it? → Pointcut (the expression that selects methods)           │
│    3. WHEN to do it?  → Advice type (@Before, @After, @Around, etc.)             │
│                                                                                  │
│  A Pointcut is like a SQL WHERE clause for methods:                              │
│                                                                                  │
│  SQL:        SELECT * FROM employees WHERE department = 'Engineering'            │
│  Pointcut:   @Before("execution(* com.app.service.*.*(..))")                     │
│                       └──────────────────────────────────────┘                    │
│                       This is the POINTCUT — it selects which methods            │
│                       the @Before advice should apply to.                         │
│                                                                                  │
│  What the pointcut does:                                                         │
│    For EVERY method call in the application, Spring asks:                        │
│      "Does this method match the pointcut expression?"                           │
│        → YES → Run the advice                                                    │
│        → NO  → Skip, call method directly                                        │
│                                                                                  │
│  Example:                                                                        │
│    Pointcut: execution(* com.app.service.OrderService.createOrder(..))           │
│                                                                                  │
│    orderService.createOrder("Laptop", 999.99)  → MATCHES ✓ → advice runs        │
│    orderService.getOrder(1L)                    → NO MATCH ✗ → no advice         │
│    paymentService.process(req)                  → NO MATCH ✗ → no advice         │
│    orderService.cancelOrder(1L)                 → NO MATCH ✗ → no advice         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut in the AOP Architecture:                                               │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │  @Aspect                                                                │     │
│  │  public class LoggingAspect {                                           │     │
│  │                                                                         │     │
│  │      @Before("execution(* com.app.service.*.*(..))")                    │     │
│  │       ↑          └──────────────────────────────────┘                    │     │
│  │       │                        ↑                                        │     │
│  │   ADVICE TYPE            POINTCUT EXPRESSION                            │     │
│  │   (WHEN)                 (WHERE — which methods)                        │     │
│  │                                                                         │     │
│  │      public void logBefore(JoinPoint jp) {                              │     │
│  │          log.info("Entering: {}", jp.getSignature().getName());         │     │
│  │      }                                                                  │     │
│  │      └──────────────────────────────────────────────┘                   │     │
│  │                        ↑                                                │     │
│  │                  ADVICE BODY                                            │     │
│  │                  (WHAT — the code to run)                               │     │
│  │  }                                                                      │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
│                                                                                  │
│  So the pointcut is ALWAYS written inside the annotation:                        │
│    @Before("POINTCUT_HERE")                                                      │
│    @After("POINTCUT_HERE")                                                       │
│    @Around("POINTCUT_HERE")                                                      │
│    @AfterReturning(pointcut = "POINTCUT_HERE", returning = "result")             │
│    @AfterThrowing(pointcut = "POINTCUT_HERE", throwing = "ex")                   │
│                                                                                  │
│  Or defined separately with @Pointcut:                                           │
│    @Pointcut("POINTCUT_HERE")                                                    │
│    public void myPointcut() {}                                                   │
│                                                                                  │
│    @Before("myPointcut()")  ← reference by name                                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example 1 — Pointcut matching a single method:**

```java
@Aspect
@Component
@Slf4j
public class OrderAuditAspect {

    // Pointcut: match ONLY createOrder() in OrderService
    @Before("execution(* com.app.service.OrderService.createOrder(..))")
    //       └─────────────────────────────────────────────────────────┘
    //                           POINTCUT
    public void auditOrderCreation(JoinPoint jp) {
        log.info("AUDIT: Order creation initiated | args: {}",
            Arrays.toString(jp.getArgs()));
    }
}
```

```java
@Service
public class OrderService {

    public Order createOrder(String product, Double amount) {
        // ← This method MATCHES the pointcut → advice RUNS
        return orderRepo.save(new Order(null, product, amount, "CREATED"));
    }

    public Order getOrder(Long id) {
        // ← This method DOES NOT match → advice does NOT run
        return orderRepo.findById(id).orElseThrow();
    }

    public void cancelOrder(Long id) {
        // ← This method DOES NOT match → advice does NOT run
        Order order = orderRepo.findById(id).orElseThrow();
        order.setStatus("CANCELLED");
        orderRepo.save(order);
    }
}
```

**Code Example 2 — Pointcut matching all methods in a class:**

```java
@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    // Pointcut: match ALL methods in OrderService
    @Around("execution(* com.app.service.OrderService.*(..))")
    //                                              ↑
    //                            * = any method name
    public Object logAllOrderMethods(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getName();
        log.info(">>> OrderService.{}() called", method);

        Object result = pjp.proceed();

        log.info("<<< OrderService.{}() returned: {}", method, result);
        return result;
    }
}

// Now ALL methods are intercepted:
//   orderService.createOrder("Laptop", 999.99)  → MATCHES ✓
//   orderService.getOrder(1L)                    → MATCHES ✓
//   orderService.cancelOrder(1L)                 → MATCHES ✓
//   orderService.getAllOrders()                   → MATCHES ✓
//   paymentService.process(req)                  → NO MATCH ✗ (different class)
```

**Code Example 3 — Pointcut matching all methods in all service classes:**

```java
@Aspect
@Component
@Slf4j
public class GlobalServiceAspect {

    // Pointcut: match ALL methods in ALL classes in the service package
    @Before("execution(* com.app.service.*.*(..))")
    //                                  ↑ ↑
    //                   any class ─────┘ └── any method
    public void logAllServices(JoinPoint jp) {
        String className = jp.getTarget().getClass().getSimpleName();
        String method = jp.getSignature().getName();
        log.info(">>> {}.{}()", className, method);
    }
}

// Now ALL service methods are intercepted:
//   orderService.createOrder(...)      → MATCHES ✓ (OrderService is in service package)
//   paymentService.process(...)        → MATCHES ✓ (PaymentService is in service package)
//   userService.createUser(...)        → MATCHES ✓ (UserService is in service package)
//   orderRepository.save(...)          → NO MATCH ✗ (repository package, not service)
//   orderController.create(...)        → NO MATCH ✗ (controller package, not service)
```

**Code Example 4 — Pointcut matching methods by annotation:**

```java
// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {}

@Aspect
@Component
@Slf4j
public class TimingAspect {

    // Pointcut: match ANY method annotated with @Timed (regardless of class/package)
    @Around("@annotation(com.app.annotation.Timed)")
    public Object timeMethod(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long duration = System.currentTimeMillis() - start;
        log.info("TIMER: {}.{}() took {}ms",
            pjp.getTarget().getClass().getSimpleName(),
            pjp.getSignature().getName(), duration);
        return result;
    }
}

@Service
public class ReportService {

    @Timed  // ← MATCHES the pointcut
    public Report generateMonthlyReport(int month, int year) {
        // ... expensive report generation
        return report;
    }

    public Report getReport(Long id) {
        // ← No @Timed annotation → NO MATCH
        return reportRepo.findById(id).orElseThrow();
    }
}

@Service
public class OrderService {

    @Timed  // ← MATCHES (works across ANY class — not limited to one service)
    public List<Order> searchOrders(String criteria) {
        return orderRepo.searchByCriteria(criteria);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut — Summary:                                                             │
│                                                                                  │
│  ┌──────────────────────────────────┬────────────────────────────────────────────┐│
│  │ Aspect                          │ Meaning                                    ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ What is it?                     │ A predicate/filter that selects methods    ││
│  │ What does it define?            │ WHERE advice should be applied             ││
│  │ Written where?                  │ Inside @Before/@After/@Around annotation   ││
│  │                                 │ or in a separate @Pointcut method          ││
│  │ Evaluated when?                 │ At startup (proxy creation) + each call    ││
│  │ What does it return?            │ true (match → run advice) or               ││
│  │                                 │ false (no match → skip advice)             ││
│  │ Can match by...                 │ Method name, class, package, annotation,   ││
│  │                                 │ arguments, return type, bean name          ││
│  │ Can be combined?                │ Yes, with && (AND), || (OR), ! (NOT)       ││
│  │ Can be reused?                  │ Yes, with @Pointcut named pointcuts        ││
│  └──────────────────────────────────┴────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


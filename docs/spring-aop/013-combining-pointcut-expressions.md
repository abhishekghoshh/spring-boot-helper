### Combining Pointcut Expressions with Boolean Operators (AND, OR, NOT)

You can combine two or more pointcut expressions using **boolean operators** to create precise, complex matching rules. Spring AOP supports three operators:

- `&&` — **AND** (both conditions must be true)
- `||` — **OR** (at least one condition must be true)
- `!`  — **NOT** (negate/invert a condition)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Boolean Operators for Pointcuts:                                                │
│                                                                                  │
│  ┌──────────┬────────────────────────────────────────────────────────────────────┐│
│  │ Operator │ Meaning                                                            ││
│  ├──────────┼────────────────────────────────────────────────────────────────────┤│
│  │ &&       │ AND — both pointcuts must match                                    ││
│  │ ||       │ OR  — at least one pointcut must match                             ││
│  │ !        │ NOT — inverts the match (match if pointcut does NOT match)         ││
│  └──────────┴────────────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  Syntax:                                                                         │
│    @Before("pointcut1() && pointcut2()")    ← AND                                │
│    @Before("pointcut1() || pointcut2()")    ← OR                                 │
│    @Before("!pointcut1()")                  ← NOT                                │
│    @Before("(p1() || p2()) && !p3()")       ← Complex (parentheses for grouping) │
│                                                                                  │
│  NOTE: In @annotation string values, use && not &, and || not |                  │
│  NOTE: When using in XML, use "and", "or", "not" instead of symbols              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ && (AND) — Both conditions must match ═══

// Example 1: Service methods that return Order
@Aspect
@Component
@Slf4j
public class OrderReturnAspect {

    @AfterReturning(
        pointcut = "execution(* com.app.service.*.*(..)) && " +
                   "execution(com.app.entity.Order *(..))",
        //          ↑ in service package          ↑ returns Order type
        returning = "order"
    )
    public void afterOrderReturned(JoinPoint jp, Order order) {
        log.info("Order returned: id={}, product={}, status={}",
            order.getId(), order.getProduct(), order.getStatus());
    }
}
// Matches: methods in service package that ALSO return Order
//   ✓ OrderService.createOrder(...)  → in service package AND returns Order
//   ✓ OrderService.getOrder(Long)    → in service package AND returns Order
//   ✗ OrderService.getAllOrders()     → returns List<Order>, not Order
//   ✗ OrderService.cancelOrder(Long) → returns void, not Order
//   ✗ PaymentService.process(...)    → returns Payment, not Order
```

```java
// Example 2: @Loggable methods in service layer only
@Aspect
@Component
@Slf4j
public class ServiceLoggableAspect {

    @Around("@annotation(loggable) && within(com.app.service.*)")
    //       ↑ has @Loggable           ↑ AND is in service package
    public Object logServiceOnly(ProceedingJoinPoint pjp, Loggable loggable) throws Throwable {
        log.info("[{}] >>> {}.{}()", loggable.value(),
            pjp.getTarget().getClass().getSimpleName(),
            pjp.getSignature().getName());
        Object result = pjp.proceed();
        log.info("[{}] <<< returned: {}", loggable.value(), result);
        return result;
    }
}
// Matches: methods with @Loggable that are ALSO in the service package
//   ✓ OrderService.createOrder()      → has @Loggable + in service package
//   ✗ OrderController.create()        → has @Loggable but in CONTROLLER package
//   ✗ OrderService.getOrder(Long)     → in service package but NO @Loggable
```

```java
// Example 3: Validate OrderRequest args in service methods only
@Aspect
@Component
@Slf4j
public class ServiceOrderValidation {

    @Before("args(orderRequest) && within(com.app.service.*)")
    //       ↑ arg is OrderRequest  ↑ AND in service package
    public void validateInService(JoinPoint jp, OrderRequest orderRequest) {
        if (orderRequest.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        log.info("Validated OrderRequest in service: {}", orderRequest);
    }
}
// Matches: service methods called with OrderRequest argument
//   ✓ orderService.createOrder(OrderRequest req)     → OrderRequest + service
//   ✗ orderController.create(OrderRequest req)       → OrderRequest but CONTROLLER
//   ✗ orderService.getOrder(1L)                      → service but Long, not OrderRequest
```

```java
// ═══ || (OR) — At least one condition must match ═══

// Example 4: Log all service OR repository methods
@Aspect
@Component
@Slf4j
public class DataLayerLoggingAspect {

    @Before("within(com.app.service.*) || within(com.app.repository.*)")
    //       ↑ service package           ↑ OR repository package
    public void beforeDataLayerMethod(JoinPoint jp) {
        log.info("DATA LAYER >>> {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
// Matches: methods in service package OR repository package
//   ✓ OrderService.createOrder(...)   → in service package
//   ✓ PaymentService.process(...)     → in service package
//   ✓ OrderRepository.save(...)       → in repository package
//   ✓ PaymentRepository.findById(...) → in repository package
//   ✗ OrderController.create(...)     → in controller package (neither)
```

```java
// Example 5: Monitor methods starting with "create" OR "delete"
@Aspect
@Component
@Slf4j
public class MutationAspect {

    @Around("execution(* com.app.service.*.create*(..)) || " +
            "execution(* com.app.service.*.delete*(..)) || " +
            "execution(* com.app.service.*.update*(..))")
    //       ↑ create*        ↑ OR delete*        ↑ OR update*
    public Object logMutations(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getName();
        log.info("MUTATION >>> {}()", method);
        Object result = pjp.proceed();
        log.info("MUTATION <<< {}() completed", method);
        return result;
    }
}
// Matches: methods starting with create, delete, or update
//   ✓ OrderService.createOrder(...)   → starts with "create"
//   ✓ OrderService.deleteOrder(...)   → starts with "delete"
//   ✓ OrderService.updateOrder(...)   → starts with "update"
//   ✗ OrderService.getOrder(...)      → starts with "get" (none of the patterns)
//   ✗ OrderService.getAllOrders()      → starts with "get"
```

```java
// Example 6: Match methods on any PaymentProcessor OR OrderService
@Aspect
@Component
@Slf4j
public class CriticalServiceAspect {

    @Around("target(com.app.service.PaymentProcessor) || " +
            "target(com.app.service.OrderService)")
    //       ↑ any PaymentProcessor impl  ↑ OR OrderService
    public Object monitorCriticalServices(ProceedingJoinPoint pjp) throws Throwable {
        log.info("CRITICAL >>> {}.{}()",
            pjp.getTarget().getClass().getSimpleName(),
            pjp.getSignature().getName());
        Object result = pjp.proceed();
        log.info("CRITICAL <<< completed");
        return result;
    }
}
// Matches:
//   ✓ orderService.createOrder(...)         → is OrderService
//   ✓ creditCardProcessor.process(...)      → implements PaymentProcessor
//   ✓ payPalProcessor.process(...)          → implements PaymentProcessor
//   ✗ reportService.generateReport(...)     → neither
```

```java
// ═══ ! (NOT) — Negate a condition ═══

// Example 7: All service methods EXCEPT getters
@Aspect
@Component
@Slf4j
public class NonGetterAspect {

    @Before("within(com.app.service.*) && !execution(* com.app.service.*.get*(..))")
    //       ↑ in service package       ↑ AND NOT starting with "get"
    public void beforeNonGetter(JoinPoint jp) {
        log.info("NON-GETTER >>> {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
// Matches: service methods that do NOT start with "get"
//   ✓ OrderService.createOrder(...)   → in service, NOT a getter
//   ✓ OrderService.cancelOrder(...)   → in service, NOT a getter
//   ✓ PaymentService.processPayment() → in service, NOT a getter
//   ✗ OrderService.getOrder(Long)     → in service, but IS a getter (excluded!)
//   ✗ OrderService.getAllOrders()      → in service, but IS a getter (excluded!)
```

```java
// Example 8: All methods EXCEPT in a specific class
@Aspect
@Component
@Slf4j
public class ExcludeClassAspect {

    @Before("within(com.app.service.*) && !within(com.app.service.ReportService)")
    //       ↑ all services              ↑ EXCEPT ReportService
    public void beforeExceptReport(JoinPoint jp) {
        log.info(">>> {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
// Matches: all service classes EXCEPT ReportService
//   ✓ OrderService.createOrder(...)      → in service, NOT ReportService
//   ✓ PaymentService.processPayment(...) → in service, NOT ReportService
//   ✗ ReportService.generateReport(...)  → EXCLUDED
```

```java
// ═══ Complex combinations — mixing &&, ||, ! ═══

// Example 9: Service OR controller methods, but NOT getters, with @Loggable
@Aspect
@Component
@Slf4j
public class ComplexPointcutAspect {

    @Around("(" +
            "  within(com.app.service.*) || within(com.app.controller.*)" +
            ") && " +
            "!execution(* *.get*(..)) && " +
            "@annotation(loggable)")
    //  Translation:
    //    (in service OR controller package)
    //    AND NOT a getter method
    //    AND has @Loggable annotation
    public Object complexPointcut(ProceedingJoinPoint pjp, Loggable loggable) throws Throwable {
        log.info("[{}] >>> {}.{}()", loggable.value(),
            pjp.getTarget().getClass().getSimpleName(),
            pjp.getSignature().getName());
        Object result = pjp.proceed();
        log.info("[{}] <<< result: {}", loggable.value(), result);
        return result;
    }
}
// Matches ALL of these must be true:
//   1. In service OR controller package  ✓
//   2. NOT a getter method               ✓
//   3. Has @Loggable annotation          ✓
//
//   ✓ OrderService.createOrder()     → service + not getter + @Loggable
//   ✗ OrderService.getOrder()        → service + IS getter (excluded!)
//   ✗ OrderService.cancelOrder()     → service + not getter + NO @Loggable
//   ✓ OrderController.create()       → controller + not getter + @Loggable
//   ✗ ReportService.generateReport() → NOT in service/controller (in sub-package)
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Boolean Operators — Summary:                                                    │
│                                                                                  │
│  ┌──────────┬────────────┬───────────────────────────────────────────────────────┐│
│  │ Operator │ Keyword    │ Use Case                                              ││
│  ├──────────┼────────────┼───────────────────────────────────────────────────────┤│
│  │ &&       │ AND        │ Narrow down — both conditions must match              ││
│  │          │            │ "Service methods WITH @Loggable"                      ││
│  │          │            │ within(..service.*) && @annotation(Loggable)          ││
│  ├──────────┼────────────┼───────────────────────────────────────────────────────┤│
│  │ ||       │ OR         │ Widen scope — either condition can match              ││
│  │          │            │ "Service OR repository methods"                       ││
│  │          │            │ within(..service.*) || within(..repository.*)         ││
│  ├──────────┼────────────┼───────────────────────────────────────────────────────┤│
│  │ !        │ NOT        │ Exclude — skip matching methods                       ││
│  │          │            │ "All methods EXCEPT getters"                          ││
│  │          │            │ within(..service.*) && !execution(* *.get*(..))       ││
│  ├──────────┼────────────┼───────────────────────────────────────────────────────┤│
│  │ ()       │ Grouping   │ Control operator precedence                           ││
│  │          │            │ (A || B) && C                                         ││
│  │          │            │ vs A || (B && C) — different results!                 ││
│  └──────────┴────────────┴───────────────────────────────────────────────────────┘│
│                                                                                  │
│  Common Combinations:                                                            │
│    within(..service.*) && @annotation(Loggable)                                  │
│      → @Loggable methods in service layer only                                   │
│                                                                                  │
│    args(OrderRequest) && within(..service.*)                                     │
│      → OrderRequest args in service layer only                                   │
│                                                                                  │
│    execution(* *.create*(..)) || execution(* *.delete*(..))                      │
│      → All create/delete operations                                              │
│                                                                                  │
│    within(..service.*) && !execution(* *.get*(..)) && !execution(* *.is*(..))    │
│      → Service mutations only (exclude all read operations)                      │
│                                                                                  │
│    (within(..service.*) || within(..controller.*)) && @annotation(Auditable)     │
│      → @Auditable methods in service or controller layer                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


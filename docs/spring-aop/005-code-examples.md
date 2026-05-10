### Code Examples with Spring AOP

**Dependencies (pom.xml):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

This brings in `spring-aop` and `aspectjweaver` — everything needed for Spring AOP with `@Aspect` annotations.

---

#### Example 1: Logging Aspect (@Before, @AfterReturning, @AfterThrowing)

```java
// Entity
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String product;
    private Double amount;
    private String status;
}
```

```java
// Repository
public interface OrderRepository extends JpaRepository<Order, Long> {}
```

```java
// Service
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public Order createOrder(String product, Double amount) {
        Order order = new Order(null, product, amount, "CREATED");
        return orderRepository.save(order);
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
```

```java
// Logging Aspect
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // POINTCUT — matches all methods in service package
    @Pointcut("execution(* com.app.service.*.*(..))")
    public void serviceLayerMethods() {}
    //  ↑ Named pointcut — reusable in multiple advice

    // @Before — runs BEFORE the target method
    @Before("serviceLayerMethods()")
    public void logBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        log.info(">>> Calling: {}.{}() with args: {}",
            joinPoint.getTarget().getClass().getSimpleName(),
            methodName,
            Arrays.toString(args));
    }

    // @AfterReturning — runs AFTER successful return
    @AfterReturning(pointcut = "serviceLayerMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("<<< {}.{}() returned: {}",
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName(),
            result);
    }

    // @AfterThrowing — runs AFTER exception is thrown
    @AfterThrowing(pointcut = "serviceLayerMethods()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {
        log.error("!!! {}.{}() threw: {}",
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName(),
            ex.getMessage());
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Order create(@RequestParam String product, @RequestParam Double amount) {
        return orderService.createOrder(product, amount);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @GetMapping
    public List<Order> getAll() {
        return orderService.getAllOrders();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Request: POST /api/orders?product=Laptop&amount=999.99                          │
│                                                                                  │
│  Console Output:                                                                 │
│    >>> Calling: OrderService.createOrder() with args: [Laptop, 999.99]           │
│    Hibernate: insert into orders (product, amount, status) values (?, ?, ?)      │
│    <<< OrderService.createOrder() returned: Order(id=1, product=Laptop, ...)     │
│                                                                                  │
│  Request: GET /api/orders/999  (non-existent)                                    │
│                                                                                  │
│  Console Output:                                                                 │
│    >>> Calling: OrderService.getOrder() with args: [999]                         │
│    !!! OrderService.getOrder() threw: Order not found: 999                       │
│                                                                                  │
│  Flow Diagram:                                                                   │
│                                                                                  │
│  Client → Controller → PROXY → @Before (log entry)                              │
│                                    │                                             │
│                                    v                                             │
│                               OrderService.createOrder()  ← actual execution     │
│                                    │                                             │
│                                    ├── success → @AfterReturning (log result)    │
│                                    └── failure → @AfterThrowing (log error)      │
│                                                                                  │
│  Notice: OrderService has ZERO logging code. The Aspect handles it all.          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Example 2: Performance Monitoring Aspect (@Around)

```java
@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    // @Around — wraps the method, can control execution
    @Around("execution(* com.app.service.*.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().toShortString();

        long startTime = System.currentTimeMillis();

        Object result;
        try {
            result = joinPoint.proceed();  // ← EXECUTES the target method
            //  Without proceed(), the target method NEVER runs!
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("PERF: {} FAILED after {}ms", methodName, duration);
            throw ex;  // re-throw to maintain normal exception flow
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("PERF: {} executed in {}ms", methodName, duration);

        if (duration > 1000) {  // Flag slow methods
            log.warn("SLOW METHOD: {} took {}ms (threshold: 1000ms)", methodName, duration);
        }

        return result;  // ← MUST return the result to caller
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Around Advice — Full Control:                                                  │
│                                                                                  │
│  public Object measureExecutionTime(ProceedingJoinPoint joinPoint) {             │
│      │                                                                           │
│      │  // Code HERE runs BEFORE target method                                   │
│      │  long start = System.currentTimeMillis();                                 │
│      │                                                                           │
│      │  Object result = joinPoint.proceed();  ← target method executes HERE      │
│      │                                                                           │
│      │  // Code HERE runs AFTER target method                                    │
│      │  long duration = System.currentTimeMillis() - start;                      │
│      │                                                                           │
│      │  return result;  ← you control what gets returned to caller               │
│      │                                                                           │
│      │  // You can even:                                                         │
│      │  //   - Modify args before proceed(): joinPoint.proceed(newArgs)          │
│      │  //   - Modify result before returning                                    │
│      │  //   - Skip proceed() entirely (return cached value)                     │
│      │  //   - Call proceed() multiple times (retry)                             │
│      │  //   - Catch exceptions and handle differently                           │
│  }                                                                               │
│                                                                                  │
│  Console Output (GET /api/orders):                                               │
│    PERF: OrderService.getAllOrders() executed in 23ms                             │
│                                                                                  │
│  Console Output (slow query):                                                    │
│    PERF: ReportService.generateReport() executed in 2341ms                       │
│    SLOW METHOD: ReportService.generateReport() took 2341ms (threshold: 1000ms)   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Example 3: Custom Annotation-Based Aspect

Instead of using package-level pointcuts, you can create custom annotations to selectively apply aspects:

```java
// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    String value() default "";
}
```

```java
// Custom annotation for rate limiting
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int maxRequests() default 10;     // max requests
    int duration() default 60;         // time window in seconds
}
```

```java
// Aspect that responds to @Loggable annotation
@Aspect
@Component
@Slf4j
public class LoggableAspect {

    @Around("@annotation(loggable)")
    //       ↑ matches methods annotated with @Loggable
    //         ↑ binds the annotation instance to the "loggable" parameter
    public Object logAnnotatedMethod(ProceedingJoinPoint joinPoint,
                                      Loggable loggable) throws Throwable {

        String context = loggable.value();  // get annotation value
        String method = joinPoint.getSignature().toShortString();

        log.info("[{}] >>> Entering: {} with args: {}",
            context, method, Arrays.toString(joinPoint.getArgs()));

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        log.info("[{}] <<< Exiting: {} ({}ms) returned: {}",
            context, method, duration, result);

        return result;
    }
}
```

```java
// Rate Limiting Aspect
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    // Simple in-memory rate limiter (use Redis in production)
    private final Map<String, List<Long>> requestLog = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint,
                                    RateLimit rateLimit) throws Throwable {

        String key = joinPoint.getSignature().toShortString();
        int maxRequests = rateLimit.maxRequests();
        int duration = rateLimit.duration();

        long now = System.currentTimeMillis();
        long windowStart = now - (duration * 1000L);

        requestLog.putIfAbsent(key, new ArrayList<>());
        List<Long> timestamps = requestLog.get(key);

        // Remove expired entries
        timestamps.removeIf(t -> t < windowStart);

        if (timestamps.size() >= maxRequests) {
            log.warn("Rate limit exceeded for: {} ({}/{} in {}s)",
                key, timestamps.size(), maxRequests, duration);
            throw new RuntimeException("Rate limit exceeded. Try again later.");
        }

        timestamps.add(now);
        return joinPoint.proceed();
    }
}
```

```java
// Service using custom annotations
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Loggable("ORDER-CREATION")   // ← only THIS method gets logged by LoggableAspect
    @RateLimit(maxRequests = 5, duration = 60)  // ← max 5 orders per minute
    public Order createOrder(String product, Double amount) {
        Order order = new Order(null, product, amount, "CREATED");
        return orderRepository.save(order);
    }

    // This method has NO custom annotations → no custom AOP applied
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    @Loggable("ORDER-LIST")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Custom Annotation Approach — Advantages:                                        │
│                                                                                  │
│  Package-based pointcut:                                                         │
│    execution(* com.app.service.*.*(..))                                          │
│    → Applies to ALL methods in ALL services                                      │
│    → No control over which methods to exclude                                    │
│    → Renaming package breaks the pointcut                                        │
│                                                                                  │
│  Annotation-based pointcut:                                                      │
│    @annotation(com.app.annotation.Loggable)                                      │
│    → Applies ONLY to methods with @Loggable                                      │
│    → Developer explicitly opts in per method                                     │
│    → Refactoring-safe (annotation travels with method)                           │
│    → Self-documenting (reading the method, you see @Loggable)                    │
│                                                                                  │
│  Console Output (POST /api/orders?product=Laptop&amount=999.99):                 │
│    [ORDER-CREATION] >>> Entering: OrderService.createOrder(..)                   │
│                         with args: [Laptop, 999.99]                              │
│    Hibernate: insert into orders (product, amount, status) values (?, ?, ?)      │
│    [ORDER-CREATION] <<< Exiting: OrderService.createOrder(..) (45ms)             │
│                         returned: Order(id=1, product=Laptop, ...)               │
│                                                                                  │
│  Console Output (6th call within 60s — rate limited):                            │
│    Rate limit exceeded for: OrderService.createOrder(..) (5/5 in 60s)            │
│    → RuntimeException: Rate limit exceeded. Try again later.                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Example 4: Transaction-Like Aspect (Understanding @Transactional Internally)

This shows how Spring's `@Transactional` works under the hood — it's an AOP `@Around` advice:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How @Transactional Works Internally (Simplified):                               │
│                                                                                  │
│  When you write:                                                                 │
│    @Transactional                                                                │
│    public Order createOrder(OrderRequest req) {                                  │
│        Order order = new Order(req);                                             │
│        return orderRepo.save(order);                                             │
│    }                                                                             │
│                                                                                  │
│  Spring internally creates an AOP advice equivalent to:                          │
│                                                                                  │
│    @Around("@annotation(org.springframework.transaction.annotation.Transactional)│
│    ")                                                                            │
│    public Object transactionAdvice(ProceedingJoinPoint pjp) throws Throwable {   │
│        TransactionStatus tx = transactionManager.getTransaction(definition);     │
│        try {                                                                     │
│            Object result = pjp.proceed();  // your business logic                │
│            transactionManager.commit(tx);  // commit on success                  │
│            return result;                                                        │
│        } catch (RuntimeException ex) {                                           │
│            transactionManager.rollback(tx);  // rollback on exception            │
│            throw ex;                                                             │
│        }                                                                         │
│    }                                                                             │
│                                                                                  │
│  Flow:                                                                           │
│    Controller calls orderService.createOrder(req)                                │
│         │                                                                        │
│         v                                                                        │
│    CGLIB Proxy intercepts                                                        │
│         │                                                                        │
│         v                                                                        │
│    TransactionInterceptor.invoke()  ← AOP advice                                │
│         │                                                                        │
│         ├─ BEGIN TRANSACTION                                                     │
│         │                                                                        │
│         v                                                                        │
│    OrderService.createOrder() ← actual method                                    │
│         │                                                                        │
│         ├── success → COMMIT                                                     │
│         └── exception → ROLLBACK                                                 │
│                                                                                  │
│  This is why self-invocation breaks @Transactional:                              │
│    public class OrderService {                                                   │
│        @Transactional                                                            │
│        public void method1() { ... }                                             │
│                                                                                  │
│        public void method2() {                                                   │
│            this.method1();  // ← "this" is the TARGET, not the PROXY             │
│            // The proxy never sees this call → no transaction!                   │
│        }                                                                         │
│    }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Example 5: Exception Handling Aspect

```java
@Aspect
@Component
@Slf4j
public class ExceptionHandlingAspect {

    @AfterThrowing(
        pointcut = "execution(* com.app.service.*.*(..))",
        throwing = "ex"
    )
    public void handleServiceException(JoinPoint joinPoint, Exception ex) {
        String method = joinPoint.getSignature().toShortString();

        // Log with full context
        log.error("Exception in {}: {} | Args: {}",
            method, ex.getMessage(), Arrays.toString(joinPoint.getArgs()));

        // Could also: send alert, increment error counter, notify monitoring system
    }

    // Convert generic exceptions to domain-specific exceptions
    @Around("execution(* com.app.repository.*.*(..))")
    public Object wrapRepositoryExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (DataAccessException ex) {
            throw new ServiceException(
                "Database error in " + joinPoint.getSignature().getName(),
                ex
            );
        }
    }
}
```

---

#### Example 6: Complete Multi-Aspect Application

This demonstrates multiple aspects working together with ordering:

```java
// Aspect ordering — lower number = higher priority (runs first)
@Aspect
@Component
@Order(1)  // Runs FIRST (outermost)
@Slf4j
public class SecurityAspect {

    @Before("@annotation(com.app.annotation.Secured)")
    public void checkSecurity(JoinPoint joinPoint) {
        // In real app, check SecurityContextHolder
        log.info("SECURITY: Checking access for {}",
            joinPoint.getSignature().toShortString());
        // throw AccessDeniedException if unauthorized
    }
}
```

```java
@Aspect
@Component
@Order(2)  // Runs SECOND
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.app.service.*.*(..))")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("LOG: >>> {}({})",
            joinPoint.getSignature().getName(),
            Arrays.toString(joinPoint.getArgs()));
        Object result = joinPoint.proceed();
        log.info("LOG: <<< {} returned: {}",
            joinPoint.getSignature().getName(), result);
        return result;
    }
}
```

```java
@Aspect
@Component
@Order(3)  // Runs THIRD (innermost)
@Slf4j
public class PerformanceAspect {

    @Around("execution(* com.app.service.*.*(..))")
    public Object time(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        log.info("PERF: {} took {}ms",
            joinPoint.getSignature().getName(),
            System.currentTimeMillis() - start);
        return result;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Multiple Aspects — Execution Order (@Order):                                    │
│                                                                                  │
│  @Order(1) SecurityAspect     ← outermost (runs first on entry, last on exit)   │
│  @Order(2) LoggingAspect      ← middle                                          │
│  @Order(3) PerformanceAspect  ← innermost (closest to target method)            │
│                                                                                  │
│  Execution flow for orderService.createOrder("Laptop", 999.99):                  │
│                                                                                  │
│  ┌─────────── @Order(1) SecurityAspect ─────────────────────────────────────┐    │
│  │  SECURITY: Checking access for OrderService.createOrder(..)              │    │
│  │  ┌─────────── @Order(2) LoggingAspect ────────────────────────────────┐  │    │
│  │  │  LOG: >>> createOrder([Laptop, 999.99])                            │  │    │
│  │  │  ┌─────────── @Order(3) PerformanceAspect ────────────────────┐   │  │    │
│  │  │  │  start timer                                                │   │  │    │
│  │  │  │  ┌────────────────────────────────────────────────────┐    │   │  │    │
│  │  │  │  │  TARGET: OrderService.createOrder() executes       │    │   │  │    │
│  │  │  │  │  → save order to DB                                │    │   │  │    │
│  │  │  │  │  → return Order(id=1, ...)                         │    │   │  │    │
│  │  │  │  └────────────────────────────────────────────────────┘    │   │  │    │
│  │  │  │  PERF: createOrder took 45ms                                │   │  │    │
│  │  │  └────────────────────────────────────────────────────────────┘   │  │    │
│  │  │  LOG: <<< createOrder returned: Order(id=1, ...)                   │  │    │
│  │  └───────────────────────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  Console Output:                                                                 │
│    SECURITY: Checking access for OrderService.createOrder(..)                    │
│    LOG: >>> createOrder([Laptop, 999.99])                                        │
│    Hibernate: insert into orders (product, amount, status) values (?, ?, ?)      │
│    PERF: createOrder took 45ms                                                   │
│    LOG: <<< createOrder returned: Order(id=1, product=Laptop, ...)               │
│                                                                                  │
│  Think of it as NESTED layers (like Russian dolls):                              │
│    Security check → then Log entry → then Timer start                            │
│    → TARGET → Timer stop → then Log exit → then Security done                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Example 7: Audit Trail Aspect (Real-World Business Requirement)

```java
// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();  // e.g., "CREATE_ORDER", "UPDATE_PAYMENT"
}
```

```java
// Audit entity
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String action;
    private String method;
    private String args;
    private String result;
    private String username;
    private LocalDateTime timestamp;
    private Long durationMs;
}
```

```java
// Audit Aspect
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {

        long start = System.currentTimeMillis();
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();  // get current user

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception ex) {
            // Log failed operation
            saveAuditLog(auditable.action(), joinPoint, "FAILED: " + ex.getMessage(),
                username, System.currentTimeMillis() - start);
            throw ex;
        }

        // Log successful operation
        saveAuditLog(auditable.action(), joinPoint, result != null ? result.toString() : "null",
            username, System.currentTimeMillis() - start);

        return result;
    }

    private void saveAuditLog(String action, ProceedingJoinPoint joinPoint,
                               String result, String username, long duration) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setMethod(joinPoint.getSignature().toShortString());
        log.setArgs(Arrays.toString(joinPoint.getArgs()));
        log.setResult(result.length() > 500 ? result.substring(0, 500) : result);
        log.setUsername(username);
        log.setTimestamp(LocalDateTime.now());
        log.setDurationMs(duration);
        auditLogRepository.save(log);
    }
}
```

```java
// Usage in service
@Service
public class OrderService {

    @Auditable(action = "CREATE_ORDER")
    @Transactional
    public Order createOrder(String product, Double amount) {
        Order order = new Order(null, product, amount, "CREATED");
        return orderRepository.save(order);
    }

    @Auditable(action = "CANCEL_ORDER")
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Not found"));
        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Audit Trail — Database Result:                                                  │
│                                                                                  │
│  audit_log table:                                                                │
│  ┌────┬──────────────┬──────────────────────────┬────────────────┬───────┬──────┐│
│  │ id │ action       │ method                   │ username       │ dur.  │ time ││
│  ├────┼──────────────┼──────────────────────────┼────────────────┼───────┼──────┤│
│  │  1 │ CREATE_ORDER │ OrderService.createOrder  │ admin@app.com  │ 45ms  │ ...  ││
│  │  2 │ CREATE_ORDER │ OrderService.createOrder  │ user1@app.com  │ 32ms  │ ...  ││
│  │  3 │ CANCEL_ORDER │ OrderService.cancelOrder  │ admin@app.com  │ 28ms  │ ...  ││
│  └────┴──────────────┴──────────────────────────┴────────────────┴───────┴──────┘│
│                                                                                  │
│  Business Value:                                                                 │
│    - WHO did WHAT, WHEN, and HOW LONG it took                                    │
│    - Zero audit code in business methods                                         │
│    - Compliance-ready (SOC2, GDPR audit requirements)                            │
│    - Add @Auditable to any method → instant audit trail                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


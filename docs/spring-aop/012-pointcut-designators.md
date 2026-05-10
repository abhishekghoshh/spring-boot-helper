### Different Types of Pointcut Designators

Pointcut designators are **keywords** used inside pointcut expressions to define HOW methods should be matched. Spring AOP supports several types, each matching methods in a different way.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut Designator Types — Overview:                                           │
│                                                                                  │
│  ┌──────────────────┬────────────────────────────────────────────────────────────┐│
│  │ Designator       │ What It Matches By                                         ││
│  ├──────────────────┼────────────────────────────────────────────────────────────┤│
│  │ execution()      │ Method SIGNATURE (return type, class, name, params)        ││
│  │ within()         │ All methods in matching CLASSES or PACKAGES                ││
│  │ @annotation()    │ Methods annotated with a specific ANNOTATION               ││
│  │ args()           │ Methods with specific ARGUMENT TYPES at runtime            ││
│  │ @args()          │ Methods where argument's CLASS has a specific annotation   ││
│  │ target()         │ Methods on beans of a specific TARGET TYPE                 ││
│  │ @within()        │ Methods in classes annotated with a specific annotation    ││
│  │ @target()        │ Methods on beans whose class has a specific annotation     ││
│  │ bean()           │ Methods on beans with a specific BEAN NAME (Spring only)   ││
│  │ this()           │ Methods on beans where the PROXY is of a specific type     ││
│  └──────────────────┴────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  Visual: How each designator "looks" at a method call differently:               │
│                                                                                  │
│  Client calls → proxy.createOrder(new OrderRequest(...))                         │
│                  ↑       ↑              ↑                                        │
│            this/target  execution     args/@args                                 │
│                  ↑       ↑              ↑                                        │
│               bean()   within()     @annotation                                  │
│                         @within()                                                │
│                         @target()                                                │
│                                                                                  │
│  execution  → "Does the method signature match?"                                 │
│  within     → "Is the class in the right package?"                               │
│  @annotation→ "Does the method have this annotation?"                            │
│  args       → "Are the runtime arguments of these types?"                        │
│  @args      → "Are the argument classes annotated with this?"                    │
│  target     → "Is the target bean of this class/interface?"                      │
│  bean       → "Is the bean name matching this pattern?"                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

For the following examples, assume this project structure:

```java
// ─── Domain Entities ───
@Entity
@Table(name = "orders")
@Data @NoArgsConstructor @AllArgsConstructor
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String product;
    private Double amount;
    private String status;
}

@Entity
@Table(name = "payments")
@Data @NoArgsConstructor @AllArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Double amount;
    private String method;
    private String status;
}
```

```java
// ─── DTOs ───
@Data @AllArgsConstructor
public class OrderRequest {
    private String product;
    private Double amount;
}

@Data @AllArgsConstructor
public class PaymentRequest {
    private Long orderId;
    private Double amount;
    private String method;
}
```

```java
// ─── Repositories ───
public interface OrderRepository extends JpaRepository<Order, Long> {}
public interface PaymentRepository extends JpaRepository<Payment, Long> {}
```

```java
// ─── Services ───
// Package: com.app.service

@Service
public class OrderService {
    @Autowired private OrderRepository orderRepo;

    public Order createOrder(String product, Double amount) {
        return orderRepo.save(new Order(null, product, amount, "CREATED"));
    }

    public Order createOrder(OrderRequest req) {
        return orderRepo.save(new Order(null, req.getProduct(), req.getAmount(), "CREATED"));
    }

    public Order getOrder(Long id) {
        return orderRepo.findById(id).orElseThrow();
    }

    public List<Order> getAllOrders() {
        return orderRepo.findAll();
    }

    public void cancelOrder(Long id) {
        Order order = orderRepo.findById(id).orElseThrow();
        order.setStatus("CANCELLED");
        orderRepo.save(order);
    }
}

@Service
public class PaymentService {
    @Autowired private PaymentRepository paymentRepo;

    public Payment processPayment(PaymentRequest req) {
        return paymentRepo.save(new Payment(null, req.getOrderId(),
            req.getAmount(), req.getMethod(), "PROCESSED"));
    }

    public Payment getPayment(Long id) {
        return paymentRepo.findById(id).orElseThrow();
    }

    public void refund(Long paymentId) {
        Payment p = paymentRepo.findById(paymentId).orElseThrow();
        p.setStatus("REFUNDED");
        paymentRepo.save(p);
    }
}
```

```java
// ─── Package: com.app.service.reporting (sub-package) ───
@Service
public class ReportService {
    public String generateReport(String type, int year) {
        return "Report: " + type + " for " + year;
    }
}
```

```java
// ─── Controllers ───
// Package: com.app.controller

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired private OrderService orderService;

    @PostMapping
    public Order create(@RequestBody OrderRequest req) {
        return orderService.createOrder(req);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id) {
        return orderService.getOrder(id);
    }
}

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    @Autowired private PaymentService paymentService;

    @PostMapping
    public Payment process(@RequestBody PaymentRequest req) {
        return paymentService.processPayment(req);
    }
}
```

---

#### 1. `execution()` — Match by Method Signature

**What:** Matches methods based on their **signature** — return type, declaring class, method name, and parameters. This is the **most commonly used** designator.

**Syntax:** `execution(modifiers? return-type declaring-type?.method-name(params) throws?)`

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  execution() — How It Decides:                                                   │
│                                                                                  │
│  For each method call, execution() asks:                                         │
│    "Does this method's SIGNATURE match the given pattern?"                       │
│                                                                                  │
│  It checks (in order):                                                           │
│    1. Does the return type match?      (e.g., * or Order or void)                │
│    2. Is the class in the right package? (e.g., com.app.service.*)               │
│    3. Does the method name match?      (e.g., create* or * or getOrder)          │
│    4. Do the parameter types match?    (e.g., (..) or (String, Double))          │
│                                                                                  │
│  execution() is STATIC — it matches based on the method declaration,             │
│  NOT based on runtime values or annotations.                                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 1: Match ONE specific method in ONE specific class ═══
@Aspect
@Component
@Slf4j
public class OrderCreationAspect {

    @Before("execution(* com.app.service.OrderService.createOrder(..))")
    public void beforeCreateOrder(JoinPoint jp) {
        log.info(">>> Creating order with args: {}", Arrays.toString(jp.getArgs()));
    }
}
// Matches:
//   ✓ OrderService.createOrder(String, Double)
//   ✓ OrderService.createOrder(OrderRequest)   (both overloads match because (..))
//   ✗ OrderService.getOrder(Long)
//   ✗ PaymentService.processPayment(PaymentRequest)
```

```java
// ═══ Example 2: Match ALL methods in a specific class ═══
@Aspect
@Component
@Slf4j
public class OrderServiceAspect {

    @Around("execution(* com.app.service.OrderService.*(..))")
    public Object logAllOrderOps(ProceedingJoinPoint pjp) throws Throwable {
        log.info(">>> OrderService.{}()", pjp.getSignature().getName());
        Object result = pjp.proceed();
        log.info("<<< OrderService.{}() returned: {}", pjp.getSignature().getName(), result);
        return result;
    }
}
// Matches ALL OrderService methods:
//   ✓ createOrder(String, Double)
//   ✓ createOrder(OrderRequest)
//   ✓ getOrder(Long)
//   ✓ getAllOrders()
//   ✓ cancelOrder(Long)
//   ✗ PaymentService.processPayment(...)  (different class)
```

```java
// ═══ Example 3: Match methods by name pattern across all service classes ═══
@Aspect
@Component
@Slf4j
public class GetterLoggingAspect {

    // All methods starting with "get" in any class in service package
    @Before("execution(* com.app.service.*.get*(..))")
    public void beforeGetter(JoinPoint jp) {
        log.info("GETTER: {}.{}()", jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
// Matches:
//   ✓ OrderService.getOrder(Long)
//   ✓ OrderService.getAllOrders()
//   ✓ PaymentService.getPayment(Long)
//   ✗ OrderService.createOrder(...)        (starts with "create")
//   ✗ PaymentService.processPayment(...)   (starts with "process")
//   ✗ ReportService.generateReport(...)    (in sub-package, and starts with "generate")
```

```java
// ═══ Example 4: Match by specific parameter types ═══
@Aspect
@Component
@Slf4j
public class RequestValidationAspect {

    // Only methods that take an OrderRequest as first parameter
    @Before("execution(* com.app.service.*.*(com.app.dto.OrderRequest, ..))")
    public void validateOrderRequest(JoinPoint jp) {
        OrderRequest req = (OrderRequest) jp.getArgs()[0];
        if (req.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (req.getProduct() == null || req.getProduct().isBlank()) {
            throw new IllegalArgumentException("Product name required");
        }
        log.info("VALIDATED: OrderRequest for {} at ${}", req.getProduct(), req.getAmount());
    }
}
// Matches:
//   ✓ OrderService.createOrder(OrderRequest req)
//   ✗ OrderService.createOrder(String product, Double amount)  (params don't match)
//   ✗ OrderService.getOrder(Long id)                           (Long ≠ OrderRequest)
//   ✗ PaymentService.processPayment(PaymentRequest req)        (PaymentRequest ≠ OrderRequest)
```

```java
// ═══ Example 5: Match void methods only (fire-and-forget operations) ═══
@Aspect
@Component
@Slf4j
public class VoidMethodAspect {

    @After("execution(void com.app.service.*.*(..))")
    public void afterVoidMethod(JoinPoint jp) {
        log.info("COMPLETED void operation: {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
// Matches:
//   ✓ OrderService.cancelOrder(Long)       → returns void
//   ✓ PaymentService.refund(Long)          → returns void
//   ✗ OrderService.createOrder(...)        → returns Order
//   ✗ OrderService.getOrder(Long)          → returns Order
//   ✗ PaymentService.getPayment(Long)      → returns Payment
```

---

#### 2. `within()` — Match All Methods in a Class or Package

**What:** Matches **all methods** within classes that match the given type pattern. Unlike `execution()`, which filters by method signature, `within()` filters by **which class the method belongs to**.

**Syntax:** `within(type-pattern)`

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  within() — How It Decides:                                                      │
│                                                                                  │
│  For each method call, within() asks:                                            │
│    "Is this method inside a class that matches the given type pattern?"          │
│                                                                                  │
│  It does NOT look at:                                                            │
│    • Method name                                                                 │
│    • Return type                                                                 │
│    • Parameters                                                                  │
│                                                                                  │
│  It ONLY looks at:                                                               │
│    • The class where the method is declared                                      │
│    • The package of that class                                                   │
│                                                                                  │
│  execution() vs within():                                                        │
│    execution(* com.app.service.*.*(..))  ← same result as:                       │
│    within(com.app.service.*)             ← shorter, cleaner                      │
│                                                                                  │
│  within() is like saying: "intercept EVERYTHING in this class/package"           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 1: Match ALL methods in a specific class ═══
@Aspect
@Component
@Slf4j
public class OrderServiceWithinAspect {

    @Before("within(com.app.service.OrderService)")
    //       └─────────────────────────────────────┘
    //       All methods in OrderService — no method/param filtering
    public void beforeAnyOrderMethod(JoinPoint jp) {
        log.info(">>> OrderService.{}()", jp.getSignature().getName());
    }
}
// Matches ALL methods in OrderService:
//   ✓ createOrder(String, Double)
//   ✓ createOrder(OrderRequest)
//   ✓ getOrder(Long)
//   ✓ getAllOrders()
//   ✓ cancelOrder(Long)
//   ✗ PaymentService.processPayment(...)  (different class)
//   ✗ ReportService.generateReport(...)   (different class)
```

```java
// ═══ Example 2: Match ALL methods in ALL classes in a package ═══
@Aspect
@Component
@Slf4j
public class ServiceLayerWithinAspect {

    @Before("within(com.app.service.*)")
    //                             ↑
    //               * = any class in the service package (ONE level)
    public void beforeAnyServiceMethod(JoinPoint jp) {
        log.info(">>> {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
// Matches:
//   ✓ OrderService.createOrder(...)      (in service package)
//   ✓ OrderService.getOrder(...)         (in service package)
//   ✓ PaymentService.processPayment(...) (in service package)
//   ✓ PaymentService.refund(...)         (in service package)
//   ✗ ReportService.generateReport(...)  (in service.reporting — sub-package!)
//   ✗ OrderController.create(...)        (in controller package)
//   ✗ OrderRepository.save(...)          (in repository package)
```

```java
// ═══ Example 3: Match ALL methods in a package AND all sub-packages ═══
@Aspect
@Component
@Slf4j
public class ServiceTreeWithinAspect {

    @Before("within(com.app.service..*)")
    //                              ↑↑
    //              .. = this package AND all sub-packages recursively
    public void beforeServiceTree(JoinPoint jp) {
        log.info(">>> {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
// Matches:
//   ✓ OrderService.createOrder(...)       (in com.app.service)
//   ✓ PaymentService.processPayment(...)  (in com.app.service)
//   ✓ ReportService.generateReport(...)   (in com.app.service.reporting) ← sub-package!
//   ✗ OrderController.create(...)         (in com.app.controller)
//   ✗ OrderRepository.save(...)           (in com.app.repository)
```

```java
// ═══ Example 4: Match ALL methods in ALL controllers ═══
@Aspect
@Component
@Slf4j
public class ControllerWithinAspect {

    @Around("within(com.app.controller.*)")
    public Object logControllerCalls(ProceedingJoinPoint pjp) throws Throwable {
        String controller = pjp.getTarget().getClass().getSimpleName();
        String method = pjp.getSignature().getName();
        log.info("HTTP >>> {}.{}()", controller, method);
        Object result = pjp.proceed();
        log.info("HTTP <<< {}.{}() → {}", controller, method, result);
        return result;
    }
}
// Matches:
//   ✓ OrderController.create(OrderRequest)
//   ✓ OrderController.get(Long)
//   ✓ PaymentController.process(PaymentRequest)
//   ✗ OrderService.createOrder(...)  (service package, not controller)
```

**`@within()` — Match methods in classes annotated with a specific annotation:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  within() vs @within():                                                          │
│                                                                                  │
│  within(com.app.service.OrderService)                                            │
│    → Match by CLASS NAME: all methods in OrderService                            │
│                                                                                  │
│  @within(org.springframework.stereotype.Service)                                 │
│    → Match by CLASS ANNOTATION: all methods in any class annotated with @Service │
│                                                                                  │
│  within  = "Is the class NAME matching?"                                         │
│  @within = "Does the class HAVE this annotation?"                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 5: Match ALL methods in classes annotated with @Service ═══
@Aspect
@Component
@Slf4j
public class ServiceAnnotationAspect {

    @Before("@within(org.springframework.stereotype.Service)")
    //       └──────────────────────────────────────────────────┘
    //       Matches any method in any class that has @Service annotation
    public void beforeAnyServiceBean(JoinPoint jp) {
        log.info("@Service >>> {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
// Matches (any method in a @Service-annotated class):
//   ✓ OrderService.createOrder(...)       → OrderService has @Service
//   ✓ OrderService.getOrder(...)          → OrderService has @Service
//   ✓ PaymentService.processPayment(...)  → PaymentService has @Service
//   ✓ ReportService.generateReport(...)   → ReportService has @Service
//   ✗ OrderController.create(...)         → has @RestController, NOT @Service
//   ✗ OrderRepository.save(...)           → extends JpaRepository, NOT @Service
```

```java
// ═══ Example 6: Match ALL methods in @RestController classes ═══
@Aspect
@Component
@Slf4j
public class RestControllerAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logRestCalls(ProceedingJoinPoint pjp) throws Throwable {
        String controller = pjp.getTarget().getClass().getSimpleName();
        String method = pjp.getSignature().getName();
        log.info("REST >>> {}.{}() | args: {}", controller, method,
            Arrays.toString(pjp.getArgs()));
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        log.info("REST <<< {}.{}() | {}ms | result: {}", controller, method,
            System.currentTimeMillis() - start, result);
        return result;
    }
}
// Matches ALL methods in ANY @RestController class:
//   ✓ OrderController.create(OrderRequest)
//   ✓ OrderController.get(Long)
//   ✓ PaymentController.process(PaymentRequest)
//   ✗ OrderService.createOrder(...)  (has @Service, not @RestController)
```

```java
// ═══ Example 7: Custom annotation on class — using @within ═══

// Custom annotation applied to CLASSES
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitored {
    String value() default "";
}

// Apply to specific service classes
@Service
@Monitored("ORDER")
public class OrderService {
    // ALL methods in this class will be intercepted
    public Order createOrder(OrderRequest req) { ... }
    public Order getOrder(Long id) { ... }
    public List<Order> getAllOrders() { ... }
}

@Service
// No @Monitored annotation!
public class PaymentService {
    // Methods here will NOT be intercepted
    public Payment processPayment(PaymentRequest req) { ... }
}

@Aspect
@Component
@Slf4j
public class MonitoredClassAspect {

    @Around("@within(com.app.annotation.Monitored)")
    public Object monitorClass(ProceedingJoinPoint pjp) throws Throwable {
        log.info("MONITOR >>> {}.{}()",
            pjp.getTarget().getClass().getSimpleName(),
            pjp.getSignature().getName());
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        log.info("MONITOR <<< {}.{}() completed in {}ms",
            pjp.getTarget().getClass().getSimpleName(),
            pjp.getSignature().getName(),
            System.currentTimeMillis() - start);
        return result;
    }
}
// Matches:
//   ✓ OrderService.createOrder(...)   → OrderService has @Monitored
//   ✓ OrderService.getOrder(...)      → OrderService has @Monitored
//   ✓ OrderService.getAllOrders()      → OrderService has @Monitored
//   ✗ PaymentService.processPayment() → PaymentService does NOT have @Monitored
//   ✗ PaymentService.getPayment()     → PaymentService does NOT have @Monitored
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  within() vs @within() — Comparison:                                             │
│                                                                                  │
│  ┌─────────────────────────────────────────────┬─────────────────────────────────┐│
│  │ within()                                    │ @within()                       ││
│  ├─────────────────────────────────────────────┼─────────────────────────────────┤│
│  │ Matches by CLASS/PACKAGE name               │ Matches by CLASS ANNOTATION    ││
│  │ within(com.app.service.OrderService)        │ @within(Service)               ││
│  │ within(com.app.service.*)                   │ @within(RestController)         ││
│  │ within(com.app.service..*)                  │ @within(Monitored)              ││
│  │ Checked at COMPILE time (static)            │ Checked at STARTUP time        ││
│  │ Tightly coupled to package structure        │ Decoupled — works by annotation││
│  │ Breaks if package is renamed                │ Survives refactoring            ││
│  │ No custom annotation needed                 │ Can use custom annotations     ││
│  └─────────────────────────────────────────────┴─────────────────────────────────┘│
│                                                                                  │
│  When to use which:                                                              │
│    within()  → when you want to target all methods in a known package/class      │
│    @within() → when you want to target all methods in annotated classes          │
│               (more flexible, survives refactoring)                              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 3. `@annotation()` — Match Methods With a Specific Annotation

**What:** Matches any method that is **annotated** with the specified annotation. Unlike `execution()` which matches by signature, and `within()` which matches by class, `@annotation()` matches by the **annotation on the method itself**.

**Syntax:** `@annotation(fully.qualified.AnnotationName)` or `@annotation(paramName)` for binding.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @annotation() — How It Decides:                                                 │
│                                                                                  │
│  For each method call, @annotation() asks:                                       │
│    "Does this method have the specified annotation on it?"                       │
│                                                                                  │
│  It does NOT look at:                                                            │
│    • Method name, return type, parameters                                        │
│    • Which class or package the method is in                                     │
│    • Annotations on the CLASS (that's @within)                                   │
│                                                                                  │
│  It ONLY looks at:                                                               │
│    • Annotations directly on the METHOD being called                             │
│                                                                                  │
│  @annotation vs @within:                                                         │
│    @annotation → checks METHODS for annotations                                 │
│    @within     → checks CLASSES for annotations                                 │
│                                                                                  │
│  Advantage:                                                                      │
│    Developer explicitly opts in per method by adding the annotation.             │
│    Most explicit and refactoring-safe approach.                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 1: Simple custom annotation matching ═══

// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    String value() default "";
}

// Aspect that intercepts @Loggable methods
@Aspect
@Component
@Slf4j
public class LoggableAspect {

    @Around("@annotation(com.app.annotation.Loggable)")
    //       └───────────────────────────────────────────┘
    //       Any method with @Loggable, in ANY class, ANY package
    public Object logMethod(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        log.info(">>> {}", method);
        Object result = pjp.proceed();
        log.info("<<< {} returned: {}", method, result);
        return result;
    }
}

// Usage in services
@Service
public class OrderService {

    @Loggable("CREATE-ORDER")  // ← MATCHES
    public Order createOrder(OrderRequest req) {
        return orderRepo.save(new Order(null, req.getProduct(), req.getAmount(), "CREATED"));
    }

    // No @Loggable annotation → NOT matched
    public Order getOrder(Long id) {
        return orderRepo.findById(id).orElseThrow();
    }

    @Loggable("CANCEL-ORDER")  // ← MATCHES
    public void cancelOrder(Long id) {
        Order order = orderRepo.findById(id).orElseThrow();
        order.setStatus("CANCELLED");
        orderRepo.save(order);
    }
}

@Service
public class PaymentService {

    @Loggable("PROCESS-PAYMENT")  // ← MATCHES (works across different classes!)
    public Payment processPayment(PaymentRequest req) {
        return paymentRepo.save(new Payment(...));
    }

    // No @Loggable → NOT matched
    public Payment getPayment(Long id) {
        return paymentRepo.findById(id).orElseThrow();
    }
}
```

```java
// ═══ Example 2: Binding the annotation to access its attributes ═══

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();       // e.g., "CREATE_ORDER", "CANCEL_ORDER"
    String entity();       // e.g., "Order", "Payment"
}

@Aspect
@Component
@Slf4j
public class AuditAspect {

    // Using parameter binding: @annotation(auditable) binds to the parameter name
    @Around("@annotation(auditable)")
    //                    ↑ lowercase — matches the method parameter name
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        //                                        ↑ Spring injects the annotation instance

        // Access annotation attributes:
        String action = auditable.action();   // "CREATE_ORDER"
        String entity = auditable.entity();   // "Order"

        log.info("AUDIT: action={}, entity={}, method={}, args={}",
            action, entity,
            pjp.getSignature().getName(),
            Arrays.toString(pjp.getArgs()));

        Object result = pjp.proceed();

        log.info("AUDIT: action={} completed successfully | result={}", action, result);
        return result;
    }
}

// Usage
@Service
public class OrderService {

    @Auditable(action = "CREATE_ORDER", entity = "Order")
    public Order createOrder(OrderRequest req) {
        return orderRepo.save(new Order(null, req.getProduct(), req.getAmount(), "CREATED"));
    }

    @Auditable(action = "CANCEL_ORDER", entity = "Order")
    public void cancelOrder(Long id) {
        Order order = orderRepo.findById(id).orElseThrow();
        order.setStatus("CANCELLED");
        orderRepo.save(order);
    }
}

// Console Output:
//   AUDIT: action=CREATE_ORDER, entity=Order, method=createOrder,
//          args=[OrderRequest(product=Laptop, amount=999.99)]
//   AUDIT: action=CREATE_ORDER completed successfully | result=Order(id=1, ...)
```

```java
// ═══ Example 3: Rate limiting via @annotation ═══

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int maxRequests() default 10;
    int windowSeconds() default 60;
}

@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private final Map<String, List<Long>> requestLog = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = pjp.getSignature().toShortString();
        int max = rateLimit.maxRequests();
        int window = rateLimit.windowSeconds();

        long now = System.currentTimeMillis();
        long windowStart = now - (window * 1000L);

        requestLog.computeIfAbsent(key, k -> new ArrayList<>());
        List<Long> timestamps = requestLog.get(key);
        timestamps.removeIf(t -> t < windowStart);  // remove expired

        if (timestamps.size() >= max) {
            throw new RuntimeException("Rate limit exceeded: " + max + " requests per " + window + "s");
        }

        timestamps.add(now);
        return pjp.proceed();
    }
}

// Usage — any method in any class can use @RateLimit:
@Service
public class OrderService {

    @RateLimit(maxRequests = 5, windowSeconds = 60)
    public Order createOrder(OrderRequest req) { ... }
}

@RestController
public class ReportController {

    @RateLimit(maxRequests = 2, windowSeconds = 300)  // stricter limit
    @GetMapping("/reports/expensive")
    public Report generateExpensiveReport() { ... }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @annotation() — Key Points:                                                     │
│                                                                                  │
│  Two ways to use it:                                                             │
│                                                                                  │
│  1. Fully qualified name (no binding):                                           │
│     @Before("@annotation(com.app.annotation.Loggable)")                          │
│     public void before(JoinPoint jp) { }                                         │
│     → Cannot access annotation attributes.                                       │
│                                                                                  │
│  2. Parameter binding (lowercase name):                                          │
│     @Before("@annotation(loggable)")                                             │
│     public void before(JoinPoint jp, Loggable loggable) {                        │
│         String value = loggable.value();  ← ACCESS attributes!                   │
│     }                                                                            │
│     → Spring matches the parameter name "loggable" to @annotation(loggable)      │
│     → Spring also uses the parameter TYPE (Loggable) to know which annotation    │
│                                                                                  │
│  REMEMBER:                                                                       │
│    • @Retention(RetentionPolicy.RUNTIME) is REQUIRED on the annotation           │
│    • Without RUNTIME retention, Spring cannot see the annotation at runtime       │
│    • @annotation matches METHOD annotations only                                 │
│    • For CLASS annotations, use @within                                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 4. `args()` — Match by Runtime Argument Types

**What:** Matches methods where the **runtime argument types** match the specified types. Unlike the parameter list in `execution()` which matches by **declared parameter types**, `args()` matches by the **actual runtime types** of the arguments being passed.

**Syntax:** `args(type1, type2, ...)` or `args(paramName)` for binding.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  args() vs execution() parameters — What's the difference?                       │
│                                                                                  │
│  execution(* *(.., String))                                                      │
│    → Matches if the method is DECLARED with last param as String                 │
│    → Checks the method SIGNATURE (static, at compile time)                       │
│                                                                                  │
│  args(.., String)                                                                │
│    → Matches if the RUNTIME argument is actually a String                        │
│    → Checks the actual OBJECT being passed (dynamic, at runtime)                 │
│                                                                                  │
│  When does this matter?                                                          │
│    When a method accepts Object but receives a String:                           │
│      void process(Object data) { ... }                                           │
│      process("hello")  ← runtime arg is String                                  │
│                                                                                  │
│      execution(* *(String))  → ✗ NO (declared param is Object)                  │
│      args(String)            → ✓ YES (runtime arg IS String)                    │
│                                                                                  │
│  Also:                                                                           │
│    args() can BIND the arguments to advice method parameters.                    │
│    execution() parameter list CANNOT bind.                                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 1: Match methods that receive a String argument ═══
@Aspect
@Component
@Slf4j
public class StringArgAspect {

    @Before("args(String)")
    //       └──────────┘
    //       ANY method that is called with exactly ONE String argument at runtime
    public void beforeStringArg(JoinPoint jp) {
        log.info("Method called with String arg: {}", jp.getArgs()[0]);
    }
}
// Matches:
//   ✓ orderService.getByStatus("ACTIVE")            → 1 String arg
//   ✗ orderService.getOrder(1L)                      → 1 Long arg
//   ✗ orderService.createOrder("Laptop", 999.99)     → 2 args (not exactly 1 String)
//   ✗ orderService.getAllOrders()                     → 0 args
```

```java
// ═══ Example 2: Match methods that receive a custom DTO type ═══
@Aspect
@Component
@Slf4j
public class OrderRequestAspect {

    // Bind the argument to a parameter:
    @Before("args(orderRequest)")
    //            ↑ parameter name — matches the advice method parameter
    public void beforeOrderRequest(JoinPoint jp, OrderRequest orderRequest) {
        //                                        ↑ Spring injects the actual argument

        // Direct access — no casting needed!
        log.info("Order Request received: product={}, amount={}",
            orderRequest.getProduct(), orderRequest.getAmount());

        // Validate
        if (orderRequest.getAmount() > 100000) {
            log.warn("HIGH VALUE ORDER: ${}", orderRequest.getAmount());
        }
    }
}
// Matches ANY method in ANY class called with exactly one OrderRequest:
//   ✓ orderService.createOrder(OrderRequest req)
//   ✓ orderController.create(OrderRequest req)  (even in controllers!)
//   ✗ paymentService.processPayment(PaymentRequest req)  (PaymentRequest ≠ OrderRequest)
//   ✗ orderService.createOrder(String, Double)  (different arg types)
```

```java
// ═══ Example 3: Match methods with multiple specific argument types ═══
@Aspect
@Component
@Slf4j
public class MultiArgAspect {

    // Match methods with exactly (String, Double)
    @Before("args(product, amount)")
    public void beforeStringDouble(JoinPoint jp, String product, Double amount) {
        log.info("Product: {}, Amount: ${}", product, amount);
    }
}
// Matches:
//   ✓ orderService.createOrder("Laptop", 999.99)  → (String, Double)
//   ✗ orderService.createOrder(OrderRequest req)   → (OrderRequest) — different types
//   ✗ orderService.getOrder(1L)                    → (Long) — different
```

```java
// ═══ Example 4: Match with wildcard — first arg specific, rest anything ═══
@Aspect
@Component
@Slf4j
public class FirstArgStringAspect {

    @Before("args(name, ..)")
    //             ↑     ↑↑
    //      1st arg   rest can be anything (zero or more)
    public void beforeFirstArgString(JoinPoint jp, String name) {
        log.info("First argument is String: {}", name);
    }
}
// Matches:
//   ✓ orderService.createOrder("Laptop", 999.99)     → 1st=String, 2nd=Double
//   ✓ orderService.getByStatus("ACTIVE")             → 1st=String
//   ✓ reportService.generateReport("monthly", 2025)  → 1st=String, 2nd=int
//   ✗ orderService.getOrder(1L)                       → 1st=Long (not String)
//   ✗ orderService.getAllOrders()                      → no args
```

```java
// ═══ Example 5: args() with custom DTO and Long — practical validation ═══
@Aspect
@Component
@Slf4j
public class PaymentValidationAspect {

    @Before("args(paymentRequest) && within(com.app.service.*)")
    //       ↑ bind PaymentRequest    ↑ only within service layer
    public void validatePayment(JoinPoint jp, PaymentRequest paymentRequest) {
        if (paymentRequest.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (paymentRequest.getMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        log.info("Payment validated: orderId={}, amount={}, method={}",
            paymentRequest.getOrderId(),
            paymentRequest.getAmount(),
            paymentRequest.getMethod());
    }
}
// Matches:
//   ✓ paymentService.processPayment(PaymentRequest req)  → PaymentRequest in service
//   ✗ paymentController.process(PaymentRequest req)      → PaymentRequest but in controller
//   ✗ orderService.createOrder(OrderRequest req)         → OrderRequest ≠ PaymentRequest
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  args() — Key Points:                                                            │
│                                                                                  │
│  1. Matches by RUNTIME argument types (not declared parameter types)             │
│  2. Can BIND arguments directly to advice method parameters                      │
│  3. Uses parameter names for binding (lowercase, matching advice params)         │
│  4. Use (..) for zero or more args, (*) for one arg of any type                  │
│  5. Common pattern: combine with within() to limit scope                         │
│     args(OrderRequest) — too broad (matches everywhere)                          │
│     args(OrderRequest) && within(com.app.service.*) — properly scoped            │
│                                                                                  │
│  ┌────────────────────────────────────┬──────────────────────────────────────────┐│
│  │ args Pattern                       │ Matches                                  ││
│  ├────────────────────────────────────┼──────────────────────────────────────────┤│
│  │ args(String)                       │ 1 String arg                             ││
│  │ args(Long)                         │ 1 Long arg                               ││
│  │ args(OrderRequest)                 │ 1 OrderRequest arg                       ││
│  │ args(String, Double)               │ String then Double (exactly 2)           ││
│  │ args(String, ..)                   │ First is String, rest anything           ││
│  │ args(.., Long)                     │ Any args, last is Long                   ││
│  │ args(..)                           │ Any args (pointless — matches everything)││
│  └────────────────────────────────────┴──────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5. `@args()` — Match When Argument's Class Has a Specific Annotation

**What:** Matches methods where the **runtime argument's class** is annotated with a specified annotation. This is different from `args()` which matches by argument type — `@args()` checks if the **argument class itself** carries a particular annotation.

**Syntax:** `@args(fully.qualified.AnnotationName)`

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @args() — How It Decides:                                                       │
│                                                                                  │
│  For each method call, @args() asks:                                             │
│    "Is the runtime argument's CLASS annotated with this annotation?"             │
│                                                                                  │
│  args(OrderRequest)                                                              │
│    → "Is the argument an OrderRequest?"                                          │
│    → Checks the TYPE of the argument                                             │
│                                                                                  │
│  @args(Validated)                                                                │
│    → "Does the argument's class have @Validated annotation?"                     │
│    → Checks for an ANNOTATION on the argument's CLASS                            │
│                                                                                  │
│  Example:                                                                        │
│    @Validated                        ← annotation on the CLASS                   │
│    public class OrderRequest {                                                   │
│        private String product;                                                   │
│        private Double amount;                                                    │
│    }                                                                             │
│                                                                                  │
│    orderService.createOrder(new OrderRequest("Laptop", 999.99))                  │
│                              └────────────────────────────────┘                   │
│                              This object's CLASS (OrderRequest)                   │
│                              has @Validated → @args(Validated) MATCHES            │
│                                                                                  │
│  Use case: Apply advice to methods that receive "validated" or "auditable"       │
│  data objects — without knowing the exact type of each DTO.                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 1: Custom annotation on DTO classes ═══

// Annotation applied to DTO CLASSES (not methods!)
@Target(ElementType.TYPE)        // ← applied to classes
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedInput {}

// DTOs with the annotation
@ValidatedInput                  // ← annotation on the CLASS
@Data @AllArgsConstructor
public class OrderRequest {
    private String product;
    private Double amount;
}

@ValidatedInput                  // ← annotation on the CLASS
@Data @AllArgsConstructor
public class PaymentRequest {
    private Long orderId;
    private Double amount;
    private String method;
}

// DTO WITHOUT the annotation
@Data @AllArgsConstructor
public class ReportRequest {
    private String type;
    private int year;
}

// Aspect matching by @args
@Aspect
@Component
@Slf4j
public class ValidatedInputAspect {

    @Before("@args(com.app.annotation.ValidatedInput)")
    //       └──────────────────────────────────────────┘
    //       Matches any method whose argument's CLASS has @ValidatedInput
    public void beforeValidatedInput(JoinPoint jp) {
        Object arg = jp.getArgs()[0];
        log.info("VALIDATED INPUT: method={}, argType={}, value={}",
            jp.getSignature().getName(),
            arg.getClass().getSimpleName(),
            arg);
    }
}

// Results:
//   orderService.createOrder(new OrderRequest("Laptop", 999.99))
//     → MATCHES ✓ (OrderRequest class has @ValidatedInput)
//
//   paymentService.processPayment(new PaymentRequest(1L, 999.99, "CARD"))
//     → MATCHES ✓ (PaymentRequest class has @ValidatedInput)
//
//   reportService.generateReport(new ReportRequest("monthly", 2025))
//     → NO MATCH ✗ (ReportRequest does NOT have @ValidatedInput)
//
//   orderService.getOrder(1L)
//     → NO MATCH ✗ (Long class does not have @ValidatedInput)
```

```java
// ═══ Example 2: Auto-validation with @args ═══

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoValidate {}

@AutoValidate
@Data @AllArgsConstructor
public class OrderRequest {
    @NotNull private String product;
    @Positive private Double amount;
}

@Aspect
@Component
@Slf4j
public class AutoValidateAspect {

    @Autowired
    private Validator validator;

    @Before("@args(com.app.annotation.AutoValidate) && within(com.app.service.*)")
    public void autoValidate(JoinPoint jp) {
        for (Object arg : jp.getArgs()) {
            if (arg != null && arg.getClass().isAnnotationPresent(AutoValidate.class)) {
                Set<ConstraintViolation<Object>> violations = validator.validate(arg);
                if (!violations.isEmpty()) {
                    String errors = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                    throw new IllegalArgumentException("Validation failed: " + errors);
                }
                log.info("Auto-validated: {}", arg.getClass().getSimpleName());
            }
        }
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @args() vs args() — Comparison:                                                 │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ args()                                   │ @args()                            ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ Matches by argument TYPE                 │ Matches by annotation on           ││
│  │                                          │ argument's CLASS                   ││
│  │                                          │                                    ││
│  │ args(OrderRequest)                       │ @args(ValidatedInput)              ││
│  │ "Is the arg an OrderRequest?"            │ "Does the arg's class have         ││
│  │                                          │  @ValidatedInput?"                 ││
│  │                                          │                                    ││
│  │ You must know the exact DTO type         │ Works for ANY class that has       ││
│  │                                          │ the annotation                     ││
│  │                                          │                                    ││
│  │ args(OrderRequest) — matches 1 type      │ @args(ValidatedInput) — matches    ││
│  │                                          │ OrderRequest, PaymentRequest,      ││
│  │                                          │ UserRequest — ALL with annotation  ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  @args() is useful when:                                                         │
│    • You have many DTO types that share a common annotation                      │
│    • You want to apply advice based on DTO category, not exact type              │
│    • Example: all @SensitiveData DTOs get masked in logs                         │
│    • Example: all @AutoValidate DTOs get validated before processing             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 6. `target()` — Match by Target Bean's Class or Interface

**What:** Matches methods on beans where the **target object** (the actual bean, not the proxy) is of a specific type. This can match by **class** or by **interface**, making it useful for intercepting all implementations of an interface.

**Syntax:** `target(fully.qualified.ClassName)` or `target(fully.qualified.InterfaceName)`

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  target() — How It Decides:                                                      │
│                                                                                  │
│  For each method call, target() asks:                                            │
│    "Is the TARGET OBJECT (the real bean behind the proxy) an instance of         │
│     this class or does it implement this interface?"                             │
│                                                                                  │
│  target() uses instanceof check at runtime:                                      │
│    target(OrderService) → targetBean instanceof OrderService                     │
│    target(PaymentProcessor) → targetBean instanceof PaymentProcessor (interface) │
│                                                                                  │
│  target() vs within():                                                           │
│    within(com.app.service.OrderService)                                          │
│      → Matches if method is DECLARED in OrderService                             │
│      → STATIC check based on class name                                          │
│                                                                                  │
│    target(com.app.service.OrderService)                                          │
│      → Matches if the runtime object IS an OrderService (or subclass)            │
│      → DYNAMIC check using instanceof                                            │
│      → Also matches subclasses and interface implementations                     │
│                                                                                  │
│  target() vs this():                                                             │
│    target() → checks the actual bean (behind the proxy)                          │
│    this()   → checks the proxy object itself                                     │
│    Usually produce the same results, but differ with JDK proxies.                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 1: Match by specific class ═══
@Aspect
@Component
@Slf4j
public class TargetClassAspect {

    @Before("target(com.app.service.OrderService)")
    //       └────────────────────────────────────┘
    //       All methods on beans that ARE OrderService instances
    public void beforeOrderService(JoinPoint jp) {
        log.info("TARGET: OrderService.{}()", jp.getSignature().getName());
    }
}
// Matches ALL methods on OrderService bean:
//   ✓ orderService.createOrder(...)
//   ✓ orderService.getOrder(...)
//   ✓ orderService.cancelOrder(...)
//   ✗ paymentService.processPayment(...)  (not an OrderService instance)
```

```java
// ═══ Example 2: Match by INTERFACE — this is where target() shines! ═══

// Define an interface
public interface PaymentProcessor {
    Payment process(PaymentRequest req);
    void refund(Long paymentId);
}

// Multiple implementations
@Service
public class CreditCardProcessor implements PaymentProcessor {
    @Override
    public Payment process(PaymentRequest req) {
        // credit card processing logic
        return new Payment(null, req.getOrderId(), req.getAmount(), "CREDIT_CARD", "PROCESSED");
    }

    @Override
    public void refund(Long paymentId) {
        // credit card refund logic
    }

    // Extra method not in interface
    public String getProcessorName() {
        return "CreditCard";
    }
}

@Service
public class PayPalProcessor implements PaymentProcessor {
    @Override
    public Payment process(PaymentRequest req) {
        // PayPal processing logic
        return new Payment(null, req.getOrderId(), req.getAmount(), "PAYPAL", "PROCESSED");
    }

    @Override
    public void refund(Long paymentId) {
        // PayPal refund logic
    }
}

@Service
public class CryptoProcessor implements PaymentProcessor {
    @Override
    public Payment process(PaymentRequest req) {
        // Crypto processing logic
        return new Payment(null, req.getOrderId(), req.getAmount(), "CRYPTO", "PROCESSED");
    }

    @Override
    public void refund(Long paymentId) {
        // Crypto refund logic
    }
}

// Aspect matching ALL implementations of PaymentProcessor interface
@Aspect
@Component
@Slf4j
public class PaymentProcessorAspect {

    @Around("target(com.app.service.PaymentProcessor)")
    //       └──────────────────────────────────────────┘
    //       Matches ALL methods on ALL beans that implement PaymentProcessor
    public Object monitorPayment(ProceedingJoinPoint pjp) throws Throwable {
        String impl = pjp.getTarget().getClass().getSimpleName();
        String method = pjp.getSignature().getName();

        log.info("PAYMENT >>> {}.{}() | args: {}",
            impl, method, Arrays.toString(pjp.getArgs()));

        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long duration = System.currentTimeMillis() - start;

        log.info("PAYMENT <<< {}.{}() completed in {}ms | result: {}",
            impl, method, duration, result);
        return result;
    }
}

// Matches:
//   ✓ creditCardProcessor.process(req)     → implements PaymentProcessor
//   ✓ creditCardProcessor.refund(1L)       → implements PaymentProcessor
//   ✓ creditCardProcessor.getProcessorName() → target IS a PaymentProcessor (all methods!)
//   ✓ payPalProcessor.process(req)         → implements PaymentProcessor
//   ✓ payPalProcessor.refund(1L)           → implements PaymentProcessor
//   ✓ cryptoProcessor.process(req)         → implements PaymentProcessor
//   ✓ cryptoProcessor.refund(1L)           → implements PaymentProcessor
//   ✗ orderService.createOrder(...)        → does NOT implement PaymentProcessor
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  target(Interface) — Visual:                                                     │
│                                                                                  │
│                    ┌──────────────────────────┐                                  │
│                    │  interface               │                                  │
│                    │  PaymentProcessor        │                                  │
│                    │  • process()             │                                  │
│                    │  • refund()              │                                  │
│                    └────────────┬─────────────┘                                  │
│                                 │                                                │
│              ┌──────────────────┼──────────────────┐                             │
│              │                  │                   │                             │
│              v                  v                   v                             │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐                  │
│  │CreditCardProcessor│ │PayPalProcessor  │ │CryptoProcessor   │                  │
│  │ process()        │ │ process()       │ │ process()        │                  │
│  │ refund()         │ │ refund()        │ │ refund()         │                  │
│  │ getProcessorName()│ │                 │ │                  │                  │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘                  │
│          ↑                      ↑                    ↑                            │
│          ALL matched by: target(PaymentProcessor)                                │
│          ALL methods on ALL implementations are intercepted!                     │
│                                                                                  │
│  This is POWERFUL because:                                                       │
│    • Add a new implementation (e.g., BankTransferProcessor)?                     │
│      → Automatically intercepted! Zero Aspect changes needed.                   │
│    • Even methods NOT in the interface are matched (getProcessorName)            │
│    • target() uses instanceof → works with class hierarchy                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 3: target() with class hierarchy (inheritance) ═══

// Base class
public abstract class BaseService {
    public void logActivity(String action) {
        System.out.println("Activity: " + action);
    }
}

// Subclasses
@Service
public class OrderService extends BaseService {
    public Order createOrder(OrderRequest req) { ... }
}

@Service
public class PaymentService extends BaseService {
    public Payment processPayment(PaymentRequest req) { ... }
}

@Service
public class ReportService {  // Does NOT extend BaseService
    public String generateReport(String type) { ... }
}

@Aspect
@Component
@Slf4j
public class BaseServiceAspect {

    @Before("target(com.app.service.BaseService)")
    //       └────────────────────────────────────┘
    //       All methods on beans that are instances of BaseService (or subclass)
    public void beforeBaseService(JoinPoint jp) {
        log.info("BaseService >>> {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
// Matches (because instanceof BaseService):
//   ✓ orderService.createOrder(...)   → OrderService extends BaseService
//   ✓ orderService.logActivity(...)   → inherited method
//   ✓ paymentService.processPayment() → PaymentService extends BaseService
//   ✓ paymentService.logActivity(...) → inherited method
//   ✗ reportService.generateReport()  → does NOT extend BaseService
```

```java
// ═══ Example 4: Binding the target object ═══
@Aspect
@Component
@Slf4j
public class TargetBindingAspect {

    // Bind target bean to a parameter
    @Before("target(orderService)")
    //              ↑ parameter name (matches advice method param)
    public void beforeWithTarget(JoinPoint jp, OrderService orderService) {
        //                                     ↑ Spring injects the actual target bean
        log.info("Target bean class: {}", orderService.getClass().getSimpleName());
        // You now have direct access to the target bean!
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  target() — Key Points:                                                          │
│                                                                                  │
│  ✓ YES — you can use INTERFACES with target():                                   │
│    target(PaymentProcessor) matches ALL classes implementing PaymentProcessor     │
│                                                                                  │
│  ✓ YES — you can use ABSTRACT CLASSES with target():                             │
│    target(BaseService) matches ALL classes extending BaseService                  │
│                                                                                  │
│  ✓ YES — target() matches ALL methods (not just interface methods):              │
│    Even methods not declared in the interface are matched.                        │
│    Because target() checks "is the bean an instance of X?"                       │
│    not "is this method declared in X?"                                            │
│                                                                                  │
│  target() vs execution() vs within():                                            │
│  ┌──────────────────────┬────────────────────────────────────────────────────────┐│
│  │ Designator           │ How it matches                                         ││
│  ├──────────────────────┼────────────────────────────────────────────────────────┤│
│  │ execution()          │ By method SIGNATURE (name, return, params)             ││
│  │ within()             │ By DECLARING class/package (static, name-based)        ││
│  │ target()             │ By TARGET bean TYPE (dynamic, instanceof)              ││
│  │                      │ Works with interfaces and inheritance!                 ││
│  └──────────────────────┴────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Pointcut Designator Types — Complete Comparison Table

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  All Pointcut Designator Types — When to Use Each:                               │
│                                                                                  │
│  ┌──────────────────┬──────────────────────────────────────┬─────────────────────┐│
│  │ Designator       │ Use When You Want To Match By        │ Example             ││
│  ├──────────────────┼──────────────────────────────────────┼─────────────────────┤│
│  │ execution()      │ Method name, params, return type     │ execution(* *.get*  ││
│  │                  │ Most common, most flexible           │ (..))               ││
│  ├──────────────────┼──────────────────────────────────────┼─────────────────────┤│
│  │ within()         │ All methods in a class/package       │ within(com.app      ││
│  │                  │ by NAME (static)                     │ .service.*)         ││
│  ├──────────────────┼──────────────────────────────────────┼─────────────────────┤│
│  │ @within()        │ All methods in classes that HAVE     │ @within(Service)    ││
│  │                  │ a specific annotation                │ @within(Monitored)  ││
│  ├──────────────────┼──────────────────────────────────────┼─────────────────────┤│
│  │ @annotation()    │ Methods that HAVE a specific         │ @annotation         ││
│  │                  │ annotation on them                   │ (Loggable)          ││
│  ├──────────────────┼──────────────────────────────────────┼─────────────────────┤│
│  │ args()           │ Methods with specific runtime        │ args(OrderRequest)  ││
│  │                  │ argument TYPES                       │ args(String, ..)    ││
│  ├──────────────────┼──────────────────────────────────────┼─────────────────────┤│
│  │ @args()          │ Methods where argument's CLASS       │ @args(Validated     ││
│  │                  │ has a specific annotation            │ Input)              ││
│  ├──────────────────┼──────────────────────────────────────┼─────────────────────┤│
│  │ target()         │ Methods on beans of a specific       │ target(Payment      ││
│  │                  │ class/interface (instanceof)         │ Processor)          ││
│  ├──────────────────┼──────────────────────────────────────┼─────────────────────┤│
│  │ bean()           │ Methods on beans with a specific     │ bean(orderService)  ││
│  │                  │ bean NAME (Spring-specific)          │ bean(*Service)      ││
│  └──────────────────┴──────────────────────────────────────┴─────────────────────┘│
│                                                                                  │
│  Decision Guide:                                                                 │
│    "I want to match..."                                                          │
│    ...by method signature?            → execution()                              │
│    ...all methods in a package?       → within()                                 │
│    ...all methods in @Service classes? → @within(Service)                         │
│    ...methods with @Loggable?         → @annotation(Loggable)                    │
│    ...methods that take OrderRequest? → args(OrderRequest)                       │
│    ...methods on PaymentProcessor?    → target(PaymentProcessor)                 │
│    ...methods on bean "orderService"? → bean(orderService)                       │
│    ...methods with @Validated DTOs?   → @args(ValidatedInput)                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


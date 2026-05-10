### How AOP Achieves Reusability and Maintainability

**Reusability:** An Aspect is defined ONCE and applied to any number of methods/classes through pointcut expressions. The same `LoggingAspect` that logs `OrderService` methods can also log `PaymentService`, `UserService`, `ReportService` — without changing a single line in those services.

**Maintainability:** When a cross-cutting concern needs to change (e.g., log format, security rules, transaction isolation level), you update ONE place (the Aspect), and the change automatically applies everywhere it's used.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  REUSABILITY — One Aspect, Many Targets:                                         │
│                                                                                  │
│                    ┌─────────────────────────┐                                   │
│                    │   LoggingAspect          │ ← Defined ONCE (30 lines)        │
│                    │   @Around(...)           │                                   │
│                    └────────────┬────────────┘                                   │
│                                 │                                                │
│              ┌──────────────────┼──────────────────┐                             │
│              │                  │                  │                             │
│              v                  v                  v                             │
│  ┌───────────────────┐ ┌───────────────────┐ ┌───────────────────┐              │
│  │ OrderService      │ │ PaymentService    │ │ UserService       │              │
│  │ • createOrder()   │ │ • processPayment()│ │ • createUser()    │              │
│  │ • cancelOrder()   │ │ • refundPayment() │ │ • updateUser()    │              │
│  │ • getOrder()      │ │ • getPayment()    │ │ • deleteUser()    │              │
│  └───────────────────┘ └───────────────────┘ └───────────────────┘              │
│         ↑                       ↑                      ↑                         │
│         ALL methods automatically logged by the ONE Aspect                       │
│         NONE of these services have any logging code                             │
│                                                                                  │
│  Add a NEW service (InventoryService)?                                           │
│    → Automatically covered by LoggingAspect! Zero configuration needed.          │
│    → Just place it in the matching package (com.app.service.*)                   │
│                                                                                  │
│  Reusability via Custom Annotations (even more explicit):                        │
│                                                                                  │
│  @Loggable("ORDER")              ← annotate any method to opt-in                │
│  public Order createOrder() {}                                                   │
│                                                                                  │
│  @Loggable("PAYMENT")            ← same Aspect, different context               │
│  public Payment process() {}                                                     │
│                                                                                  │
│  @Loggable("INVENTORY")          ← reused again                                 │
│  public Item addItem() {}                                                        │
│                                                                                  │
│  The LoggingAspect handles ALL of them. Defined ONCE. Reused EVERYWHERE.         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  MAINTAINABILITY — Single Point of Change:                                       │
│                                                                                  │
│  Scenario: "Change log format from plain text to JSON for Splunk integration"    │
│                                                                                  │
│  WITHOUT AOP:                                                                    │
│    → Find all 50 service methods with log statements                             │
│    → Update EACH one from:                                                       │
│        log.info("createOrder called with: {}", req);                             │
│      To:                                                                         │
│        log.info(JsonLog.builder().method("createOrder").args(req).build());       │
│    → 50 methods × 3 log statements each = 150 changes                           │
│    → Risk of missing some, inconsistent formatting                               │
│    → Takes hours, error-prone                                                    │
│                                                                                  │
│  WITH AOP:                                                                       │
│    → Open LoggingAspect.java (ONE file)                                          │
│    → Change:                                                                     │
│        log.info("[{}] >>> {}() called | args: {}", ctx, method, args);            │
│      To:                                                                         │
│        log.info(JsonLog.builder()                                                │
│            .context(ctx).method(method).args(args).build().toJson());             │
│    → 1 file, 2 lines changed                                                    │
│    → ALL 50 methods automatically use new format                                 │
│    → Takes seconds, zero risk of inconsistency                                   │
│                                                                                  │
│  ──────────────────────────────────────────────────────────────────────           │
│                                                                                  │
│  Scenario: "Add execution time threshold alert to all service methods"           │
│                                                                                  │
│  WITHOUT AOP:                                                                    │
│    → Add timer + threshold check to ALL 50 methods                               │
│    → 50 × 5 lines = 250 new lines of scattered code                             │
│                                                                                  │
│  WITH AOP:                                                                       │
│    → Add 3 lines to the existing @Around advice:                                 │
│        if (duration > 1000) {                                                    │
│            alertService.sendSlowMethodAlert(method, duration);                   │
│        }                                                                         │
│    → Done. All methods monitored.                                                │
│                                                                                  │
│  ──────────────────────────────────────────────────────────────────────           │
│                                                                                  │
│  Scenario: "Disable logging in production for performance"                       │
│                                                                                  │
│  WITHOUT AOP:                                                                    │
│    → Can't easily disable without removing code from 50 methods                  │
│    → Or wrap each log in if(enabled) — even MORE boilerplate                    │
│                                                                                  │
│  WITH AOP:                                                                       │
│    → Use @Profile("!prod") on LoggingAspect                                      │
│    → Or use @ConditionalOnProperty("app.logging.aspect.enabled")                 │
│    → ONE annotation = logging disabled for ALL methods                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example — Demonstrating Reusability and Maintainability:**

```java
// Reusable annotation — applies to ANY method in ANY service
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitored {
    String value() default "";           // context label
    long slowThresholdMs() default 500;  // configurable per method
}
```

```java
// ONE Aspect — reused across the ENTIRE application
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "app.monitoring.enabled", havingValue = "true",
                       matchIfMissing = true)  // ← easy to disable!
public class MonitoringAspect {

    @Autowired
    private MeterRegistry meterRegistry;  // Micrometer for Prometheus metrics

    @Around("@annotation(monitored)")
    public Object monitor(ProceedingJoinPoint pjp, Monitored monitored) throws Throwable {
        String context = monitored.value().isEmpty()
            ? pjp.getSignature().getDeclaringType().getSimpleName()
            : monitored.value();
        String method = pjp.getSignature().getName();
        long threshold = monitored.slowThresholdMs();

        // BEFORE
        log.info("[{}] >>> {}()", context, method);
        long start = System.nanoTime();

        try {
            Object result = pjp.proceed();

            // AFTER (success)
            long duration = (System.nanoTime() - start) / 1_000_000;
            log.info("[{}] <<< {}() completed in {}ms", context, method, duration);

            // Record metric
            meterRegistry.timer("method.execution", "context", context, "method", method)
                .record(duration, TimeUnit.MILLISECONDS);

            // Slow method alert
            if (duration > threshold) {
                log.warn("[{}] SLOW: {}() took {}ms (threshold: {}ms)",
                    context, method, duration, threshold);
                meterRegistry.counter("method.slow", "context", context, "method", method)
                    .increment();
            }

            return result;

        } catch (Exception ex) {
            // AFTER (failure)
            long duration = (System.nanoTime() - start) / 1_000_000;
            log.error("[{}] !!! {}() FAILED after {}ms: {}",
                context, method, duration, ex.getMessage());
            meterRegistry.counter("method.errors", "context", context, "method", method)
                .increment();
            throw ex;
        }
    }
}
```

```java
// REUSED across multiple services — just add @Monitored:

@Service
public class OrderService {

    @Monitored("ORDER")                          // ← opt-in to monitoring
    @Transactional
    public Order createOrder(OrderRequest req) {
        // Only business logic
        return orderRepo.save(new Order(req));
    }

    @Monitored(value = "ORDER", slowThresholdMs = 1000)  // ← custom threshold
    public List<Order> searchOrders(String criteria) {
        return orderRepo.searchByCriteria(criteria);
    }
}

@Service
public class PaymentService {

    @Monitored("PAYMENT")                        // ← same Aspect, different service
    @Transactional
    public Payment processPayment(PaymentRequest req) {
        return paymentRepo.save(new Payment(req));
    }

    @Monitored(value = "PAYMENT", slowThresholdMs = 200)  // ← stricter threshold
    public Payment getPayment(Long id) {
        return paymentRepo.findById(id).orElseThrow();
    }
}

@Service
public class InventoryService {

    @Monitored("INVENTORY")                      // ← same Aspect, third service
    @Transactional
    public Item addItem(ItemRequest req) {
        return itemRepo.save(new Item(req));
    }
}
// One @Monitored annotation + One MonitoringAspect = all services monitored!
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Maintainability in Action — Change Scenarios:                                   │
│                                                                                  │
│  ┌──────────────────────────────────┬────────────┬───────────────────────────────┐│
│  │ Change Required                  │ Without AOP│ With AOP                      ││
│  ├──────────────────────────────────┼────────────┼───────────────────────────────┤│
│  │ Change log format to JSON        │ 50 methods │ 1 file (MonitoringAspect)     ││
│  │ Add slow-method alerting         │ 50 methods │ 3 lines in Aspect            ││
│  │ Add Prometheus metrics           │ 50 methods │ 2 lines in Aspect            ││
│  │ Disable monitoring in prod       │ Remove code│ 1 annotation (@Profile)      ││
│  │ Change slow threshold globally   │ 50 methods │ 1 default value              ││
│  │ Add new service to monitoring    │ Copy paste │ Add @Monitored (1 annotation)││
│  │ Remove monitoring from 1 method  │ Delete code│ Remove @Monitored            ││
│  │ Add correlation ID to all logs   │ 50 methods │ 1 line in Aspect (MDC.put)   ││
│  └──────────────────────────────────┴────────────┴───────────────────────────────┘│
│                                                                                  │
│  Maintainability Principles Achieved:                                            │
│                                                                                  │
│    ✓ SINGLE RESPONSIBILITY — Each Aspect handles ONE concern                     │
│    ✓ OPEN/CLOSED — Add monitoring to new methods without modifying Aspect        │
│    ✓ DRY — Monitoring logic defined ONCE, reused everywhere                      │
│    ✓ SEPARATION OF CONCERNS — Business code has zero infrastructure noise        │
│    ✓ EASY TESTING — Test business logic without mocking logging/TX/security      │
│    ✓ EASY ONBOARDING — New developers read clean business methods                │
│    ✓ CONSISTENCY — All methods use same logging format (can't be inconsistent)   │
│    ✓ CONFIGURABILITY — Enable/disable features without code changes              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Reusability + Maintainability — Architecture Diagram:                           │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │                        ASPECTS LAYER (defined ONCE)                        │  │
│  │                                                                            │  │
│  │  ┌──────────────┐  ┌────────────────┐  ┌──────────────┐  ┌────────────┐  │  │
│  │  │ Logging      │  │ Transaction    │  │ Security     │  │ Monitoring │  │  │
│  │  │ Aspect       │  │ Aspect         │  │ Aspect       │  │ Aspect     │  │  │
│  │  │ (custom)     │  │ (Spring built) │  │ (Spring Sec) │  │ (custom)   │  │  │
│  │  └──────┬───────┘  └───────┬────────┘  └──────┬───────┘  └─────┬──────┘  │  │
│  │         │                  │                   │                │          │  │
│  └─────────┼──────────────────┼───────────────────┼────────────────┼──────────┘  │
│            │                  │                   │                │              │
│            └──────────────────┼───────────────────┼────────────────┘              │
│                               │                   │                               │
│                          ┌────┴───────────────────┴────┐                         │
│                          │        PROXY LAYER          │                         │
│                          │  (auto-generated by Spring) │                         │
│                          └────────────┬────────────────┘                         │
│                                       │                                          │
│  ┌────────────────────────────────────┼────────────────────────────────────────┐ │
│  │                    BUSINESS LAYER (pure logic only)                         │ │
│  │                                                                            │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │ │
│  │  │ OrderService │  │PaymentService│  │ UserService  │  │InventorySvc  │  │ │
│  │  │              │  │              │  │              │  │              │  │ │
│  │  │ createOrder()│  │processPaymt()│  │ createUser() │  │ addItem()    │  │ │
│  │  │ cancelOrder()│  │refundPaymt() │  │ updateUser() │  │ removeItem() │  │ │
│  │  │ getOrder()   │  │getPayment()  │  │ deleteUser() │  │ getStock()   │  │ │
│  │  │              │  │              │  │              │  │              │  │ │
│  │  │ ZERO logging │  │ ZERO logging │  │ ZERO logging │  │ ZERO logging │  │ │
│  │  │ ZERO TX mgmt │  │ ZERO TX mgmt │  │ ZERO TX mgmt │  │ ZERO TX mgmt │  │ │
│  │  │ ZERO security│  │ ZERO security│  │ ZERO security│  │ ZERO security│  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘  │ │
│  │                                                                            │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  Benefits:                                                                       │
│    • Add a new service? → Automatically gets logging/TX/security via proxy       │
│    • Change logging format? → Update 1 Aspect → ALL services updated            │
│    • Add new concern (rate limiting)? → Create 1 new Aspect → done              │
│    • Remove a concern? → Delete 1 Aspect → gone from all services               │
│    • Test business logic? → No mocks needed for infrastructure concerns         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


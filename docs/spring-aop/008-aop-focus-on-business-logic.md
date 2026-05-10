### How AOP Allows Focus on Business Logic — Handling Boilerplate

AOP's core value is **separating WHAT the method does (business logic) from HOW it's managed (infrastructure concerns)**. Without AOP, every service method becomes polluted with repetitive boilerplate for logging, transactions, and security — making it hard to read, maintain, and test.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  WITHOUT AOP — Business Logic Buried Under Boilerplate:                          │
│                                                                                  │
│  public Order createOrder(OrderRequest req) {                                    │
│      // ┌─── LOGGING boilerplate (5+ lines) ─────────────────────────────────┐   │
│      // │ log.info("createOrder called with: {}", req);                       │   │
│      // │ long start = System.currentTimeMillis();                            │   │
│      // └─────────────────────────────────────────────────────────────────────┘   │
│      // ┌─── SECURITY boilerplate (3+ lines) ────────────────────────────────┐   │
│      // │ if (!securityCtx.hasRole("ORDER_CREATE")) {                         │   │
│      // │     throw new AccessDeniedException("No permission");               │   │
│      // │ }                                                                   │   │
│      // └─────────────────────────────────────────────────────────────────────┘   │
│      // ┌─── TRANSACTION boilerplate (4+ lines) ─────────────────────────────┐   │
│      // │ TransactionStatus tx = txManager.getTransaction(def);               │   │
│      // └─────────────────────────────────────────────────────────────────────┘   │
│      //                                                                           │
│      // ════════════════════════════════════════════════════════                   │
│         Order order = new Order(req);         // ← ACTUAL BUSINESS LOGIC (2 lines)│
│         orderRepo.save(order);                // ← ACTUAL BUSINESS LOGIC         │
│      // ════════════════════════════════════════════════════════                   │
│      //                                                                           │
│      // ┌─── TRANSACTION boilerplate (5+ lines) ─────────────────────────────┐   │
│      // │ try { tx.commit(); } catch (Exception e) {                          │   │
│      // │     tx.rollback(); throw e;                                         │   │
│      // │ }                                                                   │   │
│      // └─────────────────────────────────────────────────────────────────────┘   │
│      // ┌─── LOGGING boilerplate (3+ lines) ─────────────────────────────────┐   │
│      // │ long duration = System.currentTimeMillis() - start;                 │   │
│      // │ log.info("createOrder completed in {}ms, result: {}", duration, o); │   │
│      // └─────────────────────────────────────────────────────────────────────┘   │
│      return order;                                                               │
│  }                                                                               │
│                                                                                  │
│  RATIO: ~20 lines boilerplate vs ~2 lines business logic = 90% noise!           │
│                                                                                  │
│  And this SAME boilerplate is copy-pasted into EVERY service method:             │
│    createOrder()     → 20 lines boilerplate + 2 lines logic                      │
│    updateOrder()     → 20 lines boilerplate + 3 lines logic                      │
│    deleteOrder()     → 20 lines boilerplate + 1 line logic                       │
│    processPayment()  → 20 lines boilerplate + 5 lines logic                      │
│    transferFunds()   → 20 lines boilerplate + 4 lines logic                      │
│    ... × 50 service methods = 1000 lines of DUPLICATED boilerplate!              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  WITH AOP — 100% Focus on Business Logic:                                        │
│                                                                                  │
│  @Transactional                    ← Transaction handled by AOP                  │
│  @Loggable                         ← Logging handled by AOP                      │
│  @PreAuthorize("hasRole('ADMIN')") ← Security handled by AOP                    │
│  public Order createOrder(OrderRequest req) {                                    │
│      Order order = new Order(req);    // ← ONLY business logic                   │
│      return orderRepo.save(order);    // ← ONLY business logic                   │
│  }                                                                               │
│                                                                                  │
│  RATIO: 2 lines business logic, 0 lines boilerplate = 100% signal!              │
│                                                                                  │
│  All 50 service methods:                                                         │
│    createOrder()     → 2 lines (only logic)                                      │
│    updateOrder()     → 3 lines (only logic)                                      │
│    deleteOrder()     → 1 line (only logic)                                       │
│    processPayment()  → 5 lines (only logic)                                      │
│    transferFunds()   → 4 lines (only logic)                                      │
│                                                                                  │
│  Logging defined ONCE:        LoggingAspect.java (30 lines)                      │
│  Security defined ONCE:       Spring Security AOP (configuration)                │
│  Transactions defined ONCE:   @Transactional + Spring TX AOP (built-in)          │
│                                                                                  │
│  Total boilerplate: 30 lines (ONE Aspect) instead of 1000 lines (scattered)      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Full Code Example — Business Logic Focus:**

```java
// WITHOUT AOP — Service with boilerplate everywhere
@Service
public class PaymentServiceWithoutAOP {

    @Autowired private PaymentRepository paymentRepo;
    @Autowired private PlatformTransactionManager txManager;
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceWithoutAOP.class);

    public Payment processPayment(String orderId, Double amount, String method) {
        // LOGGING — repeated in every method
        log.info("processPayment called | orderId={}, amount={}, method={}",
            orderId, amount, method);
        long start = System.currentTimeMillis();

        // SECURITY — repeated in every method
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PAYMENT"))) {
            throw new AccessDeniedException("No permission to process payment");
        }

        // TRANSACTION — repeated in every method
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus tx = txManager.getTransaction(def);

        Payment payment;
        try {
            // ═══ ACTUAL BUSINESS LOGIC — only 3 lines! ═══
            payment = new Payment(null, orderId, amount, method, "PROCESSED");
            payment = paymentRepo.save(payment);
            // ═══════════════════════════════════════════════

            txManager.commit(tx);  // TRANSACTION
        } catch (Exception e) {
            txManager.rollback(tx);  // TRANSACTION
            log.error("processPayment FAILED: {}", e.getMessage());  // LOGGING
            throw new PaymentException("Payment failed", e);
        }

        // LOGGING — repeated in every method
        long duration = System.currentTimeMillis() - start;
        log.info("processPayment completed in {}ms | result={}", duration, payment);

        return payment;
    }

    public Payment refundPayment(Long paymentId, String reason) {
        // SAME logging boilerplate...
        log.info("refundPayment called | paymentId={}, reason={}", paymentId, reason);
        long start = System.currentTimeMillis();

        // SAME security boilerplate...
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_REFUND"))) {
            throw new AccessDeniedException("No permission to refund");
        }

        // SAME transaction boilerplate...
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus tx = txManager.getTransaction(def);

        Payment payment;
        try {
            // ═══ ACTUAL BUSINESS LOGIC — only 4 lines! ═══
            payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Not found"));
            payment.setStatus("REFUNDED");
            payment = paymentRepo.save(payment);
            // ═══════════════════════════════════════════════

            txManager.commit(tx);
        } catch (Exception e) {
            txManager.rollback(tx);
            log.error("refundPayment FAILED: {}", e.getMessage());
            throw new PaymentException("Refund failed", e);
        }

        log.info("refundPayment completed in {}ms", System.currentTimeMillis() - start);
        return payment;
    }
    // Every method: ~25 lines boilerplate + ~3-4 lines actual logic
    // 10 methods = 250 lines of DUPLICATED infrastructure code!
}
```

```java
// WITH AOP — Same service, ZERO boilerplate
@Service
public class PaymentServiceWithAOP {

    @Autowired
    private PaymentRepository paymentRepo;

    @Transactional                              // ← Transaction handled by Spring AOP
    @PreAuthorize("hasRole('PAYMENT')")         // ← Security handled by Spring Security AOP
    @Loggable("PROCESS-PAYMENT")                // ← Logging handled by custom Aspect
    public Payment processPayment(String orderId, Double amount, String method) {
        // ONLY BUSINESS LOGIC — nothing else!
        Payment payment = new Payment(null, orderId, amount, method, "PROCESSED");
        return paymentRepo.save(payment);
    }

    @Transactional
    @PreAuthorize("hasRole('REFUND')")
    @Loggable("REFUND-PAYMENT")
    public Payment refundPayment(Long paymentId, String reason) {
        // ONLY BUSINESS LOGIC — nothing else!
        Payment payment = paymentRepo.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Not found"));
        payment.setStatus("REFUNDED");
        return paymentRepo.save(payment);
    }
    // Each method: ONLY business logic (3-4 lines)
    // All infrastructure: handled by aspects (defined ONCE)
}
```

```java
// The aspects that handle everything — defined ONCE, applied to ALL services:

// 1. Logging Aspect (replaces scattered log statements)
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("@annotation(loggable)")
    public Object logMethod(ProceedingJoinPoint pjp, Loggable loggable) throws Throwable {
        String ctx = loggable.value();
        String method = pjp.getSignature().getName();
        Object[] args = pjp.getArgs();

        log.info("[{}] >>> {}() called | args: {}", ctx, method, Arrays.toString(args));
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("[{}] <<< {}() returned in {}ms | result: {}",
                ctx, method, duration, result);
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("[{}] !!! {}() FAILED after {}ms | error: {}",
                ctx, method, duration, ex.getMessage());
            throw ex;
        }
    }
}

// 2. Transaction — handled by @Transactional (Spring's built-in AOP)
//    No code needed! Just annotate with @Transactional.
//    Spring AOP internally creates TransactionInterceptor.

// 3. Security — handled by @PreAuthorize (Spring Security's built-in AOP)
//    No code needed! Just annotate with @PreAuthorize.
//    Spring Security AOP internally creates MethodSecurityInterceptor.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Boilerplate Comparison — Lines of Code:                                         │
│                                                                                  │
│                           Without AOP          With AOP                          │
│  ─────────────────────────────────────────────────────────────────                │
│  processPayment()         28 lines             3 lines                           │
│  refundPayment()          26 lines             4 lines                           │
│  cancelPayment()          27 lines             3 lines                           │
│  getPayment()             15 lines             2 lines                           │
│  listPayments()           14 lines             1 line                            │
│  ─────────────────────────────────────────────────────────────────                │
│  Total (service)          110 lines            13 lines                          │
│  LoggingAspect            —                    25 lines (ONE file)               │
│  Security config          —                    10 lines (ONE file)               │
│  Transaction config       —                    0 lines (built-in)                │
│  ─────────────────────────────────────────────────────────────────                │
│  GRAND TOTAL              110 lines            48 lines                          │
│  Reduction                —                    56% less code                     │
│                                                                                  │
│  But the REAL benefit scales with number of services:                            │
│    5 services × 10 methods each = 50 methods                                    │
│    Without AOP: 50 × 25 avg = 1,250 lines of boilerplate                        │
│    With AOP:    50 × 3 avg  + 35 lines (aspects) = 185 lines                    │
│    Reduction: 85% less code!                                                     │
│                                                                                  │
│  Plus: if logging format changes, update 1 file vs 50 methods.                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How Each Cross-Cutting Concern is Handled by AOP:                               │
│                                                                                  │
│  ┌─────────────────────────┬──────────────────────────────────────────────────┐  │
│  │ Concern                 │ How AOP Handles It                               │  │
│  ├─────────────────────────┼──────────────────────────────────────────────────┤  │
│  │ LOGGING                 │ @Around advice logs entry/exit/exception.        │  │
│  │                         │ Pointcut: @annotation(Loggable) or              │  │
│  │                         │ execution(* com.app.service.*.*(..))            │  │
│  │                         │ Developer writes ZERO log statements in service.│  │
│  ├─────────────────────────┼──────────────────────────────────────────────────┤  │
│  │ TRANSACTION MANAGEMENT  │ @Transactional triggers TransactionInterceptor. │  │
│  │                         │ AOP begins TX before method, commits after,     │  │
│  │                         │ rollbacks on RuntimeException.                   │  │
│  │                         │ Developer writes ZERO TX management code.       │  │
│  ├─────────────────────────┼──────────────────────────────────────────────────┤  │
│  │ SECURITY                │ @PreAuthorize / @Secured triggers               │  │
│  │                         │ MethodSecurityInterceptor.                       │  │
│  │                         │ AOP checks permissions BEFORE method runs.      │  │
│  │                         │ Developer writes ZERO auth-check code.          │  │
│  ├─────────────────────────┼──────────────────────────────────────────────────┤  │
│  │ CACHING                 │ @Cacheable triggers CacheInterceptor.           │  │
│  │                         │ AOP checks cache BEFORE method. If hit,         │  │
│  │                         │ returns cached value (method NEVER runs).       │  │
│  │                         │ If miss, runs method, caches result.            │  │
│  │                         │ Developer writes ZERO cache code.               │  │
│  ├─────────────────────────┼──────────────────────────────────────────────────┤  │
│  │ RETRY                   │ @Retryable triggers RetryInterceptor.           │  │
│  │                         │ AOP catches exception, waits, retries method.   │  │
│  │                         │ Developer writes ZERO retry loop code.          │  │
│  └─────────────────────────┴──────────────────────────────────────────────────┘  │
│                                                                                  │
│  Flow with ALL concerns handled by AOP:                                          │
│                                                                                  │
│  Client Request                                                                  │
│       │                                                                          │
│       v                                                                          │
│  ┌─── PROXY ────────────────────────────────────────────────────────┐            │
│  │  1. SecurityInterceptor  → check @PreAuthorize                   │            │
│  │  2. TransactionInterceptor → BEGIN TX                            │            │
│  │  3. CacheInterceptor → check cache (if @Cacheable)              │            │
│  │  4. LoggingAspect → log entry                                    │            │
│  │  5. RetryInterceptor → retry wrapper (if @Retryable)            │            │
│  │                                                                  │            │
│  │        ┌─────────────────────────────────────────────┐           │            │
│  │        │  TARGET METHOD — pure business logic only   │           │            │
│  │        └─────────────────────────────────────────────┘           │            │
│  │                                                                  │            │
│  │  5. RetryInterceptor → retry if failed                           │            │
│  │  4. LoggingAspect → log exit/result                              │            │
│  │  3. CacheInterceptor → store result in cache                     │            │
│  │  2. TransactionInterceptor → COMMIT TX                           │            │
│  │  1. SecurityInterceptor → done                                   │            │
│  └──────────────────────────────────────────────────────────────────┘            │
│       │                                                                          │
│       v                                                                          │
│  Response to Client                                                              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


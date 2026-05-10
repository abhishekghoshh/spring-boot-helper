### How AOP Intercepts Method Invocation — Performing Tasks Before and After

AOP intercepts method invocations through the **proxy pattern**. When Spring detects that a bean matches one or more pointcuts, it wraps that bean in a proxy. All external calls go through the proxy first, giving AOP the ability to execute code **before**, **after**, or **around** the actual method.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Method Interception — Step by Step:                                             │
│                                                                                  │
│  1. At startup, Spring scans all @Aspect classes and their pointcuts             │
│  2. For each bean that matches a pointcut, Spring creates a PROXY object         │
│  3. The proxy is registered in the ApplicationContext (NOT the original bean)    │
│  4. When any other bean calls a method on that proxy:                            │
│     a. Proxy checks: "Does this method match any pointcut?"                      │
│     b. If YES → runs the matching advice(s) in order                             │
│     c. Advice can run code BEFORE the method                                     │
│     d. Advice calls proceed() → actual method runs                               │
│     e. Advice can run code AFTER the method                                      │
│     f. Advice returns result to the caller                                       │
│                                                                                  │
│  Without AOP (direct call):                                                      │
│    Caller ──────────────────────────→ Target.method() ──→ Result                 │
│                                                                                  │
│  With AOP (proxy intercepts):                                                    │
│    Caller ──→ PROXY ──→ Before advice ──→ Target.method() ──→ After advice ──→ R │
│                 ↑                                                                 │
│                 │  The proxy IS the bean in the Spring container.                 │
│                 │  Caller doesn't know it's talking to a proxy.                   │
│                 │  Interception is TRANSPARENT.                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Interception Timeline for a Single Method Call:                                 │
│                                                                                  │
│  Time ──────────────────────────────────────────────────────────────────────→    │
│                                                                                  │
│  ┌─────────┐  ┌────────────────┐  ┌──────────────┐  ┌─────────────────┐         │
│  │ @Before │  │ TARGET METHOD  │  │ @AfterReturn │  │ @After          │         │
│  │ advice  │  │ executes       │  │ (on success) │  │ (always runs)   │         │
│  │         │  │                │  │              │  │                 │         │
│  │ • Log   │  │ • Business     │  │ • Log result │  │ • Cleanup       │         │
│  │   entry │  │   logic runs   │  │ • Audit      │  │ • Close         │         │
│  │ • Check │  │ • DB queries   │  │ • Metrics    │  │   resources     │         │
│  │   auth  │  │ • API calls    │  │ • Cache      │  │                 │         │
│  │ • Start │  │ • Computations │  │   result     │  │                 │         │
│  │   timer │  │                │  │              │  │                 │         │
│  └─────────┘  └────────────────┘  └──────────────┘  └─────────────────┘         │
│                                                                                  │
│  With @Around — FULL CONTROL over the entire lifecycle:                          │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │                        @Around advice                                   │     │
│  │                                                                         │     │
│  │   // BEFORE phase:                                                      │     │
│  │   startTimer();                                                         │     │
│  │   validateInput(args);                                                  │     │
│  │   log.info("Entering method...");                                       │     │
│  │                                                                         │     │
│  │   ┌──────────────────────────────────────────────────────────────┐      │     │
│  │   │   Object result = joinPoint.proceed();  // TARGET EXECUTES   │      │     │
│  │   └──────────────────────────────────────────────────────────────┘      │     │
│  │                                                                         │     │
│  │   // AFTER phase:                                                       │     │
│  │   stopTimer();                                                          │     │
│  │   log.info("Exiting method, result: " + result);                        │     │
│  │   recordMetrics(duration);                                              │     │
│  │                                                                         │     │
│  │   return result;  // or modified result                                 │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Complete Code Example — Method Interception with Before and After Tasks:**

```java
// Tax calculation service — a real-world example where we need to
// perform tasks before (validation, logging) and after (audit, notification)
@Service
public class TaxService {

    @Autowired
    private TaxRepository taxRepository;

    public TaxRecord calculateTax(String taxpayerId, Double income, Integer year) {
        // PURE BUSINESS LOGIC — no logging, no validation, no audit
        Double taxRate = income > 500000 ? 0.30 : income > 250000 ? 0.20 : 0.05;
        Double taxAmount = income * taxRate;

        TaxRecord record = new TaxRecord(null, taxpayerId, income, taxRate, taxAmount, year);
        return taxRepository.save(record);
    }

    public TaxRecord getTaxRecord(Long id) {
        return taxRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tax record not found: " + id));
    }

    public List<TaxRecord> getByTaxpayer(String taxpayerId) {
        return taxRepository.findByTaxpayerId(taxpayerId);
    }
}
```

```java
// Aspect that intercepts and performs tasks BEFORE and AFTER each method
@Aspect
@Component
@Slf4j
public class TaxServiceInterceptor {

    // --- BEFORE: Runs before the method invocation ---
    @Before("execution(* com.app.service.TaxService.*(..))")
    public void beforeMethodInvocation(JoinPoint joinPoint) {
        String method = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // Task 1: Log the method entry with parameters
        log.info("[BEFORE] Invoking: TaxService.{}() | Args: {}", method, Arrays.toString(args));

        // Task 2: Validate input (e.g., taxpayerId should not be null)
        if (args.length > 0 && args[0] == null) {
            throw new IllegalArgumentException("First argument cannot be null for " + method);
        }

        // Task 3: Set MDC context for distributed tracing
        MDC.put("method", method);
        MDC.put("timestamp", Instant.now().toString());
    }

    // --- AFTER RETURNING: Runs after successful method completion ---
    @AfterReturning(pointcut = "execution(* com.app.service.TaxService.*(..))",
                    returning = "result")
    public void afterMethodSuccess(JoinPoint joinPoint, Object result) {
        String method = joinPoint.getSignature().getName();

        // Task 1: Log the successful result
        log.info("[AFTER-SUCCESS] TaxService.{}() returned: {}", method, result);

        // Task 2: Clear MDC context
        MDC.clear();
    }

    // --- AFTER THROWING: Runs after method throws exception ---
    @AfterThrowing(pointcut = "execution(* com.app.service.TaxService.*(..))",
                   throwing = "ex")
    public void afterMethodFailure(JoinPoint joinPoint, Exception ex) {
        String method = joinPoint.getSignature().getName();

        // Task 1: Log the failure
        log.error("[AFTER-FAILURE] TaxService.{}() threw: {}", method, ex.getMessage());

        // Task 2: Increment error metric counter
        // meterRegistry.counter("tax.errors", "method", method).increment();

        // Task 3: Clear MDC context
        MDC.clear();
    }

    // --- AROUND: Full control — before + after + exception handling ---
    @Around("execution(* com.app.service.TaxService.calculateTax(..))")
    public Object aroundCalculateTax(ProceedingJoinPoint joinPoint) throws Throwable {

        String method = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // ═══ BEFORE the method ═══
        log.info("[AROUND-BEFORE] Starting tax calculation for taxpayer: {}", args[0]);
        long startTime = System.nanoTime();

        Object result;
        try {
            // ═══ EXECUTE the actual method ═══
            result = joinPoint.proceed();
            //         ↑ This is where TaxService.calculateTax() actually runs

        } catch (Exception ex) {
            // ═══ ON FAILURE ═══
            long failDuration = (System.nanoTime() - startTime) / 1_000_000;
            log.error("[AROUND-FAILURE] Tax calculation failed after {}ms: {}",
                failDuration, ex.getMessage());
            throw ex;  // re-throw to maintain normal exception propagation
        }

        // ═══ AFTER the method (success path) ═══
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        log.info("[AROUND-AFTER] Tax calculation completed in {}ms | Result: {}",
            duration, result);

        // Task: Flag slow calculations for review
        if (duration > 500) {
            log.warn("[AROUND-SLOW] Tax calculation took {}ms (threshold: 500ms)", duration);
        }

        return result;  // return to caller
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/tax")
public class TaxController {

    @Autowired
    private TaxService taxService;

    @PostMapping("/calculate")
    public TaxRecord calculate(@RequestParam String taxpayerId,
                                @RequestParam Double income,
                                @RequestParam Integer year) {
        return taxService.calculateTax(taxpayerId, income, year);
    }

    @GetMapping("/{id}")
    public TaxRecord get(@PathVariable Long id) {
        return taxService.getTaxRecord(id);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Request: POST /api/tax/calculate?taxpayerId=TX001&income=600000&year=2025       │
│                                                                                  │
│  Console Output (in order):                                                      │
│                                                                                  │
│  [BEFORE] Invoking: TaxService.calculateTax() | Args: [TX001, 600000.0, 2025]   │
│  [AROUND-BEFORE] Starting tax calculation for taxpayer: TX001                    │
│  Hibernate: insert into tax_records (...) values (?, ?, ?, ?, ?, ?)              │
│  [AROUND-AFTER] Tax calculation completed in 38ms |                              │
│                 Result: TaxRecord(id=1, taxpayerId=TX001, income=600000.0,       │
│                         taxRate=0.3, taxAmount=180000.0, year=2025)              │
│  [AFTER-SUCCESS] TaxService.calculateTax() returned: TaxRecord(...)              │
│                                                                                  │
│  Interception Flow:                                                              │
│                                                                                  │
│  Controller.calculate()                                                          │
│       │                                                                          │
│       v                                                                          │
│  ┌─── PROXY (TaxService$$CGLIB) ─────────────────────────────────────────┐       │
│  │                                                                       │       │
│  │  1. @Before → log entry, validate args, set MDC                       │       │
│  │       │                                                               │       │
│  │       v                                                               │       │
│  │  2. @Around (before proceed) → log, start timer                       │       │
│  │       │                                                               │       │
│  │       v                                                               │       │
│  │  3. proceed() → TaxService.calculateTax() EXECUTES                    │       │
│  │       │                                                               │       │
│  │       v                                                               │       │
│  │  4. @Around (after proceed) → stop timer, log duration                │       │
│  │       │                                                               │       │
│  │       v                                                               │       │
│  │  5. @AfterReturning → log result, clear MDC                           │       │
│  │                                                                       │       │
│  └───────────────────────────────────────────────────────────────────────┘       │
│       │                                                                          │
│       v                                                                          │
│  Response returned to client                                                     │
│                                                                                  │
│  KEY INSIGHT:                                                                    │
│    TaxService has ZERO infrastructure code.                                      │
│    It only contains tax calculation logic.                                        │
│    ALL before/after tasks are handled by the interceptor Aspect.                 │
│    The method doesn't even know it's being intercepted.                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What You Can Do BEFORE a Method:              What You Can Do AFTER a Method:   │
│  ─────────────────────────────────              ─────────────────────────────────  │
│  • Log method entry + parameters               • Log method result/exception     │
│  • Validate input arguments                    • Record execution time/metrics   │
│  • Check authorization/permissions             • Clear security/MDC context      │
│  • Start transaction (BEGIN)                   • Commit/rollback transaction     │
│  • Start performance timer                     • Update cache with result        │
│  • Set MDC/trace context                       • Send audit event                │
│  • Check rate limit / throttle                 • Trigger async notifications     │
│  • Acquire distributed lock                    • Release distributed lock        │
│  • Check circuit breaker state                 • Update circuit breaker state    │
│  • Load tenant context                         • Clean up tenant context         │
│  • Check feature flag                          • Record feature usage metrics    │
│  • Reject if null/invalid args                 • Transform/mask sensitive data   │
│                                                                                  │
│  With @Around you can also:                                                      │
│  • SKIP the method entirely (don't call proceed())                               │
│  • RETRY the method (call proceed() multiple times)                              │
│  • MODIFY arguments before passing to method                                     │
│  • MODIFY the result before returning to caller                                  │
│  • REPLACE exceptions (catch one, throw another)                                 │
│  • CACHE results (return cached value without calling method)                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


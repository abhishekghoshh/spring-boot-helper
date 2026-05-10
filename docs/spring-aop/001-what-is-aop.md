### What is AOP? Why and How We Use It?

**AOP (Aspect-Oriented Programming)** is a programming paradigm that allows you to separate **cross-cutting concerns** from your core business logic. A cross-cutting concern is functionality that affects multiple parts of an application but doesn't belong to any single module — examples include logging, security, transaction management, caching, and error handling.


**The Problem AOP Solves:**

Without AOP, cross-cutting concerns get scattered and tangled across your business code:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  WITHOUT AOP — Cross-cutting concerns are scattered:                             │
│                                                                                  │
│  OrderService.java:                                                              │
│    public Order createOrder(OrderRequest req) {                                  │
│        log.info("createOrder called");            // ← LOGGING (scattered)       │
│        SecurityContext.check("CREATE_ORDER");     // ← SECURITY (scattered)      │
│        long start = System.currentTimeMillis();   // ← PERFORMANCE (scattered)   │
│        Transaction tx = txManager.begin();        // ← TRANSACTION (scattered)   │
│        try {                                                                     │
│            Order order = new Order(req);          // ← ACTUAL BUSINESS LOGIC     │
│            orderRepo.save(order);                 // ← ACTUAL BUSINESS LOGIC     │
│            tx.commit();                           // ← TRANSACTION               │
│        } catch (Exception e) {                                                   │
│            tx.rollback();                         // ← TRANSACTION               │
│            log.error("Failed", e);               // ← LOGGING                   │
│            throw e;                                                              │
│        }                                                                         │
│        long end = System.currentTimeMillis();     // ← PERFORMANCE              │
│        log.info("Time: " + (end-start) + "ms");  // ← LOGGING                  │
│        return order;                                                             │
│    }                                                                             │
│                                                                                  │
│  PaymentService.java:                                                            │
│    public Payment processPayment(PaymentReq req) {                               │
│        log.info("processPayment called");         // ← SAME logging pattern     │
│        SecurityContext.check("PROCESS_PAYMENT");  // ← SAME security pattern    │
│        long start = System.currentTimeMillis();   // ← SAME performance pattern │
│        Transaction tx = txManager.begin();        // ← SAME transaction pattern │
│        try {                                                                     │
│            Payment p = gateway.charge(req);       // ← ACTUAL BUSINESS LOGIC    │
│            paymentRepo.save(p);                   // ← ACTUAL BUSINESS LOGIC    │
│            tx.commit();                                                          │
│        } catch (Exception e) {                                                   │
│            tx.rollback();                                                        │
│            log.error("Failed", e);                                              │
│            throw e;                                                              │
│        }                                                                         │
│        long end = System.currentTimeMillis();                                    │
│        log.info("Time: " + (end-start) + "ms");                                 │
│        return p;                                                                 │
│    }                                                                             │
│                                                                                  │
│  PROBLEMS:                                                                       │
│    1. Code Tangling — business logic mixed with logging/security/transactions    │
│    2. Code Scattering — same concern duplicated across 100+ service methods      │
│    3. Hard to maintain — change logging format? Update ALL methods.              │
│    4. Hard to read — actual business logic is buried in boilerplate              │
│    5. SRP violation — each method has multiple responsibilities                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  WITH AOP — Cross-cutting concerns are modularized:                              │
│                                                                                  │
│  OrderService.java:                                                              │
│    @Transactional                                                                │
│    public Order createOrder(OrderRequest req) {                                  │
│        Order order = new Order(req);              // ONLY business logic!         │
│        return orderRepo.save(order);                                             │
│    }                                                                             │
│                                                                                  │
│  PaymentService.java:                                                            │
│    @Transactional                                                                │
│    public Payment processPayment(PaymentReq req) {                               │
│        Payment p = gateway.charge(req);           // ONLY business logic!         │
│        return paymentRepo.save(p);                                               │
│    }                                                                             │
│                                                                                  │
│  Logging, security, performance monitoring — defined ONCE in Aspects:            │
│    LoggingAspect.java → handles ALL logging                                      │
│    SecurityAspect.java → handles ALL security checks                             │
│    PerformanceAspect.java → handles ALL timing                                   │
│    @Transactional → Spring AOP handles ALL transactions                          │
│                                                                                  │
│  RESULT:                                                                         │
│    1. Clean business logic — no boilerplate                                      │
│    2. DRY — each concern defined ONCE                                            │
│    3. Easy to maintain — change logging? Update ONE Aspect.                      │
│    4. Easy to read — method does what its name says                              │
│    5. SRP respected — each class has one responsibility                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Definition of Key AOP Terms:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  AOP Terminology:                                                                │
│                                                                                  │
│  ┌────────────────┬──────────────────────────────────────────────────────────────┐│
│  │ Term           │ Definition                                                   ││
│  ├────────────────┼──────────────────────────────────────────────────────────────┤│
│  │ Aspect         │ A module that encapsulates a cross-cutting concern.          ││
│  │                │ A class annotated with @Aspect.                              ││
│  │                │ Example: LoggingAspect, SecurityAspect                       ││
│  ├────────────────┼──────────────────────────────────────────────────────────────┤│
│  │ Advice         │ The ACTION taken by an Aspect at a particular point.         ││
│  │                │ The actual code that runs (the method inside the Aspect).    ││
│  │                │ Types: @Before, @After, @AfterReturning, @AfterThrowing,     ││
│  │                │        @Around                                               ││
│  ├────────────────┼──────────────────────────────────────────────────────────────┤│
│  │ Join Point     │ A point during execution where an Aspect CAN be applied.     ││
│  │                │ In Spring AOP: always a METHOD EXECUTION.                    ││
│  │                │ Example: when createOrder() is called                        ││
│  ├────────────────┼──────────────────────────────────────────────────────────────┤│
│  │ Pointcut       │ A PREDICATE that matches Join Points.                        ││
│  │                │ Defines WHERE the advice should apply.                       ││
│  │                │ Example: "all methods in com.app.service.*"                  ││
│  ├────────────────┼──────────────────────────────────────────────────────────────┤│
│  │ Target Object  │ The object being advised (the original bean).                ││
│  │                │ Example: the actual OrderService instance                    ││
│  ├────────────────┼──────────────────────────────────────────────────────────────┤│
│  │ Proxy          │ The object created by AOP framework that wraps the target.   ││
│  │                │ Spring creates a proxy around the target to intercept calls. ││
│  ├────────────────┼──────────────────────────────────────────────────────────────┤│
│  │ Weaving        │ The process of linking Aspects with target objects.           ││
│  │                │ Spring does RUNTIME weaving (using proxies).                 ││
│  │                │ AspectJ supports compile-time and load-time weaving.         ││
│  └────────────────┴──────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How AOP Terms Map Together:                                                     │
│                                                                                  │
│  @Aspect                    ← ASPECT (the module)                                │
│  @Component                                                                      │
│  public class LoggingAspect {                                                    │
│                                                                                  │
│      @Around("execution(* com.app.service.*.*(..))")                             │
│      │        └──────────────────────────────────┘                               │
│      │                    ↑ POINTCUT (where to apply)                            │
│      │                                                                           │
│      public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable {   │
│      │                       └──────────────────┘                                │
│      │                              ↑ JOIN POINT (the intercepted method call)   │
│      │                                                                           │
│      │   // ADVICE — the action taken:                                           │
│      │   log.info("Calling: " + joinPoint.getSignature());                       │
│      │   Object result = joinPoint.proceed(); // call the TARGET method          │
│      │   log.info("Returned: " + result);                                        │
│      │   return result;                                                          │
│      }                                                                           │
│  }                                                                               │
│                                                                                  │
│  Execution Flow:                                                                 │
│                                                                                  │
│  Client calls orderService.createOrder(req)                                      │
│       │                                                                          │
│       v                                                                          │
│  ┌─────────────────┐                                                             │
│  │  PROXY           │ ← Spring-generated proxy wraps OrderService                │
│  │  (AOP intercept) │                                                            │
│  │                  │                                                             │
│  │  Checks: Does    │                                                            │
│  │  createOrder()   │                                                            │
│  │  match pointcut? │                                                            │
│  │  YES → run advice│                                                            │
│  └────────┬─────────┘                                                            │
│           │                                                                      │
│           v                                                                      │
│  ┌─────────────────┐                                                             │
│  │  LoggingAspect   │ ← @Before advice executes                                 │
│  │  "Calling:       │                                                            │
│  │   createOrder"   │                                                            │
│  └────────┬─────────┘                                                            │
│           │ proceed()                                                            │
│           v                                                                      │
│  ┌─────────────────┐                                                             │
│  │  TARGET OBJECT   │ ← Actual OrderService.createOrder() runs                   │
│  │  OrderService    │                                                            │
│  │  .createOrder()  │                                                            │
│  └────────┬─────────┘                                                            │
│           │ returns result                                                       │
│           v                                                                      │
│  ┌─────────────────┐                                                             │
│  │  LoggingAspect   │ ← @After advice executes                                  │
│  │  "Returned:      │                                                            │
│  │   Order#123"     │                                                            │
│  └────────┬─────────┘                                                            │
│           │                                                                      │
│           v                                                                      │
│  Result returned to Client                                                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**How Spring AOP Works Internally — Proxy Mechanism:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring AOP Proxy Creation (at startup):                                         │
│                                                                                  │
│  1. Spring scans beans and finds @Aspect classes                                 │
│  2. For each bean that matches a pointcut, Spring creates a PROXY:               │
│                                                                                  │
│      ┌─────────────────────────────────────────────────────────────────┐         │
│      │  Bean: orderService                                            │         │
│      │                                                                │         │
│      │  Without AOP:                                                  │         │
│      │    orderService → OrderService (actual instance)               │         │
│      │                                                                │         │
│      │  With AOP:                                                     │         │
│      │    orderService → PROXY → OrderService (actual instance)       │         │
│      │                   ↑                                            │         │
│      │                   Intercepts method calls                      │         │
│      │                   Runs advice before/after                     │         │
│      │                   Then delegates to actual instance             │         │
│      └─────────────────────────────────────────────────────────────────┘         │
│                                                                                  │
│  Two Proxy Types:                                                                │
│                                                                                  │
│  ┌─────────────────────────────────┬─────────────────────────────────────────┐   │
│  │ JDK Dynamic Proxy               │ CGLIB Proxy                            │   │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤   │
│  │ Used when bean implements       │ Used when bean does NOT implement      │   │
│  │ an interface                    │ any interface                           │   │
│  │                                 │                                         │   │
│  │ Proxy implements the same       │ Proxy is a SUBCLASS of the target      │   │
│  │ interface                       │ class                                   │   │
│  │                                 │                                         │   │
│  │ Uses java.lang.reflect.Proxy    │ Uses CGLIB byte code generation        │   │
│  │                                 │                                         │   │
│  │ Can only intercept interface    │ Can intercept any non-final method     │   │
│  │ methods                         │                                         │   │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤   │
│  │ Spring Boot default: CGLIB      │ (proxyTargetClass=true by default)     │   │
│  │ since Spring Boot 2.0           │                                         │   │
│  └─────────────────────────────────┴─────────────────────────────────────────┘   │
│                                                                                  │
│  CGLIB Proxy Diagram:                                                            │
│                                                                                  │
│  @Autowired                                                                      │
│  OrderService orderService;  ← This is actually a CGLIB proxy!                   │
│                                                                                  │
│  orderService.getClass() → OrderService$$EnhancerBySpringCGLIB$$a1b2c3d4         │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────┐           │
│  │ OrderService$$EnhancerBySpringCGLIB$$a1b2c3d4  (PROXY)            │           │
│  │                                                                   │           │
│  │  createOrder(req) {                                               │           │
│  │      // 1. Check if method matches any pointcut                   │           │
│  │      // 2. If yes → run advice chain                              │           │
│  │      // 3. Call super.createOrder(req) — the real method           │           │
│  │      // 4. Run after advice                                       │           │
│  │      // 5. Return result                                          │           │
│  │  }                                                                │           │
│  │                                                                   │           │
│  │  ┌───────────────────────────────────────────────────────────┐    │           │
│  │  │ OrderService  (TARGET — actual instance)                  │    │           │
│  │  │   createOrder(req) { /* business logic */ }               │    │           │
│  │  └───────────────────────────────────────────────────────────┘    │           │
│  └───────────────────────────────────────────────────────────────────┘           │
│                                                                                  │
│  IMPORTANT LIMITATION:                                                           │
│    Self-invocation (calling this.method()) BYPASSES the proxy!                   │
│    Because "this" refers to the target, not the proxy.                           │
│                                                                                  │
│    public class OrderService {                                                   │
│        @Transactional                                                            │
│        public void createOrder() { ... }                                         │
│                                                                                  │
│        public void batchCreate() {                                               │
│            this.createOrder();  // ← BYPASSES proxy! @Transactional IGNORED!     │
│        }                                                                         │
│    }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Advice Types in Spring AOP:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Five Types of Advice:                                                           │
│                                                                                  │
│  ┌──────────────────┬──────────────────────────────────────────────────────────┐ │
│  │ Advice Type      │ When It Runs                                             │ │
│  ├──────────────────┼──────────────────────────────────────────────────────────┤ │
│  │ @Before          │ Before the target method executes                        │ │
│  │ @After           │ After the target method (regardless of outcome)          │ │
│  │ @AfterReturning  │ After the target method returns successfully             │ │
│  │ @AfterThrowing   │ After the target method throws an exception              │ │
│  │ @Around          │ Wraps the target method (before + after + control)       │ │
│  └──────────────────┴──────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  Execution Order:                                                                │
│                                                                                  │
│       @Around (before proceed)                                                   │
│           │                                                                      │
│           v                                                                      │
│       @Before                                                                    │
│           │                                                                      │
│           v                                                                      │
│       TARGET METHOD EXECUTES                                                     │
│           │                                                                      │
│           ├── Success ──────────────── Failure ──┐                               │
│           │                                      │                               │
│           v                                      v                               │
│       @AfterReturning                       @AfterThrowing                       │
│           │                                      │                               │
│           v                                      v                               │
│       @After (always)                       @After (always)                      │
│           │                                      │                               │
│           v                                      v                               │
│       @Around (after proceed)               @Around (catch block)                │
│           │                                      │                               │
│           v                                      v                               │
│       Return to caller                      Exception propagates                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Pointcut Expressions — Where to Apply Advice:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut Expression Syntax:                                                     │
│                                                                                  │
│  execution(modifiers? return-type declaring-type? method-name(params) throws?)   │
│                                                                                  │
│  Examples:                                                                       │
│  ┌──────────────────────────────────────────────────────────┬────────────────────┐│
│  │ Expression                                               │ Matches            ││
│  ├──────────────────────────────────────────────────────────┼────────────────────┤│
│  │ execution(* com.app.service.*.*(..))                     │ Any method in any  ││
│  │                                                          │ class in service   ││
│  │                                                          │ package            ││
│  ├──────────────────────────────────────────────────────────┼────────────────────┤│
│  │ execution(* com.app.service..*.*(..))                    │ service package    ││
│  │                                                          │ AND sub-packages   ││
│  ├──────────────────────────────────────────────────────────┼────────────────────┤│
│  │ execution(public * *(..))                                │ Any public method  ││
│  ├──────────────────────────────────────────────────────────┼────────────────────┤│
│  │ execution(* com.app.service.OrderService.create*(..))    │ Methods starting   ││
│  │                                                          │ with "create" in   ││
│  │                                                          │ OrderService       ││
│  ├──────────────────────────────────────────────────────────┼────────────────────┤│
│  │ execution(* *(String, ..))                               │ Methods with first ││
│  │                                                          │ param as String    ││
│  ├──────────────────────────────────────────────────────────┼────────────────────┤│
│  │ @annotation(com.app.annotation.Loggable)                 │ Methods annotated  ││
│  │                                                          │ with @Loggable     ││
│  ├──────────────────────────────────────────────────────────┼────────────────────┤│
│  │ within(com.app.service.*)                                │ Any method within  ││
│  │                                                          │ service classes    ││
│  ├──────────────────────────────────────────────────────────┼────────────────────┤│
│  │ @within(org.springframework.stereotype.Service)          │ Any method in      ││
│  │                                                          │ @Service classes   ││
│  └──────────────────────────────────────────────────────────┴────────────────────┘│
│                                                                                  │
│  Combining Pointcuts:                                                            │
│    && (AND): execution(* com.app.service.*.*(..)) && @annotation(Loggable)       │
│    || (OR):  execution(* *..OrderService.*(..)) || execution(* *..PaymentSvc.*(..│
│              ))                                                                   │
│    !  (NOT): execution(* com.app.service.*.*(..)) && !execution(* *..get*(..))   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


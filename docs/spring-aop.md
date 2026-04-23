# Spring AOP


## Medium

- [Easy Spring AOP Implementation: Simplify Our Code with Aspect-Oriented Programming](https://aeontanvir.medium.com/easy-spring-aop-implementation-simplify-our-code-with-aspect-oriented-programming-1b8f6f7bfe82)



## Youtube

- [How Spring @Transactional Works Internally ? ( AOP, Proxies & Debug Walkthrough) @Javatechie](https://www.youtube.com/watch?v=eWl8G7NDKqo)
- [Mastering @Aspect Annotation & Spring AOP | Aspect-Oriented Programming in Spring](https://www.youtube.com/watch?v=EobKYha3nR0)
- [AOP Crash Course in Spring Boot | Master Aspect-Oriented Programming in 1 Hours](https://www.youtube.com/watch?v=GAH34kyy8zw)
- [Spring boot AOP (Aspect Oriented Programming)](https://www.youtube.com/watch?v=HhsAw8GVogQ)




## Theory

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

### Advantages and Disadvantages of AOP

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ADVANTAGES:                                                                     │
│                                                                                  │
│  ┌──────────────────────────────────┬────────────────────────────────────────────┐│
│  │ Advantage                        │ Explanation                                ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Separation of Concerns           │ Cross-cutting logic separated from         ││
│  │                                  │ business logic. Each class has ONE job.    ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ DRY (Don't Repeat Yourself)      │ Logging/security/transaction code defined ││
│  │                                  │ ONCE, applied to hundreds of methods.     ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Single Point of Change           │ Change logging format in ONE Aspect →      ││
│  │                                  │ automatically applied everywhere.          ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Non-Invasive                     │ Target classes don't know they're being    ││
│  │                                  │ advised. No code changes needed in them.   ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Improved Readability             │ Business methods only contain business     ││
│  │                                  │ logic. No noise from infrastructure code.  ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Easy to Enable/Disable           │ Remove @Aspect → concern disappears.      ││
│  │                                  │ No scattered code to hunt down.            ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Testability                      │ Business logic testable without            ││
│  │                                  │ cross-cutting concerns. Aspects testable  ││
│  │                                  │ independently.                            ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Declarative Programming          │ @Transactional, @Cacheable, @Secured —     ││
│  │                                  │ declare WHAT, not HOW.                     ││
│  └──────────────────────────────────┴────────────────────────────────────────────┘│
│                                                                                  │
│  DISADVANTAGES:                                                                  │
│                                                                                  │
│  ┌──────────────────────────────────┬────────────────────────────────────────────┐│
│  │ Disadvantage                     │ Explanation                                ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Debugging Complexity             │ Stack traces go through proxy layers.      ││
│  │                                  │ Harder to trace execution flow.            ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Hidden Behavior                  │ Code executes that's not visible in the    ││
│  │                                  │ method. New developers may be confused.    ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Performance Overhead             │ Proxy invocation adds slight overhead      ││
│  │                                  │ per method call (usually negligible).      ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Self-Invocation Limitation       │ this.method() bypasses proxy → advice      ││
│  │                                  │ NOT applied on internal calls.             ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Only Method-Level (Spring AOP)   │ Cannot intercept field access,             ││
│  │                                  │ constructor calls, or static methods.      ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Learning Curve                   │ Pointcut expressions, advice ordering,     ││
│  │                                  │ proxy mechanics need understanding.        ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Over-Engineering Risk            │ Developers may apply AOP for concerns     ││
│  │                                  │ that don't cross-cut (misuse).            ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ Pointcut Fragility               │ Renaming a package/method can break       ││
│  │                                  │ pointcut expressions silently.            ││
│  └──────────────────────────────────┴────────────────────────────────────────────┘│
│                                                                                  │
│  Spring AOP vs AspectJ:                                                          │
│                                                                                  │
│  ┌─────────────────────────────────┬───────────────────┬─────────────────────────┐│
│  │ Feature                         │ Spring AOP        │ AspectJ                 ││
│  ├─────────────────────────────────┼───────────────────┼─────────────────────────┤│
│  │ Weaving                         │ Runtime (proxy)   │ Compile-time/Load-time  ││
│  │ Join Points                     │ Method execution  │ Method, field, constr.  ││
│  │ Performance                     │ Slight overhead   │ No runtime overhead     ││
│  │ Self-invocation                 │ NOT intercepted   │ Intercepted             ││
│  │ Setup Complexity                │ Simple (Spring)   │ Requires AspectJ comp.  ││
│  │ Sufficient for most apps?       │ YES               │ Rarely needed           ││
│  └─────────────────────────────────┴───────────────────┴─────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Real-Life Examples and Industry Usages

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Real-World AOP Usages in Spring Boot Applications:                              │
│                                                                                  │
│  ┌──────────────────────────────────┬────────────────────────────────────────────┐│
│  │ Use Case                         │ How AOP Helps                              ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 1. Logging & Auditing            │ Log method entry/exit, parameters,         ││
│  │                                  │ execution time for all service methods.   ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 2. Transaction Management        │ @Transactional — Spring AOP creates proxy ││
│  │                                  │ that begins/commits/rollbacks TX.         ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 3. Security                      │ @Secured, @PreAuthorize — Spring Security ││
│  │                                  │ uses AOP to check authorization.          ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 4. Caching                       │ @Cacheable — AOP intercepts, checks cache ││
│  │                                  │ before calling method, stores result.     ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 5. Exception Handling            │ Global exception translation (e.g.,        ││
│  │                                  │ convert DataAccessException to custom).   ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 6. Performance Monitoring        │ Measure execution time of methods,         ││
│  │                                  │ send metrics to Prometheus/Grafana.       ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 7. Retry Logic                   │ @Retryable — Spring Retry uses AOP to     ││
│  │                                  │ automatically retry failed operations.    ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 8. Rate Limiting                 │ Custom @RateLimit annotation — AOP checks ││
│  │                                  │ request count before allowing execution.  ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 9. Input Validation              │ Validate method parameters before          ││
│  │                                  │ execution (beyond @Valid).                ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 10. Async Execution              │ @Async — Spring AOP wraps method call in  ││
│  │                                  │ a separate thread.                        ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 11. Distributed Tracing          │ Add correlation IDs / trace IDs to all    ││
│  │                                  │ service calls for observability.          ││
│  ├──────────────────────────────────┼────────────────────────────────────────────┤│
│  │ 12. Feature Flags                │ Custom @FeatureFlag annotation — AOP      ││
│  │                                  │ checks if feature is enabled before exec. ││
│  └──────────────────────────────────┴────────────────────────────────────────────┘│
│                                                                                  │
│  Spring Framework Itself Uses AOP Extensively:                                   │
│    @Transactional  → TransactionInterceptor (AOP advice)                         │
│    @Cacheable      → CacheInterceptor (AOP advice)                               │
│    @Async          → AsyncExecutionInterceptor (AOP advice)                      │
│    @Secured        → MethodSecurityInterceptor (AOP advice)                      │
│    @Retryable      → RetryOperationsInterceptor (AOP advice)                     │
│    @Validated      → MethodValidationInterceptor (AOP advice)                    │
│                                                                                  │
│  Industry Examples:                                                              │
│    - E-commerce: Log every order/payment API call for audit compliance           │
│    - Banking: Security check before every fund transfer method                   │
│    - Healthcare: Encrypt/decrypt PHI data on method boundaries                   │
│    - Microservices: Add traceId/spanId to every service-to-service call          │
│    - SaaS: Tenant isolation — validate tenant context before DB access           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### All Spring AOP Annotations Explained

#### 1. `@Aspect`

**Meaning:** Marks a class as an **Aspect** — a module that contains cross-cutting concern logic (advice methods + pointcut definitions).

**From:** `org.aspectj.lang.annotation.Aspect`

**Important:** `@Aspect` alone does NOT make the class a Spring bean. You must also add `@Component` (or another stereotype) so Spring can detect and manage it.

```java
@Aspect       // ← "This class contains AOP advice"
@Component    // ← "Register this class as a Spring bean"
public class LoggingAspect {
    // advice methods go here
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Aspect — What it does:                                                         │
│                                                                                  │
│  Without @Aspect:                                                                │
│    @Component                                                                    │
│    public class LoggingAspect {                                                  │
│        @Around("execution(* com.app.service.*.*(..))")                           │
│        public Object log(ProceedingJoinPoint pjp) { ... }                        │
│    }                                                                             │
│    → Spring treats this as a normal bean. @Around is IGNORED.                    │
│    → No AOP interception happens.                                                │
│                                                                                  │
│  With @Aspect:                                                                   │
│    @Aspect                                                                       │
│    @Component                                                                    │
│    public class LoggingAspect {                                                  │
│        @Around("execution(* com.app.service.*.*(..))")                           │
│        public Object log(ProceedingJoinPoint pjp) { ... }                        │
│    }                                                                             │
│    → Spring recognizes this as an AOP Aspect.                                    │
│    → Scans @Around/@Before/@After methods for pointcuts.                         │
│    → Creates proxies for matching beans.                                         │
│    → Intercepts method calls and runs advice.                                    │
│                                                                                  │
│  Common mistake:                                                                 │
│    @Aspect                                                                       │
│    public class LoggingAspect { ... }  // ← Missing @Component!                  │
│    → Spring never finds this class → no AOP happens.                             │
│    → No error thrown — it silently doesn't work.                                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 2. `@Before`

**Meaning:** Runs the advice method **before** the target method executes.

**From:** `org.aspectj.lang.annotation.Before`

**Use cases:** Logging method entry, validating inputs, checking permissions, setting context (MDC/tenant).

**Parameters:**
- `value` — the pointcut expression (required)

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Before("execution(* com.app.service.*.*(..))")
    //       └────────────────────────────────────┘
    //                pointcut expression
    public void logBefore(JoinPoint joinPoint) {
        //                 ↑ JoinPoint gives access to method info
        String method = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info(">>> Calling: {}() with args: {}", method, Arrays.toString(args));
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Before — Execution Flow:                                                       │
│                                                                                  │
│  Client calls orderService.createOrder("Laptop", 999.99)                         │
│       │                                                                          │
│       v                                                                          │
│  ┌──────────────────────────────────────────────────┐                            │
│  │  @Before advice runs:                             │                            │
│  │    log.info(">>> Calling: createOrder()           │                            │
│  │              with args: [Laptop, 999.99]")        │                            │
│  └──────────────────┬───────────────────────────────┘                            │
│                     │                                                            │
│                     v                                                            │
│  ┌──────────────────────────────────────────────────┐                            │
│  │  TARGET: OrderService.createOrder() executes      │                            │
│  └──────────────────┬───────────────────────────────┘                            │
│                     │                                                            │
│                     v                                                            │
│  Result returned to client                                                       │
│                                                                                  │
│  KEY POINTS:                                                                     │
│    • @Before CANNOT prevent the target from running (unless it throws exception) │
│    • @Before CANNOT access the return value                                      │
│    • @Before receives JoinPoint (not ProceedingJoinPoint)                        │
│    • If @Before throws an exception, the target method DOES NOT execute          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// @Before with JoinPoint — available information:
@Before("execution(* com.app.service.*.*(..))")
public void inspectJoinPoint(JoinPoint joinPoint) {

    // Method name: "createOrder"
    String methodName = joinPoint.getSignature().getName();

    // Full signature: "public Order com.app.service.OrderService.createOrder(String, Double)"
    String fullSignature = joinPoint.getSignature().toLongString();

    // Short signature: "OrderService.createOrder(..)"
    String shortSignature = joinPoint.getSignature().toShortString();

    // Arguments: [Laptop, 999.99]
    Object[] args = joinPoint.getArgs();

    // Target class: OrderService
    String targetClass = joinPoint.getTarget().getClass().getSimpleName();

    // Proxy class: OrderService$$EnhancerBySpringCGLIB$$abc123
    String proxyClass = joinPoint.getThis().getClass().getSimpleName();
}
```

---

#### 3. `@After`

**Meaning:** Runs the advice method **after** the target method finishes — whether it returned successfully OR threw an exception. Similar to a `finally` block.

**From:** `org.aspectj.lang.annotation.After`

**Use cases:** Cleanup operations, releasing resources, clearing context (MDC), always-run logging.

```java
@Aspect
@Component
@Slf4j
public class CleanupAspect {

    @After("execution(* com.app.service.*.*(..))")
    public void cleanupAfter(JoinPoint joinPoint) {
        log.info("<<< Finished: {}() — cleaning up", joinPoint.getSignature().getName());
        MDC.clear();  // always clear MDC, even if method threw exception
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @After — Execution Flow:                                                        │
│                                                                                  │
│  Success path:                            Failure path:                          │
│  ──────────────                           ──────────────                          │
│  TARGET executes → returns result         TARGET executes → throws exception     │
│       │                                        │                                 │
│       v                                        v                                 │
│  @After runs  ✓                           @After runs  ✓  (STILL runs!)         │
│       │                                        │                                 │
│       v                                        v                                 │
│  Result → Client                          Exception → Client                     │
│                                                                                  │
│  KEY POINTS:                                                                     │
│    • @After ALWAYS runs (like finally block)                                     │
│    • @After CANNOT access the return value                                       │
│    • @After CANNOT access the thrown exception                                   │
│    • Use @AfterReturning if you need the result                                  │
│    • Use @AfterThrowing if you need the exception                                │
│    • Use @After only for cleanup that must happen regardless of outcome          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 4. `@AfterReturning`

**Meaning:** Runs the advice method **after** the target method returns **successfully** (no exception). Gives access to the return value.

**From:** `org.aspectj.lang.annotation.AfterReturning`

**Use cases:** Logging results, auditing successful operations, caching results, sending notifications on success.

**Parameters:**
- `pointcut` or `value` — the pointcut expression
- `returning` — the name of the parameter that binds the return value

```java
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @AfterReturning(
        pointcut = "execution(* com.app.service.OrderService.createOrder(..))",
        returning = "result"   // ← binds return value to "result" parameter
    )
    public void auditOrderCreation(JoinPoint joinPoint, Object result) {
        //                                                 ↑ matches "returning" name
        log.info("Order created successfully: {}", result);
        // result is the Order object returned by createOrder()
    }

    // You can also type the return value for filtering:
    @AfterReturning(
        pointcut = "execution(* com.app.service.*.*(..))",
        returning = "order"
    )
    public void onlyForOrderReturns(JoinPoint joinPoint, Order order) {
        // This advice ONLY runs for methods that return an Order type.
        // If a method returns String, List, Payment, etc. → this advice is SKIPPED.
        log.info("An Order was returned: {}", order.getId());
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @AfterReturning — Execution Flow:                                               │
│                                                                                  │
│  Success path:                            Failure path:                          │
│  ──────────────                           ──────────────                          │
│  TARGET executes → returns result         TARGET executes → throws exception     │
│       │                                        │                                 │
│       v                                        v                                 │
│  @AfterReturning runs  ✓                  @AfterReturning DOES NOT run  ✗       │
│    (has access to result)                                                        │
│       │                                        │                                 │
│       v                                        v                                 │
│  Result → Client                          Exception → Client                     │
│                                                                                  │
│  KEY POINTS:                                                                     │
│    • Only runs on SUCCESS (no exception)                                         │
│    • Has access to return value via "returning" parameter                        │
│    • Can read the result but CANNOT change it (for that, use @Around)            │
│    • Type-narrowing: if param type is Order, only runs when Order is returned    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5. `@AfterThrowing`

**Meaning:** Runs the advice method **after** the target method throws an **exception**. Gives access to the thrown exception.

**From:** `org.aspectj.lang.annotation.AfterThrowing`

**Use cases:** Logging errors, sending alerts, incrementing error metrics, translating exceptions.

**Parameters:**
- `pointcut` or `value` — the pointcut expression
- `throwing` — the name of the parameter that binds the thrown exception

```java
@Aspect
@Component
@Slf4j
public class ErrorHandlingAspect {

    @AfterThrowing(
        pointcut = "execution(* com.app.service.*.*(..))",
        throwing = "ex"   // ← binds thrown exception to "ex" parameter
    )
    public void logError(JoinPoint joinPoint, Exception ex) {
        //                                      ↑ matches "throwing" name
        log.error("!!! Error in {}(): {} | Args: {}",
            joinPoint.getSignature().getName(),
            ex.getMessage(),
            Arrays.toString(joinPoint.getArgs()));
    }

    // Type-narrowing: only catch specific exception types
    @AfterThrowing(
        pointcut = "execution(* com.app.service.*.*(..))",
        throwing = "ex"
    )
    public void onlyDataAccessErrors(JoinPoint joinPoint, DataAccessException ex) {
        // This ONLY runs when a DataAccessException (or subclass) is thrown.
        // RuntimeException, NullPointerException, etc. → SKIPPED.
        log.error("DATABASE ERROR in {}(): {}", joinPoint.getSignature().getName(), ex);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @AfterThrowing — Execution Flow:                                                │
│                                                                                  │
│  Success path:                            Failure path:                          │
│  ──────────────                           ──────────────                          │
│  TARGET executes → returns result         TARGET executes → throws exception     │
│       │                                        │                                 │
│       v                                        v                                 │
│  @AfterThrowing DOES NOT run  ✗           @AfterThrowing runs  ✓                │
│                                             (has access to exception)            │
│       │                                        │                                 │
│       v                                        v                                 │
│  Result → Client                          Exception → Client (still propagates!) │
│                                                                                  │
│  KEY POINTS:                                                                     │
│    • Only runs on FAILURE (exception thrown)                                      │
│    • Has access to thrown exception via "throwing" parameter                      │
│    • DOES NOT swallow the exception — it still propagates to the caller          │
│    • To swallow/replace an exception, use @Around with try-catch                 │
│    • Type-narrowing: DataAccessException param → only runs for DB exceptions     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 6. `@Around`

**Meaning:** Wraps the target method **completely**. The advice controls whether the target executes, can modify arguments, modify the result, handle exceptions, measure time, and retry.

**From:** `org.aspectj.lang.annotation.Around`

**Use cases:** Performance monitoring, caching, retry logic, transaction management, comprehensive logging, any scenario requiring full control.

**Key:** Must use `ProceedingJoinPoint` (not `JoinPoint`) and call `proceed()` to execute the target method.

```java
@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    @Around("execution(* com.app.service.*.*(..))")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        //                     └──────────────────┘
        //                     ProceedingJoinPoint — has proceed() method
        //                     (JoinPoint does NOT have proceed())

        String method = joinPoint.getSignature().toShortString();

        // ═══ BEFORE the target method ═══
        long start = System.currentTimeMillis();
        log.info(">>> Starting: {}", method);

        Object result;
        try {
            // ═══ EXECUTE the target method ═══
            result = joinPoint.proceed();
            //  ↑ Without this call, the target method NEVER runs!
            //  ↑ This is what separates @Around from @Before

        } catch (Exception ex) {
            // ═══ ON EXCEPTION ═══
            log.error("!!! {} failed: {}", method, ex.getMessage());
            throw ex;  // re-throw — or throw a different exception
        }

        // ═══ AFTER the target method (success path) ═══
        long duration = System.currentTimeMillis() - start;
        log.info("<<< {} completed in {}ms", method, duration);

        return result;  // ← MUST return result to caller
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Around — Full Control Capabilities:                                            │
│                                                                                  │
│  ┌────────────────────────────┬──────────────────────────────────────────────────┐│
│  │ Capability                 │ How                                              ││
│  ├────────────────────────────┼──────────────────────────────────────────────────┤│
│  │ Run code before method     │ Write code before proceed()                      ││
│  │ Run code after method      │ Write code after proceed()                       ││
│  │ Run code on exception      │ Wrap proceed() in try-catch                      ││
│  │ Skip method execution      │ Don't call proceed() — return a value directly   ││
│  │ Retry on failure           │ Call proceed() in a loop                         ││
│  │ Modify input arguments     │ joinPoint.proceed(newArgs)                       ││
│  │ Modify return value        │ Change result before returning it                ││
│  │ Replace exception          │ Catch one exception, throw a different one       ││
│  │ Measure execution time     │ Record time before and after proceed()           ││
│  │ Caching                    │ Check cache → if hit, return cached (skip method)││
│  │                            │ If miss, proceed(), store result in cache        ││
│  └────────────────────────────┴──────────────────────────────────────────────────┘│
│                                                                                  │
│  @Around execution flow:                                                         │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │                        @Around advice method                            │     │
│  │                                                                         │     │
│  │   // Code here runs BEFORE the target                                   │     │
│  │                                                                         │     │
│  │   try {                                                                 │     │
│  │       Object result = joinPoint.proceed();  ──→ TARGET runs here        │     │
│  │                                                                         │     │
│  │       // Code here runs AFTER success                                   │     │
│  │       return result;  ──→ returned to caller                            │     │
│  │                                                                         │     │
│  │   } catch (Exception ex) {                                              │     │
│  │       // Code here runs AFTER failure                                   │     │
│  │       throw ex;  ──→ propagated to caller                               │     │
│  │   }                                                                     │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
│                                                                                  │
│  IMPORTANT:                                                                      │
│    • MUST call proceed() or the target never executes                            │
│    • MUST return the result or caller gets null                                  │
│    • MUST declare "throws Throwable" on the advice method                        │
│    • Uses ProceedingJoinPoint (not JoinPoint)                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Advanced @Around examples:

// 1. Modify arguments before calling target:
@Around("execution(* com.app.service.OrderService.createOrder(..))")
public Object uppercaseProductName(ProceedingJoinPoint pjp) throws Throwable {
    Object[] args = pjp.getArgs();
    if (args[0] instanceof String) {
        args[0] = ((String) args[0]).toUpperCase();  // modify first arg
    }
    return pjp.proceed(args);  // pass modified args to target
    // Original call: createOrder("laptop", 999.99)
    // Target receives: createOrder("LAPTOP", 999.99)
}

// 2. Skip method execution (caching):
@Around("@annotation(cacheable)")
public Object cacheResult(ProceedingJoinPoint pjp, Cacheable cacheable) throws Throwable {
    String key = generateKey(pjp);
    Object cached = cache.get(key);
    if (cached != null) {
        return cached;  // ← proceed() NOT called! Target never executes.
    }
    Object result = pjp.proceed();
    cache.put(key, result);
    return result;
}

// 3. Retry on failure:
@Around("@annotation(retryable)")
public Object retryOnFailure(ProceedingJoinPoint pjp, Retryable retryable) throws Throwable {
    int maxRetries = retryable.maxAttempts();
    Exception lastException = null;
    for (int i = 0; i < maxRetries; i++) {
        try {
            return pjp.proceed();  // ← call proceed() multiple times!
        } catch (Exception ex) {
            lastException = ex;
            Thread.sleep(1000 * (i + 1));  // backoff
        }
    }
    throw lastException;
}
```

---

#### 7. `@Pointcut`

**Meaning:** Defines a **named, reusable pointcut expression** that can be referenced by multiple advice methods. Avoids duplicating the same expression string across `@Before`, `@After`, `@Around`, etc.

**From:** `org.aspectj.lang.annotation.Pointcut`

**Applied to:** An empty method (the method body is never executed — only the method name is used as a reference).

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // ─── Named pointcut definitions ───
    @Pointcut("execution(* com.app.service.*.*(..))")
    public void serviceLayer() {}  // ← empty method — never called directly
    //          ↑ this NAME is used to reference the pointcut

    @Pointcut("execution(* com.app.repository.*.*(..))")
    public void repositoryLayer() {}

    @Pointcut("@annotation(com.app.annotation.Loggable)")
    public void loggableMethods() {}

    // ─── Combine named pointcuts ───
    @Pointcut("serviceLayer() || repositoryLayer()")
    public void dataAccessLayer() {}  // service OR repository methods

    @Pointcut("serviceLayer() && loggableMethods()")
    public void loggableServiceMethods() {}  // service methods WITH @Loggable

    // ─── Use named pointcuts in advice ───
    @Before("serviceLayer()")  // ← references the named pointcut
    public void logBeforeService(JoinPoint jp) {
        log.info(">>> {}", jp.getSignature().toShortString());
    }

    @Around("loggableServiceMethods()")  // ← references combined pointcut
    public Object logLoggable(ProceedingJoinPoint pjp) throws Throwable {
        log.info(">>> {}", pjp.getSignature().toShortString());
        Object result = pjp.proceed();
        log.info("<<< returned: {}", result);
        return result;
    }

    @AfterThrowing(pointcut = "dataAccessLayer()", throwing = "ex")
    public void logDataError(JoinPoint jp, Exception ex) {
        log.error("!!! {} threw: {}", jp.getSignature().toShortString(), ex.getMessage());
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Pointcut — Why use named pointcuts?                                            │
│                                                                                  │
│  WITHOUT @Pointcut (duplicate expressions):                                      │
│    @Before("execution(* com.app.service.*.*(..))")                               │
│    public void logBefore(...) { }                                                │
│                                                                                  │
│    @AfterReturning("execution(* com.app.service.*.*(..))")  ← SAME expression   │
│    public void logAfter(...) { }                                                 │
│                                                                                  │
│    @AfterThrowing("execution(* com.app.service.*.*(..))")   ← SAME expression   │
│    public void logError(...) { }                                                 │
│                                                                                  │
│    Problem: if package changes → update 3 places!                                │
│                                                                                  │
│  WITH @Pointcut (DRY):                                                           │
│    @Pointcut("execution(* com.app.service.*.*(..))")                             │
│    public void serviceLayer() {}   ← defined ONCE                                │
│                                                                                  │
│    @Before("serviceLayer()")       ← reference by name                           │
│    public void logBefore(...) { }                                                │
│                                                                                  │
│    @AfterReturning("serviceLayer()")  ← reference by name                        │
│    public void logAfter(...) { }                                                 │
│                                                                                  │
│    @AfterThrowing("serviceLayer()")   ← reference by name                        │
│    public void logError(...) { }                                                 │
│                                                                                  │
│    Benefit: if package changes → update 1 place!                                 │
│                                                                                  │
│  Cross-Aspect reuse (reference from another Aspect):                             │
│    @Before("com.app.aspect.LoggingAspect.serviceLayer()")                        │
│    public void securityCheck(...) { }                                            │
│    → Uses full class path + method name to reference another Aspect's pointcut   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8. `@Order`

**Meaning:** Controls the **execution order** of multiple Aspects. Lower number = higher priority = runs first on entry (outermost) and last on exit.

**From:** `org.springframework.core.annotation.Order`

**Applied to:** The Aspect class (not individual advice methods).

```java
@Aspect
@Component
@Order(1)  // ← FIRST to execute (outermost)
public class SecurityAspect {
    @Before("execution(* com.app.service.*.*(..))")
    public void checkAuth(JoinPoint jp) { /* security check */ }
}

@Aspect
@Component
@Order(2)  // ← SECOND to execute (middle)
public class LoggingAspect {
    @Around("execution(* com.app.service.*.*(..))")
    public Object log(ProceedingJoinPoint pjp) throws Throwable { /* logging */ }
}

@Aspect
@Component
@Order(3)  // ← THIRD to execute (innermost — closest to target)
public class PerformanceAspect {
    @Around("execution(* com.app.service.*.*(..))")
    public Object time(ProceedingJoinPoint pjp) throws Throwable { /* timing */ }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Order — Nesting Behavior:                                                      │
│                                                                                  │
│  @Order(1) enters first, exits last    (like outermost Russian doll)             │
│  @Order(3) enters last, exits first    (like innermost Russian doll)             │
│                                                                                  │
│  ┌──── @Order(1) SecurityAspect ──────────────────────────────────────────┐      │
│  │  ENTRY: Security check                                                 │      │
│  │  ┌──── @Order(2) LoggingAspect ─────────────────────────────────────┐  │      │
│  │  │  ENTRY: Log start                                                │  │      │
│  │  │  ┌──── @Order(3) PerformanceAspect ───────────────────────────┐  │  │      │
│  │  │  │  ENTRY: Start timer                                        │  │  │      │
│  │  │  │  ┌────────────────────────────────────────────────────┐    │  │  │      │
│  │  │  │  │  TARGET METHOD EXECUTES                            │    │  │  │      │
│  │  │  │  └────────────────────────────────────────────────────┘    │  │  │      │
│  │  │  │  EXIT: Stop timer, log duration                            │  │  │      │
│  │  │  └────────────────────────────────────────────────────────────┘  │  │      │
│  │  │  EXIT: Log result                                                │  │      │
│  │  └──────────────────────────────────────────────────────────────────┘  │      │
│  │  EXIT: Security done                                                   │      │
│  └────────────────────────────────────────────────────────────────────────┘      │
│                                                                                  │
│  Without @Order:                                                                 │
│    Execution order is UNDEFINED (random).                                        │
│    Spring does not guarantee any particular order.                                │
│    Always use @Order when multiple Aspects target the same methods.              │
│                                                                                  │
│  @Order values can be negative:                                                  │
│    @Order(-1) runs before @Order(0) which runs before @Order(1)                  │
│                                                                                  │
│  Default: Integer.MAX_VALUE (lowest priority — runs last)                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 9. `@EnableAspectJAutoProxy`

**Meaning:** Enables Spring's support for `@Aspect`-annotated classes and AOP proxy creation. Tells Spring to detect `@Aspect` beans and create proxies for advised beans.

**From:** `org.springframework.context.annotation.EnableAspectJAutoProxy`

**Applied to:** A `@Configuration` class or the main `@SpringBootApplication` class.

```java
@SpringBootApplication
@EnableAspectJAutoProxy  // ← enable AOP
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @EnableAspectJAutoProxy — Do you need it?                                       │
│                                                                                  │
│  Spring Boot: NOT required!                                                      │
│    Spring Boot auto-configures AOP when spring-boot-starter-aop is on classpath. │
│    AopAutoConfiguration enables @EnableAspectJAutoProxy automatically.           │
│    You DON'T need to add it manually.                                            │
│                                                                                  │
│  Plain Spring (non-Boot): REQUIRED!                                              │
│    Without @EnableAspectJAutoProxy, Spring ignores @Aspect classes entirely.     │
│    You must add it to a @Configuration class.                                    │
│                                                                                  │
│  Optional attributes:                                                            │
│    @EnableAspectJAutoProxy(proxyTargetClass = true)                               │
│      → Force CGLIB proxies (even when interface exists)                           │
│      → Spring Boot default is TRUE since Spring Boot 2.0                         │
│                                                                                  │
│    @EnableAspectJAutoProxy(exposeProxy = true)                                   │
│      → Makes the proxy accessible via AopContext.currentProxy()                  │
│      → Fixes the self-invocation problem:                                        │
│        ((OrderService) AopContext.currentProxy()).method1();                      │
│        → Goes through proxy instead of "this"                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 10. Pointcut Designator Annotations — `execution`, `@annotation`, `within`, `@within`

These are not Java annotations but **pointcut designators** (keywords used inside pointcut expressions). They define WHERE advice should be applied.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut Designators — Reference:                                               │
│                                                                                  │
│  ┌────────────────────┬──────────────────────────────────────────────────────────┐│
│  │ Designator         │ What It Matches                                          ││
│  ├────────────────────┼──────────────────────────────────────────────────────────┤│
│  │ execution(...)     │ Method execution matching the given signature pattern.   ││
│  │                    │ Most common designator.                                  ││
│  │                    │                                                          ││
│  │                    │ Syntax:                                                  ││
│  │                    │ execution(modifiers? return-type declaring-type?         ││
│  │                    │           method-name(params) throws?)                   ││
│  │                    │                                                          ││
│  │                    │ execution(* com.app.service.*.*(..))                     ││
│  │                    │    ↑ any return   ↑ any class  ↑ any method  ↑ any args  ││
│  ├────────────────────┼──────────────────────────────────────────────────────────┤│
│  │ @annotation(...)   │ Methods annotated with the specified annotation.         ││
│  │                    │                                                          ││
│  │                    │ @Before("@annotation(com.app.annotation.Loggable)")      ││
│  │                    │ → Matches any method with @Loggable annotation.          ││
│  │                    │                                                          ││
│  │                    │ @Around("@annotation(loggable)")                         ││
│  │                    │ public Object m(PJP pjp, Loggable loggable)             ││
│  │                    │ → Binds the annotation instance to a parameter.          ││
│  ├────────────────────┼──────────────────────────────────────────────────────────┤│
│  │ within(...)        │ Any method within classes matching the type pattern.     ││
│  │                    │                                                          ││
│  │                    │ within(com.app.service.*)                                ││
│  │                    │ → Any method in any class in the service package.        ││
│  │                    │                                                          ││
│  │                    │ within(com.app.service..*)                               ││
│  │                    │ → Service package + all sub-packages.                    ││
│  ├────────────────────┼──────────────────────────────────────────────────────────┤│
│  │ @within(...)       │ Any method within classes annotated with the given       ││
│  │                    │ annotation.                                              ││
│  │                    │                                                          ││
│  │                    │ @within(org.springframework.stereotype.Service)          ││
│  │                    │ → Any method in any class annotated with @Service.       ││
│  │                    │                                                          ││
│  │                    │ @within(org.springframework.web.bind.annotation          ││
│  │                    │         .RestController)                                 ││
│  │                    │ → Any method in any @RestController class.               ││
│  ├────────────────────┼──────────────────────────────────────────────────────────┤│
│  │ @target(...)       │ Any method on a bean whose runtime class is annotated    ││
│  │                    │ with the given annotation.                               ││
│  │                    │                                                          ││
│  │                    │ @target(com.app.annotation.Auditable)                    ││
│  │                    │ → Methods on beans whose class has @Auditable.           ││
│  │                    │ Similar to @within but resolved at runtime.              ││
│  ├────────────────────┼──────────────────────────────────────────────────────────┤│
│  │ args(...)          │ Methods where the arguments match the given types.       ││
│  │                    │                                                          ││
│  │                    │ args(String, ..)                                         ││
│  │                    │ → Methods where first arg is String, rest can be any.    ││
│  │                    │                                                          ││
│  │                    │ args(com.app.dto.OrderRequest)                           ││
│  │                    │ → Methods taking an OrderRequest parameter.              ││
│  ├────────────────────┼──────────────────────────────────────────────────────────┤│
│  │ bean(...)          │ Spring-specific. Matches by bean name.                   ││
│  │                    │                                                          ││
│  │                    │ bean(orderService)                                       ││
│  │                    │ → Any method on the bean named "orderService".           ││
│  │                    │                                                          ││
│  │                    │ bean(*Service)                                           ││
│  │                    │ → Any method on beans with names ending in "Service".    ││
│  └────────────────────┴──────────────────────────────────────────────────────────┘│
│                                                                                  │
│  Combining designators:                                                          │
│    && (AND): execution(* *(..)) && @annotation(Loggable)                         │
│    || (OR):  within(com.app.service.*) || within(com.app.repository.*)           │
│    !  (NOT): execution(* *(..)) && !execution(* *..get*(..))                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 11. Common Spring Annotations That Use AOP Internally

These annotations are not AOP annotations themselves, but they **work through Spring AOP** internally. Understanding this helps you know why they have the same limitations (e.g., self-invocation bypass).

```java
// @Transactional — Transaction management via AOP
@Transactional
public Order createOrder(OrderRequest req) {
    // Spring AOP wraps this with TransactionInterceptor
    // BEGIN TX → execute method → COMMIT (or ROLLBACK on exception)
    return orderRepo.save(new Order(req));
}

// @Transactional attributes:
@Transactional(
    readOnly = true,                    // optimize for read-only queries
    isolation = Isolation.READ_COMMITTED,// transaction isolation level
    propagation = Propagation.REQUIRED,  // transaction propagation behavior
    rollbackFor = Exception.class,       // rollback on checked exceptions too
    timeout = 30                         // seconds before timeout
)
public List<Order> getOrders() { ... }
```

```java
// @Cacheable — Cache results via AOP
@Cacheable(value = "orders", key = "#id")
public Order getOrder(Long id) {
    // First call → executes method, caches result
    // Subsequent calls with same id → returns cached result (method NEVER runs)
    return orderRepo.findById(id).orElseThrow();
}

// @CacheEvict — Remove from cache via AOP
@CacheEvict(value = "orders", key = "#id")
public void deleteOrder(Long id) {
    orderRepo.deleteById(id);
}

// @CachePut — Always execute and update cache
@CachePut(value = "orders", key = "#result.id")
public Order updateOrder(Order order) {
    return orderRepo.save(order);
}
```

```java
// @Async — Execute in separate thread via AOP
@Async
public CompletableFuture<Report> generateReport(String type) {
    // Spring AOP intercepts, submits to thread pool
    // Caller gets CompletableFuture immediately
    Report report = createReport(type);  // runs in background thread
    return CompletableFuture.completedFuture(report);
}
```

```java
// @PreAuthorize / @Secured — Security via AOP
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
    // Spring Security AOP checks permission BEFORE method runs
    // If unauthorized → AccessDeniedException thrown, method NEVER executes
    userRepo.deleteById(id);
}

@Secured("ROLE_ADMIN")  // simpler alternative to @PreAuthorize
public void deleteUser(Long id) {
    userRepo.deleteById(id);
}
```

```java
// @Retryable — Automatic retry via AOP (Spring Retry)
@Retryable(
    value = {DataAccessException.class},  // retry on these exceptions
    maxAttempts = 3,                       // try up to 3 times
    backoff = @Backoff(delay = 1000)       // wait 1s between retries
)
public Order processOrder(OrderRequest req) {
    // If DataAccessException thrown → retry up to 3 times
    // Spring Retry AOP handles the retry loop
    return externalService.process(req);
}

@Recover  // fallback when all retries exhausted
public Order recoverProcess(DataAccessException ex, OrderRequest req) {
    return new Order("FAILED");
}
```

```java
// @Validated — Method parameter validation via AOP
@Validated
@Service
public class OrderService {

    public Order createOrder(@Valid OrderRequest req) {
        // Spring AOP triggers MethodValidationInterceptor
        // If @Valid fails → ConstraintViolationException before method runs
        return orderRepo.save(new Order(req));
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Annotations That Use AOP — Summary:                                             │
│                                                                                  │
│  ┌──────────────────────┬───────────────────────────┬────────────────────────────┐│
│  │ Annotation           │ AOP Interceptor           │ What It Does              ││
│  ├──────────────────────┼───────────────────────────┼────────────────────────────┤│
│  │ @Transactional       │ TransactionInterceptor    │ Begin/commit/rollback TX  ││
│  │ @Cacheable           │ CacheInterceptor          │ Cache method results      ││
│  │ @CacheEvict          │ CacheInterceptor          │ Remove from cache         ││
│  │ @CachePut            │ CacheInterceptor          │ Update cache entry        ││
│  │ @Async               │ AsyncExecutionInterceptor │ Run in separate thread    ││
│  │ @PreAuthorize        │ MethodSecurityInterceptor │ Check authorization       ││
│  │ @Secured             │ MethodSecurityInterceptor │ Check roles               ││
│  │ @Retryable           │ RetryOperationsInterceptor│ Auto-retry on failure     ││
│  │ @Validated / @Valid  │ MethodValidationInterceptor│ Validate parameters      ││
│  └──────────────────────┴───────────────────────────┴────────────────────────────┘│
│                                                                                  │
│  ALL of these share the same AOP limitation:                                     │
│    self-invocation (this.method()) BYPASSES the proxy → annotation IGNORED.      │
│                                                                                  │
│  Example:                                                                        │
│    public class OrderService {                                                   │
│        @Transactional                                                            │
│        public void method1() { /* TX works */ }                                  │
│                                                                                  │
│        public void method2() {                                                   │
│            this.method1();  // ← @Transactional IGNORED! No transaction.         │
│        }                                                                         │
│    }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Complete Annotation Reference Table

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring AOP Annotations — Quick Reference:                                       │
│                                                                                  │
│  ┌───────────────────────────┬─────────────┬─────────────────────────────────────┐│
│  │ Annotation                │ Applied To  │ Purpose                             ││
│  ├───────────────────────────┼─────────────┼─────────────────────────────────────┤│
│  │ @Aspect                   │ Class       │ Marks class as AOP Aspect           ││
│  │ @Component                │ Class       │ Registers Aspect as Spring bean     ││
│  │ @Before                   │ Method      │ Run advice BEFORE target method     ││
│  │ @After                    │ Method      │ Run advice AFTER (always — finally) ││
│  │ @AfterReturning           │ Method      │ Run advice AFTER success            ││
│  │ @AfterThrowing            │ Method      │ Run advice AFTER exception          ││
│  │ @Around                   │ Method      │ Wrap target method (full control)   ││
│  │ @Pointcut                 │ Method      │ Define reusable pointcut expression ││
│  │ @Order                    │ Class       │ Control aspect execution order      ││
│  │ @EnableAspectJAutoProxy   │ Class       │ Enable AOP (auto in Spring Boot)    ││
│  └───────────────────────────┴─────────────┴─────────────────────────────────────┘│
│                                                                                  │
│  ┌───────────────────────────┬─────────────┬─────────────────────────────────────┐│
│  │ Advice Annotation         │ JoinPoint   │ Can Access                          ││
│  ├───────────────────────────┼─────────────┼─────────────────────────────────────┤│
│  │ @Before                   │ JoinPoint   │ Method name, args                   ││
│  │ @After                    │ JoinPoint   │ Method name, args                   ││
│  │ @AfterReturning           │ JoinPoint   │ Method name, args, return value     ││
│  │ @AfterThrowing            │ JoinPoint   │ Method name, args, exception        ││
│  │ @Around                   │ Proceeding  │ Everything + control execution      ││
│  │                           │ JoinPoint   │                                     ││
│  └───────────────────────────┴─────────────┴─────────────────────────────────────┘│
│                                                                                  │
│  When to use which advice:                                                       │
│  ┌───────────────────────────┬───────────────────────────────────────────────────┐│
│  │ Scenario                  │ Use                                               ││
│  ├───────────────────────────┼───────────────────────────────────────────────────┤│
│  │ Log method entry          │ @Before                                           ││
│  │ Log method result         │ @AfterReturning                                   ││
│  │ Log errors                │ @AfterThrowing                                    ││
│  │ Cleanup (always)          │ @After                                            ││
│  │ Measure execution time    │ @Around                                           ││
│  │ Caching                   │ @Around (skip proceed() on cache hit)             ││
│  │ Retry logic               │ @Around (call proceed() in loop)                  ││
│  │ Modify args/result        │ @Around (only advice with full control)           ││
│  │ Transaction management    │ @Around (begin before, commit/rollback after)     ││
│  │ Simple logging (all)      │ @Around (covers before + after + error)           ││
│  └───────────────────────────┴───────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```
---

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

### Summary

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring AOP — Complete Summary:                                                  │
│                                                                                  │
│  WHAT:  Programming paradigm to modularize cross-cutting concerns                │
│  WHY:   Eliminate code scattering/tangling, achieve SRP, DRY                     │
│  HOW:   Spring creates PROXIES around beans, intercepts method calls,            │
│         runs advice code before/after/around target methods                      │
│                                                                                  │
│  Setup:                                                                          │
│    1. Add spring-boot-starter-aop dependency                                     │
│    2. Create @Aspect @Component class                                            │
│    3. Define advice methods with pointcut expressions                            │
│    4. Spring auto-detects and applies at startup                                 │
│                                                                                  │
│  Key Concepts:                                                                   │
│    Aspect    = the class (@Aspect)                                               │
│    Advice    = the method (@Before, @After, @Around, etc.)                       │
│    Pointcut  = where to apply (execution expression or @annotation)              │
│    JoinPoint = the intercepted method call                                       │
│    Proxy     = the wrapper Spring creates around target bean                     │
│                                                                                  │
│  Best Practices:                                                                 │
│    ✓ Use @annotation-based pointcuts over package-based (more explicit)          │
│    ✓ Use @Order to control multiple aspect execution order                       │
│    ✓ Keep aspects focused (one concern per aspect)                               │
│    ✓ Handle exceptions in @Around (don't swallow them)                           │
│    ✓ Always call proceed() in @Around (unless intentionally skipping)            │
│    ✓ Be aware of self-invocation limitation                                      │
│    ✗ Don't put business logic in aspects                                         │
│    ✗ Don't use overly broad pointcuts (e.g., execution(* *.*(..)))              │
│    ✗ Don't forget @Component on @Aspect classes                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

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

### Pointcut Expression Structure — Complete Breakdown

The **pointcut expression** is the string you write inside `@Before(...)`, `@Around(...)`, etc. It follows a specific **syntax** that Spring AOP parses to determine which methods to intercept.

The most common pointcut type is `execution(...)`. Here is its **complete structure**:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  execution() Pointcut Expression — Full Syntax:                                  │
│                                                                                  │
│  execution( modifiers?  return-type  declaring-type?  method-name(params)  throws? )
│             ────┬────   ─────┬─────  ──────┬───────   ─────┬─────  ──┬──  ───┬───│
│                 │            │             │               │        │      │    │
│                 │            │             │               │        │      │    │
│           (optional)    (REQUIRED)    (optional)      (REQUIRED)   │  (optional)│
│            public        * or type   package.Class    methodName  args  exception│
│            protected     void/String                  or *               type   │
│                          etc.                                                    │
│                                                                                  │
│  ┌────────────────┬──────────┬───────────────────────────────────────────────────┐│
│  │ Part           │ Required │ Description                                       ││
│  ├────────────────┼──────────┼───────────────────────────────────────────────────┤│
│  │ modifiers      │ NO       │ public, protected, etc. Rarely used.              ││
│  │ return-type    │ YES      │ The return type of the method.                    ││
│  │                │          │ * = any return type.                              ││
│  │ declaring-type │ NO       │ The fully qualified class/package.                ││
│  │                │          │ If omitted, matches any class.                    ││
│  │ method-name    │ YES      │ The method name. * = any method.                  ││
│  │ params         │ YES      │ The parameter list. (..) = any params.            ││
│  │ throws         │ NO       │ The exception types. Rarely used.                 ││
│  └────────────────┴──────────┴───────────────────────────────────────────────────┘│
│                                                                                  │
│  Minimum expression (only required parts):                                       │
│    execution(* *(..))                                                            │
│              ↑ ↑  ↑                                                              │
│              │ │  └── any params                                                 │
│              │ └── any method name                                               │
│              └── any return type                                                 │
│    → Matches EVERY method in EVERY class (way too broad — never use this!)       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Part 1: Package, Class, and Method Name

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Declaring-Type (Package + Class) and Method Name:                               │
│                                                                                  │
│  execution(* com.app.service.OrderService.createOrder(..))                       │
│              └──────┬───────  ─────┬──────  ─────┬────────┘                      │
│                     │              │             │                                │
│                 PACKAGE         CLASS        METHOD NAME                          │
│          (com.app.service)  (OrderService)  (createOrder)                        │
│                                                                                  │
│  Together, the package + class form the "declaring-type":                        │
│    com.app.service.OrderService                                                  │
│    └──────────────────────────┘                                                  │
│         declaring-type                                                           │
│                                                                                  │
│  The declaring-type is OPTIONAL:                                                 │
│    execution(* createOrder(..))                                                  │
│    → Matches createOrder() in ANY class in ANY package                           │
│                                                                                  │
│    execution(* com.app.service.OrderService.createOrder(..))                     │
│    → Matches createOrder() ONLY in OrderService class                            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Match a SPECIFIC method in a SPECIFIC class ───

// Match: OrderService.createOrder()
@Before("execution(* com.app.service.OrderService.createOrder(..))")
public void beforeCreateOrder(JoinPoint jp) { }

// Match: PaymentService.processPayment()
@Before("execution(* com.app.service.PaymentService.processPayment(..))")
public void beforeProcessPayment(JoinPoint jp) { }
```

```java
// ─── Match ALL methods in a SPECIFIC class ───

// Match: any method in OrderService
@Before("execution(* com.app.service.OrderService.*(..))")
//                                                 ↑
//                                          * = any method name
public void beforeAnyOrderMethod(JoinPoint jp) { }

// Results:
//   OrderService.createOrder(...)   → MATCHES ✓
//   OrderService.getOrder(...)      → MATCHES ✓
//   OrderService.cancelOrder(...)   → MATCHES ✓
//   PaymentService.process(...)     → NO MATCH ✗
```

```java
// ─── Match ALL methods in ALL classes in a SPECIFIC package ───

// Match: any method in any class in com.app.service package
@Before("execution(* com.app.service.*.*(..))")
//                                  ↑ ↑
//                     any class ───┘ └── any method
public void beforeAnyServiceMethod(JoinPoint jp) { }

// Results:
//   OrderService.createOrder(...)     → MATCHES ✓ (in service package)
//   PaymentService.process(...)       → MATCHES ✓ (in service package)
//   UserService.createUser(...)       → MATCHES ✓ (in service package)
//   OrderRepository.save(...)         → NO MATCH ✗ (repository package)
//   OrderController.create(...)       → NO MATCH ✗ (controller package)
```

```java
// ─── Match ALL methods in a package AND all SUB-PACKAGES ───

// Match: any method in com.app.service and its sub-packages
@Before("execution(* com.app.service..*.*(..))")
//                                   ↑↑
//                                   .. = this package AND all sub-packages
public void beforeAnyServiceOrSubPackage(JoinPoint jp) { }

// Package structure:
//   com.app.service/
//     OrderService.java            → MATCHES ✓ (direct package)
//     PaymentService.java          → MATCHES ✓ (direct package)
//   com.app.service.order/
//     OrderDetailService.java      → MATCHES ✓ (sub-package)
//   com.app.service.payment/
//     PaymentGatewayService.java   → MATCHES ✓ (sub-package)
//   com.app.service.payment.stripe/
//     StripeService.java           → MATCHES ✓ (nested sub-package)
//   com.app.repository/
//     OrderRepository.java         → NO MATCH ✗ (different package tree)
```

```java
// ─── Match methods with a SPECIFIC name pattern ───

// Match: any method starting with "get" in any service class
@Before("execution(* com.app.service.*.get*(..))")
//                                        ↑
//                                    get* = starts with "get"
public void beforeAnyGetter(JoinPoint jp) { }

// Results:
//   OrderService.getOrder(...)        → MATCHES ✓ (starts with "get")
//   OrderService.getAll()             → MATCHES ✓ (starts with "get")
//   UserService.getUserById(...)      → MATCHES ✓ (starts with "get")
//   OrderService.createOrder(...)     → NO MATCH ✗ (starts with "create")

// Match: any method starting with "create" or "save"
@Before("execution(* com.app.service.*.create*(..)) || " +
        "execution(* com.app.service.*.save*(..))")
public void beforeCreateOrSave(JoinPoint jp) { }

// Match: any method ending with "Order"
@Before("execution(* com.app.service.*.*Order(..))")
public void beforeOrderMethods(JoinPoint jp) { }
// Matches: createOrder, getOrder, cancelOrder, deleteOrder, updateOrder
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Package + Class + Method — Pattern Examples:                                    │
│                                                                                  │
│  ┌──────────────────────────────────────────────────┬────────────────────────────┐│
│  │ Expression                                       │ What it matches            ││
│  ├──────────────────────────────────────────────────┼────────────────────────────┤│
│  │ * com.app.service.OrderService.createOrder(..)   │ One specific method        ││
│  │ * com.app.service.OrderService.*(..)             │ All methods in one class   ││
│  │ * com.app.service.*.*(..)                        │ All methods in one package ││
│  │ * com.app.service..*.*(..)                       │ Package + sub-packages     ││
│  │ * com.app..*.*(..)                               │ Entire app (all packages)  ││
│  │ * com.app.service.*.get*(..)                     │ Methods starting with get  ││
│  │ * com.app.service.*.*Order(..)                   │ Methods ending with Order  ││
│  │ * *.createOrder(..)                              │ createOrder in any class   ││
│  │ * createOrder(..)                                │ createOrder in any class   ││
│  │                                                  │ (no declaring-type)        ││
│  └──────────────────────────────────────────────────┴────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Part 2: Return Types

The **return type** is the first required part of the `execution()` expression. It specifies what the method must return for the pointcut to match.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Return Type Position:                                                           │
│                                                                                  │
│  execution( RETURN_TYPE  declaring-type.method-name(params) )                    │
│             ↑                                                                    │
│             First thing after "execution("                                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Match ANY return type ───
@Before("execution(* com.app.service.OrderService.*(..))")
//                  ↑
//              * = any return type (void, String, Order, List, etc.)
public void matchAnyReturn(JoinPoint jp) { }

// Matches:
//   public Order createOrder(...)           → ✓ (returns Order)
//   public List<Order> getAllOrders()        → ✓ (returns List)
//   public void cancelOrder(...)            → ✓ (returns void)
//   public String getStatus(...)            → ✓ (returns String)
```

```java
// ─── Match VOID return type only ───
@Before("execution(void com.app.service.OrderService.*(..))")
//                  ↑
//              void = only methods that return nothing
public void matchOnlyVoid(JoinPoint jp) { }

// Matches:
//   public void cancelOrder(Long id)        → ✓ (void)
//   public void deleteOrder(Long id)        → ✓ (void)
//   public Order createOrder(...)           → ✗ (returns Order, not void)
//   public String getStatus(...)            → ✗ (returns String, not void)
```

```java
// ─── Match a SPECIFIC return type ───

// Only methods that return Order
@Before("execution(com.app.entity.Order com.app.service.OrderService.*(..))")
//                  └────────────────┘
//                  fully qualified return type
public void matchOrderReturn(JoinPoint jp) { }

// Matches:
//   public Order createOrder(...)           → ✓ (returns Order)
//   public Order getOrder(Long id)          → ✓ (returns Order)
//   public List<Order> getAllOrders()        → ✗ (returns List<Order>, NOT Order)
//   public void cancelOrder(...)            → ✗ (returns void)

// Only methods that return String
@Before("execution(String com.app.service.OrderService.*(..))")
public void matchStringReturn(JoinPoint jp) { }

// Only methods that return List (note: generics are erased at runtime)
@Before("execution(java.util.List com.app.service.OrderService.*(..))")
public void matchListReturn(JoinPoint jp) { }
// Matches: List<Order>, List<String>, List<Payment> — all are just "List" at runtime
```

```java
// ─── Match boolean return type ───
@Before("execution(boolean com.app.service.*.*(..))")
public void matchBooleanReturn(JoinPoint jp) { }

// Matches:
//   public boolean isOrderValid(Long id)    → ✓
//   public boolean hasPermission(...)       → ✓
//   public Boolean isActive(...)            → ✗ (Boolean wrapper ≠ boolean primitive!)

// For wrapper type:
@Before("execution(java.lang.Boolean com.app.service.*.*(..))")
public void matchBooleanWrapperReturn(JoinPoint jp) { }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Return Type — Examples Summary:                                                 │
│                                                                                  │
│  ┌─────────────────────────────────────┬─────────────────────────────────────────┐│
│  │ Return type in expression           │ What it matches                         ││
│  ├─────────────────────────────────────┼─────────────────────────────────────────┤│
│  │ *                                   │ ANY return type (most common)           ││
│  │ void                                │ Methods that return nothing             ││
│  │ String                              │ Methods returning String                ││
│  │ int                                 │ Methods returning primitive int         ││
│  │ java.lang.Integer                   │ Methods returning Integer wrapper       ││
│  │ com.app.entity.Order                │ Methods returning Order entity          ││
│  │ java.util.List                      │ Methods returning any List              ││
│  │ java.util.Map                       │ Methods returning any Map               ││
│  │ org.springframework.http            │ Methods returning ResponseEntity        ││
│  │   .ResponseEntity                   │                                         ││
│  └─────────────────────────────────────┴─────────────────────────────────────────┘│
│                                                                                  │
│  IMPORTANT:                                                                      │
│    • For types outside java.lang, use FULLY QUALIFIED class name                 │
│    • String works without package because it's in java.lang                      │
│    • int ≠ Integer — primitive vs wrapper are different types                    │
│    • Generics are ERASED — List<Order> and List<String> both match "List"        │
│    • Use * (wildcard) 99% of the time — filtering by return type is rare         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Part 3: Arguments — With Parameters, Without Parameters

The **parameter list** (inside parentheses) specifies what arguments the method must accept for the pointcut to match.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Parameter List Position:                                                        │
│                                                                                  │
│  execution( return-type  declaring-type.method-name( PARAMS ) )                  │
│                                                      ↑                           │
│                                            Inside the parentheses                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Match methods with ANY parameters (including no parameters) ───
@Before("execution(* com.app.service.OrderService.*(..))")
//                                                    ↑↑
//                                                 (..) = zero or more arguments of any type
public void matchAnyParams(JoinPoint jp) { }

// Matches:
//   createOrder()                              → ✓ (0 args)
//   createOrder(String product)                → ✓ (1 arg)
//   createOrder(String product, Double amount) → ✓ (2 args)
//   getOrder(Long id)                          → ✓ (1 arg)
```

```java
// ─── Match methods with NO parameters (zero arguments) ───
@Before("execution(* com.app.service.OrderService.*())")
//                                                  ↑↑
//                                               () = exactly zero arguments
public void matchNoParams(JoinPoint jp) { }

// Matches:
//   getAllOrders()                              → ✓ (0 args)
//   getCount()                                 → ✓ (0 args)
//   createOrder(String product)                → ✗ (has 1 arg)
//   createOrder(String product, Double amount) → ✗ (has 2 args)
```

```java
// ─── Match methods with EXACTLY one parameter of a SPECIFIC type ───

// One String parameter
@Before("execution(* com.app.service.OrderService.*(String))")
//                                                    ↑
//                                          exactly one String arg
public void matchOneString(JoinPoint jp) { }

// Matches:
//   getByStatus(String status)                 → ✓ (1 String)
//   findByProduct(String product)              → ✓ (1 String)
//   getOrder(Long id)                          → ✗ (Long, not String)
//   createOrder(String product, Double amount) → ✗ (2 args, not 1)

// One Long parameter
@Before("execution(* com.app.service.OrderService.*(Long))")
public void matchOneLong(JoinPoint jp) { }

// Matches:
//   getOrder(Long id)                          → ✓
//   deleteOrder(Long id)                       → ✓
//   getOrder(long id)                          → ✗ (long ≠ Long!)
```

```java
// ─── Match methods with EXACTLY two specific parameters ───
@Before("execution(* com.app.service.OrderService.*(String, Double))")
//                                                    ↑        ↑
//                                              1st arg   2nd arg
public void matchStringAndDouble(JoinPoint jp) { }

// Matches:
//   createOrder(String product, Double amount) → ✓ (String, Double)
//   createOrder(String name, Double price)     → ✓ (String, Double)
//   createOrder(Double amount, String product) → ✗ (ORDER matters! Double, String)
//   createOrder(String product)                → ✗ (only 1 arg, needs 2)
```

```java
// ─── Match methods with a custom object type parameter ───
@Before("execution(* com.app.service.OrderService.*(com.app.dto.OrderRequest))")
//                                                    └──────────────────────┘
//                                                    fully qualified DTO class
public void matchOrderRequest(JoinPoint jp) { }

// Matches:
//   createOrder(OrderRequest req)              → ✓
//   updateOrder(OrderRequest req)              → ✓
//   createOrder(String product, Double amount) → ✗ (different param types)
```

```java
// ─── Match methods with one specific param followed by any number of params ───
@Before("execution(* com.app.service.OrderService.*(String, ..))")
//                                                    ↑       ↑↑
//                                              first must  rest can be
//                                              be String   anything
public void matchStringThenAnything(JoinPoint jp) { }

// Matches:
//   findByProduct(String product)                        → ✓ (String + 0 more)
//   createOrder(String product, Double amount)           → ✓ (String + 1 more)
//   search(String query, int page, int size)             → ✓ (String + 2 more)
//   getOrder(Long id)                                    → ✗ (first arg is Long, not String)
//   getAllOrders()                                        → ✗ (no args at all)
```

```java
// ─── Match methods with EXACTLY one parameter of ANY type ───
@Before("execution(* com.app.service.OrderService.*(*))")
//                                                    ↑
//                                             (*) = exactly one arg of any type
public void matchExactlyOneParam(JoinPoint jp) { }

// Matches:
//   getOrder(Long id)              → ✓ (1 arg)
//   findByStatus(String status)    → ✓ (1 arg)
//   process(OrderRequest req)      → ✓ (1 arg)
//   getAllOrders()                  → ✗ (0 args)
//   create(String p, Double a)     → ✗ (2 args)
```

```java
// ─── Match methods with EXACTLY two parameters of ANY type ───
@Before("execution(* com.app.service.OrderService.*(*, *))")
//                                                    ↑  ↑
//                                              any  any (exactly 2)
public void matchExactlyTwoParams(JoinPoint jp) { }

// Matches:
//   createOrder(String product, Double amount) → ✓ (2 args)
//   update(Long id, OrderRequest req)          → ✓ (2 args)
//   getOrder(Long id)                          → ✗ (1 arg)
//   search(String q, int page, int size)       → ✗ (3 args)
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Parameter Patterns — Complete Reference:                                        │
│                                                                                  │
│  ┌──────────────────────────┬────────────────────────────────────────────────────┐│
│  │ Pattern                  │ What it matches                                    ││
│  ├──────────────────────────┼────────────────────────────────────────────────────┤│
│  │ (..)                     │ Zero or more args of any type (most common)        ││
│  │ ()                       │ Exactly zero args                                  ││
│  │ (*)                      │ Exactly one arg of any type                        ││
│  │ (*, *)                   │ Exactly two args of any type                       ││
│  │ (*, *, *)                │ Exactly three args of any type                     ││
│  │ (String)                 │ Exactly one arg of type String                     ││
│  │ (Long)                   │ Exactly one arg of type Long                       ││
│  │ (String, Double)         │ Exactly: first String, second Double               ││
│  │ (String, ..)             │ First arg String, then zero or more of any type    ││
│  │ (.., String)             │ Any args, but last arg must be String              ││
│  │ (String, *, ..)          │ First String, then one of any, then zero or more   ││
│  │ (com.app.dto.OrderReq)  │ Exactly one arg of custom DTO type                 ││
│  │ (com.app.dto.OrderReq,  │ Custom DTO + Long (two specific args)              ││
│  │  Long)                   │                                                    ││
│  └──────────────────────────┴────────────────────────────────────────────────────┘│
│                                                                                  │
│  NOTE: Argument ORDER matters!                                                   │
│    (String, Long) ≠ (Long, String)                                               │
│    The types must match in the EXACT positions.                                   │
│                                                                                  │
│  NOTE: Primitive vs Wrapper are DIFFERENT:                                        │
│    (int)    matches method(int x)        but NOT method(Integer x)               │
│    (Long)   matches method(Long x)       but NOT method(long x)                  │
│    (double) matches method(double x)     but NOT method(Double x)                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Part 4: Wildcards — `*`, `..`, and Where to Use Each

Spring AOP provides two wildcard characters: `*` (single wildcard) and `..` (multi-level wildcard). Each has specific places where it can be used and each means something different depending on context.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Wildcard: * (STAR)                                                              │
│                                                                                  │
│  Meaning: "matches ONE thing" — what it matches depends on WHERE it's used:     │
│                                                                                  │
│  ┌───────────────────────┬───────────────────────────────────────────────────────┐│
│  │ Position              │ What * means                                          ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Return type           │ Any return type (void, String, Order, etc.)           ││
│  │   execution(* ...)    │                                                       ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Class name            │ Any class name                                        ││
│  │   com.app.service.*   │ Any class in com.app.service package                  ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Method name           │ Any method name                                       ││
│  │   *.*(...)            │ Any method in any class                               ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Method name (partial) │ Partial match (like a glob)                           ││
│  │   *.get*(...)         │ Methods starting with "get"                           ││
│  │   *.*Order(...)       │ Methods ending with "Order"                           ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Inside params         │ Exactly ONE parameter of any type                     ││
│  │   (*)                 │ One arg of any type                                   ││
│  │   (*, *)              │ Two args of any type                                  ││
│  │   (String, *)         │ First is String, second is any type                   ││
│  └───────────────────────┴───────────────────────────────────────────────────────┘│
│                                                                                  │
│  KEY RULE: * always matches exactly ONE element (one type, one name, one class). │
│            It does NOT match across package levels or multiple parameters.        │
│                                                                                  │
│  Example: com.app.* matches com.app.OrderService                                 │
│           but DOES NOT match com.app.service.OrderService (two levels)            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Wildcard: .. (DOUBLE DOT)                                                       │
│                                                                                  │
│  Meaning: "matches ZERO or MORE things" — depends on WHERE it's used:           │
│                                                                                  │
│  ┌───────────────────────┬───────────────────────────────────────────────────────┐│
│  │ Position              │ What .. means                                         ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ In package path       │ Zero or more sub-package levels                       ││
│  │   com.app..           │ com.app and ALL sub-packages recursively              ││
│  │   com.app.service..   │ com.app.service and ALL its sub-packages             ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Inside params         │ Zero or more parameters of any type                   ││
│  │   (..)                │ Any number of args (including none)                   ││
│  │   (String, ..)        │ First is String, then any number of any type         ││
│  │   (.., Long)          │ Any number of any type, last must be Long             ││
│  └───────────────────────┴───────────────────────────────────────────────────────┘│
│                                                                                  │
│  KEY RULE: .. matches ZERO or MORE elements (across package levels or params).   │
│            It CANNOT be used as return type or method name.                       │
│                                                                                  │
│  IMPORTANT:                                                                      │
│    * in package  → matches ONE package level:  com.app.*         → com.app.XYZ   │
│    .. in package → matches MULTIPLE levels:    com.app.service.. → com.app.service│
│                                                                    com.app.service.order│
│                                                                    com.app.service.order.detail│
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── * (star) in different positions ───

// Position 1: Return type
@Before("execution(* com.app.service.OrderService.createOrder(..))")
//                  ↑ = any return type

// Position 2: Class name
@Before("execution(* com.app.service.*.createOrder(..))")
//                                    ↑ = any class in the service package

// Position 3: Method name
@Before("execution(* com.app.service.OrderService.*(..))")
//                                                 ↑ = any method in OrderService

// Position 4: Partial method name
@Before("execution(* com.app.service.OrderService.get*(..))")
//                                                 ↑↑↑ = methods starting with "get"
@Before("execution(* com.app.service.OrderService.*Order(..))")
//                                                 ↑     = methods ending with "Order"

// Position 5: In parameter list (one arg of any type)
@Before("execution(* com.app.service.OrderService.*(*))")
//                                                   ↑ = exactly one arg, any type

// Combined: * in multiple positions
@Before("execution(* com.app.service.*.*(*))")
//                  ↑                ↑ ↑  ↑
//              any return     any class  │  one arg any type
//                              any method┘
```

```java
// ─── .. (double dot) in different positions ───

// Position 1: In package path (sub-packages)
@Before("execution(* com.app.service..*.*(..))")
//                                    ↑↑
//                           service package + ALL sub-packages
//  Matches: com.app.service.OrderService.createOrder()
//           com.app.service.order.OrderDetailService.getDetails()
//           com.app.service.order.history.OrderHistoryService.getHistory()

// Position 2: In parameter list (any number of args)
@Before("execution(* com.app.service.OrderService.*(..))")
//                                                    ↑↑
//                                          zero or more args of any type

// Position 3: First param fixed, rest flexible
@Before("execution(* com.app.service.OrderService.*(String, ..))")
//                                                    ↑        ↑↑
//                                              1st=String   rest=anything

// Position 4: Last param fixed, front flexible
@Before("execution(* com.app.service.OrderService.*(.., Long))")
//                                                    ↑↑    ↑
//                                              anything  last=Long
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  * vs .. — Side-by-Side Comparison:                                              │
│                                                                                  │
│  ┌──────────────────────────────────┬──────────────────────────────────────────┐  │
│  │ Using * (single)                 │ Using .. (multi)                         │  │
│  ├──────────────────────────────────┼──────────────────────────────────────────┤  │
│  │ PACKAGE:                         │ PACKAGE:                                 │  │
│  │ com.app.service.*.*(..)          │ com.app.service..*.*(..)                 │  │
│  │ Matches:                         │ Matches:                                 │  │
│  │ ✓ com.app.service.OrderService   │ ✓ com.app.service.OrderService           │  │
│  │ ✓ com.app.service.PaymentService │ ✓ com.app.service.PaymentService         │  │
│  │ ✗ com.app.service.order.Detail   │ ✓ com.app.service.order.Detail           │  │
│  │ ✗ com.app.service.a.b.Deep      │ ✓ com.app.service.a.b.Deep               │  │
│  │ (* = one level only)             │ (.. = any depth)                         │  │
│  ├──────────────────────────────────┼──────────────────────────────────────────┤  │
│  │ PARAMS:                          │ PARAMS:                                  │  │
│  │ method(*)                        │ method(..)                               │  │
│  │ Matches:                         │ Matches:                                 │  │
│  │ ✓ method(String x)              │ ✓ method()                               │  │
│  │ ✓ method(Long y)                │ ✓ method(String x)                       │  │
│  │ ✗ method()                      │ ✓ method(String x, Long y)               │  │
│  │ ✗ method(String x, Long y)     │ ✓ method(String x, Long y, int z)        │  │
│  │ (* = exactly one param)          │ (.. = zero or more params)               │  │
│  └──────────────────────────────────┴──────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Comprehensive Wildcard Examples with Matching Results:**

```java
// Assume this service class structure:
//
// Package: com.app.service
//   OrderService:
//     public Order createOrder(String product, Double amount)
//     public Order getOrder(Long id)
//     public List<Order> getAllOrders()
//     public void cancelOrder(Long id)
//     public Order updateOrder(Long id, OrderRequest req)
//
//   PaymentService:
//     public Payment processPayment(PaymentRequest req)
//     public void refund(Long paymentId)
//
// Package: com.app.service.reporting
//   ReportService:
//     public Report generateReport(String type, int year)
//
// Package: com.app.repository
//   OrderRepository:
//     public Order save(Order order)

// ═══ Example 1: * as return type ═══
@Before("execution(void com.app.service.OrderService.*(..))")
// Matches:
//   ✓ cancelOrder(Long id)               → returns void
//   ✗ createOrder(String, Double)         → returns Order
//   ✗ getOrder(Long)                      → returns Order
//   ✗ getAllOrders()                       → returns List<Order>

// ═══ Example 2: * as class name (one package level) ═══
@Before("execution(* com.app.service.*.*(..))")
// Matches:
//   ✓ OrderService.createOrder(...)       → in service package
//   ✓ OrderService.getOrder(...)          → in service package
//   ✓ PaymentService.processPayment(...)  → in service package
//   ✗ ReportService.generateReport(...)   → in service.reporting (sub-package!)
//   ✗ OrderRepository.save(...)           → in repository package

// ═══ Example 3: .. as sub-package (recursive) ═══
@Before("execution(* com.app.service..*.*(..))")
// Matches:
//   ✓ OrderService.createOrder(...)       → in service package
//   ✓ PaymentService.processPayment(...)  → in service package
//   ✓ ReportService.generateReport(...)   → in service.reporting (sub-package!) ✓
//   ✗ OrderRepository.save(...)           → in repository package (different tree)

// ═══ Example 4: * as partial method name ═══
@Before("execution(* com.app.service.OrderService.get*(..))")
// Matches:
//   ✓ getOrder(Long)                      → starts with "get"
//   ✓ getAllOrders()                       → starts with "get"
//   ✗ createOrder(String, Double)         → starts with "create"
//   ✗ cancelOrder(Long)                   → starts with "cancel"

// ═══ Example 5: (*) one param of any type ═══
@Before("execution(* com.app.service.OrderService.*(*))")
// Matches:
//   ✓ getOrder(Long id)                   → exactly 1 param
//   ✓ cancelOrder(Long id)                → exactly 1 param
//   ✗ createOrder(String, Double)         → 2 params
//   ✗ getAllOrders()                       → 0 params
//   ✗ updateOrder(Long, OrderRequest)     → 2 params

// ═══ Example 6: (String, ..) first param String, rest anything ═══
@Before("execution(* com.app.service.OrderService.*(String, ..))")
// Matches:
//   ✓ createOrder(String product, Double amount)  → String + 1 more
//   ✗ getOrder(Long id)                           → first is Long, not String
//   ✗ getAllOrders()                               → no params at all
//   ✗ cancelOrder(Long id)                        → first is Long, not String

// ═══ Example 7: (*, *) exactly two params of any type ═══
@Before("execution(* com.app.service.OrderService.*(*, *))")
// Matches:
//   ✓ createOrder(String product, Double amount)  → exactly 2 params
//   ✓ updateOrder(Long id, OrderRequest req)      → exactly 2 params
//   ✗ getOrder(Long id)                           → 1 param
//   ✗ getAllOrders()                               → 0 params
//   ✗ cancelOrder(Long id)                        → 1 param
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Wildcard Usage — Where Can You Use Each?                                        │
│                                                                                  │
│  ┌──────────────────┬──────────────────┬─────────────────────────────────────────┐│
│  │ Position         │ * allowed?       │ .. allowed?                             ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Return type      │ ✓ YES            │ ✗ NO                                   ││
│  │                  │ execution(* ...) │ (.. is not a valid return type)         ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Package path     │ ✓ YES            │ ✓ YES                                  ││
│  │                  │ com.app.*        │ com.app..                               ││
│  │                  │ (one level)      │ (zero+ sub-package levels)              ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Class name       │ ✓ YES            │ ✗ NO                                   ││
│  │                  │ com.app.service.*│ (use .. in package path instead)        ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Method name      │ ✓ YES            │ ✗ NO                                   ││
│  │                  │ * or get* or *Or │ (.. is not valid for method names)      ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Parameters       │ ✓ YES            │ ✓ YES                                  ││
│  │                  │ (*) = one arg    │ (..) = zero or more args                ││
│  │                  │ (*, *) = two     │ (String, ..) = String + rest            ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Exception type   │ ✓ YES (rare)     │ ✗ NO                                   ││
│  │                  │ * in throws      │                                         ││
│  └──────────────────┴──────────────────┴─────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Complete Pointcut Expression Anatomy — Visual Breakdown

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dissecting a Real Pointcut Expression:                                          │
│                                                                                  │
│  execution(public com.app.entity.Order com.app.service.OrderService.createOrder  │
│            (String, Double) throws RuntimeException)                             │
│                                                                                  │
│  Let's break it down:                                                            │
│                                                                                  │
│  execution(                                                                      │
│    public                          ← modifier (optional, rarely used)            │
│    com.app.entity.Order            ← return type (fully qualified)               │
│    com.app.service.OrderService    ← declaring type (package + class)            │
│    .createOrder                    ← method name                                 │
│    (String, Double)                ← parameter types (exact match)               │
│    throws RuntimeException         ← exception (optional, rarely used)           │
│  )                                                                               │
│                                                                                  │
│  ═══════════════════════════════════════════════════════════════════════════════  │
│                                                                                  │
│  Same method matched with WILDCARDS:                                             │
│                                                                                  │
│  Most specific → Most general:                                                   │
│                                                                                  │
│  1. execution(public com.app.entity.Order                                        │
│               com.app.service.OrderService.createOrder(String, Double)            │
│               throws RuntimeException)                                           │
│     → Matches only THIS exact method signature.                                  │
│                                                                                  │
│  2. execution(* com.app.service.OrderService.createOrder(String, Double))         │
│     → Any return type, specific class + method + params.                         │
│                                                                                  │
│  3. execution(* com.app.service.OrderService.createOrder(..))                     │
│     → Any return type, specific class + method, any params.                      │
│                                                                                  │
│  4. execution(* com.app.service.OrderService.*(..))                               │
│     → Any method in OrderService.                                                │
│                                                                                  │
│  5. execution(* com.app.service.*.*(..))                                          │
│     → Any method in any class in the service package.                            │
│                                                                                  │
│  6. execution(* com.app.service..*.*(..))                                         │
│     → Any method in service package + all sub-packages.                          │
│                                                                                  │
│  7. execution(* com.app..*.*(..))                                                 │
│     → Any method in the entire application.                                      │
│                                                                                  │
│  8. execution(* *(..))                                                            │
│     → ANY method ANYWHERE. (Never use — way too broad!)                          │
│                                                                                  │
│  Specificity spectrum:                                                           │
│                                                                                  │
│    MOST SPECIFIC ◄────────────────────────────────────► MOST GENERAL             │
│    (1)                                                  (8)                       │
│    Matches 1 method                           Matches ALL methods                │
│    in 1 class                                 in ALL classes                      │
│                                                                                  │
│  Best practice: Be as SPECIFIC as possible.                                      │
│  Overly broad pointcuts cause unexpected interception and performance issues.    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Quick Reference — Common Real-World Pointcut Patterns:**

```java
// ─── Most Common Patterns You'll Use in Real Projects ───

// 1. All service methods (most common)
@Around("execution(* com.app.service.*.*(..))")

// 2. All service methods including sub-packages
@Around("execution(* com.app.service..*.*(..))")

// 3. All repository methods
@Around("execution(* com.app.repository.*.*(..))")

// 4. All controller methods
@Before("execution(* com.app.controller.*.*(..))")

// 5. Specific class, all methods
@Before("execution(* com.app.service.OrderService.*(..))")

// 6. Specific method in specific class
@Before("execution(* com.app.service.OrderService.createOrder(..))")

// 7. All getter methods in services
@Before("execution(* com.app.service.*.get*(..))")

// 8. All void methods (fire-and-forget operations)
@After("execution(void com.app.service.*.*(..))")

// 9. Methods that return a specific entity
@AfterReturning(
    pointcut = "execution(com.app.entity.Order com.app.service.*.*(..))",
    returning = "order"
)
public void afterOrderReturned(JoinPoint jp, Order order) { }

// 10. Methods taking a specific DTO
@Before("execution(* com.app.service.*.*(com.app.dto.OrderRequest, ..))")

// 11. All methods across the entire application (use with caution!)
@Around("execution(* com.app..*.*(..))")
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut Expression — Complete Syntax Cheat Sheet:                              │
│                                                                                  │
│  execution( [modifiers]  return-type  [declaring-type.]method-name(params)       │
│             [throws exception-type] )                                            │
│                                                                                  │
│  ┌───────────┬───────────────────────────────────────────────────────────────────┐│
│  │ Symbol    │ Meaning & Usage                                                   ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ *         │ IN RETURN TYPE: any return type                                   ││
│  │           │ IN CLASS NAME: any single class                                   ││
│  │           │ IN METHOD NAME: any single method (or partial: get*, *Order)      ││
│  │           │ IN PARAMS: exactly one parameter of any type                      ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ ..        │ IN PACKAGE: this package + all sub-packages recursively           ││
│  │           │ IN PARAMS: zero or more parameters of any type                    ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ ()        │ Exactly zero parameters (no arguments)                            ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (..)      │ Any number of parameters of any type (most common)                ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (*)       │ Exactly one parameter of any type                                 ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (T)       │ Exactly one parameter of type T                                   ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (T, ..)   │ First param is T, then zero or more of any type                   ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (.., T)   │ Zero or more of any type, last param is T                         ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (T, U)    │ Exactly two params: first is T, second is U                       ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ &&        │ Combine pointcuts with AND                                        ││
│  │ ||        │ Combine pointcuts with OR                                         ││
│  │ !         │ Negate a pointcut (NOT)                                           ││
│  └───────────┴───────────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  REMEMBER:                                                                       │
│    • Only return-type and method-name(params) are REQUIRED                       │
│    • * matches ONE thing, .. matches ZERO OR MORE things                         │
│    • Use fully qualified names for custom types (com.app.entity.Order)           │
│    • java.lang types can be used without package (String, Integer, etc.)          │
│    • Generics are erased: List<Order> and List<String> both match "java.util.List"│
│    • Primitive ≠ Wrapper: int ≠ Integer, long ≠ Long                            │
│    • Argument ORDER matters: (String, Long) ≠ (Long, String)                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

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

### Named Pointcuts — Reusable Pointcut Definitions

A **named pointcut** is a pointcut expression defined once using `@Pointcut` and given a **name** (the method name). You can then reference it by name from any advice annotation, avoiding duplication and improving readability.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Named Pointcuts — Why?                                                          │
│                                                                                  │
│  WITHOUT named pointcuts (duplicated expressions):                               │
│                                                                                  │
│    @Before("execution(* com.app.service.*.*(..)) && " +                          │
│            "!execution(* com.app.service.*.get*(..))")                            │
│    public void logBefore(...) { }                                                │
│                                                                                  │
│    @AfterReturning("execution(* com.app.service.*.*(..)) && " +                  │
│                    "!execution(* com.app.service.*.get*(..))")   ← SAME!         │
│    public void logAfter(...) { }                                                 │
│                                                                                  │
│    @AfterThrowing("execution(* com.app.service.*.*(..)) && " +                   │
│                   "!execution(* com.app.service.*.get*(..))")    ← SAME!         │
│    public void logError(...) { }                                                 │
│                                                                                  │
│    Problem: Expression repeated 3 times. If package changes → update 3 places.   │
│                                                                                  │
│  WITH named pointcuts (DRY):                                                     │
│                                                                                  │
│    @Pointcut("execution(* com.app.service.*.*(..)) && " +                        │
│              "!execution(* com.app.service.*.get*(..))")                          │
│    public void serviceMutations() {}   ← Defined ONCE, named "serviceMutations"  │
│                                                                                  │
│    @Before("serviceMutations()")       ← Reference by NAME                       │
│    public void logBefore(...) { }                                                │
│                                                                                  │
│    @AfterReturning("serviceMutations()")  ← Reference by NAME                   │
│    public void logAfter(...) { }                                                 │
│                                                                                  │
│    @AfterThrowing("serviceMutations()")   ← Reference by NAME                   │
│    public void logError(...) { }                                                 │
│                                                                                  │
│    If package changes → update 1 place (the @Pointcut definition).               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 1: Basic named pointcuts within the same Aspect ═══
@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    // ─── Named Pointcut Definitions ───
    // (Method body is always empty — it's just a name holder)

    @Pointcut("execution(* com.app.service.*.*(..))")
    public void serviceLayer() {}
    //          ↑ NAME = "serviceLayer"

    @Pointcut("execution(* com.app.service.*.get*(..))")
    public void getterMethods() {}
    //          ↑ NAME = "getterMethods"

    @Pointcut("serviceLayer() && !getterMethods()")
    public void serviceMutations() {}
    //          ↑ COMBINED pointcut — service methods except getters

    // ─── Advice methods referencing named pointcuts ───

    @Before("serviceMutations()")  // ← reference by name
    public void logBefore(JoinPoint jp) {
        log.info(">>> MUTATION: {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }

    @AfterReturning(pointcut = "serviceMutations()", returning = "result")
    public void logAfterSuccess(JoinPoint jp, Object result) {
        log.info("<<< MUTATION SUCCESS: {}.{}() → {}",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName(), result);
    }

    @AfterThrowing(pointcut = "serviceMutations()", throwing = "ex")
    public void logAfterError(JoinPoint jp, Exception ex) {
        log.error("!!! MUTATION FAILED: {}.{}() → {}",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName(), ex.getMessage());
    }

    // Separate advice for getters (different logging behavior)
    @Before("getterMethods()")
    public void logGetter(JoinPoint jp) {
        log.debug("QUERY: {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
```

```java
// ═══ Example 2: Named pointcuts with combinations ═══
@Aspect
@Component
@Slf4j
public class ApplicationLayerAspect {

    // ─── Layer-based pointcuts ───
    @Pointcut("within(com.app.controller.*)")
    public void controllerLayer() {}

    @Pointcut("within(com.app.service.*)")
    public void serviceLayer() {}

    @Pointcut("within(com.app.repository.*)")
    public void repositoryLayer() {}

    // ─── Combined pointcuts ───
    @Pointcut("serviceLayer() || repositoryLayer()")
    public void dataAccessLayer() {}  // service OR repository

    @Pointcut("controllerLayer() || serviceLayer()")
    public void webLayer() {}  // controller OR service

    @Pointcut("controllerLayer() || serviceLayer() || repositoryLayer()")
    public void anyLayer() {}  // all three layers

    // ─── Operation-type pointcuts ───
    @Pointcut("execution(* *.create*(..)) || execution(* *.save*(..)) || " +
              "execution(* *.update*(..)) || execution(* *.delete*(..))")
    public void mutationOperations() {}

    @Pointcut("execution(* *.get*(..)) || execution(* *.find*(..)) || " +
              "execution(* *.list*(..)) || execution(* *.search*(..))")
    public void queryOperations() {}

    // ─── Highly specific combined pointcuts ───
    @Pointcut("serviceLayer() && mutationOperations()")
    public void serviceMutations() {}  // mutations in service layer only

    @Pointcut("dataAccessLayer() && queryOperations()")
    public void dataQueries() {}  // queries in service or repository

    // ─── Advices using named pointcuts ───
    @Around("serviceMutations()")
    public Object logMutation(ProceedingJoinPoint pjp) throws Throwable {
        log.info("MUTATION >>> {}.{}()",
            pjp.getTarget().getClass().getSimpleName(),
            pjp.getSignature().getName());
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        log.info("MUTATION <<< completed in {}ms", System.currentTimeMillis() - start);
        return result;
    }

    @Before("dataQueries()")
    public void logQuery(JoinPoint jp) {
        log.debug("QUERY >>> {}.{}()",
            jp.getTarget().getClass().getSimpleName(),
            jp.getSignature().getName());
    }
}
```

```java
// ═══ Example 3: Shared pointcuts — reuse across multiple Aspects ═══

// Dedicated class for shared pointcut definitions
// (No @Aspect needed if it's ONLY defining pointcuts to share)
@Aspect  // @Aspect is needed for @Pointcut methods to be recognized
@Component
public class SharedPointcuts {

    // These pointcuts can be referenced from ANY other Aspect

    @Pointcut("within(com.app.service.*)")
    public void serviceLayer() {}

    @Pointcut("within(com.app.controller.*)")
    public void controllerLayer() {}

    @Pointcut("within(com.app.repository.*)")
    public void repositoryLayer() {}

    @Pointcut("@annotation(com.app.annotation.Loggable)")
    public void loggableMethods() {}

    @Pointcut("@annotation(com.app.annotation.Auditable)")
    public void auditableMethods() {}

    @Pointcut("execution(* *.get*(..)) || execution(* *.find*(..))")
    public void readOperations() {}

    @Pointcut("!readOperations()")
    public void writeOperations() {}
}

// Another Aspect references shared pointcuts using FULL CLASS PATH
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("com.app.aspect.SharedPointcuts.serviceLayer() && " +
            "com.app.aspect.SharedPointcuts.loggableMethods()")
    //       ↑ full class path + method name
    public Object logServiceLoggable(ProceedingJoinPoint pjp) throws Throwable {
        log.info(">>> {}", pjp.getSignature().toShortString());
        Object result = pjp.proceed();
        log.info("<<< {} → {}", pjp.getSignature().toShortString(), result);
        return result;
    }
}

// Yet another Aspect reuses the same shared pointcuts
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Around("com.app.aspect.SharedPointcuts.auditableMethods() && " +
            "com.app.aspect.SharedPointcuts.writeOperations()")
    public Object auditWrites(ProceedingJoinPoint pjp) throws Throwable {
        log.info("AUDIT: {} by user={}",
            pjp.getSignature().toShortString(),
            SecurityContextHolder.getContext().getAuthentication().getName());
        return pjp.proceed();
    }
}

// And another Aspect reuses them too
@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    @Around("com.app.aspect.SharedPointcuts.serviceLayer()")
    public Object measurePerformance(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long duration = System.currentTimeMillis() - start;
        if (duration > 500) {
            log.warn("SLOW: {} took {}ms", pjp.getSignature().toShortString(), duration);
        }
        return result;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Cross-Aspect Pointcut Reference:                                                │
│                                                                                  │
│  Same Aspect:                                                                    │
│    @Before("serviceLayer()")                                                     │
│    → Just use the method name directly                                           │
│                                                                                  │
│  Different Aspect:                                                               │
│    @Before("com.app.aspect.SharedPointcuts.serviceLayer()")                      │
│             └──────────────────────────────┘ └─────────────┘                     │
│                   full class path              method name                        │
│    → Must use fully qualified class name + method name                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Example 4: Named pointcuts with parameter binding ═══
@Aspect
@Component
@Slf4j
public class AnnotationBindingAspect {

    // Named pointcut that binds annotation
    @Pointcut("@annotation(auditable)")
    public void auditableMethod(Auditable auditable) {}
    //                          ↑ parameter name must match

    // Named pointcut that binds argument
    @Pointcut("args(orderRequest, ..)")
    public void orderRequestArg(OrderRequest orderRequest) {}

    // Advice using named pointcut WITH parameter binding
    @Around("auditableMethod(auditable)")
    public Object auditMethod(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        log.info("AUDIT [{}]: {}", auditable.action(), pjp.getSignature().toShortString());
        Object result = pjp.proceed();
        log.info("AUDIT [{}]: completed", auditable.action());
        return result;
    }

    // Another advice with argument binding
    @Before("orderRequestArg(orderRequest) && within(com.app.service.*)")
    public void validateOrder(JoinPoint jp, OrderRequest orderRequest) {
        log.info("Validating order: product={}, amount={}",
            orderRequest.getProduct(), orderRequest.getAmount());
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Named Pointcuts — Complete Summary:                                             │
│                                                                                  │
│  What:     A reusable, named pointcut expression defined with @Pointcut          │
│  Why:      DRY — define once, reuse in multiple advice methods                   │
│  How:      @Pointcut on an empty method; reference by method name                │
│                                                                                  │
│  Rules:                                                                          │
│  ┌───────────────────────────────────────────────────────────────────────────────┐│
│  │ 1. @Pointcut method body must be EMPTY (never called directly)               ││
│  │ 2. Method can be public, protected, or private                               ││
│  │    - private: only within the SAME Aspect class                              ││
│  │    - public:  accessible from OTHER Aspect classes                           ││
│  │ 3. Same-Aspect reference: just use method name: serviceLayer()               ││
│  │ 4. Cross-Aspect reference: use full path: com.app.aspect.X.serviceLayer()    ││
│  │ 5. Named pointcuts can reference OTHER named pointcuts                       ││
│  │ 6. Can combine with &&, ||, ! just like inline expressions                   ││
│  │ 7. Can bind parameters (annotation, args) through method params              ││
│  └───────────────────────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  Best Practice — Shared Pointcuts Architecture:                                  │
│                                                                                  │
│  ┌──────────────────────────────┐                                                │
│  │  SharedPointcuts.java        │ ← Central pointcut definitions                 │
│  │  • serviceLayer()            │                                                │
│  │  • controllerLayer()         │                                                │
│  │  • repositoryLayer()         │                                                │
│  │  • readOperations()          │                                                │
│  │  • writeOperations()         │                                                │
│  │  • loggableMethods()         │                                                │
│  └──────────────┬───────────────┘                                                │
│                 │                                                                 │
│       ┌─────────┼──────────┬──────────┐                                          │
│       v         v          v          v                                          │
│  ┌──────────┐ ┌────────┐ ┌────────┐ ┌──────────┐                                │
│  │ Logging  │ │ Audit  │ │ Perf   │ │ Security │                                │
│  │ Aspect   │ │ Aspect │ │ Aspect │ │ Aspect   │                                │
│  │          │ │        │ │        │ │          │                                │
│  │ Uses:    │ │ Uses:  │ │ Uses:  │ │ Uses:    │                                │
│  │ service  │ │ audit  │ │ service│ │ write    │                                │
│  │ Layer()  │ │ able() │ │ Layer()│ │ Ops()    │                                │
│  └──────────┘ └────────┘ └────────┘ └──────────┘                                │
│                                                                                  │
│  Benefits:                                                                       │
│    ✓ Package changes? Update 1 file (SharedPointcuts)                            │
│    ✓ New layer? Add 1 pointcut, all Aspects can use it                           │
│    ✓ Self-documenting: "serviceMutations()" reads better than                    │
│      "execution(* com.app.service.*.*(..)) && !execution(* *.get*(..))"          │
│    ✓ Composable: build complex pointcuts from simple building blocks             │
│    ✓ Testable: pointcut names clarify what's being intercepted                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## How AOP Works Internally — Step by Step from Application Startup

This section traces the **complete lifecycle** of Spring AOP — from the moment your Spring Boot application starts, through aspect discovery, pointcut parsing, bean scanning, eligibility matching, proxy creation, and finally method interception at runtime.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  AOP Internal Lifecycle — High-Level Overview (7 Steps):                         │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │  STARTUP PHASE (happens ONCE at application boot)                           │ │
│  │                                                                             │ │
│  │  Step 1: Spring Boot App Starts                                             │ │
│  │    ↓                                                                        │ │
│  │  Step 2: @EnableAspectJAutoProxy activates AOP infrastructure               │ │
│  │    ↓                                                                        │ │
│  │  Step 3: Discover @Aspect classes and extract @Pointcut/@Before/etc.        │ │
│  │    ↓                                                                        │ │
│  │  Step 4: Parse pointcut expressions with PointcutParser                     │ │
│  │    ↓                                                                        │ │
│  │  Step 5: Store parsed pointcuts in efficient cache (PointcutExpression)      │ │
│  │    ↓                                                                        │ │
│  │  Step 6: For EACH bean → check if any pointcut matches its methods          │ │
│  │    ↓                                                                        │ │
│  │  Step 7: If match found → create Proxy (JDK Dynamic or CGLIB)              │ │
│  │    ↓                                                                        │ │
│  │  Register PROXY (not original bean) in ApplicationContext                   │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │  RUNTIME PHASE (happens on EVERY method call)                               │ │
│  │                                                                             │ │
│  │  Client calls proxy.method()                                                │ │
│  │    ↓                                                                        │ │
│  │  Proxy looks up cached interceptor chain for this method                    │ │
│  │    ↓                                                                        │ │
│  │  Execute advice chain (@Before → proceed → @After/@AfterReturning)          │ │
│  │    ↓                                                                        │ │
│  │  Return result to client                                                    │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 1: Spring Boot Application Starts

When you run `SpringApplication.run(MyApp.class)`, the following happens:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @SpringBootApplication                                                          │
│  public class MyApp {                                                            │
│      public static void main(String[] args) {                                    │
│          SpringApplication.run(MyApp.class, args);    ← ENTRY POINT             │
│      }                                                                           │
│  }                                                                               │
│                                                                                  │
│  What happens internally:                                                        │
│                                                                                  │
│  1. SpringApplication.run()                                                      │
│     ↓                                                                            │
│  2. Creates AnnotationConfigApplicationContext                                    │
│     ↓                                                                            │
│  3. Scans for @Configuration, @Component, @Service, @Controller, etc.            │
│     ↓                                                                            │
│  4. Processes @EnableAspectJAutoProxy (auto-included via spring-boot-starter-aop) │
│     ↓                                                                            │
│  5. Registers AnnotationAwareAspectJAutoProxyCreator as a BeanPostProcessor      │
│     ↓                                                                            │
│  6. For EACH bean being created → this BeanPostProcessor checks for AOP          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── @SpringBootApplication includes @EnableAutoConfiguration ───
// Spring Boot auto-configures AOP via AopAutoConfiguration.class

// What @EnableAspectJAutoProxy does internally:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AspectJAutoProxyRegistrar.class)   // ← THIS registers the key bean
public @interface EnableAspectJAutoProxy {
    boolean proxyTargetClass() default false;  // false = JDK proxy, true = CGLIB
    boolean exposeProxy() default false;
}

// AspectJAutoProxyRegistrar registers:
//   AnnotationAwareAspectJAutoProxyCreator
//   ↑ This is a BeanPostProcessor — it intercepts every bean creation
```

**The key class: `AnnotationAwareAspectJAutoProxyCreator`**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Class Hierarchy of the AOP Auto-Proxy Creator:                                  │
│                                                                                  │
│  BeanPostProcessor (interface)                                                   │
│    └── AbstractAutoProxyCreator                                                  │
│          └── AbstractAdvisorAutoProxyCreator                                     │
│                └── AspectJAwareAdvisorAutoProxyCreator                            │
│                      └── AnnotationAwareAspectJAutoProxyCreator   ← THE ONE      │
│                                                                                  │
│  This class:                                                                     │
│    • Is a BeanPostProcessor → called for EVERY bean being created                │
│    • Knows how to find @Aspect classes                                            │
│    • Knows how to parse AspectJ pointcut expressions                             │
│    • Knows how to create JDK/CGLIB proxies                                       │
│    • Decides whether each bean needs a proxy or not                              │
│                                                                                  │
│  BeanPostProcessor has 2 methods:                                                │
│    postProcessBeforeInitialization(bean, name) → called BEFORE @PostConstruct    │
│    postProcessAfterInitialization(bean, name)  → called AFTER @PostConstruct     │
│                                                                                  │
│  AOP proxy creation happens in postProcessAfterInitialization()                  │
│  (after the bean is fully initialized but before it's put in the context)        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Simplified view of what AnnotationAwareAspectJAutoProxyCreator does:

public class AnnotationAwareAspectJAutoProxyCreator extends AbstractAutoProxyCreator {

    // Called for EVERY bean after it's fully initialized
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean != null) {
            Object cacheKey = getCacheKey(bean.getClass(), beanName);

            // Check if this bean should be proxied
            if (!this.earlyProxyReferences.contains(cacheKey)) {
                return wrapIfNecessary(bean, beanName, cacheKey);
                //      ↑ THIS is where the magic happens
            }
        }
        return bean;
    }

    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // 1. Find all Advisors (aspects + pointcuts) that apply to this bean
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(
            bean.getClass(), beanName, null
        );

        if (specificInterceptors != DO_NOT_PROXY) {
            // 2. Create a proxy wrapping the original bean
            Object proxy = createProxy(
                bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean)
            );

            // 3. Return proxy INSTEAD of original bean
            return proxy;
        }

        // No matching pointcuts → return original bean (no proxy)
        return bean;
    }
}
```

---

### Step 2: Discover @Aspect Classes and Extract Advisors

Once the `AnnotationAwareAspectJAutoProxyCreator` is active, it scans the ApplicationContext for all beans annotated with `@Aspect`.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Aspect Discovery Flow:                                                          │
│                                                                                  │
│  ApplicationContext                                                               │
│    │                                                                             │
│    ├── OrderService (bean) ─── @Service                                          │
│    ├── PaymentService (bean) ─── @Service                                        │
│    ├── OrderController (bean) ─── @RestController                                │
│    ├── LoggingAspect (bean) ─── @Aspect @Component    ← FOUND!                  │
│    ├── PerformanceAspect (bean) ─── @Aspect @Component ← FOUND!                 │
│    ├── SecurityAspect (bean) ─── @Aspect @Component    ← FOUND!                 │
│    └── ...                                                                       │
│                                                                                  │
│  For EACH @Aspect class, extract:                                                │
│    1. All @Before methods → becomes a "BeforeAdvice" advisor                     │
│    2. All @After methods → becomes an "AfterAdvice" advisor                      │
│    3. All @Around methods → becomes an "AroundAdvice" advisor                    │
│    4. All @AfterReturning methods → becomes "AfterReturningAdvice" advisor       │
│    5. All @AfterThrowing methods → becomes "AfterThrowingAdvice" advisor         │
│    6. All @Pointcut methods → stored as named pointcut definitions               │
│                                                                                  │
│  Each Advisor = Pointcut Expression + Advice Method                              │
│                                                                                  │
│  Example:                                                                        │
│    @Before("execution(* com.app.service.*.*(..))")                               │
│    public void logBefore(JoinPoint jp) { ... }                                   │
│                                                                                  │
│    → Advisor {                                                                   │
│        pointcut: "execution(* com.app.service.*.*(..))"                          │
│        advice:   logBefore method reference                                      │
│        type:     BEFORE                                                          │
│      }                                                                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── The actual Spring class that discovers Aspects ───
// BeanFactoryAspectJAdvisorsBuilder.java (simplified)

public class BeanFactoryAspectJAdvisorsBuilder {

    public List<Advisor> buildAspectJAdvisors() {
        // 1. Get ALL bean names from the ApplicationContext
        String[] beanNames = this.beanFactory.getBeanNamesForType(Object.class);

        List<Advisor> advisors = new ArrayList<>();

        for (String beanName : beanNames) {
            Class<?> beanType = this.beanFactory.getType(beanName);

            // 2. Check: does this class have @Aspect annotation?
            if (this.advisorFactory.isAspect(beanType)) {
                //  ↑ Uses AnnotationUtils.findAnnotation(beanType, Aspect.class)

                AspectMetadata metadata = new AspectMetadata(beanType, beanName);

                // 3. Extract all advisor methods from this @Aspect class
                List<Advisor> classAdvisors = this.advisorFactory
                    .getAdvisors(new BeanFactoryAspectInstanceFactory(
                        this.beanFactory, beanName));

                advisors.addAll(classAdvisors);
            }
        }

        return advisors;  // All advisors from ALL @Aspect beans
    }
}
```

```java
// ─── How each @Before/@After/@Around method becomes an Advisor ───
// ReflectiveAspectJAdvisorFactory.java (simplified)

public class ReflectiveAspectJAdvisorFactory {

    public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory factory) {
        Class<?> aspectClass = factory.getAspectMetadata().getAspectClass();
        List<Advisor> advisors = new ArrayList<>();

        // Get all methods from the @Aspect class
        for (Method method : getAdvisorMethods(aspectClass)) {

            // Check for advice annotations: @Before, @After, @Around, etc.
            Advisor advisor = getAdvisor(method, factory);

            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        return advisors;
    }

    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
        List<Method> methods = new ArrayList<>();
        ReflectionUtils.doWithMethods(aspectClass, methods::add,
            // Filter: only methods with AOP annotations
            method -> AnnotationUtils.getAnnotation(method, Pointcut.class) == null
                // Skip @Pointcut methods — they are just definitions, not advice
        );

        // Sort by annotation type order: @Around → @Before → @After →
        //                                 @AfterReturning → @AfterThrowing
        methods.sort(METHOD_COMPARATOR);
        return methods;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Advisor Discovery — What Gets Created:                                          │
│                                                                                  │
│  From LoggingAspect:                                                             │
│  ┌───────────────────────────────────────────────────────────────────────────┐   │
│  │ @Aspect @Component                                                        │   │
│  │ public class LoggingAspect {                                              │   │
│  │                                                                           │   │
│  │   @Pointcut("execution(* com.app.service.*.*(..))")                       │   │
│  │   public void serviceLayer() {}                                           │   │
│  │                       ↓ stored as named pointcut definition               │   │
│  │                                                                           │   │
│  │   @Before("serviceLayer()")                                               │   │
│  │   public void logBefore(JoinPoint jp) { ... }                             │   │
│  │                       ↓ Advisor #1 { pointcut + beforeAdvice }            │   │
│  │                                                                           │   │
│  │   @AfterReturning(pointcut = "serviceLayer()", returning = "r")           │   │
│  │   public void logAfter(JoinPoint jp, Object r) { ... }                    │   │
│  │                       ↓ Advisor #2 { pointcut + afterReturningAdvice }    │   │
│  │                                                                           │   │
│  │   @Around("@annotation(com.app.annotation.Timed)")                        │   │
│  │   public Object time(ProceedingJoinPoint pjp) { ... }                     │   │
│  │                       ↓ Advisor #3 { pointcut + aroundAdvice }            │   │
│  └───────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Total Advisors collected: [Advisor#1, Advisor#2, Advisor#3, ...]                │
│  These are stored in memory and reused for every bean check.                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 3: Parse Pointcut Expressions with PointcutParser

Each advice annotation contains a **pointcut expression string** like `"execution(* com.app.service.*.*(..))"`. This string must be **parsed** into a structured, executable object that Spring can use to match methods.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut Parsing — What Happens:                                                │
│                                                                                  │
│  Input (String):                                                                 │
│    "execution(* com.app.service.*.*(..)) && @annotation(Loggable)"              │
│                                                                                  │
│  Parsing Steps:                                                                  │
│    1. Tokenize the string into parts                                             │
│    2. Identify designator types (execution, @annotation, within, etc.)           │
│    3. Parse each designator's pattern                                            │
│    4. Parse boolean operators (&&, ||, !)                                         │
│    5. Build an Abstract Syntax Tree (AST)                                        │
│    6. Create a PointcutExpression object                                          │
│                                                                                  │
│  Output (Object):                                                                │
│    PointcutExpression {                                                           │
│      type: AND                                                                   │
│      left: ExecutionPointcut {                                                   │
│        returnType: *                                                             │
│        declaringType: com.app.service.*                                          │
│        methodName: *                                                             │
│        params: (..)                                                              │
│      }                                                                           │
│      right: AnnotationPointcut {                                                 │
│        annotation: Loggable                                                      │
│      }                                                                           │
│    }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**The actual parser class: `PointcutParser`**

```java
// ─── org.aspectj.weaver.tools.PointcutParser ───
// This is from the AspectJ library (NOT Spring — Spring uses AspectJ's parser)

// Simplified view of how Spring invokes the parser:
public class AspectJExpressionPointcut implements Pointcut, ClassFilter, MethodMatcher {

    private String expression;  // "execution(* com.app.service.*.*(..))"

    private PointcutExpression pointcutExpression;  // Parsed result
    private Class<?> pointcutDeclarationScope;
    private String[] pointcutParameterNames;
    private Class<?>[] pointcutParameterTypes;

    // Lazy parsing — only parsed when first needed
    private PointcutExpression obtainPointcutExpression() {
        if (this.pointcutExpression == null) {
            // 1. Get a PointcutParser instance
            PointcutParser parser = PointcutParser.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(
                SUPPORTED_PRIMITIVES,        // execution, within, @annotation, args, etc.
                this.getClass().getClassLoader()
            );

            // 2. Parse the expression string into a PointcutExpression object
            this.pointcutExpression = parser.parsePointcutExpression(
                replaceBooleanOperators(this.expression),
                //  ↑ Replaces "and" → "&&", "or" → "||", "not" → "!"
                this.pointcutDeclarationScope,
                new PointcutParameter[0]
            );
        }
        return this.pointcutExpression;
    }
}
```

```java
// ─── PointcutParser.java (from AspectJ library) — Simplified ───
// Package: org.aspectj.weaver.tools

public class PointcutParser {

    // Supported pointcut designator types
    private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = Set.of(
        PointcutPrimitive.EXECUTION,        // execution()
        PointcutPrimitive.WITHIN,           // within()
        PointcutPrimitive.AT_WITHIN,        // @within()
        PointcutPrimitive.AT_ANNOTATION,    // @annotation()
        PointcutPrimitive.ARGS,             // args()
        PointcutPrimitive.AT_ARGS,          // @args()
        PointcutPrimitive.TARGET,           // target()
        PointcutPrimitive.THIS,             // this()
        PointcutPrimitive.REFERENCE         // named pointcut references
    );

    public PointcutExpression parsePointcutExpression(String expression) {
        // 1. TOKENIZE: break string into tokens
        //    "execution(* com.app.service.*.*(..))" → [EXECUTION, "(", PATTERN, ")"]

        // 2. BUILD AST: create Abstract Syntax Tree
        //    For compound expressions like "A && B || C":
        //
        //         OR
        //        /  \
        //      AND    C
        //     /   \
        //    A     B

        // 3. RESOLVE: resolve type references, validate patterns

        // 4. RETURN: PointcutExpression object with matches() methods
        return new PointcutExpressionImpl(/* parsed AST */, expression);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Parsing Example — Step by Step:                                                 │
│                                                                                  │
│  Input: "execution(* com.app.service.*.get*(..)) && !within(*.ReportService)"    │
│                                                                                  │
│  Step 1 — Tokenize:                                                              │
│    Token 1: EXECUTION_DESIGNATOR                                                 │
│    Token 2: "(" → open paren                                                     │
│    Token 3: "*" → return type wildcard                                           │
│    Token 4: "com.app.service.*" → declaring type pattern                         │
│    Token 5: "." → separator                                                      │
│    Token 6: "get*" → method name pattern                                         │
│    Token 7: "(..)" → any parameters                                              │
│    Token 8: ")" → close paren                                                    │
│    Token 9: "&&" → AND operator                                                  │
│    Token 10: "!" → NOT operator                                                  │
│    Token 11: WITHIN_DESIGNATOR                                                   │
│    Token 12: "(" → open paren                                                    │
│    Token 13: "*.ReportService" → type pattern                                    │
│    Token 14: ")" → close paren                                                   │
│                                                                                  │
│  Step 2 — Build AST:                                                             │
│                                                                                  │
│            AND (&&)                                                               │
│           /        \                                                             │
│    ExecutionPc   NOT (!)                                                          │
│    {               \                                                             │
│      ret: *      WithinPc                                                        │
│      cls: service.* {                                                            │
│      mtd: get*    cls: *.ReportService                                           │
│      prm: (..)  }                                                                │
│    }                                                                             │
│                                                                                  │
│  Step 3 — Create PointcutExpression:                                             │
│    The AST is wrapped in a PointcutExpression object that has:                   │
│    • couldMatchJoinPointsInType(Class) → fast class-level check                  │
│    • matchesMethodExecution(Method)    → precise method-level check              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 4: Store Parsed Pointcuts in Efficient Cache

Parsing a pointcut expression string is **expensive** (involves tokenization, AST building, type resolution). Spring parses each expression **only once** and caches the result.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut Caching — How Spring Avoids Re-Parsing:                                │
│                                                                                  │
│  FIRST TIME a pointcut is needed:                                                │
│    String "execution(* com.app.service.*.*(..))"                                 │
│      ↓ PointcutParser.parsePointcutExpression()                                  │
│    PointcutExpression object (parsed AST)                                        │
│      ↓ stored in AspectJExpressionPointcut.pointcutExpression field              │
│    CACHED ✓                                                                      │
│                                                                                  │
│  SUBSEQUENT TIMES the same pointcut is needed:                                   │
│    → Returns the cached PointcutExpression object instantly                      │
│    → No re-parsing needed                                                        │
│                                                                                  │
│  Additional caching layers:                                                      │
│    • BeanFactoryAspectJAdvisorsBuilder caches the list of all Advisors           │
│    • advisorCache: Map<String, List<Advisor>> — advisors per bean name           │
│    • advisedBeans: Map<Object, Boolean> — tracks which beans are already checked │
│    • proxyTypes: Map<Object, Class<?>> — caches proxy class types                │
│    • methodCache: Map<Method, List<Interceptor>> — interceptor chains per method │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Caching in AspectJExpressionPointcut ───
public class AspectJExpressionPointcut implements Pointcut, ClassFilter, MethodMatcher {

    // Parsed expression — computed ONCE, reused forever
    private volatile PointcutExpression pointcutExpression;

    // Method match cache — avoids re-evaluating for the same method
    private final Map<Method, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(32);

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        // Check cache first
        ShadowMatch shadowMatch = this.shadowMatchCache.get(method);

        if (shadowMatch == null) {
            // Not cached → compute and cache
            PointcutExpression pce = obtainPointcutExpression();
            shadowMatch = pce.matchesMethodExecution(method);
            this.shadowMatchCache.put(method, shadowMatch);
        }

        // Return cached result
        return shadowMatch.alwaysMatches();
    }
}
```

```java
// ─── Advisor-level caching in BeanFactoryAspectJAdvisorsBuilder ───
public class BeanFactoryAspectJAdvisorsBuilder {

    // Cache: list of all advisors from all @Aspect beans
    private volatile List<String> aspectBeanNames;      // names of @Aspect beans
    private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

    public List<Advisor> buildAspectJAdvisors() {
        List<String> aspectNames = this.aspectBeanNames;

        if (aspectNames == null) {
            // FIRST CALL: discover all @Aspect beans and build advisors
            synchronized (this) {
                aspectNames = this.aspectBeanNames;
                if (aspectNames == null) {
                    // ... discovery logic (shown in Step 2) ...
                    this.aspectBeanNames = aspectNames;  // Cache the list
                }
            }
        }

        // SUBSEQUENT CALLS: return from cache
        List<Advisor> advisors = new ArrayList<>();
        for (String aspectName : aspectNames) {
            List<Advisor> cached = this.advisorsCache.get(aspectName);
            if (cached != null) {
                advisors.addAll(cached);   // ← From cache, no re-parsing
            }
        }
        return advisors;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Complete Caching Architecture:                                                  │
│                                                                                  │
│  Level 1: Advisor Cache                                                          │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │ aspectBeanNames: ["loggingAspect", "perfAspect", "securityAspect"]        │  │
│  │ advisorsCache:                                                             │  │
│  │   "loggingAspect"  → [Advisor#1, Advisor#2]                               │  │
│  │   "perfAspect"     → [Advisor#3]                                          │  │
│  │   "securityAspect" → [Advisor#4, Advisor#5]                               │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Level 2: Pointcut Expression Cache                                              │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │ Each Advisor's AspectJExpressionPointcut caches:                           │  │
│  │   pointcutExpression: (parsed AST) — computed ONCE                        │  │
│  │   shadowMatchCache: {                                                      │  │
│  │     OrderService.createOrder → MATCH                                      │  │
│  │     OrderService.getOrder    → MATCH                                      │  │
│  │     PaymentService.process   → MATCH                                      │  │
│  │     OrderController.create   → NO_MATCH                                   │  │
│  │   }                                                                        │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Level 3: Per-Bean Advisor Cache (after proxy creation)                           │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │ advisedBeans: {                                                            │  │
│  │   "orderService"    → true   (has proxy)                                  │  │
│  │   "paymentService"  → true   (has proxy)                                  │  │
│  │   "reportService"   → false  (no proxy needed)                            │  │
│  │ }                                                                          │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Level 4: Method Interceptor Chain Cache (runtime)                               │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │ Inside the proxy:                                                          │  │
│  │ methodCache: {                                                             │  │
│  │   createOrder → [LoggingInterceptor, PerfInterceptor]                     │  │
│  │   getOrder    → [LoggingInterceptor]                                      │  │
│  │   cancelOrder → [LoggingInterceptor, SecurityInterceptor]                 │  │
│  │ }                                                                          │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 5: Finding @Component, @Service, @Controller Beans

As Spring creates each bean, the `AnnotationAwareAspectJAutoProxyCreator` (our BeanPostProcessor) is called **for every bean**. It doesn't care about what annotation the bean has — it checks **every** bean.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Bean Creation Flow (Spring's BeanFactory):                                      │
│                                                                                  │
│  For EACH bean definition found during component scanning:                       │
│                                                                                  │
│  1. Instantiate the bean (new OrderService())                                    │
│  2. Populate properties (@Autowired injection)                                   │
│  3. Call BeanPostProcessor.postProcessBeforeInitialization()                      │
│  4. Call @PostConstruct / InitializingBean.afterPropertiesSet()                   │
│  5. Call BeanPostProcessor.postProcessAfterInitialization()  ← AOP CHECKS HERE   │
│  6. Bean is ready → put in ApplicationContext                                    │
│                                                                                  │
│  Step 5 is where AnnotationAwareAspectJAutoProxyCreator runs.                    │
│  It checks: "Does any Advisor's pointcut match any method of this bean?"         │
│                                                                                  │
│  Beans checked:                                                                  │
│    @Service OrderService         → checked ✓                                     │
│    @Service PaymentService       → checked ✓                                     │
│    @RestController OrderController → checked ✓                                   │
│    @Component HealthChecker      → checked ✓                                     │
│    @Repository OrderRepository   → checked ✓                                     │
│    @Aspect LoggingAspect         → checked ✓ (but @Aspect itself is skipped)     │
│    @Configuration AppConfig      → checked ✓                                     │
│                                                                                  │
│  Note: @Aspect-annotated beans are SKIPPED for proxying                          │
│  (they are the ones creating proxies, not being proxied)                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── How Spring decides to skip @Aspect beans from being proxied ───
// AbstractAutoProxyCreator.java (simplified)

protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
    // Skip infrastructure beans (including @Aspect beans)
    if (isInfrastructureClass(bean.getClass())) {
        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;  // Return original — no proxy
    }
    // ...
}

// In AnnotationAwareAspectJAutoProxyCreator:
@Override
protected boolean isInfrastructureClass(Class<?> beanClass) {
    return super.isInfrastructureClass(beanClass) ||
           this.aspectJAdvisorFactory.isAspect(beanClass);
    //      ↑ Returns true if class has @Aspect annotation
    //        → These beans are NOT proxied
}
```

---

### Step 6: Check Each Bean's Eligibility for Pointcut Matching

For each non-infrastructure bean, Spring checks if **any** of the collected Advisors' pointcut expressions match **any** method of the bean. This is a **two-phase check**: first a fast class-level check, then precise method-level checks.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Eligibility Check — Two-Phase Matching:                                         │
│                                                                                  │
│  Phase 1: CLASS-LEVEL check (FAST — eliminates most beans)                       │
│    "Could this pointcut POSSIBLY match any method in this class?"                │
│                                                                                  │
│    Pointcut: execution(* com.app.service.*.*(..))                                │
│    Bean class: com.app.controller.OrderController                                │
│    → Package doesn't match → SKIP (no need to check methods)                    │
│                                                                                  │
│    Pointcut: execution(* com.app.service.*.*(..))                                │
│    Bean class: com.app.service.OrderService                                      │
│    → Package MATCHES → proceed to Phase 2                                        │
│                                                                                  │
│  Phase 2: METHOD-LEVEL check (precise — only for class-level matches)            │
│    For each method of the matched class, check the full pointcut:                │
│                                                                                  │
│    OrderService.createOrder(OrderRequest)  → MATCH ✓                             │
│    OrderService.getOrder(Long)             → MATCH ✓                             │
│    OrderService.getAllOrders()              → MATCH ✓                             │
│    OrderService.cancelOrder(Long)          → MATCH ✓                             │
│                                                                                  │
│  If ANY method matches → bean is eligible → CREATE PROXY                         │
│  If NO method matches → bean is NOT eligible → return original bean              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── The actual matching code ───
// AbstractAdvisorAutoProxyCreator.java (simplified)

protected Object[] getAdvicesAndAdvisorsForBean(
        Class<?> beanClass, String beanName, TargetSource targetSource) {

    // 1. Get ALL advisors (from all @Aspect beans)
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    //  → Returns: [LoggingAdvisor, PerfAdvisor, SecurityAdvisor, ...]

    // 2. Filter: which advisors match THIS bean?
    List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(
        candidateAdvisors, beanClass, beanName);

    if (eligibleAdvisors.isEmpty()) {
        return DO_NOT_PROXY;   // No advisors match → no proxy needed
    }

    return eligibleAdvisors.toArray();  // These will be wired into the proxy
}
```

```java
// ─── AopUtils.findAdvisorsThatCanApply() — The core matching logic ───
// org.springframework.aop.support.AopUtils

public static List<Advisor> findAdvisorsThatCanApply(
        List<Advisor> candidateAdvisors, Class<?> clazz) {

    List<Advisor> eligibleAdvisors = new ArrayList<>();

    for (Advisor candidate : candidateAdvisors) {
        if (canApply(candidate, clazz)) {
            eligibleAdvisors.add(candidate);
        }
    }

    return eligibleAdvisors;
}

public static boolean canApply(Advisor advisor, Class<?> targetClass) {
    Pointcut pointcut = advisor.getPointcut();

    // ─── PHASE 1: Class-level check (FAST) ───
    if (!pointcut.getClassFilter().matches(targetClass)) {
        return false;  // Class doesn't match → skip all methods
    }

    // ─── PHASE 2: Method-level check (PRECISE) ───
    MethodMatcher methodMatcher = pointcut.getMethodMatcher();

    // If the matcher says "match all methods" → no need to check individually
    if (methodMatcher == MethodMatcher.TRUE) {
        return true;
    }

    // Check each method of the target class
    Set<Class<?>> classes = new LinkedHashSet<>();
    if (!Proxy.isProxyClass(targetClass)) {
        classes.add(ClassUtils.getUserClass(targetClass));
    }
    classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));

    for (Class<?> clazz : classes) {
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
        for (Method method : methods) {
            if (methodMatcher.matches(method, targetClass)) {
                return true;  // At least ONE method matches → bean needs proxy
            }
        }
    }

    return false;  // No method matches → no proxy needed
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Eligibility Check — Walkthrough Example:                                        │
│                                                                                  │
│  Advisors collected: 3                                                           │
│    Advisor#1: execution(* com.app.service.*.*(..))     → @Before logBefore       │
│    Advisor#2: @annotation(com.app.annotation.Timed)    → @Around timeMethod      │
│    Advisor#3: execution(* com.app.controller.*.*(..))  → @Before logRequest      │
│                                                                                  │
│  Checking bean: OrderService (com.app.service.OrderService)                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │ Advisor#1: execution(* com.app.service.*.*(..))                         │     │
│  │   Phase 1: class com.app.service.OrderService → service.* → MATCH ✓   │     │
│  │   Phase 2: createOrder() → MATCH ✓ → ELIGIBLE                          │     │
│  │                                                                         │     │
│  │ Advisor#2: @annotation(Timed)                                           │     │
│  │   Phase 1: any class could have @Timed methods → PASS                   │     │
│  │   Phase 2: createOrder() → no @Timed → NO MATCH                        │     │
│  │            getOrder() → no @Timed → NO MATCH                            │     │
│  │            cancelOrder() → has @Timed → MATCH ✓ → ELIGIBLE              │     │
│  │                                                                         │     │
│  │ Advisor#3: execution(* com.app.controller.*.*(..))                      │     │
│  │   Phase 1: class com.app.service.OrderService → controller.* → FAIL ✗ │     │
│  │   → SKIP (no method check needed)                                       │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
│                                                                                  │
│  Result: OrderService gets Advisor#1 and Advisor#2 → CREATE PROXY               │
│                                                                                  │
│  Checking bean: ReportService (com.app.service.reporting.ReportService)          │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │ Advisor#1: execution(* com.app.service.*.*(..))                         │     │
│  │   Phase 1: com.app.service.reporting.ReportService                      │     │
│  │            Pattern: com.app.service.* (one level only)                  │     │
│  │            reporting sub-package → FAIL ✗ → SKIP                        │     │
│  │                                                                         │     │
│  │ Advisor#2: @annotation(Timed)                                           │     │
│  │   Phase 1: PASS  │  Phase 2: no @Timed methods → NO MATCH              │     │
│  │                                                                         │     │
│  │ Advisor#3: execution(* com.app.controller.*.*(..))                      │     │
│  │   Phase 1: FAIL ✗ → SKIP                                               │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
│                                                                                  │
│  Result: NO advisors match → NO PROXY → original bean registered                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 7: Creating Proxies — JDK Dynamic Proxy vs CGLIB Proxy

When a bean is eligible for AOP, Spring creates a **proxy** that wraps the original bean. The proxy intercepts method calls and executes the advice chain. Spring uses one of two proxy mechanisms:

- **JDK Dynamic Proxy** — Java's built-in `java.lang.reflect.Proxy`
- **CGLIB Proxy** — **C**ode **G**eneration **Lib**rary (creates a subclass at runtime)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JDK Dynamic Proxy vs CGLIB Proxy — How Spring Decides:                          │
│                                                                                  │
│  Decision Rule:                                                                  │
│                                                                                  │
│  Does the bean implement at least one interface?                                 │
│    │                                                                             │
│    ├── YES → Is proxyTargetClass = false? (default)                              │
│    │    │                                                                        │
│    │    ├── YES → Use JDK Dynamic Proxy                                          │
│    │    └── NO  → Use CGLIB Proxy                                                │
│    │                                                                             │
│    └── NO  → Use CGLIB Proxy (no choice — JDK proxy requires interface)          │
│                                                                                  │
│  Spring Boot Default (since 2.0):                                                │
│    spring.aop.proxy-target-class = true   ← CGLIB by default!                   │
│    This means: Spring Boot uses CGLIB for ALL beans by default.                  │
│    Even beans that implement interfaces get CGLIB proxies.                       │
│                                                                                  │
│  To force JDK proxies:                                                           │
│    spring.aop.proxy-target-class = false                                         │
│    OR: @EnableAspectJAutoProxy(proxyTargetClass = false)                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### JDK Dynamic Proxy

**Full Form:** JDK = Java Development Kit. It's Java's built-in proxy mechanism from `java.lang.reflect.Proxy`.

**How it works:** Creates a proxy class at runtime that **implements the same interface(s)** as the target bean. The proxy delegates all interface method calls through an `InvocationHandler`.

**Requirement:** The target bean **must implement at least one interface**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JDK Dynamic Proxy — Visual:                                                     │
│                                                                                  │
│  interface OrderProcessor {                                                      │
│    Order createOrder(OrderRequest req);                                          │
│    Order getOrder(Long id);                                                      │
│  }                                                                               │
│                                                                                  │
│  class OrderService implements OrderProcessor {                                  │
│    Order createOrder(OrderRequest req) { ... }                                   │
│    Order getOrder(Long id) { ... }                                               │
│    void internalHelper() { ... }    ← NOT in interface                           │
│  }                                                                               │
│                                                                                  │
│  JDK Proxy creates:                                                              │
│                                                                                  │
│  ┌───────────────────────────────────┐                                           │
│  │ $Proxy42 (generated class)        │                                           │
│  │ implements OrderProcessor         │ ← Same interface                          │
│  │                                   │                                           │
│  │ InvocationHandler handler;        │ ← Contains advice chain                   │
│  │                                   │                                           │
│  │ Order createOrder(OrderRequest r) │                                           │
│  │   → handler.invoke(this, method, args) │                                      │
│  │   → runs @Before advice           │                                           │
│  │   → calls target.createOrder(r)   │                                           │
│  │   → runs @After advice            │                                           │
│  │                                   │                                           │
│  │ Order getOrder(Long id)           │                                           │
│  │   → handler.invoke(this, method, args) │                                      │
│  │                                   │                                           │
│  │ ✗ internalHelper() NOT proxied    │ ← Not in interface!                       │
│  └───────────────────────────────────┘                                           │
│                                                                                  │
│  Limitations:                                                                    │
│    • Can ONLY proxy interface methods                                            │
│    • Methods not in any interface → NOT intercepted                              │
│    • Bean must implement at least one interface                                  │
│    • proxy instanceof OrderProcessor → true                                      │
│    • proxy instanceof OrderService   → FALSE!                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── What JDK Dynamic Proxy looks like internally ───
// java.lang.reflect.Proxy

// Spring creates the proxy like this (simplified):
public class JdkDynamicAopProxy implements InvocationHandler {

    private final Object target;             // The real OrderService bean
    private final List<Advisor> advisors;    // Matching advisors

    // Creating the proxy:
    public Object getProxy() {
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),  // [OrderProcessor.class]
            this  // InvocationHandler = this class
        );
    }

    // EVERY method call on the proxy goes through here:
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. Get the interceptor chain for this specific method
        List<Object> chain = getInterceptorsAndDynamicInterceptionAdvice(method);

        if (chain.isEmpty()) {
            // No advice for this method → call target directly
            return method.invoke(target, args);
        }

        // 2. Create a MethodInvocation and execute the chain
        MethodInvocation invocation = new ReflectiveMethodInvocation(
            proxy, target, method, args, target.getClass(), chain);

        return invocation.proceed();
    }
}
```

#### CGLIB Proxy

**Full Form:** CGLIB = **C**ode **G**eneration **Lib**rary. It generates a **subclass** of the target class at runtime using bytecode manipulation.

**How it works:** Creates a new class that **extends** the target bean's class. Overrides all non-final methods to add interception logic.

**Requirement:** The target class must **not be `final`** (cannot extend a final class). Methods must not be `final` either.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  CGLIB Proxy — Visual:                                                           │
│                                                                                  │
│  @Service                                                                        │
│  class OrderService {     ← No interface needed!                                 │
│    Order createOrder(OrderRequest req) { ... }                                   │
│    Order getOrder(Long id) { ... }                                               │
│    void internalHelper() { ... }                                                 │
│  }                                                                               │
│                                                                                  │
│  CGLIB creates:                                                                  │
│                                                                                  │
│  ┌───────────────────────────────────────┐                                       │
│  │ OrderService$$EnhancerBySpringCGLIB   │                                       │
│  │ extends OrderService                  │ ← SUBCLASS of target!                 │
│  │                                       │                                       │
│  │ MethodInterceptor[] callbacks;        │ ← Contains advice chain               │
│  │                                       │                                       │
│  │ @Override                             │                                       │
│  │ Order createOrder(OrderRequest r)     │                                       │
│  │   → callback.intercept(...)           │                                       │
│  │   → runs @Before advice              │                                       │
│  │   → calls super.createOrder(r)       │ ← calls PARENT class method           │
│  │   → runs @After advice               │                                       │
│  │                                       │                                       │
│  │ @Override                             │                                       │
│  │ Order getOrder(Long id)              │                                       │
│  │   → callback.intercept(...)           │                                       │
│  │                                       │                                       │
│  │ @Override                             │                                       │
│  │ void internalHelper()                │ ← Also proxied! (all methods)         │
│  │   → callback.intercept(...)           │                                       │
│  └───────────────────────────────────────┘                                       │
│                                                                                  │
│  Advantages over JDK:                                                            │
│    • No interface required                                                       │
│    • ALL methods can be proxied (not just interface methods)                      │
│    • proxy instanceof OrderService → TRUE!                                       │
│                                                                                  │
│  Limitations:                                                                    │
│    • Cannot proxy final classes (class OrderService final → ERROR)               │
│    • Cannot proxy final methods (final Order getOrder() → NOT intercepted)       │
│    • Cannot proxy private methods (private → NOT intercepted)                    │
│    • Slightly more memory than JDK proxy (generates a subclass)                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── What CGLIB Proxy looks like internally ───
// Spring's CglibAopProxy (simplified)

public class CglibAopProxy implements AopProxy {

    private final Object target;
    private final List<Advisor> advisors;

    public Object getProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target.getClass());   // extends OrderService
        enhancer.setCallback(new DynamicAdvisedInterceptor(this.advisors));
        return enhancer.create();  // Returns the CGLIB proxy instance
    }

    // Inner class that intercepts ALL method calls
    private static class DynamicAdvisedInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object proxy, Method method, Object[] args,
                                MethodProxy methodProxy) throws Throwable {

            // 1. Get the interceptor chain for this method
            List<Object> chain = getInterceptorsAndDynamicInterceptionAdvice(method);

            if (chain.isEmpty()) {
                // No advice → call original method directly
                return methodProxy.invoke(target, args);
            }

            // 2. Execute the advice chain
            CglibMethodInvocation invocation = new CglibMethodInvocation(
                proxy, target, method, args, target.getClass(), chain, methodProxy);

            return invocation.proceed();
        }
    }
}
```

#### JDK Dynamic Proxy vs CGLIB Proxy — Complete Comparison

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JDK Dynamic Proxy vs CGLIB — Comparison Table:                                  │
│                                                                                  │
│  ┌────────────────────────────┬───────────────────────┬──────────────────────────┐│
│  │ Feature                    │ JDK Dynamic Proxy     │ CGLIB Proxy              ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Full Name                  │ Java Development Kit  │ Code Generation Library  ││
│  │                            │ Dynamic Proxy         │                          ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Mechanism                  │ Implements interfaces │ Extends target class     ││
│  │                            │ at runtime            │ (creates subclass)       ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Interface Required?        │ YES — must implement  │ NO — works without       ││
│  │                            │ at least one          │ interfaces               ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Which methods proxied?     │ Only interface methods│ ALL non-final, non-      ││
│  │                            │                       │ private methods          ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ proxy instanceof Target?   │ FALSE (proxy is NOT   │ TRUE (proxy IS a         ││
│  │                            │ Target class type)    │ subclass of Target)      ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ proxy instanceof Interface?│ TRUE                  │ TRUE (inherits from      ││
│  │                            │                       │ class which implements)  ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Can proxy final class?     │ N/A (uses interface)  │ NO — cannot extend final ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Can proxy final method?    │ Only if in interface  │ NO — cannot override     ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Performance (creation)     │ Faster to create      │ Slightly slower          ││
│  │                            │                       │ (bytecode generation)    ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Performance (invocation)   │ Slightly slower       │ Faster (direct method    ││
│  │                            │ (reflection-based)    │ invocation via subclass) ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Built into Java?           │ YES (java.lang.       │ NO — third-party library ││
│  │                            │ reflect.Proxy)        │ (bundled in Spring)      ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Spring Boot default?       │ NO (since 2.0)        │ YES (since 2.0)          ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Proxy class name           │ $Proxy42              │ OrderService$$Enhancer   ││
│  │                            │                       │ BySpringCGLIB$$abc123    ││
│  └────────────────────────────┴───────────────────────┴──────────────────────────┘│
│                                                                                  │
│  When to use which:                                                              │
│    JDK Dynamic Proxy:                                                            │
│      • Bean implements interfaces                                                │
│      • You only need to intercept interface methods                              │
│      • You want to enforce programming to interfaces                             │
│      • Legacy apps that depend on Proxy.isProxyClass()                           │
│                                                                                  │
│    CGLIB Proxy (recommended — Spring Boot default):                               │
│      • Bean does NOT implement any interface                                     │
│      • You need to intercept concrete class methods                              │
│      • You need proxy instanceof TargetClass to be true                          │
│      • Modern Spring Boot applications (default since 2.0)                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── How Spring decides which proxy type to use ───
// DefaultAopProxyFactory.java (actual Spring code, simplified)

public class DefaultAopProxyFactory implements AopProxyFactory {

    @Override
    public AopProxy createAopProxy(AdvisedSupport config) {

        if (config.isOptimize() ||
            config.isProxyTargetClass() ||         // ← Spring Boot sets this TRUE
            hasNoUserSuppliedProxyInterfaces(config)) {

            Class<?> targetClass = config.getTargetClass();

            if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
                // Target is already an interface or proxy → use JDK
                return new JdkDynamicAopProxy(config);
            }

            // CGLIB proxy (default path for Spring Boot)
            return new ObjenesisCglibAopProxy(config);

        } else {
            // proxyTargetClass = false AND bean has interfaces → use JDK
            return new JdkDynamicAopProxy(config);
        }
    }
}
```

```java
// ─── Verify at runtime which proxy type was used ───
@RestController
public class DebugController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/debug/proxy")
    public Map<String, Object> proxyInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // What type is the injected bean?
        info.put("class", orderService.getClass().getName());
        // → "com.app.service.OrderService$$EnhancerBySpringCGLIB$$abc123"  (CGLIB)
        // → "com.sun.proxy.$Proxy42"  (JDK)

        // Is it a CGLIB proxy?
        info.put("isCglibProxy", AopUtils.isCglibProxy(orderService));
        // → true (for CGLIB)

        // Is it a JDK dynamic proxy?
        info.put("isJdkProxy", AopUtils.isJdkDynamicProxy(orderService));
        // → true (for JDK)

        // Is it ANY type of proxy?
        info.put("isAopProxy", AopUtils.isAopProxy(orderService));
        // → true (for both)

        // Get the target class (behind the proxy)
        info.put("targetClass", AopUtils.getTargetClass(orderService).getName());
        // → "com.app.service.OrderService"

        return info;
    }
}
```

---

### Complete Startup Flow — End-to-End Visual

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│  SpringApplication.run(MyApp.class)                                              │
│  ─────────────────────────────────                                               │
│         │                                                                        │
│         ▼                                                                        │
│  ┌──────────────────────────────────────┐                                        │
│  │ 1. Create ApplicationContext         │                                        │
│  │    - Scan for @Component, @Service,  │                                        │
│  │      @Controller, @Aspect, etc.      │                                        │
│  └──────────────────┬───────────────────┘                                        │
│                     ▼                                                            │
│  ┌──────────────────────────────────────┐                                        │
│  │ 2. Process @EnableAspectJAutoProxy   │                                        │
│  │    (auto-included by Spring Boot)    │                                        │
│  │    - Register AnnotationAwareAspect  │                                        │
│  │      JAutoProxyCreator as           │                                        │
│  │      BeanPostProcessor              │                                        │
│  └──────────────────┬───────────────────┘                                        │
│                     ▼                                                            │
│  ┌──────────────────────────────────────┐                                        │
│  │ 3. Discover @Aspect beans            │                                        │
│  │    - Find: LoggingAspect,            │                                        │
│  │      PerfAspect, SecurityAspect      │                                        │
│  │    - Extract @Before, @After,        │                                        │
│  │      @Around methods → Advisors      │                                        │
│  └──────────────────┬───────────────────┘                                        │
│                     ▼                                                            │
│  ┌──────────────────────────────────────┐                                        │
│  │ 4. Parse Pointcut Expressions        │                                        │
│  │    - PointcutParser tokenizes +      │                                        │
│  │      builds AST for each expression  │                                        │
│  │    - Cache parsed PointcutExpression  │                                        │
│  │      objects                          │                                        │
│  └──────────────────┬───────────────────┘                                        │
│                     ▼                                                            │
│  ╔══════════════════════════════════════╗                                        │
│  ║ 5-7. FOR EACH BEAN being created:   ║  ← LOOP                                │
│  ╠══════════════════════════════════════╣                                        │
│  ║                                      ║                                        │
│  ║  5. Is it an @Aspect? → SKIP        ║                                        │
│  ║     Otherwise → proceed              ║                                        │
│  ║           │                           ║                                        │
│  ║           ▼                           ║                                        │
│  ║  6. Check eligibility:               ║                                        │
│  ║     For each Advisor:                ║                                        │
│  ║       Phase 1: class-level match?    ║                                        │
│  ║         NO → skip this advisor       ║                                        │
│  ║         YES ↓                        ║                                        │
│  ║       Phase 2: any method match?     ║                                        │
│  ║         YES → advisor APPLIES        ║                                        │
│  ║           │                           ║                                        │
│  ║           ▼                           ║                                        │
│  ║  7. Any advisors apply?              ║                                        │
│  ║     NO  → register original bean     ║                                        │
│  ║     YES → create proxy:              ║                                        │
│  ║       Has interface + JDK mode?      ║                                        │
│  ║         → JDK Dynamic Proxy          ║                                        │
│  ║       Otherwise?                     ║                                        │
│  ║         → CGLIB Proxy (default)      ║                                        │
│  ║       Register PROXY in context      ║                                        │
│  ║                                      ║                                        │
│  ╚══════════════════════════════════════╝                                        │
│                     │                                                            │
│                     ▼                                                            │
│  ┌──────────────────────────────────────┐                                        │
│  │ Application Ready!                   │                                        │
│  │                                      │                                        │
│  │ ApplicationContext contains:         │                                        │
│  │   "orderService" → CGLIB Proxy       │                                        │
│  │   "paymentService" → CGLIB Proxy     │                                        │
│  │   "reportService" → Original Bean    │                                        │
│  │   "orderController" → CGLIB Proxy    │                                        │
│  │   "loggingAspect" → Original Bean    │                                        │
│  └──────────────────────────────────────┘                                        │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Startup Phase — Internal Classes Reference

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Key Spring AOP Internal Classes — Reference:                                    │
│                                                                                  │
│  ┌───────────────────────────────────────────┬───────────────────────────────────┐│
│  │ Class                                     │ Responsibility                    ││
│  ├───────────────────────────────────────────┼───────────────────────────────────┤│
│  │ @EnableAspectJAutoProxy                   │ Activates AOP (annotation)        ││
│  │ AopAutoConfiguration                     │ Auto-configures AOP in Boot       ││
│  │ AspectJAutoProxyRegistrar                 │ Registers the BeanPostProcessor   ││
│  │ AnnotationAwareAspectJAutoProxyCreator    │ The MAIN class — BeanPostProcessor││
│  │                                           │ that creates proxies              ││
│  │ BeanFactoryAspectJAdvisorsBuilder         │ Discovers @Aspect beans           ││
│  │ ReflectiveAspectJAdvisorFactory           │ Extracts Advisors from @Aspect    ││
│  │ AspectJExpressionPointcut                 │ Wraps a parsed pointcut expr      ││
│  │ PointcutParser (AspectJ lib)              │ Parses expression strings to AST  ││
│  │ AopUtils                                  │ canApply(), findAdvisors() etc.   ││
│  │ DefaultAopProxyFactory                    │ Decides JDK vs CGLIB              ││
│  │ JdkDynamicAopProxy                        │ Creates JDK proxies              ││
│  │ ObjenesisCglibAopProxy                    │ Creates CGLIB proxies            ││
│  │ ProxyFactory                              │ Configures and creates proxies    ││
│  │ AdvisedSupport                            │ Holds proxy configuration         ││
│  │ ReflectiveMethodInvocation                │ Executes the interceptor chain    ││
│  └───────────────────────────────────────────┴───────────────────────────────────┘│
│                                                                                  │
│  ┌───────────────────────────────────────────┬───────────────────────────────────┐│
│  │ Package                                   │ What's There                      ││
│  ├───────────────────────────────────────────┼───────────────────────────────────┤│
│  │ org.springframework.aop                   │ AOP interfaces & utils            ││
│  │ org.springframework.aop.framework         │ Proxy creation (JDK/CGLIB)        ││
│  │ org.springframework.aop.aspectj           │ AspectJ integration               ││
│  │ org.springframework.aop.aspectj.annotation│ @Aspect processing                ││
│  │ org.aspectj.weaver.tools                  │ PointcutParser (AspectJ lib)      ││
│  └───────────────────────────────────────────┴───────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## What is Advice? How It Works

An **Advice** is the **actual code** (the method body) that runs when a pointcut expression matches a method. It's the "action" part of AOP — the cross-cutting logic you want to execute.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Advice = WHAT to do + WHEN to do it                                             │
│                                                                                  │
│  An Advice consists of:                                                          │
│    1. A Java METHOD containing the cross-cutting logic                           │
│    2. A TIMING annotation (@Before, @After, @Around, etc.) that says WHEN        │
│    3. A POINTCUT expression (inline or named) that says WHERE                    │
│                                                                                  │
│  @Before("execution(* com.app.service.*.*(..))")                                 │
│   ↑ WHEN    ↑ WHERE (pointcut)                                                   │
│  public void logBefore(JoinPoint jp) {                                           │
│      log.info(">>> " + jp.getSignature());   ← WHAT (advice logic)              │
│  }                                                                               │
│                                                                                  │
│  In Spring's internal model:                                                     │
│    Advisor = Advice + Pointcut                                                   │
│    Advisor = "Run this code when this pattern matches"                           │
│                                                                                  │
│  Types of Advice:                                                                │
│  ┌──────────────────────┬────────────────────────────────────────────────────────┐│
│  │ Annotation           │ When It Runs                                           ││
│  ├──────────────────────┼────────────────────────────────────────────────────────┤│
│  │ @Before              │ BEFORE the target method executes                      ││
│  │ @After               │ AFTER the target method (always — success or failure)  ││
│  │ @AfterReturning      │ AFTER the target method returns SUCCESSFULLY           ││
│  │ @AfterThrowing       │ AFTER the target method THROWS an exception            ││
│  │ @Around              │ WRAPS the target method (before + after + control)     ││
│  └──────────────────────┴────────────────────────────────────────────────────────┘│
│                                                                                  │
│  Internally, Spring converts each advice annotation into an Advice interface:    │
│    @Before        → MethodBeforeAdvice                                           │
│    @After         → AfterAdvice                                                  │
│    @AfterReturning → AfterReturningAdvice                                        │
│    @AfterThrowing → ThrowsAdvice                                                 │
│    @Around        → MethodInterceptor (from AOP Alliance)                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── How Spring converts @Before/@After/@Around to internal Advice objects ───
// ReflectiveAspectJAdvisorFactory.java (simplified)

public Advisor getAdvisor(Method candidateAdviceMethod, 
                          MetadataAwareAspectInstanceFactory factory) {

    // 1. Find the advice annotation on the method
    AspectJAnnotation<?> annotation = findAspectJAnnotationOnMethod(candidateAdviceMethod);
    // Checks for: @Before, @After, @AfterReturning, @AfterThrowing, @Around

    if (annotation == null) return null;

    // 2. Extract the pointcut expression from the annotation
    AspectJExpressionPointcut pointcut = getPointcut(
        candidateAdviceMethod, factory.getAspectMetadata().getAspectClass());

    // 3. Create the Advisor (Advice + Pointcut)
    return new InstantiationModelAwarePointcutAdvisorImpl(
        pointcut, candidateAdviceMethod, this, factory);
}

// Inside InstantiationModelAwarePointcutAdvisorImpl:
private Advice instantiateAdvice() {
    switch (this.aspectJAnnotation.getAnnotationType()) {
        case AtBefore:
            return new AspectJMethodBeforeAdvice(method, pointcut, factory);
        case AtAfter:
            return new AspectJAfterAdvice(method, pointcut, factory);
        case AtAfterReturning:
            return new AspectJAfterReturningAdvice(method, pointcut, factory);
        case AtAfterThrowing:
            return new AspectJAfterThrowingAdvice(method, pointcut, factory);
        case AtAround:
            return new AspectJAroundAdvice(method, pointcut, factory);
        default:
            throw new UnsupportedOperationException("Unknown advice type");
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Advice Class Hierarchy (Spring Internal):                                       │
│                                                                                  │
│  org.aopalliance.aop.Advice (marker interface)                                   │
│    │                                                                             │
│    ├── org.aopalliance.intercept.MethodInterceptor                               │
│    │     │  → invoke(MethodInvocation) — the "interceptor chain" interface        │
│    │     │                                                                       │
│    │     ├── AspectJAroundAdvice         ← @Around                               │
│    │     ├── AspectJAfterAdvice          ← @After                                │
│    │     ├── AspectJAfterReturningAdvice ← @AfterReturning                       │
│    │     └── AspectJAfterThrowingAdvice  ← @AfterThrowing                        │
│    │                                                                             │
│    └── MethodBeforeAdvice                                                        │
│          └── AspectJMethodBeforeAdvice   ← @Before                               │
│                                                                                  │
│  KEY INSIGHT:                                                                    │
│    ALL advice types (except @Before) implement MethodInterceptor.                │
│    @Before is adapted into a MethodInterceptor via                               │
│    MethodBeforeAdviceInterceptor at runtime.                                     │
│                                                                                  │
│    This means: at runtime, EVERY advice is a MethodInterceptor.                  │
│    They all have the same invoke(MethodInvocation) method.                       │
│    The interceptor chain is a list of MethodInterceptors.                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## What is JoinPoint? How It Works

A **JoinPoint** represents a **point during program execution** where an aspect can be applied. In Spring AOP, the only supported join point type is **method execution** (unlike full AspectJ which supports field access, constructor execution, etc.).

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JoinPoint = A specific point in your program's execution                        │
│                                                                                  │
│  In Spring AOP, a JoinPoint is always a METHOD EXECUTION:                        │
│    "The moment when orderService.createOrder(req) is being called"               │
│                                                                                  │
│  JoinPoint provides METADATA about the intercepted method call:                  │
│    • Which method was called?                                                    │
│    • What arguments were passed?                                                 │
│    • What object (target) is the method being called on?                         │
│    • What is the proxy object?                                                   │
│    • What is the method signature?                                               │
│                                                                                  │
│  Two Types in Spring AOP:                                                        │
│    JoinPoint              → used in @Before, @After, @AfterReturning,            │
│                             @AfterThrowing                                       │
│    ProceedingJoinPoint    → used in @Around (adds proceed() method)              │
│                             Extends JoinPoint                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── JoinPoint interface (from AspectJ library) ───
// org.aspectj.lang.JoinPoint

public interface JoinPoint {

    // ─── Method Information ───
    Signature getSignature();
    //  → Returns: "Order com.app.service.OrderService.createOrder(OrderRequest)"
    //  → Cast to MethodSignature for full method details

    // ─── Arguments ───
    Object[] getArgs();
    //  → Returns: [OrderRequest(product=Laptop, amount=999.99)]

    // ─── Target Object (the real bean behind the proxy) ───
    Object getTarget();
    //  → Returns: the actual OrderService instance

    // ─── Proxy Object (the CGLIB/JDK proxy) ───
    Object getThis();
    //  → Returns: the proxy wrapping OrderService

    // ─── String representation of the join point ───
    String toString();
    //  → "execution(Order com.app.service.OrderService.createOrder(OrderRequest))"

    String toShortString();
    //  → "execution(OrderService.createOrder(..))"

    String toLongString();
    //  → Full details including modifiers

    // ─── Kind of join point ───
    String getKind();
    //  → "method-execution" (always in Spring AOP)

    // ─── Static part (for advanced use) ───
    JoinPoint.StaticPart getStaticPart();
}
```

```java
// ─── ProceedingJoinPoint — extends JoinPoint, used in @Around ───
// org.aspectj.lang.ProceedingJoinPoint

public interface ProceedingJoinPoint extends JoinPoint {

    // Call the ACTUAL target method
    Object proceed() throws Throwable;
    //  → Invokes the original method with original arguments
    //  → Returns the original method's return value

    // Call the ACTUAL target method with MODIFIED arguments
    Object proceed(Object[] args) throws Throwable;
    //  → Invokes the original method with new arguments
    //  → Useful for argument transformation
}
```

```java
// ═══ Complete JoinPoint Usage Example ═══
@Aspect
@Component
@Slf4j
public class JoinPointDemoAspect {

    @Before("execution(* com.app.service.OrderService.createOrder(..))")
    public void demonstrateJoinPoint(JoinPoint jp) {

        // 1. Get the method signature
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        log.info("Method: {}", method.getName());
        //  → "createOrder"

        log.info("Return type: {}", signature.getReturnType().getSimpleName());
        //  → "Order"

        log.info("Declaring class: {}", signature.getDeclaringTypeName());
        //  → "com.app.service.OrderService"

        // 2. Get the arguments
        Object[] args = jp.getArgs();
        log.info("Arguments count: {}", args.length);
        //  → 1

        log.info("Argument[0]: {}", args[0]);
        //  → "OrderRequest(product=Laptop, amount=999.99)"

        // Get parameter names and types
        String[] paramNames = signature.getParameterNames();
        Class<?>[] paramTypes = signature.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            log.info("  Param: {} ({}) = {}", paramNames[i],
                paramTypes[i].getSimpleName(), args[i]);
            //  → "Param: req (OrderRequest) = OrderRequest(product=Laptop, ...)"
        }

        // 3. Get the target object (real bean)
        Object target = jp.getTarget();
        log.info("Target class: {}", target.getClass().getSimpleName());
        //  → "OrderService"  (the real class, not proxy)

        // 4. Get the proxy object
        Object proxy = jp.getThis();
        log.info("Proxy class: {}", proxy.getClass().getSimpleName());
        //  → "OrderService$$EnhancerBySpringCGLIB$$abc123"  (the proxy)

        // 5. Get the join point kind
        log.info("Kind: {}", jp.getKind());
        //  → "method-execution"

        // 6. String representations
        log.info("Short: {}", jp.toShortString());
        //  → "execution(OrderService.createOrder(..))"
        log.info("Full: {}", jp.toString());
        //  → "execution(Order com.app.service.OrderService.createOrder(OrderRequest))"
    }
}
```

```java
// ═══ ProceedingJoinPoint in @Around — Modifying Arguments ═══
@Aspect
@Component
@Slf4j
public class ArgumentModifyAspect {

    @Around("execution(* com.app.service.OrderService.createOrder(com.app.dto.OrderRequest))")
    public Object normalizeAndProceed(ProceedingJoinPoint pjp) throws Throwable {

        // 1. Get original arguments
        Object[] args = pjp.getArgs();
        OrderRequest original = (OrderRequest) args[0];
        log.info("Original: product={}", original.getProduct());

        // 2. Modify the argument (normalize product name)
        OrderRequest normalized = new OrderRequest(
            original.getProduct().trim().toUpperCase(),
            original.getAmount()
        );

        // 3. Proceed with MODIFIED arguments
        Object result = pjp.proceed(new Object[] { normalized });
        //                          ↑ pass modified args to target method

        log.info("Order created with normalized product: {}", normalized.getProduct());
        return result;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JoinPoint vs ProceedingJoinPoint:                                               │
│                                                                                  │
│  ┌────────────────────────────┬──────────────────────┬───────────────────────────┐│
│  │ Feature                    │ JoinPoint            │ ProceedingJoinPoint       ││
│  ├────────────────────────────┼──────────────────────┼───────────────────────────┤│
│  │ Used in                    │ @Before, @After,     │ @Around ONLY              ││
│  │                            │ @AfterReturning,     │                           ││
│  │                            │ @AfterThrowing       │                           ││
│  ├────────────────────────────┼──────────────────────┼───────────────────────────┤│
│  │ getSignature()             │ ✓ YES                │ ✓ YES (inherited)         ││
│  │ getArgs()                  │ ✓ YES                │ ✓ YES (inherited)         ││
│  │ getTarget()                │ ✓ YES                │ ✓ YES (inherited)         ││
│  │ getThis()                  │ ✓ YES                │ ✓ YES (inherited)         ││
│  ├────────────────────────────┼──────────────────────┼───────────────────────────┤│
│  │ proceed()                  │ ✗ NO                 │ ✓ YES — calls target      ││
│  │ proceed(Object[] args)     │ ✗ NO                 │ ✓ YES — modified args     ││
│  ├────────────────────────────┼──────────────────────┼───────────────────────────┤│
│  │ Can skip target method?    │ ✗ NO (always runs)   │ ✓ YES (don't call proceed)││
│  │ Can modify return value?   │ ✗ NO                 │ ✓ YES                     ││
│  │ Can modify arguments?      │ ✗ NO (read-only)     │ ✓ YES (via proceed(args)) ││
│  │ Can catch exceptions?      │ ✗ NO                 │ ✓ YES (try/catch proceed) ││
│  └────────────────────────────┴──────────────────────┴───────────────────────────┘│
│                                                                                  │
│  Where JoinPoint objects come from:                                              │
│    Spring creates a JoinPoint/ProceedingJoinPoint for each method invocation     │
│    and passes it as a parameter to your advice method. You don't create it.      │
│    Internally, it wraps MethodInvocation from the interceptor chain.             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## How @Before, @After, and @Around Work Internally

At runtime, when a proxied method is called, Spring builds an **interceptor chain** — an ordered list of `MethodInterceptor` objects. Each advice type is wrapped in a `MethodInterceptor` that controls WHEN the advice method runs relative to the target method.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  The Interceptor Chain — Core Concept:                                            │
│                                                                                  │
│  When client calls: orderService.createOrder(req)                                │
│                                                                                  │
│  The proxy builds an INTERCEPTOR CHAIN for this method:                          │
│                                                                                  │
│    chain = [                                                                     │
│      AroundAdviceInterceptor,       // @Around (outermost wrapper)               │
│      BeforeAdviceInterceptor,       // @Before                                   │
│      AfterReturningInterceptor,     // @AfterReturning                           │
│      AfterThrowingInterceptor,      // @AfterThrowing                            │
│      AfterAdviceInterceptor         // @After (finally)                          │
│    ]                                                                             │
│                                                                                  │
│  Execution is like a CHAIN OF RESPONSIBILITY pattern:                            │
│    Each interceptor calls invocation.proceed() to pass to the next one.          │
│    The last one in the chain calls the ACTUAL target method.                     │
│                                                                                  │
│  Client → Proxy → Interceptor1 → Interceptor2 → ... → Target Method             │
│                 ←              ←                ←     ← Return Value             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### @Before — Internally

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Before — Execution Flow:                                                       │
│                                                                                  │
│  Client calls proxy.createOrder(req)                                             │
│    │                                                                             │
│    ▼                                                                             │
│  MethodBeforeAdviceInterceptor.invoke(MethodInvocation mi) {                     │
│    │                                                                             │
│    │  1. Run the @Before advice method                                           │
│    │     → this.advice.before(method, args, target)                              │
│    │     → YOUR logBefore(JoinPoint jp) executes HERE                            │
│    │                                                                             │
│    │  2. Then proceed to next interceptor (or target method)                     │
│    │     → return mi.proceed()                                                   │
│    │                                                                             │
│    ▼                                                                             │
│  Target method executes                                                          │
│    │                                                                             │
│    ▼                                                                             │
│  Return value flows back to client                                               │
│                                                                                  │
│  KEY: @Before CANNOT:                                                            │
│    • Stop the target method from executing (unless it throws an exception)       │
│    • Modify the return value                                                     │
│    • Modify the arguments (they are read-only via JoinPoint)                     │
│                                                                                  │
│  @Before CAN:                                                                    │
│    • Log information                                                             │
│    • Validate arguments (and throw exception to prevent execution)               │
│    • Set up context (e.g., MDC logging context)                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Spring's internal implementation of @Before ───
// MethodBeforeAdviceInterceptor.java (actual Spring class, simplified)

public class MethodBeforeAdviceInterceptor implements MethodInterceptor {

    private final MethodBeforeAdvice advice;  // Your @Before method wrapped

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        // 1. Execute the @Before advice FIRST
        this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());

        // 2. Then proceed to next interceptor or target method
        return mi.proceed();
    }
}

// AspectJMethodBeforeAdvice.java — wraps YOUR @Before method
public class AspectJMethodBeforeAdvice extends AbstractAspectJAdvice 
        implements MethodBeforeAdvice {

    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        // Create a JoinPoint and invoke YOUR advice method via reflection
        invokeAdviceMethod(getJoinPointMatch(), null, null);
        //  ↑ Calls: logBefore(JoinPoint jp) — your method
    }
}
```

### @After — Internally

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @After — Execution Flow:                                                        │
│                                                                                  │
│  Client calls proxy.createOrder(req)                                             │
│    │                                                                             │
│    ▼                                                                             │
│  AspectJAfterAdvice.invoke(MethodInvocation mi) {                                │
│    │                                                                             │
│    │  try {                                                                      │
│    │      return mi.proceed();    ← execute target method                        │
│    │  } finally {                                                                │
│    │      invokeAdviceMethod();   ← ALWAYS runs (success OR failure)             │
│    │      → YOUR logAfter() executes HERE                                        │
│    │  }                                                                          │
│    │                                                                             │
│    ▼                                                                             │
│  Return value flows back (or exception propagates)                               │
│                                                                                  │
│  KEY: @After is like a "finally" block:                                          │
│    • Runs AFTER the target method no matter what                                 │
│    • Runs even if the target method THROWS an exception                          │
│    • CANNOT access the return value (use @AfterReturning for that)               │
│    • CANNOT access the exception (use @AfterThrowing for that)                   │
│    • CANNOT modify the return value or suppress exceptions                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Spring's internal implementation of @After ───
// AspectJAfterAdvice.java (actual Spring class, simplified)

public class AspectJAfterAdvice extends AbstractAspectJAdvice
        implements MethodInterceptor, AfterAdvice {

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        try {
            // 1. Proceed to target method (or next interceptor)
            return mi.proceed();
        } finally {
            // 2. ALWAYS run the @After advice — like a finally block
            invokeAdviceMethod(getJoinPointMatch(), null, null);
            //  ↑ Calls YOUR @After method
        }
    }
}
```

### @AfterReturning and @AfterThrowing — Internally

```java
// ─── @AfterReturning — runs only on SUCCESS ───
// AspectJAfterReturningAdvice.java (simplified)

public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice
        implements MethodInterceptor, AfterReturningAdvice {

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        // 1. Proceed to target method
        Object retVal = mi.proceed();

        // 2. If we reach here → method returned successfully (no exception)
        // 3. Run @AfterReturning advice, passing the return value
        this.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
        //  ↑ Calls YOUR @AfterReturning method with the "returning" value

        return retVal;
        // Note: if mi.proceed() threw an exception, this code is SKIPPED
    }
}

// ─── @AfterThrowing — runs only on FAILURE ───
// AspectJAfterThrowingAdvice.java (simplified)

public class AspectJAfterThrowingAdvice extends AbstractAspectJAdvice
        implements MethodInterceptor, AfterAdvice {

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        try {
            // 1. Proceed to target method
            return mi.proceed();
        } catch (Throwable ex) {
            // 2. Exception caught → check if it matches the declared type
            if (shouldInvokeOnThrowing(ex)) {
                // 3. Run @AfterThrowing advice, passing the exception
                invokeAdviceMethod(getJoinPointMatch(), null, ex);
                //  ↑ Calls YOUR @AfterThrowing method with the "throwing" value
            }
            // 4. Re-throw the exception (advice cannot suppress it)
            throw ex;
        }
    }
}
```

### @Around — Internally (The Most Powerful)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Around — Execution Flow:                                                       │
│                                                                                  │
│  Client calls proxy.createOrder(req)                                             │
│    │                                                                             │
│    ▼                                                                             │
│  AspectJAroundAdvice.invoke(MethodInvocation mi) {                               │
│    │                                                                             │
│    │  1. Create a ProceedingJoinPoint wrapping the MethodInvocation              │
│    │  2. Call YOUR @Around method, passing the ProceedingJoinPoint               │
│    │     → YOUR timedExecution(ProceedingJoinPoint pjp)                          │
│    │                                                                             │
│    │  Inside YOUR method:                                                        │
│    │    ┌──────────────────────────────────────────────┐                         │
│    │    │  // Code BEFORE target (like @Before)        │                         │
│    │    │  long start = System.currentTimeMillis();    │                         │
│    │    │                                              │                         │
│    │    │  Object result = pjp.proceed();              │ ← Calls target method   │
│    │    │  //  ↑ YOU control when/if to call proceed() │                         │
│    │    │                                              │                         │
│    │    │  // Code AFTER target (like @AfterReturning) │                         │
│    │    │  long duration = System.currentTimeMillis()  │                         │
│    │    │                  - start;                     │                         │
│    │    │  return result;                              │                         │
│    │    └──────────────────────────────────────────────┘                         │
│    │                                                                             │
│    ▼                                                                             │
│  Return value flows back to client                                               │
│                                                                                  │
│  KEY: @Around CAN:                                                               │
│    • Run code before AND after the target method                                 │
│    • Choose NOT to call proceed() → target method never executes                 │
│    • Modify arguments by calling proceed(newArgs)                                │
│    • Modify the return value before returning it                                 │
│    • Catch and handle exceptions from the target method                          │
│    • Call proceed() MULTIPLE TIMES (retry logic)                                 │
│    • Measure execution time (wraps entire call)                                  │
│                                                                                  │
│  @Around is the MOST POWERFUL because YOU control the flow entirely.             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Spring's internal implementation of @Around ───
// AspectJAroundAdvice.java (actual Spring class, simplified)

public class AspectJAroundAdvice extends AbstractAspectJAdvice
        implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        // 1. Wrap MethodInvocation in a ProceedingJoinPoint
        ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint();
        //  ↑ This ProceedingJoinPoint delegates proceed() to mi.proceed()

        // 2. Call YOUR @Around advice method via reflection
        //    passing the ProceedingJoinPoint as parameter
        return invokeAdviceMethod(pjp, null, null);
        //  ↑ Calls: timedExecution(ProceedingJoinPoint pjp) — your method
        //    When YOUR method calls pjp.proceed(), it triggers mi.proceed()
        //    which moves to the next interceptor in the chain (or target)
    }
}
```

### All Advice Types — Complete Execution Order

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Execution Order When ALL 5 Advice Types Are Present:                            │
│                                                                                  │
│  Given:                                                                          │
│    @Around, @Before, @After, @AfterReturning, @AfterThrowing                    │
│    all matching the same method: orderService.createOrder(req)                   │
│                                                                                  │
│  ═══ SUCCESS PATH (method returns normally) ═══                                  │
│                                                                                  │
│    Client calls proxy.createOrder(req)                                           │
│      │                                                                           │
│      ▼                                                                           │
│    @Around (before proceed)   ← "Starting timer..."                              │
│      │                                                                           │
│      ▼                                                                           │
│    @Before                    ← ">>> OrderService.createOrder()"                 │
│      │                                                                           │
│      ▼                                                                           │
│    TARGET METHOD EXECUTES     ← actual createOrder() runs                        │
│      │                                                                           │
│      ▼ (returns Order)                                                           │
│    @AfterReturning            ← "Returned: Order(id=1, ...)"                    │
│      │                                                                           │
│      ▼                                                                           │
│    @After                     ← "Completed (finally)"                            │
│      │                                                                           │
│      ▼                                                                           │
│    @Around (after proceed)    ← "Timer: 45ms"                                   │
│      │                                                                           │
│      ▼                                                                           │
│    Return to Client                                                              │
│                                                                                  │
│  ═══ FAILURE PATH (method throws exception) ═══                                  │
│                                                                                  │
│    Client calls proxy.createOrder(req)                                           │
│      │                                                                           │
│      ▼                                                                           │
│    @Around (before proceed)   ← "Starting timer..."                              │
│      │                                                                           │
│      ▼                                                                           │
│    @Before                    ← ">>> OrderService.createOrder()"                 │
│      │                                                                           │
│      ▼                                                                           │
│    TARGET METHOD THROWS       ← throws RuntimeException("DB error")              │
│      │                                                                           │
│      ▼ (exception)                                                               │
│    @AfterThrowing             ← "Error: DB error"                                │
│      │                                                                           │
│      ▼                                                                           │
│    @After                     ← "Completed (finally)" — still runs!              │
│      │                                                                           │
│      ▼                                                                           │
│    @Around (catch block)      ← "Timer: 12ms (failed)"                           │
│      │                                                                           │
│      ▼                                                                           │
│    Exception propagates to Client                                                │
│                                                                                  │
│  Note: @AfterReturning does NOT run on failure path                              │
│  Note: @AfterThrowing does NOT run on success path                               │
│  Note: @After ALWAYS runs (like finally)                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══ Complete Example — All 5 advice types on the same method ═══
@Aspect
@Component
@Slf4j
public class FullLifecycleAspect {

    @Around("execution(* com.app.service.OrderService.createOrder(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        log.info("1. @Around BEFORE proceed");
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("6. @Around AFTER proceed (success) — {}ms",
                System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.info("6. @Around AFTER proceed (failure) — {}ms",
                System.currentTimeMillis() - start);
            throw ex;
        }
    }

    @Before("execution(* com.app.service.OrderService.createOrder(..))")
    public void before(JoinPoint jp) {
        log.info("2. @Before — args: {}", Arrays.toString(jp.getArgs()));
    }

    @AfterReturning(
        pointcut = "execution(* com.app.service.OrderService.createOrder(..))",
        returning = "result"
    )
    public void afterReturning(JoinPoint jp, Object result) {
        log.info("3. @AfterReturning — result: {}", result);
    }

    @AfterThrowing(
        pointcut = "execution(* com.app.service.OrderService.createOrder(..))",
        throwing = "ex"
    )
    public void afterThrowing(JoinPoint jp, Exception ex) {
        log.info("3. @AfterThrowing — exception: {}", ex.getMessage());
    }

    @After("execution(* com.app.service.OrderService.createOrder(..))")
    public void after(JoinPoint jp) {
        log.info("4/5. @After (finally) — always runs");
    }
}

// Console Output (SUCCESS):
//   1. @Around BEFORE proceed
//   2. @Before — args: [OrderRequest(product=Laptop, amount=999.99)]
//   [TARGET METHOD: createOrder executes]
//   3. @AfterReturning — result: Order(id=1, product=Laptop, ...)
//   4/5. @After (finally) — always runs
//   6. @Around AFTER proceed (success) — 45ms

// Console Output (FAILURE):
//   1. @Around BEFORE proceed
//   2. @Before — args: [OrderRequest(product=null, amount=-1)]
//   [TARGET METHOD: throws IllegalArgumentException]
//   3. @AfterThrowing — exception: Product name required
//   4/5. @After (finally) — always runs
//   6. @Around AFTER proceed (failure) — 12ms
```

---

## How Matching, Linking, and Interception Work Together

This section explains the **runtime flow** — what happens at the moment a proxied method is actually called. The three phases are: **matching** (already done at startup), **linking** (building the interceptor chain), and **interception** (executing the chain).

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Three Phases of AOP Method Interception:                                        │
│                                                                                  │
│  Phase 1: MATCHING (done at STARTUP — cached)                                    │
│    "Which advisors apply to which beans and methods?"                            │
│    → Already done during proxy creation (Step 6 in startup flow)                │
│    → Results cached in the proxy object                                          │
│                                                                                  │
│  Phase 2: LINKING (done at FIRST METHOD CALL — then cached)                      │
│    "For THIS specific method, build an ordered interceptor chain"                │
│    → Filters matching advisors to the specific method being called               │
│    → Orders them by advice type and @Order annotation                            │
│    → Caches the chain: Map<Method, List<MethodInterceptor>>                      │
│                                                                                  │
│  Phase 3: INTERCEPTION (done on EVERY method call)                               │
│    "Execute the interceptor chain"                                               │
│    → Retrieve cached chain for this method                                       │
│    → Execute chain using Chain of Responsibility pattern                         │
│    → Each interceptor calls proceed() to pass to the next                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Runtime Method Call — Complete Flow:                                             │
│                                                                                  │
│  // In your controller:                                                          │
│  Order order = orderService.createOrder(req);                                    │
│  // orderService is actually a CGLIB PROXY                                       │
│                                                                                  │
│  Step 1: Proxy intercepts the call                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │ CglibAopProxy.DynamicAdvisedInterceptor.intercept(                          │ │
│  │     proxy, method=createOrder, args=[req], methodProxy)                     │ │
│  └─────────────────────────────────────┬───────────────────────────────────────┘ │
│                                        │                                         │
│  Step 2: Get interceptor chain for this method (LINKING)                         │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │ List<Object> chain = this.advised.getInterceptorsAndDynamicInterception     │ │
│  │     Advice(method, targetClass);                                            │ │
│  │                                                                             │ │
│  │ What happens inside:                                                        │ │
│  │   1. Check cache: methodCache.get(method)                                   │ │
│  │      → If cached: return immediately (fast path)                            │ │
│  │      → If NOT cached: compute and cache                                     │ │
│  │                                                                             │ │
│  │   2. For each Advisor attached to this proxy:                               │ │
│  │      a. Does the advisor's pointcut match THIS method?                      │ │
│  │         → pointcut.matches(method, targetClass)                             │ │
│  │         → Uses cached ShadowMatch from startup                              │ │
│  │      b. If match: convert Advice → MethodInterceptor                        │ │
│  │         → Add to the chain                                                  │ │
│  │                                                                             │ │
│  │   3. Cache the chain for next time:                                         │ │
│  │      methodCache.put(method, chain)                                         │ │
│  │                                                                             │ │
│  │ Result: chain = [AroundInterceptor, BeforeInterceptor, AfterInterceptor]    │ │
│  └─────────────────────────────────────┬───────────────────────────────────────┘ │
│                                        │                                         │
│  Step 3: Execute the chain (INTERCEPTION)                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │ if (chain.isEmpty()) {                                                      │ │
│  │     // No interceptors → call target directly                               │ │
│  │     return methodProxy.invoke(target, args);                                │ │
│  │ }                                                                           │ │
│  │                                                                             │ │
│  │ // Has interceptors → create MethodInvocation and execute                   │ │
│  │ MethodInvocation invocation = new CglibMethodInvocation(                    │ │
│  │     proxy, target, method, args, targetClass, chain, methodProxy);          │ │
│  │ return invocation.proceed();   ← starts the chain                          │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── The core chain execution engine ───
// ReflectiveMethodInvocation.java (actual Spring class, simplified)

public class ReflectiveMethodInvocation implements ProxyMethodInvocation {

    protected final Object proxy;
    protected final Object target;
    protected final Method method;
    protected Object[] arguments;
    private final List<Object> interceptorsAndDynamicMethodMatchers;

    private int currentInterceptorIndex = -1;  // ← tracks position in chain

    @Override
    public Object proceed() throws Throwable {
        // If we've reached the end of the chain → call the actual target method
        if (this.currentInterceptorIndex == 
            this.interceptorsAndDynamicMethodMatchers.size() - 1) {

            return invokeJoinpoint();
            //  ↑ Calls: target.createOrder(req) — the REAL method
        }

        // Move to the next interceptor in the chain
        Object interceptorOrInterceptionAdvice =
            this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);

        if (interceptorOrInterceptionAdvice instanceof MethodInterceptor) {
            MethodInterceptor mi = (MethodInterceptor) interceptorOrInterceptionAdvice;

            // Call the interceptor, passing THIS invocation so it can call proceed()
            return mi.invoke(this);
            //  ↑ The interceptor runs its advice and then calls this.proceed()
            //    which increments the index and moves to the next interceptor
        }

        // Skip and proceed to next
        return proceed();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Chain Execution — Visual Walkthrough:                                           │
│                                                                                  │
│  chain = [AroundInterceptor, BeforeInterceptor, AfterInterceptor]                │
│  currentIndex starts at -1                                                       │
│                                                                                  │
│  ┌─── Call #1: proceed() ──────────────────────────────────────────────────────┐ │
│  │ currentIndex: -1 → 0                                                        │ │
│  │ chain[0] = AroundInterceptor                                                │ │
│  │ AroundInterceptor.invoke(this):                                             │ │
│  │   YOUR @Around method runs (before proceed)                                 │ │
│  │   calls pjp.proceed() ──────────────────────────┐                           │ │
│  │                                                   │                          │ │
│  │  ┌─── Call #2: proceed() ───────────────────────┐│                          │ │
│  │  │ currentIndex: 0 → 1                          ││                          │ │
│  │  │ chain[1] = BeforeInterceptor                 ││                          │ │
│  │  │ BeforeInterceptor.invoke(this):              ││                          │ │
│  │  │   YOUR @Before method runs                   ││                          │ │
│  │  │   calls mi.proceed() ──────────────┐         ││                          │ │
│  │  │                                     │        ││                          │ │
│  │  │  ┌─── Call #3: proceed() ─────────┐│        ││                          │ │
│  │  │  │ currentIndex: 1 → 2            ││        ││                          │ │
│  │  │  │ chain[2] = AfterInterceptor    ││        ││                          │ │
│  │  │  │ AfterInterceptor.invoke(this): ││        ││                          │ │
│  │  │  │   try {                        ││        ││                          │ │
│  │  │  │     mi.proceed() ─────────┐    ││        ││                          │ │
│  │  │  │                            │   ││        ││                          │ │
│  │  │  │  ┌─── Call #4: proceed() ┐│   ││        ││                          │ │
│  │  │  │  │ currentIndex: 2 → 3   ││   ││        ││                          │ │
│  │  │  │  │ index == size - 1     ││   ││        ││                          │ │
│  │  │  │  │ → invokeJoinpoint()   ││   ││        ││                          │ │
│  │  │  │  │ → TARGET METHOD RUNS  ││   ││        ││                          │ │
│  │  │  │  │ → returns Order       ││   ││        ││                          │ │
│  │  │  │  └───────────────────────┘│   ││        ││                          │ │
│  │  │  │                            │   ││        ││                          │ │
│  │  │  │   } finally {             │   ││        ││                          │ │
│  │  │  │     YOUR @After runs      │   ││        ││                          │ │
│  │  │  │   }                        │   ││        ││                          │ │
│  │  │  │   returns Order ───────────┘   ││        ││                          │ │
│  │  │  └────────────────────────────────┘│        ││                          │ │
│  │  │   returns Order ───────────────────┘        ││                          │ │
│  │  └─────────────────────────────────────────────┘│                          │ │
│  │   YOUR @Around method runs (after proceed)      │                          │ │
│  │   returns Order ────────────────────────────────┘                          │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  Final: Order returned to Client                                                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── How the proxy gets the interceptor chain for a method ───
// AdvisedSupport.java (simplified)

public class AdvisedSupport {

    // Cache: method → interceptor chain
    private transient Map<MethodCacheKey, List<Object>> methodCache;

    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
            Method method, Class<?> targetClass) {

        MethodCacheKey cacheKey = new MethodCacheKey(method);

        // Check cache first
        List<Object> cached = this.methodCache.get(cacheKey);
        if (cached != null) {
            return cached;  // ← FAST: return cached chain
        }

        // Not cached → compute the chain
        List<Object> chain = this.advisorChainFactory
            .getInterceptorsAndDynamicInterceptionAdvice(
                this, method, targetClass);

        // Cache for next time
        this.methodCache.put(cacheKey, chain);
        return chain;
    }
}

// DefaultAdvisorChainFactory.java — builds the chain
public class DefaultAdvisorChainFactory implements AdvisorChainFactory {

    @Override
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
            Advised config, Method method, Class<?> targetClass) {

        List<Object> interceptorList = new ArrayList<>();
        AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();

        // For each advisor attached to this proxy:
        for (Advisor advisor : config.getAdvisors()) {
            if (advisor instanceof PointcutAdvisor) {
                PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;

                // Check if this advisor's pointcut matches THIS method
                if (pointcutAdvisor.getPointcut().getClassFilter().matches(targetClass) &&
                    pointcutAdvisor.getPointcut().getMethodMatcher()
                        .matches(method, targetClass)) {

                    // Convert Advice → MethodInterceptor(s)
                    MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
                    interceptorList.addAll(Arrays.asList(interceptors));
                }
            }
        }

        return interceptorList;
    }
}
```

---

## What If We Have 100s of Pointcuts? — Performance & Caching

**Question:** If there are 100 pointcut expressions and I call a method, does Spring match against all 100 pointcuts every time? Will the method execute 100 times?

**Answer:** **NO** to both. Spring uses **multi-level caching** to ensure matching happens **once** (at startup or first call), and the method executes **only once** regardless of how many pointcuts match.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  100 Pointcuts — What ACTUALLY Happens:                                          │
│                                                                                  │
│  ═══ STARTUP TIME (ONCE) ═══                                                     │
│                                                                                  │
│  For each bean (e.g., OrderService):                                             │
│    Check 100 advisors' pointcuts against OrderService's methods                  │
│    → Phase 1 (class-level): 70 eliminated immediately (wrong package/class)     │
│    → Phase 2 (method-level): 30 checked, 5 match                                │
│    → Result: OrderService proxy gets [Advisor#3, #17, #42, #68, #91]            │
│    → Cached. NEVER re-computed.                                                  │
│                                                                                  │
│  ═══ FIRST METHOD CALL (ONCE PER METHOD) ═══                                     │
│                                                                                  │
│  orderService.createOrder(req) called for the first time:                        │
│    Proxy has 5 advisors. Check which match THIS method:                          │
│    → Advisor#3:  matches createOrder → YES                                       │
│    → Advisor#17: matches createOrder → YES                                       │
│    → Advisor#42: matches get* only  → NO                                        │
│    → Advisor#68: matches createOrder → YES                                       │
│    → Advisor#91: matches void only  → NO                                        │
│    → Result: chain = [Interceptor#3, Interceptor#17, Interceptor#68]             │
│    → Cached in methodCache. NEVER re-computed.                                   │
│                                                                                  │
│  ═══ EVERY SUBSEQUENT CALL ═══                                                   │
│                                                                                  │
│  orderService.createOrder(req) called again:                                     │
│    1. Proxy looks up methodCache → chain found instantly (O(1) HashMap)          │
│    2. Execute chain: 3 interceptors run                                          │
│    3. Target method executes ONCE                                                │
│    4. Return result                                                              │
│                                                                                  │
│  NO re-matching with 100 pointcuts. NO 100x execution.                           │
│  Just 3 interceptors + 1 target method execution.                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### The Method is NEVER Executed Multiple Times

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  "Will the method execute 100 times?" — NO!                                      │
│                                                                                  │
│  Even if 100 advisors match, the method executes EXACTLY ONCE.                   │
│  Here's why:                                                                     │
│                                                                                  │
│  The interceptor chain is like nested function calls:                             │
│                                                                                  │
│    Interceptor#1 (Around) {                                                      │
│      // before                                                                   │
│      Interceptor#2 (Before) {                                                    │
│        // advice runs                                                            │
│        Interceptor#3 (After) {                                                   │
│          // try {                                                                │
│            TARGET METHOD ← executes ONCE                                         │
│          // } finally { advice runs }                                            │
│        }                                                                         │
│      }                                                                           │
│      // after                                                                    │
│    }                                                                             │
│                                                                                  │
│  The chain is NESTED, not REPEATED:                                              │
│    ✗ WRONG: method() → method() → method() → method()  (100 times)              │
│    ✓ RIGHT: advice1 → advice2 → advice3 → method() → back through chain         │
│                                                                                  │
│  proceed() moves to the NEXT interceptor, not re-executes the method.            │
│  Only the LAST proceed() in the chain calls the actual target method.            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### Caching Architecture — Library Code Walkthrough

```java
// ═══ Level 1: Advisor-to-Bean matching — cached at STARTUP ═══
// AbstractAutoProxyCreator.java

public abstract class AbstractAutoProxyCreator {

    // Cache: tracks which beans have been checked
    private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);
    //  "orderService" → true (proxy created)
    //  "reportService" → false (no proxy needed)
    //  Once set, NEVER re-checked

    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // Already checked? Return immediately
        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;  // ← FAST: no re-checking
        }

        // Find matching advisors (this is the "100 pointcuts" check)
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(
            bean.getClass(), beanName, null);

        if (specificInterceptors != DO_NOT_PROXY) {
            this.advisedBeans.put(cacheKey, Boolean.TRUE);
            // Create proxy with ONLY the matching advisors (e.g., 5 out of 100)
            Object proxy = createProxy(bean.getClass(), beanName,
                specificInterceptors, new SingletonTargetSource(bean));
            return proxy;
        }

        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }
}
```

```java
// ═══ Level 2: Method-to-Interceptor chain — cached at FIRST CALL ═══
// AdvisedSupport.java

public class AdvisedSupport extends ProxyConfig {

    // The advisors attached to this proxy (subset of all 100)
    private List<Advisor> advisors = new ArrayList<>();
    //  e.g., [Advisor#3, #17, #42, #68, #91] — only 5 out of 100

    // Cache: method → interceptor chain
    private transient Map<MethodCacheKey, List<Object>> methodCache = 
        new ConcurrentHashMap<>(32);

    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
            Method method, Class<?> targetClass) {

        MethodCacheKey cacheKey = new MethodCacheKey(method);

        // FAST PATH: check cache
        List<Object> cached = this.methodCache.get(cacheKey);
        if (cached != null) {
            return cached;   // ← O(1) lookup. No pointcut matching.
        }

        // SLOW PATH: compute chain (only on FIRST call to this method)
        // Only checks the 5 advisors attached to this proxy, NOT all 100
        List<Object> chain = this.advisorChainFactory
            .getInterceptorsAndDynamicInterceptionAdvice(this, method, targetClass);

        this.methodCache.put(cacheKey, chain);   // Cache for future calls
        return chain;
    }
}
```

```java
// ═══ Level 3: Pointcut matching cache — ShadowMatch ═══
// AspectJExpressionPointcut.java

public class AspectJExpressionPointcut implements MethodMatcher {

    // Cache: Method → ShadowMatch (match result)
    private final Map<Method, ShadowMatch> shadowMatchCache = 
        new ConcurrentHashMap<>(32);

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        ShadowMatch shadowMatch = this.shadowMatchCache.get(method);

        if (shadowMatch == null) {
            // First time → evaluate and cache
            PointcutExpression pce = obtainPointcutExpression();
            shadowMatch = pce.matchesMethodExecution(method);
            //  ↑ AST-based evaluation (not string parsing!)
            this.shadowMatchCache.put(method, shadowMatch);
        }

        return shadowMatch.alwaysMatches();
    }
}
```

### Performance Numbers — What It Costs

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Performance Impact of 100 Pointcuts:                                            │
│                                                                                  │
│  ┌────────────────────────────────────────┬──────────────────────────────────────┐│
│  │ Phase                                  │ Cost                                 ││
│  ├────────────────────────────────────────┼──────────────────────────────────────┤│
│  │ Startup: Parse 100 pointcut strings    │ ~100-500ms (one-time)               ││
│  │ Startup: Match 100 pointcuts × N beans │ ~1-5ms per bean (one-time)          ││
│  │ First call: Build interceptor chain    │ ~0.01ms per method (one-time)       ││
│  │ Every call: Cache lookup + chain exec  │ ~0.001ms overhead per interceptor   ││
│  │ Every call: Target method execution    │ ONCE — same as without AOP          ││
│  └────────────────────────────────────────┴──────────────────────────────────────┘│
│                                                                                  │
│  Summary:                                                                        │
│    • Startup may be slightly slower with many pointcuts (one-time cost)          │
│    • Runtime overhead is NEGLIGIBLE — just HashMap lookups                        │
│    • Method executes EXACTLY ONCE regardless of matching pointcut count           │
│    • Interceptor chain runs in microseconds                                      │
│                                                                                  │
│  ONLY concern: if you have 50 advisors matching ONE method,                      │
│  the interceptor chain has 50 entries → 50 advice methods run (not 50 target     │
│  method calls). The target method still runs ONCE.                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### Complete Visual — 100 Pointcuts Scenario

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Scenario: 100 Advisors, OrderService.createOrder() called                       │
│                                                                                  │
│  ═══ STARTUP (happens ONCE) ═══                                                  │
│                                                                                  │
│  100 Advisors:                                                                   │
│  ┌──────────┐┌──────────┐┌──────────┐┌──────────┐     ┌──────────┐             │
│  │Advisor #1││Advisor #2││Advisor #3││Advisor #4│ ... │Advisor#100│             │
│  │service.* ││ctrl.*    ││@Loggable ││void only │     │repo.*    │             │
│  └─────┬────┘└─────┬────┘└─────┬────┘└─────┬────┘     └─────┬────┘             │
│        │           │           │           │                 │                    │
│  Check OrderService:                                                             │
│        │           │           │           │                 │                    │
│    Phase 1:     Phase 1:   Phase 1:    Phase 1:          Phase 1:                │
│    service.*    ctrl.*     any class   any class          repo.*                  │
│    MATCH ✓      FAIL ✗     PASS ✓      PASS ✓            FAIL ✗                 │
│        │                      │           │                                      │
│    Phase 2:               Phase 2:    Phase 2:                                   │
│    create*()              has @Log?   returns void?                               │
│    MATCH ✓                YES ✓       NO ✗                                       │
│        │                      │                                                  │
│  ═══════════════════════════════════                                              │
│  OrderService proxy gets: [Advisor#1, Advisor#3]  ← only 2 out of 100!          │
│  ═══════════════════════════════════                                              │
│                                                                                  │
│  ═══ FIRST CALL to createOrder() ═══                                             │
│                                                                                  │
│  Proxy checks its 2 advisors (NOT 100!):                                         │
│    Advisor#1: matches createOrder? → YES                                         │
│    Advisor#3: matches createOrder? → YES                                         │
│  chain = [BeforeInterceptor(#1), AroundInterceptor(#3)]                          │
│  Cached in methodCache ✓                                                         │
│                                                                                  │
│  ═══ EXECUTION ═══                                                               │
│                                                                                  │
│  AroundInterceptor(#3).invoke()                                                  │
│    ↓ your @Around code runs (before)                                             │
│    ↓ calls proceed()                                                             │
│      ↓ BeforeInterceptor(#1).invoke()                                            │
│        ↓ your @Before code runs                                                  │
│        ↓ calls proceed()                                                         │
│          ↓ ┌──────────────────────────────┐                                      │
│            │ TARGET: createOrder(req)     │  ← EXECUTES ONCE                     │
│            │ returns Order(id=1, ...)     │                                      │
│            └──────────────────────────────┘                                      │
│        ↓ returns Order                                                           │
│    ↓ your @Around code runs (after)                                              │
│    ↓ returns Order                                                               │
│  ↓                                                                               │
│  Client receives Order                                                           │
│                                                                                  │
│  ═══ SECOND CALL to createOrder() ═══                                            │
│                                                                                  │
│  Proxy: methodCache.get(createOrder) → [BeforeInterceptor, AroundInterceptor]    │
│  ← INSTANT (no pointcut matching at all, not even against the 2 advisors)        │
│  Execute chain → target runs ONCE → return                                       │
│                                                                                  │
│  Total pointcut evaluations for second call: ZERO                                │
│  Total target method executions: ONE                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Summary — Matching, Linking, Interception:                                      │
│                                                                                  │
│  ┌────────────────────┬──────────────────────┬───────────────────────────────────┐│
│  │ Phase              │ When It Happens      │ What Gets Cached                  ││
│  ├────────────────────┼──────────────────────┼───────────────────────────────────┤│
│  │ 1. MATCHING         │ Startup (bean        │ advisedBeans: bean → true/false   ││
│  │ (pointcut → bean)  │ creation)            │ Proxy stores only matching        ││
│  │                    │ ONCE per bean        │ advisors (e.g., 5 out of 100)    ││
│  ├────────────────────┼──────────────────────┼───────────────────────────────────┤│
│  │ 2. LINKING          │ First method call    │ methodCache: method → chain       ││
│  │ (advisor → method) │ ONCE per method      │ Only matching interceptors        ││
│  │                    │                      │ (e.g., 3 out of 5)               ││
│  ├────────────────────┼──────────────────────┼───────────────────────────────────┤│
│  │ 3. INTERCEPTION     │ Every method call    │ Uses cached chain from Level 2   ││
│  │ (execute chain)    │ EVERY TIME           │ O(1) cache lookup + chain exec   ││
│  │                    │                      │ Target method: ONCE              ││
│  └────────────────────┴──────────────────────┴───────────────────────────────────┘│
│                                                                                  │
│  Key Insight:                                                                    │
│    100 pointcuts → maybe 5 match this bean → maybe 3 match this method           │
│    → 3 interceptors run → target method runs ONCE                                │
│    → All cached → next call: 0 pointcut checks, same 3 interceptors, 1 execution │
└──────────────────────────────────────────────────────────────────────────────────┘
```

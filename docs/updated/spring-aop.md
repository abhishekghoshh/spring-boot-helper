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


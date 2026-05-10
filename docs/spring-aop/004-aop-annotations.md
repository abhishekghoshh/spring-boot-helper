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


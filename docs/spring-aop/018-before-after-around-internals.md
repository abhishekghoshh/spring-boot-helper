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


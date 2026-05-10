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


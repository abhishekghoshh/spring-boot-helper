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


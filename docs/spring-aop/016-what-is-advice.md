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


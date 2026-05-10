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


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


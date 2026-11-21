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

### Setup Snippet

Add Spring AOP to your Boot project:

```xml
<!-- Maven: spring-boot-starter-aop -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

```gradle
// Gradle: spring-boot-starter-aop
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

Minimal `@Aspect` skeleton (with imports — no extra config needed, Boot auto-detects it):

```java
package com.app.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.app.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Calling: {}", joinPoint.getSignature());
        Object result = joinPoint.proceed();
        log.info("Returned from: {} with result: {}", joinPoint.getSignature(), result);
        return result;
    }
}
```

### 5 Pitfalls to Avoid

1. **Missing `@Component` on `@Aspect`:** `@Aspect` alone is not a bean — without `@Component` (or explicit bean registration) Spring never applies the advice.
2. **Forgetting `proceed()` in `@Around`:** if you don't call `joinPoint.proceed()`, the target method never runs (only skip it intentionally, e.g. caching/rate-limit short-circuit).
3. **Overly broad pointcuts:** `execution(* *.*(..))` advises everything including Spring internals — slow startup, noisy logs, surprise recursion. Narrow to `com.app.service..*` or `@annotation`.
4. **Self-invocation bypass:** `this.internalMethod()` skips the proxy, so advice is silently ignored. Inject self-proxy or refactor to another bean.
5. **Swallowing exceptions in `@Around`:** catching `Throwable` without rethrowing breaks `@Transactional` rollback and hides failures — always rethrow unless you intentionally convert the exception.

### Interview Q&A

**Q1: What setup is needed to use Spring AOP in Spring Boot?**
Add `spring-boot-starter-aop`, write an `@Aspect` + `@Component` class with advice methods, and Boot auto-creates proxies at startup. No `@EnableAspectJAutoProxy` needed (Boot enables it by default).

**Q2: Why is `proceed()` mandatory in `@Around` advice?**
`@Around` wraps the target call — `proceed()` is what actually invokes the target method (and the rest of the advice chain). Without it the business method is skipped; you also control its arguments, return value, and exceptions from that one spot.

**Q3: What is the most common reason an aspect "does nothing"?**
Either the aspect isn't a bean (missing `@Component`), the pointcut doesn't match (wrong package/expression), or the call is a self-invocation (`this.method()`) that bypasses the proxy. Checking bean creation + pointcut match + call path fixes 90% of cases.

---


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

### Example: Logging Aspect (with imports)

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
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        log.info("Entering: {} with args: {}", joinPoint.getSignature(), joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            log.info("Exiting: {} took {}ms", joinPoint.getSignature(),
                    System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("Exception in: {} — {}", joinPoint.getSignature(), e.getMessage());
            throw e;
        }
    }
}
```

> ⚠️ **Self-Invocation Caveat:** this aspect only runs when the call goes **through the Spring proxy**. An internal call like `this.createOrder()` inside the same bean bypasses the proxy, so **no logging, timing, or exception handling above will execute**. Fix: move the callee to another bean, or self-inject the proxy (`@Autowired OrderService self; self.createOrder();`), or use AspectJ load-time weaving if true self-interception is required.

### Interview Q&A

**Q1: How does `@Transactional` use AOP in real applications?**
`@Transactional` is implemented as an AOP advice (`TransactionInterceptor`). The proxy starts a transaction before the method, commits on success, and rolls back on unchecked exceptions — so every service method gets declarative transaction boundaries without manual `begin/commit/rollback` code.

**Q2: Why doesn't my logging aspect fire on internal method calls?**
Because Spring AOP is proxy-based: only external calls routed through the proxy are intercepted. `this.helperMethod()` calls the raw target directly, skipping the proxy and its advice chain entirely.

---


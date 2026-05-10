## What If We Have 100s of Pointcuts? — Performance & Caching

**Question:** If there are 100 pointcut expressions and I call a method, does Spring match against all 100 pointcuts every time? Will the method execute 100 times?

**Answer:** **NO** to both. Spring uses **multi-level caching** to ensure matching happens **once** (at startup or first call), and the method executes **only once** regardless of how many pointcuts match.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  100 Pointcuts — What ACTUALLY Happens:                                          │
│                                                                                  │
│  ═══ STARTUP TIME (ONCE) ═══                                                     │
│                                                                                  │
│  For each bean (e.g., OrderService):                                             │
│    Check 100 advisors' pointcuts against OrderService's methods                  │
│    → Phase 1 (class-level): 70 eliminated immediately (wrong package/class)     │
│    → Phase 2 (method-level): 30 checked, 5 match                                │
│    → Result: OrderService proxy gets [Advisor#3, #17, #42, #68, #91]            │
│    → Cached. NEVER re-computed.                                                  │
│                                                                                  │
│  ═══ FIRST METHOD CALL (ONCE PER METHOD) ═══                                     │
│                                                                                  │
│  orderService.createOrder(req) called for the first time:                        │
│    Proxy has 5 advisors. Check which match THIS method:                          │
│    → Advisor#3:  matches createOrder → YES                                       │
│    → Advisor#17: matches createOrder → YES                                       │
│    → Advisor#42: matches get* only  → NO                                        │
│    → Advisor#68: matches createOrder → YES                                       │
│    → Advisor#91: matches void only  → NO                                        │
│    → Result: chain = [Interceptor#3, Interceptor#17, Interceptor#68]             │
│    → Cached in methodCache. NEVER re-computed.                                   │
│                                                                                  │
│  ═══ EVERY SUBSEQUENT CALL ═══                                                   │
│                                                                                  │
│  orderService.createOrder(req) called again:                                     │
│    1. Proxy looks up methodCache → chain found instantly (O(1) HashMap)          │
│    2. Execute chain: 3 interceptors run                                          │
│    3. Target method executes ONCE                                                │
│    4. Return result                                                              │
│                                                                                  │
│  NO re-matching with 100 pointcuts. NO 100x execution.                           │
│  Just 3 interceptors + 1 target method execution.                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### The Method is NEVER Executed Multiple Times

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  "Will the method execute 100 times?" — NO!                                      │
│                                                                                  │
│  Even if 100 advisors match, the method executes EXACTLY ONCE.                   │
│  Here's why:                                                                     │
│                                                                                  │
│  The interceptor chain is like nested function calls:                             │
│                                                                                  │
│    Interceptor#1 (Around) {                                                      │
│      // before                                                                   │
│      Interceptor#2 (Before) {                                                    │
│        // advice runs                                                            │
│        Interceptor#3 (After) {                                                   │
│          // try {                                                                │
│            TARGET METHOD ← executes ONCE                                         │
│          // } finally { advice runs }                                            │
│        }                                                                         │
│      }                                                                           │
│      // after                                                                    │
│    }                                                                             │
│                                                                                  │
│  The chain is NESTED, not REPEATED:                                              │
│    ✗ WRONG: method() → method() → method() → method()  (100 times)              │
│    ✓ RIGHT: advice1 → advice2 → advice3 → method() → back through chain         │
│                                                                                  │
│  proceed() moves to the NEXT interceptor, not re-executes the method.            │
│  Only the LAST proceed() in the chain calls the actual target method.            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### Caching Architecture — Library Code Walkthrough

```java
// ═══ Level 1: Advisor-to-Bean matching — cached at STARTUP ═══
// AbstractAutoProxyCreator.java

public abstract class AbstractAutoProxyCreator {

    // Cache: tracks which beans have been checked
    private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);
    //  "orderService" → true (proxy created)
    //  "reportService" → false (no proxy needed)
    //  Once set, NEVER re-checked

    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // Already checked? Return immediately
        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;  // ← FAST: no re-checking
        }

        // Find matching advisors (this is the "100 pointcuts" check)
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(
            bean.getClass(), beanName, null);

        if (specificInterceptors != DO_NOT_PROXY) {
            this.advisedBeans.put(cacheKey, Boolean.TRUE);
            // Create proxy with ONLY the matching advisors (e.g., 5 out of 100)
            Object proxy = createProxy(bean.getClass(), beanName,
                specificInterceptors, new SingletonTargetSource(bean));
            return proxy;
        }

        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }
}
```

```java
// ═══ Level 2: Method-to-Interceptor chain — cached at FIRST CALL ═══
// AdvisedSupport.java

public class AdvisedSupport extends ProxyConfig {

    // The advisors attached to this proxy (subset of all 100)
    private List<Advisor> advisors = new ArrayList<>();
    //  e.g., [Advisor#3, #17, #42, #68, #91] — only 5 out of 100

    // Cache: method → interceptor chain
    private transient Map<MethodCacheKey, List<Object>> methodCache = 
        new ConcurrentHashMap<>(32);

    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
            Method method, Class<?> targetClass) {

        MethodCacheKey cacheKey = new MethodCacheKey(method);

        // FAST PATH: check cache
        List<Object> cached = this.methodCache.get(cacheKey);
        if (cached != null) {
            return cached;   // ← O(1) lookup. No pointcut matching.
        }

        // SLOW PATH: compute chain (only on FIRST call to this method)
        // Only checks the 5 advisors attached to this proxy, NOT all 100
        List<Object> chain = this.advisorChainFactory
            .getInterceptorsAndDynamicInterceptionAdvice(this, method, targetClass);

        this.methodCache.put(cacheKey, chain);   // Cache for future calls
        return chain;
    }
}
```

```java
// ═══ Level 3: Pointcut matching cache — ShadowMatch ═══
// AspectJExpressionPointcut.java

public class AspectJExpressionPointcut implements MethodMatcher {

    // Cache: Method → ShadowMatch (match result)
    private final Map<Method, ShadowMatch> shadowMatchCache = 
        new ConcurrentHashMap<>(32);

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        ShadowMatch shadowMatch = this.shadowMatchCache.get(method);

        if (shadowMatch == null) {
            // First time → evaluate and cache
            PointcutExpression pce = obtainPointcutExpression();
            shadowMatch = pce.matchesMethodExecution(method);
            //  ↑ AST-based evaluation (not string parsing!)
            this.shadowMatchCache.put(method, shadowMatch);
        }

        return shadowMatch.alwaysMatches();
    }
}
```

### Performance Numbers — What It Costs

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Performance Impact of 100 Pointcuts:                                            │
│                                                                                  │
│  ┌────────────────────────────────────────┬──────────────────────────────────────┐│
│  │ Phase                                  │ Cost                                 ││
│  ├────────────────────────────────────────┼──────────────────────────────────────┤│
│  │ Startup: Parse 100 pointcut strings    │ ~100-500ms (one-time)               ││
│  │ Startup: Match 100 pointcuts × N beans │ ~1-5ms per bean (one-time)          ││
│  │ First call: Build interceptor chain    │ ~0.01ms per method (one-time)       ││
│  │ Every call: Cache lookup + chain exec  │ ~0.001ms overhead per interceptor   ││
│  │ Every call: Target method execution    │ ONCE — same as without AOP          ││
│  └────────────────────────────────────────┴──────────────────────────────────────┘│
│                                                                                  │
│  Summary:                                                                        │
│    • Startup may be slightly slower with many pointcuts (one-time cost)          │
│    • Runtime overhead is NEGLIGIBLE — just HashMap lookups                        │
│    • Method executes EXACTLY ONCE regardless of matching pointcut count           │
│    • Interceptor chain runs in microseconds                                      │
│                                                                                  │
│  ONLY concern: if you have 50 advisors matching ONE method,                      │
│  the interceptor chain has 50 entries → 50 advice methods run (not 50 target     │
│  method calls). The target method still runs ONCE.                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### Complete Visual — 100 Pointcuts Scenario

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Scenario: 100 Advisors, OrderService.createOrder() called                       │
│                                                                                  │
│  ═══ STARTUP (happens ONCE) ═══                                                  │
│                                                                                  │
│  100 Advisors:                                                                   │
│  ┌──────────┐┌──────────┐┌──────────┐┌──────────┐     ┌──────────┐             │
│  │Advisor #1││Advisor #2││Advisor #3││Advisor #4│ ... │Advisor#100│             │
│  │service.* ││ctrl.*    ││@Loggable ││void only │     │repo.*    │             │
│  └─────┬────┘└─────┬────┘└─────┬────┘└─────┬────┘     └─────┬────┘             │
│        │           │           │           │                 │                    │
│  Check OrderService:                                                             │
│        │           │           │           │                 │                    │
│    Phase 1:     Phase 1:   Phase 1:    Phase 1:          Phase 1:                │
│    service.*    ctrl.*     any class   any class          repo.*                  │
│    MATCH ✓      FAIL ✗     PASS ✓      PASS ✓            FAIL ✗                 │
│        │                      │           │                                      │
│    Phase 2:               Phase 2:    Phase 2:                                   │
│    create*()              has @Log?   returns void?                               │
│    MATCH ✓                YES ✓       NO ✗                                       │
│        │                      │                                                  │
│  ═══════════════════════════════════                                              │
│  OrderService proxy gets: [Advisor#1, Advisor#3]  ← only 2 out of 100!          │
│  ═══════════════════════════════════                                              │
│                                                                                  │
│  ═══ FIRST CALL to createOrder() ═══                                             │
│                                                                                  │
│  Proxy checks its 2 advisors (NOT 100!):                                         │
│    Advisor#1: matches createOrder? → YES                                         │
│    Advisor#3: matches createOrder? → YES                                         │
│  chain = [BeforeInterceptor(#1), AroundInterceptor(#3)]                          │
│  Cached in methodCache ✓                                                         │
│                                                                                  │
│  ═══ EXECUTION ═══                                                               │
│                                                                                  │
│  AroundInterceptor(#3).invoke()                                                  │
│    ↓ your @Around code runs (before)                                             │
│    ↓ calls proceed()                                                             │
│      ↓ BeforeInterceptor(#1).invoke()                                            │
│        ↓ your @Before code runs                                                  │
│        ↓ calls proceed()                                                         │
│          ↓ ┌──────────────────────────────┐                                      │
│            │ TARGET: createOrder(req)     │  ← EXECUTES ONCE                     │
│            │ returns Order(id=1, ...)     │                                      │
│            └──────────────────────────────┘                                      │
│        ↓ returns Order                                                           │
│    ↓ your @Around code runs (after)                                              │
│    ↓ returns Order                                                               │
│  ↓                                                                               │
│  Client receives Order                                                           │
│                                                                                  │
│  ═══ SECOND CALL to createOrder() ═══                                            │
│                                                                                  │
│  Proxy: methodCache.get(createOrder) → [BeforeInterceptor, AroundInterceptor]    │
│  ← INSTANT (no pointcut matching at all, not even against the 2 advisors)        │
│  Execute chain → target runs ONCE → return                                       │
│                                                                                  │
│  Total pointcut evaluations for second call: ZERO                                │
│  Total target method executions: ONE                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Summary — Matching, Linking, Interception:                                      │
│                                                                                  │
│  ┌────────────────────┬──────────────────────┬───────────────────────────────────┐│
│  │ Phase              │ When It Happens      │ What Gets Cached                  ││
│  ├────────────────────┼──────────────────────┼───────────────────────────────────┤│
│  │ 1. MATCHING         │ Startup (bean        │ advisedBeans: bean → true/false   ││
│  │ (pointcut → bean)  │ creation)            │ Proxy stores only matching        ││
│  │                    │ ONCE per bean        │ advisors (e.g., 5 out of 100)    ││
│  ├────────────────────┼──────────────────────┼───────────────────────────────────┤│
│  │ 2. LINKING          │ First method call    │ methodCache: method → chain       ││
│  │ (advisor → method) │ ONCE per method      │ Only matching interceptors        ││
│  │                    │                      │ (e.g., 3 out of 5)               ││
│  ├────────────────────┼──────────────────────┼───────────────────────────────────┤│
│  │ 3. INTERCEPTION     │ Every method call    │ Uses cached chain from Level 2   ││
│  │ (execute chain)    │ EVERY TIME           │ O(1) cache lookup + chain exec   ││
│  │                    │                      │ Target method: ONCE              ││
│  └────────────────────┴──────────────────────┴───────────────────────────────────┘│
│                                                                                  │
│  Key Insight:                                                                    │
│    100 pointcuts → maybe 5 match this bean → maybe 3 match this method           │
│    → 3 interceptors run → target method runs ONCE                                │
│    → All cached → next call: 0 pointcut checks, same 3 interceptors, 1 execution │
└──────────────────────────────────────────────────────────────────────────────────┘
```


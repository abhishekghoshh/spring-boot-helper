## How Matching, Linking, and Interception Work Together

This section explains the **runtime flow** — what happens at the moment a proxied method is actually called. The three phases are: **matching** (already done at startup), **linking** (building the interceptor chain), and **interception** (executing the chain).

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Three Phases of AOP Method Interception:                                        │
│                                                                                  │
│  Phase 1: MATCHING (done at STARTUP — cached)                                    │
│    "Which advisors apply to which beans and methods?"                            │
│    → Already done during proxy creation (Step 6 in startup flow)                │
│    → Results cached in the proxy object                                          │
│                                                                                  │
│  Phase 2: LINKING (done at FIRST METHOD CALL — then cached)                      │
│    "For THIS specific method, build an ordered interceptor chain"                │
│    → Filters matching advisors to the specific method being called               │
│    → Orders them by advice type and @Order annotation                            │
│    → Caches the chain: Map<Method, List<MethodInterceptor>>                      │
│                                                                                  │
│  Phase 3: INTERCEPTION (done on EVERY method call)                               │
│    "Execute the interceptor chain"                                               │
│    → Retrieve cached chain for this method                                       │
│    → Execute chain using Chain of Responsibility pattern                         │
│    → Each interceptor calls proceed() to pass to the next                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Runtime Method Call — Complete Flow:                                             │
│                                                                                  │
│  // In your controller:                                                          │
│  Order order = orderService.createOrder(req);                                    │
│  // orderService is actually a CGLIB PROXY                                       │
│                                                                                  │
│  Step 1: Proxy intercepts the call                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │ CglibAopProxy.DynamicAdvisedInterceptor.intercept(                          │ │
│  │     proxy, method=createOrder, args=[req], methodProxy)                     │ │
│  └─────────────────────────────────────┬───────────────────────────────────────┘ │
│                                        │                                         │
│  Step 2: Get interceptor chain for this method (LINKING)                         │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │ List<Object> chain = this.advised.getInterceptorsAndDynamicInterception     │ │
│  │     Advice(method, targetClass);                                            │ │
│  │                                                                             │ │
│  │ What happens inside:                                                        │ │
│  │   1. Check cache: methodCache.get(method)                                   │ │
│  │      → If cached: return immediately (fast path)                            │ │
│  │      → If NOT cached: compute and cache                                     │ │
│  │                                                                             │ │
│  │   2. For each Advisor attached to this proxy:                               │ │
│  │      a. Does the advisor's pointcut match THIS method?                      │ │
│  │         → pointcut.matches(method, targetClass)                             │ │
│  │         → Uses cached ShadowMatch from startup                              │ │
│  │      b. If match: convert Advice → MethodInterceptor                        │ │
│  │         → Add to the chain                                                  │ │
│  │                                                                             │ │
│  │   3. Cache the chain for next time:                                         │ │
│  │      methodCache.put(method, chain)                                         │ │
│  │                                                                             │ │
│  │ Result: chain = [AroundInterceptor, BeforeInterceptor, AfterInterceptor]    │ │
│  └─────────────────────────────────────┬───────────────────────────────────────┘ │
│                                        │                                         │
│  Step 3: Execute the chain (INTERCEPTION)                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │ if (chain.isEmpty()) {                                                      │ │
│  │     // No interceptors → call target directly                               │ │
│  │     return methodProxy.invoke(target, args);                                │ │
│  │ }                                                                           │ │
│  │                                                                             │ │
│  │ // Has interceptors → create MethodInvocation and execute                   │ │
│  │ MethodInvocation invocation = new CglibMethodInvocation(                    │ │
│  │     proxy, target, method, args, targetClass, chain, methodProxy);          │ │
│  │ return invocation.proceed();   ← starts the chain                          │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── The core chain execution engine ───
// ReflectiveMethodInvocation.java (actual Spring class, simplified)

public class ReflectiveMethodInvocation implements ProxyMethodInvocation {

    protected final Object proxy;
    protected final Object target;
    protected final Method method;
    protected Object[] arguments;
    private final List<Object> interceptorsAndDynamicMethodMatchers;

    private int currentInterceptorIndex = -1;  // ← tracks position in chain

    @Override
    public Object proceed() throws Throwable {
        // If we've reached the end of the chain → call the actual target method
        if (this.currentInterceptorIndex == 
            this.interceptorsAndDynamicMethodMatchers.size() - 1) {

            return invokeJoinpoint();
            //  ↑ Calls: target.createOrder(req) — the REAL method
        }

        // Move to the next interceptor in the chain
        Object interceptorOrInterceptionAdvice =
            this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);

        if (interceptorOrInterceptionAdvice instanceof MethodInterceptor) {
            MethodInterceptor mi = (MethodInterceptor) interceptorOrInterceptionAdvice;

            // Call the interceptor, passing THIS invocation so it can call proceed()
            return mi.invoke(this);
            //  ↑ The interceptor runs its advice and then calls this.proceed()
            //    which increments the index and moves to the next interceptor
        }

        // Skip and proceed to next
        return proceed();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Chain Execution — Visual Walkthrough:                                           │
│                                                                                  │
│  chain = [AroundInterceptor, BeforeInterceptor, AfterInterceptor]                │
│  currentIndex starts at -1                                                       │
│                                                                                  │
│  ┌─── Call #1: proceed() ──────────────────────────────────────────────────────┐ │
│  │ currentIndex: -1 → 0                                                        │ │
│  │ chain[0] = AroundInterceptor                                                │ │
│  │ AroundInterceptor.invoke(this):                                             │ │
│  │   YOUR @Around method runs (before proceed)                                 │ │
│  │   calls pjp.proceed() ──────────────────────────┐                           │ │
│  │                                                   │                          │ │
│  │  ┌─── Call #2: proceed() ───────────────────────┐│                          │ │
│  │  │ currentIndex: 0 → 1                          ││                          │ │
│  │  │ chain[1] = BeforeInterceptor                 ││                          │ │
│  │  │ BeforeInterceptor.invoke(this):              ││                          │ │
│  │  │   YOUR @Before method runs                   ││                          │ │
│  │  │   calls mi.proceed() ──────────────┐         ││                          │ │
│  │  │                                     │        ││                          │ │
│  │  │  ┌─── Call #3: proceed() ─────────┐│        ││                          │ │
│  │  │  │ currentIndex: 1 → 2            ││        ││                          │ │
│  │  │  │ chain[2] = AfterInterceptor    ││        ││                          │ │
│  │  │  │ AfterInterceptor.invoke(this): ││        ││                          │ │
│  │  │  │   try {                        ││        ││                          │ │
│  │  │  │     mi.proceed() ─────────┐    ││        ││                          │ │
│  │  │  │                            │   ││        ││                          │ │
│  │  │  │  ┌─── Call #4: proceed() ┐│   ││        ││                          │ │
│  │  │  │  │ currentIndex: 2 → 3   ││   ││        ││                          │ │
│  │  │  │  │ index == size - 1     ││   ││        ││                          │ │
│  │  │  │  │ → invokeJoinpoint()   ││   ││        ││                          │ │
│  │  │  │  │ → TARGET METHOD RUNS  ││   ││        ││                          │ │
│  │  │  │  │ → returns Order       ││   ││        ││                          │ │
│  │  │  │  └───────────────────────┘│   ││        ││                          │ │
│  │  │  │                            │   ││        ││                          │ │
│  │  │  │   } finally {             │   ││        ││                          │ │
│  │  │  │     YOUR @After runs      │   ││        ││                          │ │
│  │  │  │   }                        │   ││        ││                          │ │
│  │  │  │   returns Order ───────────┘   ││        ││                          │ │
│  │  │  └────────────────────────────────┘│        ││                          │ │
│  │  │   returns Order ───────────────────┘        ││                          │ │
│  │  └─────────────────────────────────────────────┘│                          │ │
│  │   YOUR @Around method runs (after proceed)      │                          │ │
│  │   returns Order ────────────────────────────────┘                          │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  Final: Order returned to Client                                                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── How the proxy gets the interceptor chain for a method ───
// AdvisedSupport.java (simplified)

public class AdvisedSupport {

    // Cache: method → interceptor chain
    private transient Map<MethodCacheKey, List<Object>> methodCache;

    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
            Method method, Class<?> targetClass) {

        MethodCacheKey cacheKey = new MethodCacheKey(method);

        // Check cache first
        List<Object> cached = this.methodCache.get(cacheKey);
        if (cached != null) {
            return cached;  // ← FAST: return cached chain
        }

        // Not cached → compute the chain
        List<Object> chain = this.advisorChainFactory
            .getInterceptorsAndDynamicInterceptionAdvice(
                this, method, targetClass);

        // Cache for next time
        this.methodCache.put(cacheKey, chain);
        return chain;
    }
}

// DefaultAdvisorChainFactory.java — builds the chain
public class DefaultAdvisorChainFactory implements AdvisorChainFactory {

    @Override
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
            Advised config, Method method, Class<?> targetClass) {

        List<Object> interceptorList = new ArrayList<>();
        AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();

        // For each advisor attached to this proxy:
        for (Advisor advisor : config.getAdvisors()) {
            if (advisor instanceof PointcutAdvisor) {
                PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;

                // Check if this advisor's pointcut matches THIS method
                if (pointcutAdvisor.getPointcut().getClassFilter().matches(targetClass) &&
                    pointcutAdvisor.getPointcut().getMethodMatcher()
                        .matches(method, targetClass)) {

                    // Convert Advice → MethodInterceptor(s)
                    MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
                    interceptorList.addAll(Arrays.asList(interceptors));
                }
            }
        }

        return interceptorList;
    }
}
```

---


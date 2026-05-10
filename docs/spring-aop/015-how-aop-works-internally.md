## How AOP Works Internally — Step by Step from Application Startup

This section traces the **complete lifecycle** of Spring AOP — from the moment your Spring Boot application starts, through aspect discovery, pointcut parsing, bean scanning, eligibility matching, proxy creation, and finally method interception at runtime.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  AOP Internal Lifecycle — High-Level Overview (7 Steps):                         │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │  STARTUP PHASE (happens ONCE at application boot)                           │ │
│  │                                                                             │ │
│  │  Step 1: Spring Boot App Starts                                             │ │
│  │    ↓                                                                        │ │
│  │  Step 2: @EnableAspectJAutoProxy activates AOP infrastructure               │ │
│  │    ↓                                                                        │ │
│  │  Step 3: Discover @Aspect classes and extract @Pointcut/@Before/etc.        │ │
│  │    ↓                                                                        │ │
│  │  Step 4: Parse pointcut expressions with PointcutParser                     │ │
│  │    ↓                                                                        │ │
│  │  Step 5: Store parsed pointcuts in efficient cache (PointcutExpression)      │ │
│  │    ↓                                                                        │ │
│  │  Step 6: For EACH bean → check if any pointcut matches its methods          │ │
│  │    ↓                                                                        │ │
│  │  Step 7: If match found → create Proxy (JDK Dynamic or CGLIB)              │ │
│  │    ↓                                                                        │ │
│  │  Register PROXY (not original bean) in ApplicationContext                   │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │  RUNTIME PHASE (happens on EVERY method call)                               │ │
│  │                                                                             │ │
│  │  Client calls proxy.method()                                                │ │
│  │    ↓                                                                        │ │
│  │  Proxy looks up cached interceptor chain for this method                    │ │
│  │    ↓                                                                        │ │
│  │  Execute advice chain (@Before → proceed → @After/@AfterReturning)          │ │
│  │    ↓                                                                        │ │
│  │  Return result to client                                                    │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 1: Spring Boot Application Starts

When you run `SpringApplication.run(MyApp.class)`, the following happens:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @SpringBootApplication                                                          │
│  public class MyApp {                                                            │
│      public static void main(String[] args) {                                    │
│          SpringApplication.run(MyApp.class, args);    ← ENTRY POINT             │
│      }                                                                           │
│  }                                                                               │
│                                                                                  │
│  What happens internally:                                                        │
│                                                                                  │
│  1. SpringApplication.run()                                                      │
│     ↓                                                                            │
│  2. Creates AnnotationConfigApplicationContext                                    │
│     ↓                                                                            │
│  3. Scans for @Configuration, @Component, @Service, @Controller, etc.            │
│     ↓                                                                            │
│  4. Processes @EnableAspectJAutoProxy (auto-included via spring-boot-starter-aop) │
│     ↓                                                                            │
│  5. Registers AnnotationAwareAspectJAutoProxyCreator as a BeanPostProcessor      │
│     ↓                                                                            │
│  6. For EACH bean being created → this BeanPostProcessor checks for AOP          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── @SpringBootApplication includes @EnableAutoConfiguration ───
// Spring Boot auto-configures AOP via AopAutoConfiguration.class

// What @EnableAspectJAutoProxy does internally:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AspectJAutoProxyRegistrar.class)   // ← THIS registers the key bean
public @interface EnableAspectJAutoProxy {
    boolean proxyTargetClass() default false;  // false = JDK proxy, true = CGLIB
    boolean exposeProxy() default false;
}

// AspectJAutoProxyRegistrar registers:
//   AnnotationAwareAspectJAutoProxyCreator
//   ↑ This is a BeanPostProcessor — it intercepts every bean creation
```

**The key class: `AnnotationAwareAspectJAutoProxyCreator`**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Class Hierarchy of the AOP Auto-Proxy Creator:                                  │
│                                                                                  │
│  BeanPostProcessor (interface)                                                   │
│    └── AbstractAutoProxyCreator                                                  │
│          └── AbstractAdvisorAutoProxyCreator                                     │
│                └── AspectJAwareAdvisorAutoProxyCreator                            │
│                      └── AnnotationAwareAspectJAutoProxyCreator   ← THE ONE      │
│                                                                                  │
│  This class:                                                                     │
│    • Is a BeanPostProcessor → called for EVERY bean being created                │
│    • Knows how to find @Aspect classes                                            │
│    • Knows how to parse AspectJ pointcut expressions                             │
│    • Knows how to create JDK/CGLIB proxies                                       │
│    • Decides whether each bean needs a proxy or not                              │
│                                                                                  │
│  BeanPostProcessor has 2 methods:                                                │
│    postProcessBeforeInitialization(bean, name) → called BEFORE @PostConstruct    │
│    postProcessAfterInitialization(bean, name)  → called AFTER @PostConstruct     │
│                                                                                  │
│  AOP proxy creation happens in postProcessAfterInitialization()                  │
│  (after the bean is fully initialized but before it's put in the context)        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Simplified view of what AnnotationAwareAspectJAutoProxyCreator does:

public class AnnotationAwareAspectJAutoProxyCreator extends AbstractAutoProxyCreator {

    // Called for EVERY bean after it's fully initialized
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean != null) {
            Object cacheKey = getCacheKey(bean.getClass(), beanName);

            // Check if this bean should be proxied
            if (!this.earlyProxyReferences.contains(cacheKey)) {
                return wrapIfNecessary(bean, beanName, cacheKey);
                //      ↑ THIS is where the magic happens
            }
        }
        return bean;
    }

    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // 1. Find all Advisors (aspects + pointcuts) that apply to this bean
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(
            bean.getClass(), beanName, null
        );

        if (specificInterceptors != DO_NOT_PROXY) {
            // 2. Create a proxy wrapping the original bean
            Object proxy = createProxy(
                bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean)
            );

            // 3. Return proxy INSTEAD of original bean
            return proxy;
        }

        // No matching pointcuts → return original bean (no proxy)
        return bean;
    }
}
```

---

### Step 2: Discover @Aspect Classes and Extract Advisors

Once the `AnnotationAwareAspectJAutoProxyCreator` is active, it scans the ApplicationContext for all beans annotated with `@Aspect`.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Aspect Discovery Flow:                                                          │
│                                                                                  │
│  ApplicationContext                                                               │
│    │                                                                             │
│    ├── OrderService (bean) ─── @Service                                          │
│    ├── PaymentService (bean) ─── @Service                                        │
│    ├── OrderController (bean) ─── @RestController                                │
│    ├── LoggingAspect (bean) ─── @Aspect @Component    ← FOUND!                  │
│    ├── PerformanceAspect (bean) ─── @Aspect @Component ← FOUND!                 │
│    ├── SecurityAspect (bean) ─── @Aspect @Component    ← FOUND!                 │
│    └── ...                                                                       │
│                                                                                  │
│  For EACH @Aspect class, extract:                                                │
│    1. All @Before methods → becomes a "BeforeAdvice" advisor                     │
│    2. All @After methods → becomes an "AfterAdvice" advisor                      │
│    3. All @Around methods → becomes an "AroundAdvice" advisor                    │
│    4. All @AfterReturning methods → becomes "AfterReturningAdvice" advisor       │
│    5. All @AfterThrowing methods → becomes "AfterThrowingAdvice" advisor         │
│    6. All @Pointcut methods → stored as named pointcut definitions               │
│                                                                                  │
│  Each Advisor = Pointcut Expression + Advice Method                              │
│                                                                                  │
│  Example:                                                                        │
│    @Before("execution(* com.app.service.*.*(..))")                               │
│    public void logBefore(JoinPoint jp) { ... }                                   │
│                                                                                  │
│    → Advisor {                                                                   │
│        pointcut: "execution(* com.app.service.*.*(..))"                          │
│        advice:   logBefore method reference                                      │
│        type:     BEFORE                                                          │
│      }                                                                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── The actual Spring class that discovers Aspects ───
// BeanFactoryAspectJAdvisorsBuilder.java (simplified)

public class BeanFactoryAspectJAdvisorsBuilder {

    public List<Advisor> buildAspectJAdvisors() {
        // 1. Get ALL bean names from the ApplicationContext
        String[] beanNames = this.beanFactory.getBeanNamesForType(Object.class);

        List<Advisor> advisors = new ArrayList<>();

        for (String beanName : beanNames) {
            Class<?> beanType = this.beanFactory.getType(beanName);

            // 2. Check: does this class have @Aspect annotation?
            if (this.advisorFactory.isAspect(beanType)) {
                //  ↑ Uses AnnotationUtils.findAnnotation(beanType, Aspect.class)

                AspectMetadata metadata = new AspectMetadata(beanType, beanName);

                // 3. Extract all advisor methods from this @Aspect class
                List<Advisor> classAdvisors = this.advisorFactory
                    .getAdvisors(new BeanFactoryAspectInstanceFactory(
                        this.beanFactory, beanName));

                advisors.addAll(classAdvisors);
            }
        }

        return advisors;  // All advisors from ALL @Aspect beans
    }
}
```

```java
// ─── How each @Before/@After/@Around method becomes an Advisor ───
// ReflectiveAspectJAdvisorFactory.java (simplified)

public class ReflectiveAspectJAdvisorFactory {

    public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory factory) {
        Class<?> aspectClass = factory.getAspectMetadata().getAspectClass();
        List<Advisor> advisors = new ArrayList<>();

        // Get all methods from the @Aspect class
        for (Method method : getAdvisorMethods(aspectClass)) {

            // Check for advice annotations: @Before, @After, @Around, etc.
            Advisor advisor = getAdvisor(method, factory);

            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        return advisors;
    }

    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
        List<Method> methods = new ArrayList<>();
        ReflectionUtils.doWithMethods(aspectClass, methods::add,
            // Filter: only methods with AOP annotations
            method -> AnnotationUtils.getAnnotation(method, Pointcut.class) == null
                // Skip @Pointcut methods — they are just definitions, not advice
        );

        // Sort by annotation type order: @Around → @Before → @After →
        //                                 @AfterReturning → @AfterThrowing
        methods.sort(METHOD_COMPARATOR);
        return methods;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Advisor Discovery — What Gets Created:                                          │
│                                                                                  │
│  From LoggingAspect:                                                             │
│  ┌───────────────────────────────────────────────────────────────────────────┐   │
│  │ @Aspect @Component                                                        │   │
│  │ public class LoggingAspect {                                              │   │
│  │                                                                           │   │
│  │   @Pointcut("execution(* com.app.service.*.*(..))")                       │   │
│  │   public void serviceLayer() {}                                           │   │
│  │                       ↓ stored as named pointcut definition               │   │
│  │                                                                           │   │
│  │   @Before("serviceLayer()")                                               │   │
│  │   public void logBefore(JoinPoint jp) { ... }                             │   │
│  │                       ↓ Advisor #1 { pointcut + beforeAdvice }            │   │
│  │                                                                           │   │
│  │   @AfterReturning(pointcut = "serviceLayer()", returning = "r")           │   │
│  │   public void logAfter(JoinPoint jp, Object r) { ... }                    │   │
│  │                       ↓ Advisor #2 { pointcut + afterReturningAdvice }    │   │
│  │                                                                           │   │
│  │   @Around("@annotation(com.app.annotation.Timed)")                        │   │
│  │   public Object time(ProceedingJoinPoint pjp) { ... }                     │   │
│  │                       ↓ Advisor #3 { pointcut + aroundAdvice }            │   │
│  └───────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Total Advisors collected: [Advisor#1, Advisor#2, Advisor#3, ...]                │
│  These are stored in memory and reused for every bean check.                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 3: Parse Pointcut Expressions with PointcutParser

Each advice annotation contains a **pointcut expression string** like `"execution(* com.app.service.*.*(..))"`. This string must be **parsed** into a structured, executable object that Spring can use to match methods.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut Parsing — What Happens:                                                │
│                                                                                  │
│  Input (String):                                                                 │
│    "execution(* com.app.service.*.*(..)) && @annotation(Loggable)"              │
│                                                                                  │
│  Parsing Steps:                                                                  │
│    1. Tokenize the string into parts                                             │
│    2. Identify designator types (execution, @annotation, within, etc.)           │
│    3. Parse each designator's pattern                                            │
│    4. Parse boolean operators (&&, ||, !)                                         │
│    5. Build an Abstract Syntax Tree (AST)                                        │
│    6. Create a PointcutExpression object                                          │
│                                                                                  │
│  Output (Object):                                                                │
│    PointcutExpression {                                                           │
│      type: AND                                                                   │
│      left: ExecutionPointcut {                                                   │
│        returnType: *                                                             │
│        declaringType: com.app.service.*                                          │
│        methodName: *                                                             │
│        params: (..)                                                              │
│      }                                                                           │
│      right: AnnotationPointcut {                                                 │
│        annotation: Loggable                                                      │
│      }                                                                           │
│    }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**The actual parser class: `PointcutParser`**

```java
// ─── org.aspectj.weaver.tools.PointcutParser ───
// This is from the AspectJ library (NOT Spring — Spring uses AspectJ's parser)

// Simplified view of how Spring invokes the parser:
public class AspectJExpressionPointcut implements Pointcut, ClassFilter, MethodMatcher {

    private String expression;  // "execution(* com.app.service.*.*(..))"

    private PointcutExpression pointcutExpression;  // Parsed result
    private Class<?> pointcutDeclarationScope;
    private String[] pointcutParameterNames;
    private Class<?>[] pointcutParameterTypes;

    // Lazy parsing — only parsed when first needed
    private PointcutExpression obtainPointcutExpression() {
        if (this.pointcutExpression == null) {
            // 1. Get a PointcutParser instance
            PointcutParser parser = PointcutParser.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(
                SUPPORTED_PRIMITIVES,        // execution, within, @annotation, args, etc.
                this.getClass().getClassLoader()
            );

            // 2. Parse the expression string into a PointcutExpression object
            this.pointcutExpression = parser.parsePointcutExpression(
                replaceBooleanOperators(this.expression),
                //  ↑ Replaces "and" → "&&", "or" → "||", "not" → "!"
                this.pointcutDeclarationScope,
                new PointcutParameter[0]
            );
        }
        return this.pointcutExpression;
    }
}
```

```java
// ─── PointcutParser.java (from AspectJ library) — Simplified ───
// Package: org.aspectj.weaver.tools

public class PointcutParser {

    // Supported pointcut designator types
    private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = Set.of(
        PointcutPrimitive.EXECUTION,        // execution()
        PointcutPrimitive.WITHIN,           // within()
        PointcutPrimitive.AT_WITHIN,        // @within()
        PointcutPrimitive.AT_ANNOTATION,    // @annotation()
        PointcutPrimitive.ARGS,             // args()
        PointcutPrimitive.AT_ARGS,          // @args()
        PointcutPrimitive.TARGET,           // target()
        PointcutPrimitive.THIS,             // this()
        PointcutPrimitive.REFERENCE         // named pointcut references
    );

    public PointcutExpression parsePointcutExpression(String expression) {
        // 1. TOKENIZE: break string into tokens
        //    "execution(* com.app.service.*.*(..))" → [EXECUTION, "(", PATTERN, ")"]

        // 2. BUILD AST: create Abstract Syntax Tree
        //    For compound expressions like "A && B || C":
        //
        //         OR
        //        /  \
        //      AND    C
        //     /   \
        //    A     B

        // 3. RESOLVE: resolve type references, validate patterns

        // 4. RETURN: PointcutExpression object with matches() methods
        return new PointcutExpressionImpl(/* parsed AST */, expression);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Parsing Example — Step by Step:                                                 │
│                                                                                  │
│  Input: "execution(* com.app.service.*.get*(..)) && !within(*.ReportService)"    │
│                                                                                  │
│  Step 1 — Tokenize:                                                              │
│    Token 1: EXECUTION_DESIGNATOR                                                 │
│    Token 2: "(" → open paren                                                     │
│    Token 3: "*" → return type wildcard                                           │
│    Token 4: "com.app.service.*" → declaring type pattern                         │
│    Token 5: "." → separator                                                      │
│    Token 6: "get*" → method name pattern                                         │
│    Token 7: "(..)" → any parameters                                              │
│    Token 8: ")" → close paren                                                    │
│    Token 9: "&&" → AND operator                                                  │
│    Token 10: "!" → NOT operator                                                  │
│    Token 11: WITHIN_DESIGNATOR                                                   │
│    Token 12: "(" → open paren                                                    │
│    Token 13: "*.ReportService" → type pattern                                    │
│    Token 14: ")" → close paren                                                   │
│                                                                                  │
│  Step 2 — Build AST:                                                             │
│                                                                                  │
│            AND (&&)                                                               │
│           /        \                                                             │
│    ExecutionPc   NOT (!)                                                          │
│    {               \                                                             │
│      ret: *      WithinPc                                                        │
│      cls: service.* {                                                            │
│      mtd: get*    cls: *.ReportService                                           │
│      prm: (..)  }                                                                │
│    }                                                                             │
│                                                                                  │
│  Step 3 — Create PointcutExpression:                                             │
│    The AST is wrapped in a PointcutExpression object that has:                   │
│    • couldMatchJoinPointsInType(Class) → fast class-level check                  │
│    • matchesMethodExecution(Method)    → precise method-level check              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 4: Store Parsed Pointcuts in Efficient Cache

Parsing a pointcut expression string is **expensive** (involves tokenization, AST building, type resolution). Spring parses each expression **only once** and caches the result.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut Caching — How Spring Avoids Re-Parsing:                                │
│                                                                                  │
│  FIRST TIME a pointcut is needed:                                                │
│    String "execution(* com.app.service.*.*(..))"                                 │
│      ↓ PointcutParser.parsePointcutExpression()                                  │
│    PointcutExpression object (parsed AST)                                        │
│      ↓ stored in AspectJExpressionPointcut.pointcutExpression field              │
│    CACHED ✓                                                                      │
│                                                                                  │
│  SUBSEQUENT TIMES the same pointcut is needed:                                   │
│    → Returns the cached PointcutExpression object instantly                      │
│    → No re-parsing needed                                                        │
│                                                                                  │
│  Additional caching layers:                                                      │
│    • BeanFactoryAspectJAdvisorsBuilder caches the list of all Advisors           │
│    • advisorCache: Map<String, List<Advisor>> — advisors per bean name           │
│    • advisedBeans: Map<Object, Boolean> — tracks which beans are already checked │
│    • proxyTypes: Map<Object, Class<?>> — caches proxy class types                │
│    • methodCache: Map<Method, List<Interceptor>> — interceptor chains per method │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Caching in AspectJExpressionPointcut ───
public class AspectJExpressionPointcut implements Pointcut, ClassFilter, MethodMatcher {

    // Parsed expression — computed ONCE, reused forever
    private volatile PointcutExpression pointcutExpression;

    // Method match cache — avoids re-evaluating for the same method
    private final Map<Method, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(32);

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        // Check cache first
        ShadowMatch shadowMatch = this.shadowMatchCache.get(method);

        if (shadowMatch == null) {
            // Not cached → compute and cache
            PointcutExpression pce = obtainPointcutExpression();
            shadowMatch = pce.matchesMethodExecution(method);
            this.shadowMatchCache.put(method, shadowMatch);
        }

        // Return cached result
        return shadowMatch.alwaysMatches();
    }
}
```

```java
// ─── Advisor-level caching in BeanFactoryAspectJAdvisorsBuilder ───
public class BeanFactoryAspectJAdvisorsBuilder {

    // Cache: list of all advisors from all @Aspect beans
    private volatile List<String> aspectBeanNames;      // names of @Aspect beans
    private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

    public List<Advisor> buildAspectJAdvisors() {
        List<String> aspectNames = this.aspectBeanNames;

        if (aspectNames == null) {
            // FIRST CALL: discover all @Aspect beans and build advisors
            synchronized (this) {
                aspectNames = this.aspectBeanNames;
                if (aspectNames == null) {
                    // ... discovery logic (shown in Step 2) ...
                    this.aspectBeanNames = aspectNames;  // Cache the list
                }
            }
        }

        // SUBSEQUENT CALLS: return from cache
        List<Advisor> advisors = new ArrayList<>();
        for (String aspectName : aspectNames) {
            List<Advisor> cached = this.advisorsCache.get(aspectName);
            if (cached != null) {
                advisors.addAll(cached);   // ← From cache, no re-parsing
            }
        }
        return advisors;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Complete Caching Architecture:                                                  │
│                                                                                  │
│  Level 1: Advisor Cache                                                          │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │ aspectBeanNames: ["loggingAspect", "perfAspect", "securityAspect"]        │  │
│  │ advisorsCache:                                                             │  │
│  │   "loggingAspect"  → [Advisor#1, Advisor#2]                               │  │
│  │   "perfAspect"     → [Advisor#3]                                          │  │
│  │   "securityAspect" → [Advisor#4, Advisor#5]                               │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Level 2: Pointcut Expression Cache                                              │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │ Each Advisor's AspectJExpressionPointcut caches:                           │  │
│  │   pointcutExpression: (parsed AST) — computed ONCE                        │  │
│  │   shadowMatchCache: {                                                      │  │
│  │     OrderService.createOrder → MATCH                                      │  │
│  │     OrderService.getOrder    → MATCH                                      │  │
│  │     PaymentService.process   → MATCH                                      │  │
│  │     OrderController.create   → NO_MATCH                                   │  │
│  │   }                                                                        │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Level 3: Per-Bean Advisor Cache (after proxy creation)                           │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │ advisedBeans: {                                                            │  │
│  │   "orderService"    → true   (has proxy)                                  │  │
│  │   "paymentService"  → true   (has proxy)                                  │  │
│  │   "reportService"   → false  (no proxy needed)                            │  │
│  │ }                                                                          │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Level 4: Method Interceptor Chain Cache (runtime)                               │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │ Inside the proxy:                                                          │  │
│  │ methodCache: {                                                             │  │
│  │   createOrder → [LoggingInterceptor, PerfInterceptor]                     │  │
│  │   getOrder    → [LoggingInterceptor]                                      │  │
│  │   cancelOrder → [LoggingInterceptor, SecurityInterceptor]                 │  │
│  │ }                                                                          │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 5: Finding @Component, @Service, @Controller Beans

As Spring creates each bean, the `AnnotationAwareAspectJAutoProxyCreator` (our BeanPostProcessor) is called **for every bean**. It doesn't care about what annotation the bean has — it checks **every** bean.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Bean Creation Flow (Spring's BeanFactory):                                      │
│                                                                                  │
│  For EACH bean definition found during component scanning:                       │
│                                                                                  │
│  1. Instantiate the bean (new OrderService())                                    │
│  2. Populate properties (@Autowired injection)                                   │
│  3. Call BeanPostProcessor.postProcessBeforeInitialization()                      │
│  4. Call @PostConstruct / InitializingBean.afterPropertiesSet()                   │
│  5. Call BeanPostProcessor.postProcessAfterInitialization()  ← AOP CHECKS HERE   │
│  6. Bean is ready → put in ApplicationContext                                    │
│                                                                                  │
│  Step 5 is where AnnotationAwareAspectJAutoProxyCreator runs.                    │
│  It checks: "Does any Advisor's pointcut match any method of this bean?"         │
│                                                                                  │
│  Beans checked:                                                                  │
│    @Service OrderService         → checked ✓                                     │
│    @Service PaymentService       → checked ✓                                     │
│    @RestController OrderController → checked ✓                                   │
│    @Component HealthChecker      → checked ✓                                     │
│    @Repository OrderRepository   → checked ✓                                     │
│    @Aspect LoggingAspect         → checked ✓ (but @Aspect itself is skipped)     │
│    @Configuration AppConfig      → checked ✓                                     │
│                                                                                  │
│  Note: @Aspect-annotated beans are SKIPPED for proxying                          │
│  (they are the ones creating proxies, not being proxied)                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── How Spring decides to skip @Aspect beans from being proxied ───
// AbstractAutoProxyCreator.java (simplified)

protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
    // Skip infrastructure beans (including @Aspect beans)
    if (isInfrastructureClass(bean.getClass())) {
        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;  // Return original — no proxy
    }
    // ...
}

// In AnnotationAwareAspectJAutoProxyCreator:
@Override
protected boolean isInfrastructureClass(Class<?> beanClass) {
    return super.isInfrastructureClass(beanClass) ||
           this.aspectJAdvisorFactory.isAspect(beanClass);
    //      ↑ Returns true if class has @Aspect annotation
    //        → These beans are NOT proxied
}
```

---

### Step 6: Check Each Bean's Eligibility for Pointcut Matching

For each non-infrastructure bean, Spring checks if **any** of the collected Advisors' pointcut expressions match **any** method of the bean. This is a **two-phase check**: first a fast class-level check, then precise method-level checks.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Eligibility Check — Two-Phase Matching:                                         │
│                                                                                  │
│  Phase 1: CLASS-LEVEL check (FAST — eliminates most beans)                       │
│    "Could this pointcut POSSIBLY match any method in this class?"                │
│                                                                                  │
│    Pointcut: execution(* com.app.service.*.*(..))                                │
│    Bean class: com.app.controller.OrderController                                │
│    → Package doesn't match → SKIP (no need to check methods)                    │
│                                                                                  │
│    Pointcut: execution(* com.app.service.*.*(..))                                │
│    Bean class: com.app.service.OrderService                                      │
│    → Package MATCHES → proceed to Phase 2                                        │
│                                                                                  │
│  Phase 2: METHOD-LEVEL check (precise — only for class-level matches)            │
│    For each method of the matched class, check the full pointcut:                │
│                                                                                  │
│    OrderService.createOrder(OrderRequest)  → MATCH ✓                             │
│    OrderService.getOrder(Long)             → MATCH ✓                             │
│    OrderService.getAllOrders()              → MATCH ✓                             │
│    OrderService.cancelOrder(Long)          → MATCH ✓                             │
│                                                                                  │
│  If ANY method matches → bean is eligible → CREATE PROXY                         │
│  If NO method matches → bean is NOT eligible → return original bean              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── The actual matching code ───
// AbstractAdvisorAutoProxyCreator.java (simplified)

protected Object[] getAdvicesAndAdvisorsForBean(
        Class<?> beanClass, String beanName, TargetSource targetSource) {

    // 1. Get ALL advisors (from all @Aspect beans)
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    //  → Returns: [LoggingAdvisor, PerfAdvisor, SecurityAdvisor, ...]

    // 2. Filter: which advisors match THIS bean?
    List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(
        candidateAdvisors, beanClass, beanName);

    if (eligibleAdvisors.isEmpty()) {
        return DO_NOT_PROXY;   // No advisors match → no proxy needed
    }

    return eligibleAdvisors.toArray();  // These will be wired into the proxy
}
```

```java
// ─── AopUtils.findAdvisorsThatCanApply() — The core matching logic ───
// org.springframework.aop.support.AopUtils

public static List<Advisor> findAdvisorsThatCanApply(
        List<Advisor> candidateAdvisors, Class<?> clazz) {

    List<Advisor> eligibleAdvisors = new ArrayList<>();

    for (Advisor candidate : candidateAdvisors) {
        if (canApply(candidate, clazz)) {
            eligibleAdvisors.add(candidate);
        }
    }

    return eligibleAdvisors;
}

public static boolean canApply(Advisor advisor, Class<?> targetClass) {
    Pointcut pointcut = advisor.getPointcut();

    // ─── PHASE 1: Class-level check (FAST) ───
    if (!pointcut.getClassFilter().matches(targetClass)) {
        return false;  // Class doesn't match → skip all methods
    }

    // ─── PHASE 2: Method-level check (PRECISE) ───
    MethodMatcher methodMatcher = pointcut.getMethodMatcher();

    // If the matcher says "match all methods" → no need to check individually
    if (methodMatcher == MethodMatcher.TRUE) {
        return true;
    }

    // Check each method of the target class
    Set<Class<?>> classes = new LinkedHashSet<>();
    if (!Proxy.isProxyClass(targetClass)) {
        classes.add(ClassUtils.getUserClass(targetClass));
    }
    classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));

    for (Class<?> clazz : classes) {
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
        for (Method method : methods) {
            if (methodMatcher.matches(method, targetClass)) {
                return true;  // At least ONE method matches → bean needs proxy
            }
        }
    }

    return false;  // No method matches → no proxy needed
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Eligibility Check — Walkthrough Example:                                        │
│                                                                                  │
│  Advisors collected: 3                                                           │
│    Advisor#1: execution(* com.app.service.*.*(..))     → @Before logBefore       │
│    Advisor#2: @annotation(com.app.annotation.Timed)    → @Around timeMethod      │
│    Advisor#3: execution(* com.app.controller.*.*(..))  → @Before logRequest      │
│                                                                                  │
│  Checking bean: OrderService (com.app.service.OrderService)                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │ Advisor#1: execution(* com.app.service.*.*(..))                         │     │
│  │   Phase 1: class com.app.service.OrderService → service.* → MATCH ✓   │     │
│  │   Phase 2: createOrder() → MATCH ✓ → ELIGIBLE                          │     │
│  │                                                                         │     │
│  │ Advisor#2: @annotation(Timed)                                           │     │
│  │   Phase 1: any class could have @Timed methods → PASS                   │     │
│  │   Phase 2: createOrder() → no @Timed → NO MATCH                        │     │
│  │            getOrder() → no @Timed → NO MATCH                            │     │
│  │            cancelOrder() → has @Timed → MATCH ✓ → ELIGIBLE              │     │
│  │                                                                         │     │
│  │ Advisor#3: execution(* com.app.controller.*.*(..))                      │     │
│  │   Phase 1: class com.app.service.OrderService → controller.* → FAIL ✗ │     │
│  │   → SKIP (no method check needed)                                       │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
│                                                                                  │
│  Result: OrderService gets Advisor#1 and Advisor#2 → CREATE PROXY               │
│                                                                                  │
│  Checking bean: ReportService (com.app.service.reporting.ReportService)          │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │ Advisor#1: execution(* com.app.service.*.*(..))                         │     │
│  │   Phase 1: com.app.service.reporting.ReportService                      │     │
│  │            Pattern: com.app.service.* (one level only)                  │     │
│  │            reporting sub-package → FAIL ✗ → SKIP                        │     │
│  │                                                                         │     │
│  │ Advisor#2: @annotation(Timed)                                           │     │
│  │   Phase 1: PASS  │  Phase 2: no @Timed methods → NO MATCH              │     │
│  │                                                                         │     │
│  │ Advisor#3: execution(* com.app.controller.*.*(..))                      │     │
│  │   Phase 1: FAIL ✗ → SKIP                                               │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
│                                                                                  │
│  Result: NO advisors match → NO PROXY → original bean registered                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Step 7: Creating Proxies — JDK Dynamic Proxy vs CGLIB Proxy

When a bean is eligible for AOP, Spring creates a **proxy** that wraps the original bean. The proxy intercepts method calls and executes the advice chain. Spring uses one of two proxy mechanisms:

- **JDK Dynamic Proxy** — Java's built-in `java.lang.reflect.Proxy`
- **CGLIB Proxy** — **C**ode **G**eneration **Lib**rary (creates a subclass at runtime)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JDK Dynamic Proxy vs CGLIB Proxy — How Spring Decides:                          │
│                                                                                  │
│  Decision Rule:                                                                  │
│                                                                                  │
│  Does the bean implement at least one interface?                                 │
│    │                                                                             │
│    ├── YES → Is proxyTargetClass = false? (default)                              │
│    │    │                                                                        │
│    │    ├── YES → Use JDK Dynamic Proxy                                          │
│    │    └── NO  → Use CGLIB Proxy                                                │
│    │                                                                             │
│    └── NO  → Use CGLIB Proxy (no choice — JDK proxy requires interface)          │
│                                                                                  │
│  Spring Boot Default (since 2.0):                                                │
│    spring.aop.proxy-target-class = true   ← CGLIB by default!                   │
│    This means: Spring Boot uses CGLIB for ALL beans by default.                  │
│    Even beans that implement interfaces get CGLIB proxies.                       │
│                                                                                  │
│  To force JDK proxies:                                                           │
│    spring.aop.proxy-target-class = false                                         │
│    OR: @EnableAspectJAutoProxy(proxyTargetClass = false)                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### JDK Dynamic Proxy

**Full Form:** JDK = Java Development Kit. It's Java's built-in proxy mechanism from `java.lang.reflect.Proxy`.

**How it works:** Creates a proxy class at runtime that **implements the same interface(s)** as the target bean. The proxy delegates all interface method calls through an `InvocationHandler`.

**Requirement:** The target bean **must implement at least one interface**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JDK Dynamic Proxy — Visual:                                                     │
│                                                                                  │
│  interface OrderProcessor {                                                      │
│    Order createOrder(OrderRequest req);                                          │
│    Order getOrder(Long id);                                                      │
│  }                                                                               │
│                                                                                  │
│  class OrderService implements OrderProcessor {                                  │
│    Order createOrder(OrderRequest req) { ... }                                   │
│    Order getOrder(Long id) { ... }                                               │
│    void internalHelper() { ... }    ← NOT in interface                           │
│  }                                                                               │
│                                                                                  │
│  JDK Proxy creates:                                                              │
│                                                                                  │
│  ┌───────────────────────────────────┐                                           │
│  │ $Proxy42 (generated class)        │                                           │
│  │ implements OrderProcessor         │ ← Same interface                          │
│  │                                   │                                           │
│  │ InvocationHandler handler;        │ ← Contains advice chain                   │
│  │                                   │                                           │
│  │ Order createOrder(OrderRequest r) │                                           │
│  │   → handler.invoke(this, method, args) │                                      │
│  │   → runs @Before advice           │                                           │
│  │   → calls target.createOrder(r)   │                                           │
│  │   → runs @After advice            │                                           │
│  │                                   │                                           │
│  │ Order getOrder(Long id)           │                                           │
│  │   → handler.invoke(this, method, args) │                                      │
│  │                                   │                                           │
│  │ ✗ internalHelper() NOT proxied    │ ← Not in interface!                       │
│  └───────────────────────────────────┘                                           │
│                                                                                  │
│  Limitations:                                                                    │
│    • Can ONLY proxy interface methods                                            │
│    • Methods not in any interface → NOT intercepted                              │
│    • Bean must implement at least one interface                                  │
│    • proxy instanceof OrderProcessor → true                                      │
│    • proxy instanceof OrderService   → FALSE!                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── What JDK Dynamic Proxy looks like internally ───
// java.lang.reflect.Proxy

// Spring creates the proxy like this (simplified):
public class JdkDynamicAopProxy implements InvocationHandler {

    private final Object target;             // The real OrderService bean
    private final List<Advisor> advisors;    // Matching advisors

    // Creating the proxy:
    public Object getProxy() {
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),  // [OrderProcessor.class]
            this  // InvocationHandler = this class
        );
    }

    // EVERY method call on the proxy goes through here:
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. Get the interceptor chain for this specific method
        List<Object> chain = getInterceptorsAndDynamicInterceptionAdvice(method);

        if (chain.isEmpty()) {
            // No advice for this method → call target directly
            return method.invoke(target, args);
        }

        // 2. Create a MethodInvocation and execute the chain
        MethodInvocation invocation = new ReflectiveMethodInvocation(
            proxy, target, method, args, target.getClass(), chain);

        return invocation.proceed();
    }
}
```

#### CGLIB Proxy

**Full Form:** CGLIB = **C**ode **G**eneration **Lib**rary. It generates a **subclass** of the target class at runtime using bytecode manipulation.

**How it works:** Creates a new class that **extends** the target bean's class. Overrides all non-final methods to add interception logic.

**Requirement:** The target class must **not be `final`** (cannot extend a final class). Methods must not be `final` either.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  CGLIB Proxy — Visual:                                                           │
│                                                                                  │
│  @Service                                                                        │
│  class OrderService {     ← No interface needed!                                 │
│    Order createOrder(OrderRequest req) { ... }                                   │
│    Order getOrder(Long id) { ... }                                               │
│    void internalHelper() { ... }                                                 │
│  }                                                                               │
│                                                                                  │
│  CGLIB creates:                                                                  │
│                                                                                  │
│  ┌───────────────────────────────────────┐                                       │
│  │ OrderService$$EnhancerBySpringCGLIB   │                                       │
│  │ extends OrderService                  │ ← SUBCLASS of target!                 │
│  │                                       │                                       │
│  │ MethodInterceptor[] callbacks;        │ ← Contains advice chain               │
│  │                                       │                                       │
│  │ @Override                             │                                       │
│  │ Order createOrder(OrderRequest r)     │                                       │
│  │   → callback.intercept(...)           │                                       │
│  │   → runs @Before advice              │                                       │
│  │   → calls super.createOrder(r)       │ ← calls PARENT class method           │
│  │   → runs @After advice               │                                       │
│  │                                       │                                       │
│  │ @Override                             │                                       │
│  │ Order getOrder(Long id)              │                                       │
│  │   → callback.intercept(...)           │                                       │
│  │                                       │                                       │
│  │ @Override                             │                                       │
│  │ void internalHelper()                │ ← Also proxied! (all methods)         │
│  │   → callback.intercept(...)           │                                       │
│  └───────────────────────────────────────┘                                       │
│                                                                                  │
│  Advantages over JDK:                                                            │
│    • No interface required                                                       │
│    • ALL methods can be proxied (not just interface methods)                      │
│    • proxy instanceof OrderService → TRUE!                                       │
│                                                                                  │
│  Limitations:                                                                    │
│    • Cannot proxy final classes (class OrderService final → ERROR)               │
│    • Cannot proxy final methods (final Order getOrder() → NOT intercepted)       │
│    • Cannot proxy private methods (private → NOT intercepted)                    │
│    • Slightly more memory than JDK proxy (generates a subclass)                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── What CGLIB Proxy looks like internally ───
// Spring's CglibAopProxy (simplified)

public class CglibAopProxy implements AopProxy {

    private final Object target;
    private final List<Advisor> advisors;

    public Object getProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target.getClass());   // extends OrderService
        enhancer.setCallback(new DynamicAdvisedInterceptor(this.advisors));
        return enhancer.create();  // Returns the CGLIB proxy instance
    }

    // Inner class that intercepts ALL method calls
    private static class DynamicAdvisedInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object proxy, Method method, Object[] args,
                                MethodProxy methodProxy) throws Throwable {

            // 1. Get the interceptor chain for this method
            List<Object> chain = getInterceptorsAndDynamicInterceptionAdvice(method);

            if (chain.isEmpty()) {
                // No advice → call original method directly
                return methodProxy.invoke(target, args);
            }

            // 2. Execute the advice chain
            CglibMethodInvocation invocation = new CglibMethodInvocation(
                proxy, target, method, args, target.getClass(), chain, methodProxy);

            return invocation.proceed();
        }
    }
}
```

#### JDK Dynamic Proxy vs CGLIB Proxy — Complete Comparison

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JDK Dynamic Proxy vs CGLIB — Comparison Table:                                  │
│                                                                                  │
│  ┌────────────────────────────┬───────────────────────┬──────────────────────────┐│
│  │ Feature                    │ JDK Dynamic Proxy     │ CGLIB Proxy              ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Full Name                  │ Java Development Kit  │ Code Generation Library  ││
│  │                            │ Dynamic Proxy         │                          ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Mechanism                  │ Implements interfaces │ Extends target class     ││
│  │                            │ at runtime            │ (creates subclass)       ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Interface Required?        │ YES — must implement  │ NO — works without       ││
│  │                            │ at least one          │ interfaces               ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Which methods proxied?     │ Only interface methods│ ALL non-final, non-      ││
│  │                            │                       │ private methods          ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ proxy instanceof Target?   │ FALSE (proxy is NOT   │ TRUE (proxy IS a         ││
│  │                            │ Target class type)    │ subclass of Target)      ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ proxy instanceof Interface?│ TRUE                  │ TRUE (inherits from      ││
│  │                            │                       │ class which implements)  ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Can proxy final class?     │ N/A (uses interface)  │ NO — cannot extend final ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Can proxy final method?    │ Only if in interface  │ NO — cannot override     ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Performance (creation)     │ Faster to create      │ Slightly slower          ││
│  │                            │                       │ (bytecode generation)    ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Performance (invocation)   │ Slightly slower       │ Faster (direct method    ││
│  │                            │ (reflection-based)    │ invocation via subclass) ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Built into Java?           │ YES (java.lang.       │ NO — third-party library ││
│  │                            │ reflect.Proxy)        │ (bundled in Spring)      ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Spring Boot default?       │ NO (since 2.0)        │ YES (since 2.0)          ││
│  ├────────────────────────────┼───────────────────────┼──────────────────────────┤│
│  │ Proxy class name           │ $Proxy42              │ OrderService$$Enhancer   ││
│  │                            │                       │ BySpringCGLIB$$abc123    ││
│  └────────────────────────────┴───────────────────────┴──────────────────────────┘│
│                                                                                  │
│  When to use which:                                                              │
│    JDK Dynamic Proxy:                                                            │
│      • Bean implements interfaces                                                │
│      • You only need to intercept interface methods                              │
│      • You want to enforce programming to interfaces                             │
│      • Legacy apps that depend on Proxy.isProxyClass()                           │
│                                                                                  │
│    CGLIB Proxy (recommended — Spring Boot default):                               │
│      • Bean does NOT implement any interface                                     │
│      • You need to intercept concrete class methods                              │
│      • You need proxy instanceof TargetClass to be true                          │
│      • Modern Spring Boot applications (default since 2.0)                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── How Spring decides which proxy type to use ───
// DefaultAopProxyFactory.java (actual Spring code, simplified)

public class DefaultAopProxyFactory implements AopProxyFactory {

    @Override
    public AopProxy createAopProxy(AdvisedSupport config) {

        if (config.isOptimize() ||
            config.isProxyTargetClass() ||         // ← Spring Boot sets this TRUE
            hasNoUserSuppliedProxyInterfaces(config)) {

            Class<?> targetClass = config.getTargetClass();

            if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
                // Target is already an interface or proxy → use JDK
                return new JdkDynamicAopProxy(config);
            }

            // CGLIB proxy (default path for Spring Boot)
            return new ObjenesisCglibAopProxy(config);

        } else {
            // proxyTargetClass = false AND bean has interfaces → use JDK
            return new JdkDynamicAopProxy(config);
        }
    }
}
```

```java
// ─── Verify at runtime which proxy type was used ───
@RestController
public class DebugController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/debug/proxy")
    public Map<String, Object> proxyInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // What type is the injected bean?
        info.put("class", orderService.getClass().getName());
        // → "com.app.service.OrderService$$EnhancerBySpringCGLIB$$abc123"  (CGLIB)
        // → "com.sun.proxy.$Proxy42"  (JDK)

        // Is it a CGLIB proxy?
        info.put("isCglibProxy", AopUtils.isCglibProxy(orderService));
        // → true (for CGLIB)

        // Is it a JDK dynamic proxy?
        info.put("isJdkProxy", AopUtils.isJdkDynamicProxy(orderService));
        // → true (for JDK)

        // Is it ANY type of proxy?
        info.put("isAopProxy", AopUtils.isAopProxy(orderService));
        // → true (for both)

        // Get the target class (behind the proxy)
        info.put("targetClass", AopUtils.getTargetClass(orderService).getName());
        // → "com.app.service.OrderService"

        return info;
    }
}
```

---

### Complete Startup Flow — End-to-End Visual

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│  SpringApplication.run(MyApp.class)                                              │
│  ─────────────────────────────────                                               │
│         │                                                                        │
│         ▼                                                                        │
│  ┌──────────────────────────────────────┐                                        │
│  │ 1. Create ApplicationContext         │                                        │
│  │    - Scan for @Component, @Service,  │                                        │
│  │      @Controller, @Aspect, etc.      │                                        │
│  └──────────────────┬───────────────────┘                                        │
│                     ▼                                                            │
│  ┌──────────────────────────────────────┐                                        │
│  │ 2. Process @EnableAspectJAutoProxy   │                                        │
│  │    (auto-included by Spring Boot)    │                                        │
│  │    - Register AnnotationAwareAspect  │                                        │
│  │      JAutoProxyCreator as           │                                        │
│  │      BeanPostProcessor              │                                        │
│  └──────────────────┬───────────────────┘                                        │
│                     ▼                                                            │
│  ┌──────────────────────────────────────┐                                        │
│  │ 3. Discover @Aspect beans            │                                        │
│  │    - Find: LoggingAspect,            │                                        │
│  │      PerfAspect, SecurityAspect      │                                        │
│  │    - Extract @Before, @After,        │                                        │
│  │      @Around methods → Advisors      │                                        │
│  └──────────────────┬───────────────────┘                                        │
│                     ▼                                                            │
│  ┌──────────────────────────────────────┐                                        │
│  │ 4. Parse Pointcut Expressions        │                                        │
│  │    - PointcutParser tokenizes +      │                                        │
│  │      builds AST for each expression  │                                        │
│  │    - Cache parsed PointcutExpression  │                                        │
│  │      objects                          │                                        │
│  └──────────────────┬───────────────────┘                                        │
│                     ▼                                                            │
│  ╔══════════════════════════════════════╗                                        │
│  ║ 5-7. FOR EACH BEAN being created:   ║  ← LOOP                                │
│  ╠══════════════════════════════════════╣                                        │
│  ║                                      ║                                        │
│  ║  5. Is it an @Aspect? → SKIP        ║                                        │
│  ║     Otherwise → proceed              ║                                        │
│  ║           │                           ║                                        │
│  ║           ▼                           ║                                        │
│  ║  6. Check eligibility:               ║                                        │
│  ║     For each Advisor:                ║                                        │
│  ║       Phase 1: class-level match?    ║                                        │
│  ║         NO → skip this advisor       ║                                        │
│  ║         YES ↓                        ║                                        │
│  ║       Phase 2: any method match?     ║                                        │
│  ║         YES → advisor APPLIES        ║                                        │
│  ║           │                           ║                                        │
│  ║           ▼                           ║                                        │
│  ║  7. Any advisors apply?              ║                                        │
│  ║     NO  → register original bean     ║                                        │
│  ║     YES → create proxy:              ║                                        │
│  ║       Has interface + JDK mode?      ║                                        │
│  ║         → JDK Dynamic Proxy          ║                                        │
│  ║       Otherwise?                     ║                                        │
│  ║         → CGLIB Proxy (default)      ║                                        │
│  ║       Register PROXY in context      ║                                        │
│  ║                                      ║                                        │
│  ╚══════════════════════════════════════╝                                        │
│                     │                                                            │
│                     ▼                                                            │
│  ┌──────────────────────────────────────┐                                        │
│  │ Application Ready!                   │                                        │
│  │                                      │                                        │
│  │ ApplicationContext contains:         │                                        │
│  │   "orderService" → CGLIB Proxy       │                                        │
│  │   "paymentService" → CGLIB Proxy     │                                        │
│  │   "reportService" → Original Bean    │                                        │
│  │   "orderController" → CGLIB Proxy    │                                        │
│  │   "loggingAspect" → Original Bean    │                                        │
│  └──────────────────────────────────────┘                                        │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Startup Phase — Internal Classes Reference

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Key Spring AOP Internal Classes — Reference:                                    │
│                                                                                  │
│  ┌───────────────────────────────────────────┬───────────────────────────────────┐│
│  │ Class                                     │ Responsibility                    ││
│  ├───────────────────────────────────────────┼───────────────────────────────────┤│
│  │ @EnableAspectJAutoProxy                   │ Activates AOP (annotation)        ││
│  │ AopAutoConfiguration                     │ Auto-configures AOP in Boot       ││
│  │ AspectJAutoProxyRegistrar                 │ Registers the BeanPostProcessor   ││
│  │ AnnotationAwareAspectJAutoProxyCreator    │ The MAIN class — BeanPostProcessor││
│  │                                           │ that creates proxies              ││
│  │ BeanFactoryAspectJAdvisorsBuilder         │ Discovers @Aspect beans           ││
│  │ ReflectiveAspectJAdvisorFactory           │ Extracts Advisors from @Aspect    ││
│  │ AspectJExpressionPointcut                 │ Wraps a parsed pointcut expr      ││
│  │ PointcutParser (AspectJ lib)              │ Parses expression strings to AST  ││
│  │ AopUtils                                  │ canApply(), findAdvisors() etc.   ││
│  │ DefaultAopProxyFactory                    │ Decides JDK vs CGLIB              ││
│  │ JdkDynamicAopProxy                        │ Creates JDK proxies              ││
│  │ ObjenesisCglibAopProxy                    │ Creates CGLIB proxies            ││
│  │ ProxyFactory                              │ Configures and creates proxies    ││
│  │ AdvisedSupport                            │ Holds proxy configuration         ││
│  │ ReflectiveMethodInvocation                │ Executes the interceptor chain    ││
│  └───────────────────────────────────────────┴───────────────────────────────────┘│
│                                                                                  │
│  ┌───────────────────────────────────────────┬───────────────────────────────────┐│
│  │ Package                                   │ What's There                      ││
│  ├───────────────────────────────────────────┼───────────────────────────────────┤│
│  │ org.springframework.aop                   │ AOP interfaces & utils            ││
│  │ org.springframework.aop.framework         │ Proxy creation (JDK/CGLIB)        ││
│  │ org.springframework.aop.aspectj           │ AspectJ integration               ││
│  │ org.springframework.aop.aspectj.annotation│ @Aspect processing                ││
│  │ org.aspectj.weaver.tools                  │ PointcutParser (AspectJ lib)      ││
│  └───────────────────────────────────────────┴───────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


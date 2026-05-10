### How @Transactional Internally Uses Spring AOP

`@Transactional` is the **most common real-world use case of Spring AOP**. When you annotate a method with `@Transactional`, Spring does NOT modify your code. Instead, it creates a **proxy object** that wraps your bean and intercepts method calls to add transaction begin/commit/rollback logic.

**The Internal Architecture:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How Spring Creates the Transactional Proxy at Startup:                          │
│                                                                                  │
│  1. Application starts → Spring scans for beans                                  │
│  2. Spring finds OrderService with @Transactional methods                        │
│  3. BeanPostProcessor (InfrastructureAdvisorAutoProxyCreator) kicks in           │
│  4. It checks: Does any Advisor match this bean?                                 │
│  5. BeanFactoryTransactionAttributeSourceAdvisor says YES:                       │
│     - Its Pointcut (TransactionAttributeSourcePointcut) checks:                  │
│       "Does this class/method have @Transactional?"                              │
│     - Match found → create a PROXY                                               │
│  6. Spring creates a CGLIB proxy (or JDK dynamic proxy) around OrderService      │
│  7. The proxy contains TransactionInterceptor as the advice                      │
│  8. The proxy is registered in ApplicationContext INSTEAD of the real bean        │
│                                                                                  │
│  ┌──────────────┐     ┌──────────────────────────────────────────────┐            │
│  │ Spring IoC    │     │ ApplicationContext                           │            │
│  │ Container     │────→│                                              │            │
│  │               │     │  "orderService" → Proxy$$OrderService (CGLIB)│            │
│  │               │     │                    │                         │            │
│  │               │     │                    ├── TransactionInterceptor │            │
│  │               │     │                    └── Real OrderService      │            │
│  └──────────────┘     └──────────────────────────────────────────────┘            │
│                                                                                  │
│  When you @Autowired OrderService → you get the PROXY, not the real bean!        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Runtime Execution Flow:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Runtime: What Happens When You Call a @Transactional Method                     │
│                                                                                  │
│  Controller calls: orderService.placeOrder(request)                              │
│       │                                                                          │
│       │ (orderService is actually Proxy$$OrderService)                           │
│       v                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐               │
│  │  CGLIB Proxy$$OrderService                                     │               │
│  │                                                                │               │
│  │  intercept(obj, method, args, methodProxy)                     │               │
│  │       │                                                        │               │
│  │       v                                                        │               │
│  │  ┌──────────────────────────────────────────────────────────┐  │               │
│  │  │  TransactionInterceptor.invoke(MethodInvocation)         │  │               │
│  │  │       │                                                  │  │               │
│  │  │       v                                                  │  │               │
│  │  │  invokeWithinTransaction(method, targetClass, invocation)│  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 1. Get TransactionAttribute                      │  │               │
│  │  │       │    (reads @Transactional properties:             │  │               │
│  │  │       │     isolation, propagation, readOnly, etc.)      │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 2. Get PlatformTransactionManager                │  │               │
│  │  │       │    (JpaTransactionManager for JPA)               │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 3. createTransactionIfNecessary()                │  │               │
│  │  │       │    → txManager.getTransaction(definition)        │  │               │
│  │  │       │    → DataSource.getConnection()                  │  │               │
│  │  │       │    → connection.setAutoCommit(false)  ← BEGIN    │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 4. invocation.proceed()                          │  │               │
│  │  │       │    → calls REAL OrderService.placeOrder()        │  │               │
│  │  │       │    → your business logic runs here               │  │               │
│  │  │       │    → Hibernate generates SQL                     │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 5a. SUCCESS → commitTransactionAfterReturning()  │  │               │
│  │  │       │     → txManager.commit(status)                   │  │               │
│  │  │       │     → connection.commit()             ← COMMIT   │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 5b. EXCEPTION → completeTransactionAfterThrowing()│ │               │
│  │  │       │     → txManager.rollback(status)                 │  │               │
│  │  │       │     → connection.rollback()           ← ROLLBACK │  │               │
│  │  │       │                                                  │  │               │
│  │  │       │ 6. cleanupTransactionInfo()                      │  │               │
│  │  │       │    → restore previous transaction (if nested)    │  │               │
│  │  └───────┴──────────────────────────────────────────────────┘  │               │
│  └────────────────────────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


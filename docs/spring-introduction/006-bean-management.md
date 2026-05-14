

## Bean Management in Spring Boot

---

### 1. What is a Bean?

A **Bean** is simply a **Java object that is created, configured, and managed by the Spring IoC (Inversion of Control) container**. Instead of you creating objects with `new`, Spring creates them, wires their dependencies, manages their lifecycle, and destroys them when the application shuts down.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Regular Java Object vs Spring Bean:                                             │
│                                                                                  │
│  Regular object (YOU manage it):                                                │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │ UserService userService = new UserService();                             │   │
│  │ // YOU create it                                                         │   │
│  │ // YOU must wire its dependencies manually:                              │   │
│  │ userService.setUserRepository(new UserRepository());                     │   │
│  │ userService.setPasswordEncoder(new BCryptPasswordEncoder());             │   │
│  │ // YOU manage its lifecycle                                              │   │
│  │ // YOU are responsible for cleanup                                       │   │
│  │ // Not a bean — Spring knows NOTHING about it                           │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Spring Bean (SPRING manages it):                                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │ @Service                                                                 │   │
│  │ public class UserService {                                               │   │
│  │     private final UserRepository userRepo;  // Spring injects this      │   │
│  │     private final PasswordEncoder encoder;  // Spring injects this      │   │
│  │     public UserService(UserRepository r, PasswordEncoder e) {           │   │
│  │         this.userRepo = r;                                              │   │
│  │         this.encoder = e;                                                │   │
│  │     }                                                                    │   │
│  │ }                                                                        │   │
│  │ // Spring CREATES the instance                                          │   │
│  │ // Spring INJECTS all dependencies                                      │   │
│  │ // Spring MANAGES the lifecycle (init → use → destroy)                  │   │
│  │ // Spring makes it available for injection into OTHER beans             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Key Properties of a Spring Bean

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Every Spring Bean Has:                                                          │
│                                                                                  │
│  ┌─────────────────────┬─────────────────────────────────────────────────────┐  │
│  │ Property            │ Description                                         │  │
│  ├─────────────────────┼─────────────────────────────────────────────────────┤  │
│  │ Bean Name           │ Unique identifier in the container.                 │  │
│  │                     │ Default: class name with lowercase first letter     │  │
│  │                     │ (UserService → "userService")                      │  │
│  │                     │ Or method name for @Bean (objectMapper → "objectM") │  │
│  ├─────────────────────┼─────────────────────────────────────────────────────┤  │
│  │ Bean Type           │ The Java class/interface the bean implements.       │  │
│  │                     │ Used for type-based autowiring.                     │  │
│  ├─────────────────────┼─────────────────────────────────────────────────────┤  │
│  │ Scope               │ How many instances and how long they live.          │  │
│  │                     │ Default: singleton (one per container).             │  │
│  │                     │ Others: prototype, request, session.                │  │
│  ├─────────────────────┼─────────────────────────────────────────────────────┤  │
│  │ Dependencies        │ Other beans this bean needs to function.            │  │
│  │                     │ Injected via constructor, setter, or field.        │  │
│  ├─────────────────────┼─────────────────────────────────────────────────────┤  │
│  │ Initialization      │ Methods to call after construction:                 │  │
│  │                     │ @PostConstruct, InitializingBean, initMethod.       │  │
│  ├─────────────────────┼─────────────────────────────────────────────────────┤  │
│  │ Destruction         │ Methods to call on shutdown:                        │  │
│  │                     │ @PreDestroy, DisposableBean, destroyMethod.         │  │
│  ├─────────────────────┼─────────────────────────────────────────────────────┤  │
│  │ Lazy/Eager          │ Default: eager (created at startup).                │  │
│  │                     │ @Lazy: created on first access.                     │  │
│  └─────────────────────┴─────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Bean Lifecycle — From Birth to Death

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Complete Bean Lifecycle:                                                         │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐                │
│  │  1. INSTANTIATION                                           │                │
│  │     Spring calls the constructor (or factory method)        │                │
│  │     → new UserService(...)                                  │                │
│  └─────────────────────┬───────────────────────────────────────┘                │
│                        ▼                                                         │
│  ┌─────────────────────────────────────────────────────────────┐                │
│  │  2. DEPENDENCY INJECTION                                    │                │
│  │     Spring injects @Autowired fields and setter methods     │                │
│  │     → Constructor args already injected in step 1           │                │
│  │     → Field injection: field.set(bean, dependency)          │                │
│  │     → Setter injection: bean.setXxx(dependency)             │                │
│  └─────────────────────┬───────────────────────────────────────┘                │
│                        ▼                                                         │
│  ┌─────────────────────────────────────────────────────────────┐                │
│  │  3. BEAN POST-PROCESSOR: postProcessBeforeInitialization()  │                │
│  │     (BeanPostProcessor implementations run here)            │                │
│  └─────────────────────┬───────────────────────────────────────┘                │
│                        ▼                                                         │
│  ┌─────────────────────────────────────────────────────────────┐                │
│  │  4. INITIALIZATION                                          │                │
│  │     a) @PostConstruct method                                │                │
│  │     b) InitializingBean.afterPropertiesSet()                │                │
│  │     c) @Bean(initMethod = "init")                           │                │
│  └─────────────────────┬───────────────────────────────────────┘                │
│                        ▼                                                         │
│  ┌─────────────────────────────────────────────────────────────┐                │
│  │  5. BEAN POST-PROCESSOR: postProcessAfterInitialization()   │                │
│  │     AOP proxies are created HERE (e.g., @Transactional)     │                │
│  └─────────────────────┬───────────────────────────────────────┘                │
│                        ▼                                                         │
│  ┌─────────────────────────────────────────────────────────────┐                │
│  │  6. ═══ BEAN IS READY FOR USE ═══                           │                │
│  │     Bean is now in the container, injectable into others    │                │
│  │     ... application runs ...                                │                │
│  └─────────────────────┬───────────────────────────────────────┘                │
│                        ▼ (application shutdown)                                  │
│  ┌─────────────────────────────────────────────────────────────┐                │
│  │  7. DESTRUCTION                                             │                │
│  │     a) @PreDestroy method                                   │                │
│  │     b) DisposableBean.destroy()                             │                │
│  │     c) @Bean(destroyMethod = "cleanup")                     │                │
│  └─────────────────────────────────────────────────────────────┘                │
│                                                                                  │
│  Note: Prototype-scoped beans do NOT go through step 7.                         │
│  Spring does NOT manage destruction of prototype beans.                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Observing the full lifecycle ─────────────────────────────────────────────────
@Component
public class LifecycleBean implements InitializingBean, DisposableBean {

    public LifecycleBean() {
        System.out.println("1. Constructor called");
    }

    @Autowired
    public void setDependency(SomeDependency dep) {
        System.out.println("2. Setter injection");
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("4a. @PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("4b. InitializingBean.afterPropertiesSet()");
    }

    // If registered via @Bean(initMethod = "customInit"):
    public void customInit() {
        System.out.println("4c. Custom init method");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("7a. @PreDestroy");
    }

    @Override
    public void destroy() {
        System.out.println("7b. DisposableBean.destroy()");
    }
}

// Output at startup:
// 1. Constructor called
// 2. Setter injection
// 4a. @PostConstruct
// 4b. InitializingBean.afterPropertiesSet()

// Output at shutdown:
// 7a. @PreDestroy
// 7b. DisposableBean.destroy()
```

#### Where Are Beans Stored?

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ApplicationContext (IoC Container)                                               │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │  DefaultListableBeanFactory                                                │  │
│  │  ┌──────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  Singleton Cache (ConcurrentHashMap<String, Object>)                │  │  │
│  │  │                                                                      │  │  │
│  │  │  "userService"       → UserService@a1b2c3                           │  │  │
│  │  │  "orderService"      → OrderService@d4e5f6                          │  │  │
│  │  │  "userRepository"    → SimpleJpaRepository@g7h8i9                   │  │  │
│  │  │  "passwordEncoder"   → BCryptPasswordEncoder@j0k1l2                 │  │  │
│  │  │  "objectMapper"      → ObjectMapper@m3n4o5                          │  │  │
│  │  │  "dataSource"        → HikariDataSource@p6q7r8                      │  │  │
│  │  │  ...                                                                 │  │  │
│  │  └──────────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                            │  │
│  │  BeanDefinition Registry (metadata — how to create each bean)             │  │
│  │  ┌──────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  "userService" → BeanDefinition {                                   │  │  │
│  │  │      class: UserService,                                             │  │  │
│  │  │      scope: singleton,                                               │  │  │
│  │  │      lazyInit: false,                                                │  │  │
│  │  │      dependencies: [userRepository, passwordEncoder],                │  │  │
│  │  │      initMethod: null,                                               │  │  │
│  │  │      destroyMethod: null                                             │  │  │
│  │  │  }                                                                   │  │  │
│  │  │  ...                                                                 │  │  │
│  │  └──────────────────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  You can access any bean programmatically:                                       │
│  UserService us = applicationContext.getBean("userService", UserService.class);  │
│  UserService us = applicationContext.getBean(UserService.class);                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2. How to Create Beans

There are **two primary ways** to register a bean in the Spring container: `@Component` (and its specialisations) and `@Bean`.

---

#### 2.1 Creating Beans with `@Component`

`@Component` (and `@Service`, `@Repository`, `@Controller`, `@RestController`, `@Configuration`) marks a **class** so that `@ComponentScan` discovers it at startup and registers it as a bean automatically.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Component — How It Works:                                                      │
│                                                                                  │
│  Step 1: You annotate a class                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  @Service                                                                │   │
│  │  public class UserService {                                              │   │
│  │      private final UserRepository userRepository;                        │   │
│  │      public UserService(UserRepository userRepository) {                 │   │
│  │          this.userRepository = userRepository;                           │   │
│  │      }                                                                   │   │
│  │  }                                                                       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Step 2: @ComponentScan runs at startup                                         │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  @SpringBootApplication  ← includes @ComponentScan                      │   │
│  │  public class MyApp { ... }                                              │   │
│  │                                                                          │   │
│  │  ComponentScan scans: com.example.myapp.**                              │   │
│  │  Found: UserService.class has @Service (which is @Component)            │   │
│  │  → Create BeanDefinition for "userService"                              │   │
│  │  → Register in BeanDefinitionRegistry                                   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Step 3: BeanFactory creates the bean                                           │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  → Resolve constructor dependencies: needs UserRepository               │   │
│  │  → Find UserRepository bean (already created or create it first)        │   │
│  │  → Call: new UserService(userRepositoryBean)                            │   │
│  │  → Run @PostConstruct                                                   │   │
│  │  → Store in singleton cache: "userService" → instance                  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Code Examples — `@Component` and Its Specialisations

```java
// ── 1. @Component — generic bean ─────────────────────────────────────────────────
@Component
public class EmailValidator {
    public boolean isValid(String email) {
        return email != null && email.contains("@");
    }
}
// Bean name: "emailValidator"
// Scope: singleton (default)
// Detected by: @ComponentScan


// ── 2. @Component with custom name ───────────────────────────────────────────────
@Component("customValidator")
public class EmailValidator { ... }
// Bean name: "customValidator" (overridden)


// ── 3. @Service — business logic layer ───────────────────────────────────────────
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    // Constructor injection — @Autowired implied for single constructor
    public OrderService(OrderRepository orderRepository,
                        PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.from(request));
        paymentService.charge(order);
        return OrderResponse.from(order);
    }
}
// Bean name: "orderService"
// Dependencies: orderRepository, paymentService (both must also be beans)


// ── 4. @Repository — data access layer ───────────────────────────────────────────
@Repository
public class CustomUserDao {

    @PersistenceContext
    private EntityManager entityManager;

    public User findByEmail(String email) {
        return entityManager.createQuery(
            "SELECT u FROM User u WHERE u.email = :email", User.class)
            .setParameter("email", email)
            .getSingleResult();
    }
}
// Bean name: "customUserDao"
// Extra: exception translation (JPA exceptions → Spring DataAccessException)


// ── 5. @RestController — web/API layer ───────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.findById(id);
    }
}
// Bean name: "userController"
// Extra: handler mapping (URL → method) + @ResponseBody on all methods


// ── 6. @Configuration — bean definition class ───────────────────────────────────
@Configuration
public class AppConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
// Bean name: "appConfig"
// Extra: CGLIB proxy for @Bean method singleton semantics
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Component Stereotype Hierarchy:                                                │
│                                                                                  │
│  @Component (root)                                                               │
│      │                                                                           │
│      ├── @Service          → Business logic layer (no extra behaviour)          │
│      │                                                                           │
│      ├── @Repository       → Data access layer (+exception translation)         │
│      │                                                                           │
│      ├── @Controller       → Web layer — returns VIEW names                     │
│      │     └── @RestController → Web layer — returns JSON (@ResponseBody)      │
│      │                                                                           │
│      └── @Configuration    → Bean factory class (+CGLIB proxy for @Bean)        │
│                                                                                  │
│  ALL of them make the class a Spring-managed bean.                              │
│  The choice is about SEMANTICS + LAYER-SPECIFIC BEHAVIOUR.                      │
│  When in doubt, use @Component.                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 2.2 Creating Beans with `@Bean`

`@Bean` marks a **method** (inside a `@Configuration` class) whose return value becomes a Spring-managed bean. Use `@Bean` when you cannot annotate the class itself (third-party libraries) or when creation requires complex logic.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Bean — How It Works:                                                           │
│                                                                                  │
│  Step 1: You write a method in a @Configuration class                           │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  @Configuration                                                          │   │
│  │  public class AppConfig {                                                │   │
│  │      @Bean                                                               │   │
│  │      public PasswordEncoder passwordEncoder() {                          │   │
│  │          return new BCryptPasswordEncoder(12);                           │   │
│  │      }                                                                   │   │
│  │  }                                                                       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Step 2: Spring processes @Configuration at startup                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ConfigurationClassPostProcessor finds AppConfig.class                   │   │
│  │  → Sees @Bean method: passwordEncoder()                                 │   │
│  │  → Creates BeanDefinition:                                              │   │
│  │      name = "passwordEncoder" (method name)                             │   │
│  │      factoryBean = appConfig                                             │   │
│  │      factoryMethod = passwordEncoder                                     │   │
│  │  → Registers in BeanDefinitionRegistry                                  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Step 3: BeanFactory calls the method to create the bean                        │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  → Resolve method parameter dependencies (if any)                       │   │
│  │  → Call: appConfig.passwordEncoder()                                    │   │
│  │  → Return value = new BCryptPasswordEncoder(12)                         │   │
│  │  → Store in singleton cache: "passwordEncoder" → instance              │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Code Examples — `@Bean`

```java
// ── 1. Basic @Bean — third-party class ───────────────────────────────────────────
@Configuration
public class AppConfig {

    @Bean    // bean name = "objectMapper"
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
        // ObjectMapper is from Jackson — you CANNOT add @Component to it
        // @Bean is the only way to register it
    }

    @Bean    // bean name = "passwordEncoder"
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
        // BCryptPasswordEncoder is from Spring Security — third-party class
    }

    @Bean    // bean name = "restTemplate"
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Spring injects RestTemplateBuilder as a method parameter
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
}


// ── 2. @Bean with custom name ────────────────────────────────────────────────────
@Bean("primaryDataSource")
public DataSource primaryDataSource() { ... }

@Bean(name = {"ds", "dataSource", "mainDS"})    // multiple aliases
public DataSource dataSource() { ... }


// ── 3. @Bean with initMethod and destroyMethod ──────────────────────────────────
@Bean(initMethod = "start", destroyMethod = "stop")
public ConnectionPool connectionPool() {
    ConnectionPool pool = new ConnectionPool();
    pool.setMaxSize(20);
    pool.setMinIdle(5);
    return pool;
    // Spring calls pool.start() after creation
    // Spring calls pool.stop() on application shutdown
}


// ── 4. @Bean with dependencies via method parameters ─────────────────────────────
@Configuration
public class PersistenceConfig {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaximumPoolSize(20);
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        // Spring injects the DataSource bean defined above
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}


// ── 5. @Bean with dependencies via inter-method calls (CGLIB) ────────────────────
@Configuration    // CGLIB proxy ensures dataSource() returns SAME singleton
public class PersistenceConfig {

    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());   // ← calls dataSource()
        // WITHOUT @Configuration CGLIB proxy: creates NEW DataSource (BUG!)
        // WITH @Configuration CGLIB proxy: returns SAME DataSource singleton ✓
    }

    @Bean
    public NamedParameterJdbcTemplate namedTemplate() {
        return new NamedParameterJdbcTemplate(dataSource());  // ← same singleton
    }
}


// ── 6. Multiple beans of the same type ───────────────────────────────────────────
@Configuration
public class DataSourceConfig {

    @Bean("primaryDS")
    @Primary      // ← default when @Autowired without @Qualifier
    public DataSource primaryDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://primary-host:3306/db");
        return ds;
    }

    @Bean("replicaDS")
    public DataSource replicaDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://replica-host:3306/db");
        return ds;
    }
}

@Service
public class ReportService {
    public ReportService(
            @Qualifier("primaryDS") DataSource primaryDs,     // specific
            @Qualifier("replicaDS") DataSource replicaDs) {   // specific
        // ...
    }
}


// ── 7. @Bean with @Scope ─────────────────────────────────────────────────────────
@Bean
@Scope("prototype")    // new instance every time it's requested
public HttpClient httpClient() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
}


// ── 8. Conditional @Bean ─────────────────────────────────────────────────────────
@Bean
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("users", "products");
}
// Bean only created if application.properties has: cache.enabled=true

@Bean
@Profile("dev")
public DataSource devDataSource() {
    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2).build();
}
// Bean only created when "dev" profile is active
```

##### `@Component` vs `@Bean` — When to Use Which

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Component vs @Bean — Complete Comparison:                                      │
│                                                                                  │
│  ┌──────────────────────────────┬────────────────────────────────────────────┐  │
│  │ @Component                   │ @Bean                                      │  │
│  ├──────────────────────────────┼────────────────────────────────────────────┤  │
│  │ Annotates a CLASS            │ Annotates a METHOD in @Configuration       │  │
│  │ Auto-detected by scan        │ Explicitly declared by developer           │  │
│  │ For classes YOU own          │ For third-party classes you can't modify   │  │
│  │ Simple creation (constructor)│ Complex creation (builder, factory, etc.)  │  │
│  │ One bean per annotated class │ Multiple beans of same type possible       │  │
│  │ Name = lowercase class name  │ Name = method name                         │  │
│  │ DI via @Autowired/constructor│ DI via method parameters                   │  │
│  │ No CGLIB involved            │ CGLIB proxy in @Configuration class        │  │
│  └──────────────────────────────┴────────────────────────────────────────────┘  │
│                                                                                  │
│  Decision:                                                                       │
│  Can you annotate the source code? → @Component                                │
│  Third-party class?               → @Bean                                       │
│  Need multiple instances of same type? → @Bean                                  │
│  Complex creation logic?          → @Bean                                       │
│  Simple class you own?            → @Component                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 3. In Which Cases Bean Could Not Be Created and Throws an Exception

Spring fails **at startup** (not at runtime) when it cannot create a bean. The application context will **not start**, and you'll see one of these exceptions in the log. Here are all the common failure scenarios:

---

#### 3.1 `NoSuchBeanDefinitionException` — Dependency Not Found

The most common error. Spring is trying to inject a dependency, but no bean of that type exists in the container.

```java
// ── Cause: missing dependency ────────────────────────────────────────────────────
@Service
public class UserService {
    private final EmailService emailService;

    public UserService(EmailService emailService) {
        this.emailService = emailService;
    }
}

// But EmailService is NOT a bean:
public class EmailService {   // ← no @Component, @Service, or @Bean!
    public void sendEmail(String to, String subject) { ... }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Exception:                                                                      │
│                                                                                  │
│  org.springframework.beans.factory.NoSuchBeanDefinitionException:               │
│  No qualifying bean of type 'com.example.EmailService' available:               │
│  expected at least 1 bean which qualifies as autowire candidate.                │
│  Dependency annotations: {}                                                      │
│                                                                                  │
│  Causes:                                                                         │
│  1. Forgot @Component/@Service/@Repository on the class                         │
│  2. Class is outside the @ComponentScan base package                            │
│  3. Missing @Bean method in @Configuration                                      │
│  4. Missing library dependency (e.g., no spring-boot-starter-data-jpa)         │
│                                                                                  │
│  Fixes:                                                                          │
│  → Add @Service (or @Component) to EmailService                                │
│  → Or add @Bean method in a @Configuration class                                │
│  → Or extend @ComponentScan to include the package                              │
│  → Or add @Autowired(required = false) if truly optional                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Fix 1: Add @Service ──────────────────────────────────────────────────────────
@Service    // ← now Spring creates this bean
public class EmailService { ... }

// ── Fix 2: Class is in wrong package — extend ComponentScan ──────────────────────
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.myapp", "com.thirdparty.services"})
public class MyApp { ... }

// ── Fix 3: Make dependency optional ──────────────────────────────────────────────
@Service
public class UserService {
    @Autowired(required = false)    // won't fail if EmailService doesn't exist
    private EmailService emailService;
}
```

---

#### 3.2 `NoUniqueBeanDefinitionException` — Multiple Beans of Same Type

Spring finds **more than one** bean matching the required type and doesn't know which one to inject.

```java
// ── Cause: two beans of same type, no disambiguation ─────────────────────────────
@Component
public class StripePaymentGateway implements PaymentGateway { ... }

@Component
public class PayPalPaymentGateway implements PaymentGateway { ... }

@Service
public class CheckoutService {
    public CheckoutService(PaymentGateway gateway) {
        // ERROR: which one? Stripe or PayPal?
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Exception:                                                                      │
│                                                                                  │
│  org.springframework.beans.factory.NoUniqueBeanDefinitionException:             │
│  No qualifying bean of type 'com.example.PaymentGateway' available:             │
│  expected single matching bean but found 2:                                     │
│  stripePaymentGateway, payPalPaymentGateway                                     │
│                                                                                  │
│  Fixes (pick one):                                                               │
│                                                                                  │
│  1. @Primary — mark one as the default:                                         │
│     @Primary @Component                                                          │
│     public class StripePaymentGateway implements PaymentGateway { ... }         │
│                                                                                  │
│  2. @Qualifier — specify which one to inject:                                   │
│     public CheckoutService(                                                      │
│         @Qualifier("stripePaymentGateway") PaymentGateway gateway) { }          │
│                                                                                  │
│  3. Field name matching — rename the parameter:                                 │
│     public CheckoutService(PaymentGateway stripePaymentGateway) { }             │
│     // parameter name matches bean name → Spring picks it                       │
│                                                                                  │
│  4. Inject ALL of them as a List:                                               │
│     public CheckoutService(List<PaymentGateway> gateways) { }                   │
│     // gateways = [StripePaymentGateway, PayPalPaymentGateway]                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Fix with @Primary ────────────────────────────────────────────────────────────
@Component
@Primary    // ← default when no @Qualifier specified
public class StripePaymentGateway implements PaymentGateway { ... }

@Component
public class PayPalPaymentGateway implements PaymentGateway { ... }

@Service
public class CheckoutService {
    public CheckoutService(PaymentGateway gateway) {
        // StripePaymentGateway injected (it's @Primary) ✓
    }
}


// ── Fix with @Qualifier ──────────────────────────────────────────────────────────
@Service
public class CheckoutService {
    public CheckoutService(
            @Qualifier("payPalPaymentGateway") PaymentGateway gateway) {
        // PayPalPaymentGateway injected ✓
    }
}
```

---

#### 3.3 `BeanCurrentlyInCreationException` — Circular Dependency

Bean A depends on Bean B, and Bean B depends on Bean A. Spring cannot create either one because each requires the other to be fully constructed first.

```java
// ── Cause: circular reference ────────────────────────────────────────────────────
@Service
public class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(ServiceB serviceB) {    // needs B
        this.serviceB = serviceB;
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;
    public ServiceB(ServiceA serviceA) {    // needs A
        this.serviceA = serviceA;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Exception:                                                                      │
│                                                                                  │
│  org.springframework.beans.factory.BeanCurrentlyInCreationException:            │
│  Error creating bean with name 'serviceA':                                      │
│  Requested bean is currently in creation:                                        │
│  Is there an unresolvable circular reference?                                   │
│                                                                                  │
│  What happens:                                                                   │
│  Spring tries to create ServiceA                                                │
│      → needs ServiceB (not yet created)                                         │
│      → Spring tries to create ServiceB                                          │
│          → needs ServiceA (currently being created!)                            │
│          → DEADLOCK — neither can be completed                                  │
│      → Exception thrown                                                          │
│                                                                                  │
│  ┌──────────────┐        ┌──────────────┐                                      │
│  │  ServiceA    │ ─────► │  ServiceB    │                                      │
│  │  needs B     │ ◄───── │  needs A     │  ← CIRCULAR!                        │
│  └──────────────┘        └──────────────┘                                      │
│                                                                                  │
│  Note: Spring Boot 2.6+ DISABLES circular references by default.               │
│  Even with field injection, circular dependencies are now an error.             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Fix 1: Redesign — extract shared logic (BEST) ────────────────────────────────
// Instead of A → B → A, extract the shared part:
@Service
public class ServiceA {
    private final SharedService shared;
    public ServiceA(SharedService shared) { this.shared = shared; }
}

@Service
public class ServiceB {
    private final SharedService shared;
    public ServiceB(SharedService shared) { this.shared = shared; }
}

@Service
public class SharedService {
    // contains the logic both A and B need
}


// ── Fix 2: @Lazy — break the cycle with a proxy (WORKAROUND) ────────────────────
@Service
public class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(@Lazy ServiceB serviceB) {
        // Spring injects a PROXY, not the real ServiceB
        // Real ServiceB is created only when first USED
        this.serviceB = serviceB;
    }
}


// ── Fix 3: Use setter injection (NOT recommended) ───────────────────────────────
@Service
public class ServiceA {
    private ServiceB serviceB;

    @Autowired
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}
// Spring can create ServiceA first (no B required at construction)
// Then create ServiceB, then inject B into A via setter
// But this hides the circular dependency — the real fix is redesign!
```

---

#### 3.4 `BeanNotOfRequiredTypeException` — Type Mismatch

The bean exists, but its type doesn't match what's expected.

```java
// ── Cause: wrong type ────────────────────────────────────────────────────────────
@Configuration
public class AppConfig {
    @Bean("userService")
    public String userService() {    // returns a String, not a UserService!
        return "user-service-v1";
    }
}

@RestController
public class UserController {
    public UserController(UserService userService) {
        // Bean "userService" exists, but it's a String, not UserService
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Exception:                                                                      │
│                                                                                  │
│  org.springframework.beans.factory.BeanNotOfRequiredTypeException:              │
│  Bean named 'userService' is expected to be of type 'UserService'              │
│  but was actually of type 'java.lang.String'                                    │
│                                                                                  │
│  Fix: Ensure @Bean method return type matches what consumers expect.            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 3.5 `BeanCreationException` — Constructor/Init Failure

The bean class is found, but creation fails because the constructor or `@PostConstruct` throws an exception.

```java
// ── Cause 1: constructor throws ──────────────────────────────────────────────────
@Service
public class DatabaseService {
    public DatabaseService(@Value("${db.url}") String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("db.url must be configured!");
        }
        // If db.url is missing from properties → exception at startup
    }
}


// ── Cause 2: @PostConstruct throws ───────────────────────────────────────────────
@Component
public class CacheWarmer {
    @PostConstruct
    public void warmCache() {
        // If external service is down → exception
        List<Product> products = externalApi.fetchAllProducts();  // throws!
    }
}


// ── Cause 3: missing property ────────────────────────────────────────────────────
@Service
public class ApiClient {
    @Value("${api.key}")    // property not defined in application.properties
    private String apiKey;
    // → IllegalArgumentException: Could not resolve placeholder 'api.key'
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Exception:                                                                      │
│                                                                                  │
│  org.springframework.beans.factory.BeanCreationException:                       │
│  Error creating bean with name 'databaseService':                               │
│  Invocation of init method failed;                                              │
│  nested exception is java.lang.IllegalArgumentException:                        │
│  db.url must be configured!                                                      │
│                                                                                  │
│  Common causes:                                                                  │
│  1. Constructor throws an exception                                              │
│  2. @PostConstruct method fails                                                  │
│  3. @Value("${property}") references a missing property                         │
│  4. Database connection fails during DataSource bean creation                   │
│  5. Missing required configuration (e.g., spring.datasource.url)                │
│                                                                                  │
│  Fixes:                                                                          │
│  → Add the missing property to application.properties                           │
│  → Use @Value("${prop:default}") with a default value                           │
│  → Handle errors gracefully in @PostConstruct                                   │
│  → Ensure external services are available at startup                            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 3.6 `UnsatisfiedDependencyException` — Cannot Wire Dependency

A wrapper exception that occurs when Spring cannot satisfy a constructor or setter parameter. Usually wraps one of the exceptions above.

```java
// ── Cause: chain of missing dependencies ─────────────────────────────────────────
@Service
public class OrderService {
    public OrderService(PaymentService paymentService) { ... }
}

@Service
public class PaymentService {
    public PaymentService(StripeClient stripeClient) { ... }
}

// StripeClient is NOT a bean → PaymentService fails → OrderService fails
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Exception:                                                                      │
│                                                                                  │
│  org.springframework.beans.factory.UnsatisfiedDependencyException:              │
│  Error creating bean with name 'orderService':                                  │
│  Unsatisfied dependency expressed through constructor parameter 0;              │
│  nested exception is:                                                            │
│  org.springframework.beans.factory.UnsatisfiedDependencyException:              │
│  Error creating bean with name 'paymentService':                                │
│  Unsatisfied dependency expressed through constructor parameter 0;              │
│  nested exception is:                                                            │
│  org.springframework.beans.factory.NoSuchBeanDefinitionException:               │
│  No qualifying bean of type 'StripeClient' available                            │
│                                                                                  │
│  Reading the stack trace:                                                        │
│  → Start from the INNERMOST exception (NoSuchBeanDefinitionException)          │
│  → That tells you the ROOT CAUSE: StripeClient is missing                      │
│  → Fix that, and the entire chain resolves                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 3.7 Other Common Bean Creation Failures

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Other Bean Creation Exceptions:                                                 │
│                                                                                  │
│  ┌───────────────────────────────────────┬───────────────────────────────────┐  │
│  │ Exception                             │ Cause                             │  │
│  ├───────────────────────────────────────┼───────────────────────────────────┤  │
│  │ BeanDefinitionOverrideException       │ Two beans with the SAME NAME.    │  │
│  │                                       │ Default: allowed (last wins).    │  │
│  │                                       │ Spring Boot 2.1+: disabled by    │  │
│  │                                       │ default. Enable with:            │  │
│  │                                       │ spring.main.allow-bean-          │  │
│  │                                       │ definition-overriding=true       │  │
│  ├───────────────────────────────────────┼───────────────────────────────────┤  │
│  │ ConflictingBeanDefinitionException    │ Two @Component beans scanned     │  │
│  │                                       │ with the same bean name from     │  │
│  │                                       │ different packages.              │  │
│  ├───────────────────────────────────────┼───────────────────────────────────┤  │
│  │ BeanInstantiationException            │ Spring cannot instantiate the    │  │
│  │                                       │ class: abstract class, interface,│  │
│  │                                       │ no public constructor, inner     │  │
│  │                                       │ class without static modifier.   │  │
│  ├───────────────────────────────────────┼───────────────────────────────────┤  │
│  │ CannotLoadBeanClassException          │ Class not found on classpath.    │  │
│  │                                       │ Missing dependency in pom.xml.   │  │
│  └───────────────────────────────────────┴───────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Summary — Quick Debugging Guide

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How to Debug Bean Creation Errors:                                              │
│                                                                                  │
│  1. Read the exception type:                                                    │
│     NoSuchBeanDefinitionException     → bean doesn't exist                     │
│     NoUniqueBeanDefinitionException   → too many beans of same type            │
│     BeanCurrentlyInCreationException  → circular dependency                    │
│     BeanCreationException             → constructor/@PostConstruct failed      │
│     UnsatisfiedDependencyException    → dependency chain broken                │
│                                                                                  │
│  2. Read the bean name in the error → which bean failed?                       │
│                                                                                  │
│  3. Read the innermost "nested exception" → the ROOT CAUSE                     │
│                                                                                  │
│  4. Common fixes:                                                                │
│     → Add @Component/@Service/@Repository to the missing class                 │
│     → Add @Bean method for third-party classes                                  │
│     → Add @Primary or @Qualifier for ambiguous types                           │
│     → Break circular dependencies with redesign or @Lazy                       │
│     → Add missing properties to application.properties                          │
│     → Check your @ComponentScan base packages                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 4. `@Component` vs `@Bean` Priority — Which One Wins?

When you have **both** `@Component` on a class **and** a `@Bean` method in a `@Configuration` class that create a bean of the **same type**, what happens?

#### The Short Answer

**`@Bean` wins.** The `@Bean` method **overrides** the `@Component`-scanned bean definition — but only if bean definition overriding is allowed (and only when they produce beans with the same name).

#### Scenario 1 — Same Bean Name (Override)

```java
// ── @Component creates bean named "userService" ─────────────────────────────────
@Service    // bean name = "userService" (lowercase class name)
public class UserService {
    public String identify() { return "I am the @Component UserService"; }
}


// ── @Bean ALSO creates bean named "userService" ─────────────────────────────────
@Configuration
public class AppConfig {

    @Bean    // bean name = "userService" (method name)
    public UserService userService() {
        return new UserService() {
            @Override
            public String identify() { return "I am the @Bean UserService"; }
        };
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens — Same Bean Name:                                                  │
│                                                                                  │
│  Both produce a bean named "userService" of type UserService.                   │
│                                                                                  │
│  Spring Boot 2.1+:                                                               │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │  DEFAULT: spring.main.allow-bean-definition-overriding = false           │  │
│  │                                                                          │  │
│  │  → BeanDefinitionOverrideException at startup!                          │  │
│  │    "The bean 'userService' could not be registered.                     │  │
│  │     A bean with that name has already been defined and overriding       │  │
│  │     is disabled."                                                        │  │
│  │                                                                          │  │
│  │  → Spring REFUSES to start. You must resolve the conflict.              │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  If you enable overriding:                                                       │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │  spring.main.allow-bean-definition-overriding = true                     │  │
│  │                                                                          │  │
│  │  → @Bean definition WINS over @Component definition                     │  │
│  │  → The @Bean method's return value is the bean in the container          │  │
│  │  → The @Component-scanned definition is silently replaced               │  │
│  │                                                                          │  │
│  │  Priority order (highest to lowest):                                     │  │
│  │  1. @Bean in @Configuration class     ← WINS (explicit > implicit)     │  │
│  │  2. @Component / @Service / @Repo     ← OVERRIDDEN                     │  │
│  │                                                                          │  │
│  │  Rationale: @Bean is an EXPLICIT declaration by the developer.          │  │
│  │  @Component is an IMPLICIT auto-detection. Explicit wins.               │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Verify which one wins ────────────────────────────────────────────────────────
// Add to application.properties:
// spring.main.allow-bean-definition-overriding=true

@Component
public class StartupChecker {

    @Autowired
    private UserService userService;

    @PostConstruct
    public void check() {
        System.out.println(userService.identify());
        // Output: "I am the @Bean UserService"
        // The @Bean version won!
    }
}
```

#### Scenario 2 — Different Bean Names (Both Exist)

```java
// ── @Component creates bean named "userService" ─────────────────────────────────
@Service    // bean name = "userService"
public class UserService {
    public String identify() { return "Component version"; }
}


// ── @Bean creates bean named "customUserService" ─────────────────────────────────
@Configuration
public class AppConfig {

    @Bean("customUserService")    // DIFFERENT name
    public UserService customUserService() {
        return new UserService() {
            @Override
            public String identify() { return "Bean version"; }
        };
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens — Different Bean Names:                                            │
│                                                                                  │
│  BOTH beans exist in the container:                                             │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │  Container:                                                              │  │
│  │  "userService"        → UserService (from @Component)                   │  │
│  │  "customUserService"  → UserService (from @Bean)                        │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Problem: If you @Autowired UserService, Spring finds TWO beans               │
│  of type UserService → NoUniqueBeanDefinitionException!                        │
│                                                                                  │
│  Fixes:                                                                          │
│  1. @Primary on one of them                                                     │
│  2. @Qualifier("userService") or @Qualifier("customUserService")               │
│  3. Remove one of them (usually remove the @Component if @Bean is intentional) │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Fix: use @Primary on the @Bean to make it the default ────────────────────────
@Configuration
public class AppConfig {

    @Bean("customUserService")
    @Primary    // ← this one wins when @Autowired by type
    public UserService customUserService() {
        return new EnhancedUserService();
    }
}

@RestController
public class UserController {
    public UserController(UserService userService) {
        // customUserService injected (it's @Primary) ✓
    }
}
```

#### Scenario 3 — Override to Customise Auto-Configured Beans

This is the **most practical use case** — overriding a Spring Boot auto-configured bean with your own `@Bean`:

```java
// ── Spring Boot auto-configures an ObjectMapper bean ─────────────────────────────
// (from JacksonAutoConfiguration — comes with spring-boot-starter-web)

// You can OVERRIDE it with your own @Bean:
@Configuration
public class JacksonConfig {

    @Bean    // overrides the auto-configured ObjectMapper
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
// This works because Spring Boot auto-configuration uses @ConditionalOnMissingBean:
//
// @ConditionalOnMissingBean     ← "only create if no ObjectMapper bean exists yet"
// public ObjectMapper objectMapper() { ... }
//
// Your @Bean is processed FIRST → ObjectMapper bean exists
// → Auto-configuration's @ConditionalOnMissingBean → skipped
// → Your custom ObjectMapper is used everywhere
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Override Priority — Complete Picture:                                            │
│                                                                                  │
│  Highest priority (wins)                                                         │
│  ─────────────────────────────────────────────────────────────────────────────── │
│  1. Your @Bean in @Configuration              ← explicit, developer-defined     │
│  2. Your @Component / @Service / @Repository  ← auto-scanned                   │
│  3. Spring Boot auto-configuration @Bean      ← @ConditionalOnMissingBean       │
│  ─────────────────────────────────────────────────────────────────────────────── │
│  Lowest priority (overridden)                                                    │
│                                                                                  │
│  This is the beauty of Spring Boot auto-configuration:                          │
│  → It provides sensible defaults                                                │
│  → You can override ANY auto-configured bean with your own @Bean                │
│  → @ConditionalOnMissingBean ensures your definition takes precedence           │
│                                                                                  │
│  Same name, overriding disabled (Spring Boot 2.1+ default):                     │
│  → BeanDefinitionOverrideException at startup                                  │
│  → Fix: use different bean names, @Primary, or @Qualifier                      │
│                                                                                  │
│  Same name, overriding enabled:                                                  │
│  → @Bean silently replaces @Component bean                                     │
│  → @Bean replaces auto-configuration bean                                       │
│  → Last registered BeanDefinition wins                                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Practical Recommendation

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Best Practice — Avoid the Conflict Entirely:                                    │
│                                                                                  │
│  1. Use @Component for your own simple classes (services, repos, controllers)   │
│  2. Use @Bean for third-party classes you can't annotate                        │
│  3. NEVER put @Component on a class AND define a @Bean for it                  │
│     → Pick ONE approach per class                                               │
│                                                                                  │
│  4. If you need to customise a Spring Boot auto-configured bean:                │
│     → Just define your own @Bean — auto-config yields automatically            │
│     → @ConditionalOnMissingBean handles it cleanly                              │
│                                                                                  │
│  5. If you MUST override (e.g., testing), enable overriding:                    │
│     spring.main.allow-bean-definition-overriding=true                           │
│     → But this is a code smell in production                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 5. How Does Spring Boot Find All the Beans?

Spring Boot discovers beans through **two mechanisms**: `@ComponentScan` (automatic class-level detection) and `@Configuration` + `@Bean` (explicit method-level declaration). Both work together during startup to populate the IoC container.

---

#### 5.1 `@ComponentScan` — Automatic Discovery

`@ComponentScan` tells Spring: "scan these packages and register every class annotated with `@Component` (or its stereotypes) as a bean."

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How @ComponentScan Works — Step by Step:                                        │
│                                                                                  │
│  Step 1: @SpringBootApplication triggers scanning                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  @SpringBootApplication   ← This is a composed annotation:              │   │
│  │    = @Configuration                                                      │   │
│  │    + @EnableAutoConfiguration                                            │   │
│  │    + @ComponentScan(basePackages = "com.example.myapp")                  │   │
│  │                                                                          │   │
│  │  Base package = the package of the main class (default)                 │   │
│  │  If MyApp is in com.example.myapp → scans com.example.myapp.**         │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Step 2: ClassPathBeanDefinitionScanner runs                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Scanner walks every .class file under the base package:                │   │
│  │                                                                          │   │
│  │  com.example.myapp/                                                      │   │
│  │  ├── MyApp.class                     → has @SpringBootApplication       │   │
│  │  ├── controller/                                                         │   │
│  │  │   └── UserController.class        → has @RestController ✓            │   │
│  │  ├── service/                                                            │   │
│  │  │   ├── UserService.class           → has @Service ✓                   │   │
│  │  │   └── OrderService.class          → has @Service ✓                   │   │
│  │  ├── repository/                                                         │   │
│  │  │   └── UserRepository.class        → has @Repository ✓               │   │
│  │  ├── config/                                                             │   │
│  │  │   └── AppConfig.class             → has @Configuration ✓             │   │
│  │  ├── model/                                                              │   │
│  │  │   └── User.class                  → no annotation ✗ (not a bean)    │   │
│  │  └── dto/                                                                │   │
│  │      └── UserResponse.class          → no annotation ✗ (not a bean)    │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Step 3: Register BeanDefinitions                                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  For each detected class, create a BeanDefinition and register it:      │   │
│  │                                                                          │   │
│  │  BeanDefinitionRegistry:                                                 │   │
│  │  "userController"   → BeanDef{class=UserController, scope=singleton}   │   │
│  │  "userService"      → BeanDef{class=UserService, scope=singleton}      │   │
│  │  "orderService"     → BeanDef{class=OrderService, scope=singleton}     │   │
│  │  "userRepository"   → BeanDef{class=UserRepository, scope=singleton}   │   │
│  │  "appConfig"        → BeanDef{class=AppConfig, scope=singleton}        │   │
│  │                                                                          │   │
│  │  NOT registered: User.class, UserResponse.class (no stereotype)        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── What annotations make a class scannable? ─────────────────────────────────────
// ANY annotation that is meta-annotated with @Component:

@Component       // generic bean
@Service         // business logic layer
@Repository      // data access layer (+ exception translation)
@Controller      // web MVC layer (view resolution)
@RestController  // web API layer (@Controller + @ResponseBody)
@Configuration   // bean definition class (@Bean factory)

// Custom stereotype annotations also work:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component       // ← meta-annotated with @Component
public @interface Gateway { }

@Gateway         // ← scanned because @Gateway is @Component
public class PaymentGateway { ... }
```

```java
// ── Customising @ComponentScan ───────────────────────────────────────────────────

// 1. Default: scan the main class package and all sub-packages
@SpringBootApplication    // @ComponentScan is included — scans from this package
public class MyApp { ... }


// 2. Scan additional packages
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.myapp",           // your app
    "com.thirdparty.services",     // third-party module
    "com.shared.utilities"         // shared library
})
public class MyApp { ... }


// 3. Type-safe scanning with basePackageClasses
@ComponentScan(basePackageClasses = {
    MyApp.class,                   // scans com.example.myapp.**
    SharedConfig.class             // scans com.shared.**
})
public class MyApp { ... }


// 4. Exclude specific classes or patterns
@ComponentScan(
    basePackages = "com.example.myapp",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                              classes = TestService.class),
        @ComponentScan.Filter(type = FilterType.REGEX,
                              pattern = "com\\.example\\.myapp\\.legacy\\..*")
    }
)
public class MyApp { ... }


// 5. Include only specific annotations
@ComponentScan(
    basePackages = "com.example.myapp",
    useDefaultFilters = false,    // ← disable default @Component scanning
    includeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION,
                              classes = Service.class)
    }
)
// Now ONLY @Service beans are detected — @Component, @Repository etc. are IGNORED
```

---

#### 5.2 `@Configuration` + `@Bean` — Explicit Declaration

`@Configuration` classes are themselves discovered by `@ComponentScan`. Then Spring processes the `@Bean` methods inside them to register additional beans.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Configuration Processing — Step by Step:                                       │
│                                                                                  │
│  Step 1: @ComponentScan finds @Configuration classes                            │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  @Configuration                                                          │   │
│  │  public class AppConfig {                                                │   │
│  │      @Bean public ObjectMapper objectMapper() { ... }                   │   │
│  │      @Bean public PasswordEncoder encoder() { ... }                     │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  @Configuration                                                          │   │
│  │  public class PersistenceConfig {                                        │   │
│  │      @Bean public DataSource dataSource() { ... }                       │   │
│  │      @Bean public JdbcTemplate jdbcTemplate() { ... }                   │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  Scanner finds: AppConfig ✓, PersistenceConfig ✓                       │   │
│  │  Both are beans themselves AND contain @Bean methods                    │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Step 2: ConfigurationClassPostProcessor processes @Bean methods                │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  For each @Configuration class:                                          │   │
│  │  → Create CGLIB proxy of the class (for singleton semantics)            │   │
│  │  → Parse all @Bean methods                                              │   │
│  │  → Create BeanDefinition for each:                                      │   │
│  │      "objectMapper"   → factoryBean=appConfig, method=objectMapper     │   │
│  │      "encoder"        → factoryBean=appConfig, method=encoder          │   │
│  │      "dataSource"     → factoryBean=persistenceConfig, method=dataS... │   │
│  │      "jdbcTemplate"   → factoryBean=persistenceConfig, method=jdbcT... │   │
│  │  → Register in BeanDefinitionRegistry                                   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Step 3: BeanFactory creates beans by calling factory methods                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  → Resolve method parameter dependencies                                │   │
│  │  → Call: appConfigProxy.objectMapper()                                  │   │
│  │  → Store return value in singleton cache                                │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5.3 `@EnableAutoConfiguration` — Spring Boot's Third Discovery Mechanism

Beyond `@ComponentScan` and `@Configuration`, Spring Boot adds a **third** mechanism: auto-configuration. This is what makes Spring Boot "opinionated" — it automatically configures beans based on the libraries on your classpath.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @EnableAutoConfiguration — How It Works:                                        │
│                                                                                  │
│  Step 1: @SpringBootApplication includes @EnableAutoConfiguration               │
│                                                                                  │
│  Step 2: Spring reads META-INF/spring/                                          │
│          org.springframework.boot.autoconfigure.AutoConfiguration.imports        │
│          (Spring Boot 3.x) or META-INF/spring.factories (Spring Boot 2.x)      │
│                                                                                  │
│  Step 3: Each auto-configuration class is a @Configuration with conditions      │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  // From spring-boot-autoconfigure.jar:                                  │   │
│  │                                                                          │   │
│  │  @Configuration                                                          │   │
│  │  @ConditionalOnClass(DataSource.class)        ← only if JDBC on path   │   │
│  │  @ConditionalOnProperty("spring.datasource.url")  ← only if configured │   │
│  │  public class DataSourceAutoConfiguration {                              │   │
│  │      @Bean                                                               │   │
│  │      @ConditionalOnMissingBean                  ← only if YOU didn't    │   │
│  │      public DataSource dataSource() { ... }     ← define your own      │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  @Configuration                                                          │   │
│  │  @ConditionalOnClass(ObjectMapper.class)      ← Jackson on classpath?  │   │
│  │  public class JacksonAutoConfiguration {                                 │   │
│  │      @Bean                                                               │   │
│  │      @ConditionalOnMissingBean                                           │   │
│  │      public ObjectMapper objectMapper() { ... }                         │   │
│  │  }                                                                       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Result: hundreds of beans created automatically based on your dependencies     │
│  Added spring-boot-starter-web?    → DispatcherServlet, TomcatServer beans    │
│  Added spring-boot-starter-data-jpa? → EntityManagerFactory, DataSource beans  │
│  Added spring-boot-starter-security? → SecurityFilterChain, AuthManager beans  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5.4 Complete Bean Discovery Flow — All Three Mechanisms Combined

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot Startup — Bean Discovery Order:                                     │
│                                                                                  │
│  @SpringBootApplication                                                          │
│  ├── @ComponentScan ─────────────────────────────────────────────────────────┐  │
│  │   │                                                                       │  │
│  │   ├── Scan classpath for @Component classes                              │  │
│  │   ├── Register: @Service, @Repository, @Controller, @RestController     │  │
│  │   ├── Register: @Configuration classes (as beans)                        │  │
│  │   └── Result: BeanDefinitions for all annotated classes                  │  │
│  │                                                                           │  │
│  ├── Process @Configuration classes ─────────────────────────────────────┐  │  │
│  │   │                                                                   │  │  │
│  │   ├── ConfigurationClassPostProcessor runs                           │  │  │
│  │   ├── Parse @Bean methods in each @Configuration                     │  │  │
│  │   ├── Handle @Import, @ImportResource                                │  │  │
│  │   └── Result: BeanDefinitions for all @Bean methods                  │  │  │
│  │                                                                       │  │  │
│  ├── @EnableAutoConfiguration ───────────────────────────────────────┐  │  │  │
│  │   │                                                               │  │  │  │
│  │   ├── Load AutoConfiguration.imports                             │  │  │  │
│  │   ├── Evaluate @Conditional annotations                          │  │  │  │
│  │   ├── Skip classes where conditions NOT met                      │  │  │  │
│  │   ├── Process @Bean methods in matching classes                   │  │  │  │
│  │   └── Result: BeanDefinitions for auto-configured beans          │  │  │  │
│  │                                                                   │  │  │  │
│  └── BeanFactory creates all beans ──────────────────────────────┐  │  │  │  │
│      │                                                            │  │  │  │  │
│      ├── Sort by dependency graph                                │  │  │  │  │
│      ├── Create each bean (dependencies first)                   │  │  │  │  │
│      ├── Inject dependencies                                      │  │  │  │  │
│      ├── Run lifecycle callbacks (@PostConstruct, etc.)           │  │  │  │  │
│      └── Store in singleton cache                                 │  │  │  │  │
│                                                                    │  │  │  │  │
│      ═══ APPLICATION IS READY ═══                                 │  │  │  │  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── See all discovered beans at startup ──────────────────────────────────────────
@SpringBootApplication
public class MyApp {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(MyApp.class, args);

        // Print all bean names
        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        System.out.println("Total beans: " + beanNames.length);
        for (String name : beanNames) {
            System.out.println("  " + name + " → " +
                ctx.getBean(name).getClass().getSimpleName());
        }
        // Typical Spring Boot web app: 150-300+ beans!
    }
}

// ── Alternatively, use Actuator ──────────────────────────────────────────────────
// Add spring-boot-starter-actuator dependency
// GET /actuator/beans → JSON of all beans, their scope, type, and dependencies
```

---

### 6. At What Time Are Beans Created? — Eager vs Lazy Initialization

Not all beans are created at the same time. Spring uses **two strategies**: **eager** (at startup) and **lazy** (on first use).

---

#### 6.1 Eager Initialization (Default)

By default, **all singleton beans** are created during application startup, when the `ApplicationContext` is being built. This is called **eager initialization**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Eager Initialization — Timeline:                                                │
│                                                                                  │
│  SpringApplication.run(MyApp.class, args)                                       │
│  │                                                                               │
│  ├── Create ApplicationContext                                                  │
│  ├── Run @ComponentScan → find all bean definitions                            │
│  ├── Process @Configuration → find @Bean definitions                           │
│  ├── Process AutoConfiguration → find auto-configured beans                    │
│  │                                                                               │
│  ├── ═══ CREATE ALL SINGLETON BEANS (EAGER) ═══                                │
│  │   ├── Create DataSource bean                     ← created NOW              │
│  │   ├── Create EntityManagerFactory bean            ← created NOW              │
│  │   ├── Create UserRepository bean                  ← created NOW              │
│  │   ├── Create UserService bean                     ← created NOW              │
│  │   ├── Create UserController bean                  ← created NOW              │
│  │   ├── Create ObjectMapper bean                    ← created NOW              │
│  │   ├── Create DispatcherServlet bean               ← created NOW              │
│  │   ├── Create SecurityFilterChain bean             ← created NOW              │
│  │   └── ... (100+ more beans)                       ← ALL created NOW         │
│  │                                                                               │
│  ├── Run ApplicationRunner / CommandLineRunner                                  │
│  ├── Start embedded Tomcat                                                      │
│  └── ═══ APPLICATION READY ═══                                                  │
│                                                                                  │
│  ALL singleton beans exist BEFORE the first HTTP request arrives.               │
│                                                                                  │
│  Advantages:                                                                     │
│  ✓ Fail-fast: configuration errors caught at startup, not at runtime           │
│  ✓ No latency on first request: everything is already wired                    │
│  ✓ Predictable: you know all beans are ready                                   │
│                                                                                  │
│  Disadvantage:                                                                   │
│  ✗ Slower startup: all beans created upfront even if not immediately needed    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Eager initialization in action ───────────────────────────────────────────────
@Service
public class UserService {

    public UserService(UserRepository userRepository) {
        System.out.println("UserService created!");    // prints DURING startup
    }
}

// Output (at startup, before any request):
// UserService created!

// ALL singleton beans print their constructor messages at startup.
// If UserRepository can't be created → startup FAILS (fail-fast ✓)
```

---

#### 6.2 Lazy Initialization

Lazy beans are **NOT** created at startup. They are created **on first access** — when someone first injects them or calls `getBean()`.

##### When Does Lazy Initialization Happen?

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Beans That Are Lazily Initialized:                                              │
│                                                                                  │
│  ┌────────────────────────────────────┬──────────────────────────────────────┐  │
│  │ Bean / Scope                       │ When Created                         │  │
│  ├────────────────────────────────────┼──────────────────────────────────────┤  │
│  │ Singleton (default)                │ At startup (EAGER) ← default        │  │
│  │ Singleton + @Lazy                  │ On first access (LAZY)              │  │
│  │ Prototype (@Scope("prototype"))    │ On EVERY getBean() / injection      │  │
│  │                                    │ (inherently lazy — never cached)    │  │
│  │ Request (@Scope("request"))        │ On each HTTP request                │  │
│  │ Session (@Scope("session"))        │ On each HTTP session                │  │
│  │ Application (@Scope("application"))│ At startup (eager, like singleton)  │  │
│  └────────────────────────────────────┴──────────────────────────────────────┘  │
│                                                                                  │
│  Key insight:                                                                    │
│  • Singleton → eager by default, can opt into lazy with @Lazy                  │
│  • Prototype → ALWAYS lazy (new instance every time, never pre-created)        │
│  • Request/Session → scoped to HTTP lifecycle, created per scope               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### `@Lazy` — Make a Singleton Bean Lazy

```java
// ── 1. @Lazy on a class ──────────────────────────────────────────────────────────
@Service
@Lazy    // ← bean is NOT created at startup
public class ReportService {

    public ReportService(DataSource dataSource) {
        System.out.println("ReportService created!");
        // This does NOT print at startup
        // It prints only when ReportService is first used
    }

    public Report generateReport() { ... }
}

// Timeline:
// Startup → all other beans created, ReportService is SKIPPED
// First call to reportService.generateReport() → ReportService created NOW
// Subsequent calls → same instance reused (it's still a singleton)


// ── 2. @Lazy on an injection point ───────────────────────────────────────────────
@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(@Lazy ReportService reportService) {
        // Spring injects a PROXY, not the real ReportService
        // Real bean is created when first METHOD is called on the proxy
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public Report getReport() {
        return reportService.generateReport();
        // ↑ FIRST call → proxy creates real ReportService, then delegates
        // Subsequent calls → same real instance
    }
}


// ── 3. @Lazy on @Bean ────────────────────────────────────────────────────────────
@Configuration
public class AppConfig {

    @Bean
    @Lazy
    public ExpensiveClient expensiveClient() {
        // This method is NOT called at startup
        // Only called when expensiveClient is first injected/used
        return new ExpensiveClient("https://api.example.com");
    }
}


// ── 4. @Lazy on @Configuration (makes ALL @Bean methods lazy) ────────────────────
@Configuration
@Lazy    // ← ALL beans in this class are lazy
public class AnalyticsConfig {

    @Bean
    public AnalyticsService analyticsService() { ... }    // lazy

    @Bean
    public TrackingService trackingService() { ... }      // lazy

    @Bean
    @Lazy(false)    // ← override: this one is EAGER despite class-level @Lazy
    public MetricsCollector metricsCollector() { ... }    // eager
}
```

##### Global Lazy Initialization

```java
// ── Make ALL beans lazy via application.properties ───────────────────────────────
// application.properties:
// spring.main.lazy-initialization=true

// OR programmatically:
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MyApp.class);
        app.setLazyInitialization(true);    // ALL beans are lazy
        app.run(args);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Global Lazy Initialization — Pros and Cons:                                     │
│                                                                                  │
│  spring.main.lazy-initialization=true                                           │
│                                                                                  │
│  Advantages:                                                                     │
│  ✓ Much faster startup time (beans created only when needed)                   │
│  ✓ Great for development (fast restart)                                        │
│  ✓ Lower memory at startup (unused beans never created)                        │
│                                                                                  │
│  Disadvantages:                                                                  │
│  ✗ Configuration errors are NOT caught at startup                              │
│  ✗ Errors appear at RUNTIME when the lazy bean is first used                   │
│  ✗ First request may be slow (bean creation + dependency wiring)               │
│  ✗ @Scheduled and @EventListener beans won't trigger until accessed            │
│                                                                                  │
│  Recommendation:                                                                 │
│  → Use in DEVELOPMENT for fast restarts                                         │
│  → Do NOT use in PRODUCTION — you want fail-fast at startup                    │
│  → In production, use selective @Lazy on specific heavy beans                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Prototype Scope — A New Instance Every Time

```java
// ── Prototype beans are inherently lazy ──────────────────────────────────────────
@Component
@Scope("prototype")
public class ShoppingCart {

    private List<Item> items = new ArrayList<>();

    public ShoppingCart() {
        System.out.println("New ShoppingCart created: " + this.hashCode());
    }

    public void addItem(Item item) { items.add(item); }
    public List<Item> getItems() { return items; }
}

@Service
public class CheckoutService {

    private final ApplicationContext context;

    public CheckoutService(ApplicationContext context) {
        this.context = context;
    }

    public ShoppingCart newCart() {
        // Every call creates a NEW ShoppingCart instance
        return context.getBean(ShoppingCart.class);
    }
}

// Output:
// checkoutService.newCart() → "New ShoppingCart created: 12345678"
// checkoutService.newCart() → "New ShoppingCart created: 87654321"  ← DIFFERENT
// checkoutService.newCart() → "New ShoppingCart created: 11223344"  ← DIFFERENT

// Prototype beans are NEVER stored in the singleton cache.
// Spring creates them on demand and does NOT manage their destruction.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Eager vs Lazy — Complete Comparison:                                            │
│                                                                                  │
│  ┌──────────────────────────┬──────────────────────┬─────────────────────────┐  │
│  │                          │ Eager (default)       │ Lazy (@Lazy)            │  │
│  ├──────────────────────────┼──────────────────────┼─────────────────────────┤  │
│  │ When created             │ Application startup  │ First access/injection  │  │
│  │ Fail-fast                │ ✓ Yes               │ ✗ No (runtime errors)  │  │
│  │ Startup time             │ Slower               │ Faster                  │  │
│  │ First request latency    │ None (already ready) │ Higher (bean created)   │  │
│  │ Memory at startup        │ Higher               │ Lower                   │  │
│  │ Applies to               │ All singletons       │ @Lazy singletons        │  │
│  │ Production recommended   │ ✓ Yes               │ Selective only          │  │
│  │ Development recommended  │ Optional             │ ✓ Yes (fast restart)   │  │
│  └──────────────────────────┴──────────────────────┴─────────────────────────┘  │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐    │
│  │  Scope-Based Initialization:                                             │    │
│  │                                                                          │    │
│  │  Singleton   → eager at startup (one instance, reused everywhere)       │    │
│  │  Singleton   → lazy with @Lazy (created on first use, then reused)      │    │
│  │  Prototype   → always lazy (new instance every getBean(), never cached) │    │
│  │  Request     → per HTTP request (new for each request)                  │    │
│  │  Session     → per HTTP session (new for each session)                  │    │
│  │  Application → eager at startup (like singleton but ServletContext)      │    │
│  └──────────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 7. How Does Spring Boot Maintain the Order and Priority of Bean Creation?

When you have hundreds of beans, Spring must decide **which bean to create first**. This is critical because a bean cannot be created until all its dependencies exist.

---

#### 7.1 Dependency Graph — Automatic Ordering

Spring's **primary** ordering mechanism is the **dependency graph**. It analyses which beans depend on which, builds a directed acyclic graph (DAG), and performs a **topological sort** to determine creation order.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dependency-Based Bean Creation Order:                                           │
│                                                                                  │
│  Your beans and their dependencies:                                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  UserController → UserService → UserRepository → DataSource            │   │
│  │  OrderController → OrderService → OrderRepository → DataSource         │   │
│  │  OrderService → PaymentService                                          │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Dependency Graph:                                                               │
│  ┌────────────────┐    ┌────────────────┐                                      │
│  │ UserController │───▶│ UserService    │                                      │
│  └────────────────┘    └───────┬────────┘                                      │
│                                │                                                 │
│  ┌────────────────┐    ┌───────▼────────┐    ┌────────────────┐                │
│  │OrderController │───▶│ OrderService   │───▶│PaymentService  │                │
│  └────────────────┘    └───────┬────────┘    └────────────────┘                │
│                                │                                                 │
│                        ┌───────▼────────┐                                      │
│                        │ UserRepository │                                      │
│                        │ OrderRepository│                                      │
│                        └───────┬────────┘                                      │
│                                │                                                 │
│                        ┌───────▼────────┐                                      │
│                        │   DataSource   │  ← created FIRST (no dependencies)   │
│                        └────────────────┘                                      │
│                                                                                  │
│  Creation Order (topological sort — leaves first):                              │
│  1. DataSource                (no dependencies)                                 │
│  2. PaymentService            (no dependencies)                                 │
│  3. UserRepository            (depends on DataSource ✓)                        │
│  4. OrderRepository           (depends on DataSource ✓)                        │
│  5. UserService               (depends on UserRepository ✓)                    │
│  6. OrderService              (depends on OrderRepository ✓, PaymentService ✓) │
│  7. UserController            (depends on UserService ✓)                       │
│  8. OrderController           (depends on OrderService ✓)                      │
│                                                                                  │
│  Spring figures this out AUTOMATICALLY from constructor/field injection.        │
│  You almost NEVER need to specify order manually.                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Spring resolves the graph automatically ──────────────────────────────────────
// You just declare dependencies — Spring handles the order:

@Service
public class OrderService {
    // Spring knows: OrderService needs OrderRepository AND PaymentService
    // → Create both BEFORE creating OrderService
    public OrderService(OrderRepository orderRepo,
                        PaymentService paymentService) {
        this.orderRepo = orderRepo;
        this.paymentService = paymentService;
    }
}

// What if two beans have NO dependency relationship?
// → Order is UNDEFINED (non-deterministic). Spring picks any order.
// → This is fine — if they don't depend on each other, order doesn't matter.
```

---

#### 7.2 `@DependsOn` — Force Explicit Ordering

When two beans have no direct injection dependency but one must be created before the other (e.g., one initialises shared state), use `@DependsOn`.

```java
// ── @DependsOn — force bean creation order ───────────────────────────────────────
@Component
public class DatabaseMigration {

    @PostConstruct
    public void migrate() {
        // Runs Flyway/Liquibase migrations
        // Must complete BEFORE any repository bean tries to query
    }
}

@Repository
@DependsOn("databaseMigration")    // ← create databaseMigration FIRST
public class UserRepository {
    // Spring ensures DatabaseMigration bean exists before creating this
}


// ── Multiple dependencies ────────────────────────────────────────────────────────
@Service
@DependsOn({"cacheInitializer", "configLoader"})
public class ProductService { ... }
// Both cacheInitializer AND configLoader are created before ProductService


// ── @DependsOn on @Bean ──────────────────────────────────────────────────────────
@Bean
@DependsOn("flyway")
public DataSource dataSource() { ... }
// Flyway bean runs migrations before DataSource is created
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @DependsOn — When to Use:                                                       │
│                                                                                  │
│  ✓ Bean A must initialise shared state (cache, config, migration)              │
│    before Bean B can work — but B doesn't inject A directly                    │
│                                                                                  │
│  ✗ Do NOT use @DependsOn for normal injection dependencies                     │
│    → Constructor injection handles that automatically                           │
│                                                                                  │
│  ✗ Avoid overuse — it creates hidden coupling between beans                    │
│    → Prefer constructor injection which makes dependencies explicit            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 7.3 `@Order` and `@Priority` — Ordering Collections of Beans

`@Order` and `@Priority` do **NOT** control when a bean is created. They control the **order within a list** when multiple beans of the same type are injected as a collection.

```java
// ── @Order — sorting beans in injected lists ─────────────────────────────────────
public interface Filter {
    void doFilter(Request request);
}

@Component
@Order(1)    // ← first in the list
public class AuthenticationFilter implements Filter {
    public void doFilter(Request request) {
        System.out.println("1. Authentication check");
    }
}

@Component
@Order(2)    // ← second in the list
public class AuthorizationFilter implements Filter {
    public void doFilter(Request request) {
        System.out.println("2. Authorization check");
    }
}

@Component
@Order(3)    // ← third in the list
public class LoggingFilter implements Filter {
    public void doFilter(Request request) {
        System.out.println("3. Logging");
    }
}

@Service
public class FilterChain {

    private final List<Filter> filters;

    public FilterChain(List<Filter> filters) {
        // filters = [AuthenticationFilter, AuthorizationFilter, LoggingFilter]
        // Ordered by @Order value: 1, 2, 3
        this.filters = filters;
    }

    public void execute(Request request) {
        filters.forEach(f -> f.doFilter(request));
        // Output:
        // 1. Authentication check
        // 2. Authorization check
        // 3. Logging
    }
}


// ── @Order constants ─────────────────────────────────────────────────────────────
@Order(Ordered.HIGHEST_PRECEDENCE)    // = Integer.MIN_VALUE (runs first)
@Order(Ordered.LOWEST_PRECEDENCE)     // = Integer.MAX_VALUE (runs last)
@Order(0)                             // default-ish (custom)

// Lower number = higher priority = first in the list
// @Order(1) comes BEFORE @Order(10)
```

```java
// ── @Priority (javax.annotation.Priority / jakarta.annotation.Priority) ──────────
// Works like @Order but also affects WHICH bean is chosen for single injection

@Component
@Priority(1)    // ← highest priority
public class PrimaryPaymentGateway implements PaymentGateway { ... }

@Component
@Priority(2)
public class FallbackPaymentGateway implements PaymentGateway { ... }

@Service
public class CheckoutService {
    // Single injection: @Priority(1) bean is chosen (PrimaryPaymentGateway)
    public CheckoutService(PaymentGateway gateway) { ... }

    // Collection injection: ordered by @Priority (1 first, then 2)
    // public CheckoutService(List<PaymentGateway> gateways) { ... }
}
```

---

#### 7.4 `Ordered` Interface — Programmatic Ordering

Instead of the `@Order` annotation, you can implement the `Ordered` interface to return the order value programmatically.

```java
// ── Ordered interface ────────────────────────────────────────────────────────────
@Component
public class RateLimitFilter implements Filter, Ordered {

    @Value("${ratelimit.filter.order:5}")
    private int order;

    @Override
    public int getOrder() {
        return order;    // configurable via properties!
    }

    @Override
    public void doFilter(Request request) { ... }
}
// This is useful when order must be configurable at runtime
```

---

#### 7.5 `SmartInitializingSingleton` and Initialization Ordering

After **all** singleton beans are created, Spring calls `SmartInitializingSingleton.afterSingletonsInstantiated()`. Use this for logic that must run after the entire container is ready.

```java
// ── SmartInitializingSingleton — runs after ALL singletons are created ────────────
@Component
public class SystemHealthChecker implements SmartInitializingSingleton {

    @Autowired
    private List<HealthIndicator> healthIndicators;

    @Override
    public void afterSingletonsInstantiated() {
        // ALL beans are created at this point — safe to check everything
        for (HealthIndicator indicator : healthIndicators) {
            Health health = indicator.health();
            System.out.println(indicator.getClass().getSimpleName()
                + " → " + health.getStatus());
        }
    }
}
// This runs AFTER all @PostConstruct methods have completed.
// Use this instead of @PostConstruct when you need ALL beans ready.
```

---

#### 7.6 `@AutoConfigureOrder` and `@AutoConfigureBefore` / `@AutoConfigureAfter`

These annotations are for **auto-configuration classes only** (not regular beans). They control the order in which Spring Boot processes auto-configuration classes.

```java
// ── @AutoConfigureBefore / @AutoConfigureAfter ───────────────────────────────────
// Used in custom auto-configuration (spring.factories / AutoConfiguration.imports)

@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class CustomDataSourceConfig {
    // This config is processed BEFORE Spring Boot's DataSourceAutoConfiguration
    // Useful when you need to set up something before the default kicks in
}

@Configuration
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class CustomSerializerConfig {
    // This config is processed AFTER JacksonAutoConfiguration
    // ObjectMapper bean is already available
}

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class EarlyConfig {
    // Processed before most other auto-configurations
}

// IMPORTANT: These ONLY work for auto-configuration classes listed in
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
// They do NOT work on regular @Configuration classes.
```

---

#### 7.7 Complete Priority and Ordering Summary

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Bean Creation Order — How Spring Decides:                                       │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ Mechanism              │ What It Controls         │ When to Use        │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │ Dependency Graph       │ Bean CREATION order      │ Always (automatic) │    │
│  │ (constructor injection)│ (A needs B → B first)    │ No annotation      │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │ @DependsOn             │ Bean CREATION order      │ Hidden dependency  │    │
│  │                        │ (force A before B)       │ (no injection)     │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │ @Order                 │ ORDER in List<T>         │ Sorting injected   │    │
│  │                        │ (NOT creation order)     │ collections         │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │ @Priority              │ ORDER in List<T> +       │ Default selection  │    │
│  │                        │ single injection winner  │ + collection sort  │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │ Ordered interface      │ Same as @Order           │ Configurable order │    │
│  │                        │ (programmatic)           │ at runtime         │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │ @Primary               │ Which bean wins for      │ Disambiguation     │    │
│  │                        │ single type injection    │ (not ordering)     │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │ @AutoConfigureBefore   │ Auto-config processing   │ Custom auto-config │    │
│  │ @AutoConfigureAfter    │ order (not bean creation)│ classes only       │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│                                                                                  │
│  CRITICAL DISTINCTION:                                                           │
│  • @DependsOn → controls WHEN a bean is CREATED                                │
│  • @Order / @Priority → controls POSITION in a LIST (not creation time)        │
│  • @Primary → controls WHICH bean is SELECTED (not order or timing)            │
│  • Dependency graph → the PRIMARY mechanism (handles 99% of cases)             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot Startup Phases — When Each Type of Bean Logic Runs:                 │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │  Phase 1: BeanDefinition Registration                                   │    │
│  │  → @ComponentScan finds classes                                        │    │
│  │  → @Configuration @Bean methods are parsed                             │    │
│  │  → AutoConfiguration classes are loaded and filtered                   │    │
│  │  → No beans created yet — only metadata                               │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │  Phase 2: BeanFactoryPostProcessor                                      │    │
│  │  → PropertySourcesPlaceholderConfigurer resolves ${...} placeholders  │    │
│  │  → ConfigurationClassPostProcessor processes @Configuration classes    │    │
│  │  → BeanDefinitions may be modified                                     │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │  Phase 3: Singleton Bean Creation (topological order)                   │    │
│  │  → Constructor called                                                   │    │
│  │  → Dependencies injected                                                │    │
│  │  → BeanPostProcessor.postProcessBeforeInitialization()                 │    │
│  │  → @PostConstruct                                                       │    │
│  │  → InitializingBean.afterPropertiesSet()                                │    │
│  │  → BeanPostProcessor.postProcessAfterInitialization() (AOP proxies)    │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │  Phase 4: After All Singletons Created                                  │    │
│  │  → SmartInitializingSingleton.afterSingletonsInstantiated()            │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │  Phase 5: Application Ready                                             │    │
│  │  → ApplicationRunner.run()                                              │    │
│  │  → CommandLineRunner.run()                                              │    │
│  │  → ApplicationReadyEvent fired                                          │    │
│  │  → Embedded server starts accepting requests                            │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │  Phase 6: Runtime                                                       │    │
│  │  → Lazy beans created on first access                                  │    │
│  │  → Prototype beans created on each getBean()                           │    │
│  │  → Request/Session beans created per HTTP request/session              │    │
│  ├─────────────────────────────────────────────────────────────────────────┤    │
│  │  Phase 7: Shutdown                                                      │    │
│  │  → @PreDestroy on singleton beans                                      │    │
│  │  → DisposableBean.destroy()                                             │    │
│  │  → Custom destroyMethod                                                 │    │
│  │  → ApplicationContext closed                                            │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 8. What Is the Lifecycle of a Bean?

Every Spring bean goes through a **well-defined lifecycle** from the moment the application starts to the moment it shuts down. Understanding this lifecycle is essential for knowing **when** your code runs, **what** Spring does behind the scenes at each step, and **where** to hook in custom initialization or cleanup logic.

---

#### 8.1 The Complete Bean Lifecycle — Overview Diagram

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│  ╔══════════════════════════════════════════════════════════════════════════════╗ │
│  ║  SPRING BEAN LIFECYCLE — FROM BIRTH TO DEATH                                ║ │
│  ╚══════════════════════════════════════════════════════════════════════════════╝ │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 1: APPLICATION START                          │                       │
│  │  SpringApplication.run(MyApp.class, args)            │                       │
│  │  → JVM starts                                        │                       │
│  │  → Spring Boot bootstrap begins                      │                       │
│  └────────────────────────┬─────────────────────────────┘                       │
│                           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 2: IoC CONTAINER STARTED                      │                       │
│  │  (ApplicationContext created, Configuration loaded)  │                       │
│  │  → @ComponentScan scans classpath                    │                       │
│  │  → @Configuration classes processed                  │                       │
│  │  → AutoConfiguration evaluated                       │                       │
│  │  → All BeanDefinitions registered (metadata only)    │                       │
│  │  → BeanFactoryPostProcessors run                     │                       │
│  │  → ${...} placeholders resolved                      │                       │
│  │  → NO beans instantiated yet — only definitions      │                       │
│  └────────────────────────┬─────────────────────────────┘                       │
│                           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 3: CONSTRUCT BEAN                             │                       │
│  │  → BeanFactory reads BeanDefinition                  │                       │
│  │  → Calls constructor (or factory method)             │                       │
│  │  → new UserService(...)                              │                       │
│  │  → Object exists in memory but NOT fully wired      │                       │
│  └────────────────────────┬─────────────────────────────┘                       │
│                           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 4: INJECT DEPENDENCIES                        │                       │
│  │  → Constructor args (already injected in Phase 3)    │                       │
│  │  → @Autowired field injection: field.set(bean, dep)  │                       │
│  │  → @Autowired setter injection: bean.setXxx(dep)     │                       │
│  │  → @Value properties resolved and injected           │                       │
│  │  → Bean is now FULLY WIRED with all dependencies     │                       │
│  └────────────────────────┬─────────────────────────────┘                       │
│                           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 4.5: BeanPostProcessor (BEFORE init)          │                       │
│  │  → postProcessBeforeInitialization(bean, beanName)   │                       │
│  │  → @Autowired processing completes here              │                       │
│  │  → @Value injection finalised here                   │                       │
│  │  → Aware interfaces called:                          │                       │
│  │    BeanNameAware.setBeanName()                       │                       │
│  │    BeanFactoryAware.setBeanFactory()                 │                       │
│  │    ApplicationContextAware.setApplicationContext()    │                       │
│  └────────────────────────┬─────────────────────────────┘                       │
│                           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 5: @PostConstruct                             │                       │
│  │  → Methods annotated with @PostConstruct execute     │                       │
│  │  → Then: InitializingBean.afterPropertiesSet()       │                       │
│  │  → Then: custom initMethod (from @Bean annotation)   │                       │
│  │  → Use this for: validation, cache warming,          │                       │
│  │    resource loading, connection verification          │                       │
│  └────────────────────────┬─────────────────────────────┘                       │
│                           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 5.5: BeanPostProcessor (AFTER init)           │                       │
│  │  → postProcessAfterInitialization(bean, beanName)    │                       │
│  │  → AOP PROXIES created HERE                          │                       │
│  │    (@Transactional, @Cacheable, @Async proxies)      │                       │
│  │  → The bean reference in the container may now be a  │                       │
│  │    PROXY wrapping the original object                │                       │
│  └────────────────────────┬─────────────────────────────┘                       │
│                           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 6: BEAN IS READY — USE THE BEAN               │                       │
│  │  ═══════════════════════════════════════════════════  │                       │
│  │  → Bean is stored in the singleton cache             │                       │
│  │  → Available for injection into other beans          │                       │
│  │  → Handles HTTP requests, processes messages,        │                       │
│  │    executes business logic, etc.                     │                       │
│  │  → ... application runs for hours/days/months ...    │                       │
│  │  ═══════════════════════════════════════════════════  │                       │
│  └────────────────────────┬─────────────────────────────┘                       │
│                           ▼ (application shutdown signal)                        │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 7: @PreDestroy                                │                       │
│  │  → Triggered by: SIGTERM, ctx.close(), JVM shutdown  │                       │
│  │  → Methods annotated with @PreDestroy execute        │                       │
│  │  → Then: DisposableBean.destroy()                    │                       │
│  │  → Then: custom destroyMethod (from @Bean)           │                       │
│  │  → Use this for: close connections, flush caches,    │                       │
│  │    release resources, deregister from service mesh   │                       │
│  └────────────────────────┬─────────────────────────────┘                       │
│                           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  PHASE 8: BEAN DESTROYED                             │                       │
│  │  → Bean reference removed from singleton cache       │                       │
│  │  → Object becomes eligible for garbage collection    │                       │
│  │  → ApplicationContext is closed                      │                       │
│  │  → JVM shuts down                                    │                       │
│  └──────────────────────────────────────────────────────┘                       │
│                                                                                  │
│  NOTE: Prototype beans do NOT go through Phase 7 & 8.                           │
│  Spring does NOT track or destroy prototype beans.                               │
│  YOU are responsible for cleanup of prototype beans.                             │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8.2 Each Phase Explained — With Code

##### Phase 1: Application Start

```java
// ── This is where everything begins ──────────────────────────────────────────────
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        System.out.println("=== PHASE 1: Application Start ===");
        SpringApplication.run(MyApp.class, args);
        // SpringApplication.run() does ALL of the following:
        // 1. Creates the ApplicationContext
        // 2. Loads configuration
        // 3. Creates all beans
        // 4. Starts embedded server
        System.out.println("=== Application is READY ===");
    }
}
```

##### Phase 2: IoC Container Started (Configuration Loaded)

```java
// ── Spring creates the container and loads all bean definitions ──────────────────
// This phase happens INSIDE SpringApplication.run():

// Step 2a: Create ApplicationContext
// AnnotationConfigServletWebServerApplicationContext created

// Step 2b: @ComponentScan runs
// → Scans com.example.myapp.** for @Component classes
// → Creates BeanDefinition for each (metadata only, no instances yet)

// Step 2c: @Configuration classes processed
// → ConfigurationClassPostProcessor finds @Bean methods
// → Creates BeanDefinition for each @Bean method

// Step 2d: AutoConfiguration evaluated
// → Reads AutoConfiguration.imports
// → Evaluates @ConditionalOnClass, @ConditionalOnProperty, etc.
// → Creates BeanDefinitions for matching auto-configuration beans

// Step 2e: BeanFactoryPostProcessors run
// → PropertySourcesPlaceholderConfigurer resolves ${...}
// → BeanDefinitions may be modified

// At this point:
// ┌─────────────────────────────────────────────────────────────────────────────┐
// │ BeanDefinitionRegistry is FULL of definitions                               │
// │ BUT no bean INSTANCES exist yet                                             │
// │ It's like having blueprints but no buildings                                │
// └─────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Phase 2 — What's in the Container at This Point:                                │
│                                                                                  │
│  BeanDefinitionRegistry (metadata only — no instances):                         │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │  "userController"   → BeanDef{class=UserController, scope=singleton}    │  │
│  │  "userService"      → BeanDef{class=UserService, scope=singleton}       │  │
│  │  "userRepository"   → BeanDef{class=UserRepository, scope=singleton}    │  │
│  │  "appConfig"        → BeanDef{class=AppConfig, scope=singleton}         │  │
│  │  "objectMapper"     → BeanDef{factory=appConfig.objectMapper()}         │  │
│  │  "dataSource"       → BeanDef{auto-config, conditional}                 │  │
│  │  ... (150+ more definitions)                                             │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Singleton Cache (EMPTY — no beans created yet):                                │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │  (empty)                                                                  │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Phase 3: Construct Bean

```java
// ── Spring calls the constructor to create the bean instance ─────────────────────
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // PHASE 3: Spring calls this constructor
    // Constructor parameters are resolved from the container
    // (dependencies must already be created or will be created first)
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        System.out.println("PHASE 3: UserService constructor called");
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        // At this point:
        // ✓ Object exists in memory
        // ✓ Constructor-injected dependencies are set
        // ✗ Field-injected dependencies are NOT yet set
        // ✗ @PostConstruct has NOT run yet
        // ✗ Bean is NOT yet in the singleton cache
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Phase 3 — How Spring Decides Constructor Arguments:                             │
│                                                                                  │
│  Spring reads the constructor signature:                                        │
│  UserService(UserRepository, PasswordEncoder)                                   │
│                                                                                  │
│  For each parameter:                                                             │
│  1. Look up bean of type UserRepository → found? use it                        │
│     → Not yet created? → CREATE IT FIRST (recursive)                           │
│  2. Look up bean of type PasswordEncoder → found? use it                       │
│     → Not yet created? → CREATE IT FIRST (recursive)                           │
│                                                                                  │
│  This is how the dependency graph drives creation order:                        │
│  DataSource created first → UserRepository → UserService                       │
│                                                                                  │
│  Single constructor: @Autowired is IMPLICIT (Spring Boot default)              │
│  Multiple constructors: one must be marked with @Autowired                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Phase 4: Inject Dependencies into the Constructed Bean

```java
// ── After construction, Spring injects remaining dependencies ────────────────────
@Service
public class NotificationService {

    // PHASE 3: Constructor injection (during construction)
    private final UserService userService;

    // PHASE 4: Field injection (AFTER construction)
    @Autowired
    private EmailClient emailClient;

    // PHASE 4: Setter injection (AFTER construction)
    private SmsClient smsClient;

    @Value("${notification.max-retries:3}")
    private int maxRetries;    // PHASE 4: @Value injection

    public NotificationService(UserService userService) {
        System.out.println("PHASE 3: Constructor — userService injected");
        this.userService = userService;
        // At this point: emailClient = null, smsClient = null, maxRetries = 0
    }

    @Autowired
    public void setSmsClient(SmsClient smsClient) {
        System.out.println("PHASE 4: Setter — smsClient injected");
        this.smsClient = smsClient;
    }

    // After Phase 4 completes:
    // ✓ userService = UserService@abc123      (from constructor)
    // ✓ emailClient = EmailClient@def456      (field injection)
    // ✓ smsClient   = SmsClient@ghi789        (setter injection)
    // ✓ maxRetries  = 3                        (@Value injection)
    // ALL dependencies are now available
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Phase 4 — Injection Order:                                                      │
│                                                                                  │
│  1. Constructor injection       ← happens in Phase 3 (during construction)     │
│  2. Field injection (@Autowired)← happens in Phase 4 (after construction)      │
│  3. Setter injection (@Autowired)← happens in Phase 4 (after construction)     │
│  4. @Value injection            ← happens in Phase 4 (after construction)      │
│                                                                                  │
│  IMPORTANT: In Phase 3 (constructor), field-injected values are still NULL!    │
│  Do NOT access @Autowired fields in the constructor — use @PostConstruct.      │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────┐         │
│  │  BAD — accessing field-injected dependency in constructor:       │         │
│  │                                                                    │         │
│  │  @Autowired                                                        │         │
│  │  private EmailClient emailClient;  // NULL during construction!   │         │
│  │                                                                    │         │
│  │  public NotificationService() {                                    │         │
│  │      emailClient.connect();  // ← NullPointerException!          │         │
│  │  }                                                                 │         │
│  │                                                                    │         │
│  │  GOOD — use @PostConstruct instead:                               │         │
│  │                                                                    │         │
│  │  @PostConstruct                                                    │         │
│  │  public void init() {                                              │         │
│  │      emailClient.connect();  // ← emailClient is set ✓           │         │
│  │  }                                                                 │         │
│  └────────────────────────────────────────────────────────────────────┘         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Phase 4.5: Aware Interfaces and BeanPostProcessor (Before Init)

```java
// ── Aware interfaces let beans access container infrastructure ───────────────────
@Component
public class AwareBean implements BeanNameAware,
                                  BeanFactoryAware,
                                  ApplicationContextAware {

    @Override
    public void setBeanName(String name) {
        System.out.println("PHASE 4.5a: BeanNameAware — my name is: " + name);
        // "awareBean"
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        System.out.println("PHASE 4.5b: BeanFactoryAware — factory: " + beanFactory);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        System.out.println("PHASE 4.5c: ApplicationContextAware — context: " + ctx);
        // Now you can programmatically access any bean:
        // ctx.getBean(SomeService.class)
    }
}

// ── BeanPostProcessor — runs for EVERY bean ──────────────────────────────────────
@Component
public class CustomBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof UserService) {
            System.out.println("PHASE 4.5: Before init — " + beanName);
        }
        return bean;    // return the bean (can return a modified/wrapped version)
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // PHASE 5.5 — see below
        if (bean instanceof UserService) {
            System.out.println("PHASE 5.5: After init — " + beanName);
        }
        return bean;    // AOP proxies are created here by Spring internally
    }
}
```

##### Phase 5: @PostConstruct

```java
// ── @PostConstruct — your custom initialization logic ────────────────────────────
@Service
public class CacheService {

    private final ProductRepository productRepository;
    private Map<Long, Product> cache;

    public CacheService(ProductRepository productRepository) {
        this.productRepository = productRepository;
        // PHASE 3: Constructor — productRepository is injected
        // But we should NOT load the cache here (if using field injection,
        // other fields might not be set yet)
    }

    @PostConstruct    // PHASE 5: Called AFTER all dependencies are injected
    public void warmUpCache() {
        System.out.println("PHASE 5: @PostConstruct — warming cache");
        // Safe to use ALL injected dependencies here
        List<Product> products = productRepository.findAll();
        this.cache = products.stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        System.out.println("Cache warmed with " + cache.size() + " products");
    }
}


// ── Multiple initialization mechanisms (execution order) ─────────────────────────
@Component
public class FullLifecycleBean implements InitializingBean {

    @PostConstruct
    public void postConstruct() {
        System.out.println("PHASE 5a: @PostConstruct");     // ← runs FIRST
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("PHASE 5b: afterPropertiesSet"); // ← runs SECOND
    }

    // If declared as @Bean(initMethod = "customInit"):
    public void customInit() {
        System.out.println("PHASE 5c: custom initMethod");  // ← runs THIRD
    }
}

// Output:
// PHASE 5a: @PostConstruct
// PHASE 5b: afterPropertiesSet
// PHASE 5c: custom initMethod
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Phase 5 — Initialization Callback Order:                                        │
│                                                                                  │
│  1. @PostConstruct              ← annotation-based (PREFERRED)                  │
│  2. InitializingBean.afterPropertiesSet()  ← interface-based (Spring-coupled)  │
│  3. @Bean(initMethod = "xxx")   ← XML/Java-config (third-party classes)        │
│                                                                                  │
│  All three run AFTER all dependencies are injected.                             │
│  All three run BEFORE the bean is put into the singleton cache.                 │
│                                                                                  │
│  Best Practice:                                                                  │
│  → Use @PostConstruct for your own classes (clean, standard Java annotation)   │
│  → Use initMethod for third-party classes registered via @Bean                  │
│  → Avoid InitializingBean (couples your code to Spring API)                    │
│                                                                                  │
│  Common @PostConstruct Use Cases:                                               │
│  → Validate configuration (@Value fields)                                      │
│  → Warm up caches                                                               │
│  → Establish connections (verify database, external API reachability)           │
│  → Register with external systems (service registry, metrics)                  │
│  → Load static data                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Phase 5.5: BeanPostProcessor (After Init) — AOP Proxies

```java
// ── This is where AOP proxies are created ────────────────────────────────────────
@Service
public class OrderService {

    @Transactional    // ← requires AOP proxy
    public void placeOrder(Order order) {
        // ...
    }
}

// What Spring does in Phase 5.5:
// 1. OrderService bean is fully initialized (Phase 5 complete)
// 2. BeanPostProcessor (postProcessAfterInitialization) runs
// 3. Spring sees @Transactional → creates a CGLIB proxy:
//
//    OrderService$$SpringCGLIB$$0 extends OrderService {
//        private OrderService target;        // the REAL bean
//        private TransactionManager txMgr;
//
//        @Override
//        public void placeOrder(Order order) {
//            txMgr.begin();                   // ← added by proxy
//            try {
//                target.placeOrder(order);    // ← calls REAL method
//                txMgr.commit();
//            } catch (Exception e) {
//                txMgr.rollback();
//                throw e;
//            }
//        }
//    }
//
// 4. The PROXY (not the original) is stored in the singleton cache
// 5. Other beans injecting OrderService get the PROXY

// This is why @Transactional doesn't work on internal calls:
// this.placeOrder()  → calls the REAL method (bypasses proxy)
// injectedService.placeOrder()  → calls the PROXY (transaction works)
```

##### Phase 6: Bean Is Ready — Use the Bean

```java
// ── The bean is now in the container and serving requests ────────────────────────
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // PHASE 6: These methods are called during the bean's active lifetime
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.findById(id);    // bean is fully operational
    }

    @PostMapping
    public UserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        return userService.create(request); // bean is fully operational
    }

    // This bean stays in the container for the entire lifetime of the application
    // Singleton scope: same instance handles ALL requests
    // The bean was created ONCE at startup and will be destroyed at shutdown
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Phase 6 — Bean in Active Use:                                                   │
│                                                                                  │
│  Singleton Cache:                                                                │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │  "userController"   → UserController@a1b2c3         (READY ✓)           │  │
│  │  "userService"      → UserService@d4e5f6            (READY ✓)           │  │
│  │  "userRepository"   → SimpleJpaRepository@g7h8i9    (READY ✓)           │  │
│  │  "orderService"     → OrderService$$CGLIB@proxy123  (READY ✓, proxied) │  │
│  │  "objectMapper"     → ObjectMapper@j0k1l2           (READY ✓)           │  │
│  │  "dataSource"       → HikariDataSource@m3n4o5       (READY ✓)           │  │
│  │  ...                                                                      │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  All beans are fully initialized, proxied if needed, and serving requests.      │
│  The application will stay in this phase until a shutdown signal is received.    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Phase 7: @PreDestroy

```java
// ── @PreDestroy — your custom cleanup logic ──────────────────────────────────────
@Service
public class ConnectionPoolService {

    private HikariDataSource dataSource;

    @PostConstruct
    public void init() {
        System.out.println("PHASE 5: Opening connection pool");
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        dataSource.setMaximumPoolSize(20);
    }

    @PreDestroy    // PHASE 7: Called during application shutdown
    public void cleanup() {
        System.out.println("PHASE 7: @PreDestroy — closing connection pool");
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();    // release all connections
        }
        System.out.println("Connection pool closed gracefully");
    }
}


// ── All three destruction mechanisms (execution order) ───────────────────────────
@Component
public class FullLifecycleBean implements DisposableBean {

    @PreDestroy
    public void preDestroy() {
        System.out.println("PHASE 7a: @PreDestroy");        // ← runs FIRST
    }

    @Override
    public void destroy() {
        System.out.println("PHASE 7b: DisposableBean.destroy()"); // ← SECOND
    }

    // If declared as @Bean(destroyMethod = "customCleanup"):
    public void customCleanup() {
        System.out.println("PHASE 7c: custom destroyMethod"); // ← runs THIRD
    }
}

// Output at shutdown:
// PHASE 7a: @PreDestroy
// PHASE 7b: DisposableBean.destroy()
// PHASE 7c: custom destroyMethod
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Phase 7 — Destruction Callback Order:                                           │
│                                                                                  │
│  1. @PreDestroy                 ← annotation-based (PREFERRED)                  │
│  2. DisposableBean.destroy()    ← interface-based (Spring-coupled)              │
│  3. @Bean(destroyMethod = "xxx")← XML/Java-config (third-party classes)        │
│                                                                                  │
│  Shutdown Triggers:                                                              │
│  → SIGTERM signal (kill <pid>, Ctrl+C)                                          │
│  → applicationContext.close()                                                   │
│  → JVM shutdown hook (registered automatically by Spring Boot)                  │
│                                                                                  │
│  Common @PreDestroy Use Cases:                                                  │
│  → Close database connections / connection pools                                │
│  → Flush and close caches                                                       │
│  → Deregister from service registry (Eureka, Consul)                           │
│  → Complete in-progress work / drain request queues                             │
│  → Close file handles, sockets, streams                                        │
│  → Save state to disk                                                           │
│                                                                                  │
│  IMPORTANT: Prototype beans do NOT receive @PreDestroy callbacks.              │
│  Spring does NOT track prototype beans after creation.                           │
│  You must manually clean up prototype beans.                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Phase 8: Bean Destroyed

```java
// ── After @PreDestroy, the bean is removed from the container ────────────────────
// Spring performs these steps:
// 1. Remove bean from the singleton cache (ConcurrentHashMap)
// 2. Clear the BeanDefinition from the registry
// 3. The Java object has no more references → eligible for GC
// 4. JVM garbage collector reclaims the memory

// You cannot interact with the bean after this point.
// The ApplicationContext is now closed.
```

---

#### 8.3 Complete Code Example — Observing the Full Lifecycle

```java
// ── A single bean that demonstrates EVERY lifecycle phase ────────────────────────
@Component
public class LifecycleDemoBean implements InitializingBean,
                                          DisposableBean,
                                          BeanNameAware,
                                          ApplicationContextAware {

    private String beanName;
    private ApplicationContext context;

    @Autowired
    private SomeDependency dependency;    // field injection

    // ── PHASE 3: Constructor ─────────────────────────────────────────────────
    public LifecycleDemoBean() {
        System.out.println("1. [CONSTRUCT]     Constructor called");
        System.out.println("   dependency = " + dependency);   // null!
    }

    // ── PHASE 4: Setter injection ────────────────────────────────────────────
    @Autowired
    public void setAnotherDep(AnotherDependency dep) {
        System.out.println("2. [INJECT]        Setter injection — dep: " + dep);
    }

    // ── PHASE 4.5: Aware interfaces ─────────────────────────────────────────
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        System.out.println("3. [AWARE]         BeanNameAware — name: " + name);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.context = ctx;
        System.out.println("4. [AWARE]         ApplicationContextAware");
    }

    // ── PHASE 5: Initialization ──────────────────────────────────────────────
    @PostConstruct
    public void postConstruct() {
        System.out.println("5. [POST-CONSTRUCT] @PostConstruct");
        System.out.println("   dependency = " + dependency);   // now it's set ✓
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("6. [INIT]          InitializingBean.afterPropertiesSet()");
    }

    // ── PHASE 6: Bean is in use ──────────────────────────────────────────────
    public void doWork() {
        System.out.println("7. [USE]           Bean is working...");
    }

    // ── PHASE 7: Destruction ─────────────────────────────────────────────────
    @PreDestroy
    public void preDestroy() {
        System.out.println("8. [PRE-DESTROY]   @PreDestroy");
    }

    @Override
    public void destroy() {
        System.out.println("9. [DESTROY]       DisposableBean.destroy()");
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Console Output — Full Lifecycle:                                                │
│                                                                                  │
│  === APPLICATION STARTING ===                                                   │
│                                                                                  │
│  1. [CONSTRUCT]     Constructor called                                          │
│     dependency = null                                                            │
│  2. [INJECT]        Setter injection — dep: AnotherDependency@abc123           │
│  3. [AWARE]         BeanNameAware — name: lifecycleDemoBean                    │
│  4. [AWARE]         ApplicationContextAware                                     │
│  5. [POST-CONSTRUCT] @PostConstruct                                              │
│     dependency = SomeDependency@def456                                          │
│  6. [INIT]          InitializingBean.afterPropertiesSet()                       │
│                                                                                  │
│  === APPLICATION READY ===                                                       │
│                                                                                  │
│  7. [USE]           Bean is working...                                          │
│  7. [USE]           Bean is working...                                          │
│  ... (application runs) ...                                                      │
│                                                                                  │
│  === APPLICATION SHUTTING DOWN ===                                               │
│                                                                                  │
│  8. [PRE-DESTROY]   @PreDestroy                                                  │
│  9. [DESTROY]       DisposableBean.destroy()                                    │
│                                                                                  │
│  === APPLICATION STOPPED ===                                                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8.4 Lifecycle Comparison — `@PostConstruct` vs Constructor vs `@EventListener`

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Where to Put Initialization Logic — Comparison:                                 │
│                                                                                  │
│  ┌──────────────────────┬──────────────────────────────────────────────────┐    │
│  │ Location              │ When It Runs              │ Use Case            │    │
│  ├──────────────────────┼──────────────────────────────────────────────────┤    │
│  │ Constructor           │ Phase 3 (earliest)       │ Assign final fields  │    │
│  │                       │ Field injection NOT done  │ Simple assignment    │    │
│  │                       │                           │ NO side effects      │    │
│  ├──────────────────────┼──────────────────────────────────────────────────┤    │
│  │ @PostConstruct        │ Phase 5 (after inject)   │ Validate config      │    │
│  │                       │ All dependencies ready    │ Warm caches          │    │
│  │                       │ Other beans may NOT be    │ Open connections     │    │
│  │                       │ fully ready yet           │ Single-bean init     │    │
│  ├──────────────────────┼──────────────────────────────────────────────────┤    │
│  │ SmartInitializing-    │ After ALL singletons     │ Cross-bean checks    │    │
│  │ Singleton             │ created (Phase 4)        │ System-wide init     │    │
│  ├──────────────────────┼──────────────────────────────────────────────────┤    │
│  │ ApplicationRunner /   │ After context ready      │ Run startup tasks    │    │
│  │ CommandLineRunner     │ (Phase 5)                │ Data migration       │    │
│  │                       │ Server about to start    │ Batch imports        │    │
│  ├──────────────────────┼──────────────────────────────────────────────────┤    │
│  │ @EventListener        │ After full startup       │ Send notifications   │    │
│  │ (ApplicationReady-    │ (Phase 5)                │ Start schedulers     │    │
│  │  Event)               │ Server IS accepting      │ Register services    │    │
│  │                       │ requests                 │                       │    │
│  └──────────────────────┴──────────────────────────────────────────────────┘    │
│                                                                                  │
│  Rule of thumb:                                                                  │
│  → Constructor: only set fields, nothing else                                   │
│  → @PostConstruct: init THIS bean (validate, connect, warm)                    │
│  → ApplicationRunner: init the APPLICATION (migrate data, seed DB)             │
│  → @EventListener(ApplicationReadyEvent): react to full startup                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8.5 Lifecycle for Different Bean Scopes

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Lifecycle Differences by Scope:                                                 │
│                                                                                  │
│  ┌───────────────────┬───────────────────┬───────────────────────────────────┐  │
│  │ Scope             │ Lifecycle Phases  │ Notes                             │  │
│  ├───────────────────┼───────────────────┼───────────────────────────────────┤  │
│  │ Singleton         │ ALL phases 1-8    │ Full lifecycle management.        │  │
│  │ (default)         │                   │ Created once, destroyed once.     │  │
│  │                   │                   │ @PreDestroy IS called.            │  │
│  ├───────────────────┼───────────────────┼───────────────────────────────────┤  │
│  │ Prototype         │ Phases 1-6 ONLY   │ @PreDestroy is NOT called.       │  │
│  │                   │ NO Phase 7-8      │ Spring creates but does NOT      │  │
│  │                   │                   │ destroy. YOU must clean up.       │  │
│  │                   │                   │ New instance every getBean().     │  │
│  ├───────────────────┼───────────────────┼───────────────────────────────────┤  │
│  │ Request           │ ALL phases        │ Created per HTTP request.         │  │
│  │                   │ per request       │ Destroyed when request ends.      │  │
│  │                   │                   │ @PreDestroy IS called.            │  │
│  ├───────────────────┼───────────────────┼───────────────────────────────────┤  │
│  │ Session           │ ALL phases        │ Created per HTTP session.         │  │
│  │                   │ per session       │ Destroyed when session expires.   │  │
│  │                   │                   │ @PreDestroy IS called.            │  │
│  └───────────────────┴───────────────────┴───────────────────────────────────┘  │
│                                                                                  │
│  IMPORTANT — Prototype gotcha:                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐    │
│  │  @Component                                                              │    │
│  │  @Scope("prototype")                                                     │    │
│  │  public class TempWorker {                                               │    │
│  │      @PreDestroy                                                         │    │
│  │      public void cleanup() {                                             │    │
│  │          // THIS WILL NEVER BE CALLED BY SPRING!                        │    │
│  │          // You must call it manually:                                   │    │
│  │          // tempWorker.cleanup();                                        │    │
│  │      }                                                                   │    │
│  │  }                                                                       │    │
│  └──────────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8.6 Real-World Lifecycle Example — E-Commerce Application

```java
// ── A realistic example showing lifecycle in an e-commerce service ────────────────
@Service
public class PaymentGatewayService {

    private final PaymentConfig config;
    private StripeClient stripeClient;
    private boolean healthy = false;

    // ── PHASE 3: Constructor ─────────────────────────────────────────────────
    public PaymentGatewayService(PaymentConfig config) {
        this.config = config;
        // Only assign fields — do NOT connect to Stripe here
        // because config might not be fully processed
    }

    // ── PHASE 5: @PostConstruct — initialize Stripe connection ───────────────
    @PostConstruct
    public void init() {
        System.out.println("Initializing Stripe connection...");
        this.stripeClient = StripeClient.builder()
            .apiKey(config.getApiKey())        // safe: @Value is injected
            .timeout(config.getTimeout())
            .build();

        // Verify connection
        try {
            stripeClient.ping();
            this.healthy = true;
            System.out.println("Stripe connection verified ✓");
        } catch (Exception e) {
            System.err.println("WARNING: Stripe unreachable at startup");
            // Don't throw — allow app to start, retry on first request
        }
    }

    // ── PHASE 6: Bean in use — handles payment requests ──────────────────────
    @Transactional
    public PaymentResult charge(Order order) {
        if (!healthy) {
            throw new ServiceUnavailableException("Payment gateway not ready");
        }
        return stripeClient.createCharge(
            order.getAmount(),
            order.getCurrency(),
            order.getCustomerToken()
        );
    }

    // ── PHASE 7: @PreDestroy — graceful shutdown ─────────────────────────────
    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down payment gateway...");
        if (stripeClient != null) {
            stripeClient.close();           // close HTTP connections
        }
        System.out.println("Payment gateway shut down ✓");
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  PaymentGatewayService Lifecycle Timeline:                                       │
│                                                                                  │
│  ┌──────────┐   ┌──────────────────┐   ┌───────────────┐   ┌────────────┐     │
│  │ CONSTRUCT │──▶│ INJECT (config)  │──▶│ @PostConstruct│──▶│ READY      │     │
│  │ new()     │   │ PaymentConfig    │   │ connect to    │   │ handling   │     │
│  │ fields=null│   │ @Value resolved  │   │ Stripe API    │   │ payments   │     │
│  └──────────┘   └──────────────────┘   │ verify health │   │ ...        │     │
│                                         └───────────────┘   └─────┬──────┘     │
│                                                                    │ (shutdown) │
│                                         ┌───────────────┐   ┌─────▼──────┐     │
│                                         │ DESTROYED     │◀──│ @PreDestroy│     │
│                                         │ GC eligible   │   │ close()    │     │
│                                         │ memory freed  │   │ flush      │     │
│                                         └───────────────┘   └────────────┘     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 9. What Is Dependency Injection and Why Is It Used?

**Dependency Injection (DI)** is a design pattern where an object **receives** its dependencies from an external source (the IoC container) rather than **creating** them itself. Instead of a class saying "I will create what I need", it says "give me what I need" — and the container provides it.

DI is the **practical implementation** of the **Inversion of Control (IoC)** principle: the control of creating and wiring dependencies is **inverted** from the class itself to the framework.

---

#### 9.1 The Problem — Without Dependency Injection

```java
// ── Without DI: the class creates its own dependencies (TIGHT COUPLING) ──────────
public class OrderService {

    // OrderService CREATES its own dependencies
    private final OrderRepository orderRepository = new OrderRepositoryImpl();
    private final PaymentService paymentService = new StripePaymentService();
    private final EmailService emailService = new SmtpEmailService();
    private final InventoryService inventoryService = new InventoryServiceImpl();

    public OrderResponse placeOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.from(request));
        paymentService.charge(order.getTotal(), request.getPaymentToken());
        inventoryService.decrementStock(order.getItems());
        emailService.sendOrderConfirmation(order);
        return OrderResponse.from(order);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Problems With This Approach:                                                    │
│                                                                                  │
│  1. TIGHT COUPLING                                                               │
│     OrderService is HARDCODED to use StripePaymentService.                      │
│     To switch to PayPalPaymentService, you MUST modify OrderService source.     │
│     → Violates Open/Closed Principle                                            │
│                                                                                  │
│  2. IMPOSSIBLE TO UNIT TEST                                                      │
│     You can't replace StripePaymentService with a mock.                         │
│     Every test hits a REAL Stripe API, a REAL database, sends REAL emails.      │
│     → Tests are slow, flaky, and expensive                                      │
│                                                                                  │
│  3. HIDDEN DEPENDENCIES                                                          │
│     Looking at the constructor, you have NO IDEA what OrderService needs.       │
│     Dependencies are buried inside the class body.                              │
│     → Hard to understand what this class requires                               │
│                                                                                  │
│  4. NO REUSABILITY                                                               │
│     If OrderRepository needs a DataSource, and InventoryService ALSO needs      │
│     the same DataSource, each creates its OWN instance.                         │
│     → Duplicate resources, wasted connections                                   │
│                                                                                  │
│  5. RIGID CONFIGURATION                                                          │
│     Dev uses H2 database, staging uses MySQL, prod uses Aurora.                 │
│     But OrderRepositoryImpl is hardcoded. No way to switch per environment.     │
│     → Environment-specific logic scattered throughout the code                  │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐        │
│  │  OrderService ─── new ──→ OrderRepositoryImpl    (HARDCODED)       │        │
│  │       │                                                             │        │
│  │       ├── new ──→ StripePaymentService           (HARDCODED)       │        │
│  │       │                                                             │        │
│  │       ├── new ──→ SmtpEmailService               (HARDCODED)       │        │
│  │       │                                                             │        │
│  │       └── new ──→ InventoryServiceImpl           (HARDCODED)       │        │
│  │                                                                     │        │
│  │  Every arrow is a HARD dependency. Change = code modification.     │        │
│  └─────────────────────────────────────────────────────────────────────┘        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 9.2 The Solution — With Dependency Injection

```java
// ── With DI: the class RECEIVES its dependencies (LOOSE COUPLING) ────────────────
@Service
public class OrderService {

    // OrderService DECLARES what it needs — Spring PROVIDES it
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final InventoryService inventoryService;

    // Dependencies are INJECTED via constructor
    public OrderService(OrderRepository orderRepository,
                        PaymentService paymentService,
                        EmailService emailService,
                        InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
        this.inventoryService = inventoryService;
    }

    public OrderResponse placeOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.from(request));
        paymentService.charge(order.getTotal(), request.getPaymentToken());
        inventoryService.decrementStock(order.getItems());
        emailService.sendOrderConfirmation(order);
        return OrderResponse.from(order);
    }
}

// The INTERFACES:
public interface PaymentService {
    void charge(BigDecimal amount, String token);
}

public interface EmailService {
    void sendOrderConfirmation(Order order);
}

// The IMPLEMENTATIONS (registered as beans):
@Service
public class StripePaymentService implements PaymentService { ... }

@Service
public class SmtpEmailService implements EmailService { ... }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  With DI — What Changed:                                                         │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐        │
│  │                                                                     │        │
│  │  Spring IoC Container                                               │        │
│  │  ┌───────────────┐                                                  │        │
│  │  │  Creates and   │                                                  │        │
│  │  │  wires all     │                                                  │        │
│  │  │  dependencies  │                                                  │        │
│  │  └───────┬───────┘                                                  │        │
│  │          │                                                           │        │
│  │          │ injects                                                   │        │
│  │          ▼                                                           │        │
│  │  ┌───────────────┐         ┌───────────────────────────────┐       │        │
│  │  │ OrderService  │ ◄────── │ PaymentService (interface)    │       │        │
│  │  │ (depends on   │         │   → StripePaymentService      │       │        │
│  │  │  interfaces,  │         │   → OR PayPalPaymentService   │       │        │
│  │  │  NOT concrete │         │   → OR MockPaymentService     │       │        │
│  │  │  classes)     │         └───────────────────────────────┘       │        │
│  │  │               │                                                  │        │
│  │  │               │ ◄────── EmailService (interface)                │        │
│  │  │               │ ◄────── InventoryService (interface)            │        │
│  │  │               │ ◄────── OrderRepository (interface)             │        │
│  │  └───────────────┘                                                  │        │
│  │                                                                     │        │
│  │  OrderService depends on ABSTRACTIONS (interfaces).                │        │
│  │  The container decides WHICH implementation to inject.             │        │
│  │  → Switch implementations WITHOUT touching OrderService           │        │
│  │  → Use mocks in tests WITHOUT touching OrderService               │        │
│  └─────────────────────────────────────────────────────────────────────┘        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 9.3 Three Types of Dependency Injection in Spring

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  DI Types — Comparison:                                                          │
│                                                                                  │
│  ┌─────────────────────┬─────────────────────┬────────────────────────────────┐ │
│  │ Type                │ Recommendation       │ Key Characteristics            │ │
│  ├─────────────────────┼─────────────────────┼────────────────────────────────┤ │
│  │ Constructor         │ ✓ RECOMMENDED       │ Immutable (final fields)       │ │
│  │ Injection           │   (Spring default)   │ All deps required at creation  │ │
│  │                     │                      │ Fail-fast, easy to test        │ │
│  ├─────────────────────┼─────────────────────┼────────────────────────────────┤ │
│  │ Setter              │ ○ Use for OPTIONAL   │ Mutable (can change at runtime)│ │
│  │ Injection           │   dependencies       │ Bean created with defaults     │ │
│  │                     │                      │ then wired via setters         │ │
│  ├─────────────────────┼─────────────────────┼────────────────────────────────┤ │
│  │ Field               │ ✗ AVOID             │ Hides dependencies             │ │
│  │ Injection           │   (except in tests)  │ Can't make fields final        │ │
│  │                     │                      │ Requires reflection            │ │
│  └─────────────────────┴─────────────────────┴────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 1. Constructor Injection (Recommended)

```java
// ── Constructor Injection — the PREFERRED approach ───────────────────────────────
@Service
public class UserService {

    private final UserRepository userRepository;      // final ✓
    private final PasswordEncoder passwordEncoder;     // final ✓
    private final EmailService emailService;           // final ✓

    // Single constructor: @Autowired is IMPLICIT in Spring Boot
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public UserResponse createUser(CreateUserRequest request) {
        String encoded = passwordEncoder.encode(request.getPassword());
        User user = userRepository.save(new User(request.getEmail(), encoded));
        emailService.sendWelcome(user);
        return UserResponse.from(user);
    }
}

// Why constructor injection is best:
// ✓ Fields are final → immutable, thread-safe
// ✓ All dependencies are REQUIRED — can't create the object without them
// ✓ Fail-fast: missing dependency → startup error (not runtime NPE)
// ✓ Easy to test: just call new UserService(mockRepo, mockEncoder, mockEmail)
// ✓ No Spring annotation needed on the constructor (single constructor)
// ✓ Works WITHOUT Spring (pure Java)
```

##### 2. Setter Injection (Optional Dependencies)

```java
// ── Setter Injection — for OPTIONAL or reconfigurable dependencies ───────────────
@Service
public class NotificationService {

    private final UserRepository userRepository;   // REQUIRED (constructor)

    private EmailService emailService;              // OPTIONAL
    private SmsService smsService;                  // OPTIONAL

    // Required dependency via constructor
    public NotificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired(required = false)    // optional — won't fail if not available
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Autowired(required = false)    // optional — won't fail if not available
    public void setSmsService(SmsService smsService) {
        this.smsService = smsService;
    }

    public void notifyUser(Long userId, String message) {
        User user = userRepository.findById(userId).orElseThrow();

        if (emailService != null) {
            emailService.send(user.getEmail(), message);
        }
        if (smsService != null) {
            smsService.send(user.getPhone(), message);
        }
    }
}

// When to use setter injection:
// ✓ Dependency is truly optional (app works without it)
// ✓ Dependency can be swapped at runtime (rare)
// ✗ Do NOT use for required dependencies (constructor injection is better)
```

##### 3. Field Injection (Avoid in Production Code)

```java
// ── Field Injection — convenient but NOT recommended ─────────────────────────────
@Service
public class OrderService {

    @Autowired    // Spring injects via reflection
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EmailService emailService;

    public void placeOrder(OrderRequest request) { ... }
}

// Problems with field injection:
// ✗ Fields can't be final → mutable, not thread-safe guarantee
// ✗ Dependencies are HIDDEN — not visible from the constructor
// ✗ Can't create the object without Spring (reflection required)
// ✗ Hard to test: must use @InjectMocks or reflection
// ✗ Encourages adding too many dependencies (no "constructor too big" signal)
// ✗ NullPointerException in constructor if you access an injected field

// The ONLY acceptable use: test classes
@SpringBootTest
class OrderServiceTest {

    @Autowired    // acceptable in test classes
    private OrderService orderService;

    @MockBean    // acceptable in test classes
    private PaymentService paymentService;
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Constructor vs Setter vs Field Injection — Visual:                              │
│                                                                                  │
│  Constructor Injection:                                                          │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  new UserService(repoBean, encoderBean, emailBean)                      │   │
│  │       ↑              ↑             ↑              ↑                      │   │
│  │  Container provides ALL dependencies at construction time               │   │
│  │  Object is FULLY WIRED from the start                                   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Setter Injection:                                                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  new NotificationService(repoBean)   ← partial construction            │   │
│  │  service.setEmailService(emailBean)  ← wired AFTER construction        │   │
│  │  service.setSmsService(smsBean)      ← wired AFTER construction        │   │
│  │  Object is wired in STAGES                                              │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Field Injection:                                                                │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  new OrderService()                  ← no-arg constructor               │   │
│  │  field.set(service, repoBean)        ← REFLECTION (bypasses access)    │   │
│  │  field.set(service, paymentBean)     ← REFLECTION                      │   │
│  │  Object is wired via MAGIC (reflection)                                 │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 9.4 Why Is Dependency Injection Used? — Practical Examples

##### Example 1: Swapping Implementations Without Changing Code

```java
// ── Same service, different implementations per environment ──────────────────────
public interface StorageService {
    void store(String key, byte[] data);
    byte[] retrieve(String key);
}

@Service
@Profile("dev")    // used in development
public class LocalStorageService implements StorageService {
    public void store(String key, byte[] data) {
        Files.write(Path.of("/tmp/" + key), data);    // local filesystem
    }
    public byte[] retrieve(String key) {
        return Files.readAllBytes(Path.of("/tmp/" + key));
    }
}

@Service
@Profile("prod")    // used in production
public class S3StorageService implements StorageService {
    private final S3Client s3Client;
    public S3StorageService(S3Client s3Client) { this.s3Client = s3Client; }

    public void store(String key, byte[] data) {
        s3Client.putObject(PutObjectRequest.builder()
            .bucket("my-bucket").key(key).build(),
            RequestBody.fromBytes(data));
    }
    public byte[] retrieve(String key) {
        return s3Client.getObject(GetObjectRequest.builder()
            .bucket("my-bucket").key(key).build())
            .readAllBytes();
    }
}

// FileUploadService doesn't know or CARE which implementation it gets:
@Service
public class FileUploadService {
    private final StorageService storageService;    // interface!

    public FileUploadService(StorageService storageService) {
        this.storageService = storageService;
        // In dev: LocalStorageService injected
        // In prod: S3StorageService injected
        // FileUploadService code is IDENTICAL in both environments
    }
}
```

##### Example 2: Easy Unit Testing with Mocks

```java
// ── Without DI: IMPOSSIBLE to unit test ──────────────────────────────────────────
public class OrderService {
    private PaymentService paymentService = new StripePaymentService();
    // ↑ Every test calls REAL Stripe API → slow, costs money, flaky
}

// ── With DI: trivial to unit test ────────────────────────────────────────────────
@Service
public class OrderService {
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public OrderResult placeOrder(Order order) {
        PaymentResult result = paymentService.charge(order.getTotal(), order.getToken());
        if (result.isSuccess()) {
            return OrderResult.success(order);
        }
        return OrderResult.failed("Payment declined");
    }
}

// Test — pure Java, no Spring needed:
class OrderServiceTest {

    @Test
    void placeOrder_successfulPayment_returnsSuccess() {
        // Arrange: create a MOCK payment service
        PaymentService mockPayment = mock(PaymentService.class);
        when(mockPayment.charge(any(), any()))
            .thenReturn(PaymentResult.success());

        // Inject the MOCK via constructor — just like Spring would
        OrderService orderService = new OrderService(mockPayment);

        // Act
        OrderResult result = orderService.placeOrder(testOrder);

        // Assert
        assertTrue(result.isSuccess());
        verify(mockPayment).charge(testOrder.getTotal(), testOrder.getToken());
        // ✓ No real API calls
        // ✓ Fast (milliseconds)
        // ✓ Deterministic (no network issues)
        // ✓ Free (no Stripe charges)
    }

    @Test
    void placeOrder_failedPayment_returnsFailed() {
        PaymentService mockPayment = mock(PaymentService.class);
        when(mockPayment.charge(any(), any()))
            .thenReturn(PaymentResult.declined("Insufficient funds"));

        OrderService orderService = new OrderService(mockPayment);

        OrderResult result = orderService.placeOrder(testOrder);

        assertFalse(result.isSuccess());
        assertEquals("Payment declined", result.getMessage());
    }
}
```

##### Example 3: Shared Singleton Resources

```java
// ── Without DI: each class creates its own DataSource (BAD) ──────────────────────
public class UserRepository {
    private DataSource ds = new HikariDataSource();    // 10 connections
}
public class OrderRepository {
    private DataSource ds = new HikariDataSource();    // 10 more connections
}
public class ProductRepository {
    private DataSource ds = new HikariDataSource();    // 10 more connections
}
// 30 connections total! 3 separate pools! Wasted resources!

// ── With DI: Spring creates ONE DataSource and shares it ─────────────────────────
@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setMaximumPoolSize(20);    // ONE pool, 20 connections
        return ds;
    }
}

@Repository
public class UserRepository {
    public UserRepository(DataSource dataSource) { ... }
    // ← same DataSource instance
}

@Repository
public class OrderRepository {
    public OrderRepository(DataSource dataSource) { ... }
    // ← SAME DataSource instance (singleton)
}

@Repository
public class ProductRepository {
    public ProductRepository(DataSource dataSource) { ... }
    // ← SAME DataSource instance (singleton)
}
// 20 connections total, ONE shared pool. Efficient!
```

---

#### 9.5 Advantages of Dependency Injection

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Advantages of Dependency Injection:                                             │
│                                                                                  │
│  ┌───────────────────────────┬───────────────────────────────────────────────┐  │
│  │ Advantage                 │ Explanation                                   │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 1. Loose Coupling         │ Classes depend on INTERFACES, not concrete   │  │
│  │                           │ implementations. Swap implementations        │  │
│  │                           │ without modifying dependent code.            │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 2. Testability            │ Inject mocks/stubs via constructor.          │  │
│  │                           │ Unit tests are fast, deterministic,          │  │
│  │                           │ and don't need Spring context.               │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 3. Single Responsibility  │ A class focuses on its OWN logic,           │  │
│  │                           │ not on creating/configuring dependencies.    │  │
│  │                           │ Object creation is the CONTAINER's job.      │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 4. Reusability            │ Same bean (e.g., DataSource) is shared       │  │
│  │                           │ across multiple consumers. No duplication.   │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 5. Configuration          │ Change behaviour per environment             │  │
│  │    Flexibility            │ (dev/staging/prod) using @Profile, @Value,   │  │
│  │                           │ @ConditionalOnProperty — no code changes.    │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 6. Lifecycle Management   │ Spring handles creation, initialization,     │  │
│  │                           │ @PostConstruct, @PreDestroy automatically.   │  │
│  │                           │ No manual cleanup code needed.               │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 7. Explicit Dependencies  │ Constructor injection makes ALL dependencies │  │
│  │                           │ visible in the method signature.             │  │
│  │                           │ Easy to understand what a class needs.       │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 8. Open/Closed Principle  │ Add new implementations without modifying   │  │
│  │                           │ existing code. Just add a new @Service.     │  │
│  └───────────────────────────┴───────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Example: Open/Closed Principle in action ─────────────────────────────────────
// Original code:
public interface NotificationChannel {
    void send(String userId, String message);
}

@Service
public class EmailNotification implements NotificationChannel { ... }

@Service
public class SmsNotification implements NotificationChannel { ... }

@Service
public class NotificationService {
    private final List<NotificationChannel> channels;

    public NotificationService(List<NotificationChannel> channels) {
        this.channels = channels;    // [EmailNotification, SmsNotification]
    }

    public void notifyAll(String userId, String message) {
        channels.forEach(ch -> ch.send(userId, message));
    }
}

// Later: Add Slack notifications WITHOUT touching any existing code:
@Service
public class SlackNotification implements NotificationChannel {
    public void send(String userId, String message) {
        slackClient.postMessage(userId, message);
    }
}
// Spring auto-discovers SlackNotification and adds it to the list.
// NotificationService now sends to Email, SMS, AND Slack — zero code changes!
```

---

#### 9.6 Disadvantages and Limitations of Dependency Injection

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Disadvantages of Dependency Injection:                                           │
│                                                                                  │
│  ┌───────────────────────────┬───────────────────────────────────────────────┐  │
│  │ Disadvantage              │ Explanation                                   │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 1. Learning Curve         │ Understanding IoC, bean lifecycle, scopes,   │  │
│  │                           │ auto-configuration, proxies, and AOP takes  │  │
│  │                           │ significant time for beginners.              │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 2. Debugging Complexity   │ Errors like NoSuchBeanDefinitionException,   │  │
│  │                           │ circular dependencies, and proxy issues      │  │
│  │                           │ can be hard to diagnose. Stack traces are   │  │
│  │                           │ long and filled with Spring internals.       │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 3. Runtime Errors         │ Wiring errors are caught at STARTUP, not    │  │
│  │    (not compile-time)     │ at compile time. Typos in @Qualifier,       │  │
│  │                           │ missing beans, wrong types — all surface    │  │
│  │                           │ only when you run the application.           │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 4. Hidden Control Flow    │ You can't "Go to Definition" on a           │  │
│  │                           │ constructor call to see who creates          │  │
│  │                           │ the bean. The container does it invisibly.   │  │
│  │                           │ Code navigation is harder.                   │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 5. Startup Overhead       │ Classpath scanning, bean creation, proxy    │  │
│  │                           │ generation, and autowiring add startup      │  │
│  │                           │ time. A large app can take 10-30+ seconds.  │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 6. Over-Engineering       │ Simple applications (scripts, CLIs, small   │  │
│  │    Risk                   │ tools) don't need DI. Adding a full IoC     │  │
│  │                           │ container for 3 classes is overkill.        │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 7. Framework Dependency   │ Your code becomes dependent on Spring       │  │
│  │                           │ annotations (@Service, @Autowired, etc.).   │  │
│  │                           │ Migrating to another framework requires     │  │
│  │                           │ changing annotations everywhere.            │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ 8. Proxy Pitfalls         │ @Transactional, @Cacheable, @Async use     │  │
│  │                           │ proxies. Internal method calls (this.xxx()) │  │
│  │                           │ bypass the proxy → unexpected behaviour.   │  │
│  └───────────────────────────┴───────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Limitations of Dependency Injection:                                            │
│                                                                                  │
│  ┌───────────────────────────┬───────────────────────────────────────────────┐  │
│  │ Limitation                │ Details                                       │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ Cannot inject into        │ Objects created with `new` are NOT managed  │  │
│  │ non-managed objects       │ by Spring. DI only works for beans.          │  │
│  │                           │ → new MyObject() won't have @Autowired     │  │
│  │                           │   fields injected                            │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ Static methods/fields     │ Spring cannot inject into static fields.    │  │
│  │                           │ DI is instance-based, not class-based.       │  │
│  │                           │ → @Autowired static SomeService svc;       │  │
│  │                           │   will NOT work                              │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ Final classes (proxies)   │ CGLIB proxies extend the target class.      │  │
│  │                           │ If a class is `final`, proxy creation fails.│  │
│  │                           │ @Transactional on final class → error       │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ Circular dependencies    │ A → B → A is not allowed (constructor DI).  │  │
│  │                           │ Requires redesign or workarounds.            │  │
│  ├───────────────────────────┼───────────────────────────────────────────────┤  │
│  │ Private constructors      │ Spring needs access to the constructor.     │  │
│  │                           │ Private constructors prevent bean creation.  │  │
│  │                           │ (Except via @Bean factory methods.)          │  │
│  └───────────────────────────┴───────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Common DI pitfalls ───────────────────────────────────────────────────────────

// ✗ PITFALL 1: new SomeClass() — Spring can't inject into objects YOU create
public class OrderProcessor {
    public void process() {
        EmailSender sender = new EmailSender();    // NOT a Spring bean!
        sender.send();                              // @Autowired fields are NULL
    }
}

// ✗ PITFALL 2: @Autowired on static field — doesn't work
@Service
public class BadService {
    @Autowired
    private static UserRepository userRepository;    // ALWAYS null!
}

// ✗ PITFALL 3: @Transactional on internal call — proxy bypassed
@Service
public class OrderService {
    @Transactional
    public void placeOrder(Order order) { ... }

    public void processMultiple(List<Order> orders) {
        for (Order order : orders) {
            this.placeOrder(order);    // ← calls REAL method, NOT proxy
            // Transaction is NOT started! @Transactional is IGNORED.
        }
    }
}
```

---

#### 9.7 Does Spring Boot Resolve Dependencies at Runtime or Compile Time?

**Spring Boot resolves ALL dependencies at RUNTIME, not at compile time.**

The Java compiler knows **nothing** about Spring beans, IoC containers, or dependency injection. At compile time, the compiler only verifies basic Java rules (types exist, methods match signatures, etc.). The actual bean discovery, creation, wiring, and injection happen entirely at runtime when the application starts.

---

##### What Happens at Compile Time (javac)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Compile Time — What javac Does:                                                 │
│                                                                                  │
│  javac compiles .java files into .class files.                                  │
│  It ONLY checks:                                                                 │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✓ Syntax: does the code follow Java grammar?                          │   │
│  │  ✓ Types: does UserService exist as a class/interface?                 │   │
│  │  ✓ Method signatures: does findById(Long) exist on UserRepository?    │   │
│  │  ✓ Imports: are all referenced classes importable?                     │   │
│  │  ✓ Generics: do types match (List<String> vs List<Integer>)?          │   │
│  │  ✓ Access modifiers: is the constructor/method public/accessible?     │   │
│  │                                                                          │   │
│  │  ✗ Does NOT check: is there a @Service bean for UserService?           │   │
│  │  ✗ Does NOT check: is there a bean of type PasswordEncoder?            │   │
│  │  ✗ Does NOT check: are there circular dependencies?                    │   │
│  │  ✗ Does NOT check: is @Value("${api.key}") resolvable?                │   │
│  │  ✗ Does NOT check: does @ComponentScan cover this package?             │   │
│  │  ✗ Does NOT check: is there exactly ONE bean of this type?             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  After compilation:                                                              │
│  UserService.class, UserRepository.class, etc. are in target/classes/           │
│  Annotations like @Service, @Autowired are PRESERVED in bytecode                │
│  (retained via @Retention(RetentionPolicy.RUNTIME))                             │
│  But they are just metadata — no DI has happened yet.                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── This code COMPILES successfully, but FAILS at runtime ────────────────────────
@Service
public class OrderService {

    private final PaymentGateway paymentGateway;

    public OrderService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }
}

// PaymentGateway interface exists (compiles ✓)
public interface PaymentGateway {
    PaymentResult charge(BigDecimal amount);
}

// BUT — no class implements PaymentGateway with @Component!
// javac: ✓ compilation successful
// Spring Boot startup: ✗ NoSuchBeanDefinitionException
// → No qualifying bean of type 'PaymentGateway' available
```

---

##### What Happens at Runtime (Spring Boot Startup)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Runtime — Spring Boot Dependency Resolution (Step by Step):                     │
│                                                                                  │
│  SpringApplication.run(MyApp.class, args)                                       │
│  │                                                                               │
│  │  STEP 1: CLASSPATH SCANNING (runtime)                                        │
│  │  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  │  ClassPathBeanDefinitionScanner walks every .class file             │    │
│  │  │  Uses Java REFLECTION to read annotations on each class:            │    │
│  │  │                                                                      │    │
│  │  │  Class<?> clazz = classLoader.loadClass("com.example.UserService"); │    │
│  │  │  if (clazz.isAnnotationPresent(Component.class)) {                  │    │
│  │  │      // register as bean definition                                  │    │
│  │  │  }                                                                   │    │
│  │  │                                                                      │    │
│  │  │  This is 100% RUNTIME — reflection reads bytecode metadata          │    │
│  │  └──────────────────────────────────────────────────────────────────────┘    │
│  │                                                                               │
│  │  STEP 2: BEAN DEFINITION REGISTRATION (runtime)                              │
│  │  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  │  For each discovered class:                                          │    │
│  │  │  → Create BeanDefinition (metadata: class, scope, dependencies)     │    │
│  │  │  → Register in BeanDefinitionRegistry (a HashMap)                   │    │
│  │  │                                                                      │    │
│  │  │  For @Bean methods in @Configuration classes:                        │    │
│  │  │  → Parse method signature using reflection                          │    │
│  │  │  → Create BeanDefinition with factoryMethod info                    │    │
│  │  │  → Register in BeanDefinitionRegistry                               │    │
│  │  └──────────────────────────────────────────────────────────────────────┘    │
│  │                                                                               │
│  │  STEP 3: DEPENDENCY RESOLUTION (runtime)                                     │
│  │  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  │  For each bean to create:                                            │    │
│  │  │  → Read constructor parameters using reflection:                    │    │
│  │  │    Constructor<?>[] ctors = clazz.getDeclaredConstructors();        │    │
│  │  │    Parameter[] params = ctor.getParameters();                       │    │
│  │  │                                                                      │    │
│  │  │  → For each parameter, find a matching bean:                        │    │
│  │  │    Type needed: UserRepository                                      │    │
│  │  │    Search BeanDefinitionRegistry for type match                     │    │
│  │  │    → Found 0 → NoSuchBeanDefinitionException                      │    │
│  │  │    → Found 1 → use it ✓                                           │    │
│  │  │    → Found 2+ → check @Primary, @Qualifier, parameter name        │    │
│  │  │      → Still ambiguous → NoUniqueBeanDefinitionException           │    │
│  │  └──────────────────────────────────────────────────────────────────────┘    │
│  │                                                                               │
│  │  STEP 4: BEAN INSTANTIATION (runtime)                                        │
│  │  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  │  → Resolve all dependencies (recursive — create dependencies first) │    │
│  │  │  → Call constructor via reflection:                                  │    │
│  │  │    ctor.newInstance(resolvedDep1, resolvedDep2, ...)                │    │
│  │  │  → Inject @Autowired fields via reflection:                         │    │
│  │  │    field.setAccessible(true);                                       │    │
│  │  │    field.set(bean, dependency);                                     │    │
│  │  │  → Run @PostConstruct                                               │    │
│  │  │  → Create AOP proxies if needed                                     │    │
│  │  │  → Store in singleton cache                                         │    │
│  │  └──────────────────────────────────────────────────────────────────────┘    │
│  │                                                                               │
│  └── APPLICATION READY (all DI is complete)                                     │
│                                                                                  │
│  EVERYTHING above happens at RUNTIME using Java Reflection API.                 │
│  The compiler played NO role in dependency resolution.                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### Why Runtime and Not Compile Time?

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why Spring Must Resolve at Runtime — Not Compile Time:                          │
│                                                                                  │
│  1. DYNAMIC DISCOVERY                                                            │
│     Beans are discovered by scanning the classpath at runtime.                  │
│     The compiler doesn't scan packages or read @ComponentScan.                  │
│     Third-party JARs on the classpath contribute auto-configured beans          │
│     that aren't known at compile time.                                          │
│                                                                                  │
│  2. CONDITIONAL BEANS                                                            │
│     @ConditionalOnProperty, @ConditionalOnClass, @Profile — these              │
│     determine WHETHER a bean is created based on runtime conditions             │
│     (environment variables, config files, classpath contents).                  │
│     The compiler can't evaluate these.                                          │
│                                                                                  │
│  3. EXTERNAL CONFIGURATION                                                       │
│     @Value("${db.url}") reads from application.properties at runtime.           │
│     The property value doesn't exist at compile time.                           │
│                                                                                  │
│  4. REFLECTION-BASED WIRING                                                      │
│     Spring uses the Reflection API to:                                          │
│     → Read annotations on classes, methods, fields, parameters                 │
│     → Call constructors                                                          │
│     → Set private fields (bypassing access control)                             │
│     → Create proxy subclasses (CGLIB)                                           │
│     All of this is a RUNTIME mechanism.                                         │
│                                                                                  │
│  5. INTERFACE-BASED INJECTION                                                    │
│     When you inject PaymentService (interface), the compiler can't know        │
│     which implementation (Stripe, PayPal, Mock) will be on the classpath.      │
│     Only at runtime does Spring scan and find the @Service class.              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### Compile Time vs Runtime — Complete Comparison

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Compile Time vs Runtime — What Gets Checked Where:                              │
│                                                                                  │
│  ┌────────────────────────────────┬──────────────┬───────────────────────────┐  │
│  │ Check                          │ Compile Time │ Runtime (Spring Startup)  │  │
│  ├────────────────────────────────┼──────────────┼───────────────────────────┤  │
│  │ Class exists?                  │ ✓            │ ✓                        │  │
│  │ Method signature match?        │ ✓            │ ✓                        │  │
│  │ Type compatibility?            │ ✓            │ ✓                        │  │
│  │ Bean exists in container?      │ ✗            │ ✓                        │  │
│  │ Only one bean of this type?    │ ✗            │ ✓                        │  │
│  │ Circular dependency?           │ ✗            │ ✓                        │  │
│  │ @Value property exists?        │ ✗            │ ✓                        │  │
│  │ @Profile active?               │ ✗            │ ✓                        │  │
│  │ @Conditional satisfied?        │ ✗            │ ✓                        │  │
│  │ AOP proxy creatable?           │ ✗            │ ✓                        │  │
│  │ @ComponentScan covers package? │ ✗            │ ✓                        │  │
│  │ Bean override conflict?        │ ✗            │ ✓                        │  │
│  └────────────────────────────────┴──────────────┴───────────────────────────┘  │
│                                                                                  │
│  Key takeaway: Java compiler ensures TYPE SAFETY.                               │
│  Spring ensures WIRING CORRECTNESS at startup (fail-fast).                      │
│  Neither catches wiring issues at compile time.                                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### Can We Get Compile-Time DI Checks?

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Alternatives with Compile-Time DI:                                              │
│                                                                                  │
│  While Spring is 100% runtime DI, some frameworks offer compile-time DI:        │
│                                                                                  │
│  ┌──────────────────────────┬────────────────────────────────────────────────┐  │
│  │ Framework                │ How DI Works                                   │  │
│  ├──────────────────────────┼────────────────────────────────────────────────┤  │
│  │ Spring (Spring Boot)     │ RUNTIME — reflection + classpath scanning     │  │
│  │                          │ Errors caught at startup, not compile time     │  │
│  ├──────────────────────────┼────────────────────────────────────────────────┤  │
│  │ Dagger 2 (Google)        │ COMPILE TIME — annotation processor (APT)    │  │
│  │                          │ Generates DI code during compilation           │  │
│  │                          │ Missing dependency = compilation error         │  │
│  ├──────────────────────────┼────────────────────────────────────────────────┤  │
│  │ Micronaut                │ COMPILE TIME — annotation processor (APT)    │  │
│  │                          │ No reflection at runtime, fast startup        │  │
│  ├──────────────────────────┼────────────────────────────────────────────────┤  │
│  │ Quarkus                  │ BUILD TIME — processes beans during build     │  │
│  │                          │ Minimal reflection at runtime                  │  │
│  └──────────────────────────┴────────────────────────────────────────────────┘  │
│                                                                                  │
│  Spring's approach:                                                              │
│  → More FLEXIBLE (conditional beans, profiles, classpath-based auto-config)    │
│  → More DYNAMIC (can add beans at runtime, hot reload in dev)                  │
│  → But SLOWER startup and NO compile-time safety for wiring                    │
│                                                                                  │
│  Spring AOT (Ahead of Time) in Spring Boot 3.x + GraalVM:                      │
│  → Moves SOME processing to build time (native compilation)                    │
│  → Generates reflection metadata ahead of time                                  │
│  → Still not full compile-time DI, but reduces runtime overhead                │
│  → Enables native images with ~50ms startup time                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Spring AOT (Spring Boot 3.x) — build-time optimisation ──────────────────────
// pom.xml:
// <plugin>
//     <groupId>org.graalvm.buildtools</groupId>
//     <artifactId>native-maven-plugin</artifactId>
// </plugin>

// Build native image:
// mvn -Pnative native:compile

// Result:
// Regular Spring Boot: 2-10 seconds startup, uses reflection
// Native image:        ~50ms startup, NO reflection at runtime
//                      BUT: reflection metadata generated at BUILD TIME
//                      Conditional beans evaluated at BUILD TIME
//                      Still not compile-time safety — build-time processing

// Spring AOT shifts WHEN the work happens:
// ┌─────────────────┬───────────────────┬──────────────────────┐
// │                  │ Traditional (JVM) │ AOT (Native Image)   │
// ├─────────────────┼───────────────────┼──────────────────────┤
// │ Classpath scan   │ Runtime           │ Build time           │
// │ Bean definitions │ Runtime           │ Build time           │
// │ Proxy generation │ Runtime (CGLIB)   │ Build time           │
// │ Bean creation    │ Runtime           │ Runtime (still)      │
// │ DI wiring        │ Runtime           │ Runtime (still)      │
// └─────────────────┴───────────────────┴──────────────────────┘
```

---

##### Summary — Runtime Resolution in Spring Boot

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  TL;DR — When Does Spring Resolve Dependencies?                                 │
│                                                                                  │
│  ┌────────────────────┬──────────────────────────────────────────────────────┐  │
│  │ Phase              │ What Happens                                         │  │
│  ├────────────────────┼──────────────────────────────────────────────────────┤  │
│  │ Compile time       │ javac checks Java syntax, types, method signatures  │  │
│  │ (mvn compile)      │ Annotations are preserved in .class files           │  │
│  │                    │ NO bean resolution, NO DI, NO Spring involvement    │  │
│  ├────────────────────┼──────────────────────────────────────────────────────┤  │
│  │ Runtime — startup  │ Spring scans classpath (reflection)                 │  │
│  │ (java -jar app.jar)│ Registers BeanDefinitions                           │  │
│  │                    │ Resolves dependency graph                            │  │
│  │                    │ Creates beans in dependency order                    │  │
│  │                    │ Injects dependencies via reflection                  │  │
│  │                    │ ALL wiring errors caught HERE (fail-fast)           │  │
│  ├────────────────────┼──────────────────────────────────────────────────────┤  │
│  │ Runtime — request  │ Lazy beans created on first access                  │  │
│  │ (during operation) │ Prototype beans created per getBean()               │  │
│  │                    │ Request/Session scoped beans per HTTP lifecycle     │  │
│  └────────────────────┴──────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Bottom line:                                                                    │
│  → The compiler ensures your JAVA CODE is correct                              │
│  → Spring ensures your WIRING is correct (at startup)                          │
│  → Missing beans, ambiguous types, circular deps → startup errors, not          │
│    compile errors                                                                │
│  → This is why good test coverage (especially @SpringBootTest) is critical     │
│    — it exercises the startup and catches wiring issues in CI/CD                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 10. @Autowired and Injection Types — Deep Dive

Section 9 introduced the three injection types at a high level. This section goes deeper into **@Autowired**, **constructor injection** (including multiple constructors), **setter injection**, and **field injection** — with detailed examples, advantages, disadvantages, limitations, and why constructor injection is the preferred approach.

---

#### 10.1 What Is @Autowired?

`@Autowired` is a Spring annotation that tells the IoC container: **"find a bean that matches this type and inject it here."** It can be placed on constructors, setter methods, fields, and even arbitrary methods.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Autowired — How It Works Internally:                                           │
│                                                                                  │
│  1. Spring sees @Autowired on a constructor / field / method                    │
│  2. It reads the TARGET TYPE (e.g., UserRepository)                             │
│  3. It searches the BeanDefinitionRegistry for a bean of that type              │
│  4. Resolution rules:                                                            │
│     ┌────────────────────────────────┬──────────────────────────────────────┐   │
│     │ Scenario                       │ Result                               │   │
│     ├────────────────────────────────┼──────────────────────────────────────┤   │
│     │ Exactly 1 bean of that type    │ ✓ Inject it                         │   │
│     │ 0 beans + required=true        │ ✗ NoSuchBeanDefinitionException     │   │
│     │ 0 beans + required=false       │ ✓ Inject null (no error)            │   │
│     │ 2+ beans, one has @Primary     │ ✓ Inject the @Primary bean          │   │
│     │ 2+ beans, @Qualifier specified │ ✓ Inject the qualified bean         │   │
│     │ 2+ beans, param name matches   │ ✓ Inject by bean name (fallback)   │   │
│     │ 2+ beans, no disambiguation    │ ✗ NoUniqueBeanDefinitionException   │   │
│     └────────────────────────────────┴──────────────────────────────────────┘   │
│                                                                                  │
│  5. Injection is performed via REFLECTION at runtime                            │
│  6. @Autowired is processed by AutowiredAnnotationBeanPostProcessor             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Where you can place @Autowired ───────────────────────────────────────────────

// 1. On a CONSTRUCTOR (implicit if single constructor in Spring Boot)
@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired    // optional for single constructor
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// 2. On a FIELD
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
}

// 3. On a SETTER method
@Service
public class NotificationService {
    private EmailService emailService;

    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
}

// 4. On an ARBITRARY method (less common)
@Service
public class ReportService {
    private DataSource dataSource;
    private ObjectMapper objectMapper;

    @Autowired
    public void configure(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }
}
```

##### @Autowired — `required` Attribute

```java
// ── required = true (DEFAULT) — throws exception if no bean found ────────────────
@Service
public class PaymentService {

    @Autowired    // required = true is the default
    private FraudDetectionService fraudService;
    // If no FraudDetectionService bean exists → NoSuchBeanDefinitionException
    // Application WILL NOT start
}

// ── required = false — injects null if no bean found ─────────────────────────────
@Service
public class PaymentService {

    @Autowired(required = false)
    private FraudDetectionService fraudService;    // may be null!

    public PaymentResult charge(Order order) {
        if (fraudService != null) {    // ← MUST null-check
            fraudService.checkFraud(order);
        }
        // proceed with payment...
    }
}

// ── Alternative: Optional<T> — cleaner than required = false ─────────────────────
@Service
public class PaymentService {
    private final Optional<FraudDetectionService> fraudService;

    public PaymentService(Optional<FraudDetectionService> fraudService) {
        this.fraudService = fraudService;
    }

    public PaymentResult charge(Order order) {
        fraudService.ifPresent(fs -> fs.checkFraud(order));
        // No null checks needed, cleaner API
    }
}
```

##### @Autowired with @Qualifier and @Primary

```java
// ── Problem: two beans of the same type ──────────────────────────────────────────
public interface NotificationSender {
    void send(String to, String message);
}

@Service
public class EmailSender implements NotificationSender {
    public void send(String to, String message) { /* send email */ }
}

@Service
public class SmsSender implements NotificationSender {
    public void send(String to, String message) { /* send SMS */ }
}

// ── Solution 1: @Primary — one bean is the DEFAULT ──────────────────────────────
@Service
@Primary    // ← this bean wins when no @Qualifier is specified
public class EmailSender implements NotificationSender { ... }

@Service
public class SmsSender implements NotificationSender { ... }

@Service
public class AlertService {
    private final NotificationSender sender;

    public AlertService(NotificationSender sender) {
        this.sender = sender;    // ← EmailSender injected (@Primary)
    }
}

// ── Solution 2: @Qualifier — explicitly name which bean ──────────────────────────
@Service
@Qualifier("email")
public class EmailSender implements NotificationSender { ... }

@Service
@Qualifier("sms")
public class SmsSender implements NotificationSender { ... }

@Service
public class AlertService {
    private final NotificationSender sender;

    public AlertService(@Qualifier("sms") NotificationSender sender) {
        this.sender = sender;    // ← SmsSender injected (by qualifier)
    }
}

// ── Solution 3: Parameter name matching (fallback) ───────────────────────────────
@Service("emailSender")
public class EmailSender implements NotificationSender { ... }

@Service("smsSender")
public class SmsSender implements NotificationSender { ... }

@Service
public class AlertService {
    public AlertService(NotificationSender smsSender) {
        // ← parameter name "smsSender" matches bean name → SmsSender injected
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Autowired — Advantages, Disadvantages, Limitations:                            │
│                                                                                  │
│  Advantages:                                                                     │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✓ Automatic wiring — no manual bean lookup or factory calls           │   │
│  │  ✓ Type-safe — Spring matches by type, not by string name             │   │
│  │  ✓ Works with @Qualifier and @Primary for disambiguation              │   │
│  │  ✓ Supports required=false and Optional<T> for optional deps          │   │
│  │  ✓ Can inject collections: List<T>, Set<T>, Map<String, T>           │   │
│  │  ✓ Implicit on single constructors (no annotation needed)             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Disadvantages:                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ Runtime resolution — wiring errors surface at startup, not compile  │   │
│  │  ✗ Ambiguity with multiple beans of same type if not qualified         │   │
│  │  ✗ @Autowired on fields hides dependencies (see Field Injection)      │   │
│  │  ✗ Couples code to Spring (@Autowired is a Spring annotation)         │   │
│  │  ✗ Can lead to circular dependency issues                              │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Limitations:                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ Cannot inject into objects created with `new` (not Spring-managed) │   │
│  │  ✗ Cannot inject into static fields                                    │   │
│  │  ✗ Cannot inject primitive types directly (use @Value instead)         │   │
│  │  ✗ Cannot inject into final fields via field injection                 │   │
│  │    (only constructor injection supports final fields)                   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 10.2 Constructor Injection — In Depth

Constructor injection is the **recommended** approach in Spring. Dependencies are provided as constructor parameters, and the IoC container calls the constructor with all resolved beans.

##### Single Constructor (Most Common)

```java
// ── Single constructor — @Autowired is IMPLICIT ──────────────────────────────────
@Service
public class OrderService {

    private final OrderRepository orderRepository;     // final ✓
    private final PaymentService paymentService;        // final ✓
    private final InventoryService inventoryService;    // final ✓

    // Spring Boot auto-detects the single constructor — no @Autowired needed
    public OrderService(OrderRepository orderRepository,
                        PaymentService paymentService,
                        InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }

    public OrderResult placeOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.from(request));
        paymentService.charge(order.getTotal(), request.getToken());
        inventoryService.reserve(order.getItems());
        return OrderResult.success(order);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Single Constructor — What Happens at Startup:                                   │
│                                                                                  │
│  1. Spring scans and finds @Service OrderService                                │
│  2. Reads the ONLY constructor:                                                  │
│     OrderService(OrderRepository, PaymentService, InventoryService)             │
│  3. For each parameter type:                                                     │
│     → OrderRepository: found JpaOrderRepository bean ✓                         │
│     → PaymentService: found StripePaymentService bean ✓                        │
│     → InventoryService: found InventoryServiceImpl bean ✓                      │
│  4. Calls: new OrderService(jpaOrderRepo, stripePay, inventorySvc)             │
│  5. All three fields are set to final — IMMUTABLE from this point              │
│                                                                                  │
│  If ANY dependency is missing → NoSuchBeanDefinitionException at STARTUP       │
│  The app NEVER starts in a broken state — this is FAIL-FAST behaviour          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Multiple Constructors

When a class has **more than one constructor**, Spring cannot automatically choose which one to use. You **must** annotate exactly one constructor with `@Autowired` to tell Spring which one to call for dependency injection.

```java
// ── Multiple constructors — @Autowired is REQUIRED on one ────────────────────────
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final EmailService emailService;
    private final CacheManager cacheManager;

    // Constructor 1: used for production (annotated with @Autowired)
    @Autowired    // ← REQUIRED when multiple constructors exist
    public ReportService(ReportRepository reportRepository,
                         EmailService emailService,
                         CacheManager cacheManager) {
        this.reportRepository = reportRepository;
        this.emailService = emailService;
        this.cacheManager = cacheManager;
    }

    // Constructor 2: used for testing or special scenarios (NOT used by Spring)
    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
        this.emailService = null;
        this.cacheManager = null;
    }

    // Constructor 3: another variant (NOT used by Spring)
    public ReportService(ReportRepository reportRepository,
                         EmailService emailService) {
        this.reportRepository = reportRepository;
        this.emailService = emailService;
        this.cacheManager = null;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Multiple Constructors — Rules:                                                  │
│                                                                                  │
│  ┌─────────────────────────────────┬─────────────────────────────────────────┐  │
│  │ Scenario                        │ Behaviour                               │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ 1 constructor, no @Autowired    │ ✓ Spring uses the single constructor   │  │
│  │                                 │   @Autowired is implicit                │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ 1 constructor, with @Autowired  │ ✓ Same as above (explicit, but         │  │
│  │                                 │   redundant)                            │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ 2+ constructors, one has        │ ✓ Spring uses the @Autowired one       │  │
│  │ @Autowired                      │                                         │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ 2+ constructors, none has       │ ✗ Spring uses the NO-ARG constructor   │  │
│  │ @Autowired                      │   if it exists. Otherwise:             │  │
│  │                                 │   BeanInstantiationException           │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ 2+ constructors, MULTIPLE have  │ ✗ BeanCreationException                │  │
│  │ @Autowired(required=true)       │   "Multiple @Autowired constructors"   │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ 2+ constructors, MULTIPLE have  │ ✓ Spring picks the constructor with    │  │
│  │ @Autowired(required=false)      │   the MOST parameters it can satisfy   │  │
│  └─────────────────────────────────┴─────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Multiple @Autowired(required=false) — greedy matching ────────────────────────
@Service
public class FlexibleService {

    private final UserRepository userRepository;
    private EmailService emailService;
    private SmsService smsService;

    // Constructor A: 3 params
    @Autowired(required = false)
    public FlexibleService(UserRepository userRepository,
                           EmailService emailService,
                           SmsService smsService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    // Constructor B: 2 params
    @Autowired(required = false)
    public FlexibleService(UserRepository userRepository,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // Constructor C: 1 param
    @Autowired(required = false)
    public FlexibleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// If all 3 beans exist → Spring uses Constructor A (most params satisfied)
// If SmsService is missing → Spring uses Constructor B
// If both EmailService & SmsService missing → Spring uses Constructor C
// NOTE: This pattern is RARE — prefer single @Autowired constructor instead
```

```java
// ── Common mistake: no @Autowired, no no-arg constructor ─────────────────────────
@Service
public class BrokenService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    // Constructor 1
    public BrokenService(UserRepository userRepository,
                         EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // Constructor 2
    public BrokenService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.emailService = null;
    }

    // ✗ 2 constructors, NEITHER has @Autowired, NO no-arg constructor
    // Spring throws: BeanInstantiationException
    // "No default constructor found"
    // FIX: add @Autowired to one constructor
}
```

##### Why Is Constructor Injection Called "Fail-Fast"?

"**Fail-fast**" means the application **refuses to start** if any dependency is missing, rather than starting and failing later at runtime with a `NullPointerException`.

```java
// ── FAIL-FAST: Constructor injection catches missing beans at STARTUP ────────────
@Service
public class PaymentService {

    private final PaymentGateway gateway;       // REQUIRED
    private final FraudChecker fraudChecker;     // REQUIRED
    private final AuditLogger auditLogger;       // REQUIRED

    public PaymentService(PaymentGateway gateway,
                          FraudChecker fraudChecker,
                          AuditLogger auditLogger) {
        this.gateway = gateway;
        this.fraudChecker = fraudChecker;
        this.auditLogger = auditLogger;
    }

    public void charge(Order order) {
        fraudChecker.check(order);         // ← guaranteed non-null
        gateway.charge(order.getTotal());  // ← guaranteed non-null
        auditLogger.log("charged", order); // ← guaranteed non-null
    }
}

// Scenario: FraudChecker bean is not registered
// ┌────────────────────────────────────────────────────────────────────────────┐
// │  APPLICATION STARTUP:                                                      │
// │                                                                            │
// │  ERROR: UnsatisfiedDependencyException                                     │
// │  → Bean 'paymentService' requires bean of type 'FraudChecker'             │
// │  → No qualifying bean of type 'FraudChecker' found                        │
// │                                                                            │
// │  APPLICATION FAILED TO START ← FAIL-FAST!                                  │
// │  The problem is caught IMMEDIATELY, not after 1000 successful payments.   │
// └────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── FAIL-SLOW: Field injection allows startup with potential null issues ─────────
@Service
public class PaymentService {

    @Autowired
    private PaymentGateway gateway;

    @Autowired(required = false)
    private FraudChecker fraudChecker;    // null if bean not found

    public void charge(Order order) {
        fraudChecker.check(order);    // ← NullPointerException at RUNTIME!
        // The app started fine, processed 1000 orders,
        // and crashed on order 1001 when this code path was hit.
        // This is FAIL-SLOW — the bug was hiding for weeks.
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Fail-Fast vs Fail-Slow — Timeline Comparison:                                   │
│                                                                                  │
│  Constructor Injection (FAIL-FAST):                                              │
│  ┌──────────┐     ┌──────────────────┐                                          │
│  │ Startup   │────▶│ MISSING BEAN?    │───YES──▶ ✗ APP DOES NOT START           │
│  │           │     │ Check ALL deps   │                                          │
│  │           │     │ at construction  │───NO───▶ ✓ ALL DEPS GUARANTEED          │
│  └──────────┘     └──────────────────┘           ▶ safe to use everywhere       │
│  Time: ──0ms─────────────────────────────────────                                │
│  Bug found: IMMEDIATELY                                                          │
│                                                                                  │
│  Field Injection (FAIL-SLOW):                                                    │
│  ┌──────────┐     ┌────────────┐     ┌────────────┐     ┌──────────────────┐   │
│  │ Startup   │────▶│ App starts │────▶│ Runs for   │────▶│ NPE at runtime!  │   │
│  │           │     │ "fine" ✓   │     │ days/weeks  │     │ field was null   │   │
│  └──────────┘     └────────────┘     └────────────┘     └──────────────────┘   │
│  Time: ──0ms──────────────────────── days later ────────────────                 │
│  Bug found: IN PRODUCTION, after damage is done                                  │
│                                                                                  │
│  Fail-fast is ALWAYS preferable:                                                 │
│  → Problems surface in DEV, not PROD                                            │
│  → CI/CD catches issues before deployment                                       │
│  → No partial startup with missing services                                     │
│  → Deterministic: either ALL deps exist or the app doesn't start               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Constructor Injection — Advantages, Disadvantages, Limitations

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Constructor Injection — Advantages:                                             │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✓ IMMUTABILITY — fields can be final, no accidental reassignment      │   │
│  │  ✓ FAIL-FAST — missing deps = startup error, never runtime NPE        │   │
│  │  ✓ MANDATORY DEPS — impossible to create object without dependencies   │   │
│  │  ✓ TESTABLE — just call new Service(mock1, mock2), no Spring needed   │   │
│  │  ✓ NO REFLECTION — constructor is called normally, no field.set()      │   │
│  │  ✓ THREAD-SAFE — final fields are safely published across threads      │   │
│  │  ✓ FRAMEWORK-AGNOSTIC — works in plain Java, no Spring annotations     │   │
│  │  ✓ SELF-DOCUMENTING — constructor shows ALL required dependencies      │   │
│  │  ✓ IMPLICIT @AUTOWIRED — no annotation needed for single constructor   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Constructor Injection — Disadvantages:                                           │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ TELESCOPING CONSTRUCTORS — many dependencies = long parameter list  │   │
│  │    (but this is a CODE SMELL: class has too many responsibilities)      │   │
│  │  ✗ CIRCULAR DEPENDENCIES — A(B) + B(A) = startup error                │   │
│  │    (constructor DI does not support circular deps at all)              │   │
│  │  ✗ OPTIONAL DEPS — all params are required; use @Nullable or          │   │
│  │    Optional<T> for optional deps, which adds noise                     │   │
│  │  ✗ MULTIPLE CONSTRUCTORS — must annotate one with @Autowired          │   │
│  │    (adds Spring coupling)                                               │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Constructor Injection — Limitations:                                            │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ Cannot resolve circular dependencies                                │   │
│  │    (requires @Lazy or redesign)                                         │   │
│  │  ✗ Cannot change dependencies after construction                       │   │
│  │    (but that's a feature, not a bug — immutability)                     │   │
│  │  ✗ With multiple same-type parameters, requires @Qualifier             │   │
│  │    on constructor params (verbose)                                      │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 10.3 Setter Injection — In Depth

Setter injection uses `@Autowired` on setter methods. Spring creates the bean with a no-arg or partial constructor, then calls the setter methods to inject dependencies.

```java
// ── Setter Injection — detailed example ──────────────────────────────────────────
@Service
public class ReportService {

    private ReportRepository reportRepository;    // NOT final (mutable)
    private EmailService emailService;             // NOT final
    private PdfGenerator pdfGenerator;             // NOT final

    // No-arg constructor (or Spring uses one with fewer params)
    public ReportService() {
        // Bean is created with NO dependencies set
        // At this point: reportRepository = null, emailService = null
    }

    @Autowired
    public void setReportRepository(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Autowired(required = false)    // truly optional dependency
    public void setPdfGenerator(PdfGenerator pdfGenerator) {
        this.pdfGenerator = pdfGenerator;
    }

    public Report generateReport(Long reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow();
        emailService.sendReport(report);

        if (pdfGenerator != null) {    // optional dep — null-check needed
            pdfGenerator.export(report);
        }
        return report;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Setter Injection — How Spring Wires It:                                         │
│                                                                                  │
│  Step 1: new ReportService()                                                     │
│          → object created, all fields are NULL                                  │
│                                                                                  │
│  Step 2: setReportRepository(reportRepoBean)                                    │
│          → reportRepository = reportRepoBean                                    │
│                                                                                  │
│  Step 3: setEmailService(emailServiceBean)                                      │
│          → emailService = emailServiceBean                                      │
│                                                                                  │
│  Step 4: setPdfGenerator(pdfGenBean)  — if bean exists                          │
│          → pdfGenerator = pdfGenBean  — otherwise skipped                       │
│                                                                                  │
│  ⚠ Between Step 1 and Step 4, the object exists in a PARTIALLY WIRED state.    │
│  If ANY code runs between construction and setter calls, fields may be null.    │
│  This is why setter injection is NOT fail-fast.                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Setter injection re-configuration (rare but possible) ────────────────────────
@Service
public class ConfigurableService {

    private DataFormatter formatter;

    @Autowired
    public void setFormatter(DataFormatter formatter) {
        this.formatter = formatter;
    }

    // Setter can be called again to swap implementation
    // (not common, but possible — this is a risk, not a feature)
    public void reconfigure(DataFormatter newFormatter) {
        this.formatter = newFormatter;    // ← mutable! changed at runtime
        // All subsequent calls use the new formatter
        // Other threads may see the old or new value (not thread-safe)
    }
}
```

##### Setter Injection — Advantages, Disadvantages, Limitations

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Setter Injection — Advantages:                                                  │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✓ OPTIONAL DEPS — naturally supports required=false with null         │   │
│  │  ✓ RECONFIGURABLE — can change dependency at runtime via setter        │   │
│  │  ✓ READABLE — setter method names describe the dependency              │   │
│  │  ✓ PARTIAL CONSTRUCTION — bean can be created without all deps        │   │
│  │  ✓ CIRCULAR DEPS — CAN resolve (Spring injects proxy/early ref)      │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Setter Injection — Disadvantages:                                               │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ NOT IMMUTABLE — fields can't be final (assigned after construction) │   │
│  │  ✗ NOT FAIL-FAST — bean created before deps are set; NPE possible     │   │
│  │  ✗ NOT THREAD-SAFE — mutable fields without synchronisation           │   │
│  │  ✗ HIDDEN OPTIONALITY — hard to tell which deps are truly required    │   │
│  │  ✗ VERBOSE — each dep needs a setter method (boilerplate)              │   │
│  │  ✗ PARTIALLY WIRED STATE — object exists with null fields briefly     │   │
│  │  ✗ HARDER TO TEST — must remember to call all setters in test setup   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Setter Injection — Limitations:                                                 │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ Cannot enforce mandatory deps at compile time                       │   │
│  │    (nothing stops you from forgetting to call a setter in tests)       │   │
│  │  ✗ Setters can be called by ANY code — not just Spring                │   │
│  │    (breaks encapsulation if setter is public)                          │   │
│  │  ✗ Order of setter calls is not guaranteed                             │   │
│  │    (don't depend on one setter running before another)                 │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 10.4 Field Injection — In Depth

Field injection uses `@Autowired` directly on a field. Spring injects the dependency using **Java Reflection** — it calls `field.setAccessible(true)` to bypass the `private` modifier, then directly sets the field value.

```java
// ── Field Injection — how it works under the hood ────────────────────────────────
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;    // private field — no setter!

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PriceCalculator priceCalculator;

    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow();
        Category category = categoryService.getCategory(product.getCategoryId());
        BigDecimal price = priceCalculator.calculate(product);
        return ProductResponse.of(product, category, price);
    }
}

// What Spring does internally (simplified):
// 1. ProductService bean = new ProductService();        ← no-arg constructor
// 2. Field repoField = ProductService.class.getDeclaredField("productRepository");
// 3. repoField.setAccessible(true);                     ← bypasses "private"
// 4. repoField.set(bean, productRepositoryBean);        ← injects via reflection
// 5. Repeat for categoryService, priceCalculator
```

```java
// ── Field injection makes testing HARD ───────────────────────────────────────────

// With field injection — how do you create this in a test?
@Service
public class ProductService {
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryService categoryService;
    @Autowired private PriceCalculator priceCalculator;
}

// Option A: Use Spring test context (SLOW — starts entire app)
@SpringBootTest
class ProductServiceTest {
    @Autowired private ProductService productService;
    @MockBean private ProductRepository productRepository;
    // Starts full Spring context just to test one class
}

// Option B: Use Mockito @InjectMocks (fragile)
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock private ProductRepository productRepository;
    @Mock private CategoryService categoryService;
    @Mock private PriceCalculator priceCalculator;
    @InjectMocks private ProductService productService;
    // @InjectMocks uses REFLECTION to inject — fragile, breaks on refactoring
    // If you add a new field, @InjectMocks silently leaves it null
}

// Option C: Use reflection manually (DON'T)
class ProductServiceTest {
    @Test
    void test() throws Exception {
        ProductService svc = new ProductService();
        Field field = ProductService.class.getDeclaredField("productRepository");
        field.setAccessible(true);
        field.set(svc, mockRepo);
        // This is awful. Don't do this.
    }
}

// COMPARE with constructor injection — trivial to test:
class ProductServiceTest {
    @Test
    void test() {
        ProductService svc = new ProductService(mockRepo, mockCategory, mockPrice);
        // Done. Pure Java. No reflection. No Spring. No annotations.
    }
}
```

##### Field Injection — Advantages, Disadvantages, Limitations

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Field Injection — Advantages:                                                   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✓ CONCISE — one line per dependency (@Autowired + field declaration)  │   │
│  │  ✓ NO BOILERPLATE — no constructor, no setters, no assignment code     │   │
│  │  ✓ QUICK PROTOTYPING — fast to write for small apps or POCs            │   │
│  │  ✓ CIRCULAR DEPS — Spring can resolve them (injects early references)  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Field Injection — Disadvantages:                                                │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ NOT IMMUTABLE — fields can't be final (reflection sets after init)  │   │
│  │  ✗ HIDDEN DEPS — looking at class from outside, you can't see deps    │   │
│  │  ✗ UNTESTABLE — no way to inject mocks without reflection/Spring      │   │
│  │  ✗ NOT FAIL-FAST — bean created first, fields set after; NPE risk     │   │
│  │  ✗ TIGHT SPRING COUPLING — class is useless without Spring DI         │   │
│  │  ✗ ENCOURAGES OVER-INJECTION — easy to add deps → God class risk     │   │
│  │    (constructor with 10 params is a visible alarm; 10 @Autowired is not)│   │
│  │  ✗ REFLECTION OVERHEAD — field.setAccessible(true) for every field    │   │
│  │  ✗ NPE IN CONSTRUCTOR — if constructor accesses @Autowired fields     │   │
│  │    they are STILL NULL during construction                             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Field Injection — Limitations:                                                  │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ Cannot make fields final → no compile-time immutability guarantee   │   │
│  │  ✗ Cannot instantiate without Spring → new MyService() leaves fields   │   │
│  │    null → NPE in any method                                            │   │
│  │  ✗ Cannot verify all deps are set at construction time                 │   │
│  │  ✗ Incompatible with GraalVM native image (strict reflection limits)  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 10.5 Complete Comparison — All Injection Types

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Constructor vs Setter vs Field Injection — Complete Comparison:                  │
│                                                                                  │
│  ┌────────────────────────┬──────────────┬──────────────┬──────────────────┐    │
│  │ Feature                │ Constructor  │ Setter       │ Field            │    │
│  ├────────────────────────┼──────────────┼──────────────┼──────────────────┤    │
│  │ Fields can be final    │ ✓ YES        │ ✗ NO         │ ✗ NO             │    │
│  │ Immutable object       │ ✓ YES        │ ✗ NO         │ ✗ NO             │    │
│  │ Thread-safe            │ ✓ YES        │ ✗ NO         │ ✗ NO             │    │
│  │ Fail-fast              │ ✓ YES        │ ✗ NO         │ ✗ NO             │    │
│  │ Testable (no Spring)   │ ✓ YES        │ ○ PARTIAL    │ ✗ NO             │    │
│  │ Optional dependencies  │ ○ via param  │ ✓ YES        │ ✓ YES            │    │
│  │ @Autowired required    │ ✗ NO (1 ctor)│ ✓ YES        │ ✓ YES            │    │
│  │ Circular dependency    │ ✗ NOT OK     │ ✓ OK         │ ✓ OK             │    │
│  │ Visible from outside   │ ✓ YES        │ ✓ YES        │ ✗ HIDDEN         │    │
│  │ Boilerplate            │ MODERATE     │ HIGH         │ LOW              │    │
│  │ Works without Spring   │ ✓ YES        │ ✓ YES        │ ✗ NO             │    │
│  │ Over-injection signal  │ ✓ YES        │ ✗ NO         │ ✗ NO             │    │
│  │ Spring recommendation  │ ✓ PREFERRED  │ ○ FOR OPTS   │ ✗ AVOID          │    │
│  │ NullPointerException   │ IMPOSSIBLE   │ POSSIBLE     │ POSSIBLE         │    │
│  │ risk at runtime        │ (deps set)   │ (mutable)    │ (reflection)     │    │
│  └────────────────────────┴──────────────┴──────────────┴──────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 10.6 Which Type of Injection Is Preferred and Why?

**Constructor injection is the preferred and recommended approach** by the Spring team, the Spring documentation, and the wider Java community.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring's Official Recommendation (from Spring Framework docs):                  │
│                                                                                  │
│  "The Spring team generally advocates constructor injection, as it lets          │
│   you implement application components as immutable objects and ensures          │
│   that required dependencies are not null."                                      │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │  Use CONSTRUCTOR injection for:                                           │  │
│  │  → All mandatory/required dependencies                                   │  │
│  │  → Services, repositories, controllers — anything that MUST have deps    │  │
│  │  → 95% of your injection needs                                           │  │
│  ├───────────────────────────────────────────────────────────────────────────┤  │
│  │  Use SETTER injection for:                                                │  │
│  │  → Truly optional dependencies (app works without them)                  │  │
│  │  → Reconfigurable dependencies (very rare)                               │  │
│  │  → Breaking circular dependencies (as a last resort)                     │  │
│  ├───────────────────────────────────────────────────────────────────────────┤  │
│  │  Use FIELD injection for:                                                 │  │
│  │  → Test classes only (@Autowired in @SpringBootTest)                     │  │
│  │  → Quick prototypes/POCs (not production code)                           │  │
│  │  → NEVER in production application classes                               │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 10.6.1 How Constructor Injection Maintains Immutable Fields

```java
// ── Constructor injection enables IMMUTABILITY via final fields ───────────────────

// With CONSTRUCTOR injection — fields are final (IMMUTABLE):
@Service
public class UserService {

    private final UserRepository userRepository;      // final — set ONCE
    private final PasswordEncoder passwordEncoder;     // final — set ONCE
    private final EmailService emailService;           // final — set ONCE

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;       // assigned in constructor
        this.passwordEncoder = passwordEncoder;     // assigned in constructor
        this.emailService = emailService;           // assigned in constructor
    }

    // After construction:
    // → userRepository can NEVER be reassigned
    // → passwordEncoder can NEVER be reassigned
    // → emailService can NEVER be reassigned
    // → The compiler ENFORCES this (final keyword)
    // → No thread-safety issues (final fields are safely published in JMM)
}

// With SETTER injection — fields CANNOT be final (MUTABLE):
@Service
public class UserService {

    private UserRepository userRepository;       // NOT final — can change!
    private PasswordEncoder passwordEncoder;     // NOT final — can change!

    @Autowired
    public void setUserRepository(UserRepository repo) {
        this.userRepository = repo;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder encoder) {
        this.passwordEncoder = encoder;
    }

    // PROBLEMS:
    // → Any code can call setUserRepository(null) → NPE later
    // → Fields can be reassigned accidentally
    // → Not thread-safe without synchronisation
    // → Compiler can't help you — no final keyword
}

// With FIELD injection — fields CANNOT be final:
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;       // NOT final — can't be!
    // "private final" + @Autowired = COMPILE ERROR
    // Because final fields MUST be set in the constructor,
    // but field injection sets them AFTER construction via reflection.

    // @Autowired
    // private final UserRepository userRepository;    ← WON'T COMPILE
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why Immutability Matters — Java Memory Model (JMM):                             │
│                                                                                  │
│  final fields have a SPECIAL guarantee in the JMM:                              │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  JMM §17.5: "An object is considered to be completely initialized      │   │
│  │  when its constructor finishes. A thread that can only see a           │   │
│  │  reference to an object after that object has been completely          │   │
│  │  initialized is guaranteed to see the correctly initialized values    │   │
│  │  for that object's final fields."                                      │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  In plain English:                                                               │
│  → final fields are SAFELY PUBLISHED across threads                             │
│  → No need for volatile, synchronized, or AtomicReference                       │
│  → Other threads are GUARANTEED to see the correct value                        │
│  → Non-final fields have NO such guarantee (can see stale/null values)          │
│                                                                                  │
│  This is why singletons (Spring beans) with final fields are naturally          │
│  thread-safe for their reference fields — without any extra synchronisation.    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 10.6.2 How Constructor Injection Helps for Unit Testing

```java
// ── Constructor injection makes unit testing TRIVIAL ─────────────────────────────

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                        PaymentService paymentService,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    public OrderResult placeOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.from(request));

        PaymentResult payment = paymentService.charge(
            order.getTotal(), request.getPaymentToken());

        if (!payment.isSuccess()) {
            return OrderResult.paymentFailed(payment.getReason());
        }

        notificationService.sendOrderConfirmation(order);
        return OrderResult.success(order);
    }
}
```

```java
// ── Unit test — pure Java, no Spring context, no reflection ──────────────────────
class OrderServiceTest {

    // Create mocks — just interfaces, no framework needed
    private OrderRepository mockOrderRepo = mock(OrderRepository.class);
    private PaymentService mockPaymentService = mock(PaymentService.class);
    private NotificationService mockNotificationService = mock(NotificationService.class);

    // Create the REAL service by calling the constructor — just like Spring would
    private OrderService orderService = new OrderService(
        mockOrderRepo,
        mockPaymentService,
        mockNotificationService
    );

    @Test
    void placeOrder_successfulPayment_sendsConfirmation() {
        // Arrange
        OrderRequest request = new OrderRequest("item-1", "tok_visa");
        Order savedOrder = new Order(1L, "item-1", BigDecimal.TEN);

        when(mockOrderRepo.save(any())).thenReturn(savedOrder);
        when(mockPaymentService.charge(any(), any()))
            .thenReturn(PaymentResult.success());

        // Act
        OrderResult result = orderService.placeOrder(request);

        // Assert
        assertTrue(result.isSuccess());
        verify(mockNotificationService).sendOrderConfirmation(savedOrder);
        verify(mockPaymentService).charge(BigDecimal.TEN, "tok_visa");
    }

    @Test
    void placeOrder_failedPayment_doesNotSendConfirmation() {
        // Arrange
        when(mockOrderRepo.save(any())).thenReturn(new Order());
        when(mockPaymentService.charge(any(), any()))
            .thenReturn(PaymentResult.declined("Insufficient funds"));

        // Act
        OrderResult result = orderService.placeOrder(new OrderRequest());

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Insufficient funds", result.getReason());
        verify(mockNotificationService, never()).sendOrderConfirmation(any());
        // ↑ Confirmed: NO notification was sent when payment failed
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why Constructor Injection Tests Are BETTER:                                     │
│                                                                                  │
│  ┌─────────────────────────────────┬─────────────────────────────────────────┐  │
│  │ With Constructor Injection      │ With Field Injection                    │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ new Service(mock1, mock2)       │ Needs @SpringBootTest or               │  │
│  │                                 │ @InjectMocks + reflection              │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ Runs in milliseconds            │ @SpringBootTest takes 2-10 seconds    │  │
│  │                                 │ to start application context           │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ No Spring dependency            │ Requires spring-boot-test,             │  │
│  │                                 │ spring-test on classpath               │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ Compiler verifies all deps      │ Forget a @Mock → silent null          │  │
│  │ (constructor params)            │ → NPE at test runtime                 │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ Refactoring-safe                │ Rename field → @InjectMocks breaks    │  │
│  │ (constructor params auto-update)│ (uses field name matching)             │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ Works in any test framework     │ Tied to Mockito/Spring                 │  │
│  │ (JUnit, TestNG, plain main())   │                                        │  │
│  ├─────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ 100 tests: ~200ms total        │ 100 tests: ~15-30 seconds             │  │
│  │                                 │ (context startup overhead)             │  │
│  └─────────────────────────────────┴─────────────────────────────────────────┘  │
│                                                                                  │
│  TL;DR: Constructor injection → new Service(mocks) → FAST, SIMPLE, RELIABLE   │
│         Field injection → @SpringBootTest → SLOW, COMPLEX, FRAGILE             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 10.7 Summary — When to Use What

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Decision Guide — Which Injection Type to Use:                                   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                          │   │
│  │  Is the dependency REQUIRED?                                             │   │
│  │  │                                                                       │   │
│  │  ├── YES → Use CONSTRUCTOR injection                                    │   │
│  │  │         → Fields are final, fail-fast, immutable, testable           │   │
│  │  │                                                                       │   │
│  │  └── NO  → Is it truly OPTIONAL?                                        │   │
│  │           │                                                              │   │
│  │           ├── YES → Use SETTER injection with required=false            │   │
│  │           │         OR constructor with Optional<T> parameter            │   │
│  │           │                                                              │   │
│  │           └── NO  → It's required. Use CONSTRUCTOR injection.           │   │
│  │                                                                          │   │
│  │  Is this a TEST CLASS?                                                   │   │
│  │  │                                                                       │   │
│  │  ├── YES → Field injection with @Autowired / @MockBean is acceptable   │   │
│  │  │                                                                       │   │
│  │  └── NO  → Use CONSTRUCTOR injection                                    │   │
│  │                                                                          │   │
│  │  ┌────────────────────────────────────────────────────────────────┐     │   │
│  │  │  GOLDEN RULE:                                                  │     │   │
│  │  │  Constructor injection for production code.                    │     │   │
│  │  │  Field injection only in test classes.                         │     │   │
│  │  │  Setter injection rarely (truly optional deps, circular deps). │     │   │
│  │  └────────────────────────────────────────────────────────────────┘     │   │
│  │                                                                          │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Quick Reference:                                                                │
│                                                                                  │
│  ┌───────────────────────┬───────────────────────────────────────────────────┐  │
│  │ Situation              │ Recommendation                                   │  │
│  ├───────────────────────┼───────────────────────────────────────────────────┤  │
│  │ @Service, @Repository │ Constructor injection (all deps required)        │  │
│  │ @Controller           │ Constructor injection                             │  │
│  │ Optional plugin/addon │ Setter injection with required=false             │  │
│  │ Circular dependency   │ Setter injection + @Lazy (redesign preferred)    │  │
│  │ @SpringBootTest       │ Field injection with @Autowired (acceptable)     │  │
│  │ Unit test (no Spring) │ Constructor — new Service(mock1, mock2)          │  │
│  │ @Configuration class  │ Constructor injection or @Bean method params     │  │
│  │ Multiple constructors │ @Autowired on one constructor                    │  │
│  │ Lombok project        │ @RequiredArgsConstructor (generates constructor)  │  │
│  └───────────────────────┴───────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Bonus: Lombok's @RequiredArgsConstructor — eliminate boilerplate ──────────────
@Service
@RequiredArgsConstructor    // Lombok generates constructor for all final fields
public class OrderService {

    private final OrderRepository orderRepository;       // final → included
    private final PaymentService paymentService;          // final → included
    private final NotificationService notificationService; // final → included

    // Lombok generates:
    // public OrderService(OrderRepository orderRepository,
    //                     PaymentService paymentService,
    //                     NotificationService notificationService) {
    //     this.orderRepository = orderRepository;
    //     this.paymentService = paymentService;
    //     this.notificationService = notificationService;
    // }

    // You get constructor injection's benefits WITHOUT writing the constructor!
    // ✓ Final fields ✓ Immutable ✓ Fail-fast ✓ Testable ✓ No boilerplate
}
```

---

### 11. Circular Dependency Injection Issue

A **circular dependency** occurs when two or more beans depend on each other, creating a cycle that Spring cannot resolve. Bean A needs Bean B to be created, but Bean B also needs Bean A to be created — a deadlock.

---

#### 11.1 What Is a Circular Dependency?

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Circular Dependency — The Problem:                                              │
│                                                                                  │
│  Simple Cycle (A ↔ B):                                                          │
│  ┌────────────────┐         ┌────────────────┐                                  │
│  │  OrderService   │────────▶│ PaymentService  │                                  │
│  │  needs          │         │ needs           │                                  │
│  │  PaymentService │◀────────│ OrderService    │                                  │
│  └────────────────┘         └────────────────┘                                  │
│                                                                                  │
│  Spring tries to create OrderService:                                            │
│  → Needs PaymentService → tries to create PaymentService                        │
│  → PaymentService needs OrderService → but OrderService isn't created yet!      │
│  → DEADLOCK — neither can be created first                                      │
│                                                                                  │
│  Three-Way Cycle (A → B → C → A):                                              │
│  ┌────────────────┐         ┌────────────────┐         ┌────────────────┐       │
│  │  OrderService   │────────▶│ PaymentService  │────────▶│ NotifyService   │       │
│  │  needs          │         │ needs           │         │ needs           │       │
│  │  PaymentService │         │ NotifyService   │         │ OrderService    │       │
│  └────────────────┘         └────────────────┘         └───────┬────────┘       │
│        ▲                                                        │                │
│        └────────────────────────────────────────────────────────┘                │
│                                                                                  │
│  Same problem — just a longer cycle. Spring detects it and refuses to start.    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 11.2 Circular Dependency with Constructor Injection — The Error

```java
// ── Circular dependency: OrderService ↔ PaymentService ───────────────────────────

@Service
public class OrderService {

    private final PaymentService paymentService;

    // OrderService NEEDS PaymentService at construction time
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public OrderResult placeOrder(Order order) {
        return paymentService.processPayment(order);
    }

    public Order getOrderForRefund(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow();
    }
}

@Service
public class PaymentService {

    private final OrderService orderService;

    // PaymentService NEEDS OrderService at construction time
    public PaymentService(OrderService orderService) {
        this.orderService = orderService;
    }

    public PaymentResult processPayment(Order order) {
        // process payment logic
        return PaymentResult.success();
    }

    public RefundResult refund(Long orderId) {
        Order order = orderService.getOrderForRefund(orderId);
        // refund logic
        return RefundResult.success(order);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens at Startup:                                                        │
│                                                                                  │
│  1. Spring starts creating OrderService                                          │
│  2. OrderService constructor needs PaymentService                                │
│  3. Spring starts creating PaymentService                                        │
│  4. PaymentService constructor needs OrderService                                │
│  5. OrderService is NOT created yet (still in step 1!)                           │
│  6. ✗ EXCEPTION:                                                                │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  BeanCurrentlyInCreationException:                                       │   │
│  │                                                                          │   │
│  │  Error creating bean with name 'orderService':                           │   │
│  │  Requested bean is currently in creation:                                │   │
│  │  Is there an unresolvable circular reference?                            │   │
│  │                                                                          │   │
│  │  The dependencies of some of the beans in the application context       │   │
│  │  form a cycle:                                                           │   │
│  │                                                                          │   │
│  │  ┌──────────────┐      ┌───────────────┐                                │   │
│  │  │ orderService  │─────▶│ paymentService │                                │   │
│  │  └──────────────┘      └───────┬───────┘                                │   │
│  │        ▲                        │                                        │   │
│  │        └────────────────────────┘                                        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  NOTE: Spring Boot 2.6+ DISABLES circular dependency resolution by default.     │
│  Earlier versions allowed circular deps with field/setter injection.            │
│  Spring Boot 2.6+ treats ALL circular deps as errors (even setter/field).       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 11.3 How to Resolve Circular Dependencies

There are multiple approaches, listed from **best practice** to **last resort**:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Resolution Strategies — Priority Order:                                         │
│                                                                                  │
│  ┌────┬──────────────────────────────┬───────────────────────────────────────┐  │
│  │ #  │ Strategy                      │ When to Use                          │  │
│  ├────┼──────────────────────────────┼───────────────────────────────────────┤  │
│  │ 1  │ REDESIGN (break the cycle)   │ ALWAYS try this first               │  │
│  │    │                              │ Extract shared logic to a new class  │  │
│  ├────┼──────────────────────────────┼───────────────────────────────────────┤  │
│  │ 2  │ Use events (decoupling)      │ When beans don't need sync calls    │  │
│  │    │                              │ ApplicationEventPublisher            │  │
│  ├────┼──────────────────────────────┼───────────────────────────────────────┤  │
│  │ 3  │ @Lazy on one dependency     │ Quick fix, acceptable in some cases  │  │
│  │    │                              │ Creates a proxy placeholder          │  │
│  ├────┼──────────────────────────────┼───────────────────────────────────────┤  │
│  │ 4  │ Setter/field injection       │ Last resort, breaks immutability    │  │
│  │    │                              │ Only with spring.main.allow-         │  │
│  │    │                              │ circular-references=true             │  │
│  ├────┼──────────────────────────────┼───────────────────────────────────────┤  │
│  │ 5  │ ObjectFactory / Provider     │ Deferred lookup, advanced use       │  │
│  └────┴──────────────────────────────┴───────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### Solution 1: Redesign — Extract Shared Logic (BEST)

The circular dependency is almost always a **design problem**. The two classes share responsibilities that should be separated into a third class.

```java
// ── BEFORE: OrderService ↔ PaymentService (circular) ─────────────────────────────
// OrderService needs PaymentService for processPayment()
// PaymentService needs OrderService for getOrderForRefund()
// This is a DESIGN SMELL — refund logic doesn't belong in PaymentService alone

// ── AFTER: Extract RefundService (no cycle) ──────────────────────────────────────

@Service
public class OrderService {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    public OrderService(PaymentService paymentService,
                        OrderRepository orderRepository) {
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
    }

    public OrderResult placeOrder(Order order) {
        return paymentService.processPayment(order);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow();
    }
}

@Service
public class PaymentService {

    // ← NO dependency on OrderService anymore!

    public PaymentResult processPayment(Order order) {
        // payment logic
        return PaymentResult.success();
    }

    public RefundResult processRefund(Order order) {
        // refund the payment
        return RefundResult.success(order);
    }
}

// NEW: RefundService owns the refund workflow
@Service
public class RefundService {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public RefundService(OrderService orderService,
                         PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    public RefundResult refund(Long orderId) {
        Order order = orderService.getOrderById(orderId);
        return paymentService.processRefund(order);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Before vs After — Dependency Graph:                                             │
│                                                                                  │
│  BEFORE (circular):                                                              │
│  ┌──────────────┐         ┌────────────────┐                                    │
│  │ OrderService  │────────▶│ PaymentService  │                                    │
│  │               │◀────────│                │                                    │
│  └──────────────┘         └────────────────┘                                    │
│  ✗ Cycle! Cannot create either bean first.                                      │
│                                                                                  │
│  AFTER (no cycle):                                                               │
│  ┌──────────────┐         ┌────────────────┐                                    │
│  │ OrderService  │────────▶│ PaymentService  │                                    │
│  └──────────────┘         └────────────────┘                                    │
│        ▲                          ▲                                               │
│        │                          │                                               │
│        └──────┐    ┌──────────────┘                                              │
│               │    │                                                              │
│         ┌─────┴────┴─────┐                                                       │
│         │ RefundService   │  ← NEW: depends on both, no cycle                   │
│         └────────────────┘                                                       │
│  ✓ Acyclic! Spring can create PaymentService → OrderService → RefundService     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### Solution 2: Use Application Events (Decoupling)

When one service needs to **notify** another (not get a return value), use events to decouple them.

```java
// ── BEFORE: OrderService calls PaymentService.onOrderPlaced() directly ───────────
// And PaymentService calls OrderService.updateStatus() → circular!

// ── AFTER: Use events — no direct dependency ─────────────────────────────────────

// Event class:
public class OrderPlacedEvent {
    private final Long orderId;
    private final BigDecimal amount;

    public OrderPlacedEvent(Long orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
    }
    // getters...
}

// OrderService publishes events — does NOT know about PaymentService
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    public OrderResult placeOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.from(request));

        // Publish event — OrderService doesn't know WHO handles it
        eventPublisher.publishEvent(
            new OrderPlacedEvent(order.getId(), order.getTotal()));

        return OrderResult.success(order);
    }
}

// PaymentService listens for events — does NOT know about OrderService
@Service
public class PaymentService {

    private final PaymentGateway paymentGateway;

    public PaymentService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        paymentGateway.charge(event.getAmount());
        // No reference to OrderService — cycle broken!
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Event-Based Decoupling — No Direct Dependencies:                                │
│                                                                                  │
│  ┌──────────────┐    publishes    ┌────────────────────┐    listens              │
│  │ OrderService  │───────────────▶│ OrderPlacedEvent    │◀──────────────┐        │
│  │               │                │ (plain Java object) │               │        │
│  └──────────────┘                └────────────────────┘    ┌──────────┴───┐    │
│                                                             │PaymentService│    │
│  Neither service knows about the other.                    └──────────────┘    │
│  They communicate through EVENTS managed by Spring.                             │
│  ✓ No cycle  ✓ Loose coupling  ✓ Easy to add more listeners                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### Solution 3: @Lazy — Create a Proxy Placeholder

`@Lazy` tells Spring: "don't create this bean now; create a **proxy** that will fetch the real bean on first use." This breaks the cycle because the constructor receives a proxy (not the real bean), so the real bean doesn't need to exist yet.

```java
// ── @Lazy on one side of the cycle — breaks the deadlock ─────────────────────────

@Service
public class OrderService {

    private final PaymentService paymentService;

    // @Lazy creates a proxy for PaymentService
    // The REAL PaymentService is NOT created during OrderService construction
    public OrderService(@Lazy PaymentService paymentService) {
        this.paymentService = paymentService;
        // paymentService is a PROXY here, not the real bean
        // The real bean is created when a METHOD is first called on it
    }

    public OrderResult placeOrder(Order order) {
        // First method call → proxy fetches the REAL PaymentService from container
        return paymentService.processPayment(order);
    }
}

@Service
public class PaymentService {

    private final OrderService orderService;

    // No @Lazy needed here — OrderService is already fully created
    // (its constructor already finished with the lazy proxy)
    public PaymentService(OrderService orderService) {
        this.orderService = orderService;
    }

    public RefundResult refund(Long orderId) {
        Order order = orderService.getOrderForRefund(orderId);
        return RefundResult.success(order);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How @Lazy Breaks the Cycle — Step by Step:                                      │
│                                                                                  │
│  1. Spring starts creating OrderService                                          │
│  2. Constructor needs PaymentService → sees @Lazy                               │
│  3. Spring creates a PROXY (lightweight placeholder) for PaymentService         │
│     ┌────────────────────────────────────────────────────────────┐              │
│     │  PaymentService$$LazyProxy                                 │              │
│     │  → Does NOT call PaymentService constructor yet            │              │
│     │  → Contains a reference to the BeanFactory                 │              │
│     │  → On first method call: fetches real bean from container  │              │
│     └────────────────────────────────────────────────────────────┘              │
│  4. OrderService constructor receives the proxy → completes ✓                   │
│  5. OrderService is now fully created and in the container                       │
│  6. Spring starts creating PaymentService (the REAL one)                        │
│  7. PaymentService needs OrderService → it's already created ✓                  │
│  8. PaymentService constructor completes ✓                                      │
│  9. Both beans are in the container — cycle resolved!                           │
│                                                                                  │
│  Timeline:                                                                       │
│  ──┬────────────────────────┬──────────────────────────┬──────                   │
│    │ OrderService created   │ PaymentService created   │ Done                    │
│    │ (with lazy proxy)      │ (with real OrderService) │                         │
│  ──┴────────────────────────┴──────────────────────────┴──────                   │
│                                                                                  │
│  ⚠ Caveat: @Lazy adds a proxy layer — minor performance overhead               │
│  ⚠ The real bean creation is deferred, not eliminated                           │
│  ⚠ If the real bean fails to create → error at FIRST USE, not startup          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### Solution 4: ObjectFactory / Provider (Deferred Lookup)

Instead of injecting the bean directly, inject a **factory** that retrieves the bean on demand.

```java
// ── ObjectFactory — deferred bean lookup ─────────────────────────────────────────

@Service
public class OrderService {

    private final ObjectFactory<PaymentService> paymentServiceFactory;

    // Inject ObjectFactory<PaymentService> instead of PaymentService directly
    public OrderService(ObjectFactory<PaymentService> paymentServiceFactory) {
        this.paymentServiceFactory = paymentServiceFactory;
        // PaymentService is NOT created yet — only a factory reference
    }

    public OrderResult placeOrder(Order order) {
        // .getObject() fetches the real bean from the container at call time
        PaymentService paymentService = paymentServiceFactory.getObject();
        return paymentService.processPayment(order);
    }
}

@Service
public class PaymentService {

    private final OrderService orderService;

    public PaymentService(OrderService orderService) {
        this.orderService = orderService;
    }

    public RefundResult refund(Long orderId) {
        Order order = orderService.getOrderForRefund(orderId);
        return RefundResult.success(order);
    }
}

// Alternative: use javax.inject.Provider (standard Java, not Spring-specific)
@Service
public class OrderService {

    private final Provider<PaymentService> paymentServiceProvider;

    public OrderService(Provider<PaymentService> paymentServiceProvider) {
        this.paymentServiceProvider = paymentServiceProvider;
    }

    public OrderResult placeOrder(Order order) {
        PaymentService paymentService = paymentServiceProvider.get();
        return paymentService.processPayment(order);
    }
}
```

---

##### Solution 5: Setter Injection + Allow Circular References (LAST RESORT)

```java
// ── Setter injection — allows Spring to create both beans partially ──────────────
// ⚠ Requires: spring.main.allow-circular-references=true (disabled by default)

@Service
public class OrderService {

    private PaymentService paymentService;    // NOT final — mutable

    @Autowired
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public OrderResult placeOrder(Order order) {
        return paymentService.processPayment(order);
    }
}

@Service
public class PaymentService {

    private OrderService orderService;    // NOT final — mutable

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    public RefundResult refund(Long orderId) {
        Order order = orderService.getOrderForRefund(orderId);
        return RefundResult.success(order);
    }
}
```

```yaml
# application.properties / application.yml — enable circular refs (NOT recommended)
spring:
  main:
    allow-circular-references: true
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How Setter Injection Resolves Circular Deps (when allowed):                     │
│                                                                                  │
│  1. Spring creates OrderService with no-arg constructor                         │
│     → OrderService exists but paymentService field is NULL                      │
│  2. Spring creates PaymentService with no-arg constructor                       │
│     → PaymentService exists but orderService field is NULL                      │
│  3. Spring calls OrderService.setPaymentService(paymentServiceBean)             │
│     → OrderService now has a reference to PaymentService                        │
│  4. Spring calls PaymentService.setOrderService(orderServiceBean)               │
│     → PaymentService now has a reference to OrderService                        │
│  5. Both beans are fully wired ✓                                                │
│                                                                                  │
│  This works because:                                                             │
│  → The OBJECTS exist before their dependencies are set                          │
│  → Spring can pass "early references" to partially-constructed beans            │
│                                                                                  │
│  Why this is BAD:                                                                │
│  ✗ Fields can't be final (breaks immutability)                                  │
│  ✗ Partially-wired state exists briefly (NPE risk)                              │
│  ✗ Hides a design problem (the cycle should be fixed, not worked around)        │
│  ✗ Disabled by default in Spring Boot 2.6+ for good reason                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 11.4 Circular Dependency — Summary

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Circular Dependency Resolution — Decision Guide:                                │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                          │   │
│  │  Do you have a circular dependency?                                      │   │
│  │  │                                                                       │   │
│  │  ├── Can you REDESIGN to eliminate the cycle?                           │   │
│  │  │   ├── YES → Extract shared logic into a new service (BEST) ✓        │   │
│  │  │   └── NO  ↓                                                          │   │
│  │  │                                                                       │   │
│  │  ├── Is the interaction one-way notification (fire-and-forget)?         │   │
│  │  │   ├── YES → Use ApplicationEventPublisher (GOOD) ✓                  │   │
│  │  │   └── NO  ↓                                                          │   │
│  │  │                                                                       │   │
│  │  ├── Do you need the return value from the other bean?                  │   │
│  │  │   ├── YES → Use @Lazy on one constructor parameter (ACCEPTABLE)     │   │
│  │  │   │         OR ObjectFactory<T> / Provider<T>                        │   │
│  │  │   └── NO  → Use events                                              │   │
│  │  │                                                                       │   │
│  │  └── Last resort: Setter injection + allow-circular-references=true    │   │
│  │      (NOT recommended — hides design problems)                          │   │
│  │                                                                          │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌───────────────────────┬──────────┬────────────┬────────────┬──────────────┐ │
│  │ Approach              │ Immutable│ Fail-Fast  │ Clean      │ Effort       │ │
│  ├───────────────────────┼──────────┼────────────┼────────────┼──────────────┤ │
│  │ Redesign              │ ✓        │ ✓          │ ✓ BEST     │ HIGH         │ │
│  │ Events                │ ✓        │ ✓          │ ✓ GOOD     │ MEDIUM       │ │
│  │ @Lazy                 │ ✓        │ ○ delayed  │ ○ OK       │ LOW          │ │
│  │ ObjectFactory/Provider│ ✓        │ ○ delayed  │ ○ OK       │ LOW          │ │
│  │ Setter injection      │ ✗        │ ✗          │ ✗ BAD      │ LOW          │ │
│  └───────────────────────┴──────────┴────────────┴────────────┴──────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### 12. Multiple Beans for the Same Interface — Resolution with @Primary and @Qualifier

When you have **two or more beans** that implement the same interface, Spring doesn't know which one to inject. This causes a `NoUniqueBeanDefinitionException`. Spring provides `@Primary`, `@Qualifier`, and other mechanisms to resolve this ambiguity.

---

#### 12.1 The Problem — Multiple Beans of the Same Type

```java
// ── Two implementations of the same interface ────────────────────────────────────
public interface NotificationService {
    void send(String to, String message);
}

@Service
public class EmailNotificationService implements NotificationService {
    public void send(String to, String message) {
        System.out.println("Sending EMAIL to " + to + ": " + message);
    }
}

@Service
public class SmsNotificationService implements NotificationService {
    public void send(String to, String message) {
        System.out.println("Sending SMS to " + to + ": " + message);
    }
}

// ── A service that depends on NotificationService ────────────────────────────────
@Service
public class AlertService {

    private final NotificationService notificationService;

    public AlertService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    // ✗ Which one? EmailNotificationService or SmsNotificationService?
    // Spring has NO WAY to decide → throws exception
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  The Error:                                                                      │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  NoUniqueBeanDefinitionException:                                        │   │
│  │                                                                          │   │
│  │  No qualifying bean of type 'NotificationService' available:            │   │
│  │  expected single matching bean but found 2:                              │   │
│  │  emailNotificationService, smsNotificationService                        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Spring found TWO beans of type NotificationService.                            │
│  It doesn't know which one you want.                                            │
│  APPLICATION FAILED TO START.                                                    │
│                                                                                  │
│  This happens with:                                                              │
│  → Constructor injection                                                        │
│  → Field injection (@Autowired)                                                 │
│  → Setter injection (@Autowired)                                                │
│  → Any injection point where the type has multiple implementations              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 12.2 Solution 1: @Primary — Set a Default Bean

`@Primary` marks one bean as the **default** choice when multiple beans of the same type exist. Spring will always inject the `@Primary` bean unless explicitly overridden with `@Qualifier`.

```java
// ── @Primary: EmailNotificationService is the DEFAULT ────────────────────────────
public interface NotificationService {
    void send(String to, String message);
}

@Service
@Primary    // ← this bean is the DEFAULT when no @Qualifier is specified
public class EmailNotificationService implements NotificationService {
    public void send(String to, String message) {
        System.out.println("Sending EMAIL to " + to + ": " + message);
    }
}

@Service
public class SmsNotificationService implements NotificationService {
    public void send(String to, String message) {
        System.out.println("Sending SMS to " + to + ": " + message);
    }
}

// ── Now AlertService works — EmailNotificationService is injected ────────────────
@Service
public class AlertService {

    private final NotificationService notificationService;

    public AlertService(NotificationService notificationService) {
        this.notificationService = notificationService;
        // ← EmailNotificationService injected (it has @Primary)
    }

    public void sendAlert(String to, String message) {
        notificationService.send(to, message);
        // Sends EMAIL (the @Primary bean)
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Primary — How It Works:                                                        │
│                                                                                  │
│  Container has:                                                                  │
│  ┌────────────────────────────────────────────────────────────────┐             │
│  │  Bean: emailNotificationService  → @Primary ★ (DEFAULT)       │             │
│  │  Bean: smsNotificationService    → (no annotation)            │             │
│  └────────────────────────────────────────────────────────────────┘             │
│                                                                                  │
│  When injecting NotificationService:                                             │
│  → 2 beans found → which one?                                                  │
│  → Check for @Primary → emailNotificationService ★                             │
│  → Inject emailNotificationService                                              │
│                                                                                  │
│  Rules:                                                                          │
│  ✓ Only ONE bean of a type should be @Primary                                  │
│  ✓ @Primary acts as the DEFAULT — can be overridden by @Qualifier              │
│  ✓ @Primary works across the entire application context                        │
│  ✗ Two @Primary beans of the same type → NoUniqueBeanDefinitionException       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── @Primary with @Bean methods (in @Configuration class) ────────────────────────
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary    // ← default DataSource
    public DataSource primaryDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://primary-db:3306/mydb");
        return ds;
    }

    @Bean
    public DataSource readReplicaDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://replica-db:3306/mydb");
        return ds;
    }
}

@Repository
public class UserRepository {
    public UserRepository(DataSource dataSource) {
        // ← primaryDataSource injected (it has @Primary)
    }
}
```

##### @Primary — Advantages, Disadvantages

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Primary — Pros and Cons:                                                       │
│                                                                                  │
│  Advantages:                                                                     │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✓ Simple — one annotation, no changes at injection points             │   │
│  │  ✓ Clean — consumers don't need to know about alternatives             │   │
│  │  ✓ Good for "default" bean — most consumers use the primary            │   │
│  │  ✓ Works with @ComponentScan and @Bean methods                         │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Disadvantages:                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ Global — affects ALL injection points for this type                 │   │
│  │  ✗ Only ONE primary per type — can't have two defaults                 │   │
│  │  ✗ Not explicit at injection point — you need to look at the bean      │   │
│  │    definition to know WHICH implementation is injected                  │   │
│  │  ✗ Doesn't help when you need DIFFERENT beans in different places      │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 12.3 Solution 2: @Qualifier — Explicitly Choose Which Bean

`@Qualifier` lets you specify **by name** which bean to inject at each injection point. It gives you **fine-grained control** — different consumers can get different beans.

```java
// ── @Qualifier: explicitly select which bean at each injection point ─────────────
public interface NotificationService {
    void send(String to, String message);
}

@Service
@Qualifier("email")    // ← this bean is named "email"
public class EmailNotificationService implements NotificationService {
    public void send(String to, String message) {
        System.out.println("Sending EMAIL to " + to + ": " + message);
    }
}

@Service
@Qualifier("sms")    // ← this bean is named "sms"
public class SmsNotificationService implements NotificationService {
    public void send(String to, String message) {
        System.out.println("Sending SMS to " + to + ": " + message);
    }
}

@Service
@Qualifier("push")
public class PushNotificationService implements NotificationService {
    public void send(String to, String message) {
        System.out.println("Sending PUSH to " + to + ": " + message);
    }
}

// ── Different services inject DIFFERENT implementations ──────────────────────────
@Service
public class OrderService {

    private final NotificationService notificationService;

    public OrderService(@Qualifier("email") NotificationService notificationService) {
        this.notificationService = notificationService;
        // ← EmailNotificationService injected
    }

    public void placeOrder(Order order) {
        notificationService.send(order.getCustomerEmail(), "Order placed!");
        // Sends EMAIL
    }
}

@Service
public class OtpService {

    private final NotificationService notificationService;

    public OtpService(@Qualifier("sms") NotificationService notificationService) {
        this.notificationService = notificationService;
        // ← SmsNotificationService injected
    }

    public void sendOtp(String phone, String otp) {
        notificationService.send(phone, "Your OTP: " + otp);
        // Sends SMS
    }
}

@Service
public class MarketingService {

    private final NotificationService notificationService;

    public MarketingService(@Qualifier("push") NotificationService notificationService) {
        this.notificationService = notificationService;
        // ← PushNotificationService injected
    }

    public void sendPromo(String userId, String promo) {
        notificationService.send(userId, promo);
        // Sends PUSH notification
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Qualifier — How It Works:                                                      │
│                                                                                  │
│  Container has:                                                                  │
│  ┌────────────────────────────────────────────────────────────────┐             │
│  │  Bean: emailNotificationService  → @Qualifier("email")        │             │
│  │  Bean: smsNotificationService    → @Qualifier("sms")          │             │
│  │  Bean: pushNotificationService   → @Qualifier("push")         │             │
│  └────────────────────────────────────────────────────────────────┘             │
│                                                                                  │
│  Injection in OrderService:                                                      │
│  → Type: NotificationService → 3 beans found                                   │
│  → @Qualifier("email") → filter to "email" → EmailNotificationService ✓       │
│                                                                                  │
│  Injection in OtpService:                                                        │
│  → Type: NotificationService → 3 beans found                                   │
│  → @Qualifier("sms") → filter to "sms" → SmsNotificationService ✓             │
│                                                                                  │
│  Each injection point EXPLICITLY declares which bean it wants.                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── @Qualifier with field injection ──────────────────────────────────────────────
@Service
public class MultiChannelService {

    @Autowired
    @Qualifier("email")
    private NotificationService emailService;

    @Autowired
    @Qualifier("sms")
    private NotificationService smsService;

    public void notifyAllChannels(String to, String message) {
        emailService.send(to, message);
        smsService.send(to, message);
    }
}

// ── @Qualifier with setter injection ─────────────────────────────────────────────
@Service
public class AlertService {

    private NotificationService priorityChannel;

    @Autowired
    @Qualifier("sms")
    public void setPriorityChannel(NotificationService priorityChannel) {
        this.priorityChannel = priorityChannel;
    }
}

// ── @Qualifier with @Bean methods ────────────────────────────────────────────────
@Configuration
public class NotificationConfig {

    @Bean
    @Qualifier("email")
    public NotificationService emailNotification() {
        return new EmailNotificationService();
    }

    @Bean
    @Qualifier("sms")
    public NotificationService smsNotification() {
        return new SmsNotificationService();
    }
}
```

##### @Qualifier — Advantages, Disadvantages

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Qualifier — Pros and Cons:                                                     │
│                                                                                  │
│  Advantages:                                                                     │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✓ EXPLICIT — clear at the injection point which bean is used          │   │
│  │  ✓ FINE-GRAINED — different injection points can get different beans   │   │
│  │  ✓ MULTIPLE BEANS — supports any number of implementations            │   │
│  │  ✓ OVERRIDES @PRIMARY — @Qualifier takes precedence over @Primary     │   │
│  │  ✓ READABLE — code shows exactly which implementation is injected     │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Disadvantages:                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  ✗ STRING-BASED — qualifier names are strings (typo → runtime error)  │   │
│  │  ✗ VERBOSE — must add @Qualifier at EVERY injection point             │   │
│  │  ✗ COUPLING — consumer knows the qualifier name (implementation hint) │   │
│  │  ✗ REFACTORING RISK — rename qualifier → update all injection points  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 12.4 @Primary + @Qualifier Together

`@Primary` and `@Qualifier` work together: `@Primary` sets the **default**, and `@Qualifier` **overrides** it at specific injection points.

```java
// ── Combine @Primary and @Qualifier for maximum flexibility ──────────────────────
public interface PaymentGateway {
    PaymentResult charge(BigDecimal amount, String token);
}

@Service
@Primary    // ← default gateway (most services use Stripe)
public class StripePaymentGateway implements PaymentGateway {
    public PaymentResult charge(BigDecimal amount, String token) {
        System.out.println("Charging via STRIPE: $" + amount);
        return PaymentResult.success();
    }
}

@Service
@Qualifier("paypal")
public class PayPalPaymentGateway implements PaymentGateway {
    public PaymentResult charge(BigDecimal amount, String token) {
        System.out.println("Charging via PAYPAL: $" + amount);
        return PaymentResult.success();
    }
}

@Service
@Qualifier("crypto")
public class CryptoPaymentGateway implements PaymentGateway {
    public PaymentResult charge(BigDecimal amount, String token) {
        System.out.println("Charging via CRYPTO: $" + amount);
        return PaymentResult.success();
    }
}

// ── Service 1: uses DEFAULT (Stripe) — no @Qualifier needed ─────────────────────
@Service
public class OrderService {
    private final PaymentGateway paymentGateway;

    public OrderService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
        // ← StripePaymentGateway (@Primary) injected automatically
    }
}

// ── Service 2: explicitly wants PayPal — overrides @Primary ──────────────────────
@Service
public class SubscriptionService {
    private final PaymentGateway paymentGateway;

    public SubscriptionService(@Qualifier("paypal") PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
        // ← PayPalPaymentGateway injected (@Qualifier overrides @Primary)
    }
}

// ── Service 3: explicitly wants Crypto ───────────────────────────────────────────
@Service
public class DonationService {
    private final PaymentGateway paymentGateway;

    public DonationService(@Qualifier("crypto") PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
        // ← CryptoPaymentGateway injected
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Primary + @Qualifier — Resolution Priority:                                    │
│                                                                                  │
│  Spring resolves beans in this order:                                            │
│                                                                                  │
│  1. @Qualifier specified?                                                        │
│     → YES: inject the bean with matching qualifier name                         │
│     → NO: continue to step 2                                                    │
│                                                                                  │
│  2. @Primary exists?                                                             │
│     → YES: inject the @Primary bean                                             │
│     → NO: continue to step 3                                                    │
│                                                                                  │
│  3. Parameter name matches a bean name?                                          │
│     → YES: inject the bean with matching name                                   │
│     → NO: throw NoUniqueBeanDefinitionException                                 │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Priority:  @Qualifier  >  @Primary  >  Parameter name  >  ERROR       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Best Practice:                                                                  │
│  → Use @Primary for the MOST COMMON implementation                              │
│  → Use @Qualifier only where you need a SPECIFIC alternative                    │
│  → This minimizes boilerplate (most injection points need no annotation)        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 12.5 Other Resolution Strategies

##### Strategy 1: Inject ALL Beans as a Collection

```java
// ── Inject all implementations as a List ─────────────────────────────────────────
@Service
public class NotificationDispatcher {

    private final List<NotificationService> allChannels;

    public NotificationDispatcher(List<NotificationService> allChannels) {
        this.allChannels = allChannels;
        // allChannels = [EmailNotificationService, SmsNotificationService, PushNotificationService]
        // Spring injects ALL beans of type NotificationService
    }

    public void notifyAll(String to, String message) {
        allChannels.forEach(channel -> channel.send(to, message));
        // Sends via ALL channels — email, SMS, and push
    }
}

// ── Inject as Map<String, T> to select by name ──────────────────────────────────
@Service
public class DynamicNotificationService {

    private final Map<String, NotificationService> channelMap;

    public DynamicNotificationService(Map<String, NotificationService> channelMap) {
        this.channelMap = channelMap;
        // channelMap = {
        //   "emailNotificationService" → EmailNotificationService,
        //   "smsNotificationService"   → SmsNotificationService,
        //   "pushNotificationService"  → PushNotificationService
        // }
    }

    public void send(String channel, String to, String message) {
        NotificationService service = channelMap.get(channel);
        if (service == null) {
            throw new IllegalArgumentException("Unknown channel: " + channel);
        }
        service.send(to, message);
    }
}
```

##### Strategy 2: Custom Qualifier Annotation (Type-Safe)

```java
// ── Custom qualifier — eliminates string typo risk ───────────────────────────────

// Define custom qualifier annotations:
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier    // ← this makes it a qualifier annotation
public @interface EmailChannel { }

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface SmsChannel { }

// Use on bean definitions:
@Service
@EmailChannel    // ← type-safe qualifier, no strings!
public class EmailNotificationService implements NotificationService { ... }

@Service
@SmsChannel
public class SmsNotificationService implements NotificationService { ... }

// Use at injection points:
@Service
public class OrderService {

    private final NotificationService notificationService;

    public OrderService(@EmailChannel NotificationService notificationService) {
        this.notificationService = notificationService;
        // ← EmailNotificationService injected
        // ✓ Type-safe — typo = compile error, not runtime error
        // ✓ Refactorable — IDE can rename the annotation
    }
}
```

##### Strategy 3: @Profile — Environment-Based Selection

```java
// ── Different bean per environment — only ONE is active at a time ────────────────
public interface CacheService {
    void put(String key, Object value);
    Object get(String key);
}

@Service
@Profile("dev")    // only active in dev
public class InMemoryCacheService implements CacheService {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    public void put(String key, Object value) { cache.put(key, value); }
    public Object get(String key) { return cache.get(key); }
}

@Service
@Profile("prod")    // only active in prod
public class RedisCacheService implements CacheService {
    private final RedisTemplate<String, Object> redis;
    public RedisCacheService(RedisTemplate<String, Object> redis) { this.redis = redis; }
    public void put(String key, Object value) { redis.opsForValue().set(key, value); }
    public Object get(String key) { return redis.opsForValue().get(key); }
}

// No ambiguity — only ONE bean exists per environment:
@Service
public class ProductService {
    private final CacheService cacheService;

    public ProductService(CacheService cacheService) {
        this.cacheService = cacheService;
        // dev  → InMemoryCacheService
        // prod → RedisCacheService
    }
}
```

---

#### 12.6 Summary — @Primary vs @Qualifier vs Other Strategies

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Multiple Beans — Resolution Strategy Comparison:                                │
│                                                                                  │
│  ┌────────────────────────┬───────────────────────────────────────────────────┐ │
│  │ Strategy               │ When to Use                                      │ │
│  ├────────────────────────┼───────────────────────────────────────────────────┤ │
│  │ @Primary               │ One bean is the DEFAULT for most consumers.     │ │
│  │                        │ Simple, minimal changes. Good for 1 default +   │ │
│  │                        │ rare overrides.                                  │ │
│  ├────────────────────────┼───────────────────────────────────────────────────┤ │
│  │ @Qualifier             │ Different consumers need DIFFERENT beans.        │ │
│  │                        │ Explicit at each injection point.                │ │
│  │                        │ Best for 2-3 implementations.                    │ │
│  ├────────────────────────┼───────────────────────────────────────────────────┤ │
│  │ @Primary + @Qualifier  │ BEST of both: default for most, specific for   │ │
│  │                        │ some. Minimizes boilerplate.                     │ │
│  ├────────────────────────┼───────────────────────────────────────────────────┤ │
│  │ Custom @Qualifier      │ Type-safe qualifiers (no string typos).         │ │
│  │ annotation             │ Best for large projects with many beans.        │ │
│  ├────────────────────────┼───────────────────────────────────────────────────┤ │
│  │ List<T> / Map<String,T>│ Need ALL implementations (strategy pattern).    │ │
│  │                        │ Dynamic dispatch, plugin architectures.          │ │
│  ├────────────────────────┼───────────────────────────────────────────────────┤ │
│  │ @Profile               │ Different beans per ENVIRONMENT.                 │ │
│  │                        │ Only one active at a time. No ambiguity.        │ │
│  ├────────────────────────┼───────────────────────────────────────────────────┤ │
│  │ @ConditionalOnProperty │ Bean exists only if a config property is set.   │ │
│  │                        │ Feature flags, optional modules.                 │ │
│  ├────────────────────────┼───────────────────────────────────────────────────┤ │
│  │ Parameter name match   │ Automatic fallback — parameter name = bean name.│ │
│  │                        │ Fragile (rename breaks it). Not recommended.    │ │
│  └────────────────────────┴───────────────────────────────────────────────────┘ │
│                                                                                  │
│  Resolution Priority (reminder):                                                │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  @Qualifier  ▶  @Primary  ▶  Parameter name  ▶  ERROR                  │   │
│  │  (highest)                                       (NoUniqueBeanDef...)   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 13. Bean Scopes — @Scope Annotation

The **scope** of a bean defines **how many instances** Spring creates and **how long** each instance lives. By default, every Spring bean is a **singleton** — one instance shared across the entire application. But Spring supports multiple scopes for different use cases.

---

#### 13.1 What Is @Scope?

`@Scope` is a Spring annotation that defines the **lifecycle and visibility** of a bean instance. It controls when a new instance is created and when it is destroyed.

```java
// ── @Scope annotation — definition ──────────────────────────────────────────────
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

    // Alias for "scopeName"
    @AliasFor("scopeName")
    String value() default "";

    // The name of the scope: "singleton", "prototype", "request", "session", "application"
    @AliasFor("value")
    String scopeName() default "";

    // How to create a proxy for this bean when injected into a different scope
    ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Scope — Attributes Explained:                                                  │
│                                                                                  │
│  ┌─────────────────┬─────────────────────────────────────────────────────────┐  │
│  │ Attribute        │ Description                                            │  │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤  │
│  │ value /          │ The scope name as a String.                            │  │
│  │ scopeName        │ Possible values:                                       │  │
│  │                  │   "singleton"   — one instance per Spring container    │  │
│  │                  │   "prototype"   — new instance every time requested   │  │
│  │                  │   "request"     — one instance per HTTP request       │  │
│  │                  │   "session"     — one instance per HTTP session       │  │
│  │                  │   "application" — one instance per ServletContext     │  │
│  │                  │   "websocket"   — one instance per WebSocket session  │  │
│  │                  │ Default: "" (resolves to "singleton")                  │  │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤  │
│  │ proxyMode        │ Determines how Spring creates a proxy when this bean  │  │
│  │                  │ is injected into a bean with a WIDER scope.           │  │
│  │                  │ Values:                                                │  │
│  │                  │   DEFAULT        — no proxy (same as NO)              │  │
│  │                  │   NO             — no proxy created                   │  │
│  │                  │   INTERFACES     — JDK dynamic proxy (interface-based)│  │
│  │                  │   TARGET_CLASS   — CGLIB proxy (class-based)          │  │
│  │                  │                                                        │  │
│  │                  │ USE proxyMode when injecting a shorter-lived bean     │  │
│  │                  │ (request/session) into a longer-lived bean (singleton)│  │
│  └─────────────────┴─────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Usage examples of @Scope ─────────────────────────────────────────────────────

// Using value attribute:
@Service
@Scope("prototype")
public class ReportGenerator { }

// Using scopeName attribute (same as value):
@Service
@Scope(scopeName = "request")
public class RequestContext { }

// Using ConfigurableBeanFactory constants (type-safe):
@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)    // = "singleton"
public class AppConfig { }

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)    // = "prototype"
public class TaskRunner { }

// Using WebApplicationContext constants for web scopes:
@Service
@Scope(WebApplicationContext.SCOPE_REQUEST)         // = "request"
public class RequestLogger { }

@Service
@Scope(WebApplicationContext.SCOPE_SESSION)         // = "session"
public class UserSession { }

// Using proxyMode:
@Service
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedBean { }

// Using @RequestScope, @SessionScope shortcuts (Spring 4.3+):
@Service
@RequestScope    // same as @Scope(value="request", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class RequestData { }

@Service
@SessionScope    // same as @Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class SessionData { }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  All Bean Scopes — Overview:                                                     │
│                                                                                  │
│  ┌────────────────┬──────────────────────────────────────────────────────────┐  │
│  │ Scope          │ Instances Created                                       │  │
│  ├────────────────┼──────────────────────────────────────────────────────────┤  │
│  │ singleton      │ ONE per Spring IoC container (DEFAULT)                  │  │
│  │ prototype      │ NEW instance every time the bean is requested           │  │
│  │ request        │ ONE per HTTP request (web apps only)                    │  │
│  │ session        │ ONE per HTTP session (web apps only)                    │  │
│  │ application    │ ONE per ServletContext (web apps only)                  │  │
│  │ websocket      │ ONE per WebSocket session (WebSocket apps only)         │  │
│  └────────────────┴──────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Lifecycle Duration (shortest → longest):                                       │
│                                                                                  │
│  prototype < request < session < application ≈ singleton                        │
│  (no mgmt)   (1 req)   (1 sess)  (1 servlet)  (1 container)                    │
│                                                                                  │
│  NOTE: singleton and prototype are available in ALL Spring apps.                │
│  request, session, application, websocket require a web-aware context           │
│  (e.g., Spring MVC or Spring WebFlux).                                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.2 Singleton Scope (Default)

**Singleton** means Spring creates **exactly ONE instance** of the bean per Spring IoC container. This single instance is **shared** across the entire application — every injection point gets the **same object reference**.

```java
// ── Singleton scope — default behavior ───────────────────────────────────────────

@Service    // Singleton by default — no @Scope needed
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println("UserService created: " + this);
        // This prints ONCE at startup — only one instance is ever created
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}

// Explicitly declaring singleton (same as above — redundant but clear):
@Service
@Scope("singleton")    // or @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class UserService { ... }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Singleton Scope — How It Works:                                                 │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                    Spring IoC Container                                   │   │
│  │                                                                          │   │
│  │    ┌────────────────────────────────────────────┐                        │   │
│  │    │  Bean: userService                         │                        │   │
│  │    │  Instance: UserService@0x7f3a (ONE object) │                        │   │
│  │    └──────────────┬─────────────────────────────┘                        │   │
│  │                   │                                                       │   │
│  │         ┌─────────┼──────────┐                                           │   │
│  │         │         │          │                                            │   │
│  │         ▼         ▼          ▼                                            │   │
│  │    Controller  Service   AnotherService                                  │   │
│  │    (gets same  (gets same (gets same                                     │   │
│  │     @0x7f3a)    @0x7f3a)   @0x7f3a)                                     │   │
│  │                                                                          │   │
│  │    ALL injection points get the SAME object reference                    │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Lifecycle:                                                                      │
│  ┌──────────┐     ┌──────────┐     ┌──────────────────────────────────────┐   │
│  │ Container │────▶│ Created  │────▶│ Lives for entire application        │   │
│  │ starts    │     │ (once)   │     │ Destroyed when container shuts down │   │
│  └──────────┘     └──────────┘     └──────────────────────────────────────┘   │
│                                                                                  │
│  Key Points:                                                                     │
│  ✓ Created ONCE at application startup (eagerly initialized)                   │
│  ✓ Cached in the singleton cache — subsequent requests return same instance    │
│  ✓ Shared across ALL threads — must be THREAD-SAFE                             │
│  ✓ Destroyed when ApplicationContext is closed                                 │
│  ✓ @PreDestroy callback IS called                                              │
│  ✓ Default scope — no annotation needed                                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 13.2.1 Singleton Controller with Mixed-Scope Dependencies

**Scenario**: A `RestController` (scope=singleton) depends on beans with scopes: singleton, prototype, and request.

```java
// ── Setup: Singleton RestController with mixed-scope dependencies ────────────────

@Service
@Scope("singleton")    // explicit, but this is the default
public class SingletonHelper {
    public SingletonHelper() {
        System.out.println("SingletonHelper created: " + this);
    }
    public String getData() { return "singleton-data"; }
}

@Service
@Scope("prototype")
public class PrototypeHelper {
    public PrototypeHelper() {
        System.out.println("PrototypeHelper created: " + this);
    }
    public String getData() { return "prototype-data-" + hashCode(); }
}

@Service
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestHelper {
    public RequestHelper() {
        System.out.println("RequestHelper created: " + this);
    }
    public String getData() { return "request-data-" + hashCode(); }
}

@RestController    // Singleton by default
public class MyController {

    private final SingletonHelper singletonHelper;
    private final PrototypeHelper prototypeHelper;
    private final RequestHelper requestHelper;

    public MyController(SingletonHelper singletonHelper,
                        PrototypeHelper prototypeHelper,
                        RequestHelper requestHelper) {
        this.singletonHelper = singletonHelper;
        this.prototypeHelper = prototypeHelper;
        this.requestHelper = requestHelper;
        System.out.println("MyController created");
    }

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of(
            "singleton", singletonHelper.getData(),
            "prototype", prototypeHelper.getData(),
            "request",   requestHelper.getData()
        );
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens at STARTUP (Application Start):                                    │
│                                                                                  │
│  1. Spring creates SingletonHelper (scope=singleton)                            │
│     → SingletonHelper@0xA1 created                                              │
│     → Stored in singleton cache                                                 │
│                                                                                  │
│  2. Spring creates PrototypeHelper (scope=prototype)                            │
│     → PrototypeHelper@0xB1 created                                              │
│     → ⚠ This is created ONCE to satisfy MyController's constructor             │
│     → NOT stored in any cache — Spring forgets about it                        │
│                                                                                  │
│  3. Spring creates a PROXY for RequestHelper (scope=request)                    │
│     → RequestHelper$$CGLIB$$Proxy@0xC0 created (NOT the real bean)             │
│     → The proxy is a lightweight placeholder                                    │
│     → The REAL RequestHelper is NOT created yet (no HTTP request exists)        │
│                                                                                  │
│  4. Spring creates MyController (scope=singleton)                               │
│     → Constructor receives:                                                     │
│       • SingletonHelper@0xA1       (real singleton bean)                        │
│       • PrototypeHelper@0xB1       (real prototype — but FROZEN in singleton!) │
│       • RequestHelper$$Proxy@0xC0  (CGLIB proxy — delegates to real per req)   │
│     → MyController@0xD1 created                                                │
│     → Stored in singleton cache                                                 │
│                                                                                  │
│  Console output at startup:                                                      │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │ SingletonHelper created: SingletonHelper@0xA1                │               │
│  │ PrototypeHelper created: PrototypeHelper@0xB1                │               │
│  │ MyController created                                          │               │
│  │ (RequestHelper is NOT created yet — no request exists)       │               │
│  └──────────────────────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens on FIRST API Call: GET /test (Request #1)                          │
│                                                                                  │
│  1. HTTP request arrives → DispatcherServlet routes to MyController.test()      │
│                                                                                  │
│  2. MyController is SINGLETON → same MyController@0xD1 handles it              │
│                                                                                  │
│  3. singletonHelper.getData() → SingletonHelper@0xA1 (same as startup)         │
│     → Returns "singleton-data"                                                  │
│                                                                                  │
│  4. prototypeHelper.getData() → PrototypeHelper@0xB1 (same as startup!)        │
│     → ⚠ PROBLEM: This is the SAME prototype instance from startup!            │
│     → Returns "prototype-data-B1" (same hash every time)                       │
│     → The prototype bean is TRAPPED inside the singleton controller            │
│     → It will NEVER be a new instance because the controller is never          │
│       re-created (it's singleton!)                                              │
│                                                                                  │
│  5. requestHelper.getData()                                                      │
│     → Proxy intercepts the call                                                 │
│     → Proxy asks Spring: "give me a RequestHelper for the CURRENT request"     │
│     → Spring creates RequestHelper@0xE1 (NEW instance for this request)        │
│     → Proxy delegates getData() to RequestHelper@0xE1                          │
│     → Returns "request-data-E1"                                                │
│                                                                                  │
│  6. Request completes → RequestHelper@0xE1 is destroyed                        │
│                                                                                  │
│  Response: { "singleton": "singleton-data",                                     │
│              "prototype": "prototype-data-B1",                                  │
│              "request":   "request-data-E1" }                                   │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens on SECOND API Call: GET /test (Request #2)                         │
│                                                                                  │
│  1. MyController@0xD1 — same singleton instance                                │
│  2. SingletonHelper@0xA1 — same singleton instance                             │
│  3. PrototypeHelper@0xB1 — ⚠ SAME prototype (stuck in singleton!)            │
│  4. RequestHelper@0xF1 — ✓ NEW instance for this request                      │
│                                                                                  │
│  Response: { "singleton": "singleton-data",                                     │
│              "prototype": "prototype-data-B1",    ← SAME as request #1!        │
│              "request":   "request-data-F1" }     ← DIFFERENT (new instance)   │
│                                                                                  │
│  ⚠ The prototype bean inside a singleton behaves like a SINGLETON!             │
│  To get a NEW prototype each time, use ObjectFactory<T> or Provider<T>:        │
│                                                                                  │
│  @RestController                                                                 │
│  public class MyController {                                                     │
│      private final ObjectFactory<PrototypeHelper> prototypeFactory;             │
│                                                                                  │
│      @GetMapping("/test")                                                        │
│      public String test() {                                                      │
│          PrototypeHelper helper = prototypeFactory.getObject(); // NEW each time│
│          return helper.getData();                                                │
│      }                                                                           │
│  }                                                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Singleton Controller — Bean Creation Diagram:                                   │
│                                                                                  │
│                        APPLICATION STARTUP                                       │
│                             │                                                    │
│              ┌──────────────┼──────────────┐                                    │
│              ▼              ▼              ▼                                    │
│     SingletonHelper   PrototypeHelper   RequestHelper                           │
│         @0xA1             @0xB1         $$Proxy@0xC0                            │
│     (real bean)       (real bean)       (CGLIB proxy)                            │
│              │              │              │                                    │
│              └──────────────┼──────────────┘                                    │
│                             ▼                                                    │
│                    MyController@0xD1 (singleton)                                │
│                             │                                                    │
│              ┌──────────────┼──────────────┐                                    │
│              │         REQUEST #1          │                                    │
│              │              │              │                                    │
│         @0xA1 (same)   @0xB1 (same!)  @0xE1 (NEW)                              │
│                                                                                  │
│              ┌──────────────┼──────────────┐                                    │
│              │         REQUEST #2          │                                    │
│              │              │              │                                    │
│         @0xA1 (same)   @0xB1 (same!)  @0xF1 (NEW)                              │
│                                                                                  │
│  Legend:                                                                          │
│  • Singleton: always same instance                                              │
│  • Prototype in singleton: STUCK as same instance (⚠ common pitfall)          │
│  • Request (with proxy): new instance per HTTP request ✓                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.3 Prototype Scope

**Prototype** means Spring creates a **new instance every time** the bean is requested from the container. Spring does NOT manage the full lifecycle — it creates the bean and hands it off. There is **no caching**, **no destruction callback**.

```java
// ── Prototype scope ──────────────────────────────────────────────────────────────

@Service
@Scope("prototype")    // or @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReportGenerator {

    private final String id = UUID.randomUUID().toString().substring(0, 8);

    public ReportGenerator() {
        System.out.println("ReportGenerator created: " + id);
    }

    public String generate(String type) {
        return "Report-" + type + "-" + id;
    }
}

// Every time Spring is asked for ReportGenerator, it creates a NEW instance:
@Service
public class ReportService {

    private final ApplicationContext context;

    public ReportService(ApplicationContext context) {
        this.context = context;
    }

    public String generateReport(String type) {
        // Each call to getBean() creates a NEW ReportGenerator instance
        ReportGenerator generator = context.getBean(ReportGenerator.class);
        return generator.generate(type);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Prototype Scope — How It Works:                                                 │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                    Spring IoC Container                                   │   │
│  │                                                                          │   │
│  │    Bean Definition: reportGenerator (scope=prototype)                    │   │
│  │    NO cached instance — definition only                                  │   │
│  │                                                                          │   │
│  │    getBean() call #1 → creates ReportGenerator@0xA1 → returns it       │   │
│  │    getBean() call #2 → creates ReportGenerator@0xB2 → returns it       │   │
│  │    getBean() call #3 → creates ReportGenerator@0xC3 → returns it       │   │
│  │                                                                          │   │
│  │    Each call = NEW object. Container does NOT track them.               │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Lifecycle:                                                                      │
│  ┌──────────┐     ┌──────────┐     ┌──────────────────────────────────────┐   │
│  │ Bean      │────▶│ Created  │────▶│ Spring FORGETS about it             │   │
│  │ requested │     │ (new)    │     │ Garbage collected when no refs      │   │
│  └──────────┘     └──────────┘     │ @PreDestroy is NOT called!          │   │
│                                     └──────────────────────────────────────┘   │
│                                                                                  │
│  Key Points:                                                                     │
│  ✓ New instance EVERY time getBean() is called or injected                     │
│  ✓ NOT cached — Spring does not hold a reference                               │
│  ✓ Useful for stateful beans (each consumer gets its own state)                │
│  ✗ @PreDestroy is NOT called (Spring doesn't track prototype beans)            │
│  ✗ YOU are responsible for cleanup                                             │
│  ✗ Injecting prototype into singleton → prototype acts like singleton!         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 13.3.1 Prototype Controller with Mixed-Scope Dependencies

**Scenario**: A `RestController` with `scope=prototype` depends on singleton, prototype, and request beans.

```java
// ── Setup: Prototype RestController with mixed-scope dependencies ────────────────

@Service
public class SingletonHelper {
    public SingletonHelper() {
        System.out.println("SingletonHelper created: " + this);
    }
}

@Service
@Scope("prototype")
public class PrototypeHelper {
    public PrototypeHelper() {
        System.out.println("PrototypeHelper created: " + this);
    }
}

@Service
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestHelper {
    public RequestHelper() {
        System.out.println("RequestHelper created: " + this);
    }
}

@RestController
@Scope("prototype")    // ⚠ UNUSUAL — controllers are almost always singletons
public class PrototypeController {

    private final SingletonHelper singletonHelper;
    private final PrototypeHelper prototypeHelper;
    private final RequestHelper requestHelper;

    public PrototypeController(SingletonHelper singletonHelper,
                               PrototypeHelper prototypeHelper,
                               RequestHelper requestHelper) {
        this.singletonHelper = singletonHelper;
        this.prototypeHelper = prototypeHelper;
        this.requestHelper = requestHelper;
        System.out.println("PrototypeController created: " + this);
    }

    @GetMapping("/proto-test")
    public Map<String, String> test() {
        return Map.of(
            "controller", toString(),
            "singleton", singletonHelper.toString(),
            "prototype", prototypeHelper.toString(),
            "request",   requestHelper.toString()
        );
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens at STARTUP:                                                        │
│                                                                                  │
│  1. Spring creates SingletonHelper@0xA1 (singleton — created eagerly)           │
│  2. PrototypeController is prototype — NOT created at startup                   │
│     → Spring only registers the bean DEFINITION                                 │
│  3. PrototypeHelper is prototype — NOT created at startup                       │
│  4. RequestHelper proxy is created (CGLIB proxy — no real instance yet)         │
│                                                                                  │
│  Console at startup:                                                             │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │ SingletonHelper created: SingletonHelper@0xA1                │               │
│  │ (nothing else — prototype beans are lazy!)                   │               │
│  └──────────────────────────────────────────────────────────────┘               │
│                                                                                  │
│  ⚠ NOTE: In practice, Spring MVC's DispatcherServlet caches handler mappings   │
│  at startup. For a prototype-scoped controller, Spring will still create         │
│  at least ONE instance to discover the @RequestMapping methods. But the         │
│  key behavior is that a NEW controller CAN be created per request if            │
│  configured with a prototype-aware handler adapter.                              │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens on FIRST API Call: GET /proto-test (Request #1)                    │
│                                                                                  │
│  1. DispatcherServlet receives request                                           │
│  2. Spring creates a NEW PrototypeController (it's prototype!)                  │
│     → Needs SingletonHelper → gets SingletonHelper@0xA1 (same singleton)       │
│     → Needs PrototypeHelper → creates NEW PrototypeHelper@0xB1                 │
│     → Needs RequestHelper → gets RequestHelper$$Proxy@0xC0                     │
│     → PrototypeController@0xD1 created                                          │
│                                                                                  │
│  3. test() method is called on PrototypeController@0xD1                         │
│     → singletonHelper → SingletonHelper@0xA1                                   │
│     → prototypeHelper → PrototypeHelper@0xB1                                   │
│     → requestHelper → proxy creates RequestHelper@0xE1 for this request        │
│                                                                                  │
│  4. Request completes                                                            │
│     → RequestHelper@0xE1 destroyed                                              │
│     → PrototypeController@0xD1 and PrototypeHelper@0xB1 become eligible        │
│       for garbage collection (Spring doesn't track them)                        │
│                                                                                  │
│  Console:                                                                        │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │ PrototypeHelper created: PrototypeHelper@0xB1                │               │
│  │ PrototypeController created: PrototypeController@0xD1        │               │
│  │ RequestHelper created: RequestHelper@0xE1                    │               │
│  └──────────────────────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens on SECOND API Call: GET /proto-test (Request #2)                   │
│                                                                                  │
│  1. Spring creates ANOTHER NEW PrototypeController                              │
│     → SingletonHelper@0xA1 (same singleton — always)                           │
│     → PrototypeHelper@0xB2 (NEW — prototype creates new each time)             │
│     → RequestHelper$$Proxy@0xC0 (same proxy object)                            │
│     → PrototypeController@0xD2 (NEW controller instance)                       │
│                                                                                  │
│  2. requestHelper.toString() → proxy creates RequestHelper@0xF2                │
│                                                                                  │
│  Everything is NEW except the singleton!                                        │
│                                                                                  │
│  ⚠ IMPORTANT: Making a @RestController prototype-scoped is UNUSUAL and         │
│  generally NOT recommended. DispatcherServlet's default handler adapter         │
│  treats controllers as singletons. You would need custom configuration          │
│  to get true prototype behavior per request.                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.4 Request Scope

**Request** scope means Spring creates a **new bean instance for each HTTP request**. The bean lives for the duration of a single HTTP request and is destroyed when the request completes. This scope is only available in **web-aware** Spring applications (Spring MVC, Spring WebFlux).

```java
// ── Request scope ────────────────────────────────────────────────────────────────

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {

    private final String requestId = UUID.randomUUID().toString().substring(0, 8);
    private final long startTime = System.currentTimeMillis();

    public RequestContext() {
        System.out.println("RequestContext created: " + requestId);
    }

    public String getRequestId() { return requestId; }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("RequestContext destroyed: " + requestId);
        // ✓ @PreDestroy IS called for request-scoped beans
    }
}

// Shorter form using @RequestScope (Spring 4.3+):
@Component
@RequestScope    // = @Scope(value="request", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class RequestContext { ... }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Request Scope — How It Works:                                                   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                          │   │
│  │  Request #1 (Thread-A):                                                  │   │
│  │  ┌──────────────────────────────────────┐                                │   │
│  │  │ RequestContext@0xA1 created          │                                │   │
│  │  │ requestId = "abc12345"               │                                │   │
│  │  │ Lives for this request ONLY          │                                │   │
│  │  │ Destroyed when response is sent      │                                │   │
│  │  └──────────────────────────────────────┘                                │   │
│  │                                                                          │   │
│  │  Request #2 (Thread-B, concurrent):                                      │   │
│  │  ┌──────────────────────────────────────┐                                │   │
│  │  │ RequestContext@0xB2 created          │                                │   │
│  │  │ requestId = "def67890"               │                                │   │
│  │  │ Completely SEPARATE instance          │                                │   │
│  │  │ Thread-B sees ONLY this instance     │                                │   │
│  │  └──────────────────────────────────────┘                                │   │
│  │                                                                          │   │
│  │  Request #3 (Thread-A reused):                                           │   │
│  │  ┌──────────────────────────────────────┐                                │   │
│  │  │ RequestContext@0xC3 created          │                                │   │
│  │  │ requestId = "ghi11111"               │                                │   │
│  │  │ NEW instance — even though same thread│                               │   │
│  │  └──────────────────────────────────────┘                                │   │
│  │                                                                          │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Lifecycle:                                                                      │
│  ┌──────────┐     ┌──────────┐     ┌──────────────────┐     ┌──────────┐      │
│  │ HTTP req  │────▶│ Created  │────▶│ Used throughout  │────▶│ Destroyed│      │
│  │ arrives   │     │ (new)    │     │ the request      │     │(@PreDest)│      │
│  └──────────┘     └──────────┘     └──────────────────┘     └──────────┘      │
│                                                                                  │
│  Key Points:                                                                     │
│  ✓ One instance per HTTP request                                                │
│  ✓ Thread-safe by nature (each request = own instance)                         │
│  ✓ Perfect for request-specific data (user context, audit info, etc.)          │
│  ✓ @PreDestroy IS called when request completes                                │
│  ✓ Stored in request attributes (HttpServletRequest)                           │
│  ✗ Only available in web-aware ApplicationContext                              │
│  ✗ Must use proxyMode when injected into singleton beans                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 13.4.1 Request Controller with Mixed-Scope Dependencies

**Scenario**: A `RestController` with `scope=request` depends on singleton, prototype, and request beans.

```java
// ── Setup: Request-scoped controller (unusual but demonstrates behavior) ─────────

@Service
public class SingletonHelper {
    public SingletonHelper() {
        System.out.println("SingletonHelper created: " + this);
    }
}

@Service
@Scope("prototype")
public class PrototypeHelper {
    public PrototypeHelper() {
        System.out.println("PrototypeHelper created: " + this);
    }
}

@Service
@RequestScope
public class RequestHelper {
    public RequestHelper() {
        System.out.println("RequestHelper created: " + this);
    }
}

@RestController
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestController {

    private final SingletonHelper singletonHelper;
    private final PrototypeHelper prototypeHelper;
    private final RequestHelper requestHelper;

    public RequestController(SingletonHelper singletonHelper,
                             PrototypeHelper prototypeHelper,
                             RequestHelper requestHelper) {
        this.singletonHelper = singletonHelper;
        this.prototypeHelper = prototypeHelper;
        this.requestHelper = requestHelper;
        System.out.println("RequestController created: " + this);
    }

    @GetMapping("/req-test")
    public Map<String, String> test() {
        return Map.of(
            "controller", toString(),
            "singleton",  singletonHelper.toString(),
            "prototype",  prototypeHelper.toString(),
            "request",    requestHelper.toString()
        );
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens at STARTUP:                                                        │
│                                                                                  │
│  1. SingletonHelper@0xA1 created (singleton — eagerly initialized)              │
│  2. RequestController is request-scoped → NOT created at startup                │
│     → A CGLIB PROXY is registered for the controller                            │
│  3. PrototypeHelper → not created yet (will be created with controller)         │
│  4. RequestHelper → proxy registered (not created yet)                          │
│                                                                                  │
│  Console at startup:                                                             │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │ SingletonHelper created: SingletonHelper@0xA1                │               │
│  └──────────────────────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens on FIRST API Call: GET /req-test (Request #1)                      │
│                                                                                  │
│  1. HTTP request arrives                                                         │
│  2. DispatcherServlet calls the controller proxy                                │
│  3. Proxy creates a NEW RequestController for this request:                     │
│     → Needs SingletonHelper → gets SingletonHelper@0xA1 (same singleton)       │
│     → Needs PrototypeHelper → creates NEW PrototypeHelper@0xB1                 │
│     → Needs RequestHelper → creates NEW RequestHelper@0xE1 (same request)      │
│     → RequestController@0xD1 created                                            │
│                                                                                  │
│  4. test() called → returns all instances                                       │
│  5. Request completes:                                                           │
│     → RequestController@0xD1 destroyed (@PreDestroy called)                    │
│     → RequestHelper@0xE1 destroyed (@PreDestroy called)                        │
│     → PrototypeHelper@0xB1 eligible for GC (no @PreDestroy)                   │
│                                                                                  │
│  Console:                                                                        │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │ PrototypeHelper created: PrototypeHelper@0xB1                │               │
│  │ RequestHelper created: RequestHelper@0xE1                    │               │
│  │ RequestController created: RequestController@0xD1            │               │
│  │ RequestController destroyed: RequestController@0xD1          │               │
│  │ RequestHelper destroyed: RequestHelper@0xE1                  │               │
│  └──────────────────────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens on SECOND API Call: GET /req-test (Request #2)                     │
│                                                                                  │
│  Everything is NEW again (except the singleton):                                │
│                                                                                  │
│  → SingletonHelper@0xA1 (SAME — always)                                        │
│  → PrototypeHelper@0xB2 (NEW)                                                  │
│  → RequestHelper@0xF2 (NEW)                                                    │
│  → RequestController@0xD2 (NEW)                                                │
│                                                                                  │
│  Each request gets a COMPLETELY FRESH controller with fresh dependencies         │
│  (except singletons which are always shared).                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 13.4.2 Singleton Depends on Request-Scoped Bean — The Proxy Problem

**The Problem**: A singleton bean is created at **startup** (before any HTTP request exists). If it depends on a request-scoped bean, the request-scoped bean **cannot be created** because there is no active HTTP request. This causes an error at startup.

```java
// ── PROBLEM: Singleton depends on request-scoped bean WITHOUT proxy ──────────────

@Component
@Scope("request")    // ⚠ NO proxyMode specified!
public class RequestContext {
    private final String requestId = UUID.randomUUID().toString();
    public String getRequestId() { return requestId; }
}

@Service    // Singleton by default
public class AuditService {

    private final RequestContext requestContext;    // ← needs request-scoped bean

    public AuditService(RequestContext requestContext) {
        this.requestContext = requestContext;
        // ✗ FAILS AT STARTUP — no active HTTP request exists!
    }

    public void logAction(String action) {
        System.out.println("[" + requestContext.getRequestId() + "] " + action);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  The Error at Startup:                                                           │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Error creating bean with name 'auditService':                           │   │
│  │                                                                          │   │
│  │  Scope 'request' is not active for the current thread;                  │   │
│  │  consider defining a scoped proxy for this bean if you intend to       │   │
│  │  refer to it from a singleton.                                          │   │
│  │                                                                          │   │
│  │  java.lang.IllegalStateException:                                        │   │
│  │  No thread-bound request found: Are you referring to request            │   │
│  │  attributes outside of an actual web request, or processing a           │   │
│  │  request outside of the originally receiving thread?                     │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  WHY?                                                                            │
│  → AuditService is SINGLETON → created at startup                               │
│  → Constructor needs RequestContext → request-scoped                            │
│  → At startup, there is NO HTTP request active                                  │
│  → Spring cannot create a request-scoped bean outside of a request!            │
│  → APPLICATION FAILS TO START                                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 13.4.3 The Solution: `proxyMode = ScopedProxyMode.TARGET_CLASS`

`ScopedProxyMode.TARGET_CLASS` tells Spring to create a **CGLIB proxy** — a lightweight stand-in class that doesn't require an active HTTP request at creation time. The proxy **delegates** all method calls to the **real** request-scoped bean, which is created lazily when an actual HTTP request is active.

```java
// ── SOLUTION: Add proxyMode to the request-scoped bean ───────────────────────────

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
// OR simply use @RequestScope which includes TARGET_CLASS proxy by default
public class RequestContext {

    private final String requestId = UUID.randomUUID().toString();

    public RequestContext() {
        System.out.println("RequestContext created for request: " + requestId);
    }

    public String getRequestId() { return requestId; }

    @PreDestroy
    public void cleanup() {
        System.out.println("RequestContext destroyed: " + requestId);
    }
}

@Service    // Singleton — created once at startup
public class AuditService {

    private final RequestContext requestContext;    // ← receives a PROXY

    public AuditService(RequestContext requestContext) {
        this.requestContext = requestContext;
        // ✓ Works! requestContext is a CGLIB PROXY, not the real bean
        // The proxy is created WITHOUT needing an HTTP request
        System.out.println("AuditService created. RequestContext is: "
            + requestContext.getClass().getName());
        // Prints: RequestContext$$EnhancerBySpringCGLIB$$abc123
    }

    public void logAction(String action) {
        // When this method is called during an HTTP request:
        // 1. requestContext.getRequestId() → proxy intercepts the call
        // 2. Proxy looks up the REAL RequestContext for the CURRENT request
        // 3. Proxy delegates getRequestId() to the REAL bean
        // 4. Returns the request-specific requestId
        System.out.println("[" + requestContext.getRequestId() + "] " + action);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How proxyMode = ScopedProxyMode.TARGET_CLASS Works:                             │
│                                                                                  │
│  AT STARTUP (no HTTP request exists):                                            │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────┐         │
│  │  Spring creates a CGLIB PROXY class:                               │         │
│  │                                                                    │         │
│  │  RequestContext$$EnhancerBySpringCGLIB$$abc123                    │         │
│  │  ┌────────────────────────────────────────────────────────────┐   │         │
│  │  │  extends RequestContext                                    │   │         │
│  │  │  holds a reference to the BeanFactory                      │   │         │
│  │  │  does NOT hold a real RequestContext instance               │   │         │
│  │  │                                                            │   │         │
│  │  │  @Override                                                 │   │         │
│  │  │  public String getRequestId() {                            │   │         │
│  │  │      // 1. Get the current HttpServletRequest              │   │         │
│  │  │      // 2. Look up the REAL RequestContext for this request │   │         │
│  │  │      // 3. Call getRequestId() on the REAL bean            │   │         │
│  │  │      RequestContext real = beanFactory.getBean(...);        │   │         │
│  │  │      return real.getRequestId();                            │   │         │
│  │  │  }                                                         │   │         │
│  │  └────────────────────────────────────────────────────────────┘   │         │
│  │                                                                    │         │
│  │  This proxy is injected into AuditService (singleton)             │         │
│  └────────────────────────────────────────────────────────────────────┘         │
│                                                                                  │
│  ┌───────────────────────┐                  ┌────────────────────────┐          │
│  │  AuditService         │    holds ref     │ RequestContext$$Proxy  │          │
│  │  (singleton)          │─────────────────▶│ (CGLIB proxy)          │          │
│  │  created at startup   │                  │ created at startup     │          │
│  └───────────────────────┘                  └────────────┬───────────┘          │
│                                                           │                     │
│  DURING HTTP REQUEST:                                    │                     │
│                                                           │                     │
│  Request #1:                                              ▼                     │
│  ┌────────────────────────────────────────────────────────────────────┐         │
│  │  auditService.logAction("user logged in")                          │         │
│  │  → requestContext.getRequestId()                                  │         │
│  │  → PROXY intercepts → "which HTTP request is active?"             │         │
│  │  → Looks in RequestAttributes (ThreadLocal-based)                 │         │
│  │  → Finds/creates RequestContext@0xR1 for Request #1              │         │
│  │  → Delegates getRequestId() to RequestContext@0xR1               │         │
│  │  → Returns "abc-111"                                              │         │
│  └────────────────────────────────────────────────────────────────────┘         │
│                                                                                  │
│  Request #2 (concurrent):                                                       │
│  ┌────────────────────────────────────────────────────────────────────┐         │
│  │  auditService.logAction("order placed")                            │         │
│  │  → requestContext.getRequestId()                                  │         │
│  │  → PROXY intercepts → "which HTTP request is active?"             │         │
│  │  → Finds/creates RequestContext@0xR2 for Request #2              │         │
│  │  → Delegates getRequestId() to RequestContext@0xR2               │         │
│  │  → Returns "def-222" (DIFFERENT — different request!)             │         │
│  └────────────────────────────────────────────────────────────────────┘         │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  SAME proxy object → but delegates to DIFFERENT real beans              │   │
│  │  based on which HTTP request is currently active on the calling thread  │   │
│  │                                                                          │   │
│  │  It uses ThreadLocal under the hood:                                     │   │
│  │  Thread-A (Request #1) → gets RequestContext@0xR1                       │   │
│  │  Thread-B (Request #2) → gets RequestContext@0xR2                       │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ScopedProxyMode Options:                                                        │
│                                                                                  │
│  ┌──────────────────────┬────────────────────────────────────────────────────┐  │
│  │ Mode                 │ Description                                       │  │
│  ├──────────────────────┼────────────────────────────────────────────────────┤  │
│  │ DEFAULT              │ Same as NO. No proxy created.                     │  │
│  │                      │ Injection into wider scope FAILS at startup.      │  │
│  ├──────────────────────┼────────────────────────────────────────────────────┤  │
│  │ NO                   │ No proxy. Bean must be created directly.          │  │
│  │                      │ Only works if injected into same or narrower     │  │
│  │                      │ scope.                                            │  │
│  ├──────────────────────┼────────────────────────────────────────────────────┤  │
│  │ INTERFACES           │ JDK dynamic proxy. Bean MUST implement an        │  │
│  │                      │ interface. Proxy implements the same interface.   │  │
│  │                      │ Lightweight. Preferred when interface exists.     │  │
│  ├──────────────────────┼────────────────────────────────────────────────────┤  │
│  │ TARGET_CLASS         │ CGLIB proxy. Subclasses the bean class.           │  │
│  │                      │ Works with classes (no interface needed).         │  │
│  │                      │ Most commonly used. @RequestScope and             │  │
│  │                      │ @SessionScope use this by default.               │  │
│  └──────────────────────┴────────────────────────────────────────────────────┘  │
│                                                                                  │
│  When to use which:                                                              │
│  → TARGET_CLASS: bean is a concrete class (most common)                        │
│  → INTERFACES: bean implements an interface and you want a lighter proxy        │
│  → NO/DEFAULT: bean is only injected into same-scope or narrower-scope beans   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.5 Session Scope

**Session** scope means Spring creates **one bean instance per HTTP session**. The bean is **lazily initialized** — it is created when first accessed during a session, and it lives for the entire duration of that session. It is destroyed when the session expires or is invalidated.

```java
// ── Session scope ────────────────────────────────────────────────────────────────

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ShoppingCart {

    private final List<String> items = new ArrayList<>();
    private final String cartId = UUID.randomUUID().toString().substring(0, 8);

    public ShoppingCart() {
        System.out.println("ShoppingCart created for session: " + cartId);
    }

    public void addItem(String item) { items.add(item); }
    public List<String> getItems() { return Collections.unmodifiableList(items); }
    public String getCartId() { return cartId; }

    @PreDestroy
    public void cleanup() {
        System.out.println("ShoppingCart destroyed (session expired): " + cartId);
        // ✓ Called when session expires or is invalidated
    }
}

// Shorter form using @SessionScope (Spring 4.3+):
@Component
@SessionScope    // = @Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS)
public class ShoppingCart { ... }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Session Scope — How It Works:                                                   │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                          │   │
│  │  User A (Session: JSESSIONID=AAA111):                                   │   │
│  │  ┌──────────────────────────────────────┐                                │   │
│  │  │ ShoppingCart@0xA1                     │                                │   │
│  │  │ cartId = "cart-aaa"                  │                                │   │
│  │  │ items = ["Laptop", "Mouse"]          │                                │   │
│  │  │                                       │                                │   │
│  │  │ Created on User A's FIRST access     │                                │   │
│  │  │ SAME instance for ALL requests       │                                │   │
│  │  │ in User A's session                  │                                │   │
│  │  │ Destroyed when session expires       │                                │   │
│  │  └──────────────────────────────────────┘                                │   │
│  │                                                                          │   │
│  │  User B (Session: JSESSIONID=BBB222):                                   │   │
│  │  ┌──────────────────────────────────────┐                                │   │
│  │  │ ShoppingCart@0xB2                     │                                │   │
│  │  │ cartId = "cart-bbb"                  │                                │   │
│  │  │ items = ["Keyboard"]                 │                                │   │
│  │  │                                       │                                │   │
│  │  │ COMPLETELY SEPARATE from User A      │                                │   │
│  │  │ Each user has their OWN cart          │                                │   │
│  │  └──────────────────────────────────────┘                                │   │
│  │                                                                          │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Lifecycle:                                                                      │
│  ┌──────────┐     ┌──────────┐     ┌──────────────────┐     ┌──────────┐      │
│  │ First     │────▶│ Created  │────▶│ Lives across     │────▶│ Destroyed│      │
│  │ access in │     │ (lazily) │     │ multiple requests│     │(session  │      │
│  │ session   │     └──────────┘     │ in SAME session  │     │ expires) │      │
│  └──────────┘                      └──────────────────┘     └──────────┘      │
│                                                                                  │
│  Key Points:                                                                     │
│  ✓ ONE instance per HTTP session                                                │
│  ✓ LAZILY initialized (created on first access, not session creation)          │
│  ✓ Persists across multiple HTTP requests within the same session              │
│  ✓ @PreDestroy IS called when session expires/invalidates                      │
│  ✓ Stored in HttpSession attributes                                            │
│  ✓ Perfect for user-specific state: shopping carts, preferences, etc.          │
│  ✗ Only available in web-aware ApplicationContext                              │
│  ✗ Must use proxyMode when injected into singleton beans                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 13.5.1 How Session Scope Differs from Request Scope

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Request Scope vs Session Scope — Comparison:                                    │
│                                                                                  │
│  ┌─────────────────────────┬────────────────────────┬────────────────────────┐  │
│  │ Feature                  │ Request Scope          │ Session Scope          │  │
│  ├─────────────────────────┼────────────────────────┼────────────────────────┤  │
│  │ Instance lifetime       │ ONE HTTP request       │ Entire HTTP session    │  │
│  │                         │ (milliseconds-seconds) │ (minutes-hours)        │  │
│  ├─────────────────────────┼────────────────────────┼────────────────────────┤  │
│  │ Created when            │ HTTP request starts    │ First access in session│  │
│  ├─────────────────────────┼────────────────────────┼────────────────────────┤  │
│  │ Destroyed when          │ HTTP response sent     │ Session expires or     │  │
│  │                         │                        │ invalidated            │  │
│  ├─────────────────────────┼────────────────────────┼────────────────────────┤  │
│  │ Shared across requests? │ NO (new per request)   │ YES (same session)     │  │
│  ├─────────────────────────┼────────────────────────┼────────────────────────┤  │
│  │ Shared across users?    │ NO                     │ NO                     │  │
│  ├─────────────────────────┼────────────────────────┼────────────────────────┤  │
│  │ Stored in               │ RequestAttributes      │ HttpSession            │  │
│  ├─────────────────────────┼────────────────────────┼────────────────────────┤  │
│  │ Use case                │ Request logging,       │ Shopping cart,         │  │
│  │                         │ correlation IDs,       │ user preferences,      │  │
│  │                         │ per-request state      │ wizard/form steps      │  │
│  ├─────────────────────────┼────────────────────────┼────────────────────────┤  │
│  │ @PreDestroy called?     │ YES                    │ YES                    │  │
│  ├─────────────────────────┼────────────────────────┼────────────────────────┤  │
│  │ Needs proxyMode?        │ YES (for singleton DI) │ YES (for singleton DI) │  │
│  └─────────────────────────┴────────────────────────┴────────────────────────┘  │
│                                                                                  │
│  Timeline Example (User A makes 3 requests in one session):                     │
│                                                                                  │
│  ──┬──────────────┬──────────────┬──────────────┬─────────── time              │
│    │ Request #1   │ Request #2   │ Request #3   │ Session                      │
│    │              │              │              │ expires                       │
│    │              │              │              │                               │
│  Request-scoped bean:                                                            │
│    │ @0xR1 ─────▶│ @0xR2 ─────▶│ @0xR3 ─────▶│                               │
│    │ (created &  │ (created &  │ (created &  │                               │
│    │  destroyed)  │  destroyed)  │  destroyed)  │                               │
│                                                                                  │
│  Session-scoped bean:                                                            │
│    │ @0xS1 ─────────────────────────────────────▶│ destroyed                   │
│    │ (created on first access, lives across     │ (session                     │
│    │  all 3 requests, same instance @0xS1)       │  expired)                   │
│                                                                                  │
│  Session scope SURVIVES across requests. Request scope does NOT.                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 13.5.2 Session Lifecycle — When Is a Session Created?

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  HTTP Session Lifecycle:                                                         │
│                                                                                  │
│  1. User makes FIRST request to the application                                 │
│     → Server creates an HttpSession (JSESSIONID cookie sent to browser)        │
│     → Session-scoped beans are NOT created yet (lazy)                          │
│                                                                                  │
│  2. Code accesses a session-scoped bean for the first time                      │
│     → Spring creates the bean instance                                          │
│     → Stores it in the HttpSession                                              │
│     → Subsequent requests in this session get the SAME instance                │
│                                                                                  │
│  3. User keeps making requests (session is ACTIVE)                              │
│     → Session timeout resets on each request                                    │
│     → Default timeout: 30 minutes (configurable)                               │
│     → Same session-scoped bean is used                                          │
│                                                                                  │
│  4. User stops making requests → session EXPIRES after timeout                  │
│     → HttpSession is invalidated                                                │
│     → Session-scoped beans are destroyed (@PreDestroy called)                  │
│     → Memory is freed                                                           │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────┐       │
│  │  # application.properties                                           │       │
│  │  server.servlet.session.timeout=30m    # 30 minutes (default)       │       │
│  │  server.servlet.session.timeout=1h     # 1 hour                     │       │
│  │  server.servlet.session.timeout=15m    # 15 minutes                 │       │
│  └──────────────────────────────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 13.5.3 Session Controller with Mixed-Scope Dependencies

**Scenario**: A `RestController` with `scope=session` depends on singleton, prototype, and request beans.

```java
// ── Setup: Session-scoped controller with mixed dependencies ─────────────────────

@Service
public class SingletonHelper {
    public SingletonHelper() {
        System.out.println("SingletonHelper created: " + this);
    }
}

@Service
@Scope("prototype")
public class PrototypeHelper {
    public PrototypeHelper() {
        System.out.println("PrototypeHelper created: " + this);
    }
}

@Service
@RequestScope
public class RequestHelper {
    public RequestHelper() {
        System.out.println("RequestHelper created: " + this);
    }
}

@RestController
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SessionController {

    private final SingletonHelper singletonHelper;
    private final PrototypeHelper prototypeHelper;
    private final RequestHelper requestHelper;

    public SessionController(SingletonHelper singletonHelper,
                             PrototypeHelper prototypeHelper,
                             RequestHelper requestHelper) {
        this.singletonHelper = singletonHelper;
        this.prototypeHelper = prototypeHelper;
        this.requestHelper = requestHelper;
        System.out.println("SessionController created: " + this);
    }

    @GetMapping("/session-test")
    public Map<String, String> test() {
        return Map.of(
            "controller", toString(),
            "singleton",  singletonHelper.toString(),
            "prototype",  prototypeHelper.toString(),
            "request",    requestHelper.toString()
        );
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Happens at STARTUP:                                                        │
│                                                                                  │
│  1. SingletonHelper@0xA1 created (singleton — eagerly initialized)              │
│  2. SessionController → session-scoped → NOT created at startup                 │
│     → A CGLIB PROXY is registered for the controller                            │
│  3. PrototypeHelper → not created yet                                           │
│  4. RequestHelper → proxy registered (not created yet)                          │
│                                                                                  │
│  Console at startup:                                                             │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │ SingletonHelper created: SingletonHelper@0xA1                │               │
│  └──────────────────────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  User A — FIRST Request: GET /session-test (Request #1, Session #AAA)           │
│                                                                                  │
│  1. HTTP request arrives → new session created (JSESSIONID=AAA)                │
│  2. DispatcherServlet calls the controller proxy                                │
│  3. Proxy checks: "Is there a SessionController in session AAA?"                │
│     → NO → creates a NEW SessionController for this session:                   │
│     → Needs SingletonHelper → gets SingletonHelper@0xA1 (same singleton)       │
│     → Needs PrototypeHelper → creates NEW PrototypeHelper@0xB1                 │
│       ⚠ This prototype is FROZEN in the session controller (same issue         │
│       as with singleton — the controller holds a direct reference)              │
│     → Needs RequestHelper → gets RequestHelper proxy                           │
│     → SessionController@0xD1 created and stored in Session #AAA                │
│                                                                                  │
│  4. test() called                                                                │
│     → requestHelper.toString() → proxy creates RequestHelper@0xE1             │
│                                                                                  │
│  5. Request completes → RequestHelper@0xE1 destroyed                           │
│     → SessionController@0xD1 SURVIVES (stored in session)                      │
│                                                                                  │
│  Console:                                                                        │
│  ┌──────────────────────────────────────────────────────────────┐               │
│  │ PrototypeHelper created: PrototypeHelper@0xB1                │               │
│  │ SessionController created: SessionController@0xD1            │               │
│  │ RequestHelper created: RequestHelper@0xE1                    │               │
│  │ RequestHelper destroyed: RequestHelper@0xE1                  │               │
│  └──────────────────────────────────────────────────────────────┘               │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  User A — SECOND Request: GET /session-test (Request #2, SAME Session #AAA)     │
│                                                                                  │
│  1. HTTP request arrives with JSESSIONID=AAA                                    │
│  2. Proxy checks: "Is there a SessionController in session AAA?"                │
│     → YES → uses SessionController@0xD1 (SAME as request #1!)                 │
│                                                                                  │
│  3. test() called:                                                               │
│     → SingletonHelper@0xA1 (SAME — always)                                    │
│     → PrototypeHelper@0xB1 (SAME — stuck in session controller!)              │
│     → RequestHelper@0xF2 (NEW — new request = new request-scoped bean)        │
│                                                                                  │
│  The SessionController is REUSED within the same session!                       │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  User B — FIRST Request: GET /session-test (Request #3, Session #BBB)           │
│                                                                                  │
│  1. HTTP request arrives → new session created (JSESSIONID=BBB)                │
│  2. Proxy checks: "Is there a SessionController in session BBB?"                │
│     → NO → creates a NEW SessionController for this session:                   │
│     → SingletonHelper@0xA1 (SAME singleton — shared across ALL sessions)       │
│     → PrototypeHelper@0xB3 (NEW — new controller = new prototype)             │
│     → RequestHelper proxy                                                       │
│     → SessionController@0xD3 created and stored in Session #BBB                │
│                                                                                  │
│  User A and User B have DIFFERENT SessionControllers:                           │
│  Session #AAA → SessionController@0xD1                                         │
│  Session #BBB → SessionController@0xD3                                         │
└──────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│  Session-Scoped Controller — Timeline Diagram:                                   │
│                                                                                  │
│  User A (Session #AAA):                                                         │
│  ──┬──────────────┬──────────────┬─────────────────────┬──── time              │
│    │ Request #1   │ Request #2   │ ... more requests   │ Session                │
│    │              │              │                     │ expires               │
│    │              │              │                     │                        │
│  SessionController:                                                              │
│    │ @0xD1 ───────────────────────────────────────────▶│ destroyed             │
│    │ (created)    │ (reused)     │ (reused)            │                        │
│                                                                                  │
│  PrototypeHelper (stuck in controller):                                         │
│    │ @0xB1 ───────────────────────────────────────────▶│ GC'd                  │
│    │ (same)       │ (same)       │ (same)             │                        │
│                                                                                  │
│  RequestHelper:                                                                  │
│    │ @0xE1 ─────▶│ @0xF2 ─────▶│ @0xG3 ────────────▶│                        │
│    │ (new)        │ (new)        │ (new per request)  │                        │
│                                                                                  │
│  SingletonHelper:                                                                │
│    │ @0xA1 ──────────────────────────────────────────────── (always same)      │
│                                                                                  │
│  User B (Session #BBB):                                                         │
│  ──┬──────────────┬──────────────────────────────────── time                   │
│    │ Request #1   │ Request #2                                                  │
│    │              │                                                              │
│  SessionController:                                                              │
│    │ @0xD3 ─────────────────▶ (different from User A's @0xD1)                  │
│                                                                                  │
│  SingletonHelper:                                                                │
│    │ @0xA1 ──────────────── (SAME as User A — shared globally)                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.6 Bean Scopes — Complete Summary

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Bean Scope — Complete Comparison:                                               │
│                                                                                  │
│  ┌──────────────┬───────────┬───────────────┬────────────┬─────────────────┐   │
│  │ Scope        │ Instances │ Created When  │ Destroyed  │ @PreDestroy?    │   │
│  ├──────────────┼───────────┼───────────────┼────────────┼─────────────────┤   │
│  │ singleton    │ 1 per     │ Startup       │ Container  │ ✓ YES           │   │
│  │ (default)    │ container │ (eager)       │ shutdown   │                 │   │
│  ├──────────────┼───────────┼───────────────┼────────────┼─────────────────┤   │
│  │ prototype    │ N (new    │ On each       │ Never by   │ ✗ NO            │   │
│  │              │ each time)│ request/inject│ Spring     │ (you manage)    │   │
│  ├──────────────┼───────────┼───────────────┼────────────┼─────────────────┤   │
│  │ request      │ 1 per     │ HTTP request  │ Response   │ ✓ YES           │   │
│  │              │ HTTP req  │ arrives       │ sent       │                 │   │
│  ├──────────────┼───────────┼───────────────┼────────────┼─────────────────┤   │
│  │ session      │ 1 per     │ First access  │ Session    │ ✓ YES           │   │
│  │              │ HTTP sess │ in session    │ expires    │                 │   │
│  ├──────────────┼───────────┼───────────────┼────────────┼─────────────────┤   │
│  │ application  │ 1 per     │ Servlet       │ Servlet    │ ✓ YES           │   │
│  │              │ Servlet   │ context init  │ context    │                 │   │
│  │              │ Context   │               │ destroyed  │                 │   │
│  ├──────────────┼───────────┼───────────────┼────────────┼─────────────────┤   │
│  │ websocket    │ 1 per     │ WebSocket     │ WebSocket  │ ✓ YES           │   │
│  │              │ WS session│ session start │ session end│                 │   │
│  └──────────────┴───────────┴───────────────┴────────────┴─────────────────┘   │
│                                                                                  │
│  Common Pitfalls:                                                                │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  1. Prototype in Singleton → prototype acts like singleton!            │   │
│  │     Fix: Use ObjectFactory<T>, Provider<T>, or @Lookup                 │   │
│  │                                                                          │   │
│  │  2. Request/Session bean in Singleton → startup error!                 │   │
│  │     Fix: Use proxyMode = ScopedProxyMode.TARGET_CLASS                  │   │
│  │     Or use @RequestScope / @SessionScope (includes proxy by default)   │   │
│  │                                                                          │   │
│  │  3. Prototype bean @PreDestroy not called!                              │   │
│  │     Fix: Implement DisposableBean and manage cleanup yourself          │   │
│  │                                                                          │   │
│  │  4. Session-scoped beans consume memory per user!                      │   │
│  │     Fix: Keep session beans lightweight. Set appropriate timeouts.     │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Decision Guide:                                                                 │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │  Is the bean STATELESS?                                                  │   │
│  │  ├── YES → singleton (default) — one instance, shared, thread-safe     │   │
│  │  └── NO  → Does it need UNIQUE state per...                            │   │
│  │           ├── ...injection/usage? → prototype                          │   │
│  │           ├── ...HTTP request?    → request (@RequestScope)            │   │
│  │           ├── ...user session?    → session (@SessionScope)            │   │
│  │           └── ...application?     → singleton (usually sufficient)     │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```


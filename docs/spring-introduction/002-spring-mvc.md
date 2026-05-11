### Spring Framework and Spring MVC

---

#### What is the Spring Framework?

The **Spring Framework** is an open-source Java application framework created by **Rod Johnson** in 2003. It was introduced as a response to the complexity of the J2EE (Java 2 Enterprise Edition) platform, which required heavy XML configuration, EJBs, and verbose boilerplate code for even the simplest enterprise applications.

Spring's core philosophy is captured in two ideas:
- **Inversion of Control (IoC)** — the framework creates and manages objects, not your code
- **Aspect-Oriented Programming (AOP)** — cross-cutting concerns (logging, transactions, security) are handled separately from business logic

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Framework — Historical Timeline:                                         │
│                                                                                  │
│  2002  Rod Johnson writes "Expert One-on-One J2EE Design and Development"        │
│        → Book includes a framework that later becomes Spring                     │
│                                                                                  │
│  2003  Spring Framework 1.0 released (open-source on SourceForge)                │
│        Core features: IoC container, AOP, JDBC abstraction                       │
│                                                                                  │
│  2006  Spring 2.0 — annotation-based config (@Component, @Autowired)             │
│                                                                                  │
│  2007  Spring 2.5 — @Controller, @RequestMapping, @Service, @Repository         │
│                                                                                  │
│  2009  Spring 3.0 — REST support, @Configuration, Java-based config              │
│                                                                                  │
│  2013  Spring 4.0 — WebSocket, Java 8 support, @RestController                  │
│                                                                                  │
│  2017  Spring 5.0 — Reactive programming (WebFlux), HTTP/2                      │
│                                                                                  │
│  2022  Spring 6.0 — Jakarta EE 9+, GraalVM native image support                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**The Spring Framework is not a single thing** — it is an ecosystem of modules:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Framework Module Architecture:                                           │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                         Your Application                                   │ │
│  └──────────────────────────────────┬──────────────────────────────────────────┘ │
│                                     │                                            │
│  ┌────────────┐ ┌────────────┐ ┌────┴───────┐ ┌────────────┐ ┌──────────────┐   │
│  │ Spring MVC │ │  Spring    │ │  Spring    │ │  Spring    │ │   Spring     │   │
│  │ (Web)      │ │ Security   │ │    Data    │ │  Batch     │ │   Cloud      │   │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘ └──────────────┘   │
│                                     │                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │                    Spring AOP  │  Spring Test                              │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                     │                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │                  Spring Core Container (IoC + DI + BeanFactory)            │  │
│  │                                                                            │  │
│  │   Beans  │  Context  │  Core  │  Expression Language (SpEL)               │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                     │                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │        JVM  /  Jakarta EE APIs  /  JDBC  /  JPA  /  JMS  /  ...           │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### What is Spring MVC?

**Spring MVC** is the web layer module of the Spring Framework. It implements the **Model-View-Controller** design pattern on top of the Java Servlet API.

The key innovation: instead of one servlet per URL, Spring MVC uses a **single front controller** called `DispatcherServlet` that receives ALL requests and dispatches them to the appropriate handler (`@Controller` methods).

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Before Spring MVC (raw Servlets):                                               │
│                                                                                  │
│  GET /users    → UserServlet  (one class per endpoint group)                     │
│  GET /orders   → OrderServlet                                                    │
│  GET /products → ProductServlet                                                  │
│  POST /login   → LoginServlet                                                    │
│  ...           → ...Servlet                                                      │
│                                                                                  │
│  50 URLs = 50 Servlet classes + 50 web.xml entries                               │
│                                                                                  │
│  With Spring MVC:                                                                │
│                                                                                  │
│  ALL requests → DispatcherServlet → @Controller methods                          │
│                                                                                  │
│  @RestController                                                                 │
│  public class UserController {                                                   │
│      @GetMapping("/users")      public List<User> list()   { ... }               │
│      @PostMapping("/users")     public User create(...)    { ... }               │
│      @GetMapping("/users/{id}") public User getById(...)   { ... }               │
│  }                                                                               │
│  // One class handles all user endpoints. No web.xml entries needed.             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Features Spring Brought to the Java Ecosystem

##### 1. Inversion of Control (IoC) and Dependency Injection (DI)

Before Spring, developers manually created and wired all objects:

```java
// ❌ Before Spring — manual wiring ("pull" model)
public class OrderService {
    private final UserDao userDao;
    private final InventoryDao inventoryDao;
    private final EmailService emailService;

    public OrderService() {
        // You are responsible for creating every dependency
        DataSource ds = new MySQLDataSource("jdbc:mysql://...", "root", "password");
        this.userDao        = new UserDaoImpl(ds);
        this.inventoryDao   = new InventoryDaoImpl(ds);
        this.emailService   = new SmtpEmailService("smtp.gmail.com", 587);
        // Change a dependency? Edit every class that creates it.
        // Want to swap MySQLDataSource for H2 in tests? Edit this constructor.
    }
}
```

With Spring IoC, the container creates and injects objects for you ("push" model):

```java
// ✅ With Spring — IoC container manages wiring
@Service
public class OrderService {
    // Spring creates these, finds matching beans, and injects them
    private final UserDao userDao;
    private final InventoryDao inventoryDao;
    private final EmailService emailService;

    // Constructor injection (recommended)
    public OrderService(UserDao userDao,
                        InventoryDao inventoryDao,
                        EmailService emailService) {
        this.userDao        = userDao;
        this.inventoryDao   = inventoryDao;
        this.emailService   = emailService;
    }

    public Order placeOrder(Long userId, Long productId) {
        User user      = userDao.findById(userId);
        boolean inStock = inventoryDao.isAvailable(productId);
        if (inStock) {
            Order order = new Order(user, productId);
            emailService.sendConfirmation(user.getEmail(), order);
            return order;
        }
        throw new OutOfStockException(productId);
    }
}

// In tests: inject mock objects
// new OrderService(mockUserDao, mockInventoryDao, mockEmailService)
// No Spring context needed for unit testing!
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  IoC Container — How Spring Manages the Object Graph:                            │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────┐                  │
│  │              Spring ApplicationContext (IoC Container)    │                  │
│  │                                                           │                  │
│  │   ┌───────────────┐     ┌───────────────┐                │                  │
│  │   │  UserDao      │◄────│  OrderService │                │                  │
│  │   │  (Bean)       │     │  (Bean)       │◄────┐          │                  │
│  │   └───────────────┘     └───────────────┘     │          │                  │
│  │   ┌───────────────┐     ┌───────────────┐     │          │                  │
│  │   │ InventoryDao  │◄────│               │     │          │                  │
│  │   │  (Bean)       │     └───────────────┘     │          │                  │
│  │   └───────────────┘                           │          │                  │
│  │   ┌───────────────┐                   ┌───────────────┐  │                  │
│  │   │ EmailService  │◄──────────────────│ OrderController│  │                  │
│  │   │  (Bean)       │                   │  (Bean)       │  │                  │
│  │   └───────────────┘                   └───────────────┘  │                  │
│  │                                                           │                  │
│  │   Beans are: created once (singleton by default),        │                  │
│  │   wired automatically, managed lifecycle (init/destroy)  │                  │
│  └───────────────────────────────────────────────────────────┘                  │
│                                                                                  │
│  Your code DECLARES dependencies → Container PROVIDES them                       │
│  (Inversion: you don't ask for objects — they are given to you)                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 2. Removal of `web.xml`

Raw servlets required a `web.xml` entry for every endpoint. Spring MVC replaces this with Java-based configuration:

```xml
<!-- ❌ Before Spring — web.xml required for EVERY servlet -->
<servlet>
    <servlet-name>UserServlet</servlet-name>
    <servlet-class>com.example.UserServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>UserServlet</servlet-name>
    <url-pattern>/users</url-pattern>
</servlet-mapping>

<!-- Repeat for every endpoint... 50 endpoints = 100+ XML lines -->
```

```java
// ✅ Spring MVC — ONE DispatcherServlet registered programmatically
// WebAppInitializer.java (implements WebApplicationInitializer)
public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) {
        // Create the Spring web application context
        AnnotationConfigWebApplicationContext context =
            new AnnotationConfigWebApplicationContext();
        context.register(AppConfig.class);   // your @Configuration class

        // Register ONE DispatcherServlet that handles ALL requests
        DispatcherServlet dispatcher = new DispatcherServlet(context);
        ServletRegistration.Dynamic registration =
            servletContext.addServlet("dispatcher", dispatcher);

        registration.setLoadOnStartup(1);
        registration.addMapping("/");   // catch-all — all URLs go through Spring
    }
}
// No web.xml needed at all.
```

With Spring Boot, even `WebAppInitializer` disappears — just run `main()`.

##### 3. Unified Request Handling — `@RequestMapping` and Variants

```java
// ❌ Servlet: one class per conceptual endpoint group, manual HTTP method switching
public class UserServlet extends HttpServlet {
    protected void doGet(...)  { /* list or get */ }
    protected void doPost(...) { /* create */ }
    // Must parse URL manually for /users vs /users/42
}

// ✅ Spring MVC: methods per operation, path variables, auto binding
@RestController
@RequestMapping("/users")      // base path for all methods in this class
public class UserController {

    @GetMapping                         // GET /users
    public List<User> list() { ... }

    @GetMapping("/{id}")                // GET /users/42
    public User getById(@PathVariable Long id) { ... }

    @PostMapping                        // POST /users
    public ResponseEntity<User> create(@RequestBody @Valid User user) { ... }

    @PutMapping("/{id}")               // PUT /users/42
    public User update(@PathVariable Long id, @RequestBody User user) { ... }

    @DeleteMapping("/{id}")            // DELETE /users/42
    public ResponseEntity<Void> delete(@PathVariable Long id) { ... }

    @GetMapping("/search")             // GET /users/search?name=Alice&age=25
    public List<User> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int age) { ... }
}
```

##### 4. Automatic JSON/XML Conversion — `HttpMessageConverter`

Before Spring, you serialized JSON manually with string concatenation. Spring MVC uses `HttpMessageConverter` to automatically convert Java objects to/from JSON (via Jackson) or XML (via JAXB):

```java
// ❌ Before Spring — manual JSON
protected void doGet(HttpServletRequest req, HttpServletResponse res) {
    User user = userDao.findById(1L);
    res.setContentType("application/json");
    // Manual, error-prone, no null handling, no nested objects
    res.getWriter().println(
        "{\"id\":" + user.getId() + ",\"name\":\"" + user.getName() + "\"}"
    );
}

// ✅ Spring MVC — return a Java object, Jackson converts to JSON automatically
@GetMapping("/{id}")
public User getById(@PathVariable Long id) {
    return userService.findById(id);
    // Spring calls Jackson: User → {"id":1,"name":"Alice","email":"a@b.com"}
    // Also works in reverse: @RequestBody User user (JSON → User object)
}
```

##### 5. Validation

```java
// User.java — declare constraints
public class User {
    @NotNull @Size(min = 2, max = 50)
    private String name;

    @Email @NotBlank
    private String email;

    @Min(0) @Max(150)
    private int age;
}

// UserController.java — @Valid triggers validation automatically
@PostMapping
public ResponseEntity<User> create(@RequestBody @Valid User user,
                                   BindingResult result) {
    if (result.hasErrors()) {
        // Spring populates BindingResult with all validation failures
        return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.status(201).body(userService.save(user));
}
```

##### 6. Exception Handling

```java
// ✅ Centralized error handling with @ControllerAdvice
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(UserNotFoundException ex) {
        return new ErrorResponse("USER_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return new ErrorResponse("VALIDATION_ERROR", msg);
    }
}
```

##### 7. Easy Integration with Other Frameworks

Spring provides first-class integration modules:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Integration Ecosystem:                                                   │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │                         Spring MVC Application                            │  │
│  └───────────────────────────────────┬────────────────────────────────────────┘  │
│                                      │                                           │
│  ┌──────────────┐  ┌──────────────┐  │  ┌──────────────┐  ┌──────────────────┐  │
│  │  Hibernate / │  │   Redis      │  │  │   Spring     │  │   Spring         │  │
│  │  Spring Data │  │   Cache      │  │  │   Security   │  │   Kafka/JMS      │  │
│  │  JPA         │  │              │  │  │              │  │                  │  │
│  │              │  │ @Cacheable   │  │  │ @Secured     │  │ @KafkaListener   │  │
│  │ @Repository  │  │ @CacheEvict  │  │  │ @PreAuthorize│  │ KafkaTemplate    │  │
│  │ JpaRepository│  │              │  │  │              │  │                  │  │
│  └──────────────┘  └──────────────┘  │  └──────────────┘  └──────────────────┘  │
│                                      │                                           │
│  ┌──────────────┐  ┌──────────────┐  │  ┌──────────────┐  ┌──────────────────┐  │
│  │   Thymeleaf  │  │   MongoDB    │  │  │   Quartz     │  │   Flyway /       │  │
│  │   (Views)    │  │   Spring     │  │  │   Scheduler  │  │   Liquibase      │  │
│  │              │  │   Data Mongo │  │  │              │  │   (DB migration) │  │
│  └──────────────┘  └──────────────┘  │  └──────────────┘  └──────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

Example — Spring + Hibernate in a few lines:

```java
// ✅ Spring Data JPA — replaces hundreds of lines of JDBC/Hibernate boilerplate
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByName(String name);               // Spring generates the query
    Optional<User> findByEmail(String email);
    List<User> findByAgeGreaterThan(int age);
}

// Use it:
@Service
public class UserService {
    private final UserRepository repo;  // injected by Spring

    public UserService(UserRepository repo) { this.repo = repo; }

    @Transactional
    public User save(User user) { return repo.save(user); }  // Spring handles the transaction
}
```

---

#### Advantages and Disadvantages of Spring MVC

**Advantages:**

| Advantage | Explanation |
|---|---|
| **Elimination of boilerplate** | No manual servlet registration, no `web.xml`, no manual JSON parsing |
| **Dependency Injection** | Loose coupling, easy to swap implementations (e.g., swap DB layer for tests) |
| **Testability** | Controllers, services, and repositories are POJOs — easily unit-tested with mocks |
| **Convention over configuration** | Sensible defaults; only configure what differs |
| **Rich annotation model** | `@Controller`, `@Service`, `@Repository`, `@Transactional`, `@Cacheable` — declare intent, not plumbing |
| **Unified exception handling** | `@ControllerAdvice` handles errors across the whole application |
| **Content negotiation** | Same controller returns JSON or XML based on `Accept` header — no code change |
| **Large ecosystem** | Spring Security, Spring Data, Spring Batch, Spring Cloud — all integrate seamlessly |
| **Active community** | One of the most widely adopted Java frameworks; extensive documentation |

**Disadvantages and Limitations:**

| Disadvantage | Explanation |
|---|---|
| **Steep learning curve** | IoC, AOP, proxying, bean scopes — many concepts to understand before being productive |
| **Magic / implicit behavior** | `@Transactional` silently creates a proxy — breaking it (calling a `@Transactional` method internally) is a common bug |
| **Slow startup** | Spring context initialization (scanning, wiring, proxying) is slow for large apps — a problem for serverless/Lambda |
| **Heavy footprint** | Even "lightweight" Spring apps pull in many transitive dependencies |
| **XML config legacy** | Older Spring projects still require understanding verbose XML configuration |
| **Complex debugging** | Stack traces go through Spring proxies/interceptors — harder to trace than plain Java |
| **Still requires external server (pre-Boot)** | Spring MVC applications still needed an external Tomcat — Spring Boot fixed this |

---

#### How Spring MVC Applications Were Deployed

Before Spring Boot, deploying a Spring MVC app involved the same WAR + external Tomcat process as raw servlets — just with a `DispatcherServlet` registered instead of many servlets.

##### Step 1 — `pom.xml` with WAR packaging

```xml
<project>
    <groupId>com.example</groupId>
    <artifactId>spring-mvc-app</artifactId>
    <version>1.0.0</version>
    <packaging>war</packaging>     <!-- WAR, not JAR -->

    <dependencies>
        <!-- Spring MVC -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
            <version>5.3.27</version>
        </dependency>

        <!-- Servlet API — provided by Tomcat, NOT bundled in WAR -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>5.0.0</version>
            <scope>provided</scope>
        </dependency>

        <!-- Jackson for JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.2</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven WAR plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.3.2</version>
                <!-- failOnMissingWebXml=false → OK without web.xml
                     (using Java-based WebAppInitializer instead) -->
                <configuration>
                    <failOnMissingWebXml>false</failOnMissingWebXml>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

##### Step 2 — Project Structure

```text
spring-mvc-app/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/example/
        │       ├── config/
        │       │   ├── AppConfig.java          ← @Configuration + @ComponentScan
        │       │   ├── WebConfig.java          ← @EnableWebMvc + MVC config
        │       │   └── WebAppInitializer.java  ← Registers DispatcherServlet
        │       ├── controller/
        │       │   └── UserController.java
        │       ├── service/
        │       │   └── UserService.java
        │       └── repository/
        │           └── UserRepository.java
        └── resources/
            └── application.properties          ← DB config, etc.
```

No `web.xml` — the `WebAppInitializer` (implementing `WebApplicationInitializer`) replaces it.

Tomcat discovers it via **SPI** (Java's `ServiceLoader`): Spring's JAR includes a `META-INF/services/jakarta.servlet.ServletContainerInitializer` file pointing to `SpringServletContainerInitializer`, which in turn calls all `WebApplicationInitializer` implementations at startup.

##### Step 3 — Build and Deploy

```bash
# Build WAR
mvn clean package
# → target/spring-mvc-app-1.0.0.war

# Deploy to existing Tomcat installation
cp target/spring-mvc-app-1.0.0.war /opt/tomcat/webapps/

# Start Tomcat (if not running)
/opt/tomcat/bin/startup.sh

# App available at:
# http://localhost:8080/spring-mvc-app-1.0.0/users
# http://localhost:8080/spring-mvc-app-1.0.0/users/42
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring MVC Deployment Flow (Pre-Spring Boot):                                   │
│                                                                                  │
│  Source Code                                                                     │
│  ──────────                                                                      │
│  @Controller classes                                                             │
│  @Configuration classes            mvn clean package                            │
│  WebAppInitializer            ──────────────────────►  spring-mvc-app.war        │
│  pom.xml (packaging=war)                                        │                │
│                                                                 │                │
│                                                                 ▼                │
│                                                   cp to /opt/tomcat/webapps/     │
│                                                                 │                │
│                                                                 ▼                │
│                                                        Tomcat startup.sh         │
│                                                                 │                │
│                                                                 ▼                │
│                                              Tomcat finds SpringServletContainer │
│                                              Initializer via SPI →               │
│                                              calls WebAppInitializer.onStartup() │
│                                                                 │                │
│                                                                 ▼                │
│                                              DispatcherServlet registered        │
│                                              mapped to "/"                       │
│                                                                 │                │
│                                                                 ▼                │
│                                              Spring context loads:               │
│                                              @Configuration scanned              │
│                                              @Component/@Service/@Repository     │
│                                              beans created and wired             │
│                                                                 │                │
│                                                                 ▼                │
│                                              App live:                           │
│                                              http://localhost:8080/myapp/users   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Complete Request Flow in Spring MVC

This is the most important diagram to understand for Spring MVC internals:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring MVC Request Lifecycle:                                                   │
│                                                                                  │
│  Browser / REST Client                                                           │
│        │                                                                         │
│        │  HTTP GET /users/42                                                      │
│        ▼                                                                         │
│  ┌──────────────────────────────────────────────────────────────────────────┐    │
│  │                   Apache Tomcat (Servlet Container)                      │    │
│  │                                                                          │    │
│  │  1. TCP connection accepted                                               │    │
│  │  2. HTTP bytes parsed → HttpServletRequest, HttpServletResponse created  │    │
│  │  3. URL "/users/42" → mapped to DispatcherServlet (registered at "/")   │    │
│  │                                                                          │    │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │    │
│  │  │                    DispatcherServlet                               │  │    │
│  │  │                    (Spring's Front Controller)                     │  │    │
│  │  │                                                                    │  │    │
│  │  │  4. Consult HandlerMapping                                         │  │    │
│  │  │     → "Which handler handles GET /users/42?"                       │  │    │
│  │  │     → Returns: UserController.getById() + interceptors             │  │    │
│  │  │                                                                    │  │    │
│  │  │  5. Consult HandlerAdapter                                         │  │    │
│  │  │     → "How do I call this handler?"                                │  │    │
│  │  │     → RequestMappingHandlerAdapter                                 │  │    │
│  │  │       - Resolves @PathVariable id = 42                             │  │    │
│  │  │       - Resolves @RequestHeader, @RequestParam etc.                │  │    │
│  │  │                                                                    │  │    │
│  │  │  6. Execute Interceptors (pre-handle)                              │  │    │
│  │  │     → HandlerInterceptor.preHandle()                               │  │    │
│  │  │       (logging, auth checks, rate limiting, etc.)                  │  │    │
│  │  │                                                                    │  │    │
│  │  │  7. Call handler method:                                           │  │    │
│  │  │     UserController.getById(42)                                     │  │    │
│  │  │       → userService.findById(42)                                   │  │    │
│  │  │         → userRepository.findById(42)  (DB query)                  │  │    │
│  │  │       ← returns User object                                        │  │    │
│  │  │                                                                    │  │    │
│  │  │  8. Execute Interceptors (post-handle)                             │  │    │
│  │  │     → HandlerInterceptor.postHandle()                              │  │    │
│  │  │                                                                    │  │    │
│  │  │  9. Resolve return value                                           │  │    │
│  │  │     @RestController → @ResponseBody implicit                       │  │    │
│  │  │     → HttpMessageConverter (Jackson) converts User → JSON          │  │    │
│  │  │     → Writes to HttpServletResponse output stream                  │  │    │
│  │  │                                                                    │  │    │
│  │  │  10. Execute Interceptors (after-completion)                       │  │    │
│  │  │      → HandlerInterceptor.afterCompletion()                        │  │    │
│  │  │                                                                    │  │    │
│  │  └────────────────────────────────────────────────────────────────────┘  │    │
│  │                                                                          │    │
│  │  11. Tomcat serializes response → TCP → Browser                          │    │
│  └──────────────────────────────────────────────────────────────────────────┘    │
│        │                                                                         │
│        │  HTTP 200 OK                                                             │
│        │  Content-Type: application/json                                          │
│        │  {"id":42,"name":"Alice","email":"alice@example.com"}                    │
│        ▼                                                                         │
│  Browser / REST Client                                                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### The Key Components Explained

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring MVC Key Components:                                                      │
│                                                                                  │
│  Component                Role                                                   │
│  ─────────────────────────────────────────────────────────────────────────────   │
│  DispatcherServlet         The ONE servlet. Entry point for all requests.         │
│                            Delegates to all other components.                    │
│                                                                                  │
│  HandlerMapping            Maps URL + HTTP method → handler (controller method)  │
│                            Default: RequestMappingHandlerMapping                 │
│                            (scans @RequestMapping annotations)                   │
│                                                                                  │
│  HandlerAdapter            Knows HOW to call the handler.                        │
│                            Resolves method arguments (@PathVariable,             │
│                            @RequestParam, @RequestBody, HttpSession, etc.)       │
│                                                                                  │
│  HandlerInterceptor        Pre/post processing around handler execution.         │
│                            Used for: logging, authentication, rate limiting,     │
│                            CORS, MDC population.                                 │
│                                                                                  │
│  HttpMessageConverter      Converts between HTTP body ↔ Java objects.            │
│                            MappingJackson2HttpMessageConverter: JSON ↔ Object    │
│                            Jaxb2RootElementHttpMessageConverter: XML ↔ Object    │
│                            StringHttpMessageConverter: String ↔ text/plain       │
│                                                                                  │
│  ViewResolver              (For MVC, not REST) Resolves logical view name        │
│                            ("home") to actual view template                      │
│                            (ThymeleafViewResolver, InternalResourceViewResolver) │
│                                                                                  │
│  HandlerExceptionResolver  Handles exceptions thrown during handler execution.   │
│                            @ExceptionHandler / @ControllerAdvice uses this.      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Request Flow — Simplified Sequence

```text
Request: GET /users/42

DispatcherServlet
    │
    ├─[1]─► HandlerMapping.getHandler("/users/42")
    │        └─► Returns: HandlerExecutionChain {
    │                handler: UserController#getById,
    │                interceptors: [LoggingInterceptor, AuthInterceptor]
    │            }
    │
    ├─[2]─► HandlerAdapter.supports(handler)
    │        └─► RequestMappingHandlerAdapter (yes, supports @RequestMapping)
    │
    ├─[3]─► LoggingInterceptor.preHandle()   → logs request
    │        AuthInterceptor.preHandle()      → checks JWT token
    │
    ├─[4]─► HandlerAdapter.handle(request, response, handler)
    │        ├─ Resolves @PathVariable id = 42L
    │        ├─ Calls UserController.getById(42L)
    │        │    └─ UserService.findById(42L)
    │        │         └─ UserRepository.findById(42L) → SELECT * FROM users WHERE id=42
    │        │    └─ returns User{id=42, name="Alice"}
    │        └─ Returns ModelAndView (or just the return value for @ResponseBody)
    │
    ├─[5]─► LoggingInterceptor.postHandle()
    │
    ├─[6]─► MessageConverter: User → JSON {"id":42,"name":"Alice"}
    │        Write to response OutputStream
    │
    └─[7]─► LoggingInterceptor.afterCompletion()
```

---

#### `@Configuration`, `@ComponentScan`, `@EnableWebMvc`, and `AppConfig`

These four annotations together replaced `web.xml` and Spring XML configuration files entirely.

##### `@Configuration`

Marks a class as a **source of bean definitions** — equivalent to a Spring XML `<beans>` file. Methods annotated with `@Bean` produce bean instances managed by the Spring container.

```java
// AppConfig.java
@Configuration   // "This class defines beans for the Spring container"
public class AppConfig {

    // @Bean: Spring calls this method and stores the returned object as a bean
    // Bean name defaults to method name: "dataSource"
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("root");
        config.setPassword("secret");
        config.setMaximumPoolSize(20);
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        // Spring injects the dataSource bean defined above
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

**`@Configuration` vs `@Component`:**
Both register the class as a bean, but `@Configuration` additionally proxies the class to ensure `@Bean` methods return the SAME singleton instance every time they are called — even when called directly in code.

```java
@Configuration
public class AppConfig {
    @Bean
    public A beanA() {
        return new A(beanC()); // calls beanC() — but Spring intercepts and returns singleton
    }

    @Bean
    public B beanB() {
        return new B(beanC()); // calls beanC() again — still the SAME instance returned
    }

    @Bean
    public C beanC() { return new C(); }
    // Without @Configuration (with just @Component), beanA and beanB would get DIFFERENT C instances!
}
```

##### `@ComponentScan`

Tells Spring **where to scan** for classes annotated with `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`, etc.

```java
// AppConfig.java
@Configuration
@ComponentScan(
    basePackages = "com.example",       // scan this package (and sub-packages)
    // OR
    basePackageClasses = AppConfig.class, // scan the package containing this class
    // Optional: exclusion filters
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = Controller.class       // exclude controllers (loaded by WebConfig)
    )
)
public class AppConfig {
    // No need to declare @Bean for UserService, OrderService, etc.
    // Spring finds them by scanning com.example and its sub-packages
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @ComponentScan — What Gets Picked Up:                                           │
│                                                                                  │
│  @ComponentScan("com.example")                                                   │
│                                                                                  │
│  com.example/                                                                    │
│  ├── controller/                                                                 │
│  │   └── UserController.java    ← @RestController (@Controller + @ResponseBody)  │
│  ├── service/                                                                    │
│  │   └── UserService.java       ← @Service  (alias for @Component)               │
│  ├── repository/                                                                 │
│  │   └── UserRepository.java    ← @Repository (alias for @Component + exception  │
│  │                                 translation)                                  │
│  └── config/                                                                     │
│      └── AppConfig.java         ← @Configuration (picked up if itself scanned)  │
│                                                                                  │
│  All annotated classes → instantiated as singletons → wired together             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### `@EnableWebMvc`

Activates Spring MVC's full annotation-driven web configuration — equivalent to `<mvc:annotation-driven />` in XML config. It registers the following beans automatically:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What @EnableWebMvc registers:                                                   │
│                                                                                  │
│  Bean                                  Purpose                                   │
│  ────────────────────────────────────────────────────────────────────────────    │
│  RequestMappingHandlerMapping          Scans @RequestMapping methods             │
│  RequestMappingHandlerAdapter          Calls those methods, resolves args        │
│  ExceptionHandlerExceptionResolver     Handles @ExceptionHandler methods         │
│  ResponseStatusExceptionResolver       Handles @ResponseStatus                   │
│  DefaultHandlerExceptionResolver       Handles Spring MVC standard exceptions    │
│  MappingJackson2HttpMessageConverter   JSON ↔ Object conversion (if Jackson JAR  │
│                                        is on classpath)                          │
│  ConversionService                     Type conversion (@PathVariable String→Long)│
│  Validator                             JSR-303 validation (@Valid)               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// WebConfig.java — the Spring MVC configuration class
@Configuration
@EnableWebMvc           // "Enable Spring MVC annotation-driven mode"
@ComponentScan("com.example.controller")   // scan controllers
public class WebConfig implements WebMvcConfigurer {
    // WebMvcConfigurer lets you customize the MVC config without replacing it entirely

    // Register custom interceptors
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor())
                .addPathPatterns("/api/**");
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/public/**");
    }

    // Configure CORS
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://myfrontend.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    // Configure static resource handling (CSS, JS, images)
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("/WEB-INF/static/");
    }

    // Register view resolver (for traditional MVC with HTML views)
    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    // Customize message converters (e.g., configure Jackson ObjectMapper)
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
    }
}
```

##### The Two-Context Architecture

Spring MVC pre-Boot used **two separate application contexts**:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring MVC — Two Application Context Architecture:                              │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │       Root ApplicationContext  (AppConfig.java)                           │  │
│  │                                                                            │  │
│  │  Contains: Services, Repositories, DataSources, Transactions, Security    │  │
│  │  Shared across ALL servlets in the app                                     │  │
│  │                                                                            │  │
│  │  @Configuration @ComponentScan("com.example.service", "com.example.repo") │  │
│  │  public class AppConfig { ... }                                            │  │
│  └──────────────────────────────────────────┬──────────────────────────────────┘  │
│                                             │  parent context                    │
│                                             │  (child can see parent's beans)    │
│  ┌──────────────────────────────────────────▼──────────────────────────────────┐  │
│  │       Web ApplicationContext  (WebConfig.java)                             │  │
│  │                                                                            │  │
│  │  Contains: Controllers, ViewResolvers, HandlerMappings, Interceptors       │  │
│  │  Scoped to the DispatcherServlet                                            │  │
│  │                                                                            │  │
│  │  @Configuration @EnableWebMvc @ComponentScan("com.example.controller")    │  │
│  │  public class WebConfig implements WebMvcConfigurer { ... }                │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Why two contexts?                                                               │
│  Multiple DispatcherServlets can share the same root context.                    │
│  E.g., one DispatcherServlet for /api/**, another for /admin/**                  │
│  Both share services/repos but have independent web configs.                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Complete Configuration Example

```java
// ── 1. WebAppInitializer.java — replaces web.xml ────────────────────────────────
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    // Root context beans (services, repos, data sources)
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[] { AppConfig.class };
    }

    // Web context beans (controllers, view resolvers)
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] { WebConfig.class };
    }

    // DispatcherServlet handles all URLs
    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    // Optional: add filters (e.g., character encoding)
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return new Filter[] { filter };
    }
}


// ── 2. AppConfig.java — root application context ─────────────────────────────────
@Configuration
@ComponentScan(basePackages = {"com.example.service", "com.example.repository"})
@EnableTransactionManagement    // enables @Transactional
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Value("${db.url}")    private String dbUrl;
    @Value("${db.user}")   private String dbUser;
    @Value("${db.pass}")   private String dbPass;

    @Bean
    public DataSource dataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(dbUrl);
        cfg.setUsername(dbUser);
        cfg.setPassword(dbPass);
        return new HikariDataSource(cfg);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource ds) {
        LocalContainerEntityManagerFactoryBean emf =
            new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setPackagesToScan("com.example.model");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        props.put("hibernate.hbm2ddl.auto", "update");
        emf.setJpaProperties(props);
        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(
            EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}


// ── 3. WebConfig.java — web application context ──────────────────────────────────
@Configuration
@EnableWebMvc
@ComponentScan("com.example.controller")
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor()).addPathPatterns("/**");
    }

    @Override
    public void configureDefaultServletHandling(
            DefaultServletHandlerConfigurer configurer) {
        configurer.enable();  // serve static files via DefaultServlet
    }
}


// ── 4. UserController.java ────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestBody @Valid User user) {
        return userService.save(user);
    }
}


// ── 5. UserService.java ───────────────────────────────────────────────────────────
@Service                     // @Component + semantic meaning "service layer"
@Transactional(readOnly = true)   // all methods read-only by default
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional   // override: this method writes
    public User save(User user) {
        return userRepository.save(user);
    }
}
```

##### How Spring Boot Changed Everything

Spring Boot (2014) eliminated the remaining boilerplate:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring MVC (Pre-Boot) vs Spring Boot:                                           │
│                                                                                  │
│  Spring MVC (Pre-Boot)                Spring Boot                                │
│  ─────────────────────                ───────────                                │
│  packaging = war                      packaging = jar (fat JAR)                  │
│  External Tomcat required             Embedded Tomcat (no install needed)        │
│  WebAppInitializer required           Just main() + @SpringBootApplication        │
│  AppConfig + WebConfig needed         Auto-configuration (smart defaults)        │
│  DataSource @Bean required            Set db.url in application.properties       │
│  Jackson @Bean or configurer          Jackson auto-configured if on classpath    │
│  Deploy: copy WAR to webapps/         Deploy: java -jar app.jar                  │
│  Restart to update                    Spring DevTools hot-reload                 │
│                                                                                  │
│  Spring Boot entry point:                                                        │
│                                                                                  │
│  @SpringBootApplication    // = @Configuration + @ComponentScan + @EnableAutoConfiguration
│  public class Application {                                                      │
│      public static void main(String[] args) {                                    │
│          SpringApplication.run(Application.class, args);                         │
│          // Starts embedded Tomcat, loads Spring context, deploys DispatcherServlet
│      }                                                                           │
│  }                                                                               │
│                                                                                  │
│  The same UserController, UserService, UserRepository work identically.          │
│  Only the bootstrap/config wiring changes.                                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

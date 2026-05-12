


### Spring Boot Annotations — Complete Reference

---

#### 1. `@SpringBootApplication`

A **meta-annotation** (an annotation composed of other annotations) that serves as the single entry point for a Spring Boot application. It combines three annotations into one:

```java
// What @SpringBootApplication actually is (Spring Boot source):
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration       // → wraps @Configuration
@EnableAutoConfiguration       // → triggers auto-configuration
@ComponentScan                 // → scans for @Component stereotypes
public @interface SpringBootApplication {

    @AliasFor(annotation = EnableAutoConfiguration.class)
    Class<?>[] exclude() default {};

    @AliasFor(annotation = EnableAutoConfiguration.class)
    String[] excludeName() default {};

    @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
    String[] scanBasePackages() default {};

    @AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
    Class<?>[] scanBasePackageClasses() default {};

    @AliasFor(annotation = Configuration.class)
    boolean proxyBeanMethods() default true;
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @SpringBootApplication = 3 Annotations Combined:                                │
│                                                                                  │
│  @SpringBootApplication                                                          │
│      │                                                                           │
│      ├── @Configuration  (via @SpringBootConfiguration)                         │
│      │     → Marks the class as a source of @Bean definitions                   │
│      │     → Enables CGLIB proxying so @Bean methods return singletons          │
│      │     → Equivalent to an XML <beans> configuration file                    │
│      │                                                                           │
│      ├── @EnableAutoConfiguration                                                │
│      │     → Tells Spring Boot to automatically configure beans                 │
│      │       based on JARs on the classpath + properties you've set             │
│      │     → Reads META-INF/spring/                                              │
│      │       org.springframework.boot.autoconfigure.AutoConfiguration.imports   │
│      │     → Each auto-config class is guarded by @Conditional annotations:     │
│      │       @ConditionalOnClass → only if class exists on classpath            │
│      │       @ConditionalOnMissingBean → only if you haven't defined one        │
│      │       @ConditionalOnProperty → only if property is set                   │
│      │     → ~150 auto-config classes evaluated at startup                      │
│      │                                                                           │
│      └── @ComponentScan                                                          │
│            → Scans the package of the annotated class + ALL sub-packages        │
│            → Finds and registers beans annotated with:                          │
│              @Component, @Service, @Repository, @Controller,                    │
│              @RestController, @Configuration, @Aspect                           │
│            → Default: scans from the package where @SpringBootApplication lives │
│            → Customisable via scanBasePackages attribute                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Properties / Attributes

```text
┌─────────────────────────┬───────────────────────────────────────────────────────┐
│ Attribute               │ Description                                           │
├─────────────────────────┼───────────────────────────────────────────────────────┤
│ exclude                 │ Auto-config classes to exclude (by class reference)   │
│ excludeName             │ Auto-config classes to exclude (by fully-qualified    │
│                         │ class name string)                                    │
│ scanBasePackages        │ Base packages to scan for components (overrides       │
│                         │ default of scanning from annotated class's package)   │
│ scanBasePackageClasses  │ Type-safe alternative — scans packages of given       │
│                         │ classes                                                │
│ proxyBeanMethods        │ Whether to proxy @Bean methods with CGLIB (default:   │
│                         │ true). Set false for "lite" mode (no inter-bean       │
│                         │ references, faster startup)                           │
└─────────────────────────┴───────────────────────────────────────────────────────┘
```

##### Code Examples

```java
// ── Basic usage ──────────────────────────────────────────────────────────────────
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
// Scans: com.example.** (assuming MyApplication is in com.example)
// Auto-configures: everything it can detect on the classpath
// Configuration: this class can define @Bean methods


// ── Exclude unwanted auto-configuration ──────────────────────────────────────────
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,     // don't auto-configure DB
    SecurityAutoConfiguration.class        // don't auto-configure Spring Security
})
public class MyApplication { ... }


// ── Custom component scan packages ───────────────────────────────────────────────
@SpringBootApplication(scanBasePackages = {
    "com.example.web",
    "com.example.service",
    "com.example.repository"
    // com.example.legacy is intentionally NOT scanned
})
public class MyApplication { ... }


// ── Type-safe base package selection ─────────────────────────────────────────────
@SpringBootApplication(scanBasePackageClasses = {
    UserController.class,    // scan the package containing UserController
    OrderService.class       // scan the package containing OrderService
})
public class MyApplication { ... }


// ── Lite mode (no CGLIB proxy — faster startup) ──────────────────────────────────
@SpringBootApplication(proxyBeanMethods = false)
public class MyApplication {
    // @Bean methods in this class are NOT proxied
    // Inter-bean references (calling one @Bean method from another) will NOT
    // return the singleton — each call creates a new instance
    // Use this only when you don't have inter-bean dependencies
}
```

##### What Happens When `@SpringBootApplication` Runs

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  SpringApplication.run(MyApplication.class, args)                                │
│      │                                                                           │
│      ▼                                                                           │
│  1. Create ApplicationContext                                                    │
│     (AnnotationConfigServletWebServerApplicationContext for web apps)            │
│      │                                                                           │
│      ▼                                                                           │
│  2. @ComponentScan fires                                                         │
│     → Scans com.example.** for @Component stereotypes                           │
│     → Registers: UserController, UserService, UserRepository, etc.              │
│      │                                                                           │
│      ▼                                                                           │
│  3. @Configuration fires                                                         │
│     → MyApplication is itself a @Configuration class                            │
│     → Any @Bean methods in it are processed                                     │
│     → CGLIB proxy created (unless proxyBeanMethods=false)                       │
│      │                                                                           │
│      ▼                                                                           │
│  4. @EnableAutoConfiguration fires                                               │
│     → AutoConfigurationImportSelector reads AutoConfiguration.imports            │
│     → ~150 candidates evaluated against @Conditional checks                     │
│     → Matching ones activated (e.g., DataSourceAutoConfiguration,               │
│       WebMvcAutoConfiguration, JacksonAutoConfiguration)                        │
│      │                                                                           │
│      ▼                                                                           │
│  5. All beans instantiated, wired, proxied                                       │
│      │                                                                           │
│      ▼                                                                           │
│  6. Embedded Tomcat started → app is live                                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 1a. `@Configuration`

Marks a class as a **bean definition source** — the Java-based equivalent of an XML `<beans>` file. Any `@Bean` methods inside it are processed by the IoC container.

```java
// Source definition (simplified):
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component    // ← it IS a @Component, so it's also auto-detected by @ComponentScan
public @interface Configuration {
    boolean proxyBeanMethods() default true;
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Configuration — CGLIB Proxy Behaviour:                                         │
│                                                                                  │
│  When proxyBeanMethods = true (DEFAULT):                                         │
│  Spring creates a CGLIB subclass of your @Configuration class.                  │
│  Any call to a @Bean method is intercepted — the container checks if            │
│  the bean already exists in the singleton cache. If yes, returns the            │
│  cached instance. If no, calls the real method and caches the result.           │
│                                                                                  │
│  @Configuration                                                                  │
│  public class AppConfig {                                                        │
│      @Bean                                                                       │
│      public DataSource dataSource() {                                           │
│          return new HikariDataSource();                                          │
│      }                                                                           │
│                                                                                  │
│      @Bean                                                                       │
│      public JdbcTemplate jdbcTemplate() {                                        │
│          return new JdbcTemplate(dataSource());                                  │
│          //                      ^^^^^^^^^^^                                    │
│          // This does NOT create a second DataSource.                            │
│          // The CGLIB proxy intercepts the call and returns the SAME singleton. │
│      }                                                                           │
│  }                                                                               │
│                                                                                  │
│  When proxyBeanMethods = false ("lite" mode):                                    │
│  No CGLIB proxy. Calling dataSource() from jdbcTemplate() creates a NEW         │
│  DataSource instance — NOT the singleton. Use only when @Bean methods don't     │
│  call each other.                                                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Full example ─────────────────────────────────────────────────────────────────
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }
}


// ── Multiple @Configuration classes are allowed ──────────────────────────────────
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users", "products");
    }
}
```

---

##### 1b. `@EnableAutoConfiguration`

Tells Spring Boot to automatically configure beans based on what's on the classpath. You almost never use this directly — `@SpringBootApplication` includes it.

```java
// Source (simplified):
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
    Class<?>[] exclude() default {};
    String[] excludeName() default {};
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How Auto-Configuration Works:                                                   │
│                                                                                  │
│  1. AutoConfigurationImportSelector reads:                                       │
│     META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports│
│     (in spring-boot-autoconfigure.jar)                                          │
│                                                                                  │
│  2. File lists ~150 auto-config classes like:                                    │
│     org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration     │
│     org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration  │
│     org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration│
│     ...                                                                          │
│                                                                                  │
│  3. Each class has @Conditional guards:                                          │
│                                                                                  │
│     @Configuration                                                               │
│     @ConditionalOnClass(DataSource.class)        ← only if JDBC driver exists  │
│     @ConditionalOnMissingBean(DataSource.class)  ← only if YOU haven't defined │
│     public class DataSourceAutoConfiguration {                                  │
│         @Bean                                                                    │
│         public DataSource dataSource(DataSourceProperties p) { ... }            │
│     }                                                                            │
│                                                                                  │
│  4. Result: add a JAR → Spring Boot auto-configures it.                         │
│     You disagree? Define your own @Bean → Boot backs off.                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Exclude auto-config you don't need:
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)

// Or via properties:
// spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

##### 1c. `@ComponentScan`

Tells Spring where to look for classes annotated with `@Component` and its stereotypes (`@Service`, `@Repository`, `@Controller`, `@RestController`, `@Configuration`).

```java
// Source (simplified):
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ComponentScans.class)
public @interface ComponentScan {
    String[] value() default {};                       // alias for basePackages
    String[] basePackages() default {};                // packages to scan
    Class<?>[] basePackageClasses() default {};        // type-safe alternative
    boolean useDefaultFilters() default true;          // scan @Component stereotypes
    ComponentScan.Filter[] includeFilters() default {};// additional include rules
    ComponentScan.Filter[] excludeFilters() default {};// exclude rules
    boolean lazyInit() default false;                  // lazy-init all scanned beans
}
```

```java
// ── Default: scan from annotated class's package ─────────────────────────────────
// If MyApplication is in com.example, scans:
// com.example, com.example.controller, com.example.service, etc.
@SpringBootApplication  // includes @ComponentScan with default settings
public class MyApplication { ... }


// ── Explicitly specify packages ──────────────────────────────────────────────────
@ComponentScan(basePackages = {
    "com.example.web",
    "com.example.service",
    "com.thirdparty.shared"    // can scan outside your base package too
})


// ── Exclude specific components ──────────────────────────────────────────────────
@ComponentScan(
    basePackages = "com.example",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = LegacyService.class       // don't register this bean
    )
)


// ── Include only specific annotations ────────────────────────────────────────────
@ComponentScan(
    basePackages = "com.example",
    useDefaultFilters = false,    // DON'T auto-detect @Component stereotypes
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = RestController.class    // ONLY scan @RestController classes
    )
)


// ── Filter by regex pattern ──────────────────────────────────────────────────────
@ComponentScan(
    basePackages = "com.example",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.example\\.test\\..*"   // exclude test package
    )
)
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @ComponentScan — What Gets Scanned:                                             │
│                                                                                  │
│  com.example/                                                                    │
│  ├── MyApplication.java          ← @SpringBootApplication here                 │
│  ├── controller/                                                                 │
│  │   ├── UserController.java     ← @RestController → SCANNED ✓                 │
│  │   └── OrderController.java    ← @RestController → SCANNED ✓                 │
│  ├── service/                                                                    │
│  │   └── UserService.java        ← @Service → SCANNED ✓                        │
│  ├── repository/                                                                 │
│  │   └── UserRepository.java     ← @Repository → SCANNED ✓                     │
│  ├── config/                                                                     │
│  │   └── SecurityConfig.java     ← @Configuration → SCANNED ✓                  │
│  └── model/                                                                      │
│      └── User.java               ← No annotation → NOT SCANNED (plain POJO)    │
│                                                                                  │
│  com.other/                                                                      │
│  └── SharedService.java          ← NOT SCANNED (different root package)         │
│                                     Unless you add it to scanBasePackages       │
│                                                                                  │
│  IMPORTANT: @SpringBootApplication should be in the ROOT package.               │
│  If it's in com.example.web, it won't scan com.example.service!                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 2. `@Controller`

Marks a class as a **Spring MVC controller** — a component that handles HTTP requests and typically returns **view names** (HTML templates rendered by Thymeleaf, FreeMarker, etc.).

```java
// Source:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component    // ← it IS a @Component, so @ComponentScan detects it
public @interface Controller {
    String value() default "";   // optional bean name
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Controller — How It Works:                                                     │
│                                                                                  │
│  1. @ComponentScan detects it (because @Controller is a @Component)             │
│  2. IoC container creates a singleton bean                                       │
│  3. RequestMappingHandlerMapping scans it for @RequestMapping methods            │
│  4. Methods return a String → interpreted as a VIEW NAME                        │
│     → ViewResolver resolves it to an actual template file                       │
│     → Template engine renders HTML → sent to client                             │
│                                                                                  │
│  Key difference from @RestController:                                            │
│  @Controller → returns VIEW NAMES by default (server-side rendered HTML)        │
│  @RestController → returns RESPONSE BODY by default (JSON/XML for APIs)         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── @Controller — returns view names (Thymeleaf, FreeMarker, etc.) ───────────────
@Controller
@RequestMapping("/web")
public class UserWebController {

    private final UserService userService;

    public UserWebController(UserService userService) {
        this.userService = userService;
    }

    // Returns a VIEW NAME — Thymeleaf resolves to templates/users/list.html
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<UserResponse> users = userService.findAll();
        model.addAttribute("users", users);    // pass data to the template
        return "users/list";                    // → templates/users/list.html
    }

    // Show form for creating a new user
    @GetMapping("/users/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new UserRequest());
        return "users/create";                 // → templates/users/create.html
    }

    // Process form submission
    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("user") UserRequest request,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "users/create";             // re-render form with validation errors
        }
        userService.create(request);
        redirectAttributes.addFlashAttribute("message", "User created!");
        return "redirect:/web/users";          // redirect after POST (PRG pattern)
    }

    // Return JSON from a @Controller (add @ResponseBody to specific methods)
    @GetMapping("/api/users")
    @ResponseBody                              // ← overrides view resolution for THIS method
    public List<UserResponse> listUsersApi() {
        return userService.findAll();          // returns JSON, not a view name
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Controller Request Flow (server-side rendering):                               │
│                                                                                  │
│  GET /web/users                                                                  │
│      │                                                                           │
│      ▼                                                                           │
│  DispatcherServlet → HandlerMapping                                              │
│      → finds UserWebController#listUsers(Model)                                 │
│      │                                                                           │
│      ▼                                                                           │
│  HandlerAdapter invokes method                                                   │
│      → model.addAttribute("users", [...])                                       │
│      → returns "users/list" (String — view name)                                │
│      │                                                                           │
│      ▼                                                                           │
│  ViewResolver resolves view name                                                 │
│      → ThymeleafViewResolver: "users/list" → templates/users/list.html          │
│      │                                                                           │
│      ▼                                                                           │
│  View.render(model, request, response)                                           │
│      → Thymeleaf processes list.html template with model data                   │
│      → Generates HTML string                                                    │
│      │                                                                           │
│      ▼                                                                           │
│  HTML written to HttpServletResponse                                             │
│      → Content-Type: text/html                                                  │
│      → Client receives rendered HTML page                                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 3. `@RestController`

A **meta-annotation** that combines `@Controller` + `@ResponseBody`. Every method in a `@RestController` returns the **response body directly** (serialised to JSON/XML), not a view name.

```java
// Source:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Controller                // ← it IS a @Controller
@ResponseBody              // ← EVERY method has @ResponseBody implicitly
public @interface RestController {
    String value() default "";   // optional bean name
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Controller vs @RestController:                                                 │
│                                                                                  │
│  @Controller:                                                                    │
│  ┌──────────────────────────────────────┐                                       │
│  │ @Controller                          │                                       │
│  │ public class UserWebController {     │                                       │
│  │     @GetMapping("/users")            │                                       │
│  │     public String list(Model m) {    │ ← returns VIEW NAME "users/list"     │
│  │         m.addAttribute("users",...); │                                       │
│  │         return "users/list";         │ → ViewResolver → Thymeleaf → HTML    │
│  │     }                                │                                       │
│  │ }                                    │                                       │
│  └──────────────────────────────────────┘                                       │
│                                                                                  │
│  @RestController:                                                                │
│  ┌──────────────────────────────────────────┐                                   │
│  │ @RestController                          │                                   │
│  │ public class UserApiController {         │                                   │
│  │     @GetMapping("/api/users")            │                                   │
│  │     public List<UserResponse> list() {   │ ← returns OBJECT (not view name) │
│  │         return userService.findAll();    │ → Jackson → JSON response body   │
│  │     }                                    │                                   │
│  │ }                                        │                                   │
│  └──────────────────────────────────────────┘                                   │
│                                                                                  │
│  @RestController = @Controller + @ResponseBody on every method                  │
│  You do NOT need @ResponseBody on individual methods.                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Full @RestController example ─────────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    // Constructor injection (no @Autowired needed — single constructor)
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/v1/users → returns JSON array
    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll();
        // Jackson serialises List<UserResponse> → JSON array
        // Content-Type: application/json (auto-set by Spring)
    }

    // GET /api/v1/users/42 → returns JSON object
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.findById(id);
        // Jackson serialises UserResponse → JSON object
    }

    // POST /api/v1/users → returns JSON object with 201 status
    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody @Valid UserRequest request) {
        UserResponse created = userService.create(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
        return ResponseEntity.created(location).body(created);
        // Status: 201 Created
        // Location: /api/v1/users/42
        // Body: {"id":42,"name":"Alice",...}
    }

    // PUT /api/v1/users/42 → returns updated JSON object
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id,
                               @RequestBody @Valid UserRequest request) {
        return userService.update(id, request);
    }

    // DELETE /api/v1/users/42 → returns 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();   // 204, no body
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @RestController Request Flow (API):                                             │
│                                                                                  │
│  GET /api/v1/users/42                                                            │
│      │                                                                           │
│      ▼                                                                           │
│  DispatcherServlet → HandlerMapping                                              │
│      → finds UserController#getById(Long)                                       │
│      │                                                                           │
│      ▼                                                                           │
│  HandlerAdapter invokes method                                                   │
│      → resolves @PathVariable id = 42L                                          │
│      → calls userService.findById(42L)                                          │
│      → returns UserResponse object                                              │
│      │                                                                           │
│      ▼                                                                           │
│  Return Value Handling (@ResponseBody is active):                                │
│      → NO ViewResolver involved (skipped entirely)                              │
│      → Content negotiation: Accept: application/json                            │
│      → Selects MappingJackson2HttpMessageConverter                              │
│      → Jackson ObjectMapper.writeValueAsBytes(userResponse)                     │
│      → Writes JSON to HttpServletResponse.getOutputStream()                     │
│      │                                                                           │
│      ▼                                                                           │
│  HTTP 200 OK                                                                     │
│  Content-Type: application/json                                                  │
│  {"id":42,"name":"Alice","email":"alice@example.com"}                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 4. `@ControllerAdvice` and `@ExceptionHandler`

##### `@ControllerAdvice`

A specialisation of `@Component` that allows you to define **global** cross-cutting concerns for all controllers — primarily **exception handling**, **model attributes**, and **data binding** — in a single centralised class instead of duplicating logic across every controller.

```java
// Source:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component     // ← auto-detected by @ComponentScan
public @interface ControllerAdvice {
    String[] value() default {};                  // alias for basePackages
    String[] basePackages() default {};           // limit to specific packages
    Class<?>[] basePackageClasses() default {};   // type-safe alternative
    Class<?>[] assignableTypes() default {};      // limit to specific controllers
    Class<? extends Annotation>[] annotations() default {};  // limit by annotation
}

// @RestControllerAdvice = @ControllerAdvice + @ResponseBody
// (returns JSON/XML instead of view names for exception responses)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ControllerAdvice
@ResponseBody
public @interface RestControllerAdvice { ... }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @ControllerAdvice — What It Can Do:                                             │
│                                                                                  │
│  ┌────────────────────────────────────┬──────────────────────────────────────┐   │
│  │ Feature                            │ Annotation Used Inside              │   │
│  ├────────────────────────────────────┼──────────────────────────────────────┤   │
│  │ Global exception handling          │ @ExceptionHandler                   │   │
│  │ Global model attributes            │ @ModelAttribute                     │   │
│  │ Global data binding customisation  │ @InitBinder                        │   │
│  └────────────────────────────────────┴──────────────────────────────────────┘   │
│                                                                                  │
│  Without @ControllerAdvice:                                                      │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐                │
│  │ UserController   │ │ OrderController  │ │ ProductController│                │
│  │ @ExceptionHandler│ │ @ExceptionHandler│ │ @ExceptionHandler│ ← DUPLICATED  │
│  │ @ExceptionHandler│ │ @ExceptionHandler│ │ @ExceptionHandler│               │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘                │
│                                                                                  │
│  With @ControllerAdvice:                                                         │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐                │
│  │ UserController   │ │ OrderController  │ │ ProductController│                │
│  │ (no exception    │ │ (no exception    │ │ (no exception    │                │
│  │  handling code)  │ │  handling code)  │ │  handling code)  │                │
│  └────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘                │
│           │                    │                      │                          │
│           └────────────────────┼──────────────────────┘                          │
│                                ▼                                                 │
│  ┌──────────────────────────────────────────────────┐                           │
│  │ GlobalExceptionHandler (@RestControllerAdvice)    │ ← CENTRALISED           │
│  │   @ExceptionHandler(UserNotFoundException.class)  │                           │
│  │   @ExceptionHandler(ValidationException.class)    │                           │
│  │   @ExceptionHandler(Exception.class)              │                           │
│  └──────────────────────────────────────────────────┘                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### `@ExceptionHandler`

Marks a method as a handler for a specific exception type. When any `@Controller` or `@RestController` method throws that exception, Spring invokes this handler instead of returning a generic error.

```java
// Source:
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExceptionHandler {
    Class<? extends Throwable>[] value() default {};   // exception types to handle
}
```

##### Properties / Attributes

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @ControllerAdvice Attributes:                                                   │
│                                                                                  │
│  ┌─────────────────────────┬─────────────────────────────────────────────────┐  │
│  │ Attribute               │ Description                                     │  │
│  ├─────────────────────────┼─────────────────────────────────────────────────┤  │
│  │ basePackages            │ Only apply to controllers in these packages     │  │
│  │ basePackageClasses      │ Type-safe alternative to basePackages           │  │
│  │ assignableTypes         │ Only apply to these specific controller classes │  │
│  │ annotations             │ Only apply to controllers with these annotations│  │
│  └─────────────────────────┴─────────────────────────────────────────────────┘  │
│                                                                                  │
│  @ExceptionHandler Attributes:                                                   │
│                                                                                  │
│  ┌─────────────────────────┬─────────────────────────────────────────────────┐  │
│  │ Attribute               │ Description                                     │  │
│  ├─────────────────────────┼─────────────────────────────────────────────────┤  │
│  │ value                   │ One or more exception classes to handle.        │  │
│  │                         │ If empty, inferred from the method parameter.  │  │
│  └─────────────────────────┴─────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Code Examples

```java
// ── Standard Error Response DTO ──────────────────────────────────────────────────
@Getter @Setter @AllArgsConstructor
public class ErrorResponse {
    private String    code;          // machine-readable error code
    private String    message;       // human-readable message
    private LocalDateTime timestamp; // when the error occurred
    private String    path;          // request path that caused the error

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}


// ── Global Exception Handler (most common pattern) ──────────────────────────────
@RestControllerAdvice    // @ControllerAdvice + @ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    // ── Handle specific custom exceptions ────────────────────────────────────
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)    // 404
    public ErrorResponse handleUserNotFound(UserNotFoundException ex,
                                            WebRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("USER_NOT_FOUND", ex.getMessage());
        error.setPath(((ServletWebRequest) request).getRequest().getRequestURI());
        return error;
        // Response: 404
        // {"code":"USER_NOT_FOUND","message":"User not found: 42",
        //  "timestamp":"2026-05-12T10:30:00","path":"/api/v1/users/42"}
    }

    // ── Handle multiple exception types in one method ────────────────────────
    @ExceptionHandler({DuplicateEmailException.class, DuplicateUsernameException.class})
    @ResponseStatus(HttpStatus.CONFLICT)     // 409
    public ErrorResponse handleDuplicate(RuntimeException ex) {
        return new ErrorResponse("DUPLICATE_RESOURCE", ex.getMessage());
    }

    // ── Handle validation errors (@Valid failures) ───────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 400
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        // Collect all field validation errors into a readable string
        String details = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return new ErrorResponse("VALIDATION_ERROR", details);
        // {"code":"VALIDATION_ERROR","message":"name: must not be blank; email: must be valid"}
    }

    // ── Handle type mismatch (e.g., /users/abc where Long expected) ──────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = String.format("Parameter '%s' should be of type '%s'",
            ex.getName(), ex.getRequiredType().getSimpleName());
        return new ErrorResponse("TYPE_MISMATCH", msg);
    }

    // ── Handle missing required request parameters ───────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParam(MissingServletRequestParameterException ex) {
        String msg = String.format("Required parameter '%s' of type '%s' is missing",
            ex.getParameterName(), ex.getParameterType());
        return new ErrorResponse("MISSING_PARAMETER", msg);
    }

    // ── Handle access denied (Spring Security) ───────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)    // 403
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return new ErrorResponse("ACCESS_DENIED", "You do not have permission");
    }

    // ── Catch-all for unexpected exceptions ──────────────────────────────────
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // 500
    public ErrorResponse handleAll(Exception ex) {
        log.error("Unexpected error", ex);
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        // NEVER expose ex.getMessage() — may leak internal details
        // NEVER expose stack traces to clients
    }
}
```

```java
// ── Scoped @ControllerAdvice — only for specific controllers ─────────────────────

// Only applies to controllers in the "api" package
@RestControllerAdvice(basePackages = "com.example.api")
public class ApiExceptionHandler { ... }

// Only applies to controllers annotated with @RestController
@ControllerAdvice(annotations = RestController.class)
public class RestOnlyExceptionHandler { ... }

// Only applies to specific controller classes
@ControllerAdvice(assignableTypes = {UserController.class, OrderController.class})
public class UserOrderExceptionHandler { ... }
```

```java
// ── @ControllerAdvice with @ModelAttribute (add data to ALL views) ───────────────
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("appVersion")
    public String appVersion() {
        return "2.5.0";    // available in ALL Thymeleaf templates as ${appVersion}
    }

    @ModelAttribute("currentUser")
    public UserResponse currentUser(@AuthenticationPrincipal UserDetails user) {
        // available in ALL templates as ${currentUser}
        return userService.findByUsername(user.getUsername());
    }
}


// ── @ControllerAdvice with @InitBinder (global data binding) ─────────────────────
@ControllerAdvice
public class GlobalBindingCustomizer {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Trim whitespace from all String inputs globally
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

        // Register custom date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Exception Handling Flow:                                                        │
│                                                                                  │
│  Client: GET /api/v1/users/999                                                   │
│      │                                                                           │
│      ▼                                                                           │
│  DispatcherServlet → HandlerMapping → UserController#getById(999L)              │
│      │                                                                           │
│      ▼                                                                           │
│  UserServiceImpl.findById(999L)                                                  │
│      → userRepository.findById(999L) → Optional.empty()                        │
│      → throw new UserNotFoundException("User not found: 999")                  │
│      │                                                                           │
│      ▼ (exception propagates up to DispatcherServlet)                           │
│                                                                                  │
│  DispatcherServlet.processHandlerException()                                     │
│      → Searches for @ExceptionHandler methods:                                  │
│        1. First: in the CONTROLLER class itself (local @ExceptionHandler)       │
│        2. Then: in @ControllerAdvice classes (global @ExceptionHandler)          │
│      → Finds: GlobalExceptionHandler#handleUserNotFound(...)                    │
│      → Invokes it with the exception                                            │
│      │                                                                           │
│      ▼                                                                           │
│  handleUserNotFound() returns ErrorResponse                                      │
│      → @ResponseStatus(404) sets HTTP status                                    │
│      → @ResponseBody (from @RestControllerAdvice) triggers JSON serialisation   │
│      → Jackson converts ErrorResponse → JSON                                   │
│      │                                                                           │
│      ▼                                                                           │
│  HTTP 404 Not Found                                                              │
│  Content-Type: application/json                                                  │
│  {"code":"USER_NOT_FOUND","message":"User not found: 999",                      │
│   "timestamp":"2026-05-12T10:30:00","path":"/api/v1/users/999"}                 │
│                                                                                  │
│  Exception Handler Priority (most specific wins):                                │
│  1. @ExceptionHandler(UserNotFoundException.class)  ← exact match              │
│  2. @ExceptionHandler(RuntimeException.class)       ← parent class             │
│  3. @ExceptionHandler(Exception.class)              ← catch-all                │
└──────────────────────────────────────────────────────────────────────────────────┘
```


---

#### 5. `@RequestMapping`

The **foundational** annotation for mapping HTTP requests to handler methods (or entire controllers). All other mapping annotations (`@GetMapping`, `@PostMapping`, etc.) are **specialisations** of `@RequestMapping`.

```java
// Source (simplified):
@Target({ElementType.TYPE, ElementType.METHOD})   // can go on class OR method
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String   name()       default "";                  // mapping name (for URI building)
    String[] value()      default {};                  // alias for path
    String[] path()       default {};                  // URL patterns
    RequestMethod[] method() default {};               // HTTP methods (GET, POST, etc.)
    String[] params()     default {};                  // required request parameters
    String[] headers()    default {};                  // required request headers
    String[] consumes()   default {};                  // Content-Type the method accepts
    String[] produces()   default {};                  // Content-Type the method produces
}
```

##### Properties / Attributes

```text
┌──────────────────────┬───────────────────────────────────────────────────────────┐
│ Attribute            │ Description                                               │
├──────────────────────┼───────────────────────────────────────────────────────────┤
│ value / path         │ URL pattern(s): "/users", "/users/{id}", "/api/**"       │
│                      │ Supports Ant-style wildcards and {pathVariable} templates │
│ method               │ HTTP method(s): GET, POST, PUT, DELETE, PATCH, etc.      │
│                      │ If omitted, matches ALL methods                           │
│ params               │ Narrows mapping by query parameter presence/value:       │
│                      │ "action=save" → only if ?action=save is present          │
│                      │ "!debug" → only if ?debug is NOT present                 │
│ headers              │ Narrows mapping by HTTP header presence/value:            │
│                      │ "X-API-Version=2" → only if that header matches          │
│ consumes             │ Content-Type the endpoint accepts:                         │
│                      │ "application/json", "multipart/form-data"                │
│ produces             │ Content-Type the endpoint returns:                         │
│                      │ "application/json", "text/html", "application/xml"       │
│ name                 │ Assign a name to this mapping (used for URI link building)│
└──────────────────────┴───────────────────────────────────────────────────────────┘
```

##### Code Examples

```java
// ── Class-level: sets base path for ALL methods in the controller ────────────────
@RestController
@RequestMapping("/api/v1/products")    // base path
public class ProductController {

    // GET /api/v1/products
    @RequestMapping(method = RequestMethod.GET)
    public List<Product> list() { ... }

    // GET /api/v1/products/42
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Product getById(@PathVariable Long id) { ... }

    // POST /api/v1/products
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json")
    public Product create(@RequestBody Product product) { ... }
}


// ── Multiple paths for same handler ──────────────────────────────────────────────
@RequestMapping(value = {"/users", "/members", "/people"}, method = RequestMethod.GET)
public List<User> listUsers() { ... }
// All three URLs invoke the same method


// ── Narrow by params ─────────────────────────────────────────────────────────────
@RequestMapping(value = "/search", method = RequestMethod.GET, params = "type=full")
public List<Product> fullSearch(@RequestParam String query) { ... }

@RequestMapping(value = "/search", method = RequestMethod.GET, params = "type=quick")
public List<Product> quickSearch(@RequestParam String query) { ... }
// GET /search?type=full&query=laptop → fullSearch()
// GET /search?type=quick&query=laptop → quickSearch()


// ── Narrow by headers ────────────────────────────────────────────────────────────
@RequestMapping(value = "/data", headers = "X-API-Version=1")
public DataV1 getDataV1() { ... }

@RequestMapping(value = "/data", headers = "X-API-Version=2")
public DataV2 getDataV2() { ... }


// ── consumes and produces ────────────────────────────────────────────────────────
@RequestMapping(
    value = "/upload",
    method = RequestMethod.POST,
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,   // only accepts file uploads
    produces = MediaType.APPLICATION_JSON_VALUE         // always returns JSON
)
public UploadResponse upload(@RequestParam("file") MultipartFile file) { ... }


// ── Ant-style path patterns ──────────────────────────────────────────────────────
@RequestMapping("/files/**")          // matches /files/a, /files/a/b/c, etc.
@RequestMapping("/users/*/orders")    // matches /users/42/orders, /users/abc/orders
@RequestMapping("/docs/{version:\\d+\\.\\d+}")  // regex: /docs/2.1, /docs/3.0
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @RequestMapping — How It Maps to HandlerMapping:                                │
│                                                                                  │
│  At startup:                                                                     │
│  RequestMappingHandlerMapping scans all @Controller/@RestController beans        │
│  and builds a registry:                                                          │
│                                                                                  │
│  Class-level @RequestMapping("/api/v1/products")                                 │
│   + Method-level @RequestMapping(value="/{id}", method=GET)                     │
│   = Combined: "GET /api/v1/products/{id}" → ProductController#getById(Long)    │
│                                                                                  │
│  At runtime:                                                                     │
│  Incoming request: GET /api/v1/products/42                                       │
│  → Match URL: "/api/v1/products/{id}" ✓                                        │
│  → Match method: GET ✓                                                          │
│  → Match consumes/produces: ✓ (or not specified = match all)                   │
│  → Extract {id} = "42"                                                          │
│  → Invoke ProductController#getById(42L)                                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 6. `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`

These are **shortcut annotations** — specialisations of `@RequestMapping` with the HTTP method pre-filled. They are cleaner and more readable.

```java
// Source — each is just @RequestMapping with method pre-set:

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping(method = RequestMethod.GET)         // ← pre-filled
public @interface GetMapping {
    String[] value()    default {};
    String[] path()     default {};
    String[] params()   default {};
    String[] headers()  default {};
    String[] consumes() default {};
    String[] produces() default {};
}

@RequestMapping(method = RequestMethod.POST)        // ← pre-filled
public @interface PostMapping { /* same attributes */ }

@RequestMapping(method = RequestMethod.PUT)         // ← pre-filled
public @interface PutMapping { /* same attributes */ }

@RequestMapping(method = RequestMethod.DELETE)      // ← pre-filled
public @interface DeleteMapping { /* same attributes */ }

@RequestMapping(method = RequestMethod.PATCH)       // ← pre-filled
public @interface PatchMapping { /* same attributes */ }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Equivalence — @RequestMapping vs Shortcut:                                      │
│                                                                                  │
│  @RequestMapping(value="/users", method=RequestMethod.GET)                       │
│  ≡ @GetMapping("/users")                                                        │
│                                                                                  │
│  @RequestMapping(value="/users", method=RequestMethod.POST)                      │
│  ≡ @PostMapping("/users")                                                       │
│                                                                                  │
│  @RequestMapping(value="/users/{id}", method=RequestMethod.PUT)                  │
│  ≡ @PutMapping("/users/{id}")                                                   │
│                                                                                  │
│  @RequestMapping(value="/users/{id}", method=RequestMethod.DELETE)                │
│  ≡ @DeleteMapping("/users/{id}")                                                │
│                                                                                  │
│  @RequestMapping(value="/users/{id}", method=RequestMethod.PATCH)                │
│  ≡ @PatchMapping("/users/{id}")                                                 │
│                                                                                  │
│  Best practice: ALWAYS use the shortcut annotations in controllers.             │
│  Use @RequestMapping only at class level for base paths.                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Full CRUD example with shortcuts ─────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/orders")       // class-level: @RequestMapping for base path
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping                                         // GET /api/v1/orders
    public List<OrderResponse> list() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")                                // GET /api/v1/orders/42
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @GetMapping(value = "/export",                      // GET /api/v1/orders/export
                produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csv = orderService.exportToCsv();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
            .body(csv);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)   // POST /api/v1/orders
    public ResponseEntity<OrderResponse> create(
            @RequestBody @Valid OrderRequest request) {
        OrderResponse created = orderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")                                // PUT /api/v1/orders/42
    public OrderResponse update(@PathVariable Long id,
                                @RequestBody @Valid OrderRequest request) {
        return orderService.update(id, request);
    }

    @PatchMapping("/{id}/status")                       // PATCH /api/v1/orders/42/status
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @RequestBody StatusUpdate status) {
        return orderService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")                             // DELETE /api/v1/orders/42
    @ResponseStatus(HttpStatus.NO_CONTENT)              // 204
    public void delete(@PathVariable Long id) {
        orderService.delete(id);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  HTTP Method Semantics (REST conventions):                                       │
│                                                                                  │
│  ┌────────────────┬──────────────────────┬──────────────┬──────────────────────┐│
│  │ Annotation     │ HTTP Method          │ Idempotent?  │ Typical Use          ││
│  ├────────────────┼──────────────────────┼──────────────┼──────────────────────┤│
│  │ @GetMapping    │ GET                  │ Yes          │ Read / fetch data    ││
│  │ @PostMapping   │ POST                 │ No           │ Create new resource  ││
│  │ @PutMapping    │ PUT                  │ Yes          │ Full replace/update  ││
│  │ @PatchMapping  │ PATCH                │ No*          │ Partial update       ││
│  │ @DeleteMapping │ DELETE               │ Yes          │ Remove resource      ││
│  └────────────────┴──────────────────────┴──────────────┴──────────────────────┘│
│  * PATCH can be idempotent depending on implementation                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 7. `@ResponseBody`

Tells Spring that the return value of a method should be written **directly to the HTTP response body** (serialised via `HttpMessageConverter`, typically Jackson for JSON) instead of being interpreted as a view name.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseBody {
    // No attributes — it's a marker annotation
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @ResponseBody — How It Changes Return Value Handling:                           │
│                                                                                  │
│  WITHOUT @ResponseBody (in @Controller):                                         │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │ return "users/list";                                                     │   │
│  │     ↓                                                                    │   │
│  │ String treated as VIEW NAME                                              │   │
│  │     ↓                                                                    │   │
│  │ ViewResolver → ThymeleafView → renders HTML → writes to response        │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  WITH @ResponseBody (or in @RestController):                                     │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │ return userResponse;                                                     │   │
│  │     ↓                                                                    │   │
│  │ Object passed to HttpMessageConverter                                    │   │
│  │     ↓                                                                    │   │
│  │ Content negotiation (Accept header → JSON? XML? text?)                  │   │
│  │     ↓                                                                    │   │
│  │ MappingJackson2HttpMessageConverter.write(userResponse)                  │   │
│  │     ↓                                                                    │   │
│  │ Jackson ObjectMapper serialises → JSON bytes → response output stream   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── @ResponseBody on individual methods in a @Controller ─────────────────────────
@Controller
public class MixedController {

    // Returns a view (HTML)
    @GetMapping("/home")
    public String homePage(Model model) {
        model.addAttribute("title", "Welcome");
        return "home";                         // → templates/home.html
    }

    // Returns JSON (because of @ResponseBody)
    @GetMapping("/api/status")
    @ResponseBody
    public Map<String, String> status() {
        return Map.of("status", "UP", "version", "1.0.0");
        // → {"status":"UP","version":"1.0.0"}
    }
}


// ── @ResponseBody at class level = @RestController ───────────────────────────────
// These two are IDENTICAL:

@Controller
@ResponseBody
public class UserApiController { ... }

@RestController    // = @Controller + @ResponseBody
public class UserApiController { ... }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @ResponseBody — Internally:                                                     │
│                                                                                  │
│  RequestMappingHandlerAdapter detects @ResponseBody on method/class              │
│      ↓                                                                           │
│  Uses RequestResponseBodyMethodProcessor as return value handler                 │
│      ↓                                                                           │
│  Content negotiation:                                                            │
│  1. Client sends: Accept: application/json                                      │
│  2. Spring iterates through registered HttpMessageConverters:                   │
│     - StringHttpMessageConverter          (text/plain)                          │
│     - MappingJackson2HttpMessageConverter  (application/json) ← SELECTED       │
│     - MappingJackson2XmlHttpMessageConverter (application/xml)                  │
│     - ByteArrayHttpMessageConverter       (application/octet-stream)            │
│  3. Selected converter serialises the return object                              │
│  4. Writes bytes to HttpServletResponse.getOutputStream()                       │
│  5. Sets Content-Type header automatically                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8. `ResponseEntity<T>`

`ResponseEntity` is **not an annotation** — it's a **class** that represents the entire HTTP response: **status code**, **headers**, and **body**. It gives you full control over the response, unlike plain return types where Spring chooses defaults.

```java
// ResponseEntity class hierarchy:
public class ResponseEntity<T> extends HttpEntity<T> {
    // HttpEntity holds: headers + body
    // ResponseEntity adds: status code

    // Factory methods:
    static <T> ResponseEntity<T> ok(T body);                  // 200 + body
    static ResponseEntity.BodyBuilder ok();                     // 200 builder
    static <T> ResponseEntity<T> of(Optional<T> body);        // 200 or 404
    static ResponseEntity.BodyBuilder created(URI location);    // 201 + Location
    static ResponseEntity.HeadersBuilder<?> noContent();        // 204
    static ResponseEntity.BodyBuilder badRequest();             // 400
    static ResponseEntity.HeadersBuilder<?> notFound();         // 404
    static ResponseEntity.BodyBuilder status(HttpStatus status);// custom status
    static ResponseEntity.BodyBuilder status(int status);       // custom int status
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why Use ResponseEntity Instead of Plain Return Types?                           │
│                                                                                  │
│  Plain return:                                                                   │
│  @GetMapping("/{id}")                                                            │
│  public UserResponse getById(@PathVariable Long id) {                           │
│      return userService.findById(id);                                           │
│  }                                                                               │
│  → Always returns 200 OK (unless exception thrown)                              │
│  → Cannot set custom headers                                                    │
│  → Cannot set Location header for 201                                           │
│                                                                                  │
│  ResponseEntity:                                                                 │
│  @GetMapping("/{id}")                                                            │
│  public ResponseEntity<UserResponse> getById(@PathVariable Long id) {           │
│      UserResponse user = userService.findById(id);                              │
│      return ResponseEntity.ok()                                                 │
│          .header("X-Custom-Header", "value")                                    │
│          .body(user);                                                            │
│  }                                                                               │
│  → Full control: status, headers, body                                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Code Examples

```java
// ── Common ResponseEntity patterns ───────────────────────────────────────────────

// 200 OK with body
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));
}


// 201 Created with Location header
@PostMapping
public ResponseEntity<UserResponse> create(@RequestBody @Valid UserRequest request) {
    UserResponse created = userService.create(request);
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.getId())
        .toUri();
    return ResponseEntity
        .created(location)        // status 201 + Location header
        .body(created);
    // Response:
    // HTTP 201 Created
    // Location: /api/v1/users/42
    // Body: {"id":42,"name":"Alice",...}
}


// 204 No Content (after delete)
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
    // HTTP 204 No Content (no body)
}


// 200 or 404 using Optional
@GetMapping("/{id}")
public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
    return ResponseEntity.of(userService.findOptional(id));
    // If Optional is present → 200 OK + body
    // If Optional is empty   → 404 Not Found (no body)
}


// Custom status + custom headers
@PostMapping("/import")
public ResponseEntity<ImportResult> importData(@RequestBody List<DataRow> rows) {
    ImportResult result = importService.process(rows);
    return ResponseEntity
        .status(HttpStatus.ACCEPTED)              // 202 Accepted
        .header("X-Processed-Count", String.valueOf(result.getCount()))
        .header("X-Request-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .body(result);
}


// Return different status codes based on logic
@PutMapping("/{id}")
public ResponseEntity<UserResponse> createOrUpdate(
        @PathVariable Long id,
        @RequestBody @Valid UserRequest request) {
    boolean existed = userService.exists(id);
    UserResponse result = userService.createOrUpdate(id, request);
    if (existed) {
        return ResponseEntity.ok(result);                   // 200 (updated)
    } else {
        URI location = URI.create("/api/v1/users/" + id);
        return ResponseEntity.created(location).body(result); // 201 (created)
    }
}


// File download with ResponseEntity
@GetMapping("/download/{filename}")
public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
    byte[] fileContent = fileService.load(filename);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(fileContent.length)
        .body(fileContent);
}


// ResponseEntity with no body (just status + headers)
@PostMapping("/{id}/approve")
public ResponseEntity<Void> approve(@PathVariable Long id) {
    orderService.approve(id);
    return ResponseEntity.ok()
        .header("X-Approved-By", "admin")
        .build();                                     // .build() → no body
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ResponseEntity — Builder Pattern:                                               │
│                                                                                  │
│  ResponseEntity.status(HttpStatus.CREATED)     ← set status code               │
│      .header("X-Custom", "value")              ← add custom header             │
│      .header(HttpHeaders.CACHE_CONTROL, "no-cache") ← standard header          │
│      .contentType(MediaType.APPLICATION_JSON)  ← Content-Type                   │
│      .contentLength(1024)                      ← Content-Length                  │
│      .lastModified(Instant.now())              ← Last-Modified                  │
│      .eTag("\"v1\"")                           ← ETag for caching              │
│      .body(responseObject);                    ← set body (or .build() for none)│
│                                                                                  │
│  Static shortcuts:                                                               │
│  ResponseEntity.ok(body)           → 200 + body                                │
│  ResponseEntity.ok().build()       → 200, no body                              │
│  ResponseEntity.created(uri).body(b) → 201 + Location + body                  │
│  ResponseEntity.noContent().build() → 204, no body                             │
│  ResponseEntity.badRequest().body(e) → 400 + error body                        │
│  ResponseEntity.notFound().build()  → 404, no body                             │
│  ResponseEntity.of(optional)       → 200 if present, 404 if empty             │
│  ResponseEntity.accepted().body(b) → 202 + body                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 9. `@RequestBody`

Tells Spring to read the HTTP request body and **deserialise** it into a Java object using an `HttpMessageConverter` (Jackson for JSON, JAXB for XML).

```java
// Source:
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestBody {
    boolean required() default true;   // if true, request body MUST be present
}
```

##### How `@RequestBody` Converts JSON to Java Object

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @RequestBody — JSON Deserialization Flow:                                        │
│                                                                                  │
│  HTTP Request:                                                                   │
│  POST /api/v1/users                                                              │
│  Content-Type: application/json                                                  │
│  Body: {"first_name":"Alice","last_name":"Smith","email":"alice@example.com",   │
│         "age":28,"role":"ADMIN"}                                                │
│      │                                                                           │
│      ▼                                                                           │
│  1. DispatcherServlet → HandlerAdapter sees @RequestBody parameter              │
│      │                                                                           │
│      ▼                                                                           │
│  2. RequestResponseBodyMethodProcessor.resolveArgument()                         │
│      → Reads Content-Type header: "application/json"                            │
│      → Iterates HttpMessageConverters to find one that supports JSON:           │
│        StringHttpMessageConverter → NO (not for objects)                         │
│        MappingJackson2HttpMessageConverter → YES ✓                              │
│      │                                                                           │
│      ▼                                                                           │
│  3. MappingJackson2HttpMessageConverter.read(UserRequest.class, inputMessage)    │
│      → Reads raw bytes from HttpServletRequest.getInputStream()                 │
│      → Calls Jackson ObjectMapper.readValue(bytes, UserRequest.class)           │
│      │                                                                           │
│      ▼                                                                           │
│  4. Jackson ObjectMapper:                                                        │
│      → Creates new UserRequest() (calls no-arg constructor or @JsonCreator)     │
│      → For each JSON field:                                                     │
│         "first_name" → looks for @JsonProperty("first_name") OR setter          │
│                         setFirstName() → calls it with "Alice"                  │
│         "last_name"  → @JsonProperty("last_name") → setLastName("Smith")       │
│         "email"      → direct match → setEmail("alice@example.com")            │
│         "age"        → direct match, auto-converts String→int → setAge(28)     │
│         "role"       → looks for enum Role.ADMIN → setRole(Role.ADMIN)          │
│      → Returns fully populated UserRequest object                               │
│      │                                                                           │
│      ▼                                                                           │
│  5. If @Valid is present:                                                        │
│      → Hibernate Validator checks @NotBlank, @Email, @Min, @Size, etc.         │
│      → If violations → throws MethodArgumentNotValidException → 400            │
│      │                                                                           │
│      ▼                                                                           │
│  6. Resolved UserRequest passed to controller method parameter                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### `@JsonProperty` — Mapping JSON Keys to Java Fields

```java
// ── When JSON field names differ from Java field names ───────────────────────────
public class UserRequest {

    @JsonProperty("first_name")    // JSON: "first_name" → Java: firstName
    private String firstName;

    @JsonProperty("last_name")     // JSON: "last_name" → Java: lastName
    private String lastName;

    @JsonProperty(value = "email_address", required = true)
    private String email;          // JSON: "email_address" → Java: email

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;               // only included in responses, ignored in requests

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;       // only read from requests, never in responses

    @JsonProperty(defaultValue = "USER")
    private String role;           // if JSON omits "role", defaults to "USER"

    // getters and setters...
}

// Incoming JSON:
// {"first_name":"Alice","last_name":"Smith","email_address":"alice@example.com"}
//
// Jackson maps:
// "first_name"    → firstName = "Alice"
// "last_name"     → lastName  = "Smith"
// "email_address" → email     = "alice@example.com"
```

```java
// ── Other Jackson annotations used with @RequestBody ─────────────────────────────
public class OrderRequest {

    @JsonProperty("order_id")
    private String orderId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;      // "2026-05-12 10:30:00" → LocalDateTime

    @JsonIgnore
    private String internalField;         // completely ignored by Jackson (both ways)

    @JsonIgnoreProperties(ignoreUnknown = true)  // at class level — ignore extra JSON fields
    // Without this, unknown JSON fields throw UnrecognizedPropertyException

    @JsonAlias({"customer_name", "clientName", "buyer"})
    private String customerName;          // accepts any of these JSON field names

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String notes;                 // omit from JSON output if null
}


// ── @JsonCreator for immutable objects ───────────────────────────────────────────
public class ImmutableUser {
    private final String name;
    private final String email;

    @JsonCreator    // tells Jackson to use this constructor for deserialization
    public ImmutableUser(
        @JsonProperty("name") String name,
        @JsonProperty("email") String email
    ) {
        this.name = name;
        this.email = email;
    }
    // no setters needed — Jackson uses constructor directly
}
```

##### Controller Examples with `@RequestBody`

```java
// ── Basic @RequestBody ───────────────────────────────────────────────────────────
@PostMapping("/users")
public ResponseEntity<UserResponse> create(
        @RequestBody @Valid UserRequest request) {
    // request is already a fully populated Java object
    return ResponseEntity.status(201).body(userService.create(request));
}


// ── Optional request body ────────────────────────────────────────────────────────
@PatchMapping("/users/{id}/preferences")
public UserResponse updatePreferences(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, Object> preferences) {
    // if body is missing/empty → preferences is null (not an error)
    return userService.updatePreferences(id, preferences);
}


// ── @RequestBody with List ───────────────────────────────────────────────────────
@PostMapping("/users/batch")
public List<UserResponse> createBatch(@RequestBody @Valid List<UserRequest> requests) {
    // JSON: [{"name":"Alice","email":"a@b.com"}, {"name":"Bob","email":"b@b.com"}]
    return requests.stream()
        .map(userService::create)
        .collect(Collectors.toList());
}


// ── @RequestBody with raw types ──────────────────────────────────────────────────
@PostMapping("/webhook")
public ResponseEntity<Void> handleWebhook(
        @RequestBody String rawBody) {
    // receives the raw JSON string as-is (StringHttpMessageConverter used)
    log.info("Webhook payload: {}", rawBody);
    return ResponseEntity.ok().build();
}

@PostMapping("/upload")
public ResponseEntity<Void> upload(@RequestBody byte[] data) {
    // receives raw bytes (ByteArrayHttpMessageConverter used)
    fileService.save(data);
    return ResponseEntity.ok().build();
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @RequestBody — HttpMessageConverter Selection:                                  │
│                                                                                  │
│  Content-Type header          → HttpMessageConverter used                       │
│  ─────────────────────────────────────────────────────────────────────────────── │
│  application/json             → MappingJackson2HttpMessageConverter              │
│  application/xml              → MappingJackson2XmlHttpMessageConverter           │
│  text/plain                   → StringHttpMessageConverter                       │
│  application/x-www-form-urlencoded → FormHttpMessageConverter                   │
│  application/octet-stream     → ByteArrayHttpMessageConverter                   │
│  multipart/form-data          → use @RequestParam MultipartFile instead         │
│                                                                                  │
│  If Content-Type doesn't match any converter → 415 Unsupported Media Type      │
│  If body is missing and required=true → 400 Bad Request                        │
│  If JSON is malformed → HttpMessageNotReadableException → 400                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 10. `@RequestParam`

Binds a **query parameter** (or form field) from the URL to a method parameter. Spring automatically converts the String value to the target Java type.

```java
// Source:
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    String  value()        default "";        // parameter name (alias for name)
    String  name()         default "";        // parameter name
    boolean required()     default true;      // is this parameter mandatory?
    String  defaultValue() default "\n\t\t\n\t\t\n..."; // default if absent
}
```

##### Properties / Attributes

```text
┌──────────────────────┬───────────────────────────────────────────────────────────┐
│ Attribute            │ Description                                               │
├──────────────────────┼───────────────────────────────────────────────────────────┤
│ value / name         │ Name of the query parameter in the URL.                   │
│                      │ If omitted, uses the Java parameter name.                │
│ required             │ Whether the parameter must be present. Default: true.     │
│                      │ If true and missing → 400 Bad Request.                   │
│ defaultValue         │ Default value if parameter is absent or empty.            │
│                      │ Setting a defaultValue implicitly makes required=false.  │
└──────────────────────┴───────────────────────────────────────────────────────────┘
```

##### Automatic Type Conversion

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @RequestParam — Automatic Type Conversion:                                      │
│                                                                                  │
│  Spring uses ConversionService (+ PropertyEditors as fallback) to convert       │
│  the String query parameter value to the target Java type automatically.        │
│                                                                                  │
│  URL: /search?page=3&size=20&active=true&price=29.99&name=Alice                 │
│                                                                                  │
│  ┌──────────────────────┬────────────────┬───────────────────────────────────┐  │
│  │ Target Type          │ String Value   │ Converted Result                  │  │
│  ├──────────────────────┼────────────────┼───────────────────────────────────┤  │
│  │ Primitives                                                                │  │
│  │ int                  │ "3"            │ 3                                 │  │
│  │ long                 │ "42"           │ 42L                               │  │
│  │ double               │ "29.99"        │ 29.99                             │  │
│  │ boolean              │ "true"         │ true                              │  │
│  │ float                │ "3.14"         │ 3.14f                             │  │
│  ├──────────────────────┼────────────────┼───────────────────────────────────┤  │
│  │ Wrapper Types                                                             │  │
│  │ Integer              │ "3"            │ Integer.valueOf(3)                │  │
│  │ Long                 │ "42"           │ Long.valueOf(42)                  │  │
│  │ Double               │ "29.99"        │ Double.valueOf(29.99)             │  │
│  │ Boolean              │ "true"         │ Boolean.TRUE                      │  │
│  ├──────────────────────┼────────────────┼───────────────────────────────────┤  │
│  │ String               │ "Alice"        │ "Alice" (no conversion needed)   │  │
│  ├──────────────────────┼────────────────┼───────────────────────────────────┤  │
│  │ Enum                 │ "ADMIN"        │ Role.ADMIN (Enum.valueOf)        │  │
│  │                      │ "admin"        │ FAILS unless custom converter   │  │
│  ├──────────────────────┼────────────────┼───────────────────────────────────┤  │
│  │ Date/Time                                                                 │  │
│  │ LocalDate            │ "2026-05-12"   │ LocalDate.parse(...)             │  │
│  │ LocalDateTime        │ "2026-05-12T10:30:00" │ LocalDateTime.parse(...)  │  │
│  ├──────────────────────┼────────────────┼───────────────────────────────────┤  │
│  │ Collections                                                               │  │
│  │ List<String>         │ "a,b,c"        │ ["a","b","c"]                    │  │
│  │ Set<Integer>         │ repeated param │ Set.of(1,2,3)                    │  │
│  ├──────────────────────┼────────────────┼───────────────────────────────────┤  │
│  │ UUID                 │ "550e8400..." │ UUID.fromString(...)              │  │
│  └──────────────────────┴────────────────┴───────────────────────────────────┘  │
│                                                                                  │
│  If conversion fails → MethodArgumentTypeMismatchException → 400 Bad Request   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Code Examples

```java
// ── Primitive and wrapper types ──────────────────────────────────────────────────
@GetMapping("/search")
public Page<Product> search(
        @RequestParam String query,                           // required
        @RequestParam(defaultValue = "0") int page,           // optional, default 0
        @RequestParam(defaultValue = "20") int size,          // optional, default 20
        @RequestParam(required = false) Double minPrice,      // optional (nullable)
        @RequestParam(required = false) Double maxPrice,
        @RequestParam(defaultValue = "false") boolean inStock // converts "true"/"false"
) {
    return productService.search(query, page, size, minPrice, maxPrice, inStock);
}
// GET /search?query=laptop&page=2&size=10&minPrice=500&inStock=true


// ── Enum type (auto-conversion) ──────────────────────────────────────────────────
public enum OrderStatus { PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED }

@GetMapping("/orders")
public List<Order> getByStatus(@RequestParam OrderStatus status) {
    return orderService.findByStatus(status);
}
// GET /orders?status=SHIPPED → status = OrderStatus.SHIPPED
// GET /orders?status=shipped → 400 Bad Request (case-sensitive by default!)


// ── Case-insensitive enum conversion (custom converter) ──────────────────────────
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToEnumConverterFactory());
    }
}

public class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {
    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> (T) Enum.valueOf(targetType, source.toUpperCase());
    }
}
// Now: GET /orders?status=shipped → OrderStatus.SHIPPED ✓


// ── List and Set parameters ──────────────────────────────────────────────────────
@GetMapping("/products/filter")
public List<Product> filter(
        @RequestParam List<String> categories,     // ?categories=electronics,books
        @RequestParam Set<Long> ids                // ?ids=1&ids=2&ids=3
) {
    return productService.filter(categories, ids);
}
// GET /products/filter?categories=electronics,books&ids=1&ids=2&ids=3


// ── Map parameter (capture all query params) ────────────────────────────────────
@GetMapping("/flexible-search")
public List<Product> flexibleSearch(@RequestParam Map<String, String> allParams) {
    // allParams = {"query":"laptop", "page":"0", "sort":"price"}
    // Useful when you don't know param names at compile time
    return productService.dynamicSearch(allParams);
}


// ── @RequestParam with @DateTimeFormat ───────────────────────────────────────────
@GetMapping("/events")
public List<Event> getEvents(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate to
) {
    return eventService.findBetween(from, to);
}
// GET /events?from=2026-05-01&to=31/05/2026


// ── @RequestParam name differs from Java param ──────────────────────────────────
@GetMapping("/users")
public List<User> search(
        @RequestParam("q") String query,         // URL: ?q=alice  → query = "alice"
        @RequestParam("sort_by") String sortBy   // URL: ?sort_by=name → sortBy = "name"
) {
    return userService.search(query, sortBy);
}
```

##### Custom Object with `PropertyEditor`

```java
// ── Custom PropertyEditor for a complex type ─────────────────────────────────────
// Convert a comma-separated string to a custom object

public class GeoLocation {
    private double latitude;
    private double longitude;
    // constructors, getters, setters...
}

// PropertyEditor converts String → GeoLocation
public class GeoLocationEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        String[] parts = text.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Format: lat,lng");
        }
        GeoLocation loc = new GeoLocation(
            Double.parseDouble(parts[0].trim()),
            Double.parseDouble(parts[1].trim())
        );
        setValue(loc);
    }
}

// Register it via @InitBinder
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(GeoLocation.class, new GeoLocationEditor());
    }

    @GetMapping("/nearby")
    public List<Store> findNearby(
            @RequestParam GeoLocation location,     // ?location=40.7128,-74.0060
            @RequestParam(defaultValue = "10") double radiusKm
    ) {
        return storeService.findNearby(location, radiusKm);
    }
}
// GET /api/v1/stores/nearby?location=40.7128,-74.0060&radiusKm=5
// location → GeoLocation(40.7128, -74.0060)


// ── Alternative: use Converter (preferred over PropertyEditor) ───────────────────
@Component
public class GeoLocationConverter implements Converter<String, GeoLocation> {
    @Override
    public GeoLocation convert(String source) {
        String[] parts = source.split(",");
        return new GeoLocation(
            Double.parseDouble(parts[0].trim()),
            Double.parseDouble(parts[1].trim())
        );
    }
}
// Register automatically via @Component — no @InitBinder needed
// Spring detects Converter<String, GeoLocation> and uses it for @RequestParam
```

---

#### 11. `@InitBinder`

Marks a method in a `@Controller` (or `@ControllerAdvice`) that customises data binding — registering custom `PropertyEditor`s, setting allowed/disallowed fields, configuring validators, etc. Called before each request that involves data binding.

```java
// Source:
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InitBinder {
    String[] value() default {};   // restrict to specific model attribute names
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @InitBinder — When It Runs:                                                     │
│                                                                                  │
│  Request arrives → DispatcherServlet → HandlerAdapter                           │
│      │                                                                           │
│      ▼                                                                           │
│  Before resolving method arguments:                                              │
│      1. Find all @InitBinder methods (local + @ControllerAdvice)                │
│      2. Call each with a WebDataBinder instance                                 │
│      3. Binder now has custom editors/validators registered                     │
│      │                                                                           │
│      ▼                                                                           │
│  Resolve method arguments (@RequestParam, @ModelAttribute, @PathVariable, ...)  │
│      → WebDataBinder uses registered editors for type conversion                │
│      → WebDataBinder runs registered validators                                 │
│      │                                                                           │
│      ▼                                                                           │
│  Controller method executes                                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Register custom PropertyEditors ──────────────────────────────────────────────
@RestController
public class UserController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Trim whitespace from all String fields
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

        // Custom date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));

        // Custom editor for specific type
        binder.registerCustomEditor(GeoLocation.class, new GeoLocationEditor());
    }
}


// ── Restrict which fields can be set (security: prevent mass assignment) ─────────
@InitBinder
public void initBinder(WebDataBinder binder) {
    // Only these fields can be set from the request
    binder.setAllowedFields("name", "email", "phone");
    // "role", "isAdmin", etc. CANNOT be set — even if sent in the request
    // Prevents: POST /users with {"name":"Alice","role":"ADMIN"}
    // "role" is silently ignored

    // Alternative: disallow specific fields
    binder.setDisallowedFields("id", "role", "createdAt");
}


// ── Scoped @InitBinder — only for specific model attributes ─────────────────────
@InitBinder("userRequest")    // only applies when binding UserRequest
public void initUserBinder(WebDataBinder binder) {
    binder.addValidators(new UserRequestValidator());
}

@InitBinder("orderRequest")   // only applies when binding OrderRequest
public void initOrderBinder(WebDataBinder binder) {
    binder.addValidators(new OrderRequestValidator());
}


// ── Register a custom Validator ──────────────────────────────────────────────────
@InitBinder
public void initBinder(WebDataBinder binder) {
    binder.addValidators(new UserRequestValidator());
    // This validator runs IN ADDITION to @Valid annotation validators
}

public class UserRequestValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return UserRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UserRequest user = (UserRequest) target;
        if (user.getPassword() != null &&
            user.getPassword().equals(user.getUsername())) {
            errors.rejectValue("password", "password.same.as.username",
                "Password cannot be the same as username");
        }
    }
}
```

---

#### 12. `@PathVariable`

Binds a **URI template variable** from the URL path to a method parameter.

```java
// Source:
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {
    String  value()    default "";     // name of the path variable
    String  name()     default "";     // alias for value
    boolean required() default true;   // must be present? (default: yes)
}
```

```java
// ── Basic usage ──────────────────────────────────────────────────────────────────
@GetMapping("/users/{id}")
public UserResponse getById(@PathVariable Long id) {
    // GET /users/42 → id = 42L
    return userService.findById(id);
}
// Spring extracts "42" from the URL and converts it to Long automatically
// Uses same ConversionService as @RequestParam


// ── Multiple path variables ──────────────────────────────────────────────────────
@GetMapping("/users/{userId}/orders/{orderId}")
public OrderResponse getUserOrder(
        @PathVariable Long userId,
        @PathVariable Long orderId) {
    // GET /users/42/orders/7 → userId=42L, orderId=7L
    return orderService.findByUserAndId(userId, orderId);
}


// ── Name differs from Java parameter ────────────────────────────────────────────
@GetMapping("/departments/{dept-id}/employees/{emp-id}")
public Employee getEmployee(
        @PathVariable("dept-id") Long departmentId,      // URL has hyphens
        @PathVariable("emp-id") Long employeeId) {       // Java params use camelCase
    return employeeService.find(departmentId, employeeId);
}


// ── Optional path variable (rare) ───────────────────────────────────────────────
@GetMapping({"/files/{path}", "/files"})
public Resource getFile(@PathVariable(required = false) String path) {
    if (path == null) {
        return fileService.listRoot();   // /files → list root
    }
    return fileService.get(path);        // /files/readme.txt → get file
}


// ── Path variable with regex ─────────────────────────────────────────────────────
@GetMapping("/docs/{version:\\d+\\.\\d+}")
public Documentation getDocs(@PathVariable String version) {
    // /docs/2.1 → version="2.1" ✓
    // /docs/latest → 404 (doesn't match regex \\d+\\.\\d+)
    return docService.getVersion(version);
}


// ── Enum path variable ──────────────────────────────────────────────────────────
@GetMapping("/orders/status/{status}")
public List<Order> getByStatus(@PathVariable OrderStatus status) {
    // /orders/status/SHIPPED → status = OrderStatus.SHIPPED
    return orderService.findByStatus(status);
}


// ── Capture all path variables into a Map ────────────────────────────────────────
@GetMapping("/api/{module}/{version}/{resource}")
public Object dynamicRoute(@PathVariable Map<String, String> pathVars) {
    // /api/users/v2/profiles
    // pathVars = {"module":"users", "version":"v2", "resource":"profiles"}
    String module = pathVars.get("module");
    String version = pathVars.get("version");
    return routingService.handle(module, version, pathVars.get("resource"));
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @PathVariable vs @RequestParam:                                                 │
│                                                                                  │
│  @PathVariable — part of the URL path:                                          │
│  GET /api/v1/users/42                                                            │
│  @GetMapping("/users/{id}")                                                      │
│  public User get(@PathVariable Long id)  → id = 42                              │
│                                                                                  │
│  @RequestParam — query string parameter:                                        │
│  GET /api/v1/users?id=42                                                         │
│  @GetMapping("/users")                                                           │
│  public User get(@RequestParam Long id)  → id = 42                              │
│                                                                                  │
│  REST convention:                                                                │
│  • @PathVariable for identifying a SPECIFIC resource: /users/{id}               │
│  • @RequestParam for filtering, pagination, search: /users?role=admin&page=2    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13. `@RequestHeader`

Binds an **HTTP request header** value to a method parameter.

```java
// Source:
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHeader {
    String  value()        default "";       // header name
    String  name()         default "";       // alias for value
    boolean required()     default true;     // must be present?
    String  defaultValue() default "\n\t\t\n\t\t\n...";  // default if missing
}
```

```java
// ── Basic usage ──────────────────────────────────────────────────────────────────
@GetMapping("/profile")
public UserResponse getProfile(
        @RequestHeader("Authorization") String authHeader) {
    // Authorization: Bearer eyJhbGci...
    // authHeader = "Bearer eyJhbGci..."
    String token = authHeader.substring(7);  // strip "Bearer "
    return userService.findByToken(token);
}


// ── Multiple headers ─────────────────────────────────────────────────────────────
@PostMapping("/webhook")
public ResponseEntity<Void> handleWebhook(
        @RequestHeader("X-Webhook-Signature") String signature,
        @RequestHeader("X-Webhook-Event") String event,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody String payload) {
    webhookService.verify(signature, payload);
    webhookService.process(event, payload);
    return ResponseEntity.ok().build();
}


// ── Optional headers with defaults ───────────────────────────────────────────────
@GetMapping("/data")
public DataResponse getData(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String language,
        @RequestHeader(value = "X-API-Version", defaultValue = "1") int apiVersion,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
) {
    if (correlationId == null) {
        correlationId = UUID.randomUUID().toString();
    }
    return dataService.fetch(language, apiVersion);
}


// ── Capture ALL headers ──────────────────────────────────────────────────────────
@GetMapping("/debug/headers")
public Map<String, String> debugHeaders(@RequestHeader Map<String, String> allHeaders) {
    // allHeaders = {"host":"localhost:8080", "accept":"application/json",
    //               "authorization":"Bearer ...", ...}
    return allHeaders;
}

@GetMapping("/debug/headers-multi")
public HttpHeaders debugMultiHeaders(@RequestHeader HttpHeaders headers) {
    // HttpHeaders supports multi-valued headers (e.g., Accept can have multiple values)
    List<MediaType> acceptTypes = headers.getAccept();
    return headers;
}


// ── Type conversion on headers ───────────────────────────────────────────────────
@GetMapping("/rate-limited")
public DataResponse getData(
        @RequestHeader("X-Rate-Limit-Remaining") int remaining,    // String → int
        @RequestHeader("X-Rate-Limit-Reset") long resetTimestamp   // String → long
) {
    if (remaining < 5) {
        log.warn("Rate limit almost exhausted. Reset at: {}", resetTimestamp);
    }
    return dataService.fetch();
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Common HTTP Headers Used in Spring Boot:                                        │
│                                                                                  │
│  ┌────────────────────────────┬──────────────────────────────────────────────┐  │
│  │ Header                     │ Typical Use                                  │  │
│  ├────────────────────────────┼──────────────────────────────────────────────┤  │
│  │ Authorization              │ Bearer token, Basic auth credentials        │  │
│  │ Content-Type               │ Format of request body (application/json)   │  │
│  │ Accept                     │ Desired response format                      │  │
│  │ Accept-Language            │ Locale/language preference                   │  │
│  │ User-Agent                 │ Client software identifier                  │  │
│  │ X-Correlation-ID           │ Distributed tracing identifier              │  │
│  │ X-Request-ID               │ Unique request identifier                   │  │
│  │ X-Forwarded-For            │ Original client IP (behind proxy)           │  │
│  │ X-API-Version              │ API version header                          │  │
│  │ If-None-Match              │ ETag for conditional GET (caching)          │  │
│  │ Cache-Control              │ Caching directives                           │  │
│  └────────────────────────────┴──────────────────────────────────────────────┘  │
│                                                                                  │
│  Standard headers: use HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, etc.│
│  Custom headers: convention is "X-" prefix (e.g., X-Correlation-ID)            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Summary — Request Parameter Annotations Comparison

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Where Each Annotation Reads From:                                               │
│                                                                                  │
│  HTTP Request:                                                                   │
│  POST /api/v1/users/42?notify=true HTTP/1.1                                     │
│  Host: localhost:8080                          ← @RequestHeader("Host")         │
│  Authorization: Bearer eyJhb...               ← @RequestHeader("Authorization")│
│  Content-Type: application/json               ← (used for converter selection) │
│  X-Request-ID: abc-123                        ← @RequestHeader("X-Request-ID") │
│         ↑↑↑↑↑↑↑↑↑ HEADERS ↑↑↑↑↑↑↑↑↑                                          │
│                                                                                  │
│  /api/v1/users/{id}                           ← @PathVariable("id") = 42       │
│                ?notify=true                   ← @RequestParam("notify") = true  │
│         ↑↑↑↑↑ URL PATH ↑↑↑   ↑↑↑ QUERY STRING ↑↑↑                             │
│                                                                                  │
│  {"name":"Alice","email":"alice@example.com"}  ← @RequestBody UserRequest      │
│         ↑↑↑↑↑↑↑↑↑↑↑↑ REQUEST BODY ↑↑↑↑↑↑↑↑↑↑↑↑                              │
│                                                                                  │
│  ┌────────────────┬──────────────────────────┬───────────────────────────────┐  │
│  │ Annotation     │ Reads From               │ Example                       │  │
│  ├────────────────┼──────────────────────────┼───────────────────────────────┤  │
│  │ @PathVariable  │ URL path template        │ /users/{id} → 42             │  │
│  │ @RequestParam  │ Query string (?key=val)  │ ?page=2&size=10              │  │
│  │ @RequestBody   │ Request body             │ JSON/XML payload             │  │
│  │ @RequestHeader │ HTTP headers             │ Authorization, X-Request-ID  │  │
│  │ @CookieValue   │ HTTP cookies             │ JSESSIONID=abc123            │  │
│  │ @ModelAttribute│ Form data + path vars    │ HTML form submission         │  │
│  └────────────────┴──────────────────────────┴───────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Dependency Injection & Bean Management Annotations

---

#### 14. `@Component`

The **generic** stereotype annotation that marks a class as a Spring-managed **bean**. When `@ComponentScan` detects it, Spring creates an instance and registers it in the IoC container.

```java
// Source:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Indexed     // speeds up component scanning (Spring 5+)
public @interface Component {
    String value() default "";   // optional bean name
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Component — The Root of All Stereotypes:                                       │
│                                                                                  │
│  @Component                                                                      │
│      ├── @Service          (business logic layer)                                │
│      ├── @Repository       (data access layer)                                  │
│      ├── @Controller       (web layer — returns views)                          │
│      │     └── @RestController  (@Controller + @ResponseBody)                   │
│      └── @Configuration    (bean definition class)                              │
│                                                                                  │
│  ALL of these are @Component at their core.                                     │
│  They all get detected by @ComponentScan.                                       │
│  They all become Spring-managed beans.                                          │
│                                                                                  │
│  The specialisations exist for:                                                  │
│  1. Semantic clarity — communicate the ROLE of the class                        │
│  2. Layer-specific behaviour — e.g., @Repository adds exception translation     │
│  3. AOP targeting — you can write pointcuts that match @Service but not @Repo   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Basic @Component usage ───────────────────────────────────────────────────────
@Component    // Spring creates a singleton bean named "emailValidator"
public class EmailValidator {

    public boolean isValid(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}

// Other beans can inject it:
@Service
public class UserService {
    private final EmailValidator emailValidator;   // ← injected by Spring

    public UserService(EmailValidator emailValidator) {
        this.emailValidator = emailValidator;
    }
}


// ── Custom bean name ─────────────────────────────────────────────────────────────
@Component("myCustomValidator")   // bean name = "myCustomValidator" instead of "emailValidator"
public class EmailValidator { ... }


// ── When to use @Component vs specialised annotations ────────────────────────────
// Use @Component when the class doesn't clearly fit Service/Repository/Controller:
@Component
public class StartupInitializer {
    @PostConstruct
    public void init() {
        // run some initialization logic at startup
    }
}

@Component
public class CacheWarmer {
    @EventListener(ApplicationReadyEvent.class)
    public void warmCaches() {
        // pre-load caches after app starts
    }
}

@Component
public class JwtTokenProvider {
    // utility/infrastructure class — not clearly service/repo/controller
    public String generateToken(UserDetails user) { ... }
    public boolean validateToken(String token) { ... }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How @Component Gets Detected:                                                   │
│                                                                                  │
│  1. @SpringBootApplication includes @ComponentScan                              │
│  2. @ComponentScan scans the base package and sub-packages                      │
│  3. For each class found:                                                        │
│      → Is it annotated with @Component (or any meta-annotation)?               │
│      → YES → Create a BeanDefinition                                           │
│      → Register it in BeanDefinitionRegistry                                   │
│  4. BeanFactory instantiates the bean (singleton by default)                    │
│  5. Dependency injection resolves all @Autowired / constructor parameters       │
│  6. @PostConstruct runs                                                          │
│  7. Bean is ready for use                                                        │
│                                                                                  │
│  Default bean name = class name with first letter lowercase:                    │
│  EmailValidator → "emailValidator"                                              │
│  JwtTokenProvider → "jwtTokenProvider"                                          │
│  Unless overridden: @Component("customName")                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 15. `@Service`

A **specialisation** of `@Component` for the **business logic / service layer**. Functionally identical to `@Component` — Spring treats it the same way — but it communicates intent: *"this class contains business logic"*.

```java
// Source:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component     // ← IS a @Component
public @interface Service {
    String value() default "";   // optional bean name
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Service — Purpose and Layer:                                                   │
│                                                                                  │
│  ┌─────────────────────┐                                                        │
│  │  @Controller /      │  ← Web Layer (HTTP handling)                           │
│  │  @RestController     │     Receives requests, delegates to services           │
│  └─────────┬───────────┘                                                        │
│            │ calls                                                               │
│            ▼                                                                     │
│  ┌─────────────────────┐                                                        │
│  │  @Service            │  ← Business Logic Layer ★                             │
│  │                      │     Orchestrates business rules                        │
│  │                      │     Transaction boundaries (@Transactional)            │
│  │                      │     Validation, computation, coordination              │
│  └─────────┬───────────┘                                                        │
│            │ calls                                                               │
│            ▼                                                                     │
│  ┌─────────────────────┐                                                        │
│  │  @Repository         │  ← Data Access Layer                                  │
│  │                      │     Database queries, CRUD operations                  │
│  └─────────────────────┘                                                        │
│                                                                                  │
│  @Service adds NO special behaviour over @Component.                            │
│  It is purely semantic — it tells developers "this is business logic".          │
│  You CAN use @Component instead, but @Service is conventional.                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Typical @Service usage ───────────────────────────────────────────────────────
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    // constructor injection (preferred — no @Autowired needed for single constructor)
    public OrderService(OrderRepository orderRepository,
                        PaymentService paymentService,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        // 1. Validate
        validateOrder(request);

        // 2. Business logic
        Order order = Order.from(request);
        order.setStatus(OrderStatus.PENDING);

        // 3. Persist
        Order saved = orderRepository.save(order);

        // 4. Side effects
        paymentService.charge(saved.getPaymentDetails());
        notificationService.sendOrderConfirmation(saved);

        return OrderResponse.from(saved);
    }

    public OrderResponse findById(Long id) {
        return orderRepository.findById(id)
            .map(OrderResponse::from)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
    }

    private void validateOrder(OrderRequest request) {
        if (request.getItems().isEmpty()) {
            throw new ValidationException("Order must have at least one item");
        }
    }
}


// ── @Service vs @Component — when to use which ──────────────────────────────────
@Service        // ✓ Business logic: use @Service
public class UserService { ... }

@Service        // ✓ Business orchestration: use @Service
public class PaymentService { ... }

@Component      // ✓ Infrastructure/utility: use @Component
public class JwtTokenProvider { ... }

@Component      // ✓ Not clearly a "service": use @Component
public class ApplicationStartupRunner { ... }
```

---

#### 16. `@Repository`

A **specialisation** of `@Component` for the **data access / persistence layer**. Unlike `@Service`, `@Repository` adds **real behaviour**: automatic **exception translation** — it converts platform-specific persistence exceptions (JDBC `SQLException`, JPA `PersistenceException`, Hibernate `HibernateException`) into Spring's `DataAccessException` hierarchy.

```java
// Source:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component     // ← IS a @Component
public @interface Repository {
    String value() default "";   // optional bean name
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Repository — Exception Translation:                                            │
│                                                                                  │
│  Without @Repository:                                                            │
│  UserDao.findById(42)                                                            │
│      → JPA throws: javax.persistence.PersistenceException                       │
│      → Hibernate throws: org.hibernate.exception.ConstraintViolationException   │
│      → JDBC throws: java.sql.SQLException                                       │
│  → These are different exceptions for different persistence technologies!       │
│  → Your service layer must catch technology-specific exceptions.                │
│                                                                                  │
│  With @Repository:                                                               │
│  Spring wraps the bean in a proxy (PersistenceExceptionTranslationPostProcessor)│
│      → JPA PersistenceException → Spring DataAccessException                   │
│      → Hibernate HibernateException → Spring DataAccessException               │
│      → JDBC SQLException → Spring DataAccessException                          │
│  → ALL mapped to Spring's DataAccessException hierarchy!                        │
│  → Your service layer catches ONE consistent exception type.                    │
│                                                                                  │
│  Spring DataAccessException hierarchy:                                           │
│  DataAccessException (abstract, unchecked)                                       │
│      ├── DataIntegrityViolationException    (unique constraint, FK violation)    │
│      ├── EmptyResultDataAccessException     (expected result but got none)       │
│      ├── DuplicateKeyException              (insert duplicate primary key)       │
│      ├── DataRetrievalFailureException      (cannot retrieve data)              │
│      ├── OptimisticLockingFailureException  (version conflict)                  │
│      ├── DeadlockLoserDataAccessException   (database deadlock)                 │
│      └── QueryTimeoutException              (query exceeded timeout)            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Custom repository with @Repository ───────────────────────────────────────────
@Repository
public class UserDao {

    @PersistenceContext
    private EntityManager entityManager;

    public User findById(Long id) {
        User user = entityManager.find(User.class, id);
        if (user == null) {
            throw new EntityNotFoundException("User not found: " + id);
        }
        return user;
        // If JPA throws PersistenceException, Spring translates it to
        // DataAccessException automatically because of @Repository
    }

    public List<User> findByEmail(String email) {
        return entityManager.createQuery(
            "SELECT u FROM User u WHERE u.email = :email", User.class)
            .setParameter("email", email)
            .getResultList();
    }

    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        }
        return entityManager.merge(user);
    }
}


// ── Spring Data JPA repositories (most common pattern) ───────────────────────────
// Spring Data JPA interfaces are ALREADY treated as @Repository
// You do NOT need to add @Repository to them — it's automatic

public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data generates the implementation at runtime
    // Exception translation is built in
    Optional<User> findByEmail(String email);
    List<User> findByRoleAndActiveTrue(Role role);

    @Query("SELECT u FROM User u WHERE u.department.id = :deptId")
    List<User> findByDepartment(@Param("deptId") Long departmentId);
}

// You only need @Repository on CUSTOM DAO classes that use EntityManager/JdbcTemplate directly


// ── @Repository with JdbcTemplate ────────────────────────────────────────────────
@Repository
public class ProductJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Product findById(Long id) {
        return jdbcTemplate.queryForObject(
            "SELECT id, name, price FROM products WHERE id = ?",
            (rs, rowNum) -> new Product(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getBigDecimal("price")
            ),
            id
        );
        // If SQLException is thrown, @Repository translates it to
        // DataAccessException (e.g., EmptyResultDataAccessException if no row found)
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Component vs @Service vs @Repository — Summary:                                │
│                                                                                  │
│  ┌────────────────┬──────────────────┬───────────────────────────────────────┐  │
│  │ Annotation     │ Layer            │ Extra Behaviour                       │  │
│  ├────────────────┼──────────────────┼───────────────────────────────────────┤  │
│  │ @Component     │ Any / generic    │ None — just registers as a bean      │  │
│  │ @Service       │ Business logic   │ None — purely semantic               │  │
│  │ @Repository    │ Data access      │ Exception translation (real!)        │  │
│  │ @Controller    │ Web (views)      │ Handler mapping + view resolution    │  │
│  │ @RestController│ Web (API)        │ @Controller + @ResponseBody          │  │
│  │ @Configuration │ Bean definitions │ Full CGLIB proxying for @Bean        │  │
│  └────────────────┴──────────────────┴───────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17. `@Autowired`

Tells Spring to **inject a dependency** automatically. Spring finds a matching bean in the IoC container and injects it into the annotated field, constructor, or setter.

```java
// Source:
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER,
         ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
    boolean required() default true;   // if true and no bean found → startup failure
}
```

##### Properties / Attributes

```text
┌──────────────────────┬───────────────────────────────────────────────────────────┐
│ Attribute            │ Description                                               │
├──────────────────────┼───────────────────────────────────────────────────────────┤
│ required             │ Default: true. If true, Spring throws                     │
│                      │ NoSuchBeanDefinitionException at startup if no matching   │
│                      │ bean exists. If false, the field/param is left null.      │
└──────────────────────┴───────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Autowired — Three Injection Styles:                                            │
│                                                                                  │
│  1. CONSTRUCTOR INJECTION (★ RECOMMENDED)                                       │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ @Service                                                                │    │
│  │ public class OrderService {                                             │    │
│  │     private final UserRepository userRepo;  ← final = immutable       │    │
│  │     private final PaymentService payment;   ← final = immutable       │    │
│  │                                                                         │    │
│  │     // @Autowired is OPTIONAL when there is only ONE constructor       │    │
│  │     public OrderService(UserRepository userRepo, PaymentService pay) { │    │
│  │         this.userRepo = userRepo;                                       │    │
│  │         this.payment = pay;                                             │    │
│  │     }                                                                   │    │
│  │ }                                                                       │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│  ✓ Fields can be final (immutable)                                              │
│  ✓ Dependencies explicit in constructor signature                               │
│  ✓ Easy to unit test (just pass mocks in constructor)                           │
│  ✓ Fails fast if dependency missing (compile-time for mandatory deps)           │
│                                                                                  │
│  2. FIELD INJECTION (discouraged but common in tutorials)                       │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ @Service                                                                │    │
│  │ public class OrderService {                                             │    │
│  │     @Autowired                                                          │    │
│  │     private UserRepository userRepo;   ← NOT final                    │    │
│  │     @Autowired                                                          │    │
│  │     private PaymentService payment;    ← NOT final                    │    │
│  │ }                                                                       │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│  ✗ Cannot be final                                                              │
│  ✗ Hides dependencies (no constructor contract)                                 │
│  ✗ Hard to unit test (need reflection or @InjectMocks)                          │
│  ✗ Can lead to circular dependencies (harder to detect)                         │
│                                                                                  │
│  3. SETTER INJECTION (rare — for optional dependencies)                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │ @Service                                                                │    │
│  │ public class OrderService {                                             │    │
│  │     private NotificationService notifications;                          │    │
│  │                                                                         │    │
│  │     @Autowired(required = false)                                        │    │
│  │     public void setNotificationService(NotificationService svc) {       │    │
│  │         this.notifications = svc;                                       │    │
│  │     }                                                                   │    │
│  │ }                                                                       │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│  ○ Use when the dependency is truly optional                                    │
│  ○ Allows reconfiguration after construction (rare need)                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### How Spring Resolves `@Autowired` — Bean Matching

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Autowired — Resolution Algorithm:                                              │
│                                                                                  │
│  Step 1: Match by TYPE                                                           │
│  → Spring looks for a bean of the required type in the container                │
│  → If exactly ONE bean matches → inject it ✓                                   │
│                                                                                  │
│  Step 2: If MULTIPLE beans of the same type exist → AMBIGUITY                   │
│  → Spring narrows down using:                                                   │
│                                                                                  │
│    a) @Primary — marks one bean as the default choice                           │
│       @Primary @Bean                                                             │
│       public DataSource primaryDataSource() { ... }                             │
│                                                                                  │
│    b) @Qualifier("name") — specifies exactly which bean to inject               │
│       @Autowired @Qualifier("secondary")                                         │
│       private DataSource dataSource;                                             │
│                                                                                  │
│    c) Field/parameter name matching — if the variable name matches a bean name  │
│       @Autowired                                                                 │
│       private DataSource secondaryDataSource;  // matches bean "secondaryDS"?   │
│                                                                                  │
│  Step 3: If STILL ambiguous → NoUniqueBeanDefinitionException at startup        │
│  Step 4: If NO bean found and required=true → NoSuchBeanDefinitionException     │
│  Step 5: If NO bean found and required=false → field remains null               │
│                                                                                  │
│  Resolution priority: @Qualifier > @Primary > field name matching              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Constructor injection — @Autowired is OPTIONAL for single constructor ────────
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Since there is only ONE constructor, @Autowired is implied (Spring 4.3+)
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}


// ── Multiple constructors — @Autowired required to pick one ──────────────────────
@Service
public class NotificationService {
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    @Autowired    // ← tells Spring to use THIS constructor
    public NotificationService(EmailSender emailSender, SmsSender smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    public NotificationService(EmailSender emailSender) {
        this(emailSender, null);
    }
}


// ── @Qualifier — when multiple beans of the same type exist ──────────────────────
@Configuration
public class DataSourceConfig {

    @Bean("primaryDS")
    @Primary      // ← default choice when no @Qualifier specified
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:mysql://primary-host:3306/db")
            .build();
    }

    @Bean("replicaDS")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:mysql://replica-host:3306/db")
            .build();
    }
}

@Service
public class ReportService {

    private final DataSource primaryDs;
    private final DataSource replicaDs;

    public ReportService(
            @Qualifier("primaryDS") DataSource primaryDs,    // inject specific bean
            @Qualifier("replicaDS") DataSource replicaDs) {
        this.primaryDs = primaryDs;
        this.replicaDs = replicaDs;
    }
}


// ── @Autowired with required = false ─────────────────────────────────────────────
@Service
public class CacheService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    // If Redis is not configured (no RedisTemplate bean), this stays null
    // No startup error

    public Object getCached(String key) {
        if (redisTemplate == null) {
            return null;     // graceful fallback — no Redis available
        }
        return redisTemplate.opsForValue().get(key);
    }
}


// ── Inject a collection of all implementations ──────────────────────────────────
public interface PaymentGateway {
    boolean supports(String method);
    PaymentResult process(PaymentRequest request);
}

@Component public class StripeGateway implements PaymentGateway { ... }
@Component public class PayPalGateway implements PaymentGateway { ... }
@Component public class BankTransferGateway implements PaymentGateway { ... }

@Service
public class PaymentService {

    private final List<PaymentGateway> gateways;   // ALL implementations injected

    public PaymentService(List<PaymentGateway> gateways) {
        this.gateways = gateways;
        // gateways = [StripeGateway, PayPalGateway, BankTransferGateway]
    }

    public PaymentResult process(String method, PaymentRequest request) {
        return gateways.stream()
            .filter(g -> g.supports(method))
            .findFirst()
            .orElseThrow(() -> new UnsupportedPaymentMethodException(method))
            .process(request);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Autowired — Internally:                                                        │
│                                                                                  │
│  AutowiredAnnotationBeanPostProcessor processes @Autowired annotations:          │
│                                                                                  │
│  1. postProcessMergedBeanDefinition() — scans for @Autowired fields/methods     │
│  2. postProcessProperties() — performs actual injection:                         │
│     a) For each @Autowired injection point:                                     │
│        → Resolve the dependency using DefaultListableBeanFactory               │
│        → Match by type → then @Qualifier → then @Primary → then name          │
│     b) If constructor injection:                                                 │
│        → ConstructorResolver picks the @Autowired constructor                  │
│        → Resolves all parameters before calling constructor                     │
│     c) If field injection:                                                       │
│        → Uses reflection: field.set(bean, resolvedDependency)                  │
│     d) If setter injection:                                                      │
│        → Invokes the setter method with the resolved dependency                │
│                                                                                  │
│  Circular dependency detection:                                                  │
│  → Constructor injection: immediate failure (cannot resolve circular ref)       │
│  → Field/setter injection: Spring uses early reference (3-level cache)          │
│    but circular dependencies are a CODE SMELL — refactor instead!              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18. `@Bean`

Marks a **method** (inside a `@Configuration` or `@Component` class) as a **bean producer**. The return value of the method is registered as a bean in the Spring IoC container. Use `@Bean` when you cannot annotate the class itself (third-party classes) or when bean creation requires complex logic.

```java
// Source:
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
    String[] value()       default {};    // bean name(s); default = method name
    String[] name()        default {};    // alias for value
    boolean  autowireCandidate() default true;  // can this bean be autowired?
    String   initMethod()  default "";    // method to call after construction
    String   destroyMethod() default "(inferred)"; // method to call on shutdown
}
```

##### Properties / Attributes

```text
┌──────────────────────┬───────────────────────────────────────────────────────────┐
│ Attribute            │ Description                                               │
├──────────────────────┼───────────────────────────────────────────────────────────┤
│ value / name         │ Bean name(s). Default: method name.                       │
│                      │ @Bean("myDS") or @Bean({"ds", "dataSource"})             │
│ autowireCandidate    │ If false, bean won't be considered for @Autowired.        │
│                      │ Still accessible via applicationContext.getBean().        │
│ initMethod           │ Name of a no-arg method on the bean to call after         │
│                      │ properties are set (alternative to @PostConstruct).       │
│ destroyMethod        │ Name of a no-arg method to call on shutdown               │
│                      │ (alternative to @PreDestroy). Default: inferred — looks  │
│                      │ for close() or shutdown() methods automatically.          │
└──────────────────────┴───────────────────────────────────────────────────────────┘
```

```java
// ── Basic @Bean — registering third-party classes ────────────────────────────────
@Configuration
public class AppConfig {

    @Bean   // bean name = "objectMapper" (method name)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    @Bean   // bean name = "restTemplate"
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Spring injects RestTemplateBuilder automatically
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
        // BCryptPasswordEncoder is a third-party class — cannot add @Component to it
        // @Bean is the only way to register it
    }
}


// ── Custom bean name ─────────────────────────────────────────────────────────────
@Bean("primaryDataSource")                // custom name
public DataSource primaryDataSource() { ... }

@Bean(name = {"ds", "dataSource"})        // multiple names (aliases)
public DataSource dataSource() { ... }


// ── initMethod and destroyMethod ─────────────────────────────────────────────────
@Bean(initMethod = "start", destroyMethod = "stop")
public ConnectionPool connectionPool() {
    ConnectionPool pool = new ConnectionPool();
    pool.setMaxSize(20);
    return pool;
    // Spring calls pool.start() after creation
    // Spring calls pool.stop() on application shutdown
}


// ── @Bean with @Scope ────────────────────────────────────────────────────────────
@Bean
@Scope("prototype")    // new instance every time it's injected/requested
public HttpClient httpClient() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
}

@Bean
@Scope("singleton")    // default — single instance shared everywhere
public CacheManager cacheManager() { ... }


// ── @Bean with dependencies (method parameters) ─────────────────────────────────
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,                     // ← Spring injects this
            JwtTokenProvider tokenProvider) {       // ← Spring injects this
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}


// ── @Bean with @Primary ──────────────────────────────────────────────────────────
@Configuration
public class MessageConfig {

    @Bean
    @Primary    // default when @Autowired without @Qualifier
    public MessageService emailMessageService() {
        return new EmailMessageService();
    }

    @Bean
    public MessageService smsMessageService() {
        return new SmsMessageService();
    }
}

@Service
public class NotificationService {
    private final MessageService messageService;

    public NotificationService(MessageService messageService) {
        // emailMessageService injected (it's @Primary)
        this.messageService = messageService;
    }
}


// ── @Bean with @ConditionalOnProperty ────────────────────────────────────────────
@Bean
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("users", "products");
}
// Bean only created if application.properties has: cache.enabled=true
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Bean vs @Component — When to Use Each:                                         │
│                                                                                  │
│  ┌──────────────────────────────────┬────────────────────────────────────────┐  │
│  │ @Component (@Service, @Repo...)  │ @Bean                                  │  │
│  ├──────────────────────────────────┼────────────────────────────────────────┤  │
│  │ Goes on the CLASS itself         │ Goes on a METHOD in @Configuration     │  │
│  │ Auto-detected by @ComponentScan  │ Explicitly declared by developer       │  │
│  │ For YOUR OWN classes             │ For THIRD-PARTY or complex creation   │  │
│  │ Simple instantiation             │ Custom instantiation logic             │  │
│  │ One bean per annotated class     │ Multiple beans of same type possible   │  │
│  └──────────────────────────────────┴────────────────────────────────────────┘  │
│                                                                                  │
│  Use @Component when:                                                            │
│  → You own the class and can annotate it                                        │
│  → Simple no-arg or constructor-injected bean                                   │
│                                                                                  │
│  Use @Bean when:                                                                 │
│  → Third-party class (can't modify source to add @Component)                   │
│  → Complex creation logic (builder pattern, factory, conditional)               │
│  → Multiple beans of the same type (e.g., two DataSources)                     │
│  → Need to call init/destroy methods on a class you don't control              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 19. `@Configuration`

Marks a class as a **source of bean definitions**. A `@Configuration` class is a special kind of `@Component` that Spring **CGLIB-proxies** to ensure that `@Bean` methods behave correctly — specifically, that calling a `@Bean` method from another `@Bean` method returns the **same singleton instance** instead of creating a new object.

```java
// Source:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component     // ← IS a @Component (auto-detected by @ComponentScan)
public @interface Configuration {
    String  value()            default "";       // bean name
    boolean proxyBeanMethods() default true;     // CGLIB proxy (true) or lite mode (false)
}
```

##### Properties / Attributes

```text
┌──────────────────────┬───────────────────────────────────────────────────────────┐
│ Attribute            │ Description                                               │
├──────────────────────┼───────────────────────────────────────────────────────────┤
│ value                │ Bean name (default = lowercase class name)                │
│ proxyBeanMethods     │ true (default): CGLIB proxy ensures @Bean method calls    │
│                      │ between beans return the SAME singleton instance.         │
│                      │ false ("lite mode"): No proxy — @Bean methods are plain  │
│                      │ Java calls. Faster startup but inter-@Bean calls create  │
│                      │ NEW instances (not singletons). Use when beans don't     │
│                      │ cross-reference each other.                               │
└──────────────────────┴───────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Configuration — CGLIB Proxy (proxyBeanMethods = true):                         │
│                                                                                  │
│  @Configuration                                                                  │
│  public class AppConfig {                                                        │
│      @Bean                                                                       │
│      public DataSource dataSource() {                                            │
│          return new HikariDataSource();   // creates ONE instance               │
│      }                                                                           │
│      @Bean                                                                       │
│      public JdbcTemplate jdbcTemplate() {                                        │
│          return new JdbcTemplate(dataSource());   // ← calls dataSource()       │
│      }                                                                           │
│      @Bean                                                                       │
│      public NamedParameterJdbcTemplate namedTemplate() {                         │
│          return new NamedParameterJdbcTemplate(dataSource()); // ← same call    │
│      }                                                                           │
│  }                                                                               │
│                                                                                  │
│  WITHOUT proxy: dataSource() called 3 times → 3 DIFFERENT DataSource objects!  │
│  WITH proxy (default): dataSource() always returns the SAME singleton instance  │
│                                                                                  │
│  How? Spring subclasses AppConfig via CGLIB:                                     │
│  class AppConfig$$EnhancerBySpringCGLIB extends AppConfig {                      │
│      @Override                                                                   │
│      public DataSource dataSource() {                                            │
│          if (beanFactory.containsSingleton("dataSource")) {                      │
│              return beanFactory.getBean("dataSource"); // ← return existing     │
│          }                                                                       │
│          return super.dataSource();  // ← create for first time only            │
│      }                                                                           │
│  }                                                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Full @Configuration class ────────────────────────────────────────────────────
@Configuration
public class PersistenceConfig {

    @Bean
    @Primary
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
        // Spring injects the DataSource bean via parameter (preferred approach)
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}


// ── Lite mode (proxyBeanMethods = false) — faster startup ────────────────────────
@Configuration(proxyBeanMethods = false)
public class UtilityConfig {
    // Use lite mode when beans DON'T call other @Bean methods internally
    // No CGLIB proxy → faster startup, less memory

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule());
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}


// ── Importing other @Configuration classes ───────────────────────────────────────
@Configuration
@Import({SecurityConfig.class, CacheConfig.class})   // merge beans from these
public class AppConfig { ... }


// ── Conditional @Configuration ───────────────────────────────────────────────────
@Configuration
@ConditionalOnProperty(name = "feature.caching", havingValue = "true")
public class CacheConfig {
    // entire @Configuration class (and all its @Beans) only loads
    // if application.properties has: feature.caching=true
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users", "products");
    }
}
```

---

#### 20. `@ComponentScan`

Tells Spring **where to look** for `@Component` (and its specialisations: `@Service`, `@Repository`, `@Controller`, `@Configuration`) annotated classes. Without `@ComponentScan`, Spring won't discover your beans.

```java
// Source:
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ComponentScans.class)
public @interface ComponentScan {
    String[]  value()              default {};    // alias for basePackages
    String[]  basePackages()       default {};    // packages to scan
    Class<?>[] basePackageClasses() default {};   // type-safe alternative
    boolean   useDefaultFilters()  default true;  // auto-detect @Component etc.
    ComponentScan.Filter[] includeFilters() default {};  // add custom matches
    ComponentScan.Filter[] excludeFilters() default {};  // exclude specific classes
    boolean   lazyInit()           default false; // lazy-init all scanned beans?
    String    nameGenerator()      default "";    // custom bean naming strategy
    String    scopeResolver()      default "";    // custom scope resolver
    String    resourcePattern()    default "**/*.class";  // classpath resource pattern
}
```

##### Properties / Attributes

```text
┌──────────────────────────┬───────────────────────────────────────────────────────┐
│ Attribute                │ Description                                           │
├──────────────────────────┼───────────────────────────────────────────────────────┤
│ basePackages / value     │ Package(s) to scan. Default: package of the          │
│                          │ annotated class (and all sub-packages).              │
│ basePackageClasses       │ Type-safe alternative — the package of each class    │
│                          │ is used as a base package.                            │
│ useDefaultFilters        │ true: auto-detect @Component, @Service, @Repo, etc. │
│                          │ false: only detect classes matching includeFilters.  │
│ includeFilters           │ Additional patterns to match (even if not @Component)│
│ excludeFilters           │ Patterns to exclude from scanning.                   │
│ lazyInit                 │ true: all scanned beans are lazy-initialised.        │
│ resourcePattern          │ Default "**/*.class" — scans all .class files.       │
└──────────────────────────┴───────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @ComponentScan — How It's Already Included:                                     │
│                                                                                  │
│  @SpringBootApplication                                                          │
│      ├── @Configuration                                                          │
│      ├── @EnableAutoConfiguration                                                │
│      └── @ComponentScan ← ALREADY INCLUDED!                                    │
│                                                                                  │
│  Default behaviour: scans the PACKAGE of the @SpringBootApplication class       │
│  and ALL sub-packages.                                                           │
│                                                                                  │
│  com.example.myapp                                                               │
│  ├── MyAppApplication.java   ← @SpringBootApplication here                     │
│  ├── controller/             ← SCANNED ✓                                       │
│  │   └── UserController.java                                                    │
│  ├── service/                ← SCANNED ✓                                       │
│  │   └── UserService.java                                                       │
│  ├── repository/             ← SCANNED ✓                                       │
│  │   └── UserRepository.java                                                    │
│  └── config/                 ← SCANNED ✓                                       │
│      └── AppConfig.java                                                          │
│                                                                                  │
│  com.other.package            ← NOT SCANNED ✗ (different base package)          │
│      └── ExternalService.java                                                    │
│                                                                                  │
│  To scan com.other.package, you must explicitly add:                            │
│  @ComponentScan(basePackages = {"com.example.myapp", "com.other.package"})      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Default: @SpringBootApplication already includes @ComponentScan ──────────────
@SpringBootApplication    // scans com.example.myapp and all sub-packages
public class MyAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyAppApplication.class, args);
    }
}


// ── Custom base packages ─────────────────────────────────────────────────────────
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.myapp",        // your app's package
    "com.example.sharedlib",    // shared library package
    "com.thirdparty.components" // third-party components
})
public class MyAppApplication { ... }


// ── Type-safe base packages (refactoring-friendly) ───────────────────────────────
@SpringBootApplication
@ComponentScan(basePackageClasses = {
    MyAppApplication.class,        // com.example.myapp
    SharedLibMarker.class          // com.example.sharedlib
})
public class MyAppApplication { ... }
// Create empty marker classes/interfaces in each package root:
// package com.example.sharedlib;
// public interface SharedLibMarker {}


// ── Exclude specific components ──────────────────────────────────────────────────
@SpringBootApplication
@ComponentScan(
    basePackages = "com.example.myapp",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                              classes = LegacyService.class),
        @ComponentScan.Filter(type = FilterType.REGEX,
                              pattern = "com\\.example\\.myapp\\.internal\\..*"),
        @ComponentScan.Filter(type = FilterType.ANNOTATION,
                              classes = Deprecated.class)   // exclude @Deprecated beans
    }
)
public class MyAppApplication { ... }


// ── Custom include filters (register classes WITHOUT @Component) ─────────────────
@ComponentScan(
    basePackages = "com.example.myapp",
    useDefaultFilters = false,     // disable auto-detection of @Component
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = MyCustomAnnotation.class   // only scan @MyCustomAnnotation classes
    )
)
public class SpecialConfig { ... }
```

---

#### 21. `@Value`

Injects **values from external sources** — property files (`application.properties` / `application.yml`), environment variables, system properties, or SpEL expressions — directly into fields, constructor parameters, or method parameters.

```java
// Source:
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
         ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    String value();   // the expression to evaluate (no default — required)
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Value — Expression Types:                                                      │
│                                                                                  │
│  1. Property placeholder: ${property.name}                                      │
│     → Reads from application.properties/yml, env vars, system props             │
│                                                                                  │
│  2. Default value: ${property.name:default}                                     │
│     → Uses "default" if property is not found                                   │
│                                                                                  │
│  3. SpEL expression: #{expression}                                               │
│     → Evaluates a Spring Expression Language expression                         │
│                                                                                  │
│  Resolution order:                                                               │
│  1. Command line args: --server.port=9090                                       │
│  2. Java system properties: -Dserver.port=9090                                  │
│  3. OS environment variables: SERVER_PORT=9090                                  │
│  4. application-{profile}.properties/yml                                        │
│  5. application.properties/yml                                                   │
│  6. Default value in @Value("${prop:default}")                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── application.properties ───────────────────────────────────────────────────────
// app.name=My Spring App
// app.version=2.5.0
// app.max-retries=3
// app.api-url=https://api.example.com
// app.feature.enabled=true
// app.allowed-origins=http://localhost:3000,http://localhost:4200
// app.admin-emails=admin@example.com,support@example.com


// ── Basic property injection ─────────────────────────────────────────────────────
@Service
public class AppService {

    @Value("${app.name}")              // injects "My Spring App"
    private String appName;

    @Value("${app.version}")           // injects "2.5.0"
    private String appVersion;

    @Value("${app.max-retries}")       // injects 3 (auto-converts String → int)
    private int maxRetries;

    @Value("${app.feature.enabled}")   // injects true (auto-converts String → boolean)
    private boolean featureEnabled;

    @Value("${app.api-url}")
    private String apiUrl;
}


// ── Default values (when property might not exist) ───────────────────────────────
@Service
public class ConfigService {

    @Value("${app.timeout:5000}")          // default: 5000 if property not found
    private int timeoutMs;

    @Value("${app.locale:en_US}")          // default: "en_US"
    private String locale;

    @Value("${app.debug:false}")           // default: false
    private boolean debugMode;

    @Value("${JAVA_HOME:not-set}")         // reads OS environment variable
    private String javaHome;

    @Value("${undefined.property:}")       // default: empty string ""
    private String optionalProperty;
}


// ── List and array injection ─────────────────────────────────────────────────────
@Service
public class CorsService {

    @Value("${app.allowed-origins}")       // comma-separated → List<String>
    private List<String> allowedOrigins;
    // = ["http://localhost:3000", "http://localhost:4200"]

    @Value("${app.admin-emails}")
    private String[] adminEmails;          // comma-separated → String[]
    // = ["admin@example.com", "support@example.com"]

    @Value("#{'${app.allowed-origins}'.split(',')}")   // SpEL: explicit split
    private List<String> origins;
}


// ── SpEL expressions ─────────────────────────────────────────────────────────────
@Service
public class SpELService {

    @Value("#{T(java.lang.Math).PI}")           // static field: 3.141592653589793
    private double pi;

    @Value("#{T(java.lang.Math).random()}")     // static method call
    private double randomValue;

    @Value("#{systemProperties['user.home']}")  // Java system property
    private String userHome;

    @Value("#{systemEnvironment['PATH']}")      // OS environment variable
    private String pathEnv;

    @Value("#{${app.max-retries} * 2}")         // arithmetic on a property
    private int doubleRetries;                   // 3 * 2 = 6

    @Value("#{new java.text.SimpleDateFormat('yyyy-MM-dd').format(new java.util.Date())}")
    private String todayDate;                    // "2026-05-12"

    @Value("#{@userService.getDefaultRole()}")   // call a method on another bean
    private String defaultRole;                  // calls userService.getDefaultRole()
}


// ── Constructor injection with @Value (preferred for immutability) ───────────────
@Service
public class ApiClient {

    private final String baseUrl;
    private final int timeout;
    private final String apiKey;

    public ApiClient(
            @Value("${api.base-url}") String baseUrl,
            @Value("${api.timeout:3000}") int timeout,
            @Value("${api.key}") String apiKey) {
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.apiKey = apiKey;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Value vs @ConfigurationProperties:                                             │
│                                                                                  │
│  @Value:                                                                         │
│  → Inject individual values one at a time                                       │
│  → Good for 1-3 properties                                                      │
│  → Supports SpEL expressions                                                    │
│  → No type safety validation at startup                                         │
│                                                                                  │
│  @ConfigurationProperties (see section 30):                                      │
│  → Bind an entire group of properties to a POJO                                │
│  → Good for many related properties                                             │
│  → Type-safe, validated at startup with @Validated                              │
│  → Supports nested objects, lists, maps                                         │
│                                                                                  │
│  // @Value approach:                                                             │
│  @Value("${mail.host}") private String host;                                    │
│  @Value("${mail.port}") private int port;                                       │
│  @Value("${mail.from}") private String from;                                    │
│                                                                                  │
│  // @ConfigurationProperties approach:                                           │
│  @ConfigurationProperties(prefix = "mail")                                      │
│  public class MailProperties {                                                   │
│      private String host;                                                        │
│      private int port;                                                           │
│      private String from;                                                        │
│      // getters, setters                                                         │
│  }                                                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 22. `@ConditionalOnProperty`

A **Spring Boot auto-configuration condition** that enables or disables bean registration based on the presence or value of a property in `application.properties` / `application.yml`. Part of the `@Conditional` family — the bean is only created if the condition is met.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnPropertyCondition.class)     // ← the condition evaluator
public @interface ConditionalOnProperty {
    String[] value()       default {};       // property name(s) (alias for name)
    String[] name()        default {};       // property name(s)
    String   prefix()      default "";       // property prefix (joined with name)
    String   havingValue() default "";       // expected value (default: not "false")
    boolean  matchIfMissing() default false; // match if property is NOT defined?
}
```

##### Properties / Attributes

```text
┌──────────────────────┬───────────────────────────────────────────────────────────┐
│ Attribute            │ Description                                               │
├──────────────────────┼───────────────────────────────────────────────────────────┤
│ name / value         │ Property name(s) to check.                                │
│                      │ Combined with prefix: prefix + "." + name                │
│ prefix               │ Common prefix for property names.                         │
│                      │ prefix="app.feature", name="caching" → app.feature.caching│
│ havingValue          │ Required value of the property.                            │
│                      │ Empty string (default) = property must exist and NOT be   │
│                      │ "false". "true" = must be exactly "true".                │
│ matchIfMissing       │ false (default): if property not defined → bean NOT made │
│                      │ true: if property not defined → bean IS created          │
└──────────────────────┴───────────────────────────────────────────────────────────┘
```

```java
// ── application.properties ───────────────────────────────────────────────────────
// feature.caching.enabled=true
// feature.notifications.enabled=false
// feature.metrics.type=prometheus


// ── Basic: enable/disable a bean based on a flag ─────────────────────────────────
@Bean
@ConditionalOnProperty(name = "feature.caching.enabled", havingValue = "true")
public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("users", "products");
}
// Bean created ONLY if feature.caching.enabled=true


// ── Using prefix ─────────────────────────────────────────────────────────────────
@Bean
@ConditionalOnProperty(prefix = "feature.caching", name = "enabled", havingValue = "true")
public CacheManager cacheManager() { ... }
// Same as: name = "feature.caching.enabled", havingValue = "true"


// ── matchIfMissing — default when property is absent ─────────────────────────────
@Bean
@ConditionalOnProperty(
    name = "feature.security.enabled",
    havingValue = "true",
    matchIfMissing = true     // ← if property not defined at all, bean IS created
)
public SecurityFilter securityFilter() {
    return new SecurityFilter();
}
// Bean created if: property not defined OR property = "true"
// Bean NOT created if: property = "false"
// Use case: feature ON by default, opt-out by setting to false


// ── Conditional @Configuration class (all beans inside are conditional) ──────────
@Configuration
@ConditionalOnProperty(name = "feature.notifications.enabled", havingValue = "true")
public class NotificationConfig {

    @Bean
    public EmailService emailService() { ... }   // only created if notifications enabled

    @Bean
    public SmsService smsService() { ... }        // only created if notifications enabled
}


// ── Multiple conditions with different values ────────────────────────────────────
@Bean
@ConditionalOnProperty(name = "feature.metrics.type", havingValue = "prometheus")
public MeterRegistry prometheusMeterRegistry() {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
}

@Bean
@ConditionalOnProperty(name = "feature.metrics.type", havingValue = "datadog")
public MeterRegistry datadogMeterRegistry() {
    return new DatadogMeterRegistry(datadogConfig, Clock.SYSTEM);
}
// Only ONE of these beans is created, depending on the property value
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Other @Conditional Annotations (same family):                                   │
│                                                                                  │
│  ┌────────────────────────────────────────┬──────────────────────────────────┐  │
│  │ Annotation                             │ Bean created when...             │  │
│  ├────────────────────────────────────────┼──────────────────────────────────┤  │
│  │ @ConditionalOnProperty                 │ Property has expected value      │  │
│  │ @ConditionalOnMissingBean              │ No bean of this type exists yet  │  │
│  │ @ConditionalOnBean                     │ A specific bean already exists   │  │
│  │ @ConditionalOnClass                    │ A class is on the classpath      │  │
│  │ @ConditionalOnMissingClass             │ A class is NOT on the classpath  │  │
│  │ @ConditionalOnExpression               │ SpEL expression evaluates true   │  │
│  │ @ConditionalOnJava                     │ Running on a specific Java ver   │  │
│  │ @ConditionalOnWebApplication           │ Running as a web application    │  │
│  │ @ConditionalOnNotWebApplication        │ NOT running as a web app        │  │
│  └────────────────────────────────────────┴──────────────────────────────────┘  │
│                                                                                  │
│  These are used EXTENSIVELY inside Spring Boot's auto-configuration classes.    │
│  They're what makes Spring Boot "opinionated but overridable".                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 23. `@Profile`

Activates a bean or `@Configuration` class **only when a specific profile is active**. Profiles allow you to have different beans for different environments (dev, test, staging, prod).

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ProfileCondition.class)     // ← evaluates active profiles
public @interface Profile {
    String[] value();   // profile name(s) — can use "!" for negation
}
```

##### How to Activate Profiles

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Activating Profiles:                                                            │
│                                                                                  │
│  1. application.properties:                                                      │
│     spring.profiles.active=dev                                                  │
│     spring.profiles.active=dev,metrics     (multiple profiles)                  │
│                                                                                  │
│  2. Command line:                                                                │
│     java -jar app.jar --spring.profiles.active=prod                             │
│                                                                                  │
│  3. Environment variable:                                                        │
│     SPRING_PROFILES_ACTIVE=prod java -jar app.jar                               │
│                                                                                  │
│  4. JVM system property:                                                         │
│     java -Dspring.profiles.active=prod -jar app.jar                             │
│                                                                                  │
│  5. Programmatically:                                                            │
│     SpringApplication app = new SpringApplication(MyApp.class);                 │
│     app.setAdditionalProfiles("dev");                                           │
│     app.run(args);                                                               │
│                                                                                  │
│  6. In tests:                                                                    │
│     @ActiveProfiles("test")                                                      │
│     @SpringBootTest                                                              │
│     class MyServiceTest { ... }                                                  │
│                                                                                  │
│  Profile-specific property files (auto-loaded when profile is active):          │
│  application-dev.properties     (loaded when dev profile is active)             │
│  application-prod.properties    (loaded when prod profile is active)            │
│  application-test.properties    (loaded when test profile is active)            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Different beans for different environments ───────────────────────────────────

// DEV: in-memory database
@Configuration
@Profile("dev")
public class DevDatabaseConfig {
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("schema.sql")
            .build();
    }
}

// PROD: real database with connection pooling
@Configuration
@Profile("prod")
public class ProdDatabaseConfig {
    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaximumPoolSize(30);
        return ds;
    }
}


// ── @Profile on individual @Bean methods ─────────────────────────────────────────
@Configuration
public class NotificationConfig {

    @Bean
    @Profile("dev")
    public EmailService emailService() {
        return new MockEmailService();       // dev: log emails instead of sending
    }

    @Bean
    @Profile("prod")
    public EmailService emailService() {
        return new SmtpEmailService();       // prod: actually send emails
    }
}


// ── @Profile with negation ("!") ─────────────────────────────────────────────────
@Bean
@Profile("!prod")       // active in ALL profiles EXCEPT prod
public DataSource embeddedDataSource() {
    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .build();
}

@Bean
@Profile("!test")       // active in ALL profiles EXCEPT test
public ScheduledTaskRunner taskRunner() { ... }


// ── Multiple profiles (OR logic) ─────────────────────────────────────────────────
@Bean
@Profile({"dev", "staging"})     // active if EITHER dev OR staging
public MockPaymentGateway paymentGateway() { ... }


// ── @Profile on @Service ─────────────────────────────────────────────────────────
public interface StorageService {
    void store(String key, byte[] data);
    byte[] retrieve(String key);
}

@Service
@Profile("dev")
public class LocalStorageService implements StorageService {
    // stores files in local filesystem during development
    @Override
    public void store(String key, byte[] data) {
        Files.write(Path.of("/tmp/storage/" + key), data);
    }
    @Override
    public byte[] retrieve(String key) {
        return Files.readAllBytes(Path.of("/tmp/storage/" + key));
    }
}

@Service
@Profile("prod")
public class S3StorageService implements StorageService {
    // stores files in AWS S3 in production
    private final AmazonS3 s3Client;
    // ...
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Profile vs @ConditionalOnProperty:                                             │
│                                                                                  │
│  @Profile("dev")                                                                 │
│  → Based on ACTIVE PROFILE (environment-wide setting)                           │
│  → Typically: dev, test, staging, prod                                          │
│  → Activates entire configuration for an environment                            │
│                                                                                  │
│  @ConditionalOnProperty(name="feature.x", havingValue="true")                   │
│  → Based on a SPECIFIC PROPERTY value                                           │
│  → Fine-grained feature toggle                                                  │
│  → Can be changed without switching profiles                                    │
│                                                                                  │
│  Use @Profile for: environment-specific configuration (DB, email, storage)      │
│  Use @ConditionalOnProperty for: feature flags (caching, metrics, scheduling)   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Lifecycle & Scope Annotations

---

#### 24. `@PostConstruct` and `@PreDestroy`

`@PostConstruct` marks a method to run **after** the bean is fully constructed and all dependencies are injected. `@PreDestroy` marks a method to run **before** the bean is destroyed (on application shutdown).

```java
// Source (Jakarta/javax annotation — not Spring-specific):
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostConstruct {}    // no attributes

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreDestroy {}       // no attributes
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Bean Lifecycle — Where @PostConstruct and @PreDestroy Fit:                      │
│                                                                                  │
│  1. Constructor called (dependencies injected via constructor)                  │
│  2. @Autowired field/setter injection                                            │
│  3. BeanPostProcessor.postProcessBeforeInitialization()                          │
│  4. ★ @PostConstruct method runs ← HERE                                        │
│  5. InitializingBean.afterPropertiesSet() (if implemented)                      │
│  6. @Bean(initMethod = "init") runs (if specified)                              │
│  7. BeanPostProcessor.postProcessAfterInitialization()                           │
│  8. ═══ Bean is ready for use ═══                                               │
│     ... application runs ...                                                     │
│  9. ★ @PreDestroy method runs ← HERE (on shutdown)                             │
│  10. DisposableBean.destroy() (if implemented)                                   │
│  11. @Bean(destroyMethod = "cleanup") runs (if specified)                       │
│  12. Bean is garbage collected                                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── @PostConstruct — initialization after injection ──────────────────────────────
@Service
public class CacheService {

    private final ProductRepository productRepository;
    private Map<Long, Product> cache;

    public CacheService(ProductRepository productRepository) {
        this.productRepository = productRepository;
        // DON'T use productRepository here — it might not be fully configured yet
    }

    @PostConstruct
    public void init() {
        // Safe to use injected dependencies here — they're fully ready
        this.cache = productRepository.findAll().stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        log.info("Cache warmed with {} products", cache.size());
    }
}


// ── @PreDestroy — cleanup before shutdown ────────────────────────────────────────
@Component
public class ConnectionManager {

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(10);
        log.info("Thread pool started");
    }

    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Thread pool shut down gracefully");
    }
}


// ── Validation at startup ────────────────────────────────────────────────────────
@Service
public class ExternalApiClient {

    @Value("${external.api.url}")
    private String apiUrl;

    @Value("${external.api.key}")
    private String apiKey;

    @PostConstruct
    public void validate() {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("external.api.url must be configured!");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("external.api.key must be configured!");
        }
        log.info("External API client configured for: {}", apiUrl);
    }
}
```

---

#### 25. `@Scope`

Defines the **lifecycle and visibility** of a bean — how many instances Spring creates and how long they live.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Scope {
    String value()        default "singleton";    // scope name
    String scopeName()    default "";             // alias for value
    ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Bean Scopes:                                                                    │
│                                                                                  │
│  ┌─────────────────┬─────────────────────────────────────────────────────────┐  │
│  │ Scope           │ Description                                             │  │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤  │
│  │ singleton       │ (DEFAULT) One instance per Spring container.            │  │
│  │                 │ Shared across all injections. Created at startup.       │  │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤  │
│  │ prototype       │ New instance every time the bean is requested.          │  │
│  │                 │ Spring does NOT manage the full lifecycle (no           │  │
│  │                 │ @PreDestroy). You are responsible for cleanup.          │  │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤  │
│  │ request         │ One instance per HTTP request (web app only).           │  │
│  │                 │ Destroyed when request completes.                       │  │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤  │
│  │ session         │ One instance per HTTP session (web app only).           │  │
│  │                 │ Destroyed when session expires/invalidated.             │  │
│  ├─────────────────┼─────────────────────────────────────────────────────────┤  │
│  │ application     │ One instance per ServletContext (web app only).         │  │
│  │                 │ Similar to singleton but scoped to servlet context.     │  │
│  └─────────────────┴─────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Singleton (default) ──────────────────────────────────────────────────────────
@Service    // singleton by default
public class UserService { ... }
// Same instance injected everywhere


// ── Prototype — new instance every time ──────────────────────────────────────────
@Component
@Scope("prototype")
public class ShoppingCart {
    private List<CartItem> items = new ArrayList<>();
    public void addItem(CartItem item) { items.add(item); }
}
// Every @Autowired injection gets a DIFFERENT ShoppingCart instance


// ── Request scope — per HTTP request ─────────────────────────────────────────────
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {
    private String correlationId;
    private Instant startTime;

    @PostConstruct
    public void init() {
        this.correlationId = UUID.randomUUID().toString();
        this.startTime = Instant.now();
    }
}
// proxyMode is REQUIRED when injecting a shorter-lived scope into a longer-lived bean
// (e.g., request-scoped into singleton)


// ── Session scope — per HTTP session ─────────────────────────────────────────────
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserPreferences {
    private String theme = "light";
    private String language = "en";
    // lives as long as the user's HTTP session
}


// ── @Scope on @Bean methods ──────────────────────────────────────────────────────
@Configuration
public class AppConfig {

    @Bean
    @Scope("prototype")
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
}
```

---

#### 26. `@Lazy`

Delays the creation of a bean **until it is first accessed** (first injection or first `getBean()` call), rather than at application startup. Useful for expensive beans that may not always be needed.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR,
         ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Lazy {
    boolean value() default true;   // true = lazy, false = eager (default)
}
```

```java
// ── On a bean class ──────────────────────────────────────────────────────────────
@Service
@Lazy       // not created at startup — only when first injected/used
public class ExpensiveReportGenerator {

    @PostConstruct
    public void init() {
        // This runs only when the bean is first accessed, NOT at startup
        log.info("Report generator initialized (this is expensive)");
        loadTemplates();
        warmCaches();
    }
}


// ── On an injection point ────────────────────────────────────────────────────────
@Service
public class OrderService {

    private final ExpensiveReportGenerator reportGenerator;

    public OrderService(@Lazy ExpensiveReportGenerator reportGenerator) {
        // Spring injects a PROXY, not the real bean
        // The real bean is created only when reportGenerator is actually USED
        this.reportGenerator = reportGenerator;
    }

    public byte[] generateReport(Long orderId) {
        // ← NOW the real ExpensiveReportGenerator is created (if not already)
        return reportGenerator.generate(orderId);
    }
}


// ── @Lazy to break circular dependencies (workaround, not recommended) ──────────
@Service
public class ServiceA {
    private final ServiceB serviceB;

    public ServiceA(@Lazy ServiceB serviceB) {
        // Proxy injected → breaks the circular dependency at construction time
        this.serviceB = serviceB;
    }
}
```

---

#### 27. `@Order` and `@Priority`

Controls the **ordering** of beans when multiple beans of the same type are injected as a `List`, or when multiple `@ControllerAdvice`, `Filter`, or `@EventListener` beans compete.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {
    int value() default Ordered.LOWEST_PRECEDENCE;   // Integer.MAX_VALUE
}
// Lower value = higher priority. Default = lowest priority.
```

```java
// ── Ordering filters ─────────────────────────────────────────────────────────────
@Component
@Order(1)    // runs FIRST
public class AuthenticationFilter implements Filter { ... }

@Component
@Order(2)    // runs SECOND
public class LoggingFilter implements Filter { ... }

@Component
@Order(3)    // runs THIRD
public class RateLimitFilter implements Filter { ... }


// ── Ordering in a List injection ─────────────────────────────────────────────────
public interface DataValidator { boolean validate(Data data); }

@Component @Order(1) public class FormatValidator implements DataValidator { ... }
@Component @Order(2) public class BusinessRuleValidator implements DataValidator { ... }
@Component @Order(3) public class SecurityValidator implements DataValidator { ... }

@Service
public class ValidationService {
    private final List<DataValidator> validators;   // injected in @Order order

    public ValidationService(List<DataValidator> validators) {
        this.validators = validators;
        // validators = [FormatValidator, BusinessRuleValidator, SecurityValidator]
    }
}


// ── @Order on @ControllerAdvice ──────────────────────────────────────────────────
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)    // checked FIRST
public class SecurityExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(...) { ... }
}

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)     // checked LAST (fallback)
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(...) { ... }
}
```

---

#### 28. `@Transactional`

Defines **transaction boundaries** on methods or classes. Spring wraps the annotated method in a database transaction — **committing** on success, **rolling back** on `RuntimeException` (unchecked) or `Error`.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
    String       value()             default "";          // transaction manager name
    Propagation  propagation()       default Propagation.REQUIRED;
    Isolation    isolation()         default Isolation.DEFAULT;
    int          timeout()           default -1;          // seconds (-1 = no timeout)
    String       timeoutString()     default "";
    boolean      readOnly()          default false;       // hint for optimisation
    Class<? extends Throwable>[] rollbackFor()    default {};  // rollback for these
    Class<? extends Throwable>[] noRollbackFor()  default {};  // don't rollback for these
    String[]     rollbackForClassName()   default {};
    String[]     noRollbackForClassName() default {};
}
```

##### Properties / Attributes

```text
┌────────────────────────┬─────────────────────────────────────────────────────────┐
│ Attribute              │ Description                                             │
├────────────────────────┼─────────────────────────────────────────────────────────┤
│ propagation            │ How this transaction relates to an existing one:        │
│                        │ REQUIRED (default): join existing or create new         │
│                        │ REQUIRES_NEW: always create new, suspend existing      │
│                        │ NESTED: nested transaction within existing              │
│                        │ SUPPORTS: use existing or run non-transactional        │
│                        │ MANDATORY: must run in existing, error if none         │
│                        │ NOT_SUPPORTED: always non-transactional                │
│                        │ NEVER: error if a transaction exists                   │
├────────────────────────┼─────────────────────────────────────────────────────────┤
│ isolation              │ Database isolation level:                               │
│                        │ DEFAULT, READ_UNCOMMITTED, READ_COMMITTED,             │
│                        │ REPEATABLE_READ, SERIALIZABLE                          │
├────────────────────────┼─────────────────────────────────────────────────────────┤
│ readOnly               │ true: hint for performance optimisation (no dirty      │
│                        │ checking, flush mode NEVER in Hibernate)               │
├────────────────────────┼─────────────────────────────────────────────────────────┤
│ timeout                │ Maximum time in seconds before auto-rollback           │
├────────────────────────┼─────────────────────────────────────────────────────────┤
│ rollbackFor            │ Checked exceptions that should trigger rollback        │
│                        │ (by default, only unchecked exceptions roll back)      │
├────────────────────────┼─────────────────────────────────────────────────────────┤
│ noRollbackFor          │ Runtime exceptions that should NOT trigger rollback    │
└────────────────────────┴─────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Transactional — How It Works Internally (AOP Proxy):                           │
│                                                                                  │
│  @Transactional                                                                  │
│  public void transferMoney(Long from, Long to, BigDecimal amount) { ... }       │
│                                                                                  │
│  Spring creates a PROXY around the bean:                                         │
│                                                                                  │
│  Caller → [TransactionInterceptor PROXY]                                        │
│      │                                                                           │
│      ▼                                                                           │
│  1. PlatformTransactionManager.getTransaction(definition)                       │
│      → Creates/joins a transaction                                              │
│      → Gets a database connection from DataSource                               │
│      → Sets connection.setAutoCommit(false)                                     │
│      │                                                                           │
│      ▼                                                                           │
│  2. Invoke the REAL method: transferMoney(from, to, amount)                     │
│      │                                                                           │
│      ├── If method completes normally:                                           │
│      │   → PlatformTransactionManager.commit()                                  │
│      │   → connection.commit()                                                  │
│      │   → return result to caller                                              │
│      │                                                                           │
│      └── If method throws RuntimeException or Error:                            │
│          → PlatformTransactionManager.rollback()                                │
│          → connection.rollback()                                                │
│          → re-throw exception to caller                                         │
│                                                                                  │
│  ⚠ IMPORTANT: @Transactional only works on PUBLIC methods!                      │
│  ⚠ IMPORTANT: Self-invocation bypasses the proxy — @Transactional is IGNORED   │
│    if you call a @Transactional method from WITHIN the same class.              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Basic @Transactional ─────────────────────────────────────────────────────────
@Service
public class BankService {

    private final AccountRepository accountRepository;

    public BankService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional    // all-or-nothing: both updates succeed or both roll back
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId)
            .orElseThrow(() -> new AccountNotFoundException(fromId));
        Account to = accountRepository.findById(toId)
            .orElseThrow(() -> new AccountNotFoundException(toId));

        from.debit(amount);      // throws InsufficientFundsException if not enough
        to.credit(amount);

        accountRepository.save(from);
        accountRepository.save(to);
        // If any exception here → BOTH saves are rolled back
    }
}


// ── readOnly for queries (performance optimisation) ──────────────────────────────
@Service
@Transactional(readOnly = true)   // class-level: all methods are read-only by default
public class ReportService {

    public List<Order> getMonthlyOrders(YearMonth month) {
        // Hibernate: no dirty checking, flush mode = NEVER
        // Database: may route to read replica
        return orderRepository.findByMonth(month);
    }

    @Transactional    // override: this method IS read-write
    public void markAsExported(List<Long> orderIds) {
        orderRepository.markExported(orderIds);
    }
}


// ── rollbackFor checked exceptions ───────────────────────────────────────────────
@Transactional(rollbackFor = Exception.class)   // rollback for ALL exceptions
public void processPayment(PaymentRequest request) throws PaymentException {
    // Without rollbackFor = Exception.class:
    //   throw new PaymentException("declined") → NO rollback (it's checked!)
    // With rollbackFor = Exception.class:
    //   throw new PaymentException("declined") → rollback ✓
}


// ── Propagation examples ─────────────────────────────────────────────────────────
@Transactional
public void placeOrder(OrderRequest request) {
    orderRepository.save(order);         // same transaction

    auditService.logAction("ORDER_PLACED");  // see below
}

@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action) {
        // Always runs in its OWN transaction
        // If placeOrder() rolls back, this audit log is STILL committed
        auditRepository.save(new AuditLog(action));
    }
}


// ── timeout ──────────────────────────────────────────────────────────────────────
@Transactional(timeout = 10)   // rollback if not complete within 10 seconds
public void longRunningBatchProcess() { ... }
```

---

#### 29. `@Async`

Marks a method to execute **asynchronously** — Spring runs it in a separate thread, and the caller returns immediately without waiting for it to complete.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {
    String value() default "";   // name of the specific TaskExecutor bean to use
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Async — Prerequisites:                                                         │
│                                                                                  │
│  1. Enable async: add @EnableAsync to a @Configuration class                    │
│  2. Return type must be: void, Future<T>, CompletableFuture<T>, ListenableFuture│
│  3. Method must be PUBLIC (AOP proxy requirement)                               │
│  4. Caller must be in a DIFFERENT class (self-invocation bypasses proxy)        │
│                                                                                  │
│  @Configuration                                                                  │
│  @EnableAsync        ← REQUIRED                                                │
│  public class AsyncConfig { }                                                    │
│                                                                                  │
│  OR add @EnableAsync to your @SpringBootApplication class.                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ── Enable async processing ──────────────────────────────────────────────────────
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}


// ── Fire-and-forget (void return) ────────────────────────────────────────────────
@Service
public class NotificationService {

    @Async
    public void sendWelcomeEmail(String email) {
        // Runs in a separate thread — caller does NOT wait
        emailClient.send(email, "Welcome!", "Welcome to our platform...");
        log.info("Welcome email sent to {}", email);
    }

    @Async
    public void sendSmsNotification(String phone, String message) {
        smsClient.send(phone, message);
    }
}

@Service
public class UserService {
    private final NotificationService notificationService;

    @Transactional
    public UserResponse register(UserRequest request) {
        User user = userRepository.save(User.from(request));

        // These run in background threads — register() returns immediately
        notificationService.sendWelcomeEmail(user.getEmail());
        notificationService.sendSmsNotification(user.getPhone(), "Welcome!");

        return UserResponse.from(user);   // returned before emails/SMS are sent
    }
}


// ── With return value (CompletableFuture) ────────────────────────────────────────
@Service
public class PricingService {

    @Async
    public CompletableFuture<BigDecimal> calculatePrice(Long productId) {
        // runs in background thread
        BigDecimal price = expensiveCalculation(productId);
        return CompletableFuture.completedFuture(price);
    }
}

@RestController
public class PricingController {

    @GetMapping("/prices")
    public ResponseEntity<PriceResponse> getPrices() throws Exception {
        // Launch 3 async calculations in parallel
        CompletableFuture<BigDecimal> price1 = pricingService.calculatePrice(1L);
        CompletableFuture<BigDecimal> price2 = pricingService.calculatePrice(2L);
        CompletableFuture<BigDecimal> price3 = pricingService.calculatePrice(3L);

        // Wait for all to complete
        CompletableFuture.allOf(price1, price2, price3).join();

        return ResponseEntity.ok(new PriceResponse(
            price1.get(), price2.get(), price3.get()
        ));
    }
}


// ── Using a specific TaskExecutor ────────────────────────────────────────────────
@Async("taskExecutor")      // uses the "taskExecutor" bean specifically
public void processInBackground() { ... }

@Async("emailExecutor")     // uses a different thread pool for emails
public void sendEmail(String to, String subject, String body) { ... }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Async — Internally (AOP Proxy):                                                │
│                                                                                  │
│  Caller → [AsyncExecutionInterceptor PROXY]                                     │
│      │                                                                           │
│      ▼                                                                           │
│  1. Proxy intercepts the method call                                             │
│  2. Gets the TaskExecutor (default: SimpleAsyncTaskExecutor or configured one)  │
│  3. Wraps the method call in a Runnable/Callable                                │
│  4. Submits to the executor's thread pool                                        │
│  5. Returns immediately to the caller:                                           │
│     → void method: returns null                                                 │
│     → Future/CompletableFuture: returns a Future proxy                          │
│  6. In the background thread: actual method executes                            │
│                                                                                  │
│  ⚠ Common pitfall: calling @Async from the SAME class                          │
│     this.sendEmail(...) → bypasses proxy → runs SYNCHRONOUSLY!                 │
│     Fix: inject the service and call through the injected reference             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Other Common Spring Boot Annotations

---

#### 30. `@ConfigurationProperties`

Binds an **entire group of external properties** to a type-safe Java POJO. Preferred over multiple `@Value` annotations when you have many related properties.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationProperties {
    String  value()  default "";     // alias for prefix
    String  prefix() default "";     // common prefix for all properties
    boolean ignoreInvalidFields() default false;
    boolean ignoreUnknownFields() default true;
}
```

```java
// ── application.yml ──────────────────────────────────────────────────────────────
// app:
//   mail:
//     host: smtp.example.com
//     port: 587
//     from: noreply@example.com
//     credentials:
//       username: admin
//       password: secret
//     recipients:
//       - admin@example.com
//       - support@example.com

@ConfigurationProperties(prefix = "app.mail")
@Validated    // enables validation
public class MailProperties {

    @NotBlank
    private String host;

    @Min(1) @Max(65535)
    private int port = 25;              // default value

    private String from;

    private Credentials credentials;
    private List<String> recipients;

    // nested object (binds app.mail.credentials.username, etc.)
    public static class Credentials {
        private String username;
        private String password;
        // getters, setters...
    }

    // getters, setters...
}


// ── Enable it ────────────────────────────────────────────────────────────────────
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    public JavaMailSender mailSender(MailProperties props) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.getHost());
        sender.setPort(props.getPort());
        sender.setUsername(props.getCredentials().getUsername());
        sender.setPassword(props.getCredentials().getPassword());
        return sender;
    }
}
```

---

#### 31. `@Scheduled` and `@EnableScheduling`

Marks a method to run on a **fixed schedule** — cron expressions, fixed delay, or fixed rate.

```java
// Source:
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {
    String cron()           default "";       // cron expression
    String zone()           default "";       // timezone for cron
    long   fixedDelay()     default -1;       // ms between end and next start
    long   fixedRate()      default -1;       // ms between starts
    long   initialDelay()   default -1;       // delay before first execution
    String fixedDelayString()   default "";   // same but from property
    String fixedRateString()    default "";
    String initialDelayString() default "";
}
```

```java
// ── Enable scheduling ────────────────────────────────────────────────────────────
@Configuration
@EnableScheduling
public class SchedulerConfig { }


// ── Fixed rate — runs every N milliseconds (regardless of previous execution) ────
@Component
public class HealthChecker {

    @Scheduled(fixedRate = 30000)   // every 30 seconds
    public void checkHealth() {
        log.info("Health check at {}", Instant.now());
    }

    @Scheduled(fixedDelay = 60000)   // 60 seconds AFTER the previous execution FINISHES
    public void processQueue() {
        // Good for tasks where you don't want overlap
    }

    @Scheduled(cron = "0 0 2 * * ?")   // every day at 2:00 AM
    public void nightlyCleanup() {
        cleanupService.removeExpiredData();
    }

    @Scheduled(cron = "0 */15 * * * ?")   // every 15 minutes
    public void syncData() { ... }

    @Scheduled(fixedRateString = "${app.poll-interval:5000}")   // from properties
    public void pollExternalApi() { ... }
}
```

---

#### 32. `@EventListener` and Application Events

Marks a method as a **listener** for Spring application events — both built-in events (startup, shutdown) and custom events.

```java
// Source:
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {
    Class<?>[] value()     default {};    // event types to handle
    String     condition() default "";    // SpEL condition to filter events
}
```

```java
// ── Built-in application events ──────────────────────────────────────────────────
@Component
public class AppEventListeners {

    @EventListener(ApplicationReadyEvent.class)
    public void onAppReady() {
        // Fires after app is fully started and ready to serve requests
        log.info("Application started successfully!");
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        log.info("Application shutting down...");
    }
}


// ── Custom events ────────────────────────────────────────────────────────────────
// 1. Define the event
public class OrderPlacedEvent {
    private final Long orderId;
    private final String customerEmail;
    public OrderPlacedEvent(Long orderId, String customerEmail) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
    }
    // getters...
}

// 2. Publish the event
@Service
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.from(request));
        eventPublisher.publishEvent(
            new OrderPlacedEvent(order.getId(), request.getEmail()));
        return OrderResponse.from(order);
    }
}

// 3. Listen for the event
@Component
public class OrderEventListener {

    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Order {} placed by {}", event.getOrderId(), event.getCustomerEmail());
        notificationService.sendOrderConfirmation(event.getCustomerEmail());
    }

    @Async
    @EventListener
    public void handleOrderPlacedAsync(OrderPlacedEvent event) {
        // runs asynchronously in a separate thread
        analyticsService.trackOrder(event.getOrderId());
    }

    @EventListener(condition = "#event.orderId > 1000")    // SpEL condition
    public void handleLargeOrders(OrderPlacedEvent event) {
        alertService.notifyVIP(event);
    }
}


// ── @TransactionalEventListener ──────────────────────────────────────────────────
@Component
public class PostCommitListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterOrderCommitted(OrderPlacedEvent event) {
        // Only runs AFTER the transaction that published the event is COMMITTED
        // If the transaction rolls back, this listener is NOT called
        emailService.sendConfirmation(event.getCustomerEmail());
    }
}
```

---

#### 33. `@ResponseStatus`

Marks a method or exception class with the **HTTP status code** and reason that should be returned.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseStatus {
    HttpStatus value()   default HttpStatus.INTERNAL_SERVER_ERROR;
    HttpStatus code()    default HttpStatus.INTERNAL_SERVER_ERROR;   // alias
    String     reason()  default "";   // optional reason phrase
}
```

```java
// ── On exception classes ─────────────────────────────────────────────────────────
@ResponseStatus(HttpStatus.NOT_FOUND)     // 404 whenever this is thrown
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Resource already exists")
public class DuplicateResourceException extends RuntimeException { ... }

// Throw from any controller:
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    // → HTTP 404 Not Found (because of @ResponseStatus on the exception)
}


// ── On controller methods ────────────────────────────────────────────────────────
@PostMapping("/users")
@ResponseStatus(HttpStatus.CREATED)    // always returns 201 Created
public UserResponse create(@RequestBody UserRequest request) {
    return userService.create(request);
}

@DeleteMapping("/users/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)  // always returns 204 No Content
public void delete(@PathVariable Long id) {
    userService.delete(id);
}
```

---

#### 34. `@CrossOrigin`

Enables **Cross-Origin Resource Sharing (CORS)** on specific controllers or methods — allows web browsers to make requests from a different domain than the server.

```java
// Source:
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossOrigin {
    String[] origins()        default {};    // allowed origins (e.g., "http://localhost:3000")
    String[] methods()        default {};    // allowed HTTP methods
    String[] allowedHeaders() default {};    // allowed request headers
    String[] exposedHeaders() default {};    // headers the client can read
    boolean  allowCredentials() default false;   // allow cookies/auth headers?
    long     maxAge()         default -1;    // preflight cache duration in seconds
}
```

```java
// ── On a specific controller ─────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "http://localhost:3000")    // allow React dev server
public class UserController { ... }


// ── On a specific method ─────────────────────────────────────────────────────────
@GetMapping("/public-data")
@CrossOrigin(origins = "*", maxAge = 3600)   // allow all origins, cache for 1 hour
public List<PublicData> getPublicData() { ... }


// ── Global CORS configuration (preferred) ────────────────────────────────────────
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "https://myapp.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

#### 35. `@Valid` and `@Validated`

Triggers **Bean Validation** (JSR 380 / Hibernate Validator) on method parameters — checks `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max`, `@Pattern`, etc.

```java
// @Valid — from Jakarta (javax.validation)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR,
         ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Valid {}     // no attributes — marker annotation

// @Validated — from Spring (supports validation groups)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Validated {
    Class<?>[] value() default {};   // validation groups
}
```

```java
// ── DTO with validation annotations ──────────────────────────────────────────────
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Must be at least 18")
    @Max(value = 150, message = "Must be at most 150")
    private Integer age;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // getters, setters...
}

// ── Validate in controller ───────────────────────────────────────────────────────
@PostMapping("/users")
public ResponseEntity<UserResponse> create(@RequestBody @Valid CreateUserRequest request) {
    // If validation fails → 400 Bad Request with error details
    // MethodArgumentNotValidException is thrown automatically
    return ResponseEntity.status(201).body(userService.create(request));
}


// ── Validation groups (with @Validated) ──────────────────────────────────────────
public interface OnCreate {}
public interface OnUpdate {}

public class UserRequest {

    @Null(groups = OnCreate.class)             // must be null when creating
    @NotNull(groups = OnUpdate.class)          // must be present when updating
    private Long id;

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    private String name;
}

@PostMapping
public UserResponse create(@RequestBody @Validated(OnCreate.class) UserRequest req) { ... }

@PutMapping("/{id}")
public UserResponse update(@RequestBody @Validated(OnUpdate.class) UserRequest req) { ... }
```

---

#### Summary — Annotation Categories Overview

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot Annotations — Quick Reference by Category:                          │
│                                                                                  │
│  ┌─── STEREOTYPES (bean registration) ────────────────────────────────────────┐ │
│  │ @Component        Generic bean                                             │ │
│  │ @Service          Business logic layer                                     │ │
│  │ @Repository       Data access layer (+ exception translation)             │ │
│  │ @Controller       Web layer (returns views)                                │ │
│  │ @RestController   Web layer (returns JSON/XML)                            │ │
│  │ @Configuration    Bean definition class (CGLIB proxied)                   │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌─── DEPENDENCY INJECTION ──────────────────────────────────────────────────┐  │
│  │ @Autowired        Inject a dependency (by type)                           │  │
│  │ @Qualifier        Disambiguate when multiple beans match                  │  │
│  │ @Primary          Mark as default bean for a type                         │  │
│  │ @Bean             Declare a bean via method (in @Configuration)           │  │
│  │ @Value            Inject property/env/SpEL values                         │  │
│  │ @ComponentScan    Define where to scan for @Component beans              │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─── WEB / REST ────────────────────────────────────────────────────────────┐  │
│  │ @RequestMapping   Map URL to handler (class or method)                    │  │
│  │ @GetMapping       GET shortcut                                            │  │
│  │ @PostMapping      POST shortcut                                           │  │
│  │ @PutMapping       PUT shortcut                                            │  │
│  │ @DeleteMapping    DELETE shortcut                                         │  │
│  │ @PatchMapping     PATCH shortcut                                          │  │
│  │ @RequestBody      Read HTTP body → Java object                           │  │
│  │ @ResponseBody     Write Java object → HTTP body                          │  │
│  │ @PathVariable     Extract from URL path                                   │  │
│  │ @RequestParam     Extract from query string                               │  │
│  │ @RequestHeader    Extract from HTTP header                                │  │
│  │ @ResponseStatus   Set HTTP status on method/exception                     │  │
│  │ @CrossOrigin      Enable CORS                                             │  │
│  │ @ExceptionHandler Handle specific exceptions                              │  │
│  │ @ControllerAdvice Global cross-cutting for controllers                    │  │
│  │ @InitBinder       Customise data binding                                  │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─── LIFECYCLE & SCOPE ─────────────────────────────────────────────────────┐  │
│  │ @PostConstruct    Run after construction + injection                      │  │
│  │ @PreDestroy       Run before bean destruction                             │  │
│  │ @Scope            Set bean scope (singleton, prototype, request, session) │  │
│  │ @Lazy             Delay bean creation until first use                     │  │
│  │ @Order            Define bean ordering (for lists, filters, etc.)         │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─── CONDITIONAL BEANS ─────────────────────────────────────────────────────┐  │
│  │ @Profile              Activate bean for specific environment              │  │
│  │ @ConditionalOnProperty Enable/disable bean based on property value       │  │
│  │ @ConditionalOnBean     Only if another bean exists                        │  │
│  │ @ConditionalOnMissingBean Only if no such bean exists yet                │  │
│  │ @ConditionalOnClass    Only if a class is on classpath                    │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─── DATA / TRANSACTION ────────────────────────────────────────────────────┐  │
│  │ @Transactional     Define transaction boundaries                          │  │
│  │ @EnableTransactionManagement Enable @Transactional processing            │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─── ASYNC / SCHEDULING ────────────────────────────────────────────────────┐  │
│  │ @Async             Run method in background thread                        │  │
│  │ @EnableAsync       Enable @Async processing                               │  │
│  │ @Scheduled         Run method on a schedule (cron, fixedRate, etc.)       │  │
│  │ @EnableScheduling  Enable @Scheduled processing                           │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─── EVENTS ────────────────────────────────────────────────────────────────┐  │
│  │ @EventListener     Listen for application events                          │  │
│  │ @TransactionalEventListener Listen after transaction commit/rollback     │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─── CONFIGURATION ─────────────────────────────────────────────────────────┐  │
│  │ @ConfigurationProperties Bind property group to a POJO                   │  │
│  │ @EnableConfigurationProperties Enable @ConfigurationProperties classes   │  │
│  │ @PropertySource    Load additional .properties files                      │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  ┌─── VALIDATION ────────────────────────────────────────────────────────────┐  │
│  │ @Valid             Trigger Bean Validation                                 │  │
│  │ @Validated         Trigger validation with groups                         │  │
│  │ @NotNull, @NotBlank, @Size, @Min, @Max, @Email, @Pattern                │  │
│  │ (from jakarta.validation.constraints — Hibernate Validator)              │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```




- @Controller vs @RestController vs @ControllerAdvice
- @Component vs @Bean
- @Component vs @Service vs @COnfiguration vs Repository
- @@RequestMapping vs @GetMapping vs @PostMapping vs @PutMapping vs@@DeleteMapping


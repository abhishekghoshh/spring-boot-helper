### 3. Request/Response Architecture in Spring Boot

---

#### 3.1 Complete Request/Response Flow — ASCII Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT REQUEST/RESPONSE FLOW                                 │
│                    (Complete Architecture)                                            │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  ┌──────────┐                                                                        │
│  │  CLIENT   │  HTTP Request (GET /api/users/1)                                      │
│  │ (Browser/ │ ────────────────────────────────────────────────────────┐              │
│  │  Postman) │                                                         │              │
│  └──────────┘                                                         │              │
│       ▲                                                                │              │
│       │                                                                ▼              │
│       │  ┌─────────────────────────────────────────────────────────────────────────┐ │
│       │  │                     SERVLET CONTAINER (Tomcat)                           │ │
│       │  │                                                                          │ │
│       │  │   Tomcat receives the raw HTTP request, creates                          │ │
│       │  │   HttpServletRequest & HttpServletResponse objects                       │ │
│       │  │                                                                          │ │
│       │  │  ┌───────────────────────────────────────────────────────────────────┐   │ │
│       │  │  │                    FILTER CHAIN                                   │   │ │
│       │  │  │                                                                   │   │ │
│       │  │  │  ┌─────────────────┐  ┌─────────────────────┐  ┌──────────────┐  │   │ │
│       │  │  │  │ CharacterEncoding│  │ Security Filter     │  │ CORS Filter  │  │   │ │
│       │  │  │  │ Filter           │─►│ Chain               │─►│              │  │   │ │
│       │  │  │  │                  │  │ (15+ security       │  │              │  │   │ │
│       │  │  │  │ Sets UTF-8      │  │  filters including:  │  │ Handles      │  │   │ │
│       │  │  │  │ encoding        │  │  - CsrfFilter       │  │ cross-origin │  │   │ │
│       │  │  │  │                  │  │  - AuthenticationF. │  │ requests     │  │   │ │
│       │  │  │  │                  │  │  - AuthorizationF.  │  │              │  │   │ │
│       │  │  │  │                  │  │  - SessionMgmtF.    │  │              │  │   │ │
│       │  │  │  │                  │  │  - ExceptionTransF. │  │              │  │   │ │
│       │  │  │  └─────────────────┘  └─────────────────────┘  └──────┬───────┘  │   │ │
│       │  │  │                                                       │          │   │ │
│       │  │  └───────────────────────────────────────────────────────┼──────────┘   │ │
│       │  │                                                          │              │ │
│       │  │  ┌───────────────────────────────────────────────────────┼──────────┐   │ │
│       │  │  │                  DISPATCHER SERVLET                    │          │   │ │
│       │  │  │                  (Front Controller)                    ▼          │   │ │
│       │  │  │                                                                  │   │ │
│       │  │  │   ┌─────────────────┐    ┌────────────────────┐                  │   │ │
│       │  │  │   │  HandlerMapping  │───►│  HandlerAdapter     │                 │   │ │
│       │  │  │   │                  │    │                     │                  │   │ │
│       │  │  │   │  Maps URL to     │    │  Adapts & invokes   │                 │   │ │
│       │  │  │   │  controller      │    │  the handler method │                 │   │ │
│       │  │  │   │  method          │    │                     │                 │   │ │
│       │  │  │   └─────────────────┘    └──────────┬──────────┘                 │   │ │
│       │  │  │                                     │                            │   │ │
│       │  │  │  ┌──────────────────────────────────┼────────────────────────┐   │   │ │
│       │  │  │  │          INTERCEPTORS             │                       │   │   │ │
│       │  │  │  │                                   ▼                       │   │   │ │
│       │  │  │  │  ┌───────────────┐  ┌──────────────┐  ┌───────────────┐  │   │   │ │
│       │  │  │  │  │  preHandle()  │─►│  CONTROLLER    │─►│ postHandle()  │  │   │   │ │
│       │  │  │  │  │               │  │                │  │               │  │   │   │ │
│       │  │  │  │  │ Logging,      │  │  @GetMapping   │  │ Modify model  │  │   │   │ │
│       │  │  │  │  │ Auth checks,  │  │  Business Logic│  │ after handler │  │   │   │ │
│       │  │  │  │  │ Rate limiting │  │  Service calls  │  │ execution     │  │   │   │ │
│       │  │  │  │  └───────────────┘  └───────┬────────┘  └───────────────┘  │   │   │ │
│       │  │  │  │                             │                              │   │   │ │
│       │  │  │  │                afterCompletion() ◄─────────────────────────┘   │   │ │
│       │  │  │  │                (cleanup, logging)                              │   │ │
│       │  │  │  └───────────────────────────────────────────────────────────┘   │   │ │
│       │  │  │                                                                  │   │ │
│       │  │  │   ┌─────────────────┐    ┌────────────────────┐                  │   │ │
│       │  │  │   │  ViewResolver    │───►│  View (JSON/HTML)   │                 │   │ │
│       │  │  │   │ (for MVC views)  │    │  HttpMessageConverter│                │   │ │
│       │  │  │   │                  │    │  (for REST/JSON)    │                  │   │ │
│       │  │  │   └─────────────────┘    └────────────────────┘                  │   │ │
│       │  │  │                                                                  │   │ │
│       │  │  └──────────────────────────────────────────────────────────────────┘   │ │
│       │  │                                                                          │ │
│       │  └──────────────────────────────────────────────────────────────────────────┘ │
│       │                                                                               │
│       │   HTTP Response (200 OK + JSON Body)                                          │
│       └───────────────────────────────────────────────────────────────────────────────┘
│                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 3.2 Detailed Component Breakdown

##### 3.2.1 Servlet Container (Embedded Tomcat)

The Servlet Container is the web server that handles low-level HTTP communication. Spring Boot embeds **Apache Tomcat** by default.

```
┌───────────────────────────────────────────────────────┐
│              SERVLET CONTAINER (Tomcat)                │
├───────────────────────────────────────────────────────┤
│                                                       │
│  Responsibilities:                                    │
│  ├── Listens on port 8080 (configurable)             │
│  ├── Manages thread pool for concurrent requests     │
│  ├── Creates HttpServletRequest/Response objects      │
│  ├── Manages servlet lifecycle (init, service,destroy)│
│  ├── Manages Filter Chain execution                  │
│  └── Routes requests to registered Servlets          │
│                                                       │
│  Embedded Server Options:                             │
│  ├── Tomcat (default)                                │
│  ├── Jetty                                           │
│  └── Undertow                                        │
│                                                       │
│  Key Servlets:                                        │
│  ├── DispatcherServlet (Spring MVC front controller) │
│  ├── Other registered servlets (rarely needed)       │
│  └── Default servlet (serves static resources)       │
│                                                       │
└───────────────────────────────────────────────────────┘
```

**How Spring Boot auto-configures Tomcat:**

```java
// Spring Boot auto-configuration (happens behind the scenes)
// TomcatServletWebServerFactory creates embedded Tomcat

// You can customize via application.yml:
// server:
//   port: 8080
//   tomcat:
//     threads:
//       max: 200
//       min-spare: 10
//     max-connections: 8192
//     accept-count: 100

// Or programmatically:
@Component
public class TomcatCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.setPort(8080);
        factory.addConnectorCustomizers(connector -> {
            connector.setMaxPostSize(10 * 1024 * 1024); // 10MB
        });
    }
}
```

##### 3.2.2 Filter Chain

Filters intercept requests **before** they reach the servlet and responses **after** the servlet processes them. They operate at the **Servlet Container level**.

```
┌──────────────────────────────────────────────────────────────────────┐
│                        FILTER CHAIN                                  │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Request ──►                                                         │
│                                                                      │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐            │
│  │   Filter 1    │──►│   Filter 2    │──►│   Filter 3    │──► Servlet│
│  │ (Encoding)    │   │ (Security)    │   │ (CORS)        │           │
│  │               │◄──│               │◄──│               │◄── Servlet│
│  └──────────────┘   └──────────────┘   └──────────────┘            │
│                                                                      │
│                                                         ◄── Response │
│                                                                      │
│  Each filter can:                                                    │
│  ├── Modify the request before passing it on                        │
│  ├── Modify the response on the way back                            │
│  ├── Short-circuit (block the request entirely)                     │
│  └── Add headers, log, authenticate, etc.                           │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**Spring Security Filter Chain (15+ filters in order):**

```
┌────────────────────────────────────────────────────────────────────┐
│          SPRING SECURITY FILTER CHAIN (Execution Order)           │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  1.  DisableEncodeUrlFilter                                       │
│  2.  WebAsyncManagerIntegrationFilter                             │
│  3.  SecurityContextHolderFilter                                  │
│  4.  HeaderWriterFilter          (adds security headers)          │
│  5.  CorsFilter                  (handles CORS)                   │
│  6.  CsrfFilter                 (CSRF protection)                │
│  7.  LogoutFilter                (handles /logout)                │
│  8.  UsernamePasswordAuthenticationFilter (form login)            │
│  9.  BasicAuthenticationFilter   (HTTP Basic auth)                │
│  10. RequestCacheAwareFilter                                      │
│  11. SecurityContextHolderAwareRequestFilter                      │
│  12. AnonymousAuthenticationFilter                                │
│  13. SessionManagementFilter                                      │
│  14. ExceptionTranslationFilter  (converts security exceptions)   │
│  15. AuthorizationFilter         (checks access rules)            │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

**Custom Filter Example:**

```java
// Creating a custom logging filter
@Component
@Order(1)  // Execution order
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        long startTime = System.currentTimeMillis();

        log.info("Incoming Request: {} {} from {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr());

        // Pass request down the chain
        chain.doFilter(request, response);

        // After servlet processes (on the way back)
        long duration = System.currentTimeMillis() - startTime;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        log.info("Response: {} {} - {} ({}ms)",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpResponse.getStatus(),
                duration);
    }
}
```

**Registering a Filter with specific URL patterns:**

```java
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> loggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestLoggingFilter());
        bean.addUrlPatterns("/api/*");   // Only for /api/* paths
        bean.setOrder(1);               // Execution order
        return bean;
    }
}
```

##### 3.2.3 DispatcherServlet (Front Controller Pattern)

The **DispatcherServlet** is the heart of Spring MVC. It acts as the **front controller** — a single entry point that receives all requests and delegates them to the appropriate handler.

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    DISPATCHER SERVLET INTERNALS                            │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  HttpServletRequest arrives at DispatcherServlet.doDispatch()              │
│       │                                                                    │
│       ▼                                                                    │
│  ┌──────────────────────────────────────────────────────┐                 │
│  │  1. HANDLER MAPPING                                   │                 │
│  │     "Which controller method handles this request?"   │                 │
│  │                                                       │                 │
│  │  Implementations:                                     │                 │
│  │  ├── RequestMappingHandlerMapping (@RequestMapping)   │                 │
│  │  ├── SimpleUrlHandlerMapping                          │                 │
│  │  └── BeanNameUrlHandlerMapping                        │                 │
│  │                                                       │                 │
│  │  GET /api/users/1 ──► UserController.getUserById(1)   │                 │
│  └──────────────────────────┬───────────────────────────┘                 │
│                             │                                              │
│                             ▼                                              │
│  ┌──────────────────────────────────────────────────────┐                 │
│  │  2. HANDLER ADAPTER                                   │                 │
│  │     "How do I invoke this handler?"                   │                 │
│  │                                                       │                 │
│  │  ├── Resolves method parameters                       │                 │
│  │  │   (@PathVariable, @RequestBody, @RequestParam)     │                 │
│  │  ├── Calls the controller method                      │                 │
│  │  ├── Handles return value                             │                 │
│  │  │   (ResponseEntity, Model, String view name)        │                 │
│  │  └── Uses HttpMessageConverters for serialization     │                 │
│  └──────────────────────────┬───────────────────────────┘                 │
│                             │                                              │
│                             ▼                                              │
│  ┌──────────────────────────────────────────────────────┐                 │
│  │  3. VIEW RESOLVER (for MVC) / MESSAGE CONVERTER (REST)│                │
│  │                                                       │                 │
│  │  For @RestController (REST API):                      │                 │
│  │  └── MappingJackson2HttpMessageConverter               │                │
│  │      converts Java object ──► JSON response           │                 │
│  │                                                       │                 │
│  │  For @Controller (MVC with views):                    │                 │
│  │  └── ThymeleafViewResolver / InternalResourceResolver │                 │
│  │      resolves view name ──► HTML template             │                 │
│  └──────────────────────────────────────────────────────┘                 │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

**DispatcherServlet Internal Source (Simplified doDispatch):**

```java
// Simplified version of what DispatcherServlet.doDispatch() does internally
protected void doDispatch(HttpServletRequest request,
                           HttpServletResponse response) throws Exception {

    // 1. Find the handler (controller method) for this request
    HandlerExecutionChain mappedHandler = getHandler(request);
    //    Internally iterates through HandlerMappings:
    //    for (HandlerMapping mapping : this.handlerMappings) {
    //        HandlerExecutionChain handler = mapping.getHandler(request);
    //        if (handler != null) return handler;
    //    }

    if (mappedHandler == null) {
        noHandlerFound(request, response);  // 404
        return;
    }

    // 2. Get the HandlerAdapter for this handler
    HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

    // 3. Execute pre-handle interceptors
    if (!mappedHandler.applyPreHandle(request, response)) {
        return;  // Interceptor rejected the request
    }

    // 4. Actually invoke the handler (controller method)
    ModelAndView mv = ha.handle(request, response, mappedHandler.getHandler());

    // 5. Execute post-handle interceptors
    mappedHandler.applyPostHandle(request, response, mv);

    // 6. Process the result (render view or write response body)
    processDispatchResult(request, response, mappedHandler, mv, null);
}
```

##### 3.2.4 Interceptors (HandlerInterceptor)

Interceptors work at the **Spring MVC level** (inside DispatcherServlet), unlike Filters which work at the Servlet Container level.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    FILTER vs INTERCEPTOR                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  FILTER (javax.servlet.Filter)      INTERCEPTOR (HandlerInterceptor)│
│  ├── Servlet Container level        ├── Spring MVC level            │
│  ├── Works on ALL servlets          ├── Only DispatcherServlet      │
│  ├── Has access to raw req/res      ├── Has access to Handler info  │
│  ├── Cannot access Spring beans     ├── Can access Spring beans     │
│  │   (without workarounds)          │   (is a Spring bean itself)   │
│  ├── Executes BEFORE servlet        ├── Executes WITHIN servlet     │
│  └── Use for: Security, CORS,      └── Use for: Logging, Auth      │
│       Encoding, Compression              Checks, Rate Limiting,     │
│                                          Tenant Resolution          │
│                                                                     │
│  Execution Order:                                                   │
│  Request ► Filter1 ► Filter2 ► Servlet ► Interceptor ► Controller  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**Custom Interceptor Example:**

```java
@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {
        String clientIp = request.getRemoteAddr();

        if (!rateLimiter.tryAcquire(clientIp)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
            return false;  // Block the request
        }

        request.setAttribute("startTime", System.currentTimeMillis());
        return true;  // Continue to the controller
    }

    @Override
    public void postHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler,
                            ModelAndView modelAndView) throws Exception {
        // Runs AFTER controller but BEFORE view rendering
        // Can modify the ModelAndView
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler,
                                 Exception ex) throws Exception {
        // Runs AFTER everything (even if exception occurred)
        // Good for cleanup and logging
        long startTime = (long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;

        if (handler instanceof HandlerMethod handlerMethod) {
            String controller = handlerMethod.getBeanType().getSimpleName();
            String method = handlerMethod.getMethod().getName();
            LoggerFactory.getLogger(getClass())
                .info("{}.{} completed in {}ms", controller, method, duration);
        }
    }
}
```

**Register Interceptor:**

```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;
    private final AuthenticationInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/**")       // Apply to API endpoints
                .excludePathPatterns("/api/health"); // Exclude health check

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/admin/**")  // Only admin endpoints
                .order(1);                         // Execution order
    }
}
```

##### 3.2.5 Controller (Business Logic)

The Controller is the final destination where business logic is executed.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       CONTROLLER LAYER                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Annotations that drive the request mapping:                            │
│                                                                         │
│  @RestController = @Controller + @ResponseBody                         │
│                                                                         │
│  Method Parameter Resolution:                                           │
│  ├── @PathVariable    ──► /users/{id}         ──► id = 1               │
│  ├── @RequestParam    ──► /users?name=John    ──► name = "John"        │
│  ├── @RequestBody     ──► JSON body           ──► Java object          │
│  ├── @RequestHeader   ──► Header value        ──► String               │
│  ├── @CookieValue     ──► Cookie value        ──► String               │
│  └── HttpServletRequest ──► Raw request object                          │
│                                                                         │
│  Return Value Handling:                                                  │
│  ├── ResponseEntity<T>  ──► Full control (status, headers, body)       │
│  ├── T (any object)     ──► Serialized to JSON (200 OK)               │
│  ├── String             ──► View name (for @Controller)                │
│  └── void               ──► 200 OK with empty body                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserResponse> users = userService.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(users.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestBody @Valid CreateUserRequest request) {
        UserResponse created = userService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

#### 3.3 Complete Request Lifecycle — Step by Step

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│            COMPLETE REQUEST LIFECYCLE: GET /api/users/1                          │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Step 1: Client sends HTTP request                                              │
│  ─────────────────────────────────                                               │
│  GET /api/users/1 HTTP/1.1                                                      │
│  Host: localhost:8080                                                            │
│  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...                                 │
│  Accept: application/json                                                        │
│                                                                                  │
│  Step 2: Tomcat receives request                                                │
│  ────────────────────────────────                                                │
│  • Assigns a thread from the thread pool                                        │
│  • Creates HttpServletRequest & HttpServletResponse                              │
│  • Passes to the Filter Chain                                                    │
│                                                                                  │
│  Step 3: Filter Chain executes                                                  │
│  ─────────────────────────────                                                   │
│  • CharacterEncodingFilter → sets UTF-8                                          │
│  • SecurityFilterChain:                                                          │
│    ├── CsrfFilter → checks CSRF token (skipped for GET)                        │
│    ├── JwtAuthFilter → extracts JWT, validates, sets SecurityContext             │
│    └── AuthorizationFilter → checks if user has access to /api/users/{id}       │
│  • CorsFilter → adds CORS headers if needed                                     │
│                                                                                  │
│  Step 4: DispatcherServlet.doDispatch()                                         │
│  ────────────────────────────────────────                                        │
│  • HandlerMapping finds: UserController.getUserById(Long)                       │
│  • HandlerAdapter prepares to invoke the method                                 │
│                                                                                  │
│  Step 5: Interceptor preHandle()                                                │
│  ─────────────────────────────────                                               │
│  • RateLimitingInterceptor checks rate limit → OK                               │
│  • Logging interceptor logs the request                                          │
│                                                                                  │
│  Step 6: Controller method executes                                             │
│  ────────────────────────────────────                                            │
│  • @PathVariable id = 1 resolved from URL                                       │
│  • userService.findById(1) → calls Repository → queries DB                     │
│  • Returns ResponseEntity.ok(userResponse)                                      │
│                                                                                  │
│  Step 7: Interceptor postHandle()                                               │
│  ──────────────────────────────────                                              │
│  • Can modify the response                                                      │
│                                                                                  │
│  Step 8: Response serialization                                                 │
│  ──────────────────────────────                                                  │
│  • MappingJackson2HttpMessageConverter converts UserResponse → JSON              │
│                                                                                  │
│  Step 9: Interceptor afterCompletion()                                          │
│  ────────────────────────────────────                                            │
│  • Logs completion time                                                          │
│                                                                                  │
│  Step 10: Filter Chain (reverse order)                                          │
│  ─────────────────────────────────────                                           │
│  • Filters can modify the response on the way back                              │
│                                                                                  │
│  Step 11: Tomcat sends HTTP response                                            │
│  ────────────────────────────────────                                            │
│  HTTP/1.1 200 OK                                                                │
│  Content-Type: application/json                                                  │
│  X-Content-Type-Options: nosniff                                                │
│  X-Frame-Options: DENY                                                           │
│                                                                                  │
│  {"id": 1, "username": "john", "email": "john@example.com"}                    │
│                                                                                  │
│  Step 12: Thread returned to Tomcat's thread pool                               │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


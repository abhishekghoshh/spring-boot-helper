# Spring Interceptor and Filter


## 1. What is a Spring Interceptor?

A **Spring MVC Interceptor** is a component that intercepts HTTP requests **before they reach a controller** and/or **after the controller returns a response**, but before it is written to the client. It operates at the **Spring MVC layer** — inside the `DispatcherServlet` — giving it access to Spring context, handler metadata, and the `ModelAndView`.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                  Request Processing Chain (Spring MVC)                   │
│                                                                          │
│  Client (Browser / API Consumer)                                         │
│       │                                                                  │
│       ▼  HTTP request                                                    │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                     Servlet Container (Tomcat)                  │    │
│  │                                                                 │    │
│  │  ┌──────────────────────────────────────────────────────────┐  │    │
│  │  │           javax.servlet.Filter chain                     │  │    │
│  │  │   (operates on raw HttpServletRequest/Response)          │  │    │
│  │  └───────────────────────┬──────────────────────────────────┘  │    │
│  │                          │                                      │    │
│  │                          ▼                                      │    │
│  │  ┌──────────────────────────────────────────────────────────┐  │    │
│  │  │                  DispatcherServlet                        │  │    │
│  │  │                                                          │  │    │
│  │  │  ┌────────────────────────────────────────────────────┐  │  │    │
│  │  │  │         HandlerInterceptor chain                   │  │  │    │
│  │  │  │   (operates inside Spring MVC, knows the handler)  │  │  │    │
│  │  │  │                                                    │  │  │    │
│  │  │  │   preHandle()  ──►  Controller  ──►  postHandle()  │  │  │    │
│  │  │  │                                    afterCompletion()│  │  │    │
│  │  │  └────────────────────────────────────────────────────┘  │  │    │
│  │  └──────────────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

The `HandlerInterceptor` interface defines three callback hooks:

```java
// org.springframework.web.servlet.HandlerInterceptor
public interface HandlerInterceptor {

    // Called BEFORE the controller method. Return false to abort the chain.
    default boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {
        return true;
    }

    // Called AFTER the controller method, BEFORE the view is rendered.
    // Only called if preHandle() returned true.
    default void postHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler,
                            @Nullable ModelAndView modelAndView) throws Exception {
    }

    // Called AFTER the complete request is done (view rendered or response written).
    // Called even if an exception was thrown by the controller.
    default void afterCompletion(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler,
                                 @Nullable Exception ex) throws Exception {
    }
}
```

```
Request lifecycle with Interceptor:

         preHandle()                   postHandle()        afterCompletion()
              │                             │                     │
──────────────┼─────────────────────────────┼─────────────────────┼──────────►
              │                             │                     │
          ┌───▼────────────────────────┐    │                     │
          │   Controller.handleRequest()├────┘                     │
          │   (produces ModelAndView)   │                          │
          └────────────────────────────┘                          │
                                                          ┌────────▼────────┐
                                                          │  View rendered  │
                                                          │  or response    │
                                                          │  written        │
                                                          └─────────────────┘
```

---

## 2. Why and How Interceptors Are Used — Industry Use Cases

### Use Case 1: Authentication and Authorization Token Validation

Every API request must carry a valid JWT. Validate it once in an interceptor rather than in every controller.

```java
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtService;

    public JwtAuthInterceptor(JwtTokenService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
            return false; // ← abort — controller is NEVER called
        }

        String token = authHeader.substring(7);
        try {
            UserClaims claims = jwtService.validate(token);
            request.setAttribute("userClaims", claims); // pass to controller
        } catch (JwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token expired or invalid\"}");
            return false;
        }

        return true; // ← proceed to controller
    }
}
```

### Use Case 2: Request Logging and Distributed Tracing (MDC)

Log every incoming request with timing and propagate a trace ID for distributed tracing.

```java
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "reqStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // Generate or extract trace ID
        String traceId = Optional
            .ofNullable(request.getHeader("X-Trace-Id"))
            .orElse(UUID.randomUUID().toString());

        MDC.put("traceId", traceId);
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());

        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        long start = (Long) request.getAttribute(START_TIME_ATTR);
        long duration = System.currentTimeMillis() - start;

        log.info("Completed: {} {} → {} in {}ms",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            duration);

        MDC.clear(); // ← critical: clear MDC to avoid leaking across threads
    }
}
```

### Use Case 3: API Rate Limiting

Throttle requests per user/IP to protect backend services.

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String clientIp = request.getRemoteAddr();

        if (!rateLimiterService.allowRequest(clientIp)) {
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
            return false;
        }
        return true;
    }
}
```

### Use Case 4: Locale / Timezone Resolution

Resolve the user's locale from a header or cookie before the controller processes the request, making it available to the response.

```java
@Component
public class LocaleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String lang = request.getHeader("Accept-Language");
        Locale locale = (lang != null) ? Locale.forLanguageTag(lang) : Locale.ENGLISH;
        LocaleContextHolder.setLocale(locale);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        LocaleContextHolder.resetLocaleContext();
    }
}
```

---

## 3. Registering Interceptors — `HandlerInterceptor` + `WebMvcConfigurer`

Interceptors are registered by implementing `WebMvcConfigurer` and overriding `addInterceptors()`:

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor,
                        RequestLoggingInterceptor requestLoggingInterceptor,
                        RateLimitInterceptor rateLimitInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.requestLoggingInterceptor = requestLoggingInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // Logging applies to ALL endpoints
        registry.addInterceptor(requestLoggingInterceptor);

        // Rate limiting — all API paths
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**");

        // JWT auth — all API paths EXCEPT login and signup
        registry.addInterceptor(jwtAuthInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/signup",
                "/api/public/**"
            );
    }
}
```

### Path Pattern Reference

```
Pattern              Matches
────────────────────────────────────────────────────────
/api/**              All paths starting with /api/
/api/users/*         /api/users/123 (single segment)
/api/users/**        /api/users/123/orders/456 (any depth)
/admin/**            All admin paths
/**                  Every path (global)
```

```
addPathPatterns()    → whitelist: interceptor runs ONLY on these paths
excludePathPatterns()→ blacklist: interceptor is SKIPPED for these paths
                       (applied after addPathPatterns — exclusions win)
```

---

## 4. Advantages, Disadvantages, and Limitations

### Advantages

| Advantage | Detail |
|-----------|--------|
| **Spring-aware** | Has access to Spring beans, `ApplicationContext`, handler metadata |
| **Knows the handler** | `Object handler` parameter reveals which controller/method handles the request — useful for reading annotations |
| **Fine path control** | Ant-style path patterns with include and exclude lists |
| **ModelAndView access** | `postHandle()` can modify the model before rendering |
| **Clean separation** | Cross-cutting concerns extracted from controllers |
| **No bytecode magic** | Pure interface implementation — no AOP proxy complexity |

### Disadvantages

| Disadvantage | Detail |
|-------------|--------|
| **Spring MVC only** | Does not apply to non-Spring endpoints (WebSockets, gRPC, actuator endpoints, etc.) |
| **Cannot modify request body** | `HttpServletRequest` body is a stream — once read, it cannot be re-read without wrapping |
| **Runs after Filter chain** | Cannot block requests before the Servlet container processes them (e.g., cannot act before multipart parsing) |
| **postHandle not called on exception** | If the controller throws and no `@ExceptionHandler` intercepts, `postHandle()` is skipped — `afterCompletion()` is always called |
| **No response body access** | Cannot read or modify the response body that was written by the controller |

### Limitations

```
Things an Interceptor CANNOT do:
  ✗ Modify or replace the request body (stream already consumed)
  ✗ Modify the response body after it has been written
  ✗ Intercept calls to non-DispatcherServlet endpoints
  ✗ Intercept static resources (unless explicitly mapped)
  ✗ Run truly before Servlet filters (Filters run first)
  ✗ Access WebSocket frames

Things only a Filter can do:
  ✓ Wrap the request/response with a custom implementation
  ✓ Act on every servlet — not just DispatcherServlet
  ✓ Decompress request bodies before Spring sees them
  ✓ Mutate the request URI before routing
```

---

## 5. Where Interceptors Work — Layer Diagram and `DispatcherServlet` Internals

### Full Layer Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        Request Processing Layers                           │
│                                                                            │
│  Network                                                                   │
│       │ HTTP                                                               │
│       ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                    Tomcat (Servlet Container)                       │  │
│  │                                                                     │  │
│  │  ┌──────────────────────────────────────────────────────────────┐  │  │
│  │  │                 javax.servlet.Filter  chain                  │  │  │
│  │  │   CorsFilter → CsrfFilter → CharacterEncodingFilter → ...   │  │  │
│  │  │   Operates on: raw HttpServletRequest / HttpServletResponse  │  │  │
│  │  │   Knows nothing about Spring controllers or handlers          │  │  │
│  │  └──────────────────────────┬───────────────────────────────────┘  │  │
│  │                             │                                       │  │
│  │                             ▼                                       │  │
│  │  ┌──────────────────────────────────────────────────────────────┐  │  │
│  │  │               DispatcherServlet (Front Controller)           │  │  │
│  │  │                                                              │  │  │
│  │  │  1. HandlerMapping.getHandler(request)                       │  │  │
│  │  │     → resolves which @Controller handles this URL            │  │  │
│  │  │     → returns HandlerExecutionChain                          │  │  │
│  │  │          = (handler + list of matching HandlerInterceptors)  │  │  │
│  │  │                                                              │  │  │
│  │  │  2. for each interceptor: interceptor.preHandle()            │  │  │
│  │  │     → if any returns false → abort, skip controller          │  │  │
│  │  │                                                              │  │  │
│  │  │  3. HandlerAdapter.handle(request, response, handler)        │  │  │
│  │  │     → invokes the @Controller method                         │  │  │
│  │  │     → produces ModelAndView (or writes response directly)    │  │  │
│  │  │                                                              │  │  │
│  │  │  4. for each interceptor (REVERSE ORDER): postHandle()       │  │  │
│  │  │                                                              │  │  │
│  │  │  5. processDispatchResult() — render view                    │  │  │
│  │  │                                                              │  │  │
│  │  │  6. for each interceptor (REVERSE ORDER): afterCompletion()  │  │  │
│  │  │     (called even on exception)                               │  │  │
│  │  └──────────────────────────────────────────────────────────────┘  │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### `DispatcherServlet.doDispatch()` — Source-Level Walk-through

This is the core method inside `DispatcherServlet` that drives the entire interceptor lifecycle:

```java
// org.springframework.web.servlet.DispatcherServlet (simplified)
protected void doDispatch(HttpServletRequest request,
                          HttpServletResponse response) throws Exception {

    HandlerExecutionChain mappedHandler = null;
    Exception dispatchException = null;

    try {
        // STEP 1 — resolve the handler
        // Returns: HandlerExecutionChain = controller method + matching interceptors
        mappedHandler = getHandler(request);
        if (mappedHandler == null) {
            noHandlerFound(request, response); // 404
            return;
        }

        // STEP 2 — find the HandlerAdapter (e.g., RequestMappingHandlerAdapter)
        HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

        // STEP 3 — call preHandle() on every interceptor in FORWARD order
        // If ANY returns false, execution stops here — controller is NOT called
        if (!mappedHandler.applyPreHandle(request, response)) {
            return; // ← early exit — afterCompletion() still called below
        }

        // STEP 4 — invoke the actual @Controller method
        ModelAndView mv = ha.handle(request, response, mappedHandler.getHandler());

        // STEP 5 — call postHandle() on every interceptor in REVERSE order
        mappedHandler.applyPostHandle(request, response, mv);

    } catch (Exception ex) {
        dispatchException = ex;
    }

    // STEP 6 — render view / write response + call afterCompletion() always
    processDispatchResult(request, response, mappedHandler, mv, dispatchException);
    // ↑ This method calls mappedHandler.triggerAfterCompletion() internally
    //   afterCompletion() is called even when an exception occurred!
}
```

### `HandlerExecutionChain.applyPreHandle()` — Source

```java
// Simplified from HandlerExecutionChain
boolean applyPreHandle(HttpServletRequest request,
                       HttpServletResponse response) throws Exception {

    for (int i = 0; i < this.interceptorList.size(); i++) {
        HandlerInterceptor interceptor = this.interceptorList.get(i);

        if (!interceptor.preHandle(request, response, this.handler)) {
            // preHandle returned false — trigger afterCompletion on already-run interceptors
            triggerAfterCompletion(request, response, null);
            return false; // abort chain
        }
        this.interceptorIndex = i; // track how far we got
    }
    return true;
}
```

### Execution Order Summary

```
Interceptors registered: [A, B, C]

preHandle:      A → B → C      (forward order)
Controller:           executes
postHandle:     C → B → A      (REVERSE order)
afterCompletion:C → B → A      (REVERSE order)

If B.preHandle() returns false:
  preHandle:    A → B (stops)
  Controller:   NOT called
  afterCompletion: A (only A — B never completed pre-handle, C was never reached)
```

---

## 6. Multiple Interceptors — Ordering and Execution

### Registering Multiple Interceptors

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired private RequestLoggingInterceptor loggingInterceptor;
    @Autowired private JwtAuthInterceptor jwtInterceptor;
    @Autowired private RateLimitInterceptor rateLimitInterceptor;
    @Autowired private TenantContextInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Order is determined by REGISTRATION ORDER
        // First registered = first to run in preHandle, LAST to run in postHandle

        registry.addInterceptor(loggingInterceptor)   // runs first (for all paths)
                .addPathPatterns("/**");

        registry.addInterceptor(rateLimitInterceptor) // runs second
                .addPathPatterns("/api/**");

        registry.addInterceptor(jwtInterceptor)       // runs third
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/public/**");

        registry.addInterceptor(tenantInterceptor)    // runs fourth
                .addPathPatterns("/api/**");
    }
}
```

### Ordering Diagram

```
Registration order:   [Logging, RateLimit, JWT, Tenant]

preHandle  (→→→):    Logging → RateLimit → JWT → Tenant → Controller
postHandle (←←←):   Tenant  → JWT → RateLimit → Logging
afterCompletion(←←←):Tenant → JWT → RateLimit → Logging

If JWT.preHandle() returns false (invalid token):
  preHandle:        Logging ✅  →  RateLimit ✅  →  JWT ✗
  Controller:       NOT called
  afterCompletion:  RateLimit → Logging    (only the ones that completed)
```

### Controlling Order with `@Order` / `Ordered`

The registry order controls execution sequence. If interceptors are Spring beans, you can also implement `org.springframework.core.Ordered`:

```java
@Component
@Order(1)                       // lowest number = highest priority = runs first
public class RequestLoggingInterceptor implements HandlerInterceptor, Ordered {

    @Override
    public int getOrder() { return 1; }

    // ... preHandle, postHandle, afterCompletion
}

@Component
@Order(2)
public class JwtAuthInterceptor implements HandlerInterceptor, Ordered {

    @Override
    public int getOrder() { return 2; }

    // ...
}
```

> `@Order` on the bean only influences Spring's injection order — the actual execution order is always determined by the **order they are added to the `InterceptorRegistry`** in `addInterceptors()`. If you want a reliable order, always explicitly control the `registry.addInterceptor()` call sequence.

### Full Example: Three Interceptors with Different Path Scopes

```java
// ─── Interceptor 1: global logging ───────────────────────────────────
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse res,
                             Object handler) {
        req.setAttribute("startTime", System.currentTimeMillis());
        log.info("→ {} {}", req.getMethod(), req.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req,
                                HttpServletResponse res,
                                Object handler, Exception ex) {
        long ms = System.currentTimeMillis() - (Long) req.getAttribute("startTime");
        log.info("← {} {} [{}ms] HTTP {}", req.getMethod(), req.getRequestURI(), ms, res.getStatus());
        MDC.clear();
    }
}

// ─── Interceptor 2: JWT auth for /api/** ─────────────────────────────
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtService;

    public JwtAuthInterceptor(JwtTokenService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse res,
                             Object handler) throws Exception {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
            return false;
        }
        UserClaims claims = jwtService.validate(header.substring(7));
        req.setAttribute("userClaims", claims);
        return true;
    }
}

// ─── Interceptor 3: admin role check for /admin/** ───────────────────
@Component
public class AdminRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse res,
                             Object handler) throws Exception {
        UserClaims claims = (UserClaims) req.getAttribute("userClaims");
        if (claims == null || !claims.getRoles().contains("ROLE_ADMIN")) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return false;
        }
        return true;
    }
}

// ─── Registration ─────────────────────────────────────────────────────
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired private RequestLoggingInterceptor logging;
    @Autowired private JwtAuthInterceptor jwt;
    @Autowired private AdminRoleInterceptor admin;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logging)
                .addPathPatterns("/**");               // all paths

        registry.addInterceptor(jwt)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");  // skip login/signup

        registry.addInterceptor(admin)
                .addPathPatterns("/admin/**");          // only admin paths
    }
}
```

```
Request: GET /api/users/123
  preHandle:   Logging ✅ → JWT ✅ (token valid) → [admin not applicable]
  Controller:  UserController.getUser(123) executes
  postHandle:  [none to call in reverse]
  afterCompletion: Logging ✅

Request: GET /admin/dashboard
  preHandle:   Logging ✅ → JWT ✅ → Admin ✅ (has ROLE_ADMIN)
  Controller:  AdminController.dashboard() executes

Request: GET /admin/dashboard (no JWT token)
  preHandle:   Logging ✅ → JWT ✗ (returns false, sends 401)
  Controller:  NOT called
  afterCompletion: Logging (only logging ran successfully)
```

---

## 7. Interceptor vs Filter — When to Use Which

```
┌────────────────────────────────────────────────────────────────────────┐
│                  Interceptor  vs  Filter                               │
│                                                                        │
│  Feature                  │  Filter              │  Interceptor        │
│  ─────────────────────────┼──────────────────────┼─────────────────────│
│  Layer                    │  Servlet container   │  Spring MVC         │
│  Interface                │  javax.servlet.Filter│  HandlerInterceptor │
│  Runs for                 │  All servlets        │  DispatcherServlet  │
│  Spring bean access       │  Limited (via DI)    │  Full Spring access │
│  Knows the handler        │  No                  │  Yes                │
│  Access to ModelAndView   │  No                  │  Yes (postHandle)   │
│  Modify request body      │  Yes (via wrap)      │  No                 │
│  Modify response body     │  Yes (via wrap)      │  No                 │
│  Non-Spring endpoints     │  Yes                 │  No                 │
│  Multipart / encoding     │  Yes (runs first)    │  No (too late)      │
│  Typical use case         │  CORS, encoding,     │  Auth, logging,     │
│                           │  compression, HTTPS  │  rate limiting,     │
│                           │  redirect            │  role checking      │
└────────────────────────────────────────────────────────────────────────┘

Rule of thumb:
  Need to work at the Servlet level or modify raw streams?  → Filter
  Need Spring context, handler info, or MVC-level control?  → Interceptor
```

---

## 8. What is a Servlet Filter?

A **Servlet Filter** (`javax.servlet.Filter` / `jakarta.servlet.Filter`) is a component that intercepts **every HTTP request and response** at the **Servlet container level** — before the request reaches any Servlet (including `DispatcherServlet`) and after the Servlet writes its response.

Filters form a **chain**. Each filter in the chain calls `chain.doFilter()` to pass control to the next filter. The last filter in the chain invokes the target Servlet (e.g., `DispatcherServlet`). On the way back, control flows in reverse through the same chain.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        Servlet Filter Chain                                  │
│                                                                              │
│  Client                                                                      │
│    │                                                                         │
│    ▼ HTTP Request                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    Tomcat (Servlet Container)                        │   │
│  │                                                                      │   │
│  │  FilterChain:                                                        │   │
│  │                                                                      │   │
│  │  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌─────────────────┐  │   │
│  │  │ Filter 1 │──►│ Filter 2 │──►│ Filter 3 │──►│DispatcherServlet│  │   │
│  │  │(Logging) │◄──│ (CORS)   │◄──│  (Auth)  │◄──│  (Spring MVC)   │  │   │
│  │  └──────────┘   └──────────┘   └──────────┘   └─────────────────┘  │   │
│  │                                                                      │   │
│  │  Request flows →→→ through each filter → Servlet                    │   │
│  │  Response flows ←←← back through each filter → Client               │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘
```

### The `Filter` Interface

```java
// jakarta.servlet.Filter
public interface Filter {

    // Called once when the filter is initialized (app startup)
    default void init(FilterConfig filterConfig) throws ServletException {}

    // Called on EVERY matching request — the core method
    void doFilter(ServletRequest request,
                  ServletResponse response,
                  FilterChain chain) throws IOException, ServletException;

    // Called once when the filter is destroyed (app shutdown)
    default void destroy() {}
}
```

The key contract of `doFilter()`:

```java
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

    // ── PRE-PROCESSING (runs BEFORE the next filter/servlet) ──────────
    // do something with request/response here

    chain.doFilter(request, response); // ← pass to next filter or servlet
    //                                    if NOT called → chain is aborted!

    // ── POST-PROCESSING (runs AFTER the servlet writes the response) ──
    // do something with request/response here
}
```

```
Execution flow for a 3-filter chain:

→ Filter1.doFilter() [pre]
    → chain.doFilter() → Filter2.doFilter() [pre]
                              → chain.doFilter() → Filter3.doFilter() [pre]
                                                        → chain.doFilter() → DispatcherServlet
                                                   Filter3.doFilter() [post] ←
                         Filter2.doFilter() [post] ←
Filter1.doFilter() [post] ←
→ response sent to client
```

---

## 9. Why and How Filters Are Used — Industry Use Cases

### Use Case 1: CORS Headers

Add `Access-Control-Allow-Origin` headers to every response before the browser sees them. This is the most common Filter use case — it must run at the container level, before Spring Security or the application logic.

```java
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        response.setHeader("Access-Control-Allow-Origin",  "https://my-frontend.com");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Trace-Id");
        response.setHeader("Access-Control-Max-Age",       "3600");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return; // ← do NOT call chain.doFilter() for preflight
        }

        chain.doFilter(request, response); // pass through for all other methods
    }
}
```

### Use Case 2: Request/Response Logging with Body Capture

Filters are the only layer that can capture the raw request and response bodies, because they can **wrap** the streams before passing them downstream.

```java
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Wrap so that the body can be read multiple times
        ContentCachingRequestWrapper  wrappedRequest  = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedRequest, wrappedResponse); // ← use wrapped versions!
        } finally {
            long duration = System.currentTimeMillis() - start;

            // Read cached body AFTER chain executes
            String requestBody  = new String(wrappedRequest.getContentAsByteArray());
            String responseBody = new String(wrappedResponse.getContentAsByteArray());

            log.info("[{}] {} {} → HTTP {} in {}ms | req={} | res={}",
                UUID.randomUUID().toString().substring(0, 8),
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                requestBody.length() > 200 ? requestBody.substring(0, 200) + "..." : requestBody,
                responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody
            );

            // CRITICAL: copy the cached response body back to the real response!
            wrappedResponse.copyBodyToResponse();
        }
    }
}
```

### Use Case 3: JWT Authentication at the Container Level

When using Spring Security, authentication is a `Filter` — not an interceptor. This is because it must run before the `DispatcherServlet` and must be able to short-circuit all downstream processing:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // OncePerRequestFilter (Spring helper) guarantees doFilterInternal()
    // is called exactly once per request, even in forward/include chains

    private final JwtTokenService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenService jwtService,
                                   UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response); // no token — let downstream decide
            return;
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    // ↑ set in SecurityContextHolder so Spring Security downstream sees it
                }
            }
        } catch (JwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid token\"}");
            return; // ← abort chain — do NOT call chain.doFilter()
        }

        chain.doFilter(request, response);
    }
}
```

### Use Case 4: Request Decompression / Encoding

Filters are the only place to handle content encoding (gzip, etc.) because they run before Spring parses the body:

```java
@Component
public class GzipDecompressionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        String encoding = request.getHeader("Content-Encoding");

        if ("gzip".equalsIgnoreCase(encoding)) {
            // Wrap with a decompressing stream — Spring sees uncompressed bytes
            chain.doFilter(new GzipRequestWrapper(request), res);
        } else {
            chain.doFilter(request, res);
        }
    }
}
```

---

## 10. Registering Filters — `@Component`, `@WebFilter`, and `FilterRegistrationBean`

There are three ways to register a Filter in Spring Boot.

### Option A: `@Component` (simplest — applies to all URLs)

```java
@Component           // ← Spring Boot auto-detects and registers this filter
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        String traceId = Optional
            .ofNullable(request.getHeader("X-Trace-Id"))
            .orElse(UUID.randomUUID().toString());

        MDC.put("traceId", traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}
```

`@Component` registers the filter for **all URL patterns (`/*`)** with a default order. You cannot control URL patterns or order with `@Component` alone.

---

### Option B: `FilterRegistrationBean` (full control — recommended for production)

`FilterRegistrationBean` is the standard Spring Boot way to register a filter with explicit URL patterns, order, and servlet name mappings:

```java
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    // ── Filter 1: Trace ID — all paths, highest priority ─────────────────
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter(TraceIdFilter filter) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(filter);
        registration.addUrlPatterns("/*");           // applies to ALL paths
        registration.setOrder(1);                    // lowest number = runs first
        registration.setName("traceIdFilter");

        return registration;
    }

    // ── Filter 2: CORS — all paths ────────────────────────────────────────
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(CorsFilter filter) {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        registration.setName("corsFilter");

        return registration;
    }

    // ── Filter 3: Auth — only API paths ──────────────────────────────────
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter(
            JwtAuthenticationFilter filter) {

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
            new FilterRegistrationBean<>();

        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");       // only /api/** paths
        registration.setOrder(3);
        registration.setName("jwtAuthFilter");

        return registration;
    }

    // ── Filter 4: Request/Response logging — API paths only ───────────────
    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> loggingFilter(
            RequestResponseLoggingFilter filter) {

        FilterRegistrationBean<RequestResponseLoggingFilter> registration =
            new FilterRegistrationBean<>();

        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*", "/admin/*"); // multiple patterns
        registration.setOrder(4);
        registration.setName("requestLoggingFilter");

        return registration;
    }
}
```

### `FilterRegistrationBean` — Key Properties

```
setFilter(filter)              → the Filter instance to register
addUrlPatterns("/*")           → Servlet-style URL patterns:
                                   /*         = all paths (Servlet-style, NOT /**)
                                   /api/*     = /api/ and one segment below
                                   /admin/*   = admin section
setOrder(int)                  → lower number = higher priority = runs first
                                 (Ordered.HIGHEST_PRECEDENCE = Integer.MIN_VALUE)
setName("name")                → filter name in Servlet context
setEnabled(true/false)         → toggle on/off without removing bean
addServletNames("dispatcher")  → restrict to named servlets
setDispatcherTypes(...)        → REQUEST, FORWARD, INCLUDE, ASYNC, ERROR
```

### URL Pattern Syntax — Filter vs Interceptor

```
Filter URL patterns (Servlet spec):     Interceptor path patterns (Ant):
  /*       = all paths                    /**     = all paths
  /api/*   = /api/users but NOT           /api/** = /api/users AND
             /api/users/123/orders                  /api/users/123/orders
                                                    (** = any depth)

⚠️  Filter patterns use Servlet spec glob: /* matches ONE level.
    To match all depths under /api, use  /api/*  and nested paths
    each separately, OR use /*  with filtering logic inside doFilter().
```

### Option C: `@WebFilter` + `@ServletComponentScan`

```java
import jakarta.servlet.annotation.WebFilter;

@WebFilter(urlPatterns = "/api/*", filterName = "apiFilter")
public class ApiFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        // ...
        chain.doFilter(req, res);
    }
}

// On main application class:
@SpringBootApplication
@ServletComponentScan   // ← required to detect @WebFilter
public class MyApp { ... }
```

`@WebFilter` cannot control order — use `FilterRegistrationBean` when order matters.

---

## 11. Advantages, Disadvantages, and Limitations of Filters

### Advantages

| Advantage | Detail |
|-----------|--------|
| **Runs before everything** | Executes before Spring Security, `DispatcherServlet`, and all Spring MVC processing |
| **Can wrap request/response** | Can replace `HttpServletRequest`/`HttpServletResponse` with custom implementations |
| **Can read/write raw bodies** | Using `ContentCachingRequestWrapper` / `ContentCachingResponseWrapper` |
| **Framework-agnostic** | Works for any Servlet — not tied to Spring MVC |
| **Applies to all servlets** | Runs for `DispatcherServlet`, static resources, error dispatcher, etc. |
| **Handles encoding/compression** | Can decompress or re-encode streams before the app sees them |
| **Full request lifecycle** | Can act on both the incoming request AND the outgoing response |

### Disadvantages

| Disadvantage | Detail |
|-------------|--------|
| **No Spring MVC context** | Cannot access `ModelAndView`, handler metadata, or controller info |
| **No fine-grained path matching** | Servlet glob (`/*`) is less expressive than Ant patterns (`/**`) used by Interceptors |
| **Cannot easily exclude paths** | Must implement path exclusion logic manually inside `doFilter()` |
| **Request body is a stream** | Reading it consumes it — must wrap with `ContentCachingRequestWrapper` to allow re-read |
| **Response body is a stream** | Must wrap with `ContentCachingResponseWrapper` and copy back; easy to forget |
| **Must call `chain.doFilter()`** | Forgetting to call it silently aborts the request with no error |

### Limitations

```
Things a Filter CANNOT do easily:
  ✗ Know which Spring controller will handle the request
  ✗ Read @RequestMapping annotations or method-level metadata
  ✗ Access Spring's ModelAndView or request-scoped beans (without workarounds)
  ✗ Use Ant-style path patterns (/api/**) natively
  ✗ Exclude paths cleanly (must implement manually)

Things only a Filter can do:
  ✓ Wrap and replace HttpServletRequest / HttpServletResponse
  ✓ Read and buffer the request/response body streams
  ✓ Intercept ALL servlets (not just DispatcherServlet)
  ✓ Act before Spring Security runs
  ✓ Handle encoding, compression, multipart pre-processing
  ✓ Modify the URL or query string before routing
```

---

## 12. Where Filters Work — Layer Diagram and Internal Mechanics

### Full Layer Diagram

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                  Complete Request Processing Stack                             │
│                                                                                │
│  Client                                                                        │
│    │ HTTP Request                                                              │
│    ▼                                                                           │
│  ┌──────────────────────────────────────────────────────────────────────────┐ │
│  │                        OS / NIO Layer                                    │ │
│  │  Tomcat NIO Connector reads bytes off the socket                         │ │
│  └──────────────────────────────┬───────────────────────────────────────────┘ │
│                                 │                                              │
│                                 ▼                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐ │
│  │               Tomcat Servlet Container                                   │ │
│  │                                                                          │ │
│  │  ┌────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                     Filter Chain (ApplicationFilterChain)          │ │ │
│  │  │                                                                    │ │ │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │ │ │
│  │  │  │  TraceId     │  │   CORS       │  │  JWT Auth    │  ...        │ │ │
│  │  │  │  Filter      │→ │  Filter      │→ │  Filter      │→           │ │ │
│  │  │  │  (order=1)   │  │  (order=2)   │  │  (order=3)   │             │ │ │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘             │ │ │
│  │  │                                                                    │ │ │
│  │  └───────────────────────────────┬────────────────────────────────────┘ │ │
│  │                                  │                                       │ │
│  │                                  ▼                                       │ │
│  │  ┌────────────────────────────────────────────────────────────────────┐ │ │
│  │  │               DispatcherServlet (Spring MVC)                       │ │ │
│  │  │                                                                    │ │ │
│  │  │   HandlerMapping → HandlerInterceptors → Controller → Response     │ │ │
│  │  └────────────────────────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────┘
```

### Tomcat's `ApplicationFilterChain` — Internal Mechanics

Tomcat maintains a list of `ApplicationFilterConfig` objects for each request. The `ApplicationFilterChain` iterates through them, calling `doFilter()` on each in turn:

```java
// Tomcat's ApplicationFilterChain (simplified)
public void doFilter(ServletRequest request, ServletResponse response)
        throws IOException, ServletException {

    // Called by each filter via chain.doFilter()
    // This is also called to start the chain initially

    if (pos < n) {
        // More filters remain — get the next one
        ApplicationFilterConfig filterConfig = filters[pos++];
        Filter filter = filterConfig.getFilter();

        filter.doFilter(request, response, this); // ← call filter, pass 'this' as chain
        // ↑ Inside the filter, calling chain.doFilter() calls THIS method again
        //   with pos incremented — picks up the next filter

    } else {
        // No more filters — invoke the Servlet
        servlet.service(request, response);
        // ← for Spring Boot, this is DispatcherServlet.service()
    }
}
```

```
Recursion/Stack diagram for 3 filters:

ApplicationFilterChain.doFilter() [pos=0]
  → TraceIdFilter.doFilter()
      → chain.doFilter()  → ApplicationFilterChain.doFilter() [pos=1]
                              → CorsFilter.doFilter()
                                  → chain.doFilter() → ApplicationFilterChain.doFilter() [pos=2]
                                                          → JwtFilter.doFilter()
                                                              → chain.doFilter() → [pos=3 == n]
                                                                  → DispatcherServlet.service()
                                                                  ← returns
                                                          JwtFilter post-processing ←
                                  CorsFilter post-processing ←
      TraceIdFilter post-processing ←
→ response returned to client
```

### `OncePerRequestFilter` — Spring's Safe Base Class

Spring provides `OncePerRequestFilter` to prevent double invocation of filters in `FORWARD`/`INCLUDE` dispatch scenarios (common with error handling):

```java
// org.springframework.web.filter.OncePerRequestFilter (simplified)
public abstract class OncePerRequestFilter extends GenericFilterBean {

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response,
                               FilterChain filterChain)
            throws ServletException, IOException {

        String alreadyFilteredAttr = getAlreadyFilteredAttributeName();

        if (request.getAttribute(alreadyFilteredAttr) != null) {
            // Already ran for this request — skip!
            filterChain.doFilter(request, response);
            return;
        }

        request.setAttribute(alreadyFilteredAttr, Boolean.TRUE);
        try {
            doFilterInternal((HttpServletRequest) request,
                             (HttpServletResponse) response,
                             filterChain);
        } finally {
            request.removeAttribute(alreadyFilteredAttr);
        }
    }

    // Subclasses implement this instead of doFilter()
    protected abstract void doFilterInternal(HttpServletRequest request,
                                             HttpServletResponse response,
                                             FilterChain filterChain)
            throws ServletException, IOException;
}
```

> Always extend `OncePerRequestFilter` rather than implementing `Filter` directly when building application-level filters in Spring Boot.

---

## 13. Multiple Filters — Ordering and Execution

### Registering Multiple Filters with Controlled Order

```java
@Configuration
public class FilterConfig {

    // Order 1 — runs FIRST on the way IN, LAST on the way OUT
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new TraceIdFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);    // = Integer.MIN_VALUE
        reg.setName("traceIdFilter");
        return reg;
    }

    // Order 2
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        FilterRegistrationBean<CorsFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new CorsFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        reg.setName("corsFilter");
        return reg;
    }

    // Order 3 — JWT auth for /api/* only
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/api/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        reg.setName("jwtFilter");
        return reg;
    }

    // Order 4 — request/response logging, narrower scope
    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> loggingFilter(
            RequestResponseLoggingFilter filter) {
        FilterRegistrationBean<FilterRegistrationBean> reg =
            new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/api/*", "/admin/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
        reg.setName("loggingFilter");
        return reg;
    }
}
```

### Execution Order Diagram

```
Filters registered (by order):  [TraceId(1), CORS(2), JWT(3), Logging(4)]

Incoming request (→):
  TraceId.doFilter [pre]
    → CORS.doFilter [pre]
       → JWT.doFilter [pre]  (only for /api/* — others skipped by URL pattern)
          → Logging.doFilter [pre]
             → DispatcherServlet.service()  ← Spring MVC takes over here
             ← DispatcherServlet returns
          Logging.doFilter [post]
       JWT.doFilter [post]
    CORS.doFilter [post]
  TraceId.doFilter [post]
→ Response sent to client
```

### Manually Excluding Paths Inside a Filter

Filters cannot use `excludePathPatterns()` like Interceptors — you must implement the logic yourself:

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // List of paths that do NOT require a token
    private static final List<String> EXCLUDED_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/signup",
        "/api/public",
        "/actuator/health"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Spring's OncePerRequestFilter calls this to decide whether to skip
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        // Only reached for paths NOT in the excluded list
        String header = request.getHeader("Authorization");
        // ... validate JWT ...
        chain.doFilter(request, response);
    }
}
```

### Complete Three-Filter Example

```java
// ─── Filter 1: Trace ID propagation (all paths) ──────────────────────
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String traceId = Optional
            .ofNullable(request.getHeader("X-Trace-Id"))
            .orElse(UUID.randomUUID().toString());

        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId); // echo back to client
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}

// ─── Filter 2: CORS (all paths) ─────────────────────────────────────
public class CorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Origin",  "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return; // preflight — do NOT continue chain
        }
        chain.doFilter(request, response);
    }
}

// ─── Filter 3: JWT Auth (/api/* only) ─────────────────────────────
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtService;

    public JwtAuthenticationFilter(JwtTokenService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
            return;
        }
        try {
            String username = jwtService.extractUsername(header.substring(7));
            request.setAttribute("username", username);
            chain.doFilter(request, response);
        } catch (JwtException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }
}

// ─── Registration ─────────────────────────────────────────────────
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        var reg = new FilterRegistrationBean<>(new TraceIdFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        var reg = new FilterRegistrationBean<>(new CorsFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(2);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter(
            JwtTokenService jwtService) {
        var reg = new FilterRegistrationBean<>(new JwtAuthenticationFilter(jwtService));
        reg.addUrlPatterns("/api/*");
        reg.setOrder(3);
        return reg;
    }
}
```

```
Request: POST /api/users  (with valid JWT)
  TraceId  [pre]  → set MDC traceId
  CORS     [pre]  → set CORS headers
  JWT      [pre]  → validate token, set username attribute
                  → DispatcherServlet → Controller executes
  JWT      [post] → (nothing)
  CORS     [post] → (nothing)
  TraceId  [post] → MDC.clear()

Request: OPTIONS /api/users  (CORS preflight)
  TraceId  [pre]  → set traceId
  CORS     [pre]  → set CORS headers, return 200 immediately
                    chain.doFilter() NOT called
  TraceId  [post] → MDC.clear()
  (JWT never reached)

Request: POST /api/auth/login  (excluded from JWT)
  TraceId  [pre]
  CORS     [pre]
  JWT      skipped (shouldNotFilter = true for /api/auth)
           → DispatcherServlet → LoginController
  CORS     [post]
  TraceId  [post]
```

---

## 14. Filter vs Interceptor — Complete Comparison

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Filter  vs  Interceptor                                │
│                                                                             │
│  Dimension              │  Filter (javax.servlet.Filter)  │  Interceptor   │
│  ───────────────────────┼──────────────────────────────────┼────────────────│
│  Layer                  │  Servlet container (Tomcat)      │  Spring MVC    │
│  Runs before            │  Everything including Spring Sec │  Controller    │
│  Path matching          │  Servlet glob (/* = 1 level)     │  Ant (/**=deep)│
│  Exclude paths          │  Manual in code                  │  Built-in API  │
│  Request body access    │  Yes (wrap with CachingWrapper)  │  No            │
│  Response body access   │  Yes (wrap + copy back)          │  No            │
│  Knows the handler      │  No                              │  Yes           │
│  ModelAndView access    │  No                              │  Yes           │
│  Spring beans injection │  Yes (as @Component or DI)       │  Yes           │
│  Non-Spring endpoints   │  Yes (all servlets)              │  No            │
│  Order control          │  FilterRegistrationBean.setOrder │  Registry order│
│  Skip per request       │  shouldNotFilter() in OPRF       │  return false  │
│  Exception handling     │  Manual try/catch                │  afterCompletion│
│  Typical uses           │  CORS, JWT auth, logging,        │  Auth check,   │
│                         │  compression, tracing            │  rate limit,   │
│                         │                                  │  locale, audit │
└─────────────────────────────────────────────────────────────────────────────┘

Decision guide:
  Need to wrap or read the request/response body?          → Filter
  Need to act before Spring Security?                      → Filter
  Need to apply to ALL servlets, not just DispatcherServlet? → Filter
  Need to handle encoding, compression, multipart?         → Filter
  Need to know which @Controller will handle the request?  → Interceptor
  Need clean Ant path patterns with built-in exclusions?   → Interceptor
  Need ModelAndView or post-controller model modification? → Interceptor
  Need Spring MVC exception handler integration?           → Interceptor
```

---

## 15. Filter vs Interceptor — Deep Comparison

### The Fundamental Difference: Where They Sit in the Stack

```
Client
  │
  ▼ HTTP Request
┌─────────────────────────────────────────────────────────────────────────┐
│                     Tomcat (Servlet Container)                          │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Filter Chain                                                    │  │
│  │  [Filter 1] → [Filter 2] → [Filter 3]                           │  │
│  │                                                                  │  │
│  │  ← operates on raw HttpServletRequest / HttpServletResponse      │  │
│  │  ← knows nothing about Spring, handlers, or controllers          │  │
│  │  ← runs for ALL servlets (DispatcherServlet, static, error, ...) │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                │                                        │
│                                ▼                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  DispatcherServlet                                               │  │
│  │                                                                  │  │
│  │    ┌───────────────────────────────────────────────────────┐    │  │
│  │    │  HandlerInterceptor Chain                             │    │  │
│  │    │  [Interceptor A] → [Interceptor B] → Controller      │    │  │
│  │    │                                                       │    │  │
│  │    │  ← knows the handler (controller + method)            │    │  │
│  │    │  ← can read @Async, @Transactional, custom annotations│    │  │
│  │    │  ← runs ONLY inside DispatcherServlet                 │    │  │
│  │    │  ← has access to Spring MVC context, ModelAndView     │    │  │
│  │    └───────────────────────────────────────────────────────┘    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
  │
  ▼ HTTP Response
Client
```

The key insight: **Filters are outside Spring. Interceptors are inside Spring.**

---

### Side-by-Side Comparison — Every Dimension

| Dimension | Filter | Interceptor |
|-----------|--------|-------------|
| **Interface** | `jakarta.servlet.Filter` | `org.springframework.web.servlet.HandlerInterceptor` |
| **Spec** | Servlet specification (Jakarta EE) | Spring MVC framework |
| **Layer** | Servlet container (Tomcat) | Spring `DispatcherServlet` |
| **Runs for** | All servlets (static resources, error pages, actuator, etc.) | Only `DispatcherServlet`-routed requests |
| **Execution position** | Before `DispatcherServlet` | After `DispatcherServlet` resolves the handler |
| **Knows the controller** | No — only sees URL and HTTP headers | Yes — `Object handler` is the `HandlerMethod` |
| **Can read method annotations** | No | Yes — cast `handler` to `HandlerMethod` and read annotations |
| **ModelAndView access** | No | Yes — in `postHandle()` |
| **Request body stream** | Yes — can wrap and buffer it | No — body already consumed by Jackson/Spring |
| **Response body stream** | Yes — can wrap and buffer it | No — body already written |
| **Path matching style** | Servlet glob (`/*`, `/api/*`) | Ant patterns (`/**`, `/api/**`) |
| **Exclude paths** | Manual: `shouldNotFilter()` or if-check inside `doFilter()` | Built-in: `excludePathPatterns()` in registry |
| **Order control** | `FilterRegistrationBean.setOrder(int)` | Registration order in `addInterceptors()` |
| **Spring bean injection** | Yes (`@Component` or constructor in `FilterRegistrationBean`) | Yes (full Spring context) |
| **Spring Security integration** | Yes — Spring Security IS a filter chain | No — runs after Spring Security |
| **Can abort the chain** | Yes — don't call `chain.doFilter()` | Yes — return `false` from `preHandle()` |
| **Exception handling** | Manual `try/catch` or container error page | `afterCompletion(ex)` + `@ControllerAdvice` still applies |
| **Registration mechanism** | `@Component`, `FilterRegistrationBean`, `@WebFilter` | `WebMvcConfigurer.addInterceptors()` |
| **Applicable to non-MVC code** | Yes | No |

---

### Execution Lifecycle Comparison

```
Filter lifecycle:                       Interceptor lifecycle:

init()  ← called once at startup        (no init — Spring manages the bean)
  │
  ▼
doFilter() [pre]  ◄─────────────────── preHandle()
  │                                       │
  chain.doFilter()                         Controller executes
  │                                       │
doFilter() [post] ◄─────────────────── postHandle()
  │                                       │
destroy() ← called at shutdown           afterCompletion()

Key difference:
  Filter has ONE method (doFilter) split by chain.doFilter() call.
  Interceptor has THREE distinct methods for before/after/completion.
```

---

### What Each Can and Cannot Read

```
Scenario: POST /api/orders  with JSON body {"amount": 100}

Filter:
  ✅ request.getHeader("Authorization")   — yes, headers
  ✅ request.getRequestURI()              — yes, URL
  ✅ request.getInputStream()            — yes, raw body bytes (once!)
  ✅ response.getOutputStream()          — yes, can write/wrap response
  ❌ which controller handles this        — no, routing not yet done
  ❌ @RequestMapping metadata             — no
  ❌ Spring ModelAndView                  — no

Interceptor:
  ✅ request.getHeader("Authorization")   — yes
  ✅ request.getRequestURI()              — yes
  ✅ handler metadata (controller method) — yes, via HandlerMethod
  ✅ method-level annotations             — yes, e.g., @PreAuthorize, custom
  ✅ ModelAndView in postHandle           — yes, can add model attributes
  ❌ request body (already read by Spring)— no (body consumed by Jackson)
  ❌ response body (already written)      — no
```

#### Reading Handler Annotations in an Interceptor

This is a powerful pattern unique to interceptors — no filter can do this:

```java
@Component
public class RequiresRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true; // not a controller method — skip
        }

        // Read a custom annotation from the controller METHOD
        RequiresRole annotation = handlerMethod.getMethodAnnotation(RequiresRole.class);
        if (annotation == null) {
            // Also check on the CLASS level
            annotation = handlerMethod.getBeanType().getAnnotation(RequiresRole.class);
        }

        if (annotation != null) {
            String requiredRole = annotation.value();
            UserClaims claims = (UserClaims) request.getAttribute("userClaims");
            if (claims == null || !claims.getRoles().contains(requiredRole)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }
        return true;
    }
}

// Custom annotation
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    String value();
}

// Usage in controller
@RestController
public class AdminController {

    @GetMapping("/admin/reports")
    @RequiresRole("ROLE_ADMIN")           // ← interceptor reads this
    public List<Report> getReports() { ... }
}
```

---

### Decision Guide — When to Use Which

```
┌────────────────────────────────────────────────────────────────────────────┐
│                  Decision Guide: Filter vs Interceptor                     │
│                                                                            │
│  Question                                          → Use                  │
│  ──────────────────────────────────────────────────────────────────────── │
│                                                                            │
│  Do you need to read or modify the request BODY?   → Filter               │
│    (e.g., log raw JSON, decompress gzip payload)     (wrap with           │
│                                                        ContentCachingWrapper)│
│                                                                            │
│  Do you need to read or modify the response BODY?  → Filter               │
│    (e.g., log response JSON, inject response headers)(wrap +              │
│                                                        copyBodyToResponse) │
│                                                                            │
│  Do you need to run BEFORE Spring Security?         → Filter               │
│    (e.g., add tenant context before auth)            (Spring Security is  │
│                                                        itself a Filter)    │
│                                                                            │
│  Do you need to intercept ALL servlets?             → Filter               │
│    (not just DispatcherServlet — static files, error)                      │
│                                                                            │
│  Do you need to handle encoding, compression,       → Filter               │
│  multipart, or raw HTTP streams?                                           │
│                                                                            │
│  ──────────────────────────────────────────────────────────────────────── │
│                                                                            │
│  Do you need to know WHICH controller method        → Interceptor          │
│  will handle the request?                                                  │
│                                                                            │
│  Do you need to read METHOD-LEVEL annotations?      → Interceptor          │
│    (e.g., @RequiresRole, @AuditLog, @RateLimit)                            │
│                                                                            │
│  Do you need to modify the ModelAndView             → Interceptor          │
│  before the view renders?                                                  │
│                                                                            │
│  Do you need clean Ant path matching with           → Interceptor          │
│  built-in excludePathPatterns()?                                           │
│    (e.g., exclude /api/auth/** from JWT check)                             │
│                                                                            │
│  Do you want the exception from the controller      → Interceptor          │
│  available in your hook (afterCompletion)?                                 │
│                                                                            │
│  ──────────────────────────────────────────────────────────────────────── │
│                                                                            │
│  Could work in either — use simpler option:                                │
│    Logging (no body)?    → Interceptor (simpler, no wrapper needed)        │
│    JWT auth validation?  → Interceptor (if no Spring Security)             │
│                          → Filter (if using Spring Security)               │
│    Rate limiting?        → Interceptor (if MVC-only) or Filter (all URLs)  │
│    CORS?                 → Filter (must run before DispatcherServlet)       │
└────────────────────────────────────────────────────────────────────────────┘
```

---

### Real-World Architecture: Using Both Together

In a production Spring Boot application, Filters and Interceptors are used simultaneously — each at the right layer:

```
┌──────────────────────────────────────────────────────────────────────────┐
│          Real Production Architecture — Both Layers Working Together     │
│                                                                          │
│  Request: POST /api/orders  (JWT: Bearer eyJ...)                         │
│                                                                          │
│  ─────────────────────── FILTER LAYER ──────────────────────────────────│
│                                                                          │
│  1. TraceIdFilter (order=1, /*)                                          │
│       → generates traceId, sets MDC                                      │
│       → adds X-Trace-Id response header                                  │
│                                                                          │
│  2. CorsFilter (order=2, /*)                                             │
│       → sets Access-Control-Allow-* headers                              │
│                                                                          │
│  3. RequestBodyCachingFilter (order=3, /api/*)                           │
│       → wraps request with ContentCachingRequestWrapper                  │
│       → wraps response with ContentCachingResponseWrapper                │
│       → logs full req/res bodies after chain returns                     │
│                                                                          │
│  4. Spring Security Filter Chain (order=100)                             │
│       → JwtAuthFilter: validates JWT → sets SecurityContext              │
│       → AuthorizationFilter: checks authorities                          │
│                                                                          │
│  ──────────────────── DISPATCHERSERVLET BOUNDARY ──────────────────────  │
│                                                                          │
│  ─────────────────── INTERCEPTOR LAYER ─────────────────────────────────│
│                                                                          │
│  5. RequestLoggingInterceptor (all /**)                                  │
│       → logs controller method name, timing                              │
│                                                                          │
│  6. RequiresRoleInterceptor (/api/**)                                    │
│       → reads @RequiresRole annotation on the matched controller method  │
│       → validates user has the required role from SecurityContext         │
│                                                                          │
│  7. TenantContextInterceptor (/api/**)                                   │
│       → reads X-Tenant-Id header                                         │
│       → sets TenantContext.set(tenantId) for DB routing                  │
│                                                                          │
│  ──────────────────────────────────────────────────────────────────────  │
│                                                                          │
│  8. OrderController.createOrder()   ← actual business logic              │
│                                                                          │
│  ──── after controller ──────────────────────────────────────────────── │
│                                                                          │
│  Interceptors (reverse): TenantContext cleanup → RoleCheck → Logging    │
│  Filters (reverse):      SecurityChain → BodyLogging (log response) →   │
│                          TraceId (MDC.clear())                           │
└──────────────────────────────────────────────────────────────────────────┘
```

```
Responsibility split:
  Filters own:       tracing, CORS, body buffering, Spring Security auth
  Interceptors own:  annotation-based auth checks, tenant context,
                     controller-level audit logging, ModelAndView enrichment

This is the correct production pattern:
  LOW-LEVEL / STREAM WORK    → Filter
  HIGH-LEVEL / SPRING WORK   → Interceptor
```

---

### Quick Reference Cheat Sheet

```
USE FILTER WHEN:                           USE INTERCEPTOR WHEN:
─────────────────────────────────────────  ──────────────────────────────────────
✓ Reading/writing request body             ✓ Checking custom method annotations
✓ Reading/writing response body            ✓ Accessing handler (controller) info
✓ CORS header injection                    ✓ Modifying ModelAndView
✓ Gzip / encoding handling                 ✓ MVC-specific path exclusions
✓ Works with Spring Security               ✓ Clean preHandle / postHandle split
✓ Must apply to non-MVC endpoints          ✓ Exception context in afterCompletion
✓ Multipart pre-processing                 ✓ Locale, tenant, MDC propagation
✓ URL rewriting before routing             ✓ Per-controller audit logging
```

---

## 16. What is an Annotation?

An **annotation** in Java is a form of metadata — a label you attach to a class, method, field, parameter, or constructor to provide information to the compiler, the JVM, or a framework at runtime. Annotations do NOT directly alter the execution of the code they annotate. Instead, other code (a framework, an AOP aspect, or the compiler itself) reads the annotation and decides what to do.

```
┌──────────────────────────────────────────────────────────────────────┐
│                  What an Annotation Is                               │
│                                                                      │
│  Source code:                                                        │
│    @Transactional                                                    │
│    public void placeOrder(Order order) { ... }                       │
│                                                                      │
│  @Transactional is just a MARKER — it does nothing by itself.        │
│                                                                      │
│  Spring reads it at startup → wraps the bean in a proxy →            │
│  proxy intercepts the call → begins/commits/rolls back a TX.         │
│                                                                      │
│  Without the Spring proxy, @Transactional is inert metadata.         │
└──────────────────────────────────────────────────────────────────────┘
```

### Annotation Declaration Syntax

An annotation type is declared with `@interface`:

```java
// Minimal annotation
public @interface MyAnnotation {}

// Annotation with attributes (elements)
public @interface MyAnnotation {
    String value();             // attribute named "value" (special: can omit name)
    String name() default "";   // attribute with a default — optional to provide
    int    retries() default 3;
    Class<?>[] groups() default {};
}
```

---

## 17. Meta-Annotations — Properties of an Annotation

When you declare an annotation, you decorate it with **meta-annotations** that control where it can be applied, how long it lives, and how it appears in documentation.

### `@Target` — Where the Annotation Can Be Applied

`@Target` restricts which Java program elements the annotation is allowed to annotate. It takes an array of `ElementType` enum values.

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({
    ElementType.METHOD,          // on methods
    ElementType.TYPE,            // on classes, interfaces, enums, records
    ElementType.FIELD,           // on fields (instance variables)
    ElementType.PARAMETER,       // on method parameters
    ElementType.CONSTRUCTOR,     // on constructors
    ElementType.LOCAL_VARIABLE,  // on local variables inside methods
    ElementType.ANNOTATION_TYPE, // on other annotation declarations (meta-annotation)
    ElementType.PACKAGE,         // on package declarations (package-info.java)
    ElementType.TYPE_PARAMETER,  // on generic type parameters <T>
    ElementType.TYPE_USE,        // on any use of a type (Java 8+)
    ElementType.RECORD_COMPONENT // on record components (Java 16+)
})
public @interface MyAnnotation {}
```

```
If @Target is OMITTED → annotation can be placed anywhere.
If @Target is specified → compiler enforces: placing it elsewhere = compile error.

Common combinations:
  @Target(ElementType.METHOD)                       → method-only
  @Target({ElementType.METHOD, ElementType.TYPE})   → methods and classes
  @Target(ElementType.FIELD)                        → field injection (@Autowired style)
  @Target(ElementType.PARAMETER)                    → @PathVariable, @RequestParam style
```

### `@Retention` — How Long the Annotation Lives

`@Retention` controls at which point in the compilation and execution lifecycle the annotation is available.

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)   // discarded by compiler — only in source
@Retention(RetentionPolicy.CLASS)    // in .class file, NOT available at runtime (default)
@Retention(RetentionPolicy.RUNTIME)  // in .class AND available via reflection at runtime
```

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Retention Policy Lifecycle                        │
│                                                                      │
│  SOURCE → .java source code only                                     │
│    Used by: compiler checks (@Override, @SuppressWarnings)           │
│    Discarded: by javac before .class is written                      │
│                                                                      │
│  CLASS → .class bytecode (default if @Retention omitted)             │
│    Used by: bytecode manipulation tools (some APT processors)        │
│    NOT available at runtime via Class.getAnnotations()               │
│                                                                      │
│  RUNTIME → .class bytecode AND loaded by JVM, accessible via        │
│            reflection                                                │
│    Used by: Spring, JPA, Jackson, any runtime framework              │
│    Available: method.getAnnotation(MyAnnotation.class) works         │
│                                                                      │
│  RULE: If you want Spring AOP or interceptors to READ your           │
│  annotation at runtime → ALWAYS use RetentionPolicy.RUNTIME          │
└──────────────────────────────────────────────────────────────────────┘
```

### `@Documented` — Include in Javadoc

`@Documented` causes the annotation to appear in the generated Javadoc of elements it annotates. Without it, annotations are hidden from the documentation.

```java
import java.lang.annotation.Documented;

@Documented   // ← this annotation will show up in Javadoc
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditLog {
    String action();
}
```

### `@Inherited` — Inherit Annotation through Class Hierarchy

`@Inherited` allows an annotation on a **class** to be inherited by its subclasses. It has NO effect on method or field annotations.

```java
import java.lang.annotation.Inherited;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Secured {}

@Secured
public class BaseService {}

public class UserService extends BaseService {}
// UserService.class.isAnnotationPresent(Secured.class) → true (inherited!)
// Without @Inherited → false
```

### `@Repeatable` — Apply the Same Annotation Multiple Times

`@Repeatable` (Java 8+) allows the same annotation to appear more than once on the same element.

```java
import java.lang.annotation.Repeatable;

@Repeatable(Schedules.class)   // ← the container annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Schedule {
    String cron();
}

// Container annotation required by @Repeatable
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Schedules {
    Schedule[] value();
}

// Usage — same annotation applied twice:
@Schedule(cron = "0 0 9 * * MON-FRI")
@Schedule(cron = "0 0 12 * * SAT")
public void runReport() { ... }
```

---

## 18. Annotation Attributes (Elements) — Passing Data

Annotation attributes are declared as methods on the `@interface`. They can have default values, and may be of these types only: primitives, `String`, `Class<?>`, enum, another annotation type, or arrays of any of these.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RateLimit {

    // ── Simple string attribute ─────────────────────────────────────
    String key() default "";               // key used to identify the limit bucket

    // ── Numeric attributes ─────────────────────────────────────────
    int    requests() default 100;         // max requests per window
    long   windowMs() default 60_000L;     // window size in milliseconds

    // ── Enum attribute ─────────────────────────────────────────────
    Scope  scope() default Scope.IP;       // per-IP or per-USER limiting

    // ── Class attribute ────────────────────────────────────────────
    Class<? extends KeyResolver> resolver() default DefaultKeyResolver.class;

    // ── Array attribute ────────────────────────────────────────────
    String[] excludePaths() default {};    // paths to skip

    // ── Nested annotation attribute ────────────────────────────────
    RetryConfig retry() default @RetryConfig(maxAttempts = 0);

    // Special: element named "value" can be set without the name
    String value() default "";             // @RateLimit("my-key") shorthand

    enum Scope { IP, USER, GLOBAL }
}
```

### Using the Annotation with Attributes

```java
// All attributes — explicit names
@RateLimit(
    key       = "order-creation",
    requests  = 10,
    windowMs  = 60_000L,
    scope     = RateLimit.Scope.USER,
    excludePaths = {"/api/health", "/api/metrics"}
)
public ResponseEntity<Order> createOrder(@RequestBody OrderRequest req) { ... }

// Only the special "value" attribute — name can be omitted
@RateLimit("payment-processing")    // equivalent to @RateLimit(value = "payment-processing")
public ResponseEntity<Payment> pay(@RequestBody PaymentRequest req) { ... }

// Class-level — applies to ALL methods in the controller
@RateLimit(requests = 1000, scope = RateLimit.Scope.GLOBAL)
@RestController
public class PublicApiController { ... }
```

### Reading Annotation Attributes via Reflection

```java
// Reading at runtime (RetentionPolicy.RUNTIME required)
Method method = OrderController.class.getMethod("createOrder", OrderRequest.class);

RateLimit annotation = method.getAnnotation(RateLimit.class);
if (annotation != null) {
    System.out.println("key:      " + annotation.key());      // "order-creation"
    System.out.println("requests: " + annotation.requests()); // 10
    System.out.println("scope:    " + annotation.scope());    // USER
    System.out.println("exclude:  " + Arrays.toString(annotation.excludePaths()));
}

// Also check the class-level annotation:
RateLimit classAnnotation = OrderController.class.getAnnotation(RateLimit.class);
```

---

## 19. Custom Annotation + AOP `@Around` Advice

The most powerful pattern in Spring is combining a **custom annotation** with an **AOP `@Around` advice**. This lets you intercept any annotated method transparently — no interface needed, no inheritance required.

### How It Works

```
┌──────────────────────────────────────────────────────────────────────────┐
│           Custom Annotation + AOP — End-to-End Flow                     │
│                                                                          │
│  1. You declare a custom annotation (@AuditLog, @RateLimit, etc.)        │
│  2. You write an @Aspect class with @Around pointing to the annotation   │
│  3. Spring Boot creates a CGLIB proxy for every bean that has the        │
│     annotation on one of its methods                                     │
│  4. When the method is called through the proxy, the @Around advice      │
│     runs FIRST — it can inspect arguments, call the real method,         │
│     modify the return value, catch exceptions                            │
│                                                                          │
│  Caller → [CGLIB Proxy] → @Around advice → ProceedingJoinPoint.proceed()│
│                                              → real method body          │
│                           @Around advice ← real method returns           │
│  Caller ←────────────────────────────────────────────────────────────── │
└──────────────────────────────────────────────────────────────────────────┘
```

### Step 1: Declare the Custom Annotation

```java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)  // MUST be RUNTIME for AOP to read it
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface AuditLog {

    String action() default "";       // business action name, e.g. "CREATE_ORDER"
    String resource() default "";     // resource type, e.g. "ORDER"
    boolean logArgs() default true;   // whether to log method arguments
    boolean logResult() default false;// whether to log return value
}
```

### Step 2: Write the `@Aspect` with `@Around`

```java
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditRepository auditRepository;

    public AuditLogAspect(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    // Pointcut: any method annotated with @AuditLog
    @Around("@annotation(auditLog)")
    public Object auditMethod(ProceedingJoinPoint joinPoint,
                              AuditLog auditLog) throws Throwable {
        // ── BEFORE the method ─────────────────────────────────────────────
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className  = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        String action   = auditLog.action().isEmpty() ? methodName : auditLog.action();
        String resource = auditLog.resource();

        if (auditLog.logArgs()) {
            log.info("[AUDIT] {}.{} | action={} resource={} | args={}",
                className, methodName, action, resource,
                Arrays.toString(joinPoint.getArgs()));
        }

        long start = System.currentTimeMillis();
        Object result = null;
        Throwable thrown = null;

        try {
            // ── INVOKE the real method ────────────────────────────────────
            result = joinPoint.proceed(); // ← calls the actual method
            return result;

        } catch (Throwable ex) {
            thrown = ex;
            throw ex; // re-throw — do NOT swallow!

        } finally {
            // ── AFTER the method (success or failure) ─────────────────────
            long duration = System.currentTimeMillis() - start;
            String status = (thrown == null) ? "SUCCESS" : "FAILURE";

            if (auditLog.logResult() && result != null) {
                log.info("[AUDIT] {}.{} | status={} | duration={}ms | result={}",
                    className, methodName, status, duration, result);
            } else {
                log.info("[AUDIT] {}.{} | status={} | duration={}ms",
                    className, methodName, status, duration);
            }

            // Persist to audit table
            auditRepository.save(AuditEntry.builder()
                .action(action)
                .resource(resource)
                .status(status)
                .durationMs(duration)
                .error(thrown != null ? thrown.getMessage() : null)
                .timestamp(Instant.now())
                .build());
        }
    }
}
```

### Step 3: Enable AOP and Use the Annotation

```java
// Enable AOP — in Spring Boot this is auto-configured if spring-boot-starter-aop is on classpath
// But can be explicit:
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {}
```

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PostMapping
    @AuditLog(action = "CREATE_ORDER", resource = "ORDER", logArgs = true, logResult = true)
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        Order order = orderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @DeleteMapping("/{id}")
    @AuditLog(action = "DELETE_ORDER", resource = "ORDER")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // No @AuditLog — aspect does NOT fire
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.find(id));
    }
}
```

```
Request: POST /api/orders

Caller → [OrderControllerProxy (CGLIB)] → AuditLogAspect.auditMethod()
                                              logs args
                                              → joinPoint.proceed()
                                                  → OrderController.createOrder() executes
                                              logs result and duration
                                              → saves AuditEntry to DB
           ← returns ResponseEntity to caller
```

---

### `@Around` Pointcut Expressions — All Variants

```java
@Aspect
@Component
public class ExampleAspect {

    // ① Any method annotated with @AuditLog (by annotation reference)
    @Around("@annotation(auditLog)")
    public Object byAnnotationRef(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        // auditLog is the annotation instance — access attributes directly
        System.out.println("action: " + auditLog.action());
        return pjp.proceed();
    }

    // ② Any method annotated with @AuditLog (by fully qualified class name)
    @Around("@annotation(com.example.annotation.AuditLog)")
    public Object byFQN(ProceedingJoinPoint pjp) throws Throwable {
        // Annotation NOT injected — must get it via reflection:
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        AuditLog ann = sig.getMethod().getAnnotation(AuditLog.class);
        System.out.println("action: " + ann.action());
        return pjp.proceed();
    }

    // ③ Any class annotated with @AuditLog (applies to ALL methods in that class)
    @Around("@within(auditLog)")
    public Object byClassAnnotation(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        System.out.println("class-level action: " + auditLog.action());
        return pjp.proceed();
    }

    // ④ Combined: annotation on the method OR on the class
    @Around("@annotation(com.example.annotation.AuditLog) || @within(com.example.annotation.AuditLog)")
    public Object byMethodOrClass(ProceedingJoinPoint pjp) throws Throwable {
        // resolve annotation from method first, then class
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        AuditLog ann = sig.getMethod().getAnnotation(AuditLog.class);
        if (ann == null) {
            ann = sig.getMethod().getDeclaringClass().getAnnotation(AuditLog.class);
        }
        System.out.println("action: " + (ann != null ? ann.action() : "unknown"));
        return pjp.proceed();
    }
}
```

---

### Real-World Example: `@RateLimit` Custom Annotation with AOP

```java
// ─── Step 1: The annotation ───────────────────────────────────────────
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface RateLimit {
    String key() default "";
    int    requests() default 100;
    long   windowMs() default 60_000L;
}

// ─── Step 2: The aspect ───────────────────────────────────────────────
@Aspect
@Component
public class RateLimitAspect {

    // Using a simple in-memory map; production would use Redis
    private final Map<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {

        // Resolve the bucket key: annotation key OR class.method name
        String bucketKey = rateLimit.key().isEmpty()
            ? pjp.getSignature().toShortString()
            : rateLimit.key();

        long now = System.currentTimeMillis();
        long windowStart = now - rateLimit.windowMs();

        // Thread-safe window cleanup and count
        Deque<Long> timestamps = requestLog
            .computeIfAbsent(bucketKey, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Remove timestamps outside the window
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= rateLimit.requests()) {
                throw new RateLimitExceededException(
                    "Rate limit exceeded for: " + bucketKey +
                    " (max " + rateLimit.requests() + " per " + rateLimit.windowMs() + "ms)"
                );
            }

            timestamps.addLast(now);
        }

        return pjp.proceed(); // within the limit — call the real method
    }
}

// ─── Step 3: Apply the annotation ────────────────────────────────────
@RestController
public class PaymentController {

    @PostMapping("/api/payments")
    @RateLimit(key = "payment-api", requests = 5, windowMs = 10_000L) // 5 per 10s
    public ResponseEntity<Payment> processPayment(@RequestBody PaymentRequest req) {
        return ResponseEntity.ok(paymentService.process(req));
    }
}
```

---

### Common Production Patterns with Custom Annotations + AOP

```
┌────────────────────────────────────────────────────────────────────────────┐
│           Common Custom Annotation + AOP Patterns                          │
│                                                                            │
│  @AuditLog(action="X")                                                     │
│    → @Around logs method, args, result, duration, persists audit trail     │
│                                                                            │
│  @RateLimit(key="X", requests=N, windowMs=M)                               │
│    → @Around counts requests per window, throws 429 if exceeded            │
│                                                                            │
│  @Cacheable(key="X", ttlSeconds=N)  (custom, not Spring's built-in)        │
│    → @Around checks cache before proceeding, stores result after           │
│                                                                            │
│  @Retry(maxAttempts=3, backoffMs=1000)                                     │
│    → @Around catches exception, retries with backoff, gives up after N     │
│                                                                            │
│  @Timed(name="metric.name")                                                │
│    → @Around records method execution time to Micrometer / Prometheus      │
│                                                                            │
│  @RequiresPermission("ORDER_WRITE")                                        │
│    → @Around reads SecurityContext, throws AccessDeniedException           │
│                                                                            │
│  @DistributedLock(key="#{#orderId}")  (SpEL for dynamic key)               │
│    → @Around acquires Redis lock before method, releases in finally        │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

### Annotation Meta-Properties Summary Table

| Meta-annotation | Value options | Effect |
|----------------|---------------|--------|
| `@Target` | `ElementType.*` (METHOD, TYPE, FIELD, PARAMETER, …) | Restricts where the annotation can be placed |
| `@Retention` | `SOURCE`, `CLASS`, `RUNTIME` | Controls annotation lifetime; must be `RUNTIME` for Spring AOP |
| `@Documented` | (no value — presence/absence) | Includes annotation in Javadoc |
| `@Inherited` | (no value) | Subclasses inherit class-level annotation |
| `@Repeatable` | Container annotation class | Allows same annotation multiple times on one element |

### Attribute Types Allowed in Annotations

```java
public @interface Example {
    // All valid element types:
    boolean     flag()   default false;
    byte        b()      default 0;
    short       s()      default 0;
    int         count()  default 0;
    long        ms()     default 0L;
    float       ratio()  default 0.0f;
    double      rate()   default 0.0;
    char        ch()     default 'A';
    String      name()   default "";
    Class<?>    type()   default Object.class;
    MyEnum      scope()  default MyEnum.DEFAULT;
    OtherAnnot  nested() default @OtherAnnot;

    // Arrays of any of the above:
    String[]    keys()   default {};
    Class<?>[]  groups() default {};
    MyEnum[]    scopes() default {};
}
```

> Annotation elements cannot be `null`. Always provide a `default` for optional attributes, or callers must always supply them.
### 14. Authenticated Request Flow — SecurityContextHolderFilter, AuthorizationFilter & Two-Phase Authorization

---

#### 14.1 Overview — What Happens on Every Request AFTER the User is Already Logged In

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  OVERVIEW — EVERY REQUEST AFTER AUTHENTICATION (Session Cookie Present)                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  After the user has successfully authenticated (via form login, basic auth, etc.),          │
│  the browser stores a JSESSIONID cookie. On EVERY subsequent request, the following         │
│  happens — NO re-authentication! The session cookie proves identity.                        │
│                                                                                              │
│  KEY INSIGHT: Authentication happens ONCE (at login). On all subsequent requests,           │
│  the server LOADS the previously stored SecurityContext from the HttpSession.               │
│  It does NOT call DaoAuthenticationProvider or UserDetailsService again!                    │
│                                                                                              │
│                                                                                              │
│  ── FILTER CHAIN FOR AN AUTHENTICATED REQUEST ──────────────────────────────               │
│                                                                                              │
│  Browser sends: GET /api/orders                                                              │
│                  Cookie: JSESSIONID=ABC123                                                   │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │  FILTER 1: SecurityContextHolderFilter                                   │               │
│  │  → Load SecurityContext from HttpSession (using JSESSIONID)              │               │
│  │  → Place it in SecurityContextHolder (ThreadLocal)                       │               │
│  │  → The user is NOW "known" to the server for this request               │               │
│  └──────────────────────┬───────────────────────────────────────────────────┘               │
│                         │                                                                    │
│                         ▼                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │  FILTER 2: CsrfFilter                                                   │               │
│  │  → Validates CSRF token (for POST/PUT/DELETE requests)                  │               │
│  └──────────────────────┬───────────────────────────────────────────────────┘               │
│                         │                                                                    │
│                         ▼                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │  FILTER 3-N: Other Security Filters                                      │               │
│  │  → UsernamePasswordAuthenticationFilter: SKIPPED (not POST /login)      │               │
│  │  → LogoutFilter: SKIPPED (not POST /logout)                             │               │
│  │  → etc.                                                                   │               │
│  └──────────────────────┬───────────────────────────────────────────────────┘               │
│                         │                                                                    │
│                         ▼                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │  FILTER LAST: AuthorizationFilter  ← ★ KEY FILTER                       │               │
│  │  → Check: does the user have permission to access /api/orders?          │               │
│  │  → Reads roles/authorities from SecurityContextHolder                    │               │
│  │  → Compares with the rules you configured in SecurityConfig             │               │
│  │  → GRANTED → continue to DispatcherServlet                              │               │
│  │  → DENIED → throw AccessDeniedException (403 Forbidden)                │               │
│  └──────────────────────┬───────────────────────────────────────────────────┘               │
│                         │                                                                    │
│                         ▼                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │  DispatcherServlet → HandlerInterceptors → Controller                    │               │
│  │  → Method-level security (@PreAuthorize, @Secured) checked here         │               │
│  │  → Controller processes the request                                      │               │
│  │  → Returns response                                                      │               │
│  └──────────────────────────────────────────────────────────────────────────┘               │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 14.2 SecurityContextHolderFilter — Step-by-Step Session Lookup

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SecurityContextHolderFilter — DETAILED INTERNAL FLOW                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  SecurityContextHolderFilter is the FIRST Spring Security filter to execute.                │
│  Its job: load the SecurityContext from the session and put it in ThreadLocal.               │
│                                                                                              │
│  REQUEST: GET /api/orders                                                                    │
│  Cookie: JSESSIONID=ABC123-DEF456-GHI789                                                    │
│                                                                                              │
│                                                                                              │
│  ── STEP 1: SecurityContextHolderFilter.doFilter() ─────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │                                                                          │               │
│  │  // SecurityContextHolderFilter delegates to SecurityContextRepository  │               │
│  │  // to load the SecurityContext. In Spring Security 6.x, loading is    │               │
│  │  // DEFERRED (lazy) — the actual session lookup only happens when      │               │
│  │  // SecurityContextHolder.getContext() is first called.                │               │
│  │                                                                          │               │
│  │  Supplier<SecurityContext> deferredContext =                             │               │
│  │      this.securityContextRepository.loadDeferredContext(request);        │               │
│  │                                                                          │               │
│  │  SecurityContextHolder.setDeferredContext(deferredContext);              │               │
│  │  // ★ No DB/session hit yet! Just a lazy Supplier stored in ThreadLocal│               │
│  │                                                                          │               │
│  │  try {                                                                   │               │
│  │      filterChain.doFilter(request, response); // continue filter chain │               │
│  │  } finally {                                                             │               │
│  │      SecurityContextHolder.clearContext(); // ★ ALWAYS clear after!    │               │
│  │  }                                                                       │               │
│  │                                                                          │               │
│  └──────────────────────────────────────────────────────────────────────────┘               │
│                                                                                              │
│                                                                                              │
│  ── STEP 2: Lazy Resolution — When getContext() is First Called ─────────                   │
│                                                                                              │
│  Later in the filter chain, when AuthorizationFilter (or any code) calls:                  │
│  SecurityContextHolder.getContext()                                                          │
│                                                                                              │
│  The deferred Supplier is invoked, which triggers:                                          │
│  HttpSessionSecurityContextRepository.loadContext(request)                                   │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │                                                                          │               │
│  │  // INSIDE HttpSessionSecurityContextRepository.loadContext():           │               │
│  │                                                                          │               │
│  │  // 2a. Get the existing HttpSession (do NOT create a new one)          │               │
│  │  HttpSession session = request.getSession(false);                       │               │
│  │  //                                       ↑                              │               │
│  │  //                    false = return null if no session exists          │               │
│  │  //                    Tomcat looks up ABC123-DEF456-GHI789 in its      │               │
│  │  //                    internal ConcurrentHashMap of sessions            │               │
│  │                                                                          │               │
│  │  // ── CASE A: Session FOUND (valid JSESSIONID) ──────────────────     │               │
│  │                                                                          │               │
│  │  if (session != null) {                                                  │               │
│  │      Object contextObj = session.getAttribute("SPRING_SECURITY_CONTEXT");│               │
│  │      //                                                                  │               │
│  │      // Tomcat returns the SecurityContext object that was stored         │               │
│  │      // during login (see Section 13.5):                                 │               │
│  │      // SecurityContext {                                                 │               │
│  │      //   authentication: UsernamePasswordAuthenticationToken {          │               │
│  │      //     principal: UserDetails { username: "john" },                 │               │
│  │      //     credentials: null,                                           │               │
│  │      //     authorities: [ROLE_USER],                                    │               │
│  │      //     authenticated: true                                          │               │
│  │      //   }                                                              │               │
│  │      // }                                                                │               │
│  │                                                                          │               │
│  │      if (contextObj instanceof SecurityContext ctx) {                    │               │
│  │          return ctx;  // ★ Return the SecurityContext to the filter    │               │
│  │      }                                                                   │               │
│  │  }                                                                       │               │
│  │                                                                          │               │
│  │  // ── CASE B: Session NOT found (invalid/expired JSESSIONID) ────     │               │
│  │                                                                          │               │
│  │  // session == null, OR getAttribute returned null                      │               │
│  │  return SecurityContextHolder.createEmptyContext();                      │               │
│  │  // Returns: SecurityContext { authentication: null }                   │               │
│  │  // ★ The user will be treated as UNAUTHENTICATED (anonymous)          │               │
│  │                                                                          │               │
│  └──────────────────────────────────────────────────────────────────────────┘               │
│                                                                                              │
│                                                                                              │
│  ── STEP 3: SecurityContext Now Available in ThreadLocal ────────────────                   │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │                                                                          │               │
│  │  CASE A (Valid Session):                                                 │               │
│  │  SecurityContextHolder (ThreadLocal) = SecurityContext {                 │               │
│  │    authentication: UsernamePasswordAuthenticationToken {                 │               │
│  │      principal: UserDetails { username: "john", password: null },        │               │
│  │      credentials: null,                                                  │               │
│  │      authorities: [SimpleGrantedAuthority("ROLE_USER")],                 │               │
│  │      authenticated: true                                                 │               │
│  │    }                                                                     │               │
│  │  }                                                                       │               │
│  │  → ★ Available for the ENTIRE request lifecycle                         │               │
│  │  → ★ Any filter, interceptor, controller, or service can read it       │               │
│  │  → ★ Cleared in the finally{} block after response is sent             │               │
│  │                                                                          │               │
│  │                                                                          │               │
│  │  CASE B (Invalid/Expired Session):                                      │               │
│  │  SecurityContextHolder (ThreadLocal) = SecurityContext {                 │               │
│  │    authentication: null   ← EMPTY! No user identity.                    │               │
│  │  }                                                                       │               │
│  │  → AnonymousAuthenticationFilter may later set an AnonymousAuthToken    │               │
│  │  → AuthorizationFilter will DENY access to protected resources         │               │
│  │  → ExceptionTranslationFilter will redirect to /login                  │               │
│  │                                                                          │               │
│  └──────────────────────────────────────────────────────────────────────────┘               │
│                                                                                              │
│                                                                                              │
│  ── VISUAL: Valid vs Invalid Session ────────────────────────────────────                   │
│                                                                                              │
│  ┌─── VALID JSESSIONID ─────────────────────────────────────────────┐                      │
│  │                                                                   │                      │
│  │  Cookie: JSESSIONID=ABC123                                        │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  Tomcat SessionManager.findSession("ABC123")                     │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  HttpSession found! (not expired, maxInactiveInterval not exceeded)│                     │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  session.getAttribute("SPRING_SECURITY_CONTEXT")                  │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  SecurityContext { john, ROLE_USER, authenticated=true }          │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  SecurityContextHolder.setContext(ctx)                            │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  Continue to AuthorizationFilter ─── ✅ User is authenticated    │                      │
│  │                                                                   │                      │
│  └───────────────────────────────────────────────────────────────────┘                      │
│                                                                                              │
│  ┌─── INVALID / EXPIRED JSESSIONID ─────────────────────────────────┐                      │
│  │                                                                   │                      │
│  │  Cookie: JSESSIONID=EXPIRED999                                    │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  Tomcat SessionManager.findSession("EXPIRED999")                 │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  HttpSession NOT found! (expired or never existed)               │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  request.getSession(false) returns null                          │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  SecurityContext { authentication: null } ← EMPTY                │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  SecurityContextHolder.setContext(emptyCtx)                      │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  AuthorizationFilter ─── ❌ DENIED ─── AccessDeniedException     │                      │
│  │       │                                                           │                      │
│  │       ▼                                                           │                      │
│  │  ExceptionTranslationFilter ─── 302 Redirect to /login          │                      │
│  │                                                                   │                      │
│  └───────────────────────────────────────────────────────────────────┘                      │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 14.3 AuthorizationFilter — How Spring Security Checks Access Permissions

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  AuthorizationFilter — COMPLETE INTERNAL FLOW                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  AuthorizationFilter is the LAST filter in the Spring Security filter chain.                │
│  It runs AFTER the SecurityContext has been loaded (by SecurityContextHolderFilter).         │
│                                                                                              │
│  Its job: check if the CURRENT user has PERMISSION to access the REQUESTED resource.        │
│                                                                                              │
│                                                                                              │
│  ── INTERNAL FLOW ───────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │  AuthorizationFilter.doFilter(request, response, chain):                │               │
│  │                                                                          │               │
│  │  // Step 1: Get the current user's Authentication from ThreadLocal      │               │
│  │  Supplier<Authentication> authentication =                              │               │
│  │      SecurityContextHolder.getContext()::getAuthentication;             │               │
│  │                                                                          │               │
│  │  // Step 2: Delegate to AuthorizationManager to make the decision       │               │
│  │  AuthorizationDecision decision = this.authorizationManager.check(      │               │
│  │      authentication,    // Supplier<Authentication> → { john, ROLE_USER }│               │
│  │      request            // HttpServletRequest → GET /api/orders         │               │
│  │  );                                                                      │               │
│  │                                                                          │               │
│  │  // Step 3: Act on the decision                                         │               │
│  │  if (decision != null && !decision.isGranted()) {                       │               │
│  │      throw new AccessDeniedException("Access Denied");                  │               │
│  │      // → Caught by ExceptionTranslationFilter (the filter BEFORE this)│               │
│  │      // → If user is anonymous → redirect to /login (401-like)         │               │
│  │      // → If user is authenticated but lacks role → 403 Forbidden      │               │
│  │  }                                                                       │               │
│  │                                                                          │               │
│  │  // Step 4: Access GRANTED → continue to DispatcherServlet              │               │
│  │  chain.doFilter(request, response);                                     │               │
│  │  // → DispatcherServlet → HandlerMapping → Interceptors → Controller   │               │
│  │                                                                          │               │
│  └──────────────────────────────────────────────────────────────────────────┘               │
│                                                                                              │
│                                                                                              │
│  ── AuthorizationManager — WHO MAKES THE DECISION? ─────────────────────                   │
│                                                                                              │
│  The AuthorizationManager implementation depends on your SecurityConfig:                    │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │  AuthorizationManager (interface)                                        │               │
│  │     │                                                                    │               │
│  │     ├── RequestMatcherDelegatingAuthorizationManager                    │               │
│  │     │      (used by .authorizeHttpRequests() — DEFAULT)                 │               │
│  │     │      Routes to different managers based on URL pattern            │               │
│  │     │      e.g., /admin/** → AuthorityAuthorizationManager("ROLE_ADMIN")│               │
│  │     │           /api/**   → AuthenticatedAuthorizationManager           │               │
│  │     │           /public/** → permit all                                  │               │
│  │     │                                                                    │               │
│  │     ├── AuthorityAuthorizationManager                                   │               │
│  │     │      Checks if user has specific roles/authorities                │               │
│  │     │      Used by: hasRole("ADMIN"), hasAnyRole("USER","ADMIN")       │               │
│  │     │                                                                    │               │
│  │     ├── AuthenticatedAuthorizationManager                               │               │
│  │     │      Checks if user is authenticated (any role)                   │               │
│  │     │      Used by: .authenticated()                                    │               │
│  │     │                                                                    │               │
│  │     └── AuthorizationManagers (composite)                               │               │
│  │            Combines multiple managers with AND/OR logic                  │               │
│  │                                                                          │               │
│  └──────────────────────────────────────────────────────────────────────────┘               │
│                                                                                              │
│                                                                                              │
│  ── DECISION FLOW DIAGRAM ───────────────────────────────────────────────                  │
│                                                                                              │
│  Request: GET /admin/users                                                                  │
│  User: john (authorities: [ROLE_USER])                                                      │
│                                                                                              │
│  AuthorizationFilter                                                                        │
│       │                                                                                      │
│       ▼                                                                                      │
│  RequestMatcherDelegatingAuthorizationManager                                               │
│       │                                                                                      │
│       │  Match request URL against configured patterns:                                     │
│       │  ┌────────────────────────────────────────────────────────┐                         │
│       │  │  Pattern           │  Required Authority               │                         │
│       │  ├────────────────────┼───────────────────────────────────┤                         │
│       │  │  /public/**        │  permitAll()                      │                         │
│       │  │  /api/**           │  authenticated()                  │                         │
│       │  │  /admin/**         │  hasRole("ADMIN") ← MATCH!       │                         │
│       │  │  /**               │  authenticated()                  │                         │
│       │  └────────────────────┴───────────────────────────────────┘                         │
│       │                                                                                      │
│       │  URL /admin/users matches /admin/** → delegates to                                 │
│       │  AuthorityAuthorizationManager("ROLE_ADMIN")                                       │
│       │                                                                                      │
│       ▼                                                                                      │
│  AuthorityAuthorizationManager.check(authentication, request)                               │
│       │                                                                                      │
│       │  authentication.getAuthorities() → [ROLE_USER]                                     │
│       │  Required: ROLE_ADMIN                                                               │
│       │                                                                                      │
│       │  Does [ROLE_USER] contain ROLE_ADMIN?  → ❌ NO                                      │
│       │                                                                                      │
│       ▼                                                                                      │
│  AuthorizationDecision { granted: false }                                                   │
│       │                                                                                      │
│       ▼                                                                                      │
│  AuthorizationFilter: throw new AccessDeniedException("Access Denied")                      │
│       │                                                                                      │
│       ▼                                                                                      │
│  ExceptionTranslationFilter catches it:                                                     │
│  → User IS authenticated (john) but lacks required role                                    │
│  → Returns: 403 Forbidden                                                                   │
│  → (If user was NOT authenticated → redirect to /login instead)                            │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 14.4 ★ Default Behavior — Spring Security Does NOT Restrict Resources By Default

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ CRITICAL — DEFAULT AUTHORIZATION BEHAVIOR                                                 │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  By default, Spring Boot auto-configures a SecurityFilterChain that:                        │
│  1. Requires authentication for ALL endpoints (.anyRequest().authenticated())               │
│  2. Enables form login with defaults                                                        │
│  3. Enables HTTP Basic with defaults                                                        │
│                                                                                              │
│  BUT it does NOT apply any ROLE-BASED restrictions!                                         │
│  Every authenticated user can access EVERY endpoint regardless of their role.               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  DEFAULT SecurityFilterChain (from SpringBootWebSecurityConfiguration):     │           │
│  │                                                                               │           │
│  │  @Bean                                                                        │           │
│  │  SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {          │           │
│  │      http                                                                     │           │
│  │          .authorizeHttpRequests(auth -> auth                                  │           │
│  │              .anyRequest().authenticated()                                    │           │
│  │              // ↑ ANY authenticated user can access ANY endpoint!            │           │
│  │              // ↑ No role checking! ROLE_USER, ROLE_ADMIN — all the same    │           │
│  │          )                                                                    │           │
│  │          .formLogin(Customizer.withDefaults())                                │           │
│  │          .httpBasic(Customizer.withDefaults());                                │           │
│  │      return http.build();                                                     │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ This default bean is ONLY created if YOU don't define your own            │           │
│  │    SecurityFilterChain bean. As soon as you define one, Spring Boot          │           │
│  │    backs off and uses YOUR configuration instead.                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── YOU MUST CONFIGURE AUTHORIZATION RULES MANUALLY ─────────────────────────               │
│                                                                                              │
│  To restrict access based on roles, you MUST define your own SecurityFilterChain:           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  @Configuration                                                               │           │
│  │  @EnableWebSecurity                                                           │           │
│  │  public class SecurityConfig {                                                │           │
│  │                                                                               │           │
│  │      @Bean                                                                    │           │
│  │      SecurityFilterChain filterChain(HttpSecurity http) throws Exception {   │           │
│  │          http                                                                 │           │
│  │              .authorizeHttpRequests(auth -> auth                              │           │
│  │                  // Public endpoints — no login required                     │           │
│  │                  .requestMatchers("/", "/login", "/register", "/css/**")      │           │
│  │                      .permitAll()                                             │           │
│  │                                                                               │           │
│  │                  // Admin-only endpoints                                     │           │
│  │                  .requestMatchers("/admin/**")                                │           │
│  │                      .hasRole("ADMIN")                                        │           │
│  │                                                                               │           │
│  │                  // User OR Admin can access                                 │           │
│  │                  .requestMatchers("/api/**")                                  │           │
│  │                      .hasAnyRole("USER", "ADMIN")                             │           │
│  │                                                                               │           │
│  │                  // Everything else requires authentication                  │           │
│  │                  .anyRequest().authenticated()                                │           │
│  │              )                                                                │           │
│  │              .formLogin(Customizer.withDefaults());                            │           │
│  │          return http.build();                                                 │           │
│  │      }                                                                        │           │
│  │  }                                                                            │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ★ ORDER MATTERS! Rules are evaluated TOP-TO-BOTTOM, first match wins.                     │
│  Always put more specific patterns (/admin/**) BEFORE less specific (/**)                  │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 14.5 ★ Two-Phase Authorization — Filter-Level vs Controller-Level

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ TWO-PHASE AUTHORIZATION — THE TWO LAYERS WHERE ACCESS IS CHECKED                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Spring Security provides TWO layers where authorization decisions are made.                │
│  Both layers work INDEPENDENTLY and can be used together for defense-in-depth.              │
│                                                                                              │
│                                                                                              │
│  Request → [Security Filters] → [DispatcherServlet] → [Controller] → Response             │
│                │                                           │                                │
│                │                                           │                                │
│         PHASE 1: URL-Level                          PHASE 2: Method-Level                   │
│         (AuthorizationFilter)                       (@PreAuthorize, @Secured)               │
│         Checks BEFORE reaching                     Checks INSIDE the controller             │
│         the controller                              / service layer                          │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PHASE 1: AUTHORIZATION AT SECURITY FILTER LEVEL (AuthorizationFilter)                     │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  WHERE: Inside the Spring Security filter chain, BEFORE DispatcherServlet                  │
│  WHEN:  On every HTTP request, before it reaches any controller                            │
│  HOW:   Configured in SecurityConfig using .authorizeHttpRequests()                        │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  WHAT IT CAN CHECK:                                                          │           │
│  │                                                                               │           │
│  │  • URL patterns:                                                             │           │
│  │    .requestMatchers("/admin/**").hasRole("ADMIN")                            │           │
│  │    .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")                   │           │
│  │    .requestMatchers("/public/**").permitAll()                                │           │
│  │                                                                               │           │
│  │  • HTTP methods:                                                             │           │
│  │    .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")           │           │
│  │    .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")   │           │
│  │                                                                               │           │
│  │  • IP address (custom RequestMatcher):                                       │           │
│  │    .requestMatchers(request -> "127.0.0.1".equals(                           │           │
│  │        request.getRemoteAddr())).permitAll()                                 │           │
│  │                                                                               │           │
│  │  • Authentication state:                                                     │           │
│  │    .anyRequest().authenticated()      // any logged-in user                  │           │
│  │    .anyRequest().denyAll()            // block everything                    │           │
│  │    .anyRequest().permitAll()          // allow everything                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  AVAILABLE METHODS for authorization rules:                                  │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────┬────────────────────────────────────────┐  │           │
│  │  │  Method                      │  What it checks                        │  │           │
│  │  ├──────────────────────────────┼────────────────────────────────────────┤  │           │
│  │  │  .permitAll()                │  Allow everyone (no auth needed)       │  │           │
│  │  │  .denyAll()                  │  Block everyone                        │  │           │
│  │  │  .authenticated()            │  Must be logged in (any role)          │  │           │
│  │  │  .hasRole("ADMIN")           │  Must have ROLE_ADMIN authority       │  │           │
│  │  │                              │  (auto-prepends "ROLE_" prefix)       │  │           │
│  │  │  .hasAnyRole("USER","ADMIN") │  Must have ROLE_USER OR ROLE_ADMIN   │  │           │
│  │  │  .hasAuthority("ROLE_ADMIN") │  Must have exact authority string     │  │           │
│  │  │                              │  (no prefix added)                    │  │           │
│  │  │  .hasAnyAuthority(...)       │  Must have any of the listed auths   │  │           │
│  │  │  .access(manager)            │  Custom AuthorizationManager logic    │  │           │
│  │  └──────────────────────────────┴────────────────────────────────────────┘  │           │
│  │                                                                               │           │
│  │  ⚠️ hasRole("ADMIN") vs hasAuthority("ROLE_ADMIN"):                         │           │
│  │  • hasRole("ADMIN") automatically prepends "ROLE_" → checks for ROLE_ADMIN  │           │
│  │  • hasAuthority("ROLE_ADMIN") checks the EXACT string → ROLE_ADMIN          │           │
│  │  • Both are equivalent! hasRole() is just a convenience shortcut.           │           │
│  │  • If your authorities DON'T use the ROLE_ prefix, use hasAuthority()       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PHASE 2: AUTHORIZATION AT CONTROLLER / METHOD LEVEL                                       │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  WHERE: Inside the controller or service layer (after passing through filters)             │
│  WHEN:  When the specific method is invoked                                                 │
│  HOW:   Using annotations: @PreAuthorize, @PostAuthorize, @Secured, @RolesAllowed         │
│                                                                                              │
│  ★ This phase is COMMON for ALL authentication types:                                      │
│    form-based login, HTTP Basic, JWT, OAuth2 — they all work the same way                  │
│    at the method level because by this point, the SecurityContext is already               │
│    populated with the authenticated user's details.                                         │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  STEP 1: Enable method-level security (REQUIRED — not enabled by default!) │           │
│  │                                                                               │           │
│  │  @Configuration                                                               │           │
│  │  @EnableWebSecurity                                                           │           │
│  │  @EnableMethodSecurity    // ← THIS annotation enables @PreAuthorize etc.    │           │
│  │  public class SecurityConfig {                                                │           │
│  │      // ...                                                                   │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  // ⚠️ DEPRECATION NOTE:                                                    │           │
│  │  // @EnableGlobalMethodSecurity → DEPRECATED in Spring Security 6.x         │           │
│  │  // @EnableMethodSecurity       → CURRENT replacement                        │           │
│  │  //                                                                           │           │
│  │  // Differences:                                                              │           │
│  │  // Old: @EnableGlobalMethodSecurity(prePostEnabled = true,                  │           │
│  │  //                                   securedEnabled = true)                  │           │
│  │  // New: @EnableMethodSecurity  ← prePostEnabled=true by default!           │           │
│  │  //      @EnableMethodSecurity(securedEnabled = true) ← if you need @Secured│           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  STEP 2: Apply annotations on Controller or Service methods                  │           │
│  │                                                                               │           │
│  │  // ── @PreAuthorize (most powerful — supports SpEL expressions) ──────      │           │
│  │                                                                               │           │
│  │  @RestController                                                              │           │
│  │  @RequestMapping("/api")                                                      │           │
│  │  public class OrderController {                                               │           │
│  │                                                                               │           │
│  │      // Only users with ROLE_USER or ROLE_ADMIN can access                   │           │
│  │      @PreAuthorize("hasAnyRole('USER', 'ADMIN')")                            │           │
│  │      @GetMapping("/orders")                                                   │           │
│  │      public List<Order> getOrders() { ... }                                  │           │
│  │                                                                               │           │
│  │      // Only ADMIN can delete                                                │           │
│  │      @PreAuthorize("hasRole('ADMIN')")                                        │           │
│  │      @DeleteMapping("/orders/{id}")                                           │           │
│  │      public void deleteOrder(@PathVariable Long id) { ... }                  │           │
│  │                                                                               │           │
│  │      // User can only access their OWN orders                                │           │
│  │      // (uses SpEL to compare principal username with path variable)         │           │
│  │      @PreAuthorize("#username == authentication.name")                        │           │
│  │      @GetMapping("/orders/user/{username}")                                   │           │
│  │      public List<Order> getUserOrders(@PathVariable String username) { ... } │           │
│  │                                                                               │           │
│  │      // Complex SpEL expression                                              │           │
│  │      @PreAuthorize("hasRole('ADMIN') or #order.createdBy == authentication.name")│       │
│  │      @PutMapping("/orders/{id}")                                              │           │
│  │      public Order updateOrder(@PathVariable Long id,                          │           │
│  │                                @RequestBody Order order) { ... }              │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  // ── @PostAuthorize (checks AFTER method executes) ─────────────────      │           │
│  │                                                                               │           │
│  │  @PostAuthorize("returnObject.createdBy == authentication.name "              │           │
│  │               + "or hasRole('ADMIN')")                                        │           │
│  │  @GetMapping("/orders/{id}")                                                  │           │
│  │  public Order getOrder(@PathVariable Long id) {                              │           │
│  │      return orderService.findById(id);                                        │           │
│  │      // Method executes first, THEN the return value is checked              │           │
│  │      // If the condition is false → 403 Forbidden                            │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  // ── @Secured (simpler, no SpEL — role names only) ────────────────       │           │
│  │  // Requires: @EnableMethodSecurity(securedEnabled = true)                   │           │
│  │                                                                               │           │
│  │  @Secured("ROLE_ADMIN")                                                       │           │
│  │  @DeleteMapping("/users/{id}")                                                │           │
│  │  public void deleteUser(@PathVariable Long id) { ... }                       │           │
│  │                                                                               │           │
│  │  @Secured({"ROLE_USER", "ROLE_ADMIN"})                                        │           │
│  │  @GetMapping("/profile")                                                      │           │
│  │  public UserProfile getProfile() { ... }                                     │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  // ── Service layer security (same annotations work here!) ──────────      │           │
│  │                                                                               │           │
│  │  @Service                                                                     │           │
│  │  public class OrderService {                                                  │           │
│  │                                                                               │           │
│  │      @PreAuthorize("hasRole('ADMIN')")                                        │           │
│  │      public void cancelAllOrders() { ... }  // Only admin can do this        │           │
│  │                                                                               │           │
│  │      @PreAuthorize("hasRole('USER')")                                         │           │
│  │      public Order placeOrder(Order order) { ... }                            │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── HOW METHOD-LEVEL SECURITY WORKS INTERNALLY (AOP Proxy) ─────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  @EnableMethodSecurity registers an AOP interceptor:                         │           │
│  │  AuthorizationManagerBeforeMethodInterceptor                                 │           │
│  │                                                                               │           │
│  │  When you call orderController.deleteOrder(1):                               │           │
│  │                                                                               │           │
│  │  1. Spring's AOP proxy intercepts the method call                            │           │
│  │  2. AuthorizationManagerBeforeMethodInterceptor reads @PreAuthorize          │           │
│  │  3. Evaluates the SpEL expression: hasRole('ADMIN')                          │           │
│  │  4. Gets Authentication from SecurityContextHolder                           │           │
│  │  5. Checks: does user have ROLE_ADMIN?                                       │           │
│  │     → YES → invoke the actual method                                        │           │
│  │     → NO  → throw AccessDeniedException (403)                               │           │
│  │                                                                               │           │
│  │  DispatcherServlet                                                            │           │
│  │       │                                                                       │           │
│  │       ▼                                                                       │           │
│  │  OrderController$$SpringCGLIB$$Proxy (AOP proxy)                             │           │
│  │       │                                                                       │           │
│  │       ├── AuthorizationManagerBeforeMethodInterceptor                        │           │
│  │       │      Reads: @PreAuthorize("hasRole('ADMIN')")                        │           │
│  │       │      Auth: { john, [ROLE_USER] }                                     │           │
│  │       │      Has ROLE_ADMIN? → NO → AccessDeniedException                   │           │
│  │       │                                                                       │           │
│  │       └── (actual method never executes if denied)                           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 14.6 Two-Phase Authorization — Visual Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  TWO-PHASE AUTHORIZATION — COMPLETE VISUAL FLOW                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Request: DELETE /api/orders/42                                                             │
│  User: john (authorities: [ROLE_USER])                                                      │
│                                                                                              │
│                                                                                              │
│  ┌── PHASE 1: SecurityFilter Level (AuthorizationFilter) ───────────────────┐              │
│  │                                                                           │              │
│  │  SecurityConfig rules:                                                    │              │
│  │  .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")                 │              │
│  │                                                                           │              │
│  │  Check: Does john have ROLE_USER or ROLE_ADMIN?                          │              │
│  │  john has: [ROLE_USER]                                                    │              │
│  │  ROLE_USER is in the list → ✅ PHASE 1 PASSED                           │              │
│  │                                                                           │              │
│  │  → Request continues to DispatcherServlet                                │              │
│  │                                                                           │              │
│  └───────────────────────────────────┬───────────────────────────────────────┘              │
│                                      │                                                      │
│                                      ▼                                                      │
│  ┌── PHASE 2: Method Level (@PreAuthorize in Controller) ───────────────────┐              │
│  │                                                                           │              │
│  │  Controller method:                                                       │              │
│  │  @PreAuthorize("hasRole('ADMIN')")                                        │              │
│  │  @DeleteMapping("/orders/{id}")                                           │              │
│  │  public void deleteOrder(@PathVariable Long id) { ... }                  │              │
│  │                                                                           │              │
│  │  Check: Does john have ROLE_ADMIN?                                       │              │
│  │  john has: [ROLE_USER]                                                    │              │
│  │  ROLE_ADMIN is NOT in the list → ❌ PHASE 2 DENIED                      │              │
│  │                                                                           │              │
│  │  → AccessDeniedException → 403 Forbidden                                │              │
│  │  → Method deleteOrder() is NEVER invoked!                                │              │
│  │                                                                           │              │
│  └───────────────────────────────────────────────────────────────────────────┘              │
│                                                                                              │
│                                                                                              │
│  ── WHY TWO PHASES? ─────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  Phase 1 (URL-Level):                                                        │           │
│  │  • Coarse-grained: "can this user ACCESS this URL at all?"                  │           │
│  │  • Configured centrally in SecurityConfig                                    │           │
│  │  • Good for: broad resource grouping (/admin/**, /api/**, /public/**)       │           │
│  │  • Runs BEFORE DispatcherServlet — rejected requests never reach controllers│           │
│  │  • Saves resources: no controller instantiation, no DB queries              │           │
│  │                                                                               │           │
│  │  Phase 2 (Method-Level):                                                      │           │
│  │  • Fine-grained: "can this user PERFORM this specific OPERATION?"            │           │
│  │  • Configured per method using annotations                                   │           │
│  │  • Good for: same URL but different operations                               │           │
│  │    (GET /orders → any user, DELETE /orders/{id} → admin only)               │           │
│  │  • Can access method parameters (SpEL):                                      │           │
│  │    @PreAuthorize("#userId == authentication.principal.id")                    │           │
│  │  • Works identically for form login, basic auth, JWT, OAuth2               │           │
│  │                                                                               │           │
│  │  BEST PRACTICE: Use BOTH phases together for defense-in-depth!              │           │
│  │  Phase 1 = gatekeeping at the door                                           │           │
│  │  Phase 2 = checking authorization at the action level                       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── COMPARISON TABLE ────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────┬──────────────────────────┬──────────────────────────┐        │
│  │  Aspect                  │  Phase 1 (Filter-Level)  │  Phase 2 (Method-Level)  │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  WHERE                   │  AuthorizationFilter     │  AOP Proxy Interceptor   │        │
│  │                          │  (Security Filter Chain) │  (Spring AOP)            │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  WHEN                    │  Before DispatcherServlet│  After DispatcherServlet │        │
│  │                          │  (very early)            │  (at method invocation)  │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  CONFIGURED IN           │  SecurityConfig class    │  @PreAuthorize, @Secured │        │
│  │                          │  .authorizeHttpRequests() │  on methods              │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  GRANULARITY             │  URL patterns + HTTP     │  Per method, with access │        │
│  │                          │  methods                 │  to method arguments     │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  SpEL SUPPORT            │  Limited (via .access()) │  Full SpEL support       │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  AUTH TYPE SPECIFIC      │  Can be (e.g., form only)│  COMMON to ALL auth types│        │
│  │                          │                          │  (form, basic, JWT, etc.)│        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  DENIED RESPONSE         │  403 Forbidden or        │  403 Forbidden           │        │
│  │                          │  redirect to /login      │                          │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  ENABLED BY DEFAULT      │  ✅ Yes (always active)  │  ❌ No (needs             │        │
│  │                          │                          │  @EnableMethodSecurity)  │        │
│  └──────────────────────────┴──────────────────────────┴──────────────────────────┘        │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 14.7 AuthorizationFilter in Action — Role Matching Internals

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  AuthorizationFilter — ROLE MATCHING INTERNALS                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  When AuthorizationFilter is invoked, here is EXACTLY what happens internally:              │
│                                                                                              │
│                                                                                              │
│  ── SCENARIO: User john (ROLE_USER) tries to access /admin/dashboard ────                  │
│                                                                                              │
│  SecurityConfig:                                                                             │
│  .requestMatchers("/admin/**").hasRole("ADMIN")                                             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. AuthorizationFilter gets the Authentication from SecurityContextHolder   │           │
│  │                                                                               │           │
│  │     Authentication auth = SecurityContextHolder.getContext()                  │           │
│  │                                .getAuthentication();                          │           │
│  │     // auth = UsernamePasswordAuthenticationToken {                           │           │
│  │     //   principal: UserDetails { username: "john" },                         │           │
│  │     //   authorities: [SimpleGrantedAuthority("ROLE_USER")],                  │           │
│  │     //   authenticated: true                                                  │           │
│  │     // }                                                                      │           │
│  │                                                                               │           │
│  │  2. RequestMatcherDelegatingAuthorizationManager matches the URL             │           │
│  │                                                                               │           │
│  │     Request URL: /admin/dashboard                                             │           │
│  │     Pattern: /admin/** → MATCH!                                              │           │
│  │     Delegates to: AuthorityAuthorizationManager("ROLE_ADMIN")                │           │
│  │                                                                               │           │
│  │  3. AuthorityAuthorizationManager checks authorities                         │           │
│  │                                                                               │           │
│  │     Required authorities: ["ROLE_ADMIN"]                                     │           │
│  │     User's authorities:   ["ROLE_USER"]                                      │           │
│  │                                                                               │           │
│  │     // Internal check (simplified):                                          │           │
│  │     for (GrantedAuthority userAuth : auth.getAuthorities()) {                │           │
│  │         if ("ROLE_ADMIN".equals(userAuth.getAuthority())) {                  │           │
│  │             return new AuthorizationDecision(true);  // GRANTED             │           │
│  │         }                                                                     │           │
│  │     }                                                                         │           │
│  │     return new AuthorizationDecision(false);  // DENIED                     │           │
│  │                                                                               │           │
│  │     Result: "ROLE_USER" != "ROLE_ADMIN" → DENIED                             │           │
│  │                                                                               │           │
│  │  4. AuthorizationFilter throws AccessDeniedException                         │           │
│  │                                                                               │           │
│  │     throw new AccessDeniedException("Access Denied");                        │           │
│  │                                                                               │           │
│  │  5. ExceptionTranslationFilter catches the exception                         │           │
│  │                                                                               │           │
│  │     // Is the user authenticated?                                            │           │
│  │     if (auth != null && auth.isAuthenticated()                               │           │
│  │             && !(auth instanceof AnonymousAuthenticationToken)) {            │           │
│  │         // YES → User is logged in but lacks permission                     │           │
│  │         // → Send 403 Forbidden                                              │           │
│  │         accessDeniedHandler.handle(request, response, exception);            │           │
│  │         // Default: returns HTTP 403 with error page                        │           │
│  │     } else {                                                                  │           │
│  │         // NO → User is anonymous (not logged in)                           │           │
│  │         // → Redirect to login page                                          │           │
│  │         authenticationEntryPoint.commence(request, response, exception);     │           │
│  │         // Default: 302 redirect to /login                                   │           │
│  │     }                                                                         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── VISUAL: 403 vs REDIRECT TO LOGIN ────────────────────────────────────                  │
│                                                                                              │
│  ┌─── Authenticated user LACKS required role ────────────────────────────┐                 │
│  │                                                                        │                 │
│  │  User: john (ROLE_USER)                                                │                 │
│  │  Endpoint: /admin/dashboard (requires ROLE_ADMIN)                      │                 │
│  │                                                                        │                 │
│  │  AuthorizationFilter → DENIED                                         │                 │
│  │  ExceptionTranslationFilter:                                           │                 │
│  │  → john IS authenticated → 403 Forbidden                             │                 │
│  │                                                                        │                 │
│  │  Response: HTTP/1.1 403 Forbidden                                      │                 │
│  │            (error page or JSON error)                                  │                 │
│  │                                                                        │                 │
│  └────────────────────────────────────────────────────────────────────────┘                 │
│                                                                                              │
│  ┌─── Anonymous user (NOT authenticated) ────────────────────────────────┐                 │
│  │                                                                        │                 │
│  │  User: (none — no JSESSIONID or expired)                               │                 │
│  │  Endpoint: /api/orders (requires authenticated())                      │                 │
│  │                                                                        │                 │
│  │  AuthorizationFilter → DENIED (no authentication)                     │                 │
│  │  ExceptionTranslationFilter:                                           │                 │
│  │  → User is NOT authenticated → save request to RequestCache           │                 │
│  │  → Redirect to /login (302)                                           │                 │
│  │  → After login, redirect back to /api/orders                          │                 │
│  │                                                                        │                 │
│  │  Response: HTTP/1.1 302 Found                                          │                 │
│  │            Location: /login                                            │                 │
│  │                                                                        │                 │
│  └────────────────────────────────────────────────────────────────────────┘                 │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 14.8 Complete Code Example — SecurityConfig with Both Authorization Phases

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig — PHASE 1 (URL-Level) + PHASE 2 (Method-Level) combined
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ★ Enables @PreAuthorize, @PostAuthorize (Phase 2)
                       // replaces deprecated @EnableGlobalMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // ═══════════════════════════════════════════════════════════════════
        //  PHASE 1: URL-LEVEL AUTHORIZATION (AuthorizationFilter)
        //  These rules are checked BEFORE the request reaches any controller.
        // ═══════════════════════════════════════════════════════════════════

        http
            .authorizeHttpRequests(auth -> auth

                // ── Public endpoints (no authentication required) ──────────
                .requestMatchers("/", "/login", "/register", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // ── Admin-only URLs ────────────────────────────────────────
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // hasRole("ADMIN") internally checks for authority "ROLE_ADMIN"

                // ── Manager or Admin can access reports ────────────────────
                .requestMatchers("/reports/**").hasAnyRole("MANAGER", "ADMIN")

                // ── Specific HTTP method restrictions ──────────────────────
                .requestMatchers(HttpMethod.GET, "/api/**")
                    .hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/**")
                    .hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**")
                    .hasRole("ADMIN")  // Only admin can delete

                // ── Everything else requires authentication ────────────────
                .anyRequest().authenticated()
            )

            // ═══════════════════════════════════════════════════════════════════
            //  Authentication configuration
            // ═══════════════════════════════════════════════════════════════════
            .formLogin(Customizer.withDefaults())
            .logout(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Controller — PHASE 2 (Method-Level Authorization)
//  These checks happen AFTER Phase 1 passes, inside the controller.
//  ★ Works the same for Form Login, Basic Auth, JWT, OAuth2!
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // GET /api/orders — Phase 1 already checked hasAnyRole("USER", "ADMIN")
    // No additional Phase 2 check needed here
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.findAll();
    }

    // GET /api/orders/{id} — Phase 2: only the order owner or admin can view
    @PreAuthorize("hasRole('ADMIN') or @orderService.isOwner(#id, authentication.name)")
    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.findById(id);
    }

    // DELETE /api/orders/{id}
    // Phase 1: hasRole("ADMIN") for DELETE /api/** (already checked!)
    // Phase 2: additional check with @PreAuthorize (defense-in-depth)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteById(id);
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Service — Phase 2 security at the service layer
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class OrderService {

    @PreAuthorize("hasRole('USER')")
    public Order placeOrder(Order order) {
        // Only authenticated users with ROLE_USER can place orders
        return orderRepository.save(order);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void cancelAllOrders() {
        // Only admin can perform bulk cancellation
        orderRepository.deleteAll();
    }

    // Helper method used in @PreAuthorize SpEL expression
    public boolean isOwner(Long orderId, String username) {
        Order order = orderRepository.findById(orderId).orElse(null);
        return order != null && order.getCreatedBy().equals(username);
    }
}
```

---

#### 14.9 Complete Sequence Diagram — Authenticated Request from Cookie to Controller

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  COMPLETE SEQUENCE DIAGRAM — AUTHENTICATED REQUEST FLOW                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  Browser              SecurityContext    Authorization    Exception       Dispatcher          │
│  (with cookie)        HolderFilter       Filter          Translation     Servlet             │
│  ─────────            ──────────────     ─────────────   Filter          ──────────           │
│       │                     │                 │              │                │               │
│       │  GET /api/orders    │                 │              │                │               │
│       │  Cookie: JSESSIONID │                 │              │                │               │
│       │  =ABC123            │                 │              │                │               │
│       │────────────────────>│                 │              │                │               │
│       │                     │                 │              │                │               │
│       │                     │ Load deferred   │              │                │               │
│       │                     │ SecurityContext  │              │                │               │
│       │                     │ from HttpSession │              │                │               │
│       │                     │                 │              │                │               │
│       │                     │ ┌─────────────────────────┐   │                │               │
│       │                     │ │ HttpSession lookup:     │   │                │               │
│       │                     │ │ session("ABC123") found │   │                │               │
│       │                     │ │ getAttribute(           │   │                │               │
│       │                     │ │  "SPRING_SECURITY_CTX") │   │                │               │
│       │                     │ │ → SecurityContext {     │   │                │               │
│       │                     │ │     john, ROLE_USER,    │   │                │               │
│       │                     │ │     authenticated=true  │   │                │               │
│       │                     │ │   }                     │   │                │               │
│       │                     │ └─────────────────────────┘   │                │               │
│       │                     │                 │              │                │               │
│       │                     │ Set in ThreadLocal             │                │               │
│       │                     │ (SecurityContextHolder)        │                │               │
│       │                     │────────────────>│              │                │               │
│       │                     │                 │              │                │               │
│       │                     │                 │ Check:       │                │               │
│       │                     │                 │ URL: /api/** │                │               │
│       │                     │                 │ Rule: hasAny │                │               │
│       │                     │                 │ Role("USER", │                │               │
│       │                     │                 │ "ADMIN")     │                │               │
│       │                     │                 │              │                │               │
│       │                     │                 │ User has:    │                │               │
│       │                     │                 │ [ROLE_USER]  │                │               │
│       │                     │                 │              │                │               │
│       │                     │                 │ ✅ GRANTED   │                │               │
│       │                     │                 │──────────────┼───────────────>│               │
│       │                     │                 │              │                │               │
│       │                     │                 │              │     ┌──────────────────────┐  │
│       │                     │                 │              │     │  HandlerMapping      │  │
│       │                     │                 │              │     │  → OrderController   │  │
│       │                     │                 │              │     │                      │  │
│       │                     │                 │              │     │  @PreAuthorize check │  │
│       │                     │                 │              │     │  (Phase 2 — if any)  │  │
│       │                     │                 │              │     │  → GRANTED           │  │
│       │                     │                 │              │     │                      │  │
│       │                     │                 │              │     │  Execute method      │  │
│       │                     │                 │              │     │  return orders list  │  │
│       │                     │                 │              │     └──────────────────────┘  │
│       │                     │                 │              │                │               │
│       │  200 OK             │                 │              │                │               │
│       │  [{ order data }]   │                 │              │                │               │
│       │<─────────────────────────────────────────────────────────────────────│               │
│       │                     │                 │              │                │               │
│       │                     │ finally {       │              │                │               │
│       │                     │  clearContext() │              │                │               │
│       │                     │ }               │              │                │               │
│       │                     │ ThreadLocal=null│              │                │               │
│       │                     │                 │              │                │               │
│                                                                                              │
│                                                                                              │
│  ── SAME FLOW BUT WITH 403 DENIED ──────────────────────────────────────────               │
│                                                                                              │
│  Request: DELETE /admin/users/5                                                             │
│  User: john (ROLE_USER) — needs ROLE_ADMIN                                                  │
│                                                                                              │
│  Browser              SecurityContext    Authorization    Exception                          │
│  ─────────            HolderFilter       Filter          Translation                        │
│       │                     │                 │              │                               │
│       │  DELETE /admin/     │                 │              │                               │
│       │  users/5            │                 │              │                               │
│       │────────────────────>│                 │              │                               │
│       │                     │ Load ctx:       │              │                               │
│       │                     │ { john,         │              │                               │
│       │                     │   ROLE_USER }   │              │                               │
│       │                     │────────────────>│              │                               │
│       │                     │                 │              │                               │
│       │                     │                 │ Check:       │                               │
│       │                     │                 │ /admin/**    │                               │
│       │                     │                 │ requires     │                               │
│       │                     │                 │ ROLE_ADMIN   │                               │
│       │                     │                 │              │                               │
│       │                     │                 │ User has:    │                               │
│       │                     │                 │ ROLE_USER    │                               │
│       │                     │                 │              │                               │
│       │                     │                 │ ❌ DENIED    │                               │
│       │                     │                 │              │                               │
│       │                     │                 │ throw Access │                               │
│       │                     │                 │ Denied       │                               │
│       │                     │                 │ Exception    │                               │
│       │                     │                 │─────────────>│                               │
│       │                     │                 │              │                               │
│       │                     │                 │              │ Is user                       │
│       │                     │                 │              │ authenticated?                │
│       │                     │                 │              │ YES (john)                    │
│       │                     │                 │              │ → 403 Forbidden               │
│       │                     │                 │              │                               │
│       │  403 Forbidden      │                 │              │                               │
│       │<─────────────────────────────────────────────────────│                               │
│       │                     │                 │              │                               │
│       │                     │ finally {       │              │                               │
│       │                     │  clearContext() │              │                               │
│       │                     │ }               │              │                               │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 14.10 ExceptionTranslationFilter — 403 Forbidden vs Redirect to Login

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ExceptionTranslationFilter — DECISION LOGIC                                                 │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ExceptionTranslationFilter sits RIGHT BEFORE AuthorizationFilter in the chain.             │
│  It catches AccessDeniedException thrown by AuthorizationFilter and decides                 │
│  how to respond based on the user's authentication state.                                   │
│                                                                                              │
│                                                                                              │
│  AccessDeniedException caught                                                                │
│       │                                                                                      │
│       ▼                                                                                      │
│  Is the user authenticated?                                                                  │
│       │                                                                                      │
│    ┌──┴──┐                                                                                   │
│    │     │                                                                                   │
│  YES    NO (anonymous / no session)                                                          │
│    │     │                                                                                   │
│    ▼     ▼                                                                                   │
│  ┌────────────────────┐  ┌────────────────────────────────────────────┐                     │
│  │  AccessDeniedHandler│  │  AuthenticationEntryPoint                  │                     │
│  │                     │  │                                            │                     │
│  │  User is logged in  │  │  User is NOT logged in                    │                     │
│  │  but LACKS the      │  │  → Save original URL to RequestCache      │                     │
│  │  required role.     │  │    (so we can redirect back after login)  │                     │
│  │                     │  │  → Redirect to /login (302)               │                     │
│  │  → Return 403       │  │                                            │                     │
│  │    Forbidden        │  │  After login:                              │                     │
│  │  → Show error page  │  │  → SavedRequestAwareSuccessHandler        │                     │
│  │    or JSON error    │  │    redirects back to the original URL     │                     │
│  │                     │  │                                            │                     │
│  └────────────────────┘  └────────────────────────────────────────────┘                     │
│                                                                                              │
│                                                                                              │
│  ── CUSTOM AccessDeniedHandler ──────────────────────────────────────────                   │
│                                                                                              │
│  You can customize the 403 response:                                                        │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  http                                                                         │           │
│  │      .exceptionHandling(ex -> ex                                              │           │
│  │          .accessDeniedPage("/access-denied")  // redirect to custom page     │           │
│  │          // OR                                                                │           │
│  │          .accessDeniedHandler((request, response, exception) -> {            │           │
│  │              response.setStatus(HttpServletResponse.SC_FORBIDDEN);            │           │
│  │              response.setContentType("application/json");                     │           │
│  │              response.getWriter().write(                                       │           │
│  │                  "{\"error\":\"You don't have permission\"}"                  │           │
│  │              );                                                               │           │
│  │          })                                                                   │           │
│  │      );                                                                       │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 14.11 Summary — Complete Flow from Cookie to Controller (or Rejection)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SUMMARY — EVERY AUTHENTICATED REQUEST IN ONE DIAGRAM                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  Browser sends request with JSESSIONID cookie                                               │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐                │
│  │  SecurityContextHolderFilter                                            │                │
│  │  ├── request.getSession(false) → lookup JSESSIONID in Tomcat          │                │
│  │  │                                                                      │                │
│  │  ├── Session FOUND?                                                     │                │
│  │  │   ├── YES → getAttribute("SPRING_SECURITY_CONTEXT")                │                │
│  │  │   │         → SecurityContext { john, ROLE_USER, auth=true }       │                │
│  │  │   │         → SecurityContextHolder.setContext(ctx)                │                │
│  │  │   │         → ★ User is "remembered" for this request             │                │
│  │  │   │                                                                  │                │
│  │  │   └── NO → SecurityContext = EMPTY { authentication: null }        │                │
│  │  │            → User is treated as anonymous                           │                │
│  │  │                                                                      │                │
│  │  └── Continue to next filter...                                        │                │
│  └─────────────────────────────────┬───────────────────────────────────────┘                │
│                                    │                                                         │
│                                    ▼                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐                │
│  │  AuthorizationFilter (PHASE 1 — URL-Level)                              │                │
│  │  ├── Read auth from SecurityContextHolder                              │                │
│  │  ├── Match request URL against SecurityConfig rules                    │                │
│  │  │                                                                      │                │
│  │  ├── GRANTED?                                                           │                │
│  │  │   ├── YES → continue to DispatcherServlet                          │                │
│  │  │   │                                                                  │                │
│  │  │   └── NO → AccessDeniedException                                   │                │
│  │  │            ├── User authenticated? → 403 Forbidden                 │                │
│  │  │            └── User anonymous?     → 302 redirect to /login        │                │
│  │  │                                                                      │                │
│  └─────────────────────────────────┬───────────────────────────────────────┘                │
│                                    │                                                         │
│                                    ▼                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐                │
│  │  DispatcherServlet → Controller (PHASE 2 — Method-Level)                │                │
│  │  ├── AOP Proxy intercepts method call                                  │                │
│  │  ├── Read @PreAuthorize annotation                                     │                │
│  │  ├── Evaluate SpEL expression against SecurityContextHolder            │                │
│  │  │                                                                      │                │
│  │  ├── GRANTED?                                                           │                │
│  │  │   ├── YES → execute controller method → return response            │                │
│  │  │   └── NO  → AccessDeniedException → 403 Forbidden                  │                │
│  │  │                                                                      │                │
│  └─────────────────────────────────────────────────────────────────────────┘                │
│                                                                                              │
│                                                                                              │
│  ★ KEY TAKEAWAYS:                                                                           │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  1. SecurityContextHolderFilter LOADS the user identity from HttpSession    │           │
│  │     → No re-authentication on every request! Cookie = proof of identity    │           │
│  │                                                                               │           │
│  │  2. SecurityContextHolder (ThreadLocal) makes the user available everywhere│           │
│  │     → Available for the ENTIRE request lifecycle                             │           │
│  │     → Cleared in finally{} block after response is sent                     │           │
│  │                                                                               │           │
│  │  3. AuthorizationFilter (Phase 1) checks URL-level access rules            │           │
│  │     → Configured centrally in SecurityConfig                                 │           │
│  │     → Runs BEFORE controller code executes                                  │           │
│  │                                                                               │           │
│  │  4. @PreAuthorize (Phase 2) checks method-level access rules               │           │
│  │     → Fine-grained, per-method, with SpEL support                           │           │
│  │     → Works identically for form login, basic auth, JWT, OAuth2            │           │
│  │                                                                               │           │
│  │  5. By default, Spring Security ONLY requires authentication               │           │
│  │     → Does NOT restrict by role — you MUST configure role rules manually   │           │
│  │                                                                               │           │
│  │  6. Invalid/expired JSESSIONID → empty SecurityContext → redirect to /login │           │
│  │     Authenticated but wrong role → 403 Forbidden                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```


---


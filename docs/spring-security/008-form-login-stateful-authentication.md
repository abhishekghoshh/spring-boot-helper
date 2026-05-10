### 8. Form Login — Stateful Authentication Deep Dive

---

#### 8.1 What Is Form Login?

Form Login is an **authentication mechanism** where the server provides an **HTML login page** (a form with username and password fields), and the user submits credentials via a **POST request**. After successful authentication, the server creates a **session** (stored server-side) and sends back a **session cookie** (`JSESSIONID`) to the browser. All subsequent requests carry this cookie — no need to re-enter credentials. It is still the defalt authentication process in apring security.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  FORM LOGIN — HOW IT WORKS                                                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Browser                                  Server (Spring Boot)                               │
│    │                                          │                                              │
│    │  1. GET /dashboard                       │                                              │
│    │─────────────────────────────────────────>│                                              │
│    │                                          │  Not authenticated!                          │
│    │                                          │  Redirect to /login                          │
│    │  2. 302 Redirect → /login                │                                              │
│    │<─────────────────────────────────────────│                                              │
│    │                                          │                                              │
│    │  3. GET /login                           │                                              │
│    │─────────────────────────────────────────>│                                              │
│    │                                          │                                              │
│    │  4. 200 OK (HTML login page)             │                                              │
│    │<─────────────────────────────────────────│                                              │
│    │                                          │                                              │
│    │  ┌────────────────────────────────┐      │                                              │
│    │  │     Spring Security Login      │      │                                              │
│    │  │                                │      │                                              │
│    │  │  Username: [john         ]     │      │                                              │
│    │  │  Password: [••••••       ]     │      │                                              │
│    │  │                                │      │                                              │
│    │  │        [Sign In]               │      │                                              │
│    │  └────────────────────────────────┘      │                                              │
│    │                                          │                                              │
│    │  5. POST /login                          │                                              │
│    │     Content-Type: x-www-form-urlencoded  │                                              │
│    │     username=john&password=john123        │                                              │
│    │─────────────────────────────────────────>│                                              │
│    │                                          │  Authenticate:                               │
│    │                                          │  - loadUserByUsername("john")                │
│    │                                          │  - BCrypt.matches("john123", "$2a$12$...")   │
│    │                                          │  - SUCCESS ✓                                │
│    │                                          │                                              │
│    │                                          │  Create HttpSession:                         │
│    │                                          │  - sessionId = "ABC123XYZ"                   │
│    │                                          │  - Store Authentication in session           │
│    │                                          │                                              │
│    │  6. 302 Redirect → /dashboard            │                                              │
│    │     Set-Cookie: JSESSIONID=ABC123XYZ     │  ← Server sends session cookie              │
│    │<─────────────────────────────────────────│                                              │
│    │                                          │                                              │
│    │  7. GET /dashboard                       │                                              │
│    │     Cookie: JSESSIONID=ABC123XYZ         │  ← Browser sends cookie automatically       │
│    │─────────────────────────────────────────>│                                              │
│    │                                          │  Look up session ABC123XYZ                   │
│    │                                          │  → Found! User = john, ROLE_USER             │
│    │                                          │  → Authenticated ✓                          │
│    │                                          │                                              │
│    │  8. 200 OK (Dashboard HTML)              │                                              │
│    │<─────────────────────────────────────────│                                              │
│    │                                          │                                              │
│    │  ALL SUBSEQUENT REQUESTS:                │                                              │
│    │  Cookie: JSESSIONID=ABC123XYZ            │  ← No username/password needed!              │
│    │  → Server finds session → authenticated  │                                              │
│    │                                          │                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**Spring Security code that enables Form Login:**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        // This single line enables the entire Form Login flow:
        .formLogin(Customizer.withDefaults());
        // ↑ Activates:
        //   - DefaultLoginPageGeneratingFilter (auto-generates /login page)
        //   - UsernamePasswordAuthenticationFilter (processes POST /login)
        //   - Session creation after successful authentication
        //   - Redirect to original URL after login
        //   - Redirect to /login?error on failure

    return http.build();
}
```

---

#### 8.2 Why Form Login Was Popular Previously

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHY FORM LOGIN WAS THE DOMINANT AUTHENTICATION METHOD (2000s–2015)                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  1. WEB APPS WERE SERVER-RENDERED (Monolithic Architecture)                                 │
│  ──────────────────────────────────────────────────────────                                   │
│  ┌─────────┐         ┌───────────────────────────────────────────┐                          │
│  │ Browser │ ──────> │  Server (Spring MVC / JSP / Thymeleaf)    │                          │
│  │         │ <────── │  Renders HTML pages on the server side    │                          │
│  └─────────┘         │  Session stored in server memory          │                          │
│                      └───────────────────────────────────────────┘                          │
│                                                                                              │
│  • The server generated HTML pages (JSP, Thymeleaf, Freemarker)                             │
│  • Browser was a "dumb" client — just rendered HTML                                         │
│  • No JavaScript frameworks (React, Angular, Vue didn't exist)                              │
│  • Server had full control over the user session                                            │
│  • Form Login + sessions was the NATURAL fit for this architecture                          │
│                                                                                              │
│  2. SINGLE SERVER DEPLOYMENT                                                                │
│  ───────────────────────────                                                                 │
│  • Apps ran on a SINGLE server (Tomcat, JBoss, WebSphere)                                   │
│  • Session stored in server's JVM memory — simple and fast                                  │
│  • No need to share sessions across multiple servers                                        │
│  • Horizontal scaling was rare for most apps                                                │
│                                                                                              │
│  3. BROWSERS WERE THE ONLY CLIENT                                                           │
│  ────────────────────────────────                                                            │
│  • No mobile apps (iPhone launched 2007, but mobile web was minimal)                        │
│  • No SPAs (Single Page Applications)                                                       │
│  • No API-first architecture                                                                │
│  • Cookies + sessions worked perfectly because ONLY browsers used the app                   │
│                                                                                              │
│  4. SIMPLICITY                                                                              │
│  ─────────────                                                                               │
│  • Session management is built into every servlet container (Tomcat, Jetty)                 │
│  • No extra libraries needed (no JWT, no OAuth2)                                            │
│  • Cookies are automatically sent by the browser — zero client-side code                    │
│  • Spring Security provides Form Login out of the box with one line of config               │
│                                                                                              │
│  5. ENTERPRISE STANDARD                                                                     │
│  ────────────────────                                                                        │
│  • Java EE / Servlet specification defined session management                               │
│  • All enterprise frameworks supported it (Struts, Spring MVC, JSF)                         │
│  • Security auditors understood sessions — it was the established pattern                   │
│  • CSRF protection worked seamlessly with sessions                                          │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8.3 Why Form Login Is NOT Used Often Now — Why JWT Is Preferred

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHY FORM LOGIN DECLINED — THE SHIFT TO JWT & STATELESS AUTH                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  THEN (2005–2015): Monolithic + Server-Rendered                                             │
│  ┌─────────┐         ┌──────────────────┐                                                   │
│  │ Browser │ ──────> │  Monolith Server  │   Session in memory ✓                            │
│  │         │ <────── │  (JSP/Thymeleaf)  │   Single server ✓                                │
│  └─────────┘         └──────────────────┘   Cookie-based ✓                                  │
│                                                                                              │
│  NOW (2016+): Microservices + SPA + Mobile + API-First                                      │
│  ┌──────────┐                                                                               │
│  │ React/   │──┐     ┌────────────────┐    ┌────────────────┐                              │
│  │ Angular  │  │     │ API Gateway    │───>│ User Service   │                              │
│  └──────────┘  │     │                │    └────────────────┘                              │
│  ┌──────────┐  ├────>│ (No sessions!) │───>┌────────────────┐                              │
│  │ Mobile   │  │     │                │    │ Order Service  │                              │
│  │ App      │──┘     └────────────────┘───>└────────────────┘                              │
│  └──────────┘                          ───>┌────────────────┐                              │
│  ┌──────────┐                              │ Payment Service│                              │
│  │ 3rd Party│─── API ────────────────────> └────────────────┘                              │
│  │ Service  │                                                                               │
│  └──────────┘                                                                               │
│                                                                                              │
│  PROBLEMS WITH FORM LOGIN IN MODERN ARCHITECTURE:                                           │
│  ─────────────────────────────────────────────────                                           │
│                                                                                              │
│  Problem 1: MULTIPLE SERVERS (Horizontal Scaling)                                           │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  Load Balancer                                                             │             │
│  │       │                                                                    │             │
│  │       ├── Server A (has session ABC123 for john)                          │             │
│  │       ├── Server B (does NOT have session ABC123!) ← 401 Unauthorized!   │             │
│  │       └── Server C (does NOT have session ABC123!) ← 401 Unauthorized!   │             │
│  │                                                                            │             │
│  │  Session is in Server A's memory. If load balancer routes the next        │             │
│  │  request to Server B → user is NOT authenticated!                          │             │
│  │                                                                            │             │
│  │  Workarounds:                                                              │             │
│  │  • Sticky sessions (bind user to one server — defeats load balancing)     │             │
│  │  • Session replication (copy sessions across servers — network overhead)   │             │
│  │  • External session store (Redis/DB — adds infrastructure complexity)     │             │
│  │                                                                            │             │
│  │  JWT solution: Token is SELF-CONTAINED. Any server can verify it.         │             │
│  │  No shared state needed. No Redis. No session replication.                │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│  Problem 2: MOBILE APPS DON'T SUPPORT COOKIES NATIVELY                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  Browser:                                                                  │             │
│  │  Set-Cookie: JSESSIONID=ABC123 → Browser stores and auto-sends ✓         │             │
│  │                                                                            │             │
│  │  Mobile App (Android/iOS):                                                 │             │
│  │  Set-Cookie: JSESSIONID=ABC123 → Not natively supported ✗                │             │
│  │  Must manually manage cookies — awkward and error-prone                    │             │
│  │                                                                            │             │
│  │  JWT solution: Token sent in Authorization header                         │             │
│  │  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...                           │             │
│  │  → Works identically on browsers, mobile apps, CLI tools, IoT devices    │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│  Problem 3: SPAs (Single Page Applications) USE APIs, NOT FORMS                             │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  Traditional (Form Login):                                                 │             │
│  │  Browser → POST /login (form) → Server renders next page                 │             │
│  │                                                                            │             │
│  │  Modern SPA (React/Angular/Vue):                                           │             │
│  │  Browser → POST /api/auth/login (JSON) → Gets JWT token                  │             │
│  │         → Stores token in memory/localStorage                              │             │
│  │         → Sends token with every API request                               │             │
│  │         → Frontend handles routing, not the server                        │             │
│  │                                                                            │             │
│  │  SPAs don't do full-page redirects to /login — they use JavaScript        │             │
│  │  to call APIs and handle login in a React/Angular component.              │             │
│  │  Form Login's redirect-based flow doesn't fit SPA architecture.           │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│  Problem 4: MICROSERVICES NEED INTER-SERVICE AUTH                                           │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  User Service ──── Order Service ──── Payment Service                     │             │
│  │                                                                            │             │
│  │  With sessions: Each service needs to validate the session.               │             │
│  │  How? Shared session store (Redis)? Session token forwarding?             │             │
│  │  Very complex. Every service needs access to the session store.            │             │
│  │                                                                            │             │
│  │  With JWT: Token is self-contained with user info + roles.                │             │
│  │  Any service can verify it independently using the secret key.            │             │
│  │  No shared state. No Redis. Each service is truly independent.            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│  Problem 5: CROSS-ORIGIN REQUESTS (CORS) + THIRD-PARTY APIs                                │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  Cookies are domain-bound:                                                 │             │
│  │  Cookie from api.example.com is NOT sent to other-api.com                 │             │
│  │  Third-party APIs cannot use your session cookies                          │             │
│  │                                                                            │             │
│  │  JWT in Authorization header:                                              │             │
│  │  Can be sent to ANY domain. Works across origins.                         │             │
│  │  Third-party services can validate the same JWT.                          │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8.4 Advantages, Disadvantages & Limitations of Form Login

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  FORM LOGIN — ADVANTAGES                                                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  1. SIMPLE TO IMPLEMENT                                                                     │
│     • One line: .formLogin(Customizer.withDefaults())                                       │
│     • Spring Security auto-generates the login page                                         │
│     • No JWT libraries, no token generation, no secret key management                       │
│                                                                                              │
│  2. SERVER-SIDE SESSION CONTROL                                                             │
│     • Server can invalidate sessions immediately (logout, ban)                              │
│     • Admin can force-logout any user by deleting their session                             │
│     • No need to wait for token expiry (unlike JWT)                                         │
│                                                                                              │
│  3. BUILT-IN CSRF PROTECTION                                                                │
│     • Sessions + CSRF tokens work together naturally                                        │
│     • Spring Security generates CSRF token per session automatically                        │
│     • Forms include hidden _csrf field — protection is transparent                          │
│                                                                                              │
│  4. BROWSER-NATIVE                                                                          │
│     • Cookies are automatically managed by the browser                                      │
│     • No JavaScript needed to handle authentication                                         │
│     • Browser handles cookie storage, expiry, and sending                                   │
│     • "Remember Me" is straightforward with persistent cookies                              │
│                                                                                              │
│  5. SESSION DATA                                                                            │
│     • Can store unlimited data in the session (cart, preferences, wizard state)             │
│     • Not limited to token payload size                                                     │
│     • Server-side data is never exposed to the client                                       │
│                                                                                              │
│  6. SECURE BY DEFAULT                                                                       │
│     • Session ID is an opaque random string — no user data leaked                          │
│     • Cannot be decoded like JWT (JWT payload is Base64 — readable!)                       │
│     • HttpOnly + Secure cookie flags prevent JS access and MITM                             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  FORM LOGIN — DISADVANTAGES                                                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  1. NOT SCALABLE HORIZONTALLY (without extra infrastructure)                                │
│     • Sessions stored in server memory → tied to one server                                 │
│     • Scaling requires sticky sessions, session replication, or Redis                       │
│     • Adds infrastructure cost and complexity                                               │
│                                                                                              │
│  2. SERVER MEMORY CONSUMPTION                                                               │
│     • Each active user consumes memory on the server                                        │
│     • 100,000 concurrent users = 100,000 session objects in memory                          │
│     • Can cause OutOfMemoryError under heavy load                                           │
│                                                                                              │
│  3. DOESN'T WORK FOR MOBILE APPS / SPAs / APIs                                             │
│     • Mobile apps don't handle cookies natively                                             │
│     • SPAs prefer token-based auth (localStorage/memory)                                    │
│     • REST APIs should be stateless (no sessions)                                           │
│                                                                                              │
│  4. CROSS-ORIGIN LIMITATIONS                                                                │
│     • Cookies are domain-bound — don't work across different domains                        │
│     • CORS + cookies requires specific configuration (withCredentials: true)                │
│     • Third-party integrations cannot use your session cookies                              │
│                                                                                              │
│  5. VULNERABLE TO CSRF ATTACKS (if not protected)                                           │
│     • Cookies are sent automatically → malicious sites can exploit this                     │
│     • Must use CSRF tokens (Spring Security does this by default)                           │
│     • JWT in Authorization header is immune to CSRF (not auto-sent)                        │
│                                                                                              │
│  6. REDIRECT-BASED FLOW                                                                     │
│     • POST /login → 302 Redirect → GET /dashboard                                         │
│     • Not suitable for JavaScript-based API calls (fetch/axios)                             │
│     • SPAs expect JSON responses, not HTML redirects                                        │
│                                                                                              │
│  7. SESSION FIXATION RISK                                                                   │
│     • Attacker could inject a known session ID before login                                 │
│     • Must rotate session ID after authentication                                           │
│     • Spring Security handles this with SessionFixationProtection                           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  FORM LOGIN — LIMITATIONS                                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  1. Browser-only       │ Designed for browser form submissions, not API clients             │
│  2. Same-origin only   │ Cookies don't cross domain boundaries easily                       │
│  3. Stateful           │ Server must maintain session state — contradicts REST principles   │
│  4. Not distributed    │ Sessions are local to one server by default                        │
│  5. No token sharing   │ Cannot share auth state with microservices without extra infra     │
│  6. Full page redirect │ Login flow requires page redirects — bad for SPA UX               │
│  7. Server affinity    │ User is effectively "bound" to the server holding their session    │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**Form Login vs JWT — Side by Side Comparison:**

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  FORM LOGIN (Sessions) vs JWT (Tokens) — COMPARISON                                    │
├──────────────────────┬──────────────────────────┬───────────────────────────────────────┤
│  Aspect               │ Form Login (Session)      │ JWT (Token)                          │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  State                │ Stateful (server stores   │ Stateless (token is self-contained) │
│                       │ session in memory/Redis)  │                                      │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Storage (server)     │ Server memory / Redis     │ Nothing (token has all info)         │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Storage (client)     │ Cookie (JSESSIONID)       │ localStorage / memory / cookie       │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Sent how?            │ Cookie (automatic)        │ Authorization: Bearer <token>        │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Scalability          │ Poor (sticky sessions /   │ Excellent (any server can verify)    │
│                       │ session replication)       │                                      │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Mobile support       │ Awkward (cookie mgmt)     │ Native (just send header)            │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Microservices        │ Needs shared session      │ Each service verifies independently  │
│                       │ store                      │                                      │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Instant revocation   │ Yes (delete session)      │ No (must wait for token expiry or   │
│                       │                            │ use blacklist)                       │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  CSRF vulnerable?     │ Yes (cookies auto-sent)   │ No (header not auto-sent)            │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  XSS risk             │ Low (HttpOnly cookie)     │ High if stored in localStorage       │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Data in auth token   │ None (opaque session ID)  │ User data in payload (readable)      │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Best for             │ Server-rendered web apps,  │ SPAs, mobile apps, microservices,   │
│                       │ monoliths, admin panels    │ API-first architecture               │
├──────────────────────┼──────────────────────────┼───────────────────────────────────────┤
│  Setup complexity     │ Very low (1 line config)  │ Medium (key mgmt, token generation, │
│                       │                            │ refresh tokens, etc.)                │
└──────────────────────┴──────────────────────────┴───────────────────────────────────────┘
```

---

#### 8.5 Why Form Login Is a Stateful Authentication Method — Deep Dive

**Stateful** means the **server maintains the user's authentication state** (the session) so that the user doesn't have to provide username/password with every request.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  STATEFUL vs STATELESS AUTHENTICATION                                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  STATEFUL (Form Login / Session-Based):                                                     │
│  ──────────────────────────────────────                                                      │
│  Server REMEMBERS who you are after login.                                                   │
│  It stores your authentication state in a session object.                                    │
│  Your browser sends a session ID cookie — server looks it up.                               │
│                                                                                              │
│  Request 1: POST /login (username + password) → Server creates session                      │
│  Request 2: GET /api/orders (Cookie: JSESSIONID=ABC) → Server looks up session → found ✓  │
│  Request 3: GET /api/profile (Cookie: JSESSIONID=ABC) → Server looks up session → found ✓ │
│  ...                                                                                         │
│  No username/password needed after login! Server remembers via session.                      │
│                                                                                              │
│  ────────────────────────────────────────────────────────────────────────────────            │
│                                                                                              │
│  STATELESS (JWT / Token-Based):                                                             │
│  ──────────────────────────────                                                              │
│  Server does NOT remember who you are. No session. No server-side storage.                  │
│  Client sends a self-contained token with EVERY request.                                    │
│  Server validates the token's signature — if valid, user is authenticated.                  │
│                                                                                              │
│  Request 1: POST /auth/login → Server returns JWT token (no session created)                │
│  Request 2: GET /api/orders (Authorization: Bearer eyJhbG...) → Verify token → valid ✓   │
│  Request 3: GET /api/profile (Authorization: Bearer eyJhbG...) → Verify token → valid ✓  │
│  ...                                                                                         │
│  Token carries user info (username, roles, expiry). Server stores NOTHING.                  │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**How Stateful Session-Based Auth Works Internally in Spring Security:**

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  STATEFUL AUTHENTICATION — INTERNAL MECHANICS                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ═══════════════════════════════════════════════════════════                                 │
│  FIRST REQUEST (Login)                                                                       │
│  ═══════════════════════════════════════════════════════════                                 │
│                                                                                              │
│  POST /login  username=john&password=john123                                                │
│       │                                                                                      │
│       ▼                                                                                      │
│  UsernamePasswordAuthenticationFilter:                                                       │
│  → Extracts username + password from form data                                              │
│  → Creates UsernamePasswordAuthenticationToken                                              │
│  → Passes to AuthenticationManager                                                           │
│       │                                                                                      │
│       ▼                                                                                      │
│  DaoAuthenticationProvider:                                                                  │
│  → loadUserByUsername("john") → DB lookup → found                                           │
│  → BCrypt.matches("john123", "$2a$12$...") → true ✓                                       │
│  → Returns authenticated token with [ROLE_USER]                                             │
│       │                                                                                      │
│       ▼                                                                                      │
│  SecurityContextHolder:                                                                      │
│  ┌──────────────────────────────────────────────────────────────────┐                       │
│  │  SecurityContext {                                                │                       │
│  │    Authentication: UsernamePasswordAuthenticationToken {          │                       │
│  │      principal: UserAuthEntity { username: "john" },             │                       │
│  │      credentials: null,  // cleared after auth for security      │                       │
│  │      authorities: [ROLE_USER],                                    │                       │
│  │      authenticated: true                                          │                       │
│  │    }                                                              │                       │
│  │  }                                                                │                       │
│  └──────────────────────────────────────────────────────────────────┘                       │
│       │                                                                                      │
│       ▼                                                                                      │
│  SecurityContextPersistenceFilter / SecurityContextHolderFilter:                            │
│  → SAVES SecurityContext INTO HttpSession                                                    │
│       │                                                                                      │
│       ▼                                                                                      │
│  Tomcat creates HttpSession:                                                                 │
│  ┌──────────────────────────────────────────────────────────────────┐                       │
│  │  SERVER MEMORY (Tomcat's ConcurrentHashMap of sessions)         │                       │
│  │                                                                  │                       │
│  │  Session ID: "ABC123XYZ789"                                      │                       │
│  │  ┌──────────────────────────────────────────────────────┐       │                       │
│  │  │  Attributes:                                          │       │                       │
│  │  │  "SPRING_SECURITY_CONTEXT" → SecurityContext {        │       │                       │
│  │  │     authentication: {                                  │       │                       │
│  │  │       principal: john,                                 │       │                       │
│  │  │       authorities: [ROLE_USER],                        │       │                       │
│  │  │       authenticated: true                              │       │                       │
│  │  │     }                                                  │       │                       │
│  │  │  }                                                     │       │                       │
│  │  │  creationTime: 2026-04-29T10:00:00                     │       │                       │
│  │  │  maxInactiveInterval: 1800 (30 min)                    │       │                       │
│  │  └──────────────────────────────────────────────────────┘       │                       │
│  └──────────────────────────────────────────────────────────────────┘                       │
│       │                                                                                      │
│       ▼                                                                                      │
│  Response to browser:                                                                        │
│  HTTP/1.1 302 Found                                                                          │
│  Location: /dashboard                                                                        │
│  Set-Cookie: JSESSIONID=ABC123XYZ789; Path=/; HttpOnly; Secure                              │
│  ────────────────────────────────────────────────────────────────                            │
│  ↑ This cookie is the KEY. Browser stores it and sends it with EVERY request.               │
│                                                                                              │
│                                                                                              │
│  ═══════════════════════════════════════════════════════════                                 │
│  SUBSEQUENT REQUESTS (No login needed!)                                                      │
│  ═══════════════════════════════════════════════════════════                                 │
│                                                                                              │
│  GET /api/orders                                                                             │
│  Cookie: JSESSIONID=ABC123XYZ789    ← Browser sends this automatically                     │
│       │                                                                                      │
│       ▼                                                                                      │
│  SecurityContextHolderFilter:                                                                │
│  → Reads JSESSIONID from cookie                                                             │
│  → Looks up HttpSession by ID "ABC123XYZ789" in Tomcat's session map                       │
│  → Finds session! Extracts "SPRING_SECURITY_CONTEXT" attribute                              │
│  → Sets SecurityContextHolder.setContext(savedContext)                                       │
│  → Now the request has Authentication = john with ROLE_USER                                 │
│       │                                                                                      │
│       ▼                                                                                      │
│  AuthorizationFilter:                                                                        │
│  → Checks: Is /api/orders accessible with ROLE_USER? → YES ✓                              │
│       │                                                                                      │
│       ▼                                                                                      │
│  Controller executes: returns orders for john                                                │
│                                                                                              │
│  ⚠️ NO username/password sent! NO authentication performed!                                 │
│  The session cookie was enough — server looked up the saved SecurityContext.                 │
│  THIS IS WHAT "STATEFUL" MEANS — the server REMEMBERS the user's auth state.               │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**The Session Lifecycle:**

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SESSION LIFECYCLE — FROM LOGIN TO LOGOUT/EXPIRY                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  1. LOGIN → Session Created                                                                 │
│     POST /login (correct credentials)                                                        │
│     → HttpSession created with unique ID                                                    │
│     → SecurityContext stored in session                                                      │
│     → JSESSIONID cookie sent to browser                                                     │
│                                                                                              │
│  2. ACTIVE → Session Used                                                                   │
│     Every request with Cookie: JSESSIONID=ABC123                                            │
│     → Server finds session → user is authenticated                                          │
│     → Session's "last accessed time" is updated                                             │
│     → No re-authentication needed                                                           │
│                                                                                              │
│  3. TIMEOUT → Session Expires                                                               │
│     If no request for 30 minutes (default):                                                  │
│     → Tomcat's background thread checks sessions                                            │
│     → Session "ABC123" expired → removed from memory                                        │
│     → Next request with Cookie: JSESSIONID=ABC123                                           │
│     → Server: "Session not found!" → Redirect to /login                                    │
│     → User must log in again                                                                │
│                                                                                              │
│  4. LOGOUT → Session Invalidated                                                            │
│     POST /logout                                                                             │
│     → HttpSession.invalidate() → removed from server memory                                │
│     → Cookie cleared: Set-Cookie: JSESSIONID=; Max-Age=0                                   │
│     → SecurityContextHolder.clearContext()                                                   │
│     → Redirect to /login?logout                                                             │
│                                                                                              │
│  Timeline:                                                                                   │
│  ─────────────────────────────────────────────────────────────────                           │
│  Login     Request  Request  Request  ... (idle for 30min) ... Timeout                      │
│  ──┬──────────┬───────┬────────┬──────────────────────────────────┬──                       │
│    │ Session  │       │        │  Session alive, last accessed     │                         │
│    │ created  │       │        │  time keeps resetting             │ Session expired          │
│    │          │       │        │                                   │ User must re-login       │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**Where sessions are stored — Server Memory Layout:**

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  TOMCAT SERVER MEMORY — SESSION STORAGE                                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  JVM Heap Memory                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────────────────────┐   │
│  │  Tomcat's SessionManager (ConcurrentHashMap<String, HttpSession>)                    │   │
│  │                                                                                      │   │
│  │  ┌─────────────────────┬──────────────────────────────────────────────────────────┐ │   │
│  │  │  Session ID          │  Session Data                                            │ │   │
│  │  ├─────────────────────┼──────────────────────────────────────────────────────────┤ │   │
│  │  │  "ABC123XYZ789"     │  { SPRING_SECURITY_CONTEXT: { auth: john, ROLE_USER } } │ │   │
│  │  │  "DEF456UVW012"     │  { SPRING_SECURITY_CONTEXT: { auth: jane, ROLE_ADMIN} } │ │   │
│  │  │  "GHI789RST345"     │  { SPRING_SECURITY_CONTEXT: { auth: bob, ROLE_USER } }  │ │   │
│  │  │  ...                 │  ... (one entry per logged-in user)                      │ │   │
│  │  └─────────────────────┴──────────────────────────────────────────────────────────┘ │   │
│  │                                                                                      │   │
│  │  ⚠️ Each session consumes memory!                                                   │   │
│  │  1,000 users → 1,000 session objects                                                │   │
│  │  100,000 users → 100,000 session objects → potential OOM!                           │   │
│  │                                                                                      │   │
│  │  ⚠️ If server restarts → ALL sessions lost → ALL users logged out!                 │   │
│  │                                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                              │
│  CONTRAST WITH JWT (Stateless):                                                             │
│  ┌──────────────────────────────────────────────────────────────────────────────────────┐   │
│  │  Server Memory: NOTHING stored per user                                              │   │
│  │  • No session objects                                                                │   │
│  │  • No HashMap of users                                                               │   │
│  │  • Just the secret key to verify tokens                                              │   │
│  │                                                                                      │   │
│  │  Each request carries a JWT: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...          │   │
│  │  Server verifies signature → extracts user info → done                               │   │
│  │  Server restart? Tokens still valid! (signature verification is stateless)           │   │
│  └──────────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**Spring Security code — Stateful (Session) vs Stateless (JWT) configuration:**

```java
// ═══════════════════════════════════════════════════════════
// STATEFUL — Form Login with Sessions (default behavior)
// ═══════════════════════════════════════════════════════════
@Bean
public SecurityFilterChain statefulFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(Customizer.withDefaults())  // Enables form login
        // Session management (defaults — usually don't need to configure explicitly)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)  // ← DEFAULT
            // IF_REQUIRED = Create session only when needed (after login)
            // This is STATEFUL — server stores session
        );
    return http.build();
}

// ═══════════════════════════════════════════════════════════
// STATELESS — JWT with No Sessions
// ═══════════════════════════════════════════════════════════
@Bean
public SecurityFilterChain statelessFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())  // No CSRF needed for stateless
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        // NO form login — APIs don't use HTML forms
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // ← KEY!
            // STATELESS = NEVER create HttpSession, NEVER use cookies
            // Every request must carry a JWT token in Authorization header
        )
        // Custom JWT filter would be added here:
        // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        ;
    return http.build();
}
```

**SessionCreationPolicy options:**

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  SessionCreationPolicy — ALL OPTIONS                                             │
├────────────────────┬─────────────────────────────────────────────────────────────┤
│  Policy             │ Behavior                                                   │
├────────────────────┼─────────────────────────────────────────────────────────────┤
│  IF_REQUIRED        │ DEFAULT. Creates session only when needed (after login).   │
│  (default)          │ Used with Form Login. STATEFUL.                            │
├────────────────────┼─────────────────────────────────────────────────────────────┤
│  ALWAYS             │ Always creates a session, even for unauthenticated users.  │
│                     │ More memory usage. Rarely needed.                           │
├────────────────────┼─────────────────────────────────────────────────────────────┤
│  NEVER              │ Spring Security NEVER creates a session, but WILL USE one  │
│                     │ if it already exists (created by other code).              │
├────────────────────┼─────────────────────────────────────────────────────────────┤
│  STATELESS          │ NEVER creates or uses a session. Every request must carry  │
│                     │ its own authentication (JWT token). Used for REST APIs.    │
│                     │ No JSESSIONID cookie. No server-side session storage.      │
└────────────────────┴─────────────────────────────────────────────────────────────┘
```

---

#### 8.6 When to Use Form Login vs JWT — Decision Guide

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  DECISION GUIDE: FORM LOGIN vs JWT                                                           │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  USE FORM LOGIN (Stateful / Sessions) when:                                                 │
│  ├── ✓ Building a traditional server-rendered web app (Thymeleaf, JSP)                     │
│  ├── ✓ Building an admin panel / back-office tool                                           │
│  ├── ✓ Single server deployment (no horizontal scaling needed)                              │
│  ├── ✓ Need instant session revocation (logout, ban)                                       │
│  ├── ✓ Want simplicity — minimal configuration                                             │
│  ├── ✓ Need CSRF protection (browser form submissions)                                     │
│  └── ✓ Users only access via browser                                                       │
│                                                                                              │
│  USE JWT (Stateless / Tokens) when:                                                         │
│  ├── ✓ Building a REST API consumed by SPA (React, Angular, Vue)                           │
│  ├── ✓ Building APIs for mobile apps (Android, iOS)                                        │
│  ├── ✓ Microservices architecture (services need to verify auth independently)             │
│  ├── ✓ Need horizontal scaling (multiple server instances behind load balancer)             │
│  ├── ✓ Third-party API consumers need authentication                                       │
│  ├── ✓ Cross-origin / cross-domain authentication needed                                   │
│  └── ✓ Want truly stateless servers (cloud-native, serverless)                              │
│                                                                                              │
│  USE BOTH when:                                                                              │
│  ├── ✓ App has both browser UI (admin panel) AND API endpoints                             │
│  ├── ✓ Form Login for the admin UI + JWT for the REST API                                  │
│  └── ✓ Multiple SecurityFilterChain beans with different rules                              │
│                                                                                              │
│  NEITHER — Use OAuth2 / OpenID Connect when:                                                │
│  ├── ✓ "Login with Google / GitHub / Microsoft" needed                                     │
│  ├── ✓ Enterprise SSO (Single Sign-On) with Keycloak, Okta, Auth0                         │
│  └── ✓ Delegated authorization (third-party app accesses user's data)                      │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---


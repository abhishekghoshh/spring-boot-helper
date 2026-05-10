### 16. HTTP Basic Authentication — Deep Dive, Internals, Security Concerns & Best Practices

---

#### 16.1 What Is HTTP Basic Authentication — Overview & How It Works

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  HTTP BASIC AUTHENTICATION — WHAT IT IS                                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  HTTP Basic Authentication is the SIMPLEST authentication mechanism defined                  │
│  in the HTTP standard. It was originally specified in RFC 2617 (1999) and                    │
│  updated by RFC 7617 (2015).                                                                │
│                                                                                              │
│  HOW IT WORKS:                                                                               │
│  1. Client sends credentials (username:password) in the HTTP header                         │
│  2. Credentials are Base64-encoded (NOT encrypted!)                                         │
│  3. Server decodes, validates against a user store, and responds                            │
│                                                                                              │
│                                                                                              │
│  ── THE AUTHORIZATION HEADER FORMAT ─────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Step 1: Combine username and password with a colon separator                │           │
│  │  "john:secret123"                                                             │           │
│  │                                                                               │           │
│  │  Step 2: Base64-encode the combined string                                   │           │
│  │  Base64("john:secret123") → "am9objpzZWNyZXQxMjM="                          │           │
│  │                                                                               │           │
│  │  Step 3: Send in the Authorization header with "Basic " prefix              │           │
│  │  Authorization: Basic am9objpzZWNyZXQxMjM=                                  │           │
│  │                                                                               │           │
│  │  ⚠️ CRITICAL: Base64 is ENCODING, not ENCRYPTION!                           │           │
│  │  Anyone who intercepts this header can trivially decode it:                  │           │
│  │  echo "am9objpzZWNyZXQxMjM=" | base64 --decode                              │           │
│  │  → john:secret123                                                             │           │
│  │  → Username and password are exposed in PLAIN TEXT!                          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── VISUAL: COMPLETE REQUEST-RESPONSE FLOW ──────────────────────────────────               │
│                                                                                              │
│  ┌─── FIRST REQUEST (No credentials) ────────────────────────────────────────┐             │
│  │                                                                            │             │
│  │  Client: GET /api/data HTTP/1.1                                            │             │
│  │          Host: api.example.com                                             │             │
│  │          (no Authorization header)                                         │             │
│  │                                                                            │             │
│  │  Server: HTTP/1.1 401 Unauthorized                                         │             │
│  │          WWW-Authenticate: Basic realm="MyApp"                             │             │
│  │          ↑                                                                 │             │
│  │          ↑ Server says: "I need Basic auth credentials"                    │             │
│  │          ↑ "realm" is a label for the protected area                       │             │
│  │                                                                            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│  ┌─── SECOND REQUEST (With credentials) ─────────────────────────────────────┐             │
│  │                                                                            │             │
│  │  Client: GET /api/data HTTP/1.1                                            │             │
│  │          Host: api.example.com                                             │             │
│  │          Authorization: Basic am9objpzZWNyZXQxMjM=                         │             │
│  │                                                                            │             │
│  │  Server: (decodes Base64 → "john:secret123")                               │             │
│  │          (validates against user store)                                     │             │
│  │          (credentials valid!)                                              │             │
│  │                                                                            │             │
│  │          HTTP/1.1 200 OK                                                   │             │
│  │          Content-Type: application/json                                    │             │
│  │          {"data": "protected resource"}                                    │             │
│  │                                                                            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│                                                                                              │
│  ── WHY BASIC AUTH IS STATELESS ─────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Basic Authentication is INHERENTLY STATELESS because:                       │           │
│  │                                                                               │           │
│  │  1. NO session is created on the server after authentication                │           │
│  │  2. NO cookie is sent back to the client                                    │           │
│  │  3. NO server-side state is maintained between requests                     │           │
│  │  4. The client sends credentials on EVERY request                           │           │
│  │  5. Each request is authenticated INDEPENDENTLY                             │           │
│  │                                                                               │           │
│  │  FORM LOGIN (stateful):                                                      │           │
│  │  POST /login (credentials) → server creates session → cookie sent          │           │
│  │  GET /api/data (cookie) → server looks up session → authenticated          │           │
│  │  GET /api/more (cookie) → server looks up session → authenticated          │           │
│  │  ★ Credentials sent ONCE, session reused for all subsequent requests       │           │
│  │                                                                               │           │
│  │  BASIC AUTH (stateless):                                                     │           │
│  │  GET /api/data (credentials in header) → server validates → authenticated  │           │
│  │  GET /api/more (credentials in header) → server validates → authenticated  │           │
│  │  GET /api/next (credentials in header) → server validates → authenticated  │           │
│  │  ★ Credentials sent EVERY TIME, no session, no cookies                     │           │
│  │                                                                               │           │
│  │  ★ This means: EVERY request requires a DB lookup to validate the          │           │
│  │    username and password. There is no session to "remember" the user.       │           │
│  │    The server FORGETS the user after each request completes.                │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.2 Why Basic Auth Is Still Used — And Where It Makes Sense

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHERE BASIC AUTH IS STILL USED TODAY                                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Despite being old and insecure over plain HTTP, Basic Auth is STILL widely                 │
│  used in specific scenarios:                                                                │
│                                                                                              │
│                                                                                              │
│  ── WHERE IT IS USED ────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. SERVICE-TO-SERVICE COMMUNICATION (Most common!)                          │           │
│  │     • Internal microservices calling each other                              │           │
│  │     • API integrations (third-party APIs)                                    │           │
│  │     • CI/CD pipelines calling deployment APIs                                │           │
│  │     • Monitoring systems calling health endpoints                            │           │
│  │     • Example: Jenkins calling your deploy API with Basic Auth              │           │
│  │                                                                               │           │
│  │  2. DEVELOPMENT & TESTING                                                    │           │
│  │     • Quick prototyping (no login pages needed)                              │           │
│  │     • Testing REST APIs with curl / Postman / HTTPie                        │           │
│  │     • Spring Actuator endpoints (health, metrics, info)                     │           │
│  │                                                                               │           │
│  │  3. SIMPLE INTERNAL TOOLS                                                    │           │
│  │     • Admin dashboards behind VPN/firewall                                   │           │
│  │     • Config servers (Spring Cloud Config Server uses Basic Auth)           │           │
│  │     • Database admin interfaces (some use Basic Auth)                        │           │
│  │                                                                               │           │
│  │  4. API GATEWAYS / REVERSE PROXIES                                           │           │
│  │     • Nginx / Apache protecting backend services                            │           │
│  │     • Simple gateway authentication before forwarding                       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHY IT IS NOT SUITED FOR USER-FACING FLOWS ──────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. NO LOGIN PAGE                                                             │           │
│  │     • Basic Auth uses the BROWSER's built-in login dialog (ugly popup)       │           │
│  │     • Cannot be styled, branded, or customized                               │           │
│  │     • No "forgot password" link, no "register" button                       │           │
│  │     • Terrible user experience                                               │           │
│  │                                                                               │           │
│  │  2. NO LOGOUT MECHANISM                                                      │           │
│  │     • Browser caches Basic Auth credentials for the entire session          │           │
│  │     • There is NO standard way to "log out" — the browser keeps sending     │           │
│  │       the Authorization header until you close ALL browser tabs/windows     │           │
│  │     • User must close the entire browser to "log out"                       │           │
│  │                                                                               │           │
│  │  3. CREDENTIALS SENT ON EVERY REQUEST                                        │           │
│  │     • Password travels over the wire on EVERY single HTTP request           │           │
│  │     • Increases attack surface — more chances to intercept                  │           │
│  │     • With form login, password is sent ONCE, then a session cookie is used │           │
│  │                                                                               │           │
│  │  4. NO CSRF PROTECTION INTERACTION                                           │           │
│  │     • Browser auto-sends Basic Auth on every request to the same domain    │           │
│  │     • But CSRF is less relevant for APIs (see 16.8)                         │           │
│  │                                                                               │           │
│  │  5. DB LOOKUP ON EVERY REQUEST                                               │           │
│  │     • Since there's no session, the server must validate credentials        │           │
│  │       against the database on EVERY request                                 │           │
│  │     • password hashing (BCrypt) is CPU-intensive by design                  │           │
│  │     • 1000 requests/sec = 1000 BCrypt verifications/sec = HIGH CPU load!   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHY IT IS SUITED FOR SERVICE-TO-SERVICE ─────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ✅ Services don't need a login page or logout mechanism                     │           │
│  │  ✅ Credentials are stored in configuration (env vars, secrets manager)     │           │
│  │  ✅ Communication is always over HTTPS (TLS) within infrastructure          │           │
│  │  ✅ Simpler than JWT — no token generation, no expiry handling              │           │
│  │  ✅ No session management needed — pure stateless                           │           │
│  │  ✅ Easy to implement in any HTTP client (curl, RestTemplate, WebClient)    │           │
│  │  ✅ Request volume is typically lower than user-facing endpoints            │           │
│  │                                                                               │           │
│  │  ┌─────────────────────────────────────────────────────────────────────┐    │           │
│  │  │  Service A ────── HTTPS + Basic Auth ──────→ Service B             │    │           │
│  │  │                                                                     │    │           │
│  │  │  Authorization: Basic c2VydmljZUE6czNjcjN0                         │    │           │
│  │  │  (serviceA:s3cr3t)                                                  │    │           │
│  │  │                                                                     │    │           │
│  │  │  ✅ HTTPS encrypts the entire header — credentials are safe        │    │           │
│  │  │  ✅ Both services are internal — no browser involved               │    │           │
│  │  │  ✅ Credentials stored in Kubernetes Secrets / Vault               │    │           │
│  │  └─────────────────────────────────────────────────────────────────────┘    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── DISADVANTAGES SUMMARY ───────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ❌ Credentials sent on EVERY request (increased exposure)                   │           │
│  │  ❌ Base64 is NOT encryption — trivially decodable                           │           │
│  │  ❌ No built-in token expiry — credentials valid until password changed     │           │
│  │  ❌ No refresh mechanism — if password is compromised, must change it       │           │
│  │  ❌ DB + BCrypt verification on EVERY request = expensive                    │           │
│  │  ❌ Browser caches credentials — no proper logout                           │           │
│  │  ❌ No built-in multi-factor authentication support                         │           │
│  │  ❌ No login page customization                                              │           │
│  │  ❌ Not suited for browser-based user-facing applications                   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.3 ★ Why Basic Auth Is Insecure Over HTTP — HTTPS Is Mandatory

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHY BASIC AUTH IS INSECURE WITHOUT HTTPS                                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Base64 encoding is NOT encryption. It is a reversible encoding scheme                      │
│  that anyone can decode. Without HTTPS (TLS), Basic Auth credentials                        │
│  travel in PLAIN TEXT over the network.                                                      │
│                                                                                              │
│                                                                                              │
│  ── WITHOUT HTTPS (HTTP) — INSECURE! ────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Client ─────────── HTTP (plain text) ───────────→ Server                    │           │
│  │                                                                               │           │
│  │  GET /api/data HTTP/1.1                                                      │           │
│  │  Authorization: Basic am9objpzZWNyZXQxMjM=                                  │           │
│  │                                                                               │           │
│  │       ↑↑↑ VISIBLE TO ANYONE ON THE NETWORK! ↑↑↑                             │           │
│  │                                                                               │           │
│  │  ┌─── ATTACKER (Man-in-the-Middle) ───────────────────────────┐             │           │
│  │  │                                                             │             │           │
│  │  │  Attacker on same network (coffee shop WiFi, corporate     │             │           │
│  │  │  network, ISP) can see the ENTIRE HTTP request:            │             │           │
│  │  │                                                             │             │           │
│  │  │  1. Capture packet with Wireshark / tcpdump                │             │           │
│  │  │  2. Read: Authorization: Basic am9objpzZWNyZXQxMjM=       │             │           │
│  │  │  3. Decode: echo "am9objpzZWNyZXQxMjM=" | base64 -d       │             │           │
│  │  │     → john:secret123                                       │             │           │
│  │  │  4. ★ Attacker now has the username AND password!          │             │           │
│  │  │  5. Can impersonate the user, access all their resources   │             │           │
│  │  │                                                             │             │           │
│  │  └─────────────────────────────────────────────────────────────┘             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WITH HTTPS (TLS) — SECURE ───────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Client ────── HTTPS (TLS encrypted tunnel) ──────→ Server                   │           │
│  │                                                                               │           │
│  │  ┌────────────────── TLS ENCRYPTED ──────────────────────┐                  │           │
│  │  │  GET /api/data HTTP/1.1                                │                  │           │
│  │  │  Authorization: Basic am9objpzZWNyZXQxMjM=             │                  │           │
│  │  │  ↑↑↑ ENCRYPTED — looks like random bytes to attacker   │                  │           │
│  │  └────────────────────────────────────────────────────────┘                  │           │
│  │                                                                               │           │
│  │  ┌─── ATTACKER ────────────────────────────────────────────┐                │           │
│  │  │                                                          │                │           │
│  │  │  Sees: 17 03 03 00 4a 8b 2c f1 9a...                   │                │           │
│  │  │  → Random encrypted bytes                               │                │           │
│  │  │  → CANNOT decode the Authorization header               │                │           │
│  │  │  → CANNOT see the URL, headers, or body                │                │           │
│  │  │  → Attack FAILS ✅                                      │                │           │
│  │  │                                                          │                │           │
│  │  └──────────────────────────────────────────────────────────┘                │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ★ HTTPS encrypts the ENTIRE HTTP message:                                  │           │
│  │    • URL path (/api/data)                                                    │           │
│  │    • ALL headers (including Authorization: Basic ...)                        │           │
│  │    • Request body                                                            │           │
│  │    • Response body                                                           │           │
│  │    Only the hostname (via SNI) and IP address are visible to the attacker.  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── RFC 7617 — MANDATE ──────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  RFC 7617 (The 'Basic' HTTP Authentication Scheme) explicitly states:        │           │
│  │                                                                               │           │
│  │  "This scheme is not considered to be a secure method of user               │           │
│  │   authentication unless used in conjunction with some external              │           │
│  │   secure system such as TLS (Transport Layer Security)"                     │           │
│  │                                                                               │           │
│  │  → ALWAYS use HTTPS when using Basic Auth!                                  │           │
│  │  → In Spring Boot, enforce HTTPS or use it behind a TLS-terminating        │           │
│  │    load balancer / reverse proxy (Nginx, AWS ALB, etc.)                     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.4 ★ Why Credentials Must Be in the Header Only (RFC 7617) — Security Concerns

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHY CREDENTIALS MUST ONLY BE IN THE AUTHORIZATION HEADER                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  RFC 7617 specifies that Basic Auth credentials MUST be sent in the                         │
│  HTTP Authorization header. Here's WHY passing credentials in other                         │
│  places is a security risk:                                                                  │
│                                                                                              │
│                                                                                              │
│  ── ✅ CORRECT: Authorization Header ────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  GET /api/data HTTP/1.1                                                      │           │
│  │  Authorization: Basic am9objpzZWNyZXQxMjM=                                  │           │
│  │                                                                               │           │
│  │  ✅ Headers are encrypted by HTTPS (TLS)                                     │           │
│  │  ✅ NOT logged by web servers by default                                     │           │
│  │  ✅ NOT cached by browsers in URL history                                    │           │
│  │  ✅ NOT stored in bookmarks                                                  │           │
│  │  ✅ NOT visible in Referer headers                                           │           │
│  │  ✅ NOT stored in proxy/CDN access logs                                      │           │
│  │  ✅ Standard-compliant (RFC 7617)                                            │           │
│  │  ✅ Spring Security's BasicAuthenticationFilter reads from this header      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── ❌ DANGEROUS: Credentials in URL (Query Parameter) ──────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  GET /api/data?username=john&password=secret123 HTTP/1.1                     │           │
│  │                                                                               │           │
│  │  ❌ URLs are logged in web server access logs (Nginx, Apache, Tomcat)        │           │
│  │     access.log: "GET /api/data?username=john&password=secret123 200"        │           │
│  │     → Password permanently stored in log files!                             │           │
│  │                                                                               │           │
│  │  ❌ URLs are stored in browser history                                       │           │
│  │     → Anyone with access to the browser can see the password               │           │
│  │                                                                               │           │
│  │  ❌ URLs appear in the Referer header when navigating to another site        │           │
│  │     Referer: https://api.example.com/api/data?password=secret123            │           │
│  │     → Third-party sites receive your password!                              │           │
│  │                                                                               │           │
│  │  ❌ URLs are cached by proxies, CDNs, and reverse proxies                    │           │
│  │     → Password stored in multiple intermediate systems                      │           │
│  │                                                                               │           │
│  │  ❌ URLs are bookmarked by users                                             │           │
│  │     → Password saved in bookmarks file, possibly synced to cloud            │           │
│  │                                                                               │           │
│  │  ❌ URLs may be visible even with HTTPS (in browser address bar, etc.)       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── ❌ RISKY: Credentials in Request Body ───────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  POST /api/data HTTP/1.1                                                     │           │
│  │  Content-Type: application/json                                              │           │
│  │  {"username": "john", "password": "secret123"}                               │           │
│  │                                                                               │           │
│  │  ⚠️ This is NOT Basic Auth — this is a custom login mechanism              │           │
│  │  ⚠️ Less dangerous than URL, but still has issues:                          │           │
│  │                                                                               │           │
│  │  ❌ Request body may be logged by application logging frameworks             │           │
│  │     (Log4j, Logback with request logging enabled)                           │           │
│  │  ❌ Request body may be logged by API gateways / WAFs                       │           │
│  │  ❌ Forces POST for every request (cannot use GET for read operations)      │           │
│  │  ❌ Not standard-compliant — every API implements it differently            │           │
│  │  ❌ No standard 401 challenge-response mechanism                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── ❌ DANGEROUS: Credentials in URL Path ───────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  GET /api/john/secret123/data HTTP/1.1                                       │           │
│  │                                                                               │           │
│  │  ❌ Same problems as query parameters (logged, cached, bookmarked)           │           │
│  │  ❌ Even worse: harder to identify and filter out of logs                    │           │
│  │  ❌ Violates REST principles (credentials are not a resource)               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── SUMMARY ─────────────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────┬────────────────────────┬────────────────────────┐             │
│  │  Location                │  Security Level        │  Risk                  │             │
│  ├──────────────────────────┼────────────────────────┼────────────────────────┤             │
│  │  Authorization Header    │  ✅ SAFE (with HTTPS)   │  Low                   │             │
│  │  Request Body (JSON)     │  ⚠️ Acceptable          │  Medium (logging risk) │             │
│  │  Query Parameter (?pwd=) │  ❌ DANGEROUS            │  HIGH (logged/cached)  │             │
│  │  URL Path (/user/pwd/)   │  ❌ DANGEROUS            │  HIGH (logged/cached)  │             │
│  │  Cookie                  │  ⚠️ Possible but wrong  │  Medium (CSRF risk)    │             │
│  └──────────────────────────┴────────────────────────┴────────────────────────┘             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.5 BasicAuthenticationFilter — Auto-Configuration & Internal Code

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  BasicAuthenticationFilter — IS IT AUTO-CONFIGURED? WHAT DOES IT DO?                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── IS BASIC AUTH AUTO-CONFIGURED? ──────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  YES! When you add spring-boot-starter-security:                             │           │
│  │                                                                               │           │
│  │  SpringBootWebSecurityConfiguration creates the default SecurityFilterChain: │           │
│  │                                                                               │           │
│  │  http                                                                         │           │
│  │      .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())       │           │
│  │      .formLogin(withDefaults())       // ← adds UsernamePasswordAuthFilter  │           │
│  │      .httpBasic(withDefaults());      // ← adds BasicAuthenticationFilter   │           │
│  │                                                                               │           │
│  │  ★ BOTH form login AND basic auth are enabled by default!                   │           │
│  │  ★ BasicAuthenticationFilter is ALREADY in the filter chain!                │           │
│  │  ★ You can use EITHER mechanism — Spring Security tries both.              │           │
│  │                                                                               │           │
│  │  If you define your OWN SecurityFilterChain, Basic Auth is NOT added        │           │
│  │  unless you explicitly call .httpBasic(withDefaults())                       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHAT IS BasicAuthenticationFilter? ──────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  BasicAuthenticationFilter is a Spring Security filter that:                 │           │
│  │                                                                               │           │
│  │  1. Checks if the request has an Authorization header starting with "Basic "│           │
│  │  2. If YES → extracts and decodes the Base64 credentials                   │           │
│  │  3. Creates a UsernamePasswordAuthenticationToken (unauthenticated)         │           │
│  │  4. Passes it to AuthenticationManager → ProviderManager                    │           │
│  │     → DaoAuthenticationProvider → UserDetailsService → DB lookup            │           │
│  │  5. If authentication succeeds → sets SecurityContext in                    │           │
│  │     SecurityContextHolder                                                    │           │
│  │  6. If authentication fails → returns 401 Unauthorized with                 │           │
│  │     WWW-Authenticate: Basic realm="..."                                     │           │
│  │  7. If NO Authorization header → SKIPS (passes to next filter)             │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  Class hierarchy:                                                             │           │
│  │  BasicAuthenticationFilter                                                    │           │
│  │    extends OncePerRequestFilter                                              │           │
│  │      extends GenericFilterBean                                               │           │
│  │        implements Filter                                                      │           │
│  │                                                                               │           │
│  │  ★ OncePerRequestFilter ensures the filter runs EXACTLY ONCE per request    │           │
│  │    (even if the request is forwarded internally)                             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHERE IN THE FILTER CHAIN? ──────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Security Filter Chain order (relevant filters):                             │           │
│  │                                                                               │           │
│  │  1. SecurityContextHolderFilter         ← load existing SecurityContext     │           │
│  │  2. CsrfFilter                          ← CSRF validation                  │           │
│  │  3. LogoutFilter                        ← handle logout                     │           │
│  │  4. UsernamePasswordAuthenticationFilter← form login (POST /login)          │           │
│  │  5. BasicAuthenticationFilter           ← ★ BASIC AUTH (this filter!)      │           │
│  │  6. AnonymousAuthenticationFilter       ← set anonymous token if no auth   │           │
│  │  7. ExceptionTranslationFilter          ← catch auth exceptions            │           │
│  │  8. AuthorizationFilter                 ← check access rules               │           │
│  │                                                                               │           │
│  │  ★ BasicAuthenticationFilter runs AFTER UsernamePasswordAuthenticationFilter│           │
│  │  ★ If form login already authenticated the user (set SecurityContext),      │           │
│  │    BasicAuthenticationFilter is SKIPPED (SecurityContext already set)       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.6 ★ Complete Internal Flow — How Spring Security Handles Basic Auth

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE INTERNAL FLOW — BasicAuthenticationFilter → DaoAuthenticationProvider             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  This is the EXACT same authentication pipeline as Form Login                               │
│  (see Section 13), but triggered by BasicAuthenticationFilter instead of                    │
│  UsernamePasswordAuthenticationFilter.                                                       │
│                                                                                              │
│                                                                                              │
│  ── STEP-BY-STEP INTERNAL CODE FLOW ────────────────────────────────────────               │
│                                                                                              │
│  Request:                                                                                    │
│  GET /api/orders HTTP/1.1                                                                    │
│  Authorization: Basic am9objpzZWNyZXQxMjM=                                                 │
│                                                                                              │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  STEP 1: BasicAuthenticationFilter.doFilterInternal()                        │           │
│  │                                                                               │           │
│  │  // Check if Authorization header exists and starts with "Basic "           │           │
│  │  String header = request.getHeader("Authorization");                        │           │
│  │  // header = "Basic am9objpzZWNyZXQxMjM="                                  │           │
│  │                                                                               │           │
│  │  if (header == null || !header.startsWith("Basic ")) {                      │           │
│  │      // No Basic Auth header → SKIP this filter, continue chain            │           │
│  │      chain.doFilter(request, response);                                     │           │
│  │      return;                                                                 │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  // ★ Also checks: is the user ALREADY authenticated?                      │           │
│  │  // If SecurityContextHolder already has an Authentication object           │           │
│  │  // (e.g., from a session), skip re-authentication                          │           │
│  │  Authentication existingAuth = SecurityContextHolder.getContext()            │           │
│  │      .getAuthentication();                                                   │           │
│  │  if (existingAuth != null && existingAuth.isAuthenticated()) {              │           │
│  │      chain.doFilter(request, response);                                     │           │
│  │      return; // Already authenticated — no need to re-validate             │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  STEP 2: Decode Base64 Credentials                                           │           │
│  │                                                                               │           │
│  │  // Extract the Base64 token after "Basic "                                 │           │
│  │  String base64Credentials = header.substring(6);                            │           │
│  │  // base64Credentials = "am9objpzZWNyZXQxMjM="                              │           │
│  │                                                                               │           │
│  │  byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);       │           │
│  │  String credentials = new String(decodedBytes, StandardCharsets.UTF_8);     │           │
│  │  // credentials = "john:secret123"                                           │           │
│  │                                                                               │           │
│  │  // Split on the FIRST colon (password may contain colons!)                 │           │
│  │  int colonIndex = credentials.indexOf(":");                                  │           │
│  │  String username = credentials.substring(0, colonIndex);    // "john"       │           │
│  │  String password = credentials.substring(colonIndex + 1);   // "secret123" │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  STEP 3: Create Unauthenticated Token                                        │           │
│  │                                                                               │           │
│  │  UsernamePasswordAuthenticationToken authRequest =                           │           │
│  │      UsernamePasswordAuthenticationToken.unauthenticated(                    │           │
│  │          username,    // "john"                                               │           │
│  │          password     // "secret123"                                         │           │
│  │      );                                                                       │           │
│  │  // authRequest = {                                                          │           │
│  │  //   principal: "john",                                                     │           │
│  │  //   credentials: "secret123",                                              │           │
│  │  //   authorities: [],                                                       │           │
│  │  //   authenticated: false  ← NOT YET VERIFIED!                             │           │
│  │  // }                                                                         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  STEP 4: Delegate to AuthenticationManager → ProviderManager                │           │
│  │                                                                               │           │
│  │  // ★ EXACTLY the same as Form Login from here!                             │           │
│  │  Authentication result = this.authenticationManager.authenticate(authRequest);│           │
│  │                                                                               │           │
│  │  // AuthenticationManager (ProviderManager) iterates through providers:     │           │
│  │  // Provider 1: DaoAuthenticationProvider → supports                        │           │
│  │  //             UsernamePasswordAuthenticationToken? → YES!                 │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  STEP 5: DaoAuthenticationProvider.authenticate()                            │           │
│  │                                                                               │           │
│  │  // 5a. Load user from database                                              │           │
│  │  UserDetails userDetails = userDetailsService.loadUserByUsername("john");    │           │
│  │  // → Executes: SELECT * FROM users WHERE username = 'john'                 │           │
│  │  // → Returns: UserDetails { username: "john", password: "$2a$10$...",      │           │
│  │  //            authorities: [ROLE_USER], enabled: true }                    │           │
│  │                                                                               │           │
│  │  // 5b. Compare passwords using PasswordEncoder                             │           │
│  │  // passwordEncoder.matches("secret123", "$2a$10$...")                       │           │
│  │  // → BCrypt hashes "secret123" and compares with stored hash              │           │
│  │  // → Match! ✅                                                              │           │
│  │                                                                               │           │
│  │  // 5c. Additional checks:                                                   │           │
│  │  // → Is account enabled? (isEnabled())                                     │           │
│  │  // → Is account non-locked? (isAccountNonLocked())                         │           │
│  │  // → Is account non-expired? (isAccountNonExpired())                       │           │
│  │  // → Are credentials non-expired? (isCredentialsNonExpired())              │           │
│  │                                                                               │           │
│  │  // 5d. Create AUTHENTICATED token (with authorities)                       │           │
│  │  return UsernamePasswordAuthenticationToken.authenticated(                   │           │
│  │      userDetails,          // principal (UserDetails object)                 │           │
│  │      null,                 // credentials ERASED for security               │           │
│  │      userDetails.getAuthorities()  // [ROLE_USER]                           │           │
│  │  );                                                                           │           │
│  │  // result = {                                                               │           │
│  │  //   principal: UserDetails { username: "john" },                           │           │
│  │  //   credentials: null,    ← ERASED!                                       │           │
│  │  //   authorities: [ROLE_USER],                                              │           │
│  │  //   authenticated: true   ← NOW VERIFIED!                                │           │
│  │  // }                                                                         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  STEP 6: BasicAuthenticationFilter — On Success                              │           │
│  │                                                                               │           │
│  │  // Save the authenticated SecurityContext                                   │           │
│  │  SecurityContext context = SecurityContextHolder.createEmptyContext();       │           │
│  │  context.setAuthentication(result);                                          │           │
│  │  SecurityContextHolder.setContext(context);                                  │           │
│  │  // ★ SecurityContext is now in ThreadLocal — available for entire request  │           │
│  │                                                                               │           │
│  │  // If STATELESS → context is NOT saved to HttpSession                     │           │
│  │  // If IF_REQUIRED → context IS saved to HttpSession (creates session)     │           │
│  │  this.securityContextRepository.saveContext(context, request, response);     │           │
│  │                                                                               │           │
│  │  // Call success handler (by default, just continues the filter chain)      │           │
│  │  this.authenticationSuccessHandler.onAuthenticationSuccess(                  │           │
│  │      request, response, result                                              │           │
│  │  );                                                                           │           │
│  │                                                                               │           │
│  │  // Continue to next filter in the chain                                    │           │
│  │  chain.doFilter(request, response);                                         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  STEP 7: AuthorizationFilter — Same as Form Login!                           │           │
│  │                                                                               │           │
│  │  // ★ EXACTLY the same AuthorizationFilter as form-based login!             │           │
│  │  // Reads Authentication from SecurityContextHolder                         │           │
│  │  // Checks URL-level access rules (.authorizeHttpRequests())                │           │
│  │  // Then @PreAuthorize checks at method level (Phase 2)                     │           │
│  │                                                                               │           │
│  │  // The Authorization flow is IDENTICAL — the AuthorizationFilter           │           │
│  │  // doesn't know or care HOW the user was authenticated.                    │           │
│  │  // It only reads the SecurityContext from SecurityContextHolder.            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  ON FAILURE: BasicAuthenticationFilter — When Credentials Are Wrong          │           │
│  │                                                                               │           │
│  │  // If DaoAuthenticationProvider throws BadCredentialsException:            │           │
│  │                                                                               │           │
│  │  SecurityContextHolder.clearContext();                                        │           │
│  │  // Clear any existing context                                               │           │
│  │                                                                               │           │
│  │  this.authenticationEntryPoint.commence(request, response, exception);      │           │
│  │  // Default BasicAuthenticationEntryPoint sends:                             │           │
│  │  //   HTTP/1.1 401 Unauthorized                                              │           │
│  │  //   WWW-Authenticate: Basic realm="Realm"                                 │           │
│  │  //                                                                          │           │
│  │  // ★ Does NOT redirect to /login like form login!                          │           │
│  │  // ★ Returns 401 with WWW-Authenticate header                              │           │
│  │  // ★ Browser shows its built-in login popup dialog                         │           │
│  │  // ★ API clients (curl, Postman) get 401 JSON/text response               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.7 Complete Flow Diagram — Basic Auth vs Form Login Side-by-Side

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  BASIC AUTH vs FORM LOGIN — SIDE-BY-SIDE COMPARISON                                          │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ┌─── FORM LOGIN FLOW ───────────────────────────────────────────────────────┐             │
│  │                                                                            │             │
│  │  POST /login (username=john&password=secret123)                            │             │
│  │       │                                                                    │             │
│  │       ▼                                                                    │             │
│  │  UsernamePasswordAuthenticationFilter                                      │             │
│  │  → extracts from request body (form data)                                 │             │
│  │       │                                                                    │             │
│  │       ▼                                                                    │             │
│  │  AuthenticationManager → DaoAuthenticationProvider                         │             │
│  │  → UserDetailsService.loadUserByUsername("john")                           │             │
│  │  → BCrypt.matches("secret123", storedHash)                                │             │
│  │       │                                                                    │             │
│  │       ▼                                                                    │             │
│  │  SecurityContext { john, ROLE_USER, authenticated=true }                   │             │
│  │  → Saved in HttpSession (JSESSIONID cookie sent to client)               │             │
│  │  → Redirect to / (default success URL)                                    │             │
│  │                                                                            │             │
│  │  SUBSEQUENT REQUESTS:                                                      │             │
│  │  Cookie: JSESSIONID=ABC123                                                 │             │
│  │  → SecurityContextHolderFilter loads from session                         │             │
│  │  → NO re-authentication! NO DB lookup!                                    │             │
│  │                                                                            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│  ┌─── BASIC AUTH FLOW ───────────────────────────────────────────────────────┐             │
│  │                                                                            │             │
│  │  GET /api/orders                                                           │             │
│  │  Authorization: Basic am9objpzZWNyZXQxMjM=                                │             │
│  │       │                                                                    │             │
│  │       ▼                                                                    │             │
│  │  BasicAuthenticationFilter                                                 │             │
│  │  → extracts from Authorization header (Base64 decode)                     │             │
│  │       │                                                                    │             │
│  │       ▼                                                                    │             │
│  │  AuthenticationManager → DaoAuthenticationProvider  ← ★ SAME!            │             │
│  │  → UserDetailsService.loadUserByUsername("john")    ← ★ SAME!            │             │
│  │  → BCrypt.matches("secret123", storedHash)          ← ★ SAME!            │             │
│  │       │                                                                    │             │
│  │       ▼                                                                    │             │
│  │  SecurityContext { john, ROLE_USER, authenticated=true }                   │             │
│  │  → Set in SecurityContextHolder (ThreadLocal only!)                       │             │
│  │  → With STATELESS: NOT saved in HttpSession, NO cookie sent              │             │
│  │  → Returns 200 OK with data                                               │             │
│  │                                                                            │             │
│  │  SUBSEQUENT REQUESTS:                                                      │             │
│  │  Authorization: Basic am9objpzZWNyZXQxMjM=   ← SENT AGAIN!              │             │
│  │  → BasicAuthenticationFilter decodes AGAIN                                │             │
│  │  → DaoAuthenticationProvider validates AGAIN (DB + BCrypt)                │             │
│  │  → ★ FULL RE-AUTHENTICATION ON EVERY REQUEST!                            │             │
│  │                                                                            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│                                                                                              │
│  ── COMPARISON TABLE ────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────┬──────────────────────────┬──────────────────────────┐        │
│  │  Aspect                  │  Form Login              │  Basic Auth              │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  Filter                  │  UsernamePasswordAuth    │  BasicAuthentication     │        │
│  │                          │  enticationFilter         │  Filter                  │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  Credentials from        │  Request body (form)     │  Authorization header    │        │
│  │                          │  POST /login              │  (Base64 encoded)        │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  Auth Provider           │  DaoAuthenticationProvider (SAME!)                  │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  UserDetailsService      │  Same DB lookup (SAME!)                             │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  Session                 │  ✅ Created after login   │  ❌ Not needed (STATELESS)│        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  Cookie                  │  JSESSIONID              │  None                    │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  Creds sent              │  Once (at login)         │  Every request           │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  DB lookup               │  Once (at login)         │  Every request           │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  On failure              │  Redirect to             │  401 Unauthorized +      │        │
│  │                          │  /login?error             │  WWW-Authenticate header │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  On success              │  Redirect to             │  Continue filter chain   │        │
│  │                          │  successUrl              │  → return response data  │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  AuthorizationFilter     │  SAME! (reads from SecurityContextHolder)           │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  @PreAuthorize           │  SAME! (works identically)                          │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  Login page              │  ✅ Custom HTML page      │  ❌ Browser popup only    │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  Logout                  │  ✅ POST /logout           │  ❌ No standard logout    │        │
│  ├──────────────────────────┼──────────────────────────┼──────────────────────────┤        │
│  │  Best for                │  Web apps with UI        │  APIs, service-to-service│        │
│  └──────────────────────────┴──────────────────────────┴──────────────────────────┘        │
│                                                                                              │
│                                                                                              │
│  ── KEY INSIGHT ─────────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The ONLY difference between Form Login and Basic Auth is:                   │           │
│  │  WHERE the credentials come from (form body vs Authorization header)        │           │
│  │  and WHICH filter extracts them.                                             │           │
│  │                                                                               │           │
│  │  After extraction, the ENTIRE authentication pipeline is IDENTICAL:          │           │
│  │  AuthenticationManager → ProviderManager → DaoAuthenticationProvider        │           │
│  │  → UserDetailsService → PasswordEncoder → SecurityContextHolder             │           │
│  │  → AuthorizationFilter → @PreAuthorize → Controller                         │           │
│  │                                                                               │           │
│  │  ★ The AuthorizationFilter and @PreAuthorize don't know or care            │           │
│  │    whether the user authenticated via form login or basic auth.             │           │
│  │    They only read the SecurityContext from SecurityContextHolder.            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.8 ★ Session, CSRF & SessionCreationPolicy for Basic Auth — What to Configure

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SESSION, CSRF & SESSION CREATION POLICY FOR BASIC AUTH                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Three important questions when configuring Basic Auth for REST APIs:                       │
│  1. Do we need sessions?                                                                    │
│  2. Should we disable CSRF?                                                                 │
│  3. Which SessionCreationPolicy to use?                                                     │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  QUESTION 1: DO WE NEED SESSIONS WITH BASIC AUTH?                                          │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  SHORT ANSWER: NO! You should disable sessions for Basic Auth APIs.         │           │
│  │                                                                               │           │
│  │  WHY:                                                                         │           │
│  │  • Basic Auth is inherently stateless — credentials come on every request   │           │
│  │  • Sessions are NOT needed because there's nothing to "remember"            │           │
│  │  • If you DON'T disable sessions, Spring Security will:                     │           │
│  │    1. Authenticate via BasicAuthenticationFilter                            │           │
│  │    2. Create a session AND store SecurityContext in it (DEFAULT behavior)   │           │
│  │    3. On next request: SecurityContextHolderFilter loads from session       │           │
│  │    4. BasicAuthenticationFilter sees existing auth → SKIPS!                 │           │
│  │    → Session is created unnecessarily, wasting memory                       │           │
│  │                                                                               │           │
│  │  ⚠️ DEFAULT GOTCHA:                                                         │           │
│  │  Without configuring SessionCreationPolicy, Spring Security uses            │           │
│  │  IF_REQUIRED, which WILL create a session after Basic Auth succeeds!        │           │
│  │  This means the first request creates a session, and subsequent requests    │           │
│  │  use the session (skipping Basic Auth validation entirely!).               │           │
│  │  → This defeats the purpose of Basic Auth being stateless                  │           │
│  │  → Set STATELESS to enforce re-authentication on every request             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  QUESTION 2: SHOULD WE DISABLE CSRF?                                                       │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  SHORT ANSWER: YES! Disable CSRF for stateless Basic Auth APIs.             │           │
│  │                                                                               │           │
│  │  WHY CSRF EXISTS:                                                             │           │
│  │  CSRF (Cross-Site Request Forgery) protects against attacks where a          │           │
│  │  malicious website tricks your browser into making requests to a site       │           │
│  │  where you're already logged in (via session cookie).                        │           │
│  │                                                                               │           │
│  │  Example CSRF attack:                                                         │           │
│  │  1. User logs into bank.com → gets session cookie                           │           │
│  │  2. User visits evil.com (in another tab)                                   │           │
│  │  3. evil.com has: <form action="bank.com/transfer" method="POST">           │           │
│  │  4. Browser auto-sends bank.com session cookie with the request!            │           │
│  │  5. Bank processes transfer — user didn't intend this!                      │           │
│  │                                                                               │           │
│  │  WHY IT'S NOT NEEDED FOR BASIC AUTH APIs:                                    │           │
│  │  • CSRF only works when the browser AUTO-SENDS credentials (cookies)        │           │
│  │  • With STATELESS + Basic Auth: there are NO cookies to auto-send!          │           │
│  │  • The client must MANUALLY add the Authorization header on every request   │           │
│  │  • A malicious website CANNOT add custom headers to cross-origin requests   │           │
│  │    (Same-Origin Policy prevents this)                                        │           │
│  │  • Therefore: CSRF attack is NOT possible without cookies                   │           │
│  │                                                                               │           │
│  │  ⚠️ EXCEPTION: If you're using Basic Auth WITH sessions                    │           │
│  │  (SessionCreationPolicy.IF_REQUIRED), then a JSESSIONID cookie IS created, │           │
│  │  and CSRF protection IS needed! But this configuration is not recommended.  │           │
│  │                                                                               │           │
│  │  ⚠️ NOTE ON BROWSER BASIC AUTH:                                              │           │
│  │  Browsers DO cache and auto-send Basic Auth credentials to the same domain.│           │
│  │  This means browser-based Basic Auth IS vulnerable to CSRF-like attacks!   │           │
│  │  Another reason Basic Auth is not suited for browser-based user flows.     │           │
│  │  For pure API clients (RestTemplate, WebClient, curl) → safe to disable.  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  QUESTION 3: WHICH SESSION CREATION POLICY?                                                 │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  USE: SessionCreationPolicy.STATELESS                                        │           │
│  │                                                                               │           │
│  │  This ensures:                                                                │           │
│  │  ✅ No HttpSession is ever created                                            │           │
│  │  ✅ No JSESSIONID cookie is ever sent                                         │           │
│  │  ✅ SecurityContext is NOT stored in session                                  │           │
│  │  ✅ NullSecurityContextRepository is used (no session lookup)                │           │
│  │  ✅ BasicAuthenticationFilter re-validates on EVERY request                   │           │
│  │  ✅ Truly stateless — any server can handle any request                      │           │
│  │                                                                               │           │
│  │  What happens internally with STATELESS:                                     │           │
│  │  1. SecurityContextHolderFilter → empty context (no session to read)        │           │
│  │  2. BasicAuthenticationFilter → decodes, validates, sets SecurityContext    │           │
│  │  3. SecurityContext saved to ThreadLocal ONLY (not HttpSession)             │           │
│  │  4. AuthorizationFilter → checks access rules                               │           │
│  │  5. Controller processes request                                             │           │
│  │  6. SecurityContextHolder.clearContext() → all auth info gone               │           │
│  │  7. Next request → start over from step 1                                   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── VISUAL: STATELESS vs DEFAULT (IF_REQUIRED) WITH BASIC AUTH ──────────────               │
│                                                                                              │
│  ┌─── WITH STATELESS (Recommended) ──────────────────────────────────────────┐             │
│  │                                                                            │             │
│  │  Request 1: GET /api/data + Authorization: Basic ...                       │             │
│  │  → BasicAuthFilter → decode → DB lookup → BCrypt → authenticate ✅        │             │
│  │  → SecurityContext in ThreadLocal → AuthorizationFilter → Controller      │             │
│  │  → Response → clearContext() → ❌ NO session created                       │             │
│  │                                                                            │             │
│  │  Request 2: GET /api/more + Authorization: Basic ...                       │             │
│  │  → BasicAuthFilter → decode → DB lookup → BCrypt → authenticate ✅        │             │
│  │  → ★ FULL re-authentication (credentials verified against DB again)       │             │
│  │                                                                            │             │
│  │  Request 3: GET /api/next + Authorization: Basic ...                       │             │
│  │  → Same process again — every request is independent                      │             │
│  │                                                                            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│  ┌─── WITH IF_REQUIRED (Default — NOT recommended for APIs) ─────────────────┐             │
│  │                                                                            │             │
│  │  Request 1: GET /api/data + Authorization: Basic ...                       │             │
│  │  → BasicAuthFilter → decode → DB lookup → BCrypt → authenticate ✅        │             │
│  │  → ★ Session CREATED! SecurityContext saved in HttpSession                │             │
│  │  → Set-Cookie: JSESSIONID=XYZ789                                          │             │
│  │                                                                            │             │
│  │  Request 2: GET /api/more + Authorization: Basic ...                       │             │
│  │  Cookie: JSESSIONID=XYZ789 (browser auto-sends!)                          │             │
│  │  → SecurityContextHolderFilter loads from session → already authenticated │             │
│  │  → BasicAuthFilter sees existing auth → SKIPS! (no DB lookup!)            │             │
│  │  → ⚠️ Credentials in header are IGNORED! Session takes precedence!       │             │
│  │                                                                            │             │
│  │  ⚠️ This is MISLEADING — looks stateless but actually uses sessions!     │             │
│  │  ⚠️ Session can be hijacked via JSESSIONID cookie                        │             │
│  │  ⚠️ CSRF becomes a concern again                                         │             │
│  │                                                                            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.9 Complete Code — Basic Auth Configuration for REST API

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig — Basic Auth for REST API (RECOMMENDED configuration)
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enable @PreAuthorize for method-level security
public class BasicAuthSecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ═══════════════════════════════════════════════════════════════
            //  1. STATELESS — No sessions, no cookies
            // ═══════════════════════════════════════════════════════════════
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // ★ No HttpSession created
                // ★ No JSESSIONID cookie
                // ★ Every request must include Authorization header
            )

            // ═══════════════════════════════════════════════════════════════
            //  2. DISABLE CSRF — Not needed for stateless APIs
            // ═══════════════════════════════════════════════════════════════
            .csrf(csrf -> csrf.disable())
            // ★ CSRF protection is session-based
            // ★ No session = no CSRF vulnerability (for non-browser clients)
            // ★ Safe to disable for API-only applications

            // ═══════════════════════════════════════════════════════════════
            //  3. AUTHORIZATION RULES
            // ═══════════════════════════════════════════════════════════════
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            // ═══════════════════════════════════════════════════════════════
            //  4. ENABLE BASIC AUTH
            // ═══════════════════════════════════════════════════════════════
            .httpBasic(basic -> basic
                .realmName("MyApp API")
                // ↑ Realm name shown in browser popup and 401 response:
                //   WWW-Authenticate: Basic realm="MyApp API"

                .authenticationEntryPoint(new BasicAuthenticationEntryPoint() {{
                    setRealmName("MyApp API");
                }})
                // ↑ Custom entry point for 401 responses (optional)
            )

            // ═══════════════════════════════════════════════════════════════
            //  5. DISABLE FORM LOGIN — Not needed for APIs
            // ═══════════════════════════════════════════════════════════════
            .formLogin(AbstractHttpConfigurer::disable);
            // ★ No login page, no UsernamePasswordAuthenticationFilter

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // ── Option 1: In-memory users (for development/testing) ────────────
        UserDetails user = User.withUsername("user")
            .password(passwordEncoder().encode("password123"))
            .roles("USER")
            .build();

        UserDetails admin = User.withUsername("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(user, admin);

        // ── Option 2: Database-backed (for production) ─────────────────────
        // Return a custom UserDetailsService that loads from DB
        // (same as form login — same interface, same implementation!)
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Controller — Same as any other secured controller
//  @PreAuthorize works EXACTLY the same as with form login!
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/public/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
        // ★ No authentication needed (permitAll)
    }

    @GetMapping("/data")
    public Map<String, String> getData() {
        return Map.of("message", "Authenticated access!");
        // ★ Requires any authenticated user
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/users/{id}")
    public Map<String, String> deleteUser(@PathVariable Long id) {
        return Map.of("deleted", id.toString());
        // ★ Requires ROLE_ADMIN (Phase 2 — method-level)
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        return Map.of(
            "username", authentication.getName(),
            "authorities", authentication.getAuthorities()
        );
        // ★ Access the authenticated user's details
    }
}
```

```properties
# ═══════════════════════════════════════════════════════════════════════════════
#  application.properties — Basic Auth with default user
#  (If you don't define a UserDetailsService bean, Spring Boot uses these)
# ═══════════════════════════════════════════════════════════════════════════════

# Default username and password (auto-configured by Spring Boot)
spring.security.user.name=admin
spring.security.user.password=secret123
spring.security.user.roles=ADMIN

# Force HTTPS (recommended for Basic Auth!)
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12

# Or if behind a reverse proxy (Nginx, AWS ALB):
server.forward-headers-strategy=framework
```

---

#### 16.10 Testing Basic Auth — curl, HTTPie & Integration Tests

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  TESTING BASIC AUTH — ALL METHODS                                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── 1. TESTING WITH curl ────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  # Using -u flag (curl encodes to Base64 automatically)                     │           │
│  │  curl -u admin:secret123 http://localhost:8080/api/data                      │           │
│  │                                                                               │           │
│  │  # Equivalent: manual Authorization header                                  │           │
│  │  curl -H "Authorization: Basic YWRtaW46c2VjcmV0MTIz" \                     │           │
│  │       http://localhost:8080/api/data                                         │           │
│  │                                                                               │           │
│  │  # Test without credentials (expect 401)                                    │           │
│  │  curl -v http://localhost:8080/api/data                                      │           │
│  │  # Response: HTTP/1.1 401 Unauthorized                                      │           │
│  │  #           WWW-Authenticate: Basic realm="MyApp API"                      │           │
│  │                                                                               │           │
│  │  # Test with wrong credentials (expect 401)                                 │           │
│  │  curl -u admin:wrongpassword http://localhost:8080/api/data                  │           │
│  │  # Response: HTTP/1.1 401 Unauthorized                                      │           │
│  │                                                                               │           │
│  │  # Test admin endpoint with non-admin user (expect 403)                     │           │
│  │  curl -u user:password123 http://localhost:8080/api/admin/users/1            │           │
│  │  # Response: HTTP/1.1 403 Forbidden                                         │           │
│  │                                                                               │           │
│  │  # Verbose output to see all headers                                        │           │
│  │  curl -v -u admin:secret123 http://localhost:8080/api/data                   │           │
│  │  # Shows: > Authorization: Basic YWRtaW46c2VjcmV0MTIz                      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── 2. TESTING WITH HTTPie ──────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  # HTTPie uses -a flag for basic auth                                       │           │
│  │  http -a admin:secret123 GET http://localhost:8080/api/data                  │           │
│  │                                                                               │           │
│  │  # Without credentials                                                      │           │
│  │  http GET http://localhost:8080/api/data                                     │           │
│  │  # Response: 401 Unauthorized                                               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── 3. TESTING WITH RestTemplate (Service-to-Service) ───────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  // Service A calling Service B with Basic Auth                              │           │
│  │  RestTemplate restTemplate = new RestTemplate();                             │           │
│  │                                                                               │           │
│  │  HttpHeaders headers = new HttpHeaders();                                    │           │
│  │  headers.setBasicAuth("serviceA", "s3cr3t");                                │           │
│  │  // ↑ Automatically encodes to Base64 and sets Authorization header         │           │
│  │                                                                               │           │
│  │  HttpEntity<Void> entity = new HttpEntity<>(headers);                       │           │
│  │                                                                               │           │
│  │  ResponseEntity<String> response = restTemplate.exchange(                   │           │
│  │      "http://service-b:8080/api/data",                                      │           │
│  │      HttpMethod.GET,                                                         │           │
│  │      entity,                                                                 │           │
│  │      String.class                                                            │           │
│  │  );                                                                           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── 4. SPRING BOOT INTEGRATION TEST ────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)│           │
│  │  class BasicAuthIntegrationTest {                                            │           │
│  │                                                                               │           │
│  │      @Autowired                                                               │           │
│  │      TestRestTemplate restTemplate;                                          │           │
│  │                                                                               │           │
│  │      @Test                                                                    │           │
│  │      void shouldReturn401WithoutCredentials() {                              │           │
│  │          ResponseEntity<String> response = restTemplate                      │           │
│  │              .getForEntity("/api/data", String.class);                       │           │
│  │          assertThat(response.getStatusCode())                                │           │
│  │              .isEqualTo(HttpStatus.UNAUTHORIZED);                            │           │
│  │      }                                                                        │           │
│  │                                                                               │           │
│  │      @Test                                                                    │           │
│  │      void shouldReturn200WithValidCredentials() {                            │           │
│  │          ResponseEntity<String> response = restTemplate                      │           │
│  │              .withBasicAuth("admin", "secret123")                            │           │
│  │              .getForEntity("/api/data", String.class);                       │           │
│  │          assertThat(response.getStatusCode())                                │           │
│  │              .isEqualTo(HttpStatus.OK);                                      │           │
│  │      }                                                                        │           │
│  │                                                                               │           │
│  │      @Test                                                                    │           │
│  │      void shouldReturn403ForNonAdminAccessingAdminEndpoint() {              │           │
│  │          ResponseEntity<String> response = restTemplate                      │           │
│  │              .withBasicAuth("user", "password123")                           │           │
│  │              .exchange("/api/admin/users/1", HttpMethod.DELETE,              │           │
│  │                  null, String.class);                                        │           │
│  │          assertThat(response.getStatusCode())                                │           │
│  │              .isEqualTo(HttpStatus.FORBIDDEN);                               │           │
│  │      }                                                                        │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── 5. MockMvc TEST (Unit Test Style) ───────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  @WebMvcTest(ApiController.class)                                            │           │
│  │  @ImportAutoConfiguration(SecurityAutoConfiguration.class)                   │           │
│  │  class BasicAuthMockMvcTest {                                                │           │
│  │                                                                               │           │
│  │      @Autowired                                                               │           │
│  │      MockMvc mockMvc;                                                        │           │
│  │                                                                               │           │
│  │      @Test                                                                    │           │
│  │      void shouldReturn401WithoutAuth() throws Exception {                   │           │
│  │          mockMvc.perform(get("/api/data"))                                   │           │
│  │              .andExpect(status().isUnauthorized())                           │           │
│  │              .andExpect(header().exists("WWW-Authenticate"));               │           │
│  │      }                                                                        │           │
│  │                                                                               │           │
│  │      @Test                                                                    │           │
│  │      @WithMockUser(username = "admin", roles = "ADMIN")                     │           │
│  │      void shouldReturn200WithMockUser() throws Exception {                  │           │
│  │          mockMvc.perform(get("/api/data"))                                   │           │
│  │              .andExpect(status().isOk());                                    │           │
│  │      }                                                                        │           │
│  │                                                                               │           │
│  │      @Test                                                                    │           │
│  │      void shouldReturn200WithHttpBasic() throws Exception {                 │           │
│  │          mockMvc.perform(get("/api/data")                                    │           │
│  │              .with(httpBasic("admin", "secret123")))                         │           │
│  │              .andExpect(status().isOk());                                    │           │
│  │      }                                                                        │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.11 Complete Sequence Diagram — Basic Auth Request from Header to Controller

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  COMPLETE SEQUENCE DIAGRAM — BASIC AUTH REQUEST FLOW                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  SessionCreationPolicy: STATELESS                                                            │
│  Request: GET /api/orders                                                                    │
│  Header: Authorization: Basic am9objpzZWNyZXQxMjM=                                         │
│                                                                                              │
│                                                                                              │
│  Client            SecurityCtx    Basic Auth     DaoAuth      Authorization  Controller     │
│  (curl/service)    HolderFilter   Filter         Provider     Filter                        │
│  ────────────      ────────────   ──────────     ────────     ─────────────  ──────────      │
│       │                 │              │              │              │            │          │
│       │  GET /api/orders│              │              │              │            │          │
│       │  Authorization: │              │              │              │            │          │
│       │  Basic am9obj...│              │              │              │            │          │
│       │────────────────>│              │              │              │            │          │
│       │                 │              │              │              │            │          │
│       │                 │ STATELESS:   │              │              │            │          │
│       │                 │ No session   │              │              │            │          │
│       │                 │ lookup.      │              │              │            │          │
│       │                 │ Empty ctx.   │              │              │            │          │
│       │                 │─────────────>│              │              │            │          │
│       │                 │              │              │              │            │          │
│       │                 │              │ Has "Basic " │              │            │          │
│       │                 │              │ header? YES! │              │            │          │
│       │                 │              │              │              │            │          │
│       │                 │              │ Base64 decode│              │            │          │
│       │                 │              │ → john:      │              │            │          │
│       │                 │              │   secret123  │              │            │          │
│       │                 │              │              │              │            │          │
│       │                 │              │ Create unauth│              │            │          │
│       │                 │              │ token:       │              │            │          │
│       │                 │              │ {john,       │              │            │          │
│       │                 │              │  secret123,  │              │            │          │
│       │                 │              │  auth=false} │              │            │          │
│       │                 │              │              │              │            │          │
│       │                 │              │─────────────>│              │            │          │
│       │                 │              │              │              │            │          │
│       │                 │              │              │ loadUserBy   │            │          │
│       │                 │              │              │ Username     │            │          │
│       │                 │              │              │ ("john")     │            │          │
│       │                 │              │              │ → DB query   │            │          │
│       │                 │              │              │              │            │          │
│       │                 │              │              │ BCrypt.      │            │          │
│       │                 │              │              │ matches(     │            │          │
│       │                 │              │              │ "secret123", │            │          │
│       │                 │              │              │  "$2a$10$...│            │          │
│       │                 │              │              │ ") → ✅      │            │          │
│       │                 │              │              │              │            │          │
│       │                 │              │              │ Create auth  │            │          │
│       │                 │              │              │ token:       │            │          │
│       │                 │              │              │ {john,       │            │          │
│       │                 │              │              │  null,       │            │          │
│       │                 │              │              │  ROLE_USER,  │            │          │
│       │                 │              │              │  auth=true}  │            │          │
│       │                 │              │<─────────────│              │            │          │
│       │                 │              │              │              │            │          │
│       │                 │              │ Set Security │              │            │          │
│       │                 │              │ Context in   │              │            │          │
│       │                 │              │ ThreadLocal  │              │            │          │
│       │                 │              │ (NOT in      │              │            │          │
│       │                 │              │  session!)   │              │            │          │
│       │                 │              │──────────────┼─────────────>│            │          │
│       │                 │              │              │              │            │          │
│       │                 │              │              │              │ Check:     │          │
│       │                 │              │              │              │ /api/**    │          │
│       │                 │              │              │              │ → auth()   │          │
│       │                 │              │              │              │ User auth? │          │
│       │                 │              │              │              │ ✅ YES     │          │
│       │                 │              │              │              │───────────>│          │
│       │                 │              │              │              │            │          │
│       │                 │              │              │              │   Process  │          │
│       │                 │              │              │              │   request  │          │
│       │                 │              │              │              │   Return   │          │
│       │                 │              │              │              │   orders   │          │
│       │                 │              │              │              │            │          │
│       │  200 OK         │              │              │              │            │          │
│       │  [{orders}]     │              │              │              │            │          │
│       │<─────────────────────────────────────────────────────────────────────────│          │
│       │                 │              │              │              │            │          │
│       │                 │ finally:     │              │              │            │          │
│       │                 │ clearContext()│              │              │            │          │
│       │                 │ ❌ No session │              │              │            │          │
│       │                 │ ❌ No cookie  │              │              │            │          │
│       │                 │              │              │              │            │          │
│                                                                                              │
│  ── NEXT REQUEST: Start Over (No Session!) ──────────────────────────────────               │
│                                                                                              │
│  Same flow repeats: decode → DB lookup → BCrypt → authenticate → authorize                 │
│  ★ Every request is independently authenticated from scratch!                               │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 16.12 Summary — HTTP Basic Authentication Key Takeaways

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SUMMARY — SECTION 16 KEY TAKEAWAYS                                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. WHAT IS BASIC AUTH                                                       │           │
│  │     • Simplest HTTP authentication: Base64(username:password) in header     │           │
│  │     • Defined by RFC 7617                                                    │           │
│  │     • Base64 is ENCODING, not ENCRYPTION — trivially reversible            │           │
│  │     • Inherently STATELESS — credentials sent on every request             │           │
│  │                                                                               │           │
│  │  2. WHY HTTPS IS MANDATORY                                                  │           │
│  │     • Without TLS, credentials are in plain text on the network            │           │
│  │     • Anyone on the network can decode Base64 and steal credentials        │           │
│  │     • RFC 7617 explicitly mandates TLS                                      │           │
│  │                                                                               │           │
│  │  3. WHY ONLY IN THE HEADER (RFC 7617)                                       │           │
│  │     • URL query params: logged by servers, cached, in browser history       │           │
│  │     • URL path: same logging/caching risks                                  │           │
│  │     • Request body: possible but non-standard, can be logged               │           │
│  │     • Authorization header: encrypted by TLS, not logged by default        │           │
│  │                                                                               │           │
│  │  4. SPRING SECURITY INTERNALS                                                │           │
│  │     • BasicAuthenticationFilter extracts from Authorization header          │           │
│  │     • Delegates to same pipeline as form login:                             │           │
│  │       AuthenticationManager → DaoAuthenticationProvider → UserDetailsService│           │
│  │     • SecurityContext stored in ThreadLocal (same as form login)            │           │
│  │     • AuthorizationFilter checks access rules (same as form login)         │           │
│  │     • @PreAuthorize works identically (same as form login)                 │           │
│  │                                                                               │           │
│  │  5. AUTO-CONFIGURATION                                                       │           │
│  │     • YES! .httpBasic(withDefaults()) is in the default SecurityFilterChain │           │
│  │     • BasicAuthenticationFilter is auto-registered                          │           │
│  │     • When you define your own SecurityFilterChain, you must re-add it     │           │
│  │                                                                               │           │
│  │  6. SESSION, CSRF & POLICY FOR BASIC AUTH APIs                              │           │
│  │     • Session: NOT needed → use SessionCreationPolicy.STATELESS            │           │
│  │     • CSRF: NOT needed → disable for stateless APIs                        │           │
│  │     • Without STATELESS, Spring creates sessions by default (gotcha!)      │           │
│  │                                                                               │           │
│  │  7. WHY NOT FOR USER-FACING APPS                                             │           │
│  │     • No customizable login page (ugly browser popup only)                  │           │
│  │     • No logout mechanism (browser caches credentials)                      │           │
│  │     • Credentials on every request = more exposure                          │           │
│  │     • DB + BCrypt on every request = expensive                              │           │
│  │     • Best suited for: service-to-service, APIs, development/testing       │           │
│  │                                                                               │           │
│  │  8. KEY INSIGHT                                                              │           │
│  │     • Only difference from form login: WHERE credentials come from          │           │
│  │       (header vs form body) and WHICH filter extracts them                  │           │
│  │     • After extraction, the ENTIRE pipeline is identical:                   │           │
│  │       DaoAuthenticationProvider → SecurityContextHolder →                   │           │
│  │       AuthorizationFilter → @PreAuthorize → Controller                     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```


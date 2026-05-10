### 21. Role-Based Authorization (RBAC) in Spring Security

---

#### 21.1 ★ What is Role-Based Authorization?

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ROLE-BASED ACCESS CONTROL (RBAC) — CONCEPT                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── DEFINITION ───────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Role-Based Authorization is a security model where access to resources     │           │
│  │  is determined by the ROLE(s) assigned to the authenticated user, rather    │           │
│  │  than the identity of the user directly.                                     │           │
│  │                                                                               │           │
│  │  ★ Authentication answers: "WHO are you?"  → User logs in                  │           │
│  │  ★ Authorization  answers: "WHAT can you do?" → Based on role              │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── CORE CONCEPTS ────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌─────────────┬──────────────────────────────────────────────────────────┐ │           │
│  │  │  Concept     │  Description                                             │ │           │
│  │  ├─────────────┼──────────────────────────────────────────────────────────┤ │           │
│  │  │  User        │  An authenticated principal (person or system)           │ │           │
│  │  │  Role        │  A named group of permissions (e.g., ADMIN, USER, MOD)  │ │           │
│  │  │  Resource    │  A URL, endpoint, or method being protected              │ │           │
│  │  │  Permission  │  A specific action allowed (read, write, delete)         │ │           │
│  │  └─────────────┴──────────────────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── EXAMPLE ROLE HIERARCHY ───────────────────────────────────────           │           │
│  │                                                                               │           │
│  │       ┌─────────────────────────────────────────────────────┐               │           │
│  │       │                     ADMIN                            │               │           │
│  │       │  /admin/**  /users/**  /reports/**  /settings/**    │               │           │
│  │       │  DELETE, WRITE, READ everything                      │               │           │
│  │       └────────────────────────┬────────────────────────────┘               │           │
│  │                                │ includes                                    │           │
│  │       ┌────────────────────────▼────────────────────────────┐               │           │
│  │       │                   MODERATOR                           │               │           │
│  │       │  /posts/**  /comments/**  /users/list               │               │           │
│  │       │  READ + WRITE (no DELETE users)                      │               │           │
│  │       └────────────────────────┬────────────────────────────┘               │           │
│  │                                │ includes                                    │           │
│  │       ┌────────────────────────▼────────────────────────────┐               │           │
│  │       │                     USER                              │               │           │
│  │       │  /profile/**  /posts/read  /api/public/**            │               │           │
│  │       │  READ own data only                                   │               │           │
│  │       └─────────────────────────────────────────────────────┘               │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── ROLE vs AUTHORITY IN SPRING SECURITY ────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌─────────────────────────────┬───────────────────────────────────────────┐ │          │
│  │  │  Role                        │  Authority (GrantedAuthority)             │ │          │
│  │  ├─────────────────────────────┼───────────────────────────────────────────┤ │          │
│  │  │  Prefixed with "ROLE_"       │  Any string (no prefix required)         │ │          │
│  │  │  e.g., "ROLE_ADMIN"          │  e.g., "SCOPE_read", "user:delete"       │ │          │
│  │  │  hasRole("ADMIN") checks for │  hasAuthority("ROLE_ADMIN") requires     │ │          │
│  │  │  "ROLE_ADMIN" automatically  │  the full string including prefix        │ │          │
│  │  │  @PreAuthorize("hasRole      │  @PreAuthorize("hasAuthority             │ │          │
│  │  │  ('ADMIN')")                 │  ('ROLE_ADMIN')")                         │ │          │
│  │  └─────────────────────────────┴───────────────────────────────────────────┘ │          │
│  │                                                                               │           │
│  │  ★ KEY: hasRole("ADMIN") automatically prepends "ROLE_" prefix!             │           │
│  │    So hasRole("ADMIN") is equivalent to hasAuthority("ROLE_ADMIN")          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.2 ★ Where Does Authorization Happen? — The Two Layers

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ TWO PLACES TO ENFORCE ROLE-BASED AUTHORIZATION IN SPRING SECURITY                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│   HTTP Request                                                                               │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐        │
│  │                     LAYER 1: SECURITY FILTER CHAIN                               │        │
│  │                     (SecurityConfig — URL-level authorization)                   │        │
│  │                                                                                   │        │
│  │  .authorizeHttpRequests(auth -> auth                                             │        │
│  │      .requestMatchers("/admin/**").hasRole("ADMIN")     ← ★ LAYER 1            │        │
│  │      .requestMatchers("/api/**").hasAnyRole("USER","ADMIN")                     │        │
│  │      .anyRequest().authenticated()                                               │        │
│  │  )                                                                               │        │
│  │                                                                                   │        │
│  │  ★ Checks: Does the user's role match the URL pattern?                          │        │
│  │  ★ If NO → 403 Forbidden (never reaches controller)                            │        │
│  │  ★ If YES → request passes to the Controller                                   │        │
│  └────────────────────────────────┬────────────────────────────────────────────────┘        │
│                                   │ passes (role matches URL)                               │
│                                   ▼                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐        │
│  │                     LAYER 2: METHOD-LEVEL SECURITY                               │        │
│  │                     (Controller / Service — method-level authorization)          │        │
│  │                                                                                   │        │
│  │  @PreAuthorize("hasRole('ADMIN')")        ← ★ LAYER 2 (BEFORE method runs)     │        │
│  │  public String deleteUser(Long id) { ... }                                       │        │
│  │                                                                                   │        │
│  │  @PostAuthorize("returnObject.owner == authentication.name")  ← AFTER method    │        │
│  │  public Document getDocument(Long id) { ... }                                    │        │
│  │                                                                                   │        │
│  │  ★ @PreAuthorize: Checks BEFORE the method runs                               │        │
│  │  ★ @PostAuthorize: Checks AFTER the method runs (on the return value)          │        │
│  │  ★ If check fails → 403 Forbidden                                              │        │
│  └─────────────────────────────────────────────────────────────────────────────────┘        │
│                                   │                                                          │
│                                   ▼                                                          │
│                            HTTP Response                                                     │
│                                                                                              │
│  ★ YOU CONTROL WHICH LAYER (or both!) to use:                                              │
│    • LAYER 1 alone: Good for coarse-grained access (entire URL paths)                      │
│    • LAYER 2 alone: Good for fine-grained access (specific methods/logic)                  │
│    • BOTH together: Defense in depth (recommended!)                                        │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.3 ★ Key Annotations — @EnableWebSecurity and @EnableMethodSecurity

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ KEY ANNOTATIONS FOR SPRING SECURITY AUTHORIZATION                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── @EnableWebSecurity ────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  @Configuration                                                               │           │
│  │  @EnableWebSecurity   ← ★★★                                                  │           │
│  │  public class SecurityConfig { ... }                                          │           │
│  │                                                                               │           │
│  │  • Activates Spring Security's web security support                          │           │
│  │  • Registers Spring Security filter chain in the servlet context             │           │
│  │  • Enables you to define custom SecurityFilterChain beans                   │           │
│  │  • Required for HttpSecurity-based configuration                             │           │
│  │  • Without it: Spring Security auto-configuration may still activate,       │           │
│  │    but you CANNOT customize it with @Bean SecurityFilterChain               │           │
│  │  • In Spring Boot: auto-applied by spring-boot-autoconfigure                │           │
│  │    if spring-security is on classpath — but explicit is better practice     │           │
│  │  • Controls: URL-level security (Layer 1)                                   │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── @EnableMethodSecurity(prePostEnabled = true) ──────────────────          │           │
│  │                                                                               │           │
│  │  @Configuration                                                               │           │
│  │  @EnableWebSecurity                                                           │           │
│  │  @EnableMethodSecurity(prePostEnabled = true)   ← ★★★                       │           │
│  │  public class SecurityConfig { ... }                                          │           │
│  │                                                                               │           │
│  │  • Enables method-level security using Spring Expression Language (SpEL)    │           │
│  │  • prePostEnabled = true → unlocks:                                          │           │
│  │    → @PreAuthorize("...")   checked BEFORE the method runs                  │           │
│  │    → @PostAuthorize("...")  checked AFTER the method returns                │           │
│  │    → @PreFilter("...")      filters method input collections                │           │
│  │    → @PostFilter("...")     filters method return collections               │           │
│  │  • Controls: Method-level security (Layer 2)                                │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────────┐   │           │
│  │  │  Additional attributes of @EnableMethodSecurity:                      │   │           │
│  │  │                                                                        │   │           │
│  │  │  @EnableMethodSecurity(                                                │   │           │
│  │  │    prePostEnabled  = true,  // @PreAuthorize, @PostAuthorize (default) │   │           │
│  │  │    securedEnabled  = true,  // @Secured("ROLE_ADMIN")                 │   │           │
│  │  │    jsr250Enabled   = true   // @RolesAllowed("ADMIN") from JSR-250    │   │           │
│  │  │  )                                                                     │   │           │
│  │  │                                                                        │   │           │
│  │  │  ★ prePostEnabled=true is the modern approach (SpEL expressions)      │   │           │
│  │  │  ★ securedEnabled: older approach, less flexible                       │   │           │
│  │  │  ★ jsr250Enabled: for Jakarta EE interoperability                      │   │           │
│  │  └──────────────────────────────────────────────────────────────────────┘   │           │
│  │                                                                               │           │
│  │  ★ IMPORTANT: @EnableMethodSecurity replaced @EnableGlobalMethodSecurity    │           │
│  │    in Spring Security 5.6+. Use @EnableMethodSecurity in new projects!      │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────────┐   │           │
│  │  │  Old (deprecated):                                                     │   │           │
│  │  │  @EnableGlobalMethodSecurity(prePostEnabled = true)  ← before 5.6     │   │           │
│  │  │                                                                        │   │           │
│  │  │  New (use this):                                                        │   │           │
│  │  │  @EnableMethodSecurity(prePostEnabled = true)         ← 5.6+          │   │           │
│  │  └──────────────────────────────────────────────────────────────────────┘   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.4 ★ Complete Architecture Diagram — RBAC in Spring Boot

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE RBAC ARCHITECTURE IN SPRING BOOT                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│   HTTP Request: GET /admin/users                                                             │
│   User: alice (ROLE_ADMIN)                                                                   │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  1. UsernamePasswordAuthenticationFilter (or JWT Filter)                    │             │
│  │     → Authenticates user → creates Authentication object                   │             │
│  │     → SecurityContextHolder.getContext().setAuthentication(auth)            │             │
│  │                                                                              │             │
│  │     Authentication = {                                                       │             │
│  │       principal: "alice",                                                    │             │
│  │       authorities: [ROLE_ADMIN, ROLE_USER],                                 │             │
│  │       authenticated: true                                                    │             │
│  │     }                                                                        │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  2. AuthorizationFilter (Layer 1 — URL Security)                            │             │
│  │     → Reads SecurityContext → gets Authentication                          │             │
│  │     → Checks request URL: GET /admin/users                                  │             │
│  │     → Matches rule: .requestMatchers("/admin/**").hasRole("ADMIN")          │             │
│  │     → Does alice have ROLE_ADMIN? YES ✅                                    │             │
│  │     → Pass through                                                          │             │
│  │                                                                              │             │
│  │     ❌ If bob (ROLE_USER) requests /admin/users:                            │             │
│  │        → Does bob have ROLE_ADMIN? NO ❌                                    │             │
│  │        → 403 Forbidden (request NEVER reaches controller)                   │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  3. DispatcherServlet → AdminController.listUsers()                         │             │
│  │                                                                              │             │
│  │     @PreAuthorize("hasRole('ADMIN')")   ← Layer 2 check BEFORE method body │             │
│  │     public List<User> listUsers() {                                          │             │
│  │         return userRepository.findAll();                                     │             │
│  │     }                                                                        │             │
│  │                                                                              │             │
│  │     → Spring's MethodSecurityInterceptor intercepts the call                │             │
│  │     → Evaluates SpEL: hasRole('ADMIN') for alice                           │             │
│  │     → alice has ROLE_ADMIN → YES ✅                                         │             │
│  │     → Method body executes                                                  │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│               200 OK { list of users }                                                       │
│                                                                                              │
│                                                                                              │
│  ★ STORAGE OF ROLES:                                                                        │
│                                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │                                                                              │             │
│  │  Option 1: In-Memory (testing)      Option 2: Database (production)        │             │
│  │  ──────────────────────────────     ────────────────────────────────────   │             │
│  │  User.withUsername("alice")         users table + roles table              │             │
│  │    .roles("ADMIN")                  UserDetailsService loads from DB       │             │
│  │    .build()                          user.getAuthorities() → roles         │             │
│  │                                                                              │             │
│  │  Option 3: JWT (stateless)                                                  │             │
│  │  ─────────────────────────                                                  │             │
│  │  { "sub":"alice",                                                           │             │
│  │    "roles":["ROLE_ADMIN","ROLE_USER"] }                                     │             │
│  │  JwtAuthenticationConverter extracts roles                                  │             │
│  │                                                                              │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.5 ★ pom.xml — Dependencies

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Spring Boot with Role-Based Authorization                           -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- ── Spring Security ───────────────────────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- ── Web ──────────────────────────────────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- ── Spring Data JPA (optional, for loading roles from DB) ────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- ── H2 (for demo; replace with MySQL/PostgreSQL in production) ─────── -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

</dependencies>
```

---

#### 21.6 ★ Layer 1 — URL-Level Security in SecurityConfig (hasRole / hasAnyRole)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ LAYER 1 — SecurityConfig URL-Level Authorization                                        │
│    Applies BEFORE the request reaches the Controller                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Methods available inside .authorizeHttpRequests():                          │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────┬────────────────────────────────────┐  │           │
│  │  │  Method                           │  Description                       │  │           │
│  │  ├──────────────────────────────────┼────────────────────────────────────┤  │           │
│  │  │  .permitAll()                     │  Allow everyone (no auth needed)   │  │           │
│  │  │  .denyAll()                       │  Block everyone                    │  │           │
│  │  │  .authenticated()                 │  Any authenticated user            │  │           │
│  │  │  .hasRole("ADMIN")                │  Must have ROLE_ADMIN              │  │           │
│  │  │  .hasAnyRole("ADMIN","MOD")       │  Must have ROLE_ADMIN OR ROLE_MOD  │  │           │
│  │  │  .hasAuthority("ROLE_ADMIN")      │  Same as hasRole but full string   │  │           │
│  │  │  .hasAnyAuthority("X","Y")        │  Same as hasAnyRole but full str   │  │           │
│  │  │  .access(AuthorizationManager)    │  Custom authorization logic        │  │           │
│  │  └──────────────────────────────────┴────────────────────────────────────┘  │           │
│  │                                                                               │           │
│  │  ★ Rules are evaluated TOP-TO-BOTTOM. First match wins!                     │           │
│  │  ★ Put most specific patterns FIRST, .anyRequest() LAST.                   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**SecurityConfig.java — Layer 1 (URL-Level Authorization)**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig.java — LAYER 1: URL-Level Role-Based Authorization
//
//  ★ @EnableWebSecurity    → Activates Spring Security web support
//  ★ @EnableMethodSecurity → Enables @PreAuthorize / @PostAuthorize
//
//  ★ .hasRole("ADMIN")     → checks for authority "ROLE_ADMIN"
//  ★ .hasAnyRole("A","B")  → checks for "ROLE_A" OR "ROLE_B"
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity                              // ★ Activate Spring Security
@EnableMethodSecurity(prePostEnabled = true)    // ★ Enable @PreAuthorize / @PostAuthorize
public class SecurityConfig {

    // ── SecurityFilterChain — URL-Level Authorization (LAYER 1) ──────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth

                // ── Public endpoints (no authentication needed) ──────────────
                .requestMatchers("/", "/public/**", "/api/public/**").permitAll()

                // ── Admin-only endpoints ────────────────────────────────────
                //    hasRole("ADMIN") checks for authority "ROLE_ADMIN"
                .requestMatchers("/admin/**").hasRole("ADMIN")              // ★ LAYER 1

                // ── Moderator OR Admin can access ─────────────────────────
                //    hasAnyRole checks if user has ANY of the listed roles
                .requestMatchers("/moderate/**").hasAnyRole("ADMIN", "MODERATOR")   // ★ LAYER 1

                // ── User dashboard — any authenticated user ────────────────
                .requestMatchers("/dashboard/**").authenticated()

                // ── API — requires either USER or ADMIN role ──────────────
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")       // ★ LAYER 1

                // ── Reports — ADMIN only, using hasAuthority (full string) ─
                .requestMatchers("/reports/**").hasAuthority("ROLE_ADMIN")  // ★ same as hasRole("ADMIN")

                // ── Actuator — allow only ADMIN ────────────────────────────
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // ── Everything else — must be authenticated ────────────────
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())    // Form login
            .httpBasic(Customizer.withDefaults())    // HTTP Basic (for API testing)
            .csrf(csrf -> csrf.disable());           // Disable CSRF for REST APIs

        return http.build();
    }

    // ── In-Memory Users with Roles (for demo) ────────────────────────────────
    //    ★ In production, replace with a UserDetailsService backed by a database
    @Bean
    public UserDetailsService userDetailsService() {

        // ── ADMIN user ────────────────────────────────────────────────────────
        //    .roles("ADMIN") automatically creates authority "ROLE_ADMIN"
        UserDetails admin = User.withUsername("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN")                          // → authority: ROLE_ADMIN
            .build();

        // ── MODERATOR user ────────────────────────────────────────────────────
        UserDetails moderator = User.withUsername("mod")
            .password(passwordEncoder().encode("mod123"))
            .roles("MODERATOR", "USER")              // → ROLE_MODERATOR, ROLE_USER
            .build();

        // ── Regular USER ──────────────────────────────────────────────────────
        UserDetails user = User.withUsername("user")
            .password(passwordEncoder().encode("user123"))
            .roles("USER")                           // → authority: ROLE_USER
            .build();

        return new InMemoryUserDetailsManager(admin, moderator, user);
    }

    // ── Password Encoder ──────────────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Required Imports — SecurityConfig**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Imports for SecurityConfig.java
// ═══════════════════════════════════════════════════════════════════════════════

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
```

---

#### 21.7 ★ Layer 2 — Method-Level Security (@PreAuthorize and @PostAuthorize)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ LAYER 2 — METHOD-LEVEL AUTHORIZATION                                                    │
│    @PreAuthorize  → runs BEFORE method body                                                  │
│    @PostAuthorize → runs AFTER method body (on the return value)                             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── @PreAuthorize ─────────────────────────────────────────────────          │           │
│  │                                                                               │           │
│  │  • Checked BEFORE the method executes                                        │           │
│  │  • If check FAILS → method NEVER runs → 403 AccessDeniedException           │           │
│  │  • Use when: you know the role from the request parameters/path             │           │
│  │  • Supports full SpEL (Spring Expression Language)                           │           │
│  │                                                                               │           │
│  │  Common SpEL expressions:                                                    │           │
│  │  ┌────────────────────────────────────────────────────────────────────────┐ │           │
│  │  │  hasRole('ADMIN')                     → user has ROLE_ADMIN           │ │           │
│  │  │  hasAnyRole('ADMIN','MOD')             → user has ROLE_ADMIN or MOD   │ │           │
│  │  │  hasAuthority('ROLE_ADMIN')            → same as hasRole, full string  │ │           │
│  │  │  isAuthenticated()                     → any authenticated user        │ │           │
│  │  │  isAnonymous()                         → not logged in                 │ │           │
│  │  │  #username == authentication.name      → parameter matches logged user │ │           │
│  │  │  authentication.name == 'admin'        → specific user                 │ │           │
│  │  │  hasRole('ADMIN') or #id == principal.id → admin OR own resource      │ │           │
│  │  └────────────────────────────────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── @PostAuthorize ────────────────────────────────────────────────          │           │
│  │                                                                               │           │
│  │  • Checked AFTER the method executes (on the return value)                  │           │
│  │  • The method ALWAYS runs; only the RETURN of the result is blocked         │           │
│  │  • Special variable: returnObject (the return value of the method)         │           │
│  │  • Use when: you need to check a field on the returned object              │           │
│  │  • ★ WARNING: The method body ALWAYS runs! Use @PreAuthorize for          │           │
│  │    preventing DB writes/deletes. @PostAuthorize is for reads only.         │           │
│  │                                                                               │           │
│  │  Common SpEL expressions:                                                    │           │
│  │  ┌────────────────────────────────────────────────────────────────────────┐ │           │
│  │  │  returnObject.owner == authentication.name  → caller owns the resource │ │           │
│  │  │  returnObject.username == principal.username → user's own data         │ │           │
│  │  │  returnObject != null                       → non-null result          │ │           │
│  │  │  hasRole('ADMIN') or returnObject.public    → admin or public resource │ │           │
│  │  └────────────────────────────────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.8 ★ @PreAuthorize and @PostAuthorize — Internal Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ INTERNAL FLOW — HOW @PreAuthorize AND @PostAuthorize WORK                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ── @PreAuthorize FLOW ─────────────────────────────────────────────────────────────────    │
│                                                                                              │
│  Controller Caller                Spring AOP Proxy              Method Body                 │
│  ────────────────                 ─────────────────             ───────────                 │
│  │                                       │                           │                      │
│  │  userController.deleteUser(42)        │                           │                      │
│  │─────────────────────────────────────> │                           │                      │
│  │                                       │                           │                      │
│  │                            ┌──────────────────────────┐          │                      │
│  │                            │  MethodSecurityInterceptor│          │                      │
│  │                            │                            │          │                      │
│  │                            │  @PreAuthorize            │          │                      │
│  │                            │  ("hasRole('ADMIN')")     │          │                      │
│  │                            │                            │          │                      │
│  │                            │  1. Get Authentication    │          │                      │
│  │                            │     from SecurityContext  │          │                      │
│  │                            │  2. Evaluate SpEL:        │          │                      │
│  │                            │     hasRole('ADMIN')?     │          │                      │
│  │                            │                            │          │                      │
│  │                            │  ✅ YES → proceed          │          │                      │
│  │                            │  ❌ NO  → throw            │          │                      │
│  │                            │     AccessDeniedException │          │                      │
│  │                            └──────────┬───────────────┘          │                      │
│  │                                       │ proceed (YES)             │                      │
│  │                                       │──────────────────────────>│                      │
│  │                                       │                           │  deleteUser runs     │
│  │                                       │                           │  userRepo.delete(42) │
│  │                                       │<──────────────────────────│                      │
│  │                                       │  method returns           │                      │
│  │  200 OK                               │                           │                      │
│  │<──────────────────────────────────────│                           │                      │
│  │                                       │                           │                      │
│                                                                                              │
│  ── @PostAuthorize FLOW ────────────────────────────────────────────────────────────────    │
│                                                                                              │
│  Controller Caller                Spring AOP Proxy              Method Body                 │
│  ────────────────                 ─────────────────             ───────────                 │
│  │                                       │                           │                      │
│  │  docController.getDocument(99)        │                           │                      │
│  │─────────────────────────────────────> │                           │                      │
│  │                                       │──────────────────────────>│                      │
│  │                                       │                           │                      │
│  │                                       │                           │  Method ALWAYS runs  │
│  │                                       │                           │  doc = docRepo.      │
│  │                                       │                           │        findById(99)  │
│  │                                       │<──────────────────────────│                      │
│  │                                       │  returns doc              │                      │
│  │                                       │                           │                      │
│  │                            ┌──────────────────────────┐          │                      │
│  │                            │  MethodSecurityInterceptor│          │                      │
│  │                            │                            │          │                      │
│  │                            │  @PostAuthorize           │          │                      │
│  │                            │  ("returnObject.owner     │          │                      │
│  │                            │   == authentication.name")│          │                      │
│  │                            │                            │          │                      │
│  │                            │  returnObject = doc        │          │                      │
│  │                            │  authentication.name = "alice"       │                      │
│  │                            │  doc.owner = "alice"      │          │                      │
│  │                            │                            │          │                      │
│  │                            │  ✅ alice == alice → return doc      │                      │
│  │                            │  ❌ alice != bob   → throw exception  │                      │
│  │                            └──────────┬───────────────┘          │                      │
│  │  200 OK { doc }  OR 403              │                           │                      │
│  │<──────────────────────────────────────│                           │                      │
│  │                                       │                           │                      │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.9 ★ Layer 2 Code — Controller with @PreAuthorize and @PostAuthorize

**AdminController.java — @PreAuthorize Examples**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AdminController.java — Method-Level Security with @PreAuthorize
//
//  ★ @PreAuthorize runs BEFORE the method body executes.
//  ★ If the SpEL expression evaluates to false → AccessDeniedException → 403.
//  ★ Requires @EnableMethodSecurity(prePostEnabled = true) on SecurityConfig!
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── Only ADMIN can list all users ─────────────────────────────────────────
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")                    // ★ LAYER 2 — BEFORE method
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(UserDto::from)
            .toList();
    }

    // ── Only ADMIN can delete a user ──────────────────────────────────────────
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")                    // ★ LAYER 2 — BEFORE method
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── ADMIN or MODERATOR can view reports ───────────────────────────────────
    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")   // ★ LAYER 2 — multiple roles
    public List<ReportDto> getReports() {
        return reportService.findAll();
    }

    // ── Only ADMIN, OR the user themselves can view their own profile ─────────
    //    #userId is a SpEL reference to the method parameter 'userId'
    //    authentication.name is the logged-in username
    @GetMapping("/users/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")   // ★ Complex SpEL
    public UserProfileDto getProfile(@PathVariable Long userId) {
        return userRepository.findProfileById(userId);
    }

    // ── Only authenticated users (any role) can access ────────────────────────
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")                  // ★ Any logged-in user
    public DashboardDto getDashboard() {
        return dashboardService.getDashboard();
    }
}
```

**DocumentController.java — @PostAuthorize Examples**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  DocumentController.java — Method-Level Security with @PostAuthorize
//
//  ★ @PostAuthorize runs AFTER the method body executes.
//  ★ The method ALWAYS runs; only the RETURN of the result is blocked.
//  ★ Special variable 'returnObject' references the method's return value.
//  ★ Use for: ownership checks on the returned data.
//  ★ WARNING: Do NOT use @PostAuthorize for deletes/writes — use @PreAuthorize!
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;

    public DocumentController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    // ── @PostAuthorize: user can only get their OWN document ─────────────────
    //    returnObject = the Document returned by the method
    //    returnObject.owner = the "owner" field of the Document entity
    //    authentication.name = username of the currently logged-in user
    @GetMapping("/{id}")
    @PostAuthorize("returnObject.owner == authentication.name or hasRole('ADMIN')")
    //              ↑ method runs first, then this is checked on the returned doc
    public Document getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        //  ★ This runs for EVERY request, even if alice tries to get bob's doc.
        //    If returnObject.owner = "bob" and authentication.name = "alice":
        //      → "bob" == "alice" is false, and alice has no ADMIN role
        //      → AccessDeniedException → 403 Forbidden
    }

    // ── @PostAuthorize: return only if the document is public OR user owns it ─
    @GetMapping("/{id}/view")
    @PostAuthorize("returnObject.isPublic() or returnObject.owner == authentication.name")
    public Document viewDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // ── Combining @PreAuthorize + @PostAuthorize for defense in depth ─────────
    @GetMapping("/{id}/sensitive")
    @PreAuthorize("isAuthenticated()")                            // ★ BEFORE: must be logged in
    @PostAuthorize("returnObject.owner == authentication.name")   // ★ AFTER: must own it
    public Document getSensitiveDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // ── @PreAuthorize for writes — NEVER use @PostAuthorize for mutations! ────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @documentService.isOwner(#id, authentication.name)")
    //             ↑ calling a Spring bean method inside SpEL with @beanName
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

**UserService.java — @PreAuthorize on the Service Layer**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  UserService.java — Method Security on the SERVICE LAYER
//
//  ★ @PreAuthorize can be placed on ANY Spring-managed bean (Controller, Service,
//    Repository), not just Controllers.
//  ★ Placing it on the Service layer provides security even if someone bypasses
//    the Controller (e.g., internal calls, batch jobs, testing).
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── Only ADMIN can access this service method ─────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    // ── User can access their own data; ADMIN can access anyone's ────────────
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // ── Only ADMIN can promote a user to MODERATOR ───────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    public void promoteToModerator(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.getRoles().add(new Role("ROLE_MODERATOR"));
        userRepository.save(user);
    }

    // ── @PostAuthorize on service — return only if the data belongs to caller ─
    @PostAuthorize("returnObject.username == authentication.name or hasRole('ADMIN')")
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
```

---

#### 21.10 ★ Complete Example — All Three User Roles in Action

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHICH USER CAN ACCESS WHAT — ROLE MATRIX                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────┬──────────┬─────────────┬───────────┐         │
│  │  Endpoint / Action                        │  USER    │  MODERATOR  │  ADMIN    │         │
│  ├──────────────────────────────────────────┼──────────┼─────────────┼───────────┤         │
│  │                                           │          │             │           │         │
│  │  GET  /public/**                          │  ✅       │  ✅          │  ✅        │         │
│  │  GET  /dashboard/**                       │  ✅       │  ✅          │  ✅        │         │
│  │  GET  /api/users/**                       │  ✅       │  ✅          │  ✅        │         │
│  │  GET  /moderate/**                        │  ❌       │  ✅          │  ✅        │         │
│  │  GET  /admin/users                        │  ❌       │  ❌          │  ✅        │         │
│  │  DELETE /admin/users/{id}                 │  ❌       │  ❌          │  ✅        │         │
│  │  GET  /reports/**                         │  ❌       │  ❌          │  ✅        │         │
│  │                                           │          │             │           │         │
│  │  @PreAuthorize("hasRole('ADMIN')")        │  ❌       │  ❌          │  ✅        │         │
│  │  @PreAuthorize("hasAnyRole(A,MOD)")       │  ❌       │  ✅          │  ✅        │         │
│  │  @PostAuthorize(returnObject.owner)       │  own ✅   │  own ✅      │  any ✅    │         │
│  │                                           │          │             │           │         │
│  └──────────────────────────────────────────┴──────────┴─────────────┴───────────┘         │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.11 ★ @PreAuthorize — SpEL Expression Reference

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMMON SpEL EXPRESSIONS FOR @PreAuthorize AND @PostAuthorize                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────┬───────────────────────────┐ │           │
│  │  │  Expression                                 │  What It Checks           │ │           │
│  │  ├────────────────────────────────────────────┼───────────────────────────┤ │           │
│  │  │  hasRole('ADMIN')                           │  Has ROLE_ADMIN          │ │           │
│  │  │  hasAnyRole('ADMIN','MOD')                  │  Has ROLE_ADMIN or MOD   │ │           │
│  │  │  hasAuthority('ROLE_ADMIN')                 │  Exact authority string  │ │           │
│  │  │  isAuthenticated()                          │  Any logged-in user      │ │           │
│  │  │  isAnonymous()                              │  Not authenticated       │ │           │
│  │  │  isFullyAuthenticated()                     │  Not "remember-me" user  │ │           │
│  │  │  principal.username == 'admin'              │  Specific username       │ │           │
│  │  │  authentication.name == 'admin'             │  Same (alternate syntax) │ │           │
│  │  │  #param == authentication.name              │  Method param matches    │ │           │
│  │  │  #userId == authentication.principal.id     │  ID param matches user   │ │           │
│  │  │  @myBean.check(#id, authentication.name)    │  Call a Spring @Bean     │ │           │
│  │  │  hasRole('ADMIN') or #id == principal.id    │  Combine with or/and     │ │           │
│  │  ├────────────────────────────────────────────┼───────────────────────────┤ │           │
│  │  │  @PostAuthorize only:                       │                           │ │           │
│  │  │  returnObject.owner == authentication.name  │  Returned obj's field    │ │           │
│  │  │  returnObject != null                       │  Non-null return         │ │           │
│  │  │  returnObject.active == true                │  Field on returned obj   │ │           │
│  │  └────────────────────────────────────────────┴───────────────────────────┘ │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.12 ★ hasRole vs hasAnyRole vs hasAuthority vs hasAnyAuthority

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ DIFFERENCE — hasRole, hasAnyRole, hasAuthority, hasAnyAuthority                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌───────────────────────────┬──────────────────────────────────────────────┐ │          │
│  │  │  Method                   │  Checks for                                  │ │          │
│  │  ├───────────────────────────┼──────────────────────────────────────────────┤ │          │
│  │  │                           │                                              │ │          │
│  │  │  hasRole("ADMIN")         │  Authority "ROLE_ADMIN" (auto-prefixes ROLE_)│ │          │
│  │  │  hasRole("USER")          │  Authority "ROLE_USER"                       │ │          │
│  │  │                           │                                              │ │          │
│  │  │  hasAnyRole("A","B")      │  Authority "ROLE_A" OR "ROLE_B"             │ │          │
│  │  │                           │                                              │ │          │
│  │  │  hasAuthority("ROLE_ADMIN")│  Exact string "ROLE_ADMIN" (no prefix       │ │          │
│  │  │                           │  added — you must write "ROLE_" yourself)   │ │          │
│  │  │                           │                                              │ │          │
│  │  │  hasAnyAuthority("X","Y") │  Exact string "X" OR "Y"                   │ │          │
│  │  │                           │                                              │ │          │
│  │  └───────────────────────────┴──────────────────────────────────────────────┘ │          │
│  │                                                                               │           │
│  │  ── EQUIVALENCES ─────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  hasRole("ADMIN")          ≡   hasAuthority("ROLE_ADMIN")                   │           │
│  │  hasAnyRole("A","B")       ≡   hasAnyAuthority("ROLE_A","ROLE_B")           │           │
│  │                                                                               │           │
│  │  ── IN CODE ──────────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  // SecurityConfig (URL level):                                              │           │
│  │  .requestMatchers("/admin/**").hasRole("ADMIN")                              │           │
│  │  .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")  // same thing    │           │
│  │                                                                               │           │
│  │  // Method level (@PreAuthorize):                                            │           │
│  │  @PreAuthorize("hasRole('ADMIN')")                                           │           │
│  │  @PreAuthorize("hasAuthority('ROLE_ADMIN')")  // same thing                 │           │
│  │                                                                               │           │
│  │  // User creation (InMemoryUserDetailsManager):                              │           │
│  │  User.withUsername("admin")                                                  │           │
│  │      .roles("ADMIN")           // stores as authority "ROLE_ADMIN"           │           │
│  │      .authorities("ROLE_ADMIN") // same result, explicit                     │           │
│  │      .build()                                                                │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.13 ★ Summary — Role-Based Authorization in Spring Security

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — ROLE-BASED AUTHORIZATION IN SPRING SECURITY                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── WHAT IS RBAC ─────────────────────────────────────────────────           │           │
│  │  • Users are assigned roles (ADMIN, USER, MODERATOR)                        │           │
│  │  • Access to resources is controlled by the user's role                     │           │
│  │  • Spring stores roles as GrantedAuthority with "ROLE_" prefix              │           │
│  │  • hasRole("ADMIN") checks for "ROLE_ADMIN" automatically                  │           │
│  │                                                                               │           │
│  │  ── TWO ANNOTATIONS ───────────────────────────────────────────────          │           │
│  │                                                                               │           │
│  │  @EnableWebSecurity           → Activates URL-level security (Layer 1)      │           │
│  │  @EnableMethodSecurity(       → Enables @PreAuthorize/@PostAuthorize        │           │
│  │    prePostEnabled=true)         (Layer 2 — method-level security)           │           │
│  │                                                                               │           │
│  │  ── LAYER 1 — SecurityConfig (URL-level) ─────────────────────────          │           │
│  │                                                                               │           │
│  │  .requestMatchers("/admin/**").hasRole("ADMIN")                              │           │
│  │  .requestMatchers("/mod/**").hasAnyRole("ADMIN","MODERATOR")                 │           │
│  │  .anyRequest().authenticated()                                               │           │
│  │                                                                               │           │
│  │  • Applied to ALL requests matching the URL pattern                         │           │
│  │  • Coarse-grained: protect entire path prefixes                             │           │
│  │  • Evaluated by AuthorizationFilter in the filter chain                     │           │
│  │  • Rule order matters — first match wins!                                   │           │
│  │                                                                               │           │
│  │  ── LAYER 2 — Controller/Service (method-level) ──────────────────          │           │
│  │                                                                               │           │
│  │  @PreAuthorize("hasRole('ADMIN')")     → checked BEFORE method runs         │           │
│  │  @PostAuthorize("returnObject.owner    → checked AFTER method runs          │           │
│  │    == authentication.name")              on the return value                 │           │
│  │                                                                               │           │
│  │  • Fine-grained: different rules per method                                 │           │
│  │  • SpEL expressions for complex conditions (#param, @bean.method())         │           │
│  │  • Applied via Spring AOP proxy (MethodSecurityInterceptor)                 │           │
│  │  • Works on Controllers, Services, Repositories                             │           │
│  │  • Use @PreAuthorize for writes/deletes; @PostAuthorize for reads only     │           │
│  │                                                                               │           │
│  │  ── BEST PRACTICE ────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • Use BOTH layers for defense in depth                                     │           │
│  │  • Layer 1: coarse protection (entire /admin/** path)                       │           │
│  │  • Layer 2: fine-grained protection (specific method/ownership logic)       │           │
│  │  • NEVER use @PostAuthorize for mutations (writes/deletes)                  │           │
│  │  • Put security at the SERVICE layer too (not just Controller)              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```


---

#### 21.14 ★ hasRole vs hasAuthority — Inside Method-Level Annotations

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ hasRole() vs hasAuthority() — INSIDE @PreAuthorize / @PostAuthorize                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── THE CORE DIFFERENCE ──────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Both hasRole() and hasAuthority() check GrantedAuthority objects             │           │
│  │  inside the Authentication. But they differ in ONE thing:                    │           │
│  │                                                                               │           │
│  │  ★ hasRole("ADMIN")      → AUTOMATICALLY prepends "ROLE_" prefix            │           │
│  │                            → Actually checks for "ROLE_ADMIN"               │           │
│  │                                                                               │           │
│  │  ★ hasAuthority("X")     → Checks for EXACT string "X" as-is               │           │
│  │                            → NO prefix added, no transformation             │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── SIDE-BY-SIDE COMPARISON TABLE ────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌───────────────────────────────────────┬────────────────────────────────┐ │           │
│  │  │  Feature                               │  hasRole()  │  hasAuthority() │ │           │
│  │  ├───────────────────────────────────────┼─────────────┼─────────────────┤ │           │
│  │  │  Auto-prefix "ROLE_"                   │  YES ✅      │  NO ❌           │ │           │
│  │  │  What you write                        │  "ADMIN"    │  "ROLE_ADMIN"   │ │           │
│  │  │  What Spring actually checks           │  ROLE_ADMIN │  ROLE_ADMIN     │ │           │
│  │  │  Can check non-ROLE authorities        │  NO ❌       │  YES ✅          │ │           │
│  │  │  e.g. "SCOPE_read", "user:write"       │  N/A        │  Exact match    │ │           │
│  │  │  Throws error if you include "ROLE_"   │  YES ❌      │  NO             │ │           │
│  │  │  e.g. hasRole("ROLE_ADMIN") → ERROR    │  → ROLE_    │  → works fine   │ │           │
│  │  │       because it becomes ROLE_ROLE_ADMIN│  ROLE_ADMIN │                 │ │           │
│  │  │  Multi-value variant                   │  hasAnyRole │  hasAnyAuthority│ │           │
│  │  └───────────────────────────────────────┴─────────────┴─────────────────┘ │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── INSIDE @PreAuthorize — CODE EXAMPLES ─────────────────────────           │           │
│  │                                                                               │           │
│  │  // ─── These two are IDENTICAL ──────────────────────────────────           │           │
│  │  @PreAuthorize("hasRole('ADMIN')")            // checks ROLE_ADMIN           │           │
│  │  @PreAuthorize("hasAuthority('ROLE_ADMIN')")  // checks ROLE_ADMIN           │           │
│  │                                                                               │           │
│  │  // ─── These two are IDENTICAL ──────────────────────────────────           │           │
│  │  @PreAuthorize("hasAnyRole('ADMIN','MOD')")                                  │           │
│  │  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MOD')")                   │           │
│  │                                                                               │           │
│  │  // ─── hasAuthority can check NON-ROLE authorities ─────────────           │           │
│  │  @PreAuthorize("hasAuthority('SCOPE_read')")       // OAuth2 scope           │           │
│  │  @PreAuthorize("hasAuthority('user:delete')")      // custom permission      │           │
│  │  @PreAuthorize("hasAuthority('PERMISSION_EXPORT')")// custom permission      │           │
│  │                                                                               │           │
│  │  // ─── hasRole CANNOT do this ──────────────────────────────────           │           │
│  │  @PreAuthorize("hasRole('SCOPE_read')")  // ❌ WRONG!                       │           │
│  │  // This checks for "ROLE_SCOPE_read" which doesn't exist!                 │           │
│  │                                                                               │           │
│  │  // ─── COMMON MISTAKE ──────────────────────────────────────────           │           │
│  │  @PreAuthorize("hasRole('ROLE_ADMIN')")  // ❌ WRONG!                       │           │
│  │  // Spring prepends "ROLE_" → checks for "ROLE_ROLE_ADMIN"                 │           │
│  │  // This will NEVER match! Always use hasRole('ADMIN') without prefix.     │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── WHEN TO USE WHICH ────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌───────────────────────────────┬─────────────────────────────────────┐   │           │
│  │  │  Use hasRole()                 │  Use hasAuthority()                  │   │           │
│  │  ├───────────────────────────────┼─────────────────────────────────────┤   │           │
│  │  │  Standard RBAC roles           │  OAuth2 scopes (SCOPE_read)         │   │           │
│  │  │  ADMIN, USER, MODERATOR        │  Custom permissions (user:write)    │   │           │
│  │  │  When User.roles("X") was used │  When authorities DON'T start with  │   │           │
│  │  │  Less typing, more readable    │  "ROLE_" prefix                     │   │           │
│  │  │  Preferred for role checks     │  More explicit, full control        │   │           │
│  │  └───────────────────────────────┴─────────────────────────────────────┘   │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── INTERNAL SOURCE CODE (Spring Security) ───────────────────────           │           │
│  │                                                                               │           │
│  │  // AuthorityAuthorizationManager.java (simplified)                         │           │
│  │  public final class AuthorityAuthorizationManager<T> {                       │           │
│  │                                                                               │           │
│  │      // hasRole() factory method — adds "ROLE_" prefix                      │           │
│  │      public static <T> AuthorityAuthorizationManager<T> hasRole(String role){│           │
│  │          return hasAuthority("ROLE_" + role);  // ★ prepends ROLE_          │           │
│  │      }                                                                       │           │
│  │                                                                               │           │
│  │      // hasAuthority() factory method — uses string as-is                   │           │
│  │      public static <T> AuthorityAuthorizationManager<T>                     │           │
│  │              hasAuthority(String authority) {                                 │           │
│  │          return new AuthorityAuthorizationManager<>(authority); // no prefix │           │
│  │      }                                                                       │           │
│  │                                                                               │           │
│  │      // check() — called at authorization time                              │           │
│  │      public AuthorizationDecision check(Supplier<Authentication> auth, T o) {│           │
│  │          for (GrantedAuthority ga : auth.get().getAuthorities()) {           │           │
│  │              if (this.authority.equals(ga.getAuthority())) {                 │           │
│  │                  return new AuthorizationDecision(true);  // ✅ GRANTED      │           │
│  │              }                                                               │           │
│  │          }                                                                   │           │
│  │          return new AuthorizationDecision(false);  // ❌ DENIED              │           │
│  │      }                                                                       │           │
│  │  }                                                                           │           │
│  │                                                                               │           │
│  │  ★ So hasRole("ADMIN") literally calls hasAuthority("ROLE_ADMIN")           │           │
│  │    They are the SAME check after the prefix is applied!                     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.15 ★ How @PreAuthorize is Intercepted — AuthorizationManagerBeforeMethodInterceptor & SpEL Internals

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW @PreAuthorize IS INTERCEPTED BEFORE THE CONTROLLER METHOD RUNS                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── THE INTERCEPTION CHAIN (Step by Step) ────────────────────────           │           │
│  │                                                                               │           │
│  │  When you annotate a method with @PreAuthorize or @PostAuthorize,           │           │
│  │  Spring Security does NOT modify your controller class. Instead, it uses    │           │
│  │  Spring AOP (Aspect-Oriented Programming) to wrap your bean in a PROXY.    │           │
│  │                                                                               │           │
│  │  ★ This happens at application startup via @EnableMethodSecurity:           │           │
│  │                                                                               │           │
│  │  1. @EnableMethodSecurity(prePostEnabled = true) is processed               │           │
│  │  2. Spring registers a BeanPostProcessor that scans all beans               │           │
│  │  3. If a bean has @PreAuthorize or @PostAuthorize on any method,            │           │
│  │     Spring creates an AOP proxy around that bean                            │           │
│  │  4. The proxy contains interceptors that run BEFORE/AFTER each             │           │
│  │     annotated method                                                        │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── KEY CLASSES IN THE INTERCEPTION CHAIN ────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────────┐ │           │
│  │  │                                                                        │ │           │
│  │  │  For @PreAuthorize:                                                    │ │           │
│  │  │  ─────────────────                                                     │ │           │
│  │  │  AuthorizationManagerBeforeMethodInterceptor                           │ │           │
│  │  │    ↓ delegates to                                                      │ │           │
│  │  │  PreAuthorizeAuthorizationManager                                      │ │           │
│  │  │    ↓ uses                                                              │ │           │
│  │  │  SpelExpressionParser → parses the SpEL string                        │ │           │
│  │  │  MethodSecurityExpressionHandler → provides evaluation context        │ │           │
│  │  │  SecurityExpressionRoot → provides hasRole(), hasAuthority(), etc.    │ │           │
│  │  │                                                                        │ │           │
│  │  │  For @PostAuthorize:                                                   │ │           │
│  │  │  ──────────────────                                                    │ │           │
│  │  │  AuthorizationManagerAfterMethodInterceptor                           │ │           │
│  │  │    ↓ delegates to                                                      │ │           │
│  │  │  PostAuthorizeAuthorizationManager                                     │ │           │
│  │  │    ↓ uses                                                              │ │           │
│  │  │  SpelExpressionParser → same parser                                   │ │           │
│  │  │  MethodSecurityExpressionHandler → adds 'returnObject' variable       │ │           │
│  │  │                                                                        │ │           │
│  │  └────────────────────────────────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE INTERCEPTION FLOW DIAGRAM — @PreAuthorize                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  HTTP Request: DELETE /admin/users/42                                                        │
│  User: alice (ROLE_ADMIN)                                                                    │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  1. DispatcherServlet resolves → AdminController.deleteUser(42)            │             │
│  │     BUT AdminController is actually an AOP PROXY (not the real bean)      │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  2. AOP Proxy (CGLIB or JDK Dynamic Proxy)                                  │             │
│  │     → Intercepts the method call                                           │             │
│  │     → Runs the advice chain (list of interceptors)                         │             │
│  │     → First interceptor: AuthorizationManagerBeforeMethodInterceptor       │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  3. AuthorizationManagerBeforeMethodInterceptor                             │             │
│  │     ★ This is THE class that intercepts @PreAuthorize                      │             │
│  │                                                                              │             │
│  │     a. Reads the @PreAuthorize annotation from the method                  │             │
│  │        → Gets the SpEL expression string: "hasRole('ADMIN')"              │             │
│  │                                                                              │             │
│  │     b. Delegates to PreAuthorizeAuthorizationManager.check()               │             │
│  │        → Passes: Authentication (alice) + MethodInvocation                 │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  4. PreAuthorizeAuthorizationManager                                        │             │
│  │                                                                              │             │
│  │     a. Gets the expression handler (DefaultMethodSecurityExpressionHandler) │             │
│  │                                                                              │             │
│  │     b. Calls SpelExpressionParser.parseExpression("hasRole('ADMIN')")      │             │
│  │        → Returns a SpelExpression object (compiled AST)                    │             │
│  │                                                                              │             │
│  │     c. Creates an EvaluationContext with:                                   │             │
│  │        → rootObject = MethodSecurityExpressionRoot                         │             │
│  │          (contains hasRole(), hasAuthority(), authentication, principal)    │             │
│  │        → method arguments: {id=42}                                         │             │
│  │        → Authentication: alice [ROLE_ADMIN]                                │             │
│  │                                                                              │             │
│  │     d. Evaluates: expression.getValue(context, Boolean.class)              │             │
│  │        → hasRole('ADMIN') → checks alice's authorities for ROLE_ADMIN     │             │
│  │        → Found ROLE_ADMIN → returns TRUE ✅                                 │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  5. AuthorizationDecision = GRANTED                                         │             │
│  │     → Interceptor allows the invocation to proceed                         │             │
│  │     → AOP Proxy calls the REAL AdminController.deleteUser(42)              │             │
│  │     → Method executes normally                                              │             │
│  │     → Returns 204 No Content                                                │             │
│  │                                                                              │             │
│  │  ── IF DENIED ────────────────────────────────────────────────────          │             │
│  │  If bob (ROLE_USER) calls this:                                              │             │
│  │  → hasRole('ADMIN') → checks bob's authorities for ROLE_ADMIN              │             │
│  │  → NOT found → returns FALSE ❌                                              │             │
│  │  → AuthorizationDecision = DENIED                                           │             │
│  │  → Throws AccessDeniedException                                              │             │
│  │  → Spring's ExceptionTranslationFilter converts to 403 Forbidden           │             │
│  │  → Method body NEVER executes                                                │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE INTERCEPTION FLOW DIAGRAM — @PostAuthorize                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  HTTP Request: GET /api/documents/99                                                         │
│  User: alice                                                                                 │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  1. AOP Proxy → NO "before" interceptor for @PostAuthorize                 │             │
│  │     → Method call proceeds IMMEDIATELY to the real bean                    │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  2. DocumentController.getDocument(99)                                      │             │
│  │     → Executes the method body                                              │             │
│  │     → Fetches document from DB: doc = { id:99, owner:"bob", ... }          │             │
│  │     → Returns the doc object                                                │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │ returns doc                                                │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  3. AuthorizationManagerAfterMethodInterceptor                              │             │
│  │     ★ This is THE class that intercepts @PostAuthorize                     │             │
│  │                                                                              │             │
│  │     a. Reads @PostAuthorize annotation:                                     │             │
│  │        "returnObject.owner == authentication.name"                          │             │
│  │                                                                              │             │
│  │     b. Delegates to PostAuthorizeAuthorizationManager.check()              │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  4. PostAuthorizeAuthorizationManager                                       │             │
│  │                                                                              │             │
│  │     a. SpelExpressionParser parses:                                         │             │
│  │        "returnObject.owner == authentication.name"                          │             │
│  │                                                                              │             │
│  │     b. Creates EvaluationContext with:                                      │             │
│  │        → rootObject = MethodSecurityExpressionRoot                         │             │
│  │        → returnObject = doc { id:99, owner:"bob" }    ★ set by interceptor │             │
│  │        → authentication = alice                                             │             │
│  │                                                                              │             │
│  │     c. Evaluates:                                                           │             │
│  │        returnObject.owner → "bob"                                           │             │
│  │        authentication.name → "alice"                                        │             │
│  │        "bob" == "alice" → FALSE ❌                                           │             │
│  │                                                                              │             │
│  │     d. AuthorizationDecision = DENIED                                       │             │
│  │        → Throws AccessDeniedException → 403 Forbidden                      │             │
│  │        → alice CANNOT see bob's document                                    │             │
│  │                                                                              │             │
│  │     ★ NOTE: The method body DID execute (DB was queried)                   │             │
│  │       but the response is blocked. This is why @PostAuthorize              │             │
│  │       should only be used for READ operations, never for writes!           │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.16 ★ What is Spring Expression Language (SpEL)?

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SPRING EXPRESSION LANGUAGE (SpEL) — OVERVIEW                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── WHAT IS SpEL? ────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Spring Expression Language (SpEL) is a powerful expression language         │           │
│  │  built into the Spring Framework. It supports querying and manipulating     │           │
│  │  an object graph at RUNTIME.                                                 │           │
│  │                                                                               │           │
│  │  ★ SpEL is NOT Java code — it's a separate expression language              │           │
│  │  ★ It runs INSIDE the Spring container at runtime                           │           │
│  │  ★ It can access Spring beans, method parameters, authentication objects   │           │
│  │  ★ Used in: @PreAuthorize, @PostAuthorize, @Value, @Cacheable, etc.        │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── KEY FEATURES ─────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • Literal expressions: 'hello', 42, true, null                             │           │
│  │  • Boolean operators: and, or, not (also &&, ||, !)                         │           │
│  │  • Relational operators: ==, !=, <, >, <=, >=                               │           │
│  │  • String operations: 'hello'.length(), 'admin'.toUpperCase()               │           │
│  │  • Property access: object.property, authentication.name                    │           │
│  │  • Method invocation: @beanName.methodName(args)                            │           │
│  │  • Collection access: list[0], map['key']                                   │           │
│  │  • Ternary operator: condition ? trueValue : falseValue                     │           │
│  │  • Elvis operator: value ?: defaultValue                                    │           │
│  │  • Safe navigation: object?.property (null-safe)                            │           │
│  │  • instanceof: #obj instanceof T(String)                                    │           │
│  │  • Type reference: T(java.lang.Math).PI                                     │           │
│  │  • Variable access: #variableName (method parameters)                       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── SpEL IN SECURITY CONTEXT ─────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  In @PreAuthorize and @PostAuthorize, SpEL has access to:                   │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────┬────────────────────────────────────┐ │           │
│  │  │  Variable / Root                  │  What it provides                  │ │           │
│  │  ├──────────────────────────────────┼────────────────────────────────────┤ │           │
│  │  │  authentication                   │  The Authentication object         │ │           │
│  │  │  authentication.name              │  Username of logged-in user        │ │           │
│  │  │  authentication.authorities       │  Collection of GrantedAuthority    │ │           │
│  │  │  authentication.principal         │  The UserDetails object            │ │           │
│  │  │  principal                        │  Shortcut for authentication       │ │           │
│  │  │                                   │  .principal                        │ │           │
│  │  │  #paramName                       │  Method parameter by name          │ │           │
│  │  │  #arg0, #arg1                     │  Method parameter by index         │ │           │
│  │  │  returnObject                     │  Return value (@PostAuthorize only)│ │           │
│  │  │  @beanName                        │  Access a Spring @Bean             │ │           │
│  │  │  hasRole('X')                     │  Method on SecurityExpressionRoot  │ │           │
│  │  │  hasAuthority('X')                │  Method on SecurityExpressionRoot  │ │           │
│  │  │  isAuthenticated()                │  Method on SecurityExpressionRoot  │ │           │
│  │  │  permitAll                        │  Always true                       │ │           │
│  │  │  denyAll                          │  Always false                      │ │           │
│  │  └──────────────────────────────────┴────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.17 ★ How SpelExpressionParser Compiles @PreAuthorize and @PostAuthorize

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW SpelExpressionParser COMPILES THE SpEL INSIDE @PreAuthorize / @PostAuthorize        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── THE COMPILATION PIPELINE ─────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  When Spring encounters:                                                     │           │
│  │  @PreAuthorize("hasRole('ADMIN') and #userId == authentication.principal.id")│           │
│  │                                                                               │           │
│  │  Here is what happens internally:                                            │           │
│  │                                                                               │           │
│  │  ┌─────────────────────────────────────────────────────────────────────┐    │           │
│  │  │  STEP 1: LEXING (Tokenization)                                      │    │           │
│  │  │  ─────────────────────────────                                      │    │           │
│  │  │  SpelExpressionParser receives the raw string:                      │    │           │
│  │  │  "hasRole('ADMIN') and #userId == authentication.principal.id"      │    │           │
│  │  │                                                                      │    │           │
│  │  │  InternalSpelExpressionParser.tokenize() breaks it into tokens:     │    │           │
│  │  │                                                                      │    │           │
│  │  │  Token[0]: IDENTIFIER    "hasRole"                                  │    │           │
│  │  │  Token[1]: LPAREN        "("                                        │    │           │
│  │  │  Token[2]: LITERAL_STRING "ADMIN"                                   │    │           │
│  │  │  Token[3]: RPAREN        ")"                                        │    │           │
│  │  │  Token[4]: AND           "and"                                      │    │           │
│  │  │  Token[5]: HASH          "#"                                        │    │           │
│  │  │  Token[6]: IDENTIFIER    "userId"                                   │    │           │
│  │  │  Token[7]: EQ            "=="                                       │    │           │
│  │  │  Token[8]: IDENTIFIER    "authentication"                           │    │           │
│  │  │  Token[9]: DOT           "."                                        │    │           │
│  │  │  Token[10]: IDENTIFIER   "principal"                                │    │           │
│  │  │  Token[11]: DOT          "."                                        │    │           │
│  │  │  Token[12]: IDENTIFIER   "id"                                       │    │           │
│  │  └─────────────────────────────────────────────────────────────────────┘    │           │
│  │                                                                               │           │
│  │  ┌─────────────────────────────────────────────────────────────────────┐    │           │
│  │  │  STEP 2: PARSING (Build Abstract Syntax Tree / AST)                 │    │           │
│  │  │  ──────────────────────────────────────────────                     │    │           │
│  │  │  InternalSpelExpressionParser.doParseExpression() builds an AST:    │    │           │
│  │  │                                                                      │    │           │
│  │  │           OpAnd                                                      │    │           │
│  │  │          /      \                                                    │    │           │
│  │  │    MethodRef     OpEQ                                                │    │           │
│  │  │    hasRole()    /    \                                                │    │           │
│  │  │      │       VariableRef  PropertyAccess                             │    │           │
│  │  │    "ADMIN"   #userId     authentication                              │    │           │
│  │  │                             .principal                               │    │           │
│  │  │                               .id                                    │    │           │
│  │  │                                                                      │    │           │
│  │  │  AST Node types used:                                                │    │           │
│  │  │  • OpAnd          → logical AND operator                            │    │           │
│  │  │  • MethodReference → hasRole('ADMIN')                               │    │           │
│  │  │  • OpEQ           → equality comparison (==)                        │    │           │
│  │  │  • VariableReference → #userId (method parameter)                   │    │           │
│  │  │  • CompoundExpression → authentication.principal.id (chained props) │    │           │
│  │  └─────────────────────────────────────────────────────────────────────┘    │           │
│  │                                                                               │           │
│  │  ┌─────────────────────────────────────────────────────────────────────┐    │           │
│  │  │  STEP 3: EXPRESSION OBJECT CREATION                                 │    │           │
│  │  │  ──────────────────────────────────                                 │    │           │
│  │  │  SpelExpressionParser.parseExpression() returns a SpelExpression:    │    │           │
│  │  │                                                                      │    │           │
│  │  │  SpelExpression spelExpr = parser.parseExpression(                   │    │           │
│  │  │      "hasRole('ADMIN') and #userId == authentication.principal.id"  │    │           │
│  │  │  );                                                                  │    │           │
│  │  │                                                                      │    │           │
│  │  │  ★ This SpelExpression holds the compiled AST.                     │    │           │
│  │  │  ★ It is CACHED — not re-parsed on every request!                  │    │           │
│  │  │  ★ Stored in PreAuthorizeAuthorizationManager's expression cache.  │    │           │
│  │  └─────────────────────────────────────────────────────────────────────┘    │           │
│  │                                                                               │           │
│  │  ┌─────────────────────────────────────────────────────────────────────┐    │           │
│  │  │  STEP 4: EVALUATION (at request time)                               │    │           │
│  │  │  ────────────────────────────────────                               │    │           │
│  │  │  On each request, Spring creates an EvaluationContext:              │    │           │
│  │  │                                                                      │    │           │
│  │  │  MethodSecurityEvaluationContext context = new ...();                │    │           │
│  │  │  context.setRootObject(new MethodSecurityExpressionRoot(auth));      │    │           │
│  │  │  context.setVariable("userId", methodArgs[0]);  // #userId = 42    │    │           │
│  │  │                                                                      │    │           │
│  │  │  // For @PostAuthorize, additionally:                               │    │           │
│  │  │  context.setVariable("returnObject", methodReturnValue);            │    │           │
│  │  │                                                                      │    │           │
│  │  │  // Evaluate the cached expression against this context:            │    │           │
│  │  │  Boolean result = spelExpr.getValue(context, Boolean.class);        │    │           │
│  │  │                                                                      │    │           │
│  │  │  // result = true  → AuthorizationDecision(true) → proceed         │    │           │
│  │  │  // result = false → AuthorizationDecision(false) → 403            │    │           │
│  │  └─────────────────────────────────────────────────────────────────────┘    │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── THE ROOT OBJECT: MethodSecurityExpressionRoot ────────────────           │           │
│  │                                                                               │           │
│  │  The root object provides all the built-in methods you can call              │           │
│  │  inside @PreAuthorize / @PostAuthorize:                                      │           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────────┐ │           │
│  │  │  class MethodSecurityExpressionRoot extends SecurityExpressionRoot {   │ │           │
│  │  │                                                                        │ │           │
│  │  │    // From SecurityExpressionRoot:                                     │ │           │
│  │  │    Authentication authentication;   // the logged-in user             │ │           │
│  │  │                                                                        │ │           │
│  │  │    boolean hasRole(String role);                                       │ │           │
│  │  │    boolean hasAnyRole(String... roles);                                │ │           │
│  │  │    boolean hasAuthority(String authority);                              │ │           │
│  │  │    boolean hasAnyAuthority(String... authorities);                     │ │           │
│  │  │    boolean isAuthenticated();                                           │ │           │
│  │  │    boolean isAnonymous();                                               │ │           │
│  │  │    boolean isFullyAuthenticated();                                      │ │           │
│  │  │    boolean isRememberMe();                                              │ │           │
│  │  │    boolean permitAll();    // always true                              │ │           │
│  │  │    boolean denyAll();      // always false                             │ │           │
│  │  │    Object getPrincipal();  // same as authentication.principal         │ │           │
│  │  │                                                                        │ │           │
│  │  │    // From MethodSecurityExpressionRoot:                               │ │           │
│  │  │    Object filterObject;    // used by @PreFilter                       │ │           │
│  │  │    Object returnObject;    // used by @PostAuthorize                   │ │           │
│  │  │    Object this;            // the target bean (controller/service)     │ │           │
│  │  │  }                                                                     │ │           │
│  │  └────────────────────────────────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │  ★ When you write hasRole('ADMIN') in SpEL, Spring calls                   │           │
│  │    MethodSecurityExpressionRoot.hasRole("ADMIN") on the root object!       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**Simplified Code — How Spring Wires It All Together**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SIMPLIFIED INTERNAL CODE — How Spring Security processes @PreAuthorize
//  (This is NOT code you write — this shows what happens inside Spring!)
// ═══════════════════════════════════════════════════════════════════════════════

// ── 1. @EnableMethodSecurity registers this interceptor ──────────────────────
//    This bean is auto-registered by PrePostMethodSecurityConfiguration
@Bean
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
static MethodInterceptor preAuthorizeAuthorizationMethodInterceptor() {
    PreAuthorizeAuthorizationManager manager = new PreAuthorizeAuthorizationManager();
    // ★ Sets up the SpelExpressionParser internally
    manager.setExpressionHandler(new DefaultMethodSecurityExpressionHandler());
    return AuthorizationManagerBeforeMethodInterceptor.preAuthorize(manager);
}

// ── 2. Inside PreAuthorizeAuthorizationManager.check() ──────────────────────
public final class PreAuthorizeAuthorizationManager
        implements AuthorizationManager<MethodInvocation> {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authentication,
            MethodInvocation invocation) {

        // a. Find the @PreAuthorize annotation on the method
        PreAuthorize preAuthorize = findAnnotation(invocation.getMethod());
        String expressionString = preAuthorize.value();
        // expressionString = "hasRole('ADMIN') and #userId == authentication.principal.id"

        // b. Parse the SpEL expression (cached after first parse)
        Expression expression = this.parser.parseExpression(expressionString);

        // c. Create the evaluation context
        EvaluationContext context = this.expressionHandler
            .createEvaluationContext(authentication, invocation);
        //  → Sets rootObject = new MethodSecurityExpressionRoot(authentication)
        //  → Sets variables: #userId = invocation.getArguments()[0], etc.

        // d. Evaluate the expression
        boolean granted = expression.getValue(context, Boolean.class);
        //  → Walks the AST: hasRole('ADMIN') → true/false
        //  → AND → #userId == authentication.principal.id → true/false

        return new AuthorizationDecision(granted);
    }
}

// ── 3. AuthorizationManagerBeforeMethodInterceptor.invoke() ──────────────────
public final class AuthorizationManagerBeforeMethodInterceptor
        implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        // ★ Check authorization BEFORE the method
        AuthorizationDecision decision = this.authorizationManager
            .check(() -> SecurityContextHolder.getContext().getAuthentication(),
                   invocation);

        if (decision != null && !decision.isGranted()) {
            throw new AccessDeniedException("Access Denied");
            // ★ Method body NEVER executes!
        }

        // ★ Authorization passed — proceed with the method call
        return invocation.proceed();
    }
}
```

---

#### 21.18 ★ Logical and Relational Operators in @PreAuthorize SpEL

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ALL OPERATORS AVAILABLE IN @PreAuthorize / @PostAuthorize SpEL EXPRESSIONS              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── LOGICAL OPERATORS ────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌────────────────┬──────────────┬────────────────────────────────────────┐ │           │
│  │  │  Operator       │  Alternative │  Example                               │ │           │
│  │  ├────────────────┼──────────────┼────────────────────────────────────────┤ │           │
│  │  │  and            │  &&          │  hasRole('ADMIN') and isAuthenticated()│ │           │
│  │  │  or             │  ||          │  hasRole('ADMIN') or hasRole('MOD')   │ │           │
│  │  │  not            │  !           │  not hasRole('GUEST')                  │ │           │
│  │  │  !              │  not         │  !isAnonymous()                        │ │           │
│  │  └────────────────┴──────────────┴────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │  ★ Prefer 'and', 'or', 'not' (textual) over &&, ||, ! in annotations       │           │
│  │    because XML/annotation processors may have issues with & and < symbols.  │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── RELATIONAL / COMPARISON OPERATORS ────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌────────────────┬──────────────┬────────────────────────────────────────┐ │           │
│  │  │  Operator       │  Textual     │  Example                               │ │           │
│  │  ├────────────────┼──────────────┼────────────────────────────────────────┤ │           │
│  │  │  ==             │  eq          │  #username == authentication.name      │ │           │
│  │  │  !=             │  ne          │  #role != 'GUEST'                      │ │           │
│  │  │  <              │  lt          │  #age lt 18                            │ │           │
│  │  │  >              │  gt          │  #price gt 100                         │ │           │
│  │  │  <=             │  le          │  #count le 10                          │ │           │
│  │  │  >=             │  ge          │  #score ge 90                          │ │           │
│  │  │  instanceof     │              │  #obj instanceof T(String)             │ │           │
│  │  │  matches        │              │  #email matches '[a-z]+@.*'            │ │           │
│  │  │  between        │              │  #age between {18, 65}                 │ │           │
│  │  └────────────────┴──────────────┴────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── ARITHMETIC OPERATORS ─────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌────────────────┬──────────────────────────────────────────────────────┐ │           │
│  │  │  Operator       │  Example                                             │ │           │
│  │  ├────────────────┼──────────────────────────────────────────────────────┤ │           │
│  │  │  +              │  #a + #b > 100                                       │ │           │
│  │  │  -              │  #deadline - #now > 0                                │ │           │
│  │  │  *              │  #price * #qty < 10000                               │ │           │
│  │  │  /              │  #total / #count >= 50                               │ │           │
│  │  │  %              │  #value % 2 == 0   (even check)                      │ │           │
│  │  │  ^              │  #base ^ 2 == 16   (power)                           │ │           │
│  │  └────────────────┴──────────────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── TERNARY & ELVIS OPERATORS ────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌────────────────┬──────────────────────────────────────────────────────┐ │           │
│  │  │  Operator       │  Example                                             │ │           │
│  │  ├────────────────┼──────────────────────────────────────────────────────┤ │           │
│  │  │  ? :  (ternary) │  #role == 'ADMIN' ? true : false                     │ │           │
│  │  │  ?:   (elvis)   │  #name ?: 'anonymous'  (null → default)             │ │           │
│  │  │  ?.   (safe nav)│  #user?.address?.city  (null-safe navigation)       │ │           │
│  │  └────────────────┴──────────────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── COLLECTION & ARRAY OPERATORS ─────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────────┐ │           │
│  │  │  Expression                              │  Description               │ │           │
│  │  ├────────────────────────────────────────────────────────────────────────┤ │           │
│  │  │  #list[0]                                 │  Index access              │ │           │
│  │  │  #map['key']                              │  Map access                │ │           │
│  │  │  #list.size()                             │  Collection size           │ │           │
│  │  │  #list.contains(#item)                    │  Contains check            │ │           │
│  │  │  #roles.?[#this == 'ADMIN']               │  Selection (filter)        │ │           │
│  │  │  #roles.![#this.toUpperCase()]             │  Projection (transform)   │ │           │
│  │  └────────────────────────────────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── REAL @PreAuthorize EXAMPLES WITH OPERATORS ───────────────────           │           │
│  │                                                                               │           │
│  │  // ── AND ───────────────────────────────────────────────────────           │           │
│  │  @PreAuthorize("hasRole('ADMIN') and #departmentId == authentication"       │           │
│  │       + ".principal.departmentId")                                           │           │
│  │  // Must be ADMIN AND same department                                        │           │
│  │                                                                               │           │
│  │  // ── OR ────────────────────────────────────────────────────────           │           │
│  │  @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")      │           │
│  │  // Admin can access anyone; regular users can access their own data        │           │
│  │                                                                               │           │
│  │  // ── NOT ───────────────────────────────────────────────────────           │           │
│  │  @PreAuthorize("isAuthenticated() and not hasRole('BANNED')")               │           │
│  │  // Any authenticated user who is NOT banned                                 │           │
│  │                                                                               │           │
│  │  // ── EQUALITY ──────────────────────────────────────────────────           │           │
│  │  @PreAuthorize("#userId == authentication.principal.id")                     │           │
│  │  // Only the user themselves can access (ID must match)                      │           │
│  │                                                                               │           │
│  │  // ── COMPARISON ────────────────────────────────────────────────           │           │
│  │  @PreAuthorize("#amount <= 10000 or hasRole('FINANCE_MANAGER')")            │           │
│  │  // Regular users limited to 10000; managers can approve any amount         │           │
│  │                                                                               │           │
│  │  // ── COMPLEX COMBINATION ───────────────────────────────────────           │           │
│  │  @PreAuthorize("(hasRole('ADMIN') or hasRole('MODERATOR'))"                 │           │
│  │       + " and (#entity.status != 'LOCKED' or hasRole('ADMIN'))")            │           │
│  │  // MOD can edit non-locked entities; ADMIN can edit everything              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.19 ★ Complete Examples — @PreAuthorize and @PostAuthorize Using Method Arguments, returnObject, and Authentication

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ACCESSING METHOD ARGUMENTS, returnObject, AND authentication IN SpEL                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── HOW TO ACCESS EACH ───────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌───────────────────────────┬──────────────────┬─────────────────────────┐ │           │
│  │  │  What                     │  SpEL Syntax      │  Available In           │ │           │
│  │  ├───────────────────────────┼──────────────────┼─────────────────────────┤ │           │
│  │  │  Method parameter (name)  │  #paramName       │  @PreAuthorize          │ │           │
│  │  │                           │                   │  @PostAuthorize         │ │           │
│  │  │  Method parameter (index) │  #a0, #a1, #p0    │  @PreAuthorize          │ │           │
│  │  │                           │                   │  @PostAuthorize         │ │           │
│  │  │  Return value             │  returnObject     │  @PostAuthorize ONLY   │ │           │
│  │  │  Current user             │  authentication   │  Both                   │ │           │
│  │  │  Username                 │  authentication   │  Both                   │ │           │
│  │  │                           │    .name           │                         │ │           │
│  │  │  Principal object         │  authentication   │  Both                   │ │           │
│  │  │                           │    .principal      │                         │ │           │
│  │  │  User's authorities       │  authentication   │  Both                   │ │           │
│  │  │                           │    .authorities    │                         │ │           │
│  │  │  Spring bean              │  @beanName         │  Both                   │ │           │
│  │  │  Bean method              │  @beanName         │  Both                   │ │           │
│  │  │                           │    .method(args)   │                         │ │           │
│  │  └───────────────────────────┴──────────────────┴─────────────────────────┘ │           │
│  │                                                                               │           │
│  │  ★ #paramName uses Java parameter names, which requires:                    │           │
│  │    → The -parameters compiler flag (Spring Boot adds this by default)       │           │
│  │    → OR @Param("paramName") annotation from Spring                          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**Entity Classes for Examples**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Entity Classes — Used in @PreAuthorize / @PostAuthorize examples below
// ═══════════════════════════════════════════════════════════════════════════════

@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerUsername;   // owner of the order
    private String status;             // PENDING, APPROVED, SHIPPED, CANCELLED
    private BigDecimal totalAmount;
    private String department;

    // getters and setters
}

@Entity
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String owner;              // username who created the doc
    private boolean isPublic;
    private String classification;     // PUBLIC, INTERNAL, CONFIDENTIAL

    // getters and setters
}
```

**Example 1 — @PreAuthorize Using Method Arguments (#paramName)**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  OrderController.java — @PreAuthorize with Method Arguments
//
//  ★ #orderId, #username, #amount → method parameter references in SpEL
//  ★ authentication.name → the logged-in user's username
//  ★ authentication.principal → the UserDetails object
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── Example 1: #username matches the logged-in user ───────────────────────
    //    Only the user themselves (or ADMIN) can view their orders
    //    #username refers to the method parameter 'username'
    @GetMapping("/user/{username}")
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    //             ↑ method param     ↑ logged-in user's username
    public List<Order> getOrdersByUsername(@PathVariable String username) {
        return orderService.findByCustomerUsername(username);
    }
    //  ★ If alice calls GET /api/orders/user/alice → #username="alice",
    //    authentication.name="alice" → "alice"=="alice" → true ✅
    //  ★ If alice calls GET /api/orders/user/bob   → #username="bob",
    //    authentication.name="alice" → "bob"=="alice" → false,
    //    hasRole('ADMIN')? → false → 403 ❌


    // ── Example 2: #id combined with principal.id ─────────────────────────────
    //    Only the user whose ID matches (or ADMIN) can access
    //    authentication.principal.id → the 'id' field on UserDetails impl
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    //                                  ↑ method param   ↑ user's ID from principal
    public Order getOrderById(@PathVariable Long id) {
        return orderService.findById(id);
    }


    // ── Example 3: #amount with comparison operators ──────────────────────────
    //    Regular users can only create orders up to 10,000
    //    Managers can create orders of any amount
    @PostMapping
    @PreAuthorize("#order.totalAmount <= 10000 or hasRole('MANAGER')")
    //             ↑ accessing a field on the request body object
    public Order createOrder(@RequestBody Order order) {
        order.setCustomerUsername(
            SecurityContextHolder.getContext().getAuthentication().getName()
        );
        return orderService.save(order);
    }
    //  ★ If user (ROLE_USER) sends order with totalAmount=5000 → 5000<=10000 → true ✅
    //  ★ If user (ROLE_USER) sends order with totalAmount=50000 → false,
    //    hasRole('MANAGER')? → false → 403 ❌
    //  ★ If manager (ROLE_MANAGER) sends totalAmount=50000 → false,
    //    hasRole('MANAGER')? → true → ✅ (short-circuit OR)


    // ── Example 4: Multiple parameters with AND ───────────────────────────────
    //    Customer can only cancel their OWN orders that are still PENDING
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or "
        + "(#username == authentication.name and #status == 'PENDING')")
    //     ↑ param 'username'                 ↑ param 'status'
    public Order cancelOrder(
            @PathVariable Long orderId,
            @RequestParam String username,
            @RequestParam String status) {
        return orderService.cancelOrder(orderId);
    }


    // ── Example 5: Calling a Spring @Bean method in SpEL ─────────────────────
    //    @orderSecurityService is a Spring bean reference
    //    .isOrderOwner(#orderId, authentication.name) calls a method on that bean
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or "
        + "@orderSecurityService.isOrderOwner(#orderId, authentication.name)")
    //   ↑ Spring bean name        ↑ method on the bean   ↑ method params
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteById(orderId);
        return ResponseEntity.noContent().build();
    }
}
```

**OrderSecurityService — Spring Bean Called from SpEL**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  OrderSecurityService.java — A Spring bean referenced from @PreAuthorize SpEL
//
//  ★ In SpEL: @orderSecurityService.isOrderOwner(#orderId, authentication.name)
//  ★ The @beanName syntax lets you call ANY method on ANY Spring bean!
// ═══════════════════════════════════════════════════════════════════════════════

@Service("orderSecurityService")   // ★ bean name = "orderSecurityService"
public class OrderSecurityService {

    private final OrderRepository orderRepository;

    public OrderSecurityService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ★ This method is called from SpEL inside @PreAuthorize!
    public boolean isOrderOwner(Long orderId, String username) {
        return orderRepository.findById(orderId)
            .map(order -> order.getCustomerUsername().equals(username))
            .orElse(false);
    }

    public boolean canAccessDepartment(String department, String username) {
        // Custom business logic to check department access
        return departmentRepository.isUserInDepartment(username, department);
    }
}
```

**Example 2 — @PostAuthorize Using returnObject**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  DocumentController.java — @PostAuthorize with returnObject
//
//  ★ returnObject = the value returned by the method
//  ★ You can access ANY field/method on the returned object
//  ★ The method ALWAYS executes — only the response is blocked if check fails
//  ★ ONLY use @PostAuthorize for READ operations (never writes/deletes!)
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;

    public DocumentController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    // ── Example 1: returnObject.owner must match the logged-in user ──────────
    //    OR the user must be an ADMIN
    @GetMapping("/{id}")
    @PostAuthorize("returnObject.owner == authentication.name or hasRole('ADMIN')")
    //              ↑ returned doc's owner  ↑ logged-in username
    public Document getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    //  ★ Method runs → fetches doc from DB → doc.owner = "bob"
    //  ★ If alice calls: "bob" == "alice" → false, hasRole('ADMIN')? →
    //    if alice is ADMIN → true ✅, if not → 403 ❌
    //  ★ If bob calls: "bob" == "bob" → true ✅ (returns doc)


    // ── Example 2: Check multiple fields on returnObject ─────────────────────
    //    Document must be PUBLIC, or owned by the caller, or caller is ADMIN
    @GetMapping("/{id}/view")
    @PostAuthorize("returnObject.isPublic() "
        + "or returnObject.owner == authentication.name "
        + "or hasRole('ADMIN')")
    public Document viewDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    //  ★ isPublic() is a method call on the Document entity!
    //  ★ SpEL calls Document.isPublic() at runtime


    // ── Example 3: returnObject field with enum/string comparison ────────────
    //    Only CONFIDENTIAL documents require ADMIN; others visible to all users
    @GetMapping("/{id}/classified")
    @PostAuthorize("returnObject.classification != 'CONFIDENTIAL' "
        + "or hasRole('ADMIN')")
    public Document getClassifiedDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }


    // ── Example 4: Combining @PreAuthorize + @PostAuthorize ──────────────────
    //    @PreAuthorize: must be authenticated (checked BEFORE)
    //    @PostAuthorize: must own the document (checked AFTER on returnObject)
    @GetMapping("/{id}/sensitive")
    @PreAuthorize("isAuthenticated()")
    @PostAuthorize("returnObject.owner == authentication.name")
    public Document getSensitiveDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }


    // ── Example 5: returnObject with nested property access ──────────────────
    //    Access a nested object's field using dot notation
    @GetMapping("/{id}/detail")
    @PostAuthorize("returnObject.department == authentication.principal.department "
        + "or hasRole('ADMIN')")
    //              ↑ doc's department    ↑ user's department from UserDetails
    public Document getDocumentDetail(@PathVariable Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
```

**Example 3 — @PreAuthorize Using authentication Object**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  UserController.java — @PreAuthorize with authentication object
//
//  ★ authentication          → the full Authentication object
//  ★ authentication.name     → username (String)
//  ★ authentication.principal → the UserDetails object
//  ★ authentication.authorities → Collection<GrantedAuthority>
//  ★ principal               → shortcut for authentication.principal
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ── Example 1: authentication.name (most common) ─────────────────────────
    //    User can only view their own profile
    @GetMapping("/profile/{username}")
    @PreAuthorize("#username == authentication.name")
    //             ↑ method param  ↑ logged-in username
    public UserProfile getProfile(@PathVariable String username) {
        return userService.getProfileByUsername(username);
    }


    // ── Example 2: authentication.principal (accessing UserDetails fields) ───
    //    Only users from the same department can view department data
    //    Assumes your UserDetails implementation has a getDepartment() method
    @GetMapping("/department/{deptId}")
    @PreAuthorize("#deptId == authentication.principal.departmentId "
        + "or hasRole('ADMIN')")
    //              ↑ method param  ↑ custom field on UserDetails impl
    public List<UserProfile> getDepartmentUsers(@PathVariable Long deptId) {
        return userService.findByDepartment(deptId);
    }


    // ── Example 3: authentication.authorities (checking authority collection) ─
    //    Check if the user has a specific authority in their collection
    @GetMapping("/special")
    @PreAuthorize("authentication.authorities.![authority].contains('SCOPE_admin')")
    //                              ↑ project to strings    ↑ check if contains
    public String getSpecialResource() {
        return "You have the admin scope!";
    }


    // ── Example 4: principal shortcut ────────────────────────────────────────
    //    'principal' is a shortcut for 'authentication.principal'
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getCurrentUser(
            @AuthenticationPrincipal UserDetails principal) {
        return Map.of(
            "username", principal.getUsername(),
            "authorities", principal.getAuthorities()
        );
    }


    // ── Example 5: Complex expression combining all three ────────────────────
    //    Method argument + authentication + principal + Spring bean
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN') "
        + "and #userId != authentication.principal.id "
        + "and @userSecurityService.canModifyRole(#userId, #newRole)")
    //   ↑ must be admin   ↑ can't change own role   ↑ bean validates the change
    public UserProfile changeRole(
            @PathVariable Long userId,
            @RequestParam String newRole) {
        return userService.changeRole(userId, newRole);
    }
    //  ★ This checks THREE conditions:
    //    1. Caller must have ROLE_ADMIN
    //    2. Caller cannot modify their own role (prevent self-demotion)
    //    3. Custom bean validates the role change is allowed
}
```

**Example 4 — Complete Service Layer with All SpEL Features**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  OrderService.java — Service layer combining ALL SpEL features
//
//  ★ This service demonstrates:
//    1. #param   → method argument
//    2. authentication → logged-in user
//    3. @bean.method() → external validation
//    4. returnObject → return value check
//    5. Logical operators → and, or, not
//    6. Comparison operators → ==, !=, <=
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ── @PreAuthorize with method argument + authentication ───────────────────
    @PreAuthorize("#customerUsername == authentication.name or hasRole('ADMIN')")
    public List<Order> findOrdersByCustomer(String customerUsername) {
        return orderRepository.findByCustomerUsername(customerUsername);
    }

    // ── @PreAuthorize with comparison operator on argument ────────────────────
    @PreAuthorize("#order.totalAmount <= 50000 or hasAnyRole('MANAGER','ADMIN')")
    public Order createOrder(Order order) {
        order.setStatus("PENDING");
        return orderRepository.save(order);
    }

    // ── @PreAuthorize: cannot approve your own order ─────────────────────────
    @PreAuthorize("hasRole('MANAGER') "
        + "and @orderSecurityService.isOrderOwner(#orderId, authentication.name) == false")
    //   ↑ must be manager   ↑ must NOT be the order owner (no self-approval!)
    public Order approveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        order.setStatus("APPROVED");
        return orderRepository.save(order);
    }

    // ── @PostAuthorize with returnObject + authentication ────────────────────
    @PostAuthorize("returnObject.customerUsername == authentication.name "
        + "or hasRole('ADMIN')")
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    // ── @PostAuthorize with returnObject field comparison ─────────────────────
    //    Only return the order if it's not in CANCELLED status
    //    (unless the caller is ADMIN)
    @PostAuthorize("returnObject.status != 'CANCELLED' or hasRole('ADMIN')")
    public Order getOrderDetails(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    // ── @PreAuthorize with NOT operator ───────────────────────────────────────
    @PreAuthorize("isAuthenticated() and not hasRole('BANNED')")
    public List<Order> getMyOrders() {
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return orderRepository.findByCustomerUsername(username);
    }

    // ── @PreAuthorize + @PostAuthorize combined ──────────────────────────────
    @PreAuthorize("isAuthenticated()")           // BEFORE: must be logged in
    @PostAuthorize("returnObject.customerUsername == authentication.name "
        + "or returnObject.status == 'PENDING' "
        + "or hasRole('ADMIN')")                 // AFTER: must own it OR pending OR admin
    public Order getOrderForReview(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ FLOW DIAGRAM — COMPLETE EXAMPLE WITH METHOD ARGS + returnObject + authentication        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  HTTP: GET /api/orders/42                                                                    │
│  User: alice (ROLE_USER)                                                                     │
│  Service method:                                                                             │
│    @PostAuthorize("returnObject.customerUsername == authentication.name "                    │
│        + "or hasRole('ADMIN')")                                                              │
│    public Order findById(Long orderId)                                                       │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  1. AOP Proxy intercepts findById(42)                                       │             │
│  │     → No @PreAuthorize → proceeds immediately                              │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  2. Method body executes:                                                    │             │
│  │     order = orderRepository.findById(42)                                    │             │
│  │     → Returns: Order { id:42, customerUsername:"alice", status:"PENDING" }  │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │ returns order                                              │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  3. AuthorizationManagerAfterMethodInterceptor                              │             │
│  │     → @PostAuthorize expression:                                            │             │
│  │       "returnObject.customerUsername == authentication.name or hasRole(..)" │             │
│  │                                                                              │             │
│  │     SpelExpressionParser evaluates with:                                    │             │
│  │     ┌──────────────────────────────────────────────────────────┐           │             │
│  │     │  EvaluationContext:                                       │           │             │
│  │     │    rootObject = MethodSecurityExpressionRoot               │           │             │
│  │     │    returnObject = Order { customerUsername:"alice", ... }  │           │             │
│  │     │    authentication = { name:"alice", roles:[ROLE_USER] }   │           │             │
│  │     └──────────────────────────────────────────────────────────┘           │             │
│  │                                                                              │             │
│  │     Evaluation:                                                              │             │
│  │     ① returnObject.customerUsername → "alice"                               │             │
│  │     ② authentication.name → "alice"                                         │             │
│  │     ③ "alice" == "alice" → TRUE ✅                                           │             │
│  │     ④ Short-circuit OR → no need to check hasRole('ADMIN')                 │             │
│  │                                                                              │             │
│  │     AuthorizationDecision = GRANTED ✅                                       │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│               200 OK { id:42, customerUsername:"alice", status:"PENDING" }                   │
│                                                                                              │
│                                                                                              │
│  ── SAME REQUEST BY bob (ROLE_USER): ─────────────────────────────────────────              │
│                                                                                              │
│     ① returnObject.customerUsername → "alice"                                               │
│     ② authentication.name → "bob"                                                           │
│     ③ "alice" == "bob" → FALSE                                                              │
│     ④ hasRole('ADMIN') → bob has ROLE_USER → FALSE                                         │
│     ⑤ FALSE or FALSE → FALSE ❌                                                             │
│     ⑥ AccessDeniedException → 403 Forbidden                                                 │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.20 ★ Summary — hasRole vs hasAuthority, SpEL Internals, Operators, and Access Patterns

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — DEEP DIVE INTO METHOD-LEVEL SECURITY                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── hasRole vs hasAuthority ──────────────────────────────────────           │           │
│  │  • hasRole("ADMIN")     → auto-prepends "ROLE_" → checks ROLE_ADMIN        │           │
│  │  • hasAuthority("X")    → checks exact string "X" (no prefix)              │           │
│  │  • hasRole is for ROLES; hasAuthority for any GrantedAuthority             │           │
│  │  • Never write hasRole("ROLE_ADMIN") → becomes ROLE_ROLE_ADMIN ❌          │           │
│  │                                                                               │           │
│  │  ── Interception Chain ───────────────────────────────────────────           │           │
│  │  • @PreAuthorize → AuthorizationManagerBeforeMethodInterceptor             │           │
│  │      → PreAuthorizeAuthorizationManager → SpEL evaluation                  │           │
│  │  • @PostAuthorize → AuthorizationManagerAfterMethodInterceptor             │           │
│  │      → PostAuthorizeAuthorizationManager → SpEL evaluation                 │           │
│  │  • Both use Spring AOP proxy (CGLIB) to wrap the target bean              │           │
│  │  • Method body NEVER runs if @PreAuthorize fails                           │           │
│  │  • Method body ALWAYS runs for @PostAuthorize (only response blocked)     │           │
│  │                                                                               │           │
│  │  ── SpEL (Spring Expression Language) ────────────────────────────           │           │
│  │  • A runtime expression language built into Spring                          │           │
│  │  • SpelExpressionParser lexes → parses → builds AST → caches it           │           │
│  │  • Evaluation uses MethodSecurityExpressionRoot as root object             │           │
│  │  • Root provides: hasRole(), hasAuthority(), isAuthenticated(), etc.       │           │
│  │  • Variables: #paramName, authentication, returnObject, @beanName          │           │
│  │                                                                               │           │
│  │  ── Operators in @PreAuthorize / @PostAuthorize ──────────────────           │           │
│  │  • Logical: and (&&), or (||), not (!)                                     │           │
│  │  • Relational: == (eq), != (ne), < (lt), > (gt), <= (le), >= (ge)         │           │
│  │  • Arithmetic: +, -, *, /, %, ^                                             │           │
│  │  • String: matches (regex), contains, startsWith                           │           │
│  │  • Special: instanceof, ?. (safe-nav), ?: (elvis), ?: (ternary)           │           │
│  │                                                                               │           │
│  │  ── Accessing Data in SpEL ───────────────────────────────────────           │           │
│  │  • Method arguments: #paramName or #a0, #a1, #p0                          │           │
│  │  • Authentication: authentication.name, authentication.principal           │           │
│  │  • Return value: returnObject (@PostAuthorize only)                        │           │
│  │  • Spring beans: @beanName.methodName(#param, authentication.name)        │           │
│  │                                                                               │           │
│  │  ── Best Practices ───────────────────────────────────────────────           │           │
│  │  • Use @PreAuthorize for writes/deletes (prevents execution)              │           │
│  │  • Use @PostAuthorize only for reads (method always runs)                 │           │
│  │  • Prefer textual operators (and, or, not) over symbols (&&, ||, !)       │           │
│  │  • Extract complex logic to a Spring @Bean method                         │           │
│  │  • Test with @WithMockUser("admin") in unit tests                         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.21 ★ Role-Based Authorization with Roles, Permissions, and Groups — Complete Use Case

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ USE CASE — ENTERPRISE AUTHORIZATION MODEL                                               │
│    User → has Roles → each Role has Permissions                                             │
│    User → belongs to Groups → each Group has Roles                                          │
│    Permission = the actual authority that gates an API                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── THE PROBLEM ──────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  In a simple system, a user has ROLES (ADMIN, USER, MODERATOR).             │           │
│  │  But in enterprise apps, roles alone aren't enough. You need:               │           │
│  │                                                                               │           │
│  │  • Roles      → coarse-grained identity (ADMIN, EDITOR, VIEWER)            │           │
│  │  • Permissions → fine-grained actions (user:read, user:write, user:delete)  │           │
│  │  • Groups     → organizational units (ENGINEERING, FINANCE, HR)             │           │
│  │                                                                               │           │
│  │  A user may have:                                                            │           │
│  │  ┌────────────────────────────────────────────────────────────────────┐     │           │
│  │  │  alice:                                                            │     │           │
│  │  │    Direct Roles:  [EDITOR]                                         │     │           │
│  │  │    Groups:        [ENGINEERING]                                     │     │           │
│  │  │      └─ Group ENGINEERING has Roles: [DEVELOPER, DEPLOYER]         │     │           │
│  │  │    Effective Roles:   EDITOR + DEVELOPER + DEPLOYER                │     │           │
│  │  │    Effective Perms:   (all permissions attached to those 3 roles)  │     │           │
│  │  └────────────────────────────────────────────────────────────────────┘     │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── THE DATA MODEL ───────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │       ┌───────────┐         ┌───────────┐         ┌──────────────┐          │           │
│  │       │   User    │ M ── N  │   Role    │ M ── N  │  Permission  │          │           │
│  │       │           │─────────│           │─────────│              │          │           │
│  │       │ username  │         │ name      │         │ name         │          │           │
│  │       │ password  │         │           │         │ e.g.         │          │           │
│  │       │ enabled   │         │ e.g.      │         │ "user:read"  │          │           │
│  │       │           │         │ "ADMIN"   │         │ "user:write" │          │           │
│  │       └─────┬─────┘         └───────────┘         │ "order:read" │          │           │
│  │             │                                      └──────────────┘          │           │
│  │             │ M ── N                                                         │           │
│  │       ┌─────▼─────┐         ┌───────────┐                                   │           │
│  │       │   Group   │ M ── N  │   Role    │  (same Role table)                │           │
│  │       │           │─────────│           │                                   │           │
│  │       │ name      │         │           │                                   │           │
│  │       │ e.g.      │         │           │                                   │           │
│  │       │ "ENGG"    │         │           │                                   │           │
│  │       └───────────┘         └───────────┘                                   │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── HOW IT MAPS TO SPRING SECURITY GrantedAuthority ──────────────           │           │
│  │                                                                               │           │
│  │  Spring Security only knows about GrantedAuthority (flat list of strings).  │           │
│  │  Our job is to FLATTEN the hierarchy at login time:                          │           │
│  │                                                                               │           │
│  │  User alice                                                                  │           │
│  │    ├── Direct Roles → [EDITOR]                                              │           │
│  │    ├── Groups → [ENGINEERING]                                               │           │
│  │    │     └── Group Roles → [DEVELOPER, DEPLOYER]                            │           │
│  │    └── All Roles = EDITOR + DEVELOPER + DEPLOYER                            │           │
│  │          ├── EDITOR perms    → [article:read, article:write, article:publish]│           │
│  │          ├── DEVELOPER perms → [code:read, code:write, deploy:staging]      │           │
│  │          └── DEPLOYER perms  → [deploy:staging, deploy:production]          │           │
│  │                                                                               │           │
│  │  ★ Final GrantedAuthority list for alice:                                   │           │
│  │  [                                                                           │           │
│  │    ROLE_EDITOR, ROLE_DEVELOPER, ROLE_DEPLOYER,    ← roles (with ROLE_)     │           │
│  │    article:read, article:write, article:publish,   ← permissions (no prefix)│           │
│  │    code:read, code:write, deploy:staging,                                   │           │
│  │    deploy:production                                                         │           │
│  │  ]                                                                           │           │
│  │                                                                               │           │
│  │  Now Spring Security can use:                                                │           │
│  │  hasRole('EDITOR')              → checks ROLE_EDITOR ✅                     │           │
│  │  hasAuthority('article:write')  → checks article:write ✅                   │           │
│  │  hasAuthority('deploy:production') → checks deploy:production ✅            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.22 ★ JPA Entity Classes — User, Role, Permission, Group

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Permission.java — Fine-grained action (e.g. "user:read", "order:write")
// ═══════════════════════════════════════════════════════════════════════════════

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;   // e.g. "user:read", "user:write", "order:delete"

    // ── constructors ──
    public Permission() {}

    public Permission(String name) {
        this.name = name;
    }

    // ── getters & setters ──
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Role.java — Coarse-grained identity (e.g. "ADMIN", "EDITOR")
//  ★ Each Role has a Set<Permission> — many-to-many
// ═══════════════════════════════════════════════════════════════════════════════

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;   // e.g. "ADMIN", "EDITOR", "VIEWER"

    // ★ Many-to-Many: Role ←→ Permission
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    // ── constructors ──
    public Role() {}

    public Role(String name) {
        this.name = name;
    }

    // ── getters & setters ──
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  UserGroup.java — Organizational grouping (e.g. "ENGINEERING", "FINANCE")
//  ★ Each Group has a Set<Role> — many-to-many
//  ★ Named "UserGroup" to avoid conflict with SQL reserved keyword "GROUP"
// ═══════════════════════════════════════════════════════════════════════════════

@Entity
@Table(name = "user_groups")
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;   // e.g. "ENGINEERING", "FINANCE", "HR"

    // ★ Many-to-Many: Group ←→ Role
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "group_roles",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // ── constructors ──
    public UserGroup() {}

    public UserGroup(String name) {
        this.name = name;
    }

    // ── getters & setters ──
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AppUser.java — The User entity
//  ★ Has direct Roles (many-to-many)
//  ★ Has Groups (many-to-many) — each Group also has Roles
//  ★ Named "AppUser" to avoid conflict with SQL reserved keyword "USER"
// ═══════════════════════════════════════════════════════════════════════════════

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;   // BCrypt encoded

    private boolean enabled = true;

    // ★ Many-to-Many: User ←→ Role (direct role assignment)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // ★ Many-to-Many: User ←→ Group
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_groups_mapping",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<UserGroup> groups = new HashSet<>();

    // ── constructors ──
    public AppUser() {}

    public AppUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // ── getters & setters ──
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public Set<UserGroup> getGroups() { return groups; }
    public void setGroups(Set<UserGroup> groups) { this.groups = groups; }
}
```

---

#### 21.23 ★ Flattening the Hierarchy — Custom UserDetailsService

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ THE KEY STEP — FLATTENING ROLES + PERMISSIONS + GROUPS INTO GrantedAuthority LIST       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── WHY FLATTEN? ─────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Spring Security ONLY understands a flat Collection<GrantedAuthority>.      │           │
│  │  It doesn't know about our Role → Permission or Group → Role hierarchy.    │           │
│  │                                                                               │           │
│  │  So at LOGIN TIME (in UserDetailsService), we must:                          │           │
│  │                                                                               │           │
│  │  1. Collect ALL roles — direct roles + roles from groups                     │           │
│  │  2. Collect ALL permissions — from every collected role                       │           │
│  │  3. Convert roles to "ROLE_xxx" GrantedAuthority                             │           │
│  │  4. Convert permissions to "xxx" GrantedAuthority (no prefix)               │           │
│  │  5. Merge into ONE flat Set<GrantedAuthority>                                │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── EXAMPLE ──────────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  User: alice                                                                 │           │
│  │  ├── Direct Roles: [EDITOR]                                                 │           │
│  │  │     └── EDITOR permissions: [article:read, article:write]                │           │
│  │  ├── Groups: [ENGINEERING]                                                   │           │
│  │  │     └── ENGINEERING roles: [DEVELOPER]                                    │           │
│  │  │           └── DEVELOPER permissions: [code:read, code:write]             │           │
│  │  │                                                                           │           │
│  │  └── Final GrantedAuthority Set:                                             │           │
│  │       ┌─────────────────────────────────────────────────────┐              │           │
│  │       │  ROLE_EDITOR        ← from direct role             │              │           │
│  │       │  ROLE_DEVELOPER     ← from group ENGINEERING       │              │           │
│  │       │  article:read       ← permission from EDITOR       │              │           │
│  │       │  article:write      ← permission from EDITOR       │              │           │
│  │       │  code:read          ← permission from DEVELOPER    │              │           │
│  │       │  code:write         ← permission from DEVELOPER    │              │           │
│  │       └─────────────────────────────────────────────────────┘              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AppUserRepository.java
// ═══════════════════════════════════════════════════════════════════════════════

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  CustomUserDetailsService.java
//
//  ★ THE MOST IMPORTANT CLASS — flattens Roles + Permissions + Groups
//    into a single Collection<GrantedAuthority> for Spring Security
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public CustomUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        AppUser appUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found: " + username));

        // ★ STEP 1: Collect ALL roles (direct + from groups)
        Set<Role> allRoles = new HashSet<>();

        // 1a. Add direct roles
        allRoles.addAll(appUser.getRoles());

        // 1b. Add roles from each group the user belongs to
        for (UserGroup group : appUser.getGroups()) {
            allRoles.addAll(group.getRoles());
        }

        // ★ STEP 2: Build the flat GrantedAuthority collection
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role role : allRoles) {
            // 2a. Add the role itself as ROLE_xxx
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

            // 2b. Add every permission attached to this role (no prefix)
            for (Permission perm : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(perm.getName()));
            }
        }

        // ★ STEP 3: Return Spring Security's User object with the flat authorities
        return new org.springframework.security.core.userdetails.User(
            appUser.getUsername(),
            appUser.getPassword(),
            appUser.isEnabled(),
            true,   // accountNonExpired
            true,   // credentialsNonExpired
            true,   // accountNonLocked
            authorities
        );
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ FLOW DIAGRAM — LOGIN → FLATTEN → GrantedAuthority SET                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  POST /login  { username: "alice", password: "secret" }                                     │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  1. AuthenticationManager.authenticate()                                    │             │
│  │     → Calls DaoAuthenticationProvider                                      │             │
│  │     → Calls CustomUserDetailsService.loadUserByUsername("alice")            │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  2. CustomUserDetailsService:                                                │             │
│  │                                                                              │             │
│  │     DB Query: SELECT * FROM app_users WHERE username = 'alice'              │             │
│  │     → alice { roles: [EDITOR], groups: [ENGINEERING] }                      │             │
│  │                                                                              │             │
│  │     Fetch group roles:                                                       │             │
│  │     → ENGINEERING.roles = [DEVELOPER]                                       │             │
│  │                                                                              │             │
│  │     All roles = [EDITOR, DEVELOPER]                                         │             │
│  │                                                                              │             │
│  │     Fetch permissions:                                                       │             │
│  │     → EDITOR.permissions     = [article:read, article:write]                │             │
│  │     → DEVELOPER.permissions  = [code:read, code:write]                      │             │
│  │                                                                              │             │
│  │     Build GrantedAuthority set:                                              │             │
│  │     ┌────────────────────────────────────────────────────────┐             │             │
│  │     │  ROLE_EDITOR, ROLE_DEVELOPER,                          │             │             │
│  │     │  article:read, article:write, code:read, code:write   │             │             │
│  │     └────────────────────────────────────────────────────────┘             │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  3. Authentication object stored in SecurityContext                         │             │
│  │     → username: "alice"                                                     │             │
│  │     → authorities: [ROLE_EDITOR, ROLE_DEVELOPER,                           │             │
│  │                      article:read, article:write,                          │             │
│  │                      code:read, code:write]                                │             │
│  │                                                                              │             │
│  │     Now EVERY subsequent request can use:                                   │             │
│  │     hasRole('EDITOR')              → checks ROLE_EDITOR ✅                 │             │
│  │     hasAuthority('code:write')     → checks code:write ✅                  │             │
│  │     hasAuthority('deploy:prod')    → NOT present → ❌ 403                  │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.24 ★ SecurityConfig — Wiring It All Together

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig.java — Spring Security Configuration
//
//  ★ Enables method-level security (@PreAuthorize, @PostAuthorize)
//  ★ Uses our CustomUserDetailsService for authentication
//  ★ Configures HTTP-level security for broad access rules
//  ★ Fine-grained permission checks done via @PreAuthorize in controllers
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // ★ enables @PreAuthorize & @PostAuthorize
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // disable for REST API (use CORS + JWT in prod)

            .authorizeHttpRequests(auth -> auth
                // ── Public endpoints ──
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()

                // ── Admin panel ──
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                //                                 ↑ coarse-grained: URL-level

                // ── Everything else requires authentication ──
                .anyRequest().authenticated()
                //  ★ Fine-grained permission checks are done inside controllers
                //    using @PreAuthorize("hasAuthority('...')")
            )

            .httpBasic(Customizer.withDefaults());  // Basic Auth for simplicity

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder =
            http.getSharedObject(AuthenticationManagerBuilder.class);
        builder
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
        return builder.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

#### 21.25 ★ Controllers — Using hasRole and hasAuthority for API Authorization

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ THE AUTHORIZATION STRATEGY                                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  URL-level (SecurityFilterChain):                                            │           │
│  │    → Coarse-grained: /api/admin/** requires ROLE_ADMIN                      │           │
│  │    → /api/public/** is open to all                                           │           │
│  │    → Everything else requires authentication                                 │           │
│  │                                                                               │           │
│  │  Method-level (@PreAuthorize):                                               │           │
│  │    → Fine-grained: each method checks specific PERMISSIONS                  │           │
│  │    → hasRole('X')      for role-based checks                                │           │
│  │    → hasAuthority('X') for permission-based checks                          │           │
│  │                                                                               │           │
│  │  ── WHY BOTH? ────────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  URL-level = first line of defense (broad categories)                        │           │
│  │  Method-level = second line of defense (exact permissions)                   │           │
│  │  Both run. If URL-level passes but method-level fails → 403 Forbidden.      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  UserManagementController.java — CRUD for user resources
//
//  ★ Uses hasAuthority() for fine-grained PERMISSION checks
//  ★ Each endpoint requires a specific permission like "user:read", "user:write"
//  ★ Permissions come from the Role → Permission mapping in DB
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService userService;

    public UserManagementController(UserManagementService userService) {
        this.userService = userService;
    }

    // ── GET /api/users — List all users ──────────────────────────────────────
    //    Requires: "user:read" permission
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public List<UserDto> getAllUsers() {
        return userService.findAll();
    }

    // ── GET /api/users/{id} — Get single user ────────────────────────────────
    //    Requires: "user:read" permission
    //    OR the user is viewing their own profile
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read') or #id == authentication.principal.id")
    public UserDto getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    // ── POST /api/users — Create a new user ──────────────────────────────────
    //    Requires: "user:write" permission
    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public UserDto createUser(@RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    // ── PUT /api/users/{id} — Update a user ──────────────────────────────────
    //    Requires: "user:write" permission
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public UserDto updateUser(@PathVariable Long id,
                              @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    // ── DELETE /api/users/{id} — Delete a user ───────────────────────────────
    //    Requires: "user:delete" permission (more restrictive than user:write)
    //    AND must be ADMIN role (double protection)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete') and hasRole('ADMIN')")
    //              ↑ permission check              ↑ role check
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  OrderController.java — Order management endpoints
//
//  ★ Different permissions for different operations
//  ★ Combining permission checks with business rules
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── List orders ──────────────────────────────────────────────────────────
    //    Anyone with "order:read" can list orders
    @GetMapping
    @PreAuthorize("hasAuthority('order:read')")
    public List<OrderDto> listOrders() {
        return orderService.findAll();
    }

    // ── Create an order ──────────────────────────────────────────────────────
    //    Requires "order:write"
    @PostMapping
    @PreAuthorize("hasAuthority('order:write')")
    public OrderDto createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.create(request, SecurityContextHolder.getContext()
            .getAuthentication().getName());
    }

    // ── Approve an order ─────────────────────────────────────────────────────
    //    Requires "order:approve" permission (only managers typically have this)
    //    AND the approver must NOT be the order creator (business rule)
    @PutMapping("/{orderId}/approve")
    @PreAuthorize("hasAuthority('order:approve') "
        + "and @orderSecurityService.isNotCreator(#orderId, authentication.name)")
    public OrderDto approveOrder(@PathVariable Long orderId) {
        return orderService.approve(orderId);
    }

    // ── Cancel an order ──────────────────────────────────────────────────────
    //    Creator can cancel their own, OR anyone with "order:cancel" permission
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('order:cancel') "
        + "or @orderSecurityService.isCreator(#orderId, authentication.name)")
    public OrderDto cancelOrder(@PathVariable Long orderId) {
        return orderService.cancel(orderId);
    }

    // ── Delete an order ──────────────────────────────────────────────────────
    //    Only ADMIN role + "order:delete" permission
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('order:delete')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.delete(orderId);
        return ResponseEntity.noContent().build();
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ReportController.java — Reports & Analytics endpoints
//
//  ★ Shows how GROUPS enable team-based access
//  ★ Members of FINANCE group get ACCOUNTANT role → has "report:finance" perm
//  ★ Members of ENGINEERING group get DEVELOPER role → has "report:engineering" perm
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ── View financial reports ───────────────────────────────────────────────
    //    Requires "report:finance" permission
    //    (Typically comes from ACCOUNTANT role → assigned via FINANCE group)
    @GetMapping("/finance")
    @PreAuthorize("hasAuthority('report:finance')")
    public FinanceReportDto getFinanceReport() {
        return reportService.generateFinanceReport();
    }

    // ── View engineering metrics ─────────────────────────────────────────────
    //    Requires "report:engineering" permission
    //    (Comes from DEVELOPER role → assigned via ENGINEERING group)
    @GetMapping("/engineering")
    @PreAuthorize("hasAuthority('report:engineering')")
    public EngineeringReportDto getEngineeringReport() {
        return reportService.generateEngineeringReport();
    }

    // ── View all reports (executive dashboard) ───────────────────────────────
    //    Requires ADMIN role OR both finance AND engineering permissions
    @GetMapping("/executive")
    @PreAuthorize("hasRole('ADMIN') or "
        + "(hasAuthority('report:finance') and hasAuthority('report:engineering'))")
    public ExecutiveReportDto getExecutiveReport() {
        return reportService.generateExecutiveReport();
    }

    // ── Export any report ────────────────────────────────────────────────────
    //    Requires "report:export" permission (very restrictive)
    @PostMapping("/export")
    @PreAuthorize("hasAuthority('report:export')")
    public byte[] exportReport(@RequestBody ExportRequest request) {
        return reportService.export(request);
    }
}
```

---

#### 21.26 ★ Sample Data — Setting Up the Role/Permission/Group Hierarchy

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  DataInitializer.java — Populates the database with sample data at startup
//
//  ★ This shows the COMPLETE hierarchy setup:
//    Permissions → attached to Roles → Roles assigned directly or via Groups
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserGroupRepository groupRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(PermissionRepository permissionRepository,
                           RoleRepository roleRepository,
                           UserGroupRepository groupRepository,
                           AppUserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // ═══════════════════════════════════════════════════════════════════════
        //  STEP 1: Create Permissions (fine-grained actions)
        // ═══════════════════════════════════════════════════════════════════════

        // ── User permissions ──
        Permission userRead   = permissionRepository.save(new Permission("user:read"));
        Permission userWrite  = permissionRepository.save(new Permission("user:write"));
        Permission userDelete = permissionRepository.save(new Permission("user:delete"));

        // ── Order permissions ──
        Permission orderRead    = permissionRepository.save(new Permission("order:read"));
        Permission orderWrite   = permissionRepository.save(new Permission("order:write"));
        Permission orderApprove = permissionRepository.save(new Permission("order:approve"));
        Permission orderCancel  = permissionRepository.save(new Permission("order:cancel"));
        Permission orderDelete  = permissionRepository.save(new Permission("order:delete"));

        // ── Report permissions ──
        Permission reportFinance     = permissionRepository.save(new Permission("report:finance"));
        Permission reportEngineering = permissionRepository.save(new Permission("report:engineering"));
        Permission reportExport      = permissionRepository.save(new Permission("report:export"));

        // ── Code/Deploy permissions ──
        Permission codeRead       = permissionRepository.save(new Permission("code:read"));
        Permission codeWrite      = permissionRepository.save(new Permission("code:write"));
        Permission deployStaging  = permissionRepository.save(new Permission("deploy:staging"));
        Permission deployProd     = permissionRepository.save(new Permission("deploy:production"));


        // ═══════════════════════════════════════════════════════════════════════
        //  STEP 2: Create Roles and assign Permissions to each Role
        // ═══════════════════════════════════════════════════════════════════════

        // ── ADMIN role → has ALL permissions ──
        Role adminRole = new Role("ADMIN");
        adminRole.setPermissions(Set.of(
            userRead, userWrite, userDelete,
            orderRead, orderWrite, orderApprove, orderCancel, orderDelete,
            reportFinance, reportEngineering, reportExport,
            codeRead, codeWrite, deployStaging, deployProd
        ));
        adminRole = roleRepository.save(adminRole);

        // ── MANAGER role → can manage users and approve orders ──
        Role managerRole = new Role("MANAGER");
        managerRole.setPermissions(Set.of(
            userRead, userWrite,
            orderRead, orderWrite, orderApprove, orderCancel,
            reportFinance, reportExport
        ));
        managerRole = roleRepository.save(managerRole);

        // ── DEVELOPER role → code + deploy to staging + engineering reports ──
        Role developerRole = new Role("DEVELOPER");
        developerRole.setPermissions(Set.of(
            codeRead, codeWrite,
            deployStaging,
            reportEngineering,
            orderRead, orderWrite
        ));
        developerRole = roleRepository.save(developerRole);

        // ── ACCOUNTANT role → financial reports + order reads ──
        Role accountantRole = new Role("ACCOUNTANT");
        accountantRole.setPermissions(Set.of(
            reportFinance, reportExport,
            orderRead
        ));
        accountantRole = roleRepository.save(accountantRole);

        // ── VIEWER role → read-only access ──
        Role viewerRole = new Role("VIEWER");
        viewerRole.setPermissions(Set.of(
            userRead, orderRead, codeRead
        ));
        viewerRole = roleRepository.save(viewerRole);

        // ── DEPLOYER role → can deploy to staging + production ──
        Role deployerRole = new Role("DEPLOYER");
        deployerRole.setPermissions(Set.of(
            deployStaging, deployProd
        ));
        deployerRole = roleRepository.save(deployerRole);


        // ═══════════════════════════════════════════════════════════════════════
        //  STEP 3: Create Groups and assign Roles to each Group
        // ═══════════════════════════════════════════════════════════════════════

        // ── ENGINEERING group → members get DEVELOPER + DEPLOYER roles ──
        UserGroup engineeringGroup = new UserGroup("ENGINEERING");
        engineeringGroup.setRoles(Set.of(developerRole, deployerRole));
        engineeringGroup = groupRepository.save(engineeringGroup);

        // ── FINANCE group → members get ACCOUNTANT role ──
        UserGroup financeGroup = new UserGroup("FINANCE");
        financeGroup.setRoles(Set.of(accountantRole));
        financeGroup = groupRepository.save(financeGroup);

        // ── MANAGEMENT group → members get MANAGER role ──
        UserGroup managementGroup = new UserGroup("MANAGEMENT");
        managementGroup.setRoles(Set.of(managerRole));
        managementGroup = groupRepository.save(managementGroup);


        // ═══════════════════════════════════════════════════════════════════════
        //  STEP 4: Create Users with direct Roles and/or Groups
        // ═══════════════════════════════════════════════════════════════════════

        // ── super_admin: direct ADMIN role, no groups needed ──
        AppUser superAdmin = new AppUser("super_admin",
            passwordEncoder.encode("admin123"));
        superAdmin.setRoles(Set.of(adminRole));
        userRepository.save(superAdmin);
        //  ★ Effective authorities: ROLE_ADMIN + ALL 15 permissions

        // ── alice: VIEWER role directly + ENGINEERING group ──
        AppUser alice = new AppUser("alice",
            passwordEncoder.encode("alice123"));
        alice.setRoles(Set.of(viewerRole));         // direct role
        alice.setGroups(Set.of(engineeringGroup));   // group membership
        userRepository.save(alice);
        //  ★ Effective roles: VIEWER + DEVELOPER + DEPLOYER
        //  ★ Effective permissions: user:read, order:read, code:read (from VIEWER)
        //     + code:read, code:write, deploy:staging, report:engineering,
        //       order:read, order:write (from DEVELOPER)
        //     + deploy:staging, deploy:production (from DEPLOYER)

        // ── bob: no direct roles, only FINANCE group ──
        AppUser bob = new AppUser("bob",
            passwordEncoder.encode("bob123"));
        bob.setGroups(Set.of(financeGroup));
        userRepository.save(bob);
        //  ★ Effective roles: ACCOUNTANT (from FINANCE group)
        //  ★ Effective permissions: report:finance, report:export, order:read

        // ── charlie: VIEWER role + MANAGEMENT group ──
        AppUser charlie = new AppUser("charlie",
            passwordEncoder.encode("charlie123"));
        charlie.setRoles(Set.of(viewerRole));
        charlie.setGroups(Set.of(managementGroup));
        userRepository.save(charlie);
        //  ★ Effective roles: VIEWER + MANAGER
        //  ★ Effective permissions: user:read, order:read, code:read (from VIEWER)
        //     + user:read, user:write, order:read, order:write,
        //       order:approve, order:cancel, report:finance,
        //       report:export (from MANAGER)

        // ── diana: ENGINEERING + FINANCE groups (cross-team) ──
        AppUser diana = new AppUser("diana",
            passwordEncoder.encode("diana123"));
        diana.setGroups(Set.of(engineeringGroup, financeGroup));
        userRepository.save(diana);
        //  ★ Effective roles: DEVELOPER + DEPLOYER + ACCOUNTANT
        //  ★ She can access BOTH engineering AND finance reports!
    }
}
```

---

#### 21.27 ★ Authorization Matrix — Who Can Access What

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ AUTHORIZATION MATRIX — MAPPING USERS → ROLES → PERMISSIONS → API ACCESS               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── USER → ROLES (direct + from groups) ──────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌──────────────┬───────────────────┬──────────────────────────────────────┐│           │
│  │  │  User         │  Direct Roles     │  Groups → Inherited Roles           ││           │
│  │  ├──────────────┼───────────────────┼──────────────────────────────────────┤│           │
│  │  │  super_admin  │  ADMIN            │  (none)                              ││           │
│  │  │  alice        │  VIEWER           │  ENGINEERING → DEVELOPER, DEPLOYER  ││           │
│  │  │  bob          │  (none)           │  FINANCE → ACCOUNTANT               ││           │
│  │  │  charlie      │  VIEWER           │  MANAGEMENT → MANAGER              ││           │
│  │  │  diana        │  (none)           │  ENGINEERING → DEVELOPER, DEPLOYER  ││           │
│  │  │               │                   │  FINANCE → ACCOUNTANT               ││           │
│  │  └──────────────┴───────────────────┴──────────────────────────────────────┘│           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── API ACCESS MATRIX ────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────┬────────┬───────┬─────┬─────────┬───────┐ │           │
│  │  │  API Endpoint                │ admin  │ alice │ bob │ charlie │ diana │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  GET  /api/users             │  ✅    │  ✅   │ ❌  │   ✅    │  ❌   │ │           │
│  │  │  (user:read)                 │        │VIEWER │     │ VIEWER  │       │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  POST /api/users             │  ✅    │  ❌   │ ❌  │   ✅    │  ❌   │ │           │
│  │  │  (user:write)                │        │       │     │ MANAGER │       │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  DELETE /api/users/{id}      │  ✅    │  ❌   │ ❌  │   ❌    │  ❌   │ │           │
│  │  │  (user:delete + ADMIN role)  │        │       │     │         │       │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  GET  /api/orders            │  ✅    │  ✅   │ ✅  │   ✅    │  ✅   │ │           │
│  │  │  (order:read)                │        │ ALL   │ACCT │ ALL     │ ALL   │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  POST /api/orders            │  ✅    │  ✅   │ ❌  │   ✅    │  ✅   │ │           │
│  │  │  (order:write)               │        │ DEV   │     │ MANAGER │ DEV   │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  PUT  /api/orders/{id}/approve│ ✅    │  ❌   │ ❌  │   ✅    │  ❌   │ │           │
│  │  │  (order:approve)             │        │       │     │ MANAGER │       │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  GET  /api/reports/finance   │  ✅    │  ❌   │ ✅  │   ✅    │  ✅   │ │           │
│  │  │  (report:finance)            │        │       │ACCT │ MANAGER │ ACCT  │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  GET  /api/reports/engineering│ ✅    │  ✅   │ ❌  │   ❌    │  ✅   │ │           │
│  │  │  (report:engineering)        │        │ DEV   │     │         │ DEV   │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  POST /api/reports/export    │  ✅    │  ❌   │ ✅  │   ✅    │  ✅   │ │           │
│  │  │  (report:export)             │        │       │ACCT │ MANAGER │ ACCT  │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  deploy:staging              │  ✅    │  ✅   │ ❌  │   ❌    │  ✅   │ │           │
│  │  │                              │        │DEPLOY │     │         │DEPLOY │ │           │
│  │  ├──────────────────────────────┼────────┼───────┼─────┼─────────┼───────┤ │           │
│  │  │  deploy:production           │  ✅    │  ✅   │ ❌  │   ❌    │  ✅   │ │           │
│  │  │                              │        │DEPLOY │     │         │DEPLOY │ │           │
│  │  └──────────────────────────────┴────────┴───────┴─────┴─────────┴───────┘ │           │
│  │                                                                               │           │
│  │  ★ diana (ENGINEERING + FINANCE) can see BOTH engineering AND finance       │           │
│  │    reports — cross-team visibility through group membership!                │           │
│  │                                                                               │           │
│  │  ★ charlie (MANAGEMENT group) can approve orders but CANNOT deploy —        │           │
│  │    permissions are strictly scoped to what each role provides.              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.28 ★ Complete Flow — Request Authorization Step by Step

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ FLOW — alice calls PUT /api/orders/55/approve                                           │
│    alice: VIEWER (direct) + ENGINEERING group → DEVELOPER + DEPLOYER                       │
│    Required permission: "order:approve"                                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  PUT /api/orders/55/approve                                                                  │
│  Authorization: Basic alice:alice123                                                         │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  1. Authentication (already done on previous request / session)             │             │
│  │     SecurityContext contains:                                                │             │
│  │     ┌──────────────────────────────────────────────────────────┐           │             │
│  │     │  username: "alice"                                        │           │             │
│  │     │  authorities:                                             │           │             │
│  │     │    ROLE_VIEWER, ROLE_DEVELOPER, ROLE_DEPLOYER,           │           │             │
│  │     │    user:read, order:read, code:read,           ← VIEWER  │           │             │
│  │     │    code:read, code:write, deploy:staging,     ← DEVELOPER│           │             │
│  │     │    report:engineering, order:read, order:write,          │           │             │
│  │     │    deploy:staging, deploy:production           ← DEPLOYER│           │             │
│  │     └──────────────────────────────────────────────────────────┘           │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  2. URL-level check (SecurityFilterChain):                                  │             │
│  │     /api/orders/55/approve → matches .anyRequest().authenticated()         │             │
│  │     alice IS authenticated → PASS ✅                                        │             │
│  └──────────────────────────────┬─────────────────────────────────────────────┘             │
│                                  │                                                            │
│                                  ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  3. Method-level check (@PreAuthorize):                                     │             │
│  │     @PreAuthorize("hasAuthority('order:approve') "                         │             │
│  │         + "and @orderSecurityService.isNotCreator(#orderId, ...)")          │             │
│  │                                                                              │             │
│  │     a. hasAuthority('order:approve')                                        │             │
│  │        → Search alice's authorities for "order:approve"                     │             │
│  │        → alice has: ROLE_VIEWER, ROLE_DEVELOPER, ROLE_DEPLOYER,            │             │
│  │          user:read, order:read, code:read, code:write,                     │             │
│  │          deploy:staging, report:engineering, order:write,                   │             │
│  │          deploy:production                                                  │             │
│  │        → "order:approve" NOT FOUND ❌                                       │             │
│  │                                                                              │             │
│  │     b. Short-circuit AND: first condition FALSE → entire expression FALSE  │             │
│  │        → AccessDeniedException thrown                                        │             │
│  │        → 403 Forbidden returned to alice                                    │             │
│  │                                                                              │             │
│  │     ★ WHY: alice is a DEVELOPER/DEPLOYER — she can write code and deploy, │             │
│  │       but she CANNOT approve orders. Only MANAGER role has "order:approve". │             │
│  │       charlie (MANAGEMENT group → MANAGER role) CAN approve.              │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│                                                                                              │
│  ── SAME REQUEST BY charlie ──────────────────────────────────────────────────              │
│                                                                                              │
│  charlie's authorities: ROLE_VIEWER, ROLE_MANAGER,                                          │
│    user:read, order:read, code:read,               ← VIEWER                                │
│    user:read, user:write, order:read, order:write,  ← MANAGER                              │
│    order:approve, order:cancel, report:finance, report:export                               │
│                                                                                              │
│  hasAuthority('order:approve') → FOUND ✅                                                   │
│  @orderSecurityService.isNotCreator(55, "charlie") → TRUE ✅                                │
│  → AuthorizationDecision = GRANTED ✅ → order approved!                                    │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 21.29 ★ Custom PermissionEvaluator — Advanced Permission Checking (Optional)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ OPTIONAL — CUSTOM PermissionEvaluator FOR hasPermission() IN SpEL                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Instead of hasAuthority('order:approve'), you can use:                      │           │
│  │  @PreAuthorize("hasPermission(#orderId, 'Order', 'APPROVE')")               │           │
│  │                                                                               │           │
│  │  This delegates to a custom PermissionEvaluator bean where you can          │           │
│  │  implement complex logic (object-level permissions, ACLs, etc.)             │           │
│  │                                                                               │           │
│  │  ★ hasAuthority() is simpler and sufficient for most cases.                 │           │
│  │  ★ hasPermission() is for when you need object-instance-level checks.      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  CustomPermissionEvaluator.java — Advanced permission checking
//
//  ★ Enables hasPermission() in SpEL expressions
//  ★ Can check permissions on specific OBJECTS (not just string authorities)
//  ★ Useful for: "Can alice approve THIS specific order?" (object-level)
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final OrderRepository orderRepository;

    public CustomPermissionEvaluator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ── hasPermission(authentication, targetObject, permission) ──────────────
    //    Called when: @PreAuthorize("hasPermission(returnObject, 'APPROVE')")
    @Override
    public boolean hasPermission(Authentication authentication,
                                  Object targetDomainObject,
                                  Object permission) {

        if (targetDomainObject == null) return false;

        String perm = (String) permission;
        String username = authentication.getName();

        if (targetDomainObject instanceof Order order) {
            return switch (perm) {
                case "APPROVE" ->
                    // Can approve if has order:approve AND is NOT the creator
                    hasAuthority(authentication, "order:approve")
                        && !order.getCreatedBy().equals(username);
                case "CANCEL" ->
                    // Can cancel if has order:cancel OR is the creator
                    hasAuthority(authentication, "order:cancel")
                        || order.getCreatedBy().equals(username);
                case "READ" ->
                    hasAuthority(authentication, "order:read");
                default -> false;
            };
        }

        return false;
    }

    // ── hasPermission(authentication, targetId, targetType, permission) ──────
    //    Called when: @PreAuthorize("hasPermission(#orderId, 'Order', 'APPROVE')")
    @Override
    public boolean hasPermission(Authentication authentication,
                                  Serializable targetId,
                                  String targetType,
                                  Object permission) {

        if ("Order".equals(targetType)) {
            Long orderId = (Long) targetId;
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) return false;
            return hasPermission(authentication, order, permission);
        }

        return false;
    }

    // ── Helper ──
    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
            .anyMatch(ga -> ga.getAuthority().equals(authority));
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  MethodSecurityConfig.java — Register the custom PermissionEvaluator
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler =
            new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Using hasPermission() in controllers
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/orders")
public class OrderControllerV2 {

    // ── Using hasPermission with target ID and type ──────────────────────────
    //    Spring calls CustomPermissionEvaluator.hasPermission(auth, 55, "Order", "APPROVE")
    @PutMapping("/{orderId}/approve")
    @PreAuthorize("hasPermission(#orderId, 'Order', 'APPROVE')")
    public OrderDto approveOrder(@PathVariable Long orderId) {
        return orderService.approve(orderId);
    }

    // ── Using hasPermission with returnObject ────────────────────────────────
    //    Spring calls CustomPermissionEvaluator.hasPermission(auth, orderObj, "READ")
    @GetMapping("/{orderId}")
    @PostAuthorize("hasPermission(returnObject, 'READ')")
    public Order getOrder(@PathVariable Long orderId) {
        return orderRepository.findById(orderId).orElseThrow();
    }
}
```

---

#### 21.30 ★ Summary — Complete Role + Permission + Group Authorization

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — ENTERPRISE RBAC WITH ROLES, PERMISSIONS, AND GROUPS                          │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── DATA MODEL ───────────────────────────────────────────────────           │           │
│  │  • Permission: fine-grained action (user:read, order:approve)               │           │
│  │  • Role: coarse-grained identity (ADMIN, DEVELOPER, ACCOUNTANT)             │           │
│  │  • Group: organizational unit (ENGINEERING, FINANCE)                         │           │
│  │  • Role ←→ Permission: many-to-many                                         │           │
│  │  • User ←→ Role: many-to-many (direct assignment)                           │           │
│  │  • User ←→ Group: many-to-many                                              │           │
│  │  • Group ←→ Role: many-to-many (inherited roles)                            │           │
│  │                                                                               │           │
│  │  ── FLATTENING AT LOGIN ──────────────────────────────────────────           │           │
│  │  • CustomUserDetailsService collects ALL roles (direct + from groups)       │           │
│  │  • Roles become "ROLE_xxx" authorities → used by hasRole()                  │           │
│  │  • Permissions become "xxx" authorities → used by hasAuthority()            │           │
│  │  • Both merged into ONE Set<GrantedAuthority>                               │           │
│  │                                                                               │           │
│  │  ── AUTHORIZATION STRATEGY ───────────────────────────────────────           │           │
│  │  • URL-level: SecurityFilterChain → coarse-grained (/admin/** = ADMIN)     │           │
│  │  • Method-level: @PreAuthorize → fine-grained (hasAuthority('order:write'))│           │
│  │  • Combine role + permission: hasRole('ADMIN') and hasAuthority('x:delete')│           │
│  │  • Spring beans: @beanService.customCheck(#param, authentication.name)     │           │
│  │  • Custom PermissionEvaluator: hasPermission(#id, 'Type', 'ACTION')        │           │
│  │                                                                               │           │
│  │  ── KEY DESIGN DECISIONS ─────────────────────────────────────────           │           │
│  │  • hasRole() for ROLE checks (adds ROLE_ prefix automatically)             │           │
│  │  • hasAuthority() for PERMISSION checks (exact string match)               │           │
│  │  • Groups simplify admin: add user to ENGINEERING → auto-gets DEVELOPER    │           │
│  │  • Cross-team users: join multiple groups → accumulate all permissions     │           │
│  │  • Principle of Least Privilege: each role has ONLY needed permissions     │           │
│  │  • Delete operations: require BOTH admin role AND delete permission        │           │
│  │                                                                               │           │
│  │  ── CLASSES TO REMEMBER ──────────────────────────────────────────           │           │
│  │  • AppUser (entity) → username, password, Set<Role>, Set<UserGroup>        │           │
│  │  • Role (entity) → name, Set<Permission>                                    │           │
│  │  • Permission (entity) → name (e.g. "user:read")                           │           │
│  │  • UserGroup (entity) → name, Set<Role>                                     │           │
│  │  • CustomUserDetailsService → flattens hierarchy into GrantedAuthority     │           │
│  │  • SecurityConfig → @EnableMethodSecurity + SecurityFilterChain            │           │
│  │  • Controllers → @PreAuthorize("hasAuthority('permission:name')")          │           │
│  │  • CustomPermissionEvaluator (optional) → object-level permission checks   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```


### 7. Database-Based Authentication — Full Production Implementation

---

#### 7.1 Architecture Overview — How Database Authentication Works

Instead of `InMemoryUserDetailsManager` (HashMap), we store users in a **real database** using JPA. Spring Security's authentication flow remains the same — only the `UserDetailsService` implementation changes.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│          DATABASE-BASED AUTHENTICATION — COMPLETE ARCHITECTURE                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  HTTP Request (POST /login or any secured endpoint)                                         │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐                    │
│  │  Spring Security Filter Chain                                       │                    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │                    │
│  │  │  UsernamePasswordAuthenticationFilter                         │  │                    │
│  │  │  → Extracts username & password from request                 │  │                    │
│  │  │  → Creates UsernamePasswordAuthenticationToken                │  │                    │
│  │  └────────────────────────┬──────────────────────────────────────┘  │                    │
│  └───────────────────────────┼────────────────────────────────────────┘                     │
│                              │                                                               │
│                              ▼                                                               │
│  ┌─────────────────────────────────────────────────────────┐                                │
│  │  ProviderManager (AuthenticationManager)                 │                                │
│  │       │                                                  │                                │
│  │       ▼                                                  │                                │
│  │  DaoAuthenticationProvider                               │                                │
│  │  ┌──────────────────────────────────────────────────┐   │                                │
│  │  │  1. Call UserDetailsService.loadUserByUsername()   │   │                                │
│  │  │  2. Compare passwords with PasswordEncoder        │   │                                │
│  │  │  3. Check account status (locked, expired, etc.)  │   │                                │
│  │  └──────────┬────────────────────────┬──────────────┘   │                                │
│  └─────────────┼────────────────────────┼──────────────────┘                                │
│                │                        │                                                    │
│       ┌────────┘                        └────────┐                                           │
│       ▼                                          ▼                                           │
│  ┌──────────────────────────────┐  ┌──────────────────────────────────┐                    │
│  │  UserAuthEntityService       │  │  BCryptPasswordEncoder            │                    │
│  │  (implements UserDetails-    │  │  (PasswordEncoder bean)           │                    │
│  │   Service)                   │  │                                   │                    │
│  │                              │  │  matches(rawPassword,             │                    │
│  │  loadUserByUsername("john")  │  │         encodedPassword)          │                    │
│  │       │                      │  └──────────────────────────────────┘                    │
│  │       ▼                      │                                                           │
│  │  UserAuthRepository          │                                                           │
│  │  .findByUsername("john")     │                                                           │
│  │       │                      │                                                           │
│  └───────┼──────────────────────┘                                                           │
│          │                                                                                   │
│          ▼                                                                                   │
│  ┌──────────────────────────────┐                                                           │
│  │  DATABASE (MySQL/PostgreSQL) │                                                           │
│  │  ┌────────────────────────┐  │                                                           │
│  │  │  users table            │  │                                                           │
│  │  │  ┌────┬───────┬──────┐ │  │                                                           │
│  │  │  │ id │ name  │ pass │ │  │                                                           │
│  │  │  ├────┼───────┼──────┤ │  │                                                           │
│  │  │  │ 1  │ john  │ $2a… │ │  │                                                           │
│  │  │  │ 2  │ admin │ $2a… │ │  │                                                           │
│  │  │  └────┴───────┴──────┘ │  │                                                           │
│  │  └────────────────────────┘  │                                                           │
│  └──────────────────────────────┘                                                           │
│                                                                                              │
│  Returns: UserAuthEntity (implements UserDetails)                                           │
│  → Spring Security uses getUsername(), getPassword(), getAuthorities(),                     │
│    isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired(),                   │
│    isEnabled() to authenticate and authorize                                                 │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**Project Structure:**

```
src/main/java/com/example/security/
├── entity/
│   └── UserAuthEntity.java          // JPA Entity + implements UserDetails
├── repository/
│   └── UserAuthRepository.java      // JPA Repository
├── service/
│   └── UserAuthEntityService.java   // implements UserDetailsService
├── controller/
│   └── UserAuthController.java      // /auth/register endpoint
├── config/
│   └── SecurityConfig.java          // SecurityFilterChain + PasswordEncoder beans
└── SecurityApplication.java         // @SpringBootApplication
```

---

#### 7.2 Complete Class Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│                          CLASS DIAGRAM — DATABASE AUTH                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────┐                                │
│  │  <<interface>> UserDetails                               │  (Spring Security)             │
│  │  (org.springframework.security.core.userdetails)         │                                │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  + getUsername(): String                                  │                                │
│  │  + getPassword(): String                                  │                                │
│  │  + getAuthorities(): Collection<GrantedAuthority>        │                                │
│  │  + isAccountNonExpired(): boolean                         │                                │
│  │  + isAccountNonLocked(): boolean                          │                                │
│  │  + isCredentialsNonExpired(): boolean                     │                                │
│  │  + isEnabled(): boolean                                   │                                │
│  └──────────────────────┬──────────────────────────────────┘                                │
│                         │ implements                                                         │
│                         │                                                                    │
│  ┌──────────────────────▼──────────────────────────────────┐                                │
│  │  @Entity UserAuthEntity                                  │  (YOUR class)                  │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  - id: Long              (@Id @GeneratedValue)           │                                │
│  │  - username: String      (@Column unique, not null)      │                                │
│  │  - password: String      (BCrypt hash)                   │                                │
│  │  - role: String          ("ROLE_USER", "ROLE_ADMIN")     │                                │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  + getAuthorities(): → [SimpleGrantedAuthority(role)]    │                                │
│  │  + getUsername(): → this.username                         │                                │
│  │  + getPassword(): → this.password                         │                                │
│  │  + isAccountNonExpired(): → true                          │                                │
│  │  + isAccountNonLocked(): → true                           │                                │
│  │  + isCredentialsNonExpired(): → true                      │                                │
│  │  + isEnabled(): → true                                    │                                │
│  └──────────────────────┬──────────────────────────────────┘                                │
│                         │ used by                                                            │
│                         │                                                                    │
│  ┌──────────────────────▼──────────────────────────────────┐                                │
│  │  UserAuthRepository extends JpaRepository<UserAuth, Long>│                                │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  + findByUsername(String): Optional<UserAuthEntity>       │                                │
│  └──────────────────────┬──────────────────────────────────┘                                │
│                         │ injected into                                                      │
│                         │                                                                    │
│  ┌──────────────────────▼──────────────────────────────────┐                                │
│  │  <<interface>> UserDetailsService                        │  (Spring Security)             │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  + loadUserByUsername(String): UserDetails                │                                │
│  └──────────────────────┬──────────────────────────────────┘                                │
│                         │ implements                                                         │
│                         │                                                                    │
│  ┌──────────────────────▼──────────────────────────────────┐                                │
│  │  @Service UserAuthEntityService                          │  (YOUR class)                  │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  - userAuthRepository: UserAuthRepository                │                                │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  + loadUserByUsername(String):                            │                                │
│  │    → repo.findByUsername(username)                        │                                │
│  │    → return UserAuthEntity (IS-A UserDetails)             │                                │
│  │    → or throw UsernameNotFoundException                   │                                │
│  └─────────────────────────────────────────────────────────┘                                │
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────┐                                │
│  │  @RestController UserAuthController ("/auth")            │                                │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  - userAuthRepository: UserAuthRepository                │                                │
│  │  - passwordEncoder: PasswordEncoder                      │                                │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  + POST /auth/register → saves new user with BCrypt hash│                                │
│  └─────────────────────────────────────────────────────────┘                                │
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────┐                                │
│  │  @Configuration SecurityConfig                           │                                │
│  ├─────────────────────────────────────────────────────────┤                                │
│  │  + @Bean SecurityFilterChain:                             │                                │
│  │    → permitAll("/auth/**")                                │                                │
│  │    → authenticate everything else                         │                                │
│  │  + @Bean PasswordEncoder:                                 │                                │
│  │    → BCryptPasswordEncoder(12)                            │                                │
│  │  + @Bean AuthenticationProvider:                           │                                │
│  │    → DaoAuthenticationProvider with UserAuthEntityService │                                │
│  └─────────────────────────────────────────────────────────┘                                │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 7.3 Full Code Implementation

##### 7.3.1 `UserAuthEntity.java` — JPA Entity implementing UserDetails

```java
package com.example.security.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class UserAuthEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;  // Stored as BCrypt hash: "$2a$12$..."

    @Column(nullable = false)
    private String role;  // e.g., "ROLE_USER", "ROLE_ADMIN"

    // ─────────────────────────────────────────────────────────
    //  Default constructor (required by JPA)
    // ─────────────────────────────────────────────────────────
    public UserAuthEntity() {}

    // ─────────────────────────────────────────────────────────
    //  Parameterized constructor
    // ─────────────────────────────────────────────────────────
    public UserAuthEntity(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // ═════════════════════════════════════════════════════════
    //  UserDetails interface methods (Spring Security contract)
    // ═════════════════════════════════════════════════════════

    /**
     * Returns the authorities (roles/permissions) granted to the user.
     * Spring Security uses this for AUTHORIZATION checks.
     * 
     * .hasRole("ADMIN") internally checks for "ROLE_ADMIN" in this collection.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert the role string to a GrantedAuthority object
        return List.of(new SimpleGrantedAuthority(this.role));
        // "ROLE_USER" → SimpleGrantedAuthority("ROLE_USER")
        // "ROLE_ADMIN" → SimpleGrantedAuthority("ROLE_ADMIN")
    }

    /**
     * Returns the password (BCrypt hash) used to authenticate the user.
     * DaoAuthenticationProvider calls this to get the stored hash
     * and compares it with the raw password from the login form.
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * Returns the username used to authenticate the user.
     * DaoAuthenticationProvider calls this for username matching.
     */
    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * Indicates whether the user's account has expired.
     * An expired account cannot be authenticated.
     * 
     * Use case: Trial accounts that expire after 30 days.
     * Return false → throws AccountExpiredException during authentication.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;  // Account never expires (simplest case)
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * A locked user cannot be authenticated.
     * 
     * Use case: Lock account after 5 failed login attempts.
     * Return false → throws LockedException during authentication.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;  // Account never locks (simplest case)
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * Expired credentials prevent authentication.
     * 
     * Use case: Force password change every 90 days.
     * Return false → throws CredentialsExpiredException during authentication.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // Credentials never expire (simplest case)
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * A disabled user cannot be authenticated.
     * 
     * Use case: Admin disables a user account, or user hasn't verified email.
     * Return false → throws DisabledException during authentication.
     */
    @Override
    public boolean isEnabled() {
        return true;  // User is always enabled (simplest case)
    }

    // ─────────────────────────────────────────────────────────
    //  Getters and Setters for JPA fields
    // ─────────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
```

**What the database table looks like:**

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  TABLE: users                                                                    │
├──────┬───────────┬──────────────────────────────────────────────┬───────────────┤
│  id  │ username  │ password                                      │ role          │
├──────┼───────────┼──────────────────────────────────────────────┼───────────────┤
│  1   │ john      │ $2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb7YCp3bVn... │ ROLE_USER     │
│  2   │ jane      │ $2a$12$xP2aB7cDe9fGhI3jKlMnO4pQrStUvWxYz... │ ROLE_USER     │
│  3   │ admin     │ $2a$12$Kp9W5xYzAb3Cd4Ef5Gh6Ij7Kl8Mn9Op0Qr... │ ROLE_ADMIN    │
├──────┴───────────┴──────────────────────────────────────────────┴───────────────┤
│  DDL (auto-generated by JPA/Hibernate):                                          │
│                                                                                  │
│  CREATE TABLE users (                                                            │
│      id BIGINT AUTO_INCREMENT PRIMARY KEY,                                       │
│      username VARCHAR(255) NOT NULL UNIQUE,                                      │
│      password VARCHAR(255) NOT NULL,                                             │
│      role VARCHAR(255) NOT NULL                                                  │
│  );                                                                              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 7.3.2 `UserAuthRepository.java` — JPA Repository

```java
package com.example.security.repository;

import com.example.security.entity.UserAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAuthRepository extends JpaRepository<UserAuthEntity, Long> {

    /**
     * Spring Data JPA query derivation:
     * Method name → SQL query automatically
     * 
     * findByUsername("john") →
     *   SELECT * FROM users WHERE username = 'john'
     * 
     * Returns Optional because the user may not exist.
     * This is called by UserAuthEntityService.loadUserByUsername()
     */
    Optional<UserAuthEntity> findByUsername(String username);
}
```

**How Spring Data derives the query:**

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  QUERY DERIVATION — findByUsername                                                │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Method:  findByUsername(String username)                                        │
│           ────┬──────────                                                        │
│               │                                                                  │
│            findBy  +  Username                                                   │
│            (query)    (field name in UserAuthEntity)                             │
│                                                                                  │
│  Generated SQL:                                                                  │
│  SELECT u FROM UserAuthEntity u WHERE u.username = :username                    │
│                                                                                  │
│  Return type: Optional<UserAuthEntity>                                          │
│  → Returns Optional.empty() if no user found                                    │
│  → Returns Optional.of(entity) if user found                                    │
│                                                                                  │
│  Why Optional?                                                                   │
│  → Forces the caller (Service layer) to handle the "not found" case            │
│  → Avoids NullPointerException                                                  │
│  → Service layer throws UsernameNotFoundException when empty                    │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 7.3.3 `UserAuthEntityService.java` — UserDetailsService Implementation

```java
package com.example.security.service;

import com.example.security.entity.UserAuthEntity;
import com.example.security.repository.UserAuthRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAuthEntityService implements UserDetailsService {

    private final UserAuthRepository userAuthRepository;

    // Constructor injection (recommended over @Autowired)
    public UserAuthEntityService(UserAuthRepository userAuthRepository) {
        this.userAuthRepository = userAuthRepository;
    }

    /**
     * This method is called by DaoAuthenticationProvider during authentication.
     * 
     * Spring Security calls: loadUserByUsername("john")
     * → We query the database for the user
     * → Return UserAuthEntity (which IS-A UserDetails)
     * → Spring Security then compares passwords, checks account status
     * 
     * If user not found: MUST throw UsernameNotFoundException
     * → DaoAuthenticationProvider catches it and throws BadCredentialsException
     *   (to avoid leaking info about which usernames exist)
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Query DB: SELECT * FROM users WHERE username = ?
        return userAuthRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + username)
                );

        // UserAuthEntity implements UserDetails, so we can return it directly.
        // No need to create a separate User.builder()... object.
        // Spring Security calls:
        //   entity.getUsername()       → for username comparison
        //   entity.getPassword()       → for password verification (BCrypt)
        //   entity.getAuthorities()    → for role-based authorization
        //   entity.isEnabled()         → pre-authentication checks
        //   entity.isAccountNonLocked()→ pre-authentication checks
        //   entity.isAccountNonExpired() → pre-authentication checks
        //   entity.isCredentialsNonExpired() → post-authentication checks
    }
}
```

**Internal flow when `loadUserByUsername` is called:**

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  DaoAuthenticationProvider → loadUserByUsername() FLOW                                │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  DaoAuthenticationProvider.authenticate(authToken):                                  │
│       │                                                                              │
│       │  username = authToken.getName();  // "john"                                 │
│       │                                                                              │
│       ▼                                                                              │
│  UserDetails user = userDetailsService.loadUserByUsername("john");                   │
│       │                                                                              │
│       ▼                                                                              │
│  UserAuthEntityService.loadUserByUsername("john"):                                   │
│       │                                                                              │
│       ▼                                                                              │
│  userAuthRepository.findByUsername("john")                                           │
│       │                                                                              │
│       ▼                                                                              │
│  Hibernate executes:                                                                 │
│  SELECT id, username, password, role FROM users WHERE username = 'john'             │
│       │                                                                              │
│       ├── Found? → Returns UserAuthEntity { id=1, username="john",                  │
│       │            password="$2a$12$...", role="ROLE_USER" }                         │
│       │            (This IS-A UserDetails — returned directly)                       │
│       │                                                                              │
│       └── Not Found? → throw UsernameNotFoundException("User not found: john")      │
│                        → DaoAuthenticationProvider catches this                      │
│                        → Throws BadCredentialsException("Bad credentials")          │
│                        → Login fails with generic error (no info leak)              │
│                                                                                      │
│  Back in DaoAuthenticationProvider:                                                  │
│       │                                                                              │
│       ├── PRE-AUTH checks (AbstractUserDetailsAuthenticationProvider):               │
│       │   user.isAccountNonLocked()?   → false? throw LockedException              │
│       │   user.isEnabled()?            → false? throw DisabledException             │
│       │   user.isAccountNonExpired()?  → false? throw AccountExpiredException       │
│       │                                                                              │
│       ├── PASSWORD CHECK:                                                            │
│       │   passwordEncoder.matches("rawPassword", user.getPassword())                │
│       │   BCryptPasswordEncoder.matches("john123", "$2a$12$...") → true/false       │
│       │                                                                              │
│       ├── POST-AUTH checks:                                                          │
│       │   user.isCredentialsNonExpired()? → false? throw CredentialsExpiredException│
│       │                                                                              │
│       └── SUCCESS → Return authenticated UsernamePasswordAuthenticationToken        │
│           with authorities from user.getAuthorities()                                │
│                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 7.3.4 `UserAuthController.java` — Registration Endpoint

```java
package com.example.security.controller;

import com.example.security.entity.UserAuthEntity;
import com.example.security.repository.UserAuthRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class UserAuthController {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAuthController(UserAuthRepository userAuthRepository,
                              PasswordEncoder passwordEncoder) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * POST /auth/register
     * 
     * Request body (JSON):
     * {
     *   "username": "john",
     *   "password": "john123",
     *   "role": "ROLE_USER"
     * }
     * 
     * This endpoint is PERMITTED (no authentication required)
     * because SecurityConfig has: requestMatchers("/auth/**").permitAll()
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserAuthEntity user) {

        // Check if username already exists
        if (userAuthRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Username already exists: " + user.getUsername());
        }

        // CRITICAL: Hash the password before saving to DB!
        // Raw password: "john123"
        // After encoding: "$2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb7YCp3bVnGJ0i3IuZpZpx9uBfGHJKS"
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set default role if not provided
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("ROLE_USER");
        }

        // Save to database
        userAuthRepository.save(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully: " + user.getUsername());
    }
}
```

**Registration Flow Diagram:**

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  USER REGISTRATION FLOW                                                              │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  Client (Postman / Frontend):                                                        │
│                                                                                      │
│  POST /auth/register                                                                │
│  Content-Type: application/json                                                      │
│  {                                                                                   │
│    "username": "john",                                                               │
│    "password": "john123",        ← Raw plain text password                          │
│    "role": "ROLE_USER"                                                               │
│  }                                                                                   │
│       │                                                                              │
│       ▼                                                                              │
│  Spring Security Filter Chain:                                                       │
│  → requestMatchers("/auth/**").permitAll()                                          │
│  → No authentication needed! Request passes through.                                │
│       │                                                                              │
│       ▼                                                                              │
│  UserAuthController.registerUser():                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                              │   │
│  │  1. Check if username exists:                                                │   │
│  │     repo.findByUsername("john") → Optional.empty() → OK, proceed            │   │
│  │                                                                              │   │
│  │  2. Hash the password:                                                       │   │
│  │     passwordEncoder.encode("john123")                                        │   │
│  │     → BCryptPasswordEncoder hashes with random salt                          │   │
│  │     → "$2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb7YCp3bVnGJ0i3IuZpZpx9uBfGHJKS"    │   │
│  │                                                                              │   │
│  │  3. Set default role if null:                                                │   │
│  │     role = "ROLE_USER"                                                       │   │
│  │                                                                              │   │
│  │  4. Save to database:                                                        │   │
│  │     repo.save(UserAuthEntity {                                               │   │
│  │       username: "john",                                                      │   │
│  │       password: "$2a$12$LJ3m4...",  ← HASHED, not plain text!              │   │
│  │       role: "ROLE_USER"                                                      │   │
│  │     })                                                                       │   │
│  │     → INSERT INTO users (username, password, role) VALUES (?, ?, ?)         │   │
│  │                                                                              │   │
│  │  5. Return: 201 Created "User registered successfully: john"                │   │
│  │                                                                              │   │
│  └──────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                      │
│  ⚠️ NEVER store the raw password! Always hash BEFORE saving.                        │
│  ⚠️ If you save "john123" instead of the hash, authentication will FAIL because     │
│     BCryptPasswordEncoder.matches("john123", "john123") → FALSE                     │
│     (BCrypt expects a valid BCrypt hash like "$2a$12$...")                           │
│                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 7.3.5 `SecurityConfig.java` — Security Configuration

```java
package com.example.security.config;

import com.example.security.service.UserAuthEntityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserAuthEntityService userAuthEntityService;

    public SecurityConfig(UserAuthEntityService userAuthEntityService) {
        this.userAuthEntityService = userAuthEntityService;
    }

    // ─────────────────────────────────────────────────────────
    //  SecurityFilterChain — HTTP security rules
    // ─────────────────────────────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST APIs (stateless, no browser forms)
            .csrf(csrf -> csrf.disable())

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // /auth/** endpoints are PUBLIC (register, login, etc.)
                .requestMatchers("/auth/**").permitAll()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // Enable HTTP Basic authentication (for REST API testing)
            .httpBasic(Customizer.withDefaults())

            // Enable form-based login (for browser access)
            .formLogin(Customizer.withDefaults());

        return http.build();
    }

    // ─────────────────────────────────────────────────────────
    //  PasswordEncoder — BCrypt with strength 12
    // ─────────────────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
        // No {id} prefix needed — BCrypt is used directly
        // Passwords stored as: "$2a$12$..." in the database
    }

    // ─────────────────────────────────────────────────────────
    //  AuthenticationProvider — wires UserDetailsService + PasswordEncoder
    // ─────────────────────────────────────────────────────────
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        // Tell Spring Security WHERE to find users
        provider.setUserDetailsService(userAuthEntityService);

        // Tell Spring Security HOW to verify passwords
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
        // DaoAuthenticationProvider now does:
        // 1. userAuthEntityService.loadUserByUsername(username) → gets UserAuthEntity from DB
        // 2. passwordEncoder.matches(rawPassword, entity.getPassword()) → BCrypt verification
        // 3. Checks isEnabled(), isAccountNonLocked(), etc.
        // 4. Returns authenticated token if all checks pass
    }
}
```

**How the SecurityFilterChain processes requests:**

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  REQUEST ROUTING — SecurityFilterChain                                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Incoming Request                                                                            │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────┐                     │
│  │  AuthorizationFilter evaluates rules (top to bottom, first match) │                     │
│  │                                                                    │                     │
│  │  Rule 1: requestMatchers("/auth/**").permitAll()                  │                     │
│  │  ├── POST /auth/register    → PERMIT ✓ (no auth needed)         │                     │
│  │  ├── POST /auth/login       → PERMIT ✓ (no auth needed)         │                     │
│  │  └── GET  /auth/anything    → PERMIT ✓ (no auth needed)         │                     │
│  │                                                                    │                     │
│  │  Rule 2: anyRequest().authenticated()                             │                     │
│  │  ├── GET  /hello            → MUST authenticate                  │                     │
│  │  ├── GET  /api/users        → MUST authenticate                  │                     │
│  │  ├── POST /api/orders       → MUST authenticate                  │                     │
│  │  └── ANY  /anything-else    → MUST authenticate                  │                     │
│  │                                                                    │                     │
│  └────────────────────────────────────────────────────────────────────┘                     │
│                                                                                              │
│  For authenticated endpoints:                                                                │
│  ┌───────────────────────────────────────────────────────────────────────────┐              │
│  │  Option A: HTTP Basic (curl / Postman)                                    │              │
│  │  curl -u john:john123 http://localhost:8080/hello                         │              │
│  │  → Authorization: Basic am9objpqb2huMTIz (Base64 encoded)               │              │
│  │  → UsernamePasswordAuthenticationFilter extracts john:john123            │              │
│  │  → DaoAuthenticationProvider → UserAuthEntityService → DB lookup         │              │
│  │  → BCrypt password verification → Success/Failure                         │              │
│  ├───────────────────────────────────────────────────────────────────────────┤              │
│  │  Option B: Form Login (browser)                                           │              │
│  │  → Redirects to /login page                                               │              │
│  │  → User enters username + password                                        │              │
│  │  → POST /login (form data) → Same authentication flow                   │              │
│  └───────────────────────────────────────────────────────────────────────────┘              │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 7.3.6 `application.properties`

```properties
# Database configuration (H2 for development)
spring.datasource.url=jdbc:h2:mem:securitydb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# H2 Console (access at http://localhost:8080/h2-console)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

```properties
# For MySQL (production)
spring.datasource.url=jdbc:mysql://localhost:3306/securitydb
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=rootpass

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

##### 7.3.7 `pom.xml` Dependencies

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Starter Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Spring Boot Starter Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- H2 Database (for development) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- MySQL Connector (for production) -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

---

##### 7.3.8 Testing the Application

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  TESTING WITH curl / Postman                                                     │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Step 1: Register a user (no authentication needed)                             │
│  ─────────────────────────────────────────────────                               │
│  curl -X POST http://localhost:8080/auth/register \                             │
│       -H "Content-Type: application/json" \                                     │
│       -d '{"username":"john","password":"john123","role":"ROLE_USER"}'           │
│                                                                                  │
│  Response: 201 Created                                                           │
│  "User registered successfully: john"                                            │
│                                                                                  │
│  Database now has:                                                                │
│  id=1, username="john", password="$2a$12$...", role="ROLE_USER"                 │
│                                                                                  │
│                                                                                  │
│  Step 2: Access secured endpoint WITHOUT credentials                            │
│  ───────────────────────────────────────────────────                              │
│  curl http://localhost:8080/hello                                                │
│                                                                                  │
│  Response: 401 Unauthorized                                                      │
│                                                                                  │
│                                                                                  │
│  Step 3: Access secured endpoint WITH credentials (HTTP Basic)                  │
│  ──────────────────────────────────────────────────────────────                   │
│  curl -u john:john123 http://localhost:8080/hello                                │
│                                                                                  │
│  Response: 200 OK                                                                │
│  "Hello, john!"                                                                  │
│                                                                                  │
│                                                                                  │
│  Step 4: Access with WRONG password                                              │
│  ───────────────────────────────────                                              │
│  curl -u john:wrongpass http://localhost:8080/hello                               │
│                                                                                  │
│  Response: 401 Unauthorized                                                      │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 7.4 Complete Authentication Sequence — End to End

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  FULL DATABASE AUTHENTICATION SEQUENCE DIAGRAM                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Client                 Filter Chain           AuthProvider        Service         Database  │
│    │                        │                      │                  │               │      │
│    │  GET /hello            │                      │                  │               │      │
│    │  Authorization: Basic  │                      │                  │               │      │
│    │  am9objpqb2huMTIz      │                      │                  │               │      │
│    │───────────────────────>│                      │                  │               │      │
│    │                        │                      │                  │               │      │
│    │                 Decode Base64:                 │                  │               │      │
│    │                 "john:john123"                 │                  │               │      │
│    │                        │                      │                  │               │      │
│    │                 Create token:                  │                  │               │      │
│    │                 UsernamePassword               │                  │               │      │
│    │                 AuthToken("john","john123")    │                  │               │      │
│    │                        │                      │                  │               │      │
│    │                        │  authenticate(token) │                  │               │      │
│    │                        │─────────────────────>│                  │               │      │
│    │                        │                      │                  │               │      │
│    │                        │                      │ loadUserByUsername("john")       │      │
│    │                        │                      │─────────────────>│               │      │
│    │                        │                      │                  │               │      │
│    │                        │                      │                  │ findByUsername │      │
│    │                        │                      │                  │ ("john")      │      │
│    │                        │                      │                  │──────────────>│      │
│    │                        │                      │                  │               │      │
│    │                        │                      │                  │  SELECT *     │      │
│    │                        │                      │                  │  FROM users   │      │
│    │                        │                      │                  │  WHERE        │      │
│    │                        │                      │                  │  username=    │      │
│    │                        │                      │                  │  'john'       │      │
│    │                        │                      │                  │               │      │
│    │                        │                      │                  │ UserAuthEntity│      │
│    │                        │                      │                  │<──────────────│      │
│    │                        │                      │                  │               │      │
│    │                        │                      │  UserAuthEntity  │               │      │
│    │                        │                      │  (UserDetails)   │               │      │
│    │                        │                      │<─────────────────│               │      │
│    │                        │                      │                  │               │      │
│    │                        │               Pre-auth checks:          │               │      │
│    │                        │               isAccountNonLocked? ✓     │               │      │
│    │                        │               isEnabled? ✓              │               │      │
│    │                        │               isAccountNonExpired? ✓    │               │      │
│    │                        │                      │                  │               │      │
│    │                        │               Password check:           │               │      │
│    │                        │               BCrypt.matches(           │               │      │
│    │                        │                 "john123",              │               │      │
│    │                        │                 "$2a$12$...")           │               │      │
│    │                        │               → TRUE ✓                 │               │      │
│    │                        │                      │                  │               │      │
│    │                        │               Post-auth check:          │               │      │
│    │                        │               isCredentialsNonExpired?✓ │               │      │
│    │                        │                      │                  │               │      │
│    │                        │  Authenticated token │                  │               │      │
│    │                        │  with authorities    │                  │               │      │
│    │                        │  [ROLE_USER]         │                  │               │      │
│    │                        │<─────────────────────│                  │               │      │
│    │                        │                      │                  │               │      │
│    │                 Store in SecurityContext       │                  │               │      │
│    │                 Forward to controller          │                  │               │      │
│    │                        │                      │                  │               │      │
│    │  200 OK "Hello, john!" │                      │                  │               │      │
│    │<───────────────────────│                      │                  │               │      │
│    │                        │                      │                  │               │      │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 7.5 Why `UserDetails` Interface Is Compulsory — Deep Dive

Spring Security's entire authentication architecture is built around the `UserDetails` interface. It is the **contract** between your application and Spring Security.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHY UserDetails IS COMPULSORY                                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Spring Security's DaoAuthenticationProvider has this HARDCODED flow:                       │
│                                                                                              │
│  public class DaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {  │
│                                                                                              │
│      private UserDetailsService userDetailsService;  // ← Returns UserDetails               │
│                                                                                              │
│      @Override                                                                               │
│      protected UserDetails retrieveUser(String username, ...) {                             │
│          UserDetails loadedUser = this.userDetailsService                                   │
│                                       .loadUserByUsername(username);                         │
│          //                             ↑ MUST return UserDetails                           │
│          return loadedUser;                                                                  │
│      }                                                                                       │
│                                                                                              │
│      @Override                                                                               │
│      protected void additionalAuthenticationChecks(                                          │
│              UserDetails userDetails,  // ← Expects UserDetails                             │
│              UsernamePasswordAuthenticationToken authentication) {                           │
│                                                                                              │
│          String rawPassword = authentication.getCredentials().toString();                    │
│          String storedHash = userDetails.getPassword();  // ← Calls UserDetails method      │
│                                                                                              │
│          if (!passwordEncoder.matches(rawPassword, storedHash)) {                           │
│              throw new BadCredentialsException("Bad credentials");                           │
│          }                                                                                   │
│      }                                                                                       │
│  }                                                                                           │
│                                                                                              │
│  And in AbstractUserDetailsAuthenticationProvider:                                           │
│                                                                                              │
│  private class DefaultPreAuthenticationChecks implements UserDetailsChecker {               │
│      public void check(UserDetails user) {                                                  │
│          if (!user.isAccountNonLocked()) { throw new LockedException(...); }                │
│          if (!user.isEnabled()) { throw new DisabledException(...); }                       │
│          if (!user.isAccountNonExpired()) { throw new AccountExpiredException(...); }       │
│      }                                                                                       │
│  }                                                                                           │
│                                                                                              │
│  private class DefaultPostAuthenticationChecks implements UserDetailsChecker {              │
│      public void check(UserDetails user) {                                                  │
│          if (!user.isCredentialsNonExpired()) {                                             │
│              throw new CredentialsExpiredException(...);                                     │
│          }                                                                                   │
│      }                                                                                       │
│  }                                                                                           │
│                                                                                              │
│  EVERY method parameter type is UserDetails. NOT Object. NOT your custom class.             │
│  This is a COMPILE-TIME CONTRACT enforced by Java's type system.                            │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**What happens if you DON'T implement UserDetails?**

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHAT HAPPENS WITHOUT UserDetails?                                                           │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Scenario 1: Entity does NOT implement UserDetails                                          │
│  ──────────────────────────────────────────────────                                          │
│                                                                                              │
│  @Entity                                                                                     │
│  public class UserAuthEntity {  // ← NO "implements UserDetails"                            │
│      private Long id;                                                                        │
│      private String username;                                                                │
│      private String password;                                                                │
│      private String role;                                                                    │
│      // only getters/setters, no UserDetails methods                                        │
│  }                                                                                           │
│                                                                                              │
│  @Service                                                                                    │
│  public class UserAuthEntityService implements UserDetailsService {                          │
│      @Override                                                                               │
│      public UserDetails loadUserByUsername(String username) {                                │
│          UserAuthEntity entity = repo.findByUsername(username).orElseThrow(...);             │
│          return entity;  // ❌ COMPILE ERROR!                                               │
│          // Type mismatch: cannot convert from UserAuthEntity to UserDetails                │
│      }                                                                                       │
│  }                                                                                           │
│                                                                                              │
│  RESULT: Your code WON'T COMPILE.                                                           │
│  The method signature demands: public UserDetails loadUserByUsername(String)                 │
│  You cannot return anything that isn't a UserDetails.                                        │
│                                                                                              │
│  ────────────────────────────────────────────────────────────────────────────────            │
│                                                                                              │
│  Scenario 2: Workaround — manually create a UserDetails in Service                         │
│  ──────────────────────────────────────────────────────────────────                          │
│                                                                                              │
│  @Override                                                                                   │
│  public UserDetails loadUserByUsername(String username) {                                    │
│      UserAuthEntity entity = repo.findByUsername(username).orElseThrow(...);                 │
│                                                                                              │
│      // Manually convert to Spring Security's User object                                   │
│      return User.builder()                                                                  │
│              .username(entity.getUsername())                                                 │
│              .password(entity.getPassword())                                                │
│              .roles(entity.getRole().replace("ROLE_", ""))                                  │
│              .build();                                                                      │
│  }                                                                                           │
│                                                                                              │
│  This WORKS but is TEDIOUS and ERROR-PRONE:                                                │
│  ✗ Duplicate mapping logic in every service method                                          │
│  ✗ If entity adds new fields (email, phone), must update mapping                           │
│  ✗ Loses the entity reference — can't access entity-specific fields later                  │
│  ✗ Extra object creation on every authentication request                                    │
│                                                                                              │
│  ────────────────────────────────────────────────────────────────────────────────            │
│                                                                                              │
│  Scenario 3: Entity implements UserDetails (CORRECT approach)                               │
│  ──────────────────────────────────────────────────────────                                   │
│                                                                                              │
│  @Entity                                                                                     │
│  public class UserAuthEntity implements UserDetails { ... }                                 │
│                                                                                              │
│  @Override                                                                                   │
│  public UserDetails loadUserByUsername(String username) {                                    │
│      return repo.findByUsername(username).orElseThrow(...);                                  │
│      // UserAuthEntity IS-A UserDetails → returned directly ✓                              │
│      // No mapping needed ✓                                                                 │
│      // All entity fields available ✓                                                       │
│      // Clean, simple, type-safe ✓                                                          │
│  }                                                                                           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**Why Spring Security ONLY works with UserDetails — the type chain:**

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  THE UserDetails TYPE CHAIN — WHY IT'S THE ONLY OPTION                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  UserDetailsService                                                                          │
│  ├── Method: UserDetails loadUserByUsername(String)                                         │
│  │                  ↑ Return type is UserDetails                                            │
│  │                                                                                          │
│  └── Used by: DaoAuthenticationProvider                                                     │
│               ├── retrieveUser() → calls loadUserByUsername() → gets UserDetails            │
│               ├── additionalAuthenticationChecks(UserDetails user, ...)                     │
│               │                                      ↑ Parameter type is UserDetails        │
│               │   ├── user.getPassword()  → needs UserDetails method                       │
│               │   └── passwordEncoder.matches(raw, user.getPassword())                      │
│               │                                                                             │
│               └── Used by: AbstractUserDetailsAuthenticationProvider                        │
│                            ├── preAuthChecks.check(UserDetails user)                        │
│                            │   ├── user.isAccountNonLocked()   → UserDetails method         │
│                            │   ├── user.isEnabled()            → UserDetails method         │
│                            │   └── user.isAccountNonExpired()  → UserDetails method         │
│                            │                                                                │
│                            ├── postAuthChecks.check(UserDetails user)                       │
│                            │   └── user.isCredentialsNonExpired() → UserDetails method      │
│                            │                                                                │
│                            └── createSuccessAuthentication(UserDetails user, ...)           │
│                                ├── user.getAuthorities() → UserDetails method              │
│                                └── Returns UsernamePasswordAuthenticationToken              │
│                                    with authorities from UserDetails                        │
│                                                                                             │
│  CONCLUSION:                                                                                │
│  ──────────                                                                                  │
│  Spring Security calls these 7 methods on YOUR object during authentication:                │
│  1. getUsername()              — to identify the user                                       │
│  2. getPassword()              — to verify credentials                                      │
│  3. getAuthorities()           — to set roles/permissions                                   │
│  4. isAccountNonExpired()      — pre-auth check                                            │
│  5. isAccountNonLocked()       — pre-auth check                                            │
│  6. isCredentialsNonExpired()  — post-auth check                                           │
│  7. isEnabled()                — pre-auth check                                            │
│                                                                                              │
│  These methods are defined ONLY in UserDetails interface.                                   │
│  Without implementing it, Spring Security has NO WAY to call these methods                  │
│  on your entity — Java's type system simply won't allow it.                                 │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 7.6 The `role` Property and All UserDetails Methods — Complete Reference

---

##### 7.6.1 Why the `role` Property Is Required

The `role` field maps directly to `getAuthorities()` — one of the **mandatory** `UserDetails` methods. Without it, Spring Security cannot perform **authorization** (deciding what the user is allowed to do).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHY role IS REQUIRED                                                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Authentication = WHO are you?  (username + password)                                       │
│  Authorization  = WHAT can you do?  (roles / authorities)                                   │
│                                                                                              │
│  SecurityFilterChain:                                                                        │
│  http.authorizeHttpRequests(auth -> auth                                                    │
│      .requestMatchers("/admin/**").hasRole("ADMIN")      ← needs getAuthorities()           │
│      .requestMatchers("/user/**").hasRole("USER")        ← needs getAuthorities()           │
│      .requestMatchers("/seller/**").hasAnyRole("SELLER", "ADMIN")  ← needs getAuthorities() │
│  );                                                                                          │
│                                                                                              │
│  When Spring evaluates .hasRole("ADMIN"):                                                   │
│  1. Gets Authentication from SecurityContext                                                 │
│  2. Calls authentication.getAuthorities()                                                    │
│     → These come from UserDetails.getAuthorities()                                          │
│     → Which comes from your entity's role field                                             │
│  3. Checks if authorities contain "ROLE_ADMIN"                                              │
│  4. If yes → allow access. If no → 403 Forbidden.                                          │
│                                                                                              │
│  Without role:                                                                               │
│  getAuthorities() returns empty list → EVERY .hasRole() check fails                        │
│  → User can authenticate but CANNOT access any role-protected endpoint                      │
│  → Effectively locked out despite valid credentials                                         │
│                                                                                              │
│  NOTE: .hasRole("ADMIN") internally checks for "ROLE_ADMIN" (adds "ROLE_" prefix)          │
│  So your role field should store "ROLE_USER", "ROLE_ADMIN", etc.                            │
│  Or use .hasAuthority("ADMIN") if you store without the "ROLE_" prefix.                     │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 7.6.2 All Abstract Methods in UserDetails — Complete Reference

```java
// org.springframework.security.core.userdetails.UserDetails

public interface UserDetails extends Serializable {

    // ─── AUTHENTICATION METHODS ───────────────────────────────────────

    /**
     * Returns the username.
     * Used by DaoAuthenticationProvider to match against submitted username.
     */
    String getUsername();

    /**
     * Returns the encoded password (BCrypt hash).
     * Used by DaoAuthenticationProvider + PasswordEncoder for verification.
     */
    String getPassword();

    // ─── AUTHORIZATION METHOD ─────────────────────────────────────────

    /**
     * Returns the authorities (roles/permissions) granted to the user.
     * Used by AuthorizationFilter for access control decisions.
     * 
     * .hasRole("ADMIN") checks this collection for "ROLE_ADMIN"
     * .hasAuthority("READ_PRIVILEGE") checks for exact match
     */
    Collection<? extends GrantedAuthority> getAuthorities();

    // ─── ACCOUNT STATUS METHODS (Pre-Authentication Checks) ──────────

    /**
     * Has the account expired? (e.g., trial period ended)
     * 
     * true  = account is valid (non-expired)
     * false = account has expired → throws AccountExpiredException
     *         → authentication FAILS before password is even checked
     * 
     * Use case: SaaS trial accounts, subscription expiry
     */
    boolean isAccountNonExpired();

    /**
     * Is the account locked? (e.g., too many failed login attempts)
     * 
     * true  = account is unlocked (not locked)
     * false = account is locked → throws LockedException
     *         → authentication FAILS before password is even checked
     * 
     * Use case: Lock after 5 failed attempts, admin-initiated lock
     */
    boolean isAccountNonLocked();

    /**
     * Is the user enabled? (e.g., email verified, admin activated)
     * 
     * true  = user is enabled (active)
     * false = user is disabled → throws DisabledException
     *         → authentication FAILS before password is even checked
     * 
     * Use case: Email verification, admin deactivation, soft delete
     */
    boolean isEnabled();

    // ─── CREDENTIAL STATUS METHOD (Post-Authentication Check) ────────

    /**
     * Have the credentials (password) expired?
     * This is checked AFTER successful password verification.
     * 
     * true  = credentials are valid (non-expired)
     * false = credentials have expired → throws CredentialsExpiredException
     *         → authentication FAILS even though password was correct
     * 
     * Use case: Force password change every 90 days (compliance requirement)
     */
    boolean isCredentialsNonExpired();
}
```

**When each check happens during authentication:**

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ORDER OF CHECKS IN DaoAuthenticationProvider                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Step 1: Load user from DB                                                                  │
│  │  userDetailsService.loadUserByUsername("john")                                           │
│  │  → UserAuthEntity loaded from database                                                   │
│  │                                                                                          │
│  ▼                                                                                          │
│  Step 2: PRE-AUTHENTICATION CHECKS  (before password verification!)                        │
│  │  ┌──────────────────────────────────────────────────────────────────────┐                │
│  │  │  user.isAccountNonLocked()    → false? throw LockedException       │                │
│  │  │  user.isEnabled()             → false? throw DisabledException     │                │
│  │  │  user.isAccountNonExpired()   → false? throw AccountExpired...     │                │
│  │  │                                                                    │                │
│  │  │  ⚠️ If ANY returns false, authentication FAILS IMMEDIATELY        │                │
│  │  │     Password is NOT even checked!                                  │                │
│  │  └──────────────────────────────────────────────────────────────────────┘                │
│  │  All passed? ✓ Continue...                                                              │
│  │                                                                                          │
│  ▼                                                                                          │
│  Step 3: PASSWORD VERIFICATION                                                              │
│  │  passwordEncoder.matches(rawPassword, user.getPassword())                                │
│  │  → BCrypt hash comparison                                                               │
│  │  → false? throw BadCredentialsException                                                 │
│  │  All passed? ✓ Continue...                                                              │
│  │                                                                                          │
│  ▼                                                                                          │
│  Step 4: POST-AUTHENTICATION CHECKS  (after password verified!)                            │
│  │  ┌──────────────────────────────────────────────────────────────────────┐                │
│  │  │  user.isCredentialsNonExpired() → false? throw CredentialsExpired  │                │
│  │  │                                                                    │                │
│  │  │  ⚠️ Password was CORRECT but credentials have expired             │                │
│  │  │     User must change password before accessing the system          │                │
│  │  └──────────────────────────────────────────────────────────────────────┘                │
│  │  Passed? ✓ Continue...                                                                  │
│  │                                                                                          │
│  ▼                                                                                          │
│  Step 5: CREATE AUTHENTICATED TOKEN                                                         │
│     → UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())               │
│     → Stored in SecurityContextHolder                                                       │
│     → Request proceeds to the controller                                                    │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 7.6.3 Enhanced Entity with All Possible Properties

In production, your entity would have more fields to support all `UserDetails` methods:

```java
@Entity
@Table(name = "users")
public class UserAuthEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    // ─── Additional properties for UserDetails methods ───────────────

    @Column(name = "account_non_expired")
    private boolean accountNonExpired = true;
    // Use case: SaaS trial expires → set to false

    @Column(name = "account_non_locked")
    private boolean accountNonLocked = true;
    // Use case: 5 failed login attempts → set to false

    @Column(name = "credentials_non_expired")
    private boolean credentialsNonExpired = true;
    // Use case: Password older than 90 days → set to false

    @Column(name = "enabled")
    private boolean enabled = true;
    // Use case: Email not verified → set to false

    // ─── Useful business properties ──────────────────────────────────

    @Column(unique = true)
    private String email;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;
    // Increment on failed login, lock account after 5

    @Column(name = "lock_time")
    private LocalDateTime lockTime;
    // When account was locked, unlock after 30 minutes

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    // Track when password was last changed for expiry

    @Column(name = "account_created_at")
    private LocalDateTime accountCreatedAt;
    // For trial account expiry calculation

    @Column(name = "email_verified")
    private boolean emailVerified = false;
    // Email verification status

    // ─── UserDetails method implementations ──────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role));
    }

    @Override
    public String getPassword() { return this.password; }

    @Override
    public String getUsername() { return this.username; }

    @Override
    public boolean isAccountNonExpired() { return this.accountNonExpired; }

    @Override
    public boolean isAccountNonLocked() { return this.accountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return this.credentialsNonExpired; }

    @Override
    public boolean isEnabled() { return this.enabled; }

    // Getters and setters for all fields...
}
```

**Database table with all columns:**

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  TABLE: users (production schema)                                                            │
├──────┬──────────┬────────────┬───────────┬───────────┬──────────┬──────────┬───────────────┤
│  id  │ username │ password   │ role      │ email     │ enabled  │ locked   │ failed_       │
│      │          │            │           │           │          │          │ attempts      │
├──────┼──────────┼────────────┼───────────┼───────────┼──────────┼──────────┼───────────────┤
│  1   │ john     │ $2a$12$... │ ROLE_USER │ john@...  │ true     │ false    │ 0             │
│  2   │ locked   │ $2a$12$... │ ROLE_USER │ lock@...  │ true     │ true     │ 5             │
│  3   │ newbie   │ $2a$12$... │ ROLE_USER │ new@...   │ false    │ false    │ 0             │
│  4   │ admin    │ $2a$12$... │ ROLE_ADMIN│ adm@...   │ true     │ false    │ 0             │
├──────┴──────────┴────────────┴───────────┴───────────┴──────────┴──────────┴───────────────┤
│                                                                                              │
│  User 1 (john):   Normal user, all checks pass ✓                                           │
│  User 2 (locked): isAccountNonLocked() → false → LockedException thrown                    │
│  User 3 (newbie): isEnabled() → false → DisabledException (email not verified)             │
│  User 4 (admin):  Normal admin, all checks pass ✓                                          │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 7.6.4 Multiple Roles — Using a Roles Collection

For users with **multiple roles**, store roles as a separate table or comma-separated string:

**Approach 1: Comma-separated string (simpler)**

```java
@Entity
@Table(name = "users")
public class UserAuthEntity implements UserDetails {

    @Column(nullable = false)
    private String roles;  // "ROLE_USER,ROLE_ADMIN,ROLE_SELLER"

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.stream(this.roles.split(","))
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        // "ROLE_USER,ROLE_ADMIN" → [SimpleGrantedAuthority("ROLE_USER"),
        //                           SimpleGrantedAuthority("ROLE_ADMIN")]
    }
}
```

**Approach 2: Separate roles table (normalized, production-grade)**

```java
@Entity
@Table(name = "users")
public class UserAuthEntity implements UserDetails {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}

// Creates two tables:
// users: id, username, password, enabled, ...
// user_roles: user_id (FK), role
//
// user_roles:
// ┌─────────┬────────────┐
// │ user_id │ role       │
// ├─────────┼────────────┤
// │ 1       │ ROLE_USER  │
// │ 1       │ ROLE_ADMIN │  ← john has 2 roles
// │ 2       │ ROLE_USER  │  ← jane has 1 role
// └─────────┴────────────┘
```

---

##### 7.6.5 Summary — All UserDetails Methods and Their Properties

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  UserDetails METHODS → ENTITY PROPERTIES → EXCEPTIONS                                       │
├───────────────────────────┬──────────────────────────┬──────────────────────────────────────┤
│  Interface Method          │ Entity Property           │ Exception If false                   │
├───────────────────────────┼──────────────────────────┼──────────────────────────────────────┤
│  getUsername()             │ String username           │ N/A (used for lookup)                │
│  getPassword()             │ String password           │ BadCredentialsException (if mismatch)│
│  getAuthorities()          │ String role / Set<String> │ AccessDeniedException (403 if no role)│
│  isAccountNonExpired()     │ boolean accountNonExpired │ AccountExpiredException              │
│  isAccountNonLocked()      │ boolean accountNonLocked  │ LockedException                      │
│  isCredentialsNonExpired() │ boolean credNonExpired    │ CredentialsExpiredException           │
│  isEnabled()               │ boolean enabled           │ DisabledException                    │
├───────────────────────────┴──────────────────────────┴──────────────────────────────────────┤
│                                                                                              │
│  CHECK ORDER:                                                                                │
│  1. Load user (getUsername, getPassword loaded from DB)                                     │
│  2. PRE-AUTH:  isAccountNonLocked → isEnabled → isAccountNonExpired                        │
│  3. PASSWORD:  passwordEncoder.matches(raw, getPassword())                                  │
│  4. POST-AUTH: isCredentialsNonExpired                                                      │
│  5. AUTHZ:    getAuthorities() used later for .hasRole() checks                            │
│                                                                                              │
│  PRODUCTION TIP:                                                                             │
│  For simple apps, return true for all status methods.                                       │
│  Add real checks only when you need:                                                        │
│  • Account locking (brute force protection)                                                 │
│  • Account expiry (SaaS trials, subscriptions)                                              │
│  • Credential expiry (compliance: password rotation every 90 days)                          │
│  • Enable/disable (email verification, admin deactivation)                                  │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---


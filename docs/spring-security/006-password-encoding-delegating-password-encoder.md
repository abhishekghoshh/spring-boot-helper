### 6. Multiple Users, Password Encoding & DelegatingPasswordEncoder Deep Dive

---

#### 6.1 Creating Multiple Users with InMemoryUserDetailsManager

The auto-configured default creates only **one** user. To create multiple users, you must define your own `UserDetailsService` bean using `InMemoryUserDetailsManager`, which accepts multiple `UserDetails` objects in its constructor.

**When you define this bean, the auto-configuration backs off completely** — no more random password in the console.

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  CREATING MULTIPLE IN-MEMORY USERS                                               │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  @Configuration                                                                  │
│  SecurityConfig                                                                  │
│       │                                                                          │
│       │  @Bean UserDetailsService                                                │
│       ▼                                                                          │
│  ┌──────────────────────────────────────────────────────────┐                   │
│  │  InMemoryUserDetailsManager(user1, user2, user3)         │                   │
│  │                                                          │                   │
│  │  Internal HashMap:                                       │                   │
│  │  ┌────────────┬──────────────────────────────────────┐  │                   │
│  │  │ Key        │ Value (MutableUserDetails)            │  │                   │
│  │  ├────────────┼──────────────────────────────────────┤  │                   │
│  │  │ "john"     │ {pass: "{noop}john123",              │  │                   │
│  │  │            │  roles: [ROLE_USER]}                  │  │                   │
│  │  ├────────────┼──────────────────────────────────────┤  │                   │
│  │  │ "jane"     │ {pass: "{bcrypt}$2a$12$...",         │  │                   │
│  │  │            │  roles: [ROLE_USER, ROLE_SELLER]}     │  │                   │
│  │  ├────────────┼──────────────────────────────────────┤  │                   │
│  │  │ "admin"    │ {pass: "{bcrypt}$2a$12$...",         │  │                   │
│  │  │            │  roles: [ROLE_ADMIN, ROLE_USER]}      │  │                   │
│  │  └────────────┴──────────────────────────────────────┘  │                   │
│  └──────────────────────────────────────────────────────────┘                   │
│                                                                                  │
│  Auto-configuration sees your UserDetailsService bean → BACKS OFF               │
│  No "Using generated security password" in console ✓                            │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example — Multiple Users with {noop} password:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {

        // User 1: Regular customer
        UserDetails user = User.builder()
                .username("john")
                .password("{noop}john123")   // {noop} = plain text, no hashing
                .roles("USER")               // Internally becomes ROLE_USER
                .build();

        // User 2: Seller
        UserDetails seller = User.builder()
                .username("jane")
                .password("{noop}jane123")
                .roles("USER", "SELLER")     // Multiple roles
                .build();

        // User 3: Admin
        UserDetails admin = User.builder()
                .username("admin")
                .password("{noop}admin123")
                .roles("ADMIN", "USER")
                .build();

        // InMemoryUserDetailsManager constructor accepts varargs UserDetails
        return new InMemoryUserDetailsManager(user, seller, admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/seller/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER", "SELLER", "ADMIN")
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

**How InMemoryUserDetailsManager constructor works internally:**

```java
// InMemoryUserDetailsManager accepts varargs
public InMemoryUserDetailsManager(UserDetails... users) {
    for (UserDetails user : users) {
        createUser(user);  // Stores each in the HashMap
    }
}

// createUser stores in HashMap with lowercase key
public void createUser(UserDetails user) {
    this.users.put(user.getUsername().toLowerCase(), new MutableUser(user));
}

// So when we call: new InMemoryUserDetailsManager(user, seller, admin)
// HashMap becomes:
// { "john" → UserDetails, "jane" → UserDetails, "admin" → UserDetails }
```

---

#### 6.2 The `{id}encodedpassword` Format — How It Works

Spring Security uses a **special format** to store passwords that **embeds the encoding algorithm** inside the password string itself:

```
Format:  {id}encodedpassword

Examples:
  {noop}plainTextPassword          → NoOpPasswordEncoder (no hashing)
  {bcrypt}$2a$12$LJ3m4...         → BCryptPasswordEncoder
  {argon2}$argon2id$v=19$m=...    → Argon2PasswordEncoder
  {scrypt}$e0801$...              → SCryptPasswordEncoder
  {pbkdf2}5d923b44...             → Pbkdf2PasswordEncoder
  {sha256}97cde38028ad...         → StandardPasswordEncoder (deprecated)
```

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                  THE {id}encodedpassword FORMAT                                      │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  Password string stored in DB / memory:                                              │
│                                                                                      │
│  ┌──────┬────────────────────────────────────────────────────┐                      │
│  │ {id} │ encodedpassword                                     │                      │
│  ├──────┼────────────────────────────────────────────────────┤                      │
│  │prefix│ the actual hashed/encoded password value            │                      │
│  └──┬───┴────────────────────────────────────┬───────────────┘                      │
│     │                                        │                                       │
│     │  Tells DelegatingPasswordEncoder       │  The hash that the selected           │
│     │  WHICH encoder to use for              │  encoder will verify against           │
│     │  matching this password                │                                       │
│     ▼                                        ▼                                       │
│                                                                                      │
│  Examples:                                                                           │
│                                                                                      │
│  {noop}myPassword123                                                                │
│  ├── id = "noop"                                                                    │
│  ├── encodedPassword = "myPassword123"                                              │
│  └── Encoder: NoOpPasswordEncoder → plain text comparison                           │
│                                                                                      │
│  {bcrypt}$2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb7YCp3bVnGJ0i3IuZpZpx9uBfGHJKS           │
│  ├── id = "bcrypt"                                                                  │
│  ├── encodedPassword = "$2a$12$LJ3m4YXh..."                                        │
│  └── Encoder: BCryptPasswordEncoder → BCrypt hash verification                     │
│                                                                                      │
│  {argon2}$argon2id$v=19$m=4096,t=3,p=1$c29tZXNhbHQ$hash...                        │
│  ├── id = "argon2"                                                                  │
│  ├── encodedPassword = "$argon2id$v=19$..."                                         │
│  └── Encoder: Argon2PasswordEncoder → Argon2 hash verification                     │
│                                                                                      │
│  WHY this format?                                                                    │
│  ─────────────────                                                                   │
│  • Allows DIFFERENT users to have passwords encoded with DIFFERENT algorithms       │
│  • Enables seamless MIGRATION from old encoding (MD5, SHA) to new (BCrypt, Argon2)  │
│  • One user can have {sha256}... while another has {bcrypt}...                      │
│  • On next login, password can be RE-ENCODED with the newer, stronger algorithm     │
│                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

**{noop} — No Operation Password Encoder:**

```java
// {noop} means "no encoding" — the password is stored and compared as plain text

// When you write:
UserDetails user = User.builder()
        .username("john")
        .password("{noop}john123")   // Stored as-is: "{noop}john123"
        .roles("USER")
        .build();

// During authentication:
// 1. User submits password: "john123" (raw, from login form)
// 2. Stored password: "{noop}john123"
// 3. DelegatingPasswordEncoder extracts id = "noop"
// 4. Delegates to NoOpPasswordEncoder
// 5. NoOpPasswordEncoder.matches("john123", "john123") → plain text equals → TRUE ✓
```

```java
// NoOpPasswordEncoder internal implementation
@Deprecated  // Should NEVER be used in production
public final class NoOpPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return rawPassword.toString();  // Returns password as-is, no hashing
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return rawPassword.toString().equals(encodedPassword);  // Simple string comparison
    }
}
```

**Mixing different encodings for different users:**

```java
@Bean
public UserDetailsService userDetailsService() {
    // User 1: Plain text password (dev only!)
    UserDetails user1 = User.builder()
            .username("dev")
            .password("{noop}devpass")           // NoOp — plain text
            .roles("USER")
            .build();

    // User 2: BCrypt encoded password
    UserDetails user2 = User.builder()
            .username("john")
            .password("{bcrypt}$2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb7YCp3bVnGJ0i3IuZpZpx9uBfGHJKS")
            .roles("USER")                       // BCrypt hash of "john123"
            .build();

    // User 3: Argon2 encoded password
    UserDetails user3 = User.builder()
            .username("admin")
            .password("{argon2}$argon2id$v=19$m=16384,t=2,p=1$c29tZXNhbHQ$hashvalue")
            .roles("ADMIN")                      // Argon2 hash of "admin123"
            .build();

    // ALL THREE can coexist! DelegatingPasswordEncoder handles each differently.
    return new InMemoryUserDetailsManager(user1, user2, user3);
}
```

---

#### 6.3 DelegatingPasswordEncoder — How Password Verification Works Internally

`DelegatingPasswordEncoder` is the **default** password encoder in Spring Security. It reads the `{id}` prefix from the stored password to determine which actual encoder to use.

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│          DelegatingPasswordEncoder — COMPLETE INTERNAL FLOW                              │
├──────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  LOGIN REQUEST: username="john", password="secret123" (raw from form)                   │
│       │                                                                                  │
│       ▼                                                                                  │
│  DaoAuthenticationProvider:                                                              │
│  1. UserDetailsService.loadUserByUsername("john")                                       │
│     → Returns UserDetails with password = "{bcrypt}$2a$12$xYz..."                      │
│                                                                                          │
│  2. passwordEncoder.matches("secret123", "{bcrypt}$2a$12$xYz...")                       │
│       │                                                                                  │
│       ▼                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │  DelegatingPasswordEncoder.matches(rawPassword, storedPassword)                  │    │
│  │                                                                                  │    │
│  │  Step 1: Extract the {id} prefix                                                │    │
│  │  ──────────────────────────────                                                  │    │
│  │  storedPassword = "{bcrypt}$2a$12$xYz..."                                       │    │
│  │  id = "bcrypt"                    ← extracted from between { and }              │    │
│  │  encodedPassword = "$2a$12$xYz..."  ← everything after the prefix              │    │
│  │                                                                                  │    │
│  │  Step 2: Look up the encoder for this id                                        │    │
│  │  ────────────────────────────────────                                            │    │
│  │  Map<String, PasswordEncoder> encoders:                                          │    │
│  │  ┌────────────┬──────────────────────────────────────┐                          │    │
│  │  │ "bcrypt"   │ BCryptPasswordEncoder                 │ ◄── MATCH!             │    │
│  │  │ "noop"     │ NoOpPasswordEncoder                   │                          │    │
│  │  │ "argon2"   │ Argon2PasswordEncoder                 │                          │    │
│  │  │ "scrypt"   │ SCryptPasswordEncoder                 │                          │    │
│  │  │ "pbkdf2"   │ Pbkdf2PasswordEncoder                 │                          │    │
│  │  │ "sha256"   │ StandardPasswordEncoder (deprecated)  │                          │    │
│  │  └────────────┴──────────────────────────────────────┘                          │    │
│  │                                                                                  │    │
│  │  PasswordEncoder delegate = encoders.get("bcrypt");                              │    │
│  │  // → BCryptPasswordEncoder                                                      │    │
│  │                                                                                  │    │
│  │  Step 3: Delegate the matching to the specific encoder                          │    │
│  │  ─────────────────────────────────────────────────────                           │    │
│  │  return delegate.matches("secret123", "$2a$12$xYz...");                         │    │
│  │  // BCryptPasswordEncoder hashes "secret123" with the same salt                 │    │
│  │  // and compares with "$2a$12$xYz..."                                            │    │
│  │  // → TRUE ✓ (password matches!)                                                │    │
│  │                                                                                  │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                          │
│  WHAT IF {id} IS NOT FOUND?                                                             │
│  ─────────────────────────────                                                           │
│  If storedPassword has no {id} prefix (e.g., legacy "$2a$12$xYz..."):                   │
│  → Uses defaultPasswordEncoderForMatches (configurable)                                  │
│  → If not configured: throws IllegalArgumentException                                    │
│     "There is no PasswordEncoder mapped for the id \"null\""                            │
│                                                                                          │
│  WHAT ABOUT ENCODING NEW PASSWORDS?                                                     │
│  ──────────────────────────────────                                                      │
│  DelegatingPasswordEncoder.encode("newPassword"):                                        │
│  → Always uses the "default" encoder (idForEncode)                                      │
│  → Default = "bcrypt"                                                                    │
│  → Result: "{bcrypt}$2a$10$..." (always adds {bcrypt} prefix)                           │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

**DelegatingPasswordEncoder — Internal Source Code (Simplified):**

```java
// org.springframework.security.crypto.password.DelegatingPasswordEncoder (simplified)

public class DelegatingPasswordEncoder implements PasswordEncoder {

    private static final String PREFIX = "{";
    private static final String SUFFIX = "}";

    private final String idForEncode;                        // Default encoder id for new passwords
    private final PasswordEncoder passwordEncoderForEncode;  // Default encoder instance
    private final Map<String, PasswordEncoder> idToPasswordEncoder;  // All known encoders
    private PasswordEncoder defaultPasswordEncoderForMatches;  // Fallback for no-prefix passwords

    public DelegatingPasswordEncoder(String idForEncode,
                                      Map<String, PasswordEncoder> idToPasswordEncoder) {
        this.idForEncode = idForEncode;
        this.passwordEncoderForEncode = idToPasswordEncoder.get(idForEncode);
        this.idToPasswordEncoder = new HashMap<>(idToPasswordEncoder);
    }

    // ENCODING a new password (registration, password change)
    @Override
    public String encode(CharSequence rawPassword) {
        // Always uses the default encoder (bcrypt) and adds the {id} prefix
        return PREFIX + this.idForEncode + SUFFIX +
               this.passwordEncoderForEncode.encode(rawPassword);
        // Input:  "secret123"
        // Output: "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"
    }

    // MATCHING a raw password against a stored {id}encodedpassword
    @Override
    public boolean matches(CharSequence rawPassword, String prefixEncodedPassword) {
        if (rawPassword == null && prefixEncodedPassword == null) {
            return true;
        }

        // Step 1: Extract the {id}
        String id = extractId(prefixEncodedPassword);
        // "{bcrypt}$2a$12$xYz..." → id = "bcrypt"

        // Step 2: Find the right encoder
        PasswordEncoder delegate = this.idToPasswordEncoder.get(id);
        // id = "bcrypt" → BCryptPasswordEncoder

        if (delegate == null) {
            // No encoder found for this id — use fallback or throw error
            return this.defaultPasswordEncoderForMatches
                       .matches(rawPassword, prefixEncodedPassword);
        }

        // Step 3: Extract the encoded password (without the {id} prefix)
        String encodedPassword = extractEncodedPassword(prefixEncodedPassword);
        // "{bcrypt}$2a$12$xYz..." → "$2a$12$xYz..."

        // Step 4: Delegate matching to the specific encoder
        return delegate.matches(rawPassword, encodedPassword);
        // BCryptPasswordEncoder.matches("secret123", "$2a$12$xYz...") → true/false
    }

    private String extractId(String prefixEncodedPassword) {
        int start = prefixEncodedPassword.indexOf(PREFIX);
        int end = prefixEncodedPassword.indexOf(SUFFIX, start);
        if (start != 0 || end < 0) {
            return null;  // No {id} prefix found
        }
        return prefixEncodedPassword.substring(start + 1, end);
    }

    private String extractEncodedPassword(String prefixEncodedPassword) {
        int end = prefixEncodedPassword.indexOf(SUFFIX);
        return prefixEncodedPassword.substring(end + 1);
    }
}
```

**How Spring creates the default DelegatingPasswordEncoder:**

```java
// PasswordEncoderFactories.createDelegatingPasswordEncoder() — what Spring uses by default

public final class PasswordEncoderFactories {

    public static PasswordEncoder createDelegatingPasswordEncoder() {
        String encodingId = "bcrypt";  // DEFAULT encoder for NEW passwords

        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt",  new BCryptPasswordEncoder());
        encoders.put("noop",    NoOpPasswordEncoder.getInstance());
        encoders.put("argon2",  new Argon2PasswordEncoder(16, 32, 1, 1 << 14, 2));
        encoders.put("scrypt",  new SCryptPasswordEncoder(16384, 8, 1, 32, 64));
        encoders.put("pbkdf2",  Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("sha256",  new StandardPasswordEncoder());  // deprecated
        encoders.put("ldap",    new LdapShaPasswordEncoder());   // deprecated
        encoders.put("MD5",     new MessageDigestPasswordEncoder("MD5"));  // deprecated

        return new DelegatingPasswordEncoder(encodingId, encoders);
        // encodingId = "bcrypt" means:
        // - encode() always produces {bcrypt}... passwords
        // - matches() reads the {id} to decide which encoder to use
    }
}
```

**Complete Authentication Flow with DelegatingPasswordEncoder — Diagram:**

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│  COMPLETE PASSWORD VERIFICATION FLOW                                                     │
├──────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  User submits login form:                                                                │
│  username = "john"                                                                       │
│  password = "secret123"  (raw text from the form)                                       │
│       │                                                                                  │
│       ▼                                                                                  │
│  UsernamePasswordAuthenticationFilter                                                    │
│  → Creates: UsernamePasswordAuthenticationToken("john", "secret123")                    │
│       │                                                                                  │
│       ▼                                                                                  │
│  ProviderManager → DaoAuthenticationProvider                                             │
│       │                                                                                  │
│       ├── Step 1: Load user                                                              │
│       │   userDetailsService.loadUserByUsername("john")                                  │
│       │   → Returns UserDetails {                                                        │
│       │       username: "john",                                                          │
│       │       password: "{bcrypt}$2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb...",                  │
│       │       authorities: [ROLE_USER]                                                   │
│       │     }                                                                            │
│       │                                                                                  │
│       ├── Step 2: Pre-checks (isEnabled, isAccountNonLocked, etc.)                      │
│       │                                                                                  │
│       ├── Step 3: Password verification                                                  │
│       │   passwordEncoder.matches("secret123",                                           │
│       │                          "{bcrypt}$2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb...")          │
│       │       │                                                                          │
│       │       ▼                                                                          │
│       │   DelegatingPasswordEncoder:                                                     │
│       │   ├── Extract id = "bcrypt"                                                     │
│       │   ├── Lookup encoder → BCryptPasswordEncoder                                    │
│       │   ├── Extract hash = "$2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb..."                     │
│       │   └── BCryptPasswordEncoder.matches("secret123", "$2a$12$LJ3m4...")             │
│       │       ├── Extract salt from stored hash                                          │
│       │       ├── Hash "secret123" with same salt → "$2a$12$LJ3m4..."                  │
│       │       └── Compare hashes → MATCH ✓                                              │
│       │                                                                                  │
│       └── Step 4: Return authenticated token                                             │
│           → SecurityContext stores Authentication                                         │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 6.4 All Password Encoders — Complete Reference

```
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│                     ALL PASSWORD ENCODERS IN SPRING SECURITY                                   │
├──────────────────────┬──────────┬────────────┬─────────────────────────────────────────────────┤
│ Encoder              │ {id}     │ Status     │ Description & Use Case                          │
├──────────────────────┼──────────┼────────────┼─────────────────────────────────────────────────┤
│ BCryptPasswordEncoder│ {bcrypt} │ RECOMMENDED│ Uses BCrypt hashing algorithm with random salt. │
│                      │          │ (DEFAULT)  │ Strength 4-31 (default 10). Intentionally slow  │
│                      │          │            │ to resist brute force. MOST COMMON in production.│
├──────────────────────┼──────────┼────────────┼─────────────────────────────────────────────────┤
│ Argon2PasswordEncoder│ {argon2} │ RECOMMENDED│ Winner of Password Hashing Competition (2015).  │
│                      │          │            │ Memory-hard: resists GPU attacks. Configurable   │
│                      │          │            │ memory, iterations, parallelism. Best security.  │
├──────────────────────┼──────────┼────────────┼─────────────────────────────────────────────────┤
│ SCryptPasswordEncoder│ {scrypt} │ GOOD       │ CPU + memory hard. Similar to Argon2 but older. │
│                      │          │            │ Configurable cost parameters. Good alternative.  │
├──────────────────────┼──────────┼────────────┼─────────────────────────────────────────────────┤
│ Pbkdf2PasswordEncoder│ {pbkdf2} │ ACCEPTABLE │ NIST recommended. Uses HMAC-SHA256/SHA512.      │
│                      │          │            │ Configurable iterations (600K+ recommended).     │
│                      │          │            │ Not memory-hard (vulnerable to GPU attacks).     │
├──────────────────────┼──────────┼────────────┼─────────────────────────────────────────────────┤
│ NoOpPasswordEncoder  │ {noop}   │ DEPRECATED │ NO hashing. Plain text comparison.              │
│                      │          │ DEV ONLY   │ NEVER use in production. Only for development.  │
├──────────────────────┼──────────┼────────────┼─────────────────────────────────────────────────┤
│ StandardPassword     │ {sha256} │ DEPRECATED │ SHA-256 with random salt. Too fast for modern   │
│ Encoder              │          │            │ hardware — vulnerable to GPU brute force.        │
├──────────────────────┼──────────┼────────────┼─────────────────────────────────────────────────┤
│ LdapShaPassword      │ {ldap}   │ DEPRECATED │ LDAP SSHA encoding. Only for LDAP integration.  │
│ Encoder              │          │            │                                                  │
├──────────────────────┼──────────┼────────────┼─────────────────────────────────────────────────┤
│ MessageDigest        │ {MD5}    │ DEPRECATED │ MD5 hash. Cryptographically broken.             │
│ PasswordEncoder      │ {SHA-1}  │ INSECURE   │ SHA-1 hash. Collision attacks known.            │
│                      │ {SHA-256}│            │ Only for legacy migration.                      │
├──────────────────────┼──────────┼────────────┼─────────────────────────────────────────────────┤
│ DelegatingPassword   │ (meta)   │ DEFAULT    │ Not an encoder itself — DELEGATES to the right  │
│ Encoder              │          │ ENCODER    │ encoder based on {id} prefix. Used as the       │
│                      │          │            │ default when no PasswordEncoder bean is defined. │
└──────────────────────┴──────────┴────────────┴─────────────────────────────────────────────────┘
```

**Security Comparison:**

```
┌────────────────────────────────────────────────────────────────────────┐
│  ENCODER SECURITY COMPARISON                                          │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  STRENGTH →  Weakest                                      Strongest   │
│                                                                        │
│  NoOp ──── MD5 ──── SHA-256 ──── PBKDF2 ──── BCrypt ──── Argon2     │
│  (plain)   (broken) (too fast)  (CPU-hard)  (CPU-hard)  (memory +    │
│                                                          CPU-hard)    │
│                                                                        │
│  Recommendation:                                                       │
│  • New projects: BCrypt (strength 12) or Argon2                       │
│  • Legacy migration: DelegatingPasswordEncoder (read old, write new)  │
│  • Development only: {noop}                                           │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

**Code Examples for Each Encoder:**

```java
// BCryptPasswordEncoder — RECOMMENDED (default strength = 10)
PasswordEncoder bcrypt = new BCryptPasswordEncoder();
String hash = bcrypt.encode("secret123");
// → "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"
bcrypt.matches("secret123", hash);  // → true

// BCrypt with custom strength (4-31, higher = slower)
PasswordEncoder bcryptStrong = new BCryptPasswordEncoder(12);  // Production: use 12+
String hash12 = bcryptStrong.encode("secret123");
// → "$2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb7YCp3bVnGJ0i3IuZpZpx9uBfGHJKS"

// Argon2PasswordEncoder — MOST SECURE
PasswordEncoder argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
// Parameters: saltLength=16, hashLength=32, parallelism=1, memory=16384, iterations=2
String argonHash = argon2.encode("secret123");
// → "$argon2id$v=19$m=16384,t=2,p=1$randomsalt$hashvalue"

// SCryptPasswordEncoder
PasswordEncoder scrypt = SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
String scryptHash = scrypt.encode("secret123");

// Pbkdf2PasswordEncoder
PasswordEncoder pbkdf2 = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
// Uses HMAC-SHA256 with 310,000 iterations by default
String pbkdf2Hash = pbkdf2.encode("secret123");

// NoOpPasswordEncoder — NEVER in production
@SuppressWarnings("deprecation")
PasswordEncoder noop = NoOpPasswordEncoder.getInstance();
String noopHash = noop.encode("secret123");
// → "secret123" (no hashing at all!)
```

---

#### 6.5 Using BCrypt with InMemoryUserDetailsManager

There are **two approaches** to use BCrypt with `InMemoryUserDetailsManager`:

**Approach 1: Using {bcrypt} prefix (with DelegatingPasswordEncoder — default)**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        // Pre-hash the passwords and store with {bcrypt} prefix
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        UserDetails user = User.builder()
                .username("john")
                .password("{bcrypt}" + encoder.encode("john123"))
                // Stored: "{bcrypt}$2a$12$LJ3m4YXhQK0..."
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password("{bcrypt}" + encoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    // No PasswordEncoder bean needed!
    // DelegatingPasswordEncoder (default) reads {bcrypt} prefix
    // and uses BCryptPasswordEncoder internally
}
```

**Approach 2: Using User.withDefaultPasswordEncoder() — convenience for dev/testing**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @SuppressWarnings("deprecation")  // deprecated because it's not for production
    public UserDetailsService userDetailsService() {
        // withDefaultPasswordEncoder() auto-encodes with bcrypt and adds {bcrypt} prefix
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("john")
                .password("john123")       // You provide plain text
                .roles("USER")
                .build();
        // Internally creates: "{bcrypt}$2a$10$..." — auto-hashed!

        UserDetails admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin123")
                .roles("ADMIN", "USER")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }
}
```

---

#### 6.6 Skipping the {id} Prefix — Using a Dedicated PasswordEncoder Bean

If you don't want passwords stored with the `{id}encodedpassword` format (e.g., your database already has BCrypt hashes without the `{bcrypt}` prefix), you can define a **specific PasswordEncoder bean**. This replaces the default `DelegatingPasswordEncoder`.

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  WITH vs WITHOUT {id} PREFIX                                                         │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  WITH DelegatingPasswordEncoder (default — uses {id} prefix):                       │
│  ┌────────────────────────────────────────────────────────────┐                     │
│  │  Stored:   {bcrypt}$2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb...    │                     │
│  │  Encoder:  DelegatingPasswordEncoder (reads {id} prefix)   │                     │
│  │  Benefit:  Supports multiple algorithms, migration          │                     │
│  │  Drawback: Passwords have {id} prefix in DB                 │                     │
│  └────────────────────────────────────────────────────────────┘                     │
│                                                                                      │
│  WITH Dedicated BCryptPasswordEncoder bean (no {id} prefix):                        │
│  ┌────────────────────────────────────────────────────────────┐                     │
│  │  Stored:   $2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb...             │                     │
│  │  Encoder:  BCryptPasswordEncoder (directly)                 │                     │
│  │  Benefit:  Cleaner DB values, no prefix needed              │                     │
│  │  Drawback: Locked to one algorithm, harder to migrate       │                     │
│  └────────────────────────────────────────────────────────────┘                     │
│                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example — Dedicated PasswordEncoder Bean with InMemoryUserDetailsManager:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Step 1: Define PasswordEncoder bean — this REPLACES DelegatingPasswordEncoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
        // Now ALL password matching uses BCrypt directly
        // No {id} prefix needed in stored passwords
    }

    // Step 2: Use the encoder to hash passwords (no {id} prefix!)
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {

        UserDetails user = User.builder()
                .username("john")
                .password(encoder.encode("john123"))
                // Stored: "$2a$12$LJ3m4YXh..." (NO {bcrypt} prefix!)
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))
                // Stored: "$2a$12$Kp9W5xYz..." (NO {bcrypt} prefix!)
                .roles("ADMIN", "USER")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```

**How it works when PasswordEncoder bean is defined:**

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  PASSWORD MATCHING — WITH DEDICATED PasswordEncoder BEAN                        │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  User submits: password = "john123"                                             │
│  Stored in InMemoryUserDetailsManager: "$2a$12$LJ3m4YXh..." (no {id} prefix)   │
│       │                                                                          │
│       ▼                                                                          │
│  DaoAuthenticationProvider:                                                      │
│  passwordEncoder.matches("john123", "$2a$12$LJ3m4YXh...")                       │
│       │                                                                          │
│       ▼                                                                          │
│  BCryptPasswordEncoder.matches() ← directly called (not DelegatingPE)          │
│  ├── Extracts salt from "$2a$12$LJ3m4YXh..."                                   │
│  ├── Hashes "john123" with same salt                                             │
│  ├── Compares hashes → MATCH ✓                                                  │
│  └── Returns true                                                                │
│                                                                                  │
│  KEY DIFFERENCE:                                                                │
│  • No {id} prefix parsing                                                       │
│  • No DelegatingPasswordEncoder involved                                        │
│  • BCryptPasswordEncoder called directly                                         │
│  • Simpler, but locked to ONE algorithm                                          │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Using other encoders as dedicated bean:**

```java
// Argon2 as the dedicated encoder
@Bean
public PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
}

// SCrypt as the dedicated encoder
@Bean
public PasswordEncoder passwordEncoder() {
    return SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
}

// PBKDF2 as the dedicated encoder
@Bean
public PasswordEncoder passwordEncoder() {
    return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
}

// DelegatingPasswordEncoder (explicit — same as default but you control it)
@Bean
public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

---

#### 6.7 When to Use Which Approach — Decision Guide

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│  DECISION GUIDE: {id} PREFIX vs DEDICATED ENCODER                                       │
├──────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  USE {id}encodedpassword (DelegatingPasswordEncoder):                                   │
│  ├── ✓ Migrating from an old system with mixed password formats                        │
│  ├── ✓ Want to gradually upgrade from SHA-256/MD5 to BCrypt/Argon2                     │
│  ├── ✓ Multiple services share the same user database with different encodings          │
│  ├── ✓ Want the flexibility to change algorithms in the future                          │
│  └── ✓ Spring Security default — no extra configuration needed                          │
│                                                                                          │
│  USE Dedicated PasswordEncoder Bean (no {id} prefix):                                   │
│  ├── ✓ New project — no legacy passwords to support                                    │
│  ├── ✓ Want cleaner password values in DB (no {bcrypt} prefix)                         │
│  ├── ✓ Single encoding standard across the entire system                                │
│  ├── ✓ External systems (LDAP, Active Directory) provide pre-hashed passwords          │
│  └── ✓ Simpler mental model for the team                                                │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 6.8 Why InMemoryUserDetailsManager Is NOT Used in Production

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  ⚠️  InMemoryUserDetailsManager — LIMITATIONS                                        │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  Problem                        │ Impact in Production                               │
│  ───────────────────────────────┼────────────────────────────────────────────────────│
│  1. HashMap storage only        │ ALL users lost on every application restart.       │
│                                 │ No persistence whatsoever.                         │
│  ───────────────────────────────┼────────────────────────────────────────────────────│
│  2. Cannot scale horizontally   │ Each instance has its own HashMap. User created    │
│                                 │ on Instance A doesn't exist on Instance B.         │
│                                 │ Load balancers route to different instances.        │
│  ───────────────────────────────┼────────────────────────────────────────────────────│
│  3. No dynamic user management  │ Cannot register new users at runtime without       │
│                                 │ coding it in Java. No admin UI for user CRUD.      │
│  ───────────────────────────────┼────────────────────────────────────────────────────│
│  4. Not thread-safe for writes  │ HashMap is not synchronized. Concurrent writes     │
│                                 │ (user creation) can cause data corruption.         │
│  ───────────────────────────────┼────────────────────────────────────────────────────│
│  5. Memory consumption          │ All users loaded in JVM heap. With millions of     │
│                                 │ users, this causes OutOfMemoryError.               │
│  ───────────────────────────────┼────────────────────────────────────────────────────│
│  6. No audit trail              │ No logging of password changes, account locks,     │
│                                 │ login history, or security events.                 │
│  ───────────────────────────────┼────────────────────────────────────────────────────│
│  7. Hardcoded users             │ Users defined in Java config or properties.        │
│                                 │ Adding a user requires code change + redeploy.     │
│                                                                                      │
│  USE INSTEAD:                                                                        │
│  ┌───────────────────────────────────────────────────────────────────────────────┐  │
│  │ Development / Testing → InMemoryUserDetailsManager ✓                         │  │
│  │ Small apps / Prototypes → JdbcUserDetailsManager (built-in DB schema)        │  │
│  │ Production apps → Custom UserDetailsService + JPA Repository + Database      │  │
│  │ Enterprise → LDAP / OAuth2 / Keycloak / Okta                                │  │
│  └───────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

**Production Pattern — Custom UserDetailsService replacing InMemoryUserDetailsManager:**

```java
// PRODUCTION: Custom UserDetailsService backed by database
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;  // JPA Repository

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}

// PRODUCTION: SecurityConfig with dedicated BCrypt encoder
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final DatabaseUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // No {id} prefix needed in DB
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}

// DB stores passwords like: $2a$12$LJ3m4YXhQK0R1xLq3TP.hOFb... (no {bcrypt} prefix)
// Users persisted across restarts ✓
// Horizontally scalable ✓
// Dynamic user registration ✓
// Thread-safe (database handles concurrency) ✓
``` 

---


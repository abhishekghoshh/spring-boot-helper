### 5. Default User Creation in Spring Security — Auto-Configuration Deep Dive

---

#### 5.1 What Happens When You Just Add the Dependency?

Before we can authenticate or authorize any user, the user must first **exist**. Spring Security needs a `UserDetailsService` to look up users during authentication. But what if you configure **nothing** — just add the dependency?

```xml
<!-- Just this in pom.xml — no SecurityConfig, no UserDetailsService bean -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

When you start the application, you see this in the console:

```
Using generated security password: 8e557c5e-d7f4-4a5f-b4c3-2f9d5e8a1b3c

This generated password is for development use only.
Your security configuration must be updated before running your application in production.
```

**What just happened?** Spring Boot auto-configured everything for you:
- Created a user with username `user`
- Generated a random UUID password (changes every restart)
- Stored it in an `InMemoryUserDetailsManager`
- Secured ALL endpoints with HTTP Basic + Form Login
- Generated a default login page at `/login`

---

#### 5.2 Auto-Configuration Flow — Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│        SPRING BOOT SECURITY AUTO-CONFIGURATION FLOW (Zero Config)                       │
├──────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  Application Startup                                                                     │
│       │                                                                                  │
│       ▼                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │  Step 1: SecurityProperties.java loads properties                               │    │
│  │  ────────────────────────────────────────────                                   │    │
│  │  @ConfigurationProperties(prefix = "spring.security")                           │    │
│  │                                                                                 │    │
│  │  Reads from application.properties / application.yml:                           │    │
│  │  ├── spring.security.user.name     → default: "user"                           │    │
│  │  ├── spring.security.user.password → default: null (triggers UUID generation)  │    │
│  │  └── spring.security.user.roles    → default: empty []                          │    │
│  │                                                                                 │    │
│  │  Creates SecurityProperties.User object with these values                       │    │
│  └──────────────────────────────┬──────────────────────────────────────────────────┘    │
│                                 │                                                        │
│                                 ▼                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │  Step 2: UserDetailsServiceAutoConfiguration.java checks conditions             │    │
│  │  ──────────────────────────────────────────────────────────────                 │    │
│  │                                                                                 │    │
│  │  @ConditionalOnMissingBean({                                                    │    │
│  │      AuthenticationManager.class,                                               │    │
│  │      AuthenticationProvider.class,                                               │    │
│  │      UserDetailsService.class,                                                  │    │
│  │      AuthenticationManagerResolver.class                                         │    │
│  │  })                                                                             │    │
│  │                                                                                 │    │
│  │  Translation: "Only create this if the developer has NOT defined ANY of these"  │    │
│  │                                                                                 │    │
│  │  Developer defined nothing? → YES, proceed with auto-config                    │    │
│  │  Developer defined a custom UserDetailsService? → SKIP auto-config             │    │
│  └──────────────────────────────┬──────────────────────────────────────────────────┘    │
│                                 │                                                        │
│                                 ▼                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │  Step 3: Check if password was provided                                         │    │
│  │  ──────────────────────────────────────                                         │    │
│  │                                                                                 │    │
│  │  if (user.getPassword() == null) {                                              │    │
│  │      // No password in properties → Generate random UUID                        │    │
│  │      String password = UUID.randomUUID().toString();                             │    │
│  │      user.setPassword(password);                                                │    │
│  │                                                                                 │    │
│  │      // Log it to console so developer can use it                               │    │
│  │      logger.warn("Using generated security password: " + password);             │    │
│  │      logger.warn("This generated password is for development use only...");     │    │
│  │  }                                                                              │    │
│  │                                                                                 │    │
│  │  ⚠️ Password is regenerated on EVERY server restart!                            │    │
│  └──────────────────────────────┬──────────────────────────────────────────────────┘    │
│                                 │                                                        │
│                                 ▼                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │  Step 4: Create InMemoryUserDetailsManager bean                                 │    │
│  │  ──────────────────────────────────────────────                                 │    │
│  │                                                                                 │    │
│  │  UserDetails userDetails = User.builder()                                       │    │
│  │      .username("user")                                                          │    │
│  │      .password("{noop}8e557c5e-d7f4-4a5f-b4c3-2f9d5e8a1b3c")                  │    │
│  │      .roles()          // empty roles                                           │    │
│  │      .build();                                                                  │    │
│  │                                                                                 │    │
│  │  return new InMemoryUserDetailsManager(userDetails);                            │    │
│  │  // Stores user in a HashMap<String, MutableUserDetails>                        │    │
│  │  // This bean is registered as the UserDetailsService                           │    │
│  └──────────────────────────────┬──────────────────────────────────────────────────┘    │
│                                 │                                                        │
│                                 ▼                                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │  Step 5: SpringBootWebSecurityConfiguration auto-configures SecurityFilterChain │    │
│  │  ─────────────────────────────────────────────────────────────────────────────  │    │
│  │                                                                                 │    │
│  │  @Bean SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {       │    │
│  │      http                                                                       │    │
│  │          .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())       │    │
│  │          .formLogin(withDefaults())    // login page at /login                  │    │
│  │          .httpBasic(withDefaults());   // HTTP Basic auth header support        │    │
│  │      return http.build();                                                       │    │
│  │  }                                                                              │    │
│  │                                                                                 │    │
│  │  Result: ALL endpoints require authentication, with form + basic login          │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                          │
│  NOW the app is running:                                                                 │
│  • GET /anything → redirects to /login (form login page)                                │
│  • Login with username: user, password: <from console>                                   │
│  • Or use HTTP Basic: curl -u user:<password> http://localhost:8080/anything             │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5.3 Internal Source Code — The Three Key Classes

##### 5.3.1 `SecurityProperties.java`

This class binds `spring.security.user.*` properties from `application.properties` / `application.yml` into a Java object.

```java
// org.springframework.boot.autoconfigure.security.SecurityProperties (simplified)

@ConfigurationProperties(prefix = "spring.security")
public class SecurityProperties {

    private final User user = new User();

    public User getUser() {
        return this.user;
    }

    public static class User {

        private String name = "user";          // Default username

        private String password;                // Default: null → triggers UUID generation

        private List<String> roles = new ArrayList<>();  // Default: empty list

        private boolean passwordGenerated = true;  // Tracks if password was auto-generated

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(String password) {
            if (!StringUtils.hasLength(password)) {
                return;
            }
            this.passwordGenerated = false;  // Developer provided a password
            this.password = password;
        }

        public List<String> getRoles() {
            return this.roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = new ArrayList<>(roles);
        }

        public boolean isPasswordGenerated() {
            return this.passwordGenerated;
        }
    }
}
```

**Key points:**
- Default username is `"user"` (hardcoded)
- Default password is `null` — which signals auto-generation
- `passwordGenerated` flag tracks whether the password was set by the developer or auto-generated
- When `setPassword()` is called with a non-empty value (from properties), it sets `passwordGenerated = false`

---

##### 5.3.2 `UserDetailsServiceAutoConfiguration.java`

This is the **heart** of the auto-configuration. It decides whether to create a default `InMemoryUserDetailsManager`.

```java
// org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration (simplified)

@AutoConfiguration
@ConditionalOnClass(AuthenticationManager.class)
@ConditionalOnBean(ObjectPostProcessor.class)
@ConditionalOnMissingBean(
    value = {
        AuthenticationManager.class,
        AuthenticationProvider.class,
        UserDetailsService.class,
        AuthenticationManagerResolver.class
    },
    type = "org.springframework.security.oauth2.jwt.JwtDecoder"
)
public class UserDetailsServiceAutoConfiguration {

    private static final String NOOP_PASSWORD_PREFIX = "{noop}";
    private static final Pattern PASSWORD_ALGORITHM_PATTERN = Pattern.compile("^\\{.+}.*$");
    private static final Log logger = LogFactory.getLog(UserDetailsServiceAutoConfiguration.class);

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(
            SecurityProperties properties,
            ObjectProvider<PasswordEncoder> passwordEncoder) {

        SecurityProperties.User user = properties.getUser();

        // List of roles from properties (default: empty)
        List<String> roles = user.getRoles();
        String[] roleArray = roles.toArray(new String[0]);

        // Build the UserDetails object
        return new InMemoryUserDetailsManager(
            User.withUsername(user.getName())
                .password(getOrDeducePassword(user, passwordEncoder.getIfAvailable()))
                .roles(roleArray)
                .build()
        );
    }

    private String getOrDeducePassword(SecurityProperties.User user,
                                        PasswordEncoder encoder) {
        String password = user.getPassword();

        if (user.isPasswordGenerated()) {
            // Password was NOT set in properties → generate random UUID
            logger.warn(String.format(
                "%n%nUsing generated security password: %s%n%n" +
                "This generated password is for development use only. " +
                "Your security configuration must be updated before " +
                "running your application in production.%n",
                user.getPassword()));
        }

        // If no PasswordEncoder bean exists AND password doesn't already
        // have an encoding prefix like {bcrypt}... → prepend {noop}
        if (encoder != null || PASSWORD_ALGORITHM_PATTERN.matcher(password).matches()) {
            return password;
        }
        return NOOP_PASSWORD_PREFIX + password;
        // Result: "{noop}8e557c5e-d7f4-4a5f-b4c3-2f9d5e8a1b3c"
        // {noop} tells DelegatingPasswordEncoder to use NoOpPasswordEncoder (plain text match)
    }
}
```

**The `@ConditionalOnMissingBean` is the critical part:**

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│  WHEN DOES AUTO-CONFIGURATION ACTIVATE?                                          │
├───────────────────────────────────────────────────────────────────────────────────┤
│                                                                                   │
│  Auto-config creates InMemoryUserDetailsManager ONLY when ALL of these           │
│  are missing from the Spring context:                                             │
│                                                                                   │
│  ┌──────────────────────────────────┬─────────────────────────────────────────┐  │
│  │ Bean NOT defined                  │ Means developer hasn't...               │  │
│  ├──────────────────────────────────┼─────────────────────────────────────────┤  │
│  │ AuthenticationManager             │ created a custom AuthenticationManager  │  │
│  │ AuthenticationProvider            │ created a custom AuthenticationProvider │  │
│  │ UserDetailsService                │ created a custom UserDetailsService     │  │
│  │ AuthenticationManagerResolver     │ created a resolver for multi-tenant     │  │
│  │ JwtDecoder                        │ configured OAuth2 Resource Server       │  │
│  └──────────────────────────────────┴─────────────────────────────────────────┘  │
│                                                                                   │
│  If you define ANY ONE of these beans, auto-config BACKS OFF completely!         │
│                                                                                   │
│  Examples that disable auto-config:                                               │
│  ✓ @Bean UserDetailsService myService() { ... }                                 │
│  ✓ @Bean AuthenticationProvider myProvider() { ... }                             │
│  ✓ Adding spring-boot-starter-oauth2-resource-server (provides JwtDecoder)       │
│                                                                                   │
└───────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 5.3.3 `InMemoryUserDetailsManager.java`

This is the actual `UserDetailsService` implementation that stores users in a `HashMap` in memory.

```java
// org.springframework.security.provisioning.InMemoryUserDetailsManager (simplified)

public class InMemoryUserDetailsManager implements UserDetailsManager, UserDetailsPasswordService {

    // In-memory storage — simple HashMap
    private final Map<String, MutableUserDetails> users = new HashMap<>();

    // Constructor — accepts one or more UserDetails
    public InMemoryUserDetailsManager(UserDetails... users) {
        for (UserDetails user : users) {
            createUser(user);
        }
    }

    // Constructor — accepts a Collection
    public InMemoryUserDetailsManager(Collection<UserDetails> users) {
        for (UserDetails user : users) {
            createUser(user);
        }
    }

    @Override
    public void createUser(UserDetails user) {
        // Stores in HashMap with lowercase username as key
        this.users.put(user.getUsername().toLowerCase(), new MutableUser(user));
    }

    @Override
    public void deleteUser(String username) {
        this.users.remove(username.toLowerCase());
    }

    @Override
    public void updateUser(UserDetails user) {
        this.users.put(user.getUsername().toLowerCase(), new MutableUser(user));
    }

    @Override
    public boolean userExists(String username) {
        return this.users.containsKey(username.toLowerCase());
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        String username = currentUser.getName();
        MutableUserDetails user = this.users.get(username.toLowerCase());
        // Verify old password, set new password
        user.setPassword(newPassword);
    }

    // THIS is called by DaoAuthenticationProvider during authentication
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = this.users.get(username.toLowerCase());
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        // Return a copy to prevent external modification
        return new User(user.getUsername(), user.getPassword(),
                user.isEnabled(), user.isAccountNonExpired(),
                user.isCredentialsNonExpired(), user.isAccountNonLocked(),
                user.getAuthorities());
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        String username = user.getUsername();
        MutableUserDetails mutableUser = this.users.get(username.toLowerCase());
        mutableUser.setPassword(newPassword);
        return mutableUser;
    }
}
```

**Key characteristics:**
- Uses a simple `HashMap` — not thread-safe for writes (fine for dev, not production)
- All data lost on application restart
- Case-insensitive username lookup (`.toLowerCase()`)
- Implements both `UserDetailsService` (read) and `UserDetailsManager` (CRUD)

---

#### 5.4 How Password Generation Works — Sequence Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│         PASSWORD GENERATION SEQUENCE (When No Config Provided)                           │
├──────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  Spring Boot Startup                                                                     │
│       │                                                                                  │
│       ▼                                                                                  │
│  SecurityProperties loads:                                                               │
│  ┌────────────────────────────────────────────────────┐                                 │
│  │  name = "user"           (default)                  │                                 │
│  │  password = null          (no properties set)       │                                 │
│  │  passwordGenerated = true (flag stays true)         │                                 │
│  │  roles = []               (empty)                   │                                 │
│  └────────────────────────┬───────────────────────────┘                                 │
│                           │                                                              │
│                           ▼                                                              │
│  SecurityProperties @PostConstruct / lazy init:                                          │
│  ┌────────────────────────────────────────────────────┐                                 │
│  │  if (password == null) {                            │                                 │
│  │      password = UUID.randomUUID().toString();       │                                 │
│  │      // e.g., "8e557c5e-d7f4-4a5f-b4c3-2f9d5e8a"  │                                 │
│  │      // passwordGenerated remains TRUE              │                                 │
│  │  }                                                  │                                 │
│  └────────────────────────┬───────────────────────────┘                                 │
│                           │                                                              │
│                           ▼                                                              │
│  UserDetailsServiceAutoConfiguration:                                                    │
│  ┌────────────────────────────────────────────────────┐                                 │
│  │  1. Check: any UserDetailsService bean exists?      │                                 │
│  │     → NO → proceed                                  │                                 │
│  │                                                     │                                 │
│  │  2. Get SecurityProperties.User                     │                                 │
│  │     username = "user"                               │                                 │
│  │     password = "8e557c5e-d7f4-..."                  │                                 │
│  │     passwordGenerated = true                        │                                 │
│  │                                                     │                                 │
│  │  3. passwordGenerated == true?                      │                                 │
│  │     → YES → Log warning to console:                 │                                 │
│  │     "Using generated security password: 8e557..."   │                                 │
│  │                                                     │                                 │
│  │  4. No PasswordEncoder bean? No {bcrypt} prefix?    │                                 │
│  │     → Prepend {noop}                                │                                 │
│  │     password = "{noop}8e557c5e-d7f4-..."            │                                 │
│  │                                                     │                                 │
│  │  5. Create UserDetails:                             │                                 │
│  │     User.withUsername("user")                       │                                 │
│  │         .password("{noop}8e557c5e-d7f4-...")        │                                 │
│  │         .roles()  // empty                          │                                 │
│  │         .build()                                    │                                 │
│  │                                                     │                                 │
│  │  6. return new InMemoryUserDetailsManager(user)     │                                 │
│  │     → stores in HashMap: {"user" → UserDetails}     │                                 │
│  └────────────────────────────────────────────────────┘                                 │
│                                                                                          │
│  Console Output:                                                                         │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                                  │   │
│  │  Using generated security password: 8e557c5e-d7f4-4a5f-b4c3-2f9d5e8a1b3c       │   │
│  │                                                                                  │   │
│  │  This generated password is for development use only. Your security              │   │
│  │  configuration must be updated before running your application in production.    │   │
│  │                                                                                  │   │
│  └──────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  ⚠️ Restart the server → New UUID generated → Different password each time!             │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5.5 Authentication Flow with Default Config

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│  AUTHENTICATION FLOW WITH DEFAULT AUTO-CONFIGURED USER                                   │
├──────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  1. User opens browser → http://localhost:8080/hello                                    │
│       │                                                                                  │
│       ▼                                                                                  │
│  2. AuthorizationFilter → anyRequest().authenticated() → NOT authenticated              │
│       │                                                                                  │
│       ▼                                                                                  │
│  3. ExceptionTranslationFilter catches AccessDeniedException                            │
│     → Redirects to /login (default form login page)                                     │
│       │                                                                                  │
│       ▼                                                                                  │
│  4. DefaultLoginPageGeneratingFilter serves the auto-generated login form               │
│     ┌────────────────────────────────────┐                                              │
│     │  ┌──────────────────────────────┐  │                                              │
│     │  │       Spring Security         │  │                                              │
│     │  │                               │  │                                              │
│     │  │  Username: [user          ]   │  │                                              │
│     │  │  Password: [<from console>]   │  │                                              │
│     │  │                               │  │                                              │
│     │  │       [Sign In]               │  │                                              │
│     │  └──────────────────────────────┘  │                                              │
│     └────────────────────────────────────┘                                              │
│       │                                                                                  │
│       ▼                                                                                  │
│  5. User submits: POST /login {username=user, password=8e557c5e-d7f4-...}               │
│       │                                                                                  │
│       ▼                                                                                  │
│  6. UsernamePasswordAuthenticationFilter                                                 │
│     → Creates UsernamePasswordAuthenticationToken("user", "8e557c5e-...")               │
│     → Passes to AuthenticationManager                                                    │
│       │                                                                                  │
│       ▼                                                                                  │
│  7. ProviderManager → DaoAuthenticationProvider                                          │
│     → UserDetailsService.loadUserByUsername("user")                                      │
│     → InMemoryUserDetailsManager.loadUserByUsername("user")                              │
│     → HashMap lookup → finds UserDetails                                                 │
│       │                                                                                  │
│       ▼                                                                                  │
│  8. DelegatingPasswordEncoder.matches()                                                  │
│     → Sees {noop} prefix → delegates to NoOpPasswordEncoder                             │
│     → Plain text comparison: "8e557c5e-..." == "8e557c5e-..." → TRUE ✓                 │
│       │                                                                                  │
│       ▼                                                                                  │
│  9. Authentication SUCCESS                                                               │
│     → SecurityContextHolder stores Authentication                                        │
│     → Session created with JSESSIONID cookie                                             │
│     → Redirect to original URL /hello                                                    │
│       │                                                                                  │
│       ▼                                                                                  │
│  10. GET /hello → AuthorizationFilter → authenticated ✓ → Controller executes           │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5.6 Overriding Default User in `application.properties`

You can customize the default user without writing any Java code:

**application.properties:**

```properties
# Override default username (default: "user")
spring.security.user.name=admin

# Override default password (prevents random UUID generation)
spring.security.user.password=mySecretPassword123

# Assign roles (default: empty)
spring.security.user.roles=ADMIN,USER
```

**application.yml equivalent:**

```yaml
spring:
  security:
    user:
      name: admin
      password: mySecretPassword123
      roles:
        - ADMIN
        - USER
```

**What changes internally when you set these properties:**

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  WITH CUSTOM PROPERTIES                                                          │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  SecurityProperties loads:                                                       │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  name = "admin"               (from properties)      │                       │
│  │  password = "mySecretPassword123" (from properties)  │                       │
│  │  passwordGenerated = FALSE     (setPassword called)  │                       │
│  │  roles = ["ADMIN", "USER"]     (from properties)     │                       │
│  └──────────────────────────────────────────────────────┘                       │
│                                                                                  │
│  UserDetailsServiceAutoConfiguration:                                            │
│  ┌──────────────────────────────────────────────────────┐                       │
│  │  1. passwordGenerated == false?                       │                       │
│  │     → YES → Do NOT log password to console            │                       │
│  │     → No "Using generated security password" message  │                       │
│  │                                                       │                       │
│  │  2. Create UserDetails:                               │                       │
│  │     User.withUsername("admin")                        │                       │
│  │         .password("{noop}mySecretPassword123")        │                       │
│  │         .roles("ADMIN", "USER")                       │                       │
│  │         .build()                                      │                       │
│  │                                                       │                       │
│  │  3. InMemoryUserDetailsManager(userDetails)           │                       │
│  │     HashMap: {"admin" → UserDetails with              │                       │
│  │               authorities=[ROLE_ADMIN, ROLE_USER]}    │                       │
│  └──────────────────────────────────────────────────────┘                       │
│                                                                                  │
│  Now login with:                                                                 │
│  Username: admin                                                                 │
│  Password: mySecretPassword123                                                   │
│                                                                                  │
│  ⚠️ Password is NOT regenerated on restart (it's fixed in properties)           │
│  ⚠️ Still NOT suitable for production (plain text password in config file)       │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5.7 How the Auto-Config Gets Disabled

The moment you define your own `UserDetailsService`, `AuthenticationProvider`, or any related bean, the auto-configuration **backs off completely**:

```java
// Any ONE of these disables the auto-configured InMemoryUserDetailsManager:

// Option 1: Custom UserDetailsService bean
@Bean
public UserDetailsService userDetailsService() {
    // Your own implementation — auto-config backs off
    return new CustomUserDetailsService(userRepository);
}

// Option 2: Custom InMemoryUserDetailsManager with your own users
@Bean
public InMemoryUserDetailsManager inMemoryUsers(PasswordEncoder encoder) {
    UserDetails user = User.builder()
        .username("john")
        .password(encoder.encode("john123"))
        .roles("USER")
        .build();
    UserDetails admin = User.builder()
        .username("admin")
        .password(encoder.encode("admin123"))
        .roles("ADMIN", "USER")
        .build();
    return new InMemoryUserDetailsManager(user, admin);
}

// Option 3: Custom AuthenticationProvider
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(customUserDetailsService);
    provider.setPasswordEncoder(new BCryptPasswordEncoder());
    return provider;
}

// Option 4: Adding OAuth2 Resource Server dependency (provides JwtDecoder)
// Just adding this to pom.xml disables the default user:
// spring-boot-starter-oauth2-resource-server
```

```
┌────────────────────────────────────────────────────────────────────┐
│  AUTO-CONFIG DECISION TREE                                        │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  spring-boot-starter-security on classpath?                       │
│  ├── NO  → No security at all                                    │
│  └── YES → Check for developer-defined beans:                    │
│            │                                                       │
│            ├── UserDetailsService bean exists?                    │
│            │   └── YES → Use developer's bean, SKIP auto-config  │
│            │                                                       │
│            ├── AuthenticationProvider bean exists?                │
│            │   └── YES → Use developer's bean, SKIP auto-config  │
│            │                                                       │
│            ├── AuthenticationManager bean exists?                 │
│            │   └── YES → Use developer's bean, SKIP auto-config  │
│            │                                                       │
│            ├── JwtDecoder bean exists?                            │
│            │   └── YES → Use OAuth2 Resource Server, SKIP        │
│            │                                                       │
│            └── NONE of above exist?                               │
│                └── AUTO-CONFIGURE:                                │
│                    ├── Create InMemoryUserDetailsManager          │
│                    ├── user = "user" or from properties           │
│                    ├── password = UUID or from properties         │
│                    └── Log password to console (if generated)     │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

#### 5.8 Why This Is Only for Development — NOT Production

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  ⚠️  WHY DEFAULT USER IS NOT FOR PRODUCTION                                         │
├──────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  Problem                              │ Why it's bad                                │
│  ─────────────────────────────────────┼─────────────────────────────────────────────│
│  1. Password in console logs          │ Logs may be captured by monitoring tools,   │
│                                       │ CI/CD pipelines, or log aggregators.        │
│                                       │ Anyone with log access has credentials.     │
│  ─────────────────────────────────────┼─────────────────────────────────────────────│
│  2. Password changes on restart       │ Impossible to use in clustered/scaled       │
│                                       │ environments. Different instances have      │
│                                       │ different passwords.                        │
│  ─────────────────────────────────────┼─────────────────────────────────────────────│
│  3. In-memory storage                 │ No persistence. All users lost on restart.  │
│                                       │ Cannot scale horizontally.                  │
│  ─────────────────────────────────────┼─────────────────────────────────────────────│
│  4. Single user only                  │ Real apps need multiple users with          │
│                                       │ different roles, permissions, and profiles. │
│  ─────────────────────────────────────┼─────────────────────────────────────────────│
│  5. No password hashing ({noop})      │ Password stored in plain text in memory.    │
│                                       │ No BCrypt/Argon2 protection.                │
│  ─────────────────────────────────────┼─────────────────────────────────────────────│
│  6. Password in properties file       │ Even with spring.security.user.password,    │
│                                       │ the password is plain text in config file   │
│                                       │ which may be committed to version control.  │
│                                                                                      │
│  PRODUCTION ALTERNATIVES:                                                            │
│  ✓ Custom UserDetailsService backed by database (JPA/JDBC)                          │
│  ✓ OAuth2 / OpenID Connect (Keycloak, Okta, Auth0)                                  │
│  ✓ LDAP / Active Directory                                                           │
│  ✓ JWT-based stateless authentication                                                │
│  ✓ Passwords hashed with BCrypt (strength 12+) or Argon2                            │
│                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

**Production-ready example (replacing the default):**

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ProductionSecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider());
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);  // DB-backed
        provider.setPasswordEncoder(passwordEncoder());       // BCrypt hashing
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // Production-grade hashing
    }
    // ↑ Defining this bean means UserDetailsServiceAutoConfiguration BACKS OFF
    //   No more "Using generated security password" in console
    //   No more InMemoryUserDetailsManager
}
```

---


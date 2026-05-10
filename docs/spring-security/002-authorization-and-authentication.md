### 2. Authentication and Authorization

---

#### 2.1 What is Authentication?

**Authentication** is the process of verifying **"Who are you?"** — confirming the identity of a user, service, or system. It ensures that the entity is who they claim to be.

#### 2.2 What is Authorization?

**Authorization** is the process of verifying **"What are you allowed to do?"** — determining what resources or actions an authenticated entity has permission to access.

**Authentication vs Authorization — Diagram**

```
┌──────────────────────────────────────────────────────────────────────────────┐
│              AUTHENTICATION vs AUTHORIZATION                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐       ┌──────────────────────────────────┐     │
│  │    AUTHENTICATION        │       │    AUTHORIZATION                  │     │
│  │    (Who are you?)        │       │    (What can you do?)             │     │
│  ├─────────────────────────┤       ├──────────────────────────────────┤     │
│  │                          │       │                                   │     │
│  │  • Verifies identity     │       │  • Verifies permissions           │     │
│  │  • Happens FIRST         │       │  • Happens AFTER authentication   │     │
│  │  • User provides         │       │  • System checks roles/policies   │     │
│  │    credentials           │       │  • Determines access level        │     │
│  │                          │       │                                   │     │
│  │  Methods:                │       │  Methods:                         │     │
│  │  - Username/Password     │       │  - Role-Based (RBAC)              │     │
│  │  - OAuth2 / OIDC         │       │  - Attribute-Based (ABAC)         │     │
│  │  - JWT Tokens            │       │  - Permission-Based               │     │
│  │  - Biometric             │       │  - ACL (Access Control List)      │     │
│  │  - MFA (Multi-Factor)    │       │                                   │     │
│  │  - Certificate-Based     │       │                                   │     │
│  └─────────────────────────┘       └──────────────────────────────────┘     │
│                                                                              │
│  Real-world Analogy:                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Airport Security                                                    │    │
│  │                                                                      │    │
│  │  Authentication = Showing your passport (proving who you are)        │    │
│  │  Authorization  = Your boarding pass (proving you can board Flight X) │    │
│  │                                                                      │    │
│  │  You need BOTH to board the plane!                                   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Spring Security Authentication Architecture — Diagram**

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│               SPRING SECURITY AUTHENTICATION ARCHITECTURE                        │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────┐   POST /login           ┌──────────────────────────────┐          │
│  │  Client   │ ──────────────────────►│  AuthenticationFilter         │          │
│  │           │  {user, password}       │  (UsernamePasswordAuth       │          │
│  └──────────┘                          │   enticationFilter)           │          │
│       ▲                                └──────────┬───────────────────┘          │
│       │                                           │                              │
│       │                                           │ Creates                      │
│       │                                           │ UsernamePasswordAuth         │
│       │                                           │ enticationToken              │
│       │                                           │ (unauthenticated)            │
│       │                                           ▼                              │
│       │                                ┌────────────────────────┐                │
│       │                                │  AuthenticationManager  │                │
│       │                                │  (ProviderManager)      │                │
│       │                                └──────────┬─────────────┘                │
│       │                                           │                              │
│       │                                           │ Delegates to                 │
│       │                                           ▼                              │
│       │                                ┌────────────────────────┐                │
│       │                                │ AuthenticationProvider  │                │
│       │                                │ (DaoAuthenticationProv) │                │
│       │                                └──────────┬─────────────┘                │
│       │                                           │                              │
│       │                         ┌─────────────────┼──────────────────┐           │
│       │                         │                 │                   │           │
│       │                         ▼                 ▼                   │           │
│       │              ┌──────────────────┐ ┌──────────────┐           │           │
│       │              │ UserDetailsService│ │ PasswordEncoder│          │           │
│       │              │ loadUserByUsername│ │ matches()      │          │           │
│       │              └────────┬─────────┘ └──────────────┘           │           │
│       │                       │                                      │           │
│       │                       ▼                                      │           │
│       │              ┌──────────────────┐                            │           │
│       │              │ UserDetails       │                            │           │
│       │              │ (username, pass,  │                            │           │
│       │              │  authorities)     │                            │           │
│       │              └──────────────────┘                            │           │
│       │                                                              │           │
│       │                         Authentication Success               │           │
│       │                                │                             │           │
│       │                                ▼                             │           │
│       │                 ┌────────────────────────────┐               │           │
│       │                 │  SecurityContextHolder      │               │           │
│       │                 │  stores Authentication obj  │               │           │
│       │                 │  (authenticated = true,     │               │           │
│       │                 │   principal, authorities)   │               │           │
│       │                 └────────────────────────────┘               │           │
│       │                                                              │           │
│       │ 200 OK + JWT/Session                                         │           │
│       └──────────────────────────────────────────────────────────────┘           │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Industry Use Case: E-commerce Platform with Role-Based Access**

```
┌─────────────────────────────────────────────────────────────────┐
│  E-COMMERCE ROLE-BASED ACCESS CONTROL                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CUSTOMER (ROLE_USER)        SELLER (ROLE_SELLER)              │
│  ├── View products ✓         ├── View products ✓               │
│  ├── Place orders ✓          ├── Manage own products ✓         │
│  ├── View own orders ✓       ├── View own orders ✓             │
│  ├── Manage own products ✗   ├── View analytics ✓              │
│  └── Admin panel ✗           └── Admin panel ✗                 │
│                                                                 │
│  ADMIN (ROLE_ADMIN)          SUPER_ADMIN (ROLE_SUPER_ADMIN)    │
│  ├── View products ✓         ├── Everything ADMIN can do ✓     │
│  ├── Manage all products ✓   ├── Manage users ✓                │
│  ├── View all orders ✓       ├── System configuration ✓        │
│  ├── View analytics ✓        └── Delete accounts ✓             │
│  └── Manage users ✗                                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Complete Authentication & Authorization Code**

**User Entity:**

```java
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;  // ROLE_USER, ROLE_SELLER, ROLE_ADMIN

    private boolean enabled = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }

    // Getters and Setters
}

public enum Role {
    ROLE_USER,
    ROLE_SELLER,
    ROLE_ADMIN,
    ROLE_SUPER_ADMIN
}
```

**Custom UserDetailsService:**

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found with username: " + username));
    }
}
```

**Security Configuration with Authentication & Authorization:**

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @PostAuthorize, @Secured
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // --- AUTHENTICATION ---
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // --- AUTHORIZATION (URL-based) ---
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Role-based access
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "SELLER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

                // Everything else needs authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**JWT Authentication Filter:**

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource()
                    .buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**Method-Level Authorization:**

```java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResourceController {

    private final OrderService orderService;
    private final ProductService productService;

    // Only authenticated users
    @GetMapping("/products")
    public List<Product> getProducts() {
        return productService.findAll();
    }

    // Only ADMIN can delete
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.delete(id);
    }

    // Only the owner of the order OR admin can view it
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.principal.username")
    @GetMapping("/orders/{username}")
    public List<Order> getUserOrders(@PathVariable String username) {
        return orderService.findByUsername(username);
    }

    // Only sellers can create products
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("/products")
    public Product createProduct(@RequestBody @Valid ProductRequest request) {
        return productService.create(request);
    }

    // Check authorization AFTER fetching (PostAuthorize)
    @PostAuthorize("returnObject.owner == authentication.principal.username or hasRole('ADMIN')")
    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.findById(id);
    }
}
```

**Authentication Endpoint (Login/Register):**

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword()));

        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
```

---


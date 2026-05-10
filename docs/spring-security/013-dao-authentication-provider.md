### 13. Deep Dive — DaoAuthenticationProvider, Session Creation & Post-Authentication Flow

---

#### 13.1 DaoAuthenticationProvider — The Heart of Username/Password Authentication

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHAT IS DaoAuthenticationProvider?                                                          │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  DaoAuthenticationProvider is a CONCRETE implementation of AuthenticationProvider            │
│  that is specifically designed for USERNAME + PASSWORD authentication.                       │
│                                                                                              │
│  "Dao" = Data Access Object — it uses a UserDetailsService (which is a DAO)                │
│  to FETCH user data from a data store (DB, LDAP, in-memory, etc.)                          │
│                                                                                              │
│  CLASS HIERARCHY:                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  AuthenticationProvider (interface)                                            │           │
│  │     │                                                                         │           │
│  │     └── AbstractUserDetailsAuthenticationProvider (abstract class)            │           │
│  │            │  • Template method pattern                                       │           │
│  │            │  • Defines authenticate() skeleton                               │           │
│  │            │  • Has UserCache (default = NullUserCache → no caching)          │           │
│  │            │  • Calls abstract retrieveUser() and additionalAuthenticationChecks()│      │
│  │            │                                                                    │           │
│  │            └── DaoAuthenticationProvider (concrete class)                      │           │
│  │                   • Implements retrieveUser() → calls UserDetailsService      │           │
│  │                   • Implements additionalAuthenticationChecks()                │           │
│  │                     → uses PasswordEncoder to compare passwords               │           │
│  │                   • This is what Spring Boot auto-configures for you           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  KEY DEPENDENCIES of DaoAuthenticationProvider:                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. UserDetailsService  — fetches UserDetails from data store                │           │
│  │     • YOUR implementation (e.g., CustomUserDetailsService)                    │           │
│  │     • Must return a UserDetails object                                        │           │
│  │                                                                               │           │
│  │  2. PasswordEncoder     — compares raw password with encoded password        │           │
│  │     • BCryptPasswordEncoder (recommended)                                     │           │
│  │     • The raw password from the login form is NEVER stored                   │           │
│  │     • encoder.matches(rawPassword, encodedPassword) → true/false             │           │
│  │                                                                               │           │
│  │  3. UserDetailsChecker  — pre-authentication checks                          │           │
│  │     • DefaultPreAuthenticationChecks: isAccountNonLocked(),                   │           │
│  │       isEnabled(), isAccountNonExpired()                                      │           │
│  │     • DefaultPostAuthenticationChecks: isCredentialsNonExpired()              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.2 DaoAuthenticationProvider — Internal authenticate() Flow (Step-by-Step)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  DaoAuthenticationProvider.authenticate() — COMPLETE INTERNAL FLOW                           │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  INPUT: UsernamePasswordAuthenticationToken (unauthenticated)                               │
│         { principal: "john", credentials: "john123", authenticated: false }                 │
│                                                                                              │
│  ── STEP 1: Extract username from the Authentication token ──────────────────               │
│                                                                                              │
│  String username = authentication.getName();  // "john"                                     │
│                                                                                              │
│                                                                                              │
│  ── STEP 2: Fetch UserDetails via UserDetailsService ────────────────────────               │
│                                                                                              │
│  UserDetails userDetails = this.userDetailsService.loadUserByUsername("john");               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐                   │
│  │  YOUR CustomUserDetailsService.loadUserByUsername("john"):           │                   │
│  │                                                                      │                   │
│  │  1. Query database: SELECT * FROM users WHERE username = 'john'     │                   │
│  │  2. If not found → throw UsernameNotFoundException                  │                   │
│  │  3. If found → build and return UserDetails object:                 │                   │
│  │     UserDetails {                                                    │                   │
│  │       username: "john",                                              │                   │
│  │       password: "$2a$10$xYz..." (BCrypt hash from DB),              │                   │
│  │       authorities: [ROLE_USER],                                      │                   │
│  │       accountNonExpired: true,                                       │                   │
│  │       accountNonLocked: true,                                        │                   │
│  │       credentialsNonExpired: true,                                   │                   │
│  │       enabled: true                                                  │                   │
│  │     }                                                                │                   │
│  └──────────────────────────────────────────────────────────────────────┘                   │
│                                                                                              │
│  ⚠️ If UsernameNotFoundException is thrown:                                                  │
│     → DaoAuthenticationProvider catches it                                                  │
│     → Throws BadCredentialsException("Bad credentials")                                    │
│     → This is intentional — to NOT reveal whether username exists or not                   │
│       (prevents username enumeration attacks)                                               │
│                                                                                              │
│                                                                                              │
│  ── STEP 3: Pre-Authentication Checks ──────────────────────────────────────               │
│                                                                                              │
│  DefaultPreAuthenticationChecks.check(userDetails):                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐                   │
│  │  if (!userDetails.isAccountNonLocked())                              │                   │
│  │      throw new LockedException("User account is locked");           │                   │
│  │                                                                      │                   │
│  │  if (!userDetails.isEnabled())                                       │                   │
│  │      throw new DisabledException("User is disabled");                │                   │
│  │                                                                      │                   │
│  │  if (!userDetails.isAccountNonExpired())                              │                   │
│  │      throw new AccountExpiredException("User account has expired");  │                   │
│  └──────────────────────────────────────────────────────────────────────┘                   │
│                                                                                              │
│  These checks happen BEFORE password comparison!                                            │
│  Why? No point comparing passwords if the account is locked/disabled.                       │
│                                                                                              │
│                                                                                              │
│  ── STEP 4: Password Comparison (additionalAuthenticationChecks) ────────                  │
│                                                                                              │
│  DaoAuthenticationProvider.additionalAuthenticationChecks(userDetails, authentication):     │
│  ┌──────────────────────────────────────────────────────────────────────┐                   │
│  │                                                                      │                   │
│  │  String rawPassword = authentication.getCredentials().toString();    │                   │
│  │  // rawPassword = "john123" (from login form)                        │                   │
│  │                                                                      │                   │
│  │  String encodedPassword = userDetails.getPassword();                 │                   │
│  │  // encodedPassword = "$2a$10$xYz..." (BCrypt hash from DB)          │                   │
│  │                                                                      │                   │
│  │  boolean matches = this.passwordEncoder.matches(                     │                   │
│  │      rawPassword,      // "john123"                                  │                   │
│  │      encodedPassword   // "$2a$10$xYz..."                            │                   │
│  │  );                                                                  │                   │
│  │                                                                      │                   │
│  │  if (!matches) {                                                     │                   │
│  │      throw new BadCredentialsException("Bad credentials");           │                   │
│  │  }                                                                   │                   │
│  │                                                                      │                   │
│  │  // If matches → password is correct! Continue to Step 5            │                   │
│  │                                                                      │                   │
│  └──────────────────────────────────────────────────────────────────────┘                   │
│                                                                                              │
│  HOW BCryptPasswordEncoder.matches() works:                                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐                   │
│  │  Raw: "john123"                                                      │                   │
│  │  Stored: "$2a$10$xYzABC...encodedHash..."                            │                   │
│  │                                                                      │                   │
│  │  1. Extract salt from stored hash: "$2a$10$xYzABC..."               │                   │
│  │  2. Hash the raw password with the SAME salt                        │                   │
│  │  3. Compare the result with stored hash                             │                   │
│  │  4. If identical → return true (password correct!)                  │                   │
│  │                                                                      │                   │
│  │  Note: BCrypt NEVER decrypts the stored hash.                       │                   │
│  │  It hashes the raw password and compares hashes.                    │                   │
│  └──────────────────────────────────────────────────────────────────────┘                   │
│                                                                                              │
│                                                                                              │
│  ── STEP 5: Post-Authentication Checks ─────────────────────────────────                   │
│                                                                                              │
│  DefaultPostAuthenticationChecks.check(userDetails):                                        │
│  ┌──────────────────────────────────────────────────────────────────────┐                   │
│  │  if (!userDetails.isCredentialsNonExpired())                         │                   │
│  │      throw new CredentialsExpiredException(                          │                   │
│  │          "User credentials have expired"                             │                   │
│  │      );                                                              │                   │
│  └──────────────────────────────────────────────────────────────────────┘                   │
│                                                                                              │
│  This check happens AFTER password comparison succeeds.                                     │
│  Use case: force password reset every 90 days.                                              │
│                                                                                              │
│                                                                                              │
│  ── STEP 6: Erase Credentials ──────────────────────────────────────────                   │
│                                                                                              │
│  After successful authentication, DaoAuthenticationProvider calls:                          │
│  ┌──────────────────────────────────────────────────────────────────────┐                   │
│  │  userDetails.eraseCredentials();                                     │                   │
│  │  // Sets the password field to null                                  │                   │
│  │  // Why? So the plaintext password is NOT kept in memory             │                   │
│  │  // after authentication is complete (security best practice)        │                   │
│  └──────────────────────────────────────────────────────────────────────┘                   │
│                                                                                              │
│  NOTE: This only works if your UserDetails class implements                                 │
│  CredentialsContainer (which Spring's User class does by default).                          │
│  If you use a custom UserAuthEntity, implement eraseCredentials()                           │
│  or extend Spring's User class.                                                             │
│                                                                                              │
│                                                                                              │
│  ── STEP 7: Create AUTHENTICATED UsernamePasswordAuthenticationToken ───                   │
│                                                                                              │
│  return UsernamePasswordAuthenticationToken.authenticated(                                   │
│      userDetails,        // principal  → the full UserDetails object                        │
│      null,               // credentials → erased (set to null for security)                 │
│      userDetails.getAuthorities()  // authorities → [ROLE_USER]                             │
│  );                                                                                         │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐                   │
│  │  BEFORE authentication (created by UsernamePasswordAuthFilter):     │                   │
│  │  UsernamePasswordAuthenticationToken {                               │                   │
│  │      principal: "john"          ← just a String (username)          │                   │
│  │      credentials: "john123"     ← raw password (plaintext)          │                   │
│  │      authorities: []            ← empty                              │                   │
│  │      authenticated: false       ← NOT authenticated                  │                   │
│  │      details: WebAuthDetails    ← IP, session ID                     │                   │
│  │  }                                                                   │                   │
│  │                                                                      │                   │
│  │  AFTER authentication (returned by DaoAuthenticationProvider):      │                   │
│  │  UsernamePasswordAuthenticationToken {                               │                   │
│  │      principal: UserDetails {   ← full UserDetails object           │                   │
│  │          username: "john",                                           │                   │
│  │          password: null,        ← erased!                            │                   │
│  │          authorities: [ROLE_USER],                                   │                   │
│  │          enabled: true, ...                                          │                   │
│  │      }                                                               │                   │
│  │      credentials: null          ← erased for security               │                   │
│  │      authorities: [ROLE_USER]   ← populated from UserDetails        │                   │
│  │      authenticated: true        ← NOW authenticated!                │                   │
│  │      details: WebAuthDetails    ← preserved from original token     │                   │
│  │  }                                                                   │                   │
│  └──────────────────────────────────────────────────────────────────────┘                   │
│                                                                                              │
│  ★ KEY POINT: The token CHANGES from unauthenticated to authenticated.                     │
│  It's NOT the same object — a NEW token is created with the static factory:                │
│  UsernamePasswordAuthenticationToken.authenticated(principal, credentials, authorities)     │
│  This internally sets super.setAuthenticated(true) which is a TRUSTED call                 │
│  (unlike the public setAuthenticated() which throws an exception).                          │
│                                                                                              │
│                                                                                              │
│  OUTPUT: UsernamePasswordAuthenticationToken (authenticated)                                │
│          { principal: UserDetails{john}, credentials: null, authenticated: true,            │
│            authorities: [ROLE_USER] }                                                       │
│                                                                                              │
│  ★ AT THIS POINT: NO SESSION HAS BEEN CREATED YET!                                         │
│  The DaoAuthenticationProvider only validates credentials and returns the token.            │
│  Session creation happens in the NEXT phase.                                                │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.3 Visual Flow — DaoAuthenticationProvider Step-by-Step

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  DaoAuthenticationProvider — VISUAL FLOW DIAGRAM                                             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  UsernamePasswordAuthenticationFilter                                                       │
│       │                                                                                      │
│       │  Creates: UsernamePasswordAuthenticationToken("john", "john123")                    │
│       │           { authenticated: false }                                                   │
│       │                                                                                      │
│       ▼                                                                                      │
│  AuthenticationManager (ProviderManager)                                                    │
│       │                                                                                      │
│       │  Iterates through List<AuthenticationProvider>                                      │
│       │  Finds: DaoAuthenticationProvider.supports(                                         │
│       │            UsernamePasswordAuthenticationToken.class) → true                        │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─── DaoAuthenticationProvider.authenticate(token) ──────────────────────────────────┐    │
│  │                                                                                     │    │
│  │  Step 1: username = token.getName() → "john"                                       │    │
│  │       │                                                                             │    │
│  │       ▼                                                                             │    │
│  │  Step 2: userDetails = userDetailsService.loadUserByUsername("john")                │    │
│  │       │                                                                             │    │
│  │       │  ┌───────────────────────────────────────────────────────┐                 │    │
│  │       │  │  CustomUserDetailsService                             │                 │    │
│  │       │  │  → userRepository.findByUsername("john")              │                 │    │
│  │       │  │  → SELECT * FROM users WHERE username = 'john'       │                 │    │
│  │       │  │  → Returns: UserAuthEntity {                          │                 │    │
│  │       │  │       id: 1,                                          │                 │    │
│  │       │  │       username: "john",                                │                 │    │
│  │       │  │       password: "$2a$10$xYz...",                       │                 │    │
│  │       │  │       role: "ROLE_USER"                                │                 │    │
│  │       │  │    }                                                   │                 │    │
│  │       │  └───────────────────────────────────────────────────────┘                 │    │
│  │       │              │                                                              │    │
│  │       │              │ Not found? → UsernameNotFoundException                      │    │
│  │       │              │              → caught → BadCredentialsException             │    │
│  │       │              │                                                              │    │
│  │       ▼              ▼                                                              │    │
│  │  Step 3: PRE-AUTH CHECKS                                                           │    │
│  │       │  ├── isAccountNonLocked()?    → No → LockedException                      │    │
│  │       │  ├── isEnabled()?             → No → DisabledException                    │    │
│  │       │  └── isAccountNonExpired()?   → No → AccountExpiredException              │    │
│  │       │                                                                             │    │
│  │       ▼                                                                             │    │
│  │  Step 4: PASSWORD COMPARISON                                                       │    │
│  │       │  passwordEncoder.matches("john123", "$2a$10$xYz...")                       │    │
│  │       │  ├── Match?  → Continue to Step 5                                          │    │
│  │       │  └── No match? → BadCredentialsException("Bad credentials")               │    │
│  │       │                                                                             │    │
│  │       ▼                                                                             │    │
│  │  Step 5: POST-AUTH CHECKS                                                          │    │
│  │       │  └── isCredentialsNonExpired()? → No → CredentialsExpiredException        │    │
│  │       │                                                                             │    │
│  │       ▼                                                                             │    │
│  │  Step 6: ERASE CREDENTIALS                                                         │    │
│  │       │  userDetails.eraseCredentials() → password set to null                     │    │
│  │       │                                                                             │    │
│  │       ▼                                                                             │    │
│  │  Step 7: CREATE AUTHENTICATED TOKEN                                                │    │
│  │       │  return UsernamePasswordAuthenticationToken.authenticated(                 │    │
│  │       │      userDetails,              // principal (UserDetails with null pwd)    │    │
│  │       │      null,                     // credentials (erased)                     │    │
│  │       │      userDetails.getAuthorities()  // [ROLE_USER]                         │    │
│  │       │  );                                                                        │    │
│  │       │  // authenticated = TRUE                                                   │    │
│  │       │                                                                             │    │
│  └───────┼─────────────────────────────────────────────────────────────────────────────┘    │
│          │                                                                                   │
│          ▼                                                                                   │
│  ★ Authenticated token returned to UsernamePasswordAuthenticationFilter                     │
│  ★ NO SESSION CREATED YET — only credential validation is complete                          │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.4 DaoAuthenticationProvider — Actual Source Code (Simplified)

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AbstractUserDetailsAuthenticationProvider.authenticate() — SIMPLIFIED SOURCE
//  (Parent class of DaoAuthenticationProvider)
// ═══════════════════════════════════════════════════════════════════════════════

public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    // Step 1: Extract username
    String username = determineUsername(authentication);

    // Step 2: Try to load from cache first (default = NullUserCache → always misses)
    boolean cacheWasUsed = true;
    UserDetails user = this.userCache.getUserFromCache(username);

    if (user == null) {
        cacheWasUsed = false;

        // Step 2b: Load from UserDetailsService (YOUR implementation)
        // This is the abstract method implemented by DaoAuthenticationProvider
        user = retrieveUser(username, (UsernamePasswordAuthenticationToken) authentication);
        // DaoAuthenticationProvider.retrieveUser() internally calls:
        //   this.getUserDetailsService().loadUserByUsername(username)
    }

    try {
        // Step 3: Pre-authentication checks (locked? disabled? expired?)
        this.preAuthenticationChecks.check(user);

        // Step 4: Password comparison (abstract method → DaoAuthenticationProvider)
        additionalAuthenticationChecks(user, (UsernamePasswordAuthenticationToken) authentication);
        // DaoAuthenticationProvider.additionalAuthenticationChecks() internally calls:
        //   this.passwordEncoder.matches(rawPassword, user.getPassword())

    } catch (AuthenticationException ex) {
        // If cache was used and auth failed, try again without cache
        if (cacheWasUsed) {
            cacheWasUsed = false;
            user = retrieveUser(username, (UsernamePasswordAuthenticationToken) authentication);
            this.preAuthenticationChecks.check(user);
            additionalAuthenticationChecks(user, (UsernamePasswordAuthenticationToken) authentication);
        } else {
            throw ex;  // Re-throw the exception
        }
    }

    // Step 5: Post-authentication checks (credentials expired?)
    this.postAuthenticationChecks.check(user);

    // Put in cache for future use
    if (!cacheWasUsed) {
        this.userCache.putUserInCache(user);
    }

    // Step 6 & 7: Create the authenticated token
    Object principalToReturn = user;
    // forcePrincipalAsString = false by default → returns UserDetails object
    if (this.forcePrincipalAsString) {
        principalToReturn = user.getUsername();
    }

    return createSuccessAuthentication(principalToReturn, authentication, user);
}

// ═══════════════════════════════════════════════════════════════════════════════
//  createSuccessAuthentication() — Creates the AUTHENTICATED token
// ═══════════════════════════════════════════════════════════════════════════════

protected Authentication createSuccessAuthentication(
        Object principal, Authentication authentication, UserDetails user) {

    // ★ This is where the authenticated token is created!
    UsernamePasswordAuthenticationToken result =
        UsernamePasswordAuthenticationToken.authenticated(
            principal,                    // UserDetails object (with password erased)
            authentication.getCredentials(), // will be erased below
            user.getAuthorities()          // [ROLE_USER, ROLE_ADMIN, etc.]
        );

    result.setDetails(authentication.getDetails());  // preserve WebAuthenticationDetails

    // ★ Erase credentials from the token (security best practice)
    // This calls result.eraseCredentials() → sets credentials to null
    // Also calls userDetails.eraseCredentials() → sets password to null
    this.eraseCredentialsAfterAuthentication = true; // default
    // (erasing happens in ProviderManager after this method returns)

    return result;
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  DaoAuthenticationProvider — SIMPLIFIED SOURCE (extends AbstractUserDetailsAuthenticationProvider)
// ═══════════════════════════════════════════════════════════════════════════════

public class DaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    private UserDetailsService userDetailsService;
    private PasswordEncoder passwordEncoder;

    // ── retrieveUser() — Called by parent's authenticate() ──────────────────
    @Override
    protected UserDetails retrieveUser(String username,
            UsernamePasswordAuthenticationToken authentication) {
        try {
            // ★ YOUR UserDetailsService is called here!
            UserDetails loadedUser = this.getUserDetailsService().loadUserByUsername(username);

            if (loadedUser == null) {
                throw new InternalAuthenticationServiceException(
                    "UserDetailsService returned null, which is an interface contract violation"
                );
            }
            return loadedUser;

        } catch (UsernameNotFoundException ex) {
            // ★ Username not found — but we HIDE this information
            // The exception is caught by the parent class and converted to
            // BadCredentialsException (to prevent username enumeration)
            if (this.hideUserNotFoundExceptions) {  // default = true
                throw new BadCredentialsException("Bad credentials");
            }
            throw ex;
        }
    }

    // ── additionalAuthenticationChecks() — Password comparison ──────────────
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication) {

        if (authentication.getCredentials() == null) {
            throw new BadCredentialsException("Bad credentials");
        }

        String presentedPassword = authentication.getCredentials().toString();

        // ★ Password comparison using PasswordEncoder!
        if (!this.passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
    }

    // ── supports() — Which Authentication tokens this provider handles ──────
    // Inherited from AbstractUserDetailsAuthenticationProvider:
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

---

#### 13.5 Session Creation — What Happens AFTER DaoAuthenticationProvider Returns

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SESSION CREATION FLOW — FROM AUTHENTICATED TOKEN TO HttpSession                             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ★ CRITICAL CONCEPT: DaoAuthenticationProvider does NOT create sessions.                    │
│  It ONLY validates credentials and returns an authenticated token.                          │
│  The session creation happens in subsequent filters/handlers.                                │
│                                                                                              │
│                                                                                              │
│  ── THE COMPLETE CHAIN (After DaoAuthenticationProvider returns) ─────────────              │
│                                                                                              │
│  DaoAuthenticationProvider                                                                   │
│       │                                                                                      │
│       │  Returns: authenticated UsernamePasswordAuthenticationToken                         │
│       │                                                                                      │
│       ▼                                                                                      │
│  ProviderManager (AuthenticationManager)                                                    │
│       │                                                                                      │
│       │  1. Receives the authenticated token from DaoAuthenticationProvider                 │
│       │  2. Calls token.eraseCredentials() → password set to null in token                 │
│       │  3. Returns the authenticated token to UsernamePasswordAuthenticationFilter         │
│       │                                                                                      │
│       ▼                                                                                      │
│  UsernamePasswordAuthenticationFilter.successfulAuthentication()                             │
│       │                                                                                      │
│       │  This method does THREE critical things:                                            │
│       │                                                                                      │
│       │  ┌──────────────────────────────────────────────────────────────────────┐           │
│       │  │  1. SET SecurityContext in SecurityContextHolder                     │           │
│       │  │                                                                      │           │
│       │  │     SecurityContext context = SecurityContextHolder.createEmptyContext();│        │
│       │  │     context.setAuthentication(authenticatedToken);                   │           │
│       │  │     SecurityContextHolder.setContext(context);                       │           │
│       │  │                                                                      │           │
│       │  │     // ThreadLocal now holds: SecurityContext { Authentication {     │           │
│       │  │     //   principal: UserDetails{john}, authenticated: true,          │           │
│       │  │     //   authorities: [ROLE_USER] } }                               │           │
│       │  │                                                                      │           │
│       │  │  2. SAVE SecurityContext to SecurityContextRepository                │           │
│       │  │     (This is where the SESSION is created!)                          │           │
│       │  │                                                                      │           │
│       │  │     this.securityContextRepository.saveContext(context, req, resp);  │           │
│       │  │     // → HttpSessionSecurityContextRepository.saveContext()          │           │
│       │  │     // → Creates HttpSession + stores SecurityContext inside it     │           │
│       │  │                                                                      │           │
│       │  │  3. CALL AuthenticationSuccessHandler                               │           │
│       │  │     this.successHandler.onAuthenticationSuccess(req, resp, auth);    │           │
│       │  │     // → Redirects to saved request URL or default URL              │           │
│       │  │                                                                      │           │
│       │  └──────────────────────────────────────────────────────────────────────┘           │
│       │                                                                                      │
│       ▼                                                                                      │
│  SecurityContextRepository.saveContext()                                                     │
│  (implementation: HttpSessionSecurityContextRepository)                                     │
│       │                                                                                      │
│       │  ┌──────────────────────────────────────────────────────────────────────┐           │
│       │  │  HttpSessionSecurityContextRepository.saveContext():                  │           │
│       │  │                                                                      │           │
│       │  │  1. HttpSession session = request.getSession(true);                  │           │
│       │  │     // ★ THIS IS WHERE THE HttpSession IS CREATED!                  │           │
│       │  │     // Tomcat generates a new JSESSIONID (UUID)                     │           │
│       │  │     // e.g., "ABC123-DEF456-GHI789"                                 │           │
│       │  │                                                                      │           │
│       │  │  2. session.setAttribute(                                            │           │
│       │  │         "SPRING_SECURITY_CONTEXT",  // attribute key (constant)     │           │
│       │  │         securityContext              // the SecurityContext object   │           │
│       │  │     );                                                               │           │
│       │  │     // ★ SecurityContext is now STORED in the HttpSession!          │           │
│       │  │     // The HttpSession contains:                                    │           │
│       │  │     // { "SPRING_SECURITY_CONTEXT" → SecurityContext {              │           │
│       │  │     //     authentication: UsernamePasswordAuthenticationToken {    │           │
│       │  │     //       principal: UserDetails{john},                          │           │
│       │  │     //       authenticated: true,                                   │           │
│       │  │     //       authorities: [ROLE_USER]                               │           │
│       │  │     //     }                                                        │           │
│       │  │     //   }                                                          │           │
│       │  │     // }                                                            │           │
│       │  │                                                                      │           │
│       │  └──────────────────────────────────────────────────────────────────────┘           │
│       │                                                                                      │
│       ▼                                                                                      │
│  Response sent to browser                                                                    │
│       │                                                                                      │
│       │  HTTP Response Headers:                                                             │
│       │  Set-Cookie: JSESSIONID=ABC123-DEF456-GHI789; Path=/; HttpOnly                     │
│       │  Location: /dashboard (or the original saved request URL)                           │
│       │  Status: 302 Found                                                                  │
│       │                                                                                      │
│       ▼                                                                                      │
│  Browser stores the JSESSIONID cookie and follows the redirect                              │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.6 ⚠️ Deprecation Notice — HttpSessionSecurityContextRepository (Spring Security 6.x)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ⚠️ DEPRECATION — HttpSessionSecurityContextRepository in Spring Security 6.x               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  In Spring Security 5.x (Spring Boot 2.x):                                                 │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  • SecurityContextPersistenceFilter was used (NOW DEPRECATED!)              │           │
│  │  • It used HttpSessionSecurityContextRepository internally                  │           │
│  │  • It loaded SecurityContext BEFORE the request AND saved it AFTER          │           │
│  │  • The SecurityContext was automatically persisted to HttpSession           │           │
│  │  • Problem: auto-save behavior could lead to unexpected session creation   │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  In Spring Security 6.x (Spring Boot 3.x) — CURRENT:                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  • SecurityContextHolderFilter REPLACED SecurityContextPersistenceFilter    │           │
│  │  • HttpSessionSecurityContextRepository is still used but behavior changed  │           │
│  │  • SecurityContext is NO LONGER auto-saved after request!                   │           │
│  │  • You must EXPLICITLY save the SecurityContext using                       │           │
│  │    SecurityContextRepository.saveContext()                                  │           │
│  │                                                                              │           │
│  │  WHY the change?                                                             │           │
│  │  • More explicit control over when sessions are created                     │           │
│  │  • Prevents accidental session creation for stateless APIs                  │           │
│  │  • Better support for reactive and non-servlet environments                 │           │
│  │  • Follows the principle of least surprise                                  │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── COMPARISON: Old vs New ──────────────────────────────────────────────────               │
│                                                                                              │
│  ┌────────────────────────────────────┬────────────────────────────────────────┐            │
│  │  Spring Security 5.x (OLD)         │  Spring Security 6.x (CURRENT)         │            │
│  │  Spring Boot 2.x                   │  Spring Boot 3.x                       │            │
│  ├────────────────────────────────────┼────────────────────────────────────────┤            │
│  │  SecurityContextPersistenceFilter  │  SecurityContextHolderFilter           │            │
│  │  (DEPRECATED in 5.7+)             │  (replacement)                          │            │
│  ├────────────────────────────────────┼────────────────────────────────────────┤            │
│  │  Auto-LOADS SecurityContext from   │  LOADS SecurityContext from            │            │
│  │  HttpSession at start of request   │  HttpSession at start of request      │            │
│  │                                    │  (same behavior for loading)           │            │
│  ├────────────────────────────────────┼────────────────────────────────────────┤            │
│  │  Auto-SAVES SecurityContext to     │  Does NOT auto-save!                   │            │
│  │  HttpSession at end of request     │  SecurityContext must be saved         │            │
│  │  (always, even if unchanged)       │  EXPLICITLY by the authentication     │            │
│  │                                    │  filter (e.g., UsernamePassword        │            │
│  │                                    │  AuthenticationFilter calls            │            │
│  │                                    │  securityContextRepository.saveContext)│            │
│  ├────────────────────────────────────┼────────────────────────────────────────┤            │
│  │  SecurityContextRepository         │  SecurityContextRepository            │            │
│  │  (interface)                        │  (interface — same)                    │            │
│  │  └── HttpSessionSecurityContext    │  ├── HttpSessionSecurityContext       │            │
│  │      Repository (default)           │  │   Repository (default for form)    │            │
│  │                                    │  ├── RequestAttributeSecurityContext  │            │
│  │                                    │  │   Repository (for stateless APIs)  │            │
│  │                                    │  └── DelegatingSecurityContext        │            │
│  │                                    │      Repository (composite)            │            │
│  └────────────────────────────────────┴────────────────────────────────────────┘            │
│                                                                                              │
│                                                                                              │
│  ── HOW TO CONFIGURE (Spring Security 6.x / Spring Boot 3.x) ───────────────               │
│                                                                                              │
│  By default, when you use .formLogin(), Spring Security 6.x:                                │
│  1. Registers SecurityContextHolderFilter (not the deprecated one)                          │
│  2. Uses HttpSessionSecurityContextRepository as the SecurityContextRepository              │
│  3. The UsernamePasswordAuthenticationFilter explicitly calls                               │
│     securityContextRepository.saveContext() on successful authentication                    │
│                                                                                              │
│  You DON'T need to change anything for form-based login!                                    │
│  The explicit save is handled internally by the authentication filter.                      │
│                                                                                              │
│  ⚠️ BUT if you write a CUSTOM authentication filter, YOU must save explicitly:              │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Spring Security 6.x — Explicit SecurityContext save in custom filters
// ═══════════════════════════════════════════════════════════════════════════════

// ★ OLD WAY (Spring Security 5.x — DEPRECATED)
// SecurityContextPersistenceFilter auto-saved the SecurityContext at end of request.
// You just had to set the authentication and it was auto-persisted:
SecurityContextHolder.getContext().setAuthentication(authToken);
// ↑ This was enough — SecurityContextPersistenceFilter would auto-save to HttpSession


// ★ NEW WAY (Spring Security 6.x — CURRENT)
// You MUST explicitly save the SecurityContext using SecurityContextRepository:

@Component
public class CustomAuthFilter extends OncePerRequestFilter {

    private final SecurityContextRepository securityContextRepository =
        new HttpSessionSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) {

        // ... your custom authentication logic ...

        // After successful authentication:
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticatedToken);
        SecurityContextHolder.setContext(context);

        // ★ YOU MUST EXPLICITLY SAVE — otherwise no session is created!
        this.securityContextRepository.saveContext(context, request, response);
        //                              ↑
        // This creates the HttpSession and stores the SecurityContext in it.
        // Without this line, the user will NOT be remembered on next request!

        chain.doFilter(request, response);
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig — Configuring SecurityContextRepository (Spring Security 6.x)
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // ★ For form-based login — HttpSessionSecurityContextRepository is default
        // No explicit configuration needed — Spring Security handles it:
        http
            .formLogin(Customizer.withDefaults())  // uses HttpSession automatically
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            );

        // ★ For stateless APIs (JWT) — use RequestAttributeSecurityContextRepository
        // This tells Spring Security to NOT create HttpSessions:
        // http
        //     .securityContext(ctx -> ctx
        //         .securityContextRepository(new RequestAttributeSecurityContextRepository())
        //     )
        //     .sessionManagement(session -> session
        //         .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        //     );

        return http.build();
    }
}
```

---

#### 13.7 SecurityContextHolderFilter — Loading SecurityContext on Subsequent Requests

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SecurityContextHolderFilter — WHAT HAPPENS ON SUBSEQUENT REQUESTS                           │
│  (After the user is already authenticated and has a JSESSIONID cookie)                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ── REQUEST: GET /dashboard ─────────────────────────────────────────────────               │
│  Cookie: JSESSIONID=ABC123-DEF456-GHI789                                                    │
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐                │
│  │  STEP 1: SecurityContextHolderFilter executes (FIRST security filter)  │                │
│  │                                                                         │                │
│  │  SecurityContextHolderFilter.doFilter():                               │                │
│  │                                                                         │                │
│  │  // Load SecurityContext from the repository (HttpSession)             │                │
│  │  Supplier<SecurityContext> deferredContext =                            │                │
│  │      this.securityContextRepository.loadDeferredContext(request);       │                │
│  │                                                                         │                │
│  │  // Set it in the SecurityContextHolder (ThreadLocal)                  │                │
│  │  SecurityContextHolder.setDeferredContext(deferredContext);             │                │
│  │  // ★ In Spring Security 6.x, loading is DEFERRED (lazy)             │                │
│  │  // The actual HttpSession lookup only happens when                    │                │
│  │  // SecurityContextHolder.getContext() is first called                 │                │
│  │                                                                         │                │
│  └─────────────────────────────────────────────────────────────────────────┘                │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐                │
│  │  STEP 2: When SecurityContextHolder.getContext() is called             │                │
│  │  (by AuthorizationFilter or any other filter/code)                     │                │
│  │                                                                         │                │
│  │  HttpSessionSecurityContextRepository.loadContext(request):            │                │
│  │                                                                         │                │
│  │  1. HttpSession session = request.getSession(false);                   │                │
│  │     // false = don't create new session, just look for existing one    │                │
│  │     // Tomcat looks up the session using JSESSIONID cookie value       │                │
│  │                                                                         │                │
│  │  2. if (session == null) {                                             │                │
│  │         return SecurityContextHolder.createEmptyContext();              │                │
│  │         // No session → no authentication → anonymous user            │                │
│  │     }                                                                   │                │
│  │                                                                         │                │
│  │  3. Object contextFromSession = session.getAttribute(                  │                │
│  │         "SPRING_SECURITY_CONTEXT"                                      │                │
│  │     );                                                                  │                │
│  │     // Retrieves the SecurityContext stored during login               │                │
│  │                                                                         │                │
│  │  4. if (contextFromSession == null) {                                  │                │
│  │         return SecurityContextHolder.createEmptyContext();              │                │
│  │     }                                                                   │                │
│  │                                                                         │                │
│  │  5. return (SecurityContext) contextFromSession;                        │                │
│  │     // SecurityContext { Authentication {                               │                │
│  │     //   principal: UserDetails{john}, authenticated: true,            │                │
│  │     //   authorities: [ROLE_USER] } }                                  │                │
│  │                                                                         │                │
│  └─────────────────────────────────────────────────────────────────────────┘                │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐                │
│  │  STEP 3: SecurityContext is now in SecurityContextHolder (ThreadLocal)  │                │
│  │                                                                         │                │
│  │  SecurityContextHolder (ThreadLocal):                                  │                │
│  │  ┌──────────────────────────────────────────────────────────┐          │                │
│  │  │  SecurityContext {                                        │          │                │
│  │  │    authentication: UsernamePasswordAuthenticationToken {  │          │                │
│  │  │      principal: UserDetails { username: "john", ... },    │          │                │
│  │  │      credentials: null,                                   │          │                │
│  │  │      authorities: [ROLE_USER],                            │          │                │
│  │  │      authenticated: true                                  │          │                │
│  │  │    }                                                      │          │                │
│  │  │  }                                                        │          │                │
│  │  └──────────────────────────────────────────────────────────┘          │                │
│  │                                                                         │                │
│  │  ★ This SecurityContext is available for the ENTIRE request lifecycle  │                │
│  │  ★ Any code can access it via:                                         │                │
│  │    SecurityContextHolder.getContext().getAuthentication()               │                │
│  │    or @AuthenticationPrincipal annotation in controllers                │                │
│  │                                                                         │                │
│  └─────────────────────────────────────────────────────────────────────────┘                │
│       │                                                                                      │
│       │  Continue through remaining security filters...                                     │
│       │                                                                                      │
│       ▼                                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐                │
│  │  STEP 4: AuthorizationFilter checks access                             │                │
│  │                                                                         │                │
│  │  AuthorizationFilter.doFilter():                                       │                │
│  │  Authentication auth = SecurityContextHolder.getContext()               │                │
│  │                             .getAuthentication();                       │                │
│  │  // auth = UsernamePasswordAuthenticationToken { john, ROLE_USER }     │                │
│  │                                                                         │                │
│  │  AuthorizationDecision decision = this.authorizationManager.check(     │                │
│  │      () -> auth,  // Supplier<Authentication>                          │                │
│  │      request      // the HttpServletRequest                            │                │
│  │  );                                                                     │                │
│  │                                                                         │                │
│  │  if (decision is DENIED) {                                             │                │
│  │      throw new AccessDeniedException("Access Denied");                 │                │
│  │      // → caught by ExceptionTranslationFilter                        │                │
│  │      // → returns 403 Forbidden                                        │                │
│  │  }                                                                      │                │
│  │  // If GRANTED → continue to DispatcherServlet → Controller           │                │
│  │                                                                         │                │
│  └─────────────────────────────────────────────────────────────────────────┘                │
│       │                                                                                      │
│       ▼                                                                                      │
│  DispatcherServlet → HandlerMapping → HandlerInterceptors → Controller                     │
│                                                                                              │
│                                                                                              │
│  ── WHAT IF JSESSIONID IS INVALID? ──────────────────────────────────────                   │
│                                                                                              │
│  Cookie: JSESSIONID=INVALID-SESSION-ID                                                      │
│       │                                                                                      │
│       ▼                                                                                      │
│  SecurityContextHolderFilter:                                                                │
│  → request.getSession(false) → Tomcat checks "INVALID-SESSION-ID"                          │
│  → NOT found in session manager → returns null                                              │
│  → SecurityContext = empty (no Authentication)                                              │
│       │                                                                                      │
│       ▼                                                                                      │
│  AuthorizationFilter:                                                                        │
│  → authentication = null (or AnonymousAuthenticationToken)                                  │
│  → User is NOT authenticated → AccessDeniedException                                       │
│       │                                                                                      │
│       ▼                                                                                      │
│  ExceptionTranslationFilter:                                                                 │
│  → Saves the original request URL to RequestCache                                           │
│  → Redirects to /login (302 Found)                                                          │
│  → User sees the login page again                                                           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.8 Visual Flow — Complete Request Lifecycle Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  COMPLETE REQUEST LIFECYCLE — FIRST LOGIN + SUBSEQUENT REQUEST                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PHASE 1: FIRST LOGIN (POST /login)                                                        │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  Browser                              Server                                                │
│  ═══════                              ══════                                                │
│    │                                      │                                                  │
│    │  POST /login                         │                                                  │
│    │  Body: username=john&password=123    │                                                  │
│    │  (NO cookie — first time)            │                                                  │
│    │─────────────────────────────────────>│                                                  │
│    │                                      │                                                  │
│    │                  ┌───────────────────────────────────────────────────┐                  │
│    │                  │  SecurityContextHolderFilter                      │                  │
│    │                  │  → No JSESSIONID → empty SecurityContext         │                  │
│    │                  │  → SecurityContextHolder = empty                  │                  │
│    │                  └──────────────────────┬────────────────────────────┘                  │
│    │                                         │                                               │
│    │                  ┌──────────────────────▼────────────────────────────┐                  │
│    │                  │  UsernamePasswordAuthenticationFilter             │                  │
│    │                  │  → Matches POST /login → intercepts!             │                  │
│    │                  │  → Extracts: username="john", password="123"     │                  │
│    │                  │  → Creates: UnauthToken("john", "123")           │                  │
│    │                  │  → Calls: authManager.authenticate(token)        │                  │
│    │                  │     │                                             │                  │
│    │                  │     ▼                                             │                  │
│    │                  │  ProviderManager                                  │                  │
│    │                  │     │                                             │                  │
│    │                  │     ▼                                             │                  │
│    │                  │  DaoAuthenticationProvider                        │                  │
│    │                  │     ├── loadUserByUsername("john") → UserDetails  │                  │
│    │                  │     ├── Pre-checks (locked? disabled?)            │                  │
│    │                  │     ├── passwordEncoder.matches("123","$2a$..") ✓│                  │
│    │                  │     ├── Post-checks (credentials expired?)       │                  │
│    │                  │     ├── Erase credentials                        │                  │
│    │                  │     └── Return: AuthenticatedToken               │                  │
│    │                  │         { john, null, [ROLE_USER], auth=true }   │                  │
│    │                  │                                                   │                  │
│    │                  │  successfulAuthentication():                      │                  │
│    │                  │  1. SecurityContext ctx = createEmptyContext()    │                  │
│    │                  │  2. ctx.setAuthentication(authenticatedToken)     │                  │
│    │                  │  3. SecurityContextHolder.setContext(ctx)         │                  │
│    │                  │  4. securityContextRepository.saveContext(ctx)    │                  │
│    │                  │     ★ → HttpSession CREATED here!               │                  │
│    │                  │     ★ → session.setAttribute(                    │                  │
│    │                  │          "SPRING_SECURITY_CONTEXT", ctx)         │                  │
│    │                  │  5. successHandler.onAuthenticationSuccess()     │                  │
│    │                  │     → redirect to /dashboard (or saved URL)      │                  │
│    │                  └──────────────────────────────────────────────────┘                  │
│    │                                      │                                                  │
│    │  302 Found                           │                                                  │
│    │  Location: /dashboard                │                                                  │
│    │  Set-Cookie: JSESSIONID=ABC123       │                                                  │
│    │<─────────────────────────────────────│                                                  │
│    │                                      │                                                  │
│    │  ★ Browser stores JSESSIONID cookie  │                                                  │
│    │                                      │                                                  │
│    │                                      │                                                  │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PHASE 2: SUBSEQUENT REQUEST (GET /dashboard) — With JSESSIONID cookie                     │
│  ════════════════════════════════════════════════════════════════════════════════             │
│    │                                      │                                                  │
│    │  GET /dashboard                      │                                                  │
│    │  Cookie: JSESSIONID=ABC123           │                                                  │
│    │─────────────────────────────────────>│                                                  │
│    │                                      │                                                  │
│    │                  ┌───────────────────────────────────────────────────┐                  │
│    │                  │  SecurityContextHolderFilter                      │                  │
│    │                  │  → JSESSIONID=ABC123 → lookup HttpSession        │                  │
│    │                  │  → session found → getAttribute(                 │                  │
│    │                  │       "SPRING_SECURITY_CONTEXT")                  │                  │
│    │                  │  → SecurityContext { john, ROLE_USER, auth=true } │                  │
│    │                  │  → SecurityContextHolder.setContext(ctx)          │                  │
│    │                  │  ★ User is now authenticated for this request!   │                  │
│    │                  └──────────────────────┬────────────────────────────┘                  │
│    │                                         │                                               │
│    │                  ┌──────────────────────▼────────────────────────────┐                  │
│    │                  │  UsernamePasswordAuthenticationFilter             │                  │
│    │                  │  → Request is GET /dashboard (not POST /login)   │                  │
│    │                  │  → Does NOT match → SKIPS this filter            │                  │
│    │                  └──────────────────────┬────────────────────────────┘                  │
│    │                                         │                                               │
│    │                  ┌──────────────────────▼────────────────────────────┐                  │
│    │                  │  AuthorizationFilter                              │                  │
│    │                  │  → auth = SecurityContextHolder.getContext()      │                  │
│    │                  │            .getAuthentication()                   │                  │
│    │                  │  → auth = { john, ROLE_USER, authenticated=true } │                  │
│    │                  │  → Check: does john have access to /dashboard?   │                  │
│    │                  │  → YES ✓ → continue                              │                  │
│    │                  └──────────────────────┬────────────────────────────┘                  │
│    │                                         │                                               │
│    │                  ┌──────────────────────▼────────────────────────────┐                  │
│    │                  │  DispatcherServlet → Controller                    │                  │
│    │                  │  @GetMapping("/dashboard")                         │                  │
│    │                  │  public String dashboard(@AuthenticationPrincipal  │                  │
│    │                  │      UserDetails user) {                           │                  │
│    │                  │      // user.getUsername() → "john"                │                  │
│    │                  │      return "dashboard";                           │                  │
│    │                  │  }                                                 │                  │
│    │                  └──────────────────────────────────────────────────┘                  │
│    │                                      │                                                  │
│    │  200 OK                              │                                                  │
│    │  (dashboard HTML page)               │                                                  │
│    │<─────────────────────────────────────│                                                  │
│    │                                      │                                                  │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PHASE 3: REQUEST WITH INVALID/EXPIRED SESSION                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│    │                                      │                                                  │
│    │  GET /dashboard                      │                                                  │
│    │  Cookie: JSESSIONID=EXPIRED123       │                                                  │
│    │─────────────────────────────────────>│                                                  │
│    │                                      │                                                  │
│    │                  ┌───────────────────────────────────────────────────┐                  │
│    │                  │  SecurityContextHolderFilter                      │                  │
│    │                  │  → JSESSIONID=EXPIRED123 → lookup HttpSession    │                  │
│    │                  │  → session NOT found (expired/invalid)           │                  │
│    │                  │  → SecurityContext = empty (no authentication)   │                  │
│    │                  └──────────────────────┬────────────────────────────┘                  │
│    │                                         │                                               │
│    │                  ┌──────────────────────▼────────────────────────────┐                  │
│    │                  │  AuthorizationFilter                              │                  │
│    │                  │  → auth = null (or AnonymousAuthToken)           │                  │
│    │                  │  → /dashboard requires authenticated user        │                  │
│    │                  │  → DENIED → throw AccessDeniedException          │                  │
│    │                  └──────────────────────┬────────────────────────────┘                  │
│    │                                         │                                               │
│    │                  ┌──────────────────────▼────────────────────────────┐                  │
│    │                  │  ExceptionTranslationFilter                       │                  │
│    │                  │  → Catches AccessDeniedException                  │                  │
│    │                  │  → User is anonymous → authentication entry point │                  │
│    │                  │  → Save original URL (/dashboard) to RequestCache│                  │
│    │                  │  → Redirect to /login                             │                  │
│    │                  └──────────────────────────────────────────────────┘                  │
│    │                                      │                                                  │
│    │  302 Found                           │                                                  │
│    │  Location: /login                    │                                                  │
│    │<─────────────────────────────────────│                                                  │
│    │                                      │                                                  │
│    │  ★ After login, user is redirected   │                                                  │
│    │    back to /dashboard (saved URL)    │                                                  │
│    │                                      │                                                  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.9 Post-Authentication Redirect — Where Does the User Go After Login?

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  POST-AUTHENTICATION REDIRECT — SavedRequestAwareAuthenticationSuccessHandler                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  After successful authentication, UsernamePasswordAuthenticationFilter calls:               │
│  successHandler.onAuthenticationSuccess(request, response, authentication)                  │
│                                                                                              │
│  Default handler: SavedRequestAwareAuthenticationSuccessHandler                              │
│                                                                                              │
│                                                                                              │
│  ── SCENARIO 1: User directly visits /login ─────────────────────────────────               │
│                                                                                              │
│  1. User navigates to /login directly (bookmarked or typed in browser)                     │
│  2. Submits credentials → authentication succeeds                                          │
│  3. SavedRequestAwareAuthenticationSuccessHandler:                                          │
│     → Checks RequestCache for a saved request → NONE (user came directly to /login)        │
│     → Falls back to defaultTargetUrl                                                        │
│     → defaultTargetUrl = "/" (root) by default                                              │
│     → Redirects to "/"                                                                      │
│                                                                                              │
│  Browser: POST /login → 302 → GET / (home page)                                            │
│                                                                                              │
│                                                                                              │
│  ── SCENARIO 2: User was redirected to /login from a protected resource ─────               │
│                                                                                              │
│  1. User tries GET /api/orders → denied (not authenticated)                                │
│  2. ExceptionTranslationFilter SAVES the original request:                                  │
│     requestCache.saveRequest(request);  // saves "/api/orders" with all params             │
│  3. Redirects to /login                                                                     │
│  4. User logs in → authentication succeeds                                                  │
│  5. SavedRequestAwareAuthenticationSuccessHandler:                                          │
│     → Checks RequestCache → FOUND saved request: "/api/orders"                             │
│     → Redirects to the SAVED request URL: /api/orders                                      │
│     → User lands on the page they originally wanted!                                       │
│                                                                                              │
│  Browser: GET /api/orders → 302 /login → POST /login → 302 → GET /api/orders              │
│                                                                                              │
│                                                                                              │
│  ── CONFIGURING THE DEFAULT TARGET URL ──────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐                   │
│  │  http.formLogin(form -> form                                         │                   │
│  │      .defaultSuccessUrl("/dashboard")                                │                   │
│  │      // alwaysUseDefaultTargetUrl defaults to FALSE                  │                   │
│  │      // meaning: if saved request exists, use it; else /dashboard   │                   │
│  │  );                                                                  │                   │
│  │                                                                      │                   │
│  │  // OR: always redirect to /dashboard regardless of saved request:  │                   │
│  │  http.formLogin(form -> form                                         │                   │
│  │      .defaultSuccessUrl("/dashboard", true)                          │                   │
│  │      // alwaysUseDefaultTargetUrl = TRUE                             │                   │
│  │      // ALWAYS goes to /dashboard, even if user originally           │                   │
│  │      // tried to access /api/orders                                  │                   │
│  │  );                                                                  │                   │
│  └──────────────────────────────────────────────────────────────────────┘                   │
│                                                                                              │
│                                                                                              │
│  ── DECISION FLOW ───────────────────────────────────────────────────────                   │
│                                                                                              │
│  onAuthenticationSuccess() called                                                           │
│       │                                                                                      │
│       ▼                                                                                      │
│  alwaysUseDefaultTargetUrl == true?                                                         │
│       │                                                                                      │
│    YES ▼                    NO ▼                                                             │
│  Redirect to              Check RequestCache                                                │
│  defaultTargetUrl              │                                                             │
│  ("/dashboard")          Saved request exists?                                              │
│                                │                                                             │
│                          YES ▼           NO ▼                                                │
│                     Redirect to       Redirect to                                            │
│                     saved URL         defaultTargetUrl                                       │
│                     ("/api/orders")   ("/" or "/dashboard")                                  │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.10 SecurityContextHolder — ThreadLocal Lifecycle

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SecurityContextHolder — ThreadLocal LIFECYCLE                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  SecurityContextHolder stores the SecurityContext in a ThreadLocal variable.                 │
│  This means the SecurityContext is available ONLY within the SAME thread                    │
│  that handles the HTTP request.                                                             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  THREAD LIFECYCLE OF SecurityContext                                         │           │
│  │                                                                               │           │
│  │  HTTP Request arrives                                                        │           │
│  │       │                                                                       │           │
│  │       ▼                                                                       │           │
│  │  Tomcat assigns Thread-1 from thread pool                                    │           │
│  │       │                                                                       │           │
│  │       ▼                                                                       │           │
│  │  SecurityContextHolderFilter:                                                │           │
│  │  → Loads SecurityContext from HttpSession                                    │           │
│  │  → ThreadLocal<SecurityContext> for Thread-1 = ctx { john, ROLE_USER }       │           │
│  │       │                                                                       │           │
│  │       ▼                                                                       │           │
│  │  All filters + Controller execute on Thread-1                                │           │
│  │  → SecurityContextHolder.getContext() works everywhere                       │           │
│  │  → @AuthenticationPrincipal works in controllers                             │           │
│  │  → @PreAuthorize("hasRole('USER')") works in services                        │           │
│  │       │                                                                       │           │
│  │       ▼                                                                       │           │
│  │  Response sent to browser                                                    │           │
│  │       │                                                                       │           │
│  │       ▼                                                                       │           │
│  │  SecurityContextHolderFilter (finally block):                                │           │
│  │  → SecurityContextHolder.clearContext()                                      │           │
│  │  → ThreadLocal<SecurityContext> for Thread-1 = null                          │           │
│  │  → ★ CRITICAL: Context is CLEARED after every request!                      │           │
│  │  → Thread-1 is returned to Tomcat's thread pool                             │           │
│  │  → Next request using Thread-1 starts with a CLEAN ThreadLocal              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── THREE STORAGE STRATEGIES ────────────────────────────────────────────────               │
│                                                                                              │
│  SecurityContextHolder supports three strategies (set via system property                   │
│  or SecurityContextHolder.setStrategyName()):                                                │
│                                                                                              │
│  ┌────────────────────────────┬──────────────────────────────────────────────┐              │
│  │  Strategy                   │  Description                                 │              │
│  ├────────────────────────────┼──────────────────────────────────────────────┤              │
│  │  MODE_THREADLOCAL           │  DEFAULT. Context in ThreadLocal.            │              │
│  │  (default)                  │  Each thread has its own isolated copy.      │              │
│  │                             │  Suitable for servlet-based apps.            │              │
│  ├────────────────────────────┼──────────────────────────────────────────────┤              │
│  │  MODE_INHERITABLETHREADLOCAL│  Context is inherited by child threads.     │              │
│  │                             │  Use when spawning threads that need auth.   │              │
│  │                             │  e.g., @Async methods (with config)          │              │
│  ├────────────────────────────┼──────────────────────────────────────────────┤              │
│  │  MODE_GLOBAL               │  Single global context for ALL threads.      │              │
│  │                             │  Rarely used. Only for standalone/desktop.   │              │
│  │                             │  NOT suitable for web apps!                  │              │
│  └────────────────────────────┴──────────────────────────────────────────────┘              │
│                                                                                              │
│                                                                                              │
│  ── ACCESSING SecurityContext IN CODE ───────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  // Option 1: Direct access via SecurityContextHolder                       │           │
│  │  Authentication auth = SecurityContextHolder.getContext().getAuthentication();│           │
│  │  String username = auth.getName();                                           │           │
│  │                                                                               │           │
│  │  // Option 2: In Controller — @AuthenticationPrincipal                       │           │
│  │  @GetMapping("/profile")                                                      │           │
│  │  public String profile(@AuthenticationPrincipal UserDetails user) {          │           │
│  │      String username = user.getUsername();                                    │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  // Option 3: In Controller — Principal parameter                            │           │
│  │  @GetMapping("/profile")                                                      │           │
│  │  public String profile(Principal principal) {                                │           │
│  │      String username = principal.getName();                                   │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  // Option 4: In Controller — Authentication parameter                       │           │
│  │  @GetMapping("/profile")                                                      │           │
│  │  public String profile(Authentication authentication) {                      │           │
│  │      UserDetails user = (UserDetails) authentication.getPrincipal();         │           │
│  │      Collection<? extends GrantedAuthority> roles = authentication            │           │
│  │          .getAuthorities();                                                   │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  // Option 5: In Service layer — @PreAuthorize / @Secured                   │           │
│  │  @PreAuthorize("hasRole('ADMIN')")                                            │           │
│  │  public void deleteUser(Long userId) { ... }                                 │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 13.11 Object State Transitions — Complete Token & Context Lifecycle

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  OBJECT STATE TRANSITIONS — Token → Context → Session → Cookie                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── OBJECTS CREATED DURING AUTHENTICATION ──────────────────────────────────                │
│                                                                                              │
│  Stage 1: UsernamePasswordAuthenticationFilter                                              │
│  ┌───────────────────────────────────────────────────────────────────┐                      │
│  │  Creates: UsernamePasswordAuthenticationToken (UNAUTHENTICATED)  │                      │
│  │  {                                                                │                      │
│  │    principal: "john" (String),                                    │                      │
│  │    credentials: "john123" (raw password),                         │                      │
│  │    authorities: [] (empty),                                       │                      │
│  │    authenticated: false,                                          │                      │
│  │    details: WebAuthenticationDetails { remoteAddr, sessionId }    │                      │
│  │  }                                                                │                      │
│  └───────────────────────────────────────────────────────────────────┘                      │
│       │                                                                                      │
│       ▼                                                                                      │
│  Stage 2: DaoAuthenticationProvider                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐                      │
│  │  Creates: UserDetails (from UserDetailsService)                   │                      │
│  │  {                                                                │                      │
│  │    username: "john",                                              │                      │
│  │    password: "$2a$10$..." → then erased to null,                  │                      │
│  │    authorities: [SimpleGrantedAuthority("ROLE_USER")],            │                      │
│  │    accountNonExpired: true,                                       │                      │
│  │    accountNonLocked: true,                                        │                      │
│  │    credentialsNonExpired: true,                                   │                      │
│  │    enabled: true                                                  │                      │
│  │  }                                                                │                      │
│  │                                                                   │                      │
│  │  Creates: UsernamePasswordAuthenticationToken (AUTHENTICATED)     │                      │
│  │  {                                                                │                      │
│  │    principal: UserDetails { john, null, [ROLE_USER] },            │                      │
│  │    credentials: null (erased),                                    │                      │
│  │    authorities: [ROLE_USER],                                      │                      │
│  │    authenticated: true ★,                                         │                      │
│  │    details: WebAuthenticationDetails (preserved from Stage 1)     │                      │
│  │  }                                                                │                      │
│  └───────────────────────────────────────────────────────────────────┘                      │
│       │                                                                                      │
│       ▼                                                                                      │
│  Stage 3: successfulAuthentication() in UsernamePasswordAuthenticationFilter                │
│  ┌───────────────────────────────────────────────────────────────────┐                      │
│  │  Creates: SecurityContext                                         │                      │
│  │  {                                                                │                      │
│  │    authentication: UsernamePasswordAuthenticationToken {           │                      │
│  │      principal: UserDetails{john}, auth: true, [ROLE_USER]        │                      │
│  │    }                                                              │                      │
│  │  }                                                                │                      │
│  │                                                                   │                      │
│  │  Sets: SecurityContextHolder.setContext(ctx)                      │                      │
│  │  → ThreadLocal now holds the SecurityContext                      │                      │
│  └───────────────────────────────────────────────────────────────────┘                      │
│       │                                                                                      │
│       ▼                                                                                      │
│  Stage 4: HttpSessionSecurityContextRepository.saveContext()                                │
│  ┌───────────────────────────────────────────────────────────────────┐                      │
│  │  Creates: HttpSession (by Tomcat)                                 │                      │
│  │  {                                                                │                      │
│  │    id: "ABC123-DEF456-GHI789" (JSESSIONID),                       │                      │
│  │    creationTime: 2026-05-09T10:00:00Z,                            │                      │
│  │    maxInactiveInterval: 1800 (30 min default),                    │                      │
│  │    attributes: {                                                  │                      │
│  │      "SPRING_SECURITY_CONTEXT" → SecurityContext {                │                      │
│  │        authentication: UsernamePasswordAuthenticationToken { ... }│                      │
│  │      }                                                            │                      │
│  │    }                                                              │                      │
│  │  }                                                                │                      │
│  └───────────────────────────────────────────────────────────────────┘                      │
│       │                                                                                      │
│       ▼                                                                                      │
│  Stage 5: Response to Browser                                                               │
│  ┌───────────────────────────────────────────────────────────────────┐                      │
│  │  HTTP Response:                                                   │                      │
│  │  Status: 302 Found                                                │                      │
│  │  Location: /dashboard                                             │                      │
│  │  Set-Cookie: JSESSIONID=ABC123-DEF456-GHI789; Path=/; HttpOnly   │                      │
│  │                                                                   │                      │
│  │  ★ The JSESSIONID cookie is the ONLY thing stored in the browser │                      │
│  │  ★ The actual SecurityContext is stored SERVER-SIDE in HttpSession│                      │
│  │  ★ The cookie is just a KEY to look up the session                │                      │
│  └───────────────────────────────────────────────────────────────────┘                      │
│                                                                                              │
│                                                                                              │
│  ── SUMMARY OF OBJECTS ──────────────────────────────────────────────────                   │
│                                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │  Object                              │  Where it lives                      │             │
│  ├──────────────────────────────────────┼──────────────────────────────────────┤             │
│  │  JSESSIONID cookie                   │  Browser (client-side)               │             │
│  │  HttpSession                         │  Tomcat JVM heap (server-side)       │             │
│  │  SecurityContext                     │  Inside HttpSession (as attribute)   │             │
│  │  Authentication (token)              │  Inside SecurityContext              │             │
│  │  UserDetails (principal)             │  Inside Authentication token        │             │
│  │  GrantedAuthority (roles)            │  Inside Authentication token        │             │
│  │  SecurityContextHolder (ThreadLocal) │  Per-thread, per-request lifecycle  │             │
│  └──────────────────────────────────────┴──────────────────────────────────────┘             │
│                                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────────┐             │
│  │                                                                            │             │
│  │  Cookie: JSESSIONID=ABC123                                                │             │
│  │       │                                                                    │             │
│  │       └──▶ HttpSession { id: ABC123 }                                     │             │
│  │               │                                                            │             │
│  │               └──▶ attribute: "SPRING_SECURITY_CONTEXT"                   │             │
│  │                       │                                                    │             │
│  │                       └──▶ SecurityContext                                │             │
│  │                               │                                            │             │
│  │                               └──▶ Authentication                         │             │
│  │                                       │          │                         │             │
│  │                                       │          └──▶ authorities          │             │
│  │                                       │               [ROLE_USER]          │             │
│  │                                       │                                    │             │
│  │                                       └──▶ principal (UserDetails)        │             │
│  │                                            { username: "john" }           │             │
│  │                                                                            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

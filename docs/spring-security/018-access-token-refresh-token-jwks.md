### 18. Access Token, ID Token, Refresh Token, JWKS, Token Blacklisting & The Statelessness Question

---

#### 18.1 The Three Token Types — Access Token, ID Token & Refresh Token

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  THE THREE TOKEN TYPES IN MODERN AUTHENTICATION                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  In OAuth 2.0 and OpenID Connect (OIDC), there are THREE types of tokens,                   │
│  each with a DIFFERENT purpose:                                                              │
│                                                                                              │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐                 │           │
│  │  │  ACCESS TOKEN   │  │  ID TOKEN       │  │  REFRESH TOKEN │                 │           │
│  │  │                │  │                │  │                │                 │           │
│  │  │  "What can I   │  │  "Who am I?"   │  │  "Get me a    │                 │           │
│  │  │   do?"         │  │                │  │   new access  │                 │           │
│  │  │                │  │  Identity      │  │   token"      │                 │           │
│  │  │  Authorization │  │  proof         │  │                │                 │           │
│  │  │                │  │                │  │  Token renewal │                 │           │
│  │  └────────────────┘  └────────────────┘  └────────────────┘                 │           │
│  │  Sent to API         Used by Client      Sent to Auth                       │           │
│  │  servers              App only            Server only                        │           │
│  │  15-30 min            5-60 min            7-30 days                          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18.2 ★ Access Token — Deep Dive

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ACCESS TOKEN — "What Can I Do?"                                                           │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  PURPOSE:                                                                     │           │
│  │  The Access Token is used to ACCESS protected resources (APIs).              │           │
│  │  It is sent to the RESOURCE SERVER (API) in every request.                  │           │
│  │  It answers: "Is this user AUTHORIZED to access this endpoint?"             │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── HOW IT'S USED ──────────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  Client ──── GET /api/orders ─────────────→ API Server                      │           │
│  │              Authorization: Bearer eyJhbG...  (Access Token)                │           │
│  │                                                                               │           │
│  │  API Server:                                                                  │           │
│  │  1. Verify signature ✅                                                      │           │
│  │  2. Check expiration ✅                                                      │           │
│  │  3. Extract roles: ["ROLE_ADMIN"]                                            │           │
│  │  4. AuthorizationFilter checks access rules ✅                              │           │
│  │  5. Return data                                                               │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── TYPICAL PAYLOAD ───────────────────────────────────────────             │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "iss": "https://auth.myapp.com",                                          │           │
│  │    "sub": "user-uuid-12345",          ← subject (user ID)                   │           │
│  │    "aud": "https://api.myapp.com",    ← audience (API server)               │           │
│  │    "iat": 1715184000,                  ← issued at                           │           │
│  │    "exp": 1715185800,                  ← expires in 30 minutes              │           │
│  │    "scope": "read write",              ← OAuth2 scopes                      │           │
│  │    "roles": ["ROLE_ADMIN"],            ← user roles                          │           │
│  │    "client_id": "frontend-app"         ← which app requested it             │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── KEY CHARACTERISTICS ────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  • SHORT-LIVED: 15-30 minutes (limits damage if stolen)                     │           │
│  │  • Sent to: RESOURCE SERVERS (API endpoints)                                │           │
│  │  • Contains: roles, scopes, permissions (for authorization)                 │           │
│  │  • Format: Usually a JWT (can be opaque in some systems)                    │           │
│  │  • Sent in: Authorization: Bearer <token> header                            │           │
│  │  • Audience: The API server(s) that will validate it                        │           │
│  │  • Intended consumer: The API SERVER, not the client app                    │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── ADVANTAGES ─────────────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  ✅ Stateless — API server validates without DB lookup                       │           │
│  │  ✅ Self-contained — carries roles/permissions for authorization             │           │
│  │  ✅ Short-lived — limits window of attack if compromised                    │           │
│  │  ✅ Cross-service — passed between microservices for propagated auth        │           │
│  │  ✅ Scalable — any API server can validate with the signing key             │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── DISADVANTAGES ──────────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  ❌ Cannot be revoked before expiry (unless blacklisted)                     │           │
│  │  ❌ If stolen, attacker can use it until it expires                          │           │
│  │  ❌ Stale permissions — if roles change, old token keeps old roles          │           │
│  │  ❌ Larger than session cookies (500-2000 bytes per request)                │           │
│  │  ❌ Payload is readable (Base64 encoded, not encrypted)                     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18.3 ★ ID Token — Deep Dive

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ID TOKEN — "Who Am I?"                                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  PURPOSE:                                                                     │           │
│  │  The ID Token is defined by OpenID Connect (OIDC). It proves that           │           │
│  │  the user has been AUTHENTICATED and provides identity information.          │           │
│  │  It answers: "WHO is this user? WHEN did they log in?"                      │           │
│  │                                                                               │           │
│  │  ⚠️ CRITICAL DISTINCTION:                                                   │           │
│  │  • Access Token → sent to API SERVERS (for authorization)                   │           │
│  │  • ID Token → consumed by the CLIENT APPLICATION only (for identity)       │           │
│  │  • ID Token should NEVER be sent to API servers as authorization!           │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── HOW IT'S USED ──────────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  1. User logs in via OAuth2/OIDC (Google, Keycloak, Auth0, Okta)            │           │
│  │  2. Auth Server returns: { access_token, id_token, refresh_token }          │           │
│  │  3. Client app reads the ID Token to:                                        │           │
│  │     • Display user's name, email, profile picture on the UI                 │           │
│  │     • Know WHO the user is                                                   │           │
│  │     • Know WHEN the user authenticated (auth_time)                          │           │
│  │  4. Client DOES NOT send ID Token to API servers                            │           │
│  │     → Use the Access Token for API calls instead!                           │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── TYPICAL PAYLOAD (OIDC Standard Claims) ────────────────────            │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "iss": "https://accounts.google.com",  ← issuer (auth provider)         │           │
│  │    "sub": "110169484474386276334",        ← unique user ID at provider     │           │
│  │    "aud": "my-client-app-id",             ← audience (YOUR app's client ID)│           │
│  │    "iat": 1715184000,                      ← issued at                      │           │
│  │    "exp": 1715187600,                      ← expires in 1 hour             │           │
│  │    "auth_time": 1715183900,                ← when user actually logged in  │           │
│  │    "nonce": "abc123xyz",                   ← replay protection             │           │
│  │    "name": "John Doe",                     ← user's display name           │           │
│  │    "email": "john@example.com",            ← user's email                  │           │
│  │    "email_verified": true,                 ← is email verified?            │           │
│  │    "picture": "https://lh3.goo.../photo.jpg", ← profile picture URL       │           │
│  │    "given_name": "John",                   ← first name                    │           │
│  │    "family_name": "Doe",                   ← last name                     │           │
│  │    "locale": "en"                          ← language preference           │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ Notice: NO "roles" or "scope" — ID Token is for IDENTITY, not access   │           │
│  │  ★ "aud" is the CLIENT APP, not the API server                              │           │
│  │  ★ "nonce" prevents replay attacks (client generates, server echoes)       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── KEY CHARACTERISTICS ────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  • SHORT-LIVED: 5-60 minutes                                                │           │
│  │  • Consumed by: CLIENT APPLICATION ONLY (React, Angular, mobile app)       │           │
│  │  • Contains: user identity info (name, email, picture)                      │           │
│  │  • Format: Always a JWT (mandated by OIDC spec)                             │           │
│  │  • Audience: The CLIENT APP (not the API server!)                           │           │
│  │  • Defined by: OpenID Connect (OIDC) spec — extension of OAuth 2.0        │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── ADVANTAGES ─────────────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  ✅ Provides user identity without extra API calls to auth server            │           │
│  │  ✅ Standard format (OIDC) — works with Google, Keycloak, Okta, Auth0      │           │
│  │  ✅ Self-contained — user info embedded in token                             │           │
│  │  ✅ Verifiable — signature proves it came from the auth server              │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── DISADVANTAGES ──────────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  ❌ Should NOT be used for API authorization (common mistake!)               │           │
│  │  ❌ Contains PII (name, email) — privacy concern if leaked                  │           │
│  │  ❌ Audience is the client app — API servers should reject it               │           │
│  │  ❌ Stale data — if user changes name/email, old token has old data        │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18.4 ★ Refresh Token — Deep Dive

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ REFRESH TOKEN — "Get Me a New Access Token"                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  PURPOSE:                                                                     │           │
│  │  The Refresh Token is used to obtain a NEW Access Token when the             │           │
│  │  current one expires — WITHOUT requiring the user to log in again.          │           │
│  │  It answers: "The access token expired, give me a new one."                 │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── WHY REFRESH TOKENS EXIST ───────────────────────────────────            │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  WITHOUT Refresh Token:                                         │       │           │
│  │  │                                                                  │       │           │
│  │  │  Access Token expires (30 min) → User must log in AGAIN        │       │           │
│  │  │  → Enter username + password AGAIN                             │       │           │
│  │  │  → Terrible user experience!                                    │       │           │
│  │  │  → Users logging in every 30 minutes = unacceptable            │       │           │
│  │  │                                                                  │       │           │
│  │  │  Alternative: make access token long-lived (30 days)           │       │           │
│  │  │  → If stolen, attacker has 30 DAYS of access!                  │       │           │
│  │  │  → Terrible security!                                           │       │           │
│  │  │                                                                  │       │           │
│  │  │  WITH Refresh Token:                                            │       │           │
│  │  │                                                                  │       │           │
│  │  │  Access Token: 30 min (short = secure)                         │       │           │
│  │  │  Refresh Token: 7-30 days (long = convenient)                  │       │           │
│  │  │  → Access Token expires → client uses Refresh Token to get    │       │           │
│  │  │    a NEW Access Token → user doesn't notice!                   │       │           │
│  │  │  → BEST OF BOTH: security + convenience                       │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── HOW THE REFRESH FLOW WORKS ─────────────────────────────────            │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  Step 1: Login                                                  │       │           │
│  │  │  POST /auth/login → { accessToken (30min), refreshToken (7d) } │       │           │
│  │  │                                                                  │       │           │
│  │  │  Step 2: Use Access Token for 30 minutes                       │       │           │
│  │  │  GET /api/data + Authorization: Bearer <accessToken>           │       │           │
│  │  │  → Works ✅                                                     │       │           │
│  │  │                                                                  │       │           │
│  │  │  Step 3: Access Token expires (30 min later)                   │       │           │
│  │  │  GET /api/data + Authorization: Bearer <expired accessToken>   │       │           │
│  │  │  → 401 Unauthorized ❌ (token expired)                         │       │           │
│  │  │                                                                  │       │           │
│  │  │  Step 4: Client uses Refresh Token to get NEW Access Token     │       │           │
│  │  │  POST /auth/refresh                                            │       │           │
│  │  │  { "refreshToken": "eyJhbG..." }                               │       │           │
│  │  │  → Server validates refresh token                              │       │           │
│  │  │  → Returns NEW access token + (optionally) NEW refresh token  │       │           │
│  │  │  → { accessToken (30min), refreshToken (7d) }                  │       │           │
│  │  │                                                                  │       │           │
│  │  │  Step 5: Use NEW Access Token                                  │       │           │
│  │  │  GET /api/data + Authorization: Bearer <newAccessToken>        │       │           │
│  │  │  → Works ✅                                                     │       │           │
│  │  │                                                                  │       │           │
│  │  │  ★ User never had to log in again!                             │       │           │
│  │  │  ★ Seamless token renewal in the background                    │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── TYPICAL PAYLOAD ───────────────────────────────────────────             │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "iss": "https://auth.myapp.com",                                          │           │
│  │    "sub": "user-uuid-12345",          ← user ID                              │           │
│  │    "iat": 1715184000,                  ← issued at                           │           │
│  │    "exp": 1715788800,                  ← expires in 7 days                  │           │
│  │    "jti": "refresh-uuid-67890",        ← unique ID for this refresh token  │           │
│  │    "type": "refresh"                   ← token type identifier              │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ MINIMAL claims — no roles, no scopes!                                   │           │
│  │  ★ Only needs enough info to identify the user and issue a new access token│           │
│  │  ★ Some implementations use OPAQUE tokens (not JWT) for refresh tokens     │           │
│  │    and store them server-side (in DB/Redis) for revocation                  │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── KEY CHARACTERISTICS ────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  • LONG-LIVED: 7-30 days                                                    │           │
│  │  • Sent to: AUTHORIZATION SERVER ONLY (POST /auth/refresh)                 │           │
│  │  • NEVER sent to API servers!                                               │           │
│  │  • Contains: minimal claims (sub, exp, jti)                                 │           │
│  │  • Format: JWT or opaque string (implementation varies)                     │           │
│  │  • Storage: httpOnly secure cookie (browser) or secure storage (mobile)    │           │
│  │  • Often stored server-side (DB/Redis) for revocation capability           │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── ADVANTAGES ─────────────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  ✅ Enables short-lived access tokens (security) with long sessions (UX)    │           │
│  │  ✅ User doesn't need to log in again when access token expires             │           │
│  │  ✅ Can be revoked server-side (stored in DB/Redis → delete to revoke)     │           │
│  │  ✅ Refresh token rotation detects token theft                               │           │
│  │  ✅ Only sent to auth server (smaller attack surface)                        │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── DISADVANTAGES ──────────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  ❌ If stolen, attacker can get new access tokens for days/weeks             │           │
│  │  ❌ Requires server-side storage (DB/Redis) for revocation → adds state    │           │
│  │  ❌ Adds complexity (token refresh flow, rotation logic)                     │           │
│  │  ❌ Must be stored very securely on client (httpOnly cookie, Keychain)      │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── REFRESH TOKEN ROTATION (Security Best Practice) ────────────            │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  1. Client sends refresh token RT1 to get new access token      │       │           │
│  │  │  2. Server issues NEW access token + NEW refresh token RT2     │       │           │
│  │  │  3. Server INVALIDATES RT1 (one-time use)                      │       │           │
│  │  │                                                                  │       │           │
│  │  │  If attacker steals RT1 and tries to use it:                   │       │           │
│  │  │  → RT1 already used → server detects reuse → REVOKE ALL       │       │           │
│  │  │    tokens for this user (including RT2) → force re-login!     │       │           │
│  │  │                                                                  │       │           │
│  │  │  ★ This detects token theft and limits the damage!             │       │           │
│  │  │  ★ Auth0, Keycloak, Okta all support refresh token rotation   │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18.5 Comparison Table — Access Token vs ID Token vs Refresh Token

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  COMPARISON TABLE — ALL THREE TOKEN TYPES                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌─────────────────────┬──────────────────────┬──────────────────────┬──────────────────────┐│
│  │  Aspect              │  Access Token        │  ID Token            │  Refresh Token       ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Purpose             │  API authorization   │  User identity       │  Token renewal       ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Answers             │  "What can I do?"    │  "Who am I?"         │  "Give me a new      ││
│  │                     │                      │                      │   access token"       ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Sent to             │  API/Resource        │  Client app ONLY     │  Auth server ONLY    ││
│  │                     │  server              │  (never to APIs!)    │  (never to APIs!)    ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Contains            │  roles, scopes,      │  name, email,        │  sub, jti            ││
│  │                     │  permissions         │  picture, auth_time  │  (minimal claims)    ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Expiry              │  15-30 minutes       │  5-60 minutes        │  7-30 days           ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Format              │  JWT (usually)       │  JWT (always, per    │  JWT or opaque       ││
│  │                     │                      │  OIDC spec)          │  string              ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Audience (aud)      │  API server(s)       │  Client app          │  Auth server         ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Defined by          │  OAuth 2.0           │  OpenID Connect      │  OAuth 2.0           ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Revocable?          │  ❌ Hard (needs        │  ❌ Hard (usually     │  ✅ Yes (stored       ││
│  │                     │  blacklist/Redis)    │  short enough)       │  server-side)        ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Server storage      │  ❌ None (stateless)   │  ❌ None              │  ✅ DB/Redis          ││
│  │                     │                      │                      │  (for revocation)    ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  In header           │  Authorization:      │  NOT sent in         │  NOT sent in         ││
│  │                     │  Bearer <token>      │  API requests        │  API requests        ││
│  ├─────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤│
│  │  Common mistake      │  Making it too       │  Using it for API    │  Storing in          ││
│  │                     │  long-lived          │  authorization       │  localStorage        ││
│  └─────────────────────┴──────────────────────┴──────────────────────┴──────────────────────┘│
│                                                                                              │
│                                                                                              │
│  ── VISUAL: HOW ALL THREE TOKENS WORK TOGETHER ──────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Client App         Auth Server              API Server                      │           │
│  │  ──────────         ───────────              ──────────                      │           │
│  │      │                   │                        │                           │           │
│  │      │  POST /auth/login │                        │                           │           │
│  │      │  {username, pwd}  │                        │                           │           │
│  │      │──────────────────>│                        │                           │           │
│  │      │                   │ Validate credentials   │                           │           │
│  │      │                   │ (DB + BCrypt)           │                           │           │
│  │      │                   │                        │                           │           │
│  │      │  {                │                        │                           │           │
│  │      │   accessToken,    │                        │                           │           │
│  │      │   idToken,        │                        │                           │           │
│  │      │   refreshToken    │                        │                           │           │
│  │      │  }                │                        │                           │           │
│  │      │<──────────────────│                        │                           │           │
│  │      │                   │                        │                           │           │
│  │      │  Read ID Token    │                        │                           │           │
│  │      │  → Display:       │                        │                           │           │
│  │      │  "Hello, John!"   │                        │                           │           │
│  │      │                   │                        │                           │           │
│  │      │  GET /api/orders  │                        │                           │           │
│  │      │  Authorization: Bearer <accessToken>       │                           │           │
│  │      │────────────────────────────────────────────>│                           │           │
│  │      │                   │                        │ Verify access token       │           │
│  │      │                   │                        │ Check roles/scopes       │           │
│  │      │  200 OK [{data}]  │                        │                           │           │
│  │      │<────────────────────────────────────────────│                           │           │
│  │      │                   │                        │                           │           │
│  │      │  ··· 30 min later ··· access token expired │                           │           │
│  │      │                   │                        │                           │           │
│  │      │  POST /auth/refresh                        │                           │           │
│  │      │  {refreshToken}   │                        │                           │           │
│  │      │──────────────────>│                        │                           │           │
│  │      │                   │ Validate refresh token │                           │           │
│  │      │                   │ Issue NEW tokens       │                           │           │
│  │      │  {                │                        │                           │           │
│  │      │   newAccessToken, │                        │                           │           │
│  │      │   newRefreshToken │  (rotation)            │                           │           │
│  │      │  }                │                        │                           │           │
│  │      │<──────────────────│                        │                           │           │
│  │      │                   │                        │                           │           │
│  │      │  Continue using new access token...        │                           │           │
│  │      │                   │                        │                           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18.6 ★ JWKS (JSON Web Key Set) — What It Is, How and Why to Use It

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ JWKS (JSON WEB KEY SET) — WHAT, HOW & WHY                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── WHAT IS JWKS? ───────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  JWKS (JSON Web Key Set) is a JSON document that contains the                │           │
│  │  PUBLIC KEYS used to verify JWT signatures.                                  │           │
│  │                                                                               │           │
│  │  It is hosted at a well-known URL by the Authorization Server:              │           │
│  │  https://auth.myapp.com/.well-known/jwks.json                               │           │
│  │                                                                               │           │
│  │  ★ Only relevant for ASYMMETRIC algorithms (RS256, ES256, PS256)           │           │
│  │  ★ NOT needed for symmetric algorithms (HS256) — symmetric uses a          │           │
│  │    shared secret, not public/private keys                                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── THE PROBLEM JWKS SOLVES ─────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  WITHOUT JWKS (manual key distribution):                                     │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  Auth Server generates RSA key pair                             │       │           │
│  │  │  → Private key: kept by auth server (signs JWTs)               │       │           │
│  │  │  → Public key: must be manually copied to EVERY API server     │       │           │
│  │  │                                                                  │       │           │
│  │  │  Problems:                                                      │       │           │
│  │  │  ❌ Manual key distribution to 20 microservices = nightmare     │       │           │
│  │  │  ❌ Key rotation requires updating ALL services simultaneously │       │           │
│  │  │  ❌ Downtime during key rotation                               │       │           │
│  │  │  ❌ Error-prone manual process                                  │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  │  WITH JWKS (automatic key discovery):                                        │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  Auth Server hosts JWKS endpoint:                               │       │           │
│  │  │  https://auth.myapp.com/.well-known/jwks.json                  │       │           │
│  │  │                                                                  │       │           │
│  │  │  API servers automatically FETCH public keys from this URL:    │       │           │
│  │  │  → On startup: download keys from JWKS endpoint               │       │           │
│  │  │  → Cache keys locally                                          │       │           │
│  │  │  → If token has unknown "kid" → re-fetch JWKS (key rotation!) │       │           │
│  │  │                                                                  │       │           │
│  │  │  Benefits:                                                      │       │           │
│  │  │  ✅ NO manual key distribution needed                           │       │           │
│  │  │  ✅ Key rotation is automatic (add new key, remove old key)    │       │           │
│  │  │  ✅ Zero-downtime key rotation                                 │       │           │
│  │  │  ✅ API servers auto-discover new keys                         │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── JWKS JSON STRUCTURE ─────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  // GET https://auth.myapp.com/.well-known/jwks.json                        │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "keys": [                                                                  │           │
│  │      {                                                                        │           │
│  │        "kty": "RSA",                    ← Key Type (RSA, EC, etc.)          │           │
│  │        "use": "sig",                    ← Usage: signature verification     │           │
│  │        "kid": "my-key-id-2024",         ← Key ID (matches JWT header kid)  │           │
│  │        "alg": "RS256",                  ← Algorithm                          │           │
│  │        "n": "0vx7agoebGcQSuu...",      ← RSA modulus (public key part)    │           │
│  │        "e": "AQAB"                      ← RSA exponent (public key part)   │           │
│  │      },                                                                       │           │
│  │      {                                                                        │           │
│  │        "kty": "RSA",                                                         │           │
│  │        "use": "sig",                                                         │           │
│  │        "kid": "my-key-id-2025",         ← NEW key (after rotation)         │           │
│  │        "alg": "RS256",                                                       │           │
│  │        "n": "1b9aF3n8vK2L...",                                              │           │
│  │        "e": "AQAB"                                                           │           │
│  │      }                                                                        │           │
│  │    ]                                                                          │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ Multiple keys can exist simultaneously (for key rotation!)               │           │
│  │  ★ JWT header "kid" tells which key was used to sign                        │           │
│  │  ★ Only PUBLIC keys — private key never leaves auth server!                │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── HOW JWKS VERIFICATION WORKS ─────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  JWT Header:                                                                  │           │
│  │  { "alg": "RS256", "typ": "JWT", "kid": "my-key-id-2024" }                 │           │
│  │                                       ↑                                      │           │
│  │                                       │                                      │           │
│  │  API Server receives JWT:             │                                      │           │
│  │  1. Read "kid" from header ───────────┘                                     │           │
│  │  2. Look up "kid" in cached JWKS keys                                       │           │
│  │  3. If found → use that public key to verify signature                     │           │
│  │  4. If NOT found → re-fetch JWKS from auth server (key may have rotated)  │           │
│  │  5. If still not found → REJECT token (unknown key)                        │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── KEY ROTATION FLOW ────────────────────────────────────────             │           │
│  │                                                                               │           │
│  │  Day 1: Auth server uses key "kid-2024" to sign tokens                     │           │
│  │  JWKS: [kid-2024]                                                            │           │
│  │                                                                               │           │
│  │  Day 30: Auth server adds new key "kid-2025"                                │           │
│  │  JWKS: [kid-2024, kid-2025]  ← both keys active!                          │           │
│  │  Auth server starts signing NEW tokens with kid-2025                        │           │
│  │  Old tokens signed with kid-2024 still verify ✅                            │           │
│  │                                                                               │           │
│  │  Day 31: Remove old key kid-2024                                             │           │
│  │  JWKS: [kid-2025]  ← only new key                                          │           │
│  │  Old tokens with kid-2024 are now rejected ❌ (expired anyway)             │           │
│  │                                                                               │           │
│  │  ★ Zero-downtime key rotation!                                              │           │
│  │  ★ Overlap period ensures no valid tokens are rejected                     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── SPRING SECURITY + JWKS ──────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Spring Security has built-in support for JWKS via Resource Server:         │           │
│  │                                                                               │           │
│  │  # application.yml                                                           │           │
│  │  spring:                                                                      │           │
│  │    security:                                                                  │           │
│  │      oauth2:                                                                  │           │
│  │        resourceserver:                                                        │           │
│  │          jwt:                                                                 │           │
│  │            jwk-set-uri: https://auth.myapp.com/.well-known/jwks.json        │           │
│  │            # ★ Spring auto-fetches public keys from this URL                │           │
│  │            # ★ Caches keys and re-fetches on unknown "kid"                 │           │
│  │            # ★ Verifies JWT signatures automatically                       │           │
│  │                                                                               │           │
│  │  Or for Keycloak:                                                            │           │
│  │  spring:                                                                      │           │
│  │    security:                                                                  │           │
│  │      oauth2:                                                                  │           │
│  │        resourceserver:                                                        │           │
│  │          jwt:                                                                 │           │
│  │            issuer-uri: https://keycloak.myapp.com/realms/myrealm            │           │
│  │            # ★ Spring discovers JWKS URL from .well-known/openid-config    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHEN TO USE JWKS vs SHARED SECRET ───────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────┬─────────────────────────┬─────────────────────────┐           │
│  │  Aspect                  │  Shared Secret (HS256)  │  JWKS (RS256)           │           │
│  ├──────────────────────────┼─────────────────────────┼─────────────────────────┤           │
│  │  Key distribution        │  Manual to each service │  Automatic via URL      │           │
│  ├──────────────────────────┼─────────────────────────┼─────────────────────────┤           │
│  │  Key rotation            │  ❌ Manual, risky         │  ✅ Zero-downtime        │           │
│  ├──────────────────────────┼─────────────────────────┼─────────────────────────┤           │
│  │  Who can create tokens   │  Anyone with the secret │  Only auth server       │           │
│  │                          │  (all services!)        │  (private key holder)   │           │
│  ├──────────────────────────┼─────────────────────────┼─────────────────────────┤           │
│  │  If key leaks            │  ❌ Attacker can forge    │  ✅ Public key is public │           │
│  │                          │  tokens                 │  (can't forge tokens)   │           │
│  ├──────────────────────────┼─────────────────────────┼─────────────────────────┤           │
│  │  Performance             │  ✅ Faster (HMAC)         │  ⚠️ Slower (RSA verify)  │           │
│  ├──────────────────────────┼─────────────────────────┼─────────────────────────┤           │
│  │  Best for                │  Single service /       │  Microservices /        │           │
│  │                          │  monolith               │  multi-service          │           │
│  └──────────────────────────┴─────────────────────────┴─────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18.7 ★ Is JWT Really Stateless? — The Honest Truth

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ IS JWT REALLY STATELESS? — THE HONEST TRUTH                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── THE SHORT ANSWER ────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  JWT is stateless IN THEORY, but most real-world applications                │           │
│  │  need SOME server-side state to handle these requirements:                   │           │
│  │                                                                               │           │
│  │  1. Token revocation (logout, user banned, password changed)                │           │
│  │  2. Refresh token storage (for rotation and revocation)                     │           │
│  │  3. Token blacklisting (invalidate compromised tokens)                     │           │
│  │                                                                               │           │
│  │  ★ PURE stateless JWT = no logout, no revocation, no blacklisting          │           │
│  │  ★ PRACTICAL JWT = stateless verification + stateful revocation layer      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── THE SPECTRUM OF STATELESSNESS ───────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ← FULLY STATELESS ─────────────────────────── FULLY STATEFUL →             │           │
│  │                                                                               │           │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐                 │           │
│  │  │  PURE JWT       │  │  JWT + BLACKLIST│  │  SESSION-BASED │                 │           │
│  │  │  (No server     │  │  (Redis for     │  │  (Full server  │                 │           │
│  │  │   state at all) │  │   revocation    │  │   state for    │                 │           │
│  │  │                │  │   only)         │  │   every user)  │                 │           │
│  │  └────────────────┘  └────────────────┘  └────────────────┘                 │           │
│  │                                                                               │           │
│  │  ❌ No logout       ✅ Logout works       ✅ Logout works                    │           │
│  │  ❌ No revocation   ✅ Revocation works   ✅ Instant revocation              │           │
│  │  ✅ No DB needed    ⚠️ Redis needed       ❌ Session DB needed               │           │
│  │  ✅ Max scalable    ✅ Very scalable       ⚠️ Scaling overhead               │           │
│  │                                                                               │           │
│  │  ★ Most production apps are in the MIDDLE: JWT + Redis blacklist           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHY PURE STATELESS JWT DOESN'T WORK IN PRODUCTION ───────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  SCENARIO 1: USER LOGS OUT                                                   │           │
│  │  ──────────────────────────                                                  │           │
│  │  Pure stateless: User clicks "logout" → client deletes token               │           │
│  │  BUT: The token is STILL VALID on the server until exp!                    │           │
│  │  → If attacker already copied the token, they can still use it!            │           │
│  │  → "Logout" is just a CLIENT-SIDE illusion, not server-side!              │           │
│  │                                                                               │           │
│  │  SCENARIO 2: USER IS BANNED / SUSPENDED                                     │           │
│  │  ──────────────────────────────────────                                      │           │
│  │  Admin bans user → user's access token is STILL VALID for 30 min!         │           │
│  │  → Banned user can keep making API calls until token expires!             │           │
│  │  → Unacceptable for security-sensitive applications!                       │           │
│  │                                                                               │           │
│  │  SCENARIO 3: PASSWORD CHANGED / ACCOUNT COMPROMISED                         │           │
│  │  ─────────────────────────────────────────────────                           │           │
│  │  User changes password → old tokens are STILL VALID!                       │           │
│  │  → Attacker with old token can still access the account!                  │           │
│  │  → Should invalidate all existing tokens when password changes            │           │
│  │                                                                               │           │
│  │  SCENARIO 4: ROLE/PERMISSION CHANGES                                         │           │
│  │  ───────────────────────────────────                                         │           │
│  │  Admin removes ADMIN role from user → old token STILL has ADMIN role!     │           │
│  │  → User can still perform admin actions until token expires!              │           │
│  │                                                                               │           │
│  │  ★ ALL these scenarios require the ability to INVALIDATE a token           │           │
│  │    before its natural expiration → requires SERVER-SIDE STATE             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── BUT JWT IS STILL "MOSTLY STATELESS" — HERE'S WHY ────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Session-based: EVERY request → look up full session from DB/Redis          │           │
│  │  → Read user, roles, permissions, session metadata EVERY TIME               │           │
│  │  → 100% of requests hit the session store                                   │           │
│  │                                                                               │           │
│  │  JWT + Blacklist: EVERY request → check if token is blacklisted (Redis)    │           │
│  │  → Quick SET membership check: SISMEMBER blacklist <jti>                   │           │
│  │  → Only returns true/false (is this token ID blacklisted?)                 │           │
│  │  → User info (roles, name) still comes from the token itself              │           │
│  │  → ★ MUCH lighter than full session lookup!                                │           │
│  │                                                                               │           │
│  │  The blacklist contains ONLY revoked token IDs — not all active sessions   │           │
│  │  → Blacklist: ~0.1% of all tokens (only revoked ones)                      │           │
│  │  → Session store: 100% of all active users                                 │           │
│  │  → DRAMATICALLY less data in the blacklist vs session store               │           │
│  │                                                                               │           │
│  │  ★ JWT verification is still CPU-only (no I/O for signature check)        │           │
│  │  ★ Blacklist check is a SINGLE Redis lookup (O(1), sub-millisecond)       │           │
│  │  ★ Token itself still carries user info (no session data to retrieve)     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18.8 ★ Where Exactly You Need Redis — Token Blacklisting Architecture

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHERE EXACTLY YOU NEED REDIS — AND HOW TOKEN BLACKLISTING WORKS                          │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── WHERE REDIS IS NEEDED ───────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Redis is needed for TWO purposes in JWT-based authentication:              │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  1. TOKEN BLACKLIST (Access Token Revocation)                               │           │
│  │  ──────────────────────────────────────────                                  │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  When a user logs out or token needs to be revoked:             │       │           │
│  │  │                                                                  │       │           │
│  │  │  Redis Key:   "blacklist:<jti>"                                 │       │           │
│  │  │  Redis Value: "true" (or empty — just the key existing matters) │       │           │
│  │  │  Redis TTL:   remaining time until token's exp                 │       │           │
│  │  │                                                                  │       │           │
│  │  │  Example:                                                       │       │           │
│  │  │  SET blacklist:550e8400-e29b-41d4-a716 "revoked" EX 1200       │       │           │
│  │  │  ↑ key = jti from token                           ↑ TTL = 20min│       │           │
│  │  │                                                                  │       │           │
│  │  │  ★ TTL = token's remaining time to expiry                      │       │           │
│  │  │  ★ Key auto-deletes from Redis when token would have expired   │       │           │
│  │  │  ★ No need to manually clean up! Redis handles it!             │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  2. REFRESH TOKEN STORAGE (For Rotation & Revocation)                       │           │
│  │  ──────────────────────────────────────────────────                          │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  Refresh tokens can be stored in:                               │       │           │
│  │  │  • Redis (fast, auto-expiry with TTL)                          │       │           │
│  │  │  • Database (persistent, survives Redis restart)               │       │           │
│  │  │                                                                  │       │           │
│  │  │  Redis Key:   "refresh_token:<userId>"                         │       │           │
│  │  │  Redis Value: "<refresh-token-jti>"                            │       │           │
│  │  │  Redis TTL:   7 days (refresh token expiry)                    │       │           │
│  │  │                                                                  │       │           │
│  │  │  On logout: DELETE refresh_token:<userId>                      │       │           │
│  │  │  → Refresh token is now invalid                                │       │           │
│  │  │  → User must log in again when access token expires            │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── HOW TOKEN BLACKLISTING WORKS — COMPLETE FLOW ────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── ON LOGOUT ──────────────────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  Client → POST /auth/logout (with access token)                             │           │
│  │                                                                               │           │
│  │  Server:                                                                      │           │
│  │  1. Extract jti (token ID) from the JWT: "550e8400-e29b..."                 │           │
│  │  2. Extract exp from the JWT: 1715185800 (Unix timestamp)                   │           │
│  │  3. Calculate TTL: exp - currentTime = 1200 seconds (20 min remaining)     │           │
│  │  4. Store in Redis:                                                          │           │
│  │     SET "blacklist:550e8400-e29b..." "revoked" EX 1200                      │           │
│  │  5. Delete refresh token from Redis:                                         │           │
│  │     DEL "refresh_token:<userId>"                                             │           │
│  │  6. Return 200 OK                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── ON EVERY API REQUEST ───────────────────────────────────────            │           │
│  │                                                                               │           │
│  │  Client → GET /api/data + Authorization: Bearer <JWT>                       │           │
│  │                                                                               │           │
│  │  JwtAuthFilter:                                                              │           │
│  │  1. Extract token from header ✅                                             │           │
│  │  2. Verify signature (CPU only, no I/O) ✅                                  │           │
│  │  3. Check exp (not expired?) ✅                                              │           │
│  │  4. ★ Check blacklist (Redis lookup):                                       │           │
│  │     EXISTS "blacklist:<jti>"                                                 │           │
│  │     → If EXISTS (true) → 401 Unauthorized (token revoked!)                 │           │
│  │     → If NOT EXISTS (false) → ✅ Token is valid, continue                  │           │
│  │  5. Extract claims (sub, roles)                                              │           │
│  │  6. Set SecurityContext                                                       │           │
│  │  7. Continue to AuthorizationFilter → Controller                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── ARCHITECTURE DIAGRAM ────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │                       ┌─────────────────┐                                    │           │
│  │                       │   REDIS          │                                    │           │
│  │                       │                 │                                    │           │
│  │                       │  blacklist:     │                                    │           │
│  │                       │   jti-abc (TTL) │                                    │           │
│  │                       │   jti-def (TTL) │                                    │           │
│  │                       │                 │                                    │           │
│  │                       │  refresh_token: │                                    │           │
│  │                       │   user1 → rt1   │                                    │           │
│  │                       │   user2 → rt2   │                                    │           │
│  │                       └────────┬────────┘                                    │           │
│  │                                │                                              │           │
│  │                    ┌───────────┼───────────┐                                 │           │
│  │                    │           │           │                                 │           │
│  │              ┌─────▼─────┐ ┌──▼────────┐ ┌▼──────────┐                     │           │
│  │              │ API       │ │ API       │ │ API       │                     │           │
│  │              │ Server 1  │ │ Server 2  │ │ Server 3  │                     │           │
│  │              │           │ │           │ │           │                     │           │
│  │              │ JWT verify│ │ JWT verify│ │ JWT verify│                     │           │
│  │              │ + blacklist│ │ + blacklist│ │ + blacklist│                    │           │
│  │              │   check   │ │   check   │ │   check   │                     │           │
│  │              └───────────┘ └───────────┘ └───────────┘                     │           │
│  │                    ↑           ↑           ↑                                 │           │
│  │                    └─────┬─────┘           │                                 │           │
│  │                          │                 │                                 │           │
│  │                    ┌─────▼─────────────────▼──┐                              │           │
│  │                    │     LOAD BALANCER         │                              │           │
│  │                    └──────────┬────────────────┘                              │           │
│  │                               │                                               │           │
│  │                          ┌────▼────┐                                          │           │
│  │                          │ CLIENT  │                                          │           │
│  │                          └─────────┘                                          │           │
│  │                                                                               │           │
│  │  ★ All API servers connect to the SAME Redis instance                       │           │
│  │  ★ Logout on Server 1 → blacklisted in Redis → Server 2 & 3 also reject  │           │
│  │  ★ Redis is a lightweight shared layer (not full session storage)          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18.9 Token Blacklisting — Complete Code Implementation

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  TokenBlacklistService — Redis-backed token blacklist
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Blacklist a token by storing its jti in Redis with TTL = remaining expiry time.
     * The key auto-deletes from Redis when the token would have expired anyway.
     */
    public void blacklistToken(String jti, Date expiration) {
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti,
                "revoked",
                ttlMillis,
                TimeUnit.MILLISECONDS
            );
        }
        // If ttlMillis <= 0, token is already expired — no need to blacklist
    }

    /**
     * Check if a token has been blacklisted.
     */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(BLACKLIST_PREFIX + jti)
        );
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  JwtAuthenticationFilter — Check blacklist during token validation
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                    TokenBlacklistService blacklistService) {
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 2. Extract jti and check blacklist BEFORE full validation
            String jti = jwtService.extractClaim(token, Claims::getId);
            if (jti != null && blacklistService.isBlacklisted(jti)) {
                // ★ Token has been revoked — reject immediately!
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Token has been revoked\"}");
                return;
            }

            // 3. Extract username and validate token
            String username = jwtService.extractUsername(token);

            if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

                // 4. Extract roles from token (NO DB lookup!)
                List<String> roles = jwtService.extractRoles(token);
                List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

                // 5. Create Authentication token and set SecurityContext
                UsernamePasswordAuthenticationToken authToken =
                    UsernamePasswordAuthenticationToken.authenticated(
                        username, null, authorities
                    );
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token has expired\"}");
            return;
        } catch (JwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AuthController — Login, Refresh & Logout endpoints
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final UserDetailsService userDetailsService;

    // constructor injection...

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest loginRequest) {
        // Authenticate with DaoAuthenticationProvider (DB + BCrypt)
        Authentication auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequest.username(), loginRequest.password()
            )
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return ResponseEntity.ok(Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "expiresIn", 1800  // 30 minutes
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        // Validate the refresh token
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (jwtService.isTokenValid(refreshToken, userDetails)) {
            // Issue new access token (with FRESH roles from DB!)
            String newAccessToken = jwtService.generateAccessToken(userDetails);
            // ★ Roles are fetched fresh from DB during refresh!
            // ★ This solves the "stale roles" problem!

            return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "expiresIn", 1800
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // ★ Blacklist the access token
            String jti = jwtService.extractClaim(token, Claims::getId);
            Date expiration = jwtService.extractExpiration(token);
            blacklistService.blacklistToken(jti, expiration);

            // ★ Also delete the refresh token (stored in Redis/DB)
            // refreshTokenService.deleteByUsername(username);
        }

        return ResponseEntity.ok().build();
    }
}
```

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — Redis configuration for token blacklisting
# ═══════════════════════════════════════════════════════════════════════════════

spring:
  data:
    redis:
      host: localhost
      port: 6379
      # password: ${REDIS_PASSWORD}  # for production

  # Redis dependency in pom.xml:
  # <dependency>
  #     <groupId>org.springframework.boot</groupId>
  #     <artifactId>spring-boot-starter-data-redis</artifactId>
  # </dependency>
```

---

#### 18.10 When to Blacklist — Complete List of Scenarios

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHEN TO BLACKLIST A TOKEN — ALL SCENARIOS                                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  SCENARIO                          │  ACTION                                 │           │
│  │  ─────────────────────────────────│──────────────────────────────────────── │           │
│  │                                    │                                         │           │
│  │  1. User logs out                  │  Blacklist current access token         │           │
│  │                                    │  Delete refresh token from Redis/DB    │           │
│  │                                    │                                         │           │
│  │  2. User changes password          │  Blacklist ALL active access tokens    │           │
│  │                                    │  for this user                          │           │
│  │                                    │  Delete ALL refresh tokens              │           │
│  │                                    │                                         │           │
│  │  3. Admin bans/suspends user       │  Blacklist ALL active access tokens    │           │
│  │                                    │  Delete ALL refresh tokens              │           │
│  │                                    │                                         │           │
│  │  4. User's role/permission changed │  Blacklist current access token        │           │
│  │                                    │  (force refresh → new token gets new  │           │
│  │                                    │   roles from DB)                        │           │
│  │                                    │                                         │           │
│  │  5. Token theft detected           │  Blacklist ALL tokens for this user    │           │
│  │     (refresh token reuse)          │  Force complete re-login               │           │
│  │                                    │                                         │           │
│  │  6. Security incident              │  Rotate signing key                    │           │
│  │     (secret key compromised)       │  ALL old tokens become invalid         │           │
│  │                                    │  (no blacklisting needed — new key    │           │
│  │                                    │   won't verify old signatures)         │           │
│  │                                    │                                         │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── ALTERNATIVE TO BLACKLISTING: SHORT EXPIRY + NO BLACKLIST ────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Some systems choose to NOT implement blacklisting:                          │           │
│  │                                                                               │           │
│  │  Strategy: Very short access tokens (5 minutes) + no blacklist              │           │
│  │  → Maximum exposure window is 5 minutes                                     │           │
│  │  → Acceptable for some low-sensitivity applications                        │           │
│  │  → Truly stateless (no Redis needed)                                        │           │
│  │  → Trade-off: more frequent refresh calls (every 5 min)                    │           │
│  │                                                                               │           │
│  │  ★ Decision depends on your security requirements:                          │           │
│  │    • Banking / healthcare: MUST have blacklisting                           │           │
│  │    • Social media / blog: short expiry might be acceptable                 │           │
│  │    • Internal tools: short expiry usually sufficient                        │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 18.11 Summary — Section 18 Key Takeaways

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SUMMARY — SECTION 18 KEY TAKEAWAYS                                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. THREE TOKEN TYPES                                                        │           │
│  │     • Access Token: "What can I do?" — sent to API servers for             │           │
│  │       authorization, contains roles/scopes, short-lived (15-30 min)        │           │
│  │     • ID Token: "Who am I?" — consumed by client app for identity,         │           │
│  │       contains name/email/picture, NEVER sent to APIs (OIDC spec)          │           │
│  │     • Refresh Token: "Get me a new access token" — sent ONLY to auth      │           │
│  │       server, long-lived (7-30 days), stored server-side for revocation    │           │
│  │                                                                               │           │
│  │  2. KEY RULE: DON'T MIX UP TOKEN PURPOSES                                  │           │
│  │     • Access Token → API servers (for authorization)                       │           │
│  │     • ID Token → client app only (for UI display)                          │           │
│  │     • Refresh Token → auth server only (for token renewal)                 │           │
│  │                                                                               │           │
│  │  3. JWKS (JSON WEB KEY SET)                                                  │           │
│  │     • JSON document hosting PUBLIC keys at a well-known URL                │           │
│  │     • Used with asymmetric algorithms (RS256, ES256)                        │           │
│  │     • Solves: automatic key distribution & zero-downtime key rotation     │           │
│  │     • API servers auto-fetch public keys → no manual key copying          │           │
│  │     • "kid" in JWT header maps to specific key in JWKS                     │           │
│  │     • Spring: spring.security.oauth2.resourceserver.jwt.jwk-set-uri       │           │
│  │                                                                               │           │
│  │  4. IS JWT REALLY STATELESS?                                                 │           │
│  │     • In theory: YES — server stores nothing, token is self-contained      │           │
│  │     • In practice: NO — you need Redis for token blacklisting              │           │
│  │     • Pure stateless = no logout, no revocation, no blacklisting           │           │
│  │     • Practical JWT = stateless verification + stateful blacklist layer   │           │
│  │     • Still MUCH lighter than full session storage                          │           │
│  │                                                                               │           │
│  │  5. WHERE REDIS IS NEEDED                                                    │           │
│  │     • Token blacklist: store revoked token JTIs with TTL = remaining exp  │           │
│  │     • Refresh token storage: for rotation and revocation                   │           │
│  │     • Auto-cleanup: Redis TTL auto-deletes expired entries                 │           │
│  │                                                                               │           │
│  │  6. HOW TO BLACKLIST A TOKEN                                                 │           │
│  │     • On logout: SET "blacklist:<jti>" with TTL = remaining exp time      │           │
│  │     • On every request: EXISTS "blacklist:<jti>" → if true, reject 401    │           │
│  │     • Auto-cleanup: TTL ensures blacklisted tokens are removed            │           │
│  │       from Redis when they would have expired anyway                        │           │
│  │                                                                               │           │
│  │  7. WHEN TO BLACKLIST                                                        │           │
│  │     • User logout, password change, admin ban, role change,                │           │
│  │       token theft detection                                                  │           │
│  │     • Alternative: very short access tokens (5 min) without blacklisting  │           │
│  │       for low-sensitivity applications                                      │           │
│  │                                                                               │           │
│  │  8. REFRESH TOKEN ROTATION (Security Best Practice)                         │           │
│  │     • Issue new refresh token on each refresh (one-time use)               │           │
│  │     • If old refresh token is reused → detect theft → revoke all tokens   │           │
│  │     • Supported by Auth0, Keycloak, Okta out of the box                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```


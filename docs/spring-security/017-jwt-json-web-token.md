### 17. JWT (JSON Web Token) — Deep Dive, Structure, Internals, Verification & Best Practices

---

#### 17.1 What Is JWT — Overview & Definition

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHAT IS JWT (JSON WEB TOKEN)?                                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  JWT (JSON Web Token) is an open standard (RFC 7519) that defines a compact,                │
│  self-contained way to securely transmit information between parties as a                    │
│  JSON object. This information can be verified and trusted because it is                    │
│  digitally signed.                                                                           │
│                                                                                              │
│                                                                                              │
│  ── KEY CHARACTERISTICS ─────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. COMPACT — Small enough to be sent in URL, HTTP header, or POST body    │           │
│  │  2. SELF-CONTAINED — The token itself carries all user info (claims)        │           │
│  │     → Server doesn't need to query a database to know who the user is      │           │
│  │  3. DIGITALLY SIGNED — Cannot be tampered with without detection           │           │
│  │  4. STATELESS — Server doesn't store the token; client stores it            │           │
│  │  5. JSON-BASED — Uses JSON for the payload (human-readable)                │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── HOW JWT DIFFERS FROM SESSION-BASED AUTH ─────────────────────────────────               │
│                                                                                              │
│  ┌─── SESSION-BASED (Form Login / Basic Auth) ───────────────────────────────┐             │
│  │                                                                            │             │
│  │  Client ──── POST /login ────→ Server                                      │             │
│  │                                  │                                          │             │
│  │                                  ▼                                          │             │
│  │                              Create Session in SERVER MEMORY               │             │
│  │                              {sessionId: "ABC", user: "john", roles: [...]}│             │
│  │                              Store in HttpSession (server-side)            │             │
│  │                                  │                                          │             │
│  │  Client ←── Set-Cookie: JSESSIONID=ABC ── Server                           │             │
│  │                                                                            │             │
│  │  Subsequent requests:                                                      │             │
│  │  Client ──── Cookie: JSESSIONID=ABC ────→ Server                           │             │
│  │                                              │                              │             │
│  │                                              ▼                              │             │
│  │                                          Look up session ABC               │             │
│  │                                          in server memory/DB               │             │
│  │                                          → Found! User = john              │             │
│  │                                                                            │             │
│  │  ★ Server STORES state (session) — STATEFUL                               │             │
│  │  ★ Server must look up session on EVERY request                           │             │
│  │  ★ Sticky sessions needed in multi-server setups                          │             │
│  │                                                                            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
│  ┌─── JWT-BASED (Token Auth) ────────────────────────────────────────────────┐             │
│  │                                                                            │             │
│  │  Client ──── POST /auth/login ────→ Server                                 │             │
│  │                                       │                                     │             │
│  │                                       ▼                                     │             │
│  │                                   Validate credentials (DB lookup)         │             │
│  │                                   Generate JWT token containing:           │             │
│  │                                   {sub:"john", roles:["ADMIN"], exp:...}   │             │
│  │                                   Sign with secret key                     │             │
│  │                                       │                                     │             │
│  │  Client ←── { "token": "eyJhbG..." } ── Server                             │             │
│  │                                                                            │             │
│  │  Subsequent requests:                                                      │             │
│  │  Client ──── Authorization: Bearer eyJhbG... ────→ Server                  │             │
│  │                                                      │                      │             │
│  │                                                      ▼                      │             │
│  │                                                  Verify signature          │             │
│  │                                                  (NO DB lookup!)           │             │
│  │                                                  Decode payload            │             │
│  │                                                  → User = john             │             │
│  │                                                  → Roles = [ADMIN]         │             │
│  │                                                                            │             │
│  │  ★ Server stores NOTHING — STATELESS                                      │             │
│  │  ★ Token carries all info — NO session lookup                             │             │
│  │  ★ ANY server can validate the token (just needs the secret key)          │             │
│  │  ★ Perfect for microservices and horizontal scaling                       │             │
│  │                                                                            │             │
│  └────────────────────────────────────────────────────────────────────────────┘             │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.2 ★ Structure of a JWT Token — The Three Parts (Header.Payload.Signature)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ STRUCTURE OF A JWT TOKEN                                                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  A JWT token is a string with THREE parts, separated by dots (.):                           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.                                     │           │
│  │  eyJzdWIiOiJqb2huIiwicm9sZXMiOlsiUk9MRV9BRE1JTiJdLCJpYXQiOjE3MTU...      │           │
│  │  .SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c                             │           │
│  │                                                                               │           │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌──────────────────┐     │           │
│  │  │      HEADER          │  │      PAYLOAD         │  │    SIGNATURE     │     │           │
│  │  │  (Algorithm + Type)  │  │  (Claims / Data)     │  │  (Verification)  │     │           │
│  │  └─────────────────────┘  └─────────────────────┘  └──────────────────┘     │           │
│  │         Part 1        .         Part 2          .        Part 3             │           │
│  │                                                                               │           │
│  │  ★ Each part is Base64Url-encoded (NOT encrypted!)                          │           │
│  │  ★ Anyone can decode and READ the header and payload                        │           │
│  │  ★ Only the SIGNATURE prevents tampering                                    │           │
│  │  ★ NEVER put sensitive data (passwords, SSN, credit card) in JWT!          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PART 1: HEADER                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The header typically contains:                                               │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "alg": "HS256",     ← Algorithm used for signing                         │           │
│  │    "typ": "JWT"        ← Token type (always "JWT")                          │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ── COMMON ALGORITHMS ───────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌──────────────┬────────────────────────┬──────────────────────────────┐   │           │
│  │  │  Algorithm    │  Type                  │  Description                 │   │           │
│  │  ├──────────────┼────────────────────────┼──────────────────────────────┤   │           │
│  │  │  HS256       │  HMAC + SHA-256        │  Symmetric (same secret key  │   │           │
│  │  │              │  (Symmetric)           │  for sign AND verify)        │   │           │
│  │  ├──────────────┼────────────────────────┼──────────────────────────────┤   │           │
│  │  │  HS384       │  HMAC + SHA-384        │  Symmetric, stronger hash    │   │           │
│  │  ├──────────────┼────────────────────────┼──────────────────────────────┤   │           │
│  │  │  HS512       │  HMAC + SHA-512        │  Symmetric, strongest hash   │   │           │
│  │  ├──────────────┼────────────────────────┼──────────────────────────────┤   │           │
│  │  │  RS256       │  RSA + SHA-256         │  Asymmetric (private key to  │   │           │
│  │  │              │  (Asymmetric)          │  sign, public key to verify) │   │           │
│  │  ├──────────────┼────────────────────────┼──────────────────────────────┤   │           │
│  │  │  RS384       │  RSA + SHA-384         │  Asymmetric, stronger hash   │   │           │
│  │  ├──────────────┼────────────────────────┼──────────────────────────────┤   │           │
│  │  │  RS512       │  RSA + SHA-512         │  Asymmetric, strongest hash  │   │           │
│  │  ├──────────────┼────────────────────────┼──────────────────────────────┤   │           │
│  │  │  ES256       │  ECDSA + SHA-256       │  Asymmetric, elliptic curve  │   │           │
│  │  │              │  (Asymmetric)          │  (smaller keys, same safety) │   │           │
│  │  ├──────────────┼────────────────────────┼──────────────────────────────┤   │           │
│  │  │  PS256       │  RSASSA-PSS + SHA-256  │  Asymmetric, newer RSA      │   │           │
│  │  └──────────────┴────────────────────────┴──────────────────────────────┘   │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── SYMMETRIC vs ASYMMETRIC ─────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  SYMMETRIC (HS256):                                                          │           │
│  │  ┌──────────────────────────────────────────────────────────────┐            │           │
│  │  │  Same secret key used to SIGN and VERIFY                     │            │           │
│  │  │                                                              │            │           │
│  │  │  Auth Server ──── sign with "mySecret" ────→ JWT token      │            │           │
│  │  │  API Server  ──── verify with "mySecret" ──→ Valid!         │            │           │
│  │  │                                                              │            │           │
│  │  │  ★ Both servers must have the SAME secret                   │            │           │
│  │  │  ★ Good for: single service or tightly coupled services    │            │           │
│  │  │  ★ Problem: if secret leaks, anyone can create fake tokens │            │           │
│  │  └──────────────────────────────────────────────────────────────┘            │           │
│  │                                                                               │           │
│  │  ASYMMETRIC (RS256):                                                         │           │
│  │  ┌──────────────────────────────────────────────────────────────┐            │           │
│  │  │  Private key to SIGN, Public key to VERIFY                   │            │           │
│  │  │                                                              │            │           │
│  │  │  Auth Server ──── sign with PRIVATE KEY ────→ JWT token     │            │           │
│  │  │  API Server  ──── verify with PUBLIC KEY ───→ Valid!        │            │           │
│  │  │                                                              │            │           │
│  │  │  ★ Only auth server has the private key (can create tokens)│            │           │
│  │  │  ★ API servers only have the public key (can only verify)  │            │           │
│  │  │  ★ Public key can be shared freely (via JWKS endpoint)     │            │           │
│  │  │  ★ Good for: microservices, multi-service architectures    │            │           │
│  │  │  ★ Even if public key leaks → attacker can't create tokens│            │           │
│  │  └──────────────────────────────────────────────────────────────┘            │           │
│  │                                                                               │           │
│  │  For with-JWKS asymmetric header:                                            │           │
│  │  {                                                                            │           │
│  │    "alg": "RS256",                                                           │           │
│  │    "typ": "JWT",                                                             │           │
│  │    "kid": "my-key-id-1"   ← Key ID (identifies which key pair was used)    │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PART 2: PAYLOAD (Claims)                                                                    │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The payload contains CLAIMS — statements about the user and metadata.      │           │
│  │  There are three types of claims:                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 1. REGISTERED CLAIMS (Standard, defined by RFC 7519) ─────            │           │
│  │  These are predefined keys with specific meanings:                          │           │
│  │                                                                               │           │
│  │  ┌──────────┬────────────────────┬────────────────────────────────────────┐ │           │
│  │  │  Key     │  Full Name         │  Description                           │ │           │
│  │  ├──────────┼────────────────────┼────────────────────────────────────────┤ │           │
│  │  │  iss     │  Issuer            │  Who issued this token                 │ │           │
│  │  │          │                    │  e.g., "https://auth.myapp.com"        │ │           │
│  │  ├──────────┼────────────────────┼────────────────────────────────────────┤ │           │
│  │  │  sub     │  Subject           │  Who this token is about (user ID)    │ │           │
│  │  │          │                    │  e.g., "john" or "user-uuid-123"      │ │           │
│  │  ├──────────┼────────────────────┼────────────────────────────────────────┤ │           │
│  │  │  aud     │  Audience          │  Who this token is intended for       │ │           │
│  │  │          │                    │  e.g., "https://api.myapp.com"         │ │           │
│  │  ├──────────┼────────────────────┼────────────────────────────────────────┤ │           │
│  │  │  exp     │  Expiration Time   │  When this token expires (Unix time)  │ │           │
│  │  │          │                    │  e.g., 1715270400 (epoch seconds)     │ │           │
│  │  ├──────────┼────────────────────┼────────────────────────────────────────┤ │           │
│  │  │  nbf     │  Not Before        │  Token not valid before this time     │ │           │
│  │  │          │                    │  e.g., 1715184000                     │ │           │
│  │  ├──────────┼────────────────────┼────────────────────────────────────────┤ │           │
│  │  │  iat     │  Issued At         │  When the token was created           │ │           │
│  │  │          │                    │  e.g., 1715184000                     │ │           │
│  │  ├──────────┼────────────────────┼────────────────────────────────────────┤ │           │
│  │  │  jti     │  JWT ID            │  Unique identifier for this token     │ │           │
│  │  │          │                    │  Used to prevent token replay attacks │ │           │
│  │  └──────────┴────────────────────┴────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │  ★ All registered claims are OPTIONAL — but iss, sub, exp, iat              │           │
│  │    are strongly recommended!                                                 │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 2. PUBLIC CLAIMS (Custom, should be collision-resistant) ──            │           │
│  │  Custom claims registered in the IANA JSON Web Token Claims registry       │           │
│  │  or using collision-resistant names (e.g., namespaced URIs):               │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "https://myapp.com/roles": ["ADMIN", "USER"],                            │           │
│  │    "https://myapp.com/tenant": "org-123"                                    │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 3. PRIVATE CLAIMS (Custom, agreed between parties) ────────           │           │
│  │  Application-specific claims not registered anywhere:                       │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "roles": ["ROLE_ADMIN", "ROLE_USER"],                                    │           │
│  │    "userId": 42,                                                             │           │
│  │    "email": "john@example.com",                                              │           │
│  │    "tenantId": "org-123"                                                     │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── COMPLETE PAYLOAD EXAMPLE ──────────────────────────────────            │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "iss": "https://auth.myapp.com",       ← who issued it                  │           │
│  │    "sub": "john",                          ← subject (username/userId)      │           │
│  │    "aud": "https://api.myapp.com",        ← intended audience              │           │
│  │    "iat": 1715184000,                      ← issued at (Unix timestamp)    │           │
│  │    "exp": 1715270400,                      ← expires at (Unix timestamp)   │           │
│  │    "jti": "550e8400-e29b-41d4-a716-446655440000",  ← unique token ID      │           │
│  │    "roles": ["ROLE_ADMIN", "ROLE_USER"],   ← custom: user roles            │           │
│  │    "email": "john@example.com"             ← custom: user email            │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ════════════════════════════════════════════════════════════════════════════════             │
│  PART 3: SIGNATURE                                                                           │
│  ════════════════════════════════════════════════════════════════════════════════             │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The signature is created by combining the encoded header and payload       │           │
│  │  and signing them with a secret key (or private key):                        │           │
│  │                                                                               │           │
│  │  HMAC-SHA256 (symmetric):                                                    │           │
│  │  ┌─────────────────────────────────────────────────────────────────────┐    │           │
│  │  │                                                                     │    │           │
│  │  │  signature = HMACSHA256(                                            │    │           │
│  │  │      base64UrlEncode(header) + "." + base64UrlEncode(payload),     │    │           │
│  │  │      secret                                                        │    │           │
│  │  │  )                                                                  │    │           │
│  │  │                                                                     │    │           │
│  │  │  Input:   "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIn0"             │    │           │
│  │  │  Secret:  "mySecretKey123"                                         │    │           │
│  │  │  Output:  "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"          │    │           │
│  │  │                                                                     │    │           │
│  │  └─────────────────────────────────────────────────────────────────────┘    │           │
│  │                                                                               │           │
│  │  RSA-SHA256 (asymmetric):                                                    │           │
│  │  ┌─────────────────────────────────────────────────────────────────────┐    │           │
│  │  │                                                                     │    │           │
│  │  │  signature = RSA_SHA256(                                            │    │           │
│  │  │      base64UrlEncode(header) + "." + base64UrlEncode(payload),     │    │           │
│  │  │      privateKey                                                    │    │           │
│  │  │  )                                                                  │    │           │
│  │  │                                                                     │    │           │
│  │  │  Verification:                                                      │    │           │
│  │  │  isValid = RSA_SHA256_VERIFY(                                       │    │           │
│  │  │      base64UrlEncode(header) + "." + base64UrlEncode(payload),     │    │           │
│  │  │      signature,                                                    │    │           │
│  │  │      publicKey                                                     │    │           │
│  │  │  )                                                                  │    │           │
│  │  │                                                                     │    │           │
│  │  └─────────────────────────────────────────────────────────────────────┘    │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── WHAT THE SIGNATURE PROTECTS AGAINST ──────────────────────            │           │
│  │                                                                               │           │
│  │  ★ TAMPERING: If an attacker changes ANY character in the header           │           │
│  │    or payload (e.g., changes "role":"USER" to "role":"ADMIN"),              │           │
│  │    the signature will NOT match → server rejects the token!                │           │
│  │                                                                               │           │
│  │  ★ FORGERY: Without the secret/private key, an attacker CANNOT             │           │
│  │    create a valid signature → cannot create fake tokens!                    │           │
│  │                                                                               │           │
│  │  ⚠️ DOES NOT PROTECT: The payload is NOT encrypted!                        │           │
│  │  Anyone with the token can decode and READ the claims.                     │           │
│  │  → NEVER put passwords, credit cards, SSN in a JWT!                        │           │
│  │  → If you need encrypted payloads, use JWE (JSON Web Encryption)          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── VISUAL: THE COMPLETE JWT ────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9                                      │           │
│  │  ▲ HEADER (Base64Url encoded)                                                │           │
│  │  Decodes to: {"alg":"HS256","typ":"JWT"}                                    │           │
│  │                                                                               │           │
│  │  .                                                                            │           │
│  │                                                                               │           │
│  │  eyJzdWIiOiJqb2huIiwicm9sZXMiOlsiUk9MRV9BRE1JTiJdLCJpYXQiOjE3MTUxODQw   │           │
│  │  MDAsImV4cCI6MTcxNTI3MDQwMH0                                                │           │
│  │  ▲ PAYLOAD (Base64Url encoded)                                               │           │
│  │  Decodes to: {"sub":"john","roles":["ROLE_ADMIN"],"iat":1715184000,         │           │
│  │               "exp":1715270400}                                              │           │
│  │                                                                               │           │
│  │  .                                                                            │           │
│  │                                                                               │           │
│  │  SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c                              │           │
│  │  ▲ SIGNATURE (cannot be decoded — it's a hash)                               │           │
│  │  Created by: HMACSHA256(header + "." + payload, secret)                     │           │
│  │                                                                               │           │
│  │  ★ You can paste this token into jwt.io to decode and inspect it!          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.3 What Data Should (and Should NOT) Go Inside a JWT

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHAT DATA TO PUT IN A JWT — AND WHAT TO NEVER PUT                                           │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── ✅ DATA YOU SHOULD PUT IN A JWT ─────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "iss": "https://auth.myapp.com",     ✅ Issuer (who created it)           │           │
│  │    "sub": "user-uuid-12345",            ✅ Subject (user identifier)         │           │
│  │    "aud": "https://api.myapp.com",      ✅ Audience (intended recipient)     │           │
│  │    "iat": 1715184000,                    ✅ Issued at (creation timestamp)   │           │
│  │    "exp": 1715185800,                    ✅ Expiration (30 min from iat)     │           │
│  │    "jti": "uuid-unique-token-id",        ✅ Unique token ID (replay guard)  │           │
│  │    "roles": ["ROLE_ADMIN","ROLE_USER"],   ✅ User roles/authorities          │           │
│  │    "email": "john@example.com",           ✅ Email (if needed by services)   │           │
│  │    "tenantId": "org-456"                  ✅ Tenant (multi-tenant apps)      │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  GUIDELINES:                                                                  │           │
│  │  • Keep the payload SMALL — large tokens mean larger HTTP headers            │           │
│  │  • Include only what the API servers NEED to process the request            │           │
│  │  • Include roles/permissions to avoid DB lookups on every request           │           │
│  │  • Include user identifier (sub) for audit logging                          │           │
│  │  • Always include exp (expiration) — tokens without expiry are dangerous!  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── ❌ DATA YOU SHOULD NEVER PUT IN A JWT ───────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ❌ Password / Password hash                                                  │           │
│  │     → JWT payload is NOT encrypted — anyone can decode it!                  │           │
│  │                                                                               │           │
│  │  ❌ Social Security Number (SSN) / National ID                                │           │
│  │     → PII that should never travel in a readable token                      │           │
│  │                                                                               │           │
│  │  ❌ Credit card numbers / Bank account details                                │           │
│  │     → PCI-DSS compliance violation                                           │           │
│  │                                                                               │           │
│  │  ❌ Secret keys / API keys                                                    │           │
│  │     → Would expose secrets to anyone who has the token                      │           │
│  │                                                                               │           │
│  │  ❌ Large data blobs (full user profile, preferences, images)                 │           │
│  │     → Makes the token too large, exceeds header size limits                 │           │
│  │     → HTTP headers have typical limits of 8KB-16KB                          │           │
│  │                                                                               │           │
│  │  ❌ Frequently changing data (shopping cart, notification count)               │           │
│  │     → Token is immutable once signed — can't update without reissuing      │           │
│  │                                                                               │           │
│  │  RULE OF THUMB:                                                               │           │
│  │  ★ If the data is SENSITIVE → don't put it in JWT                           │           │
│  │  ★ If the data is LARGE → don't put it in JWT                              │           │
│  │  ★ If the data CHANGES FREQUENTLY → don't put it in JWT                    │           │
│  │  ★ JWT payload should be under 1KB ideally                                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.4 Expiration Time — How It Works and Best Practices

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  JWT EXPIRATION TIME (exp claim)                                                             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The "exp" claim is a Unix timestamp (seconds since Jan 1, 1970 UTC)        │           │
│  │  that defines WHEN the token becomes invalid.                                │           │
│  │                                                                               │           │
│  │  EXAMPLE:                                                                     │           │
│  │  {                                                                            │           │
│  │    "iat": 1715184000,     ← Issued at: May 8, 2024 12:00:00 UTC           │           │
│  │    "exp": 1715185800      ← Expires at: May 8, 2024 12:30:00 UTC          │           │
│  │  }                                                                            │           │
│  │  → Token valid for 30 minutes (1800 seconds)                                │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  HOW VERIFICATION WORKS:                                                     │           │
│  │  if (currentTime > token.exp) {                                              │           │
│  │      throw new ExpiredJwtException("Token has expired!");                   │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── RECOMMENDED EXPIRATION TIMES ────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌─────────────────────┬───────────────────┬───────────────────────────────┐│           │
│  │  │  Token Type          │  Expiry            │  Reason                       ││           │
│  │  ├─────────────────────┼───────────────────┼───────────────────────────────┤│           │
│  │  │  Access Token        │  15-30 minutes     │  Short-lived: limits damage  ││           │
│  │  │                     │                   │  if token is stolen           ││           │
│  │  ├─────────────────────┼───────────────────┼───────────────────────────────┤│           │
│  │  │  Refresh Token       │  7-30 days         │  Long-lived: used to get new ││           │
│  │  │                     │                   │  access tokens without login  ││           │
│  │  ├─────────────────────┼───────────────────┼───────────────────────────────┤│           │
│  │  │  ID Token            │  5-60 minutes      │  Short-lived: represents     ││           │
│  │  │                     │                   │  current authentication       ││           │
│  │  ├─────────────────────┼───────────────────┼───────────────────────────────┤│           │
│  │  │  Service-to-Service  │  5-15 minutes      │  Very short: machine tokens  ││           │
│  │  └─────────────────────┴───────────────────┴───────────────────────────────┘│           │
│  │                                                                               │           │
│  │  ⚠️ NEVER create tokens without an exp claim!                               │           │
│  │  ⚠️ A token without expiry is valid FOREVER until the secret key changes   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.5 ★ How JWT Actually Works — Complete Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW JWT AUTHENTICATION WORKS — COMPLETE FLOW                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── PHASE 1: LOGIN (Obtain Token) ───────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Client                              Auth Server                             │           │
│  │  ──────                              ───────────                             │           │
│  │    │                                       │                                  │           │
│  │    │  POST /auth/login                     │                                  │           │
│  │    │  { "username": "john",                │                                  │           │
│  │    │    "password": "secret123" }           │                                  │           │
│  │    │─────────────────────────────────────→ │                                  │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  1. Load user from DB            │           │
│  │    │                                       │     SELECT * FROM users           │           │
│  │    │                                       │     WHERE username = 'john'      │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  2. Verify password               │           │
│  │    │                                       │     BCrypt.matches(               │           │
│  │    │                                       │       "secret123",               │           │
│  │    │                                       │       storedHash) → ✅            │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  3. Build JWT payload:            │           │
│  │    │                                       │     {sub:"john",                 │           │
│  │    │                                       │      roles:["ADMIN"],            │           │
│  │    │                                       │      iat: now(),                 │           │
│  │    │                                       │      exp: now()+30min}           │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  4. Sign JWT with secret key     │           │
│  │    │                                       │     HMACSHA256(                   │           │
│  │    │                                       │       header.payload,            │           │
│  │    │                                       │       secretKey)                 │           │
│  │    │                                       │                                  │           │
│  │    │  200 OK                               │                                  │           │
│  │    │  {                                    │                                  │           │
│  │    │    "accessToken": "eyJhbG...",        │                                  │           │
│  │    │    "refreshToken": "eyJhbG...",       │                                  │           │
│  │    │    "expiresIn": 1800                  │                                  │           │
│  │    │  }                                    │                                  │           │
│  │    │←─────────────────────────────────────│                                  │           │
│  │    │                                       │                                  │           │
│  │    │  Client stores token                  │                                  │           │
│  │    │  (localStorage, cookie, memory)       │                                  │           │
│  │    │                                       │                                  │           │
│  │                                                                               │           │
│  │  ★ This is the ONLY time credentials are sent and DB is queried!           │           │
│  │  ★ After this, the token is used for ALL subsequent requests               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── PHASE 2: ACCESSING PROTECTED RESOURCES (Use Token) ──────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Client                              API Server                              │           │
│  │  ──────                              ──────────                              │           │
│  │    │                                       │                                  │           │
│  │    │  GET /api/orders                      │                                  │           │
│  │    │  Authorization: Bearer eyJhbG...      │                                  │           │
│  │    │─────────────────────────────────────→ │                                  │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  1. Extract token from header    │           │
│  │    │                                       │     "Bearer eyJhbG..." → token  │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  2. Verify signature             │           │
│  │    │                                       │     HMACSHA256(header.payload,   │           │
│  │    │                                       │       secretKey) == signature?   │           │
│  │    │                                       │     → ✅ Valid! Not tampered     │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  3. Check expiration             │           │
│  │    │                                       │     now() < exp?                 │           │
│  │    │                                       │     → ✅ Not expired             │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  4. Extract claims               │           │
│  │    │                                       │     sub: "john"                  │           │
│  │    │                                       │     roles: ["ADMIN"]             │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  5. Set SecurityContext           │           │
│  │    │                                       │     (in ThreadLocal)             │           │
│  │    │                                       │     ★ NO DB LOOKUP!             │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  6. AuthorizationFilter          │           │
│  │    │                                       │     checks access rules          │           │
│  │    │                                       │                                  │           │
│  │    │                                       │  7. Controller processes         │           │
│  │    │                                       │     request and returns data     │           │
│  │    │                                       │                                  │           │
│  │    │  200 OK                               │                                  │           │
│  │    │  [{ orderId: 1, ... }]                │                                  │           │
│  │    │←─────────────────────────────────────│                                  │           │
│  │    │                                       │                                  │           │
│  │                                                                               │           │
│  │  ★ NO session lookup, NO DB query, NO password verification!                │           │
│  │  ★ Server only verifies the signature (CPU operation, NO I/O!)             │           │
│  │  ★ ALL user info comes from the token itself (self-contained!)             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.6 ★ How JWT Verification Is Actually Done — Step-by-Step

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW JWT VERIFICATION WORKS — STEP BY STEP                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  When a server receives a JWT, it performs these steps IN ORDER:                             │
│                                                                                              │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Token received: eyJhbGci...<HEADER>.eyJzdWIi...<PAYLOAD>.SflKx...<SIG>     │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  STEP 1: SPLIT THE TOKEN                                                     │           │
│  │  ─────────────────────────                                                   │           │
│  │  Split by "." → [header, payload, signature]                                │           │
│  │  ★ If not exactly 3 parts → REJECT (malformed token)                       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  STEP 2: DECODE THE HEADER (Base64Url → JSON)                               │           │
│  │  ─────────────────────────────────────────────                               │           │
│  │  Base64UrlDecode("eyJhbGciOiJIUzI1NiJ9")                                   │           │
│  │  → {"alg":"HS256","typ":"JWT"}                                               │           │
│  │  ★ Check "alg" is an ALLOWED algorithm (prevent "alg":"none" attack!)      │           │
│  │  ★ If "alg" is "none" → REJECT immediately!                               │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  STEP 3: VERIFY THE SIGNATURE                                                │           │
│  │  ─────────────────────────────                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────┐     │           │
│  │  │  a. Take the first two parts (header.payload) as-is              │     │           │
│  │  │     input = "eyJhbGci...eyJzdWIi..."                             │     │           │
│  │  │                                                                   │     │           │
│  │  │  b. Compute a NEW signature using the same algorithm:             │     │           │
│  │  │     expectedSig = HMACSHA256(input, secretKey)                   │     │           │
│  │  │                                                                   │     │           │
│  │  │  c. Compare expected signature with received signature:           │     │           │
│  │  │     if (expectedSig != receivedSig) → REJECT! Tampered!         │     │           │
│  │  │     if (expectedSig == receivedSig) → ✅ Valid! Not tampered    │     │           │
│  │  │                                                                   │     │           │
│  │  │  ★ This is WHY tampering is detected:                           │     │           │
│  │  │    Attacker changes payload → signature no longer matches       │     │           │
│  │  │    Attacker can't compute new signature without secret key      │     │           │
│  │  │                                                                   │     │           │
│  │  └────────────────────────────────────────────────────────────────────┘     │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  STEP 4: DECODE THE PAYLOAD (Base64Url → JSON)                              │           │
│  │  ─────────────────────────────────────────────                               │           │
│  │  Base64UrlDecode("eyJzdWIi...")                                              │           │
│  │  → {"sub":"john","roles":["ADMIN"],"iat":1715184000,"exp":1715185800}       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  STEP 5: VALIDATE CLAIMS                                                     │           │
│  │  ─────────────────────────                                                   │           │
│  │  ┌────────────────────────────────────────────────────────────────────┐     │           │
│  │  │  a. Check "exp" (expiration):                                     │     │           │
│  │  │     if (currentTime > exp) → REJECT! Token expired              │     │           │
│  │  │                                                                   │     │           │
│  │  │  b. Check "nbf" (not before), if present:                        │     │           │
│  │  │     if (currentTime < nbf) → REJECT! Token not yet valid        │     │           │
│  │  │                                                                   │     │           │
│  │  │  c. Check "iss" (issuer), if configured:                         │     │           │
│  │  │     if (token.iss != expectedIssuer) → REJECT!                  │     │           │
│  │  │                                                                   │     │           │
│  │  │  d. Check "aud" (audience), if configured:                       │     │           │
│  │  │     if (token.aud != expectedAudience) → REJECT!                │     │           │
│  │  │                                                                   │     │           │
│  │  └────────────────────────────────────────────────────────────────────┘     │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  STEP 6: EXTRACT USER INFORMATION                                            │           │
│  │  ────────────────────────────────                                            │           │
│  │  username = claims.get("sub")          → "john"                              │           │
│  │  roles    = claims.get("roles")        → ["ROLE_ADMIN"]                      │           │
│  │  email    = claims.get("email")        → "john@example.com"                  │           │
│  │                                                                               │           │
│  │  → Create Authentication object and set in SecurityContextHolder            │           │
│  │  → ★ ALL user info came from the token — NO DATABASE QUERY!                │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── VISUAL: WHY TAMPERING IS DETECTED ───────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ORIGINAL TOKEN (valid):                                                     │           │
│  │  Header: {"alg":"HS256"}                                                     │           │
│  │  Payload: {"sub":"john","roles":["USER"]}                                    │           │
│  │  Signature: HMACSHA256("header.payload", secret) = "abc123"                 │           │
│  │                                                                               │           │
│  │  ATTACKER modifies payload:                                                  │           │
│  │  Payload: {"sub":"john","roles":["ADMIN"]}   ← changed USER to ADMIN!      │           │
│  │  Signature: still "abc123" (attacker can't recompute without secret)        │           │
│  │                                                                               │           │
│  │  SERVER verifies:                                                             │           │
│  │  expectedSig = HMACSHA256("header.MODIFIED_payload", secret) = "xyz789"     │           │
│  │  receivedSig = "abc123"                                                      │           │
│  │  "xyz789" != "abc123" → ❌ SIGNATURE MISMATCH → TOKEN REJECTED!            │           │
│  │                                                                               │           │
│  │  ★ Without the secret key, the attacker CANNOT create a matching signature │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.7 Why JWT Is Called Stateless — And Why That Matters

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  WHY JWT IS CALLED STATELESS — AND WHY IT MATTERS                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── WHAT "STATELESS" MEANS ──────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  "Stateless" means the SERVER does not store ANY information about           │           │
│  │  the user's authentication between requests.                                 │           │
│  │                                                                               │           │
│  │  STATEFUL (Session-based):                                                   │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │  Server stores:                                                  │       │           │
│  │  │  sessions = {                                                    │       │           │
│  │  │    "ABC123": { user: "john", roles: ["ADMIN"], loginTime: ...}, │       │           │
│  │  │    "DEF456": { user: "jane", roles: ["USER"],  loginTime: ...}, │       │           │
│  │  │    "GHI789": { user: "bob",  roles: ["USER"],  loginTime: ...}  │       │           │
│  │  │  }                                                               │       │           │
│  │  │  ★ Server REMEMBERS every logged-in user                        │       │           │
│  │  │  ★ Server memory grows with number of active sessions          │       │           │
│  │  │  ★ 10,000 users = 10,000 sessions in server memory            │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  │  STATELESS (JWT-based):                                                      │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │  Server stores: NOTHING!                                        │       │           │
│  │  │                                                                  │       │           │
│  │  │  sessions = {}   ← empty, always                                │       │           │
│  │  │                                                                  │       │           │
│  │  │  The TOKEN itself carries all the info the server needs:        │       │           │
│  │  │  Token: {sub:"john", roles:["ADMIN"], exp:1715185800}           │       │           │
│  │  │                                                                  │       │           │
│  │  │  ★ Server FORGETS the user immediately after each request      │       │           │
│  │  │  ★ Next request: server reads token again → knows the user     │       │           │
│  │  │  ★ Server memory is CONSTANT regardless of users               │       │           │
│  │  │  ★ 10,000 users or 1,000,000 users → same server memory      │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHY STATELESS MATTERS — SCALING BENEFITS ────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  STATEFUL (Sessions) — Scaling Problem:                                      │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  Load Balancer                                                   │       │           │
│  │  │       │                                                          │       │           │
│  │  │  ┌────┼────────────────┐                                        │       │           │
│  │  │  │    │                 │                                        │       │           │
│  │  │  ▼    ▼                 ▼                                        │       │           │
│  │  │  Server A    Server B    Server C                               │       │           │
│  │  │  session:    session:    session:                                │       │           │
│  │  │  {ABC:john}  {}          {}                                     │       │           │
│  │  │                                                                  │       │           │
│  │  │  ❌ Request with JSESSIONID=ABC goes to Server B → session     │       │           │
│  │  │     NOT FOUND! → 401 Unauthorized!                             │       │           │
│  │  │                                                                  │       │           │
│  │  │  Solutions (all have overhead):                                 │       │           │
│  │  │  • Sticky sessions (load balancer always sends to Server A)    │       │           │
│  │  │  • Shared session store (Redis, database)                      │       │           │
│  │  │  • Session replication across servers                          │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  │  STATELESS (JWT) — Scaling Solution:                                         │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  Load Balancer                                                   │       │           │
│  │  │       │                                                          │       │           │
│  │  │  ┌────┼────────────────┐                                        │       │           │
│  │  │  │    │                 │                                        │       │           │
│  │  │  ▼    ▼                 ▼                                        │       │           │
│  │  │  Server A    Server B    Server C                               │       │           │
│  │  │  sessions:   sessions:   sessions:                              │       │           │
│  │  │  NONE        NONE        NONE                                   │       │           │
│  │  │  secret key  secret key  secret key  ← all have the same key  │       │           │
│  │  │                                                                  │       │           │
│  │  │  ✅ Request with JWT goes to ANY server → all can verify!      │       │           │
│  │  │  ✅ No sticky sessions needed                                  │       │           │
│  │  │  ✅ No shared session store needed                             │       │           │
│  │  │  ✅ Add/remove servers freely                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.8 Why and How to Create JWT — Code with jjwt Library

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  pom.xml — Add jjwt dependency (most popular Java JWT library)
// ═══════════════════════════════════════════════════════════════════════════════

// Maven dependency:
// <dependency>
//     <groupId>io.jsonwebtoken</groupId>
//     <artifactId>jjwt-api</artifactId>
//     <version>0.12.6</version>
// </dependency>
// <dependency>
//     <groupId>io.jsonwebtoken</groupId>
//     <artifactId>jjwt-impl</artifactId>
//     <version>0.12.6</version>
//     <scope>runtime</scope>
// </dependency>
// <dependency>
//     <groupId>io.jsonwebtoken</groupId>
//     <artifactId>jjwt-jackson</artifactId>
//     <version>0.12.6</version>
//     <scope>runtime</scope>
// </dependency>
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  JwtService — Generate & Validate JWT Tokens
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class JwtService {

    // ★ Secret key — should come from application.properties or Vault
    // Must be at least 256 bits (32 bytes) for HS256
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration:1800000}")  // 30 minutes in ms
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")  // 7 days in ms
    private long refreshTokenExpiration;

    // ═══════════════════════════════════════════════════════════════════════════
    //  GENERATE ACCESS TOKEN
    // ═══════════════════════════════════════════════════════════════════════════
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        // Add any custom claims needed by downstream services

        return buildToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GENERATE REFRESH TOKEN (minimal claims — just sub and exp)
    // ═══════════════════════════════════════════════════════════════════════════
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails.getUsername(), refreshTokenExpiration);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BUILD TOKEN — Core token creation logic
    // ═══════════════════════════════════════════════════════════════════════════
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
            .claims(extraClaims)                          // custom claims (roles, etc.)
            .subject(subject)                             // "sub" — username or userId
            .issuer("https://auth.myapp.com")             // "iss" — who issued it
            .issuedAt(new Date(System.currentTimeMillis()))           // "iat"
            .expiration(new Date(System.currentTimeMillis() + expiration))  // "exp"
            .id(UUID.randomUUID().toString())              // "jti" — unique token ID
            .signWith(getSigningKey(), Jwts.SIG.HS256)     // sign with HS256
            .compact();                                    // serialize to string
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  VALIDATE TOKEN — Verify signature + expiration + subject
    // ═══════════════════════════════════════════════════════════════════════════
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXTRACT CLAIMS — Parse and decode the JWT
    // ═══════════════════════════════════════════════════════════════════════════
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())    // set the key for verification
            .build()
            .parseSignedClaims(token)       // parse + verify signature + check exp
            .getPayload();                  // get the claims (payload)
        // ★ If signature is invalid → SignatureException
        // ★ If token is expired → ExpiredJwtException
        // ★ If token is malformed → MalformedJwtException
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

```properties
# ═══════════════════════════════════════════════════════════════════════════════
#  application.properties — JWT Configuration
# ═══════════════════════════════════════════════════════════════════════════════

# Secret key (Base64 encoded, at least 256 bits / 32 bytes for HS256)
# ★ NEVER hardcode in source code! Use environment variable or Vault!
jwt.secret=${JWT_SECRET:dGhpcyBpcyBhIHNhbXBsZSBzZWNyZXQga2V5IGZvciBkZXZlbG9wbWVudA==}

# Access token expiration (30 minutes = 1800000 ms)
jwt.access-token-expiration=1800000

# Refresh token expiration (7 days = 604800000 ms)
jwt.refresh-token-expiration=604800000
```

---

#### 17.9 Advantages and Disadvantages of JWT

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ADVANTAGES AND DISADVANTAGES OF JWT                                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── ✅ ADVANTAGES ───────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. STATELESS — No Server-Side Storage                                      │           │
│  │     • Server stores nothing — no sessions, no session DB                    │           │
│  │     • All user info is inside the token itself                              │           │
│  │     • Server memory doesn't grow with number of users                       │           │
│  │                                                                               │           │
│  │  2. HORIZONTALLY SCALABLE                                                    │           │
│  │     • ANY server can validate the token (just needs the secret key)         │           │
│  │     • No sticky sessions needed                                              │           │
│  │     • Add/remove servers freely behind load balancer                        │           │
│  │     • Perfect for microservices architecture                                │           │
│  │                                                                               │           │
│  │  3. NO DATABASE LOOKUP ON EVERY REQUEST                                     │           │
│  │     • Signature verification is a CPU operation (no I/O)                    │           │
│  │     • No DB query to look up session                                        │           │
│  │     • Much faster than session-based auth at scale                          │           │
│  │                                                                               │           │
│  │  4. CROSS-SERVICE AUTHENTICATION                                             │           │
│  │     • Token can be passed between microservices                             │           │
│  │     • Each service independently verifies the token                         │           │
│  │     • No shared session store needed between services                       │           │
│  │     • Gateway validates once, passes token to downstream services           │           │
│  │                                                                               │           │
│  │  5. CROSS-DOMAIN / CROSS-ORIGIN FRIENDLY                                    │           │
│  │     • Sent in Authorization header (not cookies)                            │           │
│  │     • Works across different domains without cookie limitations             │           │
│  │     • Good for SPAs (Single Page Applications) calling APIs on             │           │
│  │       different domains                                                      │           │
│  │                                                                               │           │
│  │  6. MOBILE-FRIENDLY                                                          │           │
│  │     • Mobile apps can easily store tokens (SharedPreferences, Keychain)    │           │
│  │     • No cookie management needed                                           │           │
│  │     • Works natively with REST APIs                                         │           │
│  │                                                                               │           │
│  │  7. SELF-CONTAINED — Reduces Chattiness                                     │           │
│  │     • User roles embedded in token → no extra call to auth service         │           │
│  │     • Reduces network roundtrips in microservices                           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── ❌ DISADVANTAGES ────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. CANNOT BE REVOKED (Biggest Problem!)                                     │           │
│  │     • Once issued, a JWT is valid until it expires                          │           │
│  │     • If a user logs out → token is STILL valid!                           │           │
│  │     • If a user is banned → token is STILL valid until exp!               │           │
│  │     • If a token is stolen → attacker can use it until exp!               │           │
│  │     • Solutions: short expiry + token blacklist (Redis) — but adds state! │           │
│  │                                                                               │           │
│  │  2. TOKEN SIZE                                                               │           │
│  │     • JWTs are larger than session cookies (JSESSIONID is ~32 chars)       │           │
│  │     • A JWT can be 500-2000+ bytes depending on claims                     │           │
│  │     • Sent on EVERY request in the header → more bandwidth                 │           │
│  │     • HTTP headers have size limits (8KB-16KB for most servers)            │           │
│  │                                                                               │           │
│  │  3. PAYLOAD IS NOT ENCRYPTED                                                 │           │
│  │     • Anyone with the token can decode and READ the payload                │           │
│  │     • Sensitive data must NOT be included                                   │           │
│  │     • Base64 is encoding, NOT encryption                                   │           │
│  │                                                                               │           │
│  │  4. STALE DATA                                                               │           │
│  │     • If user roles change → old token still has old roles                 │           │
│  │     • Token is immutable once signed                                        │           │
│  │     • Must wait for token to expire or force re-login                      │           │
│  │                                                                               │           │
│  │  5. SECRET KEY MANAGEMENT                                                    │           │
│  │     • If secret key is compromised → ALL tokens can be forged             │           │
│  │     • Key rotation requires careful handling of existing tokens            │           │
│  │     • Must be stored securely (Vault, env vars, K8s Secrets)               │           │
│  │                                                                               │           │
│  │  6. COMPLEXITY                                                               │           │
│  │     • More complex than session-based auth                                  │           │
│  │     • Need to handle token refresh, expiration, blacklisting              │           │
│  │     • Client must manage token storage securely                            │           │
│  │     • Token refresh flow adds complexity                                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── COMPARISON TABLE: JWT vs SESSION ────────────────────────────────────────               │
│                                                                                              │
│  ┌─────────────────────────┬──────────────────────────┬──────────────────────────┐         │
│  │  Aspect                 │  Session-Based           │  JWT-Based               │         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  State on server        │  ✅ Session stored        │  ❌ Nothing stored        │         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  DB lookup per request  │  Session store lookup    │  ❌ None (CPU-only verify)│         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  Horizontal scaling     │  ❌ Sticky sessions/Redis │  ✅ Any server can verify │         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  Revocation             │  ✅ Delete session         │  ❌ Hard (needs blacklist)│         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  Token/Cookie size      │  Small (32 chars)        │  Large (500-2000 bytes)  │         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  Cross-domain           │  ❌ Cookie limitations     │  ✅ Works with any domain │         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  Mobile-friendly        │  ⚠️ Cookie issues         │  ✅ Easy token storage    │         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  Microservices          │  ❌ Shared session store   │  ✅ Self-contained tokens │         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  Real-time revocation   │  ✅ Immediate              │  ❌ Must wait for expiry  │         │
│  ├─────────────────────────┼──────────────────────────┼──────────────────────────┤         │
│  │  Best for               │  Server-rendered web apps│  REST APIs, SPAs,        │         │
│  │                         │  (Thymeleaf, JSP)        │  mobile, microservices   │         │
│  └─────────────────────────┴──────────────────────────┴──────────────────────────┘         │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.10 ★ JWT Best Practices — Security Checklist

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ JWT BEST PRACTICES — SECURITY CHECKLIST                                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. ALWAYS SET EXPIRATION (exp)                                              │           │
│  │     • Access tokens: 15-30 minutes                                           │           │
│  │     • Refresh tokens: 7-30 days                                              │           │
│  │     • NEVER create tokens without expiry!                                   │           │
│  │                                                                               │           │
│  │  2. USE STRONG SECRET KEYS                                                   │           │
│  │     • HS256: minimum 256 bits (32 bytes)                                     │           │
│  │     • HS384: minimum 384 bits (48 bytes)                                     │           │
│  │     • HS512: minimum 512 bits (64 bytes)                                     │           │
│  │     • Use cryptographically random keys, not human-readable strings        │           │
│  │     • Store in Vault / K8s Secrets / environment variables                  │           │
│  │     • NEVER hardcode in source code or commit to Git!                       │           │
│  │                                                                               │           │
│  │  3. USE HTTPS ALWAYS                                                         │           │
│  │     • JWT tokens travel in the Authorization header                         │           │
│  │     • Without HTTPS, token can be intercepted (like Basic Auth)            │           │
│  │                                                                               │           │
│  │  4. VALIDATE ALL CLAIMS                                                      │           │
│  │     • Always verify: signature, exp, iss, aud                               │           │
│  │     • Reject tokens with "alg":"none" (critical vulnerability!)            │           │
│  │     • Whitelist allowed algorithms — never accept any algorithm            │           │
│  │                                                                               │           │
│  │  5. DON'T STORE SENSITIVE DATA IN PAYLOAD                                   │           │
│  │     • Payload is Base64 encoded, NOT encrypted                              │           │
│  │     • Anyone can decode and read it                                          │           │
│  │     • No passwords, SSN, credit cards, API keys                             │           │
│  │                                                                               │           │
│  │  6. USE APPROPRIATE ALGORITHM                                                │           │
│  │     • Single service: HS256 (symmetric) is fine                             │           │
│  │     • Multi-service/microservices: RS256 (asymmetric) is better            │           │
│  │     • RS256: only auth server has private key, others verify with public   │           │
│  │                                                                               │           │
│  │  7. IMPLEMENT TOKEN REFRESH FLOW                                             │           │
│  │     • Short-lived access tokens + long-lived refresh tokens                │           │
│  │     • Refresh token stored securely (httpOnly cookie or server-side)        │           │
│  │     • Rotate refresh tokens on each use (detect token theft)               │           │
│  │                                                                               │           │
│  │  8. SECURE TOKEN STORAGE ON CLIENT                                           │           │
│  │     • Browser: httpOnly secure cookie (best) or in-memory (good)           │           │
│  │     • NOT localStorage (XSS vulnerable) unless unavoidable                 │           │
│  │     • Mobile: Keychain (iOS) or EncryptedSharedPreferences (Android)       │           │
│  │                                                                               │           │
│  │  9. IMPLEMENT KEY ROTATION                                                   │           │
│  │     • Plan for regular secret key rotation                                  │           │
│  │     • Use "kid" (key ID) in header to support multiple active keys         │           │
│  │     • Old key verifies old tokens while new key signs new tokens           │           │
│  │                                                                               │           │
│  │  10. KEEP TOKENS SMALL                                                       │           │
│  │      • Only include necessary claims                                         │           │
│  │      • Large tokens → larger HTTP headers → performance impact             │           │
│  │      • Use short claim names when possible                                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── COMMON VULNERABILITIES TO AVOID ─────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. "alg":"none" ATTACK                                                     │           │
│  │     Attacker sets algorithm to "none" in header → no signature needed!     │           │
│  │     → FIX: Always validate algorithm, reject "none"                        │           │
│  │     → jjwt library rejects "none" by default ✅                            │           │
│  │                                                                               │           │
│  │  2. ALGORITHM CONFUSION ATTACK                                               │           │
│  │     Attacker changes RS256 to HS256 and uses PUBLIC key as HMAC secret     │           │
│  │     → FIX: Enforce expected algorithm on the server side                   │           │
│  │     → Never derive algorithm from the token header alone                   │           │
│  │                                                                               │           │
│  │  3. WEAK SECRET KEY                                                          │           │
│  │     Using "secret" or "password123" as HMAC key                            │           │
│  │     → FIX: Use minimum 256-bit cryptographically random key               │           │
│  │                                                                               │           │
│  │  4. TOKEN IN URL                                                             │           │
│  │     Passing JWT as query parameter: /api/data?token=eyJ...                 │           │
│  │     → Same risks as Basic Auth in URL (logged, cached, Referer leak)      │           │
│  │     → FIX: Always use Authorization: Bearer header                        │           │
│  │                                                                               │           │
│  │  5. NOT VALIDATING EXPIRATION                                                │           │
│  │     Using the token without checking exp claim                             │           │
│  │     → FIX: Always validate exp (jjwt does this automatically)             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.11 Complete Sequence Diagram — JWT Login + Authenticated Request

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  COMPLETE SEQUENCE DIAGRAM — JWT AUTHENTICATION FLOW                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── PHASE 1: LOGIN (Token Generation) ───────────────────────────────────────               │
│                                                                                              │
│  Client        AuthController    AuthManager    DaoAuthProvider    JwtService                │
│  ──────        ──────────────    ───────────    ────────────────    ──────────                │
│    │                │                │                │                │                      │
│    │  POST /auth/login              │                │                │                      │
│    │  {username,password}           │                │                │                      │
│    │───────────────>│                │                │                │                      │
│    │                │                │                │                │                      │
│    │                │ authenticate() │                │                │                      │
│    │                │───────────────>│                │                │                      │
│    │                │                │                │                │                      │
│    │                │                │  authenticate()│                │                      │
│    │                │                │───────────────>│                │                      │
│    │                │                │                │                │                      │
│    │                │                │                │ loadUserByUsername()                  │
│    │                │                │                │ → DB query                            │
│    │                │                │                │ BCrypt.matches()                      │
│    │                │                │                │ → ✅ Valid                             │
│    │                │                │                │                │                      │
│    │                │                │  Authenticated │                │                      │
│    │                │                │  Token returned│                │                      │
│    │                │                │<───────────────│                │                      │
│    │                │                │                │                │                      │
│    │                │  generateAccessToken()          │                │                      │
│    │                │────────────────────────────────────────────────>│                      │
│    │                │                │                │                │                      │
│    │                │                │                │  Build JWT:    │                      │
│    │                │                │                │  {sub:"john",  │                      │
│    │                │                │                │   roles:[...], │                      │
│    │                │                │                │   exp:...}     │                      │
│    │                │                │                │  Sign with key │                      │
│    │                │                │                │                │                      │
│    │                │  "eyJhbG..."   │                │                │                      │
│    │                │<────────────────────────────────────────────────│                      │
│    │                │                │                │                │                      │
│    │  200 OK        │                │                │                │                      │
│    │  {accessToken, │                │                │                │                      │
│    │   refreshToken}│                │                │                │                      │
│    │<───────────────│                │                │                │                      │
│    │                │                │                │                │                      │
│    │  Store token   │                │                │                │                      │
│    │  in memory/    │                │                │                │                      │
│    │  localStorage  │                │                │                │                      │
│                                                                                              │
│                                                                                              │
│  ── PHASE 2: AUTHENTICATED REQUEST (Token Validation) ───────────────────────               │
│                                                                                              │
│  Client        JwtAuthFilter    JwtService    SecurityCtxHolder  AuthzFilter  Controller    │
│  ──────        ─────────────    ──────────    ─────────────────  ──────────   ──────────     │
│    │                │                │                │              │            │          │
│    │  GET /api/data │                │                │              │            │          │
│    │  Authorization:│                │                │              │            │          │
│    │  Bearer eyJ... │                │                │              │            │          │
│    │───────────────>│                │                │              │            │          │
│    │                │                │                │              │            │          │
│    │                │ Has "Bearer "? │                │              │            │          │
│    │                │ YES! Extract   │                │              │            │          │
│    │                │ token          │                │              │            │          │
│    │                │                │                │              │            │          │
│    │                │ extractUsername │                │              │            │          │
│    │                │───────────────>│                │              │            │          │
│    │                │                │ Verify sig     │              │            │          │
│    │                │                │ Check exp      │              │            │          │
│    │                │                │ Extract sub    │              │            │          │
│    │                │  "john"        │ → "john"       │              │            │          │
│    │                │<───────────────│                │              │            │          │
│    │                │                │                │              │            │          │
│    │                │ extractRoles   │                │              │            │          │
│    │                │───────────────>│                │              │            │          │
│    │                │ ["ROLE_ADMIN"] │                │              │            │          │
│    │                │<───────────────│                │              │            │          │
│    │                │                │                │              │            │          │
│    │                │ Create auth    │                │              │            │          │
│    │                │ token with     │                │              │            │          │
│    │                │ username+roles │                │              │            │          │
│    │                │                │                │              │            │          │
│    │                │ setAuthentication()             │              │            │          │
│    │                │───────────────────────────────>│              │            │          │
│    │                │                │                │              │            │          │
│    │                │ chain.doFilter()│               │              │            │          │
│    │                │─────────────────────────────────┼─────────────>│            │          │
│    │                │                │                │              │            │          │
│    │                │                │                │              │ Check      │          │
│    │                │                │                │              │ access     │          │
│    │                │                │                │              │ rules ✅   │          │
│    │                │                │                │              │───────────>│          │
│    │                │                │                │              │            │          │
│    │                │                │                │              │  Process   │          │
│    │                │                │                │              │  return    │          │
│    │                │                │                │              │  data      │          │
│    │  200 OK        │                │                │              │            │          │
│    │  [{data}]      │                │                │              │            │          │
│    │<─────────────────────────────────────────────────────────────────────────────│          │
│    │                │                │                │              │            │          │
│                                                                                              │
│  ★ NO DB LOOKUP during Phase 2! Token verified using SECRET KEY only (CPU)                 │
│  ★ User info (username, roles) extracted FROM the token itself                              │
│  ★ ANY server with the secret key can perform this validation                              │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 17.12 Summary — Section 17 Key Takeaways

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SUMMARY — SECTION 17 KEY TAKEAWAYS                                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. WHAT IS JWT                                                              │           │
│  │     • Open standard (RFC 7519) for compact, self-contained tokens           │           │
│  │     • Three parts: Header.Payload.Signature (separated by dots)             │           │
│  │     • Base64Url encoded (NOT encrypted — anyone can decode payload!)        │           │
│  │     • Digitally signed — tampering is detected via signature mismatch      │           │
│  │                                                                               │           │
│  │  2. THREE PARTS                                                              │           │
│  │     • HEADER: algorithm (HS256/RS256) + type (JWT)                          │           │
│  │     • PAYLOAD: claims (iss, sub, aud, exp, iat, jti + custom claims)       │           │
│  │     • SIGNATURE: HMAC or RSA hash of (header + payload + secret)           │           │
│  │                                                                               │           │
│  │  3. ALGORITHMS                                                               │           │
│  │     • Symmetric (HS256): same secret to sign and verify (single service)   │           │
│  │     • Asymmetric (RS256): private key signs, public key verifies           │           │
│  │       (microservices — auth server signs, API servers verify)               │           │
│  │                                                                               │           │
│  │  4. WHY STATELESS                                                            │           │
│  │     • Server stores NOTHING — no sessions, no session database             │           │
│  │     • Token itself carries all user info (self-contained)                   │           │
│  │     • Any server can validate (just needs secret/public key)               │           │
│  │     • Perfect for horizontal scaling and microservices                      │           │
│  │                                                                               │           │
│  │  5. VERIFICATION STEPS                                                       │           │
│  │     • Split token by "." → decode header → verify signature →              │           │
│  │       decode payload → check exp, iss, aud → extract user info             │           │
│  │     • Signature verification: re-compute hash of (header.payload)          │           │
│  │       with secret key and compare with received signature                   │           │
│  │                                                                               │           │
│  │  6. ADVANTAGES                                                               │           │
│  │     • Stateless, scalable, no DB lookup per request                         │           │
│  │     • Cross-domain, mobile-friendly, microservices-ready                    │           │
│  │                                                                               │           │
│  │  7. DISADVANTAGES                                                            │           │
│  │     • Cannot be revoked once issued (biggest problem!)                      │           │
│  │     • Payload not encrypted, token size larger than session cookies         │           │
│  │     • Stale data until token expires, secret key management complexity      │           │
│  │                                                                               │           │
│  │  8. BEST PRACTICES                                                           │           │
│  │     • Always set exp, use strong keys, HTTPS only                           │           │
│  │     • Validate all claims, reject "alg":"none"                             │           │
│  │     • Never put sensitive data in payload                                   │           │
│  │     • Use RS256 for multi-service, implement token refresh flow            │           │
│  │     • Store securely on client (httpOnly cookie > localStorage)            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

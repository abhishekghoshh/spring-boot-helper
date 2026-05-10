### 20. ★ OAuth 2.0 & OpenID Connect (OIDC) — Complete Guide

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ OAUTH 2.0 & OPENID CONNECT (OIDC) — COMPLETE GUIDE                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  OAuth 2.0 and OIDC are the foundation of modern authentication and                         │
│  authorization on the internet. Every "Sign in with Google/GitHub/Facebook"                  │
│  button you see uses these protocols.                                                        │
│                                                                                              │
│  This section covers:                                                                        │
│  • What is OAuth 2.0 and how it works (with diagrams)                                       │
│  • The 4 actors in OAuth 2.0                                                                 │
│  • All grant types and where each is used                                                    │
│  • Advantages and disadvantages                                                              │
│  • What is OIDC and how it extends OAuth 2.0                                                 │
│  • OAuth 2.0 vs OIDC — key differences                                                      │
│  • Spring Security implementation code                                                       │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.1 ★ What is OAuth 2.0? — The Problem It Solves

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHAT IS OAuth 2.0? — THE PROBLEM IT SOLVES                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── FULL FORM ───────────────────────────────────────────────────────────────               │
│                                                                                              │
│  OAuth = Open Authorization                                                                  │
│  OAuth 2.0 = Open Authorization 2.0 (second version, not backward-compatible)               │
│                                                                                              │
│  ★ It is an AUTHORIZATION framework, NOT authentication.                                    │
│  ★ It answers: "What is this app ALLOWED to do?" NOT "Who is this user?"                    │
│                                                                                              │
│                                                                                              │
│  ── THE PROBLEM (Without OAuth) ─────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Imagine you have a photo printing app (PrintMyPhotos.com) that needs        │           │
│  │  to access your Google Photos to print them.                                  │           │
│  │                                                                               │           │
│  │  WITHOUT OAuth (the old, dangerous way):                                     │           │
│  │                                                                               │           │
│  │  PrintMyPhotos: "Give me your Gmail username and password so I can          │           │
│  │                  access your Google Photos"                                   │           │
│  │                                                                               │           │
│  │  ❌ Problems:                                                                 │           │
│  │  • You give your FULL Google password to a third-party app!                 │           │
│  │  • PrintMyPhotos can now read your emails, delete files, send emails       │           │
│  │  • You can't limit what PrintMyPhotos can do (it has your full password)   │           │
│  │  • The only way to revoke access is to CHANGE your Google password          │           │
│  │  • If PrintMyPhotos is hacked, your Google account is compromised          │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  WITH OAuth 2.0 (the secure way):                                            │           │
│  │                                                                               │           │
│  │  PrintMyPhotos: "Click this button to authorize me to READ your photos"     │           │
│  │  → You are redirected to Google's login page (you NEVER give your          │           │
│  │    password to PrintMyPhotos!)                                               │           │
│  │  → Google asks: "PrintMyPhotos wants to READ your photos. Allow?"          │           │
│  │  → You click "Allow"                                                        │           │
│  │  → Google gives PrintMyPhotos a LIMITED access token                        │           │
│  │  → PrintMyPhotos can ONLY read photos (not emails, not files)              │           │
│  │  → You can REVOKE access anytime from Google settings                      │           │
│  │                                                                               │           │
│  │  ✅ Benefits:                                                                 │           │
│  │  • Your password NEVER leaves Google                                        │           │
│  │  • PrintMyPhotos gets LIMITED access (only what you approved)              │           │
│  │  • You can revoke access without changing your password                    │           │
│  │  • If PrintMyPhotos is hacked, attacker only gets limited access           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.2 ★ The 4 Actors (Roles) in OAuth 2.0

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ THE 4 ACTORS (ROLES) IN OAuth 2.0                                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  1. RESOURCE OWNER (The User)                                   │       │           │
│  │  │  ─────────────────────────────                                  │       │           │
│  │  │  • The person who OWNS the data/resource                       │       │           │
│  │  │  • Example: YOU — the person who owns the Gmail account        │       │           │
│  │  │  • You authorize the client app to access YOUR resources       │       │           │
│  │  │  • You decide WHAT the client can access (scopes)              │       │           │
│  │  │  • You can REVOKE access at any time                           │       │           │
│  │  │                                                                  │       │           │
│  │  │  Real-world: John who has a Google account with photos, emails  │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  2. CLIENT (The Application)                                    │       │           │
│  │  │  ───────────────────────────                                    │       │           │
│  │  │  • The third-party application that WANTS to access the data   │       │           │
│  │  │  • Example: PrintMyPhotos.com, Canva, Slack, etc.              │       │           │
│  │  │  • It does NOT have direct access to the user's resources      │       │           │
│  │  │  • It must get PERMISSION from the resource owner first        │       │           │
│  │  │  • It receives an ACCESS TOKEN (not the user's password!)      │       │           │
│  │  │  • It uses the access token to call the Resource Server APIs   │       │           │
│  │  │                                                                  │       │           │
│  │  │  The client registers with the Authorization Server and gets:  │       │           │
│  │  │  • client_id — public identifier for the app                   │       │           │
│  │  │  • client_secret — secret known only to the app and auth server│       │           │
│  │  │  • redirect_uri — URL to send the user back to after auth      │       │           │
│  │  │                                                                  │       │           │
│  │  │  Real-world: PrintMyPhotos.com that wants to read your photos  │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  3. AUTHORIZATION SERVER (The Gatekeeper)                       │       │           │
│  │  │  ────────────────────────────────────────                       │       │           │
│  │  │  • The server that AUTHENTICATES the user and ISSUES tokens    │       │           │
│  │  │  • Example: Google's OAuth server (accounts.google.com)        │       │           │
│  │  │  • Shows the login page to the user                            │       │           │
│  │  │  • Shows the consent screen ("Allow PrintMyPhotos to...?")     │       │           │
│  │  │  • Issues Authorization Code, Access Token, Refresh Token      │       │           │
│  │  │  • Validates client_id and client_secret                       │       │           │
│  │  │                                                                  │       │           │
│  │  │  Endpoints:                                                     │       │           │
│  │  │  • /authorize — shows login + consent screen                   │       │           │
│  │  │  • /token — exchanges auth code for tokens                     │       │           │
│  │  │  • /revoke — revokes tokens                                    │       │           │
│  │  │  • /.well-known/openid-configuration — discovery document      │       │           │
│  │  │                                                                  │       │           │
│  │  │  Real-world: Google (accounts.google.com),                      │       │           │
│  │  │  GitHub (github.com/login/oauth), Keycloak, Auth0, Okta       │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐       │           │
│  │  │                                                                  │       │           │
│  │  │  4. RESOURCE SERVER (The API Server)                            │       │           │
│  │  │  ───────────────────────────────────                            │       │           │
│  │  │  • The server that HOSTS the protected resources (data/APIs)   │       │           │
│  │  │  • Example: Google Photos API, Gmail API, Google Drive API     │       │           │
│  │  │  • Validates the access token on each request                  │       │           │
│  │  │  • Returns data only if token is valid and has required scopes │       │           │
│  │  │  • Does NOT authenticate the user — that's the auth server's   │       │           │
│  │  │    job. It only AUTHORIZES based on the access token.          │       │           │
│  │  │                                                                  │       │           │
│  │  │  ★ The Authorization Server and Resource Server can be the     │       │           │
│  │  │    SAME server (e.g., Google handles both) or DIFFERENT         │       │           │
│  │  │    servers (common in microservices)                             │       │           │
│  │  │                                                                  │       │           │
│  │  │  Real-world: Google Photos API (photoslibrary.googleapis.com)  │       │           │
│  │  │                                                                  │       │           │
│  │  └──────────────────────────────────────────────────────────────────┘       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── REAL-WORLD EXAMPLE: Gmail OAuth ─────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Scenario: Slack wants to access your Gmail contacts to invite them         │           │
│  │                                                                               │           │
│  │  Resource Owner:        YOU (John, the Gmail account holder)                │           │
│  │  Client:                Slack (the app that wants your contacts)            │           │
│  │  Authorization Server:  Google (accounts.google.com)                        │           │
│  │  Resource Server:       Gmail API (gmail.googleapis.com)                    │           │
│  │                                                                               │           │
│  │  ┌─────────┐     ┌─────────┐     ┌────────────────┐     ┌──────────────┐  │           │
│  │  │  YOU     │     │  SLACK  │     │  GOOGLE AUTH   │     │  GMAIL API   │  │           │
│  │  │ (Resource│     │ (Client)│     │  SERVER        │     │  (Resource   │  │           │
│  │  │  Owner)  │     │         │     │ (Authorization │     │   Server)    │  │           │
│  │  │          │     │         │     │  Server)       │     │              │  │           │
│  │  └────┬─────┘     └────┬────┘     └───────┬────────┘     └──────┬───────┘  │           │
│  │       │                │                  │                     │          │           │
│  │       │  1. Click      │                  │                     │          │           │
│  │       │  "Connect      │                  │                     │          │           │
│  │       │   Gmail"       │                  │                     │          │           │
│  │       │───────────────>│                  │                     │          │           │
│  │       │                │                  │                     │          │           │
│  │       │                │  2. Redirect to  │                     │          │           │
│  │       │                │  Google login     │                     │          │           │
│  │       │<───────────────│──────────────────>│                     │          │           │
│  │       │                │                  │                     │          │           │
│  │       │  3. Login to Google               │                     │          │           │
│  │       │  (your password goes ONLY to      │                     │          │           │
│  │       │   Google, NOT to Slack!)           │                     │          │           │
│  │       │──────────────────────────────────>│                     │          │           │
│  │       │                │                  │                     │          │           │
│  │       │  4. Google shows consent screen:  │                     │          │           │
│  │       │  "Slack wants to READ contacts.   │                     │          │           │
│  │       │   Allow?"                         │                     │          │           │
│  │       │<──────────────────────────────────│                     │          │           │
│  │       │                │                  │                     │          │           │
│  │       │  5. You click  │                  │                     │          │           │
│  │       │  "Allow" ✅    │                  │                     │          │           │
│  │       │──────────────────────────────────>│                     │          │           │
│  │       │                │                  │                     │          │           │
│  │       │                │  6. Google sends  │                     │          │           │
│  │       │                │  authorization    │                     │          │           │
│  │       │                │  code to Slack     │                     │          │           │
│  │       │                │<─────────────────│                     │          │           │
│  │       │                │                  │                     │          │           │
│  │       │                │  7. Slack exchanges│                    │          │           │
│  │       │                │  code for tokens  │                     │          │           │
│  │       │                │  (server-to-server)│                    │          │           │
│  │       │                │─────────────────>│                     │          │           │
│  │       │                │                  │                     │          │           │
│  │       │                │  8. Google returns │                    │          │           │
│  │       │                │  access_token     │                     │          │           │
│  │       │                │<─────────────────│                     │          │           │
│  │       │                │                  │                     │          │           │
│  │       │                │  9. Slack uses access_token             │          │           │
│  │       │                │  to GET /contacts from Gmail API       │          │           │
│  │       │                │───────────────────────────────────────>│          │           │
│  │       │                │                  │                     │          │           │
│  │       │                │  10. Gmail API validates token         │          │           │
│  │       │                │  and returns contacts                  │          │           │
│  │       │                │<───────────────────────────────────────│          │           │
│  │       │                │                  │                     │          │           │
│  │       │  11. Slack shows your contacts    │                     │          │           │
│  │       │<───────────────│                  │                     │          │           │
│  │       │                │                  │                     │          │           │
│  │                                                                               │           │
│  │  ★ Slack NEVER saw your Google password!                                     │           │
│  │  ★ Slack can ONLY read contacts (not emails, not drive files)               │           │
│  │  ★ You can revoke Slack's access from Google account settings               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.3 ★ OAuth 2.0 Authorization Code Flow — The Most Common Flow

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ AUTHORIZATION CODE FLOW — THE MOST COMMON AND SECURE OAuth 2.0 FLOW                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  This is the RECOMMENDED flow for server-side web applications.                             │
│  It uses a two-step process: first get an authorization code, then                          │
│  exchange it for an access token (server-to-server, more secure).                           │
│                                                                                              │
│                                                                                              │
│  ── STEP-BY-STEP FLOW ──────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  User          Client App           Authorization          Resource           │           │
│  │  (Browser)     (Your Server)        Server (Google)        Server (API)       │           │
│  │  ─────────     ─────────────        ───────────────        ────────────       │           │
│  │     │               │                     │                     │             │           │
│  │     │  1. Click     │                     │                     │             │           │
│  │     │  "Login with  │                     │                     │             │           │
│  │     │   Google"     │                     │                     │             │           │
│  │     │──────────────>│                     │                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  2. Redirect to Authorization Server                     │             │           │
│  │     │<──────────────│                     │                     │             │           │
│  │     │  302 Redirect to:                   │                     │             │           │
│  │     │  https://accounts.google.com/o/oauth2/v2/auth             │             │           │
│  │     │  ?response_type=code                │                     │             │           │
│  │     │  &client_id=abc123                  │                     │             │           │
│  │     │  &redirect_uri=https://myapp.com/callback                │             │           │
│  │     │  &scope=openid+email+profile        │                     │             │           │
│  │     │  &state=random_csrf_string          │                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  3. Browser navigates to Google Login                     │             │           │
│  │     │─────────────────────────────────────>│                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  4. Google shows login page         │                     │             │           │
│  │     │<─────────────────────────────────────│                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  5. User enters credentials          │                     │             │           │
│  │     │  (password goes to Google ONLY!)     │                     │             │           │
│  │     │─────────────────────────────────────>│                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  6. Google shows CONSENT screen:    │                     │             │           │
│  │     │  "MyApp wants to:                   │                     │             │           │
│  │     │   ✓ View your email                 │                     │             │           │
│  │     │   ✓ View your profile               │                     │             │           │
│  │     │   Allow / Deny"                     │                     │             │           │
│  │     │<─────────────────────────────────────│                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  7. User clicks "Allow"             │                     │             │           │
│  │     │─────────────────────────────────────>│                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  8. Google redirects back to client  │                     │             │           │
│  │     │  with AUTHORIZATION CODE             │                     │             │           │
│  │     │<─────────────────────────────────────│                     │             │           │
│  │     │  302 Redirect to:                   │                     │             │           │
│  │     │  https://myapp.com/callback          │                     │             │           │
│  │     │  ?code=AUTH_CODE_XYZ                │                     │             │           │
│  │     │  &state=random_csrf_string          │                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  9. Browser hits the callback URL                         │             │           │
│  │     │──────────────>│                     │                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │               │  10. Server exchanges                     │             │           │
│  │     │               │  authorization code for tokens            │             │           │
│  │     │               │  POST /token                              │             │           │
│  │     │               │  {                   │                     │             │           │
│  │     │               │   grant_type:        │                     │             │           │
│  │     │               │    authorization_code│                     │             │           │
│  │     │               │   code: AUTH_CODE_XYZ│                     │             │           │
│  │     │               │   client_id: abc123  │                     │             │           │
│  │     │               │   client_secret: s3cr│                     │             │           │
│  │     │               │   redirect_uri: ...  │                     │             │           │
│  │     │               │  }                   │                     │             │           │
│  │     │               │─────────────────────>│                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │               │  11. Auth server returns TOKENS           │             │           │
│  │     │               │  {                   │                     │             │           │
│  │     │               │   access_token: eyJ..│                     │             │           │
│  │     │               │   refresh_token: eyJ.│                     │             │           │
│  │     │               │   token_type: Bearer │                     │             │           │
│  │     │               │   expires_in: 3600   │                     │             │           │
│  │     │               │  }                   │                     │             │           │
│  │     │               │<─────────────────────│                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │               │  12. Use access_token to call API         │             │           │
│  │     │               │  GET /api/photos                          │             │           │
│  │     │               │  Authorization: Bearer eyJ...             │             │           │
│  │     │               │──────────────────────────────────────────>│             │           │
│  │     │               │                     │                     │             │           │
│  │     │               │  13. API returns data                     │             │           │
│  │     │               │<──────────────────────────────────────────│             │           │
│  │     │               │                     │                     │             │           │
│  │     │  14. Show data│                     │                     │             │           │
│  │     │<──────────────│                     │                     │             │           │
│  │     │               │                     │                     │             │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── WHY TWO STEPS? (Code → Token) ──────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ★ The authorization code is returned via the BROWSER (front-channel)       │           │
│  │    → It appears in the URL: /callback?code=AUTH_CODE_XYZ                    │           │
│  │    → URLs can be logged, cached, visible in browser history                 │           │
│  │    → NOT safe to put access tokens in URLs!                                 │           │
│  │                                                                               │           │
│  │  ★ The token exchange happens SERVER-TO-SERVER (back-channel)               │           │
│  │    → Client server sends POST /token with client_secret                     │           │
│  │    → Access token is returned in the HTTP response body                     │           │
│  │    → NEVER exposed to the browser or URL!                                   │           │
│  │    → client_secret proves the request is from the real client app           │           │
│  │                                                                               │           │
│  │  ★ Authorization code is SHORT-LIVED (10 seconds to few minutes)            │           │
│  │    → Even if intercepted, it's useless without the client_secret            │           │
│  │    → Can only be used ONCE                                                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── KEY URL PARAMETERS EXPLAINED ────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── Authorization Request (/authorize) ──                                    │           │
│  │                                                                               │           │
│  │  response_type=code       → "I want an authorization code"                  │           │
│  │  client_id=abc123         → "I am this registered application"              │           │
│  │  redirect_uri=...         → "Send the code to this URL"                     │           │
│  │  scope=email+photos       → "I want access to email and photos"             │           │
│  │  state=random_string      → CSRF protection (verified on callback)          │           │
│  │                                                                               │           │
│  │  ── Token Request (/token) ──                                                │           │
│  │                                                                               │           │
│  │  grant_type=authorization_code  → "I'm exchanging a code for a token"      │           │
│  │  code=AUTH_CODE_XYZ             → "Here is the authorization code"          │           │
│  │  client_id=abc123               → "I am this registered application"        │           │
│  │  client_secret=s3cret           → "Prove I'm the real client"              │           │
│  │  redirect_uri=...               → Must match the original redirect_uri     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.4 ★ All OAuth 2.0 Grant Types — When to Use Each

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ALL OAuth 2.0 GRANT TYPES — WHEN TO USE EACH                                             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────┐      │           │
│  │  │                                                                    │      │           │
│  │  │  1. AUTHORIZATION CODE GRANT (Most Common & Secure)               │      │           │
│  │  │  ──────────────────────────────────────────────────               │      │           │
│  │  │                                                                    │      │           │
│  │  │  Flow:  User → Browser → Auth Server → Code → Server → Token    │      │           │
│  │  │  Use:   Server-side web applications (Spring Boot, Django, etc.)  │      │           │
│  │  │  Why:   Server can securely store client_secret                   │      │           │
│  │  │         Token exchange happens server-to-server (back-channel)   │      │           │
│  │  │  Token: Access Token + Refresh Token                              │      │           │
│  │  │                                                                    │      │           │
│  │  │  ★ RECOMMENDED for most applications!                            │      │           │
│  │  │  ★ Most secure because tokens are never exposed to the browser   │      │           │
│  │  │                                                                    │      │           │
│  │  │  Example: "Login with Google" in a Spring Boot application        │      │           │
│  │  │                                                                    │      │           │
│  │  └────────────────────────────────────────────────────────────────────┘      │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────┐      │           │
│  │  │                                                                    │      │           │
│  │  │  2. AUTHORIZATION CODE + PKCE (Proof Key for Code Exchange)       │      │           │
│  │  │  ──────────────────────────────────────────────────────────       │      │           │
│  │  │                                                                    │      │           │
│  │  │  Flow:  Same as Auth Code, but adds code_verifier + code_challenge│      │           │
│  │  │  Use:   Single-Page Apps (React, Angular), Mobile Apps            │      │           │
│  │  │  Why:   SPAs and mobile apps CANNOT securely store client_secret  │      │           │
│  │  │         PKCE replaces client_secret with a dynamic challenge     │      │           │
│  │  │  Token: Access Token + Refresh Token                              │      │           │
│  │  │                                                                    │      │           │
│  │  │  How PKCE works:                                                  │      │           │
│  │  │  1. Client generates random code_verifier                        │      │           │
│  │  │  2. Client hashes it → code_challenge = SHA256(code_verifier)    │      │           │
│  │  │  3. Sends code_challenge to /authorize                            │      │           │
│  │  │  4. Auth server stores code_challenge                             │      │           │
│  │  │  5. Client sends code_verifier to /token                          │      │           │
│  │  │  6. Auth server: SHA256(code_verifier) == stored code_challenge? │      │           │
│  │  │  7. If match → issue tokens                                       │      │           │
│  │  │                                                                    │      │           │
│  │  │  ★ RECOMMENDED for SPAs and mobile apps!                         │      │           │
│  │  │  ★ Prevents authorization code interception attacks              │      │           │
│  │  │                                                                    │      │           │
│  │  └────────────────────────────────────────────────────────────────────┘      │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────┐      │           │
│  │  │                                                                    │      │           │
│  │  │  3. CLIENT CREDENTIALS GRANT (Machine-to-Machine)                 │      │           │
│  │  │  ────────────────────────────────────────────────                 │      │           │
│  │  │                                                                    │      │           │
│  │  │  Flow:  Client → Auth Server → Token (NO user involved!)         │      │           │
│  │  │  Use:   Service-to-service communication (microservices)          │      │           │
│  │  │         Cron jobs, batch processes, daemon services               │      │           │
│  │  │  Why:   No human user — the CLIENT itself is the resource owner  │      │           │
│  │  │  Token: Access Token ONLY (no refresh token, no user context)    │      │           │
│  │  │                                                                    │      │           │
│  │  │  POST /token                                                      │      │           │
│  │  │  {                                                                │      │           │
│  │  │    grant_type: client_credentials,                                │      │           │
│  │  │    client_id: service-a,                                          │      │           │
│  │  │    client_secret: s3cret,                                         │      │           │
│  │  │    scope: read:orders                                             │      │           │
│  │  │  }                                                                │      │           │
│  │  │                                                                    │      │           │
│  │  │  ★ Simplest flow — just client_id + client_secret → token        │      │           │
│  │  │  ★ No browser, no user, no consent screen                        │      │           │
│  │  │                                                                    │      │           │
│  │  │  Example: Order Service calling Payment Service in microservices │      │           │
│  │  │                                                                    │      │           │
│  │  └────────────────────────────────────────────────────────────────────┘      │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────┐      │           │
│  │  │                                                                    │      │           │
│  │  │  4. RESOURCE OWNER PASSWORD CREDENTIALS (ROPC) ⚠️ DEPRECATED     │      │           │
│  │  │  ────────────────────────────────────────────────────────────     │      │           │
│  │  │                                                                    │      │           │
│  │  │  Flow:  User gives username+password DIRECTLY to client app      │      │           │
│  │  │         Client sends them to auth server → gets token            │      │           │
│  │  │  Use:   Legacy apps, trusted first-party apps ONLY               │      │           │
│  │  │  Why:   ⚠️ User's password is exposed to the client app!         │      │           │
│  │  │         Defeats the purpose of OAuth!                             │      │           │
│  │  │  Token: Access Token + Refresh Token                              │      │           │
│  │  │                                                                    │      │           │
│  │  │  POST /token                                                      │      │           │
│  │  │  {                                                                │      │           │
│  │  │    grant_type: password,                                          │      │           │
│  │  │    username: john,                                                │      │           │
│  │  │    password: secret123,                                           │      │           │
│  │  │    client_id: my-app                                              │      │           │
│  │  │  }                                                                │      │           │
│  │  │                                                                    │      │           │
│  │  │  ❌ DEPRECATED in OAuth 2.1 — do NOT use in new applications!    │      │           │
│  │  │  ❌ User's password is given to the client (violates OAuth goal) │      │           │
│  │  │                                                                    │      │           │
│  │  └────────────────────────────────────────────────────────────────────┘      │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────┐      │           │
│  │  │                                                                    │      │           │
│  │  │  5. IMPLICIT GRANT ❌ DEPRECATED                                  │      │           │
│  │  │  ──────────────────────────────                                   │      │           │
│  │  │                                                                    │      │           │
│  │  │  Flow:  Access token returned DIRECTLY in URL fragment            │      │           │
│  │  │         (no authorization code, no server-to-server exchange)     │      │           │
│  │  │  Use:   Was designed for SPAs (before PKCE existed)               │      │           │
│  │  │  Why:   ❌ Token exposed in URL (can be logged, leaked, stolen)   │      │           │
│  │  │         ❌ No refresh token                                        │      │           │
│  │  │         ❌ No client authentication                                │      │           │
│  │  │  Token: Access Token ONLY (in URL fragment!)                      │      │           │
│  │  │                                                                    │      │           │
│  │  │  ❌ REMOVED in OAuth 2.1 — replaced by Auth Code + PKCE          │      │           │
│  │  │  ❌ NEVER use this in new applications!                           │      │           │
│  │  │                                                                    │      │           │
│  │  └────────────────────────────────────────────────────────────────────┘      │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────────┐      │           │
│  │  │                                                                    │      │           │
│  │  │  6. DEVICE AUTHORIZATION GRANTode Flow)                 │      │           │
│  │  │  ────────────────────────────────────────────────                 │      │           │
│  │  │                                                                    │      │           │
│  │  │  Flow:  Device shows code → User enters code on another device   │      │           │
│  │  │  Use:   Smart TVs, IoT devices, CLI tools                        │      │           │
│  │  │  Why:   Device has no browser or limited input capability        │      │           │
│  │  │  Token: Access Token + Refresh Token                              │      │           │
│  │  │                                                                    │      │           │
│  │  │  Example: Netflix on Smart TV says:                               │      │           │
│  │  │  "Go to netflix.com/activate and enter code: ABCD-1234"          │      │           │
│  │  │  You open your phone browser, login, enter the code → TV is     │      │           │
│  │  │  now authenticated.                                               │      │           │
│  │  │                                                                    │      │           │
│  │  └────────────────────────────────────────────────────────────────────┘      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── SUMMARY TABLE ───────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────┬──────────────────────┬────────────────┬────────────┐              │
│  │  Grant Type           │  Best For             │  User Involved │  Status    │              │
│  ├──────────────────────┼──────────────────────┼────────────────┼────────────┤              │
│  │  Authorization Code   │  Server-side web apps │  Yes           │  ✅ Use     │              │
│  ├──────────────────────┼──────────────────────┼────────────────┼────────────┤              │
│  │  Auth Code + PKCE     │  SPAs, mobile apps    │  Yes           │  ✅ Use     │              │
│  ├──────────────────────┼──────────────────────┼────────────────┼────────────┤              │
│  │  Client Credentials   │  Machine-to-machine   │  No            │  ✅ Use     │              │
│  ├──────────────────────┼──────────────────────┼────────────────┼────────────┤              │
│  │  Device Code          │  Smart TV, IoT, CLI   │  Yes (2nd dev) │  ✅ Use     │              │
│  ├──────────────────────┼──────────────────────┼────────────────┼────────────┤              │
│  │  ROPC (Password)      │  Legacy/first-party   │  Yes           │  ⚠️ Avoid  │              │
│  ├──────────────────────┼──────────────────────┼────────────────┼────────────┤              │
│  │  Implicit             │  Was for SPAs          │  Yes           │  ❌ Removed │              │
│  └──────────────────────┴──────────────────────┴────────────────┴────────────┘              │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.5 ★ Advantages and Disadvantages of OAuth 2.0

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ADVANTAGES AND DISADVANTAGES OF OAuth 2.0                                                 │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── ADVANTAGES ──────────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ✅ NO PASSWORD SHARING                                                      │           │
│  │     User's password NEVER leaves the authorization server                   │           │
│  │     Third-party apps never see the user's credentials                       │           │
│  │                                                                               │           │
│  │  ✅ GRANULAR PERMISSIONS (Scopes)                                            │           │
│  │     User can grant limited access (e.g., "read photos" only)               │           │
│  │     App can't access anything beyond the approved scopes                   │           │
│  │                                                                               │           │
│  │  ✅ REVOCABLE ACCESS                                                         │           │
│  │     User can revoke a specific app's access without changing password       │           │
│  │     Go to Google Settings → Security → Third-party apps → Remove           │           │
│  │                                                                               │           │
│  │  ✅ SEPARATION OF CONCERNS                                                   │           │
│  │     Authentication (who are you?) → Authorization Server handles it        │           │
│  │     Authorization (what can you do?) → Resource Server handles it          │           │
│  │     Your app doesn't need to handle user registration/passwords            │           │
│  │                                                                               │           │
│  │  ✅ STANDARDIZED PROTOCOL                                                    │           │
│  │     Same flow works with Google, GitHub, Facebook, Microsoft, etc.         │           │
│  │     Libraries exist for every language/framework                            │           │
│  │     Spring Security has built-in OAuth 2.0 client and resource server      │           │
│  │                                                                               │           │
│  │  ✅ TOKEN-BASED (Stateless possible)                                         │           │
│  │     Access tokens can be JWT (self-contained, no session needed)            │           │
│  │     Scales well in microservices architecture                               │           │
│  │                                                                               │           │
│  │  ✅ SINGLE SIGN-ON (SSO) ENABLED                                            │           │
│  │     Login once with Google → access multiple apps                          │           │
│  │     User doesn't need separate accounts for every app                      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── DISADVANTAGES ───────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ❌ COMPLEXITY                                                                │           │
│  │     Multiple grant types, flows, token types, scopes                        │           │
│  │     Many ways to implement it wrong (security vulnerabilities)             │           │
│  │     Requires understanding of redirects, codes, tokens, etc.               │           │
│  │                                                                               │           │
│  │  ❌ DEPENDENCY ON THIRD-PARTY                                                │           │
│  │     If Google/GitHub's auth server goes down, YOUR users can't login       │           │
│  │     You depend on the availability of the authorization server             │           │
│  │     The provider can change their API, deprecate endpoints, etc.           │           │
│  │                                                                               │           │
│  │  ❌ TOKEN MANAGEMENT OVERHEAD                                                │           │
│  │     Must handle token refresh, expiration, revocation                      │           │
│  │     Must securely store tokens on client and server                        │           │
│  │     Token leakage is a security risk                                        │           │
│  │                                                                               │           │
│  │  ❌ NOT AUTHENTICATION (OAuth 2.0 alone)                                     │           │
│  │     OAuth 2.0 is AUTHORIZATION only — it tells you what an app can do      │           │
│  │     It does NOT tell you WHO the user is (no standard identity claims)      │           │
│  │     Need OIDC (OpenID Connect) on top for authentication                   │           │
│  │                                                                               │           │
│  │  ❌ PHISHING RISK                                                            │           │
│  │     Users can be tricked into authorizing malicious apps                   │           │
│  │     Fake consent screens can steal authorization codes                     │           │
│  │     Requires careful redirect_uri validation                               │           │
│  │                                                                               │           │
│  │  ❌ OVER-SCOPING RISK                                                        │           │
│  │     Apps may request more permissions than they need                        │           │
│  │     Users often click "Allow" without reading the scope list               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.6 ★ What is OIDC (OpenID Connect)? — Authentication Layer on Top of OAuth 2.0

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHAT IS OIDC (OpenID Connect)?                                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── FULL FORM ───────────────────────────────────────────────────────────────               │
│                                                                                              │
│  OIDC = OpenID Connect                                                                       │
│  It is an IDENTITY LAYER built ON TOP of OAuth 2.0.                                         │
│                                                                                              │
│  ★ OAuth 2.0 = AUTHORIZATION ("What can this app do?")                                      │
│  ★ OIDC = AUTHENTICATION ("Who is this user?") + Authorization                              │
│                                                                                              │
│                                                                                              │
│  ── THE PROBLEM OIDC SOLVES ─────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  OAuth 2.0 alone:                                                            │           │
│  │  → Gives you an ACCESS TOKEN                                                │           │
│  │  → Access token says: "This app can read photos" (authorization)            │           │
│  │  → Access token does NOT say: "The user is john@gmail.com" (identity)       │           │
│  │  → To get user info, you must make an EXTRA API call:                       │           │
│  │    GET /userinfo with the access token                                       │           │
│  │  → ❌ Not standardized — each provider has different userinfo endpoints      │           │
│  │  → ❌ Extra network call on every login                                      │           │
│  │                                                                               │           │
│  │  OIDC adds:                                                                  │           │
│  │  → An ID TOKEN alongside the access token                                   │           │
│  │  → ID token is a JWT that contains user identity claims:                    │           │
│  │    { sub: "12345", name: "John", email: "john@gmail.com", picture: "..." } │           │
│  │  → ✅ Standardized claims — same format across all OIDC providers            │           │
│  │  → ✅ No extra API call needed — user info is IN the token!                  │           │
│  │  → ✅ The client can immediately display "Hello, John!" from the ID token   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── OIDC = OAuth 2.0 + Identity Layer ───────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌────────────────────────────────────────────────────────────────┐          │           │
│  │  │                                                                │          │           │
│  │  │  ┌──────────────────────────────────────────────────────┐     │          │           │
│  │  │  │                                                      │     │          │           │
│  │  │  │           OPENID CONNECT (OIDC)                      │     │          │           │
│  │  │  │                                                      │     │          │           │
│  │  │  │  • ID Token (JWT with user identity)                │     │          │           │
│  │  │  │  • UserInfo endpoint                                 │     │          │           │
│  │  │  │  • Standard claims (sub, name, email, picture)      │     │          │           │
│  │  │  │  • Discovery document (.well-known/openid-config)   │     │          │           │
│  │  │  │  • scope: openid (required!)                        │     │          │           │
│  │  │  │                                                      │     │          │           │
│  │  │  └──────────────────────────────────────────────────────┘     │          │           │
│  │  │                                                                │          │           │
│  │  │               OAuth 2.0 (Foundation)                           │          │           │
│  │  │                                                                │          │           │
│  │  │  • Access Token                                                │          │           │
│  │  │  • Refresh Token                                               │          │           │
│  │  │  • Authorization Code Flow                                     │          │           │
│  │  │  • Client Credentials, PKCE, etc.                              │          │           │
│  │  │  • Scopes, Redirect URIs                                       │          │           │
│  │  │                                                                │          │           │
│  │  └────────────────────────────────────────────────────────────────┘          │           │
│  │                                                                               │           │
│  │  ★ OIDC is NOT a separate protocol — it IS OAuth 2.0 with extra features!  │           │
│  │  ★ Every OIDC flow IS an OAuth 2.0 flow + ID token                         │           │
│  │  ★ You can do OIDC without changing any OAuth 2.0 infrastructure!          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.7 ★ OIDC Flow — How to Get the ID Token

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ OIDC FLOW — HOW TO GET THE ID TOKEN                                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  The OIDC flow is IDENTICAL to the OAuth 2.0 Authorization Code flow,                       │
│  with ONE key difference: you add "openid" to the scope parameter.                          │
│  This tells the Authorization Server: "I also want an ID Token!"                            │
│                                                                                              │
│                                                                                              │
│  ── OIDC AUTHORIZATION CODE FLOW ────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  User          Client App           Authorization          Resource           │           │
│  │  (Browser)     (Your Server)        Server (Google)        Server (API)       │           │
│  │  ─────────     ─────────────        ───────────────        ────────────       │           │
│  │     │               │                     │                     │             │           │
│  │     │  1. Click "Login with Google"       │                     │             │           │
│  │     │──────────────>│                     │                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  2. Redirect to Google              │                     │             │           │
│  │     │<──────────────│                     │                     │             │           │
│  │     │  https://accounts.google.com/o/oauth2/v2/auth             │             │           │
│  │     │  ?response_type=code                │                     │             │           │
│  │     │  &client_id=abc123                  │                     │             │           │
│  │     │  &redirect_uri=https://myapp.com/callback                │             │           │
│  │     │  &scope=openid+email+profile        │  ★ "openid" is the key!         │           │
│  │     │  &state=random_csrf_string          │                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │  3-7. Same as OAuth 2.0 (login, consent, code)           │             │           │
│  │     │  ...                                │                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │               │  8. Exchange code for tokens              │             │           │
│  │     │               │  POST /token         │                     │             │           │
│  │     │               │  {                   │                     │             │           │
│  │     │               │   grant_type:         │                     │             │           │
│  │     │               │    authorization_code │                     │             │           │
│  │     │               │   code: AUTH_CODE     │                     │             │           │
│  │     │               │   client_id: abc123   │                     │             │           │
│  │     │               │   client_secret: s3cr │                     │             │           │
│  │     │               │  }                   │                     │             │           │
│  │     │               │─────────────────────>│                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │               │  9. Auth server returns THREE tokens!     │             │           │
│  │     │               │  {                   │                     │             │           │
│  │     │               │   access_token: eyJ.. │  ← For API calls  │             │           │
│  │     │               │   ★ id_token: eyJ..   │  ← For identity!  │             │           │
│  │     │               │   refresh_token: eyJ. │  ← For renewal    │             │           │
│  │     │               │   token_type: Bearer  │                     │             │           │
│  │     │               │   expires_in: 3600   │                     │             │           │
│  │     │               │  }                   │                     │             │           │
│  │     │               │<─────────────────────│                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │               │  10. Decode the id_token (JWT):           │             │           │
│  │     │               │  {                   │                     │             │           │
│  │     │               │   "iss": "https://accounts.google.com",   │             │           │
│  │     │               │   "sub": "110248495921238986420",          │             │           │
│  │     │               │   "aud": "abc123",   │  ← your client_id  │             │           │
│  │     │               │   "email": "john@gmail.com",              │             │           │
│  │     │               │   "name": "John Doe",│                     │             │           │
│  │     │               │   "picture": "https://...",               │             │           │
│  │     │               │   "iat": 1715184000, │                     │             │           │
│  │     │               │   "exp": 1715187600  │                     │             │           │
│  │     │               │  }                   │                     │             │           │
│  │     │               │                     │                     │             │           │
│  │     │               │  ★ Client now knows the user's identity   │             │           │
│  │     │               │  ★ WITHOUT making any extra API calls!    │             │           │
│  │     │               │  ★ Can display: "Welcome, John Doe!"      │             │           │
│  │     │               │                     │                     │             │           │
│  │     │               │  11. Use access_token for API calls       │             │           │
│  │     │               │  Authorization: Bearer <access_token>     │             │           │
│  │     │               │──────────────────────────────────────────>│             │           │
│  │     │               │                     │                     │             │           │
│  │     │  12. Show data to user              │                     │             │           │
│  │     │<──────────────│                     │                     │             │           │
│  │     │               │                     │                     │             │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── THE MAGIC: scope=openid ─────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  OAuth 2.0 only:        scope=email+photos                                  │           │
│  │  → Returns: access_token + refresh_token                                    │           │
│  │  → NO id_token!                                                              │           │
│  │                                                                               │           │
│  │  OIDC (OAuth 2.0 + openid): scope=openid+email+profile                     │           │
│  │  → Returns: access_token + refresh_token + ★ id_token                      │           │
│  │  → id_token contains user identity!                                          │           │
│  │                                                                               │           │
│  │  ★ Adding "openid" to scope is ALL you need to switch from                  │           │
│  │    OAuth 2.0 to OIDC! Everything else stays the same!                       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  Common OIDC scopes:                                                         │           │
│  │  ┌──────────────┬─────────────────────────────────────────────┐             │           │
│  │  │  Scope        │  Claims in ID Token                        │             │           │
│  │  ├──────────────┼─────────────────────────────────────────────┤             │           │
│  │  │  openid       │  sub (subject — required!)                 │             │           │
│  │  ├──────────────┼─────────────────────────────────────────────┤             │           │
│  │  │  profile      │  name, family_name, given_name, picture,   │             │           │
│  │  │              │  gender, birthdate, locale, updated_at     │             │           │
│  │  ├──────────────┼─────────────────────────────────────────────┤             │           │
│  │  │  email        │  email, email_verified                     │             │           │
│  │  ├──────────────┼─────────────────────────────────────────────┤             │           │
│  │  │  phone        │  phone_number, phone_number_verified       │             │           │
│  │  ├──────────────┼─────────────────────────────────────────────┤             │           │
│  │  │  address      │  address (JSON object)                     │             │           │
│  │  └──────────────┴─────────────────────────────────────────────┘             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.8 ★ OAuth 2.0 vs OIDC — Complete Comparison

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ OAuth 2.0 vs OIDC — COMPLETE COMPARISON                                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌───────────────────┬──────────────────────────────┬──────────────────────────────┐        │
│  │  Aspect            │  OAuth 2.0                    │  OIDC (OpenID Connect)       │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Full Form         │  Open Authorization 2.0      │  OpenID Connect 1.0          │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Purpose           │  AUTHORIZATION               │  AUTHENTICATION +            │        │
│  │                    │  "What can this app do?"      │  Authorization               │        │
│  │                    │                              │  "Who is this user?" +        │        │
│  │                    │                              │  "What can this app do?"      │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Built On          │  Standalone protocol         │  Built ON TOP of OAuth 2.0   │        │
│  │                    │                              │  (extension, not separate)    │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Tokens Generated  │  Access Token                │  Access Token                │        │
│  │                    │  Refresh Token               │  Refresh Token               │        │
│  │                    │                              │  ★ + ID Token (JWT!)         │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Token Types       │  Access Token: JWT or opaque │  Access Token: JWT or opaque │        │
│  │                    │  Refresh Token: opaque/JWT   │  Refresh Token: opaque/JWT   │        │
│  │                    │                              │  ID Token: ALWAYS JWT         │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Scope             │  Custom scopes               │  Standard scopes:            │        │
│  │                    │  (read, write, photos, etc.) │  openid (required!)          │        │
│  │                    │                              │  profile, email, phone,      │        │
│  │                    │                              │  address                     │        │
│  │                    │                              │  + custom scopes too         │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  User Identity     │  ❌ No standard way to get    │  ✅ ID Token contains user    │        │
│  │                    │  user identity               │  identity (sub, name, email) │        │
│  │                    │  Must call /userinfo API     │  Standardized claims         │        │
│  │                    │  (non-standard endpoint)     │  UserInfo endpoint also      │        │
│  │                    │                              │  available                   │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Discovery         │  ❌ No standard discovery      │  ✅ .well-known/              │        │
│  │                    │                              │  openid-configuration         │        │
│  │                    │                              │  (auto-discovery of all      │        │
│  │                    │                              │  endpoints, keys, etc.)      │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  How to Get        │  scope=photos+email          │  scope=openid+email+profile  │        │
│  │  Both Tokens       │  → access_token only         │  → access_token + id_token   │        │
│  │                    │  (no id_token!)              │  ★ Just add "openid" scope!  │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Spec Defines      │  How to get access tokens    │  Everything in OAuth 2.0 +   │        │
│  │                    │  Grant types, scopes,        │  ID Token format,            │        │
│  │                    │  refresh tokens              │  Standard claims,            │        │
│  │                    │                              │  UserInfo endpoint,          │        │
│  │                    │                              │  Discovery document,         │        │
│  │                    │                              │  Session management          │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Use Case          │  Third-party API access      │  Login / SSO / Identity      │        │
│  │                    │  "Let Slack read my Gmail"   │  "Login with Google"         │        │
│  │                    │                              │  "Who is this user?"         │        │
│  ├───────────────────┼──────────────────────────────┼──────────────────────────────┤        │
│  │  Examples          │  GitHub API access,          │  "Sign in with Google",      │        │
│  │                    │  Stripe API,                 │  "Login with Microsoft",     │        │
│  │                    │  Twitter API                 │  Keycloak SSO, Auth0 login   │        │
│  └───────────────────┴──────────────────────────────┴──────────────────────────────┘        │
│                                                                                              │
│                                                                                              │
│  ── VISUAL COMPARISON ───────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  OAuth 2.0 Response (scope=email+photos):                                    │           │
│  │  {                                                                            │           │
│  │    "access_token": "eyJhbGci...",         ← for API access                  │           │
│  │    "refresh_token": "eyJhbGci...",        ← for token renewal               │           │
│  │    "token_type": "Bearer",                                                   │           │
│  │    "expires_in": 3600                                                        │           │
│  │  }                                                                            │           │
│  │  → ❌ No user identity! Must call GET /userinfo separately!                  │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  OIDC Response (scope=openid+email+profile):                                 │           │
│  │  {                                                                            │           │
│  │    "access_token": "eyJhbGci...",         ← for API access                  │           │
│  │    "refresh_token": "eyJhbGci...",        ← for token renewal               │           │
│  │    ★ "id_token": "eyJhbGci...",           ← user identity (JWT!)            │           │
│  │    "token_type": "Bearer",                                                   │           │
│  │    "expires_in": 3600                                                        │           │
│  │  }                                                                            │           │
│  │  → ✅ Decode id_token → { name: "John", email: "john@gmail.com" }           │           │
│  │  → ✅ Instant user identity without extra API calls!                         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── ONE-LINE SUMMARY ────────────────────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  OAuth 2.0: "I authorize this app to access my photos" (WHAT)               │           │
│  │  OIDC:      "I am John Doe, and I authorize this app" (WHO + WHAT)          │           │
│  │                                                                               │           │
│  │  ★ OIDC = OAuth 2.0 + scope=openid + ID Token                              │           │
│  │  ★ That's literally the only difference!                                     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.9 ★ Spring Security OAuth 2.0 / OIDC Implementation — Code

##### 20.9.1 Spring Boot OAuth 2.0 Client — "Login with Google"

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — OAuth 2.0 / OIDC Client Configuration
#
#  ★ Spring Security supports OIDC out of the box!
#  Just add the Google/GitHub client configuration and Spring handles:
#  → Redirect to authorization server
#  → Authorization code exchange
#  → Token parsing (access + id token)
#  → User info extraction
#  → SecurityContext population
# ═══════════════════════════════════════════════════════════════════════════════

spring:
  security:
    oauth2:
      client:
        registration:
          # ── Google OIDC Login ────────────────────────────────────
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - openid      # ★ This makes it OIDC (returns id_token!)
              - email
              - profile
            # Spring knows Google's endpoints automatically!
            # No need to configure authorize-uri, token-uri, etc.

          # ── GitHub OAuth 2.0 Login ───────────────────────────────
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope:
              - read:user
              - user:email
            # ★ GitHub does NOT support OIDC — only OAuth 2.0
            # → No id_token, only access_token
            # → Spring calls /user API to get user info

        # ── Custom Authorization Server (Keycloak, Auth0, etc.) ────
        # If not using Google/GitHub, configure provider manually:
        provider:
          keycloak:
            issuer-uri: https://keycloak.myapp.com/realms/myrealm
            # ★ Spring auto-discovers all endpoints from:
            # https://keycloak.myapp.com/realms/myrealm/.well-known/openid-configuration
```

##### 20.9.2 SecurityConfig — OAuth 2.0 / OIDC

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig — OAuth 2.0 / OIDC Login Configuration
//
//  ★ Spring Security handles the ENTIRE OAuth 2.0 / OIDC flow automatically:
//  → Redirects to Google/GitHub login page
//  → Handles the callback URL (/login/oauth2/code/{provider})
//  → Exchanges authorization code for tokens
//  → Parses the ID Token (OIDC) or calls /userinfo (OAuth 2.0)
//  → Creates Authentication object and sets SecurityContext
//  → ALL without writing any filter or provider code!
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error", "/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // ★ This single line enables the ENTIRE OAuth 2.0 / OIDC flow!
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")                              // Custom login page
                .defaultSuccessUrl("/dashboard", true)            // Where to go after login
                .failureUrl("/login?error=true")                  // Where to go on failure
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())        // For OAuth 2.0 (GitHub)
                    .oidcUserService(customOidcUserService())      // For OIDC (Google)
                )
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
            );

        return http.build();
    }

    // ── Custom OAuth2 User Service (for GitHub — no OIDC) ─────────────
    // ★ Called when user logs in via OAuth 2.0 (no id_token)
    // Spring calls the /userinfo endpoint and passes the result here
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User oAuth2User = delegate.loadUser(request);
            // ★ oAuth2User contains: name, email, avatar_url, etc.
            // You can load/create user in your DB here
            // Map attributes to your User entity
            return oAuth2User;
        };
    }

    // ── Custom OIDC User Service (for Google — has id_token!) ─────────
    // ★ Called when user logs in via OIDC (id_token is available)
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> customOidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser oidcUser = delegate.loadUser(request);
            // ★ oidcUser contains:
            //   - ID Token claims: sub, name, email, picture (from id_token)
            //   - User info claims: same but from /userinfo endpoint
            //   - Both are merged automatically by Spring!
            
            String email = oidcUser.getEmail();           // From id_token
            String name = oidcUser.getFullName();          // From id_token
            String picture = oidcUser.getPicture();        // From id_token
            String subject = oidcUser.getSubject();        // Unique user ID
            
            // You can load/create user in your DB here
            // Map OIDC claims to your User entity
            
            return oidcUser;
        };
    }
}
```

##### 20.9.3 Accessing User Info in Controllers

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Accessing OAuth 2.0 / OIDC User Info in Controllers
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
public class UserController {

    // ── Method 1: OAuth2User parameter (works for both OAuth2 and OIDC) ──
    @GetMapping("/user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OAuth2User principal) {
        // ★ principal.getAttributes() returns ALL user attributes
        return principal.getAttributes();
        // Google (OIDC): { sub, name, email, picture, email_verified, ... }
        // GitHub (OAuth2): { login, id, avatar_url, name, email, ... }
    }

    // ── Method 2: OidcUser parameter (OIDC only — Google, Keycloak) ──
    @GetMapping("/oidc-user")
    public Map<String, Object> getOidcUser(@AuthenticationPrincipal OidcUser principal) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", principal.getFullName());
        userInfo.put("email", principal.getEmail());
        userInfo.put("picture", principal.getPicture());
        userInfo.put("subject", principal.getSubject());

        // ★ Access the raw ID Token (JWT)
        String idTokenValue = principal.getIdToken().getTokenValue();
        userInfo.put("idToken", idTokenValue);

        // ★ Access individual ID Token claims
        userInfo.put("issuer", principal.getIdToken().getIssuer().toString());
        userInfo.put("issuedAt", principal.getIdToken().getIssuedAt().toString());
        userInfo.put("expiresAt", principal.getIdToken().getExpiresAt().toString());

        return userInfo;
    }

    // ── Method 3: OAuth2AuthenticationToken (full authentication object) ──
    @GetMapping("/auth-details")
    public Map<String, Object> getAuthDetails(
            OAuth2AuthenticationToken authentication) {
        Map<String, Object> details = new HashMap<>();
        details.put("provider", authentication.getAuthorizedClientRegistrationId());
        // → "google" or "github"
        details.put("principal", authentication.getPrincipal().getAttributes());
        details.put("authorities", authentication.getAuthorities());
        return details;
    }
}
```

##### 20.9.4 Spring Boot OAuth 2.0 Resource Server — Validating Access Tokens

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Resource Server Configuration — Validates access tokens from external
//  authorization server (Google, Keycloak, Auth0, etc.)
//
//  ★ Use this when your Spring Boot app is an API server that receives
//  access tokens from clients that logged in elsewhere.
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("SCOPE_admin")
                .requestMatchers("/api/**").hasAuthority("SCOPE_read")
                .anyRequest().authenticated()
            )
            // ★ This configures your app as a Resource Server
            // It validates JWT access tokens automatically!
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // Option 1: JWKS URI — auto-fetch public keys for signature verification
                    .jwkSetUri("https://auth.myapp.com/.well-known/jwks.json")
                    
                    // Option 2: Issuer URI — auto-discover everything
                    // .issuerUri("https://accounts.google.com")
                )
            );

        return http.build();
    }
}
```

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — Resource Server Configuration
# ═══════════════════════════════════════════════════════════════════════════════

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Option 1: JWKS URI (explicit)
          jwk-set-uri: https://auth.myapp.com/.well-known/jwks.json

          # Option 2: Issuer URI (auto-discovery)
          # issuer-uri: https://accounts.google.com
          # ★ Spring auto-discovers JWKS URI from:
          # https://accounts.google.com/.well-known/openid-configuration
```

##### 20.9.5 Dependencies (pom.xml)

```xml
<!-- ═══════════════════════════════════════════════════════════════════════ -->
<!--  Required Dependencies for OAuth 2.0 / OIDC                          -->
<!-- ═══════════════════════════════════════════════════════════════════════ -->

<dependencies>
    <!-- Spring Boot Starter Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- ★ OAuth 2.0 CLIENT — for "Login with Google/GitHub" -->
    <!-- Handles: redirect, code exchange, token parsing, user info -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>

    <!-- ★ OAuth 2.0 RESOURCE SERVER — for validating access tokens -->
    <!-- Handles: JWT validation, signature verification, claims extraction -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

---

#### 20.10 ★ How Spring Security Handles OAuth 2.0 / OIDC Internally

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW SPRING SECURITY HANDLES OAUTH 2.0 / OIDC INTERNALLY                                 │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  When you add .oauth2Login(), Spring Security AUTO-REGISTERS:               │           │
│  │                                                                               │           │
│  │  1. OAuth2AuthorizationRequestRedirectFilter                                │           │
│  │     → Intercepts /oauth2/authorization/{provider}                           │           │
│  │     → Builds the authorization URL with client_id, scope, state, etc.      │           │
│  │     → Redirects user to Google/GitHub login page                            │           │
│  │                                                                               │           │
│  │  2. OAuth2LoginAuthenticationFilter                                          │           │
│  │     → Intercepts /login/oauth2/code/{provider} (callback URL)              │           │
│  │     → Receives the authorization code from the auth server                  │           │
│  │     → Exchanges code for tokens (POST /token — server-to-server)           │           │
│  │     → Parses access token, refresh token, and id token (if OIDC)           │           │
│  │     → Calls OAuth2UserService or OidcUserService to load user info         │           │
│  │     → Creates OAuth2AuthenticationToken with user details + authorities    │           │
│  │     → Sets SecurityContextHolder.getContext().setAuthentication(auth)       │           │
│  │                                                                               │           │
│  │  ★ All of this happens WITHOUT you writing ANY filter or provider!          │           │
│  │  ★ Spring handles the entire Authorization Code flow automatically!        │           │
│  │  ★ You only configure client-id, client-secret, and scope in YAML!         │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── FILTER CHAIN ORDER (with OAuth2 Login) ──                               │           │
│  │                                                                               │           │
│  │  Request                                                                      │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  SecurityContextHolderFilter (creates empty context)                         │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  OAuth2AuthorizationRequestRedirectFilter                                    │           │
│  │    │  → /oauth2/authorization/google → redirect to Google                   │           │
│  │    ▼                                                                          │           │
│  │  OAuth2LoginAuthenticationFilter                                             │           │
│  │    │  → /login/oauth2/code/google → exchange code → set auth               │           │
│  │    ▼                                                                          │           │
│  │  AuthorizationFilter (hasRole, hasAuthority)                                 │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  Controller (DispatcherServlet)                                               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.11 Summary — Section 20 Key Takeaways

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  SUMMARY — SECTION 20 KEY TAKEAWAYS                                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. OAuth 2.0 = AUTHORIZATION (Open Authorization)                           │           │
│  │     • Answers: "What can this app do?" NOT "Who is this user?"              │           │
│  │     • User's password NEVER leaves the authorization server                 │           │
│  │     • Third-party apps get LIMITED access tokens, not passwords             │           │
│  │                                                                               │           │
│  │  2. THE 4 ACTORS                                                             │           │
│  │     • Resource Owner: the user (you)                                        │           │
│  │     • Client: the app that wants access (Slack, PrintMyPhotos)             │           │
│  │     • Authorization Server: authenticates user, issues tokens (Google)     │           │
│  │     • Resource Server: hosts the data/APIs (Gmail API, Photos API)         │           │
│  │                                                                               │           │
│  │  3. AUTHORIZATION CODE FLOW (Most Common)                                    │           │
│  │     • Two-step: get code (browser) → exchange for token (server-to-server) │           │
│  │     • Code in URL is safe (useless without client_secret)                  │           │
│  │     • Token never exposed to the browser                                    │           │
│  │                                                                               │           │
│  │  4. GRANT TYPES                                                               │           │
│  │     • Auth Code: server-side web apps (most secure) ✅                      │           │
│  │     • Auth Code + PKCE: SPAs, mobile apps ✅                                │           │
│  │     • Client Credentials: machine-to-machine, no user ✅                    │           │
│  │     • Device Code: smart TVs, IoT ✅                                        │           │
│  │     • ROPC (Password): ⚠️ deprecated                                       │           │
│  │     • Implicit: ❌ removed in OAuth 2.1                                     │           │
│  │                                                                               │           │
│  │  5. OIDC = OAuth 2.0 + Identity Layer                                        │           │
│  │     • Full form: OpenID Connect                                              │           │
│  │     • Adds AUTHENTICATION on top of OAuth 2.0's authorization              │           │
│  │     • Returns an ID Token (JWT) with user identity claims                  │           │
│  │     • Triggered by adding "openid" to the scope parameter                  │           │
│  │                                                                               │           │
│  │  6. KEY DIFFERENCE: OAuth 2.0 vs OIDC                                        │           │
│  │     • OAuth 2.0: access_token + refresh_token (no identity)                │           │
│  │     • OIDC: access_token + refresh_token + ★ id_token (identity!)          │           │
│  │     • OIDC = OAuth 2.0 + scope=openid → that's the ONLY change!           │           │
│  │                                                                               │           │
│  │  7. SPRING SECURITY SUPPORT                                                   │           │
│  │     • spring-boot-starter-oauth2-client → "Login with Google/GitHub"       │           │
│  │     • spring-boot-starter-oauth2-resource-server → validate access tokens  │           │
│  │     • .oauth2Login() → enables entire OAuth 2.0 / OIDC flow automatically  │           │
│  │     • Only need client-id, client-secret, scope in application.yml!        │           │
│  │     • Spring auto-handles: redirect, code exchange, token parsing,         │           │
│  │       user info extraction, SecurityContext population                     │           │
│  │                                                                               │           │
│  │  8. ADVANTAGES vs DISADVANTAGES                                               │           │
│  │     • ✅ No password sharing, granular scopes, revocable, standardized      │           │
│  │     • ❌ Complex, third-party dependency, not authentication (need OIDC)    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```



---

#### 20.12 ★ Authorization Code Grant Flow with Spring Security — Step-by-Step Configuration & Internals

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ AUTHORIZATION CODE GRANT FLOW WITH SPRING SECURITY                                       │
│    — COMPLETE STEP-BY-STEP CONFIGURATION & INTERNAL WORKING                                 │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  This section explains how to configure Spring Security OAuth 2.0 / OIDC                    │
│  Authorization Code Grant flow from SCRATCH — from app registration to the                  │
│  final JSESSIONID cookie, covering every internal class that participates.                   │
│                                                                                              │
│  ★ Key Insight: Most of the code for popular vendors (Google, GitHub, GitLab,               │
│    Auth0) is ALREADY written inside the spring-boot-starter-oauth2-client                   │
│    dependency. We just need:                                                                 │
│    1. Configuration in application.yml                                                       │
│    2. Security filter chain with .oauth2Login()                                              │
│    That's it!                                                                                │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.12.1 ★ Step 1 — Register Your App with the Authorization Server

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ STEP 1: REGISTER YOUR APP WITH THE AUTHORIZATION SERVER                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Before writing any code, you must register your application with each                       │
│  OAuth 2.0 / OIDC provider (Google, GitHub, GitLab, Auth0, etc.).                           │
│                                                                                              │
│  ── WHAT YOU DO DURING REGISTRATION ─────────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. Go to the Provider's Developer Console:                                  │           │
│  │     • Google:  https://console.cloud.google.com/apis/credentials            │           │
│  │     • GitHub:  https://github.com/settings/developers                        │           │
│  │     • GitLab:  https://gitlab.com/-/user_settings/applications              │           │
│  │     • Auth0:   https://manage.auth0.com/dashboard → Applications            │           │
│  │                                                                               │           │
│  │  2. Create a new OAuth 2.0 Application / Client                             │           │
│  │     • Give it a name (e.g., "My Spring Boot App")                           │           │
│  │     • Select application type: "Web Application"                            │           │
│  │                                                                               │           │
│  │  3. Set the Redirect URI / Callback URL:                                     │           │
│  │     ★ This is the URL the Authorization Server will redirect to             │           │
│  │       AFTER the user successfully logs in.                                   │           │
│  │     ★ Spring Security's default callback URL pattern:                       │           │
│  │                                                                               │           │
│  │       http://localhost:8080/login/oauth2/code/{registrationId}              │           │
│  │                                                                               │           │
│  │     For each provider, set:                                                  │           │
│  │     • Google:  http://localhost:8080/login/oauth2/code/google               │           │
│  │     • GitHub:  http://localhost:8080/login/oauth2/code/github               │           │
│  │     • GitLab:  http://localhost:8080/login/oauth2/code/gitlab               │           │
│  │     • Auth0:   http://localhost:8080/login/oauth2/code/auth0                │           │
│  │                                                                               │           │
│  │     ★ {registrationId} = the key you use in application.yml under           │           │
│  │       spring.security.oauth2.client.registration.{registrationId}           │           │
│  │                                                                               │           │
│  │  4. Set the Scopes:                                                          │           │
│  │     • Google:  openid, email, profile                                       │           │
│  │     • GitHub:  read:user, user:email (GitHub uses OAuth 2.0, not OIDC)      │           │
│  │     • GitLab:  openid, email, profile, read_user                            │           │
│  │     • Auth0:   openid, email, profile                                       │           │
│  │                                                                               │           │
│  │  5. Retrieve the Credentials:                                                │           │
│  │     ┌───────────────────┬──────────────────────────────────────┐             │           │
│  │     │  Credential        │  Description                         │             │           │
│  │     ├───────────────────┼──────────────────────────────────────┤             │           │
│  │     │  Client ID         │  Public identifier for your app      │             │           │
│  │     │                    │  (safe to expose in browser)         │             │           │
│  │     ├───────────────────┼──────────────────────────────────────┤             │           │
│  │     │  Client Secret     │  Private secret (NEVER expose!)      │             │           │
│  │     │                    │  Used in server-to-server token      │             │           │
│  │     │                    │  exchange (back-channel)             │             │           │
│  │     └───────────────────┴──────────────────────────────────────┘             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── PROVIDER REGISTRATION SUMMARY TABLE ─────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌────────────┬───────────────────────────────────────────┬───────────────┐  │           │
│  │  │  Provider   │  Developer Console URL                    │  Protocol     │  │           │
│  │  ├────────────┼───────────────────────────────────────────┼───────────────┤  │           │
│  │  │  Google     │  console.cloud.google.com/apis/credentials│  OIDC         │  │           │
│  │  ├────────────┼───────────────────────────────────────────┼───────────────┤  │           │
│  │  │  GitHub     │  github.com/settings/developers           │  OAuth 2.0    │  │           │
│  │  ├────────────┼───────────────────────────────────────────┼───────────────┤  │           │
│  │  │  GitLab     │  gitlab.com/-/user_settings/applications  │  OIDC         │  │           │
│  │  ├────────────┼───────────────────────────────────────────┼───────────────┤  │           │
│  │  │  Auth0      │  manage.auth0.com/dashboard               │  OIDC         │  │           │
│  │  ├────────────┼───────────────────────────────────────────┼───────────────┤  │           │
│  │  │  Facebook   │  developers.facebook.com/apps             │  OAuth 2.0    │  │           │
│  │  ├────────────┼───────────────────────────────────────────┼───────────────┤  │           │
│  │  │  Okta       │  developer.okta.com                       │  OIDC         │  │           │
│  │  └────────────┴───────────────────────────────────────────┴───────────────┘  │           │
│  │                                                                               │           │
│  │  ★ OIDC providers return: access_token + id_token + refresh_token            │           │
│  │  ★ OAuth 2.0 only (GitHub): access_token only (no id_token)                 │           │
│  │    → Spring calls /user API to get user info instead                        │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.12.2 ★ Step 2 — Add the Dependency (spring-boot-starter-oauth2-client)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ STEP 2: ADD THE DEPENDENCY                                                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ★ The spring-boot-starter-oauth2-client dependency contains:                               │
│    → Pre-built OAuth 2.0 client code for Google, GitHub, GitLab, Facebook, Okta            │
│    → All the filters: OAuth2AuthorizationRequestRedirectFilter,                             │
│      OAuth2LoginAuthenticationFilter, etc.                                                   │
│    → All the providers: OidcAuthorizationCodeAuthenticationProvider, etc.                    │
│    → Token parsers, user info services, authorized client services                          │
│    → DefaultLoginPageGeneratingFilter for auto-generated login page                         │
│                                                                                              │
│  ★ For popular providers (Google, GitHub, GitLab, Facebook, Okta),                          │
│    the library already knows:                                                                │
│    → Authorization URL                                                                       │
│    → Token URL                                                                               │
│    → UserInfo URL                                                                            │
│    → Default scopes                                                                          │
│    → User name attribute                                                                     │
│    → So we ONLY need client-id and client-secret in config!                                 │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Add these dependencies                                             -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- ★ Spring Boot Starter Security (base security framework) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- ★ Spring Boot Starter OAuth2 Client
         This single dependency gives us:
         → OAuth 2.0 Authorization Code flow support
         → OIDC (OpenID Connect) support
         → Pre-configured clients for Google, GitHub, GitLab, Facebook, Okta
         → Auto-generated login page with provider links
         → All necessary filters and providers
         → Token management (in-memory by default)
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>

    <!-- Spring Boot Starter Web (required for MVC) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

</dependencies>
```

```groovy
// ═══════════════════════════════════════════════════════════════════════════════
//  build.gradle — For Gradle users
// ═══════════════════════════════════════════════════════════════════════════════

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHAT'S INSIDE spring-boot-starter-oauth2-client?                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  spring-boot-starter-oauth2-client                                           │           │
│  │  │                                                                            │           │
│  │  ├── spring-security-oauth2-client                                           │           │
│  │  │   ├── OAuth2AuthorizationRequestRedirectFilter                           │           │
│  │  │   ├── OAuth2LoginAuthenticationFilter                                     │           │
│  │  │   ├── OAuth2AuthorizedClientService (InMemory / JDBC)                    │           │
│  │  │   ├── OAuth2AuthorizedClientRepository                                    │           │
│  │  │   ├── OAuth2UserService (DefaultOAuth2UserService)                       │           │
│  │  │   └── ClientRegistration (Google, GitHub, GitLab, Facebook, Okta)        │           │
│  │  │                                                                            │           │
│  │  ├── spring-security-oauth2-jose                                             │           │
│  │  │   ├── JWT parsing and validation                                          │           │
│  │  │   ├── JWK (JSON Web Key) support                                         │           │
│  │  │   └── ID Token decoder                                                    │           │
│  │  │                                                                            │           │
│  │  └── spring-security-oauth2-core                                             │           │
│  │      ├── OAuth2 token types (Access, Refresh, ID)                           │           │
│  │      ├── OAuth2User, OidcUser                                                │           │
│  │      ├── OAuth2AuthenticationToken                                           │           │
│  │      └── Scopes, grant types, endpoints                                      │           │
│  │                                                                               │           │
│  │  ★ Pre-configured Provider Details (CommonOAuth2Provider enum):             │           │
│  │  ┌─────────────────────────────────────────────────────────────────────┐     │           │
│  │  │  GOOGLE  → authorization-uri, token-uri, user-info-uri, jwk-set   │     │           │
│  │  │  GITHUB  → authorization-uri, token-uri, user-info-uri            │     │           │
│  │  │  FACEBOOK→ authorization-uri, token-uri, user-info-uri            │     │           │
│  │  │  OKTA    → authorization-uri, token-uri, user-info-uri, jwk-set   │     │           │
│  │  └─────────────────────────────────────────────────────────────────────┘     │           │
│  │                                                                               │           │
│  │  ★ For these 4 providers, Spring already knows ALL endpoints!               │           │
│  │  ★ You ONLY need to provide: client-id and client-secret                    │           │
│  │  ★ For others (GitLab, Auth0, Keycloak), you need to provide                │           │
│  │    the endpoints manually in application.yml                                │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.12.3 ★ Step 3 — Configure application.yml (All Providers with Explanations)

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — OAuth 2.0 / OIDC Client Configuration
#
#  ★ You can configure MULTIPLE providers simultaneously!
#  ★ For Google, GitHub, Facebook, Okta — minimal config needed
#    (Spring knows the endpoints via CommonOAuth2Provider)
#  ★ For GitLab, Auth0, Keycloak — you must also provide the endpoints
#
#  Structure:
#  spring.security.oauth2.client.registration.{registrationId}  → YOUR app's config
#  spring.security.oauth2.client.provider.{providerId}          → Provider's endpoints
# ═══════════════════════════════════════════════════════════════════════════════

spring:
  security:
    oauth2:
      client:

        # ═══════════════════════════════════════════════════════════════════
        #  REGISTRATION — Your App's Configuration for each Provider
        #
        #  ★ {registrationId} is a UNIQUE key you choose.
        #    For built-in providers (google, github, facebook, okta),
        #    use the provider name as the registrationId so Spring
        #    auto-maps to CommonOAuth2Provider.
        # ═══════════════════════════════════════════════════════════════════
        registration:

          # ─────────────────────────────────────────────────────────────
          #  GOOGLE (OIDC) — Minimal config needed!
          #  ★ Spring auto-fills: authorization-uri, token-uri,
          #    user-info-uri, jwk-set-uri, user-name-attribute
          # ─────────────────────────────────────────────────────────────
          google:
            client-id: ${GOOGLE_CLIENT_ID}           # From Google Console
            client-secret: ${GOOGLE_CLIENT_SECRET}   # From Google Console
            scope:                                    # What access to request
              - openid                                # ★ Required for OIDC (returns id_token)
              - email                                 # User's email address
              - profile                               # User's name, picture, etc.
            # redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            #   ↑ DEFAULT — you don't need to set this! Spring sets it auto.
            #   ↑ Expands to: http://localhost:8080/login/oauth2/code/google
            # client-name: Google                     # Display name on login page (auto)
            # authorization-grant-type: authorization_code  # DEFAULT for oauth2Login
            # client-authentication-method: client_secret_basic  # DEFAULT

          # ─────────────────────────────────────────────────────────────
          #  GITHUB (OAuth 2.0 — NOT full OIDC)
          #  ★ GitHub does NOT support OIDC (no id_token)
          #  ★ Spring calls GitHub's /user API to get user info instead
          #  ★ Minimal config needed!
          # ─────────────────────────────────────────────────────────────
          github:
            client-id: ${GITHUB_CLIENT_ID}           # From GitHub Developer Settings
            client-secret: ${GITHUB_CLIENT_SECRET}   # From GitHub Developer Settings
            scope:
              - read:user                             # Read user profile
              - user:email                            # Read user email
            # redirect-uri: auto → http://localhost:8080/login/oauth2/code/github
            # client-name: GitHub                     # Display name on login page (auto)

          # ─────────────────────────────────────────────────────────────
          #  FACEBOOK (OAuth 2.0 — Limited OIDC)
          #  ★ Minimal config needed!
          # ─────────────────────────────────────────────────────────────
          facebook:
            client-id: ${FACEBOOK_CLIENT_ID}         # From Facebook Developer Console
            client-secret: ${FACEBOOK_CLIENT_SECRET} # From Facebook Developer Console
            scope:
              - email                                 # User's email
              - public_profile                        # Name, picture, etc.
            # redirect-uri: auto → http://localhost:8080/login/oauth2/code/facebook

          # ─────────────────────────────────────────────────────────────
          #  OKTA (OIDC) — Minimal config needed!
          #  ★ BUT you must provide the issuer-uri in the provider section
          # ─────────────────────────────────────────────────────────────
          okta:
            client-id: ${OKTA_CLIENT_ID}
            client-secret: ${OKTA_CLIENT_SECRET}
            scope:
              - openid
              - email
              - profile

          # ─────────────────────────────────────────────────────────────
          #  GITLAB (OIDC) — NOT a built-in provider
          #  ★ Must specify provider name AND give full endpoint config
          #  ★ registrationId = "gitlab" (your choice)
          #  ★ provider = "gitlab" (must match a provider section below)
          # ─────────────────────────────────────────────────────────────
          gitlab:
            client-id: ${GITLAB_CLIENT_ID}
            client-secret: ${GITLAB_CLIENT_SECRET}
            provider: gitlab                          # ★ Maps to provider.gitlab below
            scope:
              - openid
              - email
              - profile
              - read_user
            client-name: GitLab                       # Display name on login page
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"

          # ─────────────────────────────────────────────────────────────
          #  AUTH0 (OIDC) — NOT a built-in provider
          #  ★ Must specify provider name AND give endpoint config
          # ─────────────────────────────────────────────────────────────
          auth0:
            client-id: ${AUTH0_CLIENT_ID}
            client-secret: ${AUTH0_CLIENT_SECRET}
            provider: auth0                           # ★ Maps to provider.auth0 below
            scope:
              - openid
              - email
              - profile
            client-name: Auth0
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"

          # ─────────────────────────────────────────────────────────────
          #  KEYCLOAK (OIDC) — NOT a built-in provider
          #  ★ Must specify provider name AND give endpoint config
          # ─────────────────────────────────────────────────────────────
          keycloak:
            client-id: ${KEYCLOAK_CLIENT_ID}
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            provider: keycloak
            scope:
              - openid
              - email
              - profile
            client-name: Keycloak
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"


        # ═══════════════════════════════════════════════════════════════════
        #  PROVIDER — Authorization Server's Endpoint Configuration
        #
        #  ★ For Google, GitHub, Facebook, Okta (built-in) — you CAN skip
        #    this section! Spring auto-fills from CommonOAuth2Provider.
        #  ★ For GitLab, Auth0, Keycloak — you MUST provide these!
        #  ★ If provider supports OIDC discovery, just set issuer-uri
        #    and Spring will auto-discover all endpoints from:
        #    {issuer-uri}/.well-known/openid-configuration
        # ═══════════════════════════════════════════════════════════════════
        provider:

          # ─── OKTA (issuer-uri based — auto-discovers everything!) ─────
          okta:
            issuer-uri: https://${OKTA_DOMAIN}/oauth2/default
            # ★ Spring fetches: https://${OKTA_DOMAIN}/oauth2/default/.well-known/openid-configuration
            # ★ From that JSON, it auto-fills: authorization-uri, token-uri,
            #    user-info-uri, jwk-set-uri, user-name-attribute

          # ─── GITLAB (manual endpoint config) ────────────────────────
          gitlab:
            authorization-uri: https://gitlab.com/oauth/authorize
            token-uri: https://gitlab.com/oauth/token
            user-info-uri: https://gitlab.com/api/v4/user
            user-name-attribute: username
            jwk-set-uri: https://gitlab.com/oauth/discovery/keys
            # OR use issuer-uri for auto-discovery:
            # issuer-uri: https://gitlab.com

          # ─── AUTH0 (issuer-uri based — auto-discovers!) ──────────────
          auth0:
            issuer-uri: https://${AUTH0_DOMAIN}/
            # ★ Spring fetches: https://${AUTH0_DOMAIN}/.well-known/openid-configuration
            # ★ Auto-fills all endpoints from the discovery document

          # ─── KEYCLOAK (issuer-uri based) ─────────────────────────────
          keycloak:
            issuer-uri: http://localhost:8180/realms/${KEYCLOAK_REALM}
            # ★ Auto-discovers from:
            # http://localhost:8180/realms/{realm}/.well-known/openid-configuration
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ALL CONFIGURATION PROPERTIES EXPLAINED                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ── REGISTRATION PROPERTIES ─────────────────────────────────────────────────               │
│  (spring.security.oauth2.client.registration.{registrationId}.*)                            │
│                                                                                              │
│  ┌──────────────────────────────────┬────────────────────────────────────────────────────┐  │
│  │  Property                        │  Meaning                                           │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  client-id                       │  Public identifier from provider's console.        │  │
│  │                                  │  Sent in authorization & token requests.           │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  client-secret                   │  Private secret from provider's console.           │  │
│  │                                  │  Used in back-channel token exchange ONLY.         │  │
│  │                                  │  ★ NEVER expose in frontend or browser!            │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  scope                           │  Permissions to request from the user.             │  │
│  │                                  │  "openid" = OIDC mode (returns id_token)          │  │
│  │                                  │  "email", "profile" = user info claims             │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  redirect-uri                    │  URL the auth server sends user back to            │  │
│  │                                  │  after login, along with the authorization code.   │  │
│  │                                  │  Default: {baseUrl}/login/oauth2/code/{regId}     │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  authorization-grant-type        │  The OAuth 2.0 grant type to use.                  │  │
│  │                                  │  Default: authorization_code                       │  │
│  │                                  │  Others: client_credentials, refresh_token        │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  client-authentication-method    │  How to send client credentials to token endpoint. │  │
│  │                                  │  client_secret_basic → HTTP Basic Auth header     │  │
│  │                                  │  client_secret_post  → Form body parameters       │  │
│  │                                  │  none → No secret (PKCE / public clients)         │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  client-name                     │  Display name on the auto-generated login page.    │  │
│  │                                  │  e.g., "Google", "GitHub", "Sign in with Auth0"   │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  provider                        │  Maps to a provider.{name} section below.          │  │
│  │                                  │  For built-in providers, registrationId IS the     │  │
│  │                                  │  provider name (auto-mapped).                      │  │
│  └──────────────────────────────────┴────────────────────────────────────────────────────┘  │
│                                                                                              │
│                                                                                              │
│  ── PROVIDER PROPERTIES ─────────────────────────────────────────────────────               │
│  (spring.security.oauth2.client.provider.{providerId}.*)                                    │
│                                                                                              │
│  ┌──────────────────────────────────┬────────────────────────────────────────────────────┐  │
│  │  Property                        │  Meaning                                           │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  issuer-uri                      │  OIDC issuer identifier. Spring fetches            │  │
│  │                                  │  {issuer}/.well-known/openid-configuration        │  │
│  │                                  │  and auto-discovers ALL endpoints below.           │  │
│  │                                  │  ★ If set, you can skip all other provider props! │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  authorization-uri               │  URL to redirect user for login & consent.         │  │
│  │                                  │  e.g., https://gitlab.com/oauth/authorize         │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  token-uri                       │  URL for server-to-server token exchange.           │  │
│  │                                  │  e.g., https://gitlab.com/oauth/token             │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  user-info-uri                   │  URL to fetch user profile with access token.      │  │
│  │                                  │  e.g., https://gitlab.com/api/v4/user             │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  jwk-set-uri                     │  URL for JSON Web Key Set (public keys) to         │  │
│  │                                  │  verify JWT signature of id_token.                 │  │
│  ├──────────────────────────────────┼────────────────────────────────────────────────────┤  │
│  │  user-name-attribute             │  Which claim in user-info response to use as       │  │
│  │                                  │  the "name" of the authenticated principal.        │  │
│  │                                  │  Google: "sub", GitHub: "login", GitLab: "username"│  │
│  └──────────────────────────────────┴────────────────────────────────────────────────────┘  │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.12.4 ★ Step 4 — Security Filter Chain Configuration

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig.java — Enable OAuth 2.0 Login
//
//  ★ This is the ONLY Java code needed for basic OAuth 2.0 / OIDC login!
//  ★ Everything else is handled by Spring Security automatically!
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/public/**", "/error").permitAll()  // Public endpoints
                .anyRequest().authenticated()                              // Everything else → login
            )
            // ★ THIS ONE LINE enables the ENTIRE OAuth 2.0 / OIDC flow!
            // It auto-registers:
            //   → DefaultLoginPageGeneratingFilter (auto login page)
            //   → OAuth2AuthorizationRequestRedirectFilter (redirect to provider)
            //   → OAuth2LoginAuthenticationFilter (handle callback, exchange code)
            //   → OidcAuthorizationCodeAuthenticationProvider (token parsing)
            //   → InMemoryOAuth2AuthorizedClientService (store tokens)
            .oauth2Login(Customizer.withDefaults());

        return http.build();
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHAT .oauth2Login(Customizer.withDefaults()) DOES INTERNALLY                             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  When you call .oauth2Login(Customizer.withDefaults()), Spring Security      │           │
│  │  internally registers ALL of the following:                                   │           │
│  │                                                                               │           │
│  │  1. DefaultLoginPageGeneratingFilter                                         │           │
│  │     → Generates HTML login page at /login                                   │           │
│  │     → Lists all configured providers as clickable links                     │           │
│  │                                                                               │           │
│  │  2. OAuth2AuthorizationRequestRedirectFilter                                │           │
│  │     → Intercepts /oauth2/authorization/{registrationId}                     │           │
│  │     → Redirects browser to provider's authorization endpoint               │           │
│  │                                                                               │           │
│  │  3. OAuth2LoginAuthenticationFilter                                          │           │
│  │     → Intercepts /login/oauth2/code/{registrationId} (callback)            │           │
│  │     → Exchanges authorization code for tokens                               │           │
│  │                                                                               │           │
│  │  4. AuthenticationManager with providers:                                    │           │
│  │     → OidcAuthorizationCodeAuthenticationProvider (for OIDC providers)      │           │
│  │     → OAuth2LoginAuthenticationProvider (for OAuth 2.0 only providers)      │           │
│  │                                                                               │           │
│  │  5. InMemoryOAuth2AuthorizedClientService                                    │           │
│  │     → Stores OAuth2AuthorizedClient (tokens + user info) in memory          │           │
│  │                                                                               │           │
│  │  6. HttpSessionOAuth2AuthorizationRequestRepository                          │           │
│  │     → Stores pending authorization requests in HTTP session                 │           │
│  │                                                                               │           │
│  │  7. OAuth2UserService / OidcUserService                                      │           │
│  │     → Loads user details from provider's user-info endpoint                 │           │
│  │                                                                               │           │
│  │  ★ ALL OF THIS from ONE line of code!                                        │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.12.5 ★ Step 5 — The Complete Internal Flow (Step-by-Step with Filters & Classes)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE AUTHORIZATION CODE GRANT FLOW — SPRING SECURITY INTERNALS                       │
│    (Every Filter & Class Involved, Step by Step)                                             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│  PHASE 1: USER VISITS THE APP → AUTO-GENERATED LOGIN PAGE                                   │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  User                Spring Security                                         │           │
│  │  (Browser)           Filter Chain                                             │           │
│  │  ────────            ─────────────                                            │           │
│  │     │                     │                                                   │           │
│  │     │  GET /dashboard     │                                                   │           │
│  │     │ ──────────────────> │                                                   │           │
│  │     │                     │                                                   │           │
│  │     │         AuthorizationFilter: "Not authenticated!"                      │           │
│  │     │         → Throws AccessDeniedException                                 │           │
│  │     │         → ExceptionTranslationFilter catches it                        │           │
│  │     │         → Redirects to /login                                           │           │
│  │     │                     │                                                   │           │
│  │     │  302 Redirect       │                                                   │           │
│  │     │  Location: /login   │                                                   │           │
│  │     │ <────────────────── │                                                   │           │
│  │     │                     │                                                   │           │
│  │     │  GET /login         │                                                   │           │
│  │     │ ──────────────────> │                                                   │           │
│  │     │                     │                                                   │           │
│  │     │         ★ DefaultLoginPageGeneratingFilter.java is invoked             │           │
│  │     │           → It reads all configured ClientRegistrations                │           │
│  │     │           → Generates an HTML page with links for each provider        │           │
│  │     │           → The HTML contains links like:                               │           │
│  │     │                                                                         │           │
│  │     │             <a href="/oauth2/authorization/google">Google</a>          │           │
│  │     │             <a href="/oauth2/authorization/github">GitHub</a>          │           │
│  │     │             <a href="/oauth2/authorization/gitlab">GitLab</a>          │           │
│  │     │             <a href="/oauth2/authorization/auth0">Auth0</a>            │           │
│  │     │                                                                         │           │
│  │     │  200 OK (HTML login page with provider list)                           │           │
│  │     │ <────────────────── │                                                   │           │
│  │     │                     │                                                   │           │
│  │     │  ★ User sees a page with all configured provider buttons/links        │           │
│  │     │                                                                         │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│  PHASE 2: USER CLICKS A PROVIDER → REDIRECT TO AUTHORIZATION SERVER                        │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  User              Spring Security           Authorization Server             │           │
│  │  (Browser)         Filter Chain               (e.g., Google)                  │           │
│  │  ────────          ─────────────              ──────────────────               │           │
│  │     │                   │                           │                         │           │
│  │     │  User clicks "Google"                         │                         │           │
│  │     │                   │                           │                         │           │
│  │     │  GET /oauth2/authorization/google             │                         │           │
│  │     │ ────────────────> │                           │                         │           │
│  │     │                   │                           │                         │           │
│  │     │       ★ OAuth2AuthorizationRequestRedirectFilter is invoked            │           │
│  │     │         │                                     │                         │           │
│  │     │         │ 1. Matches URL pattern: /oauth2/authorization/{registrationId}           │
│  │     │         │ 2. Looks up ClientRegistration for "google" from config     │           │
│  │     │         │ 3. Reads authorization-uri from config/CommonOAuth2Provider │           │
│  │     │         │    → https://accounts.google.com/o/oauth2/v2/auth           │           │
│  │     │         │ 4. Generates a random "state" parameter (CSRF protection)   │           │
│  │     │         │ 5. Stores the authorization request in HttpSession          │           │
│  │     │         │    via HttpSessionOAuth2AuthorizationRequestRepository      │           │
│  │     │         │ 6. Builds the full authorization URL:                        │           │
│  │     │         │                                     │                         │           │
│  │     │         │    https://accounts.google.com/o/oauth2/v2/auth              │           │
│  │     │         │    ?response_type=code               │                         │           │
│  │     │         │    &client_id=abc123                  │                         │           │
│  │     │         │    &redirect_uri=http://localhost:8080/login/oauth2/code/google           │
│  │     │         │    &scope=openid+email+profile        │                         │           │
│  │     │         │    &state=random_csrf_string           │                         │           │
│  │     │         │                                     │                         │           │
│  │     │         │ 7. Sends 302 Redirect to the browser │                         │           │
│  │     │         │                                     │                         │           │
│  │     │  302 Redirect                                 │                         │           │
│  │     │  Location: https://accounts.google.com/o/oauth2/v2/auth?...           │           │
│  │     │ <──────────────── │                           │                         │           │
│  │     │                   │                           │                         │           │
│  │     │  Browser follows redirect → lands on Google login page                │           │
│  │     │ ─────────────────────────────────────────────>│                         │           │
│  │     │                   │                           │                         │           │
│  │     │                   │          Google shows login form                    │           │
│  │     │                   │          User enters email + password               │           │
│  │     │                   │          Google shows consent screen                │           │
│  │     │                   │          User clicks "Allow"                        │           │
│  │     │                   │                           │                         │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│  PHASE 3: AUTHORIZATION SERVER REDIRECTS BACK → CALLBACK URL                                │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Authorization          User                Spring Security                   │           │
│  │  Server (Google)        (Browser)           Filter Chain                      │           │
│  │  ──────────────         ────────            ─────────────                     │           │
│  │     │                     │                      │                            │           │
│  │     │  After successful login & consent:          │                            │           │
│  │     │                     │                      │                            │           │
│  │     │  302 Redirect       │                      │                            │           │
│  │     │  Location:          │                      │                            │           │
│  │     │    http://localhost:8080/login/oauth2/code/google                       │           │
│  │     │    ?code=AUTH_CODE_XYZ                      │                            │           │
│  │     │    &state=random_csrf_string                │                            │           │
│  │     │ ──────────────────> │                      │                            │           │
│  │     │                     │                      │                            │           │
│  │     │     Browser follows redirect (callback URL)│                            │           │
│  │     │                     │                      │                            │           │
│  │     │     GET /login/oauth2/code/google?code=AUTH_CODE_XYZ&state=...         │           │
│  │     │                     │ ───────────────────> │                            │           │
│  │     │                     │                      │                            │           │
│  │     │                     │      ★ OAuth2LoginAuthenticationFilter            │           │
│  │     │                     │        is invoked (handles the callback)          │           │
│  │     │                     │                      │                            │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│  PHASE 4: TOKEN EXCHANGE & AUTHENTICATION (All Server-Side / Back-Channel)                  │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  This is the MOST important phase — all happens server-to-server,            │           │
│  │  NEVER exposed to the browser!                                                │           │
│  │                                                                               │           │
│  │  OAuth2LoginAuthenticationFilter                                              │           │
│  │     │                                                                         │           │
│  │     │ 1. Extracts "code" and "state" from the callback URL parameters        │           │
│  │     │                                                                         │           │
│  │     │ 2. Validates "state" matches what was stored in HttpSession            │           │
│  │     │    (CSRF protection — prevents authorization code injection!)          │           │
│  │     │                                                                         │           │
│  │     │ 3. Creates an OAuth2LoginAuthenticationToken (unauthenticated)         │           │
│  │     │    containing: authorization code, clientRegistration, redirectUri     │           │
│  │     │                                                                         │           │
│  │     │ 4. Passes it to AuthenticationManager.authenticate()                   │           │
│  │     │                                                                         │           │
│  │     ▼                                                                         │           │
│  │  AuthenticationManager                                                        │           │
│  │     │                                                                         │           │
│  │     │ Delegates to the appropriate AuthenticationProvider:                    │           │
│  │     │                                                                         │           │
│  │     │ IF scope contains "openid" (OIDC):                                     │           │
│  │     │   → ★ OidcAuthorizationCodeAuthenticationProvider                     │           │
│  │     │                                                                         │           │
│  │     │ IF scope does NOT contain "openid" (plain OAuth 2.0, e.g., GitHub):   │           │
│  │     │   → OAuth2LoginAuthenticationProvider                                  │           │
│  │     │                                                                         │           │
│  │     ▼                                                                         │           │
│  │  OidcAuthorizationCodeAuthenticationProvider (for OIDC like Google)           │           │
│  │     │                                                                         │           │
│  │     │ 5. Makes a server-to-server POST request to the Token Endpoint:        │           │
│  │     │                                                                         │           │
│  │     │    POST https://oauth2.googleapis.com/token                             │           │
│  │     │    Content-Type: application/x-www-form-urlencoded                      │           │
│  │     │                                                                         │           │
│  │     │    grant_type=authorization_code                                        │           │
│  │     │    code=AUTH_CODE_XYZ                    ← Authorization code           │           │
│  │     │    client_id=abc123                      ← From config                  │           │
│  │     │    client_secret=s3cret                  ← From config (NEVER in browser!)         │
│  │     │    redirect_uri=http://localhost:8080/login/oauth2/code/google          │           │
│  │     │    state=random_csrf_string              ← If applicable               │           │
│  │     │                                                                         │           │
│  │     │ 6. Authorization Server validates and returns tokens:                   │           │
│  │     │                                                                         │           │
│  │     │    {                                                                    │           │
│  │     │      "access_token": "eyJhbGci...",     ← For API calls (JWT or opaque)│           │
│  │     │      "id_token": "eyJhbGci...",         ← User identity (ALWAYS JWT!)  │           │
│  │     │      "refresh_token": "eyJhbGci...",    ← For token renewal (if configured)       │
│  │     │      "token_type": "Bearer",                                           │           │
│  │     │      "expires_in": 3600                                                │           │
│  │     │    }                                                                    │           │
│  │     │                                                                         │           │
│  │     │    ★ Access token can be JWT or OPAQUE — depends on the provider       │           │
│  │     │    ★ ID token MUST ALWAYS be JWT — per OIDC specification              │           │
│  │     │                                                                         │           │
│  │     │ 7. Decodes and validates the id_token (JWT):                            │           │
│  │     │    → Verifies signature using JWK Set from jwk-set-uri                │           │
│  │     │    → Checks iss (issuer), aud (audience = client_id), exp (expiry)    │           │
│  │     │    → Extracts claims: sub, name, email, picture, etc.                 │           │
│  │     │                                                                         │           │
│  │     │ 8. Calls OidcUserService.loadUser() to create OidcUser object          │           │
│  │     │    → Contains: id_token claims + userInfo (if user-info-uri configured)│           │
│  │     │    → Implements OAuth2User and OidcUser interfaces                     │           │
│  │     │                                                                         │           │
│  │     │ 9. Returns OAuth2LoginAuthenticationToken (NOW authenticated!)         │           │
│  │     │    containing: OidcUser, authorities, tokens                           │           │
│  │     │                                                                         │           │
│  │     ▼                                                                         │           │
│  │  Back in OAuth2LoginAuthenticationFilter                                      │           │
│  │     │                                                                         │           │
│  │     │ 10. Receives the authenticated token                                    │           │
│  │     │                                                                         │           │
│  │     │ 11. ★ InMemoryOAuth2AuthorizedClientService.saveAuthorizedClient()     │           │
│  │     │     → Creates OAuth2AuthorizedClient containing:                       │           │
│  │     │       • ClientRegistration (provider config)                           │           │
│  │     │       • principalName (user identifier)                               │           │
│  │     │       • OAuth2AccessToken (access token)                               │           │
│  │     │       • OAuth2RefreshToken (refresh token, if present)                 │           │
│  │     │     → Stores it in an in-memory ConcurrentHashMap                     │           │
│  │     │     ★ Instead of in-memory, you can use:                               │           │
│  │     │       • JdbcOAuth2AuthorizedClientService → store in database         │           │
│  │     │       • Custom implementation → store in Redis                        │           │
│  │     │                                                                         │           │
│  │     │ 12. Creates OAuth2AuthenticationToken (Spring Security principal)      │           │
│  │     │     → wraps the OidcUser with granted authorities                     │           │
│  │     │                                                                         │           │
│  │     │ 13. Sets SecurityContextHolder.getContext().setAuthentication(token)    │           │
│  │     │     → The user is NOW authenticated in Spring Security!                │           │
│  │     │                                                                         │           │
│  │     │ 14. Spring creates an HttpSession and stores SecurityContext in it     │           │
│  │     │     → HttpSessionSecurityContextRepository saves the context           │           │
│  │     │                                                                         │           │
│  │     │ 15. Returns 302 Redirect to the original URL (/dashboard)             │           │
│  │     │     + Set-Cookie: JSESSIONID=abc123xyz                                 │           │
│  │     │     → This JSESSIONID cookie identifies the session                   │           │
│  │     │     → All subsequent requests use this cookie instead of OAuth tokens │           │
│  │     │                                                                         │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│  PHASE 5: SUBSEQUENT REQUESTS — JSESSIONID COOKIE                                           │
│  ═══════════════════════════════════════════════════════════════════════════                  │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  User                Spring Security                 Controller               │           │
│  │  (Browser)           Filter Chain                                             │           │
│  │  ────────            ─────────────                   ──────────               │           │
│  │     │                     │                              │                    │           │
│  │     │  GET /dashboard     │                              │                    │           │
│  │     │  Cookie: JSESSIONID=abc123xyz                      │                    │           │
│  │     │ ──────────────────> │                              │                    │           │
│  │     │                     │                              │                    │           │
│  │     │       ★ SecurityContextHolderFilter is invoked                         │           │
│  │     │         │                                          │                    │           │
│  │     │         │ 1. Reads JSESSIONID cookie from request  │                    │           │
│  │     │         │ 2. Looks up HttpSession by JSESSIONID    │                    │           │
│  │     │         │ 3. HttpSessionSecurityContextRepository  │                    │           │
│  │     │         │    loads the saved SecurityContext from   │                    │           │
│  │     │         │    the session                            │                    │           │
│  │     │         │ 4. Sets SecurityContextHolder.getContext()│                    │           │
│  │     │         │    .setAuthentication(savedAuth)          │                    │           │
│  │     │         │                                          │                    │           │
│  │     │         │ ★ User is authenticated from session!    │                    │           │
│  │     │         │   No need to re-do OAuth flow!           │                    │           │
│  │     │         │                                          │                    │           │
│  │     │         ▼                                          │                    │           │
│  │     │       AuthorizationFilter: "User is authenticated, has access"         │           │
│  │     │         │                                          │                    │           │
│  │     │         ▼                                          │                    │           │
│  │     │       Request reaches Controller                   │                    │           │
│  │     │                     │ ─────────────────────────── >│                    │           │
│  │     │                     │                              │                    │           │
│  │     │                     │    Controller can access:    │                    │           │
│  │     │                     │    @AuthenticationPrincipal  │                    │           │
│  │     │                     │    OidcUser user             │                    │           │
│  │     │                     │    → user.getEmail()         │                    │           │
│  │     │                     │    → user.getFullName()      │                    │           │
│  │     │                     │    → user.getIdToken()       │                    │           │
│  │     │                     │    → user.getAccessTokenValue()                   │           │
│  │     │                     │                              │                    │           │
│  │     │  200 OK (dashboard page with user data)            │                    │           │
│  │     │ <────────────────── │                              │                    │           │
│  │     │                     │                              │                    │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.12.6 ★ Complete Flow Diagram — All 5 Phases in One View

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE AUTHORIZATION CODE GRANT FLOW — ALL PHASES                                      │
│    (Browser ↔ Spring Security ↔ Authorization Server)                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  User            Spring Security              Authorization         Your                    │
│  (Browser)       (Filter Chain)               Server (Google)       Controller               │
│  ────────        ─────────────                ──────────────        ──────────               │
│     │                 │                             │                   │                    │
│     │  ── PHASE 1: Login Page ──────────────────────────────────────────                    │
│     │                 │                             │                   │                    │
│     │  GET /dashboard │                             │                   │                    │
│     │────────────────>│                             │                   │                    │
│     │                 │ AuthorizationFilter:        │                   │                    │
│     │                 │ "Not authenticated!"        │                   │                    │
│     │  302 → /login   │                             │                   │                    │
│     │<────────────────│                             │                   │                    │
│     │                 │                             │                   │                    │
│     │  GET /login     │                             │                   │                    │
│     │────────────────>│                             │                   │                    │
│     │                 │ DefaultLoginPageGeneratingFilter                │                    │
│     │                 │ generates HTML with provider links              │                    │
│     │  200 OK (HTML)  │                             │                   │                    │
│     │  [Google]       │                             │                   │                    │
│     │  [GitHub]       │                             │                   │                    │
│     │  [GitLab]       │                             │                   │                    │
│     │<────────────────│                             │                   │                    │
│     │                 │                             │                   │                    │
│     │  ── PHASE 2: Redirect to Authorization Server ────────────────────                   │
│     │                 │                             │                   │                    │
│     │  Click "Google" │                             │                   │                    │
│     │  GET /oauth2/authorization/google             │                   │                    │
│     │────────────────>│                             │                   │                    │
│     │                 │ OAuth2AuthorizationRequestRedirectFilter        │                    │
│     │                 │ → Builds auth URL with client_id, scope, state │                    │
│     │                 │ → Saves request in HttpSession                 │                    │
│     │  302 → Google   │                             │                   │                    │
│     │<────────────────│                             │                   │                    │
│     │                 │                             │                   │                    │
│     │  GET https://accounts.google.com/o/oauth2/v2/auth?...           │                    │
│     │──────────────────────────────────────────────>│                   │                    │
│     │                 │                             │                   │                    │
│     │                 │      User logs in + consents│                   │                    │
│     │                 │                             │                   │                    │
│     │  ── PHASE 3: Callback with Authorization Code ────────────────────                   │
│     │                 │                             │                   │                    │
│     │  302 → callback │                             │                   │                    │
│     │  /login/oauth2/code/google?code=XYZ&state=... │                   │                    │
│     │<─────────────────────────────────────────────│                   │                    │
│     │                 │                             │                   │                    │
│     │  GET /login/oauth2/code/google?code=XYZ&state=...                │                    │
│     │────────────────>│                             │                   │                    │
│     │                 │                             │                   │                    │
│     │  ── PHASE 4: Token Exchange (Back-Channel, Server-to-Server) ────                    │
│     │                 │                             │                   │                    │
│     │                 │ OAuth2LoginAuthenticationFilter                 │                    │
│     │                 │ → Validates state parameter  │                   │                    │
│     │                 │ → Creates auth token object  │                   │                    │
│     │                 │ → Calls AuthenticationManager│                   │                    │
│     │                 │                             │                   │                    │
│     │                 │ OidcAuthorizationCodeAuthenticationProvider     │                    │
│     │                 │ POST /token                  │                   │                    │
│     │                 │ {code, client_id, secret}    │                   │                    │
│     │                 │────────────────────────────>│                   │                    │
│     │                 │                             │                   │                    │
│     │                 │ {access_token, id_token,    │                   │                    │
│     │                 │  refresh_token}             │                   │                    │
│     │                 │<────────────────────────────│                   │                    │
│     │                 │                             │                   │                    │
│     │                 │ → Validates & decodes id_token (JWT)            │                    │
│     │                 │ → Creates OidcUser with claims                  │                    │
│     │                 │ → InMemoryOAuth2AuthorizedClientService         │                    │
│     │                 │   stores tokens + user details                  │                    │
│     │                 │ → SecurityContextHolder.setAuthentication()     │                    │
│     │                 │ → Creates HttpSession                           │                    │
│     │                 │                             │                   │                    │
│     │  302 → /dashboard                            │                   │                    │
│     │  Set-Cookie: JSESSIONID=abc123xyz             │                   │                    │
│     │<────────────────│                             │                   │                    │
│     │                 │                             │                   │                    │
│     │  ── PHASE 5: Subsequent Requests (Session-Based) ─────────────────                   │
│     │                 │                             │                   │                    │
│     │  GET /dashboard │                             │                   │                    │
│     │  Cookie: JSESSIONID=abc123xyz                 │                   │                    │
│     │────────────────>│                             │                   │                    │
│     │                 │ SecurityContextHolderFilter  │                   │                    │
│     │                 │ → Reads JSESSIONID           │                   │                    │
│     │                 │ → Loads SecurityContext from HttpSession        │                    │
│     │                 │ → User is authenticated!     │                   │                    │
│     │                 │                             │                   │                    │
│     │                 │ AuthorizationFilter: ✅ OK   │                   │                    │
│     │                 │                             │                   │                    │
│     │                 │──────────────────────────────────────────────>│                    │
│     │                 │                             │                │                    │
│     │                 │                             │   Controller    │                    │
│     │                 │                             │   processes     │                    │
│     │                 │                             │   request       │                    │
│     │  200 OK (data)  │                             │                │                    │
│     │<────────────────│                             │                │                    │
│     │                 │                             │                   │                    │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.12.7 ★ Storing Tokens — InMemory vs DB vs Redis

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ TOKEN STORAGE OPTIONS — InMemory vs JDBC (DB) vs Redis                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  By default, Spring uses InMemoryOAuth2AuthorizedClientService to store                     │
│  the OAuth2AuthorizedClient (contains access token, refresh token, user info).              │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌────────────────────┬──────────────────────────┬──────────────────────┐    │           │
│  │  │  Option             │  Class                    │  When to Use         │    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  In-Memory (Default)│  InMemoryOAuth2           │  Dev / single instance│    │           │
│  │  │                    │  AuthorizedClientService  │  ★ Lost on restart!  │    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  JDBC (Database)   │  JdbcOAuth2               │  Production / multi  │    │           │
│  │  │                    │  AuthorizedClientService  │  instance deployment │    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  Redis             │  Custom implementation    │  High performance    │    │           │
│  │  │                    │                           │  distributed caching │    │           │
│  │  └────────────────────┴──────────────────────────┴──────────────────────┘    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Option 1: JDBC — Store OAuth2AuthorizedClient in Database
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
public class OAuth2ClientConfig {

    // ★ Use JdbcOAuth2AuthorizedClientService instead of InMemory
    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            JdbcOperations jdbcOperations,
            ClientRegistrationRepository clientRegistrationRepository) {
        return new JdbcOAuth2AuthorizedClientService(jdbcOperations, clientRegistrationRepository);
    }
}
```

```sql
-- ═══════════════════════════════════════════════════════════════════════════════
--  Required DB Table for JdbcOAuth2AuthorizedClientService
--  (Spring provides this schema — run it on your DB)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE oauth2_authorized_client (
    client_registration_id  VARCHAR(100)  NOT NULL,
    principal_name          VARCHAR(200)  NOT NULL,
    access_token_type       VARCHAR(100)  NOT NULL,
    access_token_value      BLOB          NOT NULL,
    access_token_issued_at  TIMESTAMP     NOT NULL,
    access_token_expires_at TIMESTAMP     NOT NULL,
    access_token_scopes     VARCHAR(1000) DEFAULT NULL,
    refresh_token_value     BLOB          DEFAULT NULL,
    refresh_token_issued_at TIMESTAMP     DEFAULT NULL,
    created_at              TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (client_registration_id, principal_name)
);
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Option 2: Redis — Custom OAuth2AuthorizedClientService with Redis
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class RedisOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private final RedisTemplate<String, OAuth2AuthorizedClient> redisTemplate;

    public RedisOAuth2AuthorizedClientService(
            RedisTemplate<String, OAuth2AuthorizedClient> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
            String clientRegistrationId, String principalName) {
        String key = buildKey(clientRegistrationId, principalName);
        return (T) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void saveAuthorizedClient(
            OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        String key = buildKey(
            authorizedClient.getClientRegistration().getRegistrationId(),
            principal.getName()
        );
        redisTemplate.opsForValue().set(key, authorizedClient, Duration.ofHours(1));
    }

    @Override
    public void removeAuthorizedClient(
            String clientRegistrationId, String principalName) {
        redisTemplate.delete(buildKey(clientRegistrationId, principalName));
    }

    private String buildKey(String clientRegistrationId, String principalName) {
        return "oauth2:client:" + clientRegistrationId + ":" + principalName;
    }
}
```

---

##### 20.12.8 ★ Key Points — Token Types

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ KEY POINTS — TOKEN TYPES IN OAUTH 2.0 / OIDC                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌──────────────────┬──────────────────────────────────────────────┐         │           │
│  │  │  Token             │  Format                                     │         │           │
│  │  ├──────────────────┼──────────────────────────────────────────────┤         │           │
│  │  │  Access Token      │  JWT or Opaque (provider decides)           │         │           │
│  │  │                    │  • Google: JWT                              │         │           │
│  │  │                    │  • GitHub: opaque string                   │         │           │
│  │  │                    │  • Auth0: JWT (configurable)               │         │           │
│  │  │                    │  • Keycloak: JWT                            │         │           │
│  │  ├──────────────────┼──────────────────────────────────────────────┤         │           │
│  │  │  ID Token          │  ★ ALWAYS JWT (required by OIDC spec!)     │         │           │
│  │  │                    │  Contains: sub, name, email, picture,      │         │           │
│  │  │                    │  iss, aud, iat, exp                        │         │           │
│  │  │                    │  Signed with provider's private key        │         │           │
│  │  │                    │  Verified using jwk-set-uri public keys    │         │           │
│  │  ├──────────────────┼──────────────────────────────────────────────┤         │           │
│  │  │  Refresh Token     │  Usually opaque string                      │         │           │
│  │  │                    │  Used to get new access tokens              │         │           │
│  │  │                    │  Long-lived (days to months)               │         │           │
│  │  │                    │  ★ Not always returned — depends on config │         │           │
│  │  └──────────────────┴──────────────────────────────────────────────┘         │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ★ WHY ID TOKEN MUST BE JWT?                                                 │           │
│  │    → The client needs to READ it (decode claims like name, email)           │           │
│  │    → The client needs to VERIFY it (check signature, issuer, audience)      │           │
│  │    → Opaque tokens can't be read without calling the server                 │           │
│  │    → JWT is self-contained: the client can verify and read it locally       │           │
│  │                                                                               │           │
│  │  ★ WHY ACCESS TOKEN CAN BE OPAQUE?                                           │           │
│  │    → The resource server can validate opaque tokens via introspection        │           │
│  │      endpoint (POST /introspect with the token)                              │           │
│  │    → The client doesn't need to read the access token, only send it         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.12.9 ★ Summary — What YOU Need to Write vs What Spring Provides

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — WHAT YOU WRITE vs WHAT SPRING PROVIDES                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── WHAT YOU WRITE (Minimal!) ──────────────────────────────────────────     │           │
│  │                                                                               │           │
│  │  1. pom.xml / build.gradle                                                   │           │
│  │     → Add spring-boot-starter-oauth2-client dependency                      │           │
│  │                                                                               │           │
│  │  2. application.yml                                                           │           │
│  │     → client-id, client-secret, scopes for each provider                   │           │
│  │     → For non-built-in providers: issuer-uri or endpoint URLs               │           │
│  │                                                                               │           │
│  │  3. SecurityConfig.java                                                       │           │
│  │     → .oauth2Login(Customizer.withDefaults())                               │           │
│  │                                                                               │           │
│  │  That's it! Just 3 things!                                                   │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── WHAT SPRING PROVIDES (Everything else!) ──────────────────────────       │           │
│  │                                                                               │           │
│  │  ✅ DefaultLoginPageGeneratingFilter      → Auto login page with links      │           │
│  │  ✅ OAuth2AuthorizationRequestRedirectFilter → Redirect to auth server      │           │
│  │  ✅ OAuth2LoginAuthenticationFilter        → Handle callback URL            │           │
│  │  ✅ OidcAuthorizationCodeAuthenticationProvider → Token exchange            │           │
│  │  ✅ OAuth2LoginAuthenticationProvider      → For non-OIDC providers         │           │
│  │  ✅ OidcUserService / DefaultOAuth2UserService → Load user info            │           │
│  │  ✅ InMemoryOAuth2AuthorizedClientService  → Store tokens                  │           │
│  │  ✅ HttpSessionSecurityContextRepository   → Session management            │           │
│  │  ✅ JWT decoding & validation (id_token)   → Via spring-security-oauth2-jose│           │
│  │  ✅ CSRF protection via state parameter    → Auto-generated & validated    │           │
│  │  ✅ SecurityContext population              → Automatic after auth          │           │
│  │  ✅ JSESSIONID cookie management            → Standard servlet session     │           │
│  │  ✅ CommonOAuth2Provider configs            → Google, GitHub, Facebook, Okta│           │
│  │                                                                               │           │
│  │  ★ For popular vendors (Google, GitHub, GitLab, Auth0, Okta), almost        │           │
│  │    ALL the code is already written in the library!                           │           │
│  │    You just add the configuration + one line in SecurityFilterChain.         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.13 ★ Making OAuth 2.0 Login Stateless — Returning Tokens Instead of JSESSIONID

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ MAKING OAUTH 2.0 LOGIN STATELESS — TOKENS INSTEAD OF SESSIONS                           │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ── THE PROBLEM: SPRING BOOT'S DEFAULT IS STATEFUL ──────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  By default, Spring Boot assumes OAuth 2.0 login happens in a BROWSER:      │           │
│  │                                                                               │           │
│  │  ★ Default Behavior (Stateful):                                              │           │
│  │    1. User logs in via OAuth 2.0 (Google, GitHub, etc.)                     │           │
│  │    2. Spring creates an HttpSession on the server                           │           │
│  │    3. Stores SecurityContext (with user info, tokens) in that session       │           │
│  │    4. Returns a JSESSIONID cookie to the browser                            │           │
│  │    5. Every subsequent request sends the JSESSIONID cookie                  │           │
│  │    6. Spring looks up the session by JSESSIONID to authenticate             │           │
│  │                                                                               │           │
│  │  ❌ Problems with Stateful Sessions:                                         │           │
│  │    • Server must store session state → not scalable horizontally            │           │
│  │    • Sticky sessions needed in load-balanced environments                   │           │
│  │    • Not suitable for SPAs, mobile apps, or API-first architectures        │           │
│  │    • Session replication across multiple instances is complex               │           │
│  │    • Can't share session between different services (microservices)         │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ★ Solution: Make it STATELESS!                                              │           │
│  │    1. User logs in via OAuth 2.0 (same flow — redirect to Google, etc.)    │           │
│  │    2. After successful login, instead of creating a session,                │           │
│  │       RETURN the Access Token and ID Token directly to the client           │           │
│  │    3. Client stores the tokens (localStorage, secure cookie, etc.)         │           │
│  │    4. Client sends the ID Token / Access Token in every request header:    │           │
│  │       Authorization: Bearer <id_token>                                      │           │
│  │    5. Spring validates the token on EVERY request using provider's          │           │
│  │       public keys (JWK Set) — NO session needed!                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│                                                                                              │
│  ── STATEFUL vs STATELESS — COMPARISON ──────────────────────────────────────               │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌────────────────────┬──────────────────────────┬──────────────────────┐    │           │
│  │  │  Aspect             │  Stateful (Default)       │  Stateless (Token)   │    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  Auth carried via   │  JSESSIONID cookie        │  Authorization header│    │           │
│  │  │                    │                           │  (Bearer token)      │    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  Server stores      │  HttpSession (in memory) │  Nothing!            │    │           │
│  │  │                    │                           │  Token is self-      │    │           │
│  │  │                    │                           │  contained (JWT)     │    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  Scaling            │  Sticky sessions or      │  Any server can      │    │           │
│  │  │                    │  session replication      │  validate the token │    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  Best for           │  Server-rendered apps     │  SPAs, mobile apps,  │    │           │
│  │  │                    │  (Thymeleaf, JSP)        │  API-first, micro-  │    │           │
│  │  │                    │                           │  services            │    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  After OAuth login  │  Creates session +       │  Returns tokens in   │    │           │
│  │  │                    │  JSESSIONID cookie       │  response body/header│    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  Subsequent requests│  Cookie: JSESSIONID      │  Authorization:      │    │           │
│  │  │                    │                           │  Bearer <token>      │    │           │
│  │  ├────────────────────┼──────────────────────────┼──────────────────────┤    │           │
│  │  │  Validation         │  Session lookup          │  JWT signature       │    │           │
│  │  │                    │                           │  verification        │    │           │
│  │  └────────────────────┴──────────────────────────┴──────────────────────┘    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.13.1 ★ How to Make It Stateless — The Key Insight

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ THE KEY INSIGHT — WHERE TO HOOK IN                                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  In the default OAuth 2.0 flow, after successful authentication:             │           │
│  │                                                                               │           │
│  │  OAuth2LoginAuthenticationFilter                                              │           │
│  │     │                                                                         │           │
│  │     │ ... token exchange completes successfully ...                           │           │
│  │     │                                                                         │           │
│  │     │ ★ Calls onAuthenticationSuccess() method                               │           │
│  │     │   │                                                                     │           │
│  │     │   │ This method uses AuthenticationSuccessHandler                       │           │
│  │     │   │ (specifically SavedRequestAwareAuthenticationSuccessHandler)        │           │
│  │     │   │                                                                     │           │
│  │     │   │ Default behavior:                                                   │           │
│  │     │   │   1. Cleans up the authorization request from session              │           │
│  │     │   │   2. Creates HttpSession                                            │           │
│  │     │   │   3. Stores SecurityContext in session                              │           │
│  │     │   │   4. Redirects to the originally requested URL                     │           │
│  │     │   │   5. Sets JSESSIONID cookie                                         │           │
│  │     │   │                                                                     │           │
│  │     │   │ ★ WE CAN REPLACE THIS with a CUSTOM AuthenticationSuccessHandler! │           │
│  │     │   │                                                                     │           │
│  │     │   │ Custom behavior:                                                    │           │
│  │     │   │   1. Extract the Access Token and ID Token from the                │           │
│  │     │   │      authenticated principal                                        │           │
│  │     │   │   2. Return them in the HTTP response body (JSON)                  │           │
│  │     │   │   3. Do NOT create a session → STATELESS!                          │           │
│  │     │   │                                                                     │           │
│  │     ▼   ▼                                                                     │           │
│  │                                                                               │           │
│  │  ★ The hook point is: .oauth2Login().successHandler(customHandler)           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.13.2 ★ Step 1 — Custom AuthenticationSuccessHandler (Return Tokens in Response)

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  OAuth2LoginSuccessHandler.java
//
//  ★ Custom AuthenticationSuccessHandler that returns the Access Token
//    and ID Token in the response body instead of creating a session.
//
//  ★ In OAuth2LoginAuthenticationFilter, the method onAuthenticationSuccess()
//    is called after successful OAuth login. By default, it uses
//    SavedRequestAwareAuthenticationSuccessHandler which creates a session
//    and redirects. We REPLACE it with this custom handler.
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        // ★ The Authentication object is an OAuth2AuthenticationToken
        // which wraps the OidcUser (for OIDC providers like Google)
        // or OAuth2User (for plain OAuth 2.0 providers like GitHub)

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        // ─────────────────────────────────────────────────────────────
        //  Extract tokens from the authenticated user
        // ─────────────────────────────────────────────────────────────

        String idToken = null;
        String accessToken = null;

        Object principal = oauthToken.getPrincipal();

        if (principal instanceof OidcUser oidcUser) {
            // ★ OIDC Provider (Google, GitLab, Auth0, Keycloak)
            // → Has both ID Token and Access Token

            // ID Token (ALWAYS JWT — per OIDC spec)
            idToken = oidcUser.getIdToken().getTokenValue();

            // Access Token — we need to get it from the OAuth2AuthorizedClient
            // (not directly available on OidcUser)
        }

        // ─────────────────────────────────────────────────────────────
        //  Build the JSON response with tokens
        // ─────────────────────────────────────────────────────────────

        Map<String, Object> tokenResponse = new LinkedHashMap<>();
        tokenResponse.put("id_token", idToken);
        tokenResponse.put("token_type", "Bearer");
        tokenResponse.put("message", "OAuth2 login successful. Use the id_token in " +
                "the Authorization header for subsequent requests: " +
                "Authorization: Bearer <id_token>");

        // ─────────────────────────────────────────────────────────────
        //  Write the response — NO redirect, NO session!
        // ─────────────────────────────────────────────────────────────

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));
        response.getWriter().flush();

        // ★ We do NOT:
        //   - Create an HttpSession
        //   - Set JSESSIONID cookie
        //   - Redirect to any URL
        //   - Store anything in SecurityContextHolder for future requests
        //
        // ★ The client must now store the id_token and send it
        //   in every subsequent request as: Authorization: Bearer <id_token>
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ENHANCED VERSION — Including Access Token from OAuth2AuthorizedClientService             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  If you also need the Access Token (e.g., for calling Google APIs),                          │
│  you need to inject the OAuth2AuthorizedClientService:                                       │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  OAuth2LoginSuccessHandler.java — Enhanced version with Access Token
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuth2LoginSuccessHandler(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        // ─────────────────────────────────────────────────────────────
        //  Get the OAuth2AuthorizedClient (contains all tokens)
        // ─────────────────────────────────────────────────────────────

        // ★ The OAuth2AuthorizedClient was saved by
        //    InMemoryOAuth2AuthorizedClientService during the OAuth flow.
        //    It contains: access_token, refresh_token, client_registration

        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient(
                        oauthToken.getAuthorizedClientRegistrationId(),  // "google"
                        oauthToken.getName()                              // principal name
                );

        // ─────────────────────────────────────────────────────────────
        //  Extract all tokens
        // ─────────────────────────────────────────────────────────────

        String idToken = null;
        String accessToken = null;
        String refreshToken = null;

        // Access Token (JWT or opaque — depends on provider)
        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
            accessToken = authorizedClient.getAccessToken().getTokenValue();
        }

        // Refresh Token (if present)
        if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
            refreshToken = authorizedClient.getRefreshToken().getTokenValue();
        }

        // ID Token (ALWAYS JWT — only available for OIDC providers)
        Object principal = oauthToken.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            idToken = oidcUser.getIdToken().getTokenValue();
        }

        // ─────────────────────────────────────────────────────────────
        //  Build JSON response
        // ─────────────────────────────────────────────────────────────

        Map<String, Object> tokenResponse = new LinkedHashMap<>();
        tokenResponse.put("access_token", accessToken);
        tokenResponse.put("id_token", idToken);
        tokenResponse.put("refresh_token", refreshToken);
        tokenResponse.put("token_type", "Bearer");

        // ─────────────────────────────────────────────────────────────
        //  Write response — STATELESS, no session!
        // ─────────────────────────────────────────────────────────────

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));
        response.getWriter().flush();
    }
}
```

---

##### 20.13.3 ★ Step 2 — SecurityConfig with Custom Success Handler & Stateless Session

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig.java — Stateless OAuth 2.0 Configuration
//
//  ★ Key changes from the default stateful config:
//    1. .oauth2Login() → supply custom successHandler
//    2. .sessionManagement() → set STATELESS (no session creation)
//    3. .addFilterBefore() → add ID Token validation filter
//    4. .csrf().disable() → CSRF not needed for stateless APIs
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final IdTokenValidationFilter idTokenValidationFilter;

    public SecurityConfig(
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            IdTokenValidationFilter idTokenValidationFilter) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.idTokenValidationFilter = idTokenValidationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── 1. Disable CSRF (not needed for stateless token-based auth) ──
            .csrf(csrf -> csrf.disable())

            // ── 2. Make session management STATELESS ──
            // ★ This tells Spring Security: do NOT create HttpSession!
            // ★ No JSESSIONID cookie will be set!
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── 3. Authorization rules ──
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/",
                    "/public/**",
                    "/error",
                    "/login",
                    "/oauth2/**",                           // OAuth2 endpoints
                    "/login/oauth2/code/*"                  // Callback URLs
                ).permitAll()
                .anyRequest().authenticated()
            )

            // ── 4. OAuth2 Login with CUSTOM success handler ──
            // ★ Instead of the default SavedRequestAwareAuthenticationSuccessHandler
            //   (which creates session + redirect), we use our custom handler
            //   that returns tokens in the response body!
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)  // ★ Custom handler!
                // ★ This replaces the default onAuthenticationSuccess() behavior
                // in OAuth2LoginAuthenticationFilter.
                //
                // Default: session → JSESSIONID → redirect
                // Custom:  tokens → JSON response → NO session
            )

            // ── 5. Add ID Token Validation Filter ──
            // ★ This filter intercepts EVERY request (except OAuth endpoints),
            //   reads the "Authorization: Bearer <id_token>" header,
            //   validates the id_token JWT, and sets the SecurityContext.
            //
            // ★ We add it BEFORE UsernamePasswordAuthenticationFilter because
            //   we want token validation to happen early in the filter chain,
            //   before Spring tries any other authentication method.
            .addFilterBefore(
                idTokenValidationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHY .addFilterBefore(idTokenValidationFilter,                                            │
│                          UsernamePasswordAuthenticationFilter.class)?                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The Spring Security filter chain order (simplified):                        │           │
│  │                                                                               │           │
│  │  Request                                                                      │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  SecurityContextHolderFilter        ← creates empty context                 │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  OAuth2AuthorizationRequestRedirectFilter  ← handles /oauth2/authorization  │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  OAuth2LoginAuthenticationFilter    ← handles /login/oauth2/code callback   │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  ★ IdTokenValidationFilter (OUR CUSTOM FILTER)                              │           │
│  │    │  → Reads Authorization: Bearer <id_token> header                       │           │
│  │    │  → Validates the JWT                                                    │           │
│  │    │  → Sets SecurityContextHolder.setAuthentication()                       │           │
│  │    │  → Request is now authenticated!                                         │           │
│  │    │                                                                          │           │
│  │    ▼   (inserted BEFORE ↓)                                                   │           │
│  │  UsernamePasswordAuthenticationFilter  ← we don't use this for OAuth        │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  AuthorizationFilter                ← checks hasRole, hasAuthority          │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  Controller (DispatcherServlet)                                               │           │
│  │                                                                               │           │
│  │  ★ We add our filter BEFORE UsernamePasswordAuthenticationFilter because:   │           │
│  │    1. It's early enough to authenticate before authorization checks         │           │
│  │    2. It's after the OAuth2 filters (so OAuth flow still works)             │           │
│  │    3. It's the conventional position for token validation filters           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.13.4 ★ Step 3 — ID Token Validation Filter (Validates JWT on Every Request)

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  IdTokenValidationFilter.java
//
//  ★ This filter runs on EVERY request (except OAuth endpoints).
//  ★ It reads the ID Token from the "Authorization: Bearer <id_token>" header.
//  ★ Validates the JWT using the Authorization Server's public keys (JWK Set).
//  ★ If valid, creates an Authentication object and sets it in SecurityContext.
//
//  ★ Key class: JwtDecoders.fromIssuerLocation(issuerUri)
//    → This method takes the issuer URI (e.g., "https://accounts.google.com")
//    → Fetches the OIDC discovery document:
//       {issuerUri}/.well-known/openid-configuration
//    → From that JSON, it reads the "jwks_uri" field
//    → Fetches the JWK Set (public keys) from that URI
//    → Creates a JwtDecoder that can verify JWT signatures using those keys
//    → ALL of this happens automatically!
// ═══════════════════════════════════════════════════════════════════════════════

@Component
public class IdTokenValidationFilter extends OncePerRequestFilter {

    // ★ Cache JwtDecoders per issuer to avoid fetching JWK Set on every request
    // Key: issuer URI (e.g., "https://accounts.google.com")
    // Value: JwtDecoder configured with that issuer's public keys
    private final Map<String, JwtDecoder> jwtDecoderCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // ─────────────────────────────────────────────────────────────
        //  1. Extract the Bearer token from the Authorization header
        // ─────────────────────────────────────────────────────────────

        String authHeader = request.getHeader("Authorization");

        // If no Authorization header or not a Bearer token, skip this filter
        // (let the request continue to the next filter in the chain)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);  // Remove "Bearer " prefix

        try {
            // ─────────────────────────────────────────────────────────
            //  2. Decode the JWT WITHOUT validation first to read the issuer
            //     (We need the issuer to know WHICH provider's public keys to use)
            // ─────────────────────────────────────────────────────────

            // ★ Parse the JWT payload to extract the "iss" (issuer) claim
            // The ID Token is a JWT with 3 parts: header.payload.signature
            // We decode the payload (Base64) to read the issuer

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                // Not a valid JWT format
                sendErrorResponse(response, "Invalid token format");
                return;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode claims = mapper.readTree(payload);
            String issuer = claims.get("iss").asText();

            // ─────────────────────────────────────────────────────────
            //  3. Get or create a JwtDecoder for this issuer
            // ─────────────────────────────────────────────────────────

            // ★ JwtDecoders.fromIssuerLocation(issuer) does the following:
            //   a. Fetches {issuer}/.well-known/openid-configuration
            //   b. Reads the "jwks_uri" from the discovery document
            //   c. Fetches the JWK Set (public keys) from jwks_uri
            //   d. Creates a NimbusJwtDecoder configured with those public keys
            //   e. Sets up issuer validation (iss claim must match)
            //
            // ★ We cache the decoder because this HTTP call is expensive!

            JwtDecoder jwtDecoder = jwtDecoderCache.computeIfAbsent(
                    issuer,
                    iss -> JwtDecoders.fromIssuerLocation(iss)
            );

            // ─────────────────────────────────────────────────────────
            //  4. Validate and decode the ID Token
            // ─────────────────────────────────────────────────────────

            // ★ jwtDecoder.decode() does ALL of the following:
            //   a. Verifies the JWT signature using the provider's public key
            //      (from the JWK Set fetched above)
            //   b. Validates the "iss" (issuer) claim matches expected issuer
            //   c. Validates the "exp" (expiration) claim — token not expired
            //   d. Validates the "iat" (issued at) claim — not in the future
            //   e. Returns a Jwt object with all claims if valid
            //   f. Throws JwtException if ANY validation fails

            Jwt jwt = jwtDecoder.decode(token);

            // ─────────────────────────────────────────────────────────
            //  5. Create an Authentication object and set it in SecurityContext
            // ─────────────────────────────────────────────────────────

            // ★ Extract user details from the JWT claims
            // Common claims in an ID Token:
            //   sub     → subject (unique user ID)
            //   email   → user's email
            //   name    → user's display name
            //   picture → profile picture URL

            // Create granted authorities (you can customize this)
            Collection<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            // ★ Create a JwtAuthenticationToken — this is the Authentication object
            //   that represents the authenticated user
            JwtAuthenticationToken authenticationToken =
                    new JwtAuthenticationToken(jwt, authorities);

            // ★ Set the Authentication in the SecurityContextHolder
            //   → This is the SAME thing that happens in the stateful flow
            //     when SecurityContextHolderFilter loads from session
            //   → Now the user is considered AUTHENTICATED for this request
            //   → AuthorizationFilter will allow access to protected endpoints
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        } catch (JwtException e) {
            // ★ JWT validation failed (expired, bad signature, wrong issuer, etc.)
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, "Invalid or expired token: " + e.getMessage());
            return;
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, "Token validation error");
            return;
        }

        // ─────────────────────────────────────────────────────────────
        //  6. Continue the filter chain — request is now authenticated!
        // ─────────────────────────────────────────────────────────────

        filterChain.doFilter(request, response);
    }

    // ★ Skip this filter for OAuth2 login endpoints
    // (these need to go through the OAuth2 flow, not token validation)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.equals("/login")
                || path.equals("/")
                || path.startsWith("/public/");
    }

    private void sendErrorResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        response.getWriter().flush();
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW JwtDecoders.fromIssuerLocation(issuer) WORKS INTERNALLY                             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  JwtDecoders.fromIssuerLocation("https://accounts.google.com")              │           │
│  │     │                                                                         │           │
│  │     │ 1. Appends /.well-known/openid-configuration to the issuer URI        │           │
│  │     │    → GET https://accounts.google.com/.well-known/openid-configuration │           │
│  │     │                                                                         │           │
│  │     │ 2. Receives the OIDC Discovery Document (JSON):                        │           │
│  │     │    {                                                                    │           │
│  │     │      "issuer": "https://accounts.google.com",                          │           │
│  │     │      "authorization_endpoint": "https://accounts.google.com/o/...",   │           │
│  │     │      "token_endpoint": "https://oauth2.googleapis.com/token",         │           │
│  │     │      ★ "jwks_uri": "https://www.googleapis.com/oauth2/v3/certs",     │           │
│  │     │      "userinfo_endpoint": "https://openidconnect.googleapis.com/...", │           │
│  │     │      ...                                                                │           │
│  │     │    }                                                                    │           │
│  │     │                                                                         │           │
│  │     │ 3. Reads the "jwks_uri" field from the discovery document              │           │
│  │     │    → https://www.googleapis.com/oauth2/v3/certs                        │           │
│  │     │                                                                         │           │
│  │     │ 4. Fetches the JWK Set (JSON Web Key Set) from jwks_uri:              │           │
│  │     │    GET https://www.googleapis.com/oauth2/v3/certs                      │           │
│  │     │    {                                                                    │           │
│  │     │      "keys": [                                                         │           │
│  │     │        {                                                                │           │
│  │     │          "kty": "RSA",                                                 │           │
│  │     │          "kid": "key-id-1",         ← Key ID (matches JWT header kid) │           │
│  │     │          "n": "0vx7agoebGc...",     ← RSA public key modulus         │           │
│  │     │          "e": "AQAB",               ← RSA public key exponent        │           │
│  │     │          "alg": "RS256",            ← Algorithm                       │           │
│  │     │          "use": "sig"               ← Used for signature verification│           │
│  │     │        },                                                               │           │
│  │     │        { ... more keys ... }                                           │           │
│  │     │      ]                                                                 │           │
│  │     │    }                                                                    │           │
│  │     │                                                                         │           │
│  │     │ 5. Creates a NimbusJwtDecoder with:                                    │           │
│  │     │    → The public keys from JWK Set (for signature verification)        │           │
│  │     │    → Issuer validation (iss must match the original issuer URI)       │           │
│  │     │    → Timestamp validation (exp, iat, nbf)                              │           │
│  │     │                                                                         │           │
│  │     │ 6. Returns the configured JwtDecoder                                   │           │
│  │     │                                                                         │           │
│  │     ▼                                                                         │           │
│  │  JwtDecoder ready to validate any JWT from that issuer!                      │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── WHAT jwtDecoder.decode(token) VALIDATES ──                               │           │
│  │                                                                               │           │
│  │  ┌──────────────────┬─────────────────────────────────────────────┐          │           │
│  │  │  Check             │  What it does                              │          │           │
│  │  ├──────────────────┼─────────────────────────────────────────────┤          │           │
│  │  │  Signature         │  Uses the public key (from JWK Set) to    │          │           │
│  │  │                    │  verify the JWT was signed by the provider │          │           │
│  │  │                    │  → Prevents token tampering!              │          │           │
│  │  ├──────────────────┼─────────────────────────────────────────────┤          │           │
│  │  │  Issuer (iss)      │  Checks the "iss" claim matches the       │          │           │
│  │  │                    │  expected issuer URI                       │          │           │
│  │  │                    │  → Prevents tokens from wrong providers!  │          │           │
│  │  ├──────────────────┼─────────────────────────────────────────────┤          │           │
│  │  │  Expiration (exp)  │  Checks the token is not expired          │          │           │
│  │  │                    │  → Prevents use of old tokens!            │          │           │
│  │  ├──────────────────┼─────────────────────────────────────────────┤          │           │
│  │  │  Issued At (iat)   │  Checks the token was not issued in the   │          │           │
│  │  │                    │  future (clock skew tolerance applied)    │          │           │
│  │  ├──────────────────┼─────────────────────────────────────────────┤          │           │
│  │  │  Not Before (nbf)  │  If present, checks current time is after │          │           │
│  │  │                    │  the "not before" timestamp               │          │           │
│  │  └──────────────────┴─────────────────────────────────────────────┘          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.13.5 ★ Complete Stateless Flow Diagram — All Phases

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE STATELESS OAUTH 2.0 FLOW — TOKEN-BASED (NO SESSION)                            │
│    (From OAuth Login to Token-Based API Access)                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  User/SPA          Spring Security              Authorization         Your                  │
│  (Client App)      (Filter Chain)               Server (Google)       Controller             │
│  ───────────       ─────────────                ──────────────        ──────────             │
│     │                   │                             │                   │                  │
│     │  ══ PHASE 1: OAuth 2.0 Login (Same as Stateful!) ═══════════════════                 │
│     │                   │                             │                   │                  │
│     │  GET /oauth2/authorization/google               │                   │                  │
│     │──────────────────>│                             │                   │                  │
│     │                   │ OAuth2AuthorizationRequestRedirectFilter        │                  │
│     │  302 → Google     │                             │                   │                  │
│     │<──────────────────│                             │                   │                  │
│     │                   │                             │                   │                  │
│     │  Browser → Google login page                    │                   │                  │
│     │──────────────────────────────────────────────>│                   │                  │
│     │                   │                             │                   │                  │
│     │         User logs in + consents                 │                   │                  │
│     │                   │                             │                   │                  │
│     │  302 → /login/oauth2/code/google?code=XYZ&state=...               │                  │
│     │<─────────────────────────────────────────────│                   │                  │
│     │                   │                             │                   │                  │
│     │  GET /login/oauth2/code/google?code=XYZ&state=...                 │                  │
│     │──────────────────>│                             │                   │                  │
│     │                   │ OAuth2LoginAuthenticationFilter                 │                  │
│     │                   │ → Validates state            │                   │                  │
│     │                   │ → Calls AuthenticationManager│                   │                  │
│     │                   │                             │                   │                  │
│     │                   │ OidcAuthorizationCodeAuthenticationProvider     │                  │
│     │                   │ POST /token {code, client_id, secret}          │                  │
│     │                   │────────────────────────────>│                   │                  │
│     │                   │                             │                   │                  │
│     │                   │ {access_token, id_token, refresh_token}        │                  │
│     │                   │<────────────────────────────│                   │                  │
│     │                   │                             │                   │                  │
│     │  ══ PHASE 2: ★ DIFFERENT FROM STATEFUL! ════════════════════════════                 │
│     │  ══ Custom AuthenticationSuccessHandler Returns Tokens ═════════════                 │
│     │                   │                             │                   │                  │
│     │                   │ ★ OAuth2LoginSuccessHandler.onAuthenticationSuccess()             │
│     │                   │   (our CUSTOM handler — NOT the default!)      │                  │
│     │                   │                             │                   │                  │
│     │                   │   → Extracts id_token from OidcUser            │                  │
│     │                   │   → Extracts access_token from AuthorizedClient│                  │
│     │                   │   → Builds JSON response                       │                  │
│     │                   │   → ★ Does NOT create HttpSession!             │                  │
│     │                   │   → ★ Does NOT set JSESSIONID cookie!          │                  │
│     │                   │                             │                   │                  │
│     │  200 OK           │                             │                   │                  │
│     │  Content-Type: application/json                 │                   │                  │
│     │  {                │                             │                   │                  │
│     │    "access_token": "eyJhbGci...",               │                   │                  │
│     │    "id_token": "eyJhbGci...",                   │                   │                  │
│     │    "refresh_token": "eyJhbGci...",              │                   │                  │
│     │    "token_type": "Bearer"                       │                   │                  │
│     │  }                │                             │                   │                  │
│     │<──────────────────│                             │                   │                  │
│     │                   │                             │                   │                  │
│     │  ★ Client stores the id_token                   │                   │                  │
│     │  (localStorage, sessionStorage, or secure cookie)│                  │                  │
│     │                   │                             │                   │                  │
│     │                   │                             │                   │                  │
│     │  ══ PHASE 3: ★ SUBSEQUENT REQUESTS WITH TOKEN ══════════════════════                 │
│     │  ══ (Instead of JSESSIONID Cookie) ══════════════════════════════════                 │
│     │                   │                             │                   │                  │
│     │  GET /api/dashboard                             │                   │                  │
│     │  Authorization: Bearer eyJhbGci... (id_token)   │                   │                  │
│     │──────────────────>│                             │                   │                  │
│     │                   │                             │                   │                  │
│     │                   │ ★ IdTokenValidationFilter (OUR CUSTOM FILTER)  │                  │
│     │                   │   │                          │                   │                  │
│     │                   │   │ 1. Reads Authorization: Bearer <token>      │                  │
│     │                   │   │ 2. Decodes JWT payload → reads "iss" claim  │                  │
│     │                   │   │    iss = "https://accounts.google.com"      │                  │
│     │                   │   │                          │                   │                  │
│     │                   │   │ 3. JwtDecoders.fromIssuerLocation(iss)      │                  │
│     │                   │   │    → Fetches /.well-known/openid-configuration               │
│     │                   │   │    → Gets jwks_uri from discovery doc       │                  │
│     │                   │   │    → Fetches public keys from jwks_uri      │                  │
│     │                   │   │    → Creates JwtDecoder (cached!)           │                  │
│     │                   │   │                          │                   │                  │
│     │                   │   │ 4. jwtDecoder.decode(token)                 │                  │
│     │                   │   │    → Verifies signature with public key     │                  │
│     │                   │   │    → Validates iss, exp, iat                │                  │
│     │                   │   │    → Returns Jwt object with claims         │                  │
│     │                   │   │                          │                   │                  │
│     │                   │   │ 5. Creates JwtAuthenticationToken           │                  │
│     │                   │   │ 6. SecurityContextHolder.setAuthentication()│                  │
│     │                   │   │    → User is NOW authenticated!             │                  │
│     │                   │   │                          │                   │                  │
│     │                   │   ▼                          │                   │                  │
│     │                   │ AuthorizationFilter: ✅ OK   │                   │                  │
│     │                   │                             │                   │                  │
│     │                   │────────────────────────────────────────────────>│                  │
│     │                   │                             │                   │                  │
│     │                   │                             │  Controller       │                  │
│     │                   │                             │  processes        │                  │
│     │                   │                             │  request          │                  │
│     │  200 OK (data)    │                             │                   │                  │
│     │<──────────────────│                             │                   │                  │
│     │                   │                             │                   │                  │
│     │                   │                             │                   │                  │
│     │  ★ NO SESSION! NO JSESSIONID! Every request validated independently!                 │
│     │  ★ ANY server instance can handle ANY request (horizontally scalable!)               │
│     │                   │                             │                   │                  │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.13.6 ★ Stateful vs Stateless — Side-by-Side Flow Comparison

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ STATEFUL vs STATELESS — SIDE-BY-SIDE FLOW COMPARISON                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌────────────────────────────────┬────────────────────────────────────────┐                │
│  │  STATEFUL (Default)            │  STATELESS (Custom)                    │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  OAuth Login:                  │  OAuth Login:                          │                │
│  │  SAME — redirect to Google,   │  SAME — redirect to Google,           │                │
│  │  code exchange, get tokens    │  code exchange, get tokens            │                │
│  │                                │                                        │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  After Login:                  │  After Login:                          │                │
│  │  SavedRequestAware             │  ★ OAuth2LoginSuccessHandler          │                │
│  │  AuthenticationSuccessHandler  │    (custom)                            │                │
│  │  → Creates HttpSession        │  → Returns tokens in JSON body        │                │
│  │  → Stores SecurityContext     │  → NO session created                 │                │
│  │  → Sets JSESSIONID cookie     │  → NO JSESSIONID cookie              │                │
│  │  → 302 Redirect to /dashboard │  → 200 OK with token payload         │                │
│  │                                │                                        │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  Subsequent Requests:          │  Subsequent Requests:                  │                │
│  │  Cookie: JSESSIONID=abc123     │  Authorization: Bearer <id_token>     │                │
│  │                                │                                        │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  Validation:                   │  Validation:                           │                │
│  │  SecurityContextHolderFilter   │  ★ IdTokenValidationFilter            │                │
│  │  → Reads JSESSIONID           │    (custom)                            │                │
│  │  → Loads SecurityContext      │  → Reads Bearer token from header     │                │
│  │    from HttpSession           │  → Extracts issuer from JWT payload   │                │
│  │  → Sets Authentication        │  → JwtDecoders.fromIssuerLocation()   │                │
│  │                                │  → Fetches JWK Set (public keys)     │                │
│  │                                │  → Validates signature, exp, iss     │                │
│  │                                │  → Creates JwtAuthenticationToken    │                │
│  │                                │  → SecurityContextHolder             │                │
│  │                                │    .setAuthentication()               │                │
│  │                                │                                        │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  SecurityConfig:               │  SecurityConfig:                       │                │
│  │  .oauth2Login(                 │  .oauth2Login(oauth2 -> oauth2        │                │
│  │    Customizer.withDefaults()   │    .successHandler(customHandler)     │                │
│  │  )                             │  )                                     │                │
│  │                                │  .sessionManagement(session ->        │                │
│  │                                │    session.sessionCreationPolicy(     │                │
│  │                                │      SessionCreationPolicy.STATELESS)│                │
│  │                                │  )                                     │                │
│  │                                │  .addFilterBefore(                     │                │
│  │                                │    idTokenFilter,                      │                │
│  │                                │    UsernamePasswordAuth...Filter.class│                │
│  │                                │  )                                     │                │
│  │                                │                                        │                │
│  └────────────────────────────────┴────────────────────────────────────────┘                │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.13.7 ★ Required Imports for All Classes

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Required imports for the stateless OAuth 2.0 implementation
// ═══════════════════════════════════════════════════════════════════════════════

// ── SecurityConfig ──
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

// ── OAuth2LoginSuccessHandler ──
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import com.fasterxml.jackson.databind.ObjectMapper;

// ── IdTokenValidationFilter ──
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
```

---

##### 20.13.8 ★ Summary — Making OAuth 2.0 Login Stateless

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — STATELESS OAUTH 2.0 LOGIN                                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. DEFAULT BEHAVIOR (Stateful)                                              │           │
│  │     • Spring assumes OAuth 2.0 login is done in a browser                   │           │
│  │     • Creates HttpSession + JSESSIONID cookie after login                   │           │
│  │     • Subsequent requests authenticated via session lookup                  │           │
│  │                                                                               │           │
│  │  2. THE HOOK POINT                                                            │           │
│  │     • OAuth2LoginAuthenticationFilter.onAuthenticationSuccess()              │           │
│  │     • Uses AuthenticationSuccessHandler (default creates session + redirect) │           │
│  │     • ★ We replace it with a custom handler that returns tokens in JSON!    │           │
│  │     • ★ In SecurityConfig: .oauth2Login(o -> o.successHandler(custom))      │           │
│  │                                                                               │           │
│  │  3. CUSTOM AuthenticationSuccessHandler                                       │           │
│  │     • Extracts id_token from OidcUser                                        │           │
│  │     • Extracts access_token from OAuth2AuthorizedClientService               │           │
│  │     • Returns both in the HTTP response body as JSON                        │           │
│  │     • Does NOT create session, does NOT set JSESSIONID                      │           │
│  │                                                                               │           │
│  │  4. ID TOKEN VALIDATION FILTER (IdTokenValidationFilter)                     │           │
│  │     • Extends OncePerRequestFilter                                           │           │
│  │     • Reads "Authorization: Bearer <id_token>" from request header          │           │
│  │     • Decodes JWT payload to extract "iss" (issuer) claim                   │           │
│  │     • Uses JwtDecoders.fromIssuerLocation(iss) to get JwtDecoder            │           │
│  │       → Fetches /.well-known/openid-configuration from issuer              │           │
│  │       → Gets jwks_uri (public keys endpoint)                                │           │
│  │       → Fetches JWK Set (RSA public keys)                                   │           │
│  │       → Creates decoder (cached per issuer!)                                │           │
│  │     • Validates: signature, issuer, expiration, issued-at                   │           │
│  │     • Creates JwtAuthenticationToken                                         │           │
│  │     • Sets SecurityContextHolder.getContext().setAuthentication()            │           │
│  │                                                                               │           │
│  │  5. SECURITY FILTER CHAIN CONFIG                                              │           │
│  │     • .sessionManagement(SessionCreationPolicy.STATELESS)                   │           │
│  │     • .csrf().disable() (not needed for token-based APIs)                   │           │
│  │     • .oauth2Login(o -> o.successHandler(customHandler))                    │           │
│  │     • .addFilterBefore(idTokenFilter, UsernamePasswordAuthFilter.class)     │           │
│  │                                                                               │           │
│  │  6. TOKEN TYPES                                                               │           │
│  │     • Access Token: JWT or opaque (provider decides)                        │           │
│  │     • ★ ID Token: ALWAYS JWT (OIDC spec requirement)                        │           │
│  │       → That's why we can validate ID Token on every request!               │           │
│  │       → JWT is self-contained — no server-side lookup needed               │           │
│  │                                                                               │           │
│  │  7. KEY CLASSES                                                               │           │
│  │     • AuthenticationSuccessHandler — interface to customize post-login      │           │
│  │     • JwtDecoders.fromIssuerLocation() — auto-discovers provider keys       │           │
│  │     • JwtDecoder.decode() — validates JWT signature + claims                │           │
│  │     • JwtAuthenticationToken — Authentication object for JWT-based auth     │           │
│  │     • SecurityContextHolder — holds the current user's authentication       │           │
│  │     • OncePerRequestFilter — base class for custom request filters          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.14 ★ Authorization Code Grant with Custom Authorization Server & Resource Server — Spring Security

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ AUTHORIZATION CODE GRANT WITH CUSTOM AUTHORIZATION SERVER + RESOURCE SERVER              │
│    — BUILDING YOUR OWN OAuth 2.0 / OIDC INFRASTRUCTURE WITH SPRING SECURITY                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  So far we used THIRD-PARTY authorization servers (Google, GitHub, Auth0).                   │
│  Now we build our OWN:                                                                       │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  3 SEPARATE Spring Boot Applications:                                        │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐        │           │
│  │  │  1. AUTHORIZATION SERVER  (Port 9000)                           │        │           │
│  │  │     → spring-security-oauth2-authorization-server               │        │           │
│  │  │     → Issues tokens (access_token, id_token, refresh_token)    │        │           │
│  │  │     → Handles /authorize, /token, /userinfo, /jwks endpoints   │        │           │
│  │  │     → Manages client registrations (which apps can use it)     │        │           │
│  │  │     → Manages user authentication (login page, user store)     │        │           │
│  │  │     → Provides OIDC discovery (/.well-known/openid-config)     │        │           │
│  │  └──────────────────────────────────────────────────────────────────┘        │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐        │           │
│  │  │  2. RESOURCE SERVER  (Port 8090)                                │        │           │
│  │  │     → spring-boot-starter-oauth2-resource-server                │        │           │
│  │  │     → Hosts protected APIs (e.g., /api/users, /api/orders)     │        │           │
│  │  │     → Validates access tokens on every request                 │        │           │
│  │  │     → Uses the authorization server's JWK Set to verify JWTs   │        │           │
│  │  └──────────────────────────────────────────────────────────────────┘        │           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────────────────────────────────────┐        │           │
│  │  │  3. CLIENT APPLICATION  (Port 8080)                             │        │           │
│  │  │     → spring-boot-starter-oauth2-client                         │        │           │
│  │  │     → The web app users interact with                          │        │           │
│  │  │     → Redirects users to authorization server for login        │        │           │
│  │  │     → Exchanges authorization code for tokens                  │        │           │
│  │  │     → Calls resource server APIs using access tokens           │        │           │
│  │  └──────────────────────────────────────────────────────────────────┘        │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.14.1 ★ Architecture Overview — 3 Applications

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ARCHITECTURE — THREE SPRING BOOT APPLICATIONS                                           │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐            │
│  │                                                                              │            │
│  │       ┌──────────────────┐                                                  │            │
│  │       │  User (Browser)  │                                                  │            │
│  │       └────────┬─────────┘                                                  │            │
│  │                │                                                             │            │
│  │                │ 1. GET /dashboard                                           │            │
│  │                ▼                                                             │            │
│  │       ┌──────────────────────────────────┐                                  │            │
│  │       │  CLIENT APPLICATION               │                                  │            │
│  │       │  (Port 8080)                      │                                  │            │
│  │       │                                    │                                  │            │
│  │       │  spring-boot-starter-oauth2-client│                                  │            │
│  │       │                                    │                                  │            │
│  │       │  Responsibilities:                │                                  │            │
│  │       │  • Redirect to auth server        │                                  │            │
│  │       │  • Exchange code for tokens       │                                  │            │
│  │       │  • Call resource server with      │                                  │            │
│  │       │    access token                   │                                  │            │
│  │       └───────┬────────────────┬──────────┘                                  │            │
│  │               │                │                                              │            │
│  │    2. Redirect│     5. Call    │                                              │            │
│  │    to login   │     API with   │                                              │            │
│  │               │     Bearer     │                                              │            │
│  │               │     token      │                                              │            │
│  │               ▼                ▼                                              │            │
│  │  ┌────────────────────┐  ┌────────────────────────┐                         │            │
│  │  │ AUTHORIZATION      │  │ RESOURCE SERVER         │                         │            │
│  │  │ SERVER             │  │ (Port 8090)             │                         │            │
│  │  │ (Port 9000)        │  │                         │                         │            │
│  │  │                    │  │ spring-boot-starter-    │                         │            │
│  │  │ spring-security-   │  │ oauth2-resource-server  │                         │            │
│  │  │ oauth2-            │  │                         │                         │            │
│  │  │ authorization-     │  │ Responsibilities:       │                         │            │
│  │  │ server             │  │ • Host protected APIs   │                         │            │
│  │  │                    │  │ • Validate access tokens│                         │            │
│  │  │ Responsibilities:  │  │ • Use JWK Set from auth │                         │            │
│  │  │ • Login page       │  │   server to verify JWTs│                         │            │
│  │  │ • Issue tokens     │  │                         │                         │            │
│  │  │ • /authorize       │  └───────────┬─────────────┘                         │            │
│  │  │ • /token           │              │                                        │            │
│  │  │ • /jwks            │    6. Fetch  │                                        │            │
│  │  │ • /userinfo        │    JWK Set   │                                        │            │
│  │  │ • /.well-known/    │    to verify │                                        │            │
│  │  │   openid-config    │    JWT       │                                        │            │
│  │  │                    │◄─────────────┘                                        │            │
│  │  └────────────────────┘                                                      │            │
│  │                                                                              │            │
│  └─────────────────────────────────────────────────────────────────────────────┘            │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.14.2 ★ Application 1 — Authorization Server (Port 9000)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ APPLICATION 1: AUTHORIZATION SERVER                                                       │
│    This is the server that authenticates users and issues tokens.                            │
│    It replaces Google/GitHub/Auth0 — YOU are the identity provider now!                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Dependency: spring-security-oauth2-authorization-server                                    │
│  Port: 9000                                                                                  │
│                                                                                              │
│  Endpoints provided automatically:                                                           │
│  ┌──────────────────────────────────────────────────────────────────────┐                   │
│  │  /oauth2/authorize           → Authorization endpoint (user login)  │                   │
│  │  /oauth2/token               → Token endpoint (code → tokens)      │                   │
│  │  /oauth2/jwks                → JWK Set (public keys for JWT verify)│                   │
│  │  /userinfo                    → User info endpoint (OIDC)           │                   │
│  │  /oauth2/revoke              → Token revocation                     │                   │
│  │  /oauth2/introspect          → Token introspection                  │                   │
│  │  /.well-known/openid-configuration → OIDC Discovery                │                   │
│  │  /connect/register            → Dynamic client registration (OIDC) │                   │
│  └──────────────────────────────────────────────────────────────────────┘                   │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**pom.xml — Authorization Server**

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Authorization Server Dependencies                                  -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- ★ Spring Authorization Server — the core dependency!
         This provides ALL OAuth 2.0 / OIDC authorization server functionality.
         It's a separate project from Spring Security, specifically designed
         to be a full-featured authorization server.
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
    </dependency>

    <!-- Spring Boot Starter Security (pulled transitively, but explicit is good) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

</dependencies>
```

**application.yml — Authorization Server**

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — Authorization Server (Port 9000)
# ═══════════════════════════════════════════════════════════════════════════════

server:
  port: 9000

logging:
  level:
    org.springframework.security: DEBUG    # ★ Helpful for debugging OAuth flows
```

**AuthorizationServerConfig.java — Core Configuration**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AuthorizationServerConfig.java — Authorization Server Configuration
//
//  ★ This configures the OAuth 2.0 / OIDC Authorization Server.
//  ★ It defines:
//    1. Authorization server security filter chain (OAuth endpoints)
//    2. Registered clients (which apps can request tokens)
//    3. User store (who can log in)
//    4. JWK source (key pair for signing JWTs)
//    5. Authorization server settings (issuer URI, endpoints)
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    // ══════════════════════════════════════════════════════════════════════════
    //  1. AUTHORIZATION SERVER SECURITY FILTER CHAIN
    //     → Handles all OAuth 2.0 / OIDC protocol endpoints
    //     → /oauth2/authorize, /oauth2/token, /oauth2/jwks, etc.
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    @Order(1)  // ★ Higher priority — handles OAuth endpoints FIRST
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {

        // ★ Apply default OAuth 2.0 Authorization Server configuration
        // This auto-configures ALL OAuth 2.0 endpoints:
        //   /oauth2/authorize, /oauth2/token, /oauth2/jwks,
        //   /oauth2/revoke, /oauth2/introspect
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // ★ Enable OpenID Connect 1.0 (OIDC) support
        // This adds: /userinfo, /connect/register,
        //            /.well-known/openid-configuration
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        // ★ If user is not authenticated when accessing OAuth endpoints,
        //   redirect to the login page
        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
        );

        // ★ Enable the resource server to accept access tokens
        //   (needed for /userinfo endpoint which requires an access token)
        http.oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(Customizer.withDefaults())
        );

        return http.build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  2. DEFAULT SECURITY FILTER CHAIN
    //     → Handles ALL other requests (login page, static resources, etc.)
    //     → Uses form login for user authentication
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    @Order(2)  // ★ Lower priority — handles non-OAuth requests
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()          // All requests need authentication
            )
            .formLogin(Customizer.withDefaults());     // ★ Standard form login page
            // This is the login page where users enter username/password
            // when redirected from /oauth2/authorize

        return http.build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  3. REGISTERED CLIENT REPOSITORY
    //     → Defines which CLIENT APPLICATIONS are allowed to use this
    //       authorization server.
    //     → This is the equivalent of registering your app on
    //       Google Developer Console / GitHub Developer Settings.
    //     → Each RegisteredClient has: client_id, client_secret, scopes,
    //       redirect_uris, grant_types, etc.
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())

                // ★ client-id: The public identifier for the client app
                // This is what the client sends in the authorization request
                .clientId("my-client-app")

                // ★ client-secret: The secret used for back-channel token exchange
                // Must be encoded with PasswordEncoder (BCrypt here)
                // The client app stores this in its application.yml
                .clientSecret(passwordEncoder().encode("my-client-secret"))

                // ★ client-authentication-method: How the client proves its identity
                // CLIENT_SECRET_BASIC → sends client_id:client_secret as
                //   HTTP Basic Auth header (Base64 encoded)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)

                // ★ authorization-grant-type: Which OAuth flows this client can use
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)

                // ★ redirect-uri: Where the auth server sends the user AFTER login
                // This MUST match what the client app has configured!
                // The auth server validates this to prevent authorization code theft
                .redirectUri("http://localhost:8080/login/oauth2/code/my-auth-server")

                // ★ post-logout-redirect-uri: Where to send user after logout
                .postLogoutRedirectUri("http://localhost:8080/")

                // ★ scopes: What permissions this client can request
                // "openid" → OIDC mode (returns id_token)
                // "profile", "email" → user info claims
                // "read", "write" → custom resource scopes
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .scope("read")
                .scope("write")

                // ★ Client settings
                .clientSettings(ClientSettings.builder()
                        // Require user to approve/consent to the requested scopes
                        .requireAuthorizationConsent(true)
                        // Require PKCE for public clients (SPAs, mobile)
                        // Set to false for confidential clients (server-side)
                        .requireProofKey(false)
                        .build()
                )

                // ★ Token settings
                .tokenSettings(TokenSettings.builder()
                        // Access token lifetime (how long it's valid)
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        // Refresh token lifetime
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        // Access token format: JWT (self-contained) vs OPAQUE (reference)
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)  // JWT
                        // Reuse refresh tokens or issue new ones
                        .reuseRefreshTokens(false)
                        // ID token signature algorithm
                        .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
                        .build()
                )
                .build();

        // ★ InMemoryRegisteredClientRepository for development
        // For production, use JdbcRegisteredClientRepository with a database
        return new InMemoryRegisteredClientRepository(webClient);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  4. USER DETAILS SERVICE
    //     → Defines the users who can log in to this authorization server
    //     → Same as any Spring Security UserDetailsService
    //     → In production, use JdbcUserDetailsManager or a custom implementation
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin"))
                .roles("USER", "ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  5. JWK SOURCE (JSON Web Key)
    //     → RSA key pair used to SIGN JWTs (access tokens, id tokens)
    //     → Private key: signs the token (kept secret on auth server)
    //     → Public key: published at /oauth2/jwks endpoint
    //       so resource servers can VERIFY the token signature
    //     → This is how the resource server trusts tokens from this auth server
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // Generate an RSA key pair
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        // Build a JWK (JSON Web Key) from the RSA key pair
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())  // Unique key ID
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    // ★ Generate RSA key pair (2048-bit)
    // In production, load from a keystore file or secrets manager!
    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    // ★ JwtDecoder for the auth server itself (to validate tokens at /userinfo)
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  6. AUTHORIZATION SERVER SETTINGS
    //     → Configures the issuer URI and endpoint paths
    //     → The issuer URI is included in every token as the "iss" claim
    //     → Resource servers use this to discover the JWK Set endpoint
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")  // ★ Issuer URI — appears in JWT "iss" claim
                // All endpoints below are auto-configured with default paths.
                // You can customize them here if needed:
                // .authorizationEndpoint("/oauth2/authorize")
                // .tokenEndpoint("/oauth2/token")
                // .jwkSetEndpoint("/oauth2/jwks")
                // .oidcUserInfoEndpoint("/userinfo")
                // .tokenRevocationEndpoint("/oauth2/revoke")
                // .tokenIntrospectionEndpoint("/oauth2/introspect")
                // .oidcClientRegistrationEndpoint("/connect/register")
                .build();
    }
}
```

**Required Imports — Authorization Server**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Imports for AuthorizationServerConfig.java
// ═══════════════════════════════════════════════════════════════════════════════

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;
```

---

##### 20.14.3 ★ Application 2 — Resource Server (Port 8090)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ APPLICATION 2: RESOURCE SERVER                                                            │
│    This server hosts your protected APIs.                                                    │
│    It validates the access token from the authorization server on every request.             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  How it works:                                                               │           │
│  │                                                                               │           │
│  │  1. Client sends: GET /api/users                                             │           │
│  │     Authorization: Bearer <access_token>                                     │           │
│  │                                                                               │           │
│  │  2. Resource server reads the Bearer token from the header                   │           │
│  │                                                                               │           │
│  │  3. Resource server fetches the JWK Set (public keys) from:                  │           │
│  │     http://localhost:9000/oauth2/jwks                                        │           │
│  │     (the authorization server's JWK endpoint)                                │           │
│  │                                                                               │           │
│  │  4. Uses the public key to verify the JWT signature                          │           │
│  │                                                                               │           │
│  │  5. Validates claims: iss, exp, aud, scope                                   │           │
│  │                                                                               │           │
│  │  6. If valid → creates JwtAuthenticationToken → request proceeds            │           │
│  │     If invalid → 401 Unauthorized                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**pom.xml — Resource Server**

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Resource Server Dependencies                                       -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- ★ Spring Boot Starter OAuth2 Resource Server
         This provides:
         → JWT token validation (signature, claims)
         → JWK Set fetching (from authorization server)
         → BearerTokenAuthenticationFilter (reads Authorization header)
         → JwtAuthenticationProvider (validates JWT and creates Authentication)
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

</dependencies>
```

**application.yml — Resource Server**

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — Resource Server (Port 8090)
# ═══════════════════════════════════════════════════════════════════════════════

server:
  port: 8090

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # ★ issuer-uri: Points to the authorization server
          # Spring will auto-discover the JWK Set endpoint by fetching:
          #   http://localhost:9000/.well-known/openid-configuration
          # From the discovery doc, it reads "jwks_uri" and fetches public keys.
          issuer-uri: http://localhost:9000

          # ★ Alternative: Specify the JWK Set URI directly
          # (use this if the auth server doesn't support OIDC discovery)
          # jwk-set-uri: http://localhost:9000/oauth2/jwks
```

**ResourceServerConfig.java**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ResourceServerConfig.java — Resource Server Security Configuration
//
//  ★ This configures the resource server to validate JWT access tokens.
//  ★ Spring Security auto-registers:
//    → BearerTokenAuthenticationFilter (reads Authorization: Bearer <token>)
//    → JwtAuthenticationProvider (validates JWT using JWK Set)
//    → JwtDecoder (configured from issuer-uri in application.yml)
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints — no token required
                .requestMatchers("/api/public/**").permitAll()

                // ★ Scope-based authorization
                // The access token contains a "scope" claim with the granted scopes.
                // hasAuthority("SCOPE_read") checks if the JWT has scope "read"
                // Spring Security auto-prefixes scopes with "SCOPE_"
                .requestMatchers(HttpMethod.GET, "/api/**")
                    .hasAuthority("SCOPE_read")
                .requestMatchers(HttpMethod.POST, "/api/**")
                    .hasAuthority("SCOPE_write")
                .requestMatchers(HttpMethod.PUT, "/api/**")
                    .hasAuthority("SCOPE_write")
                .requestMatchers(HttpMethod.DELETE, "/api/**")
                    .hasAuthority("SCOPE_write")

                .anyRequest().authenticated()
            )

            // ★ Enable OAuth 2.0 Resource Server with JWT validation
            // This one line does everything:
            //   → Registers BearerTokenAuthenticationFilter
            //   → Creates JwtDecoder from issuer-uri in application.yml
            //   → Fetches JWK Set from authorization server
            //   → Validates JWT signature, iss, exp on every request
            //   → Creates JwtAuthenticationToken with claims and scopes
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}
```

**Resource Server Controller — Protected APIs**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ApiController.java — Protected API endpoints on the Resource Server
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api")
public class ApiController {

    // ★ GET /api/users — requires scope "read"
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @AuthenticationPrincipal Jwt jwt) {

        // ★ The Jwt object contains all claims from the access token
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Protected resource accessed successfully!");
        response.put("subject", jwt.getSubject());           // "sub" claim (user ID)
        response.put("issuer", jwt.getIssuer().toString());  // "iss" claim
        response.put("scopes", jwt.getClaimAsStringList("scope"));
        response.put("users", List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@example.com"),
                Map.of("id", 2, "name", "Bob", "email", "bob@example.com")
        ));

        return ResponseEntity.ok(response);
    }

    // ★ POST /api/users — requires scope "write"
    @PostMapping("/users")
    public ResponseEntity<Map<String, String>> createUser(
            @RequestBody Map<String, String> user,
            @AuthenticationPrincipal Jwt jwt) {

        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", "User created by: " + jwt.getSubject());
        response.put("name", user.get("name"));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

##### 20.14.4 ★ Application 3 — Client Application (Port 8080)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ APPLICATION 3: CLIENT APPLICATION                                                         │
│    This is the web app that users interact with.                                             │
│    It redirects users to the authorization server for login, gets tokens,                    │
│    and calls the resource server APIs using the access token.                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  This is EXACTLY the same as configuring Google/GitHub as a provider!        │           │
│  │  The ONLY difference: instead of Google's endpoints, we point to             │           │
│  │  our own authorization server at localhost:9000.                              │           │
│  │                                                                               │           │
│  │  Since our auth server is NOT a built-in provider (not in                    │           │
│  │  CommonOAuth2Provider enum), we must provide the provider                    │           │
│  │  endpoints manually OR use issuer-uri for auto-discovery.                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**pom.xml — Client Application**

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Client Application Dependencies                                    -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- ★ Spring Boot Starter OAuth2 Client
         Same dependency used for Google/GitHub login!
         Now configured to point to our custom authorization server.
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- ★ WebClient (reactive) for making API calls to resource server
         WebClient is the recommended HTTP client for OAuth 2.0 token propagation.
         Spring Security integrates with WebClient to auto-attach Bearer tokens.
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

</dependencies>
```

**application.yml — Client Application**

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — Client Application (Port 8080)
#
#  ★ This is a custom provider (not Google/GitHub), so we must configure
#    the provider endpoints. We use issuer-uri for OIDC auto-discovery.
# ═══════════════════════════════════════════════════════════════════════════════

server:
  port: 8080

spring:
  security:
    oauth2:
      client:

        registration:
          # ★ "my-auth-server" is the registrationId — your chosen key
          #   It appears in the redirect URI and login page
          my-auth-server:
            # ★ client-id and client-secret MUST match what's registered
            #   on the authorization server (RegisteredClient)
            client-id: my-client-app
            client-secret: my-client-secret

            # ★ Maps to the provider section below
            provider: my-auth-server

            # ★ Scopes to request — must be a subset of what the auth
            #   server allows for this client
            scope:
              - openid       # OIDC — returns id_token
              - profile      # User's name, etc.
              - email        # User's email
              - read         # Custom scope — access to read APIs
              - write        # Custom scope — access to write APIs

            # ★ Grant type — authorization_code for the redirect flow
            authorization-grant-type: authorization_code

            # ★ Redirect URI — where auth server sends user after login
            #   Must match the redirectUri registered on the auth server!
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            # Expands to: http://localhost:8080/login/oauth2/code/my-auth-server

            # ★ How to send client credentials in the token request
            client-authentication-method: client_secret_basic

            # ★ Display name on the auto-generated login page
            client-name: My Custom Auth Server

        provider:
          # ★ Provider configuration — points to our authorization server
          my-auth-server:
            # ★ issuer-uri: OIDC auto-discovery!
            # Spring fetches: http://localhost:9000/.well-known/openid-configuration
            # and auto-discovers ALL endpoints:
            #   → authorization-uri (/oauth2/authorize)
            #   → token-uri (/oauth2/token)
            #   → jwk-set-uri (/oauth2/jwks)
            #   → user-info-uri (/userinfo)
            issuer-uri: http://localhost:9000

            # ★ Alternative: Specify endpoints manually (if no OIDC discovery)
            # authorization-uri: http://localhost:9000/oauth2/authorize
            # token-uri: http://localhost:9000/oauth2/token
            # jwk-set-uri: http://localhost:9000/oauth2/jwks
            # user-info-uri: http://localhost:9000/userinfo
            # user-name-attribute: sub
```

**ClientSecurityConfig.java**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ClientSecurityConfig.java — Client Application Security Configuration
//
//  ★ Same as configuring Google/GitHub login!
//  ★ .oauth2Login() enables the entire Authorization Code flow:
//    → Auto login page with "My Custom Auth Server" link
//    → Redirect to http://localhost:9000/oauth2/authorize
//    → Handle callback at /login/oauth2/code/my-auth-server
//    → Exchange code for tokens at http://localhost:9000/oauth2/token
//    → Parse tokens and create SecurityContext
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class ClientSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/public/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            // ★ Enable OAuth 2.0 Login — same as with Google/GitHub!
            .oauth2Login(Customizer.withDefaults())
            // ★ Enable OAuth 2.0 Client for making authenticated API calls
            // This allows using OAuth2AuthorizedClientManager to get
            // access tokens for calling the resource server
            .oauth2Client(Customizer.withDefaults());

        return http.build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  WebClient configured to auto-attach Bearer access tokens
    //  when making API calls to the resource server.
    //
    //  ★ ServletOAuth2AuthorizedClientExchangeFilterFunction:
    //    → Automatically reads the access token from the current
    //      OAuth2AuthorizedClient (stored by InMemoryOAuth2AuthorizedClientService)
    //    → Attaches it as "Authorization: Bearer <access_token>" header
    //    → You don't manually handle tokens at all!
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        // ★ Use the default OAuth2AuthorizedClient for all requests
        // This means WebClient will auto-attach the current user's access token
        oauth2Client.setDefaultOAuth2AuthorizedClient(true);

        return WebClient.builder()
                .apply(oauth2Client.oauth2Configuration())
                .build();
    }

    // ★ OAuth2AuthorizedClientManager manages access tokens:
    //   → Gets tokens from OAuth2AuthorizedClientService
    //   → Auto-refreshes expired tokens using refresh_token
    //   → Handles re-authorization if needed
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }
}
```

**Client Controller — Calls Resource Server APIs**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  DashboardController.java — Client Controller that calls Resource Server
//
//  ★ Uses WebClient (configured with OAuth2 token auto-attachment)
//    to call the resource server APIs.
//  ★ The access token is automatically added to every request!
// ═══════════════════════════════════════════════════════════════════════════════

@Controller
public class DashboardController {

    private final WebClient webClient;

    public DashboardController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping("/dashboard")
    @ResponseBody
    public Map<String, Object> dashboard(
            @AuthenticationPrincipal OidcUser oidcUser) {

        // ─────────────────────────────────────────────────────────
        //  1. Get user info from the ID Token (already available)
        // ─────────────────────────────────────────────────────────

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user_name", oidcUser.getFullName());
        response.put("user_email", oidcUser.getEmail());
        response.put("user_subject", oidcUser.getSubject());

        // ─────────────────────────────────────────────────────────
        //  2. Call the Resource Server API
        //     ★ WebClient auto-attaches the access token!
        //     ★ Header: Authorization: Bearer <access_token>
        // ─────────────────────────────────────────────────────────

        Map<String, Object> apiResponse = webClient
                .get()
                .uri("http://localhost:8090/api/users")  // Resource Server URL
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        response.put("resource_server_response", apiResponse);

        return response;
    }

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return "<h1>Welcome!</h1><a href='/dashboard'>Go to Dashboard (login required)</a>";
    }
}
```

**Required Imports — Client Application**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Imports for ClientSecurityConfig.java
// ═══════════════════════════════════════════════════════════════════════════════

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client
        .ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

// ═══════════════════════════════════════════════════════════════════════════════
//  Imports for DashboardController.java
// ═══════════════════════════════════════════════════════════════════════════════

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;
```

---

##### 20.14.5 ★ Complete Flow Diagram — Custom Auth Server + Resource Server + Client

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE FLOW — CUSTOM AUTH SERVER + RESOURCE SERVER + CLIENT                            │
│    Authorization Code Grant with 3 Spring Boot Applications                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  User           Client App           Authorization        Resource                           │
│  (Browser)      (Port 8080)          Server (Port 9000)   Server (Port 8090)                │
│  ────────       ──────────           ──────────────────   ──────────────────                 │
│     │                │                      │                   │                            │
│     │  ── PHASE 1: User accesses protected resource ──────────────                          │
│     │                │                      │                   │                            │
│     │  GET /dashboard│                      │                   │                            │
│     │───────────────>│                      │                   │                            │
│     │                │ Not authenticated!   │                   │                            │
│     │                │ Redirect to login    │                   │                            │
│     │  302 → /login  │                      │                   │                            │
│     │<───────────────│                      │                   │                            │
│     │                │                      │                   │                            │
│     │  GET /login    │                      │                   │                            │
│     │───────────────>│                      │                   │                            │
│     │                │ DefaultLoginPageGeneratingFilter         │                            │
│     │  200 OK (HTML) │ Shows: [My Custom Auth Server]           │                            │
│     │<───────────────│                      │                   │                            │
│     │                │                      │                   │                            │
│     │  ── PHASE 2: Redirect to Authorization Server ────────────                            │
│     │                │                      │                   │                            │
│     │  Click link    │                      │                   │                            │
│     │  GET /oauth2/authorization/my-auth-server                 │                            │
│     │───────────────>│                      │                   │                            │
│     │                │ OAuth2AuthorizationRequestRedirectFilter  │                            │
│     │                │ Builds auth URL:     │                   │                            │
│     │                │  http://localhost:9000/oauth2/authorize   │                            │
│     │                │  ?response_type=code  │                   │                            │
│     │                │  &client_id=my-client-app                │                            │
│     │                │  &redirect_uri=http://localhost:8080/     │                            │
│     │                │    login/oauth2/code/my-auth-server       │                            │
│     │                │  &scope=openid+profile+email+read+write  │                            │
│     │                │  &state=random_csrf   │                   │                            │
│     │  302 → Auth    │                      │                   │                            │
│     │  Server        │                      │                   │                            │
│     │<───────────────│                      │                   │                            │
│     │                │                      │                   │                            │
│     │  GET http://localhost:9000/oauth2/authorize?...            │                            │
│     │────────────────────────────────────>│                   │                            │
│     │                │                      │                   │                            │
│     │  ── PHASE 3: User logs in on Authorization Server ─────────                           │
│     │                │                      │                   │                            │
│     │                │                Auth server checks:      │                            │
│     │                │                "Is user authenticated?"  │                            │
│     │                │                → No → redirect to /login │                            │
│     │  302 → /login  │                      │                   │                            │
│     │<──────────────────────────────────│                   │                            │
│     │                │                      │                   │                            │
│     │  GET http://localhost:9000/login       │                   │                            │
│     │────────────────────────────────────>│                   │                            │
│     │                │                      │                   │                            │
│     │  200 OK (Form login page)             │                   │                            │
│     │  Username: [________]                 │                   │                            │
│     │  Password: [________]                 │                   │                            │
│     │  [Login]                              │                   │                            │
│     │<──────────────────────────────────│                   │                            │
│     │                │                      │                   │                            │
│     │  POST /login {username=user, password=password}          │                            │
│     │────────────────────────────────────>│                   │                            │
│     │                │                      │                   │                            │
│     │                │                Auth server:             │                            │
│     │                │                → Validates credentials   │                            │
│     │                │                → UserDetailsService      │                            │
│     │                │                → User is authenticated!  │                            │
│     │                │                      │                   │                            │
│     │                │                Shows consent screen:    │                            │
│     │  200 OK        │                "my-client-app wants:"   │                            │
│     │  (Consent page)│                [✓] openid               │                            │
│     │                │                [✓] profile              │                            │
│     │                │                [✓] email                │                            │
│     │                │                [✓] read                 │                            │
│     │                │                [✓] write                │                            │
│     │                │                [Approve] [Deny]         │                            │
│     │<──────────────────────────────────│                   │                            │
│     │                │                      │                   │                            │
│     │  POST (approve consent)               │                   │                            │
│     │────────────────────────────────────>│                   │                            │
│     │                │                      │                   │                            │
│     │  ── PHASE 4: Auth Server redirects back with code ─────────                           │
│     │                │                      │                   │                            │
│     │                │                Auth server generates:   │                            │
│     │                │                → Authorization code     │                            │
│     │                │                → Validates redirect_uri │                            │
│     │  302 → callback│                      │                   │                            │
│     │  http://localhost:8080/login/oauth2/code/my-auth-server   │                            │
│     │  ?code=AUTH_CODE_XYZ&state=random_csrf                    │                            │
│     │<──────────────────────────────────│                   │                            │
│     │                │                      │                   │                            │
│     │  GET /login/oauth2/code/my-auth-server?code=XYZ&state=...│                            │
│     │───────────────>│                      │                   │                            │
│     │                │                      │                   │                            │
│     │  ── PHASE 5: Client exchanges code for tokens (back-channel) ──                       │
│     │                │                      │                   │                            │
│     │                │ OAuth2LoginAuthenticationFilter           │                            │
│     │                │ → Validates state    │                   │                            │
│     │                │                      │                   │                            │
│     │                │ OidcAuthorizationCodeAuthenticationProvider                           │
│     │                │ POST http://localhost:9000/oauth2/token   │                            │
│     │                │ {                    │                   │                            │
│     │                │   grant_type: authorization_code          │                            │
│     │                │   code: AUTH_CODE_XYZ│                   │                            │
│     │                │   client_id: my-client-app               │                            │
│     │                │   client_secret: my-client-secret        │                            │
│     │                │   redirect_uri: http://localhost:8080/... │                            │
│     │                │ }                    │                   │                            │
│     │                │──────────────────────>│                   │                            │
│     │                │                      │                   │                            │
│     │                │                Auth server:             │                            │
│     │                │                → Validates code          │                            │
│     │                │                → Validates client creds  │                            │
│     │                │                → Signs JWT with RSA key  │                            │
│     │                │                → Issues tokens:          │                            │
│     │                │ {                    │                   │                            │
│     │                │   access_token: "eyJ..." (JWT, signed)  │                            │
│     │                │   id_token: "eyJ..."    (JWT, signed)   │                            │
│     │                │   refresh_token: "..."   │               │                            │
│     │                │   token_type: Bearer     │               │                            │
│     │                │   expires_in: 3600       │               │                            │
│     │                │ }                    │                   │                            │
│     │                │<──────────────────────│                   │                            │
│     │                │                      │                   │                            │
│     │                │ → Validates id_token using JWK Set from  │                            │
│     │                │   http://localhost:9000/oauth2/jwks       │                            │
│     │                │ → Creates OidcUser with claims            │                            │
│     │                │ → Stores tokens in AuthorizedClientService│                            │
│     │                │ → Sets SecurityContextHolder              │                            │
│     │                │ → Creates HttpSession + JSESSIONID        │                            │
│     │                │                      │                   │                            │
│     │  302 → /dashboard                    │                   │                            │
│     │  Set-Cookie: JSESSIONID=abc123       │                   │                            │
│     │<───────────────│                      │                   │                            │
│     │                │                      │                   │                            │
│     │  ── PHASE 6: Client calls Resource Server with access token ──                        │
│     │                │                      │                   │                            │
│     │  GET /dashboard│                      │                   │                            │
│     │  Cookie: JSESSIONID=abc123            │                   │                            │
│     │───────────────>│                      │                   │                            │
│     │                │                      │                   │                            │
│     │                │ DashboardController:  │                   │                            │
│     │                │ Uses WebClient to call Resource Server    │                            │
│     │                │                      │                   │                            │
│     │                │ GET http://localhost:8090/api/users       │                            │
│     │                │ Authorization: Bearer <access_token>     │                            │
│     │                │ (★ auto-attached by WebClient!)          │                            │
│     │                │───────────────────────────────────────>│                            │
│     │                │                      │                   │                            │
│     │                │                      │  Resource Server: │                            │
│     │                │                      │  BearerTokenAuth  │                            │
│     │                │                      │  enticationFilter │                            │
│     │                │                      │  → Reads Bearer   │                            │
│     │                │                      │    token from     │                            │
│     │                │                      │    header         │                            │
│     │                │                      │  → Fetches JWK Set│                            │
│     │                │                      │    from auth      │                            │
│     │                │                      │    server's       │                            │
│     │                │                      │    /oauth2/jwks   │                            │
│     │                │                      │  → Verifies JWT   │                            │
│     │                │                      │    signature      │                            │
│     │                │                      │  → Validates iss, │                            │
│     │                │                      │    exp, scope     │                            │
│     │                │                      │  → ✅ Valid!       │                            │
│     │                │                      │                   │                            │
│     │                │ 200 OK {users: [...]}│                   │                            │
│     │                │<───────────────────────────────────────│                            │
│     │                │                      │                   │                            │
│     │  200 OK (dashboard with user data + API response)        │                            │
│     │<───────────────│                      │                   │                            │
│     │                │                      │                   │                            │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.14.6 ★ How the Resource Server Validates Tokens from the Authorization Server

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW THE RESOURCE SERVER TRUSTS THE AUTHORIZATION SERVER                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The resource server NEVER talks to the authorization server during          │           │
│  │  normal API calls. It validates tokens LOCALLY using public keys.            │           │
│  │                                                                               │           │
│  │  ── THE TRUST CHAIN ──                                                       │           │
│  │                                                                               │           │
│  │  Authorization Server (Port 9000)                                            │           │
│  │     │                                                                         │           │
│  │     │ 1. Generates RSA key pair at startup:                                  │           │
│  │     │    → Private key: kept secret, used to SIGN JWTs                      │           │
│  │     │    → Public key: published at /oauth2/jwks                            │           │
│  │     │                                                                         │           │
│  │     │ 2. When issuing tokens:                                                │           │
│  │     │    → Signs the JWT with the PRIVATE key                               │           │
│  │     │    → Includes "iss": "http://localhost:9000" in claims                │           │
│  │     │    → Includes "kid" (key ID) in JWT header                            │           │
│  │     │                                                                         │           │
│  │     │ 3. Publishes public keys at:                                           │           │
│  │     │    GET http://localhost:9000/oauth2/jwks                               │           │
│  │     │    Response: { "keys": [{ "kty":"RSA", "n":"...", "e":"..." }] }     │           │
│  │     │                                                                         │           │
│  │     ▼                                                                         │           │
│  │  Resource Server (Port 8090)                                                 │           │
│  │     │                                                                         │           │
│  │     │ 1. On startup (or first request):                                      │           │
│  │     │    → Reads issuer-uri from application.yml                            │           │
│  │     │    → Fetches /.well-known/openid-configuration from auth server       │           │
│  │     │    → Reads "jwks_uri" from discovery document                         │           │
│  │     │    → Fetches JWK Set (public keys) from jwks_uri                      │           │
│  │     │    → Caches the public keys                                            │           │
│  │     │                                                                         │           │
│  │     │ 2. On every API request:                                               │           │
│  │     │    → Reads "Authorization: Bearer <jwt>" header                       │           │
│  │     │    → Uses cached public key to verify JWT signature                   │           │
│  │     │    → Checks "iss" matches configured issuer-uri                       │           │
│  │     │    → Checks "exp" not expired                                          │           │
│  │     │    → Extracts "scope" claim for authorization                         │           │
│  │     │    → Creates JwtAuthenticationToken                                    │           │
│  │     │                                                                         │           │
│  │     │ ★ NO network call to auth server during validation!                   │           │
│  │     │ ★ Public key is cached — validation is purely LOCAL!                  │           │
│  │     │ ★ Only fetches JWK Set once (with periodic refresh)                   │           │
│  │     │                                                                         │           │
│  │                                                                               │           │
│  │  ── JWT CONTENTS (What the auth server puts in the access token) ──          │           │
│  │                                                                               │           │
│  │  Header:                                                                     │           │
│  │  {                                                                            │           │
│  │    "alg": "RS256",        ← Signing algorithm (RSA + SHA-256)               │           │
│  │    "kid": "key-id-123"    ← Which key from JWK Set was used to sign         │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  Payload:                                                                     │           │
│  │  {                                                                            │           │
│  │    "iss": "http://localhost:9000",     ← Issuer (auth server)               │           │
│  │    "sub": "user",                      ← Subject (username)                 │           │
│  │    "aud": "my-client-app",             ← Audience (client_id)               │           │
│  │    "scope": "openid profile email read write",  ← Granted scopes           │           │
│  │    "iat": 1715184000,                  ← Issued at (timestamp)              │           │
│  │    "exp": 1715187600                   ← Expires at (timestamp)             │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  Signature:                                                                   │           │
│  │    RS256(header + "." + payload, PRIVATE_KEY)                                │           │
│  │    → Resource server verifies: RS256(header + "." + payload, PUBLIC_KEY)     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.14.7 ★ Mapping Between Authorization Server Config and Client Config

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ HOW CONFIGS MAP BETWEEN AUTHORIZATION SERVER AND CLIENT                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Authorization Server (RegisteredClient)    Client App (application.yml)     │           │
│  │  ─────────────────────────────────────      ─────────────────────────────    │           │
│  │                                                                               │           │
│  │  .clientId("my-client-app")           →    client-id: my-client-app          │           │
│  │                                              ★ MUST MATCH!                    │           │
│  │                                                                               │           │
│  │  .clientSecret(encode("my-client     →    client-secret: my-client-secret    │           │
│  │     -secret"))                                ★ MUST MATCH (plain text on     │           │
│  │                                              client, encoded on server)       │           │
│  │                                                                               │           │
│  │  .clientAuthenticationMethod(         →    client-authentication-method:      │           │
│  │     CLIENT_SECRET_BASIC)                     client_secret_basic              │           │
│  │                                              ★ MUST MATCH!                    │           │
│  │                                                                               │           │
│  │  .authorizationGrantType(             →    authorization-grant-type:          │           │
│  │     AUTHORIZATION_CODE)                      authorization_code              │           │
│  │                                              ★ MUST MATCH!                    │           │
│  │                                                                               │           │
│  │  .redirectUri("http://localhost:8080/ →    redirect-uri: "{baseUrl}/login/    │           │
│  │     login/oauth2/code/my-auth-server")       oauth2/code/{registrationId}"   │           │
│  │                                              ★ MUST MATCH after expansion!   │           │
│  │                                                                               │           │
│  │  .scope(OidcScopes.OPENID)           →    scope:                             │           │
│  │  .scope(OidcScopes.PROFILE)                 - openid                          │           │
│  │  .scope(OidcScopes.EMAIL)                   - profile                         │           │
│  │  .scope("read")                             - email                           │           │
│  │  .scope("write")                            - read                            │           │
│  │                                              - write                          │           │
│  │                                              ★ Client can request a SUBSET   │           │
│  │                                              of server-allowed scopes         │           │
│  │                                                                               │           │
│  │  AuthorizationServerSettings          →    provider:                          │           │
│  │  .issuer("http://localhost:9000")          my-auth-server:                   │           │
│  │                                               issuer-uri: http://localhost:9000│           │
│  │                                              ★ MUST MATCH!                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.14.8 ★ Summary — Custom Authorization Server + Resource Server + Client

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — BUILDING YOUR OWN OAUTH 2.0 / OIDC INFRASTRUCTURE                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── 3 SEPARATE SPRING BOOT APPLICATIONS ────────────────────────             │           │
│  │                                                                               │           │
│  │  1. AUTHORIZATION SERVER (Port 9000)                                         │           │
│  │     • Dependency: spring-boot-starter-oauth2-authorization-server            │           │
│  │     • Configures: RegisteredClientRepository, UserDetailsService,            │           │
│  │       JWKSource, AuthorizationServerSettings                                 │           │
│  │     • Two SecurityFilterChains: @Order(1) for OAuth endpoints,               │           │
│  │       @Order(2) for form login                                                │           │
│  │     • Auto-provides: /oauth2/authorize, /oauth2/token, /oauth2/jwks,        │           │
│  │       /userinfo, /.well-known/openid-configuration                           │           │
│  │     • Signs JWTs with RSA private key, publishes public key at /jwks        │           │
│  │                                                                               │           │
│  │  2. RESOURCE SERVER (Port 8090)                                               │           │
│  │     • Dependency: spring-boot-starter-oauth2-resource-server                 │           │
│  │     • Config: issuer-uri pointing to authorization server                    │           │
│  │     • Auto-fetches JWK Set and validates JWT access tokens                  │           │
│  │     • .oauth2ResourceServer(oauth2 -> oauth2.jwt(...))                      │           │
│  │     • Uses hasAuthority("SCOPE_read") for scope-based authorization         │           │
│  │     • NO session needed — purely stateless token validation                 │           │
│  │                                                                               │           │
│  │  3. CLIENT APPLICATION (Port 8080)                                            │           │
│  │     • Dependency: spring-boot-starter-oauth2-client                           │           │
│  │     • Config: registration + provider pointing to authorization server       │           │
│  │     • .oauth2Login() + .oauth2Client() in SecurityFilterChain                │           │
│  │     • WebClient with ServletOAuth2AuthorizedClientExchangeFilterFunction     │           │
│  │       for auto-attaching Bearer tokens to API calls                          │           │
│  │     • OAuth2AuthorizedClientManager for token lifecycle management           │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── KEY CONFIGURATION RULES ────────────────────────────────────────         │           │
│  │                                                                               │           │
│  │  ★ client-id on client MUST match clientId on auth server                   │           │
│  │  ★ client-secret on client MUST match clientSecret on auth server           │           │
│  │  ★ redirect-uri on client MUST match redirectUri on auth server             │           │
│  │  ★ scopes on client must be a SUBSET of scopes on auth server              │           │
│  │  ★ issuer-uri on client and resource server MUST match issuer on auth server│           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── JWT TRUST CHAIN ────────────────────────────────────────────────         │           │
│  │                                                                               │           │
│  │  Auth Server: signs JWT with RSA private key                                │           │
│  │       ↓ publishes public key at /oauth2/jwks                                │           │
│  │  Resource Server: fetches public key from /oauth2/jwks                      │           │
│  │       → Verifies JWT signature LOCALLY (no network call per request!)       │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── FLOW SUMMARY ──────────────────────────────────────────────────         │           │
│  │                                                                               │           │
│  │  User → Client App → (redirect) → Auth Server → (login) → (consent)        │           │
│  │  → (code) → Client App → (exchange code for tokens) → Auth Server           │           │
│  │  → (tokens) → Client App → (call API with access_token) → Resource Server  │           │
│  │  → (validate JWT) → (return data) → Client App → User                      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.15 ★ Client Credentials Grant Flow — Spring Security

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ CLIENT CREDENTIALS GRANT — MACHINE-TO-MACHINE (M2M) AUTHENTICATION                      │
│    — NO USER INVOLVED, NO BROWSER, NO LOGIN PAGE, NO REDIRECT                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  What is Client Credentials Grant?                                           │           │
│  │  ─────────────────────────────────                                           │           │
│  │                                                                               │           │
│  │  • An OAuth 2.0 grant type where the CLIENT ITSELF authenticates            │           │
│  │    (not a user).                                                              │           │
│  │  • The client sends its own client_id + client_secret directly              │           │
│  │    to the /token endpoint and gets back an access_token.                     │           │
│  │  • NO authorization code, NO redirect, NO login page, NO consent screen.    │           │
│  │  • The token represents the CLIENT APPLICATION, not a user.                 │           │
│  │                                                                               │           │
│  │  When to use it?                                                              │           │
│  │  ─────────────────                                                           │           │
│  │  • Service-to-service communication (microservices calling each other)      │           │
│  │  • Background jobs / cron jobs / batch processing                            │           │
│  │  • CLI tools that need API access                                            │           │
│  │  • Internal APIs where no human user is involved                            │           │
│  │  • Any "machine-to-machine" (M2M) scenario                                 │           │
│  │                                                                               │           │
│  │  When NOT to use it?                                                          │           │
│  │  ──────────────────                                                          │           │
│  │  • When you need to act on behalf of a USER (use Authorization Code instead)│           │
│  │  • When a browser/user is involved in the flow                              │           │
│  │  • When you need user-specific permissions                                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.1 ★ Client Credentials vs Authorization Code — Key Differences

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ CLIENT CREDENTIALS vs AUTHORIZATION CODE — COMPARISON                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌────────────────────────────────┬────────────────────────────────────────┐                │
│  │  AUTHORIZATION CODE GRANT      │  CLIENT CREDENTIALS GRANT              │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  ★ Involves a USER + CLIENT   │  ★ Involves ONLY the CLIENT           │                │
│  │                                │                                        │                │
│  │  User opens browser            │  No user, no browser                   │                │
│  │  → Redirected to auth server  │  → Client directly calls /token       │                │
│  │  → User logs in               │  → No login page                      │                │
│  │  → User consents              │  → No consent screen                  │                │
│  │  → Auth code returned         │  → No authorization code              │                │
│  │  → Code exchanged for token   │  → Token returned immediately        │                │
│  │                                │                                        │                │
│  │  4-step flow                   │  1-step flow (single POST request!)   │                │
│  │                                │                                        │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  Token represents: USER        │  Token represents: CLIENT APP         │                │
│  │  JWT "sub" = user ID           │  JWT "sub" = client_id                │                │
│  │  Scopes = user permissions     │  Scopes = client permissions          │                │
│  │                                │                                        │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  Use case: Web apps,           │  Use case: Microservices,              │                │
│  │  mobile apps, SPAs            │  background jobs, CLI tools            │                │
│  │                                │                                        │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  Returns: access_token +       │  Returns: access_token ONLY           │                │
│  │  id_token + refresh_token     │  (no id_token — no user to identify!) │                │
│  │                                │  (no refresh_token usually)           │                │
│  │                                │                                        │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  Spring dependency:            │  Spring dependency:                    │                │
│  │  spring-boot-starter-          │  spring-boot-starter-                  │                │
│  │  oauth2-client                 │  oauth2-client                         │                │
│  │                                │  (same dependency, different config!)  │                │
│  │                                │                                        │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  grant_type:                   │  grant_type:                           │                │
│  │  authorization_code            │  client_credentials                    │                │
│  │                                │                                        │                │
│  └────────────────────────────────┴────────────────────────────────────────┘                │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.2 ★ Client Credentials Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ CLIENT CREDENTIALS FLOW — THE SIMPLEST OAUTH 2.0 FLOW                                   │
│    Only 1 request to get a token!                                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐            │
│  │                                                                              │            │
│  │  Client App                Authorization Server          Resource Server    │            │
│  │  (Service A)               (Port 9000)                   (Service B / API) │            │
│  │  ───────────               ────────────────              ────────────────── │            │
│  │      │                           │                             │             │            │
│  │      │                           │                             │             │            │
│  │      │  STEP 1: Request token    │                             │             │            │
│  │      │  ─────────────────────    │                             │             │            │
│  │      │                           │                             │             │            │
│  │      │  POST /oauth2/token       │                             │             │            │
│  │      │  Authorization: Basic     │                             │             │            │
│  │      │    base64(client_id:      │                             │             │            │
│  │      │    client_secret)         │                             │             │            │
│  │      │  Body:                    │                             │             │            │
│  │      │    grant_type=            │                             │             │            │
│  │      │    client_credentials     │                             │             │            │
│  │      │    &scope=read write      │                             │             │            │
│  │      │──────────────────────────>│                             │             │            │
│  │      │                           │                             │             │            │
│  │      │                     Auth Server:                        │             │            │
│  │      │                     → Validates client_id               │             │            │
│  │      │                     → Validates client_secret           │             │            │
│  │      │                     → Checks requested scopes           │             │            │
│  │      │                       are allowed for this client       │             │            │
│  │      │                     → Signs JWT with RSA private key    │             │            │
│  │      │                           │                             │             │            │
│  │      │  STEP 2: Receive token    │                             │             │            │
│  │      │  ─────────────────────    │                             │             │            │
│  │      │                           │                             │             │            │
│  │      │  200 OK                   │                             │             │            │
│  │      │  {                        │                             │             │            │
│  │      │    "access_token": "eyJ..",│                             │             │            │
│  │      │    "token_type": "Bearer",│                             │             │            │
│  │      │    "expires_in": 3600,    │                             │             │            │
│  │      │    "scope": "read write"  │                             │             │            │
│  │      │  }                        │                             │             │            │
│  │      │<──────────────────────────│                             │             │            │
│  │      │                           │                             │             │            │
│  │      │  ★ No id_token! (no user to identify)                  │             │            │
│  │      │  ★ No refresh_token! (client can just re-authenticate) │             │            │
│  │      │                           │                             │             │            │
│  │      │  STEP 3: Call API with token                            │             │            │
│  │      │  ───────────────────────────                            │             │            │
│  │      │                           │                             │             │            │
│  │      │  GET /api/data            │                             │             │            │
│  │      │  Authorization: Bearer    │                             │             │            │
│  │      │    <access_token>         │                             │             │            │
│  │      │──────────────────────────────────────────────────────>│             │            │
│  │      │                           │                             │             │            │
│  │      │                           │                       Resource Server:   │            │
│  │      │                           │                       → Reads Bearer     │            │
│  │      │                           │                         token            │            │
│  │      │                           │                       → Fetches JWK Set  │            │
│  │      │                           │                         from auth server │            │
│  │      │                           │                       → Verifies JWT     │            │
│  │      │                           │                         signature        │            │
│  │      │                           │                       → Validates iss,   │            │
│  │      │                           │                         exp, scope       │            │
│  │      │                           │                       → ✅ Valid!         │            │
│  │      │                           │                             │             │            │
│  │      │  200 OK { data: [...] }   │                             │             │            │
│  │      │<──────────────────────────────────────────────────────│             │            │
│  │      │                           │                             │             │            │
│  │                                                                              │            │
│  └─────────────────────────────────────────────────────────────────────────────┘            │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ★ KEY POINT: The entire flow is just 2 HTTP calls:                          │           │
│  │                                                                               │           │
│  │    1. POST /oauth2/token → get access_token                                 │           │
│  │    2. GET /api/resource  → use access_token                                 │           │
│  │                                                                               │           │
│  │  Compare to Authorization Code which needs 6+ redirects and user interaction!│           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.3 ★ Architecture — 3 Applications for Client Credentials

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ARCHITECTURE — 3 SPRING BOOT APPLICATIONS (Client Credentials)                           │
│    Same 3 apps as Authorization Code, but the CLIENT is a service/microservice!             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐            │
│  │                                                                              │            │
│  │  ┌─────────────────────────────────────────────────────────┐                │            │
│  │  │  1. AUTHORIZATION SERVER  (Port 9000)                    │                │            │
│  │  │     → Same as section 20.14!                             │                │            │
│  │  │     → Just add CLIENT_CREDENTIALS to the RegisteredClient│                │            │
│  │  │     → No redirect_uri needed for this grant type         │                │            │
│  │  │     → Issues access_token (no id_token, no login page)  │                │            │
│  │  └─────────────────────────────────────────────────────────┘                │            │
│  │                         ▲                                                    │            │
│  │                         │ 1. POST /oauth2/token                             │            │
│  │                         │    {grant_type=client_credentials}                │            │
│  │                         │                                                    │            │
│  │  ┌─────────────────────┴───────────────────────────────────┐                │            │
│  │  │  2. CLIENT APPLICATION — "Service A"  (Port 8080)        │                │            │
│  │  │     → spring-boot-starter-oauth2-client                  │                │            │
│  │  │     → A microservice / background job / scheduler        │                │            │
│  │  │     → NO user, NO browser, NO login page                │                │            │
│  │  │     → Authenticates itself using client_id + secret      │                │            │
│  │  │     → Gets access_token from auth server                 │                │            │
│  │  │     → Calls Resource Server APIs with Bearer token       │                │            │
│  │  └─────────────────────┬───────────────────────────────────┘                │            │
│  │                         │                                                    │            │
│  │                         │ 2. GET /api/data                                  │            │
│  │                         │    Authorization: Bearer <token>                  │            │
│  │                         ▼                                                    │            │
│  │  ┌─────────────────────────────────────────────────────────┐                │            │
│  │  │  3. RESOURCE SERVER — "Service B"  (Port 8090)           │                │            │
│  │  │     → Same as section 20.14!                             │                │            │
│  │  │     → Validates JWT access token                         │                │            │
│  │  │     → No changes needed from Authorization Code setup!   │                │            │
│  │  └─────────────────────────────────────────────────────────┘                │            │
│  │                                                                              │            │
│  └─────────────────────────────────────────────────────────────────────────────┘            │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  ★ The RESOURCE SERVER is EXACTLY the same as in 20.14!                     │           │
│  │    It doesn't care HOW the token was obtained.                              │           │
│  │    It just validates the JWT signature, issuer, expiry, and scopes.         │           │
│  │    Authorization Code token vs Client Credentials token → same validation! │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.4 ★ Authorization Server — Registering a Client for Client Credentials

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ AUTHORIZATION SERVER — RegisteredClient FOR CLIENT CREDENTIALS                           │
│                                                                                              │
│  The authorization server from section 20.14 works perfectly!                                │
│  You just need to add/modify the RegisteredClient to support CLIENT_CREDENTIALS.            │
│  You can even have ONE client that supports BOTH authorization_code AND                      │
│  client_credentials (different grant types for different use cases).                          │
│                                                                                              │
│  Below we show a NEW RegisteredClient specifically for machine-to-machine.                  │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**AuthorizationServerConfig.java — Adding a Client Credentials Client**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AuthorizationServerConfig.java — Authorization Server
//
//  ★ Add a RegisteredClient that uses CLIENT_CREDENTIALS grant type.
//  ★ This client has NO redirect_uri (no browser redirect needed).
//  ★ This client has NO openid scope (no user = no OIDC).
//  ★ Keep all other beans (JWKSource, UserDetailsService, etc.) from 20.14.
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    // ... (keep authorizationServerSecurityFilterChain, defaultSecurityFilterChain,
    //      userDetailsService, passwordEncoder, jwkSource, jwtDecoder,
    //      authorizationServerSettings from section 20.14)

    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        // ── 1. Web Client (Authorization Code — from section 20.14) ──────────
        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("my-client-app")
                .clientSecret(passwordEncoder().encode("my-client-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/login/oauth2/code/my-auth-server")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("read")
                .scope("write")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        .build())
                .build();

        // ── 2. Service Client (Client Credentials — NEW!) ────────────────────
        //    ★ This is the machine-to-machine client.
        //    ★ No redirectUri needed — there's no browser redirect.
        //    ★ No OIDC scopes — there's no user to identify.

        RegisteredClient serviceClient = RegisteredClient.withId(UUID.randomUUID().toString())

                // ★ client-id for the service/microservice
                .clientId("service-a")

                // ★ client-secret — the service authenticates with this
                .clientSecret(passwordEncoder().encode("service-a-secret"))

                // ★ CLIENT_SECRET_BASIC: sends credentials as
                //   Authorization: Basic base64(service-a:service-a-secret)
                // ★ CLIENT_SECRET_POST: sends credentials in the request body
                //   as client_id=service-a&client_secret=service-a-secret
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)

                // ★ CLIENT_CREDENTIALS — the key difference!
                // No AUTHORIZATION_CODE, no REFRESH_TOKEN needed.
                // The client can simply request a new token when the old one expires.
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)

                // ★ NO .redirectUri() — not needed for client_credentials!
                // There's no browser redirect in this flow.

                // ★ Scopes — what this service is allowed to access
                // NO .scope(OidcScopes.OPENID) — no user, no OIDC!
                .scope("read")
                .scope("write")
                .scope("internal")  // Custom scope for internal service communication

                .tokenSettings(TokenSettings.builder()
                        // ★ Access token lifetime
                        // For M2M, you might want shorter lifetimes since the
                        // client can easily request a new token
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        // ★ JWT format (self-contained, can be verified without auth server)
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        .build()
                )
                .build();

        // ★ Register BOTH clients — the auth server can serve
        //   web apps AND microservices simultaneously!
        return new InMemoryRegisteredClientRepository(webClient, serviceClient);
    }

    // ... (keep all other beans from section 20.14 unchanged)
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ KEY DIFFERENCES IN RegisteredClient FOR CLIENT CREDENTIALS                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Authorization Code Client          Client Credentials Client                │           │
│  │  ─────────────────────────          ─────────────────────────               │           │
│  │                                                                               │           │
│  │  .authorizationGrantType(            .authorizationGrantType(                │           │
│  │    AUTHORIZATION_CODE)                 CLIENT_CREDENTIALS)                   │           │
│  │  .authorizationGrantType(            ★ NO REFRESH_TOKEN!                    │           │
│  │    REFRESH_TOKEN)                    (client re-authenticates to get new)    │           │
│  │                                                                               │           │
│  │  .redirectUri(                       ★ NO .redirectUri()!                    │           │
│  │    "http://localhost:8080/...")       (no browser redirect)                   │           │
│  │                                                                               │           │
│  │  .scope(OidcScopes.OPENID)          ★ NO OidcScopes.OPENID!                │           │
│  │  .scope(OidcScopes.PROFILE)         (no user = no OIDC identity)            │           │
│  │  .scope("read")                      .scope("read")                          │           │
│  │  .scope("write")                     .scope("write")                         │           │
│  │                                       .scope("internal")                      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.5 ★ Resource Server — No Changes Needed!

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ RESOURCE SERVER — EXACTLY THE SAME AS SECTION 20.14!                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The Resource Server does NOT care how the token was obtained!               │           │
│  │                                                                               │           │
│  │  Whether the token came from:                                                │           │
│  │    • Authorization Code flow (user logged in via browser)                   │           │
│  │    • Client Credentials flow (service authenticated directly)              │           │
│  │                                                                               │           │
│  │  The Resource Server does the SAME thing:                                    │           │
│  │    1. Read Bearer token from Authorization header                           │           │
│  │    2. Fetch JWK Set from authorization server's /oauth2/jwks               │           │
│  │    3. Verify JWT signature using public key                                 │           │
│  │    4. Validate claims (iss, exp, scope)                                     │           │
│  │    5. Create JwtAuthenticationToken                                          │           │
│  │    6. Authorize based on scopes (hasAuthority("SCOPE_read"))               │           │
│  │                                                                               │           │
│  │  ★ ZERO code changes needed in the Resource Server!                         │           │
│  │  ★ Use the exact same ResourceServerConfig.java from section 20.14.3        │           │
│  │  ★ Use the exact same application.yml from section 20.14.3                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── JWT from Authorization Code vs JWT from Client Credentials ──            │           │
│  │                                                                               │           │
│  │  Authorization Code JWT:              Client Credentials JWT:               │           │
│  │  {                                     {                                      │           │
│  │    "iss": "http://localhost:9000",     "iss": "http://localhost:9000",       │           │
│  │    "sub": "user",  ← username         "sub": "service-a",  ← client_id     │           │
│  │    "aud": "my-client-app",            "aud": "service-a",                   │           │
│  │    "scope": "openid profile read",    "scope": "read write internal",       │           │
│  │    "iat": 1715184000,                  "iat": 1715184000,                    │           │
│  │    "exp": 1715187600                   "exp": 1715185800                     │           │
│  │  }                                     }                                      │           │
│  │                                                                               │           │
│  │  ★ The ONLY difference is the "sub" claim:                                  │           │
│  │    → Authorization Code: sub = username (a person)                          │           │
│  │    → Client Credentials: sub = client_id (an application)                  │           │
│  │                                                                               │           │
│  │  ★ The resource server doesn't distinguish between them!                    │           │
│  │    It only checks scope, issuer, and expiry.                                │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.6 ★ Client Application — Service That Uses Client Credentials

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ CLIENT APPLICATION — THE MICROSERVICE / BACKGROUND JOB                                   │
│    This is where the main difference is!                                                     │
│    Instead of .oauth2Login() (browser flow), we use                                          │
│    OAuth2AuthorizedClientManager with CLIENT_CREDENTIALS grant.                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Two approaches to implement the client:                                     │           │
│  │                                                                               │           │
│  │  1. WebClient with automatic token management (RECOMMENDED)                  │           │
│  │     → Spring's OAuth2 integration auto-fetches and caches tokens            │           │
│  │     → Auto-refreshes expired tokens                                          │           │
│  │     → Zero manual token handling                                             │           │
│  │                                                                               │           │
│  │  2. RestTemplate / RestClient with manual token management                   │           │
│  │     → You call OAuth2AuthorizedClientManager.authorize() yourself            │           │
│  │     → Extract access token and add it to headers manually                   │           │
│  │                                                                               │           │
│  │  We'll show BOTH approaches.                                                 │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**pom.xml — Client Application (Service)**

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Client Service using Client Credentials                             -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- ★ Spring Boot Starter OAuth2 Client
         SAME dependency as Authorization Code flow!
         The grant type is configured in application.yml, not in code.
    -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- ★ WebFlux for WebClient (recommended for OAuth 2.0 token management) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

</dependencies>
```

**application.yml — Client Application (Service)**

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — Client Service using Client Credentials (Port 8080)
#
#  ★ KEY DIFFERENCE from Authorization Code:
#    → authorization-grant-type: client_credentials (not authorization_code)
#    → NO redirect-uri (no browser redirect)
#    → NO openid scope (no user)
#    → NO provider issuer-uri (not needed for client_credentials —
#      we only need the token-uri to POST to /oauth2/token)
# ═══════════════════════════════════════════════════════════════════════════════

server:
  port: 8080

spring:
  security:
    oauth2:
      client:

        registration:
          # ★ "service-a-client" is the registrationId — your chosen key
          service-a-client:
            # ★ Must match the clientId registered on the authorization server
            client-id: service-a
            # ★ Must match the clientSecret registered on the authorization server
            client-secret: service-a-secret

            # ★★★ THE KEY CONFIG — client_credentials grant type!
            authorization-grant-type: client_credentials

            # ★ How to send credentials in the token request
            client-authentication-method: client_secret_basic

            # ★ Scopes to request — must be subset of what auth server allows
            scope:
              - read
              - write
              - internal

            # ★ NO redirect-uri! (not needed for client_credentials)
            # ★ NO openid scope! (no user to identify)

            # ★ Maps to the provider section below
            provider: my-auth-server

        provider:
          my-auth-server:
            # ★ For client_credentials, we only need the token endpoint!
            # We don't need issuer-uri because there's no OIDC discovery needed.
            # But you CAN use issuer-uri if you want auto-discovery.
            token-uri: http://localhost:9000/oauth2/token

            # ★ Alternative: Use issuer-uri for auto-discovery
            # issuer-uri: http://localhost:9000
            # This would auto-discover token-uri from:
            #   http://localhost:9000/.well-known/openid-configuration
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ application.yml — AUTHORIZATION CODE vs CLIENT CREDENTIALS COMPARISON                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌────────────────────────────────┬────────────────────────────────────────┐                │
│  │  Authorization Code Client     │  Client Credentials Client             │                │
│  ├────────────────────────────────┼────────────────────────────────────────┤                │
│  │                                │                                        │                │
│  │  authorization-grant-type:     │  authorization-grant-type:             │                │
│  │    authorization_code          │    client_credentials                   │                │
│  │                                │                                        │                │
│  │  redirect-uri: "{baseUrl}/...  │  ★ NO redirect-uri                    │                │
│  │    /login/oauth2/code/..."     │                                        │                │
│  │                                │                                        │                │
│  │  scope:                        │  scope:                                │                │
│  │    - openid                    │    - read                              │                │
│  │    - profile                   │    - write                             │                │
│  │    - read                      │    - internal                          │                │
│  │    - write                     │  ★ NO openid/profile!                 │                │
│  │                                │                                        │                │
│  │  provider:                     │  provider:                             │                │
│  │    issuer-uri: ...             │    token-uri: .../oauth2/token         │                │
│  │    (needs OIDC discovery)     │    (only need token endpoint!)         │                │
│  │                                │                                        │                │
│  └────────────────────────────────┴────────────────────────────────────────┘                │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.7 ★ Approach 1 — WebClient with Automatic Token Management (Recommended)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ APPROACH 1: WEBCLIENT WITH AUTOMATIC TOKEN MANAGEMENT                                   │
│    Spring OAuth2 Client + WebClient = ZERO manual token handling!                            │
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │  How it works:                                                                │           │
│  │                                                                               │           │
│  │  1. You call webClient.get().uri("/api/data").retrieve()                     │           │
│  │                                                                               │           │
│  │  2. ServletOAuth2AuthorizedClientExchangeFilterFunction (registered in       │           │
│  │     WebClient) intercepts the request                                        │           │
│  │                                                                               │           │
│  │  3. It asks OAuth2AuthorizedClientManager: "Do I have a token for           │           │
│  │     registration 'service-a-client'?"                                        │           │
│  │                                                                               │           │
│  │  4. Manager checks OAuth2AuthorizedClientService:                            │           │
│  │     → No token yet? → Use ClientCredentialsOAuth2AuthorizedClientProvider   │           │
│  │       to POST to /oauth2/token and get a new access_token                    │           │
│  │     → Token exists but expired? → Get a new one (re-authenticate)           │           │
│  │     → Token exists and valid? → Use it                                      │           │
│  │                                                                               │           │
│  │  5. Token is automatically added as:                                         │           │
│  │     Authorization: Bearer <access_token>                                     │           │
│  │                                                                               │           │
│  │  6. Request proceeds to the resource server                                  │           │
│  │                                                                               │           │
│  │  ★ You NEVER manually call /oauth2/token!                                   │           │
│  │  ★ You NEVER manually read or store the access token!                       │           │
│  │  ★ Spring handles EVERYTHING!                                                │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**ClientCredentialsConfig.java — WebClient Approach**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ClientCredentialsConfig.java — Client Credentials with WebClient
//
//  ★ This configures:
//    1. OAuth2AuthorizedClientManager (manages token lifecycle)
//    2. WebClient (auto-attaches Bearer token to all requests)
//
//  ★ For Client Credentials, we use:
//    → AuthorizedClientServiceOAuth2AuthorizedClientManager
//      (NOT DefaultOAuth2AuthorizedClientManager!)
//    → Why? DefaultOAuth2AuthorizedClientManager requires an HttpServletRequest
//      (it's designed for web flows with a user). But in M2M scenarios,
//      there may be no HttpServletRequest (e.g., in a @Scheduled method).
//    → AuthorizedClientServiceOAuth2AuthorizedClientManager works WITHOUT
//      an HttpServletRequest. It stores tokens in OAuth2AuthorizedClientService.
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
public class ClientCredentialsConfig {

    // ══════════════════════════════════════════════════════════════════════════
    //  1. OAuth2AuthorizedClientManager
    //     → Manages the lifecycle of OAuth2AuthorizedClients (tokens)
    //     → For client_credentials: auto-fetches tokens from /oauth2/token
    //     → Caches tokens until they expire
    //     → Re-authenticates automatically when token expires
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        // ★ ClientCredentialsOAuth2AuthorizedClientProvider
        // This is the provider that actually makes the POST /oauth2/token call
        // with grant_type=client_credentials
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()  // ★ Enable client_credentials grant!
                        .build();

        // ★ AuthorizedClientServiceOAuth2AuthorizedClientManager
        // Uses OAuth2AuthorizedClientService (NOT OAuth2AuthorizedClientRepository)
        // → Works without HttpServletRequest
        // → Perfect for @Scheduled, @Async, background threads, CLI tools
        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  2. WebClient with OAuth2 token auto-attachment
    //     → Every request made through this WebClient automatically includes
    //       the access token in the Authorization header
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        // ★ IMPORTANT for client_credentials:
        // Set the default client registration to use.
        // This tells WebClient WHICH registration from application.yml to use
        // when fetching tokens.
        oauth2Client.setDefaultClientRegistrationId("service-a-client");

        return WebClient.builder()
                .apply(oauth2Client.oauth2Configuration())
                .build();
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ IMPORTANT: DefaultOAuth2AuthorizedClientManager vs                                       │
│    AuthorizedClientServiceOAuth2AuthorizedClientManager                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  DefaultOAuth2AuthorizedClientManager                                        │           │
│  │  ────────────────────────────────────                                        │           │
│  │  → Used in section 20.14 (Authorization Code + web browser)                 │           │
│  │  → Requires HttpServletRequest and HttpServletResponse                      │           │
│  │  → Stores tokens in OAuth2AuthorizedClientRepository (session-based)        │           │
│  │  → Works ONLY inside a web request context (e.g., controller methods)       │           │
│  │  → ❌ FAILS in @Scheduled, @Async, background threads!                      │           │
│  │                                                                               │           │
│  │  AuthorizedClientServiceOAuth2AuthorizedClientManager                        │           │
│  │  ────────────────────────────────────────────────────                        │           │
│  │  → Used for Client Credentials (machine-to-machine)                         │           │
│  │  → Does NOT require HttpServletRequest                                      │           │
│  │  → Stores tokens in OAuth2AuthorizedClientService (in-memory by default)    │           │
│  │  → Works ANYWHERE — controllers, @Scheduled, @Async, CLI runners           │           │
│  │  → ✅ Perfect for M2M scenarios!                                             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**ServiceController.java — Using WebClient to Call Resource Server**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ServiceController.java — Calls Resource Server using WebClient
//
//  ★ WebClient automatically:
//    1. Checks if we have a valid access token for "service-a-client"
//    2. If not → POST /oauth2/token (client_credentials) to get one
//    3. Attaches token as: Authorization: Bearer <access_token>
//    4. Makes the API call to the resource server
//  ★ You just write: webClient.get().uri("...").retrieve()
//    Token management is completely invisible!
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/service")
public class ServiceController {

    private final WebClient webClient;

    public ServiceController(WebClient webClient) {
        this.webClient = webClient;
    }

    // ★ This endpoint demonstrates M2M communication:
    //   Service A (this app) → calls → Service B (resource server)
    @GetMapping("/fetch-data")
    public Map<String, Object> fetchDataFromResourceServer() {

        // ★ WebClient auto-handles everything:
        //   → Token acquisition (POST /oauth2/token)
        //   → Token caching (reuses until expired)
        //   → Token attachment (Authorization: Bearer <token>)
        Map<String, Object> resourceData = webClient
                .get()
                .uri("http://localhost:8090/api/users")  // Resource Server URL
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Service A successfully called Service B!");
        response.put("service_b_response", resourceData);

        return response;
    }
}
```

**ScheduledTaskService.java — Using WebClient in @Scheduled (No HTTP Request Context!)**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ScheduledTaskService.java — Background job that calls Resource Server
//
//  ★ This demonstrates why AuthorizedClientServiceOAuth2AuthorizedClientManager
//    is critical for client_credentials.
//  ★ @Scheduled runs in a background thread — there's NO HttpServletRequest!
//  ★ DefaultOAuth2AuthorizedClientManager would FAIL here.
//  ★ AuthorizedClientServiceOAuth2AuthorizedClientManager works perfectly.
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class ScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    private final WebClient webClient;

    public ScheduledTaskService(WebClient webClient) {
        this.webClient = webClient;
    }

    // ★ Runs every 60 seconds — no user, no browser, no HTTP request!
    @Scheduled(fixedRate = 60000)
    public void syncDataFromResourceServer() {

        log.info("Background sync: Fetching data from Resource Server...");

        Map<String, Object> data = webClient
                .get()
                .uri("http://localhost:8090/api/users")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        log.info("Background sync: Received data = {}", data);

        // Process the data... (save to DB, send to queue, etc.)
    }
}
```

---

##### 20.15.8 ★ Approach 2 — RestClient / RestTemplate with Manual Token Management

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ APPROACH 2: MANUAL TOKEN MANAGEMENT WITH OAuth2AuthorizedClientManager                   │
│    Use this if you don't want WebClient / WebFlux dependency.                                │
│    You manually authorize, extract the token, and set the header.                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Steps:                                                                       │           │
│  │  1. Build an OAuth2AuthorizeRequest for "service-a-client"                   │           │
│  │  2. Call authorizedClientManager.authorize(request)                          │           │
│  │     → This calls POST /oauth2/token internally if no valid token cached     │           │
│  │  3. Get the OAuth2AuthorizedClient (contains the access token)              │           │
│  │  4. Extract access token: client.getAccessToken().getTokenValue()           │           │
│  │  5. Add to request: Authorization: Bearer <token>                           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**ManualTokenService.java — Manual Token Management**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ManualTokenService.java — Manual token management with RestClient
//
//  ★ This approach does NOT use WebClient.
//  ★ You manually call OAuth2AuthorizedClientManager.authorize()
//    to get a token, then attach it to your HTTP requests.
//  ★ The manager still handles caching and re-authentication!
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class ManualTokenService {

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final RestClient restClient;

    public ManualTokenService(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
        this.restClient = RestClient.create();
    }

    public Map<String, Object> callResourceServer() {

        // ── STEP 1: Build an authorize request ──────────────────────────────
        // ★ "service-a-client" is the registrationId from application.yml
        // ★ "service-a" is a principal name (can be any string for M2M)
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("service-a-client")
                .principal("service-a")  // Principal name for M2M (the app itself)
                .build();

        // ── STEP 2: Authorize (this fetches/caches the token) ───────────────
        // ★ First call: POST /oauth2/token with client_credentials
        // ★ Subsequent calls: returns cached token if not expired
        // ★ Expired token: automatically re-authenticates
        OAuth2AuthorizedClient authorizedClient =
                authorizedClientManager.authorize(authorizeRequest);

        // ── STEP 3: Extract the access token ────────────────────────────────
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        // ── STEP 4: Call the resource server with the token ─────────────────
        Map<String, Object> response = restClient
                .get()
                .uri("http://localhost:8090/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return response;
    }
}
```

---

##### 20.15.9 ★ Security Configuration — Client Application (No Login Needed!)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SECURITY CONFIG FOR CLIENT CREDENTIALS CLIENT                                            │
│    Since there's no user login, the security config is much simpler!                         │
│    No .oauth2Login(), no form login, no session management for users.                        │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**SecurityConfig.java — Client Application**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig.java — Client Application Security Config
//
//  ★ For a pure M2M service, this is very simple!
//  ★ No .oauth2Login() (no user login flow)
//  ★ No .formLogin() (no login page)
//  ★ You might even permitAll() if this service isn't exposed externally,
//    or add its own security if it has public APIs.
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // ★ If this service has its own APIs, you can protect them
                // For internal microservices, you might permitAll()
                // or use OAuth2 resource server validation for incoming requests
                .requestMatchers("/service/**").permitAll()
                .anyRequest().authenticated()
            )
            // ★ No .oauth2Login() — there's no user login!
            // ★ No .formLogin() — there's no login page!
            .csrf(csrf -> csrf.disable());  // Disable CSRF for API-only services

        return http.build();
    }
}
```

---

##### 20.15.10 ★ Complete Internal Flow — What Happens Inside Spring

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMPLETE INTERNAL FLOW — CLIENT CREDENTIALS WITH SPRING SECURITY                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Your Code                                                                   │           │
│  │    │                                                                          │           │
│  │    │  webClient.get().uri("http://localhost:8090/api/users").retrieve()      │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  ServletOAuth2AuthorizedClientExchangeFilterFunction                         │           │
│  │    │                                                                          │           │
│  │    │  "I need to attach a Bearer token. Let me check if I have one."        │           │
│  │    │  Registration ID: "service-a-client" (from setDefaultClientRegistrationId)│          │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  OAuth2AuthorizedClientManager.authorize(request)                            │           │
│  │    │                                                                          │           │
│  │    │  "Do I have a cached OAuth2AuthorizedClient for 'service-a-client'?"   │           │
│  │    │                                                                          │           │
│  │    ├── YES, token is valid ──────────────────────────────────────────────    │           │
│  │    │   → Return cached OAuth2AuthorizedClient                                │           │
│  │    │   → Skip token request (no HTTP call needed!)                           │           │
│  │    │                                                                          │           │
│  │    ├── YES, token is EXPIRED ────────────────────────────────────────────    │           │
│  │    │   → Re-authenticate: POST /oauth2/token (client_credentials)           │           │
│  │    │   → Cache new OAuth2AuthorizedClient                                    │           │
│  │    │                                                                          │           │
│  │    └── NO, no cached token ──────────────────────────────────────────────   │           │
│  │        │                                                                      │           │
│  │        ▼                                                                      │           │
│  │      ClientCredentialsOAuth2AuthorizedClientProvider                         │           │
│  │        │                                                                      │           │
│  │        │  1. Read ClientRegistration from application.yml:                   │           │
│  │        │     → client-id: service-a                                          │           │
│  │        │     → client-secret: service-a-secret                               │           │
│  │        │     → token-uri: http://localhost:9000/oauth2/token                 │           │
│  │        │     → scope: read write internal                                    │           │
│  │        │                                                                      │           │
│  │        │  2. Build token request:                                             │           │
│  │        │     POST http://localhost:9000/oauth2/token                         │           │
│  │        │     Authorization: Basic base64("service-a:service-a-secret")      │           │
│  │        │     Content-Type: application/x-www-form-urlencoded                │           │
│  │        │     Body: grant_type=client_credentials&scope=read+write+internal  │           │
│  │        │                                                                      │           │
│  │        ▼                                                                      │           │
│  │      ┌────────────────────────────────────────────────────────────┐          │           │
│  │      │  Authorization Server (Port 9000)                          │          │           │
│  │      │                                                             │          │           │
│  │      │  OAuth2TokenEndpointFilter                                 │          │           │
│  │      │    │                                                        │          │           │
│  │      │    │  1. ClientSecretBasicAuthenticationConverter          │          │           │
│  │      │    │     → Decodes Authorization: Basic header             │          │           │
│  │      │    │     → Extracts client_id + client_secret              │          │           │
│  │      │    │                                                        │          │           │
│  │      │    │  2. RegisteredClientRepository.findByClientId()       │          │           │
│  │      │    │     → Finds "service-a" in registered clients         │          │           │
│  │      │    │     → Validates client_secret (BCrypt match)          │          │           │
│  │      │    │                                                        │          │           │
│  │      │    │  3. Checks grant_type: CLIENT_CREDENTIALS             │          │           │
│  │      │    │     → Is this client allowed to use this grant type? │          │           │
│  │      │    │     → Yes! (we registered it with CLIENT_CREDENTIALS)│          │           │
│  │      │    │                                                        │          │           │
│  │      │    │  4. Validates requested scopes:                       │          │           │
│  │      │    │     → Are "read", "write", "internal" allowed        │          │           │
│  │      │    │       for this client?                                │          │           │
│  │      │    │     → Yes! (we registered them)                       │          │           │
│  │      │    │                                                        │          │           │
│  │      │    │  5. OAuth2TokenGenerator                              │          │           │
│  │      │    │     → JwtGenerator (creates JWT access token)         │          │           │
│  │      │    │     → Signs with RSA private key (from JWKSource)     │          │           │
│  │      │    │     → Sets claims:                                    │          │           │
│  │      │    │       iss: "http://localhost:9000"                    │          │           │
│  │      │    │       sub: "service-a" (client_id, not username!)    │          │           │
│  │      │    │       aud: "service-a"                               │          │           │
│  │      │    │       scope: "read write internal"                   │          │           │
│  │      │    │       iat: <now>                                      │          │           │
│  │      │    │       exp: <now + 30 minutes>                        │          │           │
│  │      │    │                                                        │          │           │
│  │      │    │  6. Returns:                                          │          │           │
│  │      │    │     200 OK                                            │          │           │
│  │      │    │     {                                                 │          │           │
│  │      │    │       "access_token": "eyJhbGciOi...",               │          │           │
│  │      │    │       "token_type": "Bearer",                        │          │           │
│  │      │    │       "expires_in": 1800,                            │          │           │
│  │      │    │       "scope": "read write internal"                 │          │           │
│  │      │    │     }                                                 │          │           │
│  │      │    │     ★ NO id_token! (not OIDC, no user)               │          │           │
│  │      │    │     ★ NO refresh_token! (client re-authenticates)    │          │           │
│  │      │                                                             │          │           │
│  │      └────────────────────────────────────────────────────────────┘          │           │
│  │        │                                                                      │           │
│  │        │  3. Parse response → create OAuth2AccessToken                       │           │
│  │        │  4. Create OAuth2AuthorizedClient (registration + token)            │           │
│  │        │  5. Save to OAuth2AuthorizedClientService (cache)                   │           │
│  │        │                                                                      │           │
│  │        ▼                                                                      │           │
│  │  Back in ServletOAuth2AuthorizedClientExchangeFilterFunction:                │           │
│  │    │                                                                          │           │
│  │    │  Extract token: authorizedClient.getAccessToken().getTokenValue()       │           │
│  │    │  Add header: Authorization: Bearer eyJhbGciOi...                       │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  HTTP Request to Resource Server:                                             │           │
│  │    GET http://localhost:8090/api/users                                        │           │
│  │    Authorization: Bearer eyJhbGciOi...                                       │           │
│  │    │                                                                          │           │
│  │    ▼                                                                          │           │
│  │  ┌────────────────────────────────────────────────────────────┐              │           │
│  │  │  Resource Server (Port 8090)                                │              │           │
│  │  │                                                             │              │           │
│  │  │  BearerTokenAuthenticationFilter                           │              │           │
│  │  │    → Reads Bearer token from header                        │              │           │
│  │  │                                                             │              │           │
│  │  │  JwtAuthenticationProvider                                 │              │           │
│  │  │    → JwtDecoder: fetches JWK Set from auth server's        │              │           │
│  │  │      /oauth2/jwks (cached)                                 │              │           │
│  │  │    → Verifies JWT signature with RSA public key            │              │           │
│  │  │    → Validates: iss == "http://localhost:9000" ✅            │              │           │
│  │  │    → Validates: exp > now ✅                                │              │           │
│  │  │    → Extracts scope: "read write internal"                 │              │           │
│  │  │    → Creates JwtAuthenticationToken with authorities:      │              │           │
│  │  │      [SCOPE_read, SCOPE_write, SCOPE_internal]            │              │           │
│  │  │                                                             │              │           │
│  │  │  AuthorizationFilter                                       │              │           │
│  │  │    → GET /api/users requires SCOPE_read → ✅ authorized     │              │           │
│  │  │                                                             │              │           │
│  │  │  ApiController.getUsers(Jwt jwt)                           │              │           │
│  │  │    → jwt.getSubject() == "service-a" (the client, not user)│              │           │
│  │  │    → Returns response data                                 │              │           │
│  │  │                                                             │              │           │
│  │  └────────────────────────────────────────────────────────────┘              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.11 ★ Required Imports for All Classes

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Required imports for Client Credentials implementation
// ═══════════════════════════════════════════════════════════════════════════════

// ── ClientCredentialsConfig.java ──
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client
        .ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

// ── SecurityConfig.java ──
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

// ── ServiceController.java ──
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

// ── ManualTokenService.java (Approach 2) ──
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

// ── ScheduledTaskService.java ──
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
```

---

##### 20.15.12 ★ Token Request and Response — Raw HTTP

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ RAW HTTP — TOKEN REQUEST AND RESPONSE                                                    │
│    This is what Spring sends/receives behind the scenes.                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── REQUEST ──────────────────────────────────────────────────               │           │
│  │                                                                               │           │
│  │  POST /oauth2/token HTTP/1.1                                                │           │
│  │  Host: localhost:9000                                                         │           │
│  │  Authorization: Basic c2VydmljZS1hOnNlcnZpY2UtYS1zZWNyZXQ=                 │           │
│  │  Content-Type: application/x-www-form-urlencoded                             │           │
│  │                                                                               │           │
│  │  grant_type=client_credentials&scope=read+write+internal                    │           │
│  │                                                                               │           │
│  │  ★ Authorization: Basic base64("service-a:service-a-secret")               │           │
│  │    = "c2VydmljZS1hOnNlcnZpY2UtYS1zZWNyZXQ="                               │           │
│  │                                                                               │           │
│  │  ★ With CLIENT_SECRET_POST (alternative method):                            │           │
│  │  POST /oauth2/token HTTP/1.1                                                │           │
│  │  Content-Type: application/x-www-form-urlencoded                             │           │
│  │                                                                               │           │
│  │  grant_type=client_credentials                                               │           │
│  │  &client_id=service-a                                                        │           │
│  │  &client_secret=service-a-secret                                            │           │
│  │  &scope=read+write+internal                                                  │           │
│  │                                                                               │           │
│  │  ── RESPONSE ─────────────────────────────────────────────────               │           │
│  │                                                                               │           │
│  │  HTTP/1.1 200 OK                                                             │           │
│  │  Content-Type: application/json                                               │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "access_token": "eyJraWQiOiI3ZjM4YmQ0Ny0xM2U2LTQ5ZDEtYWI3ZS01...",     │           │
│  │    "token_type": "Bearer",                                                   │           │
│  │    "expires_in": 1800,                                                        │           │
│  │    "scope": "read write internal"                                            │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ ONLY access_token! No id_token (no user), no refresh_token.             │           │
│  │                                                                               │           │
│  │  ── DECODED JWT (access_token) ───────────────────────────────               │           │
│  │                                                                               │           │
│  │  Header:                                                                     │           │
│  │  {                                                                            │           │
│  │    "alg": "RS256",                                                           │           │
│  │    "kid": "7f38bd47-13e6-49d1-ab7e-..."                                    │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  Payload:                                                                     │           │
│  │  {                                                                            │           │
│  │    "iss": "http://localhost:9000",            ← Auth server issuer          │           │
│  │    "sub": "service-a",                        ← client_id (NOT a username!) │           │
│  │    "aud": "service-a",                        ← Audience = client_id        │           │
│  │    "nbf": 1715184000,                         ← Not before                  │           │
│  │    "scope": "read write internal",            ← Granted scopes             │           │
│  │    "iat": 1715184000,                         ← Issued at                   │           │
│  │    "exp": 1715185800                          ← Expires at (30 min)         │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ Notice: "sub" = "service-a" (client_id)                                 │           │
│  │    In Authorization Code, "sub" would be the username.                      │           │
│  │    In Client Credentials, "sub" is the client application itself.           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.13 ★ Real-World Use Cases — Client Credentials in Microservices

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ REAL-WORLD USE CASES — CLIENT CREDENTIALS IN MICROSERVICES                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── USE CASE 1: Microservice-to-Microservice Communication ──                │           │
│  │                                                                               │           │
│  │  ┌──────────────┐      ┌────────────────┐      ┌──────────────┐            │           │
│  │  │ Order Service │──M2M──│ Auth Server    │      │ Inventory    │            │           │
│  │  │ (client_cred) │      │ (issues token) │      │ Service      │            │           │
│  │  │              │      └────────────────┘      │ (resource    │            │           │
│  │  │              │───────── Bearer token ───────>│  server)     │            │           │
│  │  └──────────────┘                               └──────────────┘            │           │
│  │                                                                               │           │
│  │  Order Service needs to check inventory → uses client_credentials            │           │
│  │  to authenticate as "order-service" (no user context needed).                │           │
│  │                                                                               │           │
│  │  ── USE CASE 2: Scheduled Batch Processing ──────────────────                │           │
│  │                                                                               │           │
│  │  ┌──────────────┐      ┌────────────────┐      ┌──────────────┐            │           │
│  │  │ Report        │──M2M──│ Auth Server    │      │ Data API     │            │           │
│  │  │ Generator    │      │ (issues token) │      │ (resource    │            │           │
│  │  │ (cron job)   │      └────────────────┘      │  server)     │            │           │
│  │  │              │───────── Bearer token ───────>│              │            │           │
│  │  └──────────────┘                               └──────────────┘            │           │
│  │                                                                               │           │
│  │  Nightly report generator fetches data from Data API.                        │           │
│  │  Runs at 2 AM — no user, no browser, just service-to-service.               │           │
│  │                                                                               │           │
│  │  ── USE CASE 3: External Partner API Access ─────────────────                │           │
│  │                                                                               │           │
│  │  ┌──────────────┐      ┌────────────────┐      ┌──────────────┐            │           │
│  │  │ Partner's     │──M2M──│ Your Auth      │      │ Your API     │            │           │
│  │  │ Backend      │      │ Server         │      │ Gateway      │            │           │
│  │  │ (their app)  │      │ (issues token) │      │ (resource    │            │           │
│  │  │              │      └────────────────┘      │  server)     │            │           │
│  │  │              │───────── Bearer token ───────>│              │            │           │
│  │  └──────────────┘                               └──────────────┘            │           │
│  │                                                                               │           │
│  │  Partner company's backend calls your API.                                   │           │
│  │  You register them as a client with client_credentials.                      │           │
│  │  They authenticate with their client_id + client_secret.                     │           │
│  │                                                                               │           │
│  │  ── USE CASE 4: CLI Tool / DevOps Script ────────────────────                │           │
│  │                                                                               │           │
│  │  $ curl -X POST http://localhost:9000/oauth2/token \                         │           │
│  │      -H "Authorization: Basic $(echo -n 'cli-tool:cli-secret' | base64)" \  │           │
│  │      -d "grant_type=client_credentials&scope=read"                           │           │
│  │                                                                               │           │
│  │  # Response: { "access_token": "eyJ...", ... }                              │           │
│  │                                                                               │           │
│  │  $ curl http://localhost:8090/api/users \                                    │           │
│  │      -H "Authorization: Bearer eyJ..."                                      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.15.14 ★ Summary — Client Credentials Grant Flow

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — CLIENT CREDENTIALS GRANT FLOW                                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── WHAT IS IT ───────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • OAuth 2.0 grant type for MACHINE-TO-MACHINE (M2M) communication          │           │
│  │  • NO user involved — the client authenticates ITSELF                       │           │
│  │  • 1-step flow: POST /oauth2/token with client_id + client_secret           │           │
│  │  • Returns access_token ONLY (no id_token, no refresh_token)               │           │
│  │  • Token "sub" claim = client_id (not a username)                           │           │
│  │                                                                               │           │
│  │  ── AUTHORIZATION SERVER ─────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • Same as section 20.14 (reuse everything!)                                │           │
│  │  • Add RegisteredClient with:                                                │           │
│  │    → .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)     │           │
│  │    → NO .redirectUri() (no browser redirect)                                │           │
│  │    → NO OidcScopes.OPENID (no user = no OIDC)                              │           │
│  │                                                                               │           │
│  │  ── RESOURCE SERVER ──────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • EXACTLY same as section 20.14 (ZERO changes!)                            │           │
│  │  • Validates JWT the same way regardless of grant type                      │           │
│  │                                                                               │           │
│  │  ── CLIENT APPLICATION (M2M Service) ─────────────────────────────           │           │
│  │                                                                               │           │
│  │  • application.yml:                                                           │           │
│  │    → authorization-grant-type: client_credentials                           │           │
│  │    → NO redirect-uri, NO openid scope                                       │           │
│  │    → provider.token-uri: http://localhost:9000/oauth2/token                 │           │
│  │                                                                               │           │
│  │  • Configuration:                                                             │           │
│  │    → AuthorizedClientServiceOAuth2AuthorizedClientManager                   │           │
│  │      (NOT DefaultOAuth2AuthorizedClientManager!)                             │           │
│  │    → OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials()     │           │
│  │    → WebClient with setDefaultClientRegistrationId("...")                    │           │
│  │                                                                               │           │
│  │  • No .oauth2Login() needed (no user login!)                                │           │
│  │  • WebClient auto-fetches, caches, and refreshes tokens                     │           │
│  │  • Works in @Scheduled, @Async, and background threads                      │           │
│  │                                                                               │           │
│  │  ── KEY CLASSES ──────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • AuthorizedClientServiceOAuth2AuthorizedClientManager                     │           │
│  │    → Manages tokens without HttpServletRequest                              │           │
│  │  • ClientCredentialsOAuth2AuthorizedClientProvider                          │           │
│  │    → Fetches tokens via POST /oauth2/token                                  │           │
│  │  • OAuth2AuthorizedClientService                                             │           │
│  │    → Stores/caches OAuth2AuthorizedClient (tokens)                          │           │
│  │  • ServletOAuth2AuthorizedClientExchangeFilterFunction                      │           │
│  │    → Auto-attaches Bearer token to WebClient requests                       │           │
│  │                                                                               │           │
│  │  ── FLOW IN ONE LINE ─────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Service A → POST /token (client_credentials) → Auth Server                │           │
│  │  → access_token → GET /api/data (Bearer token) → Service B → response     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.16 ★ Device Authorization Grant Flow (Device Code Flow) — Spring Security

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ DEVICE AUTHORIZATION GRANT (RFC 8628) — THE "DEVICE CODE" FLOW                          │
│    — FOR DEVICES WITHOUT A BROWSER OR WITH LIMITED INPUT CAPABILITY                          │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  What is the Device Authorization Grant?                                     │           │
│  │  ───────────────────────────────────────                                     │           │
│  │                                                                               │           │
│  │  • An OAuth 2.0 grant type designed for devices that:                       │           │
│  │    → Have NO browser (Smart TVs, IoT devices, CLI tools)                    │           │
│  │    → Have LIMITED input capability (game consoles, printers)                │           │
│  │    → Cannot easily type a full URL or complex credentials                   │           │
│  │                                                                               │           │
│  │  • Instead of redirecting to a login page (like Authorization Code),         │           │
│  │    the device shows a SHORT CODE and a URL.                                  │           │
│  │    The user opens the URL on their PHONE or LAPTOP (a different device!)    │           │
│  │    and enters the code to authorize the device.                              │           │
│  │                                                                               │           │
│  │  Real-world examples:                                                         │           │
│  │  ───────────────────                                                         │           │
│  │  • Logging into Netflix/YouTube on a Smart TV                               │           │
│  │    → TV shows: "Go to netflix.com/activate and enter code: ABCD-1234"       │           │
│  │  • Logging into GitHub CLI (gh auth login)                                  │           │
│  │    → Terminal shows: "Go to github.com/login/device and enter code: XYZ"   │           │
│  │  • Logging into Azure CLI (az login --use-device-code)                      │           │
│  │    → Terminal shows: "Go to microsoft.com/devicelogin and enter code: ABC"  │           │
│  │  • Smart home devices (Alexa, Google Home)                                  │           │
│  │  • Printers, set-top boxes, game consoles                                   │           │
│  │                                                                               │           │
│  │  Why not use Authorization Code?                                              │           │
│  │  ────────────────────────────────                                            │           │
│  │  • Authorization Code requires a browser on the SAME device                 │           │
│  │  • A Smart TV can't easily open a browser and redirect back                 │           │
│  │  • A CLI tool can't spawn a browser on a headless server                    │           │
│  │  • Device Code solves this by using a SEPARATE device (phone/laptop)        │           │
│  │    for the login, while the original device polls for completion             │           │
│  │                                                                               │           │
│  │  Why not use Client Credentials?                                              │           │
│  │  ────────────────────────────────                                            │           │
│  │  • Client Credentials is for M2M (no user involved)                         │           │
│  │  • Device Code IS user-involved — the user must authenticate and consent    │           │
│  │  • The token represents the USER (not the device/app)                       │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.1 ★ Device Code Flow vs Other Grant Types — Comparison

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ DEVICE CODE vs AUTHORIZATION CODE vs CLIENT CREDENTIALS                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────┬──────────────────────┬──────────────────────┐                     │
│  │  Authorization Code  │  Client Credentials  │  Device Code         │                     │
│  ├──────────────────────┼──────────────────────┼──────────────────────┤                     │
│  │                      │                      │                      │                     │
│  │  USER + BROWSER      │  NO user             │  USER + NO BROWSER   │                     │
│  │  on SAME device      │  Machine-to-machine  │  on the DEVICE       │                     │
│  │                      │                      │  (uses 2nd device!)  │                     │
│  │                      │                      │                      │                     │
│  ├──────────────────────┼──────────────────────┼──────────────────────┤                     │
│  │                      │                      │                      │                     │
│  │  Browser redirect    │  Direct POST         │  Device shows CODE   │                     │
│  │  → login page        │  → /token            │  → User goes to URL  │                     │
│  │  → consent           │  → get token         │  → Enters code       │                     │
│  │  → redirect back     │                      │  → Device polls      │                     │
│  │  → exchange code     │                      │  → Gets token        │                     │
│  │                      │                      │                      │                     │
│  ├──────────────────────┼──────────────────────┼──────────────────────┤                     │
│  │                      │                      │                      │                     │
│  │  Token = USER        │  Token = CLIENT      │  Token = USER        │                     │
│  │  sub = username      │  sub = client_id     │  sub = username      │                     │
│  │                      │                      │                      │                     │
│  ├──────────────────────┼──────────────────────┼──────────────────────┤                     │
│  │                      │                      │                      │                     │
│  │  Web apps, SPAs,     │  Microservices,      │  Smart TVs, CLIs,    │                     │
│  │  mobile apps         │  batch jobs          │  IoT, game consoles  │                     │
│  │                      │                      │                      │                     │
│  ├──────────────────────┼──────────────────────┼──────────────────────┤                     │
│  │                      │                      │                      │                     │
│  │  grant_type:         │  grant_type:         │  grant_type:         │                     │
│  │  authorization_code  │  client_credentials  │  urn:ietf:params:    │                     │
│  │                      │                      │  oauth:grant-type:   │                     │
│  │                      │                      │  device_code         │                     │
│  │                      │                      │                      │                     │
│  ├──────────────────────┼──────────────────────┼──────────────────────┤                     │
│  │                      │                      │                      │                     │
│  │  Endpoints:          │  Endpoints:          │  Endpoints:          │                     │
│  │  /oauth2/authorize   │  /oauth2/token       │  /oauth2/device_     │                     │
│  │  /oauth2/token       │                      │    authorization     │                     │
│  │                      │                      │  /oauth2/device_     │                     │
│  │                      │                      │    verification      │                     │
│  │                      │                      │  /oauth2/token       │                     │
│  │                      │                      │                      │                     │
│  └──────────────────────┴──────────────────────┴──────────────────────┘                     │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.2 ★ Device Code Flow Diagram — Complete Step-by-Step

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ DEVICE CODE FLOW — COMPLETE STEP-BY-STEP                                                │
│    TWO devices involved: the DEVICE (TV/CLI) and the USER'S PHONE/LAPTOP                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│                                                                                              │
│  Device (Smart TV /     Authorization           User's Phone /                              │
│  CLI / IoT)             Server (Port 9000)      Laptop (Browser)                            │
│  ─────────────────      ────────────────────    ────────────────                             │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── PHASE 1: Device requests a device code ──────                                     │
│     │                        │                        │                                      │
│     │  POST /oauth2/device_authorization              │                                      │
│     │  {                     │                        │                                      │
│     │    client_id=tv-app    │                        │                                      │
│     │    scope=openid profile read                    │                                      │
│     │  }                     │                        │                                      │
│     │───────────────────────>│                        │                                      │
│     │                        │                        │                                      │
│     │                  Auth server generates:         │                                      │
│     │                  → device_code (long, secret)   │                                      │
│     │                  → user_code (short, displayed) │                                      │
│     │                  → verification_uri (URL)       │                                      │
│     │                  → verification_uri_complete    │                                      │
│     │                  → expires_in (lifetime)        │                                      │
│     │                  → interval (polling interval)  │                                      │
│     │                        │                        │                                      │
│     │  200 OK                │                        │                                      │
│     │  {                     │                        │                                      │
│     │    "device_code":      │                        │                                      │
│     │      "GmRh...long",   │                        │                                      │
│     │    "user_code":        │                        │                                      │
│     │      "ABCD-1234",     │                        │                                      │
│     │    "verification_uri": │                        │                                      │
│     │      "http://localhost:9000/activate",          │                                      │
│     │    "verification_uri_complete":                 │                                      │
│     │      "http://localhost:9000/activate            │                                      │
│     │       ?user_code=ABCD-1234",                   │                                      │
│     │    "expires_in": 600,  │                        │                                      │
│     │    "interval": 5       │                        │                                      │
│     │  }                     │                        │                                      │
│     │<───────────────────────│                        │                                      │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── PHASE 2: Device displays code to user ──────                                      │
│     │                        │                        │                                      │
│     │  ┌────────────────────────────────────────┐    │                                      │
│     │  │                                         │    │                                      │
│     │  │   ┌─────────────────────────────────┐  │    │                                      │
│     │  │   │        SMART TV SCREEN          │  │    │                                      │
│     │  │   │                                  │  │    │                                      │
│     │  │   │   To sign in, visit:            │  │    │                                      │
│     │  │   │                                  │  │    │                                      │
│     │  │   │   http://localhost:9000/activate │  │    │                                      │
│     │  │   │                                  │  │    │                                      │
│     │  │   │   and enter code:               │  │    │                                      │
│     │  │   │                                  │  │    │                                      │
│     │  │   │        ┌──────────────┐         │  │    │                                      │
│     │  │   │        │  ABCD-1234   │         │  │    │                                      │
│     │  │   │        └──────────────┘         │  │    │                                      │
│     │  │   │                                  │  │    │                                      │
│     │  │   │   Or scan this QR code:         │  │    │                                      │
│     │  │   │        ┌──────────┐             │  │    │                                      │
│     │  │   │        │ ▓▓░░▓▓░░ │             │  │    │                                      │
│     │  │   │        │ ░░▓▓░░▓▓ │             │  │    │                                      │
│     │  │   │        │ ▓▓░░▓▓░░ │             │  │    │                                      │
│     │  │   │        └──────────┘             │  │    │                                      │
│     │  │   │   (QR = verification_uri_       │  │    │                                      │
│     │  │   │    complete with code embedded) │  │    │                                      │
│     │  │   │                                  │  │    │                                      │
│     │  │   └─────────────────────────────────┘  │    │                                      │
│     │  │                                         │    │                                      │
│     │  └────────────────────────────────────────┘    │                                      │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── PHASE 3: User opens URL on phone/laptop and enters code ──                       │
│     │                        │                        │                                      │
│     │                        │  GET /activate          │                                      │
│     │                        │  (user navigates here) │                                      │
│     │                        │<───────────────────────│                                      │
│     │                        │                        │                                      │
│     │                        │  200 OK                │                                      │
│     │                        │  (Device verification  │                                      │
│     │                        │   page with code input)│                                      │
│     │                        │───────────────────────>│                                      │
│     │                        │                        │                                      │
│     │                        │                        │  User enters:                        │
│     │                        │                        │  ABCD-1234                           │
│     │                        │                        │                                      │
│     │                        │  POST /activate        │                                      │
│     │                        │  { user_code:          │                                      │
│     │                        │    ABCD-1234 }         │                                      │
│     │                        │<───────────────────────│                                      │
│     │                        │                        │                                      │
│     │                        │  Auth server:          │                                      │
│     │                        │  → Validates user_code │                                      │
│     │                        │  → Matches to          │                                      │
│     │                        │    device_code         │                                      │
│     │                        │  → Checks: is user     │                                      │
│     │                        │    authenticated?      │                                      │
│     │                        │  → No → redirect to    │                                      │
│     │                        │    /login              │                                      │
│     │                        │                        │                                      │
│     │                        │  302 → /login          │                                      │
│     │                        │───────────────────────>│                                      │
│     │                        │                        │                                      │
│     │                        │                        │  User logs in:                       │
│     │                        │                        │  username: user                      │
│     │                        │                        │  password: password                  │
│     │                        │                        │                                      │
│     │                        │  POST /login           │                                      │
│     │                        │<───────────────────────│                                      │
│     │                        │                        │                                      │
│     │                        │  Auth server:          │                                      │
│     │                        │  → Validates creds     │                                      │
│     │                        │  → Shows consent:      │                                      │
│     │                        │    "tv-app wants:"    │                                      │
│     │                        │    [✓] profile         │                                      │
│     │                        │    [✓] read            │                                      │
│     │                        │    [Approve]           │                                      │
│     │                        │───────────────────────>│                                      │
│     │                        │                        │                                      │
│     │                        │  POST (approve)        │                                      │
│     │                        │<───────────────────────│                                      │
│     │                        │                        │                                      │
│     │                        │  Auth server:          │                                      │
│     │                        │  → Marks device_code   │                                      │
│     │                        │    as APPROVED          │                                      │
│     │                        │  → Associates user     │                                      │
│     │                        │    with device_code    │                                      │
│     │                        │                        │                                      │
│     │                        │  200 OK                │                                      │
│     │                        │  "Device authorized!   │                                      │
│     │                        │   You can close this   │                                      │
│     │                        │   window."             │                                      │
│     │                        │───────────────────────>│                                      │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── PHASE 4: Device polls for token (meanwhile) ──                                    │
│     │                        │                        │                                      │
│     │  ★ While the user is logging in on their phone,│                                      │
│     │    the DEVICE keeps POLLING the /token endpoint│                                      │
│     │    every N seconds (interval from Phase 1).    │                                      │
│     │                        │                        │                                      │
│     │  POST /oauth2/token    │                        │                                      │
│     │  {                     │                        │                                      │
│     │    grant_type=urn:ietf:params:oauth:            │                                      │
│     │      grant-type:device_code                    │                                      │
│     │    device_code=GmRh...long                     │                                      │
│     │    client_id=tv-app    │                        │                                      │
│     │  }                     │                        │                                      │
│     │───────────────────────>│                        │                                      │
│     │                        │                        │                                      │
│     │  ★ POLL 1 (user hasn't logged in yet):         │                                      │
│     │  400 Bad Request       │                        │                                      │
│     │  {                     │                        │                                      │
│     │    "error":            │                        │                                      │
│     │      "authorization_pending"                   │                                      │
│     │  }                     │                        │                                      │
│     │<───────────────────────│                        │                                      │
│     │                        │                        │                                      │
│     │  (wait 5 seconds...)   │                        │                                      │
│     │                        │                        │                                      │
│     │  ★ POLL 2 (still waiting):                     │                                      │
│     │  POST /oauth2/token    │                        │                                      │
│     │  { same body }         │                        │                                      │
│     │───────────────────────>│                        │                                      │
│     │  400 { "error": "authorization_pending" }      │                                      │
│     │<───────────────────────│                        │                                      │
│     │                        │                        │                                      │
│     │  (wait 5 seconds...)   │                        │                                      │
│     │                        │                        │                                      │
│     │  ★ POLL 3 (user has approved on phone!):       │                                      │
│     │  POST /oauth2/token    │                        │                                      │
│     │  { same body }         │                        │                                      │
│     │───────────────────────>│                        │                                      │
│     │                        │                        │                                      │
│     │                  Auth server:                   │                                      │
│     │                  → device_code is APPROVED!     │                                      │
│     │                  → Signs JWT with RSA key       │                                      │
│     │                  → Issues tokens for the USER   │                                      │
│     │                        │                        │                                      │
│     │  200 OK                │                        │                                      │
│     │  {                     │                        │                                      │
│     │    "access_token": "eyJ...",                   │                                      │
│     │    "id_token": "eyJ...",                       │                                      │
│     │    "refresh_token": "...",                     │                                      │
│     │    "token_type": "Bearer",                    │                                      │
│     │    "expires_in": 3600, │                        │                                      │
│     │    "scope": "openid profile read"             │                                      │
│     │  }                     │                        │                                      │
│     │<───────────────────────│                        │                                      │
│     │                        │                        │                                      │
│     │  ★ Device now has tokens!                      │                                      │
│     │  ★ Token "sub" = the USER (not the device)    │                                      │
│     │  ★ Can call APIs on behalf of the user        │                                      │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── PHASE 5: Device calls Resource Server ──    │                                      │
│     │                        │                        │                                      │
│     │  GET /api/user-profile │                        │                                      │
│     │  Authorization: Bearer <access_token>          │                                      │
│     │───────────────────────────────────────> Resource Server                               │
│     │                                         → Validates JWT                               │
│     │                                         → ✅ Authorized                                │
│     │  200 OK { profile data }                                                              │
│     │<─────────────────────────────────────── Resource Server                               │
│     │                        │                        │                                      │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.3 ★ Polling Responses — What the Device Receives During Polling

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ POLLING RESPONSES — WHAT THE DEVICE SEES WHILE POLLING /oauth2/token                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The device keeps POSTing to /oauth2/token every "interval" seconds.        │           │
│  │  It receives one of these responses:                                         │           │
│  │                                                                               │           │
│  │  ┌─────────────────────────────┬──────────────────────────────────────────┐ │           │
│  │  │  Response                   │  Meaning & Device Action                  │ │           │
│  │  ├─────────────────────────────┼──────────────────────────────────────────┤ │           │
│  │  │                             │                                           │ │           │
│  │  │  400 Bad Request            │  User hasn't done anything yet.          │ │           │
│  │  │  {                          │  → Keep polling (wait "interval" secs)   │ │           │
│  │  │    "error":                 │                                           │ │           │
│  │  │    "authorization_pending"  │                                           │ │           │
│  │  │  }                          │                                           │ │           │
│  │  │                             │                                           │ │           │
│  │  ├─────────────────────────────┼──────────────────────────────────────────┤ │           │
│  │  │                             │                                           │ │           │
│  │  │  400 Bad Request            │  Device is polling too fast!             │ │           │
│  │  │  {                          │  → Increase interval by 5 seconds       │ │           │
│  │  │    "error":                 │  → Then continue polling                 │ │           │
│  │  │    "slow_down"              │                                           │ │           │
│  │  │  }                          │                                           │ │           │
│  │  │                             │                                           │ │           │
│  │  ├─────────────────────────────┼──────────────────────────────────────────┤ │           │
│  │  │                             │                                           │ │           │
│  │  │  400 Bad Request            │  The device_code has expired.            │ │           │
│  │  │  {                          │  → Stop polling                          │ │           │
│  │  │    "error":                 │  → Start over (request new device_code)  │ │           │
│  │  │    "expired_token"          │                                           │ │           │
│  │  │  }                          │                                           │ │           │
│  │  │                             │                                           │ │           │
│  │  ├─────────────────────────────┼──────────────────────────────────────────┤ │           │
│  │  │                             │                                           │ │           │
│  │  │  400 Bad Request            │  User denied the authorization.          │ │           │
│  │  │  {                          │  → Stop polling                          │ │           │
│  │  │    "error":                 │  → Show "Access denied" message          │ │           │
│  │  │    "access_denied"          │                                           │ │           │
│  │  │  }                          │                                           │ │           │
│  │  │                             │                                           │ │           │
│  │  ├─────────────────────────────┼──────────────────────────────────────────┤ │           │
│  │  │                             │                                           │ │           │
│  │  │  200 OK                     │  ★ User approved! Token issued!          │ │           │
│  │  │  {                          │  → Stop polling                          │ │           │
│  │  │    "access_token": "eyJ..", │  → Store tokens                         │ │           │
│  │  │    "token_type": "Bearer",  │  → Start making API calls               │ │           │
│  │  │    "expires_in": 3600,      │                                           │ │           │
│  │  │    "scope": "..."           │                                           │ │           │
│  │  │  }                          │                                           │ │           │
│  │  │                             │                                           │ │           │
│  │  └─────────────────────────────┴──────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.4 ★ Architecture — 3 Applications for Device Code Flow

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ARCHITECTURE — DEVICE CODE FLOW APPLICATIONS                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐            │
│  │                                                                              │            │
│  │  ┌───────────────────────────────────────────────────────────┐              │            │
│  │  │  1. AUTHORIZATION SERVER  (Port 9000)                      │              │            │
│  │  │     → Same as section 20.14 + Device Code support          │              │            │
│  │  │     → Endpoints:                                            │              │            │
│  │  │       /oauth2/device_authorization (device requests code)  │              │            │
│  │  │       /oauth2/device_verification  (user enters code)      │              │            │
│  │  │       /oauth2/token (device polls for token)               │              │            │
│  │  │     → RegisteredClient with DEVICE_CODE grant type         │              │            │
│  │  └───────────────────────────────────────────────────────────┘              │            │
│  │                      ▲               ▲                                       │            │
│  │        1. POST       │               │ 3. User enters                       │            │
│  │   /device_           │               │    code & logs in                    │            │
│  │   authorization      │               │    (on phone/laptop)                 │            │
│  │                      │               │                                       │            │
│  │        4. POST       │               │                                       │            │
│  │   /token (poll)      │               │                                       │            │
│  │                      │               │                                       │            │
│  │  ┌──────────────────┴───┐   ┌──────┴─────────────────────────┐             │            │
│  │  │  2. DEVICE / CLIENT   │   │  User's Phone / Laptop          │             │            │
│  │  │  (Smart TV / CLI)     │   │  (Browser)                      │             │            │
│  │  │                       │   │                                  │             │            │
│  │  │  → Shows user_code    │   │  → Opens verification_uri      │             │            │
│  │  │  → Polls /token       │   │  → Enters user_code            │             │            │
│  │  │  → Uses access_token  │   │  → Logs in (username/password) │             │            │
│  │  │    to call APIs       │   │  → Consents to scopes          │             │            │
│  │  └──────────┬────────────┘   └─────────────────────────────────┘             │            │
│  │             │                                                                │            │
│  │             │ 5. GET /api/data                                               │            │
│  │             │    Authorization: Bearer <token>                               │            │
│  │             ▼                                                                │            │
│  │  ┌───────────────────────────────────────────────────────────┐              │            │
│  │  │  3. RESOURCE SERVER  (Port 8090)                            │              │            │
│  │  │     → Same as section 20.14 (ZERO changes!)                │              │            │
│  │  │     → Validates JWT access token                           │              │            │
│  │  └───────────────────────────────────────────────────────────┘              │            │
│  │                                                                              │            │
│  └─────────────────────────────────────────────────────────────────────────────┘            │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.5 ★ Authorization Server — Enabling Device Code Grant

**pom.xml — Authorization Server (Same as 20.14)**

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Authorization Server (same dependency as 20.14)                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- ★ Spring Authorization Server — supports Device Code since 1.1+ -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

</dependencies>
```

**AuthorizationServerConfig.java — Enabling Device Code Grant**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AuthorizationServerConfig.java — Authorization Server with Device Code
//
//  ★ Changes from section 20.14:
//    1. Enable device_code grant in SecurityFilterChain
//    2. Add RegisteredClient with DEVICE_CODE grant type
//    3. Add deviceCodeEndpoint() customization
//  ★ Keep ALL other beans from 20.14 (JWKSource, UserDetailsService, etc.)
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    // ══════════════════════════════════════════════════════════════════════════
    //  1. AUTHORIZATION SERVER SECURITY FILTER CHAIN
    //     → Added: .deviceCodeEndpoint() and .deviceVerificationEndpoint()
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                // ★ Enable OIDC (same as 20.14)
                .oidc(Customizer.withDefaults())

                // ★★★ NEW: Enable Device Code Grant endpoints!
                // This auto-configures:
                //   → /oauth2/device_authorization (POST — device requests code)
                //   → /oauth2/device_verification  (GET/POST — user enters code)
                .deviceAuthorizationEndpoint(Customizer.withDefaults())
                .deviceVerificationEndpoint(Customizer.withDefaults());

        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
        );

        http.oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(Customizer.withDefaults())
        );

        return http.build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  2. DEFAULT SECURITY FILTER CHAIN (same as 20.14)
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  3. REGISTERED CLIENT REPOSITORY
    //     → Added: RegisteredClient for device code (Smart TV / CLI)
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        // ── Web Client (Authorization Code — from 20.14) ─────────────────────
        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("my-client-app")
                .clientSecret(passwordEncoder().encode("my-client-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/login/oauth2/code/my-auth-server")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("read")
                .scope("write")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        .build())
                .build();

        // ── Device Client (Device Code — NEW!) ──────────────────────────────
        //    ★ This is the Smart TV / CLI / IoT device client.

        RegisteredClient deviceClient = RegisteredClient.withId(UUID.randomUUID().toString())

                // ★ client-id for the device application
                .clientId("tv-app")

                // ★★★ CLIENT_SECRET vs NO SECRET:
                // For CONFIDENTIAL devices (server-side CLI): use CLIENT_SECRET_BASIC
                // For PUBLIC devices (Smart TV, game console): use NONE
                //
                // Smart TVs and IoT devices are considered PUBLIC clients
                // because they can't keep a secret (the app binary can be decompiled).
                // So we use ClientAuthenticationMethod.NONE.
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)

                // ★★★ DEVICE_CODE grant type — the key config!
                .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                // ★ REFRESH_TOKEN — so the device can get new access tokens
                //   without requiring the user to re-authorize
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)

                // ★ NO .redirectUri() — no browser redirect on the device!
                // (The user authorizes on a DIFFERENT device)

                // ★ Scopes the device can request
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("read")

                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        // ★ Reuse refresh tokens = false → more secure
                        // Each refresh issues a new refresh_token
                        .reuseRefreshTokens(false)
                        .build()
                )

                .clientSettings(ClientSettings.builder()
                        // ★ Require consent — user must approve scopes
                        .requireAuthorizationConsent(true)
                        // ★ No PKCE needed for device code flow
                        .requireProofKey(false)
                        .build()
                )
                .build();

        return new InMemoryRegisteredClientRepository(webClient, deviceClient);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Keep ALL other beans from section 20.14:
    //  → userDetailsService()
    //  → passwordEncoder()
    //  → jwkSource()
    //  → jwtDecoder()
    //  → authorizationServerSettings()
    // ══════════════════════════════════════════════════════════════════════════

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")
                // ★ Device code endpoints are auto-configured with default paths:
                // .deviceAuthorizationEndpoint("/oauth2/device_authorization")
                // .deviceVerificationEndpoint("/oauth2/device_verification")
                .build();
    }
}
```

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ KEY DIFFERENCES — DEVICE CODE RegisteredClient                                           │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Authorization Code Client           Device Code Client                      │           │
│  │  ─────────────────────────           ──────────────────                      │           │
│  │                                                                               │           │
│  │  .clientAuthenticationMethod(        .clientAuthenticationMethod(             │           │
│  │    CLIENT_SECRET_BASIC)                NONE)  ★ Public client!               │           │
│  │                                                                               │           │
│  │  .authorizationGrantType(            .authorizationGrantType(                │           │
│  │    AUTHORIZATION_CODE)                 DEVICE_CODE) ★★★                      │           │
│  │                                                                               │           │
│  │  .redirectUri(                       ★ NO .redirectUri()!                    │           │
│  │    "http://localhost:8080/...")       (no browser on device)                  │           │
│  │                                                                               │           │
│  │  .clientSecret(...)                  ★ NO .clientSecret()!                   │           │
│  │                                       (public client → no secret)            │           │
│  │                                                                               │           │
│  │  SecurityFilterChain:                SecurityFilterChain:                     │           │
│  │  (default from 20.14)                + .deviceAuthorizationEndpoint(...)     │           │
│  │                                       + .deviceVerificationEndpoint(...)     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.6 ★ Endpoints Provided by the Authorization Server for Device Code

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ DEVICE CODE ENDPOINTS — AUTO-CONFIGURED BY SPRING AUTHORIZATION SERVER                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Endpoint                          Who calls it    Purpose                   │           │
│  │  ────────────────────────────────  ──────────────  ─────────────────────     │           │
│  │                                                                               │           │
│  │  POST /oauth2/device_authorization Device          Request device_code +     │           │
│  │                                                     user_code               │           │
│  │                                                     Input: client_id, scope  │           │
│  │                                                     Output: device_code,     │           │
│  │                                                       user_code,             │           │
│  │                                                       verification_uri       │           │
│  │                                                                               │           │
│  │  GET  /oauth2/device_verification  User (browser)  Shows the code entry     │           │
│  │                                                     page where user types   │           │
│  │                                                     the user_code           │           │
│  │                                                                               │           │
│  │  POST /oauth2/device_verification  User (browser)  Submits the user_code    │           │
│  │                                                     → triggers login +      │           │
│  │                                                     consent flow            │           │
│  │                                                                               │           │
│  │  POST /oauth2/token                Device          Polls for tokens         │           │
│  │                                     (polling)       Input: grant_type=       │           │
│  │                                                       device_code,           │           │
│  │                                                       device_code,           │           │
│  │                                                       client_id             │           │
│  │                                                     Output: access_token    │           │
│  │                                                       OR error              │           │
│  │                                                                               │           │
│  │  ★ Existing endpoints from 20.14 also still work:                           │           │
│  │    /oauth2/authorize, /oauth2/token (other grants), /oauth2/jwks,          │           │
│  │    /userinfo, /.well-known/openid-configuration                             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.7 ★ Device Client Application — CLI / Smart TV Simulator

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ DEVICE CLIENT — THE SMART TV / CLI APPLICATION                                           │
│    This is NOT a web application!                                                            │
│    It's a console/CLI app (or embedded device app) that:                                     │
│    1. Requests a device code from the auth server                                            │
│    2. Displays the user_code and verification_uri                                            │
│    3. Polls /oauth2/token until the user approves                                            │
│    4. Uses the access_token to call APIs                                                     │
│                                                                                              │
│    ★ We do NOT use spring-boot-starter-oauth2-client here!                                  │
│      Spring's OAuth2 client doesn't have built-in Device Code support.                      │
│      The device implements the flow manually using RestClient / WebClient.                   │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**pom.xml — Device Client (CLI Simulator)**

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Device Client (CLI / Smart TV simulator)                            -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- Spring Boot Starter Web (for RestClient) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Jackson for JSON parsing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

</dependencies>
```

**DeviceClientApplication.java — Device Code Flow Implementation**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  DeviceClientApplication.java — CLI/Smart TV Device Code Flow
//
//  ★ This simulates a Smart TV or CLI tool performing the Device Code flow.
//  ★ It runs as a Spring Boot CommandLineRunner (console app, no web server).
//  ★ Steps:
//    1. POST /oauth2/device_authorization → get device_code + user_code
//    2. Display user_code and verification_uri to the user (console output)
//    3. Poll POST /oauth2/token every N seconds until approved
//    4. Use access_token to call the resource server
// ═══════════════════════════════════════════════════════════════════════════════

@SpringBootApplication
public class DeviceClientApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DeviceClientApplication.class);

    private static final String AUTH_SERVER = "http://localhost:9000";
    private static final String CLIENT_ID = "tv-app";
    private static final String SCOPES = "openid profile read";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        // ★ Disable the web server — this is a CLI app!
        new SpringApplicationBuilder(DeviceClientApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void run(String... args) throws Exception {

        // ══════════════════════════════════════════════════════════════════════
        //  STEP 1: Request device code from the authorization server
        // ══════════════════════════════════════════════════════════════════════

        log.info("=== DEVICE CODE FLOW ===");
        log.info("Step 1: Requesting device code from authorization server...");

        String deviceAuthResponse = restClient
                .post()
                .uri(AUTH_SERVER + "/oauth2/device_authorization")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("client_id=" + CLIENT_ID + "&scope=" + SCOPES.replace(" ", "+"))
                .retrieve()
                .body(String.class);

        JsonNode deviceAuth = objectMapper.readTree(deviceAuthResponse);

        String deviceCode = deviceAuth.get("device_code").asText();
        String userCode = deviceAuth.get("user_code").asText();
        String verificationUri = deviceAuth.get("verification_uri").asText();
        String verificationUriComplete = deviceAuth.has("verification_uri_complete")
                ? deviceAuth.get("verification_uri_complete").asText()
                : null;
        int expiresIn = deviceAuth.get("expires_in").asInt();
        int interval = deviceAuth.has("interval") ? deviceAuth.get("interval").asInt() : 5;

        // ══════════════════════════════════════════════════════════════════════
        //  STEP 2: Display code to the user (this is what the TV screen shows)
        // ══════════════════════════════════════════════════════════════════════

        log.info("Step 2: Display this to the user:");
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║                                                  ║");
        System.out.println("║   To sign in, open a browser and visit:          ║");
        System.out.println("║                                                  ║");
        System.out.println("║   " + verificationUri);
        System.out.println("║                                                  ║");
        System.out.println("║   And enter the code:                            ║");
        System.out.println("║                                                  ║");
        System.out.println("║          ┌────────────────┐                      ║");
        System.out.println("║          │   " + userCode + "   │                      ║");
        System.out.println("║          └────────────────┘                      ║");
        System.out.println("║                                                  ║");
        if (verificationUriComplete != null) {
            System.out.println("║   Or visit directly:                             ║");
            System.out.println("║   " + verificationUriComplete);
        }
        System.out.println("║                                                  ║");
        System.out.println("║   This code expires in " + expiresIn + " seconds.             ║");
        System.out.println("║                                                  ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        // ══════════════════════════════════════════════════════════════════════
        //  STEP 3: Poll for token (device keeps asking "has the user approved?")
        // ══════════════════════════════════════════════════════════════════════

        log.info("Step 3: Polling for token every {} seconds...", interval);

        String accessToken = null;
        int pollInterval = interval;
        long startTime = System.currentTimeMillis();
        long maxWaitMillis = expiresIn * 1000L;

        while (accessToken == null) {

            // Check if device_code has expired
            if (System.currentTimeMillis() - startTime > maxWaitMillis) {
                log.error("Device code expired! Please start over.");
                return;
            }

            // Wait for the polling interval
            Thread.sleep(pollInterval * 1000L);

            try {
                // ★ POST /oauth2/token with grant_type=device_code
                String tokenResponse = restClient
                        .post()
                        .uri(AUTH_SERVER + "/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body("grant_type=" +
                              "urn:ietf:params:oauth:grant-type:device_code" +
                              "&device_code=" + deviceCode +
                              "&client_id=" + CLIENT_ID)
                        .retrieve()
                        .body(String.class);

                // ★ 200 OK — user approved! Token received!
                JsonNode tokenJson = objectMapper.readTree(tokenResponse);
                accessToken = tokenJson.get("access_token").asText();

                log.info("★ Token received! User has authorized the device!");

                if (tokenJson.has("refresh_token")) {
                    String refreshToken = tokenJson.get("refresh_token").asText();
                    log.info("Refresh token: {}...", refreshToken.substring(0, 20));
                }

            } catch (Exception e) {
                // ★ Non-200 response — check the error
                String errorBody = e.getMessage();

                if (errorBody != null && errorBody.contains("authorization_pending")) {
                    // User hasn't approved yet — keep polling
                    log.info("  Waiting... (authorization_pending)");
                } else if (errorBody != null && errorBody.contains("slow_down")) {
                    // Polling too fast — increase interval
                    pollInterval += 5;
                    log.info("  Slowing down... (new interval: {} seconds)", pollInterval);
                } else if (errorBody != null && errorBody.contains("access_denied")) {
                    // User denied the authorization
                    log.error("User denied access!");
                    return;
                } else if (errorBody != null && errorBody.contains("expired_token")) {
                    // Device code expired
                    log.error("Device code expired! Please start over.");
                    return;
                } else {
                    log.warn("  Unexpected error: {}", errorBody);
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        //  STEP 4: Use the access token to call the Resource Server!
        // ══════════════════════════════════════════════════════════════════════

        log.info("Step 4: Calling resource server with access token...");

        String apiResponse = restClient
                .get()
                .uri("http://localhost:8090/api/users")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(String.class);

        log.info("★ Resource server response: {}", apiResponse);
        log.info("=== DEVICE CODE FLOW COMPLETE ===");
    }
}
```

**Required Imports — Device Client Application**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Imports for DeviceClientApplication.java
// ═══════════════════════════════════════════════════════════════════════════════

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
```

---

##### 20.16.8 ★ Resource Server — No Changes Needed!

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ RESOURCE SERVER — EXACTLY THE SAME AS SECTIONS 20.14 AND 20.15!                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The Resource Server doesn't care about the grant type!                     │           │
│  │  It just validates the JWT access token.                                     │           │
│  │                                                                               │           │
│  │  ★ ZERO changes from section 20.14.3!                                       │           │
│  │  ★ Same application.yml (issuer-uri: http://localhost:9000)                 │           │
│  │  ★ Same ResourceServerConfig.java                                            │           │
│  │  ★ Same ApiController.java                                                   │           │
│  │                                                                               │           │
│  │  Token from Device Code flow:                                                │           │
│  │  {                                                                            │           │
│  │    "iss": "http://localhost:9000",                                           │           │
│  │    "sub": "user",    ← the USER who approved on their phone                │           │
│  │    "aud": "tv-app",  ← the device client                                   │           │
│  │    "scope": "openid profile read",                                          │           │
│  │    "iat": 1715184000,                                                        │           │
│  │    "exp": 1715187600                                                         │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ "sub" = USERNAME (like Authorization Code), NOT client_id                │           │
│  │    (unlike Client Credentials where sub = client_id)                        │           │
│  │  ★ The token represents the USER acting through the DEVICE                  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.9 ★ Raw HTTP — Device Authorization Request and Response

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ RAW HTTP — DEVICE AUTHORIZATION REQUEST AND TOKEN POLLING                                │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── 1. DEVICE AUTHORIZATION REQUEST ──────────────────────────               │           │
│  │                                                                               │           │
│  │  POST /oauth2/device_authorization HTTP/1.1                                  │           │
│  │  Host: localhost:9000                                                         │           │
│  │  Content-Type: application/x-www-form-urlencoded                             │           │
│  │                                                                               │           │
│  │  client_id=tv-app&scope=openid+profile+read                                 │           │
│  │                                                                               │           │
│  │  ★ Note: NO client_secret! (public client, NONE auth method)               │           │
│  │  ★ Note: NO redirect_uri! (no browser redirect)                            │           │
│  │                                                                               │           │
│  │  ── RESPONSE ─────────────────────────────────────────────────               │           │
│  │                                                                               │           │
│  │  HTTP/1.1 200 OK                                                             │           │
│  │  Content-Type: application/json                                               │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "device_code": "GmRhmhcxhwAzkoEqiMEg_DnyEysNkuNhszIySk9eS",            │           │
│  │    "user_code": "BCDF-GHJK",                                                │           │
│  │    "verification_uri": "http://localhost:9000/oauth2/device_verification",  │           │
│  │    "verification_uri_complete":                                               │           │
│  │      "http://localhost:9000/oauth2/device_verification?user_code=BCDF-GHJK",│           │
│  │    "expires_in": 300,                                                         │           │
│  │    "interval": 5                                                              │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ device_code:    Long, secret code — used by device to poll for token    │           │
│  │  ★ user_code:      Short, human-readable — displayed on device screen      │           │
│  │  ★ verification_uri: URL the user opens on their phone/laptop              │           │
│  │  ★ verification_uri_complete: URL with user_code pre-filled                │           │
│  │  ★ expires_in:     Seconds until the codes expire (5 min here)             │           │
│  │  ★ interval:       Seconds between polling attempts (5 sec here)           │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 2. TOKEN POLLING REQUEST ─────────────────────────────────               │           │
│  │                                                                               │           │
│  │  POST /oauth2/token HTTP/1.1                                                 │           │
│  │  Host: localhost:9000                                                         │           │
│  │  Content-Type: application/x-www-form-urlencoded                             │           │
│  │                                                                               │           │
│  │  grant_type=urn:ietf:params:oauth:grant-type:device_code                    │           │
│  │  &device_code=GmRhmhcxhwAzkoEqiMEg_DnyEysNkuNhszIySk9eS                   │           │
│  │  &client_id=tv-app                                                           │           │
│  │                                                                               │           │
│  │  ★ grant_type: urn:ietf:params:oauth:grant-type:device_code                │           │
│  │    (full URN, not just "device_code"!)                                       │           │
│  │  ★ device_code: the long code from step 1                                  │           │
│  │  ★ client_id: identifies which device app is polling                        │           │
│  │                                                                               │           │
│  │  ── POLLING RESPONSE (pending) ───────────────────────────────               │           │
│  │                                                                               │           │
│  │  HTTP/1.1 400 Bad Request                                                    │           │
│  │  { "error": "authorization_pending" }                                        │           │
│  │                                                                               │           │
│  │  ── POLLING RESPONSE (success) ───────────────────────────────               │           │
│  │                                                                               │           │
│  │  HTTP/1.1 200 OK                                                             │           │
│  │  {                                                                            │           │
│  │    "access_token": "eyJraWQiOiI3ZjM4YmQ0Ny...",                            │           │
│  │    "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2g...",                          │           │
│  │    "id_token": "eyJhbGciOiJSUzI1NiIs...",                                  │           │
│  │    "token_type": "Bearer",                                                   │           │
│  │    "expires_in": 3600,                                                        │           │
│  │    "scope": "openid profile read"                                            │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ UNLIKE Client Credentials, Device Code CAN return:                       │           │
│  │    → access_token  (always)                                                  │           │
│  │    → id_token      (if openid scope was requested — it's user-based!)       │           │
│  │    → refresh_token (if REFRESH_TOKEN grant is enabled)                      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.10 ★ Testing with curl — Manual Device Code Flow

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ TESTING WITH curl — MANUAL DEVICE CODE FLOW                                             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── Step 1: Request device code ──────────────────────────────               │           │
│  │                                                                               │           │
│  │  $ curl -X POST http://localhost:9000/oauth2/device_authorization \          │           │
│  │      -d "client_id=tv-app&scope=openid+profile+read"                        │           │
│  │                                                                               │           │
│  │  Response:                                                                    │           │
│  │  {                                                                            │           │
│  │    "device_code": "GmRhmhcx...",                                            │           │
│  │    "user_code": "BCDF-GHJK",                                                │           │
│  │    "verification_uri": "http://localhost:9000/oauth2/device_verification",  │           │
│  │    "expires_in": 300,                                                         │           │
│  │    "interval": 5                                                              │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ── Step 2: Open browser and go to verification_uri ─────────────            │           │
│  │                                                                               │           │
│  │  Open: http://localhost:9000/oauth2/device_verification                      │           │
│  │  → Enter the user_code: BCDF-GHJK                                          │           │
│  │  → Log in as: user / password                                               │           │
│  │  → Approve the consent                                                       │           │
│  │                                                                               │           │
│  │  ── Step 3: Poll for token ───────────────────────────────────               │           │
│  │                                                                               │           │
│  │  $ curl -X POST http://localhost:9000/oauth2/token \                         │           │
│  │      -d "grant_type=urn:ietf:params:oauth:grant-type:device_code\           │           │
│  │      &device_code=GmRhmhcx...\                                              │           │
│  │      &client_id=tv-app"                                                      │           │
│  │                                                                               │           │
│  │  Before approval: { "error": "authorization_pending" }                      │           │
│  │  After approval:  { "access_token": "eyJ...", "id_token": "eyJ..." }       │           │
│  │                                                                               │           │
│  │  ── Step 4: Use the token ────────────────────────────────────               │           │
│  │                                                                               │           │
│  │  $ curl http://localhost:8090/api/users \                                    │           │
│  │      -H "Authorization: Bearer eyJ..."                                      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.11 ★ Internal Filter Flow — What Happens Inside Spring Authorization Server

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ INTERNAL FILTER FLOW — DEVICE CODE PROCESSING IN SPRING AUTHORIZATION SERVER             │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── 1. POST /oauth2/device_authorization ────────────────────────            │           │
│  │                                                                               │           │
│  │  OAuth2DeviceAuthorizationEndpointFilter                                     │           │
│  │    │                                                                          │           │
│  │    │  1. OAuth2DeviceAuthorizationRequestAuthenticationConverter             │           │
│  │    │     → Extracts client_id and scope from request body                   │           │
│  │    │     → Creates OAuth2DeviceAuthorizationRequestAuthenticationToken       │           │
│  │    │                                                                          │           │
│  │    │  2. OAuth2DeviceAuthorizationRequestAuthenticationProvider              │           │
│  │    │     → Validates client_id exists in RegisteredClientRepository          │           │
│  │    │     → Checks client has DEVICE_CODE grant type                         │           │
│  │    │     → Validates requested scopes are allowed                            │           │
│  │    │     → Generates device_code (long, random, URL-safe)                   │           │
│  │    │     → Generates user_code (short, human-readable, e.g., BCDF-GHJK)    │           │
│  │    │     → Stores mapping: device_code → { client_id, scopes, user_code,   │           │
│  │    │                                        status: PENDING, expires_at }    │           │
│  │    │     → Creates OAuth2DeviceAuthorizationConsentAuthenticationToken       │           │
│  │    │                                                                          │           │
│  │    │  3. Returns JSON response:                                              │           │
│  │    │     { device_code, user_code, verification_uri, expires_in, interval } │           │
│  │    │                                                                          │           │
│  │                                                                               │           │
│  │  ── 2. GET/POST /oauth2/device_verification ────────────────────             │           │
│  │                                                                               │           │
│  │  OAuth2DeviceVerificationEndpointFilter                                      │           │
│  │    │                                                                          │           │
│  │    │  1. User opens verification_uri in browser                             │           │
│  │    │     → If not authenticated → redirect to /login (form login)           │           │
│  │    │     → If authenticated → show code entry page                          │           │
│  │    │                                                                          │           │
│  │    │  2. User submits user_code                                              │           │
│  │    │     → Validates user_code matches a pending device_code                │           │
│  │    │     → If requireAuthorizationConsent=true → show consent page          │           │
│  │    │     → User approves scopes                                              │           │
│  │    │                                                                          │           │
│  │    │  3. OAuth2DeviceCodeAuthenticationProvider                              │           │
│  │    │     → Updates stored device_code status: PENDING → APPROVED            │           │
│  │    │     → Associates the authenticated user with the device_code           │           │
│  │    │     → Shows "Device authorized! You can close this window."            │           │
│  │    │                                                                          │           │
│  │                                                                               │           │
│  │  ── 3. POST /oauth2/token (device polling) ─────────────────────            │           │
│  │                                                                               │           │
│  │  OAuth2TokenEndpointFilter                                                   │           │
│  │    │                                                                          │           │
│  │    │  1. Detects grant_type = urn:ietf:params:oauth:grant-type:device_code  │           │
│  │    │     → Uses OAuth2DeviceCodeAuthenticationConverter                      │           │
│  │    │     → Extracts device_code and client_id                               │           │
│  │    │                                                                          │           │
│  │    │  2. OAuth2DeviceCodeAuthenticationProvider                              │           │
│  │    │     → Looks up device_code in storage                                  │           │
│  │    │     → Validates client_id matches                                      │           │
│  │    │     → Checks status:                                                    │           │
│  │    │                                                                          │           │
│  │    │     ├── PENDING → return 400 { "error": "authorization_pending" }      │           │
│  │    │     ├── EXPIRED → return 400 { "error": "expired_token" }              │           │
│  │    │     ├── DENIED  → return 400 { "error": "access_denied" }              │           │
│  │    │     └── APPROVED →                                                      │           │
│  │    │          → Retrieve authenticated user associated with device_code     │           │
│  │    │          → OAuth2TokenGenerator → JwtGenerator                          │           │
│  │    │          → Sign JWT with RSA private key                               │           │
│  │    │          → Set claims: iss, sub=username, aud, scope, iat, exp        │           │
│  │    │          → Generate refresh_token (if grant type enabled)              │           │
│  │    │          → Generate id_token (if openid scope requested)               │           │
│  │    │          → Return 200 { access_token, refresh_token, id_token }       │           │
│  │    │          → Mark device_code as CONSUMED (can't be used again)          │           │
│  │    │                                                                          │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.12 ★ Security Considerations — Device Code Flow

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SECURITY CONSIDERATIONS — DEVICE CODE FLOW                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. SHORT USER CODE                                                           │           │
│  │     • user_code is SHORT and human-readable (e.g., BCDF-GHJK)              │           │
│  │     • Easy to type on any device                                             │           │
│  │     • Has limited character set (avoids 0/O, 1/I confusion)                 │           │
│  │     • Short-lived (typically 5-15 minutes)                                  │           │
│  │                                                                               │           │
│  │  2. DEVICE CODE IS SECRET                                                     │           │
│  │     • device_code is long, random, and URL-safe                             │           │
│  │     • Only the device knows it (not displayed to user)                      │           │
│  │     • Used only for polling — never transmitted through browser             │           │
│  │                                                                               │           │
│  │  3. RATE LIMITING (slow_down)                                                │           │
│  │     • Auth server returns "slow_down" if device polls too fast             │           │
│  │     • Prevents brute-force attacks on device_code                           │           │
│  │     • Device must increase polling interval                                 │           │
│  │                                                                               │           │
│  │  4. CODE EXPIRATION                                                           │           │
│  │     • Both codes expire after a short time (typically 5-15 minutes)         │           │
│  │     • Limits the window for attacks                                          │           │
│  │                                                                               │           │
│  │  5. ONE-TIME USE                                                              │           │
│  │     • device_code is consumed after successful token exchange               │           │
│  │     • Cannot be reused even if intercepted                                  │           │
│  │                                                                               │           │
│  │  6. PUBLIC CLIENT CONSIDERATIONS                                              │           │
│  │     • Device Code clients are typically PUBLIC (no client_secret)           │           │
│  │     • Anyone with the client_id can START the flow                          │           │
│  │     • But they CANNOT complete it without user approval                     │           │
│  │     • User must physically approve on a separate, trusted device            │           │
│  │                                                                               │           │
│  │  7. PHISHING RISK                                                             │           │
│  │     • Attacker could start a Device Code flow and trick user into           │           │
│  │       approving it (remote phishing attack)                                 │           │
│  │     • Mitigation: Auth server should clearly show WHAT is being authorized  │           │
│  │     • Mitigation: Show client name, requested scopes, warning messages     │           │
│  │     • Mitigation: Some implementations require the user to confirm          │           │
│  │       a code match on both devices                                           │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.13 ★ Real-World Use Cases — Device Code Flow

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ REAL-WORLD USE CASES — DEVICE CODE FLOW                                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── 1. SMART TV APPS ─────────────────────────────────────────               │           │
│  │                                                                               │           │
│  │  ┌──────────────────────┐     ┌───────────┐     ┌──────────────┐           │           │
│  │  │  Netflix/YouTube     │     │ Auth      │     │ User's Phone │           │           │
│  │  │  on Smart TV         │ ──> │ Server    │ <── │ (Browser)    │           │           │
│  │  │                      │     │           │     │              │           │           │
│  │  │  "Enter code at      │     └───────────┘     │  Opens URL   │           │           │
│  │  │   netflix.com/       │                        │  Enters code │           │           │
│  │  │   activate"          │                        │  Logs in     │           │           │
│  │  └──────────────────────┘                        └──────────────┘           │           │
│  │                                                                               │           │
│  │  ── 2. CLI TOOLS ─────────────────────────────────────────────               │           │
│  │                                                                               │           │
│  │  $ gh auth login                                                             │           │
│  │  ! First copy your one-time code: ABCD-1234                                 │           │
│  │  Press Enter to open github.com/login/device in your browser...             │           │
│  │  ✓ Authentication complete.                                                  │           │
│  │                                                                               │           │
│  │  $ az login --use-device-code                                                │           │
│  │  To sign in, use a web browser to open the page                             │           │
│  │  https://microsoft.com/devicelogin and enter the code G2S9K32DP             │           │
│  │                                                                               │           │
│  │  ── 3. IoT DEVICES ───────────────────────────────────────────               │           │
│  │                                                                               │           │
│  │  Smart home hub, smart speaker, security camera                              │           │
│  │  → Display code on LED screen or speak it                                   │           │
│  │  → User authorizes via phone app                                            │           │
│  │                                                                               │           │
│  │  ── 4. GAME CONSOLES ─────────────────────────────────────────               │           │
│  │                                                                               │           │
│  │  PlayStation / Xbox / Nintendo Switch                                        │           │
│  │  → Linking to a streaming service account                                   │           │
│  │  → Easier than typing email/password with a game controller                 │           │
│  │                                                                               │           │
│  │  ── 5. HEADLESS SERVERS ───────────────────────────────────────               │           │
│  │                                                                               │           │
│  │  SSH into a remote server → need to authenticate                            │           │
│  │  → No browser available on the server                                       │           │
│  │  → Device code flow lets you auth on your local machine                     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.16.14 ★ Summary — Device Authorization Grant Flow

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — DEVICE AUTHORIZATION GRANT FLOW (RFC 8628)                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── WHAT IS IT ───────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • OAuth 2.0 grant type for devices WITHOUT a browser or with limited      │           │
│  │    input (Smart TVs, CLIs, IoT, game consoles, headless servers)            │           │
│  │  • Device shows a SHORT CODE + URL to the user                             │           │
│  │  • User opens URL on their phone/laptop, enters code, logs in, approves    │           │
│  │  • Device POLLS the /token endpoint until the user approves                │           │
│  │  • Token represents the USER (not the device) — sub = username             │           │
│  │  • Returns access_token + refresh_token + id_token (if openid scope)       │           │
│  │                                                                               │           │
│  │  ── 5-PHASE FLOW ────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Phase 1: Device → POST /oauth2/device_authorization → gets codes          │           │
│  │  Phase 2: Device displays user_code + verification_uri to user             │           │
│  │  Phase 3: User opens URL on phone → enters code → logs in → approves      │           │
│  │  Phase 4: Device polls POST /oauth2/token → authorization_pending...       │           │
│  │           → eventually 200 OK with tokens!                                  │           │
│  │  Phase 5: Device uses access_token to call Resource Server APIs            │           │
│  │                                                                               │           │
│  │  ── AUTHORIZATION SERVER ─────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • Same as section 20.14 + enable device code endpoints:                    │           │
│  │    → .deviceAuthorizationEndpoint(Customizer.withDefaults())               │           │
│  │    → .deviceVerificationEndpoint(Customizer.withDefaults())                │           │
│  │  • RegisteredClient with:                                                    │           │
│  │    → .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)           │           │
│  │    → .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)           │           │
│  │    → NO .redirectUri(), NO .clientSecret() (public client)                 │           │
│  │                                                                               │           │
│  │  ── NEW ENDPOINTS ────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • POST /oauth2/device_authorization → device requests codes               │           │
│  │  • GET/POST /oauth2/device_verification → user enters code + logs in       │           │
│  │  • POST /oauth2/token (with grant_type=device_code) → device polls         │           │
│  │                                                                               │           │
│  │  ── DEVICE CLIENT ────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • NOT a web app — console/CLI/embedded app                                 │           │
│  │  • Does NOT use spring-boot-starter-oauth2-client                           │           │
│  │  • Implements the flow manually with RestClient:                            │           │
│  │    1. POST /device_authorization → get codes                                │           │
│  │    2. Display user_code + URL to user                                       │           │
│  │    3. Poll /token every N seconds                                           │           │
│  │    4. Use access_token for API calls                                        │           │
│  │                                                                               │           │
│  │  ── RESOURCE SERVER ──────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • ZERO changes from section 20.14!                                          │           │
│  │  • JWT validation is the same regardless of grant type                      │           │
│  │                                                                               │           │
│  │  ── POLLING RESPONSES ────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • "authorization_pending" → keep polling                                   │           │
│  │  • "slow_down" → increase interval by 5 seconds                            │           │
│  │  • "expired_token" → start over                                             │           │
│  │  • "access_denied" → user denied                                            │           │
│  │  • 200 OK → tokens received! stop polling                                   │           │
│  │                                                                               │           │
│  │  ── KEY CLASSES (Spring Authorization Server) ────────────────────           │           │
│  │                                                                               │           │
│  │  • OAuth2DeviceAuthorizationEndpointFilter                                  │           │
│  │    → Handles POST /device_authorization                                     │           │
│  │  • OAuth2DeviceVerificationEndpointFilter                                   │           │
│  │    → Handles GET/POST /device_verification                                  │           │
│  │  • OAuth2DeviceCodeAuthenticationProvider                                    │           │
│  │    → Processes device_code grant at /token endpoint                         │           │
│  │  • OAuth2AuthorizationServerConfigurer                                       │           │
│  │    → .deviceAuthorizationEndpoint() to enable device code                   │           │
│  │    → .deviceVerificationEndpoint() to enable user verification             │           │
│  │                                                                               │           │
│  │  ── FLOW IN ONE LINE ─────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Device → POST /device_authorization → shows code → User opens URL         │           │
│  │  on phone → enters code → logs in → approves → Device polls /token         │           │
│  │  → gets tokens → calls Resource Server                                      │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.17 ★ PKCE — Proof Key for Code Exchange (RFC 7636)

---

##### 20.17.1 ★ What is PKCE and Why Does It Exist?

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ PKCE (Proof Key for Code Exchange) — "pixy"                                              │
│    RFC 7636 — Extension to the Authorization Code Flow                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── THE PROBLEM ──────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  In the standard Authorization Code flow:                                    │           │
│  │                                                                               │           │
│  │    1. Client redirects user to auth server                                   │           │
│  │    2. User logs in and approves                                              │           │
│  │    3. Auth server redirects back with an AUTHORIZATION CODE in the URL:     │           │
│  │       http://localhost:8080/callback?code=abc123                             │           │
│  │    4. Client exchanges the code + client_secret for tokens                  │           │
│  │                                                                               │           │
│  │  ★ VULNERABILITY: The authorization code travels through the browser        │           │
│  │    redirect (front-channel). It can be INTERCEPTED by:                      │           │
│  │                                                                               │           │
│  │    • Malicious apps on mobile devices that register the same custom         │           │
│  │      URL scheme (e.g., myapp://callback)                                    │           │
│  │    • Browser extensions that read URLs                                       │           │
│  │    • Network proxies / logs that capture redirect URLs                      │           │
│  │    • Shared devices where browser history is accessible                     │           │
│  │                                                                               │           │
│  │  For CONFIDENTIAL clients (server-side apps):                                │           │
│  │    → client_secret protects the token exchange step                         │           │
│  │    → Even if code is intercepted, attacker can't exchange it                │           │
│  │      without the secret                                                      │           │
│  │                                                                               │           │
│  │  For PUBLIC clients (SPAs, mobile apps, desktop apps):                       │           │
│  │    → NO client_secret! (can't be stored securely)                           │           │
│  │    → If code is intercepted → attacker CAN exchange it for tokens!         │           │
│  │    → ★★★ THIS IS THE AUTHORIZATION CODE INTERCEPTION ATTACK ★★★           │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── THE SOLUTION: PKCE ───────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  PKCE adds a DYNAMIC, ONE-TIME SECRET to each authorization request.        │           │
│  │  Instead of a static client_secret, the client generates a fresh random     │           │
│  │  value for EVERY authorization request.                                      │           │
│  │                                                                               │           │
│  │  • The client proves it is the SAME client that started the flow            │           │
│  │  • Even if the authorization code is intercepted, the attacker can't        │           │
│  │    exchange it because they don't have the original random value            │           │
│  │  • No shared secret needed — works perfectly for public clients             │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.17.2 ★ PKCE Terminology — code_verifier and code_challenge

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ PKCE KEY CONCEPTS — TWO VALUES THAT MAKE IT WORK                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌───────────────────┬────────────────────────────────────────────────────┐ │           │
│  │  │  Term              │  Description                                       │ │           │
│  │  ├───────────────────┼────────────────────────────────────────────────────┤ │           │
│  │  │                    │                                                    │ │           │
│  │  │  code_verifier     │  A HIGH-ENTROPY cryptographic random string       │ │           │
│  │  │                    │  generated by the CLIENT.                          │ │           │
│  │  │                    │  • 43-128 characters long                         │ │           │
│  │  │                    │  • Characters: [A-Z] [a-z] [0-9] - . _ ~         │ │           │
│  │  │                    │  • Generated fresh for EVERY auth request         │ │           │
│  │  │                    │  • Stored locally by the client (never sent       │ │           │
│  │  │                    │    through the browser redirect)                   │ │           │
│  │  │                    │  • Example: "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW..."│ │           │
│  │  │                    │                                                    │ │           │
│  │  ├───────────────────┼────────────────────────────────────────────────────┤ │           │
│  │  │                    │                                                    │ │           │
│  │  │  code_challenge    │  A TRANSFORMED version of the code_verifier.      │ │           │
│  │  │                    │  Sent in the AUTHORIZATION REQUEST (front-channel).│ │           │
│  │  │                    │                                                    │ │           │
│  │  │                    │  Transformation method (code_challenge_method):    │ │           │
│  │  │                    │                                                    │ │           │
│  │  │                    │  • S256 (RECOMMENDED):                            │ │           │
│  │  │                    │    code_challenge = BASE64URL(SHA256(code_verifier))│ │          │
│  │  │                    │    → One-way hash: can't reverse to get verifier  │ │           │
│  │  │                    │                                                    │ │           │
│  │  │                    │  • plain (NOT recommended):                       │ │           │
│  │  │                    │    code_challenge = code_verifier (no transform)  │ │           │
│  │  │                    │    → Only use if client can't do SHA-256          │ │           │
│  │  │                    │                                                    │ │           │
│  │  ├───────────────────┼────────────────────────────────────────────────────┤ │           │
│  │  │                    │                                                    │ │           │
│  │  │  code_challenge    │  The method used to transform code_verifier      │ │           │
│  │  │  _method           │  into code_challenge.                             │ │           │
│  │  │                    │  • "S256" → SHA-256 hash + Base64URL encoding    │ │           │
│  │  │                    │  • "plain" → no transformation                   │ │           │
│  │  │                    │                                                    │ │           │
│  │  └───────────────────┴────────────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── HOW THE TWO VALUES RELATE ────────────────────────────────────           │           │
│  │                                                                               │           │
│  │                    code_verifier                                              │           │
│  │                    (random string, 43-128 chars)                             │           │
│  │                         │                                                     │           │
│  │                         │  SHA-256 hash                                       │           │
│  │                         ▼                                                     │           │
│  │                    SHA-256 digest                                             │           │
│  │                    (32 bytes)                                                 │           │
│  │                         │                                                     │           │
│  │                         │  Base64URL encode                                   │           │
│  │                         ▼                                                     │           │
│  │                    code_challenge                                             │           │
│  │                    (43 chars, URL-safe)                                       │           │
│  │                                                                               │           │
│  │  ★ KEY INSIGHT: You CANNOT reverse code_challenge back to code_verifier     │           │
│  │    (SHA-256 is a one-way hash function). This is what makes PKCE secure.    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.17.3 ★ PKCE Flow — Complete Step-by-Step Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ PKCE FLOW — AUTHORIZATION CODE + PKCE (Step-by-Step)                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│     Client (SPA/Mobile)   Authorization Server    User (Browser)                            │
│     ───────────────────   ────────────────────    ──────────────                            │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── STEP 1: Client generates code_verifier and code_challenge ──                      │
│     │                        │                        │                                      │
│     │  ┌─────────────────────────────────────────────────────┐                              │
│     │  │  CLIENT (locally, before any network calls):        │                              │
│     │  │                                                      │                              │
│     │  │  1. Generate code_verifier:                         │                              │
│     │  │     code_verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r │                              │
│     │  │                      _wW1gFWFOEjXk..."              │                              │
│     │  │     (43-128 random chars from [A-Za-z0-9-._~])     │                              │
│     │  │                                                      │                              │
│     │  │  2. Compute code_challenge:                         │                              │
│     │  │     code_challenge = BASE64URL(SHA256(code_verifier))│                              │
│     │  │                    = "E9Melhoa2OwvFrEMTJguCHaoeK1t8U │                              │
│     │  │                       RWbuGJSstw-cM"                │                              │
│     │  │                                                      │                              │
│     │  │  3. Store code_verifier in memory / session storage │                              │
│     │  │     (NEVER send it through the browser!)            │                              │
│     │  └─────────────────────────────────────────────────────┘                              │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── STEP 2: Authorization Request (with code_challenge) ──                            │
│     │                        │                        │                                      │
│     │  302 Redirect → Auth Server                    │                                      │
│     │  GET /oauth2/authorize?                        │                                      │
│     │    response_type=code                          │                                      │
│     │    &client_id=my-app                           │                                      │
│     │    &redirect_uri=http://localhost:8080/callback │                                      │
│     │    &scope=openid+profile                       │                                      │
│     │    &state=xyz123                               │                                      │
│     │    &code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM                      │
│     │    &code_challenge_method=S256    ★★★ PKCE PARAMS ★★★                               │
│     │───────────────────────>│                        │                                      │
│     │                        │                        │                                      │
│     │                        │  Auth Server:          │                                      │
│     │                        │  → Stores code_challenge                                     │
│     │                        │    and code_challenge_method                                  │
│     │                        │    (associated with this                                      │
│     │                        │     authorization session)                                    │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── STEP 3: User logs in and approves (same as normal Auth Code flow) ──              │
│     │                        │                        │                                      │
│     │                        │  Login page            │                                      │
│     │                        │───────────────────────>│                                      │
│     │                        │                        │  User enters                         │
│     │                        │                        │  username/password                    │
│     │                        │  POST /login           │                                      │
│     │                        │<───────────────────────│                                      │
│     │                        │                        │                                      │
│     │                        │  Consent page          │                                      │
│     │                        │───────────────────────>│                                      │
│     │                        │                        │  User approves                       │
│     │                        │  POST /approve         │                                      │
│     │                        │<───────────────────────│                                      │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── STEP 4: Auth server redirects back with authorization code ──                     │
│     │                        │                        │                                      │
│     │  302 Redirect          │                        │                                      │
│     │  Location: http://localhost:8080/callback       │                                      │
│     │    ?code=SplxlOBeZQQYbYS6WxSbIA                │                                      │
│     │    &state=xyz123       │                        │                                      │
│     │<───────────────────────│                        │                                      │
│     │                        │                        │                                      │
│     │  ★ The authorization code travels through the browser (front-channel)                │
│     │  ★ An attacker COULD intercept this code!                                            │
│     │  ★ But they DON'T have the code_verifier...                                         │
│     │                        │                        │                                      │
│     │                        │                        │                                      │
│     │  ── STEP 5: Token Exchange (with code_verifier) ★ THE KEY STEP ★ ──                  │
│     │                        │                        │                                      │
│     │  POST /oauth2/token    │                        │                                      │
│     │  {                     │                        │                                      │
│     │    grant_type=authorization_code                │                                      │
│     │    code=SplxlOBeZQQYbYS6WxSbIA                 │                                      │
│     │    redirect_uri=http://localhost:8080/callback  │                                      │
│     │    client_id=my-app    │                        │                                      │
│     │    code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk  ★★★ PKCE ★★★        │
│     │  }                     │                        │                                      │
│     │───────────────────────>│                        │                                      │
│     │                        │                        │                                      │
│     │                  Auth Server:                   │                                      │
│     │                  ┌──────────────────────────────────────────────┐                     │
│     │                  │  PKCE VERIFICATION:                          │                     │
│     │                  │                                               │                     │
│     │                  │  1. Retrieve stored code_challenge from      │                     │
│     │                  │     the authorization session                │                     │
│     │                  │                                               │                     │
│     │                  │  2. Compute:                                  │                     │
│     │                  │     expected = BASE64URL(SHA256(code_verifier))│                    │
│     │                  │                                               │                     │
│     │                  │  3. Compare:                                  │                     │
│     │                  │     expected == stored code_challenge ?       │                     │
│     │                  │                                               │                     │
│     │                  │     ✅ MATCH → Client is legitimate!          │                     │
│     │                  │        Issue tokens.                          │                     │
│     │                  │                                               │                     │
│     │                  │     ❌ MISMATCH → Reject request!             │                     │
│     │                  │        Client is NOT the one that started    │                     │
│     │                  │        the flow. Possible interception.      │                     │
│     │                  └──────────────────────────────────────────────┘                     │
│     │                        │                        │                                      │
│     │  200 OK                │                        │                                      │
│     │  {                     │                        │                                      │
│     │    "access_token": "eyJ...",                   │                                      │
│     │    "id_token": "eyJ...",                       │                                      │
│     │    "refresh_token": "...",                     │                                      │
│     │    "token_type": "Bearer",                    │                                      │
│     │    "expires_in": 3600  │                        │                                      │
│     │  }                     │                        │                                      │
│     │<───────────────────────│                        │                                      │
│     │                        │                        │                                      │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.17.4 ★ Why the Attacker Fails — PKCE Security Explained

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHY THE ATTACKER FAILS — PKCE PREVENTS AUTHORIZATION CODE INTERCEPTION                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── WITHOUT PKCE (Vulnerable) ────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Legitimate Client                Auth Server         Attacker               │           │
│  │  ─────────────────                ───────────         ────────               │           │
│  │  │                                   │                   │                    │           │
│  │  │  GET /authorize?                  │                   │                    │           │
│  │  │    code=abc123                    │                   │                    │           │
│  │  │<──────────────────────────────────│                   │                    │           │
│  │  │                                   │                   │                    │           │
│  │  │  ★ Attacker intercepts code=abc123 via:             │                    │           │
│  │  │    • Malicious app on same device                    │                    │           │
│  │  │    • Browser extension                               │                    │           │
│  │  │    • URL scheme hijacking (mobile)                   │                    │           │
│  │  │                                   │    ┌─────────────┤                    │           │
│  │  │                                   │    │ code=abc123 │                    │           │
│  │  │                                   │    └─────────────┤                    │           │
│  │  │                                   │                   │                    │           │
│  │  │                                   │  POST /token      │                    │           │
│  │  │                                   │  code=abc123      │                    │           │
│  │  │                                   │  client_id=my-app │                    │           │
│  │  │                                   │<──────────────────│                    │           │
│  │  │                                   │                   │                    │           │
│  │  │                                   │  200 OK           │                    │           │
│  │  │                                   │  access_token=eyJ.│                    │           │
│  │  │                                   │──────────────────>│                    │           │
│  │  │                                   │                   │                    │           │
│  │  │                                   │         ★ ATTACKER HAS TOKENS! ★     │           │
│  │  │                                   │                   │                    │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── WITH PKCE (Protected) ────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Legitimate Client                Auth Server         Attacker               │           │
│  │  ─────────────────                ───────────         ────────               │           │
│  │  │                                   │                   │                    │           │
│  │  │  code_verifier = "dBjft..."       │                   │                    │           │
│  │  │  code_challenge = SHA256(above)   │                   │                    │           │
│  │  │                                   │                   │                    │           │
│  │  │  GET /authorize?                  │                   │                    │           │
│  │  │    code_challenge=E9Mel...        │                   │                    │           │
│  │  │──────────────────────────────────>│                   │                    │           │
│  │  │                                   │  Stores           │                    │           │
│  │  │                                   │  code_challenge   │                    │           │
│  │  │                                   │                   │                    │           │
│  │  │  Redirect: code=abc123            │                   │                    │           │
│  │  │<──────────────────────────────────│                   │                    │           │
│  │  │                                   │                   │                    │           │
│  │  │  ★ Attacker intercepts code=abc123                   │                    │           │
│  │  │                                   │    ┌─────────────┤                    │           │
│  │  │                                   │    │ code=abc123 │                    │           │
│  │  │                                   │    └─────────────┤                    │           │
│  │  │                                   │                   │                    │           │
│  │  │                                   │  POST /token      │                    │           │
│  │  │                                   │  code=abc123      │                    │           │
│  │  │                                   │  code_verifier=???│  ★ Attacker       │           │
│  │  │                                   │<──────────────────│  DOESN'T KNOW     │           │
│  │  │                                   │                   │  the verifier!    │           │
│  │  │                                   │                   │                    │           │
│  │  │                             Auth Server:              │                    │           │
│  │  │                             SHA256(???) ≠ E9Mel...    │                    │           │
│  │  │                             ❌ MISMATCH! REJECTED!    │                    │           │
│  │  │                                   │                   │                    │           │
│  │  │                                   │  400 Bad Request  │                    │           │
│  │  │                                   │  "invalid_grant"  │                    │           │
│  │  │                                   │──────────────────>│                    │           │
│  │  │                                   │                   │                    │           │
│  │  │                                   │        ★ ATTACKER BLOCKED! ★          │           │
│  │  │                                   │                   │                    │           │
│  │  │                                   │                   │                    │           │
│  │  │  ★ Meanwhile, legitimate client sends:               │                    │           │
│  │  │  POST /token                      │                   │                    │           │
│  │  │  code=abc123                      │                   │                    │           │
│  │  │  code_verifier=dBjft...  ★ KNOWS the real verifier   │                    │           │
│  │  │──────────────────────────────────>│                   │                    │           │
│  │  │                                   │                   │                    │           │
│  │  │                             Auth Server:              │                    │           │
│  │  │                             SHA256(dBjft...) == E9Mel.│                    │           │
│  │  │                             ✅ MATCH! Issue tokens.   │                    │           │
│  │  │                                   │                   │                    │           │
│  │  │  200 OK { access_token, ... }     │                   │                    │           │
│  │  │<──────────────────────────────────│                   │                    │           │
│  │  │                                   │                   │                    │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.17.5 ★ Authorization Code Flow — With vs Without PKCE (Side-by-Side)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SIDE-BY-SIDE — AUTHORIZATION CODE FLOW WITH AND WITHOUT PKCE                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Step    Without PKCE                    With PKCE                           │           │
│  │  ─────   ────────────────────────────    ─────────────────────────────────   │           │
│  │                                                                               │           │
│  │  1.      (nothing extra)                 Client generates:                   │           │
│  │                                            • code_verifier (random)          │           │
│  │                                            • code_challenge = SHA256(above)  │           │
│  │                                                                               │           │
│  │  2.      GET /authorize?                 GET /authorize?                     │           │
│  │            response_type=code              response_type=code               │           │
│  │            &client_id=app                  &client_id=app                   │           │
│  │            &redirect_uri=...               &redirect_uri=...               │           │
│  │            &scope=openid                   &scope=openid                    │           │
│  │            &state=xyz                      &state=xyz                       │           │
│  │                                          + &code_challenge=E9Mel...  ★★★   │           │
│  │                                          + &code_challenge_method=S256 ★★★ │           │
│  │                                                                               │           │
│  │  3.      User logs in & approves         User logs in & approves            │           │
│  │          (identical)                      (identical)                         │           │
│  │                                                                               │           │
│  │  4.      Redirect: ?code=abc123          Redirect: ?code=abc123             │           │
│  │          (identical)                      (identical)                         │           │
│  │                                                                               │           │
│  │  5.      POST /token                     POST /token                        │           │
│  │            grant_type=auth_code            grant_type=auth_code             │           │
│  │            &code=abc123                    &code=abc123                      │           │
│  │            &redirect_uri=...               &redirect_uri=...                │           │
│  │            &client_id=app                  &client_id=app                    │           │
│  │            &client_secret=secret           &code_verifier=dBjft...  ★★★     │           │
│  │            (confidential client)          (NO client_secret needed!)        │           │
│  │                                                                               │           │
│  │  6.      Auth server:                    Auth server:                        │           │
│  │          → Validates client_secret       → Computes SHA256(code_verifier)   │           │
│  │          → Issues tokens                 → Compares with stored challenge   │           │
│  │                                           → Match? → Issues tokens          │           │
│  │                                                                               │           │
│  │  ★ With PKCE, the code_verifier REPLACES the client_secret as the          │           │
│  │    proof of identity during token exchange.                                  │           │
│  │  ★ But even CONFIDENTIAL clients SHOULD use PKCE for defense in depth!     │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.17.6 ★ PKCE in Spring Security — Configuration

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ PKCE IN SPRING — BOTH CLIENT AND AUTHORIZATION SERVER                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── SPRING AUTHORIZATION SERVER (supports PKCE by default!) ──────           │           │
│  │                                                                               │           │
│  │  ★ Spring Authorization Server supports PKCE out of the box.                │           │
│  │    No special configuration needed on the server side.                       │           │
│  │    It automatically:                                                          │           │
│  │    → Accepts code_challenge and code_challenge_method in /authorize          │           │
│  │    → Stores the challenge with the authorization session                    │           │
│  │    → Validates code_verifier during token exchange                          │           │
│  │    → Supports both S256 and plain methods                                   │           │
│  │                                                                               │           │
│  │  To REQUIRE PKCE for a client, set:                                          │           │
│  │  clientSettings(ClientSettings.builder()                                     │           │
│  │      .requireProofKey(true)  ★★★                                            │           │
│  │      .build())                                                               │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── SPRING OAUTH2 CLIENT (sends PKCE automatically!) ─────────────           │           │
│  │                                                                               │           │
│  │  ★ Spring's OAuth2 Client (spring-boot-starter-oauth2-client)               │           │
│  │    automatically adds PKCE for PUBLIC clients.                              │           │
│  │    For confidential clients, it needs explicit configuration:               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**RegisteredClient with PKCE Required — Authorization Server**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  RegisteredClient — Requiring PKCE on Authorization Server
// ═══════════════════════════════════════════════════════════════════════════════

@Bean
public RegisteredClientRepository registeredClientRepository() {

    // ── PUBLIC Client (SPA / Mobile) — PKCE is MANDATORY ─────────────────────
    RegisteredClient publicClient = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("spa-app")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)  // ★ No secret!
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("http://localhost:3000/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)           // ★★★ PKCE REQUIRED
            .requireAuthorizationConsent(true)
            .build())
        .build();

    // ── CONFIDENTIAL Client — PKCE is OPTIONAL but RECOMMENDED ───────────────
    RegisteredClient confidentialClient = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("web-app")
        .clientSecret(passwordEncoder().encode("secret"))
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("http://localhost:8080/login/oauth2/code/web-app")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)           // ★ Even confidential clients SHOULD use PKCE
            .requireAuthorizationConsent(true)
            .build())
        .build();

    return new InMemoryRegisteredClientRepository(publicClient, confidentialClient);
}
```

**application.yml — Client Application Using PKCE**

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — OAuth2 Client with PKCE
#  ★ Spring Boot's OAuth2 Client handles PKCE automatically for public clients.
#  ★ For confidential clients, PKCE is also sent if the server supports it.
# ═══════════════════════════════════════════════════════════════════════════════

spring:
  security:
    oauth2:
      client:
        registration:
          my-app:
            client-id: spa-app
            # ★ No client-secret for public clients!
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid, profile
            client-authentication-method: none   # ★ This triggers PKCE automatically!
        provider:
          my-app:
            issuer-uri: http://localhost:9000
```

---

##### 20.17.7 ★ Advantages of PKCE

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ADVANTAGES OF PKCE                                                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. PREVENTS AUTHORIZATION CODE INTERCEPTION ATTACK                          │           │
│  │     • The primary purpose of PKCE                                            │           │
│  │     • Even if an attacker steals the authorization code from the redirect   │           │
│  │       URL, they CANNOT exchange it for tokens without the code_verifier     │           │
│  │     • The code_verifier never travels through the browser (back-channel)    │           │
│  │                                                                               │           │
│  │  2. ENABLES SECURE PUBLIC CLIENTS                                            │           │
│  │     • SPAs, mobile apps, and desktop apps can't store a client_secret       │           │
│  │     • PKCE provides equivalent security without needing a static secret     │           │
│  │     • Makes the Implicit flow OBSOLETE (OAuth 2.1 removes it entirely)     │           │
│  │                                                                               │           │
│  │  3. DYNAMIC PER-REQUEST SECRET                                               │           │
│  │     • Unlike client_secret which is the same for every request,             │           │
│  │       code_verifier is generated fresh for each authorization request       │           │
│  │     • Compromising one verifier doesn't help with future requests           │           │
│  │     • No long-lived secrets to protect                                       │           │
│  │                                                                               │           │
│  │  4. DEFENSE IN DEPTH (even for confidential clients)                         │           │
│  │     • Confidential clients already have client_secret, but PKCE adds       │           │
│  │       another layer of protection                                            │           │
│  │     • OAuth 2.1 (draft) REQUIRES PKCE for ALL clients, including           │           │
│  │       confidential ones                                                      │           │
│  │     • Protects against compromised TLS or leaked client credentials         │           │
│  │                                                                               │           │
│  │  5. NO SERVER-SIDE STATE CHANGE REQUIRED                                     │           │
│  │     • The auth server only needs to store the code_challenge temporarily   │           │
│  │       (already stores the authorization session anyway)                     │           │
│  │     • Minimal implementation overhead                                        │           │
│  │                                                                               │           │
│  │  6. PREVENTS CSRF-LIKE ATTACKS ON AUTHORIZATION ENDPOINT                    │           │
│  │     • Since the code_verifier is tied to the specific authorization         │           │
│  │       request, an attacker can't inject their own authorization code       │           │
│  │       into the client's callback                                             │           │
│  │     • Provides similar protection as the state parameter but stronger      │           │
│  │                                                                               │           │
│  │  7. SIMPLE TO IMPLEMENT                                                      │           │
│  │     • Just two extra parameters: code_challenge and code_verifier           │           │
│  │     • Standard SHA-256 + Base64URL — available in every platform            │           │
│  │     • Spring Security handles it automatically                              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.17.8 ★ Disadvantages of PKCE

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ DISADVANTAGES / LIMITATIONS OF PKCE                                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  1. DOES NOT PROTECT AGAINST ALL ATTACKS                                     │           │
│  │     • PKCE protects the authorization CODE, NOT the tokens                  │           │
│  │     • If an attacker can intercept the token response itself                │           │
│  │       (e.g., XSS on the client app), PKCE doesn't help                    │           │
│  │     • Doesn't protect against phishing (user entering credentials          │           │
│  │       on a fake login page)                                                  │           │
│  │     • Doesn't protect against compromised authorization server              │           │
│  │                                                                               │           │
│  │  2. ADDED COMPLEXITY (MINOR)                                                 │           │
│  │     • Client must generate cryptographically random values                  │           │
│  │     • Client must implement SHA-256 + Base64URL encoding                    │           │
│  │     • Client must store code_verifier temporarily (memory/session)          │           │
│  │     • More parameters in the authorization and token requests               │           │
│  │     • ★ Mitigated by: frameworks handle this automatically (Spring,         │           │
│  │       AppAuth, MSAL, etc.)                                                   │           │
│  │                                                                               │           │
│  │  3. "plain" METHOD WEAKNESS                                                  │           │
│  │     • If code_challenge_method=plain is used, code_challenge ==             │           │
│  │       code_verifier (no hashing)                                             │           │
│  │     • An attacker who intercepts the authorization REQUEST could            │           │
│  │       extract the code_challenge and use it as code_verifier                │           │
│  │     • ★ Solution: ALWAYS use S256, never plain                              │           │
│  │                                                                               │           │
│  │  4. DOESN'T ELIMINATE NEED FOR OTHER SECURITY MEASURES                      │           │
│  │     • Still need HTTPS for all communications                               │           │
│  │     • Still need proper redirect_uri validation                             │           │
│  │     • Still need state parameter for CSRF protection                        │           │
│  │     • Still need secure token storage on the client                         │           │
│  │     • PKCE is one layer in a defense-in-depth strategy                     │           │
│  │                                                                               │           │
│  │  5. BACKWARDS COMPATIBILITY                                                  │           │
│  │     • Older authorization servers may not support PKCE                      │           │
│  │     • Need to check server capabilities before using PKCE                   │           │
│  │     • ★ Less of an issue now — all modern auth servers support PKCE        │           │
│  │       (Google, Microsoft, Okta, Auth0, Keycloak, Spring Auth Server)       │           │
│  │                                                                               │           │
│  │  6. DOES NOT REPLACE CLIENT AUTHENTICATION FOR CONFIDENTIAL CLIENTS        │           │
│  │     • PKCE doesn't authenticate the CLIENT to the server                   │           │
│  │     • It only proves the token requester is the same entity that started   │           │
│  │       the authorization request                                              │           │
│  │     • Confidential clients should STILL use client_secret + PKCE together  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.17.9 ★ Real-World Use Cases — PKCE

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ REAL-WORLD USE CASES — WHERE PKCE IS USED                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── 1. SINGLE PAGE APPLICATIONS (SPAs) ───────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌──────────────────────┐     ┌───────────┐     ┌──────────────┐           │           │
│  │  │  React / Angular /   │     │ Auth      │     │ Resource     │           │           │
│  │  │  Vue.js App          │ ──> │ Server    │ ──> │ Server (API) │           │           │
│  │  │                      │     │ (Google,  │     │              │           │           │
│  │  │  ★ Runs entirely in │     │  Okta,    │     │              │           │           │
│  │  │    the browser       │     │  Auth0)   │     │              │           │           │
│  │  │  ★ Can't hide a     │     └───────────┘     └──────────────┘           │           │
│  │  │    client_secret     │                                                   │           │
│  │  │  ★ PKCE replaces    │                                                   │           │
│  │  │    the old Implicit  │                                                   │           │
│  │  │    flow              │                                                   │           │
│  │  └──────────────────────┘                                                   │           │
│  │                                                                               │           │
│  │  Example: Gmail web app, GitHub web UI, Spotify web player                  │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 2. MOBILE APPLICATIONS ───────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌──────────────────────┐     ┌───────────┐                                │           │
│  │  │  iOS / Android App   │     │ Auth      │                                │           │
│  │  │                      │ ──> │ Server    │                                │           │
│  │  │  ★ Uses AppAuth SDK │     │           │                                │           │
│  │  │  ★ Custom URL scheme│     └───────────┘                                │           │
│  │  │    (myapp://callback)│                                                   │           │
│  │  │  ★ Other apps could │                                                   │           │
│  │  │    register same     │                                                   │           │
│  │  │    URL scheme!       │                                                   │           │
│  │  │  ★ PKCE prevents    │                                                   │           │
│  │  │    code theft        │                                                   │           │
│  │  └──────────────────────┘                                                   │           │
│  │                                                                               │           │
│  │  Example: Banking apps, social media apps (Instagram, Twitter/X),           │           │
│  │  fitness apps (Strava), ride-sharing apps (Uber)                            │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 3. DESKTOP APPLICATIONS ──────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ┌──────────────────────┐     ┌───────────┐                                │           │
│  │  │  Electron / Native   │     │ Auth      │                                │           │
│  │  │  Desktop App         │ ──> │ Server    │                                │           │
│  │  │                      │     │           │                                │           │
│  │  │  ★ VS Code           │     └───────────┘                                │           │
│  │  │  ★ Slack Desktop    │                                                   │           │
│  │  │  ★ Spotify Desktop  │                                                   │           │
│  │  │  ★ Can't securely   │                                                   │           │
│  │  │    store secrets in  │                                                   │           │
│  │  │    distributed binary│                                                   │           │
│  │  └──────────────────────┘                                                   │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 4. CLI TOOLS ─────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  $ gh auth login                                                             │           │
│  │  → Opens browser for GitHub OAuth login                                     │           │
│  │  → Uses PKCE + Authorization Code flow                                      │           │
│  │  → Callback to localhost:PORT (ephemeral port)                              │           │
│  │                                                                               │           │
│  │  $ az login                                                                  │           │
│  │  → Opens browser for Azure AD login                                         │           │
│  │  → Uses PKCE + Authorization Code flow                                      │           │
│  │                                                                               │           │
│  │  $ gcloud auth login                                                         │           │
│  │  → Opens browser for Google login                                           │           │
│  │  → Uses PKCE + Authorization Code flow                                      │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 5. CONFIDENTIAL WEB SERVERS (defense in depth) ───────────────           │           │
│  │                                                                               │           │
│  │  ┌──────────────────────┐     ┌───────────┐                                │           │
│  │  │  Spring Boot /       │     │ Auth      │                                │           │
│  │  │  Node.js / Django    │ ──> │ Server    │                                │           │
│  │  │  Server-side App     │     │           │                                │           │
│  │  │                      │     └───────────┘                                │           │
│  │  │  ★ Has client_secret│                                                   │           │
│  │  │  ★ Uses PKCE anyway │                                                   │           │
│  │  │    for extra safety  │                                                   │           │
│  │  │  ★ OAuth 2.1 will   │                                                   │           │
│  │  │    REQUIRE this!     │                                                   │           │
│  │  └──────────────────────┘                                                   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.17.10 ★ PKCE in OAuth 2.0 vs OAuth 2.1

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ PKCE IN OAUTH 2.0 vs OAUTH 2.1                                                          │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────┬─────────────────────────────────────────┐ │           │
│  │  │  Aspect                      │  OAuth 2.0           │  OAuth 2.1       │ │           │
│  │  ├──────────────────────────────┼──────────────────────┼──────────────────┤ │           │
│  │  │                              │                      │                  │ │           │
│  │  │  PKCE for public clients     │  Recommended (RFC    │  REQUIRED ★      │ │           │
│  │  │                              │  7636, optional)     │                  │ │           │
│  │  │                              │                      │                  │ │           │
│  │  │  PKCE for confidential       │  Optional            │  REQUIRED ★      │ │           │
│  │  │  clients                     │                      │                  │ │           │
│  │  │                              │                      │                  │ │           │
│  │  │  Implicit flow               │  Allowed (but        │  REMOVED ★★★    │ │           │
│  │  │                              │  discouraged)        │                  │ │           │
│  │  │                              │                      │                  │ │           │
│  │  │  Password grant              │  Allowed             │  REMOVED ★★★    │ │           │
│  │  │                              │                      │                  │ │           │
│  │  │  code_challenge_method       │  S256 or plain       │  S256 only ★     │ │           │
│  │  │                              │                      │  (plain removed) │ │           │
│  │  │                              │                      │                  │ │           │
│  │  │  Recommended auth flow       │  Auth Code + PKCE    │  Auth Code +     │ │           │
│  │  │  for public clients          │  (best practice)     │  PKCE (ONLY way) │ │           │
│  │  │                              │                      │                  │ │           │
│  │  └──────────────────────────────┴──────────────────────┴──────────────────┘ │           │
│  │                                                                               │           │
│  │  ★ KEY TAKEAWAY: OAuth 2.1 makes PKCE mandatory for ALL clients.            │           │
│  │    Start using PKCE NOW — it will be required by the standard.              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.17.11 ★ Summary — PKCE (Proof Key for Code Exchange)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — PKCE (Proof Key for Code Exchange, RFC 7636)                                   │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── WHAT IS IT ───────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • Extension to Authorization Code flow that prevents code interception     │           │
│  │  • Client generates a random code_verifier and sends its SHA-256 hash      │           │
│  │    (code_challenge) in the authorization request                            │           │
│  │  • During token exchange, client sends the original code_verifier           │           │
│  │  • Auth server verifies SHA256(code_verifier) == stored code_challenge      │           │
│  │  • Proves the token requester is the same entity that started the flow     │           │
│  │                                                                               │           │
│  │  ── HOW IT WORKS (in one line) ───────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Client generates verifier → sends hash to /authorize → gets code →         │           │
│  │  sends original verifier to /token → server verifies hash matches →        │           │
│  │  issues tokens                                                               │           │
│  │                                                                               │           │
│  │  ── TWO KEY VALUES ───────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • code_verifier:  Random string (43-128 chars), kept secret by client     │           │
│  │  • code_challenge: BASE64URL(SHA256(code_verifier)), sent in /authorize    │           │
│  │                                                                               │           │
│  │  ── ADVANTAGES ───────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ✅ Prevents authorization code interception attacks                         │           │
│  │  ✅ Enables secure public clients (SPAs, mobile, desktop)                   │           │
│  │  ✅ Dynamic per-request secret (no static secrets to leak)                  │           │
│  │  ✅ Defense in depth for confidential clients                               │           │
│  │  ✅ Simple to implement, framework-supported                               │           │
│  │  ✅ Makes the Implicit flow obsolete                                        │           │
│  │                                                                               │           │
│  │  ── DISADVANTAGES ────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  ❌ Doesn't protect tokens after issuance (XSS can still steal them)       │           │
│  │  ❌ "plain" method offers weak security (always use S256)                   │           │
│  │  ❌ Doesn't replace other security measures (HTTPS, state, etc.)           │           │
│  │  ❌ Doesn't authenticate the client (only proves same-origin request)      │           │
│  │                                                                               │           │
│  │  ── SPRING SECURITY ─────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • Auth Server: Supports PKCE by default; enforce with                     │           │
│  │    .clientSettings(ClientSettings.builder().requireProofKey(true))          │           │
│  │  • Client: Automatically adds PKCE when                                     │           │
│  │    client-authentication-method: none (public client)                       │           │
│  │                                                                               │           │
│  │  ── USE CASES ────────────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • SPAs (React, Angular, Vue)                                               │           │
│  │  • Mobile apps (iOS, Android — via AppAuth)                                 │           │
│  │  • Desktop apps (Electron, native)                                          │           │
│  │  • CLI tools (gh, az, gcloud)                                               │           │
│  │  • Server-side apps (defense in depth — OAuth 2.1 requires it!)            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 20.18 ★ Integrating PKCE with Spring Security — Complete Working Example

---

##### 20.18.1 ★ Architecture Overview — 3 Applications with PKCE

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ ARCHITECTURE — PKCE INTEGRATION WITH SPRING SECURITY (3 Applications)                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐            │
│  │                                                                              │            │
│  │  ┌───────────────────────────────────────────────────────────┐              │            │
│  │  │  1. AUTHORIZATION SERVER  (Port 9000)                      │              │            │
│  │  │     → Spring Authorization Server                          │              │            │
│  │  │     → Supports PKCE out of the box                         │              │            │
│  │  │     → RegisteredClient with requireProofKey(true)          │              │            │
│  │  │     → Validates code_challenge at /authorize               │              │            │
│  │  │     → Validates code_verifier at /token                    │              │            │
│  │  │     → Endpoints: /oauth2/authorize, /oauth2/token,         │              │            │
│  │  │       /oauth2/jwks, /.well-known/openid-configuration     │              │            │
│  │  └───────────────────────────────────────────────────────────┘              │            │
│  │                      ▲               ▲                                       │            │
│  │    1. GET /authorize │               │ 3. User logs in                      │            │
│  │    + code_challenge  │               │    & approves                         │            │
│  │    + code_challenge  │               │                                       │            │
│  │      _method=S256    │               │                                       │            │
│  │                      │               │                                       │            │
│  │    4. POST /token    │               │                                       │            │
│  │    + code_verifier   │               │                                       │            │
│  │                      │               │                                       │            │
│  │  ┌──────────────────┴───┐   ┌──────┴─────────────────────────┐             │            │
│  │  │  2. CLIENT APP         │   │  User's Browser                │             │            │
│  │  │  (Port 8080)           │   │                                 │             │            │
│  │  │                        │   │  → Redirected to auth server   │             │            │
│  │  │  Scenario A:           │   │  → Enters username/password   │             │            │
│  │  │  Server-side Client    │   │  → Approves scopes            │             │            │
│  │  │  (Spring MVC +         │   │  → Redirected back to client  │             │            │
│  │  │   OAuth2 Client)       │   │                                 │             │            │
│  │  │                        │   └─────────────────────────────────┘             │            │
│  │  │  Scenario B:           │                                                  │            │
│  │  │  Public Client         │                                                  │            │
│  │  │  (SPA / React /        │                                                  │            │
│  │  │   Mobile App)          │                                                  │            │
│  │  │                        │                                                  │            │
│  │  └──────────┬─────────────┘                                                  │            │
│  │             │                                                                │            │
│  │             │ 5. GET /api/data                                               │            │
│  │             │    Authorization: Bearer <token>                               │            │
│  │             ▼                                                                │            │
│  │  ┌───────────────────────────────────────────────────────────┐              │            │
│  │  │  3. RESOURCE SERVER  (Port 8090)                            │              │            │
│  │  │     → Validates JWT access token                           │              │            │
│  │  │     → ZERO PKCE awareness needed!                          │              │            │
│  │  │     → Token is the same regardless of PKCE                 │              │            │
│  │  └───────────────────────────────────────────────────────────┘              │            │
│  │                                                                              │            │
│  └─────────────────────────────────────────────────────────────────────────────┘            │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.18.2 ★ Complete PKCE Flow Inside Spring Security — Internal Filter Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ INTERNAL SPRING SECURITY FLOW — PKCE STEP BY STEP (What happens inside)                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│     Client App              Spring Auth Server Filters            Auth Server Internals     │
│     ──────────              ─────────────────────────            ──────────────────────     │
│     │                              │                                   │                    │
│     │                              │                                   │                    │
│     │  ── PHASE 1: Client starts OAuth2 login ───────────────────────                      │
│     │                              │                                   │                    │
│     │  User clicks "Login"        │                                   │                    │
│     │  → Spring's OAuth2          │                                   │                    │
│     │    AuthorizationRequest     │                                   │                    │
│     │    RequestRedirectFilter    │                                   │                    │
│     │    intercepts               │                                   │                    │
│     │                              │                                   │                    │
│     │  ┌────────────────────────────────────────────────┐             │                    │
│     │  │  OAuth2AuthorizationRequestRedirectFilter       │             │                    │
│     │  │                                                  │             │                    │
│     │  │  1. Detects: client-authentication-method=none  │             │                    │
│     │  │     OR requireProofKey=true for the client      │             │                    │
│     │  │                                                  │             │                    │
│     │  │  2. DefaultOAuth2AuthorizationRequestResolver:  │             │                    │
│     │  │     → Generates code_verifier (43-128 random)   │             │                    │
│     │  │     → Computes code_challenge = BASE64URL(      │             │                    │
│     │  │         SHA256(code_verifier))                   │             │                    │
│     │  │     → Stores code_verifier in the               │             │                    │
│     │  │       OAuth2AuthorizationRequest (session/state) │             │                    │
│     │  │                                                  │             │                    │
│     │  │  3. Builds redirect URL:                        │             │                    │
│     │  │     GET /oauth2/authorize?                      │             │                    │
│     │  │       response_type=code                        │             │                    │
│     │  │       &client_id=pkce-client                    │             │                    │
│     │  │       &redirect_uri=http://localhost:8080/...   │             │                    │
│     │  │       &scope=openid+profile                     │             │                    │
│     │  │       &state=abc123                             │             │                    │
│     │  │       &code_challenge=E9Melhoa2Owv...  ★★★     │             │                    │
│     │  │       &code_challenge_method=S256      ★★★     │             │                    │
│     │  │                                                  │             │                    │
│     │  │  4. 302 Redirect → Authorization Server         │             │                    │
│     │  └────────────────────────────────────────────────┘             │                    │
│     │                              │                                   │                    │
│     │  ────────────────────────────>                                   │                    │
│     │                              │                                   │                    │
│     │                              │                                   │                    │
│     │  ── PHASE 2: Auth Server processes /authorize ──────────────────                     │
│     │                              │                                   │                    │
│     │                    OAuth2AuthorizationEndpointFilter             │                    │
│     │                              │                                   │                    │
│     │                              │  1. OAuth2AuthorizationCodeRequestAuthentication       │
│     │                              │     AuthenticationConverter:                           │
│     │                              │     → Extracts code_challenge from query params       │
│     │                              │     → Extracts code_challenge_method (S256)            │
│     │                              │     → Validates client has requireProofKey=true        │
│     │                              │       and code_challenge IS present                    │
│     │                              │       (rejects if missing when required!)              │
│     │                              │                                   │                    │
│     │                              │  2. OAuth2AuthorizationCodeRequestAuthentication       │
│     │                              │     Provider:                                          │
│     │                              │     → Validates client_id, redirect_uri, scopes       │
│     │                              │     → Stores in OAuth2Authorization:                   │
│     │                              │       ┌──────────────────────────────────┐             │
│     │                              │       │  authorization.attributes = {    │             │
│     │                              │       │    "code_challenge": "E9Mel...", │             │
│     │                              │       │    "code_challenge_method":"S256"│             │
│     │                              │       │  }                               │             │
│     │                              │       └──────────────────────────────────┘             │
│     │                              │     → Redirects to /login (if not authenticated)      │
│     │                              │                                   │                    │
│     │                              │  3. User logs in (form login)     │                    │
│     │                              │  4. User approves consent         │                    │
│     │                              │                                   │                    │
│     │                              │  5. Generates authorization_code  │                    │
│     │                              │     → Associates code with the   │                    │
│     │                              │       stored code_challenge       │                    │
│     │                              │                                   │                    │
│     │  302 Redirect                │                                   │                    │
│     │  Location: /callback?code=SplxlO...&state=abc123                │                    │
│     │  <────────────────────────────                                   │                    │
│     │                              │                                   │                    │
│     │                              │                                   │                    │
│     │  ── PHASE 3: Client exchanges code for token (with code_verifier) ──                 │
│     │                              │                                   │                    │
│     │  ┌────────────────────────────────────────────────┐             │                    │
│     │  │  OAuth2LoginAuthenticationFilter                │             │                    │
│     │  │  (on the CLIENT application)                    │             │                    │
│     │  │                                                  │             │                    │
│     │  │  1. Receives callback with ?code=SplxlO...      │             │                    │
│     │  │                                                  │             │                    │
│     │  │  2. OAuth2AuthorizationCodeAuthenticationProvider│             │                    │
│     │  │     → Retrieves stored code_verifier from       │             │                    │
│     │  │       the OAuth2AuthorizationRequest             │             │                    │
│     │  │                                                  │             │                    │
│     │  │  3. Calls DefaultAuthorizationCodeTokenResponse │             │                    │
│     │  │     Client → builds token request:              │             │                    │
│     │  │                                                  │             │                    │
│     │  │     POST /oauth2/token                          │             │                    │
│     │  │     grant_type=authorization_code               │             │                    │
│     │  │     &code=SplxlO...                             │             │                    │
│     │  │     &redirect_uri=http://localhost:8080/...     │             │                    │
│     │  │     &client_id=pkce-client                      │             │                    │
│     │  │     &code_verifier=dBjftJeZ4CVP...  ★★★        │             │                    │
│     │  │                                                  │             │                    │
│     │  └────────────────────────────────────────────────┘             │                    │
│     │                              │                                   │                    │
│     │  ────────────────────────────>                                   │                    │
│     │                              │                                   │                    │
│     │                    OAuth2TokenEndpointFilter                     │                    │
│     │                              │                                   │                    │
│     │                              │  1. OAuth2AuthorizationCodeAuthentication              │
│     │                              │     Provider:                                          │
│     │                              │                                   │                    │
│     │                              │  ┌───────────────────────────────────────────┐        │
│     │                              │  │  ★★★ PKCE VERIFICATION ★★★                │        │
│     │                              │  │                                            │        │
│     │                              │  │  a. Retrieve stored code_challenge from   │        │
│     │                              │  │     OAuth2Authorization.attributes         │        │
│     │                              │  │                                            │        │
│     │                              │  │  b. Retrieve code_verifier from request   │        │
│     │                              │  │                                            │        │
│     │                              │  │  c. Compute:                               │        │
│     │                              │  │     computed = BASE64URL(SHA256(            │        │
│     │                              │  │                   code_verifier))           │        │
│     │                              │  │                                            │        │
│     │                              │  │  d. Compare:                               │        │
│     │                              │  │     computed == stored code_challenge ?    │        │
│     │                              │  │                                            │        │
│     │                              │  │  ✅ YES → Continue to issue tokens         │        │
│     │                              │  │  ❌ NO  → 400 { "error":"invalid_grant" } │        │
│     │                              │  └───────────────────────────────────────────┘        │
│     │                              │                                   │                    │
│     │                              │  2. If PKCE valid:                │                    │
│     │                              │     → JwtGenerator signs tokens   │                    │
│     │                              │     → Returns access_token,       │                    │
│     │                              │       id_token, refresh_token     │                    │
│     │                              │                                   │                    │
│     │  200 OK { access_token, id_token, refresh_token }               │                    │
│     │  <────────────────────────────                                   │                    │
│     │                              │                                   │                    │
│     │                              │                                   │                    │
│     │  ── PHASE 4: Client calls Resource Server ──────────────────────                     │
│     │                              │                                   │                    │
│     │  GET /api/users                                                  │                    │
│     │  Authorization: Bearer <access_token>                            │                    │
│     │  ──────────────────────────────────────> Resource Server (8090)  │                    │
│     │                                          → Validates JWT         │                    │
│     │                                          → ✅ Authorized          │                    │
│     │  200 OK { user data }                                            │                    │
│     │  <────────────────────────────────────── Resource Server          │                    │
│     │                              │                                   │                    │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.18.3 ★ Key Spring Security Classes Involved in PKCE

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ KEY CLASSES — PKCE PROCESSING IN SPRING SECURITY                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── CLIENT-SIDE (spring-boot-starter-oauth2-client) ──────────────           │           │
│  │                                                                               │           │
│  │  Class                                    Role in PKCE                       │           │
│  │  ─────────────────────────────────────    ──────────────────────────────     │           │
│  │                                                                               │           │
│  │  OAuth2AuthorizationRequestRedirectFilter Intercepts /oauth2/authorization  │           │
│  │                                            request, delegates to resolver   │           │
│  │                                                                               │           │
│  │  DefaultOAuth2AuthorizationRequestResolver Generates code_verifier and      │           │
│  │                                            code_challenge, adds them to     │           │
│  │                                            the authorization URL.           │           │
│  │                                            ★ Automatically detects when     │           │
│  │                                            PKCE is needed (public client    │           │
│  │                                            or server requires it)           │           │
│  │                                                                               │           │
│  │  OAuth2AuthorizationRequest               Stores code_verifier in its       │           │
│  │                                            "additionalParameters" map.      │           │
│  │                                            Serialized to session/state.     │           │
│  │                                                                               │           │
│  │  OAuth2LoginAuthenticationFilter           Processes the callback, retrieves │           │
│  │                                            code_verifier from stored request│           │
│  │                                                                               │           │
│  │  DefaultAuthorizationCodeTokenResponse     Builds token request with        │           │
│  │  Client                                    code_verifier parameter          │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── SERVER-SIDE (spring-security-oauth2-authorization-server) ────           │           │
│  │                                                                               │           │
│  │  Class                                    Role in PKCE                       │           │
│  │  ─────────────────────────────────────    ──────────────────────────────     │           │
│  │                                                                               │           │
│  │  OAuth2AuthorizationEndpointFilter         Receives /authorize with         │           │
│  │                                            code_challenge and               │           │
│  │                                            code_challenge_method            │           │
│  │                                                                               │           │
│  │  OAuth2AuthorizationCodeRequestAuthentication Stores code_challenge in      │           │
│  │  Provider                                  OAuth2Authorization.attributes   │           │
│  │                                                                               │           │
│  │  OAuth2TokenEndpointFilter                 Receives /token with             │           │
│  │                                            code_verifier                    │           │
│  │                                                                               │           │
│  │  OAuth2AuthorizationCodeAuthenticationProvider                              │           │
│  │                                            ★★★ THE PKCE VALIDATOR ★★★      │           │
│  │                                            → Retrieves stored               │           │
│  │                                              code_challenge                 │           │
│  │                                            → Computes SHA256(code_verifier) │           │
│  │                                            → Compares with stored challenge │           │
│  │                                            → Rejects if mismatch           │           │
│  │                                                                               │           │
│  │  RegisteredClient.ClientSettings           .requireProofKey(true) to       │           │
│  │                                            enforce PKCE for a client        │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.18.4 ★ Application 1 — Authorization Server with PKCE (Port 9000)

**pom.xml — Authorization Server**

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Authorization Server with PKCE Support                              -->
<!--  ★ Same dependencies as section 20.14 — PKCE is built-in!                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- ── Spring Authorization Server ──────────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
    </dependency>

    <!-- ── Web (for serving login/consent pages) ────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

</dependencies>
```

**application.yml — Authorization Server**

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — Authorization Server (Port 9000)
# ═══════════════════════════════════════════════════════════════════════════════

server:
  port: 9000
```

**AuthorizationServerConfig.java — Full Configuration with PKCE**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  AuthorizationServerConfig.java — Authorization Server with PKCE
//
//  ★ PKCE is supported by default in Spring Authorization Server.
//  ★ We just need to configure RegisteredClients with requireProofKey(true)
//     to ENFORCE PKCE (reject requests without code_challenge).
//  ★ If requireProofKey(false) (default), PKCE is optional —
//     the server accepts code_challenge if sent but doesn't require it.
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    // ── 1. Authorization Server Security Filter Chain ─────────────────────────
    //    ★ Standard config — NO special PKCE configuration needed here!
    //    ★ PKCE support is built into the authorization and token endpoints.
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());    // Enable OpenID Connect 1.0

        http.exceptionHandling(exceptions -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
            )
        );

        return http.build();
    }

    // ── 2. Default Security Filter Chain (form login for user authentication) ─
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }

    // ── 3. RegisteredClient Repository ────────────────────────────────────────
    //    ★★★ THIS IS WHERE PKCE IS CONFIGURED ★★★
    //    ★ requireProofKey(true) enforces PKCE for the client
    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        // ── PUBLIC Client (SPA / Mobile) — PKCE REQUIRED, NO client_secret ──
        RegisteredClient publicClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("pkce-public-client")
            // ★ NO .clientSecret() — public client!
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)  // ★ Public client
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/login/oauth2/code/pkce-public-client")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope("read")
            .clientSettings(ClientSettings.builder()
                .requireProofKey(true)              // ★★★ PKCE IS MANDATORY
                .requireAuthorizationConsent(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .build())
            .build();

        // ── CONFIDENTIAL Client — PKCE REQUIRED + client_secret (defense in depth) ──
        RegisteredClient confidentialClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("pkce-confidential-client")
            .clientSecret(passwordEncoder().encode("secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/login/oauth2/code/pkce-confidential-client")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope("read")
            .clientSettings(ClientSettings.builder()
                .requireProofKey(true)              // ★★★ PKCE ENFORCED even for confidential!
                .requireAuthorizationConsent(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .build())
            .build();

        return new InMemoryRegisteredClientRepository(publicClient, confidentialClient);
    }

    // ── 4. JWK Source (RSA key pair for signing JWTs) ─────────────────────────
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    // ── 5. JWT Decoder ────────────────────────────────────────────────────────
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    // ── 6. Authorization Server Settings ──────────────────────────────────────
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    // ── 7. User Details Service (in-memory users for login) ───────────────────
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

    // ── 8. Password Encoder ──────────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Required Imports — Authorization Server**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Imports for AuthorizationServerConfig.java
// ═══════════════════════════════════════════════════════════════════════════════

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;
```

---

##### 20.18.5 ★ Application 2 — Client Application with PKCE (Port 8080)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ CLIENT APPLICATION — TWO SCENARIOS                                                       │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  Scenario A: CONFIDENTIAL CLIENT with PKCE (server-side Spring Boot app)    │           │
│  │  → Has client_secret + PKCE → Defense in depth                              │           │
│  │  → Spring adds PKCE automatically when server requires it                   │           │
│  │                                                                               │           │
│  │  Scenario B: PUBLIC CLIENT with PKCE (SPA acting through Spring backend)    │           │
│  │  → No client_secret, PKCE only                                              │           │
│  │  → client-authentication-method: none triggers automatic PKCE              │           │
│  │                                                                               │           │
│  │  ★ Both scenarios shown below in the same application.yml                   │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**pom.xml — Client Application**

```xml
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->
<!--  pom.xml — Client Application with PKCE                                       -->
<!-- ═══════════════════════════════════════════════════════════════════════════════ -->

<dependencies>

    <!-- ── OAuth2 Client (handles PKCE automatically!) ──────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>

    <!-- ── Web ──────────────────────────────────────────────────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- ── WebFlux (for WebClient to call resource server) ──────────────────── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

</dependencies>
```

**application.yml — Client Application (Both Public and Confidential Scenarios)**

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — Client Application with PKCE (Port 8080)
#
#  ★ TWO client registrations shown:
#    1. pkce-public-client     → Public client, PKCE is automatic
#    2. pkce-confidential-client → Confidential client, PKCE + client_secret
# ═══════════════════════════════════════════════════════════════════════════════

server:
  port: 8080

spring:
  security:
    oauth2:
      client:
        registration:

          # ── Scenario A: PUBLIC Client — PKCE automatic! ──────────────────
          pkce-public-client:
            client-id: pkce-public-client
            # ★ NO client-secret! (public client)
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid, profile, read
            client-authentication-method: none   # ★★★ This triggers PKCE!
            #
            # ★ When client-authentication-method=none:
            #   Spring's DefaultOAuth2AuthorizationRequestResolver
            #   AUTOMATICALLY:
            #     1. Generates code_verifier
            #     2. Computes code_challenge = BASE64URL(SHA256(code_verifier))
            #     3. Adds code_challenge + code_challenge_method=S256
            #        to the authorization URL
            #     4. Stores code_verifier in session
            #     5. Sends code_verifier during token exchange

          # ── Scenario B: CONFIDENTIAL Client — PKCE + client_secret ───────
          pkce-confidential-client:
            client-id: pkce-confidential-client
            client-secret: secret
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid, profile, read
            client-authentication-method: client_secret_basic
            #
            # ★ For confidential clients, PKCE is NOT automatically added
            #   by default. The server's requireProofKey(true) setting
            #   tells Spring to add PKCE.
            #
            # ★ Spring 6.x+ automatically sends PKCE when the server's
            #   .well-known/openid-configuration advertises it OR
            #   when the auth server rejects the first request.

        provider:
          pkce-public-client:
            issuer-uri: http://localhost:9000
          pkce-confidential-client:
            issuer-uri: http://localhost:9000
```

**SecurityConfig.java — Client Application**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  SecurityConfig.java — Client Application Security Config
//
//  ★ Standard OAuth2 login config.
//  ★ PKCE is handled AUTOMATICALLY by Spring — no custom code needed!
//  ★ For confidential clients that need PKCE, we customize the
//     OAuth2AuthorizationRequestResolver.
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        // ── Create a custom request resolver that enables PKCE for ALL clients ──
        DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
            );

        // ★★★ KEY LINE: Force PKCE for ALL clients (including confidential ones)
        // This customizer adds code_challenge and code_challenge_method
        // to every authorization request, regardless of client type.
        authorizationRequestResolver.setAuthorizationRequestCustomizer(
            OAuth2AuthorizationRequestCustomizers.withPkce()  // ★★★ ENABLE PKCE!
        );

        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/public").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(authorizationRequestResolver)  // ★★★ Use PKCE resolver
                )
            )
            .oauth2Client(Customizer.withDefaults());

        return http.build();
    }
}
```

**Required Imports — Client SecurityConfig**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Imports for SecurityConfig.java (Client Application)
// ═══════════════════════════════════════════════════════════════════════════════

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.SecurityFilterChain;
```

**ClientController.java — Controller that Calls Resource Server**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ClientController.java — Calls Resource Server with the access token
//  ★ The access token was obtained via PKCE flow — but you use it the same way!
//  ★ There is NO difference in how you use the token after PKCE.
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
public class ClientController {

    private final WebClient webClient;

    public ClientController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // ── Home page (public) ────────────────────────────────────────────────────
    @GetMapping("/")
    public String home() {
        return "Welcome! <a href='/dashboard'>Login with PKCE</a>";
    }

    // ── Dashboard (protected — requires OAuth2 login) ─────────────────────────
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(
            @RegisteredOAuth2AuthorizedClient("pkce-public-client")
            OAuth2AuthorizedClient authorizedClient,
            @AuthenticationPrincipal OidcUser oidcUser) {

        // ── Call the resource server using the access token ───────────────────
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        String resourceResponse = webClient
            .get()
            .uri("http://localhost:8090/api/users")
            .headers(headers -> headers.setBearerAuth(accessToken))
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return Map.of(
            "user", oidcUser.getFullName(),
            "email", oidcUser.getEmail() != null ? oidcUser.getEmail() : "N/A",
            "resourceServerResponse", resourceResponse != null ? resourceResponse : "No data",
            "accessToken", accessToken.substring(0, 20) + "...",  // First 20 chars only
            "tokenType", authorizedClient.getAccessToken().getTokenType().getValue()
        );
    }
}
```

**Required Imports — ClientController**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  Imports for ClientController.java
// ═══════════════════════════════════════════════════════════════════════════════

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
```

---

##### 20.18.6 ★ Application 3 — Resource Server (Port 8090) — ZERO PKCE Changes!

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ RESOURCE SERVER — NO PKCE AWARENESS NEEDED!                                              │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  The Resource Server is IDENTICAL to section 20.14.3.                        │           │
│  │  It doesn't know or care about PKCE!                                         │           │
│  │                                                                               │           │
│  │  ★ Why? Because PKCE only affects the authorization code exchange.          │           │
│  │    By the time the access token reaches the resource server, it's a         │           │
│  │    normal JWT — signed, validated, and used the same way.                   │           │
│  │                                                                               │           │
│  │  ★ The JWT looks the same whether PKCE was used or not:                     │           │
│  │  {                                                                            │           │
│  │    "iss": "http://localhost:9000",                                           │           │
│  │    "sub": "user",                                                            │           │
│  │    "aud": "pkce-public-client",                                              │           │
│  │    "scope": "openid profile read",                                          │           │
│  │    "iat": 1715184000,                                                        │           │
│  │    "exp": 1715187600                                                         │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

**application.yml — Resource Server**

```yaml
# ═══════════════════════════════════════════════════════════════════════════════
#  application.yml — Resource Server (Port 8090)
#  ★ IDENTICAL to section 20.14.3 — ZERO PKCE changes!
# ═══════════════════════════════════════════════════════════════════════════════

server:
  port: 8090

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
```

**ResourceServerConfig.java**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ResourceServerConfig.java — Resource Server (ZERO PKCE awareness!)
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/**").hasAuthority("SCOPE_read")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );
        return http.build();
    }
}
```

**ApiController.java — Resource Server**

```java
// ═══════════════════════════════════════════════════════════════════════════════
//  ApiController.java — Resource Server API
// ═══════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/users")
    public Map<String, Object> getUsers(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "Hello from Resource Server!",
            "authenticatedUser", jwt.getSubject(),
            "scopes", jwt.getClaimAsStringList("scope"),
            "tokenIssuedAt", jwt.getIssuedAt(),
            "tokenExpiresAt", jwt.getExpiresAt()
        );
    }
}
```

---

##### 20.18.7 ★ What Happens on the Wire — Raw HTTP with PKCE

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ RAW HTTP — WHAT SPRING SENDS OVER THE WIRE (WITH PKCE)                                  │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── 1. AUTHORIZATION REQUEST (Client → Auth Server) ─────────────            │           │
│  │                                                                               │           │
│  │  ★ Spring's DefaultOAuth2AuthorizationRequestResolver builds this URL:      │           │
│  │                                                                               │           │
│  │  GET /oauth2/authorize?                                                      │           │
│  │    response_type=code                                                        │           │
│  │    &client_id=pkce-public-client                                             │           │
│  │    &redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Flogin%2Foauth2%2F          │           │
│  │      code%2Fpkce-public-client                                               │           │
│  │    &scope=openid%20profile%20read                                            │           │
│  │    &state=abc123def456                                                       │           │
│  │    &code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM  ★★★        │           │
│  │    &code_challenge_method=S256                                    ★★★        │           │
│  │  HTTP/1.1                                                                    │           │
│  │  Host: localhost:9000                                                         │           │
│  │                                                                               │           │
│  │  ★ code_challenge and code_challenge_method are the ONLY additions!         │           │
│  │  ★ Everything else is identical to standard Authorization Code flow.        │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 2. AUTHORIZATION RESPONSE (Auth Server → Client) ────────────            │           │
│  │                                                                               │           │
│  │  HTTP/1.1 302 Found                                                          │           │
│  │  Location: http://localhost:8080/login/oauth2/code/pkce-public-client        │           │
│  │    ?code=SplxlOBeZQQYbYS6WxSbIA                                             │           │
│  │    &state=abc123def456                                                       │           │
│  │                                                                               │           │
│  │  ★ IDENTICAL to standard flow — no PKCE params in the redirect!             │           │
│  │  ★ The authorization code is still in the URL (vulnerable to interception)  │           │
│  │  ★ BUT the attacker can't use it without code_verifier!                    │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 3. TOKEN REQUEST (Client → Auth Server) ── PUBLIC CLIENT ─────           │           │
│  │                                                                               │           │
│  │  POST /oauth2/token HTTP/1.1                                                 │           │
│  │  Host: localhost:9000                                                         │           │
│  │  Content-Type: application/x-www-form-urlencoded                             │           │
│  │                                                                               │           │
│  │  grant_type=authorization_code                                               │           │
│  │  &code=SplxlOBeZQQYbYS6WxSbIA                                               │           │
│  │  &redirect_uri=http://localhost:8080/login/oauth2/code/pkce-public-client    │           │
│  │  &client_id=pkce-public-client                                               │           │
│  │  &code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk  ★★★          │           │
│  │                                                                               │           │
│  │  ★ NO client_secret (public client)                                         │           │
│  │  ★ code_verifier is sent instead — this is what the server validates        │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 4. TOKEN REQUEST — CONFIDENTIAL CLIENT (with PKCE + secret) ──           │           │
│  │                                                                               │           │
│  │  POST /oauth2/token HTTP/1.1                                                 │           │
│  │  Host: localhost:9000                                                         │           │
│  │  Content-Type: application/x-www-form-urlencoded                             │           │
│  │  Authorization: Basic cGtjZS1jb25maWRlbnRpYWwtY2xpZW50OnNlY3JldA==         │           │
│  │                 ↑ Base64(pkce-confidential-client:secret)                    │           │
│  │                                                                               │           │
│  │  grant_type=authorization_code                                               │           │
│  │  &code=SplxlOBeZQQYbYS6WxSbIA                                               │           │
│  │  &redirect_uri=http://localhost:8080/login/oauth2/code/pkce-confidential     │           │
│  │  &code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk  ★★★          │           │
│  │                                                                               │           │
│  │  ★ BOTH client_secret (in Authorization header) AND code_verifier!          │           │
│  │  ★ Double protection = defense in depth!                                    │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── 5. TOKEN RESPONSE (Auth Server → Client) ────────────────────            │           │
│  │                                                                               │           │
│  │  HTTP/1.1 200 OK                                                             │           │
│  │  Content-Type: application/json                                               │           │
│  │                                                                               │           │
│  │  {                                                                            │           │
│  │    "access_token": "eyJraWQiOiI3ZjM4YmQ0Ny...",                            │           │
│  │    "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2g...",                          │           │
│  │    "id_token": "eyJhbGciOiJSUzI1NiIs...",                                  │           │
│  │    "token_type": "Bearer",                                                   │           │
│  │    "expires_in": 3600,                                                        │           │
│  │    "scope": "openid profile read"                                            │           │
│  │  }                                                                            │           │
│  │                                                                               │           │
│  │  ★ IDENTICAL response whether PKCE was used or not!                         │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.18.8 ★ Testing with curl — Manual PKCE Flow

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ TESTING WITH curl — MANUAL PKCE FLOW                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── Step 1: Generate code_verifier and code_challenge ────────────           │           │
│  │                                                                               │           │
│  │  # Generate a random code_verifier (43-128 chars)                           │           │
│  │  $ CODE_VERIFIER=$(openssl rand -base64 32 | tr -d '=/+' | cut -c1-43)     │           │
│  │  $ echo "code_verifier: $CODE_VERIFIER"                                     │           │
│  │                                                                               │           │
│  │  # Compute code_challenge = BASE64URL(SHA256(code_verifier))                │           │
│  │  $ CODE_CHALLENGE=$(echo -n "$CODE_VERIFIER" | \                            │           │
│  │      openssl dgst -sha256 -binary | \                                       │           │
│  │      openssl base64 -A | \                                                   │           │
│  │      tr '+/' '-_' | tr -d '=')                                              │           │
│  │  $ echo "code_challenge: $CODE_CHALLENGE"                                   │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── Step 2: Open this URL in a browser (authorization request) ────          │           │
│  │                                                                               │           │
│  │  http://localhost:9000/oauth2/authorize?\                                    │           │
│  │    response_type=code\                                                       │           │
│  │    &client_id=pkce-public-client\                                            │           │
│  │    &redirect_uri=http://localhost:8080/login/oauth2/code/pkce-public-client\ │           │
│  │    &scope=openid+profile+read\                                               │           │
│  │    &code_challenge=$CODE_CHALLENGE\                                          │           │
│  │    &code_challenge_method=S256                                               │           │
│  │                                                                               │           │
│  │  → Log in as: user / password                                               │           │
│  │  → Approve the scopes                                                        │           │
│  │  → Copy the "code" from the redirect URL                                    │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── Step 3: Exchange code for tokens (with code_verifier) ────────           │           │
│  │                                                                               │           │
│  │  $ AUTH_CODE=<paste-code-from-redirect>                                     │           │
│  │                                                                               │           │
│  │  $ curl -X POST http://localhost:9000/oauth2/token \                         │           │
│  │      -d "grant_type=authorization_code" \                                    │           │
│  │      -d "code=$AUTH_CODE" \                                                  │           │
│  │      -d "redirect_uri=http://localhost:8080/login/oauth2/code/pkce-public-client" \     │
│  │      -d "client_id=pkce-public-client" \                                     │           │
│  │      -d "code_verifier=$CODE_VERIFIER"                                      │           │
│  │                                                                               │           │
│  │  ★ Response: { access_token, id_token, refresh_token, ... }                 │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── Step 4: Test with WRONG code_verifier (should FAIL!) ─────────           │           │
│  │                                                                               │           │
│  │  $ curl -X POST http://localhost:9000/oauth2/token \                         │           │
│  │      -d "grant_type=authorization_code" \                                    │           │
│  │      -d "code=$AUTH_CODE" \                                                  │           │
│  │      -d "redirect_uri=http://localhost:8080/login/oauth2/code/pkce-public-client" \     │
│  │      -d "client_id=pkce-public-client" \                                     │           │
│  │      -d "code_verifier=WRONG_VERIFIER_VALUE_HERE"                           │           │
│  │                                                                               │           │
│  │  ★ Response: { "error": "invalid_grant" }  ← ★ REJECTED!                  │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── Step 5: Test WITHOUT code_verifier (should FAIL!) ────────────           │           │
│  │                                                                               │           │
│  │  $ curl -X POST http://localhost:9000/oauth2/token \                         │           │
│  │      -d "grant_type=authorization_code" \                                    │           │
│  │      -d "code=$AUTH_CODE" \                                                  │           │
│  │      -d "redirect_uri=http://localhost:8080/login/oauth2/code/pkce-public-client" \     │
│  │      -d "client_id=pkce-public-client"                                      │           │
│  │                                                                               │           │
│  │  ★ Response: { "error": "invalid_grant" }  ← ★ REJECTED (no verifier)!    │           │
│  │                                                                               │           │
│  │                                                                               │           │
│  │  ── Step 6: Use the access token to call Resource Server ─────────           │           │
│  │                                                                               │           │
│  │  $ curl http://localhost:8090/api/users \                                    │           │
│  │      -H "Authorization: Bearer eyJraWQi..."                                 │           │
│  │                                                                               │           │
│  │  ★ Response: { "message": "Hello from Resource Server!", ... }              │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.18.9 ★ What YOU Write vs What Spring Does Automatically

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ WHAT YOU WRITE vs WHAT SPRING HANDLES AUTOMATICALLY FOR PKCE                            │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌──────────────────────────────────┬──────────────────────────────────────┐ │           │
│  │  │  YOU WRITE (configuration)        │  SPRING DOES (automatically)         │ │           │
│  │  ├──────────────────────────────────┼──────────────────────────────────────┤ │           │
│  │  │                                   │                                      │ │           │
│  │  │  ── AUTHORIZATION SERVER ──       │                                      │ │           │
│  │  │                                   │                                      │ │           │
│  │  │  RegisteredClient with:           │  • Accepts code_challenge in         │ │           │
│  │  │  .requireProofKey(true)           │    /authorize request               │ │           │
│  │  │                                   │  • Stores code_challenge with the   │ │           │
│  │  │  (ONE line of config!)            │    authorization session            │ │           │
│  │  │                                   │  • Validates code_verifier at       │ │           │
│  │  │                                   │    /token endpoint                  │ │           │
│  │  │                                   │  • SHA-256 computation              │ │           │
│  │  │                                   │  • Rejects mismatched verifiers    │ │           │
│  │  │                                   │  • Rejects missing code_challenge   │ │           │
│  │  │                                   │    when requireProofKey=true        │ │           │
│  │  │                                   │                                      │ │           │
│  │  ├──────────────────────────────────┼──────────────────────────────────────┤ │           │
│  │  │                                   │                                      │ │           │
│  │  │  ── CLIENT (PUBLIC) ──            │                                      │ │           │
│  │  │                                   │                                      │ │           │
│  │  │  application.yml:                 │  • Generates code_verifier           │ │           │
│  │  │  client-authentication-method:    │    (cryptographically random)        │ │           │
│  │  │    none                           │  • Computes SHA-256 code_challenge  │ │           │
│  │  │                                   │  • Adds to authorization URL        │ │           │
│  │  │  (ONE line of config!)            │  • Stores verifier in session       │ │           │
│  │  │                                   │  • Sends verifier during token      │ │           │
│  │  │                                   │    exchange                          │ │           │
│  │  │                                   │                                      │ │           │
│  │  ├──────────────────────────────────┼──────────────────────────────────────┤ │           │
│  │  │                                   │                                      │ │           │
│  │  │  ── CLIENT (CONFIDENTIAL) ──      │                                      │ │           │
│  │  │                                   │                                      │ │           │
│  │  │  SecurityConfig:                  │  Same as above, PLUS:               │ │           │
│  │  │  OAuth2AuthorizationRequest       │  • Sends client_secret in           │ │           │
│  │  │  Customizers.withPkce()           │    Authorization header             │ │           │
│  │  │                                   │  • Sends code_verifier in body     │ │           │
│  │  │  (ONE method call!)               │                                      │ │           │
│  │  │                                   │                                      │ │           │
│  │  ├──────────────────────────────────┼──────────────────────────────────────┤ │           │
│  │  │                                   │                                      │ │           │
│  │  │  ── RESOURCE SERVER ──            │                                      │ │           │
│  │  │                                   │                                      │ │           │
│  │  │  NOTHING! Zero PKCE config!       │  • Validates JWT as usual           │ │           │
│  │  │                                   │  • No PKCE awareness needed        │ │           │
│  │  │                                   │                                      │ │           │
│  │  └──────────────────────────────────┴──────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  │  ★ TOTAL PKCE CODE YOU WRITE: ~3 lines of configuration!                   │           │
│  │    1. requireProofKey(true) on the RegisteredClient                         │           │
│  │    2. client-authentication-method: none (for public clients)               │           │
│  │    3. OAuth2AuthorizationRequestCustomizers.withPkce() (for confidential)   │           │
│  │                                                                               │           │
│  │  ★ Spring handles ALL the cryptography, state management, and validation!  │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.18.10 ★ Common Errors and Troubleshooting — PKCE

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ COMMON ERRORS — PKCE INTEGRATION TROUBLESHOOTING                                        │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ┌───────────────────────────┬────────────────────────────────────────────┐ │           │
│  │  │  Error                    │  Cause & Fix                                │ │           │
│  │  ├───────────────────────────┼────────────────────────────────────────────┤ │           │
│  │  │                           │                                             │ │           │
│  │  │  "invalid_grant" at       │  CAUSE: code_verifier doesn't match the    │ │           │
│  │  │  /token endpoint          │  code_challenge sent during /authorize.    │ │           │
│  │  │                           │                                             │ │           │
│  │  │                           │  FIX: Ensure the same code_verifier is     │ │           │
│  │  │                           │  stored and sent. Spring does this auto-   │ │           │
│  │  │                           │  matically — if you see this, check if    │ │           │
│  │  │                           │  your session storage is working properly. │ │           │
│  │  │                           │                                             │ │           │
│  │  ├───────────────────────────┼────────────────────────────────────────────┤ │           │
│  │  │                           │                                             │ │           │
│  │  │  "invalid_request" —      │  CAUSE: requireProofKey=true but client   │ │           │
│  │  │  code_challenge required  │  didn't send code_challenge.               │ │           │
│  │  │                           │                                             │ │           │
│  │  │                           │  FIX: For public clients, set              │ │           │
│  │  │                           │  client-authentication-method: none.       │ │           │
│  │  │                           │  For confidential clients, add             │ │           │
│  │  │                           │  OAuth2AuthorizationRequestCustomizers     │ │           │
│  │  │                           │  .withPkce() to the request resolver.      │ │           │
│  │  │                           │                                             │ │           │
│  │  ├───────────────────────────┼────────────────────────────────────────────┤ │           │
│  │  │                           │                                             │ │           │
│  │  │  "unauthorized_client"    │  CAUSE: Client registered with             │ │           │
│  │  │                           │  CLIENT_SECRET_BASIC but no secret sent.   │ │           │
│  │  │                           │                                             │ │           │
│  │  │                           │  FIX: For public clients, use              │ │           │
│  │  │                           │  ClientAuthenticationMethod.NONE on       │ │           │
│  │  │                           │  the RegisteredClient (auth server).       │ │           │
│  │  │                           │                                             │ │           │
│  │  ├───────────────────────────┼────────────────────────────────────────────┤ │           │
│  │  │                           │                                             │ │           │
│  │  │  PKCE not being sent      │  CAUSE: Confidential client doesn't add   │ │           │
│  │  │  for confidential client  │  PKCE by default.                          │ │           │
│  │  │                           │                                             │ │           │
│  │  │                           │  FIX: Add the withPkce() customizer:       │ │           │
│  │  │                           │  authorizationRequestResolver              │ │           │
│  │  │                           │    .setAuthorizationRequestCustomizer(     │ │           │
│  │  │                           │      OAuth2AuthorizationRequest            │ │           │
│  │  │                           │      Customizers.withPkce())               │ │           │
│  │  │                           │                                             │ │           │
│  │  ├───────────────────────────┼────────────────────────────────────────────┤ │           │
│  │  │                           │                                             │ │           │
│  │  │  Session lost between     │  CAUSE: code_verifier stored in HTTP      │ │           │
│  │  │  /authorize and /callback │  session, but session expired or was lost. │ │           │
│  │  │                           │                                             │ │           │
│  │  │                           │  FIX: Ensure session management is         │ │           │
│  │  │                           │  configured. For stateless SPAs, use      │ │           │
│  │  │                           │  a custom OAuth2AuthorizationRequest      │ │           │
│  │  │                           │  Repository (e.g., cookie-based).          │ │           │
│  │  │                           │                                             │ │           │
│  │  └───────────────────────────┴────────────────────────────────────────────┘ │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

##### 20.18.11 ★ Summary — Integrating PKCE with Spring Security

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ★ SUMMARY — PKCE INTEGRATION WITH SPRING SECURITY                                         │
├──────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────────┐           │
│  │                                                                               │           │
│  │  ── AUTHORIZATION SERVER ─────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • Dependency: spring-boot-starter-oauth2-authorization-server              │           │
│  │  • PKCE built-in — NO special filter or endpoint config!                    │           │
│  │  • Enforce PKCE: RegisteredClient.clientSettings(                           │           │
│  │      ClientSettings.builder().requireProofKey(true))                        │           │
│  │  • Public client: .clientAuthenticationMethod(NONE), no clientSecret()     │           │
│  │  • Confidential client: .clientSecret() + .requireProofKey(true)           │           │
│  │                                                                               │           │
│  │  ── CLIENT APPLICATION ───────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • Dependency: spring-boot-starter-oauth2-client                            │           │
│  │  • Public client: set client-authentication-method: none in YAML            │           │
│  │    → Spring AUTOMATICALLY adds PKCE!                                        │           │
│  │  • Confidential client: add OAuth2AuthorizationRequestCustomizers           │           │
│  │    .withPkce() to the authorization request resolver                        │           │
│  │  • Spring handles: code_verifier generation, SHA-256 hashing,              │           │
│  │    session storage, and sending verifier during token exchange              │           │
│  │                                                                               │           │
│  │  ── RESOURCE SERVER ──────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  • ZERO changes! No PKCE awareness needed.                                  │           │
│  │  • JWT validation is identical regardless of PKCE.                          │           │
│  │                                                                               │           │
│  │  ── KEY SPRING CLASSES ───────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Client-side:                                                                │           │
│  │  • DefaultOAuth2AuthorizationRequestResolver → generates PKCE values       │           │
│  │  • OAuth2AuthorizationRequestCustomizers.withPkce() → enables PKCE         │           │
│  │  • OAuth2AuthorizationRequest → stores code_verifier                       │           │
│  │                                                                               │           │
│  │  Server-side:                                                                │           │
│  │  • OAuth2AuthorizationCodeAuthenticationProvider → validates PKCE           │           │
│  │  • ClientSettings.requireProofKey(true) → enforces PKCE                    │           │
│  │                                                                               │           │
│  │  ── FLOW IN ONE LINE ─────────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Client generates verifier → sends SHA-256 hash in /authorize →            │           │
│  │  user logs in → client gets code → client sends verifier to /token →       │           │
│  │  server verifies hash → issues tokens → client calls Resource Server       │           │
│  │                                                                               │           │
│  │  ── TOTAL CONFIG EFFORT ──────────────────────────────────────────           │           │
│  │                                                                               │           │
│  │  Public client:       2 lines (requireProofKey + auth method none)         │           │
│  │  Confidential client: 2 lines (requireProofKey + withPkce() customizer)    │           │
│  │  Resource server:     0 lines                                               │           │
│  │                                                                               │           │
│  └──────────────────────────────────────────────────────────────────────────────┘           │
│                                                                                              │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```



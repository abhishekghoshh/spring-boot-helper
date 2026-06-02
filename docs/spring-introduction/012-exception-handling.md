# Spring Exception Handling

## Theory

---

## HTTP Response Structure

Every HTTP response consists of exactly **3 parts**:

```
HTTP/1.1 200 OK                         ← Status Line (version + code + reason)
Content-Type: application/json          ← Headers (metadata about the response)
Cache-Control: no-cache
                                        ← Blank line separating headers from body
{ "id": 1, "name": "John" }            ← Body (actual payload)
```

| Part | Description |
|------|-------------|
| **Status Code** | 3-digit number indicating the result (e.g., 200, 404, 500) |
| **Headers** | Key-value metadata: content type, cache info, auth tokens, etc. |
| **Body** | The actual payload — JSON, XML, HTML, binary, or empty |

### Building Responses Manually with `ResponseEntity` in Spring Boot

`ResponseEntity<T>` gives you full control over all three parts of the HTTP response.

```java
// Full control: status + headers + body
@GetMapping("/user/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Custom-Header", "my-value");
    
    return ResponseEntity
            .status(HttpStatus.OK)           // status code
            .headers(headers)                 // headers
            .body(user);                      // body
}

// Shortcut builders
ResponseEntity.ok(user);                                  // 200 with body
ResponseEntity.created(URI.create("/user/1")).build();    // 201 with Location header
ResponseEntity.noContent().build();                       // 204 no body
ResponseEntity.notFound().build();                        // 404 no body
ResponseEntity.status(HttpStatus.CONFLICT).body(error);  // 409 with error body
```

---

## HTTP Response Code Categories Overview

```
┌─────────────────────────────────────────────────────────┐
│                  HTTP Status Code Ranges                  │
├──────────┬──────────────────────────────────────────────┤
│  1xx     │  Informational  - Request received, continue  │
│  2xx     │  Success        - Request fulfilled           │
│  3xx     │  Redirection    - Further action needed       │
│  4xx     │  Client Error   - Bad request from client     │
│  5xx     │  Server Error   - Server failed to fulfill    │
└──────────┴──────────────────────────────────────────────┘
```

> **Key Rule:** Only **5xx errors** are the responsibility of the server-side development team to fix. 4xx errors are caused by incorrect client behaviour.

---

## 1xx — Informational Responses

These are **provisional responses**. The server is acknowledging the request and telling the client to continue. The final response comes later.

### `100 Continue`

**Purpose:** The client wants to check if the server is willing to accept a large request (e.g., a big file upload) before actually sending the body. This avoids wasting bandwidth sending a large payload that the server will immediately reject.

**How it works:**

```
┌────────┐                                          ┌────────┐
│ Client │                                          │ Server │
└───┬────┘                                          └───┬────┘
    │                                                   │
    │  POST /upload                                     │
    │  Content-Type: application/octet-stream           │
    │  Content-Length: 50000000                         │
    │  Expect: 100-continue          ─────────────────► │
    │                                                   │
    │                  ◄─────────────────  100 Continue │
    │                                                   │
    │  [Actual 50MB body]            ─────────────────► │
    │                                                   │
    │                  ◄─────────────────────  200 OK   │
    └───────────────────────────────────────────────────┘
```

- Client sends the request **headers only** with `Expect: 100-continue`
- Server validates headers (auth, content-type, size limits, etc.)
- If valid → server sends `100 Continue` → client sends the body
- If invalid → server sends `413 Payload Too Large` or `401 Unauthorized` → client never sends the body

**Common HTTP methods:** `POST`, `PUT`

**Real-world use case:** Uploading a 2GB video file. Check auth and content-type first before streaming the entire file.

**Spring Boot — handling `Expect: 100-continue`:**

Spring Boot (via the embedded Tomcat/Jetty) handles `100-continue` automatically. But you can configure max body size to trigger rejections:

```yaml
# application.yml
server:
  tomcat:
    max-http-form-post-size: 10MB   # reject large uploads early
  max-http-request-header-size: 8KB
```

```java
@PostMapping("/upload")
public ResponseEntity<String> uploadFile(
        @RequestHeader("Content-Length") long contentLength,
        HttpServletRequest request) {

    // Server validates headers before reading body
    // Tomcat automatically sends 100-continue if Expect header is present
    // This method is only called after the full body is received

    if (contentLength > 10_000_000L) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body("File exceeds 10MB limit");
    }
    // process upload...
    return ResponseEntity.ok("Upload successful");
}
```

---

## 2xx — Success Responses

The request was received, understood, and accepted.

### `200 OK`

The standard success response. The body contains the requested resource or result.

**Common methods:** `GET`, `POST`, `PUT`, `PATCH`

```java
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id)); // 200 with body
}
```

---

### `201 Created`

**Purpose:** A new resource was successfully created. The response **must** include a `Location` header pointing to the newly created resource's URL.

**Common methods:** `POST`, sometimes `PUT`

**Real-world use case:** User registration, creating an order, adding a product to a catalogue.

```
Client                              Server
  │  POST /users                      │
  │  { "name": "Alice", ... }  ──────►│
  │                                   │ (creates user with id=42)
  │◄─────────────────────────── 201   │
  │  Location: /users/42              │
  │  { "id": 42, "name": "Alice" }   │
```

```java
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody @Valid UserRequest request) {
    User created = userService.create(request);
    
    URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    
    return ResponseEntity
            .created(location)   // sets status=201 and Location header
            .body(created);
}
```

---

### `202 Accepted`

**Purpose:** The request has been accepted for processing, but processing has **not been completed yet**. Used for asynchronous / long-running operations.

**Common methods:** `POST`, `PUT`, `DELETE`

**Real-world use case:** Triggering a report generation, video transcoding, sending bulk emails — work is queued and done in the background.

```
┌────────┐                                          ┌────────┐
│ Client │                                          │ Server │
└───┬────┘                                          └───┬────┘
    │  POST /reports/generate                           │
    │  { "type": "monthly-sales" }  ─────────────────► │
    │                                                   │ (queues job)
    │◄──────────── 202 Accepted                         │
    │  { "jobId": "abc-123",                            │
    │    "statusUrl": "/jobs/abc-123" }                 │
    │                                                   │
    │  GET /jobs/abc-123             ─────────────────► │
    │◄──────────── 200 { "status": "IN_PROGRESS" }      │
    │                                                   │
    │  GET /jobs/abc-123             ─────────────────► │
    │◄──────────── 200 { "status": "DONE",              │
    │                    "resultUrl": "/reports/xyz" }   │
```

```java
@PostMapping("/reports/generate")
public ResponseEntity<JobResponse> generateReport(@RequestBody ReportRequest request) {
    String jobId = reportService.queueAsync(request);
    
    URI statusUrl = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/jobs/{id}")
            .buildAndExpand(jobId)
            .toUri();
    
    return ResponseEntity
            .accepted()    // 202
            .body(new JobResponse(jobId, statusUrl.toString()));
}
```

---

### `204 No Content`

**Purpose:** The request succeeded but there is **no body** to return. Do not include a response body.

**Common methods:** `DELETE`, `PUT`, `PATCH`

**Real-world use case:** Deleting a resource, updating preferences where the updated state doesn't need to be returned.

```java
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build(); // 204, no body
}

@PutMapping("/users/{id}/preferences")
public ResponseEntity<Void> updatePreferences(
        @PathVariable Long id,
        @RequestBody PreferencesRequest request) {
    userService.updatePreferences(id, request);
    return ResponseEntity.noContent().build(); // 204
}
```

---

### `206 Partial Content`

**Purpose:** The server is returning only part of the resource because the client requested a specific byte range using the `Range` header. Essential for video streaming and resumable downloads.

**Common methods:** `GET`

**Real-world use case:** Netflix/YouTube streaming — browser requests video in chunks, not all at once. Resumable file downloads.

```
Client                                    Server
  │  GET /videos/movie.mp4                  │
  │  Range: bytes=0-1048575    ───────────► │
  │                                         │
  │◄─── 206 Partial Content                 │
  │  Content-Range: bytes 0-1048575/5242880 │
  │  Content-Length: 1048576                │
  │  [first 1MB of video data]              │
  │                                         │
  │  GET /videos/movie.mp4                  │
  │  Range: bytes=1048576-2097151 ────────► │
  │◄─── 206 Partial Content                 │
  │  [second 1MB chunk]                     │
```

```java
@GetMapping("/files/{name}")
public ResponseEntity<Resource> downloadFile(
        @PathVariable String name,
        @RequestHeader(value = "Range", required = false) String rangeHeader) {

    Resource file = storageService.load(name);
    long fileSize = file.contentLength();

    if (rangeHeader == null) {
        return ResponseEntity.ok()
                .contentLength(fileSize)
                .body(file);
    }

    // Parse "bytes=start-end"
    long start = parseRangeStart(rangeHeader);
    long end = Math.min(parseRangeEnd(rangeHeader, fileSize), fileSize - 1);
    long rangeLength = end - start + 1;

    InputStreamResource partialResource = new InputStreamResource(
            getPartialStream(file, start, rangeLength));

    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .header(HttpHeaders.CONTENT_RANGE,
                    "bytes " + start + "-" + end + "/" + fileSize)
            .contentLength(rangeLength)
            .body(partialResource);
}
```

---

## 3xx — Redirection Responses

The client must take additional action to complete the request — usually following a new URL.

### `301 Moved Permanently`

**Purpose:** The resource has been permanently moved to a new URL. All future requests should use the new URL. The HTTP method **may change** (browsers typically downgrade POST to GET on redirect).

**Common methods:** `GET`, `POST`

**Real-world use case:** Domain migration (`http://` → `https://`), website restructuring, retiring old API endpoints.

```
Client                                Server
  │  GET /old-path          ─────────►│
  │◄──── 301                          │
  │  Location: /new-path              │
  │                                   │
  │  GET /new-path          ─────────►│  ← browser may change POST to GET
  │◄──── 200 OK                       │
```

```java
@GetMapping("/old-endpoint")
public ResponseEntity<Void> redirectOldEndpoint() {
    return ResponseEntity
            .status(HttpStatus.MOVED_PERMANENTLY)
            .location(URI.create("/new-endpoint"))
            .build();
}
```

---

### `308 Permanent Redirect`

**Purpose:** Same as `301` (permanent, cached by clients), but the **HTTP method must NOT change**. A `POST` to the old URL will be `POST`-ed to the new URL.

**Common methods:** `POST`, `PUT`, `PATCH`

**Real-world use case:** Permanently moving an API endpoint while ensuring clients continue using the same HTTP method (e.g., a form submission endpoint that moved).

```
┌────────────────────────────────────────────────────────────────┐
│               301 vs 308 Comparison                            │
├──────────────────────┬─────────────────────────────────────────┤
│        301           │               308                       │
├──────────────────────┼─────────────────────────────────────────┤
│ Permanent redirect   │ Permanent redirect                      │
│ Method MAY change    │ Method MUST stay the same               │
│ POST → may become GET│ POST → stays POST                       │
│ Cached by client     │ Cached by client                        │
└──────────────────────┴─────────────────────────────────────────┘
```

```java
@PostMapping("/api/v1/orders")
public ResponseEntity<Void> redirectToV2() {
    return ResponseEntity
            .status(HttpStatus.PERMANENT_REDIRECT)  // 308
            .location(URI.create("/api/v2/orders"))
            .build();
    // Client will POST to /api/v2/orders (method preserved)
}
```

---

### `304 Not Modified`

**Purpose:** The resource has not changed since the client last fetched it. The server tells the client to use its cached copy — **no body is returned**, saving bandwidth.

**Common methods:** `GET`, `HEAD`

**How it works — Conditional GET flow:**

```
┌────────┐                                              ┌────────┐
│ Client │                                              │ Server │
└───┬────┘                                              └───┬────┘
    │                                                       │
    │  GET /products/42               ────────────────────► │
    │◄────────────────────────── 200 OK                     │
    │  Last-Modified: Mon, 01 Jun 2026 10:00:00 GMT         │
    │  ETag: "abc123"                                       │
    │  { "id": 42, "name": "Widget" }                      │
    │  [Client caches this response]                        │
    │                                                       │
    │  GET /products/42               ────────────────────► │
    │  If-Modified-Since: Mon, 01 Jun 2026 10:00:00 GMT     │
    │  If-None-Match: "abc123"                              │
    │                                                       │
    │          (Server checks: resource NOT modified)       │
    │◄──────────────────────────── 304 Not Modified         │
    │  [No body — client uses its cached copy]              │
    │                                                       │
    │  GET /products/42               ────────────────────► │
    │  If-Modified-Since: Mon, 01 Jun 2026 10:00:00 GMT     │
    │                                                       │
    │          (Server checks: resource WAS modified)       │
    │◄────────────────────────── 200 OK                     │
    │  Last-Modified: Tue, 02 Jun 2026 09:00:00 GMT         │
    │  { "id": 42, "name": "Widget Pro" }                  │
```

**Real-world use case:** Reducing API load for frequently-read but rarely-updated resources (product catalogues, configuration, reference data).

```java
@GetMapping("/products/{id}")
public ResponseEntity<Product> getProduct(
        @PathVariable Long id,
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

    Product product = productService.findById(id);
    String currentETag = "\"" + product.getVersion() + "\"";

    // Check if client already has the latest version
    if (currentETag.equals(ifNoneMatch)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .eTag(currentETag)
                .build(); // 304 — no body
    }

    return ResponseEntity.ok()
            .eTag(currentETag)
            .lastModified(product.getUpdatedAt())
            .body(product); // 200 with body
}
```

> **Important:** Do **not** use `304 Not Modified` with `PATCH`. If a client PATCHes a field that already has the same value in the DB (no-op update), return `200 OK` or `204 No Content` — **not** `304`. `304` is specifically for caching conditional GETs.

---

## 4xx — Client Error Responses

The request contains bad syntax or cannot be fulfilled. The **client** is responsible for the error.

### `400 Bad Request`

**Purpose:** The server cannot process the request because of a client-side error — malformed JSON, invalid field type, missing required fields, constraint violations.

**Common methods:** `POST`, `PUT`, `PATCH`

**Real-world use case:** User submits a registration form with an invalid email format, or sends a JSON body with a typo making it unparseable.

```
Client                            Server
  │  POST /users                    │
  │  { "age": "not-a-number" } ────►│
  │◄──── 400 Bad Request            │
  │  {                              │
  │    "error": "Validation failed",│
  │    "details": [                 │
  │      "age must be an integer"   │
  │    ]                            │
  │  }                              │
```

```java
// Global validation error handler
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        return ResponseEntity
                .badRequest()   // 400
                .body(new ErrorResponse("Validation failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("Malformed JSON body", null));
    }
}

// DTO with validation
public record UserRequest(
    @NotBlank String name,
    @Email String email,
    @Min(0) @Max(150) int age
) {}
```

---

### `401 Unauthorized`

**Purpose:** The request requires authentication but the client has not provided credentials (or the credentials are missing/expired). The response **must** include a `WWW-Authenticate` header telling the client how to authenticate.

> Despite the name "Unauthorized", this actually means **Unauthenticated** — "we don't know who you are."

**Common methods:** All methods on protected endpoints.

**Real-world use case:** Calling a protected API endpoint without providing a Bearer token, or using an expired JWT.

```
┌────────┐                                        ┌────────┐
│ Client │                                        │ Server │
└───┬────┘                                        └───┬────┘
    │  GET /api/profile                               │
    │  [no Authorization header]    ────────────────► │
    │◄────────────── 401 Unauthorized                 │
    │  WWW-Authenticate: Bearer realm="api"           │
    │                                                 │
    │  GET /api/profile                               │
    │  Authorization: Bearer eyJhbGci... ───────────► │
    │◄────────────── 200 OK                           │
```

```java
// Spring Security configuration
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/public/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults())
        )
        // Spring Security automatically returns 401 with WWW-Authenticate header
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                    """
                    {"error": "Authentication required",
                     "message": "%s"}
                    """.formatted(authException.getMessage())
                );
            })
        );
    return http.build();
}
```

---

### `403 Forbidden`

**Purpose:** The server knows **who you are** (authenticated), but you do not have **permission** to access this resource (not authorized). Unlike `401`, re-authenticating won't help.

**Common methods:** All methods on role-restricted endpoints.

**Real-world use case:** A regular user trying to access the admin dashboard, or trying to delete another user's account.

```
┌──────────────────────────────────────────────────────────────┐
│               401 vs 403 — Key Difference                    │
├─────────────────────────┬────────────────────────────────────┤
│          401            │               403                  │
├─────────────────────────┼────────────────────────────────────┤
│ Not authenticated       │ Authenticated but not authorized   │
│ "Who are you?"          │ "I know who you are, but NO"       │
│ Re-auth might help      │ Re-auth will NOT help              │
│ Missing/invalid token   │ Wrong role/permission              │
└─────────────────────────┴────────────────────────────────────┘
```

```java
// Method-level security
@RestController
@RequestMapping("/users")
public class UserController {

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")  // only ADMIN can delete
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }
    // Non-admin gets 403 Forbidden automatically from Spring Security
}

// Custom 403 handler
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.exceptionHandling(ex -> ex
        .accessDeniedHandler((request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                """
                {"error": "Access denied",
                 "message": "You don't have permission to perform this action"}
                """
            );
        })
    );
    return http.build();
}
```

---

### `404 Not Found`

**Purpose:** The server cannot find the requested resource. Either the API endpoint itself doesn't exist, or the specific resource (by ID) doesn't exist.

**Common methods:** `GET`, `DELETE`, `PUT`, `PATCH`

**Real-world use case:** Fetching a user who was already deleted, or hitting a misspelled endpoint URL.

```java
// Custom exception
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " not found");
    }
}

// Service layer
public User findById(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
}

// Global handler
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("Not Found", ex.getMessage()));
}
```

---

### `405 Method Not Allowed`

**Purpose:** The HTTP method used is not supported for the requested endpoint. The response **must** include an `Allow` header listing the permitted methods.

**Common methods:** Triggered by using any wrong method.

**Real-world use case:** A client sends `GET /submit` but the server only supports `POST /submit`.

```
Client                                  Server
  │  GET /orders/submit    ───────────► │
  │◄──── 405 Method Not Allowed         │
  │  Allow: POST, OPTIONS               │
```

Spring Boot returns `405` automatically when a method mismatch occurs. To customize:

```java
@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException ex) {

    HttpHeaders headers = new HttpHeaders();
    if (ex.getSupportedHttpMethods() != null) {
        headers.setAllow(ex.getSupportedHttpMethods());
    }

    return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .headers(headers)
            .body(new ErrorResponse(
                "Method Not Allowed",
                "Use one of: " + ex.getSupportedMethods()
            ));
}
```

---

### `409 Conflict`

**Purpose:** The request conflicts with the current state of the server. The client's request is valid, but it cannot be completed due to a conflict with an existing resource or ongoing operation.

**Common methods:** `POST`, `PUT`, `PATCH`, `DELETE`

**Real-world use cases:**
- Creating a user with an email that already exists
- Trying to delete a resource that is currently being processed by another request
- Optimistic locking conflict (two clients trying to update the same record simultaneously)

```
Client A                          Server                  Client B
  │  DELETE /users/42     ───────►│                          │
  │                               │ (processing...)          │
  │                               │◄── DELETE /users/42 ────│
  │                               │                          │
  │                               │─── 409 Conflict ────────►│
  │◄── 204 No Content             │
```

```java
// Duplicate resource
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody @Valid UserRequest request) {
    if (userRepository.existsByEmail(request.email())) {
        throw new ConflictException("User with email " + request.email() + " already exists");
    }
    User created = userService.create(request);
    return ResponseEntity.created(buildLocationUri(created.getId())).body(created);
}

// Optimistic lock conflict
@ExceptionHandler(OptimisticLockingFailureException.class)
public ResponseEntity<ErrorResponse> handleOptimisticLock(
        OptimisticLockingFailureException ex) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("Conflict",
                "Resource was modified by another request. Please retry."));
}

@ExceptionHandler(ConflictException.class)
public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("Conflict", ex.getMessage()));
}
```

---

### `422 Unprocessable Entity`

**Purpose:** The request is syntactically correct and well-formed, but the server **cannot process** it due to **business logic / semantic validation** failures. Unlike `400` (malformed input), with `422` the server understood the request but rejected it based on business rules.

**Common methods:** `POST`, `PUT`, `PATCH`

**Real-world use case:** Placing an order for a product only available in the US, but the user's shipping address is in a restricted country. The JSON is valid, but business rules prevent processing.

```
┌─────────────────────────────────────────────────────────────────┐
│               400 vs 422 — Key Difference                       │
├──────────────────────────┬──────────────────────────────────────┤
│          400             │               422                    │
├──────────────────────────┼──────────────────────────────────────┤
│ Malformed request        │ Well-formed but semantically invalid │
│ Can't parse/deserialize  │ Parsed fine, fails business rules    │
│ Missing required fields  │ Country not allowed for this product │
│ Invalid data types       │ Account balance insufficient         │
└──────────────────────────┴──────────────────────────────────────┘
```

```java
// Business rule violation
@PostMapping("/orders")
public ResponseEntity<Order> placeOrder(@RequestBody @Valid OrderRequest request) {
    String country = request.shippingAddress().country();
    
    if (restrictedCountryService.isRestricted(country)) {
        throw new UnprocessableEntityException(
            "Shipping to " + country + " is not available for this product"
        );
    }
    
    Order order = orderService.place(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(order);
}

@ExceptionHandler(UnprocessableEntityException.class)
public ResponseEntity<ErrorResponse> handleUnprocessable(
        UnprocessableEntityException ex) {
    return ResponseEntity
            .unprocessableEntity()   // 422
            .body(new ErrorResponse("Unprocessable Entity", ex.getMessage()));
}
```

---

### `429 Too Many Requests`

**Purpose:** The client has sent too many requests in a given time window (**rate limiting**). The response should include a `Retry-After` header indicating when the client can try again.

**Common methods:** All methods on rate-limited endpoints.

**Real-world use case:** Public APIs limiting free tier users (e.g., 100 requests/minute), DDoS protection, preventing brute-force login attempts.

```
┌────────┐                                        ┌────────┐
│ Client │                                        │ Server │
└───┬────┘                                        └───┬────┘
    │  [101st request within 1 minute]               │
    │  GET /api/search?q=java        ───────────────►│
    │◄──────── 429 Too Many Requests                 │
    │  Retry-After: 30                               │
    │  X-RateLimit-Limit: 100                        │
    │  X-RateLimit-Remaining: 0                      │
    │  X-RateLimit-Reset: 1717321800                 │
```

```java
// Using Bucket4j for rate limiting
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        String clientIp = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-RateLimit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                """
                {"error": "Too Many Requests",
                 "message": "Rate limit exceeded. Try again in %d seconds."}
                """.formatted(retryAfterSeconds)
            );
        }
    }

    private Bucket createBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                .build();
    }
}
```

---

## 5xx — Server Error Responses

The server failed to fulfil a valid request. These are **the server team's responsibility** to fix. 4xx errors are caused by clients; 5xx errors are caused by the server.

### `500 Internal Server Error`

**Purpose:** A generic catch-all for unexpected server-side failures. Use when no more specific 5xx code applies. Indicates a bug or unhandled exception in your application.

**Real-world use case:** Unhandled `NullPointerException`, database connection pool exhausted, unexpected exception escaping your error handlers.

```java
// Catch-all exception handler — prevents stack traces leaking to clients
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
        HttpServletRequest request) {
    // Log the full stack trace internally (never expose it to the client)
    log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

    return ResponseEntity
            .internalServerError()  // 500
            .body(new ErrorResponse(
                "Internal Server Error",
                "An unexpected error occurred. Please try again later."
            ));
}
```

> **Security Note:** Never expose stack traces, exception messages, or internal details in `500` responses. Log them server-side only.

---

### `501 Not Implemented`

**Purpose:** The server does not support the functionality required to fulfil the request. The endpoint or HTTP method is recognized but not yet implemented.

**Real-world use case:** A feature is in development, an API stub is deployed but the implementation isn't complete yet, or a method is intentionally not supported.

```java
@DeleteMapping("/legacy-data")
public ResponseEntity<ErrorResponse> deleteLegacyData() {
    return ResponseEntity
            .status(HttpStatus.NOT_IMPLEMENTED)
            .body(new ErrorResponse(
                "Not Implemented",
                "Bulk deletion is scheduled for v2.1. Expected: Q3 2026."
            ));
}

// Or throw from a service
public void unsupportedOperation() {
    throw new UnsupportedOperationException("Feature not yet implemented");
}

@ExceptionHandler(UnsupportedOperationException.class)
public ResponseEntity<ErrorResponse> handleNotImplemented(UnsupportedOperationException ex) {
    return ResponseEntity
            .status(HttpStatus.NOT_IMPLEMENTED)
            .body(new ErrorResponse("Not Implemented", ex.getMessage()));
}
```

---

### `502 Bad Gateway`

**Purpose:** The server is acting as a **gateway or proxy** and received an **invalid response** from an upstream server it was trying to reach.

**Common causes:**
- Your Spring Boot app calls a downstream microservice and it returns garbage
- NGINX cannot reach your Spring Boot application (app crashed or port mismatch)
- A third-party API returns an unexpected error format

```
┌────────┐        ┌────────┐        ┌─────────────┐
│ Client │        │ NGINX  │        │ Spring Boot │
└───┬────┘        └───┬────┘        └──────┬──────┘
    │                 │                    │
    │  GET /api/data  │                    │
    │───────────────► │                    │
    │                 │  [App is down]     │
    │                 │ ─────────────────► │  ✗ Connection refused
    │                 │                    │
    │ ◄─────── 502    │
    │  Bad Gateway    │
```

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────────┐
│   Client     │     │  Your Service   │     │  Partner API     │
└──────┬───────┘     └────────┬────────┘     └────────┬─────────┘
       │                      │                       │
       │  POST /payment        │                       │
       │─────────────────────►│                       │
       │                      │  POST /charge         │
       │                      │──────────────────────►│
       │                      │◄── 200 {invalid JSON} │
       │                      │                       │
       │◄─── 502 Bad Gateway  │
       │  "Upstream service   │
       │   returned invalid   │
       │   response"          │
```

```java
// Feign client calling upstream service
@FeignClient(name = "payment-service")
public interface PaymentClient {
    @PostMapping("/charge")
    PaymentResult charge(@RequestBody ChargeRequest request);
}

// Handling upstream errors in your service
@Service
public class PaymentService {

    @Autowired
    private PaymentClient paymentClient;

    public PaymentResult processPayment(ChargeRequest request) {
        try {
            return paymentClient.charge(request);
        } catch (FeignException.BadGateway | FeignException.ServiceUnavailable ex) {
            throw new UpstreamServiceException("Payment gateway is unavailable", ex);
        } catch (DecodeException ex) {
            throw new UpstreamServiceException("Payment gateway returned invalid response", ex);
        }
    }
}

@ExceptionHandler(UpstreamServiceException.class)
public ResponseEntity<ErrorResponse> handleBadGateway(UpstreamServiceException ex) {
    log.error("Upstream service error: {}", ex.getMessage(), ex);
    return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(new ErrorResponse("Bad Gateway",
                "An upstream service is unavailable. Please try again shortly."));
}
```

---

## Summary: Who is Responsible?

```
┌──────────────────────────────────────────────────────────────────┐
│                   Error Responsibility Matrix                     │
├──────────┬──────────────────┬──────────────────────────────────  │
│  Range   │  Owner           │  Action                            │
├──────────┼──────────────────┼──────────────────────────────────  │
│  4xx     │  Client          │  Fix the request (retry w/ fix)    │
│  5xx     │  Server team     │  Fix the server-side code/infra    │
└──────────┴──────────────────┴──────────────────────────────────  │
```

| Code | Name | Responsibility | Common Methods |
|------|------|---------------|----------------|
| 100 | Continue | Infrastructure | POST, PUT |
| 200 | OK | - | GET, POST, PUT, PATCH |
| 201 | Created | - | POST |
| 202 | Accepted | - | POST, PUT |
| 204 | No Content | - | DELETE, PUT, PATCH |
| 206 | Partial Content | - | GET |
| 301 | Moved Permanently | - | GET, POST |
| 304 | Not Modified | - | GET |
| 308 | Permanent Redirect | - | POST, PUT |
| 400 | Bad Request | **Client** | POST, PUT, PATCH |
| 401 | Unauthorized | **Client** | All |
| 403 | Forbidden | **Client** | All |
| 404 | Not Found | **Client** | GET, DELETE, PUT |
| 405 | Method Not Allowed | **Client** | All |
| 409 | Conflict | **Client** | POST, PUT, DELETE |
| 422 | Unprocessable Entity | **Client** | POST, PUT, PATCH |
| 429 | Too Many Requests | **Client** | All |
| 500 | Internal Server Error | **Server Team** | All |
| 501 | Not Implemented | **Server Team** | All |
| 502 | Bad Gateway | **Server Team** | All |




---

## Spring Exception Handling Architecture

### Class Hierarchy Overview

When an exception escapes a controller method, Spring's `DispatcherServlet` does **not** handle it directly. It delegates to a chain of `HandlerExceptionResolver` implementations. Understanding this chain is essential for knowing where your own exception handling plugs in.

```
                        «interface»
                  HandlerExceptionResolver
                          │
          ┌───────────────┼──────────────────────┐
          │               │                      │
HandlerExceptionR    DefaultError          AbstractHandlerExceptionResolver
 esolverComposite    Attributes (*)              │ (abstract)
  (delegates to                    ┌─────────────┼──────────────────────┐
   ordered list)                   │             │                      │
                        AbstractHandlerMethod  ResponseStatus    DefaultHandler
                        ExceptionResolver      ExceptionResolver  ExceptionResolver
                          (abstract)
                              │
                    ExceptionHandlerException
                          Resolver
                    (handles @ExceptionHandler)
```

> `DefaultErrorAttributes` is not a resolver in the same chain — it is used by Spring Boot's `BasicErrorController` to produce `/error` responses. It is listed separately below.

---

### Full Exception Handling Flow in Spring MVC

```
HTTP Request
     │
     ▼
DispatcherServlet.doDispatch()
     │
     │  calls HandlerAdapter → your @Controller method
     │
     │  ← Exception is thrown
     │
     ▼
DispatcherServlet.processHandlerException()
     │
     ▼
HandlerExceptionResolverComposite
     │  iterates resolvers in order (by priority):
     │
     ├─► [1] ExceptionHandlerExceptionResolver  ← @ExceptionHandler / @ControllerAdvice
     │         resolved? → write response & return ModelAndView
     │
     ├─► [2] ResponseStatusExceptionResolver    ← @ResponseStatus on exception class
     │         resolved? → write response & return ModelAndView
     │
     ├─► [3] DefaultHandlerExceptionResolver    ← Spring MVC built-in exceptions
     │         resolved? → write response & return ModelAndView
     │
     │  none resolved?
     │
     ▼
Servlet container error handling
     │
     ▼
Spring Boot BasicErrorController  ← GET /error
     │
     ▼
DefaultErrorAttributes  ← builds the error attributes map
     │
     ▼
{ timestamp, status, error, message, path }  → client
```

---

### 1. `HandlerExceptionResolver` — The Root Interface

```java
// org.springframework.web.servlet
public interface HandlerExceptionResolver {

    /**
     * Try to resolve the given exception.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param handler  the handler (controller method) that raised the exception, may be null
     * @param ex       the exception that was raised
     * @return a ModelAndView to use for error rendering, or null if not resolved
     */
    @Nullable
    ModelAndView resolveException(HttpServletRequest request,
                                  HttpServletResponse response,
                                  @Nullable Object handler,
                                  Exception ex);
}
```

**Key contract:**
- Returns a **non-null `ModelAndView`** → exception is considered handled, processing stops
- Returns **`null`** → this resolver cannot handle the exception, try the next one
- Returns **empty `ModelAndView`** → handled, but no view to render (response already written)

**Custom implementation (rarely needed, prefer `@ExceptionHandler`):**

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomHandlerExceptionResolver implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception ex) {
        if (ex instanceof MyDomainException domainEx) {
            try {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                    """
                    {"error": "%s"}
                    """.formatted(domainEx.getMessage())
                );
            } catch (IOException ignored) {}

            return new ModelAndView(); // non-null empty = handled, no view
        }
        return null; // not handled, pass to next resolver
    }
}
```

---

### 2. `HandlerExceptionResolverComposite` — The Delegating Composite

`HandlerExceptionResolverComposite` is the **single resolver registered with `DispatcherServlet`**. Internally it holds an **ordered list** of the real resolvers and iterates them in priority order until one returns a non-null `ModelAndView`.

```
HandlerExceptionResolverComposite
│
│  private List<HandlerExceptionResolver> resolvers;
│
│  resolveException(request, response, handler, ex) {
│      for (resolver in resolvers) {
│          ModelAndView mav = resolver.resolveException(...);
│          if (mav != null) return mav;   ← stop, handled
│      }
│      return null;   ← unhandled
│  }
```

**Default ordered list (Spring MVC):**

| Order | Resolver | Priority |
|-------|----------|----------|
| 1 | `ExceptionHandlerExceptionResolver` | Highest |
| 2 | `ResponseStatusExceptionResolver` | Medium |
| 3 | `DefaultHandlerExceptionResolver` | Lowest |

**How Spring registers them (in `WebMvcConfigurationSupport`):**

```java
// Simplified from Spring source
@Bean
public HandlerExceptionResolver handlerExceptionResolver() {
    List<HandlerExceptionResolver> resolvers = new ArrayList<>();

    resolvers.add(new ExceptionHandlerExceptionResolver()); // order=0
    resolvers.add(new ResponseStatusExceptionResolver());    // order=1
    resolvers.add(new DefaultHandlerExceptionResolver());    // order=2

    HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
    composite.setOrder(0);
    composite.setExceptionResolvers(resolvers);
    return composite;
}
```

**Plugging in a custom resolver via configuration:**

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        // Add at the beginning so it runs first
        resolvers.add(0, new CustomHandlerExceptionResolver());
    }
}
```

---

### 3. `AbstractHandlerExceptionResolver` — Base Template

An **abstract base class** that provides common plumbing: handler type/class filtering, logging, and the template method pattern. Concrete resolvers extend this instead of implementing the interface directly.

```java
// Simplified from Spring source
public abstract class AbstractHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    private int order = Ordered.LOWEST_PRECEDENCE;

    @Nullable
    private Set<Class<?>> mappedHandlerClasses;  // restrict to specific handler types

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                          HttpServletResponse response,
                                          @Nullable Object handler,
                                          Exception ex) {

        if (shouldApplyTo(request, handler)) {       // ← filter check
            prepareResponse(ex, response);
            ModelAndView result = doResolveException(request, response, handler, ex);
            if (result != null) {
                logException(ex, request);
            }
            return result;
        }
        return null;  // skip this resolver for this handler type
    }

    // Subclasses implement this:
    @Nullable
    protected abstract ModelAndView doResolveException(HttpServletRequest request,
                                                        HttpServletResponse response,
                                                        @Nullable Object handler,
                                                        Exception ex);
}
```

**What `shouldApplyTo()` does:**

```
shouldApplyTo(request, handler)
    │
    ├─ mappedHandlers set?    → check if handler is in the set
    ├─ mappedHandlerClasses set? → check if handler's class is listed
    └─ neither set?           → applies to ALL handlers (default)
```

**Restricting a resolver to specific controller classes:**

```java
@Bean
public MyCustomResolver myResolver() {
    MyCustomResolver resolver = new MyCustomResolver();
    resolver.setMappedHandlerClasses(OrderController.class, PaymentController.class);
    return resolver;
}
```

---

### 4. `AbstractHandlerMethodExceptionResolver` — Filters to `HandlerMethod`

Extends `AbstractHandlerExceptionResolver` and adds an additional filter: it only applies when the `handler` is a `HandlerMethod` (i.e., a `@RequestMapping` method on a `@Controller`). Non-method handlers (like `HttpRequestHandler`) are skipped.

```java
public abstract class AbstractHandlerMethodExceptionResolver
        extends AbstractHandlerExceptionResolver {

    @Override
    protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
        if (handler == null) {
            return super.shouldApplyTo(request, null);
        }
        // Only apply to @Controller methods, not HttpRequestHandler etc.
        if (handler instanceof HandlerMethod handlerMethod) {
            return super.shouldApplyTo(request, handlerMethod.getBean());
        }
        return false;
    }

    @Override
    protected final ModelAndView doResolveException(...) {
        return doResolveHandlerMethodException(request, response, (HandlerMethod) handler, ex);
    }

    // Subclasses implement this:
    protected abstract ModelAndView doResolveHandlerMethodException(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable HandlerMethod handlerMethod,
            Exception ex);
}
```

---

### 5. `ExceptionHandlerExceptionResolver` — Your `@ExceptionHandler` Methods

**This is the most important resolver.** It finds and invokes `@ExceptionHandler` methods — both on the same `@Controller` class and on any `@ControllerAdvice` / `@RestControllerAdvice` beans.

**Resolution priority:**

```
Exception thrown by @Controller method
         │
         ▼
ExceptionHandlerExceptionResolver
         │
         ├─► Look for @ExceptionHandler in the SAME @Controller first
         │       matching exception type (most-specific wins)
         │
         └─► If not found, search @ControllerAdvice beans
                 in declaration order / @Order
                 matching exception type (most-specific wins)
```

**How exception type matching works:**

```
Exception:  UserNotFoundException extends ResourceNotFoundException extends RuntimeException

@ExceptionHandler candidates:
  A: @ExceptionHandler(UserNotFoundException.class)     → exact match   ← WINS
  B: @ExceptionHandler(ResourceNotFoundException.class) → supertype
  C: @ExceptionHandler(RuntimeException.class)          → wider supertype

Most-specific (closest in hierarchy) always wins.
```

```java
// ── Local handler (inside @Controller) — highest priority ──────────────────
@RestController
@RequestMapping("/orders")
public class OrderController {

    // Only handles exceptions from THIS controller
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Order not found", ex.getMessage()));
    }
}

// ── Global handler (@RestControllerAdvice) — applies to ALL controllers ─────
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Most-specific first (good practice, though Spring resolves by type)

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Not Found", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Validation Failed", errors.toString()));
    }

    @ExceptionHandler(Exception.class)  // catch-all — least specific
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest req) {
        log.error("Unhandled: {} at {}", ex.getMessage(), req.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Internal Server Error",
                        "An unexpected error occurred"));
    }
}
```

**Scoping `@ControllerAdvice` to specific packages or annotations:**

```java
// Only applies to controllers in the 'orders' package
@RestControllerAdvice(basePackages = "com.example.orders")
public class OrderExceptionHandler { ... }

// Only applies to @RestController classes
@ControllerAdvice(annotations = RestController.class)
public class RestExceptionHandler { ... }
```

**Access to full method parameter resolution in `@ExceptionHandler`:**

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest request,     // ← injected automatically
        WebRequest webRequest,          // ← injected automatically
        Locale locale,                  // ← injected automatically
        Authentication authentication) { // ← injected automatically
    log.warn("Access denied for user {} at {}", authentication.getName(),
              request.getRequestURI());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("Forbidden", "Access denied"));
}
```

---

### 6. `ResponseStatusExceptionResolver` — `@ResponseStatus` on Exception Classes

Handles exceptions annotated with `@ResponseStatus`. When this resolver encounters such an exception, it reads the annotation and sets the HTTP status code + reason phrase on the response — **no `@ExceptionHandler` method needed**.

**How it works:**

```
@ResponseStatus(HttpStatus.NOT_FOUND, reason = "User not found")
public class UserNotFoundException extends RuntimeException { ... }

    Controller throws UserNotFoundException
            │
            ▼
    ExceptionHandlerExceptionResolver  → no @ExceptionHandler for this? → null
            │
            ▼
    ResponseStatusExceptionResolver
            │ finds @ResponseStatus on UserNotFoundException
            │ sets response.status = 404
            │ sets response.sendError(404, "User not found")
            ▼
    returns non-null ModelAndView (handled)
```

**Also handles `ResponseStatusException` directly (no annotation needed):**

```java
// Option A: Annotate the exception class
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Product not found")
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product " + id + " not found");
    }
}

// Usage — Spring automatically returns 404 with reason
public Product findProduct(Long id) {
    return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
}
```

```java
// Option B: Throw ResponseStatusException inline (no custom class needed)
@GetMapping("/products/{id}")
public Product getProduct(@PathVariable Long id) {
    return productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product " + id + " not found"
            ));
}
```

**Limitation of `@ResponseStatus`:** The `reason` is sent via `HttpServletResponse.sendError()`, which means the Servlet container handles the error — the response body is the container's default HTML error page, **not** your JSON error format. Prefer `@ExceptionHandler` for JSON APIs; use `ResponseStatusException` only for simple status-only responses.

---

### 7. `DefaultHandlerExceptionResolver` — Spring MVC Built-in Exceptions

The last resort resolver in the chain. Handles well-known Spring MVC exceptions and maps them to appropriate HTTP status codes **without** requiring any `@ExceptionHandler` methods. This is what makes Spring automatically return `405` for a wrong HTTP method, `400` for a type mismatch, etc.

**Built-in exception mappings:**

| Spring MVC Exception | HTTP Status |
|----------------------|-------------|
| `HttpRequestMethodNotSupportedException` | `405 Method Not Allowed` |
| `HttpMediaTypeNotSupportedException` | `415 Unsupported Media Type` |
| `HttpMediaTypeNotAcceptableException` | `406 Not Acceptable` |
| `MissingPathVariableException` | `500 Internal Server Error` |
| `MissingServletRequestParameterException` | `400 Bad Request` |
| `MissingServletRequestPartException` | `400 Bad Request` |
| `ServletRequestBindingException` | `400 Bad Request` |
| `MethodArgumentTypeMismatchException` | `400 Bad Request` |
| `NoHandlerFoundException` | `404 Not Found` |
| `AsyncRequestTimeoutException` | `503 Service Unavailable` |
| `BindException` | `400 Bad Request` |

**These are handled automatically — you get the right status code for free.** You can override them in `@ExceptionHandler` if you need a custom JSON body:

```java
// Override the default 405 behaviour to add a custom JSON body
@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException ex) {

    HttpHeaders headers = new HttpHeaders();
    if (ex.getSupportedHttpMethods() != null) {
        headers.setAllow(ex.getSupportedHttpMethods());
    }

    return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .headers(headers)
            .body(new ErrorResponse("Method Not Allowed",
                "Supported methods: " + ex.getSupportedMethods()));
}

// Override the default 400 to show which parameter is missing
@ExceptionHandler(MissingServletRequestParameterException.class)
public ResponseEntity<ErrorResponse> handleMissingParam(
        MissingServletRequestParameterException ex) {
    return ResponseEntity.badRequest()
            .body(new ErrorResponse("Bad Request",
                "Required parameter '" + ex.getParameterName() + "' is missing"));
}
```

---

### 8. `DefaultErrorAttributes` — Spring Boot's `/error` Fallback

`DefaultErrorAttributes` is **not part of the `HandlerExceptionResolver` chain** — it operates at a different level. When no resolver handles the exception (or the exception escapes the Servlet container), the Servlet container forwards the request to `/error`. Spring Boot's `BasicErrorController` handles that path, and it uses `DefaultErrorAttributes` to build the error response attributes.

```
Unhandled exception
        │
        ▼
Servlet container catches it
        │  forwards to /error (configured in server.error.path)
        ▼
BasicErrorController.error(request)
        │
        ▼
DefaultErrorAttributes.getErrorAttributes(webRequest, options)
        │  reads attributes stored by ErrorAttributes from the request
        ▼
Returns Map:
{
  "timestamp": "2026-06-02T10:30:00",
  "status":    500,
  "error":     "Internal Server Error",
  "message":   "...",
  "path":      "/api/users/99"
}
```

**Customising `DefaultErrorAttributes`:**

```java
// Extend to add your own fields to the /error response
@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                                                   ErrorAttributeOptions options) {
        Map<String, Object> attrs = super.getErrorAttributes(webRequest, options);

        // Add a correlation ID for distributed tracing
        attrs.put("correlationId", MDC.get("correlationId"));

        // Remove verbose 'trace' field in production
        attrs.remove("trace");

        // Add support URL
        attrs.put("support", "https://support.example.com");

        return attrs;
    }
}
```

**Configure what fields are included:**

```yaml
# application.yml
server:
  error:
    include-message: always       # include exception message
    include-binding-errors: always # include field validation errors
    include-stacktrace: never     # never expose stack traces (security)
    include-exception: false      # hide exception class name
```

---

### Full Picture — All Classes Together

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Spring Exception Handling Architecture                │
└─────────────────────────────────────────────────────────────────────────┘

  HTTP Request → DispatcherServlet
                      │
                      │  controller method throws Exception
                      ▼
         HandlerExceptionResolverComposite
         (single entry point, iterates in order)
                      │
          ┌───────────┼────────────────────┐
          │           │                    │
          ▼           ▼                    ▼
  [1] ExceptionHandler  [2] ResponseStatus  [3] DefaultHandler
      ExceptionResolver      ExceptionResolver     ExceptionResolver
          │                      │                      │
    Looks for           Reads @ResponseStatus   Handles built-in
    @ExceptionHandler   on exception class      Spring MVC exceptions
    in @Controller      or ResponseStatus        e.g. 405, 415, 400
    then @ControllerAdvice  Exception directly
          │                      │                      │
          └───────────┬──────────┘                      │
                      │                                  │
                resolved (non-null ModelAndView)?         │
                Yes → response written, done             │
                No  → pass to next resolver ─────────────┘
                      │
               still unresolved?
                      │
                      ▼
              Servlet container /error
                      │
                      ▼
          BasicErrorController (Spring Boot)
                      │
                      ▼
          DefaultErrorAttributes
          → builds { timestamp, status, error, message, path }
                      │
                      ▼
                Error JSON → Client
```

---

### Choosing the Right Mechanism — Decision Guide

```
Need to handle an exception?
        │
        ├─ Is it a Spring MVC built-in exception (wrong method, missing param)?
        │       → DefaultHandlerExceptionResolver handles it automatically
        │         Override with @ExceptionHandler only if you need a custom body
        │
        ├─ Simple exception, just need a status code, no JSON body?
        │       → @ResponseStatus on the exception class
        │         or throw ResponseStatusException(status, "message")
        │
        ├─ Need full control: custom JSON body, headers, logging?
        │       → @ExceptionHandler in @RestControllerAdvice  ← most common
        │
        ├─ Need to handle exceptions from non-@Controller handlers?
        │       → Implement HandlerExceptionResolver directly
        │
        └─ Need to customise the fallback /error response?
                → Extend DefaultErrorAttributes
```

---

## Exception Lifecycle — From Controller Throw to Client Response

### The Three Scenarios

There are three fundamentally different ways an exception can be thrown in a controller, and each one produces a **different outcome**:

| Scenario | What you throw | Who handles it | Response |
|----------|---------------|----------------|----------|
| **A** | Raw `RuntimeException` or custom exception with no special config | Spring's fallback (`DefaultErrorAttributes`) | `500 Internal Server Error` |
| **B** | Exception annotated with `@ResponseStatus` or `ResponseStatusException` | `ResponseStatusExceptionResolver` | Your chosen status code, but HTML body |
| **C** | Exception caught by an `@ExceptionHandler` / `@RestControllerAdvice` | `ExceptionHandlerExceptionResolver` | Fully custom JSON body + status |

---

### Scenario A — Raw Exception (Spring Handles It — You Get 500)

This is the most misunderstood case. A developer creates a custom exception with a status code and message in it, throws it, and **expects Spring to pick up the status code automatically**. It does not.

```java
// ─── What the developer writes ────────────────────────────────────────────

// A custom exception that "carries" a status code
public class UserNotFoundException extends RuntimeException {
    private final HttpStatus status;

    public UserNotFoundException(Long id) {
        super("User with id " + id + " was not found");
        this.status = HttpStatus.NOT_FOUND;   // ← developer thinks Spring will use this
    }

    public HttpStatus getStatus() { return status; }
}

// Controller — throws the exception and does nothing else
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        throw new UserNotFoundException(id);   // ← developer expects 404
    }
}
```

**What actually happens — step by step:**

```
GET /users/99
     │
     ▼
DispatcherServlet.doDispatch()
     │
     │  invokes UserController.getUser(99)
     │
     │  ← UserNotFoundException is thrown
     │
     ▼
DispatcherServlet.processHandlerException()
     │
     ▼
HandlerExceptionResolverComposite.resolveException()
     │
     ├─► [1] ExceptionHandlerExceptionResolver
     │         → searches for @ExceptionHandler(UserNotFoundException.class)
     │         → NONE found anywhere
     │         → returns null  (cannot handle)
     │
     ├─► [2] ResponseStatusExceptionResolver
     │         → checks if UserNotFoundException has @ResponseStatus annotation
     │         → NO annotation on the class
     │         → returns null  (cannot handle)
     │
     ├─► [3] DefaultHandlerExceptionResolver
     │         → checks if it is a known Spring MVC exception
     │         → UserNotFoundException is NOT a Spring MVC type
     │         → returns null  (cannot handle)
     │
     │  ALL resolvers returned null — exception is UNRESOLVED
     │
     ▼
Exception is re-thrown to the Servlet container (Tomcat)
     │
     │  Tomcat sets request attribute: javax.servlet.error.status_code = 500
     │  Tomcat forwards internally to /error
     │
     ▼
Spring Boot BasicErrorController.error(request)
     │
     ▼
DefaultErrorAttributes.getErrorAttributes(...)
     │  reads attributes set by Tomcat on the request
     │  NOTE: it does NOT read UserNotFoundException.getStatus()
     │  it only reads the Tomcat-set status code (500)
     ▼
Response sent to client:
{
  "timestamp": "2026-06-02T10:00:00.000+00:00",
  "status": 500,                          ← always 500, ignores your field
  "error": "Internal Server Error",
  "message": "",                          ← message hidden by default
  "path": "/users/99"
}
```

> **Why Spring ignores `exception.getStatus()`:** The resolver chain only knows about the Java exception *type* and its *annotations*. There is no Spring contract that says "read a `getStatus()` method on the exception". Spring has no idea your field exists — it only uses `@ResponseStatus` annotation or explicit `@ExceptionHandler` methods to determine the HTTP status.

---

### Scenario B — `@ResponseStatus` Annotation (Spring Reads the Status, but Body is HTML)

```java
// Add @ResponseStatus — now ResponseStatusExceptionResolver CAN handle it
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "User not found")
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("User with id " + id + " was not found");
    }
}
```

**What happens now:**

```
GET /users/99
     │
     ▼
HandlerExceptionResolverComposite
     │
     ├─► [1] ExceptionHandlerExceptionResolver  → no @ExceptionHandler → null
     │
     ├─► [2] ResponseStatusExceptionResolver
     │         → finds @ResponseStatus(NOT_FOUND, reason="User not found")
     │         → calls response.sendError(404, "User not found")
     │         → returns non-null ModelAndView  ← HANDLED
     │
     ▼
Servlet container receives sendError(404)
     │  forwards to /error
     ▼
BasicErrorController → DefaultErrorAttributes
     │
     ▼
{
  "status": 404,                    ← correct now!
  "error": "Not Found",
  "message": "User not found",      ← from @ResponseStatus reason
  "path": "/users/99"
}
```

**Problem:** This still goes through `sendError()` → `BasicErrorController` → `DefaultErrorAttributes`. The response body is Spring Boot's standard `/error` JSON shape. You cannot add custom fields, change the structure, or return your own `ErrorResponse` POJO. Also `reason` only appears if `server.error.include-message=always` is set.

---

### Scenario C — `@ExceptionHandler` (Full Control — The Right Way)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Not Found", ex.getMessage()));
    }
}
```

**What happens:**

```
GET /users/99
     │
     ▼
HandlerExceptionResolverComposite
     │
     ├─► [1] ExceptionHandlerExceptionResolver
     │         → finds @ExceptionHandler(UserNotFoundException.class)
     │            in GlobalExceptionHandler (@RestControllerAdvice)
     │         → invokes handleUserNotFound(ex)
     │         → writes ResponseEntity(404, ErrorResponse) directly to response
     │         → returns non-null ModelAndView  ← HANDLED
     │
     │  ← processing stops here, resolvers 2 and 3 are never called
     │  ← BasicErrorController is NEVER involved
     │  ← DefaultErrorAttributes is NEVER called
     │
     ▼
{
  "error": "Not Found",
  "message": "User with id 99 was not found"
}
                                    ← YOUR structure, YOUR fields, YOUR status
```

---

### Side-by-Side Comparison of All Three Scenarios

```
┌────────────────────────┬──────────────────────┬──────────────────────┬───────────────────────┐
│                        │  Scenario A           │  Scenario B          │  Scenario C           │
│                        │  Raw exception        │  @ResponseStatus     │  @ExceptionHandler    │
├────────────────────────┼──────────────────────┼──────────────────────┼───────────────────────┤
│ HTTP Status            │ Always 500           │ Annotation value     │ You decide            │
│ Response Body          │ Spring's /error JSON │ Spring's /error JSON │ Your POJO/structure   │
│ Custom fields in body  │ No                  │ No                   │ Yes                   │
│ BasicErrorController   │ Involved             │ Involved             │ NOT involved          │
│ DefaultErrorAttributes │ Involved             │ Involved             │ NOT involved          │
│ Logging control        │ None                │ None                 │ Full (you log)        │
│ Response Headers       │ No control           │ No control           │ Full control          │
│ Best for               │ Never (accidental)  │ Simple status-only   │ REST APIs (always)    │
└────────────────────────┴──────────────────────┴──────────────────────┴───────────────────────┘
```

---

### Scenario D — Handling Directly in the Controller (Avoid This Pattern)

You can catch the exception inside the controller itself and return a `ResponseEntity` directly. This works, but is **not recommended** — it duplicates error logic across controllers and defeats the purpose of a centralised `@RestControllerAdvice`.

```java
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException ex) {
            // Handled locally — no resolver is involved at all
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity
                    .internalServerError()
                    .body(new ErrorResponse("Internal Server Error",
                            "An unexpected error occurred"));
        }
    }
}
```

**Flow when catching locally:**

```
GET /users/99
     │
     ▼
DispatcherServlet → UserController.getUser(99)
     │
     │  UserNotFoundException is thrown inside userService
     │  caught by try/catch in controller
     │  ResponseEntity(404, ErrorResponse) returned normally
     │
     ▼
DispatcherServlet writes the ResponseEntity to the HTTP response
     │
     │  ← HandlerExceptionResolverComposite is NEVER called
     │  ← BasicErrorController is NEVER called
     │  ← DefaultErrorAttributes is NEVER called
     │
     ▼
{ "error": "Not Found", "message": "User with id 99 was not found" }
```

> **Why avoid this:** Every controller method needs its own try/catch. Error logic is scattered. A `@RestControllerAdvice` centralises everything in one place, making it far easier to maintain consistent error responses across all endpoints.

---

### Complete Working Example — The Recommended Pattern

```java
// ─── 1. Custom exception (no @ResponseStatus annotation needed) ───────────

public class UserNotFoundException extends RuntimeException {
    private final Long userId;

    public UserNotFoundException(Long userId) {
        super("User with id " + userId + " was not found");
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
}

// ─── 2. Standard error response POJO ─────────────────────────────────────

public record ErrorResponse(
        String error,
        String message,
        String timestamp,
        String path
) {
    public static ErrorResponse of(String error, String message, String path) {
        return new ErrorResponse(
                error,
                message,
                Instant.now().toString(),
                path
        );
    }
}

// ─── 3. Service layer throws the exception ────────────────────────────────

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        //                         ↑ thrown here, propagates up
    }
}

// ─── 4. Controller — clean, no try/catch ─────────────────────────────────

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
        // If findById throws UserNotFoundException it propagates to DispatcherServlet
        // which passes it to HandlerExceptionResolverComposite
        // which calls ExceptionHandlerExceptionResolver
        // which finds the @ExceptionHandler below
    }
}

// ─── 5. Centralised global exception handler ──────────────────────────────

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handles UserNotFoundException → 404
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {

        log.warn("User not found: id={}", ex.getUserId());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("Not Found", ex.getMessage(),
                        request.getRequestURI()));
    }

    // Handles validation failures → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of("Validation Failed", details,
                        request.getRequestURI()));
    }

    // Catch-all → 500 (never exposes internal details)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at {}", request.getRequestURI(), ex);

        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of("Internal Server Error",
                        "An unexpected error occurred. Please try again later.",
                        request.getRequestURI()));
    }
}
```

**Exact call stack for `GET /users/99`:**

```
1. HTTP GET /users/99 arrives at Tomcat
2. Tomcat → DispatcherServlet.service()
3. DispatcherServlet.doDispatch()
4.   HandlerMapping finds UserController.getUser(Long)
5.   HandlerAdapter.handle() invokes getUser(99)
6.     UserService.findById(99)
7.       UserRepository.findById(99) → Optional.empty()
8.       throws UserNotFoundException("User with id 99 was not found")
9.     propagates up through UserService
10.   propagates up through UserController.getUser()
11.   HandlerAdapter catches it, rethrows
12. DispatcherServlet.processHandlerException(request, response, handler, ex)
13.   HandlerExceptionResolverComposite.resolveException(...)
14.     ExceptionHandlerExceptionResolver.resolveException(...)
15.       searches GlobalExceptionHandler for @ExceptionHandler(UserNotFoundException)
16.       FOUND → invokes handleUserNotFound(ex, request)
17.       writes 404 + ErrorResponse JSON to HttpServletResponse
18.       returns new ModelAndView()   ← non-null = handled
19.   returns ModelAndView to DispatcherServlet ← stops here
20. DispatcherServlet sees non-null ModelAndView, no further processing
21. Tomcat sends the already-written response to client

Response:
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": "Not Found",
  "message": "User with id 99 was not found",
  "timestamp": "2026-06-02T10:00:00Z",
  "path": "/users/99"
}
```

---

## `ExceptionHandlerExceptionResolver` — Deep Dive

`ExceptionHandlerExceptionResolver` is the **first and most powerful** resolver in the chain. It is the engine that drives both `@ExceptionHandler` (controller-local) and `@ControllerAdvice` (global) exception handling.

### How it searches for a handler — internally

```
ExceptionHandlerExceptionResolver.resolveException(request, response, handler, ex)
         │
         ▼
  Is the handler a HandlerMethod? (i.e., a @Controller method)
         │
         ├─ YES
         │    │
         │    ▼
         │  [Step 1] Look inside the SAME @Controller class
         │             → find @ExceptionHandler methods
         │             → do any of them match the exception type?
         │                 YES → invoke it, write response, return non-null MAV
         │                 NO  → continue to step 2
         │
         │    ▼
         │  [Step 2] Look in all @ControllerAdvice beans
         │             (ordered by @Order / Ordered interface)
         │             → find @ExceptionHandler methods
         │             → do any of them match the exception type?
         │                 YES → invoke it, write response, return non-null MAV
         │                 NO  → return null (resolver gives up)
         │
         └─ NO → return null (not a @Controller handler)
```

---

### Part 1 — `@ExceptionHandler` (Controller-Local)

#### Basic Usage

`@ExceptionHandler` placed **inside the same `@Controller`** class handles exceptions thrown **only by methods in that controller**. Other controllers are not affected.

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
        // If OrderNotFoundException is thrown, the handler below intercepts it
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody @Valid OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(request));
        // If InvalidOrderException is thrown, the handler below intercepts it
    }

    // ─── Local exception handler — only active for THIS controller ────────
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Order Not Found", ex.getMessage()));
    }
}
```

#### Handling Multiple Exception Types — Aggregated `@ExceptionHandler`

A single `@ExceptionHandler` method can handle **multiple exception types** by listing them in the annotation's value array. This avoids duplicating identical error-response logic for related exception classes.

```java
@RestController
@RequestMapping("/payments")
public class PaymentController {

    // ─── Handles BOTH exception types with one method ─────────────────────
    @ExceptionHandler({CustomPaymentException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handlePaymentErrors(Exception ex) {
        //                                                    ↑ use base Exception
        //                                                      to accept both types
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Payment Error", ex.getMessage()));
    }

    // ─── Or, type the parameter to the common supertype ───────────────────
    @ExceptionHandler({InsufficientFundsException.class, CardExpiredException.class,
                       CardDeclinedException.class})
    public ResponseEntity<ErrorResponse> handleCardErrors(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("Card Error", ex.getMessage()));
    }
}
```

> **Rule:** When listing multiple types, the method parameter type must be a common supertype (or `Exception` / `Throwable`) that is assignment-compatible with all listed exception classes. You cannot type the parameter as `CustomPaymentException` and also list `IllegalArgumentException` — they share no specific common type other than `Exception`.

#### Supported Method Parameters

`@ExceptionHandler` methods support rich **parameter injection** — Spring resolves them automatically from the current request context. You declare only what you need.

| Parameter Type | What Spring injects |
|----------------|---------------------|
| `SomeException` (the exception type) | The thrown exception itself |
| `HttpServletRequest` | The raw servlet request |
| `HttpServletResponse` | The raw servlet response |
| `WebRequest` / `NativeWebRequest` | Spring's request abstraction |
| `HttpSession` | The current HTTP session |
| `Principal` | The authenticated principal |
| `Authentication` | Spring Security authentication |
| `Locale` | The resolved locale for the request |
| `TimeZone` / `ZoneId` | The resolved time zone |
| `OutputStream` / `Writer` | To write the response body directly |
| `Model` | For MVC view rendering (non-REST) |

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex,           // the thrown exception
        HttpServletRequest request,         // to read URI, headers, IP
        HttpServletResponse response,       // to set headers on the response
        Authentication authentication,      // who is making the request
        Locale locale) {                    // for i18n error messages

    log.warn("Access denied: user='{}', uri='{}', ip='{}'",
            authentication.getName(),
            request.getRequestURI(),
            request.getRemoteAddr());

    String message = messageSource.getMessage("error.access.denied", null, locale);

    return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("Forbidden", message));
}
```

#### Using `HttpServletResponse.sendError()` directly

Instead of returning a `ResponseEntity`, you can write directly to the `HttpServletResponse`. This is useful when you want to delegate to the container's error page (and ultimately `DefaultErrorAttributes`) while still influencing the status code.

```java
@ExceptionHandler(MaintenanceModeException.class)
public void handleMaintenance(
        MaintenanceModeException ex,
        HttpServletResponse response) throws IOException {

    // Directly write the error — no ResponseEntity needed
    // This goes through the Servlet container's /error path
    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                       "Service is under maintenance. Try again later.");

    // OR: write a full JSON response directly
    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(
        """
        {"error": "Service Unavailable", "message": "%s"}
        """.formatted(ex.getMessage())
    );
}
```

> **`sendError()` vs `ResponseEntity`:** `sendError()` triggers the Servlet container to forward to `/error` (so `DefaultErrorAttributes` and `BasicErrorController` get involved). Returning `ResponseEntity` writes **directly** to the response — the container's error pipeline is bypassed entirely. Prefer `ResponseEntity` for REST APIs to maintain full control.

---

### Part 2 — `@ControllerAdvice` (Global Exception Handling)

`@ControllerAdvice` is a specialisation of `@Component` that marks a class as a **cross-cutting concern** for controllers. When combined with `@ExceptionHandler`, it provides **application-wide** exception handling.

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody` — the response body is automatically serialised to JSON (no need to annotate each method with `@ResponseBody`).

#### Priority Rules

```
Exception thrown by @Controller method
         │
         ▼
ExceptionHandlerExceptionResolver searches in this order:

  ┌─────────────────────────────────────────────────────────────────┐
  │  PRIORITY 1 (Highest)                                           │
  │  @ExceptionHandler inside the SAME @Controller class           │
  │  Scope: only this controller's methods                          │
  └─────────────────────────────────────────────────────────────────┘
                         │ not found? ↓
  ┌─────────────────────────────────────────────────────────────────┐
  │  PRIORITY 2                                                     │
  │  @ExceptionHandler inside @ControllerAdvice beans              │
  │  Scope: all controllers (or scoped subset)                      │
  │  Sub-priority: most-specific exception type wins               │
  └─────────────────────────────────────────────────────────────────┘
                         │ not found? ↓
  resolver returns null → next resolver in the chain
```

**Concrete example:**

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        throw new UserNotFoundException(id);
    }

    // ── Local handler: handles UserNotFoundException FROM THIS CONTROLLER ─
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLocalNotFound(UserNotFoundException ex) {
        // This runs — NOT the one in GlobalExceptionHandler
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Local Handler: User Not Found", ex.getMessage()));
    }
}

@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        throw new UserNotFoundException(id); // same exception type
    }
    // No local @ExceptionHandler here
    // → GlobalExceptionHandler.handleNotFound() will be used instead
}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex) {
        // Runs for ProductController, but NOT for UserController
        // (UserController has its own local handler for this type)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Global Handler: Not Found", ex.getMessage()));
    }
}
```

#### Exception Type Specificity — Most-Specific Match Wins

When multiple `@ExceptionHandler` methods in a `@ControllerAdvice` could handle an exception, Spring picks the **most-specific** one — the handler whose declared exception type is **closest in the inheritance hierarchy** to the actual thrown exception.

```
CustomException extends RuntimeException extends Exception
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handler A — handles CustomException (exact match for CustomException)
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustom(CustomException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("Custom Error", ex.getMessage()));
    }

    // Handler B — handles RuntimeException (broader — catches all RuntimeExceptions)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse("Runtime Error", ex.getMessage()));
    }

    // Handler C — broadest catch-all
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse("Unexpected Error", "An error occurred"));
    }
}
```

**Resolution for each thrown exception type:**

```
throw new CustomException(...)
      → Handler A wins  (CustomException == CustomException, exact match)

throw new IllegalArgumentException(...)   (extends RuntimeException)
      → Handler B wins  (RuntimeException is the closest match)
      → Handler A does NOT match (IllegalArgumentException is not a CustomException)

throw new IOException(...)               (extends Exception, not RuntimeException)
      → Handler C wins  (only Exception matches IOException)
```

```
Exception Hierarchy:
  Exception
    │
    ├── RuntimeException          → Handler B matches
    │     │
    │     ├── CustomException     → Handler A matches (most specific, wins over B)
    │     │
    │     └── IllegalArgumentException → Handler B matches
    │
    └── IOException               → Handler C matches (B doesn't cover it)
```

#### Scoping `@ControllerAdvice` — Applying to a Subset of Controllers

By default `@ControllerAdvice` applies to **all controllers**. You can narrow the scope:

```java
// ── By base package ───────────────────────────────────────────────────────
@RestControllerAdvice(basePackages = "com.example.payments")
public class PaymentExceptionHandler {
    // Only active for controllers in com.example.payments package
}

// ── By annotation ─────────────────────────────────────────────────────────
@ControllerAdvice(annotations = RestController.class)
public class RestOnlyExceptionHandler {
    // Only active for @RestController classes (not plain @Controller)
}

// ── By specific controller classes ────────────────────────────────────────
@ControllerAdvice(assignableTypes = {OrderController.class, PaymentController.class})
public class OrderPaymentExceptionHandler {
    // Only active for OrderController and PaymentController
}
```

#### Ordering Multiple `@ControllerAdvice` Beans

When you have more than one `@ControllerAdvice`, use `@Order` to define which one is searched first:

```java
@RestControllerAdvice
@Order(1)  // searched first — highest priority
public class DomainExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) { ... }
}

@RestControllerAdvice
@Order(2)  // searched second
public class InfrastructureExceptionHandler {

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDb(DatabaseException ex) { ... }
}

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)  // searched last — catch-all
public class GlobalFallbackHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) { ... }
}
```

> **Rule:** If two `@ControllerAdvice` beans both have a handler for the same exception type, the one with the **lower `@Order` value** (higher priority) wins. If no `@Order` is set, the order is undefined and should not be relied upon.

#### Complete Real-World Example

```java
// ─── Exception hierarchy ──────────────────────────────────────────────────

public class AppException extends RuntimeException {
    private final HttpStatus status;
    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
}

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " not found", HttpStatus.NOT_FOUND);
    }
}

public class ConflictException extends AppException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

// ─── Controller ───────────────────────────────────────────────────────────

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody @Valid UserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req));
    }

    // ── Local handler: only for UserController, only for UserConflictException ──
    @ExceptionHandler(UserConflictException.class)
    public ResponseEntity<ErrorResponse> handleUserConflict(UserConflictException ex,
                                                             HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("User Conflict",
                        ex.getMessage(), request.getRequestURI()));
    }
}

// ─── Global handler ───────────────────────────────────────────────────────

@RestControllerAdvice
@Order(1)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handles ResourceNotFoundException (extends AppException)
    // More specific than AppException handler below → wins
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Not Found", ex.getMessage(),
                        request.getRequestURI()));
    }

    // Handles all other AppExceptions (ConflictException, etc.)
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex,
            HttpServletRequest request) {
        return ResponseEntity
                .status(ex.getStatus())  // uses the status from the exception
                .body(new ErrorResponse(ex.getStatus().getReasonPhrase(),
                        ex.getMessage(), request.getRequestURI()));
    }

    // Handles validation failures — aggregated with binding errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Validation Failed", details,
                        request.getRequestURI()));
    }

    // Catch-all — must be least specific (Exception.class)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Internal Server Error",
                        "An unexpected error occurred", request.getRequestURI()));
    }
}
```

**Resolution table for this setup:**

| Exception thrown | From controller | Handler that runs |
|-----------------|-----------------|-------------------|
| `UserConflictException` | `UserController` | `UserController.handleUserConflict()` (local, priority 1) |
| `UserConflictException` | `ProductController` | `GlobalExceptionHandler.handleAppException()` (no local handler there) |
| `ResourceNotFoundException` | Any controller | `GlobalExceptionHandler.handleNotFound()` (most specific in global) |
| `ConflictException` | Any controller | `GlobalExceptionHandler.handleAppException()` (AppException handler matches) |
| `MethodArgumentNotValidException` | Any controller | `GlobalExceptionHandler.handleValidation()` |
| `NullPointerException` | Any controller | `GlobalExceptionHandler.handleAll()` (catch-all) |



---

## `ResponseStatusExceptionResolver` — Deep Dive

### What it handles

`ResponseStatusExceptionResolver` is the **second resolver** in the chain. It activates only when `ExceptionHandlerExceptionResolver` returns `null` (i.e., no `@ExceptionHandler` matched). It handles two cases:

1. Any exception class annotated with `@ResponseStatus`
2. `ResponseStatusException` thrown directly (no annotation needed)

```
Controller throws CustomException
         │
         ▼
[1] ExceptionHandlerExceptionResolver  → no @ExceptionHandler found → null
         │
         ▼
[2] ResponseStatusExceptionResolver
         │ checks: does the exception class have @ResponseStatus?
         │   YES → reads value (status) and reason
         │   NO  → checks: is it a ResponseStatusException?
         │     YES → reads status and reason from the exception itself
         │     NO  → returns null, passes to DefaultHandlerExceptionResolver
```

---

### Case 1 — `@ResponseStatus` on the exception class (reason vs message priority)

```java
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid input data")
public class CustomException extends RuntimeException {

    public CustomException(String message) {
        super(message);   // ← this is the exception message
    }
}
```

When `ResponseStatusExceptionResolver` processes this:

```java
// Simplified from Spring source — ResponseStatusExceptionResolver
protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus,
                                              HttpServletRequest request,
                                              HttpServletResponse response,
                                              Object handler,
                                              Exception ex) throws Exception {

    int statusCode = responseStatus.code().value();
    String reason  = responseStatus.reason();  // from the annotation

    if (!StringUtils.hasLength(reason)) {
        // No reason set → use sendError with status only (no message)
        response.sendError(statusCode);
    } else {
        // Reason IS set → use the annotation reason, IGNORE ex.getMessage()
        String resolvedReason = (this.messageSource != null
                ? this.messageSource.getMessage(reason, null,
                        reason, LocaleContextHolder.getLocale())
                : reason);
        response.sendError(statusCode, resolvedReason);
    }
    return new ModelAndView();
}
```

**Priority rule: `reason` beats `exception message`**

```
@ResponseStatus(value = BAD_REQUEST, reason = "Invalid input data")
throw new CustomException("This is the constructor message")

      ↓ what goes into the response

  "message": "Invalid input data"   ← annotation reason wins
                                       ex.getMessage() is completely ignored

If reason = "" (empty):
  response.sendError(400)            ← no message at all, just the status code
```

```
┌──────────────────────┬────────────────────────────────────────────────┐
│  @ResponseStatus     │  What appears in the response                  │
├──────────────────────┼────────────────────────────────────────────────┤
│ reason = "X"         │ "message": "X"  (reason takes priority)        │
│ reason = ""          │ no message field (sendError with status only)  │
│ no reason attribute  │ no message field                                │
└──────────────────────┴────────────────────────────────────────────────┘
```

---

### Case 2 — `@ResponseStatus` + `@ExceptionHandler` returning `ResponseEntity`: what wins?

This is the subtle and often misunderstood case.

**Setup:**

```java
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid input data")
public class CustomException extends RuntimeException {
    public CustomException(String msg) { super(msg); }
}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustom(CustomException ex) {
        // Developer expects 422 + custom body to be the final response
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("Custom Error", ex.getMessage()));
    }
}
```

**What you expect:** `422` with `{ "error": "Custom Error", ... }`

**What actually happens:**

```
Exception thrown
     │
     ▼
[1] ExceptionHandlerExceptionResolver
     │  finds handleCustom()  → invokes it
     │  handleCustom() returns ResponseEntity(422, body)
     │
     │  BUT before writing the response, ExceptionHandlerExceptionResolver
     │  calls ServletInvocableHandlerMethod.invokeAndHandle()
     │  which internally calls:
     │
     │      setResponseStatus(webRequest)   ← THIS is the key call
     │
     ▼
ServletInvocableHandlerMethod.setResponseStatus(webRequest):
     │
     │  // Simplified from Spring source
     │  if (this.responseStatus != null) {
     │      String reason = this.responseStatus.reason();
     │      if (!StringUtils.hasLength(reason)) {
     │          response.setStatus(this.responseStatus.value().value());
     │      } else {
     │          response.sendError(
     │              this.responseStatus.value().value(),
     │              reason    // ← "Invalid input data" from @ResponseStatus
     │          );
     │      }
     │  }
     │
     │  Since reason = "Invalid input data" (non-empty):
     │  → response.sendError(400, "Invalid input data") is called
     │  → This commits the response with 400
     │  → The ResponseEntity(422, body) you returned is OVERRIDDEN
     │
     ▼
Final response:
{
  "status":  400,                      ← from @ResponseStatus, NOT your 422
  "error":   "Bad Request",
  "message": "Invalid input data",     ← from @ResponseStatus reason
  "path":    "/..."
}
```

**The key internal method — `ServletInvocableHandlerMethod.setResponseStatus()`:**

```java
// org.springframework.web.servlet.mvc.method.annotation
// ServletInvocableHandlerMethod (simplified from Spring source)

private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
    HttpStatus status = getResponseStatus();     // reads @ResponseStatus on the handler method
    if (status == null) {
        return;
    }

    HttpServletResponse response = webRequest.getResponse();
    String reason = getResponseStatusReason();   // reads reason attribute

    if (StringUtils.hasText(reason)) {
        // reason is present → sendError overwrites everything
        response.sendError(status.value(), reason);
    } else {
        // no reason → just set the status code (body from ResponseEntity survives)
        response.setStatus(status.value());
    }

    // Mark response as handled so view resolution is skipped
    webRequest.getRequest().setAttribute(
        View.RESPONSE_STATUS_ATTRIBUTE, status);
}
```

> **This `setResponseStatus` is called on the `@ExceptionHandler` method itself, not on the exception class.** Spring reads `@ResponseStatus` from the exception class and copies it onto the handler method's metadata. So even though `ResponseStatusExceptionResolver` is never called (because `ExceptionHandlerExceptionResolver` handled it first), the `@ResponseStatus` annotation still takes effect through this internal hook.

**Visual summary:**

```
┌──────────────────────────────────────────────────────────────────────────┐
│  @ResponseStatus(BAD_REQUEST, reason="Invalid input data")               │
│  + @ExceptionHandler returning ResponseEntity(UNPROCESSABLE, body)       │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ExceptionHandlerExceptionResolver invokes your @ExceptionHandler        │
│  method → returns ResponseEntity(422, customBody)                        │
│                                                                          │
│  THEN  ServletInvocableHandlerMethod.setResponseStatus() is called:      │
│    reason is non-empty → response.sendError(400, "Invalid input data")   │
│    → commits response with 400, discards your ResponseEntity body        │
│                                                                          │
│  Final: 400 + Spring's /error JSON  (your body is LOST)                 │
└──────────────────────────────────────────────────────────────────────────┘
```

**Only when `reason` is empty does your `ResponseEntity` body survive:**

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)   // NO reason attribute
public class CustomException extends RuntimeException { ... }

// Now setResponseStatus() calls response.setStatus(400) — NOT sendError()
// → response is NOT committed, your ResponseEntity body IS written
// Final: 400 + your custom JSON body ✓
```

---

### Case 3 — `@ResponseStatus` + `@ExceptionHandler` + `response.sendError()` in the method body

**Setup — the dangerous combination:**

```java
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid request sent")
public class CustomException extends RuntimeException {
    public CustomException(String msg) { super(msg); }
}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public void handleCustom(CustomException ex,
                             HttpServletResponse response) throws IOException {
        // Developer manually calls sendError inside the handler method
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "you're not authorized");
    }
}
```

**What actually happens — step by step with internals:**

```
Step 1: ExceptionHandlerExceptionResolver finds handleCustom()
        → invokes it
        → inside the method: response.sendError(403, "you're not authorized")

Step 2: response.sendError(403, ...) is called
        → Tomcat marks the response as ERROR committed
        → sets status=403, stores error message in request attributes
        → the response output stream is now in ERROR state

Step 3: handleCustom() returns (void method, no ResponseEntity)
        → ExceptionHandlerExceptionResolver calls
          ServletInvocableHandlerMethod.setResponseStatus(webRequest)

Step 4: setResponseStatus() reads @ResponseStatus(BAD_REQUEST, "Invalid request sent")
        → reason = "Invalid request sent"  (non-empty)
        → tries to call response.sendError(400, "Invalid request sent")
        → BUT the response is already committed from Step 2!

Step 5: Tomcat throws:
        java.lang.IllegalStateException: Cannot call sendError() after the response
        has been committed

Step 6: This IllegalStateException propagates up to DispatcherServlet
        → DispatcherServlet has no handler for it
        → passes to the Servlet container

Step 7: Servlet container catches the IllegalStateException
        → sets status = 500
        → forwards to /error

Step 8: BasicErrorController + DefaultErrorAttributes produce:
{
  "status":  500,
  "error":   "Internal Server Error",
  "message": "",
  "path":    "/..."
}
```

**The internal code path that causes this:**

```java
// Tomcat's Response implementation (simplified)
public void sendError(int status, String message) throws IOException {
    if (isCommitted()) {
        // Response already committed — cannot change it
        throw new IllegalStateException(
            "Cannot call sendError() after the response has been committed"
        );
    }
    // ... set error state
}

// Spring's setResponseStatus — called AFTER your handler method returns
private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
    // reads @ResponseStatus(BAD_REQUEST, "Invalid request sent")
    String reason = getResponseStatusReason();  // = "Invalid request sent"
    if (StringUtils.hasText(reason)) {
        // Calls sendError on an ALREADY COMMITTED response → IllegalStateException
        webRequest.getResponse().sendError(400, reason);
    }
}
```

**Timeline of the failure:**

```
Your handler body:    response.sendError(403, ...)  → response COMMITTED (403)
                                                           │
Spring internals:     setResponseStatus() → sendError(400) → IllegalStateException
                                                           │
DispatcherServlet:    unhandled exception              → 500 to client
```

---

### Case 4 — The Correct Way: Use Both `@ResponseStatus` + `@ExceptionHandler` with an Empty Body

If you want `@ResponseStatus` to fully control the response status and message, declare the `@ExceptionHandler` method but **leave the body empty**. Let Spring's internal `setResponseStatus()` do all the work.

```java
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid request sent")
public class CustomException extends RuntimeException {
    public CustomException(String msg) { super(msg); }
}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public void handleCustom(CustomException ex) {
        // ← intentionally empty body
        // Do NOT call response.sendError() here
        // Do NOT return a ResponseEntity here
        //
        // Spring will call setResponseStatus() after this method returns,
        // which calls response.sendError(400, "Invalid request sent")
        // producing the @ResponseStatus values in the response
    }
}
```

**What happens:**

```
handleCustom() returns (empty body, void)
     │
     ▼
setResponseStatus() is called
     │  reason = "Invalid request sent"  (non-empty)
     │  response.sendError(400, "Invalid request sent")
     │  response is committed cleanly — NO IllegalStateException
     ▼
Servlet container forwards to /error
     ▼
BasicErrorController + DefaultErrorAttributes:
{
  "status":  400,
  "error":   "Bad Request",
  "message": "Invalid request sent",
  "path":    "/..."
}
```

> **Why even have an empty `@ExceptionHandler` if `@ResponseStatus` alone would work?** Because the local controller `@ExceptionHandler` has higher priority than `ResponseStatusExceptionResolver`. If you need the exception to be caught at the controller level first (e.g., for logging, MDC context, metrics) but still want `@ResponseStatus` to set the final response, an empty handler is the correct pattern.

```java
// Practical empty handler — logs but lets @ResponseStatus control the response
@ExceptionHandler(CustomException.class)
public void handleCustom(CustomException ex, HttpServletRequest request) {
    // Only side effects allowed here — no response writing
    log.warn("CustomException at {}: {}", request.getRequestURI(), ex.getMessage());
    MDC.put("errorType", "CUSTOM");
    // Spring handles the actual response via @ResponseStatus
}
```

---

### Summary — All `@ResponseStatus` + `@ExceptionHandler` Combinations

```
┌────────────────────────────────┬───────────────────────────────────────────────────┐
│  Configuration                 │  Final response                                   │
├────────────────────────────────┼───────────────────────────────────────────────────┤
│ @ResponseStatus only           │ Status+reason from annotation via                 │
│ (no @ExceptionHandler)         │ ResponseStatusExceptionResolver → /error JSON     │
├────────────────────────────────┼───────────────────────────────────────────────────┤
│ @ExceptionHandler only         │ Fully your ResponseEntity — status, body,         │
│ returning ResponseEntity       │ headers. @ResponseStatus NOT involved.            │
├────────────────────────────────┼───────────────────────────────────────────────────┤
│ Both, handler returns          │ @ResponseStatus reason wins for status+message    │
│ ResponseEntity + reason set    │ Your ResponseEntity body is LOST. → /error JSON  │
├────────────────────────────────┼───────────────────────────────────────────────────┤
│ Both, handler returns          │ Your ResponseEntity status wins                   │
│ ResponseEntity, reason=""      │ Your ResponseEntity body IS written ✓             │
├────────────────────────────────┼───────────────────────────────────────────────────┤
│ Both, handler calls            │ IllegalStateException → 500 Internal Server Error │
│ response.sendError() manually  │ NEVER do this                                     │
├────────────────────────────────┼───────────────────────────────────────────────────┤
│ Both, handler body is EMPTY    │ @ResponseStatus controls status+reason cleanly    │
│ (void, no response writes)     │ Good for logging-only handlers ✓                 │
└────────────────────────────────┴───────────────────────────────────────────────────┘
```



---

## `DefaultHandlerExceptionResolver` — Deep Dive

### What it handles

`DefaultHandlerExceptionResolver` is the **third and final resolver** in the Spring MVC chain. It activates only when both `ExceptionHandlerExceptionResolver` and `ResponseStatusExceptionResolver` return `null`. Its job is to translate well-known **Spring MVC infrastructure exceptions** into sensible HTTP status codes — automatically, without any `@ExceptionHandler` method in your code.

```
Controller throws exception
         │
         ▼
[1] ExceptionHandlerExceptionResolver  → no @ExceptionHandler matched → null
         │
         ▼
[2] ResponseStatusExceptionResolver    → no @ResponseStatus / not ResponseStatusException → null
         │
         ▼
[3] DefaultHandlerExceptionResolver
         │  Is this a known Spring MVC exception type?
         │    YES → set the appropriate HTTP status, return non-null ModelAndView
         │    NO  → return null (exception goes unhandled, Servlet container gets it → 500)
```

---

### The Problem it Solves

Without `DefaultHandlerExceptionResolver`, Spring would have to return `500 Internal Server Error` for every infrastructure-level mistake — a client sending `POST` to a `GET` endpoint, an unsupported `Content-Type`, a missing required request parameter. These are **client errors (4xx)**, not server errors. `DefaultHandlerExceptionResolver` is what makes Spring automatically return the correct status code for these situations without any developer intervention.

```
Client sends:   DELETE /users/99
Server defines: @GetMapping("/users/{id}")

Without DefaultHandlerExceptionResolver:
  → HttpRequestMethodNotSupportedException thrown internally
  → no resolver handles it
  → 500 Internal Server Error ← wrong, misleading

With DefaultHandlerExceptionResolver:
  → HttpRequestMethodNotSupportedException thrown internally
  → DefaultHandlerExceptionResolver maps it to 405
  → 405 Method Not Allowed ← correct, informative
```

---

### Complete Exception-to-Status Mapping

```
┌────────────────────────────────────────────────┬────────┬───────────────────────────────┐
│  Spring MVC Exception                          │ Status │  When it is thrown            │
├────────────────────────────────────────────────┼────────┼───────────────────────────────┤
│ HttpRequestMethodNotSupportedException         │  405   │ Wrong HTTP method on endpoint │
│ HttpMediaTypeNotSupportedException             │  415   │ Unsupported Content-Type      │
│ HttpMediaTypeNotAcceptableException            │  406   │ Can't produce requested type  │
│ MissingPathVariableException                   │  500   │ @PathVariable not bound       │
│ MissingServletRequestParameterException        │  400   │ Required @RequestParam absent │
│ MissingServletRequestPartException             │  400   │ Required multipart part absent│
│ ServletRequestBindingException                 │  400   │ Header/cookie binding failure │
│ MethodArgumentTypeMismatchException            │  400   │ Type conversion failure       │
│ NoHandlerFoundException                        │  404   │ No mapping for the URL        │
│ AsyncRequestTimeoutException                   │  503   │ Async processing timed out   │
│ BindException                                  │  400   │ @ModelAttribute binding fail  │
│ ErrorResponseException                         │ varies │ Programmatic error response   │
│ MaxUploadSizeExceededException                 │  400   │ File upload too large         │
│ ConversionNotSupportedException                │  500   │ No converter found for type   │
│ TypeMismatchException                          │  400   │ Property type mismatch        │
└────────────────────────────────────────────────┴────────┴───────────────────────────────┘
```

---

### How Each Exception is Triggered

#### `HttpRequestMethodNotSupportedException` → 405

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")          // ← only GET is mapped
    public User getUser(@PathVariable Long id) { ... }
}
```

```
Client:  DELETE /users/99
Spring:  finds the mapping for /users/{id}, but DELETE is not registered
         → throws HttpRequestMethodNotSupportedException("DELETE")
         → DefaultHandlerExceptionResolver → 405

Response headers set by the resolver:
  Allow: GET, HEAD   ← Spring adds the Allow header automatically
```

```java
// Simplified from DefaultHandlerExceptionResolver source
protected ModelAndView handleHttpRequestMethodNotSupported(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler) throws IOException {

    Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();
    if (!CollectionUtils.isEmpty(supportedMethods)) {
        response.setHeader("Allow", StringUtils.collectionToCommaDelimitedString(supportedMethods));
    }
    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
    return new ModelAndView();
}
```

#### `HttpMediaTypeNotSupportedException` → 415

```java
@PostMapping(path = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
public User createUser(@RequestBody UserRequest req) { ... }
```

```
Client:  POST /users  Content-Type: application/xml
Spring:  endpoint only consumes application/json
         → throws HttpMediaTypeNotSupportedException
         → DefaultHandlerExceptionResolver → 415

Response headers:
  Accept: application/json   ← tells client what types ARE supported
```

#### `HttpMediaTypeNotAcceptableException` → 406

```java
@GetMapping(path = "/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
public User getUser(@PathVariable Long id) { ... }
```

```
Client:  GET /users/99  Accept: application/xml
Spring:  endpoint can only produce application/json
         → throws HttpMediaTypeNotAcceptableException
         → DefaultHandlerExceptionResolver → 406
```

#### `MissingServletRequestParameterException` → 400

```java
@GetMapping("/search")
public List<User> search(@RequestParam String query) { ... }  // required by default
```

```
Client:  GET /search          (no ?query= parameter)
Spring:  required parameter 'query' is not present in the request
         → throws MissingServletRequestParameterException("query", "String")
         → DefaultHandlerExceptionResolver → 400

{
  "status": 400,
  "error":  "Bad Request",
  "message": "Required request parameter 'query' for method parameter type String is not present"
}
```

#### `MethodArgumentTypeMismatchException` → 400

```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) { ... }
```

```
Client:  GET /users/abc        (can't convert "abc" to Long)
Spring:  type conversion fails
         → throws MethodArgumentTypeMismatchException
         → DefaultHandlerExceptionResolver → 400
```

#### `NoHandlerFoundException` → 404

```
Client:  GET /api/nonexistent
Spring:  no mapping found for GET /api/nonexistent
         → throws NoHandlerFoundException
         → DefaultHandlerExceptionResolver → 404
```

> **Note:** `NoHandlerFoundException` is only thrown (and thus only handled by the resolver) if you configure Spring to throw it instead of forwarding to the container's default 404 handling:

```yaml
# application.yml — enable the exception to be thrown
spring:
  mvc:
    throw-exception-if-no-handler-found: true
  web:
    resources:
      add-mappings: false   # disable default static resource handler which catches all paths
```

#### `BindException` → 400

```java
@GetMapping("/users")
public List<User> listUsers(@ModelAttribute UserFilter filter) { ... }
// UserFilter has validation constraints
```

```
Client:  GET /users?age=notANumber
Spring:  binding "notANumber" to UserFilter.age (int) fails
         → throws BindException
         → DefaultHandlerExceptionResolver → 400
```

> **Note:** `MethodArgumentNotValidException` (thrown when `@Valid` fails on `@RequestBody`) extends `BindException` but is handled separately by `ExceptionHandlerExceptionResolver` through `@ExceptionHandler`. `DefaultHandlerExceptionResolver` handles `BindException` directly for `@ModelAttribute` binding failures.

---

### Internal Template — How `DefaultHandlerExceptionResolver` Works

`DefaultHandlerExceptionResolver` extends `AbstractHandlerExceptionResolver` and overrides `doResolveException()`. Internally it is a large `instanceof` chain that routes each known exception type to a dedicated `handleXxx()` method.

```java
// Simplified from Spring source — DefaultHandlerExceptionResolver
public class DefaultHandlerExceptionResolver extends AbstractHandlerExceptionResolver {

    @Override
    protected ModelAndView doResolveException(HttpServletRequest request,
                                               HttpServletResponse response,
                                               Object handler,
                                               Exception ex) {
        try {
            if (ex instanceof HttpRequestMethodNotSupportedException subEx) {
                return handleHttpRequestMethodNotSupported(subEx, request, response, handler);
            }
            else if (ex instanceof HttpMediaTypeNotSupportedException subEx) {
                return handleHttpMediaTypeNotSupported(subEx, request, response, handler);
            }
            else if (ex instanceof HttpMediaTypeNotAcceptableException subEx) {
                return handleHttpMediaTypeNotAcceptable(subEx, request, response, handler);
            }
            else if (ex instanceof MissingPathVariableException subEx) {
                return handleMissingPathVariable(subEx, request, response, handler);
            }
            else if (ex instanceof MissingServletRequestParameterException subEx) {
                return handleMissingServletRequestParameter(subEx, request, response, handler);
            }
            else if (ex instanceof MissingServletRequestPartException subEx) {
                return handleMissingServletRequestPartException(subEx, request, response, handler);
            }
            else if (ex instanceof ServletRequestBindingException subEx) {
                return handleServletRequestBindingException(subEx, request, response, handler);
            }
            else if (ex instanceof NoHandlerFoundException subEx) {
                return handleNoHandlerFoundException(subEx, request, response, handler);
            }
            else if (ex instanceof AsyncRequestTimeoutException subEx) {
                return handleAsyncRequestTimeoutException(subEx, request, response, handler);
            }
            else if (ex instanceof BindException subEx) {
                return handleBindException(subEx, request, response, handler);
            }
            // ... more types
        }
        catch (Exception handlerEx) {
            // log the secondary exception
        }
        return null;  // unknown exception type — let it go unhandled
    }
}
```

Every `handleXxx()` method follows the same pattern:

```java
protected ModelAndView handleHttpRequestMethodNotSupported(...) throws IOException {
    // 1. (Optional) Set relevant response headers
    response.setHeader("Allow", ...);

    // 2. Call sendError() to commit the status code + message
    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());

    // 3. Return a non-null (but empty) ModelAndView to signal "handled"
    return new ModelAndView();
}
```

The empty `ModelAndView` is the signal that resolution succeeded. No view is rendered — the response is already written via `sendError()`. The Servlet container then forwards to `/error` where `BasicErrorController` and `DefaultErrorAttributes` produce the final JSON.

---

### What Happens to Unrecognised Exceptions

If the thrown exception is **not** one of the known Spring MVC types, `doResolveException()` falls through the `instanceof` chain and returns `null`. This means `DefaultHandlerExceptionResolver` gives up, and no resolver in the chain handled the exception.

```
Controller throws SomeUnknownException
         │
         ▼
[1] ExceptionHandlerExceptionResolver  → null
[2] ResponseStatusExceptionResolver    → null
[3] DefaultHandlerExceptionResolver    → null (not a known Spring MVC type)
         │
         ▼
HandlerExceptionResolverComposite returns null to DispatcherServlet
         │
         ▼
DispatcherServlet re-throws the exception to the Servlet container
         │
         ▼
Tomcat catches it → status 500 → forwards to /error
         │
         ▼
BasicErrorController → DefaultErrorAttributes → 500 JSON response
```

---

### Overriding `DefaultHandlerExceptionResolver` Behaviour

You can override the default handling for any Spring MVC exception by providing an `@ExceptionHandler` in a `@RestControllerAdvice`. Your handler runs first (it's in `ExceptionHandlerExceptionResolver`, which is priority 1) and `DefaultHandlerExceptionResolver` is never reached.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Override 405 — add custom JSON body instead of Spring's plain /error JSON
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(new ErrorResponse(
                        "Method Not Allowed",
                        "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint. " +
                        "Supported: " + ex.getSupportedHttpMethods(),
                        request.getRequestURI()
                ));
    }

    // Override 400 for missing request parameters — more user-friendly message
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(
                        "Missing Required Parameter",
                        "Required parameter '" + ex.getParameterName() +
                        "' of type " + ex.getParameterType() + " is missing",
                        request.getRequestURI()
                ));
    }

    // Override 400 for type mismatches — show what was received and what was expected
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String expected = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName() : "unknown";

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(
                        "Type Mismatch",
                        "Parameter '" + ex.getName() + "' should be of type " + expected +
                        " but received: '" + ex.getValue() + "'",
                        request.getRequestURI()
                ));
    }
}
```

---

### Flow When You Override vs When You Don't

```
┌──────────────────────────────────────────────────────────────────────┐
│  Without @ExceptionHandler override                                  │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  [1] ExceptionHandlerExceptionResolver → null (no @ExceptionHandler)│
│  [2] ResponseStatusExceptionResolver   → null (no @ResponseStatus)  │
│  [3] DefaultHandlerExceptionResolver   → 405/400/406/...            │
│       calls response.sendError(status) → Servlet container          │
│       → BasicErrorController → /error JSON                          │
│                                                                      │
│  Result: correct status, BUT Spring's standard /error JSON shape    │
│          {"timestamp","status","error","message","path"}            │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│  With @ExceptionHandler override in @RestControllerAdvice            │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  [1] ExceptionHandlerExceptionResolver → FOUND → invokes your method│
│       writes ResponseEntity(405, customBody) directly               │
│       → BasicErrorController NOT involved                           │
│  [2] [3] never reached                                              │
│                                                                      │
│  Result: correct status, YOUR JSON shape, YOUR fields               │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Summary

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  DefaultHandlerExceptionResolver — Key Facts                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│  Position in chain     │  3rd (last) — lowest priority                      │
│  Resolver base class   │  AbstractHandlerExceptionResolver                   │
│  What it handles       │  Well-known Spring MVC infrastructure exceptions     │
│  What it does NOT do   │  Handle your custom exceptions                      │
│  Response body format  │  Spring's /error JSON (via sendError → container)  │
│  Can be overridden?    │  YES — add @ExceptionHandler for any of its types   │
│  Automatic?            │  YES — zero configuration needed for basic use      │
└──────────────────────────────────────────────────────────────────────────────┘
```

> **Bottom line:** `DefaultHandlerExceptionResolver` is the "safety net" for Spring MVC's own exceptions. It ensures clients get a meaningful 4xx status instead of a generic 500 for common client-side mistakes. For REST APIs that need a custom JSON error structure for these cases, override the specific exception types with `@ExceptionHandler` in your `@RestControllerAdvice`.




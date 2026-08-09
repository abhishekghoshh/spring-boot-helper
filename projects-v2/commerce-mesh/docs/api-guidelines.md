# API Design Guidelines

## Base URL & Versioning

All APIs are versioned and accessed through the API Gateway:

```
http://localhost:8080/api/v1/
```

The version (`v1`, `v2`) is part of the URL path. Breaking changes warrant a new version.

---

## HTTP Methods

| Method | Purpose | Idempotent | Safe |
|--------|---------|-----------|------|
| `GET` | Retrieve resource(s) | Yes | Yes |
| `POST` | Create resource | No | No |
| `PUT` | Full update/replace | Yes | No |
| `PATCH` | Partial update | No | No |
| `DELETE` | Delete resource | Yes | No |

---

## Standard Response Envelope

Every API response uses a consistent wrapper:

### Success Response (2xx)

```json
{
  "success": true,
  "data": { },
  "message": "Operation completed successfully",
  "timestamp": "2026-07-25T15:30:00Z"
}
```

### Paginated Response

```json
{
  "success": true,
  "data": {
    "content": [ ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "timestamp": "2026-07-25T15:30:00Z"
}
```

### Error Response (4xx/5xx)

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Field validation failed",
    "details": [
      {
        "field": "email",
        "message": "must be a valid email address",
        "rejectedValue": "notanemail"
      }
    ]
  },
  "timestamp": "2026-07-25T15:30:00Z"
}
```

Standard error codes:
| Code | HTTP | Meaning |
|------|------|---------|
| `VALIDATION_ERROR` | 400 | Request body/param validation failed |
| `UNAUTHORIZED` | 401 | Missing or invalid JWT |
| `FORBIDDEN` | 403 | Valid JWT but insufficient permissions |
| `NOT_FOUND` | 404 | Resource does not exist |
| `CONFLICT` | 409 | Duplicate resource (e.g. username taken) |
| `BUSINESS_ERROR` | 422 | Business rule violation (e.g. insufficient stock) |
| `INTERNAL_ERROR` | 500 | Unexpected server error |
| `SERVICE_UNAVAILABLE` | 503 | Circuit breaker open, downstream service unavailable |

---

## HTTP Status Codes — Usage Guide

| Code | Usage |
|------|-------|
| **200** | Successful GET, PUT, PATCH |
| **201** | Successful POST — include `Location` header with new resource URL |
| **204** | Successful DELETE — no response body |
| **400** | Bad Request — validation error, malformed JSON, invalid query param |
| **401** | Unauthorized — missing/expired/blacklisted JWT |
| **403** | Forbidden — authenticated user lacks required role/permission |
| **404** | Not Found — resource ID doesn't exist |
| **409** | Conflict — duplicate username/email, order already processed |
| **422** | Unprocessable Entity — business rule violation (e.g., insufficient inventory) |
| **429** | Too Many Requests — rate limit exceeded |
| **500** | Internal Server Error — unhandled exception |
| **503** | Service Unavailable — downstream service unavailable (circuit breaker open) |

---

## Pagination & Sorting

All list endpoints support pagination via query parameters:

```
GET /api/v1/products?page=0&size=20&sort=name,asc&sort=price,desc
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Zero-based page index |
| `size` | int | 20 | Items per page (max 100) |
| `sort` | string | — | Comma-separated `property,direction` pairs |

---

## Filtering & Search

Filtering is resource-specific. Common patterns:

```
GET /api/v1/products?category=electronics&brand=samsung&minPrice=100&maxPrice=500
GET /api/v1/orders?status=PENDING&fromDate=2026-01-01&toDate=2026-07-01
GET /api/v1/search?q=wireless+headphones&page=0&size=20
```

---

## Request/Response Headers

### Standard Request Headers

| Header | Required | Value |
|--------|----------|-------|
| `Authorization` | Yes (except auth endpoints) | `Bearer <jwt_access_token>` |
| `Content-Type` | Yes (POST/PUT/PATCH) | `application/json` |
| `Accept` | No | `application/json` |
| `X-Request-Id` | No | UUID for tracing correlation |
| `X-Idempotency-Key` | No (payment endpoints) | UUID to prevent duplicate operations |

### Standard Response Headers

| Header | Description |
|--------|-------------|
| `Location` | URL of created resource (201 responses) |
| `X-Request-Id` | Echo back request ID, or generated if absent |
| `X-RateLimit-Remaining` | Remaining requests in the current window |
| `X-RateLimit-Reset` | Unix timestamp when the rate limit resets |

---

## Naming Conventions

- **URL paths:** plural nouns, kebab-case: `/product-categories`, `/order-items`
- **JSON properties:** camelCase: `createdAt`, `orderId`, `shippingAddress`
- **Query parameters:** camelCase: `?startDate=&pageSize=`
- **Enum values:** UPPER_SNAKE_CASE: `"PENDING"`, `"IN_PROGRESS"`, `"SHIPPED"`

---

## Field Naming Standards

| Concept | Property Name | Type |
|---------|--------------|------|
| Primary key | `id` | String (UUID) |
| Timestamps | `createdAt`, `updatedAt` | ISO 8601 string |
| Soft delete | `deletedAt` | ISO 8601 string (null = active) |
| Audit | `createdBy`, `updatedBy` | String (user ID) |
| Versioning | `version` | Long (optimistic lock) |
| Status | `status` | Enum string |
| Active flag | `active` | Boolean |

---

## Complete API Endpoint Catalog

### Authentication Service (`/api/v1/auth`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| `POST` | `/auth/register` | No | — | Register new user |
| `POST` | `/auth/login` | No | — | Login, returns JWT + refresh token |
| `POST` | `/auth/refresh` | No | — | Exchange refresh token for new JWT |
| `POST` | `/auth/logout` | Yes | Any | Invalidate tokens |
| `POST` | `/auth/verify-email` | No | — | Verify email with token |
| `POST` | `/auth/forgot-password` | No | — | Send password reset email |
| `POST` | `/auth/reset-password` | No | — | Reset password with token |
| `GET` | `/auth/me` | Yes | Any | Get current user profile |
| `PUT` | `/auth/change-password` | Yes | Any | Change own password |

### User Service (`/api/v1/users`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| `GET` | `/users` | Yes | ADMIN, CUSTOMER_SUPPORT | List all users (paginated) |
| `GET` | `/users/{id}` | Yes | ADMIN, CUSTOMER_SUPPORT | Get user by ID |
| `PUT` | `/users/{id}` | Yes | ADMIN | Update user |
| `DELETE` | `/users/{id}` | Yes | SUPER_ADMIN | Soft-delete user |
| `GET` | `/users/{id}/addresses` | Yes | User or ADMIN | Get user addresses |
| `POST` | `/users/{id}/addresses` | Yes | User or ADMIN | Add address |
| `PUT` | `/users/{id}/addresses/{addrId}` | Yes | User or ADMIN | Update address |
| `DELETE` | `/users/{id}/addresses/{addrId}` | Yes | User or ADMIN | Delete address |
| `PUT` | `/users/{id}/roles` | Yes | SUPER_ADMIN | Assign roles to user |

### Product Catalog Service (`/api/v1/products`, `/api/v1/categories`, `/api/v1/brands`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| `GET` | `/products` | Yes | Any | List products (paginated, filterable) |
| `GET` | `/products/{id}` | Yes | Any | Get product by ID |
| `POST` | `/products` | Yes | ADMIN, PRODUCT_MANAGER | Create product |
| `PUT` | `/products/{id}` | Yes | ADMIN, PRODUCT_MANAGER | Update product |
| `DELETE` | `/products/{id}` | Yes | ADMIN, PRODUCT_MANAGER | Delete product |
| `POST` | `/products/{id}/reviews` | Yes | CUSTOMER | Add review |
| `GET` | `/products/{id}/reviews` | Yes | Any | Get product reviews |
| `GET` | `/categories` | Yes | Any | List categories |
| `POST` | `/categories` | Yes | ADMIN, PRODUCT_MANAGER | Create category |
| `PUT` | `/categories/{id}` | Yes | ADMIN, PRODUCT_MANAGER | Update category |
| `DELETE` | `/categories/{id}` | Yes | ADMIN, PRODUCT_MANAGER | Delete category |
| `GET` | `/brands` | Yes | Any | List brands |
| `POST` | `/brands` | Yes | ADMIN, PRODUCT_MANAGER | Create brand |
| `PUT` | `/brands/{id}` | Yes | ADMIN, PRODUCT_MANAGER | Update brand |
| `DELETE` | `/brands/{id}` | Yes | ADMIN, PRODUCT_MANAGER | Delete brand |

### Cart Service (`/api/v1/cart`, `/api/v1/wishlist`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| `GET` | `/cart` | Yes | CUSTOMER | Get current user's cart |
| `POST` | `/cart/items` | Yes | CUSTOMER | Add item to cart |
| `PUT` | `/cart/items/{itemId}` | Yes | CUSTOMER | Update item quantity |
| `DELETE` | `/cart/items/{itemId}` | Yes | CUSTOMER | Remove item from cart |
| `DELETE` | `/cart` | Yes | CUSTOMER | Clear entire cart |
| `POST` | `/cart/coupons` | Yes | CUSTOMER | Apply coupon code |
| `DELETE` | `/cart/coupons/{code}` | Yes | CUSTOMER | Remove coupon |
| `GET` | `/wishlist` | Yes | CUSTOMER | Get wishlist |
| `POST` | `/wishlist/items` | Yes | CUSTOMER | Add to wishlist |
| `DELETE` | `/wishlist/items/{productId}` | Yes | CUSTOMER | Remove from wishlist |

### Inventory Service (`/api/v1/inventory`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| `GET` | `/inventory/warehouses` | Yes | ADMIN, PRODUCT_MANAGER | List warehouses |
| `POST` | `/inventory/warehouses` | Yes | ADMIN | Create warehouse |
| `GET` | `/inventory/stock/{productId}` | Yes | Any | Get stock for a product |
| `PUT` | `/inventory/stock/{productId}` | Yes | ADMIN, PRODUCT_MANAGER | Update stock quantity |
| `POST` | `/inventory/reserve` | Yes | Internal | Reserve stock for order |
| `POST` | `/inventory/release` | Yes | Internal | Release reserved stock |
| `GET` | `/inventory/movements` | Yes | ADMIN | View stock movements |

### Order Service (`/api/v1/orders`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| `GET` | `/orders` | Yes | CUSTOMER, ORDER_MANAGER | List orders (filtered by role) |
| `GET` | `/orders/{id}` | Yes | CUSTOMER, ORDER_MANAGER | Get order detail |
| `POST` | `/orders` | Yes | CUSTOMER | Place new order (checkout) |
| `PUT` | `/orders/{id}/status` | Yes | ADMIN, ORDER_MANAGER | Update order status |
| `PUT` | `/orders/{id}/cancel` | Yes | CUSTOMER | Cancel order (before shipping) |
| `GET` | `/orders/{id}/history` | Yes | Any | Get order status history |

### Payment Service (`/api/v1/payments`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| `POST` | `/payments` | Yes | Internal (order-service) | Process payment |
| `GET` | `/payments/{id}` | Yes | CUSTOMER, ADMIN | Get payment detail |
| `POST` | `/payments/{id}/refund` | Yes | ADMIN, ORDER_MANAGER | Process refund |
| `GET` | `/payments/{id}/invoice` | Yes | CUSTOMER, ADMIN | Get invoice PDF |

### Notification Service (`/api/v1/notifications`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| `GET` | `/notifications` | Yes | CUSTOMER | Get user's notifications |
| `PUT` | `/notifications/{id}/read` | Yes | CUSTOMER | Mark notification as read |
| `POST` | `/notifications/send` | Yes | Internal | Send notification (consumed by RabbitMQ handler too) |

### Search Service (`/api/v1/search`)

| Method | Path | Auth | Roles | Description |
|--------|------|------|-------|-------------|
| `GET` | `/search` | Yes | Any | Full-text product search |
| `GET` | `/search/suggestions` | Yes | Any | Auto-complete suggestions |
| `GET` | `/search/facets` | Yes | Any | Search facets (category, brand, price range) |

---

## Input Validation

All request bodies must include Bean Validation annotations on DTOs:

```java
public record CreateProductRequest(
    @NotBlank @Size(min = 2, max = 200) String name,
    @NotBlank @Size(min = 10, max = 5000) String description,
    @NotBlank @Pattern(regexp = "^[A-Z]{2,4}-\\d{4,10}$") String sku,
    @NotNull @Positive BigDecimal price,
    @PositiveOrZero BigDecimal comparePrice,
    @NotBlank String categoryId,
    @NotBlank String brandId
) {}
```

Validation errors return 400 with field-level details.

---

## Rate Limiting

API Gateway enforces rate limits per authenticated user:

| Tier | Requests/Minute | Burst |
|------|----------------|-------|
| Anonymous | 30 | 5 |
| CUSTOMER | 100 | 20 |
| ADMIN | 500 | 50 |
| Internal (service-to-service) | Unlimited | — |

Rate limit headers are included in every response when applicable.

---

## OpenAPI / Swagger

Every service exposes OpenAPI 3.0 documentation:

- **Swagger UI:** `http://<service-host>:<port>/swagger-ui.html`
- **OpenAPI JSON:** `http://<service-host>:<port>/v3/api-docs`
- **Aggregated (Gateway):** `http://localhost:8080/swagger-ui.html` (aggregates all services)

---

## Security

- All endpoints (except `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/verify-email`) require `Authorization: Bearer <JWT>` header
- API Gateway validates JWT signature, expiry, and blacklist before forwarding
- Service-to-service calls use Feign clients with automatic JWT propagation
- Use `@PreAuthorize("hasRole('ADMIN')")` or `@PreAuthorize("hasAuthority('product:write')")` for method security

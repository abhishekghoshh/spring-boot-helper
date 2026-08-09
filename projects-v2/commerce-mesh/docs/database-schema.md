# Database Schema & Persistence

## Overview

CommerceMesh uses a **polyglot persistence** strategy: PostgreSQL for ACID transactional data, MongoDB for flexible document data, and Redis for high-speed caching and transient state. Each microservice owns its own database — there is no shared database between services.

---

## Database Per Service Strategy

```
┌──────────────────────────────────────────────────────────────────────┐
│                        PostgreSQL 17                                │
│                                                                      │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                 │
│  │ Schema: auth │ │Schema: users │ │Schema: orders│                 │
│  │              │ │              │ │              │                 │
│  │ auth-service │ │ user-service │ │order-service │                 │
│  └──────────────┘ └──────────────┘ └──────────────┘                 │
│                                                                      │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                 │
│  │Schema:invntry│ │Schema: pyment│ │Schema:ntfctns│                 │
│  │              │ │              │ │              │                 │
│  │inventory-svc │ │payment-svc   │ │notification  │                 │
│  └──────────────┘ └──────────────┘ └──────────────┘                 │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                        MongoDB 8                                     │
│                                                                      │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐     │
│  │Collection:       │ │Collection:       │ │Collection:       │     │
│  │products, categs, │ │carts, wishlists, │ │search_index      │     │
│  │brands, reviews   │ │coupons           │ │                  │     │
│  │                  │ │                  │ │                  │     │
│  │product-catalog   │ │cart-service      │ │search-service    │     │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘     │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                        Redis 7                                       │
│                                                                      │
│  Key Patterns: session:{id}, product:{id}, cart:{userId},           │
│                jwt:blacklist:{jti}, rate:{user}:{endpoint}          │
│                                                                      │
│  Used by: auth-service, cart-service, product-catalog-service       │
└──────────────────────────────────────────────────────────────────────┘
```

---

## PostgreSQL Schemas

### Schema: `auth` (authentication-service)

```sql
CREATE SCHEMA IF NOT EXISTS auth;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE auth.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT false,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    account_locked BOOLEAN NOT NULL DEFAULT false,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auth.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auth.permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,   -- e.g. "product:write"
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auth.user_roles (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
    granted_by UUID REFERENCES auth.users(id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE auth.role_permissions (
    role_id UUID NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES auth.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE auth.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auth.email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auth.password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_users_email ON auth.users(email);
CREATE INDEX idx_users_username ON auth.users(username);
CREATE INDEX idx_refresh_tokens_user ON auth.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON auth.refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expiry ON auth.refresh_tokens(expires_at) WHERE revoked = false;
```

### Schema: `users` (user-service)

```sql
CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE users.profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,           -- FK to auth.users(id) conceptually, not enforced by DB
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    date_of_birth DATE,
    preferences JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users.addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    label VARCHAR(50) DEFAULT 'Home',
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    address_type VARCHAR(20) DEFAULT 'SHIPPING',  -- SHIPPING or BILLING
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_addresses_user ON users.addresses(user_id);
CREATE INDEX idx_addresses_user_default ON users.addresses(user_id) WHERE is_default = true;
```

### Schema: `inventory` (inventory-service)

```sql
CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.warehouses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL UNIQUE,
    address_line VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE inventory.stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id VARCHAR(36) NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    low_stock_threshold INTEGER DEFAULT 10,
    version BIGINT NOT NULL DEFAULT 0,       -- optimistic locking
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, warehouse_id)
);

CREATE TABLE inventory.stock_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id VARCHAR(36) NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES inventory.warehouses(id),
    movement_type VARCHAR(20) NOT NULL,       -- INBOUND, OUTBOUND, RESERVATION, RELEASE, ADJUSTMENT
    quantity INTEGER NOT NULL,
    reference_type VARCHAR(20),               -- ORDER, PURCHASE_ORDER, MANUAL
    reference_id VARCHAR(36),
    notes TEXT,
    performed_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_product ON inventory.stock(product_id);
CREATE INDEX idx_stock_warehouse ON inventory.stock(warehouse_id);
CREATE INDEX idx_stock_low ON inventory.stock(warehouse_id) WHERE quantity <= low_stock_threshold;
CREATE INDEX idx_movements_product ON inventory.stock_movements(product_id);
CREATE INDEX idx_movements_date ON inventory.stock_movements(created_at DESC);
```

### Schema: `orders` (order-service)

```sql
CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE orders.orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    order_number VARCHAR(20) NOT NULL UNIQUE GENERATED ALWAYS AS ('ORD-' || to_char(created_at, 'YYYYMMDD') || '-' || substring(id::text, 1, 8)) STORED,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',     -- PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED
    subtotal DECIMAL(12,2) NOT NULL,
    tax DECIMAL(12,2) NOT NULL DEFAULT 0,
    shipping_cost DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    shipping_address_id UUID NOT NULL,
    billing_address_id UUID NOT NULL,
    coupon_code VARCHAR(50),
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE orders.order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders.orders(id) ON DELETE CASCADE,
    product_id VARCHAR(36) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_sku VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE orders.order_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders.orders(id) ON DELETE CASCADE,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    notes TEXT,
    changed_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE orders.processed_events (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user ON orders.orders(user_id);
CREATE INDEX idx_orders_status ON orders.orders(status);
CREATE INDEX idx_orders_date ON orders.orders(created_at DESC);
CREATE INDEX idx_order_items_order ON orders.order_items(order_id);
CREATE INDEX idx_history_order ON orders.order_history(order_id);
CREATE INDEX idx_processed_events_expiry ON orders.processed_events(processed_at);
```

### Schema: `payments` (payment-service)

```sql
CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE payments.payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_method VARCHAR(20) NOT NULL,        -- CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, PARTIALLY_REFUNDED
    transaction_id VARCHAR(100),
    gateway_response JSONB,
    idempotency_key VARCHAR(36) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payments.refunds (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID NOT NULL REFERENCES payments.payments(id),
    amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    transaction_id VARCHAR(100),
    processed_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payments.invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL,
    invoice_number VARCHAR(20) NOT NULL UNIQUE,
    payment_id UUID REFERENCES payments.payments(id),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    pdf_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order ON payments.payments(order_id);
CREATE INDEX idx_payments_status ON payments.payments(status);
CREATE INDEX idx_payments_idempotency ON payments.payments(idempotency_key);
CREATE INDEX idx_refunds_payment ON payments.refunds(payment_id);
```

### Schema: `notifications` (notification-service)

```sql
CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE notifications.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,                  -- EMAIL, SMS, PUSH, IN_APP
    template_name VARCHAR(50) NOT NULL,         -- ORDER_CONFIRMATION, SHIPPING_UPDATE, PASSWORD_RESET, etc.
    subject VARCHAR(255),
    body TEXT,
    recipient VARCHAR(255) NOT NULL,            -- email address, phone number, device token
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, FAILED, BOUNCED
    event_id VARCHAR(36),                       -- correlation to the triggering event
    retry_count INTEGER NOT NULL DEFAULT 0,
    sent_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications.email_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    subject_template VARCHAR(500) NOT NULL,     -- Thymeleaf template
    body_template TEXT NOT NULL,                -- Thymeleaf template
    variables JSONB DEFAULT '[]',               -- ['orderId', 'total', 'items']
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications.processed_events (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications.notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications.notifications(status);
CREATE INDEX idx_notifications_event ON notifications.notifications(event_id);
```

### PostgreSQL Common Indexing Strategy

- **Foreign keys:** Always indexed (avoid sequential scans on joins)
- **Status columns:** Indexed for list-by-status queries (e.g., get all PENDING orders)
- **Timestamps:** Descending indexes for "most recent" queries
- **Unique constraints:** On natural keys (username, email, sku)
- **Partial indexes:** For filtered queries (e.g., only non-revoked tokens)

---

## MongoDB Collections

### Collection: `products` (product-catalog-service)

```json
{
  "_id": "product-123",
  "name": "Wireless Bluetooth Headphones",
  "slug": "wireless-bluetooth-headphones",
  "description": "Premium noise-cancelling wireless headphones...",
  "shortDescription": "Premium ANC headphones",
  "sku": "WH-1000XM5",
  "brand": {
    "id": "brand-456",
    "name": "Sony"
  },
  "category": {
    "id": "category-789",
    "name": "Electronics",
    "path": ["Electronics", "Audio", "Headphones"]
  },
  "price": {
    "amount": 349.99,
    "currency": "USD",
    "compareAtAmount": 399.99
  },
  "inventory": {
    "available": 150,
    "reserved": 12,
    "lowStockThreshold": 10
  },
  "images": [
    {
      "url": "https://cdn.example.com/products/123/main.jpg",
      "alt": "Front view",
      "isPrimary": true,
      "order": 0
    }
  ],
  "attributes": {
    "color": "Black",
    "wireless": true,
    "battery_life": "30 hours",
    "weight_grams": 250
  },
  "variants": [
    {
      "sku": "WH-1000XM5-BLK",
      "attributes": { "color": "Black" },
      "price": { "amount": 349.99, "currency": "USD" }
    }
  ],
  "tags": ["wireless", "noise-cancelling", "bluetooth", "premium"],
  "reviewSummary": {
    "averageRating": 4.5,
    "totalReviews": 234,
    "distribution": { "5": 150, "4": 50, "3": 20, "2": 8, "1": 6 }
  },
  "searchableText": "wireless bluetooth headphones noise cancelling premium sony",
  "isActive": true,
  "createdAt": "2026-07-25T15:30:00Z",
  "updatedAt": "2026-07-25T15:30:00Z"
}
```

### Collection: `categories`

```json
{
  "_id": "category-789",
  "name": "Headphones",
  "slug": "headphones",
  "description": "Audio headphones and earbuds",
  "parentId": "category-electronic",
  "ancestors": ["category-electronics"],
  "image": "https://cdn.example.com/categories/headphones.jpg",
  "sortOrder": 10,
  "isActive": true,
  "createdAt": "2026-07-25T15:30:00Z",
  "updatedAt": "2026-07-25T15:30:00Z"
}
```

### Collection: `brands`

```json
{
  "_id": "brand-456",
  "name": "Sony",
  "slug": "sony",
  "description": "Sony electronics",
  "logo": "https://cdn.example.com/brands/sony.png",
  "website": "https://sony.com",
  "isActive": true,
  "createdAt": "2026-07-25T15:30:00Z"
}
```

### Collection: `reviews`

```json
{
  "_id": "review-990",
  "productId": "product-123",
  "userId": "user-456",
  "userName": "John D.",
  "rating": 5,
  "title": "Best headphones I've owned",
  "comment": "The noise cancellation is incredible...",
  "isVerifiedPurchase": true,
  "helpfulCount": 12,
  "createdAt": "2026-07-25T15:30:00Z"
}
```

### Collection: `carts` (cart-service)

```json
{
  "_id": "cart-user-456",
  "userId": "user-456",
  "items": [
    {
      "productId": "product-123",
      "name": "Wireless Bluetooth Headphones",
      "sku": "WH-1000XM5",
      "image": "https://cdn.example.com/products/123/thumb.jpg",
      "price": 349.99,
      "quantity": 1,
      "addedAt": "2026-07-25T15:30:00Z"
    }
  ],
  "coupon": {
    "code": "SUMMER20",
    "discountType": "PERCENTAGE",
    "discountValue": 20,
    "discountAmount": 70.00
  },
  "itemCount": 1,
  "subtotal": 349.99,
  "discount": 70.00,
  "total": 279.99,
  "expiresAt": "2026-08-01T15:30:00Z",
  "createdAt": "2026-07-25T15:30:00Z",
  "updatedAt": "2026-07-25T15:30:00Z"
}
```

### Collection: `wishlists`

```json
{
  "_id": "wishlist-user-456",
  "userId": "user-456",
  "products": [
    {
      "productId": "product-123",
      "addedAt": "2026-07-25T15:30:00Z"
    }
  ],
  "createdAt": "2026-07-25T15:30:00Z",
  "updatedAt": "2026-07-25T15:30:00Z"
}
```

### Collection: `search_index` (search-service)

```json
{
  "_id": "product-123",
  "productId": "product-123",
  "name": "Wireless Bluetooth Headphones",
  "description": "Premium noise-cancelling wireless headphones",
  "category": "Headphones",
  "brand": "Sony",
  "price": 349.99,
  "image": "https://cdn.example.com/products/123/thumb.jpg",
  "rating": 4.5,
  "reviewCount": 234,
  "tags": ["wireless", "noise-cancelling", "bluetooth", "premium"],
  "searchableText": "wireless bluetooth headphones noise cancelling premium sony wh-1000xm5",
  "createdAt": "2026-07-25T15:30:00Z"
}
```

### MongoDB Indexing Strategy

```javascript
// Products
db.products.createIndex({ slug: 1 }, { unique: true });
db.products.createIndex({ "brand.id": 1 });
db.products.createIndex({ "category.id": 1 });
db.products.createIndex({ "price.amount": 1 });
db.products.createIndex({ name: "text", description: "text", "tags": "text" });
db.products.createIndex({ "reviewSummary.averageRating": -1 });
db.products.createIndex({ isActive: 1, "price.amount": 1 });

// Categories
db.categories.createIndex({ slug: 1 }, { unique: true });
db.categories.createIndex({ parentId: 1 });

// Carts
db.carts.createIndex({ userId: 1 }, { unique: true });
db.carts.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 });  // TTL index

// Wishlists
db.wishlists.createIndex({ userId: 1 }, { unique: true });

// Reviews
db.reviews.createIndex({ productId: 1, createdAt: -1 });
db.reviews.createIndex({ userId: 1 });

// Search
db.search_index.createIndex({ searchableText: "text" });
db.search_index.createIndex({ tags: 1 });
db.search_index.createIndex({ category: 1, brand: 1 });
db.search_index.createIndex({ price: 1 });
```

---

## Redis Cache Design

### Cache Key Patterns

| Key Pattern | Data Type | TTL | Owner Service | Eviction Strategy |
|------------|-----------|-----|---------------|-------------------|
| `session:{userId}` | Hash | 30 min | auth-service | TTL |
| `product:{productId}` | Hash | 1 hour | product-catalog-service | TTL + explicit on update |
| `cart:{userId}` | Hash | 2 hours | cart-service | TTL + explicit on checkout |
| `otp:{email}` | String | 5 min | auth-service | TTL |
| `jwt:blacklist:{jti}` | String (1) | token remaining lifetime | auth-service | TTL |
| `trending:products` | ZSET | 15 min | product-catalog-service | TTL |
| `dashboard:stats` | Hash | 5 min | aggregation | TTL |
| `rate:{userId}:{endpoint}` | String (counter) | 1 min | gateway | TTL |
| `user:{userId}` | Hash | 30 min | auth-service | TTL + explicit on update |

### Key Design Rules

- Use colon (`:`) as namespace separator: `{domain}:{subdomain}:{identifier}`
- Include TTL on every key (no infinite keys)
- Never use `KEYS *` in production (use `SCAN` or maintain a set of active keys)
- Serialize complex objects as JSON strings or use Redis Hashes for field-level access

### Cache-Aside Pattern

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│ Service  │     │  Redis   │     │ Database │
└────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │                │
     │ 1. GET cache   │                │
     │───────────────▶│                │
     │                │                │
     │ 2. Cache MISS  │                │
     │◀───────────────│                │
     │                │                │
     │ 3. Query DB                    │
     │────────────────────────────────▶
     │                │                │
     │ 4. Return data                 │
     │◀────────────────────────────────
     │                │                │
     │ 5. SET cache                   │
     │───────────────▶│                │
     │                │                │
     │ 6. Return data to caller       │
```

### Write-Through vs Write-Behind

- **Write-through (products):** Update DB → update cache. Consistent, but slower writes.
- **Write-behind (cart):** Redis is primary source of truth. Async flush to MongoDB via scheduled task. Fast, eventually consistent.

---

## Data Migration & Versioning

### Flyway for PostgreSQL

Each service with PostgreSQL uses Flyway for schema migrations:

```
src/main/resources/db/migration/
├── V1__create_auth_schema.sql
├── V2__add_email_verification_tokens.sql
├── V3__seed_default_roles.sql
└── V4__add_account_locked_column.sql
```

- Migrations are versioned and immutable
- Run automatically on startup: `spring.flyway.enabled=true`
- Baseline on existing databases: `spring.flyway.baseline-on-migrate=true`

### MongoDB Schema Evolution

MongoDB schemas are flexible, but changes are tracked:

- Add new fields with sensible defaults
- Application handles reading old documents (backward compatible)
- Run migration scripts for bulk updates when needed
- Never delete fields without a deprecation period

---

## Connection Pooling

### PostgreSQL (HikariCP — default Spring Boot pool)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000          # 5 min
      connection-timeout: 30000     # 30 sec
      max-lifetime: 1800000         # 30 min
      leak-detection-threshold: 10000 # 10 sec
```

### MongoDB

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://user:pass@host:27017/commercemesh
      min-connection-per-host: 5
      max-connection-per-host: 50
      max-wait-time: 5000
```

### Redis (Lettuce)

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

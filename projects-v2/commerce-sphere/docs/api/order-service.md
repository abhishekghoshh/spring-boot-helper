# Order Service API

Base URL: `http://localhost:8085` (Docker) or `http://localhost:8086` (from host via Docker Compose)

## Checkout

```
POST /api/orders/checkout
```

**Request Body:**
```json
{
  "cartId": "guest-abc123",
  "userId": "user-456",
  "userEmail": "john@example.com",
  "shippingAddress": "123 Main St, San Francisco, CA 94105"
}
```

**Response:** `200 OK`
```json
{
  "id": "order-id",
  "orderNumber": "ORD-A1B2C3D4",
  "userId": "user-456",
  "userEmail": "john@example.com",
  "items": [
    {
      "productId": "prod-1",
      "productName": "Product Name",
      "price": 29.99,
      "quantity": 2
    }
  ],
  "subtotal": 59.98,
  "tax": 4.80,
  "shipping": 0.00,
  "total": 64.78,
  "status": "CONFIRMED",
  "shippingAddress": "123 Main St, San Francisco, CA 94105",
  "createdAt": "2026-01-01T00:00:00Z"
}
```

**Checkout Flow:**
1. Fetches cart from Cart Service
2. Checks inventory availability
3. Creates order with calculated totals (8% tax, free shipping over $50)
4. Reserves inventory in Inventory Service
5. Clears the cart
6. Publishes `OrderPlacedEvent` for async processing

---

## Order Endpoints

### Get Order by ID

```
GET /api/orders/{orderId}
```

**Response:** Order object

### Get User's Orders

```
GET /api/orders/user/{userId}?page=0&size=20
```

**Response:** Array of orders (most recent first)

### Cancel Order

```
POST /api/orders/{orderId}/cancel
```

**Response:** Updated order with status `CANCELLED`

**Note:** Only orders in `CONFIRMED` status can be cancelled.

---

## Order Statuses

| Status | Description |
|--------|-------------|
| `CONFIRMED` | Order placed and confirmed |
| `CANCELLED` | Order cancelled by user or admin |
| `SHIPPED` | Order shipped (future) |
| `DELIVERED` | Order delivered (future) |

## Events

The Order Service publishes Spring application events for async processing:

- `OrderPlacedEvent` — Fired when a new order is created
- `OrderCancelledEvent` — Fired when an order is cancelled

Event listeners handle inventory updates, notifications, and logging.

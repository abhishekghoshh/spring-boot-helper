# Cart Service API

Base URL: `http://localhost:8083` (Docker) or `http://localhost:8084` (from host via Docker Compose)

Cart data is stored in Redis with a 7-day TTL. Cart IDs are UUIDs (guest-<random> for guests, user ID for authenticated users).

## Cart Endpoints

### Get Cart

```
GET /api/cart/{cartId}
```

**Response:**
```json
{
  "id": "guest-abc123",
  "items": [
    {
      "productId": "prod-1",
      "productName": "Product Name",
      "price": 29.99,
      "quantity": 2,
      "imageUrl": "/uploads/img.jpg"
    }
  ],
  "totalAmount": 59.98,
  "totalItems": 2
}
```

### Add Item to Cart

```
POST /api/cart/{cartId}/items
```
```json
{
  "productId": "prod-1",
  "productName": "Product Name",
  "price": 29.99,
  "quantity": 1,
  "imageUrl": "/uploads/img.jpg"
}
```

### Update Item Quantity

```
PUT /api/cart/{cartId}/items/{productId}?quantity=3
```

Setting quantity to 0 removes the item.

### Remove Item

```
DELETE /api/cart/{cartId}/items/{productId}
```

### Clear Cart

```
DELETE /api/cart/{cartId}
```

### Merge Guest Cart

```
POST /api/cart/merge
```
```json
{
  "guestId": "guest-abc123",
  "userId": "user-id-456"
}
```

---

## Wishlist Endpoints

Wishlist data is stored in Redis with a 30-day TTL, keyed by user ID.

### Get Wishlist

```
GET /api/cart/wishlist/{userId}
```

**Response:** Array of wishlist items

### Add to Wishlist

```
POST /api/cart/wishlist/{userId}/items
```
```json
{
  "productId": "prod-1",
  "productName": "Product Name",
  "price": 29.99,
  "imageUrl": "/uploads/img.jpg"
}
```

### Remove from Wishlist

```
DELETE /api/cart/wishlist/{userId}/items/{productId}
```

### Check Wishlist Status

```
GET /api/cart/wishlist/{userId}/check/{productId}
```

**Response:** `true` or `false`

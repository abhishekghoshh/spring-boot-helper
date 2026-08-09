# Cart Service
Shopping cart and wishlist backed by Redis with MongoDB history.

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/cart/{cartId}` | Get cart |
| POST | `/api/cart/{cartId}/items` | Add item |
| DELETE | `/api/cart/{cartId}/items/{productId}` | Remove item |
| PUT | `/api/cart/{cartId}/items/{productId}?quantity=N` | Update quantity |
| DELETE | `/api/cart/{cartId}` | Clear cart |
| POST | `/api/cart/merge` | Merge guest cart |
| GET | `/api/cart/wishlist/{userId}` | Get wishlist |
| POST | `/api/cart/wishlist/{userId}/items` | Add to wishlist |
| DELETE | `/api/cart/wishlist/{userId}/items/{productId}` | Remove from wishlist |

Swagger: http://localhost:8083/swagger-ui.html

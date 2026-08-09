# Order Service
Order placement, history, cancellation, and checkout orchestration.

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/orders/checkout` | Checkout and place order |
| GET | `/api/orders/{orderId}` | Get order by ID |
| GET | `/api/orders/user/{userId}` | Get user's orders |
| POST | `/api/orders/{orderId}/cancel` | Cancel an order |

Swagger: http://localhost:8085/swagger-ui.html

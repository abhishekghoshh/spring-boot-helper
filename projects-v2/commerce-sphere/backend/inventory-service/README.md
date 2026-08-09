# Inventory Service
Stock and warehouse management with reservation support.

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/inventory/product/{productId}` | Get stock for product |
| POST | `/api/inventory/stock` | Add stock |
| POST | `/api/inventory/check` | Check availability |
| POST | `/api/inventory/reserve` | Reserve stock |
| GET | `/api/inventory/warehouses` | List warehouses |
| POST | `/api/inventory/warehouses` | Create warehouse |

Swagger: http://localhost:8084/swagger-ui.html

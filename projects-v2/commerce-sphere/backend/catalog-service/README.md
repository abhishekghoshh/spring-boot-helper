# Catalog Service

Product catalog management with MongoDB document storage and Redis caching.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/catalog/products` | Search products (pagination, filters) |
| GET | `/api/catalog/products/featured` | Featured products |
| GET | `/api/catalog/products/{id}` | Get product |
| POST | `/api/catalog/products` | Create product (admin) |
| PUT | `/api/catalog/products/{id}` | Update product (admin) |
| DELETE | `/api/catalog/products/{id}` | Delete product (admin) |
| POST | `/api/catalog/products/{id}/images` | Upload product image |
| GET | `/api/catalog/categories` | List categories |
| POST | `/api/catalog/categories` | Create category (admin) |
| GET | `/api/catalog/brands` | List brands |
| POST | `/api/catalog/brands` | Create brand (admin) |
| GET | `/api/catalog/reviews/product/{id}` | Product reviews |
| POST | `/api/catalog/reviews` | Create review |

Swagger UI: http://localhost:8082/swagger-ui.html

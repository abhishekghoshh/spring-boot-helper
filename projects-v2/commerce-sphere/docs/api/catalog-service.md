# Catalog Service API

Base URL: `http://localhost:8082` (Docker) or `http://localhost:8083` (from host via Docker Compose)

## Products

### Search / List Products

```
GET /api/catalog/products?page=0&size=20&sortBy=createdAt&sortDir=desc&active=true&featured=true&categoryId=xxx&minPrice=10&maxPrice=100
```

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "product-id",
      "sku": "SKU-001",
      "name": "Product Name",
      "description": "Product description",
      "price": 99.99,
      "currency": "USD",
      "categoryId": "cat-id",
      "categoryName": "Electronics",
      "brandId": "brand-id",
      "brandName": "TechPro",
      "imageUrls": ["/uploads/img.jpg"],
      "active": true,
      "featured": false,
      "createdAt": "2026-01-01T00:00:00Z",
      "updatedAt": "2026-01-01T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "last": false
}
```

### Get Product

```
GET /api/catalog/products/{id}
```

### Create Product

```
POST /api/catalog/products
```

```json
{
  "sku": "SKU-001",
  "name": "New Product",
  "description": "Description",
  "price": 49.99,
  "currency": "USD",
  "categoryId": "cat-id",
  "brandId": "brand-id",
  "active": true,
  "featured": false
}
```

### Update Product

```
PUT /api/catalog/products/{id}
```

### Delete Product

```
DELETE /api/catalog/products/{id}
```

### Upload Image

```
POST /api/catalog/products/{id}/images
Content-Type: multipart/form-data

file: <image file>
```

**Response:** Image URL string

---

## Categories

### List All

```
GET /api/catalog/categories
```

### Create Category

```
POST /api/catalog/categories
```
```json
{
  "name": "New Category",
  "slug": "new-category",
  "description": "Category description",
  "imageUrl": ""
}
```

### Update Category

```
PUT /api/catalog/categories/{id}
```

### Delete Category

```
DELETE /api/catalog/categories/{id}
```

---

## Brands

### List All

```
GET /api/catalog/brands
```

### Create Brand

```
POST /api/catalog/brands
```
```json
{
  "name": "New Brand",
  "description": "Brand description",
  "logoUrl": ""
}
```

---

## Reviews

### Get Reviews for Product

```
GET /api/catalog/reviews/product/{productId}?page=0&size=20
```

### Create Review

```
POST /api/catalog/reviews
```
```json
{
  "productId": "prod-id",
  "userId": "user-id",
  "userName": "John",
  "rating": 5,
  "comment": "Great product!"
}
```

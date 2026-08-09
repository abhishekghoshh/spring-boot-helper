# Inventory Service API

Base URL: `http://localhost:8084` (Docker) or `http://localhost:8085` (from host via Docker Compose)

## Inventory Endpoints

### Get Stock for Product

```
GET /api/inventory/product/{productId}
```

**Response:**
```json
[
  {
    "id": "inv-1",
    "productId": "prod-1",
    "productName": "Product Name",
    "warehouseId": "wh-1",
    "warehouseName": "Main Warehouse",
    "quantity": 100,
    "reservedQuantity": 5,
    "availableQuantity": 95
  }
]
```

### Add Stock

```
POST /api/inventory/stock?productId=prod-1&warehouseId=wh-1&productName=Product&quantity=50
```

**Response:** Updated inventory record

### Check Availability

```
POST /api/inventory/check
```
```json
{
  "productId": "prod-1",
  "quantity": 3
}
```

**Response:** `true` or `false`

### Reserve Inventory

```
POST /api/inventory/reserve
```
```json
{
  "productId": "prod-1",
  "orderId": "order-123",
  "quantity": 2
}
```

**Response:** Updated inventory with reserved quantity incremented

---

## Warehouse Endpoints

### List Warehouses

```
GET /api/inventory/warehouses
```

**Response:**
```json
[
  {
    "id": "wh-1",
    "name": "Main Warehouse",
    "code": "WH-MAIN-01",
    "street": "100 Commerce Blvd",
    "city": "San Francisco",
    "state": "CA",
    "zipCode": "94105",
    "country": "USA",
    "active": true
  }
]
```

### Create Warehouse

```
POST /api/inventory/warehouses
```
```json
{
  "name": "East Warehouse",
  "code": "WH-EAST-01",
  "street": "200 Industrial Dr",
  "city": "New York",
  "state": "NY",
  "zipCode": "10001",
  "country": "USA",
  "active": true
}
```

## Inventory Reservation Flow

1. Customer initiates checkout → Order Service calls `POST /api/inventory/check` to verify stock
2. Order is placed → Order Service calls `POST /api/inventory/reserve` to reserve stock
3. Stale reservations expire via Redis TTL (30 minutes)
4. Reservations are released when orders are cancelled

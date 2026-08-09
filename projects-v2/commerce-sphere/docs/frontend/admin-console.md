# Admin Console Guide

**URL:** http://localhost:3000  
**Login:** admin / Admin@123

The admin console provides full management capabilities for the CommerceSphere platform.

## Pages Overview

### Dashboard
Shows platform-wide statistics:
- Total Products
- Total Categories
- Total Brands
- Active Users

### Products
- View all products in a paginated table
- Search by product name
- Add new products (name, SKU, description, price, category, brand)
- Edit existing products
- Delete products

### Categories
- View all product categories
- Add new categories (name, slug, description)
- Delete categories

### Brands
- View all product brands
- Add new brands (name, description)
- Delete brands

### Inventory
- Add stock to products (product ID, warehouse ID, quantity)
- View warehouse list

### Orders
- View all orders with status
- View order details (items, totals, shipping address)
- Cancel orders (CONFIRMED status only)

### Customers
- View registered users with roles

### Settings
- Change admin password

## Tech Stack
- React 19.2 with TypeScript
- Material UI 6 for components and theming
- TanStack Query 5 for data fetching and caching
- React Router 7 for client-side routing
- Axios for HTTP requests with JWT interceptor
- Recharts for dashboard charts

## How to Access
1. Ensure all services are running: `docker compose up -d`
2. Open http://localhost:3000 in your browser
3. Login with username `admin` and password `Admin@123`

## API Communication
The admin console communicates with all 5 backend services through nginx proxy:
- Auth → identity-service on port 8081
- Catalog → catalog-service on port 8082
- Cart → cart-service on port 8083
- Inventory → inventory-service on port 8084
- Orders → order-service on port 8085

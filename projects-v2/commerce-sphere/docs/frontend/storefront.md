# Frontend Applications

## Admin Console

**URL:** http://localhost:3000  
**Default Login:** admin / Admin@123

The admin console is a React 19 dashboard for managing the CommerceSphere platform.

### Pages

| Page | Route | Description |
|------|-------|-------------|
| Login | `/login` | Admin authentication |
| Dashboard | `/dashboard` | Platform statistics (products, categories, brands, users) |
| Products | `/products` | Product list with search, CRUD operations |
| Product Form | `/products/new`, `/products/:id/edit` | Create/edit product forms |
| Categories | `/categories` | Category CRUD |
| Brands | `/brands` | Brand CRUD |
| Inventory | `/inventory` | Stock management |
| Orders | `/orders` | Order list |
| Order Detail | `/orders/:id` | Order details |
| Customers | `/customers` | User list |
| Settings | `/settings` | Password change |

### Tech Stack

- React 19 with TypeScript
- Material UI 6 for components
- TanStack Query 5 for server state
- React Router 7 for navigation
- Axios for API calls
- Recharts for dashboard charts

### API Integration

The nginx config in the Docker container proxies API requests to the appropriate backend services:
- `/api/auth/*` → identity-service:8081
- `/api/catalog/*` → catalog-service:8082
- `/api/cart/*` → cart-service:8083
- `/api/inventory/*` → inventory-service:8084
- `/api/orders/*` → order-service:8085

---

## Customer Storefront

**URL:** http://localhost:3001

The storefront is a customer-facing React 19 e-commerce application.

### Pages

| Page | Route | Auth Required |
|------|-------|---------------|
| Home | `/` | No |
| Product Listing | `/products` | No |
| Product Detail | `/products/:id` | No |
| Category Landing | `/category/:slug` | No |
| Search Results | `/search?q=...` | No |
| Shopping Cart | `/cart` | No (guest) |
| Wishlist | `/wishlist` | Yes |
| Checkout | `/checkout` | Yes |
| Order Confirmation | `/order-confirmation/:id` | Yes |
| Order History | `/orders` | Yes |
| Order Detail | `/orders/:id` | Yes |
| Login | `/login` | No |
| Register | `/register` | No |
| Forgot Password | `/forgot-password` | No |
| My Account | `/account` | Yes |

### Features

- **Guest browsing** — Products, categories, and search work without login
- **Guest cart** — Anonymous users can add items to a guest cart (stored in Redis via Cart Service)
- **Cart merge on login** — Guest cart items are merged when the user logs in
- **Wishlist** — Authenticated users can save items to wishlist
- **Checkout flow** — Cart → Checkout → Order Confirmation
- **Order history** — View past orders and their status

### Tech Stack

- React 19 with TypeScript
- Material UI 6 for components
- TanStack Query 5 for server state
- React Router 7 for navigation

### How to Access

1. Open http://localhost:3001 in your browser
2. Browse products without logging in
3. Add items to cart (guest cart)
4. Click "Login" and register a new account
5. Your guest cart items will be available
6. Proceed to checkout with your account

### API Integration

Same nginx proxy configuration as the Admin Console, routing API calls to the appropriate backend services.

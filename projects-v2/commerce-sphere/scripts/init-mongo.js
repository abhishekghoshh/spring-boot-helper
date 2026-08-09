// CommerceSphere MongoDB Initialization Script
// Creates databases and collections with indexes

// Switch to commerce sphere database
db = db.getSiblingDB('commercesphere');

// ============================================
// Identity Service Collections
// ============================================
db.createCollection('users');
db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ username: 1 }, { unique: true });

db.createCollection('roles');
db.roles.createIndex({ name: 1 }, { unique: true });

// Insert default roles
db.roles.insertMany([
  { name: 'ROLE_ADMIN', description: 'Administrator with full access' },
  { name: 'ROLE_USER', description: 'Regular customer user' }
]);

// Insert default admin user (password: Admin@123, bcrypt encoded)
db.users.insertOne({
  username: 'admin',
  email: 'admin@commercesphere.com',
  password: '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  firstName: 'Admin',
  lastName: 'User',
  enabled: true,
  roles: ['ROLE_ADMIN'],
  createdAt: new Date(),
  updatedAt: new Date()
});

// ============================================
// Catalog Service Collections
// ============================================
db.createCollection('products');
db.products.createIndex({ name: 'text', description: 'text' });
db.products.createIndex({ categoryId: 1 });
db.products.createIndex({ brandId: 1 });
db.products.createIndex({ price: 1 });
db.products.createIndex({ createdAt: -1 });

db.createCollection('categories');
db.categories.createIndex({ name: 1 }, { unique: true });
db.categories.createIndex({ slug: 1 }, { unique: true });

db.createCollection('brands');
db.brands.createIndex({ name: 1 }, { unique: true });

db.createCollection('reviews');
db.reviews.createIndex({ productId: 1 });
db.reviews.createIndex({ userId: 1 });
db.reviews.createIndex({ productId: 1, userId: 1 }, { unique: true });

// Insert default categories
db.categories.insertMany([
  { name: 'Electronics', slug: 'electronics', description: 'Electronic devices and gadgets', imageUrl: '', createdAt: new Date(), updatedAt: new Date() },
  { name: 'Clothing', slug: 'clothing', description: 'Apparel and fashion items', imageUrl: '', createdAt: new Date(), updatedAt: new Date() },
  { name: 'Home & Garden', slug: 'home-garden', description: 'Home improvement and garden supplies', imageUrl: '', createdAt: new Date(), updatedAt: new Date() },
  { name: 'Books', slug: 'books', description: 'Books and publications', imageUrl: '', createdAt: new Date(), updatedAt: new Date() },
  { name: 'Sports', slug: 'sports', description: 'Sports equipment and gear', imageUrl: '', createdAt: new Date(), updatedAt: new Date() }
]);

// Insert default brands
db.brands.insertMany([
  { name: 'TechPro', description: 'Premium electronics brand', logoUrl: '', createdAt: new Date(), updatedAt: new Date() },
  { name: 'StyleWear', description: 'Contemporary fashion brand', logoUrl: '', createdAt: new Date(), updatedAt: new Date() },
  { name: 'HomeEssentials', description: 'Quality home products', logoUrl: '', createdAt: new Date(), updatedAt: new Date() },
  { name: 'SportMax', description: 'Professional sports equipment', logoUrl: '', createdAt: new Date(), updatedAt: new Date() }
]);

// ============================================
// Cart Service Collections
// ============================================
db.createCollection('cartHistory');
db.cartHistory.createIndex({ userId: 1 });
db.cartHistory.createIndex({ createdAt: -1 });

// ============================================
// Inventory Service Collections
// ============================================
db.createCollection('inventory');
db.inventory.createIndex({ productId: 1, warehouseId: 1 }, { unique: true });
db.inventory.createIndex({ productId: 1 });

db.createCollection('warehouses');
db.warehouses.createIndex({ name: 1 }, { unique: true });

db.createCollection('stockMovements');
db.stockMovements.createIndex({ productId: 1 });
db.stockMovements.createIndex({ createdAt: -1 });

// Insert default warehouse
db.warehouses.insertOne({
  name: 'Main Warehouse',
  code: 'WH-MAIN-01',
  address: {
    street: '100 Commerce Blvd',
    city: 'San Francisco',
    state: 'CA',
    zipCode: '94105',
    country: 'USA'
  },
  active: true,
  createdAt: new Date(),
  updatedAt: new Date()
});

// ============================================
// Order Service Collections
// ============================================
db.createCollection('orders');
db.orders.createIndex({ userId: 1 });
db.orders.createIndex({ orderNumber: 1 }, { unique: true });
db.orders.createIndex({ status: 1 });
db.orders.createIndex({ createdAt: -1 });

print('CommerceSphere MongoDB initialization complete!');

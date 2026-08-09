// CommerceMesh MongoDB Initialization
db = db.getSiblingDB('commercemesh');

db.createCollection('products');
db.createCollection('categories');
db.createCollection('brands');
db.createCollection('reviews');
db.createCollection('carts');
db.createCollection('wishlists');

// Create indexes for products
db.products.createIndex({ name: 'text', description: 'text' });
db.products.createIndex({ category: 1 });
db.products.createIndex({ brand: 1 });

// Create indexes for categories
db.categories.createIndex({ slug: 1 }, { unique: true });

print('MongoDB initialized successfully');

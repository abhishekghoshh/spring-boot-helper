### Method-Level vs Class-Level @Transactional

**Method-Level** — Applied to specific methods. More granular control.

```java
@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    // ✅ Method-level: Only THIS method runs inside a transaction
    @Transactional
    public Product createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product = productRepository.save(product);

        AuditLog log = new AuditLog("PRODUCT_CREATED", product.getId());
        auditLogRepository.save(log);

        return product;
    }

    // ❌ No @Transactional — runs with auto-commit (each query is independent)
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    // ✅ Method-level with readOnly optimization
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContaining(keyword);
    }
}
```

**Generated SQL — Method-Level:**

```sql
-- createProduct() — has @Transactional
SET autocommit = 0;
START TRANSACTION;
Hibernate: insert into products (name, price) values (?, ?)
           -- Parameters: ['MacBook Pro', 2499.99]
Hibernate: insert into audit_log (action, entity_id) values (?, ?)
           -- Parameters: ['PRODUCT_CREATED', 1]
COMMIT;

-- getProduct() — no @Transactional → auto-commit mode
Hibernate: select p.id, p.name, p.price from products p where p.id=?
           -- Parameters: [1]
-- Each query auto-commits independently. No explicit BEGIN/COMMIT.

-- searchProducts() — @Transactional(readOnly = true)
SET autocommit = 0;
SET TRANSACTION READ ONLY;   -- ← Hibernate optimization hint
START TRANSACTION;
Hibernate: select p.id, p.name, p.price from products p
           where p.name like ?
           -- Parameters: ['%MacBook%']
COMMIT;
-- readOnly = true → Hibernate skips dirty checking (performance boost)
```

**Class-Level** — All public methods in the class run inside a transaction. Individual methods can override.

```java
@Service
@Transactional  // ✅ Class-level: ALL public methods are transactional
public class InventoryService {

    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    // Inherits class-level @Transactional (readOnly = false, default isolation)
    public void addStock(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        inv.setQuantity(inv.getQuantity() + quantity);
        inventoryRepository.save(inv);

        StockMovement movement = new StockMovement(productId, quantity, "IN");
        stockMovementRepository.save(movement);
    }

    // Inherits class-level @Transactional
    public void removeStock(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductId(productId).orElseThrow();
        if (inv.getQuantity() < quantity) {
            throw new OutOfStockException("Not enough stock");
        }
        inv.setQuantity(inv.getQuantity() - quantity);
        inventoryRepository.save(inv);

        StockMovement movement = new StockMovement(productId, -quantity, "OUT");
        stockMovementRepository.save(movement);
    }

    // ✅ OVERRIDES class-level: readOnly = true for better performance
    @Transactional(readOnly = true)
    public Inventory getStock(Long productId) {
        return inventoryRepository.findByProductId(productId).orElseThrow();
    }

    // ✅ OVERRIDES class-level: SERIALIZABLE isolation for critical operation
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transferStock(Long fromProductId, Long toProductId, int quantity) {
        removeStock(fromProductId, quantity);
        addStock(toProductId, quantity);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Class-Level vs Method-Level @Transactional:                                     │
│                                                                                  │
│  @Service                                                                        │
│  @Transactional            ← Class-Level (DEFAULT for all public methods)        │
│  public class InventoryService {                                                 │
│                                                                                  │
│      public void addStock() { ... }                                              │
│      │  └──→ Uses class-level defaults: readOnly=false, isolation=DEFAULT        │
│      │                                                                           │
│      public void removeStock() { ... }                                           │
│      │  └──→ Uses class-level defaults: readOnly=false, isolation=DEFAULT        │
│      │                                                                           │
│      @Transactional(readOnly = true)   ← Method-Level OVERRIDES class-level     │
│      public Inventory getStock() { ... }                                         │
│      │  └──→ Uses: readOnly=true (overridden)                                   │
│      │                                                                           │
│      @Transactional(isolation = Isolation.SERIALIZABLE)   ← OVERRIDES           │
│      public void transferStock() { ... }                                         │
│         └──→ Uses: isolation=SERIALIZABLE (overridden), readOnly=false (default)│
│  }                                                                               │
│                                                                                  │
│  Rule: Method-level @Transactional ALWAYS takes priority over class-level.       │
│  Tip: Use class-level for common defaults, override at method-level for          │
│       special cases (readOnly queries, stricter isolation).                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


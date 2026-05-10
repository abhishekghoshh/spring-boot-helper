### OneToOne Unidirectional Mapping

In a unidirectional `@OneToOne` mapping, only the **parent** entity has a reference to the **child** entity. The child has no knowledge of the parent.

```text
Java Objects:                              Database Tables:

┌───────────────────┐                      ┌───────────────────────────┐
│  User             │                      │  users                    │
│                   │                      ├───────────────────────────┤
│  id: Long         │                      │  id (PK, BIGINT)          │
│  name: String     │    @OneToOne         │  name (VARCHAR)           │
│  address: Address ├───────────────>      │  address_id (FK, BIGINT)  │──┐
│                   │                      │                           │  │
└───────────────────┘                      └───────────────────────────┘  │
                                                                          │ references
┌───────────────────┐                      ┌───────────────────────────┐  │
│  Address          │                      │  addresses                │  │
│                   │                      ├───────────────────────────┤  │
│  id: Long         │                      │  id (PK, BIGINT)          │<─┘
│  city: String     │                      │  city (VARCHAR)           │
│  zipCode: String  │                      │  zip_code (VARCHAR)       │
│                   │  (no ref to User)    │                           │
└───────────────────┘                      └───────────────────────────┘

Direction: User ──────> Address   (one way only)
           User knows Address.   Address does NOT know User.
```

**Code example**

```java
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    // constructors, getters, setters
    // NO reference to User — this is unidirectional
}
```

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;       // User has a reference to Address

    // constructors, getters, setters
}
```

**Usage**

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User createUserWithAddress() {
        Address address = new Address();
        address.setCity("Bangalore");
        address.setZipCode("560001");

        User user = new User();
        user.setName("Alice");
        user.setAddress(address);   // set the child on parent

        return userRepository.save(user);
        // CascadeType.ALL → saving User also saves Address automatically
    }
}
```

```text
SQL executed:
  INSERT INTO addresses (city, zip_code) VALUES ('Bangalore', '560001');     ← Address saved first (child)
  INSERT INTO users (name, address_id) VALUES ('Alice', 1);                  ← User saved with FK to Address
```

---

### What is Cascade Type

Cascade defines how **persistence operations on the parent entity propagate to the child entity**. Without cascading, you would need to explicitly save, update, and delete each entity individually.

```text
WITHOUT Cascade:                           WITH CascadeType.ALL:

userRepository.save(user);                 userRepository.save(user);
addressRepository.save(address);           // Address is saved automatically!
// You must manually save both

userRepository.delete(user);               userRepository.delete(user);
addressRepository.delete(address);         // Address is deleted automatically!
// You must manually delete both
```

**Why cascading is needed**

In a parent-child relationship, the child's lifecycle often depends on the parent:

```text
┌────────────┐    owns    ┌────────────┐
│   User     │ ──────────>│  Address   │
│  (parent)  │            │  (child)   │
└────────────┘            └────────────┘

Parent created  → Child should also be created  (CASCADE PERSIST)
Parent updated  → Child should also be updated  (CASCADE MERGE)
Parent deleted  → Child should also be deleted  (CASCADE REMOVE)
Parent detached → Child should also be detached (CASCADE DETACH)
Parent refreshed→ Child should also be refreshed(CASCADE REFRESH)
```

Without cascade, the child entity is an **orphan** — the parent has a reference to it but JPA does not know to propagate operations to it. You get errors like:

```text
org.hibernate.TransientObjectException:
  object references an unsaved transient instance — save the transient instance before flushing
```

This happens when you `persist(user)` but `address` is still in `NEW` state and was not persisted separately.

---

### All Cascade Types Explained

```text
┌──────────────────┬──────────────────────────────────────────────────────────────┐
│ CascadeType      │ What it does                                                │
├──────────────────┼──────────────────────────────────────────────────────────────┤
│ PERSIST          │ When parent is persisted, child is also persisted            │
│ MERGE            │ When parent is merged (updated), child is also merged        │
│ REMOVE           │ When parent is removed, child is also removed               │
│ REFRESH          │ When parent is refreshed from DB, child is also refreshed   │
│ DETACH           │ When parent is detached, child is also detached             │
│ ALL              │ All of the above combined                                    │
└──────────────────┴──────────────────────────────────────────────────────────────┘
```

**CascadeType.PERSIST**

```java
@OneToOne(cascade = CascadeType.PERSIST)
@JoinColumn(name = "address_id")
private Address address;
```

```java
Address address = new Address();
address.setCity("Bangalore");

User user = new User();
user.setName("Alice");
user.setAddress(address);

userRepository.save(user);
// Hibernate executes:
//   INSERT INTO addresses (city, zip_code) VALUES ('Bangalore', null);
//   INSERT INTO users (name, address_id) VALUES ('Alice', 1);
```

**CascadeType.MERGE**

```java
@OneToOne(cascade = CascadeType.MERGE)
@JoinColumn(name = "address_id")
private Address address;
```

```java
// user and address were previously saved, now detached
user.setName("Bob");
user.getAddress().setCity("Mumbai");      // modify child too

userRepository.save(user);               // save() calls merge() for existing entities
// Hibernate executes:
//   UPDATE users SET name = 'Bob' WHERE id = 1;
//   UPDATE addresses SET city = 'Mumbai' WHERE id = 1;
```

**CascadeType.REMOVE**

```java
@OneToOne(cascade = CascadeType.REMOVE)
@JoinColumn(name = "address_id")
private Address address;
```

```java
userRepository.delete(user);
// Hibernate executes:
//   DELETE FROM users WHERE id = 1;          ← parent deleted
//   DELETE FROM addresses WHERE id = 1;      ← child also deleted automatically
```

**CascadeType.REFRESH**

```java
@OneToOne(cascade = CascadeType.REFRESH)
@JoinColumn(name = "address_id")
private Address address;
```

```java
// Someone updated the address directly in DB
entityManager.refresh(user);
// Hibernate re-reads BOTH user AND address from DB
// Overwrites any in-memory changes with database values
```

**CascadeType.DETACH**

```java
@OneToOne(cascade = CascadeType.DETACH)
@JoinColumn(name = "address_id")
private Address address;
```

```java
entityManager.detach(user);
// Both user AND address become DETACHED from persistence context
// Changes to either will NOT be auto-detected by dirty checking
```

**CascadeType.ALL**

```java
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "address_id")
private Address address;
```

Equivalent to `{PERSIST, MERGE, REMOVE, REFRESH, DETACH}`. All operations propagate.

**When to use which Cascade type**

| Cascade Type | When to use |
|---|---|
| `PERSIST` | Child should be auto-saved when parent is saved. Use when child cannot exist without parent. |
| `MERGE` | Child should be auto-updated when parent is updated. Common for parent-child forms. |
| `REMOVE` | Child should be auto-deleted when parent is deleted. Use for owned entities (address, profile). |
| `REFRESH` | Need to reload the entire object graph from DB. Rare — used in long conversations or concurrent environments. |
| `DETACH` | Need to detach the entire graph. Rare. |
| `ALL` | Child lifecycle is fully owned by parent. **Most common in industry for @OneToOne.** |

**Which Cascade is used most in industry?**

```text
@OneToOne  →  CascadeType.ALL          (child lifecycle = parent lifecycle)
@OneToMany →  CascadeType.ALL          (children owned by parent)
@ManyToOne →  Usually NO cascade       (many children share one parent — don't cascade delete!)
@ManyToMany→  CascadeType.PERSIST + MERGE (shared entities, never cascade remove)
```

`CascadeType.ALL` is the most common for `@OneToOne` because the child entity (Address, Profile, Passport) is fully owned by the parent (User). If the User is deleted, the Address should also be deleted. If the User is saved, the Address should be saved too.

---

### How OneToOne Mapping Converts to Table Structure

When Hibernate encounters `@OneToOne` on a field, it creates a **foreign key column** in the **parent table** that points to the **primary key** of the child table.

```text
@Entity User has:
    @OneToOne
    private Address address;

Hibernate generates:

┌──────────────────────────────┐          ┌─────────────────────────┐
│  users                       │          │  addresses              │
├──────────────────────────────┤          ├─────────────────────────┤
│  id          BIGINT (PK)     │          │  id       BIGINT (PK)   │
│  name        VARCHAR(255)    │          │  city     VARCHAR(255)   │
│  address_id  BIGINT (FK) ────┼─────────>│  zip_code VARCHAR(255)  │
│              UNIQUE          │          │                         │
└──────────────────────────────┘          └─────────────────────────┘
```

**How Hibernate decides the foreign key column name**

```text
Default naming convention:

  Field name in Java:   address
  Separator:            _
  Referenced PK column: id

  FK column name:       address_id
```

So `User.address` becomes `address_id` in the `users` table, referencing the `id` column in the `addresses` table.

**How Hibernate automatically chooses the primary key of the referred table**

```text
@OneToOne
private Address address;

Hibernate:
  1. Looks at the type of the field → Address.class
  2. Finds @Entity Address
  3. Finds the field marked with @Id in Address → "id"
  4. Uses that as the referencedColumnName for the FK
  5. Creates: address_id BIGINT REFERENCES addresses(id)
```

If `Address` had `@Id private Long addressId`, Hibernate would still reference that field because `@Id` is what matters, not the field name.

Generated DDL:
```sql
CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city VARCHAR(255),
    zip_code VARCHAR(255)
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    address_id BIGINT UNIQUE,
    CONSTRAINT fk_users_address FOREIGN KEY (address_id) REFERENCES addresses(id)
);
```

The `UNIQUE` constraint on `address_id` enforces the "one-to-one" relationship — no two users can share the same address.

---

### @JoinColumn for More Control

`@JoinColumn` gives you explicit control over the foreign key column name, referenced column, nullability, and constraints.

**Properties of @JoinColumn**

| Property | Purpose | Default |
|---|---|---|
| `name` | FK column name in the parent table | `<field>_<referenced_pk>` |
| `referencedColumnName` | Which column in the child table the FK points to | The `@Id` column |
| `nullable` | Allow NULL in the FK column | `true` |
| `unique` | Add UNIQUE constraint on the FK column | `true` for `@OneToOne` |
| `insertable` | Include in INSERT statements | `true` |
| `updatable` | Include in UPDATE statements | `true` |
| `columnDefinition` | Raw DDL for the column | Provider-inferred |
| `foreignKey` | Customize the FK constraint name | Auto-generated |

**Code example with full control**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(
        name = "addr_id",                              // custom FK column name
        referencedColumnName = "id",                    // points to addresses.id
        nullable = false,                               // NOT NULL — every user MUST have an address
        unique = true,                                  // UNIQUE — one address per user
        foreignKey = @ForeignKey(name = "fk_user_addr") // custom FK constraint name
    )
    private Address address;
}
```

Generated DDL:
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    addr_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_user_addr FOREIGN KEY (addr_id) REFERENCES addresses(id)
);
```

**Without @JoinColumn vs with @JoinColumn**

```text
Without @JoinColumn:
  @OneToOne
  private Address address;
  → FK column: address_id (auto-generated name)
  → References: addresses.id (auto-detected from @Id)
  → Nullable: true
  → Constraint name: auto-generated (random-looking)

With @JoinColumn:
  @OneToOne
  @JoinColumn(name = "addr_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_addr"))
  private Address address;
  → FK column: addr_id (you chose it)
  → References: addresses.id
  → Nullable: false
  → Constraint name: fk_user_addr (you chose it)
```

---

### @JoinColumns for Composite Key References

When the child entity has a **composite primary key** (using `@IdClass`), the parent must reference **all columns** of that composite key. You use `@JoinColumns` (plural) with multiple `@JoinColumn` entries.

**Step 1: Child entity with composite key using @IdClass**

```java
// ID class
public class WarehouseLocationId implements Serializable {

    private String warehouseCode;
    private String zoneCode;

    public WarehouseLocationId() {}

    public WarehouseLocationId(String warehouseCode, String zoneCode) {
        this.warehouseCode = warehouseCode;
        this.zoneCode = zoneCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarehouseLocationId that = (WarehouseLocationId) o;
        return Objects.equals(warehouseCode, that.warehouseCode)
            && Objects.equals(zoneCode, that.zoneCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseCode, zoneCode);
    }
}
```

```java
// Child entity with composite PK
@Entity
@Table(name = "warehouse_locations")
@IdClass(WarehouseLocationId.class)
public class WarehouseLocation {

    @Id
    @Column(name = "warehouse_code")
    private String warehouseCode;           // part of composite PK

    @Id
    @Column(name = "zone_code")
    private String zoneCode;                // part of composite PK

    private String description;
    private Integer capacity;

    // constructors, getters, setters
}
```

**Step 2: Parent entity referencing the composite key**

```java
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumns({
        @JoinColumn(
            name = "wh_code",                                  // FK column in inventory_items
            referencedColumnName = "warehouse_code"             // PK column in warehouse_locations
        ),
        @JoinColumn(
            name = "wh_zone",                                  // FK column in inventory_items
            referencedColumnName = "zone_code"                  // PK column in warehouse_locations
        )
    })
    private WarehouseLocation location;

    // constructors, getters, setters
}
```

**Resulting table structure**

```text
┌─────────────────────────────────┐          ┌──────────────────────────────────┐
│  inventory_items                │          │  warehouse_locations             │
├─────────────────────────────────┤          ├──────────────────────────────────┤
│  id            BIGINT (PK)      │          │  warehouse_code VARCHAR (PK)     │
│  product_name  VARCHAR(255)     │          │  zone_code      VARCHAR (PK)     │
│  wh_code       VARCHAR (FK) ────┼──┐       │  description    VARCHAR(255)     │
│  wh_zone       VARCHAR (FK) ────┼──┼──────>│  capacity       INT              │
│                                 │  │       │                                  │
│  UNIQUE(wh_code, wh_zone)       │  │       │  PRIMARY KEY (warehouse_code,    │
│                                 │  │       │               zone_code)         │
└─────────────────────────────────┘  │       └──────────────────────────────────┘
                                     │
                          Both FK columns together
                          reference the composite PK
```

Generated DDL:
```sql
CREATE TABLE warehouse_locations (
    warehouse_code VARCHAR(255) NOT NULL,
    zone_code VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    capacity INT,
    PRIMARY KEY (warehouse_code, zone_code)
);

CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255),
    wh_code VARCHAR(255),
    wh_zone VARCHAR(255),
    CONSTRAINT fk_inventory_location
        FOREIGN KEY (wh_code, wh_zone)
        REFERENCES warehouse_locations(warehouse_code, zone_code)
);
```

**Usage**

```java
@Transactional
public void createInventory() {
    WarehouseLocation location = new WarehouseLocation();
    location.setWarehouseCode("WH-BLR-001");
    location.setZoneCode("ZONE-A");
    location.setDescription("Electronics Zone");
    location.setCapacity(500);

    InventoryItem item = new InventoryItem();
    item.setProductName("Laptop");
    item.setLocation(location);

    inventoryItemRepository.save(item);
    // INSERT INTO warehouse_locations (warehouse_code, zone_code, description, capacity)
    //   VALUES ('WH-BLR-001', 'ZONE-A', 'Electronics Zone', 500);
    // INSERT INTO inventory_items (product_name, wh_code, wh_zone)
    //   VALUES ('Laptop', 'WH-BLR-001', 'ZONE-A');
}
```

---


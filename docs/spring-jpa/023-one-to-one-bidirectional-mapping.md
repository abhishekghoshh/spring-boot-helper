### OneToOne Bidirectional Mapping

In a bidirectional `@OneToOne` mapping, **both** entities have a reference to each other. The parent knows the child, and the child knows the parent.

```text
Java Objects:                              Database Tables:

┌───────────────────┐                      ┌───────────────────────────┐
│  User             │                      │  users                    │
│                   │                      ├───────────────────────────┤
│  id: Long         │    @OneToOne         │  id (PK, BIGINT)          │
│  name: String     ├───────────────>      │  name (VARCHAR)           │
│  address: Address │                      │  address_id (FK, BIGINT)  │──┐
│                   │<───────────────┐     │                           │  │
└───────────────────┘                │     └───────────────────────────┘  │
                                     │                                    │
┌───────────────────┐                │     ┌───────────────────────────┐  │
│  Address          │                │     │  addresses                │  │
│                   │  @OneToOne     │     ├───────────────────────────┤  │
│  id: Long         │  (mappedBy)   │     │  id (PK, BIGINT)          │<─┘
│  city: String     ├───────────────┘     │  city (VARCHAR)           │
│  zipCode: String  │                      │  zip_code (VARCHAR)       │
│  user: User       │  ← back reference   │                           │
│                   │                      │  (NO FK column here!)     │
└───────────────────┘                      └───────────────────────────┘

Direction: User <────────> Address   (both ways)
           User knows Address.   Address also knows User.
```

---

### Difference from Unidirectional

| | Unidirectional | Bidirectional |
|---|---|---|
| **Parent (User)** | Has `@OneToOne Address address` | Has `@OneToOne Address address` |
| **Child (Address)** | No reference to User | Has `@OneToOne(mappedBy="address") User user` |
| **Navigation** | User → Address only | User → Address AND Address → User |
| **Table structure** | Same — FK in users table | **Same** — FK in users table only |
| **Extra FK in child table?** | No | **No** — `mappedBy` prevents it |
| **Java code difference** | Address has no User field | Address has a User field with `mappedBy` |

**Critical point**: Adding bidirectional mapping **does NOT change the table structure**. The foreign key still exists only in the **owner** table (users). The `mappedBy` side (Address) does not add a foreign key column.

```text
Unidirectional tables:                    Bidirectional tables:
┌──────────┐    ┌──────────┐             ┌──────────┐    ┌──────────┐
│ users    │    │ addresses│             │ users    │    │ addresses│
│ id (PK)  │    │ id (PK)  │             │ id (PK)  │    │ id (PK)  │
│ name     │    │ city     │             │ name     │    │ city     │
│ addr_id──┼───>│ zip_code │             │ addr_id──┼───>│ zip_code │
└──────────┘    └──────────┘             └──────────┘    └──────────┘

IDENTICAL table structure!  The difference is only in Java — Address now has a User field.
```

---

### Owner Side and Inverse Side

In a bidirectional relationship, JPA requires you to designate:

- **Owner side**: The entity whose table holds the **foreign key column**. This side controls the relationship.
- **Inverse side**: The entity that mirrors the relationship using `mappedBy`. This side does NOT have a FK column.

```text
┌──────────────────────┐                    ┌──────────────────────────────┐
│  User (OWNER)        │                    │  Address (INVERSE)           │
│                      │                    │                              │
│  @OneToOne           │                    │  @OneToOne(mappedBy="address")│
│  @JoinColumn(...)    │                    │  private User user;          │
│  private Address     │                    │                              │
│     address;         │                    │  (no @JoinColumn here)       │
│                      │                    │  (no FK in addresses table)  │
│  → FK: address_id    │                    │                              │
│    in users table    │                    │  "I'm just the mirror"       │
└──────────────────────┘                    └──────────────────────────────┘
```

**Who is the owner?**

The entity that has `@JoinColumn` (the FK column) is the owner. The entity that has `mappedBy` is the inverse.

**Code example**

```java
// OWNER SIDE — User holds the FK
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")   // FK here
    private Address address;

    // constructors, getters, setters
}
```

```java
// INVERSE SIDE — Address mirrors the relationship
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @OneToOne(mappedBy = "address")      // "address" = field name in User class
    private User user;                    // back reference to User

    // constructors, getters, setters
}
```

---

### Why We Need `mappedBy`

Without `mappedBy`, JPA treats both sides as **independent** owner relationships and creates **two FK columns** — one in each table.

```text
WITHOUT mappedBy (WRONG):

@Entity User:
    @OneToOne @JoinColumn(name = "address_id")
    private Address address;

@Entity Address:
    @OneToOne @JoinColumn(name = "user_id")
    private User user;

Result — TWO foreign keys:
┌──────────────────┐         ┌─────────────────────┐
│ users            │         │ addresses            │
│ id (PK)          │         │ id (PK)              │
│ name             │         │ city                 │
│ address_id (FK) ─┼────────>│ zip_code             │
│                  │<────────┼─ user_id (FK)        │
└──────────────────┘         └─────────────────────┘

Two FK columns = data duplication, sync issues, potential inconsistency.
```

```text
WITH mappedBy (CORRECT):

@Entity User:
    @OneToOne @JoinColumn(name = "address_id")
    private Address address;

@Entity Address:
    @OneToOne(mappedBy = "address")       // points to User.address field
    private User user;

Result — ONE foreign key only:
┌──────────────────┐         ┌─────────────────────┐
│ users            │         │ addresses            │
│ id (PK)          │         │ id (PK)              │
│ name             │         │ city                 │
│ address_id (FK) ─┼────────>│ zip_code             │
│                  │         │                      │
└──────────────────┘         └─────────────────────┘

One FK = clean, no duplication, single source of truth.
```

`mappedBy = "address"` tells Hibernate: "Don't create a FK here. The relationship is already managed by the `address` field in the `User` entity. I'm just a mirror."

---

### Real-World Example — Getting User from Address

**Use case**: User → Address is the primary mapping. But sometimes you want to know which User lives at a given Address — that requires Address → User navigation.

```java
// OWNER
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address address;

    // getters, setters
}
```

```java
// INVERSE
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @OneToOne(mappedBy = "address")
    private User user;              // back reference

    // getters, setters
}
```

**Navigate both directions**

```java
@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public void findUserByAddress(Long addressId) {
        Address address = addressRepository.findById(addressId).orElseThrow();

        // Navigate from Address → User (bidirectional)
        User user = address.getUser();
        System.out.println("User at this address: " + user.getName());
    }
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public void findAddressByUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        // Navigate from User → Address (standard direction)
        Address address = user.getAddress();
        System.out.println("User lives at: " + address.getCity());
    }
}
```

**SQL for Address → User navigation**

```text
addressRepository.findById(1L)
  → SELECT a.* FROM addresses a WHERE a.id = 1
  → Then (eager or lazy): SELECT u.* FROM users u WHERE u.address_id = 1
```

---

### Infinite Recursion Problem in Bidirectional Mapping

When you return a bidirectional entity directly as a REST response, Jackson serializes User → Address → User → Address → ... infinitely.

```text
Jackson serializes User:
{
  "id": 1,
  "name": "Alice",
  "address": {                        ← serializes Address
    "id": 10,
    "city": "Bangalore",
    "user": {                          ← serializes User AGAIN
      "id": 1,
      "name": "Alice",
      "address": {                     ← serializes Address AGAIN
        "id": 10,
        "city": "Bangalore",
        "user": {                      ← INFINITE LOOP
          ...
        }
      }
    }
  }
}

Result: StackOverflowError or HttpMessageNotWritableException
```

```text
The recursion cycle:

User.address → Address.user → User.address → Address.user → ...
     │              │              │              │
     └──────────────┘              └──────────────┘
         cycle 1                       cycle 2     → StackOverflow
```

---

### Solution 1: @JsonManagedReference and @JsonBackReference

This pair tells Jackson which side to serialize (managed = forward) and which to skip (back = ignored during serialization).

```text
@JsonManagedReference  →  "Serialize this side" (included in JSON)
@JsonBackReference     →  "Do NOT serialize this side" (excluded from JSON)
```

**Code example**

```java
// OWNER — User (forward reference, serialized)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    @JsonManagedReference                    // ← serialize this side
    private Address address;
}
```

```java
// INVERSE — Address (back reference, NOT serialized)
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @OneToOne(mappedBy = "address")
    @JsonBackReference                       // ← do NOT serialize this side
    private User user;
}
```

**JSON response when fetching User**

```json
{
    "id": 1,
    "name": "Alice",
    "address": {
        "id": 10,
        "city": "Bangalore",
        "zipCode": "560001"
    }
}
```

Address is included in the User response, but `user` field inside Address is **skipped** → no infinite recursion.

**JSON response when fetching Address directly**

```json
{
    "id": 10,
    "city": "Bangalore",
    "zipCode": "560001"
}
```

The `user` field is **not included** because `@JsonBackReference` always omits it. You **cannot** see User from the Address response.

**Where to put which annotation and why**

| Annotation | Put on | Why |
|---|---|---|
| `@JsonManagedReference` | Parent side (User.address) | This is the "forward" direction you typically serialize |
| `@JsonBackReference` | Child side (Address.user) | This is the "back" direction that causes the recursion |

**Limitation**: `@JsonBackReference` **always** hides the annotated field. When you fetch Address, you can never see its User in JSON. If you need both directions visible in the response, use `@JsonIdentityInfo`.

---

### Solution 2: @JsonIdentityInfo

`@JsonIdentityInfo` handles recursion by serializing the **full object the first time** and only its **ID** on subsequent references. This way, both directions are visible.

```java
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@Entity
@Table(name = "users")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"                          // use the @Id field as the identity
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address address;                 // no @JsonManagedReference needed
}
```

```java
@Entity
@Table(name = "addresses")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"                          // use the @Id field as the identity
)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @OneToOne(mappedBy = "address")
    private User user;                       // no @JsonBackReference needed
}
```

**JSON response when fetching User**

```json
{
    "id": 1,
    "name": "Alice",
    "address": {
        "id": 10,
        "city": "Bangalore",
        "zipCode": "560001",
        "user": 1
    }
}
```

When Jackson encounters User for the second time (inside Address.user), it replaces the full object with just its `id` value (`1`). No infinite recursion, and **both directions are visible**.

**JSON response when fetching Address**

```json
{
    "id": 10,
    "city": "Bangalore",
    "zipCode": "560001",
    "user": {
        "id": 1,
        "name": "Alice",
        "address": 10
    }
}
```

When Jackson encounters Address for the second time (inside User.address), it replaces it with just `10`.

**How @JsonIdentityInfo works**

```text
First encounter of User(id=1):
  → Serialize full object: { "id": 1, "name": "Alice", "address": {...} }
  → Register: User#1 = already serialized

Inside Address, encounter User(id=1) again:
  → Already serialized! Output just the id: 1
  → No recursion!
```

**Why use it on the @Id field**

The `@Id` field is unique for every entity instance. Using it as the identity marker ensures no collisions. `ObjectIdGenerators.PropertyGenerator.class` tells Jackson "use an existing property of the object (id) as the identity, don't generate a synthetic one."

---

### When to Use Which Solution

| Solution | Behavior | Use when |
|---|---|---|
| `@JsonManagedReference` + `@JsonBackReference` | Forward side serialized, back side always hidden | You only need one direction in the response (most common) |
| `@JsonIdentityInfo` | Both sides serialized, second occurrence replaced with ID | You need both directions visible, API consumers can resolve IDs |
| DTO (recommended) | Full control, no annotations on entity | Production APIs — cleanest, most flexible, no entity leakage |

**Summary of all approaches**

```text
Problem: User → Address → User → Address → ... (infinite recursion)

Solution 1: @JsonManagedReference + @JsonBackReference
  User → Address ✅ (full object)
  Address → User ❌ (always hidden)

Solution 2: @JsonIdentityInfo
  User → Address ✅ (full object first time, ID on repeat)
  Address → User ✅ (full object first time, ID on repeat)

Solution 3: DTO (best practice)
  UserDTO → AddressDTO ✅ (exactly what you want)
  AddressDTO → UserDTO ✅ (if you include it)
  No recursion, no proxies, no Jackson annotations on entities
```


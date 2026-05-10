### @GeneratedValue and Generation Strategies

`@GeneratedValue` tells JPA **how** to auto-generate the primary key value. It works only with single `@Id` fields — **not** with composite keys (`@EmbeddedId` / `@IdClass`).

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**All strategies**

```text
┌───────────────────┬───────────────────────────────────────────────────────┐
│ Strategy          │ How it generates IDs                                  │
├───────────────────┼───────────────────────────────────────────────────────┤
│ IDENTITY          │ DB auto-increment column (MySQL AUTO_INCREMENT)       │
│ SEQUENCE          │ DB sequence object (PostgreSQL, Oracle)               │
│ TABLE             │ Simulates sequence using a dedicated DB table         │
│ AUTO              │ Hibernate picks the best strategy for the DB dialect  │
└───────────────────┴───────────────────────────────────────────────────────┘
```

---

**GenerationType.IDENTITY**

The database generates the ID using an auto-increment column. Hibernate gets the ID **after** the INSERT.

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal price;
}
```

```text
How IDENTITY works:

em.persist(product)
     │
     ├─ product.id is NULL at this point
     │
     ├─ Hibernate MUST execute INSERT immediately
     │   (cannot defer — it needs the generated ID)
     │
     ├─ INSERT INTO products (name, price) VALUES ('Widget', 9.99)
     │
     ├─ DB returns generated ID (e.g., 42)
     │
     └─ product.id = 42 (now assigned)
```

Generated DDL (MySQL):
```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    price DECIMAL(19,2)
);
```

| Pros | Cons |
|---|---|
| Simple, no extra DB objects needed | INSERT cannot be batched (must happen immediately to get the ID) |
| Works well with MySQL | Breaks Hibernate's write-behind optimization |
| No allocationSize tuning needed | Not available in all databases (Oracle pre-12c had no identity columns) |

**Use when**: Simple apps with MySQL, low write throughput, batch insert performance is not a concern.

---

**GenerationType.SEQUENCE**

Uses a database sequence object to generate IDs. Hibernate fetches the next value from the sequence **before** the INSERT, so the ID is known immediately and INSERTs can be batched.

```java
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "emp_seq_gen"                  // references @SequenceGenerator.name
    )
    @SequenceGenerator(
        name = "emp_seq_gen",                       // logical name
        sequenceName = "employee_seq",              // actual DB sequence
        allocationSize = 50                         // pre-fetch 50 IDs at a time
    )
    private Long id;

    private String name;
}
```

```text
How SEQUENCE works:

em.persist(employee)
     │
     ├─ Hibernate calls: SELECT nextval('employee_seq')  → returns 1
     │   (IDs 1–50 now reserved in memory)
     │
     ├─ employee.id = 1 (assigned immediately, before INSERT)
     │
     ├─ INSERT is QUEUED in ActionQueue (not executed yet!)
     │
     ├─ em.persist(employee2)
     │   employee2.id = 2 (from memory, no DB call)
     │
     ├─ ... persist 48 more ...
     │   all IDs 3–50 from memory, zero DB round-trips
     │
     ├─ em.persist(employee51)
     │   Hibernate calls: SELECT nextval('employee_seq')  → returns 51
     │   IDs 51–100 now reserved
     │
     └─ flush() → BATCH INSERT all 51 rows in one go
```

```text
Why SEQUENCE + @SequenceGenerator is heavily used in industry:

1. BATCH INSERTS:   ID is known before INSERT → Hibernate can batch multiple INSERTs
2. PERFORMANCE:     allocationSize=50 means 1 DB call per 50 inserts (vs 1 per insert with IDENTITY)
3. PORTABILITY:     Sequences work across PostgreSQL, Oracle, H2, SQL Server
4. WRITE-BEHIND:    Hibernate can optimize flush order since it doesn't need immediate INSERT
5. STANDARD:        JPA standard, supported by all JPA providers
```

**What is the `generator` property?**

`generator` in `@GeneratedValue(generator = "emp_seq_gen")` is a **logical name** that links to the `@SequenceGenerator(name = "emp_seq_gen")`. It tells Hibernate "use this specific sequence configuration to generate IDs".

```text
@GeneratedValue(generator = "emp_seq_gen")  ─── links to ──→  @SequenceGenerator(name = "emp_seq_gen")
                                                                      │
                                                               sequenceName = "employee_seq"
                                                               allocationSize = 50
```

| Pros | Cons |
|---|---|
| Supports JDBC batch inserts | Requires a sequence object in the DB |
| allocationSize reduces DB calls | allocationSize mismatch between app and DB sequence INCREMENT causes ID gaps |
| ID available before INSERT | Slightly more setup than IDENTITY |
| Works with all major DBs | — |

**Use when**: Production applications, especially with PostgreSQL/Oracle. This is the **recommended default** for most JPA projects.

---

**GenerationType.TABLE**

Simulates a sequence by using a dedicated table that stores the last generated ID. Hibernate locks a row, reads the current value, increments it, and uses it as the new ID.

```java
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(
        strategy = GenerationType.TABLE,
        generator = "invoice_table_gen"
    )
    @TableGenerator(
        name = "invoice_table_gen",
        table = "id_generator",              // the table that stores IDs
        pkColumnName = "gen_name",           // column that holds the generator name
        valueColumnName = "gen_value",       // column that holds the current ID value
        pkColumnValue = "invoice_id",        // value in gen_name for this entity
        allocationSize = 50
    )
    private Long id;

    private BigDecimal amount;
}
```

```text
The id_generator table looks like:

+──────────────+───────────+
│  gen_name    │ gen_value  │
+──────────────+───────────+
│  invoice_id  │  1050      │
│  order_id    │  5023      │
│  user_id     │  340       │
+──────────────+───────────+

How TABLE strategy works:

em.persist(invoice)
     │
     ├─ BEGIN TRANSACTION on id_generator table
     ├─ SELECT gen_value FROM id_generator WHERE gen_name = 'invoice_id' FOR UPDATE
     │   → returns 1050
     ├─ UPDATE id_generator SET gen_value = 1100 WHERE gen_name = 'invoice_id'
     ├─ COMMIT
     │
     ├─ invoice.id = 1051 (IDs 1051–1100 reserved in memory)
     │
     └─ INSERT INTO invoices ...
```

| Pros | Cons |
|---|---|
| Works on ALL databases (even those without sequences) | **Row-level locking** on the generator table → contention under load |
| Portable | Worst performance of all strategies |
| One table for all generators | Extra table to maintain |

**Why it is NOT used much**

- The generator table becomes a **bottleneck** under concurrent writes — every ID allocation locks a row.
- Most modern databases support sequences natively (PostgreSQL, Oracle, SQL Server, H2).
- `SEQUENCE` strategy gives the same benefits without the locking overhead.
- Only consider `TABLE` if you must support a database that has **no sequence support** (e.g., very old MySQL versions before 8.0).

---

**Strategy comparison**

```text
                    IDENTITY       SEQUENCE        TABLE
───────────────── ──────────── ──────────────── ────────────
ID known before
INSERT?              No            Yes             Yes

Batch INSERT
supported?           No            Yes             Yes

DB round-trips
per N inserts        N             N/alloc_size    N/alloc_size + lock

Requires extra
DB object?           No            Sequence        Table

Locking overhead     None          None            Row lock on generator table

Performance rank     ★★★           ★★★★★           ★★

Recommended?         Simple apps   Production      Last resort
                     MySQL         default
```


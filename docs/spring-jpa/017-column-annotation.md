### @Column Annotation

`@Column` maps an entity field to a specific database column. It is **optional** — if omitted, Hibernate maps the field name directly to a column of the same name.

**Properties of @Column**

| Property | Purpose | Default |
|---|---|---|
| `name` | Column name in DB | Field name |
| `unique` | Adds a unique constraint on this column | `false` |
| `nullable` | Whether the column allows NULL | `true` |
| `length` | Column length (for `VARCHAR`) | `255` |
| `columnDefinition` | Raw DDL for the column type | Provider-inferred |
| `precision` | Total digits for `DECIMAL` | `0` |
| `scale` | Decimal digits for `DECIMAL` | `0` |
| `insertable` | Include in INSERT statements | `true` |
| `updatable` | Include in UPDATE statements | `true` |

**Examples**

```java
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String name;
    // → full_name VARCHAR(100) NOT NULL

    @Column(unique = true, nullable = false, length = 150)
    private String email;
    // → email VARCHAR(150) NOT NULL UNIQUE

    @Column(nullable = true, length = 500)
    private String bio;
    // → bio VARCHAR(500) NULL

    @Column(precision = 10, scale = 2)
    private BigDecimal salary;
    // → salary DECIMAL(10,2)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    // → created_at TIMESTAMP — excluded from UPDATE statements

    @Column(columnDefinition = "TEXT")
    private String notes;
    // → notes TEXT (raw DDL override)
}
```

Generated DDL:
```sql
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    bio VARCHAR(500),
    salary DECIMAL(10,2),
    created_at TIMESTAMP,
    notes TEXT
);
```

---

### GOLD: insertable/updatable cases + portability warning + Q&A

When to use `insertable = false, updatable = false`:

```java
@Column(name = "created_at", updatable = false) // audit column set once
private LocalDateTime createdAt;

@Column(name = "dept_id", insertable = false, updatable = false) // read-only FK mirror of a @ManyToOne
private Long deptId;
```

Portability warning: `columnDefinition = "TEXT"` is raw DDL — valid on Postgres/MySQL but breaks on Oracle/others. Prefer portable attributes (`length`, `precision`) and reserve `columnDefinition` for dialect-specific migrations.

**Interview Q&A**

1. **Q: insertable=false vs updatable=false?** A: `insertable=false` excludes the column from INSERT (DB default fills it); `updatable=false` excludes it from UPDATE (e.g. `createdAt`). Both together make a read-only mapping.
2. **Q: Why is columnDefinition risky?** A: It embeds database-specific DDL, killing portability — a `TEXT` definition that works on Postgres may fail schema validation on Oracle.


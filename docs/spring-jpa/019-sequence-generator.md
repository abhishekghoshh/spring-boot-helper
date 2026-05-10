### @SequenceGenerator

`@SequenceGenerator` configures a database **sequence** that generates unique numeric IDs. A sequence is a database-level object that produces auto-incrementing numbers independently of any table.

```text
┌────────────────────────┐
│  Database Sequence     │
│  employee_seq          │
│                        │
│  nextval() → 1         │
│  nextval() → 2         │
│  nextval() → 3         │
│  ...                   │
└────────────────────────┘
```

**Properties of @SequenceGenerator**

| Property | Purpose | Default |
|---|---|---|
| `name` | Logical name used by `@GeneratedValue(generator = "...")` | Required |
| `sequenceName` | Actual sequence name in the database | Generator name |
| `initialValue` | Starting value of the sequence | `1` |
| `allocationSize` | How many IDs Hibernate pre-fetches per DB call (batch) | `50` |
| `schema` | Database schema containing the sequence | Default schema |

**allocationSize explained**

```text
allocationSize = 50 (default):

Hibernate calls: SELECT nextval('employee_seq')  →  returns 1
Hibernate now has IDs 1–50 in memory (no more DB calls needed for next 49 inserts)

After 50 persists:
Hibernate calls: SELECT nextval('employee_seq')  →  returns 51
Hibernate now has IDs 51–100 in memory

Benefit: 1 DB round-trip per 50 inserts instead of 1 per insert
```

**Code example**

```java
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emp_seq_gen")
    @SequenceGenerator(
        name = "emp_seq_gen",           // logical name (matches generator = )
        sequenceName = "employee_seq",   // actual DB sequence name
        initialValue = 1,
        allocationSize = 50
    )
    private Long id;

    private String name;
}
```

The database sequence must exist (or Hibernate creates it if `ddl-auto=update`):

```sql
CREATE SEQUENCE employee_seq START WITH 1 INCREMENT BY 50;
```

**Should you use @SequenceGenerator or create the sequence manually via SQL?**

| Approach | When to use |
|---|---|
| Let Hibernate create it (`ddl-auto=update`) | Local development only |
| Create via Flyway/Liquibase migration | Production — sequence is version-controlled, reviewed, and repeatable |
| Use a shared Sequence Service API | Enterprise — when multiple teams/microservices need unique IDs across tables |

**Shared Sequence Service pattern (industry practice)**

In large enterprise architectures, teams create a standalone microservice or shared library for generating unique IDs. This decouples ID generation from any specific table or database.

```text
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Service A    │    │ Service B    │    │ Service C    │
│ (Orders)     │    │ (Users)      │    │ (Payments)   │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────┴──────────┐
                    │ ID Generator    │
                    │ Service / API   │
                    │                 │
                    │ GET /next-id    │
                    │ ?entity=order   │
                    │                 │
                    │ Uses: DB seq,   │
                    │ Snowflake, UUID │
                    │ or custom algo  │
                    └─────────────────┘
```

```java
// Simplified sequence service
@RestController
@RequestMapping("/api/sequences")
public class SequenceController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/next")
    public Long getNextId(@RequestParam String sequenceName) {
        return jdbcTemplate.queryForObject(
            "SELECT nextval(?)", Long.class, sequenceName
        );
    }
}

// Client microservice uses the ID before persisting
Long orderId = restTemplate.getForObject(
    "http://id-service/api/sequences/next?sequenceName=order_seq", Long.class);
order.setId(orderId);
orderRepository.save(order);
```

---


### @Table Annotation

`@Table` maps an entity class to a specific database table. It is **optional** — if omitted, Hibernate uses the class name as the table name.

```java
@Entity
@Table(name = "employees")       // maps to "employees" table instead of default "Employee"
public class Employee { ... }
```

**Properties of @Table**

| Property | Purpose | Default |
|---|---|---|
| `name` | Table name in the database | Entity class name |
| `schema` | Database schema (e.g. `public`, `hr`) | Default schema |
| `catalog` | Database catalog | Default catalog |
| `uniqueConstraints` | Declare unique constraints (single or composite) | None |
| `indexes` | Declare indexes (single or composite) | None |

**name and schema**

```java
@Entity
@Table(name = "emp_records", schema = "hr")
public class Employee {
    // Maps to: hr.emp_records
    @Id
    private Long id;
    private String name;
    private String email;
}
```

**Unique Constraints — single column**

```java
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_users_email",        // constraint name in DB
        columnNames = {"email"}          // single column
    )
)
public class User {
    @Id
    private Long id;
    private String name;
    private String email;              // must be unique
}
```

Generated DDL:
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255),
    CONSTRAINT uk_users_email UNIQUE (email)
);
```

**Unique Constraints — composite (multiple columns)**

```java
@Entity
@Table(
    name = "enrollments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_enrollment_student_course",
        columnNames = {"student_id", "course_id"}    // composite: same student can't enroll twice
    )
)
public class Enrollment {
    @Id
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "course_id")
    private Long courseId;

    private LocalDate enrolledAt;
}
```

Generated DDL:
```sql
CREATE TABLE enrollments (
    id BIGINT PRIMARY KEY,
    student_id BIGINT,
    course_id BIGINT,
    enrolled_at DATE,
    CONSTRAINT uk_enrollment_student_course UNIQUE (student_id, course_id)
);
```

**Index — single column**

```java
@Entity
@Table(
    name = "orders",
    indexes = @Index(
        name = "idx_orders_customer_id",
        columnList = "customer_id"
    )
)
public class Order {
    @Id
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    private BigDecimal total;
}
```

Generated DDL:
```sql
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
```

**Index — composite (multiple columns)**

```java
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
        @Index(name = "idx_orders_status_date", columnList = "status, order_date DESC")  // composite
    }
)
public class Order {
    @Id
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    private String status;

    @Column(name = "order_date")
    private LocalDate orderDate;

    private BigDecimal total;
}
```

Generated DDL:
```sql
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status_date ON orders (status, order_date DESC);
```

---


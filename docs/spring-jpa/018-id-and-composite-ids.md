### @Id Annotation

`@Id` marks a field as the **primary key** of the entity. Every JPA entity **must have exactly one** `@Id` (or a composite key using `@EmbeddedId` or `@IdClass`).

```java
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;     // primary key

    private String name;
    private String email;
}
```

```text
┌──────────────────────┐
│  users               │
├──────────────────────┤
│  id (PK, BIGINT)     │  ← @Id
│  name (VARCHAR)       │
│  email (VARCHAR)      │
└──────────────────────┘
```

**Rule**: One entity can have only one `@Id` field. If you need a primary key consisting of multiple columns (composite key), you cannot simply put `@Id` on two fields without additional setup. You need either `@EmbeddedId` or `@IdClass`.

---

### Composite ID — Rules for the ID Class

JPA requires composite ID classes to follow strict rules. Here is why each rule exists:

| Rule | Why |
|---|---|
| Must be a **public** class | JPA/Hibernate needs to instantiate it via reflection |
| Must implement **`Serializable`** | The ID may be serialized for caching (L2 cache), detached state, or distributed environments |
| Must have a **no-arg constructor** | JPA creates the ID object via `Class.newInstance()` during entity load |
| Must override **`equals()`** | JPA uses `equals()` to compare entity identities (same PK = same entity) |
| Must override **`hashCode()`** | Required for correct behavior in `HashMap`, `HashSet`, and the Persistence Context map |

**Why `equals()` and `hashCode()` matter**

```text
Persistence Context internal map:
   Map<EntityKey, Object>

   EntityKey = (entity type + primary key)

   If two composite IDs are logically equal (same student_id + course_id)
   but equals()/hashCode() are not overridden:
      → JPA treats them as DIFFERENT entities
      → duplicate entries in persistence context
      → data corruption
```

**Example composite ID class**

```java
import java.io.Serializable;
import java.util.Objects;

public class EnrollmentId implements Serializable {    // public, Serializable

    private Long studentId;
    private Long courseId;

    public EnrollmentId() {}                            // no-arg constructor

    public EnrollmentId(Long studentId, Long courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
    }

    // getters, setters...

    @Override
    public boolean equals(Object o) {                   // equals override
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrollmentId that = (EnrollmentId) o;
        return Objects.equals(studentId, that.studentId)
            && Objects.equals(courseId, that.courseId);
    }

    @Override
    public int hashCode() {                             // hashCode override
        return Objects.hash(studentId, courseId);
    }
}
```

---

### @Embeddable and @EmbeddedId

This is the **first approach** to create composite primary keys. You create a separate embeddable class for the key, then embed it in the entity using `@EmbeddedId`.

```text
┌─────────────────────────────┐
│  @Embeddable                │
│  EnrollmentId               │
│  ┌─────────────────────┐    │
│  │ studentId (Long)     │    │
│  │ courseId (Long)       │    │
│  └─────────────────────┘    │
└─────────────────────────────┘
              │
              │  @EmbeddedId
              v
┌─────────────────────────────┐
│  @Entity                    │
│  Enrollment                 │
│  ┌─────────────────────┐    │
│  │ id: EnrollmentId (PK)│    │
│  │ enrolledAt: LocalDate │    │
│  │ grade: String         │    │
│  └─────────────────────┘    │
└─────────────────────────────┘
```

**Step 1: Create the embeddable ID class**

```java
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EnrollmentId implements Serializable {

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "course_id")
    private Long courseId;

    public EnrollmentId() {}

    public EnrollmentId(Long studentId, Long courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
    }

    // getters, setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrollmentId that = (EnrollmentId) o;
        return Objects.equals(studentId, that.studentId)
            && Objects.equals(courseId, that.courseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, courseId);
    }
}
```

**Step 2: Use @EmbeddedId in the entity**

```java
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @EmbeddedId
    private EnrollmentId id;       // composite PK

    @Column(name = "enrolled_at")
    private LocalDate enrolledAt;

    private String grade;

    // constructors, getters, setters
}
```

**Step 3: Repository**

```java
public interface EnrollmentRepository
        extends JpaRepository<Enrollment, EnrollmentId> {

    // Spring Data JPA uses EnrollmentId as the ID type
}
```

**Step 4: Usage**

```java
// Create
EnrollmentId id = new EnrollmentId(101L, 501L);
Enrollment enrollment = new Enrollment();
enrollment.setId(id);
enrollment.setEnrolledAt(LocalDate.now());
enrollmentRepository.save(enrollment);

// Find
Optional<Enrollment> found = enrollmentRepository.findById(new EnrollmentId(101L, 501L));
```

Generated DDL:
```sql
CREATE TABLE enrollments (
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrolled_at DATE,
    grade VARCHAR(255),
    PRIMARY KEY (student_id, course_id)
);
```

---

### @IdClass and @Id

This is the **second approach** to composite keys. Instead of embedding the key, you annotate the entity with `@IdClass` and mark each key field with `@Id`.

```text
┌─────────────────────────────┐
│  @IdClass = EnrollmentId    │
│                             │
│  @Entity                    │
│  Enrollment                 │
│  ┌─────────────────────┐    │
│  │ @Id studentId (Long)  │    │
│  │ @Id courseId (Long)    │    │
│  │ enrolledAt: LocalDate  │    │
│  │ grade: String          │    │
│  └─────────────────────┘    │
└─────────────────────────────┘
```

**Step 1: Create the ID class (same rules, but NOT @Embeddable)**

```java
import java.io.Serializable;
import java.util.Objects;

public class EnrollmentId implements Serializable {

    private Long studentId;
    private Long courseId;

    public EnrollmentId() {}

    public EnrollmentId(Long studentId, Long courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
    }

    // getters, setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrollmentId that = (EnrollmentId) o;
        return Objects.equals(studentId, that.studentId)
            && Objects.equals(courseId, that.courseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, courseId);
    }
}
```

**Step 2: Use @IdClass on the entity, @Id on each key field**

```java
@Entity
@Table(name = "enrollments")
@IdClass(EnrollmentId.class)
public class Enrollment {

    @Id
    @Column(name = "student_id")
    private Long studentId;          // part of composite PK

    @Id
    @Column(name = "course_id")
    private Long courseId;           // part of composite PK

    @Column(name = "enrolled_at")
    private LocalDate enrolledAt;

    private String grade;

    // constructors, getters, setters
}
```

**Step 3: Repository and usage**

```java
public interface EnrollmentRepository
        extends JpaRepository<Enrollment, EnrollmentId> {}

// Find
Optional<Enrollment> found = enrollmentRepository.findById(new EnrollmentId(101L, 501L));

// JPQL — you can reference fields directly (no id.studentId nesting)
@Query("SELECT e FROM Enrollment e WHERE e.studentId = :sid")
List<Enrollment> findByStudent(@Param("sid") Long studentId);
```

**@EmbeddedId vs @IdClass comparison**

| | @EmbeddedId | @IdClass |
|---|---|---|
| ID class annotation | `@Embeddable` | Plain class (no annotation) |
| Entity fields | Single `@EmbeddedId` field | Multiple `@Id` fields |
| JPQL access | `e.id.studentId` (nested) | `e.studentId` (flat, cleaner) |
| Field duplication | No duplication | Fields appear in both ID class and entity |
| JPA standard | Yes | Yes |
| Popular choice | When ID is reused across entities | When simpler JPQL queries are preferred |

---


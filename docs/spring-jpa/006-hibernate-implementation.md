### What is Hibernate and How it Implements EntityManager

Hibernate is an **ORM framework** and the most widely used **JPA implementation**.

```text
     JPA Specification (interfaces)         Hibernate Implementation (classes)
     ──────────────────────────────         ────────────────────────────────────
     EntityManagerFactory           ──>     SessionFactory
     EntityManager                  ──>     Session (wraps EntityManager contract)
     EntityTransaction              ──>     Transaction
     Query                          ──>     org.hibernate.query.Query
     PersistenceContext              ──>     Session's internal state (ActionQueue + PersistenceContext)
```

- When you call `entityManager.persist(entity)`, Hibernate's `Session` receives the call.
- `Session` is the Hibernate-specific implementation of `EntityManager`.
- Spring Boot uses `HibernateJpaVendorAdapter` to tell Spring: "use Hibernate as the JPA provider."

```java
// You can unwrap the Hibernate Session from EntityManager if needed:
Session session = entityManager.unwrap(Session.class);
```

---

### How Hibernate Internally Creates a Session

```text
Application startup
       │
       v
+---------------------+
| SessionFactory      |   (created once, heavyweight)
| = EntityManager     |
|   Factory           |
+---------------------+
       │
       │  openSession() / getCurrentSession()
       v
+---------------------+
|   Session           |   (created per transaction, lightweight)
|   = EntityManager   |
+---------------------+
       │
       │  internally holds:
       v
+---------------------+
| PersistenceContext   |   - first-level cache (Map<EntityKey, Object>)
| ActionQueue         |   - pending INSERT, UPDATE, DELETE actions
| JDBC Connection     |   - borrowed from connection pool (HikariCP)
+---------------------+
```

1. At startup, `SessionFactory` is built from configuration (properties + entity metadata).
2. When a `@Transactional` method runs, Spring asks `SessionFactory` for a new `Session`.
3. The `Session` borrows a JDBC connection from the pool (lazily, on first SQL execution).
4. The `Session` holds:
   - **PersistenceContext**: an internal `Map<EntityKey, Object>` that tracks all managed entities.
   - **ActionQueue**: a queue of pending INSERT, UPDATE, and DELETE operations, flushed on commit.
5. When the transaction commits, the `Session` flushes the `ActionQueue` (executes SQL), commits via JDBC, and returns the connection to the pool.
6. The `Session` is closed and becomes unusable.

---

### GOLD: Imports fix + unwrap + SQL + interview Q&A

The `unwrap` snippet above needs this import (common compile error):

```java
import org.hibernate.Session; // NOT jakarta.persistence.* — Session is Hibernate-specific
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.EntityManager;

Session session = entityManager.unwrap(Session.class); // access Hibernate-native API
```

SQL generated on flush (example):

```sql
INSERT INTO employees (name, email) VALUES ('Alice', 'alice@x.com');
UPDATE employees SET name = 'Alicia' WHERE id = 1;
DELETE FROM employees WHERE id = 2;
```

**Interview Q&A**

1. **Q: EntityManager vs Session?** A: `EntityManager` is the JPA interface; `Session` is Hibernate's implementation. `unwrap(Session.class)` exposes native features (filters, custom cascades).
2. **Q: Why is SessionFactory heavyweight but Session lightweight?** A: Factory parses metadata + builds connection pool once; Session is one persistence context + borrowed connection per transaction.
3. **Q: When does Hibernate hit the DB?** A: Only on `flush()` (commit/query-flush) — `persist()` just enqueues an action in the `ActionQueue`.


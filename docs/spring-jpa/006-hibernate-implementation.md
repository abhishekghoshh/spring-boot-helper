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


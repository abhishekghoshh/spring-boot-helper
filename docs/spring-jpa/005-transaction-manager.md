### Transaction Manager and its Connection to EntityManagerFactory

```text
+--------------------+       creates        +------------------+
| EntityManager      | <────────────────── | EntityManager    |
|                    |                      | Factory          |
+--------------------+                      +------------------+
         │                                          ^
         │ uses                                     │ wraps
         v                                          │
+--------------------+                      +------------------+
| Transaction        | ────────────────────>| DataSource       |
| Manager            |    gets connections  |                  |
| (JpaTransaction    |    from              | (HikariCP pool)  |
|  Manager)          |                      |                  |
+--------------------+                      +------------------+
```

- `JpaTransactionManager` is the default transaction manager in Spring Boot for JPA.
- It holds a reference to the `EntityManagerFactory`.
- When a `@Transactional` method begins:
  1. The transaction manager asks `EntityManagerFactory` for an `EntityManager`.
  2. It gets a JDBC `Connection` from the `DataSource`.
  3. It binds both the `EntityManager` and `Connection` to the current thread.
  4. The method runs.
  5. On success: flush the persistence context, commit the JDBC transaction.
  6. On failure: rollback.

```java
// Spring Boot auto-configures this, but you can define it explicitly:
@Bean
public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
}
```

---

### Resource Local Transaction vs JTA

| | Resource Local | JTA (Java Transaction API) |
|---|---|---|
| **Scope** | Single database | Multiple databases / resources |
| **Who manages** | Application code or Spring | Application server / container |
| **Transaction type in persistence.xml** | `RESOURCE_LOCAL` | `JTA` |
| **EntityManager creation** | `emf.createEntityManager()` | Container injects it |
| **Commit** | `em.getTransaction().commit()` | `UserTransaction.commit()` |
| **Use case** | Most Spring Boot apps (single DB) | Distributed systems, multiple DBs, JMS + DB |

**Resource Local example (manual)**

```java
EntityManager em = emf.createEntityManager();
em.getTransaction().begin();

em.persist(new Employee("Alice"));

em.getTransaction().commit();  // commits to single DB
em.close();
```

**JTA example (container-managed)**

```java
@Stateless  // EJB or Jakarta EE
public class EmployeeService {

    @PersistenceContext
    private EntityManager em;  // container provides this

    public void transfer() {
        // operations on Database A
        em.persist(new Employee("Alice"));

        // operations on Database B (different persistence unit)
        // ...

        // container commits BOTH databases atomically using JTA
    }
}
```

---

### 2-Phase Commit and JTA

2-Phase Commit (2PC) is the protocol JTA uses to ensure atomicity across multiple resources.

```text
                          Transaction Manager
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
                    v            v            v
              +---------+  +---------+  +---------+
              |  DB A   |  |  DB B   |  |  JMS    |
              |         |  |         |  | Queue   |
              +---------+  +---------+  +---------+

Phase 1: PREPARE
─────────────────
   TM ──> DB A:  "Can you commit?"   ──> DB A: "YES (prepared)"
   TM ──> DB B:  "Can you commit?"   ──> DB B: "YES (prepared)"
   TM ──> JMS:   "Can you commit?"   ──> JMS:  "YES (prepared)"

   All said YES?  ──> proceed to Phase 2

Phase 2: COMMIT
─────────────────
   TM ──> DB A:  "COMMIT"   ──> DB A: committed
   TM ──> DB B:  "COMMIT"   ──> DB B: committed
   TM ──> JMS:   "COMMIT"   ──> JMS:  committed

If ANY resource says NO in Phase 1:
─────────────────────────────────────
   TM ──> ALL:   "ROLLBACK"
```

- **Phase 1 (Prepare)**: Transaction manager asks each resource "can you commit?". Each resource locks its data and responds YES or NO.
- **Phase 2 (Commit/Rollback)**: If all said YES, the transaction manager tells all to commit. If any said NO, all rollback.
- **Guarantees atomicity**: either all resources commit or none do.
- JTA implementations: **Atomikos**, **Narayana**, **Bitronix**.

---


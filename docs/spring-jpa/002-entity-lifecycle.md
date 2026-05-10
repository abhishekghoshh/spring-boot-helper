### Entity Lifecycle

Every JPA entity exists in one of four states. The `EntityManager` moves it between them.

```text
                                      em.persist(entity)
    +-----------------+  ──────────────────────────────────────>  +--------------------+
    |    NEW          |                                          |     MANAGED        |
    |  (Transient)    |                                          |  (Persistence      |
    |                 |                                          |   Context)         |
    |  new Employee() |                                     ┌──> |                    | <──┐
    +-----------------+                                     │    +--------------------+    │
           │                                                │     │    │    ^    ^         │
           │                                                │     │    │    │    │         │
           │ (garbage                              merge()  │     │    │    │    │         │ em.find() /
           │  collected,                        (re-attach) │     │    │    │    │         │ JPQL query /
           │  no ref)                                       │     │    │    │    │         │ Native query
           v                                                │     │    │    │    │         │
    +-----------------+                                     │     │    │    │    │    ┌────┴─────────────┐
    |  (out of scope) |                                     │     │    │    │    │    │    DATABASE      │
    |   no longer     |                                     │     │    │    │    │    │                  │
    |   reachable     |                                     │     │    │    │    │    │  SELECT * FROM   │
    +-----------------+                                     │     │    │    │    │    │  employees       │
                                                            │     │    │    │    │    │  WHERE ...       │
                                              em.close() /  │     │    │    │    │    │                  │
                                              em.clear() /  │     │    │    │    │    │  returns entity  │
                                              em.detach()   │     │    │    │    │    │  as Managed      │
                                                            │     │    │    │    │    └────┬─────────────┘
                                                            │     │    │    │    │         ^
                                                            │     │    │    │    │         │
    +-----------------+                                     │     │    │    │    │         │ flush() /
    |   DETACHED      | <───────────────────────────────────┘     │    │    │    │         │ commit()
    |                 |                                           │    │    │    │         │
    |  not tracked,   |                em.remove()                │    │    │    │         │
    |  changes lost   |                (schedule delete)          │    │    │    │         │
    |  unless merge() |                                           v    │    │    │         │
    +-----------------+                                    +──────────┴────┘    │         │
                                                           │    REMOVED         │         │
                                                           │                    │         │
                                                           │  DELETE FROM ...   ├─────────┘
                                                           │                    │ flush() /
                                                           │                    │ commit()
                                                           +────────────────────+

    Summary:
    NEW ──── persist() ──────────────> MANAGED
    DB  ──── find()/query() ─────────> MANAGED  (entity loaded into Persistence Context)
    MANAGED ─ flush()/commit() ──────> DB       (INSERT / UPDATE synced to database)
    MANAGED ─ remove() ─────────────-> REMOVED
    REMOVED ─ flush()/commit() ──────> DB       (DELETE executed in database)
    MANAGED ─ close()/clear()/detach()> DETACHED
    DETACHED ─ merge() ─────────────-> MANAGED
```

**The Four States**

1. **New (Transient)**
   - The entity is created with `new` but not yet known to the persistence context.
   - It has no representation in the database.
   ```java
   Employee emp = new Employee();   // New / Transient
   emp.setName("Alice");
   ```

2. **Managed (Persistent)**
   - The entity is tracked by the persistence context.
   - Any changes to it are automatically detected (dirty checking) and synced to the database on flush/commit.
   ```java
   entityManager.persist(emp);      // New -> Managed
   // or
   Employee emp = entityManager.find(Employee.class, 1L);  // Loaded as Managed
   emp.setEmail("new@mail.com");    // change is auto-detected, no explicit save needed
   ```

3. **Detached**
   - The entity was previously managed but the persistence context was closed, cleared, or the entity was explicitly detached.
   - Changes are **not** tracked. To re-attach, use `merge()`.
   ```java
   entityManager.close();           // all entities become Detached
   // or
   entityManager.detach(emp);       // single entity becomes Detached

   // later, to re-attach:
   Employee managedEmp = entityManager.merge(emp);  // Detached -> Managed (returns managed copy)
   ```

4. **Removed**
   - The entity is scheduled for deletion. On the next flush/commit, the corresponding row is deleted from the database.
   ```java
   entityManager.remove(emp);       // Managed -> Removed
   // after flush: DELETE FROM employees WHERE id = ?
   ```

**State Transitions Summary**

| From       | To       | Trigger                                     |
|------------|----------|---------------------------------------------|
| New        | Managed  | `persist(entity)`                           |
| Managed    | Detached | `detach(entity)`, `clear()`, `close()`      |
| Managed    | Removed  | `remove(entity)`                            |
| Detached   | Managed  | `merge(entity)`                             |
| Removed    | Managed  | `persist(entity)` (before flush)            |

**Spring Data JPA Mapping**

| Repository method   | EntityManager call         | State transition      |
|---------------------|----------------------------|-----------------------|
| `save(newEntity)`   | `persist(entity)`          | New -> Managed        |
| `save(existing)`    | `merge(entity)`            | Detached -> Managed   |
| `findById(id)`      | `find(Entity.class, id)`   | -> Managed            |
| `delete(entity)`    | `remove(entity)`           | Managed -> Removed    |
| `deleteById(id)`    | `find()` then `remove()`   | -> Managed -> Removed |


### Why @Transactional is Used for EntityManager Methods

The `EntityManager` requires an active transaction for any **write** operation (`persist`, `merge`, `remove`). Without `@Transactional`, there is no transaction context.

**What happens without @Transactional?**

```java
@Repository
public class EmployeeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // NO @Transactional
    public void saveEmployee(Employee emp) {
        entityManager.persist(emp);
        // throws javax.persistence.TransactionRequiredException:
        // "No EntityManager with actual transaction available for current thread"
    }
}
```

- `persist()`, `merge()`, `remove()` → **throw `TransactionRequiredException`** if no transaction is active.
- `find()`, `createQuery().getResultList()` → **work without a transaction** (read-only operations), but the returned entities are detached immediately since there is no persistence context bound to a transaction.

**What @Transactional does internally**

```text
@Transactional method call
         │
         v
Spring AOP proxy intercepts
         │
         ├─ TransactionManager.begin()
         │     ├─ gets EntityManager from EntityManagerFactory
         │     ├─ gets Connection from DataSource (HikariCP)
         │     ├─ sets Connection.setAutoCommit(false)
         │     └─ binds EM + Connection to current thread (ThreadLocal)
         │
         ├─ actual method executes
         │     ├─ entityManager.persist(...)   ← works, transaction is active
         │     ├─ entityManager.find(...)
         │     └─ entity.setName("updated")    ← dirty checking at flush
         │
         ├─ method returns successfully
         │     ├─ EntityManager.flush()        ← generates SQL
         │     ├─ Connection.commit()
         │     └─ EntityManager.close()
         │
         └─ if exception:
               ├─ Connection.rollback()
               └─ EntityManager.close()
```

---


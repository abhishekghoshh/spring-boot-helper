### Transaction-Bounded Operations

A transaction-bounded operation is any persistence operation that **requires an active database transaction** to succeed. The persistence context is tied to the transaction — it is created when the transaction begins and closed when it ends.

```text
@Transactional begins
     │
     ├─ Persistence Context created
     ├─ JDBC Connection acquired
     │
     ├─ persist(), merge(), remove()  ← transaction-bounded (WRITE)
     ├─ find(), query()               ← can read without txn, but entities become detached
     │
     ├─ method returns
     ├─ flush() ── SQL executed
     ├─ commit()
     └─ Persistence Context closed, all entities become DETACHED
```

**Examples of transaction-bounded operations**

```java
@Service
public class EmployeeService {

    @PersistenceContext
    private EntityManager em;

    // Transaction-bounded: persist requires active transaction
    @Transactional
    public void createEmployee(String name) {
        Employee emp = new Employee(name);
        em.persist(emp);  // fails without @Transactional
    }

    // Transaction-bounded: remove requires active transaction
    @Transactional
    public void deleteEmployee(Long id) {
        Employee emp = em.find(Employee.class, id);
        em.remove(emp);  // fails without @Transactional
    }

    // Transaction-bounded: dirty checking only works inside a transaction
    @Transactional
    public void updateEmployee(Long id, String newName) {
        Employee emp = em.find(Employee.class, id);  // MANAGED
        emp.setName(newName);
        // no explicit save — dirty checking generates UPDATE on commit
    }

    // NOT transaction-bounded: read-only, works without @Transactional
    // but entity is immediately detached (no dirty checking)
    public Employee getEmployee(Long id) {
        return em.find(Employee.class, id);  // works, but entity is detached
    }
}
```

**Key rule**: If the operation changes database state (`INSERT`, `UPDATE`, `DELETE`), it must run inside a transaction. Read operations can work without one, but you lose the benefits of the persistence context (caching, dirty checking, managed state).

---

### GOLD: readOnly alternative + OSIV nuance + Q&A

Prefer `@Transactional(readOnly = true)` over no-transaction reads — entities stay managed (lazy loading works), but Hibernate skips dirty checking:

```java
@Transactional(readOnly = true)
public Employee getEmployee(Long id) {
    return em.find(Employee.class, id); // MANAGED, no UPDATE generated
}
```

OSIV nuance: with `spring.jpa.open-in-view=true` (Boot default), the persistence context stays open during view rendering so lazy loading works — but it holds the JDBC connection longer. Disable (`false`) + use fetch joins/DTOs in APIs.

**Interview Q&A**

1. **Q: Read without transaction vs readOnly transaction?** A: No-txn → entity immediately detached (lazy load fails); readOnly txn → managed with lazy loading but no flush overhead.
2. **Q: Why disable OSIV?** A: OSIV masks N+1/lazy issues and holds connections through rendering — explicit fetch plans + `open-in-view=false` surface problems early.


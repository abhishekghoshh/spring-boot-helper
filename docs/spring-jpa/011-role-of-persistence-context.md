### Role of Persistence Context

**1. First-Level Cache**

```java
@Transactional
public void example() {
    // FIRST call: SQL SELECT executed, entity cached in persistence context
    Employee emp1 = entityManager.find(Employee.class, 1L);

    // SECOND call: NO SQL — returns the same object from persistence context
    Employee emp2 = entityManager.find(Employee.class, 1L);

    System.out.println(emp1 == emp2);  // true (same object reference)
}
```

```text
find(Employee, 1L)   ──>  Persistence Context cache check
                               │
                      ┌────────┴────────┐
                      │                 │
                   CACHE HIT         CACHE MISS
                      │                 │
                return cached       execute SELECT
                  entity            add to cache
                      │             return entity
                      v                 │
                  Employee(1)           v
                                    Employee(1)
```

**2. Entity Lifecycle Management**

The persistence context tracks the state of every entity (New, Managed, Detached, Removed) and determines what SQL to generate at flush time.

```java
@Transactional
public void lifecycleExample() {
    Employee emp = new Employee("Alice");   // NEW — not in persistence context

    entityManager.persist(emp);              // MANAGED — now tracked
    emp.setEmail("alice@example.com");       // dirty checking will pick this up

    entityManager.flush();                   // INSERT + UPDATE (or just INSERT with latest values)

    entityManager.remove(emp);               // REMOVED — scheduled for deletion
    entityManager.flush();                   // DELETE FROM employees WHERE id = ?
}
```

**3. Dirty Checking**

```text
 persist(emp)           emp.setEmail(...)          flush()
      │                       │                       │
      v                       v                       v
 snapshot taken:        entity mutated:           compare snapshot vs current:
 {name="Alice",        {name="Alice",            email changed!
  email=null}           email="alice@.."}         ──> generate UPDATE SQL
```

**4. Write-Behind (ActionQueue)**

The persistence context does not execute SQL immediately. It queues operations and executes them at flush time (usually right before commit). This allows Hibernate to batch and reorder operations for efficiency.

---


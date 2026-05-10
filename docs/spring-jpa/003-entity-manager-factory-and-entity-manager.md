### EntityManagerFactory and EntityManager

```text
+--------------------+         creates          +------------------+          owns           +---------------------+
| Persistence Unit   | ──────────────────────>  | EntityManager    | ──────────────────────> | Persistence Context |
| (configuration)    |                          | Factory          |                         | (first-level cache) |
|                    |                          | (one per app)    |                         |                     |
| - datasource URL   |                          |                  |                         | tracks managed      |
| - dialect          |                          | creates many     |                         | entities            |
| - entity classes   |                          | EntityManagers   |                         |                     |
+--------------------+                          +------------------+                         +---------------------+
                                                    │          │
                                                    │          │
                                                    v          v
                                            +----------+  +----------+
                                            |   EM 1   |  |   EM 2   |
                                            | (txn A)  |  | (txn B)  |
                                            +----------+  +----------+
```

- There is **one `EntityManagerFactory` per Persistence Unit** (per database).
- The factory is heavyweight and created once at application startup.
- Each `EntityManager` is lightweight and typically scoped to a single transaction.
- The `EntityManager` is the interface you use for all persistence operations: `persist()`, `find()`, `merge()`, `remove()`.

**How Spring Boot auto-configures it**

Spring Boot reads `spring.datasource.*` and `spring.jpa.*` properties, creates a `DataSource`, wraps it into a `LocalContainerEntityManagerFactoryBean`, and registers the resulting `EntityManagerFactory` as a bean.

```java
// Spring Boot auto-configuration (simplified, happens behind the scenes)
@Bean
public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPackagesToScan("com.example.entities");
    em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    return em;
}
```

**Creating EntityManager manually (plain JPA, no Spring)**

```java
// Using persistence.xml
EntityManagerFactory emf = Persistence.createEntityManagerFactory("myPersistenceUnit");
EntityManager em = emf.createEntityManager();

em.getTransaction().begin();

Employee emp = new Employee();
emp.setName("Alice");
em.persist(emp);

em.getTransaction().commit();
em.close();
emf.close();
```

**Using EntityManager in Spring (injected)**

```java
@Repository
public class EmployeeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveEmployee(Employee emp) {
        entityManager.persist(emp);
    }

    public Employee findEmployee(Long id) {
        return entityManager.find(Employee.class, id);
    }
}
```

---


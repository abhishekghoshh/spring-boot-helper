# Spring JDBC JPA



## Documentation

- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/)
- [An Introduction to Hibernate 6](https://docs.hibernate.org/orm/current/introduction/html_single/#preface)
- [An Introduction to Hibernate 6](https://docs.hibernate.org/orm/current/introduction/pdf/Hibernate_Introduction.pdf)
- [Hibernate ORM User Guide](https://docs.hibernate.org/orm/current/userguide/html_single/)


## books

- [Java Persistence with Spring Data and Hibernate](https://www.manning.com/books/java-persistence-with-spring-data-and-hibernate)


## Blogs


- [Spring Persistence Series](https://www.baeldung.com/persistence-with-spring-series)
- [Learn JPA & Hibernate Series](https://www.baeldung.com/learn-jpa-hibernate)


## Medium

- [JPA, Hibernate And Spring Data JPA](https://medium.com/@burakkocakeu/jpa-hibernate-and-spring-data-jpa-efa71feb82ac)
- [In Spring Data JPA OneToMany What are These Fields (mappedBy, fetch, cascade and orphanRemoval)](https://medium.com/@burakkocakeu/in-spring-data-jpa-onetomany-what-are-these-fields-mappedby-fetch-cascade-and-orphanremoval-2655f4027c4f)
- [Kotlin and JPA — a good match or a recipe for failure?](https://blog.kotlin-academy.com/kotlin-and-jpa-a-good-match-or-a-recipe-for-failure-e52718d93b4f)
- [Hibernate is not so evil](https://medium.com/better-programming/hibernate-is-not-so-evil-84ca72b959c3)
- [Stop Guessing: A Deep Dive into Tuning HikariCP in Spring Boot for Maximum Performance](https://medium.com/@mesfandiari77/stop-guessing-a-deep-dive-into-tuning-hikaricp-in-spring-boot-for-maximum-performance-829edb7195ee)



## Youtube

- [Hibernate & JPA Tutorial - Crash Course](https://www.youtube.com/watch?v=xHminZ9Dxm4)
    - [marcobehlerjetbrains/hibernate-tutorial](https://github.com/marcobehlerjetbrains/hibernate-tutorial)

- [Spring Data JPA Tutorial | Full In-depth Course](https://www.youtube.com/watch?v=XszpXoII9Sg)
- [Master Spring Data JPA In One Video | Hindi](https://www.youtube.com/watch?v=8SxJNqeq_zc)

- [Spring Transactions](https://www.youtube.com/playlist?list=PL-bgVzzRdaPimI4ERQ9gOtUKLEIALmoFL)



## Udemy

- [Master Hibernate and JPA with Spring Boot in 100 Steps](https://udemy.com/course/hibernate-jpa-tutorial-for-beginners-in-100-steps/)
- [Spring Data JPA Using Hibernate](https://udemy.com/course/spring-data-jpa-using-hibernate/)
- [Hibernate and Spring Data JPA: Beginner to Guru](https://udemy.com/course/hibernate-and-spring-data-jpa-beginner-to-guru/)


## Theory


### Architecture

At a high level, the request flow in a Spring Data JPA application looks like this:

`JPA -> Hibernate -> JDBC -> Specific DB Driver -> Relational DB`

Spring Data JPA repositories work on top of JPA, and internally they use the `EntityManager` to perform persistence operations.

### Diagram

**Request Flow (left to right)**

```text
+----------------------------+      +----------+      +-----------+      +------+      +----------------+      +---------------+
| Spring Data JPA Repository | ---> | JPA API  | ---> | Hibernate | ---> | JDBC | ---> | DB Driver      | ---> | Relational DB |
| e.g. employeeRepo.save()  |      |          |      |           |      |      |      | e.g. mysql-    |      | e.g. MySQL,   |
|                            |      |          |      |           |      |      |      | connector-java |      | PostgreSQL    |
+----------------------------+      +----------+      +-----------+      +------+      +----------------+      +---------------+
```

**Internal Object Lifecycle (left to right)**

```text
+------------------+      +----------------------+      +----------------+      +---------------------+      +--------+
| Persistence Unit | ---> | EntityManagerFactory  | ---> | EntityManager  | ---> | Persistence Context | ---> | Entity |
|                  |      | (one per app)         |      | (one per       |      | (first-level cache) |      |        |
| defined in       |      |                      |      |  transaction)  |      |                     |      |        |
| persistence.xml  |      | creates EntityManager |      | persist, find, |      | tracks managed      |      | @Entity|
| or auto-config   |      | instances             |      | merge, remove  |      | entities in memory  |      | @Table |
+------------------+      +----------------------+      +----------------+      +---------------------+      +--------+
```

**Hibernate to Database (left to right)**

```text
+-----------+      +-----------------+      +----------------+      +---------------+
| Hibernate | ---> | Dialect         | ---> | DB Driver      | ---> | Relational DB |
|           |      |                 |      |                |      |               |
| converts  |      | generates SQL   |      | sends SQL over |      | executes SQL  |
| entity    |      | specific to the |      | the network    |      | and returns   |
| operations|      | target database |      | using JDBC     |      | result sets   |
| into SQL  |      |                 |      |                |      |               |
|           |      | MySQL8Dialect,  |      | mysql-connector|      | tables, rows, |
|           |      | PostgreSQLDialect|     | postgresql     |      | indexes       |
+-----------+      +-----------------+      +----------------+      +---------------+
```

**Example: What happens when you call `employeeRepo.save(employee)`**

```text
employeeRepo.save(employee)
       |
       v
[Spring Data JPA] ---> calls entityManager.persist(employee)
       |
       v
[EntityManager] ---> adds employee to Persistence Context (first-level cache)
       |
       v
[Hibernate] ---> reads @Entity, @Table, @Column annotations
       |            generates: INSERT INTO employees(name, email, age) VALUES (?, ?, ?)
       v
[Dialect] ---> adjusts SQL syntax for target DB (e.g. AUTO_INCREMENT for MySQL, SERIAL for PostgreSQL)
       |
       v
[JDBC + Driver] ---> sends the final SQL statement to the database
       |
       v
[Database] ---> inserts the row, returns generated ID
```

**Example: What happens when you call `employeeRepo.findById(1L)`**

```text
employeeRepo.findById(1L)
       |
       v
[Spring Data JPA] ---> calls entityManager.find(Employee.class, 1L)
       |
       v
[EntityManager] ---> checks Persistence Context first (cache hit = no DB call)
       |                if not found in cache:
       v
[Hibernate] ---> generates: SELECT id, name, email, age FROM employees WHERE id = ?
       |
       v
[Dialect + JDBC + Driver] ---> sends SQL to database
       |
       v
[Database] ---> returns the row
       |
       v
[Hibernate] ---> maps ResultSet columns to Employee fields, stores in Persistence Context
       |
       v
returns Employee object
```

### How the Architecture Works

1. **JPA**  
     JPA is a specification. It defines the standard contracts for ORM in Java, such as `EntityManager`, entity mappings, and persistence operations.
2. **Hibernate**  
     Hibernate is the most common JPA implementation. It takes JPA operations and translates them into actual SQL behavior.
3. **JDBC**  
     JDBC is the low-level database communication API. Hibernate eventually uses JDBC to send SQL to the database.
4. **Specific DB Driver**  
     This is the vendor-specific JDBC driver, such as MySQL or PostgreSQL driver. It knows how to talk to that database.
5. **Relational DB**  
     The final destination where tables, rows, indexes, and constraints actually live.

### Core Terms

- **Persistence Unit**: The logical configuration boundary for JPA. It defines which entities are managed and how persistence is configured.
- **EntityManagerFactory**: A heavyweight factory object created once per persistence unit. It is responsible for creating `EntityManager` instances.
- **EntityManager**: The main JPA interface used to persist, update, remove, and fetch entities.
- **Persistence Context**: The first-level cache managed by an `EntityManager`. It keeps track of entities currently being managed.
- **Entity**: A Java object mapped to a database table using annotations like `@Entity`, `@Table`, and `@Id`.
- **Dialect**: Hibernate's database-specific strategy layer. It adapts SQL generation to the target database, because SQL syntax differs across vendors.
- **JDBC Driver**: The actual library that lets JDBC connect to a concrete database product.

### Mapping the Existing Points

- **Persistence Unit -> EntityManagerFactory**  
    One persistence unit creates or configures one `EntityManagerFactory`.
- **EntityManagerFactory -> EntityManager1, EntityManager2**  
    The factory can create many `EntityManager` instances.
- **EntityManager -> Persistence Context**  
    Each `EntityManager` owns its own persistence context.
- **Persistence Context -> Entity**  
    Managed entities live inside the persistence context.
- **Entity -> Dialect**  
    Hibernate reads entity mappings and uses the dialect to decide how SQL should be generated.
- **Dialect -> JDBC Driver**  
    Hibernate generates SQL suitable for the target DB, then JDBC and the driver carry it to the database.
- **JDBC Driver -> DB**  
    The driver sends SQL to the relational database and returns results.

### Practical Interpretation

When you call a repository method like `save()` or `findById()`:

1. Spring Data JPA delegates internally to the `EntityManager`.
2. The `EntityManager` works within its persistence context.
3. Hibernate converts entity operations into SQL.
4. Hibernate uses JDBC plus the configured dialect and driver.
5. The database executes the SQL and returns the result.

This is why JPA feels high-level in application code, while JDBC still exists underneath the abstraction.

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

### What is Persistence Context

The Persistence Context is a **set of managed entity instances** that the `EntityManager` keeps track of during a unit of work.

```text
+-----------------------------------------------------------+
|  EntityManager                                            |
|                                                           |
|  +-----------------------------------------------------+ |
|  |  Persistence Context (first-level cache)             | |
|  |                                                      | |
|  |   Employee(id=1, name="Alice")  ── MANAGED           | |
|  |   Employee(id=2, name="Bob")    ── MANAGED           | |
|  |   Employee(id=3, name="Eve")    ── REMOVED           | |
|  |                                                      | |
|  +-----------------------------------------------------+ |
|                                                           |
|  persist()  find()  merge()  remove()  flush()            |
+-----------------------------------------------------------+
```

- Acts as a **first-level cache**: if you call `find(Employee.class, 1L)` twice in the same transaction, only one SQL SELECT runs.
- Performs **dirty checking**: at flush time, it compares the current state of each managed entity with a snapshot taken when it was first loaded. Changed fields generate UPDATE statements automatically.
- Guarantees **identity**: for a given entity type and primary key, only one Java object instance exists in the persistence context (same object reference).
- Scoped to the **transaction** by default in Spring (transaction-scoped persistence context).

---

### persistence.xml

In plain JPA (without Spring Boot auto-config), `persistence.xml` is the configuration file. It lives at `META-INF/persistence.xml`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                                 https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
             version="3.0">

    <persistence-unit name="employeePU" transaction-type="RESOURCE_LOCAL">

        <!-- JPA provider (Hibernate) -->
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>

        <!-- Entity classes to manage -->
        <class>com.example.entity.Employee</class>
        <class>com.example.entity.Department</class>

        <!-- Exclude unlisted classes -->
        <exclude-unlisted-classes>true</exclude-unlisted-classes>

        <properties>
            <!-- JDBC connection settings -->
            <property name="jakarta.persistence.jdbc.url"      value="jdbc:mysql://localhost:3306/company"/>
            <property name="jakarta.persistence.jdbc.user"     value="root"/>
            <property name="jakarta.persistence.jdbc.password" value="root"/>
            <property name="jakarta.persistence.jdbc.driver"   value="com.mysql.cj.jdbc.Driver"/>

            <!-- Hibernate dialect: tells Hibernate how to generate SQL for MySQL -->
            <property name="hibernate.dialect" value="org.hibernate.dialect.MySQL8Dialect"/>

            <!-- Schema generation: create / create-drop / update / validate / none -->
            <property name="hibernate.hbm2ddl.auto" value="update"/>

            <!-- Show generated SQL in logs -->
            <property name="hibernate.show_sql"   value="true"/>
            <property name="hibernate.format_sql" value="true"/>

            <!-- Second-level cache (optional) -->
            <property name="hibernate.cache.use_second_level_cache" value="false"/>
        </properties>
    </persistence-unit>
</persistence>
```

**Key properties explained**

| Property | Purpose |
|---|---|
| `persistence-unit name` | Logical name of this unit. Used in `Persistence.createEntityManagerFactory("employeePU")`. |
| `transaction-type` | `RESOURCE_LOCAL` (app manages transactions) or `JTA` (container manages transactions). |
| `provider` | The JPA implementation class. Usually Hibernate. |
| `class` | Fully qualified entity classes to include. |
| `jakarta.persistence.jdbc.*` | JDBC connection details. |
| `hibernate.dialect` | Tells Hibernate which SQL dialect to use (MySQL, PostgreSQL, Oracle, etc.). |
| `hibernate.hbm2ddl.auto` | Schema auto-generation strategy. `update` adds missing columns/tables without dropping. |
| `hibernate.show_sql` | Prints generated SQL to console. |

> In Spring Boot, you typically **do not need** `persistence.xml`. The same configuration is done via `application.properties` / `application.yml` and Spring Boot auto-configuration.

**Spring Boot equivalent**

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/company
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

### How Persistence Context is Used in Spring Boot

In Spring Boot, the persistence context is **transaction-scoped** by default.

```text
HTTP Request
     │
     v
@Service ── @Transactional method begins
     │
     ├─ Spring opens a transaction
     ├─ Spring creates (or reuses) an EntityManager
     ├─ EntityManager creates a Persistence Context
     │
     ├─ repository.save(entity)    ── entity enters Persistence Context (MANAGED)
     ├─ repository.findById(1L)    ── entity loaded into Persistence Context
     ├─ entity.setName("updated")  ── dirty checking picks this up automatically
     │
     ├─ method returns
     ├─ Spring flushes Persistence Context (generates SQL)
     ├─ Spring commits transaction
     └─ Persistence Context is closed, entities become DETACHED
```

```java
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;  // Spring Data JPA

    @Transactional
    public void updateEmployeeName(Long id, String newName) {
        // find() loads entity as MANAGED inside the persistence context
        Employee emp = employeeRepository.findById(id).orElseThrow();

        // just set the field — no save() call needed
        // dirty checking detects the change at flush time
        emp.setName(newName);

        // when @Transactional method ends:
        //   -> persistence context is flushed (UPDATE SQL generated)
        //   -> transaction commits
        //   -> persistence context closes
    }
}
```

---

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

### How JPA Repository Internally Uses EntityManager

```text
EmployeeRepository.save(entity)
         │
         v
SimpleJpaRepository.save(entity)        ← Spring Data JPA's default implementation
         │
         ├─ if entity.isNew():
         │      entityManager.persist(entity)     ← INSERT
         │
         └─ else:
                entityManager.merge(entity)       ← UPDATE (returns managed copy)
```

Spring Data JPA generates a proxy class for each repository interface. The actual implementation is `SimpleJpaRepository`:

```java
// Simplified version of Spring Data JPA's SimpleJpaRepository
@Repository
@Transactional(readOnly = true)
public class SimpleJpaRepository<T, ID> implements JpaRepository<T, ID> {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public <S extends T> S save(S entity) {
        if (entityInformation.isNew(entity)) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(entityManager.find(domainClass, id));
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        T entity = findById(id).orElseThrow();
        entityManager.remove(entity);
    }

    @Override
    public List<T> findAll() {
        return entityManager.createQuery("SELECT e FROM " + entityName + " e", domainClass)
                            .getResultList();
    }
}
```

Every Spring Data JPA method — `save()`, `findById()`, `delete()`, `findAll()` — calls `EntityManager` underneath.

---

### Why @PersistenceContext Instead of @Autowired for EntityManager

```java
// CORRECT
@PersistenceContext
private EntityManager entityManager;

// WRONG (but compiles)
@Autowired
private EntityManager entityManager;
```

| | `@PersistenceContext` | `@Autowired` |
|---|---|---|
| **What it injects** | A **transaction-scoped proxy** that delegates to the correct EntityManager for the current transaction | A single shared EntityManager bean instance |
| **Thread safety** | Safe — each thread/transaction gets its own EntityManager behind the proxy | Unsafe — same instance shared across threads |
| **Lifecycle** | New EntityManager per transaction, auto-closes when transaction ends | One instance for the entire application |
| **Standard** | JPA standard annotation | Spring-specific |

```text
Thread A (txn 1)  ──>  @PersistenceContext proxy  ──>  EntityManager A (txn 1's PC)
Thread B (txn 2)  ──>  @PersistenceContext proxy  ──>  EntityManager B (txn 2's PC)

vs.

Thread A  ──>  @Autowired  ──>  same EntityManager instance  <──  Thread B
                                  (SHARED = data corruption!)
```

`@PersistenceContext` creates a **shared proxy** that internally uses a `ThreadLocal` to route each call to the transaction-bound `EntityManager`. This is why it is thread-safe.

---

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

### How EntityManager Creates Persistence Context Internally

```text
EntityManagerFactory.createEntityManager()
         │
         v
new SessionImpl(...)                       ← Hibernate's EntityManager implementation
         │
         ├─ creates StatefulPersistenceContext
         │      ├─ entitiesByKey: Map<EntityKey, Object>        ← managed entities
         │      ├─ entitySnapshotsByKey: Map<EntityKey, Object[]> ← snapshot for dirty check
         │      └─ collectionsByKey: Map<CollectionKey, PersistentCollection>
         │
         ├─ creates ActionQueue
         │      ├─ insertions: List<AbstractEntityInsertAction>
         │      ├─ updates: List<EntityUpdateAction>
         │      ├─ deletions: List<EntityDeleteAction>
         │      └─ (flushed in order: inserts → updates → deletes)
         │
         └─ lazily acquires JDBC Connection from pool on first SQL
```

- The `PersistenceContext` is created **inside** the `Session` (EntityManager) at construction time.
- It is an in-memory structure — a map from entity key (type + id) to the entity object.
- **Snapshots** are taken when an entity enters the persistence context (on `persist()` or `find()`). At flush time, Hibernate compares the current field values to the snapshot to detect changes (dirty checking).
- The `ActionQueue` batches all pending database operations and executes them in the correct order during `flush()`.

---

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

### What is First-Level Caching

First-level cache is the **Persistence Context** itself. Every `EntityManager` automatically has its own first-level cache. You cannot disable it.

```text
entityManager.find(User.class, 1L)
         │
         v
  ┌─────────────────────────────────┐
  │  Persistence Context            │
  │  (first-level cache)            │
  │                                 │
  │  Key: User(id=1)                │
  │  ┌──────────┐                   │
  │  │ found?   │── YES ──> return cached object (NO SQL)
  │  └──────────┘                   │
  │       │                         │
  │       NO                        │
  │       │                         │
  │       v                         │
  │  Execute SELECT SQL             │
  │  Map ResultSet to User object   │
  │  Store in cache: User(id=1)     │
  │  Return entity                  │
  └─────────────────────────────────┘
```

**Rules of the first-level cache**

- Scoped to a **single EntityManager** instance (one cache per EntityManager).
- Lives as long as the EntityManager is open (or the transaction, in Spring's transaction-scoped mode).
- Guarantees **object identity**: `find(User.class, 1L) == find(User.class, 1L)` returns the exact same Java object reference.
- Cleared when you call `em.clear()` or `em.close()`.
- After the EntityManager is closed, a **new** EntityManager has an **empty** cache.

**Example code**

```java
@Transactional
public void firstLevelCacheDemo() {
    // SQL: SELECT * FROM users WHERE id = 1
    User user1 = entityManager.find(User.class, 1L);   // cache MISS → hits DB

    // NO SQL — same EntityManager, same transaction, same id → cache HIT
    User user2 = entityManager.find(User.class, 1L);

    System.out.println(user1 == user2);  // true — same object reference

    // Force cache eviction for this entity
    entityManager.detach(user1);

    // SQL: SELECT * FROM users WHERE id = 1  (cache was cleared for this entity)
    User user3 = entityManager.find(User.class, 1L);   // cache MISS → hits DB again

    System.out.println(user1 == user3);  // false — different object
}
```

**Console output (SQL log)**

```text
Hibernate: SELECT u.id, u.name, u.email FROM users u WHERE u.id = ?    ← first find()
                                                                        ← second find() — NO SQL
Hibernate: SELECT u.id, u.name, u.email FROM users u WHERE u.id = ?    ← third find() after detach
```

---

### Why and When the Entity Does Not Go to DB Directly

When you call `em.persist(entity)`, the entity is **not immediately written** to the database. It is added to the Persistence Context and queued in the **ActionQueue**. The actual SQL is executed later, at **flush time**.

```text
em.persist(user)
     │
     v
Persistence Context                          ActionQueue
┌────────────────────┐                  ┌─────────────────────┐
│ User(id=null,      │                  │ INSERT User pending  │
│   name="Alice")    │                  │                     │
│ state: MANAGED     │                  │ (not yet sent to DB) │
└────────────────────┘                  └─────────────────────┘
                                               │
                                    flush() or commit()
                                               │
                                               v
                                        ┌─────────────┐
                                        │  DATABASE    │
                                        │  INSERT INTO │
                                        │  users ...   │
                                        └─────────────┘
```

**When does flush happen?**

| Trigger | When |
|---|---|
| `em.flush()` | Explicit call — forces SQL execution immediately |
| Transaction commit | Spring calls flush automatically before committing |
| Before a JPQL/native query | Hibernate auto-flushes to ensure the query sees up-to-date data |

**Why does JPA delay the SQL?**

1. **Batching**: Multiple `persist()` calls can be combined into a single batch INSERT.
2. **Ordering**: Hibernate reorders operations (inserts before updates before deletes) to avoid constraint violations.
3. **Deduplication**: If you `persist()` an entity and then change a field before flush, Hibernate generates a single INSERT with the latest values instead of INSERT + UPDATE.
4. **Performance**: Fewer round-trips to the database.

**Example: persist does NOT hit DB immediately**

```java
@Transactional
public void delayedWriteDemo() {
    User user = new User("Alice");

    System.out.println("Before persist");
    entityManager.persist(user);         // NO SQL yet — entity added to Persistence Context
    System.out.println("After persist");

    user.setEmail("alice@example.com");  // still no SQL — dirty checking will use final value

    System.out.println("Before commit");
    // @Transactional method returns → Spring calls flush() → SQL executes → commit
}
```

```text
Console output:
Before persist
After persist
Before commit
Hibernate: INSERT INTO users (name, email) VALUES ('Alice', 'alice@example.com')
```

Notice: the INSERT has the **final** email value. Hibernate never generated a separate UPDATE — it waited until flush and used the latest state.

**When does entity go to DB directly?**

- When you call `em.flush()` explicitly.
- When Hibernate's **FlushMode** is `AUTO` (default) and you execute a query that overlaps with pending changes.
- When the transaction commits.

---

### How EntityManager is Created for Each Request

In a Spring Boot web application, the EntityManager lifecycle is managed through the **OpenEntityManagerInViewInterceptor** (or filter) and the **transaction infrastructure**.

```text
HTTP Request arrives
       │
       v
┌──────────────────────────────────────────────────────────────────────┐
│  DispatcherServlet                                                   │
│       │                                                              │
│       v                                                              │
│  OpenEntityManagerInViewInterceptor.preHandle()                      │
│       │                                                              │
│       ├─ EntityManagerFactory.createEntityManager()                   │
│       ├─ Bind EntityManager to current thread (ThreadLocal)          │
│       │                                                              │
│       v                                                              │
│  Handler (Controller) is called                                      │
│       │                                                              │
│       v                                                              │
│  @Service method with @Transactional                                 │
│       │                                                              │
│       ├─ TransactionManager detects the thread-bound EntityManager    │
│       ├─ Begins transaction on the existing EntityManager            │
│       ├─ Service logic runs (persist, find, etc.)                    │
│       ├─ On return: flush → commit → transaction ends                │
│       │                                                              │
│       v                                                              │
│  Controller returns response (view rendering can still lazy-load)    │
│       │                                                              │
│       v                                                              │
│  OpenEntityManagerInViewInterceptor.afterCompletion()                 │
│       │                                                              │
│       ├─ EntityManager.close()                                       │
│       └─ Remove EntityManager from ThreadLocal                       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
       │
       v
HTTP Response sent
```

**Two modes in Spring Boot**

| Mode | Property | Behavior |
|---|---|---|
| OSIV ON (default) | `spring.jpa.open-in-view=true` | EntityManager opened in interceptor `preHandle()`, closed in `afterCompletion()`. Lazy loading works in views. |
| OSIV OFF | `spring.jpa.open-in-view=false` | EntityManager only exists inside `@Transactional` boundaries. Lazy loading outside transaction throws `LazyInitializationException`. |

**With OSIV ON (default)**

```text
preHandle()               @Transactional              afterCompletion()
     │                    ┌──────────┐                      │
     │                    │ begin txn│                      │
  EM created              │ persist  │                   EM closed
  bound to thread         │ find     │                   unbound
     │                    │ flush    │                      │
     │                    │ commit   │                      │
     │                    └──────────┘                      │
     │                         │                            │
     │    lazy loading still works here (EM open)           │
     │◄────────────────────────┼───────────────────────────►│
         EntityManager is open for the full request
```

**With OSIV OFF**

```text
                          @Transactional
                          ┌──────────┐
                          │ EM created│
                          │ begin txn │
                          │ persist   │
                          │ find      │
                          │ flush     │
                          │ commit    │
                          │ EM closed │
                          └──────────┘
                               │
                    lazy load here → LazyInitializationException!
```

**The interceptor code (simplified)**

```java
// Spring's OpenEntityManagerInViewInterceptor (simplified)
public class OpenEntityManagerInViewInterceptor implements AsyncWebRequestInterceptor {

    private EntityManagerFactory emf;

    @Override
    public void preHandle(WebRequest request) {
        EntityManager em = emf.createEntityManager();
        // Bind to current thread so @PersistenceContext proxy can find it
        TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
    }

    @Override
    public void afterCompletion(WebRequest request, Exception ex) {
        EntityManagerHolder emHolder = (EntityManagerHolder)
            TransactionSynchronizationManager.unbindResource(emf);
        emHolder.getEntityManager().close();
    }
}
```

---

### First-Level Caching Explained with Code

```java
public class UserService {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    public void save(User user) {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        em.persist(user);
        em.find(User.class, 1L);
        em.find(User.class, 1L);
        em.getTransaction().commit();
        em.close();


        em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        em.find(User.class, 1L);
        em.find(User.class, 1L);
        em.getTransaction().commit();
        em.close();
    }
}
```

**Line-by-line breakdown**

```text
LINE                                      WHAT HAPPENS                                      SQL?
────                                      ─────────────                                     ────

EntityManager em = emf.createEntityManager()
                                          EM #1 created. Empty Persistence Context (PC #1). 
                                                                                            No

em.getTransaction().begin()
                                          Transaction started on EM #1.                     No

em.persist(user)
                                          user enters PC #1 as MANAGED.                     No (queued)
                                          If user.id is generated (e.g. @GeneratedValue),
                                          Hibernate MAY execute INSERT now to get the id.   
                                                                                            INSERT*

em.find(User.class, 1L)
                                          Case A: user was just persisted with id=1
                                            → PC #1 already has User(id=1) → cache HIT      No
                                          Case B: user has a different id
                                            → PC #1 does not have User(id=1) → cache MISS   SELECT

em.find(User.class, 1L)
                                          User(id=1) is now definitely in PC #1
                                          → cache HIT, returns same object                  No

em.getTransaction().commit()
                                          Flush: any pending INSERT/UPDATE executed.
                                          Transaction committed. PC #1 still alive.         INSERT/UPDATE

em.close()
                                          EM #1 closed. PC #1 destroyed.
                                          All entities become DETACHED.                     No

──────────────── NEW EntityManager ────────────────

em = emf.createEntityManager()
                                          EM #2 created. NEW empty Persistence Context      No
                                          (PC #2). Previous cache is GONE.

em.getTransaction().begin()
                                          Transaction started on EM #2.                     No

em.find(User.class, 1L)
                                          PC #2 is empty → cache MISS → must go to DB       SELECT

em.find(User.class, 1L)
                                          PC #2 now has User(id=1) → cache HIT              No

em.getTransaction().commit()
                                          Nothing to flush (only reads). Commit.            No

em.close()
                                          EM #2 closed. PC #2 destroyed.                    No
```

**Visual: Persistence Context state at each step**

```text
═══════════════ EM #1 / PC #1 ═══════════════

After persist(user):
    PC #1: { User(id=1, name="Alice") → MANAGED }

After first find(1L):
    PC #1: { User(id=1, name="Alice") → MANAGED }    ← same entry, cache hit (or loaded if different id)

After second find(1L):
    PC #1: { User(id=1, name="Alice") → MANAGED }    ← cache hit, NO SQL

After em.close():
    PC #1: DESTROYED — all entities detached


═══════════════ EM #2 / PC #2 ═══════════════

After createEntityManager():
    PC #2: { }    ← EMPTY, no knowledge of previous EM's cache

After first find(1L):
    PC #2: { User(id=1, name="Alice") → MANAGED }    ← cache miss, SELECT executed

After second find(1L):
    PC #2: { User(id=1, name="Alice") → MANAGED }    ← cache hit, NO SQL

After em.close():
    PC #2: DESTROYED
```

**Total SQL executed**

```text
EM #1:
  1. INSERT INTO users (name) VALUES ('Alice')       ← from persist() or flush
  2. SELECT * FROM users WHERE id = 1                ← first find() (if user.id != 1)
  (second find → cache hit → no SQL)

EM #2:
  3. SELECT * FROM users WHERE id = 1                ← first find() (new EM, empty cache)
  (second find → cache hit → no SQL)
```

**Key takeaway**: Closing the `EntityManager` destroys its Persistence Context (first-level cache). A new `EntityManager` starts fresh with an empty cache and must go to the database again, even for the same entity id.

---

### What is Second-Level Caching

The second-level (L2) cache is a **SessionFactory-scoped** cache shared across all `EntityManager` / `Session` instances in the application. Unlike the first-level cache (which dies with the EntityManager), the L2 cache survives across transactions.

```text
em.find(User.class, 1L)
         │
         v
┌─────────────────────────────────────────────────────────────────────┐
│  1st-Level Cache (Persistence Context)                              │
│  Scoped to: this EntityManager / this transaction                   │
│                                                                     │
│  User(id=1) found?                                                  │
│       │                                                             │
│    YES → return immediately (same object reference)                 │
│       │                                                             │
│    NO ↓                                                             │
├─────────────────────────────────────────────────────────────────────┤
│  2nd-Level Cache (SessionFactory-level)                             │
│  Scoped to: entire application, shared across all EntityManagers    │
│                                                                     │
│  User(id=1) found?                                                  │
│       │                                                             │
│    YES → hydrate entity from cached data, put in L1 cache, return  │
│       │                                                             │
│    NO ↓                                                             │
├─────────────────────────────────────────────────────────────────────┤
│  DATABASE                                                           │
│  SELECT * FROM users WHERE id = 1                                   │
│  → put result in L2 cache AND L1 cache, return entity               │
└─────────────────────────────────────────────────────────────────────┘
```

**Why use it?**

- Reduces database load for **read-heavy, rarely-changing** data (countries, categories, config, roles).
- Avoids repeated SELECT statements across different transactions/requests for the same entity.
- The L1 cache only helps within a single transaction. The L2 cache helps across all transactions.

**When to use it?**

| Good fit | Bad fit |
|---|---|
| Reference data (countries, currencies, roles) | Frequently updated data (orders, inventory) |
| Read-heavy, write-rare entities | Data that changes per request |
| Single-instance or single-DB apps | Multi-instance apps without distributed cache |
| Entities loaded by primary key often | Complex queries (L2 cache is entity-level, not query-level by default) |

**Code example**

```java
// Transaction A
@Transactional
public void txnA() {
    // L1 miss, L2 miss → SELECT from DB → stored in L2 and L1
    User user = entityManager.find(User.class, 1L);
}
// EntityManager A closes → L1 cache destroyed, L2 cache still has User(id=1)

// Transaction B (different EntityManager, maybe different thread)
@Transactional
public void txnB() {
    // L1 miss (new EM), L2 HIT → no SQL, entity hydrated from L2 cache
    User user = entityManager.find(User.class, 1L);
}
```

---

### How to Use It — Maven Dependencies

The L2 cache requires three layers:

```text
+------------------+      +-------------------+      +---------------------+
| Hibernate L2     | ---> | JCache API        | ---> | Cache Provider      |
| Cache SPI        |      | (JSR-107)         |      | (EhCache/Caffeine/  |
|                  |      |                   |      |  Hazelcast)         |
| hibernate-jcache |      | cache-api         |      | ehcache / caffeine  |
+------------------+      +-------------------+      +---------------------+
```

**Maven dependencies**

```xml
<!-- 1. Hibernate JCache integration — bridges Hibernate's L2 cache SPI to JCache API -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-jcache</artifactId>
</dependency>

<!-- 2. JCache API (JSR-107) — standard caching interface -->
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
</dependency>

<!-- 3. Pick ONE cache provider implementation -->

<!-- Option A: EhCache (most popular with Hibernate, feature-rich, XML-configurable) -->
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
    <classifier>jakarta</classifier>
</dependency>

<!-- Option B: Caffeine (lightweight, high-performance, in-memory only) -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>jcache</artifactId>
</dependency>

<!-- Option C: Hazelcast (distributed cache, works across multiple app instances) -->
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast</artifactId>
</dependency>
```

### Purpose of Each Import

| Dependency | What it does | Why you need it |
|---|---|---|
| `hibernate-jcache` | Hibernate's adapter that plugs into the JCache API | Tells Hibernate "use JCache as the L2 cache backend" |
| `cache-api` (JSR-107) | Standard Java caching interfaces (`CacheManager`, `Cache`, `CacheConfiguration`) | Common API so you can swap providers without changing code |
| `ehcache` | Actual cache implementation — stores data in heap/off-heap/disk | The engine that holds cached entities in memory |
| `caffeine` | Lightweight in-memory cache with high hit rates | Alternative to EhCache, best for single-instance apps |
| `hazelcast` | Distributed in-memory cache with cluster sync | Use when multiple app instances need to share cache state |

```text
Hibernate ──> hibernate-jcache ──> JCache API (cache-api) ──> EhCache / Caffeine / Hazelcast
              (bridge)              (standard interface)       (actual storage engine)
```

---

### Configuration in application.yml

**Using EhCache**

```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Enable L2 cache
        cache:
          use_second_level_cache: true
          use_query_cache: true          # optional: also cache JPQL/HQL query results
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        # Point to EhCache config file
        javax:
          cache:
            provider: org.ehcache.jsr107.EhcacheCachingProvider
            uri: classpath:ehcache.xml
```

**ehcache.xml** (in `src/main/resources/`)

```xml
<config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://www.ehcache.org/v3"
        xsi:noNamespaceSchemaLocation="http://www.ehcache.org/schema/ehcache-core-3.0.xsd">

    <!-- Default cache template -->
    <cache-template name="default">
        <expiry>
            <ttl unit="minutes">30</ttl>
        </expiry>
        <heap unit="entries">1000</heap>
    </cache-template>

    <!-- Entity-specific cache region -->
    <cache alias="com.example.entity.User" uses-template="default">
        <heap unit="entries">500</heap>
    </cache>

    <cache alias="com.example.entity.Country" uses-template="default">
        <expiry>
            <ttl unit="hours">24</ttl>
        </expiry>
        <heap unit="entries">300</heap>
    </cache>
</config>
```

**Using Caffeine (simpler, no XML)**

```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        javax:
          cache:
            provider: com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
```

---

### Annotations and Usage

**Entity-level annotations**

```java
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "users")
@Cacheable                                                     // JPA standard: marks entity as cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE,           // Hibernate-specific: concurrency strategy
       region = "com.example.entity.User")                     // cache region name (maps to ehcache.xml alias)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToMany(mappedBy = "user")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)        // cache the collection too
    private List<Address> addresses;
}
```

| Annotation | Source | Purpose |
|---|---|---|
| `@Cacheable` | JPA (`jakarta.persistence`) | Marks the entity as eligible for L2 caching |
| `@Cache` | Hibernate (`org.hibernate.annotations`) | Configures the concurrency strategy and cache region |
| `usage` | — | Defines how concurrent reads/writes to cached data are handled |
| `region` | — | Maps to a named cache in ehcache.xml. Defaults to the fully qualified class name |

---

### CacheConcurrencyStrategy — All Types Explained

```text
+─────────────────+───────────────+─────────────────+──────────────────────────────────────────────+
│ Strategy        │ Read Safe     │ Write Safe      │ Use Case                                      │
+─────────────────+───────────────+─────────────────+──────────────────────────────────────────────+
│ READ_ONLY       │ ✅            │ ❌ (never write) │ Truly immutable data: countries, zip codes     │
│ NONSTRICT_      │ ✅            │ ⚠️ eventual     │ Rarely updated, stale OK: user profiles        │
│ READ_WRITE      │               │                 │                                                │
│ READ_WRITE      │ ✅            │ ✅ (soft lock)   │ Most entities: users, products, categories     │
│ TRANSACTIONAL   │ ✅            │ ✅ (XA/JTA)      │ Distributed txn with JTA: banking, payments    │
+─────────────────+───────────────+─────────────────+──────────────────────────────────────────────+
```

**1. READ_ONLY**

- Entity is **never updated** after initial insert.
- Cache never needs invalidation for writes.
- Highest performance, lowest overhead.

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Country {
    @Id private String code;    // "US", "IN", "DE"
    private String name;        // never changes
}
```

**Business context**: Country lists, currency codes, static configuration, enum-like lookup tables.

**2. NONSTRICT_READ_WRITE**

- Reads are cached, writes invalidate the cache **after** commit.
- Between the write commit and cache invalidation, other transactions may see **stale** data (brief window).
- No locking, so highest write throughput among writable strategies.

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UserProfile {
    @Id private Long id;
    private String bio;         // rarely updated, stale for a moment is OK
    private String avatarUrl;
}
```

**Business context**: User profiles, blog posts, settings that update infrequently and brief staleness is acceptable.

**3. READ_WRITE**

- Uses **soft locks** to prevent stale reads during writes.
- When a transaction updates an entity, the cache entry is locked. Other transactions see a cache miss and go to the DB until the lock is released on commit.
- **No stale reads**, but slightly more overhead than NONSTRICT.

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {
    @Id private Long id;
    private String name;
    private BigDecimal price;   // price updates must be immediately visible
}
```

```text
Write flow with READ_WRITE:

Thread A: UPDATE Product(id=1) price = 99.99
     │
     ├─ Soft-lock cache entry for Product(id=1)
     ├─ Execute UPDATE SQL
     ├─ Commit transaction
     └─ Release soft-lock, update cache with new value

Thread B: find(Product.class, 1L) during soft-lock
     │
     ├─ L2 cache: entry is soft-locked → treat as cache MISS
     ├─ Go to DB → gets latest value
     └─ (no stale read)
```

**Business context**: Products, inventory, employee records, anything that updates sometimes and stale reads are unacceptable.

**4. TRANSACTIONAL**

- Uses **full XA/JTA transactions** to coordinate cache and database.
- Cache participates in the same distributed transaction as the DB.
- Requires a JTA-capable cache provider (e.g., EhCache with JTA, Infinispan).
- Heaviest overhead, strongest consistency.

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class BankAccount {
    @Id private Long id;
    private BigDecimal balance;  // absolute consistency required
}
```

**Business context**: Financial systems, banking, payment processing where cache and DB must always be in sync.

### Why READ_WRITE is Used for Most Use Cases

```text
                      READ_ONLY   NONSTRICT_RW   READ_WRITE   TRANSACTIONAL
───────────────────   ─────────   ────────────   ──────────   ─────────────
Performance           ★★★★★       ★★★★           ★★★          ★★
Consistency           immutable   eventual       strong       strongest
Write support         ❌           ✅              ✅            ✅
Stale reads           N/A         possible       prevented    prevented
Infrastructure        minimal     minimal        minimal      JTA required
───────────────────   ─────────   ────────────   ──────────   ─────────────
```

- Most entities **do** get updated occasionally, so `READ_ONLY` is too restrictive.
- Stale data is usually unacceptable in business apps, so `NONSTRICT_READ_WRITE` is risky.
- `TRANSACTIONAL` requires JTA infrastructure, which most Spring Boot apps do not use.
- `READ_WRITE` offers the **best balance**: strong consistency with soft locks, no JTA required, works with standard RESOURCE_LOCAL transactions.

---

### How Update and Cache Invalidation Happens

**Update flow (READ_WRITE strategy)**

```text
Step 1: @Transactional method starts
        EntityManager opened, L1 cache empty

Step 2: user = em.find(User.class, 1L)
        L1 miss → L2 hit → entity hydrated into L1 as MANAGED

Step 3: user.setName("Bob")
        L1 dirty checking detects change

Step 4: Method returns → flush
        ├─ Hibernate generates: UPDATE users SET name='Bob' WHERE id=1
        ├─ BEFORE transaction commit: soft-lock L2 cache entry for User(id=1)
        │     (other threads see cache miss during lock)
        ├─ JDBC commit
        └─ AFTER commit: update L2 cache entry with new data, release soft-lock

Step 5: Next transaction calls find(User.class, 1L)
        L2 cache has updated value → cache HIT with fresh data
```

```text
Timeline:
─────────────────────────────────────────────────────
  T1: begin          flush       commit      after-commit
       │               │           │              │
       │               │        soft-lock      update L2
       │               │        L2 entry       release lock
       │               │           │              │
  T2:                       find(1L)
                            L2 locked → cache MISS → go to DB
                                                      │
  T3:                                              find(1L)
                                                   L2 updated → cache HIT (fresh data)
```

**Delete flow**

```text
em.remove(user)  →  flush  →  DELETE FROM users WHERE id=1  →  commit  →  EVICT from L2 cache
```

**Manual cache eviction**

```java
// Evict a specific entity from L2 cache
sessionFactory.getCache().evict(User.class, userId);

// Evict all User entities from L2 cache
sessionFactory.getCache().evict(User.class);

// Evict everything from L2 cache
sessionFactory.getCache().evictAllRegions();
```

---

### Multi-Instance: L2 Cache vs Redis with @Cacheable

In a multi-instance environment (multiple Spring Boot instances behind a load balancer, all connecting to the same PostgreSQL), the choice depends on the architecture.

```text
Scenario: 3 Spring Boot instances → 1 PostgreSQL

┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Instance A   │    │ Instance B   │    │ Instance C   │
│              │    │              │    │              │
│ L2 Cache (A) │    │ L2 Cache (B) │    │ L2 Cache (C) │
│ (in-memory)  │    │ (in-memory)  │    │ (in-memory)  │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────┴──────┐
                    │ PostgreSQL  │
                    └─────────────┘

Problem: Instance A updates User(id=1).
         Instance A's L2 cache is updated.
         Instance B and C still have STALE data in their L2 caches!
```

**Option 1: Hibernate L2 Cache with distributed provider (Hazelcast/Infinispan)**

```text
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Instance A   │    │ Instance B   │    │ Instance C   │
│              │    │              │    │              │
│ L2 Cache ────┼────┼── L2 Cache ──┼────┼── L2 Cache   │
│ (Hazelcast   │    │ (Hazelcast   │    │ (Hazelcast   │
│  cluster)    │    │  cluster)    │    │  cluster)    │
└──────────────┘    └──────────────┘    └──────────────┘
     Hazelcast syncs cache across all instances automatically
```

- Works, but adds complexity (Hazelcast cluster management, network overhead).
- Only works for entity-level caching (by primary key).

**Option 2: Redis with Spring @Cacheable (recommended for multi-instance)**

```text
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Instance A   │    │ Instance B   │    │ Instance C   │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────┴──────┐
                    │    Redis    │    ← single source of truth for cache
                    │  (shared)   │
                    └──────┬──────┘
                           │
                    ┌──────┴──────┐
                    │ PostgreSQL  │
                    └─────────────┘

Instance A updates User(id=1) → updates Redis → all instances see fresh data
```

**Redis + @Cacheable code example**

```yaml
# application.yml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Cacheable(value = "users", key = "#id")           // cache on read
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    @CachePut(value = "users", key = "#user.id")       // update cache on write
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#id")           // evict from cache on delete
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @CacheEvict(value = "users", allEntries = true)     // clear ALL user cache
    public void clearUserCache() {}
}
```

**Comparison table**

| | Hibernate L2 Cache (EhCache) | Hibernate L2 Cache (Hazelcast) | Redis + @Cacheable |
|---|---|---|---|
| **Scope** | Single JVM | Distributed (cluster) | Centralized (shared server) |
| **Multi-instance safe** | ❌ stale data | ✅ cluster sync | ✅ single source |
| **Cache level** | Entity-level (by PK) | Entity-level (by PK) | Any level (method result, DTO, custom key) |
| **Integration** | Deep Hibernate integration, automatic | Deep Hibernate integration, auto-sync | Spring Cache abstraction, manual annotations |
| **Flexibility** | Only works with `em.find()` by PK | Only works with `em.find()` by PK | Cache any method result, any key pattern |
| **Infrastructure** | None (in-process) | Hazelcast cluster | Redis server |
| **Best for** | Single-instance apps | Multi-instance, entity-heavy | Multi-instance, flexible caching |

**Recommendation for multi-instance**

```text
Single instance app               → Hibernate L2 Cache with EhCache/Caffeine
                                     (simple, zero infrastructure, automatic)

Multi-instance + entity PK lookups → Hibernate L2 Cache with Hazelcast
                                     (if you want transparent ORM-level caching)

Multi-instance + flexible caching  → Redis + @Cacheable / @CachePut / @CacheEvict
                                     (recommended — works for any method, any key,
                                      DTOs, aggregated results, not just entities)

Multi-instance + both needs        → Redis for application-level caching (@Cacheable)
                                     + Hibernate L2 with Hazelcast for entity-level
                                     (maximum performance, highest complexity)
```

For most production multi-instance Spring Boot apps connecting to PostgreSQL: **use Redis with `@Cacheable`**. It gives you a single shared cache, works for any type of result (not just JPA entities), and is simpler to reason about than distributed Hibernate L2 caching.

---

### spring.jpa.hibernate.ddl-auto

This property controls what Hibernate does to the database **schema** at application startup. It compares entity classes against the existing tables and decides what DDL to execute.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update   # one of: none, validate, update, create, create-drop
```

**All options explained**

| Value | What it does | SQL generated at startup | Data preserved? |
|---|---|---|---|
| `none` | Does absolutely nothing to the schema | None | Yes |
| `validate` | Compares entities against DB schema, throws exception if mismatch | None (read-only check) | Yes |
| `update` | Adds missing tables/columns, never drops anything | `ALTER TABLE ADD COLUMN ...`, `CREATE TABLE ...` | Yes |
| `create` | Drops all managed tables, then re-creates them | `DROP TABLE ...`, `CREATE TABLE ...` | **No — all data lost** |
| `create-drop` | Same as `create`, but also drops tables when app shuts down | `DROP TABLE` on startup + shutdown | **No — all data lost** |

**Detailed breakdown**

**1. `none`**

- Hibernate does not touch the schema at all.
- You manage schema externally via Flyway, Liquibase, or manual SQL.
- **Use in**: Production, always. Schema changes should be version-controlled migrations, not auto-generated.

```yaml
# Production
spring.jpa.hibernate.ddl-auto=none
# Combined with Flyway or Liquibase for migrations
```

**2. `validate`**

- Hibernate checks that every `@Entity`, `@Table`, `@Column` annotation matches the actual DB schema.
- If a table or column is missing, the app **fails to start** with `SchemaManagementException`.
- Does not modify the database.
- **Use in**: Staging/pre-production to catch mismatches between code and schema after a migration.

```text
Entity: @Column(name = "email") on User
DB:     users table has no "email" column

Result: App fails at startup → SchemaManagementException:
        "Schema-validation: missing column [email] in table [users]"
```

**3. `update`**

- Hibernate scans entities and adds any missing tables or columns.
- **Never drops** columns, tables, or constraints (even if you removed the field from the entity).
- May generate `ALTER TABLE ADD COLUMN`, `CREATE TABLE`, `CREATE INDEX`.
- **Use in**: Local development for quick iteration.

```text
You add a new field to User:
   private String phone;

At startup, Hibernate runs:
   ALTER TABLE users ADD COLUMN phone VARCHAR(255);

You REMOVE the phone field from User:
   Hibernate does NOTHING — the column stays in the DB (orphaned column)
```

**4. `create`**

- Drops all tables Hibernate manages, then re-creates them from scratch.
- **All data is destroyed** on every startup.
- **Use in**: Unit testing with in-memory databases (H2).

```text
Startup:
   DROP TABLE IF EXISTS users CASCADE;
   DROP TABLE IF EXISTS addresses CASCADE;
   CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(255), ...);
   CREATE TABLE addresses (id BIGINT PRIMARY KEY, ...);
```

**5. `create-drop`**

- Same as `create`, plus drops all tables when the `SessionFactory` is closed (app shutdown).
- **Use in**: Integration tests where you need a clean database per test run.

```text
Startup:   DROP + CREATE all tables
Shutdown:  DROP all tables
```

**Real-life usage by environment**

```text
┌────────────────┬──────────────┐
│  Environment   │  ddl-auto    │
├────────────────┼──────────────┤
│  Local dev     │  update      │
│  Unit tests    │  create-drop │
│  Integration   │  create-drop │
│  Staging       │  validate    │
│  Production    │  none        │
└────────────────┴──────────────┘
```

---

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

### @Column Annotation

`@Column` maps an entity field to a specific database column. It is **optional** — if omitted, Hibernate maps the field name directly to a column of the same name.

**Properties of @Column**

| Property | Purpose | Default |
|---|---|---|
| `name` | Column name in DB | Field name |
| `unique` | Adds a unique constraint on this column | `false` |
| `nullable` | Whether the column allows NULL | `true` |
| `length` | Column length (for `VARCHAR`) | `255` |
| `columnDefinition` | Raw DDL for the column type | Provider-inferred |
| `precision` | Total digits for `DECIMAL` | `0` |
| `scale` | Decimal digits for `DECIMAL` | `0` |
| `insertable` | Include in INSERT statements | `true` |
| `updatable` | Include in UPDATE statements | `true` |

**Examples**

```java
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String name;
    // → full_name VARCHAR(100) NOT NULL

    @Column(unique = true, nullable = false, length = 150)
    private String email;
    // → email VARCHAR(150) NOT NULL UNIQUE

    @Column(nullable = true, length = 500)
    private String bio;
    // → bio VARCHAR(500) NULL

    @Column(precision = 10, scale = 2)
    private BigDecimal salary;
    // → salary DECIMAL(10,2)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    // → created_at TIMESTAMP — excluded from UPDATE statements

    @Column(columnDefinition = "TEXT")
    private String notes;
    // → notes TEXT (raw DDL override)
}
```

Generated DDL:
```sql
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    bio VARCHAR(500),
    salary DECIMAL(10,2),
    created_at TIMESTAMP,
    notes TEXT
);
```

---

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

### @SequenceGenerator

`@SequenceGenerator` configures a database **sequence** that generates unique numeric IDs. A sequence is a database-level object that produces auto-incrementing numbers independently of any table.

```text
┌────────────────────────┐
│  Database Sequence     │
│  employee_seq          │
│                        │
│  nextval() → 1         │
│  nextval() → 2         │
│  nextval() → 3         │
│  ...                   │
└────────────────────────┘
```

**Properties of @SequenceGenerator**

| Property | Purpose | Default |
|---|---|---|
| `name` | Logical name used by `@GeneratedValue(generator = "...")` | Required |
| `sequenceName` | Actual sequence name in the database | Generator name |
| `initialValue` | Starting value of the sequence | `1` |
| `allocationSize` | How many IDs Hibernate pre-fetches per DB call (batch) | `50` |
| `schema` | Database schema containing the sequence | Default schema |

**allocationSize explained**

```text
allocationSize = 50 (default):

Hibernate calls: SELECT nextval('employee_seq')  →  returns 1
Hibernate now has IDs 1–50 in memory (no more DB calls needed for next 49 inserts)

After 50 persists:
Hibernate calls: SELECT nextval('employee_seq')  →  returns 51
Hibernate now has IDs 51–100 in memory

Benefit: 1 DB round-trip per 50 inserts instead of 1 per insert
```

**Code example**

```java
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emp_seq_gen")
    @SequenceGenerator(
        name = "emp_seq_gen",           // logical name (matches generator = )
        sequenceName = "employee_seq",   // actual DB sequence name
        initialValue = 1,
        allocationSize = 50
    )
    private Long id;

    private String name;
}
```

The database sequence must exist (or Hibernate creates it if `ddl-auto=update`):

```sql
CREATE SEQUENCE employee_seq START WITH 1 INCREMENT BY 50;
```

**Should you use @SequenceGenerator or create the sequence manually via SQL?**

| Approach | When to use |
|---|---|
| Let Hibernate create it (`ddl-auto=update`) | Local development only |
| Create via Flyway/Liquibase migration | Production — sequence is version-controlled, reviewed, and repeatable |
| Use a shared Sequence Service API | Enterprise — when multiple teams/microservices need unique IDs across tables |

**Shared Sequence Service pattern (industry practice)**

In large enterprise architectures, teams create a standalone microservice or shared library for generating unique IDs. This decouples ID generation from any specific table or database.

```text
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Service A    │    │ Service B    │    │ Service C    │
│ (Orders)     │    │ (Users)      │    │ (Payments)   │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────┴──────────┐
                    │ ID Generator    │
                    │ Service / API   │
                    │                 │
                    │ GET /next-id    │
                    │ ?entity=order   │
                    │                 │
                    │ Uses: DB seq,   │
                    │ Snowflake, UUID │
                    │ or custom algo  │
                    └─────────────────┘
```

```java
// Simplified sequence service
@RestController
@RequestMapping("/api/sequences")
public class SequenceController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/next")
    public Long getNextId(@RequestParam String sequenceName) {
        return jdbcTemplate.queryForObject(
            "SELECT nextval(?)", Long.class, sequenceName
        );
    }
}

// Client microservice uses the ID before persisting
Long orderId = restTemplate.getForObject(
    "http://id-service/api/sequences/next?sequenceName=order_seq", Long.class);
order.setId(orderId);
orderRepository.save(order);
```

---

### @GeneratedValue and Generation Strategies

`@GeneratedValue` tells JPA **how** to auto-generate the primary key value. It works only with single `@Id` fields — **not** with composite keys (`@EmbeddedId` / `@IdClass`).

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**All strategies**

```text
┌───────────────────┬───────────────────────────────────────────────────────┐
│ Strategy          │ How it generates IDs                                  │
├───────────────────┼───────────────────────────────────────────────────────┤
│ IDENTITY          │ DB auto-increment column (MySQL AUTO_INCREMENT)       │
│ SEQUENCE          │ DB sequence object (PostgreSQL, Oracle)               │
│ TABLE             │ Simulates sequence using a dedicated DB table         │
│ AUTO              │ Hibernate picks the best strategy for the DB dialect  │
└───────────────────┴───────────────────────────────────────────────────────┘
```

---

**GenerationType.IDENTITY**

The database generates the ID using an auto-increment column. Hibernate gets the ID **after** the INSERT.

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal price;
}
```

```text
How IDENTITY works:

em.persist(product)
     │
     ├─ product.id is NULL at this point
     │
     ├─ Hibernate MUST execute INSERT immediately
     │   (cannot defer — it needs the generated ID)
     │
     ├─ INSERT INTO products (name, price) VALUES ('Widget', 9.99)
     │
     ├─ DB returns generated ID (e.g., 42)
     │
     └─ product.id = 42 (now assigned)
```

Generated DDL (MySQL):
```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    price DECIMAL(19,2)
);
```

| Pros | Cons |
|---|---|
| Simple, no extra DB objects needed | INSERT cannot be batched (must happen immediately to get the ID) |
| Works well with MySQL | Breaks Hibernate's write-behind optimization |
| No allocationSize tuning needed | Not available in all databases (Oracle pre-12c had no identity columns) |

**Use when**: Simple apps with MySQL, low write throughput, batch insert performance is not a concern.

---

**GenerationType.SEQUENCE**

Uses a database sequence object to generate IDs. Hibernate fetches the next value from the sequence **before** the INSERT, so the ID is known immediately and INSERTs can be batched.

```java
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "emp_seq_gen"                  // references @SequenceGenerator.name
    )
    @SequenceGenerator(
        name = "emp_seq_gen",                       // logical name
        sequenceName = "employee_seq",              // actual DB sequence
        allocationSize = 50                         // pre-fetch 50 IDs at a time
    )
    private Long id;

    private String name;
}
```

```text
How SEQUENCE works:

em.persist(employee)
     │
     ├─ Hibernate calls: SELECT nextval('employee_seq')  → returns 1
     │   (IDs 1–50 now reserved in memory)
     │
     ├─ employee.id = 1 (assigned immediately, before INSERT)
     │
     ├─ INSERT is QUEUED in ActionQueue (not executed yet!)
     │
     ├─ em.persist(employee2)
     │   employee2.id = 2 (from memory, no DB call)
     │
     ├─ ... persist 48 more ...
     │   all IDs 3–50 from memory, zero DB round-trips
     │
     ├─ em.persist(employee51)
     │   Hibernate calls: SELECT nextval('employee_seq')  → returns 51
     │   IDs 51–100 now reserved
     │
     └─ flush() → BATCH INSERT all 51 rows in one go
```

```text
Why SEQUENCE + @SequenceGenerator is heavily used in industry:

1. BATCH INSERTS:   ID is known before INSERT → Hibernate can batch multiple INSERTs
2. PERFORMANCE:     allocationSize=50 means 1 DB call per 50 inserts (vs 1 per insert with IDENTITY)
3. PORTABILITY:     Sequences work across PostgreSQL, Oracle, H2, SQL Server
4. WRITE-BEHIND:    Hibernate can optimize flush order since it doesn't need immediate INSERT
5. STANDARD:        JPA standard, supported by all JPA providers
```

**What is the `generator` property?**

`generator` in `@GeneratedValue(generator = "emp_seq_gen")` is a **logical name** that links to the `@SequenceGenerator(name = "emp_seq_gen")`. It tells Hibernate "use this specific sequence configuration to generate IDs".

```text
@GeneratedValue(generator = "emp_seq_gen")  ─── links to ──→  @SequenceGenerator(name = "emp_seq_gen")
                                                                      │
                                                               sequenceName = "employee_seq"
                                                               allocationSize = 50
```

| Pros | Cons |
|---|---|
| Supports JDBC batch inserts | Requires a sequence object in the DB |
| allocationSize reduces DB calls | allocationSize mismatch between app and DB sequence INCREMENT causes ID gaps |
| ID available before INSERT | Slightly more setup than IDENTITY |
| Works with all major DBs | — |

**Use when**: Production applications, especially with PostgreSQL/Oracle. This is the **recommended default** for most JPA projects.

---

**GenerationType.TABLE**

Simulates a sequence by using a dedicated table that stores the last generated ID. Hibernate locks a row, reads the current value, increments it, and uses it as the new ID.

```java
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(
        strategy = GenerationType.TABLE,
        generator = "invoice_table_gen"
    )
    @TableGenerator(
        name = "invoice_table_gen",
        table = "id_generator",              // the table that stores IDs
        pkColumnName = "gen_name",           // column that holds the generator name
        valueColumnName = "gen_value",       // column that holds the current ID value
        pkColumnValue = "invoice_id",        // value in gen_name for this entity
        allocationSize = 50
    )
    private Long id;

    private BigDecimal amount;
}
```

```text
The id_generator table looks like:

+──────────────+───────────+
│  gen_name    │ gen_value  │
+──────────────+───────────+
│  invoice_id  │  1050      │
│  order_id    │  5023      │
│  user_id     │  340       │
+──────────────+───────────+

How TABLE strategy works:

em.persist(invoice)
     │
     ├─ BEGIN TRANSACTION on id_generator table
     ├─ SELECT gen_value FROM id_generator WHERE gen_name = 'invoice_id' FOR UPDATE
     │   → returns 1050
     ├─ UPDATE id_generator SET gen_value = 1100 WHERE gen_name = 'invoice_id'
     ├─ COMMIT
     │
     ├─ invoice.id = 1051 (IDs 1051–1100 reserved in memory)
     │
     └─ INSERT INTO invoices ...
```

| Pros | Cons |
|---|---|
| Works on ALL databases (even those without sequences) | **Row-level locking** on the generator table → contention under load |
| Portable | Worst performance of all strategies |
| One table for all generators | Extra table to maintain |

**Why it is NOT used much**

- The generator table becomes a **bottleneck** under concurrent writes — every ID allocation locks a row.
- Most modern databases support sequences natively (PostgreSQL, Oracle, SQL Server, H2).
- `SEQUENCE` strategy gives the same benefits without the locking overhead.
- Only consider `TABLE` if you must support a database that has **no sequence support** (e.g., very old MySQL versions before 8.0).

---

**Strategy comparison**

```text
                    IDENTITY       SEQUENCE        TABLE
───────────────── ──────────── ──────────────── ────────────
ID known before
INSERT?              No            Yes             Yes

Batch INSERT
supported?           No            Yes             Yes

DB round-trips
per N inserts        N             N/alloc_size    N/alloc_size + lock

Requires extra
DB object?           No            Sequence        Table

Locking overhead     None          None            Row lock on generator table

Performance rank     ★★★           ★★★★★           ★★

Recommended?         Simple apps   Production      Last resort
                     MySQL         default
```




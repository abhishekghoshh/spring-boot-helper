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



## Udemy

- [Master Hibernate and JPA with Spring Boot in 100 Steps](https://www.udemy.com/course/hibernate-jpa-tutorial-for-beginners-in-100-steps/)


## Youtube

- [Hibernate & JPA Tutorial - Crash Course](https://www.youtube.com/watch?v=xHminZ9Dxm4)
    - [marcobehlerjetbrains/hibernate-tutorial](https://github.com/marcobehlerjetbrains/hibernate-tutorial)


- [Concept && Coding - by Shrayansh](https://www.youtube.com/@ConceptAndCodingByShrayansh)

- [Daily Code Buffer](https://www.youtube.com/@DailyCodeBuffer)
    - [Spring Data JPA Tutorial | Full In-depth Course](https://www.youtube.com/watch?v=XszpXoII9Sg)
    - [MySQL Integration with Spring Boot Application | Daily Code Buffer](https://www.youtube.com/watch?v=qsioDgfs7jc)

- [Master Spring Data JPA In One Video | Hindi](https://www.youtube.com/watch?v=8SxJNqeq_zc)

- [Spring Security JPA Authentication in Spring Boot](https://www.youtube.com/watch?v=awcCiqBO36E)


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



### OneToOne Unidirectional Mapping

In a unidirectional `@OneToOne` mapping, only the **parent** entity has a reference to the **child** entity. The child has no knowledge of the parent.

```text
Java Objects:                              Database Tables:

┌───────────────────┐                      ┌───────────────────────────┐
│  User             │                      │  users                    │
│                   │                      ├───────────────────────────┤
│  id: Long         │                      │  id (PK, BIGINT)          │
│  name: String     │    @OneToOne         │  name (VARCHAR)           │
│  address: Address ├───────────────>      │  address_id (FK, BIGINT)  │──┐
│                   │                      │                           │  │
└───────────────────┘                      └───────────────────────────┘  │
                                                                          │ references
┌───────────────────┐                      ┌───────────────────────────┐  │
│  Address          │                      │  addresses                │  │
│                   │                      ├───────────────────────────┤  │
│  id: Long         │                      │  id (PK, BIGINT)          │<─┘
│  city: String     │                      │  city (VARCHAR)           │
│  zipCode: String  │                      │  zip_code (VARCHAR)       │
│                   │  (no ref to User)    │                           │
└───────────────────┘                      └───────────────────────────┘

Direction: User ──────> Address   (one way only)
           User knows Address.   Address does NOT know User.
```

**Code example**

```java
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    // constructors, getters, setters
    // NO reference to User — this is unidirectional
}
```

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;       // User has a reference to Address

    // constructors, getters, setters
}
```

**Usage**

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User createUserWithAddress() {
        Address address = new Address();
        address.setCity("Bangalore");
        address.setZipCode("560001");

        User user = new User();
        user.setName("Alice");
        user.setAddress(address);   // set the child on parent

        return userRepository.save(user);
        // CascadeType.ALL → saving User also saves Address automatically
    }
}
```

```text
SQL executed:
  INSERT INTO addresses (city, zip_code) VALUES ('Bangalore', '560001');     ← Address saved first (child)
  INSERT INTO users (name, address_id) VALUES ('Alice', 1);                  ← User saved with FK to Address
```

---

### What is Cascade Type

Cascade defines how **persistence operations on the parent entity propagate to the child entity**. Without cascading, you would need to explicitly save, update, and delete each entity individually.

```text
WITHOUT Cascade:                           WITH CascadeType.ALL:

userRepository.save(user);                 userRepository.save(user);
addressRepository.save(address);           // Address is saved automatically!
// You must manually save both

userRepository.delete(user);               userRepository.delete(user);
addressRepository.delete(address);         // Address is deleted automatically!
// You must manually delete both
```

**Why cascading is needed**

In a parent-child relationship, the child's lifecycle often depends on the parent:

```text
┌────────────┐    owns    ┌────────────┐
│   User     │ ──────────>│  Address   │
│  (parent)  │            │  (child)   │
└────────────┘            └────────────┘

Parent created  → Child should also be created  (CASCADE PERSIST)
Parent updated  → Child should also be updated  (CASCADE MERGE)
Parent deleted  → Child should also be deleted  (CASCADE REMOVE)
Parent detached → Child should also be detached (CASCADE DETACH)
Parent refreshed→ Child should also be refreshed(CASCADE REFRESH)
```

Without cascade, the child entity is an **orphan** — the parent has a reference to it but JPA does not know to propagate operations to it. You get errors like:

```text
org.hibernate.TransientObjectException:
  object references an unsaved transient instance — save the transient instance before flushing
```

This happens when you `persist(user)` but `address` is still in `NEW` state and was not persisted separately.

---

### All Cascade Types Explained

```text
┌──────────────────┬──────────────────────────────────────────────────────────────┐
│ CascadeType      │ What it does                                                │
├──────────────────┼──────────────────────────────────────────────────────────────┤
│ PERSIST          │ When parent is persisted, child is also persisted            │
│ MERGE            │ When parent is merged (updated), child is also merged        │
│ REMOVE           │ When parent is removed, child is also removed               │
│ REFRESH          │ When parent is refreshed from DB, child is also refreshed   │
│ DETACH           │ When parent is detached, child is also detached             │
│ ALL              │ All of the above combined                                    │
└──────────────────┴──────────────────────────────────────────────────────────────┘
```

**CascadeType.PERSIST**

```java
@OneToOne(cascade = CascadeType.PERSIST)
@JoinColumn(name = "address_id")
private Address address;
```

```java
Address address = new Address();
address.setCity("Bangalore");

User user = new User();
user.setName("Alice");
user.setAddress(address);

userRepository.save(user);
// Hibernate executes:
//   INSERT INTO addresses (city, zip_code) VALUES ('Bangalore', null);
//   INSERT INTO users (name, address_id) VALUES ('Alice', 1);
```

**CascadeType.MERGE**

```java
@OneToOne(cascade = CascadeType.MERGE)
@JoinColumn(name = "address_id")
private Address address;
```

```java
// user and address were previously saved, now detached
user.setName("Bob");
user.getAddress().setCity("Mumbai");      // modify child too

userRepository.save(user);               // save() calls merge() for existing entities
// Hibernate executes:
//   UPDATE users SET name = 'Bob' WHERE id = 1;
//   UPDATE addresses SET city = 'Mumbai' WHERE id = 1;
```

**CascadeType.REMOVE**

```java
@OneToOne(cascade = CascadeType.REMOVE)
@JoinColumn(name = "address_id")
private Address address;
```

```java
userRepository.delete(user);
// Hibernate executes:
//   DELETE FROM users WHERE id = 1;          ← parent deleted
//   DELETE FROM addresses WHERE id = 1;      ← child also deleted automatically
```

**CascadeType.REFRESH**

```java
@OneToOne(cascade = CascadeType.REFRESH)
@JoinColumn(name = "address_id")
private Address address;
```

```java
// Someone updated the address directly in DB
entityManager.refresh(user);
// Hibernate re-reads BOTH user AND address from DB
// Overwrites any in-memory changes with database values
```

**CascadeType.DETACH**

```java
@OneToOne(cascade = CascadeType.DETACH)
@JoinColumn(name = "address_id")
private Address address;
```

```java
entityManager.detach(user);
// Both user AND address become DETACHED from persistence context
// Changes to either will NOT be auto-detected by dirty checking
```

**CascadeType.ALL**

```java
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "address_id")
private Address address;
```

Equivalent to `{PERSIST, MERGE, REMOVE, REFRESH, DETACH}`. All operations propagate.

**When to use which Cascade type**

| Cascade Type | When to use |
|---|---|
| `PERSIST` | Child should be auto-saved when parent is saved. Use when child cannot exist without parent. |
| `MERGE` | Child should be auto-updated when parent is updated. Common for parent-child forms. |
| `REMOVE` | Child should be auto-deleted when parent is deleted. Use for owned entities (address, profile). |
| `REFRESH` | Need to reload the entire object graph from DB. Rare — used in long conversations or concurrent environments. |
| `DETACH` | Need to detach the entire graph. Rare. |
| `ALL` | Child lifecycle is fully owned by parent. **Most common in industry for @OneToOne.** |

**Which Cascade is used most in industry?**

```text
@OneToOne  →  CascadeType.ALL          (child lifecycle = parent lifecycle)
@OneToMany →  CascadeType.ALL          (children owned by parent)
@ManyToOne →  Usually NO cascade       (many children share one parent — don't cascade delete!)
@ManyToMany→  CascadeType.PERSIST + MERGE (shared entities, never cascade remove)
```

`CascadeType.ALL` is the most common for `@OneToOne` because the child entity (Address, Profile, Passport) is fully owned by the parent (User). If the User is deleted, the Address should also be deleted. If the User is saved, the Address should be saved too.

---

### How OneToOne Mapping Converts to Table Structure

When Hibernate encounters `@OneToOne` on a field, it creates a **foreign key column** in the **parent table** that points to the **primary key** of the child table.

```text
@Entity User has:
    @OneToOne
    private Address address;

Hibernate generates:

┌──────────────────────────────┐          ┌─────────────────────────┐
│  users                       │          │  addresses              │
├──────────────────────────────┤          ├─────────────────────────┤
│  id          BIGINT (PK)     │          │  id       BIGINT (PK)   │
│  name        VARCHAR(255)    │          │  city     VARCHAR(255)   │
│  address_id  BIGINT (FK) ────┼─────────>│  zip_code VARCHAR(255)  │
│              UNIQUE          │          │                         │
└──────────────────────────────┘          └─────────────────────────┘
```

**How Hibernate decides the foreign key column name**

```text
Default naming convention:

  Field name in Java:   address
  Separator:            _
  Referenced PK column: id

  FK column name:       address_id
```

So `User.address` becomes `address_id` in the `users` table, referencing the `id` column in the `addresses` table.

**How Hibernate automatically chooses the primary key of the referred table**

```text
@OneToOne
private Address address;

Hibernate:
  1. Looks at the type of the field → Address.class
  2. Finds @Entity Address
  3. Finds the field marked with @Id in Address → "id"
  4. Uses that as the referencedColumnName for the FK
  5. Creates: address_id BIGINT REFERENCES addresses(id)
```

If `Address` had `@Id private Long addressId`, Hibernate would still reference that field because `@Id` is what matters, not the field name.

Generated DDL:
```sql
CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city VARCHAR(255),
    zip_code VARCHAR(255)
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    address_id BIGINT UNIQUE,
    CONSTRAINT fk_users_address FOREIGN KEY (address_id) REFERENCES addresses(id)
);
```

The `UNIQUE` constraint on `address_id` enforces the "one-to-one" relationship — no two users can share the same address.

---

### @JoinColumn for More Control

`@JoinColumn` gives you explicit control over the foreign key column name, referenced column, nullability, and constraints.

**Properties of @JoinColumn**

| Property | Purpose | Default |
|---|---|---|
| `name` | FK column name in the parent table | `<field>_<referenced_pk>` |
| `referencedColumnName` | Which column in the child table the FK points to | The `@Id` column |
| `nullable` | Allow NULL in the FK column | `true` |
| `unique` | Add UNIQUE constraint on the FK column | `true` for `@OneToOne` |
| `insertable` | Include in INSERT statements | `true` |
| `updatable` | Include in UPDATE statements | `true` |
| `columnDefinition` | Raw DDL for the column | Provider-inferred |
| `foreignKey` | Customize the FK constraint name | Auto-generated |

**Code example with full control**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(
        name = "addr_id",                              // custom FK column name
        referencedColumnName = "id",                    // points to addresses.id
        nullable = false,                               // NOT NULL — every user MUST have an address
        unique = true,                                  // UNIQUE — one address per user
        foreignKey = @ForeignKey(name = "fk_user_addr") // custom FK constraint name
    )
    private Address address;
}
```

Generated DDL:
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    addr_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_user_addr FOREIGN KEY (addr_id) REFERENCES addresses(id)
);
```

**Without @JoinColumn vs with @JoinColumn**

```text
Without @JoinColumn:
  @OneToOne
  private Address address;
  → FK column: address_id (auto-generated name)
  → References: addresses.id (auto-detected from @Id)
  → Nullable: true
  → Constraint name: auto-generated (random-looking)

With @JoinColumn:
  @OneToOne
  @JoinColumn(name = "addr_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_addr"))
  private Address address;
  → FK column: addr_id (you chose it)
  → References: addresses.id
  → Nullable: false
  → Constraint name: fk_user_addr (you chose it)
```

---

### @JoinColumns for Composite Key References

When the child entity has a **composite primary key** (using `@IdClass`), the parent must reference **all columns** of that composite key. You use `@JoinColumns` (plural) with multiple `@JoinColumn` entries.

**Step 1: Child entity with composite key using @IdClass**

```java
// ID class
public class WarehouseLocationId implements Serializable {

    private String warehouseCode;
    private String zoneCode;

    public WarehouseLocationId() {}

    public WarehouseLocationId(String warehouseCode, String zoneCode) {
        this.warehouseCode = warehouseCode;
        this.zoneCode = zoneCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarehouseLocationId that = (WarehouseLocationId) o;
        return Objects.equals(warehouseCode, that.warehouseCode)
            && Objects.equals(zoneCode, that.zoneCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseCode, zoneCode);
    }
}
```

```java
// Child entity with composite PK
@Entity
@Table(name = "warehouse_locations")
@IdClass(WarehouseLocationId.class)
public class WarehouseLocation {

    @Id
    @Column(name = "warehouse_code")
    private String warehouseCode;           // part of composite PK

    @Id
    @Column(name = "zone_code")
    private String zoneCode;                // part of composite PK

    private String description;
    private Integer capacity;

    // constructors, getters, setters
}
```

**Step 2: Parent entity referencing the composite key**

```java
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumns({
        @JoinColumn(
            name = "wh_code",                                  // FK column in inventory_items
            referencedColumnName = "warehouse_code"             // PK column in warehouse_locations
        ),
        @JoinColumn(
            name = "wh_zone",                                  // FK column in inventory_items
            referencedColumnName = "zone_code"                  // PK column in warehouse_locations
        )
    })
    private WarehouseLocation location;

    // constructors, getters, setters
}
```

**Resulting table structure**

```text
┌─────────────────────────────────┐          ┌──────────────────────────────────┐
│  inventory_items                │          │  warehouse_locations             │
├─────────────────────────────────┤          ├──────────────────────────────────┤
│  id            BIGINT (PK)      │          │  warehouse_code VARCHAR (PK)     │
│  product_name  VARCHAR(255)     │          │  zone_code      VARCHAR (PK)     │
│  wh_code       VARCHAR (FK) ────┼──┐       │  description    VARCHAR(255)     │
│  wh_zone       VARCHAR (FK) ────┼──┼──────>│  capacity       INT              │
│                                 │  │       │                                  │
│  UNIQUE(wh_code, wh_zone)       │  │       │  PRIMARY KEY (warehouse_code,    │
│                                 │  │       │               zone_code)         │
└─────────────────────────────────┘  │       └──────────────────────────────────┘
                                     │
                          Both FK columns together
                          reference the composite PK
```

Generated DDL:
```sql
CREATE TABLE warehouse_locations (
    warehouse_code VARCHAR(255) NOT NULL,
    zone_code VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    capacity INT,
    PRIMARY KEY (warehouse_code, zone_code)
);

CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255),
    wh_code VARCHAR(255),
    wh_zone VARCHAR(255),
    CONSTRAINT fk_inventory_location
        FOREIGN KEY (wh_code, wh_zone)
        REFERENCES warehouse_locations(warehouse_code, zone_code)
);
```

**Usage**

```java
@Transactional
public void createInventory() {
    WarehouseLocation location = new WarehouseLocation();
    location.setWarehouseCode("WH-BLR-001");
    location.setZoneCode("ZONE-A");
    location.setDescription("Electronics Zone");
    location.setCapacity(500);

    InventoryItem item = new InventoryItem();
    item.setProductName("Laptop");
    item.setLocation(location);

    inventoryItemRepository.save(item);
    // INSERT INTO warehouse_locations (warehouse_code, zone_code, description, capacity)
    //   VALUES ('WH-BLR-001', 'ZONE-A', 'Electronics Zone', 500);
    // INSERT INTO inventory_items (product_name, wh_code, wh_zone)
    //   VALUES ('Laptop', 'WH-BLR-001', 'ZONE-A');
}
```

---

### Eager and Lazy Loading

**Does the child entity always load with the parent?**

No — it depends on the **fetch type**. The `@OneToOne` annotation has a `fetch` attribute that controls when the child is loaded.

```java
@OneToOne(fetch = FetchType.EAGER)   // child loaded immediately WITH parent (DEFAULT for @OneToOne)
@OneToOne(fetch = FetchType.LAZY)    // child loaded ONLY when accessed
```

---

### What is Eager Loading

With eager loading, when you load the parent entity, Hibernate **immediately** loads the child entity too, in the **same SQL query** (using a JOIN).

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)   // EAGER = default for @OneToOne
    @JoinColumn(name = "address_id")
    private Address address;
}
```

```java
// Load user
User user = userRepository.findById(1L).orElseThrow();
// At this point, user.address is ALREADY loaded — no extra query
System.out.println(user.getAddress().getCity());   // works, no additional SQL
```

```text
SQL generated (single query with LEFT JOIN):

SELECT u.id, u.name, u.address_id,
       a.id, a.city, a.zip_code
FROM users u
LEFT JOIN addresses a ON u.address_id = a.id
WHERE u.id = 1;
```

---

### What is Lazy Loading

With lazy loading, when you load the parent entity, Hibernate does **NOT** load the child entity. It creates a **proxy object** instead. The real data is fetched from the database **only when you access** the child's fields.

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;       // not loaded until accessed
}
```

```java
@Transactional
public void lazyLoadingExample() {
    // SQL: SELECT u.id, u.name, u.address_id FROM users u WHERE u.id = 1
    // (NO JOIN — address is NOT loaded)
    User user = userRepository.findById(1L).orElseThrow();

    // user.address is a Hibernate PROXY at this point (not real Address)
    // Accessing a field on the proxy triggers the second query:
    // SQL: SELECT a.id, a.city, a.zip_code FROM addresses a WHERE a.id = 1
    String city = user.getAddress().getCity();
}
```

```text
Eager loading (1 query):
  SELECT u.*, a.* FROM users u LEFT JOIN addresses a ON u.address_id = a.id WHERE u.id = 1;

Lazy loading (2 queries, second only if accessed):
  Query 1: SELECT u.id, u.name, u.address_id FROM users u WHERE u.id = 1;
  Query 2: SELECT a.id, a.city, a.zip_code FROM addresses a WHERE a.id = 1;   ← only when getAddress().getCity() called
```

---

### Difference Between Eager and Lazy Loading

| | Eager (FetchType.EAGER) | Lazy (FetchType.LAZY) |
|---|---|---|
| **When child loads** | Immediately with parent (same query) | Only when child field is accessed |
| **SQL generated** | JOIN query (1 query) | Separate SELECT for child (2 queries) |
| **Memory usage** | Higher — always loads full graph | Lower — loads only what you need |
| **Performance risk** | Loads unnecessary data, especially for large graphs | N+1 problem if iterating over parent list |
| **Proxy** | No proxy — real object | Hibernate proxy until accessed |
| **Outside transaction** | Always works (data already loaded) | Fails outside transaction (`LazyInitializationException`) |

**Advantages of Eager**

- Simple — child data always available, no extra queries.
- No `LazyInitializationException`.
- Good when you **always** need the child with the parent.

**Disadvantages of Eager**

- Loads child even when you only need the parent.
- If entity graph is deep (User → Address → Country → Continent), all levels load eagerly.
- Wastes memory and DB resources for unused data.

**Advantages of Lazy**

- Only loads data you actually use.
- Better performance for large or complex entity graphs.
- Reduces memory footprint.

**Disadvantages of Lazy**

- Requires an open persistence context (active transaction) to access the child.
- `LazyInitializationException` if accessed outside a transaction.
- N+1 query problem when iterating over a list of parents.

**Use cases**

| Use case | Recommended fetch type |
|---|---|
| User → Profile (almost always needed together) | EAGER |
| User → Address (needed on user detail page only) | LAZY |
| Order → OrderItems (large collection) | LAZY |
| Product → Category (small lookup, always displayed) | EAGER |

---

### Why Eager is Default for @OneToOne and Lazy for @ManyToMany

```text
@OneToOne   → default: FetchType.EAGER
@ManyToOne  → default: FetchType.EAGER
@OneToMany  → default: FetchType.LAZY
@ManyToMany → default: FetchType.LAZY
```

**Why @OneToOne defaults to EAGER**

- `@OneToOne` loads **exactly one** related entity — very low cost.
- In most cases, if you load a User, you also need its Address or Profile.
- One extra JOIN adds negligible overhead.

**Why @ManyToMany defaults to LAZY**

- `@ManyToMany` can load **hundreds or thousands** of related entities.
- Loading all students for a course, or all courses for a student, eagerly would cause massive data transfer.
- Lazy loading prevents this by only fetching the collection when explicitly accessed.

**Can we override it?**

Yes — you override the default by specifying `fetch` explicitly:

```java
// Override @OneToOne EAGER default → make it LAZY
@OneToOne(fetch = FetchType.LAZY)
private Address address;

// Override @ManyToMany LAZY default → make it EAGER (not recommended)
@ManyToMany(fetch = FetchType.EAGER)
private Set<Course> courses;
```

---

### Why Hibernate Uses LEFT JOIN for @OneToOne

Hibernate generates a **LEFT JOIN** (not INNER JOIN) to load the parent and child together.

```text
SELECT u.id, u.name, u.address_id,
       a.id, a.city, a.zip_code
FROM users u
LEFT JOIN addresses a ON u.address_id = a.id
WHERE u.id = 1;
```

**Why LEFT JOIN instead of INNER JOIN?**

- A `LEFT JOIN` ensures the parent row is **always returned**, even if the FK column is `NULL` (no child exists).
- If the `address_id` is `NULL` in the `users` table, an `INNER JOIN` would return **zero rows**, and you could not load the User at all.
- Since JPA allows `@OneToOne` to be optional (nullable FK), LEFT JOIN is the safe choice.

```text
INNER JOIN:
  User(id=1, address_id=NULL)  →  NOT returned (no matching address row)
  User lost!

LEFT JOIN:
  User(id=1, address_id=NULL)  →  Returned with address columns as NULL
  User preserved, address = null in Java
```

**What LEFT JOIN does in eager vs lazy loading**

```text
Eager loading (FetchType.EAGER):
  SELECT u.*, a.* FROM users u LEFT JOIN addresses a ON u.address_id = a.id WHERE u.id = ?
  → Parent AND child data in ONE query. If address_id is NULL, address columns are NULL → user.address = null.

Lazy loading (FetchType.LAZY):
  Query 1: SELECT u.id, u.name, u.address_id FROM users u WHERE u.id = ?
  → Only parent loaded. No JOIN at all.

  Query 2 (triggered on access): SELECT a.* FROM addresses a WHERE a.id = ?
  → Simple SELECT by PK. No LEFT JOIN needed because the FK value is already known.
```

If you make the FK `NOT NULL` (`@JoinColumn(nullable = false)`), Hibernate knows the child always exists and **may** optimize to use `INNER JOIN` instead of `LEFT JOIN`:

```java
@OneToOne(fetch = FetchType.EAGER, optional = false)
@JoinColumn(name = "address_id", nullable = false)
private Address address;
// Hibernate may use INNER JOIN since address is guaranteed to exist
```

---

### Lazy Loading and Jackson Serialization Exception

When you use lazy loading and return the **entity directly** as a REST response (not a DTO), Jackson encounters the Hibernate **proxy** object and fails to serialize it.

**The problem**

```java
@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElseThrow();
        // Returns User entity directly as JSON
    }
}
```

```java
@Entity
public class User {
    @Id
    private Long id;
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;         // Hibernate proxy — not real Address
}
```

```text
What happens:

1. @Transactional (in repository) loads User from DB
2. Transaction commits → persistence context closes → EntityManager closed
3. user.address is still a Hibernate PROXY (not initialized)
4. Controller returns the User object
5. Jackson (HttpMessageConverter) tries to serialize the User to JSON
6. Jackson calls user.getAddress() → triggers proxy initialization
7. But the persistence context is CLOSED → no active Session
8. EXCEPTION:

   com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
   No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor
   
   OR
   
   org.hibernate.LazyInitializationException:
   could not initialize proxy — no Session
```

```text
Timeline:

 @Transactional        Transaction ends       Jackson serializes
      │                     │                       │
  findById()            PC closed              user.getAddress()
  user loaded          address is              proxy init fails!
  address = proxy      still a proxy           → EXCEPTION
      │                     │                       │
  EM open ──────────── EM closed ──────────── no EM available
```

---

**How @JsonIgnore helps (and its limitation)**

`@JsonIgnore` tells Jackson to **skip** the field entirely during serialization.

```java
@Entity
public class User {
    @Id
    private Long id;
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    @JsonIgnore                           // Jackson will skip this field
    private Address address;
}
```

```text
Response JSON:
{
    "id": 1,
    "name": "Alice"
    // address is completely absent — @JsonIgnore skipped it
}
```

**The limitation**: `@JsonIgnore` **always** ignores the field — even when the address IS loaded (e.g., you explicitly fetched it via a JOIN query or accessed it within the transaction). You can never include the address in the response.

```java
@Transactional
public User getUserWithAddress(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    user.getAddress().getCity();   // force initialization inside transaction
    return user;
}
// Even though address IS loaded, @JsonIgnore still excludes it from JSON!
```

---

**Why DTO is the Right Solution (and Recommended)**

A **DTO (Data Transfer Object)** decouples the API response from the JPA entity. You control exactly what data is included.

```java
public class UserDTO {
    private Long id;
    private String name;
    private String city;        // flat field from Address, only what the API needs
    private String zipCode;

    // constructors, getters, setters
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow();

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());

        if (user.getAddress() != null) {
            dto.setCity(user.getAddress().getCity());       // accessed inside transaction → works
            dto.setZipCode(user.getAddress().getZipCode());
        }

        return dto;   // safe to serialize — plain POJO, no proxy
    }
}
```

```java
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/users/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.getUserById(id);   // returns DTO, not entity
    }
}
```

**Why DTO is recommended over returning Entity**

| Concern | Returning Entity | Returning DTO |
|---|---|---|
| Lazy loading exceptions | Risk of `LazyInitializationException` | No proxy — plain POJO |
| API response shape | Coupled to entity structure | Decoupled — API can change independently |
| Security | May expose sensitive fields (password, internal IDs) | Only expose what you choose |
| Hibernate proxy in JSON | `ByteBuddyInterceptor` errors | No proxy — clean serialization |
| Versioning | Entity changes break API | DTO versioned separately |
| Circular references | Infinite recursion (bidirectional) | No cycles — flat structure |

---

### When Does Lazy Loading Actually Fetch the Child

The child entity is fetched **when you access any field** on the proxy object, and the persistence context must still be open.

```java
@Transactional
public void lazyLoadTiming() {
    User user = userRepository.findById(1L).orElseThrow();
    // SQL: SELECT u.id, u.name, u.address_id FROM users u WHERE u.id = 1
    // user.address = Hibernate PROXY (not initialized)

    // This DOES NOT trigger loading — just returns the proxy reference:
    Address addressProxy = user.getAddress();

    // This TRIGGERS the actual SQL:
    String city = addressProxy.getCity();
    // SQL: SELECT a.id, a.city, a.zip_code FROM addresses a WHERE a.id = 1
    // Now the proxy is "initialized" and has real data

    // Subsequent calls do NOT trigger SQL — proxy is already initialized:
    String zip = addressProxy.getZipCode();   // no SQL
}
```

```text
Access sequence:

user.getAddress()              → returns PROXY reference (no SQL)
user.getAddress().getCity()    → TRIGGERS SQL (proxy initialized)
user.getAddress().getZipCode() → uses initialized proxy (no SQL)
user.getAddress().getId()      → may NOT trigger SQL (ID is in the FK column, already known)
```

**Key rules**:
- Accessing `getAddress()` alone returns the proxy — no SQL.
- Accessing any **non-ID field** on the proxy (like `getCity()`) triggers the query.
- Accessing `getId()` on the proxy may **not** trigger a query because the ID is already stored in the parent's FK column.
- Once initialized, subsequent field accesses use the loaded data — no repeated queries.

---

### Why No Exception When Inserting Parent with Child via Lazy Loading

When you **save** (insert) a parent with its child, even with lazy loading, there is no `LazyInitializationException`. Here is why:

```java
@Transactional
public User createUserWithAddress() {
    Address address = new Address();
    address.setCity("Bangalore");
    address.setZipCode("560001");

    User user = new User();
    user.setName("Alice");
    user.setAddress(address);       // setting real Address object (not a proxy)

    return userRepository.save(user);
    // Works fine — no lazy loading exception
}
```

**Why it works**

```text
During INSERT:

1. user.address = actual Address object (you created it with "new")
   It is NOT a Hibernate proxy — it is a real object you built in Java.

2. The persistence context is OPEN (inside @Transactional).

3. CascadeType.ALL → Hibernate persists Address first, gets its ID,
   then persists User with the FK pointing to Address.

4. No proxy involved, no lazy loading triggered.
   The Address object is already in memory — you just created it!
```

```text
During SELECT with lazy loading (where the problem occurs):

1. Hibernate loads User from DB.
2. Hibernate creates a PROXY for Address (does NOT load it).
3. Transaction ends → persistence context closes.
4. Later, you access user.getAddress().getCity().
5. Proxy tries to initialize → NO persistence context → EXCEPTION.
```

**Does the persistence context help?**

Yes — the persistence context is critical. As long as it is open, lazy proxy initialization works:

```text
@Transactional method:
  ├─ Persistence context OPEN
  ├─ Load user (lazy address = proxy)
  ├─ user.getAddress().getCity()     ← proxy initializes → SQL executes → WORKS
  └─ Persistence context CLOSES

Outside @Transactional:
  ├─ Persistence context CLOSED
  ├─ user.getAddress().getCity()     ← proxy tries to initialize → FAILS
  └─ LazyInitializationException
```

During **insert**, you are always inside a `@Transactional` method, and you are passing real objects (not proxies). During **read**, if you exit the transactional boundary before accessing the lazy proxy, you get the exception.

---

### OneToOne Bidirectional Mapping

In a bidirectional `@OneToOne` mapping, **both** entities have a reference to each other. The parent knows the child, and the child knows the parent.

```text
Java Objects:                              Database Tables:

┌───────────────────┐                      ┌───────────────────────────┐
│  User             │                      │  users                    │
│                   │                      ├───────────────────────────┤
│  id: Long         │    @OneToOne         │  id (PK, BIGINT)          │
│  name: String     ├───────────────>      │  name (VARCHAR)           │
│  address: Address │                      │  address_id (FK, BIGINT)  │──┐
│                   │<───────────────┐     │                           │  │
└───────────────────┘                │     └───────────────────────────┘  │
                                     │                                    │
┌───────────────────┐                │     ┌───────────────────────────┐  │
│  Address          │                │     │  addresses                │  │
│                   │  @OneToOne     │     ├───────────────────────────┤  │
│  id: Long         │  (mappedBy)   │     │  id (PK, BIGINT)          │<─┘
│  city: String     ├───────────────┘     │  city (VARCHAR)           │
│  zipCode: String  │                      │  zip_code (VARCHAR)       │
│  user: User       │  ← back reference   │                           │
│                   │                      │  (NO FK column here!)     │
└───────────────────┘                      └───────────────────────────┘

Direction: User <────────> Address   (both ways)
           User knows Address.   Address also knows User.
```

---

### Difference from Unidirectional

| | Unidirectional | Bidirectional |
|---|---|---|
| **Parent (User)** | Has `@OneToOne Address address` | Has `@OneToOne Address address` |
| **Child (Address)** | No reference to User | Has `@OneToOne(mappedBy="address") User user` |
| **Navigation** | User → Address only | User → Address AND Address → User |
| **Table structure** | Same — FK in users table | **Same** — FK in users table only |
| **Extra FK in child table?** | No | **No** — `mappedBy` prevents it |
| **Java code difference** | Address has no User field | Address has a User field with `mappedBy` |

**Critical point**: Adding bidirectional mapping **does NOT change the table structure**. The foreign key still exists only in the **owner** table (users). The `mappedBy` side (Address) does not add a foreign key column.

```text
Unidirectional tables:                    Bidirectional tables:
┌──────────┐    ┌──────────┐             ┌──────────┐    ┌──────────┐
│ users    │    │ addresses│             │ users    │    │ addresses│
│ id (PK)  │    │ id (PK)  │             │ id (PK)  │    │ id (PK)  │
│ name     │    │ city     │             │ name     │    │ city     │
│ addr_id──┼───>│ zip_code │             │ addr_id──┼───>│ zip_code │
└──────────┘    └──────────┘             └──────────┘    └──────────┘

IDENTICAL table structure!  The difference is only in Java — Address now has a User field.
```

---

### Owner Side and Inverse Side

In a bidirectional relationship, JPA requires you to designate:

- **Owner side**: The entity whose table holds the **foreign key column**. This side controls the relationship.
- **Inverse side**: The entity that mirrors the relationship using `mappedBy`. This side does NOT have a FK column.

```text
┌──────────────────────┐                    ┌──────────────────────────────┐
│  User (OWNER)        │                    │  Address (INVERSE)           │
│                      │                    │                              │
│  @OneToOne           │                    │  @OneToOne(mappedBy="address")│
│  @JoinColumn(...)    │                    │  private User user;          │
│  private Address     │                    │                              │
│     address;         │                    │  (no @JoinColumn here)       │
│                      │                    │  (no FK in addresses table)  │
│  → FK: address_id    │                    │                              │
│    in users table    │                    │  "I'm just the mirror"       │
└──────────────────────┘                    └──────────────────────────────┘
```

**Who is the owner?**

The entity that has `@JoinColumn` (the FK column) is the owner. The entity that has `mappedBy` is the inverse.

**Code example**

```java
// OWNER SIDE — User holds the FK
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")   // FK here
    private Address address;

    // constructors, getters, setters
}
```

```java
// INVERSE SIDE — Address mirrors the relationship
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @OneToOne(mappedBy = "address")      // "address" = field name in User class
    private User user;                    // back reference to User

    // constructors, getters, setters
}
```

---

### Why We Need `mappedBy`

Without `mappedBy`, JPA treats both sides as **independent** owner relationships and creates **two FK columns** — one in each table.

```text
WITHOUT mappedBy (WRONG):

@Entity User:
    @OneToOne @JoinColumn(name = "address_id")
    private Address address;

@Entity Address:
    @OneToOne @JoinColumn(name = "user_id")
    private User user;

Result — TWO foreign keys:
┌──────────────────┐         ┌─────────────────────┐
│ users            │         │ addresses            │
│ id (PK)          │         │ id (PK)              │
│ name             │         │ city                 │
│ address_id (FK) ─┼────────>│ zip_code             │
│                  │<────────┼─ user_id (FK)        │
└──────────────────┘         └─────────────────────┘

Two FK columns = data duplication, sync issues, potential inconsistency.
```

```text
WITH mappedBy (CORRECT):

@Entity User:
    @OneToOne @JoinColumn(name = "address_id")
    private Address address;

@Entity Address:
    @OneToOne(mappedBy = "address")       // points to User.address field
    private User user;

Result — ONE foreign key only:
┌──────────────────┐         ┌─────────────────────┐
│ users            │         │ addresses            │
│ id (PK)          │         │ id (PK)              │
│ name             │         │ city                 │
│ address_id (FK) ─┼────────>│ zip_code             │
│                  │         │                      │
└──────────────────┘         └─────────────────────┘

One FK = clean, no duplication, single source of truth.
```

`mappedBy = "address"` tells Hibernate: "Don't create a FK here. The relationship is already managed by the `address` field in the `User` entity. I'm just a mirror."

---

### Real-World Example — Getting User from Address

**Use case**: User → Address is the primary mapping. But sometimes you want to know which User lives at a given Address — that requires Address → User navigation.

```java
// OWNER
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address address;

    // getters, setters
}
```

```java
// INVERSE
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @OneToOne(mappedBy = "address")
    private User user;              // back reference

    // getters, setters
}
```

**Navigate both directions**

```java
@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public void findUserByAddress(Long addressId) {
        Address address = addressRepository.findById(addressId).orElseThrow();

        // Navigate from Address → User (bidirectional)
        User user = address.getUser();
        System.out.println("User at this address: " + user.getName());
    }
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public void findAddressByUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        // Navigate from User → Address (standard direction)
        Address address = user.getAddress();
        System.out.println("User lives at: " + address.getCity());
    }
}
```

**SQL for Address → User navigation**

```text
addressRepository.findById(1L)
  → SELECT a.* FROM addresses a WHERE a.id = 1
  → Then (eager or lazy): SELECT u.* FROM users u WHERE u.address_id = 1
```

---

### Infinite Recursion Problem in Bidirectional Mapping

When you return a bidirectional entity directly as a REST response, Jackson serializes User → Address → User → Address → ... infinitely.

```text
Jackson serializes User:
{
  "id": 1,
  "name": "Alice",
  "address": {                        ← serializes Address
    "id": 10,
    "city": "Bangalore",
    "user": {                          ← serializes User AGAIN
      "id": 1,
      "name": "Alice",
      "address": {                     ← serializes Address AGAIN
        "id": 10,
        "city": "Bangalore",
        "user": {                      ← INFINITE LOOP
          ...
        }
      }
    }
  }
}

Result: StackOverflowError or HttpMessageNotWritableException
```

```text
The recursion cycle:

User.address → Address.user → User.address → Address.user → ...
     │              │              │              │
     └──────────────┘              └──────────────┘
         cycle 1                       cycle 2     → StackOverflow
```

---

### Solution 1: @JsonManagedReference and @JsonBackReference

This pair tells Jackson which side to serialize (managed = forward) and which to skip (back = ignored during serialization).

```text
@JsonManagedReference  →  "Serialize this side" (included in JSON)
@JsonBackReference     →  "Do NOT serialize this side" (excluded from JSON)
```

**Code example**

```java
// OWNER — User (forward reference, serialized)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    @JsonManagedReference                    // ← serialize this side
    private Address address;
}
```

```java
// INVERSE — Address (back reference, NOT serialized)
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @OneToOne(mappedBy = "address")
    @JsonBackReference                       // ← do NOT serialize this side
    private User user;
}
```

**JSON response when fetching User**

```json
{
    "id": 1,
    "name": "Alice",
    "address": {
        "id": 10,
        "city": "Bangalore",
        "zipCode": "560001"
    }
}
```

Address is included in the User response, but `user` field inside Address is **skipped** → no infinite recursion.

**JSON response when fetching Address directly**

```json
{
    "id": 10,
    "city": "Bangalore",
    "zipCode": "560001"
}
```

The `user` field is **not included** because `@JsonBackReference` always omits it. You **cannot** see User from the Address response.

**Where to put which annotation and why**

| Annotation | Put on | Why |
|---|---|---|
| `@JsonManagedReference` | Parent side (User.address) | This is the "forward" direction you typically serialize |
| `@JsonBackReference` | Child side (Address.user) | This is the "back" direction that causes the recursion |

**Limitation**: `@JsonBackReference` **always** hides the annotated field. When you fetch Address, you can never see its User in JSON. If you need both directions visible in the response, use `@JsonIdentityInfo`.

---

### Solution 2: @JsonIdentityInfo

`@JsonIdentityInfo` handles recursion by serializing the **full object the first time** and only its **ID** on subsequent references. This way, both directions are visible.

```java
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@Entity
@Table(name = "users")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"                          // use the @Id field as the identity
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address address;                 // no @JsonManagedReference needed
}
```

```java
@Entity
@Table(name = "addresses")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"                          // use the @Id field as the identity
)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @OneToOne(mappedBy = "address")
    private User user;                       // no @JsonBackReference needed
}
```

**JSON response when fetching User**

```json
{
    "id": 1,
    "name": "Alice",
    "address": {
        "id": 10,
        "city": "Bangalore",
        "zipCode": "560001",
        "user": 1
    }
}
```

When Jackson encounters User for the second time (inside Address.user), it replaces the full object with just its `id` value (`1`). No infinite recursion, and **both directions are visible**.

**JSON response when fetching Address**

```json
{
    "id": 10,
    "city": "Bangalore",
    "zipCode": "560001",
    "user": {
        "id": 1,
        "name": "Alice",
        "address": 10
    }
}
```

When Jackson encounters Address for the second time (inside User.address), it replaces it with just `10`.

**How @JsonIdentityInfo works**

```text
First encounter of User(id=1):
  → Serialize full object: { "id": 1, "name": "Alice", "address": {...} }
  → Register: User#1 = already serialized

Inside Address, encounter User(id=1) again:
  → Already serialized! Output just the id: 1
  → No recursion!
```

**Why use it on the @Id field**

The `@Id` field is unique for every entity instance. Using it as the identity marker ensures no collisions. `ObjectIdGenerators.PropertyGenerator.class` tells Jackson "use an existing property of the object (id) as the identity, don't generate a synthetic one."

---

### When to Use Which Solution

| Solution | Behavior | Use when |
|---|---|---|
| `@JsonManagedReference` + `@JsonBackReference` | Forward side serialized, back side always hidden | You only need one direction in the response (most common) |
| `@JsonIdentityInfo` | Both sides serialized, second occurrence replaced with ID | You need both directions visible, API consumers can resolve IDs |
| DTO (recommended) | Full control, no annotations on entity | Production APIs — cleanest, most flexible, no entity leakage |

**Summary of all approaches**

```text
Problem: User → Address → User → Address → ... (infinite recursion)

Solution 1: @JsonManagedReference + @JsonBackReference
  User → Address ✅ (full object)
  Address → User ❌ (always hidden)

Solution 2: @JsonIdentityInfo
  User → Address ✅ (full object first time, ID on repeat)
  Address → User ✅ (full object first time, ID on repeat)

Solution 3: DTO (best practice)
  UserDTO → AddressDTO ✅ (exactly what you want)
  AddressDTO → UserDTO ✅ (if you include it)
  No recursion, no proxies, no Jackson annotations on entities
```




### OneToMany Unidirectional Mapping

In a `@OneToMany` unidirectional mapping, one parent entity is associated with **multiple** child entities. The reference exists only in the parent — the child has no knowledge of the parent.

**Real-life use case**: A `User` can place many `Order`s. The User entity has a list of Orders. The Order entity does NOT have a reference back to User.

```text
Java Objects:                              

┌───────────────────────┐                  
│  User                 │                  
│                       │                  
│  id: Long             │    @OneToMany    
│  name: String         │                  
│  orders: List<Order>  ├─────────────────> ┌────────────────────┐
│                       │                   │  Order             │
│                       │                   │                    │
└───────────────────────┘                   │  id: Long          │
                                            │  product: String   │
           1 User  ───────>  N Orders       │  amount: BigDecimal│
           (parent)          (children)     │                    │
                                            │  (NO ref to User)  │
                                            └────────────────────┘

Direction: User ──────> Orders   (one way only)
           User knows its Orders.   Order does NOT know its User.
```

**Code example**

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private BigDecimal amount;

    // NO reference to User — this is unidirectional
    // constructors, getters, setters
}
```

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();

    // constructors, getters, setters
}
```

**Usage**

```java
@Transactional
public User createUserWithOrders() {
    Order order1 = new Order();
    order1.setProduct("Laptop");
    order1.setAmount(new BigDecimal("999.99"));

    Order order2 = new Order();
    order2.setProduct("Mouse");
    order2.setAmount(new BigDecimal("29.99"));

    User user = new User();
    user.setName("Alice");
    user.getOrders().add(order1);
    user.getOrders().add(order2);

    return userRepository.save(user);
}
```

---

### How OneToMany Unidirectional is Stored in DB — The Join Table

By default, when you use `@OneToMany` **without** `@JoinColumn`, Hibernate creates a **third join table** to link the parent and child tables. This is because in a unidirectional `@OneToMany`, the child table has no FK column to the parent — so Hibernate needs a separate table to hold the relationship.

```text
DEFAULT behavior (@OneToMany without @JoinColumn):

┌──────────────┐        ┌────────────────────────┐        ┌──────────────────────┐
│ users         │        │ users_orders            │        │ orders               │
├──────────────┤        │ (AUTO-GENERATED         │        ├──────────────────────┤
│ id (PK)      │───────>│  JOIN TABLE)            │<───────│ id (PK)              │
│ name         │        ├────────────────────────┤        │ product              │
│              │        │ user_id (FK) ──> users  │        │ amount               │
│              │        │ orders_id (FK) ──> orders│        │                      │
│              │        │                        │        │ (NO user_id here!)    │
└──────────────┘        └────────────────────────┘        └──────────────────────┘
```

**Generated DDL (3 tables)**

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product VARCHAR(255),
    amount DECIMAL(19,2)
);

-- Hibernate auto-creates this join table:
CREATE TABLE users_orders (
    user_id BIGINT NOT NULL,
    orders_id BIGINT NOT NULL UNIQUE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (orders_id) REFERENCES orders(id)
);
```

**SQL generated when saving User with Orders**

```text
INSERT INTO orders (product, amount) VALUES ('Laptop', 999.99);          ← Order 1 saved
INSERT INTO orders (product, amount) VALUES ('Mouse', 29.99);            ← Order 2 saved
INSERT INTO users (name) VALUES ('Alice');                                ← User saved
INSERT INTO users_orders (user_id, orders_id) VALUES (1, 1);             ← link User to Order 1
INSERT INTO users_orders (user_id, orders_id) VALUES (1, 2);             ← link User to Order 2
```

**5 SQL statements** for a simple save — this is **inefficient**.

```text
Why does Hibernate create a join table by default?

1. @OneToMany is on the PARENT (User).
2. The CHILD (Order) has NO reference to User and NO FK column.
3. Hibernate cannot add a FK to Order because Order doesn't "know" about User.
4. So Hibernate creates a middle table to store the relationship.
```

---

### @JoinColumn — Avoiding the Join Table (Industry Standard)

By adding `@JoinColumn` to the `@OneToMany`, you tell Hibernate: **"Put the foreign key column directly in the child table. Do NOT create a join table."**

```text
WITH @JoinColumn (@OneToMany + @JoinColumn):

┌──────────────┐                              ┌──────────────────────┐
│ users         │                              │ orders               │
├──────────────┤                              ├──────────────────────┤
│ id (PK)      │──────────────────────────────>│ id (PK)              │
│ name         │                              │ product              │
│              │                              │ amount               │
│              │                              │ user_id (FK)  ←──────│── FK added to child table
│              │                              │                      │
└──────────────┘                              └──────────────────────┘

NO join table!   FK lives in the CHILD table (orders).
Only 2 tables.
```

**Code example**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")                // FK "user_id" goes into the orders table
    private List<Order> orders = new ArrayList<>();
}
```

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private BigDecimal amount;

    // Still NO reference to User — still unidirectional
    // The "user_id" column is managed by Hibernate, not by Order entity
}
```

**Generated DDL (2 tables only)**

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product VARCHAR(255),
    amount DECIMAL(19,2),
    user_id BIGINT,                                -- FK added by @JoinColumn
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**SQL generated when saving**

```text
INSERT INTO users (name) VALUES ('Alice');                                       ← User saved first
INSERT INTO orders (product, amount) VALUES ('Laptop', 999.99);                  ← Order 1 saved (user_id is NULL initially)
INSERT INTO orders (product, amount) VALUES ('Mouse', 29.99);                    ← Order 2 saved (user_id is NULL initially)
UPDATE orders SET user_id = 1 WHERE id = 1;                                      ← FK set via UPDATE
UPDATE orders SET user_id = 1 WHERE id = 2;                                      ← FK set via UPDATE
```

Note: Hibernate inserts the child rows with `user_id = NULL` first, then issues **UPDATE** statements to set the FK. This is because in unidirectional `@OneToMany`, the child entity does not know about the FK — the parent manages it.

**Is @JoinColumn the industry standard?**

Yes. Using `@JoinColumn` with `@OneToMany` is the **standard approach** in production code.

```text
Without @JoinColumn:
  - 3 tables (extra join table)
  - 5+ SQL statements for a simple save
  - Extra JOINs needed for every query
  - Harder to understand schema

With @JoinColumn:
  - 2 tables (clean, normalized schema)
  - Fewer SQL statements
  - Standard FK relationship
  - Simpler queries
```

**Will it make queries faster?**

```text
Without @JoinColumn (join table):
  SELECT u.*, o.*
  FROM users u
  JOIN users_orders uo ON u.id = uo.user_id       ← extra join through middle table
  JOIN orders o ON uo.orders_id = o.id
  WHERE u.id = 1;

With @JoinColumn (direct FK):
  SELECT u.*, o.*
  FROM users u
  LEFT JOIN orders o ON u.id = o.user_id           ← direct join, one less table
  WHERE u.id = 1;

Direct FK = one fewer JOIN = faster queries, simpler execution plan.
```

---

### Lazy Loading in OneToMany (Default)

`@OneToMany` defaults to `FetchType.LAZY`. The child collection is **not loaded** when the parent is fetched. It is loaded only when you **access** the collection.

**Entity**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)   // LAZY is default for @OneToMany
    @JoinColumn(name = "user_id")
    private List<Order> orders = new ArrayList<>();
}
```

**Controller + Service**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        // SQL #1: SELECT u.id, u.name FROM users u WHERE u.id = 1
        // At this point, user.orders is a Hibernate PersistentBag PROXY (not loaded)

        List<OrderDTO> orderDTOs = user.getOrders().stream()     // ← TRIGGERS SQL #2
            .map(o -> new OrderDTO(o.getId(), o.getProduct(), o.getAmount()))
            .toList();
        // SQL #2: SELECT o.id, o.product, o.amount, o.user_id FROM orders o WHERE o.user_id = 1
        // Orders loaded NOW because we accessed the collection

        return new UserDTO(user.getId(), user.getName(), orderDTOs);
    }
}
```

```text
SQL generated (lazy loading):

Query 1 (load parent):
  SELECT u.id, u.name FROM users u WHERE u.id = 1;

Query 2 (load children — triggered by user.getOrders()):
  SELECT o.id, o.product, o.amount, o.user_id
  FROM orders o
  WHERE o.user_id = 1;

Total: 2 queries (second one only if you access the orders)
```

```text
Timeline:

findById(1L)                    user.getOrders().stream()
     │                                   │
     v                                   v
SELECT users ...              SELECT orders WHERE user_id = 1
(orders = PROXY)              (orders loaded into real list)
     │                                   │
     └───── orders NOT loaded ───────────┘
                                    loaded here
```

---

### Returning Entity Directly — Jackson Exception

If you return the **entity** directly as a REST response with lazy loading, you **will** get a Jackson serialization exception for `@OneToMany`:

```java
// DANGEROUS — returning entity directly
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
    // Transaction ends here (if no @Transactional on controller)
    // user.orders = Hibernate PersistentBag proxy (not initialized)
    // Jackson tries to serialize user.orders → EXCEPTION
}
```

```text
What happens:

1. Repository loads User inside its own transaction
2. Transaction commits → persistence context closes
3. user.orders = PersistentBag proxy (NOT initialized)
4. Jackson tries to call user.getOrders() to serialize the list
5. Proxy tries to initialize → NO active Session
6. EXCEPTION:

   com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
   No serializer found for class org.hibernate.collection.spi.PersistentBag

   OR

   org.hibernate.LazyInitializationException:
   failed to lazily initialize a collection of role: User.orders — could not initialize proxy — no Session
```

**Will the child entities be loaded automatically?** No. Unlike `@OneToOne` with eager default, `@OneToMany` defaults to LAZY. Jackson will NOT trigger the load — it will fail because the session is closed.

**With OSIV (Open Session In View) enabled** (`spring.jpa.open-in-view=true`, the default): Jackson CAN trigger the lazy load because the EntityManager is still open during view rendering. But this is considered a **bad practice** because:
- It hides the N+1 problem.
- It couples the view layer to the persistence layer.
- It keeps the database connection open longer than necessary.

**The right solution is to use DTOs** (as shown in the lazy loading example above).

---

### Eager Loading in OneToMany

With eager loading, Hibernate loads the parent **and all children** in a single JOIN query when the parent is fetched.

**Entity**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)   // EAGER — load children immediately
    @JoinColumn(name = "user_id")
    private List<Order> orders = new ArrayList<>();
}
```

**Controller + Service**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        // SQL: SELECT u.*, o.* FROM users u LEFT JOIN orders o ON u.id = o.user_id WHERE u.id = 1
        // Orders are ALREADY loaded — no proxy, real list

        List<OrderDTO> orderDTOs = user.getOrders().stream()     // NO extra SQL — already loaded
            .map(o -> new OrderDTO(o.getId(), o.getProduct(), o.getAmount()))
            .toList();

        return new UserDTO(user.getId(), user.getName(), orderDTOs);
    }
}
```

```text
SQL generated (eager loading — single query):

SELECT u.id, u.name,
       o.id, o.product, o.amount, o.user_id
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.id = 1;

Total: 1 query (all data loaded at once)
```

```text
Timeline:

findById(1L)
     │
     v
SELECT users LEFT JOIN orders ...
(user + ALL orders loaded in one shot)
     │
     └───── orders are REAL objects, not proxies
            no second query needed
```

**Why EAGER is dangerous for @OneToMany**

```text
Scenario: findAll() with EAGER @OneToMany

userRepository.findAll();
→ SELECT * FROM users;                                    ← loads 100 users
→ For EACH user: SELECT * FROM orders WHERE user_id = ?   ← 100 more queries!

Total: 101 queries (N+1 problem)
This is why @OneToMany defaults to LAZY.
```

Eager loading is only safe for `@OneToMany` when:
- You always need the children with the parent.
- The collection is small and bounded.
- You are loading a single parent entity (not a list).

---

### Cascade Types in OneToMany Unidirectional

**CascadeType.PERSIST — Saving parent auto-saves children**

```java
@OneToMany(cascade = CascadeType.PERSIST)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public User createUserWithOrders() {
    Order order1 = new Order("Laptop", new BigDecimal("999.99"));
    Order order2 = new Order("Mouse", new BigDecimal("29.99"));

    User user = new User("Alice");
    user.getOrders().add(order1);
    user.getOrders().add(order2);

    return userRepository.save(user);   // saves User AND both Orders
}
```

```text
SQL:
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount) VALUES ('Laptop', 999.99);
  INSERT INTO orders (product, amount) VALUES ('Mouse', 29.99);
  UPDATE orders SET user_id = 1 WHERE id = 1;
  UPDATE orders SET user_id = 1 WHERE id = 2;
```

```text
persist(user)
     │
     ├─ INSERT User
     ├─ CASCADE PERSIST → INSERT Order 1
     ├─ CASCADE PERSIST → INSERT Order 2
     └─ UPDATE Orders to set FK
```

---

**CascadeType.MERGE — Updating parent auto-updates children**

```java
@OneToMany(cascade = CascadeType.MERGE)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public User updateUserAndOrders(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    user.setName("Bob");
    user.getOrders().get(0).setProduct("Gaming Laptop");   // update first order

    return userRepository.save(user);
}
```

```text
SQL:
  UPDATE users SET name = 'Bob' WHERE id = 1;
  UPDATE orders SET product = 'Gaming Laptop' WHERE id = 1;
```

```text
merge(user)
     │
     ├─ UPDATE User (name changed)
     └─ CASCADE MERGE → UPDATE Order 1 (product changed)
```

---

**CascadeType.REMOVE — Deleting parent auto-deletes children**

```java
@OneToMany(cascade = CascadeType.REMOVE)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public void deleteUser(Long userId) {
    userRepository.deleteById(userId);
}
```

```text
SQL:
  SELECT u.* FROM users u WHERE u.id = 1;                    ← load user
  SELECT o.* FROM orders o WHERE o.user_id = 1;              ← load orders (for cascade)
  DELETE FROM orders WHERE id = 1;                            ← cascade delete order 1
  DELETE FROM orders WHERE id = 2;                            ← cascade delete order 2
  DELETE FROM users WHERE id = 1;                             ← delete user
```

```text
remove(user)
     │
     ├─ CASCADE REMOVE → DELETE Order 1
     ├─ CASCADE REMOVE → DELETE Order 2
     └─ DELETE User
```

---

**CascadeType.REFRESH — Refreshing parent auto-refreshes children**

```java
@OneToMany(cascade = CascadeType.REFRESH)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public void refreshUser(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();

    // Someone changed the order amount directly in DB to 1500.00
    user.getOrders().get(0).setAmount(new BigDecimal("0.00"));  // in-memory change

    entityManager.refresh(user);
    // Hibernate re-reads user AND all orders from DB
    // in-memory change is DISCARDED
    // order.amount is back to 1500.00 (from DB)
}
```

```text
SQL:
  SELECT u.* FROM users u WHERE u.id = 1;                    ← refresh user
  SELECT o.* FROM orders o WHERE o.user_id = 1;              ← cascade refresh all orders
```

---

**CascadeType.DETACH — Detaching parent auto-detaches children**

```java
@OneToMany(cascade = CascadeType.DETACH)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public void detachExample(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    user.getOrders().size();    // initialize lazy collection

    entityManager.detach(user);
    // user AND all orders are now DETACHED
    // changes to user or orders will NOT be auto-detected by dirty checking

    user.setName("Changed");                           // NOT tracked
    user.getOrders().get(0).setProduct("Changed");     // NOT tracked
    // No UPDATE SQL will be generated
}
```

---

**CascadeType.ALL — All operations cascaded (most common for owned collections)**

```java
@OneToMany(cascade = CascadeType.ALL)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

Equivalent to `{PERSIST, MERGE, REMOVE, REFRESH, DETACH}`.

```text
Cascade usage summary for @OneToMany:

┌──────────────────┬────────────────────────────────────────────────────┐
│ Cascade Type     │ Propagates                                        │
├──────────────────┼────────────────────────────────────────────────────┤
│ PERSIST          │ save(parent) → saves all children                 │
│ MERGE            │ save(existing parent) → updates all children      │
│ REMOVE           │ delete(parent) → deletes all children             │
│ REFRESH          │ refresh(parent) → re-reads all children from DB   │
│ DETACH           │ detach(parent) → detaches all children            │
│ ALL              │ All of the above                                  │
└──────────────────┴────────────────────────────────────────────────────┘

Industry standard for @OneToMany:
  - Owned collections (User → Orders):     CascadeType.ALL
  - Shared references (Order → Product):   NO cascade or PERSIST + MERGE only
```

---

### Orphan Child Problem — When Updating the Child List

An **orphan** is a child entity that was **removed from the parent's collection** but still exists in the database. This happens when you update the parent's list by replacing or removing children — the old children are disconnected from the parent but NOT deleted.

**The problem**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private List<Order> orders = new ArrayList<>();
}
```

**API + Service that causes orphans**

```java
// DTO for the update request
public class UpdateUserOrdersRequest {
    private String name;
    private List<OrderDTO> orders;    // new list of orders from the client
}
```

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/{id}/orders")
    public UserDTO updateUserOrders(@PathVariable Long id,
                                     @RequestBody UpdateUserOrdersRequest request) {
        return userService.updateUserOrders(id, request);
    }
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserDTO updateUserOrders(Long userId, UpdateUserOrdersRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        // user.orders = [Order(id=1, "Laptop"), Order(id=2, "Mouse")]

        // Client sends a NEW list of orders — replaces old ones
        user.getOrders().clear();    // remove old orders from the collection

        for (OrderDTO dto : request.getOrders()) {
            Order newOrder = new Order(dto.getProduct(), dto.getAmount());
            user.getOrders().add(newOrder);
        }

        userRepository.save(user);
        // ...
    }
}
```

```text
What happens WITHOUT orphanRemoval:

Before update:
┌──────────────┐      ┌──────────────────────────────┐
│ users         │      │ orders                       │
├──────────────┤      ├──────────────────────────────┤
│ id=1 "Alice" │      │ id=1, "Laptop", user_id=1    │
│              │      │ id=2, "Mouse",  user_id=1    │
└──────────────┘      └──────────────────────────────┘

After user.getOrders().clear() + add new orders:
┌──────────────┐      ┌──────────────────────────────┐
│ users         │      │ orders                       │
├──────────────┤      ├──────────────────────────────┤
│ id=1 "Alice" │      │ id=1, "Laptop", user_id=NULL │ ← ORPHAN! (FK set to NULL, row NOT deleted)
│              │      │ id=2, "Mouse",  user_id=NULL │ ← ORPHAN! (FK set to NULL, row NOT deleted)
│              │      │ id=3, "Keyboard", user_id=1  │ ← new order
│              │      │ id=4, "Monitor",  user_id=1  │ ← new order
└──────────────┘      └──────────────────────────────┘
```

**SQL generated (orphans created)**

```text
UPDATE orders SET user_id = NULL WHERE id = 1;     ← FK set to NULL (not deleted!)
UPDATE orders SET user_id = NULL WHERE id = 2;     ← FK set to NULL (not deleted!)
INSERT INTO orders (product, amount) VALUES ('Keyboard', 49.99);
INSERT INTO orders (product, amount) VALUES ('Monitor', 299.99);
UPDATE orders SET user_id = 1 WHERE id = 3;
UPDATE orders SET user_id = 1 WHERE id = 4;
```

The old orders (id=1, id=2) are now **orphans** — they exist in the database with `user_id = NULL`, not connected to any user, wasting space and causing data inconsistency.

---

### Solving with orphanRemoval = true

`orphanRemoval = true` tells Hibernate: **"If a child entity is removed from the parent's collection, DELETE it from the database."**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)   // ← orphanRemoval added
    @JoinColumn(name = "user_id")
    private List<Order> orders = new ArrayList<>();
}
```

**Same service code, different result**

```java
@Transactional
public UserDTO updateUserOrders(Long userId, UpdateUserOrdersRequest request) {
    User user = userRepository.findById(userId).orElseThrow();
    // user.orders = [Order(id=1, "Laptop"), Order(id=2, "Mouse")]

    user.getOrders().clear();    // remove old orders from the collection

    for (OrderDTO dto : request.getOrders()) {
        Order newOrder = new Order(dto.getProduct(), dto.getAmount());
        user.getOrders().add(newOrder);
    }

    userRepository.save(user);
    // Now orphans are DELETED, not just disconnected
}
```

```text
What happens WITH orphanRemoval = true:

Before update:
┌──────────────┐      ┌──────────────────────────────┐
│ users         │      │ orders                       │
├──────────────┤      ├──────────────────────────────┤
│ id=1 "Alice" │      │ id=1, "Laptop", user_id=1    │
│              │      │ id=2, "Mouse",  user_id=1    │
└──────────────┘      └──────────────────────────────┘

After user.getOrders().clear() + add new orders:
┌──────────────┐      ┌──────────────────────────────┐
│ users         │      │ orders                       │
├──────────────┤      ├──────────────────────────────┤
│ id=1 "Alice" │      │ id=3, "Keyboard", user_id=1  │ ← new order
│              │      │ id=4, "Monitor",  user_id=1  │ ← new order
└──────────────┘      └──────────────────────────────┘

Orders id=1 and id=2 are DELETED from the database — no orphans!
```

**SQL generated (orphans deleted)**

```text
DELETE FROM orders WHERE id = 1;                       ← orphan removed!
DELETE FROM orders WHERE id = 2;                       ← orphan removed!
INSERT INTO orders (product, amount) VALUES ('Keyboard', 49.99);
INSERT INTO orders (product, amount) VALUES ('Monitor', 299.99);
UPDATE orders SET user_id = 1 WHERE id = 3;
UPDATE orders SET user_id = 1 WHERE id = 4;
```

```text
Comparison:

WITHOUT orphanRemoval:
  user.getOrders().clear()  →  UPDATE orders SET user_id = NULL   (orphans remain)

WITH orphanRemoval = true:
  user.getOrders().clear()  →  DELETE FROM orders WHERE id = ?    (orphans deleted)
```

**When does orphanRemoval trigger?**

```text
user.getOrders().remove(order1);        → DELETE order1 from DB
user.getOrders().clear();               → DELETE ALL orders from DB
user.setOrders(newList);                → DELETE old orders, INSERT new ones

It triggers whenever a child is REMOVED from the parent's collection,
regardless of whether the parent is being deleted.
```

---

### Orphan Removal vs Cascade Delete

These are **different operations** that solve different problems.

```text
┌─────────────────────────┬───────────────────────────────────────────────────────┐
│                         │ What triggers deletion                               │
├─────────────────────────┼───────────────────────────────────────────────────────┤
│ CascadeType.REMOVE      │ Parent entity is DELETED                             │
│                         │ → all children are also deleted                      │
│                         │                                                      │
│ orphanRemoval = true    │ Child is REMOVED FROM THE COLLECTION                 │
│                         │ → that specific child is deleted from DB             │
│                         │ (parent is NOT deleted — it still exists)            │
└─────────────────────────┴───────────────────────────────────────────────────────┘
```

**CascadeType.REMOVE scenario — parent is deleted**

```java
@OneToMany(cascade = CascadeType.REMOVE)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
userRepository.deleteById(1L);
// DELETE FROM orders WHERE user_id = 1;     ← all orders deleted
// DELETE FROM users WHERE id = 1;           ← user deleted
```

```text
CASCADE REMOVE:
  Delete parent → children are also deleted

  user deleted
     │
     ├─ Order 1 deleted  (because parent was deleted)
     └─ Order 2 deleted  (because parent was deleted)
```

**orphanRemoval scenario — child is removed from collection, parent still exists**

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```java
@Transactional
public void removeOneOrder(Long userId, Long orderId) {
    User user = userRepository.findById(userId).orElseThrow();
    user.getOrders().removeIf(o -> o.getId().equals(orderId));
    // orphanRemoval triggers: DELETE FROM orders WHERE id = ?
    // User is NOT deleted — only the removed order is deleted
}
```

```text
ORPHAN REMOVAL:
  Remove child from collection → only that child is deleted
  Parent still exists

  user still exists
     │
     ├─ Order 1 removed from list → DELETED from DB
     └─ Order 2 still in list → still in DB
```

**Side-by-side comparison**

| | CascadeType.REMOVE | orphanRemoval = true |
|---|---|---|
| **Trigger** | `delete(parent)` | `collection.remove(child)` or `collection.clear()` |
| **Parent deleted?** | Yes | No — parent still exists |
| **Which children deleted?** | ALL children | Only the removed child(ren) |
| **Use case** | Delete user → delete all their orders | Update user's orders → remove old, add new |
| **Without the other** | Cannot clean orphans on collection update | Does not auto-delete children when parent is deleted |
| **Together** | `cascade = ALL, orphanRemoval = true` covers both scenarios |

**Best practice for owned @OneToMany collections**

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "user_id")
private List<Order> orders = new ArrayList<>();
```

```text
cascade = CascadeType.ALL    → handles: save, update, delete parent → propagates to children
orphanRemoval = true         → handles: remove child from collection → deletes it from DB

Together they cover ALL lifecycle scenarios:
  ✅ Save parent with children
  ✅ Update parent and children
  ✅ Delete parent → children deleted
  ✅ Remove child from list → child deleted from DB (no orphans)
```




### OneToMany Bidirectional Mapping

In a bidirectional `@OneToMany` / `@ManyToOne` mapping, **both** the parent and child entities have references to each other. The parent has a collection of children, and each child has a reference back to the parent.

**Real-life use case**: A `User` has many `Order`s (parent → children). Each `Order` belongs to one `User` (child → parent). You can navigate both directions: get all orders for a user, or get the user who placed an order.

```text
Java Objects:                              Database Tables:

┌───────────────────────┐                  ┌──────────────────────────┐
│  User (PARENT)        │                  │  users                   │
│                       │                  ├──────────────────────────┤
│  id: Long             │  @OneToMany      │  id (PK, BIGINT)         │
│  name: String         │  (mappedBy)      │  name (VARCHAR)          │
│  orders: List<Order>  ├────────────>     │                          │
│                       │                  └──────────────────────────┘
│                       │<────────────┐
└───────────────────────┘             │
                                      │    ┌──────────────────────────┐
┌───────────────────────┐             │    │  orders                  │
│  Order (CHILD)        │             │    ├──────────────────────────┤
│                       │  @ManyToOne │    │  id (PK, BIGINT)         │
│  id: Long             │  @JoinColumn│    │  product (VARCHAR)       │
│  product: String      │             │    │  amount (DECIMAL)        │
│  amount: BigDecimal   ├─────────────┘    │  user_id (FK, BIGINT) ───┼──> users.id
│  user: User           │                  │                          │
│                       │                  │  FK lives in CHILD table │
└───────────────────────┘                  └──────────────────────────┘

Direction: User <──────────> Order   (both ways)
           User knows its Orders.   Order knows its User.
```

---

### Owning Side vs Inverse Side

In every bidirectional relationship, JPA requires you to define:

- **Owning side**: The entity whose table holds the **foreign key**. This side **controls** the relationship — JPA reads the owning side to decide what SQL to generate.
- **Inverse side**: The entity that **mirrors** the relationship using `mappedBy`. It does NOT control the FK — it is just a convenience for navigation.

**Is the owning side always the parent?**

**No — it is the opposite.** In a `@OneToMany` / `@ManyToOne` bidirectional mapping, the **child** (the `@ManyToOne` side) is **always the owning side** because the FK naturally lives in the child table.

```text
┌────────────────────────────────┐        ┌────────────────────────────────┐
│  User (INVERSE SIDE)           │        │  Order (OWNING SIDE)           │
│                                │        │                                │
│  @OneToMany(mappedBy = "user") │        │  @ManyToOne                    │
│  private List<Order> orders;   │        │  @JoinColumn(name = "user_id") │
│                                │        │  private User user;            │
│  "I'm just the mirror.        │        │                                │
│   I do NOT control the FK."   │        │  "I control the FK.            │
│                                │        │   user_id is in MY table."     │
│  → NO @JoinColumn here        │        │  → @JoinColumn here            │
└────────────────────────────────┘        └────────────────────────────────┘
```

```text
Why the child is the owning side:

In relational databases, a "many" side always holds the FK:
  - One User has many Orders
  - The FK (user_id) goes in the Orders table
  - Each Order row points to its User via user_id
  - You can't put a "list of order IDs" inside a single user row

Therefore:
  Child table (orders) has the FK → Child entity is the owning side
  Parent table (users) has NO FK → Parent entity is the inverse side
```

**Which side contains the FK relationship?** The **child** (owning side).

**Where do we put @JoinColumn?** On the **child** entity (the `@ManyToOne` side).

---

### Child Entity Holds the Foreign Key by Default

In a bidirectional `@OneToMany` / `@ManyToOne`, the FK **always** lives in the child table. This is a fundamental rule of relational databases — the "many" side holds the FK to the "one" side.

**Code example**

```java
// CHILD — OWNING SIDE (holds the FK)
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")                // FK column in orders table
    private User user;                            // reference to parent

    // constructors, getters, setters
}
```

```java
// PARENT — INVERSE SIDE (mirrors the relationship)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // constructors, getters, setters
}
```

**Generated DDL**

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product VARCHAR(255),
    amount DECIMAL(19,2),
    user_id BIGINT,                                -- FK in CHILD table
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

```text
DB Tables:

┌──────────────────┐                    ┌──────────────────────────────┐
│ users             │                    │ orders                       │
├──────────────────┤                    ├──────────────────────────────┤
│ id=1, "Alice"    │◄───────────────────│ id=1, "Laptop", user_id=1   │
│                  │◄───────────────────│ id=2, "Mouse",  user_id=1   │
│                  │                    │                              │
│ id=2, "Bob"      │◄───────────────────│ id=3, "Phone",  user_id=2   │
└──────────────────┘                    └──────────────────────────────┘

FK (user_id) is in the orders table.
No FK in the users table.
Only 2 tables — no join table needed.
```

**SQL for saving**

```java
@Transactional
public User createUserWithOrders() {
    User user = new User();
    user.setName("Alice");

    Order order1 = new Order();
    order1.setProduct("Laptop");
    order1.setAmount(new BigDecimal("999.99"));
    order1.setUser(user);                     // ← SET PARENT IN CHILD (critical!)

    Order order2 = new Order();
    order2.setProduct("Mouse");
    order2.setAmount(new BigDecimal("29.99"));
    order2.setUser(user);                     // ← SET PARENT IN CHILD (critical!)

    user.getOrders().add(order1);
    user.getOrders().add(order2);

    return userRepository.save(user);
}
```

```text
SQL generated:
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, 1);    ← FK set directly!
  INSERT INTO orders (product, amount, user_id) VALUES ('Mouse', 29.99, 1);      ← FK set directly!
```

Notice: Unlike unidirectional `@OneToMany` (which does INSERT with NULL FK + UPDATE), bidirectional sets the FK **directly** in the INSERT — more efficient!

---

### mappedBy on the @OneToMany (Inverse/Parent Side)

`mappedBy` tells JPA: "I am NOT the owner of this relationship. The owner is the field named in `mappedBy` on the other entity. Do NOT create a FK or join table for me."

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> orders = new ArrayList<>();
```

The value `"user"` refers to the **field name** in the `Order` entity:

```java
// In Order.java
@ManyToOne
@JoinColumn(name = "user_id")
private User user;              // ← this is the field "user" that mappedBy refers to
```

```text
@OneToMany(mappedBy = "user")  ─── refers to ──→  Order.user field
                                                        │
                                                  @ManyToOne
                                                  @JoinColumn(name = "user_id")
                                                  The REAL FK owner
```

**What happens WITHOUT mappedBy?**

```text
WITHOUT mappedBy on @OneToMany:

@Entity User:
    @OneToMany(cascade = CascadeType.ALL)
    private List<Order> orders;

@Entity Order:
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

Result: JPA sees TWO independent relationships!
  1. User → Orders  (creates a users_orders join table or separate FK management)
  2. Order → User   (creates user_id FK in orders table)

  ┌──────────────┐     ┌──────────────────────┐     ┌─────────────────────┐
  │ users         │     │ users_orders          │     │ orders              │
  │ id (PK)      │────>│ user_id, orders_id    │<────│ id (PK)             │
  │ name         │     │                      │     │ product             │
  │              │     └──────────────────────┘     │ amount              │
  │              │                                  │ user_id (FK) ───────│──> users.id
  └──────────────┘                                  └─────────────────────┘

  3 tables!  Duplicate relationship!  Extra INSERTs and UPDATEs!
```

```text
WITH mappedBy = "user" on @OneToMany:

@Entity User:
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders;

@Entity Order:
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

Result: JPA sees ONE relationship managed by Order.user:
  ┌──────────────┐                              ┌─────────────────────┐
  │ users         │                              │ orders              │
  │ id (PK)      │                              │ id (PK)             │
  │ name         │                              │ product             │
  │              │                              │ amount              │
  │              │◄──────────────────────────────│ user_id (FK)        │
  └──────────────┘                              └─────────────────────┘

  2 tables!  Single FK!  Clean and efficient!
```

**Generated SQL comparison**

```text
WITHOUT mappedBy (3 tables, extra SQL):
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, 1);
  INSERT INTO users_orders (user_id, orders_id) VALUES (1, 1);     ← unnecessary!

WITH mappedBy (2 tables, clean SQL):
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, 1);
  (no join table inserts)
```

---

### What Happens If You Only Set the Child List on the Parent (Not the Parent on Child)

Since JPA **only reads the owning side** (`Order.user`) to manage the FK, if you only add Orders to User's list but do NOT set `order.setUser(user)`, the FK column will be **NULL**.

**The problem**

```java
@Transactional
public User createUserWithOrders_WRONG() {
    User user = new User();
    user.setName("Alice");

    Order order1 = new Order();
    order1.setProduct("Laptop");
    order1.setAmount(new BigDecimal("999.99"));
    // order1.setUser(user);     ← NOT SET! Missing!

    Order order2 = new Order();
    order2.setProduct("Mouse");
    order2.setAmount(new BigDecimal("29.99"));
    // order2.setUser(user);     ← NOT SET! Missing!

    user.getOrders().add(order1);      // only adding to parent's list
    user.getOrders().add(order2);      // only adding to parent's list

    return userRepository.save(user);
}
```

```text
SQL generated:
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, NULL);    ← user_id is NULL!
  INSERT INTO orders (product, amount, user_id) VALUES ('Mouse', 29.99, NULL);      ← user_id is NULL!
```

```text
DB after save:

┌──────────────────┐                    ┌──────────────────────────────────┐
│ users             │                    │ orders                           │
├──────────────────┤                    ├──────────────────────────────────┤
│ id=1, "Alice"    │    NO connection!  │ id=1, "Laptop", user_id=NULL    │ ← orphan!
│                  │                    │ id=2, "Mouse",  user_id=NULL    │ ← orphan!
└──────────────────┘                    └──────────────────────────────────┘

The orders exist in the DB but have NO connection to the user!
```

**Why does this happen?**

```text
JPA relationship management rules:

1. JPA only looks at the OWNING SIDE to determine FK values.
2. The owning side is Order.user (annotated with @ManyToOne + @JoinColumn).
3. We never called order.setUser(user) → Order.user = null → user_id = NULL.
4. Adding orders to user.getOrders() is for Java navigation only.
   JPA IGNORES the inverse side (User.orders) when generating INSERT SQL.

  User.orders = [order1, order2]    ← JPA ignores this for FK
  Order.user = null                 ← JPA reads THIS for FK → NULL
```

**The correct way — always set BOTH sides**

```java
@Transactional
public User createUserWithOrders_CORRECT() {
    User user = new User();
    user.setName("Alice");

    Order order1 = new Order();
    order1.setProduct("Laptop");
    order1.setAmount(new BigDecimal("999.99"));
    order1.setUser(user);                     // ← SET owning side (controls FK)

    Order order2 = new Order();
    order2.setProduct("Mouse");
    order2.setAmount(new BigDecimal("29.99"));
    order2.setUser(user);                     // ← SET owning side (controls FK)

    user.getOrders().add(order1);             // ← SET inverse side (for Java navigation)
    user.getOrders().add(order2);             // ← SET inverse side (for Java navigation)

    return userRepository.save(user);
}
```

```text
SQL generated:
  INSERT INTO users (name) VALUES ('Alice');
  INSERT INTO orders (product, amount, user_id) VALUES ('Laptop', 999.99, 1);    ← FK set correctly!
  INSERT INTO orders (product, amount, user_id) VALUES ('Mouse', 29.99, 1);      ← FK set correctly!
```

**Best practice — add a helper method to the parent to keep both sides in sync**

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // Helper method — sets BOTH sides of the relationship
    public void addOrder(Order order) {
        orders.add(order);          // inverse side (Java navigation)
        order.setUser(this);        // owning side (controls FK)
    }

    public void removeOrder(Order order) {
        orders.remove(order);       // inverse side
        order.setUser(null);        // owning side (nullifies FK)
    }
}
```

```java
// Now usage is simple and safe:
User user = new User();
user.setName("Alice");
user.addOrder(order1);    // sets both sides automatically
user.addOrder(order2);    // sets both sides automatically
userRepository.save(user);
```

---

### @JsonIgnore on the Parent Reference in Child Entity

Yes — you **should** put `@JsonIgnore` on the parent reference in the child entity to prevent infinite recursion when serializing bidirectional relationships.

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore                           // ← prevents infinite recursion
    private User user;
}
```

**Why it is needed**

Without `@JsonIgnore`, Jackson serializes User → orders → [Order → user → User → orders → ...] infinitely:

```text
WITHOUT @JsonIgnore:

{
  "id": 1,
  "name": "Alice",
  "orders": [
    {
      "id": 1,
      "product": "Laptop",
      "user": {                          ← serializes User AGAIN
        "id": 1,
        "name": "Alice",
        "orders": [                      ← serializes orders AGAIN
          {
            "user": {                    ← INFINITE LOOP → StackOverflowError
              ...
```

```text
WITH @JsonIgnore on Order.user:

{
  "id": 1,
  "name": "Alice",
  "orders": [
    {
      "id": 1,
      "product": "Laptop",
      "amount": 999.99
    },
    {
      "id": 2,
      "product": "Mouse",
      "amount": 29.99
    }
  ]
}

Clean response — no infinite recursion.
"user" field in each Order is skipped by Jackson.
```

**Limitation**: `@JsonIgnore` **always** hides the `user` field in Order's JSON. If you need to show the user when fetching an Order individually, you have the same alternatives as `@OneToOne`:

| Solution | Behavior |
|---|---|
| `@JsonIgnore` on `Order.user` | Always hides user in Order JSON. Simplest. |
| `@JsonManagedReference` on `User.orders` + `@JsonBackReference` on `Order.user` | User shows orders, Order hides user. |
| `@JsonIdentityInfo` on both entities | Both directions visible, second occurrence replaced with ID. |
| **DTO (recommended)** | Full control over response shape. No annotations on entities. |

**In practice**, for `@OneToMany` bidirectional, using `@JsonIgnore` on the child's parent reference is the most common approach because:
- When fetching User, you want orders included → User.orders is serialized.
- When fetching Order, you rarely need the full User object embedded → `@JsonIgnore` on `Order.user` is acceptable.
- For cases where you need User info in the Order response, you create a DTO with a flat `userId` or `userName` field.

---

### ManyToOne Bidirectional Mapping

A `@ManyToOne` bidirectional mapping is the **exact same relationship** as `@OneToMany` bidirectional, viewed from the **child's perspective**. It is not a separate concept — it is the other side of the same coin.

**Real-life use case**: An `Order` belongs to one `User`. The Order has a reference to its User. The User has a list of Orders. This is the same User-Order relationship — you are just starting from the Order's point of view.

```text
From @OneToMany perspective (User's view):
    User (1) ──────────> Orders (N)
    "I have many orders"

From @ManyToOne perspective (Order's view):
    Order (N) ──────────> User (1)
    "I belong to one user"

Both views describe the SAME relationship and the SAME tables.
```

```text
Java Objects:

┌───────────────────────┐    @ManyToOne     ┌───────────────────────┐
│  Order (CHILD)        │ ──────────────>   │  User (PARENT)        │
│                       │                   │                       │
│  id: Long             │                   │  id: Long             │
│  product: String      │                   │  name: String         │
│  amount: BigDecimal   │                   │  orders: List<Order>  │
│  user: User ──────────┼─── FK             │                       │
│                       │                   │                       │
└───────────────────────┘   <──────────────┤│                       │
                              @OneToMany    └───────────────────────┘
                              (mappedBy)


Database Tables (IDENTICAL to @OneToMany bidirectional):

┌──────────────────┐                    ┌──────────────────────────────┐
│ users             │                    │ orders                       │
├──────────────────┤                    ├──────────────────────────────┤
│ id (PK)          │◄───────────────────│ id (PK)                      │
│ name             │                    │ product                      │
│                  │                    │ amount                       │
│                  │                    │ user_id (FK) ────────────────│──> users.id
└──────────────────┘                    └──────────────────────────────┘
```

**How it is similar to @OneToMany bidirectional**

| Aspect | @OneToMany Bidirectional | @ManyToOne Bidirectional |
|---|---|---|
| **Relationship** | Same | Same |
| **Tables** | users + orders (FK in orders) | users + orders (FK in orders) |
| **Owning side** | Order (child, @ManyToOne) | Order (child, @ManyToOne) |
| **Inverse side** | User (parent, @OneToMany mappedBy) | User (parent, @OneToMany mappedBy) |
| **Code** | Identical | Identical |
| **SQL** | Identical | Identical |
| **FK location** | orders.user_id | orders.user_id |

They are **the same mapping** — just described from different perspectives. When we say "OneToMany bidirectional" we emphasize the parent having a list. When we say "ManyToOne bidirectional" we emphasize the child pointing to its parent. The code, tables, and SQL are identical.

**Code example (identical to @OneToMany bidirectional)**

```java
// CHILD — OWNING SIDE (same as before)
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;

    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")               // FK in child table
    @JsonIgnore
    private User user;                           // Many orders → One user
}
```

```java
// PARENT — INVERSE SIDE (same as before)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();   // One user → Many orders

    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }
}
```

**Real-life usage — starting from the Order's perspective**

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    // Create an order for an existing user (ManyToOne perspective)
    @Transactional
    public Order createOrder(Long userId, String product, BigDecimal amount) {
        User user = userRepository.findById(userId).orElseThrow();

        Order order = new Order();
        order.setProduct(product);
        order.setAmount(amount);
        order.setUser(user);           // ManyToOne — set the parent on the child

        return orderRepository.save(order);
    }

    // Get the user who placed an order (ManyToOne navigation)
    @Transactional(readOnly = true)
    public UserDTO getUserForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User user = order.getUser();    // navigate from child → parent
        return new UserDTO(user.getId(), user.getName());
    }
}
```

```text
SQL for createOrder:
  INSERT INTO orders (product, amount, user_id) VALUES ('Keyboard', 49.99, 1);

SQL for getUserForOrder:
  SELECT o.* FROM orders o WHERE o.id = 1;                    ← load order
  SELECT u.* FROM users u WHERE u.id = 1;                     ← lazy load user (when getUser() accessed)
```

**When to use which name**

```text
Say "@OneToMany bidirectional" when:
  - You are working from the parent side (User managing its Orders)
  - You are designing the parent entity and its collection

Say "@ManyToOne bidirectional" when:
  - You are working from the child side (Order referencing its User)
  - You are creating a child entity and assigning it to a parent
  - You need to navigate from child → parent

Both are the same underlying relationship — only the emphasis differs.
```

---

### How @ManyToOne Bidirectional is the Same as @OneToMany Bidirectional

They are **not two different mappings** — they are the **same single relationship** expressed with two annotations. A bidirectional relationship always requires **both** `@OneToMany` on the parent and `@ManyToOne` on the child. You cannot have one without the other in a bidirectional setup.

**Real-life use case — Department and Employee**

A `Department` has many `Employee`s. An `Employee` belongs to one `Department`. Whether you call this "@OneToMany bidirectional" or "@ManyToOne bidirectional" depends only on which entity you start talking about.

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│  "@OneToMany bidirectional" = "@ManyToOne bidirectional"                     │
│                                                                              │
│  They are the SAME relationship, described from two viewpoints:              │
│                                                                              │
│  ┌─────────────────────┐          ┌──────────────────────┐                   │
│  │  Department          │          │  Employee             │                   │
│  │  (PARENT)            │          │  (CHILD)              │                   │
│  │                      │          │                       │                   │
│  │  @OneToMany          │          │  @ManyToOne           │                   │
│  │  (mappedBy)          │          │  @JoinColumn          │                   │
│  │  List<Employee>      │─────────>│  department: Dept     │                   │
│  │  employees           │<─────────│                       │                   │
│  │                      │          │  FK: department_id    │                   │
│  │  INVERSE side        │          │  OWNING side          │                   │
│  └─────────────────────┘          └──────────────────────┘                   │
│                                                                              │
│  Both annotations exist in EVERY bidirectional 1:N relationship.             │
│  You can't have @OneToMany bidirectional WITHOUT @ManyToOne.                 │
│  You can't have @ManyToOne bidirectional WITHOUT @OneToMany.                 │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Code — the complete bidirectional relationship**

```java
// PARENT — Department has many Employees
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Employee> employees = new ArrayList<>();

    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setDepartment(this);
    }

    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.setDepartment(null);
    }
}
```

```java
// CHILD — Employee belongs to one Department
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnore
    private Department department;
}
```

**Same relationship, two entry points**

```java
// FROM @OneToMany PERSPECTIVE — "Department has employees"
@Service
public class DepartmentService {

    @Transactional
    public DepartmentDTO getDepartmentWithEmployees(Long deptId) {
        Department dept = departmentRepository.findById(deptId).orElseThrow();
        // Navigate: Department → employees (OneToMany direction)
        List<EmployeeDTO> empDTOs = dept.getEmployees().stream()
            .map(e -> new EmployeeDTO(e.getId(), e.getName()))
            .toList();
        return new DepartmentDTO(dept.getId(), dept.getName(), empDTOs);
    }
}
```

```java
// FROM @ManyToOne PERSPECTIVE — "Employee belongs to department"
@Service
public class EmployeeService {

    @Transactional
    public EmployeeWithDeptDTO getEmployeeWithDepartment(Long empId) {
        Employee emp = employeeRepository.findById(empId).orElseThrow();
        // Navigate: Employee → department (ManyToOne direction)
        Department dept = emp.getDepartment();
        return new EmployeeWithDeptDTO(emp.getId(), emp.getName(), dept.getName());
    }

    @Transactional
    public Employee assignToDepartment(Long empId, Long deptId) {
        Employee emp = employeeRepository.findById(empId).orElseThrow();
        Department dept = departmentRepository.findById(deptId).orElseThrow();
        emp.setDepartment(dept);    // ManyToOne — set FK on owning side
        return employeeRepository.save(emp);
    }
}
```

**SQL is identical regardless of which name you use**

```text
getDepartmentWithEmployees (OneToMany perspective):
  SELECT d.* FROM departments d WHERE d.id = 1;
  SELECT e.* FROM employees e WHERE e.department_id = 1;       ← lazy load collection

getEmployeeWithDepartment (ManyToOne perspective):
  SELECT e.* FROM employees e WHERE e.id = 1;
  SELECT d.* FROM departments d WHERE d.id = 1;                ← lazy load parent

assignToDepartment (ManyToOne perspective):
  UPDATE employees SET department_id = 2 WHERE id = 1;         ← FK updated on owning side
```

**DB tables — always the same structure**

```text
┌──────────────────────┐                    ┌───────────────────────────────────┐
│ departments           │                    │ employees                         │
├──────────────────────┤                    ├───────────────────────────────────┤
│ id=1, "Engineering"  │◄───────────────────│ id=1, "Alice", department_id=1   │
│                      │◄───────────────────│ id=2, "Bob",   department_id=1   │
│ id=2, "Marketing"    │◄───────────────────│ id=3, "Eve",   department_id=2   │
└──────────────────────┘                    └───────────────────────────────────┘

Whether you call this "@OneToMany bidirectional" or "@ManyToOne bidirectional":
  - Same 2 tables
  - Same FK (department_id in employees)
  - Same owning side (Employee)
  - Same inverse side (Department)
  - Same SQL
```

**Summary**

```text
"@OneToMany bidirectional" and "@ManyToOne bidirectional" are just two names
for the SAME relationship.

Every bidirectional 1:N relationship has BOTH annotations:
  - @OneToMany(mappedBy = "...") on the parent    (inverse side)
  - @ManyToOne + @JoinColumn on the child          (owning side)

The difference is only in perspective:
  ┌─────────────────────────────┬────────────────────────────────────┐
  │ Name                        │ Emphasis                           │
  ├─────────────────────────────┼────────────────────────────────────┤
  │ @OneToMany bidirectional    │ Parent → children (list)           │
  │ @ManyToOne bidirectional    │ Child → parent (single reference)  │
  └─────────────────────────────┴────────────────────────────────────┘

  Code? Same.   Tables? Same.   SQL? Same.   FK? Same.
```

---

### ManyToMany Unidirectional Mapping

In a `@ManyToMany` relationship, **multiple entities on one side** are associated with **multiple entities on the other side**. There is no parent-child hierarchy — both entities are **independent** and equal.

**Real-life use case**: An `Order` can contain many `Product`s. A `Product` can appear in many `Order`s. Neither owns the other — they are independent business entities.

```text
Java Objects:

┌───────────────────────┐                  ┌───────────────────────┐
│  Order                │    @ManyToMany   │  Product              │
│                       │                  │                       │
│  id: Long             │                  │  id: Long             │
│  orderDate: LocalDate │                  │  name: String         │
│  products: Set<Product├─────────────────>│  price: BigDecimal    │
│                       │                  │                       │
│  (OWNING entity)      │                  │  (NO ref to Order)    │
└───────────────────────┘                  └───────────────────────┘

Direction: Order ──────> Products   (one way only, unidirectional)
           Order knows its Products.   Product does NOT know its Orders.
```

---

### No Parent-Child Concept in ManyToMany

In `@OneToMany` / `@ManyToOne`, there is a clear parent-child hierarchy: User (parent) owns Orders (children), the child cannot exist without the parent. In `@ManyToMany`, **neither entity owns the other**.

```text
@OneToMany (parent-child):                  @ManyToMany (peers):

  User (PARENT)                               Order ←──────→ Product
    │                                         (independent)   (independent)
    └── Order (CHILD, depends on User)
                                              Neither is parent.
  Parent deleted → child deleted              Neither depends on the other.
  Child has FK to parent                      Deleting Order does NOT delete Product.
                                              Deleting Product does NOT delete Order.
```

**Who is the owning entity in unidirectional @ManyToMany?**

The entity that has the `@ManyToMany` annotation with `@JoinTable` is the **owning entity**. In unidirectional, there is no inverse entity — only one side knows about the relationship.

```text
Order (OWNING):
  - Has @ManyToMany
  - Has @JoinTable
  - JPA manages the join table through this entity

Product (NOT AWARE):
  - No @ManyToMany
  - No reference to Order
  - Completely independent
```

Since there is no `mappedBy` (only the owning side exists in unidirectional), JPA **fully manages** the join table lifecycle through the owning entity. You do not need to create a join table entity — JPA handles it.

---

### @ManyToMany with @JoinTable — Code Example

**Entity code**

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    // NO reference to Order — unidirectional
    // constructors, getters, setters
}
```

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "order_products",                                    // join table name
        joinColumns = @JoinColumn(name = "order_id"),               // FK to THIS entity (Order)
        inverseJoinColumns = @JoinColumn(name = "product_id")       // FK to the OTHER entity (Product)
    )
    private Set<Product> products = new HashSet<>();

    // constructors, getters, setters

    public void addProduct(Product product) {
        products.add(product);
    }

    public void removeProduct(Product product) {
        products.remove(product);
    }
}
```

**@JoinTable properties explained**

```text
@JoinTable(
    name = "order_products",                                  ← name of the join table in DB
    joinColumns = @JoinColumn(name = "order_id"),             ← FK column pointing to the OWNING entity (Order)
    inverseJoinColumns = @JoinColumn(name = "product_id")     ← FK column pointing to the OTHER entity (Product)
)
```

```text
┌─────────────────────┐
│  @JoinTable          │
│                      │
│  name                │── "order_products" (the join table name)
│                      │
│  joinColumns         │── FK to the entity WHERE @JoinTable is declared (Order)
│    @JoinColumn       │      name = "order_id" → references orders.id
│                      │
│  inverseJoinColumns  │── FK to the OTHER entity (Product)
│    @JoinColumn       │      name = "product_id" → references products.id
│                      │
└─────────────────────┘
```

**Yes — it creates a new join table**

```text
┌──────────────────┐       ┌─────────────────────┐       ┌──────────────────┐
│ orders            │       │ order_products       │       │ products          │
├──────────────────┤       │ (JOIN TABLE)         │       ├──────────────────┤
│ id (PK)          │──────>│ order_id (FK)        │       │ id (PK)          │
│ order_date       │       │ product_id (FK) ─────┼──────>│ name             │
│                  │       │                     │       │ price            │
│                  │       │ PK(order_id,        │       │                  │
│                  │       │    product_id)       │       │                  │
└──────────────────┘       └─────────────────────┘       └──────────────────┘

3 tables: orders, products, and the auto-generated order_products join table.
The join table has a composite PK of (order_id, product_id) to prevent duplicates.
```

**Generated DDL**

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    price DECIMAL(19,2)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_date DATE
);

-- JPA auto-creates this join table:
CREATE TABLE order_products (
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (order_id, product_id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

**API + Service code**

```java
// DTO for creating an order
public class CreateOrderRequest {
    private LocalDate orderDate;
    private List<Long> productIds;      // IDs of existing products

    // getters, setters
}
```

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public OrderDTO createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
```

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Fetch existing products by IDs
        List<Product> products = productRepository.findAllById(request.getProductIds());

        Order order = new Order();
        order.setOrderDate(request.getOrderDate());

        for (Product product : products) {
            order.addProduct(product);       // add to the Set
        }

        Order saved = orderRepository.save(order);
        // JPA inserts into orders table + order_products join table automatically

        return toDTO(saved);
    }

    private OrderDTO toDTO(Order order) {
        List<ProductDTO> productDTOs = order.getProducts().stream()
            .map(p -> new ProductDTO(p.getId(), p.getName(), p.getPrice()))
            .toList();
        return new OrderDTO(order.getId(), order.getOrderDate(), productDTOs);
    }
}
```

**Generated SQL for POST /api/orders**

```text
Request body:
{
    "orderDate": "2026-04-16",
    "productIds": [1, 3, 5]
}

SQL executed:
  SELECT p.* FROM products p WHERE p.id IN (1, 3, 5);              ← fetch existing products

  INSERT INTO orders (order_date) VALUES ('2026-04-16');             ← insert order (gets id=10)

  INSERT INTO order_products (order_id, product_id) VALUES (10, 1); ← link order to product 1
  INSERT INTO order_products (order_id, product_id) VALUES (10, 3); ← link order to product 3
  INSERT INTO order_products (order_id, product_id) VALUES (10, 5); ← link order to product 5
```

```text
DB after save:

orders:
  id=10, order_date='2026-04-16'

order_products (join table):
  order_id=10, product_id=1
  order_id=10, product_id=3
  order_id=10, product_id=5

products (unchanged — they already existed):
  id=1, "Laptop", 999.99
  id=3, "Keyboard", 49.99
  id=5, "Monitor", 299.99
```

**JPA manages the join table — you do not**

Since the owning entity (Order) has `@ManyToMany` + `@JoinTable` without `mappedBy`, JPA fully manages the `order_products` table:

```text
What JPA manages automatically:
  ✅ Creating the join table at startup (if ddl-auto = update/create)
  ✅ Inserting rows when you add products to order.products
  ✅ Deleting rows when you remove products from order.products
  ✅ Deleting all join rows when the order is deleted

What you do NOT need to do:
  ❌ No @Entity for order_products
  ❌ No Repository for order_products
  ❌ No manual INSERT/DELETE on the join table
  ❌ No mappedBy (unidirectional — only one side manages it)
```

**Why CascadeType.PERSIST + MERGE (not ALL) for @ManyToMany?**

```text
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})

Why NOT CascadeType.ALL (which includes REMOVE)?

  - Products are INDEPENDENT entities shared across many Orders.
  - If you delete Order #10, you do NOT want to delete "Laptop" from products table.
  - Other orders also reference "Laptop".
  - CascadeType.REMOVE on @ManyToMany would delete the Product entity itself,
    breaking all other orders that reference it.

  CascadeType.PERSIST  → auto-save new products when saving an order (if needed)
  CascadeType.MERGE    → auto-update product changes when updating an order
  NO REMOVE            → deleting order only removes join table rows, not products
```

---

### ManyToMany Bidirectional Mapping

In a bidirectional `@ManyToMany`, **both** entities have a reference to each other. Both can navigate to the other side.

**Real-life use case**: An `Order` has many `Product`s, and a `Product` can be in many `Order`s. You want to navigate both ways: get products for an order, AND get all orders for a product.

```text
Java Objects:

┌───────────────────────┐    @ManyToMany    ┌───────────────────────┐
│  Order                │    @JoinTable     │  Product              │
│                       │                   │                       │
│  id: Long             │                   │  id: Long             │
│  orderDate: LocalDate ├──────────────────>│  name: String         │
│  products: Set<Product│                   │  price: BigDecimal    │
│                       │<──────────────────┤  orders: Set<Order>   │
│  (OWNING side)        │    @ManyToMany    │                       │
│                       │    (mappedBy)     │  (INVERSE side)       │
└───────────────────────┘                   └───────────────────────┘

Direction: Order <──────────> Product   (both ways)
```

---

### Anyone Can Be Owning or Inverse Side

In `@ManyToMany`, since both entities are **peers** (no parent-child), you can choose either as the owning side. The choice is a **design decision**, not a database constraint.

```text
Option A: Order is owning side (more common)
  Order has @ManyToMany + @JoinTable
  Product has @ManyToMany(mappedBy = "products")

Option B: Product is owning side
  Product has @ManyToMany + @JoinTable
  Order has @ManyToMany(mappedBy = "orders")

Both produce the SAME join table and SAME DB structure.
The difference: JPA reads the OWNING side to manage the join table.
So you should make the entity you UPDATE MORE OFTEN the owning side.
```

**Convention**: The entity from which you **add/remove relationships most often** should be the owning side. Typically, when placing an Order you add products to it — so Order is the owning side.

---

### DB Structure — Same as Unidirectional

The database structure is **identical** to unidirectional `@ManyToMany`. Adding bidirectional navigation does NOT change the tables — it only adds a Java-side reference.

```text
DB Tables (SAME for unidirectional and bidirectional):

┌──────────────────┐       ┌─────────────────────┐       ┌──────────────────┐
│ orders            │       │ order_products       │       │ products          │
├──────────────────┤       │ (JOIN TABLE)         │       ├──────────────────┤
│ id (PK)          │──────>│ order_id (FK)        │       │ id (PK)          │
│ order_date       │       │ product_id (FK) ─────┼──────>│ name             │
│                  │       │                     │       │ price            │
└──────────────────┘       │ PK(order_id,        │       └──────────────────┘
                           │    product_id)       │
                           └─────────────────────┘

Still 3 tables.  Still a join table.
Neither entity table holds FKs to the other.
ALL relationship data is in the join table.
```

```text
Why a join table (not FKs in entity tables)?

  - Cannot put "list of product IDs" in a single orders row    → violates 1NF
  - Cannot put "list of order IDs" in a single products row    → violates 1NF
  - A separate join table is the ONLY way to represent M:N in relational DBs

  Unidirectional:  Same 3 tables, only Order knows about Products in Java.
  Bidirectional:   Same 3 tables, both Order and Product know about each other in Java.
```

---

### Code Example — Bidirectional @ManyToMany

**Owning side — Order (has @ManyToMany + @JoinTable)**

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "order_products",
        joinColumns = @JoinColumn(name = "order_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> products = new HashSet<>();

    // Helper methods — keep both sides in sync
    public void addProduct(Product product) {
        products.add(product);
        product.getOrders().add(this);      // sync inverse side
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.getOrders().remove(this);   // sync inverse side
    }

    // constructors, getters, setters
}
```

**Inverse side — Product (has @ManyToMany with mappedBy)**

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    @ManyToMany(mappedBy = "products")       // "products" = field name in Order
    @JsonIgnore                               // prevent infinite recursion
    private Set<Order> orders = new HashSet<>();

    // constructors, getters, setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();     // stable hashCode for Sets
    }
}
```

**Why `equals()` and `hashCode()` on entities used in Sets?**

```text
@ManyToMany uses Set<Product> and Set<Order>.
Sets rely on equals()/hashCode() to determine membership.

Without proper equals()/hashCode():
  - set.add(product) and set.remove(product) may not work correctly.
  - Duplicate entries can appear in the Set.
  - Hibernate may generate unnecessary INSERT/DELETE on the join table.

Rule: Use the @Id field for equals(), and a stable constant for hashCode().
```

---

### API + Service Code — Bidirectional

**DTOs**

```java
public class CreateOrderRequest {
    private LocalDate orderDate;
    private List<Long> productIds;
}

public class OrderDTO {
    private Long id;
    private LocalDate orderDate;
    private List<ProductDTO> products;
}

public class ProductDTO {
    private Long id;
    private String name;
    private BigDecimal price;
}

public class ProductWithOrdersDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private List<OrderSummaryDTO> orders;
}

public class OrderSummaryDTO {
    private Long id;
    private LocalDate orderDate;
}
```

**Controller**

```java
@RestController
@RequestMapping("/api")
public class OrderProductController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    // Create order with products (owning side)
    @PostMapping("/orders")
    public OrderDTO createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    // Get all orders for a product (inverse side navigation)
    @GetMapping("/products/{id}/orders")
    public ProductWithOrdersDTO getProductWithOrders(@PathVariable Long id) {
        return productService.getProductWithOrders(id);
    }
}
```

**OrderService — owning side operations**

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        List<Product> products = productRepository.findAllById(request.getProductIds());

        Order order = new Order();
        order.setOrderDate(request.getOrderDate());

        for (Product product : products) {
            order.addProduct(product);    // sets BOTH sides (owning + inverse)
        }

        Order saved = orderRepository.save(order);
        // JPA manages join table via the OWNING side (Order)

        return toDTO(saved);
    }

    @Transactional
    public OrderDTO removeProductFromOrder(Long orderId, Long productId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();

        order.removeProduct(product);   // removes from BOTH sides
        // JPA deletes the join table row automatically

        return toDTO(order);
    }

    private OrderDTO toDTO(Order order) {
        List<ProductDTO> productDTOs = order.getProducts().stream()
            .map(p -> new ProductDTO(p.getId(), p.getName(), p.getPrice()))
            .toList();
        return new OrderDTO(order.getId(), order.getOrderDate(), productDTOs);
    }
}
```

**ProductService — inverse side navigation**

```java
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductWithOrdersDTO getProductWithOrders(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow();

        // Navigate from inverse side: Product → Orders
        List<OrderSummaryDTO> orderDTOs = product.getOrders().stream()
            .map(o -> new OrderSummaryDTO(o.getId(), o.getOrderDate()))
            .toList();

        return new ProductWithOrdersDTO(
            product.getId(), product.getName(), product.getPrice(), orderDTOs
        );
    }
}
```

**Generated SQL for POST /api/orders**

```text
Request:
{
    "orderDate": "2026-04-16",
    "productIds": [1, 3, 5]
}

SQL:
  SELECT p.* FROM products p WHERE p.id IN (1, 3, 5);              ← fetch products

  INSERT INTO orders (order_date) VALUES ('2026-04-16');             ← insert order (id=10)

  INSERT INTO order_products (order_id, product_id) VALUES (10, 1); ← join table row
  INSERT INTO order_products (order_id, product_id) VALUES (10, 3); ← join table row
  INSERT INTO order_products (order_id, product_id) VALUES (10, 5); ← join table row
```

**Generated SQL for GET /api/products/1/orders**

```text
SQL:
  SELECT p.* FROM products p WHERE p.id = 1;                                     ← load product

  SELECT o.* FROM orders o                                                        ← load orders
  JOIN order_products op ON o.id = op.order_id                                     via join table
  WHERE op.product_id = 1;
```

**Generated SQL for removing a product from an order**

```text
SQL:
  DELETE FROM order_products WHERE order_id = 10 AND product_id = 3;   ← only join table row deleted
                                                                        ← Product(id=3) still exists
                                                                        ← Order(id=10) still exists
```

---

### JPA Manages Everything from the Owning Side

JPA only reads the **owning side** to decide what to do with the join table. The inverse side (`mappedBy`) is purely for Java navigation.

```text
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  OWNING SIDE (Order):                                               │
│    order.addProduct(product)  → JPA inserts into order_products     │
│    order.removeProduct(product) → JPA deletes from order_products   │
│    orderRepository.delete(order) → JPA deletes join rows + order    │
│                                                                     │
│  INVERSE SIDE (Product):                                            │
│    product.getOrders().add(order)  → JPA does NOTHING               │
│    product.getOrders().remove(order) → JPA does NOTHING             │
│    productRepository.delete(product) → deletes product only,        │
│                                         NOT join table rows!        │
│                                         (may cause FK violation)    │
│                                                                     │
│  Rule: ALWAYS manage the relationship through the OWNING side.      │
│        The inverse side is just a mirror for navigation.            │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**What happens if you only modify the inverse side?**

```java
// WRONG — modifying only the INVERSE side
@Transactional
public void addOrderToProduct_WRONG(Long productId, Long orderId) {
    Product product = productRepository.findById(productId).orElseThrow();
    Order order = orderRepository.findById(orderId).orElseThrow();

    product.getOrders().add(order);     // inverse side only
    // JPA generates NO INSERT into order_products!
    // The relationship is NOT saved!
}
```

```java
// CORRECT — modify the OWNING side (or use helper that syncs both)
@Transactional
public void addProductToOrder_CORRECT(Long orderId, Long productId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    Product product = productRepository.findById(productId).orElseThrow();

    order.addProduct(product);          // owning side + syncs inverse
    // JPA generates INSERT into order_products
}
```

**Summary — @ManyToMany bidirectional**

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│  @ManyToMany Bidirectional                                                   │
│                                                                              │
│  DB structure: SAME as unidirectional (3 tables: orders, products, join)     │
│  Java difference: Product now has Set<Order> orders (back reference)         │
│                                                                              │
│  Owning side (Order):                                                        │
│    @ManyToMany + @JoinTable(name, joinColumns, inverseJoinColumns)           │
│    → JPA manages join table through this side                                │
│                                                                              │
│  Inverse side (Product):                                                     │
│    @ManyToMany(mappedBy = "products")                                        │
│    → Just for navigation, JPA ignores changes here                           │
│                                                                              │
│  Always modify the OWNING side to persist relationship changes.              │
│  Use helper methods (addProduct/removeProduct) to sync both sides.           │
│  Use Set (not List) for @ManyToMany to avoid Hibernate bag issues.           │
│  Use CascadeType.PERSIST + MERGE (never ALL/REMOVE for shared entities).    │
│  Use @JsonIgnore on inverse side to prevent infinite recursion.              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

### How JPA Internally Manages the Join Table from the Owning Entity

JPA treats the owning side as the **single source of truth** for the `@ManyToMany` relationship. Every operation on the join table — INSERT, DELETE, full re-sync — is derived by comparing the owning entity's collection state **before and after** the transaction.

```text
┌─────────────────────────────────────────────────────────────────────────────────┐
│  JPA's Internal Flow for @ManyToMany (Owning Side = Order)                     │
│                                                                                 │
│  @Transactional method begins                                                   │
│       │                                                                         │
│       v                                                                         │
│  1. Load Order (owning entity)                                                  │
│     → Hibernate takes a SNAPSHOT of order.products = {P1, P3, P5}               │
│                                                                                 │
│  2. You modify the collection:                                                  │
│     order.removeProduct(P3);       → order.products = {P1, P5}                  │
│     order.addProduct(P7);          → order.products = {P1, P5, P7}              │
│                                                                                 │
│  3. Flush / Commit                                                              │
│     → Hibernate compares SNAPSHOT vs CURRENT:                                   │
│                                                                                 │
│     Snapshot:  {P1, P3, P5}                                                     │
│     Current:   {P1, P5, P7}                                                     │
│                                                                                 │
│     Diff:                                                                       │
│       REMOVED: P3  → DELETE FROM order_products WHERE order_id=10 AND product_id=3│
│       ADDED:   P7  → INSERT INTO order_products (order_id, product_id) VALUES (10,7)│
│       KEPT:    P1, P5 → no SQL (already in join table)                          │
│                                                                                 │
│  4. Join table is now in sync with the owning collection.                       │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Step-by-step code example showing exactly what JPA does**

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public void updateOrderProducts(Long orderId) {

        // STEP 1: Load owning entity — JPA snapshots the collection
        Order order = orderRepository.findById(orderId).orElseThrow();
        // SQL: SELECT o.* FROM orders o WHERE o.id = 10
        // SQL: SELECT p.* FROM products p
        //        JOIN order_products op ON p.id = op.product_id
        //        WHERE op.order_id = 10
        //
        // Snapshot taken: order.products = {Product(1), Product(3), Product(5)}

        // STEP 2: Modify the collection on the OWNING side
        Product p3 = order.getProducts().stream()
            .filter(p -> p.getId().equals(3L)).findFirst().orElseThrow();
        order.removeProduct(p3);              // remove P3

        Product p7 = productRepository.findById(7L).orElseThrow();
        order.addProduct(p7);                 // add P7

        // At this point IN MEMORY:
        //   order.products = {Product(1), Product(5), Product(7)}
        //   No SQL yet — changes are in Persistence Context only

        // STEP 3: Transaction commits → flush
        // JPA compares snapshot vs current and generates:
    }
    // After @Transactional method returns:
    // SQL: DELETE FROM order_products WHERE order_id = 10 AND product_id = 3
    // SQL: INSERT INTO order_products (order_id, product_id) VALUES (10, 7)
}
```

```text
Join table state:

BEFORE:                                   AFTER:
┌──────────┬────────────┐                ┌──────────┬────────────┐
│ order_id │ product_id │                │ order_id │ product_id │
├──────────┼────────────┤                ├──────────┼────────────┤
│    10    │     1      │   (kept)       │    10    │     1      │
│    10    │     3      │   (removed)    │    10    │     5      │
│    10    │     5      │   (kept)       │    10    │     7      │  (added)
└──────────┴────────────┘                └──────────┴────────────┘
```

---

**What JPA manages for each operation on the owning entity**

```text
┌─────────────────────────────────────┬──────────────────────────────────────────────────────┐
│ Operation on Owning Entity (Order)  │ What JPA does to the join table                      │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ order.addProduct(product)           │ INSERT INTO order_products (order_id, product_id)     │
│                                     │ VALUES (?, ?)                                        │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ order.removeProduct(product)        │ DELETE FROM order_products                            │
│                                     │ WHERE order_id = ? AND product_id = ?                │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ order.getProducts().clear()         │ DELETE FROM order_products WHERE order_id = ?         │
│                                     │ (all rows for this order removed)                    │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ order.setProducts(newSet)           │ DELETE all old rows + INSERT all new rows             │
│                                     │ (full re-sync of join table for this order)          │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ orderRepository.save(newOrder)      │ INSERT INTO orders + INSERT all join rows             │
│ (with products added)               │                                                      │
├─────────────────────────────────────┼──────────────────────────────────────────────────────┤
│ orderRepository.delete(order)       │ DELETE FROM order_products WHERE order_id = ?         │
│                                     │ DELETE FROM orders WHERE id = ?                      │
│                                     │ (join rows deleted FIRST to avoid FK violation)      │
│                                     │ Products are NOT deleted (independent entities)      │
└─────────────────────────────────────┴──────────────────────────────────────────────────────┘
```

---

**What JPA does NOT manage (inverse side changes)**

```java
@Transactional
public void inverseSideDemo(Long productId, Long orderId) {
    Product product = productRepository.findById(productId).orElseThrow();
    Order order = orderRepository.findById(orderId).orElseThrow();

    // Modifying INVERSE side only:
    product.getOrders().add(order);

    // Flush / Commit:
    // SQL generated: NOTHING!
    // JPA does not read the inverse side. The join table is UNCHANGED.
}
```

```text
Why JPA ignores the inverse side:

  mappedBy = "products" means:
    "The field 'products' in Order is the REAL owner."
    "I (Product.orders) am just a MIRROR."
    "Don't read me for persistence decisions."

  JPA's dirty checking for @ManyToMany ONLY compares:
    owning collection snapshot  vs  owning collection current state

  It NEVER compares the inverse collection.

  ┌───────────────────────────────┐     ┌───────────────────────────────┐
  │  Order.products (OWNING)      │     │  Product.orders (INVERSE)     │
  │                               │     │                               │
  │  Changed? → JPA generates SQL │     │  Changed? → JPA ignores it    │
  │  Snapshot tracked? → YES      │     │  Snapshot tracked? → NO       │
  │  Drives join table? → YES     │     │  Drives join table? → NO      │
  └───────────────────────────────┘     └───────────────────────────────┘
```

---

**Complete lifecycle managed by JPA from the owning entity**

```java
@Service
public class OrderLifecycleService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;

    // CREATE — JPA inserts order + join rows
    @Transactional
    public Order createOrderWithProducts(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        Order order = new Order();
        order.setOrderDate(LocalDate.now());
        products.forEach(order::addProduct);
        return orderRepository.save(order);
        // SQL: INSERT INTO orders ...
        // SQL: INSERT INTO order_products (order_id, product_id) VALUES (?, ?) × N
    }

    // READ — JPA loads order and lazily loads join + products
    @Transactional(readOnly = true)
    public Order getOrderWithProducts(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.getProducts().size();    // trigger lazy load
        return order;
        // SQL: SELECT o.* FROM orders WHERE id = ?
        // SQL: SELECT p.* FROM products JOIN order_products ON ... WHERE order_id = ?
    }

    // UPDATE — JPA diffs the collection and syncs join table
    @Transactional
    public Order replaceProducts(Long orderId, List<Long> newProductIds) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        // Snapshot: order.products = {P1, P3, P5}

        // Clear old (JPA will diff)
        order.getProducts().forEach(p -> p.getOrders().remove(order));
        order.getProducts().clear();

        // Add new
        List<Product> newProducts = productRepository.findAllById(newProductIds);
        newProducts.forEach(order::addProduct);
        // Current: order.products = {P2, P4}

        return order;
        // SQL: DELETE FROM order_products WHERE order_id = 10 AND product_id = 1
        // SQL: DELETE FROM order_products WHERE order_id = 10 AND product_id = 3
        // SQL: DELETE FROM order_products WHERE order_id = 10 AND product_id = 5
        // SQL: INSERT INTO order_products (order_id, product_id) VALUES (10, 2)
        // SQL: INSERT INTO order_products (order_id, product_id) VALUES (10, 4)
    }

    // DELETE — JPA removes join rows first, then order
    @Transactional
    public void deleteOrder(Long orderId) {
        orderRepository.deleteById(orderId);
        // SQL: DELETE FROM order_products WHERE order_id = 10  ← join rows first
        // SQL: DELETE FROM orders WHERE id = 10                ← then the order
        // Products are NOT deleted — they are independent entities
    }
}
```

```text
Full CRUD lifecycle — all managed by JPA from the owning entity:

  CREATE:  save(order with products)  → INSERT orders + INSERT join rows
  READ:    findById + access products → SELECT orders + SELECT via join table
  UPDATE:  modify products set        → DELETE old join rows + INSERT new join rows
  DELETE:  deleteById(orderId)        → DELETE join rows + DELETE order
                                         (products untouched)

  You never write SQL for the join table.
  You never create an @Entity for the join table.
  You never create a Repository for the join table.
  JPA handles it all — as long as you modify the OWNING side.
```


---

### Derived Query Methods in Spring Data JPA, When Repository Built-in Methods Are Not Enough. 

`JpaRepository` gives us built-in methods like `findAll()`, `findById()`, `save()`, `deleteById()`, etc. But these are generic — they operate on the **entire entity** without any filtering conditions.

```text
┌────────────────────────────────────────────────────────────────────────────────┐
│  Built-in JpaRepository methods:                                              │
│                                                                                │
│    findAll()      → SELECT * FROM employees                                    │
│    findById(1L)   → SELECT * FROM employees WHERE id = 1                       │
│    save(entity)   → INSERT / UPDATE                                            │
│    deleteById(1L) → DELETE FROM employees WHERE id = 1                         │
│                                                                                │
│  But what if we need:                                                          │
│    "Find employees whose salary is greater than 50000"                         │
│    "Find employees whose name starts with 'A' and department is 'IT'"          │
│    "Count employees in a given city"                                           │
│    "Check if an employee exists with a given email"                            │
│    "Delete employees whose status is 'INACTIVE'"                               │
│                                                                                │
│  No built-in method supports these WHERE-clause conditions!                    │
│                                                                                │
│  Solution → Derived Query Methods                                              │
│             Just declare a method in the repository interface                   │
│             with a specific naming convention. Spring Data JPA                  │
│             will DERIVE the SQL from the method name itself.                   │
│             No implementation needed. No SQL written.                           │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

### What Is a Derived Query and Why Is It Called So?

A **derived query** is a query that Spring Data JPA **derives** (generates) automatically from the **method name** you declare in the repository interface. You don't write any SQL, JPQL, or provide any implementation — the framework parses the method name, breaks it into tokens, maps them to entity properties and SQL operators, and generates the query at application startup.

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│  Why "Derived"?                                                              │
│                                                                              │
│  The query is DERIVED FROM the method name.                                  │
│                                                                              │
│  Method name:  findByNameAndSalaryGreaterThan(String name, Double salary)    │
│                  │      │       │                                             │
│                  │      │       └─ DERIVED operator: >                        │
│                  │      └────────── DERIVED property: salary                  │
│                  └───────────────── DERIVED property: name + AND combinator   │
│                                                                              │
│  DERIVED SQL:   SELECT * FROM employees                                      │
│                 WHERE name = ? AND salary > ?                                 │
│                                                                              │
│  You wrote: just a method signature in the interface.                        │
│  Spring wrote: the full SQL, the parameter binding, the result mapping.      │
└──────────────────────────────────────────────────────────────────────────────┘
```

It is called **derived** because:
1. The **query** is derived from the **method name**
2. The **parameters** are derived from the **method arguments**
3. The **return type** tells Spring what shape the result should be (single entity, list, page, count, boolean)
4. You never write the implementation — Spring's proxy generates it at startup by parsing your method name using `PartTree.java`

---

### How Spring Data JPA Parses the Method Name — PartTree.java and Part.java

When your application starts, Spring Data JPA creates a **proxy** for your repository interface. For each derived query method, it feeds the method name into `PartTree.java` which is a **parser class** in `org.springframework.data.repository.query.parser`.

#### PartTree.java — The Method Name Parser

`PartTree` parses the full method name into a tree structure of parts. It uses regular expressions to identify the **subject** (find/count/exists/delete), split the **predicate** by `Or` and `And`, and extract the **OrderBy** clause.

**Key constants and regex from the actual source code:**

```java
// PartTree.java — Spring Data Commons source

// Keyword template used to split at camelCase boundaries after a keyword
private static final String KEYWORD_TEMPLATE = "(%s)(?=(\\p{Lu}|\\P{InBASIC_LATIN}))";

// Subject patterns — what kind of operation is this?
private static final String QUERY_PATTERN  = "find|read|get|query|search|stream";
private static final String COUNT_PATTERN  = "count";
private static final String EXISTS_PATTERN = "exists";
private static final String DELETE_PATTERN = "delete|remove";

// The master regex — matches the FULL prefix up to "By"
private static final Pattern PREFIX_TEMPLATE = Pattern.compile(
    "^(" + QUERY_PATTERN + "|" + COUNT_PATTERN + "|" + EXISTS_PATTERN + "|"
        + DELETE_PATTERN + ")((\\p{Lu}.*?))??By"
);

// Subject-specific patterns
private static final Pattern COUNT_BY_TEMPLATE   = Pattern.compile("^count(\\p{Lu}.*?)??By");
private static final Pattern EXISTS_BY_TEMPLATE  = Pattern.compile("^(" + EXISTS_PATTERN + ")(\\p{Lu}.*?)??By");
private static final Pattern DELETE_BY_TEMPLATE  = Pattern.compile("^(" + DELETE_PATTERN + ")(\\p{Lu}.*?)??By");

// Top/First limiting pattern — e.g., findFirst5By, findTop3By
private static final String LIMITING_QUERY_PATTERN = "(First|Top)(\\d*)?";
private static final Pattern LIMITED_QUERY_TEMPLATE = Pattern.compile(
    "^(" + QUERY_PATTERN + ")(" + "Distinct" + ")?" + LIMITING_QUERY_PATTERN + "(\\p{Lu}.*?)??By"
);
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  PREFIX_TEMPLATE regex (expanded):                                               │
│                                                                                  │
│  ^(find|read|get|query|search|stream|count|exists|delete|remove)                 │
│   ((\p{Lu}.*?))??By                                                              │
│                                                                                  │
│  This means:                                                                     │
│    ^ → start of method name                                                      │
│    (find|read|...|remove) → one of the allowed operation keywords                │
│    ((\p{Lu}.*?))?? → optional entity name (e.g., "User" in findUserBy)           │
│                       \p{Lu} = uppercase Unicode letter                           │
│                       .*?    = lazy match for rest of entity name                 │
│                       ??     = possessive optional (prefer empty match)           │
│    By → the literal keyword "By" that separates subject from predicate           │
│                                                                                  │
│  Example matches:                                                                │
│    findBy...            → QUERY, no entity hint                                  │
│    findEmployeeBy...    → QUERY, entity hint = "Employee"                        │
│    countBy...           → COUNT                                                  │
│    existsBy...          → EXISTS                                                 │
│    deleteBy...          → DELETE                                                 │
│    removeBy...          → DELETE                                                 │
│    readBy...            → QUERY                                                  │
│    queryBy...           → QUERY                                                  │
│    searchBy...          → QUERY                                                  │
│    streamBy...          → QUERY (returns Stream<T>)                              │
│    streamAllBy...       → QUERY                                                  │
│    findDistinctBy...    → QUERY with DISTINCT                                    │
│    findFirst5By...      → QUERY with LIMIT 5                                     │
│    findTop3By...        → QUERY with LIMIT 3                                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**How PartTree parses a method name step by step:**

```text
Method name: findByNameAndSalaryGreaterThanOrderByNameAsc

STEP 1: Match PREFIX_TEMPLATE → "findBy"
         subject = "find" → QUERY operation
         remainder after "By" = "NameAndSalaryGreaterThanOrderByNameAsc"

STEP 2: Split at "OrderBy" →
         predicatePart = "NameAndSalaryGreaterThan"
         orderByPart   = "NameAsc"

STEP 3: Split predicatePart at "Or" →
         orPart[0] = "NameAndSalaryGreaterThan"
         (No "Or" found, so just one OrPart)

STEP 4: Split each OrPart at "And" →
         andPart[0] = "Name"              → Part(Name, SIMPLE_PROPERTY)
         andPart[1] = "SalaryGreaterThan" → Part(Salary, GREATER_THAN)

STEP 5: Parse OrderBy →
         "NameAsc" → Sort by "name" ASC

STEP 6: Build the query tree:

  PartTree
  ├── Subject: QUERY (find)
  ├── Predicate
  │   └── OrPart[0]
  │       ├── Part(name, SIMPLE_PROPERTY, 1 arg)  → WHERE name = ?
  │       └── Part(salary, GREATER_THAN, 1 arg)   → AND salary > ?
  └── OrderBy: name ASC

  Generated SQL:
    SELECT * FROM employees
    WHERE name = ?1 AND salary > ?2
    ORDER BY name ASC
```

#### Part.java — Individual Condition Parser

Each condition token (e.g., `NameStartingWith`, `SalaryGreaterThan`, `AgeIsNull`) is parsed by `Part.java`. It inspects the token string against an ordered list of keyword `Type` enums and extracts the **property name** and **operator type**.

**Part.Type enum — ALL available keywords from the actual source code:**

```text
┌──────────────────────────┬──────────────────────────────────┬──────────┬──────────────────────────┐
│ Type (Enum Constant)     │ Keywords (in method name)        │ # Args   │ SQL Equivalent           │
├──────────────────────────┼──────────────────────────────────┼──────────┼──────────────────────────┤
│ BETWEEN                  │ IsBetween, Between               │ 2        │ BETWEEN ? AND ?          │
│ IS_NOT_NULL              │ IsNotNull, NotNull               │ 0        │ IS NOT NULL              │
│ IS_NULL                  │ IsNull, Null                     │ 0        │ IS NULL                  │
│ LESS_THAN                │ IsLessThan, LessThan             │ 1        │ < ?                      │
│ LESS_THAN_EQUAL          │ IsLessThanEqual, LessThanEqual   │ 1        │ <= ?                     │
│ GREATER_THAN             │ IsGreaterThan, GreaterThan       │ 1        │ > ?                      │
│ GREATER_THAN_EQUAL       │ IsGreaterThanEqual,              │ 1        │ >= ?                     │
│                          │ GreaterThanEqual                 │          │                          │
│ BEFORE                   │ IsBefore, Before                 │ 1        │ < ? (for dates)          │
│ AFTER                    │ IsAfter, After                   │ 1        │ > ? (for dates)          │
│ NOT_LIKE                 │ IsNotLike, NotLike               │ 1        │ NOT LIKE ?               │
│ LIKE                     │ IsLike, Like                     │ 1        │ LIKE ?                   │
│ STARTING_WITH            │ IsStartingWith, StartingWith,    │ 1        │ LIKE ?%                  │
│                          │ StartsWith                       │          │                          │
│ ENDING_WITH              │ IsEndingWith, EndingWith,        │ 1        │ LIKE %?                  │
│                          │ EndsWith                         │          │                          │
│ IS_NOT_EMPTY             │ IsNotEmpty, NotEmpty             │ 0        │ <> '' (collections)      │
│ IS_EMPTY                 │ IsEmpty, Empty                   │ 0        │ = '' (collections)       │
│ NOT_CONTAINING           │ IsNotContaining, NotContaining,  │ 1        │ NOT LIKE %?%             │
│                          │ NotContains                      │          │                          │
│ CONTAINING               │ IsContaining, Containing,        │ 1        │ LIKE %?%                 │
│                          │ Contains                         │          │                          │
│ NOT_IN                   │ IsNotIn, NotIn                   │ 1        │ NOT IN (?)               │
│ IN                       │ IsIn, In                         │ 1        │ IN (?)                   │
│ NEAR                     │ IsNear, Near                     │ 1        │ (geo-spatial)            │
│ WITHIN                   │ IsWithin, Within                 │ 1        │ (geo-spatial)            │
│ REGEX                    │ MatchesRegex, Matches, Regex     │ 1        │ REGEXP ?                 │
│ EXISTS                   │ Exists                           │ 0        │ EXISTS (subquery)        │
│ TRUE                     │ IsTrue, True                     │ 0        │ = TRUE                   │
│ FALSE                    │ IsFalse, False                   │ 0        │ = FALSE                  │
│ NEGATING_SIMPLE_PROPERTY │ IsNot, Not                       │ 1        │ <> ?                     │
│ SIMPLE_PROPERTY          │ Is, Equals                       │ 1        │ = ?                      │
│ (default if no keyword)  │ (property name only)             │ 1        │ = ?                      │
└──────────────────────────┴──────────────────────────────────┴──────────┴──────────────────────────┘

NOTE: The order in the ALL list matters! PartTree iterates in this exact order.
      e.g., "IsNotNull" is checked BEFORE "IsNull" so "NameIsNotNull" doesn't
      accidentally match "NameIsNot" + "Null" as property.
```

**Additional keywords for the method structure (not Part.Type but PartTree-level):**

```text
┌────────────────────┬─────────────────────────────────────────────────────────┐
│ Keyword            │ Purpose                                                 │
├────────────────────┼─────────────────────────────────────────────────────────┤
│ By                 │ Separator between subject (find/count/exists/delete)    │
│                    │ and predicate (conditions). MANDATORY.                  │
├────────────────────┼─────────────────────────────────────────────────────────┤
│ And                │ Combines two conditions with SQL AND                    │
│                    │ e.g., NameAndAge → WHERE name = ? AND age = ?           │
├────────────────────┼─────────────────────────────────────────────────────────┤
│ Or                 │ Combines two conditions with SQL OR                     │
│                    │ e.g., NameOrEmail → WHERE name = ? OR email = ?         │
├────────────────────┼─────────────────────────────────────────────────────────┤
│ OrderBy            │ Adds ORDER BY clause                                    │
│                    │ e.g., OrderByNameAsc → ORDER BY name ASC                │
├────────────────────┼─────────────────────────────────────────────────────────┤
│ Asc / Desc         │ Sort direction (appended after property in OrderBy)     │
│                    │ e.g., OrderByNameAscAgeDesc → ORDER BY name ASC, age DESC│
├────────────────────┼─────────────────────────────────────────────────────────┤
│ Distinct           │ Adds SELECT DISTINCT                                    │
│                    │ e.g., findDistinctByName → SELECT DISTINCT ...          │
├────────────────────┼─────────────────────────────────────────────────────────┤
│ First / Top        │ Limits results (with optional count)                    │
│                    │ e.g., findFirst5By → LIMIT 5, findTopBy → LIMIT 1      │
├────────────────────┼─────────────────────────────────────────────────────────┤
│ AllIgnoreCase /    │ Apply case-insensitive matching to ALL or single        │
│ IgnoreCase         │ conditions. e.g., findByNameIgnoreCase                  │
│ / IgnoringCase     │ → WHERE LOWER(name) = LOWER(?)                         │
└────────────────────┴─────────────────────────────────────────────────────────┘
```

---

### Method Name Structure

```text
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Method Name Structure for Derived Query:                                            │
│                                                                                      │
│  ┌─────────┐ ┌──────────┐ ┌────┐ ┌──────────────────┐ ┌────────────────────────────┐│
│  │ Subject  │ │ Optional │ │ By │ │    Predicate      │ │ Optional OrderBy           ││
│  │ Keyword  │ │ Modifiers│ │    │ │    (conditions)   │ │                            ││
│  └────┬─────┘ └────┬─────┘ └─┬──┘ └────────┬─────────┘ └───────────┬────────────────┘│
│       │             │         │              │                       │                 │
│       v             v         v              v                       v                 │
│  find/read/    Distinct    By       Property+Operator+        OrderBy+Property+       │
│  get/query/    First5              And/Or+Property+Operator    Asc/Desc                │
│  search/stream Top3                                                                    │
│  count                                                                                 │
│  exists                                                                                │
│  delete/remove                                                                         │
│                                                                                        │
│  Examples:                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │ find         │          │ By │ Name                                             │   │
│  │ find         │          │ By │ NameAndSalaryGreaterThan                         │   │
│  │ find         │Distinct  │ By │ DepartmentContaining │ OrderByNameAsc            │   │
│  │ find         │First5    │ By │ StatusTrue           │ OrderBySalaryDesc         │   │
│  │ count        │          │ By │ DepartmentAndActive                              │   │
│  │ exists       │          │ By │ Email                                            │   │
│  │ delete       │          │ By │ StatusAndLastLoginBefore                         │   │
│  │ read         │          │ By │ AgeBetween                                       │   │
│  │ stream       │All       │ By │ DepartmentIn                                     │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### How Method Name + Arguments + Return Type Create the Dynamic Query

Spring Data JPA combines three things to build the full query:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  THREE INPUTS that Spring uses to build the query:                               │
│                                                                                  │
│  1. METHOD NAME    → determines the SQL structure                                │
│     findByNameAndSalaryGreaterThan → SELECT ... WHERE name = ? AND salary > ?    │
│                                                                                  │
│  2. METHOD ARGUMENTS → bound to the ? placeholders (positional order)            │
│     (String name, Double salary) → ?1 = name, ?2 = salary                        │
│                                                                                  │
│  3. RETURN TYPE    → determines how to shape the result                          │
│     List<Employee>  → return multiple rows as a List                              │
│     Employee        → return single row (or null / throw if not found)           │
│     Optional<Employee> → return single row wrapped in Optional                   │
│     Page<Employee>  → return a page with pagination metadata                     │
│     Slice<Employee> → return a slice (knows if more pages exist)                 │
│     Stream<Employee> → return as Java Stream (must be closed)                    │
│     Long / long     → for count queries                                          │
│     Boolean / boolean → for exists queries                                       │
│     void            → for delete queries                                         │
│     Long            → for delete queries (returns count of deleted rows)         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Complete code example with Entity, Repository, Controller, and generated SQL:**

```java
// Entity
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String department;
    private Double salary;
    private Boolean active;
    private LocalDate joiningDate;
    // getters, setters, constructors
}
```

```java
// Repository — all methods are derived queries, NO implementation needed
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // 1. SIMPLE PROPERTY — WHERE name = ?
    List<Employee> findByName(String name);
    // SQL: SELECT * FROM employees WHERE name = ?1

    // 2. AND combinator — WHERE department = ? AND active = ?
    List<Employee> findByDepartmentAndActive(String department, Boolean active);
    // SQL: SELECT * FROM employees WHERE department = ?1 AND active = ?2

    // 3. OR combinator — WHERE name = ? OR email = ?
    List<Employee> findByNameOrEmail(String name, String email);
    // SQL: SELECT * FROM employees WHERE name = ?1 OR email = ?2

    // 4. GREATER_THAN — WHERE salary > ?
    List<Employee> findBySalaryGreaterThan(Double minSalary);
    // SQL: SELECT * FROM employees WHERE salary > ?1

    // 5. BETWEEN — WHERE salary BETWEEN ? AND ?  (takes 2 args)
    List<Employee> findBySalaryBetween(Double min, Double max);
    // SQL: SELECT * FROM employees WHERE salary BETWEEN ?1 AND ?2

    // 6. LIKE — WHERE name LIKE ?  (caller must include % wildcards)
    List<Employee> findByNameLike(String pattern);
    // SQL: SELECT * FROM employees WHERE name LIKE ?1

    // 7. STARTING_WITH — WHERE name LIKE 'A%'  (Spring adds the %)
    List<Employee> findByNameStartingWith(String prefix);
    // SQL: SELECT * FROM employees WHERE name LIKE ?1 || '%'
    //      (or: WHERE name LIKE 'A%' — Spring wraps the param)

    // 8. CONTAINING — WHERE department LIKE '%eng%'
    List<Employee> findByDepartmentContaining(String keyword);
    // SQL: SELECT * FROM employees WHERE department LIKE '%' || ?1 || '%'

    // 9. IS_NULL — WHERE email IS NULL  (no argument needed)
    List<Employee> findByEmailIsNull();
    // SQL: SELECT * FROM employees WHERE email IS NULL

    // 10. IS_NOT_NULL — WHERE email IS NOT NULL
    List<Employee> findByEmailIsNotNull();
    // SQL: SELECT * FROM employees WHERE email IS NOT NULL

    // 11. IN — WHERE department IN ('IT', 'HR', 'Finance')
    List<Employee> findByDepartmentIn(List<String> departments);
    // SQL: SELECT * FROM employees WHERE department IN (?1, ?2, ?3)

    // 12. TRUE / FALSE — WHERE active = TRUE
    List<Employee> findByActiveTrue();
    // SQL: SELECT * FROM employees WHERE active = TRUE

    // 13. ORDER BY — WHERE department = ? ORDER BY salary DESC
    List<Employee> findByDepartmentOrderBySalaryDesc(String department);
    // SQL: SELECT * FROM employees WHERE department = ?1 ORDER BY salary DESC

    // 14. BEFORE (for dates) — WHERE joiningDate < ?
    List<Employee> findByJoiningDateBefore(LocalDate date);
    // SQL: SELECT * FROM employees WHERE joining_date < ?1

    // 15. AFTER (for dates) — WHERE joiningDate > ?
    List<Employee> findByJoiningDateAfter(LocalDate date);
    // SQL: SELECT * FROM employees WHERE joining_date > ?1

    // 16. NEGATING — WHERE department <> ?
    List<Employee> findByDepartmentNot(String department);
    // SQL: SELECT * FROM employees WHERE department <> ?1

    // 17. IGNORE CASE — WHERE LOWER(name) = LOWER(?)
    List<Employee> findByNameIgnoreCase(String name);
    // SQL: SELECT * FROM employees WHERE LOWER(name) = LOWER(?1)

    // 18. COUNT — SELECT COUNT(*) WHERE department = ?
    long countByDepartment(String department);
    // SQL: SELECT COUNT(*) FROM employees WHERE department = ?1

    // 19. EXISTS — SELECT CASE WHEN COUNT > 0 THEN TRUE ELSE FALSE
    boolean existsByEmail(String email);
    // SQL: SELECT CASE WHEN COUNT(e.id) > 0 THEN TRUE ELSE FALSE END
    //      FROM employees e WHERE e.email = ?1

    // 20. DELETE — DELETE WHERE status = ?
    void deleteByActive(Boolean active);
    // SQL: SELECT * FROM employees WHERE active = ?1  (loads first)
    //      DELETE FROM employees WHERE id = ?  (then deletes one by one)

    // 21. DISTINCT — SELECT DISTINCT department FROM employees WHERE active = ?
    List<Employee> findDistinctByActiveTrue();
    // SQL: SELECT DISTINCT * FROM employees WHERE active = TRUE

    // 22. First/Top — LIMIT results
    List<Employee> findFirst5BySalaryGreaterThanOrderBySalaryDesc(Double salary);
    // SQL: SELECT * FROM employees WHERE salary > ?1 ORDER BY salary DESC LIMIT 5

    // 23. Single result — returns Optional
    Optional<Employee> findByEmail(String email);
    // SQL: SELECT * FROM employees WHERE email = ?1  (expects 0 or 1 row)

    // 24. COMPLEX — multiple conditions + ordering
    List<Employee> findByDepartmentAndSalaryGreaterThanAndActiveTrueOrderByNameAsc(
        String department, Double salary
    );
    // SQL: SELECT * FROM employees
    //      WHERE department = ?1 AND salary > ?2 AND active = TRUE
    //      ORDER BY name ASC
}
```

```text
How argument binding works — positional order:

  Method: findByNameAndSalaryBetweenAndActiveTrue(String name, Double min, Double max)
                    │              │          │                 │           │        │
                    │              │          │                 │           │        │
  Argument #:      (1)           (2)        (3)               ─┘          ─┘       ─┘
                    │              │          │              no arg       no arg    no arg
                    v              v          v            (0 args)     (0 args)  (0 args)
  SQL: WHERE name = ?1 AND salary BETWEEN ?2 AND ?3 AND active = TRUE

  Rules:
  - Arguments are bound LEFT TO RIGHT
  - 0-arg types (IsNull, IsNotNull, True, False) consume no arguments
  - BETWEEN consumes 2 arguments
  - All others consume 1 argument
  - The argument count MUST match the total required by all Part.Types
  - If mismatch → startup error: "Failed to create query for method..."
```

**Controller + Service using derived queries:**

```java
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/by-department")
    public List<Employee> getByDepartment(@RequestParam String department) {
        return employeeService.getByDepartment(department);
    }

    @GetMapping("/high-earners")
    public List<Employee> getHighEarners(@RequestParam Double minSalary) {
        return employeeService.getHighEarners(minSalary);
    }

    @GetMapping("/count-by-department")
    public long countByDepartment(@RequestParam String department) {
        return employeeService.countByDepartment(department);
    }

    @GetMapping("/exists-by-email")
    public boolean existsByEmail(@RequestParam String email) {
        return employeeService.existsByEmail(email);
    }
}

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getByDepartment(String department) {
        return employeeRepository.findByDepartmentAndActiveTrue(department);
        // SQL: SELECT * FROM employees WHERE department = 'IT' AND active = TRUE
    }

    public List<Employee> getHighEarners(Double minSalary) {
        return employeeRepository.findBySalaryGreaterThanOrderBySalaryDesc(minSalary);
        // SQL: SELECT * FROM employees WHERE salary > 50000 ORDER BY salary DESC
    }

    public long countByDepartment(String department) {
        return employeeRepository.countByDepartment(department);
        // SQL: SELECT COUNT(*) FROM employees WHERE department = 'IT'
    }

    public boolean existsByEmail(String email) {
        return employeeRepository.existsByEmail(email);
        // SQL: SELECT CASE WHEN COUNT(e.id) > 0 THEN TRUE ELSE FALSE END
        //      FROM employees e WHERE e.email = 'john@example.com'
    }
}
```

```text
Complete flow — from method call to SQL:

  Controller calls:  employeeRepository.findByDepartmentAndSalaryGreaterThan("IT", 50000)
       │
       v
  Spring Proxy intercepts the call
       │
       v
  PartTree already parsed at startup:
    Subject:  QUERY (find)
    Predicate:
      OrPart[0]:
        Part(department, SIMPLE_PROPERTY, 1 arg)   → AND department = ?1
        Part(salary, GREATER_THAN, 1 arg)          → AND salary > ?2
       │
       v
  JPA Criteria Query / JPQL is built:
    SELECT e FROM Employee e WHERE e.department = :param0 AND e.salary > :param1
       │
       v
  Hibernate translates to SQL:
    SELECT e.id, e.name, e.email, e.department, e.salary, e.active, e.joining_date
    FROM employees e
    WHERE e.department = 'IT' AND e.salary > 50000
       │
       v
  JDBC executes → ResultSet → mapped to List<Employee> → returned to Controller
```

---

### Why Derived Queries Support GET/REMOVE Only — Not INSERT/UPDATE

Derived queries can only be used for **read (SELECT)** and **delete (DELETE)** operations. You **cannot** create a derived query for INSERT or UPDATE.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Supported:                                                                      │
│    find/read/get/query/search/stream + By  → SELECT query                        │
│    count + By                              → SELECT COUNT query                   │
│    exists + By                             → SELECT EXISTS query                  │
│    delete/remove + By                      → DELETE operation                     │
│                                                                                  │
│  NOT Supported:                                                                  │
│    insert + By     → ✗ No such prefix in PartTree                                │
│    update + By     → ✗ No such prefix in PartTree                                │
│    save + By       → ✗ No such prefix in PartTree                                │
│    merge + By      → ✗ No such prefix in PartTree                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Reason 1 — PartTree's PREFIX_TEMPLATE simply doesn't include insert/update:**

```java
// From PartTree.java source:
private static final String QUERY_PATTERN  = "find|read|get|query|search|stream";
private static final String COUNT_PATTERN  = "count";
private static final String EXISTS_PATTERN = "exists";
private static final String DELETE_PATTERN = "delete|remove";

// COMBINED regex:
"^(" + QUERY_PATTERN + "|" + COUNT_PATTERN + "|" + EXISTS_PATTERN + "|" + DELETE_PATTERN + ")..."

// There is NO "insert|save" or "update|modify" pattern!
// If you write insertByName(...) → PartTree won't match → startup error.
```

**Reason 2 — Semantic mismatch: derived queries describe WHERE clauses, not SET clauses:**

```text
A derived method name describes WHAT to filter:
  findByNameAndDepartment(name, dept)  →  WHERE name = ? AND department = ?
       ↑ describes the WHERE clause

For UPDATE, you would need to express BOTH:
  - WHAT to update (SET clause) — which columns, what values
  - WHERE to apply (WHERE clause) — which rows to target

  UPDATE employees SET salary = ?, department = ? WHERE name = ? AND active = ?
                       ↑ SET clause                     ↑ WHERE clause

  A single method name cannot cleanly express both SET and WHERE.
  "updateSalaryAndDepartmentByNameAndActive" → ambiguous!
  Does "Salary" go to SET or WHERE? There's no keyword to distinguish them.

For INSERT, there is no WHERE clause at all:
  INSERT INTO employees (name, email, salary) VALUES (?, ?, ?)
  There's nothing to "derive" — you just need the full entity.
  That's exactly what save() does.
```

**Reason 3 — JPA's EntityManager design for writes:**

```text
JPA manages writes through the Persistence Context:

  INSERT → entityManager.persist(entity)  → save() method
  UPDATE → entityManager.merge(entity)    → save() method (if entity has ID)
           OR dirty checking (modify managed entity, flush auto-applies)

  Both INSERT and UPDATE need the FULL entity object (or at least the fields to set).
  A derived query method with just a WHERE clause can't express what values to write.

  DELETE works because:
    DELETE only needs a WHERE clause — "which rows to remove"
    findByDepartment(dept)   → SELECT ... WHERE dept = ?  → returns entities
    deleteByDepartment(dept) → DELETE ... WHERE dept = ?   → removes those rows
    Same WHERE clause structure — just different operation.

  So the query derivation mechanism maps perfectly to SELECT and DELETE
  but NOT to INSERT (no WHERE) or UPDATE (needs SET + WHERE).
```

**What to use for INSERT/UPDATE instead:**

```text
┌──────────────────────┬─────────────────────────────────────────────────────────┐
│ Operation            │ How to do it                                            │
├──────────────────────┼─────────────────────────────────────────────────────────┤
│ INSERT               │ repository.save(newEntity)                              │
│                      │ (built-in JpaRepository method)                         │
├──────────────────────┼─────────────────────────────────────────────────────────┤
│ UPDATE (full entity) │ repository.save(existingEntity)                         │
│                      │ (if entity has ID → merge instead of persist)           │
├──────────────────────┼─────────────────────────────────────────────────────────┤
│ UPDATE (partial /    │ @Modifying @Query("UPDATE Employee e SET e.salary =     │
│ bulk)                │ :salary WHERE e.department = :dept")                    │
│                      │ int updateSalary(@Param("salary") Double salary,       │
│                      │                  @Param("dept") String dept);          │
│                      │ (custom JPQL with @Query annotation — next topic)       │
└──────────────────────┴─────────────────────────────────────────────────────────┘
```

---

### Why @Transactional Is Required on Derived Delete Methods

When you declare a derived delete method like `deleteByDepartment(String department)`, you **must** annotate the calling service method with `@Transactional`. Without it, you get an error.

**How derived delete actually works internally:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Derived delete does NOT generate a direct "DELETE FROM ... WHERE ..." SQL!      │
│                                                                                  │
│  It is a TWO-STEP process:                                                       │
│                                                                                  │
│  STEP 1: SELECT — find all matching entities                                     │
│    SQL: SELECT e.* FROM employees e WHERE e.department = 'INACTIVE'              │
│    → Returns List<Employee> into the Persistence Context                         │
│                                                                                  │
│  STEP 2: DELETE — remove each entity one by one via EntityManager.remove()       │
│    SQL: DELETE FROM employees WHERE id = 1                                       │
│    SQL: DELETE FROM employees WHERE id = 5                                       │
│    SQL: DELETE FROM employees WHERE id = 9                                       │
│    → Each entity is removed individually (lifecycle callbacks fire)              │
│                                                                                  │
│  This is BY DESIGN — so that:                                                    │
│    - @PreRemove / @PostRemove lifecycle callbacks fire                           │
│    - Cascade operations (CascadeType.REMOVE) propagate to children              │
│    - Orphan removal triggers                                                     │
│    - L1 cache stays consistent (entities transition to REMOVED state)            │
│                                                                                  │
│  But this also means:                                                            │
│    - It needs a TRANSACTION for the SELECT + multiple DELETEs                    │
│    - Without @Transactional → TransactionRequiredException                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why it needs @Transactional specifically:**

```text
Reason 1: Multiple SQL statements must be atomic
  ─────────────────────────────────────────────
  The derived delete runs: SELECT + DELETE + DELETE + DELETE + ...
  If any DELETE fails, ALL should roll back.
  Without @Transactional, each SQL runs in its own auto-commit.
  → Partial deletion = data inconsistency.

Reason 2: EntityManager.remove() requires an active transaction
  ─────────────────────────────────────────────
  The Persistence Context must be in "transaction" mode to call remove().
  Without @Transactional → javax.persistence.TransactionRequiredException:
      "No EntityManager with actual transaction available for current thread"

Reason 3: Flush + Commit
  ─────────────────────────────────────────────
  The DELETE SQL statements are sent to the DB during flush (before commit).
  Flush only happens within a transaction boundary.
  Without @Transactional → no flush → no SQL sent → nothing deleted.
```

**Code example:**

```java
// Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    void deleteByDepartment(String department);
    long deleteByActivefalse();  // returns count of deleted rows
    List<Employee> removeByDepartment(String department);  // returns deleted entities
}

// Service — MUST have @Transactional
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Transactional   // ← MANDATORY for derived delete
    public void removeInactiveDepartment(String department) {
        employeeRepository.deleteByDepartment(department);
        // Internally:
        // SQL 1: SELECT * FROM employees WHERE department = 'OLD_DEPT'
        //        → loads [Employee(id=1), Employee(id=5), Employee(id=9)]
        // SQL 2: DELETE FROM employees WHERE id = 1
        // SQL 3: DELETE FROM employees WHERE id = 5
        // SQL 4: DELETE FROM employees WHERE id = 9
        // All within one transaction — commit or rollback together
    }

    // WITHOUT @Transactional → RUNTIME ERROR:
    // org.springframework.dao.InvalidDataAccessApiUsageException:
    //   No EntityManager with actual transaction available for current thread
    //   - cannot reliably process 'remove' call
}
```

```text
Note: READ operations (find/get/read) do NOT require @Transactional because:
  - SELECT queries don't modify data
  - They run in auto-commit mode by default
  - No flush/commit needed

  COUNT and EXISTS also don't need @Transactional (they are SELECT queries).

  Only DELETE/REMOVE derived queries need @Transactional because they
  modify data via EntityManager.remove() in a multi-step process.

  ┌──────────────┬──────────────────┐
  │ Operation    │ @Transactional?  │
  ├──────────────┼──────────────────┤
  │ findBy...    │ Not required     │
  │ countBy...   │ Not required     │
  │ existsBy...  │ Not required     │
  │ deleteBy...  │ REQUIRED ✓       │
  │ removeBy...  │ REQUIRED ✓       │
  └──────────────┴──────────────────┘
```

---

### Pagination in Derived Queries Using Pageable

Instead of loading thousands of rows into memory, you can pass a `Pageable` parameter to any derived query method. Spring Data JPA will append `LIMIT` and `OFFSET` to the generated SQL.

**Conventions:**

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│  Pagination Conventions:                                                           │
│                                                                                    │
│  1. Add Pageable as the LAST parameter of the method                               │
│  2. Return type can be:                                                            │
│     - Page<T>  → includes total count, total pages, page number, content          │
│     - Slice<T> → lighter than Page, knows hasNext but NOT total count             │
│     - List<T>  → just the content, no metadata                                   │
│                                                                                    │
│  3. Pageable is created using PageRequest.of(page, size)                          │
│     - page → 0-INDEXED (first page = 0, second page = 1, ...)                    │
│     - size → number of records per page                                            │
│                                                                                    │
│  4. Page<T> triggers an EXTRA COUNT query to calculate total:                     │
│     - Query 1: SELECT * FROM ... WHERE ... LIMIT ? OFFSET ?                      │
│     - Query 2: SELECT COUNT(*) FROM ... WHERE ...                                │
│                                                                                    │
│  5. Slice<T> does NOT trigger the count query (more efficient):                   │
│     - Only: SELECT * FROM ... WHERE ... LIMIT (size+1) OFFSET ?                  │
│     - Fetches one extra row to determine hasNext                                  │
│                                                                                    │
│  6. List<T> does NOT trigger the count query either:                              │
│     - Only: SELECT * FROM ... WHERE ... LIMIT ? OFFSET ?                         │
└────────────────────────────────────────────────────────────────────────────────────┘
```

```text
When to use Page vs Slice vs List:

  ┌────────────┬───────────────────────┬─────────────────────────────────────────────┐
  │ Return     │ Extra COUNT query?    │ Use when                                    │
  ├────────────┼───────────────────────┼─────────────────────────────────────────────┤
  │ Page<T>    │ YES (extra SQL)       │ UI needs "Page 3 of 12" or total count.    │
  │            │                       │ e.g., admin dashboard, data table.          │
  ├────────────┼───────────────────────┼─────────────────────────────────────────────┤
  │ Slice<T>   │ NO                    │ "Load More" button or infinite scroll.      │
  │            │                       │ Only need to know if more data exists.      │
  ├────────────┼───────────────────────┼─────────────────────────────────────────────┤
  │ List<T>    │ NO                    │ Internal use where you just need the data.  │
  │            │                       │ No pagination metadata needed.              │
  └────────────┴───────────────────────┴─────────────────────────────────────────────┘
```

**Complete code example:**

```java
// Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Paginated derived query — returns Page with full metadata
    Page<Employee> findByDepartment(String department, Pageable pageable);
    // SQL 1: SELECT * FROM employees WHERE department = ?1 LIMIT ? OFFSET ?
    // SQL 2: SELECT COUNT(*) FROM employees WHERE department = ?1

    // Paginated with additional condition
    Page<Employee> findByDepartmentAndSalaryGreaterThan(
        String department, Double salary, Pageable pageable
    );
    // SQL 1: SELECT * FROM employees WHERE department = ?1 AND salary > ?2 LIMIT ? OFFSET ?
    // SQL 2: SELECT COUNT(*) FROM employees WHERE department = ?1 AND salary > ?2

    // Slice — no count query
    Slice<Employee> findByActiveTrue(Pageable pageable);
    // SQL: SELECT * FROM employees WHERE active = TRUE LIMIT (size+1) OFFSET ?

    // List — just data
    List<Employee> findByDepartmentContaining(String keyword, Pageable pageable);
    // SQL: SELECT * FROM employees WHERE department LIKE '%keyword%' LIMIT ? OFFSET ?
}
```

```java
// Controller
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/by-department")
    public Page<Employee> getByDepartment(
        @RequestParam String department,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return employeeService.getByDepartment(department, page, size);
    }

    @GetMapping("/active")
    public Slice<Employee> getActiveEmployees(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return employeeService.getActiveEmployees(page, size);
    }
}

// Service
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public Page<Employee> getByDepartment(String department, int page, int size) {

        // Create Pageable: page 0 = first page, size 10 = 10 records per page
        Pageable pageable = PageRequest.of(page, size);

        return employeeRepository.findByDepartment(department, pageable);
        // Two SQL queries executed:
        //
        // SQL 1 (data):
        //   SELECT e.id, e.name, e.email, e.department, e.salary, e.active, e.joining_date
        //   FROM employees e
        //   WHERE e.department = 'IT'
        //   LIMIT 10 OFFSET 0
        //
        // SQL 2 (count — because return type is Page):
        //   SELECT COUNT(e.id)
        //   FROM employees e
        //   WHERE e.department = 'IT'
    }

    public Slice<Employee> getActiveEmployees(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        return employeeRepository.findByActiveTrue(pageable);
        // Only ONE SQL query executed (Slice = no count):
        //
        // SQL:
        //   SELECT e.id, e.name, e.email, e.department, e.salary, e.active, e.joining_date
        //   FROM employees e
        //   WHERE e.active = TRUE
        //   LIMIT 11 OFFSET 0
        //          ↑ fetches size+1 (10+1=11) to check if next page exists
    }
}
```

```text
Page<Employee> object contains:

  ┌──────────────────────────────────────────────────────────────────┐
  │  Page<Employee>                                                  │
  │  ├── content: List<Employee>     → the actual 10 records        │
  │  ├── totalElements: 47           → total matching rows in DB    │
  │  ├── totalPages: 5               → ceil(47 / 10) = 5           │
  │  ├── number: 0                   → current page number (0-based)│
  │  ├── size: 10                    → page size                    │
  │  ├── numberOfElements: 10        → elements in THIS page        │
  │  ├── first: true                 → is this the first page?     │
  │  ├── last: false                 → is this the last page?      │
  │  ├── hasNext: true               → does a next page exist?     │
  │  ├── hasPrevious: false          → does a previous page exist? │
  │  └── sort: Sort.UNSORTED         → sort info (if provided)     │
  └──────────────────────────────────────────────────────────────────┘

  JSON response (Spring auto-serializes Page):

  {
    "content": [ { "id": 1, "name": "Alice", ... }, ... ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": { "sorted": false, "unsorted": true, "empty": true },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 47,
    "totalPages": 5,
    "last": false,
    "first": true,
    "size": 10,
    "number": 0,
    "numberOfElements": 10,
    "sort": { "sorted": false, "unsorted": true, "empty": true },
    "empty": false
  }
```

```text
Page calculation:

  Total rows matching WHERE clause: 47
  Page size: 10

  Page 0: OFFSET 0,  LIMIT 10 → rows 1-10
  Page 1: OFFSET 10, LIMIT 10 → rows 11-20
  Page 2: OFFSET 20, LIMIT 10 → rows 21-30
  Page 3: OFFSET 30, LIMIT 10 → rows 31-40
  Page 4: OFFSET 40, LIMIT 10 → rows 41-47 (only 7 records)

  PageRequest.of(page, size) → OFFSET = page * size, LIMIT = size

  ┌─────────────────────────────────────────────────────────────────────────┐
  │  PageRequest.of(0, 10) → LIMIT 10 OFFSET 0    │  Page 0: rows 1-10   │
  │  PageRequest.of(1, 10) → LIMIT 10 OFFSET 10   │  Page 1: rows 11-20  │
  │  PageRequest.of(2, 10) → LIMIT 10 OFFSET 20   │  Page 2: rows 21-30  │
  │  PageRequest.of(3, 10) → LIMIT 10 OFFSET 30   │  Page 3: rows 31-40  │
  │  PageRequest.of(4, 10) → LIMIT 10 OFFSET 40   │  Page 4: rows 41-47  │
  └─────────────────────────────────────────────────────────────────────────┘
```

---

### Sorting in Derived Queries Using Sort Object

You can sort results dynamically by passing a `Sort` parameter to any derived query method. This appends `ORDER BY` to the generated SQL.

**Conventions:**

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│  Sorting Conventions:                                                              │
│                                                                                    │
│  1. Add Sort as the LAST parameter (or Pageable which includes Sort)              │
│  2. Sort.by("propertyName")             → default ASC                             │
│  3. Sort.by(Sort.Direction.DESC, "prop") → explicit direction                     │
│  4. Sort.by("prop1").and(Sort.by("prop2")) → multiple properties                  │
│  5. Sort.by(Sort.Order.asc("name"), Sort.Order.desc("salary"))                    │
│     → different directions for different properties                                │
│                                                                                    │
│  6. Property names must match ENTITY field names (Java names, not column names):  │
│     ✓ Sort.by("joiningDate")   → ORDER BY joining_date                            │
│     ✗ Sort.by("joining_date")  → error! Not an entity property                   │
│                                                                                    │
│  7. You can ALSO hardcode OrderBy in the method name:                             │
│     findByDepartmentOrderByNameAsc → always sorts by name ASC                     │
│     But this is STATIC — cannot change at runtime.                                │
│     Sort parameter is DYNAMIC — caller decides the sort.                          │
│                                                                                    │
│  8. If both OrderBy in method name AND Sort parameter exist:                      │
│     The method-name OrderBy is applied FIRST, then Sort parameter appended.       │
│     Generally avoid mixing both — use one approach.                                │
└────────────────────────────────────────────────────────────────────────────────────┘
```

**Complete code example:**

```java
// Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Derived query with Sort parameter
    List<Employee> findByDepartment(String department, Sort sort);
    // SQL depends on the Sort passed at runtime

    // Multiple conditions with Sort
    List<Employee> findByDepartmentAndActiveTrue(String department, Sort sort);

    // Static OrderBy in method name — always sorts by salary DESC
    List<Employee> findByDepartmentOrderBySalaryDesc(String department);
    // SQL: SELECT * FROM employees WHERE department = ?1 ORDER BY salary DESC
}
```

```java
// Controller
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/by-department/sorted")
    public List<Employee> getByDepartmentSorted(
        @RequestParam String department,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return employeeService.getByDepartmentSorted(department, sortBy, direction);
    }

    @GetMapping("/by-department/multi-sorted")
    public List<Employee> getByDepartmentMultiSorted(
        @RequestParam String department
    ) {
        return employeeService.getByDepartmentMultiSorted(department);
    }
}

// Service
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    // ── SINGLE PROPERTY ASCENDING ──
    public List<Employee> sortByNameAsc(String department) {
        Sort sort = Sort.by("name");  // default direction = ASC
        return employeeRepository.findByDepartment(department, sort);
        // SQL: SELECT * FROM employees WHERE department = 'IT' ORDER BY name ASC
    }

    // ── SINGLE PROPERTY DESCENDING ──
    public List<Employee> sortBySalaryDesc(String department) {
        Sort sort = Sort.by(Sort.Direction.DESC, "salary");
        return employeeRepository.findByDepartment(department, sort);
        // SQL: SELECT * FROM employees WHERE department = 'IT' ORDER BY salary DESC
    }

    // ── MULTIPLE PROPERTIES, SAME DIRECTION ──
    public List<Employee> sortByMultipleSameDirection(String department) {
        Sort sort = Sort.by(Sort.Direction.ASC, "department", "name");
        return employeeRepository.findByDepartment(department, sort);
        // SQL: SELECT * FROM employees
        //      WHERE department = 'IT'
        //      ORDER BY department ASC, name ASC
    }

    // ── MULTIPLE PROPERTIES, DIFFERENT DIRECTIONS ──
    public List<Employee> getByDepartmentMultiSorted(String department) {
        Sort sort = Sort.by(
            Sort.Order.asc("name"),        // name ascending
            Sort.Order.desc("salary"),     // salary descending
            Sort.Order.asc("joiningDate")  // joiningDate ascending
        );
        return employeeRepository.findByDepartment(department, sort);
        // SQL: SELECT * FROM employees
        //      WHERE department = 'IT'
        //      ORDER BY name ASC, salary DESC, joining_date ASC
    }

    // ── DYNAMIC SORT FROM REQUEST PARAMETERS ──
    public List<Employee> getByDepartmentSorted(
        String department, String sortBy, String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(Sort.Direction.DESC, sortBy)
            : Sort.by(Sort.Direction.ASC, sortBy);

        return employeeRepository.findByDepartment(department, sort);
        // If sortBy="salary", direction="desc":
        // SQL: SELECT * FROM employees WHERE department = 'IT' ORDER BY salary DESC
    }

    // ── CHAINING SORTS WITH .and() ──
    public List<Employee> chainedSort(String department) {
        Sort sort = Sort.by("department").ascending()
                        .and(Sort.by("salary").descending())
                        .and(Sort.by("name").ascending());
        return employeeRepository.findByDepartment(department, sort);
        // SQL: SELECT * FROM employees
        //      WHERE department = 'IT'
        //      ORDER BY department ASC, salary DESC, name ASC
    }
}
```

```text
Sort object creation — all approaches:

  ┌─────────────────────────────────────────────────────────┬─────────────────────────────┐
  │ Code                                                    │ SQL ORDER BY                │
  ├─────────────────────────────────────────────────────────┼─────────────────────────────┤
  │ Sort.by("name")                                         │ ORDER BY name ASC           │
  │ Sort.by(Direction.DESC, "salary")                       │ ORDER BY salary DESC        │
  │ Sort.by("name", "salary")                               │ ORDER BY name ASC,          │
  │                                                         │          salary ASC         │
  │ Sort.by(Direction.DESC, "name", "salary")               │ ORDER BY name DESC,         │
  │                                                         │          salary DESC        │
  │ Sort.by(Order.asc("name"), Order.desc("salary"))        │ ORDER BY name ASC,          │
  │                                                         │          salary DESC        │
  │ Sort.by("name").ascending()                             │ ORDER BY name ASC           │
  │      .and(Sort.by("salary").descending())               │        , salary DESC        │
  │ Sort.unsorted()                                         │ (no ORDER BY)               │
  └─────────────────────────────────────────────────────────┴─────────────────────────────┘
```

```text
Static OrderBy (in method name) vs Dynamic Sort (parameter):

  ┌──────────────────────────────────────┬────────────────────────────────────────┐
  │ Static (method name)                 │ Dynamic (Sort parameter)               │
  ├──────────────────────────────────────┼────────────────────────────────────────┤
  │ findByDeptOrderByNameAsc             │ findByDept(dept, Sort)                 │
  │ Sort fixed at compile time           │ Sort decided at runtime                │
  │ Cannot change without code change    │ Caller passes any Sort object          │
  │ Simple, no extra parameter           │ More flexible, one method for all sorts│
  │ Good for: always-same-sort queries   │ Good for: user-controlled sorting      │
  │ e.g., "latest orders"               │ e.g., sortable table columns           │
  └──────────────────────────────────────┴────────────────────────────────────────┘
```

---

### Pagination with Sorting Combined

`Pageable` already supports sorting built-in. You can create a `PageRequest` with both page/size AND sort information. This generates SQL with `ORDER BY`, `LIMIT`, and `OFFSET` all together.

**Conventions:**

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│  Pagination + Sorting Conventions:                                                 │
│                                                                                    │
│  1. Use PageRequest.of(page, size, Sort) — combines both in one object            │
│  2. Or use PageRequest.of(page, size, Direction, "property1", "property2")        │
│  3. The repository method takes Pageable (which includes Sort info)               │
│  4. Return Page<T> if you need total count, Slice<T> if not                       │
│  5. Sort is ALWAYS applied BEFORE pagination:                                      │
│     → First ORDER BY → then LIMIT/OFFSET                                          │
│     → This ensures consistent page boundaries                                     │
│     → Without sorting, pagination can return inconsistent results                 │
│       (DB may return rows in different order between queries)                      │
│  6. Pageable parameter ALREADY contains Sort, so you do NOT need both             │
│     Pageable and Sort parameters — Pageable is enough.                            │
└────────────────────────────────────────────────────────────────────────────────────┘
```

**Complete code example:**

```java
// Repository — same method serves pagination, sorting, and both combined
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // One method handles: pagination only, sorting only, OR both
    Page<Employee> findByDepartment(String department, Pageable pageable);

    // With additional conditions
    Page<Employee> findByDepartmentAndSalaryGreaterThan(
        String department, Double salary, Pageable pageable
    );

    Slice<Employee> findByActiveTrue(Pageable pageable);
}
```

```java
// Controller
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // GET /api/employees/search?department=IT&page=0&size=10&sortBy=salary&direction=desc
    @GetMapping("/search")
    public Page<Employee> search(
        @RequestParam String department,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return employeeService.search(department, page, size, sortBy, direction);
    }

    // GET /api/employees/advanced-search?department=IT&page=0&size=10
    @GetMapping("/advanced-search")
    public Page<Employee> advancedSearch(
        @RequestParam String department,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return employeeService.advancedSearch(department, page, size);
    }
}

// Service
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    // ── PAGINATION + SINGLE SORT ──
    public Page<Employee> search(
        String department, int page, int size, String sortBy, String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(Sort.Direction.DESC, sortBy)
            : Sort.by(Sort.Direction.ASC, sortBy);

        Pageable pageable = PageRequest.of(page, size, sort);

        return employeeRepository.findByDepartment(department, pageable);
        // SQL 1 (data):
        //   SELECT e.id, e.name, e.email, e.department, e.salary, e.active, e.joining_date
        //   FROM employees e
        //   WHERE e.department = 'IT'
        //   ORDER BY e.salary DESC
        //   LIMIT 10 OFFSET 0
        //
        // SQL 2 (count — because return type is Page):
        //   SELECT COUNT(e.id) FROM employees e WHERE e.department = 'IT'
    }

    // ── PAGINATION + MULTIPLE SORTS WITH DIFFERENT DIRECTIONS ──
    public Page<Employee> advancedSearch(String department, int page, int size) {

        Sort sort = Sort.by(
            Sort.Order.asc("name"),
            Sort.Order.desc("salary"),
            Sort.Order.asc("joiningDate")
        );

        Pageable pageable = PageRequest.of(page, size, sort);

        return employeeRepository.findByDepartment(department, pageable);
        // SQL 1 (data):
        //   SELECT e.id, e.name, e.email, e.department, e.salary, e.active, e.joining_date
        //   FROM employees e
        //   WHERE e.department = 'IT'
        //   ORDER BY e.name ASC, e.salary DESC, e.joining_date ASC
        //   LIMIT 10 OFFSET 0
        //
        // SQL 2 (count):
        //   SELECT COUNT(e.id) FROM employees e WHERE e.department = 'IT'
    }

    // ── SHORTHAND — PageRequest.of with direction and properties ──
    public Page<Employee> simpleSearch(String department, int page, int size) {

        // All properties will have SAME direction
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "salary", "name");

        return employeeRepository.findByDepartment(department, pageable);
        // SQL:
        //   SELECT * FROM employees
        //   WHERE department = 'IT'
        //   ORDER BY salary DESC, name DESC
        //   LIMIT 10 OFFSET 0
    }

    // ── PAGINATION ONLY (no sort) ──
    public Page<Employee> paginatedOnly(String department, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);  // no Sort

        return employeeRepository.findByDepartment(department, pageable);
        // SQL:
        //   SELECT * FROM employees WHERE department = 'IT' LIMIT 10 OFFSET 0
        //   SELECT COUNT(*) FROM employees WHERE department = 'IT'
    }

    // ── SORTING ONLY (no pagination) ──
    public Page<Employee> sortedOnly(String department) {

        // Unpaged with sort — returns ALL matching rows sorted
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name"));

        return employeeRepository.findByDepartment(department, pageable);
        // SQL:
        //   SELECT * FROM employees WHERE department = 'IT' ORDER BY name ASC
        //   (effectively no LIMIT because MAX_VALUE)
    }
}
```

```text
PageRequest creation — all approaches:

  ┌──────────────────────────────────────────────────────────┬──────────────────────────────────┐
  │ Code                                                     │ SQL Generated                    │
  ├──────────────────────────────────────────────────────────┼──────────────────────────────────┤
  │ PageRequest.of(0, 10)                                    │ LIMIT 10 OFFSET 0                │
  │                                                          │ (no ORDER BY)                    │
  ├──────────────────────────────────────────────────────────┼──────────────────────────────────┤
  │ PageRequest.of(2, 10, Sort.by("name"))                   │ ORDER BY name ASC                │
  │                                                          │ LIMIT 10 OFFSET 20               │
  ├──────────────────────────────────────────────────────────┼──────────────────────────────────┤
  │ PageRequest.of(0, 5, Sort.Direction.DESC, "salary")      │ ORDER BY salary DESC             │
  │                                                          │ LIMIT 5 OFFSET 0                 │
  ├──────────────────────────────────────────────────────────┼──────────────────────────────────┤
  │ PageRequest.of(1, 10, Sort.by(                           │ ORDER BY name ASC, salary DESC   │
  │     Sort.Order.asc("name"),                              │ LIMIT 10 OFFSET 10               │
  │     Sort.Order.desc("salary")))                          │                                  │
  ├──────────────────────────────────────────────────────────┼──────────────────────────────────┤
  │ PageRequest.of(0, 20, Sort.Direction.ASC,                │ ORDER BY department ASC,         │
  │     "department", "name", "salary")                      │   name ASC, salary ASC           │
  │                                                          │ LIMIT 20 OFFSET 0                │
  └──────────────────────────────────────────────────────────┴──────────────────────────────────┘
```

```text
How Pagination + Sorting work together at the SQL level:

  Request: Page 2, Size 10, Sort by salary DESC then name ASC
  PageRequest.of(2, 10, Sort.by(Order.desc("salary"), Order.asc("name")))

  Step 1: WHERE clause (from derived query)
    WHERE department = 'IT'

  Step 2: ORDER BY (from Sort in Pageable) — applied FIRST
    ORDER BY salary DESC, name ASC

  Step 3: LIMIT / OFFSET (from page + size in Pageable)
    LIMIT 10 OFFSET 20    (page=2, size=10, offset=2*10=20)

  Final SQL:
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  SELECT e.id, e.name, e.email, e.department, e.salary, e.active,           │
  │         e.joining_date                                                      │
  │  FROM employees e                                                           │
  │  WHERE e.department = 'IT'                 ← from derived query             │
  │  ORDER BY e.salary DESC, e.name ASC        ← from Sort                     │
  │  LIMIT 10 OFFSET 20                        ← from PageRequest              │
  └─────────────────────────────────────────────────────────────────────────────┘

  Why ORDER BY must come before LIMIT/OFFSET:
    Without ORDER BY, the DB returns rows in arbitrary order.
    LIMIT/OFFSET on unsorted data → Page 1 and Page 2 might overlap!
    With ORDER BY → stable, deterministic ordering → consistent pages.

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  Without Sort:                                                              │
  │  Page 0: [Alice, Bob, Charlie, ...]    ← DB returns in any order           │
  │  Page 1: [Alice, Dave, Eve, ...]       ← might repeat Alice!              │
  │                                                                             │
  │  With Sort (by name ASC):                                                   │
  │  Page 0: [Alice, Bob, Charlie, ...]    ← deterministic                     │
  │  Page 1: [Dave, Eve, Frank, ...]       ← guaranteed no overlap            │
  └─────────────────────────────────────────────────────────────────────────────┘
```

```text
Complete flow — Pagination + Sorting in derived query:

  Client: GET /api/employees/search?department=IT&page=1&size=5&sortBy=salary&direction=desc
     │
     v
  Controller: extracts params → calls service
     │
     v
  Service:
    Sort sort = Sort.by(Direction.DESC, "salary");
    Pageable pageable = PageRequest.of(1, 5, sort);
    employeeRepository.findByDepartment("IT", pageable);
     │
     v
  Spring Proxy:
    PartTree already parsed: Subject=QUERY, Predicate=Part(department, SIMPLE_PROPERTY)
    Appends Sort from Pageable → ORDER BY salary DESC
    Appends pagination from Pageable → LIMIT 5 OFFSET 5
     │
     v
  Hibernate generates SQL:
    SQL 1: SELECT * FROM employees WHERE department='IT' ORDER BY salary DESC LIMIT 5 OFFSET 5
    SQL 2: SELECT COUNT(*) FROM employees WHERE department='IT'
     │
     v
  Results:
    Page<Employee> {
      content: [emp6, emp7, emp8, emp9, emp10],  ← 5 records (page 1)
      totalElements: 23,                          ← total matching IT dept
      totalPages: 5,                              ← ceil(23/5) = 5
      number: 1,                                  ← current page (0-indexed)
      size: 5,                                    ← page size
      sort: { sorted: true, orders: [{property: "salary", direction: "DESC"}] }
    }
     │
     v
  Controller returns → Jackson serializes to JSON → Client receives paginated, sorted response
```

---


### JPQL — Java Persistence Query Language, When Derived Queries Are Not Enough, 

Derived queries work great for simple conditions, but they break down when:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Limitations of Derived Queries:                                                 │
│                                                                                  │
│  1. Complex conditions → method name becomes unreadably long                     │
│     findByDepartmentAndSalaryGreaterThanAndActiveTrueAndJoiningDateAfter         │
│     AndNameContainingIgnoreCaseOrderBySalaryDesc(...)                            │
│                                                                                  │
│  2. JOIN queries across entities                                                 │
│     → Derived queries cannot express JOINs                                       │
│                                                                                  │
│  3. Aggregate functions (SUM, AVG, MAX, MIN, COUNT with GROUP BY)                │
│     → Not supported in derived queries                                           │
│                                                                                  │
│  4. Subqueries                                                                   │
│     → Not supported in derived queries                                           │
│                                                                                  │
│  5. Selecting specific columns (projections)                                     │
│     → Derived queries always return the full entity                              │
│                                                                                  │
│  6. UPDATE / INSERT with custom WHERE clauses                                    │
│     → Derived queries don't support INSERT/UPDATE at all                         │
│                                                                                  │
│  Solution → JPQL with @Query annotation                                          │
│             Write the query yourself using entity/class names,                    │
│             not table/column names. DB-independent.                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### What Is JPQL and Why Is It Called So?

**JPQL** stands for **Java Persistence Query Language**. It is a query language defined by the JPA specification that lets you write queries against **entity objects** (Java classes) rather than **database tables**. Hibernate (or any JPA provider) then translates the JPQL into the actual SQL dialect of the underlying database.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JPQL vs SQL — Key Difference:                                                   │
│                                                                                  │
│  SQL works on TABLES and COLUMNS:                                                │
│    SELECT e.emp_name, e.emp_salary                                               │
│    FROM employee_table e                                                         │
│    WHERE e.emp_department = 'IT'                                                 │
│         ↑ table name        ↑ column name                                        │
│                                                                                  │
│  JPQL works on ENTITIES and FIELDS:                                              │
│    SELECT e.name, e.salary                                                       │
│    FROM Employee e                                                               │
│    WHERE e.department = 'IT'                                                     │
│         ↑ entity class name  ↑ Java field name                                   │
│                                                                                  │
│  JPQL is DATABASE INDEPENDENT because:                                           │
│    - You write against Java entities, not DB tables                              │
│    - Hibernate translates JPQL → SQL for YOUR specific database                  │
│    - Same JPQL works on MySQL, PostgreSQL, Oracle, H2, etc.                      │
│    - Hibernate handles dialect differences (LIMIT vs ROWNUM, etc.)               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**How JPQL works on Entity Objects:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│  Java Entity:                         Database Table:                            │
│  ┌────────────────────┐               ┌────────────────────────────┐             │
│  │ @Entity            │               │ employees                  │             │
│  │ @Table(name =      │    Hibernate  │                            │             │
│  │   "employees")     │   translates  │ id BIGINT PRIMARY KEY      │             │
│  │ class Employee {   │  ──────────>  │ emp_name VARCHAR(100)      │             │
│  │   Long id;         │               │ emp_email VARCHAR(200)     │             │
│  │   String name;     │               │ department VARCHAR(50)     │             │
│  │   String email;    │               │ salary DOUBLE              │             │
│  │   String department│               │                            │             │
│  │   Double salary;   │               └────────────────────────────┘             │
│  │ }                  │                                                          │
│  └────────────────────┘                                                          │
│                                                                                  │
│  JPQL query:                                                                     │
│    SELECT e FROM Employee e WHERE e.department = :dept                            │
│           ↑        ↑                  ↑                                           │
│       alias   Entity class name   Java field name                                │
│                                                                                  │
│  Hibernate reads the @Entity and @Column annotations and translates to:          │
│                                                                                  │
│  SQL (MySQL):                                                                    │
│    SELECT e.id, e.emp_name, e.emp_email, e.department, e.salary                  │
│    FROM employees e                                                              │
│    WHERE e.department = ?                                                        │
│              ↑                                                                   │
│         actual DB column name                                                    │
│                                                                                  │
│  SQL (Oracle) — same JPQL produces Oracle-specific SQL:                          │
│    SELECT e.id, e.emp_name, e.emp_email, e.department, e.salary                  │
│    FROM employees e                                                              │
│    WHERE e.department = ?                                                        │
│                                                                                  │
│  JPQL never changes. Only the generated SQL changes per DB dialect.              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
When to use JPQL vs Derived Query:

  ┌───────────────────────────────────┬────────────────────────────────────────────┐
  │ Use Derived Query when:           │ Use JPQL when:                             │
  ├───────────────────────────────────┼────────────────────────────────────────────┤
  │ Simple 1-3 conditions             │ Complex conditions (4+ conditions)         │
  │ Single entity, no JOINs           │ JOINs across multiple entities             │
  │ Equality / comparison operators   │ Aggregate functions (SUM, AVG, COUNT)      │
  │ findByName, findBySalaryGreater   │ GROUP BY, HAVING clauses                   │
  │ Method name stays readable        │ Subqueries                                 │
  │ No projections needed             │ Specific column projections                │
  │ No update/insert with WHERE       │ Bulk UPDATE/DELETE with WHERE              │
  │ No GROUP BY / HAVING              │ Constructor expressions (DTO projections)  │
  └───────────────────────────────────┴────────────────────────────────────────────┘
```

---

### @Query and @Param Annotations

`@Query` is the annotation you place on a repository method to provide a custom JPQL (or native SQL) query instead of letting Spring derive it from the method name.

`@Param` binds a method parameter to a **named parameter** in the JPQL query.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Method Structure with @Query and @Param:                                        │
│                                                                                  │
│  @Query("SELECT e FROM Employee e WHERE e.department = :dept AND e.salary > :sal")│
│  List<Employee> findHighEarnersByDept(@Param("dept") String department,           │
│                                       @Param("salary") Double salary);           │
│                                                                                  │
│  ┌─────────┐   ┌──────────────────────────┐   ┌──────────────┐                  │
│  │ @Query   │   │ JPQL string              │   │ Return type  │                  │
│  │          │   │ uses :namedParams        │   │ determines   │                  │
│  │          │   │ works on entity names    │   │ result shape │                  │
│  └────┬─────┘   └────────────┬─────────────┘   └──────┬───────┘                  │
│       │                      │                         │                          │
│       v                      v                         v                          │
│  Tells Spring:          Hibernate                List<Employee>                   │
│  "Don't derive          translates               = multiple rows                  │
│   from method name.     to SQL at                Employee                         │
│   Use this JPQL."       runtime.                 = single row                     │
│                                                  Optional<Employee>               │
│  ┌─────────┐                                     = 0 or 1 row                     │
│  │ @Param   │                                    Long / long                      │
│  │          │                                    = count / aggregate               │
│  └────┬─────┘                                    Boolean                           │
│       │                                          = exists check                    │
│       v                                                                           │
│  Binds method argument                                                            │
│  to :namedParam in JPQL                                                           │
│  @Param("dept") → :dept                                                           │
│  @Param("sal")  → :sal                                                            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Two ways to bind parameters:**

```text
1. Named Parameters (recommended):
   @Query("SELECT e FROM Employee e WHERE e.name = :name")
   List<Employee> findByName(@Param("name") String name);
   → :name is bound to the method argument via @Param("name")

2. Positional Parameters:
   @Query("SELECT e FROM Employee e WHERE e.name = ?1 AND e.department = ?2")
   List<Employee> findByNameAndDept(String name, String department);
   → ?1 = first argument (name), ?2 = second argument (department)
   → No @Param needed, but less readable. Order matters!
```

**Complete code examples with Entity, Repository, Service, and generated SQL:**

```java
// Entity
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_name")
    private String name;

    private String email;
    private String department;
    private Double salary;
    private Boolean active;
    private LocalDate joiningDate;
    // getters, setters, constructors
}
```

```java
// Repository with @Query examples
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // 1. Simple SELECT with named parameter
    @Query("SELECT e FROM Employee e WHERE e.department = :dept")
    List<Employee> findByDept(@Param("dept") String department);
    // JPQL: SELECT e FROM Employee e WHERE e.department = :dept
    // SQL:  SELECT e.id, e.emp_name, e.email, e.department, e.salary, e.active, e.joining_date
    //       FROM employees e WHERE e.department = ?

    // 2. Multiple conditions with named parameters
    @Query("SELECT e FROM Employee e WHERE e.department = :dept AND e.salary > :minSalary")
    List<Employee> findHighEarners(@Param("dept") String department,
                                   @Param("minSalary") Double salary);
    // SQL: SELECT ... FROM employees e WHERE e.department = ? AND e.salary > ?

    // 3. LIKE with named parameter
    @Query("SELECT e FROM Employee e WHERE e.name LIKE %:keyword%")
    List<Employee> searchByName(@Param("keyword") String keyword);
    // SQL: SELECT ... FROM employees e WHERE e.emp_name LIKE '%keyword%'

    // 4. IN clause
    @Query("SELECT e FROM Employee e WHERE e.department IN :departments")
    List<Employee> findByDepartments(@Param("departments") List<String> departments);
    // SQL: SELECT ... FROM employees e WHERE e.department IN (?, ?, ?)

    // 5. BETWEEN
    @Query("SELECT e FROM Employee e WHERE e.salary BETWEEN :min AND :max")
    List<Employee> findBySalaryRange(@Param("min") Double min, @Param("max") Double max);
    // SQL: SELECT ... FROM employees e WHERE e.salary BETWEEN ? AND ?

    // 6. ORDER BY in JPQL
    @Query("SELECT e FROM Employee e WHERE e.department = :dept ORDER BY e.salary DESC")
    List<Employee> findByDeptOrderedBySalary(@Param("dept") String department);
    // SQL: SELECT ... FROM employees e WHERE e.department = ? ORDER BY e.salary DESC

    // 7. Aggregate — COUNT
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department = :dept")
    long countByDept(@Param("dept") String department);
    // SQL: SELECT COUNT(e.id) FROM employees e WHERE e.department = ?

    // 8. Aggregate — AVG
    @Query("SELECT AVG(e.salary) FROM Employee e WHERE e.department = :dept")
    Double averageSalaryByDept(@Param("dept") String department);
    // SQL: SELECT AVG(e.salary) FROM employees e WHERE e.department = ?

    // 9. DISTINCT
    @Query("SELECT DISTINCT e.department FROM Employee e")
    List<String> findAllDepartments();
    // SQL: SELECT DISTINCT e.department FROM employees e

    // 10. Single result
    @Query("SELECT e FROM Employee e WHERE e.email = :email")
    Optional<Employee> findByEmail(@Param("email") String email);
    // SQL: SELECT ... FROM employees e WHERE e.email = ?

    // 11. Positional parameter (no @Param)
    @Query("SELECT e FROM Employee e WHERE e.name = ?1 AND e.department = ?2")
    List<Employee> findByNameAndDept(String name, String department);
    // SQL: SELECT ... FROM employees e WHERE e.emp_name = ? AND e.department = ?
}
```

**Return type — Single object vs List:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Return Type Rules:                                                              │
│                                                                                  │
│  1. If query CAN return MULTIPLE rows → use List<Employee>                       │
│     @Query("SELECT e FROM Employee e WHERE e.department = :dept")                │
│     List<Employee> findByDept(@Param("dept") String department);                 │
│                                                                                  │
│  2. If query ALWAYS returns 0 or 1 row → use Employee or Optional<Employee>      │
│     @Query("SELECT e FROM Employee e WHERE e.email = :email")                    │
│     Optional<Employee> findByEmail(@Param("email") String email);                │
│                                                                                  │
│  3. If query returns a single VALUE → use that type directly                     │
│     @Query("SELECT COUNT(e) FROM Employee e WHERE e.department = :dept")         │
│     long countByDept(@Param("dept") String department);                          │
│                                                                                  │
│     @Query("SELECT AVG(e.salary) FROM Employee e")                               │
│     Double averageSalary();                                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**What happens with mismatched return types:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  SCENARIO 1: Query returns MULTIPLE rows, but return type is single object       │
│                                                                                  │
│  @Query("SELECT e FROM Employee e WHERE e.department = :dept")                   │
│  Employee findSingleByDept(@Param("dept") String dept);                          │
│                                                                                  │
│  If dept = 'IT' returns 5 employees:                                             │
│  → javax.persistence.NonUniqueResultException:                                   │
│    "query did not return a unique result: 5"                                     │
│  → RUNTIME ERROR! Application crashes.                                           │
│                                                                                  │
│  If dept = 'UNKNOWN' returns 0 employees:                                        │
│  → Returns NULL (or empty Optional if return type is Optional<Employee>)         │
│                                                                                  │
│  If dept = 'CEO-Office' returns exactly 1 employee:                              │
│  → Returns that single Employee. Works fine.                                     │
│                                                                                  │
│──────────────────────────────────────────────────────────────────────────────────│
│  SCENARIO 2: Query returns 0 or 1 row, but return type is List                   │
│                                                                                  │
│  @Query("SELECT e FROM Employee e WHERE e.email = :email")                       │
│  List<Employee> findByEmail(@Param("email") String email);                       │
│                                                                                  │
│  If email matches 1 row:                                                         │
│  → Returns List with 1 element. Works fine.                                      │
│                                                                                  │
│  If email matches 0 rows:                                                        │
│  → Returns EMPTY list (not null). Works fine.                                    │
│                                                                                  │
│  CONCLUSION: Using List<T> is ALWAYS SAFE.                                       │
│  Using single T is ONLY safe when you're 100% sure query returns 0 or 1 row.    │
│                                                                                  │
│  ┌────────────────────────────┬──────────────────────────────────────────────┐    │
│  │ Return Type                │ Behavior                                     │    │
│  ├────────────────────────────┼──────────────────────────────────────────────┤    │
│  │ List<Employee>             │ Always safe. 0 rows = empty list.            │    │
│  │ Employee                   │ >1 row = NonUniqueResultException.           │    │
│  │                            │ 0 rows = null.                               │    │
│  │ Optional<Employee>         │ >1 row = NonUniqueResultException.           │    │
│  │                            │ 0 rows = Optional.empty().                   │    │
│  │ Stream<Employee>           │ Always safe. Must be closed.                 │    │
│  └────────────────────────────┴──────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Service and Controller:**

```java
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getHighEarners(String department, Double minSalary) {
        return employeeRepository.findHighEarners(department, minSalary);
        // JPQL: SELECT e FROM Employee e WHERE e.department = :dept AND e.salary > :minSalary
        // SQL:  SELECT e.id, e.emp_name, e.email, e.department, e.salary, e.active, e.joining_date
        //       FROM employees e
        //       WHERE e.department = 'IT' AND e.salary > 50000
    }

    public Double getAverageSalary(String department) {
        return employeeRepository.averageSalaryByDept(department);
        // JPQL: SELECT AVG(e.salary) FROM Employee e WHERE e.department = :dept
        // SQL:  SELECT AVG(e.salary) FROM employees e WHERE e.department = 'IT'
    }
}

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/high-earners")
    public List<Employee> getHighEarners(
        @RequestParam String department,
        @RequestParam Double minSalary
    ) {
        return employeeService.getHighEarners(department, minSalary);
    }
}
```

```text
Complete flow — @Query with JPQL:

  Controller: getHighEarners("IT", 50000)
       │
       v
  Service: employeeRepository.findHighEarners("IT", 50000)
       │
       v
  Spring Proxy sees @Query annotation on the method
  → Does NOT parse the method name (ignores PartTree)
  → Uses the JPQL string directly
       │
       v
  @Param("dept") = "IT"  → binds to :dept in JPQL
  @Param("minSalary") = 50000 → binds to :minSalary in JPQL
       │
       v
  JPQL: SELECT e FROM Employee e WHERE e.department = :dept AND e.salary > :minSalary
       │
       v
  Hibernate reads entity annotations:
    Employee class → employees table
    e.department → e.department column
    e.salary → e.salary column
    e.name → e.emp_name column (@Column(name = "emp_name"))
       │
       v
  Generated SQL (MySQL dialect):
    SELECT e.id, e.emp_name, e.email, e.department, e.salary, e.active, e.joining_date
    FROM employees e
    WHERE e.department = 'IT' AND e.salary > 50000
       │
       v
  JDBC executes → ResultSet → mapped to List<Employee> → returned
```

---

### JOIN with JPQL — OneToOne Mapping Example

JPQL supports `JOIN` to query across related entities. Let's use a `UserDetails` (parent) and `Address` (child) with `@OneToOne` mapping.

**Entities:**

```java
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    // getters, setters, constructors
}

@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String country;
    private String zipCode;

    // getters, setters, constructors
}
```

```text
Database Tables:

  user_details                          addresses
  ┌────┬──────────┬──────────────┬────────────┐   ┌────┬──────────────┬──────────┬─────────┬──────────┐
  │ id │ username │ email        │ address_id │   │ id │ street       │ city     │ country │ zip_code │
  ├────┼──────────┼──────────────┼────────────┤   ├────┼──────────────┼──────────┼─────────┼──────────┤
  │  1 │ alice    │ alice@ex.com │     10     │──>│ 10 │ 123 Main St  │ New York │ USA     │ 10001    │
  │  2 │ bob      │ bob@ex.com   │     11     │──>│ 11 │ 456 Oak Ave  │ London   │ UK      │ SW1A 1AA │
  │  3 │ charlie  │ char@ex.com  │     12     │──>│ 12 │ 789 Pine Rd  │ Mumbai   │ India   │ 400001   │
  └────┴──────────┴──────────────┴────────────┘   └────┴──────────────┴──────────┴─────────┴──────────┘
```

**Repository with JOIN JPQL:**

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // 1. JOIN — get UserDetails with Address for a username
    @Query("SELECT u FROM UserDetails u JOIN u.address a WHERE u.username = :username")
    UserDetails findUserWithAddressByUsername(@Param("username") String username);
    // JPQL: SELECT u FROM UserDetails u JOIN u.address a WHERE u.username = :username
    // SQL:  SELECT u.id, u.username, u.email, u.address_id
    //       FROM user_details u
    //       INNER JOIN addresses a ON u.address_id = a.id
    //       WHERE u.username = ?

    // 2. JOIN — filter by child entity field (city)
    @Query("SELECT u FROM UserDetails u JOIN u.address a WHERE a.city = :city")
    List<UserDetails> findUsersByCity(@Param("city") String city);
    // JPQL: SELECT u FROM UserDetails u JOIN u.address a WHERE a.city = :city
    // SQL:  SELECT u.id, u.username, u.email, u.address_id
    //       FROM user_details u
    //       INNER JOIN addresses a ON u.address_id = a.id
    //       WHERE a.city = ?

    // 3. JOIN FETCH — eager load address in one query (avoids N+1)
    @Query("SELECT u FROM UserDetails u JOIN FETCH u.address WHERE u.username = :username")
    UserDetails findUserWithAddressFetched(@Param("username") String username);
    // JPQL: SELECT u FROM UserDetails u JOIN FETCH u.address WHERE u.username = :username
    // SQL:  SELECT u.id, u.username, u.email, u.address_id,
    //              a.id, a.street, a.city, a.country, a.zip_code
    //       FROM user_details u
    //       INNER JOIN addresses a ON u.address_id = a.id
    //       WHERE u.username = ?
    //       ↑ ONE query loads BOTH user AND address

    // 4. LEFT JOIN — include users even if they don't have an address
    @Query("SELECT u FROM UserDetails u LEFT JOIN u.address a WHERE a.country = :country OR a IS NULL")
    List<UserDetails> findUsersByCountryIncludingNoAddress(@Param("country") String country);
    // SQL:  SELECT u.id, u.username, u.email, u.address_id
    //       FROM user_details u
    //       LEFT JOIN addresses a ON u.address_id = a.id
    //       WHERE a.country = ? OR u.address_id IS NULL

    // 5. Multiple conditions on both entities
    @Query("SELECT u FROM UserDetails u JOIN u.address a " +
           "WHERE u.username LIKE %:keyword% AND a.country = :country")
    List<UserDetails> searchUsers(@Param("keyword") String keyword,
                                  @Param("country") String country);
    // SQL:  SELECT u.* FROM user_details u
    //       INNER JOIN addresses a ON u.address_id = a.id
    //       WHERE u.username LIKE '%keyword%' AND a.country = ?
}
```

```text
JOIN vs JOIN FETCH — Important Difference:

  ┌────────────────────────────────────────────────────────────────────────────────┐
  │  JOIN (regular):                                                              │
  │    @Query("SELECT u FROM UserDetails u JOIN u.address a WHERE a.city = :city")│
  │                                                                                │
  │    SQL 1: SELECT u.* FROM user_details u                                       │
  │           JOIN addresses a ON u.address_id = a.id                              │
  │           WHERE a.city = 'New York'                                            │
  │    → Returns UserDetails but address is NOT loaded yet (lazy proxy)            │
  │                                                                                │
  │    SQL 2: SELECT a.* FROM addresses WHERE id = ?  (triggered on access)        │
  │    → Lazy load fires when you call user.getAddress()                           │
  │    → N+1 problem if you load many users!                                       │
  │                                                                                │
  │  JOIN FETCH:                                                                   │
  │    @Query("SELECT u FROM UserDetails u JOIN FETCH u.address WHERE ...")         │
  │                                                                                │
  │    SQL: SELECT u.*, a.* FROM user_details u                                    │
  │         JOIN addresses a ON u.address_id = a.id                                │
  │         WHERE ...                                                              │
  │    → Returns UserDetails WITH address already loaded in ONE query              │
  │    → No N+1 problem. Address is populated immediately.                         │
  └────────────────────────────────────────────────────────────────────────────────┘
```

**Service and Controller:**

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public UserDetails getUserWithAddress(String username) {
        return userDetailsRepository.findUserWithAddressFetched(username);
        // JPQL: SELECT u FROM UserDetails u JOIN FETCH u.address WHERE u.username = :username
        // SQL:  SELECT u.id, u.username, u.email, u.address_id,
        //              a.id, a.street, a.city, a.country, a.zip_code
        //       FROM user_details u
        //       INNER JOIN addresses a ON u.address_id = a.id
        //       WHERE u.username = 'alice'
        //
        // Result: UserDetails { id=1, username="alice", email="alice@ex.com",
        //                       address=Address { street="123 Main St", city="New York", ... } }
    }
}

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{username}")
    public UserDetails getUser(@PathVariable String username) {
        return userService.getUserWithAddress(username);
    }
}
```

---

### Returning Specific Fields — List<Object[]> and DTO Constructor Expression

When you need fields from **multiple entities** (or only a few columns), you don't have to return the full entity. JPQL supports two approaches: `List<Object[]>` and **constructor expression** (DTO projection).

#### Approach 1: List<Object[]>

Each row is an `Object[]` where each element corresponds to one selected field, in order.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Select specific fields from UserDetails and Address
    @Query("SELECT u.username, a.country FROM UserDetails u JOIN u.address a")
    List<Object[]> findUsernameAndCountry();
    // JPQL: SELECT u.username, a.country FROM UserDetails u JOIN u.address a
    // SQL:  SELECT u.username, a.country
    //       FROM user_details u
    //       INNER JOIN addresses a ON u.address_id = a.id
    //
    // Result:  List<Object[]>
    //   [0] = Object[] { "alice",   "USA"   }
    //   [1] = Object[] { "bob",     "UK"    }
    //   [2] = Object[] { "charlie", "India" }

    // With WHERE condition
    @Query("SELECT u.username, u.email, a.city, a.country " +
           "FROM UserDetails u JOIN u.address a WHERE a.country = :country")
    List<Object[]> findUserInfoByCountry(@Param("country") String country);
    // SQL:  SELECT u.username, u.email, a.city, a.country
    //       FROM user_details u
    //       INNER JOIN addresses a ON u.address_id = a.id
    //       WHERE a.country = ?
}
```

```java
// Service — extracting values from Object[]
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public void printUsernameAndCountry() {
        List<Object[]> results = userDetailsRepository.findUsernameAndCountry();

        for (Object[] row : results) {
            String username = (String) row[0];   // first field in SELECT
            String country  = (String) row[1];   // second field in SELECT
            System.out.println(username + " → " + country);
        }
        // Output:
        //   alice → USA
        //   bob → UK
        //   charlie → India
    }
}
```

```text
Object[] mapping — positional:

  SELECT u.username, a.country FROM UserDetails u JOIN u.address a
         ↑ index 0    ↑ index 1

  Object[] row = { "alice", "USA" }
                    row[0]  row[1]

  Conventions:
  - Order in SELECT determines index in Object[]
  - You must cast each element manually: (String) row[0]
  - No type safety — compiler won't catch wrong casts
  - Fragile — if you reorder SELECT fields, all array indexes break
  - Use for quick ad-hoc queries, NOT for production APIs

  ┌──────────────────────────────────────────────────────────────┐
  │  Drawbacks of List<Object[]>:                                │
  │  - No compile-time type safety                               │
  │  - Manual casting: (String) row[0], (Double) row[1]          │
  │  - Index-based access is error-prone                         │
  │  - Refactoring the SELECT breaks consuming code              │
  │  - Not self-documenting — what is row[0]?                    │
  │                                                              │
  │  Better alternative → DTO Constructor Expression             │
  └──────────────────────────────────────────────────────────────┘
```

#### Approach 2: Constructor Expression with DTO (Recommended)

Instead of `Object[]`, you can tell JPQL to directly call a **DTO constructor** using the `NEW` keyword. JPQL maps the selected fields to the constructor parameters.

**Step 1: Create the DTO class:**

```java
// DTO — must have a matching constructor
public class UserCountryDTO {
    private String username;
    private String country;

    // Constructor — parameters must match JPQL SELECT fields in order and type
    public UserCountryDTO(String username, String country) {
        this.username = username;
        this.country = country;
    }

    // getters (and setters if needed)
    public String getUsername() { return username; }
    public String getCountry() { return country; }
}
```

**Step 2: Use `NEW` in JPQL with fully qualified class name:**

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Constructor expression — directly creates DTOs in JPQL
    @Query("SELECT NEW com.example.dto.UserCountryDTO(u.username, a.country) " +
           "FROM UserDetails u JOIN u.address a")
    List<UserCountryDTO> findUserCountryDTOs();
    // JPQL: SELECT NEW com.example.dto.UserCountryDTO(u.username, a.country)
    //       FROM UserDetails u JOIN u.address a
    // SQL:  SELECT u.username, a.country
    //       FROM user_details u
    //       INNER JOIN addresses a ON u.address_id = a.id
    //
    // Result: List<UserCountryDTO>  ← type-safe! Not Object[]
    //   [0] = UserCountryDTO { username="alice",   country="USA"   }
    //   [1] = UserCountryDTO { username="bob",     country="UK"    }
    //   [2] = UserCountryDTO { username="charlie", country="India" }

    // With WHERE condition
    @Query("SELECT NEW com.example.dto.UserCountryDTO(u.username, a.country) " +
           "FROM UserDetails u JOIN u.address a WHERE a.country = :country")
    List<UserCountryDTO> findUserCountryByCountry(@Param("country") String country);

    // More fields — use a different DTO
    @Query("SELECT NEW com.example.dto.UserDetailDTO(u.username, u.email, a.city, a.country) " +
           "FROM UserDetails u JOIN u.address a WHERE a.country = :country")
    List<UserDetailDTO> findDetailedUsers(@Param("country") String country);
}
```

```java
// DTO with more fields
public class UserDetailDTO {
    private String username;
    private String email;
    private String city;
    private String country;

    public UserDetailDTO(String username, String email, String city, String country) {
        this.username = username;
        this.email = email;
        this.city = city;
        this.country = country;
    }
    // getters
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Constructor Expression Conventions:                                             │
│                                                                                  │
│  1. Use keyword NEW followed by FULLY QUALIFIED class name:                      │
│     NEW com.example.dto.UserCountryDTO(u.username, a.country)                    │
│         ↑ full package path (cannot use short name)                              │
│                                                                                  │
│  2. The DTO class MUST have a constructor matching the parameter types:           │
│     JPQL: NEW ...DTO(u.username, a.country) → String, String                     │
│     Java: public UserCountryDTO(String username, String country)                 │
│     → Types and ORDER must match exactly                                         │
│                                                                                  │
│  3. The DTO does NOT need to be an @Entity — it's a plain POJO/record            │
│                                                                                  │
│  4. Return type is List<DTO> — fully type-safe                                   │
│                                                                                  │
│  5. The generated SQL is the SAME as Object[] approach — only the Java           │
│     mapping is different (constructor call vs raw array)                          │
│                                                                                  │
│  6. You can use Java records too:                                                │
│     public record UserCountryDTO(String username, String country) {}             │
│     → Constructor is auto-generated by the record                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Object[] vs DTO Constructor Expression:

  ┌────────────────────────────────┬────────────────────────────────────────────┐
  │ List<Object[]>                 │ List<UserCountryDTO> (constructor expr.)   │
  ├────────────────────────────────┼────────────────────────────────────────────┤
  │ No type safety                 │ Full type safety at compile time           │
  │ Manual cast: (String) row[0]  │ Direct getter: dto.getUsername()            │
  │ Index-based, fragile           │ Named fields, self-documenting             │
  │ No extra class needed          │ Need a DTO class with matching constructor │
  │ Quick prototyping              │ Production-ready                            │
  │ Breaks if SELECT order changes │ Breaks only if constructor changes         │
  └────────────────────────────────┴────────────────────────────────────────────┘
```

**Service using DTO approach:**

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    // Type-safe — no casting needed
    public List<UserCountryDTO> getUserCountries() {
        return userDetailsRepository.findUserCountryDTOs();
        // Each element is a UserCountryDTO, not Object[]
        // dto.getUsername(), dto.getCountry() — clean and safe
    }
}

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/countries")
    public List<UserCountryDTO> getUserCountries() {
        return userService.getUserCountries();
        // JSON: [{"username":"alice","country":"USA"}, {"username":"bob","country":"UK"}, ...]
    }
}
```

---

### @Modifying Annotation

`@Modifying` tells Spring Data JPA that the `@Query` is **not** a SELECT query — it is an **INSERT**, **UPDATE**, or **DELETE** that modifies data.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why @Modifying is needed:                                                       │
│                                                                                  │
│  By default, Spring assumes @Query contains a SELECT statement.                  │
│  It tries to execute it as a read query and map the ResultSet to entities.        │
│                                                                                  │
│  If you put an UPDATE/DELETE inside @Query without @Modifying:                    │
│  → Spring tries to read results from an UPDATE statement                         │
│  → Exception: "Expecting a SELECT query"                                         │
│  → or: "Not supported for DML operations"                                        │
│                                                                                  │
│  @Modifying tells Spring:                                                        │
│    "This is a DML (Data Manipulation Language) query."                            │
│    "Execute it with executeUpdate(), not executeQuery()."                         │
│    "The return value is the number of affected rows (int), not entities."         │
│                                                                                  │
│  ┌───────────────────────────────────────────────────────────────────────┐        │
│  │  Without @Modifying:                                                  │        │
│  │    @Query("UPDATE Employee e SET e.salary = :sal WHERE e.id = :id")   │        │
│  │    → Spring calls: entityManager.createQuery(jpql).getResultList()    │        │
│  │    → ERROR: not a SELECT!                                             │        │
│  │                                                                       │        │
│  │  With @Modifying:                                                     │        │
│  │    @Modifying                                                         │        │
│  │    @Query("UPDATE Employee e SET e.salary = :sal WHERE e.id = :id")   │        │
│  │    → Spring calls: entityManager.createQuery(jpql).executeUpdate()    │        │
│  │    → Returns int = number of rows updated                             │        │
│  └───────────────────────────────────────────────────────────────────────┘        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code examples:**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // UPDATE — bulk salary update
    @Modifying
    @Query("UPDATE Employee e SET e.salary = :newSalary WHERE e.department = :dept")
    int updateSalaryByDepartment(@Param("newSalary") Double newSalary,
                                 @Param("dept") String department);
    // JPQL: UPDATE Employee e SET e.salary = :newSalary WHERE e.department = :dept
    // SQL:  UPDATE employees SET salary = ? WHERE department = ?
    // Returns: int = number of rows updated (e.g., 15)

    // UPDATE — single field
    @Modifying
    @Query("UPDATE Employee e SET e.active = false WHERE e.id = :id")
    int deactivateEmployee(@Param("id") Long id);
    // SQL: UPDATE employees SET active = FALSE WHERE id = ?
    // Returns: 1 (one row updated) or 0 (no match)

    // DELETE — bulk delete
    @Modifying
    @Query("DELETE FROM Employee e WHERE e.department = :dept AND e.active = false")
    int deleteInactiveByDepartment(@Param("dept") String department);
    // JPQL: DELETE FROM Employee e WHERE e.department = :dept AND e.active = false
    // SQL:  DELETE FROM employees WHERE department = ? AND active = FALSE
    // Returns: int = number of rows deleted

    // UPDATE — multiple fields
    @Modifying
    @Query("UPDATE Employee e SET e.salary = :salary, e.department = :dept WHERE e.id = :id")
    int updateSalaryAndDepartment(@Param("salary") Double salary,
                                  @Param("dept") String department,
                                  @Param("id") Long id);
    // SQL: UPDATE employees SET salary = ?, department = ? WHERE id = ?
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Modifying — Flow Diagram:                                                      │
│                                                                                  │
│  Service calls: employeeRepository.updateSalaryByDepartment(60000, "IT")         │
│       │                                                                          │
│       v                                                                          │
│  Spring Proxy sees @Modifying + @Query                                           │
│       │                                                                          │
│       v                                                                          │
│  Calls: entityManager.createQuery(jpql).executeUpdate()                          │
│       │         (NOT getResultList() — because @Modifying)                       │
│       v                                                                          │
│  Hibernate translates JPQL → SQL:                                                │
│    UPDATE employees SET salary = 60000 WHERE department = 'IT'                   │
│       │                                                                          │
│       v                                                                          │
│  JDBC executes → returns int (number of rows affected)                           │
│       │                                                                          │
│       v                                                                          │
│  Returns 15 → "15 employees in IT department had their salary updated"           │
│                                                                                  │
│  IMPORTANT: This bypasses the Persistence Context entirely!                      │
│  The DB is updated, but cached entities in L1 cache are STALE.                   │
│  (More on this below)                                                            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### What Happens When You Use INSERT/UPDATE/DELETE Inside @Query

When you write a modifying JPQL query (UPDATE, DELETE) inside `@Query`, the SQL goes **directly to the database**, bypassing the normal JPA entity lifecycle.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Normal JPA flow (without @Query/@Modifying):                                    │
│                                                                                  │
│    Service: employee.setSalary(60000);                                           │
│       │                                                                          │
│       v                                                                          │
│    Persistence Context detects dirty field (salary changed)                      │
│       │                                                                          │
│       v                                                                          │
│    At flush: Hibernate generates UPDATE SQL                                      │
│       │                                                                          │
│       v                                                                          │
│    DB updated AND L1 cache entity is in sync (both have salary=60000)            │
│                                                                                  │
│──────────────────────────────────────────────────────────────────────────────────│
│  @Modifying @Query flow (JPQL UPDATE/DELETE):                                    │
│                                                                                  │
│    Service: employeeRepository.updateSalaryByDepartment(60000, "IT");            │
│       │                                                                          │
│       v                                                                          │
│    Hibernate sends UPDATE SQL directly to DB                                     │
│    SQL: UPDATE employees SET salary = 60000 WHERE department = 'IT'              │
│       │                                                                          │
│       v                                                                          │
│    DB is updated (salary = 60000 for all IT employees)                           │
│    BUT Persistence Context is NOT updated!                                       │
│    L1 cache still has old salary values!                                          │
│       │                                                                          │
│       v                                                                          │
│    If you now call findById(id) for an IT employee:                              │
│    → Returns STALE data from L1 cache (old salary)                               │
│    → NOT the 60000 you just set!                                                 │
│    ← This is the PERSISTENCE CONTEXT STALENESS problem.                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Why does this happen?

  @Modifying @Query executes: entityManager.createQuery(jpql).executeUpdate()
  This sends SQL DIRECTLY to the database.
  It does NOT go through the Persistence Context's dirty checking mechanism.
  The L1 cache has no idea the database changed.

  Think of it as:
    You opened a Word document.
    Someone else edited the file directly on disk.
    Your open document still shows the OLD content.
    You need to RELOAD (clear cache) to see the new content.
```

---

### @Transactional with @Modifying @Query

Yes, `@Transactional` is **required** for all `@Modifying` queries. Without it, you get a `TransactionRequiredException`.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why @Transactional is needed for @Modifying:                                    │
│                                                                                  │
│  1. executeUpdate() requires an active transaction                               │
│     → Without it: TransactionRequiredException at runtime                        │
│                                                                                  │
│  2. UPDATE/DELETE must be atomic                                                 │
│     → If bulk UPDATE fails halfway, all changes must roll back                   │
│                                                                                  │
│  3. Same reason as derived delete — modifying data needs a transaction           │
│                                                                                  │
│  Where to put @Transactional:                                                    │
│    Option A: On the Service method (RECOMMENDED)                                 │
│    Option B: On the Repository method                                            │
│    → Either works, but Service is preferred (business transaction boundary)      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Service — @Transactional is MANDATORY
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Transactional   // ← REQUIRED for @Modifying queries
    public int updateSalaryForDepartment(String department, Double newSalary) {
        return employeeRepository.updateSalaryByDepartment(newSalary, department);
        // SQL: UPDATE employees SET salary = ? WHERE department = ?
    }

    @Transactional   // ← REQUIRED
    public int removeInactiveEmployees(String department) {
        return employeeRepository.deleteInactiveByDepartment(department);
        // SQL: DELETE FROM employees WHERE department = ? AND active = FALSE
    }

    // WITHOUT @Transactional → RUNTIME ERROR:
    // org.springframework.dao.InvalidDataAccessApiUsageException:
    //   Executing an update/delete query;
    //   nested exception is javax.persistence.TransactionRequiredException
}
```

---

### Persistence Context Staleness — flushAutomatically and clearAutomatically

Since `@Modifying @Query` bypasses the Persistence Context, entities cached in the L1 cache become **stale** (outdated). Spring Data JPA provides two attributes on `@Modifying` to handle this.

**The Problem:**

```java
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Transactional
    public void demonstrateStaleness() {

        // STEP 1: Load employee into Persistence Context (L1 cache)
        Employee emp = employeeRepository.findById(1L).orElseThrow();
        System.out.println("Before: " + emp.getSalary());  // prints: 50000
        // SQL: SELECT * FROM employees WHERE id = 1
        // L1 cache: { Employee(id=1, salary=50000) }

        // STEP 2: Bulk update salary via @Modifying @Query
        employeeRepository.updateSalaryByDepartment(80000.0, "IT");
        // SQL: UPDATE employees SET salary = 80000 WHERE department = 'IT'
        // DB now has: salary = 80000 for employee id=1
        // BUT L1 cache STILL has: salary = 50000   ← STALE!

        // STEP 3: Read the same employee again
        Employee emp2 = employeeRepository.findById(1L).orElseThrow();
        System.out.println("After: " + emp2.getSalary());  // prints: 50000 ← WRONG!
        // Hibernate returns from L1 cache (no SQL executed)
        // L1 cache hit: Employee(id=1, salary=50000) — the STALE value
        // The DB has 80000 but we see 50000!
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  The Staleness Problem — Visualized:                                             │
│                                                                                  │
│  STEP 1: findById(1)                                                             │
│                                                                                  │
│    Persistence Context (L1 Cache)        Database                                │
│    ┌─────────────────────────┐           ┌─────────────────────────┐             │
│    │ Employee(id=1)          │           │ employees               │             │
│    │   salary = 50000  ✓     │    ==     │   id=1, salary=50000 ✓  │             │
│    │   (in sync with DB)     │           │                         │             │
│    └─────────────────────────┘           └─────────────────────────┘             │
│                                                                                  │
│  STEP 2: updateSalaryByDepartment(80000, "IT")  ← @Modifying @Query             │
│                                                                                  │
│    Persistence Context (L1 Cache)        Database                                │
│    ┌─────────────────────────┐           ┌─────────────────────────┐             │
│    │ Employee(id=1)          │           │ employees               │             │
│    │   salary = 50000  ✗     │    !=     │   id=1, salary=80000 ✓  │             │
│    │   (STALE — out of sync!)│           │   (updated by SQL)      │             │
│    └─────────────────────────┘           └─────────────────────────┘             │
│                                                                                  │
│  STEP 3: findById(1) again                                                       │
│    → Hibernate checks L1 cache first                                             │
│    → Cache HIT: Employee(id=1, salary=50000)                                     │
│    → Returns STALE entity without going to DB!                                   │
│    → You see 50000 instead of 80000!                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Solution 1: clearAutomatically = true

`@Modifying(clearAutomatically = true)` tells Spring to **clear the entire Persistence Context** (L1 cache) AFTER the modifying query executes. This forces subsequent reads to go to the database.

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Modifying(clearAutomatically = true)   // ← clears L1 cache AFTER update
    @Query("UPDATE Employee e SET e.salary = :newSalary WHERE e.department = :dept")
    int updateSalaryByDepartment(@Param("newSalary") Double newSalary,
                                 @Param("dept") String department);
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  clearAutomatically = true — What happens:                                       │
│                                                                                  │
│  STEP 1: findById(1) → loads Employee(salary=50000) into L1 cache               │
│                                                                                  │
│  STEP 2: updateSalaryByDepartment(80000, "IT")                                   │
│    2a. SQL: UPDATE employees SET salary = 80000 WHERE department = 'IT'           │
│    2b. entityManager.clear()  ← ALL entities evicted from L1 cache               │
│                                                                                  │
│    Persistence Context (L1 Cache)        Database                                │
│    ┌─────────────────────────┐           ┌─────────────────────────┐             │
│    │       (EMPTY)           │           │ employees               │             │
│    │   All entities cleared  │           │   id=1, salary=80000 ✓  │             │
│    └─────────────────────────┘           └─────────────────────────┘             │
│                                                                                  │
│  STEP 3: findById(1)                                                             │
│    → L1 cache is EMPTY (was cleared)                                             │
│    → Hibernate MUST go to DB                                                     │
│    → SQL: SELECT * FROM employees WHERE id = 1                                   │
│    → Returns Employee(salary=80000) ← CORRECT!                                   │
│                                                                                  │
│  WARNING: clear() evicts ALL entities, not just the updated ones.                │
│  Any other loaded entities are also gone (must be re-fetched).                   │
│  Any unsaved changes to other entities are LOST!                                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Solution 2: flushAutomatically = true

`@Modifying(flushAutomatically = true)` tells Spring to **flush the Persistence Context** BEFORE the modifying query executes. This ensures any pending dirty changes are written to the DB before the bulk update runs.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  flushAutomatically = true — Why it's needed:                                    │
│                                                                                  │
│  Problem scenario WITHOUT flushAutomatically:                                    │
│                                                                                  │
│  STEP 1: Load employee and modify in-memory                                     │
│    Employee emp = findById(1);   // salary = 50000                               │
│    emp.setDepartment("Finance"); // dirty change in L1 cache                     │
│    // L1 cache: Employee(id=1, department="Finance") — NOT flushed to DB yet     │
│    // DB still has: department = "IT"                                             │
│                                                                                  │
│  STEP 2: Run @Modifying query                                                   │
│    updateSalaryByDepartment(80000, "IT");                                        │
│    SQL: UPDATE employees SET salary = 80000 WHERE department = 'IT'              │
│    → This updates employee id=1 in DB (because DB still has department='IT')     │
│    → But we wanted to move them to Finance first!                                │
│    → The in-memory change was LOST — it wasn't flushed before the bulk update   │
│                                                                                  │
│  With flushAutomatically = true:                                                 │
│  STEP 1: Same as above                                                           │
│  STEP 2: Before running the @Modifying query:                                    │
│    → entityManager.flush()  ← writes pending changes to DB                       │
│    → SQL: UPDATE employees SET department = 'Finance' WHERE id = 1               │
│    → NOW the DB has department = 'Finance' for emp id=1                          │
│    → Then: UPDATE employees SET salary = 80000 WHERE department = 'IT'           │
│    → Employee id=1 is NOT affected (department is now Finance)                   │
│    → Correct behavior!                                                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Modifying(flushAutomatically = true)   // ← flushes BEFORE update
    @Query("UPDATE Employee e SET e.salary = :newSalary WHERE e.department = :dept")
    int updateSalaryByDepartment(@Param("newSalary") Double newSalary,
                                 @Param("dept") String department);
}
```

#### Using Both Together (Recommended for @Modifying)

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Employee e SET e.salary = :newSalary WHERE e.department = :dept")
    int updateSalaryByDepartment(@Param("newSalary") Double newSalary,
                                 @Param("dept") String department);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Employee e WHERE e.department = :dept AND e.active = false")
    int deleteInactiveByDepartment(@Param("dept") String department);
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Complete flow with BOTH flushAutomatically + clearAutomatically:                 │
│                                                                                  │
│  Service method:                                                                 │
│    emp = findById(1);               // load into L1 cache                        │
│    emp.setDepartment("Finance");    // dirty change (in L1, not DB)              │
│    updateSalaryByDepartment(80000, "IT");  // @Modifying query                   │
│       │                                                                          │
│       v                                                                          │
│  STEP 1: flushAutomatically = true → entityManager.flush()                       │
│    SQL: UPDATE employees SET department = 'Finance' WHERE id = 1                 │
│    → Pending dirty changes written to DB first                                   │
│       │                                                                          │
│       v                                                                          │
│  STEP 2: Execute the @Modifying query                                            │
│    SQL: UPDATE employees SET salary = 80000 WHERE department = 'IT'              │
│    → Runs against the LATEST DB state (department='Finance' for emp 1)           │
│       │                                                                          │
│       v                                                                          │
│  STEP 3: clearAutomatically = true → entityManager.clear()                       │
│    → L1 cache wiped. All entities detached.                                      │
│       │                                                                          │
│       v                                                                          │
│  STEP 4: Any subsequent findById(1) goes to DB (fresh data)                      │
│    SQL: SELECT * FROM employees WHERE id = 1                                     │
│    → Returns: Employee(department='Finance', salary=50000)                        │
│    → Correct! (emp 1 was moved to Finance BEFORE the IT salary update)           │
│                                                                                  │
│  Timeline:                                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  ┌──────────┐  ┌──────────────┐  │
│  │ flush()  │→│ UPDATE   │→│ clear()       │→│ findById │→│ Returns      │  │
│  │ dirty    │  │ salary   │  │ L1 cache     │  │ goes to  │  │ fresh data   │  │
│  │ changes  │  │ = 80000  │  │ emptied      │  │ DB       │  │ from DB      │  │
│  └──────────┘  └──────────┘  └──────────────┘  └──────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Complete Service example with all the pieces:**

```java
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Transactional
    public void correctBulkUpdate() {

        // 1. Load employee — enters L1 cache
        Employee emp = employeeRepository.findById(1L).orElseThrow();
        System.out.println("Before: " + emp.getSalary());  // 50000
        // SQL: SELECT * FROM employees WHERE id = 1
        // L1 cache: { Employee(id=1, salary=50000) }

        // 2. Bulk update via @Modifying(flushAutomatically=true, clearAutomatically=true)
        int updated = employeeRepository.updateSalaryByDepartment(80000.0, "IT");
        System.out.println("Updated: " + updated + " rows");
        // flush() runs first (no dirty changes here, so no extra SQL)
        // SQL: UPDATE employees SET salary = 80000 WHERE department = 'IT'
        // clear() runs after → L1 cache is EMPTY

        // 3. Read again — must go to DB because cache was cleared
        Employee emp2 = employeeRepository.findById(1L).orElseThrow();
        System.out.println("After: " + emp2.getSalary());  // 80000 ← CORRECT!
        // SQL: SELECT * FROM employees WHERE id = 1
        // Returns fresh data from DB
    }
}
```

```text
Summary of @Modifying attributes:

  ┌──────────────────────────┬─────────────────────────────────────────────────────┐
  │ Attribute                │ What it does                                        │
  ├──────────────────────────┼─────────────────────────────────────────────────────┤
  │ (no attributes)          │ @Modifying                                          │
  │                          │ → Just executes the query. No flush, no clear.     │
  │                          │ → L1 cache may be STALE after the query.           │
  │                          │ → Pending dirty changes may not be in DB yet.      │
  ├──────────────────────────┼─────────────────────────────────────────────────────┤
  │ flushAutomatically=true  │ entityManager.flush() called BEFORE the query.     │
  │                          │ → Writes pending dirty changes to DB first.        │
  │                          │ → Ensures bulk query operates on latest DB state.  │
  │                          │ → L1 cache still stale AFTER the query.            │
  ├──────────────────────────┼─────────────────────────────────────────────────────┤
  │ clearAutomatically=true  │ entityManager.clear() called AFTER the query.      │
  │                          │ → Evicts ALL entities from L1 cache.               │
  │                          │ → Subsequent reads go to DB (fresh data).          │
  │                          │ → WARNING: unsaved changes to other entities LOST! │
  ├──────────────────────────┼─────────────────────────────────────────────────────┤
  │ BOTH = true              │ flush() BEFORE + clear() AFTER.                    │
  │ (RECOMMENDED)            │ → Safest option. Flush pending → execute query →   │
  │                          │   clear cache → all subsequent reads are fresh.    │
  └──────────────────────────┴─────────────────────────────────────────────────────┘
```

---

### Pagination in JPQL Queries Using Pageable

Just like derived queries, you can add `Pageable` as a parameter to any `@Query` method. Spring Data JPA appends `LIMIT` and `OFFSET` to the generated SQL.

**Conventions:**

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│  Pagination in @Query Conventions:                                                 │
│                                                                                    │
│  1. Add Pageable as the LAST parameter of the method                               │
│  2. Do NOT include LIMIT/OFFSET in the JPQL — Spring adds them automatically      │
│  3. Return type: Page<T>, Slice<T>, or List<T>                                    │
│  4. If return type is Page<T>, Spring needs a COUNT query:                         │
│     - Spring auto-generates a count query from your JPQL                          │
│     - OR you can provide a custom count query via @Query(countQuery = "...")       │
│  5. The method name does NOT matter for @Query — naming is free                   │
│  6. Pageable contains both pagination (page, size) and sort info                  │
└────────────────────────────────────────────────────────────────────────────────────┘
```

**Code examples:**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Paginated JPQL query — returns Page
    @Query("SELECT e FROM Employee e WHERE e.department = :dept")
    Page<Employee> findByDeptPaginated(@Param("dept") String department, Pageable pageable);
    // JPQL: SELECT e FROM Employee e WHERE e.department = :dept
    // SQL 1: SELECT ... FROM employees e WHERE e.department = ? LIMIT ? OFFSET ?
    // SQL 2: SELECT COUNT(e) FROM employees e WHERE e.department = ?  (auto-generated count)

    // With custom count query (for complex JOINs where auto-count is expensive)
    @Query(value = "SELECT e FROM Employee e JOIN e.department d WHERE d.name = :dept",
           countQuery = "SELECT COUNT(e) FROM Employee e JOIN e.department d WHERE d.name = :dept")
    Page<Employee> findByDeptWithCustomCount(@Param("dept") String department, Pageable pageable);

    // Complex conditions + pagination
    @Query("SELECT e FROM Employee e WHERE e.salary > :minSalary AND e.active = true")
    Page<Employee> findActiveHighEarners(@Param("minSalary") Double minSalary, Pageable pageable);
    // SQL 1: SELECT ... FROM employees e WHERE e.salary > ? AND e.active = TRUE LIMIT ? OFFSET ?
    // SQL 2: SELECT COUNT(e) FROM employees e WHERE e.salary > ? AND e.active = TRUE

    // Slice — no count query
    @Query("SELECT e FROM Employee e WHERE e.department = :dept")
    Slice<Employee> findByDeptSliced(@Param("dept") String department, Pageable pageable);
    // SQL: SELECT ... FROM employees e WHERE e.department = ? LIMIT (size+1) OFFSET ?

    // List — no count, no hasNext
    @Query("SELECT e FROM Employee e WHERE e.department = :dept")
    List<Employee> findByDeptList(@Param("dept") String department, Pageable pageable);
    // SQL: SELECT ... FROM employees e WHERE e.department = ? LIMIT ? OFFSET ?
}
```

```java
// Service
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public Page<Employee> getByDepartmentPaginated(String department, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return employeeRepository.findByDeptPaginated(department, pageable);
        // SQL 1: SELECT e.id, e.emp_name, e.email, e.department, e.salary, e.active, e.joining_date
        //        FROM employees e
        //        WHERE e.department = 'IT'
        //        LIMIT 10 OFFSET 0
        //
        // SQL 2: SELECT COUNT(e.id) FROM employees e WHERE e.department = 'IT'
        //
        // Returns Page<Employee> with content + totalElements + totalPages
    }
}

// Controller
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/department")
    public Page<Employee> getByDepartment(
        @RequestParam String department,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return employeeService.getByDepartmentPaginated(department, page, size);
    }
}
```

```text
Auto-generated count query:

  Your JPQL:
    SELECT e FROM Employee e WHERE e.department = :dept

  Spring auto-generates count:
    SELECT COUNT(e) FROM Employee e WHERE e.department = :dept

  For simple queries, auto-generation works perfectly.
  For complex JOINs, you may need a custom countQuery to avoid performance issues:

    @Query(
      value = "SELECT e FROM Employee e JOIN FETCH e.address WHERE e.department = :dept",
      countQuery = "SELECT COUNT(e) FROM Employee e WHERE e.department = :dept"
    )
    Page<Employee> findByDept(@Param("dept") String dept, Pageable pageable);
    // The countQuery skips the JOIN — faster count
```

---

### Sorting in JPQL Queries Using Sort Object

You can pass `Sort` as a parameter to JPQL `@Query` methods for dynamic sorting. Spring appends `ORDER BY` to the generated SQL.

**Conventions:**

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│  Sorting in @Query Conventions:                                                    │
│                                                                                    │
│  1. Add Sort as the LAST parameter                                                │
│  2. Do NOT include ORDER BY in the JPQL — Spring adds it from Sort parameter      │
│     (Unless you want a FIXED sort, then include ORDER BY in JPQL)                 │
│  3. Sort properties must match ENTITY field names (not column names)              │
│  4. You CAN have ORDER BY in JPQL AND Sort parameter both:                        │
│     JPQL ORDER BY is applied first, then Sort parameter appended                  │
│     (but generally avoid mixing — confusing)                                       │
│  5. For JPQL queries, Spring uses JpaSort.unsafe() for SQL functions:             │
│     Sort.by("LENGTH(name)") → ERROR (Spring tries to validate as property)       │
│     JpaSort.unsafe("LENGTH(name)") → works (bypasses validation)                 │
└────────────────────────────────────────────────────────────────────────────────────┘
```

**Code examples:**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // JPQL with dynamic Sort
    @Query("SELECT e FROM Employee e WHERE e.department = :dept")
    List<Employee> findByDeptSorted(@Param("dept") String department, Sort sort);
    // Sort.by("salary") → SQL: ... ORDER BY e.salary ASC
    // Sort.by(Direction.DESC, "name") → SQL: ... ORDER BY e.emp_name DESC

    // Multiple conditions with Sort
    @Query("SELECT e FROM Employee e WHERE e.salary > :minSalary AND e.active = true")
    List<Employee> findActiveHighEarnersSorted(@Param("minSalary") Double salary, Sort sort);
}
```

```java
// Service
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    // Single sort — ascending
    public List<Employee> sortByName(String department) {
        Sort sort = Sort.by("name");
        return employeeRepository.findByDeptSorted(department, sort);
        // JPQL: SELECT e FROM Employee e WHERE e.department = :dept
        // SQL:  SELECT ... FROM employees e WHERE e.department = 'IT' ORDER BY e.emp_name ASC
    }

    // Single sort — descending
    public List<Employee> sortBySalaryDesc(String department) {
        Sort sort = Sort.by(Sort.Direction.DESC, "salary");
        return employeeRepository.findByDeptSorted(department, sort);
        // SQL: SELECT ... FROM employees e WHERE e.department = 'IT' ORDER BY e.salary DESC
    }

    // Multiple sorts — different directions
    public List<Employee> multiSort(String department) {
        Sort sort = Sort.by(
            Sort.Order.asc("department"),
            Sort.Order.desc("salary"),
            Sort.Order.asc("name")
        );
        return employeeRepository.findByDeptSorted(department, sort);
        // SQL: SELECT ... FROM employees e
        //      WHERE e.department = 'IT'
        //      ORDER BY e.department ASC, e.salary DESC, e.emp_name ASC
    }
}
```

```text
Sort in @Query vs Sort in Derived Query — identical behavior:

  Both use the same Sort object.
  Both generate the same ORDER BY clause.
  The only difference is WHERE the base query comes from:

  ┌──────────────────────────────┬──────────────────────────────────────┐
  │ Derived Query                │ @Query                               │
  ├──────────────────────────────┼──────────────────────────────────────┤
  │ Base query from method name  │ Base query from JPQL string          │
  │ Sort appended to generated   │ Sort appended to JPQL-generated SQL  │
  │ query                        │                                      │
  │ findByDept(dept, Sort)       │ @Query("SELECT e FROM Employee e     │
  │                              │   WHERE e.department = :dept")       │
  │                              │ findX(@Param("dept") dept, Sort)     │
  └──────────────────────────────┴──────────────────────────────────────┘
```

---

### Pagination with Sorting in JPQL Queries

Combine `Pageable` (which includes `Sort`) with `@Query` to get paginated AND sorted results from a JPQL query.

**Code examples:**

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Pageable includes Sort — one parameter for both
    @Query("SELECT e FROM Employee e WHERE e.department = :dept")
    Page<Employee> findByDeptPagedAndSorted(@Param("dept") String department, Pageable pageable);

    // Complex query with pagination + sorting
    @Query("SELECT e FROM Employee e WHERE e.salary BETWEEN :min AND :max AND e.active = true")
    Page<Employee> findActiveBySalaryRange(@Param("min") Double min,
                                           @Param("max") Double max,
                                           Pageable pageable);

    // JOIN with pagination + sorting
    @Query("SELECT u FROM UserDetails u JOIN u.address a WHERE a.country = :country")
    Page<UserDetails> findUsersByCountryPaged(@Param("country") String country, Pageable pageable);
}
```

```java
// Service
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    // Pagination + single sort
    public Page<Employee> searchPaged(String department, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "salary");
        Pageable pageable = PageRequest.of(page, size, sort);

        return employeeRepository.findByDeptPagedAndSorted(department, pageable);
        // JPQL: SELECT e FROM Employee e WHERE e.department = :dept
        //
        // SQL 1 (data):
        //   SELECT e.id, e.emp_name, e.email, e.department, e.salary, e.active, e.joining_date
        //   FROM employees e
        //   WHERE e.department = 'IT'
        //   ORDER BY e.salary DESC
        //   LIMIT 10 OFFSET 0
        //
        // SQL 2 (count):
        //   SELECT COUNT(e.id) FROM employees e WHERE e.department = 'IT'
    }

    // Pagination + multiple sorts with different directions
    public Page<Employee> advancedSearchPaged(String department, int page, int size) {
        Sort sort = Sort.by(
            Sort.Order.asc("name"),
            Sort.Order.desc("salary")
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        return employeeRepository.findByDeptPagedAndSorted(department, pageable);
        // SQL 1: SELECT ... FROM employees e
        //        WHERE e.department = 'IT'
        //        ORDER BY e.emp_name ASC, e.salary DESC
        //        LIMIT 10 OFFSET 0
        //
        // SQL 2: SELECT COUNT(e.id) FROM employees e WHERE e.department = 'IT'
    }

    // Shorthand — direction applied to all properties
    public Page<Employee> simplePagedSort(String department, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "salary", "name");

        return employeeRepository.findByDeptPagedAndSorted(department, pageable);
        // SQL: ... ORDER BY e.salary DESC, e.emp_name DESC LIMIT 10 OFFSET 0
    }
}

// Controller
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // GET /api/employees/search?department=IT&page=0&size=10
    @GetMapping("/search")
    public Page<Employee> search(
        @RequestParam String department,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return employeeService.searchPaged(department, page, size);
    }
}
```

```text
Complete flow — Pagination + Sorting in @Query JPQL:

  Client: GET /api/employees/search?department=IT&page=2&size=5
     │
     v
  Controller: extracts params → calls service
     │
     v
  Service:
    Sort sort = Sort.by(Order.desc("salary"), Order.asc("name"));
    Pageable pageable = PageRequest.of(2, 5, sort);
    employeeRepository.findByDeptPagedAndSorted("IT", pageable);
     │
     v
  Spring Proxy:
    Sees @Query annotation → uses JPQL string (not method name parsing)
    JPQL: SELECT e FROM Employee e WHERE e.department = :dept
    Appends Sort → ORDER BY e.salary DESC, e.emp_name ASC
    Appends Pagination → LIMIT 5 OFFSET 10
     │
     v
  Hibernate generates SQL:
    SQL 1: SELECT e.id, e.emp_name, e.email, e.department, e.salary,
                  e.active, e.joining_date
           FROM employees e
           WHERE e.department = 'IT'
           ORDER BY e.salary DESC, e.emp_name ASC
           LIMIT 5 OFFSET 10
    SQL 2: SELECT COUNT(e.id) FROM employees e WHERE e.department = 'IT'
     │
     v
  Results:
    Page<Employee> {
      content: [emp11, emp12, emp13, emp14, emp15],
      totalElements: 47,
      totalPages: 10,        ← ceil(47/5) = 10
      number: 2,             ← current page (0-indexed)
      size: 5,
      sort: { sorted: true, orders: [
        {property: "salary", direction: "DESC"},
        {property: "name", direction: "ASC"}
      ]}
    }
     │
     v
  Controller returns → Jackson serializes to JSON → Client receives response
```

```text
Summary — Derived Query vs JPQL for Pagination and Sorting:

  ┌──────────────────────────────────────┬──────────────────────────────────────┐
  │ Feature                              │ Derived Query vs JPQL @Query         │
  ├──────────────────────────────────────┼──────────────────────────────────────┤
  │ Pageable parameter                   │ Same — add as last parameter         │
  │ Sort parameter                       │ Same — add as last parameter         │
  │ PageRequest.of(page, size, sort)     │ Same — works for both                │
  │ Return type: Page, Slice, List       │ Same — all supported                 │
  │ Page triggers COUNT query            │ Same — yes for both                  │
  │ Custom count query                   │ Derived: auto only                   │
  │                                      │ JPQL: @Query(countQuery = "...")     │
  │ Where base query comes from          │ Derived: method name parsed          │
  │                                      │ JPQL: @Query string                  │
  │ LIMIT/OFFSET generation              │ Same — Spring adds automatically     │
  │ ORDER BY generation from Sort        │ Same — Spring appends automatically  │
  └──────────────────────────────────────┴──────────────────────────────────────┘
```

---

### The N+1 Problem in JPA, What Is the N+1 Problem?

The N+1 problem is a **performance anti-pattern** where loading N parent entities triggers N additional SQL queries to load their child entities — resulting in **1 + N total queries** instead of a single query.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 Problem — The Core Issue:                                                   │
│                                                                                  │
│  You want: "Give me all users and their addresses"                               │
│                                                                                  │
│  What SHOULD happen (1 query):                                                   │
│    SQL: SELECT u.*, a.* FROM user_details u                                      │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│    → 1 query, all data loaded                                                    │
│                                                                                  │
│  What ACTUALLY happens with N+1 (1 + N queries):                                 │
│    SQL 1: SELECT u.* FROM user_details               ← 1 query for parents      │
│           → Returns 5 users                                                      │
│                                                                                  │
│    SQL 2: SELECT a.* FROM user_addresses WHERE user_id = 1  ← child for user 1  │
│    SQL 3: SELECT a.* FROM user_addresses WHERE user_id = 2  ← child for user 2  │
│    SQL 4: SELECT a.* FROM user_addresses WHERE user_id = 3  ← child for user 3  │
│    SQL 5: SELECT a.* FROM user_addresses WHERE user_id = 4  ← child for user 4  │
│    SQL 6: SELECT a.* FROM user_addresses WHERE user_id = 5  ← child for user 5  │
│           → 5 additional queries (one per parent)                                │
│                                                                                  │
│    Total: 1 + 5 = 6 queries instead of 1!                                       │
│                                                                                  │
│  If N = 1000 users → 1001 queries!                                               │
│  If N = 10000 users → 10001 queries!                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**When does it happen and in which mapping?**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 happens in ANY relationship mapping with collections:                       │
│                                                                                  │
│  @OneToMany  → MOST COMMON. Parent has List<Child>.                              │
│  @ManyToMany → Parent has Set<Child> via join table.                             │
│  @ManyToOne  → Less common, but possible with eager fetch on the "many" side.    │
│  @OneToOne   → Possible with lazy proxy, but rare (only 1 extra query per row).  │
│                                                                                  │
│  The problem is worst with @OneToMany and @ManyToMany because each parent        │
│  has a COLLECTION of children — each collection triggers a separate SQL query.   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Does it happen on EAGER or LAZY?**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 can happen with BOTH FetchType.EAGER and FetchType.LAZY:                    │
│                                                                                  │
│  EAGER (FetchType.EAGER):                                                        │
│    N+1 happens IMMEDIATELY when you load the parent.                             │
│    Hibernate loads the parent → then fires N queries for children automatically. │
│    You can't avoid it — children are loaded whether you need them or not.        │
│    N+1 is HIDDEN — you don't explicitly call getChildren(), but queries fire.    │
│                                                                                  │
│  LAZY (FetchType.LAZY) — default for @OneToMany / @ManyToMany:                  │
│    N+1 happens WHEN YOU ACCESS the children collection.                          │
│    Loading the parent fires 1 query. Children are proxied (not loaded).          │
│    When you call user.getAddresses() for each user → N queries fire.            │
│    N+1 is VISIBLE — you can see it when you iterate and access children.        │
│                                                                                  │
│  KEY POINT: Lazy doesn't SOLVE N+1. It only DEFERS it.                          │
│  If you eventually access all children, you still get N+1 queries.              │
│  Lazy is better ONLY if you DON'T always need the children.                     │
│                                                                                  │
│  ┌──────────────┬──────────────────────────┬────────────────────────────────┐    │
│  │ FetchType    │ When N+1 fires           │ Can you avoid the N queries?  │    │
│  ├──────────────┼──────────────────────────┼────────────────────────────────┤    │
│  │ EAGER        │ Immediately on load      │ No — always fires N queries   │    │
│  │ LAZY         │ When you access children │ Yes — if you never access     │    │
│  │              │                          │ children, no extra queries    │    │
│  └──────────────┴──────────────────────────┴────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Performance impact:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Performance Impact of N+1:                                                      │
│                                                                                  │
│  1. DATABASE ROUND-TRIPS:                                                        │
│     Each query = 1 network round-trip to the database.                           │
│     1001 queries = 1001 round-trips over the network.                            │
│     Even if each query takes 1ms → total = 1+ second just in network latency.   │
│                                                                                  │
│  2. CONNECTION POOL EXHAUSTION:                                                   │
│     Each query holds a DB connection.                                            │
│     With many concurrent requests, the connection pool drains.                   │
│     Other requests wait → timeouts → application becomes unresponsive.           │
│                                                                                  │
│  3. DATABASE CPU/IO:                                                             │
│     1001 separate queries = 1001 query plans, 1001 index lookups.               │
│     1 JOIN query = 1 query plan, 1 optimized scan.                              │
│     The database does far more work with N+1.                                    │
│                                                                                  │
│  4. MEMORY PRESSURE:                                                             │
│     With EAGER, all children loaded into memory even if not needed.              │
│     1000 parents × 10 children each = 10,000 entities in L1 cache.              │
│                                                                                  │
│  5. LATENCY IN API RESPONSE:                                                     │
│     REST API that returns paginated users with addresses:                        │
│     Without N+1 fix: 200ms+ response time.                                      │
│     With N+1 fix (JOIN FETCH): 5-20ms response time.                            │
│     10x-100x performance difference!                                             │
│                                                                                  │
│  Example:                                                                        │
│    N = 100 parents, each with 5 children                                         │
│    Without fix: 1 + 100 = 101 SQL queries                                        │
│    With JOIN FETCH: 1 SQL query                                                  │
│    With @BatchSize(25): 1 + 4 = 5 SQL queries                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### N+1 with Eager Loading — Multiple Parents with Child Collections

When you load multiple parent entities that have `FetchType.EAGER` on a `@OneToMany` relationship, Hibernate fires 1 query for the parents and then N additional queries — one per parent — to load the children.

**Entities:**

```java
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToMany(mappedBy = "userDetails", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<UserAddress> userAddressList = new ArrayList<>();
    //                                              ↑ EAGER = load children IMMEDIATELY

    // getters, setters, constructors
}

@Entity
@Table(name = "user_addresses")
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserDetails userDetails;

    // getters, setters, constructors
}
```

```text
Database:

  user_details                           user_addresses
  ┌────┬────────┬──────────────┐         ┌────┬──────────────┬──────────┬─────────┬─────────┐
  │ id │ name   │ email        │         │ id │ street       │ city     │ country │ user_id │
  ├────┼────────┼──────────────┤         ├────┼──────────────┼──────────┼─────────┼─────────┤
  │  1 │ Alice  │ alice@ex.com │         │ 10 │ 123 Main St  │ New York │ USA     │    1    │
  │  2 │ Bob    │ bob@ex.com   │         │ 11 │ 456 Oak Ave  │ Boston   │ USA     │    1    │
  │  3 │ Charlie│ char@ex.com  │         │ 12 │ 789 Pine Rd  │ London   │ UK      │    2    │
  └────┴────────┴──────────────┘         │ 13 │ 321 Elm St   │ Mumbai   │ India   │    3    │
                                          │ 14 │ 654 Birch Dr │ Delhi    │ India   │    3    │
                                          └────┴──────────────┴──────────┴─────────┴─────────┘
```

```java
// Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {
    // Using built-in findAll()
}

// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public List<UserDetails> getAllUsers() {
        return userDetailsRepository.findAll();
        // You expect 1 query. You get 1 + 3 = 4 queries!
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 with FetchType.EAGER — What Hibernate does:                                 │
│                                                                                  │
│  You call: userDetailsRepository.findAll()                                       │
│                                                                                  │
│  QUERY 1 (the "1" in N+1) — Load all parents:                                   │
│    SQL: SELECT ud.id, ud.name, ud.email                                          │
│         FROM user_details ud                                                     │
│    → Returns 3 users: [Alice(1), Bob(2), Charlie(3)]                             │
│                                                                                  │
│  Now Hibernate sees FetchType.EAGER on userAddressList.                          │
│  It MUST load addresses NOW, before returning results.                           │
│                                                                                  │
│  QUERY 2 (N=1) — Load addresses for Alice (user_id = 1):                        │
│    SQL: SELECT ua.id, ua.street, ua.city, ua.country, ua.user_id                 │
│         FROM user_addresses ua                                                   │
│         WHERE ua.user_id = 1                                                     │
│    → Returns 2 addresses [10, 11]                                                │
│                                                                                  │
│  QUERY 3 (N=2) — Load addresses for Bob (user_id = 2):                          │
│    SQL: SELECT ua.id, ua.street, ua.city, ua.country, ua.user_id                 │
│         FROM user_addresses ua                                                   │
│         WHERE ua.user_id = 2                                                     │
│    → Returns 1 address [12]                                                      │
│                                                                                  │
│  QUERY 4 (N=3) — Load addresses for Charlie (user_id = 3):                      │
│    SQL: SELECT ua.id, ua.street, ua.city, ua.country, ua.user_id                 │
│         FROM user_addresses ua                                                   │
│         WHERE ua.user_id = 3                                                     │
│    → Returns 2 addresses [13, 14]                                                │
│                                                                                  │
│  TOTAL: 1 + 3 = 4 queries for 3 parents.                                        │
│  If you had 1000 users → 1 + 1000 = 1001 queries!                               │
│                                                                                  │
│  Timeline:                                                                       │
│  ┌────────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐                      │
│  │ SELECT     │  │ SELECT   │  │ SELECT   │  │ SELECT   │                      │
│  │ all users  │→│ addr for │→│ addr for │→│ addr for │                      │
│  │ (1 query)  │  │ Alice    │  │ Bob      │  │ Charlie  │                      │
│  └────────────┘  └──────────┘  └──────────┘  └──────────┘                      │
│       "1"             N=1           N=2           N=3                            │
│                                                                                  │
│  This happens AUTOMATICALLY with EAGER. You don't call getAddresses().          │
│  Hibernate fires N queries silently behind the scenes.                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### N+1 with JPQL JOIN — UserDetails → UserAddress (@OneToMany, Lazy)

Now let's use `FetchType.LAZY` (default for @OneToMany) and a JPQL `JOIN` query. The N+1 still happens — just **deferred** to when you access the collection.

**Entities (with LAZY this time):**

```java
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToMany(mappedBy = "userDetails", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAddress> userAddressList = new ArrayList<>();
    //                                              ↑ LAZY = don't load until accessed

    // getters, setters
}
```

```java
// Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query("SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad WHERE ud.name = :userFirstName")
    List<UserDetails> findUserDetailsWithAddress(@Param("userFirstName") String userFirstName);
}
```

```java
// Service — N+1 happens here
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional(readOnly = true)
    public List<UserDetails> getUsersWithAddresses(String name) {
        List<UserDetails> users = userDetailsRepository.findUserDetailsWithAddress(name);
        // QUERY 1: SELECT ud.* FROM user_details ud
        //          JOIN user_addresses ad ON ud.id = ad.user_id
        //          WHERE ud.name = 'Alice'
        // → Returns 1 user (Alice) — addresses NOT loaded yet (LAZY proxy)

        // Now we iterate and access the addresses:
        for (UserDetails user : users) {
            List<UserAddress> addresses = user.getUserAddressList();
            // ↑ LAZY proxy triggered! Hibernate fires a query NOW.
            System.out.println(user.getName() + " has " + addresses.size() + " addresses");
        }
        // If findUserDetailsWithAddress returned 5 users:
        //   QUERY 2: SELECT * FROM user_addresses WHERE user_id = 1
        //   QUERY 3: SELECT * FROM user_addresses WHERE user_id = 2
        //   QUERY 4: SELECT * FROM user_addresses WHERE user_id = 3
        //   QUERY 5: SELECT * FROM user_addresses WHERE user_id = 4
        //   QUERY 6: SELECT * FROM user_addresses WHERE user_id = 5
        //   → 5 extra queries! (N+1 problem)

        return users;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  N+1 with JPQL JOIN (FetchType.LAZY):                                            │
│                                                                                  │
│  JPQL: SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad                  │
│        WHERE ud.name = :userFirstName                                            │
│                                                                                  │
│  NOTE: JPQL "JOIN" is used ONLY for filtering (WHERE clause).                    │
│        It does NOT load the child collection!                                    │
│        The addresses are still LAZY proxies after this query.                    │
│                                                                                  │
│  QUERY 1 (the "1"):                                                              │
│    SQL: SELECT ud.id, ud.name, ud.email                                          │
│         FROM user_details ud                                                     │
│         INNER JOIN user_addresses ad ON ud.id = ad.user_id                       │
│         WHERE ud.name = 'Alice'                                                  │
│    → Returns UserDetails(Alice) with userAddressList = LAZY PROXY (not loaded)   │
│                                                                                  │
│  When you call: user.getUserAddressList().size()                                 │
│                                                                                  │
│  QUERY 2 (N=1):                                                                 │
│    SQL: SELECT ua.id, ua.street, ua.city, ua.country, ua.user_id                 │
│         FROM user_addresses ua                                                   │
│         WHERE ua.user_id = 1                                                     │
│    → Loads addresses for Alice                                                   │
│                                                                                  │
│  If the query returned multiple users (e.g., 3 users named "Alice"):            │
│                                                                                  │
│  QUERY 3 (N=2): SELECT ... FROM user_addresses WHERE user_id = 2                │
│  QUERY 4 (N=3): SELECT ... FROM user_addresses WHERE user_id = 3                │
│                                                                                  │
│  TOTAL: 1 + 3 = 4 queries.                                                      │
│                                                                                  │
│  Flow:                                                                           │
│  ┌─────────────────┐      ┌──────────────────┐                                  │
│  │ JPQL JOIN query  │      │ Returns users    │                                  │
│  │ (1 SQL query)    │ ──>  │ with LAZY proxy  │                                  │
│  │                  │      │ for addresses    │                                  │
│  └─────────────────┘      └────────┬─────────┘                                  │
│                                     │                                            │
│                            for each user:                                        │
│                            user.getUserAddressList()                              │
│                                     │                                            │
│                        ┌────────────┼────────────┐                               │
│                        v            v            v                                │
│                  ┌──────────┐ ┌──────────┐ ┌──────────┐                          │
│                  │ SELECT   │ │ SELECT   │ │ SELECT   │                          │
│                  │ addr for │ │ addr for │ │ addr for │                          │
│                  │ user 1   │ │ user 2   │ │ user 3   │                          │
│                  └──────────┘ └──────────┘ └──────────┘                          │
│                      N=1          N=2          N=3                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Solution 1: JOIN FETCH — Load Everything in One Query

`JOIN FETCH` tells Hibernate: "Not only JOIN for filtering, but also **FETCH** (load) the child collection into memory in the SAME query." This eliminates all N extra queries.

```java
// Repository — using JOIN FETCH
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query("SELECT ud FROM UserDetails ud JOIN FETCH ud.userAddressList ad WHERE ud.name = :userFirstName")
    List<UserDetails> findUserDetailsWithAddress(@Param("userFirstName") String userFirstName);
    //                                    ↑ FETCH keyword added
}
```

```java
// Service — NO more N+1!
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional(readOnly = true)
    public List<UserDetails> getUsersWithAddresses(String name) {
        List<UserDetails> users = userDetailsRepository.findUserDetailsWithAddress(name);
        // ONLY 1 QUERY! All addresses loaded in the same query.

        for (UserDetails user : users) {
            List<UserAddress> addresses = user.getUserAddressList();
            // ↑ NO lazy proxy trigger! Addresses are ALREADY loaded.
            // NO additional SQL query fired here.
            System.out.println(user.getName() + " has " + addresses.size() + " addresses");
        }

        return users;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JOIN FETCH — How it solves N+1:                                                 │
│                                                                                  │
│  JPQL: SELECT ud FROM UserDetails ud                                             │
│        JOIN FETCH ud.userAddressList ad                                          │
│        WHERE ud.name = :userFirstName                                            │
│                                                                                  │
│  SINGLE SQL QUERY generated:                                                     │
│    SELECT ud.id, ud.name, ud.email,                                              │
│           ad.id, ad.street, ad.city, ad.country, ad.user_id                      │
│    FROM user_details ud                                                          │
│    INNER JOIN user_addresses ad ON ud.id = ad.user_id                            │
│    WHERE ud.name = 'Alice'                                                       │
│                                                                                  │
│  Result set (raw SQL rows):                                                      │
│  ┌──────┬───────┬──────────────┬──────┬──────────────┬──────────┬─────────┐      │
│  │ud.id │ud.name│ ud.email     │ ad.id│ ad.street    │ ad.city  │ad.country│     │
│  ├──────┼───────┼──────────────┼──────┼──────────────┼──────────┼─────────┤      │
│  │  1   │ Alice │ alice@ex.com │  10  │ 123 Main St  │ New York │ USA     │      │
│  │  1   │ Alice │ alice@ex.com │  11  │ 456 Oak Ave  │ Boston   │ USA     │      │
│  └──────┴───────┴──────────────┴──────┴──────────────┴──────────┴─────────┘      │
│                                                                                  │
│  Hibernate de-duplicates and builds:                                             │
│    UserDetails(id=1, name="Alice") {                                             │
│      userAddressList: [                                                          │
│        UserAddress(id=10, street="123 Main St", city="New York"),                │
│        UserAddress(id=11, street="456 Oak Ave", city="Boston")                   │
│      ]                                                                           │
│    }                                                                             │
│                                                                                  │
│  TOTAL QUERIES: 1  (instead of 1 + N)                                            │
│                                                                                  │
│  Before (JOIN):       1 + N queries    │   After (JOIN FETCH):  1 query          │
│  ┌──────────────┐     ┌──────────┐     │   ┌─────────────────────────┐           │
│  │ SELECT users │ ──> │ SELECT   │×N   │   │ SELECT users + addresses│           │
│  └──────────────┘     │ addresses│     │   │ in ONE query            │           │
│                       └──────────┘     │   └─────────────────────────┘           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
JOIN vs JOIN FETCH — Comparison:

  ┌──────────────────────────────────────┬────────────────────────────────────────┐
  │ JOIN (regular)                       │ JOIN FETCH                             │
  ├──────────────────────────────────────┼────────────────────────────────────────┤
  │ Used for filtering (WHERE clause)    │ Used for filtering AND loading         │
  │ Children are NOT loaded              │ Children ARE loaded in the same query  │
  │ Children remain LAZY proxies         │ Children are fully initialized         │
  │ Accessing children = N extra queries │ Accessing children = 0 extra queries   │
  │ SELECT returns parent columns only   │ SELECT returns parent + child columns  │
  │ N+1 problem remains                  │ N+1 problem SOLVED                     │
  └──────────────────────────────────────┴────────────────────────────────────────┘

  NOTE: JOIN FETCH cannot be used with Pageable directly.
  Hibernate warns: "firstResult/maxResults specified with collection fetch; applying in memory!"
  For pagination + JOIN FETCH, use a two-query approach or @EntityGraph.
```

---

### Solution 2: @BatchSize — Batch the N Queries Into Fewer Queries

`@BatchSize(size = N)` tells Hibernate: "Instead of loading children one parent at a time, load children for N parents in a single query using `IN (?, ?, ?, ...)`."

This doesn't reduce to 1 query like JOIN FETCH, but it reduces from N to ⌈N/batchSize⌉.

```java
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @OneToMany(mappedBy = "userDetails", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)   // ← load addresses for 10 users at a time
    private List<UserAddress> userAddressList = new ArrayList<>();

    // getters, setters
}
```

```java
// Repository — regular JPQL JOIN (NOT JOIN FETCH)
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query("SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad WHERE ud.name = :userFirstName")
    List<UserDetails> findUserDetailsWithAddress(@Param("userFirstName") String userFirstName);
}
```

```java
// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional(readOnly = true)
    public List<UserDetails> getUsersWithAddresses(String name) {
        List<UserDetails> users = userDetailsRepository.findUserDetailsWithAddress(name);
        // QUERY 1: SELECT ud.* FROM user_details ud
        //          JOIN user_addresses ad ON ud.id = ad.user_id
        //          WHERE ud.name = 'Alice'
        // → Returns users (addresses are still LAZY proxies)

        for (UserDetails user : users) {
            user.getUserAddressList().size();  // triggers batch load
        }
        // With @BatchSize(size = 10):
        // If 25 users returned, instead of 25 individual queries:
        //   QUERY 2: SELECT * FROM user_addresses WHERE user_id IN (1,2,3,4,5,6,7,8,9,10)
        //   QUERY 3: SELECT * FROM user_addresses WHERE user_id IN (11,12,13,14,15,16,17,18,19,20)
        //   QUERY 4: SELECT * FROM user_addresses WHERE user_id IN (21,22,23,24,25)
        // Total: 1 + 3 = 4 queries instead of 1 + 25 = 26!

        return users;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @BatchSize(size = 10) — How it works:                                           │
│                                                                                  │
│  Without @BatchSize (N+1):                                                       │
│    Query 1: SELECT users → 25 users                                              │
│    Query 2: SELECT addresses WHERE user_id = 1                                   │
│    Query 3: SELECT addresses WHERE user_id = 2                                   │
│    ...                                                                           │
│    Query 26: SELECT addresses WHERE user_id = 25                                 │
│    TOTAL: 26 queries                                                             │
│                                                                                  │
│  With @BatchSize(size = 10):                                                     │
│    Query 1: SELECT users → 25 users                                              │
│    Query 2: SELECT addresses WHERE user_id IN (1,2,3,4,5,6,7,8,9,10)            │
│    Query 3: SELECT addresses WHERE user_id IN (11,12,13,14,15,16,17,18,19,20)    │
│    Query 4: SELECT addresses WHERE user_id IN (21,22,23,24,25)                   │
│    TOTAL: 4 queries                                                              │
│                                                                                  │
│  Reduction: from 26 queries to 4 queries!                                        │
│                                                                                  │
│  Formula: Total queries = 1 + ⌈N / batchSize⌉                                   │
│    N = 25, batchSize = 10 → 1 + ⌈25/10⌉ = 1 + 3 = 4                            │
│    N = 100, batchSize = 25 → 1 + ⌈100/25⌉ = 1 + 4 = 5                          │
│    N = 1000, batchSize = 50 → 1 + ⌈1000/50⌉ = 1 + 20 = 21                      │
│                                                                                  │
│  Timeline comparison:                                                            │
│                                                                                  │
│  Without @BatchSize (25 users):                                                  │
│  ┌──────┐ ┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐...┌──┐                │
│  │users │ │a1││a2││a3││a4││a5││a6││a7││a8││a9││10││11│   │25│                │
│  └──────┘ └──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘└──┘...└──┘                │
│  26 queries total                                                                │
│                                                                                  │
│  With @BatchSize(10):                                                            │
│  ┌──────┐ ┌─────────────────┐ ┌─────────────────┐ ┌──────────┐                  │
│  │users │ │ addr IN(1..10)  │ │ addr IN(11..20) │ │IN(21..25)│                  │
│  └──────┘ └─────────────────┘ └─────────────────┘ └──────────┘                  │
│  4 queries total                                                                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Does @BatchSize work with FetchType.EAGER?**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @BatchSize + FetchType.EAGER — YES, it works!                                   │
│                                                                                  │
│  @OneToMany(mappedBy = "userDetails", fetch = FetchType.EAGER)                   │
│  @BatchSize(size = 10)                                                           │
│  private List<UserAddress> userAddressList;                                      │
│                                                                                  │
│  With EAGER, Hibernate loads children IMMEDIATELY after loading parents.         │
│  Without @BatchSize: fires N individual queries (1 per parent).                  │
│  With @BatchSize(10): batches the eager loading into chunks of 10.               │
│                                                                                  │
│  findAll() → 25 users                                                            │
│                                                                                  │
│  Without @BatchSize (EAGER):                                                     │
│    Query 1: SELECT users                                                         │
│    Query 2-26: SELECT addresses WHERE user_id = ? (× 25)                         │
│    Total: 26                                                                     │
│                                                                                  │
│  With @BatchSize(10) + EAGER:                                                    │
│    Query 1: SELECT users                                                         │
│    Query 2: SELECT addresses WHERE user_id IN (1,2,...,10)   ← auto-triggered    │
│    Query 3: SELECT addresses WHERE user_id IN (11,12,...,20) ← auto-triggered    │
│    Query 4: SELECT addresses WHERE user_id IN (21,...,25)    ← auto-triggered    │
│    Total: 4                                                                      │
│                                                                                  │
│  The difference from LAZY:                                                       │
│  - EAGER + @BatchSize: batched queries fire IMMEDIATELY (no access needed)       │
│  - LAZY + @BatchSize: batched queries fire when you ACCESS the collection        │
│  - The SQL is the same — only the TIMING differs                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Solution 3: @EntityGraph — Override Fetch Strategy Per Query

`@EntityGraph(attributePaths = "userAddressList")` tells JPA: "For this specific query, fetch `userAddressList` eagerly using a LEFT JOIN — regardless of the entity's FetchType annotation."

This is placed on **repository methods** (both derived queries and @Query methods).

```java
// Repository — using @EntityGraph on a derived query
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Derived query with @EntityGraph — eagerly fetches addresses in ONE query
    @EntityGraph(attributePaths = "userAddressList")
    List<UserDetails> findByName(String name);
    // Without @EntityGraph:
    //   SQL 1: SELECT ud.* FROM user_details ud WHERE ud.name = ?
    //   SQL 2-N: SELECT ua.* FROM user_addresses ua WHERE ua.user_id = ?  (per user)
    //
    // With @EntityGraph:
    //   SQL: SELECT ud.id, ud.name, ud.email,
    //               ua.id, ua.street, ua.city, ua.country, ua.user_id
    //        FROM user_details ud
    //        LEFT JOIN user_addresses ua ON ud.id = ua.user_id
    //        WHERE ud.name = ?
    //   → 1 query! Addresses loaded via LEFT JOIN.

    // @EntityGraph on findAll (override the built-in method)
    @EntityGraph(attributePaths = "userAddressList")
    @Override
    List<UserDetails> findAll();
    // SQL: SELECT ud.*, ua.*
    //      FROM user_details ud
    //      LEFT JOIN user_addresses ua ON ud.id = ua.user_id

    // @EntityGraph on a @Query method — also works!
    @EntityGraph(attributePaths = "userAddressList")
    @Query("SELECT ud FROM UserDetails ud WHERE ud.name = :name")
    List<UserDetails> findUsersByName(@Param("name") String name);
    // SQL: SELECT ud.*, ua.*
    //      FROM user_details ud
    //      LEFT JOIN user_addresses ua ON ud.id = ua.user_id
    //      WHERE ud.name = ?

    // Multiple attribute paths — fetch multiple collections/associations
    @EntityGraph(attributePaths = {"userAddressList", "orders"})
    List<UserDetails> findByEmail(String email);
    // SQL: SELECT ud.*, ua.*, o.*
    //      FROM user_details ud
    //      LEFT JOIN user_addresses ua ON ud.id = ua.user_id
    //      LEFT JOIN orders o ON ud.id = o.user_id
    //      WHERE ud.email = ?
}
```

```java
// Service — uses @EntityGraph method
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional(readOnly = true)
    public List<UserDetails> getUsersByName(String name) {
        List<UserDetails> users = userDetailsRepository.findByName(name);
        // ONLY 1 SQL query executed (LEFT JOIN includes addresses)

        for (UserDetails user : users) {
            // No lazy proxy trigger — addresses already loaded
            System.out.println(user.getName() + " has "
                + user.getUserAddressList().size() + " addresses");
            // NO additional SQL! Everything loaded in the initial query.
        }

        return users;
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @EntityGraph — How it works:                                                    │
│                                                                                  │
│  @EntityGraph(attributePaths = "userAddressList")                                │
│  List<UserDetails> findByName(String name);                                      │
│                                                                                  │
│  At query time, JPA sees the @EntityGraph and:                                   │
│    1. Ignores the entity's FetchType (LAZY or EAGER — doesn't matter)            │
│    2. Adds a LEFT JOIN for "userAddressList" to the generated SQL                │
│    3. Fetches parent + child data in ONE query                                   │
│                                                                                  │
│  Generated SQL:                                                                  │
│    SELECT ud.id, ud.name, ud.email,                                              │
│           ua.id, ua.street, ua.city, ua.country, ua.user_id                      │
│    FROM user_details ud                                                          │
│    LEFT JOIN user_addresses ua ON ud.id = ua.user_id                             │
│    WHERE ud.name = 'Alice'                                                       │
│                                                                                  │
│  NOTE: @EntityGraph uses LEFT JOIN (not INNER JOIN)                               │
│  This means users WITHOUT addresses are also returned (with null address list).  │
│  JOIN FETCH uses INNER JOIN by default — users without addresses are excluded.   │
│                                                                                  │
│  ┌─────────────────────────────┬───────────────────────────────────┐             │
│  │ Without @EntityGraph        │ With @EntityGraph                  │             │
│  ├─────────────────────────────┼───────────────────────────────────┤             │
│  │ SQL 1: SELECT users         │ SQL 1: SELECT users LEFT JOIN     │             │
│  │ SQL 2: SELECT addr user 1   │        addresses                  │             │
│  │ SQL 3: SELECT addr user 2   │ (1 query total)                   │             │
│  │ SQL 4: SELECT addr user 3   │                                   │             │
│  │ (4 queries total)           │                                   │             │
│  └─────────────────────────────┴───────────────────────────────────┘             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Why JPA Tries to Optimize and What Happens with JOIN FETCH / @BatchSize / @EntityGraph

JPA (Hibernate) defaults to **lazy loading** as an optimization strategy: don't load data you don't need. This is the right default for most cases. But when you DO need the related data, lazy loading backfires into the N+1 problem.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why JPA defaults to LAZY (optimization):                                        │
│                                                                                  │
│  Scenario: Employee entity has 5 relationships:                                  │
│    @OneToMany  addresses                                                         │
│    @OneToMany  orders                                                            │
│    @ManyToMany projects                                                          │
│    @ManyToOne  department                                                        │
│    @OneToOne   profilePicture                                                    │
│                                                                                  │
│  If ALL were EAGER:                                                              │
│    findById(1) would fire:                                                       │
│      SQL 1: SELECT employee WHERE id = 1                                         │
│      SQL 2: SELECT addresses WHERE employee_id = 1                               │
│      SQL 3: SELECT orders WHERE employee_id = 1                                  │
│      SQL 4: SELECT projects via join table WHERE employee_id = 1                 │
│      SQL 5: SELECT department WHERE id = ?                                       │
│      SQL 6: SELECT profilePicture WHERE employee_id = 1                          │
│    → 6 queries every time, even if you only need the employee's name!            │
│                                                                                  │
│  With LAZY (default for collections):                                            │
│    findById(1) fires only:                                                       │
│      SQL 1: SELECT employee WHERE id = 1                                         │
│    → 1 query. Other data loaded ONLY when accessed.                              │
│    → If API only needs employee name → no wasted queries.                        │
│                                                                                  │
│  JPA's philosophy: "Load the minimum. Fetch more on demand."                     │
│  This is optimal for SINGLE entity access.                                       │
│  It becomes a problem for BULK access (loading many parents + children).         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**What each solution does to override JPA's default:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What happens when you add JOIN FETCH / @BatchSize / @EntityGraph:               │
│                                                                                  │
│  JPA's default behavior (LAZY):                                                  │
│    "Load parent. Create proxy for children. Load children only on access."       │
│    → Minimal initial load, but N+1 on bulk access.                               │
│                                                                                  │
│  JOIN FETCH:                                                                     │
│    "Override LAZY. Load parent AND children in ONE SQL with INNER JOIN."          │
│    → Maximum efficiency for known data needs.                                    │
│    → But: INNER JOIN excludes parents without children.                          │
│    → But: Can't use with Pageable (pagination done in memory).                   │
│    → But: Multiple JOIN FETCH on collections = Cartesian product.               │
│                                                                                  │
│  @BatchSize(size = N):                                                           │
│    "Keep LAZY, but when children are accessed, load them in batches of N."       │
│    → Doesn't change to 1 query, but reduces from N to ⌈N/batchSize⌉.            │
│    → Works with LAZY and EAGER both.                                             │
│    → Works with Pageable (no in-memory pagination issue).                        │
│    → Transparent — no change to repository methods.                              │
│    → But: Still more than 1 query.                                               │
│                                                                                  │
│  @EntityGraph(attributePaths = "..."):                                           │
│    "Override LAZY for THIS specific query. Fetch specified paths via LEFT JOIN." │
│    → Per-method control. Same entity, different fetch strategies per use case.   │
│    → Uses LEFT JOIN (includes parents without children).                         │
│    → Works with derived queries and @Query.                                      │
│    → Works with Pageable.                                                        │
│    → But: LEFT JOIN may return duplicate parents (use DISTINCT).                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code comparison — all three solutions side by side:**

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // PROBLEM — N+1 with regular JOIN (children not loaded)
    @Query("SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad WHERE ud.name = :name")
    List<UserDetails> findWithN1Problem(@Param("name") String name);
    // SQL 1: SELECT ud.* ... INNER JOIN ... WHERE name = ?      (1 query)
    // SQL 2..N: SELECT ua.* WHERE user_id = ?                    (N queries on access)

    // SOLUTION 1: JOIN FETCH — 1 query total
    @Query("SELECT ud FROM UserDetails ud JOIN FETCH ud.userAddressList WHERE ud.name = :name")
    List<UserDetails> findWithJoinFetch(@Param("name") String name);
    // SQL: SELECT ud.*, ua.* ... INNER JOIN ... WHERE name = ?   (1 query)

    // SOLUTION 2: @BatchSize — defined on entity, no repo change needed
    // Just use the same query as the N+1 version.
    // @BatchSize(size=10) on the entity field handles the batching.

    // SOLUTION 3: @EntityGraph — 1 query total (LEFT JOIN)
    @EntityGraph(attributePaths = "userAddressList")
    @Query("SELECT ud FROM UserDetails ud WHERE ud.name = :name")
    List<UserDetails> findWithEntityGraph(@Param("name") String name);
    // SQL: SELECT ud.*, ua.* ... LEFT JOIN ... WHERE name = ?    (1 query)
}
```

```text
Comparison table — all N+1 solutions:

  Scenario: 100 parents, each with children, @BatchSize(size = 25)

  ┌──────────────────────┬──────────┬────────────┬──────────────────────────────────┐
  │ Approach             │ Queries  │ JOIN type  │ Trade-offs                        │
  ├──────────────────────┼──────────┼────────────┼──────────────────────────────────┤
  │ No fix (N+1)         │ 101      │ —          │ Worst performance                │
  ├──────────────────────┼──────────┼────────────┼──────────────────────────────────┤
  │ JOIN FETCH           │ 1        │ INNER JOIN │ Best performance.                │
  │                      │          │            │ No Pageable support.             │
  │                      │          │            │ Excludes parents w/o children.   │
  │                      │          │            │ Cartesian product with multiple  │
  │                      │          │            │ collection fetches.              │
  ├──────────────────────┼──────────┼────────────┼──────────────────────────────────┤
  │ @BatchSize(25)       │ 5        │ —          │ Good performance.                │
  │                      │          │ (IN clause)│ Works with Pageable.             │
  │                      │          │            │ Transparent (entity-level).      │
  │                      │          │            │ Still multiple queries.           │
  ├──────────────────────┼──────────┼────────────┼──────────────────────────────────┤
  │ @EntityGraph         │ 1        │ LEFT JOIN  │ Best per-method control.         │
  │                      │          │            │ Works with Pageable.             │
  │                      │          │            │ Includes parents w/o children.   │
  │                      │          │            │ May need DISTINCT for dupes.     │
  └──────────────────────┴──────────┴────────────┴──────────────────────────────────┘

  When to use which:

  ┌──────────────────────────────────────────┬──────────────────────────────────────┐
  │ Use Case                                 │ Best Solution                        │
  ├──────────────────────────────────────────┼──────────────────────────────────────┤
  │ Single collection, no pagination needed  │ JOIN FETCH                           │
  │ Need pagination + eager child loading    │ @EntityGraph                         │
  │ Global optimization, minimal code change │ @BatchSize                           │
  │ Multiple collections on same entity      │ @BatchSize (avoid Cartesian product) │
  │ Different fetch strategies per use case  │ @EntityGraph (per-method control)    │
  └──────────────────────────────────────────┴──────────────────────────────────────┘
```

```text
How JPA's optimization philosophy changes with each solution:

  DEFAULT (LAZY — JPA's optimization):
  ┌──────────────────────────────────────────────────────────────────┐
  │  "I'll load the MINIMUM. If you need children, ask me later."   │
  │                                                                  │
  │  findAll() → SELECT users only                                   │
  │  user.getAddresses() → SELECT addresses WHERE user_id = ?       │
  │  user.getOrders() → SELECT orders WHERE user_id = ?             │
  │                                                                  │
  │  RESULT: Many small queries. Efficient if you don't need all    │
  │  relationships. Terrible if you do.                              │
  └──────────────────────────────────────────────────────────────────┘

  JOIN FETCH — "Override LAZY, load everything NOW in ONE query":
  ┌──────────────────────────────────────────────────────────────────┐
  │  SELECT u.*, a.* FROM users u                                    │
  │  INNER JOIN addresses a ON u.id = a.user_id                      │
  │                                                                  │
  │  You tell JPA: "I KNOW I need the addresses. Load them now."    │
  │  JPA abandons its lazy strategy for this query.                  │
  │  ONE big query instead of many small ones.                       │
  └──────────────────────────────────────────────────────────────────┘

  @BatchSize — "Keep LAZY, but be smarter about batch loading":
  ┌──────────────────────────────────────────────────────────────────┐
  │  SELECT users → 100 users loaded (addresses still lazy)          │
  │  user[0].getAddresses() triggered →                              │
  │    Instead of: SELECT WHERE user_id = 1 (just this one)         │
  │    Hibernate: SELECT WHERE user_id IN (1,2,...,25) (batch of 25)│
  │                                                                  │
  │  You tell JPA: "Stay lazy, but when you DO load, do it in bulk."│
  │  JPA optimizes the lazy loading itself.                          │
  └──────────────────────────────────────────────────────────────────┘

  @EntityGraph — "Override LAZY for THIS specific method":
  ┌──────────────────────────────────────────────────────────────────┐
  │  @EntityGraph(attributePaths = "userAddressList")                 │
  │  List<UserDetails> findByName(name);                             │
  │                                                                  │
  │  SELECT u.*, a.* FROM users u                                    │
  │  LEFT JOIN addresses a ON u.id = a.user_id                       │
  │  WHERE u.name = ?                                                │
  │                                                                  │
  │  You tell JPA: "For findByName, I need addresses. Fetch them."  │
  │  Other methods (findById, findAll) still use LAZY.               │
  │  Per-method override of fetch strategy.                          │
  └──────────────────────────────────────────────────────────────────┘
```

---


### What Is @NamedQuery?

`@NamedQuery` is a JPA annotation placed on the **entity class** (not the repository) that defines a **pre-compiled, named JPQL query**. The query is validated at application startup, giving you early error detection instead of runtime failures.

```java
@Entity
@Table(name = "user_details")
@NamedQuery(
    name = "UserDetails.findByName",
    query = "SELECT ud FROM UserDetails ud WHERE ud.name = :name"
)
@NamedQuery(
    name = "UserDetails.findByEmailAndActive",
    query = "SELECT ud FROM UserDetails ud WHERE ud.email = :email AND ud.active = true"
)
@NamedQuery(
    name = "UserDetails.countByCountry",
    query = "SELECT COUNT(ud) FROM UserDetails ud JOIN ud.userAddressList a WHERE a.country = :country"
)
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private Boolean active;

    @OneToMany(mappedBy = "userDetails", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAddress> userAddressList = new ArrayList<>();

    // getters, setters
}
```

**Using @NamedQuery in a repository:**

```java
// Spring Data JPA automatically matches @NamedQuery by convention:
//   Entity name + "." + method name = NamedQuery name
//   UserDetails.findByName matches → @NamedQuery(name = "UserDetails.findByName")

public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Spring finds @NamedQuery(name = "UserDetails.findByName") and uses its JPQL
    List<UserDetails> findByName(@Param("name") String name);
    // JPQL (from @NamedQuery): SELECT ud FROM UserDetails ud WHERE ud.name = :name
    // SQL: SELECT ud.* FROM user_details ud WHERE ud.name = ?

    // Spring finds @NamedQuery(name = "UserDetails.findByEmailAndActive")
    List<UserDetails> findByEmailAndActive(@Param("email") String email);
    // JPQL (from @NamedQuery): SELECT ud FROM UserDetails ud
    //                          WHERE ud.email = :email AND ud.active = true

    // Spring finds @NamedQuery(name = "UserDetails.countByCountry")
    long countByCountry(@Param("country") String country);
    // JPQL (from @NamedQuery): SELECT COUNT(ud) FROM UserDetails ud
    //                          JOIN ud.userAddressList a WHERE a.country = :country
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @NamedQuery — Resolution Order in Spring Data JPA:                              │
│                                                                                  │
│  When Spring sees a method in a repository, it looks for the query in this       │
│  order:                                                                          │
│                                                                                  │
│  1. @Query annotation on the method → highest priority                           │
│  2. @NamedQuery with name = "EntityName.methodName" → second priority            │
│  3. Derive query from method name (PartTree) → fallback                         │
│                                                                                  │
│  If you have BOTH @Query on the method AND a matching @NamedQuery:               │
│  → @Query wins. @NamedQuery is ignored.                                          │
│                                                                                  │
│  If you have a matching @NamedQuery AND the method name is derivable:            │
│  → @NamedQuery wins. Spring doesn't parse the method name.                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why should we use @NamedQuery?**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why use @NamedQuery:                                                            │
│                                                                                  │
│  1. STARTUP VALIDATION — queries are parsed and validated when the application   │
│     starts. If there's a typo in the JPQL (e.g., wrong field name), you get     │
│     an error at STARTUP, not at runtime when a user hits the API.                │
│                                                                                  │
│     @NamedQuery(query = "SELECT ud FROM UserDetails ud WHERE ud.naem = :name")   │
│                                                                      ↑ typo!     │
│     → Application FAILS TO START with: "Could not resolve property: naem"        │
│     → You catch the error BEFORE deployment.                                     │
│                                                                                  │
│     Compare with @Query on repository:                                           │
│     @Query also validates at startup → same benefit.                             │
│                                                                                  │
│  2. PRE-COMPILATION — Hibernate parses and compiles @NamedQuery ONCE at startup. │
│     On subsequent calls, it reuses the compiled query plan.                      │
│     @Query methods also get this benefit in practice.                            │
│                                                                                  │
│  3. CENTRALIZED on entity — queries live with the entity they operate on.        │
│     Good for: seeing all queries for an entity in one place.                     │
│     Bad for: entity class becomes cluttered with many @NamedQuery annotations.   │
│                                                                                  │
│  4. REUSABLE — multiple repository methods or EntityManager calls can use the    │
│     same @NamedQuery by name.                                                    │
│                                                                                  │
│  5. LEGACY / JPA STANDARD — @NamedQuery is part of the JPA specification.        │
│     Works with any JPA provider (Hibernate, EclipseLink, etc.).                  │
│     @Query is Spring Data JPA specific (not portable to non-Spring apps).        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
@NamedQuery vs @Query comparison:

  ┌──────────────────────────────────────┬────────────────────────────────────────┐
  │ @NamedQuery                          │ @Query                                 │
  ├──────────────────────────────────────┼────────────────────────────────────────┤
  │ Defined on the Entity class          │ Defined on the Repository method       │
  │ JPA standard annotation              │ Spring Data JPA annotation             │
  │ Validated at startup                 │ Validated at startup                   │
  │ Pre-compiled once                    │ Pre-compiled once                      │
  │ Name convention: Entity.methodName   │ Directly on the method (no naming)     │
  │ Can clutter entity with many queries │ Query stays with the method (cleaner)  │
  │ Reusable across multiple repos       │ Tied to one method                     │
  │ Portable to non-Spring JPA apps      │ Spring-specific                        │
  │ Matched by naming convention         │ Explicitly annotated                   │
  │ Older, JPA 1.0+ standard             │ Modern, Spring Data convenience        │
  ├──────────────────────────────────────┼────────────────────────────────────────┤
  │ Most teams prefer @Query on the      │ ← RECOMMENDED for Spring Data JPA     │
  │ repository. @NamedQuery is used in   │    projects. Query lives next to the   │
  │ legacy codebases or pure JPA apps.   │    method that uses it.                │
  └──────────────────────────────────────┴────────────────────────────────────────┘
```

---


### What Is a Native Query?

A **Native Query** is a raw SQL query that you write directly in the database's SQL dialect (MySQL, PostgreSQL, Oracle, etc.) instead of JPQL. It bypasses Hibernate's entity-based query translation and sends the SQL **directly to the database**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JPQL vs Native Query — The Key Difference:                                      │
│                                                                                  │
│  JPQL:                                                                           │
│    @Query("SELECT e FROM Employee e WHERE e.salary > :sal")                      │
│    → Works on ENTITY names and FIELD names                                       │
│    → Hibernate translates to SQL for YOUR database dialect                       │
│    → DB-independent: same query works on MySQL, PostgreSQL, Oracle               │
│    → Goes through Hibernate's query parser → entity model → SQL generator        │
│                                                                                  │
│  Native Query:                                                                   │
│    @Query(value = "SELECT * FROM employees WHERE salary > :sal",                 │
│           nativeQuery = true)                                                    │
│    → Works on TABLE names and COLUMN names                                       │
│    → SQL goes DIRECTLY to the database — no Hibernate translation               │
│    → DB-DEPENDENT: query may break if you switch databases                      │
│    → Bypasses Hibernate's query parser — you write the exact SQL                 │
│                                                                                  │
│  Flow comparison:                                                                │
│                                                                                  │
│  JPQL:                                                                           │
│    @Query("SELECT e FROM Employee e")                                            │
│       │                                                                          │
│       v                                                                          │
│    Hibernate JPQL Parser → reads entity metadata → generates SQL                │
│       │                                                                          │
│       v                                                                          │
│    SQL: SELECT e.id, e.emp_name, e.salary FROM employees e                       │
│       │                                                                          │
│       v                                                                          │
│    JDBC → Database                                                               │
│                                                                                  │
│  Native Query:                                                                   │
│    @Query(value = "SELECT * FROM employees", nativeQuery = true)                 │
│       │                                                                          │
│       v                                                                          │
│    Spring sends SQL DIRECTLY to JDBC (NO Hibernate JPQL parsing)                │
│       │                                                                          │
│       v                                                                          │
│    JDBC → Database                                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Native Query Is Plain SQL — Database Dependent

Since native queries use raw SQL with actual table and column names, they are **tied to the specific database**. If you switch from MySQL to PostgreSQL (or vice versa), your native queries may break because SQL dialects differ.

**Entity and Database Table:**

```java
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "active")
    private Boolean active;

    // getters, setters, constructors
}
```

```text
Database Table — user_details:

  ┌────┬───────────┬──────────────────┬────────────────┬────────┐
  │ id │ user_name │ email            │ phone          │ active │
  ├────┼───────────┼──────────────────┼────────────────┼────────┤
  │  1 │ Alice     │ alice@ex.com     │ +1-555-0101    │ true   │
  │  2 │ Bob       │ bob@ex.com       │ +1-555-0102    │ true   │
  │  3 │ Charlie   │ charlie@ex.com   │ +1-555-0103    │ false  │
  │  4 │ Diana     │ diana@ex.com     │ +1-555-0104    │ true   │
  └────┴───────────┴──────────────────┴────────────────┴────────┘
```

```java
// Repository — Native Query uses TABLE and COLUMN names
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // JPQL — uses entity field name "name" (Java field)
    @Query("SELECT u FROM UserDetails u WHERE u.name = :name")
    List<UserDetails> findByNameJpql(@Param("name") String name);
    // Hibernate generates: SELECT u.id, u.user_name, u.email, u.phone, u.active
    //                      FROM user_details u WHERE u.user_name = ?

    // Native Query — uses column name "user_name" (database column)
    @Query(value = "SELECT * FROM user_details WHERE user_name = :name",
           nativeQuery = true)
    List<UserDetails> findByNameNative(@Param("name") String name);
    // SQL sent directly: SELECT * FROM user_details WHERE user_name = ?
    // NO Hibernate translation — this IS the final SQL.
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why Native Query is DB-dependent — real examples:                                │
│                                                                                  │
│  MySQL:                                                                          │
│    SELECT * FROM user_details LIMIT 10 OFFSET 0                                  │
│    SELECT IF(active, 'Yes', 'No') FROM user_details                              │
│    SELECT JSON_EXTRACT(metadata, '$.key') FROM user_details                      │
│                                                                                  │
│  PostgreSQL:                                                                     │
│    SELECT * FROM user_details LIMIT 10 OFFSET 0  (same — lucky!)                │
│    SELECT CASE WHEN active THEN 'Yes' ELSE 'No' END FROM user_details            │
│    SELECT metadata->>'key' FROM user_details   (JSONB operator)                  │
│                                                                                  │
│  Oracle:                                                                         │
│    SELECT * FROM user_details WHERE ROWNUM <= 10  (no LIMIT!)                    │
│    SELECT DECODE(active, 1, 'Yes', 'No') FROM user_details                      │
│    SELECT JSON_VALUE(metadata, '$.key') FROM user_details                        │
│                                                                                  │
│  If you wrote a MySQL native query with LIMIT and later switched to Oracle:      │
│  → Query BREAKS at runtime. Oracle doesn't have LIMIT.                           │
│                                                                                  │
│  JPQL doesn't have this problem:                                                 │
│    JPQL: SELECT u FROM UserDetails u  (with Pageable)                            │
│    → MySQL:  ... LIMIT 10 OFFSET 0                                               │
│    → Oracle: ... FETCH FIRST 10 ROWS ONLY                                        │
│    → Hibernate handles the dialect difference automatically.                     │
│                                                                                  │
│  RULE: If your application might switch databases → prefer JPQL.                 │
│        If your application is locked to one DB → native query is fine.           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### No Caching, Lazy Loading, or Entity Lifecycle Management

When you use a native query, Hibernate's **Persistence Context** features are limited. The result may be mapped to entities, but certain behaviors are bypassed or weakened.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Native Query BYPASSES:                                                     │
│                                                                                  │
│  1. NO JPQL QUERY CACHE:                                                         │
│     JPQL queries can be cached by Hibernate's query cache (2nd-level cache).     │
│     Native queries are NOT eligible for JPQL query caching.                      │
│     Each call hits the database directly.                                        │
│                                                                                  │
│  2. LAZY LOADING MAY NOT WORK AS EXPECTED:                                       │
│     If your native SELECT returns only some columns:                             │
│       SELECT user_name, phone FROM user_details                                  │
│     The result is NOT a managed entity — it's raw data.                          │
│     Relationships like @OneToMany on UserDetails WON'T be lazy-loadable.        │
│     There's no proxy — just raw column values.                                   │
│                                                                                  │
│     If SELECT * is used and mapped to entity → entity IS managed.               │
│     Lazy loading works on managed entities returned from native queries.        │
│                                                                                  │
│  3. NO DIRTY CHECKING for partial results:                                       │
│     If you return a DTO or Object[] from native query → no entity managed.      │
│     Hibernate won't track changes. No automatic UPDATE on flush.                │
│     If you return a full entity (SELECT *) → dirty checking works.              │
│                                                                                  │
│  4. NO AUTOMATIC SQL TRANSLATION:                                                │
│     JPQL: Hibernate reads @Column(name="user_name") and maps field → column.    │
│     Native: YOU must use the correct column names in the SQL.                    │
│     If you rename a column in DB, JPQL auto-adjusts. Native breaks.             │
│                                                                                  │
│  5. NO ENTITY LIFECYCLE EVENTS for non-entity results:                           │
│     @PrePersist, @PostLoad, @PreUpdate etc. fire for managed entities.           │
│     If native query returns Object[] or DTO → no lifecycle events.              │
│     If native query returns full entity (SELECT *) → @PostLoad fires.           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Demonstration — entity lifecycle with native vs JPQL
@Entity
@Table(name = "user_details")
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "active")
    private Boolean active;

    @OneToMany(mappedBy = "userDetails", fetch = FetchType.LAZY)
    private List<UserAddress> userAddressList;

    @PostLoad
    public void onLoad() {
        System.out.println("Entity loaded: " + this.name);
    }

    // getters, setters
}
```

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Native — SELECT * → full entity → managed → lazy loading works, @PostLoad fires
    @Query(value = "SELECT * FROM user_details WHERE active = true", nativeQuery = true)
    List<UserDetails> findActiveNative();
    // Returns managed entities. Lazy loading of userAddressList works.
    // @PostLoad fires for each entity.

    // Native — partial columns → NOT a managed entity → no lazy loading
    @Query(value = "SELECT user_name, phone FROM user_details WHERE active = true",
           nativeQuery = true)
    List<Object[]> findNameAndPhoneNative();
    // Returns Object[] — NOT entities.
    // No lazy loading, no dirty checking, no @PostLoad.
    // Just raw data: Object[0] = "Alice", Object[1] = "+1-555-0101"
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JPQL vs Native — Feature Comparison:                                            │
│                                                                                  │
│  ┌────────────────────────────────┬──────────────────┬──────────────────────────┐│
│  │ Feature                        │ JPQL             │ Native Query             ││
│  ├────────────────────────────────┼──────────────────┼──────────────────────────┤│
│  │ Query cache (2nd level)        │ ✓ Supported      │ ✗ Not supported          ││
│  │ Lazy loading on full entity    │ ✓ Works          │ ✓ Works (SELECT *)       ││
│  │ Lazy loading on partial result │ N/A              │ ✗ No proxy               ││
│  │ Dirty checking (full entity)   │ ✓ Entity managed │ ✓ Entity managed         ││
│  │ Dirty checking (partial cols)  │ N/A              │ ✗ Not managed            ││
│  │ @PostLoad lifecycle event      │ ✓ Fires          │ ✓ Only for SELECT *      ││
│  │ Auto column name resolution    │ ✓ From @Column   │ ✗ You write column names ││
│  │ DB independence                │ ✓ Dialect handles│ ✗ DB-specific SQL        ││
│  └────────────────────────────────┴──────────────────┴──────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### When to Use Native Query Over JPQL

#### 1. Database-Specific Features (JSONB, Full-Text Search, Window Functions)

JPQL doesn't support database-specific operators like PostgreSQL's JSONB operators, MySQL's `MATCH AGAINST`, or SQL window functions. For these, you need native queries.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // PostgreSQL JSONB — not possible in JPQL
    @Query(value = "SELECT * FROM user_details WHERE metadata @> '{\"role\": \"admin\"}'::jsonb",
           nativeQuery = true)
    List<UserDetails> findAdminsByJsonMetadata();
    // SQL: SELECT * FROM user_details WHERE metadata @> '{"role": "admin"}'::jsonb
    // The @> operator (contains) and ::jsonb cast are PostgreSQL-specific.
    // JPQL has NO equivalent for JSONB operators.

    // PostgreSQL full-text search — not possible in JPQL
    @Query(value = "SELECT * FROM user_details WHERE to_tsvector('english', user_name) @@ to_tsquery(:term)",
           nativeQuery = true)
    List<UserDetails> fullTextSearch(@Param("term") String term);
    // SQL: SELECT * FROM user_details
    //      WHERE to_tsvector('english', user_name) @@ to_tsquery('alice')

    // SQL Window Function — not possible in JPQL
    @Query(value = "SELECT *, ROW_NUMBER() OVER (ORDER BY user_name) as row_num FROM user_details",
           nativeQuery = true)
    List<Object[]> findWithRowNumber();
    // SQL: SELECT *, ROW_NUMBER() OVER (ORDER BY user_name) as row_num FROM user_details
    // Window functions (ROW_NUMBER, RANK, DENSE_RANK, LAG, LEAD) are not in JPQL.

    // MySQL-specific — MATCH AGAINST for full-text search
    @Query(value = "SELECT * FROM user_details WHERE MATCH(user_name) AGAINST(:keyword IN BOOLEAN MODE)",
           nativeQuery = true)
    List<UserDetails> mysqlFullTextSearch(@Param("keyword") String keyword);
}
```

```text
Database:

  user_details
  ┌────┬───────────┬──────────────────┬────────────────┬────────┬──────────────────────────┐
  │ id │ user_name │ email            │ phone          │ active │ metadata (JSONB)         │
  ├────┼───────────┼──────────────────┼────────────────┼────────┼──────────────────────────┤
  │  1 │ Alice     │ alice@ex.com     │ +1-555-0101    │ true   │ {"role":"admin","lvl":5}  │
  │  2 │ Bob       │ bob@ex.com       │ +1-555-0102    │ true   │ {"role":"user","lvl":2}   │
  │  3 │ Charlie   │ charlie@ex.com   │ +1-555-0103    │ false  │ {"role":"admin","lvl":3}  │
  └────┴───────────┴──────────────────┴────────────────┴────────┴──────────────────────────┘

  findAdminsByJsonMetadata() → returns Alice (id=1) and Charlie (id=3)
  because their metadata contains {"role": "admin"}
```

#### 2. Non-Entity Results — COUNT(*), JOINs Without Relationships

JPQL can only query entities and their mapped relationships. If you need to JOIN two tables that have **no JPA relationship** (no `@OneToMany`, no `@ManyToOne`), or if you need aggregate queries that don't map to any entity, you need native queries.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // COUNT(*) with GROUP BY — returns non-entity result
    @Query(value = "SELECT active, COUNT(*) as total FROM user_details GROUP BY active",
           nativeQuery = true)
    List<Object[]> countByActiveStatus();
    // SQL: SELECT active, COUNT(*) as total FROM user_details GROUP BY active
    //
    // Result:
    //   Object[0] = { true, 3 }   → 3 active users
    //   Object[1] = { false, 1 }  → 1 inactive user
    //
    // This COUNT(*) with GROUP BY returning raw columns doesn't map to an entity.

    // JOIN tables WITHOUT any @Entity relationship
    // Suppose "audit_logs" table exists but has NO @Entity or JPA relationship to user_details.
    @Query(value = "SELECT u.user_name, a.action, a.timestamp " +
                   "FROM user_details u " +
                   "JOIN audit_logs a ON u.id = a.user_id " +
                   "WHERE a.action = :action",
           nativeQuery = true)
    List<Object[]> findUserActionsFromAuditLog(@Param("action") String action);
    // SQL: SELECT u.user_name, a.action, a.timestamp
    //      FROM user_details u
    //      JOIN audit_logs a ON u.id = a.user_id
    //      WHERE a.action = 'LOGIN'
    //
    // JPQL CANNOT do this JOIN because there is no @ManyToOne or @OneToMany
    // between UserDetails and AuditLog entities.
    // Native query JOINs on raw foreign keys — no relationship mapping needed.

    // Subquery with NOT EXISTS — complex logic
    @Query(value = "SELECT u.* FROM user_details u " +
                   "WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id)",
           nativeQuery = true)
    List<UserDetails> findUsersWithNoOrders();
    // Returns users who have never placed an order.
    // "orders" table may not have a JPA entity at all.
}
```

```text
Database tables (no JPA relationship between them):

  user_details                           audit_logs (NO @Entity)
  ┌────┬───────────┬──────────────────┐  ┌────┬─────────┬────────────┬─────────────────────┐
  │ id │ user_name │ email            │  │ id │ user_id │ action     │ timestamp           │
  ├────┼───────────┼──────────────────┤  ├────┼─────────┼────────────┼─────────────────────┤
  │  1 │ Alice     │ alice@ex.com     │  │ 10 │    1    │ LOGIN      │ 2026-04-18 10:00:00 │
  │  2 │ Bob       │ bob@ex.com       │  │ 11 │    1    │ LOGOUT     │ 2026-04-18 11:00:00 │
  │  3 │ Charlie   │ charlie@ex.com   │  │ 12 │    2    │ LOGIN      │ 2026-04-18 09:00:00 │
  └────┴───────────┴──────────────────┘  └────┴─────────┴────────────┴─────────────────────┘

  findUserActionsFromAuditLog("LOGIN") returns:
    Object[] { "Alice",   "LOGIN", "2026-04-18 10:00:00" }
    Object[] { "Bob",     "LOGIN", "2026-04-18 09:00:00" }

  JPQL cannot express: FROM UserDetails u JOIN audit_logs a ON u.id = a.user_id
  because audit_logs is not mapped as a JPA relationship on UserDetails entity.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why JPQL can't do this:                                                         │
│                                                                                  │
│  JPQL JOIN requires a mapped relationship:                                       │
│    "SELECT u FROM UserDetails u JOIN u.addresses a"                              │
│                                      ↑                                           │
│                                u.addresses must be a @OneToMany or               │
│                                @ManyToMany field on the UserDetails entity.       │
│                                                                                  │
│  You CANNOT write in JPQL:                                                       │
│    "SELECT u FROM UserDetails u JOIN AuditLog a ON u.id = a.userId"              │
│    → Compilation error: "unexpected token: ON"                                   │
│    → JPQL doesn't support arbitrary ON clauses for unrelated entities            │
│      (JPA 2.1+ supports ON for additional conditions, but the entities           │
│       must still be related via a mapped association)                             │
│                                                                                  │
│  Native query has no such restriction:                                           │
│    "SELECT u.*, a.* FROM user_details u JOIN audit_logs a ON u.id = a.user_id"   │
│    → Works! SQL can JOIN any tables by any column.                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 3. Query Efficiency — Bulk Operations Bypass Persistence Context

JPQL `@Modifying` queries also bypass the entity lifecycle, but native queries are even more efficient for bulk operations because they skip JPQL parsing entirely. Additionally, JPQL keeps the Persistence Context updated (with `flushAutomatically`/`clearAutomatically`), adding overhead. Native queries go straight to the database.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Native bulk UPDATE — fastest possible, no JPQL parsing, no entity overhead
    @Modifying
    @Query(value = "UPDATE user_details SET active = false WHERE id IN (:ids)",
           nativeQuery = true)
    int bulkDeactivateNative(@Param("ids") List<Long> ids);
    // SQL: UPDATE user_details SET active = false WHERE id IN (1, 2, 3, ...)
    // → Sent directly to DB. No JPQL parsing. No Hibernate entity involvement.
    // → Returns int = number of rows updated.

    // Native bulk DELETE
    @Modifying
    @Query(value = "DELETE FROM user_details WHERE active = false AND " +
                   "id NOT IN (SELECT user_id FROM orders)",
           nativeQuery = true)
    int deleteInactiveUsersWithNoOrders();
    // SQL: DELETE FROM user_details WHERE active = false
    //      AND id NOT IN (SELECT user_id FROM orders)
    // → Complex subquery in DELETE — easier in native than JPQL.

    // Native INSERT (JPQL doesn't support INSERT...VALUES at all)
    @Modifying
    @Query(value = "INSERT INTO user_details (user_name, email, phone, active) " +
                   "VALUES (:name, :email, :phone, true)",
           nativeQuery = true)
    int insertUser(@Param("name") String name,
                   @Param("email") String email,
                   @Param("phone") String phone);
    // SQL: INSERT INTO user_details (user_name, email, phone, active)
    //      VALUES ('Alice', 'alice@ex.com', '+1-555-0101', true)
    // JPQL does NOT support INSERT...VALUES. Only INSERT...SELECT.
}
```

```java
// Service — @Transactional required for @Modifying
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional
    public int deactivateUsers(List<Long> ids) {
        return userDetailsRepository.bulkDeactivateNative(ids);
        // Fastest path: SQL goes directly to DB.
        // No JPQL parsing, no entity loading, no Persistence Context involvement.
        // WARNING: L1 cache is STALE after this. Use clearAutomatically = true
        // on @Modifying if you read these entities afterwards.
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Performance: JPQL @Modifying vs Native @Modifying:                              │
│                                                                                  │
│  JPQL @Modifying:                                                                │
│    @Modifying                                                                    │
│    @Query("UPDATE UserDetails u SET u.active = false WHERE u.id IN :ids")        │
│       │                                                                          │
│       v                                                                          │
│    1. JPQL Parser → parses "UPDATE UserDetails u SET..."                         │
│    2. Entity Metadata → resolves UserDetails → user_details table                │
│    3. SQL Generator → generates: UPDATE user_details SET active = false ...       │
│    4. (optional) flush() if flushAutomatically = true                            │
│    5. JDBC → executes SQL on DB                                                  │
│    6. (optional) clear() if clearAutomatically = true                            │
│    → Overhead: JPQL parsing + entity metadata lookup + SQL generation            │
│                                                                                  │
│  Native @Modifying:                                                              │
│    @Modifying                                                                    │
│    @Query(value = "UPDATE user_details SET active = false WHERE id IN (:ids)",   │
│           nativeQuery = true)                                                    │
│       │                                                                          │
│       v                                                                          │
│    1. JDBC → executes SQL on DB directly                                         │
│    → No JPQL parsing, no entity metadata lookup, no SQL generation               │
│    → Fastest possible path                                                       │
│                                                                                  │
│  For single queries, the difference is negligible.                               │
│  For bulk operations on millions of rows, native is noticeably faster.           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### How to Use Native Query — @Query(value = "...", nativeQuery = true)

The syntax is simple: add `nativeQuery = true` to the `@Query` annotation.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Basic native query — find all active users
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           nativeQuery = true)
    List<UserDetails> findAllActiveNative();
    // SQL sent to DB: SELECT * FROM user_details WHERE active = true
    // Result: mapped to List<UserDetails> entities

    // Native query with named parameter
    @Query(value = "SELECT * FROM user_details WHERE user_name = :name AND active = :active",
           nativeQuery = true)
    List<UserDetails> findByNameAndActiveNative(@Param("name") String name,
                                                @Param("active") Boolean active);
    // SQL: SELECT * FROM user_details WHERE user_name = 'Alice' AND active = true

    // Native query with positional parameter
    @Query(value = "SELECT * FROM user_details WHERE user_name = ?1 AND email = ?2",
           nativeQuery = true)
    UserDetails findByNameAndEmailNative(String name, String email);
    // SQL: SELECT * FROM user_details WHERE user_name = 'Alice' AND email = 'alice@ex.com'

    // Native query with LIKE
    @Query(value = "SELECT * FROM user_details WHERE user_name LIKE %:keyword%",
           nativeQuery = true)
    List<UserDetails> searchByNameNative(@Param("keyword") String keyword);
    // SQL: SELECT * FROM user_details WHERE user_name LIKE '%ali%'

    // Native query returning single value
    @Query(value = "SELECT COUNT(*) FROM user_details WHERE active = true",
           nativeQuery = true)
    long countActiveUsersNative();
    // SQL: SELECT COUNT(*) FROM user_details WHERE active = true
    // Returns: 3
}
```

```java
// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public List<UserDetails> getActiveUsers() {
        return userDetailsRepository.findAllActiveNative();
    }

    public List<UserDetails> searchUsers(String keyword) {
        return userDetailsRepository.searchByNameNative(keyword);
    }

    public long getActiveCount() {
        return userDetailsRepository.countActiveUsersNative();
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/active")
    public List<UserDetails> getActive() {
        return userService.getActiveUsers();
    }

    @GetMapping("/search")
    public List<UserDetails> search(@RequestParam String keyword) {
        return userService.searchUsers(keyword);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @Query(nativeQuery = true) — How Spring processes it:                           │
│                                                                                  │
│  @Query(value = "SELECT * FROM user_details WHERE active = true",                │
│         nativeQuery = true)                                                      │
│  List<UserDetails> findAllActiveNative();                                        │
│       │                                                                          │
│       v                                                                          │
│  Spring Proxy:                                                                   │
│    sees nativeQuery = true                                                       │
│    → Does NOT parse as JPQL                                                      │
│    → Creates a NativeQuery via entityManager.createNativeQuery(sql, class)       │
│       │                                                                          │
│       v                                                                          │
│  Hibernate:                                                                      │
│    → Does NOT translate entity names → table names (already raw SQL)             │
│    → Sends SQL directly to JDBC PreparedStatement                                │
│       │                                                                          │
│       v                                                                          │
│  JDBC executes: SELECT * FROM user_details WHERE active = true                   │
│       │                                                                          │
│       v                                                                          │
│  ResultSet returned → Hibernate maps columns to entity fields:                   │
│    id → UserDetails.id                                                           │
│    user_name → UserDetails.name (via @Column(name="user_name"))                  │
│    email → UserDetails.email                                                     │
│    phone → UserDetails.phone                                                     │
│    active → UserDetails.active                                                   │
│       │                                                                          │
│       v                                                                          │
│  Returns List<UserDetails> — managed entities in Persistence Context             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### SELECT * — How Native Query Maps to Entity

When you write `SELECT * FROM user_details` in a native query and the return type is `List<UserDetails>`, Hibernate uses the entity's `@Column` annotations to map each column from the ResultSet to the corresponding Java field.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(value = "SELECT * FROM user_details", nativeQuery = true)
    List<UserDetails> findAllNative();
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  SELECT * FROM user_details — Column to Entity mapping:                          │
│                                                                                  │
│  SQL Result (from SELECT *):                                                     │
│  ┌────┬───────────┬──────────────────┬────────────────┬────────┐                 │
│  │ id │ user_name │ email            │ phone          │ active │                 │
│  ├────┼───────────┼──────────────────┼────────────────┼────────┤                 │
│  │  1 │ Alice     │ alice@ex.com     │ +1-555-0101    │ true   │                 │
│  │  2 │ Bob       │ bob@ex.com       │ +1-555-0102    │ true   │                 │
│  └────┴───────────┴──────────────────┴────────────────┴────────┘                 │
│                                                                                  │
│  Hibernate reads the ResultSet column names and matches with @Column:            │
│                                                                                  │
│    ResultSet column    @Column annotation          Java field                    │
│    ───────────────     ─────────────────────       ──────────                    │
│    id              →   @Id                     →   Long id                       │
│    user_name       →   @Column(name="user_name") → String name                  │
│    email           →   @Column(name="email")   →   String email                 │
│    phone           →   @Column(name="phone")   →   String phone                 │
│    active          →   @Column(name="active")  →   Boolean active               │
│                                                                                  │
│  Result: Fully populated UserDetails entities.                                   │
│  Entities are MANAGED in Persistence Context (L1 cache).                         │
│  Dirty checking works. Lazy loading of relationships works.                      │
│  @PostLoad lifecycle event fires.                                                │
│                                                                                  │
│  This is identical to what JPQL "SELECT u FROM UserDetails u" produces,          │
│  except the SQL was written by YOU instead of generated by Hibernate.            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// Service — entity is fully managed
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Transactional
    public void demonstrateNativeSelectStar() {
        List<UserDetails> users = userDetailsRepository.findAllNative();
        // SELECT * returns all columns → full entity mapping

        UserDetails alice = users.get(0);
        System.out.println(alice.getName());   // "Alice" — field populated
        System.out.println(alice.getEmail());   // "alice@ex.com" — field populated
        System.out.println(alice.getPhone());   // "+1-555-0101" — field populated

        // Entity is MANAGED → dirty checking works
        alice.setName("Alice Updated");
        // At transaction commit: Hibernate detects the change
        // → SQL: UPDATE user_details SET user_name = 'Alice Updated' WHERE id = 1
        // This happens automatically — same as with JPQL.

        // Lazy loading of relationships also works
        // alice.getUserAddressList() → triggers lazy SQL query
    }
}
```

---

### Partial SELECT — Will It Map to Entity?

If you write `SELECT user_name, phone FROM user_details` (only some columns, not all), it **CANNOT** be directly mapped to the `UserDetails` entity. Hibernate expects ALL columns to populate the entity. Missing columns cause an error or null values depending on the approach.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // THIS WILL FAIL — partial columns, return type is entity
    @Query(value = "SELECT user_name, phone FROM user_details", nativeQuery = true)
    List<UserDetails> findNameAndPhoneAsEntity();  // ← RUNTIME ERROR!
    // Hibernate tries to map ResultSet to UserDetails entity.
    // ResultSet has only 2 columns: user_name, phone
    // UserDetails needs: id, user_name, email, phone, active
    // → Missing columns (id, email, active)
    // → Exception: "Could not read entity" or "Column 'id' not found"
    //
    // Hibernate CANNOT create a managed entity without the @Id column (id).
    // Even if other columns were nullable, the primary key is mandatory.
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Partial SELECT → Entity mapping — WHY it fails:                                 │
│                                                                                  │
│  SELECT user_name, phone FROM user_details                                       │
│                                                                                  │
│  ResultSet:                                                                      │
│  ┌───────────┬────────────────┐                                                  │
│  │ user_name │ phone          │                                                  │
│  ├───────────┼────────────────┤                                                  │
│  │ Alice     │ +1-555-0101    │                                                  │
│  │ Bob       │ +1-555-0102    │                                                  │
│  └───────────┴────────────────┘                                                  │
│                                                                                  │
│  Hibernate tries to build UserDetails:                                           │
│    id     = ??? → NOT in ResultSet → ERROR! @Id is mandatory                     │
│    name   = "Alice" → mapped from user_name column ✓                             │
│    email  = ??? → NOT in ResultSet → null or error                               │
│    phone  = "+1-555-0101" → mapped from phone column ✓                           │
│    active = ??? → NOT in ResultSet → null or error                               │
│                                                                                  │
│  Without the @Id (primary key), Hibernate CANNOT:                                │
│    - Register the entity in the Persistence Context                              │
│    - Track changes (dirty checking)                                              │
│    - Maintain identity (two entities with same id are the same object)           │
│                                                                                  │
│  RESULT: RuntimeException at query execution time.                               │
│                                                                                  │
│  Solutions for partial SELECT:                                                   │
│    1. Use List<Object[]> as return type                                          │
│    2. Use @SqlResultSetMapping + @NamedNativeQuery → DTO mapping                 │
│    3. Use interface-based projection (Spring Data)                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Partial SELECT → DTO Using @SqlResultSetMapping + @NamedNativeQuery

When you need to map partial columns to a DTO class, you can use the JPA standard approach: `@SqlResultSetMapping` with `@ConstructorResult` on the entity class, combined with `@NamedNativeQuery` that references the mapping.

**Step 1: Create the DTO class:**

```java
// DTO — plain POJO, NOT an @Entity
public class UserPhoneDTO {
    private String userName;
    private String phone;

    // Constructor — parameter order and types must match @ColumnResult declarations
    public UserPhoneDTO(String userName, String phone) {
        this.userName = userName;
        this.phone = phone;
    }

    // getters
    public String getUserName() { return userName; }
    public String getPhone() { return phone; }
}
```

**Step 2: Add @SqlResultSetMapping and @NamedNativeQuery on the entity:**

```java
@Entity
@Table(name = "user_details")
@SqlResultSetMapping(
    name = "UserPhoneDTOMapping",                          // ← mapping name (referenced by @NamedNativeQuery)
    classes = @ConstructorResult(
        targetClass = UserPhoneDTO.class,                  // ← DTO class to construct
        columns = {
            @ColumnResult(name = "user_name", type = String.class),  // ← 1st constructor param
            @ColumnResult(name = "phone", type = String.class)       // ← 2nd constructor param
        }
    )
)
@NamedNativeQuery(
    name = "UserDetails.findUserPhone",                     // ← query name (Entity.methodName convention)
    query = "SELECT user_name, phone FROM user_details",    // ← raw SQL
    resultSetMapping = "UserPhoneDTOMapping"                // ← links to @SqlResultSetMapping above
)
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "active")
    private Boolean active;

    // getters, setters
}
```

**Step 3: Repository method matches @NamedNativeQuery name:**

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Spring matches "UserDetails.findUserPhone" → @NamedNativeQuery on entity
    List<UserPhoneDTO> findUserPhone();
    // The method name "findUserPhone" + entity name "UserDetails"
    // → looks for @NamedNativeQuery(name = "UserDetails.findUserPhone")
    // → executes: SELECT user_name, phone FROM user_details
    // → maps result using @SqlResultSetMapping("UserPhoneDTOMapping")
    // → calls: new UserPhoneDTO(resultSet.getString("user_name"), resultSet.getString("phone"))
    // → returns List<UserPhoneDTO>

    // With WHERE condition — add another @NamedNativeQuery
    // (need to add on entity: @NamedNativeQuery(name="UserDetails.findUserPhoneByActive", ...))
    List<UserPhoneDTO> findUserPhoneByActive(@Param("active") Boolean active);
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @SqlResultSetMapping + @NamedNativeQuery — Flow:                                │
│                                                                                  │
│  Entity class (UserDetails):                                                     │
│    @SqlResultSetMapping(                                                         │
│      name = "UserPhoneDTOMapping",                                               │
│      classes = @ConstructorResult(                                                │
│        targetClass = UserPhoneDTO.class,                                         │
│        columns = { @ColumnResult(name="user_name"), @ColumnResult(name="phone") }│
│      )                                                                           │
│    )                                                                             │
│    @NamedNativeQuery(                                                            │
│      name = "UserDetails.findUserPhone",                                         │
│      query = "SELECT user_name, phone FROM user_details",                        │
│      resultSetMapping = "UserPhoneDTOMapping"                                    │
│    )                                                                             │
│                                                                                  │
│  Repository: List<UserPhoneDTO> findUserPhone();                                 │
│       │                                                                          │
│       v                                                                          │
│  Spring sees method "findUserPhone" on UserDetails repository                    │
│    → Looks for @NamedNativeQuery(name = "UserDetails.findUserPhone") ✓           │
│       │                                                                          │
│       v                                                                          │
│  Executes: SELECT user_name, phone FROM user_details                             │
│       │                                                                          │
│       v                                                                          │
│  ResultSet:                                                                      │
│    ┌───────────┬────────────────┐                                                │
│    │ user_name │ phone          │                                                │
│    ├───────────┼────────────────┤                                                │
│    │ Alice     │ +1-555-0101    │                                                │
│    │ Bob       │ +1-555-0102    │                                                │
│    └───────────┴────────────────┘                                                │
│       │                                                                          │
│       v                                                                          │
│  @ConstructorResult mapping:                                                     │
│    new UserPhoneDTO("Alice", "+1-555-0101")    → UserPhoneDTO[0]                 │
│    new UserPhoneDTO("Bob",   "+1-555-0102")    → UserPhoneDTO[1]                 │
│       │                                                                          │
│       v                                                                          │
│  Returns: List<UserPhoneDTO> with 2 elements                                    │
│                                                                                  │
│  Column matching:                                                                │
│    @ColumnResult(name = "user_name") → ResultSet column "user_name" → 1st param  │
│    @ColumnResult(name = "phone")     → ResultSet column "phone"     → 2nd param  │
│    ORDER in @ColumnResult = ORDER of constructor parameters                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**With a WHERE condition — multiple @NamedNativeQuery on the same entity:**

```java
@Entity
@Table(name = "user_details")
@SqlResultSetMapping(
    name = "UserPhoneDTOMapping",
    classes = @ConstructorResult(
        targetClass = UserPhoneDTO.class,
        columns = {
            @ColumnResult(name = "user_name", type = String.class),
            @ColumnResult(name = "phone", type = String.class)
        }
    )
)
@NamedNativeQuery(
    name = "UserDetails.findUserPhone",
    query = "SELECT user_name, phone FROM user_details",
    resultSetMapping = "UserPhoneDTOMapping"
)
@NamedNativeQuery(
    name = "UserDetails.findUserPhoneByActive",
    query = "SELECT user_name, phone FROM user_details WHERE active = :active",
    resultSetMapping = "UserPhoneDTOMapping"
)
public class UserDetails {
    // ... fields same as above
}
```

```java
// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public List<UserPhoneDTO> getAllUserPhones() {
        List<UserPhoneDTO> results = userDetailsRepository.findUserPhone();
        // SQL: SELECT user_name, phone FROM user_details
        // → List<UserPhoneDTO> — type-safe, no manual casting

        for (UserPhoneDTO dto : results) {
            System.out.println(dto.getUserName() + " → " + dto.getPhone());
            // Alice → +1-555-0101
            // Bob → +1-555-0102
        }
        return results;
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/phones")
    public List<UserPhoneDTO> getPhones() {
        return userService.getAllUserPhones();
        // JSON: [{"userName":"Alice","phone":"+1-555-0101"}, {"userName":"Bob","phone":"+1-555-0102"}]
    }
}
```

---

### Partial SELECT → List<Object[]> and Manual DTO Conversion

A simpler (but less type-safe) approach is to return `List<Object[]>` from the repository and convert to the DTO manually in the service layer.

```java
// Repository — returns raw Object[]
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(value = "SELECT user_name, phone FROM user_details",
           nativeQuery = true)
    List<Object[]> findUserNameAndPhoneRaw();
    // SQL: SELECT user_name, phone FROM user_details
    //
    // Result: List<Object[]>
    //   Object[0] = { "Alice",   "+1-555-0101" }
    //   Object[1] = { "Bob",     "+1-555-0102" }
    //   Object[2] = { "Charlie", "+1-555-0103" }

    @Query(value = "SELECT user_name, phone FROM user_details WHERE active = :active",
           nativeQuery = true)
    List<Object[]> findUserNameAndPhoneByActiveRaw(@Param("active") Boolean active);
}
```

```java
// DTO class — same as before
public class UserPhoneDTO {
    private String userName;
    private String phone;

    public UserPhoneDTO(String userName, String phone) {
        this.userName = userName;
        this.phone = phone;
    }

    public String getUserName() { return userName; }
    public String getPhone() { return phone; }
}
```

```java
// Service — conversion happens HERE in the service layer
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public List<UserPhoneDTO> getAllUserPhones() {
        List<Object[]> rawResults = userDetailsRepository.findUserNameAndPhoneRaw();
        // SQL: SELECT user_name, phone FROM user_details
        //
        // rawResults:
        //   [0] = Object[] { "Alice",   "+1-555-0101" }
        //   [1] = Object[] { "Bob",     "+1-555-0102" }

        // Convert Object[] → UserPhoneDTO manually
        List<UserPhoneDTO> dtos = rawResults.stream()
            .map(row -> new UserPhoneDTO(
                (String) row[0],    // user_name (index 0 matches SELECT order)
                (String) row[1]     // phone     (index 1 matches SELECT order)
            ))
            .collect(Collectors.toList());

        // dtos:
        //   [0] = UserPhoneDTO { userName="Alice",   phone="+1-555-0101" }
        //   [1] = UserPhoneDTO { userName="Bob",     phone="+1-555-0102" }

        return dtos;
    }

    // Alternative — using a loop instead of stream
    public List<UserPhoneDTO> getAllUserPhonesLoop() {
        List<Object[]> rawResults = userDetailsRepository.findUserNameAndPhoneRaw();
        List<UserPhoneDTO> dtos = new ArrayList<>();

        for (Object[] row : rawResults) {
            String userName = (String) row[0];
            String phone = (String) row[1];
            dtos.add(new UserPhoneDTO(userName, phone));
        }

        return dtos;
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/phones")
    public List<UserPhoneDTO> getPhones() {
        return userService.getAllUserPhones();
        // JSON: [{"userName":"Alice","phone":"+1-555-0101"}, ...]
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Object[] mapping — positional (same as JPQL Object[]):                          │
│                                                                                  │
│  SELECT user_name, phone FROM user_details                                       │
│         ↑ index 0   ↑ index 1                                                    │
│                                                                                  │
│  Object[] row = { "Alice", "+1-555-0101" }                                       │
│                    row[0]   row[1]                                                │
│                                                                                  │
│  Conversion location: SERVICE LAYER                                              │
│    Repository → returns List<Object[]>                                           │
│    Service → converts Object[] → UserPhoneDTO                                    │
│    Controller → receives List<UserPhoneDTO>                                      │
│                                                                                  │
│  Flow:                                                                           │
│  ┌───────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │ Repository │    │  Service     │    │  Service     │    │ Controller   │      │
│  │ returns    │ →  │  receives    │ →  │  converts    │ →  │ receives     │      │
│  │ Object[]   │    │  Object[]    │    │  to DTO      │    │ DTO list     │      │
│  └───────────┘    └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                                  │
│  Drawbacks of Object[] approach (same as with JPQL):                             │
│  - No compile-time type safety                                                   │
│  - Manual casting: (String) row[0]                                               │
│  - Index-based, fragile: if SELECT order changes, all indexes break              │
│  - Not self-documenting: what is row[0]?                                         │
│                                                                                  │
│  When to use Object[] vs @SqlResultSetMapping:                                   │
│  ┌─────────────────────────────────┬───────────────────────────────────────────┐ │
│  │ List<Object[]>                  │ @SqlResultSetMapping + @NamedNativeQuery  │ │
│  ├─────────────────────────────────┼───────────────────────────────────────────┤ │
│  │ Quick, simple                   │ More setup, more annotations              │ │
│  │ No extra annotations on entity  │ Annotations on entity class               │ │
│  │ Manual casting in service       │ Automatic DTO construction                │ │
│  │ Fragile (index-based)           │ Robust (column-name based)                │ │
│  │ Good for prototyping            │ Good for production                       │ │
│  └─────────────────────────────────┴───────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Pagination and Sorting in Native Queries

Native queries support both `Pageable` (pagination) and `Sort` (sorting), but with important differences from JPQL.

**Pagination with PageRequest:**

For pagination, you must provide a `countQuery` parameter because Spring **cannot auto-generate** a count query from raw SQL (it can for JPQL because it understands the entity model).

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // Paginated native query — countQuery is REQUIRED for Page<T> return type
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           countQuery = "SELECT COUNT(*) FROM user_details WHERE active = true",
           nativeQuery = true)
    Page<UserDetails> findActiveUsersPaginated(Pageable pageable);
    // SQL 1 (data):  SELECT * FROM user_details WHERE active = true LIMIT 10 OFFSET 0
    // SQL 2 (count): SELECT COUNT(*) FROM user_details WHERE active = true
    //
    // countQuery is mandatory for Page<T>.
    // Without it: Spring cannot derive COUNT from raw SQL → exception.

    // With WHERE condition and parameters
    @Query(value = "SELECT * FROM user_details WHERE user_name LIKE %:keyword%",
           countQuery = "SELECT COUNT(*) FROM user_details WHERE user_name LIKE %:keyword%",
           nativeQuery = true)
    Page<UserDetails> searchUsersPaginated(@Param("keyword") String keyword, Pageable pageable);

    // Slice — no countQuery needed (Slice doesn't need total count)
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           nativeQuery = true)
    Slice<UserDetails> findActiveUsersSliced(Pageable pageable);
    // SQL: SELECT * FROM user_details WHERE active = true LIMIT 11 OFFSET 0
    //      (fetches size+1 rows to determine hasNext)

    // List — no countQuery needed
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           nativeQuery = true)
    List<UserDetails> findActiveUsersList(Pageable pageable);
    // SQL: SELECT * FROM user_details WHERE active = true LIMIT 10 OFFSET 0
}
```

**Sorting in Native Queries:**

Sorting with the `Sort` parameter object does **NOT work** with native queries in most Spring Data JPA versions. Spring cannot validate sort properties against entity metadata for raw SQL. You must include `ORDER BY` directly in the SQL string.

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    // SORTING — include ORDER BY directly in the SQL (NOT via Sort parameter)
    @Query(value = "SELECT * FROM user_details WHERE active = true ORDER BY user_name ASC",
           nativeQuery = true)
    List<UserDetails> findActiveUsersSortedByName();
    // SQL: SELECT * FROM user_details WHERE active = true ORDER BY user_name ASC
    // Sort is HARDCODED in the SQL. Not dynamic.

    // Dynamic sorting — use Sort via Pageable (works for simple column names)
    @Query(value = "SELECT * FROM user_details WHERE active = true",
           countQuery = "SELECT COUNT(*) FROM user_details WHERE active = true",
           nativeQuery = true)
    Page<UserDetails> findActiveUsersSorted(Pageable pageable);
    // When Pageable contains Sort info, Spring appends ORDER BY to the SQL.
    // BUT: Sort property names must match COLUMN names (not entity field names).
    //
    // Sort.by("user_name") → works (user_name is the actual column name)
    // Sort.by("name") → FAILS (name is the Java field, not the column name)
    //
    // This is the opposite of JPQL where you use Java field names for sorting.

    // For complex sorting that can't be expressed via Pageable:
    @Query(value = "SELECT * FROM user_details WHERE active = true " +
                   "ORDER BY active DESC, user_name ASC",
           nativeQuery = true)
    List<UserDetails> findWithComplexSort();
}
```

```java
// Service
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    // Pagination only
    public Page<UserDetails> getActiveUsersPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userDetailsRepository.findActiveUsersPaginated(pageable);
        // SQL 1: SELECT * FROM user_details WHERE active = true LIMIT 10 OFFSET 0
        // SQL 2: SELECT COUNT(*) FROM user_details WHERE active = true
    }

    // Pagination + Sorting (dynamic via Pageable)
    public Page<UserDetails> getActiveUsersPaginatedAndSorted(int page, int size) {
        // IMPORTANT: Use COLUMN names for Sort, not Java field names!
        Sort sort = Sort.by(Sort.Direction.DESC, "user_name");  // ← column name
        Pageable pageable = PageRequest.of(page, size, sort);
        return userDetailsRepository.findActiveUsersSorted(pageable);
        // SQL 1: SELECT * FROM user_details WHERE active = true
        //        ORDER BY user_name DESC
        //        LIMIT 10 OFFSET 0
        // SQL 2: SELECT COUNT(*) FROM user_details WHERE active = true
    }

    // Multiple sort columns
    public Page<UserDetails> getActiveUsersMultiSort(int page, int size) {
        Sort sort = Sort.by(
            Sort.Order.asc("user_name"),   // ← column name (not "name")
            Sort.Order.desc("id")           // ← column name
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        return userDetailsRepository.findActiveUsersSorted(pageable);
        // SQL: SELECT * FROM user_details WHERE active = true
        //      ORDER BY user_name ASC, id DESC
        //      LIMIT 10 OFFSET 0
    }

    // Pagination with Object[] result (partial columns)
    public List<Object[]> getUserPhonesPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userDetailsRepository.findUserNameAndPhonePaginated(pageable);
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/active")
    public Page<UserDetails> getActiveUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "user_name") String sortBy,
        @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        // NOTE: sortBy must be a COLUMN name for native queries
        Pageable pageable = PageRequest.of(page, size, sort);
        return userService.getActiveUsersPaginatedAndSorted(page, size);
    }
}
```

```java
// Pagination with partial columns (Object[]) — needs countQuery too
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(value = "SELECT user_name, phone FROM user_details WHERE active = true",
           countQuery = "SELECT COUNT(*) FROM user_details WHERE active = true",
           nativeQuery = true)
    Page<Object[]> findUserNameAndPhonePaginated(Pageable pageable);
    // SQL 1: SELECT user_name, phone FROM user_details WHERE active = true LIMIT 10 OFFSET 0
    // SQL 2: SELECT COUNT(*) FROM user_details WHERE active = true
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pagination + Sorting in Native Query — Key Differences from JPQL:               │
│                                                                                  │
│  ┌──────────────────────────────────┬────────────────────────────────────────────┐│
│  │ Feature                          │ JPQL                │ Native Query         ││
│  ├──────────────────────────────────┼─────────────────────┼──────────────────────┤│
│  │ Pageable parameter               │ ✓ Works             │ ✓ Works              ││
│  │ countQuery                       │ Auto-generated      │ MUST be provided     ││
│  │                                  │ (or custom)         │ for Page<T>          ││
│  │ Sort via Pageable                │ Uses Java FIELD     │ Uses DB COLUMN       ││
│  │                                  │ names               │ names                ││
│  │ Sort via Sort parameter alone    │ ✓ Works             │ ✗ Often fails        ││
│  │ ORDER BY in query string         │ ✓ Possible          │ ✓ Recommended for    ││
│  │                                  │                     │ complex sorts        ││
│  │ LIMIT/OFFSET generation          │ Auto from Pageable  │ Auto from Pageable   ││
│  │ Slice<T> (no count)              │ ✓ Works             │ ✓ Works              ││
│  │ Page<T> (with count)             │ ✓ Auto count        │ ✓ Needs countQuery   ││
│  └──────────────────────────────────┴─────────────────────┴──────────────────────┘│
│                                                                                  │
│  CRITICAL DIFFERENCE — Sort property names:                                      │
│                                                                                  │
│  JPQL:   Sort.by("name")      → works (Java field name)                         │
│  Native: Sort.by("name")      → FAILS (no column called "name")                 │
│  Native: Sort.by("user_name") → works (actual column name)                      │
│                                                                                  │
│  CRITICAL DIFFERENCE — countQuery:                                               │
│                                                                                  │
│  JPQL:                                                                           │
│    @Query("SELECT u FROM UserDetails u WHERE u.active = true")                   │
│    Page<UserDetails> findActive(Pageable pageable);                              │
│    → Spring auto-generates: SELECT COUNT(u) FROM UserDetails u WHERE ...         │
│                                                                                  │
│  Native:                                                                         │
│    @Query(value = "SELECT * FROM user_details WHERE active = true",              │
│           nativeQuery = true)                                                    │
│    Page<UserDetails> findActive(Pageable pageable);                              │
│    → Spring CANNOT auto-generate count from raw SQL → EXCEPTION                  │
│    → You MUST provide: countQuery = "SELECT COUNT(*) FROM user_details ..."      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Complete flow — Native Query with Pagination + Sorting:

  Client: GET /api/users/active?page=1&size=5&sortBy=user_name&direction=DESC
     │
     v
  Controller: extracts params → creates Sort and Pageable
    Sort sort = Sort.by(Sort.Direction.DESC, "user_name");   ← COLUMN name!
    Pageable pageable = PageRequest.of(1, 5, sort);
     │
     v
  Service: calls repository
    userDetailsRepository.findActiveUsersSorted(pageable);
     │
     v
  Spring Proxy:
    sees @Query(nativeQuery = true) → treats as raw SQL
    Original SQL: SELECT * FROM user_details WHERE active = true
    Appends Sort → ORDER BY user_name DESC
    Appends Pagination → LIMIT 5 OFFSET 5
     │
     v
  SQL 1 (data):
    SELECT * FROM user_details
    WHERE active = true
    ORDER BY user_name DESC
    LIMIT 5 OFFSET 5
     │
  SQL 2 (count):
    SELECT COUNT(*) FROM user_details WHERE active = true
     │
     v
  Results:
    Page<UserDetails> {
      content: [user6, user7, user8, user9, user10],  ← page 1 (0-indexed)
      pageNumber: 1,
      pageSize: 5,
      totalElements: 12,
      totalPages: 3,
      sort: Sort { orders: [Order(user_name, DESC)] }
    }
     │
     v
  Controller returns → Jackson serializes to JSON → Client receives response
```

```text
Summary — When to use Native Query vs JPQL vs Derived Query:

  ┌──────────────────────────────────┬──────────────────────────────────────────────┐
  │ Scenario                         │ Best Approach                                │
  ├──────────────────────────────────┼──────────────────────────────────────────────┤
  │ Simple CRUD / findBy conditions  │ Derived Query (method name)                  │
  │ Complex WHERE, JOINs on entities │ JPQL (@Query)                                │
  │ DB-specific features (JSONB,     │ Native Query (@Query nativeQuery=true)        │
  │   full-text, window functions)   │                                              │
  │ JOIN tables without relationship │ Native Query                                 │
  │ Bulk INSERT...VALUES             │ Native Query (JPQL can't do INSERT VALUES)   │
  │ Bulk UPDATE/DELETE               │ Either (Native slightly faster)              │
  │ Aggregates with GROUP BY         │ Native Query or JPQL (both work)             │
  │ Partial columns → DTO            │ JPQL (NEW constructor) or Native (Object[],  │
  │                                  │   @SqlResultSetMapping)                      │
  │ Database portability needed      │ JPQL (DB-independent)                         │
  │ Maximum performance              │ Native Query (no JPQL parsing overhead)       │
  └──────────────────────────────────┴──────────────────────────────────────────────┘
```

---


### What Is a Dynamic Native Query?

A **dynamic native query** is a raw SQL query that is built **at runtime** in the service layer using `EntityManager`, rather than being defined statically in a repository with `@Query`. This lets you construct the SQL string conditionally based on method arguments — adding or removing WHERE clauses, JOIN conditions, ORDER BY, and LIMIT/OFFSET dynamically.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Static @Query (Repository) vs Dynamic Query (EntityManager):                    │
│                                                                                  │
│  Static — @Query on Repository:                                                  │
│    @Query(value = "SELECT * FROM user_details WHERE active = true",              │
│           nativeQuery = true)                                                    │
│    List<UserDetails> findActive();                                               │
│    → Query is FIXED at compile time. Cannot add/remove conditions dynamically.   │
│    → If you need 10 different filter combinations, you need 10 methods.          │
│                                                                                  │
│  Dynamic — EntityManager in Service:                                             │
│    String sql = "SELECT * FROM user_details WHERE 1=1";                          │
│    if (name != null) sql += " AND user_name = :name";                            │
│    if (active != null) sql += " AND active = :active";                           │
│    Query query = entityManager.createNativeQuery(sql, UserDetails.class);        │
│    → Query is BUILT at runtime based on which parameters are provided.           │
│    → One method handles ANY combination of filters.                              │
│                                                                                  │
│  When to use Dynamic Native Query:                                               │
│    - Search/filter APIs with many optional parameters                            │
│    - Dynamic report generation where columns/conditions change                   │
│    - Admin dashboards with configurable filters                                  │
│    - Any scenario where the number of WHERE conditions is unknown at compile time│
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Database Table — user_details (used in all examples below):

  ┌────┬───────────┬──────────────────┬────────────────┬────────┐
  │ id │ user_name │ email            │ phone          │ active │
  ├────┼───────────┼──────────────────┼────────────────┼────────┤
  │  1 │ Alice     │ alice@ex.com     │ +1-555-0101    │ true   │
  │  2 │ Bob       │ bob@ex.com       │ +1-555-0102    │ true   │
  │  3 │ Charlie   │ charlie@ex.com   │ +1-555-0103    │ false  │
  │  4 │ Diana     │ diana@ex.com     │ +1-555-0104    │ true   │
  │  5 │ Eve       │ eve@ex.com       │ +1-555-0105    │ false  │
  │  6 │ Frank     │ frank@ex.com     │ +1-555-0106    │ true   │
  │  7 │ Grace     │ grace@ex.com     │ +1-555-0107    │ true   │
  │  8 │ Hank      │ hank@ex.com      │ +1-555-0108    │ false  │
  └────┴───────────┴──────────────────┴────────────────┴────────┘
```

---

### @PersistenceContext and EntityManager

`@PersistenceContext` injects the JPA `EntityManager` — the core JPA interface for interacting with the Persistence Context. You use it in service classes to create and execute queries programmatically.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @PersistenceContext vs @Autowired for EntityManager:                             │
│                                                                                  │
│  @PersistenceContext                                                             │
│  private EntityManager entityManager;                                            │
│  → JPA standard annotation.                                                      │
│  → Injects a PROXY that is bound to the current transaction's Persistence Context│
│  → Thread-safe: each request gets its own Persistence Context.                   │
│  → RECOMMENDED for EntityManager injection.                                      │
│                                                                                  │
│  @Autowired                                                                      │
│  private EntityManager entityManager;                                            │
│  → Spring-specific. Also works because Spring registers EntityManager as a bean. │
│  → Same proxy behavior in practice.                                              │
│  → But @PersistenceContext is the JPA-standard way.                              │
│                                                                                  │
│  Key EntityManager methods for native queries:                                   │
│    entityManager.createNativeQuery(sql)                                          │
│      → returns Query object for raw SQL                                          │
│    entityManager.createNativeQuery(sql, EntityClass.class)                       │
│      → returns Query object that maps results to an entity                       │
│    query.setParameter("name", value)                                             │
│      → binds a named parameter                                                   │
│    query.setParameter(1, value)                                                  │
│      → binds a positional parameter                                              │
│    query.getResultList()                                                         │
│      → executes SELECT, returns List                                             │
│    query.getSingleResult()                                                       │
│      → executes SELECT, returns single object                                    │
│    query.executeUpdate()                                                         │
│      → executes INSERT/UPDATE/DELETE, returns int (affected rows)                │
│    query.setFirstResult(offset)                                                  │
│      → sets the starting row (OFFSET)                                            │
│    query.setMaxResults(limit)                                                    │
│      → sets max rows to return (LIMIT)                                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Basic Dynamic Native Query — createNativeQuery + setParameter

**Building a query dynamically based on which parameters are non-null:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic search — only filters that are non-null are added to the query.
     * Any combination of name, email, active can be provided.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<UserDetails> searchUsers(String name, String email, Boolean active) {

        // Step 1: Start building the SQL string
        StringBuilder sql = new StringBuilder("SELECT * FROM user_details WHERE 1=1");
        //                                                                    ↑
        //  "WHERE 1=1" is a trick so every subsequent condition can use "AND ..."
        //  Without it, the first condition would need "WHERE" and others "AND".
        //  1=1 is always true, so it doesn't affect results.

        // Step 2: Conditionally add WHERE clauses
        if (name != null && !name.isEmpty()) {
            sql.append(" AND user_name = :name");
        }
        if (email != null && !email.isEmpty()) {
            sql.append(" AND email = :email");
        }
        if (active != null) {
            sql.append(" AND active = :active");
        }

        // Step 3: Create the native query with entity class mapping
        Query query = entityManager.createNativeQuery(sql.toString(), UserDetails.class);
        //                                             ↑ raw SQL       ↑ map to entity

        // Step 4: Bind parameters (only the ones we added to the SQL)
        if (name != null && !name.isEmpty()) {
            query.setParameter("name", name);
        }
        if (email != null && !email.isEmpty()) {
            query.setParameter("email", email);
        }
        if (active != null) {
            query.setParameter("active", active);
        }

        // Step 5: Execute and return
        return query.getResultList();
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users/search?name=Alice
    // GET /api/users/search?active=true
    // GET /api/users/search?name=Alice&active=true
    // GET /api/users/search?name=Alice&email=alice@ex.com&active=true
    // All combinations work with the SAME endpoint!
    @GetMapping("/search")
    public List<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Boolean active
    ) {
        return userService.searchUsers(name, email, active);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dynamic Query Building — Example Scenarios:                                     │
│                                                                                  │
│  Scenario 1: searchUsers("Alice", null, null)                                    │
│    Built SQL: SELECT * FROM user_details WHERE 1=1 AND user_name = :name         │
│    Bound:     :name = "Alice"                                                    │
│    Final SQL: SELECT * FROM user_details WHERE 1=1 AND user_name = 'Alice'       │
│    Result:    [UserDetails(id=1, name="Alice", ...)]                             │
│                                                                                  │
│  Scenario 2: searchUsers(null, null, true)                                       │
│    Built SQL: SELECT * FROM user_details WHERE 1=1 AND active = :active          │
│    Bound:     :active = true                                                     │
│    Final SQL: SELECT * FROM user_details WHERE 1=1 AND active = true             │
│    Result:    [Alice, Bob, Diana, Frank, Grace] — 5 active users                 │
│                                                                                  │
│  Scenario 3: searchUsers("Bob", "bob@ex.com", true)                              │
│    Built SQL: SELECT * FROM user_details WHERE 1=1                               │
│               AND user_name = :name AND email = :email AND active = :active      │
│    Bound:     :name = "Bob", :email = "bob@ex.com", :active = true               │
│    Final SQL: SELECT * FROM user_details WHERE 1=1                               │
│               AND user_name = 'Bob' AND email = 'bob@ex.com' AND active = true   │
│    Result:    [UserDetails(id=2, name="Bob", ...)]                               │
│                                                                                  │
│  Scenario 4: searchUsers(null, null, null)                                       │
│    Built SQL: SELECT * FROM user_details WHERE 1=1                               │
│    Final SQL: SELECT * FROM user_details WHERE 1=1                               │
│    Result:    All 8 users (no filters applied)                                   │
│                                                                                  │
│  ONE method handles ALL combinations — no need for multiple @Query methods!      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Flow — Dynamic Native Query:                                                    │
│                                                                                  │
│  Controller: GET /api/users/search?name=Alice&active=true                        │
│       │                                                                          │
│       v                                                                          │
│  Service: searchUsers("Alice", null, true)                                       │
│       │                                                                          │
│       v                                                                          │
│  Build SQL string dynamically:                                                   │
│    "SELECT * FROM user_details WHERE 1=1"                                        │
│     + " AND user_name = :name"      ← name is non-null                           │
│     (email is null → skipped)                                                    │
│     + " AND active = :active"       ← active is non-null                         │
│       │                                                                          │
│       v                                                                          │
│  entityManager.createNativeQuery(sql, UserDetails.class)                         │
│       │                                                                          │
│       v                                                                          │
│  query.setParameter("name", "Alice")                                             │
│  query.setParameter("active", true)                                              │
│       │                                                                          │
│       v                                                                          │
│  query.getResultList()                                                           │
│       │                                                                          │
│       v                                                                          │
│  Hibernate sends to DB:                                                          │
│    SELECT * FROM user_details                                                    │
│    WHERE 1=1 AND user_name = 'Alice' AND active = true                           │
│       │                                                                          │
│       v                                                                          │
│  ResultSet → mapped to UserDetails entity (SELECT * → full entity)               │
│       │                                                                          │
│       v                                                                          │
│  Returns List<UserDetails> → Controller → JSON response                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Returning Object[] instead of Entity (partial columns):**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<UserPhoneDTO> searchUserPhones(String name, Boolean active) {

        // Select only specific columns — returns Object[], NOT entity
        StringBuilder sql = new StringBuilder("SELECT user_name, phone FROM user_details WHERE 1=1");

        if (name != null) {
            sql.append(" AND user_name LIKE :name");
        }
        if (active != null) {
            sql.append(" AND active = :active");
        }

        // No entity class → returns List<Object[]>
        Query query = entityManager.createNativeQuery(sql.toString());
        //                                             ↑ no second parameter = Object[] result

        if (name != null) {
            query.setParameter("name", "%" + name + "%");
        }
        if (active != null) {
            query.setParameter("active", active);
        }

        List<Object[]> results = query.getResultList();

        // Convert Object[] → DTO in service layer
        return results.stream()
            .map(row -> new UserPhoneDTO((String) row[0], (String) row[1]))
            .collect(Collectors.toList());
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  createNativeQuery — Two Overloads:                                              │
│                                                                                  │
│  1. entityManager.createNativeQuery(sql, UserDetails.class)                      │
│     → SQL must SELECT all entity columns (SELECT *)                              │
│     → ResultSet mapped to entity automatically                                   │
│     → Returns managed entities (dirty checking, lazy loading work)               │
│     → getResultList() returns List<UserDetails>                                  │
│                                                                                  │
│  2. entityManager.createNativeQuery(sql)                                         │
│     → SQL can select ANY columns                                                 │
│     → ResultSet returned as raw Object[]                                         │
│     → NOT managed entities — just raw data                                       │
│     → getResultList() returns List<Object[]>                                     │
│     → You convert to DTO manually in service layer                               │
│                                                                                  │
│  IMPORTANT — SQL Injection Protection:                                           │
│    ALWAYS use setParameter() for user inputs.                                    │
│    NEVER concatenate user values directly into SQL:                              │
│                                                                                  │
│    ✗ WRONG (SQL injection):                                                      │
│      sql.append(" AND user_name = '" + name + "'");                              │
│      → If name = "'; DROP TABLE user_details; --"                                │
│      → SQL: ... AND user_name = ''; DROP TABLE user_details; --'                 │
│      → TABLE DELETED!                                                            │
│                                                                                  │
│    ✓ CORRECT (parameterized):                                                    │
│      sql.append(" AND user_name = :name");                                       │
│      query.setParameter("name", name);                                           │
│      → JDBC escapes the value. SQL injection impossible.                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Dynamic Native Query with Pagination and Sorting

`EntityManager` provides `setFirstResult(offset)` and `setMaxResults(limit)` for pagination. For sorting, you append `ORDER BY` to the SQL string dynamically.

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic search with pagination and sorting.
     * @param name     optional filter
     * @param active   optional filter
     * @param page     0-indexed page number
     * @param size     page size
     * @param sortBy   column name to sort by (e.g., "user_name", "id", "email")
     * @param sortDir  sort direction ("ASC" or "DESC")
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Page<UserDetails> searchUsersPaginated(String name, Boolean active,
                                                   int page, int size,
                                                   String sortBy, String sortDir) {

        // === DATA QUERY ===
        StringBuilder dataSql = new StringBuilder("SELECT * FROM user_details WHERE 1=1");

        // === COUNT QUERY (for Page metadata) ===
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM user_details WHERE 1=1");

        // Conditionally add WHERE clauses to BOTH queries
        if (name != null && !name.isEmpty()) {
            dataSql.append(" AND user_name LIKE :name");
            countSql.append(" AND user_name LIKE :name");
        }
        if (active != null) {
            dataSql.append(" AND active = :active");
            countSql.append(" AND active = :active");
        }

        // Add ORDER BY (sorting) — only to data query, NOT count query
        // IMPORTANT: Validate sortBy against a whitelist to prevent SQL injection!
        List<String> allowedSortColumns = List.of("id", "user_name", "email", "phone", "active");
        if (sortBy != null && allowedSortColumns.contains(sortBy)) {
            String direction = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
            dataSql.append(" ORDER BY ").append(sortBy).append(" ").append(direction);
        } else {
            dataSql.append(" ORDER BY id ASC");  // default sort
        }

        // Create data query
        Query dataQuery = entityManager.createNativeQuery(dataSql.toString(), UserDetails.class);

        // Create count query
        Query countQuery = entityManager.createNativeQuery(countSql.toString());

        // Bind parameters to BOTH queries
        if (name != null && !name.isEmpty()) {
            dataQuery.setParameter("name", "%" + name + "%");
            countQuery.setParameter("name", "%" + name + "%");
        }
        if (active != null) {
            dataQuery.setParameter("active", active);
            countQuery.setParameter("active", active);
        }

        // Apply pagination — OFFSET and LIMIT
        dataQuery.setFirstResult(page * size);   // OFFSET = page * size
        dataQuery.setMaxResults(size);            // LIMIT = size

        // Execute both queries
        List<UserDetails> content = dataQuery.getResultList();
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        // Build and return Page object
        Pageable pageable = PageRequest.of(page, size, Sort.by(
            "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy != null ? sortBy : "id"
        ));
        return new PageImpl<>(content, pageable, totalElements);
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users/search?active=true&page=0&size=3&sortBy=user_name&sortDir=DESC
    @GetMapping("/search")
    public Page<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        return userService.searchUsersPaginated(name, active, page, size, sortBy, sortDir);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dynamic Query with Pagination + Sorting — Scenario:                             │
│                                                                                  │
│  Request: GET /api/users/search?active=true&page=1&size=2&sortBy=user_name       │
│                                                      &sortDir=ASC                │
│                                                                                  │
│  Built data SQL:                                                                 │
│    SELECT * FROM user_details                                                    │
│    WHERE 1=1 AND active = :active                                                │
│    ORDER BY user_name ASC                                                        │
│                                                                                  │
│  After setParameter + pagination:                                                │
│    SELECT * FROM user_details                                                    │
│    WHERE 1=1 AND active = true                                                   │
│    ORDER BY user_name ASC                                                        │
│    LIMIT 2 OFFSET 2                                                              │
│         ↑ size    ↑ page(1) × size(2)                                            │
│                                                                                  │
│  Built count SQL:                                                                │
│    SELECT COUNT(*) FROM user_details                                             │
│    WHERE 1=1 AND active = true                                                   │
│    → Returns: 5 (Alice, Bob, Diana, Frank, Grace)                                │
│                                                                                  │
│  Active users sorted by name: Alice(1), Bob(2), Diana(4), Frank(6), Grace(7)    │
│  Page 0 (OFFSET 0, LIMIT 2): [Alice, Bob]                                       │
│  Page 1 (OFFSET 2, LIMIT 2): [Diana, Frank]  ← THIS page                        │
│  Page 2 (OFFSET 4, LIMIT 2): [Grace]                                            │
│                                                                                  │
│  Response:                                                                       │
│    Page<UserDetails> {                                                           │
│      content: [Diana, Frank],                                                    │
│      pageNumber: 1,                                                              │
│      pageSize: 2,                                                                │
│      totalElements: 5,                                                           │
│      totalPages: 3                                                               │
│    }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pagination Methods — setFirstResult / setMaxResults:                            │
│                                                                                  │
│  query.setFirstResult(offset)                                                    │
│    → Translates to SQL OFFSET                                                    │
│    → Skips the first N rows                                                      │
│    → Page 0: setFirstResult(0)   → OFFSET 0                                     │
│    → Page 1: setFirstResult(10)  → OFFSET 10  (if size = 10)                    │
│    → Page 2: setFirstResult(20)  → OFFSET 20                                    │
│    → Formula: offset = page × size                                               │
│                                                                                  │
│  query.setMaxResults(limit)                                                      │
│    → Translates to SQL LIMIT                                                     │
│    → Returns at most N rows                                                      │
│    → setMaxResults(10) → LIMIT 10                                                │
│                                                                                  │
│  Combined:                                                                       │
│    query.setFirstResult(page * size);                                            │
│    query.setMaxResults(size);                                                    │
│    → SQL: ... LIMIT size OFFSET (page * size)                                    │
│                                                                                  │
│  Hibernate translates to DB-specific syntax:                                     │
│    MySQL/PostgreSQL: LIMIT 10 OFFSET 20                                          │
│    Oracle: OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY                               │
│    SQL Server: OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY                           │
│                                                                                  │
│  NOTE: For COUNT query, do NOT set pagination — you want the TOTAL count.        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Sorting — SQL Injection Protection for ORDER BY:                                │
│                                                                                  │
│  setParameter() CANNOT be used for ORDER BY column names:                        │
│    query.setParameter("sort", "user_name")                                       │
│    SQL: ... ORDER BY :sort → becomes ORDER BY 'user_name' → ERROR!               │
│    Parameterized values are treated as STRING LITERALS, not column identifiers.  │
│                                                                                  │
│  You MUST concatenate column name into the SQL string:                            │
│    sql.append(" ORDER BY ").append(sortBy).append(" ASC")                        │
│                                                                                  │
│  But this opens up SQL injection if sortBy comes from user input!                │
│    sortBy = "user_name; DROP TABLE user_details; --"                             │
│    SQL: ... ORDER BY user_name; DROP TABLE user_details; -- ASC                  │
│                                                                                  │
│  SOLUTION: Validate sortBy against a WHITELIST of allowed column names:          │
│    List<String> allowed = List.of("id", "user_name", "email", "phone");          │
│    if (allowed.contains(sortBy)) {                                               │
│        sql.append(" ORDER BY ").append(sortBy);                                  │
│    }                                                                             │
│    → Only pre-approved column names can be used.                                 │
│    → Unknown values are rejected.                                                │
│    → SQL injection impossible.                                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Complex Queries Using Dynamic Native Query

Dynamic native queries shine when you need complex search logic with optional JOINs, subqueries, GROUP BY, HAVING, or database-specific features — all built conditionally.

**Example 1: Multi-table search with optional JOINs:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Complex search: filter by user fields AND optionally by address fields.
     * If no address filters are provided, no JOIN is added (faster query).
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<UserDetails> advancedSearch(String name, Boolean active,
                                             String city, String country) {

        StringBuilder sql = new StringBuilder("SELECT DISTINCT u.* FROM user_details u");

        // Only JOIN addresses if address filters are provided
        boolean needsAddressJoin = (city != null && !city.isEmpty())
                                || (country != null && !country.isEmpty());
        if (needsAddressJoin) {
            sql.append(" JOIN user_addresses a ON u.id = a.user_id");
        }

        sql.append(" WHERE 1=1");

        if (name != null && !name.isEmpty()) {
            sql.append(" AND u.user_name LIKE :name");
        }
        if (active != null) {
            sql.append(" AND u.active = :active");
        }
        if (city != null && !city.isEmpty()) {
            sql.append(" AND a.city = :city");
        }
        if (country != null && !country.isEmpty()) {
            sql.append(" AND a.country = :country");
        }

        Query query = entityManager.createNativeQuery(sql.toString(), UserDetails.class);

        if (name != null && !name.isEmpty()) {
            query.setParameter("name", "%" + name + "%");
        }
        if (active != null) {
            query.setParameter("active", active);
        }
        if (city != null && !city.isEmpty()) {
            query.setParameter("city", city);
        }
        if (country != null && !country.isEmpty()) {
            query.setParameter("country", country);
        }

        return query.getResultList();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Complex Query — Dynamic JOIN Scenarios:                                         │
│                                                                                  │
│  Database Tables:                                                                │
│                                                                                  │
│  user_details                         user_addresses                             │
│  ┌────┬───────────┬────────┐          ┌────┬──────────────┬──────────┬─────────┐ │
│  │ id │ user_name │ active │          │ id │ street       │ city     │ country │ │
│  ├────┼───────────┼────────┤          ├────┼──────────────┼──────────┼─────────┤ │
│  │  1 │ Alice     │ true   │          │ 10 │ 123 Main St  │ New York │ USA     │ │
│  │  2 │ Bob       │ true   │          │ 11 │ 456 Oak Ave  │ London   │ UK      │ │
│  │  3 │ Charlie   │ false  │          │ 12 │ 789 Pine Rd  │ Mumbai   │ India   │ │
│  └────┴───────────┴────────┘          └────┴──────────────┴──────────┴─────────┘ │
│                                                                                  │
│  Scenario 1: advancedSearch("Alice", null, null, null)                           │
│    SQL: SELECT DISTINCT u.* FROM user_details u                                  │
│         WHERE 1=1 AND u.user_name LIKE '%Alice%'                                 │
│    → NO JOIN (address filters not provided → faster query)                       │
│                                                                                  │
│  Scenario 2: advancedSearch(null, true, "London", null)                          │
│    SQL: SELECT DISTINCT u.* FROM user_details u                                  │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│         WHERE 1=1 AND u.active = true AND a.city = 'London'                      │
│    → JOIN added because city filter is present                                   │
│                                                                                  │
│  Scenario 3: advancedSearch("Bob", true, "London", "UK")                         │
│    SQL: SELECT DISTINCT u.* FROM user_details u                                  │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│         WHERE 1=1 AND u.user_name LIKE '%Bob%'                                   │
│         AND u.active = true AND a.city = 'London' AND a.country = 'UK'           │
│    → All filters applied, JOIN included                                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 2: Aggregation with GROUP BY and HAVING:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic report: count users per city, with optional filters.
     * Returns aggregated data — not entities.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Object[]> getUserCountByCity(Boolean active, Integer minCount) {

        StringBuilder sql = new StringBuilder(
            "SELECT a.city, a.country, COUNT(DISTINCT u.id) as user_count " +
            "FROM user_details u " +
            "JOIN user_addresses a ON u.id = a.user_id " +
            "WHERE 1=1"
        );

        if (active != null) {
            sql.append(" AND u.active = :active");
        }

        sql.append(" GROUP BY a.city, a.country");

        if (minCount != null && minCount > 0) {
            sql.append(" HAVING COUNT(DISTINCT u.id) >= :minCount");
        }

        sql.append(" ORDER BY user_count DESC");

        Query query = entityManager.createNativeQuery(sql.toString());

        if (active != null) {
            query.setParameter("active", active);
        }
        if (minCount != null && minCount > 0) {
            query.setParameter("minCount", minCount);
        }

        return query.getResultList();
        // Returns: List<Object[]>
        //   Object[] { "New York", "USA", 15 }
        //   Object[] { "London",   "UK",  12 }
        //   Object[] { "Mumbai",   "India", 8 }
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Aggregation Query — Scenarios:                                                  │
│                                                                                  │
│  Scenario 1: getUserCountByCity(true, null)                                      │
│    SQL: SELECT a.city, a.country, COUNT(DISTINCT u.id) as user_count             │
│         FROM user_details u                                                      │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│         WHERE 1=1 AND u.active = true                                            │
│         GROUP BY a.city, a.country                                               │
│         ORDER BY user_count DESC                                                 │
│    → Counts active users per city, no minimum threshold                          │
│                                                                                  │
│  Scenario 2: getUserCountByCity(null, 10)                                        │
│    SQL: SELECT a.city, a.country, COUNT(DISTINCT u.id) as user_count             │
│         FROM user_details u                                                      │
│         JOIN user_addresses a ON u.id = a.user_id                                │
│         WHERE 1=1                                                                │
│         GROUP BY a.city, a.country                                               │
│         HAVING COUNT(DISTINCT u.id) >= 10                                        │
│         ORDER BY user_count DESC                                                 │
│    → All users, only cities with 10+ users                                       │
│                                                                                  │
│  Scenario 3: getUserCountByCity(true, 5)                                         │
│    SQL: ... WHERE 1=1 AND u.active = true                                        │
│         GROUP BY ... HAVING COUNT(...) >= 5                                       │
│         ORDER BY user_count DESC                                                 │
│    → Active users, only cities with 5+ active users                              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 3: Dynamic UPDATE with EntityManager:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic bulk update — only SET the fields that are non-null.
     */
    @Transactional
    public int dynamicUpdate(Long id, String newName, String newEmail, Boolean newActive) {

        StringBuilder sql = new StringBuilder("UPDATE user_details SET ");
        List<String> setClauses = new ArrayList<>();

        if (newName != null) {
            setClauses.add("user_name = :name");
        }
        if (newEmail != null) {
            setClauses.add("email = :email");
        }
        if (newActive != null) {
            setClauses.add("active = :active");
        }

        if (setClauses.isEmpty()) {
            return 0;  // nothing to update
        }

        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE id = :id");

        Query query = entityManager.createNativeQuery(sql.toString());

        if (newName != null) {
            query.setParameter("name", newName);
        }
        if (newEmail != null) {
            query.setParameter("email", newEmail);
        }
        if (newActive != null) {
            query.setParameter("active", newActive);
        }
        query.setParameter("id", id);

        return query.executeUpdate();
        // Returns int = number of rows affected (0 or 1)
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dynamic UPDATE — Scenarios:                                                     │
│                                                                                  │
│  Scenario 1: dynamicUpdate(1, "Alice Updated", null, null)                       │
│    SQL: UPDATE user_details SET user_name = :name WHERE id = :id                 │
│    Final: UPDATE user_details SET user_name = 'Alice Updated' WHERE id = 1       │
│    → Only name updated. Email and active unchanged.                              │
│                                                                                  │
│  Scenario 2: dynamicUpdate(2, null, "bob.new@ex.com", false)                     │
│    SQL: UPDATE user_details SET email = :email, active = :active WHERE id = :id  │
│    Final: UPDATE user_details SET email = 'bob.new@ex.com', active = false       │
│           WHERE id = 2                                                           │
│    → Email and active updated. Name unchanged.                                   │
│                                                                                  │
│  Scenario 3: dynamicUpdate(3, "Chuck", "chuck@ex.com", true)                     │
│    SQL: UPDATE user_details SET user_name = :name, email = :email,               │
│         active = :active WHERE id = :id                                          │
│    Final: UPDATE user_details SET user_name = 'Chuck',                           │
│           email = 'chuck@ex.com', active = true WHERE id = 3                     │
│    → All three fields updated.                                                   │
│                                                                                  │
│  NOTE: executeUpdate() returns int, not entities.                                │
│  The Persistence Context is NOT updated — cached entities are STALE.             │
│  If you need fresh data after update, call entityManager.clear()                 │
│  or entityManager.refresh(entity).                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Summary — Dynamic Native Query Patterns

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dynamic Native Query — Complete API:                                            │
│                                                                                  │
│  @PersistenceContext                                                             │
│  private EntityManager entityManager;                                            │
│                                                                                  │
│  Creating:                                                                       │
│    entityManager.createNativeQuery(sql)                  → List<Object[]>        │
│    entityManager.createNativeQuery(sql, Entity.class)    → List<Entity>          │
│                                                                                  │
│  Binding parameters:                                                             │
│    query.setParameter("name", value)     → named :name                           │
│    query.setParameter(1, value)          → positional ?1                         │
│                                                                                  │
│  Pagination:                                                                     │
│    query.setFirstResult(page * size)     → OFFSET                                │
│    query.setMaxResults(size)             → LIMIT                                 │
│                                                                                  │
│  Executing:                                                                      │
│    query.getResultList()                 → SELECT → List                          │
│    query.getSingleResult()               → SELECT → single Object                │
│    query.executeUpdate()                 → INSERT/UPDATE/DELETE → int             │
│                                                                                  │
│  Building dynamic SQL:                                                           │
│    StringBuilder sql = new StringBuilder("SELECT ... WHERE 1=1");                │
│    if (param != null) sql.append(" AND column = :param");                        │
│    → Only non-null filters are added to the WHERE clause                        │
│    → Always use setParameter() for values (prevents SQL injection)               │
│    → Whitelist validate column names for ORDER BY (can't parameterize)           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
When to use which query approach — Complete Decision Table:

  ┌─────────────────────────────────────┬──────────────────────────────────────────┐
  │ Scenario                            │ Best Approach                            │
  ├─────────────────────────────────────┼──────────────────────────────────────────┤
  │ Simple findBy with 1-2 conditions   │ Derived Query (method name)              │
  │ Fixed complex query on entities     │ JPQL @Query on Repository                │
  │ Fixed complex query on tables       │ Native @Query on Repository              │
  │ DB-specific features (JSONB, etc.)  │ Native @Query on Repository              │
  │ Optional filters (search/filter API)│ Dynamic Query with EntityManager         │
  │ Dynamic ORDER BY from user input    │ Dynamic Query with EntityManager         │
  │ Dynamic JOINs (conditional)         │ Dynamic Query with EntityManager         │
  │ Dynamic GROUP BY / HAVING           │ Dynamic Query with EntityManager         │
  │ Dynamic UPDATE (partial fields)     │ Dynamic Query with EntityManager         │
  │ Report queries with many variations │ Dynamic Query with EntityManager         │
  └─────────────────────────────────────┴──────────────────────────────────────────┘
```

---


### What Is the Criteria API?

The **Criteria API** is a programmatic, type-safe way to build JPA queries using Java objects instead of writing query strings (JPQL or raw SQL). It is part of the **JPA specification** (javax.persistence.criteria / jakarta.persistence.criteria) and is implemented by Hibernate.

Instead of writing `"SELECT u FROM UserDetails u WHERE u.active = true"` as a string, you build the same query using method calls on `CriteriaBuilder`, `CriteriaQuery`, `Root`, `Predicate`, etc. The result is:
- **Type-safe**: compile-time checks catch errors (no typos in field names if using metamodel)
- **Database-independent**: uses JPA abstraction — Hibernate generates the correct SQL dialect
- **Dynamic**: build queries programmatically, adding/removing conditions at runtime

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Query Approaches — Evolution:                                                   │
│                                                                                  │
│  1. Derived Query (method name)                                                  │
│     findByActiveTrue()                                                           │
│     → Simplest. Fixed at compile time. Spring parses the method name.            │
│                                                                                  │
│  2. JPQL (@Query)                                                                │
│     @Query("SELECT u FROM UserDetails u WHERE u.active = :active")               │
│     → String-based. Entity field names. DB-independent. Fixed query.             │
│                                                                                  │
│  3. Native Query (@Query nativeQuery=true)                                       │
│     @Query(value = "SELECT * FROM user_details WHERE active = true",             │
│            nativeQuery = true)                                                   │
│     → String-based. DB column names. DB-dependent. Fixed query.                  │
│                                                                                  │
│  4. Dynamic Native Query (EntityManager + createNativeQuery)                     │
│     StringBuilder sql = "SELECT * FROM user_details WHERE 1=1";                  │
│     if (name != null) sql.append(" AND user_name = :name");                      │
│     → String-based. DB column names. DB-dependent. DYNAMIC query.               │
│                                                                                  │
│  5. Criteria API (CriteriaBuilder + CriteriaQuery)          ← THIS SECTION      │
│     CriteriaBuilder cb = entityManager.getCriteriaBuilder();                     │
│     CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);           │
│     Root<UserDetails> root = cq.from(UserDetails.class);                         │
│     cq.select(root).where(cb.equal(root.get("active"), true));                   │
│     → Object-based. Java field names. DB-independent. DYNAMIC query.            │
│     → Type-safe (especially with Metamodel). No raw SQL strings at all.          │
│                                                                                  │
│  KEY INSIGHT:                                                                    │
│  Criteria API = Dynamic queries (like EntityManager native) + DB independence    │
│                 (like JPQL) + Type safety (no strings at all with Metamodel)      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Database Tables (used in all examples below):

  user_details                                          user_addresses
  ┌────┬───────────┬──────────────────┬────────┐        ┌────┬─────────┬──────────┬─────────┬────────────┐
  │ id │ user_name │ email            │ active │        │ id │ street  │ city     │ country │ user_id(FK)│
  ├────┼───────────┼──────────────────┼────────┤        ├────┼─────────┼──────────┼─────────┼────────────┤
  │  1 │ Alice     │ alice@ex.com     │ true   │        │ 10 │ Main St │ New York │ USA     │     1      │
  │  2 │ Bob       │ bob@ex.com       │ true   │        │ 11 │ Oak Ave │ London   │ UK      │     2      │
  │  3 │ Charlie   │ charlie@ex.com   │ false  │        │ 12 │ Pine Rd │ Mumbai   │ India   │     1      │
  │  4 │ Diana     │ diana@ex.com     │ true   │        │ 13 │ Elm St  │ Paris    │ France  │     3      │
  │  5 │ Eve       │ eve@ex.com       │ false  │        │ 14 │ Bay Dr  │ New York │ USA     │     4      │
  │  6 │ Frank     │ frank@ex.com     │ true   │        └────┴─────────┴──────────┴─────────┴────────────┘
  │  7 │ Grace     │ grace@ex.com     │ true   │
  │  8 │ Hank      │ hank@ex.com      │ false  │
  └────┴───────────┴──────────────────┴────────┘

  Entity classes:

  @Entity
  @Table(name = "user_details")
  public class UserDetails {
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(name = "user_name")
      private String name;

      @Column(name = "email")
      private String email;

      @Column(name = "active")
      private Boolean active;

      @OneToMany(mappedBy = "userDetails", fetch = FetchType.LAZY)
      private List<UserAddress> addresses;
      // getters, setters
  }

  @Entity
  @Table(name = "user_addresses")
  public class UserAddress {
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(name = "street")
      private String street;

      @Column(name = "city")
      private String city;

      @Column(name = "country")
      private String country;

      @ManyToOne(fetch = FetchType.LAZY)
      @JoinColumn(name = "user_id")
      private UserDetails userDetails;
      // getters, setters
  }
```

---

### Native Query vs Criteria API — DB Dependency vs JPA Abstraction

Dynamic Native Queries (using `EntityManager.createNativeQuery()`) let you build queries at runtime, but they are **database-dependent** — you write raw SQL with DB-specific column names, functions, and syntax. The Criteria API achieves the same dynamic query building but through **JPA abstraction** — you use Java field names, and Hibernate translates to the correct SQL dialect for your database.

```java
// APPROACH 1: Dynamic Native Query — database dependent
@Service
public class UserServiceNative {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<UserDetails> searchUsers(String name, Boolean active) {

        // Raw SQL — uses DB column names (user_name, not "name")
        StringBuilder sql = new StringBuilder("SELECT * FROM user_details WHERE 1=1");

        if (name != null) {
            sql.append(" AND user_name LIKE :name");   // ← DB column name "user_name"
        }
        if (active != null) {
            sql.append(" AND active = :active");       // ← DB column name "active"
        }

        Query query = entityManager.createNativeQuery(sql.toString(), UserDetails.class);

        if (name != null) {
            query.setParameter("name", "%" + name + "%");
        }
        if (active != null) {
            query.setParameter("active", active);
        }

        return query.getResultList();
    }
    // Generated SQL (MySQL):
    //   SELECT * FROM user_details WHERE 1=1 AND user_name LIKE '%Alice%' AND active = true
    //
    // If you switch to Oracle → still works (basic SQL is universal)
    // But if you used MySQL-specific functions (LIMIT, IFNULL, etc.) → BREAKS on Oracle
}
```

```java
// APPROACH 2: Criteria API — database independent
@Service
public class UserServiceCriteria {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<UserDetails> searchUsers(String name, Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        // Build predicates dynamically — uses JAVA FIELD names
        List<Predicate> predicates = new ArrayList<>();

        if (name != null) {
            predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            //                          ↑ Java field name "name", NOT DB column "user_name"
        }
        if (active != null) {
            predicates.add(cb.equal(root.get("active"), active));
            //                           ↑ Java field name "active"
        }

        cq.select(root).where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getResultList();
    }
    // Generated SQL (MySQL dialect):
    //   SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active
    //   FROM user_details u1_0
    //   WHERE u1_0.user_name LIKE '%Alice%' AND u1_0.active = true
    //
    // Generated SQL (Oracle dialect):
    //   SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active
    //   FROM user_details u1_0
    //   WHERE u1_0.user_name LIKE '%Alice%' AND u1_0.active = 1
    //                                                        ↑ Oracle uses 1/0 for boolean
    //
    // Hibernate handles the dialect difference automatically!
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Native Query vs Criteria API — Side by Side:                                    │
│                                                                                  │
│  ┌───────────────────────────────┬──────────────────────┬────────────────────────┐│
│  │ Aspect                        │ Dynamic Native Query │ Criteria API           ││
│  ├───────────────────────────────┼──────────────────────┼────────────────────────┤│
│  │ Query language                │ Raw SQL strings      │ Java method calls      ││
│  │ Column/field references       │ DB column names      │ Java field names       ││
│  │                               │ ("user_name")        │ ("name")               ││
│  │ DB independence               │ ✗ DB-specific SQL    │ ✓ Hibernate generates  ││
│  │                               │                      │   dialect-specific SQL ││
│  │ Type safety                   │ ✗ String-based       │ ✓ Compile-time checks  ││
│  │                               │   (runtime errors)   │   (with Metamodel)     ││
│  │ Dynamic query building        │ ✓ StringBuilder      │ ✓ Predicate list       ││
│  │ SQL injection risk            │ ✗ Must use           │ ✓ Parameterized by     ││
│  │                               │   setParameter()     │   design (no raw SQL)  ││
│  │ DB-specific features          │ ✓ Full access        │ ✗ Limited to JPA spec  ││
│  │ (JSONB, window functions)     │                      │                        ││
│  │ Performance                   │ Slightly faster      │ Slightly slower        ││
│  │                               │ (no JPQL parsing)    │ (builds AST → SQL)     ││
│  │ Readability                   │ ✓ Familiar SQL       │ ✗ Verbose Java code    ││
│  │ Maintainability               │ ✗ String fragile     │ ✓ Refactor-safe        ││
│  └───────────────────────────────┴──────────────────────┴────────────────────────┘│
│                                                                                  │
│  WHEN TO USE CRITERIA API over Dynamic Native Query:                             │
│    - Application must support multiple databases (MySQL + Oracle + PostgreSQL)   │
│    - You want compile-time safety (catch typos in field names early)             │
│    - You want to avoid SQL injection risk entirely (no string concatenation)     │
│    - You are building complex dynamic queries with many optional conditions      │
│                                                                                  │
│  WHEN TO USE DYNAMIC NATIVE QUERY over Criteria API:                             │
│    - You need DB-specific features (JSONB, full-text search, window functions)   │
│    - You need maximum query performance (skip Criteria → JPQL → SQL translation)│
│    - Your team prefers reading SQL over verbose Criteria API code                │
│    - Application is locked to a single database                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  DB Portability — Criteria API vs Native Query:                                  │
│                                                                                  │
│  Scenario: Boolean column "active" — switching from MySQL to Oracle              │
│                                                                                  │
│  Native Query:                                                                   │
│    MySQL:  "SELECT * FROM user_details WHERE active = true"   ← works            │
│    Oracle: "SELECT * FROM user_details WHERE active = true"   ← ERROR!           │
│            Oracle has no BOOLEAN type. Uses NUMBER(1): 0 or 1.                   │
│    Fix:    "SELECT * FROM user_details WHERE active = 1"      ← Oracle-specific  │
│    → You must maintain different SQL strings per database!                        │
│                                                                                  │
│  Criteria API:                                                                   │
│    cb.equal(root.get("active"), true)                                            │
│    MySQL:  → WHERE active = true      (Hibernate MySQL dialect)                  │
│    Oracle: → WHERE active = 1         (Hibernate Oracle dialect)                 │
│    → Same Java code. Hibernate handles the translation automatically.            │
│                                                                                  │
│  Scenario: Pagination — switching from MySQL to Oracle                           │
│                                                                                  │
│  Native Query:                                                                   │
│    MySQL:  "SELECT * FROM user_details LIMIT 10 OFFSET 20"                       │
│    Oracle: "SELECT * FROM user_details OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY"  │
│    → Different syntax! Must rewrite.                                             │
│                                                                                  │
│  Criteria API:                                                                   │
│    query.setFirstResult(20);                                                     │
│    query.setMaxResults(10);                                                      │
│    MySQL:  → LIMIT 10 OFFSET 20                                                  │
│    Oracle: → OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY                             │
│    → Same Java code. Hibernate generates dialect-specific pagination.            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### How Criteria API Works — JPA Abstraction and Type Safety

The Criteria API operates entirely at the **JPA entity level**, not the database level. You reference Java field names, Java types, and entity relationships. Hibernate's Criteria engine builds an internal AST (Abstract Syntax Tree), then translates it to SQL using the configured dialect.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API — How it generates SQL (internal flow):                            │
│                                                                                  │
│  Your Java code:                                                                 │
│    CriteriaBuilder cb = entityManager.getCriteriaBuilder();                      │
│    CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);            │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    cq.select(root).where(cb.equal(root.get("name"), "Alice"));                   │
│    TypedQuery<UserDetails> query = entityManager.createQuery(cq);                │
│    List<UserDetails> result = query.getResultList();                              │
│       │                                                                          │
│       v                                                                          │
│  Step 1: CriteriaBuilder builds an AST (Abstract Syntax Tree)                   │
│    SelectStatement                                                               │
│      ├─ FROM: UserDetails (entity) → table "user_details"                        │
│      ├─ SELECT: root (all fields)                                                │
│      └─ WHERE: EqualPredicate                                                    │
│                  ├─ left: root.get("name") → @Column(name="user_name")           │
│                  └─ right: "Alice" (literal)                                     │
│       │                                                                          │
│       v                                                                          │
│  Step 2: Hibernate reads entity metadata                                         │
│    UserDetails.class:                                                            │
│      @Table(name = "user_details")                                               │
│      field "name" → @Column(name = "user_name")                                  │
│      field "email" → @Column(name = "email")                                     │
│      field "active" → @Column(name = "active")                                   │
│       │                                                                          │
│       v                                                                          │
│  Step 3: SQL Generator (uses configured Dialect)                                 │
│    Dialect = MySQL8Dialect                                                        │
│    → SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name = 'Alice'                                              │
│       │                                                                          │
│       v                                                                          │
│  Step 4: JDBC PreparedStatement → executed on DB → ResultSet                     │
│       │                                                                          │
│       v                                                                          │
│  Step 5: ResultSet → mapped to UserDetails entities (managed in L1 cache)        │
│                                                                                  │
│  KEY: You wrote "name" (Java field). Hibernate resolved it to "user_name" (DB).  │
│  KEY: If you rename the DB column, only @Column annotation changes.              │
│       Your Criteria code stays the same.                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Type Safety — String-based vs Metamodel:**

The Criteria API supports two ways to reference entity fields:

```java
// Option 1: String-based field reference (not fully type-safe)
root.get("name")       // field name as String — typo causes runtime error
root.get("naem")       // ← typo! Compiles fine. Fails at RUNTIME.

// Option 2: JPA Metamodel (fully type-safe)
root.get(UserDetails_.name)     // field reference as generated class constant
root.get(UserDetails_.naem)     // ← typo! COMPILE ERROR. Caught immediately.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JPA Metamodel — Static type safety:                                             │
│                                                                                  │
│  Hibernate can generate a "metamodel" class for each entity at compile time.     │
│  This class has static fields for every entity attribute.                        │
│                                                                                  │
│  Entity:                                          Generated Metamodel:           │
│  @Entity                                          @StaticMetamodel(UserDetails)  │
│  public class UserDetails {                       public class UserDetails_ {    │
│      @Id                                              public static volatile     │
│      private Long id;                                   SingularAttribute<       │
│      @Column(name="user_name")                            UserDetails, Long> id; │
│      private String name;                             public static volatile     │
│      @Column(name="email")                              SingularAttribute<       │
│      private String email;                                UserDetails,String>name│
│      @Column(name="active")                           public static volatile     │
│      private Boolean active;                            SingularAttribute<       │
│  }                                                        UserDetails,String>    │
│                                                           email;                 │
│                                                       public static volatile     │
│                                                         SingularAttribute<       │
│                                                           UserDetails,Boolean>   │
│                                                           active;                │
│                                                   }                              │
│                                                                                  │
│  Usage with Metamodel:                                                           │
│    cb.equal(root.get(UserDetails_.name), "Alice")                                │
│    cb.greaterThan(root.get(UserDetails_.id), 5L)                                 │
│                                                                                  │
│  Benefits:                                                                       │
│    - Typo in field name → COMPILE ERROR (not runtime)                            │
│    - Type mismatch → COMPILE ERROR                                               │
│      e.g., cb.equal(root.get(UserDetails_.name), 42) → error (String != int)    │
│    - IDE autocomplete works — root.get(UserDetails_. → shows all fields          │
│    - When entity field is renamed → refactoring updates Metamodel too            │
│                                                                                  │
│  NOTE: In examples below, we use String-based root.get("name") for simplicity.  │
│  In production, prefer the Metamodel approach for type safety.                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Type Safety Comparison:                                                         │
│                                                                                  │
│  ┌─────────────────────────────────┬──────────────────┬──────────────────────────┐│
│  │ Approach                        │ Field Reference   │ Typo Detection           ││
│  ├─────────────────────────────────┼──────────────────┼──────────────────────────┤│
│  │ Native Query (raw SQL)          │ "user_name"      │ Runtime only             ││
│  │ JPQL (@Query string)            │ "u.name"         │ Startup (Spring parses)  ││
│  │ Criteria API (String-based)     │ root.get("name") │ Runtime (query exec)     ││
│  │ Criteria API (Metamodel)        │ root.get(U_.name)│ COMPILE TIME             ││
│  └─────────────────────────────────┴──────────────────┴──────────────────────────┘│
│                                                                                  │
│  Criteria API with Metamodel = SAFEST approach for dynamic queries               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### CriteriaBuilder → CriteriaQuery → TypedQuery — The Three Steps

Every Criteria API query follows this pattern:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API — Three Step Pattern:                                              │
│                                                                                  │
│  Step 1: Get CriteriaBuilder from EntityManager                                  │
│    CriteriaBuilder cb = entityManager.getCriteriaBuilder();                      │
│    → CriteriaBuilder is a FACTORY for creating query components:                 │
│      predicates (WHERE conditions), expressions, orderings, etc.                 │
│                                                                                  │
│  Step 2: Build CriteriaQuery using CriteriaBuilder                               │
│    CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);            │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    cq.select(root).where(cb.equal(root.get("active"), true));                    │
│    → CriteriaQuery defines the STRUCTURE of the query:                           │
│      what to SELECT, FROM which entity, WHERE conditions, ORDER BY, GROUP BY     │
│                                                                                  │
│  Step 3: Create TypedQuery from CriteriaQuery and execute                        │
│    TypedQuery<UserDetails> query = entityManager.createQuery(cq);                │
│    List<UserDetails> results = query.getResultList();                             │
│    → TypedQuery is the EXECUTABLE form — translates to SQL and runs it           │
│                                                                                  │
│  Flow diagram:                                                                   │
│                                                                                  │
│  EntityManager                                                                   │
│       │                                                                          │
│       ├─ .getCriteriaBuilder() ──→ CriteriaBuilder (factory)                     │
│       │                                │                                         │
│       │                                ├─ .createQuery(Class) → CriteriaQuery    │
│       │                                ├─ .equal()    → Predicate                │
│       │                                ├─ .like()     → Predicate                │
│       │                                ├─ .and()      → Predicate                │
│       │                                ├─ .or()       → Predicate                │
│       │                                ├─ .asc()      → Order                    │
│       │                                ├─ .desc()     → Order                    │
│       │                                ├─ .count()    → Expression               │
│       │                                └─ .sum()      → Expression               │
│       │                                                                          │
│       │   CriteriaQuery                                                          │
│       │       │                                                                  │
│       │       ├─ .from(Entity.class) → Root<Entity>                              │
│       │       ├─ .select(root)                                                   │
│       │       ├─ .where(predicates)                                              │
│       │       ├─ .orderBy(orders)                                                │
│       │       ├─ .groupBy(expressions)                                           │
│       │       └─ .having(predicate)                                              │
│       │                                                                          │
│       └─ .createQuery(criteriaQuery) ──→ TypedQuery<T>                           │
│                                              │                                   │
│                                              ├─ .getResultList() → List<T>       │
│                                              ├─ .getSingleResult() → T           │
│                                              ├─ .setFirstResult(offset)          │
│                                              ├─ .setMaxResults(limit)            │
│                                              └─ .setParameter(name, value)       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### CriteriaBuilder — All Comparison and Logical Operators

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  CriteriaBuilder — Comparison Operators:                                         │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ CriteriaBuilder Method                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ cb.equal(expr, value)                    │ column = value                    ││
│  │ cb.notEqual(expr, value)                 │ column != value (column <> value) ││
│  │ cb.greaterThan(expr, value)              │ column > value                    ││
│  │ cb.greaterThanOrEqualTo(expr, value)     │ column >= value                   ││
│  │ cb.lessThan(expr, value)                 │ column < value                    ││
│  │ cb.lessThanOrEqualTo(expr, value)        │ column <= value                   ││
│  │ cb.between(expr, low, high)              │ column BETWEEN low AND high       ││
│  │ cb.like(expr, pattern)                   │ column LIKE pattern               ││
│  │ cb.notLike(expr, pattern)                │ column NOT LIKE pattern           ││
│  │ cb.isNull(expr)                          │ column IS NULL                    ││
│  │ cb.isNotNull(expr)                       │ column IS NOT NULL                ││
│  │ cb.in(expr)  or  expr.in(collection)     │ column IN (val1, val2, ...)       ││
│  │ cb.isTrue(expr)                          │ column = true (or = 1)            ││
│  │ cb.isFalse(expr)                         │ column = false (or = 0)           ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  CriteriaBuilder — Logical Operators:                                            │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ CriteriaBuilder Method                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ cb.and(predicate1, predicate2)           │ cond1 AND cond2                   ││
│  │ cb.or(predicate1, predicate2)            │ cond1 OR cond2                    ││
│  │ cb.not(predicate)                        │ NOT cond                          ││
│  │ cb.conjunction()                         │ 1=1 (always true — AND identity)  ││
│  │ cb.disjunction()                         │ 1=0 (always false — OR identity)  ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  CriteriaBuilder — Aggregate Functions:                                          │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ CriteriaBuilder Method                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ cb.count(expr)                           │ COUNT(column)                     ││
│  │ cb.countDistinct(expr)                   │ COUNT(DISTINCT column)            ││
│  │ cb.sum(expr)                             │ SUM(column)                       ││
│  │ cb.avg(expr)                             │ AVG(column)                       ││
│  │ cb.max(expr)                             │ MAX(column)                       ││
│  │ cb.min(expr)                             │ MIN(column)                       ││
│  │ cb.greatest(expr)                        │ GREATEST(column) (like max)       ││
│  │ cb.least(expr)                           │ LEAST(column) (like min)          ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  CriteriaBuilder — String Functions:                                             │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ CriteriaBuilder Method                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ cb.upper(expr)                           │ UPPER(column)                     ││
│  │ cb.lower(expr)                           │ LOWER(column)                     ││
│  │ cb.concat(expr1, expr2)                  │ CONCAT(col1, col2)               ││
│  │ cb.substring(expr, start, len)           │ SUBSTRING(column, start, len)    ││
│  │ cb.trim(expr)                            │ TRIM(column)                     ││
│  │ cb.length(expr)                          │ LENGTH(column)                   ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  TypedQuery — Execution Methods:                                                 │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ TypedQuery Method                        │ Meaning                           ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ query.getResultList()                    │ Execute SELECT → List<T>          ││
│  │ query.getSingleResult()                  │ Execute SELECT → single T         ││
│  │                                          │ (throws if 0 or 2+ results)      ││
│  │ query.setFirstResult(offset)             │ OFFSET (for pagination)           ││
│  │ query.setMaxResults(limit)               │ LIMIT (for pagination)            ││
│  │ query.setParameter(name, value)          │ Bind named parameter              ││
│  │ query.setParameter(position, value)      │ Bind positional parameter         ││
│  │ query.setHint(name, value)               │ Set query hint (cache, timeout)   ││
│  │ query.setLockMode(LockModeType)          │ Set lock mode (PESSIMISTIC, etc.) ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Complete Service + Controller Example — Dynamic Search with Criteria API

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Dynamic search using Criteria API.
     * Filters are optional — only non-null parameters become WHERE conditions.
     */
    @Transactional(readOnly = true)
    public List<UserDetails> searchUsers(String name, String email, Boolean active) {

        // Step 1: Get CriteriaBuilder
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Step 2: Create CriteriaQuery for UserDetails entity
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        //                                 ↑ result type = UserDetails

        // Step 3: Define the FROM clause — the root entity
        Root<UserDetails> root = cq.from(UserDetails.class);
        //  root represents the "user_details" table
        //  root.get("name") = reference to "user_name" column
        //  root.get("email") = reference to "email" column

        // Step 4: Build predicates (WHERE conditions) dynamically
        List<Predicate> predicates = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            // SQL: user_name LIKE '%Alice%'
        }

        if (email != null && !email.isEmpty()) {
            predicates.add(cb.equal(root.get("email"), email));
            // SQL: email = 'alice@ex.com'
        }

        if (active != null) {
            predicates.add(cb.equal(root.get("active"), active));
            // SQL: active = true
        }

        // Step 5: Apply SELECT and WHERE
        cq.select(root).where(predicates.toArray(new Predicate[0]));
        //  .where() accepts Predicate[] (varargs)
        //  Multiple predicates are combined with AND by default.

        // Step 6: Create TypedQuery and execute
        TypedQuery<UserDetails> query = entityManager.createQuery(cq);
        return query.getResultList();
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users/search?name=Alice
    // GET /api/users/search?active=true
    // GET /api/users/search?name=Alice&email=alice@ex.com&active=true
    @GetMapping("/search")
    public List<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Boolean active
    ) {
        return userService.searchUsers(name, email, active);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API — Generated SQL for each scenario:                                 │
│                                                                                  │
│  Scenario 1: searchUsers("Alice", null, null)                                    │
│    Predicates: [cb.like(root.get("name"), "%Alice%")]                            │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Alice%'                                         │
│    Result: [UserDetails(id=1, name="Alice", ...)]                                │
│                                                                                  │
│  Scenario 2: searchUsers(null, null, true)                                       │
│    Predicates: [cb.equal(root.get("active"), true)]                              │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.active = true                                                    │
│    Result: [Alice, Bob, Diana, Frank, Grace] — 5 active users                    │
│                                                                                  │
│  Scenario 3: searchUsers("Bob", "bob@ex.com", true)                              │
│    Predicates: [like, equal(email), equal(active)]                               │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Bob%'                                           │
│        AND u1_0.email = 'bob@ex.com'                                             │
│        AND u1_0.active = true                                                    │
│    Result: [UserDetails(id=2, name="Bob", ...)]                                  │
│                                                                                  │
│  Scenario 4: searchUsers(null, null, null)                                       │
│    Predicates: [] (empty)                                                        │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│    Result: All 8 users (no WHERE clause at all)                                  │
│                                                                                  │
│  NOTE: When predicates list is empty, .where() with empty array = no WHERE.      │
│  This is the Criteria API equivalent of the "WHERE 1=1" trick.                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Using OR, NOT, and complex logic:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Complex search with OR and NOT logic:
     * Find users where (name LIKE keyword OR email LIKE keyword) AND active = true
     */
    @Transactional(readOnly = true)
    public List<UserDetails> complexSearch(String keyword, Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        List<Predicate> predicates = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            // OR condition: name LIKE keyword OR email LIKE keyword
            Predicate nameLike = cb.like(root.get("name"), "%" + keyword + "%");
            Predicate emailLike = cb.like(root.get("email"), "%" + keyword + "%");
            predicates.add(cb.or(nameLike, emailLike));
            // SQL: (user_name LIKE '%keyword%' OR email LIKE '%keyword%')
        }

        if (active != null) {
            predicates.add(cb.equal(root.get("active"), active));
        }

        // All predicates combined with AND
        cq.select(root).where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getResultList();
    }

    /**
     * Using IN, BETWEEN, IS NOT NULL:
     */
    @Transactional(readOnly = true)
    public List<UserDetails> advancedSearch(List<Long> ids, Long minId, Long maxId) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        List<Predicate> predicates = new ArrayList<>();

        if (ids != null && !ids.isEmpty()) {
            predicates.add(root.get("id").in(ids));
            // SQL: id IN (1, 2, 5)
        }

        if (minId != null && maxId != null) {
            predicates.add(cb.between(root.get("id"), minId, maxId));
            // SQL: id BETWEEN 3 AND 7
        }

        predicates.add(cb.isNotNull(root.get("email")));
        // SQL: email IS NOT NULL

        cq.select(root).where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getResultList();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Complex Logic — Generated SQL:                                                  │
│                                                                                  │
│  complexSearch("ali", true):                                                     │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE (u1_0.user_name LIKE '%ali%' OR u1_0.email LIKE '%ali%')                │
│      AND u1_0.active = true                                                      │
│    Result: [Alice] — name matches "ali", active = true                           │
│                                                                                  │
│  advancedSearch([1, 2, 5], null, null):                                          │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.id IN (1, 2, 5)                                                    │
│      AND u1_0.email IS NOT NULL                                                  │
│    Result: [Alice, Bob, Eve]                                                     │
│                                                                                  │
│  advancedSearch(null, 3L, 7L):                                                   │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.id BETWEEN 3 AND 7                                                 │
│      AND u1_0.email IS NOT NULL                                                  │
│    Result: [Charlie, Diana, Eve, Frank, Grace]                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Multiselect — Selecting Multiple Fields as List<Object[]>

When you don't need the full entity but only specific fields, use `cq.multiselect()` to select individual columns. The result type is `Object[]` — each array element corresponds to one selected field.

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Select only name and email — returns List<Object[]>
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUserNamesAndEmails(Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Result type = Object[] (not entity — because we select partial fields)
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<UserDetails> root = cq.from(UserDetails.class);

        // multiselect — select specific fields
        cq.multiselect(
            root.get("name"),     // index 0 → user_name column
            root.get("email")     // index 1 → email column
        );

        if (active != null) {
            cq.where(cb.equal(root.get("active"), active));
        }

        TypedQuery<Object[]> query = entityManager.createQuery(cq);
        return query.getResultList();
    }
}
```

```java
// Service — converting Object[] to DTO
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<UserEmailDTO> getUserEmails(Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        cq.multiselect(
            root.get("name"),
            root.get("email")
        );

        if (active != null) {
            cq.where(cb.equal(root.get("active"), active));
        }

        List<Object[]> results = entityManager.createQuery(cq).getResultList();

        // Convert Object[] to DTO
        return results.stream()
            .map(row -> new UserEmailDTO((String) row[0], (String) row[1]))
            .collect(Collectors.toList());
    }
}

// DTO
public class UserEmailDTO {
    private String name;
    private String email;

    public UserEmailDTO(String name, String email) {
        this.name = name;
        this.email = email;
    }
    // getters
    public String getName() { return name; }
    public String getEmail() { return email; }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/emails")
    public List<UserEmailDTO> getEmails(@RequestParam(required = false) Boolean active) {
        return userService.getUserEmails(active);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  multiselect — How it works:                                                     │
│                                                                                  │
│  Java code:                                                                      │
│    cq.multiselect(root.get("name"), root.get("email"));                          │
│                                                                                  │
│  Generated SQL:                                                                  │
│    SELECT u1_0.user_name, u1_0.email                                             │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.active = true                                                      │
│                                                                                  │
│  ResultSet:                                                                      │
│    ┌───────────┬──────────────────┐                                              │
│    │ user_name │ email            │                                              │
│    ├───────────┼──────────────────┤                                              │
│    │ Alice     │ alice@ex.com     │   → Object[] { "Alice", "alice@ex.com" }     │
│    │ Bob       │ bob@ex.com       │   → Object[] { "Bob",   "bob@ex.com" }       │
│    │ Diana     │ diana@ex.com     │   → Object[] { "Diana", "diana@ex.com" }     │
│    │ Frank     │ frank@ex.com     │   → Object[] { "Frank", "frank@ex.com" }     │
│    │ Grace     │ grace@ex.com     │   → Object[] { "Grace", "grace@ex.com" }     │
│    └───────────┴──────────────────┘                                              │
│                                                                                  │
│  Object[] index mapping:                                                         │
│    row[0] → "name"  (first in multiselect)  → user_name column                  │
│    row[1] → "email" (second in multiselect) → email column                       │
│                                                                                  │
│  Alternative — Direct DTO construction (no Object[] step):                       │
│    CriteriaQuery<UserEmailDTO> cq = cb.createQuery(UserEmailDTO.class);          │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    cq.select(cb.construct(UserEmailDTO.class,                                    │
│        root.get("name"),                                                         │
│        root.get("email")                                                         │
│    ));                                                                            │
│    → Hibernate calls new UserEmailDTO(name, email) directly!                     │
│    → No manual Object[] conversion needed.                                       │
│    → DTO constructor parameter order must match select order.                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Using cb.construct() for direct DTO mapping (cleaner approach):**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<UserEmailDTO> getUserEmailsDirect(Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Result type = DTO class directly
        CriteriaQuery<UserEmailDTO> cq = cb.createQuery(UserEmailDTO.class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        // cb.construct() — calls the DTO constructor directly
        cq.select(cb.construct(UserEmailDTO.class,
            root.get("name"),      // → 1st constructor parameter (String name)
            root.get("email")      // → 2nd constructor parameter (String email)
        ));

        if (active != null) {
            cq.where(cb.equal(root.get("active"), active));
        }

        return entityManager.createQuery(cq).getResultList();
        // Returns List<UserEmailDTO> directly — no Object[] conversion!
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  multiselect vs cb.construct — Comparison:                                       │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ cq.multiselect() → List<Object[]>        │ cb.construct() → List<DTO>        ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ Returns raw Object[]                     │ Returns DTO instances directly    ││
│  │ Manual casting: (String) row[0]          │ No casting needed                 ││
│  │ Index-based (fragile)                    │ Constructor-based (robust)        ││
│  │ Flexible — any column combination        │ Needs matching DTO constructor    ││
│  │ Good for prototyping/ad-hoc queries      │ Good for production code          ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  Both generate the same SQL:                                                     │
│    SELECT u1_0.user_name, u1_0.email FROM user_details u1_0 WHERE ...            │
│                                                                                  │
│  The difference is only in how the ResultSet is mapped to Java objects.           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Multiselect with aggregation:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Count users by active status — GROUP BY + aggregate
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUserCountByStatus() {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        cq.multiselect(
            root.get("active"),              // GROUP BY column
            cb.count(root.get("id"))         // COUNT(id)
        );

        cq.groupBy(root.get("active"));

        return entityManager.createQuery(cq).getResultList();
    }
    // Generated SQL:
    //   SELECT u1_0.active, COUNT(u1_0.id)
    //   FROM user_details u1_0
    //   GROUP BY u1_0.active
    //
    // Result:
    //   Object[] { true,  5 }   — 5 active users
    //   Object[] { false, 3 }   — 3 inactive users
}
```

---

### JOIN Two Entities Using Criteria API

The Criteria API supports JOINs through `Root.join()`, which follows JPA entity relationships (`@OneToMany`, `@ManyToOne`, etc.). Unlike native queries, you don't write `JOIN ... ON ...` — you reference the Java field that holds the relationship.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API JOIN — How it works:                                               │
│                                                                                  │
│  Entity relationship:                                                            │
│    UserDetails ───@OneToMany(mappedBy="userDetails")──→ List<UserAddress>        │
│    UserAddress ───@ManyToOne──→ UserDetails                                      │
│                                                                                  │
│  Criteria API:                                                                   │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    Join<UserDetails, UserAddress> addressJoin = root.join("addresses");           │
│    //          ↑ parent entity    ↑ child entity       ↑ Java field name         │
│    //  "addresses" is the field on UserDetails that holds List<UserAddress>       │
│                                                                                  │
│  Hibernate reads the relationship:                                               │
│    @OneToMany(mappedBy = "userDetails")                                          │
│    private List<UserAddress> addresses;                                           │
│    → Knows that user_addresses.user_id = user_details.id                         │
│                                                                                  │
│  Generated SQL:                                                                  │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                            │
│    //                          ↑ Hibernate generates the ON clause automatically │
│                                                                                  │
│  JOIN types:                                                                     │
│    root.join("addresses")                         → INNER JOIN (default)         │
│    root.join("addresses", JoinType.INNER)         → INNER JOIN                   │
│    root.join("addresses", JoinType.LEFT)          → LEFT OUTER JOIN              │
│    root.join("addresses", JoinType.RIGHT)         → RIGHT OUTER JOIN             │
│                                                                                  │
│  IMPORTANT: Unlike native query, you CANNOT join unrelated tables.               │
│  The entities MUST have a mapped JPA relationship (@OneToMany, @ManyToOne, etc.) │
│  For unrelated tables, use native query.                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 1: JOIN + multiselect — user name with city:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Search users by city — JOIN UserDetails with UserAddress.
     * Returns user name + city as Object[].
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUsersWithCity(String city) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        // FROM user_details
        Root<UserDetails> userRoot = cq.from(UserDetails.class);

        // JOIN user_addresses ON user_details.id = user_addresses.user_id
        Join<UserDetails, UserAddress> addressJoin = userRoot.join("addresses");
        //                                                    ↑ field name on UserDetails entity

        // SELECT user_name, city
        cq.multiselect(
            userRoot.get("name"),         // user_details.user_name
            addressJoin.get("city")       // user_addresses.city
        );

        // WHERE city = :city (optional)
        if (city != null && !city.isEmpty()) {
            cq.where(cb.equal(addressJoin.get("city"), city));
        }

        return entityManager.createQuery(cq).getResultList();
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users/by-city?city=New York
    @GetMapping("/by-city")
    public List<Object[]> getUsersByCity(@RequestParam(required = false) String city) {
        return userService.getUsersWithCity(city);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JOIN + multiselect — Scenarios:                                                 │
│                                                                                  │
│  Scenario 1: getUsersWithCity("New York")                                        │
│    Generated SQL:                                                                │
│      SELECT u1_0.user_name, a1_0.city                                            │
│      FROM user_details u1_0                                                      │
│      JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                          │
│      WHERE a1_0.city = 'New York'                                                │
│                                                                                  │
│    Result:                                                                       │
│      ┌───────────┬──────────┐                                                    │
│      │ user_name │ city     │                                                    │
│      ├───────────┼──────────┤                                                    │
│      │ Alice     │ New York │  → Object[] { "Alice", "New York" }                │
│      │ Diana     │ New York │  → Object[] { "Diana", "New York" }                │
│      └───────────┴──────────┘                                                    │
│    (Alice has address id=10 in New York, Diana has address id=14 in New York)    │
│                                                                                  │
│  Scenario 2: getUsersWithCity(null) — no filter                                  │
│    Generated SQL:                                                                │
│      SELECT u1_0.user_name, a1_0.city                                            │
│      FROM user_details u1_0                                                      │
│      JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                          │
│                                                                                  │
│    Result:                                                                       │
│      ┌───────────┬──────────┐                                                    │
│      │ user_name │ city     │                                                    │
│      ├───────────┼──────────┤                                                    │
│      │ Alice     │ New York │  → from address id=10                              │
│      │ Alice     │ Mumbai   │  → from address id=12 (Alice has 2 addresses)      │
│      │ Bob       │ London   │  → from address id=11                              │
│      │ Charlie   │ Paris    │  → from address id=13                              │
│      │ Diana     │ New York │  → from address id=14                              │
│      └───────────┴──────────┘                                                    │
│    NOTE: Alice appears TWICE because she has 2 addresses (INNER JOIN behavior)   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 2: JOIN + multiselect + dynamic conditions on both entities:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Advanced search across user_details and user_addresses.
     * Dynamic conditions on BOTH entities.
     */
    @Transactional(readOnly = true)
    public List<Object[]> advancedJoinSearch(String name, Boolean active,
                                              String city, String country) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<UserDetails> userRoot = cq.from(UserDetails.class);
        Join<UserDetails, UserAddress> addrJoin = userRoot.join("addresses", JoinType.LEFT);
        //                                                                   ↑ LEFT JOIN
        //  LEFT JOIN ensures users without addresses are still returned

        // SELECT user fields + address fields
        cq.multiselect(
            userRoot.get("id"),
            userRoot.get("name"),
            userRoot.get("active"),
            addrJoin.get("city"),
            addrJoin.get("country")
        );

        // Dynamic WHERE conditions
        List<Predicate> predicates = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            predicates.add(cb.like(userRoot.get("name"), "%" + name + "%"));
        }
        if (active != null) {
            predicates.add(cb.equal(userRoot.get("active"), active));
        }
        if (city != null && !city.isEmpty()) {
            predicates.add(cb.equal(addrJoin.get("city"), city));
        }
        if (country != null && !country.isEmpty()) {
            predicates.add(cb.equal(addrJoin.get("country"), country));
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        return entityManager.createQuery(cq).getResultList();
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Advanced JOIN — Generated SQL:                                                  │
│                                                                                  │
│  advancedJoinSearch(null, true, "New York", "USA"):                              │
│    SELECT u1_0.id, u1_0.user_name, u1_0.active, a1_0.city, a1_0.country         │
│    FROM user_details u1_0                                                        │
│    LEFT JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                       │
│    WHERE u1_0.active = true AND a1_0.city = 'New York' AND a1_0.country = 'USA'  │
│                                                                                  │
│    Result:                                                                       │
│      ┌────┬───────────┬────────┬──────────┬─────────┐                            │
│      │ id │ user_name │ active │ city     │ country │                            │
│      ├────┼───────────┼────────┼──────────┼─────────┤                            │
│      │  1 │ Alice     │ true   │ New York │ USA     │                            │
│      │  4 │ Diana     │ true   │ New York │ USA     │                            │
│      └────┴───────────┴────────┴──────────┴─────────┘                            │
│                                                                                  │
│  advancedJoinSearch("Alice", null, null, null):                                  │
│    SELECT u1_0.id, u1_0.user_name, u1_0.active, a1_0.city, a1_0.country         │
│    FROM user_details u1_0                                                        │
│    LEFT JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                       │
│    WHERE u1_0.user_name LIKE '%Alice%'                                           │
│                                                                                  │
│    Result:                                                                       │
│      ┌────┬───────────┬────────┬──────────┬─────────┐                            │
│      │ id │ user_name │ active │ city     │ country │                            │
│      ├────┼───────────┼────────┼──────────┼─────────┤                            │
│      │  1 │ Alice     │ true   │ New York │ USA     │ ← address id=10            │
│      │  1 │ Alice     │ true   │ Mumbai   │ India   │ ← address id=12            │
│      └────┴───────────┴────────┴──────────┴─────────┘                            │
│    Alice appears twice (she has 2 addresses)                                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example 3: JOIN with GROUP BY and HAVING — count addresses per user:**

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Count addresses per user, optionally filter by minimum count.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUsersWithAddressCount(Integer minAddresses) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<UserDetails> userRoot = cq.from(UserDetails.class);
        Join<UserDetails, UserAddress> addrJoin = userRoot.join("addresses", JoinType.LEFT);

        cq.multiselect(
            userRoot.get("name"),
            cb.count(addrJoin.get("id"))      // COUNT(user_addresses.id)
        );

        cq.groupBy(userRoot.get("id"), userRoot.get("name"));

        if (minAddresses != null && minAddresses > 0) {
            cq.having(cb.ge(cb.count(addrJoin.get("id")), minAddresses));
            // HAVING COUNT(address.id) >= minAddresses
        }

        cq.orderBy(cb.desc(cb.count(addrJoin.get("id"))));

        return entityManager.createQuery(cq).getResultList();
    }
    // Generated SQL (minAddresses = 2):
    //   SELECT u1_0.user_name, COUNT(a1_0.id)
    //   FROM user_details u1_0
    //   LEFT JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id
    //   GROUP BY u1_0.id, u1_0.user_name
    //   HAVING COUNT(a1_0.id) >= 2
    //   ORDER BY COUNT(a1_0.id) DESC
    //
    // Result:
    //   Object[] { "Alice", 2 }   — Alice has 2 addresses (New York + Mumbai)
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JOIN Variations — Criteria API:                                                 │
│                                                                                  │
│  INNER JOIN (default — only matching rows):                                      │
│    root.join("addresses")                                                        │
│    root.join("addresses", JoinType.INNER)                                        │
│    SQL: ... JOIN user_addresses a ON u.id = a.user_id                            │
│    → Users WITHOUT addresses are EXCLUDED                                        │
│                                                                                  │
│  LEFT JOIN (all parent rows, null for missing child):                            │
│    root.join("addresses", JoinType.LEFT)                                         │
│    SQL: ... LEFT JOIN user_addresses a ON u.id = a.user_id                       │
│    → Users WITHOUT addresses are INCLUDED (address columns = null)               │
│                                                                                  │
│  FETCH JOIN (load relationship eagerly to avoid N+1):                            │
│    root.fetch("addresses")                                                       │
│    root.fetch("addresses", JoinType.LEFT)                                        │
│    SQL: ... LEFT JOIN user_addresses a ON u.id = a.user_id                       │
│    → Same SQL as LEFT JOIN                                                       │
│    → PLUS: loaded addresses are populated into entity's addresses field          │
│    → Used with entity results, NOT with multiselect/Object[]                     │
│                                                                                  │
│  ┌────────────────────────┬──────────────────────────────────────────────────────┐│
│  │ Method                 │ Use when                                             ││
│  ├────────────────────────┼──────────────────────────────────────────────────────┤│
│  │ root.join()            │ Filtering/selecting by child fields + multiselect   ││
│  │ root.fetch()           │ Loading full entities with relationships (avoid N+1)││
│  └────────────────────────┴──────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Pagination and Sorting with Criteria API

Pagination uses `TypedQuery.setFirstResult(offset)` and `TypedQuery.setMaxResults(limit)` — same methods as with native queries. Sorting uses `CriteriaQuery.orderBy()` with `CriteriaBuilder.asc()` or `CriteriaBuilder.desc()`.

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Full-featured dynamic search with Criteria API:
     * - Optional filters
     * - Pagination (page + size)
     * - Dynamic sorting (sortBy field name + direction)
     * Returns Page<UserDetails>
     */
    @Transactional(readOnly = true)
    public Page<UserDetails> searchUsersPaginated(String name, Boolean active,
                                                   int page, int size,
                                                   String sortBy, String sortDir) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // === DATA QUERY ===
        CriteriaQuery<UserDetails> dataCq = cb.createQuery(UserDetails.class);
        Root<UserDetails> dataRoot = dataCq.from(UserDetails.class);

        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        if (name != null && !name.isEmpty()) {
            predicates.add(cb.like(dataRoot.get("name"), "%" + name + "%"));
        }
        if (active != null) {
            predicates.add(cb.equal(dataRoot.get("active"), active));
        }

        dataCq.select(dataRoot);
        if (!predicates.isEmpty()) {
            dataCq.where(predicates.toArray(new Predicate[0]));
        }

        // Sorting — dynamic ORDER BY using Java field names
        List<String> allowedSortFields = List.of("id", "name", "email", "active");
        if (sortBy != null && allowedSortFields.contains(sortBy)) {
            if ("DESC".equalsIgnoreCase(sortDir)) {
                dataCq.orderBy(cb.desc(dataRoot.get(sortBy)));
            } else {
                dataCq.orderBy(cb.asc(dataRoot.get(sortBy)));
            }
        } else {
            dataCq.orderBy(cb.asc(dataRoot.get("id")));  // default sort
        }

        // Apply pagination
        TypedQuery<UserDetails> dataQuery = entityManager.createQuery(dataCq);
        dataQuery.setFirstResult(page * size);   // OFFSET
        dataQuery.setMaxResults(size);            // LIMIT

        List<UserDetails> content = dataQuery.getResultList();

        // === COUNT QUERY ===
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<UserDetails> countRoot = countCq.from(UserDetails.class);

        countCq.select(cb.count(countRoot));

        // Rebuild predicates for count query (same conditions, different Root)
        List<Predicate> countPredicates = new ArrayList<>();
        if (name != null && !name.isEmpty()) {
            countPredicates.add(cb.like(countRoot.get("name"), "%" + name + "%"));
        }
        if (active != null) {
            countPredicates.add(cb.equal(countRoot.get("active"), active));
        }
        if (!countPredicates.isEmpty()) {
            countCq.where(countPredicates.toArray(new Predicate[0]));
        }

        Long totalElements = entityManager.createQuery(countCq).getSingleResult();

        // Build Page object
        Pageable pageable = PageRequest.of(page, size, Sort.by(
            "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy != null && allowedSortFields.contains(sortBy) ? sortBy : "id"
        ));
        return new PageImpl<>(content, pageable, totalElements);
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users/search?active=true&page=0&size=3&sortBy=name&sortDir=DESC
    @GetMapping("/search")
    public Page<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        return userService.searchUsersPaginated(name, active, page, size, sortBy, sortDir);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API Pagination + Sorting — Scenario:                                   │
│                                                                                  │
│  Request: GET /api/users/search?active=true&page=1&size=2&sortBy=name&sortDir=ASC│
│                                                                                  │
│  Data query generated SQL:                                                       │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.active = true                                                      │
│    ORDER BY u1_0.user_name ASC                                                   │
│    LIMIT 2 OFFSET 2                                                              │
│         ↑ size   ↑ page(1) × size(2)                                             │
│                                                                                  │
│  Count query generated SQL:                                                      │
│    SELECT COUNT(u1_0.id)                                                         │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.active = true                                                      │
│    → Returns: 5 (Alice, Bob, Diana, Frank, Grace)                                │
│                                                                                  │
│  Active users sorted by name: Alice(1), Bob(2), Diana(4), Frank(6), Grace(7)    │
│  Page 0 (OFFSET 0, LIMIT 2): [Alice, Bob]                                       │
│  Page 1 (OFFSET 2, LIMIT 2): [Diana, Frank]  ← THIS page                        │
│  Page 2 (OFFSET 4, LIMIT 2): [Grace]                                            │
│                                                                                  │
│  Response:                                                                       │
│    Page<UserDetails> {                                                           │
│      content: [Diana, Frank],                                                    │
│      pageNumber: 1,                                                              │
│      pageSize: 2,                                                                │
│      totalElements: 5,                                                           │
│      totalPages: 3                                                               │
│    }                                                                             │
│                                                                                  │
│  KEY DIFFERENCE from Native Query pagination:                                    │
│  - sortBy = "name" (JAVA FIELD name, not DB column "user_name")                 │
│  - Hibernate translates "name" → "user_name" via @Column annotation              │
│  - No whitelist needed for SQL injection (no string concatenation in SQL)        │
│  - But whitelist is still good practice to reject unexpected field names          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Multi-column sorting:**

```java
// Sort by active DESC, then by name ASC
dataCq.orderBy(
    cb.desc(dataRoot.get("active")),    // active users first
    cb.asc(dataRoot.get("name"))        // then alphabetical by name
);

// Generated SQL:
//   ... ORDER BY u1_0.active DESC, u1_0.user_name ASC
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API Sorting vs Other Approaches:                                       │
│                                                                                  │
│  ┌────────────────────────────────────────┬──────────────────────────────────────┐│
│  │ Approach                               │ Sorting Mechanism                   ││
│  ├────────────────────────────────────────┼──────────────────────────────────────┤│
│  │ Derived Query                          │ findByActiveOrderByNameAsc()        ││
│  │                                        │ → Fixed in method name              ││
│  │ JPQL                                   │ "... ORDER BY u.name ASC"           ││
│  │                                        │ → String (Java field names)         ││
│  │ Native Query                           │ "... ORDER BY user_name ASC"        ││
│  │                                        │ → String (DB column names)          ││
│  │ Dynamic Native (EntityManager)         │ sql.append(" ORDER BY " + col)      ││
│  │                                        │ → String concat (injection risk!)   ││
│  │ Criteria API                           │ cb.asc(root.get("name"))            ││
│  │                                        │ → Method call (type-safe, no SQL)   ││
│  └────────────────────────────────────────┴──────────────────────────────────────┘│
│                                                                                  │
│  Criteria API sorting is the safest:                                             │
│    - No SQL string concatenation → no SQL injection possible                    │
│    - Uses Java field names → Hibernate resolves to DB column names               │
│    - Compile-time safe with Metamodel: cb.asc(root.get(UserDetails_.name))       │
│    - Dynamic: can add/remove orderBy at runtime based on parameters              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Summary — Criteria API

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Criteria API — Complete Pattern:                                                │
│                                                                                  │
│  @PersistenceContext                                                             │
│  private EntityManager entityManager;                                            │
│                                                                                  │
│  // 1. Get builder                                                               │
│  CriteriaBuilder cb = entityManager.getCriteriaBuilder();                        │
│                                                                                  │
│  // 2. Create query                                                              │
│  CriteriaQuery<Entity> cq = cb.createQuery(Entity.class);     // entity result   │
│  CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class); // partial cols    │
│  CriteriaQuery<Long> cq = cb.createQuery(Long.class);         // count           │
│  CriteriaQuery<DTO> cq = cb.createQuery(DTO.class);           // DTO (construct) │
│                                                                                  │
│  // 3. Define FROM                                                               │
│  Root<Entity> root = cq.from(Entity.class);                                      │
│                                                                                  │
│  // 4. Optional JOIN                                                             │
│  Join<Parent, Child> join = root.join("fieldName");                               │
│  Join<Parent, Child> join = root.join("fieldName", JoinType.LEFT);               │
│                                                                                  │
│  // 5. SELECT                                                                    │
│  cq.select(root);                                  // full entity                │
│  cq.multiselect(root.get("a"), root.get("b"));     // partial → Object[]        │
│  cq.select(cb.construct(DTO.class, ...));           // direct DTO                │
│  cq.select(cb.count(root));                         // aggregate                 │
│                                                                                  │
│  // 6. WHERE (dynamic predicates)                                                │
│  List<Predicate> preds = new ArrayList<>();                                      │
│  preds.add(cb.equal(...));  preds.add(cb.like(...));                             │
│  cq.where(preds.toArray(new Predicate[0]));                                      │
│                                                                                  │
│  // 7. ORDER BY                                                                  │
│  cq.orderBy(cb.asc(root.get("field")));                                          │
│                                                                                  │
│  // 8. GROUP BY + HAVING                                                         │
│  cq.groupBy(root.get("field"));                                                  │
│  cq.having(cb.ge(cb.count(root), 5));                                            │
│                                                                                  │
│  // 9. Execute with pagination                                                   │
│  TypedQuery<T> query = entityManager.createQuery(cq);                            │
│  query.setFirstResult(page * size);                                              │
│  query.setMaxResults(size);                                                      │
│  List<T> results = query.getResultList();                                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Complete Decision Table — All Query Approaches:

  ┌─────────────────────────────────────┬──────────────────────────────────────────┐
  │ Scenario                            │ Best Approach                            │
  ├─────────────────────────────────────┼──────────────────────────────────────────┤
  │ Simple findBy with 1-2 conditions   │ Derived Query (method name)              │
  │ Fixed complex query on entities     │ JPQL @Query on Repository                │
  │ Fixed complex query on tables       │ Native @Query on Repository              │
  │ DB-specific features (JSONB, etc.)  │ Native @Query on Repository              │
  │ Dynamic query, DB-locked            │ Dynamic Native Query (EntityManager)     │
  │ Dynamic query, DB-independent       │ Criteria API                             │
  │ Dynamic query, type-safe            │ Criteria API + Metamodel                 │
  │ Dynamic JOINs (related entities)    │ Criteria API                             │
  │ Dynamic JOINs (unrelated tables)    │ Dynamic Native Query (EntityManager)     │
  │ Dynamic GROUP BY / HAVING           │ Criteria API or Dynamic Native Query     │
  │ Dynamic UPDATE (partial fields)     │ Dynamic Native Query (EntityManager)     │
  │ Maximum readability                 │ JPQL or Native Query (SQL is familiar)   │
  │ Maximum type safety                 │ Criteria API + Metamodel                 │
  │ Maximum DB portability              │ Criteria API or JPQL                     │
  └─────────────────────────────────────┴──────────────────────────────────────────┘
```

---

## JPA Specification API

### What Is the Specification API?

The **Specification API** is a Spring Data JPA abstraction built **on top of** the Criteria API. It solves two major problems with raw Criteria API usage:

1. **Code Duplicity** — the same predicate logic (e.g., "filter by active = true") gets copy-pasted across multiple service methods
2. **Boilerplate** — every Criteria query needs the same `CriteriaBuilder` → `CriteriaQuery` → `Root` → `TypedQuery` setup code

The Specification API encapsulates each WHERE condition as a **reusable, composable** `Specification` object. Spring Data's `JpaSpecificationExecutor` then handles all the CriteriaBuilder/CriteriaQuery/TypedQuery boilerplate for you.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Evolution — from Criteria API to Specification API:                             │
│                                                                                  │
│  Criteria API (manual):                                                          │
│    @Service                                                                      │
│    public class UserService {                                                    │
│        @PersistenceContext                                                        │
│        private EntityManager entityManager;                                      │
│                                                                                  │
│        public List<UserDetails> search(String name, Boolean active) {            │
│            CriteriaBuilder cb = entityManager.getCriteriaBuilder();       // ┐   │
│            CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);//│  │
│            Root<UserDetails> root = cq.from(UserDetails.class);           // │   │
│            List<Predicate> preds = new ArrayList<>();                     // │   │
│            if (name != null) preds.add(cb.like(root.get("name"), ...));   // │   │
│            if (active != null) preds.add(cb.equal(root.get("active"),    // ├ B  │
│                                                    active));             // │ O  │
│            cq.select(root).where(preds.toArray(new Predicate[0]));       // │ I  │
│            TypedQuery<UserDetails> query = entityManager.createQuery(cq); // │ L  │
│            return query.getResultList();                                  // │ E  │
│        }                                                                  // │ R  │
│        // EVERY method repeats this boilerplate!                          // ┘ P  │
│    }                                                                             │
│                                                                                  │
│  Specification API (Spring Data):                                                │
│    // Repository — just extend JpaSpecificationExecutor                          │
│    public interface UserDetailsRepository extends JpaRepository<UserDetails, Long>│
│                                                , JpaSpecificationExecutor<        │
│                                                    UserDetails> { }              │
│                                                                                  │
│    // Specification — reusable predicate                                         │
│    public class UserSpecs {                                                      │
│        public static Specification<UserDetails> hasName(String name) {           │
│            return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%"│
│            );                                                                    │
│        }                                                                         │
│        public static Specification<UserDetails> isActive() {                     │
│            return (root, query, cb) -> cb.equal(root.get("active"), true);       │
│        }                                                                         │
│    }                                                                             │
│                                                                                  │
│    // Service — NO boilerplate!                                                  │
│    @Service                                                                      │
│    public class UserService {                                                    │
│        @Autowired                                                                │
│        private UserDetailsRepository repository;                                 │
│                                                                                  │
│        public List<UserDetails> search(String name, Boolean active) {            │
│            Specification<UserDetails> spec = Specification.where(null);           │
│            if (name != null) spec = spec.and(UserSpecs.hasName(name));            │
│            if (active != null) spec = spec.and(UserSpecs.isActive());            │
│            return repository.findAll(spec);                                      │
│        }                                                                         │
│    }                                                                             │
│                                                                                  │
│  RESULT:                                                                         │
│    - No EntityManager, CriteriaBuilder, CriteriaQuery, Root, TypedQuery          │
│    - Each predicate defined ONCE, reused everywhere                              │
│    - Predicates composed with .and(), .or(), .not()                              │
│    - Spring handles all the Criteria API plumbing                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Database Tables (same as Criteria API section):

  user_details                                          user_addresses
  ┌────┬───────────┬──────────────────┬────────┐        ┌────┬─────────┬──────────┬─────────┬────────────┐
  │ id │ user_name │ email            │ active │        │ id │ street  │ city     │ country │ user_id(FK)│
  ├────┼───────────┼──────────────────┼────────┤        ├────┼─────────┼──────────┼─────────┼────────────┤
  │  1 │ Alice     │ alice@ex.com     │ true   │        │ 10 │ Main St │ New York │ USA     │     1      │
  │  2 │ Bob       │ bob@ex.com       │ true   │        │ 11 │ Oak Ave │ London   │ UK      │     2      │
  │  3 │ Charlie   │ charlie@ex.com   │ false  │        │ 12 │ Pine Rd │ Mumbai   │ India   │     1      │
  │  4 │ Diana     │ diana@ex.com     │ true   │        │ 13 │ Elm St  │ Paris    │ France  │     3      │
  │  5 │ Eve       │ eve@ex.com       │ false  │        │ 14 │ Bay Dr  │ New York │ USA     │     4      │
  │  6 │ Frank     │ frank@ex.com     │ true   │        └────┴─────────┴──────────┴─────────┴────────────┘
  │  7 │ Grace     │ grace@ex.com     │ true   │
  │  8 │ Hank      │ hank@ex.com      │ false  │
  └────┴───────────┴──────────────────┴────────┘
```

---

### Problem 1: Code Duplicity in Criteria API — Solved by Specification.toPredicate

**The Problem:** With raw Criteria API, the same predicate logic is duplicated across multiple service methods. If the "active = true" condition appears in 10 different search methods, you write `cb.equal(root.get("active"), true)` in all 10 places. When the business rule changes (e.g., "active" is renamed to "enabled"), you must update all 10 methods.

```java
// PROBLEM — Criteria API code duplicity:
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    // Method 1: search by name + active
    public List<UserDetails> searchByNameAndActive(String name, Boolean active) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);
        List<Predicate> preds = new ArrayList<>();
        if (name != null) {
            preds.add(cb.like(root.get("name"), "%" + name + "%"));  // ← DUPLICATED
        }
        if (active != null) {
            preds.add(cb.equal(root.get("active"), active));         // ← DUPLICATED
        }
        cq.select(root).where(preds.toArray(new Predicate[0]));
        return entityManager.createQuery(cq).getResultList();
    }

    // Method 2: search by email + active — SAME active predicate!
    public List<UserDetails> searchByEmailAndActive(String email, Boolean active) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);
        Root<UserDetails> root = cq.from(UserDetails.class);
        List<Predicate> preds = new ArrayList<>();
        if (email != null) {
            preds.add(cb.equal(root.get("email"), email));
        }
        if (active != null) {
            preds.add(cb.equal(root.get("active"), active));         // ← SAME CODE AGAIN
        }
        cq.select(root).where(preds.toArray(new Predicate[0]));
        return entityManager.createQuery(cq).getResultList();
    }

    // Method 3: count active users — SAME active predicate AGAIN!
    public long countActiveUsers() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<UserDetails> root = cq.from(UserDetails.class);
        cq.select(cb.count(root));
        cq.where(cb.equal(root.get("active"), true));               // ← SAME CODE AGAIN
        return entityManager.createQuery(cq).getSingleResult();
    }

    // The "active" predicate is written 3 times.
    // If "active" field is renamed to "enabled" → must update ALL 3 methods.
    // If the active check changes (e.g., check active AND NOT deleted) → update ALL.
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Code Duplicity Problem — Visual:                                                │
│                                                                                  │
│  searchByNameAndActive():                                                        │
│    cb.like(root.get("name"), ...)              ← unique to this method           │
│    cb.equal(root.get("active"), active)        ← DUPLICATED                      │
│                                                                                  │
│  searchByEmailAndActive():                                                       │
│    cb.equal(root.get("email"), email)          ← unique to this method           │
│    cb.equal(root.get("active"), active)        ← SAME predicate, copied again    │
│                                                                                  │
│  countActiveUsers():                                                             │
│    cb.equal(root.get("active"), true)          ← SAME predicate, copied AGAIN    │
│                                                                                  │
│  searchByNameInCity():                                                           │
│    cb.like(root.get("name"), ...)              ← copied from method 1            │
│    cb.equal(root.get("active"), active)        ← copied AGAIN (4th time)         │
│    cb.equal(addrJoin.get("city"), city)        ← unique to this method           │
│                                                                                  │
│  As the application grows:                                                       │
│    10 methods × same "active" check = 10 copies of the same code                │
│    Bug in one? Must fix all 10.                                                  │
│    Rename field? Must update all 10.                                             │
│    DRY principle violated!                                                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**The Solution: `Specification<T>` — Encapsulate Each Predicate:**

The `Specification<T>` interface has a single method:

```java
// The Specification interface (from Spring Data JPA):
@FunctionalInterface
public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);
}
```

Each `Specification` wraps ONE predicate (WHERE condition). You define it once and reuse it everywhere.

```java
// SOLUTION — Define each predicate as a reusable Specification:
public class UserSpecs {

    // Specification for: WHERE user_name LIKE '%name%'
    public static Specification<UserDetails> hasName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
        //      ↑ Root   ↑ CriteriaQuery  ↑ CriteriaBuilder
        //      These are provided by Spring when the Specification is executed.
        //      You just define WHAT predicate to create.
    }

    // Specification for: WHERE email = :email
    public static Specification<UserDetails> hasEmail(String email) {
        return (root, query, cb) -> cb.equal(root.get("email"), email);
    }

    // Specification for: WHERE active = :active
    public static Specification<UserDetails> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    // Specification for: WHERE active = true (no parameter)
    public static Specification<UserDetails> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }

    // Specification for: WHERE id IN (:ids)
    public static Specification<UserDetails> hasIdIn(List<Long> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }

    // Specification for: WHERE id BETWEEN :min AND :max
    public static Specification<UserDetails> hasIdBetween(Long min, Long max) {
        return (root, query, cb) -> cb.between(root.get("id"), min, max);
    }

    // Each predicate is defined ONCE. Reused across ALL service methods.
    // If "active" is renamed to "enabled" → change ONE place (isActive method).
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification.toPredicate — How it works:                                       │
│                                                                                  │
│  Specification<UserDetails> spec = UserSpecs.hasName("Alice");                   │
│                                                                                  │
│  What hasName("Alice") returns:                                                  │
│    A lambda: (root, query, cb) -> cb.like(root.get("name"), "%Alice%")           │
│                                                                                  │
│  This lambda is NOT executed immediately.                                        │
│  It's a "recipe" for creating a Predicate.                                       │
│                                                                                  │
│  When Spring executes the Specification:                                         │
│    1. Spring creates CriteriaBuilder, CriteriaQuery, Root internally             │
│    2. Spring calls spec.toPredicate(root, query, cb)                             │
│    3. The lambda executes: cb.like(root.get("name"), "%Alice%")                  │
│    4. Returns a Predicate                                                        │
│    5. Spring adds the Predicate to the CriteriaQuery's WHERE clause              │
│    6. Spring creates TypedQuery, executes, returns results                        │
│                                                                                  │
│  You provide the WHAT (predicate logic).                                         │
│  Spring handles the HOW (CriteriaBuilder, CriteriaQuery, execution).            │
│                                                                                  │
│  Defined ONCE:                               Reused EVERYWHERE:                  │
│  ┌─────────────────────────┐                 ┌─────────────────────────────────┐ │
│  │ UserSpecs.hasName(name) │ ────────────→   │ searchByNameAndActive()         │ │
│  │ UserSpecs.isActive()    │ ──┬──────────→  │ searchByEmailAndActive()        │ │
│  │ UserSpecs.hasEmail(e)   │   ├──────────→  │ countActiveUsers()              │ │
│  └─────────────────────────┘   └──────────→  │ searchByNameInCity()            │ │
│                                              └─────────────────────────────────┘ │
│                                                                                  │
│  Change "active" → "enabled"? Fix ONLY isActive(). All methods auto-updated.    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Problem 2: Boilerplate in Criteria API — Solved by JpaSpecificationExecutor

**The Problem:** Even after extracting predicates into `Specification` objects, you still need to write the `CriteriaBuilder` → `CriteriaQuery` → `Root` → `TypedQuery` boilerplate in every service method to execute them.

**The Solution:** `JpaSpecificationExecutor<T>` is a Spring Data interface that provides pre-built methods for executing Specifications. You just extend it on your repository — no `EntityManager` needed.

```java
// Repository — extend JpaSpecificationExecutor
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long>,
                                               JpaSpecificationExecutor<UserDetails> {
    // That's it! No method declarations needed.
    // JpaSpecificationExecutor provides:
    //   List<T> findAll(Specification<T> spec)
    //   Page<T> findAll(Specification<T> spec, Pageable pageable)
    //   List<T> findAll(Specification<T> spec, Sort sort)
    //   Optional<T> findOne(Specification<T> spec)
    //   long count(Specification<T> spec)
    //   boolean exists(Specification<T> spec)
    //   void delete(Specification<T> spec)
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JpaSpecificationExecutor — Methods provided:                                    │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────┬─────────────────────────┐│
│  │ Method                                              │ SQL Equivalent          ││
│  ├─────────────────────────────────────────────────────┼─────────────────────────┤│
│  │ findAll(Specification<T> spec)                      │ SELECT * WHERE ...      ││
│  │ findAll(Specification<T> spec, Sort sort)           │ SELECT * WHERE ...      ││
│  │                                                     │   ORDER BY ...          ││
│  │ findAll(Specification<T> spec, Pageable pageable)   │ SELECT * WHERE ...      ││
│  │                                                     │   ORDER BY ... LIMIT .. ││
│  │ findOne(Specification<T> spec)                      │ SELECT * WHERE ... (1)  ││
│  │ count(Specification<T> spec)                        │ SELECT COUNT(*) WHERE ..││
│  │ exists(Specification<T> spec)                       │ SELECT EXISTS(...)      ││
│  │ delete(Specification<T> spec)                       │ DELETE WHERE ...        ││
│  └─────────────────────────────────────────────────────┴─────────────────────────┘│
│                                                                                  │
│  ALL of these methods internally:                                                │
│    1. Get CriteriaBuilder from EntityManager                                     │
│    2. Create CriteriaQuery                                                       │
│    3. Create Root                                                                │
│    4. Call spec.toPredicate(root, query, cb) to get your Predicate               │
│    5. Apply the Predicate to WHERE clause                                        │
│    6. Create TypedQuery and execute                                              │
│    7. Return results                                                             │
│                                                                                  │
│  You NEVER write this boilerplate yourself!                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// BEFORE — Criteria API (with boilerplate):
@Service
public class UserServiceBefore {

    @PersistenceContext
    private EntityManager entityManager;   // ← need EntityManager

    public List<UserDetails> searchByNameAndActive(String name, Boolean active) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();              // boilerplate
        CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);   // boilerplate
        Root<UserDetails> root = cq.from(UserDetails.class);                 // boilerplate
        List<Predicate> preds = new ArrayList<>();                           // boilerplate
        if (name != null) preds.add(cb.like(root.get("name"), "%" + name + "%"));
        if (active != null) preds.add(cb.equal(root.get("active"), active));
        cq.select(root).where(preds.toArray(new Predicate[0]));             // boilerplate
        TypedQuery<UserDetails> query = entityManager.createQuery(cq);       // boilerplate
        return query.getResultList();                                        // boilerplate
    }
    // 9 lines of boilerplate for every method!
}
```

```java
// AFTER — Specification API (no boilerplate):
@Service
public class UserServiceAfter {

    @Autowired
    private UserDetailsRepository repository;  // ← just the repository

    public List<UserDetails> searchByNameAndActive(String name, Boolean active) {
        Specification<UserDetails> spec = Specification.where(null);  // start with no condition

        if (name != null) {
            spec = spec.and(UserSpecs.hasName(name));
        }
        if (active != null) {
            spec = spec.and(UserSpecs.isActive(active));
        }

        return repository.findAll(spec);  // Spring handles ALL the Criteria API plumbing
    }
    // ZERO boilerplate! Just compose Specifications and call findAll().
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Boilerplate Comparison:                                                         │
│                                                                                  │
│  Criteria API (per method):                    Specification API (per method):    │
│  ─────────────────────────                     ──────────────────────────────     │
│  CriteriaBuilder cb = ...;                     Specification<T> spec = where(null│
│  CriteriaQuery<T> cq = ...;                    );                                │
│  Root<T> root = ...;                           spec = spec.and(hasName(name));    │
│  List<Predicate> preds = ...;                  spec = spec.and(isActive(active)); │
│  if (...) preds.add(cb.like(...));             return repo.findAll(spec);         │
│  if (...) preds.add(cb.equal(...));                                              │
│  cq.select(root).where(...);                   TOTAL: 4 lines                    │
│  TypedQuery<T> query = ...;                                                      │
│  return query.getResultList();                                                   │
│  TOTAL: 9 lines                                                                  │
│                                                                                  │
│  With 10 search methods:                                                         │
│    Criteria API: 10 × 9 = 90 lines of boilerplate                               │
│    Specification: 10 × 4 = 40 lines total + ~20 lines for Specification defs     │
│    = 60 lines total (33% less) + predicates are REUSABLE                         │
│                                                                                  │
│  The real win is reusability:                                                    │
│    If 10 methods use "isActive" → defined ONCE, not 10 times                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Flow — Specification API execution:                                             │
│                                                                                  │
│  Controller: GET /api/users/search?name=Alice&active=true                        │
│       │                                                                          │
│       v                                                                          │
│  Service: searchByNameAndActive("Alice", true)                                   │
│       │                                                                          │
│       ├─ Specification.where(null)          → empty spec (no condition)           │
│       ├─ spec.and(UserSpecs.hasName("Alice"))  → adds name LIKE '%Alice%'        │
│       ├─ spec.and(UserSpecs.isActive(true))    → adds active = true              │
│       │                                                                          │
│       v                                                                          │
│  repository.findAll(spec)                                                        │
│       │                                                                          │
│       v                                                                          │
│  Spring Data JPA internally:                                                     │
│    CriteriaBuilder cb = entityManager.getCriteriaBuilder();                      │
│    CriteriaQuery<UserDetails> cq = cb.createQuery(UserDetails.class);            │
│    Root<UserDetails> root = cq.from(UserDetails.class);                          │
│    Predicate p = spec.toPredicate(root, cq, cb);                                │
│    //  → cb.and(                                                                 │
│    //      cb.like(root.get("name"), "%Alice%"),                                 │
│    //      cb.equal(root.get("active"), true)                                    │
│    //    )                                                                       │
│    cq.where(p);                                                                  │
│    TypedQuery<UserDetails> query = entityManager.createQuery(cq);                │
│    return query.getResultList();                                                 │
│       │                                                                          │
│       v                                                                          │
│  Generated SQL:                                                                  │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.user_name LIKE '%Alice%' AND u1_0.active = true                    │
│       │                                                                          │
│       v                                                                          │
│  Returns List<UserDetails> → Controller → JSON response                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Complete Setup — Specifications + Repository + Service + Controller

**Step 1: Specification class (define reusable predicates):**

```java
// UserSpecs.java — all specifications for UserDetails entity
public class UserSpecs {

    public static Specification<UserDetails> hasName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<UserDetails> hasEmail(String email) {
        return (root, query, cb) -> cb.equal(root.get("email"), email);
    }

    public static Specification<UserDetails> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<UserDetails> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }

    public static Specification<UserDetails> hasIdIn(List<Long> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }

    public static Specification<UserDetails> emailContains(String keyword) {
        return (root, query, cb) -> cb.like(root.get("email"), "%" + keyword + "%");
    }

    public static Specification<UserDetails> hasIdGreaterThan(Long id) {
        return (root, query, cb) -> cb.greaterThan(root.get("id"), id);
    }
}
```

**Step 2: Repository (extend JpaSpecificationExecutor):**

```java
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long>,
                                               JpaSpecificationExecutor<UserDetails> {
    // No custom methods needed for Specification-based queries.
    // All findAll(spec), findAll(spec, pageable), count(spec), etc. are inherited.
}
```

**Step 3: Service (compose Specifications dynamically):**

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Dynamic search — any combination of filters.
     */
    public List<UserDetails> searchUsers(String name, String email, Boolean active) {

        Specification<UserDetails> spec = Specification.where(null);
        //  Specification.where(null) starts with NO condition (returns all rows).
        //  Equivalent to "WHERE 1=1" in dynamic native queries.

        if (name != null && !name.isEmpty()) {
            spec = spec.and(UserSpecs.hasName(name));
        }
        if (email != null && !email.isEmpty()) {
            spec = spec.and(UserSpecs.hasEmail(email));
        }
        if (active != null) {
            spec = spec.and(UserSpecs.isActive(active));
        }

        return repository.findAll(spec);
    }

    /**
     * Count active users — reuses the same isActive() Specification
     */
    public long countActiveUsers() {
        return repository.count(UserSpecs.isActive());
        //  Spring generates: SELECT COUNT(*) FROM user_details WHERE active = true
    }

    /**
     * Check if user exists — reuses hasEmail() Specification
     */
    public boolean existsByEmail(String email) {
        return repository.exists(UserSpecs.hasEmail(email));
        //  Spring generates: SELECT 1 FROM user_details WHERE email = 'alice@ex.com' LIMIT 1
    }
}
```

**Step 4: Controller:**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users/search?name=Alice
    // GET /api/users/search?active=true
    // GET /api/users/search?name=Alice&email=alice@ex.com&active=true
    @GetMapping("/search")
    public List<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Boolean active
    ) {
        return userService.searchUsers(name, email, active);
    }

    @GetMapping("/count-active")
    public long countActive() {
        return userService.countActiveUsers();
    }

    @GetMapping("/exists")
    public boolean exists(@RequestParam String email) {
        return userService.existsByEmail(email);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification API — Generated SQL for each scenario:                            │
│                                                                                  │
│  Scenario 1: searchUsers("Alice", null, null)                                    │
│    spec = where(null).and(hasName("Alice"))                                      │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Alice%'                                         │
│    Result: [UserDetails(id=1, name="Alice", ...)]                                │
│                                                                                  │
│  Scenario 2: searchUsers(null, null, true)                                       │
│    spec = where(null).and(isActive(true))                                        │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.active = true                                                    │
│    Result: [Alice, Bob, Diana, Frank, Grace] — 5 active users                    │
│                                                                                  │
│  Scenario 3: searchUsers("Bob", "bob@ex.com", true)                              │
│    spec = where(null).and(hasName("Bob")).and(hasEmail("bob@ex.com"))             │
│                       .and(isActive(true))                                       │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Bob%'                                           │
│        AND u1_0.email = 'bob@ex.com'                                             │
│        AND u1_0.active = true                                                    │
│    Result: [UserDetails(id=2, name="Bob", ...)]                                  │
│                                                                                  │
│  Scenario 4: searchUsers(null, null, null)                                       │
│    spec = where(null) — no .and() called                                         │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│    Result: All 8 users (no WHERE clause)                                         │
│                                                                                  │
│  Scenario 5: countActiveUsers()                                                  │
│    spec = isActive()                                                             │
│    Generated SQL:                                                                │
│      SELECT COUNT(u1_0.id) FROM user_details u1_0 WHERE u1_0.active = true       │
│    Result: 5                                                                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### AND, OR, NOT, and Complex Predicates with Specification API

Specifications support composition using `.and()`, `.or()`, and `Specification.not()`.

```java
// UserSpecs.java — reusable specifications
public class UserSpecs {

    public static Specification<UserDetails> hasName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<UserDetails> hasEmail(String email) {
        return (root, query, cb) -> cb.equal(root.get("email"), email);
    }

    public static Specification<UserDetails> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<UserDetails> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }

    public static Specification<UserDetails> emailContains(String keyword) {
        return (root, query, cb) -> cb.like(root.get("email"), "%" + keyword + "%");
    }

    public static Specification<UserDetails> hasIdGreaterThan(Long id) {
        return (root, query, cb) -> cb.greaterThan(root.get("id"), id);
    }

    public static Specification<UserDetails> hasIdBetween(Long min, Long max) {
        return (root, query, cb) -> cb.between(root.get("id"), min, max);
    }
}
```

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * AND — all conditions must match
     * Find active users whose name contains "a"
     */
    public List<UserDetails> findActiveByName(String name) {
        Specification<UserDetails> spec = Specification
            .where(UserSpecs.hasName(name))
            .and(UserSpecs.isActive());
        return repository.findAll(spec);
        // SQL: WHERE user_name LIKE '%a%' AND active = true
    }

    /**
     * OR — any condition can match
     * Find users where name contains keyword OR email contains keyword
     */
    public List<UserDetails> searchByKeyword(String keyword) {
        Specification<UserDetails> spec = Specification
            .where(UserSpecs.hasName(keyword))
            .or(UserSpecs.emailContains(keyword));
        return repository.findAll(spec);
        // SQL: WHERE user_name LIKE '%keyword%' OR email LIKE '%keyword%'
    }

    /**
     * NOT — negate a specification
     * Find inactive users (NOT active)
     */
    public List<UserDetails> findInactiveUsers() {
        Specification<UserDetails> spec = Specification.not(UserSpecs.isActive());
        return repository.findAll(spec);
        // SQL: WHERE NOT (active = true)
        // Same as: WHERE active = false
    }

    /**
     * Complex: (name LIKE keyword OR email LIKE keyword) AND active = true AND id > 2
     */
    public List<UserDetails> complexSearch(String keyword, Long minId) {

        // Build OR condition: name LIKE keyword OR email LIKE keyword
        Specification<UserDetails> keywordSpec = Specification
            .where(UserSpecs.hasName(keyword))
            .or(UserSpecs.emailContains(keyword));

        // Combine with AND
        Specification<UserDetails> spec = Specification
            .where(keywordSpec)                            // (name LIKE OR email LIKE)
            .and(UserSpecs.isActive())                     // AND active = true
            .and(UserSpecs.hasIdGreaterThan(minId));       // AND id > minId

        return repository.findAll(spec);
    }

    /**
     * Dynamic AND + OR with optional parameters
     */
    public List<UserDetails> dynamicSearch(String name, String email,
                                            Boolean active, String keyword) {

        Specification<UserDetails> spec = Specification.where(null);

        if (name != null) {
            spec = spec.and(UserSpecs.hasName(name));
        }
        if (email != null) {
            spec = spec.and(UserSpecs.hasEmail(email));
        }
        if (active != null) {
            spec = spec.and(UserSpecs.isActive(active));
        }
        if (keyword != null) {
            // OR within the keyword filter, AND with the rest
            Specification<UserDetails> keywordSpec = Specification
                .where(UserSpecs.hasName(keyword))
                .or(UserSpecs.emailContains(keyword));
            spec = spec.and(keywordSpec);
        }

        return repository.findAll(spec);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification Composition — Generated SQL:                                      │
│                                                                                  │
│  AND:                                                                            │
│    where(hasName("a")).and(isActive())                                            │
│    SQL: WHERE user_name LIKE '%a%' AND active = true                             │
│                                                                                  │
│  OR:                                                                             │
│    where(hasName("ali")).or(emailContains("ali"))                                 │
│    SQL: WHERE user_name LIKE '%ali%' OR email LIKE '%ali%'                       │
│                                                                                  │
│  NOT:                                                                            │
│    Specification.not(isActive())                                                 │
│    SQL: WHERE NOT (active = true)                                                │
│                                                                                  │
│  Complex: (OR group) AND active AND id > 2                                       │
│    where(                                                                        │
│      where(hasName("ali")).or(emailContains("ali"))                               │
│    ).and(isActive()).and(hasIdGreaterThan(2L))                                    │
│    SQL: WHERE (user_name LIKE '%ali%' OR email LIKE '%ali%')                     │
│           AND active = true                                                      │
│           AND id > 2                                                             │
│    Result from DB:                                                               │
│      ┌────┬───────────┬──────────────────┬────────┐                              │
│      │ id │ user_name │ email            │ active │                              │
│      ├────┼───────────┼──────────────────┼────────┤                              │
│      │  4 │ Diana     │ diana@ex.com     │ true   │ ← id > 2, active, name has a│
│      │  6 │ Frank     │ frank@ex.com     │ true   │ ← id > 2, active, name has a│
│      │  7 │ Grace     │ grace@ex.com     │ true   │ ← id > 2, active, name has a│
│      └────┴───────────┴──────────────────┴────────┘                              │
│    (Alice matches "ali" but id=1, not > 2)                                       │
│                                                                                  │
│  Dynamic: name="Bob" + keyword="bob"                                             │
│    spec = where(null)                                                            │
│           .and(hasName("Bob"))                                                   │
│           .and( where(hasName("bob")).or(emailContains("bob")) )                  │
│    SQL: WHERE user_name LIKE '%Bob%'                                             │
│           AND (user_name LIKE '%bob%' OR email LIKE '%bob%')                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification Composition Methods:                                              │
│                                                                                  │
│  ┌──────────────────────────────────────────┬────────────────────────────────────┐│
│  │ Method                                   │ SQL Equivalent                    ││
│  ├──────────────────────────────────────────┼────────────────────────────────────┤│
│  │ Specification.where(spec)                │ Start a chain (initial condition) ││
│  │ Specification.where(null)                │ No initial condition (1=1)        ││
│  │ spec.and(otherSpec)                      │ cond1 AND cond2                   ││
│  │ spec.or(otherSpec)                       │ cond1 OR cond2                    ││
│  │ Specification.not(spec)                  │ NOT (cond)                        ││
│  │ spec.and(spec2.or(spec3))               │ cond1 AND (cond2 OR cond3)        ││
│  └──────────────────────────────────────────┴────────────────────────────────────┘│
│                                                                                  │
│  Note on .where(null):                                                           │
│    Specification.where(null) returns a Specification that produces NO predicate.  │
│    When Spring sees a null Specification, it skips the WHERE clause entirely.    │
│    This is how you handle the "no filters" case cleanly.                         │
│                                                                                  │
│    spec.and(null) is also safe — the null Specification is ignored.              │
│    This means you can write:                                                     │
│      spec = spec.and(name != null ? UserSpecs.hasName(name) : null);             │
│    → If name is null, the .and(null) is a no-op.                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Cleaner dynamic search using ternary with null:**

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Clean dynamic search using ternary null pattern
     */
    public List<UserDetails> searchUsers(String name, String email, Boolean active) {

        Specification<UserDetails> spec = Specification
            .where(name != null ? UserSpecs.hasName(name) : null)
            .and(email != null ? UserSpecs.hasEmail(email) : null)
            .and(active != null ? UserSpecs.isActive(active) : null);
        //  null Specifications are ignored in .and() and .where()!

        return repository.findAll(spec);
    }
    // If name="Alice", email=null, active=true:
    //   where(hasName("Alice"))       ← name is non-null
    //   .and(null)                    ← email is null → ignored
    //   .and(isActive(true))          ← active is non-null
    //   SQL: WHERE user_name LIKE '%Alice%' AND active = true
}
```

---

### JOIN and Multiselect with Specification API

The Specification API's `toPredicate` method receives `Root`, `CriteriaQuery`, and `CriteriaBuilder` — the same objects used in the Criteria API. So you can perform JOINs inside a Specification. However, `JpaSpecificationExecutor.findAll()` always returns entities (not `Object[]`), so multiselect requires using EntityManager directly with the Specifications providing predicates.

**JOIN inside a Specification — filtering by child entity fields:**

```java
// UserSpecs.java — JOIN specifications
public class UserSpecs {

    // Existing specs...
    public static Specification<UserDetails> hasName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<UserDetails> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }

    // JOIN spec — filter by address city
    public static Specification<UserDetails> hasCity(String city) {
        return (root, query, cb) -> {
            // JOIN user_addresses ON user_details.id = user_addresses.user_id
            Join<UserDetails, UserAddress> addressJoin = root.join("addresses");
            //                                                ↑ Java field name on UserDetails

            // Prevent duplicate results from JOIN (one user may have multiple addresses)
            query.distinct(true);

            return cb.equal(addressJoin.get("city"), city);
            // SQL: ... JOIN user_addresses a ON u.id = a.user_id WHERE a.city = :city
        };
    }

    // JOIN spec — filter by address country
    public static Specification<UserDetails> hasCountry(String country) {
        return (root, query, cb) -> {
            Join<UserDetails, UserAddress> addressJoin = root.join("addresses");
            query.distinct(true);
            return cb.equal(addressJoin.get("country"), country);
        };
    }

    // LEFT JOIN spec — include users without addresses
    public static Specification<UserDetails> hasCityOptional(String city) {
        return (root, query, cb) -> {
            Join<UserDetails, UserAddress> addressJoin = root.join("addresses", JoinType.LEFT);
            query.distinct(true);
            return cb.equal(addressJoin.get("city"), city);
        };
    }
}
```

```java
// Service — composing JOIN specifications with other specifications
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Find active users in a specific city
     */
    public List<UserDetails> findActiveUsersInCity(String city) {
        Specification<UserDetails> spec = Specification
            .where(UserSpecs.isActive())
            .and(UserSpecs.hasCity(city));
        return repository.findAll(spec);
    }

    /**
     * Dynamic search with optional city filter
     */
    public List<UserDetails> search(String name, Boolean active, String city, String country) {
        Specification<UserDetails> spec = Specification
            .where(name != null ? UserSpecs.hasName(name) : null)
            .and(active != null ? UserSpecs.isActive() : null)
            .and(city != null ? UserSpecs.hasCity(city) : null)
            .and(country != null ? UserSpecs.hasCountry(country) : null);
        return repository.findAll(spec);
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users/search?name=Alice&city=New York
    // GET /api/users/search?active=true&country=USA
    @GetMapping("/search")
    public List<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String country
    ) {
        return userService.search(name, active, city, country);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification JOIN — Generated SQL:                                             │
│                                                                                  │
│  findActiveUsersInCity("New York"):                                              │
│    spec = where(isActive()).and(hasCity("New York"))                              │
│    Generated SQL:                                                                │
│      SELECT DISTINCT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active           │
│      FROM user_details u1_0                                                      │
│      JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                          │
│      WHERE u1_0.active = true AND a1_0.city = 'New York'                         │
│                                                                                  │
│    Result:                                                                       │
│      ┌────┬───────────┬──────────────────┬────────┐                              │
│      │ id │ user_name │ email            │ active │                              │
│      ├────┼───────────┼──────────────────┼────────┤                              │
│      │  1 │ Alice     │ alice@ex.com     │ true   │ ← address in New York        │
│      │  4 │ Diana     │ diana@ex.com     │ true   │ ← address in New York        │
│      └────┴───────────┴──────────────────┴────────┘                              │
│                                                                                  │
│  search("Alice", null, null, null):                                              │
│    spec = where(hasName("Alice")).and(null).and(null).and(null)                   │
│    Generated SQL:                                                                │
│      SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                    │
│      FROM user_details u1_0                                                      │
│      WHERE u1_0.user_name LIKE '%Alice%'                                         │
│    → No JOIN (city and country are null → JOIN specs not added)                   │
│                                                                                  │
│  search(null, true, "London", "UK"):                                             │
│    spec = where(null).and(isActive()).and(hasCity("London"))                      │
│                       .and(hasCountry("UK"))                                     │
│    Generated SQL:                                                                │
│      SELECT DISTINCT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active           │
│      FROM user_details u1_0                                                      │
│      JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id                          │
│      WHERE u1_0.active = true AND a1_0.city = 'London' AND a1_0.country = 'UK'  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Multiselect with Specification API — using EntityManager for Object[] results:**

`JpaSpecificationExecutor.findAll()` always returns full entities. For partial column selection (multiselect/Object[]), you can use Specifications as predicate providers inside an EntityManager-based approach:

```java
@Service
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Multiselect (specific columns) — uses Specification as predicate provider
     * but executes via EntityManager for Object[] result.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUserNamesWithCity(String city, Boolean active) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<UserDetails> root = cq.from(UserDetails.class);

        Join<UserDetails, UserAddress> addrJoin = root.join("addresses");

        cq.multiselect(root.get("name"), addrJoin.get("city"));

        // Build WHERE using Specification objects
        List<Predicate> predicates = new ArrayList<>();
        if (city != null) {
            // Call Specification.toPredicate manually
            predicates.add(
                UserSpecs.hasCity(city).toPredicate(root, cq, cb)
            );
        }
        if (active != null) {
            predicates.add(
                UserSpecs.isActive(active).toPredicate(root, cq, cb)
            );
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        return entityManager.createQuery(cq).getResultList();
    }
    // Generated SQL:
    //   SELECT u1_0.user_name, a1_0.city
    //   FROM user_details u1_0
    //   JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id
    //   WHERE a1_0.city = 'New York' AND u1_0.active = true
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification API + Multiselect — Two Approaches:                               │
│                                                                                  │
│  Approach 1: JpaSpecificationExecutor.findAll(spec)                              │
│    → Always returns full entities (List<UserDetails>)                            │
│    → Handles all boilerplate                                                     │
│    → CANNOT do multiselect / Object[]                                            │
│    → Use for 90% of queries                                                      │
│                                                                                  │
│  Approach 2: EntityManager + spec.toPredicate() manually                         │
│    → Can do multiselect → Object[]                                               │
│    → Still reuses Specification predicate logic (no code duplication)             │
│    → Requires some boilerplate (CriteriaBuilder, Root, etc.)                     │
│    → Use for the 10% of queries that need partial columns                        │
│                                                                                  │
│  In Approach 2, you call spec.toPredicate(root, query, cb) manually              │
│  to extract the Predicate from a Specification and apply it to your custom query.│
│  The predicate logic is still defined once in UserSpecs.                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  NOTE — Duplicate JOIN issue:                                                    │
│                                                                                  │
│  If two Specifications both call root.join("addresses"), two separate JOINs      │
│  are created:                                                                    │
│    hasCity("New York")  → root.join("addresses") → JOIN #1                       │
│    hasCountry("USA")    → root.join("addresses") → JOIN #2                       │
│    SQL: ... JOIN user_addresses a1 ON ... JOIN user_addresses a2 ON ...           │
│    → Two JOINs on the same table! Inefficient but correct.                       │
│                                                                                  │
│  To avoid duplicate JOINs, either:                                               │
│  1. Combine conditions into one Specification:                                   │
│     public static Specification<UserDetails> hasAddress(String city, String co) { │
│         return (root, query, cb) -> {                                            │
│             Join<UserDetails, UserAddress> join = root.join("addresses");         │
│             query.distinct(true);                                                │
│             List<Predicate> preds = new ArrayList<>();                            │
│             if (city != null) preds.add(cb.equal(join.get("city"), city));        │
│             if (co != null) preds.add(cb.equal(join.get("country"), co));         │
│             return cb.and(preds.toArray(new Predicate[0]));                       │
│         };                                                                       │
│     }                                                                            │
│                                                                                  │
│  2. Reuse existing JOINs by checking root.getJoins():                            │
│     Join<?, ?> join = root.getJoins().stream()                                   │
│         .filter(j -> j.getAttribute().getName().equals("addresses"))              │
│         .findFirst()                                                             │
│         .orElseGet(() -> root.join("addresses"));                                │
│     → Uses existing JOIN if already present, creates new one if not.             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Pagination and Sorting with Specification API

`JpaSpecificationExecutor` provides `findAll(Specification, Pageable)` which handles pagination and sorting automatically. No manual `setFirstResult`/`setMaxResults` or `orderBy` needed.

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Dynamic search with pagination and sorting.
     */
    public Page<UserDetails> searchUsersPaginated(String name, String email,
                                                   Boolean active,
                                                   int page, int size,
                                                   String sortBy, String sortDir) {

        // Build Specification dynamically
        Specification<UserDetails> spec = Specification
            .where(name != null ? UserSpecs.hasName(name) : null)
            .and(email != null ? UserSpecs.hasEmail(email) : null)
            .and(active != null ? UserSpecs.isActive(active) : null);

        // Build Sort
        Sort sort;
        List<String> allowedSortFields = List.of("id", "name", "email", "active");
        if (sortBy != null && allowedSortFields.contains(sortBy)) {
            sort = Sort.by(
                "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
            );
        } else {
            sort = Sort.by(Sort.Direction.ASC, "id");  // default sort
        }

        // Build Pageable (page number + page size + sort)
        Pageable pageable = PageRequest.of(page, size, sort);

        // Execute — Spring handles EVERYTHING:
        //   1. Creates CriteriaBuilder, CriteriaQuery, Root
        //   2. Calls spec.toPredicate() for WHERE clause
        //   3. Adds ORDER BY from Sort
        //   4. Adds LIMIT/OFFSET from Pageable
        //   5. Executes DATA query
        //   6. Executes COUNT query (for totalElements)
        //   7. Returns Page<UserDetails>
        return repository.findAll(spec, pageable);
    }

    /**
     * Search with sorting only (no pagination — returns List, not Page)
     */
    public List<UserDetails> searchUsersSorted(Boolean active, String sortBy) {
        Specification<UserDetails> spec = active != null ? UserSpecs.isActive(active) : null;

        Sort sort = Sort.by(Sort.Direction.ASC, sortBy != null ? sortBy : "id");

        return repository.findAll(spec, sort);
    }
}
```

```java
// Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users/search?active=true&page=0&size=3&sortBy=name&sortDir=DESC
    @GetMapping("/search")
    public Page<UserDetails> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        return userService.searchUsersPaginated(name, email, active,
                                                 page, size, sortBy, sortDir);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification + Pagination — Scenario:                                          │
│                                                                                  │
│  Request: GET /api/users/search?active=true&page=1&size=2&sortBy=name&sortDir=ASC│
│                                                                                  │
│  spec = where(null).and(null).and(isActive(true))                                │
│  pageable = PageRequest.of(1, 2, Sort.by(ASC, "name"))                           │
│                                                                                  │
│  Spring internally generates TWO queries:                                        │
│                                                                                  │
│  Data query:                                                                     │
│    SELECT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active                      │
│    FROM user_details u1_0                                                        │
│    WHERE u1_0.active = true                                                      │
│    ORDER BY u1_0.user_name ASC                                                   │
│    LIMIT 2 OFFSET 2                                                              │
│                                                                                  │
│  Count query:                                                                    │
│    SELECT COUNT(u1_0.id) FROM user_details u1_0 WHERE u1_0.active = true         │
│    → Returns: 5                                                                  │
│                                                                                  │
│  Active users sorted by name: Alice(1), Bob(2), Diana(4), Frank(6), Grace(7)    │
│  Page 0 (OFFSET 0, LIMIT 2): [Alice, Bob]                                       │
│  Page 1 (OFFSET 2, LIMIT 2): [Diana, Frank]  ← THIS page                        │
│  Page 2 (OFFSET 4, LIMIT 2): [Grace]                                            │
│                                                                                  │
│  Response:                                                                       │
│    Page<UserDetails> {                                                           │
│      content: [Diana, Frank],                                                    │
│      pageNumber: 1,                                                              │
│      pageSize: 2,                                                                │
│      totalElements: 5,                                                           │
│      totalPages: 3                                                               │
│    }                                                                             │
│                                                                                  │
│  KEY ADVANTAGE over Criteria API pagination:                                     │
│    - No manual setFirstResult/setMaxResults                                      │
│    - No manual count query (Spring generates it automatically!)                  │
│    - No manual PageImpl construction                                             │
│    - Just: repository.findAll(spec, pageable) → Page<T>                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Pagination with JOIN Specifications:**

```java
@Service
public class UserService {

    @Autowired
    private UserDetailsRepository repository;

    /**
     * Paginated search with JOIN filter
     */
    public Page<UserDetails> searchInCity(String city, Boolean active,
                                           int page, int size) {

        Specification<UserDetails> spec = Specification
            .where(city != null ? UserSpecs.hasCity(city) : null)
            .and(active != null ? UserSpecs.isActive(active) : null);

        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));

        return repository.findAll(spec, pageable);
    }
    // SQL (city="New York", active=true, page=0, size=2):
    //   SELECT DISTINCT u1_0.id, u1_0.user_name, u1_0.email, u1_0.active
    //   FROM user_details u1_0
    //   JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id
    //   WHERE a1_0.city = 'New York' AND u1_0.active = true
    //   ORDER BY u1_0.user_name ASC
    //   LIMIT 2 OFFSET 0
    //
    // Count SQL (auto-generated):
    //   SELECT COUNT(DISTINCT u1_0.id)
    //   FROM user_details u1_0
    //   JOIN user_addresses a1_0 ON u1_0.id = a1_0.user_id
    //   WHERE a1_0.city = 'New York' AND u1_0.active = true
}
```

**Multi-column sorting:**

```java
// Sort by active DESC, then by name ASC
Sort sort = Sort.by(
    Sort.Order.desc("active"),
    Sort.Order.asc("name")
);
Pageable pageable = PageRequest.of(0, 10, sort);

// SQL: ... ORDER BY u1_0.active DESC, u1_0.user_name ASC LIMIT 10 OFFSET 0
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification Pagination vs Criteria API Pagination:                            │
│                                                                                  │
│  Criteria API:                                                                   │
│    // Data query                                                                 │
│    CriteriaBuilder cb = entityManager.getCriteriaBuilder();                      │
│    CriteriaQuery<UserDetails> dataCq = cb.createQuery(UserDetails.class);        │
│    Root<UserDetails> dataRoot = dataCq.from(UserDetails.class);                  │
│    dataCq.select(dataRoot).where(predicates);                                    │
│    dataCq.orderBy(cb.asc(dataRoot.get("name")));                                 │
│    TypedQuery<UserDetails> dataQuery = entityManager.createQuery(dataCq);        │
│    dataQuery.setFirstResult(page * size);                                        │
│    dataQuery.setMaxResults(size);                                                │
│    List<UserDetails> content = dataQuery.getResultList();                         │
│    // Count query (separate!)                                                    │
│    CriteriaQuery<Long> countCq = cb.createQuery(Long.class);                     │
│    Root<UserDetails> countRoot = countCq.from(UserDetails.class);                │
│    countCq.select(cb.count(countRoot)).where(countPredicates);                   │
│    Long total = entityManager.createQuery(countCq).getSingleResult();            │
│    return new PageImpl<>(content, pageable, total);                               │
│    → 14 lines of boilerplate!                                                    │
│                                                                                  │
│  Specification API:                                                              │
│    Pageable pageable = PageRequest.of(page, size, Sort.by("name"));              │
│    return repository.findAll(spec, pageable);                                    │
│    → 2 lines! Spring handles data query, count query, and PageImpl.              │
│                                                                                  │
│  Specification API eliminates:                                                   │
│    ✓ Manual CriteriaBuilder/CriteriaQuery/Root setup                             │
│    ✓ Manual orderBy with cb.asc()/cb.desc()                                      │
│    ✓ Manual setFirstResult/setMaxResults                                         │
│    ✓ Manual count query creation and execution                                   │
│    ✓ Manual PageImpl construction                                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Summary — Specification API

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Specification API — Complete Pattern:                                           │
│                                                                                  │
│  1. Define Specifications (once):                                                │
│     public class UserSpecs {                                                     │
│         static Specification<UserDetails> hasName(String name) {                 │
│             return (root, query, cb) -> cb.like(root.get("name"), "%" +name+ "%"│
│             );                                                                   │
│         }                                                                        │
│         static Specification<UserDetails> isActive() {                           │
│             return (root, query, cb) -> cb.equal(root.get("active"), true);      │
│         }                                                                        │
│     }                                                                            │
│                                                                                  │
│  2. Repository (extend JpaSpecificationExecutor):                                │
│     interface UserDetailsRepository                                              │
│         extends JpaRepository<UserDetails, Long>,                                │
│                 JpaSpecificationExecutor<UserDetails> { }                         │
│                                                                                  │
│  3. Compose and Execute (in service):                                            │
│     Specification<UserDetails> spec = Specification                              │
│         .where(name != null ? hasName(name) : null)                              │
│         .and(active != null ? isActive() : null);                                │
│     repository.findAll(spec);                     // List<T>                     │
│     repository.findAll(spec, pageable);           // Page<T>                     │
│     repository.findAll(spec, sort);               // List<T> sorted              │
│     repository.count(spec);                       // long                        │
│     repository.exists(spec);                      // boolean                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
Complete Decision Table — All Query Approaches (updated):

  ┌─────────────────────────────────────┬──────────────────────────────────────────┐
  │ Scenario                            │ Best Approach                            │
  ├─────────────────────────────────────┼──────────────────────────────────────────┤
  │ Simple findBy with 1-2 conditions   │ Derived Query (method name)              │
  │ Fixed complex query on entities     │ JPQL @Query on Repository                │
  │ Fixed complex query on tables       │ Native @Query on Repository              │
  │ DB-specific features (JSONB, etc.)  │ Native @Query on Repository              │
  │ Dynamic query, DB-locked            │ Dynamic Native Query (EntityManager)     │
  │ Dynamic query, DB-independent       │ Criteria API or Specification API        │
  │ Dynamic query, type-safe            │ Criteria API + Metamodel                 │
  │ Reusable dynamic predicates         │ Specification API                        │
  │ Dynamic query + minimal boilerplate │ Specification API                        │
  │ Dynamic JOINs (related entities)    │ Specification API or Criteria API        │
  │ Dynamic JOINs (unrelated tables)    │ Dynamic Native Query (EntityManager)     │
  │ Multiselect (partial columns)       │ Criteria API (multiselect)               │
  │ Pagination + sorting + dynamic WHERE│ Specification API (simplest)             │
  │ Dynamic GROUP BY / HAVING           │ Criteria API or Dynamic Native Query     │
  │ Dynamic UPDATE (partial fields)     │ Dynamic Native Query (EntityManager)     │
  │ Maximum readability                 │ JPQL or Native Query (SQL is familiar)   │
  │ Maximum type safety                 │ Criteria API + Metamodel                 │
  │ Maximum DB portability              │ Specification API, Criteria API, or JPQL │
  │ Maximum reusability                 │ Specification API                        │
  └─────────────────────────────────────┴──────────────────────────────────────────┘
```

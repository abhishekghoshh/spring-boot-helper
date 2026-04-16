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


- [Concept && Coding - by Shrayansh](https://www.youtube.com/@ConceptAndCodingByShrayansh)
    - 

- [Spring Data JPA Tutorial | Full In-depth Course](https://www.youtube.com/watch?v=XszpXoII9Sg)
- [Master Spring Data JPA In One Video | Hindi](https://www.youtube.com/watch?v=8SxJNqeq_zc)



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




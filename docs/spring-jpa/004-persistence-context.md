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


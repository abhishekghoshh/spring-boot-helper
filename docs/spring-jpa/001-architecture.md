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


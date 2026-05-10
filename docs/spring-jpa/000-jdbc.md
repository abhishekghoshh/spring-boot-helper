# Spring JDBC

## Blogs

- [Spring Persistence Series](https://www.baeldung.com/persistence-with-spring-series)
    - [Spring JDBC](https://www.baeldung.com/spring-jdbc-jdbctemplate)
- [JDBC in Java](https://www.tpointtech.com/jdbc-tutorial)


## Medium

- [CRUD Operations using Spring Boot, JdbcTemplate, and MySQL](https://medium.com/@gauravshah97/crud-operations-using-spring-boot-jdbctemplate-and-mysql-aa9cc7855025)


## Youtube

- [JDBC Tutorial - Crash Course](https://www.youtube.com/watch?v=KgXq2UBNEhA)
    - [marcobehlerjetbrains/jdbc-tutorial](https://github.com/marcobehlerjetbrains/jdbc-tutorial)
- [Java Database Connectivity | JDBC](https://www.youtube.com/watch?v=7v2OnUti2eM)

- [Spring Security JDBC: How to authenticate against a database in Spring Boot](https://www.youtube.com/watch?v=d7ZmZFbE_qY)



## Reading Chronology

### Phase 1: The Foundations (No Magic Yet)
Before relying on Spring or Hibernate to do the heavy lifting, you need to understand how Java actually talks to a database.

1. **JDBC (Java Database Connectivity)**  
   The absolute bedrock. Learn how to open a connection, create a statement, loop through a `ResultSet`, and handle SQL exceptions. You will quickly see how repetitive and verbose this is.
2. **JdbcTemplate**  
   Spring's solution to JDBC verbosity. Learn how Spring handles opening and closing connections automatically, allowing you to focus on SQL and result mapping.

### Phase 2: Introduction to ORM and JPA (Basic Magic)
Now you transition from writing raw SQL to Object-Relational Mapping (ORM).

3. **JPA (Java Persistence API)**  
   Understand the concept. JPA is the specification (rules), and Hibernate is usually the implementation (engine).
4. **EntityManager**  
   The core JPA interface. Learn how it manages entity lifecycle operations: `persist`, `merge`, `remove`, and `find`.
5. **Map Entity to Table with JPA annotations**  
   Map entities (not DTOs) using `@Entity`, `@Table`, `@Id`, and related annotations. Learn basic CRUD operations.

### Phase 3: Relational Mapping (Connecting Entities)
Databases are relational; your Java objects should be too.

6. **One-to-One (Unidirectional and Bidirectional)**  
   Learn how to link two entities using `@OneToOne` and `@JoinColumn`, and understand owning side vs inverse side (`mappedBy`).
7. **One-to-Many and Many-to-One (Unidirectional and Bidirectional)**  
   The most common relationship pattern. Master parent-child mappings and navigation in both directions.

### Phase 4: Querying and Core Pitfalls
`findById` is not enough for real-world apps. You need custom queries.

8. **JPQL, Joins, Pagination, Sorting**  
   Learn JPQL (querying Java objects instead of database tables), plus `Pageable` and sorting support in Spring Data JPA.
9. **Native Query**  
   When JPQL is not enough for database-specific or complex operations, use native SQL via `@Query(nativeQuery = true)`.
10. **The N+1 Problem**  
	Critical performance pitfall when relationships and pagination are involved. Learn to fix it using `JOIN FETCH` or Entity Graphs.

### Phase 5: Dynamic Querying
If users can filter by any combination of fields, hardcoded JPQL will not scale.

11. **Criteria API (and its challenge)**  
	The standard JPA way to build dynamic queries programmatically, but often verbose and hard to read.
12. **Specification API**  
	Spring Data JPA's wrapper around Criteria API for cleaner, reusable dynamic `WHERE` clauses.

### Phase 6: Advanced Optimization (Caching)
Once your app works and queries are optimized, reduce DB load further using caching.

13. **First-level cache**  
	Built-in and automatic. Scoped to `EntityManager`/Session. Re-fetching the same entity in one transaction usually avoids extra DB calls.
14. **Second-level cache**  
	Shared across sessions/transactions (typically backed by tools like Ehcache or Redis) for read-heavy optimization.

## How JDBC Works

JDBC works as a bridge between your Java application and the database.

1. Load/register a JDBC driver.
2. Create a connection using `DriverManager` (or a datasource/pool).
3. Create a statement (`Statement` or `PreparedStatement`).
4. Execute SQL (`executeQuery`, `executeUpdate`).
5. Read results using `ResultSet`.
6. Close resources (`ResultSet`, `Statement`, `Connection`) safely.

## JDBC Components

- `DriverManager`: Creates DB connections from JDBC URL, username, and password.
- `Connection`: Represents an active connection/session with the database.
- `Statement`: Executes static SQL.
- `PreparedStatement`: Executes parameterized SQL safely (recommended).
- `ResultSet`: Cursor-like structure for reading query results row by row.
- `SQLException`: Exception type for JDBC/database-related errors.

## JDBC Connection and CRUD Examples

```java
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcEmployeeRepository {

	private static final String URL = "jdbc:mysql://localhost:3306/company";
	private static final String USER = "root";
	private static final String PASSWORD = "root";

	// Create and get a Connection
	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL, USER, PASSWORD);
	}

	// CREATE
	public int createEmployee(String name, String email, int age) throws SQLException {
		String sql = "INSERT INTO employees(name, email, age) VALUES (?, ?, ?)";
		try (Connection con = getConnection();
			 PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, name);
			ps.setString(2, email);
			ps.setInt(3, age);
			return ps.executeUpdate();
		}
	}

	// READ by id
	public Employee getEmployeeById(long id) throws SQLException {
		String sql = "SELECT id, name, email, age FROM employees WHERE id = ?";
		try (Connection con = getConnection();
			 PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setLong(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return new Employee(
							rs.getLong("id"),
							rs.getString("name"),
							rs.getString("email"),
							rs.getInt("age")
					);
				}
			}
		}
		return null;
	}

	// READ with WHERE filters
	public List<Employee> findEmployeesByFilters(String nameLike, Integer minAge) throws SQLException {
		String sql = "SELECT id, name, email, age FROM employees " +
					 "WHERE name LIKE ? AND age >= ? ORDER BY id DESC";

		List<Employee> result = new ArrayList<>();
		try (Connection con = getConnection();
			 PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, "%" + nameLike + "%");
			ps.setInt(2, minAge != null ? minAge : 0);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					result.add(new Employee(
							rs.getLong("id"),
							rs.getString("name"),
							rs.getString("email"),
							rs.getInt("age")
					));
				}
			}
		}
		return result;
	}

	// UPDATE
	public int updateEmployeeEmail(long id, String newEmail) throws SQLException {
		String sql = "UPDATE employees SET email = ? WHERE id = ?";
		try (Connection con = getConnection();
			 PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, newEmail);
			ps.setLong(2, id);
			return ps.executeUpdate();
		}
	}

	// DELETE
	public int deleteEmployee(long id) throws SQLException {
		String sql = "DELETE FROM employees WHERE id = ?";
		try (Connection con = getConnection();
			 PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setLong(1, id);
			return ps.executeUpdate();
		}
	}

	public record Employee(long id, String name, String email, int age) {}
}
```

## JdbcTemplate Connection and CRUD Examples

`JdbcTemplate` eliminates the boilerplate of raw JDBC (manual `try/catch`, closing connections, etc.) while still giving you full control over SQL.

**Maven dependency**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

**`application.properties`**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/company
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

```java
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcTemplateEmployeeRepository {

    private final JdbcTemplate jdbcTemplate;

    // Spring auto-configures and injects a DataSource; JdbcTemplate wraps it
    public JdbcTemplateEmployeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // RowMapper reusable for all SELECT queries
    private final RowMapper<Employee> rowMapper = (rs, rowNum) -> new Employee(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getInt("age")
    );

    // CREATE
    public int createEmployee(String name, String email, int age) {
        String sql = "INSERT INTO employees(name, email, age) VALUES (?, ?, ?)";
        return jdbcTemplate.update(sql, name, email, age);
    }

    // READ by id
    public Employee getEmployeeById(long id) {
        String sql = "SELECT id, name, email, age FROM employees WHERE id = ?";
        // Returns null if not found (queryForObject throws EmptyResultDataAccessException)
        return jdbcTemplate.query(sql, rowMapper, id)
                .stream()
                .findFirst()
                .orElse(null);
    }

    // READ all
    public List<Employee> getAllEmployees() {
        String sql = "SELECT id, name, email, age FROM employees ORDER BY id DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    // READ with WHERE filters
    public List<Employee> findEmployeesByFilters(String nameLike, int minAge) {
        String sql = "SELECT id, name, email, age FROM employees " +
                     "WHERE name LIKE ? AND age >= ? ORDER BY id DESC";
        return jdbcTemplate.query(sql, rowMapper, "%" + nameLike + "%", minAge);
    }

    // UPDATE
    public int updateEmployeeEmail(long id, String newEmail) {
        String sql = "UPDATE employees SET email = ? WHERE id = ?";
        return jdbcTemplate.update(sql, newEmail, id);
    }

    // DELETE
    public int deleteEmployee(long id) {
        String sql = "DELETE FROM employees WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    public record Employee(long id, String name, String email, int age) {}
}
```
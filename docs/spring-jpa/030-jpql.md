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


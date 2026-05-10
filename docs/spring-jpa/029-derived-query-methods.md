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


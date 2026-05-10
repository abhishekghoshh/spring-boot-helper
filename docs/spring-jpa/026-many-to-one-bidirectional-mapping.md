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


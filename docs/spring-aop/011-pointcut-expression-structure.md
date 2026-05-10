### Pointcut Expression Structure — Complete Breakdown

The **pointcut expression** is the string you write inside `@Before(...)`, `@Around(...)`, etc. It follows a specific **syntax** that Spring AOP parses to determine which methods to intercept.

The most common pointcut type is `execution(...)`. Here is its **complete structure**:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  execution() Pointcut Expression — Full Syntax:                                  │
│                                                                                  │
│  execution( modifiers?  return-type  declaring-type?  method-name(params)  throws? )
│             ────┬────   ─────┬─────  ──────┬───────   ─────┬─────  ──┬──  ───┬───│
│                 │            │             │               │        │      │    │
│                 │            │             │               │        │      │    │
│           (optional)    (REQUIRED)    (optional)      (REQUIRED)   │  (optional)│
│            public        * or type   package.Class    methodName  args  exception│
│            protected     void/String                  or *               type   │
│                          etc.                                                    │
│                                                                                  │
│  ┌────────────────┬──────────┬───────────────────────────────────────────────────┐│
│  │ Part           │ Required │ Description                                       ││
│  ├────────────────┼──────────┼───────────────────────────────────────────────────┤│
│  │ modifiers      │ NO       │ public, protected, etc. Rarely used.              ││
│  │ return-type    │ YES      │ The return type of the method.                    ││
│  │                │          │ * = any return type.                              ││
│  │ declaring-type │ NO       │ The fully qualified class/package.                ││
│  │                │          │ If omitted, matches any class.                    ││
│  │ method-name    │ YES      │ The method name. * = any method.                  ││
│  │ params         │ YES      │ The parameter list. (..) = any params.            ││
│  │ throws         │ NO       │ The exception types. Rarely used.                 ││
│  └────────────────┴──────────┴───────────────────────────────────────────────────┘│
│                                                                                  │
│  Minimum expression (only required parts):                                       │
│    execution(* *(..))                                                            │
│              ↑ ↑  ↑                                                              │
│              │ │  └── any params                                                 │
│              │ └── any method name                                               │
│              └── any return type                                                 │
│    → Matches EVERY method in EVERY class (way too broad — never use this!)       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Part 1: Package, Class, and Method Name

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Declaring-Type (Package + Class) and Method Name:                               │
│                                                                                  │
│  execution(* com.app.service.OrderService.createOrder(..))                       │
│              └──────┬───────  ─────┬──────  ─────┬────────┘                      │
│                     │              │             │                                │
│                 PACKAGE         CLASS        METHOD NAME                          │
│          (com.app.service)  (OrderService)  (createOrder)                        │
│                                                                                  │
│  Together, the package + class form the "declaring-type":                        │
│    com.app.service.OrderService                                                  │
│    └──────────────────────────┘                                                  │
│         declaring-type                                                           │
│                                                                                  │
│  The declaring-type is OPTIONAL:                                                 │
│    execution(* createOrder(..))                                                  │
│    → Matches createOrder() in ANY class in ANY package                           │
│                                                                                  │
│    execution(* com.app.service.OrderService.createOrder(..))                     │
│    → Matches createOrder() ONLY in OrderService class                            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Match a SPECIFIC method in a SPECIFIC class ───

// Match: OrderService.createOrder()
@Before("execution(* com.app.service.OrderService.createOrder(..))")
public void beforeCreateOrder(JoinPoint jp) { }

// Match: PaymentService.processPayment()
@Before("execution(* com.app.service.PaymentService.processPayment(..))")
public void beforeProcessPayment(JoinPoint jp) { }
```

```java
// ─── Match ALL methods in a SPECIFIC class ───

// Match: any method in OrderService
@Before("execution(* com.app.service.OrderService.*(..))")
//                                                 ↑
//                                          * = any method name
public void beforeAnyOrderMethod(JoinPoint jp) { }

// Results:
//   OrderService.createOrder(...)   → MATCHES ✓
//   OrderService.getOrder(...)      → MATCHES ✓
//   OrderService.cancelOrder(...)   → MATCHES ✓
//   PaymentService.process(...)     → NO MATCH ✗
```

```java
// ─── Match ALL methods in ALL classes in a SPECIFIC package ───

// Match: any method in any class in com.app.service package
@Before("execution(* com.app.service.*.*(..))")
//                                  ↑ ↑
//                     any class ───┘ └── any method
public void beforeAnyServiceMethod(JoinPoint jp) { }

// Results:
//   OrderService.createOrder(...)     → MATCHES ✓ (in service package)
//   PaymentService.process(...)       → MATCHES ✓ (in service package)
//   UserService.createUser(...)       → MATCHES ✓ (in service package)
//   OrderRepository.save(...)         → NO MATCH ✗ (repository package)
//   OrderController.create(...)       → NO MATCH ✗ (controller package)
```

```java
// ─── Match ALL methods in a package AND all SUB-PACKAGES ───

// Match: any method in com.app.service and its sub-packages
@Before("execution(* com.app.service..*.*(..))")
//                                   ↑↑
//                                   .. = this package AND all sub-packages
public void beforeAnyServiceOrSubPackage(JoinPoint jp) { }

// Package structure:
//   com.app.service/
//     OrderService.java            → MATCHES ✓ (direct package)
//     PaymentService.java          → MATCHES ✓ (direct package)
//   com.app.service.order/
//     OrderDetailService.java      → MATCHES ✓ (sub-package)
//   com.app.service.payment/
//     PaymentGatewayService.java   → MATCHES ✓ (sub-package)
//   com.app.service.payment.stripe/
//     StripeService.java           → MATCHES ✓ (nested sub-package)
//   com.app.repository/
//     OrderRepository.java         → NO MATCH ✗ (different package tree)
```

```java
// ─── Match methods with a SPECIFIC name pattern ───

// Match: any method starting with "get" in any service class
@Before("execution(* com.app.service.*.get*(..))")
//                                        ↑
//                                    get* = starts with "get"
public void beforeAnyGetter(JoinPoint jp) { }

// Results:
//   OrderService.getOrder(...)        → MATCHES ✓ (starts with "get")
//   OrderService.getAll()             → MATCHES ✓ (starts with "get")
//   UserService.getUserById(...)      → MATCHES ✓ (starts with "get")
//   OrderService.createOrder(...)     → NO MATCH ✗ (starts with "create")

// Match: any method starting with "create" or "save"
@Before("execution(* com.app.service.*.create*(..)) || " +
        "execution(* com.app.service.*.save*(..))")
public void beforeCreateOrSave(JoinPoint jp) { }

// Match: any method ending with "Order"
@Before("execution(* com.app.service.*.*Order(..))")
public void beforeOrderMethods(JoinPoint jp) { }
// Matches: createOrder, getOrder, cancelOrder, deleteOrder, updateOrder
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Package + Class + Method — Pattern Examples:                                    │
│                                                                                  │
│  ┌──────────────────────────────────────────────────┬────────────────────────────┐│
│  │ Expression                                       │ What it matches            ││
│  ├──────────────────────────────────────────────────┼────────────────────────────┤│
│  │ * com.app.service.OrderService.createOrder(..)   │ One specific method        ││
│  │ * com.app.service.OrderService.*(..)             │ All methods in one class   ││
│  │ * com.app.service.*.*(..)                        │ All methods in one package ││
│  │ * com.app.service..*.*(..)                       │ Package + sub-packages     ││
│  │ * com.app..*.*(..)                               │ Entire app (all packages)  ││
│  │ * com.app.service.*.get*(..)                     │ Methods starting with get  ││
│  │ * com.app.service.*.*Order(..)                   │ Methods ending with Order  ││
│  │ * *.createOrder(..)                              │ createOrder in any class   ││
│  │ * createOrder(..)                                │ createOrder in any class   ││
│  │                                                  │ (no declaring-type)        ││
│  └──────────────────────────────────────────────────┴────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Part 2: Return Types

The **return type** is the first required part of the `execution()` expression. It specifies what the method must return for the pointcut to match.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Return Type Position:                                                           │
│                                                                                  │
│  execution( RETURN_TYPE  declaring-type.method-name(params) )                    │
│             ↑                                                                    │
│             First thing after "execution("                                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Match ANY return type ───
@Before("execution(* com.app.service.OrderService.*(..))")
//                  ↑
//              * = any return type (void, String, Order, List, etc.)
public void matchAnyReturn(JoinPoint jp) { }

// Matches:
//   public Order createOrder(...)           → ✓ (returns Order)
//   public List<Order> getAllOrders()        → ✓ (returns List)
//   public void cancelOrder(...)            → ✓ (returns void)
//   public String getStatus(...)            → ✓ (returns String)
```

```java
// ─── Match VOID return type only ───
@Before("execution(void com.app.service.OrderService.*(..))")
//                  ↑
//              void = only methods that return nothing
public void matchOnlyVoid(JoinPoint jp) { }

// Matches:
//   public void cancelOrder(Long id)        → ✓ (void)
//   public void deleteOrder(Long id)        → ✓ (void)
//   public Order createOrder(...)           → ✗ (returns Order, not void)
//   public String getStatus(...)            → ✗ (returns String, not void)
```

```java
// ─── Match a SPECIFIC return type ───

// Only methods that return Order
@Before("execution(com.app.entity.Order com.app.service.OrderService.*(..))")
//                  └────────────────┘
//                  fully qualified return type
public void matchOrderReturn(JoinPoint jp) { }

// Matches:
//   public Order createOrder(...)           → ✓ (returns Order)
//   public Order getOrder(Long id)          → ✓ (returns Order)
//   public List<Order> getAllOrders()        → ✗ (returns List<Order>, NOT Order)
//   public void cancelOrder(...)            → ✗ (returns void)

// Only methods that return String
@Before("execution(String com.app.service.OrderService.*(..))")
public void matchStringReturn(JoinPoint jp) { }

// Only methods that return List (note: generics are erased at runtime)
@Before("execution(java.util.List com.app.service.OrderService.*(..))")
public void matchListReturn(JoinPoint jp) { }
// Matches: List<Order>, List<String>, List<Payment> — all are just "List" at runtime
```

```java
// ─── Match boolean return type ───
@Before("execution(boolean com.app.service.*.*(..))")
public void matchBooleanReturn(JoinPoint jp) { }

// Matches:
//   public boolean isOrderValid(Long id)    → ✓
//   public boolean hasPermission(...)       → ✓
//   public Boolean isActive(...)            → ✗ (Boolean wrapper ≠ boolean primitive!)

// For wrapper type:
@Before("execution(java.lang.Boolean com.app.service.*.*(..))")
public void matchBooleanWrapperReturn(JoinPoint jp) { }
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Return Type — Examples Summary:                                                 │
│                                                                                  │
│  ┌─────────────────────────────────────┬─────────────────────────────────────────┐│
│  │ Return type in expression           │ What it matches                         ││
│  ├─────────────────────────────────────┼─────────────────────────────────────────┤│
│  │ *                                   │ ANY return type (most common)           ││
│  │ void                                │ Methods that return nothing             ││
│  │ String                              │ Methods returning String                ││
│  │ int                                 │ Methods returning primitive int         ││
│  │ java.lang.Integer                   │ Methods returning Integer wrapper       ││
│  │ com.app.entity.Order                │ Methods returning Order entity          ││
│  │ java.util.List                      │ Methods returning any List              ││
│  │ java.util.Map                       │ Methods returning any Map               ││
│  │ org.springframework.http            │ Methods returning ResponseEntity        ││
│  │   .ResponseEntity                   │                                         ││
│  └─────────────────────────────────────┴─────────────────────────────────────────┘│
│                                                                                  │
│  IMPORTANT:                                                                      │
│    • For types outside java.lang, use FULLY QUALIFIED class name                 │
│    • String works without package because it's in java.lang                      │
│    • int ≠ Integer — primitive vs wrapper are different types                    │
│    • Generics are ERASED — List<Order> and List<String> both match "List"        │
│    • Use * (wildcard) 99% of the time — filtering by return type is rare         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Part 3: Arguments — With Parameters, Without Parameters

The **parameter list** (inside parentheses) specifies what arguments the method must accept for the pointcut to match.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Parameter List Position:                                                        │
│                                                                                  │
│  execution( return-type  declaring-type.method-name( PARAMS ) )                  │
│                                                      ↑                           │
│                                            Inside the parentheses                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── Match methods with ANY parameters (including no parameters) ───
@Before("execution(* com.app.service.OrderService.*(..))")
//                                                    ↑↑
//                                                 (..) = zero or more arguments of any type
public void matchAnyParams(JoinPoint jp) { }

// Matches:
//   createOrder()                              → ✓ (0 args)
//   createOrder(String product)                → ✓ (1 arg)
//   createOrder(String product, Double amount) → ✓ (2 args)
//   getOrder(Long id)                          → ✓ (1 arg)
```

```java
// ─── Match methods with NO parameters (zero arguments) ───
@Before("execution(* com.app.service.OrderService.*())")
//                                                  ↑↑
//                                               () = exactly zero arguments
public void matchNoParams(JoinPoint jp) { }

// Matches:
//   getAllOrders()                              → ✓ (0 args)
//   getCount()                                 → ✓ (0 args)
//   createOrder(String product)                → ✗ (has 1 arg)
//   createOrder(String product, Double amount) → ✗ (has 2 args)
```

```java
// ─── Match methods with EXACTLY one parameter of a SPECIFIC type ───

// One String parameter
@Before("execution(* com.app.service.OrderService.*(String))")
//                                                    ↑
//                                          exactly one String arg
public void matchOneString(JoinPoint jp) { }

// Matches:
//   getByStatus(String status)                 → ✓ (1 String)
//   findByProduct(String product)              → ✓ (1 String)
//   getOrder(Long id)                          → ✗ (Long, not String)
//   createOrder(String product, Double amount) → ✗ (2 args, not 1)

// One Long parameter
@Before("execution(* com.app.service.OrderService.*(Long))")
public void matchOneLong(JoinPoint jp) { }

// Matches:
//   getOrder(Long id)                          → ✓
//   deleteOrder(Long id)                       → ✓
//   getOrder(long id)                          → ✗ (long ≠ Long!)
```

```java
// ─── Match methods with EXACTLY two specific parameters ───
@Before("execution(* com.app.service.OrderService.*(String, Double))")
//                                                    ↑        ↑
//                                              1st arg   2nd arg
public void matchStringAndDouble(JoinPoint jp) { }

// Matches:
//   createOrder(String product, Double amount) → ✓ (String, Double)
//   createOrder(String name, Double price)     → ✓ (String, Double)
//   createOrder(Double amount, String product) → ✗ (ORDER matters! Double, String)
//   createOrder(String product)                → ✗ (only 1 arg, needs 2)
```

```java
// ─── Match methods with a custom object type parameter ───
@Before("execution(* com.app.service.OrderService.*(com.app.dto.OrderRequest))")
//                                                    └──────────────────────┘
//                                                    fully qualified DTO class
public void matchOrderRequest(JoinPoint jp) { }

// Matches:
//   createOrder(OrderRequest req)              → ✓
//   updateOrder(OrderRequest req)              → ✓
//   createOrder(String product, Double amount) → ✗ (different param types)
```

```java
// ─── Match methods with one specific param followed by any number of params ───
@Before("execution(* com.app.service.OrderService.*(String, ..))")
//                                                    ↑       ↑↑
//                                              first must  rest can be
//                                              be String   anything
public void matchStringThenAnything(JoinPoint jp) { }

// Matches:
//   findByProduct(String product)                        → ✓ (String + 0 more)
//   createOrder(String product, Double amount)           → ✓ (String + 1 more)
//   search(String query, int page, int size)             → ✓ (String + 2 more)
//   getOrder(Long id)                                    → ✗ (first arg is Long, not String)
//   getAllOrders()                                        → ✗ (no args at all)
```

```java
// ─── Match methods with EXACTLY one parameter of ANY type ───
@Before("execution(* com.app.service.OrderService.*(*))")
//                                                    ↑
//                                             (*) = exactly one arg of any type
public void matchExactlyOneParam(JoinPoint jp) { }

// Matches:
//   getOrder(Long id)              → ✓ (1 arg)
//   findByStatus(String status)    → ✓ (1 arg)
//   process(OrderRequest req)      → ✓ (1 arg)
//   getAllOrders()                  → ✗ (0 args)
//   create(String p, Double a)     → ✗ (2 args)
```

```java
// ─── Match methods with EXACTLY two parameters of ANY type ───
@Before("execution(* com.app.service.OrderService.*(*, *))")
//                                                    ↑  ↑
//                                              any  any (exactly 2)
public void matchExactlyTwoParams(JoinPoint jp) { }

// Matches:
//   createOrder(String product, Double amount) → ✓ (2 args)
//   update(Long id, OrderRequest req)          → ✓ (2 args)
//   getOrder(Long id)                          → ✗ (1 arg)
//   search(String q, int page, int size)       → ✗ (3 args)
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Parameter Patterns — Complete Reference:                                        │
│                                                                                  │
│  ┌──────────────────────────┬────────────────────────────────────────────────────┐│
│  │ Pattern                  │ What it matches                                    ││
│  ├──────────────────────────┼────────────────────────────────────────────────────┤│
│  │ (..)                     │ Zero or more args of any type (most common)        ││
│  │ ()                       │ Exactly zero args                                  ││
│  │ (*)                      │ Exactly one arg of any type                        ││
│  │ (*, *)                   │ Exactly two args of any type                       ││
│  │ (*, *, *)                │ Exactly three args of any type                     ││
│  │ (String)                 │ Exactly one arg of type String                     ││
│  │ (Long)                   │ Exactly one arg of type Long                       ││
│  │ (String, Double)         │ Exactly: first String, second Double               ││
│  │ (String, ..)             │ First arg String, then zero or more of any type    ││
│  │ (.., String)             │ Any args, but last arg must be String              ││
│  │ (String, *, ..)          │ First String, then one of any, then zero or more   ││
│  │ (com.app.dto.OrderReq)  │ Exactly one arg of custom DTO type                 ││
│  │ (com.app.dto.OrderReq,  │ Custom DTO + Long (two specific args)              ││
│  │  Long)                   │                                                    ││
│  └──────────────────────────┴────────────────────────────────────────────────────┘│
│                                                                                  │
│  NOTE: Argument ORDER matters!                                                   │
│    (String, Long) ≠ (Long, String)                                               │
│    The types must match in the EXACT positions.                                   │
│                                                                                  │
│  NOTE: Primitive vs Wrapper are DIFFERENT:                                        │
│    (int)    matches method(int x)        but NOT method(Integer x)               │
│    (Long)   matches method(Long x)       but NOT method(long x)                  │
│    (double) matches method(double x)     but NOT method(Double x)                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Part 4: Wildcards — `*`, `..`, and Where to Use Each

Spring AOP provides two wildcard characters: `*` (single wildcard) and `..` (multi-level wildcard). Each has specific places where it can be used and each means something different depending on context.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Wildcard: * (STAR)                                                              │
│                                                                                  │
│  Meaning: "matches ONE thing" — what it matches depends on WHERE it's used:     │
│                                                                                  │
│  ┌───────────────────────┬───────────────────────────────────────────────────────┐│
│  │ Position              │ What * means                                          ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Return type           │ Any return type (void, String, Order, etc.)           ││
│  │   execution(* ...)    │                                                       ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Class name            │ Any class name                                        ││
│  │   com.app.service.*   │ Any class in com.app.service package                  ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Method name           │ Any method name                                       ││
│  │   *.*(...)            │ Any method in any class                               ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Method name (partial) │ Partial match (like a glob)                           ││
│  │   *.get*(...)         │ Methods starting with "get"                           ││
│  │   *.*Order(...)       │ Methods ending with "Order"                           ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Inside params         │ Exactly ONE parameter of any type                     ││
│  │   (*)                 │ One arg of any type                                   ││
│  │   (*, *)              │ Two args of any type                                  ││
│  │   (String, *)         │ First is String, second is any type                   ││
│  └───────────────────────┴───────────────────────────────────────────────────────┘│
│                                                                                  │
│  KEY RULE: * always matches exactly ONE element (one type, one name, one class). │
│            It does NOT match across package levels or multiple parameters.        │
│                                                                                  │
│  Example: com.app.* matches com.app.OrderService                                 │
│           but DOES NOT match com.app.service.OrderService (two levels)            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Wildcard: .. (DOUBLE DOT)                                                       │
│                                                                                  │
│  Meaning: "matches ZERO or MORE things" — depends on WHERE it's used:           │
│                                                                                  │
│  ┌───────────────────────┬───────────────────────────────────────────────────────┐│
│  │ Position              │ What .. means                                         ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ In package path       │ Zero or more sub-package levels                       ││
│  │   com.app..           │ com.app and ALL sub-packages recursively              ││
│  │   com.app.service..   │ com.app.service and ALL its sub-packages             ││
│  ├───────────────────────┼───────────────────────────────────────────────────────┤│
│  │ Inside params         │ Zero or more parameters of any type                   ││
│  │   (..)                │ Any number of args (including none)                   ││
│  │   (String, ..)        │ First is String, then any number of any type         ││
│  │   (.., Long)          │ Any number of any type, last must be Long             ││
│  └───────────────────────┴───────────────────────────────────────────────────────┘│
│                                                                                  │
│  KEY RULE: .. matches ZERO or MORE elements (across package levels or params).   │
│            It CANNOT be used as return type or method name.                       │
│                                                                                  │
│  IMPORTANT:                                                                      │
│    * in package  → matches ONE package level:  com.app.*         → com.app.XYZ   │
│    .. in package → matches MULTIPLE levels:    com.app.service.. → com.app.service│
│                                                                    com.app.service.order│
│                                                                    com.app.service.order.detail│
└──────────────────────────────────────────────────────────────────────────────────┘
```

```java
// ─── * (star) in different positions ───

// Position 1: Return type
@Before("execution(* com.app.service.OrderService.createOrder(..))")
//                  ↑ = any return type

// Position 2: Class name
@Before("execution(* com.app.service.*.createOrder(..))")
//                                    ↑ = any class in the service package

// Position 3: Method name
@Before("execution(* com.app.service.OrderService.*(..))")
//                                                 ↑ = any method in OrderService

// Position 4: Partial method name
@Before("execution(* com.app.service.OrderService.get*(..))")
//                                                 ↑↑↑ = methods starting with "get"
@Before("execution(* com.app.service.OrderService.*Order(..))")
//                                                 ↑     = methods ending with "Order"

// Position 5: In parameter list (one arg of any type)
@Before("execution(* com.app.service.OrderService.*(*))")
//                                                   ↑ = exactly one arg, any type

// Combined: * in multiple positions
@Before("execution(* com.app.service.*.*(*))")
//                  ↑                ↑ ↑  ↑
//              any return     any class  │  one arg any type
//                              any method┘
```

```java
// ─── .. (double dot) in different positions ───

// Position 1: In package path (sub-packages)
@Before("execution(* com.app.service..*.*(..))")
//                                    ↑↑
//                           service package + ALL sub-packages
//  Matches: com.app.service.OrderService.createOrder()
//           com.app.service.order.OrderDetailService.getDetails()
//           com.app.service.order.history.OrderHistoryService.getHistory()

// Position 2: In parameter list (any number of args)
@Before("execution(* com.app.service.OrderService.*(..))")
//                                                    ↑↑
//                                          zero or more args of any type

// Position 3: First param fixed, rest flexible
@Before("execution(* com.app.service.OrderService.*(String, ..))")
//                                                    ↑        ↑↑
//                                              1st=String   rest=anything

// Position 4: Last param fixed, front flexible
@Before("execution(* com.app.service.OrderService.*(.., Long))")
//                                                    ↑↑    ↑
//                                              anything  last=Long
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  * vs .. — Side-by-Side Comparison:                                              │
│                                                                                  │
│  ┌──────────────────────────────────┬──────────────────────────────────────────┐  │
│  │ Using * (single)                 │ Using .. (multi)                         │  │
│  ├──────────────────────────────────┼──────────────────────────────────────────┤  │
│  │ PACKAGE:                         │ PACKAGE:                                 │  │
│  │ com.app.service.*.*(..)          │ com.app.service..*.*(..)                 │  │
│  │ Matches:                         │ Matches:                                 │  │
│  │ ✓ com.app.service.OrderService   │ ✓ com.app.service.OrderService           │  │
│  │ ✓ com.app.service.PaymentService │ ✓ com.app.service.PaymentService         │  │
│  │ ✗ com.app.service.order.Detail   │ ✓ com.app.service.order.Detail           │  │
│  │ ✗ com.app.service.a.b.Deep      │ ✓ com.app.service.a.b.Deep               │  │
│  │ (* = one level only)             │ (.. = any depth)                         │  │
│  ├──────────────────────────────────┼──────────────────────────────────────────┤  │
│  │ PARAMS:                          │ PARAMS:                                  │  │
│  │ method(*)                        │ method(..)                               │  │
│  │ Matches:                         │ Matches:                                 │  │
│  │ ✓ method(String x)              │ ✓ method()                               │  │
│  │ ✓ method(Long y)                │ ✓ method(String x)                       │  │
│  │ ✗ method()                      │ ✓ method(String x, Long y)               │  │
│  │ ✗ method(String x, Long y)     │ ✓ method(String x, Long y, int z)        │  │
│  │ (* = exactly one param)          │ (.. = zero or more params)               │  │
│  └──────────────────────────────────┴──────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Comprehensive Wildcard Examples with Matching Results:**

```java
// Assume this service class structure:
//
// Package: com.app.service
//   OrderService:
//     public Order createOrder(String product, Double amount)
//     public Order getOrder(Long id)
//     public List<Order> getAllOrders()
//     public void cancelOrder(Long id)
//     public Order updateOrder(Long id, OrderRequest req)
//
//   PaymentService:
//     public Payment processPayment(PaymentRequest req)
//     public void refund(Long paymentId)
//
// Package: com.app.service.reporting
//   ReportService:
//     public Report generateReport(String type, int year)
//
// Package: com.app.repository
//   OrderRepository:
//     public Order save(Order order)

// ═══ Example 1: * as return type ═══
@Before("execution(void com.app.service.OrderService.*(..))")
// Matches:
//   ✓ cancelOrder(Long id)               → returns void
//   ✗ createOrder(String, Double)         → returns Order
//   ✗ getOrder(Long)                      → returns Order
//   ✗ getAllOrders()                       → returns List<Order>

// ═══ Example 2: * as class name (one package level) ═══
@Before("execution(* com.app.service.*.*(..))")
// Matches:
//   ✓ OrderService.createOrder(...)       → in service package
//   ✓ OrderService.getOrder(...)          → in service package
//   ✓ PaymentService.processPayment(...)  → in service package
//   ✗ ReportService.generateReport(...)   → in service.reporting (sub-package!)
//   ✗ OrderRepository.save(...)           → in repository package

// ═══ Example 3: .. as sub-package (recursive) ═══
@Before("execution(* com.app.service..*.*(..))")
// Matches:
//   ✓ OrderService.createOrder(...)       → in service package
//   ✓ PaymentService.processPayment(...)  → in service package
//   ✓ ReportService.generateReport(...)   → in service.reporting (sub-package!) ✓
//   ✗ OrderRepository.save(...)           → in repository package (different tree)

// ═══ Example 4: * as partial method name ═══
@Before("execution(* com.app.service.OrderService.get*(..))")
// Matches:
//   ✓ getOrder(Long)                      → starts with "get"
//   ✓ getAllOrders()                       → starts with "get"
//   ✗ createOrder(String, Double)         → starts with "create"
//   ✗ cancelOrder(Long)                   → starts with "cancel"

// ═══ Example 5: (*) one param of any type ═══
@Before("execution(* com.app.service.OrderService.*(*))")
// Matches:
//   ✓ getOrder(Long id)                   → exactly 1 param
//   ✓ cancelOrder(Long id)                → exactly 1 param
//   ✗ createOrder(String, Double)         → 2 params
//   ✗ getAllOrders()                       → 0 params
//   ✗ updateOrder(Long, OrderRequest)     → 2 params

// ═══ Example 6: (String, ..) first param String, rest anything ═══
@Before("execution(* com.app.service.OrderService.*(String, ..))")
// Matches:
//   ✓ createOrder(String product, Double amount)  → String + 1 more
//   ✗ getOrder(Long id)                           → first is Long, not String
//   ✗ getAllOrders()                               → no params at all
//   ✗ cancelOrder(Long id)                        → first is Long, not String

// ═══ Example 7: (*, *) exactly two params of any type ═══
@Before("execution(* com.app.service.OrderService.*(*, *))")
// Matches:
//   ✓ createOrder(String product, Double amount)  → exactly 2 params
//   ✓ updateOrder(Long id, OrderRequest req)      → exactly 2 params
//   ✗ getOrder(Long id)                           → 1 param
//   ✗ getAllOrders()                               → 0 params
//   ✗ cancelOrder(Long id)                        → 1 param
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Wildcard Usage — Where Can You Use Each?                                        │
│                                                                                  │
│  ┌──────────────────┬──────────────────┬─────────────────────────────────────────┐│
│  │ Position         │ * allowed?       │ .. allowed?                             ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Return type      │ ✓ YES            │ ✗ NO                                   ││
│  │                  │ execution(* ...) │ (.. is not a valid return type)         ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Package path     │ ✓ YES            │ ✓ YES                                  ││
│  │                  │ com.app.*        │ com.app..                               ││
│  │                  │ (one level)      │ (zero+ sub-package levels)              ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Class name       │ ✓ YES            │ ✗ NO                                   ││
│  │                  │ com.app.service.*│ (use .. in package path instead)        ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Method name      │ ✓ YES            │ ✗ NO                                   ││
│  │                  │ * or get* or *Or │ (.. is not valid for method names)      ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Parameters       │ ✓ YES            │ ✓ YES                                  ││
│  │                  │ (*) = one arg    │ (..) = zero or more args                ││
│  │                  │ (*, *) = two     │ (String, ..) = String + rest            ││
│  ├──────────────────┼──────────────────┼─────────────────────────────────────────┤│
│  │ Exception type   │ ✓ YES (rare)     │ ✗ NO                                   ││
│  │                  │ * in throws      │                                         ││
│  └──────────────────┴──────────────────┴─────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Complete Pointcut Expression Anatomy — Visual Breakdown

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dissecting a Real Pointcut Expression:                                          │
│                                                                                  │
│  execution(public com.app.entity.Order com.app.service.OrderService.createOrder  │
│            (String, Double) throws RuntimeException)                             │
│                                                                                  │
│  Let's break it down:                                                            │
│                                                                                  │
│  execution(                                                                      │
│    public                          ← modifier (optional, rarely used)            │
│    com.app.entity.Order            ← return type (fully qualified)               │
│    com.app.service.OrderService    ← declaring type (package + class)            │
│    .createOrder                    ← method name                                 │
│    (String, Double)                ← parameter types (exact match)               │
│    throws RuntimeException         ← exception (optional, rarely used)           │
│  )                                                                               │
│                                                                                  │
│  ═══════════════════════════════════════════════════════════════════════════════  │
│                                                                                  │
│  Same method matched with WILDCARDS:                                             │
│                                                                                  │
│  Most specific → Most general:                                                   │
│                                                                                  │
│  1. execution(public com.app.entity.Order                                        │
│               com.app.service.OrderService.createOrder(String, Double)            │
│               throws RuntimeException)                                           │
│     → Matches only THIS exact method signature.                                  │
│                                                                                  │
│  2. execution(* com.app.service.OrderService.createOrder(String, Double))         │
│     → Any return type, specific class + method + params.                         │
│                                                                                  │
│  3. execution(* com.app.service.OrderService.createOrder(..))                     │
│     → Any return type, specific class + method, any params.                      │
│                                                                                  │
│  4. execution(* com.app.service.OrderService.*(..))                               │
│     → Any method in OrderService.                                                │
│                                                                                  │
│  5. execution(* com.app.service.*.*(..))                                          │
│     → Any method in any class in the service package.                            │
│                                                                                  │
│  6. execution(* com.app.service..*.*(..))                                         │
│     → Any method in service package + all sub-packages.                          │
│                                                                                  │
│  7. execution(* com.app..*.*(..))                                                 │
│     → Any method in the entire application.                                      │
│                                                                                  │
│  8. execution(* *(..))                                                            │
│     → ANY method ANYWHERE. (Never use — way too broad!)                          │
│                                                                                  │
│  Specificity spectrum:                                                           │
│                                                                                  │
│    MOST SPECIFIC ◄────────────────────────────────────► MOST GENERAL             │
│    (1)                                                  (8)                       │
│    Matches 1 method                           Matches ALL methods                │
│    in 1 class                                 in ALL classes                      │
│                                                                                  │
│  Best practice: Be as SPECIFIC as possible.                                      │
│  Overly broad pointcuts cause unexpected interception and performance issues.    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Quick Reference — Common Real-World Pointcut Patterns:**

```java
// ─── Most Common Patterns You'll Use in Real Projects ───

// 1. All service methods (most common)
@Around("execution(* com.app.service.*.*(..))")

// 2. All service methods including sub-packages
@Around("execution(* com.app.service..*.*(..))")

// 3. All repository methods
@Around("execution(* com.app.repository.*.*(..))")

// 4. All controller methods
@Before("execution(* com.app.controller.*.*(..))")

// 5. Specific class, all methods
@Before("execution(* com.app.service.OrderService.*(..))")

// 6. Specific method in specific class
@Before("execution(* com.app.service.OrderService.createOrder(..))")

// 7. All getter methods in services
@Before("execution(* com.app.service.*.get*(..))")

// 8. All void methods (fire-and-forget operations)
@After("execution(void com.app.service.*.*(..))")

// 9. Methods that return a specific entity
@AfterReturning(
    pointcut = "execution(com.app.entity.Order com.app.service.*.*(..))",
    returning = "order"
)
public void afterOrderReturned(JoinPoint jp, Order order) { }

// 10. Methods taking a specific DTO
@Before("execution(* com.app.service.*.*(com.app.dto.OrderRequest, ..))")

// 11. All methods across the entire application (use with caution!)
@Around("execution(* com.app..*.*(..))")
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Pointcut Expression — Complete Syntax Cheat Sheet:                              │
│                                                                                  │
│  execution( [modifiers]  return-type  [declaring-type.]method-name(params)       │
│             [throws exception-type] )                                            │
│                                                                                  │
│  ┌───────────┬───────────────────────────────────────────────────────────────────┐│
│  │ Symbol    │ Meaning & Usage                                                   ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ *         │ IN RETURN TYPE: any return type                                   ││
│  │           │ IN CLASS NAME: any single class                                   ││
│  │           │ IN METHOD NAME: any single method (or partial: get*, *Order)      ││
│  │           │ IN PARAMS: exactly one parameter of any type                      ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ ..        │ IN PACKAGE: this package + all sub-packages recursively           ││
│  │           │ IN PARAMS: zero or more parameters of any type                    ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ ()        │ Exactly zero parameters (no arguments)                            ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (..)      │ Any number of parameters of any type (most common)                ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (*)       │ Exactly one parameter of any type                                 ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (T)       │ Exactly one parameter of type T                                   ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (T, ..)   │ First param is T, then zero or more of any type                   ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (.., T)   │ Zero or more of any type, last param is T                         ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ (T, U)    │ Exactly two params: first is T, second is U                       ││
│  ├───────────┼───────────────────────────────────────────────────────────────────┤│
│  │ &&        │ Combine pointcuts with AND                                        ││
│  │ ||        │ Combine pointcuts with OR                                         ││
│  │ !         │ Negate a pointcut (NOT)                                           ││
│  └───────────┴───────────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  REMEMBER:                                                                       │
│    • Only return-type and method-name(params) are REQUIRED                       │
│    • * matches ONE thing, .. matches ZERO OR MORE things                         │
│    • Use fully qualified names for custom types (com.app.entity.Order)           │
│    • java.lang types can be used without package (String, Integer, etc.)          │
│    • Generics are erased: List<Order> and List<String> both match "java.util.List"│
│    • Primitive ≠ Wrapper: int ≠ Integer, long ≠ Long                            │
│    • Argument ORDER matters: (String, Long) ≠ (Long, String)                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


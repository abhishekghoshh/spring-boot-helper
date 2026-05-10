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


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


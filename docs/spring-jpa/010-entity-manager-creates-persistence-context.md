### How EntityManager Creates Persistence Context Internally

```text
EntityManagerFactory.createEntityManager()
         │
         v
new SessionImpl(...)                       ← Hibernate's EntityManager implementation (simplified)
          │
          ├─ creates StatefulPersistenceContext (simplified)
          │      ├─ entitiesByKey: Map<EntityKey, Object>        ← managed entities
          │      ├─ entitySnapshotsByKey: Map<EntityKey, Object[]> ← snapshot for dirty check
          │      └─ collectionsByKey: Map<CollectionKey, PersistentCollection>
          │
          ├─ creates ActionQueue (simplified)
          │      ├─ insertions: List<AbstractEntityInsertAction>
          │      ├─ updates: List<EntityUpdateAction>
          │      ├─ deletions: List<EntityDeleteAction>
          │      └─ (flushed in order determined by ActionQueue sorting (roughly inserts→updates→deletes, plus collection/orphan actions))
         │
         └─ lazily acquires JDBC Connection from pool on first SQL
```

- The `PersistenceContext` is created **inside** the `Session` (EntityManager) at construction time.
- It is an in-memory structure — a map from entity key (type + id) to the entity object.
- **Snapshots** are taken when an entity enters the persistence context (on `persist()` or `find()`). At flush time, Hibernate compares the current field values to the snapshot to detect changes (dirty checking).
- The `ActionQueue` batches all pending database operations and executes them in the correct order during `flush()`.

---

### GOLD: SQL log example + interview Q&A

Enable SQL logging to see persistence-context flushes:

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

```sql
-- em.persist(emp) → queued, no SQL yet
-- tx commit → flush() executes:
INSERT INTO employees (name, email) VALUES ('Bob', 'bob@x.com');
```

**Interview Q&A**

1. **Q: When is the persistence context created?** A: When the `EntityManager`/`Session` is created — one context per EM, holding `entitiesByKey` + snapshots for dirty checking.
2. **Q: persist() vs flush()?** A: `persist()` only marks the entity managed + queues an insert; `flush()` executes the SQL. Commit always flushes first.
3. **Q: Why compare snapshots at flush?** A: Dirty checking — Hibernate diffs current fields against the snapshot taken at load/persist time and generates UPDATE only for changed entities.


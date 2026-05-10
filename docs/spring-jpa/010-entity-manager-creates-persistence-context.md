### How EntityManager Creates Persistence Context Internally

```text
EntityManagerFactory.createEntityManager()
         │
         v
new SessionImpl(...)                       ← Hibernate's EntityManager implementation
         │
         ├─ creates StatefulPersistenceContext
         │      ├─ entitiesByKey: Map<EntityKey, Object>        ← managed entities
         │      ├─ entitySnapshotsByKey: Map<EntityKey, Object[]> ← snapshot for dirty check
         │      └─ collectionsByKey: Map<CollectionKey, PersistentCollection>
         │
         ├─ creates ActionQueue
         │      ├─ insertions: List<AbstractEntityInsertAction>
         │      ├─ updates: List<EntityUpdateAction>
         │      ├─ deletions: List<EntityDeleteAction>
         │      └─ (flushed in order: inserts → updates → deletes)
         │
         └─ lazily acquires JDBC Connection from pool on first SQL
```

- The `PersistenceContext` is created **inside** the `Session` (EntityManager) at construction time.
- It is an in-memory structure — a map from entity key (type + id) to the entity object.
- **Snapshots** are taken when an entity enters the persistence context (on `persist()` or `find()`). At flush time, Hibernate compares the current field values to the snapshot to detect changes (dirty checking).
- The `ActionQueue` batches all pending database operations and executes them in the correct order during `flush()`.

---


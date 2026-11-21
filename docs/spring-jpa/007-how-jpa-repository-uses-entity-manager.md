### How JPA Repository Internally Uses EntityManager

```text
EmployeeRepository.save(entity)
         │
         v
SimpleJpaRepository.save(entity)        ← Spring Data JPA's default implementation
         │
         ├─ if entity.isNew():
         │      entityManager.persist(entity)     ← INSERT
         │
         └─ else:
                entityManager.merge(entity)       ← UPDATE (returns managed copy)
```

Spring Data JPA generates a proxy class for each repository interface. The actual implementation is `SimpleJpaRepository`:

```java
// Simplified version of Spring Data JPA's SimpleJpaRepository
@Repository
@Transactional(readOnly = true)
public class SimpleJpaRepository<T, ID> implements JpaRepository<T, ID> {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public <S extends T> S save(S entity) {
        if (entityInformation.isNew(entity)) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(entityManager.find(domainClass, id));
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        T entity = findById(id).orElseThrow();
        entityManager.remove(entity);
    }

    @Override
    public List<T> findAll() {
        return entityManager.createQuery("SELECT e FROM " + entityName + " e", domainClass)
                            .getResultList();
    }
}
```

Every Spring Data JPA method — `save()`, `findById()`, `delete()`, `findAll()` — calls `EntityManager` underneath.

---

### GOLD: isNew detection + readOnly hint + Q&A

How `isNew()` is decided (in order):

- `@Version` present → new if version is `null`.
- Assigned `@Id` (no generation) → new if `@Id` is `null`; a non-null Id triggers `merge()` (SELECT-then-UPDATE).
- Implement `Persistable<ID>` + override `isNew()` when Id is assigned but entity is still new — avoids the extra SELECT from `merge()`.

readOnly hint: the class-level `@Transactional(readOnly = true)` in `SimpleJpaRepository` means `findById`/`findAll` skip dirty checking; write methods re-declare `@Transactional` (read-write) to override it.

**Interview Q&A**

1. **Q: save() does INSERT or UPDATE?** A: New → `persist()` (INSERT); existing → `merge()` (SELECT + UPDATE). Assigned IDs with non-null value are treated as existing unless you implement `Persistable`.
2. **Q: Why is merge() slower than persist()?** A: `merge()` copies state onto a managed instance (extra SELECT + managed-copy return) — never use its return value interchangeably with the passed entity.


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


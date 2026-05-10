### Hierarchy of TransactionManager

Spring's transaction management is built on a well-designed **interface hierarchy** that follows the Template Method design pattern. Each level adds more specific behavior.

**Complete Hierarchy Diagram:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  TransactionManager Hierarchy:                                                   │
│                                                                                  │
│  TransactionManager (empty marker interface)                                     │
│  └── PlatformTransactionManager (3 methods: getTransaction, commit, rollback)    │
│      └── AbstractPlatformTransactionManager (template method implementations)    │
│          ├── DataSourceTransactionManager (plain JDBC / MyBatis)                 │
│          │   └── JdbcTransactionManager (Spring 5.3+ — same as parent + logging)│
│          ├── JpaTransactionManager (JPA / Hibernate via EntityManager)           │
│          ├── HibernateTransactionManager (native Hibernate SessionFactory)       │
│          └── JtaTransactionManager (distributed / XA transactions)              │
│                                                                                  │
│  Also:                                                                           │
│  TransactionManager                                                              │
│  └── ReactiveTransactionManager (for WebFlux / reactive stack)                   │
│      └── R2dbcTransactionManager                                                 │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  LOCAL Transaction Managers          │  DISTRIBUTED Transaction Manager    │  │
│  │  (single datasource/resource)        │  (multiple datasources/resources)   │  │
│  │                                      │                                     │  │
│  │  • DataSourceTransactionManager      │  • JtaTransactionManager            │  │
│  │  • JdbcTransactionManager            │    (uses XA protocol / 2PC)         │  │
│  │  • JpaTransactionManager             │                                     │  │
│  │  • HibernateTransactionManager       │                                     │  │
│  └──────────────────────────────────────┴─────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 1. TransactionManager — The Empty Marker Interface

`TransactionManager` is a **marker interface** with no methods. It exists purely as a **common type** so that both imperative (`PlatformTransactionManager`) and reactive (`ReactiveTransactionManager`) transaction managers share a common root.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: org.springframework.transaction.TransactionManager
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction;

/**
 * Marker interface for transaction manager implementations.
 * 
 * This is the SUPER-INTERFACE for:
 *   - PlatformTransactionManager (imperative / servlet stack)
 *   - ReactiveTransactionManager (reactive / WebFlux stack)
 *
 * @since 5.2
 */
public interface TransactionManager {
    // No methods — just a marker/tag interface
    // Purpose: common type for dependency injection and type checks
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why Does an Empty Interface Exist?                                              │
│                                                                                  │
│  Before Spring 5.2:                                                              │
│    PlatformTransactionManager was the root → only imperative (servlet) stack     │
│                                                                                  │
│  After Spring 5.2 (WebFlux introduced):                                          │
│    TransactionManager                                                            │
│    ├── PlatformTransactionManager    ← imperative (Servlet, MVC)                │
│    └── ReactiveTransactionManager    ← reactive (WebFlux, R2DBC)                │
│                                                                                  │
│  Having a common root allows:                                                    │
│    1. @Transactional to work with BOTH imperative and reactive managers          │
│    2. TransactionInterceptor to accept either type                               │
│    3. Spring to auto-detect any transaction manager bean in the context          │
│                                                                                  │
│  Usage:                                                                          │
│    @Bean                                                                         │
│    TransactionManager txManager() {  // ← Can return either type                │
│        return new JpaTransactionManager(emf);       // imperative                │
│        // return new R2dbcTransactionManager(cf);   // reactive                  │
│    }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 2. PlatformTransactionManager — The Core Contract (3 Methods)

`PlatformTransactionManager` defines the **3 fundamental operations** of transaction management. Every imperative (non-reactive) transaction manager must implement these.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: org.springframework.transaction.PlatformTransactionManager
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction;

public interface PlatformTransactionManager extends TransactionManager {

    /**
     * 1. BEGIN or JOIN a transaction based on propagation behavior.
     *
     * If no transaction exists → create a NEW one
     * If transaction already exists → join/suspend/nest based on propagation
     *
     * @param definition - contains isolation, propagation, timeout, readOnly
     *                     (parsed from @Transactional attributes)
     * @return TransactionStatus - handle to the current transaction
     *         (used later for commit/rollback)
     */
    TransactionStatus getTransaction(TransactionDefinition definition)
            throws TransactionException;

    /**
     * 2. COMMIT the transaction.
     *
     * Flushes all pending changes to the database and makes them permanent.
     * After this call, other transactions can see the changes.
     *
     * @param status - the handle returned by getTransaction()
     */
    void commit(TransactionStatus status) throws TransactionException;

    /**
     * 3. ROLLBACK the transaction.
     *
     * Undoes ALL changes made since getTransaction() was called.
     * Database returns to the state before the transaction began.
     *
     * @param status - the handle returned by getTransaction()
     */
    void rollback(TransactionStatus status) throws TransactionException;
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  PlatformTransactionManager — 3 Methods Lifecycle:                               │
│                                                                                  │
│  @Transactional method called                                                    │
│       │                                                                          │
│       v                                                                          │
│  ┌─────────────────────────────────────────────────┐                             │
│  │ 1. getTransaction(TransactionDefinition)        │                             │
│  │    ├── Reads: isolation, propagation, readOnly  │                             │
│  │    ├── Gets JDBC Connection from DataSource     │                             │
│  │    ├── connection.setAutoCommit(false)           │                             │
│  │    └── Returns: TransactionStatus               │                             │
│  │         (holds reference to connection & state)  │                             │
│  └──────────────────────┬──────────────────────────┘                             │
│                         │                                                        │
│                         v                                                        │
│            ┌────────────────────────┐                                            │
│            │  Your Business Logic   │                                            │
│            │  runs here             │                                            │
│            │  (SQL queries execute) │                                            │
│            └────────────┬───────────┘                                            │
│                         │                                                        │
│                    ┌────┴────┐                                                   │
│                    │ Success?│                                                   │
│                    └────┬────┘                                                   │
│               YES /          \ NO (exception)                                    │
│                 /              \                                                  │
│  ┌─────────────────────┐  ┌──────────────────────┐                               │
│  │ 2. commit(status)   │  │ 3. rollback(status)  │                               │
│  │    connection.commit│  │    connection.rollback│                               │
│  │    release conn     │  │    release conn       │                               │
│  └─────────────────────┘  └──────────────────────┘                               │
│                                                                                  │
│  TransactionDefinition (input) contains:                                         │
│    • propagation: REQUIRED, REQUIRES_NEW, NESTED, etc.                           │
│    • isolation: READ_COMMITTED, REPEATABLE_READ, etc.                            │
│    • timeout: max seconds before auto-rollback                                   │
│    • readOnly: optimization hint (skip dirty checking)                           │
│    • name: transaction name (for monitoring)                                     │
│                                                                                  │
│  TransactionStatus (output) contains:                                            │
│    • isNewTransaction(): was a new TX created or did we join existing?            │
│    • hasSavepoint(): does this TX have a savepoint (for NESTED)?                 │
│    • setRollbackOnly(): mark TX for rollback (even if no exception)              │
│    • isRollbackOnly(): has TX been marked for rollback?                           │
│    • isCompleted(): has commit/rollback already been called?                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 3. AbstractPlatformTransactionManager — Template Method Pattern

`AbstractPlatformTransactionManager` is an **abstract class** that provides the **default implementation** of `getTransaction()`, `commit()`, and `rollback()` using the **Template Method design pattern**. It handles all the common logic (propagation behavior, savepoints, status tracking) and delegates resource-specific operations to subclasses via **abstract hook methods**.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: AbstractPlatformTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction.support;

public abstract class AbstractPlatformTransactionManager
        implements PlatformTransactionManager, Serializable {

    // ─── getTransaction() — handles propagation logic ───
    @Override
    public final TransactionStatus getTransaction(TransactionDefinition definition)
            throws TransactionException {

        TransactionDefinition def = (definition != null ?
                definition : TransactionDefinition.withDefaults());

        // STEP 1: Ask subclass for current transaction object (if any)
        Object transaction = doGetTransaction();
        // ↑ ABSTRACT — subclass returns its resource holder
        //   e.g., DataSourceTransactionManager returns ConnectionHolder

        // STEP 2: Check if transaction already exists
        if (isExistingTransaction(transaction)) {
            // Handle propagation for EXISTING transaction:
            //   REQUIRED → join existing
            //   REQUIRES_NEW → suspend existing, create new
            //   NESTED → create savepoint
            //   NOT_SUPPORTED → suspend existing, run non-transactional
            //   NEVER → throw exception
            return handleExistingTransaction(def, transaction, debugEnabled);
        }

        // STEP 3: No existing transaction — check propagation
        if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
            throw new IllegalTransactionStateException(
                    "No existing transaction found for MANDATORY propagation");
        }

        if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {

            // STEP 4: Suspend any synchronization (no actual TX to suspend)
            SuspendedResourcesHolder suspendedResources = suspend(null);

            try {
                // STEP 5: Ask subclass to BEGIN the actual transaction
                doBegin(transaction, def);
                // ↑ ABSTRACT — subclass opens connection, sets autoCommit=false

                // STEP 6: Register synchronization
                prepareSynchronization(status, def);

                return status;
            }
            catch (RuntimeException | Error ex) {
                resume(null, suspendedResources);
                throw ex;
            }
        }

        // PROPAGATION_SUPPORTS, NOT_SUPPORTED, NEVER → no transaction
        return prepareTransactionStatus(def, null, true, ...);
    }

    // ─── commit() — handles commit with status checks ───
    @Override
    public final void commit(TransactionStatus status) throws TransactionException {

        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction already completed");
        }

        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;

        // Check if someone called setRollbackOnly()
        if (defStatus.isLocalRollbackOnly()) {
            processRollback(defStatus, false);
            return;
        }

        // Check global rollback-only (set by inner transaction participation)
        if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
            processRollback(defStatus, true);
            return;
        }

        // All checks passed → actually commit
        processCommit(defStatus);
    }

    private void processCommit(DefaultTransactionStatus status) throws TransactionException {
        try {
            if (status.hasSavepoint()) {
                // NESTED → release savepoint
                status.releaseHeldSavepoint();
            }
            else if (status.isNewTransaction()) {
                // REQUIRED / REQUIRES_NEW → actual commit
                doCommit(status);
                // ↑ ABSTRACT — subclass calls connection.commit()
            }
            // else: participating in existing TX → don't commit yet
        }
        finally {
            cleanupAfterCompletion(status);
        }
    }

    // ─── rollback() — handles rollback ───
    @Override
    public final void rollback(TransactionStatus status) throws TransactionException {
        if (status.isCompleted()) {
            throw new IllegalTransactionStateException("Transaction already completed");
        }
        processRollback((DefaultTransactionStatus) status, false);
    }

    private void processRollback(DefaultTransactionStatus status, boolean unexpected) {
        try {
            if (status.hasSavepoint()) {
                // NESTED → rollback to savepoint
                status.rollbackToHeldSavepoint();
            }
            else if (status.isNewTransaction()) {
                // We own the transaction → do actual rollback
                doRollback(status);
                // ↑ ABSTRACT — subclass calls connection.rollback()
            }
            else {
                // Participating in larger TX → mark rollback-only
                if (status.hasTransaction()) {
                    doSetRollbackOnly(status);
                }
            }
        }
        finally {
            cleanupAfterCompletion(status);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ABSTRACT HOOK METHODS — Subclasses MUST implement these:
    // ═══════════════════════════════════════════════════════════════════════

    /** Return current transaction object (e.g., ConnectionHolder) */
    protected abstract Object doGetTransaction();

    /** Begin a new transaction with the given definition */
    protected abstract void doBegin(Object transaction, TransactionDefinition definition);

    /** Perform actual commit on the underlying resource */
    protected abstract void doCommit(DefaultTransactionStatus status);

    /** Perform actual rollback on the underlying resource */
    protected abstract void doRollback(DefaultTransactionStatus status);

    /** Check if the transaction object represents an existing transaction */
    protected boolean isExistingTransaction(Object transaction) {
        return false; // subclasses override
    }

    /** Mark the transaction as rollback-only */
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        throw new IllegalTransactionStateException("Not supported");
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Template Method Pattern in AbstractPlatformTransactionManager:                  │
│                                                                                  │
│  AbstractPlatformTransactionManager                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐      │
│  │                                                                         │      │
│  │  getTransaction() ──── FINAL (cannot override)                          │      │
│  │  ├── handles propagation logic (REQUIRED, REQUIRES_NEW, etc.)           │      │
│  │  ├── calls doGetTransaction()     ← ABSTRACT (subclass provides)        │      │
│  │  ├── calls isExistingTransaction()← OVERRIDABLE                         │      │
│  │  └── calls doBegin()              ← ABSTRACT (subclass provides)        │      │
│  │                                                                         │      │
│  │  commit() ────────── FINAL                                              │      │
│  │  ├── checks rollbackOnly flags                                          │      │
│  │  ├── handles savepoints                                                 │      │
│  │  └── calls doCommit()             ← ABSTRACT (subclass provides)        │      │
│  │                                                                         │      │
│  │  rollback() ─────── FINAL                                               │      │
│  │  ├── handles savepoints                                                 │      │
│  │  ├── handles participant vs owner                                       │      │
│  │  └── calls doRollback()           ← ABSTRACT (subclass provides)        │      │
│  │                                                                         │      │
│  └─────────────────────────────────────────────────────────────────────────┘      │
│                                                                                  │
│  Subclasses only implement the resource-specific parts:                          │
│                                                                                  │
│  DataSourceTransactionManager:                                                   │
│    doGetTransaction() → return ConnectionHolder from ThreadLocal                 │
│    doBegin()          → dataSource.getConnection(); conn.setAutoCommit(false)    │
│    doCommit()         → connection.commit()                                      │
│    doRollback()       → connection.rollback()                                    │
│                                                                                  │
│  JpaTransactionManager:                                                          │
│    doGetTransaction() → return EntityManager + ConnectionHolder                  │
│    doBegin()          → emf.createEntityManager(); get JDBC connection           │
│    doCommit()         → entityManager.getTransaction().commit()                  │
│    doRollback()       → entityManager.getTransaction().rollback()                │
│                                                                                  │
│  The COMMON logic (propagation, savepoints, status tracking) is written          │
│  ONCE in AbstractPlatformTransactionManager. Subclasses only handle              │
│  the database-specific connection/commit/rollback operations.                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 4. Concrete Implementations of AbstractPlatformTransactionManager

##### 4a. DataSourceTransactionManager (Plain JDBC / MyBatis)

Manages transactions for a single **JDBC DataSource**. Works directly with `java.sql.Connection`. Used when you use plain JDBC, Spring `JdbcTemplate`, or MyBatis.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: DataSourceTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.jdbc.datasource;

public class DataSourceTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager, InitializingBean {

    private DataSource dataSource;

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected Object doGetTransaction() {
        DataSourceTransactionObject txObject = new DataSourceTransactionObject();
        // Check if there's already a connection bound to current thread
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager
                .getResource(this.dataSource);
        txObject.setConnectionHolder(conHolder, false);
        return txObject;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

        Connection con = null;
        try {
            // Get a new connection from the DataSource (connection pool)
            if (!txObject.hasConnectionHolder()) {
                Connection newCon = obtainDataSource().getConnection();
                txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
            }

            con = txObject.getConnectionHolder().getConnection();

            // Apply isolation level
            Integer previousIsolationLevel = DataSourceUtils
                    .prepareConnectionForTransaction(con, definition);
            // ↑ Internally calls: con.setTransactionIsolation(isolationLevel)

            txObject.setPreviousIsolationLevel(previousIsolationLevel);
            txObject.setReadOnly(definition.isReadOnly());

            // Switch to manual commit — THIS IS WHERE THE TRANSACTION BEGINS
            if (con.getAutoCommit()) {
                txObject.setMustRestoreAutoCommit(true);
                con.setAutoCommit(false);
                // ↑ Equivalent to: START TRANSACTION
            }

            prepareTransactionalConnection(con, definition);
            // ↑ Sets connection to read-only if @Transactional(readOnly=true)

            // Bind connection to current thread (ThreadLocal)
            if (txObject.isNewConnectionHolder()) {
                TransactionSynchronizationManager
                        .bindResource(obtainDataSource(), txObject.getConnectionHolder());
            }
        }
        catch (Throwable ex) {
            if (txObject.isNewConnectionHolder()) {
                DataSourceUtils.releaseConnection(con, obtainDataSource());
            }
            throw new CannotCreateTransactionException("Could not open connection", ex);
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        DataSourceTransactionObject txObject =
                (DataSourceTransactionObject) status.getTransaction();
        Connection con = txObject.getConnectionHolder().getConnection();
        try {
            con.commit();   // ← ACTUAL SQL COMMIT
        }
        catch (SQLException ex) {
            throw translateException("JDBC commit", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        DataSourceTransactionObject txObject =
                (DataSourceTransactionObject) status.getTransaction();
        Connection con = txObject.getConnectionHolder().getConnection();
        try {
            con.rollback();  // ← ACTUAL SQL ROLLBACK
        }
        catch (SQLException ex) {
            throw translateException("JDBC rollback", ex);
        }
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        // Unbind connection from ThreadLocal
        TransactionSynchronizationManager.unbindResource(obtainDataSource());
        // Restore auto-commit
        Connection con = txObject.getConnectionHolder().getConnection();
        if (txObject.isMustRestoreAutoCommit()) {
            con.setAutoCommit(true);
        }
        // Release connection back to pool
        DataSourceUtils.releaseConnection(con, obtainDataSource());
    }
}
```

##### 4b. JdbcTransactionManager (Spring 5.3+)

`JdbcTransactionManager` extends `DataSourceTransactionManager` — it's essentially the same but adds **better exception translation** (converts JDBC `SQLException` to Spring's `DataAccessException` hierarchy using `SQLExceptionTranslator`).

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: JdbcTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.jdbc.support;

/**
 * @since 5.3
 * Recommended replacement for DataSourceTransactionManager.
 * Adds proper JDBC 4 exception translation.
 */
public class JdbcTransactionManager extends DataSourceTransactionManager {

    public JdbcTransactionManager(DataSource dataSource) {
        super(dataSource);
    }

    // Overrides exception translation to use SQLExceptionTranslator
    // instead of generic TransactionSystemException
    @Override
    protected RuntimeException translateException(String task, SQLException ex) {
        // Uses SQLErrorCodeSQLExceptionTranslator to provide
        // database-specific exceptions like:
        //   DuplicateKeyException, DataIntegrityViolationException, etc.
        return getExceptionTranslator().translate(task, null, ex);
    }
}
```

##### 4c. JpaTransactionManager (JPA / Hibernate via EntityManager)

Manages transactions through **JPA's `EntityManager`** and `EntityManagerFactory`. This is the **most commonly used** transaction manager in Spring Boot applications with `spring-boot-starter-data-jpa`. It manages both the JPA `EntityManager` lifecycle AND the underlying JDBC connection.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: JpaTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.orm.jpa;

public class JpaTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

    private EntityManagerFactory entityManagerFactory;
    private DataSource dataSource;   // extracted from EMF for JDBC-level binding

    @Override
    protected Object doGetTransaction() {
        JpaTransactionObject txObject = new JpaTransactionObject();

        // Check for existing EntityManager bound to thread
        EntityManagerHolder emHolder = (EntityManagerHolder)
                TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
        if (emHolder != null) {
            txObject.setEntityManagerHolder(emHolder, false);
        }

        // Also check for existing JDBC connection
        if (getDataSource() != null) {
            ConnectionHolder conHolder = (ConnectionHolder)
                    TransactionSynchronizationManager.getResource(getDataSource());
            txObject.setConnectionHolder(conHolder);
        }

        return txObject;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        JpaTransactionObject txObject = (JpaTransactionObject) transaction;

        try {
            // STEP 1: Create a new EntityManager if needed
            if (!txObject.hasEntityManagerHolder()) {
                EntityManager em = createEntityManagerForTransaction();
                // ↑ Internally: entityManagerFactory.createEntityManager()
                txObject.setEntityManagerHolder(
                        new EntityManagerHolder(em), true);
            }

            EntityManager em = txObject.getEntityManagerHolder().getEntityManager();

            // STEP 2: Begin JPA transaction
            EntityTransaction etx = em.getTransaction();
            etx.begin();
            // ↑ Internally: Hibernate calls connection.setAutoCommit(false)

            // STEP 3: Get the underlying JDBC connection for isolation/readOnly
            // Hibernate exposes it via Session.doWork()
            Object rawConnection = em.unwrap(java.sql.Connection.class);
            // Set isolation level, readOnly etc. on the JDBC connection

            // STEP 4: Bind EntityManager to current thread
            if (txObject.isNewEntityManagerHolder()) {
                TransactionSynchronizationManager.bindResource(
                        obtainEntityManagerFactory(),
                        txObject.getEntityManagerHolder());
            }

            // STEP 5: Also bind JDBC connection to thread
            // (so JdbcTemplate can participate in the same transaction)
            if (getDataSource() != null) {
                ConnectionHolder conHolder = new ConnectionHolder(
                        (Connection) rawConnection);
                TransactionSynchronizationManager.bindResource(
                        getDataSource(), conHolder);
                txObject.setConnectionHolder(conHolder);
            }
        }
        catch (TransactionException ex) {
            closeEntityManagerAfterFailedBegin(txObject);
            throw ex;
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        JpaTransactionObject txObject =
                (JpaTransactionObject) status.getTransaction();
        EntityTransaction etx =
                txObject.getEntityManagerHolder().getEntityManager().getTransaction();
        try {
            etx.commit();
            // ↑ Hibernate internally:
            //   1. session.flush() → generates and executes pending SQL
            //   2. connection.commit() → commits to database
        }
        catch (RollbackException ex) {
            throw new UnexpectedRollbackException("JPA commit failed", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        JpaTransactionObject txObject =
                (JpaTransactionObject) status.getTransaction();
        EntityTransaction etx =
                txObject.getEntityManagerHolder().getEntityManager().getTransaction();
        try {
            etx.rollback();
            // ↑ Hibernate internally:
            //   1. session.clear() → discards all pending changes
            //   2. connection.rollback() → rollbacks to database
        }
        catch (PersistenceException ex) {
            throw new TransactionSystemException("JPA rollback failed", ex);
        }
    }
}
```

##### 4d. HibernateTransactionManager (Native Hibernate Session)

Manages transactions through **Hibernate's native `SessionFactory`** (not JPA `EntityManagerFactory`). Used in legacy applications or when you need Hibernate-specific features not available through JPA.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: HibernateTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.orm.hibernate5;

public class HibernateTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

    private SessionFactory sessionFactory;

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;

        Session session = obtainSessionFactory().openSession();
        // ↑ Opens a Hibernate Session (wraps a JDBC Connection)

        txObject.setSession(session);

        session.beginTransaction();
        // ↑ Internally: connection.setAutoCommit(false)

        // Bind session to thread
        TransactionSynchronizationManager.bindResource(
                obtainSessionFactory(), new SessionHolder(session));
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        HibernateTransactionObject txObject =
                (HibernateTransactionObject) status.getTransaction();
        Transaction hibernateTx = txObject.getSession().getTransaction();

        hibernateTx.commit();
        // ↑ Internally: session.flush() → connection.commit()
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        HibernateTransactionObject txObject =
                (HibernateTransactionObject) status.getTransaction();
        Transaction hibernateTx = txObject.getSession().getTransaction();

        hibernateTx.rollback();
        // ↑ Internally: session.clear() → connection.rollback()
    }
}
```

##### 4e. JtaTransactionManager (Distributed / XA Transactions)

Manages **distributed transactions** across multiple resources (databases, message queues, etc.) using the **JTA (Java Transaction API)** and **XA protocol with Two-Phase Commit (2PC)**.

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: JtaTransactionManager (simplified)
// ═══════════════════════════════════════════════════════════════════════════════

package org.springframework.transaction.jta;

public class JtaTransactionManager extends AbstractPlatformTransactionManager
        implements TransactionFactory, InitializingBean, Serializable {

    // JTA interfaces — provided by app server or standalone JTA impl (Atomikos, Narayana)
    private transient UserTransaction userTransaction;
    private transient TransactionManager transactionManager;

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        JtaTransactionObject txObject = (JtaTransactionObject) transaction;
        try {
            // Set timeout if specified
            if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
                userTransaction.setTransactionTimeout(definition.getTimeout());
            }

            // Begin distributed transaction
            userTransaction.begin();
            // ↑ JTA coordinates ALL enlisted resources (databases, JMS, etc.)
            //   Each resource gets an XA transaction branch
        }
        catch (NotSupportedException | SystemException ex) {
            throw new CannotCreateTransactionException("JTA begin failed", ex);
        }
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        try {
            userTransaction.commit();
            // ↑ TWO-PHASE COMMIT:
            //   Phase 1 (PREPARE): Ask ALL resources — "Can you commit?"
            //     DB1: "Yes, prepared"
            //     DB2: "Yes, prepared"
            //     JMS: "Yes, prepared"
            //   Phase 2 (COMMIT): Tell ALL resources — "Commit now"
            //     DB1: COMMIT
            //     DB2: COMMIT
            //     JMS: COMMIT
        }
        catch (RollbackException | HeuristicMixedException |
               HeuristicRollbackException | SystemException ex) {
            throw new TransactionSystemException("JTA commit failed", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        try {
            userTransaction.rollback();
            // ↑ ALL enlisted resources roll back
        }
        catch (SystemException ex) {
            throw new TransactionSystemException("JTA rollback failed", ex);
        }
    }
}
```

**Comparison of All Implementations:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Comparison of TransactionManager Implementations:                               │
│                                                                                  │
│  ┌──────────────────────┬────────────────┬──────────────────┬───────────────┐     │
│  │ TransactionManager   │ Manages        │ Used With        │ Auto-Config   │     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ DataSource            │ JDBC           │ JdbcTemplate,    │ starter-jdbc  │     │
│  │ TransactionManager    │ Connection     │ MyBatis          │               │     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ Jdbc                  │ JDBC           │ Same as above +  │ starter-jdbc  │     │
│  │ TransactionManager    │ Connection     │ better exceptions│ (Spring 5.3+)│     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ Jpa                   │ EntityManager  │ Spring Data JPA, │ starter-      │     │
│  │ TransactionManager    │ + Connection   │ Hibernate (JPA)  │ data-jpa     │     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ Hibernate              │ Hibernate      │ Native Hibernate │ Manual        │     │
│  │ TransactionManager    │ Session        │ SessionFactory   │ config        │     │
│  ├──────────────────────┼────────────────┼──────────────────┼───────────────┤     │
│  │ Jta                   │ JTA            │ Multiple DBs,    │ starter-jta-  │     │
│  │ TransactionManager    │ UserTransaction│ DB + JMS, XA     │ atomikos      │     │
│  └──────────────────────┴────────────────┴──────────────────┴───────────────┘     │
│                                                                                  │
│  Resource Flow:                                                                  │
│                                                                                  │
│  DataSourceTxManager:  TxManager → DataSource → Connection → SQL                │
│  JpaTxManager:         TxManager → EMF → EntityManager → Session → Conn → SQL   │
│  HibernateTxManager:   TxManager → SessionFactory → Session → Conn → SQL        │
│  JtaTxManager:         TxManager → UserTransaction → XA Resources → SQL/JMS     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5. Local vs Distributed Transaction Manager

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  LOCAL Transaction Manager:                                                      │
│                                                                                  │
│  Manages transactions for a SINGLE resource (one database, one connection).      │
│  Uses native resource-level transaction APIs (JDBC commit/rollback).             │
│                                                                                  │
│  ┌────────────────┐      ┌──────────────┐      ┌──────────────┐                  │
│  │ Application     │─────→│ Local         │─────→│ Single       │                  │
│  │ @Transactional  │      │ TxManager     │      │ Database     │                  │
│  └────────────────┘      │ (JPA/JDBC)    │      │ (MySQL)      │                  │
│                          └──────────────┘      └──────────────┘                  │
│                                                                                  │
│  Examples:                                                                       │
│  • DataSourceTransactionManager  → 1 DataSource → 1 DB                          │
│  • JdbcTransactionManager        → 1 DataSource → 1 DB                          │
│  • JpaTransactionManager         → 1 EntityManagerFactory → 1 DB                │
│  • HibernateTransactionManager   → 1 SessionFactory → 1 DB                      │
│                                                                                  │
│  Pros: Simple, fast, no overhead                                                 │
│  Cons: Cannot span multiple databases or message queues                          │
│                                                                                  │
│──────────────────────────────────────────────────────────────────────────────────│
│                                                                                  │
│  DISTRIBUTED Transaction Manager (JTA):                                          │
│                                                                                  │
│  Manages transactions across MULTIPLE resources using Two-Phase Commit (2PC).    │
│  Coordinates XA-capable resources via JTA UserTransaction.                       │
│                                                                                  │
│  ┌────────────────┐      ┌──────────────┐      ┌──────────────┐                  │
│  │ Application     │─────→│ JTA           │──┬──→│ Database 1   │                  │
│  │ @Transactional  │      │ TxManager     │  │   │ (MySQL)      │                  │
│  └────────────────┘      │ (Atomikos/    │  │   └──────────────┘                  │
│                          │  Narayana)    │  │                                    │
│                          └──────────────┘  │   ┌──────────────┐                  │
│                                             ├──→│ Database 2   │                  │
│                                             │   │ (PostgreSQL) │                  │
│                                             │   └──────────────┘                  │
│                                             │                                    │
│                                             │   ┌──────────────┐                  │
│                                             └──→│ Message Queue│                  │
│                                                  │ (ActiveMQ)   │                  │
│                                                  └──────────────┘                  │
│                                                                                  │
│  Two-Phase Commit (2PC) Protocol:                                                │
│                                                                                  │
│  ┌─────────────┐                                                                 │
│  │ TxManager    │                                                                │
│  │ (coordinator)│                                                                │
│  └──────┬──────┘                                                                 │
│         │                                                                        │
│    PHASE 1: PREPARE                                                              │
│         │──── "Can you commit?" ───→  DB1: ✅ PREPARED                           │
│         │──── "Can you commit?" ───→  DB2: ✅ PREPARED                           │
│         │──── "Can you commit?" ───→  JMS: ✅ PREPARED                           │
│         │                                                                        │
│         │  ALL said YES?                                                         │
│         │                                                                        │
│    PHASE 2: COMMIT                                                               │
│         │──── "COMMIT now" ────────→  DB1: COMMITTED ✅                          │
│         │──── "COMMIT now" ────────→  DB2: COMMITTED ✅                          │
│         │──── "COMMIT now" ────────→  JMS: COMMITTED ✅                          │
│         │                                                                        │
│    If ANY resource said NO in Phase 1:                                           │
│    PHASE 2: ROLLBACK                                                             │
│         │──── "ROLLBACK" ──────────→  DB1: ROLLED BACK                           │
│         │──── "ROLLBACK" ──────────→  DB2: ROLLED BACK                           │
│         │──── "ROLLBACK" ──────────→  JMS: ROLLED BACK                           │
│                                                                                  │
│  Examples:                                                                       │
│  • JtaTransactionManager (Spring) + Atomikos / Narayana / Bitronix              │
│  • App server managed: WebLogic, WildFly, WebSphere                              │
│                                                                                  │
│  Pros: ACID across multiple resources                                            │
│  Cons: Complex, slower (2PC overhead), requires XA-capable drivers               │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │ WHEN TO USE:                                                               │  │
│  │                                                                            │  │
│  │ • Single DB → use LOCAL (JpaTransactionManager)                            │  │
│  │ • Multiple DBs in one transaction → use JTA (distributed)                  │  │
│  │ • DB + JMS in one transaction → use JTA (distributed)                      │  │
│  │                                                                            │  │
│  │ Modern alternative to JTA:                                                 │  │
│  │ • Saga Pattern (eventual consistency, no 2PC overhead)                     │  │
│  │ • Outbox Pattern (reliable event publishing)                               │  │
│  │ • These are preferred in microservices architectures                       │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 6. Configuring and Using Specific Transaction Managers

When you have **multiple data sources** or need different transaction managers for different operations, you can define named transaction managers and reference them from `@Transactional`.

**Scenario: Application with MySQL (primary) + PostgreSQL (analytics) + MongoDB:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// STEP 1: Configuration — Define multiple TransactionManagers
// ═══════════════════════════════════════════════════════════════════════════════

@Configuration
public class DataSourceConfig {

    // ─── Primary DataSource (MySQL) ───
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    // ─── Analytics DataSource (PostgreSQL) ───
    @Bean
    @ConfigurationProperties("spring.datasource.analytics")
    public DataSource analyticsDataSource() {
        return DataSourceBuilder.create().build();
    }
}

@Configuration
public class TransactionManagerConfig {

    // ─── Primary JPA TransactionManager (MySQL) ───
    @Bean
    @Primary   // ← Default when @Transactional doesn't specify a manager
    public PlatformTransactionManager transactionManager(
            EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // ─── Analytics JDBC TransactionManager (PostgreSQL) ───
    @Bean("analyticsTransactionManager")   // ← Named bean
    public PlatformTransactionManager analyticsTransactionManager(
            @Qualifier("analyticsDataSource") DataSource analyticsDataSource) {
        return new DataSourceTransactionManager(analyticsDataSource);
    }

    // ─── MongoDB TransactionManager ───
    @Bean("mongoTransactionManager")   // ← Named bean
    public PlatformTransactionManager mongoTransactionManager(
            MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTransactionManager(mongoDatabaseFactory);
    }
}
```

```yaml
# application.yml — Multiple DataSources
spring:
  datasource:
    primary:
      url: jdbc:mysql://localhost:3306/orders_db
      username: root
      password: secret
      driver-class-name: com.mysql.cj.jdbc.Driver
    analytics:
      url: jdbc:postgresql://localhost:5432/analytics_db
      username: postgres
      password: secret
      driver-class-name: org.postgresql.Driver
  data:
    mongodb:
      uri: mongodb://localhost:27017/audit_db?replicaSet=rs0
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
// STEP 2: Service — Reference specific TransactionManager in @Transactional
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private AnalyticsJdbcTemplate analyticsJdbcTemplate;
    @Autowired private AuditMongoRepository auditMongoRepository;

    // ─── Uses @Primary (default) TransactionManager → JpaTransactionManager (MySQL)
    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order(request);
        return orderRepository.save(order);
        // SQL runs against MySQL via JPA EntityManager
    }

    // ─── Uses named TransactionManager → analyticsTransactionManager (PostgreSQL)
    @Transactional(transactionManager = "analyticsTransactionManager")
    public void recordAnalytics(AnalyticsEvent event) {
        analyticsJdbcTemplate.update(
                "INSERT INTO events (type, data, created_at) VALUES (?, ?, ?)",
                event.getType(), event.getData(), LocalDateTime.now()
        );
        // SQL runs against PostgreSQL via JDBC DataSource
    }

    // ─── Uses named TransactionManager → mongoTransactionManager (MongoDB)
    @Transactional("mongoTransactionManager")   // shorthand (value = transactionManager)
    public void createAuditLog(AuditEntry entry) {
        auditMongoRepository.save(entry);
        // Operations run against MongoDB replica set
    }

    // ─── Using BOTH primary + analytics in one method ───
    // ⚠️ This does NOT create a distributed transaction!
    // Each @Transactional only manages ONE resource.
    // For true distributed transactions, use JtaTransactionManager.
    @Transactional  // manages MySQL
    public Order createOrderWithAnalytics(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));

        // Analytics runs in a SEPARATE transaction (different DB)
        analyticsService.recordAnalytics(new AnalyticsEvent("ORDER_CREATED", order.getId()));

        return order;
    }
}
```

```java
// ═══════════════════════════════════════════════════════════════════════════════
// ALTERNATIVE: Using @Qualifier to inject specific TransactionManager
// (useful in programmatic transaction management)
// ═══════════════════════════════════════════════════════════════════════════════

@Service
public class ReportService {

    private final TransactionTemplate primaryTxTemplate;
    private final TransactionTemplate analyticsTxTemplate;

    public ReportService(
            PlatformTransactionManager transactionManager,   // @Primary → JPA
            @Qualifier("analyticsTransactionManager")
            PlatformTransactionManager analyticsTxManager) {

        this.primaryTxTemplate = new TransactionTemplate(transactionManager);
        this.analyticsTxTemplate = new TransactionTemplate(analyticsTxManager);
    }

    // Programmatic transaction management with specific managers:
    public void generateReport(Long orderId) {
        // Read from MySQL (primary)
        Order order = primaryTxTemplate.execute(status -> {
            return orderRepository.findById(orderId).orElseThrow();
        });

        // Write to PostgreSQL (analytics)
        analyticsTxTemplate.execute(status -> {
            analyticsJdbcTemplate.update(
                    "INSERT INTO reports (order_id, generated_at) VALUES (?, ?)",
                    orderId, LocalDateTime.now());
            return null;
        });
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  How @Transactional Resolves the TransactionManager:                             │
│                                                                                  │
│  @Transactional                                                                  │
│  public void someMethod() { ... }                                                │
│       │                                                                          │
│       v                                                                          │
│  TransactionInterceptor.determineTransactionManager(txAttr)                      │
│       │                                                                          │
│       ├── Is transactionManager specified in @Transactional?                     │
│       │   │                                                                      │
│       │   ├── YES: @Transactional("analyticsTransactionManager")                 │
│       │   │        → Look up bean by name: "analyticsTransactionManager"         │
│       │   │        → Return DataSourceTransactionManager (PostgreSQL)            │
│       │   │                                                                      │
│       │   └── NO:  @Transactional (no name specified)                            │
│       │            → Look up @Primary PlatformTransactionManager bean            │
│       │            → Return JpaTransactionManager (MySQL)                        │
│       │                                                                          │
│       v                                                                          │
│  TransactionManager resolved → used for getTransaction/commit/rollback           │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  SUMMARY:                                                                  │  │
│  │                                                                            │  │
│  │  @Transactional                          → uses @Primary TxManager         │  │
│  │  @Transactional("myTxManager")           → uses bean named "myTxManager"   │  │
│  │  @Transactional(transactionManager =     │                                 │  │
│  │       "analyticsTransactionManager")     → uses bean named "analytics..."  │  │
│  │                                                                            │  │
│  │  Spring Boot auto-configures:                                              │  │
│  │    • JpaTransactionManager    if spring-boot-starter-data-jpa present      │  │
│  │    • DataSourceTransactionManager if only spring-boot-starter-jdbc present │  │
│  │    • MongoTransactionManager  MUST be configured manually                  │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


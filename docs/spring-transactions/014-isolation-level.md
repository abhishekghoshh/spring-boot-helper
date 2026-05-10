### What is Isolation Level?

Isolation Level controls **how the changes made by one transaction are visible to other transactions running concurrently**. It answers the question: "If Transaction A modifies a row, can Transaction B see that modification before A commits?"

In ACID, the **I (Isolation)** defines the degree to which one transaction must be isolated from the effects of other concurrent transactions. Higher isolation = more safety but less concurrency. Lower isolation = more concurrency but more anomalies.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Isolation Level — The Core Problem:                                             │
│                                                                                  │
│  Thread-1 (TX-A)                     Thread-2 (TX-B)                             │
│  ┌────────────────────────┐          ┌────────────────────────┐                  │
│  │ BEGIN;                  │          │ BEGIN;                  │                  │
│  │                         │          │                         │                  │
│  │ UPDATE accounts         │          │ SELECT balance          │                  │
│  │ SET balance = 500       │          │ FROM accounts           │                  │
│  │ WHERE id = 1;           │          │ WHERE id = 1;           │                  │
│  │                         │          │                         │                  │
│  │ -- balance was 1000     │          │ -- What does TX-B see?  │                  │
│  │ -- now changed to 500   │          │ -- 1000 (old value)?    │                  │
│  │ -- but NOT committed    │          │ -- 500 (uncommitted)?   │                  │
│  │                         │          │                         │                  │
│  │ COMMIT;  (or ROLLBACK?) │          │ COMMIT;                 │                  │
│  └────────────────────────┘          └────────────────────────┘                  │
│                                                                                  │
│  The ISOLATION LEVEL answers: "What does TX-B see?"                              │
│                                                                                  │
│  READ_UNCOMMITTED → TX-B sees 500 (uncommitted!) — DANGEROUS                    │
│  READ_COMMITTED   → TX-B sees 1000 (only committed data) — SAFE                 │
│  REPEATABLE_READ  → TX-B sees 1000 (and will ALWAYS see 1000) — SAFER           │
│  SERIALIZABLE     → TX-B WAITS until TX-A commits/rolls back — SAFEST           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Internal Code — How Spring Sets Isolation Level:**

```java
// ═══════════════════════════════════════════════════════════════════════════════
// Spring Internal: DataSourceTransactionManager.doBegin() — sets isolation
// (simplified from Spring source)
// ═══════════════════════════════════════════════════════════════════════════════

@Override
protected void doBegin(Object transaction, TransactionDefinition definition) {

    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

    Connection con = obtainDataSource().getConnection();
    txObject.setConnectionHolder(new ConnectionHolder(con));

    // ─── SET ISOLATION LEVEL ───
    Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(
            con, definition);
    // Internally this calls:
    //   if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
    //       previousIsolationLevel = con.getTransactionIsolation();
    //       con.setTransactionIsolation(definition.getIsolationLevel());
    //       // ↑ Maps to JDBC: connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
    //       // ↑ Which sends to DB: SET TRANSACTION ISOLATION LEVEL READ COMMITTED
    //   }

    txObject.setPreviousIsolationLevel(previousIsolationLevel);

    con.setAutoCommit(false);  // BEGIN TRANSACTION

    // ... bind connection to ThreadLocal via TransactionSynchronizationManager
}

// ═══════════════════════════════════════════════════════════════════════════════
// How @Transactional(isolation = ...) maps to JDBC:
// ═══════════════════════════════════════════════════════════════════════════════

// @Transactional(isolation = Isolation.READ_UNCOMMITTED)
//   → con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED)   // value = 1
//   → SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED

// @Transactional(isolation = Isolation.READ_COMMITTED)
//   → con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)     // value = 2
//   → SET TRANSACTION ISOLATION LEVEL READ COMMITTED

// @Transactional(isolation = Isolation.REPEATABLE_READ)
//   → con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)    // value = 4
//   → SET TRANSACTION ISOLATION LEVEL REPEATABLE READ

// @Transactional(isolation = Isolation.SERIALIZABLE)
//   → con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)       // value = 8
//   → SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
```

**Industry Use Case — E-Commerce Inventory:**

```java
@Service
public class InventoryService {

    @Autowired private ProductRepository productRepository;

    // Default isolation (DB default) — for most operations
    @Transactional
    public void updateStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    // READ_COMMITTED — for balance checks (don't read dirty data)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int getStock(Long productId) {
        return productRepository.findById(productId)
                .map(Product::getStock)
                .orElse(0);
    }

    // REPEATABLE_READ — for price calculations (price must not change mid-TX)
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BigDecimal calculateOrderTotal(List<Long> productIds) {
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : productIds) {
            Product p = productRepository.findById(id).orElseThrow();
            total = total.add(p.getPrice());
            // Even if another TX changes the price, THIS TX will see
            // the same price it saw the first time (repeatable read)
        }
        return total;
    }

    // SERIALIZABLE — for critical financial operations
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transferStock(Long fromProductId, Long toProductId, int qty) {
        Product from = productRepository.findById(fromProductId).orElseThrow();
        Product to = productRepository.findById(toProductId).orElseThrow();
        from.setStock(from.getStock() - qty);
        to.setStock(to.getStock() + qty);
        productRepository.save(from);
        productRepository.save(to);
        // SERIALIZABLE ensures no other TX can read or write these rows
        // until this TX completes — as if TXs run one after another
    }
}
```

---

#### 1. Dirty Read Problem

A **Dirty Read** occurs when Transaction B reads data that Transaction A has **modified but NOT yet committed**. If Transaction A later **rolls back**, Transaction B has read data that **never actually existed** in the database.

**Real-World Use Case — Bank Account:**
A customer has $1000 in their account. Transaction A (a transfer) deducts $500 but hasn't committed yet. Transaction B (balance check) reads the balance as $500. Then Transaction A rolls back (transfer failed). The customer's real balance is $1000, but Transaction B reported $500 — a **dirty read**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dirty Read — Timeline:                                                          │
│                                                                                  │
│  Time    TX-A (Transfer)                    TX-B (Balance Check)                 │
│  ─────   ─────────────────────────────      ──────────────────────────────       │
│                                                                                  │
│  T1      BEGIN;                                                                  │
│          SELECT balance FROM accounts                                            │
│          WHERE id = 1;                                                           │
│          -- Result: balance = 1000                                               │
│                                                                                  │
│  T2      UPDATE accounts                                                         │
│          SET balance = 500                                                       │
│          WHERE id = 1;                                                           │
│          -- balance changed to 500                                               │
│          -- but NOT COMMITTED yet!                                               │
│                                                                                  │
│  T3                                         BEGIN;                               │
│                                             SELECT balance FROM accounts         │
│                                             WHERE id = 1;                        │
│                                                                                  │
│                                             ┌─────────────────────────────┐      │
│                                             │ 🔴 DIRTY READ!              │      │
│                                             │ Result: balance = 500       │      │
│                                             │ (reads UNCOMMITTED data!)   │      │
│                                             └─────────────────────────────┘      │
│                                                                                  │
│  T4      ROLLBACK;                                                               │
│          -- Transfer failed!                                                     │
│          -- balance is back to 1000                                              │
│                                                                                  │
│  T5                                         -- TX-B used balance = 500           │
│                                             -- but real balance is 1000!         │
│                                             -- TX-B made decisions based on      │
│                                             -- data that NEVER existed!          │
│                                             COMMIT;                              │
│                                                                                  │
│  Concurrency Level: HIGHEST (no locks, no waits)                                 │
│  Data Integrity:    LOWEST  (reading phantom/ghost data)                         │
│                                                                                  │
│  ✅ SOLUTION: Use READ_COMMITTED or higher isolation level.                      │
│  At READ_COMMITTED, TX-B would see balance = 1000 (only committed data).         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example — Dirty Read Happening:**

```java
// ─── TX-A: Transfer (runs on Thread-1) ───
@Service
public class TransferService {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void transfer(Long fromAccountId, BigDecimal amount) {
        // T1: Deduct from sender
        jdbcTemplate.update(
                "UPDATE accounts SET balance = balance - ? WHERE id = ?",
                amount, fromAccountId);
        // balance changed in DB buffer — NOT committed yet

        // T2: Some slow validation...
        validateTransfer(fromAccountId, amount);  // takes 5 seconds

        // T3: If validation fails → ROLLBACK
        // But TX-B may have already read the deducted balance!
    }
}

// ─── TX-B: Balance Check (runs on Thread-2) ───
@Service
public class BalanceService {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public BigDecimal getBalance(Long accountId) {
        // 🔴 DIRTY READ: Reads TX-A's uncommitted UPDATE!
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?",
                BigDecimal.class, accountId);
        // Returns 500 instead of 1000!
    }
}
```

```sql
-- T1: TX-A begins and deducts
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
START TRANSACTION;  -- TX-A
UPDATE accounts SET balance = balance - 500 WHERE id = 1;
-- balance in buffer: 500 (was 1000). NOT committed.

-- T3: TX-B reads UNCOMMITTED data (DIRTY READ!)
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
START TRANSACTION;  -- TX-B
SELECT balance FROM accounts WHERE id = 1;
-- Returns: 500  ← 🔴 DIRTY! This is uncommitted data from TX-A!
COMMIT;  -- TX-B

-- T4: TX-A rolls back
ROLLBACK;  -- TX-A
-- balance reverts to 1000
-- But TX-B already used 500! DATA INCONSISTENCY!
```

**How to Solve:** Use `Isolation.READ_COMMITTED` or higher:

```java
@Transactional(isolation = Isolation.READ_COMMITTED)  // ✅ Prevents dirty reads
public BigDecimal getBalance(Long accountId) {
    // Now TX-B will ONLY see committed data
    // It will see balance = 1000 (the last committed value)
    return jdbcTemplate.queryForObject(
            "SELECT balance FROM accounts WHERE id = ?",
            BigDecimal.class, accountId);
}
```

---

#### 2. Non-Repeatable Read Problem

A **Non-Repeatable Read** occurs when Transaction B reads the **same row twice** and gets **different values** because Transaction A **committed an UPDATE** between the two reads.

**Real-World Use Case — Flight Ticket Pricing:**
A customer checks a flight price ($300), adds it to cart, then proceeds to checkout. Between the price check and checkout, another transaction changes the price to $450. When the checkout reads the price again, it's $450 — the price **changed between two reads in the same transaction**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Non-Repeatable Read — Timeline:                                                 │
│                                                                                  │
│  Time    TX-A (Price Update)                TX-B (Booking Flow)                  │
│  ─────   ─────────────────────────────      ──────────────────────────────       │
│                                                                                  │
│  T1                                         BEGIN;                               │
│                                             SELECT price FROM flights            │
│                                             WHERE flight_id = 'AA101';           │
│                                             -- Result: price = 300               │
│                                             -- (shows $300 to customer)          │
│                                                                                  │
│  T2      BEGIN;                                                                  │
│          UPDATE flights                                                          │
│          SET price = 450                                                         │
│          WHERE flight_id = 'AA101';                                              │
│          COMMIT;                                                                 │
│          -- ✅ Committed! Price is now 450.                                      │
│                                                                                  │
│  T3                                         -- Customer clicks "Checkout"        │
│                                             -- System reads price AGAIN:         │
│                                             SELECT price FROM flights            │
│                                             WHERE flight_id = 'AA101';           │
│                                                                                  │
│                                             ┌─────────────────────────────┐      │
│                                             │ 🟡 NON-REPEATABLE READ!     │      │
│                                             │ Result: price = 450         │      │
│                                             │ (was 300 at T1, now 450!)   │      │
│                                             │ Same row, different value!  │      │
│                                             └─────────────────────────────┘      │
│                                                                                  │
│  T4                                         -- Charges customer $450             │
│                                             -- Customer expected $300!           │
│                                             COMMIT;                              │
│                                                                                  │
│  Concurrency Level: HIGH (only committed reads, but rows can change)             │
│  Data Integrity:    MEDIUM (no dirty reads, but same query returns diff results) │
│                                                                                  │
│  ✅ SOLUTION: Use REPEATABLE_READ or higher isolation level.                     │
│  At REPEATABLE_READ, TX-B's second SELECT would still return 300.                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example:**

```java
// ─── TX-A: Airline updates price (Thread-1) ───
@Service
public class FlightPricingService {

    @Transactional
    public void updatePrice(String flightId, BigDecimal newPrice) {
        // T2: Update and commit
        flightRepository.updatePrice(flightId, newPrice);
        // After commit, price = 450 in DB
    }
}

// ─── TX-B: Customer booking (Thread-2) ───
@Service
public class BookingService {

    @Autowired private FlightRepository flightRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)  // allows non-repeatable reads!
    public Booking bookFlight(String flightId, Long customerId) {
        // T1: First read — price = 300
        Flight flight = flightRepository.findById(flightId).orElseThrow();
        BigDecimal displayPrice = flight.getPrice();  // 300

        // ... customer reviews booking details, takes 10 seconds ...
        processCustomerReview(customerId);

        // T3: Second read — price = 450 (TX-A committed between T1 and T3!)
        flight = flightRepository.findById(flightId).orElseThrow();
        BigDecimal checkoutPrice = flight.getPrice();  // 🟡 450! Non-repeatable read!

        if (!displayPrice.equals(checkoutPrice)) {
            throw new PriceChangedException("Price changed from " +
                    displayPrice + " to " + checkoutPrice);
        }

        return createBooking(flight, customerId, checkoutPrice);
    }
}
```

```sql
-- T1: TX-B first read
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;  -- TX-B
SELECT price FROM flights WHERE flight_id = 'AA101';
-- Returns: 300

-- T2: TX-A updates and commits
START TRANSACTION;  -- TX-A
UPDATE flights SET price = 450 WHERE flight_id = 'AA101';
COMMIT;  -- TX-A committed ✅

-- T3: TX-B second read — SAME query, DIFFERENT result!
SELECT price FROM flights WHERE flight_id = 'AA101';
-- Returns: 450  ← 🟡 NON-REPEATABLE READ!
-- Was 300 at T1, now 450 at T3 (because TX-A committed in between)

COMMIT;  -- TX-B
```

**How to Solve:** Use `Isolation.REPEATABLE_READ`:

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)  // ✅ Prevents non-repeatable reads
public Booking bookFlight(String flightId, Long customerId) {
    // T1: First read — price = 300
    Flight flight = flightRepository.findById(flightId).orElseThrow();
    // T3: Second read — STILL price = 300 (even though TX-A committed 450!)
    // REPEATABLE_READ guarantees the SAME value for the same row within a TX
    flight = flightRepository.findById(flightId).orElseThrow();
    // price is still 300 ✅
}
```

---

#### 3. Phantom Read Problem

A **Phantom Read** occurs when Transaction B executes the **same range query twice** and gets **different sets of rows** because Transaction A **INSERT**ed or **DELETE**d rows that match the query between the two reads.

The key difference from non-repeatable read: **non-repeatable read** is about the same row returning different values; **phantom read** is about **new/deleted rows appearing/disappearing**.

**Real-World Use Case — Seat Availability:**
A booking system counts available seats on a flight (10 seats). While the customer is confirming, another transaction books 3 seats. The second count shows only 7 seats — **3 phantom rows disappeared**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Phantom Read — Timeline:                                                        │
│                                                                                  │
│  Time    TX-A (New Bookings)                TX-B (Seat Count)                    │
│  ─────   ─────────────────────────────      ──────────────────────────────       │
│                                                                                  │
│  T1                                         BEGIN;                               │
│                                             SELECT COUNT(*) FROM bookings        │
│                                             WHERE flight_id = 'AA101'            │
│                                             AND status = 'CONFIRMED';            │
│                                             -- Result: 40 bookings               │
│                                             -- Available = 200 - 40 = 160 seats  │
│                                                                                  │
│  T2      BEGIN;                                                                  │
│          INSERT INTO bookings                                                    │
│            (flight_id, customer_id, status)                                      │
│          VALUES ('AA101', 501, 'CONFIRMED'),                                     │
│                 ('AA101', 502, 'CONFIRMED'),                                     │
│                 ('AA101', 503, 'CONFIRMED');                                     │
│          COMMIT;  ✅                                                             │
│          -- 3 NEW rows added to bookings table                                   │
│                                                                                  │
│  T3                                         -- System re-counts for validation:  │
│                                             SELECT COUNT(*) FROM bookings        │
│                                             WHERE flight_id = 'AA101'            │
│                                             AND status = 'CONFIRMED';            │
│                                                                                  │
│                                             ┌─────────────────────────────┐      │
│                                             │ 👻 PHANTOM READ!            │      │
│                                             │ Result: 43 bookings         │      │
│                                             │ (was 40 at T1, now 43!)     │      │
│                                             │ 3 "phantom" rows appeared!  │      │
│                                             └─────────────────────────────┘      │
│                                                                                  │
│  T4                                         -- Available = 200 - 43 = 157 seats  │
│                                             -- But we told customer 160!         │
│                                             COMMIT;                              │
│                                                                                  │
│  Concurrency Level: MEDIUM (existing rows locked, but range not locked)          │
│  Data Integrity:    MEDIUM-HIGH (rows can appear/disappear in range queries)     │
│                                                                                  │
│  ✅ SOLUTION: Use SERIALIZABLE isolation level.                                  │
│  At SERIALIZABLE, TX-A's INSERT would BLOCK until TX-B completes.                │
│  (or TX-B would see a consistent snapshot — depending on DB implementation)      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Code Example:**

```java
// ─── TX-A: New bookings (Thread-1) ───
@Service
public class BookingService {

    @Transactional
    public void bulkBook(String flightId, List<Long> customerIds) {
        // T2: Insert new bookings and commit
        for (Long customerId : customerIds) {
            bookingRepository.save(new Booking(flightId, customerId, "CONFIRMED"));
        }
        // After commit: 3 new rows in bookings table
    }
}

// ─── TX-B: Availability check (Thread-2) ───
@Service
public class AvailabilityService {

    @Autowired private BookingRepository bookingRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ)  // still allows phantom reads!
    public int getAvailableSeats(String flightId, int totalCapacity) {
        // T1: First count
        int bookedCount = bookingRepository.countByFlightIdAndStatus(
                flightId, "CONFIRMED");
        // Result: 40

        // ... processing time ...
        calculatePricing(flightId);

        // T3: Second count — PHANTOM READ!
        int recount = bookingRepository.countByFlightIdAndStatus(
                flightId, "CONFIRMED");
        // Result: 43  ← 👻 3 phantom rows appeared!
        // REPEATABLE_READ prevents existing rows from changing,
        // but CANNOT prevent NEW rows from appearing in range queries

        return totalCapacity - recount;
    }
}
```

```sql
-- T1: TX-B counts bookings
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;  -- TX-B
SELECT COUNT(*) FROM bookings
WHERE flight_id = 'AA101' AND status = 'CONFIRMED';
-- Returns: 40

-- T2: TX-A inserts new bookings and commits
START TRANSACTION;  -- TX-A
INSERT INTO bookings (flight_id, customer_id, status)
VALUES ('AA101', 501, 'CONFIRMED'),
       ('AA101', 502, 'CONFIRMED'),
       ('AA101', 503, 'CONFIRMED');
COMMIT;  -- TX-A ✅

-- T3: TX-B re-counts — same query, different row count!
SELECT COUNT(*) FROM bookings
WHERE flight_id = 'AA101' AND status = 'CONFIRMED';
-- Returns: 43  ← 👻 PHANTOM READ! 3 new rows appeared!

COMMIT;  -- TX-B

-- NOTE: In MySQL/InnoDB with REPEATABLE_READ, phantom reads are actually
-- prevented by "gap locks" (next-key locking). This is MySQL-specific.
-- In PostgreSQL and standard SQL, REPEATABLE_READ does NOT prevent phantoms.
```

**How to Solve:** Use `Isolation.SERIALIZABLE`:

```java
@Transactional(isolation = Isolation.SERIALIZABLE)  // ✅ Prevents phantom reads
public int getAvailableSeats(String flightId, int totalCapacity) {
    // T1: First count = 40
    int count1 = bookingRepository.countByFlightIdAndStatus(flightId, "CONFIRMED");
    // T3: Second count = 40 (TX-A's INSERT is BLOCKED or invisible)
    int count2 = bookingRepository.countByFlightIdAndStatus(flightId, "CONFIRMED");
    // Both counts are 40 ✅ — no phantom rows
    return totalCapacity - count2;
}
```

---

#### 4. Database Locking — Shared Lock (Read Lock) and Exclusive Lock (Write Lock)

Database locks are the **mechanism** that isolation levels use to prevent concurrency anomalies. There are two fundamental lock types:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Two Types of Locks:                                                             │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐     │
│  │ SHARED LOCK (S-Lock) — Read Lock                                        │     │
│  │                                                                         │     │
│  │ • Acquired when a transaction READS a row (SELECT)                      │     │
│  │ • Multiple transactions can hold S-Locks on the SAME row simultaneously │     │
│  │ • Allows OTHER transactions to also READ (acquire S-Lock)               │     │
│  │ • BLOCKS other transactions from WRITING (acquiring X-Lock)             │     │
│  │ • Purpose: "I'm reading this — don't modify it while I'm reading"      │     │
│  │                                                                         │     │
│  │ SQL: SELECT ... LOCK IN SHARE MODE  (MySQL)                             │     │
│  │      SELECT ... FOR SHARE           (PostgreSQL)                        │     │
│  └──────────────────────────────────────────────────────────────────────────┘     │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐     │
│  │ EXCLUSIVE LOCK (X-Lock) — Write Lock                                    │     │
│  │                                                                         │     │
│  │ • Acquired when a transaction WRITES a row (INSERT/UPDATE/DELETE)        │     │
│  │ • Only ONE transaction can hold an X-Lock on a row at a time            │     │
│  │ • BLOCKS all other transactions from READING (S-Lock) and WRITING       │     │
│  │ • Purpose: "I'm modifying this — nobody can read or write it"           │     │
│  │                                                                         │     │
│  │ SQL: SELECT ... FOR UPDATE          (MySQL & PostgreSQL)                │     │
│  │      UPDATE / DELETE / INSERT       (automatically acquires X-Lock)     │     │
│  └──────────────────────────────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Lock Compatibility Matrix:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Lock Compatibility Matrix:                                                      │
│                                                                                  │
│  "If TX-A holds Lock X on a row, can TX-B acquire Lock Y on the SAME row?"      │
│                                                                                  │
│  ┌──────────────────────┬─────────────────────┬─────────────────────┐            │
│  │ TX-A holds \  TX-B   │ S-Lock (Read)       │ X-Lock (Write)      │            │
│  │ wants →               │                     │                     │            │
│  ├──────────────────────┼─────────────────────┼─────────────────────┤            │
│  │ S-Lock (Read)         │ ✅ YES (compatible)  │ ❌ NO (blocks/waits) │            │
│  │                      │ Both can read       │ TX-B must WAIT      │            │
│  ├──────────────────────┼─────────────────────┼─────────────────────┤            │
│  │ X-Lock (Write)        │ ❌ NO (blocks/waits) │ ❌ NO (blocks/waits) │            │
│  │                      │ TX-B must WAIT      │ TX-B must WAIT      │            │
│  └──────────────────────┴─────────────────────┴─────────────────────┘            │
│                                                                                  │
│  Summary:                                                                        │
│  • S + S = ✅ (multiple readers allowed)                                         │
│  • S + X = ❌ (reader blocks writer)                                             │
│  • X + S = ❌ (writer blocks reader)                                             │
│  • X + X = ❌ (writer blocks writer)                                             │
│                                                                                  │
│  Key Insight: EXCLUSIVE lock is truly exclusive — it blocks EVERYTHING.           │
│  SHARED lock only blocks writes, not other reads.                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Multithreading Example — Two Transactions Competing for Locks:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Two Transactions — Lock Interaction:                                            │
│                                                                                  │
│  SCENARIO 1: Both READ (S-Lock + S-Lock) → ✅ No blocking                       │
│  ──────────────────────────────────────────────                                   │
│  Time   Thread-1 (TX-A)                    Thread-2 (TX-B)                       │
│  T1     SELECT ... WHERE id=1              SELECT ... WHERE id=1                 │
│         Acquires S-Lock on row 1 ✅         Acquires S-Lock on row 1 ✅           │
│         (reads balance = 1000)             (reads balance = 1000)                │
│  T2     -- Both proceed. No blocking.      -- Both proceed.                      │
│                                                                                  │
│  ──────────────────────────────────────────────                                   │
│  SCENARIO 2: One READS, One WRITES (S-Lock + X-Lock) → ❌ Blocking              │
│  ──────────────────────────────────────────────                                   │
│  Time   Thread-1 (TX-A)                    Thread-2 (TX-B)                       │
│  T1     SELECT ... WHERE id=1                                                    │
│         Acquires S-Lock on row 1 ✅                                               │
│  T2                                        UPDATE ... WHERE id=1                 │
│                                            Wants X-Lock on row 1                 │
│                                            ❌ BLOCKED! (S-Lock held by TX-A)     │
│                                            ⏳ Waiting...                          │
│  T3     COMMIT;                                                                  │
│         Releases S-Lock ✅                                                        │
│  T4                                        X-Lock granted ✅                      │
│                                            UPDATE proceeds.                      │
│                                                                                  │
│  ──────────────────────────────────────────────                                   │
│  SCENARIO 3: Both WRITE (X-Lock + X-Lock) → ❌ Blocking                         │
│  ──────────────────────────────────────────────                                   │
│  Time   Thread-1 (TX-A)                    Thread-2 (TX-B)                       │
│  T1     UPDATE ... SET balance=500         UPDATE ... SET balance=300            │
│         WHERE id=1                         WHERE id=1                            │
│         Acquires X-Lock on row 1 ✅         Wants X-Lock on row 1                │
│                                            ❌ BLOCKED! (X-Lock held by TX-A)     │
│  T2     -- TX-A does more work...          ⏳ Waiting...                          │
│  T3     COMMIT;                                                                  │
│         Releases X-Lock ✅                                                        │
│  T4                                        X-Lock granted ✅                      │
│                                            UPDATE proceeds.                      │
│                                            (balance becomes 300, not 500)        │
│                                                                                  │
│  ──────────────────────────────────────────────                                   │
│  DEADLOCK SCENARIO: Both want what the other has → 💀                            │
│  ──────────────────────────────────────────────                                   │
│  Time   Thread-1 (TX-A)                    Thread-2 (TX-B)                       │
│  T1     X-Lock on row 1 ✅                  X-Lock on row 2 ✅                    │
│  T2     Wants X-Lock on row 2              Wants X-Lock on row 1                 │
│         ❌ BLOCKED (TX-B holds row 2)       ❌ BLOCKED (TX-A holds row 1)         │
│         ⏳ Waiting for TX-B...              ⏳ Waiting for TX-A...                 │
│                                                                                  │
│         💀 DEADLOCK! Both waiting for each other forever!                        │
│         DB detects → kills one TX with "Deadlock found" error                    │
│         Killed TX gets rolled back; surviving TX proceeds.                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**When Locks Are Released — Depends on Isolation Level:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  When Are Locks Released?                                                        │
│                                                                                  │
│  ┌──────────────────────┬──────────────────────────────────────────────────┐      │
│  │ Isolation Level       │ When S-Lock (Read Lock) is Released              │      │
│  ├──────────────────────┼──────────────────────────────────────────────────┤      │
│  │ READ_UNCOMMITTED     │ No S-Lock acquired at all (reads without lock)   │      │
│  ├──────────────────────┼──────────────────────────────────────────────────┤      │
│  │ READ_COMMITTED       │ Released IMMEDIATELY after the SELECT completes  │      │
│  │                      │ (short-duration lock)                            │      │
│  ├──────────────────────┼──────────────────────────────────────────────────┤      │
│  │ REPEATABLE_READ      │ Held until END of transaction (COMMIT/ROLLBACK)  │      │
│  │                      │ (long-duration lock)                             │      │
│  ├──────────────────────┼──────────────────────────────────────────────────┤      │
│  │ SERIALIZABLE         │ Held until END of transaction + RANGE LOCKS on   │      │
│  │                      │ index ranges (prevents phantom inserts)          │      │
│  └──────────────────────┴──────────────────────────────────────────────────┘      │
│                                                                                  │
│  X-Lock (Write Lock) is ALWAYS held until COMMIT/ROLLBACK at all levels.         │
│                                                                                  │
│  Key Insight: The difference between isolation levels is primarily about          │
│  HOW LONG shared (read) locks are held.                                          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 5. Isolation Levels — Values, Defaults, and Anomaly Matrix

**Spring `Isolation` enum values and their JDBC constants:**

```java
// Spring's Isolation enum:
public enum Isolation {
    DEFAULT(-1),                // Use the DB's default isolation
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),   // 1
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),       // 2
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),     // 4
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);           // 8
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Isolation Levels — Anomaly Matrix:                                              │
│                                                                                  │
│  ┌──────────────────────┬────────────┬──────────────────┬──────────────┬────────┐│
│  │ Isolation Level       │ Dirty Read │ Non-Repeatable   │ Phantom Read │ Concur-││
│  │                      │            │ Read             │              │ rency  ││
│  ├──────────────────────┼────────────┼──────────────────┼──────────────┼────────┤│
│  │ READ_UNCOMMITTED     │ ✅ Possible │ ✅ Possible       │ ✅ Possible   │ ⬆⬆⬆⬆   ││
│  │                      │            │                  │              │ Highest││
│  ├──────────────────────┼────────────┼──────────────────┼──────────────┼────────┤│
│  │ READ_COMMITTED       │ ❌ Prevented│ ✅ Possible       │ ✅ Possible   │ ⬆⬆⬆    ││
│  │                      │            │                  │              │ High   ││
│  ├──────────────────────┼────────────┼──────────────────┼──────────────┼────────┤│
│  │ REPEATABLE_READ      │ ❌ Prevented│ ❌ Prevented      │ ✅ Possible*  │ ⬆⬆     ││
│  │                      │            │                  │              │ Medium ││
│  ├──────────────────────┼────────────┼──────────────────┼──────────────┼────────┤│
│  │ SERIALIZABLE         │ ❌ Prevented│ ❌ Prevented      │ ❌ Prevented  │ ⬆      ││
│  │                      │            │                  │              │ Lowest ││
│  └──────────────────────┴────────────┴──────────────────┴──────────────┴────────┘│
│                                                                                  │
│  * MySQL/InnoDB's REPEATABLE_READ also prevents phantoms via gap locks.           │
│    Standard SQL and PostgreSQL's REPEATABLE_READ do NOT prevent phantoms.         │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────────┐ │
│  │                   Safety ──────────────────────► Higher                     │ │
│  │  READ_UNCOMMITTED → READ_COMMITTED → REPEATABLE_READ → SERIALIZABLE        │ │
│  │                   Concurrency ─────────────────► Lower                     │ │
│  └──────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Default Isolation Level Per Database:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Default Isolation Level Per Database:                                            │
│                                                                                  │
│  ┌────────────────────────────┬────────────────────────────────────────────────┐  │
│  │ Database                   │ Default Isolation Level                        │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ MySQL (InnoDB)             │ REPEATABLE_READ                               │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ PostgreSQL                 │ READ_COMMITTED                                │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ Oracle                     │ READ_COMMITTED                                │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ SQL Server                 │ READ_COMMITTED                                │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ H2 (in-memory)            │ READ_COMMITTED                                │  │
│  ├────────────────────────────┼────────────────────────────────────────────────┤  │
│  │ SQLite                     │ SERIALIZABLE                                  │  │
│  └────────────────────────────┴────────────────────────────────────────────────┘  │
│                                                                                  │
│  Important: When Spring uses @Transactional(isolation = Isolation.DEFAULT),       │
│  it does NOT set any isolation level — it lets the DB use its own default.        │
│                                                                                  │
│  To check current isolation in your DB:                                           │
│  MySQL:      SELECT @@transaction_isolation;                                     │
│  PostgreSQL: SHOW default_transaction_isolation;                                 │
│  Oracle:     SELECT * FROM V$TRANSACTION;                                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 6. Isolation.READ_UNCOMMITTED

**Behavior:** The **lowest** isolation level. Transactions can read data that has been **modified but NOT yet committed** by other transactions.

**Prevents:** Nothing.
**Allows:** Dirty reads ✅, Non-repeatable reads ✅, Phantom reads ✅

**Concurrency Level:** **HIGHEST** — almost no locking, maximum throughput.

**Locking Strategy:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  READ_UNCOMMITTED — Locking Behavior:                                            │
│                                                                                  │
│  READ operations:                                                                │
│    • NO shared (S) locks acquired at all                                         │
│    • Reads directly from the buffer/dirty pages                                  │
│    • Ignores other transactions' exclusive (X) locks for reading                 │
│    • Can see uncommitted changes in real-time                                    │
│                                                                                  │
│  WRITE operations:                                                               │
│    • X-Lock acquired (same as all isolation levels)                              │
│    • Held until COMMIT/ROLLBACK                                                  │
│                                                                                  │
│  Lock Timeline:                                                                  │
│  TX-B: SELECT balance FROM accounts WHERE id = 1;                                │
│        │                                                                         │
│        ├── No S-Lock acquired                                                    │
│        ├── Reads directly from data page (even if modified by TX-A)              │
│        └── Done. Nothing to release.                                             │
│                                                                                  │
│  Because no read locks are taken:                                                │
│    • Writers are NEVER blocked by readers                                        │
│    • Readers are NEVER blocked by writers                                        │
│    • Maximum concurrency, minimum safety                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why Does READ_UNCOMMITTED Exist?**

1. **Approximate analytics/reporting** — When you need rough counts or sums and absolute accuracy doesn't matter
2. **Monitoring dashboards** — Real-time metrics where a 0.01% error is acceptable
3. **Large table scans** — Reading millions of rows without blocking any writers
4. **Queue length estimation** — "Approximately how many jobs are pending?"

**Industry Use Case — Real-Time Dashboard Metrics:**

```java
@Service
public class DashboardService {

    @Autowired private JdbcTemplate jdbcTemplate;

    // READ_UNCOMMITTED: We want FAST approximate counts for a dashboard.
    // It's OK if the numbers are slightly off — they refresh every 5 seconds.
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
    public DashboardMetrics getLiveMetrics() {

        // These queries run WITHOUT acquiring any shared locks.
        // They can read uncommitted data — but that's fine for a dashboard.

        long totalOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE created_at >= CURDATE()",
                Long.class);

        BigDecimal totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payments " +
                "WHERE status = 'COMPLETED' AND created_at >= CURDATE()",
                BigDecimal.class);

        long pendingShipments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status = 'PENDING_SHIPMENT'",
                Long.class);

        return new DashboardMetrics(totalOrders, totalRevenue, pendingShipments);
    }
}
```

**Internal Code — How Spring Sets READ_UNCOMMITTED:**

```java
// @Transactional(isolation = Isolation.READ_UNCOMMITTED) triggers:

// Step 1: TransactionInterceptor reads the isolation attribute
TransactionAttribute txAttr = ...;  // isolation = READ_UNCOMMITTED

// Step 2: DataSourceTransactionManager.doBegin()
Connection con = dataSource.getConnection();
con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);  // value = 1
// ↑ This sends to MySQL:
// SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;

con.setAutoCommit(false);  // BEGIN TRANSACTION
```

**Generated Raw SQL:**

```sql
-- Spring internally executes:
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SET autocommit = 0;
START TRANSACTION;

-- TX-B: Dashboard query (no S-locks acquired!)
SELECT COUNT(*) FROM orders WHERE created_at >= CURDATE();
-- Returns: 1547 (includes 3 uncommitted INSERTs from TX-A)
-- ↑ 🔴 Dirty read! But acceptable for dashboard metrics.

SELECT COALESCE(SUM(amount), 0) FROM payments
WHERE status = 'COMPLETED' AND created_at >= CURDATE();
-- Returns: 45230.50 (includes an uncommitted UPDATE from TX-C that changed
-- a payment from 100 to 200 — adds an extra 100)
-- ↑ 🔴 Dirty read! But we're showing approximate revenue anyway.

SELECT COUNT(*) FROM orders WHERE status = 'PENDING_SHIPMENT';
-- Returns: 23

COMMIT;
-- No locks held at any point during these reads.
-- Writers (TX-A, TX-C) were NEVER blocked.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  READ_UNCOMMITTED — Dirty Read in Action:                                        │
│                                                                                  │
│  Time   TX-A (Order Creation)              TX-B (Dashboard — READ_UNCOMMITTED)   │
│  ─────  ──────────────────────             ──────────────────────────────────     │
│                                                                                  │
│  T1     BEGIN;                                                                   │
│         INSERT INTO orders (...)                                                 │
│         VALUES (..., 'PENDING');                                                 │
│         -- NOT committed yet!                                                    │
│                                                                                  │
│  T2                                        BEGIN;                                │
│                                            SELECT COUNT(*) FROM orders           │
│                                            WHERE created_at >= CURDATE();        │
│                                            -- 🔴 INCLUDES TX-A's uncommitted row!│
│                                            -- Returns: 1548 (instead of 1547)    │
│                                                                                  │
│  T3     ROLLBACK;                          -- TX-B used count 1548               │
│         -- Order insert undone!            -- Real count was 1547                 │
│                                            -- Dashboard showed 1548              │
│                                            -- Refreshes in 5 sec anyway ¯\_(ツ)_/¯│
│                                            COMMIT;                               │
│                                                                                  │
│  For dashboards, this is ACCEPTABLE.                                             │
│  For financial transactions, this is DANGEROUS.                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 7. Isolation.READ_COMMITTED

**Behavior:** A transaction can only read data that has been **committed** by other transactions. Uncommitted (dirty) data is invisible.

**Prevents:** Dirty reads ❌
**Allows:** Non-repeatable reads ✅, Phantom reads ✅

**Concurrency Level:** **HIGH** — short-lived read locks, good throughput.

This is the **default isolation level** for PostgreSQL, Oracle, SQL Server, and H2.

**Locking Strategy:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  READ_COMMITTED — Locking Behavior:                                              │
│                                                                                  │
│  READ operations:                                                                │
│    • S-Lock acquired at the START of the SELECT                                  │
│    • S-Lock released IMMEDIATELY after the SELECT completes                      │
│    • This means: between two SELECTs in the same TX,                             │
│      another TX can UPDATE and COMMIT the same row                               │
│    • Result: non-repeatable reads are possible                                   │
│                                                                                  │
│  WRITE operations:                                                               │
│    • X-Lock acquired (same as all levels)                                        │
│    • Held until COMMIT/ROLLBACK                                                  │
│                                                                                  │
│  Lock Timeline:                                                                  │
│                                                                                  │
│  TX-B: SELECT #1              SELECT #2                                          │
│        ├──S-Lock──┤           ├──S-Lock──┤                                       │
│        Acquire  Release       Acquire  Release                                   │
│                    ↑                                                             │
│                    │ Gap! No lock held here.                                     │
│                    │ TX-A can UPDATE and COMMIT in this gap!                      │
│                    │ → Non-repeatable read!                                       │
│                                                                                  │
│  PostgreSQL/Oracle approach (MVCC — no S-locks at all):                          │
│    • Instead of S-Locks, reads see a SNAPSHOT of committed data                  │
│    • Each SELECT sees the latest COMMITTED version at the time of that SELECT    │
│    • Two SELECTs in the same TX can see different committed versions              │
│    • Result: same effect — non-repeatable reads possible, but no lock contention │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why Does READ_COMMITTED Exist?**

1. **Best balance** for most OLTP applications — prevents dirty reads without hurting concurrency
2. **Default for PostgreSQL/Oracle** — industry standard for transactional applications
3. **Sufficient for most CRUD** — rarely do you read the same row twice in one transaction
4. **Low lock contention** — S-locks are released immediately, writers rarely blocked

**Industry Use Case — E-Commerce Order Processing:**

```java
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryRepository inventoryRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Order placeOrder(Long productId, int quantity) {

        // Read 1: Check inventory (S-Lock acquired and RELEASED immediately)
        Product product = inventoryRepository.findById(productId).orElseThrow();
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Not enough stock");
        }
        // S-Lock released! Another TX could update stock RIGHT NOW.

        // ... build order, validate address, calculate shipping (takes time) ...
        Order order = buildOrder(product, quantity);

        // Read 2: Re-check inventory (new S-Lock, new read)
        product = inventoryRepository.findById(productId).orElseThrow();
        // 🟡 NON-REPEATABLE READ possible here!
        // Stock might have changed if another TX committed an UPDATE between
        // Read 1 and Read 2.

        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Stock changed — not enough");
        }

        // Deduct stock (X-Lock acquired, held until COMMIT)
        product.setStock(product.getStock() - quantity);
        inventoryRepository.save(product);
        order.setStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }
}
```

**Internal Code — How Spring Sets READ_COMMITTED:**

```java
// @Transactional(isolation = Isolation.READ_COMMITTED) triggers:

Connection con = dataSource.getConnection();
con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);  // value = 2

// MySQL:      SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
// PostgreSQL: SET TRANSACTION ISOLATION LEVEL READ COMMITTED;  (this is the default)
// Oracle:     (this is the default, no SET needed)

con.setAutoCommit(false);  // BEGIN TRANSACTION
```

**Generated Raw SQL:**

```sql
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SET autocommit = 0;
START TRANSACTION;  -- TX-B

-- Read 1: Check inventory (S-Lock acquired and released immediately)
Hibernate: select p.id, p.name, p.stock, p.price
           from products p where p.id = ?
           -- Binding: [42]
           -- Result: {id=42, name='Laptop', stock=10, price=999.99}
           -- S-Lock RELEASED immediately after SELECT completes

-- ... time passes (order building, validation) ...

-- Meanwhile, TX-A runs:
-- START TRANSACTION;
-- UPDATE products SET stock = 3 WHERE id = 42;  -- X-Lock acquired
-- COMMIT;  -- stock is now 3, committed ✅

-- Read 2: Re-check inventory (new S-Lock, new committed snapshot)
Hibernate: select p.id, p.name, p.stock, p.price
           from products p where p.id = ?
           -- Binding: [42]
           -- Result: {id=42, name='Laptop', stock=3, price=999.99}
           -- 🟡 stock was 10 at Read 1, now 3 at Read 2! (Non-repeatable read)
           -- But this is COMMITTED data — NOT dirty. TX-A committed its update.

-- Deduct stock (X-Lock held until COMMIT)
Hibernate: update products set stock = ? where id = ?
           -- Binding: [1, 42]  -- 3 - 2 = 1

Hibernate: insert into orders (product_id, quantity, status) values (?, ?, ?)
           -- Binding: [42, 2, 'CONFIRMED']

COMMIT;  -- TX-B (releases X-Lock)
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  READ_COMMITTED — Non-Repeatable Read in Action:                                 │
│                                                                                  │
│  Time   TX-A (Stock Update)                TX-B (Order — READ_COMMITTED)         │
│  ─────  ──────────────────────             ──────────────────────────────         │
│                                                                                  │
│  T1                                        BEGIN;                                │
│                                            SELECT stock FROM products            │
│                                            WHERE id = 42;                        │
│                                            -- stock = 10 ✅ (committed data)     │
│                                            -- S-Lock acquired and RELEASED       │
│                                                                                  │
│  T2     BEGIN;                                                                   │
│         UPDATE products SET stock = 3                                            │
│         WHERE id = 42;                                                           │
│         -- X-Lock acquired                                                       │
│         COMMIT; ✅                                                               │
│         -- stock is now 3 in DB                                                  │
│                                                                                  │
│  T3                                        SELECT stock FROM products            │
│                                            WHERE id = 42;                        │
│                                            -- stock = 3 🟡 (non-repeatable!)     │
│                                            -- But it IS committed data ✅        │
│                                            -- No dirty read, just changed value  │
│                                                                                  │
│  T4                                        UPDATE products SET stock = 1          │
│                                            WHERE id = 42;                        │
│                                            COMMIT;                               │
│                                                                                  │
│  ❌ Dirty Read:          PREVENTED (TX-B never sees uncommitted data)            │
│  🟡 Non-Repeatable Read: POSSIBLE  (stock changed from 10 to 3 between reads)   │
│  👻 Phantom Read:        POSSIBLE  (new rows can appear in range queries)        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 8. Isolation.REPEATABLE_READ

**Behavior:** Once a transaction reads a row, it will **always see the same value** for that row throughout the transaction, even if other transactions commit updates to it. The read is "repeatable."

**Prevents:** Dirty reads ❌, Non-repeatable reads ❌
**Allows:** Phantom reads ✅ (except in MySQL InnoDB which uses gap locks to prevent phantoms too)

**Concurrency Level:** **MEDIUM** — read locks held longer, more blocking.

This is the **default isolation level** for MySQL (InnoDB).

**Locking Strategy:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  REPEATABLE_READ — Locking Behavior:                                             │
│                                                                                  │
│  READ operations:                                                                │
│    • S-Lock acquired at the START of the SELECT                                  │
│    • S-Lock held until END of transaction (COMMIT/ROLLBACK)                      │
│    • No other TX can UPDATE or DELETE this row while S-Lock is held              │
│    • Result: reading the same row always returns the same value ✅               │
│                                                                                  │
│  WRITE operations:                                                               │
│    • X-Lock acquired (same as all levels)                                        │
│    • Held until COMMIT/ROLLBACK                                                  │
│                                                                                  │
│  Lock Timeline:                                                                  │
│                                                                                  │
│  TX-B: SELECT #1              SELECT #2                    COMMIT                │
│        ├────────── S-Lock held for entire transaction ──────────┤                │
│                                                                                  │
│        TX-A tries UPDATE in between?  ❌ BLOCKED until TX-B commits!             │
│        → Same value guaranteed on second SELECT ✅                               │
│                                                                                  │
│  Compare with READ_COMMITTED:                                                    │
│  TX-B: SELECT #1   SELECT #2                                                    │
│        ├──S──┤      ├──S──┤     (locks released between reads)                   │
│               ↑                 (TX-A can UPDATE in the gap!)                    │
│                                                                                  │
│  MySQL InnoDB approach (MVCC + Gap Locks):                                       │
│    • Uses CONSISTENT SNAPSHOT from the FIRST read                                │
│    • All subsequent reads see data as of that snapshot                            │
│    • Gap locks prevent INSERTs in index ranges → prevents phantoms too            │
│    • No S-locks needed for reads → better concurrency than locking approach       │
│                                                                                  │
│  PostgreSQL approach (MVCC Snapshot):                                             │
│    • Snapshot taken at FIRST statement (not at BEGIN)                             │
│    • All reads see data as of that snapshot                                       │
│    • Does NOT use gap locks → phantoms still possible in edge cases              │
│    • On write conflict → throws serialization error, TX must retry               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why Does REPEATABLE_READ Exist?**

1. **Financial calculations** — When you read account balance multiple times and need the same value every time
2. **Report generation** — When a report reads data multiple times for cross-validation
3. **Inventory reservation** — Ensure the stock count doesn't change while you're processing
4. **MySQL default** — Deemed a good balance between safety and performance for InnoDB

**Industry Use Case — Financial Report Generation:**

```java
@Service
public class FinancialReportService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public FinancialReport generateMonthlyReport(Long accountId, YearMonth month) {

        // Read 1: Get account balance
        Account account = accountRepository.findById(accountId).orElseThrow();
        BigDecimal currentBalance = account.getBalance();  // 10,000

        // Read transactions for the month
        List<Transaction> txns = transactionRepository
                .findByAccountIdAndMonth(accountId, month);
        BigDecimal totalDebits = txns.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = txns.stream()
                .filter(t -> t.getType() == TransactionType.CREDIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Read 2: Re-read balance for cross-validation
        account = accountRepository.findById(accountId).orElseThrow();
        BigDecimal rereadBalance = account.getBalance();

        // With REPEATABLE_READ: currentBalance == rereadBalance ALWAYS ✅
        // Even if another TX committed a balance change between Read 1 and Read 2
        assert currentBalance.equals(rereadBalance);

        BigDecimal openingBalance = currentBalance
                .subtract(totalCredits)
                .add(totalDebits);

        return new FinancialReport(accountId, month,
                openingBalance, totalDebits, totalCredits, currentBalance);
    }
}
```

**Internal Code — How Spring Sets REPEATABLE_READ:**

```java
// @Transactional(isolation = Isolation.REPEATABLE_READ) triggers:

Connection con = dataSource.getConnection();
con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);  // value = 4

// MySQL:      SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;  (this is the default)
// PostgreSQL: SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;

con.setAutoCommit(false);  // BEGIN TRANSACTION
// MySQL InnoDB: first SELECT establishes the CONSISTENT SNAPSHOT
```

**Generated Raw SQL:**

```sql
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SET autocommit = 0;
START TRANSACTION;  -- TX-B (REPEATABLE_READ)

-- Read 1: Get account balance
-- (MySQL: establishes CONSISTENT SNAPSHOT at this point)
Hibernate: select a.id, a.balance, a.name
           from accounts a where a.id = ?
           -- Binding: [101]
           -- Result: {id=101, balance=10000.00, name='Alice'}
           -- S-Lock on row held until COMMIT (or snapshot established)

-- Read transactions
Hibernate: select t.id, t.account_id, t.amount, t.type, t.created_at
           from transactions t
           where t.account_id = ? and t.created_at between ? and ?
           -- Binding: [101, '2026-04-01', '2026-04-30']

-- Meanwhile, TX-A runs:
-- START TRANSACTION;
-- UPDATE accounts SET balance = 12000 WHERE id = 101;
-- COMMIT; ✅
-- But TX-B will NOT see this change!

-- Read 2: Re-read balance (REPEATABLE READ guarantees same value!)
Hibernate: select a.id, a.balance, a.name
           from accounts a where a.id = ?
           -- Binding: [101]
           -- Result: {id=101, balance=10000.00, name='Alice'}
           --                          ↑ STILL 10000! ✅
           -- Even though TX-A committed balance=12000, TX-B's snapshot
           -- was established at T1 and doesn't see TX-A's change.

COMMIT;  -- TX-B
-- After COMMIT, the NEXT transaction will see balance=12000.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  REPEATABLE_READ — Consistent Reads:                                             │
│                                                                                  │
│  Time   TX-A (Balance Update)              TX-B (Report — REPEATABLE_READ)       │
│  ─────  ──────────────────────             ──────────────────────────────         │
│                                                                                  │
│  T1                                        BEGIN;                                │
│                                            SELECT balance FROM accounts          │
│                                            WHERE id = 101;                       │
│                                            -- balance = 10000 ✅                 │
│                                            -- (snapshot established)             │
│                                                                                  │
│  T2     BEGIN;                                                                   │
│         UPDATE accounts SET balance = 12000                                      │
│         WHERE id = 101;                                                          │
│         COMMIT; ✅                                                               │
│         -- Committed! Real balance is 12000.                                     │
│                                                                                  │
│  T3                                        SELECT balance FROM accounts          │
│                                            WHERE id = 101;                       │
│                                            -- balance = 10000 ✅                 │
│                                            -- STILL 10000! Repeatable! ✅        │
│                                            -- TX-B reads from its snapshot       │
│                                                                                  │
│  T4                                        COMMIT;                               │
│                                            -- Snapshot released.                 │
│                                            -- Next TX will see 12000.            │
│                                                                                  │
│  ❌ Dirty Read:          PREVENTED                                               │
│  ❌ Non-Repeatable Read: PREVENTED (balance = 10000 in both reads) ✅            │
│  👻 Phantom Read:        POSSIBLE* (new rows can appear in range queries)        │
│                                                                                  │
│  * MySQL InnoDB: Also prevents phantoms via gap locks.                           │
│  * PostgreSQL:   Does NOT prevent phantoms.                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### 9. Isolation.SERIALIZABLE

**Behavior:** The **highest** isolation level. Transactions execute as if they were running **one after another** (serially), even though they may actually run concurrently. Completely eliminates all concurrency anomalies.

**Prevents:** Dirty reads ❌, Non-repeatable reads ❌, Phantom reads ❌
**Allows:** Nothing — all anomalies prevented.

**Concurrency Level:** **LOWEST** — heavy locking, transactions block each other frequently.

**Locking Strategy:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  SERIALIZABLE — Locking Behavior:                                                │
│                                                                                  │
│  READ operations:                                                                │
│    • S-Lock on ALL read rows held until COMMIT/ROLLBACK                          │
│    • RANGE LOCKS (Gap Locks) on index ranges — prevents INSERTs in ranges        │
│    • SELECT ... WHERE price > 100  →  locks the INDEX RANGE for price > 100     │
│    • No new rows matching the WHERE clause can be inserted by other TXs          │
│                                                                                  │
│  WRITE operations:                                                               │
│    • X-Lock acquired (same as all levels)                                        │
│    • Held until COMMIT/ROLLBACK                                                  │
│                                                                                  │
│  Lock Types Used:                                                                │
│    • Row-level S-Locks and X-Locks (same as REPEATABLE_READ)                     │
│    • Range Locks / Gap Locks (NEW — blocks INSERTs in index gaps)                │
│    • Next-Key Locks (combination of row lock + gap lock)                         │
│    • Predicate Locks (lock based on WHERE clause predicate)                      │
│                                                                                  │
│  Lock Timeline:                                                                  │
│                                                                                  │
│  TX-B: SELECT WHERE category='electronics'                      COMMIT           │
│        ├──── S-Lock on ALL matching rows ────────────────────────┤               │
│        ├──── Gap Lock on index range for category='electronics' ─┤               │
│                                                                                  │
│        TX-A tries INSERT INTO products (category='electronics')?                 │
│        ❌ BLOCKED! Gap lock prevents insert until TX-B commits.                  │
│        → No phantom rows can appear ✅                                           │
│                                                                                  │
│  PostgreSQL approach (SSI — Serializable Snapshot Isolation):                     │
│    • Uses MVCC snapshots (like REPEATABLE_READ) + dependency tracking            │
│    • Detects "dangerous structures" (read-write conflicts between TXs)           │
│    • If conflict detected → throws serialization error, TX must retry            │
│    • More optimistic — less blocking, but more rollbacks                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Why Does SERIALIZABLE Exist?**

1. **Financial transfers** — Double-spending prevention, account-to-account transfers
2. **Seat booking** — Ensuring no overbooking (exactly N seats, never N+1 bookings)
3. **Inventory management** — Stock can never go negative, even under extreme concurrency
4. **Regulatory compliance** — Banking systems that require zero anomalies by law

**Industry Use Case — Bank Account Transfer (Double-Spend Prevention):**

```java
@Service
public class BankTransferService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransferLogRepository transferLogRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransferResult transfer(Long fromAccountId, Long toAccountId,
                                   BigDecimal amount) {

        // SERIALIZABLE: These reads lock the rows AND the index ranges
        // No other TX can read, update, or insert conflicting data

        Account from = accountRepository.findById(fromAccountId).orElseThrow();
        Account to = accountRepository.findById(toAccountId).orElseThrow();

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Balance " + from.getBalance() + " < transfer " + amount);
        }

        // Deduct and credit
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);

        // Log the transfer
        TransferLog log = new TransferLog(fromAccountId, toAccountId,
                amount, LocalDateTime.now());
        transferLogRepository.save(log);

        // At SERIALIZABLE, if another TX tried to transfer from the same
        // account concurrently, it would be BLOCKED until this TX completes.
        // This prevents double-spending: only ONE TX at a time can
        // deduct from the same account.

        return new TransferResult(true, from.getBalance(), to.getBalance());
    }
}
```

**Internal Code — How Spring Sets SERIALIZABLE:**

```java
// @Transactional(isolation = Isolation.SERIALIZABLE) triggers:

Connection con = dataSource.getConnection();
con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);  // value = 8

// MySQL:      SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
// PostgreSQL: SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

con.setAutoCommit(false);  // BEGIN TRANSACTION
```

**Generated Raw SQL:**

```sql
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SET autocommit = 0;
START TRANSACTION;  -- TX-B (SERIALIZABLE)

-- Read from-account (S-Lock + Range Lock on account row)
Hibernate: select a.id, a.balance, a.name
           from accounts a where a.id = ?
           -- Binding: [101]
           -- Result: {id=101, balance=10000.00, name='Alice'}
           -- S-Lock on row id=101 — held until COMMIT

-- Read to-account (S-Lock + Range Lock on account row)
Hibernate: select a.id, a.balance, a.name
           from accounts a where a.id = ?
           -- Binding: [202]
           -- Result: {id=202, balance=5000.00, name='Bob'}
           -- S-Lock on row id=202 — held until COMMIT

-- If TX-A tries to read/update account 101 or 202 RIGHT NOW:
-- TX-A: UPDATE accounts SET balance = 9000 WHERE id = 101;
-- ❌ BLOCKED! TX-B holds S-Lock on row 101.
-- TX-A must WAIT until TX-B commits or rolls back.

-- Deduct from sender
Hibernate: update accounts set balance = ? where id = ?
           -- Binding: [7000.00, 101]  -- 10000 - 3000
           -- S-Lock upgraded to X-Lock on row 101

-- Credit to receiver
Hibernate: update accounts set balance = ? where id = ?
           -- Binding: [8000.00, 202]  -- 5000 + 3000
           -- S-Lock upgraded to X-Lock on row 202

-- Log the transfer
Hibernate: insert into transfer_log (from_account, to_account, amount, created_at)
           values (?, ?, ?, ?)
           -- Binding: [101, 202, 3000.00, '2026-04-26T14:30:00']

COMMIT;  -- TX-B
-- ALL locks released. TX-A can now proceed with its update.
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  SERIALIZABLE — Complete Protection (No Anomalies):                              │
│                                                                                  │
│  Time   TX-A (Another Transfer)            TX-B (Transfer — SERIALIZABLE)        │
│  ─────  ──────────────────────             ──────────────────────────────         │
│                                                                                  │
│  T1                                        BEGIN;                                │
│                                            SELECT ... FROM accounts              │
│                                            WHERE id = 101;                       │
│                                            -- balance = 10000                    │
│                                            -- S-Lock on row 101 ✅               │
│                                                                                  │
│  T2     BEGIN;                                                                   │
│         UPDATE accounts                                                          │
│         SET balance = balance - 5000                                             │
│         WHERE id = 101;                                                          │
│         ❌ BLOCKED! TX-B holds S-Lock on row 101!                                │
│         ⏳ Waiting for TX-B to commit/rollback...                                 │
│                                                                                  │
│  T3                                        UPDATE accounts                       │
│                                            SET balance = 7000                    │
│                                            WHERE id = 101;  -- 10000 - 3000     │
│                                            -- X-Lock on row 101                  │
│                                                                                  │
│  T4                                        UPDATE accounts                       │
│                                            SET balance = 8000                    │
│                                            WHERE id = 202;  -- 5000 + 3000      │
│                                                                                  │
│  T5                                        COMMIT; ✅                            │
│                                            -- All locks released                 │
│                                                                                  │
│  T6     -- TX-A UNBLOCKED!                                                       │
│         -- TX-A reads balance = 7000 (TX-B's committed value)                    │
│         -- TX-A: 7000 - 5000 = 2000 ✅ (correct!)                               │
│         UPDATE accounts SET balance = 2000 WHERE id = 101;                       │
│         COMMIT;                                                                  │
│                                                                                  │
│  WITHOUT SERIALIZABLE:                                                           │
│    TX-A might have read balance = 10000 (before TX-B committed)                  │
│    TX-A: 10000 - 5000 = 5000                                                    │
│    TX-B: 10000 - 3000 = 7000                                                    │
│    Last writer wins: balance = 5000 or 7000 (one transfer LOST!)                 │
│    This is the "lost update" problem. SERIALIZABLE prevents it.                  │
│                                                                                  │
│  ❌ Dirty Read:          PREVENTED                                               │
│  ❌ Non-Repeatable Read: PREVENTED                                               │
│  ❌ Phantom Read:        PREVENTED (range/gap locks block inserts)               │
│  ✅ Full Serial Execution Guarantee                                              │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │  PERFORMANCE WARNING:                                                      │  │
│  │                                                                            │  │
│  │  SERIALIZABLE dramatically reduces concurrency:                            │  │
│  │  • More lock contention → more waiting                                     │  │
│  │  • Higher chance of deadlocks                                              │  │
│  │  • Transactions may be rolled back due to serialization failures           │  │
│  │  • Should be used ONLY for critical operations (transfers, bookings)       │  │
│  │  • For 95% of operations, READ_COMMITTED is sufficient                     │  │
│  │                                                                            │  │
│  │  Rule of Thumb:                                                            │  │
│  │  • READ_COMMITTED:   General CRUD, most business operations                │  │
│  │  • REPEATABLE_READ:  Reports, calculations that read same data twice       │  │
│  │  • SERIALIZABLE:     Financial transfers, booking systems, critical writes │  │
│  │  • READ_UNCOMMITTED: Dashboard metrics, approximate analytics              │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

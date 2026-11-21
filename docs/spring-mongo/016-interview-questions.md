# 016 — Spring + MongoDB Interview Questions (Boot 4.1.0 + Java 25, MongoDB 8)

> Capstone Q&A for the Spring Mongo track: 24 questions, Junior → Senior.
> Each question has (1) interview-ready answer 2–4 sentences,
> (2) deeper explanation, (3) code/query where useful,
> (4) scenario/trade-off, (5) follow-up question.
> Stack: Spring Boot **4.1.0** · Java **25** · Spring Data MongoDB ·
> MongoDB **8** (replica set `rs0` / sharded cluster).
>
> Hub: [001 — Setup](./001-spring-boot-mongodb-setup.md) ·
> [002 — Document Mapping](./002-document-mapping.md) ·
> [003 — Repository Query Methods](./003-repository-query-methods.md) ·
> [004 — MongoTemplate CRUD](./004-mongotemplate-crud.md) ·
> [005 — Query Language](./005-mongodb-query-language.md) ·
> [006 — Data Modeling](./006-data-modeling-schema-design.md) ·
> [007 — Aggregation](./007-aggregation-framework.md) ·
> [008 — Indexing & Performance](./008-indexing-query-performance.md) ·
> [009 — Transactions & Concurrency](./009-transactions-concurrency.md) ·
> [010 — Replication & Sharding](./010-replication-sharding-advanced.md) ·
> [011 — Reactive, Testing & Debugging](./011-reactive-testing-debugging.md) ·
> [012 — Production Readiness](./012-production-readiness.md) ·
> [013 — Product Catalog Example](./013-product-catalog-example.md) ·
> [014 — Technology Comparisons](./014-technology-comparisons.md) ·
> [015 — System Design Scenarios](./015-system-design-scenarios.md) ·
> [Basics](./mongodb.md) · [Hub](./index.md)

## Table of Contents

- [How to Use This Guide](#how-to-use-this-guide)
- [Beginner (Q1–Q6)](#beginner)
- [Intermediate (Q7–Q14)](#intermediate)
- [Advanced/Senior (Q15–Q24)](#advancedsenior)
- [Cheat-Sheet Table](#cheat-sheet-table)
- [Resources](#resources)

## How to Use This Guide

- **Candidate:** memorise only the **Answer** (2–4 sentences).
> Use **Deeper** + **Code** when the interviewer says "go deeper".
- **Interviewer:** ask the **Scenario**, then the **Follow-up**.
- Tags `Scenario (required)` are the 12 mandatory scenario questions
> for this track — do not skip them in a loop.
- Conventions: `Instant` UTC, `BigDecimal` money (`Decimal128` on the wire),
> `com.commercemesh.*` packages. Snippets mirror repo style, not compiled.

---

### Beginner

Beginner questions test mental models: documents vs rows, BSON types, keys, embedding, basic CRUD, and knowing when MongoDB is the wrong tool.

#### Q1. What is MongoDB and when would you choose it?
**Answer (interview-ready):** MongoDB is a document database that stores BSON documents inside collections, with a flexible schema per document. You choose it when your domain is document-shaped, read-heavy, or rapidly evolving — catalogs, carts, events, CMS — and you want to co-locate related data instead of joining five tables. You avoid it as the system of record for heavily relational, multi-entity ACID reporting workloads.

**Deeper:** There are no tables or rows; a collection holds JSON-like documents with nested arrays and sub-documents. Flexible schema means the `products` collection can gain a `tags` field without `ALTER TABLE`, but the application must then enforce invariants via Bean Validation plus MongoDB schema validation. Availability comes from replica sets and horizontal scale from sharding. In Spring Boot you access it through `MongoRepository` for CRUD plus derived queries, or `MongoTemplate` for fine-grained queries, updates, aggregations.

```java
@Document("products")
public record Product(@Id String id, String name,
    BigDecimal price, List<String> tags) {}
```

```javascript
db.products.insertOne({ name: "Lamp", price: 49.99, tags: ["home"] });
```

**Scenario / Trade-off:** A greenfield catalog with weekly attribute changes and a 10:1 read/write ratio favours MongoDB on velocity and read locality. A double-entry ledger with auditor SQL across eight tables favours Postgres. Trade-off: development velocity plus locality versus relational integrity plus ad-hoc analytics.

**Follow-up:** What breaks first when you treat MongoDB like Postgres — normalising everything into tiny collections and joining in code?

#### Q2. BSON vs JSON — what is the difference and why does it matter?
**Answer (interview-ready):** JSON is a text interchange format; BSON is MongoDB's binary superset that adds types JSON lacks — `ObjectId`, `Date`, `Decimal128`, binary — plus length prefixes for fast traversal. It matters because storage, sorting, and driver mapping depend on BSON types, not on what the JSON "looked like". A price stored as double versus `Decimal128` sorts and rounds differently.

**Deeper:** BSON encodes each field's length so the server can skip without parsing, and it preserves type fidelity (`ISODate`, `NumberDecimal`, `BinData`). Spring Data's `MappingMongoConverter` maps `BigDecimal` to `Decimal128` and `Instant` to `Date` by default. Mixing types inside one field — `price` as a string in some documents and a number in others — breaks sorts, range filters, and aggregations in ways that look like "wrong results" rather than clean errors.

```java
@Field(targetType = FieldType.DECIMAL128)
private BigDecimal price; // exact money, never double

@Field(targetType = FieldType.DATE_TIME)
private Instant createdAt;
```

```javascript
// mongosh: inspect real types, don't trust pretty JSON
typeof db.products.findOne({ sku: "LAMP-01" }).price; // object (Decimal128)
```

**Scenario / Trade-off:** Prices stored as `double` accumulate errors such as `19.99 * 3 = 59.969999`; `Decimal128` is exact but slightly larger and needs explicit mapping. Rule: `Decimal128` for money, `Date` (never strings) for anything you range-query or sort.

**Follow-up:** A `createdAt` stored as `"2026-05-01"` string versus `ISODate` — which range query uses the index and why?

#### Q3. What is an ObjectId and how should you use it as `_id`?
**Answer (interview-ready):** `ObjectId` is MongoDB's default 12-byte primary key — 4 bytes timestamp plus 5 bytes random plus 3 bytes counter — globally unique without coordination and roughly time-ordered. Use it as `_id` unless you have a natural key such as SKU or tenant-plus-email that is immutable and unique. Never put business meaning into it beyond recency ordering.

**Deeper:** Because the leading bytes are seconds since the epoch, `ObjectId` values sort approximately by creation time, so the `_id` index doubles as a rough insertion timeline. The driver generates them client-side, so inserts need no key-allocation round-trip unlike sequences. In Spring Data map the id as `String` or `ObjectId` — both work. Monotonicity is not strict across clients, so never use `ObjectId` for gapless sequencing or invoice numbers.

```java
@Document("orders")
public class Order {
  @Id private ObjectId id; // or String id — driver generates either
  private Instant createdAt;
}
// Leave id null on new docs → driver fills ObjectId on save.
```

```javascript
ObjectId("681f3a2b9c1e5a3d4f000001").getTimestamp();
db.orders.find({ _id: { $gte: ObjectId.createFromTime(Date.now() / 1000 - 86400) } });
```

**Scenario / Trade-off:** "All orders created in the last hour, but there is no `createdAt` index" can still be answered by range-scanning `_id` during an incident. User-facing order numbers stay a separate human key; `_id` remains an opaque infrastructure key you never expose as a business identifier.

**Follow-up:** When would you replace `ObjectId` with a natural `_id` such as `sku`, and what shard-key implication follows from that choice?

#### Q4. Embed vs reference — how do you model one-to-many in MongoDB?
**Answer (interview-ready):** Embed when the child is always read with the parent, is small and bounded, and never needs independent querying — order lines inside a small order, addresses inside a user. Reference when the child is large, unbounded, shared, or queried alone — products referenced from orders, authors from books. The rule is access-pattern first, not normalisation first.

**Deeper:** Embedding gives single-round-trip reads and atomic single-document writes, but documents cap at 16 MB and large arrays force every update to rewrite the whole document. Referencing keeps documents small and avoids duplication, but pushes joins to the application via `$lookup` or a second query. The Spring convention is to embed value objects as sub-documents and to link entities with manual id fields rather than `@DBRef`, which costs an extra lazy query. See [006](./006-data-modeling-schema-design.md).

```java
// EMBED: bounded value objects, read together, written together
public record LineItem(String sku, int qty, BigDecimal price) {}

@Document("orders")
public record Order(@Id String id, List<LineItem> items) {}
```

```java
// REFERENCE: shared entity — store the id, not the whole product
@Document("orders")
public record OrderRef(@Id String id, List<String> productIds) {}
```

**Scenario (required) — 1000s-items order:** A B2B order routinely carries 2,000–5,000 line items and the team embedded them all. The document approaches 16 MB, every `qty` tweak rewrites megabytes, and the list page that only needs the summary still fetches everything. **Fix:** keep a summary in the order (`itemCount`, `total`, first N lines) and spill lines to an `order_lines` collection keyed `{ orderId, seq }`, or bucket lines at 100 per bucket document. Two-step reads beat unbounded-document blowup — always cap embedded arrays.

**Follow-up:** How do you enforce the cap — `$push` with `$each` plus `$slice`, or bucketing — and which approach preserves queryability?

#### Q5. CRUD basics — how do you create, read, update, and delete with Spring Data MongoDB?
**Answer (interview-ready):** Reads and simple writes go through `MongoRepository` (`save`, `findById`, derived finders); partial updates, upserts, and bulk work go through `MongoTemplate` (`updateFirst`, `upsert`, `bulkOps`). Deletes are `deleteById` for single documents and `remove(Query)` for filtered deletes. For partial updates never read-modify-write — use atomic `$set`/`$inc`.

**Deeper:** `save()` is a full-document upsert on `_id` — convenient but dangerous for concurrent partial edits because it replaces the whole document. `MongoTemplate.updateFirst(query, update, entityClass)` issues an atomic `$set` without loading the document. `upsert()` creates the document when absent, which suits idempotent consumers. Always scope writes with a `Query` and inspect `UpdateResult.getModifiedCount()` instead of assuming success.

```java
// READ via repository
Optional<Product> found = productRepo.findById(id);
```

```java
// PARTIAL UPDATE via template — no read-modify-write
Query q = Query.query(Criteria.where("_id").is(id));
Update u = new Update().set("price", newPrice).inc("stock", -1);
UpdateResult r = mongoTemplate.updateFirst(q, u, Product.class);
```

```java
// UPSERT for idempotent consumers
mongoTemplate.upsert(Query.query(Criteria.where("sku").is(sku)),
    new Update().setOnInsert("createdAt", Instant.now()).set("price", p),
    Product.class);
```

**Scenario / Trade-off:** Decrementing stock via `find` plus `save` loses writes under load; `$inc` is atomic. Full `save()` suits cold admin edits on uncontended documents; atomic operators are mandatory for hot counters and flags toggled by concurrent writers.

**Follow-up:** `save()` versus `updateFirst()` versus `upsert()` — which one overwrites concurrent fields, and how would you prove it with `@Version`?

#### Q6. MongoDB vs SQL — how do you decide?
**Answer (interview-ready):** Choose MongoDB when data is document-shaped, read-locality matters, schema evolves fast, and scale is horizontal — catalog, cart, feed, events. Choose SQL when invariants span many entities, joins and ad-hoc analytics dominate, and strict relational integrity is the product — money ledger, complex reporting. Most real systems use both.

**Deeper:** MongoDB offers flexible schema, nested documents, single-document atomicity plus scoped multi-document transactions, sharding-native scale, and a limited `$lookup` join. Postgres offers JOINs, window functions, constraints, serialisable transactions, and mature analytics. Operationally, MongoDB scales writes by adding shards while Postgres scales reads with replicas and writes vertically until you partition. See [006](./006-data-modeling-schema-design.md) and [014](./014-technology-comparisons.md).

```javascript
// MongoDB: one fetch, no join — the decision in a nutshell
db.orders.findOne({ _id: ObjectId("...") }); // items[] already inside
```

```sql
-- SQL: the same order is orders JOIN order_lines JOIN products
SELECT * FROM orders o
JOIN order_lines l ON l.order_id = o.id
JOIN products p ON p.sku = l.sku WHERE o.id = ?;
```

| Signal    | MongoDB                       | Postgres                    |
|-----------|-------------------------------|-----------------------------|
| Shape     | Nested doc, read together     | Flat rows, joined many ways |
| Schema    | Evolves weekly                | Stable, constrained         |
| TX        | Single-doc / scoped multi-doc | Cross-entity serialisable   |
| Analytics | Pipeline plus OLAP export     | Native SQL                  |

**Scenario (required) — Mongo-vs-Postgres:** A startup must pick the store for two services: (a) a product catalog with 50k SKUs, volatile attributes, and a 20:1 read ratio; (b) a payments ledger with double-entry postings and auditor SQL. **Decision:** (a) goes to MongoDB with embedded variants plus compound and text indexes and horizontal read scale; (b) goes to Postgres with foreign keys, `SERIALIZABLE`, and windowed reconciliation. Sharing one store forces either join-in-code pain or schema-volatility pain — polyglot is cheaper even with two operational footprints.

**Follow-up:** If the order lives in MongoDB and the payment lives in Postgres and both must stay consistent, do you use 2PC, outbox plus CDC, or eventual reconciliation — and why?

---

### Intermediate

Intermediate questions test working Spring Data skills: repositories versus templates, mapping, query styles, ESR indexing, `explain()`, aggregations, optimistic locking, and pagination at scale.

#### Q7. MongoRepository vs MongoTemplate — when do you use which?
**Answer (interview-ready):** `MongoRepository` is the declarative path — CRUD, paging, derived queries such as `findByStatusAndPriceLessThan`, and `@Query` cover roughly 80% of access code. `MongoTemplate` is the imperative path — dynamic `Criteria`, atomic `Update` operators, upserts, bulk operations, aggregations, and `explain()`. Use repositories for shape-stable access and the template at the seams where queries are built at runtime.

**Deeper:** Repository methods are proxied; custom behaviour belongs in a `…Custom` interface with a `…CustomImpl` class backed by `MongoTemplate`, which Spring wires automatically. `MongoTemplate` additionally exposes `bulkOps`, atomic `findAndModify` claims, and `aggregate()`. Test repositories with `@DataMongoTest` plus Testcontainers `mongo:8`; unit-test template logic by asserting on the built `Query`/`Update` documents. Boot 4.1.0 auto-configures both from `spring.data.mongodb.*`.

```java
public interface OrderRepo extends MongoRepository<Order, String> {
  List<Order> findByStatusAndTotalGreaterThan(String status, BigDecimal total);
}
```

```java
// Custom seam — Spring picks up *CustomImpl automatically
interface OrderRepoCustom { long bulkCancel(List<String> ids); }

class OrderRepoCustomImpl implements OrderRepoCustom {
  private final MongoTemplate template;
  public long bulkCancel(List<String> ids) {
    return template.updateMulti(
        Query.query(Criteria.where("_id").in(ids)),
        new Update().set("status", "CANCELLED"), Order.class)
        .getModifiedCount();
  }
}
```

**Scenario / Trade-off:** A search screen with six optional filters would need 2⁶ derived methods; one template method composing `Criteria` only for non-null filters stays linear. Conversely, a plain `findByCustomerId` needs no template ceremony — reaching for `MongoTemplate` there is over-engineering.

**Follow-up:** How does Spring discover the `Impl` class, and what changes when you need two templates backed by two databases?

#### Q8. How does @Document mapping work (id, field, converters, validation)?
**Answer (interview-ready):** `@Document("orders")` binds a class to a collection; `@Id` binds `_id`; `@Field("x")` renames a field; `@Indexed` and `@CompoundIndex` declare indexes; converters handle `BigDecimal` to `Decimal128` and `Instant` to `Date`; Jakarta Validation such as `@NotNull` runs on `save` when validation is wired. Mapping is convention-first and explicit where types or names diverge.

**Deeper:** `MappingMongoConverter` reads annotations at startup; mismatched stored types surface as conversion errors — the classic legacy-data failure after a type change. Custom converters such as `Converter<Money, Decimal128>` plug in via `MongoCustomConversions`. Prefer manual id links over `@DBRef`, which triggers an extra lazy query. `@Version Long` enables optimistic locking (Q13) and `@TimeToLive` auto-expires carts and sessions. Auditing annotations `@CreatedDate` / `@LastModifiedDate` fill timestamps. See [002](./002-document-mapping.md).

```java
@Document("products")
@CompoundIndex(name = "cat_price", def = "{ category: 1, price: 1 }")
public class Product {
  @Id private String id;
  @Indexed(unique = true) private String sku;
  @Field(targetType = FieldType.DECIMAL128) @NotNull private BigDecimal price;
  @Version private Long version;
  @CreatedDate private Instant createdAt;
}
```

**Scenario / Trade-off:** Legacy `price` stored as strings in 5% of documents breaks reads after adding a `BigDecimal` mapping. The fix is a migration script plus a reading converter that coerces both types during the transition — not catching conversion exceptions per document in a loop.

**Follow-up:** Where do you register a reading converter that accepts both string and `Decimal128` during a zero-downtime type migration?

#### Q9. Derived queries vs @Query — what are the limits of each?
**Answer (interview-ready):** Derived queries such as `findByStatusOrderByCreatedAtDesc` cover equality, ranges, `In`, `Like`, sorting, and paging with zero boilerplate, but they collapse past roughly three predicates or any dynamic filter. `@Query("{ status: ?0 }")` handles Mongo-native JSON — elemMatch, regex, `$or` — in one place. Anything dynamic, scored, or geo belongs in `MongoTemplate` and `Criteria`.

**Deeper:** The method-name parser supports `And` and `Or`, `LessThan` and `GreaterThan`, `Between`, `Containing`, `IgnoreCase`, `Top10`, and `Pageable`. It cannot express `$elemMatch` on nested arrays, `$text` scoring, or conditional predicates that depend on null arguments. `@Query` with `?0` placeholders keeps MongoDB syntax visible and supports `sort` and `fields` attributes for ordering and projections. The rule is derived queries for stable shapes, `@Query` for Mongo-native one-offs, and the template for dynamic composition. See [003](./003-repository-query-methods.md).

```java
// Derived — stable shape, Spring builds the query
Page<Order> findByCustomerIdAndStatus(String customerId, String status, Pageable p);
```

```java
// @Query — Mongo-native, elemMatch plus projection in one place
@Query(value = "{ 'items.sku': { $in: ?0 }, status: ?1 }",
       fields = "{ items: 1, total: 1 }")
List<Order> findWithSkus(List<String> skus, String status);
```

**Scenario / Trade-off:** An admin search with optional status, date, SKU, and price filters cannot be served by one derived method per combination, and a single `@Query` cannot skip null arguments either. A template builder that adds `Criteria` only for non-null filters is the maintainable answer — linear code for a combinatorial problem.

**Follow-up:** How do you add case-insensitive SKU search without a regex `COLLSCAN` — collation versus regex versus text index?

#### Q10. How do you choose and order a compound index (ESR rule)?
**Answer (interview-ready):** Order compound fields Equality → Sort → Range (ESR): equality fields first with the most selective leading, then the sort field, then range fields. A query efficiently uses only one index's prefix, so design the index for the query shape rather than per field. Verify with `explain()` that `totalKeysExamined` is close to `nReturned`.

**Deeper:** `{ customerId: 1, createdAt: -1, total: 1 }` serves `customerId = ? AND total > ? ORDER BY createdAt DESC` but cannot serve a bare `total > ?` query because of the prefix rule. Keeping the sort inside the index avoids the in-memory `SORT` stage, which caps at 32 MB and fails without `allowDiskUse`. One well-designed compound beats three single-field indexes that the planner cannot intersect efficiently. Re-check ESR after every new query shape ships. See [008](./008-indexing-query-performance.md).

```javascript
db.orders.createIndex({ customerId: 1, createdAt: -1, total: 1 }); // E, S, R
```

```javascript
db.orders.find({ customerId: "c1", total: { $gt: 100 } })
  .sort({ createdAt: -1 }).explain("executionStats");
// want IXSCAN with keysExamined ≈ returned
```

```java
@CompoundIndex(name = "cust_created_total",
  def = "{ customerId: 1, createdAt: -1, total: 1 }")
```

**Scenario (required) — compound index choice:** The workload has three shapes: (a) a `{ customerId, status }` list, (b) `{ customerId }` plus a date range plus sort, and (c) a global `{ status }` dashboard. **Choice:** `{ customerId: 1, status: 1, createdAt: -1 }` serves (a) exactly and (b) via its prefix plus an in-index sort; (c) needs a separate `{ status: 1, createdAt: -1 }` because it has a different leading equality. A single "cover-all" `{ status, customerId, createdAt }` serves neither prefix cleanly. Measure `keysExamined / returned` per shape instead of guessing.

**Follow-up:** Why does putting the range field before the sort field force an in-memory sort, and what does `explain()` show when that happens?

#### Q11. How do you diagnose a slow query — IXSCAN vs COLLSCAN?
**Answer (interview-ready):** Run `explain("executionStats")`: `COLLSCAN` means no usable index and a full collection scan; `IXSCAN` with `totalDocsExamined` far above `nReturned` means the index is not selective or a FETCH filter discards most keys. Fix in order: rewrite the shape with a projection, limit, and equality, then add or adjust the ESR compound, then check the profiler for recurrence — not hardware.

**Deeper:** The fields that matter are `winningPlan.stage`, `totalKeysExamined`, `totalDocsExamined`, `executionTimeMillis`, and any `SORT` stage indicating a sort without an index. A `docsExamined / returned` ratio above 10 is the FETCH smell; `keysExamined / returned` above 10 is the low-selectivity smell. Enable the profiler with `slowms: 100` to catch production shapes, and in Spring issue `explain` through a `Document` command. Correlate with deploy markers — new code ships new shapes. See [008](./008-indexing-query-performance.md) and [012](./012-production-readiness.md).

```javascript
db.orders.find({ status: "SHIPPED" }).sort({ createdAt: -1 })
  .explain("executionStats");
// BAD:  { stage: "COLLSCAN", docsExamined: 2000000, time: 1800ms }
// GOOD: { stage: "IXSCAN", indexName: "status_created",
//         keysExamined: 50, docsExamined: 50 }
```

```javascript
db.setProfilingLevel(1, { slowms: 100 });
db.system.profile.find({ millis: { $gt: 300 } }).sort({ ts: -1 }).limit(5);
```

**Scenario (required) — slow query:** Latency p95 on `GET /orders?status=&from=` jumps from 200 ms to 3 s after a promo doubles volume. `explain()` shows a `COLLSCAN` over 2 M documents because someone removed the `status` predicate default so the UI now means "show all". **Fix:** restore a required equality with a sensible default, add `{ status: 1, createdAt: -1 }`, cap the page size, and project only list fields. The trade-off is honest: a "show everything" UX versus a paged-and-filtered contract — unbounded queries are the incident, not the data growth.

**Follow-up:** `IXSCAN` but still slow, with `docsExamined` at 100× returned — what FETCH pattern causes it and which two fixes apply?

#### Q12. How do you build $group / $lookup aggregations in Spring?
**Answer (interview-ready):** `$group` reshapes many documents into summaries such as revenue per category; `$lookup` left-joins another collection such as order plus customer. In Spring use `Aggregation.newAggregation(match, group, lookup, project, sort)` with `mongoTemplate.aggregate(...)`. Always `$match` and `$project` early so later stages process fewer, narrower documents.

**Deeper:** Pipeline order is performance: a selective `$match` first so indexes apply, `$group` on indexed keys, `$lookup` late against a small cardinality, and `$limit` before `$lookup` whenever possible. A `$lookup` without an index on the foreign field becomes a nested-loop disaster. For large reports set `allowDiskUse(true)` deliberately — never as a default that masks a missing early `$match`. See [007](./007-aggregation-framework.md).

```java
Aggregation agg = Aggregation.newAggregation(
  Aggregation.match(Criteria.where("status").is("PAID")),
  Aggregation.group("category").sum("total").as("revenue").count().as("n"),
  Aggregation.sort(Sort.by(Sort.Direction.DESC, "revenue")),
  Aggregation.limit(10));
```

```java
List<Document> top =
  mongoTemplate.aggregate(agg, "orders", Document.class).getMappedResults();
```

```javascript
db.orders.aggregate([
  { $match: { status: "PAID" } },
  { $lookup: { from: "customers", localField: "customerId",
               foreignField: "_id", as: "c" } },
  { $unwind: "$c" },
  { $project: { total: 1, "c.email": 1 } }
]);
```

**Scenario / Trade-off:** A dashboard showing "revenue by category, last 90 days" rescans 5 M documents per page view. Move a `$match` on indexed `createdAt` to the front and pre-aggregate hourly into a `sales_daily` collection with a scheduled pipeline; the dashboard then reads the summary. The trade-off is minutes-stale data versus a per-request full scan — pre-aggregation wins for every dashboard with a refresh SLA above seconds.

**Follow-up:** When does `$lookup` beat an application-side join, and when does denormalisation beat both?

#### Q13. How does @Version prevent lost updates?
**Answer (interview-ready):** `@Version Long` adds optimistic locking: every update matches on `_id AND version = N` and increments the version; a concurrent writer with a stale version matches zero documents and Spring throws `OptimisticLockingFailureException`. The loser retries with fresh state instead of silently overwriting. No locks and no replica-set transaction are needed.

**Deeper:** Versioning works for `save()` and for versioned template updates; raw bulk operations and raw driver writes bypass it unless you include the version predicate manually. Retry with backoff — Spring Retry `@Retryable` with a maximum of about three attempts — for rarely contended documents. For true counters prefer an atomic `$inc`, which needs no version at all. The version field must be `Long` or `Integer` and must never be reset manually. See [009](./009-transactions-concurrency.md).

```java
@Document("products")
public class Product {
  @Id String id; BigDecimal price; @Version Long version;
}
```

```java
// A reads v3, B reads v3 → A saves (v3→v4 OK) → B saves →
// 0 matched → OptimisticLockingFailureException
@Retryable(value = OptimisticLockingFailureException.class,
  maxAttempts = 3, backoff = @Backoff(delay = 100))
public Product rename(String id, String name) {
  Product p = repo.findById(id).orElseThrow();
  p.setName(name);
  return repo.save(p);
}
```

**Scenario (required) — lost updates:** Two support agents apply a refund and an address change to the same order through read-modify-`save()`; the second save wipes out the first. The root cause is a full-document replace with no version check. **Fix:** add `@Version`, catch the failure, re-read, re-apply, and retry with jitter up to three times. Optimistic locking wins when conflicts are rare; atomic `$inc` or a transaction wins when conflicts are the norm rather than the exception.

**Follow-up:** What breaks when you mix versioned `save()` and unversioned `updateFirst()` against the same collection?

#### Q14. Offset vs cursor pagination — which one scales?
**Answer (interview-ready):** Offset (`page=40, size=20` becomes `skip(800)`) is simple but scans and discards everything skipped — latency grows linearly and concurrent inserts shift pages, causing duplicates and skips. Cursor or keyset pagination (`createdAt` plus `_id` less than the last seen) seeks directly through the index in O(page size) and stays stable under inserts. Use offset for small admin tables and cursor for feeds, infinite scroll, and exports.

**Deeper:** `skip(500_000)` still walks 500k index keys even though it returns 20 documents. The standard cursor is the composite `(createdAt, _id)` — unique and monotonic, so ties on timestamps resolve deterministically. In Spring, `Pageable` and `Slice` serve offset pagination while cursor pagination is a manual `Criteria` plus `limit(20)`. Export jobs must persist the cursor after each batch so a crash resumes instead of restarting. See [008](./008-indexing-query-performance.md).

```java
// OFFSET — fine for pages 1–10, poison past 10k
Page<Order> page = repo.findByStatus("PAID",
  PageRequest.of(400, 20, Sort.by("createdAt").descending()));
```

```java
// CURSOR — stable, index-seeking
Query q = Query.query(Criteria.where("status").is("PAID").orOperator(
    Criteria.where("createdAt").lt(lastSeen),
    Criteria.where("createdAt").is(lastSeen).and("_id").lt(lastId)))
  .with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("_id")))
  .limit(20);
```

**Scenario (required) — cursor pagination:** A mobile feed built on `skip`-based pages gets slower with every page and shows duplicate items when new orders arrive mid-scroll. **Fix:** return an opaque Base64 `nextCursor(createdAt, _id)` with each page; the client passes it back and the server seeks instead of skipping. The trade-off is no "jump to page 47" — feeds never need random access, while admin tables keep bounded offset with tight page-size limits.

**Follow-up:** How do you encode the cursor opaquely and handle filter or sort-direction changes between requests?

---

### Advanced/Senior

Senior questions test production judgement: transactions, durability tuning, failover, sharding, hot keys, streams, reactive trade-offs, pools, migrations, and structured outage debugging on MongoDB 8.

#### Q15. Transactions in MongoDB — what is the replica-set prerequisite and when do you avoid TX?
**Answer (interview-ready):** Multi-document ACID transactions require a replica set — even a single node for development — because they ride on the oplog, logical sessions, and snapshot reads, which a standalone `mongod` lacks. In Spring you add `@Transactional` plus `MongoTransactionManager`, keep transactions short, and retry transient errors. Avoid transactions whenever single-document atomicity or embedding already suffices — transactions run 2–5× slower and invite conflicts.

**Deeper:** A standalone server rejects `startTransaction` with "transaction numbers are only allowed on a replica set member". The development fix is `mongod --replSet rs0` plus `rs.initiate()`, or Testcontainers `mongo:8` with replica-set initialisation; production needs at least three nodes. Keep transactions under roughly 100 documents and 5 seconds, set `maxTransactionTime`, and never perform HTTP calls inside. Retry the whole transaction on `WriteConflict` and transient labels — never single statements in isolation. See [009](./009-transactions-concurrency.md).

```java
@Bean
MongoTransactionManager transactionManager(MongoDatabaseFactory f) {
  return new MongoTransactionManager(f); // requires a replica set
}
```

```java
@Transactional // short, no I/O inside
public void checkout(String orderId) {
  mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(orderId)),
    new Update().set("status", "PAID"), Order.class);
  mongoTemplate.updateFirst(Query.query(Criteria.where("orderId").is(orderId)),
    new Update().set("state", "RESERVED"), Inventory.class);
}
```

```bash
mongod --replSet rs0 --dbpath /data/db &
mongosh --eval 'rs.initiate()'
```

**Scenario (required) — TX-vs-embed:** Checkout writes `orders`, `inventory`, and `ledger_entries` in one `@Transactional` method and times out with `WriteConflict` errors under promo load. **Redesign:** embed the reservation inside the order document as one atomic write (`status` plus `reservedUntil`), emit an `OrderPaid` event through a transactional outbox, and let inventory and ledger consume it idempotently. Keep a transaction only for the money move that truly spans documents, shrink it to two updates, and retry with backoff. Choreography complexity beats a contended distributed transaction.

**Follow-up:** Which retry label distinguishes a safe whole-transaction retry from an application bug you must not retry?

#### Q16. Read/write concerns — how do you tune durability vs availability?
**Answer (interview-ready):** Write concern (`w:1`, `majority`, `all`, `j:true`) controls how many replicas must acknowledge before success; read concern (`local`, `majority`, `snapshot`) controls which committed state you are allowed to see. Money paths use `w:majority` with `majority` or `snapshot` reads; analytics and caches accept `w:1` with `local`. Stronger concerns cost latency and fail during partitions — so tune per operation, not globally.

**Deeper:** `w:majority` survives the loss of a primary; `w:1` can roll back after a failover, which is never acceptable for money. `snapshot` inside a transaction gives a consistent view while `local` may read data that later rolls back. `readPreference` — where to read — is orthogonal to concern — how durable the read is. Configure concerns per template or per operation and watch `writeConcernMajorityJournalLatency` for journal pressure. See [010](./010-replication-sharding-advanced.md).

```java
// Money path: durable writes, consistent reads
mongoTemplate.setWriteConcern(WriteConcern.MAJORITY);
mongoTemplate.updateFirst(query,
  new Update().set("status", "PAID"), Order.class);
```

```javascript
db.orders.insertOne({ sku: "A" },
  { writeConcern: { w: "majority", j: true, wtimeout: 3000 } });
```

```yaml
# Baseline in config; override per operation, not the reverse
# spring.data.mongodb.write-concern: majority
```

**Scenario (required) — duplicate keys:** Order intake retries after timeouts create duplicate `POST /orders` documents because each retry mints a new id and `save()` happily inserts again. **Fix:** make the idempotency key a unique index on `clientRequestId` and `upsert` on that key with `w:majority`; catch `DuplicateKeyException` and return the existing document instead of inserting. One unique index's write cost beats double charges — and never "check-then-insert", whose race window duplicates anyway under a retry storm.

**Follow-up:** Check-then-insert versus unique-index-upsert under a 500-rps retry storm — which one duplicates, and what exception and status contract do you expose to callers?

#### Q17. Primary failover — what happens second by second and what must the app do?
**Answer (interview-ready):** Missed heartbeats trigger an election — typically 5–12 seconds — and a caught-up secondary becomes primary; drivers block in server selection until `serverSelectionTimeoutMS` and then retarget. The app must fail fast with a 5-second timeout instead of the 30-second default, let retryable writes re-fire idempotent operations once, open the circuit breaker into degraded reads, and shed traffic through a `ping`-based readiness probe. Money writes pause honestly while browsing degrades to cache.

**Deeper:** In-flight writes on the old primary may roll back — which is why money uses `w:majority`. `retryWrites=true` retries once automatically but is only safe for idempotent single-statement operations. Money reads pin to `primary` and never accept stale secondary data at checkout; analytics accept `secondaryPreferred` with `maxStalenessSeconds`. Gate traffic with readiness rather than liveness, drill failover quarterly in staging, and alert on election counts plus rollback files. See [010](./010-replication-sharding-advanced.md) and [012](./012-production-readiness.md).

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://h1,h2,h3/shop?replicaSet=rs0&retryWrites=true
      server-selection-timeout: 5s # fail fast into breaker, not thread pool
      timeout: 3s
```

```java
// Readiness: ping mongo, not just "context is up"
@Component
class MongoReady implements ReactiveHealthIndicator {
  public Mono<Health> health() {
    // run { ping: 1 }; DOWN on failure so k8s sheds traffic
    return null;
  }
}
```

**Scenario (required) — primary failover:** The primary dies at peak: threads pile up for the full 30-second default, manual retries double-submit checkout, and browse returns 500s. **Fix:** a 5-second selection timeout, a breaker at 50% over 50 calls serving stale catalog plus honest checkout errors that preserve idempotency keys, `ping` readiness shedding, and a retry budget of driver-once plus app-once with jitter. The post-mortem covers election duration, rollback documents, and whether any money path read from a secondary. Brief honest unavailability beats phantom "successes" that double-charge.

**Follow-up:** Why is `retryWrites=true` unsafe for a non-idempotent `findAndModify`-claimed job queue, and which pattern replaces it?

#### Q18. How do you choose a shard key?
**Answer (interview-ready):** A good shard key has high cardinality, an even write distribution, and covers the dominant query's equality — `customerId`, `tenantId` plus `createdAt`, or a hashed high-cardinality id. The key is immutable, so a wrong choice means resharding pain. Never shard on a monotonic-only key such as `ObjectId` or `createdAt`, or on low-cardinality `status` alone — one chunk then absorbs all writes.

**Deeper:** Ranged keys preserve locality so range queries stay fast but inserts run hot; hashed keys spread writes while point reads scatter unless the key is in the filter. Compound keys such as `{ tenantId: 1, orderId: 1 }` isolate tenants and keep each tenant's orders together. Every query should include the shard key or it scatter-gathers across shards. Pre-split chunks and schedule balancer windows for bulk loads; validate the choice with `sh.status()` chunk distribution plus `explain()` shard targeting. See [010](./010-replication-sharding-advanced.md).

```javascript
sh.enableSharding("shop");
sh.shardCollection("shop.orders", { customerId: 1, _id: 1 }); // ranged, tenant-local
// write-spread alternative:
// sh.shardCollection("shop.events", { eventId: "hashed" });
db.orders.find({ customerId: "c1" }).explain(); // want single-shard target
```

**Scenario (required) — multi-tenant:** A SaaS with three whale tenants at 70% of writes and 2,000 minnows shards on `tenantId` alone — whale chunks pin two shards while the rest idle, and minnow queries scatter. **Fix:** a compound `{ tenantId: 1, _id: 1 }` key, or a hashed suffix, plus zone sharding that pins whales to dedicated shards with pre-split whale ranges. Slightly fanned minnow reads beat whale-hotspot collapse; add quotas and per-tenant metrics to catch the next whale before it pins the cluster.

**Follow-up:** Ranged versus hashed on `{ customerId }` for (a) per-customer order-history pages against (b) 50k-wps global ingest — which wins each case and why?

#### Q19. Hot shards / hot documents — how do you detect and fix them?
**Answer (interview-ready):** Detect heat through per-shard operation counters (`mongostat`, Atlas shard metrics), chunk imbalance in `sh.status()`, and `writeConflicts` plus WiredTiger dirty-cache pressure on one primary. Fix it by spreading the heat: a hashed or compound shard key, pre-splitting, counter striping across N documents with a `$group` sum on read, and Redis buffering for flash spikes. Never start with a bigger primary — heat follows the key.

**Deeper:** A single document absorbing 10k `$inc` operations per second serialises on the document lock and the oplog — no instance size raises that ceiling. Striping writes each increment to a random one of 16 `counter_stripes{key, stripe, n}` documents and sums on read. Cap event arrays with `$push` plus `$slice` and overflow into a cold collection. The balancer cannot fix intra-chunk heat — only a key redesign does. Verify by watching shard op/s converge and conflicts fall toward zero. See [010](./010-replication-sharding-advanced.md) and [012](./012-production-readiness.md).

```java
// Striped counter: spread writes, sum on read
int stripe = ThreadLocalRandom.current().nextInt(16);
mongoTemplate.upsert(
  Query.query(Criteria.where("key").is(key).and("stripe").is(stripe)),
  new Update().inc("n", 1), "counter_stripes");
```

```java
// Read path tolerates ~1s staleness; reconcile exactly offline
Aggregation sum = Aggregation.newAggregation(
  Aggregation.match(Criteria.where("key").is(key)),
  Aggregation.group().sum("n").as("total"));
```

**Scenario / Trade-off:** A flash-sale counter on one `products` document stalls the primary at 8k rps. Stripe it over 16 documents with a one-second Redis buffer and periodic flush; reads accept roughly one second of staleness. An exact real-time single counter versus an available checkout — pick availability and reconcile exact counts offline.

**Follow-up:** How do you size the stripe count, and which read-path regression proves you over-striped?

#### Q20. Change streams — what are they and how do you consume them in Spring?
**Answer (interview-ready):** Change streams are MongoDB's ordered, resumable oplog feed of `insert`, `update`, and `delete` events per collection, database, or cluster, and they require a replica set. In Spring you subscribe through a `MessageListenerContainer` or a reactive `ChangeStreamRequest`, persist the `resumeToken`, request full-document lookup for updates, and keep handlers idempotent. Use them for cache invalidation, outbox relay, and search-index sync — not as a general message bus.

**Deeper:** Delivery is at-least-once: persist the token only after processing, handle `invalidate` events, and expect duplicates across failovers. `fullDocument: UPDATE_LOOKUP` costs an extra read, so filter to the operations you need. Scale with one stream per shard boundary and alert on `clusterTime` drift. For cross-service contracts prefer the outbox-collection pattern — one explicitly schematised collection tailed by a single relay — over tailing every domain collection. See [015](./015-system-design-scenarios.md) for relay topologies and [014](./014-technology-comparisons.md) for bus trade-offs.

```java
container.register(new ChangeStreamRequest<>(
  MessageListenerContainer.changeStream("orders",
    ChangeStreamOptions.builder()
      .filter(Aggregation.match(
        Criteria.where("operationType").is("insert"))).build(),
    Order.class),
  (ChangeStreamDocument<Order> doc) -> {
    // idempotent: upsert search index by doc.getBody().getId()
  }), Order.class);
```

**Scenario / Trade-off:** A search index lags ten minutes because a per-doc REST call runs sequentially inside the listener. Batch to 500 documents or a two-second window, parallelise by shard-key hash, and checkpoint tokens per batch. Per-key parallelism preserves correctness at speed; global ordering does not scale.

**Follow-up:** The resume token is lost after a three-day outage that exceeds the oplog window — full re-sync versus timestamp-seeded replay, and how do you detect the gap?

#### Q21. Reactive MongoDB — when does WebFlux + ReactiveMongo pay off and what are the trade-offs?
**Answer (interview-ready):** Reactive pays when you fan out to many slow backends — MongoDB plus three HTTP calls — on few threads with backpressure, as in gateways and BFFs holding 10k-plus mostly-idle connections. It costs debuggability through async stack traces, fiddly transaction and session ergonomics, and team fluency. CRUD services with simple MongoDB access stay on MVC plus virtual threads on Java 25 — the same throughput with far simpler code.

**Deeper:** `ReactiveMongoRepository` and `ReactiveMongoTemplate` return `Mono` and `Flux`; any blocking call inside — `block()` or a JDBC repository — collapses the event loop. Reactive transactions exist but session-passing is awkward. Virtual threads via `spring.threads.virtual.enabled=true` give MVC thread-per-request semantics at reactive-like scale without the operator soup. Mix freely: reactive at the fan-out edge, MVC everywhere else. See [011](./011-reactive-testing-debugging.md).

```java
// Reactive: fan-out with bounded concurrency plus backpressure
Flux<Order> feed(ReactiveMongoTemplate t, List<String> ids) {
  return Flux.fromIterable(ids)
    .flatMap(id -> t.findById(id, Order.class), 64);
}
```

```yaml
# Default choice for CRUD on Java 25:
# spring.threads.virtual.enabled: true  # plain MongoRepository, simple traces
```

**Scenario / Trade-off:** A BFF aggregating catalog, pricing, and reviews per request exhausts 200 platform threads at 5k concurrent. The options are (a) a full WebFlux rewrite or (b) MVC plus virtual threads with concurrent fan-out. Pick (b) unless sustained connections above roughly 20k idle demand (a) — measure thread-wait versus latency first, because reactive rewrites are never free in debugging or hiring cost.

**Follow-up:** What happens when someone calls `.block()` or a blocking JPA repository inside a `flatMap`, and which metric exposes it first?

#### Q22. Connection pool tuning — how do you size it and survive failures?
**Answer (interview-ready):** Size by Little's law: pool size ≈ peak RPS × p95 latency in seconds plus headroom — for example 500 × 0.05 = 25, so configure 50. Keep `minPool` warm for bursts and `waitQueueTimeout` short so failures surface fast. Survive outages with a 5-second selection timeout, bounded retries with jitter, a breaker plus fallback, and a `ping` readiness probe — never infinite waits or unbounded pools.

**Deeper:** There is one pool per singleton `MongoClient` — never a client per request. Raising `maxPoolSize` to 500 does not fix slow queries; it moves the queue into `mongod` and melts it. The knobs that matter are `maxPoolSize`, `minPoolSize`, `maxIdleTimeMS`, `waitQueueTimeoutMS`, `serverSelectionTimeoutMS`, and heartbeat frequency. Observe checkout latency, wait-queue depth, and pool utilisation, and alert before saturation. Count pods times pool against Atlas caps rather than sizing one pod alone. See [001](./001-spring-boot-mongodb-setup.md) and [012](./012-production-readiness.md).

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://app:***@h1,h2,h3/shop?maxPoolSize=50&minPoolSize=10
        &maxIdleTimeMS=30000&waitQueueTimeoutMS=2000
        &serverSelectionTimeoutMS=5000&retryWrites=true
```

```java
// Singleton client; prefer typed settings over URI string-soup when complex
@Bean
MongoClientSettingsBuilderCustomizer pool() {
  return b -> b.applyToConnectionPoolSettings(p -> p.maxSize(50).minSize(10)
    .maxWaitTime(2, java.util.concurrent.TimeUnit.SECONDS));
}
```

**Scenario (required) — connection failure:** A rolling restart storms the primary with pods-times-50 new connections; `waitQueueTimeout` fires, checkout p99 reaches 8 seconds, and the failure cascades. **Fix:** stagger the rollout with `maxUnavailable: 1`, keep `minPool` warm, right-size `maxPoolSize` from RPS-times-latency math (roughly 30 here), set a 2-second wait timeout into the breaker with a cached fallback, and pre-warm one pod before moving to the next. A smaller pool with fast failure beats a big pool with a slow queue and a full outage.

**Follow-up:** Checkout latency spikes while database latency stays flat — is the pool too small or leaking through unclosed cursors or per-request clients, and which two metrics distinguish the causes?

#### Q23. Zero-downtime schema migration — how do you rename a field or change a type on millions of docs?
**Answer (interview-ready):** Follow expand → migrate → contract: first dual-write and dual-read so new code handles both `addr` and `address`; then backfill in throttled unordered bulks of 500–1,000 off-peak with a resumable cursor; then flip reads to the new field, stop old writes, and drop the old field later. Type changes add a coercing reading-converter during the window. Never run stop-the-world rewrites.

**Deeper:** Backfill with unordered `bulkOps` plus pauses, checkpoint the `_id` cursor for resume, and use throttled `w:majority` — or `w:1` scratch writes plus a verify pass. Dual-read through `@Field` plus a custom getter or a reading converter; validate with sampled parity checks such as `$type` stray counts and missing-field counts. Build the new index with a rolling build before cutover. Rollback is a flag flip because the old field stays populated throughout. See [002](./002-document-mapping.md) and [012](./012-production-readiness.md).

```java
// Phase 1: dual-read entity (old field removed only in Phase 3)
@Document("customers")
public class Customer {
  @Field("address") private Address address;       // new
  @Field("addr") private Address legacyAddr;        // old
  public Address getAddress() {
    return address != null ? address : legacyAddr;
  }
}
```

```java
// Phase 2: throttled backfill — page by _id cursor, bulk, sleep, repeat
BulkOperations bulk =
  mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Customer.class);
page.forEach(c -> bulk.updateOne(
  Query.query(Criteria.where("_id").is(c.getId())),
  new Update().set("address", c.getLegacyAddr())));
bulk.execute();
Thread.sleep(200);
```

```javascript
// Parity audit: how many docs still lack the new field or old type?
db.customers.countDocuments({ address: { $exists: false } });
db.products.countDocuments({ price: { $type: "string" } });
```

**Scenario (required) — zero-downtime migration:** Rename `addr` to `address` across 8 M customers with no maintenance window; a naive `updateMany` floods the oplog, lags secondaries by 40 seconds, and serves stale reads at checkout. **Plan:** ship dual-read in deploy one, backfill 1k batches with 200 ms pauses overnight plus cursor checkpoints that resume on kill, build the new index with a rolling build, flip reads per tenant with the whale last, and drop the old field next sprint. Three deploys plus temporary dual storage beat one risky big-bang.

**Follow-up:** The backfill crashes at 60% — how does the `_id` cursor checkpoint make resume O(remaining) instead of restarting, and how do you prove parity before contract?

#### Q24. Production outage debugging — MongoDB p99 spikes, where do you look first?
**Answer (interview-ready):** Work top-down with no guessing: application timers by collection and command to see which shape moved, then `db.currentOp()` for live hogs, the profiler and slow log for repeat shapes, `explain()` for regressed plans, `$indexStats` for missing or unused indexes, pool checkout versus database time for queueing, WiredTiger evictions and dirty ratio plus disk await for working-set pressure, and replication lag for stale reads. Fix in that order and scale hardware last.

**Deeper:** Kill runaway operations with `db.killOp` before deep-diving, then correlate deploy markers — new code ships new query shapes that miss indexes. Check balancer chunk moves, primary elections, and connection storms sized as deploys times pool. Keep a runbook with the RED dashboard link, the slow top-10 query, an `explain` template, kill and rollback permissions, and breaker state. Every incident closes with an index audit plus an alert-threshold action. See [011](./011-reactive-testing-debugging.md) and [012](./012-production-readiness.md).

```javascript
// Live hogs — killOp first, analyse second
db.currentOp({ active: true, secs_running: { $gt: 5 } });
```

```javascript
db.system.profile.find({ millis: { $gt: 300 } }).sort({ ts: -1 }).limit(10);
db.orders.aggregate([{ $indexStats: {} }, { $sort: { "accesses.ops": -1 } }]);
```

```java
// App side: tag command timers so the dashboard names the shape
// micrometer metric: mongodb.commands{collection, command} → p50/p95/p99
```

**Scenario / Trade-off:** Catalog p99 jumps from 40 ms to 900 ms for 20 minutes with no deploy. The trail shows a bi-hourly analytics aggregate with `allowDiskUse:true` scanning `orders` without an early `$match`, evicting the working set at 25k evictions per second. **Fix:** move analytics to a secondary with `maxTimeMS`, add the early `$match` on indexed `createdAt`, and schedule it off-peak. Slightly stale analytics on a secondary beat OLTP cache eviction — isolate workloads instead of sharing the primary's cache.

**Follow-up:** How do you prove the cause was working-set eviction rather than a missing index — which two WiredTiger plus `explain` signals diverge?

---

## Cheat-Sheet Table

| # | Question | Level | Key idea | Deep-dive |
|---|---|---|---|---|
| Q1 | What is MongoDB | Beginner | Document DB, flexible schema | [006](./006-data-modeling-schema-design.md) |
| Q2 | BSON vs JSON | Beginner | Binary types, Decimal128 | [002](./002-document-mapping.md) |
| Q3 | ObjectId | Beginner | 12-byte, time-ordered | [002](./002-document-mapping.md) |
| Q4 | Embed vs ref (1000s-items order) | Beginner `Scenario (required)` | Bounded embed, bucket overflow | [006](./006-data-modeling-schema-design.md) |
| Q5 | CRUD basics | Beginner | Template for partial/bulk | [004](./004-mongotemplate-crud.md) |
| Q6 | Mongo-vs-Postgres | Beginner `Scenario (required)` | Polyglot by shape | [014](./014-technology-comparisons.md) |
| Q7 | Repository vs Template | Intermediate | Declarative vs imperative seam | [003](./003-repository-query-methods.md) |
| Q8 | @Document mapping | Intermediate | Converters, validation | [002](./002-document-mapping.md) |
| Q9 | Derived + @Query | Intermediate | 3-predicate cliff | [003](./003-repository-query-methods.md) |
| Q10 | Compound index + ESR (choice) | Intermediate `Scenario (required)` | Equality→Sort→Range | [008](./008-indexing-query-performance.md) |
| Q11 | explain slow query | Intermediate `Scenario (required)` | COLLSCAN vs FETCH smell | [008](./008-indexing-query-performance.md) |
| Q12 | $group/$lookup | Intermediate | Match-early, lookup-late | [007](./007-aggregation-framework.md) |
| Q13 | @Version lost updates | Intermediate `Scenario (required)` | Optimistic retry | [009](./009-transactions-concurrency.md) |
| Q14 | Cursor pagination | Intermediate `Scenario (required)` | (createdAt,_id) seek | [008](./008-indexing-query-performance.md) |
| Q15 | TX + replica-set, TX vs embed | Advanced `Scenario (required)` | Shrink TX, outbox | [009](./009-transactions-concurrency.md) |
| Q16 | Read/write concerns (dup keys) | Advanced `Scenario (required)` | majority + idempotency upsert | [010](./010-replication-sharding-advanced.md) |
| Q17 | Primary failover | Advanced `Scenario (required)` | 5s + breaker + ping | [010](./010-replication-sharding-advanced.md) |
| Q18 | Shard-key + multi-tenant | Advanced `Scenario (required)` | Compound, zones | [010](./010-replication-sharding-advanced.md) |
| Q19 | Hot shards / hot docs | Advanced | Striping, pre-split | [012](./012-production-readiness.md) |
| Q20 | Change streams | Advanced | Resume token, idempotent | [015](./015-system-design-scenarios.md) |
| Q21 | Reactive trade-offs | Advanced | MVC+virtual vs WebFlux | [011](./011-reactive-testing-debugging.md) |
| Q22 | Pool + connection failure | Advanced `Scenario (required)` | RPS×latency sizing | [001](./001-spring-boot-mongodb-setup.md) |
| Q23 | Zero-downtime migration | Advanced `Scenario (required)` | Expand–migrate–contract | [012](./012-production-readiness.md) |
| Q24 | Outage debugging | Advanced | Top-down runbook | [011](./011-reactive-testing-debugging.md) |

---

## Resources

**This track (001–015 hub):** [001 — Setup](./001-spring-boot-mongodb-setup.md) (pool, timeouts, Atlas) · [002 — Document Mapping](./002-document-mapping.md) (converters, `@Version`) · [003 — Repository Query Methods](./003-repository-query-methods.md) (derived, `@Query`) · [004 — MongoTemplate CRUD](./004-mongotemplate-crud.md) (updates, bulk) · [005 — Query Language](./005-mongodb-query-language.md) (Criteria, operators) · [006 — Data Modeling](./006-data-modeling-schema-design.md) (embed vs ref, buckets) · [007 — Aggregation](./007-aggregation-framework.md) (`$group`, `$lookup`) · [008 — Indexing & Performance](./008-indexing-query-performance.md) (ESR, `explain`, profiler) · [009 — Transactions](./009-transactions-concurrency.md) (replica-set TX, `@Version`) · [010 — Replication & Sharding](./010-replication-sharding-advanced.md) (elections, shard keys) · [011 — Reactive, Testing & Debugging](./011-reactive-testing-debugging.md) (Testcontainers, `currentOp`) · [012 — Production Readiness](./012-production-readiness.md) (pool math, breakers, migrations) · [013 — Product Catalog Example](./013-product-catalog-example.md) (end-to-end domain) · [014 — Technology Comparisons](./014-technology-comparisons.md) (MongoDB vs Postgres, bus trade-offs) · [015 — System Design Scenarios](./015-system-design-scenarios.md) (checkout, search, feed, relay) · [MongoDB Basics](./mongodb.md) · [Hub](./index.md).

**Official docs:** [MongoDB Manual](https://www.mongodb.com/docs/manual/) · [BSON Types](https://www.mongodb.com/docs/manual/reference/bson-types/) · [ObjectId](https://www.mongodb.com/docs/manual/reference/method/ObjectId/) · [Data Modeling](https://www.mongodb.com/docs/manual/core/data-modeling-introduction/) · [Indexes + ESR](https://www.mongodb.com/docs/manual/reference/method/db.collection.createIndex/) · [Explain](https://www.mongodb.com/docs/manual/reference/method/db.collection.explain/) · [Aggregation](https://www.mongodb.com/docs/manual/core/aggregation-pipeline/) · [Transactions](https://www.mongodb.com/docs/manual/core/transactions/) · [Read/Write Concern](https://www.mongodb.com/docs/manual/reference/read-concern/) · [Replica Sets + Elections](https://www.mongodb.com/docs/manual/core/replica-set-elections/) · [Sharding + Shard Keys](https://www.mongodb.com/docs/manual/core/sharding-shard-key/) · [Change Streams](https://www.mongodb.com/docs/manual/core/change-streams/) · [Connection Pool (Java driver)](https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/connection/) · [Spring Data MongoDB](https://docs.spring.io/spring-data-mongodb/reference/) · [Spring Boot 4.1.0](https://docs.spring.io/spring-boot/index.html).

---

*Prev: [013 — Product Catalog Example](./013-product-catalog-example.md) ·*
*Next: [014 — Technology Comparisons](./014-technology-comparisons.md) ·*
*Capstone scenarios: [015 — System Design Scenarios](./015-system-design-scenarios.md).*

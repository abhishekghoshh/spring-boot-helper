# 014 — Technology Comparisons (Boot 4.1.0 + Java 25)

> Decision guide for the `orders` + `products` + `payments` domain: MongoDB vs PostgreSQL / MySQL / Redis / Elasticsearch, plus in-stack choices (normalize vs denormalize, repository vs template, driver vs Spring Data, embed vs reference, offset vs cursor, single-doc vs multi-doc TX, replica set vs sharding) — each with differences, trade-offs, ops cost, and a scenario verdict.
>
> **Stack:** Spring Boot **4.1.0** · Java **25** · Spring Data MongoDB 5.x · MongoDB **7.x/8.x**.
>
> Companion guides: [004 — MongoTemplate CRUD](./004-mongotemplate-crud.md) · [006 — Data Modeling](./006-data-modeling-schema-design.md) · [009 — Transactions & Concurrency](./009-transactions-concurrency.md) · [010 — Replication, Sharding & Advanced Ops](./010-replication-sharding-advanced.md) · [MongoDB Introduction](./mongodb.md) · [Spring Cache Fundamentals](../spring-redis/001-spring-cache-fundamentals.md).

## Table of Contents

- [How to Use This Guide](#how-to-use-this-guide-read-verdicts-last)
- [1. MongoDB vs PostgreSQL](#1-mongodb-vs-postgresql-document-flexibility-vs-relational-depth)
- [2. MongoDB vs MySQL](#2-mongodb-vs-mysql-flexible-documents-vs-mainstream-rdbms)
- [3. MongoDB vs Redis](#3-mongodb-vs-redis-persistent-documents-vs-in-memory-cache)
- [4. MongoDB vs Elasticsearch](#4-mongodb-vs-elasticsearch-primary-store-vs-search-index)
- [5. Normalization vs Denormalization](#5-normalization-vs-denormalization-in-mongodb)
- [6. MongoRepository vs MongoTemplate](#6-mongorepository-vs-mongotemplate-decision-table)
- [7. Java Driver vs Spring Data MongoDB](#7-java-driver-vs-spring-data-mongodb)
- [8. Embed vs Reference](#8-embed-vs-reference)
- [9. Offset vs Cursor Pagination](#9-offset-vs-cursor-pagination)
- [10. Single-Doc Update vs Multi-Doc Transaction](#10-single-doc-update-vs-multi-doc-transaction)
- [11. Replica Set vs Sharding](#11-replica-set-vs-sharding)
- [12. Master Verdict Matrix](#12-master-verdict-matrix-one-line-per-scenario)
- [Pitfalls (GOLD)](#pitfalls-gold)
- [Interview Q&A](#interview-qa)
- [Resources](#resources)

> **Convention:** `com.commercemesh.orders.*`, `Instant` UTC, `BigDecimal` + `Decimal128`. Snippets mirror repo style, **not compiled**.

---

## How to Use This Guide (Read Verdicts Last)

Each comparison follows the same shape: **Difference** (what changes), **Strengths / weaknesses**, **Use cases**, **Ops complexity**, **Consistency / perf trade-offs**, and a conditional **Scenario verdict** for the order/payment/catalog domain. Verdicts are tendencies, not rankings — workload shape, data size, team skill, and latency budgets often shift the answer more than the technology name.

```java
// PostgreSQL/MySQL -> rows + joins + relational integrity, scale-up first.
// MongoDB          -> BSON documents + co-located reads, scale-out friendly.
// Redis            -> in-memory structures + TTL, generally complements the DB.
// Elasticsearch    -> inverted index + relevance ranking, usually a derived view.
```

---

## 1. MongoDB vs PostgreSQL (Document Flexibility vs Relational Depth)

**Difference.** PostgreSQL stores normalized rows with fixed schema, declarative joins, and constraints (FKs, unique/exclusion constraints, `JSONB` for semi-structured pockets). MongoDB stores self-contained BSON documents with flexible per-document shape and nested arrays/objects, so aggregate reads often complete in one fetch. In Spring, PostgreSQL generally pairs with Spring Data JPA/Hibernate; MongoDB pairs with Spring Data MongoDB (`MongoRepository`/`MongoTemplate`, see [004](./004-mongotemplate-crud.md)).

**Strengths / weaknesses.**

| Aspect | MongoDB tends to offer | PostgreSQL tends to offer |
|---|---|---|
| Schema evolution | New fields often need no migration; mixed shapes can coexist | `ALTER TABLE` + migrations are explicit; `JSONB` softens this |
| Aggregate reads | One-document reads (order + items + snapshot) can be fast | Aggregates generally need joins; views/`JSONB` can help |
| Relational queries | `$lookup` exists but is generally less expressive than SQL | Joins, window functions, CTEs are mature and optimizer-backed |
| Integrity | `$jsonSchema` validation is opt-in (see [006](./006-data-modeling-schema-design.md)) | FKs, deferrable constraints, serializable isolation built in |

**Use cases.** MongoDB-leaning: variant-heavy catalogs, order snapshots, nested user profiles, fast-iterating domains where read shape ≈ write shape. PostgreSQL-leaning: ledger-style payments with reconciliation, multi-entity reporting, combined relational + geospatial queries, teams with deep SQL tuning skill.

**Ops complexity.** Both need backups, monitoring, and HA planning. PostgreSQL HA generally centers on primary + streaming replicas + failover tooling (Patroni/Stolon/managed) plus pooling (HikariCP, often PgBouncer). MongoDB HA centers on replica sets (elections, lag — see [010](./010-replication-sharding-advanced.md)) with sharding only if one set is outgrown. Neither is clearly lighter; managed-vs-self-hosted tends to dominate cost.

**Consistency / perf trade-offs.** PostgreSQL on the primary generally gives familiar read-your-write with well-understood isolation levels; read replicas introduce lag to reason about. MongoDB single-document writes are atomic; multi-document work needs an explicit TX on a replica set (see [009](./009-transactions-concurrency.md)), adding latency + retries. Embedding can reduce round trips; SQL joins often reduce app-side stitching for analytics.

**Scenario verdict.** Heterogeneous catalog + high read volume → MongoDB document per product tends to fit. Payments ledger + audit reports → PostgreSQL with FKs + explicit TX tends to be easier to audit. Mixed shops (`products` in MongoDB, `payments` in PostgreSQL) are common — keep one system of record per aggregate and sync via outbox/change streams rather than dual-writing from controllers.

```java
// Shape contrast on the same "order" concept (illustrative, not compiled):
// JPA/PostgreSQL: normalized entities + join to reassemble.
@Entity class OrderEntity { @Id Long id; @OneToMany List<OrderItemEntity> items; }
// MongoDB: one document carrying its snapshot.
@Document("orders") class OrderDoc { @Id String id; List<ItemSnapshot> items; Instant createdAt; }
```

Hybrid caution: when truth is split across engines, generally avoid distributed transactions spanning both. Prefer an outbox row (or change-stream event) published after the local commit, with an idempotent consumer updating the other side — the same pattern §4 uses for search indexes.

---

## 2. MongoDB vs MySQL (Flexible Documents vs Mainstream RDBMS)

**Difference.** Much of §1 applies, with MySQL/InnoDB specifics: row storage, B-tree/secondary indexes, InnoDB transactions + FKs, and a large operational footprint (RDS/Cloud SQL, Vitess/ProxySQL layers at scale). MongoDB keeps the document + replica-set + optional-sharding model from [010](./010-replication-sharding-advanced.md) and [mongodb.md](./mongodb.md).

**Strengths / weaknesses.** MongoDB tends to reduce mapping friction for nested aggregates and tolerate heterogeneous catalog shapes without migration scripts. MySQL tends to offer familiar relational modeling, broad hosting/tooling, and straightforward TX semantics for row-centric workloads. MySQL `JSON` columns cover semi-structured pockets, though deep document-graph queries are generally less natural; MongoDB `$lookup`/aggregation covers joins, though multi-collection analytics are generally more verbose than SQL.

**Use cases.** MongoDB-leaning: evolving catalogs, display snapshots, "fetch aggregate by key" workloads. MySQL-leaning: stable tabular CRUD, existing MySQL operational muscle, ecosystems built on MySQL-compatible tooling.

**Ops complexity.** MySQL replication (primary/replica, Group Replication, managed failover) is widely operated; large-scale sharding often adds proxy middleware with its own runbook. MongoDB replica sets provide elections out of the box; sharding adds `mongos` + config-server concerns. At modest scale on managed clouds, both can be comparably hands-off.

**Consistency / perf trade-offs.** Single-row InnoDB TX and single-document MongoDB writes are both generally low-friction; cross-row/cross-document invariants both need care (deadlocks on one side, 60 s default TX limit + retries on the other — see [009](./009-transactions-concurrency.md)). Aggregate-heavy reads often favor embedding; shared counters and join-heavy reporting often favor indexed relational rows.

**Scenario verdict.** New variant-heavy catalog with few cross-table reports → MongoDB is generally worth evaluating first. Order service already on MySQL with stable schema and team expertise → staying tends to be lower-risk, with MongoDB optionally projecting catalog/search read models.

```sql
-- MySQL JSON pocket vs MongoDB native document (illustrative):
SELECT id, JSON_EXTRACT(attrs, '$.color') FROM products WHERE JSON_CONTAINS(attrs, '"red"', '$.color');
-- vs MongoDB: db.products.find({ "attrs.color": "red" }, { name: 1, "attrs.color": 1 })
```

MySQL `JSON` works well for sparse optional attributes on an otherwise tabular row; once queries filter, sort, and aggregate *inside* the JSON across many rows, a document collection with dedicated indexes and pipelines tends to read more naturally.

---

## 3. MongoDB vs Redis (Persistent Documents vs In-Memory Cache)

**Difference.** MongoDB is a persistent system of record: disk-first (WiredTiger), replica sets, query language, secondary indexes, aggregation. Redis is primarily an in-memory structure store (strings, hashes, sets, sorted sets, streams) with optional persistence (RDB/AOF), TTL expiry, and sub-millisecond access suited to caching, counters, rate limits, and leaderboards. They answer different questions: *where truth lives* vs *how hot reads stay fast*. Caching background: [Spring Cache Fundamentals](../spring-redis/001-spring-cache-fundamentals.md).

**Strengths / weaknesses.**

| Aspect | MongoDB | Redis |
|---|---|---|
| Durability | Disk-first + journaling + `majority`/`j:true` for critical writes | Memory-first; persistence generally aids recovery, not ledgering |
| Query power | Rich queries, indexes, aggregation pipeline | Key access + structure ops; complex queries belong elsewhere |
| Data size | GBs–TBs per deployment, shardable | Working sets that fit memory; large sets get expensive |
| Latency | ms-range reads, faster with right indexes | Sub-ms hits in the same region/AZ |
| Expiry | TTL indexes for retention (see [010](./010-replication-sharding-advanced.md)) | Native `EXPIRE`/`SET EX`, suited to sessions/caches |

**Use cases.** MongoDB alone: catalog truth, order history, anything queried after restart. Redis alone: hot product cards, sessions, idempotency windows, limits, locks (with care). Both together (common): MongoDB as truth + Redis as look-aside cache for read-heavy catalog paths.

**When BOTH coexist: Mongo source of truth + Redis cache.**

```java
// Look-aside: truth in MongoDB (products); Redis holds a TTL'd projection.
public ProductCard getProductCard(String id) {
    ProductCard cached = redisOps.opsForValue().get("product:card:" + id);
    if (cached != null) return cached;
    Product doc = mongoTemplate.findById(id, Product.class);
    ProductCard card = ProductCard.from(doc);
    redisOps.opsForValue().set("product:card:" + id, card, Duration.ofMinutes(5));
    return card;
}
// Update MongoDB first, then delete the Redis key. Key/TTL design: redis 001/005/006.
```

Guidelines that tend to work: prefer `update-DB-then-delete-cache`; give every projection a TTL (minutes for cards, shorter for stock hints) so missed invalidations self-heal; generally avoid treating cached stock/price as authoritative at checkout — re-read truth in the TX (see [009](./009-transactions-concurrency.md)); use change streams/outbox for invalidation signals rather than ad-hoc dual writes.

```java
// Write path pairs with the read path above (illustrative):
public void updatePrice(String id, BigDecimal price) {
    mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(id)),
        new Update().set("price", price).set("updatedAt", Instant.now()), Product.class);
    redisOps.delete("product:card:" + id); // next read repopulates with fresh TTL
}
```

**Ops complexity.** Two runbooks: replica-set lag/elections/backups plus Redis memory sizing, eviction policy (`noeviction` vs `allkeys-lru`), persistence, and failover (Sentinel/Cluster/managed). Coexistence tends to pay off with high hit ratios; for low-traffic services the extra component may not earn its keep.

**Consistency / perf trade-offs.** Cache-aside trades a staleness window (TTL + invalidation lag) for lower p95/p99 on hot keys and less MongoDB load. Checkout/capture paths generally bypass or revalidate the cache; display paths can often tolerate seconds of staleness.

**Scenario verdict.** Hot catalog (`GET /products/{id}` at high RPS, rare writes) → MongoDB + Redis look-aside with 1–5 min TTL tends to fit. Checkout/stock → read truth via atomic operator or TX; Redis holds at most a hint. Low-traffic admin reads → MongoDB alone is often sufficient.

---

## 4. MongoDB vs Elasticsearch (Primary Store vs Search Index)

**Difference.** MongoDB is a primary store: document CRUD, selective indexes, aggregation pipeline. Elasticsearch (Lucene-based) is a search/analytics engine: inverted indexes, analyzers, relevance scoring — strong at full-text, facets, typo tolerance. In Spring shops MongoDB goes through Spring Data MongoDB while Elasticsearch typically goes through the Java API Client / Spring Data Elasticsearch as a *derived* index fed from truth.

**Atlas Search middle ground.** For many catalogs, **Atlas Search** (Lucene-backed indexes in Atlas) covers faceted search without a second cluster: `$search` stages, analyzers, autocomplete, synonyms. It tends to suit teams wanting relevance + facets without operating Elasticsearch. Limits tend to appear with heavy cross-index analytics, custom plugins, or existing ELK investments — where dedicated Elasticsearch may still be justified.

```javascript
// Illustrative Atlas Search stage (mongosh aggregation):
db.products.aggregate([
  { $search: { index: "catalog-search",
    compound: { must: [{ text: { query: "espresso", path: "name" } }],
                filter: [{ range: { path: "price", gte: 10 } }] } } },
  { $limit: 20 },
  { $project: { name: 1, price: 1, score: { $meta: "searchScore" } } }
]);
```

**Strengths / weaknesses.** MongoDB (+ Atlas Search): one cluster for CRUD + search in many catalog cases; `$search` composes with `$match`/`$facet`/`$sort`. Elasticsearch: richer relevance tuning, language analyzers, kNN/vector options, Kibana analytics — at the cost of a second distributed system (sharding, mappings, refresh tuning). Plain `$text` indexes cover modest keyword needs but generally lag dedicated search on ranking/facets.

**Use cases.** MongoDB sufficient: filter/sort/paginate, `$text` or Atlas Search facets, "search within my orders." Elasticsearch justified: marketplace search with synonym/relevance experiments, cross-catalog analytics, review/log mining at scale.

**Ops complexity.** Elasticsearch adds mapping/analyzer lifecycle, shard/replica sizing, reindexing, plus a sync pipeline (outbox/change-stream → indexer with resume tokens — see [010](./010-replication-sharding-advanced.md) §12). Atlas Search trims this to index definitions + pipeline stages. Either way the search view is generally treated as rebuildable from MongoDB truth.

**Consistency / perf trade-offs.** Search views are eventually consistent by design (refresh + pipeline lag); listing/search paths usually tolerate this while checkout/inventory paths generally should not depend on it. Indexing every write adds amplification; batching + idempotent upserts by `_id` tends to keep pipelines stable.

**Scenario verdict.** Faceted catalog search with no dedicated search team → Atlas Search over MongoDB truth tends to be pragmatic. Search-as-a-product (merchandising, experiments) → MongoDB record + Elasticsearch derived index via change streams/outbox. Generally avoid treating the index as the order/payment ledger.

```java
// Sync sketch: change-stream event -> idempotent index upsert (illustrative).
changeStreamFlux.subscribe(event -> searchIndexer.upsert(
    event.getDocumentKey().getObjectId("_id").toString(), toSearchDoc(event)));
// Persist the resume token per batch so restarts continue rather than replay
// everything; tombstone deletes rather than silently dropping them.
```

---

## 5. Normalization vs Denormalization in MongoDB

**Difference.** Normalization splits facts across collections with references + join-time assembly; denormalization copies read-needed facts into the consuming document (product `name` + `price` snapshot in each order item). Both coexist — the choice is per relationship, not per database (see [006](./006-data-modeling-schema-design.md)).

```json
// Normalized: reference to live truth
{ "_id": "OI-1", "orderId": "ORD1001", "productId": "P-ESPR-001", "qty": 2 }
// Denormalized: display facts snapshotted into the order
{ "_id": "ORD1001", "items": [
  { "productId": "P-ESPR-001", "name": "Espresso Beans (snapshot)",
    "unitPrice": 19.99, "qty": 2 } ] }
```

**Strengths / weaknesses.** Normalized tends to keep one truth (price change = one write) at the cost of `$lookup`/multi-query assembly. Denormalized tends to make reads single-fetch and history-safe (orders keep price paid) at the cost of duplication + fan-out. Denormalization without a staleness policy is a common source of "which price is right?" bugs.

**Use cases.** Normalize when facts change independently (live price, stock) or many writers share one entity. Denormalize (snapshot) when history matters, read shape is stable (order confirmation), or reads outnumber source changes by orders of magnitude.

**Ops complexity.** Normalized shifts cost to query time (joins, index coordination); denormalized shifts it to write/sync time (fan-out jobs, propagators, reconciliation). Snapshot fields (`unitPrice`, `nameSnapshot`, `snapshottedAt`) make staleness auditable.

**Consistency / perf trade-offs.** Denormalized reads are often faster with no cross-collection TX; the trade is bounded staleness on copied fields. Normalized reads are fresher by construction but can need multi-doc TX for consistent cross-collection writes (see [009](./009-transactions-concurrency.md)).

**Scenario verdict.** Order items → snapshot `name` + `unitPrice`, keep `productId` for linkage; display reads skip joins. Live catalog facts → keep normalized in `products`; propagate to caches/search views async rather than rewriting history.

```json
// Staleness-auditable snapshot: readers see what was true at purchase.
{ "productId": "P-ESPR-001", "name": "Espresso Beans", "unitPrice": 19.99,
  "snapshottedAt": "2026-09-01T10:00:00Z", "schemaVersion": 2 }
```

A `snapshottedAt`/`schemaVersion` pair lets support answer "why does this 2024 order show the old name?" without guessing, and lets migrations target only pre-v2 order docs.

---

## 6. MongoRepository vs MongoTemplate (Decision Table)

**Difference.** `MongoRepository` is declarative: derived methods, paging/sorting conventions, mapping handled for you. `MongoTemplate` is imperative: `Query`/`Criteria`/`Update`, `findAndModify`, aggregations, bulk, collection management with explicit control (depth in [004](./004-mongotemplate-crud.md)).

```java
// Repository: concise CRUD + derived queries
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategoryAndPriceLessThan(String category, BigDecimal price);
}
// Template: explicit atomic update
Query q = new Query(Criteria.where("_id").is(id).and("stock").gte(qty));
Update u = new Update().inc("stock", -qty).set("updatedAt", Instant.now());
Product updated = mongoTemplate.findAndModify(q, u,
    FindAndModifyOptions.options().returnNew(true), Product.class);
```

**Decision table.**

| Task | `MongoRepository` | `MongoTemplate` | Tendency |
|---|---|---|---|
| CRUD by id / save / delete | One-liner | Verbose | Repository |
| Derived filters | Ideal | Overkill | Repository |
| Standard paging/sorting | Built-in `Pageable` | Manual `with(pageable)` | Repository |
| Dynamic facet/range filters | Gets awkward | `Criteria` composition shines | Template |
| Atomic `findAndModify` | Not directly supported | First-class | Template |
| Upserts / aggregations / bulk | Limited / `@Aggregation` basics | Full control / `BulkOperations` | Template |

**Strengths / weaknesses.** Repositories tend to minimize boilerplate but can obscure generated queries as method names grow. Template code tends to make operators/options explicit at the cost of more code to maintain.

**Use cases / ops / consistency.** No cluster impact — same driver/session. Repositories join TX via `@Transactional` + `MongoTransactionManager`; template code can also use `TransactionTemplate`/`ClientSession` (see [009](./009-transactions-concurrency.md)).

**Scenario verdict.** Catalog CRUD + standard listings → start with `MongoRepository`. Checkout paths (guarded decrements, idempotent upserts) and reporting pipelines → prefer `MongoTemplate`, often alongside the repository with a documented boundary.

```java
// Coexistence in one service (illustrative): repository for reads, template for the guarded write.
@Service class CatalogService {
    private final ProductRepository repo; private final MongoTemplate template;
    public Page<Product> active(Pageable p) { return repo.findByActiveTrue(p); }
    public Optional<Product> reserve(String id, int qty) { /* findAndModify via template */ return Optional.empty(); }
}
```

---

## 7. Java Driver vs Spring Data MongoDB

**Difference.** The **Java driver** (`MongoClient`, `MongoCollection<Document>`, `ClientSession`) is the low-level API: explicit BSON, command control, minimal abstraction. **Spring Data MongoDB** builds on it: POJO mapping (`@Document`, `Instant`/`Decimal128` converters), repositories, template, auditing, TX integration.

```java
// Driver: explicit BSON
col.updateOne(Filters.eq("_id", id), Updates.inc("stock", -qty));
// Spring Data: mapped POJO (see 004)
mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(id)),
    new Update().inc("stock", -qty), Product.class);
```

**Strengths / weaknesses.** Driver tends to give direct command/session control — handy for tooling, migrations, non-Spring runtimes. Spring Data tends to shorten app code (mapping, `DataAccessException` translation, `@Transactional`) at the cost of an abstraction to understand when debugging. Version coupling matters: Boot 4.1.0 → Spring Data MongoDB 5.x tracks a driver baseline; overriding the driver independently can introduce subtle issues.

**Use cases.** Driver: CLIs, migration jobs, minimal services without Spring, raw command access. Spring Data: typical Boot services where repository/template ergonomics and TX integration pay off.

**Ops complexity / consistency / perf.** Topology is identical (same replica set/cluster). Perf gaps are generally small next to schema/index choices; mapping overhead rarely dominates unless documents are huge or writes loop per-document instead of bulk.

**Scenario verdict.** Boot microservices (`orders`, `catalog`) → Spring Data is generally the productive default. Shared non-Spring libraries or one-off ops tooling → plain driver tends to keep dependencies lighter.

```java
// Auditing + mapping you get "for free" with Spring Data (illustrative):
@Document("products") class Product {
    @Id String id; String sku;
    @CreatedDate Instant createdAt; @LastModifiedDate Instant updatedAt; @Version Long version;
}
// Driver equivalent needs manual timestamp/version handling on every write path.
```

---

## 8. Embed vs Reference

**Difference.** Embedding nests related data in the parent (order → items, product → variants). Referencing stores a key (`productId`) resolved separately. Deep patterns live in [006](./006-data-modeling-schema-design.md).

**Strengths / weaknesses.** Embed tends to give single-read retrieval + single-doc atomicity; risks include 16 MiB growth, hot-document contention, awkward independent querying. Reference tends to keep documents small and evolvable; costs include extra queries/`$lookup` + multi-doc consistency handling.

| Signal | Leans embed | Leans reference |
|---|---|---|
| Access | Almost every read needs both | Often read separately |
| Cardinality | One–few, bounded | Many / unbounded / "squillions" |
| Change | Changes with parent | Independent lifecycle |
| Query | Fetch by parent key | Query child directly |
| History | Snapshot belongs to parent event | Live truth shared across parents |

**Use cases.** Embed: order + items snapshot, user + addresses, product + few variants. Reference: shared catalog entities, users ↔ roles, large comment streams (bucket or separate collection).

**Ops complexity.** Embedded arrays need multikey-index care + growth awareness (large rewrites cost more). Referenced models need index discipline on both sides (`productId`, `orderId`) and shard-key locality in sharded setups (see [010](./010-replication-sharding-advanced.md)).

**Consistency / perf trade-offs.** Embedded updates to one doc are atomic without TX; referenced cross-doc updates generally need TX or compensating design (see [009](./009-transactions-concurrency.md)). Reads favor embedding when the aggregate is the access unit; shared write-heavy entities often favor referencing to avoid fan-out.

**Scenario verdict.** `orders.items[]` → embed snapshots (bounded, historical). Live `orders → products` → reference `productId`, join at display or via cached projection. `products.reviews` → embed only when few + display-coupled; otherwise bucket or separate collection.

```json
// Bucket alternative for unbounded children (illustrative review bucket):
{ "_id": "P-ESPR-001#page-042", "productId": "P-ESPR-001", "page": 42,
  "reviews": [ { "userId": "u-7", "stars": 5, "at": "2026-08-01T09:00:00Z" } ] }
```

Buckets cap document size while keeping page-fetch reads to one document — the middle ground between "embed everything" and "one collection per review" (see [006](./006-data-modeling-schema-design.md)).

---

## 9. Offset vs Cursor Pagination

**Difference.** Offset paging (`page`/`size` → `skip`/`limit`) jumps to arbitrary pages. Cursor (keyset) paging filters forward from the last seen key (`_id` or `createdAt + _id`). Both work via `MongoTemplate` + `Pageable` or manual `Query` (see [004](./004-mongotemplate-crud.md) §11).

```java
// Offset: simple; deep pages scan + discard
Query offset = new Query().with(PageRequest.of(page, size, Sort.by("_id")));
// Cursor: index-friendly forward scan
Query next = new Query(Criteria.where("_id").gt(lastSeenId)).limit(size).with(Sort.by("_id"));
```

**Strengths / weaknesses.** Offset tends to suit "go to page 7" admin UX and small sets; deep skips generally waste work and results can shift under concurrent writes. Cursor tends to stay O(seek + limit) with stable forward scans; it generally cannot jump to arbitrary pages and needs a well-ordered unique key.

**Use cases.** Offset: back-office lists, bounded exports with page numbers. Cursor: storefront scroll, order-history feeds, sync/poll endpoints, collections expected to grow large.

**Ops complexity.** Offset needs a supporting sort index; cursor needs a unique indexed sort key (often `_id` or `(createdAt, _id)`) plus client cursor handling and a documented keyset contract.

**Consistency / perf trade-offs.** Under concurrent inserts, offset pages can duplicate/miss rows; cursor pages anchored on an immutable key tend to stay consistent. Gap widens with depth: page 1 is similar; page 10,000 generally favors cursor strongly.

**Scenario verdict.** Customer-facing catalog/history → cursor is generally the safer default. Internal admin tables with bounded sizes → offset remains acceptable with max page/size caps.

```java
// Cursor response envelope: clients echo back nextCursor (illustrative).
record ProductPage(List<Product> items, String nextCursor) {}
// Offset guard: cap size and depth so one caller cannot force skip(1_000_000).
int size = Math.min(requestedSize, 100); int page = Math.min(requestedPage, 100);
```

---

## 10. Single-Doc Update vs Multi-Doc Transaction

**Difference.** A single-document write (operators, `findAndModify`) is atomic by itself. A multi-document TX wraps cross-document/collection reads + writes in an all-or-nothing session with `snapshot`/`majority` concerns. Mechanics and retries: [009](./009-transactions-concurrency.md).

```java
// Single-doc atomic guard, no TX
Query q = new Query(Criteria.where("_id").is(sku).and("stock").gte(qty));
mongoTemplate.updateFirst(q, new Update().inc("stock", -qty), Product.class);
// Multi-doc: stock + order + payment commit or roll back together
@Transactional // MongoTransactionManager; replica set required
public void placeOrder(OrderRequest req) { /* decrement + insert + insert */ }
```

**Strengths / weaknesses.** Single-doc tends to be lower-latency and retry-friendly (idempotent operators + retryable writes) on more topologies. Multi-doc TX expresses cross-entity invariants directly at the cost of session overhead, 60 s default limit, oplog pressure, and transient-error retries.

**Use cases.** Single-doc: counters, reservations via `findAndModify`, status transitions. Multi-doc TX: `placeOrder` spanning `products`/`orders`/`payments`, transfers, flows where partial persistence would corrupt the domain.

**Ops complexity.** TX needs a replica set minimum, `majority` write concern on critical paths, and dashboards on duration/abort rates. Single-doc needs more schema thought (can the boundary collapse into one document? — [006](./006-data-modeling-schema-design.md) §11, [009](./009-transactions-concurrency.md) §16).

**Consistency / perf trade-offs.** Single-doc scopes atomicity to one `_id`; wider invariants are stitched by app logic. Multi-doc TX gives snapshot isolation across the set; per-node throughput is generally lower, so keeping TX sets small and short matters.

**Scenario verdict.** Prefer single-doc designs (embed, state machines, idempotency keys) where the domain allows. Use multi-doc TX when invariants genuinely span documents — then keep it tight (few docs, no network calls inside) with explicit concerns and bounded retries on `TransientTransactionError`/`UnknownTransactionCommitResult`.

```java
// Retry shape for commit-unknown outcomes (illustrative, not compiled):
try { txTemplate.execute(status -> { placeOrderParts(req); return null; }); }
catch (TransientTransactionException retryable) { /* bounded retry with backoff */ }
// Pair with an idempotency_keys insert (see 009 §14) so a retried placeOrder
// cannot mint a second order when the first attempt actually committed.
```

---

## 11. Replica Set vs Sharding

**Difference.** A **replica set** replicates one data image across nodes for availability + read scaling (primary + secondaries + election). **Sharding** partitions data by shard key with `mongos` routing + config servers for write/scale-out (see [010](./010-replication-sharding-advanced.md)).

**Strengths / weaknesses.** Replica set tends to be simpler (one image, familiar failover) with vertical limits. Sharding tends to lift storage/throughput ceilings at the cost of key-design permanence, balancer/chunk work, scatter-gather risk, and multi-shard TX overhead.

**Use cases.** Replica set sufficient: most services well under ~500 GB / ~10k ops/s or fitting vertical headroom with good indexing. Sharding justified: sustained growth past single-set limits, key-partitionable workloads (`customerId`/`tenantId` prefixes), high-volume ingest.

**Ops complexity.** Replica sets need election/lag/backup runbooks. Sharded clusters add `mongos` sizing, CSRS health, chunk-balance review, zone discipline, coordinated PITR. Shard keys are difficult to change — prototype with production-like distribution first.

**Consistency / perf trade-offs.** Key-routed (targeted) queries can scale near-linearly; scatter-gather queries fanning to many shards generally pay latency each request. Multi-shard TX works with more coordination cost — another reason to co-locate related data by key.

**Scenario verdict.** Start on a 3-node replica set (even locally for TX/change-stream features); add `ReadPreference` read scaling where suitable. Shard on measured pressure (disk, ops/s, connections), picking the key from the dominant query path (`tenantId + _id`, `customerId + createdAt`).

```javascript
// Pre-sharding sanity checks (mongosh, illustrative):
db.products.getShardDistribution(); // even spread, no jumbo chunks?
db.products.aggregate([{ $group: { _id: "$tenantId", n: { $sum: 1 } } },
  { $sort: { n: -1 } }, { $limit: 5 }]); // one tenant dwarfing the rest?
// A key dominated by one tenant value tends to hot-shard; prefer higher-cardinality prefixes.
```

---

## 12. Master Verdict Matrix (One Line per Scenario)

| Scenario | Tendency |
|---|---|
| Variant-heavy catalog, aggregate reads | MongoDB documents |
| Ledger + reconciliation + cross-entity reports | PostgreSQL (or current RDBMS) with explicit TX |
| Stable CRUD on existing MySQL estate | Stay on MySQL; project read models if needed |
| Hot cards / sessions / counters / limits | MongoDB truth + Redis cache (TTL + invalidation) |
| Faceted search, no search team | MongoDB + Atlas Search |
| Search-as-product, relevance tuning | MongoDB truth + Elasticsearch derived index |
| Immutable history (price paid) | Denormalize snapshot into event doc |
| Live shared facts (price/stock) | Keep normalized; propagate async |
| Simple CRUD + derived queries | `MongoRepository` |
| Atomic ops, upserts, pipelines, bulk | `MongoTemplate` |
| Boot service with mapping + TX | Spring Data MongoDB |
| Non-Spring lib / CLI / migration | Plain Java driver |
| Bounded, co-read, historical children | Embed |
| Shared, unbounded, separately queried | Reference (or buckets) |
| Feeds, scroll, large collections | Cursor pagination |
| Admin tables, bounded, page-number UX | Offset with caps |
| One-aggregate invariant | Single-doc atomic update |
| Cross-collection invariant | Short multi-doc TX with retries |
| Under single-set limits | Replica set |
| Past limits on partitionable key | Shard on dominant query key |

---

## Pitfalls (GOLD)

1. **"Flexible schema" as "no schema."** Mixed shapes without `$jsonSchema` + `schemaVersion` tend to push migration cost into every reader. Start `moderate`/`warn`, tighten to `strict`/`error` once stable ([010](./010-replication-sharding-advanced.md) §14, [006](./006-data-modeling-schema-design.md)).
2. **Caching truth instead of projections.** Reading cached `stock`/`price` at checkout tends to oversell or misprice. Cache display cards; re-read truth on the critical path (§3, [009](./009-transactions-concurrency.md)).
3. **TTL *or* invalidation instead of both.** TTL-only leaves staleness; invalidation-only leaks keys on missed paths. Use both and document the keyspace (redis 001/005/006).
4. **`$lookup` as SQL joins.** Per-request multi-collection joins that could be snapshots tend to inflate p95. Model queries-first per [006](./006-data-modeling-schema-design.md).
5. **Repository for everything.** Long derived names hide query shape; atomic counters and pipelines get harder to reason about. Keep template methods for guarded updates/aggregations ([004](./004-mongotemplate-crud.md) §17).
6. **Deep offset pages in storefront APIs.** Large `skip` tends to slow down exactly as the catalog succeeds. Prefer cursor pagination (§9).
7. **TX as modeling band-aid.** Wrapping every write in multi-doc TX instead of collapsing the boundary tends to trade a schema discussion for permanent latency + retries. Check [009](./009-transactions-concurrency.md) §16 first.
8. **Standalone for TX/change streams.** Both need a replica set (single-node `rs0` suffices for dev). Standalone silently removes options teams assume exist ([010](./010-replication-sharding-advanced.md) §1, [009](./009-transactions-concurrency.md) §4).
9. **Sharding on day one.** A key chosen without query-shape data tends to bake in hot shards or scatter-gather. Start replica set; shard on evidence (§11).
10. **Index as system of record.** Rebuilding orders from Elasticsearch after mapping mishaps tends to be painful. Keep MongoDB/ledger rebuildable and the index disposable (§4).

---

## Interview Q&A

**Q1 — When would you choose MongoDB over PostgreSQL, and when would you not?**
Document-shaped reads with bounded cross-entity integrity (catalog, order snapshots) tend to fit MongoDB; ledger reconciliation, heavy joins, and constraint-driven integrity (payments audit, reporting) tend to favor PostgreSQL. Mention team skill, `$jsonSchema` vs DDL discipline, and the hybrid (MongoDB catalog + PostgreSQL ledger via outbox/change streams) rather than ranking either universally higher.

**Q2 — Catalog reads are slow at peak. Redis, Elasticsearch, or Atlas Search — keeping MongoDB as truth?**
Hot-key reads → Redis look-aside with TTL + invalidation; relevance/facets → Atlas Search for simpler estates or Elasticsearch as derived index for heavier needs. MongoDB stays rebuildable source (change-stream/outbox sync, idempotent upserts by `_id`); checkout re-reads truth, not cache/index projections ([010](./010-replication-sharding-advanced.md) §12, [Spring Cache Fundamentals](../spring-redis/001-spring-cache-fundamentals.md)).

**Q3 — `MongoRepository` vs `MongoTemplate` for a checkout stock decrement?**
Prefer `MongoTemplate.findAndModify` (or guarded `updateFirst`) with `stock >= qty` + `$inc`: the atomic read-plus-write maps to the invariant and returns post-state for branching. Repositories suit CRUD/derived queries; template suits operators, upserts, pipelines, bulk (§6, [004](./004-mongotemplate-crud.md)).

**Q4 — Why do order items snapshot `name`/`unitPrice` instead of referencing `products`?**
History vs liveness: snapshots preserve price-at-purchase for receipts without joins and survive catalog edits, while `productId` remains for linkage and live reads hit `products`. Note the policy (`snapshottedAt`, no retroactive rewrites) and that denormalization is per-relationship ([006](./006-data-modeling-schema-design.md) §8, §5 here).

**Q5 — Checkout spans `products`, `orders`, `payments`: single-doc or multi-doc TX?**
Single-doc operators where the invariant fits one document (guarded `$inc`); short multi-doc TX with `MongoTransactionManager` where all-or-nothing across collections is required. Cover replica-set prerequisite, `majority`/`snapshot` concerns, 60 s default limit, transient-error retries, idempotency keys, and whether embedding could collapse the boundary ([009](./009-transactions-concurrency.md)).

---

## Resources

- Track companions (this repo):
  - [004 — MongoTemplate CRUD](./004-mongotemplate-crud.md) — `Query`/`Criteria`/`Update`, `findAndModify`, upserts, aggregation, bulk; §17 repo-vs-template table.
  - [006 — Data Modeling & Schema Design](./006-data-modeling-schema-design.md) — embed vs reference, cardinality, denormalization, buckets, validation, migrations.
  - [009 — Transactions & Concurrency](./009-transactions-concurrency.md) — single-doc atomicity, replica-set TX, concerns, retries, `@Version`, idempotency.
  - [010 — Replication, Sharding & Advanced Ops](./010-replication-sharding-advanced.md) — replica sets, shard keys, change streams, TTL/time-series/GridFS, pooling, backup.
  - [MongoDB Introduction](./mongodb.md) — document model, BSON, architecture baseline.
  - [Spring Cache Fundamentals](../spring-redis/001-spring-cache-fundamentals.md) — cache-aside, TTL, eviction, Redis coexistence patterns for §3.
- Official docs (version behavior for Boot 4.1.0 / Java 25 / MongoDB 7.x–8.x):
  - MongoDB Manual — Modeling, Transactions, Replica Sets, Sharding, Change Streams, Atlas Search.
  - Spring Data MongoDB Reference — `MongoRepository`, `MongoTemplate`, mapping/converters, transactions.
  - Spring Boot Reference — MongoDB auto-configuration and connection properties.
  - Redis + Elasticsearch docs — persistence/eviction and index-mapping guidance for §3/§4 hybrids.

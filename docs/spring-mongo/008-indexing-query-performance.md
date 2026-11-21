# 008 — Indexing & Query Performance (Boot 4.1.0 + Java 25, Product / Order Domain)

> Every slow query in `product-catalog-service` and `order-service` is an index decision — single-field, compound + ESR, multikey, unique, partial, TTL, text, `2dsphere`, wildcard, hashed — proved with `explain("executionStats")`, profiling, and the slow log.
>
> Companion guides: [002 — Document Mapping](./002-document-mapping.md) · [005 — MongoDB Query Language](./005-mongodb-query-language.md) · [007 — Aggregation Framework](./007-aggregation-framework.md) · [012 — Production Readiness](./012-production-readiness.md) · [MongoDB Introduction](./mongodb.md).

## Table of Contents

- [1. What Indexes Are + What They Cost](#1-what-indexes-are--what-they-cost)
- [2. Single-Field Indexes](#2-single-field-indexes)
- [3. Compound Indexes, Ordering, and Prefixes](#3-compound-indexes-ordering-and-prefixes)
- [4. ESR Guideline + ESR Limits](#4-esr-guideline--esr-limits)
- [5. Multikey Indexes (Arrays)](#5-multikey-indexes-arrays)
- [6. Unique Indexes + Duplicate-Key Handling (→ 409)](#6-unique-indexes--duplicate-key-handling--409)
- [7. Partial Indexes](#7-partial-indexes)
- [8. Sparse vs Partial (Table)](#8-sparse-vs-partial-table)
- [9. TTL Indexes](#9-ttl-indexes)
- [10. Text Indexes + search-service Example](#10-text-indexes--search-service-example)
- [11. Geospatial 2dsphere Indexes](#11-geospatial-2dsphere-indexes)
- [12. Wildcard Indexes](#12-wildcard-indexes)
- [13. Hashed Indexes](#13-hashed-indexes)
- [14. Covered Queries + Selectivity + Intersection](#14-covered-queries--selectivity--intersection)
- [15. Storage and Write Overhead](#15-storage-and-write-overhead)
- [16. createIndex / dropIndex + @Indexed / @CompoundIndex + MongoConfig](#16-createindex--dropindex--spring-indexed--compoundindex--mongoconfig-bootstrap)
- [17. Nested Fields and Array-Element Indexes](#17-nested-fields-and-array-element-indexes)
- [18. Missing / Null Behavior](#18-missing--null-behavior)
- [19. Pagination Indexes (Sort + _id Tiebreaker)](#19-pagination-indexes-sort--_id-tiebreaker)
- [20. Aggregation Index Use](#20-aggregation-index-use)
- [21. explain(): executionStats, IXSCAN vs COLLSCAN, Winning Plan](#21-explain-executionstats-ixscan-vs-collscan-winning-plan)
- [22. Profiling (Level 2 + system.profile) and the Slow Log](#22-profiling-level-2--systemprofile-and-the-slow-log)
- [23. Three Before / After Slow Queries (With Plans)](#23-three-before--after-slow-queries-with-plans)
- [24. Pitfalls (GOLD)](#24-pitfalls-gold)
- [25. Interview Q&A](#25-interview-qa)
- [26. Resources](#26-resources)

> Boot `4.1.0`, Java `25`, `spring-boot-starter-data-mongodb` (Spring Data MongoDB 5.x), MongoDB `7.x/8.x`. Domain `com.commercemesh.catalog.document` (`products`) + `com.commercemesh.order.document` (`orders`). Snippets mirror repo style, **not compiled**.

---

## 1. What Indexes Are + What They Cost

**Purpose:** an index is a sorted B-tree of field values + `RecordId` pointers. Without one MongoDB walks every doc (`COLLSCAN`); with one it range-seeks (`IXSCAN`) and fetches only matches.

```javascript
db.products.find({ sku: "AUR-HP-001" }); // no index → checks all 2M docs
db.products.createIndex({ sku: 1 });     // B-tree on sku ascending
db.products.find({ sku: "AUR-HP-001" }); // IXSCAN 1 key → FETCH 1 doc
```

```java
// Reference shapes used through this guide (fields trimmed for brevity)
@Document(collection = "products") public class Product {
    @MongoId private String id; private String name; private String description;
    private String brand; private String sku; private BigDecimal price;
    private String categoryId; private List<String> tags = new ArrayList<>();
    private Map<String, String> attributes = new HashMap<>();
    private double averageRating; private boolean isActive = true; private Instant createdAt;
}
@Document(collection = "orders") public class Order {
    @MongoId private String id; private String customerId; private String status;
    private List<OrderItem> items = new ArrayList<>(); private BigDecimal total;
    private Instant createdAt; private Address shippingAddress;
    public record OrderItem(String sku, int qty, BigDecimal price) {}
    public record Address(String city, String zip, double[] location) {}
}
```

Every index charges three rents: **write** (one B-tree write per index per document write — 10 indexes ≈ 10 extra writes), **storage** (one key per doc per index; multikey = one key per element; text/wildcard keys widest), **RAM** (only the working set in WiredTiger cache is fast; more indexes = more eviction pressure). Rule: index the hot path (top 5 queries by QPS × p95), not every field. Start from [005](./005-mongodb-query-language.md), prove with §21, ship with §16.

---

## 2. Single-Field Indexes

**Purpose:** one field — equality + range + sort on that field. `_id_` is a unique single-field index you already have.

```javascript
db.products.createIndex({ sku: 1 });        // equality: find({sku:"X"})
db.products.createIndex({ categoryId: 1 }); // equality + grouping
db.products.createIndex({ price: -1 });     // range + newest/cheapest sorts
db.orders.createIndex({ customerId: 1 });   // "my orders" filter
```

```java
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findBySku(String sku); // uses { sku: 1 }
}
```

Direction (`1` vs `-1`) is irrelevant for pure filters (backward scan covers reverse) but matters for sorts and compounds (§3). Use `-1` for newest-first so forward scan matches the Dominant sort.

---

## 3. Compound Indexes, Ordering, and Prefixes

**Purpose:** one index serving multi-field filters plus the sort. Field order is the API contract.

```javascript
db.products.createIndex({ categoryId: 1, price: 1 });     // browse + sort/filter price
db.orders.createIndex({ customerId: 1, createdAt: -1 });  // "my orders, newest first"
db.orders.createIndex({ status: 1, createdAt: -1 });      // ops dashboard
```

**Prefixes (leftmost rule):** `{ a:1, b:1, c:1 }` serves `a`, `a+b`, `a+b+c` — never `b` alone, `c` alone, or `b+c`.

```javascript
db.products.find({ categoryId: "cat-audio" });                     // YES — prefix
db.products.find({ categoryId: "cat-audio", price: { $gte: 50 } }); // YES — full key
db.products.find({ price: { $gte: 50 } });                          // NO — needs own index
```

**Sort rule:** `{ categoryId:1, price:1 }` serves `sort({categoryId:1, price:1})` and its exact reverse, NOT mixed `sort({categoryId:1, price:-1})` — build the mixed index for mixed UI sorts. Spring `Sort` key order must equal index key order or the plan falls back to in-memory `SORT` (§21).

---

## 4. ESR Guideline + ESR Limits

**Purpose:** order compound fields **E**quality → **S**ort → **R**ange so one index filters, orders, and bounds with no in-memory sort.

```javascript
// E: categoryId | S: price (free order) | R: averageRating (bounded tail)
db.products.createIndex({ categoryId: 1, price: 1, averageRating: 1 });
db.products.find({ categoryId: "cat-audio", averageRating: { $gte: 4.0 } }).sort({ price: 1 });
db.orders.createIndex({ customerId: 1, status: 1, createdAt: -1, total: 1 });
```

ESR limits: **range-after-sort blocks the tail** — fields after a `$gte`/`$lt`/`$in` can't sort or second-range efficiently (`{a:1, b_range:1, c_sort:1}` still spills to `SORT`; fix: `{a:1, c_sort:1, b_range:1}` or split indexes). **One array field per compound** — two array fields fail with "cannot index parallel arrays" (use separate multikeys, §5). **`$in` is a mini-range** — large `$in` on the lead degrades trailing sorts; keep `$in` small or flip field order when sort dominates. **Negations void ESR** — `$ne`/`$nin`/`$not` never equality-seek ([005](./005-mongodb-query-language.md)); index the positive shape.

---

## 5. Multikey Indexes (Arrays)

**Purpose:** indexing an array creates one key per element automatically. `tags:["audio","wireless"]` yields two keys → same doc.

```javascript
db.products.createIndex({ tags: 1 });
db.products.find({ tags: "wireless" });       // IXSCAN 1 key → FETCH
db.orders.createIndex({ "items.sku": 1 });    // array of sub-docs (§17)
db.orders.find({ "items.sku": "AUR-HP-001" }); // orders containing SKU
```

Limits: one array field per compound (§4); multikey fields rarely `cover` (§14 — plan still shows `FETCH`); `$size` never uses an index — materialize `tagsCount` for hot length queries; empty arrays contribute zero keys (§18).

---

## 6. Unique Indexes + Duplicate-Key Handling (→ 409)

**Purpose:** storage-layer "one doc per value" (`sku`, idempotency key) — the only race-safe double-insert guard ([005](./005-mongodb-query-language.md) §14).

```javascript
db.products.createIndex({ sku: 1 }, { unique: true, name: "uq_products_sku" });
db.orders.createIndex({ idempotencyKey: 1 },
  { unique: true, partialFilterExpression: { idempotencyKey: { $exists: true } } });
// Second insert with same sku → E11000 duplicate key
```

```java
// Annotation form (needs §16 bootstrap to take effect)
public class Product { @Indexed(unique = true) private String sku; }
// Duplicate-key → 409 (commerce-mesh GlobalExceptionHandler pattern)
@RestControllerAdvice public class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiError> onDuplicate(DuplicateKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError(409, "Duplicate key: SKU already exists"));
    }
    public record ApiError(int status, String message) {}
}
```

Upsert races: two concurrent `placeOrder` calls with the same idempotency key both miss, both insert, one gets `E11000` — catch it and re-read the winner. Never check-then-insert; the unique index IS the check.

---

## 7. Partial Indexes

**Purpose:** index only matching docs — smaller, faster, and the safe way to scope uniqueness (soft-delete, active-only).

```javascript
db.products.createIndex({ categoryId: 1, price: 1 }, { partialFilterExpression: { isActive: true } });
db.products.createIndex({ sku: 1 }, { unique: true, partialFilterExpression: { deletedAt: { $exists: false } } });
```

Catch: the query MUST imply the filter. `find({categoryId:"X"})` ignores the first index; `find({categoryId:"X", isActive:true})` uses it. Always include the predicate in hot queries ([006](./006-data-modeling-schema-design.md) §18).

---

## 8. Sparse vs Partial (Table)

**Purpose:** both shrink indexes by skipping docs — `sparse` is legacy, `partial` is the successor. Prefer `partial` for new code.

| Aspect | `sparse: true` | `partialFilterExpression` |
|---|---|---|
| Skips | Docs MISSING the field | Any predicate (`{isActive:true}`, ranges) |
| Query must contain | Nothing special | Must imply the filter or planner skips the index |
| Unique + missing | Multiple missing allowed (null may collide) | Precise: uniqueness only among live docs |
| Compound | Skips docs missing ANY field (surprising) | One shared predicate |
| TTL / text / geo | TTL forbids sparse | TTL forbids partial too — standalone TTL field (§9) |
| Verdict | Legacy only | Default for soft-delete / active-only / tenant indexes |

```javascript
db.products.createIndex({ compareAtPrice: 1 }, { sparse: true }); // OLD
db.products.createIndex({ compareAtPrice: 1 }, { partialFilterExpression: { compareAtPrice: { $exists: true } } }); // NEW
```

---

## 9. TTL Indexes

**Purpose:** auto-delete after a timestamp — guest carts, stock holds, tokens ([006](./006-data-modeling-schema-design.md) §19). Background sweep ≈ every 60s; expiry is eventual, not exact.

```javascript
db.carts.createIndex({ expiresAt: 1 }, { expireAfter: 0 });          // absolute: deleted ~60s after expiresAt
db.reservations.createIndex({ createdAt: 1 }, { expireAfter: 900 }); // relative: 15-min holds
```

```java
@Document(collection = "reservations") public class StockReservation {
    @Indexed(expireAfter = "900s") private Instant createdAt;
}
```

Rules: TTL field must be a BSON date (arrays: earliest wins); single-field only, no compound/partial/sparse TTL, `_id` can't be TTL, capped collections ignore TTL. For orders/money prefer explicit retention jobs ([012](./012-production-readiness.md)) — TTL deletes are un-audited and un-transactional.

---

## 10. Text Indexes + search-service Example

**Purpose:** stemmed, scored search (`"wireless headphones"` matches `"Wireless Headphone"`). Fix for leading-wildcard `$regex` (§23 case 2). One text index per collection; it tokenizes — NOT a substring index. Mirrors commerce-mesh `search-service` (`ProductDocument` + `SearchService` + catalog `MongoConfig.product_text_search`).

```java
// search-service ProductDocument — weights mirror the real service
@Document(collection = "products") public class ProductDocument {
    @Id private String id;
    @TextIndexed(weight = 10) private String name;       // title hit ≫ body hit
    @TextIndexed(weight = 5) private String description; // stemmed body
    private String category; private BigDecimal price; private boolean isActive;
    @TextScore private Float textScore; // for sortByScore
}
// SearchService.textSearch: $text + filter + score sort
TextCriteria criteria = TextCriteria.forDefaultLanguage().matching(keyword);
Query query = TextQuery.queryText(criteria).sortByScore();
query.addCriteria(Criteria.where("isActive").is(true));
return mongoTemplate.find(query, ProductDocument.class);
```

```javascript
db.products.createIndex({ name: "text", description: "text" },
  { name: "product_text_search", weights: { name: 10, description: 5 }, default_language: "english" });
db.products.find({ $text: { $search: "wireless headphones" } }, { score: { $meta: "textScore" } })
  .sort({ score: { $meta: "textScore" } });
```

Limits: one `$text` per query, first aggregation stage only; extra filters post-scan (add compounds for faceted browse, §14); `"aur"` does NOT substring-match `"aurora"` — prefix autocomplete is anchored `^` regex on `{name:1}` (§23) or Atlas `$search`. See [002](./002-document-mapping.md) §8.

---

## 11. Geospatial 2dsphere Indexes

**Purpose:** "warehouses near me", delivery-radius checks. `2dsphere` = GeoJSON + spherical math; legacy `2d` is obsolete.

```javascript
db.warehouses.createIndex({ location: "2dsphere" });
db.warehouses.find({ location: { $near: { $geometry: { type: "Point", coordinates: [-97.75, 30.27] }, $maxDistance: 10000 } } });
db.orders.find({ "shippingAddress.location": { $geoWithin: { $centerSphere: [[-97.7431, 30.2672], 5 / 3963.2] } } });
```

```java
public class Warehouse {
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE) private double[] location; // {lng,lat}
}
```

Rules: coordinates are `[lng, lat]` — swapped values query the wrong hemisphere; one `$near`/`$geoNear` per query and it must be FIRST; compound geo allowed (`{city:1, location:"2dsphere"}`); every doc needs the same GeoJSON shape (§18).

---

## 12. Wildcard Indexes

**Purpose:** index unpredictable schemaless keys — `attributes:{color,size,…}` where sellers invent keys weekly. One `$**` covers all sub-paths.

```javascript
db.products.createIndex({ "attributes.$**": 1 }, { name: "wx_attributes" });
db.products.find({ "attributes.color": "black" }); // IXSCAN wildcard, no new index needed
db.products.createIndex({ "attributes.$**": 1 }, { wildcardProjection: { "attributes.color": 1 } }); // scoped, cheaper
```

Trade-offs: WIDE (every sub-key = keys) — fine for admin/ops, wrong for the hot storefront (explicit compounds per §4); no `unique`, no compound-prefix composition, no text scoring. Put `@Indexed` on the top-3 known keys + wildcard as backstop.

---

## 13. Hashed Indexes

**Purpose:** uniform hash distribution — sharding + O(1) point lookups. Equality ONLY: no ranges, sorts, or prefixes.

```javascript
db.products.createIndex({ sku: "hashed" });
db.products.find({ sku: "AUR-HP-001" });  // IXSCAN hash seek — fast
db.products.find({ sku: { $gte: "A" } }); // COLLSCAN — hash destroys order!
sh.shardCollection("mesh.products", { sku: "hashed" });
```

Use for high-cardinality shard keys (`sku`/`customerId` write fan-out) or equality-only idempotency lookups at extreme QPS. Never when the query sorts, ranges, or prefixes on the field.

---

## 14. Covered Queries + Selectivity + Intersection

**Purpose:** three planner concepts deciding whether an index pays. Covered = answered from keys alone (`totalDocsExamined: 0`, `PROJECTION_COVERED`):

```javascript
db.products.createIndex({ categoryId: 1, price: 1 });
db.products.find({ categoryId: "cat-audio" }, { categoryId: 1, price: 1, _id: 0 }); // COVERED (no FETCH)
db.products.find({ categoryId: "cat-audio" }, { name: 1 }); // NOT covered → FETCH
```

Selectivity: lead with high cardinality — `sku` (2M) > `categoryId` (200) > `status` (5) > `isActive` (2). A lone boolean is never worth an index; fold it into a compound tail or a partial predicate (§7). Intersection (`AND_HASH` of two indexes) is the planner's fallback when no compound exists — treat it as a work order to build the compound (`{categoryId:1, price:1}` = one ordered seek vs two seeks + hash join). Prefer compound, always.

---

## 15. Storage and Write Overhead

**Purpose:** numbers on §1 rents for review boards.

```javascript
db.products.totalSize(); db.products.dataSize();
db.products.stats().indexSizes; // per-index bytes
```

Rules of thumb (7.x, WiredTiger+snappy): narrow keys ≈ 10–30% of `dataSize` each, text/wildcard 50%+; 200 QPS `placeOrder` × 8 order indexes ≈ 1.6k index writes/s before the doc write; updates to NON-indexed fields skip index maintenance — co-locate churn in unindexed fields ([006](./006-data-modeling-schema-design.md) §10). Hard cap 64 indexes/collection; pain starts at ~10. Drop zero-op indexes via `$indexStats` (§22).

---

## 16. createIndex / dropIndex + Spring @Indexed / @CompoundIndex + MongoConfig Bootstrap

**Purpose:** full lifecycle — shell DDL, annotations, and the commerce-mesh bootstrap making annotations real.

```javascript
db.products.createIndex({ sku: 1 }, { unique: true, name: "uq_products_sku" });
db.orders.createIndex({ customerId: 1, createdAt: -1 }, { name: "idx_orders_customer_created" });
db.products.dropIndex("idx_stale_price_only"); // by name in runbooks
db.products.hideIndex("idx_candidate"); db.products.unhideIndex("idx_candidate"); // 7.x+ A/B test
```

```java
// @Indexed + @CompoundIndex declarations (see [002](./002-document-mapping.md) §6–§7)
@Document(collection = "products")
@CompoundIndexes(@CompoundIndex(name = "idx_products_category_price", def = "{'categoryId': 1, 'price': 1}"))
public class Product { @Indexed(unique = true) private String sku; private String categoryId; }
```

```java
// MongoConfig bootstrap — mirrors commerce-mesh product-catalog-service MongoConfig
@Configuration @EnableMongoAuditing public class MongoConfig {
    private final MongoTemplate mongoTemplate;
    public MongoConfig(MongoTemplate mongoTemplate) { this.mongoTemplate = mongoTemplate; }
    @PostConstruct public void initIndexes() {
        IndexOperations ops = mongoTemplate.indexOps(Product.class); // annotated @Indexed/@CompoundIndex
        IndexResolver resolver = new MongoPersistentEntityIndexResolver(
                mongoTemplate.getConverter().getMappingContext());
        resolver.resolveIndexFor(Product.class).forEach(ops::ensureIndex);
        // Weighted text index has no annotation equivalent — raw driver DDL, idempotent:
        MongoDatabase db = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase();
        db.getCollection("products").createIndex(new Document("name", "text").append("description", "text"),
            new IndexOptions().name("product_text_search").background(true));
        // Orders compound via IndexOperations:
        mongoTemplate.indexOps(Order.class).ensureIndex(new CompoundIndexDefinition(
            new Document("customerId", 1).append("createdAt", -1)).named("idx_orders_customer_created"));
    }
}
```

`ensureIndex`/`createIndex` are idempotent; changing options needs `dropIndex` first; commerce-mesh uses explicit `@PostConstruct` over `auto-index-creation` so indexes are code-reviewed; heavy builds run in maintenance windows ([012](./012-production-readiness.md)).

---

## 17. Nested Fields and Array-Element Indexes

**Purpose:** dot-notation reaches into sub-docs and arrays — `items.sku`, `shippingAddress.zip`.

```javascript
db.orders.createIndex({ "shippingAddress.zip": 1 });     // nested object
db.orders.createIndex({ "items.sku": 1 });                // multikey: key per line item
db.orders.createIndex({ customerId: 1, "items.sku": 1 }); // ONE array field max (§4)
db.orders.find({ "items": { $elemMatch: { sku: "AUR-HP-001", qty: { $gte: 2 } } } });
```

Index finds candidates, `$elemMatch` verifies ONE element satisfies all clauses — without it, `{"items.sku":"X","items.qty":2}` can match different elements ([005](./005-mongodb-query-language.md) §6). Bucket stale items out of the hot doc ([006](./006-data-modeling-schema-design.md) §9) to keep the multikey narrow.

---

## 18. Missing / Null Behavior

**Purpose:** `null` vs missing decides matches and collisions — phantom reads + surprise `E11000`s ([005](./005-mongodb-query-language.md) §11).

```javascript
db.products.insertMany([{ _id: 1, brand: "Aurora" }, { _id: 2, brand: null }, { _id: 3 }]);
db.products.find({ brand: null });                         // → 2 AND 3 (missing counts as null!)
db.products.find({ brand: { $eq: null, $exists: true } }); // → 2 only
db.products.find({ brand: { $exists: false } });           // → 3 only
```

Regular indexes contain null/missing entries (queryable); `sparse` skips missing; `partial` contains only predicate matches (query must imply it, §7); multikey skips null slots and `[]` contributes zero keys (invisible to the index). Normalize one convention per collection: explicit `brand:null` on write OR `$exists` in queries.

---

## 19. Pagination Indexes (Sort + _id Tiebreaker)

**Purpose:** stable fast paging — `skip()` re-scans, keyset seeks. `_id` tiebreaker pins rows sharing a sort key.

```javascript
db.orders.createIndex({ customerId: 1, createdAt: -1, _id: -1 });
// SKIP: totalKeysExamined ≈ 100020 for 20 rows — page 5000 is 5000× slower than page 1
db.orders.find({ customerId: "usr-42" }).sort({ createdAt: -1, _id: -1 }).skip(100000).limit(20);
// KEYSET: totalKeysExamined ≈ 20 — every page costs one page
db.orders.find({ customerId: "usr-42",
  $or: [{ createdAt: { $lt: ISODate("2026-06-10T09:00:00Z") } },
        { createdAt: ISODate("2026-06-10T09:00:00Z"), _id: { $lt: "ORD-9000" } }] })
  .sort({ createdAt: -1, _id: -1 }).limit(20);
```

```java
// Keyset slice: no COUNT, no OFFSET — client keeps (lastCreatedAt, lastId)
Criteria c = Criteria.where("customerId").is(customerId);
if (lastSeen != null) c.andOperator(new Criteria().orOperator(Criteria.where("createdAt").lt(lastSeen),
    new Criteria().andOperator(Criteria.where("createdAt").is(lastSeen), Criteria.where("_id").lt(lastId))));
return mongoTemplate.find(new Query(c).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(20), Order.class);
```

Without a unique tail, boundary rows duplicate or vanish. Return `hasNext` (fetch `limit+1`) instead of `countDocuments` totals on infinite scroll. Full rewrite with plans: §23 case 3.

---

## 20. Aggregation Index Use

**Purpose:** pipelines reuse B-trees only in early `$match`/`$sort`/`$limit`. After a blocking stage (`$group`, `$lookup`, `$unwind`) everything is in-memory.

```javascript
// Index-friendly: match + sort first, limit coalesced to top-K from index
db.orders.aggregate([
  { $match: { customerId: "usr-42", status: "SHIPPED" } }, // IXSCAN
  { $sort: { createdAt: -1 } }, { $limit: 20 },           // index order, no SORT stage
  { $lookup: { from: "products", localField: "items.sku", foreignField: "sku", as: "catalog" } }
]);
// Index-hostile: $group first destroys order; late $match scans grouped output
db.orders.aggregate([{ $group: { _id: "$status", revenue: { $sum: "$total" } } },
  { $match: { revenue: { $gte: 1000 } } }, { $sort: { revenue: -1 } }]);
```

`$text`/`$geoNear` must be FIRST stages; move `$match` before `$lookup`/`$unwind` so joins fan out from 20 docs not 2M; adjacent `$sort`+`$limit` enables top-K optimization. Early `$project` does NOT create coverage — verify with `explain("executionStats")` on `aggregate()`. More in [007](./007-aggregation-framework.md).

---

## 21. explain(): executionStats, IXSCAN vs COLLSCAN, Winning Plan

**Purpose:** the planner's receipt — which index won, keys/docs touched, sort free or not.

```javascript
db.products.explain("executionStats").find({ categoryId: "cat-audio" }).sort({ price: 1 });
// .explain() === .explain("queryPlanner"); "executionStats" (or "allPlansExecution") adds timings.
```

```json
{ "queryPlanner": { "winningPlan": { "stage": "FETCH",
  "inputStage": { "stage": "IXSCAN", "indexName": "categoryId_1_price_1", "direction": "forward" } } },
  "executionStats": { "nReturned": 20, "totalKeysExamined": 20, "totalDocsExamined": 20, "executionTimeMillis": 1 } }
```

| Signal | Meaning | Action |
|---|---|---|
| `IXSCAN`+`FETCH`, keys ≈ docs ≈ nReturned | Ideal point/range | Ship it |
| `IXSCAN`, docs ≫ nReturned | Low selectivity, filter discards most | Reorder lead (§14) / add ESR equality |
| `COLLSCAN` | No usable index | Build compound (§3–§4) / check predicate mismatch |
| `SORT` stage | In-memory sort (100 MiB cap) | Add sort to compound (§4) |
| `TEXT` / `GEO_NEAR_2DSPHERE` | Special index, valid first-only | Extra filters post-scan (§10–§11) |
| `PROJECTION_COVERED`, docs 0 | Covered (§14) | Best case |
| `rejectedPlans` | Tried-and-discarded alternatives | Persistent rejects → drop loser |

`executionTimeMillis` (microsecond `executionTimeMicros` on newer servers) caveats: server-side only (excludes network, driver decode, replica lag); first run is cold (10–100× inflated); single-ms diffs are noise — compare key/doc ratios, not one timing; plan cache can flip choices under load, re-run twice. `winningPlan` is cached; `rejectedPlans` shows losers. `hint()` pins plans for diagnostics only — never in app code (rots when indexes change).

---

## 22. Profiling (Level 2 + system.profile) and the Slow Log

**Purpose:** find slow queries in production instead of guessing.

```javascript
// Levels: 0 off | 1 slow-only (> slowms) | 2 ALL ops (bursts only, never prod steady-state)
db.setProfilingLevel(1, { slowms: 50 }); // production: ops slower than 50ms
db.setProfilingLevel(2);                 // staging/load-test: everything
db.system.profile.find({ millis: { $gt: 100 } }, { op: 1, ns: 1, millis: 1, planSummary: 1 }).sort({ ts: -1 }).limit(10);
// planSummary COLLSCAN → unindexed (§21); IXSCAN-but-slow → FETCH-heavy or lock.
db.setProfilingLevel(0); // ALWAYS reset level 2 — costs ~5–10% throughput + disk
db.products.aggregate([{ $indexStats: {} }]); // ops: 90000 vs 0 → drop zero-op indexes (§15)
db.adminCommand({ setParameter: 1, slowOpThresholdMs: 50 }); // align mongod slow log with profiler
```

Workflow: slow-log alert → `ns`+`command` → replay in staging with `explain` (§21) → build compound (§3–§4) → verify `IXSCAN`, keys ≈ returned → deploy via §16 → watch `$indexStats`. Never build directly on the prod primary — runbook + `background` builds ([012](./012-production-readiness.md)).

---

## 23. Three Before / After Slow Queries (With Plans)

**Purpose:** the three fixes behind ~80% of product/order performance tickets.

Case 1 — unindexed sort → compound (browse category sorted by price):

```javascript
// BEFORE: IXSCAN + in-memory SORT — keys 12000, docs 12000, nReturned 500
db.products.explain("executionStats").find({ categoryId: "cat-audio" }).sort({ price: 1 });
// AFTER: one ESR compound, NO SORT stage — keys 500, docs 500, ~38ms → ~2ms
db.products.createIndex({ categoryId: 1, price: 1 }, { name: "idx_products_category_price" });
db.products.explain("executionStats").find({ categoryId: "cat-audio" }).sort({ price: 1 });
```

Case 2 — `$regex` leading wildcard → text (search "wireless"):

```javascript
// BEFORE: COLLSCAN — docs 2,000,000, nReturned 34, ~1800ms
db.products.explain("executionStats").find({ name: { $regex: ".*wireless.*", $options: "i" } });
// AFTER: TEXT → FETCH — keys 210, docs 34, ~6ms (mirrors SearchService, §10)
db.products.createIndex({ name: "text", description: "text" },
  { name: "product_text_search", weights: { name: 10, description: 5 } });
db.products.explain("executionStats").find({ $text: { $search: "wireless" } },
  { score: { $meta: "textScore" } }).sort({ score: { $meta: "textScore" } });
// Prefix autocomplete stays anchored regex on {name:1}: find({name:/^wire/}) → IXSCAN.
```

Case 3 — skip-deep → keyset (order history page 5000):

```javascript
// BEFORE: SKIP — keys 100020, nReturned 20, ~240ms
db.orders.explain("executionStats").find({ customerId: "usr-42" })
  .sort({ createdAt: -1, _id: -1 }).skip(100000).limit(20);
// AFTER: keyset seek — keys 20, nReturned 20, ~2ms (§19; client keeps lastCreatedAt+lastId)
db.orders.explain("executionStats").find({ customerId: "usr-42",
  $or: [{ createdAt: { $lt: ISODate("2026-06-10T09:00:00Z") } },
        { createdAt: ISODate("2026-06-10T09:00:00Z"), _id: { $lt: "ORD-9000" } }] })
  .sort({ createdAt: -1, _id: -1 }).limit(20);
```

---

## 24. Pitfalls (GOLD)

!!! warning "GOLD — indexing & performance pitfalls (read before shipping)"
    - **Annotations without bootstrap create nothing** — `@Indexed`/`@CompoundIndex` need the §16 resolver.
    - **Compound order is the contract** — `{price:1, categoryId:1}` doesn't serve `find({categoryId})` (§3).
    - **Sort direction must match** — mixed sorts on uniform indexes spill to `SORT` (§3–§4).
    - **Range kills the tail** — sort goes BEFORE range in ESR; one array field per compound (§4).
    - **`$ne`/leading-`.*regex` void indexes** — anchor (`^wire`) or move to `$text` (§10, §23).
    - **Partial without the predicate is invisible** — query must imply `partialFilterExpression` (§7).
    - **Unique + null collides** — scope nullable uniqueness with partial (§6, §18); `DuplicateKeyException` → 409.
    - **TTL is eventual + un-audited** — ~60s sweep, date-only, no compound/partial; jobs for money (§9).
    - **One text index per collection** — fold fields into one weighted index (§10); `[lng,lat]` not `[lat,lng]` (§11).
    - **Wildcard/hashed compose with nothing** — hot paths get explicit compounds (§12–§13).
    - **`{field:null}` matches missing too** — add `$exists:true` for explicit-null-only (§18).
    - **Deep `skip` is linear** — keyset + `_id` tiebreaker past ~1k docs (§19, §23 case 3).
    - **`$group`/`$lookup` first blinds indexes** — `$match`/`$sort` lead ([007](./007-aggregation-framework.md), §20).
    - **`executionTimeMillis` is server-only** — compare key/doc ratios, not single timings (§21); level 2 profiler off after use (§22).
    - **>~10 indexes is a smell** — `$indexStats` audit, drop zero-op; every index taxes writes (§15).

---

## 25. Interview Q&A

**Q1. `explain("executionStats")` shows `COLLSCAN`, `totalDocsExamined: 2,000,000`, `nReturned: 20` — diagnose and fix?**
A: No usable index — full walk, 20 survivors (§21). Check leftmost-prefix compound (§3), partial predicate implied (§7), predicate/index compatibility (`$ne`, leading regex, collation). Fix: ESR compound (`{categoryId:1, price:1}`), re-`explain` → `IXSCAN`, keys ≈ docs ≈ 20, no `SORT` (§23 case 1).

**Q2. ESR order — and what breaks when a range precedes the sort?**
A: Equality filters, Sort rides the index order free, Range bounds the tail (§4). A range first ends efficient traversal — trailing fields can't seek or order, adding in-memory `SORT` (100 MiB cap). Fix: sort field before range, or split indexes.

**Q3. Two `placeOrder` calls share an idempotency key; one gets `E11000` — bug? API response?**
A: Design (§6): unique index serialized the race. Catch `DuplicateKeyException` → `409 Conflict`, re-read the winner by idempotency key. Never check-then-insert.

**Q4. Order history page 1 is 3ms, page 5000 is 400ms — fix without row instability?**
A: `skip(100000)` re-examines 100k keys (§19, §23 case 3). Keyset on `{customerId:1, createdAt:-1, _id:-1}`: `createdAt < last OR (== last AND _id < lastId)`, `limit(20)`. `_id` tiebreaker pins same-millisecond rows.

**Q5. `{name:/.*wireless/i}` scans 2M docs; PM wants ranking + autocomplete?**
A: Leading-wildcard regex can't IXSCAN (§23 case 2). One weighted text index + `$text`/`$meta textScore` sort for relevance (search-service `ProductDocument`, §10); separate `{name:1}` for anchored `^wire` autocomplete. `$text` is stemmed-word, not substring — set expectations.

---

## 26. Resources

**This track:** [002 — Document Mapping](./002-document-mapping.md) (`@Indexed`/`@CompoundIndex`/`@TextIndexed`, auditing) · [005 — MongoDB Query Language](./005-mongodb-query-language.md) (operators, null-vs-missing, `$regex`, upsert races) · [007 — Aggregation Framework](./007-aggregation-framework.md) (pipeline index use, first-stage rules) · [012 — Production Readiness](./012-production-readiness.md) (build runbooks, slow-log alerts, `$indexStats`) · [MongoDB intro](./mongodb.md) (BSON, collections, index kinds, `mongosh`).

**Official docs:** [Indexes](https://www.mongodb.com/docs/manual/indexes/) · [Compound + Prefixes](https://www.mongodb.com/docs/manual/core/index-compound/) · [ESR](https://www.mongodb.com/docs/manual/tutorial/optimize-query-performance-with-indexes-and-projections/) · [Multikey](https://www.mongodb.com/docs/manual/core/index-multikey/) · [Unique](https://www.mongodb.com/docs/manual/core/index-unique/) · [Partial](https://www.mongodb.com/docs/manual/core/index-partial/) · [Sparse](https://www.mongodb.com/docs/manual/core/index-sparse/) · [TTL](https://www.mongodb.com/docs/manual/core/index-ttl/) · [Text](https://www.mongodb.com/docs/manual/core/index-text/) · [2dsphere](https://www.mongodb.com/docs/manual/core/2dsphere/) · [Wildcard](https://www.mongodb.com/docs/manual/core/index-wildcard/) · [Hashed](https://www.mongodb.com/docs/manual/core/index-hashed/) · [explain](https://www.mongodb.com/docs/manual/reference/command/explain/) · [Profiler](https://www.mongodb.com/docs/manual/tutorial/manage-the-database-profiler/) · [Spring Data MongoDB — Indexing](https://docs.spring.io/spring-data-mongodb/reference/mapping/indexes.html)

---

*Next: [007 — Aggregation Framework](./007-aggregation-framework.md) · production runbooks: [012 — Production Readiness](./012-production-readiness.md).*

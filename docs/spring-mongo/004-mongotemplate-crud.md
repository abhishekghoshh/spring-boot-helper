# 004 — MongoTemplate CRUD (Boot 4.1.0 + Java 25, Product / Order Domain)

> Section D of the Spring Mongo track: imperative CRUD with `MongoTemplate` — reads, writes, updates, upserts, `findAndModify`, deletes, counts, projections, paging, text search, aggregation, bulk, collections, exceptions — on the product/order domain.
>
> **Stack:** Spring Boot **4.1.0** · Java **25** · Spring Data MongoDB 5.x · MongoDB **7.x**.
>
> **Convention mirror — verified by reading, not compiling.** Read/search patterns mirror `search-service/.../search/service/SearchService.java` (`MongoTemplate` + `Criteria`/`Query` + `TextCriteria`/`TextQuery` + `Pageable`) and catalog shapes from [003](./003-repository-query-methods.md). Snippets were **not compiled.**
>
> Companion guides: [003](./003-repository-query-methods.md) · [005](./005-mongodb-query-language.md) · [007](./007-aggregation-framework.md) · [MongoDB intro](./mongodb.md).

## Table of Contents

- [1. Domain and wiring](#1-domain-and-wiring) · [2. Reads](#2-reads-findfindonefindbyid) · [3. Writes](#3-writes-insertsaveinsertall) · [4. Criteria](#4-querycriteria-builder-depth) · [5. Update](#5-update-builder-depth) · [6. updateFirst/updateMulti/upsert](#6-updatefirstupdatemultiupsert) · [7. findAndModify](#7-findandmodify--atomic-counter) · [8. Deletes](#8-findandremove--remove) · [9. count/exists](#9-countexists) · [10. Projections](#10-projections) · [11. Sorting/pagination](#11-sortingpagination) · [12. Text search](#12-text-search-searchservice-mirror) · [13. Aggregation](#13-aggregation-via-mongotemplateaggregate) · [14. Bulk](#14-bulk-bulkoperations) · [15. Collections](#15-collection-ops) · [16. Exceptions](#16-exception-translation) · [17. Repo vs template](#17-repo-vs-template-decision-table) · [18. Pitfalls](#18-pitfalls-gold) · [19. Q&A](#19-interview-qa) · [20. Resources](#20-resources)

```xml
<!-- parent/java (Boot 4.1.0 + Java 25) + data-mongodb -->
<parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>4.1.0</version></parent>
<properties><java.version>25</java.version></properties>
<!-- + org.springframework.boot:spring-boot-starter-data-mongodb -->
```

---

## 1. Domain and wiring

Catalog `Product` (`products`) plus a Mongo-shaped `Order` twin (`orders`) for nested/array ops — real `order-service` in this repo is JPA, so `Order` below is illustrative only (mapping detail in [002](./002-document-mapping.md)).

```java
// java — documents
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class Product {
    @MongoId private String id;
    private String name; private String description; private String brand;
    private String sku; // @Indexed(unique = true)
    private java.math.BigDecimal price; private String categoryId;
    private java.util.List<String> tags = new java.util.ArrayList<>();
    private java.util.Map<String, String> attributes = new java.util.HashMap<>();
    private double averageRating; private boolean isActive = true;
}
@Document(collection = "orders")
public class Order {
    @org.springframework.data.annotation.Id private String id;
    private String customerId; private String status; // PENDING | PAID | SHIPPED
    private java.util.List<Item> items = new java.util.ArrayList<>();
    public record Item(String sku, int qty, java.math.BigDecimal price) {}
}
```

```javascript
// mongosh — collections this guide writes to
db.products.findOne({ sku: "sku-1001" })
db.orders.findOne({ status: "PENDING" })
// → products.sku unique; orders.customerId plain reference, no join.
```

```java
// java — inject the template (constructor injection, same as SearchService)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
@Service
public class CatalogTemplateService {
    private final MongoTemplate mongoTemplate;
    public CatalogTemplateService(MongoTemplate mongoTemplate) { this.mongoTemplate = mongoTemplate; }
}
```

```javascript
// mongosh — template == authenticated connection; no wiring needed
db.runCommand({ connectionStatus: 1 })
// → MongoTemplate maps POJO <-> BSON and translates exceptions (§16).
```

---

## 2. Reads — find/findOne/findById

```java
// java — find many (empty list, never null) + findOne/findById (single doc or null)
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.util.List;
Query q = new Query(Criteria.where("categoryId").is("footwear").and("isActive").is(true));
List<Product> hits = mongoTemplate.find(q, Product.class);
Product one = mongoTemplate.findOne(Query.query(Criteria.where("sku").is("sku-1001")), Product.class);
Product byId = mongoTemplate.findById("689f1a2b3c4d5e6f7a8b9c0d1", Product.class);
```

```javascript
// mongosh
db.products.find({ categoryId: "footwear", isActive: true })
db.products.findOne({ sku: "sku-1001" }) // null when missing
db.products.findOne({ _id: ObjectId("689f1a2b3c4d5e6f7a8b9c0d1") })
// → find→cursor/list; findOne/findById→doc or null (wrap Optional at service boundary).
```

---

## 3. Writes — insert/save/insertAll

| Op | Id | Exists? | Result |
|---|---|---|---|
| `insert(doc)` | set/null | must **not** exist | `insertOne`; dup `_id`/`sku` → `DuplicateKeyException` |
| `save(doc)` | null | — | generates id, **inserts** |
| `save(doc)` | set | missing | **inserts** (upsert-by-id) |
| `save(doc)` | set | exists | **replaces whole doc** |
| `insertAll(list)` | any | must be new | ordered `insertMany`; no pre-read, fastest import |

```java
// java — insert (strict create, id populated in place)
import com.commercemesh.catalog.document.Product;
Product p = new Product(); p.setSku("sku-1001"); p.setName("Trail Sneakers");
Product inserted = mongoTemplate.insert(p);
```

```javascript
// mongosh
db.products.insertOne({ sku: "sku-1001", name: "Trail Sneakers", isActive: true })
// → duplicate sku-1001 → E11000; template raises DuplicateKeyException (§16).
```

```java
// java — save (upsert-by-id, full replace) + insertAll (bulk import)
import com.commercemesh.catalog.document.Product;
p.setPrice(new java.math.BigDecimal("79.99"));
Product saved = mongoTemplate.save(p); // null id→insert; set id→replace
mongoTemplate.insertAll(loadCatalogChunk(5_000)); // ordered insertMany, throws on first dup
```

```javascript
// mongosh
db.products.replaceOne({ _id: ObjectId("...") }, { sku: "sku-1001", price: NumberDecimal("79.99") }, { upsert: true })
db.products.insertMany([{ sku: "sku-9001" }, { sku: "sku-9002" }], { ordered: true })
// → save overwrites omitted fields (patch via $set §6); import→insertAll, sync/merge→bulk UPSERT (§14).
```

---

## 4. Query/Criteria builder depth

Property names are **Java fields** (resolved via `@Field`); values convert (`BigDecimal`→`Decimal128`).

```java
// java — and / or / nor
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
Query and = new Query(new Criteria().andOperator(Criteria.where("isActive").is(true), Criteria.where("categoryId").is("footwear")));
Query or = new Query(new Criteria().orOperator(Criteria.where("name").regex("trail", "i"), Criteria.where("description").regex("trail", "i")));
Query nor = new Query(new Criteria().norOperator(Criteria.where("status").is("CANCELLED"), Criteria.where("isActive").is(false)));
mongoTemplate.find(and, Product.class);
```

```javascript
// mongosh
db.products.find({ $and: [{ isActive: true }, { categoryId: "footwear" }] })
db.products.find({ $or: [{ name: /trail/i }, { description: /trail/i }] })
db.products.find({ $nor: [{ status: "CANCELLED" }, { isActive: false }] })
// → implicit AND ({a,b}) == $and top-level; $or/$nor need explicit operator.
```

```java
// java — in, gte/lte, regex, exists, type, elemMatch + dot notation
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
Query q = new Query(Criteria.where("tags").in("trail", "sale").and("price").gte(50).lte(100)
    .and("name").regex("^trail", "i").and("brand").exists(true).and("price").type(org.bson.BsonType.DECIMAL128));
Query nested = new Query(Criteria.where("items").elemMatch(Criteria.where("sku").is("sku-1001").and("qty").gte(2)));
mongoTemplate.find(q, Product.class); mongoTemplate.find(nested, Order.class);
mongoTemplate.find(new Query(Criteria.where("attributes.color").is("red")), Product.class);
```

```javascript
// mongosh
db.products.find({ tags: { $in: ["trail", "sale"] }, price: { $gte: 50, $lte: 100 }, name: { $regex: "^trail", $options: "i" }, brand: { $exists: true }, price: { $type: "decimal" } })
db.orders.find({ items: { $elemMatch: { sku: "sku-1001", qty: { $gte: 2 } } } })
db.products.find({ "attributes.color": "red" })
// → $in==any-match (multikey); multi-cond on ONE element needs $elemMatch; plain path matches ANY element ([005](./005-mongodb-query-language.md)).
```

---

## 5. Update builder depth

```java
// java — every Update operator used in this guide
import org.springframework.data.mongodb.core.query.Update;
Update u = new Update().set("price", new java.math.BigDecimal("89.99")).unset("compareAtPrice")
    .inc("reviewCount", 1).push("tags", "sale").pull("tags", "clearance").addToSet("tags", "trail")
    .rename("brand", "brandName").currentDate("updatedAt");
Update capped = new Update().push("tags").each(java.util.List.of("a", "b")).slice(10).build();
```

```javascript
// mongosh — exact equivalents
db.products.updateOne({ sku: "sku-1001" }, { $set: { price: NumberDecimal("89.99") }, $unset: { compareAtPrice: "" }, $inc: { reviewCount: 1 }, $push: { tags: "sale" }, $pull: { tags: "clearance" }, $addToSet: { tags: "trail" }, $rename: { brand: "brandName" }, $currentDate: { updatedAt: true } })
db.products.updateOne({ sku: "sku-1001" }, { $push: { tags: { $each: ["a", "b"], $slice: 10 } } })
// → atomic single-doc ops; $rename rewrites every match; cap $push arrays (16 MB doc limit).
```

---

## 6. updateFirst/updateMulti/upsert

```java
// java — updateFirst (first match) vs updateMulti (all matches)
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
UpdateResult r1 = mongoTemplate.updateFirst(Query.query(Criteria.where("sku").is("sku-1001")), Update.update("price", new java.math.BigDecimal("89.99")), Product.class);
UpdateResult r2 = mongoTemplate.updateMulti(Query.query(Criteria.where("isActive").is(false)), Update.update("isFeatured", false), Product.class);
```

```javascript
// mongosh
db.products.updateOne({ sku: "sku-1001" }, { $set: { price: NumberDecimal("89.99") } })
db.products.updateMany({ isActive: false }, { $set: { isFeatured: false } })
// → matchedCount 0 + modifiedCount 0 usually means filter missed; large sweeps want bulk (§14) + indexed filter.
```

```java
// java — upsert (update or insert on miss; $setOnInsert fires only on insert)
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
UpdateResult r3 = mongoTemplate.upsert(Query.query(Criteria.where("sku").is("sku-9001")), new Update().set("name", "Fallback Tee").setOnInsert("createdAt", java.time.Instant.now()), Product.class);
String id = r3.getUpsertedId() != null ? r3.getUpsertedId().toString() : "updated-existing";
```

```javascript
// mongosh
db.products.updateOne({ sku: "sku-9001" }, { $set: { name: "Fallback Tee" }, $setOnInsert: { createdAt: new Date() } }, { upsert: true })
// → filter-equality fields seed the new doc; concurrent upserts can race → unique index + retry (§16).
```

---

## 7. findAndModify — atomic counter

`findAndModify` reads **and** writes atomically — the fix for read-then-write races.

```java
// java — returnNew + upsert options, order-sequence counter
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
FindAndModifyOptions opts = FindAndModifyOptions.options().returnNew(true).upsert(true);
Counter c = mongoTemplate.findAndModify(Query.query(Criteria.where("_id").is("orderSeq")), new Update().inc("seq", 1), opts, Counter.class, "counters");
long nextOrderNumber = c.getSeq(); // atomic, gap-free under concurrency
@org.springframework.data.mongodb.core.mapping.Document(collection = "counters") class Counter { @org.springframework.data.annotation.Id private String id; private long seq; public long getSeq() { return seq; } }
```

```javascript
// mongosh
db.counters.findOneAndUpdate({ _id: "orderSeq" }, { $inc: { seq: 1 } }, { upsert: true, returnDocument: "after" })
// → { _id: "orderSeq", seq: 42 }; returnDocument:"before" (default) returns stale value — returnNew(true) is the fix.
```

```java
// java — claim-one-order pattern (exactly one worker wins, losers get null)
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
Order claimed = mongoTemplate.findAndModify(Query.query(Criteria.where("status").is("PENDING")).with(org.springframework.data.domain.Sort.by("_id")).limit(1),
    new Update().set("status", "PROCESSING").currentDate("updatedAt"), FindAndModifyOptions.options().returnNew(false), Order.class);
```

```javascript
// mongosh
db.orders.findOneAndUpdate({ status: "PENDING" }, { $set: { status: "PROCESSING" }, $currentDate: { updatedAt: true } }, { sort: { _id: 1 }, returnDocument: "before" })
// → losers get null → poll/backoff; no transaction needed for single doc.
```

---

## 8. findAndRemove / remove

```java
// java — findAndRemove returns the deleted doc (or null); remove returns counts
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.mongodb.client.result.DeleteResult;
Order removed = mongoTemplate.findAndRemove(Query.query(Criteria.where("_id").is("ord-1")), Order.class);
DeleteResult d = mongoTemplate.remove(Query.query(Criteria.where("status").is("CANCELLED")), Order.class);
long deleted = d.getDeletedCount();
```

```javascript
// mongosh
db.orders.findOneAndDelete({ _id: "ord-1" }) // deleted doc or null
db.orders.deleteMany({ status: "CANCELLED" })
// → findAndRemove when caller needs old values (audit/outbox); else remove(); deletes permanent — or soft-delete via $set isActive:false.
```

---

## 9. count/exists

```java
// java — count + exists (no docs cross the wire)
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
long active = mongoTemplate.count(Query.query(Criteria.where("isActive").is(true)), Product.class);
boolean skuTaken = mongoTemplate.exists(Query.query(Criteria.where("sku").is("sku-1001")), Product.class);
```

```javascript
// mongosh
db.products.countDocuments({ isActive: true })
db.products.countDocuments({ sku: "sku-1001" }, { limit: 1 }) // >0 → exists
// → exists stops at first index hit; count strips skip/limit — pass a cleared Query (§11).
```

---

## 10. Projections

```java
// java — include card fields / exclude heavy field
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
Query q = Query.query(Criteria.where("isActive").is(true));
q.fields().include("name", "price", "brand").exclude("_id");
java.util.List<Product> cards = mongoTemplate.find(q, Product.class);
Query q2 = new Query(); q2.fields().exclude("description");
mongoTemplate.find(q2, Product.class);
```

```javascript
// mongosh
db.products.find({ isActive: true }, { name: 1, price: 1, brand: 1, _id: 0 })
db.products.find({}, { description: 0 })
// → include+exclude mix allowed ONLY for _id; prefer include-mode on hot paths (smaller payload, covered-query eligible).
```

---

## 11. Sorting/pagination

```java
// java — Pageable carries page + sort; count on cleared copy
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
Pageable p = PageRequest.of(0, 20, Sort.by("createdAt").descending().and(Sort.by("price")));
Query q = new Query(Criteria.where("categoryId").is("footwear")).with(p);
java.util.List<Product> content = mongoTemplate.find(q, Product.class);
Page<Product> page = PageableExecutionUtils.getPage(content, p, () -> mongoTemplate.count(Query.of(q).limit(0).skip(0), Product.class));
```

```javascript
// mongosh — page + hidden totals query
db.products.find({ categoryId: "footwear" }).sort({ createdAt: -1, price: 1 }).skip(0).limit(20)
db.products.countDocuments({ categoryId: "footwear" })
// → Page fires find + count; Slice/List fire find only. Unindexed sort spills (32 MB cap) — index { filter + sort }.
```

```java
// java — Slice-style hasNext probe (N+1 fetch, no count) + keyset alternative
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
Query q2 = Query.query(Criteria.where("isActive").is(true)).with(org.springframework.data.domain.Sort.by("price")).limit(21);
boolean hasNext = mongoTemplate.find(q2, Product.class).size() > 20;
```

```javascript
// mongosh
db.products.find({ isActive: true }).sort({ price: 1 }).limit(21)
db.products.find({ isActive: true, _id: { $gt: lastSeenId } }).sort({ _id: 1 }).limit(20)
// → scroll: fetch N+1, hasNext = overflow; deep offsets prefer keyset (seeks via index, skip() scans+discards) ([005](./005-mongodb-query-language.md)).
```

---

## 12. Text search — SearchService mirror

Direct mirror of `SearchService.textSearch` / `filterSearch` (field names are `ProductDocument` paths: `category`, not `categoryId`).

```java
// java — keyword path: TextCriteria + TextQuery + score sort + filters (verbatim pattern)
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.data.domain.Pageable;
TextCriteria tc = TextCriteria.forDefaultLanguage().matching(keyword);
Query query = TextQuery.queryText(tc).sortByScore().with(pageable);
query.addCriteria(Criteria.where("isActive").is(true));
if (category != null && !category.isBlank()) query.addCriteria(Criteria.where("category").is(category));
if (minPrice != null) query.addCriteria(Criteria.where("price").gte(minPrice.doubleValue()));
if (maxPrice != null) query.addCriteria(Criteria.where("price").lte(maxPrice.doubleValue()));
long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), ProductDocument.class);
java.util.List<ProductDocument> docs = mongoTemplate.find(query, ProductDocument.class);
```

```javascript
// mongosh
db.products.find({ $text: { $search: "trail waterproof" }, isActive: true, category: "footwear", price: { $gte: 50, $lte: 100 } }, { score: { $meta: "textScore" } }).sort({ score: { $meta: "textScore" } }).skip(0).limit(20)
// → one text index per collection (weighted name/description/brand); stemming ("trails"→"trail"); missing index throws.
```

```java
// java — browse path (no keyword): plain Query + Pageable + dynamic Sort (verbatim pattern)
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
Query query = new Query().with(pageable);
query.addCriteria(Criteria.where("isActive").is(true));
if (category != null) query.addCriteria(Criteria.where("category").is(category));
if (sortBy != null) switch (sortBy) {
    case PRICE_ASC -> query.with(Sort.by(Sort.Direction.ASC, "price"));
    case PRICE_DESC -> query.with(Sort.by(Sort.Direction.DESC, "price"));
    case RATING -> query.with(Sort.by(Sort.Direction.DESC, "averageRating"));
    case NEWEST -> query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
}
long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), ProductDocument.class);
java.util.List<ProductDocument> docs = mongoTemplate.find(query, ProductDocument.class);
```

```javascript
// mongosh — same browse newest-first
db.products.find({ isActive: true, category: "footwear" }).sort({ createdAt: -1 }).skip(0).limit(20)
// → Query.of(query) copies filter WITHOUT mutating paging; count on the copy keeps totals right.
```

---

## 13. Aggregation via mongoTemplate.aggregate

Repository `@Aggregation` ([003](./003-repository-query-methods.md) §10) is fixed pipelines; template builds them dynamically. Full stage reference: [007](./007-aggregation-framework.md).

```java
// java — category facet over id set (mirrors ProductSearchRepository.countByCategory)
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
Aggregation agg = newAggregation(match(Criteria.where("_id").in(java.util.List.of("p1", "p2", "p3"))), group("category").count().as("count"));
AggregationResults<CategoryCount> res = mongoTemplate.aggregate(agg, "products", CategoryCount.class);
record CategoryCount(String id, long count) {}
```

```javascript
// mongosh
db.products.aggregate([{ $match: { _id: { $in: ["p1", "p2", "p3"] } } }, { $group: { _id: "$category", count: { $sum: 1 } } }])
// → output _id = category → CategoryCount.id; empty ids → [] (guard before calling); "products" string bypasses entity mapping — spelling matters.
```

---

## 14. Bulk — BulkOperations

```java
// java — one round trip: UPSERT + INSERT + UPDATE (unordered import/sync)
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.bulk.BulkWriteResult;
BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Product.class);
for (Product p : feed) ops.upsert(Query.query(Criteria.where("sku").is(p.getSku())), new Update().set("name", p.getName()).set("price", p.getPrice()).setOnInsert("createdAt", java.time.Instant.now()));
ops.insert(java.util.List.of(newProductA, newProductB));
ops.updateOne(Query.query(Criteria.where("status").is("PENDING")), new Update().set("status", "PAID"));
BulkWriteResult r = ops.execute();
```

```javascript
// mongosh
db.products.bulkWrite([{ updateOne: { filter: { sku: "sku-1" }, update: { $set: { name: "N" }, $setOnInsert: { createdAt: new Date() } }, upsert: true } },
  { insertOne: { document: { sku: "sku-2", name: "M" } } },
  { updateOne: { filter: { status: "PENDING" }, update: { $set: { status: "PAID" } } } }], { ordered: false })
// → UNORDERED continues past dup errors + parallelizes; ORDERED aborts at first error (use for sequenced ledger writes).
```

| Mode | On error | Order | Use for |
|---|---|---|---|
| `ORDERED` | aborts batch | guaranteed | sequenced, dependent writes |
| `UNORDERED` | continues, reports all | parallelized | catalog sync, imports (higher throughput) |

---

## 15. Collection ops

```java
// java — create / list / drop
boolean exists = mongoTemplate.collectionExists("orders");
if (!exists) mongoTemplate.createCollection("orders");
java.util.Set<String> names = mongoTemplate.getCollectionNames();
// mongoTemplate.dropCollection(Order.class); // whole collection + its indexes
```

```javascript
// mongosh
db.createCollection("orders", { validator: { $jsonSchema: { bsonType: "object", required: ["customerId", "items"] } } })
db.getCollectionNames()
db.orders.drop()
// → $jsonSchema validator = DB-level guardrail; drop is irreversible (snapshot first).
```

---

## 16. Exception translation

`MongoTemplate` converts driver errors to Spring's `DataAccessException` hierarchy — catch Spring types, not `MongoException`.

| Trigger | Spring exception | Extends | Handle by |
|---|---|---|---|
| dup `_id`/`sku` insert | `DuplicateKeyException` | `DataIntegrityViolationException` → `NonTransientDataAccessException` | 409 / retry-as-update |
| stale `@Version` save | `OptimisticLockingFailureException` | `ConcurrencyFailureException` → `TransientDataAccessException` | reload + retry |
| transient failover/timeout | `TransientDataAccessException` subtypes | retryable | backoff + retry |
| bad query/validation | `InvalidDataAccessApiUsageException` | `NonTransient…` | fix + fail fast |

```java
// java — duplicate sku → 409; stale @Version → reload + retry
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
try { mongoTemplate.insert(product); }
catch (DuplicateKeyException e) { throw new SkuAlreadyExistsException(product.getSku(), e); }
try { mongoTemplate.save(product); } // @Version Long version on Product
catch (OptimisticLockingFailureException e) { product = mongoTemplate.findById(product.getId(), Product.class); /* merge + retry */ }
```

```javascript
// mongosh — what the driver saw
db.products.insertOne({ sku: "sku-1001" }) // E11000 duplicate key error index: sku_1 dup key: { sku: "sku-1001" }
db.products.updateOne({ _id: ObjectId("..."), version: 3 }, { $set: { price: NumberDecimal("9.99") }, $inc: { version: 1 } })
// → unique sku turns races into catchable errors; stale version → matchedCount 0 → template raises, never swallow as "no-op".
```

---

## 17. Repo-vs-template decision table

| Need | Repository ([003](./003-repository-query-methods.md)) | MongoTemplate (this guide) |
|---|---|---|
| simple CRUD + paging | ✅ derive `findByCategoryId(Pageable)` | overkill |
| `$text` + `$meta` score sort + dynamic filters | `@Query` static only | ✅ `TextCriteria`/`TextQuery` + `addCriteria` (§12) |
| dynamic AND/OR builder | ❌ | ✅ `Criteria` combinators (§4) |
| partial patch / counters | ❌ full-doc `save` | ✅ `updateFirst`/`inc`/`findAndModify` (§6–§7) |
| facets / histograms | ✅ fixed `@Aggregation` | ✅ dynamic `aggregate()` (§13) |
| sync / import throughput | `saveAll` (upsert each) | ✅ `insertAll` / `BulkOperations` (§14) |
| collection DDL | ❌ | ✅ `createCollection`/`dropCollection` (§15) |

Rule: derive equality/range/paging; template the dynamic, atomic, and bulk.

---

## 18. Pitfalls (GOLD)

!!! warning "GOLD — MongoTemplate pitfalls (read before shipping)"
    - **`save` replaces, `updateFirst` patches** — half-filled `save` deletes fields; patch with `$set` (§3 vs §6).
    - **`insert` on existing `_id` throws** — `DuplicateKeyException`, never silent upsert (§3, §16).
    - **Count the cleared copy** — `mongoTemplate.count(Query.of(q).limit(0).skip(0))`; counting `q` totals one page (§11–§12).
    - **`findAndModify` default returns BEFORE** — new counter/claim needs `returnNew(true)` (§7).
    - **`updateFirst` ≠ `updateMulti`** — `updateOne` vs `updateMany`; wrong one half-migrates data (§6).
    - **`$elemMatch` vs dotted path** — multi-cond on one element needs `$elemMatch`; dotted matches any element (§4).
    - **Unanchored regex scans** — `/trail/i` ok <10k docs, then `$text`/Atlas `$search` (§12, [005](./005-mongodb-query-language.md)).
    - **Ordered bulk aborts early** — default `insertAll`/ordered stops at first dup; imports want `UNORDERED` (§14).
    - **Catch Spring, not driver, exceptions** — `DuplicateKeyException`/`OptimisticLockingFailureException`, not `MongoException` (§16).
    - **`category` ≠ `categoryId`** — `ProductDocument.category` vs `Product.categoryId`; pasted filters match nothing (§12).

---

## 19. Interview Q&A

**Q1. `insert` vs `save` vs `insertAll` — what happens when the doc already exists?**
A: `insert` throws `DuplicateKeyException`; `save` replaces the whole doc by `_id` (upsert-by-id); `insertAll` is a fast ordered `insertMany` with no pre-check — imports use it, sync uses bulk UPSERT (§3, §14).
**Q2. Why is `findAndModify` the only safe counter/claim primitive?**
A: It reads + writes atomically server-side; `returnNew(true).upsert(true)` + `$inc` yields gap-free sequences, and status-flip claims elect exactly one worker — read-then-write races without it (§7).
**Q3. How does the SearchService text path keep totals correct with paging?**
A: `TextQuery.queryText(tc).sortByScore().with(pageable)` + `addCriteria` filters for `find`, then `count(Query.of(query).limit(0).skip(0))` on a cleared copy — same filter, no skip/limit (§12).
**Q4. `updateFirst` vs `updateMulti` vs `upsert` — which Mongo op is each?**
A: `updateOne` / `updateMany` / `updateOne + {upsert:true}`; `UpdateResult` distinguishes `matched`/`modified`/`upsertedId`, and `$setOnInsert` fires only on insert (§6).
**Q5. Repository vs MongoTemplate — the one-line rule?**
A: Derive static equality/range/paging in the repo; drop to the template for dynamic `Criteria`, `$meta` text sorts, atomic `findAndModify`, dynamic `aggregate()`, and `BulkOperations`/collection DDL (§17).

---

## 20. Resources

**This track:** [003 — Repository Query Methods](./003-repository-query-methods.md) (derived names, `@Query`, `@Aggregation`, custom impl) · [005 — MongoDB Query Language](./005-mongodb-query-language.md) (`$in`/`$all`/`$regex`, keyset pages) · [007 — Aggregation Framework](./007-aggregation-framework.md) (`$match`/`$group`/`$bucket`/`$facet`/`$lookup`) · [MongoDB intro](./mongodb.md) (collections, text indexes, `mongosh`).

**Official docs:** [Spring Data MongoDB — MongoTemplate](https://docs.spring.io/spring-data-mongodb/reference/mongo-template.html) · [Query/Criteria/Update](https://docs.spring.io/spring-data-mongodb/reference/mongo-template.html#mongo-template.query) · [Text Queries](https://docs.spring.io/spring-data-mongodb/reference/mongodb/text-search.html) · [Bulk Operations](https://docs.spring.io/spring-data-mongodb/reference/mongo-template.html#mongo-template.bulk-operations) · [Exception Translation](https://docs.spring.io/spring-data-mongodb/reference/mongodb/exceptions.html) · [MongoDB findAndModify](https://www.mongodb.com/docs/manual/reference/command/findAndModify/) · [MongoDB bulkWrite](https://www.mongodb.com/docs/manual/reference/method/db.collection.bulkWrite/)

**Repo mirrors (read, not compiled):** `projects-v2/commerce-mesh/backend/search-service/.../search/service/SearchService.java` (§12: `textSearch`/`filterSearch`, `TextCriteria`/`TextQuery`, `Query.of(q)` count, `Pageable`) · `.../search/repository/ProductSearchRepository.java` (§10, §13 facets) · `.../catalog/document/Product.java` (§1 domain).

---

*Next: [005 — MongoDB Query Language](./005-mongodb-query-language.md).*

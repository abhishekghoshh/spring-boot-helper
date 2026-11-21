# 003 — Repository Query Methods (Boot 4.1.0 + Java 25, Product Domain)

> Section C of the Spring Mongo track: derived names, `@Query`, `@Aggregation`,
> pagination, projections, custom impls, and bulk — on the real product domain.
>
> **Stack:** Spring Boot **4.1.0** · Java **25** · Spring Data MongoDB · MongoDB **7.x**.
>
> **Convention mirror — verified by reading, not compiling.** Methods mirror
> `product-catalog-service/.../catalog/repository/ProductRepository.java`
> (`Product`, `products`) and `search-service/.../search/repository/ProductSearchRepository.java`
> (`ProductDocument`, weighted `@TextIndexed`). Snippets were **not compiled**.
>
> Companion guides: [002](./002-document-mapping.md) · [004](./004-mongotemplate-crud.md) · [005](./005-mongodb-query-language.md) · [007](./007-aggregation-framework.md) · [MongoDB intro](./mongodb.md).

## Table of Contents

- [1. Domain and mirrored repositories](#1-domain-and-mirrored-repositories) · [2. CRUD table](#2-mongorepository-crud-methods) · [3. Derived names](#3-derived-naming-conventions) · [4. Derivation limits](#4-derivation-limits--when-to-stop-deriving) · [5. Pagination](#5-pagination--pageable-page-slice) · [6. Sorting](#6-sorting) · [7. Projections](#7-projections) · [8. Exists and counts](#8-exists-and-counts) · [9. @Query](#9-query--text-search-and-regex--fields) · [10. @Aggregation](#10-aggregation--match--group--bucket) · [11. Custom impl](#11-custom-implementation-with-mongotemplate) · [12. Optional](#12-optional-handling) · [13. Bulk](#13-bulk--saveall-batching) · [14. Cookbook](#14-query-cookbook--prefix-pattern-range-nested-array) · [15. isEmpty()](#15-default-isempty) · [16. Pitfalls](#16-pitfalls-gold) · [17. Q&A](#17-interview-qa) · [18. Resources](#18-resources)

---

## 1. Domain and mirrored repositories

Catalog `Product` (`com.commercemesh.catalog.document.Product`, `products`
collection — mapping detail in [002](./002-document-mapping.md)):

```java
@Document(collection = "products")
public class Product {
    @MongoId private String id;
    @TextIndexed private String name; @TextIndexed private String description;
    private String brand; @Indexed(unique = true) private String sku;
    private BigDecimal price; private String categoryId; // plain reference, no join
    private List<String> tags = new ArrayList<>();      // multikey index target
    private Map<String, String> attributes = new HashMap<>(); // dot-notation queries
    private double averageRating; private boolean isActive = true; private boolean isFeatured = false;
    @CreatedDate private LocalDateTime createdAt;
}
```

Search `ProductDocument` (`com.commercemesh.search.document.ProductDocument`):
read view of the same collection with weighted text index + `@TextScore`:

```java
@Document(collection = "products")
public class ProductDocument {
    @Id private String id;
    @TextIndexed(weight = 10) @Field("name") private String name;
    @TextIndexed(weight = 5) @Field("description") private String description;
    @TextIndexed(weight = 3) @Field("brand") private String brand;
    @Field("category") private String category;      // NOTE: not categoryId
    @Field("price") private BigDecimal price;
    @TextIndexed(weight = 2) @Field("tags") private List<String> tags;
    @Field("isActive") private boolean isActive;
    @TextScore private Float textScore;
}
```

Real `ProductRepository` (used verbatim through this guide):

```java
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);
    Page<Product> findByIsActiveTrue(Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String n, String d, Pageable p);
    Page<Product> findByTagsIn(List<String> tags, Pageable pageable);
    Page<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);
    @Query("{ $text: { $search: ?0 } }")
    Page<Product> searchByText(String searchText, Pageable pageable);
    List<Product> findByCategoryIdAndIsActiveTrue(String categoryId);
}
```

Real `ProductSearchRepository`:

```java
@Repository
public interface ProductSearchRepository extends MongoRepository<ProductDocument, String> {
    @Query("{ '$text': { '$search': ?0 }, 'isActive': true }")
    Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);
    Page<ProductDocument> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    @Query(value = "{ 'name': { '$regex': ?0, '$options': 'i' }, 'isActive': true }", fields = "{ 'name': 1 }")
    List<ProductDocument> findByNameRegex(String regex, Pageable pageable);
    @Aggregation(pipeline = {"{ '$match': { '_id': { '$in': ?0 } } }",
            "{ '$group': { '_id': '$category', 'count': { '$sum': 1 } } }"})
    List<CategoryCount> countByCategory(List<String> productIds);
    @Aggregation(pipeline = {"{ '$match': { '_id': { '$in': ?0 } } }",
            "{ '$bucket': { 'groupBy': '$price', 'boundaries': ?1, 'default': 'other', 'output': { 'count': { '$sum': 1 } } } }"})
    List<PriceRangeCount> countByPriceRange(List<String> productIds, List<Double> boundaries);
    default boolean isEmpty() { return count() == 0; }
    record CategoryCount(String id, long count) {}
    record PriceRangeCount(String id, long count) {}
}
```

```xml
<!-- product-catalog-service parent/java (Boot 4.1.0 + Java 25) + data-mongodb -->
<parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>4.1.0</version></parent>
<properties><java.version>25</java.version></properties>
<!-- + org.springframework.boot:spring-boot-starter-data-mongodb -->
```

---

## 2. MongoRepository CRUD methods

Proxied — never implemented by hand:

| Method | Signature | Mongo equivalent |
|---|---|---|
| `save` | `S save(S e)` | upsert by `_id` (`insertOne`/`updateOne`) |
| `saveAll` | `List<S> saveAll(Iterable<S>)` | batched upserts (see §13) |
| `findById` | `Optional<T> findById(ID)` | `find({ _id }).limit(1)` |
| `existsById` | `boolean existsById(ID)` | `count({ _id }, limit 1) > 0` |
| `findAll` / `findAll(Pageable)` | `List<T>` / `Page<T>` | `find({})` / `find + skip/limit + count` |
| `findAllById` | `List<T> findAllById(Iterable<ID>)` | `find({ _id: { $in } })`, order not guaranteed |
| `count` | `long count()` | `countDocuments({})` (§15 builds on this) |
| `deleteById` / `deleteAll` | `void` | `deleteOne({ _id })` / `deleteMany({})` |

```java
// java — null id inserts and populates id; re-save upserts on _id; empty Optional, never null
Product saved = repository.save(newProduct);
Optional<Product> hit = repository.findById(saved.getId());
repository.deleteById(saved.getId()); // no-op when missing
```

```javascript
// mongosh
db.products.insertOne({ name: "Trail Sneakers", sku: "sku-1001", price: 79.99 })
db.products.findOne({ _id: ObjectId("...") })
```

---

## 3. Derived naming conventions

Shape: `<prefix> [Distinct] [Top|First N] By <predicate> [OrderBy ...]`.
Property names are Java fields (resolved through `@Field` at runtime).

| Prefix | Meaning | Example |
|---|---|---|
| `findBy` / `readBy` / `getBy` / `queryBy` | select (aliases) | `findByCategoryId(String c, Pageable p)` |
| `countBy` | `countDocuments` | `long countByCategoryId(String c)` |
| `existsBy` | probe, `limit 1` | `boolean existsBySku(String sku)` |
| `deleteBy` / `removeBy` | delete matches | `long deleteByIsActiveFalse()` |

```java
// java — AND + OR (both mirrored)
Page<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable); // → { isFeatured: true, isActive: true }
Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String n, String d, Pageable p); // → { $or: [{ name: /n/i }, { description: /d/i }] }
```

| Keyword | Java example | Mongo |
|---|---|---|
| equality | `findByCategoryId(String c)` | `{ categoryId: ?0 }` |
| `Containing` / `StartingWith` / `EndingWith` | `findByNameContaining(s)` / `...StartingWith(p)` / `findBySkuEndingWith(s)` | `{ name: /s/ }` / `{ name: /^p/ }` / `{ sku: /s$/ }` |
| `In` | `findByTagsIn(List<String> t)` (mirrored) | `{ tags: { $in } }` |
| `Between` | `findByPriceBetween(lo, hi)` | `{ price: { $gte, $lte } }` (inclusive) |
| `GreaterThan` / `LessThan` | `findByPriceGreaterThan(p)` | `{ price: { $gt } }` |
| `IgnoreCase` | `...ContainingIgnoreCase(...)` (mirrored) | case-insensitive regex |
| `True` / `False` | `findByIsActiveTrue(p)` (mirrored, **no arg**) | `{ isActive: true }` |
| `IsNull` / `IsNotNull` | `findByBrandIsNull()` | `{ brand: null }` / `{ brand: { $ne: null } }` |
| `ElementMatch` | hand to `@Query` (`$elemMatch`) | `{ tags: { $elemMatch } }` |

```java
// java — mirrored calls
Page<Product> active = repository.findByIsActiveTrue(Pageable.ofSize(20));
Page<Product> tagged = repository.findByTagsIn(List.of("trail", "sale"), Pageable.ofSize(20));
List<Product> inCat = repository.findByCategoryIdAndIsActiveTrue("footwear");
```

```javascript
// mongosh — what Spring derives
db.products.find({ isActive: true }).limit(20)
db.products.find({ tags: { $in: ["trail", "sale"] } }).limit(20)
db.products.find({ categoryId: "footwear", isActive: true })
// → True/False take NO argument; findByIsActiveTrue(true) fails at startup.
```

---

## 4. Derivation limits — when to stop deriving

1. **No joins.** `categoryId` is a stored string; no `findByCategoryName` crosses
   collections. `$lookup` ([007](./007-aggregation-framework.md)) or denormalize.
2. **No OR across deep nesting.** Top-level `NameOrDescription` is fine; OR across
   two nested maps/arrays needs `@Query("{ '$or': [...] }")` ([005](./005-mongodb-query-language.md)).
3. **No relevance ranking or facets.** `$text`+`$meta` → `@Query` (§9);
   `$group`/`$bucket` → `@Aggregation` (§10).
4. **Name ceiling.** Past ~3 predicates switch to `@Query` or a custom impl (§11).

Rule: derive equality/range/paging; annotate search, facets, nested `$or`.

---

## 5. Pagination — Pageable, Page, Slice

Every mirrored catalog method takes `Pageable` **last** and returns `Page<T>`.

```java
// java — paged browse (mirrored)
Page<Product> page = repository.findByCategoryId("footwear", PageRequest.of(0, 20, Sort.by("createdAt").descending()));
long total = page.getTotalElements(); // ← extra count query (see warning)
Pageable next = page.nextPageable();
```

```javascript
// mongosh — same page + the hidden totals query
db.products.find({ categoryId: "footwear" }).sort({ createdAt: -1 }).skip(0).limit(20)
db.products.countDocuments({ categoryId: "footwear" })
```

| Return | Total? | Extra `count`? | Use for |
|---|---|---|---|
| `Page<T>` | yes | **yes** | admin grids ("page 3 of 42") |
| `Slice<T>` | `hasNext` only | no | infinite scroll / feeds |
| `List<T>` + `Pageable` | no | no | autocomplete (`findByNameRegex`) |

> ⚠️ **Count cost warning.** `Page` fires `countDocuments` with the same filter on
> every call — on a million-row `products` that count can dwarf the 20-row fetch.
> Scroll → `Slice`/`List`; totals UIs → `Page`. Deep pages prefer keyset
> `{ _id: { $gt: lastId } }` + `limit` over large `skip()` ([005](./005-mongodb-query-language.md)).

---

## 6. Sorting

Sort travels on `Pageable`, not in the method name (except static `OrderBy`).

```java
// java
Pageable sorted = PageRequest.of(0, 20, Sort.by("createdAt").descending().and(Sort.by("price").ascending()));
Page<Product> page = repository.findByIsActiveTrue(sorted);
List<Product> top = repository.findByCategoryIdOrderByAverageRatingDesc("footwear");
```

```javascript
// mongosh
db.products.find({ isActive: true }).sort({ createdAt: -1, price: 1 }).limit(20)
// → unindexed sort spills to memory (32 MB cap); index the { filter + sort } pair ([002](./002-document-mapping.md)).
```

---

## 7. Projections

**Interface-based (closed)** — Spring derives the select:

```java
// java
public interface ProductCard { String getName(); BigDecimal getPrice(); String getId(); }
List<ProductCard> findByCategoryId(String categoryId);
// mongosh: db.products.find({ categoryId: "footwear" }, { name: 1, price: 1, _id: 1 })
// → open (@Value SpEL) projections re-fetch full docs; avoid on hot paths.
```

**`fields=` in `@Query`** — mirrored autocomplete, names only:

```java
// java — mirrored from ProductSearchRepository
@Query(value = "{ 'name': { '$regex': ?0, '$options': 'i' }, 'isActive': true }", fields = "{ 'name': 1 }")
List<ProductDocument> findByNameRegex(String regex, Pageable pageable);
repo.findByNameRegex("^trail", PageRequest.of(0, 10));
```

```javascript
// mongosh
db.products.find({ name: { $regex: "^trail", $options: "i" }, isActive: true }, { name: 1 }).limit(10)
// → [{ _id, name }] only; unanchored patterns skip the index.
```

---

## 8. Exists and counts

```java
// java — no documents leave the server
boolean skuTaken = repository.existsBySku("sku-1001");
long active = repository.countByIsActiveTrue();
```

```javascript
// mongosh
db.products.countDocuments({ sku: "sku-1001" }, { limit: 1 }) // >0 → exists
db.products.countDocuments({ isActive: true })
// → existsBy* stops at the first index hit; back it with @Indexed(unique=true) on sku ([002](./002-document-mapping.md)).
```

---

## 9. @Query — text search and regex + fields

`?0`, `?1` bind in order. Names are **document** fields — `category` vs
`categoryId` differ between the two repositories.

```java
// java — both text mirrors (need exactly one text index per collection)
@Query("{ $text: { $search: ?0 } }")
Page<Product> searchByText(String searchText, Pageable pageable);
@Query("{ '$text': { '$search': ?0 }, 'isActive': true }")
Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);
```

```javascript
// mongosh
db.products.createIndex({ name: "text", description: "text" })
db.products.find({ $text: { $search: "trail waterproof" }, isActive: true })
// → stemming ("trails"→"trail"); missing index throws, never []. Ranked $meta sort → MongoTemplate ([004](./004-mongotemplate-crud.md)).
```

```java
// java — mirrored regex + fields (autocomplete)
@Query(value = "{ 'name': { '$regex': ?0, '$options': 'i' }, 'isActive': true }", fields = "{ 'name': 1 }")
List<ProductDocument> findByNameRegex(String regex, Pageable pageable);
```

```javascript
// mongosh
db.products.find({ name: { $regex: "^trail", $options: "i" }, isActive: true }, { name: 1 }).limit(10)
// → 10 names, no prices/descriptions; 'i' skips most indexes — Atlas $search wins at scale ([005](./005-mongodb-query-language.md)).
```

---

## 10. @Aggregation — $match / $group / $bucket

Native pipeline; `?0`/`?1` bind; each output doc maps to the return record.
Pattern: filter ids first, then facet.

```java
// java — mirrored category facet
@Aggregation(pipeline = {"{ '$match': { '_id': { '$in': ?0 } } }",
        "{ '$group': { '_id': '$category', 'count': { '$sum': 1 } } }"})
List<CategoryCount> countByCategory(List<String> productIds);
record CategoryCount(String id, long count) {}
```

```javascript
// mongosh
db.products.aggregate([{ $match: { _id: { $in: ["p1", "p2", "p3"] } } }, { $group: { _id: "$category", count: { $sum: 1 } } }])
// → [{ _id: "footwear", count: 2 }]; output _id = category → CategoryCount.id; [] when ids empty.
```

```java
// java — mirrored price histogram, caller-supplied buckets
@Aggregation(pipeline = {"{ '$match': { '_id': { '$in': ?0 } } }",
        "{ '$bucket': { 'groupBy': '$price', 'boundaries': ?1, 'default': 'other', 'output': { 'count': { '$sum': 1 } } } }"})
List<PriceRangeCount> countByPriceRange(List<String> ids, List<Double> boundaries);
repo.countByPriceRange(ids, List.of(0.0, 50.0, 100.0, 250.0));
```

```javascript
// mongosh
db.products.aggregate([{ $match: { _id: { $in: ["p1", "p2"] } } },
  { $bucket: { groupBy: "$price", boundaries: [0, 50, 100, 250], default: "other", output: { count: { $sum: 1 } } } }])
// → [{ _id: 50, count: 1 }]; _id = bucket floor, strays → "other"; boundaries ascending, one type ([007](./007-aggregation-framework.md)).
```

---

## 11. Custom implementation with MongoTemplate

Escape hatch for dynamic filters / `$meta` sorts / geo: fragment + `Impl`.
Suffix `Impl` is load-bearing — `ProductRepositoryCustomImpl` is ignored.

```java
// java
public interface ProductRepositoryCustom {
    Page<Product> searchDynamic(String k, String categoryId, BigDecimal lo, BigDecimal hi, Pageable p);
}
public class ProductRepositoryImpl implements ProductRepositoryCustom {
    private final MongoTemplate mongoTemplate;
    public ProductRepositoryImpl(MongoTemplate mongoTemplate) { this.mongoTemplate = mongoTemplate; }
    @Override
    public Page<Product> searchDynamic(String k, String categoryId, BigDecimal lo, BigDecimal hi, Pageable p) {
        List<Criteria> and = new ArrayList<>();
        if (k != null && !k.isBlank())
            and.add(new Criteria().orOperator(Criteria.where("name").regex(k, "i"), Criteria.where("description").regex(k, "i")));
        if (categoryId != null) and.add(Criteria.where("categoryId").is(categoryId));
        if (lo != null || hi != null) {
            Criteria r = Criteria.where("price");
            if (lo != null) r.gte(lo);
            if (hi != null) r.lte(hi);
            and.add(r);
        }
        and.add(Criteria.where("isActive").is(true));
        Query q = new Query(new Criteria().andOperator(and)).with(p);
        long total = mongoTemplate.count(q.skip(0).limit(0), Product.class);
        return new PageImpl<>(mongoTemplate.find(q, Product.class), p, total);
    }
}
public interface ProductRepository extends MongoRepository<Product, String>, ProductRepositoryCustom {
    Page<Product> findByCategoryId(String categoryId, Pageable pageable);
}
```

```javascript
// mongosh — searchDynamic("trail", "footwear", 0–100)
db.products.find({ $and: [{ $or: [{ name: /trail/i }, { description: /trail/i }] },
  { categoryId: "footwear" }, { price: { $gte: 0, $lte: 100 } }, { isActive: true }] }).skip(0).limit(20)
// → count clears skip/limit so totals stay right. Full API: [004](./004-mongotemplate-crud.md).
```

---

## 12. Optional handling

Single-row finders return `Optional`, never `null`.

```java
// java
Optional<Product> findBySku(String sku);
Product p = repository.findBySku("sku-1001").orElseThrow(() -> new ProductNotFoundException("sku-1001"));
```

```javascript
// mongosh
db.products.findOne({ sku: "sku-1001" }) // null → Optional.empty()
// → throw at the service boundary (→ 404 once); bare Product returners reintroduce null.
```

---

## 13. Bulk — saveAll batching

```java
// java — chunk so converted docs never balloon memory (5k/batch)
for (List<Product> chunk : Lists.partition(loadCatalog(), 5_000)) repository.saveAll(chunk);
// insert-only fast path (no pre-check): mongoTemplate.insertAll(chunk) ([004](./004-mongotemplate-crud.md))
```

```javascript
// mongosh
db.products.insertMany([{ sku: "sku-9001" }, { sku: "sku-9002" }], { ordered: false })
// → saveAll upserts each row; ordered insertMany aborts on dup sku. Nightly import → insertAll + unique sku; sync → saveAll.
```

---

## 14. Query cookbook — prefix, pattern, range, nested, array

**Prefix (anchored, index-friendly).**

```java
// java
List<Product> findByNameStartingWith(String prefix);
```

```javascript
// mongosh
db.products.find({ name: /^Trail/ }).limit(20) // → range scan; unanchored contains scans.
```

**Pattern (contains, case-insensitive — mirrored).**

```java
// java
Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String n, String d, Pageable p);
```

```javascript
// mongosh
db.products.find({ $or: [{ name: /trail/i }, { description: /trail/i }] }).limit(20)
// → anywhere-match page; dual regex ≈ scan past ~10k rows → $text (§9).
```

**Range (price window, inclusive).**

```java
// java
Page<Product> findByPriceBetween(BigDecimal lo, BigDecimal hi, Pageable p);
```

```javascript
// mongosh
db.products.find({ price: { $gte: 50, $lte: 100 } }).limit(20)
// → BigDecimal→Decimal128 avoids 19.99 double edges; index price.
```

**Nested (map attribute, dot notation).**

```java
// java — attributes: Map<String,String>; underscore walks the map
List<Product> findByAttributes_Color(String color);
```

```javascript
// mongosh
db.products.find({ "attributes.color": "red" }) // → variable keys need @Query("{ 'attributes.?0': ?1 }").
```

**Array (any-tag overlap — mirrored).**

```java
// java
Page<Product> findByTagsIn(List<String> tags, Pageable p);
```

```javascript
// mongosh
db.products.find({ tags: { $in: ["trail", "waterproof"] } }).limit(20)
// → OR across values (multikey index); AND-across-values → { $all } via @Query ([005](./005-mongodb-query-language.md)).
```

---

## 15. default isEmpty()

Verbatim mirror of `ProductSearchRepository.isEmpty()` — unfiltered; answers
"collection empty", not "no active products".

```java
// java
default boolean isEmpty() { return count() == 0; }
if (searchRepository.isEmpty()) searchRepository.saveAll(seedDocuments());
```

```javascript
// mongosh — what count() sends
db.products.countDocuments({}) // → 0 → seed; metadata scan, boot-safe. Never findAll().isEmpty().
```

---

## 16. Pitfalls (GOLD)

!!! warning "GOLD — repository query pitfalls (read before shipping)"
    - **`True`/`False` take no argument** — `findByIsActiveTrue()` is zero-param; a stray boolean breaks startup parsing.
    - **`Pageable` goes last** — `(Pageable, String)` fails derivation; filters first.
    - **`ContainingIgnoreCase` is a scan** — fine for small catalogs, `$text` before launch (§9).
    - **Document vs Java names** — `category` ≠ `categoryId` across the two repos; pasted `@Query` silently matches nothing.
    - **`fields=` trims columns, never rows** — `isActive: true` must stay in the filter.
    - **`$text` without a text index throws** — check `@TextIndexed` ([002](./002-document-mapping.md)).
    - **`$bucket` needs one numeric type** — mixed `Double`/`Decimal128`/`String` aborts.
    - **`Page` on scroll doubles load** — `Slice`/`List` for feeds (§5).
    - **`Impl` suffix is load-bearing** — `...CustomImpl` never wires (§11).
    - **`isEmpty()` is unfiltered** — name wider checks explicitly (`hasActiveProducts()`).

---

## 17. Interview Q&A

**Q1. What does `findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase` derive, and when do you replace it?**
A: `$or` of two case-insensitive regexes, paged. Keep for small/admin search; swap to `@Query $text` (§9) when scans dominate — text index + stemming beats dual regex.
**Q2. Why does `Page<Product> findByCategoryId(...)` cost two round trips?**
A: `Page` needs totals → `find` + `countDocuments` (§5). Scroll UIs take `Slice`/`List` + `Pageable` (one query, `hasNext`).
**Q3. Derived name vs `@Query` vs `@Aggregation` — the rule?**
A: Derive equality/range/paging; `@Query` for `$text`, nested `$or`, regex+`fields` (§9); `@Aggregation` for `$match`/`$group`/`$bucket` facets (§10); past ~3 predicates use `@Query`/custom impl (§11).
**Q4. What does `_id` mean in each mirrored facet output?**
A: `countByCategory`: `_id` = category value → `CategoryCount.id`. `countByPriceRange`: `_id` = bucket floor (or `"other"`) ([007](./007-aggregation-framework.md)).
**Q5. How does the custom impl wire, and what breaks on misname?**
A: Repo extends `ProductRepositoryCustom`; `ProductRepositoryImpl` (+ `MongoTemplate` ctor) merges by `Impl` suffix (§11, [004](./004-mongotemplate-crud.md)). `...CustomImpl` is ignored → runtime "no implementation" failure.

---

## 18. Resources

**This track:** [002 — Document Mapping](./002-document-mapping.md) (`@Document`/`@Field`/`@TextIndexed`, indexes) · [004 — MongoTemplate CRUD](./004-mongotemplate-crud.md) (`Criteria`/`Query`, `TextQuery`, `insertAll`) · [005 — MongoDB Query Language](./005-mongodb-query-language.md) (`$in`/`$all`/`$regex`, keyset pages) · [007 — Aggregation Framework](./007-aggregation-framework.md) (`$match`/`$group`/`$bucket`/`$facet`/`$lookup`) · [MongoDB intro](./mongodb.md) (collections, text indexes, `mongosh`).

**Official docs:** [Spring Data MongoDB — Repositories](https://docs.spring.io/spring-data-mongodb/reference/repositories.html) · [Query Methods](https://docs.spring.io/spring-data-mongodb/reference/repositories/query-methods.html) · [`@Query`](https://docs.spring.io/spring-data-mongodb/reference/repositories/mongo-repository-annotation.html) · [`@Aggregation`](https://docs.spring.io/spring-data-mongodb/reference/repositories/aggregation-repository-methods.html) · [Paging/Sorting](https://docs.spring.io/spring-data/commons/repository/paging-and-sorting/) · [MongoDB `$text`](https://www.mongodb.com/docs/manual/text-search/) · [MongoDB `$bucket`](https://www.mongodb.com/docs/manual/reference/operator/aggregation/bucket/)

**Repo mirrors (read, not compiled):** `projects-v2/commerce-mesh/backend/product-catalog-service/.../catalog/repository/ProductRepository.java` (§1, §3, §9) · `projects-v2/commerce-mesh/backend/search-service/.../search/repository/ProductSearchRepository.java` (§9, §10, §15) · `.../catalog/document/Product.java` + `.../search/document/ProductDocument.java` (field paths).

---

*Next: [004 — MongoTemplate CRUD](./004-mongotemplate-crud.md).*

# 007 — Aggregation Framework (Boot 4.1.0 + Java 25, Sales / Orders Domain)

> Section G of the Spring Mongo track: pipeline stages, expressions, dates, windows,
> `$lookup`, `$facet` pages, `$bucket` histograms, `$merge`/`$out` — on sales/orders.
>
> **Stack:** Spring Boot **4.1.0** · Java **25** · Spring Data MongoDB 5.x · MongoDB **7.x**.
>
> **Convention mirror — verified by reading, not compiling.** `@Aggregation` `$group`/`$bucket`
> shapes mirror `search-service/.../search/repository/ProductSearchRepository.java`
> (`countByCategory`, `countByPriceRange` — see §15, [003](./003-repository-query-methods.md)).
> Template `aggregate()` mirrors `SearchService.java` ([004](./004-mongotemplate-crud.md)). Snippets were **not compiled.**
>
> Companion guides: [003](./003-repository-query-methods.md) · [004](./004-mongotemplate-crud.md) · [008](./008-indexing-query-performance.md) · [MongoDB intro](./mongodb.md).

## Table of Contents

- [1. Domain + seed](#1-domain--seed-input-for-all-scenarios) · [2. Stage catalog](#2-stage-catalog) · [3. match/project/set/unset](#3-matchprojectsetaddfieldsunset) · [4. group](#4-group--sumsumavgminmax) · [5. sort/skip/limit](#5-sortskiplimit) · [6. unwind](#6-unwind--nestedarray-aggregation) · [7. Expressions](#7-condifnullmapfilterreduce) · [8. Dates](#8-date-ops-yearmonthdatetrunc) · [9. lookup](#9-lookup--left-outer-join) · [10. facet/count/bucket](#10-facetcountbucketbucketauto) · [11. replaceRoot/merge/out](#11-replacerootreplacewithmergeout) · [12. Windows](#12-setwindowfields-rankrunning-total) · [13. Scenarios](#13-scenarios) · [14. TypedAggregation](#14-typedaggregation--dto-mapping--aggregationresults) · [15. @Aggregation mirror](#15-aggregation-mirror-search-service-groupbucket) · [16. Optimization](#16-optimization-match-first-index-coalescence) · [17. Memory](#17-memory-100mb--allowdiskuse) · [18. Pagination+count](#18-paginationcount-pattern) · [19. Join vs embed](#19-join-vs-embed-verdict) · [20. Pitfalls](#20-pitfalls-gold) · [21. Q&A](#21-interview-qa) · [22. Resources](#22-resources)

```xml
<!-- parent/java (Boot 4.1.0 + Java 25) + data-mongodb -->
<parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>4.1.0</version></parent>
<properties><java.version>25</java.version></properties>
<!-- + org.springframework.boot:spring-boot-starter-data-mongodb -->
```

---

## 1. Domain + seed (input for all scenarios)

`orders` embeds lines (hot path, no join); `customers` is the `$lookup` side. Statuses: `PENDING | PAID | SHIPPED | CANCELLED`.

```java
// java — documents (orders illustrative; real order-service here is JPA)
@Document(collection = "orders") public class Order {
    @Id private String id; private String customerId; // plain ref → customers._id
    private String status; private Instant orderedAt; private List<Item> items;
    public record Item(String sku, String name, int qty, BigDecimal price) {}
}
@Document(collection = "customers") public class Customer {
    @Id private String id; private String name; private String region;
}
```

```json
// input — orders (4 docs drive every §13 scenario)
{ "_id": "O-1001", "customerId": "C-01", "status": "PAID", "orderedAt": { "$date": "2026-01-15T10:00:00Z" }, "items": [{ "sku": "TRAIL-1", "qty": 2, "price": { "$numberDecimal": "120.00" } }, { "sku": "SOCK-9", "qty": 3, "price": { "$numberDecimal": "15.00" } }] }
{ "_id": "O-1002", "customerId": "C-01", "status": "SHIPPED", "orderedAt": { "$date": "2026-02-03T12:00:00Z" }, "items": [{ "sku": "TRAIL-1", "qty": 1, "price": { "$numberDecimal": "120.00" } }] }
{ "_id": "O-1003", "customerId": "C-02", "status": "PENDING", "orderedAt": { "$date": "2026-02-20T09:00:00Z" }, "items": [{ "sku": "TENT-5", "qty": 1, "price": { "$numberDecimal": "499.00" } }] }
{ "_id": "O-1004", "customerId": "C-03", "status": "PAID", "orderedAt": { "$date": "2026-03-05T14:00:00Z" }, "items": [{ "sku": "SOCK-9", "qty": 10, "price": { "$numberDecimal": "15.00" } }] }
```

```json
// input — customers (C-04 never orders → §13.5 anti-join)
{ "_id": "C-01", "name": "Asha", "region": "EAST" } { "_id": "C-02", "name": "Ben", "region": "WEST" }
{ "_id": "C-03", "name": "Cara", "region": "EAST" } { "_id": "C-04", "name": "Dev", "region": "WEST" }
```

```javascript
// mongosh — load
db.orders.insertMany([/* 4 docs above */]); db.customers.insertMany([/* 4 docs above */]);
```

---

## 2. Stage catalog

Pipeline = Unix pipe over docs. Order matters: `$match` first shrinks everything downstream (§16).

| Stage | Does | Spring API |
|---|---|---|
| `$match` | filter (index-backed when leading) | `Aggregation.match(Criteria…)` |
| `$project` / `$set` (`$addFields`) / `$unset` | keep+compute / add-or-overwrite / drop fields | `project(…)`, `addFields()`, `unset(…)` |
| `$group` + `$sum`/`$avg`/`$min`/`$max` (+`$push`/`$first`) | group-by + accumulators | `group(…).sum(…).avg(…).min(…).max(…)` |
| `$sort` / `$skip` / `$limit` | order + page slice | `sort(…)`, `skip(…)`, `limit(…)` |
| `$unwind` | 1 doc per array element | `unwind("items")` |
| `$lookup` | left-outer join | `lookup(…)` |
| `$facet` | parallel sub-pipelines (page+totals) | `facet(…)` / `FacetOperation` |
| `$count` | `{ $count: "total" }` sugar | `count().as("total")` |
| `$bucket` / `$bucketAuto` | fixed / auto histograms | `bucket(…)` / `bucketAuto(…)` |
| `$replaceRoot` / `$replaceWith` | promote sub-doc to root | `replaceRoot(…)` / `replaceWith(…)` |
| `$merge` / `$out` | write results (upsert / replace) | `merge()` / `out(…)` |

```java
// java — skeleton: MongoTemplate.aggregate → AggregationResults
Aggregation agg = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("status").is("PAID")),
    Aggregation.group("customerId").sum("total").as("revenue"),
    Aggregation.sort(Sort.Direction.DESC, "revenue"), Aggregation.limit(10));
AggregationResults<CustomerRevenue> r = mongoTemplate.aggregate(agg, "orders", CustomerRevenue.class);
```

```javascript
// mongosh — same skeleton
db.orders.aggregate([{ $match: { status: "PAID" } },
  { $group: { _id: "$customerId", revenue: { $sum: "$total" } } },
  { $sort: { revenue: -1 } }, { $limit: 10 }])
```

---

## 3. `$match`/`$project`/`$set`(`$addFields`)`/`$unset`

`$match` filters (index-backed when leading). `$project` keeps/renames/computes;
`$set` (alias `$addFields`) adds-or-overwrites in place; `$unset` drops fields.
Project early to shrink docs (§16); `$set` for additive math.

```javascript
// mongosh — line total via $map+$multiply, then trim
db.orders.aggregate([{ $match: { status: { $ne: "CANCELLED" } } },
  { $set: { total: { $sum: { $map: { input: "$items", as: "it",
    in: { $multiply: ["$$it.qty", "$$it.price"] } } } } } },
  { $project: { customerId: 1, status: 1, total: 1 } }, { $unset: "_id" }])
// → { customerId: "C-01", status: "PAID", total: NumberDecimal("285.00") } …
```

```java
// java — same three stages ($set ≡ $addFields; use $set)
Aggregation agg = newAggregation(match(Criteria.where("status").ne("CANCELLED")),
  addFields().addFieldWithValueOf("total", AccumulatorOperators.Sum.sumOf(
    VariableOperators.Map.itemsOf("items").as("it").andApply(
      ctx -> new org.bson.Document("$multiply", java.util.List.of("$$it.qty", "$$it.price"))))).build(),
  project("customerId", "status", "total").andExclude("_id"), unset("_id"));
```

---

## 4. `$group` + `$sum`/`$avg`/`$min`/`$max`

`_id` = group key (`null` = whole-collection rollup). Accumulators run per group.

| Accumulator | Meaning | Spring |
|---|---|---|
| `$sum: 1` / `$sum: "$qty"` | count / sum | `.count().as("n")` / `.sum("qty").as("units")` |
| `$avg` / `$min` / `$max` | mean / extremes | `.avg(…).as(…)` / `.min(…).as(…)` / `.max(…).as(…)` |
| `$push` / `$addToSet` / `$first`+`$last` | collect / distinct / edges | `.push(…)` / `.addToSet(…)` / `.first(…)`/`.last(…)` |

```javascript
// mongosh — per-status rollup
db.orders.aggregate([
  { $set: { total: { $sum: { $map: { input: "$items", as: "it", in: { $multiply: ["$$it.qty", "$$it.price"] } } } } } },
  { $group: { _id: "$status", orders: { $sum: 1 }, revenue: { $sum: "$total" },
    avgOrder: { $avg: "$total" }, minOrder: { $min: "$total" }, maxOrder: { $max: "$total" } } },
  { $sort: { revenue: -1 } }])
```

```java
// java — same rollup
Aggregation agg = newAggregation(group("status").count().as("orders")
  .sum("total").as("revenue").avg("total").as("avgOrder")
  .min("total").as("minOrder").max("total").as("maxOrder"),
  sort(Sort.Direction.DESC, "revenue"));
```

---

## 5. `$sort`/`$skip`/`$limit`

`$sort`→`$skip`→`$limit` = deterministic page. Keep `$sort`+`$limit` adjacent:
optimizer coalesces to top-N sort (bounded heap, §16).

```javascript
// mongosh — page 2, newest first (needs { orderedAt: -1 } or compound §16)
db.orders.aggregate([{ $match: { status: { $ne: "CANCELLED" } } },
  { $sort: { orderedAt: -1 } }, { $skip: 20 }, { $limit: 20 }])
```

```java
// java — same page (skip = page * size, 0-based)
Aggregation agg = newAggregation(match(Criteria.where("status").ne("CANCELLED")),
  sort(Sort.Direction.DESC, "orderedAt"), skip(1L * 20), limit(20));
```

---

## 6. `$unwind` + nested/array aggregation

`$unwind` emits one doc per array element so `$group` aggregates *inside* arrays
(per-SKU revenue). `preserveNullAndEmptyArrays` keeps item-less orders;
`includeArrayIndex` records position. Chain two `$unwind`s for doubly-nested arrays.

```javascript
// mongosh — per-SKU units + revenue
db.orders.aggregate([{ $unwind: "$items" },
  { $group: { _id: "$items.sku", units: { $sum: "$items.qty" },
    revenue: { $sum: { $multiply: ["$items.qty", "$items.price"] } } } },
  { $sort: { revenue: -1 } }])
// → { _id: "TENT-5", units: 1, revenue: 499 } { _id: "TRAIL-1", units: 3, revenue: 360 } …
```

```java
// java — unwind then group (drives §13.3 + §13.6)
Aggregation agg = newAggregation(unwind("items"),
  group("items.sku").sum("items.qty").as("units")
    .sum(ArithmeticOperators.Multiply.valueOf("items.qty").multiplyBy("items.price")).as("revenue"),
  sort(Sort.Direction.DESC, "revenue"));
```

```javascript
// mongosh — orphan-safe unwind + doubly-nested chain
db.orders.aggregate([{ $unwind: { path: "$items", preserveNullAndEmptyArrays: true } }])
db.bundles.aggregate([{ $unwind: "$packs" }, { $unwind: "$packs.skus" },
  { $group: { _id: "$packs.skus", n: { $sum: 1 } } }])
```

---

## 7. `$cond`/`$ifNull`/`$map`/`$filter`/`$reduce`

Live inside `$project`/`$set`/`$group`. `$cond` = ternary, `$ifNull` = null-guard,
`$map`/`$filter` transform arrays, `$reduce` folds to scalar.

```javascript
// mongosh — tier ($cond), region default ($ifNull), big lines ($filter), csv ($reduce), total ($map)
db.orders.aggregate([{ $set: {
    total: { $sum: { $map: { input: "$items", as: "it", in: { $multiply: ["$$it.qty", "$$it.price"] } } } },
    bigSkus: { $filter: { input: "$items", as: "it", cond: { $gte: ["$$it.qty", 3] } } },
    skuCsv: { $reduce: { input: "$items", initialValue: "",
      in: { $concat: ["$$value", "$$this.sku", ";"] } } } } },
  { $project: { customerId: 1, total: 1,
    tier: { $cond: [{ $gte: ["$total", 400] }, "VIP", "STANDARD"] },
    region: { $ifNull: ["$region", "UNKNOWN"] } } }])
```

```java
// java — $cond + $ifNull via projection expressions
Aggregation agg = newAggregation(project("customerId", "total")
  .andExpression("{ $cond: [ { $gte: ['$total', 400] }, 'VIP', 'STANDARD' ] }").as("tier")
  .andExpression("{ $ifNull: ['$region', 'UNKNOWN'] }").as("region"));
```

| Op | Use when | Watch out |
|---|---|---|
| `$cond` / `$ifNull` | tiers / defaults | nest >2 ternaries → `$switch`/`$bucket`; `$ifNull` misses `""`/`[]` |
| `$map` / `$filter` | per-element math / keep matches | `cond` sees `$$it`, not `$items.qty`; `$map` keeps length — fold with `$sum` |
| `$reduce` | fold to scalar | keep `$$value`/`$$this` + stable `initialValue` type |

---

## 8. Date ops — `$year`/`$month`/`$dateTrunc`

`$year`/`$month`/`$dayOfMonth` build calendar keys (UTC); `$dateTrunc` truncates to
a grain tz-aware. Prefer `$dateTrunc` month buckets — one key, correct boundaries.

```javascript
// mongosh — monthly revenue, NY month boundaries
db.orders.aggregate([
  { $set: { total: { $sum: { $map: { input: "$items", as: "it", in: { $multiply: ["$$it.qty", "$$it.price"] } } } } } },
  { $group: { _id: { $dateTrunc: { date: "$orderedAt", unit: "month", timezone: "America/New_York" } },
    revenue: { $sum: "$total" }, orders: { $sum: 1 } } }, { $sort: { _id: 1 } }])
// → Jan-2026: 285 · Feb-2026: 619 · Mar-2026: 150
```

```java
// java — same bucket via DateOperators
Aggregation agg = newAggregation(
  group(DateOperators.dateOf("orderedAt").truncate("month").withTimezone("America/New_York"))
    .sum("total").as("revenue").count().as("orders"), sort(Sort.Direction.ASC, "_id"));
```

```javascript
// mongosh — classic $year/$month key (UTC) without driver date ops
db.orders.aggregate([{ $group: { _id: { y: { $year: "$orderedAt" }, m: { $month: "$orderedAt" } },
  revenue: { $sum: "$total" } } }, { $sort: { "_id.y": 1, "_id.m": 1 } }])
```

---

## 9. `$lookup` — left-outer join

Pulls `customers` in as an **array**; `$unwind` (+`preserveNullAndEmptyArrays`)
flattens; `$match: { arr: { $eq: [] } }` finds orphans (§13.5). Correlated form
uses `let` + inner `pipeline`.

```javascript
// mongosh — orders enriched with customer fields
db.orders.aggregate([
  { $lookup: { from: "customers", localField: "customerId", foreignField: "_id", as: "customer" } },
  { $unwind: { path: "$customer", preserveNullAndEmptyArrays: true } },
  { $project: { status: 1, total: 1, customerName: "$customer.name",
    region: { $ifNull: ["$customer.region", "UNKNOWN"] } } }])
```

```java
// java — lookup + orphan-safe unwind
Aggregation agg = newAggregation(lookup("customers", "customerId", "_id", "customer"),
  unwind("customer", true), project("status", "total").and("customer.name").as("customerName"));
```

```javascript
// mongosh — correlated: newest order per customer pushed into foreign side
db.customers.aggregate([{ $lookup: { from: "orders", let: { cid: "$_id" },
  pipeline: [{ $match: { $expr: { $eq: ["$customerId", "$$cid"] } } }, { $sort: { orderedAt: -1 } }],
  as: "recentOrders" } }])
```

---

## 10. `$facet`/`$count`/`$bucket`/`$bucketAuto`

`$facet` runs parallel sub-pipelines on one input — metadata + data in one trip.
`$count` is sugar. `$bucket` = fixed boundaries (lower-inclusive); `$bucketAuto` =
N even-ish buckets for shape discovery.

```javascript
// mongosh — one trip: total + page + status histogram + auto price buckets
db.orders.aggregate([{ $match: { status: { $ne: "CANCELLED" } } }, { $facet: {
    metadata: [{ $count: "total" }],
    data: [{ $sort: { orderedAt: -1 } }, { $skip: 0 }, { $limit: 20 }],
    byStatus: [{ $group: { _id: "$status", n: { $sum: 1 } } }],
    priceShape: [{ $unwind: "$items" }, { $bucketAuto: { groupBy: "$items.price", buckets: 3 } }] } }])
```

```java
// java — facet with sub-pipelines (full page in §13.7/§18)
Aggregation agg = newAggregation(match(Criteria.where("status").ne("CANCELLED")),
  new FacetOperation()
    .and(Aggregation.count().as("total")).as("metadata")
    .and(sort(Sort.Direction.DESC, "orderedAt"), skip(0L), limit(20)).as("data")
    .and(group("status").count().as("n")).as("byStatus"));
```

```javascript
// mongosh — fixed revenue tiers ($bucket needs one numeric type + default)
db.orders.aggregate([
  { $set: { total: { $sum: { $map: { input: "$items", as: "it", in: { $multiply: ["$$it.qty", "$$it.price"] } } } } } },
  { $bucket: { groupBy: "$total", boundaries: [0, 200, 500, 5000], default: "other",
    output: { n: { $sum: 1 }, revenue: { $sum: "$total" } } } }])
// → { _id: 200, n: 1, revenue: 285 } … (mixed Double/Decimal128/String aborts — §20)
```

---

## 11. `$replaceRoot`/`$replaceWith`/`$merge`/`$out`

After `$group`/`$lookup`, promote the computed doc to top level, then persist:
`$merge` upserts (keeps indexes — nightly rollups), `$out` replaces wholesale.

```javascript
// mongosh — flatten _id key, materialize nightly rollup
db.orders.aggregate([{ $group: { _id: "$customerId", revenue: { $sum: "$total" } } },
  { $replaceWith: { customerId: "$_id", revenue: "$revenue" } },
  { $merge: { into: "customer_revenue", on: "customerId", whenMatched: "replace", whenNotMatched: "insert" } }])
// $replaceRoot variant: { $replaceRoot: { newRoot: "$customer" } } — after lookup+unwind.
// $out variant (destructive): { $out: "customer_revenue" }
```

```java
// java — replaceWith then merge (needs MongoDB 4.4+)
Aggregation agg = newAggregation(group("customerId").sum("total").as("revenue"),
  replaceWith(new org.bson.Document("customerId", "$_id").append("revenue", "$revenue")),
  Aggregation.merge().intoCollection("customer_revenue")
    .on("_id").whenMatchedReplace().whenNotMatchedInsert().build());
```

---

## 12. `$setWindowFields` — rank/running total

Partitions + sorts, then window fns: `$rank`/`$denseRank`, `$sum` with
`documents: ["unbounded", "current"]`. Needs MongoDB 5.0+; Spring has no
first-class builder — use a raw `Document` stage.

```javascript
// mongosh — rank customers by revenue + running total
db.orders.aggregate([
  { $set: { total: { $sum: { $map: { input: "$items", as: "it", in: { $multiply: ["$$it.qty", "$$it.price"] } } } } } },
  { $group: { _id: "$customerId", revenue: { $sum: "$total" } } },
  { $setWindowFields: { sortBy: { revenue: -1 }, output: { rank: { $rank: {} },
    runningRevenue: { $sum: "$revenue", window: { documents: ["unbounded", "current"] } } } } },
  { $sort: { rank: 1 } }])
// → C-02/499/rank 1/run 499 · C-01/405/rank 2/run 904 … ($rank gaps on ties; $denseRank doesn't)
```

```java
// java — raw Document window stage (no builder in Spring Data 5.x)
AggregationOperation rankWindow = ctx -> new org.bson.Document("$setWindowFields",
  new org.bson.Document("sortBy", new org.bson.Document("revenue", -1)).append("output",
    new org.bson.Document("rank", new org.bson.Document("$rank", new org.bson.Document())).append(
      "runningRevenue", new org.bson.Document("$sum", "$revenue").append("window",
        new org.bson.Document("documents", java.util.List.of("unbounded", "current"))))));
Aggregation agg = newAggregation(group("customerId").sum("total").as("revenue"),
  rankWindow, sort(Sort.Direction.ASC, "rank"));
```

---

## 13. Scenarios

Shared input: §1 seed (`total` = `$map`/`$multiply` line total; `Decimal128` shell / `BigDecimal` Java).

### 13.1 Total sales by customer

```javascript
// shell
db.orders.aggregate([{ $match: { status: { $in: ["PAID", "SHIPPED"] } } },
  { $set: { total: { $sum: { $map: { input: "$items", as: "it", in: { $multiply: ["$$it.qty", "$$it.price"] } } } } } },
  { $group: { _id: "$customerId", revenue: { $sum: "$total" }, orders: { $sum: 1 } } },
  { $sort: { revenue: -1 } }])
// output → { _id: "C-01", revenue: 405, orders: 2 } { _id: "C-03", revenue: 150, orders: 1 }
```

```java
// Spring — Aggregation.match/group/sort + AggregationResults + DTO
Aggregation agg = newAggregation(match(Criteria.where("status").in("PAID", "SHIPPED")),
  group("customerId").sum("total").as("revenue").count().as("orders"), sort(Sort.Direction.DESC, "revenue"));
AggregationResults<CustomerRevenue> r = mongoTemplate.aggregate(agg, "orders", CustomerRevenue.class);
record CustomerRevenue(String customerId, BigDecimal revenue, long orders) {}
// output → [C-01/405.00/2, C-03/150.00/1]
```

### 13.2 Monthly revenue (`$dateTrunc`)

```javascript
// shell
db.orders.aggregate([
  { $set: { total: { $sum: { $map: { input: "$items", as: "it", in: { $multiply: ["$$it.qty", "$$it.price"] } } } } } },
  { $group: { _id: { $dateTrunc: { date: "$orderedAt", unit: "month" } },
    revenue: { $sum: "$total" }, orders: { $sum: 1 } } }, { $sort: { _id: 1 } }])
// output → Jan-2026: 285 · Feb-2026: 619 · Mar-2026: 150
```

```java
// Spring
Aggregation agg = newAggregation(
  group(DateOperators.dateOf("orderedAt").truncate("month")).sum("total").as("revenue").count().as("orders"),
  sort(Sort.Direction.ASC, "_id"));
AggregationResults<MonthlyRevenue> r = mongoTemplate.aggregate(agg, "orders", MonthlyRevenue.class);
record MonthlyRevenue(Instant id, BigDecimal revenue, long orders) {}
```

### 13.3 Top products (`$unwind`+`$group`+`$sort`+`$limit`)

```javascript
// shell
db.orders.aggregate([{ $unwind: "$items" },
  { $group: { _id: "$items.sku", units: { $sum: "$items.qty" },
    revenue: { $sum: { $multiply: ["$items.qty", "$items.price"] } } } },
  { $sort: { revenue: -1 } }, { $limit: 3 }])
// output → TENT-5/499 · TRAIL-1/360 · SOCK-9/195
```

```java
// Spring
Aggregation agg = newAggregation(unwind("items"),
  group("items.sku").sum("items.qty").as("units")
    .sum(ArithmeticOperators.Multiply.valueOf("items.qty").multiplyBy("items.price")).as("revenue"),
  sort(Sort.Direction.DESC, "revenue"), limit(3));
AggregationResults<ProductSales> r = mongoTemplate.aggregate(agg, "orders", ProductSales.class);
record ProductSales(String sku, long units, BigDecimal revenue) {}
```

### 13.4 Orders by status (`$group` count + `$avg`)

```javascript
// shell
db.orders.aggregate([{ $group: { _id: "$status", n: { $sum: 1 }, avgItems: { $avg: { $size: "$items" } } } },
  { $sort: { n: -1 } }])
// output → PAID: 2 · SHIPPED: 1 · PENDING: 1
```

```java
// Spring
Aggregation agg = newAggregation(
  group("status").count().as("n").avg(ArrayOperators.Size.lengthOfArray("items")).as("avgItems"),
  sort(Sort.Direction.DESC, "n"));
AggregationResults<StatusCount> r = mongoTemplate.aggregate(agg, "orders", StatusCount.class);
record StatusCount(String status, long n, double avgItems) {}
```

### 13.5 Customers with no orders (`$lookup` + match empty)

```javascript
// shell — anti-join: lookup then keep empty arrays
db.customers.aggregate([
  { $lookup: { from: "orders", localField: "_id", foreignField: "customerId", as: "orders" } },
  { $match: { orders: { $eq: [] } } }, { $project: { name: 1, region: 1 } }])
// output → { _id: "C-04", name: "Dev", region: "WEST" }
```

```java
// Spring — Aggregation.lookup + match empty
Aggregation agg = newAggregation(lookup("orders", "_id", "customerId", "orders"),
  match(Criteria.where("orders").is(java.util.List.of())), project("name", "region"));
AggregationResults<Customer> r = mongoTemplate.aggregate(agg, "customers", Customer.class);
// output → [C-04/Dev/WEST]
```

### 13.6 Nested order items (`$unwind` + `$group` back)

```javascript
// shell — per-order lines/maxQty via $first, then per-customer $avg/$max
db.orders.aggregate([{ $unwind: { path: "$items", includeArrayIndex: "lineNo" } },
  { $group: { _id: "$_id", customerId: { $first: "$customerId" },
    lines: { $sum: 1 }, maxQty: { $max: "$items.qty" } } },
  { $group: { _id: "$customerId", avgLines: { $avg: "$lines" }, maxQty: { $max: "$maxQty" } } }])
// output → C-01: avgLines 1.5/maxQty 3 …
```

```java
// Spring — chained $group: per-order then per-customer
Aggregation agg = newAggregation(unwind("items"),
  group("_id").first("customerId").as("customerId").count().as("lines").max("items.qty").as("maxQty"),
  group("customerId").avg("lines").as("avgLines").max("maxQty").as("maxQty"));
```

### 13.7 Pagination (`$facet` metadata + data)

```javascript
// shell — one trip: total + page (empty match → metadata: [] — §20)
db.orders.aggregate([{ $match: { status: { $ne: "CANCELLED" } } }, { $sort: { orderedAt: -1 } },
  { $facet: { metadata: [{ $count: "total" }], data: [{ $skip: 0 }, { $limit: 20 }] } }])
// output → { metadata: [{ total: 4 }], data: […] }
```

```java
// Spring — Aggregation.facet page into DTO
Aggregation agg = newAggregation(match(Criteria.where("status").ne("CANCELLED")),
  sort(Sort.Direction.DESC, "orderedAt"), new FacetOperation()
    .and(Aggregation.count().as("total")).as("metadata").and(skip(0L), limit(20)).as("data"));
AggregationResults<FacetPage> r = mongoTemplate.aggregate(agg, "orders", FacetPage.class);
record FacetPage(List<Count> metadata, List<Order> data) {} record Count(long total) {}
```

---

## 14. TypedAggregation + DTO mapping + AggregationResults

`TypedAggregation` resolves Java field names to doc paths (rename-proof);
`AggregationResults.getMappedResults()` is `[]` (never null) on no match;
`getRawResults()` aids mismatch debugging. DTO ctor names/`@Field` must match
output keys (`$group` emits `_id` — alias it).

```java
// java — type-safe pipeline + mapped results
TypedAggregation<Order> agg = Aggregation.newAggregation(Order.class,
  match(Criteria.where("status").is("PAID")),
  group("customerId").sum("total").as("revenue"), sort(Sort.Direction.DESC, "revenue"));
AggregationResults<CustomerRevenue> r = mongoTemplate.aggregate(agg, CustomerRevenue.class);
List<CustomerRevenue> rows = r.getMappedResults(); // [] when nothing matches
r.getRawResults(); // { cursor, ok } — log on DTO mismatch
```

```javascript
// mongosh — what it sent (names already resolved)
db.orders.aggregate([{ $match: { status: "PAID" } },
  { $group: { _id: "$customerId", revenue: { $sum: "$total" } } }, { $sort: { revenue: -1 } }])
```

---

## 15. `@Aggregation` mirror — search-service `$group`/`$bucket`

Verbatim mirrors of `ProductSearchRepository` ([003](./003-repository-query-methods.md)):
fixed pipelines on repo methods; dynamic ones → `MongoTemplate` ([004](./004-mongotemplate-crud.md)).

```java
// java — mirrored verbatim (read, not compiled)
@Aggregation(pipeline = {"{ '$match': { '_id': { '$in': ?0 } } }",
  "{ '$group': { '_id': '$category', 'count': { '$sum': 1 } } }"})
List<CategoryCount> countByCategory(List<String> productIds);
@Aggregation(pipeline = {"{ '$match': { '_id': { '$in': ?0 } } }",
  "{ '$bucket': { 'groupBy': '$price', 'boundaries': ?1, 'default': 'other', 'output': { 'count': { '$sum': 1 } } } }"})
List<PriceRangeCount> countByPriceRange(List<String> productIds, List<Double> boundaries);
record CategoryCount(String id, long count) {} record PriceRangeCount(String id, long count) {}
```

```javascript
// mongosh — sales equivalents
db.orders.aggregate([{ $match: { status: "PAID" } }, { $group: { _id: "$customerId", count: { $sum: 1 } } }])
db.orders.aggregate([{ $match: { status: "PAID" } },
  { $bucket: { groupBy: "$total", boundaries: [0, 200, 500, 5000],
    default: "other", output: { count: { $sum: 1 } } } }])
```

---

## 16. Optimization — match-first, index use, coalescence

1. **Match-first.** Selective `$match` (status/date) at head; `$lookup`/`$unwind` after — every early-filtered doc skips `$group` cost.
2. **Project-early.** Drop heavy fields post-`$match` (`items.description`, audit blobs) — less BSON through `$sort`/`$group`.
3. **Index use in `$match`/`$sort` prefix.** Leading `$match` on `{ status: 1, orderedAt: -1 }` uses that compound index; same-prefix `$sort` avoids in-memory sort. `$group`-first can't use indexes — filter before grouping.
4. **`$sort`+`$limit` coalescence.** Adjacent stages → top-N sort (bounded heap). Never wedge `$skip`/computed `$project` between them.
5. **`$lookup` last, correlated + indexed.** Join after filtering; index `foreignField`; `let`/`pipeline` form pushes `$match`+`$sort`+`$limit` into the foreign side.

```javascript
// mongosh — good shape + serving index
db.orders.aggregate([{ $match: { status: "PAID" } },
  { $project: { customerId: 1, orderedAt: 1, total: 1 } },
  { $sort: { orderedAt: -1 } }, { $limit: 20 },
  { $lookup: { from: "customers", localField: "customerId", foreignField: "_id", as: "customer" } }])
db.orders.createIndex({ status: 1, orderedAt: -1 })
```

```java
// java — explain before prod (IXSCAN? top-N SORT? spill?)
org.bson.Document explain = mongoTemplate.executeCommand(new org.bson.Document("aggregate", "orders")
  .append("pipeline", agg.getPipeline().getOperations()).append("explain", true));
```

---

## 17. Memory — 100MB + `allowDiskUse`

Each stage caps at **100MB RAM**. Big `$group`/`$sort`/`$bucket` fails without
`allowDiskUse(true)` (pages to temp files — slower, completes). `$facet`
branches share the pressure: past ~100k docs prefer two queries over one giant facet.

```java
// java — disk spill for nightly rollups only, never live pages
AggregationOptions spill = AggregationOptions.builder().allowDiskUse(true).build();
Aggregation agg = newAggregation(Order.class,
  group("customerId").sum("total").as("revenue")).withOptions(spill);
```

```javascript
// mongosh — same knob + the error without it
db.orders.aggregate([{ $group: { _id: "$customerId", revenue: { $sum: "$total" } } }], { allowDiskUse: true })
// w/o flag: "Exceeded memory limit for group, but didn't allow external sort"
```

> Shrink first (§16) — `$match`-first + `$project`-early beats `allowDiskUse` on every percentile.

---

## 18. Pagination+count pattern

`$facet` = total + page in one trip (§13.7), but totals sub-pipeline re-scans the
match set. Large collections: **two queries** — `countDocuments` (same filter,
no skip/limit) + page — same `Page`-vs-`Slice` rule as [003](./003-repository-query-methods.md)
and the `Query.of(q)` cleared-copy rule from `SearchService` ([004](./004-mongotemplate-crud.md)).

```java
// java — two-query page (totals correct, page fast)
Query q = new Query(Criteria.where("status").ne("CANCELLED"));
long total = mongoTemplate.count(q, Order.class);
q.with(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderedAt")));
List<Order> data = mongoTemplate.find(q, Order.class); // total 4 · hasNext false
```

```javascript
// mongosh — two-trip equivalent
db.orders.countDocuments({ status: { $ne: "CANCELLED" } }) // → 4
db.orders.find({ status: { $ne: "CANCELLED" } }).sort({ orderedAt: -1 }).limit(20)
```

---

## 19. Join-vs-embed verdict

| Shape | Read | Write | Agg cost | Verdict |
|---|---|---|---|---|
| embed `items[]` in order | one read, no join | one atomic write | `$unwind`+`$group` in place | ✅ default for sale lines |
| ref `customerId` → customers | needs `$lookup` | independent lifecycles | join after `$match`, index foreignField | ✅ correct for customers |
| embed customer snapshot | fastest reads | stale on rename | none | only immutable copies (name-at-purchase) |

Rule: embed what the write owns together (order+lines); reference what lives
independently (customers). `$lookup` serves case 2, not case 1. Index/tx detail: [008](./008-indexing-query-performance.md).

---

## 20. Pitfalls (GOLD)

!!! warning "GOLD — aggregation pitfalls (read before shipping)"
    - **`$group` sans leading `$match` scans all** — filter indexed `status`/`orderedAt` first (§16).
    - **`_id: null` = one giant group** — grand-total intent only; fatal as typo.
    - **`$unwind` drops empty/missing arrays** — need `preserveNullAndEmptyArrays: true` (§6).
    - **Empty `$facet` metadata = `[]`, not `[{total: 0}]`** — coalesce to 0 in code (§13.7).
    - **`$bucket` single numeric type** — mixed Double/Decimal128/String aborts ([003](./003-repository-query-methods.md)).
    - **`$lookup` always emits array** — `$unwind`/`$first` before projecting `customer.name`.
    - **Anti-join is `[]`, not null** — `{ orders: { $eq: [] } }` (§13.5).
    - **Money = BigDecimal/Decimal128** — `double` `$sum` drifts.
    - **Unindexed `$sort` spills past 100MB** — compound index or batch-only `allowDiskUse` (§17).
    - **`$merge` upserts, `$out` replaces** — `$out` on live collection drops readers' data (§11).

---

## 21. Interview Q&A

**Q1. Why `$match` first, and what index serves `match(status)+sort(orderedAt)`?**
A: Shrinks every downstream stage + can use an index (`$group`-first can't). Compound `{ status: 1, orderedAt: -1 }` serves filter and sort prefix; adjacent `$sort`+`$limit` coalesces to top-N (§16).
**Q2. `$facet` one-trip page vs count+find two-trip — which ships?**
A: `$facet` metadata+data for small/medium sets; count+page for large where totals re-scan (§18). Empty = `metadata: []` → default 0 (§20).
**Q3. Customers with no orders — pipeline and why `[]`?**
A: `$lookup` orders per customer + `$match: { orders: { $eq: [] } }` — `$lookup` always emits an array, so orphans are `[]` not null; keep `preserveNullAndEmptyArrays` on unwinds (§13.5).
**Q4. `$bucket` vs `$bucketAuto`, and what breaks `$bucket`?**
A: Fixed boundaries (tiers) vs N even-ish buckets (shape discovery). Mixed numeric types abort `$bucket` — one type + `default: "other"` (§10, §15 mirror).
**Q5. `allowDiskUse` — what cap, and when is it the wrong fix?**
A: 100MB per-stage RAM cap. Right for batch `$merge` rollups; wrong for live pages — `$match`-first + `$project`-early + sort index fixes latency instead (§16–§17).

---

## 22. Resources

**This track:** [003 — Repository Query Methods](./003-repository-query-methods.md) (mirrored `@Aggregation` `$group`/`$bucket`, `Page` vs `Slice`) · [004 — MongoTemplate CRUD](./004-mongotemplate-crud.md) (`aggregate()`, `Query.of(q)` count, SearchService mirror) · [008](./008-indexing-query-performance.md) (compound indexes, order-write transactions) · [MongoDB intro](./mongodb.md) (collections, `Decimal128`, `mongosh`).

**Official docs:** [Pipeline](https://www.mongodb.com/docs/manual/core/aggregation-pipeline/) · [`$group`](https://www.mongodb.com/docs/manual/reference/operator/aggregation/group/) · [`$lookup`](https://www.mongodb.com/docs/manual/reference/operator/aggregation/lookup/) · [`$facet`](https://www.mongodb.com/docs/manual/reference/operator/aggregation/facet/) · [`$bucket`](https://www.mongodb.com/docs/manual/reference/operator/aggregation/bucket/) · [`$setWindowFields`](https://www.mongodb.com/docs/manual/reference/operator/aggregation/setWindowFields/) · [`$merge`](https://www.mongodb.com/docs/manual/reference/operator/aggregation/merge/) · [Optimization](https://www.mongodb.com/docs/manual/core/aggregation-pipeline-optimization/) · [Spring Aggregation](https://docs.spring.io/spring-data-mongodb/reference/mongodb/aggregation-framework.html) · [`@Aggregation`](https://docs.spring.io/spring-data-mongodb/reference/repositories/aggregation-repository-methods.html)

**Repo mirrors (read, not compiled):** `search-service/.../search/repository/ProductSearchRepository.java` (`countByCategory` `$group`, `countByPriceRange` `$bucket` — §15) · `search-service/.../search/service/SearchService.java` (`MongoTemplate` + `TextQuery` + `Query.of(q)` — §18).

---

*Next: [008](./008-indexing-query-performance.md).*

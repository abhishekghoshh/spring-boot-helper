# 005 — MongoDB Query Language (Boot 4.1.0 + Java 25, Employees Domain)

> Section D of the Spring Mongo track: the full MongoDB Query Language (MQL)
> operator-by-operator — comparison, logical, element, array, regex, `$expr`,
> dates, collation, null semantics — plus update operators, array filters,
> upserts, and atomic conditional updates. One dataset throughout.
>
> **Stack:** Spring Boot **4.1.0** · Java **25** · Spring Data MongoDB · MongoDB **7.x**.
>
> **Convention — verified by reading, not compiling.** Snippets mirror the
> `Product`/`products` conventions in [003](./003-repository-query-methods.md)
> but run against a dedicated `employees` teaching collection so every operator
> result is predictable. Snippets were **not compiled**.
>
> Companion guides: [002](./002-document-mapping.md) · [003](./003-repository-query-methods.md) · [004](./004-mongotemplate-crud.md) · [007](./007-aggregation-framework.md) · [008](./008-indexing-query-performance.md) · [MongoDB intro](./mongodb.md).

## Table of Contents

- [1. The one dataset (employees + products/orders)](#1-the-one-dataset-employees--productsorders)
- [2. Employee mapping (Java)](#2-employee-mapping-java)
- [3. Comparison operators](#3-comparison-operators-query)
- [4. Logical operators](#4-logical-operators-query)
- [5. Element operators](#5-element-operators-query)
- [6. Array operators](#6-array-operators-query)
- [7. $regex and dot notation](#7-regex-and-dot-notation-query)
- [8. $expr and field-to-field](#8-expr-and-field-to-field-query-mostly-aggregation-capable)
- [9. Dates and ranges](#9-dates-and-ranges-query)
- [10. Case-sensitive vs collation](#10-case-sensitive-vs-collation-query)
- [11. Null vs missing](#11-null-vs-missing-query)
- [12. Update operators](#12-update-operators-update)
- [13. Positional $ and array filters](#13-positional--and-array-filters-update)
- [14. Upserts](#14-upserts-update-with-insert-fallback)
- [15. Atomic conditional updates](#15-atomic-conditional-updates-update)
- [16. Operator-kind map](#16-operator-kind-map-query-vs-update-vs-aggregation)
- [17. Pitfalls (GOLD)](#17-pitfalls-gold)
- [18. Interview Q&A](#18-interview-qa)
- [19. Resources](#19-resources)

> **Kind labels used below:** `QUERY` = usable in `find()` filter · `UPDATE` =
> usable in `updateOne/updateMany` update document · `AGGREGATION` = usable as an
> aggregation expression/stage operator. Some operators (e.g. `$eq`, `$gt`) exist
> in both query and aggregation with different syntax — the map in §16 calls this out.

---

## 1. The one dataset (employees + products/orders)

All query results in this guide refer to these 5 `employees` docs. Memorize them;
every "Result" line is a subset of this list.

```javascript
// mongosh — seed (run once)
db.employees.deleteMany({});
db.employees.insertMany([
  { _id: 1, name: "Asha Rao",  dept: "Engineering", salary: 95000,  bonus: 8000,
    skills: ["java", "spring", "mongo"], address: { city: "Austin", zip: "73301" },
    joinDate: ISODate("2022-03-15T00:00:00Z"), active: true, nickname: "Ash" },
  { _id: 2, name: "boris lee", dept: "Engineering", salary: 72000,  bonus: 75000,
    skills: ["java", "react"], address: { city: "Seattle", zip: "98101" },
    joinDate: ISODate("2023-07-01T00:00:00Z"), active: true, nickname: null },
  { _id: 3, name: "Cara Menon", dept: "HR",    salary: 58000,  bonus: 5000,
    skills: ["recruiting", "excel", "onboarding"], address: { city: "Austin", zip: "73301" },
    joinDate: ISODate("2021-01-10T00:00:00Z"), active: false },
  { _id: 4, name: "Dev Patel", dept: "Sales",   salary: 120000, bonus: 20000,
    skills: ["negotiation", "salesforce", "excel"], address: { city: "Denver", zip: "80202" },
    joinDate: ISODate("2020-06-20T00:00:00Z"), active: true },
  { _id: 5, name: "eve kim",   dept: "Engineering", salary: 66000, bonus: 6000,
    skills: ["python", "mongo"], address: { city: "Seattle", zip: "98101" },
    joinDate: ISODate("2024-02-01T00:00:00Z"), active: true, nickname: "Evie" }
]);
// → 5 docs. Note the traps: "boris lee"/"eve kim" are lowercase (case tests),
// _id:2 has explicit nickname:null, _id:3/_id:4 have NO nickname (missing),
// _id:2 has bonus > salary (for $expr).
```

`products` + `orders` (referenced briefly — same operators apply, see [003](./003-repository-query-methods.md)):

```javascript
db.products.insertMany([{ _id: "p1", name: "Trail Sneakers", price: 79.99 }, { _id: "p2", name: "Rain Jacket", price: 149.0 }]);
db.orders.insertMany([{ _id: "o1", productId: "p1", qty: 2 }, { _id: "o2", productId: "p2", qty: 1 }]);
// e.g. db.products.find({ price: { $gte: 50, $lte: 100 } }) → p1. Same $gte/$lte as §3.
```

---

## 2. Employee mapping (Java)

```java
// java — document (imports on first use for this guide)
package com.commercemesh.hr.document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
@Document(collection = "employees")
public class Employee {
    @Id private String id; // mapped from _id; seed uses 1..5 for readability
    private String name; private String dept; private int salary; private int bonus;
    private List<String> skills = new ArrayList<>();
    private Address address; private LocalDateTime joinDate;
    private boolean active; private String nickname; // null vs missing demo (§11)
    public record Address(String city, String zip) {}
}
```

```java
// java — repository slice used through this guide (imports on first use)
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
public interface EmployeeRepository extends MongoRepository<Employee, String> {
    List<Employee> findByDept(String dept);
    List<Employee> findBySalaryGreaterThan(int salary);
    List<Employee> findBySkillsIn(List<String> skills);
}
```

---

## 3. Comparison operators (`QUERY`)

All of `$eq/$ne/$gt/$gte/$lt/$lte/$in/$nin` are **query operators** (also valid
inside `$match`; `$eq/$gt/...` additionally exist as **aggregation expression**
operators with `{$gt: ["$salary", 70000]}` syntax — see §16).

### $eq — equal. **Kind:** `QUERY` (+ `AGGREGATION` form)

- **Context:** exact match on `dept` — Asha, boris, eve are Engineering.
- **mongosh:** `db.employees.find({ dept: { $eq: "Engineering" } })`
- **Spring:** `repository.findByDept("Engineering"); // → { dept: "Engineering" }`
- **Result:** `_id` 1, 2, 5 (Asha, boris, eve).
- **Index note:** equality on `dept` uses a single-field `{ dept: 1 }` index as an
  `IXSCAN`; make it a prefix of compound `{ dept: 1, salary: 1 }` if you also
  range/sort ([008](./008-indexing-query-performance.md)).

### $ne — not equal. **Kind:** `QUERY` (+ `AGGREGATION` form)

- **Context:** everyone NOT in Engineering — Cara (HR), Dev (Sales).
- **mongosh:** `db.employees.find({ dept: { $ne: "Engineering" } })`
- **Spring:**
```java
// java — MongoTemplate (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
List<Employee> r = mongoTemplate.find(new Query(Criteria.where("dept").ne("Engineering")), Employee.class);
```
- **Result:** `_id` 3, 4.
- **Index note:** `$ne` is poorly selective — the planner often prefers a
  `COLLSCAN` or scans most of the index; pair it with a selective `$eq` or use a
  partial index rather than indexing for `$ne` alone.

### $gt / $gte — greater than / greater-or-equal. **Kind:** `QUERY` (+ `AGGREGATION` form)

- **Context:** salaries 95k/72k/58k/120k/66k; threshold 70k splits the set.
- **mongosh:** `db.employees.find({ salary: { $gt: 70000 } })` / `{ salary: { $gte: 72000 } }`
- **Spring:** `repository.findBySalaryGreaterThan(70000);` / `new Query(Criteria.where("salary").gte(72000))`
- **Result:** `$gt: 70000` → 1, 2, 4 · `$gte: 72000` → 1, 2, 4 (boris at exactly 72k included).
- **Index note:** range on `salary` wants `{ salary: 1 }` (or ESR-ordered compound);
  open-ended `$gt` scans the index tail — bounded `$gte/$lte` windows are cheaper.

### $lt / $lte — less than / less-or-equal. **Kind:** `QUERY` (+ `AGGREGATION` form)

- **Context:** junior band — Cara 58k, eve 66k.
- **mongosh:** `db.employees.find({ salary: { $lt: 70000 } })`
- **Spring:**
```java
// java — derived + Criteria equivalent
List<Employee> findBySalaryLessThan(int salary); // → { salary: { $lt: ?0 } }
mongoTemplate.find(new Query(Criteria.where("salary").lt(70000)), Employee.class);
```
- **Result:** `_id` 3, 5.
- **Index note:** same `{ salary: 1 }` range guidance as `$gt`; combine with equality
  first per ESR (`{ dept: 1, salary: 1 }` serves `dept = X AND salary < Y`).

### $in — value in list. **Kind:** `QUERY`

- **Context:** city is Austin OR Denver — Asha, Cara (Austin), Dev (Denver).
- **mongosh:** `db.employees.find({ "address.city": { $in: ["Austin", "Denver"] } })`
- **Spring:** `mongoTemplate.find(new Query(Criteria.where("address.city").in("Austin", "Denver")), Employee.class);`
- **Result:** `_id` 1, 3, 4.
- **Index note:** `$in` with ≤ ~10 values becomes multiple index point-lookups
  (efficient); hundreds of values degrade toward a scan — chunk or restructure.

### $nin — value NOT in list. **Kind:** `QUERY`

- **Context:** skills overlap demo — who knows NEITHER java NOR mongo.
- **mongosh:** `db.employees.find({ skills: { $nin: ["java", "mongo"] } })`
- **Spring:** `mongoTemplate.find(new Query(Criteria.where("skills").nin("java", "mongo")), Employee.class);`
- **Result:** `_id` 3, 4 (Cara, Dev).
- **Index note:** like `$ne`, `$nin` over a multikey index still examines most
  entries — avoid indexing *for* `$nin`; index the positive query shape instead.

---

## 4. Logical operators (`QUERY`)

### $and — all clauses match. **Kind:** `QUERY`

- **Context:** active engineers — Asha, boris, eve (Cara inactive, Dev wrong dept).
- **mongosh:** `db.employees.find({ $and: [{ dept: "Engineering" }, { active: true }] })`
- **Spring:**
```java
// java — MongoTemplate (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
Query q = new Query(new Criteria().andOperator(Criteria.where("dept").is("Engineering"), Criteria.where("active").is(true)));
List<Employee> r = mongoTemplate.find(q, Employee.class);
// derived equivalent: List<Employee> findByDeptAndActiveTrue(String dept);
```
- **Result:** `_id` 1, 2, 5.
- **Index note:** compound `{ dept: 1, active: 1 }` serves the whole `$and` in one
  `IXSCAN`; order fields equality-first ([008](./008-indexing-query-performance.md)).

### $or — any clause matches. **Kind:** `QUERY`

- **Context:** Sales dept OR salary < 60k — Dev (Sales), Cara (58k).
- **mongosh:** `db.employees.find({ $or: [{ dept: "Sales" }, { salary: { $lt: 60000 } }] })`
- **Spring:** `mongoTemplate.find(new Query(new Criteria().orOperator(Criteria.where("dept").is("Sales"), Criteria.where("salary").lt(60000))), Employee.class);`
- **Result:** `_id` 3, 4.
- **Index note:** each `$or` branch needs its OWN index (`{ dept: 1 }`,
  `{ salary: 1 }`); one unindexed branch forces a scan for that branch.

### $nor — none match. **Kind:** `QUERY`

- **Context:** neither Engineering nor active — only Cara (HR + inactive).
- **mongosh:** `db.employees.find({ $nor: [{ dept: "Engineering" }, { active: true }] })`
- **Spring:** `mongoTemplate.find(new Query(new Criteria().norOperator(Criteria.where("dept").is("Engineering"), Criteria.where("active").is(true))), Employee.class);`
- **Result:** `_id` 3.
- **Index note:** `$nor` is the inverse of `$or` — it cannot efficiently drive an
  index; expect a `COLLSCAN` on large collections and keep `$nor` to admin/small sets.

### $not — negates an operator expression. **Kind:** `QUERY` (meta-operator)

- **Context:** salary NOT ≥ 100k — everyone except Dev (120k).
- **mongosh:** `db.employees.find({ salary: { $not: { $gte: 100000 } } })`
- **Spring:**
```java
// java — $not has no first-class Criteria method; use @Query or BasicQuery
import org.springframework.data.mongodb.repository.Query;
@Query("{ 'salary': { '$not': { '$gte': ?0 } } }")
List<Employee> findBySalaryNotGte(int salary);
```
- **Result:** `_id` 1, 2, 3, 5.
- **Index note:** `$not` rarely yields a tight index bound — the planner scans
  broadly; prefer `$lt` (`{ salary: { $lt: 100000 } }`) whenever the negation has
  a direct form.

---

## 5. Element operators (`QUERY`)

### $exists — field presence. **Kind:** `QUERY`

- **Context:** `nickname` present on 1 ("Ash"), 2 (null — still present), missing on 3, 4, 5? No — 5 has "Evie". Missing: 3, 4.
- **mongosh:** `db.employees.find({ nickname: { $exists: true } })` / `{ $exists: false }`
- **Spring:**
```java
// java — MongoTemplate (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
mongoTemplate.find(new Query(Criteria.where("nickname").exists(true)), Employee.class);
```
- **Result:** `exists:true` → 1, 2, 5 · `exists:false` → 3, 4.
- **Index note:** `{ nickname: 1 }` helps `exists:true` (sparse/partial index even
  better — see §11); `exists:false` still scans for absent entries.

### $type — BSON type check. **Kind:** `QUERY` (+ `AGGREGATION` form)

- **Context:** `salary` is int everywhere; `joinDate` is date; `skills` is array.
- **mongosh:** `db.employees.find({ salary: { $type: "int" } })` · `db.employees.find({ joinDate: { $type: "date" } })` · `db.employees.find({ skills: { $type: "array" } })`
- **Spring:**
```java
// java — $type via @Query (no first-class Criteria.type in common use)
import org.springframework.data.mongodb.core.query.BasicQuery;
mongoTemplate.find(new BasicQuery("{ 'salary': { '$type': 'int' } }"), Employee.class);
```
- **Result:** all 5 for each of the three probes (schema is clean here; a stray
  `"95000"` string salary would be excluded from the first probe).
- **Index note:** `$type` does not select an index by itself — combine with an
  equality/range on the same field so the index drives the scan.

---

## 6. Array operators (`QUERY`)

### $all — array contains ALL values (order-free). **Kind:** `QUERY`

- **Context:** Asha `["java","spring","mongo"]` is the only doc with BOTH java AND mongo.
- **mongosh:** `db.employees.find({ skills: { $all: ["java", "mongo"] } })`
- **Spring:**
```java
// java — MongoTemplate (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.util.List;
mongoTemplate.find(new Query(Criteria.where("skills").all(List.of("java", "mongo"))), Employee.class);
// derived $in (ANY) for contrast: List<Employee> findBySkillsIn(List<String> skills);
```
- **Result:** `_id` 1 (Asha only; boris has java but not mongo, eve has mongo but not java).
- **Index note:** multikey `{ skills: 1 }` serves `$all` as intersected point lookups;
  contrast `$in` (OR/any) vs `$all` (AND/all) — see [003](./003-repository-query-methods.md) §14.

### $elemMatch — ONE element satisfies ALL conditions. **Kind:** `QUERY`

- **Context:** single skill matching `^mo` case-insensitive — Asha ("mongo"), eve ("mongo").
- **mongosh:** `db.employees.find({ skills: { $elemMatch: { $regex: "^mo", $options: "i" } } })`
- **Spring:**
```java
// java — scalar-array $elemMatch via @Query (cleanest); doc-arrays use Criteria.elemMatch
import org.springframework.data.mongodb.repository.Query;
@Query("{ 'skills': { '$elemMatch': { '$regex': ?0, '$options': 'i' } } }")
List<Employee> findBySkillPattern(String regex);
```
- **Result:** `_id` 1, 5.
- **Index note:** multikey index can bound the `$elemMatch` prefix when the inner
  predicate is index-friendly (anchored regex, equality); unanchored inner regex scans.

### $size — array length. **Kind:** `QUERY`

- **Context:** skill counts: Asha 3, boris 2, Cara 3, Dev 3, eve 2.
- **mongosh:** `db.employees.find({ skills: { $size: 2 } })`
- **Spring:** `mongoTemplate.find(new Query(Criteria.where("skills").size(2)), Employee.class);`
- **Result:** `_id` 2, 5.
- **Index note:** `$size` CANNOT use an index (no B-tree entry encodes array length);
  for hot "length = N" queries store a `skillsCount` field and index that instead.

---

## 7. $regex and dot notation (`QUERY`)

### $regex — pattern match. **Kind:** `QUERY`

- **Context:** names starting with lowercase letter — "boris lee", "eve kim".
- **mongosh:** `db.employees.find({ name: { $regex: "^[a-z]", $options: "" } }) // case-sensitive`
- **Spring:**
```java
// java — MongoTemplate (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
mongoTemplate.find(new Query(Criteria.where("name").regex("^[a-z]")), Employee.class);
// case-insensitive variant: .regex("^as", "i") → Asha
```
- **Result:** `_id` 2, 5 (case-sensitive `^[a-z]` skips "Asha"/"Cara"/"Dev").
- **Index note (prefix rule):** ONLY left-anchored patterns (`^Ash`, `^trail` in
  [003](./003-repository-query-methods.md) §9/§14) become index range scans; leading
  wildcards (`/rao/i`, `/trail/`) and MOST `"i"` case-insensitive patterns force a
  scan — text index or Atlas `$search` wins at scale ([008](./008-indexing-query-performance.md)).

### Dot notation — reach into embedded docs. **Kind:** `QUERY` (path syntax, not an operator)

- **Context:** Seattle staff live at `address.city = "Seattle"` — boris, eve.
- **mongosh:** `db.employees.find({ "address.city": "Seattle" })`
- **Spring:** `mongoTemplate.find(new Query(Criteria.where("address.city").is("Seattle")), Employee.class); // derived: findByAddress_City(String c) — underscore walks the object`
- **Result:** `_id` 2, 5.
- **Index note:** index the PATH (`{ "address.city": 1 }`), not the parent object;
  an index on bare `address` does NOT serve `address.city` predicates.

---

## 8. $expr and field-to-field (`QUERY`, mostly `AGGREGATION`-capable)

- **Context:** boris is the only employee whose `bonus` (75k) exceeds `salary` (72k).
- **mongosh:** `db.employees.find({ $expr: { $gt: ["$salary", "$bonus"] } }) // salary > bonus`
- **Spring:**
```java
// java — $expr via BasicQuery (field-to-field has no derived-method form)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
mongoTemplate.find(new BasicQuery("{ '$expr': { '$gt': ['$salary', '$bonus'] } }"), Employee.class);
// inverse (bonus >= salary): { '$gte': ['$bonus', '$salary'] } → _id 2 only … wait, bonus>salary only for boris; others → _id 1,3,4,5
```
- **Result:** `salary > bonus` → `_id` 1, 3, 4, 5 · inverse `bonus > salary` → `_id` 2 only.
- **Index note:** `$expr` field-to-field comparisons CANNOT use indexes efficiently
  in most versions — expect a scan; for hot paths precompute a boolean/derived field
  (e.g. `bonusExceedsSalary`) and index THAT. `$expr` shines inside aggregation
  `$match` with computed expressions ([007](./007-aggregation-framework.md)).

---

## 9. Dates and ranges (`QUERY`)

- **Context:** joinDates: Dev 2020-06-20, Cara 2021-01-10, Asha 2022-03-15, boris 2023-07-01, eve 2024-02-01.
- **mongosh:** `db.employees.find({ joinDate: { $gte: ISODate("2022-01-01T00:00:00Z"), $lt: ISODate("2024-01-01T00:00:00Z") } })`
- **Spring:**
```java
// java — MongoTemplate (imports on first use per section)
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
mongoTemplate.find(new Query(Criteria.where("joinDate").gte(LocalDateTime.of(2022, 1, 1, 0, 0)).lt(LocalDateTime.of(2024, 1, 1, 0, 0))), Employee.class);
```
- **Result:** `_id` 1, 2 (Asha 2022, boris 2023 — half-open `[gte, lt)` window).
- **Index note:** date ranges are the textbook single-field range index
  (`{ joinDate: 1 }`); store REAL BSON dates (not strings) or ordering/ranges break —
  see [002](./002-document-mapping.md) §15 and TTL notes in [mongodb.md].

---

## 10. Case-sensitive vs collation (`QUERY` modifier)

- **Context:** `"boris lee" == "Boris Lee"`? Default binary compare says NO.
- **mongosh:**
```javascript
db.employees.find({ name: "Boris Lee" }) // → 0 (binary: "b" ≠ "B")
db.employees.find({ name: "Boris Lee" }).collation({ locale: "en", strength: 2 }) // → _id 2
```
- **Spring:**
```java
// java — collation on the Query (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
Query q = new Query(Criteria.where("name").is("Boris Lee"))
    .collation(Collation.of("en").strength(Collation.ComparisonLevel.secondary()));
mongoTemplate.find(q, Employee.class); // → boris (_id 2)
```
- **Result:** without collation → empty; with `{ locale: "en", strength: 2 }` → `_id` 2.
- **Index note:** a collation-enabled query can ONLY use an index with the SAME
  collation — create `{ name: 1 }` with `collation: { locale: "en", strength: 2 }`
  or the query falls back to a scan; alternatively set collection-default collation
  at `createCollection` ([mongodb.md] collection options).

---

## 11. Null vs missing (`QUERY`)

- **Context:** `nickname`: 1 = "Ash", 2 = explicit `null`, 5 = "Evie", 3/4 = MISSING.
- **mongosh:**
```javascript
db.employees.find({ nickname: null })                              // → 2, 3, 4 (null OR missing!)
db.employees.find({ nickname: { $eq: null, $exists: true } })      // → 2 only (explicit null)
db.employees.find({ nickname: { $ne: null } })                     // → 1, 5
```
- **Spring:**
```java
// java — MongoTemplate (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
mongoTemplate.find(new Query(Criteria.where("nickname").is(null)), Employee.class); // → 2,3,4 (null+missing)
mongoTemplate.find(new Query(Criteria.where("nickname").exists(true).is(null)), Employee.class); // → 2 only
```
- **Result:** `{ nickname: null }` → 2, 3, 4 · +`$exists:true` → 2 only · `$ne: null` → 1, 5.
- **Index note:** sparse index skips missing docs (good for "has nickname" queries);
  partial index `partialFilterExpression: { nickname: { $exists: true } }` is the modern
  equivalent — see [008](./008-indexing-query-performance.md) and [mongodb.md] sparse vs partial.

---

## 12. Update operators (`UPDATE`)

All of `$set/$unset/$inc/$push/$pull/$addToSet/$rename` are **update operators** —
they belong in the update document (`updateOne(filter, update)`), NEVER in a `find()`
filter. Each block below mutates the seed; re-seed (§1) between experiments.

### $set — set/add fields. **Kind:** `UPDATE`

- **Context:** activate Cara (`_id: 3`, currently `active: false`).
- **mongosh:** `db.employees.updateOne({ _id: 3 }, { $set: { active: true } })`
- **Spring:**
```java
// java — MongoTemplate updates (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(3)), new Update().set("active", true), Employee.class);
```
- **Result:** `matchedCount 1, modifiedCount 1`; Cara now `active: true`.
- **Index note:** `$set` on an indexed field forces index maintenance per updated doc —
  batch `$set`s on indexed fields cost write amplification; `$set` on unindexed fields is cheap.

### $unset — remove a field. **Kind:** `UPDATE`

- **Context:** drop eve's nickname (`_id: 5`).
- **mongosh:** `db.employees.updateOne({ _id: 5 }, { $unset: { nickname: "" } })`
- **Spring:** `mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(5)), new Update().unset("nickname"), Employee.class);`
- **Result:** eve's doc no longer HAS `nickname` (now "missing" per §11 — `{ nickname: null }` would match her).
- **Index note:** `$unset` on a sparse/partial-indexed field removes the index entry —
  good (shrinks index); on a regular index writes a null-marker instead.

### $inc — atomic numeric increment. **Kind:** `UPDATE`

- **Context:** give Asha (`_id: 1`) a 5k raise: 95k → 100k.
- **mongosh:** `db.employees.updateOne({ _id: 1 }, { $inc: { salary: 5000 } })`
- **Spring:** `mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(1)), new Update().inc("salary", 5000), Employee.class);`
- **Result:** Asha `salary: 100000`, atomically (no read-modify-write race).
- **Index note:** `$inc` on an indexed numeric field updates the B-tree entry in place —
  contention hotspot if thousands of threads `$inc` the same doc (e.g. counters; shard or bucket them).

### $push — append to array (duplicates allowed). **Kind:** `UPDATE`

- **Context:** boris (`_id: 2`, skills `["java","react"]`) learns mongo.
- **mongosh:** `db.employees.updateOne({ _id: 2 }, { $push: { skills: "mongo" } })`
- **Spring:** `mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(2)), new Update().push("skills", "mongo"), Employee.class);`
- **Result:** boris skills → `["java","react","mongo"]` (now matches `$all: [java, mongo]`).
- **Index note:** each `$push` adds a multikey index entry; unbounded `$push` growth
  bloats the index — cap with `$slice` (`$push: { $each: [...], $slice: -N }`) for rolling windows.

### $pull — remove matching array elements. **Kind:** `UPDATE`

- **Context:** strip "excel" from everyone who has it (Cara, Dev).
- **mongosh:** `db.employees.updateMany({ skills: "excel" }, { $pull: { skills: "excel" } })`
- **Spring:** `mongoTemplate.updateMulti(new Query(Criteria.where("skills").is("excel")), new Update().pull("skills", "excel"), Employee.class);`
- **Result:** Cara → `["recruiting","onboarding"]`, Dev → `["negotiation","salesforce"]`; `matched 2`.
- **Index note:** `$pull` with an unselective filter (`{ skills: "excel" }` alone) scans;
  scope the filter with an indexed `$eq` (e.g. `dept`) so the match phase uses an index.

### $addToSet — append only if absent (set semantics). **Kind:** `UPDATE`

- **Context:** ensure Asha has "java" (she already does — no-op) and eve gains "java".
- **mongosh:** `db.employees.updateOne({ _id: 1 }, { $addToSet: { skills: "java" } }) // no-op` · `db.employees.updateOne({ _id: 5 }, { $addToSet: { skills: "java" } })`
- **Spring:** `mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(5)), new Update().addToSet("skills", "java"), Employee.class);`
- **Result:** Asha unchanged; eve → `["python","mongo","java"]`.
- **Index note:** `$addToSet` reads the array before writing (uniqueness check) — slightly
  heavier than `$push`; prefer `$push` when duplicates are impossible by construction.

### $rename — rename a field. **Kind:** `UPDATE`

- **Context:** rename `nickname` → `alias` for Asha (`_id: 1`).
- **mongosh:** `db.employees.updateOne({ _id: 1 }, { $rename: { nickname: "alias" } })`
- **Spring:** `mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(1)), new Update().rename("nickname", "alias"), Employee.class);`
- **Result:** Asha now has `alias: "Ash"`, no `nickname` (queries on `nickname` stop matching her).
- **Index note:** `$rename` on an indexed field REBUILDS that field's index entries for
  every touched doc — run off-peak on large collections; update `@Field`/repository
  mappings in the same deploy ([002](./002-document-mapping.md) §4).

---

## 13. Positional `$` and array filters (`UPDATE`)

### Positional `$` — first matching element. **Kind:** `UPDATE`

- **Context:** boris skills `["java","react"]` — rename the matched "react" → "reactjs".
- **mongosh:** `db.employees.updateOne({ _id: 2, skills: "react" }, { $set: { "skills.$": "reactjs" } })`
- **Spring:**
```java
// java — positional $ (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
Query q = new Query(Criteria.where("_id").is(2).and("skills").is("react"));
mongoTemplate.updateFirst(q, new Update().set("skills.$", "reactjs"), Employee.class);
```
- **Result:** boris → `["java","reactjs"]` (ONLY the first match is replaced).
- **Index note:** the FILTER (`_id` + multikey `skills`) drives index use, not the `$`;
  positional `$` updates exactly one element — for "all matches" use `$[elem]` below.

### Array filters `$[elem]` — all elements matching a predicate. **Kind:** `UPDATE`

- **Context:** normalize every "excel" skill to "spreadsheets" across Cara and Dev.
- **mongosh:**
```javascript
db.employees.updateMany({ skills: "excel" },
  { $set: { "skills.$[e]": "spreadsheets" } },
  { arrayFilters: [{ e: { $eq: "excel" } }] })
```
- **Spring (stable driver-level form — `MongoCollection` API):**
```java
// java — arrayFilters via the driver (imports on first use per section)
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
MongoCollection<Document> coll = mongoTemplate.getCollection("employees");
coll.updateMany(new Document("skills", "excel"),
  List.of(new Document("$set", new Document("skills.$[e]", "spreadsheets"))),
  new UpdateOptions().arrayFilters(List.of((Bson) new Document("e", new Document("$eq", "excel")))));
```
- **Result:** Cara → `["recruiting","spreadsheets","onboarding"]`, Dev →
  `["negotiation","salesforce","spreadsheets"]`.
- **Index note:** same as `$pull`: the top-level filter should be index-backed;
  `arrayFilters` themselves never use an index (they run per-matched-doc in memory).

---

## 14. Upserts (`UPDATE` with insert fallback)

- **Context:** employee `_id: 99` ("Zed") does not exist yet — upsert creates them; re-run updates them.
- **mongosh:** `db.employees.updateOne({ _id: 99 }, { $set: { name: "Zed", dept: "Engineering", salary: 80000 } }, { upsert: true })`
- **Spring:**
```java
// java — MongoTemplate upsert (imports on first use per section)
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
mongoTemplate.upsert(new Query(Criteria.where("_id").is(99)),
  new Update().set("name", "Zed").set("dept", "Engineering").set("salary", 80000), Employee.class);
```
- **Result:** 1st run → insert (`upsertedId: 99`); 2nd run → matched + updated, no duplicate.
- **Index note:** the upsert FILTER should hit a unique index (`_id` or unique `sku`-style
  key) — concurrent upserts on a non-unique filter can double-insert; add
  `$setOnInsert` for create-only fields (e.g. `joinDate`) so re-runs don't clobber them.

---

## 15. Atomic conditional updates (`UPDATE`)

Single-document writes are atomic; the patterns below turn that into
check-and-set semantics without transactions (see [004](./004-mongotemplate-crud.md)).

### findAndModify — read + write in one atomic step. **Kind:** `UPDATE`

- **Context:** hire-adjust Dev (`_id: 4`): bump salary +10k AND return the NEW doc.
- **mongosh:** `db.employees.findAndModify({ query: { _id: 4 }, update: { $inc: { salary: 10000 } }, new: true })`
- **Spring:**
```java
// java — findAndModify (imports on first use per section)
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
Employee updated = mongoTemplate.findAndModify(new Query(Criteria.where("_id").is(4)),
  new Update().inc("salary", 10000), FindAndModifyOptions.options().returnNew(true), Employee.class);
// → Dev salary 120k → 130k, returned doc already reflects it
```
- **Result:** Dev `salary: 130000`, returned inline (no second `find` race).
- **Index note:** point filter on `_id` = single index seek + in-place update; the
  `returnNew` variant costs one extra in-memory projection — negligible at point scale.

### @Version CAS — optimistic-locking compare-and-swap. **Kind:** `UPDATE`

- **Context:** two threads read Asha (`version: 7`); only the first writer should win.
- **mongosh (manual CAS — what Spring generates):**
```javascript
db.employees.updateOne({ _id: 1, version: 7 }, { $set: { salary: 100000 }, $inc: { version: 1 } })
// → thread A: matched 1 · thread B (stale version 7): matched 0 → retry on fresh read
```
- **Spring:**
```java
// java — declarative optimistic locking (imports on first use per section)
import org.springframework.data.annotation.Version;
public class Employee {
    @Version private Long version; // Spring appends { version: <read> } + $inc version on save
}
// manual CAS equivalent when bypassing save():
Query q = new Query(Criteria.where("_id").is(1).and("version").is(7L));
mongoTemplate.updateFirst(q, new Update().set("salary", 100000).inc("version", 1L), Employee.class);
```
- **Result:** exactly one writer's `matchedCount` is 1; losers see 0 and must re-read
  + retry (never blindly overwrite).
- **Index note:** `_id`-scoped CAS stays a point write; NEVER CAS-scan on an unindexed
  predicate under contention — the loser-retry storm multiplies the scan cost.

---

## 16. Operator-kind map (query vs update vs aggregation)

| Operator | Kind | Where it lives | Notes |
|---|---|---|---|
| `$eq`, `$ne`, `$gt`, `$gte`, `$lt`, `$lte`, `$in`, `$nin` | `QUERY` (+`AGGREGATION` expr form) | `find` filter / `$match` / `{$gt:["$a",b]}` expr | Workhorse filters; see §3 |
| `$and`, `$or`, `$nor`, `$not` | `QUERY` | filter / `$match` | `$not` wraps one operator expr only |
| `$exists`, `$type` | `QUERY` | filter / `$match` | Schema-probing; `$type` also an expr |
| `$all`, `$elemMatch`, `$size` | `QUERY` | filter / `$match` on arrays | `$elemMatch` also projects in `find` projection |
| `$regex` | `QUERY` | filter / `$match` | Left-anchored ⇒ index range; else scan |
| `$expr` | `QUERY` (expression bridge) | filter / `$match` | Lets queries run aggregation expressions (field-to-field) |
| Dot notation (`address.city`) | path syntax | filter / update paths / `$project` | Index the full PATH |
| `$set`, `$unset`, `$inc`, `$push`, `$pull`, `$addToSet`, `$rename` | `UPDATE` | update document only | Never valid in a `find()` filter |
| `$`, `$[elem]` (+ `arrayFilters`) | `UPDATE` | update document paths | `$` = first match; `$[elem]` = all filtered |
| Upsert (`upsert: true`) | `UPDATE` modifier | `updateOne/Many`, `findAndModify` | Insert-on-no-match; needs unique filter |
| `findAndModify`, `@Version` CAS | `UPDATE` pattern | atomic single-doc RMW | No multi-doc transaction needed |
| `$group`, `$bucket`, `$lookup`, `$text`+`$meta` | `AGGREGATION` | pipeline stages (NOT `find`) | Covered in [007](./007-aggregation-framework.md); `$text` in [003](./003-repository-query-methods.md) §9 |

---

## 17. Pitfalls (GOLD)

!!! warning "GOLD — query-language pitfalls (read before shipping)"
    - **`{ nickname: null }` matches missing too** — add `$exists: true` for explicit-null-only (§11).
    - **`$ne`/`$nin`/`$nor`/`$not` are scan-prone** — index the positive shape, not the negation (§3–§4).
    - **Unanchored `$regex` + `"i"` skips indexes** — anchor (`^`) or move to text/`$search` (§7, [003](./003-repository-query-methods.md) §9).
    - **`$size` never uses an index** — materialize a `skillsCount` field for hot length queries (§6).
    - **`$expr` field-to-field doesn't index** — precompute a boolean for hot paths (§8).
    - **Update ops in a `find()` filter silently match nothing** — `$set`/`$inc` belong ONLY in updates (§12, §16).
    - **Positional `$` touches ONE element** — "update all matches" needs `$[elem]` + `arrayFilters` (§13).
    - **Upsert without a unique filter double-inserts under races** — unique index + `$setOnInsert` (§14).
    - **Blind overwrites lose writes** — `findAndModify(returnNew)` or `@Version` CAS for RMW (§15).
    - **Collation mismatch voids indexes** — query collation must equal index collation (§10).

---

## 18. Interview Q&A

**Q1. Why does `{ nickname: null }` return docs that have no `nickname` at all, and how do you get only explicit nulls?**
A: Plain null-equality matches both BSON-null AND missing (docs 2, 3, 4 here). Add presence: `{ nickname: { $eq: null, $exists: true } }` → doc 2 only (§11).

**Q2. Your `{ name: /rao/i }` query is slow on 10M employees — what do you check and what do you change?**
A: `explain("executionStats")` shows `COLLSCAN` — unanchored case-insensitive regex can't range-scan (§7). Anchor it (`^rao`) if UX allows, else move to a text index/`$text` ([003](./003-repository-query-methods.md) §9) or Atlas `$search`, and confirm `IXSCAN` in [008](./008-indexing-query-performance.md).

**Q3. `$in` vs `$all` vs `$elemMatch` on `skills` — when is each correct?**
A: `$in` = ANY overlap (OR across values); `$all` = contains ALL listed values in one array (§6 — only Asha has java+mongo); `$elemMatch` = ONE element satisfies multiple conditions (e.g. one skill matching a regex, or one sub-doc matching score+reviewer).

**Q4. Two instances upsert `{ _id: 99 }` concurrently and you get a duplicate-key error — is that a bug?**
A: No — both saw "no match" and raced to insert (§14). The unique `_id` index did its job by failing one; catch duplicate-key and retry as an update, and use `$setOnInsert` for create-only fields.

**Q5. How do you implement "give Dev a raise only if his salary is still 120k" without transactions?**
A: Single-doc conditional write: `updateOne({ _id: 4, salary: 120000 }, { $inc: { salary: 10000 } })` or `@Version` CAS (`{ _id, version }` + `$inc version`, §15). `matchedCount 0` means stale read → re-fetch and retry; `findAndModify(returnNew)` when you also need the post-image atomically.

---

## 19. Resources

**This track:** [002 — Document Mapping](./002-document-mapping.md) (types, dates, `@Version`) · [003 — Repository Query Methods](./003-repository-query-methods.md) (derived names, `@Query $text/$regex`, `@Aggregation`) · [004 — MongoTemplate CRUD](./004-mongotemplate-crud.md) (`Criteria`/`Query`/`Update`, `findAndModify`, bulk) · [008 — Indexing & Query Performance](./008-indexing-query-performance.md) (`explain`, ESR, multikey/partial/collation indexes) · [MongoDB intro](./mongodb.md) (BSON types, collections, index kinds, `mongosh`).

**Official docs:** [MongoDB — Query Operators](https://www.mongodb.com/docs/manual/reference/operator/query/) · [Update Operators](https://www.mongodb.com/docs/manual/reference/operator/update/) · [Array Update Operators (`$`, `$[elem]`)](https://www.mongodb.com/docs/manual/reference/operator/update/positional/) · [`$expr`](https://www.mongodb.com/docs/manual/reference/operator/query/expr/) · [Collation](https://www.mongodb.com/docs/manual/reference/collation/) · [Null vs Missing](https://www.mongodb.com/docs/manual/faq/developers/#how-do-i-query-for-fields-that-have-null-values-versus-fields-that-are-missing) · [Atomicity & `findAndModify`](https://www.mongodb.com/docs/manual/reference/command/findAndModify/) · [Spring Data MongoDB — `MongoTemplate`](https://docs.spring.io/spring-data-mongodb/reference/mongo-template.html) · [Optimistic Locking (`@Version`)](https://docs.spring.io/spring-data-mongodb/reference/mapping/optimistic-locking.html)

---

*Next: [004 — MongoTemplate CRUD](./004-mongotemplate-crud.md) · deep-dive performance: [008 — Indexing & Query Performance](./008-indexing-query-performance.md).*

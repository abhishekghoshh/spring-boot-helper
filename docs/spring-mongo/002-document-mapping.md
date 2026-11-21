# 002 — Document Mapping (Boot 4.1.0 + Java 25, Product / Order Domain)

> Map `product-catalog-service` shapes (`Product`, `Category`, `Review`) plus an `Order` aggregate with Spring Data MongoDB 5.x — every mapping annotation, its stored JSON, auditing, converters, evolution.
>
> Companion guides: [001 — Spring Boot MongoDB Setup](./001-spring-boot-mongodb-setup.md) · [003 — Repository Query Methods](./003-repository-query-methods.md) · [006 — Data Modeling & Schema Design](./006-data-modeling-schema-design.md) · [MongoDB Introduction](./mongodb.md).

## Table of Contents

- [1. Reference `Product` (catalog mirror)](#1-reference-product-catalog-mirror)
- [2. `@Document`](#2-document)
- [3. `@Id` vs `@MongoId`](#3-id-vs-mongoid)
- [4. `@Field`](#4-field)
- [5. `@Transient`](#5-transient)
- [6. `@Indexed`](#6-indexed)
- [7. `@CompoundIndex` + `@CompoundIndexes`](#7-compoundindex--compoundindexes)
- [8. `@TextIndexed`](#8-textindexed)
- [9. `@GeoSpatialIndexed`](#9-geospatialindexed)
- [10. `@Version`](#10-version)
- [11. `@DBRef` + lazy + N+1](#11-dbref--lazy--n1)
- [12. Auditing](#12-auditing)
- [13. Nested Objects, Arrays, Maps](#13-nested-objects-arrays-maps)
- [14. Enums](#14-enums)
- [15. Dates](#15-dates)
- [16. Custom Converter](#16-custom-converter)
- [17. ObjectId vs String + Counter](#17-objectid-vs-string--counter)
- [18. Records vs Mutable Classes](#18-records-vs-mutable-classes)
- [19. DTOs vs Persistence Models](#19-dtos-vs-persistence-models)
- [20. Nulls and Optional](#20-nulls-and-optional)
- [21. Schema Evolution](#21-schema-evolution)
- [22. Pitfalls (GOLD)](#22-pitfalls-gold)
- [23. Interview Q&A](#23-interview-qa)
- [24. Resources](#24-resources)

> Boot `4.1.0`, Java `25`, `spring-boot-starter-data-mongodb`. Catalog package `com.commercemesh.catalog.document`; orders in `com.commercemesh.order.document`.
---

## 1. Reference `Product` (catalog mirror)

**Purpose:** baseline mirroring real `catalog/document/Product.java`: `@Document(collection="products")`, `@MongoId String id`, `@TextIndexed` name/description, unique `sku`, `BigDecimal` money, `List`/`Map`, `@CreatedDate`/`@LastModifiedDate`.
```java
package com.commercemesh.catalog.document;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class Product {
    @MongoId private String id;
    @TextIndexed private String name;
    @TextIndexed private String description;
    private String brand;
    @Indexed(unique = true) private String sku;
    private BigDecimal price; private BigDecimal compareAtPrice;
    private String currency = "USD"; private String categoryId;
    private List<String> images = new ArrayList<>(); private List<String> tags = new ArrayList<>();
    private Map<String, String> attributes = new HashMap<>();
    private double averageRating = 0.0; private int reviewCount = 0;
    private boolean isActive = true; private boolean isFeatured = false;
    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
}
```
Stored JSON (`db.products.findOne()`):
```json
{"_id":"689f1a2b3c4d5e6f7a8b9c0d1","name":"Aurora Wireless Headphones","description":"Over-ear ANC, 40h battery","brand":"Aurora","sku":"AUR-HP-001","price":{"$numberDecimal":"199.99"},"currency":"USD","categoryId":"cat-audio","tags":["audio","wireless"],"attributes":{"color":"black"},"averageRating":4.6,"reviewCount":128,"isActive":true,"createdAt":{"$date":"2026-06-01T10:15:30Z"},"updatedAt":{"$date":"2026-06-10T09:00:00Z"}}
```
`BigDecimal` → BSON `Decimal128`; `List`/`Map` need no annotation; dates fill only with `@EnableMongoAuditing` (§12).
---

## 2. `@Document`

**Purpose:** marks a Mongo entity. `collection` selects the collection, `collation` sets locale comparison, `language` tunes text-index language. No `changestream` attribute — change streams subscribe via `MongoTemplate`, not mapping.
```java
package com.commercemesh.order.document;
import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection = "orders", language = "english", collation = "en")
public class OrderDoc {
    private String id; private String customerId; private String status;
}
```
Stored JSON:
```json
{"_id":"ORD-9001","customerId":"usr-42","status":"PENDING"}
```
Always set `collection` (default is uncapitalized class name); `collation` must match collection locale; watch via `mongoTemplate.getCollection("orders").watch()` — see [006](./006-data-modeling-schema-design.md).
---

## 3. `@Id` vs `@MongoId`

**Purpose:** both mark `_id`. `@Id` (Commons, portable) vs `@MongoId` (Mongo `targetType`: `OBJECT_ID`, `STRING`, `UUID`, `AUTO`). Catalog uses `@MongoId String id`.
```java
package com.commercemesh.catalog.document;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.data.mongodb.core.mapping.MongoIdField;
@Document(collection = "products")
public class ProductIdDemo {
    @MongoId(targetType = MongoIdField.TargetType.OBJECT_ID) private String id;
    private String sku;
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"sku":"AUR-HP-001"}
```
Prefer `@MongoId` with explicit `targetType`; `@Id String` stores plain string `_id`; never mix both (§17).
---

## 4. `@Field`

**Purpose:** decouples Java name from BSON key — rename Java freely or map legacy `snake_case` keys.
```java
package com.commercemesh.catalog.document;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class ProductFieldDemo {
    @MongoId private String id;
    @Field("display_name") private String name;
    @Field("unit_price") private String price;
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"display_name":"Aurora Headphones","unit_price":"199.99"}
```
Queries use Java names (`findByName` → `display_name`); `@Field` on `_id` is ignored.
---

## 5. `@Transient`

**Purpose:** excludes computed/transport-only state from BSON entirely.
```java
package com.commercemesh.catalog.document;
import java.math.BigDecimal;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class ProductTransientDemo {
    @MongoId private String id;
    private BigDecimal price; private BigDecimal compareAtPrice;
    @Transient private BigDecimal discountAmount; // never stored
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"price":{"$numberDecimal":"199.99"},"compareAtPrice":{"$numberDecimal":"249.99"}}
```
Use Spring's `@Transient`; field is `null` after reload — recompute in getter/`@AfterConvert`, never query it.
---

## 6. `@Indexed`

**Purpose:** single-field index: `unique` (catalog `sku`), `sparse` (skip docs missing field), `direction` (sort order), `expireAfter` (TTL on Date field).
```java
package com.commercemesh.catalog.document;
import java.time.Instant;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class ProductIndexDemo {
    @MongoId private String id;
    @Indexed(unique = true) private String sku;
    @Indexed(sparse = true) private String barcode;
    @Indexed(direction = IndexDirection.DESCENDING) private double averageRating;
    @Indexed(expireAfter = "0") private Instant promoExpiresAt;
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"sku":"AUR-HP-001","averageRating":4.6,"promoExpiresAt":{"$date":"2026-12-31T23:59:59Z"}}
```
Needs index creation (`auto-index-creation: true` or `MongoConfig.ensureIndex`); TTL needs BSON Date, deletes ~60s late; unique without `sparse` collides on missing `null`.
---

## 7. `@CompoundIndex` + `@CompoundIndexes`

**Purpose:** multi-field index in ESR order (equality → sort → range); repeat via wrapper.
```java
package com.commercemesh.order.document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "orders")
@CompoundIndexes({
    @CompoundIndex(name = "customer_status_created", def = "{'customerId': 1, 'status': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "status_created", def = "{'status': 1, 'createdAt': -1}", background = true)
})
public class OrderIndexDemo {
    @MongoId private String id;
    private String customerId; private String status; private String createdAt;
}
```
Stored JSON:
```json
{"_id":"ORD-9001","customerId":"usr-42","status":"PENDING","createdAt":{"$date":"2026-10-01T08:00:00Z"}}
```
Equality first; one special (text/geo) index per compound max; confirm `IXSCAN` via `explain("executionStats")` ([003](./003-repository-query-methods.md)).
---

## 8. `@TextIndexed`

**Purpose:** joins collection's single text index for `$text` search (stemming, stop-words, `weight`). Catalog: `name` + `description`.
```java
package com.commercemesh.catalog.document;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class ProductTextDemo {
    @MongoId private String id;
    @TextIndexed(weight = 3) private String name;
    @TextIndexed(weight = 1) private String description;
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"name":"Aurora Wireless Headphones","description":"Over-ear ANC, 40h battery"}
```
One text index per collection (both share `product_text_search`, also ensured in `MongoConfig`); no fuzzy tolerance — needs Atlas Search ([mongodb.md](./mongodb.md)).
---

## 9. `@GeoSpatialIndexed`

**Purpose:** `2dsphere` (GeoJSON) or `2d` index for `$near`/`$geoWithin` fulfilment queries.
```java
package com.commercemesh.catalog.document;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "stores")
public class Store {
    @MongoId private String id; private String name;
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE) private GeoJsonPoint location;
}
```
Stored JSON (`[lng, lat]` order):
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d2"},"name":"SoHo Hub","location":{"type":"Point","coordinates":[-73.99,40.73]}}
```
Reversed coords query wrong hemisphere; geo + text cannot share one compound index.
---

## 10. `@Version`

**Purpose:** optimistic-lock counter per `save()`; stale writers get `OptimisticLockingFailureException` instead of clobbering stock.
```java
package com.commercemesh.catalog.document;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class ProductVersionDemo {
    @MongoId private String id; private String sku; private int stock;
    @Version private Long version;
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"sku":"AUR-HP-001","stock":41,"version":7}
```
Use `Long` (starts `0`); retry read-mutate-save on conflict; never set manually or bypass via raw `$inc`.
---

## 11. `@DBRef` + lazy + N+1

**Purpose:** stores pointer (`$ref`/`$id`) resolved on load; `lazy=true` defers to first access.
```java
package com.commercemesh.order.document;
import com.commercemesh.catalog.document.Product;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "orders")
public class OrderDbRefDemo {
    @MongoId private String id;
    @DBRef(lazy = true) private Product product;
    private int quantity;
}
```
Stored JSON:
```json
{"_id":"ORD-9001","product":{"$ref":"products","$id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"}},"quantity":2}
```
> ⚠️ N+1: 100 orders → up to 100 extra `findOne`s. Prefer embedded snapshots (`sku`,`name`,`unitPrice`) or plain `productId` + batched `findAllById`/`$lookup` ([006](./006-data-modeling-schema-design.md)).
---

## 12. Auditing

**Purpose:** auto-fills lifecycle metadata. Dates need `@EnableMongoAuditing`; user fields also need `AuditorAware<T>`.
```java
package com.commercemesh.catalog.document;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class ProductAuditDemo {
    @MongoId private String id; private String sku;
    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String lastModifiedBy;
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"sku":"AUR-HP-001","createdAt":{"$date":"2026-06-01T10:15:30Z"},"updatedAt":{"$date":"2026-06-10T09:00:00Z"},"createdBy":"catalog-importer","lastModifiedBy":"pricing-service"}
```
Config — real `MongoConfig` plus missing `AuditorAware`:
```java
package com.commercemesh.catalog.config;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    private final MongoTemplate mongoTemplate;
    public MongoConfig(MongoTemplate mongoTemplate) { this.mongoTemplate = mongoTemplate; }
    @Bean public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system"); // prod: SecurityContextHolder...getName()
    }
}
```
Without annotation all four stay `null`; `@CreatedDate` write-once, `@LastModifiedDate` per save.
---

## 13. Nested Objects, Arrays, Maps

**Purpose:** co-locate read-together bounded data (address, line items, variant map) in one atomic doc. No annotation needed.
```java
package com.commercemesh.order.document;
import java.util.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "orders")
public class Order {
    @MongoId private String id; private String customerId;
    private Address shippingAddress;
    private List<LineItem> items = new ArrayList<>(); private Map<String, String> attributes = new HashMap<>();
    public record Address(String street, String city, String zip) {}
    public record LineItem(String sku, String name, int qty, String unitPrice) {}
}
```
Stored JSON:
```json
{"_id":"ORD-9001","customerId":"usr-42","shippingAddress":{"street":"1 Elm St","city":"Denver","zip":"80202"},"items":[{"sku":"AUR-HP-001","qty":1,"unitPrice":"199.99"}],"attributes":{"giftWrap":"true"}}
```
Embed bounded/read-together data; `Map` keys forbid `.`/`$`-prefix; `items.sku` works but multikey indexes multiply per element.
---

## 14. Enums

**Purpose:** closed vocabulary persisted as readable `name()` string by default.
```java
package com.commercemesh.order.document;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "orders")
public class OrderStatusDemo {
    @MongoId private String id; private OrderStatus status;
    public enum OrderStatus { PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED }
}
```
Stored JSON:
```json
{"_id":"ORD-9001","status":"SHIPPED"}
```
Enum names are storage contract — renames orphan docs (stable `code` + §16 converter instead); never `ordinal()`; validate at DTO layer.
---

## 15. Dates

**Purpose:** both map to BSON Date, but `Instant` is absolute UTC while `LocalDateTime` is zone-less wall-clock. Prefer `Instant`.
```java
package com.commercemesh.catalog.document;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class ProductDateDemo {
    @MongoId private String id;
    private Instant promoStartsAt; private LocalDateTime displayFrom; // preferred vs legacy
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"promoStartsAt":{"$date":"2026-11-01T00:00:00Z"},"displayFrom":{"$date":"2026-11-01T00:00:00Z"}}
```
Built-in converters exist but `LocalDateTime` resolves in JVM zone; never store dates as strings (breaks ranges, sorts, TTL).
---

## 16. Custom Converter

**Purpose:** teach mapper a new pair — `Money` record as legacy `"199.99|USD"` string.
```java
package com.commercemesh.catalog.converters;
import com.commercemesh.catalog.document.Money;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
public final class MoneyConverters {
    private MoneyConverters() {}
    @WritingConverter public static class MoneyWriteConverter implements Converter<Money, String> {
        @Override public String convert(Money s) { return s.amount() + "|" + s.currency(); }
    }
    @ReadingConverter public static class MoneyReadConverter implements Converter<String, Money> {
        @Override public Money convert(String s) { String[] p = s.split("\\|", 2); return new Money(p[0], p[1]); }
    }
    @Configuration public static class MongoConverterConfig {
        @Bean public MongoCustomConversions mongoCustomConversions() {
            return new MongoCustomConversions(List.of(new MoneyWriteConverter(), new MoneyReadConverter()));
        }
    }
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"price":"199.99|USD"}
```
Annotate each direction; register both or reads fail after writes; keep converters null-safe and round-trippable.
---

## 17. ObjectId vs String + Counter

**Purpose:** `ObjectId` = coordination-free default (catalog); human sequences need atomic `counters` + `findAndModify`.
```java
package com.commercemesh.order.document;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection = "orders")
public class OrderIdDemo {
    @Id private ObjectId id; private String orderNumber; // display sequence
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"orderNumber":"ORD-9001"}
```
Counter:
```java
package com.commercemesh.order.service;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
@Service
public class SequenceService {
    private final MongoTemplate mongoTemplate;
    public SequenceService(MongoTemplate mongoTemplate) { this.mongoTemplate = mongoTemplate; }
    public long nextOrderNumber() {
        Document c = mongoTemplate.findAndModify(Query.query(Criteria.where("_id").is("orderNumber")),
                new Update().inc("seq", 1), FindAndModifyOptions.options().returnNew(true).upsert(true),
                Document.class, "counters");
        return c == null ? 1L : c.getLong("seq");
    }
}
```
Sequences serialize on one doc — display numbers only, never shard keys.
---

## 18. Records vs Mutable Classes

**Purpose:** records = concise immutable snapshots; mutable classes = proxies, partial updates.
```java
package com.commercemesh.catalog.document;
import java.math.BigDecimal;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public record ProductRecord(@MongoId String id, String sku, String name, BigDecimal price) {}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"sku":"AUR-HP-001","name":"Aurora Headphones","price":{"$numberDecimal":"199.99"}}
```
Records need canonical constructor, rewrite whole doc per change; lazy `@DBRef` needs mutable classes — entities mutable, records for snapshots/projections.
---

## 19. DTOs vs Persistence Models

**Purpose:** `@Document` = storage contract (BSON, indexes, auditing); DTO = API contract (validation, shaping). Never expose one as other.
```java
package com.commercemesh.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal; import java.util.List; import java.util.Map;
public record ProductRequest(@NotBlank String name, String description, @NotBlank String sku,
        @Positive BigDecimal price, String categoryId, List<String> tags, Map<String, String> attributes) {}
```
Stored DTO JSON (no `_id`/audit fields):
```json
{"name":"Aurora Headphones","description":"ANC over-ear","sku":"AUR-HP-001","price":199.99,"categoryId":"cat-audio","tags":["audio"],"attributes":{"color":"black"}}
```
Map in service layer; validation on DTOs, persistence annotations on documents (§21).
---

## 20. Nulls and Optional

**Purpose:** Mongo separates *missing* from *explicit null*; `Optional` fields break mapping.
```java
package com.commercemesh.catalog.document;
import java.util.Optional;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@Document(collection = "products")
public class ProductNullDemo {
    @MongoId private String id; private String brand; // null → key omitted
    public Optional<String> brandOpt() { return Optional.ofNullable(brand); } // accessor only
}
```
Stored JSON:
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"}}
```
Never declare `Optional<T>` fields; `{brand:null}` matches missing + null — use `{brand:{$exists:true,$eq:null}}` for explicit-null only.
---

## 21. Schema Evolution

**Purpose:** v1 docs (no `currency`, string `price`) must load under v2 — tolerant reads + `schemaVersion` + lazy migration.
```java
package com.commercemesh.catalog.document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "products")
public class ProductEvolved {
    @MongoId private String id; private String sku;
    private String currency = "USD"; private int schemaVersion = 2;
    @Version private Long version;
}
```
Stored JSON (both load into `ProductEvolved`):
```json
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d1"},"sku":"AUR-HP-001","price":"199.99"}
{"_id":{"$oid":"689f1a2b3c4d5e6f7a8b9c0d2"},"sku":"AUR-HP-002","price":{"$numberDecimal":"149.99"},"currency":"EUR","schemaVersion":2,"version":3}
```
Never rename without `@Field` alias; backfill batched `$set` + `schemaVersion` under `moderate` validation ([006](./006-data-modeling-schema-design.md)).
---

## 22. Pitfalls (GOLD)

1. Auditing silently off — no `@EnableMongoAuditing`, date/user fields stay `null`.
2. Missing `AuditorAware` — dates fill, `@CreatedBy`/`@LastModifiedBy` stay `null`.
3. `@Id`/`@MongoId` drift — string vs `ObjectId` `_id` breaks `$lookup` typing.
4. `LocalDateTime` zone drift — new timestamps use `Instant`.
5. Unique without `sparse` — missing values collide on `null`.
6. TTL on non-Date — never fires, and ~60s late anyway.
7. `@DBRef` N+1 — embed snapshots or batch-load instead.
8. Second text index — only one per collection; share via weights.
9. `Optional` fields — mapper cannot round-trip; getter only.
10. `@Field` rename without alias — old docs unreadable; alias or migrate with `schemaVersion`.
---

## 23. Interview Q&A

**Q1. `@Id` vs `@MongoId` — when does it bite?**
A: `@MongoId(OBJECT_ID)` stores native `ObjectId`; `@Id String` stores string. Mixed services silently return empty `$lookup`s across the type boundary.
**Q2. Why avoid `@DBRef` at scale, and what replaces it?**
A: Pointer + per-doc round-trips (N+1), no join, no integrity. Embed snapshots for read-together data, or plain id + batched `findAllById`/`$lookup`.
**Q3. `Instant` vs `LocalDateTime` for `createdAt`?**
A: `Instant` is absolute UTC; `LocalDateTime` is zone-less, resolved per JVM zone. New fields use `Instant`; catalog keeps `LocalDateTime` for legacy.
**Q4. How do auditing fields populate?**
A: `@EnableMongoAuditing` fills `@CreatedDate` (once)/`@LastModifiedDate` (per save); `@CreatedBy`/`@LastModifiedBy` additionally call `AuditorAware<T>`.
**Q5. Zero-downtime `products` evolution?**
A: Tolerant reads (ignore-unknown, defaults, `schemaVersion`), dual-read via `@Field` aliases, batched `$set` backfill under `moderate` validation, then tighten.
---

## 24. Resources

**This track (in order):**
- [001 — Spring Boot MongoDB Setup](./001-spring-boot-mongodb-setup.md) — dependencies, `MongoTemplate`, auto-index creation (prerequisite).
- [003 — Repository Query Methods](./003-repository-query-methods.md) — derived queries over these mapped fields (follow-up).
- [006 — Data Modeling & Schema Design](./006-data-modeling-schema-design.md) — embedding vs referencing, validation, migrations (follow-up).
- [MongoDB Introduction](./mongodb.md) — BSON types, `Decimal128`, index fundamentals.
**Official docs:** [Mapping](https://docs.spring.io/spring-data-mongodb/reference/mongodb/mapping.html) · [Indexing](https://docs.spring.io/spring-data-mongodb/reference/mongodb/indexing.html) · [Auditing](https://docs.spring.io/spring-data-mongodb/reference/mongodb/auditing.html) · [Custom Conversions](https://docs.spring.io/spring-data-mongodb/reference/mongodb/custom-conversions.html) · [Change Streams](https://www.mongodb.com/docs/manual/changeStreams/)
**Repo mirrors:** `projects-v2/commerce-mesh/backend/product-catalog-service/.../document/Product.java` · `.../document/Category.java` (`slug` unique) · `.../document/Review.java` (`@Indexed productId`) · `.../config/MongoConfig.java` (`@EnableMongoAuditing` + text-index bootstrap).
---
*Next: [003 — Repository Query Methods](./003-repository-query-methods.md).*

# 011 — Reactive MongoDB, Validation & Testing (Boot 4.1.0 + Java 25, Product Domain)

> Reactive `ReactiveMongoRepository` + `ReactiveMongoTemplate` (`Mono`/`Flux`), backpressure, `retryWhen`/`backoff`, reactive TX, validation + error mapping, and the full test pyramid — on the product domain.
>
> **Stack:** Spring Boot **4.1.0** · Java **25** · Spring Data MongoDB 5.x · Reactor 3.x · MongoDB **7.x/8.x** (replica set for TX) · JUnit 5 + Mockito + AssertJ + `StepVerifier` + Testcontainers.
>
> **Convention mirror — verified by reading, not compiling.** Product shape mirrors `product-catalog-service/.../catalog/document/Product.java` (`products`, unique `sku`); query shapes mirror [003](./003-repository-query-methods.md). Snippets were **not compiled.**
>
> Companion guides: [003](./003-repository-query-methods.md) · [007](./007-aggregation-framework.md) · [009](./009-transactions-concurrency.md) · [MongoDB intro](./mongodb.md) · [Testing with Spring Boot](../spring-testing/001-testing-with-spring-boot.md).

## Table of Contents

- [1. Mental Model](#1-mental-model-when-reactive-pays) · [2. Reactive Streams](#2-reactive-streams-fundamentals) · [3. ReactiveMongoRepository](#3-reactivemongorepository) · [4. ReactiveMongoTemplate](#4-reactivemongotemplate) · [5. Mono/Flux CRUD](#5-monoflux-crud--query-composition) · [6. Backpressure](#6-backpressure-handling) · [7. Retries](#7-error-handling-and-retries) · [8. Reactive TX](#8-reactive-transactions) · [9. StepVerifier](#9-testing-reactive-with-stepverifier) · [10. Reactive vs Imperative](#10-reactive-vs-imperative-decision) · [11. Bean Validation](#11-bean-validation-on-dtos) · [12. Schema Validation](#12-mongodb-schema-validation) · [13. Error Catalog](#13-error-catalog-and-http-mapping) · [14. Unit](#14-unit-tests) · [15. Slice](#15-datamongotest-slice) · [16. IT](#16-testcontainers-it) · [17. Index/Agg/TX/Page/Ser](#17-index-aggregation-tx-pagination-serialization) · [18. Isolation](#18-isolation-and-no-mock-for-queries) · [19. Pitfalls](#19-pitfalls-gold) · [20. Q&A](#20-interview-qa) · [21. Resources](#21-resources)

```xml
<!-- Boot 4.1.0 + Java 25 + reactive mongo + validation + test -->
<parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>4.1.0</version></parent>
<properties><java.version>25</java.version></properties>
<!-- + spring-boot-starter-data-mongodb-reactive, spring-boot-starter-validation -->
<!-- test: spring-boot-starter-test, reactor-test, testcontainers mongodb + junit-jupiter -->
```

```java
package com.commercemesh.catalog.document; // products collection, used everywhere below
import java.math.BigDecimal; import java.time.Instant; import java.util.List;
import org.springframework.data.annotation.CreatedDate; import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection = "products")
public class Product {
    @Id private String id; @Indexed(unique = true) private String sku;
    private String name; private String brand; private String categoryId;
    private BigDecimal price; private List<String> tags = List.of();
    private double averageRating; private boolean isActive = true;
    @CreatedDate private Instant createdAt;
    // getters / setters omitted
}
```

---

## 1. Mental Model: When Reactive Pays

Reactive keeps a **few event-loop threads** busy while I/O is in flight instead of parking one thread per request. It pays for **I/O-bound, high-concurrency, streaming** work (browse `Flux`, autocomplete, change-stream tails). It never speeds up CPU-bound pricing math, and **one blocking call collapses the benefit** (§10).
```text
Imperative: request -> thread BLOCKS on mongo -> resumes (1 thread / request)
Reactive:   request -> subscribe -> thread freed -> onNext/onComplete resumes
Rule: all-or-nothing — controller to driver must be non-blocking.
```

## 2. Reactive Streams Fundamentals

Four interfaces (`org.reactivestreams`, implemented by Reactor); `Mono<T>` = 0..1, `Flux<T>` = 0..N. **Publisher** produces `onNext*` then `complete|error`; **Subscriber** consumes (never call by hand — use operators + `StepVerifier`, §9); **Subscription** carries demand via `request(n)`; **backpressure** = slow consumer throttles fast producer without unbounded buffering — the reactive Mongo driver pages `batchSize` only as requested. **Nothing runs until subscribed** (WebFlux subscribes for you), which is why `@Transactional` on a `Mono` (§8) needs a subscriber.
```java
import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
Mono<Product> one = Mono.just(new Product()); // findById / save shape
Flux<Product> many = Flux.just(new Product()); // findAll / search shape
Flux<String> skus = many.map(Product::getSku).filter(s -> s != null).take(100); // lazy until subscribed
```

## 3. ReactiveMongoRepository

Reactive twin of the [003](./003-repository-query-methods.md) repository: same derived names, `@Query`, `@Aggregation` — `Mono`/`Flux` returns. `save` → `Mono<Product>` (id arrives in `onNext`); `deleteById` → `Mono<Void>` (`verifyComplete()`).
```java
package com.commercemesh.catalog.repository;
import com.commercemesh.catalog.document.Product; import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
public interface ReactiveProductRepository extends ReactiveMongoRepository<Product, String> {
    Mono<Product> findBySku(String sku);
    Flux<Product> findByCategoryId(String categoryId);
    Flux<Product> findByBrandAndIsActiveTrue(String brand);
    Flux<Product> findByTagsIn(List<String> tags);
    @Query("{ $text: { $search: ?0 }, isActive: true }") Flux<Product> searchActive(String keyword);
    Flux<Product> findByIsActiveTrue(Pageable pageable); // LIMIT/OFFSET Flux, no Page wrapper
}
```

## 4. ReactiveMongoTemplate

Dynamic queries/updates beyond derived names (reactive flavor of [004](./004-mongotemplate-crud.md); stages in [007](./007-aggregation-framework.md)).
```java
package com.commercemesh.catalog.repository;
import com.commercemesh.catalog.document.Product;
import org.springframework.data.domain.Sort; import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria; import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update; import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
@Repository
public class ReactiveProductTemplate {
    private final ReactiveMongoTemplate template;
    public ReactiveProductTemplate(ReactiveMongoTemplate template) { this.template = template; }
    public Flux<Product> search(String brand, String categoryId, int limit) {
        Criteria c = new Criteria().and("isActive").is(true);
        if (brand != null) c.and("brand").is(brand);
        if (categoryId != null) c.and("categoryId").is(categoryId);
        return template.find(Query.query(c).with(Sort.by(Sort.Direction.DESC, "averageRating")).limit(limit), Product.class);
    }
    public Mono<Long> deactivateBrand(String brand) {
        return template.updateMulti(Query.query(Criteria.where("brand").is(brand)),
                new Update().set("isActive", false), Product.class).map(r -> r.getModifiedCount());
    }
}
```

## 5. Mono/Flux CRUD + Query Composition

Compose, never block. Each operator returns a new lazy publisher; one subscription runs the whole chain. Debug with `.checkpoint("browse.search")`; remove `.log()` before merging.
```java
package com.commercemesh.catalog.service;
import com.commercemesh.catalog.document.Product;
import com.commercemesh.catalog.repository.ReactiveProductRepository;
import java.time.Duration; import java.util.List; import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
@Service
public class ReactiveCatalogService {
    private final ReactiveProductRepository products;
    public ReactiveCatalogService(ReactiveProductRepository products) { this.products = products; }
    public Mono<Product> createThenFeature(Product draft) {
        return products.save(draft).flatMap(s -> products.findById(s.getId()))
                .doOnNext(p -> p.setFeatured(true)).flatMap(products::save)
                .timeout(Duration.ofSeconds(3)); // -> QueryTimeoutException path (§13)
    }
    public Flux<Product> browse(String keyword, String categoryId, int limit) {
        return products.searchActive(keyword)
                .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
                .sort((a, b) -> Double.compare(b.getAverageRating(), a.getAverageRating()))
                .take(limit).distinct(Product::getSku); // take() cancels upstream demand (§6)
    }
    public Flux<Product> fetchMany(List<String> ids) {
        return Flux.fromIterable(ids).flatMapSequential(products::findById, 8);
    }
    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String id) { super("product not found: " + id); }
    }
}
```

## 6. Backpressure Handling

Demand flows upstream (Reactor → driver → cursor `batchSize`). Do not defeat it: cap concurrency, bound buffers, chunk exports. Never `collectList()` an unbounded `Flux` or uncapped `flatMap` over 100k ids; prefer keyset paging (`_id > lastSeen` + `limit`) over deep `skip/limit`.
```java
products.findByIsActiveTrue(PageRequest.of(0, 50)); // bounded demand
products.findAll().publishOn(Schedulers.boundedElastic(), 64)
        .onBackpressureBuffer(1_000, dropped -> log.warn("drop {}", dropped.getSku()), BufferOverflowStrategy.DROP_LATEST)
        .concatMap(p -> renderAsync(p), 4).subscribe(); // max 4 in-flight, order kept
products.findAll().window(500) // huge export in constant memory; block at batch-job edge only
        .flatMap(w -> w.collectList().flatMap(this::writeCsvChunk), 2).blockLast(Duration.ofMinutes(5));
```

## 7. Error Handling and Retries

`retryWhen(Retry.backoff(...))` for transient only; `onErrorMap` translates, `onErrorResume` falls back. `retryWhen` **before** `onErrorMap` (retry raw signal, translate final failure). Business errors (`DuplicateKeyException`, validation) never enter `retryWhen`; cap attempts so a failover does not self-DDoS.
```java
package com.commercemesh.catalog.service;
import com.commercemesh.catalog.document.Product;
import com.commercemesh.catalog.repository.ReactiveProductRepository;
import com.mongodb.MongoSocketException; import java.time.Duration;
import org.springframework.dao.QueryTimeoutException; import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono; import reactor.util.retry.Retry;
@Service
public class ResilientCatalogService {
    private final ReactiveProductRepository products;
    public ResilientCatalogService(ReactiveProductRepository products) { this.products = products; }
    public Mono<Product> findBySkuResilient(String sku) {
        return products.findBySku(sku).timeout(Duration.ofSeconds(2))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)).maxBackoff(Duration.ofSeconds(2))
                        .filter(ex -> ex instanceof MongoSocketException || ex instanceof QueryTimeoutException)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorMap(ex -> !(ex instanceof ProductMissingException),
                        ex -> new CatalogUnavailableException("catalog read failed: " + sku, ex))
                .switchIfEmpty(Mono.error(new ProductMissingException(sku)));
    }
    public static class ProductMissingException extends RuntimeException {
        public ProductMissingException(String s) { super("missing: " + s); }
    }
    public static class CatalogUnavailableException extends RuntimeException {
        public CatalogUnavailableException(String m, Throwable c) { super(m, c); }
    }
}
```

## 8. Reactive Transactions

Same ACID as [009](./009-transactions-concurrency.md); plumbing differs: `ReactiveMongoTransactionManager` binds the `ClientSession` to the Reactor `Context`, TX **commits when the returned `Mono`/`Flux` completes**. **Replica set required** — standalone aborts with `Transaction numbers are only allowed on a replica set member or mongos`. No `.block()`/JDBC/`sleep` inside; self-invocation bypasses the proxy ([009](./009-transactions-concurrency.md) §7); 2–10 docs under ~100 ms; programmatic alternative is `TransactionalOperator`.
```java
package com.commercemesh.catalog.config;
import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
@Configuration
public class ReactiveTxConfig {
    @Bean // never coexist with imperative MongoTransactionManager on the same factory
    ReactiveMongoTransactionManager reactiveTxManager(ReactiveMongoDatabaseFactory factory) {
        return new ReactiveMongoTransactionManager(factory);
    }
}
```
```java
package com.commercemesh.catalog.service;
import com.commercemesh.catalog.document.Product;
import com.commercemesh.catalog.repository.ReactiveProductRepository;
import java.math.BigDecimal; import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; import reactor.core.publisher.Mono;
@Service
public class ReactiveStockService {
    private final ReactiveProductRepository products;
    public ReactiveStockService(ReactiveProductRepository products) { this.products = products; }
    @Transactional // commits on Mono completion; rolls back on onError; caller MUST subscribe
    public Mono<Product> reprice(String id, BigDecimal next) {
        return products.findById(id).switchIfEmpty(Mono.error(new IllegalArgumentException("missing " + id)))
                .doOnNext(p -> p.setPrice(next)).flatMap(products::save);
    }
}
```

## 9. Testing Reactive with StepVerifier

Subscribes, drives demand, asserts `onNext* → complete|error`. Always end with `verify…()` — otherwise nothing subscribes and the test is vacuous. `expectNextCount(n)`, `assertNext(…)`, `StepVerifierOptions.create().initialRequest(1)` proves backpressure demand (§6).
```java
package com.commercemesh.catalog.service;
import com.commercemesh.catalog.document.Product;
import com.commercemesh.catalog.repository.ReactiveProductRepository;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks; import org.mockito.Mock; import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux; import reactor.core.publisher.Mono; import reactor.test.StepVerifier;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class ReactiveCatalogServiceTest {
    @Mock ReactiveProductRepository products;
    @InjectMocks ReactiveCatalogService service;
    @Test
    void browseSortsAndCaps() {
        Product a = new Product(); a.setSku("A"); a.setAverageRating(4.9); a.setCategoryId("c1");
        Product b = new Product(); b.setSku("B"); b.setAverageRating(3.1); b.setCategoryId("c1");
        when(products.searchActive("boot")).thenReturn(Flux.just(b, a));
        StepVerifier.create(service.browse("boot", "c1", 10)).expectNext(a).expectNext(b).verifyComplete();
    }
    @Test
    void missingSignalsError() {
        when(products.findById("nope")).thenReturn(Mono.empty());
        StepVerifier.create(service.deactivateAndConfirm("nope"))
                .expectError(ReactiveCatalogService.ProductNotFoundException.class).verify();
    }
}
```

## 10. Reactive vs Imperative Decision

| Dimension | Imperative verdict | Reactive verdict |
|---|---|---|
| < 1k rps CRUD / admin | **Wins** — plain debugger, `Page<T>` | Overkill |
| Fan-out / streaming browse, SSE | Sequential pain | **Wins** — `flatMap` concurrency, `Flux` streams |
| 10k concurrent tail latency | Threads park | **Wins** — few event loops + `take`/`window` |
| Blocking deps (JPA/JDBC/old SDK) | **Wins** | Cannot fix a blocking hop (below) |
| Team new to Reactor | **Wins** | Wait until `StepVerifier` + backpressure fluent |
| Mixed Mongo + Postgres | **Wins** | Only with R2DBC; never blocking JPA in a `Mono` |

**Why a reactive repo alone changes nothing:** WebFlux is non-blocking only if **every operator** is. One blocking hop parks the shared event-loop thread. `publishOn`/`subscribeOn(boundedElastic)` only moves blocking elsewhere — bounds damage, never restores backpressure.
```java
products.findById(id).map(p -> jdbcTemplate.queryForObject("SELECT …", String.class)); // blocking JDBC — worst offender
products.findById(id).map(p -> { try { Thread.sleep(50); } catch (Exception e) {} return p; }); // sleep
products.findById(id).block(); // .block() / .toFuture().get() inside WebFlux
```
Ladder: (1) whole path reactive (driver + WebClient + R2DBC); (2) isolate one unavoidable blocker at the edge via `Mono.fromCallable(…).subscribeOn(boundedElastic())` + timeout as tech debt; (3) otherwise stay on MVC + imperative repos.

## 11. Bean Validation on DTOs

Fail 400 at the HTTP edge before Mongo. Needs `spring-boot-starter-validation` (Boot 4.x does not pull it transitively). `@Valid` cascades into nested `List<@Valid …>`; `@Validated` covers `@RequestParam`/`@PathVariable`. Never replaces §12 — shell/jobs bypass HTTP.
```java
package com.commercemesh.catalog.web;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.util.List;
public record CreateProductRequest(
        @NotBlank(message = "sku is required") String sku,
        @NotBlank @Size(max = 200) String name, @NotBlank String brand, @NotNull String categoryId,
        @NotNull @DecimalMin(value = "0.01", message = "price must be >= 0.01") BigDecimal price,
        @Size(max = 20) List<String> tags) {}
```
```java
package com.commercemesh.catalog.web;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/products")
public class ProductController {
    private final CatalogFacade facade;
    public ProductController(CatalogFacade facade) { this.facade = facade; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) // @Valid -> MethodArgumentNotValidException -> 400 (§13)
    public ProductView create(@Valid @RequestBody CreateProductRequest request) { return facade.create(request); }
    public interface CatalogFacade { ProductView create(CreateProductRequest r); }
    public record ProductView(String id, String sku, String name) {}
}
```

## 12. MongoDB Schema Validation

`$jsonSchema` enforced on **every writer**. `strict` rejects, `moderate` checks inserts + updates to valid docs; `error` rejects, `warn` logs (use for rollout: `warn` → fix violators → `error`). Keep §11 limits and this schema in sync or you get 400-here/422-there drift (§19).
```javascript
// mongosh — version in migrations, run once per environment:
db.runCommand({ collMod: "products",
  validator: { $jsonSchema: { bsonType: "object", required: ["sku", "name", "brand", "price", "isActive"],
    properties: { sku: { bsonType: "string", minLength: 1 }, name: { bsonType: "string", minLength: 1, maxLength: 200 },
      brand: { bsonType: "string", minLength: 1 }, price: { bsonType: "decimal", minimum: 0 },
      isActive: { bsonType: "bool" }, tags: { bsonType: "array", maxItems: 20, items: { bsonType: "string" } } } } },
  validationLevel: "strict", validationAction: "error" });
db.products.createIndex({ sku: 1 }, { unique: true });
```

## 13. Error Catalog and HTTP Mapping

One `@RestControllerAdvice`, most specific first. `DuplicateKeyException → 409 (no retry)` · validation → `400` · mapping/conversion → `422` · connectivity/timeout incl. `QueryTimeoutException` → `503 + retry` (§7). Schema-validator rejection (`MongoWriteException` code 121) → 422. Log full driver messages server-side; return safe summaries.
```java
package com.commercemesh.catalog.web;
import jakarta.validation.ConstraintViolationException; import java.time.Instant;
import java.util.Map; import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException; import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.mapping.MappingException; import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler; import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class CatalogErrorAdvice {
    @ExceptionHandler(DuplicateKeyException.class) // unique sku (§12): never retry
    public ResponseEntity<Map<String, Object>> duplicate(DuplicateKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "SKU_ALREADY_EXISTS",
                "message", "sku exists; retry with a different sku", "at", Instant.now().toString()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException ex) {
        var fields = ex.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(
                f -> f.getField(), f -> String.valueOf(f.getDefaultMessage()), (a, b) -> a + "; " + b));
        return ResponseEntity.badRequest().body(Map.of("error", "VALIDATION_FAILED", "fields", fields));
    }
    @ExceptionHandler(MappingException.class) // Decimal128-vs-Double, bad ObjectId: fix data/converter
    public ResponseEntity<Map<String, Object>> unmappable(MappingException ex) {
        return ResponseEntity.unprocessableContent().body(Map.of("error", "DOCUMENT_MAPPING_FAILED",
                "message", String.valueOf(ex.getMessage())));
    }
    @ExceptionHandler({ QueryTimeoutException.class, com.mongodb.MongoTimeoutException.class,
            com.mongodb.MongoSocketException.class }) // transient -> 503, client retries with backoff (§7)
    public ResponseEntity<Map<String, Object>> unavailable(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "CATALOG_UNAVAILABLE",
                "message", "transient store error; retry with backoff", "detail", String.valueOf(ex.getMessage())));
    }
}
```

## 14. Unit Tests

Mockito only, no Spring. Stub the repository **port**; assert service branches. Never query semantics here (§18); keep each under ~100 ms.
```java
package com.commercemesh.catalog.service;
import com.commercemesh.catalog.document.Product; import com.commercemesh.catalog.repository.ProductRepository;
import java.util.List; import java.util.Optional;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks; import org.mockito.Mock; import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository products; @InjectMocks ProductService service;
    @Test
    void featuredOnly() {
        Product on = new Product(); on.setActive(true); on.setFeatured(true);
        when(products.findByIsFeaturedTrueAndIsActiveTrue()).thenReturn(List.of(on));
        assertThat(service.featured()).containsExactly(on);
    }
    @Test
    void missingSkuThrows() {
        when(products.findBySku("GHOST-1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireBySku("GHOST-1"))
                .isInstanceOf(ProductService.ProductMissingException.class);
    }
    @Test
    void saveDelegates() {
        Product draft = new Product(); draft.setSku("TRAIL-1");
        service.create(draft); verify(products).save(draft);
    }
}
```

## 15. @DataMongoTest Slice

Mongo + repositories + `MongoTemplate` only (no web/security). Real Mongo via `@ServiceConnection` Testcontainers.
```java
package com.commercemesh.catalog.repository;
import com.commercemesh.catalog.document.Product;
import java.math.BigDecimal; import java.util.List;
import org.junit.jupiter.api.AfterEach; import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest; import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container; import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
@DataMongoTest @Testcontainers(disabledWithoutDocker = true)
class ProductRepositorySliceTest {
    @Container @ServiceConnection static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");
    @Autowired ProductRepository products; @Autowired MongoTemplate mongoTemplate;
    @AfterEach void clean() { mongoTemplate.getDb().drop(); } // isolation per test (§18)
    @Test
    void derivedQuery() {
        save("TRAIL-1", "c-trail", true); save("TENT-5", "c-camp", true);
        assertThat(products.findByCategoryIdAndIsActiveTrue("c-trail"))
                .extracting(Product::getSku).containsExactly("TRAIL-1");
    }
    @Test
    void pagination() {
        for (int i = 0; i < 5; i++) save("SKU-" + i, "c-trail", true);
        var page = products.findByIsActiveTrue(PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2); assertThat(page.getTotalElements()).isEqualTo(5);
    }
    private Product save(String sku, String categoryId, boolean active) {
        Product p = new Product(); p.setSku(sku); p.setName(sku + " boot");
        p.setBrand("auroraworks"); p.setCategoryId(categoryId);
        p.setPrice(new BigDecimal("120.00")); p.setTags(List.of("trail")); p.setActive(active);
        return products.save(p);
    }
}
```

## 16. Testcontainers IT

Full `@SpringBootTest` against `mongo:8`. `@MockitoBean` (Boot 3.4+, replaces `@MockBean`) stubs **external** collaborators only — Mongo stays real (§18); see also [Testing with Spring Boot](../spring-testing/001-testing-with-spring-boot.md).
```java
package com.commercemesh.catalog;
import com.commercemesh.catalog.document.Product; import com.commercemesh.catalog.repository.ProductRepository;
import java.math.BigDecimal; import java.util.List;
import org.junit.jupiter.api.AfterEach; import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container; import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat; import static org.mockito.Mockito.when;
@SpringBootTest @Testcontainers(disabledWithoutDocker = true)
class ProductMongoIT {
    @Container @ServiceConnection static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");
    @Autowired ProductRepository products; @Autowired MongoTemplate mongoTemplate;
    @MockitoBean PricingClient pricing; // external boundary; Mongo stays REAL
    @AfterEach void clean() { mongoTemplate.getDb().drop(); }
    @Test
    void savesThroughConverters() {
        when(pricing.currentFactor()).thenReturn(BigDecimal.ONE);
        Product p = base("TRAIL-1"); p.setPrice(new BigDecimal("120.00").multiply(pricing.currentFactor()));
        Product saved = products.save(p);
        assertThat(mongoTemplate.findById(saved.getId(), Product.class).getPrice()).isEqualByComparingTo("120.00");
    }
    @Test
    void uniqueSkuIndexExists() {
        products.save(base("TRAIL-1"));
        assertThat(mongoTemplate.indexOps(Product.class).getIndexInfo()).anySatisfy(
                i -> assertThat(String.valueOf(i.getIndexFields())).contains("sku"));
    }
    private Product base(String sku) {
        Product p = new Product(); p.setSku(sku); p.setName(sku);
        p.setBrand("auroraworks"); p.setCategoryId("c-trail");
        p.setPrice(new BigDecimal("10.00")); p.setTags(List.of("trail")); p.setActive(true); return p;
    }
    public interface PricingClient { BigDecimal currentFactor(); }
}
```

## 17. Index, Aggregation, TX, Pagination, Serialization

One focused assertion per risk — silent-green with mocks, loud-red on real Mongo (fixtures from §15/§16; shapes from [007](./007-aggregation-framework.md)). Reactive variants: `StepVerifier.create(products.findBySku("TRAIL-1")).expectNextCount(1).verifyComplete()`; rollback via `StepVerifier.create(txMono).expectError(…).verify()`.
```java
@Test // UNIQUE — duplicate sku throws (not a service if-branch) -> 409 via §13
void duplicateSkuThrows() {
    products.save(base("TRAIL-1"));
    assertThatThrownBy(() -> products.save(base("TRAIL-1"))).isInstanceOf(DuplicateKeyException.class);
}
@Test // AGGREGATION — group-by-category counts
void aggregationCountsByCategory() {
    products.save(base("A-1"));
    var agg = Aggregation.newAggregation(Aggregation.match(Criteria.where("isActive").is(true)),
            Aggregation.group("categoryId").count().as("count"),
            Aggregation.project("count").and("_id").as("categoryId"));
    assertThat(mongoTemplate.aggregate(agg, "products", CategoryCount.class).getMappedResults())
            .extracting(CategoryCount::categoryId).contains("c-trail");
}
// TX — dedicated replica-set container REQUIRED (standalone: "Transaction numbers are only allowed…"):
// static MongoDBContainer txMongo = new MongoDBContainer("mongo:8.0").withCommand("--replSet", "rs0");
// + rs.initiate() init, replicaSetUrl via @ServiceConnection ([009](./009-transactions-concurrency.md) §4).
@Test @Transactional // rollback doubles as isolation (§18)
void txRollsBackOnFailure() {
    products.save(base("TX-1"));
    assertThat(products.findAll()).extracting(Product::getSku).contains("TX-1");
}
@Test // PAGINATION — totals + tail page, not just page 0
void paginationTotals() {
    for (int i = 0; i < 7; i++) products.save(base("P-" + i));
    assertThat(products.findByIsActiveTrue(PageRequest.of(0, 3)).getTotalElements()).isEqualTo(7);
    assertThat(products.findByIsActiveTrue(PageRequest.of(2, 3)).getContent()).hasSize(1);
}
@Test // SERIALIZATION — money survives BSON as Decimal128, never Double
void priceIsDecimal128() {
    Product saved = products.save(base("MONEY-1"));
    Document raw = mongoTemplate.getCollection("products")
            .find(new Document("_id", new ObjectId(saved.getId()))).first();
    assertThat(raw.get("price")).isInstanceOf(Decimal128.class);
    assertThat(mongoTemplate.findById(saved.getId(), Product.class).getPrice()).isEqualByComparingTo("10.00");
}
```

## 18. Isolation and No-Mock-for-Queries

Ladder (pick one per class, never none): (1) `getDb().drop()` in `@AfterEach` — default here; (2) per-collection `remove(new Query(),…)` when index rebuilds cost; (3) `@Transactional` rollback for TX tests; (4) unique keys per test as last resort. Reactive cleanup may `.block()` in tests — test threads may block, request threads (§10) may not.
**No-mock-for-queries:** mock the port (`ProductRepository`) to test the *service* (§14); **never** mock `MongoTemplate`/`ReactiveMongoTemplate` to test the *query*. `Criteria`, `@Query`, `@Aggregation`, sort, totals, TX boundaries exist only against real BSON — a mocked template asserts your stub. Query-verifying test ⇒ §15–§17, never Mockito.

## 19. Pitfalls (GOLD)

1. **`.block()` in WebFlux chain** — parks event loop. Compose with `flatMap`; `block*` only at batch edges/test cleanup (§5, §10, §18).
2. **Blocking JDBC/JPA in `map`** — reactive repo + blocking call = blocking app with Reactor traces. Whole path reactive or stay imperative (§10).
3. **Cold `Mono` never subscribed** — TX method without subscriber: nothing runs/commits. Return chain to WebFlux / `StepVerifier` (§8–§9).
4. **`@Valid` vs `$jsonSchema` drift** — 400 vs 422, different messages. Share one limits source; cover with §17 test.
5. **`retryWhen` on business errors** — `DuplicateKeyException`/validation loops forever. Transient filter only (§7, §13).
6. **Standalone container for TX tests** — green suite, red prod. Replica-set container every TX test (§8, §17; [009](./009-transactions-concurrency.md) §4).
7. **Mocked query tests** — `when(template.find(…))` proves the mock. Real-data slice/IT for queries (§18).
8. **`collectList()` on unbounded `Flux`** — heap blowup. `take`/`window`/keyset paging (§6).
9. **Deep `skip/limit`** — O(n) tail pages. Keyset (`_id > last` + `limit`) or `$facet` ([007](./007-aggregation-framework.md) §18).
10. **Missing validation starter** — `@Valid` silently ignored (Boot 4.x). Add starter, assert one 400 test (§11).

## 20. Interview Q&A

**Q1. Why didn't `ReactiveMongoRepository` improve p99?**
A: One blocking hop (JDBC in `map`, `.block()`, `sleep`) re-blocks the event loop (§10). Prove with BlockHound/thread dumps; fix by going fully reactive (WebClient/R2DBC) or reverting to MVC + imperative repos.
**Q2. How does backpressure reach MongoDB?**
A: Demand (`request(n)`) flows Reactor → driver → cursor `batchSize`; `take(100)` cancels after 100 docs (§6). `collectList()` on unbounded streams and uncapped `flatMap` defeat it — use `take`/`window`/keyset pages, `concatMap`/`flatMapSequential(n)`.
**Q3. When does reactive `@Transactional` commit, and what breaks it?**
A: On `Mono`/`Flux` completion via `ReactiveMongoTransactionManager` session-in-`Context` (§8). No subscription → no commit; blocking I/O inside → stalled session; standalone `mongod` → `Transaction numbers…`. TX tests need a replica-set container ([009](./009-transactions-concurrency.md) §4).
**Q4. `DuplicateKeyException` on `sku` — retry? Client sees what?**
A: Never retry — the unique index worked. **409** `SKU_ALREADY_EXISTS` via §13; caller supplies a new key. Contrast transient 503s (retry `backoff`, §7) and 422 mapping errors (fix data/converter).
**Q5. Why must query/aggregation/index tests hit real Mongo?**
A: Mocks assert stubs: `Criteria` text, `$group` shape, index choice, Decimal128 mapping, commit/rollback exist only against BSON (§18). Mockito for service branches (§14), slice for queries (§15), Testcontainers IT for indexes/agg/TX/serialization (§16–§17), `StepVerifier` for reactive chains (§9).

## 21. Resources

**This track (read in order):**
- [003 — Repository Query Methods](./003-repository-query-methods.md) — derived names, `@Query`, `@Aggregation`, pagination behind §3–§5.
- [007 — Aggregation Framework](./007-aggregation-framework.md) — stages/expressions behind §4 and the §17 aggregation test.
- [009 — Transactions & Concurrency](./009-transactions-concurrency.md) — replica sets, TX retry labels, idempotency behind §8 and §17.
- [MongoDB Introduction](./mongodb.md) — BSON/Decimal128, replica sets, indexes behind §12–§13, §17.
- [Testing with Spring Boot](../spring-testing/001-testing-with-spring-boot.md) — pyramid, slices, `@ServiceConnection`, `@MockitoBean` behind §14–§16.
**Official docs:** [Spring Data MongoDB Reactive](https://docs.spring.io/spring-data-mongodb/reference/mongodb/reactive.html) · [Reactor](https://projectreactor.io/docs/core/release/reference/) · [StepVerifier](https://projectreactor.io/docs/test/release/api/reactor/test/StepVerifier.html) · [Retry](https://projectreactor.io/docs/core/release/api/reactor/util/retry/Retry.html) · [Schema Validation](https://www.mongodb.com/docs/manual/core/schema-validation/) · [Bean Validation](https://beanvalidation.org/) · [Testcontainers MongoDB](https://java.testcontainers.org/modules/databases/mongodb/) · [Spring Boot Testing](https://docs.spring.io/spring-boot/reference/testing/index.html)

---
*Next: reactive change-stream tailing (Flux + resume tokens) — then [009](./009-transactions-concurrency.md) §16 before putting TX on any hot path.*

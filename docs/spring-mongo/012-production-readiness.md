# 012 — Production Readiness (Boot 4.1.0 + Java 25, MongoDB 8)

> Shipping `product-catalog-service` and `order-service` to prod is a capacity + observability + failure-mode decision — pool math, slow-log budgets, index rent, 16 MB guards, bulk discipline, lag guards, cache/disk headroom, timeouts/retries, breaker + degrade, backups, and TLS/RBAC — all proved under load before the incident.
>
> Companion guides: [008 — Indexing & Query Performance](./008-indexing-query-performance.md) · [010 — Replication & Sharding Advanced](./010-replication-sharding-advanced.md) · [013 — Spring Actuator](../spring-introduction/013-spring-actuator.md) · [MongoDB Introduction](./mongodb.md).

## Table of Contents

- [1. Readiness Snapshot + SLOs](#1-readiness-snapshot--slos)
- [2. Connection Pool Sizing (min/max, Queue, RPS × Latency)](#2-connection-pool-sizing-minmax-queue-rps--latency)
- [3. Timeouts + Retries (serverSelection, Socket, retryWrites/Reads)](#3-timeouts--retries-serverselection-socket-retrywritesreads)
- [4. Query Latency + Load (Slow Threshold, Profiler)](#4-query-latency--load-slow-threshold-profiler)
- [5. Index Overhead (Write Cost, $indexStats Cadence)](#5-index-overhead-write-cost-indexstats-cadence)
- [6. Large Documents (16 MB Guard, Projection, GridFS Overflow)](#6-large-documents-16-mb-guard-projection-gridfs-overflow)
- [7. N+1-Style App Patterns (@DBRef Loop → Single $in)](#7-n1-style-app-patterns-dbref-loop--single-in)
- [8. Aggregation Efficiency (Early $match, allowDiskUse Governance)](#8-aggregation-efficiency-early-match-allowdiskuse-governance)
- [9. Bulk Operations](#9-bulk-operations)
- [10. Read/Write Contention (ReadPreference, WriteConcern)](#10-readwrite-contention-readpreference-writeconcern)
- [11. Replication Lag (Monitoring + Stale-Read Guards)](#11-replication-lag-monitoring--stale-read-guards)
- [12. Hot Documents + Hot Shards](#12-hot-documents--hot-shards)
- [13. WiredTiger Cache Pressure (50%-RAM Rule, Evictions)](#13-wiredtiger-cache-pressure-50-ram-rule-evictions)
- [14. Disk + Storage Headroom](#14-disk--storage-headroom)
- [15. Outage Handling (Breaker + Degrade + Cached Fallback)](#15-outage-handling-breaker--degrade--cached-fallback)
- [16. Backups + Restore Drills](#16-backups--restore-drills)
- [17. Monitoring (Actuator, Micrometer Timer, Logs, OTel)](#17-monitoring-actuator-micrometer-timer-logs-otel)
- [18. TLS / RBAC / Secrets / K8s (Stateful vs Atlas, Probes, Limits)](#18-tls--rbac--secrets--k8s-stateful-vs-atlas-probes-limits)
- [19. Responsibility Split: App Tuning vs DBA](#19-responsibility-split-app-tuning-vs-dba)
- [20. Pitfalls (GOLD)](#20-pitfalls-gold)
- [21. Interview Q&A](#21-interview-qa)
- [22. Resources](#22-resources)

> Boot `4.1.0`, Java `25`, `spring-boot-starter-data-mongodb` (Spring Data MongoDB 5.x), MongoDB `8.0`. Domain `com.commercemesh.catalog.document` (`products`) + `com.commercemesh.order.document` (`orders`). Snippets mirror repo style, **not compiled**.

---

## 1. Readiness Snapshot + SLOs

**Purpose:** define "ready" before traffic. Every subsection below maps to a gate in this table — no gate, no launch.

| Gate | SLO / threshold | Proved by |
|---|---|---|
| Hot-path p95 | `GET /products/{sku} < 40ms`, `POST /orders < 120ms` | k6 + Atlas slow log (§4, §17) |
| Error budget | `< 0.5%` 5xx on catalog, `< 0.1%` failed checkouts | Breaker + fallback (§15) |
| Pool saturation | Wait-queue `0`, checkout `< 5ms` p99 | Metrics (§2, §17) |
| Replication lag | `< 2s` p99, stale-read guard on | `replSetGetStatus` (§11) |
| Cache health | WiredTiger evictions `< 5k/s`, dirty `< 10%` | `serverStatus` (§13) |
| Disk headroom | `< 70%` used, oplog window `> 24h` | Alerts (§14) |
| Backup | Daily snapshot + PITR, restore drill `/ quarter` | Drill log (§16) |
| Security | TLS on, least-privilege roles, rotated secrets | Audit (§18) |

```java
// Reference shapes used through this guide (fields trimmed for brevity)
@Document(collection = "products") public class Product {
    @MongoId private String id; private String sku; private String name;
    private BigDecimal price; private String categoryId; private boolean isActive = true;
}
@Document(collection = "orders") public class Order {
    @MongoId private String id; private String customerId; private String status;
    private BigDecimal total; private Instant createdAt;
}
```

Rule: load-test the *writes* too — reads pass, writes page the DBA at 3 AM. Start from §2–§4, ship with §17–§18.

---

## 2. Connection Pool Sizing (min/max, Queue, RPS × Latency)

**Purpose:** one `MongoClient` per app, pool per replica-set member. Too small queues; too large stampedes the server.

**Formula (Little's Law):** `connections ≈ peak_RPS × avg_latency_s × headroom`. Example: catalog `800 RPS × 0.025s = 20` concurrent → set `maxPoolSize=50` (≈2.5×) per pod, `minPoolSize=10` warm. Order writes `200 RPS × 0.080s = 16` → `maxPoolSize=40`. Multiply by pods: 6 pods × 50 = 300 server-side connections — check `maxIncomingConnections` (default `65536`, Atlas M30 ≈ `3000`).

```yaml
# application-prod.yml — Spring Boot 4.1 property style
spring:
  data:
    mongodb:
      uri: mongodb+srv://catalog-svc:${MONGO_PW}@prod-8x.mongodb.net/commerce?retryWrites=true&w=majority
      # Driver knobs below mirror MongoClientSettings; prefer code (§below) for typing
```

```java
// MongoConfig — explicit pool + queue governance (mirrors repo style, not compiled)
@Configuration public class MongoConfig {
    @Bean MongoClientSettingsBuilderCustomizer poolCustomizer() {
        return builder -> builder.applyToConnectionPoolSettings(pool ->
            pool.minSize(10)              // warm, avoids cold-start handshake storm
                .maxSize(50)             // Little's Law + headroom; NOT 500
                .maxWaitTime(5, TimeUnit.SECONDS)   // queue bound: fail fast → breaker (§15)
                .maxConnectionIdleTime(60, TimeUnit.SECONDS)
                .maxConnectionLifeTime(30, TimeUnit.MINUTES));
    }
}
```

**Queue rule:** `maxWaitTime` 2–5s. A growing `waitQueueSize` means pool too small *or* DB too slow — never "fix" by raising `maxSize` to 500 first; check slow log (§4) and cache (§13). Export `mongodb.driver.pool.waitqueuesize` + `checkout.time` via Micrometer (§17). `minSize == maxSize` only for ultra-stable latency; otherwise keep `min < max` so idle sockets close.

---

## 3. Timeouts + Retries (serverSelection, Socket, retryWrites/Reads)

**Purpose:** bound every hop so one sick node can't freeze all pods.

```java
@Bean MongoClientSettingsBuilderCustomizer timeoutCustomizer() {
    return builder -> builder
        .applyToClusterSettings(c -> c.serverSelectionTimeout(5, TimeUnit.SECONDS))
        .applyToSocketSettings(s -> s
            .connectTimeout(5, TimeUnit.SECONDS)  // TCP + TLS handshake
            .readTimeout(20, TimeUnit.SECONDS)   // socketTimeout: one slow op max
            .writeTimeout(10, TimeUnit.SECONDS))
        .retryWrites(true)   // idempotent insert/update with txn id; needs replica set
        .retryReads(true);   // safe re-route on network blip
}
```

| Knob | Prod value | What it saves |
|---|---|---|
| `serverSelectionTimeoutMS` | `5000` | No healthy node → fail in 5s, breaker opens (§15), not 30s hang |
| `connectTimeoutMS` | `5000` | TLS storm / SG misroute surfaces fast |
| `socketTimeoutMS` (`readTimeout`) | `20000` | One 90s `COLLSCAN` can't pin a thread forever |
| `maxWaitTimeMS` | `2000–5000` | Pool exhaustion → fast 503 + fallback |
| `retryWrites / retryReads` | `true` | Transient failover retried once by driver |
| App `timeoutMs` (find/aggregate option) | `5000–15000` | Per-op ceiling tighter than socket |

**Retry governance:** driver retries *once* on retryable errors only. App-level retry: max 2 with jittered backoff (`100ms → 400ms`), retry on `MongoSocketException` / `MongoNotPrimaryException`, **never** blind-retry non-idempotent `insert` without an idempotency key (→ `E11000` + 409 pattern, [008](./008-indexing-query-performance.md) §6). Writes use `w: majority` + `wtimeout: 5000`; analytics reads may use `w: 1` / secondary (§10).

---

## 4. Query Latency + Load (Slow Threshold, Profiler)

**Purpose:** budget per query, alert on breach. p95 without a slow log is fiction.

```javascript
// mongosh — prod slow threshold + sampled profiler (no always-on level 2)
db.setProfilingLevel(1, { slowms: 100, sampleRate: 0.1 }); // log ops >100ms, 10% sample
db.system.profile.find({ millis: { $gte: 200 } }).sort({ ts: -1 }).limit(5);
db.adminCommand({ getParameter: 1, slowOpThresholdMs: 1 });
db.setParameter({ slowOpThresholdMs: 100 }); // MongoDB 8 slow-log threshold
```

| Tier | Threshold | Action |
|---|---|---|
| Hot read (`find by sku/_id`) | `> 20ms` investigate | `explain("executionStats")`, [008](./008-indexing-query-performance.md) §21 |
| Standard read / paged list | `> 100ms` slow | Slow-log alert → index or keyset fix |
| Write / txn | `> 200ms` slow | Check writeConcern + contention (§10) |
| Aggregation | `> 1000ms` slow | Early `$match`, `allowDiskUse` review (§8) |

Spring side: time every command with a `MongoCommandListener` (§17) and tag `collection/command/success`. Load-test with realistic mix (80/15/5 read/write/agg) — a read-only test hides write lock + index rent. Profiler level 2 (`all ops`) only in staging or for minutes in prod; it writes to `system.profile` (a capped collection) and costs write throughput.

---

## 5. Index Overhead (Write Cost, $indexStats Cadence)

**Purpose:** every index is a write amplifier. Prod rule: index the hot path, audit the rest quarterly.

Each document insert/update/delete pays **one B-tree write per index that covers a changed field** — 12 indexes ≈ 12× write I/O before replication. Wide multikey/text/wildcard keys inflate storage + WiredTiger cache (§13). Covered queries ([008](./008-indexing-query-performance.md) §14) are the only "free lunch".

```javascript
// Quarterly index audit (put in DBA runbook)
db.products.aggregate([{ $indexStats: {} },
  { $project: { name: 1, accesses: "$accesses.ops", since: "$accesses.since" } },
  { $sort: { accesses: 1 } }]);
db.orders.aggregate([{ $indexStats: {} }]);
// Drop candidate: accesses == 0 since last quarter AND not _id_ / unique constraint.
// db.products.dropIndex("stale_field_1");
```

**Cadence:** `$indexStats` review **monthly** for high-churn services, **quarterly** minimum; after every launch (new query → new ESR compound or reuse). Alert if index count `> ~10` per collection or index size `> 30%` of data size. Build big indexes with `background` rolling builds on replica sets (foreground blocks writes pre-8.0 semantics; on Atlas use Rolling Index Build / `maxIndexBuildMemoryUsageMB`). Cross-link: full taxonomy in [008](./008-indexing-query-performance.md).

---

## 6. Large Documents (16 MB Guard, Projection, GridFS Overflow)

**Purpose:** the 16 MB BSON hard limit is a cliff — design so no legit write can approach it.

| Guard | How |
|---|---|
| Schema cap | Bound arrays: keep last-N reviews/line-items in-doc, spill history to child collection |
| Write-time check | Validate `BSON size < 2 MB` soft limit in service; reject/overflow above |
| Read-time projection | Always `fields(include/exclude)` — never `findAll()` full docs to list pages |
| Binary overflow | Images/PDFs/ML blobs → GridFS (`fs.files` + `fs.chunks` 255 KB) or S3 + store URL |

```java
// Projection: list page fetches 4 fields, not 40 (mirrors repo style)
Query q = new Query(Criteria.where("isActive").is(true)).limit(20);
q.fields().include("sku", "name", "price", "categoryId");
List<Product> page = mongoTemplate.find(q, Product.class);

// Soft guard before save
Document raw = new Document(mongoTemplate.getConverter().convertToMongoType(product));
if (raw.toJson().length() > 2_000_000) throw new DocumentTooLargeException(product.getSku());

// GridFS overflow for manuals/images
GridFSBucket bucket = GridFSBuckets.create(mongoDatabase, "catalogAssets");
ObjectId fileId = bucket.uploadFromStream("manual.pdf", inputStream);
product.setManualRef(fileId.toHexString()); // doc stores reference, not bytes
```

Monitor `avgObjSize` (`db.products.stats()`) — sustained growth `> 100 KB` average deserves a modeling review ([006](./006-data-modeling-schema-design.md) pattern). Moving a 10 MB array element on every `$push` rewrites the whole doc — hot docs (§12) and oplog explode together.

---

## 7. N+1-Style App Patterns (@DBRef Loop → Single $in)

**Purpose:** MongoDB has no server join for OLTP reads — a loop of single-key fetches is N round trips where one `$in` suffices.

```java
// ❌ BAN: unbounded + N+1 — one query for orders, then one per order's product
List<Order> orders = orderRepo.findAll();                    // unbounded!
for (Order o : orders) productRepo.findById(o.getProductId()); // N round trips

// ✅ Single $in batch (store productId, not @DBRef, for catalog reads)
List<String> skus = orders.stream().map(Order::getSku).distinct().toList();
List<Product> products = mongoTemplate.find(
    new Query(Criteria.where("sku").in(skus)), Product.class);
Map<String, Product> bySku = products.stream().collect(toMap(Product::getSku, p -> p));
```

**Bans (lint + review gate):** `findAll()` without `limit`/`Pageable` on any user path; `@DBRef(lazy=true)` inside a serialization loop (each getter = a query); per-item `save()` in import jobs (§9). Pagination rule: keyset (`createdAt + _id`) past ~1k docs, [008](./008-indexing-query-performance.md) §19. `@DBRef` only for admin/debug traversals; hot paths denormalize (`sku + name + price` snapshot into order item) per [006](./006-data-modeling-schema-design.md).

---

## 8. Aggregation Efficiency (Early $match, allowDiskUse Governance)

**Purpose:** pipelines stream — filter + project early so later stages chew 1k docs, not 2M.

```javascript
// ✅ $match + $project lead; $group/$lookup/$sort last
db.orders.aggregate([
  { $match: { createdAt: { $gte: ISODate("2026-09-01") }, status: "PAID" } },
  { $project: { customerId: 1, total: 1, createdAt: 1 } },
  { $group: { _id: "$customerId", revenue: { $sum: "$total" } } },
  { $sort: { revenue: -1 } }, { $limit: 100 }
], { allowDiskUse: false }); // OLTP default: spill = bug, fix pipeline/index instead
```

**Governance:** `allowDiskUse: true` only for offline/batch jobs with a capped `maxTimeMS` (e.g. `10 min`) and off-peak scheduling — it spills the 100 MiB in-memory stage cap to disk and masks a missing index. Every `$lookup` needs an indexed foreign field + `pipeline` pre-filter; unindexed `$lookup` is a nested-loop `COLLSCAN`. Prefer pre-aggregated counters (hourly rollup collection) over re-scanning raw orders per dashboard hit. Full stage rules: [007](./007-aggregation-framework.md).

---

## 9. Bulk Operations

**Purpose:** imports and backfills do one batched write path, not N acknowledged round trips.

```java
// ✅ Unordered bulk, 500–1000/batch, backpressure between batches
BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Product.class);
for (int i = 0; i < updates.size(); i++) {
    bulk.updateOne(new Query(Criteria.where("sku").is(updates.get(i).sku())),
        new Update().set("price", updates.get(i).price()));
    if (i % 500 == 499) { bulk.execute(); bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Product.class); }
}
bulk.execute();
```

Rules: `UNORDERED` for throughput (continues past single failures; inspect `BulkWriteException` per-item), `ORDERED` only when sequence matters. Batch `500–1000` ops or `< 5 MB`; throttle (e.g. 50ms sleep / token bucket) so backfills don't starve OLTP; run with `w: majority`, `j: true` for money, `w: 1` for rebuildable caches. Never per-doc `save()` in a 1M-row job — that is 1M RTTs + 1M index-amplified writes (§5).

---

## 10. Read/Write Contention (ReadPreference, WriteConcern)

**Purpose:** route pressure away from the primary; price every write's durability explicitly.

```java
// Reads: secondary for stale-tolerant analytics; primary for money paths
@Bean MongoClientSettingsBuilderCustomizer readPrefCustomizer() {
    return b -> b.readPreference(ReadPreference.secondaryPreferred(
        new TagSet(new Tag("workload", "analytics"))));
}
// Per-op override for checkout (never stale):
// mongoTemplate.setReadPreference(ReadPreference.primary());
// mongoTemplate.setWriteConcern(WriteConcern.MAJORITY.withWTimeout(5, TimeUnit.SECONDS));
```

| Path | ReadPreference | WriteConcern |
|---|---|---|
| Price/stock check, `placeOrder`, payment | `primary` | `majority`, `wtimeout 5000` |
| Catalog browse, search suggestions | `secondaryPreferred` | n/a (read) |
| Dashboards, nightly rollups | `secondary` + `maxStaleness 90s` | `w:1` scratch writes |
| Idempotent seed/backfill | `secondaryPreferred` | `majority`, throttled (§9) |

Transactions ([009](./009-transactions-concurrency.md)) pin to primary — keep them `< 5s`, touch few docs, never span a user think-time. Write contention signal: `writeConflicts` rising in `serverStatus` + `WT write` queue — split hot docs (§12), shrink txn scope.

---

## 11. Replication Lag (Monitoring + Stale-Read Guards)

**Purpose:** secondaries lag by design (async oplog apply) — monitor it, and never serve money from a stale node.

```javascript
// Lag check (mongosh / exporter query): optimeDate delta primary vs each secondary
rs.status().members.forEach(m =>
  print(m.name + " state=" + m.stateStr + " lag=" + (m.optimeDate ? new Date() - m.optimeDate : "n/a") + "ms"));
// Deeper: db.adminCommand({ replSetGetStatus: 1 }).members[].optimeDate
```

**Monitoring:** alert `lag > 5s warning / > 15s page`; alert `oplog window < 24h` (secondaries can't rejoin after outage without resync). Causes in order: long-running writes blocking apply, undersized secondary (same class as primary — no burstable secondaries), network saturation, huge transactions.

**Stale-read guards:** money paths `readPreference=primary` (§10); `secondaryPreferred` only with `maxStalenessSeconds=90`; after-own-write reads pass the write's `clusterTime` (causal consistency session) or re-read primary. Expose `mongodb_replication_lag_seconds` (Atlas / exporter) on the checkout dashboard — on-call sees lag next to error rate.

---

## 12. Hot Documents + Hot Shards

**Purpose:** one doc (or one shard key value) absorbing all writes serializes the cluster.

| Smell | Fix |
|---|---|
| Counter doc (`product.viewCount` `$inc` per hit) | Shard the counter: N stripe docs, sum on read; or Redis counter + periodic flush |
| Single `categoryId: "SALE"` shard key hotspot | Hashed or compound shard key (`{categoryId:1, _id:1}`), pre-split chunks |
| Growing array `$push` per event | Cap array (`$slice`) + spill to time-series/event collection |
| One tenant = 80% traffic, range-sharded by tenant | Zone/tag isolation or hashed key spreads heat |

```javascript
// Striped counter: write random stripe, read sum (no single hot doc)
db.product_counters.updateOne({ sku: "AUR-HP-001", stripe: Math.floor(Math.random() * 16) },
  { $inc: { views: 1 } }, { upsert: true });
db.product_counters.aggregate([{ $match: { sku: "AUR-HP-001" } },
  { $group: { _id: "$sku", views: { $sum: "$views" } } }]);
// Shard-key check: balancer state + chunk distribution
sh.status(); db.getSiblingDB("config").chunks.aggregate(
  [{ $group: { _id: "$shard", n: { $sum: 1 } } }]);
```

Deep sharding design: [010](./010-replication-sharding-advanced.md). Jumbo chunks + balancer-off are page-worthy — alert both.

---

## 13. WiredTiger Cache Pressure (50%-RAM Rule, Evictions)

**Purpose:** MongoDB is fast when the working set lives in cache; evictions are the cliff edge.

**50%-RAM rule:** default `wiredTigerCacheSizeGB ≈ max(0.5 × (RAM − 1 GB), 0.25 GB)`. On a 32 GB node → ~15 GB cache; leave the rest for OS page cache, connections (~1 MB stack × 300), and aggregations. Never set cache `> 60% RAM` — the OOM killer picks `mongod`.

```javascript
// Health signals (mongosh): evictions + dirty ratio
db.adminCommand({ serverStatus: 1 }).wiredTiger.cache;
// Watch: "pages evicted" rate, "tracked dirty bytes in the cache" / cache size,
// "checkpoint" duration. Alert: eviction rate > 5k/s sustained OR dirty > 10%.
```

| Signal | Meaning | Fix |
|---|---|---|
| `eviction server evicting pages` climbing | Working set > cache | Add RAM, trim indexes (§5), project less (§6) |
| Dirty `> 20%` | Checkpoint can't keep up with writes | Throttle backfills (§9), IOPS headroom (§14) |
| `cache overflow / stuck` | Pathological large docs or `$sort` spill | Kill op, fix pipeline (§8), cap doc size (§6) |

Size from production trace, not hope: cache-hit ratio `> 99%` sustained before cutting instance class.

---

## 14. Disk + Storage Headroom

**Purpose:** disk-full is a leader-election + data-loss incident, not a metric.

Rules: keep usage `< 70%` (compaction, index builds, oplog growth need room); alert `75% warn / 85% page`; oplog sized for `24–72h` window (`oplogSizeMB`, Atlas auto-scales — verify after traffic ×2 events). Separate volumes for `dbPath`, journal, and logs so one fills without killing all three. IOPS: provision for p99 write burst (bulk jobs §9 + checkpoint), not average — `iostat await > 10ms` sustained means EBS/PD class up.

```javascript
db.products.stats();           // dataSize vs storageSize vs indexSizes (compression ratio)
db.adminCommand({ getCmdLineOpts: 1 }); // dbPath, wiredTiger config audit
// Atlas: Data Size vs Storage Size divergence → fragmentation; schedule compact window.
```

Snappy (default) balances CPU/compression; zlib saves disk at CPU cost — change only with a benchmark. Expired TTL tombstones + deleted-doc holes fragment; `compact` needs rolling maintenance window on replica sets.

---

## 15. Outage Handling (Breaker + Degrade + Cached Fallback)

**Purpose:** when MongoDB is down, the app answers *something useful* in milliseconds — stale catalog beats 503 for browse; checkout fails honest-fast.

```java
// Imports sketch — resilience + cache fallback shape (not compiled)
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service public class CatalogService {
    private final MongoTemplate mongo; private final ProductCache cache;
    public CatalogService(MongoTemplate mongo, ProductCache cache) {
        this.mongo = mongo; this.cache = cache;
    }

    @Cacheable(value = "products", key = "#sku")           // Caffeine/Redis front (TTL 5 min)
    @CircuitBreaker(name = "mongo-catalog", fallbackMethod = "staleOrPlaceholder")
    public Product getBySku(String sku) {
        return mongo.findById(sku, Product.class);          // throws on pool/timeout (§2–§3)
    }

    // Same signature + Throwable tail: Resilience4j fallback contract
    private Product staleOrPlaceholder(String sku, Throwable t) {
        return cache.lastKnown(sku)                          // Redis/Caffeine stale copy
            .orElse(Product.placeholder(sku));               // honest degraded shell
    }
}
```

**Breaker tuning (Resilience4j):** `failureRateThreshold: 50%`, `slidingWindowSize: 50`, `waitDurationInOpenState: 30s`, `permittedCallsInHalfOpen: 5`; bulkhead `maxConcurrentCalls = pool.maxSize` so threads can't outnumber sockets. **Degrade ladder:** browse → stale cache; search → top-sellers static list; checkout → fail fast with idempotency key preserved (retry safe, no double-charge). Chaos-drill quarterly: kill primary, blackhole a secondary, fill disk to 85% in staging — runbook must fit on one page.

---

## 16. Backups + Restore Drills

**Purpose:** backups you never restored are rumors. RPO/RTO are product decisions, signed by the PM.

| Layer | Prod setting |
|---|---|
| Atlas continuous backup / PITR | On, `7–30d` retention; oplog-chasing point-in-time window verified |
| Self-hosted | `mongodump --oplog` (small) or filesystem snapshot + `oplog` replay (large); snapshot before index builds |
| Tenant-critical exports | Nightly `mongoexport` of `orders` to object store for legal/compliance |
| Secrets | Connection strings + TLS certs versioned with backup (restore needs them) |

**Restore drill (quarterly, timed):** spin ephemeral env → restore snapshot → replay to PITR target → run smoke (`count + checksum on orders`, boot both services, place test order) → record RTO vs target (`< 30 min` standard, `< 4h` disaster). Log the drill date + RTO in the runbook; a missed quarter pages the eng manager. Test major-version restores (7→8) in staging first — index format + featureCompatibilityVersion bite.

---

## 17. Monitoring (Actuator, Micrometer Timer, Logs, OTel)

**Purpose:** latency, saturation, errors on one dashboard; traceId from HTTP to `mongod` log line.

```yaml
# Actuator surface (detail: [013 — Spring Actuator](../spring-introduction/013-spring-actuator.md))
management:
  endpoints.web.exposure.include: health,metrics,prometheus
  endpoint.health.show-details: when_authorized
  health.mongo.enabled: true
  metrics.tags.application: product-catalog-service
```

```java
// Micrometer MongoCommandListener — per-command timer (command/collection/status tags)
import com.mongodb.event.CommandStartedEvent; import com.mongodb.event.CommandSucceededEvent;
import com.mongodb.event.CommandFailedEvent; import com.mongodb.event.CommandListener;
import io.micrometer.core.instrument.MeterRegistry; import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap; import java.util.concurrent.TimeUnit;

public class MicrometerCommandListener implements CommandListener {
    private final MeterRegistry registry;
    private final ConcurrentHashMap<Integer, Long> starts = new ConcurrentHashMap<>();
    public MicrometerCommandListener(MeterRegistry registry) { this.registry = registry; }
    @Override public void commandStarted(CommandStartedEvent e) { starts.put(e.getRequestId(), e.getTimeNanos()); }
    @Override public void commandSucceeded(CommandSucceededEvent e) {
        record(e.getCommandName(), e.getElapsedTime(TimeUnit.NANOSECONDS), "success"); }
    @Override public void commandFailed(CommandFailedEvent e) {
        record(e.getCommandName(), e.getElapsedTime(TimeUnit.NANOSECONDS), "failed"); }
    private void record(String command, long nanos, String status) {
        Timer.builder("mongodb.commands")  // collection tag parsed from command doc in real impl
            .tag("command", command).tag("status", status)
            .register(registry).record(nanos, TimeUnit.NANOSECONDS);
    }
}
// Register: builder.addCommandListener(new MicrometerCommandListener(registry)) in MongoConfig.
```

**Dashboards:** RED per collection (rate/errors/duration p50/p95/p99), pool (`size/checkout/waitqueue`), lag (§11), cache (§13), disk (§14). **Structured logs:** JSON (`traceId`, `spanId`, `collection`, `command`, `millis`, `planSummary` on slow) — MDC traceId propagated by OTel instrumentation; slow commands log `explain` plan summary. **OTel spans:** `spring-boot-starter-opentelemetry` auto-wraps `MongoTemplate` calls — alert on span error rate, sample slow spans 100%.

---

## 18. TLS / RBAC / Secrets / K8s (Stateful vs Atlas, Probes, Limits)

**Purpose:** encryption everywhere, least privilege, secrets never in git, pods that don't flap during failover.

```javascript
// Least-privilege roles (mongosh, admin db) — one per service, no atlasAdmin in apps
db.createRole({ role: "catalogSvc", privileges: [
  { resource: { db: "commerce", collection: "products" },
    actions: ["find", "insert", "update"] }], roles: [] });
db.createUser({ user: "catalog-svc", pwd: "<vault-issued>",
  roles: [{ role: "catalogSvc", db: "admin" }] });
```

| Concern | Rule |
|---|---|
| TLS | `tls=true`, X.509 or SCRAM-SHA-256; verify CA in-driver; rotate certs before expiry alert (30d) |
| RBAC | Per-service user, per-collection actions; analytics gets `find` on secondary only; no `dbAdmin` for apps |
| Secrets | Vault/ESO → env (`${MONGO_PW}`), rotation without restart (reloadable `MongoCredential`); git history scanned |
| Network | VPC peering/PrivateLink (Atlas) or NetworkPolicy (self-hosted); no `0.0.0.0/0` |

**K8s:** StatefulSet + anti-affinity + PodDisruptionBudget for self-hosted replica sets (stable network IDs); Atlas preferred when team has no DBA rotation. Probes: liveness = JVM alive, readiness = `MongoTemplate.executeCommand({ping:1})` with 2s timeout (pod leaves service during failover instead of serving 500s). Resources: `requests ≈ p95 usage`, `limits ≈ 1.5× requests` for app pods; `mongod` gets Guaranteed QoS + local SSD. HPA on app latency/RPS, never on `mongod` CPU — databases scale by plan, not autoscaler.

---

## 19. Responsibility Split: App Tuning vs DBA

**Purpose:** stop paging the wrong human. App owns query shape + resilience; DBA owns topology + durability.

| Area | App (service team) | DBA / Platform |
|---|---|---|
| Pool, timeouts, retries | Size per Little's Law, `maxWaitTime`, breaker tuning (§2, §3, §15) | `maxIncomingConnections`, node class headroom |
| Queries + indexes requested | `explain`, ESR compounds, projections, keyset pages ([008](./008-indexing-query-performance.md)) | `$indexStats` audit, rolling builds, drop approval (§5) |
| Doc shape | 16 MB guard, denormalize-vs-link, N+1 bans (§6, §7) | `avgObjSize` trend, `compact` windows (§14) |
| Aggregations | Early `$match`, `maxTimeMS`, batch `allowDiskUse` only (§8) | Kill runaway ops, secondary routing for BI |
| Writes | Bulk batches, idempotency keys, txn scope (§9, [009](./009-transactions-concurrency.md)) | `writeConcern` defaults, oplog sizing (§11) |
| Reads at scale | `readPreference` per path, stale-tolerance labels (§10) | Lag alerts, tag sets, maxStaleness enforcement |
| Hot spots | Stripe counters, shard-key choice proposal (§12) | Pre-split, balancer, zone moves ([010](./010-replication-sharding-advanced.md)) |
| Cache/disk | Reduce working set, throttle backfills | RAM class, cache %, IOPS, disk expansion (§13, §14) |
| Outage UX | Degrade ladder, cached fallback, honest errors (§15) | Failover, restore execution (§16) |
| Observability | Actuator, command timers, traceId logs, OTel (§17) | `mongod` logs, profiler, Atlas alerts |
| Security | Secret consumption, rotation handling | TLS CA, RBAC roles, audits, K8s volumes (§18) |

Joint quarterly review: slow-top-10, index audit, restore-drill RTO, capacity forecast (traffic ×2 headroom).

---

## 20. Pitfalls (GOLD)

!!! warning "GOLD — production-readiness pitfalls (read before launch)"
    - **500 maxPoolSize "fixes" queuing** — it moves the queue into `mongod`; size by RPS × latency (§2).
    - **30s serverSelection default in prod** — fail in 5s into the breaker, not into thread starvation (§3, §15).
    - **Blind app retry on inserts** — duplicates without idempotency keys; driver retries once, you retry twice max with jitter (§3).
    - **Profiler level 2 left on** — it is a write workload; sample + revert (§4).
    - **Index-per-field optimism** — each index taxes every write; `$indexStats` quarterly (§5).
    - **Unbounded `findAll()` in a controller** — page or project; loops of `findById` become one `$in` (§7).
    - **`allowDiskUse:true` as a habit** — masks missing early-`$match`; OLTP default false (§8).
    - **Per-doc `save()` backfills** — bulk 500–1000 unordered with throttle (§9).
    - **Checkout reads from secondary** — money reads primary; analytics take the staleness (§10–§11).
    - **One counter doc per flash sale** — stripe it or lose the primary (§12).
    - **Cache at 80% RAM** — OOM killer incident; 50% rule + eviction alerts (§13).
    - **Disk alert at 95%** — too late; 70/75/85 headroom ladder (§14).
    - **Breaker without fallback data** — open circuit must serve stale cache, not empty 503 (§15).
    - **Unrestored backups** — quarterly timed drill or RTO is fiction (§16).
    - **`health` without mongo ping in readiness** — pods serve 500s through failover (§17–§18).
    - **Logs without traceId** — app-to-`mongod` correlation impossible during incidents (§17).

---

## 21. Interview Q&A

**Q1. Traffic doubled and catalog p95 rose 30ms → 400ms over two weeks — walk me through the investigation.**
A: Top-down, no guessing: (1) RED dashboard — which collection/command moved (§17 timer tags); (2) slow log + `system.profile` for new shapes (§4); (3) `explain("executionStats")` — `COLLSCAN` or key/doc divergence means regressed plan or missing ESR compound ([008](./008-indexing-query-performance.md) §21); (4) `$indexStats` — did deploy add queries without indexes (§5); (5) pool checkout + waitQueue (§2) — queueing means DB slow, not pool small; (6) WiredTiger evictions + dirty ratio (§13) and disk await (§14) — working set outgrew cache; (7) replication lag (§11) if secondaries serve reads. Fix in that order — plan, index, working set — then resize pool/class with numbers.

**Q2. Primary goes down at peak — what should the app do second by second?**
A: Driver `serverSelectionTimeout 5s` fails fast (§3); in-flight money writes with `retryWrites` re-target the new primary once; breaker counts failures, opens at 50%/50 calls (§15) — browse serves stale cache/placeholder, checkout fails honest-fast preserving idempotency keys for safe retry; readiness probe (`ping`) pulls pods out of rotation until election completes; HPA does NOT flap the DB. Post-mortem: election time, rollback files, lag at failover, whether any secondary read served stale money data (§10–§11). Drill this quarterly in staging.

**Q3. Secondaries lag 30s during the nightly import — reads show yesterday's prices. Fix?**
A: Import is an unordered bulk with throttle, off-peak, `w:1` scratch or throttled `majority` (§9); price reads pin `primary` or `secondaryPreferred + maxStalenessSeconds 90` with causal session after own writes (§10–§11); alert lag `> 5s`/page `> 15s`; check secondary class parity + oplog window (§14). Long-term: split import batches, pre-split hot chunks (§12). Never "fix" by reading stale and hoping.

**Q4. One `products` doc gets 10k `$inc`s/sec during a drop — primary stalls. Redesign?**
A: Classic hot doc (§12): stripe the counter over 16 docs (random stripe write, `$group` sum on read) or buffer in Redis + flush per second; cap event arrays with `$slice`, spill to event collection (§6). If sharded, the shard key carrying the drop needs hashed/compound spread + pre-split ([010](./010-replication-sharding-advanced.md)). Verify via `writeConflicts` + WT cache dirty (§10, §13).

**Q5. WiredTiger evictions 20k/s, dirty 25%, disk 82% — what do you touch first?**
A: Stop the bleeding: kill/throttle backfills and `allowDiskUse` analytics (§8–§9); confirm working set vs cache — trim unused indexes (§5), enforce projections (§6), kill N+1 fan-out (§7); dirty 25% means checkpoints behind — IOPS headroom + smaller write batches (§13–§14); disk 82% is near-page — expand + compact window now, find growth source (`stats()`, oplog). Then capacity: RAM class up per 50%-rule before traffic ×2, alerts at 70/75/85 so this never pages again.

---

## 22. Resources

**This track:** [008 — Indexing & Query Performance](./008-indexing-query-performance.md) (ESR, `explain`, profiler, `$indexStats`) · [010 — Replication & Sharding Advanced](./010-replication-sharding-advanced.md) (elections, lag, shard keys, balancer) · [013 — Spring Actuator](../spring-introduction/013-spring-actuator.md) (health, metrics, Prometheus) · [MongoDB intro](./mongodb.md) (BSON, replica sets, sharding basics).

**Official docs:** [Connection Pool](https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/connection/) · [Timeouts + Retryable Writes](https://www.mongodb.com/docs/manual/reference/method/MongoClient/) · [Profiler + slowOpThresholdMs](https://www.mongodb.com/docs/manual/tutorial/manage-the-database-profiler/) · [$indexStats](https://www.mongodb.com/docs/manual/reference/operator/aggregation/indexStats/) · [GridFS](https://www.mongodb.com/docs/manual/core/gridfs/) · [Bulk Write](https://www.mongodb.com/docs/manual/core/bulk-write-operations/) · [Read Preference](https://www.mongodb.com/docs/manual/core/read-preference/) · [Replication Lag](https://www.mongodb.com/docs/manual/tutorial/troubleshoot-replica-sets/) · [WiredTiger Cache](https://www.mongodb.com/docs/manual/core/wiredtiger/) · [Backups (Atlas)](https://www.mongodb.com/docs/atlas/backup-restore-cluster/) · [TLS + RBAC](https://www.mongodb.com/docs/manual/security/) · [Spring Data MongoDB](https://docs.spring.io/spring-data-mongodb/reference/) · [Resilience4j CircuitBreaker](https://resilience4j.readme.io/docs/circuitbreaker) · [Micrometer + OTel](https://micrometer.io/docs) · [Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html)

---

*Next: [010 — Replication & Sharding Advanced](./010-replication-sharding-advanced.md) · observability detail: [013 — Spring Actuator](../spring-introduction/013-spring-actuator.md).*

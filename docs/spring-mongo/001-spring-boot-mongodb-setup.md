# 001 — Spring Boot MongoDB Setup (Boot 4.1.0 + Java 25, MongoDB 8, Spring Data MongoDB)

> Section A of the Spring Mongo track: connect a Boot 4.1.0 app to MongoDB 8 over
> Spring Data MongoDB, externalize config via `MONGO_*` env vars, and verify end to end
> across local, `docker run`, Compose, and Atlas topologies.
>
> Companion guides: [MongoDB Basics](./mongodb.md) ·
> [002 — Document Mapping](./002-document-mapping.md) ·
> [013 — Product Catalog Example](./013-product-catalog-example.md) ·
> [001 — Testing with Spring Boot](../spring-testing/001-testing-with-spring-boot.md).

## Table of Contents

- [1. Prerequisites](#1-prerequisites)
- [2. Dependencies — Maven and Gradle](#2-dependencies--maven-and-gradle)
- [3. Auto-Configuration](#3-auto-configuration)
- [4. Full `application.yml`](#4-full-applicationyml)
- [5. Every Property Explained](#5-every-property-explained)
- [6. Run MongoDB Locally and via `docker run`](#6-run-mongodb-locally-and-via-docker-run)
- [7. Full `docker-compose` — MongoDB + mongo-express](#7-full-docker-compose--mongodb--mongo-express)
- [8. Connect with mongosh and Compass](#8-connect-with-mongosh-and-compass)
- [9. Users and Roles](#9-users-and-roles)
- [10. Atlas SRV URI and TLS](#10-atlas-srv-uri-and-tls)
- [11. Timeouts, Pool, and Credentials Hygiene](#11-timeouts-pool-and-credentials-hygiene)
- [12. Externalized Config per Environment](#12-externalized-config-per-environment)
- [13. Verify the Connection](#13-verify-the-connection)
- [14. Topology — Standalone vs Compose vs Atlas](#14-topology--standalone-vs-compose-vs-atlas)
- [15. Troubleshooting](#15-troubleshooting)
- [16. Pitfalls (GOLD)](#16-pitfalls-gold)
- [17. Interview Q&A](#17-interview-qa)
- [18. Resources](#18-resources)

---

## 1. Prerequisites

| Requirement | Version | Check |
|---|---|---|
| Java | **25** | `java -version` (matches `java.version=25`) |
| Spring Boot | **4.1.0** | parent POM in §2; manages Spring Data MongoDB |
| MongoDB server | **8.x** | `mongosh --eval 'db.version()'` → `8.*` |
| Build tool | Maven 3.9+ or Gradle 8.10+ | `mvn -version` / `gradle --version` |
| Docker + Compose v2 | 24+ | `docker compose version` |
| Clients | `mongosh` + Compass | `mongosh --version` |

Keep ports **27017** and **8081** free. Stop the bare `mongod` before starting a
container on the same port, and vice versa (§15).

---

## 2. Dependencies — Maven and Gradle

Mirrors `projects-v2/commerce-mesh/backend/product-catalog-service/pom.xml` for the
MongoDB slice, with pinned `springdoc` 2.8.8 and Testcontainers 1.20.6:

- `spring-boot-starter-data-mongodb` — `MongoTemplate`, `MongoRepository`, driver. No version.
- `spring-boot-starter-actuator` — `mongo` health indicator (§13). No version.
- `spring-boot-starter-validation` — Jakarta Validation for `@Document` beans. No version.
- `springdoc-openapi-starter-webmvc-ui` **2.8.8** — Swagger UI.
- `org.testcontainers:mongodb` **1.20.6** (`test`) — ephemeral `mongo:8` for tests.
- `spring-boot-starter-test` (`test`) — JUnit 5 + AssertJ + Mockito. No version.

### Maven (`pom.xml`)

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
    <relativePath/>
</parent>

<properties>
    <java.version>25</java.version>
    <springdoc.version>2.8.8</springdoc.version>
    <testcontainers.version>1.20.6</testcontainers.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mongodb</artifactId>
        <version>${testcontainers.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Gradle (`build.gradle`)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.0'
    id 'io.spring.dependency-management' version '1.1.7'
}
java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8'
    testImplementation 'org.testcontainers:mongodb:1.20.6'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

```bash
./mvnw dependency:tree | grep -i -E "mongodb|spring-data-mongodb"
./gradlew dependencies --configuration runtimeClasspath | grep -i -E "mongodb"
```

---

## 3. Auto-Configuration

With the MongoDB starter present, Boot wires everything with zero `@Bean` methods:

1. **`MongoAutoConfiguration`** — builds the shared `MongoClient` (pool, server selection,
   TLS) plus the `MongoDatabaseFactory` (`SimpleMongoClientDatabaseFactory`) bound to
   `spring.data.mongodb.database`. Fires when `MongoClient` is on the classpath and no
   custom `MongoClient` bean exists.
2. **`MongoDataAutoConfiguration`** — builds `MongoTemplate`, `MappingMongoConverter` /
   `MongoMappingContext`, and a `MongoTransactionManager` on a replica set.
   `MongoRepository` interfaces are auto-enabled.
3. **Actuator contributor** — exposes the `mongo` indicator at `/actuator/health` (§13).

Rules: a custom `MongoClient`/`MongoDatabaseFactory` bean disables that slice (only for
custom codecs or multi-DB routing, see [002](./002-document-mapping.md)).
`spring.data.mongodb.uri` overrides all discrete keys — never set both (§10, §15).
No starter → no `MongoTemplate`, no `mongo` health key, inert `spring.data.mongodb.*`.

---

## 4. Full `application.yml`

Mirrors `product-catalog-service/src/main/resources/application.yml` lines 14–22
for the MongoDB block, keeping the `MONGO_*` env style:

```yaml
server:
  port: ${CATALOG_SERVICE_PORT:8083}

spring:
  threads:
    virtual:
      enabled: true
  application:
    name: product-catalog-service
  data:
    mongodb:
      host: ${MONGO_HOST:localhost}
      port: 27017
      authentication-database: admin
      username: ${MONGO_ROOT_USER:root}
      password: ${MONGO_ROOT_PASSWORD:rootroot}
      database: ${MONGO_DATABASE:commercemesh}
      # uri: ${MONGO_URI:}                  # Atlas only — overrides keys above (§10)
      # connection-pool-max-size: 50
      # connection-pool-min-size: 5
      # server-selection-timeout: 5s

management:
  endpoint:
    health:
      probes:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

All six `spring.data.mongodb.*` keys are byte-identical to `product-catalog-service`.
Pool/timeout keys stay commented locally and are enabled per §11.

---

## 5. Every Property Explained

| Property | Value | Meaning |
|---|---|---|
| `host` | `${MONGO_HOST:localhost}` | Hostname; Compose services use `MONGO_HOST: mongodb`. Wrong value → `ServerSelectionTimeout`. |
| `port` | `27017` | MongoDB default port. Kept literal; externalize only for non-standard ports. |
| `authentication-database` | `admin` | DB holding the credential (`admin` for `MONGO_INITDB_ROOT_*` users). Wrong value → `Authentication failed`. |
| `username` | `${MONGO_ROOT_USER:root}` | SCRAM user; must match `MONGO_INITDB_ROOT_USERNAME`. `root` is dev-only. |
| `password` | `${MONGO_ROOT_PASSWORD:rootroot}` | SCRAM secret; must match `MONGO_INITDB_ROOT_PASSWORD`. Dev-only. |
| `database` | `${MONGO_DATABASE:commercemesh}` | Working DB for `MongoTemplate`. Created lazily; must match `MONGO_INITDB_DATABASE`. |
| `uri` *(commented)* | `${MONGO_URI:}` | Atlas SRV string. When set, ignores all six keys above — one style only. |
| `connection-pool-min/max-size` | `5` / `50` | Pool floor/ceiling; defaults suffice locally (§11). |
| `server-selection-timeout` | `5s` | Max wait for a usable server; raise for Atlas/cross-AZ (§11). |

---

## 6. Run MongoDB Locally and via `docker run`

```bash
# macOS (MongoDB 8)
brew tap mongodb/brew
brew install mongodb-community@8.0
brew services start mongodb-community@8.0

# Ubuntu / Debian (MongoDB 8.0)
sudo apt-get install -y mongodb-org   # after adding the 8.0 repo + key
sudo systemctl start mongod
mongosh --eval 'db.runCommand({ ping: 1 })'   # { ok: 1 } — no auth on fresh bare install
```

Create the dev user from §9 afterwards (a bare install has no `root`/`rootroot` yet).
One-off container (ephemeral storage — add `-v mongo_data:/data/db` to keep data):

```bash
docker run -d --name cm-mongodb-standalone -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=root \
  -e MONGO_INITDB_ROOT_PASSWORD=rootroot \
  -e MONGO_INITDB_DATABASE=commercemesh \
  mongo:8
echo 'db.runCommand("ping").ok' | mongosh localhost:27017/test --quiet   # 1
MONGO_HOST=localhost MONGO_ROOT_USER=root MONGO_ROOT_PASSWORD=rootroot \
  MONGO_DATABASE=commercemesh ./mvnw spring-boot:run
docker stop cm-mongodb-standalone && docker rm cm-mongodb-standalone
```

---

## 7. Full `docker-compose` — MongoDB + mongo-express

Mirrors `mongodb` + `mongo-express` from `projects-v2/commerce-mesh/docker-compose.yaml`
lines 40–73. Save as `docker-compose.mongo.yml`:

```yaml
services:
  mongodb:
    image: mongo:8
    container_name: cm-mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_ROOT_USER:-root}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_ROOT_PASSWORD:-rootroot}
      MONGO_INITDB_DATABASE: ${MONGO_DATABASE:-commercemesh}
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db
      - ./scripts/init-mongo.js:/docker-entrypoint-initdb.d/init.js
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh localhost:27017/test --quiet
      interval: 5s
      timeout: 3s
      retries: 5
    networks:
      - cm-network

  mongo-express:
    image: mongo-express:latest
    container_name: cm-mongo-express
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: ${MONGO_ROOT_USER:-root}
      ME_CONFIG_MONGODB_ADMINPASSWORD: ${MONGO_ROOT_PASSWORD:-rootroot}
      ME_CONFIG_MONGODB_URL: mongodb://${MONGO_ROOT_USER:-root}:${MONGO_ROOT_PASSWORD:-rootroot}@mongodb:27017/
    ports:
      - "8081:8081"
    depends_on:
      mongodb:
        condition: service_healthy
    networks:
      - cm-network

networks:
  cm-network:
    driver: bridge

volumes:
  mongo_data:
```

```bash
MONGO_ROOT_USER=root MONGO_ROOT_PASSWORD=rootroot MONGO_DATABASE=commercemesh \
  docker compose -f docker-compose.mongo.yml up -d
MONGO_HOST=localhost MONGO_ROOT_USER=root MONGO_ROOT_PASSWORD=rootroot \
  MONGO_DATABASE=commercemesh ./mvnw spring-boot:run
# from INSIDE another compose service: MONGO_HOST=mongodb
```

`mongo_data:/data/db` survives `down` (only `down -v` wipes it). `init-mongo.js` seeds
`products`, `categories`, `brands`, `reviews`, `carts`, `wishlists` plus indexes on first volume creation only. mongo-express at `http://localhost:8081` is an inspector only.

---

## 8. Connect with mongosh and Compass

```bash
mongosh "mongodb://root:rootroot@localhost:27017/commercemesh?authSource=admin"
# inside the shell:
show dbs
use commercemesh
show collections
db.products.findOne()
db.runCommand({ ping: 1 })   # { ok: 1 }
```

Compass: paste `mongodb://root:rootroot@localhost:27017/commercemesh?authSource=admin`
into the connection dialog. Use Compass to browse documents, prototype aggregation
pipelines, and check indexes; use `mongosh` for anything scriptable. Concepts live in
[MongoDB Basics](./mongodb.md).

---

## 9. Users and Roles

The Compose root user is a dev-only superuser. Create scoped app users with `db.createUser`:

```javascript
use commercemesh
db.createUser({
  user: "catalog_app",
  pwd: "REPLACE-WITH-VAULT-SECRET",
  roles: [{ role: "readWrite", db: "commercemesh" }]
})
db.createUser({
  user: "catalog_reader",
  pwd: "REPLACE-WITH-VAULT-SECRET",
  roles: [{ role: "read", db: "commercemesh" }]
})
db.getUsers()
db.auth("catalog_app", "REPLACE-WITH-VAULT-SECRET")   // 1
```

Point the app at `catalog_app` (`MONGO_ROOT_USER=catalog_app`) from staging on.
Root users authenticate against `admin`; users created via `use commercemesh`
authenticate against `commercemesh` — set `authentication-database` accordingly (§15).

---

## 10. Atlas SRV URI and TLS

For Atlas, switch from discrete keys to one SRV URI — `uri` ignores the six discrete keys:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGO_URI:mongodb+srv://catalog_app:<password>@cluster0.abc12.mongodb.net/commercemesh?retryWrites=true&w=majority}
      database: ${MONGO_DATABASE:commercemesh}
```

```bash
mongosh "mongodb+srv://catalog_app:<password>@cluster0.abc12.mongodb.net/commercemesh"
# Compass: paste the same mongodb+srv:// string
```

`mongodb+srv://` negotiates TLS by default — never append `&tls=false` to "fix" a
handshake. `PKIX path building failed` means the CA is missing from the JVM truststore.
Allowlist app egress (or VPC-peer Atlas) and keep `retryWrites=true&w=majority`.

---

## 11. Timeouts, Pool, and Credentials Hygiene

Defaults suit localhost; tune for Atlas, cross-AZ, or high concurrency:

```yaml
spring:
  data:
    mongodb:
      connection-pool-min-size: 5      # warm idle connections
      connection-pool-max-size: 50     # per-instance ceiling; raise on load-test evidence
      server-selection-timeout: 5s     # 10–30s for Atlas/cross-region
```

Exhaustion looks like climbing latency then `ServerSelectionTimeout` under load — scale the pool with replica count, not instead of it.

Hygiene: never hardcode secrets in YAML, Java, or Compose — everything flows from `MONGO_*` env vars. Prod uses bare `${MONGO_ROOT_PASSWORD}` / `${MONGO_URI}` (no default) to fail fast, injected from Secrets Manager / Vault / K8s Secrets and rotated with the server. Services get `readWrite` on one DB (§9), never `root`. Keep `27017` off the public edge beyond dev.

---

## 12. Externalized Config per Environment

Same artifact everywhere; only env changes (`MONGO_HOST: mongodb` in Compose).

| Env | Host / URI | Credentials | DB / Pool | TLS |
|---|---|---|---|---|
| **dev** | `localhost` | `root` / `rootroot` defaults | `commercemesh`, defaults | off |
| **test** | ephemeral container port | test-only secret | throwaway DB | off |
| **staging** | `staging-mongo.internal` | vault value | `commercemesh_staging`, §11 pool | on if managed |
| **prod** | `MONGO_URI` (Atlas SRV) | secret manager, rotated | `commercemesh`, load-tested pool | **on** |

```bash
export MONGO_HOST=staging-mongo.internal MONGO_ROOT_USER='catalog_app' MONGO_ROOT_PASSWORD='s3cr3t-from-vault'
export MONGO_DATABASE=commercemesh_staging SPRING_DATA_MONGODB_SERVER_SELECTION_TIMEOUT=10s
# prod: export MONGO_URI='mongodb+srv://catalog_app:s3cr3t@cluster0.abc12.mongodb.net/commercemesh?retryWrites=true&w=majority'
```

Tests use Testcontainers `mongodb:1.20.6` + `@ServiceConnection` (`mongo:8` image);
see [testing guide](../spring-testing/001-testing-with-spring-boot.md).

---

## 13. Verify the Connection

Run all three — together they are conclusive.

**Check 1 — `mongosh ping`.** Server reachable + auth works:

```bash
echo 'db.runCommand("ping").ok' | mongosh localhost:27017/test --quiet   # 1
mongosh "mongodb://root:rootroot@localhost:27017/commercemesh?authSource=admin" \
  --eval 'db.runCommand({ ping: 1 })'   # { ok: 1 }
```

**Check 2 — Actuator `mongo` indicator.** Boot sees it too:

```bash
curl -s http://localhost:8083/actuator/health | python3 -m json.tool
# { "status": "UP", "components": { "mongo": { "status": "UP", ... } } }
```

**Check 3 — `MongoTemplate.getDb()` probe.** Right DB, read-write:

```java
@Component
public class MongoProbe implements CommandLineRunner {
    private final MongoTemplate mongoTemplate;
    public MongoProbe(MongoTemplate mongoTemplate) { this.mongoTemplate = mongoTemplate; }
    @Override
    public void run(String... args) {
        System.out.println("MONGO_DB=" + mongoTemplate.getDb().getName());
        System.out.println("PING=" + mongoTemplate.getDb().runCommand(new Document("ping", 1)));
        System.out.println("PRODUCTS=" + mongoTemplate.getCollection("products").estimatedDocumentCount());
    }
}
```

Expect `MONGO_DB=commercemesh`, `PING={"ok"=1.0}`, `PRODUCTS>=0` (seeded under Compose).
Wrong DB → `MONGO_DATABASE` mismatch; auth error → §9 roles / `authentication-database`.

---

## 14. Topology — Standalone vs Compose vs Atlas

Same app and YAML; only `MONGO_HOST` / `MONGO_URI` and secret injection change.

```mermaid
flowchart LR
    SVC["Catalog service<br/>MongoTemplate + MongoRepository"] --> FAC["MongoDatabaseFactory<br/>host / authSource / user / db"] --> DRV["Java driver<br/>pool + selection + TLS"]
    DRV -- "wire :27017 / SCRAM" --> LOC["Standalone<br/>localhost:27017"]
    DRV -- "wire :27017 / SCRAM" --> CMP["Compose mongo:8 :27017<br/>mongo_data + :8081 express"]
    DRV -- "mongodb+srv / TLS" --> ATL["Atlas cluster<br/>TLS + retryWrites"]
```

Local: app → driver → `localhost:27017`. Compose: app → driver → `cm-mongodb:27017`
(`depends_on: service_healthy` orders startup). Atlas: app → driver → SRV over TLS.
Secrets always come from env / Secrets Manager, never hardcoded Java.

---

## 15. Troubleshooting

| Symptom | Signal | Cause | Fix |
|---|---|---|---|
| **Connection refused** | `ServerSelectionTimeout`, `mongo: DOWN` | No server; containerized app using `localhost` | Start MongoDB (§6–§7); Compose → `MONGO_HOST=mongodb`, host → `localhost`. |
| **Auth failed** | `Authentication failed` | Password or `authentication-database` mismatch | Re-run §13 Check 1; `admin` for root, owning DB for app users (§9). |
| **Selection timeout** | `after 30s` | Firewall/SG, TLS mismatch, Atlas allowlist | Raise timeout (§11); allowlist egress; match TLS + port. |
| **URI + keys both set** | Wrong host/DB | `uri` silently overrides discrete keys | One style only: keys locally, `uri` for Atlas (§10). |
| **Empty after Compose up** | `UP` but `find()` empty | Seed runs only on first volume creation | `down -v` to re-seed (wipes data); check `MONGO_DATABASE`. |
| **Test hits dev DB** | Shared data corrupted | Missing `@ServiceConnection` wiring | `MongoDBContainer("mongo:8")` + `@ServiceConnection` (§12). |
| **TLS fails (Atlas)** | `PKIX`, `SSLHandshakeException` | CA missing from truststore | Import CA; never set `tls=false` (§10). |

Drill order: `mongosh ping` → health → `getDb()` probe. First failure names the layer.

---

## 16. Pitfalls (GOLD)

1. **Discrete keys + `uri` together** — `uri` wins silently; one style per environment.
2. **Wrong `authentication-database`** — `admin` for root, owning DB for app users.
3. **`localhost` inside containers** — use the `MONGO_HOST=mongodb` pattern, not the literal.
4. **Dev password leaks to prod** — bare `${MONGO_ROOT_PASSWORD}` / `${MONGO_URI}` fails loudly.
5. **Expecting re-seed** — `init.js` runs once per volume; `down -v` is the only re-seed.
6. **Root past dev** — services get `readWrite` on one DB, never `root`.
7. **Publishing 27017 beyond dev** — private network / Atlas allowlist only.
8. **Early pool tuning** — defaults first; §11 sizing follows load evidence after §13 is green.
9. **Early mapping tuning** — converters and indexes are [002](./002-document-mapping.md), after connect.
10. **Health-only verification** — `mongo: UP` without the `getDb()` probe hides wrong-DB wiring.

---

## 17. Interview Q&A

**Q1. What does `spring-boot-starter-data-mongodb` auto-configure, and what disables it?**
A: `MongoAutoConfiguration` (`MongoClient` + `MongoDatabaseFactory`) and
`MongoDataAutoConfiguration` (`MongoTemplate`, converter, `mongo` health). A custom
`MongoClient`/`MongoDatabaseFactory` bean disables that slice — only for custom codecs (§3).

**Q2. When do you use `uri` versus discrete `host`/`port` keys?**
A: Discrete keys for local/Compose with `MONGO_*` env vars; one `mongodb+srv:// uri` for
Atlas where TLS and options travel together. Setting both is a bug — `uri` wins (§10).

**Q3. Right password but `Authentication failed` — top causes?**
A: (1) Wrong `authentication-database` (`admin` vs owning DB). (2) App points at a
different server/DB than the `mongosh` test — re-run §13 Check 1 with the exact URI.

**Q4. Same YAML works locally but `ServerSelectionTimeout` in Compose. Why?**
A: `${MONGO_HOST:localhost}` is the container's own loopback. Inside Compose it must be
the service name (`MONGO_HOST=mongodb`) — same artifact, different env (§12).

**Q5. How do you prove traffic reaches MongoDB?**
A: `mongosh ping` → `{ ok: 1 }`, `/actuator/health` → `mongo: UP`, and the `getDb()`
probe prints the expected DB plus a document count. Health alone can hide wrong-DB wiring.

---

## 18. Resources

**This track (in order):**

- [MongoDB Basics](./mongodb.md) — server, `mongosh`, Compass, modeling (prerequisite).
- [002 — Document Mapping](./002-document-mapping.md) — `@Document`, converters, indexes (follow-up).
- [013 — Product Catalog Example](./013-product-catalog-example.md) — worked catalog domain (follow-up).
- [001 — Testing with Spring Boot](../spring-testing/001-testing-with-spring-boot.md) — Testcontainers `mongodb:1.20.6`, `@ServiceConnection`.

**Official docs:** [Boot Reference — MongoDB](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.mongodb) · [Spring Data MongoDB](https://docs.spring.io/spring-data-mongodb/reference/) · [Java Driver](https://www.mongodb.com/docs/drivers/java/sync/current/) · [MongoDB 8.0](https://www.mongodb.com/docs/manual/release-notes/8.0/) · [mongosh](https://www.mongodb.com/docs/mongodb-shell/) · [Compass](https://www.mongodb.com/docs/compass/current/) · [Atlas](https://www.mongodb.com/docs/atlas/driver-connection/)

**Repo mirrors:** `product-catalog-service/pom.xml` (Boot 4.1.0, Java 25) · `.../application.yml` (`MONGO_HOST`/`MONGO_ROOT_*`/`MONGO_DATABASE`, `admin`) · `docker-compose.yaml` (`mongo:8`, `MONGO_INITDB_*`, `mongo_data:/data/db`, `init.js`, ping healthcheck, express) · `scripts/init-mongo.js` (seed + indexes).

---

*Next: [002 — Document Mapping](./002-document-mapping.md).*

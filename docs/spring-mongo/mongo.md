# MongoDB

## MongoDB Fundamentals

### What is MongoDB?

MongoDB is a document-oriented, general-purpose NoSQL database that stores data as flexible, JSON-like documents (BSON internally) rather than rows and columns. It was designed to handle large volumes of unstructured or semi-structured data with high availability and horizontal scalability through replication and sharding. MongoDB is developed and maintained by MongoDB Inc. and is available in Community and Enterprise editions, as well as a fully managed cloud offering called Atlas.

In production, teams choose MongoDB when the data model is naturally hierarchical (e.g., a product catalog with nested attributes, or a user profile with embedded addresses) and when schema flexibility is needed to iterate quickly without expensive migrations.

```javascript
// Connect via mongosh and insert a simple document
use myAppDb
db.users.insertOne({ name: "Alice", email: "alice@example.com", roles: ["admin"] })
```

**Advantages:**
- Flexible schema allows rapid iteration
- Horizontal scaling via sharding
- Rich query language and aggregation framework
- Native support for high availability via replica sets

**Disadvantages:**
- No native multi-document ACID transactions across shards before certain limits/performance costs
- Denormalized data can lead to duplication and update complexity
- Less mature tooling for complex joins compared to relational databases

**Interview Questions:**
- What is MongoDB and how does it differ from traditional databases?
- What problems was MongoDB designed to solve?
- What are the core building blocks of MongoDB (database, collection, document)?
- When would you NOT choose MongoDB for a project?

### NoSQL Databases

NoSQL ("Not Only SQL") databases are a broad category of non-relational data stores designed for flexible schemas, horizontal scalability, and high performance on large or fast-changing datasets. The four common categories are document stores (MongoDB), key-value stores (Redis), wide-column stores (Cassandra), and graph databases (Neo4j). Unlike relational databases, most NoSQL systems relax strict ACID guarantees in favor of availability and partition tolerance (per the CAP theorem), though modern MongoDB supports multi-document ACID transactions as well.

NoSQL databases are typically used for use cases like content management, real-time analytics, IoT data ingestion, and catalogs where the data shape varies or scale requirements exceed what a single relational server can handle efficiently.

**Differences:**

| Aspect | SQL (Relational) | NoSQL |
|---|---|---|
| Schema | Fixed, predefined | Flexible/dynamic |
| Scaling | Primarily vertical | Primarily horizontal |
| Data model | Tables with rows/columns | Documents, key-value, columnar, graph |
| Transactions | Strong ACID by default | Varies; MongoDB supports ACID multi-document transactions |
| Joins | Native, optimized | Limited or application-side |

**Interview Questions:**
- What are the main categories of NoSQL databases?
- How does the CAP theorem relate to NoSQL database design?
- Why might a team pick a NoSQL database over a relational one?
- Can NoSQL databases guarantee ACID transactions? Explain with MongoDB as an example.

### Document-Oriented Database

A document-oriented database stores data as self-contained documents (in MongoDB's case, BSON) that can contain nested objects and arrays, rather than normalizing data across multiple tables. Each document can have its own structure, meaning different documents in the same collection do not need identical fields. This model maps naturally to objects in application code, reducing the "object-relational impedance mismatch" common with relational databases.

For example, an e-commerce order with line items, shipping address, and payment info can be stored as a single document, avoiding multiple joins to reconstruct the order at read time.

```json
{
  "_id": "ORD1001",
  "customer": { "name": "Bob", "email": "bob@example.com" },
  "items": [
    { "sku": "A1", "qty": 2, "price": 19.99 },
    { "sku": "B2", "qty": 1, "price": 49.99 }
  ],
  "status": "SHIPPED"
}
```

**Interview Questions:**
- What does "document-oriented" mean in the context of MongoDB?
- How does storing nested data in one document compare to normalizing it across tables?
- What are the trade-offs of embedding related data inside a single document?

### BSON vs JSON

BSON (Binary JSON) is the binary-encoded serialization format MongoDB uses internally to store documents. It extends JSON's data model with additional types not natively supported by JSON, such as `Date`, `ObjectId`, `Binary Data`, `Decimal128`, and 64-bit integers, while also being more efficient to parse and traverse than text-based JSON. Applications interact with MongoDB using JSON-like syntax (e.g., in mongosh or driver code), but the data is stored and transmitted as BSON.

**Differences:**

| Aspect | JSON | BSON |
|---|---|---|
| Format | Text-based | Binary |
| Data types | String, Number, Boolean, Array, Object, Null | Adds Date, ObjectId, Binary, Decimal128, Int32/Int64, Timestamp, etc. |
| Parsing speed | Slower (text parsing) | Faster (binary, length-prefixed) |
| Size on disk | Typically smaller as text | Slightly larger due to type/length metadata, but faster to traverse |
| Human readability | Readable | Not directly readable |

**Interview Questions:**
- Why did MongoDB choose BSON over plain JSON internally?
- What extra data types does BSON support that JSON does not?
- Is BSON always more compact than JSON? Explain.

### MongoDB Architecture

MongoDB's architecture centers on a `mongod` process that manages data storage, a storage engine (WiredTiger by default) that handles on-disk data structures and concurrency, and optionally `mongos` routers for sharded clusters. In a replica set, one primary node handles writes and multiple secondary nodes replicate data asynchronously via the oplog, providing high availability and read scaling. Sharded clusters distribute data across multiple shards (each potentially a replica set) using a shard key, with config servers storing cluster metadata.

```mermaid
flowchart TD
    App[Application] --> Mongos[mongos Router]
    Mongos --> Shard1[Shard 1: Replica Set]
    Mongos --> Shard2[Shard 2: Replica Set]
    Mongos --> Config[Config Servers]
    Shard1 --> P1[Primary]
    Shard1 --> S1[Secondary]
    Shard1 --> S1b[Secondary]
    Shard2 --> P2[Primary]
    Shard2 --> S2[Secondary]
```

**Interview Questions:**
- What is the role of `mongod` versus `mongos`?
- How does data flow from a client write request to disk in a replica set?
- What is the purpose of config servers in a sharded cluster?
- What storage engine does MongoDB use by default and what does it provide?

### MongoDB Use Cases

MongoDB is well-suited for content management systems, product catalogs, user profiles, real-time analytics, IoT sensor data, and mobile/web application backends where data shapes evolve frequently. Its flexible schema and horizontal scalability make it a good fit for rapidly growing startups and applications with large write throughput. It is less ideal for workloads that require complex multi-table joins with strict referential integrity, such as heavy financial reporting systems traditionally built on relational databases.

**Interview Questions:**
- What kinds of applications benefit most from MongoDB?
- Give an example of a workload where MongoDB would be a poor fit.
- How does schema flexibility influence MongoDB's suitability for a use case?

### MongoDB vs Relational Databases

MongoDB and relational databases (like PostgreSQL or MySQL) differ fundamentally in data modeling, schema enforcement, and scaling strategy. Relational databases normalize data into tables connected via foreign keys and joins, enforcing a fixed schema and strong ACID guarantees by default. MongoDB favors denormalized, document-based models that reduce the need for joins, offers flexible schemas, and scales horizontally via sharding, while still supporting ACID transactions across documents when needed.

**Differences:**

| Aspect | Relational Database | MongoDB |
|---|---|---|
| Data unit | Row in a table | Document in a collection |
| Schema | Fixed, enforced by DDL | Flexible, optionally validated |
| Relationships | Foreign keys + joins | Embedding or manual references |
| Scaling | Mostly vertical (harder to shard) | Built-in horizontal sharding |
| Query language | SQL | MongoDB Query Language (MQL) / Aggregation Framework |
| Transactions | ACID by default | ACID supported (single and multi-document) |

**Interview Questions:**
- What are the core modeling differences between MongoDB and a relational database?
- When would you choose a relational database over MongoDB?
- How do joins in SQL compare to `$lookup` in MongoDB's aggregation framework?

### MongoDB Editions (Community vs Enterprise)

MongoDB Community Edition is the free, open-source version providing the core database engine, replication, and sharding. MongoDB Enterprise Advanced adds features such as LDAP/Kerberos authentication, encryption at rest with an external key management integration, auditing, in-memory storage engine, and Ops Manager for on-premises operational tooling. MongoDB Atlas, the managed cloud service, is built on Enterprise-grade features and removes the operational burden of provisioning and maintaining servers.

**Differences:**

| Aspect | Community Edition | Enterprise Advanced |
|---|---|---|
| Cost | Free | Licensed/paid |
| Authentication | Basic (SCRAM, x.509) | Adds LDAP, Kerberos |
| Auditing | Not included | Included |
| Encryption at rest | Manual/filesystem-level | Native with KMIP integration |
| Support | Community forums | Official MongoDB support |

**Interview Questions:**
- What is the difference between MongoDB Community and Enterprise editions?
- What advanced security features does Enterprise Edition add?
- How does MongoDB Atlas relate to Community and Enterprise editions?

## MongoDB Installation and Tools

### MongoDB Server

The MongoDB Server (`mongod`) is the core database process responsible for handling data requests, managing data storage on disk, and background management operations like replication. It can be installed locally via package managers (`apt`, `brew`), run as a Docker container, or provisioned through a cloud provider. Configuration is controlled via a YAML config file or command-line flags, covering things like the data directory, bind IP, and port.

```bash
# macOS install via Homebrew
brew tap mongodb/brew
brew install mongodb-community@7.0
brew services start mongodb-community@7.0
```

**Interview Questions:**
- What is `mongod` and what is its role?
- How would you install and start MongoDB locally on your OS of choice?
- What are common configuration options you would set for a production `mongod` instance?

### MongoDB Shell (mongosh)

`mongosh` is the modern, official command-line shell for interacting with MongoDB, offering a JavaScript-based REPL for running queries, administrative commands, and scripts. It replaced the legacy `mongo` shell and adds improved syntax highlighting, auto-completion, and better error messages. It's commonly used for ad-hoc queries, debugging, and running maintenance scripts.

```javascript
mongosh "mongodb://localhost:27017"
show dbs
use myAppDb
db.users.find({ age: { $gt: 25 } }).limit(5)
```

**Interview Questions:**
- What is `mongosh` and how does it differ from the legacy `mongo` shell?
- How do you connect to a remote MongoDB instance using `mongosh`?
- What are some common `mongosh` commands you use for troubleshooting?

### MongoDB Compass

MongoDB Compass is the official GUI application for visually exploring databases, collections, and documents, building and testing queries and aggregation pipelines, analyzing schema, and monitoring index/query performance. It's especially useful for developers who prefer a visual interface over the shell for exploring unfamiliar datasets or building complex aggregation pipelines interactively.

**Advantages:**
- Visual schema analysis and index recommendations
- Interactive aggregation pipeline builder with stage-by-stage preview
- Built-in performance/explain plan visualization

**Disadvantages:**
- Not scriptable like `mongosh` for automation
- Adds a GUI dependency not suited for headless/server environments

**Interview Questions:**
- What is MongoDB Compass used for?
- How can Compass help when designing or debugging an aggregation pipeline?
- What are the limitations of using Compass compared to the shell for automation?

### MongoDB Atlas (Overview)

MongoDB Atlas is the official fully managed Database-as-a-Service offering from MongoDB Inc., available on AWS, Azure, and GCP. It handles provisioning, patching, backups, monitoring, and scaling of clusters, while providing additional managed features like Atlas Search, Atlas Data Federation, and Atlas Triggers. Atlas is commonly used to avoid operational overhead of self-managing replica sets and sharded clusters.

**Advantages:**
- No infrastructure management; automated backups and patching
- Built-in monitoring, alerting, and performance advisor
- Global cluster support for multi-region deployments

**Disadvantages:**
- Ongoing subscription cost versus self-hosted
- Less control over low-level server configuration

**Interview Questions:**
- What is MongoDB Atlas and what problems does it solve?
- What managed features does Atlas provide beyond a plain MongoDB server?
- What trade-offs exist between self-hosting MongoDB and using Atlas?

### Configuration Basics

MongoDB server configuration is typically defined in a YAML file (commonly `mongod.conf`) covering settings such as `storage.dbPath`, `net.port`, `net.bindIp`, `security.authorization`, and `replication.replSetName`. Configuration can also be overridden via command-line flags when starting `mongod`. Properly securing configuration (enabling authentication, restricting bind IP, enabling TLS) is critical before exposing a MongoDB instance beyond localhost.

```yaml
# mongod.conf example
storage:
  dbPath: /var/lib/mongodb
net:
  port: 27017
  bindIp: 127.0.0.1
security:
  authorization: enabled
replication:
  replSetName: rs0
```

**Interview Questions:**
- What are the key sections of a `mongod.conf` file?
- How do you enable authentication/authorization on a MongoDB instance?
- Why is restricting `bindIp` important for security?

### MongoDB Drivers

MongoDB provides official drivers for many languages (Java, Node.js, Python, C#, Go, etc.) that translate native language calls into wire-protocol operations against the server. In the Java/Spring ecosystem, Spring Data MongoDB builds on the underlying MongoDB Java driver to provide repository abstractions, `MongoTemplate`, and object mapping between POJOs and BSON documents.

```java
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String name;
    private String email;
}

public interface UserRepository extends MongoRepository<User, String> {
    List<User> findByEmail(String email);
}
```

**Interview Questions:**
- What is the role of a MongoDB driver in an application?
- How does Spring Data MongoDB relate to the underlying MongoDB Java driver?
- What is the difference between using `MongoRepository` and `MongoTemplate` in Spring Data MongoDB?

### MongoDB Atlas Search (Overview)

Atlas Search is a full-text search capability built directly into MongoDB Atlas, powered by Apache Lucene, that allows rich text search (fuzzy matching, relevance scoring, autocomplete, faceting) without needing a separate search engine like Elasticsearch. Search indexes are defined separately from regular MongoDB indexes and queried using the `$search` aggregation stage.

```javascript
db.products.aggregate([
  {
    $search: {
      text: {
        query: "wireless headphones",
        path: "description"
      }
    }
  }
])
```

**Interview Questions:**
- What is Atlas Search and what underlying technology powers it?
- How does the `$search` aggregation stage differ from a regular `$match` query?
- When would you choose Atlas Search over a standalone search engine like Elasticsearch?

## Database Structure

### Databases

A MongoDB server can host multiple databases, each acting as an independent namespace containing its own set of collections. Databases are created implicitly the first time data is written to a collection within them (`use myDb` alone does not create the database until a write occurs). Each database has its own set of files on disk (per storage engine allocation) and can have its own users and access controls.

```javascript
use inventoryDb
db.products.insertOne({ name: "Widget" }) // creates inventoryDb on first write
show dbs
```

**Interview Questions:**
- When is a MongoDB database actually created on disk?
- How are databases isolated from one another in terms of access control?
- What built-in databases does MongoDB create by default (e.g., `admin`, `local`, `config`)?

### Collections

A collection is a grouping of documents, analogous to a table in a relational database, but without enforcing a rigid schema across its documents. Collections are created implicitly on first insert or explicitly via `createCollection()`, which also allows specifying options like schema validation rules, capped size, or collation. Indexes are defined per collection to optimize query performance.

```javascript
db.createCollection("orders", {
  validator: { $jsonSchema: { bsonType: "object", required: ["customerId", "items"] } }
})
```

**Interview Questions:**
- How does a MongoDB collection differ from a relational table?
- What options can you specify when explicitly creating a collection?
- What is a capped collection and when would you use one?

### Documents

A document is the basic unit of data in MongoDB, stored in BSON format, conceptually similar to a JSON object. Each document must have a unique `_id` field within its collection, which acts as the primary key and is automatically indexed. Documents can vary in structure from one another within the same collection, enabling polymorphic data models.

```json
{
  "_id": ObjectId("64f1a2b3c4d5e6f7a8b9c0d1"),
  "name": "Charlie",
  "age": 30,
  "address": { "city": "Seattle", "zip": "98101" }
}
```

**Interview Questions:**
- What role does the `_id` field play in a MongoDB document?
- What is the maximum size of a single BSON document and why does this limit exist?
- Can two documents in the same collection have completely different fields? Explain the implications.

### Fields

Fields are the key-value pairs that make up a document, similar to columns in a relational row, but not required to be consistent across documents in a collection. Field names are strings and values can be any BSON type, including nested documents and arrays. Field order is preserved in storage and retrieval but generally should not be relied upon logically.

**Interview Questions:**
- What data types can a field's value hold in MongoDB?
- Are field names case-sensitive in MongoDB?
- What restrictions exist on field names (e.g., leading `$`, dots)?

### Embedded Documents

Embedded documents are documents nested within a field of another document, allowing related data to be co-located and retrieved together in a single read operation. This is a core technique for denormalization in MongoDB's document model, reducing the need for joins/`$lookup`. Embedding is best suited for data that is always accessed together and doesn't grow unboundedly.

```json
{
  "_id": 1,
  "name": "Dana",
  "address": { "street": "123 Main St", "city": "Austin", "zip": "73301" }
}
```

**Interview Questions:**
- What is an embedded document and why would you use one?
- What are the risks of embedding documents that grow unbounded over time?
- How deep can BSON document nesting go, and what practical limits should you consider?

### Arrays

Arrays allow a single field to hold an ordered list of values, which can be primitives, embedded documents, or a mix of types. MongoDB provides rich query operators (`$elemMatch`, `$size`, `$all`) and update operators (`$push`, `$pull`, `$addToSet`) specifically for working with array fields. Arrays are also central to multikey indexes, which index each element of an array separately.

```javascript
db.students.updateOne(
  { _id: 1 },
  { $push: { grades: 95 } }
)

db.students.find({ grades: { $elemMatch: { $gte: 90 } } })
```

**Interview Questions:**
- How does MongoDB index array fields (multikey indexes)?
- What is the difference between `$push` and `$addToSet`?
- How would you query for documents where an array contains a specific value versus an element matching multiple conditions?

### Dynamic Schema

MongoDB's dynamic (flexible) schema means collections do not enforce a fixed structure by default — documents in the same collection can have different fields or types for the same field. This enables fast iteration during development since schema migrations aren't required to add new fields. However, uncontrolled flexibility can lead to inconsistent data, which is why MongoDB offers optional JSON Schema validation to enforce structure when needed.

**Advantages:**
- Fast iteration without downtime for schema migrations
- Naturally supports polymorphic and evolving data models

**Disadvantages:**
- Risk of inconsistent or malformed data without validation
- Application code must handle missing/optional fields defensively

**Interview Questions:**
- What does "dynamic schema" mean in MongoDB?
- How can you enforce structure on an otherwise schema-less collection?
- What are the risks of a fully dynamic schema in a large production system?

## BSON Data Types

### String

The `String` BSON type stores UTF-8 encoded text and is the most commonly used data type for textual data such as names, descriptions, and identifiers. MongoDB has no fixed length limit for strings other than the overall 16MB document size limit. String comparisons and sorting respect UTF-8 byte ordering by default unless a collation is specified.

```javascript
db.users.insertOne({ name: "Élise", bio: "Backend engineer" })
```

**Interview Questions:**
- How does MongoDB store and encode string data?
- How can you perform case-insensitive or locale-aware string comparisons in MongoDB?
- Is there a length limit on string fields?

### Number Types

MongoDB supports several numeric BSON types: `Int32`, `Int64` (Long), and `Double`, each with different precision and storage size. By default, numbers entered in `mongosh` without a suffix are stored as `Double`, which can cause unexpected precision issues for large integers unless explicitly cast with `NumberInt()` or `NumberLong()`. Choosing the right numeric type matters for both storage efficiency and accurate arithmetic (especially for financial data, where `Decimal128` is often preferred).

```javascript
db.metrics.insertOne({
  views: NumberInt(1000),
  totalRevenueCents: NumberLong(9999999999)
})
```

**Interview Questions:**
- What numeric BSON types does MongoDB support and how do they differ?
- Why might you explicitly cast a number to `NumberLong` in mongosh?
- Why is `Decimal128` sometimes preferred over `Double` for monetary values?

### Boolean

The `Boolean` type stores `true` or `false` values and is commonly used for flags such as `isActive` or `isDeleted`. Booleans are frequently used in query filters and are efficiently indexed, though a boolean index alone is usually low-selectivity and often combined with other fields in a compound index.

```javascript
db.users.find({ isActive: true })
```

**Interview Questions:**
- When is it appropriate to index a boolean field?
- Why is a standalone index on a low-cardinality boolean field often ineffective?

### Date

The `Date` BSON type stores a 64-bit integer representing milliseconds since the Unix epoch (January 1, 1970 UTC), independent of timezone — timezone formatting is a client-side/display concern. Dates should always be stored using the `Date` type (not strings) to enable proper range queries, sorting, and use with TTL indexes for automatic document expiration.

```javascript
db.sessions.insertOne({ userId: 1, createdAt: new Date() })
db.sessions.createIndex({ createdAt: 1 }, { expireAfterSeconds: 3600 })
```

**Interview Questions:**
- How does MongoDB internally store `Date` values?
- Why should dates be stored as the `Date` type rather than as strings?
- How do TTL indexes use `Date` fields to expire documents automatically?

### ObjectId

`ObjectId` is a special 12-byte BSON type most commonly used as the default value for a document's `_id` field. It's designed to be generated efficiently in a distributed manner without a central coordinator while remaining roughly sortable by creation time. (See the dedicated ObjectId section below for structure details.)

**Interview Questions:**
- Why does MongoDB default to `ObjectId` for the `_id` field instead of an auto-increment integer?
- Is `ObjectId` guaranteed to be globally unique? Why or why not?

### Array

The `Array` BSON type stores an ordered list of values under a single field, and is one of the two composite BSON types (along with embedded documents). Arrays support mixed element types, though consistent typing is best practice for predictable querying. MongoDB automatically creates multikey indexes when an indexed field contains an array.

**Interview Questions:**
- What is a multikey index and how does it relate to array fields?
- What data types can be stored inside a single array field?

### Embedded Document

The `Embedded Document` (aka `Object`) BSON type allows a field's value to itself be a full BSON document, enabling nested, hierarchical data structures. This is the mechanism behind "embedding" in MongoDB's data modeling and is queried using dot notation (e.g., `address.city`).

```javascript
db.users.find({ "address.city": "Austin" })
```

**Interview Questions:**
- How do you query fields inside an embedded document?
- What is the practical nesting depth limit for embedded documents in MongoDB?

### Null

The `Null` BSON type represents a deliberately empty or unknown value for a field, distinct from a field simply being absent from the document. Queries can distinguish between "field is null" and "field does not exist" using `$exists` combined with equality checks.

```javascript
db.users.find({ middleName: null })              // matches null OR missing field
db.users.find({ middleName: { $exists: true, $eq: null } }) // matches only explicit null
```

**Interview Questions:**
- What is the difference between a field being `null` versus not existing at all?
- How would you query specifically for documents where a field is explicitly set to `null`?

### Binary Data

The `Binary Data` (`BinData`) BSON type stores raw binary content such as images, files, or encrypted blobs directly within a document, subject to the overall 16MB document size limit. For larger files, MongoDB's GridFS specification splits data into chunks stored across multiple documents instead of using a single `BinData` field.

**Interview Questions:**
- When would you store binary data directly in a document versus using GridFS?
- What is GridFS and how does it work around the 16MB document size limit?

### Timestamp

The BSON `Timestamp` type is an internal MongoDB type used primarily by the oplog for replication, consisting of a 32-bit seconds value and a 32-bit ordinal counter to disambiguate operations within the same second. It is distinct from the `Date` type and is generally not intended for use in application-level document fields.

**Interview Questions:**
- How does BSON `Timestamp` differ from BSON `Date`?
- Where is the `Timestamp` type primarily used internally in MongoDB?

### Decimal128

`Decimal128` provides 128-bit decimal floating-point precision, avoiding the rounding errors inherent in binary floating-point (`Double`) representations. It is the recommended type for financial or monetary calculations that require exact decimal precision.

```javascript
db.invoices.insertOne({ amount: NumberDecimal("19.99") })
```

**Interview Questions:**
- Why is `Decimal128` preferred over `Double` for monetary values?
- What precision does `Decimal128` provide compared to standard floating-point types?

### UUID

MongoDB can store universally unique identifiers using the `Binary` subtype 4 (UUID), often used when integrating with external systems that already generate UUIDs, or when a non-sequential, globally unique identifier is needed outside of `ObjectId`. Drivers typically provide native UUID type mapping for convenience.

**Interview Questions:**
- How is a UUID represented at the BSON level?
- When might you use a UUID instead of the default `ObjectId` for `_id`?

### MinKey and MaxKey

`MinKey` and `MaxKey` are special BSON types that compare lower than and higher than all other BSON values, respectively, regardless of type. They're primarily used internally for sharding range boundaries and occasionally in queries to bound comparisons across mixed-type fields.

**Interview Questions:**
- What are `MinKey` and `MaxKey` used for in MongoDB?
- How does sharding use `MinKey`/`MaxKey` for chunk range boundaries?

### Regular Expression

The `Regular Expression` BSON type stores a pattern that can be used directly in queries for pattern matching, equivalent to using the `$regex` operator. Regex queries can leverage indexes efficiently only when the pattern is left-anchored (e.g., `^prefix`); unanchored patterns typically require a full collection scan.

```javascript
db.products.find({ sku: { $regex: /^AB-/ } })
```

**Interview Questions:**
- Under what conditions can a regex query use an index efficiently?
- What is the difference between storing a BSON regex versus using `$regex` in a query filter?

## ObjectId

### Structure of ObjectId

An `ObjectId` is a 12-byte value composed of a 4-byte timestamp (seconds since Unix epoch), a 5-byte random value unique to a machine/process, and a 3-byte incrementing counter, initialized to a random value. This structure guarantees a very high probability of uniqueness across distributed inserts without requiring coordination between servers, while still being roughly sortable by creation time due to the leading timestamp.

```mermaid
flowchart LR
    A["4 bytes: Timestamp"] --> B["5 bytes: Random value"] --> C["3 bytes: Counter"]
```

**Interview Questions:**
- What are the three components that make up an `ObjectId`?
- Why is the timestamp placed as the leading bytes of an `ObjectId`?
- How does the random value component help avoid collisions across different machines?

### Automatic ID Generation

If a document is inserted without an explicit `_id` field, the MongoDB driver automatically generates an `ObjectId` client-side before sending the insert to the server. This client-side generation avoids a round trip to the server just to obtain an ID and allows the application to know the ID immediately after calling insert.

```javascript
const result = db.users.insertOne({ name: "Eve" })
print(result.insertedId) // auto-generated ObjectId
```

**Interview Questions:**
- Where is the default `ObjectId` generated — on the client/driver or the server?
- What is the benefit of generating the `_id` before sending the insert to the server?

### Custom IDs

Applications can supply their own value for `_id` instead of relying on the auto-generated `ObjectId`, as long as the value is unique within the collection. Common alternatives include natural keys (e.g., an email or SKU), UUIDs, or application-specific sequence numbers. Using a custom `_id` avoids a separate unique index if the natural key is already guaranteed unique and frequently queried.

```javascript
db.products.insertOne({ _id: "SKU-1001", name: "Keyboard" })
```

**Interview Questions:**
- Can you use your own value for `_id` instead of `ObjectId`? What are the constraints?
- What are the trade-offs of using a natural key as `_id` versus an auto-generated `ObjectId`?
- What happens if you try to insert a document with a duplicate `_id`?

### ObjectId Advantages

Using `ObjectId` as the default identifier provides distributed, coordination-free uniqueness generation, rough time-ordering (useful for natural sort by creation time), and a compact 12-byte representation compared to a 36-character UUID string. Because it embeds a timestamp, you can even extract the approximate creation time of a document directly from its `_id` without a separate `createdAt` field.

```javascript
const id = ObjectId("64f1a2b3c4d5e6f7a8b9c0d1")
print(id.getTimestamp())
```

**Advantages:**
- No central coordination needed to guarantee uniqueness
- Roughly sortable by creation time
- Compact (12 bytes) compared to string UUIDs (16 bytes raw / 36 chars as text)

**Interview Questions:**
- What advantages does `ObjectId` provide over a simple auto-incrementing integer in a distributed system?
- How can you derive a document's approximate creation timestamp from its `ObjectId`?
- Why is `ObjectId` more compact than a UUID string representation?

## CRUD Concepts

### Insert Operations

Insert operations add new documents to a collection using `insertOne()` for a single document or `insertMany()` for multiple documents in one call. By default, `insertMany()` stops on the first error (ordered inserts), but this can be changed to continue past errors by passing `{ ordered: false }`. If `_id` is omitted, MongoDB generates it automatically.

```javascript
db.orders.insertOne({ customerId: 1, total: 59.99 })
db.orders.insertMany(
  [{ customerId: 2, total: 20 }, { customerId: 3, total: 45 }],
  { ordered: false }
)
```

**Interview Questions:**
- What is the difference between `insertOne()` and `insertMany()`?
- What does the `ordered` option control during a bulk insert?
- What happens if you attempt to insert a document with a duplicate `_id`?

### Read Operations

Read operations retrieve documents using `find()` (returns a cursor over multiple matching documents) or `findOne()` (returns the first match). Query filters use MongoDB Query Language operators (`$eq`, `$gt`, `$in`, etc.), and results can be shaped further with projections, sorting, and pagination via `sort()`, `limit()`, and `skip()`.

```javascript
db.orders.find({ total: { $gte: 50 } }, { customerId: 1, total: 1, _id: 0 })
  .sort({ total: -1 })
  .limit(10)
```

**Interview Questions:**
- What is the difference between `find()` and `findOne()`?
- How do projections improve query performance and network efficiency?
- Why can `skip()` become inefficient for pagination over large datasets, and what's an alternative?

### Update Operations

Update operations modify existing documents using `updateOne()`, `updateMany()`, or `findOneAndUpdate()`, typically applying update operators like `$set`, `$inc`, `$unset`, or `$push` rather than replacing the whole document. By default, only matched fields specified with update operators are changed — other fields remain untouched.

```javascript
db.orders.updateMany(
  { status: "PENDING" },
  { $set: { status: "PROCESSING" }, $currentDate: { updatedAt: true } }
)
```

**Interview Questions:**
- What is the difference between `updateOne()`, `updateMany()`, and `findOneAndUpdate()`?
- What is the difference between using `$set` versus passing a plain replacement document to `updateOne()`?
- How would you atomically increment a counter field on a document?

### Delete Operations

Delete operations remove documents using `deleteOne()` (removes the first match) or `deleteMany()` (removes all matches), both taking a filter document just like `find()`. There is no built-in "trash"/recycle bin — deletions are permanent, so applications requiring soft-deletes typically implement a boolean `isDeleted` flag instead of physically removing documents.

```javascript
db.orders.deleteMany({ status: "CANCELLED", createdAt: { $lt: new Date("2024-01-01") } })
```

**Interview Questions:**
- What is the difference between `deleteOne()` and `deleteMany()`?
- How would you implement a "soft delete" pattern in MongoDB?
- Is a deleted document recoverable directly from MongoDB after a `deleteMany()` call?

### Bulk Operations

Bulk operations allow grouping multiple insert, update, and delete operations into a single request via `bulkWrite()`, reducing network round trips and improving throughput for batch processing. Operations can be executed in `ordered` mode (stops at first error, preserves order) or `unordered` mode (continues past errors, can run in parallel server-side).

```javascript
db.orders.bulkWrite([
  { insertOne: { document: { customerId: 4, total: 15 } } },
  { updateOne: { filter: { customerId: 1 }, update: { $set: { total: 65 } } } },
  { deleteOne: { filter: { customerId: 2 } } }
])
```

**Interview Questions:**
- What is the benefit of using `bulkWrite()` over issuing individual operations?
- What is the difference between ordered and unordered bulk operations?
- How does the server handle errors mid-way through an unordered bulk operation?

### Upsert

An upsert is an update operation that inserts a new document if no document matches the filter, or updates the existing document if a match is found, enabled via the `{ upsert: true }` option. This is useful for "create or update" patterns, such as maintaining a counter or caching computed data, without requiring a separate existence check.

```javascript
db.counters.updateOne(
  { _id: "orderId" },
  { $inc: { seq: 1 } },
  { upsert: true }
)
```

**Interview Questions:**
- What does the `upsert` option do in an update operation?
- What value would `_id` take if an upsert results in an insert, and how is it determined?
- What race conditions can occur with upserts under concurrent writes, and how can unique indexes help?

### Replace Operations

`replaceOne()` replaces an entire matched document with a new document (except for `_id`, which cannot be changed), as opposed to `updateOne()` which modifies specific fields using operators. Because the whole document is replaced, any fields not included in the replacement document are removed.

```javascript
db.users.replaceOne(
  { _id: 1 },
  { name: "Frank", email: "frank@example.com" }
)
```

**Differences:**

| Aspect | `updateOne()` with `$set` | `replaceOne()` |
|---|---|---|
| Scope of change | Only specified fields | Entire document body |
| Fields not mentioned | Left untouched | Removed |
| `_id` field | Untouched | Cannot be changed, preserved |

**Interview Questions:**
- How does `replaceOne()` differ from `updateOne()` in terms of what gets modified?
- What happens to fields present in the old document but absent in the replacement document?
- Can you change a document's `_id` using `replaceOne()`?

## Data Modeling

### Embedding Documents

Embedding stores related data as nested sub-documents or arrays within a single parent document, favoring read performance and atomicity since the entire related dataset is fetched or updated in a single operation. It works best when related data is accessed together, has a bounded size, and doesn't need to be queried independently at scale.

```json
{
  "_id": 1,
  "name": "Order #1",
  "shippingAddress": { "street": "1 Elm St", "city": "Denver" }
}
```

**Advantages:**
- Single read retrieves all related data (fewer round trips)
- Updates to embedded data are atomic within the document

**Disadvantages:**
- Document can grow toward the 16MB size limit if embedding unbounded arrays
- Duplicated data across documents if the same sub-data appears in multiple parents

**Interview Questions:**
- When is embedding the right modeling choice versus referencing?
- What risks arise from embedding a continuously growing array (e.g., comments on a post)?
- How does embedding affect the atomicity of updates to related data?

### Referencing Documents

Referencing stores a relationship by saving the `_id` (or another key) of a related document instead of embedding its full content, similar to a foreign key in relational databases. Related data is retrieved with a separate query or via the `$lookup` aggregation stage, trading some read performance for reduced duplication and support for larger, independently-growing related datasets.

```javascript
// orders collection references customers by _id
db.orders.aggregate([
  { $match: { _id: 1 } },
  { $lookup: { from: "customers", localField: "customerId", foreignField: "_id", as: "customer" } }
])
```

**Advantages:**
- Avoids data duplication
- Supports large or independently-growing related collections

**Disadvantages:**
- Requires additional queries or `$lookup` joins, impacting read performance
- No native cross-collection referential integrity/foreign key enforcement

**Interview Questions:**
- How do you model a relationship using references instead of embedding?
- How does `$lookup` work and what are its performance considerations?
- Does MongoDB enforce referential integrity between referenced collections?

### One-to-One Relationships

A one-to-one relationship associates exactly one document in a collection with exactly one document in another (or embedded within the same document). Simple, bounded one-to-one data (like a user and their profile settings) is typically embedded, while larger or optional one-to-one data might be referenced to keep the primary document lean.

```json
{
  "_id": 1,
  "username": "gwen",
  "profile": { "bio": "Loves databases", "avatarUrl": "https://..." }
}
```

**Interview Questions:**
- Would you embed or reference a one-to-one relationship? What factors influence the decision?
- Give an example of a one-to-one relationship you'd model by embedding versus referencing.

### One-to-Many Relationships

A one-to-many relationship connects one document to multiple related documents, such as a blog post with many comments, or a customer with many orders. The modeling choice depends on scale: a "one-to-few" relationship (e.g., a few addresses per user) is often embedded, while "one-to-many" or "one-to-squillions" (e.g., an order history with thousands of entries, or sensor readings) is typically referenced with the "many" side storing a reference back to the "one" side.

```javascript
// customers collection (one) referenced from orders (many)
db.orders.find({ customerId: 1 })
```

**Interview Questions:**
- How would you decide between embedding and referencing for a one-to-many relationship?
- What is the "one-to-squillions" pattern and how should it be modeled?
- How would you paginate through the "many" side of a one-to-many relationship efficiently?

### Many-to-Many Relationships

Many-to-many relationships connect multiple documents on each side, such as students enrolled in multiple courses and courses having multiple students. This is typically modeled by storing arrays of references on one or both sides (e.g., an array of `courseIds` on the student document, or a separate join/junction collection for very large or frequently changing relationships).

```json
{ "_id": 1, "name": "Student A", "courseIds": [101, 102, 103] }
```

**Interview Questions:**
- How do you model a many-to-many relationship in MongoDB without a join table?
- When would you introduce a dedicated junction collection instead of array references?
- What are the query implications of storing an array of foreign references on both sides of the relationship?

### Denormalization

Denormalization intentionally duplicates data across documents/collections to optimize for read performance, avoiding the need for joins at query time. It's a natural fit for MongoDB's document model, but introduces the challenge of keeping duplicated copies in sync when the source data changes.

```json
{
  "_id": 1,
  "orderId": "ORD1",
  "customerName": "Grace",
  "customerEmail": "grace@example.com"
}
```

**Advantages:**
- Faster reads (no joins needed)
- Simplifies read-heavy access patterns

**Disadvantages:**
- Data can become inconsistent if not updated everywhere it's duplicated
- Extra write complexity to propagate changes

**Interview Questions:**
- What is denormalization and why is it common in MongoDB schema design?
- How would you keep denormalized copies of data consistent when the source changes?
- What's the trade-off between read performance and write complexity with denormalization?

### Schema Design Principles

Effective MongoDB schema design starts from the application's query patterns ("design for your queries") rather than normalizing data first. Key principles include: favor embedding for data accessed together, use references for large or independently-changing data, avoid unbounded array growth, and consider read/write ratios when deciding what to duplicate. Modeling should also account for document growth to avoid frequent document relocation on disk.

**Interview Questions:**
- What does "design your schema based on your application's query patterns" mean in practice?
- What factors would push you toward embedding versus referencing for a given relationship?
- How does anticipated document growth affect your schema design decisions?

### Schema Validation

MongoDB supports optional schema validation using JSON Schema rules attached to a collection via `$jsonSchema`, enforced on inserts and updates. Validation can specify required fields, types, value ranges, and more, with configurable validation levels (`strict` or `moderate`) and actions (`error` to reject or `warn` to just log). This lets teams keep MongoDB's flexibility while still guarding against malformed data for critical collections.

```javascript
db.createCollection("users", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["email"],
      properties: {
        email: { bsonType: "string", pattern: "^.+@.+$" },
        age: { bsonType: "int", minimum: 0 }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
})
```

**Interview Questions:**
- How do you enforce structure on a MongoDB collection despite its flexible schema?
- What is the difference between `validationLevel: "strict"` and `"moderate"`?
- What happens to existing documents when you add a validator to a collection that already has data violating the rules?

### Polymorphic Documents

Polymorphic documents are documents within the same collection that share some common fields but differ in others based on a "type" discriminator field, useful for modeling entities with shared behavior but different attributes (e.g., different payment methods, or different types of notifications). This pattern leverages MongoDB's flexible schema to avoid separate collections or excessive nullable columns as required in relational databases.

```json
{ "_id": 1, "type": "CREDIT_CARD", "last4": "4242", "expiry": "12/26" }
{ "_id": 2, "type": "PAYPAL", "email": "user@example.com" }
```

**Interview Questions:**
- What is a polymorphic document pattern and when would you use it?
- How would you query a collection efficiently when documents have a discriminator field?
- How does this pattern compare to modeling the same requirement in a relational database using nullable columns or table inheritance?

## Collections

### Capped Collections

Capped collections are fixed-size collections that preserve insertion order and automatically overwrite the oldest documents once the configured size (or document count) limit is reached, behaving like a circular buffer. Because there is no need to compute delete operations, writes are extremely fast and predictable in disk usage.

A common production use case is storing rolling application logs, recent audit events, or high-frequency telemetry where only the most recent window of data matters and older data can be discarded automatically without a background job.

```javascript
db.createCollection("appLogs", { capped: true, size: 5242880, max: 5000 });
db.appLogs.insertOne({ level: "INFO", msg: "service started", ts: new Date() });
db.appLogs.isCapped(); // true
```

**Advantages:**
- Very high write throughput; no delete overhead
- Naturally preserves insertion order without a secondary index
- Predictable, bounded disk footprint

**Disadvantages:**
- Individual documents cannot be deleted (only the whole collection can be dropped/recreated)
- Updates that increase a document's size are rejected
- Cannot be sharded, and cannot be manually resized without dropping and recreating

**Interview Questions:**
- What happens internally when a capped collection reaches its configured size limit?
- Can you delete a single document from a capped collection? Why or why not?
- How do capped collections differ from using a TTL index on a regular collection?
- Why can't capped collections be sharded?

### Time Series Collections

Time series collections (introduced in MongoDB 5.0) are a purpose-built collection type optimized for storing sequences of measurements over time. Internally, MongoDB automatically groups related documents into compressed "buckets" by time range and metadata, which drastically reduces storage size and improves the performance of time-range queries and aggregations compared to storing raw documents.

They are ideal for IoT sensor readings, financial tick data, infrastructure/application metrics, or any workload that continuously ingests timestamped data and queries it by time range.

```javascript
db.createCollection("deviceReadings", {
  timeseries: {
    timeField: "timestamp",
    metaField: "deviceMetadata",
    granularity: "seconds"
  },
  expireAfterSeconds: 2592000 // optional TTL, 30 days
});

db.deviceReadings.insertOne({
  timestamp: new Date(),
  deviceMetadata: { deviceId: "sensor-42", location: "warehouse-1" },
  temperature: 21.5
});
```

```mermaid
flowchart LR
    A[Insert measurement] --> B{Matching bucket<br/>by metaField + time range?}
    B -- yes --> C[Append to existing bucket]
    B -- no --> D[Create new bucket]
    C --> E[Compressed on-disk storage]
    D --> E
```

**Advantages:**
- Automatic bucketing reduces storage and index overhead significantly
- Optimized query performance for time-range scans and aggregations
- Integrates with TTL for automatic data expiration

**Disadvantages:**
- Update/delete flexibility is more limited than regular collections
- Requires careful selection of `metaField` and `granularity` for optimal compression

**Differences:**

| Aspect | Time Series Collection | Capped Collection |
|---|---|---|
| Purpose | Timestamped measurement data | Fixed-size rolling buffer |
| Storage | Compressed, bucketed by time/metadata | Raw documents, fixed allocation |
| Expiration | TTL-based, automatic | Overwrite oldest on size limit |
| Query optimization | Time-range aware | None specific |

**Interview Questions:**
- What problem do time series collections solve compared to storing raw documents?
- What role does the `metaField` play in bucketing?
- How can you expire old time series data automatically?
- How does granularity affect bucket size and query performance?

### Collection Validation

Collection validation lets you enforce a schema on documents using `$jsonSchema` (or query-style validation expressions) at the collection level. MongoDB checks the validator on inserts and updates, with `validationLevel` controlling how strictly it's enforced (`strict` vs `moderate`) and `validationAction` controlling whether violations are rejected (`error`) or just logged as warnings (`warn`).

This is useful when you want the flexibility of a document model during development but need guardrails in production to prevent malformed or inconsistent data from entering critical collections.

```javascript
db.createCollection("orders", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["customerId", "items", "status"],
      properties: {
        status: { enum: ["PENDING", "SHIPPED", "DELIVERED"] },
        items: { bsonType: "array", minItems: 1 }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

// Modify validation on an existing collection
db.runCommand({
  collMod: "orders",
  validationLevel: "moderate"
});
```

**Advantages:**
- Prevents malformed documents without requiring an external schema layer
- `moderate`/`warn` modes allow gradual rollout of stricter schemas
- Works alongside application-level validation (e.g., Spring Data MongoDB `@Validated` beans) as a defense-in-depth layer

**Disadvantages:**
- Validation errors can be less descriptive than application-level validation messages
- Schema changes require `collMod` and coordination with application deploys
- Does not validate documents already in the collection prior to the rule being added

**Interview Questions:**
- What is the difference between `validationLevel: strict` and `moderate`?
- What happens to existing documents when you add a validator to a populated collection?
- How would you roll out a stricter schema without breaking existing writes?
- How does collection validation compare to enforcing structure at the application layer?

### Collection Options

Collections can be created with several configuration options beyond the default: `capped`, `size`, `max` (capped settings), `collation` (locale-aware string comparison rules), `storageEngine` (engine-specific options), `validator`/`validationLevel`/`validationAction` (schema rules), `timeseries` (time series settings), and `clusteredIndex`. These options are set at creation time via `db.createCollection()` and some can later be altered using the `collMod` command.

A common real-world example is setting a collection-wide `collation` so that string sorting and comparisons follow a specific language's rules (e.g., case-insensitive comparisons) without needing to specify collation on every query.

```javascript
db.createCollection("products", {
  collation: { locale: "en", strength: 2 } // case-insensitive comparisons
});
```

**Interview Questions:**
- What options can be configured when creating a collection in MongoDB?
- How does setting a default `collation` on a collection affect query behavior?
- Which collection options can be changed after creation using `collMod`, and which cannot?

### Views

A view is a read-only, non-materialized "virtual collection" whose contents are computed on-the-fly by running an aggregation pipeline over an underlying source collection (or another view) each time it is queried. Views are useful for exposing a simplified, filtered, or reshaped projection of data to consumers without duplicating storage.

For example, you might expose an `activeCustomers` view over a large `customers` collection so reporting tools only ever see active, non-sensitive fields, without granting direct access to the raw collection.

```javascript
db.createView("activeCustomers", "customers", [
  { $match: { status: "ACTIVE" } },
  { $project: { name: 1, email: 1 } }
]);

db.activeCustomers.find({ name: /^A/ });
```

**Advantages:**
- No data duplication; always reflects current underlying data
- Simplifies access control by exposing only derived/filtered fields
- Reuses the full power of the aggregation framework

**Disadvantages:**
- Cannot be written to directly (no insert/update/delete on a view)
- No dedicated indexes of its own; performance depends on the underlying collection's indexes and pipeline complexity
- Adds pipeline execution overhead on every query compared to a materialized collection

**Interview Questions:**
- How is a view different from a materialized collection produced by `$merge` or `$out`?
- Can you create indexes directly on a view?
- Why might you use a view instead of restricting fields at the application layer?
- What happens to a view's results if the underlying collection is updated?

### Clustered Collections

A clustered collection stores documents ordered directly by the value of a clustered index key (typically `_id`), meaning the collection's data file itself acts as the index — similar to a clustered index in relational engines like InnoDB. This eliminates the need for a separate `_id` index and can significantly reduce storage overhead and improve range-scan performance on the cluster key.

This is particularly beneficial for large, append-heavy collections such as time-ordered event logs where most queries filter or range-scan on `_id` or another monotonically increasing key.

```javascript
db.createCollection("events", {
  clusteredIndex: { key: { _id: 1 }, unique: true }
});
```

**Advantages:**
- Reduced storage footprint by avoiding a duplicate `_id` index
- Faster range scans on the clustered key
- Good fit for time-series-like or append-only workloads

**Disadvantages:**
- The clustering key must be chosen at creation time and is difficult to change later
- Not a general-purpose replacement for secondary indexes on other fields

**Differences:**

| Aspect | Clustered Collection | Regular Collection |
|---|---|---|
| `_id` storage | Data ordered by `_id`, no separate index | Separate B-tree index on `_id` |
| Range scans on `_id` | Very efficient | Requires index lookup + fetch |
| Storage overhead | Lower | Higher (extra index) |
| Flexibility | Cluster key fixed at creation | N/A |

**Interview Questions:**
- What is the core difference between a clustered collection and a normal collection with a default `_id` index?
- What kinds of workloads benefit most from clustered collections?
- Can the clustered index key be changed after the collection is created?

## Indexing

### Index Fundamentals

Indexes are special data structures (B-trees in MongoDB's WiredTiger engine) that store a small, ordered subset of a collection's data, allowing the query engine to find matching documents without scanning every document (a "collection scan"). Every collection automatically gets a default index on `_id`; all other indexes must be created explicitly based on query patterns.

```mermaid
flowchart TD
    Q[Query arrives] --> P{Index available<br/>on filter field?}
    P -- yes --> I[Index seek: traverse B-tree]
    P -- no --> C[Collection scan: read every document]
    I --> R[Fetch matching documents]
    C --> R
```

**Advantages:**
- Dramatically reduces query latency for selective filters, sorts, and joins (`$lookup`)
- Enables efficient range queries and sorted results

**Disadvantages:**
- Each index adds write overhead (every insert/update/delete must maintain the index) and consumes RAM/disk
- Poorly chosen indexes can be worse than no index at all (unused index bloat)

**Interview Questions:**
- What is the default index every MongoDB collection has?
- What data structure does MongoDB use to implement indexes?
- What is the tradeoff between adding more indexes and write performance?
- How do you determine whether a query is using an index effectively?

### Single Field Index

A single field index is built on one field of the documents in a collection, in ascending (1) or descending (-1) order. It's the simplest and most common index type, ideal for equality and range queries on a specific field.

```javascript
db.users.createIndex({ email: 1 });
db.users.find({ email: "a@example.com" }); // uses the index
```

**Interview Questions:**
- Does the sort direction (1 vs -1) of a single field index matter for query performance?
- When would a single field index be insufficient and a compound index be required?

### Compound Index

A compound index spans multiple fields, stored in the order the fields are declared. The order of fields matters greatly — MongoDB follows the "ESR rule" (Equality, Sort, Range) for optimal field ordering, and a compound index can support queries on a leading prefix of its fields (similar to how B-tree prefixes work).

Real-world example: an e-commerce query that filters by `status` (equality), sorts by `createdAt`, would benefit from a compound index `{ status: 1, createdAt: -1 }`.

```javascript
db.orders.createIndex({ status: 1, createdAt: -1 });
db.orders.find({ status: "SHIPPED" }).sort({ createdAt: -1 }); // fully covered by index
```

**Advantages:**
- Supports multiple query shapes from one index (via prefix matching)
- Can satisfy filter + sort in a single index traversal, avoiding an in-memory sort

**Disadvantages:**
- Field order is critical; a poorly ordered compound index may not be used as expected
- More expensive to maintain on writes than a single-field index

**Differences:**

| Aspect | Single Field Index | Compound Index |
|---|---|---|
| Fields indexed | One | Two or more |
| Prefix queries | N/A | Supports queries on leading prefixes |
| Use case | Simple equality/range on one field | Multi-field filter/sort combinations |

**Interview Questions:**
- What is the ESR (Equality, Sort, Range) rule for compound index field ordering?
- Can a query use only part of a compound index? Explain prefix matching.
- Why does field order matter in a compound index but not necessarily in the query filter itself?

### Multikey Index

A multikey index is automatically created when you index a field that holds an array — MongoDB creates a separate index entry for each element of the array. This lets you efficiently query for documents where an array field contains a specific value.

```javascript
db.products.createIndex({ tags: 1 });
db.products.insertOne({ name: "Laptop", tags: ["electronics", "computers", "sale"] });
db.products.find({ tags: "sale" }); // uses multikey index
```

**Disadvantages:**
- Cannot create a compound multikey index where more than one field being indexed is an array in the same document
- Larger index size proportional to array length

**Differences:**

| Aspect | Regular Index | Multikey Index |
|---|---|---|
| Field type | Scalar value | Array value |
| Index entries per document | 1 | 1 per array element |
| Compound restriction | None | Only one array field per compound index |

**Interview Questions:**
- Why can't a compound index have more than one array field?
- How does MongoDB decide whether to build a multikey index automatically?
- What is the storage cost implication of indexing a large array field?

### Text Index

A text index enables full-text search across string content in one or more fields, supporting language-aware stemming, stop-word removal, and relevance scoring via `$text` and `$meta: "textScore"`. A collection can have at most one text index (though it can cover multiple fields).

```javascript
db.articles.createIndex({ title: "text", body: "text" });
db.articles.find(
  { $text: { $search: "mongodb indexing" } },
  { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } });
```

**Advantages:**
- Built-in relevance scoring and language stemming without an external search engine
- Simple to set up for basic search requirements

**Disadvantages:**
- Limited compared to dedicated search engines (e.g., Atlas Search/Elasticsearch) — no fuzzy matching, typo tolerance, or advanced ranking
- Only one text index allowed per collection

**Interview Questions:**
- How many text indexes can a single collection have?
- How does `$text` search differ from a regex-based search?
- When would you choose Atlas Search or an external search engine over a native text index?

### Geospatial Index

Geospatial indexes (`2dsphere` for GeoJSON/earth-like geometry, `2d` for legacy planar coordinates) allow efficient queries on location data, such as finding documents within a radius, inside a polygon, or nearest to a point.

```javascript
db.places.createIndex({ location: "2dsphere" });
db.places.find({
  location: {
    $near: {
      $geometry: { type: "Point", coordinates: [-73.99, 40.73] },
      $maxDistance: 5000
    }
  }
});
```

**Interview Questions:**
- What is the difference between a `2d` index and a `2dsphere` index?
- What GeoJSON operators can be used alongside a `2dsphere` index (e.g., `$near`, `$geoWithin`, `$geoIntersects`)?
- What real-world features would require a geospatial index?

### Hashed Index

A hashed index stores hashes of a field's value rather than the value itself, producing a uniform, random distribution of index keys. It's primarily used as a shard key strategy to avoid monotonically increasing shard keys causing "hot" shards, since hashing evenly spreads writes across the cluster.

```javascript
db.sessions.createIndex({ userId: "hashed" });
sh.shardCollection("app.sessions", { userId: "hashed" });
```

**Advantages:**
- Even data distribution across shards, avoiding hotspots
- Good for equality queries on the hashed field

**Disadvantages:**
- Cannot efficiently support range queries (hashes destroy ordering)
- Cannot be a compound index or a multikey index

**Interview Questions:**
- Why is a hashed index commonly used as a shard key?
- Why can't a hashed index support range queries?
- What problem does hashed sharding solve compared to range-based sharding on a monotonically increasing field?

### TTL Index

A TTL (Time-To-Live) index automatically deletes documents from a collection after a specified number of seconds past a date field, implemented via a background task that runs periodically (roughly every 60 seconds). It's commonly used for session data, verification tokens, caches, or logs that should expire automatically.

```javascript
db.sessions.createIndex({ lastAccessed: 1 }, { expireAfterSeconds: 1800 });
```

**Advantages:**
- Automatic cleanup without cron jobs or application-level deletion logic
- Reduces storage growth for transient data

**Disadvantages:**
- Deletion isn't immediate/precise — the background TTL thread runs periodically, so expired documents can linger briefly
- Only works on a single date field per index (or via `expireAfterSeconds: 0` for exact expiry timestamps)

**Interview Questions:**
- How precise is the timing of TTL-based document deletion?
- Can a TTL index be a compound index?
- How would you implement a "delete exactly at a given timestamp" pattern using a TTL index?

### Unique Index

A unique index enforces that no two documents in a collection can have the same value for the indexed field(s), rejecting inserts/updates that would create a duplicate. It can be a single or compound index, and combined with `sparse` to allow multiple documents missing the field.

```javascript
db.users.createIndex({ email: 1 }, { unique: true });
```

**Interview Questions:**
- What happens if you try to create a unique index on a field with existing duplicate values?
- How do unique indexes interact with sharding (constraints on shard key)?
- How would you allow multiple documents to omit a uniquely-indexed field?

### Sparse Index

A sparse index only includes documents that actually contain the indexed field, skipping documents where the field is missing. This keeps the index smaller and avoids issues like unique-index conflicts among documents that lack the field (which would otherwise all be treated as `null`).

```javascript
db.users.createIndex({ phoneNumber: 1 }, { sparse: true, unique: true });
```

**Disadvantages:**
- Queries that don't account for sparseness might unexpectedly miss documents that don't have the field, when using that index for sorting

**Differences:**

| Aspect | Sparse Index | Partial Index |
|---|---|---|
| Inclusion rule | Skips docs missing the field | Skips docs not matching a filter expression |
| Flexibility | Field-existence only | Arbitrary filter conditions |
| Recommended usage | Legacy option | Preferred, more expressive (MongoDB recommends partial over sparse) |

**Interview Questions:**
- What is the difference between a sparse index and a partial index?
- Why might a sort operation behave unexpectedly when using a sparse index?
- Why does MongoDB generally recommend partial indexes over sparse indexes today?

### Partial Index

A partial index only indexes documents that satisfy a specified filter expression, reducing index size and maintenance cost by excluding irrelevant documents. This is more flexible than a sparse index since the filter can be any valid query expression, not just field existence.

```javascript
db.orders.createIndex(
  { customerId: 1 },
  { partialFilterExpression: { status: "ACTIVE" } }
);
```

**Advantages:**
- Smaller index size and lower write overhead by excluding irrelevant documents
- More expressive filtering than sparse indexes

**Interview Questions:**
- How does a partial index reduce storage and write costs compared to a full index?
- Can a query use a partial index if its filter doesn't match the partial filter expression exactly?
- Give an example of a real-world scenario where a partial index is preferable to a full index.

### Covered Queries

A covered query is one where all the fields requested in the query (both the filter and the projection) are present in the index itself, so MongoDB can return results directly from the index without ever reading the actual documents. This significantly improves performance since it avoids extra document fetches.

```javascript
db.users.createIndex({ email: 1, name: 1 });
db.users.find({ email: "a@example.com" }, { _id: 0, email: 1, name: 1 }); // covered
```

**Interview Questions:**
- What conditions must be met for a query to be "covered" by an index?
- Why must `_id` be explicitly excluded in the projection for many covered queries?
- How would you verify using `explain()` whether a query is covered?

### Index Selection

Index selection refers to the query planner's process of choosing which available index (if any) to use for a given query, based on the query shape, sort, and estimated selectivity. MongoDB caches winning query plans and may run a "plan ranking" competition among candidate indexes using sampled execution.

```javascript
db.orders.find({ status: "SHIPPED" }).explain("executionStats");
```

**Interview Questions:**
- How does MongoDB decide which index to use when multiple indexes could satisfy a query?
- What is plan caching, and when does MongoDB re-evaluate a cached plan?
- How can you force MongoDB to use a specific index?

### Index Best Practices

Good indexing strategy balances query performance against write overhead and memory usage. Key practices: index fields used in frequent equality/range/sort filters, follow the ESR rule for compound indexes, avoid redundant/overlapping indexes, use partial indexes to shrink footprint, monitor with `$indexStats` and `explain()`, and drop unused indexes.

```javascript
db.orders.aggregate([{ $indexStats: {} }]); // shows usage counts per index
```

**Interview Questions:**
- What metrics would you look at to decide whether an index is unused and safe to drop?
- What is index "prefix redundancy" and how do you avoid it?
- How many indexes is "too many" for a write-heavy collection, and why?

### Wildcard Index

A wildcard index (`{ "field.$**": 1 }`) indexes all fields (or all fields under a subdocument/array) dynamically, which is useful for collections with highly variable or unknown schemas where you can't predict every field that will need indexing ahead of time.

```javascript
db.products.createIndex({ "attributes.$**": 1 });
db.products.find({ "attributes.color": "red" }); // uses wildcard index
```

**Advantages:**
- Supports flexible, schema-less or highly variable document shapes
- Avoids needing to create/maintain dozens of individual field indexes

**Disadvantages:**
- Larger and more expensive to maintain than targeted indexes
- Cannot be used as a unique index, and has restrictions around compound usage

**Interview Questions:**
- When would a wildcard index be preferable to creating many individual single-field indexes?
- What are the limitations of wildcard indexes compared to targeted indexes?
- Can a wildcard index enforce uniqueness?

## Query Processing

### Query Execution

Query execution is the process by which MongoDB takes a parsed query, consults the query planner to select an execution plan, and then runs that plan against the storage engine to produce a result cursor. It involves stages such as index scans, document fetches, filtering, sorting, and projection, each represented as a node in the execution plan tree.

```mermaid
flowchart LR
    A[Parse query] --> B[Query Planner]
    B --> C[Select best plan]
    C --> D[Execute plan]
    D --> E[Return cursor to client]
```

**Interview Questions:**
- What are the high-level steps MongoDB takes from receiving a query to returning results?
- What is a cursor and how does it relate to query execution?

### Query Planner

The query planner evaluates candidate indexes and access paths for a given query shape, running short trial executions of each candidate plan and selecting the most efficient one based on the fewest documents examined/work done. The winning plan is cached for reuse on subsequent queries with the same shape.

**Interview Questions:**
- How does the query planner choose between multiple candidate indexes?
- What is a "query shape" and why does it matter for plan caching?
- When does MongoDB invalidate or re-evaluate a cached query plan?

### Execution Plans

An execution plan is a tree of stages (e.g., `IXSCAN`, `COLLSCAN`, `FETCH`, `SORT`, `PROJECTION`) describing exactly how MongoDB will retrieve and process data for a query. Understanding this tree is essential for diagnosing slow queries.

```javascript
db.orders.find({ status: "SHIPPED" }).sort({ createdAt: -1 }).explain("executionStats");
```

**Interview Questions:**
- What does an `IXSCAN` stage indicate versus a `COLLSCAN` stage?
- What does a `FETCH` stage do, and why is minimizing fetched documents important?
- What does a `SORT` stage appearing in the plan (rather than being satisfied by an index) imply about performance?

### Explain Plans

The `explain()` method reveals how a query was (or would be) executed, with three verbosity modes: `queryPlanner` (chosen plan only), `executionStats` (actual runtime stats like documents examined/returned), and `allPlansExecution` (stats for all candidate plans considered).

```javascript
db.orders.find({ status: "SHIPPED" }).explain("executionStats");
```

**Interview Questions:**
- What is the difference between `queryPlanner`, `executionStats`, and `allPlansExecution` modes?
- What key metrics would you check in `executionStats` to detect an inefficient query (e.g., `totalDocsExamined` vs `nReturned`)?
- How would you use `explain()` to confirm a covered query?

### Query Optimization

Query optimization is the practice of restructuring queries and indexes so MongoDB examines the minimum number of documents/index entries needed to satisfy a request. Techniques include adding appropriate indexes, following the ESR rule for compound indexes, using projections to limit returned fields, and avoiding unselective regex or `$where` queries.

**Advantages:**
- Reduces latency and server resource consumption
- Improves throughput under concurrent load

**Interview Questions:**
- What is the ratio between `nReturned` and `totalDocsExamined` telling you about query efficiency?
- Why are unanchored regular expressions (e.g., `/abc/`) generally bad for query performance?
- How would you optimize a query that currently triggers a full collection scan?

### Projection

Projection controls which fields are included or excluded from query results, reducing network payload size and, when combined with a covering index, avoiding document fetches entirely. Projections can use inclusion (`{ field: 1 }`) or exclusion (`{ field: 0 }`), but generally not both (except for `_id`).

```javascript
db.users.find({ status: "ACTIVE" }, { name: 1, email: 1, _id: 0 });
```

**Interview Questions:**
- Can you mix inclusion and exclusion in the same projection document?
- How does projection interact with covered queries?
- What is the default behavior for the `_id` field in projections?

### Pagination

Pagination retrieves data in pages/chunks rather than all at once. MongoDB supports offset-based pagination (`skip()`/`limit()`) and cursor/range-based ("keyset") pagination using a sort field and `$gt`/`$lt` filters. Keyset pagination scales far better for deep pages since `skip()` still has to walk over skipped documents internally.

```javascript
// Offset-based (slow for large skip values)
db.products.find().sort({ _id: 1 }).skip(1000).limit(20);

// Keyset/range-based (efficient, uses index)
db.products.find({ _id: { $gt: lastSeenId } }).sort({ _id: 1 }).limit(20);
```

**Differences:**

| Aspect | Offset (`skip`/`limit`) | Keyset (range-based) |
|---|---|---|
| Performance at scale | Degrades with larger skip values | Consistent, index-driven |
| Random page access | Yes (jump to page N) | No (sequential only) |
| Implementation complexity | Simple | Requires tracking last seen key |

**Interview Questions:**
- Why does `skip()` become slow for large offsets even with an index?
- How would you implement keyset (cursor-based) pagination in MongoDB?
- What are the tradeoffs of keyset pagination versus offset pagination?

### Sorting

Sorting orders query results by one or more fields, either using an index (fast, no extra memory) or, if no suitable index exists, an in-memory sort (subject to a 100MB memory limit unless `allowDiskUse` is enabled in aggregation). Compound indexes matching the sort fields (in the ESR order) avoid expensive in-memory sorts.

```javascript
db.orders.createIndex({ status: 1, createdAt: -1 });
db.orders.find({ status: "SHIPPED" }).sort({ createdAt: -1 }); // index-based sort
```

**Interview Questions:**
- What happens when MongoDB cannot satisfy a sort using an index?
- What is the 100MB in-memory sort limit, and how can it be worked around in aggregation pipelines?
- How does field order in a compound index affect whether it can support a sort?

### Cursors

A cursor is a pointer to the result set of a query, allowing the client to iterate through results in batches instead of loading everything into memory at once. Cursors are lazily evaluated on the server and can time out if left idle (unless configured otherwise), and methods like `sort()`, `limit()`, and `skip()` can be chained onto them before iteration begins.

```javascript
const cursor = db.orders.find({ status: "SHIPPED" }).batchSize(100);
while (cursor.hasNext()) {
  printjson(cursor.next());
}
```

**Interview Questions:**
- How does `batchSize()` affect network round trips when iterating a cursor?
- What causes a cursor to time out, and how can you prevent it for long-running operations?
- How do cursors relate to pagination strategies in an application?

## Aggregation Framework

### Aggregation Pipeline

The aggregation pipeline is a framework for transforming and analyzing documents by passing them through a sequence of stages, each performing an operation (filter, reshape, group, join, etc.) on the data and passing the result to the next stage — conceptually similar to Unix pipes. It is MongoDB's primary tool for complex analytics, reporting, and data transformation that goes beyond simple `find()` queries.

```mermaid
flowchart LR
    A[(Collection)] --> S1["$match"] --> S2["$group"] --> S3["$sort"] --> R[Result documents]
```

```javascript
db.orders.aggregate([
  { $match: { status: "SHIPPED" } },
  { $group: { _id: "$customerId", total: { $sum: "$amount" } } },
  { $sort: { total: -1 } }
]);
```

**Advantages:**
- Expressive, composable stages cover filtering, joining, grouping, and reshaping in one query
- Can push computation to the server, reducing data transferred to the application

**Disadvantages:**
- Complex pipelines can be harder to read/debug than simple queries
- Some stages (e.g., unindexed `$sort`, `$group`) can be memory-intensive

**Interview Questions:**
- How is the aggregation pipeline conceptually similar to Unix pipes?
- What is the difference between using `find()` with a filter versus an aggregation `$match` stage?
- How would you debug a slow aggregation pipeline?

### Pipeline Stages

Pipeline stages are the individual building blocks of an aggregation, executed in order, each consuming the output documents of the previous stage. Common stages include `$match`, `$project`, `$group`, `$sort`, `$limit`, `$skip`, `$unwind`, `$lookup`, `$facet`, `$bucket`, `$merge`, and `$out`. Stage order matters both for correctness and performance.

**Interview Questions:**
- Why does the order of stages in an aggregation pipeline matter for performance?
- Which pipeline stages can make use of an index?
- Can the same stage type (e.g., `$match`) appear multiple times in one pipeline?

### Match

`$match` filters documents based on a query condition, functioning like the `find()` filter but within a pipeline. Placing `$match` as early as possible in a pipeline is a key optimization since it reduces the document count flowing into subsequent stages, and an early `$match` can use indexes.

```javascript
{ $match: { status: "SHIPPED", createdAt: { $gte: ISODate("2026-01-01") } } }
```

**Interview Questions:**
- Why is it recommended to place `$match` as early as possible in a pipeline?
- Can `$match` use an index? Under what conditions?

### Project

`$project` reshapes documents by including, excluding, renaming, or computing new fields using expressions. It's often used to strip unneeded fields early (reducing memory for later stages) or to compute derived values.

```javascript
{ $project: { name: 1, totalWithTax: { $multiply: ["$amount", 1.08] }, _id: 0 } }
```

**Interview Questions:**
- How does `$project` differ from the `projection` argument of `find()`?
- Can `$project` create entirely new computed fields? Give an example.

### Group

`$group` aggregates documents by a specified `_id` key (which can be `null` for a single overall group), computing accumulator expressions like `$sum`, `$avg`, `$min`, `$max`, `$push`, and `$addToSet` per group — analogous to SQL's `GROUP BY`.

```javascript
{ $group: { _id: "$customerId", orderCount: { $sum: 1 }, avgAmount: { $avg: "$amount" } } }
```

**Interview Questions:**
- How would you compute a grand total across all documents (not grouped by any field)?
- What is the difference between `$push` and `$addToSet` accumulators?
- Why can `$group` be memory-intensive on large datasets, and how does `allowDiskUse` help?

### Sort

The `$sort` stage orders documents by one or more fields within the pipeline. Placing `$sort` before a `$limit` lets MongoDB optimize using a top-K sort algorithm, and placing it early enough may allow it to use an index.

```javascript
{ $sort: { totalSpent: -1 } }
```

**Interview Questions:**
- How does combining `$sort` immediately followed by `$limit` improve performance?
- When can `$sort` in an aggregation pipeline use an index versus requiring an in-memory sort?

### Limit

`$limit` restricts the number of documents passed to the next stage, commonly used with `$sort` for top-N queries or with `$skip` for pagination.

```javascript
{ $limit: 10 }
```

**Interview Questions:**
- Why is `{ $sort } { $limit }` more efficient than reversing the stage order?

### Skip

`$skip` bypasses a specified number of documents before passing the rest along the pipeline, typically used for pagination. Like the `skip()` cursor method, it still requires MongoDB to walk over the skipped documents, so it degrades for large offsets.

```javascript
{ $skip: 100 }
```

**Interview Questions:**
- Why is `$skip` inefficient for deep pagination on large collections?
- What alternative pagination strategy avoids the cost of `$skip`?

### Unwind

`$unwind` deconstructs an array field, producing one output document per array element (each output document is a copy of the original with the array field replaced by a single element). It's essential for performing per-element analysis or joins on array data.

```javascript
db.orders.aggregate([
  { $unwind: "$items" },
  { $group: { _id: "$items.sku", totalQty: { $sum: "$items.qty" } } }
]);
```

**Interview Questions:**
- What happens to a document if the array field being unwound is empty or missing?
- How does `preserveNullAndEmptyArrays` change `$unwind`'s default behavior?
- Why might `$unwind` followed by `$group` be used together in real reporting pipelines?

### Lookup

`$lookup` performs a left outer join against another collection in the same database, matching a local field to a foreign field (or using a more flexible sub-pipeline form for complex join conditions), attaching matched documents as an array field.

```javascript
db.orders.aggregate([
  {
    $lookup: {
      from: "customers",
      localField: "customerId",
      foreignField: "_id",
      as: "customer"
    }
  },
  { $unwind: "$customer" }
]);
```

**Advantages:**
- Enables relational-style joins without denormalizing data
- The pipeline form supports complex, multi-condition joins

**Disadvantages:**
- Can be slow on large foreign collections without an index on the `foreignField`
- Adds latency compared to embedding related data directly

**Interview Questions:**
- What index should exist on the foreign collection to make `$lookup` efficient?
- How does the pipeline-based form of `$lookup` differ from the simple `localField`/`foreignField` form?
- When would you choose embedding data over using `$lookup`?

### Facet

`$facet` runs multiple independent aggregation sub-pipelines within a single stage against the same input documents, returning all results together — useful for building a single response containing several different views of the data (e.g., paginated results plus a total count plus category breakdowns) in one round trip.

```javascript
db.products.aggregate([
  {
    $facet: {
      paginatedResults: [{ $skip: 0 }, { $limit: 10 }],
      totalCount: [{ $count: "count" }],
      byCategory: [{ $group: { _id: "$category", count: { $sum: 1 } } }]
    }
  }
]);
```

**Interview Questions:**
- What problem does `$facet` solve compared to running multiple separate aggregation queries?
- Are the sub-pipelines inside `$facet` executed on the original input documents or on each other's output?

### Bucket

`$bucket` groups documents into discrete ranges ("buckets") based on a specified expression and boundary values, similar to a histogram — useful for reporting distributions like price ranges or age groups. `$bucketAuto` is a related stage that automatically computes boundaries for a target number of buckets.

```javascript
db.products.aggregate([
  {
    $bucket: {
      groupBy: "$price",
      boundaries: [0, 50, 100, 200, 500],
      default: "500+",
      output: { count: { $sum: 1 } }
    }
  }
]);
```

**Interview Questions:**
- What is the difference between `$bucket` and `$bucketAuto`?
- What happens to a document whose value falls outside all defined boundaries?

### Merge

`$merge` writes the aggregation pipeline's results into a specified collection (which can be a new or existing one, even in another database), supporting insert, replace, merge, or fail behaviors for conflicting documents — enabling incremental materialized views.

```javascript
db.orders.aggregate([
  { $group: { _id: "$customerId", total: { $sum: "$amount" } } },
  { $merge: { into: "customerTotals", whenMatched: "replace", whenNotMatched: "insert" } }
]);
```

**Differences:**

| Aspect | `$merge` | `$out` |
|---|---|---|
| Target collection | Can already contain data; merges/updates | Fully replaced |
| Conflict handling | Configurable (`whenMatched`/`whenNotMatched`) | N/A, full overwrite |
| Use case | Incremental materialized views | One-time snapshot/full refresh |

**Interview Questions:**
- How does `$merge` differ from `$out`?
- What options control how `$merge` handles documents that already exist in the target collection?
- What is a practical use case for `$merge` (e.g., incrementally maintained materialized views)?

### Out

`$out` writes the aggregation results to a collection, completely replacing its previous contents (or creating it if it doesn't exist). It must be the last stage in the pipeline.

```javascript
db.orders.aggregate([
  { $group: { _id: "$customerId", total: { $sum: "$amount" } } },
  { $out: "customerTotalsSnapshot" }
]);
```

**Interview Questions:**
- Why must `$out` be the final stage of a pipeline?
- What happens to the target collection's existing indexes when `$out` replaces its contents?

### Aggregation Pipeline Optimization

MongoDB automatically applies certain pipeline optimizations (e.g., merging adjacent `$match`/`$sort` stages, pushing `$match`/`$project` earlier when semantically safe, coalescing `$limit` into `$sort`). Developers can further optimize by placing `$match`/`$limit` early, projecting out unneeded fields before expensive stages, ensuring `$match`/`$sort` fields are indexed, and using `allowDiskUse: true` only when unavoidable for large `$group`/`$sort` operations.

```javascript
db.orders.aggregate(pipeline, { allowDiskUse: true });
```

**Interview Questions:**
- What automatic optimizations does the aggregation engine perform on pipeline stages?
- When would you need to enable `allowDiskUse`, and what is the tradeoff of doing so?
- How would you use `explain()` on an aggregation pipeline to identify bottlenecks?

### Window Functions

Window functions, exposed via the `$setWindowFields` stage, compute values across a "window" of related documents (partitioned and ordered, similar to SQL window functions) without collapsing them into groups the way `$group` does — enabling running totals, moving averages, and rankings while preserving each original document.

```javascript
db.sales.aggregate([
  {
    $setWindowFields: {
      partitionBy: "$region",
      sortBy: { date: 1 },
      output: {
        runningTotal: { $sum: "$amount", window: { documents: ["unbounded", "current"] } }
      }
    }
  }
]);
```

**Differences:**

| Aspect | `$group` | `$setWindowFields` |
|---|---|---|
| Output | One document per group | One document per input document, enriched |
| Preserves original rows | No | Yes |
| Typical use | Aggregated summaries | Running totals, moving averages, ranks |

**Interview Questions:**
- How does `$setWindowFields` differ from `$group` in terms of output shape?
- How would you compute a 7-day moving average using window functions?
- What does `partitionBy` control in `$setWindowFields`?

## Transactions

### Single Document Atomicity

MongoDB guarantees that all changes within a single document — including nested arrays and subdocuments — are applied atomically, even across multiple fields. This built-in atomicity is why well-designed document models (embedding related data in one document) often avoid the need for multi-document transactions entirely.

```javascript
db.accounts.updateOne(
  { _id: "acc1" },
  { $inc: { balance: -100 }, $push: { history: { type: "DEBIT", amount: 100 } } }
);
```

**Interview Questions:**
- Why is single-document atomicity considered a core design principle in MongoDB's data modeling philosophy?
- How can embedding data to leverage single-document atomicity reduce the need for multi-document transactions?

### Multi-Document Transactions

Multi-document transactions (available since MongoDB 4.0 for replica sets, and 4.2 for sharded clusters) allow multiple read/write operations across multiple documents, collections, or even databases to be grouped into a single all-or-nothing unit, with snapshot isolation. They're used when operations spanning several documents must all succeed or all fail together, such as transferring funds between two account documents.

```javascript
const session = client.startSession();
try {
  session.startTransaction();
  accounts.updateOne({ _id: "acc1" }, { $inc: { balance: -100 } }, { session });
  accounts.updateOne({ _id: "acc2" }, { $inc: { balance: 100 } }, { session });
  session.commitTransaction();
} catch (e) {
  session.abortTransaction();
  throw e;
} finally {
  session.endSession();
}
```

```java
@Transactional
public void transferFunds(String fromId, String toId, BigDecimal amount) {
    mongoTemplate.updateFirst(query(where("_id").is(fromId)),
        new Update().inc("balance", amount.negate()), Account.class);
    mongoTemplate.updateFirst(query(where("_id").is(toId)),
        new Update().inc("balance", amount), Account.class);
}
```

**Advantages:**
- Guarantees atomicity/consistency across multiple documents and collections
- Simplifies application logic for genuinely multi-document invariants

**Disadvantages:**
- Higher latency and resource cost than single-document operations
- Long-running transactions can hold locks/snapshots and impact throughput

**Interview Questions:**
- When would you reach for a multi-document transaction instead of relying on document embedding?
- What is snapshot isolation, and how does it apply to MongoDB transactions?
- How would you implement a multi-document transaction using Spring Data MongoDB's `@Transactional`?

### ACID Properties

MongoDB transactions provide the classic ACID guarantees: **Atomicity** (all operations in the transaction succeed or none do), **Consistency** (the database moves from one valid state to another), **Isolation** (concurrent transactions don't see each other's uncommitted changes, via snapshot isolation), and **Durability** (committed changes survive failures, backed by the write-ahead journal and replication).

**Interview Questions:**
- How does MongoDB implement isolation for concurrent transactions?
- How does write concern relate to the durability guarantee of a committed transaction?
- Can you explain atomicity in the context of a MongoDB multi-document transaction with an example?

### Transaction Lifecycle

A transaction moves through a defined lifecycle: start a client session, start the transaction, execute one or more operations against that session, then either commit (making all changes visible/durable) or abort (rolling back all changes). Retryable logic is often layered on top to handle transient errors like `TransientTransactionError`.

```mermaid
sequenceDiagram
    participant App
    participant Session
    participant MongoDB
    App->>Session: startSession()
    App->>Session: startTransaction()
    App->>MongoDB: op1 (with session)
    App->>MongoDB: op2 (with session)
    alt success
        App->>Session: commitTransaction()
        Session->>MongoDB: apply changes durably
    else error
        App->>Session: abortTransaction()
        Session->>MongoDB: discard changes
    end
```

**Interview Questions:**
- What are the distinct phases of a MongoDB transaction's lifecycle?
- What kinds of errors should trigger a transaction retry versus an abort?
- Why is a client session required before starting a transaction?

### Transaction Limitations

MongoDB transactions have practical constraints: a default 60-second maximum runtime, restrictions on certain DDL operations (e.g., creating collections/indexes inside a transaction, though this improved in later versions), increased oplog/cache pressure for large transactions, and a general recommendation to keep transactions short and touch a limited number of documents.

**Disadvantages:**
- Not designed for bulk/batch operations touching very large numbers of documents
- Can increase contention and WiredTiger cache pressure if overused

**Interview Questions:**
- What is the default time limit for a MongoDB transaction, and what happens if it's exceeded?
- Why are multi-document transactions discouraged for large bulk-update workloads?
- What operations were historically restricted from running inside a transaction?

### Retryable Writes

Retryable writes automatically retry a single write operation (insert, update, delete, findAndModify) once if it fails due to a transient network error or replica set election, using a unique transaction number to ensure the operation isn't applied twice. This is enabled by default in modern drivers and improves resilience during failovers without requiring application-level retry logic for simple writes.

```javascript
// Enabled by default via connection string option
mongodb://host1,host2,host3/?retryWrites=true
```

**Differences:**

| Aspect | Retryable Writes | Multi-Document Transactions |
|---|---|---|
| Scope | Single write operation | Multiple operations/documents |
| Purpose | Resilience against transient failures | Atomicity across multiple operations |
| Overhead | Low | Higher (snapshot, locks) |

**Interview Questions:**
- How does MongoDB avoid applying a retried write twice?
- What kinds of failures trigger a retryable write to automatically retry?
- How do retryable writes differ from multi-document transactions in purpose and scope?

## Concurrency

### Document-Level Locking

MongoDB (with the WiredTiger storage engine) uses document-level concurrency control, meaning writes to different documents can proceed fully in parallel, and only the specific document being modified is locked for the duration of that operation. This is far more granular than older collection-level or database-level locking schemes.

```mermaid
flowchart LR
    W1[Write to Doc A] -->|locks only Doc A| DocA[(Document A)]
    W2[Write to Doc B] -->|locks only Doc B, runs concurrently| DocB[(Document B)]
```

**Advantages:**
- High write concurrency since unrelated documents never block each other
- Scales well for workloads with many independent documents

**Interview Questions:**
- What granularity of locking does WiredTiger use for writes?
- How did document-level locking improve on MongoDB's older MMAPv1 storage engine locking model?
- Can two concurrent writes to the same document proceed simultaneously?

### Optimistic Concurrency Concepts

Optimistic concurrency assumes conflicts are rare and lets operations proceed without locking ahead of time, detecting conflicts only at write time — typically implemented via a version field checked with a conditional update (compare-and-swap). If another process modified the document first, the conditional update matches zero documents and the application retries.

```java
@Version
private Long version; // Spring Data MongoDB optimistic locking field
```

```javascript
db.docs.updateOne(
  { _id: id, version: currentVersion },
  { $set: { value: newValue }, $inc: { version: 1 } }
);
// if matchedCount === 0, a concurrent update won the race — retry
```

**Differences:**

| Aspect | Optimistic Concurrency | Pessimistic Concurrency |
|---|---|---|
| Locking strategy | No upfront lock; detect conflicts at write time | Acquire lock before reading/modifying |
| Throughput | High under low contention | Lower due to blocking |
| Conflict handling | Retry on version mismatch | Prevented by blocking other readers/writers |
| MongoDB native support | Common pattern via version field + conditional update | Achieved via external locks or `findAndModify` |

**Interview Questions:**
- How would you implement optimistic concurrency control in MongoDB using a version field?
- What should an application do when an optimistic update fails due to a version mismatch?
- Why is optimistic concurrency generally preferred in high-throughput, low-contention systems?

### Atomic Operations

MongoDB provides several atomic single-document operations — `findAndModify`/`findOneAndUpdate`, update operators (`$inc`, `$set`, `$push`, etc.), and array operators — which are guaranteed to apply completely or not at all, and can be used to implement counters, queues, or state machines without a full transaction.

```javascript
db.counters.findOneAndUpdate(
  { _id: "orderSeq" },
  { $inc: { seq: 1 } },
  { returnDocument: "after", upsert: true }
);
```

**Interview Questions:**
- How can `findOneAndUpdate` with `$inc` be used to implement an atomic counter/sequence generator?
- Why are atomic update operators preferable to a read-modify-write pattern in application code?

### Write Conflicts

A write conflict occurs when two concurrent operations (often within transactions) attempt to modify the same document at the same time; MongoDB's WiredTiger engine detects this and aborts one of the operations with a `WriteConflict` error, which the driver/application should retry.

```javascript
try {
  session.startTransaction();
  // ... conflicting update ...
  session.commitTransaction();
} catch (e) {
  if (e.hasErrorLabel("TransientTransactionError")) {
    // retry the whole transaction
  }
}
```

**Interview Questions:**
- What causes a `WriteConflict` error in MongoDB, and which storage engine component detects it?
- How should an application respond when it receives a `TransientTransactionError`?
- How do write conflicts relate to the isolation guarantees of multi-document transactions?

## Replication

### Replica Sets

A replica set is a group of MongoDB servers (mongod processes) that maintain the same data set, providing high availability and data redundancy. One member is elected Primary and accepts all writes; the others are Secondaries that replicate the Primary's oplog and can serve reads. If the Primary becomes unavailable, the remaining members automatically hold an election to choose a new Primary.

```mermaid
flowchart TD
    P((Primary)) -->|replicates oplog| S1((Secondary 1))
    P -->|replicates oplog| S2((Secondary 2))
    Client -->|writes| P
    Client -.->|optional reads| S1
    Client -.->|optional reads| S2
```

```javascript
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "mongo1:27017" },
    { _id: 1, host: "mongo2:27017" },
    { _id: 2, host: "mongo3:27017" }
  ]
});
```

**Advantages:**
- Automatic failover with no manual intervention required
- Data redundancy across multiple nodes protects against hardware failure
- Can offload read traffic to secondaries when eventual consistency is acceptable

**Disadvantages:**
- Secondary reads may return stale data (eventual consistency) unless using majority read concern with appropriate settings
- Requires at least 3 nodes (or a primary + secondary + arbiter) for robust automatic failover

**Interview Questions:**
- What is the minimum recommended replica set topology for automatic failover, and why?
- What happens to in-flight writes on the Primary if it suddenly becomes unreachable?
- How does a replica set differ from a sharded cluster in terms of what problem it solves?

### Primary

The Primary is the single replica set member that receives all write operations at any given time. All writes are recorded to its oplog, which secondaries then pull and replay to stay in sync. Only one node can be Primary at a time within a replica set.

**Interview Questions:**
- Can a replica set have more than one Primary at the same time under normal operation?
- What happens to write requests sent to a node that is not currently Primary?

### Secondary

Secondaries replicate data from the Primary by continuously applying operations from its oplog, keeping their data near real-time consistent with the Primary. They can serve read traffic if the client's read preference allows it, and are also eligible to be elected Primary if the current Primary fails.

**Differences:**

| Aspect | Primary | Secondary |
|---|---|---|
| Accepts writes | Yes | No (by default) |
| Serves reads | Yes (default) | Only if read preference allows |
| Election eligibility | N/A (already primary) | Yes (unless configured non-electable) |
| Data source | Client writes | Replicates Primary's oplog |

**Interview Questions:**
- Can a Secondary accept write operations directly from a client?
- How would you configure a Secondary to never become Primary (e.g., for a dedicated analytics/backup node)?
- What read preference would you use to allow reads from Secondaries?

### Automatic Failover

When the Primary becomes unreachable (crash, network partition, planned maintenance), the remaining eligible replica set members automatically detect the failure via heartbeats and initiate an election to choose a new Primary, typically completing within a few seconds, minimizing write downtime.

```mermaid
sequenceDiagram
    participant S1 as Secondary 1
    participant S2 as Secondary 2
    participant P as Primary
    P--xS1: heartbeat timeout
    P--xS2: heartbeat timeout
    S1->>S2: request votes
    S2-->>S1: vote granted
    Note over S1: Elected new Primary
```

**Interview Questions:**
- What mechanism does a replica set use to detect that the Primary is down?
- Roughly how long does automatic failover typically take, and what happens to writes during that window?
- Can automatic failover be disabled or delayed for specific members (e.g., a reporting-only secondary)?

### Elections

An election is the process by which replica set members vote to select a new Primary, based on factors like member priority, most recent oplog timestamp (most up-to-date data wins), and the number of votes each member has. A majority of voting members must be reachable and agree for an election to succeed, which is why an odd number of voting members (or an arbiter) is recommended.

```javascript
cfg = rs.conf();
cfg.members[0].priority = 2; // prefer this member as Primary
rs.reconfig(cfg);
```

**Interview Questions:**
- What factors influence which member wins an election to become Primary?
- Why is it important to have an odd number of voting members in a replica set?
- How does member `priority` influence election outcomes?

### Read Preference

Read preference determines which replica set members (Primary and/or Secondaries) a client will route read operations to. Modes include `primary` (default, strongest consistency), `primaryPreferred`, `secondary`, `secondaryPreferred`, and `nearest` (lowest network latency). Choosing a secondary-based mode trades consistency for read scalability or geographic latency reduction.

```javascript
db.orders.find().readPref("secondaryPreferred");
```

```java
mongoTemplate.getDb().withCodecRegistry(...); // in Spring, configure via ReadPreference on MongoClientSettings
```

**Differences:**

| Mode | Reads From | Consistency |
|---|---|---|
| `primary` | Primary only | Strongly consistent |
| `secondaryPreferred` | Secondary if available, else Primary | Possibly stale |
| `nearest` | Lowest-latency member | Possibly stale |

**Interview Questions:**
- What is the tradeoff of using `secondaryPreferred` read preference?
- Why might `nearest` be chosen for a globally distributed application?
- How does read preference interact with read concern to determine overall consistency?

### Write Concern

Write concern specifies the level of acknowledgment MongoDB requires from replica set members before considering a write operation successful, expressed as `w` (number of nodes, or `"majority"`), `j` (journal acknowledgment), and `wtimeout`. Stronger write concerns (e.g., `w: "majority"`) increase durability guarantees at the cost of latency.

```javascript
db.orders.insertOne(
  { customerId: "c1", amount: 250 },
  { writeConcern: { w: "majority", j: true, wtimeout: 5000 } }
);
```

**Advantages:**
- `w: "majority"` protects against data loss from a subsequent failover/rollback
- Tunable per-operation to balance latency vs durability

**Disadvantages:**
- Higher write concern levels increase write latency, especially across geographically distributed replica sets

**Interview Questions:**
- What does `w: "majority"` guarantee that `w: 1` does not?
- What is the role of the `j` (journal) option in write concern?
- Why might an application use a weaker write concern for non-critical writes (e.g., logging)?

### Read Concern

Read concern controls the consistency/isolation guarantees of data returned by a read operation, with levels such as `local` (default, may return data that could later be rolled back), `available`, `majority` (only data acknowledged by a majority of nodes, safe from rollback), `linearizable`, and `snapshot` (used with transactions).

```javascript
db.orders.find({ status: "SHIPPED" }).readConcern("majority");
```

**Differences:**

| Level | Guarantee |
|---|---|
| `local` | Latest data on that node, may be rolled back later |
| `majority` | Data acknowledged by a majority, won't be rolled back |
| `linearizable` | Strongest; reflects all previously completed majority writes |

**Interview Questions:**
- Why could data returned under `local` read concern later be "rolled back"?
- When would you use `majority` read concern together with `majority` write concern?
- What is the difference between `majority` and `linearizable` read concern?

### Oplog

The oplog (operations log) is a special capped collection on each replica set member (`local.oplog.rs`) that records every write operation applied to the Primary, in idempotent form. Secondaries continuously tail and replay this log to stay synchronized. Its size determines the "replication window" — how far behind a secondary can fall before it can no longer catch up via normal replication.

```javascript
db.getReplicationInfo(); // shows oplog size and time window
rs.printReplicationInfo();
```

**Interview Questions:**
- Why must operations recorded in the oplog be idempotent?
- What happens if a Secondary falls behind further than the oplog's retention window?
- How would you resize the oplog on a running replica set member?

### Arbiter Nodes

An arbiter is a replica set member that participates in elections (casting a vote) but does not hold a copy of the data and cannot become Primary. Arbiters are useful for achieving an odd number of voting members (breaking election ties) without the cost of an additional full data-bearing node.

```javascript
rs.addArb("mongo4:27017");
```

**Advantages:**
- Lightweight way to maintain odd vote count without extra storage/replication cost

**Disadvantages:**
- Cannot serve reads or hold data, so it doesn't add redundancy for data durability
- MongoDB generally recommends data-bearing nodes over arbiters where resources allow, since arbiters can complicate majority write-concern calculations

**Differences:**

| Aspect | Data-Bearing Secondary | Arbiter |
|---|---|---|
| Holds data | Yes | No |
| Can become Primary | Yes | No |
| Participates in elections | Yes | Yes |
| Resource cost | Full (storage, replication) | Minimal |

**Interview Questions:**
- What is the main purpose of adding an arbiter to a replica set?
- Why might MongoDB's documentation recommend avoiding arbiters in favor of additional data-bearing nodes when possible?
- Can an arbiter ever be elected Primary?

## Sharding

### Sharding Concepts

Sharding is MongoDB's horizontal scaling strategy that partitions a collection's data across multiple servers (shards) so that no single node has to hold the entire dataset or absorb all read/write load. A sharded cluster consists of shards (which store the data, typically as replica sets), config servers (which store cluster metadata), and `mongos` routers (which route client requests to the correct shards). Sharding is transparent to the application: queries are issued the same way whether the collection is sharded or not.

Use sharding when a single replica set can no longer handle the working set size, storage capacity, or throughput requirements of a workload — for example, an e-commerce order history collection growing into billions of documents where a single primary can no longer serve write traffic fast enough.

```mermaid
flowchart LR
    Client -->|query| Mongos[mongos Router]
    Mongos --> Config[(Config Servers)]
    Mongos --> Shard1[(Shard 1 - Replica Set)]
    Mongos --> Shard2[(Shard 2 - Replica Set)]
    Mongos --> Shard3[(Shard 3 - Replica Set)]
```

**Advantages:**
- Scales writes and storage horizontally across many machines
- Enables geographic data distribution via zone sharding
- Individual shards can be replica sets, preserving high availability

**Disadvantages:**
- Adds significant operational complexity (config servers, routers, balancer)
- Cross-shard queries and transactions are more expensive
- Poor shard key choice can lead to unbalanced clusters that are hard to fix later

**Interview Questions:**
- What problem does sharding solve that replication alone cannot?
- What are the three main components of a sharded cluster and what does each do?
- What happens to a query that doesn't include the shard key?
- How does sharding interact with replica sets within each shard?

### Shard Key

The shard key is the field (or combination of fields) used to determine how MongoDB distributes documents across shards. It is immutable for the collection's lifetime (in older versions) and is chosen at the time a collection is sharded via `sh.shardCollection()`. MongoDB uses the shard key values to compute chunk ranges (for ranged sharding) or hashed values (for hashed sharding) that determine which shard owns a given document.

```javascript
// Enable sharding on a database and shard a collection using a compound shard key
sh.enableSharding("ecommerceDb")
sh.shardCollection("ecommerceDb.orders", { customerId: 1, orderDate: 1 })
```

**Advantages:**
- Well-chosen keys enable even data and query distribution
- Compound shard keys can support both range queries and cardinality

**Disadvantages:**
- Poorly chosen keys cause "hot shards" or jumbo chunks
- Changing a shard key historically required re-sharding (dropping and reloading data), though newer MongoDB versions support limited shard key refinement

**Interview Questions:**
- What makes a good shard key in terms of cardinality, frequency, and monotonicity?
- What is a "hot shard" and how does shard key choice cause it?
- Can you change a shard key after a collection is sharded?
- How does a compound shard key affect query routing compared to a single-field key?

### Config Servers

Config servers store the sharded cluster's metadata, including the mapping of chunks to shards, shard cluster authentication settings, and cluster-wide configuration. In production, config servers are deployed as a dedicated replica set (CSRS - Config Server Replica Set), ensuring the metadata itself is highly available. Every `mongos` instance reads from and caches this metadata to route requests efficiently.

```mermaid
flowchart TD
    CS1[Config Server 1 - Primary] --- CS2[Config Server 2]
    CS1 --- CS3[Config Server 3]
    Mongos1[mongos] -->|reads metadata| CS1
    Mongos2[mongos] -->|reads metadata| CS1
```

**Advantages:**
- Centralizes cluster metadata, enabling consistent routing across many `mongos` instances
- Being a replica set itself, it tolerates node failures

**Disadvantages:**
- If the config server replica set is unavailable, chunk migrations and metadata updates halt (though reads/writes with cached metadata can often continue briefly)

**Interview Questions:**
- What metadata do config servers store?
- Why are config servers deployed as a replica set rather than a single node?
- What happens to a sharded cluster if the config server replica set becomes unavailable?

### Mongos Router

`mongos` acts as a query router that sits between client applications and the sharded cluster. It has no persistent data of its own — it fetches and caches cluster metadata from the config servers, then routes each incoming query or write to the appropriate shard(s), merging results as needed (for example, sorting or aggregating results scattered across shards). Applications connect to `mongos` exactly as they would to a single `mongod` instance.

A typical production deployment runs multiple `mongos` instances (often co-located with application servers) behind the driver's connection pooling, so no single router process is a bottleneck or single point of failure.

**Advantages:**
- Transparent routing means application code doesn't need to know about sharding
- Stateless, so multiple instances can be run for redundancy and load distribution

**Disadvantages:**
- Adds an extra network hop compared to talking directly to a `mongod`
- Scatter-gather queries (without shard key) must fan out to all shards, which is slower

**Interview Questions:**
- What is the role of `mongos` in a sharded cluster?
- Why is `mongos` considered stateless, and what does it cache?
- What is a scatter-gather query and why is it less efficient than a targeted query?

### Chunk Migration

Data in a sharded collection is divided into chunks — contiguous ranges of shard key values (or hash buckets). As data grows or shrinks unevenly, the balancer process moves chunks between shards to keep the data distribution even. During a migration, the source shard continues serving reads/writes for the chunk until the destination shard has fully copied the data and the metadata is atomically updated on the config servers.

```mermaid
sequenceDiagram
    participant Balancer
    participant Source as Source Shard
    participant Dest as Destination Shard
    participant Config as Config Servers
    Balancer->>Dest: Start chunk clone
    Dest->>Source: Copy documents in chunk range
    Source-->>Dest: Stream data + oplog changes
    Balancer->>Config: Commit new chunk ownership
    Config-->>Balancer: Acknowledge metadata update
    Balancer->>Source: Delete migrated chunk range
```

**Advantages:**
- Keeps data evenly distributed automatically without manual intervention
- Migrations are designed to be non-blocking for normal operations

**Disadvantages:**
- Migrations consume extra CPU, memory, and I/O, which can affect performance during peak load
- Can be paused/scheduled to avoid business-critical hours

**Interview Questions:**
- What triggers a chunk migration?
- How does MongoDB ensure data consistency during a chunk migration?
- Can chunk migrations be scheduled or throttled, and why would you want that?

### Balancer

The balancer is a background process (running as part of the config server replica set primary in modern MongoDB versions) that monitors the distribution of chunks across shards and automatically migrates chunks from over-loaded shards to under-loaded ones to maintain balance. It can be enabled/disabled cluster-wide or per-collection, and its activity can be restricted to specific time windows to avoid impacting peak traffic.

```javascript
// Check balancer status
sh.getBalancerState()

// Disable the balancer temporarily (e.g. before a maintenance window)
sh.stopBalancer()

// Set a balancing window (only run between 23:00 and 06:00)
db.settings.updateOne(
  { _id: "balancer" },
  { $set: { activeWindow: { start: "23:00", stop: "06:00" } } },
  { upsert: true }
)
```

**Advantages:**
- Automates cluster-wide data balancing with no manual chunk management
- Configurable scheduling minimizes impact on production traffic

**Disadvantages:**
- Uncontrolled balancing can compete with application workload for resources
- Imbalanced shard keys can cause the balancer to run constantly without achieving good balance

**Interview Questions:**
- What is the balancer responsible for and where does it run?
- How would you prevent the balancer from running during business hours?
- What symptoms would indicate the balancer is struggling to keep a cluster balanced?

### Choosing a Shard Key

Selecting a shard key is one of the most consequential decisions in a sharded cluster because it directly determines query targeting, write distribution, and how easily the cluster scales. Good shard keys have high cardinality (many possible distinct values), even frequency (no single value dominates), and low monotonicity (avoiding always-increasing values like timestamps or auto-incrementing IDs, which concentrate writes on one shard). Compound shard keys are often used to balance these properties, and hashed shard keys can be used to avoid monotonic write hot-spotting at the cost of losing efficient range queries.

For example, sharding an `orders` collection purely on an ever-increasing `orderDate` would send all new writes to a single "hot" shard; using a hashed `customerId` or a compound key like `{ region: 1, orderId: 1 }` spreads writes more evenly.

**Differences:**

| Shard Key Type | Write Distribution | Range Query Support |
|---|---|---|
| Ranged (ascending, e.g. `_id`) | Poor (hot shard) | Excellent |
| Hashed | Excellent (even) | Poor (requires scatter-gather) |
| Compound (low + high cardinality) | Good | Good, if prefix field is used in query |

**Interview Questions:**
- Why is a monotonically increasing field usually a poor shard key choice?
- What is the trade-off between hashed and ranged shard keys?
- How do you evaluate cardinality and frequency when selecting a shard key?
- How would you shard a collection to support both even write distribution and efficient range queries?

### Zone Sharding

Zone sharding (formerly called tag-aware sharding) lets you associate ranges of shard key values with specific zones, and then assign zones to specific shards. This is commonly used to keep data physically located near its users (data locality/compliance, e.g. keeping EU customer data on shards hosted in EU data centers) or to isolate specific workloads (e.g. archiving old data onto cheaper hardware).

```javascript
// Define a zone and associate it with a shard
sh.addShardTag("shard0000", "EU")
sh.addShardTag("shard0001", "US")

// Assign shard key ranges to zones
sh.addTagRange(
  "ecommerceDb.orders",
  { region: "EU", orderId: MinKey },
  { region: "EU", orderId: MaxKey },
  "EU"
)
```

**Advantages:**
- Enables data residency/compliance requirements (e.g. GDPR) at the infrastructure level
- Supports tiered storage strategies (hot vs. cold hardware)

**Disadvantages:**
- Adds configuration complexity and requires careful shard key design that includes the zoning field
- Misconfigured zones can create unintended hot spots

**Interview Questions:**
- What is zone sharding used for in a real-world multi-region deployment?
- How does zone sharding relate to shard key design?
- Give an example of a compliance requirement that zone sharding could help satisfy.

## Consistency and Availability

### CAP Theorem

The CAP theorem states that a distributed data system can only guarantee two out of three properties at any given moment when a network partition occurs: Consistency (every read receives the most recent write), Availability (every request receives a response, without guaranteeing it's the latest), and Partition tolerance (the system continues operating despite network failures between nodes). Since network partitions are unavoidable in real distributed systems, the practical choice is between consistency and availability during a partition.

MongoDB is generally classified as a CP system: by default, it favors consistency by routing all writes through a single primary and only exposing acknowledged, replicated data as durable, rather than allowing every replica to accept writes independently. However, its tunable read/write concerns and read preferences let application developers shift the balance toward availability when appropriate.

```mermaid
flowchart TD
    A[Network Partition Occurs] --> B{Choose 2 of 3}
    B --> C[Consistency + Partition Tolerance = CP]
    B --> D[Availability + Partition Tolerance = AP]
    C --> E["MongoDB default: single primary, majority writes"]
    D --> F["e.g. reading from secondaries during primary election"]
```

**Interview Questions:**
- Explain the CAP theorem in your own words and how it applies to MongoDB.
- Why is MongoDB typically categorized as CP rather than AP?
- How can you tune MongoDB's behavior to favor availability over strict consistency?
- What happens to write availability during a primary election in a MongoDB replica set?

### Eventual Consistency

Eventual consistency means that if no new writes occur, all replicas of a piece of data will eventually converge to the same value, but reads immediately after a write are not guaranteed to reflect it everywhere. In MongoDB, secondary reads (when using non-primary read preferences) are eventually consistent because secondaries apply oplog entries asynchronously and may lag behind the primary (replication lag).

A common real-world use case is offloading reporting or analytics queries to secondary nodes to reduce load on the primary, accepting that the data might be a few seconds stale.

```javascript
// Explicitly allow eventually consistent reads from secondaries
db.orders.find({ status: "shipped" }).readPref("secondary")
```

**Advantages:**
- Improves read scalability and reduces load on the primary
- Keeps the system available even if some nodes are behind

**Disadvantages:**
- Application must tolerate stale reads, which is unsuitable for strongly consistent needs like financial balances
- Replication lag can vary unpredictably under heavy write load

**Interview Questions:**
- What causes eventual consistency in a MongoDB replica set?
- In what scenarios is eventual consistency an acceptable trade-off?
- How would you detect and monitor replication lag?

### Read Preference

Read preference determines which member of a replica set (primary or secondaries) handles a given read operation. MongoDB supports five modes: `primary` (default, strongest consistency), `primaryPreferred`, `secondary`, `secondaryPreferred`, and `nearest` (lowest network latency, regardless of role). Read preference is set per-query, per-connection, or per-client, giving fine-grained control over the consistency/availability/latency trade-off.

```javascript
// Route analytics-style reads to secondaries to reduce primary load
db.orders.find({ region: "EU" }).readPref("secondaryPreferred")
```

```java
// Spring Data MongoDB: setting read preference via MongoTemplate
mongoTemplate.setReadPreference(ReadPreference.secondaryPreferred());
```

**Interview Questions:**
- What are the five read preference modes and when would you use each?
- What is the risk of using `secondary` read preference for critical reads?
- How does `nearest` differ from `secondaryPreferred` in terms of node selection?

### Read Concern

Read concern controls the consistency guarantee of the data returned by a read operation, independent of which node serves it. Common levels include `local` (returns whatever data is on the queried node, no guarantee it's been replicated), `available`, `majority` (only returns data acknowledged by a majority of replica set members, guaranteeing it won't be rolled back), `linearizable` (strongest, guarantees the read reflects all previously completed majority-acknowledged writes), and `snapshot` (used with multi-document transactions).

```javascript
// Read only majority-committed data (won't be rolled back)
db.orders.find({ status: "paid" }).readConcern("majority")
```

**Differences:**

| Read Concern | Guarantee | Typical Use Case |
|---|---|---|
| `local` | Fastest, may return uncommitted/rollback-able data | Default, non-critical reads |
| `majority` | Data acknowledged by majority of replica set | Critical reads that must survive failover |
| `linearizable` | Reflects all prior majority writes, single-document only | Strongest consistency needs |
| `snapshot` | Consistent point-in-time view | Multi-document transactions |

**Interview Questions:**
- What is the difference between `local` and `majority` read concern?
- Why would `linearizable` read concern be slower than `majority`?
- When is `snapshot` read concern used?

### Write Concern

Write concern specifies the level of acknowledgment MongoDB requires from replica set members before considering a write operation successful. It's expressed as `w` (number of nodes, or `"majority"`), `j` (whether the write must be committed to the on-disk journal), and `wtimeout` (how long to wait before giving up). Choosing a stronger write concern increases durability guarantees at the cost of latency.

```javascript
// Require acknowledgment from a majority of voting members, with journal commit
db.orders.insertOne(
  { customerId: "C123", total: 99.99 },
  { writeConcern: { w: "majority", j: true, wtimeout: 5000 } }
)
```

**Advantages:**
- `w: "majority"` protects against data loss from primary failover (rollback)
- Tunable per-operation, so critical writes can be stronger than bulk/log writes

**Disadvantages:**
- Higher write concerns increase write latency
- `wtimeout` too low can cause spurious failures under load; too high can hang requests

**Interview Questions:**
- What does `w: "majority"` protect against that `w: 1` does not?
- What role does the `j` option play in write concern?
- How would you choose write concern differently for an audit log vs. a financial transaction?

### Causal Consistency

Causal consistency guarantees that operations that are causally related (e.g. a write followed by a dependent read) are observed by every node in the correct order, even when reading from secondaries. MongoDB implements this via causally consistent sessions (`ClientSession`), which track logical timestamps (`operationTime`) and pass them between operations so subsequent reads wait until the required data has replicated.

A real-world scenario: a user updates their profile and immediately reloads the page — causal consistency ensures that even if the read is served from a secondary, it will reflect the just-completed write rather than stale data.

```javascript
const session = db.getMongo().startSession({ causalConsistency: true })
const orders = session.getDatabase("ecommerceDb").orders
orders.insertOne({ customerId: "C123", total: 49.99 })
// Subsequent reads within this session are guaranteed to see the insert above
orders.find({ customerId: "C123" }).readPref("secondary")
session.endSession()
```

**Interview Questions:**
- What problem does causal consistency solve for applications reading from secondaries?
- How does MongoDB track causal relationships between operations?
- What is the relationship between causally consistent sessions and read/write concern?

## Storage Engine

### WiredTiger

WiredTiger is MongoDB's default storage engine (since MongoDB 3.2), responsible for how data is actually stored on disk and managed in memory. It provides document-level concurrency control (instead of locking entire collections or the database), compression, and a checkpoint-based durability model combined with a write-ahead journal. Each collection and index is stored in its own WiredTiger file, allowing fine-grained I/O and compression settings.

```mermaid
flowchart LR
    App[Write Operation] --> Cache[WiredTiger In-Memory Cache]
    Cache --> Journal[Write-Ahead Journal]
    Cache -->|periodic checkpoint| Disk[(Data Files on Disk)]
    Journal -->|replay on crash recovery| Disk
```

**Advantages:**
- Document-level locking allows much higher write concurrency than the older MMAPv1 engine
- Built-in compression reduces storage footprint and I/O
- Checkpoints + journaling provide crash resilience

**Disadvantages:**
- Higher memory overhead for its internal cache compared to simpler engines
- Compression trades some CPU for reduced disk usage

**Interview Questions:**
- What concurrency model does WiredTiger use compared to MongoDB's legacy MMAPv1 engine?
- How do checkpoints and the journal work together to guarantee durability?
- Why does each collection and index get its own WiredTiger file?

### Compression

WiredTiger compresses both collection data and indexes by default to reduce disk usage and improve I/O throughput, since compressed data means fewer bytes read from/written to disk. MongoDB supports `snappy` (default, fast with moderate compression), `zlib` (higher compression ratio, more CPU cost), and `zstd` (good balance of ratio and speed, available in newer versions) for collection data, and `prefix` compression for indexes.

```javascript
// Create a collection with zstd compression for higher compression ratio
db.createCollection("auditLogs", {
  storageEngine: { wiredTiger: { configString: "block_compressor=zstd" } }
})
```

**Differences:**

| Compressor | Compression Ratio | CPU Cost | Typical Use |
|---|---|---|---|
| `snappy` | Moderate | Low | Default, general purpose |
| `zlib` | High | High | Storage-constrained, less write-heavy |
| `zstd` | High | Moderate | Modern balanced choice |
| `none` | None | None | Rare, latency-critical, ample disk |

**Interview Questions:**
- Why does MongoDB compress data by default and what's the trade-off?
- When would you choose `zlib` over `snappy`?
- How does index prefix compression differ from block compression of documents?

### Journaling

Journaling is WiredTiger's write-ahead log mechanism that records write operations before they are applied to the in-memory data structures are checkpointed to disk, ensuring that MongoDB can recover uncommitted-to-checkpoint data after an unclean shutdown (e.g. power loss or crash). By default, the journal is flushed to disk roughly every 100 milliseconds (previously called `commitIntervalMs`), and write concern `j: true` forces a write to wait for its journal flush before acknowledging.

```mermaid
sequenceDiagram
    participant Client
    participant Mongod
    participant Journal
    participant DataFiles
    Client->>Mongod: insertOne({...}, {writeConcern:{j:true}})
    Mongod->>Journal: append operation
    Journal-->>Mongod: fsync complete
    Mongod-->>Client: acknowledge write
    Mongod->>DataFiles: apply at next checkpoint
```

**Advantages:**
- Enables crash recovery without losing acknowledged writes
- `j: true` gives per-operation durability guarantees independent of checkpoint frequency

**Disadvantages:**
- Waiting on journal flushes (`j: true`) adds write latency
- Journal files consume additional disk space and I/O bandwidth

**Interview Questions:**
- What does the journal protect against that checkpoints alone do not?
- What happens if MongoDB crashes between two checkpoints, with journaling enabled?
- What is the performance trade-off of using `j: true` on every write?

### Checkpoints

A checkpoint is a consistent, point-in-time snapshot of the data that WiredTiger writes to disk, by default every 60 seconds or after 2GB of journal data has accumulated, whichever comes first. Between checkpoints, durability is provided by the journal; checkpoints simply reduce the amount of journal data that would need to be replayed during crash recovery, and they are how data actually becomes durable on disk in the storage engine's data files.

**Advantages:**
- Bounds crash recovery time by limiting how much journal must be replayed
- Provides a consistent on-disk snapshot for tools like file-system backups

**Disadvantages:**
- Checkpointing consumes disk I/O and can cause momentary latency spikes on busy systems

**Interview Questions:**
- What triggers a WiredTiger checkpoint by default?
- How do checkpoints relate to crash recovery time?
- Why might a filesystem snapshot backup want to be taken right after a checkpoint?

### Cache Management

WiredTiger maintains an in-memory cache (default: 50% of (RAM - 1GB), or 256MB, whichever is greater) that holds frequently accessed, uncompressed data and indexes for fast access. When the working set exceeds the cache size, WiredTiger must evict pages to disk, which increases I/O and can degrade performance; monitoring cache utilization and eviction rates is a key part of MongoDB performance tuning.

```javascript
// Check current WiredTiger cache statistics
db.serverStatus().wiredTiger.cache
```

**Advantages:**
- Keeps hot data in memory for low-latency access
- Configurable size lets operators tune for available hardware

**Disadvantages:**
- Undersized cache relative to working set causes excessive eviction and disk I/O
- Oversized cache can starve the OS file system cache and other processes on the same host

**Interview Questions:**
- What is the default WiredTiger cache size formula?
- What happens when the working set doesn't fit in the WiredTiger cache?
- Which `serverStatus` metrics would you check to diagnose cache pressure?

## Schema Validation

### JSON Schema Validation

MongoDB supports document validation rules expressed using a JSON Schema-based syntax (`$jsonSchema`), allowing you to enforce structure, types, required fields, and value constraints on documents in a collection, even though MongoDB is schemaless by default. This gives teams the flexibility of a document model while still enforcing data integrity guarantees similar to a relational schema, and it's commonly adopted incrementally as an application matures.

```javascript
db.createCollection("orders", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["customerId", "total", "status"],
      properties: {
        customerId: { bsonType: "string", description: "must be a string and is required" },
        total: { bsonType: "double", minimum: 0, description: "must be a non-negative number" },
        status: { enum: ["pending", "shipped", "delivered", "cancelled"] }
      }
    }
  }
})
```

**Advantages:**
- Enforces data integrity at the database layer, independent of application code
- Supports gradual, incremental schema enforcement on existing collections

**Disadvantages:**
- Overly strict schemas reduce the flexibility that made MongoDB attractive in the first place
- Validation errors can be less descriptive than application-level validation messages

**Interview Questions:**
- What is `$jsonSchema` used for in MongoDB?
- How does document validation reconcile with MongoDB's schemaless design philosophy?
- Give an example of a field constraint you might enforce with JSON Schema validation.

### Validation Rules

Validation rules are the actual constraints defined within a validator — such as required fields, `bsonType`, `enum` value lists, numeric ranges (`minimum`/`maximum`), string patterns (regex), and nested object/array schemas. Rules can be combined with logical operators like `allOf`, `anyOf`, and `not` for complex constraints, and they apply to `insert` and `update` operations on the collection.

```javascript
db.runCommand({
  collMod: "orders",
  validator: {
    $jsonSchema: {
      properties: {
        email: { bsonType: "string", pattern: "^.+@.+\\..+$" },
        total: { bsonType: "double", minimum: 0, maximum: 100000 }
      }
    }
  }
})
```

**Interview Questions:**
- How would you enforce that a field must match one of a fixed set of values?
- How can you validate nested subdocuments or array elements?
- What operators let you combine multiple validation rules together?

### Validation Levels

The validation level determines which write operations are checked against the validator: `strict` (default) validates all inserts and updates; `moderate` only validates inserts and updates to documents that already satisfy the validation criteria, allowing existing invalid documents to be updated without being forced to become compliant immediately. This is useful when rolling out validation onto a collection that already contains some non-conforming legacy documents.

```javascript
db.runCommand({
  collMod: "orders",
  validationLevel: "moderate"
})
```

**Differences:**

| Level | Applies To |
|---|---|
| `strict` | All inserts and updates must pass validation |
| `moderate` | Only validates inserts and updates to already-valid documents; allows edits to pre-existing invalid documents |
| `off` | Validation disabled entirely |

**Interview Questions:**
- What is the difference between `strict` and `moderate` validation levels?
- Why would you use `moderate` when introducing validation on an existing collection?
- How would you temporarily disable validation entirely?

### Validation Actions

The validation action determines what happens when a document fails validation: `error` (default) rejects the write and returns an error to the client, while `warn` logs a warning to the MongoDB log but still allows the write to proceed. `warn` is useful for safely testing new validation rules in production without risking application-breaking write rejections.

```javascript
db.runCommand({
  collMod: "orders",
  validator: { $jsonSchema: { required: ["customerId"] } },
  validationAction: "warn"
})
```

**Differences:**

| Action | Behavior on Invalid Document |
|---|---|
| `error` | Write is rejected, error returned to client |
| `warn` | Write proceeds, warning logged to MongoDB log |

**Interview Questions:**
- What's the practical use case for `validationAction: "warn"`?
- How would you safely roll out a new stricter validation rule to a production collection?
- Where would you find the warnings logged when using the `warn` action?

## Security

### Authentication

Authentication verifies the identity of a client connecting to MongoDB before allowing any operations. MongoDB supports several mechanisms: SCRAM (Salted Challenge Response Authentication Mechanism, the default username/password mechanism), x.509 certificate authentication, and enterprise-only mechanisms like LDAP and Kerberos. Authentication is disabled by default on a fresh standalone install but should always be enabled (`--auth` or `security.authorization: enabled`) before exposing MongoDB beyond a trusted local environment.

```javascript
// Create an authenticated user with SCRAM
db.createUser({
  user: "appUser",
  pwd: "strongPasswordHere",
  roles: [{ role: "readWrite", db: "ecommerceDb" }]
})
```

```java
// Spring Boot: configuring authenticated MongoDB connection
spring.data.mongodb.uri=mongodb://appUser:strongPasswordHere@localhost:27017/ecommerceDb?authSource=admin
```

**Differences:**

| Concept | Authentication | Authorization |
|---|---|---|
| Question answered | "Who are you?" | "What are you allowed to do?" |
| Mechanism examples | SCRAM, x.509, LDAP, Kerberos | Roles and privileges (RBAC) |
| Failure result | Connection/login rejected | Operation rejected despite valid login |

**Interview Questions:**
- What authentication mechanisms does MongoDB support?
- Why should authentication always be enabled in production, even on an internal network?
- How does x.509 authentication differ from SCRAM?

### Authorization

Authorization determines what an authenticated user is permitted to do, enforced in MongoDB through Role-Based Access Control (RBAC). Every user is granted one or more roles, and each role bundles a set of privileges (actions on specific resources, like `find` on a particular collection). MongoDB ships built-in roles (e.g. `read`, `readWrite`, `dbAdmin`, `clusterAdmin`) and also supports fully custom roles for fine-grained control.

```javascript
// Grant a custom role with narrowly scoped privileges
db.createRole({
  role: "orderReaderOnly",
  privileges: [
    { resource: { db: "ecommerceDb", collection: "orders" }, actions: ["find"] }
  ],
  roles: []
})
```

**Advantages:**
- Enforces least-privilege access, limiting blast radius of compromised credentials
- Built-in roles cover most common scenarios without custom configuration

**Interview Questions:**
- How does role-based authorization work in MongoDB?
- What is the principle of least privilege and how does RBAC support it?
- How would you grant a service account read-only access to a single collection?

### Users and Roles

Users are created per-database (though a user's `authSource` can be a shared `admin` database) and are assigned one or more roles that may themselves be scoped to different databases. MongoDB stores user credentials and role assignments in the `admin.system.users` and `admin.system.roles` collections. Managing users typically involves `db.createUser()`, `db.updateUser()`, `db.dropUser()`, and `db.grantRolesToUser()`.

```javascript
use admin
db.createUser({
  user: "reportingUser",
  pwd: "anotherStrongPassword",
  roles: [
    { role: "read", db: "ecommerceDb" },
    { role: "read", db: "analyticsDb" }
  ]
})
```

**Interview Questions:**
- Where does MongoDB store user credentials and role definitions?
- Can a single user have roles that span multiple databases? How?
- What is `authSource` and why does it matter when authenticating?

### Role-Based Access Control (RBAC)

RBAC is MongoDB's authorization model where privileges (allowed actions on specific resources) are grouped into roles, and roles are assigned to users rather than granting individual privileges directly. This indirection makes access management scalable: you can update a role's privileges once and have it apply to every user holding that role, and you can compose custom roles from other roles.

```mermaid
flowchart LR
    User1[appUser] --> Role1[readWrite role]
    User2[reportingUser] --> Role2[read role]
    Role1 --> Priv1[Privileges: find, insert, update, remove on ecommerceDb]
    Role2 --> Priv2[Privileges: find on ecommerceDb, analyticsDb]
```

**Advantages:**
- Simplifies permission management at scale compared to per-user privilege grants
- Custom roles allow precise least-privilege modeling for microservices

**Interview Questions:**
- Why is RBAC preferred over granting privileges directly to individual users?
- How would you design roles for a microservices architecture with several services accessing shared collections?
- What built-in MongoDB roles exist for common administrative tasks?

### TLS/SSL

TLS/SSL encrypts data in transit between MongoDB clients, drivers, and cluster members (replica set/shard internal traffic), preventing eavesdropping and man-in-the-middle attacks on the network. MongoDB is configured with `net.tls.mode` (e.g. `requireTLS`) along with certificate and key file paths, and clients must be configured with matching TLS options and trusted CA certificates.

```yaml
# mongod.conf: enabling TLS
net:
  tls:
    mode: requireTLS
    certificateKeyFile: /etc/ssl/mongodb.pem
    CAFile: /etc/ssl/ca.pem
```

```java
// Spring Boot connection string requiring TLS
spring.data.mongodb.uri=mongodb://appUser:pass@host:27017/ecommerceDb?tls=true
```

**Advantages:**
- Protects credentials and sensitive data from network-level interception
- Can also be used for mutual authentication via x.509 client certificates

**Disadvantages:**
- Adds CPU overhead for encryption/decryption and certificate management complexity

**Interview Questions:**
- What network-level threats does TLS protect MongoDB against?
- What MongoDB configuration options control TLS enforcement?
- How can TLS certificates also be used for client authentication?

### Encryption at Rest

Encryption at rest protects data stored on disk (data files, journal, logs, backups) from being read directly if physical storage or disk images are stolen or improperly accessed. MongoDB Enterprise offers native WiredTiger encryption at rest using AES-256, integrated with a key management system (KMIP) or a local key file; alternatively, encryption can be achieved at the filesystem/disk layer (e.g. LUKS, encrypted EBS volumes) independent of MongoDB itself.

```yaml
# mongod.conf: enabling native encryption at rest (Enterprise)
security:
  enableEncryption: true
  encryptionKeyFile: /etc/mongodb/keyfile
```

**Differences:**

| Approach | Layer | Availability |
|---|---|---|
| Native WiredTiger encryption | Storage engine | MongoDB Enterprise only |
| Filesystem/disk encryption (e.g. LUKS, encrypted EBS) | OS/infrastructure | Available on Community edition |

**Interview Questions:**
- What is the difference between encryption at rest and TLS in transit?
- What options exist for encrypting MongoDB data at rest on the Community edition?
- What key management approaches does MongoDB Enterprise support for encryption at rest?

### Auditing (Overview)

Auditing (a MongoDB Enterprise feature) records system events such as authentication attempts, authorization checks, CRUD operations, and administrative actions, providing a trail for compliance and security forensics. Audit filters let you narrow the captured events (e.g. only failed authentication attempts, or actions by a specific user) to control log volume, and output can be written to a file, syslog, or console in JSON or BSON format.

```javascript
// Example audit filter: only log authentication failures
{
  atype: "authenticate",
  "param.result": { $ne: 0 }
}
```

**Advantages:**
- Provides an immutable trail for compliance requirements (SOC 2, HIPAA, PCI-DSS)
- Filters allow focused auditing without excessive log noise

**Interview Questions:**
- What kinds of events can MongoDB's auditing feature capture?
- Why would you configure an audit filter rather than logging every event?
- What compliance scenarios typically require database auditing?

### Client-Side Field Level Encryption

Client-Side Field Level Encryption (CSFLE) encrypts specific document fields on the client, before they are ever sent to MongoDB, so that even database administrators or anyone with direct data file access cannot read the plaintext values. Encryption keys are managed via a key management system (local, AWS KMS, Azure Key Vault, GCP KMS) and never exposed to the server; MongoDB stores only ciphertext for the designated fields, while the driver transparently encrypts/decrypts them for authorized clients holding the correct keys.

```mermaid
sequenceDiagram
    participant App
    participant Driver as Driver (CSFLE)
    participant KMS
    participant Mongo as MongoDB Server
    App->>Driver: insert({ssn: "123-45-6789"})
    Driver->>KMS: fetch data encryption key
    KMS-->>Driver: return key
    Driver->>Driver: encrypt ssn field locally
    Driver->>Mongo: insert({ssn: <ciphertext>})
    Mongo-->>Driver: stored (server never sees plaintext)
```

**Advantages:**
- Protects highly sensitive fields (SSNs, payment data) even from privileged database access
- Supports queryable encryption variants for equality searches on encrypted fields (in newer versions)

**Disadvantages:**
- Adds application/driver complexity and key management overhead
- Encrypted fields have restricted query capabilities compared to plaintext fields

**Interview Questions:**
- How does Client-Side Field Level Encryption differ from encryption at rest and TLS?
- Who manages the encryption keys in a CSFLE setup, and why does that matter?
- What are the query limitations on fields encrypted with CSFLE?

## Backup and Recovery

### mongodump

`mongodump` is a command-line utility that creates a binary (BSON) export of the data in a MongoDB database or collection, capturing documents and optionally indexes/metadata for later restoration with `mongorestore`. It can target a whole deployment, a single database, or a single collection, and supports query filters to export a subset of data. On a replica set, running it against a secondary with `--readPreference` avoids adding load to the primary.

```bash
# Dump an entire database to a local directory
mongodump --uri="mongodb://localhost:27017" --db=ecommerceDb --out=/tmp/backups/2026-08-02

# Dump only orders placed in the last 24 hours
mongodump --uri="mongodb://localhost:27017" --db=ecommerceDb --collection=orders \
  --query='{"orderDate": {"$gte": {"$date": "2026-08-01T00:00:00Z"}}}' \
  --out=/tmp/backups/orders-incremental
```

**Advantages:**
- Simple, built-in, no extra tooling required
- Supports filtering and per-collection granularity

**Disadvantages:**
- Not a point-in-time consistent snapshot across a whole sharded cluster unless carefully coordinated
- Slower and more resource-intensive than filesystem/block-level snapshots for very large datasets

**Interview Questions:**
- What does `mongodump` actually capture, and in what format?
- How would you take a backup without impacting the primary's performance?
- What are the limitations of `mongodump` for very large or sharded deployments?

### mongorestore

`mongorestore` loads BSON data produced by `mongodump` back into a MongoDB deployment, recreating collections, documents, and (optionally) indexes. It supports restoring into a different database/collection name than the source, dropping existing data before restore, and parallelism options (`--numParallelCollections`, `--numInsertionWorkersPerCollection`) to speed up large restores.

```bash
# Restore a database, dropping existing collections first
mongorestore --uri="mongodb://localhost:27017" --db=ecommerceDb --drop /tmp/backups/2026-08-02/ecommerceDb

# Restore into a differently named database (e.g. for testing)
mongorestore --uri="mongodb://localhost:27017" --nsFrom="ecommerceDb.*" --nsTo="ecommerceDbTest.*" /tmp/backups/2026-08-02/ecommerceDb
```

**Differences:**

| Tool | Direction | Common Flags |
|---|---|---|
| `mongodump` | Database → BSON files | `--db`, `--collection`, `--query`, `--out` |
| `mongorestore` | BSON files → Database | `--drop`, `--nsFrom`/`--nsTo`, `--numParallelCollections` |

**Interview Questions:**
- What does the `--drop` flag do during a restore, and when would you use it?
- How can you restore a backup into a differently named database for testing?
- How would you speed up a restore of a very large dataset?

### Point-in-Time Recovery (Overview)

Point-in-time recovery (PITR) allows restoring a database to a specific moment rather than only to the time of the last full backup, typically by combining a base snapshot/backup with replayed oplog entries up to the desired timestamp. MongoDB Atlas offers continuous backup with PITR out of the box; self-managed deployments can approximate this by periodically archiving the oplog alongside filesystem snapshots and replaying oplog entries during restore.

```mermaid
flowchart LR
    Snapshot[Base Snapshot at T0] --> Replay[Replay Oplog Entries]
    Replay -->|up to desired timestamp T1| Restored[Restored State at T1]
```

**Advantages:**
- Enables recovery from logical errors (e.g. accidental deletes) to the exact moment before the mistake
- Reduces data loss window compared to relying solely on periodic full backups

**Disadvantages:**
- Requires retaining oplog history long enough to cover the desired recovery window
- More complex to implement and test on self-managed infrastructure compared to managed offerings

**Interview Questions:**
- What is the difference between a regular backup and point-in-time recovery?
- How does the oplog enable point-in-time recovery?
- What operational requirement (oplog retention) is necessary to support PITR?

### Backup Strategies

Backup strategy choices generally fall into logical backups (`mongodump`/`mongorestore`), filesystem/block-level snapshots (e.g. LVM snapshots, cloud disk snapshots), and managed continuous backup (MongoDB Atlas). Strategy selection depends on dataset size, RPO/RTO requirements, and whether the cluster is sharded (requiring coordinated snapshots across shards and config servers for consistency).

**Differences:**

| Strategy | Consistency | Speed (large data) | Sharded Cluster Support |
|---|---|---|---|
| `mongodump`/`mongorestore` | Per-collection, not cluster-wide atomic unless paused | Slower | Requires care/coordination |
| Filesystem snapshot | Point-in-time per node | Fast | Needs coordinated snapshot across shards + config servers |
| Managed (Atlas) continuous backup | Point-in-time, cluster-wide | Fast | Native support |

**Advantages:**
- Filesystem snapshots are typically faster and more scalable for large datasets
- Managed backup services remove most operational burden

**Disadvantages:**
- Filesystem snapshots require careful coordination in sharded clusters to avoid inconsistent cross-shard state
- Logical backups can be slow to restore for very large collections

**Interview Questions:**
- What factors determine whether you'd choose logical backups vs. filesystem snapshots?
- Why is backing up a sharded cluster more complex than a single replica set?
- What are RPO and RTO, and how do they influence backup strategy?

### Disaster Recovery

Disaster recovery (DR) planning covers how a MongoDB deployment survives catastrophic failures — data center outages, region-wide cloud provider incidents, or severe data corruption — going beyond routine backups to include geographically distributed replica set members, cross-region backup replication, documented and tested restore procedures, and defined RPO (Recovery Point Objective) and RTO (Recovery Time Objective) targets.

A typical production DR setup places replica set members across multiple availability zones or regions so a regional outage doesn't take down the whole deployment, combined with off-site backup copies and periodic restore drills to validate the plan actually works.

```mermaid
flowchart TD
    Primary[Primary - Region A] --- Secondary1[Secondary - Region A]
    Primary --- Secondary2[Secondary - Region B]
    Secondary2 --- Backup[Off-site Encrypted Backups]
```

**Advantages:**
- Multi-region replica sets provide resilience against full data center/region loss
- Regularly tested restore procedures reduce the risk of backups being unusable when actually needed

**Disadvantages:**
- Cross-region replication adds network latency to write acknowledgment (if those members count toward write concern)
- Maintaining and regularly testing a DR plan requires ongoing operational investment

**Interview Questions:**
- What is the difference between a backup strategy and a full disaster recovery plan?
- How would you design replica set topology to survive a regional outage?
- What are RPO and RTO, and how would you define them for a critical production database?
- Why is periodically testing a restore procedure just as important as taking the backup itself?

## Performance Tuning

### Query Optimization

Query optimization involves shaping queries and the underlying data/index design so MongoDB can satisfy requests with minimal document scanning, primarily by ensuring queries are covered by appropriate indexes and by using `explain()` to inspect the query planner's chosen execution plan (`IXSCAN` vs. the much slower `COLLSCAN`). Common techniques include projecting only needed fields, avoiding unnecessary `$where`/regex-without-anchors, and structuring compound indexes to match query patterns (equality, sort, range — the "ESR rule").

```javascript
// Inspect the execution plan for a query
db.orders.find({ customerId: "C123", status: "shipped" })
  .sort({ orderDate: -1 })
  .explain("executionStats")
```

**Interview Questions:**
- What is the difference between `COLLSCAN` and `IXSCAN` in an `explain()` output?
- What is the ESR (Equality, Sort, Range) rule for compound index design?
- How would you diagnose why a query is running slowly in production?

### Index Optimization

Index optimization is the ongoing practice of ensuring the right indexes exist (supporting actual query patterns) without over-indexing, since every index adds write overhead and memory/disk usage. This includes removing unused indexes (identified via `$indexStats`), preferring compound indexes over multiple single-field indexes when queries combine filters, and considering partial or sparse indexes to reduce index size for selective query patterns.

```javascript
// Find unused indexes based on collected usage stats
db.orders.aggregate([{ $indexStats: {} }])

// Create a partial index that only indexes shipped orders
db.orders.createIndex(
  { orderDate: 1 },
  { partialFilterExpression: { status: "shipped" } }
)
```

**Advantages:**
- Well-tuned indexes dramatically reduce query latency and CPU usage
- Partial/sparse indexes reduce index size and memory footprint for selective queries

**Disadvantages:**
- Every additional index slows down writes (inserts/updates/deletes) and consumes RAM/disk
- Redundant or unused indexes waste resources without benefit

**Interview Questions:**
- How would you identify unused indexes in a production collection?
- What's the trade-off of adding more indexes to speed up reads?
- When would you use a partial index instead of a full index?

### Connection Pooling

Connection pooling allows a MongoDB driver to reuse a set of established TCP connections across many operations instead of opening/closing a new connection per request, dramatically reducing connection setup overhead (including TLS handshakes) under load. Drivers expose pool size settings (e.g. `maxPoolSize`, `minPoolSize`) that should be tuned relative to expected application concurrency and the server's `maxIncomingConnections` limit.

```java
// Spring Boot application.properties: tuning the MongoDB connection pool
spring.data.mongodb.uri=mongodb://localhost:27017/ecommerceDb?maxPoolSize=100&minPoolSize=10&maxIdleTimeMS=60000
```

**Advantages:**
- Reduces latency from repeated connection establishment
- Bounds resource usage on both client and server sides

**Disadvantages:**
- Undersized pools cause request queuing/timeouts under concurrent load
- Oversized pools across many application instances can overwhelm the server's connection limits

**Interview Questions:**
- Why is connection pooling important for MongoDB driver performance?
- What happens if `maxPoolSize` is set too low for a high-concurrency service?
- How would you size connection pools across multiple application instances sharing one MongoDB cluster?

### Bulk Writes

Bulk write operations let a client send multiple insert, update, or delete operations in a single request, reducing network round-trips and improving throughput compared to issuing each operation individually. MongoDB supports ordered bulk writes (stop on first error, operations execute in sequence) and unordered bulk writes (continue past errors, operations may execute in any order, allowing more parallelism).

```javascript
db.orders.bulkWrite([
  { insertOne: { document: { customerId: "C1", total: 20 } } },
  { updateOne: { filter: { customerId: "C2" }, update: { $set: { status: "shipped" } } } },
  { deleteOne: { filter: { customerId: "C3" } } }
], { ordered: false })
```

**Differences:**

| Mode | Error Behavior | Execution Order |
|---|---|---|
| Ordered (default) | Stops at first error | Sequential, guaranteed order |
| Unordered | Continues past errors, reports all at the end | May execute in parallel, no order guarantee |

**Interview Questions:**
- What is the difference between ordered and unordered bulk writes?
- Why would unordered bulk writes generally perform better?
- What happens to remaining operations in an ordered bulk write after one fails?

### Batch Processing

Batch processing refers to structuring application workloads to read, transform, and write data in chunks rather than one document at a time, reducing overhead and improving throughput for large-scale operations like ETL jobs, migrations, or nightly reporting. This is typically combined with cursor batching (`cursor.batchSize()`) on reads and bulk writes on the write side, and is a common pattern implemented with Spring Batch when integrating with MongoDB in Spring Boot applications.

```java
// Spring Data MongoDB: reading in batches with a cursor batch size
MongoCursor<Document> cursor = collection.find()
    .batchSize(500)
    .iterator();
```

**Advantages:**
- Reduces per-document network and processing overhead for large datasets
- Pairs naturally with bulk writes for efficient large-scale updates

**Disadvantages:**
- Larger batch sizes increase memory usage per batch
- Requires careful error handling/retry logic since a failure mid-batch can leave partial progress

**Interview Questions:**
- Why is batch processing more efficient than per-document processing for large datasets?
- How does cursor `batchSize` affect memory usage and round-trips?
- How would you handle a failure partway through processing a large batch job?

### Profiling

The MongoDB database profiler captures detailed information about executed operations (queries, updates, commands) including execution time, and stores them in the `system.profile` capped collection, enabling analysis of what's actually running against the database and how expensive each operation is. It has three levels: `0` (off), `1` (log slow operations above a threshold), and `2` (log every operation — very verbose, typically only for short-term debugging).

```javascript
// Enable profiling for operations slower than 100ms
db.setProfilingLevel(1, { slowms: 100 })

// Review the slowest recent operations
db.system.profile.find().sort({ millis: -1 }).limit(10)
```

**Advantages:**
- Provides ground-truth visibility into actual query performance in a live system
- Level 1 with a threshold has low overhead, safe for production use

**Disadvantages:**
- Level 2 (profile everything) adds significant overhead and is unsuitable for sustained production use
- `system.profile` is a capped collection, so old entries roll off and must be exported for long-term analysis

**Interview Questions:**
- What are the differences between profiling levels 0, 1, and 2?
- Where does MongoDB store profiling data, and what are the implications of that?
- What overhead considerations apply to running the profiler in production?

### Slow Query Analysis

Slow query analysis is the practice of identifying, inspecting, and fixing operations that exceed acceptable latency thresholds, typically by combining the database profiler or `mongod` log's slow-query entries (operations exceeding `slowms`, default 100ms) with `explain()` to understand why a specific query is slow (missing index, poor index selectivity, large result sets, or query patterns causing collection scans).

```bash
# Search the mongod log for slow query entries
grep -i "COMMAND" /var/log/mongodb/mongod.log | grep -i "durationMillis" | awk '$NF+0 > 200'
```

**Interview Questions:**
- What is a practical workflow for diagnosing a slow query in production?
- How does `explain("executionStats")` help identify the root cause of slowness?
- What are common root causes of slow queries in MongoDB (missing indexes, large scans, poor shard key, etc.)?

### Memory Considerations

MongoDB performance is heavily influenced by how much of the "working set" (the data and indexes actively accessed) fits in available memory — primarily the WiredTiger cache, but also the OS page cache for compressed on-disk pages. When the working set exceeds available memory, MongoDB must read from disk more frequently, increasing latency; sizing RAM appropriately, using efficient data types/indexes, and monitoring page faults and cache eviction rates are core parts of capacity planning.

```javascript
// Check for page faults, an indicator of memory pressure
db.serverStatus().extra_info.page_faults
```

**Advantages:**
- Ensuring the working set fits in memory is one of the single biggest performance levers available
- Monitoring memory metrics proactively helps catch capacity issues before they cause outages

**Disadvantages:**
- Simply adding more RAM has diminishing returns if data models or indexes are inefficient
- Under-provisioned memory on write-heavy sharded clusters can cause cascading replication lag

**Interview Questions:**
- What is the "working set" and why does its relationship to available RAM matter so much for performance?
- What metrics would you monitor to detect memory pressure in MongoDB?
- How would you approach capacity planning for RAM on a new MongoDB deployment?

## Monitoring

### MongoDB Profiler

The database profiler collects detailed performance data about read and write operations, cursor operations, and database commands executed against a MongoDB instance. It can be enabled at different levels (0 = off, 1 = slow operations only, 2 = all operations) and stores its output in the `system.profile` capped collection within each database.

In production, teams typically enable level 1 profiling with a slow-operation threshold (e.g. 100ms) to capture only problematic queries without incurring the overhead of profiling every operation, then use the captured data to identify missing indexes or inefficient aggregation pipelines.

```javascript
// Enable profiling for operations slower than 100ms
db.setProfilingLevel(1, { slowms: 100 })

// Check current profiling status
db.getProfilingStatus()

// Query the profile collection for the slowest operations
db.system.profile.find().sort({ millis: -1 }).limit(5).pretty()
```

**Advantages:**
- Captures real query shapes and timings directly from production traffic
- Configurable granularity (off / slow-only / all)
- No application code changes required

**Disadvantages:**
- Level 2 profiling adds noticeable overhead and disk usage
- Capped collection can roll over quickly under heavy load, losing older samples
- Requires manual analysis unless paired with tools like Compass or Atlas Performance Advisor

**Interview Questions:**
- What are the different MongoDB profiler levels and when would you use each?
- Where is profiler data stored and what are the risks of leaving profiling on in production?
- How would you use the profiler output to identify a missing index?
- What is the performance impact of enabling profiling level 2 on a busy cluster?

### Server Status

The `serverStatus` command returns a comprehensive snapshot of the current state of a `mongod` or `mongos` instance, including memory usage, connection counts, opcounters, replication lag, journaling stats, and WiredTiger cache metrics. It's the primary building block for most external monitoring integrations (Atlas, Ops Manager, Prometheus exporters).

```javascript
// Get a full server status document
db.serverStatus()

// Inspect just the connection and opcounters sections
db.serverStatus().connections
db.serverStatus().opcounters
```

**Interview Questions:**
- What key sections of `serverStatus()` would you check first when diagnosing a performance incident?
- How can `serverStatus()` help detect connection pool exhaustion?
- What WiredTiger metrics in `serverStatus()` indicate cache pressure?

### Database Statistics

The `dbStats` command reports storage-level metrics for a single database, such as total data size, storage size, index size, and object/collection counts. It's useful for capacity planning and tracking storage growth trends over time.

```javascript
use myDatabase
db.stats()
```

**Interview Questions:**
- What is the difference between `dataSize` and `storageSize` in `dbStats()` output?
- How would you use `dbStats()` to plan disk capacity for a growing collection?

### Collection Statistics

The `collStats` command (or `db.collection.stats()`) provides detailed size, document count, average object size, and index size information for a specific collection, plus sharding-specific data (like chunk distribution) when run against a sharded collection.

```javascript
db.orders.stats()

// Sharding-aware stats
db.orders.stats({ scale: 1024 * 1024 })
```

**Interview Questions:**
- How do you find the average document size of a collection and why does it matter?
- What extra information does `collStats` return for a sharded collection compared to an unsharded one?

### Index Statistics

The `$indexStats` aggregation stage returns usage statistics for each index on a collection, including how many times each index has been used to serve a query since the last server restart. This is the primary tool for identifying unused indexes that can be safely dropped to save write overhead and storage.

```javascript
db.orders.aggregate([{ $indexStats: {} }])
```

**Advantages:**
- Directly identifies dead/unused indexes
- Low overhead to query

**Disadvantages:**
- Counters reset on server restart or failover, so long observation windows are needed before drawing conclusions

**Interview Questions:**
- How would you identify and safely remove unused indexes from a large production collection?
- Why might `$indexStats` counters be misleading right after a replica set election?

### Performance Metrics

Beyond individual commands, MongoDB exposes performance metrics through `serverStatus().metrics`, FTDC (Full Time Diagnostic Data Capture) files, and integrations like MongoDB Atlas charts or Prometheus/Grafana dashboards. Key metrics to track include query targeting ratio (scanned vs. returned documents), replication lag, cache eviction rate, and queue lengths for reads/writes.

```mermaid
flowchart LR
    A[mongod process] -->|writes| B[FTDC diagnostic files]
    A -->|serverStatus/dbStats| C[Monitoring Agent]
    C --> D[Prometheus / Atlas / Ops Manager]
    D --> E[Dashboards & Alerts]
```

**Interview Questions:**
- Which metrics best indicate that a collection is missing a critical index?
- What is FTDC and how is it used for post-incident diagnostics?
- How would you set up alerting for replication lag in a production cluster?

### Mongostat and Mongotop

`mongostat` provides a `vmstat`-like, per-second view of key server counters (inserts, queries, updates, deletes, connections, cache metrics) directly in the terminal, useful for a quick live health check. `mongotop` shows the amount of time each collection spends performing read and write operations, helping to quickly spot which collection is the hot spot.

```bash
# Live server-wide counters refreshed every second
mongostat --host localhost:27017

# Per-collection read/write time, refreshed every 5 seconds
mongotop 5
```

**Interview Questions:**
- When would you reach for `mongostat` versus a full monitoring dashboard?
- How does `mongotop` help you identify a hot collection during an incident?

## GridFS

### GridFS Fundamentals

GridFS is a specification for storing and retrieving files that exceed the BSON document size limit of 16MB, or files that you want to serve in chunks (such as streaming video). Instead of storing a file as a single document, GridFS splits it into smaller chunks and stores each chunk as a separate document, alongside a single metadata document describing the file as a whole.

Use GridFS when you need to store large binary assets (images, videos, PDFs, backups) directly in MongoDB and want features like range queries on parts of a file, or when your file storage needs to scale and replicate along with the rest of your data. For simple small file storage, a plain `Binary` field or an external object store (S3, GCS) is often simpler and cheaper.

**Advantages:**
- Files larger than 16MB can be stored without hitting the BSON limit
- Chunks replicate/shard along with the rest of the database
- Partial reads of large files are efficient (e.g. seeking into a video)

**Disadvantages:**
- Higher operational complexity than a dedicated object store
- Not ideal for very small, frequently changing files due to per-chunk overhead
- No built-in atomicity across all chunks of a file during concurrent writes

**Interview Questions:**
- Why does GridFS exist and when should you use it instead of storing raw binary data in a document?
- What are the alternatives to GridFS for storing large files and when would you choose them?

### File Storage

When a file is stored with GridFS, it is broken into chunks (default 255KB each) which are written to the `fs.chunks` collection, while a single document describing the file (filename, length, chunkSize, uploadDate, metadata) is written to `fs.files`. Drivers handle this splitting and reassembly transparently through a GridFS bucket API.

```javascript
// Using the mongosh/Node.js driver GridFSBucket API
const bucket = new mongodb.GridFSBucket(db, { bucketName: 'uploads' })

const uploadStream = bucket.openUploadStream('report.pdf', {
  metadata: { contentType: 'application/pdf', uploadedBy: 'user123' }
})
fs.createReadStream('./report.pdf').pipe(uploadStream)
```

```mermaid
flowchart TD
    A[Original File] -->|split into 255KB chunks| B[fs.chunks collection]
    A -->|metadata: filename, length, uploadDate| C[fs.files collection]
    C -->|references chunks by files_id + n| B
```

**Interview Questions:**
- What is the default chunk size in GridFS and can it be changed?
- What information is stored in `fs.files` versus `fs.chunks`?

### Buckets

A GridFS "bucket" is simply a named pair of collections (`<bucketName>.files` and `<bucketName>.chunks`) used to organize stored files. The default bucket name is `fs`, but applications can define multiple buckets (e.g. `images`, `videos`) to logically separate different kinds of file storage within the same database.

```javascript
// Create/use a custom bucket named "images"
const imagesBucket = new mongodb.GridFSBucket(db, { bucketName: 'images' })
```

**Interview Questions:**
- Why might an application use multiple GridFS buckets instead of the default one?
- What indexes should exist on the `files` and `chunks` collections of a bucket for good performance?

### File Retrieval

Files are read back from GridFS by opening a download stream against the bucket, either by file `_id` or filename, which transparently reassembles the chunks in order into a continuous byte stream. GridFS also supports range reads (`start`/`end` options) so consumers can stream only part of a file, which is essential for features like video seeking.

```javascript
// Download a file by its ObjectId and pipe it to an HTTP response
const downloadStream = bucket.openDownloadStream(fileId)
downloadStream.pipe(res)

// Download only a byte range of the file
bucket.openDownloadStream(fileId, { start: 1000, end: 5000 }).pipe(res)
```

**Interview Questions:**
- How does GridFS support streaming a large video file without loading it entirely into memory?
- What happens if a chunk in the middle of a file is missing or corrupted during retrieval?

## Change Streams

### Change Streams

Change Streams allow applications to subscribe to real-time notifications of data changes (inserts, updates, deletes, replaces) on a collection, database, or entire deployment, without the complexity and inefficiency of tailing the oplog directly. They are built on top of the replication oplog but expose a stable, versioned API that is resilient to internal storage changes.

A common production use case is invalidating a cache or updating a search index (e.g. Elasticsearch) whenever a document changes, without polling the database or coupling the write path to secondary systems synchronously.

```javascript
// Open a change stream on a single collection
const changeStream = db.orders.watch()

changeStream.on('change', (change) => {
  console.log('Change detected:', change)
})
```

**Advantages:**
- Real-time, push-based notification instead of polling
- Resumable via resume tokens after a disconnect
- Can filter changes with an aggregation pipeline

**Disadvantages:**
- Requires a replica set or sharded cluster (not available on standalone servers)
- Consumers must handle resuming and de-duplication correctly to avoid missed or duplicated events

**Interview Questions:**
- What underlying mechanism do change streams use internally?
- Why are change streams not available on a standalone `mongod` instance?
- How would you use a change stream to keep a search index in sync with MongoDB?

### Event Notifications

Each change stream event is a document describing the operation type (`insert`, `update`, `delete`, `replace`, `invalidate`, etc.), the affected namespace, the document key, and — for updates — the specific fields that changed (`updateDescription`). Consumers can filter events using a `$match` stage passed to `watch()`.

```javascript
// Only watch for insert and update operations on the "orders" collection
db.orders.watch([
  { $match: { operationType: { $in: ['insert', 'update'] } } }
])
```

```mermaid
sequenceDiagram
    participant App as Application
    participant Mongo as MongoDB Replica Set
    participant Oplog as Oplog
    App->>Mongo: db.collection.watch()
    Mongo->>Oplog: Subscribe to relevant oplog entries
    Oplog-->>Mongo: New write operation recorded
    Mongo-->>App: Change event (with resume token)
```

**Interview Questions:**
- What fields are typically present in a change stream event document?
- How would you filter a change stream to only receive events for a specific field update?

### Resume Tokens

A resume token is an opaque value included in every change stream event that marks its position in the stream. If a consumer's connection drops, it can pass the last seen resume token back to `watch()` (via `resumeAfter` or `startAfter`) to continue exactly where it left off, without missing or reprocessing events.

```javascript
let resumeToken = null
const stream = db.orders.watch()

stream.on('change', (change) => {
  resumeToken = change._id
  // process change...
})

// On reconnect:
db.orders.watch([], { resumeAfter: resumeToken })
```

**Interview Questions:**
- What is a resume token used for and how does it enable fault-tolerant change stream consumers?
- What is the difference between `resumeAfter` and `startAfter`?
- What happens if the resume token refers to an oplog entry that has already been rolled off?

### Real-Time Data Processing

Change streams are commonly used to build event-driven pipelines: syncing data to caches or search engines, triggering microservice workflows, feeding data lakes, or driving real-time dashboards — all without a separate CDC (Change Data Capture) tool. In Spring applications, this is typically implemented with `MongoTemplate`'s reactive change stream support or Spring Data's `@Tailable` cursors for capped collections.

```java
// Spring Data MongoDB reactive change stream example
ChangeStreamOptions options = ChangeStreamOptions.builder()
    .filter(Aggregation.newAggregation(match(where("operationType").is("insert"))))
    .build();

reactiveMongoTemplate.changeStream("orders", options, Order.class)
    .doOnNext(event -> System.out.println("Change: " + event.getBody()))
    .subscribe();
```

**Interview Questions:**
- How would you design a real-time notification system on top of MongoDB change streams?
- What are the trade-offs of using change streams versus a dedicated message broker (Kafka, RabbitMQ) for event-driven architectures?

## MongoDB Design Patterns

### Bucket Pattern

The Bucket Pattern groups multiple related small documents (typically time-series or IoT sensor readings) into a single "bucket" document that holds an array of measurements within a fixed time range, instead of storing one document per reading. This drastically reduces the total document count and index overhead while keeping related data physically co-located.

A common use case is storing sensor readings taken every second: rather than one document per reading (millions per day), you bucket all readings for a sensor within a one-hour window into a single document with an array field.

```json
{
  "sensorId": "sensor-42",
  "startTime": "2026-08-02T10:00:00Z",
  "endTime": "2026-08-02T11:00:00Z",
  "readingsCount": 60,
  "readings": [
    { "ts": "2026-08-02T10:00:00Z", "temp": 21.5 },
    { "ts": "2026-08-02T10:01:00Z", "temp": 21.6 }
  ]
}
```

**Advantages:**
- Fewer documents means smaller indexes and less per-document overhead
- Related readings are read together in a single disk fetch

**Disadvantages:**
- Documents can grow large and require careful bucket-size limits
- Updating/appending to buckets requires careful array-size management to avoid document growth churn

**Interview Questions:**
- How does the Bucket Pattern improve performance for time-series workloads?
- What are the risks of unbounded array growth within a bucket document?
- How would you decide the time window size for a bucket?

### Attribute Pattern

The Attribute Pattern converts many similar optional fields into an array of key/value sub-documents, making it easy to index and query a variable/sparse set of attributes without creating a separate index per field. It's especially useful for product catalogs where different products have different specification fields.

```json
{
  "name": "Wireless Headphones",
  "attributes": [
    { "k": "color", "v": "black" },
    { "k": "batteryLifeHours", "v": 30 },
    { "k": "wireless", "v": true }
  ]
}
```

**Advantages:**
- A single compound index on `attributes.k` and `attributes.v` can serve queries across many different attribute types
- Avoids needing dozens of sparse single-field indexes

**Disadvantages:**
- Queries become slightly more verbose (`{"attributes.k": "color", "attributes.v": "black"}`)
- Not ideal for attributes you always need to project directly

**Interview Questions:**
- What problem does the Attribute Pattern solve compared to storing each attribute as a top-level field?
- How would you write an index to efficiently support the Attribute Pattern?

### Subset Pattern

The Subset Pattern addresses the "unbounded array" or "large document" problem by storing only a small, frequently accessed subset of a large array or related dataset in the main document (e.g. the 10 most recent reviews), while the full dataset is kept in a separate collection. This keeps the main document's working set small enough to fit comfortably in the WiredTiger cache.

```json
// product document keeps only the 5 most recent reviews inline
{
  "_id": "prod-1",
  "name": "Laptop",
  "recentReviews": [
    { "user": "alice", "rating": 5, "comment": "Great!" }
  ]
}
// full review history lives in a separate "reviews" collection, queried on demand
```

**Interview Questions:**
- When would you apply the Subset Pattern instead of embedding a full array?
- How does the Subset Pattern help with working-set size and cache efficiency?

### Extended Reference Pattern

The Extended Reference Pattern denormalizes a few frequently accessed fields from a referenced document into the referencing document, avoiding an extra lookup ($lookup or a second query) for common read paths, while still keeping the full referenced document elsewhere for less frequent needs.

```json
// order document embeds a small "extended reference" of the customer
{
  "_id": "order-100",
  "customerRef": { "customerId": "cust-9", "name": "Jane Doe", "email": "jane@example.com" },
  "items": [ { "sku": "SKU1", "qty": 2 } ]
}
```

**Advantages:**
- Eliminates a join/lookup for the most common read pattern
- Reduces read latency for frequently displayed data (e.g. order lists showing customer name)

**Disadvantages:**
- Denormalized copies must be kept in sync when the source data changes

**Interview Questions:**
- How does the Extended Reference Pattern trade off consistency for read performance?
- What strategy would you use to keep denormalized extended reference fields up to date?

### Computed Pattern

The Computed Pattern precomputes and stores the result of an expensive calculation (sums, averages, counts) rather than recalculating it on every read. The computed value is updated periodically or on each write, trading a small amount of write-time cost for much cheaper reads.

```javascript
// Instead of summing all order line items on every product page view,
// maintain a precomputed "totalSold" field updated on each order.
db.products.updateOne(
  { _id: 'prod-1' },
  { $inc: { totalSold: 1, totalRevenue: 49.99 } }
)
```

**Interview Questions:**
- When is it worth the added write complexity to precompute a value versus calculating it on read?
- How would you keep a computed field consistent if updates can fail partway through?

### Outlier Pattern

The Outlier Pattern handles the rare cases where a small percentage of documents would otherwise become abnormally large (e.g. a celebrity account with millions of followers) by flagging those documents and moving their overflow data to a secondary collection, while the common case remains a simple, compact document.

```json
{
  "_id": "user-1",
  "username": "celebrity_account",
  "hasOutlierFollowers": true,
  "followersPreview": ["user-2", "user-3"]
}
// full follower list for this specific user lives in a "user_followers_overflow" collection
```

**Interview Questions:**
- What problem does the Outlier Pattern solve that the Subset Pattern doesn't fully address?
- How would you detect which documents need to be treated as outliers in your schema?

### Schema Versioning Pattern

The Schema Versioning Pattern adds an explicit `schemaVersion` field to documents so that application code can support multiple document shapes simultaneously during a gradual, zero-downtime migration, instead of requiring a risky big-bang migration of all documents at once.

```json
{ "_id": "u1", "schemaVersion": 2, "name": "Alice", "address": { "city": "NYC" } }
{ "_id": "u2", "schemaVersion": 1, "name": "Bob", "city": "LA" }
```

```mermaid
flowchart LR
    A[Old documents v1] --> C[App reads schemaVersion]
    B[New documents v2] --> C
    C -->|v1| D[Apply migration logic on read]
    C -->|v2| E[Use document as-is]
    D --> F[Optionally re-save as v2]
```

**Advantages:**
- Enables rolling deployments and gradual background migration
- Avoids a long-running blocking migration script

**Disadvantages:**
- Application code must handle multiple schema versions concurrently, adding complexity

**Interview Questions:**
- How would you migrate a large production collection to a new schema without downtime?
- Where should version-aware transformation logic live in a layered application (e.g. Spring service vs. repository)?

## High Availability

### Replica Set Failover

A replica set is a group of `mongod` instances maintaining the same data set, consisting of one primary (accepts writes) and multiple secondaries (replicate from the primary). If the primary becomes unreachable, the remaining members automatically hold an election to promote a new primary, allowing the application to continue writing with minimal manual intervention.

```mermaid
sequenceDiagram
    participant P as Primary
    participant S1 as Secondary 1
    participant S2 as Secondary 2
    P->>S1: Heartbeat (OK)
    P->>S2: Heartbeat (OK)
    Note over P: Primary crashes
    S1--xP: Heartbeat timeout
    S2--xP: Heartbeat timeout
    S1->>S2: Call for election
    S2-->>S1: Vote granted
    Note over S1: S1 becomes new Primary
```

**Advantages:**
- Automatic failover with no manual DBA intervention required
- Reads can be distributed to secondaries for read scaling (with eventual consistency trade-offs)

**Disadvantages:**
- A brief write-unavailability window occurs during election (typically a few seconds)
- Requires an odd number of voting members (or an arbiter) to avoid split votes

**Interview Questions:**
- What triggers a replica set election and how long does failover typically take?
- What is the role of an arbiter in a replica set and when would you use one?
- How does a driver detect that a primary has changed and redirect writes?

### Heartbeats

Replica set members send heartbeat pings to each other roughly every 2 seconds to monitor availability. If a member doesn't respond within the configured `electionTimeoutMillis` (default 10 seconds), the other members consider it down and may trigger an election if the unreachable member was the primary.

**Interview Questions:**
- What is the default heartbeat interval and election timeout in a MongoDB replica set?
- How would you tune heartbeat/election timeouts for a cross-region replica set with higher network latency?

### Elections

Replica set elections use a Raft-inspired consensus protocol to select a new primary from the eligible secondaries. Members vote based on factors including data freshness (highest priority to the member with the most recent oplog entries), configured member `priority`, and network connectivity.

```javascript
// Configure member priority to influence election outcomes
cfg = rs.conf()
cfg.members[0].priority = 2   // prefer this member as primary
rs.reconfig(cfg)
```

**Interview Questions:**
- What factors determine which secondary is elected as the new primary?
- How can you configure a replica set member to never become primary?
- What is a rollback and when can it occur after an election?

### Disaster Recovery Concepts

Disaster recovery for MongoDB combines replica sets (for automatic failover within/across data centers), regular backups (logical `mongodump`/`mongorestore` or filesystem/volume snapshots), and point-in-time recovery via oplog replay. Multi-region replica set deployments protect against full data-center outages, while backups protect against logical corruption or accidental deletes that replication would otherwise faithfully propagate.

**Advantages:**
- Replica sets handle hardware/node failures automatically
- Snapshot + oplog backups enable point-in-time recovery from logical errors

**Disadvantages:**
- Replication alone does not protect against accidental deletes or application bugs that corrupt data (they replicate too)
- Cross-region replicas add write latency due to majority write concerns

**Interview Questions:**
- Why is replication alone not sufficient as a disaster recovery strategy?
- How would you design a backup strategy that supports point-in-time recovery?
- What is the difference between a logical backup and a filesystem snapshot backup in MongoDB?

## Best Practices

### Schema Design Best Practices

MongoDB schema design should be driven by application query patterns rather than pure normalization — "data that is accessed together should be stored together." This means starting from the application's most frequent and performance-critical queries, then modeling documents to satisfy them with minimal lookups.

**Interview Questions:**
- What does "design for your queries, not your data" mean in MongoDB schema design?
- How would you approach schema design differently for MongoDB versus a relational database?

### Embedding vs Referencing

Embedding stores related data as nested sub-documents within a single parent document, favoring read performance and atomicity for data that is always accessed together. Referencing stores related data in a separate collection and links via an `_id`, favoring flexibility and avoiding document growth/duplication for data that is large, frequently changing, or shared across many parents.

**Differences:**

| Aspect | Embedding | Referencing |
|---|---|---|
| Read performance | Single query, no join | Requires `$lookup` or a second query |
| Data duplication | Higher (duplicated across parents) | None (single source of truth) |
| Document size growth | Can hit 16MB limit if unbounded | Unaffected by related data size |
| Atomic updates | Atomic within one document | Not atomic across documents (needs transaction) |
| Best for | 1-to-few, data accessed together | 1-to-many/many-to-many, independently updated data |

**Interview Questions:**
- What criteria would you use to decide between embedding and referencing for a one-to-many relationship?
- How does the 16MB document size limit influence this decision?
- How would you model a many-to-many relationship (e.g. students and courses) in MongoDB?

### Index Design

Effective index design follows the ESR (Equality, Sort, Range) rule for compound indexes: place equality-matched fields first, sort fields second, and range-filtered fields last. Every index should be justified by an actual query pattern — unused indexes only add write overhead and storage cost.

```javascript
// Query: find active orders for a customer, sorted by date, within a price range
db.orders.find({ customerId: 'c1', status: 'active', total: { $gte: 100 } }).sort({ orderDate: -1 })

// ESR-compliant compound index: Equality(customerId,status), Sort(orderDate), Range(total)
db.orders.createIndex({ customerId: 1, status: 1, orderDate: -1, total: 1 })
```

**Interview Questions:**
- What is the ESR rule and how does it guide compound index field ordering?
- How would you identify unused or redundant indexes in a production collection?
- What is index intersection and why shouldn't you rely on it instead of proper compound indexes?

### Collection Design

Collection design decisions include when to split data into multiple collections versus a single collection with a discriminator field, how to handle time-series data (using time-series collections since MongoDB 5.0), and how to avoid unbounded collection growth patterns that hurt performance.

**Interview Questions:**
- When would you use MongoDB's native time-series collections instead of a manually bucketed schema?
- What are the trade-offs of using a single collection with a `type` discriminator field versus multiple collections?

### Shard Key Selection

The shard key determines how data is distributed across shards in a sharded cluster, and it cannot be changed (prior to resharding support) once chosen, making it one of the most consequential early decisions. A good shard key has high cardinality, even distribution of writes, and aligns with common query patterns to avoid scatter-gather queries.

**Advantages of a good shard key:**
- Even data and write distribution across shards
- Queries can be targeted to specific shards instead of broadcasting to all

**Disadvantages of a poor shard key:**
- Hotspots on a single shard (monotonically increasing keys like timestamps or ObjectIds)
- Scatter-gather queries across all shards, hurting performance

**Interview Questions:**
- What makes a good versus a bad shard key choice?
- Why is a monotonically increasing field like a timestamp often a poor shard key on its own?
- What is resharding and when would you need it?

### Performance Optimization

Performance tuning in MongoDB revolves around ensuring queries use appropriate indexes (verified with `explain()`), keeping the working set within available RAM/WiredTiger cache, using projections to limit returned fields, and using the aggregation pipeline efficiently (filtering early with `$match` before expensive stages).

```javascript
// Analyze query execution to check for collection scans (COLLSCAN)
db.orders.find({ status: 'active' }).explain('executionStats')
```

**Interview Questions:**
- How would you use `explain()` to diagnose a slow query?
- What is the significance of placing `$match` early in an aggregation pipeline?
- How does working-set size relative to available memory affect performance?

### Security Best Practices

MongoDB production security best practices include enabling authentication and role-based access control (RBAC), enabling TLS/SSL for data in transit, encrypting data at rest, restricting network access via firewalls/VPC peering and binding to specific IPs, enabling auditing, and following the principle of least privilege for database users/roles.

**Interview Questions:**
- What steps would you take to secure a MongoDB deployment before going to production?
- What is the principle of least privilege and how does it apply to MongoDB user roles?
- How does field-level or client-side field level encryption differ from encryption at rest?

## Concepts for Spring Data MongoDB

### Object-Document Mapping (ODM)

Object-Document Mapping is the process Spring Data MongoDB uses to automatically convert between Java objects (POJOs annotated with `@Document`) and BSON documents stored in MongoDB. The `MappingMongoConverter` handles this translation, using field names, type information, and custom converters to map Java types to their BSON representation and back.

```java
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private String customerName;
    private BigDecimal total;
    private List<OrderItem> items;
    // getters/setters
}
```

**Interview Questions:**
- How does Spring Data MongoDB convert between a Java object and a BSON document?
- What is `MappingMongoConverter` and when would you register a custom converter?

### Entity vs Document

In Spring Data MongoDB terminology, an "entity" is the Java class representing your domain model, while a "document" is the actual BSON representation stored in MongoDB. The `@Document` annotation marks a Java class as mapped to a MongoDB collection, and field-level annotations (`@Field`, `@Id`) fine-tune how entity properties map to document fields.

```java
@Document(collection = "products")
public class Product {
    @Id
    private String id;

    @Field("product_name")
    private String name;
}
```

**Interview Questions:**
- What is the purpose of the `@Document` annotation and what happens if you omit the collection name?
- How would you map a Java field name to a differently named document field?

### Mapping Nested Documents

Nested Java objects (POJOs referenced as fields) are automatically mapped by Spring Data MongoDB into embedded BSON sub-documents, without requiring any special annotation — this naturally models the embedding pattern for one-to-few relationships.

```java
public class Address {
    private String street;
    private String city;
}

@Document(collection = "customers")
public class Customer {
    @Id
    private String id;
    private String name;
    private Address address; // embedded as a nested document
}
```

```json
{ "_id": "c1", "name": "Jane", "address": { "street": "1 Main St", "city": "NYC" } }
```

**Interview Questions:**
- How does Spring Data MongoDB decide whether to embed a nested object versus store a reference?
- What happens when you query using a property path into a nested document (e.g. `address.city`)?

### Mapping Collections

Java `List`/`Set`/arrays of simple types or nested objects are mapped directly to BSON arrays. Spring Data MongoDB also supports mapping a `Map<String, T>` to a BSON sub-document with dynamic keys, useful for the Attribute Pattern.

```java
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private List<String> tags;
    private Map<String, Object> attributes;
}
```

**Interview Questions:**
- How would you map a Java `Map` field to a MongoDB document and what are the query implications?
- What are the trade-offs of storing a `List<NestedObject>` versus a separate referenced collection?

### Embedded Documents vs References

Spring Data MongoDB does not provide a native "join" like JPA — relationships modeled with `@DBRef` (or manually stored ID fields) require an additional query or `$lookup` aggregation stage, whereas embedded objects are fetched as part of the parent document with zero extra queries.

**Differences:**

| Aspect | Embedded Document | `@DBRef` / Manual Reference |
|---|---|---|
| Fetch cost | Included in parent read | Extra query per reference (lazy or eager) |
| Data consistency | Duplicated if repeated across parents | Single source of truth |
| Transactions needed for updates | No (atomic within document) | Yes, for multi-document consistency |
| Recommended for | 1-to-few, tightly coupled data | 1-to-many/many-to-many, independently managed entities |

```java
// @DBRef reference example (generally discouraged in favor of manual ID + repository lookup)
@Document(collection = "orders")
public class Order {
    @Id
    private String id;

    @DBRef
    private Customer customer;
}
```

**Interview Questions:**
- Why is `@DBRef` generally discouraged in favor of manually storing an ID and querying explicitly?
- What are the performance implications of lazy `@DBRef` resolution?

### ObjectId Mapping

MongoDB's native `ObjectId` type can be mapped directly to a Java `org.bson.types.ObjectId` field, or converted to/from a `String` when annotated with `@Id` on a `String` field — Spring Data MongoDB handles the conversion automatically as long as the `String` value is a valid 24-character hex ObjectId.

```java
@Document(collection = "orders")
public class Order {
    @Id
    private String id; // stored as ObjectId in MongoDB, exposed as String in Java
}
```

**Interview Questions:**
- How does Spring Data MongoDB convert between `ObjectId` and a Java `String` `@Id` field?
- What happens if you assign a non-ObjectId-format string to an `@Id` field?

### Custom ID Strategies

Instead of relying on MongoDB's auto-generated `ObjectId`, applications can define custom identifier strategies — using a business key (e.g. SKU, email), a UUID, or an application-managed sequence (simulated via a counters collection, since MongoDB has no native auto-increment).

```java
@Document(collection = "products")
public class Product {
    @Id
    private String sku; // business key used as the identifier instead of ObjectId
}
```

**Interview Questions:**
- How would you implement an auto-incrementing numeric ID in MongoDB, given it has no native sequence support?
- What are the trade-offs of using a business key as `_id` versus a generated `ObjectId`?

### Optimistic Locking Concepts

Spring Data MongoDB supports optimistic locking via the `@Version` annotation: on each update, the driver checks that the version field in the database still matches the version in the entity being saved, and increments it. If another process updated the document in the meantime, an `OptimisticLockingFailureException` is thrown, preventing lost updates.

```java
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;

    @Version
    private Long version;

    private BigDecimal balance;
}
```

**Advantages:**
- Prevents lost updates without holding database locks
- Lightweight compared to pessimistic locking

**Disadvantages:**
- Requires the application to handle retries on version conflicts
- Only protects the single document carrying the `@Version` field

**Interview Questions:**
- How does `@Version`-based optimistic locking work in Spring Data MongoDB?
- What exception is thrown on a version conflict and how should the application handle it?
- How does optimistic locking compare to using MongoDB transactions for concurrency control?

### Auditing Concepts

Spring Data MongoDB's auditing support (`@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`) automatically populates timestamp and user-tracking fields when documents are inserted or updated, once `@EnableMongoAuditing` is enabled on a configuration class.

```java
@Configuration
@EnableMongoAuditing
public class MongoConfig { }

@Document(collection = "orders")
public class Order {
    @Id
    private String id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
```

**Interview Questions:**
- What annotation enables automatic auditing support in a Spring Boot application?
- How would you populate `@CreatedBy`/`@LastModifiedBy` with the current authenticated user?

### Lazy vs Eager References (Concept)

When using `@DBRef(lazy = true)`, Spring Data MongoDB returns a proxy object for the referenced entity and only issues the query to fetch it when a property is first accessed, deferring the cost. Eager (default) `@DBRef` resolves the reference immediately when the parent document is loaded.

**Differences:**

| Aspect | Eager `@DBRef` (default) | Lazy `@DBRef(lazy = true)` |
|---|---|---|
| Fetch timing | Immediately with parent | On first access via proxy |
| Extra queries | Always incurred | Only if referenced object is actually used |
| Risk | N+1 queries when loading collections of parents | `LazyInitializationException`-style issues outside a session/proxy context |

**Interview Questions:**
- What is the difference between lazy and eager `@DBRef` resolution?
- Why is `@DBRef` (lazy or eager) generally less recommended than manual reference resolution in Spring Data MongoDB?

### Repository Pattern (Concept)

Spring Data MongoDB's repository abstraction (`MongoRepository`) lets you define an interface and get CRUD and query-derivation methods (e.g. `findByCustomerNameAndStatus`) generated automatically at runtime, without writing implementation code. For queries that don't fit method-name derivation, `@Query` with a raw MongoDB query string can be used.

```java
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByCustomerNameAndStatus(String customerName, String status);

    @Query("{ 'total': { $gte: ?0 } }")
    List<Order> findOrdersAboveTotal(BigDecimal minTotal);
}
```

**Interview Questions:**
- How does Spring Data MongoDB generate query implementations from repository method names?
- When would you use `@Query` instead of a derived query method?

### MongoTemplate (Concept)

`MongoTemplate` is the lower-level, imperative API in Spring Data MongoDB, offering full control over queries, updates, and aggregations using the `Query`/`Criteria` and `Aggregation` builder classes. It's used when repository method derivation isn't expressive enough, or when building queries dynamically at runtime.

```java
Query query = new Query(Criteria.where("status").is("active").and("total").gte(new BigDecimal("100")));
List<Order> orders = mongoTemplate.find(query, Order.class);

Update update = new Update().set("status", "shipped");
mongoTemplate.updateFirst(query, update, Order.class);
```

**Differences:**

| Aspect | `MongoRepository` | `MongoTemplate` |
|---|---|---|
| Style | Declarative (interface + derived/`@Query` methods) | Imperative (explicit Query/Update/Aggregation objects) |
| Best for | Standard CRUD and simple queries | Dynamic/complex queries built at runtime, bulk operations, aggregations |
| Boilerplate | Minimal | More verbose but more flexible |

**Interview Questions:**
- When would you choose `MongoTemplate` over a `MongoRepository` interface?
- How would you build a dynamic query with an unknown number of optional filter criteria using `MongoTemplate`?

### Aggregation Pipeline Concepts

Spring Data MongoDB exposes the native aggregation framework through the `Aggregation` builder class, letting you compose stages (`match`, `group`, `project`, `lookup`, `sort`, `unwind`) in type-safe Java code that gets translated into the equivalent MongoDB aggregation pipeline.

```java
Aggregation agg = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("status").is("active")),
    Aggregation.group("customerId").sum("total").as("totalSpent"),
    Aggregation.sort(Sort.Direction.DESC, "totalSpent")
);

AggregationResults<CustomerSpend> results =
    mongoTemplate.aggregate(agg, "orders", CustomerSpend.class);
```

**Interview Questions:**
- How do you translate a raw MongoDB aggregation pipeline into Spring Data MongoDB's `Aggregation` builder API?
- How would you perform the equivalent of a SQL join using Spring Data MongoDB's aggregation support?

### Transactions with Spring Data MongoDB

Since MongoDB 4.0 (replica sets) and 4.2 (sharded clusters), multi-document ACID transactions are supported, and Spring Data MongoDB exposes them through `MongoTransactionManager` combined with Spring's standard `@Transactional` annotation, or programmatically via `TransactionTemplate`.

```java
@Configuration
public class MongoTransactionConfig {
    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}

@Service
public class TransferService {
    @Transactional
    public void transferFunds(String fromId, String toId, BigDecimal amount) {
        accountRepository.decrementBalance(fromId, amount);
        accountRepository.incrementBalance(toId, amount);
    }
}
```

**Advantages:**
- Guarantees atomicity across multiple documents/collections
- Familiar `@Transactional` programming model for developers coming from JPA

**Disadvantages:**
- Requires a replica set or sharded cluster (not standalone)
- Higher latency and lock contention compared to single-document atomic updates
- Long-running transactions can hold resources and hurt throughput

**Interview Questions:**
- What MongoDB deployment topology is required to use multi-document transactions?
- How do you configure `@Transactional` support for MongoDB in a Spring Boot application?
- When should you prefer redesigning a schema to avoid needing a transaction versus actually using one?

### Reactive MongoDB Support

Spring Data MongoDB provides a fully reactive, non-blocking API (`ReactiveMongoRepository`, `ReactiveMongoTemplate`) built on Project Reactor (`Mono`/`Flux`) and the reactive streams MongoDB driver, suited for reactive Spring WebFlux applications that need to avoid blocking threads while waiting on I/O.

```java
public interface ReactiveOrderRepository extends ReactiveMongoRepository<Order, String> {
    Flux<Order> findByStatus(String status);
}

@RestController
public class OrderController {
    @GetMapping("/orders/{status}")
    public Flux<Order> getOrders(@PathVariable String status) {
        return orderRepository.findByStatus(status);
    }
}
```

**Differences:**

| Aspect | `MongoRepository` (blocking) | `ReactiveMongoRepository` |
|---|---|---|
| Threading model | Blocking, one thread per request | Non-blocking, event-loop based |
| Return types | `List<T>`, `T`, `Optional<T>` | `Flux<T>`, `Mono<T>` |
| Best paired with | Spring MVC | Spring WebFlux |
| Transactions | `@Transactional` (imperative) | `ReactiveMongoTransactionManager` |

**Interview Questions:**
- What are the key differences between `MongoRepository` and `ReactiveMongoRepository`?
- When would reactive MongoDB support provide a real throughput benefit versus the blocking API?
- How do you handle multi-document transactions in a reactive Spring Data MongoDB application?

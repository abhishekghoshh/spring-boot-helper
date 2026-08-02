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
- What is MongoDB and how does it differ from traditional databases? — MongoDB is a document-oriented NoSQL database that stores data as flexible, JSON-like BSON documents rather than rows and columns, enabling schema flexibility and horizontal scaling via sharding, whereas traditional relational databases enforce fixed schemas and rely primarily on vertical scaling.
- What problems was MongoDB designed to solve? — MongoDB was designed to handle large volumes of unstructured or semi-structured data with high availability and horizontal scalability, addressing the rigidity and scaling difficulty of traditional relational databases for rapidly evolving, high-throughput applications.
- What are the core building blocks of MongoDB (database, collection, document)? — A MongoDB deployment is organized into databases, which contain collections (analogous to tables), which in turn contain documents (analogous to rows) stored as BSON key-value structures.
- When would you NOT choose MongoDB for a project? — MongoDB is a poor fit when the application requires complex multi-table joins with strict referential integrity, such as heavy financial reporting systems, or when data is inherently tabular with a stable, well-defined schema best served by a relational database.

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
- What are the main categories of NoSQL databases? — The four common categories are document stores (MongoDB), key-value stores (Redis), wide-column stores (Cassandra), and graph databases (Neo4j).
- How does the CAP theorem relate to NoSQL database design? — Per the CAP theorem, a distributed system can only guarantee two of Consistency, Availability, and Partition tolerance simultaneously, and most NoSQL systems relax strict consistency in favor of availability and partition tolerance, unlike relational databases which typically prioritize consistency.
- Why might a team pick a NoSQL database over a relational one? — Teams pick NoSQL databases like MongoDB for flexible schemas that support rapid iteration, horizontal scalability for large or fast-changing datasets, and suitability for use cases like content management, real-time analytics, or IoT ingestion.
- Can NoSQL databases guarantee ACID transactions? Explain with MongoDB as an example. — Many NoSQL databases traditionally relax ACID guarantees for availability, but modern MongoDB supports full ACID multi-document transactions, allowing consistent, atomic updates across multiple documents and collections when needed.

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
- What does "document-oriented" mean in the context of MongoDB? — It means data is stored as self-contained BSON documents that can contain nested objects and arrays, with each document able to have its own structure rather than requiring identical fields across the collection.
- How does storing nested data in one document compare to normalizing it across tables? — Storing nested data in one document avoids joins by co-locating related data for single-read retrieval, whereas normalizing across tables reduces duplication but requires joins to reconstruct related data at query time.
- What are the trade-offs of embedding related data inside a single document? — Embedding improves read performance and atomicity for data accessed together, but can lead to document growth, duplication across documents, and more complex updates when the embedded data changes independently of its parent.

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
- Why did MongoDB choose BSON over plain JSON internally? — BSON extends JSON's data model with additional types like Date, ObjectId, Binary Data, and Decimal128, and its binary, length-prefixed encoding is faster to parse and traverse than text-based JSON.
- What extra data types does BSON support that JSON does not? — BSON adds Date, ObjectId, Binary Data, Decimal128, distinct Int32/Int64 integer types, and Timestamp, none of which exist natively in JSON.
- Is BSON always more compact than JSON? Explain. — Not always; BSON can be slightly larger on disk than equivalent JSON text due to embedded type and length metadata, but it trades a small size overhead for significantly faster parsing and traversal.

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
- What is the role of `mongod` versus `mongos`? — `mongod` is the core database process that manages data storage, indexing, and replication, while `mongos` is a lightweight query router used in sharded clusters to direct client requests to the appropriate shard(s).
- How does data flow from a client write request to disk in a replica set? — A client sends a write to the primary node, which applies it to its data set and records the operation in its oplog; secondaries then asynchronously replicate and apply that oplog entry to reach eventual consistency.
- What is the purpose of config servers in a sharded cluster? — Config servers store the cluster's metadata, including the mapping of shard key ranges to shards, which `mongos` routers use to determine where to route queries and writes.
- What storage engine does MongoDB use by default and what does it provide? — MongoDB uses the WiredTiger storage engine by default, which provides document-level concurrency control, compression, and efficient on-disk data structures.

### MongoDB Use Cases

MongoDB is well-suited for content management systems, product catalogs, user profiles, real-time analytics, IoT sensor data, and mobile/web application backends where data shapes evolve frequently. Its flexible schema and horizontal scalability make it a good fit for rapidly growing startups and applications with large write throughput. It is less ideal for workloads that require complex multi-table joins with strict referential integrity, such as heavy financial reporting systems traditionally built on relational databases.

**Interview Questions:**
- What kinds of applications benefit most from MongoDB? — Content management systems, product catalogs, user profiles, real-time analytics, IoT sensor data, and mobile/web backends benefit most, since these workloads have frequently evolving data shapes and require high write throughput and horizontal scale.
- Give an example of a workload where MongoDB would be a poor fit. — Heavy financial reporting systems requiring complex multi-table joins and strict referential integrity across normalized tables are traditionally better served by a relational database.
- How does schema flexibility influence MongoDB's suitability for a use case? — Schema flexibility lets applications add or change fields without expensive migrations, making MongoDB well suited to rapidly evolving applications, but less suited to workloads needing strict, enforced structure across all records.

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
- What are the core modeling differences between MongoDB and a relational database? — Relational databases normalize data into tables connected by foreign keys and joins with a fixed, DDL-enforced schema, whereas MongoDB models data as denormalized documents within collections, using embedding or manual references instead of joins, with an optionally validated flexible schema.
- When would you choose a relational database over MongoDB? — A relational database is preferable when the application needs complex, ad hoc multi-table joins, strict referential integrity, and a stable, well-understood tabular schema, such as in traditional accounting or ERP systems.
- How do joins in SQL compare to `$lookup` in MongoDB's aggregation framework? — SQL joins are a native, highly optimized relational operation performed across normalized tables, while `$lookup` is an aggregation pipeline stage that performs a left outer join-like operation between collections, generally less optimized for very large cross-collection joins than native SQL joins.

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
- What is the difference between MongoDB Community and Enterprise editions? — Community Edition is the free, open-source core database engine with replication and sharding, while Enterprise Advanced adds features like LDAP/Kerberos authentication, auditing, an in-memory storage engine, encryption at rest with KMIP integration, and Ops Manager, backed by official support.
- What advanced security features does Enterprise Edition add? — Enterprise Edition adds LDAP and Kerberos authentication, auditing, and native encryption at rest with external key management (KMIP) integration, beyond Community's basic SCRAM and x.509 authentication.
- How does MongoDB Atlas relate to Community and Enterprise editions? — Atlas is MongoDB's fully managed cloud database service, built on Enterprise-grade features, which removes the operational burden of provisioning, patching, and maintaining servers that self-hosted Community or Enterprise deployments require.

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
- What is `mongod` and what is its role? — `mongod` is the core MongoDB database process responsible for handling client data requests, managing on-disk data storage, and performing background operations like replication.
- How would you install and start MongoDB locally on your OS of choice? — On macOS, MongoDB can be installed via Homebrew with `brew tap mongodb/brew`, `brew install mongodb-community@7.0`, and started with `brew services start mongodb-community@7.0`; other OSes provide equivalent package manager installs or Docker images.
- What are common configuration options you would set for a production `mongod` instance? — Common production settings include the data directory path (`storage.dbPath`), bind IP and port (`net.bindIp`, `net.port`), enabling authentication (`security.authorization`), and the replica set name (`replication.replSetName`).

### MongoDB Shell (mongosh)

`mongosh` is the modern, official command-line shell for interacting with MongoDB, offering a JavaScript-based REPL for running queries, administrative commands, and scripts. It replaced the legacy `mongo` shell and adds improved syntax highlighting, auto-completion, and better error messages. It's commonly used for ad-hoc queries, debugging, and running maintenance scripts.

```javascript
mongosh "mongodb://localhost:27017"
show dbs
use myAppDb
db.users.find({ age: { $gt: 25 } }).limit(5)
```

**Interview Questions:**
- What is `mongosh` and how does it differ from the legacy `mongo` shell? — `mongosh` is the modern, official JavaScript-based command-line shell for MongoDB, replacing the legacy `mongo` shell with improved syntax highlighting, auto-completion, and clearer error messages.
- How do you connect to a remote MongoDB instance using `mongosh`? — You connect by passing a connection string URI, such as `mongosh "mongodb://<host>:27017"` or an Atlas SRV connection string with credentials, to the `mongosh` command.
- What are some common `mongosh` commands you use for troubleshooting? — Common troubleshooting commands include `show dbs`, `use <db>`, `db.collection.find()`, `db.collection.explain()`, and `db.serverStatus()` to inspect data, query plans, and server health.

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
- What is MongoDB Compass used for? — Compass is the official GUI for visually exploring databases, collections, and documents, building and testing queries and aggregation pipelines, and analyzing schema and index/query performance.
- How can Compass help when designing or debugging an aggregation pipeline? — Compass provides an interactive pipeline builder that shows stage-by-stage output previews, making it easier to iteratively construct and debug complex aggregation pipelines visually.
- What are the limitations of using Compass compared to the shell for automation? — Compass is not scriptable like `mongosh`, so it can't be used for automated tasks or CI/CD scripts, and it adds a GUI dependency unsuited for headless server environments.

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
- What is MongoDB Atlas and what problems does it solve? — Atlas is MongoDB's fully managed Database-as-a-Service, available on AWS, Azure, and GCP, that eliminates the operational overhead of provisioning, patching, backing up, and scaling self-managed replica sets and sharded clusters.
- What managed features does Atlas provide beyond a plain MongoDB server? — Atlas provides automated backups and patching, built-in monitoring/alerting and a performance advisor, global multi-region cluster support, and additional services like Atlas Search, Data Federation, and Triggers.
- What trade-offs exist between self-hosting MongoDB and using Atlas? — Self-hosting offers more control over low-level server configuration but requires operational effort for provisioning and maintenance, while Atlas removes that burden at the cost of an ongoing subscription fee and reduced low-level control.

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
- What are the key sections of a `mongod.conf` file? — Key sections include `storage` (e.g., `dbPath`), `net` (port and bindIp), `security` (authorization), and `replication` (replSetName), each configuring a different aspect of the server.
- How do you enable authentication/authorization on a MongoDB instance? — Authentication/authorization is enabled by setting `security.authorization: enabled` in the config file (or the `--auth` flag), which requires clients to authenticate with valid credentials before performing operations.
- Why is restricting `bindIp` important for security? — Restricting `bindIp` limits which network interfaces `mongod` listens on, preventing the instance from being reachable over public or untrusted networks, which reduces the attack surface if authentication is misconfigured.

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
- What is the role of a MongoDB driver in an application? — A MongoDB driver translates native language calls into MongoDB's wire protocol operations, handling connection pooling, serialization/deserialization between language objects and BSON, and communication with the server.
- How does Spring Data MongoDB relate to the underlying MongoDB Java driver? — Spring Data MongoDB is a higher-level abstraction built on top of the MongoDB Java driver, providing repository interfaces, `MongoTemplate`, and object mapping between POJOs and BSON documents, while the driver handles the low-level wire protocol communication.
- What is the difference between using `MongoRepository` and `MongoTemplate` in Spring Data MongoDB? — `MongoRepository` provides a declarative, interface-based approach with derived query methods for common CRUD operations, while `MongoTemplate` offers a lower-level, imperative API giving finer control over queries, updates, and aggregation pipelines.

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
- What is Atlas Search and what underlying technology powers it? — Atlas Search is a full-text search capability built into MongoDB Atlas, powered by Apache Lucene, enabling fuzzy matching, relevance scoring, autocomplete, and faceting without a separate search engine.
- How does the `$search` aggregation stage differ from a regular `$match` query? — `$search` leverages a dedicated Lucene-based search index to perform relevance-scored full-text search with features like fuzzy matching and autocomplete, whereas `$match` filters documents using standard MongoDB query operators against regular indexes without relevance scoring.
- When would you choose Atlas Search over a standalone search engine like Elasticsearch? — Atlas Search is preferable when you want full-text search capabilities integrated directly into your existing MongoDB Atlas cluster without operating and syncing a separate Elasticsearch deployment.

## Database Structure

### Databases

A MongoDB server can host multiple databases, each acting as an independent namespace containing its own set of collections. Databases are created implicitly the first time data is written to a collection within them (`use myDb` alone does not create the database until a write occurs). Each database has its own set of files on disk (per storage engine allocation) and can have its own users and access controls.

```javascript
use inventoryDb
db.products.insertOne({ name: "Widget" }) // creates inventoryDb on first write
show dbs
```

**Interview Questions:**
- When is a MongoDB database actually created on disk? — A database is only actually created once data is written to a collection within it; simply running `use myDb` switches context but does not persist the database until a write occurs.
- How are databases isolated from one another in terms of access control? — Databases can have their own users and roles scoped specifically to that database, allowing fine-grained access control so a user can be restricted to reading/writing only within particular databases.
- What built-in databases does MongoDB create by default (e.g., `admin`, `local`, `config`)? — MongoDB creates the `admin` database for authentication/authorization data, `local` for replication-related data like the oplog, and `config` for sharded cluster metadata.

### Collections

A collection is a grouping of documents, analogous to a table in a relational database, but without enforcing a rigid schema across its documents. Collections are created implicitly on first insert or explicitly via `createCollection()`, which also allows specifying options like schema validation rules, capped size, or collation. Indexes are defined per collection to optimize query performance.

```javascript
db.createCollection("orders", {
  validator: { $jsonSchema: { bsonType: "object", required: ["customerId", "items"] } }
})
```

**Interview Questions:**
- How does a MongoDB collection differ from a relational table? — A collection groups documents without enforcing a single rigid schema across them, unlike a relational table where every row must conform to the same fixed set of columns.
- What options can you specify when explicitly creating a collection? — When explicitly creating a collection with `createCollection()`, you can specify options like a JSON Schema validator, capped collection size and document count limits, and collation rules.
- What is a capped collection and when would you use one? — A capped collection is a fixed-size collection that automatically overwrites its oldest documents once it reaches its size limit, useful for high-throughput use cases like logging or caching recent events where insertion order matters.

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
- What role does the `_id` field play in a MongoDB document? — The `_id` field uniquely identifies a document within its collection, acts as the primary key, and is automatically indexed to enforce uniqueness and support fast lookups.
- What is the maximum size of a single BSON document and why does this limit exist? — The maximum BSON document size is 16MB, a limit chosen to prevent excessive use of RAM and network bandwidth for a single document and to encourage efficient data modeling.
- Can two documents in the same collection have completely different fields? Explain the implications. — Yes, MongoDB's dynamic schema allows documents in the same collection to have entirely different fields, which enables flexible, polymorphic data models but requires application code to handle missing or varying fields defensively.

### Fields

Fields are the key-value pairs that make up a document, similar to columns in a relational row, but not required to be consistent across documents in a collection. Field names are strings and values can be any BSON type, including nested documents and arrays. Field order is preserved in storage and retrieval but generally should not be relied upon logically.

**Interview Questions:**
- What data types can a field's value hold in MongoDB? — A field's value can be any BSON type, including strings, numbers, booleans, dates, ObjectIds, arrays, and nested embedded documents.
- Are field names case-sensitive in MongoDB? — Yes, field names in MongoDB are case-sensitive, so `Name` and `name` are treated as distinct fields.
- What restrictions exist on field names (e.g., leading `$`, dots)? — Field names cannot start with a `$` character and cannot contain the `.` character, since both are reserved for MongoDB's query operators and dot-notation path syntax.

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
- What is an embedded document and why would you use one? — An embedded document is a document nested within a field of another document, used to co-locate related data so it can be retrieved together in a single read, reducing the need for joins.
- What are the risks of embedding documents that grow unbounded over time? — Unbounded embedded arrays or documents can push a parent document toward the 16MB size limit, degrade update/read performance, and increase memory pressure, so embedding is best reserved for bounded, tightly related data.
- How deep can BSON document nesting go, and what practical limits should you consider? — BSON supports nesting up to 100 levels deep, but in practice documents should stay much shallower, since deep nesting complicates queries and indexing and increases the risk of approaching the 16MB document size limit.

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
- How does MongoDB index array fields (multikey indexes)? — MongoDB automatically creates a multikey index when an indexed field contains an array, creating a separate index entry for each element so queries can match any value within the array.
- What is the difference between `$push` and `$addToSet`? — `$push` appends a value to an array regardless of duplicates, while `$addToSet` only adds the value if it doesn't already exist in the array, effectively treating the array like a set.
- How would you query for documents where an array contains a specific value versus an element matching multiple conditions? — Use a simple equality match like `{ grades: 90 }` to find a specific value in the array, and `$elemMatch` (e.g., `{ grades: { $elemMatch: { $gte: 90 } } }`) when a single array element must satisfy multiple conditions simultaneously.

### Dynamic Schema

MongoDB's dynamic (flexible) schema means collections do not enforce a fixed structure by default — documents in the same collection can have different fields or types for the same field. This enables fast iteration during development since schema migrations aren't required to add new fields. However, uncontrolled flexibility can lead to inconsistent data, which is why MongoDB offers optional JSON Schema validation to enforce structure when needed.

**Advantages:**
- Fast iteration without downtime for schema migrations
- Naturally supports polymorphic and evolving data models

**Disadvantages:**
- Risk of inconsistent or malformed data without validation
- Application code must handle missing/optional fields defensively

**Interview Questions:**
- What does "dynamic schema" mean in MongoDB? — Dynamic schema means collections don't enforce a fixed structure by default, so documents within the same collection can have different fields or types for the same field name.
- How can you enforce structure on an otherwise schema-less collection? — You can enforce structure using MongoDB's optional JSON Schema validation, defined via the `validator` option on `createCollection()` or `collMod`, which rejects documents that don't match the specified rules.
- What are the risks of a fully dynamic schema in a large production system? — Without validation, a fully dynamic schema risks inconsistent or malformed data across documents, forcing application code to handle missing or unexpected fields defensively and complicating long-term data quality.

## BSON Data Types

### String

The `String` BSON type stores UTF-8 encoded text and is the most commonly used data type for textual data such as names, descriptions, and identifiers. MongoDB has no fixed length limit for strings other than the overall 16MB document size limit. String comparisons and sorting respect UTF-8 byte ordering by default unless a collation is specified.

```javascript
db.users.insertOne({ name: "Élise", bio: "Backend engineer" })
```

**Interview Questions:**
- How does MongoDB store and encode string data? — MongoDB stores string data as UTF-8 encoded text within BSON documents, with no fixed length limit beyond the overall 16MB document size cap.
- How can you perform case-insensitive or locale-aware string comparisons in MongoDB? — You can specify a collation (e.g., with strength settings for case-insensitivity) on a collection, index, or individual query/aggregation operation to perform locale-aware and case-insensitive string comparisons.
- Is there a length limit on string fields? — There's no explicit per-string length limit; the only practical constraint is the overall 16MB maximum BSON document size.

### Number Types

MongoDB supports several numeric BSON types: `Int32`, `Int64` (Long), and `Double`, each with different precision and storage size. By default, numbers entered in `mongosh` without a suffix are stored as `Double`, which can cause unexpected precision issues for large integers unless explicitly cast with `NumberInt()` or `NumberLong()`. Choosing the right numeric type matters for both storage efficiency and accurate arithmetic (especially for financial data, where `Decimal128` is often preferred).

```javascript
db.metrics.insertOne({
  views: NumberInt(1000),
  totalRevenueCents: NumberLong(9999999999)
})
```

**Interview Questions:**
- What numeric BSON types does MongoDB support and how do they differ? — MongoDB supports `Int32`, `Int64` (Long), `Double`, and `Decimal128`, differing in storage size and precision, with `Decimal128` providing exact base-10 precision needed for financial calculations that `Double` cannot guarantee.
- Why might you explicitly cast a number to `NumberLong` in mongosh? — Because numbers entered without a suffix in mongosh default to `Double`, explicitly casting with `NumberLong()` avoids floating-point precision issues for large integer values that need exact representation.
- Why is `Decimal128` sometimes preferred over `Double` for monetary values? — `Decimal128` represents decimal values exactly using base-10 floating point, avoiding the rounding errors inherent in `Double`'s binary floating-point representation, which is critical for accurate financial calculations.

### Boolean

The `Boolean` type stores `true` or `false` values and is commonly used for flags such as `isActive` or `isDeleted`. Booleans are frequently used in query filters and are efficiently indexed, though a boolean index alone is usually low-selectivity and often combined with other fields in a compound index.

```javascript
db.users.find({ isActive: true })
```

**Interview Questions:**
- When is it appropriate to index a boolean field? — Indexing a boolean field alone is rarely useful due to low cardinality; it's more appropriate as part of a compound index alongside higher-selectivity fields to narrow down result sets efficiently.
- Why is a standalone index on a low-cardinality boolean field often ineffective? — With only two possible values, a boolean index doesn't narrow down the candidate document set much, so the query planner often still needs to scan a large fraction of the index, providing little performance benefit over a collection scan.

### Date

The `Date` BSON type stores a 64-bit integer representing milliseconds since the Unix epoch (January 1, 1970 UTC), independent of timezone — timezone formatting is a client-side/display concern. Dates should always be stored using the `Date` type (not strings) to enable proper range queries, sorting, and use with TTL indexes for automatic document expiration.

```javascript
db.sessions.insertOne({ userId: 1, createdAt: new Date() })
db.sessions.createIndex({ createdAt: 1 }, { expireAfterSeconds: 3600 })
```

**Interview Questions:**
- How does MongoDB internally store `Date` values? — MongoDB stores `Date` values as a 64-bit integer representing milliseconds since the Unix epoch (January 1, 1970 UTC), independent of any timezone.
- Why should dates be stored as the `Date` type rather than as strings? — Storing dates as the `Date` type enables correct range queries, sorting, and arithmetic, and is required for TTL indexes to automatically expire documents, none of which work reliably with string-formatted dates.
- How do TTL indexes use `Date` fields to expire documents automatically? — A TTL index is created on a `Date` field with an `expireAfterSeconds` option, causing a background process to automatically delete documents once that date field is older than the specified number of seconds.

### ObjectId

`ObjectId` is a special 12-byte BSON type most commonly used as the default value for a document's `_id` field. It's designed to be generated efficiently in a distributed manner without a central coordinator while remaining roughly sortable by creation time. (See the dedicated ObjectId section below for structure details.)

**Interview Questions:**
- Why does MongoDB default to `ObjectId` for the `_id` field instead of an auto-increment integer? — `ObjectId` can be generated independently by clients or servers in a distributed system without needing a central coordinator to hand out sequential values, avoiding the bottleneck and coordination overhead of auto-increment counters.
- Is `ObjectId` guaranteed to be globally unique? Why or why not? — `ObjectId` is not mathematically guaranteed unique but is unique with an extremely high probability in practice, since it combines a timestamp, a random value, and an incrementing counter to minimize collision risk.

### Array

The `Array` BSON type stores an ordered list of values under a single field, and is one of the two composite BSON types (along with embedded documents). Arrays support mixed element types, though consistent typing is best practice for predictable querying. MongoDB automatically creates multikey indexes when an indexed field contains an array.

**Interview Questions:**
- What is a multikey index and how does it relate to array fields? — A multikey index is created automatically when MongoDB indexes a field containing an array, generating an index entry for each array element so queries can efficiently match individual values within it.
- What data types can be stored inside a single array field? — An array can hold a mix of BSON types, including primitives like strings and numbers as well as embedded documents, though consistent typing is best practice for predictable querying.

### Embedded Document

The `Embedded Document` (aka `Object`) BSON type allows a field's value to itself be a full BSON document, enabling nested, hierarchical data structures. This is the mechanism behind "embedding" in MongoDB's data modeling and is queried using dot notation (e.g., `address.city`).

```javascript
db.users.find({ "address.city": "Austin" })
```

**Interview Questions:**
- How do you query fields inside an embedded document? — You query fields inside an embedded document using dot notation, such as `{ "address.city": "Austin" }`, to match a value at a specific nested path.
- What is the practical nesting depth limit for embedded documents in MongoDB? — BSON technically supports nesting up to 100 levels, but practical data models should stay much shallower to keep queries, indexing, and updates manageable and to avoid approaching the 16MB document size limit.

### Null

The `Null` BSON type represents a deliberately empty or unknown value for a field, distinct from a field simply being absent from the document. Queries can distinguish between "field is null" and "field does not exist" using `$exists` combined with equality checks.

```javascript
db.users.find({ middleName: null })              // matches null OR missing field
db.users.find({ middleName: { $exists: true, $eq: null } }) // matches only explicit null
```

**Interview Questions:**
- What is the difference between a field being `null` versus not existing at all? — A field explicitly set to `null` is present in the document with a `Null` BSON value, while a missing field is entirely absent; a plain equality query like `{ field: null }` matches both cases, but `$exists` can distinguish between them.
- How would you query specifically for documents where a field is explicitly set to `null`? — Combine `$exists: true` with `$eq: null`, e.g. `{ middleName: { $exists: true, $eq: null } }`, to match only documents where the field is present and explicitly null, excluding documents where it's missing.

### Binary Data

The `Binary Data` (`BinData`) BSON type stores raw binary content such as images, files, or encrypted blobs directly within a document, subject to the overall 16MB document size limit. For larger files, MongoDB's GridFS specification splits data into chunks stored across multiple documents instead of using a single `BinData` field.

**Interview Questions:**
- When would you store binary data directly in a document versus using GridFS? — Store binary data directly as `BinData` when it's small and well within the 16MB document limit (e.g., thumbnails, small icons), and use GridFS when files exceed or approach that limit, such as large images, videos, or documents.
- What is GridFS and how does it work around the 16MB document size limit? — GridFS is a MongoDB specification that splits large files into smaller chunks stored as separate documents in a `chunks` collection, with metadata tracked in a `files` collection, allowing files far larger than 16MB to be stored and streamed back together.

### Timestamp

The BSON `Timestamp` type is an internal MongoDB type used primarily by the oplog for replication, consisting of a 32-bit seconds value and a 32-bit ordinal counter to disambiguate operations within the same second. It is distinct from the `Date` type and is generally not intended for use in application-level document fields.

**Interview Questions:**
- How does BSON `Timestamp` differ from BSON `Date`? — `Timestamp` is an internal type composed of a 32-bit seconds value plus a 32-bit ordinal counter used to order operations within the same second, whereas `Date` is a 64-bit millisecond value intended for application-level date/time storage.
- Where is the `Timestamp` type primarily used internally in MongoDB? — `Timestamp` is primarily used internally in the oplog to uniquely order replicated operations for replica set synchronization.

### Decimal128

`Decimal128` provides 128-bit decimal floating-point precision, avoiding the rounding errors inherent in binary floating-point (`Double`) representations. It is the recommended type for financial or monetary calculations that require exact decimal precision.

```javascript
db.invoices.insertOne({ amount: NumberDecimal("19.99") })
```

**Interview Questions:**
- Why is `Decimal128` preferred over `Double` for monetary values? — `Decimal128` uses exact base-10 decimal floating-point representation, avoiding the binary floating-point rounding errors that `Double` introduces, which is essential for accurate financial calculations.
- What precision does `Decimal128` provide compared to standard floating-point types? — `Decimal128` provides 128-bit decimal floating-point precision with up to 34 significant decimal digits, far exceeding the precision and exactness guarantees of a standard 64-bit `Double`.

### UUID

MongoDB can store universally unique identifiers using the `Binary` subtype 4 (UUID), often used when integrating with external systems that already generate UUIDs, or when a non-sequential, globally unique identifier is needed outside of `ObjectId`. Drivers typically provide native UUID type mapping for convenience.

**Interview Questions:**
- How is a UUID represented at the BSON level? — A UUID is represented as BSON `Binary` data with subtype 4, storing the 16-byte UUID value, with drivers typically providing native UUID type mapping for convenience.
- When might you use a UUID instead of the default `ObjectId` for `_id`? — You might use a UUID when integrating with external systems that already generate UUIDs, or when you need a globally unique identifier generated independently of MongoDB's own ID scheme.

### MinKey and MaxKey

`MinKey` and `MaxKey` are special BSON types that compare lower than and higher than all other BSON values, respectively, regardless of type. They're primarily used internally for sharding range boundaries and occasionally in queries to bound comparisons across mixed-type fields.

**Interview Questions:**
- What are `MinKey` and `MaxKey` used for in MongoDB? — `MinKey` and `MaxKey` are special values that compare lower than and higher than all other BSON values respectively, used internally for defining open-ended sharding range boundaries and occasionally in queries that need to bound comparisons across mixed types.
- How does sharding use `MinKey`/`MaxKey` for chunk range boundaries? — Sharding uses `MinKey` and `MaxKey` to represent the unbounded lower and upper edges of the very first and last chunk ranges for a shard key, ensuring every possible value is covered by some chunk.

### Regular Expression

The `Regular Expression` BSON type stores a pattern that can be used directly in queries for pattern matching, equivalent to using the `$regex` operator. Regex queries can leverage indexes efficiently only when the pattern is left-anchored (e.g., `^prefix`); unanchored patterns typically require a full collection scan.

```javascript
db.products.find({ sku: { $regex: /^AB-/ } })
```

**Interview Questions:**
- Under what conditions can a regex query use an index efficiently? — A regex query can use an index efficiently only when the pattern is left-anchored (e.g., `^AB-`), allowing the index to be scanned as a range; unanchored or case-insensitive patterns generally force a full collection or full index scan.
- What is the difference between storing a BSON regex versus using `$regex` in a query filter? — A stored BSON regex is a document field value containing a pattern for later matching, while `$regex` in a query filter is an operator applied at query time to match string fields against a pattern.

## ObjectId

### Structure of ObjectId

An `ObjectId` is a 12-byte value composed of a 4-byte timestamp (seconds since Unix epoch), a 5-byte random value unique to a machine/process, and a 3-byte incrementing counter, initialized to a random value. This structure guarantees a very high probability of uniqueness across distributed inserts without requiring coordination between servers, while still being roughly sortable by creation time due to the leading timestamp.

```mermaid
flowchart LR
    A["4 bytes: Timestamp"] --> B["5 bytes: Random value"] --> C["3 bytes: Counter"]
```

**Interview Questions:**
- What are the three components that make up an `ObjectId`? — An `ObjectId` is composed of a 4-byte timestamp (seconds since the Unix epoch), a 5-byte random value unique to a machine/process, and a 3-byte incrementing counter initialized to a random value.
- Why is the timestamp placed as the leading bytes of an `ObjectId`? — Placing the timestamp first makes `ObjectId` values roughly sortable by creation time when compared or indexed as byte strings, since earlier-created documents naturally sort before later ones.
- How does the random value component help avoid collisions across different machines? — The 5-byte random value is unique per machine/process, so even if two processes generate an `ObjectId` within the same second, they are extremely unlikely to produce identical values, avoiding coordination between distributed nodes.

### Automatic ID Generation

If a document is inserted without an explicit `_id` field, the MongoDB driver automatically generates an `ObjectId` client-side before sending the insert to the server. This client-side generation avoids a round trip to the server just to obtain an ID and allows the application to know the ID immediately after calling insert.

```javascript
const result = db.users.insertOne({ name: "Eve" })
print(result.insertedId) // auto-generated ObjectId
```

**Interview Questions:**
- Where is the default `ObjectId` generated — on the client/driver or the server? — The default `ObjectId` is generated client-side by the driver before the insert is sent to the server, avoiding an extra round trip just to obtain an ID.
- What is the benefit of generating the `_id` before sending the insert to the server? — Generating the `_id` client-side lets the application know the document's identifier immediately after calling insert, without waiting for a server round trip, and allows the ID to be used in related operations right away.

### Custom IDs

Applications can supply their own value for `_id` instead of relying on the auto-generated `ObjectId`, as long as the value is unique within the collection. Common alternatives include natural keys (e.g., an email or SKU), UUIDs, or application-specific sequence numbers. Using a custom `_id` avoids a separate unique index if the natural key is already guaranteed unique and frequently queried.

```javascript
db.products.insertOne({ _id: "SKU-1001", name: "Keyboard" })
```

**Interview Questions:**
- Can you use your own value for `_id` instead of `ObjectId`? What are the constraints? — Yes, any BSON value can be used for `_id` as long as it is unique within the collection; common choices include natural keys like an email or SKU, UUIDs, or application-specific sequence numbers.
- What are the trade-offs of using a natural key as `_id` versus an auto-generated `ObjectId`? — A natural key avoids needing a separate unique index and can simplify lookups by that key, but it requires the application to guarantee uniqueness itself and may not be as compact or naturally sortable by creation time as `ObjectId`.
- What happens if you try to insert a document with a duplicate `_id`? — MongoDB rejects the insert with a duplicate key error, since `_id` is automatically indexed with a unique constraint.

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
- What advantages does `ObjectId` provide over a simple auto-incrementing integer in a distributed system? — `ObjectId` can be generated independently on any client or server node without a central coordinator handing out sequential values, avoiding the bottleneck and single point of failure an auto-incrementing counter would introduce in a distributed system.
- How can you derive a document's approximate creation timestamp from its `ObjectId`? — Since the leading 4 bytes of an `ObjectId` encode seconds since the Unix epoch, calling a method like `.getTimestamp()` on the `ObjectId` extracts that value to give the document's approximate creation time.
- Why is `ObjectId` more compact than a UUID string representation? — `ObjectId` is a fixed 12-byte binary value, while a UUID is typically 16 bytes raw but commonly represented as a 36-character hyphenated string, making `ObjectId` more compact both in storage and as a textual representation.

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
- What is the difference between `insertOne()` and `insertMany()`? — `insertOne()` adds a single document in one call, while `insertMany()` adds an array of documents in a single request, which is more efficient than issuing multiple `insertOne()` calls.
- What does the `ordered` option control during a bulk insert? — The `ordered` option controls whether `insertMany()` stops at the first failed document (default, `ordered: true`) or continues attempting to insert the remaining documents after an error (`ordered: false`).
- What happens if you attempt to insert a document with a duplicate `_id`? — MongoDB rejects that specific insert with a duplicate key error; with `ordered: true` this halts the remaining batch, while `ordered: false` allows subsequent documents to still be inserted.

### Read Operations

Read operations retrieve documents using `find()` (returns a cursor over multiple matching documents) or `findOne()` (returns the first match). Query filters use MongoDB Query Language operators (`$eq`, `$gt`, `$in`, etc.), and results can be shaped further with projections, sorting, and pagination via `sort()`, `limit()`, and `skip()`.

```javascript
db.orders.find({ total: { $gte: 50 } }, { customerId: 1, total: 1, _id: 0 })
  .sort({ total: -1 })
  .limit(10)
```

**Interview Questions:**
- What is the difference between `find()` and `findOne()`? — `find()` returns a cursor over all documents matching the filter, allowing further chaining like `sort()` and `limit()`, while `findOne()` returns only the first matching document directly (or null if none match).
- How do projections improve query performance and network efficiency? — Projections limit the fields returned from a query to only what's needed, reducing the amount of data transferred over the network and the memory/CPU needed to serialize and deserialize documents.
- Why can `skip()` become inefficient for pagination over large datasets, and what's an alternative? — `skip()` still requires the server to scan and discard all skipped documents before returning results, making it increasingly slow at large offsets; a more efficient alternative is range-based (keyset) pagination using a query filter on the last seen sort key, such as `{ _id: { $gt: lastId } }`.

### Update Operations

Update operations modify existing documents using `updateOne()`, `updateMany()`, or `findOneAndUpdate()`, typically applying update operators like `$set`, `$inc`, `$unset`, or `$push` rather than replacing the whole document. By default, only matched fields specified with update operators are changed — other fields remain untouched.

```javascript
db.orders.updateMany(
  { status: "PENDING" },
  { $set: { status: "PROCESSING" }, $currentDate: { updatedAt: true } }
)
```

**Interview Questions:**
- What is the difference between `updateOne()`, `updateMany()`, and `findOneAndUpdate()`? — `updateOne()` modifies the first matching document, `updateMany()` modifies all matching documents, and `findOneAndUpdate()` updates a single matching document while also atomically returning either its pre- or post-update state.
- What is the difference between using `$set` versus passing a plain replacement document to `updateOne()`? — Using `$set` only modifies the specified fields and leaves the rest of the document untouched, whereas passing a plain document without operators replaces the entire matched document (equivalent to `replaceOne()`), removing any fields not included.
- How would you atomically increment a counter field on a document? — Use the `$inc` update operator, e.g. `{ $inc: { count: 1 } }`, which atomically increments the field's numeric value on the server without a separate read-modify-write cycle.

### Delete Operations

Delete operations remove documents using `deleteOne()` (removes the first match) or `deleteMany()` (removes all matches), both taking a filter document just like `find()`. There is no built-in "trash"/recycle bin — deletions are permanent, so applications requiring soft-deletes typically implement a boolean `isDeleted` flag instead of physically removing documents.

```javascript
db.orders.deleteMany({ status: "CANCELLED", createdAt: { $lt: new Date("2024-01-01") } })
```

**Interview Questions:**
- What is the difference between `deleteOne()` and `deleteMany()`? — `deleteOne()` removes only the first document matching the filter, while `deleteMany()` removes every document that matches.
- How would you implement a "soft delete" pattern in MongoDB? — Instead of physically removing documents, set a boolean flag such as `isDeleted: true` (optionally with a `deletedAt` timestamp) via an update, and filter out flagged documents in normal application queries.
- Is a deleted document recoverable directly from MongoDB after a `deleteMany()` call? — No, MongoDB does not provide a built-in recycle bin, so a document removed via `deleteMany()` is permanently gone unless it was already captured in a backup or point-in-time snapshot.

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
- What is the benefit of using `bulkWrite()` over issuing individual operations? — `bulkWrite()` batches multiple insert, update, and delete operations into a single network round trip, reducing latency overhead and improving throughput compared to issuing each operation individually.
- What is the difference between ordered and unordered bulk operations? — Ordered bulk operations execute sequentially and stop at the first error, preserving execution order, while unordered operations continue past errors and can be executed in parallel server-side for better performance.
- How does the server handle errors mid-way through an unordered bulk operation? — In unordered mode, the server continues attempting all remaining operations even after individual failures, then returns a summary of all successes and errors once the batch completes.

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
- What does the `upsert` option do in an update operation? — The `upsert` option makes the update insert a new document based on the filter and update when no document matches, instead of doing nothing, enabling a single "create or update" call.
- What value would `_id` take if an upsert results in an insert, and how is it determined? — If the filter includes an `_id` value it's used directly; otherwise, MongoDB auto-generates a new `ObjectId` for the inserted document, similar to a normal insert.
- What race conditions can occur with upserts under concurrent writes, and how can unique indexes help? — Two concurrent upserts with the same filter can both see no matching document and attempt to insert simultaneously, causing a duplicate key error on one of them if a unique index exists on the filtered field(s), which the application should handle by retrying as an update.

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
- How does `replaceOne()` differ from `updateOne()` in terms of what gets modified? — `replaceOne()` replaces the entire matched document body with a new one, while `updateOne()` with operators like `$set` only modifies the specific fields named in the update.
- What happens to fields present in the old document but absent in the replacement document? — Those fields are removed, since `replaceOne()` overwrites the whole document except for `_id`, unlike `updateOne()` which leaves unmentioned fields untouched.
- Can you change a document's `_id` using `replaceOne()`? — No, `_id` is immutable during a replace operation; MongoDB preserves the original `_id` regardless of what's specified in the replacement document.

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
- When is embedding the right modeling choice versus referencing? — Embedding is right when related data is consistently accessed together, has a bounded/predictable size, and doesn't need to be queried independently at scale; referencing is better for large, independently-growing, or shared data.
- What risks arise from embedding a continuously growing array (e.g., comments on a post)? — A continuously growing embedded array can push the parent document toward the 16MB size limit, degrade read/write performance as the document grows, and increase disk relocation overhead.
- How does embedding affect the atomicity of updates to related data? — Because embedded data lives in the same document as its parent, updates to both can be performed atomically in a single write operation, unlike referenced data spread across multiple documents/collections which requires a multi-document transaction for the same guarantee.

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
- How do you model a relationship using references instead of embedding? — Store the `_id` (or another key) of the related document in the referencing document, similar to a foreign key, and retrieve the related data with a separate query or a `$lookup` aggregation stage.
- How does `$lookup` work and what are its performance considerations? — `$lookup` performs a left outer join by matching a `localField` in the current collection against a `foreignField` in another collection, but it can be costly at scale since it isn't as optimized as native SQL joins and benefits greatly from an index on the foreign field.
- Does MongoDB enforce referential integrity between referenced collections? — No, MongoDB does not natively enforce foreign key constraints between referenced collections, so the application is responsible for ensuring referenced documents exist and handling orphaned references.

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
- Would you embed or reference a one-to-one relationship? What factors influence the decision? — Small, bounded, always-together data is typically embedded, while larger or optional one-to-one data is referenced to keep the primary document lean; the deciding factors are data size, access frequency, and whether the related data is optional.
- Give an example of a one-to-one relationship you'd model by embedding versus referencing. — A user's profile settings (bio, avatar URL) are naturally embedded since they're small and always fetched with the user, whereas a large, rarely-accessed one-to-one dataset like a full audit history document might be referenced instead.

### One-to-Many Relationships

A one-to-many relationship connects one document to multiple related documents, such as a blog post with many comments, or a customer with many orders. The modeling choice depends on scale: a "one-to-few" relationship (e.g., a few addresses per user) is often embedded, while "one-to-many" or "one-to-squillions" (e.g., an order history with thousands of entries, or sensor readings) is typically referenced with the "many" side storing a reference back to the "one" side.

```javascript
// customers collection (one) referenced from orders (many)
db.orders.find({ customerId: 1 })
```

**Interview Questions:**
- How would you decide between embedding and referencing for a one-to-many relationship? — The decision depends on scale: a "one-to-few" relationship (e.g., a handful of addresses per user) is often embedded, while "one-to-many" or "one-to-squillions" relationships (e.g., thousands of orders or sensor readings) are typically referenced with the "many" side storing a reference back to the "one" side.
- What is the "one-to-squillions" pattern and how should it be modeled? — The "one-to-squillions" pattern describes a one-to-many relationship where the "many" side can grow unbounded into the thousands or millions (e.g., sensor readings), and should be modeled by storing a reference to the "one" side on each "many" document rather than embedding them.
- How would you paginate through the "many" side of a one-to-many relationship efficiently? — Use range-based (keyset) pagination filtering on an indexed field like `{ customerId: 1, _id: { $gt: lastId } }` combined with `sort()` and `limit()`, avoiding the performance penalty of `skip()` at large offsets.

### Many-to-Many Relationships

Many-to-many relationships connect multiple documents on each side, such as students enrolled in multiple courses and courses having multiple students. This is typically modeled by storing arrays of references on one or both sides (e.g., an array of `courseIds` on the student document, or a separate join/junction collection for very large or frequently changing relationships).

```json
{ "_id": 1, "name": "Student A", "courseIds": [101, 102, 103] }
```

**Interview Questions:**
- How do you model a many-to-many relationship in MongoDB without a join table? — Store an array of references on one or both sides, such as an array of `courseIds` on each student document and/or an array of `studentIds` on each course document, rather than a relational join table.
- When would you introduce a dedicated junction collection instead of array references? — A junction collection is preferable when the relationship itself carries additional attributes (e.g., enrollment date, grade) or when either side's array of references would grow too large or change too frequently to embed efficiently.
- What are the query implications of storing an array of foreign references on both sides of the relationship? — Storing references on both sides requires keeping both arrays in sync on every relationship change, doubling write complexity, though it simplifies querying from either direction without needing `$lookup`.

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
- What is denormalization and why is it common in MongoDB schema design? — Denormalization is intentionally duplicating data across documents/collections to optimize for read performance by avoiding joins at query time, and it's common in MongoDB because the document model favors read efficiency over strict normalization.
- How would you keep denormalized copies of data consistent when the source changes? — You can update all duplicated copies within the same write operation or transaction when the source changes, or use asynchronous background jobs/change streams to propagate updates to denormalized copies, accepting some eventual consistency.
- What's the trade-off between read performance and write complexity with denormalization? — Denormalization speeds up reads by eliminating joins, but increases write complexity and risk of inconsistency since every duplicated copy of the data must be updated whenever the source value changes.

### Schema Design Principles

Effective MongoDB schema design starts from the application's query patterns ("design for your queries") rather than normalizing data first. Key principles include: favor embedding for data accessed together, use references for large or independently-changing data, avoid unbounded array growth, and consider read/write ratios when deciding what to duplicate. Modeling should also account for document growth to avoid frequent document relocation on disk.

**Interview Questions:**
- What does "design your schema based on your application's query patterns" mean in practice? — It means structuring documents and collections around how the application will read and write data, rather than normalizing first, so that the most common queries can be satisfied with minimal joins and efficient index use.
- What factors would push you toward embedding versus referencing for a given relationship? — Data size and growth (bounded vs. unbounded), access patterns (always accessed together vs. independently), and read/write ratio all push the decision — bounded, co-accessed data favors embedding, while large or independently-changing data favors referencing.
- How does anticipated document growth affect your schema design decisions? — Anticipated growth influences whether to embed (risking document relocation and size limits) or reference data, and may lead to pre-allocating space or choosing referencing to keep documents from growing unpredictably large over time.

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
- How do you enforce structure on a MongoDB collection despite its flexible schema? — You attach a `$jsonSchema` validator to the collection via `createCollection()` or `collMod`, specifying required fields, types, and constraints that are enforced on inserts and updates.
- What is the difference between `validationLevel: "strict"` and `"moderate"`? — `strict` applies validation rules to all inserts and updates, while `moderate` only applies validation to inserts and updates to documents that already satisfy the rules, allowing existing invalid documents to still be modified without being forced to comply immediately.
- What happens to existing documents when you add a validator to a collection that already has data violating the rules? — Existing documents that violate the rules are not automatically modified or rejected; they remain in the collection as-is, but further updates to them may be blocked or allowed depending on the `validationLevel` and `validationAction` settings.

### Polymorphic Documents

Polymorphic documents are documents within the same collection that share some common fields but differ in others based on a "type" discriminator field, useful for modeling entities with shared behavior but different attributes (e.g., different payment methods, or different types of notifications). This pattern leverages MongoDB's flexible schema to avoid separate collections or excessive nullable columns as required in relational databases.

```json
{ "_id": 1, "type": "CREDIT_CARD", "last4": "4242", "expiry": "12/26" }
{ "_id": 2, "type": "PAYPAL", "email": "user@example.com" }
```

**Interview Questions:**
- What is a polymorphic document pattern and when would you use it? — The polymorphic document pattern stores documents with shared common fields but type-specific differing fields in the same collection, distinguished by a discriminator field like `type`; it's useful for modeling entities with shared behavior but different attributes, such as different payment methods.
- How would you query a collection efficiently when documents have a discriminator field? — Index the discriminator field (often as part of a compound index) and filter queries by its value (e.g., `{ type: "CREDIT_CARD" }`) so the query planner can quickly narrow down to the relevant subset of documents.
- How does this pattern compare to modeling the same requirement in a relational database using nullable columns or table inheritance? — In a relational database, this typically requires many nullable columns or complex table inheritance schemes to accommodate varying attributes, whereas MongoDB's flexible schema lets each document naturally carry only the fields relevant to its type without wasted nullable columns.

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
- What happens internally when a capped collection reaches its configured size limit? — Once the capped collection reaches its configured size (or document count) limit, MongoDB automatically overwrites the oldest documents to make room for new inserts, behaving like a circular buffer without requiring explicit delete operations.
- Can you delete a single document from a capped collection? Why or why not? — No, individual documents cannot be deleted from a capped collection; only the entire collection can be dropped and recreated, since the fixed-size, insertion-order design relies on automatic overwrite rather than arbitrary deletion.
- How do capped collections differ from using a TTL index on a regular collection? — A capped collection evicts the oldest documents purely based on reaching a size/count limit regardless of age, while a TTL index expires documents based on elapsed time from a date field, independent of collection size.
- Why can't capped collections be sharded? — Capped collections rely on maintaining strict insertion order and a fixed total size, which conflicts with sharding's need to distribute and rebalance data across multiple shards, so MongoDB disallows sharding them.

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
- What problem do time series collections solve compared to storing raw documents? — They solve the storage and query inefficiency of storing millions of individual timestamped documents by automatically bucketing related measurements together, drastically reducing storage size and speeding up time-range queries and aggregations.
- What role does the `metaField` play in bucketing? — The `metaField` identifies the metadata (e.g., device or source identifier) that groups related measurements together, so MongoDB buckets documents sharing the same metadata and time range into the same compressed bucket.
- How can you expire old time series data automatically? — You can set an `expireAfterSeconds` option on the time series collection, which automatically deletes buckets/documents older than the specified duration, similar to a TTL index.
- How does granularity affect bucket size and query performance? — The `granularity` setting (e.g., seconds, minutes, hours) tells MongoDB the expected time span between measurements, which it uses to size buckets appropriately — matching granularity to actual ingestion frequency optimizes both compression and query performance.

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
- What is the difference between `validationLevel: strict` and `moderate`? — `strict` enforces the validator on all inserts and updates, while `moderate` only enforces it on inserts and on updates to documents that already satisfy the rules, letting pre-existing invalid documents continue to be modified without immediate compliance.
- What happens to existing documents when you add a validator to a populated collection? — Existing documents that violate the new validator are left untouched and remain in the collection; the validator only affects subsequent inserts and updates according to the configured `validationLevel`.
- How would you roll out a stricter schema without breaking existing writes? — Start with `validationAction: "warn"` and `validationLevel: "moderate"` to log violations without rejecting writes, monitor and fix offending data, then progressively tighten to `validationAction: "error"` and `validationLevel: "strict"`.
- How does collection validation compare to enforcing structure at the application layer? — Collection validation provides a database-level guardrail that applies regardless of which application or script writes to the collection, complementing (not replacing) application-level validation, which offers richer, more user-friendly error messages.

### Collection Options

Collections can be created with several configuration options beyond the default: `capped`, `size`, `max` (capped settings), `collation` (locale-aware string comparison rules), `storageEngine` (engine-specific options), `validator`/`validationLevel`/`validationAction` (schema rules), `timeseries` (time series settings), and `clusteredIndex`. These options are set at creation time via `db.createCollection()` and some can later be altered using the `collMod` command.

A common real-world example is setting a collection-wide `collation` so that string sorting and comparisons follow a specific language's rules (e.g., case-insensitive comparisons) without needing to specify collation on every query.

```javascript
db.createCollection("products", {
  collation: { locale: "en", strength: 2 } // case-insensitive comparisons
});
```

**Interview Questions:**
- What options can be configured when creating a collection in MongoDB? — Options include `capped`, `size`, and `max` for capped collections, `collation` for locale-aware comparisons, `storageEngine` for engine-specific settings, `validator`/`validationLevel`/`validationAction` for schema rules, `timeseries` settings, and `clusteredIndex`.
- How does setting a default `collation` on a collection affect query behavior? — A default collation applies its locale and comparison strength (e.g., case-insensitivity) to all string comparisons, sorts, and index usage on that collection unless a query explicitly overrides it with its own collation.
- Which collection options can be changed after creation using `collMod`, and which cannot? — Options like `validator`, `validationLevel`, `validationAction`, and TTL `expireAfterSeconds` can be changed later via `collMod`, while structural options like `capped` size/max, `collation`, and `clusteredIndex` key are generally fixed at creation time.

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
- How is a view different from a materialized collection produced by `$merge` or `$out`? — A view is computed on-the-fly from its source every time it's queried and stores no data of its own, while `$merge`/`$out` write the aggregation results into an actual, persisted (materialized) collection that must be refreshed to stay current.
- Can you create indexes directly on a view? — No, a view has no storage or indexes of its own; query performance depends entirely on the indexes available on the underlying source collection and the complexity of the view's pipeline.
- Why might you use a view instead of restricting fields at the application layer? — A view enforces filtering/reshaping at the database level for every consumer regardless of which application or tool queries it, centralizing access control and projection logic rather than duplicating it across multiple application codebases.
- What happens to a view's results if the underlying collection is updated? — Since a view is non-materialized, its results always reflect the current state of the underlying collection the moment it's queried, with no risk of stale cached data.

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
- What is the core difference between a clustered collection and a normal collection with a default `_id` index? — In a clustered collection, documents are physically stored in order of the clustered key (typically `_id`), so the data file itself acts as the index, whereas a normal collection maintains a separate B-tree index pointing to document locations.
- What kinds of workloads benefit most from clustered collections? — Large, append-heavy, time-ordered workloads such as event logs or audit trails that frequently range-scan on `_id` or another monotonically increasing key benefit most, due to reduced storage overhead and faster range scans.
- Can the clustered index key be changed after the collection is created? — No, the clustering key must be chosen at collection creation time and cannot be changed afterward without recreating the collection.

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
- What is the default index every MongoDB collection has? — Every collection automatically has a default unique index on the `_id` field.
- What data structure does MongoDB use to implement indexes? — MongoDB's default WiredTiger storage engine implements indexes as B-trees.
- What is the tradeoff between adding more indexes and write performance? — Every additional index must be updated on every insert, update, and delete, so more indexes improve read performance for the queries they support but add write latency and consume additional RAM/disk.
- How do you determine whether a query is using an index effectively? — Run `explain("executionStats")` on the query and compare `totalDocsExamined`/`totalKeysExamined` against `nReturned` — values close to `nReturned` indicate efficient index usage, while a large gap or a `COLLSCAN` stage indicates poor or missing index usage.

### Single Field Index

A single field index is built on one field of the documents in a collection, in ascending (1) or descending (-1) order. It's the simplest and most common index type, ideal for equality and range queries on a specific field.

```javascript
db.users.createIndex({ email: 1 });
db.users.find({ email: "a@example.com" }); // uses the index
```

**Interview Questions:**
- Does the sort direction (1 vs -1) of a single field index matter for query performance? — For a single field index, sort direction generally doesn't matter for performance since MongoDB can traverse a single-field B-tree index in either direction efficiently; direction becomes significant mainly in compound indexes supporting multi-field sorts.
- When would a single field index be insufficient and a compound index be required? — A single field index is insufficient when queries filter or sort on multiple fields together, since a compound index covering those fields in the right order can satisfy the whole query in one efficient index traversal.

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
- What is the ESR (Equality, Sort, Range) rule for compound index field ordering? — The ESR rule recommends ordering compound index fields as Equality filters first, then Sort fields, then Range filters, since this ordering lets MongoDB narrow down candidates with equality, use the index directly for sorting, and finally apply range bounds most efficiently.
- Can a query use only part of a compound index? Explain prefix matching. — Yes, a compound index can support queries that only use a leading prefix of its fields (e.g., an index on `{a:1,b:1,c:1}` can serve a query filtering only on `a`, or `a` and `b`), similar to how B-tree prefixes work, but it cannot efficiently serve a query that skips the leading field(s).
- Why does field order matter in a compound index but not necessarily in the query filter itself? — The index's field order determines how the B-tree is physically sorted and which prefixes can be used, whereas the query optimizer can match filter conditions against the index regardless of the order they're written in the query document.

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
- Why can't a compound index have more than one array field? — Indexing more than one array field in the same compound index would require generating index entries for every combination of elements across both arrays, causing a combinatorial explosion in index size, so MongoDB disallows it.
- How does MongoDB decide whether to build a multikey index automatically? — MongoDB automatically detects when any indexed field's value is an array during index creation or document insertion and marks the index as multikey, generating an entry per array element.
- What is the storage cost implication of indexing a large array field? — Since a multikey index creates one entry per array element, indexing a field with large arrays significantly increases index size and write overhead proportional to the array length.

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
- How many text indexes can a single collection have? — A collection can have at most one text index, though that single text index can cover multiple fields.
- How does `$text` search differ from a regex-based search? — `$text` search uses the text index with language-aware stemming, stop-word removal, and relevance scoring, while regex search performs literal pattern matching with no linguistic awareness or ranking, and generally cannot use an index unless the pattern is left-anchored.
- When would you choose Atlas Search or an external search engine over a native text index? — Choose Atlas Search or a dedicated engine like Elasticsearch when you need fuzzy matching, typo tolerance, advanced relevance tuning, faceted search, or autocomplete, which go beyond what a native text index supports.

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
- What is the difference between a `2d` index and a `2dsphere` index? — A `2d` index supports legacy planar (flat) coordinate geometry, while a `2dsphere` index supports GeoJSON objects and calculates distances on a spherical (earth-like) surface, making it suitable for real-world geographic data.
- What GeoJSON operators can be used alongside a `2dsphere` index (e.g., `$near`, `$geoWithin`, `$geoIntersects`)? — Common operators include `$near`/`$nearSphere` for proximity queries, `$geoWithin` for containment within a shape, and `$geoIntersects` for finding geometries that intersect a given shape.
- What real-world features would require a geospatial index? — Features like "find nearby stores," "drivers within delivery radius," or "properties within a drawn map boundary" all require efficient location-based queries powered by a geospatial index.

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
- Why is a hashed index commonly used as a shard key? — Hashing a shard key produces a uniform, random distribution of values, spreading writes evenly across shards and avoiding the "hot shard" problem that a monotonically increasing key would cause by always routing new writes to the same shard.
- Why can't a hashed index support range queries? — Hashing destroys the original ordering of values, so consecutive original values no longer map to consecutive hash values, making range scans over hashed index entries meaningless.
- What problem does hashed sharding solve compared to range-based sharding on a monotonically increasing field? — Hashed sharding avoids concentrating all new writes on a single shard (a hotspot), which range-based sharding on a monotonically increasing field like a timestamp or auto-incrementing ID would otherwise cause.

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
- How precise is the timing of TTL-based document deletion? — TTL deletion is not immediate; a background thread runs periodically (roughly every 60 seconds) to remove expired documents, so documents can linger briefly past their exact expiration time.
- Can a TTL index be a compound index? — No, a TTL index must be a single-field index on a date field; it cannot be part of a compound index.
- How would you implement a "delete exactly at a given timestamp" pattern using a TTL index? — Store the exact expiration timestamp in the date field and create the TTL index with `expireAfterSeconds: 0`, which causes documents to expire once the current time passes the stored date value.

### Unique Index

A unique index enforces that no two documents in a collection can have the same value for the indexed field(s), rejecting inserts/updates that would create a duplicate. It can be a single or compound index, and combined with `sparse` to allow multiple documents missing the field.

```javascript
db.users.createIndex({ email: 1 }, { unique: true });
```

**Interview Questions:**
- What happens if you try to create a unique index on a field with existing duplicate values? — The index creation fails with a duplicate key error, since MongoDB cannot enforce uniqueness on a field that already contains duplicate values in the collection.
- How do unique indexes interact with sharding (constraints on shard key)? — A unique index can only be enforced across the full range of a sharded collection if it is on the shard key itself (or a prefix of it), since MongoDB cannot efficiently enforce global uniqueness for arbitrary fields spread across independent shards.
- How would you allow multiple documents to omit a uniquely-indexed field? — Combine the unique index with the `sparse` option, so the index only includes documents that actually have the field, allowing any number of documents missing the field to coexist without violating uniqueness.

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
- What is the difference between a sparse index and a partial index? — A sparse index only excludes documents missing the indexed field entirely, while a partial index excludes documents based on an arbitrary filter expression, offering much more flexible control over which documents are indexed.
- Why might a sort operation behave unexpectedly when using a sparse index? — Because a sparse index omits documents lacking the field, a sort relying on that index may silently skip those documents rather than including them (e.g., with a null/missing value), producing incomplete results if not accounted for.
- Why does MongoDB generally recommend partial indexes over sparse indexes today? — Partial indexes support arbitrary filter conditions rather than just field existence, making them a strict superset of sparse index functionality with more precise control over which documents get indexed.

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
- How does a partial index reduce storage and write costs compared to a full index? — By only indexing documents matching the `partialFilterExpression`, a partial index excludes irrelevant documents entirely, resulting in a smaller index that's cheaper to maintain on every write.
- Can a query use a partial index if its filter doesn't match the partial filter expression exactly? — The query planner can use a partial index only if the query's filter logically implies the partial filter expression (i.e., every document the query could match must satisfy the partial filter), otherwise it falls back to a different index or a collection scan.
- Give an example of a real-world scenario where a partial index is preferable to a full index. — Indexing only `{ status: "ACTIVE" }` orders in a large orders collection where most historical orders are completed/archived and rarely queried is a good use case for a partial index, since it keeps the index small and focused on the hot subset of data.

### Covered Queries

A covered query is one where all the fields requested in the query (both the filter and the projection) are present in the index itself, so MongoDB can return results directly from the index without ever reading the actual documents. This significantly improves performance since it avoids extra document fetches.

```javascript
db.users.createIndex({ email: 1, name: 1 });
db.users.find({ email: "a@example.com" }, { _id: 0, email: 1, name: 1 }); // covered
```

**Interview Questions:**
- What conditions must be met for a query to be "covered" by an index? — Every field referenced in the query's filter and projection must be present in the index, and the `_id` field must be explicitly excluded from the projection unless it's also part of the index, so MongoDB never needs to fetch the actual document.
- Why must `_id` be explicitly excluded in the projection for many covered queries? — Because `_id` is returned by default even without explicit projection, and if it's not part of the index being used, including it would force MongoDB to fetch the full document, breaking the covered query optimization.
- How would you verify using `explain()` whether a query is covered? — Run `explain("executionStats")` and check that the plan contains no `FETCH` stage and that `totalDocsExamined` is 0, indicating results came entirely from the index.

### Index Selection

Index selection refers to the query planner's process of choosing which available index (if any) to use for a given query, based on the query shape, sort, and estimated selectivity. MongoDB caches winning query plans and may run a "plan ranking" competition among candidate indexes using sampled execution.

```javascript
db.orders.find({ status: "SHIPPED" }).explain("executionStats");
```

**Interview Questions:**
- How does MongoDB decide which index to use when multiple indexes could satisfy a query? — The query planner runs a short trial competition among candidate indexes for the query shape, selecting the plan that examines the fewest documents/keys, and caches that winner for future queries with the same shape.
- What is plan caching, and when does MongoDB re-evaluate a cached plan? — Plan caching stores the winning execution plan for a query shape so it can be reused without re-running the planning competition; MongoDB re-evaluates the plan when indexes change, the collection changes significantly, or after a threshold of executions since caching.
- How can you force MongoDB to use a specific index? — You can use the `hint()` method to explicitly tell MongoDB which index to use for a query, overriding the query planner's own selection.

### Index Best Practices

Good indexing strategy balances query performance against write overhead and memory usage. Key practices: index fields used in frequent equality/range/sort filters, follow the ESR rule for compound indexes, avoid redundant/overlapping indexes, use partial indexes to shrink footprint, monitor with `$indexStats` and `explain()`, and drop unused indexes.

```javascript
db.orders.aggregate([{ $indexStats: {} }]); // shows usage counts per index
```

**Interview Questions:**
- What metrics would you look at to decide whether an index is unused and safe to drop? — Use `$indexStats` to check the usage `count` and `since` timestamp for each index; an index with zero or near-zero usage over a representative time period is a strong candidate to drop.
- What is index "prefix redundancy" and how do you avoid it? — Prefix redundancy occurs when one compound index's leading fields are a full prefix of another index (e.g., `{a:1}` is redundant if `{a:1,b:1}` exists), since the shorter index adds maintenance cost without enabling any query the longer index can't already serve; avoid it by auditing indexes and dropping true prefix duplicates.
- How many indexes is "too many" for a write-heavy collection, and why? — There's no fixed number, but MongoDB's official guidance suggests being cautious beyond roughly a dozen indexes per collection, because each additional index adds write amplification and memory pressure that can outweigh its read benefits on write-heavy workloads.

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
- When would a wildcard index be preferable to creating many individual single-field indexes? — A wildcard index is preferable when a collection has a highly variable or unpredictable schema (e.g., user-defined attributes), letting you index all fields under a subdocument dynamically rather than maintaining dozens of individual indexes manually.
- What are the limitations of wildcard indexes compared to targeted indexes? — Wildcard indexes are larger and more expensive to maintain, cannot serve as unique indexes, and have restrictions on compound usage compared to precisely targeted single-field or compound indexes.
- Can a wildcard index enforce uniqueness? — No, wildcard indexes cannot be created as unique indexes.

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
- What are the high-level steps MongoDB takes from receiving a query to returning results? — MongoDB parses the query, consults the query planner to select an execution plan from candidate indexes/access paths, executes that plan against the storage engine (index scans, fetches, filtering, sorting, projection), and returns a result cursor to the client.
- What is a cursor and how does it relate to query execution? — A cursor is a pointer to the result set of a query that the server returns incrementally in batches rather than all at once, allowing the client to iterate through potentially large results without loading everything into memory at once.

### Query Planner

The query planner evaluates candidate indexes and access paths for a given query shape, running short trial executions of each candidate plan and selecting the most efficient one based on the fewest documents examined/work done. The winning plan is cached for reuse on subsequent queries with the same shape.

**Interview Questions:**
- How does the query planner choose between multiple candidate indexes? — The query planner runs a short trial execution of each candidate plan and selects the one that does the least work (fewest documents/keys examined) to satisfy the query, then caches that winning plan for the query's shape.
- What is a "query shape" and why does it matter for plan caching? — A query shape is the structural signature of a query (its filter fields, sort, and projection) independent of literal values, and MongoDB caches one winning plan per shape so future queries with the same shape skip the planning competition.
- When does MongoDB invalidate or re-evaluate a cached query plan? — A cached plan is invalidated and re-evaluated when indexes are added or dropped, the collection changes significantly, the server restarts, or after enough writes/executions have occurred since the plan was cached.

### Execution Plans

An execution plan is a tree of stages (e.g., `IXSCAN`, `COLLSCAN`, `FETCH`, `SORT`, `PROJECTION`) describing exactly how MongoDB will retrieve and process data for a query. Understanding this tree is essential for diagnosing slow queries.

```javascript
db.orders.find({ status: "SHIPPED" }).sort({ createdAt: -1 }).explain("executionStats");
```

**Interview Questions:**
- What does an `IXSCAN` stage indicate versus a `COLLSCAN` stage? — `IXSCAN` indicates MongoDB traversed an index to find candidate documents, while `COLLSCAN` indicates a full collection scan reading every document, which is typically much slower for selective queries.
- What does a `FETCH` stage do, and why is minimizing fetched documents important? — A `FETCH` stage retrieves the full document from disk/cache after an index scan identifies a candidate, and minimizing fetches (ideally via a covered query) is important because fetching documents is significantly more expensive than reading compact index entries.
- What does a `SORT` stage appearing in the plan (rather than being satisfied by an index) imply about performance? — A `SORT` stage means MongoDB had to sort results in memory after retrieval rather than relying on an index's natural order, which is slower and can hit memory limits on large result sets, indicating a missing or poorly ordered index for that sort.

### Explain Plans

The `explain()` method reveals how a query was (or would be) executed, with three verbosity modes: `queryPlanner` (chosen plan only), `executionStats` (actual runtime stats like documents examined/returned), and `allPlansExecution` (stats for all candidate plans considered).

```javascript
db.orders.find({ status: "SHIPPED" }).explain("executionStats");
```

**Interview Questions:**
- What is the difference between `queryPlanner`, `executionStats`, and `allPlansExecution` modes? — `queryPlanner` shows only the chosen plan without executing it, `executionStats` executes the query and reports actual runtime statistics like documents examined and returned, and `allPlansExecution` additionally reports stats for all candidate plans that were considered.
- What key metrics would you check in `executionStats` to detect an inefficient query (e.g., `totalDocsExamined` vs `nReturned`)? — Compare `totalDocsExamined` and `totalKeysExamined` against `nReturned` — a large gap indicates the query is examining far more documents/index entries than it actually returns, signaling an inefficient or missing index.
- How would you use `explain()` to confirm a covered query? — Check that the execution plan has no `FETCH` stage and that `totalDocsExamined` is 0, confirming all data was served directly from the index.

### Query Optimization

Query optimization is the practice of restructuring queries and indexes so MongoDB examines the minimum number of documents/index entries needed to satisfy a request. Techniques include adding appropriate indexes, following the ESR rule for compound indexes, using projections to limit returned fields, and avoiding unselective regex or `$where` queries.

**Advantages:**
- Reduces latency and server resource consumption
- Improves throughput under concurrent load

**Interview Questions:**
- What is the ratio between `nReturned` and `totalDocsExamined` telling you about query efficiency? — A ratio close to 1 indicates an efficient query that examines roughly as many documents as it returns, while a low ratio (examining far more documents than returned) signals a missing or poor index requiring optimization.
- Why are unanchored regular expressions (e.g., `/abc/`) generally bad for query performance? — Unanchored regex patterns can match anywhere within a string, so MongoDB cannot use an index range scan and typically must examine every document's field value, resulting in a full collection or full index scan.
- How would you optimize a query that currently triggers a full collection scan? — Analyze the query's filter and sort fields with `explain()`, then create an appropriate single-field or compound index (following the ESR rule) covering those fields so the query can use an `IXSCAN` instead of a `COLLSCAN`.

### Projection

Projection controls which fields are included or excluded from query results, reducing network payload size and, when combined with a covering index, avoiding document fetches entirely. Projections can use inclusion (`{ field: 1 }`) or exclusion (`{ field: 0 }`), but generally not both (except for `_id`).

```javascript
db.users.find({ status: "ACTIVE" }, { name: 1, email: 1, _id: 0 });
```

**Interview Questions:**
- Can you mix inclusion and exclusion in the same projection document? — Generally no, a projection must be either all-inclusion or all-exclusion, with the one exception being `_id`, which can be explicitly excluded (`_id: 0`) even in an otherwise inclusion-based projection.
- How does projection interact with covered queries? — A projection that only requests fields already present in the index used for the query (and excludes `_id` unless it's also indexed) allows MongoDB to serve results entirely from the index without fetching documents, forming a covered query.
- What is the default behavior for the `_id` field in projections? — By default, `_id` is included in query results even if not explicitly mentioned in an inclusion projection, unless it's explicitly excluded with `_id: 0`.

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
- Why does `skip()` become slow for large offsets even with an index? — Even with an index, `skip()` must still internally walk over and discard every skipped document before returning results, so the work grows linearly with the offset regardless of indexing.
- How would you implement keyset (cursor-based) pagination in MongoDB? — Track the sort key value of the last document seen on the current page, then query for documents beyond that value (e.g., `{ _id: { $gt: lastSeenId } }`) combined with the same `sort()` and `limit()`, letting the index directly seek to the right starting point.
- What are the tradeoffs of keyset pagination versus offset pagination? — Keyset pagination scales much better for deep pages since it's index-driven and doesn't degrade with offset, but it only supports sequential navigation and can't jump directly to an arbitrary page number like offset pagination can.

### Sorting

Sorting orders query results by one or more fields, either using an index (fast, no extra memory) or, if no suitable index exists, an in-memory sort (subject to a 100MB memory limit unless `allowDiskUse` is enabled in aggregation). Compound indexes matching the sort fields (in the ESR order) avoid expensive in-memory sorts.

```javascript
db.orders.createIndex({ status: 1, createdAt: -1 });
db.orders.find({ status: "SHIPPED" }).sort({ createdAt: -1 }); // index-based sort
```

**Interview Questions:**
- What happens when MongoDB cannot satisfy a sort using an index? — MongoDB performs an in-memory sort of the result set, which is slower and subject to a 100MB memory limit unless `allowDiskUse` is enabled for aggregation pipelines.
- What is the 100MB in-memory sort limit, and how can it be worked around in aggregation pipelines? — MongoDB caps in-memory sort operations at 100MB of RAM by default; in aggregation pipelines, this can be worked around by enabling `allowDiskUse: true`, which lets MongoDB spill intermediate sort data to disk.
- How does field order in a compound index affect whether it can support a sort? — A compound index can satisfy a sort only if the sort fields (and directions, accounting for reversal) match a prefix of the index's field order following any equality filters, per the ESR rule; mismatched order forces an in-memory sort.

### Cursors

A cursor is a pointer to the result set of a query, allowing the client to iterate through results in batches instead of loading everything into memory at once. Cursors are lazily evaluated on the server and can time out if left idle (unless configured otherwise), and methods like `sort()`, `limit()`, and `skip()` can be chained onto them before iteration begins.

```javascript
const cursor = db.orders.find({ status: "SHIPPED" }).batchSize(100);
while (cursor.hasNext()) {
  printjson(cursor.next());
}
```

**Interview Questions:**
- How does `batchSize()` affect network round trips when iterating a cursor? — `batchSize()` controls how many documents the server sends per network round trip; a larger batch size reduces the number of round trips needed to iterate the full result set at the cost of more memory used per batch.
- What causes a cursor to time out, and how can you prevent it for long-running operations? — A cursor times out if left idle on the server for too long (default ~10 minutes) without being iterated; this can be prevented by using `noCursorTimeout()` (with care to always close it) or by iterating the cursor promptly.
- How do cursors relate to pagination strategies in an application? — Cursors provide the underlying mechanism for streaming results in batches, which applications build pagination on top of, either by tracking cursor position/batches directly or by combining `sort()`/`limit()`/`skip()` or keyset filters with a fresh cursor per page request.

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
- How is the aggregation pipeline conceptually similar to Unix pipes? — Like Unix pipes chaining commands where each command's output feeds the next, an aggregation pipeline chains stages where each stage consumes the previous stage's output documents and passes its transformed result to the next stage.
- What is the difference between using `find()` with a filter versus an aggregation `$match` stage? — `find()` with a filter is a simple query returning matching documents (optionally reshaped via projection), while `$match` is one stage within a larger aggregation pipeline that can be combined with grouping, joining, and reshaping stages for far more complex analytics.
- How would you debug a slow aggregation pipeline? — Use `explain()` on the aggregation to inspect each stage's execution stats, check whether early stages like `$match`/`$sort` are using indexes, and look for expensive stages like unindexed `$sort`, `$group`, or `$lookup` without a foreign index that may need reordering or optimization.

### Pipeline Stages

Pipeline stages are the individual building blocks of an aggregation, executed in order, each consuming the output documents of the previous stage. Common stages include `$match`, `$project`, `$group`, `$sort`, `$limit`, `$skip`, `$unwind`, `$lookup`, `$facet`, `$bucket`, `$merge`, and `$out`. Stage order matters both for correctness and performance.

**Interview Questions:**
- Why does the order of stages in an aggregation pipeline matter for performance? — Placing filtering stages like `$match` early reduces the number of documents flowing into later, more expensive stages like `$group` or `$lookup`, minimizing total work done; reordering incorrectly can force MongoDB to process far more data than necessary.
- Which pipeline stages can make use of an index? — Stages like `$match` (especially when first in the pipeline), `$sort` (when it can use an index's natural order), and `$geoNear` can leverage indexes, while stages like `$group`, `$project`, and `$unwind` generally cannot use indexes directly.
- Can the same stage type (e.g., `$match`) appear multiple times in one pipeline? — Yes, the same stage type can appear multiple times at different points in a pipeline, each operating on the documents as transformed by the preceding stages at that point.

### Match

`$match` filters documents based on a query condition, functioning like the `find()` filter but within a pipeline. Placing `$match` as early as possible in a pipeline is a key optimization since it reduces the document count flowing into subsequent stages, and an early `$match` can use indexes.

```javascript
{ $match: { status: "SHIPPED", createdAt: { $gte: ISODate("2026-01-01") } } }
```

**Interview Questions:**
- Why is it recommended to place `$match` as early as possible in a pipeline? — An early `$match` filters out irrelevant documents before they reach more expensive downstream stages like `$group`, `$sort`, or `$lookup`, reducing the total data volume processed and improving overall pipeline performance.
- Can `$match` use an index? Under what conditions? — Yes, `$match` can use an index when it is the first stage in the pipeline (or immediately follows only index-compatible stages) and its filter conditions align with an existing index, just like a regular `find()` query.

### Project

`$project` reshapes documents by including, excluding, renaming, or computing new fields using expressions. It's often used to strip unneeded fields early (reducing memory for later stages) or to compute derived values.

```javascript
{ $project: { name: 1, totalWithTax: { $multiply: ["$amount", 1.08] }, _id: 0 } }
```

**Interview Questions:**
- How does `$project` differ from the `projection` argument of `find()`? — `$project` is a full pipeline stage that supports computed fields and expressions (like `$multiply` or `$concat`) in addition to inclusion/exclusion, while `find()`'s projection argument only supports simple field inclusion/exclusion without computed expressions.
- Can `$project` create entirely new computed fields? Give an example. — Yes, `$project` can compute new fields using aggregation expressions, for example `{ $project: { totalWithTax: { $multiply: ["$amount", 1.08] } } }` creates a new `totalWithTax` field derived from `amount`.

### Group

`$group` aggregates documents by a specified `_id` key (which can be `null` for a single overall group), computing accumulator expressions like `$sum`, `$avg`, `$min`, `$max`, `$push`, and `$addToSet` per group — analogous to SQL's `GROUP BY`.

```javascript
{ $group: { _id: "$customerId", orderCount: { $sum: 1 }, avgAmount: { $avg: "$amount" } } }
```

**Interview Questions:**
- How would you compute a grand total across all documents (not grouped by any field)? — Use `$group` with `_id: null`, which places every input document into a single overall group, allowing accumulators like `$sum` to compute a grand total across the entire collection (or pipeline input).
- What is the difference between `$push` and `$addToSet` accumulators? — `$push` collects every value into an array for each group, including duplicates, while `$addToSet` collects only distinct values, effectively deduplicating as it accumulates.
- Why can `$group` be memory-intensive on large datasets, and how does `allowDiskUse` help? — `$group` must hold intermediate group data (accumulator state per group) in memory as it processes documents, which can be large for many groups or large arrays; `allowDiskUse: true` lets MongoDB spill this intermediate data to disk when it exceeds the memory limit, trading speed for the ability to complete the operation.

### Sort

The `$sort` stage orders documents by one or more fields within the pipeline. Placing `$sort` before a `$limit` lets MongoDB optimize using a top-K sort algorithm, and placing it early enough may allow it to use an index.

```javascript
{ $sort: { totalSpent: -1 } }
```

**Interview Questions:**
- How does combining `$sort` immediately followed by `$limit` improve performance? — MongoDB recognizes the adjacent `$sort`+`$limit` combination and optimizes it into a top-K sort, only keeping track of the N smallest/largest documents in memory instead of sorting the entire input set.
- When can `$sort` in an aggregation pipeline use an index versus requiring an in-memory sort? — `$sort` can use an index when it's early enough in the pipeline (ideally the first stage, possibly after a `$match` using the same index) and its sort fields/directions match the index; otherwise it falls back to an in-memory sort subject to the 100MB limit.

### Limit

`$limit` restricts the number of documents passed to the next stage, commonly used with `$sort` for top-N queries or with `$skip` for pagination.

```javascript
{ $limit: 10 }
```

**Interview Questions:**
- Why is `{ $sort } { $limit }` more efficient than reversing the stage order? — With `$sort` before `$limit`, MongoDB can optimize the pair into a top-K sort that only tracks the needed number of top documents; reversing the order (`$limit` then `$sort`) would sort an arbitrary, potentially unrelated subset of documents, producing an incorrect and non-optimizable result.

### Skip

`$skip` bypasses a specified number of documents before passing the rest along the pipeline, typically used for pagination. Like the `skip()` cursor method, it still requires MongoDB to walk over the skipped documents, so it degrades for large offsets.

```javascript
{ $skip: 100 }
```

**Interview Questions:**
- Why is `$skip` inefficient for deep pagination on large collections? — Like the `skip()` cursor method, the `$skip` stage still requires MongoDB to iterate over and discard every skipped document, so cost grows linearly with the offset regardless of any index.
- What alternative pagination strategy avoids the cost of `$skip`? — Keyset (range-based) pagination, filtering with `$match` on the last seen sort key value (e.g., `{ _id: { $gt: lastSeenId } }`) instead of using `$skip`, avoids the linear scan cost of skipped documents.

### Unwind

`$unwind` deconstructs an array field, producing one output document per array element (each output document is a copy of the original with the array field replaced by a single element). It's essential for performing per-element analysis or joins on array data.

```javascript
db.orders.aggregate([
  { $unwind: "$items" },
  { $group: { _id: "$items.sku", totalQty: { $sum: "$items.qty" } } }
]);
```

**Interview Questions:**
- What happens to a document if the array field being unwound is empty or missing? — By default, `$unwind` drops the document entirely from the output if the array field is empty, missing, or not an array, unless `preserveNullAndEmptyArrays` is set to true.
- How does `preserveNullAndEmptyArrays` change `$unwind`'s default behavior? — Setting `preserveNullAndEmptyArrays: true` keeps documents with a missing, null, or empty array field in the output (with the field set to null/absent), instead of silently dropping them as `$unwind` does by default.
- Why might `$unwind` followed by `$group` be used together in real reporting pipelines? — `$unwind` flattens array elements into individual documents so that `$group` can then aggregate metrics per array element (e.g., total quantity sold per line item SKU across all orders), which wouldn't be possible while the values remained nested in an array.

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
- What index should exist on the foreign collection to make `$lookup` efficient? — An index on the `foreignField` used in the `$lookup` (matching the collection referenced via `from`) is essential, since without it MongoDB must scan the entire foreign collection for every input document.
- How does the pipeline-based form of `$lookup` differ from the simple `localField`/`foreignField` form? — The pipeline-based form lets you run an arbitrary sub-pipeline (with `$match`, multiple conditions, `$expr`, etc.) against the foreign collection, supporting complex, multi-field join conditions that the simple `localField`/`foreignField` equality form cannot express.
- When would you choose embedding data over using `$lookup`? — Choose embedding when the joined data is small, bounded, and consistently accessed together with the parent, since embedding avoids the extra latency and potential index requirements of a `$lookup` join at query time.

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
- What problem does `$facet` solve compared to running multiple separate aggregation queries? — `$facet` computes multiple independent views of the same input documents (e.g., paginated results, total count, and category breakdown) in a single aggregation call and round trip, instead of issuing several separate queries against the collection.
- Are the sub-pipelines inside `$facet` executed on the original input documents or on each other's output? — Each sub-pipeline inside `$facet` runs independently against the same original input documents entering the `$facet` stage; they do not see or depend on each other's output.

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
- What is the difference between `$bucket` and `$bucketAuto`? — `$bucket` requires you to explicitly specify the boundary values for each bucket, while `$bucketAuto` automatically computes bucket boundaries to distribute documents into a target number of buckets as evenly as possible.
- What happens to a document whose value falls outside all defined boundaries? — In `$bucket`, a document whose value falls outside all specified boundaries is placed into the bucket named by the `default` option, or the stage errors if no `default` is specified.

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
- How does `$merge` differ from `$out`? — `$merge` can write into a collection that already contains data, merging or updating matching documents based on configurable behaviors, while `$out` completely replaces the target collection's entire contents with the pipeline's output.
- What options control how `$merge` handles documents that already exist in the target collection? — The `whenMatched` option (e.g., `replace`, `merge`, `keepExisting`, `fail`, or a custom pipeline) and `whenNotMatched` option (e.g., `insert`, `discard`, `fail`) control how `$merge` handles existing versus new documents in the target collection.
- What is a practical use case for `$merge` (e.g., incrementally maintained materialized views)? — A practical use case is incrementally maintaining a `customerTotals` materialized view collection that's updated by re-running a nightly aggregation and merging new totals into the existing collection without wiping out unrelated historical data.

### Out

`$out` writes the aggregation results to a collection, completely replacing its previous contents (or creating it if it doesn't exist). It must be the last stage in the pipeline.

```javascript
db.orders.aggregate([
  { $group: { _id: "$customerId", total: { $sum: "$amount" } } },
  { $out: "customerTotalsSnapshot" }
]);
```

**Interview Questions:**
- Why must `$out` be the final stage of a pipeline? — `$out` completely replaces the target collection with the pipeline's output, so it wouldn't make sense to run further stages afterward since there is no further transformation needed; MongoDB enforces this by requiring `$out` to be last.
- What happens to the target collection's existing indexes when `$out` replaces its contents? — MongoDB preserves the target collection's existing indexes across the `$out` operation, rebuilding them against the new data rather than dropping them.

### Aggregation Pipeline Optimization

MongoDB automatically applies certain pipeline optimizations (e.g., merging adjacent `$match`/`$sort` stages, pushing `$match`/`$project` earlier when semantically safe, coalescing `$limit` into `$sort`). Developers can further optimize by placing `$match`/`$limit` early, projecting out unneeded fields before expensive stages, ensuring `$match`/`$sort` fields are indexed, and using `allowDiskUse: true` only when unavoidable for large `$group`/`$sort` operations.

```javascript
db.orders.aggregate(pipeline, { allowDiskUse: true });
```

**Interview Questions:**
- What automatic optimizations does the aggregation engine perform on pipeline stages? — MongoDB automatically merges adjacent `$match`/`$sort` stages where possible, pushes `$match`/`$project` earlier in the pipeline when semantically safe, and coalesces `$limit` into a preceding `$sort` for a top-K optimization.
- When would you need to enable `allowDiskUse`, and what is the tradeoff of doing so? — Enable `allowDiskUse: true` when a `$group` or `$sort` stage's intermediate data exceeds the 100MB in-memory limit; the tradeoff is slower execution since data must be spilled to and read from disk instead of staying fully in RAM.
- How would you use `explain()` on an aggregation pipeline to identify bottlenecks? — Call `.explain("executionStats")` on the aggregation to see per-stage execution statistics, revealing which stages examine the most documents, whether early stages use an index, and where time is spent so you can reorder or index accordingly.

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
- How does `$setWindowFields` differ from `$group` in terms of output shape? — `$setWindowFields` enriches each original input document with a computed window value while preserving one output document per input document, whereas `$group` collapses multiple documents into a single summary document per group.
- How would you compute a 7-day moving average using window functions? — Use `$setWindowFields` with `sortBy` on the date field and an `$avg` accumulator with a `window` range like `{ range: [-6, 0], unit: "day" }` (or a documents-based window sized to the desired span) to compute a rolling average over the trailing 7 days.
- What does `partitionBy` control in `$setWindowFields`? — `partitionBy` groups documents into separate partitions (similar to `$group`'s `_id`) so that window calculations like running totals or moving averages are computed independently within each partition, such as per-region rather than across the entire dataset.

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
- Why is single-document atomicity considered a core design principle in MongoDB's data modeling philosophy? — Because MongoDB guarantees atomic changes across all fields, arrays, and subdocuments within a single document, schema designs that embed related data into one document can rely on this built-in atomicity for consistency, reducing the need for more expensive multi-document transactions.
- How can embedding data to leverage single-document atomicity reduce the need for multi-document transactions? — By co-locating related data that must change together (e.g., an account balance and its transaction history) within one document, a single atomic update operation can satisfy consistency requirements that would otherwise require wrapping multiple documents in a transaction.

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
- When would you reach for a multi-document transaction instead of relying on document embedding? — Use a multi-document transaction when an operation must atomically update multiple separate documents, collections, or databases that cannot reasonably be embedded together, such as transferring funds between two independent account documents.
- What is snapshot isolation, and how does it apply to MongoDB transactions? — Snapshot isolation means a transaction sees a consistent point-in-time view of the data for its entire duration, unaffected by concurrent writes from other transactions, and its own writes aren't visible to others until it commits.
- How would you implement a multi-document transaction using Spring Data MongoDB's `@Transactional`? — Annotate a service method with `@Transactional`, ensure a `MongoTransactionManager` bean is configured, and perform multiple repository/template operations inside that method; Spring Data automatically starts, commits, or rolls back the underlying MongoDB session transaction around the method boundary.

### ACID Properties

MongoDB transactions provide the classic ACID guarantees: **Atomicity** (all operations in the transaction succeed or none do), **Consistency** (the database moves from one valid state to another), **Isolation** (concurrent transactions don't see each other's uncommitted changes, via snapshot isolation), and **Durability** (committed changes survive failures, backed by the write-ahead journal and replication).

**Interview Questions:**
- How does MongoDB implement isolation for concurrent transactions? — MongoDB implements isolation through snapshot isolation, giving each transaction a consistent view of data as of its start time and hiding uncommitted changes from other concurrent transactions until commit.
- How does write concern relate to the durability guarantee of a committed transaction? — The write concern (e.g., `majority`) used when committing a transaction determines how many replica set members must acknowledge the write before it's considered durable, directly controlling the strength of the durability guarantee.
- Can you explain atomicity in the context of a MongoDB multi-document transaction with an example? — In a funds transfer transaction that debits one account and credits another, atomicity guarantees that either both updates succeed and are committed together, or if either fails, both are rolled back, so the system never ends up with money debited from one account without being credited to the other.

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
- What are the distinct phases of a MongoDB transaction's lifecycle? — A transaction starts a client session, starts the transaction itself, executes one or more operations scoped to that session, and finally either commits (making changes durable and visible) or aborts (discarding all changes).
- What kinds of errors should trigger a transaction retry versus an abort? — Errors labeled `TransientTransactionError` (e.g., transient network issues or replica set elections) should trigger a retry of the whole transaction, while non-transient errors like application-level validation failures should trigger an abort without retrying.
- Why is a client session required before starting a transaction? — A client session provides the context (including the transaction number and causal consistency guarantees) that MongoDB uses to associate a sequence of operations with a single transaction and to support retryable writes.

### Transaction Limitations

MongoDB transactions have practical constraints: a default 60-second maximum runtime, restrictions on certain DDL operations (e.g., creating collections/indexes inside a transaction, though this improved in later versions), increased oplog/cache pressure for large transactions, and a general recommendation to keep transactions short and touch a limited number of documents.

**Disadvantages:**
- Not designed for bulk/batch operations touching very large numbers of documents
- Can increase contention and WiredTiger cache pressure if overused

**Interview Questions:**
- What is the default time limit for a MongoDB transaction, and what happens if it's exceeded? — MongoDB transactions default to a 60-second maximum runtime; if exceeded, the transaction is automatically aborted and any changes made within it are rolled back.
- Why are multi-document transactions discouraged for large bulk-update workloads? — Large transactions touching many documents increase oplog size, WiredTiger cache pressure, and contention/lock hold time, risking timeouts and degraded throughput, so bulk updates are generally better handled with `bulkWrite()` outside a transaction.
- What operations were historically restricted from running inside a transaction? — Certain DDL-like operations, such as creating collections or indexes, were historically restricted from running inside a transaction, though later MongoDB versions relaxed some of these restrictions.

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
- How does MongoDB avoid applying a retried write twice? — Retryable writes are tagged with a unique transaction number per session, which the server uses to detect and deduplicate a retried attempt, ensuring the operation's effect is applied only once even if the client resends it.
- What kinds of failures trigger a retryable write to automatically retry? — Transient failures such as network errors or a replica set election (temporary loss of a Primary) trigger an automatic retry of the write by the driver.
- How do retryable writes differ from multi-document transactions in purpose and scope? — Retryable writes provide resilience for a single write operation against transient failures with low overhead, while multi-document transactions provide atomicity across multiple operations/documents at a higher performance cost.

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
- What granularity of locking does WiredTiger use for writes? — WiredTiger uses document-level locking, meaning a write only locks the specific document being modified rather than the whole collection or database.
- How did document-level locking improve on MongoDB's older MMAPv1 storage engine locking model? — MMAPv1 used coarser database-level (and earlier, global) locking, which serialized many unrelated writes; WiredTiger's document-level locking allows writes to different documents to proceed fully in parallel, dramatically improving write concurrency.
- Can two concurrent writes to the same document proceed simultaneously? — No, two concurrent writes targeting the same document cannot proceed simultaneously; one must wait for the other to complete since the document itself is locked for the duration of the write.

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
- How would you implement optimistic concurrency control in MongoDB using a version field? — Add a `version` field to the document, read it along with the rest of the data, and perform the update with a filter that matches both the `_id` and the expected `version` value while incrementing the version, so the update only succeeds if no one else has modified the document in between.
- What should an application do when an optimistic update fails due to a version mismatch? — The application should detect that the update matched zero documents (indicating a concurrent modification), re-read the current document state, and retry its logic against the fresh data.
- Why is optimistic concurrency generally preferred in high-throughput, low-contention systems? — Optimistic concurrency avoids the overhead of acquiring locks upfront, allowing higher throughput when conflicts are rare, since most operations succeed on the first attempt and only the occasional conflicting write needs to retry.

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
- How can `findOneAndUpdate` with `$inc` be used to implement an atomic counter/sequence generator? — Calling `findOneAndUpdate` with an `$inc` operator on a dedicated counter document atomically increments and returns the new sequence value in one server-side operation, avoiding race conditions from a separate read-then-write approach.
- Why are atomic update operators preferable to a read-modify-write pattern in application code? — Update operators like `$inc` and `$push` execute atomically on the server in a single step, whereas a read-modify-write pattern in application code has a window where a concurrent write can be lost or overwritten between the read and the write.

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
- What causes a `WriteConflict` error in MongoDB, and which storage engine component detects it? — A `WriteConflict` occurs when two concurrent operations (often within transactions) attempt to modify the same document at the same time; MongoDB's WiredTiger storage engine detects this conflict and aborts one of the operations.
- How should an application respond when it receives a `TransientTransactionError`? — The application should catch the error label `TransientTransactionError` and retry the entire transaction from the beginning, since the error indicates the conflict was transient and likely to succeed on a subsequent attempt.
- How do write conflicts relate to the isolation guarantees of multi-document transactions? — Write conflicts are the mechanism by which MongoDB enforces snapshot isolation for transactions, ensuring that conflicting concurrent modifications to the same document are not silently merged but instead cause one transaction to abort and retry.

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
- What is the minimum recommended replica set topology for automatic failover, and why? — A minimum of three data-bearing (or voting) members is recommended, since a majority of members must be reachable to elect a new Primary, and a two-node set can't safely determine a majority if either node fails.
- What happens to in-flight writes on the Primary if it suddenly becomes unreachable? — Writes that had not yet been acknowledged with a strong enough write concern (e.g., `majority`) may be lost, while writes acknowledged by a majority of nodes before the failure are preserved and reflected in the newly elected Primary.
- How does a replica set differ from a sharded cluster in terms of what problem it solves? — A replica set solves high availability and data redundancy by keeping identical copies of the full data set on multiple nodes, while a sharded cluster solves horizontal scalability by partitioning data across multiple independent shards.

### Primary

The Primary is the single replica set member that receives all write operations at any given time. All writes are recorded to its oplog, which secondaries then pull and replay to stay in sync. Only one node can be Primary at a time within a replica set.

**Interview Questions:**
- Can a replica set have more than one Primary at the same time under normal operation? — No, under normal operation a replica set has exactly one Primary at any given time; MongoDB's election protocol is designed to prevent more than one node from believing it's Primary simultaneously.
- What happens to write requests sent to a node that is not currently Primary? — Write requests sent to a Secondary are rejected with a "not writable primary" error, since only the Primary accepts writes.

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
- Can a Secondary accept write operations directly from a client? — No, Secondaries only apply writes replicated from the Primary's oplog and reject direct write operations from clients.
- How would you configure a Secondary to never become Primary (e.g., for a dedicated analytics/backup node)? — Set that member's `priority` to `0` in the replica set configuration, which makes it ineligible to ever be elected Primary while still allowing it to vote and serve reads.
- What read preference would you use to allow reads from Secondaries? — Use a read preference mode such as `secondary`, `secondaryPreferred`, or `nearest` to allow (or prefer) routing reads to Secondary members.

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
- What mechanism does a replica set use to detect that the Primary is down? — Members exchange periodic heartbeats with each other, and when a member fails to receive heartbeat responses from the Primary within the configured timeout, it triggers an election to select a new Primary.
- Roughly how long does automatic failover typically take, and what happens to writes during that window? — Failover typically completes within a few seconds, during which time the replica set has no Primary and all write operations fail until a new Primary is elected.
- Can automatic failover be disabled or delayed for specific members (e.g., a reporting-only secondary)? — Yes, a member's `priority` can be set to `0` to make it ineligible for election entirely, effectively excluding it from ever becoming Primary during a failover.

### Elections

An election is the process by which replica set members vote to select a new Primary, based on factors like member priority, most recent oplog timestamp (most up-to-date data wins), and the number of votes each member has. A majority of voting members must be reachable and agree for an election to succeed, which is why an odd number of voting members (or an arbiter) is recommended.

```javascript
cfg = rs.conf();
cfg.members[0].priority = 2; // prefer this member as Primary
rs.reconfig(cfg);
```

**Interview Questions:**
- What factors influence which member wins an election to become Primary? — Election outcome is influenced by each member's configured `priority`, how up-to-date its oplog is (most recent data wins), and whether it can secure votes from a majority of the voting members.
- Why is it important to have an odd number of voting members in a replica set? — An odd number of voting members avoids ties when determining a majority, ensuring an election can always resolve to a clear winner without a deadlock.
- How does member `priority` influence election outcomes? — Higher `priority` values make a member more likely to be elected Primary when eligible, allowing administrators to bias elections toward preferred, typically higher-capacity or lower-latency, nodes.

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
- What is the tradeoff of using `secondaryPreferred` read preference? — `secondaryPreferred` offloads read load from the Primary and improves read scalability, but reads may return slightly stale data due to asynchronous replication lag on secondaries.
- Why might `nearest` be chosen for a globally distributed application? — `nearest` routes reads to whichever member has the lowest network latency to the client, which minimizes read latency for geographically distributed applications at the cost of potentially reading from a lagging secondary.
- How does read preference interact with read concern to determine overall consistency? — Read preference chooses which node serves a read, while read concern determines what guarantee the returned data must satisfy (e.g., majority-committed); together they determine both where and how consistent a read is, such as reading from a secondary but still requiring `majority` read concern.

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
- What does `w: "majority"` guarantee that `w: 1` does not? — `w: "majority"` guarantees the write has been acknowledged by a majority of voting replica set members, protecting it from being rolled back after a failover, whereas `w: 1` only requires acknowledgment from the Primary, risking loss if the Primary fails before replicating that write.
- What is the role of the `j` (journal) option in write concern? — The `j` option requires the acknowledging node(s) to have written the operation to the on-disk journal before acknowledging, protecting against data loss from an unexpected process crash or power failure.
- Why might an application use a weaker write concern for non-critical writes (e.g., logging)? — A weaker write concern like `w: 1` reduces write latency since it doesn't wait for replication acknowledgment, which is an acceptable trade-off for non-critical data like logs where occasional loss is tolerable.

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
- Why could data returned under `local` read concern later be "rolled back"? — Data read with `local` concern reflects the queried node's current state, which may include writes that haven't yet been acknowledged by a majority and could be undone if the Primary changes during a failover before replicating them.
- When would you use `majority` read concern together with `majority` write concern? — Use both together when an application needs "read your own writes" durability guarantees, ensuring that once a majority-acknowledged write is made, subsequent majority reads will reliably see it and it won't be rolled back.
- What is the difference between `majority` and `linearizable` read concern? — `majority` guarantees data acknowledged by a majority of nodes won't be rolled back, while `linearizable` additionally guarantees the read reflects the absolute latest completed majority write, even accounting for concurrent operations, at the cost of higher latency.

### Oplog

The oplog (operations log) is a special capped collection on each replica set member (`local.oplog.rs`) that records every write operation applied to the Primary, in idempotent form. Secondaries continuously tail and replay this log to stay synchronized. Its size determines the "replication window" — how far behind a secondary can fall before it can no longer catch up via normal replication.

```javascript
db.getReplicationInfo(); // shows oplog size and time window
rs.printReplicationInfo();
```

**Interview Questions:**
- Why must operations recorded in the oplog be idempotent? — Oplog entries must be idempotent because secondaries (and initial sync/resync processes) may need to reapply entries, and idempotent operations ensure reapplying them produces the same result without corrupting data.
- What happens if a Secondary falls behind further than the oplog's retention window? — If a Secondary falls too far behind, the oplog entries it needs may have already been overwritten (since the oplog is a capped collection), requiring the Secondary to perform a full initial resync from another member instead of normal replication.
- How would you resize the oplog on a running replica set member? — Use the `replSetResizeOplog` administrative command to change the oplog size on a running member without requiring a restart or full resync.

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
- What is the main purpose of adding an arbiter to a replica set? — An arbiter's main purpose is to provide an additional vote to help achieve a majority during elections (e.g., maintaining an odd number of voting members) without the storage and replication cost of a full data-bearing node.
- Why might MongoDB's documentation recommend avoiding arbiters in favor of additional data-bearing nodes when possible? — Arbiters don't hold data, so they don't add real redundancy, and their presence can complicate `majority` write concern calculations since a majority reachable but data-thin cluster may still not guarantee durability the way an all-data-bearing majority would.
- Can an arbiter ever be elected Primary? — No, an arbiter never holds data and cannot be elected Primary; it can only participate in voting.

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
- What problem does sharding solve that replication alone cannot? — Sharding solves horizontal scalability of storage capacity and write/read throughput by partitioning data across multiple servers, whereas replication alone only provides redundancy and read scaling while every replica still holds the entire dataset.
- What are the three main components of a sharded cluster and what does each do? — Shards store the actual partitioned data (typically as replica sets), config servers store the cluster's metadata mapping chunks to shards, and `mongos` routers direct client queries to the appropriate shard(s) based on that metadata.
- What happens to a query that doesn't include the shard key? — A query without the shard key becomes a "scatter-gather" operation, where `mongos` must broadcast the query to every shard and merge the results, which is slower than a targeted single-shard query.
- How does sharding interact with replica sets within each shard? — Each shard is typically deployed as its own replica set, so sharding provides horizontal scalability across shards while replication within each shard continues to provide high availability and failover for that shard's data.

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
- What makes a good shard key in terms of cardinality, frequency, and monotonicity? — A good shard key has high cardinality (many distinct values so data can be split into many chunks), even frequency (no single value dominates, avoiding oversized chunks), and low monotonicity (avoiding ever-increasing values that concentrate new writes onto a single shard).
- What is a "hot shard" and how does shard key choice cause it? — A "hot shard" is a shard that receives a disproportionate share of reads/writes, typically caused by choosing a monotonically increasing shard key (like a timestamp or auto-incrementing ID) that always routes the newest data to the same shard.
- Can you change a shard key after a collection is sharded? — Historically no, changing a shard key required un-sharding, dropping, and reloading the collection with a new key, though newer MongoDB versions support limited in-place shard key refinement (adding suffix fields) without a full reload.
- How does a compound shard key affect query routing compared to a single-field key? — A compound shard key allows `mongos` to target a single shard efficiently for queries that include at least the leading field(s) of the key, similar to compound index prefix matching, while queries omitting the leading field(s) fall back to a scatter-gather across all shards.

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
- What metadata do config servers store? — Config servers store the sharded cluster's metadata, including the mapping of chunk ranges to shards, cluster authentication/settings, and other cluster-wide configuration used by `mongos` routers.
- Why are config servers deployed as a replica set rather than a single node? — Deploying config servers as a replica set (CSRS) ensures the critical cluster metadata itself is highly available and tolerant of individual node failures, avoiding a single point of failure for cluster routing.
- What happens to a sharded cluster if the config server replica set becomes unavailable? — Chunk migrations and metadata updates halt, and while `mongos` instances can often continue routing briefly using cached metadata, most cluster management operations and new `mongos` connections are impacted until the config servers recover.

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
- What is the role of `mongos` in a sharded cluster? — `mongos` acts as a stateless query router, fetching and caching cluster metadata from config servers to route each client query or write to the appropriate shard(s) and merge results as needed.
- Why is `mongos` considered stateless, and what does it cache? — `mongos` holds no persistent data of its own; it only caches the cluster metadata (chunk-to-shard mappings) fetched from config servers, which is why multiple `mongos` instances can be run interchangeably for redundancy.
- What is a scatter-gather query and why is it less efficient than a targeted query? — A scatter-gather query is one that must be broadcast to every shard because it doesn't include the shard key, requiring `mongos` to fan out the request and merge results from all shards, which is slower than a targeted query routed to a single shard.

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
- What triggers a chunk migration? — A chunk migration is triggered by the balancer when it detects an uneven distribution of chunks across shards, moving chunks from over-loaded shards to under-loaded ones to restore balance.
- How does MongoDB ensure data consistency during a chunk migration? — The source shard continues serving reads/writes for the chunk while the destination shard copies the data and any concurrent oplog changes, and only once the copy is complete does MongoDB atomically update the chunk's ownership in the config server metadata before the source deletes its copy.
- Can chunk migrations be scheduled or throttled, and why would you want that? — Yes, the balancer's activity window can be scheduled (e.g., only running overnight) and migrations can be throttled, which is useful to avoid consuming CPU, memory, and I/O resources during peak business hours.

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
- What is the balancer responsible for and where does it run? — The balancer monitors chunk distribution across shards and automatically migrates chunks from over-loaded to under-loaded shards to maintain balance; in modern MongoDB versions it runs as part of the config server replica set's primary.
- How would you prevent the balancer from running during business hours? — Configure a balancing activity window (e.g., `activeWindow` with start/stop times) so the balancer only migrates chunks during off-peak hours like overnight, or disable it temporarily with `sh.stopBalancer()` before a maintenance window.
- What symptoms would indicate the balancer is struggling to keep a cluster balanced? — Symptoms include persistently uneven chunk counts across shards, the balancer running continuously without converging, or repeated jumbo chunk warnings, often caused by a poorly chosen shard key with low cardinality or high monotonicity.

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
- Why is a monotonically increasing field usually a poor shard key choice? — A monotonically increasing field like a timestamp always routes new writes to the shard owning the highest current range, concentrating all write load onto a single "hot" shard instead of spreading it evenly.
- What is the trade-off between hashed and ranged shard keys? — Hashed shard keys distribute writes very evenly but destroy value ordering, making range queries inefficient (scatter-gather), while ranged shard keys preserve efficient range queries but risk uneven write distribution if the key is monotonic.
- How do you evaluate cardinality and frequency when selecting a shard key? — Evaluate cardinality by checking how many distinct values the field can take (higher is better for enabling many chunks) and frequency by checking whether any single value appears disproportionately often (high frequency for one value risks an oversized, unsplittable chunk).
- How would you shard a collection to support both even write distribution and efficient range queries? — Use a compound shard key combining a high-cardinality, evenly-distributed field (like a hashed or randomized prefix) with a field needed for range queries (like a date), so writes spread evenly while range queries on the secondary field remain efficient within a scoped prefix.

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
- What is zone sharding used for in a real-world multi-region deployment? — Zone sharding is used to pin specific ranges of shard key values (e.g., customers in a particular region) to specific shards, keeping data physically located near its users for latency, data residency, or compliance reasons.
- How does zone sharding relate to shard key design? — Zone sharding requires the shard key to include a field (like `region`) that can be used to define zone ranges, since zones are defined as ranges of shard key values that must be routed to particular shards.
- Give an example of a compliance requirement that zone sharding could help satisfy. — GDPR requirements that EU customer data physically reside within EU data centers can be satisfied by zoning EU-tagged shard key ranges to shards hosted in EU regions.

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
- Explain the CAP theorem in your own words and how it applies to MongoDB. — The CAP theorem states a distributed system can only guarantee two of Consistency, Availability, and Partition tolerance during a network partition; MongoDB, being a distributed replica set/sharded system, must choose how to behave when nodes can't communicate, and it defaults to prioritizing consistency.
- Why is MongoDB typically categorized as CP rather than AP? — MongoDB is categorized as CP because it routes all writes through a single Primary and only considers data durable once acknowledged per the configured write concern, favoring consistency over allowing every node to independently accept writes during a partition.
- How can you tune MongoDB's behavior to favor availability over strict consistency? — You can use weaker read/write concerns (e.g., `w:1`, `local` read concern) and secondary-friendly read preferences (`secondaryPreferred`, `nearest`), which allow reads and writes to succeed with less coordination, trading consistency guarantees for availability and latency.
- What happens to write availability during a primary election in a MongoDB replica set? — Writes are unavailable for the brief window (typically a few seconds) between the old Primary becoming unreachable and a new Primary being elected, since only a Primary can accept writes.

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
- What causes eventual consistency in a MongoDB replica set? — Secondaries apply oplog entries from the Primary asynchronously, so there's a natural replication lag during which a secondary's data may not yet reflect the Primary's latest writes, causing eventual (rather than immediate) consistency for secondary reads.
- In what scenarios is eventual consistency an acceptable trade-off? — It's acceptable for read-heavy, non-critical workloads like reporting, analytics dashboards, or offloading traffic from the Primary, where slightly stale data doesn't cause business harm.
- How would you detect and monitor replication lag? — Use `rs.printSecondaryReplicationInfo()` or monitor the `optimeDate` differences between the Primary and Secondaries (also exposed via monitoring tools like Atlas or `db.serverStatus()`'s replication metrics) to detect and track lag.

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
- What are the five read preference modes and when would you use each? — `primary` (default, strongest consistency, use for critical reads), `primaryPreferred` (fall back to secondary only if primary is down), `secondary` (always read from secondaries, for offloading load), `secondaryPreferred` (prefer secondaries but fall back to primary), and `nearest` (lowest latency node, for latency-sensitive global apps).
- What is the risk of using `secondary` read preference for critical reads? — Since secondaries replicate asynchronously, a `secondary` read may return stale data that doesn't reflect the most recent writes, which is risky for critical reads that require up-to-date information.
- How does `nearest` differ from `secondaryPreferred` in terms of node selection? — `nearest` selects whichever member (primary or secondary) has the lowest network latency to the client, while `secondaryPreferred` specifically prefers any secondary over the primary regardless of latency, only falling back to the primary if no secondary is available.

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
- What is the difference between `local` and `majority` read concern? — `local` returns whatever data is currently on the queried node with no guarantee it has been replicated to a majority, while `majority` only returns data that has been acknowledged by a majority of replica set members, guaranteeing it won't later be rolled back.
- Why would `linearizable` read concern be slower than `majority`? — `linearizable` requires the read to confirm it reflects all previously completed majority writes, which involves additional coordination (including waiting on a no-op write to the majority) beyond simply checking majority-committed data, adding latency.
- When is `snapshot` read concern used? — `snapshot` read concern is used within multi-document transactions to give all reads within the transaction a consistent, point-in-time view of the data as of the transaction's start.

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
- What does `w: "majority"` protect against that `w: 1` does not? — `w: "majority"` protects against the write being rolled back after a primary failover by ensuring it's replicated to a majority of nodes first, whereas `w: 1` only confirms the primary accepted it, risking loss if the primary fails before replicating.
- What role does the `j` option play in write concern? — The `j` option requires the write to be committed to the on-disk journal on the acknowledging node(s) before the write is considered successful, protecting against loss from a process crash even before the next checkpoint.
- How would you choose write concern differently for an audit log vs. a financial transaction? — An audit log might use a lighter write concern like `w: 1` for lower latency since occasional loss is tolerable, while a financial transaction should use `w: "majority"` with `j: true` to guarantee durability and prevent silent data loss.

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
- What problem does causal consistency solve for applications reading from secondaries? — It solves the problem of a client reading stale data from a secondary immediately after making a write, guaranteeing that causally related operations (a write followed by a dependent read) are observed in the correct order even across nodes.
- How does MongoDB track causal relationships between operations? — MongoDB uses causally consistent client sessions that track logical `operationTime` timestamps, passing them between operations so subsequent reads within the session wait until the relevant data has replicated to the node serving the read.
- What is the relationship between causally consistent sessions and read/write concern? — Causally consistent sessions work alongside read/write concern (typically requiring at least `majority` read/write concern) to guarantee that causal ordering is honored even when reads are routed to different nodes than the originating writes.

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
- What concurrency model does WiredTiger use compared to MongoDB's legacy MMAPv1 engine? — WiredTiger uses document-level locking, allowing concurrent writes to different documents to proceed in parallel, whereas the legacy MMAPv1 engine used coarser collection-level (and earlier database/global) locking that serialized many unrelated writes.
- How do checkpoints and the journal work together to guarantee durability? — The journal records every write immediately as a write-ahead log, providing durability between checkpoints, while periodic checkpoints flush a consistent snapshot of the in-memory data to disk, reducing how much journal data needs replaying during crash recovery.
- Why does each collection and index get its own WiredTiger file? — Separate files per collection/index allow WiredTiger to apply fine-grained I/O, compression, and configuration settings independently, and make operations like dropping a collection simple file removal rather than complex in-place data manipulation.

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
- Why does MongoDB compress data by default and what's the trade-off? — Compression reduces disk usage and I/O by storing fewer bytes for the same data, at the cost of additional CPU time needed to compress and decompress data on every read/write.
- When would you choose `zlib` over `snappy`? — Choose `zlib` when storage space is more constrained than CPU capacity and the workload isn't extremely write-heavy, since `zlib` achieves a higher compression ratio at a higher CPU cost than the default `snappy`.
- How does index prefix compression differ from block compression of documents? — Index prefix compression exploits the fact that adjacent keys in a sorted B-tree index often share common prefixes, storing only the differing suffix, while block compression (used for document data) applies general-purpose compression algorithms like snappy/zlib/zstd to blocks of raw document data.

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
- What does the journal protect against that checkpoints alone do not? — The journal protects against losing writes that occurred after the last checkpoint but before a crash, since checkpoints only persist data periodically while the journal records every write immediately as it happens.
- What happens if MongoDB crashes between two checkpoints, with journaling enabled? — On restart, MongoDB replays the journal entries recorded since the last checkpoint to bring the data files back to a consistent state reflecting all acknowledged writes, avoiding data loss.
- What is the performance trade-off of using `j: true` on every write? — Requiring a journal flush acknowledgment on every write adds latency since the write must wait for the fsync to complete, trading some throughput/latency for stronger per-operation durability.

### Checkpoints

A checkpoint is a consistent, point-in-time snapshot of the data that WiredTiger writes to disk, by default every 60 seconds or after 2GB of journal data has accumulated, whichever comes first. Between checkpoints, durability is provided by the journal; checkpoints simply reduce the amount of journal data that would need to be replayed during crash recovery, and they are how data actually becomes durable on disk in the storage engine's data files.

**Advantages:**
- Bounds crash recovery time by limiting how much journal must be replayed
- Provides a consistent on-disk snapshot for tools like file-system backups

**Disadvantages:**
- Checkpointing consumes disk I/O and can cause momentary latency spikes on busy systems

**Interview Questions:**
- What triggers a WiredTiger checkpoint by default? — A checkpoint is triggered by default every 60 seconds or after 2GB of journal data has accumulated, whichever comes first.
- How do checkpoints relate to crash recovery time? — Checkpoints bound crash recovery time by limiting how much journal data must be replayed on restart, since recovery only needs to reapply operations recorded after the most recent checkpoint.
- Why might a filesystem snapshot backup want to be taken right after a checkpoint? — Taking a snapshot right after a checkpoint captures the data files in the most recently known consistent, fully-flushed state, minimizing the amount of journal replay needed to make the backup usable.

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
- What is the default WiredTiger cache size formula? — By default, WiredTiger's cache size is 50% of (total RAM minus 1GB), or 256MB, whichever is greater.
- What happens when the working set doesn't fit in the WiredTiger cache? — WiredTiger must evict pages from cache to make room for new data, increasing disk I/O and degrading performance since frequently accessed data may need to be re-read from disk.
- Which `serverStatus` metrics would you check to diagnose cache pressure? — Check `wiredTiger.cache` metrics such as "bytes currently in the cache", "tracked dirty bytes in the cache", and eviction-related counters like "pages evicted by application threads" to diagnose cache pressure.

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
- What is `$jsonSchema` used for in MongoDB? — `$jsonSchema` defines a validator that enforces structure, required fields, types, and value constraints on documents in a collection, giving MongoDB relational-like data integrity guarantees while remaining otherwise schemaless.
- How does document validation reconcile with MongoDB's schemaless design philosophy? — Validation is optional and can be applied incrementally or loosely (via `moderate`/`warn` settings), letting teams keep the flexibility of a document model during early development while adding guardrails as the application and data requirements mature.
- Give an example of a field constraint you might enforce with JSON Schema validation. — You might enforce that a `total` field is a non-negative `double` (`{ bsonType: "double", minimum: 0 }`) or that a `status` field is restricted to an enumerated set of valid values like `["pending", "shipped", "delivered", "cancelled"]`.

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
- How would you enforce that a field must match one of a fixed set of values? — Use the `enum` keyword within the field's schema, e.g. `{ status: { enum: ["pending", "shipped", "delivered"] } }`, which rejects any document where the field's value isn't in the specified list.
- How can you validate nested subdocuments or array elements? — Define nested `properties` schemas for subdocuments and an `items` schema for array elements within the `$jsonSchema` validator, allowing validation rules to apply recursively to nested structures.
- What operators let you combine multiple validation rules together? — Logical operators like `allOf`, `anyOf`, `oneOf`, and `not` let you combine multiple validation rules or express more complex conditional constraints within a single validator.

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
- What is the difference between `strict` and `moderate` validation levels? — `strict` validates all inserts and updates against the rules, while `moderate` only validates inserts and updates to documents that already satisfy the rules, letting existing non-conforming documents continue to be edited without being forced into immediate compliance.
- Why would you use `moderate` when introducing validation on an existing collection? — `moderate` avoids breaking application writes to legacy documents that don't yet conform to the new rules, allowing a gradual migration toward full compliance instead of an abrupt cutover.
- How would you temporarily disable validation entirely? — Set `validationLevel` (or the validator itself) to `off`, or run `collMod` to remove/relax the validator temporarily, then re-enable it once ready.

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
- What's the practical use case for `validationAction: "warn"`? — `warn` lets you test new validation rules in production by logging violations without rejecting any writes, so you can observe how much existing data or traffic would be affected before enforcing the rule strictly.
- How would you safely roll out a new stricter validation rule to a production collection? — Start with `validationAction: "warn"` and `validationLevel: "moderate"`, monitor the logs for violations, fix or migrate offending data, then progressively switch to `validationAction: "error"` and `validationLevel: "strict"` once confident.
- Where would you find the warnings logged when using the `warn` action? — Validation warnings are written to the MongoDB server log (`mongod` log file or configured log destination).

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
- What authentication mechanisms does MongoDB support? — MongoDB supports SCRAM (default username/password), x.509 certificate authentication, and enterprise-only mechanisms like LDAP and Kerberos.
- Why should authentication always be enabled in production, even on an internal network? — Internal networks can still be breached, misconfigured, or accessed by insider threats, so enabling authentication ensures only verified clients can perform operations regardless of network trust assumptions, following defense-in-depth principles.
- How does x.509 authentication differ from SCRAM? — x.509 authenticates clients using cryptographic certificates rather than a shared username/password, which is useful for mutual TLS setups and avoids managing passwords, while SCRAM uses a salted challenge-response protocol based on a username and password.

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
- How does role-based authorization work in MongoDB? — Each user is assigned one or more roles, and each role bundles a set of privileges (specific actions on specific resources), so a user's effective permissions are the union of privileges granted by all their assigned roles.
- What is the principle of least privilege and how does RBAC support it? — The principle of least privilege means granting only the minimum access necessary to perform a task; RBAC supports this by letting administrators define narrowly scoped custom roles (e.g., read-only access to one collection) rather than broad, all-encompassing permissions.
- How would you grant a service account read-only access to a single collection? — Create a custom role with a single privilege granting `find` (and similar read) actions scoped to that specific collection's resource, then assign that role to the service account's user.

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
- Where does MongoDB store user credentials and role definitions? — MongoDB stores user credentials and role assignments in the `admin.system.users` and `admin.system.roles` collections.
- Can a single user have roles that span multiple databases? How? — Yes, when creating a user you can assign an array of role objects each specifying its own `db`, allowing a single user to hold different roles scoped to different databases.
- What is `authSource` and why does it matter when authenticating? — `authSource` specifies which database holds the user's credentials to authenticate against (often `admin` even when accessing a different database), and it matters because the client must point to the correct database or authentication will fail.

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
- Why is RBAC preferred over granting privileges directly to individual users? — RBAC lets administrators update a role's privileges once and have the change automatically apply to every user holding that role, making permission management scalable and consistent compared to managing privileges per-user.
- How would you design roles for a microservices architecture with several services accessing shared collections? — Create narrowly scoped custom roles per service reflecting exactly the collections and actions that service needs (e.g., an order service gets read/write on `orders` only), assigning each service its own dedicated user with only its required role(s).
- What built-in MongoDB roles exist for common administrative tasks? — Built-in roles include `read`, `readWrite`, `dbAdmin`, `userAdmin`, `clusterAdmin`, `backup`, and `restore`, among others, covering common database and cluster administration needs without requiring custom role definitions.

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
- What network-level threats does TLS protect MongoDB against? — TLS protects against eavesdropping and man-in-the-middle attacks by encrypting data in transit between clients, drivers, and cluster members, preventing attackers on the network from reading or tampering with traffic.
- What MongoDB configuration options control TLS enforcement? — The `net.tls.mode` setting (e.g., `requireTLS`) along with `certificateKeyFile` and `CAFile` paths in `mongod.conf` control whether and how TLS is enforced for client and internal cluster connections.
- How can TLS certificates also be used for client authentication? — MongoDB supports x.509 certificate authentication, where a client presents a valid TLS certificate signed by a trusted CA as proof of identity instead of (or in addition to) a username/password.

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
- What is the difference between encryption at rest and TLS in transit? — Encryption at rest protects data stored on disk from being read if the physical storage or backups are stolen, while TLS in transit protects data as it moves across the network between clients and servers; the two address different threat surfaces and are typically used together.
- What options exist for encrypting MongoDB data at rest on the Community edition? — Community edition lacks native WiredTiger encryption, so encryption at rest must be achieved at the filesystem or disk layer, such as using LUKS or encrypted cloud disk volumes (e.g., encrypted EBS).
- What key management approaches does MongoDB Enterprise support for encryption at rest? — Enterprise supports a local key file or integration with an external key management system via the KMIP protocol for managing the master encryption key used by native WiredTiger encryption.

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
- What kinds of events can MongoDB's auditing feature capture? — Auditing can capture authentication attempts, authorization checks, CRUD operations, and administrative actions such as user or role changes.
- Why would you configure an audit filter rather than logging every event? — An audit filter narrows captured events (e.g., only failed authentication attempts or actions by a specific user) to reduce log volume and noise, making the audit trail more manageable and focused on relevant security events.
- What compliance scenarios typically require database auditing? — Compliance frameworks like SOC 2, HIPAA, and PCI-DSS typically require an auditable trail of who accessed or modified sensitive data and when, which MongoDB's auditing feature helps satisfy.

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
- How does Client-Side Field Level Encryption differ from encryption at rest and TLS? — CSFLE encrypts specific fields on the client before they're ever sent to the server, so even privileged database administrators or anyone with raw data file access only see ciphertext, whereas encryption at rest and TLS protect data at the storage and network layers but leave data readable to anyone with legitimate server-side access.
- Who manages the encryption keys in a CSFLE setup, and why does that matter? — Encryption keys are managed via an external key management system (local key file, AWS KMS, Azure Key Vault, or GCP KMS) and are never exposed to the MongoDB server, which matters because it ensures the database itself never has the ability to decrypt the protected fields.
- What are the query limitations on fields encrypted with CSFLE? — Encrypted fields generally cannot be used in most query operators or indexed for range queries; only newer "queryable encryption" variants support limited equality (and in some versions range) queries directly on encrypted fields.

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
- What does `mongodump` actually capture, and in what format? — `mongodump` captures documents (and optionally indexes/metadata) from a database or collection and exports them as binary BSON files that can later be restored with `mongorestore`.
- How would you take a backup without impacting the primary's performance? — Run `mongodump` against a secondary node using an appropriate `--readPreference` setting, offloading the backup's read load away from the primary that's serving live traffic.
- What are the limitations of `mongodump` for very large or sharded deployments? — `mongodump` is not inherently a point-in-time consistent snapshot across an entire sharded cluster unless carefully coordinated, and it's slower and more resource-intensive than filesystem or block-level snapshots for very large datasets.

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
- What does the `--drop` flag do during a restore, and when would you use it? — `--drop` tells `mongorestore` to drop each target collection before restoring its data, which you'd use when you want the restore to fully replace existing collection contents rather than merge with them.
- How can you restore a backup into a differently named database for testing? — Use the `--nsFrom` and `--nsTo` options to remap the namespace during restore, for example restoring `ecommerceDb.*` into `ecommerceDbTest.*` without altering the original backup files.
- How would you speed up a restore of a very large dataset? — Increase parallelism using flags like `--numParallelCollections` and `--numInsertionWorkersPerCollection`, which restore multiple collections and insert documents using multiple worker threads concurrently.

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
- What is the difference between a regular backup and point-in-time recovery? — A regular backup restores data to the exact moment the backup was taken, while point-in-time recovery can restore data to any specific moment (even between backups) by combining a base snapshot with replayed oplog entries up to the desired timestamp.
- How does the oplog enable point-in-time recovery? — Since the oplog records every write operation in sequence, replaying oplog entries from a base snapshot up to a chosen timestamp reconstructs the exact database state at that specific point in time.
- What operational requirement (oplog retention) is necessary to support PITR? — The oplog must be retained (not overwritten) for at least as long as the desired recovery window, meaning oplog size/retention must be large enough to cover the time span between backups plus any additional buffer needed for recovery scenarios.

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
- What factors determine whether you'd choose logical backups vs. filesystem snapshots? — Dataset size, required restore speed, and whether the cluster is sharded all matter: logical backups (`mongodump`) suit smaller datasets or selective exports, while filesystem snapshots scale better for very large datasets but require careful coordination across shards and config servers for a sharded cluster.
- Why is backing up a sharded cluster more complex than a single replica set? — A sharded cluster's data is spread across multiple independent shards plus config servers, so achieving a consistent, point-in-time backup requires coordinating snapshots across all of them simultaneously, unlike a single replica set where one consistent snapshot suffices.
- What are RPO and RTO, and how do they influence backup strategy? — RPO (Recovery Point Objective) defines the maximum acceptable data loss measured in time, and RTO (Recovery Time Objective) defines the maximum acceptable downtime during recovery; tighter RPO/RTO targets push toward more frequent backups, point-in-time recovery, and faster restore mechanisms like snapshots over logical dumps.

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
- What is the difference between a backup strategy and a full disaster recovery plan? — A backup strategy focuses narrowly on taking and storing copies of data, while a full disaster recovery plan additionally covers geographically distributed infrastructure, documented and tested restore procedures, and defined RPO/RTO targets to survive catastrophic failures like a regional outage.
- How would you design replica set topology to survive a regional outage? — Distribute replica set members across multiple availability zones or regions (e.g., primary and one secondary in Region A, another secondary in Region B) so that the loss of a single region still leaves enough members to maintain quorum and potentially elect a new primary.
- What are RPO and RTO, and how would you define them for a critical production database? — RPO is the maximum tolerable data loss window and RTO is the maximum tolerable downtime; for a critical production database you might define an RPO of minutes (via continuous oplog-based backups) and an RTO of under an hour, backed by tested automated restore procedures.
- Why is periodically testing a restore procedure just as important as taking the backup itself? — A backup that has never been restored successfully is unproven; regular restore drills catch issues like corrupted backups, missing files, or outdated procedures before they're discovered during an actual emergency when there's no room for error.

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
- What is the difference between `COLLSCAN` and `IXSCAN` in an `explain()` output? — `COLLSCAN` indicates MongoDB scanned every document in the collection to find matches, while `IXSCAN` indicates it used an index to efficiently locate candidate documents, typically examining far fewer entries.
- What is the ESR (Equality, Sort, Range) rule for compound index design? — The ESR rule recommends ordering compound index fields with equality filters first, then sort fields, then range filters, so the index can most efficiently narrow, order, and bound the result set in a single traversal.
- How would you diagnose why a query is running slowly in production? — Use the profiler or slow query log to identify the offending query, then run `explain("executionStats")` on it to check whether it's using an appropriate index, examining far more documents than it returns, or missing an index entirely.

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
- How would you identify unused indexes in a production collection? — Run the `$indexStats` aggregation stage to see each index's usage count since the last restart, and any index showing zero or negligible usage over a representative period is a candidate for removal.
- What's the trade-off of adding more indexes to speed up reads? — More indexes speed up the specific reads they support but add overhead to every insert, update, and delete since each index must also be maintained, along with increased RAM and disk consumption.
- When would you use a partial index instead of a full index? — Use a partial index when queries consistently filter on a specific condition (e.g., only "shipped" orders) so only the relevant subset of documents needs indexing, reducing index size and write overhead compared to indexing the entire collection.

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
- Why is connection pooling important for MongoDB driver performance? — Connection pooling reuses established TCP (and TLS) connections across operations, avoiding the latency and CPU cost of repeatedly opening and closing connections for every request under load.
- What happens if `maxPoolSize` is set too low for a high-concurrency service? — Operations queue up waiting for an available connection from the pool, increasing request latency and potentially causing timeouts under high concurrent load.
- How would you size connection pools across multiple application instances sharing one MongoDB cluster? — Size each instance's pool so the sum across all application instances stays comfortably within the MongoDB server's `maxIncomingConnections` limit, accounting for expected concurrency per instance and the total number of instances that will connect simultaneously.

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
- What is the difference between ordered and unordered bulk writes? — Ordered bulk writes execute sequentially and stop at the first error, preserving the guaranteed order of operations, while unordered bulk writes continue past errors and may execute in any order, reporting all successes and failures at the end.
- Why would unordered bulk writes generally perform better? — Unordered bulk writes can be executed in parallel and don't need to preserve strict sequencing, allowing the server to process them more efficiently than the sequential, stop-on-error behavior of ordered writes.
- What happens to remaining operations in an ordered bulk write after one fails? — In an ordered bulk write, all operations after the first failure are not executed at all; the batch stops immediately upon encountering the error.

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
- Why is batch processing more efficient than per-document processing for large datasets? — Processing data in batches amortizes network round-trip and per-operation overhead across many documents at once, rather than paying that fixed cost for every single document individually.
- How does cursor `batchSize` affect memory usage and round-trips? — A larger `batchSize` retrieves more documents per network round-trip, reducing the number of round-trips needed but increasing the memory required to hold each batch on the client.
- How would you handle a failure partway through processing a large batch job? — Design the job to be idempotent and track progress (e.g., a checkpoint or last-processed ID) so it can safely resume from where it left off, or use bulk writes with `ordered: false` combined with per-item error handling and retry logic to isolate failures to individual items.

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
- What are the differences between profiling levels 0, 1, and 2? — Level 0 disables profiling entirely, level 1 logs only operations slower than a configurable threshold (`slowms`), and level 2 logs every single operation regardless of duration, which is very verbose and typically reserved for short-term debugging.
- Where does MongoDB store profiling data, and what are the implications of that? — Profiling data is stored in the `system.profile` capped collection within each database, meaning older entries are automatically overwritten once the collection reaches its size limit, so data must be exported or analyzed promptly for long-term trend analysis.
- What overhead considerations apply to running the profiler in production? — Level 1 with a sensible threshold has low overhead and is generally safe for production, but level 2 logs every operation and can noticeably degrade performance and consume significant disk space, so it should only be used briefly and deliberately.

### Slow Query Analysis

Slow query analysis is the practice of identifying, inspecting, and fixing operations that exceed acceptable latency thresholds, typically by combining the database profiler or `mongod` log's slow-query entries (operations exceeding `slowms`, default 100ms) with `explain()` to understand why a specific query is slow (missing index, poor index selectivity, large result sets, or query patterns causing collection scans).

```bash
# Search the mongod log for slow query entries
grep -i "COMMAND" /var/log/mongodb/mongod.log | grep -i "durationMillis" | awk '$NF+0 > 200'
```

**Interview Questions:**
- What is a practical workflow for diagnosing a slow query in production? — First identify the offending query via the profiler or slow-query log entries exceeding `slowms`, then run `explain("executionStats")` on it to see whether it's using an index efficiently, and finally add or adjust indexes (or rewrite the query) based on what the plan reveals.
- How does `explain("executionStats")` help identify the root cause of slowness? — It reveals the chosen execution plan (`IXSCAN` vs `COLLSCAN`), the number of documents examined versus returned, and time spent, making it clear whether the query is missing an index, has poor selectivity, or is scanning far more data than necessary.
- What are common root causes of slow queries in MongoDB (missing indexes, large scans, poor shard key, etc.)? — Common causes include missing or poorly designed indexes forcing collection scans, queries that don't align with the ESR rule for compound indexes, an inefficient shard key causing scatter-gather queries across all shards, and unbounded result sets lacking pagination.

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
- What is the "working set" and why does its relationship to available RAM matter so much for performance? — The working set is the subset of data and indexes actively accessed by the application's typical workload; when it fits entirely in the WiredTiger cache/RAM, reads are served from memory, but once it exceeds available memory, MongoDB must fetch pages from disk far more often, significantly increasing latency.
- What metrics would you monitor to detect memory pressure in MongoDB? — Monitor page fault counts (`serverStatus().extra_info.page_faults`), WiredTiger cache eviction rate, and cache usage percentage, all of which rise when the working set no longer comfortably fits in available memory.
- How would you approach capacity planning for RAM on a new MongoDB deployment? — Estimate the expected working set size (active data plus indexes) based on projected data volume and access patterns, provision RAM with headroom above that estimate, and monitor page faults and cache eviction rates post-launch to validate and adjust sizing over time.

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
- What are the different MongoDB profiler levels and when would you use each? — Level 0 (off) is used when profiling isn't needed, level 1 (slow operations only) is the safe default for production to capture problematic queries, and level 2 (all operations) is reserved for short, targeted debugging sessions due to its overhead.
- Where is profiler data stored and what are the risks of leaving profiling on in production? — Profiler data is stored in the per-database `system.profile` capped collection; leaving level 2 profiling on in production risks meaningful performance overhead and rapid rollover of the capped collection, losing historical data.
- How would you use the profiler output to identify a missing index? — Look for slow operations in `system.profile` with a high ratio of documents scanned (`docsExamined`) to documents returned (`nreturned`), which indicates the query is scanning far more data than needed and would likely benefit from a supporting index.
- What is the performance impact of enabling profiling level 2 on a busy cluster? — Level 2 logs every single operation, adding CPU and I/O overhead proportional to the cluster's total operation throughput, which can measurably slow down a busy production cluster and should only be enabled briefly and deliberately.

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
- What key sections of `serverStatus()` would you check first when diagnosing a performance incident? — Typically check `connections` (for pool exhaustion), `opcounters` (for traffic spikes), `wiredTiger.cache` (for cache pressure/eviction), and replication-related fields (for lag) as a fast first pass.
- How can `serverStatus()` help detect connection pool exhaustion? — The `connections` section reports current, available, and total connections; a `current` value approaching the configured maximum, combined with rising queued/rejected connections, indicates the pool is being exhausted.
- What WiredTiger metrics in `serverStatus()` indicate cache pressure? — High `wiredTiger.cache` eviction rates (pages evicted per second) and a cache usage percentage close to the configured cache size both indicate the working set is outgrowing available cache memory.

### Database Statistics

The `dbStats` command reports storage-level metrics for a single database, such as total data size, storage size, index size, and object/collection counts. It's useful for capacity planning and tracking storage growth trends over time.

```javascript
use myDatabase
db.stats()
```

**Interview Questions:**
- What is the difference between `dataSize` and `storageSize` in `dbStats()` output? — `dataSize` reflects the logical (uncompressed) size of the data, while `storageSize` reflects the actual space allocated on disk, which can be smaller due to WiredTiger compression or larger due to fragmentation.
- How would you use `dbStats()` to plan disk capacity for a growing collection? — Track `storageSize` and `indexSize` over time to observe growth trends, then project future storage needs and provision disk capacity with sufficient headroom before the current trend exhausts available space.

### Collection Statistics

The `collStats` command (or `db.collection.stats()`) provides detailed size, document count, average object size, and index size information for a specific collection, plus sharding-specific data (like chunk distribution) when run against a sharded collection.

```javascript
db.orders.stats()

// Sharding-aware stats
db.orders.stats({ scale: 1024 * 1024 })
```

**Interview Questions:**
- How do you find the average document size of a collection and why does it matter? — The `avgObjSize` field in `collStats()`/`db.collection.stats()` output reports this directly; it matters because it affects storage planning, network transfer costs, and whether documents are approaching the 16MB BSON limit.
- What extra information does `collStats` return for a sharded collection compared to an unsharded one? — For a sharded collection, `collStats` additionally reports per-shard document counts, sizes, and chunk distribution, revealing whether data is balanced evenly across shards.

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
- How would you identify and safely remove unused indexes from a large production collection? — Query `$indexStats` over a representative time period (covering peak and off-peak traffic and spanning any recent failovers) and drop indexes showing consistently zero or negligible usage counts.
- Why might `$indexStats` counters be misleading right after a replica set election? — Usage counters are maintained in memory per `mongod` process and reset on restart or failover, so a newly elected primary/secondary will show artificially low counts until it has been running long enough to reflect real usage patterns.

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
- Which metrics best indicate that a collection is missing a critical index? — A high query targeting ratio (documents scanned per document returned) is the clearest signal, often paired with rising `COLLSCAN` counts in `opcounters`/profiler data and elevated query latency.
- What is FTDC and how is it used for post-incident diagnostics? — FTDC (Full Time Diagnostic Data Capture) is a lightweight background mechanism that continuously records diagnostic metrics to compact binary files on disk, which engineers can later decode and analyze to reconstruct exactly what the server was doing before, during, and after an incident.
- How would you set up alerting for replication lag in a production cluster? — Feed replication lag metrics (from `rs.status()` or `serverStatus()`) into a monitoring system like Prometheus/Grafana or Atlas, and configure an alert to fire when lag exceeds a threshold that would risk stale reads or violate SLA commitments.

### Mongostat and Mongotop

`mongostat` provides a `vmstat`-like, per-second view of key server counters (inserts, queries, updates, deletes, connections, cache metrics) directly in the terminal, useful for a quick live health check. `mongotop` shows the amount of time each collection spends performing read and write operations, helping to quickly spot which collection is the hot spot.

```bash
# Live server-wide counters refreshed every second
mongostat --host localhost:27017

# Per-collection read/write time, refreshed every 5 seconds
mongotop 5
```

**Interview Questions:**
- When would you reach for `mongostat` versus a full monitoring dashboard? — `mongostat` is ideal for a quick, live terminal-based health check during an active incident or ad hoc troubleshooting session, while a full dashboard (Atlas, Grafana) is better for historical trend analysis, alerting, and long-term capacity planning.
- How does `mongotop` help you identify a hot collection during an incident? — `mongotop` shows the time each collection spends on reads versus writes per interval, making it immediately obvious which collection is consuming a disproportionate share of server activity during a performance incident.

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
- Why does GridFS exist and when should you use it instead of storing raw binary data in a document? — GridFS exists because a single BSON document is capped at 16MB, so files larger than that (or files you want to stream/range-read) cannot be stored as a plain `Binary` field; GridFS splits them into chunks to work around that limit while still replicating and sharding alongside the rest of the data.
- What are the alternatives to GridFS for storing large files and when would you choose them? — A dedicated object store like Amazon S3 or Google Cloud Storage is often preferable for large files, offering cheaper storage, built-in CDN integration, and simpler operations; GridFS is preferable when you want files to live alongside their metadata in the same database and replicate/shard as one system.

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
- What is the default chunk size in GridFS and can it be changed? — The default chunk size is 255KB, and it can be changed by specifying a custom `chunkSizeBytes` option when opening an upload stream via the GridFS bucket API.
- What information is stored in `fs.files` versus `fs.chunks`? — `fs.files` stores one metadata document per file (filename, length, chunkSize, uploadDate, custom metadata), while `fs.chunks` stores the actual binary chunk data, each chunk referencing its parent file's `_id` and its sequence number.

### Buckets

A GridFS "bucket" is simply a named pair of collections (`<bucketName>.files` and `<bucketName>.chunks`) used to organize stored files. The default bucket name is `fs`, but applications can define multiple buckets (e.g. `images`, `videos`) to logically separate different kinds of file storage within the same database.

```javascript
// Create/use a custom bucket named "images"
const imagesBucket = new mongodb.GridFSBucket(db, { bucketName: 'images' })
```

**Interview Questions:**
- Why might an application use multiple GridFS buckets instead of the default one? — Separate buckets (e.g. `images`, `videos`) logically organize different kinds of file storage, allow different indexing or lifecycle policies per bucket, and keep unrelated file types from mixing within the same `files`/`chunks` collections.
- What indexes should exist on the `files` and `chunks` collections of a bucket for good performance? — The `chunks` collection should have a compound unique index on `{ files_id: 1, n: 1 }` for efficient ordered chunk retrieval, and the `files` collection typically has an index on `{ filename: 1, uploadDate: 1 }` to support lookups by filename.

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
- How does GridFS support streaming a large video file without loading it entirely into memory? — GridFS download streams read and emit chunks incrementally as they're fetched from `fs.chunks`, and combined with range options (`start`/`end`), a client can request and stream only the needed byte range without ever holding the entire file in memory at once.
- What happens if a chunk in the middle of a file is missing or corrupted during retrieval? — The download stream will fail with an error once it reaches the missing or corrupted chunk, since GridFS relies on all chunks being present and in sequence to reassemble the complete file correctly.

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
- What underlying mechanism do change streams use internally? — Change streams are built on top of the replication oplog, tailing it internally but exposing a stable, versioned public API rather than requiring applications to parse raw oplog entries directly.
- Why are change streams not available on a standalone `mongod` instance? — Change streams depend on the oplog, which only exists on replica sets (and by extension sharded clusters); a standalone server has no oplog since it doesn't replicate, so there's nothing for change streams to tail.
- How would you use a change stream to keep a search index in sync with MongoDB? — Open a change stream on the relevant collection(s), and for each insert/update/delete event received, apply the corresponding change to the search index (e.g. Elasticsearch) asynchronously, using resume tokens to ensure no events are missed across restarts.

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
- What fields are typically present in a change stream event document? — A change event typically includes `_id` (the resume token), `operationType`, `ns` (namespace), `documentKey`, `fullDocument` (for inserts/replaces or when configured), and `updateDescription` (for updates, listing changed and removed fields).
- How would you filter a change stream to only receive events for a specific field update? — Pass a `$match` stage to `watch()` that filters on `operationType` and inspects `updateDescription.updatedFields` to only pass through events where the specific field of interest was modified.

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
- What is a resume token used for and how does it enable fault-tolerant change stream consumers? — A resume token marks a change stream event's exact position in the underlying oplog; by persisting the last processed token, a consumer can reconnect after a crash or network drop and resume exactly where it left off, avoiding missed or duplicated events.
- What is the difference between `resumeAfter` and `startAfter`? — `resumeAfter` resumes strictly after a given token and can fail if that token refers to an invalidating event, while `startAfter` can also resume after an invalidate event (such as a collection drop/rename), making it more resilient for certain reconnection scenarios.
- What happens if the resume token refers to an oplog entry that has already been rolled off? — The change stream resume attempt fails with an error indicating the resume point is no longer available, since the corresponding oplog history has been overwritten; the consumer must then start a new change stream from the current time, potentially missing intervening events.

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
- How would you design a real-time notification system on top of MongoDB change streams? — Open a change stream (filtered to relevant operations) on the collections of interest, transform each change event into a notification payload, and publish it to connected clients via WebSockets or Server-Sent Events, persisting resume tokens so the pipeline can recover from restarts without losing events.
- What are the trade-offs of using change streams versus a dedicated message broker (Kafka, RabbitMQ) for event-driven architectures? — Change streams avoid the operational overhead of running a separate messaging system and stay tightly consistent with the database, but lack advanced broker features like topic partitioning, long-term message retention, and cross-service pub/sub fan-out that a dedicated broker like Kafka provides.

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
- How does the Bucket Pattern improve performance for time-series workloads? — By grouping many time-series readings for a period (e.g. an hour) into a single document with an array field, the Bucket Pattern reduces the number of documents scanned and index entries needed, improving both read aggregation speed and write efficiency compared to one document per reading.
- What are the risks of unbounded array growth within a bucket document? — Without a cap on the number of readings per bucket, a document can grow toward the 16MB limit and suffer repeated in-place growth/relocation on disk, so buckets should have a fixed time window or maximum element count.
- How would you decide the time window size for a bucket? — Choose a window based on expected write frequency and typical query granularity, balancing enough readings per bucket to reduce document count against keeping bucket size well under the 16MB limit and matching how data is usually queried (e.g. hourly or daily rollups).

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
- What problem does the Attribute Pattern solve compared to storing each attribute as a top-level field? — It avoids needing a separate index per sparse/optional field by moving variable attributes into a key/value array, letting a single compound index serve queries across many different attribute types.
- How would you write an index to efficiently support the Attribute Pattern? — Create a compound index on the key and value sub-fields, e.g. `{ "attributes.k": 1, "attributes.v": 1 }`, so queries filtering on any attribute name/value pair can use the same index.

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
- When would you apply the Subset Pattern instead of embedding a full array? — When the full related array (e.g. all reviews) is large and rarely needed in its entirety, so embedding only the most relevant subset keeps the primary document small while the full data lives in a separate collection queried on demand.
- How does the Subset Pattern help with working-set size and cache efficiency? — By keeping frequently accessed documents small, more of the working set fits in the WiredTiger cache, reducing disk reads and improving overall read performance for the common access pattern.

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
- How does the Extended Reference Pattern trade off consistency for read performance? — It duplicates a few frequently needed fields from a referenced document into the referencing document, eliminating an extra lookup on the hot read path at the cost of the duplicated copy potentially becoming stale until it's synchronized.
- What strategy would you use to keep denormalized extended reference fields up to date? — Update the denormalized copies synchronously in the same write operation when the source changes, or use an asynchronous background job/change stream listener to propagate updates, accepting eventual consistency for rarely changing fields.

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
- When is it worth the added write complexity to precompute a value versus calculating it on read? — When the value is read far more often than it changes, or the calculation is expensive (e.g. aggregating across many documents), so precomputing on write amortizes the cost across many cheap reads instead of recalculating every time.
- How would you keep a computed field consistent if updates can fail partway through? — Use atomic single-document update operators like `$inc` where possible, or wrap the computation and update in a multi-document transaction, and consider a periodic reconciliation job to detect and correct drift.

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
- What problem does the Outlier Pattern solve that the Subset Pattern doesn't fully address? — It handles the rare extreme case (e.g. a celebrity account) where even a small embedded subset would still make that specific document abnormally large, by flagging and offloading the overflow data for just those outlier documents while most documents remain simple.
- How would you detect which documents need to be treated as outliers in your schema? — Track a count or size threshold (e.g. follower count, array length) and set a boolean flag such as `hasOutlierFollowers` once it's exceeded, then route overflow data to a secondary collection only for flagged documents.

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
- How would you migrate a large production collection to a new schema without downtime? — Add a `schemaVersion` field, deploy application code that can read both old and new shapes, then gradually migrate documents in the background (e.g. on read-and-resave or via a batch job) rather than running one large blocking migration.
- Where should version-aware transformation logic live in a layered application (e.g. Spring service vs. repository)? — It belongs in a dedicated mapping/conversion layer (such as a custom converter or service-level adapter) rather than scattered across repositories, so version-handling logic is centralized and easy to remove once migration completes.

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
- What triggers a replica set election and how long does failover typically take? — An election is triggered when secondaries stop receiving heartbeats from the primary within the election timeout (default 10 seconds); failover including detecting the failure and electing a new primary typically completes within a few seconds to around 12 seconds.
- What is the role of an arbiter in a replica set and when would you use one? — An arbiter is a voting-only member that participates in elections but holds no data, used to maintain an odd number of voters (avoiding split votes) when adding a full data-bearing member isn't justified by cost or hardware.
- How does a driver detect that a primary has changed and redirect writes? — The MongoDB driver continuously monitors the replica set topology via `isMaster`/`hello` commands and server description updates, and automatically retries and redirects writes to the newly elected primary once it's discovered.

### Heartbeats

Replica set members send heartbeat pings to each other roughly every 2 seconds to monitor availability. If a member doesn't respond within the configured `electionTimeoutMillis` (default 10 seconds), the other members consider it down and may trigger an election if the unreachable member was the primary.

**Interview Questions:**
- What is the default heartbeat interval and election timeout in a MongoDB replica set? — By default, members send heartbeats roughly every 2 seconds, and `electionTimeoutMillis` defaults to 10,000ms (10 seconds) before an unresponsive primary is considered down.
- How would you tune heartbeat/election timeouts for a cross-region replica set with higher network latency? — Increase `electionTimeoutMillis` (and possibly the heartbeat interval) to accommodate higher round-trip latency between regions, reducing the chance of unnecessary elections caused by transient network delay rather than an actual failure.

### Elections

Replica set elections use a Raft-inspired consensus protocol to select a new primary from the eligible secondaries. Members vote based on factors including data freshness (highest priority to the member with the most recent oplog entries), configured member `priority`, and network connectivity.

```javascript
// Configure member priority to influence election outcomes
cfg = rs.conf()
cfg.members[0].priority = 2   // prefer this member as primary
rs.reconfig(cfg)
```

**Interview Questions:**
- What factors determine which secondary is elected as the new primary? — Election outcome is determined primarily by which member has the most up-to-date oplog (most recent data), then by configured member `priority`, and by overall network connectivity/reachability to a majority of voting members.
- How can you configure a replica set member to never become primary? — Set that member's `priority` to 0 in the replica set configuration (`rs.reconfig()`), which allows it to vote and hold data/serve reads but excludes it from ever being elected primary.
- What is a rollback and when can it occur after an election? — A rollback occurs when a former primary rejoins the replica set after an election and discovers it had accepted writes that were never replicated to the new primary before failover; those un-replicated writes are reverted (rolled back) to keep the data set consistent.

### Disaster Recovery Concepts

Disaster recovery for MongoDB combines replica sets (for automatic failover within/across data centers), regular backups (logical `mongodump`/`mongorestore` or filesystem/volume snapshots), and point-in-time recovery via oplog replay. Multi-region replica set deployments protect against full data-center outages, while backups protect against logical corruption or accidental deletes that replication would otherwise faithfully propagate.

**Advantages:**
- Replica sets handle hardware/node failures automatically
- Snapshot + oplog backups enable point-in-time recovery from logical errors

**Disadvantages:**
- Replication alone does not protect against accidental deletes or application bugs that corrupt data (they replicate too)
- Cross-region replicas add write latency due to majority write concerns

**Interview Questions:**
- Why is replication alone not sufficient as a disaster recovery strategy? — Replication faithfully copies every write, including accidental deletes, data corruption, or application bugs, to all secondaries, so it protects against hardware failure but not against logical errors that need to be recovered from a point-in-time backup.
- How would you design a backup strategy that supports point-in-time recovery? — Combine periodic full snapshots (filesystem/volume snapshots or `mongodump`) with continuous oplog capture, so you can restore the last snapshot and then replay oplog entries up to the exact moment before the incident occurred.
- What is the difference between a logical backup and a filesystem snapshot backup in MongoDB? — A logical backup (`mongodump`) exports data as BSON documents independent of storage engine internals and is more portable but slower to restore, while a filesystem/volume snapshot captures the raw on-disk data files nearly instantaneously but is tied to the same storage engine and typically requires the mongod to be stopped or fsync-locked for consistency.

## Best Practices

### Schema Design Best Practices

MongoDB schema design should be driven by application query patterns rather than pure normalization — "data that is accessed together should be stored together." This means starting from the application's most frequent and performance-critical queries, then modeling documents to satisfy them with minimal lookups.

**Interview Questions:**
- What does "design for your queries, not your data" mean in MongoDB schema design? — It means modeling documents around how the application will read and write data (which fields are queried together, sorted, or updated) rather than pursuing normalization for its own sake, so the schema minimizes lookups and matches real access patterns.
- How would you approach schema design differently for MongoDB versus a relational database? — In a relational database you normalize first and optimize queries later with joins, whereas in MongoDB you start from the application's query patterns and choose embedding or referencing per relationship to satisfy those queries efficiently, accepting some denormalization.

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
- What criteria would you use to decide between embedding and referencing for a one-to-many relationship? — Consider whether the related data is always accessed together with the parent (favoring embedding), how large and unbounded the related collection could grow, whether it needs to be updated/queried independently, and whether it's shared across multiple parents (favoring referencing).
- How does the 16MB document size limit influence this decision? — If embedding an unbounded or fast-growing array risks approaching the 16MB BSON document limit, referencing (or patterns like Subset/Bucket) should be used instead to keep documents bounded in size.
- How would you model a many-to-many relationship (e.g. students and courses) in MongoDB? — Typically use referencing with an array of IDs on one or both sides (e.g. a `courseIds` array on the student document), or a separate join/linking collection when the relationship itself carries additional attributes (like enrollment date).

### Index Design

Effective index design follows the ESR (Equality, Sort, Range) rule for compound indexes: place equality-matched fields first, sort fields second, and range-filtered fields last. Every index should be justified by an actual query pattern — unused indexes only add write overhead and storage cost.

```javascript
// Query: find active orders for a customer, sorted by date, within a price range
db.orders.find({ customerId: 'c1', status: 'active', total: { $gte: 100 } }).sort({ orderDate: -1 })

// ESR-compliant compound index: Equality(customerId,status), Sort(orderDate), Range(total)
db.orders.createIndex({ customerId: 1, status: 1, orderDate: -1, total: 1 })
```

**Interview Questions:**
- What is the ESR rule and how does it guide compound index field ordering? — ESR stands for Equality, Sort, Range: compound index fields should be ordered with equality-matched fields first, sort fields second, and range-filtered fields last, so the index can narrow results with equality, satisfy the sort order, then scan the range efficiently.
- How would you identify unused or redundant indexes in a production collection? — Use `$indexStats` (or the Atlas Performance Advisor) to see index usage counters over time, and flag indexes with zero or near-zero usage, or indexes that are a strict prefix subset of another compound index, as candidates for removal.
- What is index intersection and why shouldn't you rely on it instead of proper compound indexes? — Index intersection lets MongoDB combine two separate single-field indexes to satisfy a query, but it's generally less efficient than a single well-ordered compound index and isn't used for sorting, so compound indexes matching the actual query pattern should be preferred.

### Collection Design

Collection design decisions include when to split data into multiple collections versus a single collection with a discriminator field, how to handle time-series data (using time-series collections since MongoDB 5.0), and how to avoid unbounded collection growth patterns that hurt performance.

**Interview Questions:**
- When would you use MongoDB's native time-series collections instead of a manually bucketed schema? — Use native time-series collections (available since MongoDB 5.0) when you want MongoDB to automatically handle bucketing, compression, and time-based indexing for you, avoiding the manual complexity of designing and maintaining your own Bucket Pattern schema.
- What are the trade-offs of using a single collection with a `type` discriminator field versus multiple collections? — A single collection with a discriminator simplifies polymorphic queries and shared indexes but can mix unrelated document shapes and make schema validation/indexing less targeted, while multiple collections give cleaner separation and indexing at the cost of needing application-level joins for cross-type queries.

### Shard Key Selection

The shard key determines how data is distributed across shards in a sharded cluster, and it cannot be changed (prior to resharding support) once chosen, making it one of the most consequential early decisions. A good shard key has high cardinality, even distribution of writes, and aligns with common query patterns to avoid scatter-gather queries.

**Advantages of a good shard key:**
- Even data and write distribution across shards
- Queries can be targeted to specific shards instead of broadcasting to all

**Disadvantages of a poor shard key:**
- Hotspots on a single shard (monotonically increasing keys like timestamps or ObjectIds)
- Scatter-gather queries across all shards, hurting performance

**Interview Questions:**
- What makes a good versus a bad shard key choice? — A good shard key has high cardinality, distributes writes evenly across shards, and aligns with common query patterns so queries can target specific shards; a bad shard key has low cardinality, creates write hotspots, or forces scatter-gather queries across all shards.
- Why is a monotonically increasing field like a timestamp often a poor shard key on its own? — Because all new writes have ever-increasing values, they all land on the same shard (the one owning the current highest range), creating a write hotspot instead of distributing load evenly across the cluster.
- What is resharding and when would you need it? — Resharding is the process (supported since MongoDB 5.0) of changing a collection's shard key after the fact; it's needed when the original shard key causes hotspots or uneven distribution and the data must be redistributed without a full manual migration.

### Performance Optimization

Performance tuning in MongoDB revolves around ensuring queries use appropriate indexes (verified with `explain()`), keeping the working set within available RAM/WiredTiger cache, using projections to limit returned fields, and using the aggregation pipeline efficiently (filtering early with `$match` before expensive stages).

```javascript
// Analyze query execution to check for collection scans (COLLSCAN)
db.orders.find({ status: 'active' }).explain('executionStats')
```

**Interview Questions:**
- How would you use `explain()` to diagnose a slow query? — Run the query with `.explain('executionStats')` to inspect whether it used an index scan (`IXSCAN`) or a full collection scan (`COLLSCAN`), how many documents were examined versus returned, and total execution time, then adjust indexes accordingly.
- What is the significance of placing `$match` early in an aggregation pipeline? — Placing `$match` as early as possible lets MongoDB use indexes to filter out unneeded documents before later, more expensive stages (like `$group` or `$lookup`) run, drastically reducing the amount of data processed downstream.
- How does working-set size relative to available memory affect performance? — If the frequently accessed data and indexes (the working set) fit within the WiredTiger cache/RAM, reads are served from memory; once the working set exceeds available memory, MongoDB must fetch pages from disk more often, significantly increasing latency.

### Security Best Practices

MongoDB production security best practices include enabling authentication and role-based access control (RBAC), enabling TLS/SSL for data in transit, encrypting data at rest, restricting network access via firewalls/VPC peering and binding to specific IPs, enabling auditing, and following the principle of least privilege for database users/roles.

**Interview Questions:**
- What steps would you take to secure a MongoDB deployment before going to production? — Enable authentication and RBAC, enforce TLS/SSL for all connections, enable encryption at rest, restrict network access via firewalls/VPC peering and IP binding, enable auditing, and apply least-privilege roles to all database users.
- What is the principle of least privilege and how does it apply to MongoDB user roles? — It means granting each user or application only the minimum permissions needed for its function (e.g. read-only or restricted to specific collections) rather than broad admin roles, limiting the damage a compromised credential can cause.
- How does field-level or client-side field level encryption differ from encryption at rest? — Encryption at rest protects the entire data files on disk transparently to the application, while field-level/client-side field level encryption (CSFLE) encrypts specific sensitive fields before they leave the client, so even database administrators or anyone with disk/backup access cannot read those fields without the client-held keys.

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
- How does Spring Data MongoDB convert between a Java object and a BSON document? — It uses `MappingMongoConverter`, which inspects the entity's fields and type metadata (via reflection and the mapping context) to serialize Java properties into BSON fields on write and reconstruct Java objects from BSON documents on read.
- What is `MappingMongoConverter` and when would you register a custom converter? — `MappingMongoConverter` is the default converter Spring Data MongoDB uses for object-document mapping; you'd register a custom `Converter<S, T>` bean when you need special handling for a type the default converter can't map correctly, such as a third-party class or a custom value representation.

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
- What is the purpose of the `@Document` annotation and what happens if you omit the collection name? — `@Document` marks a class as mapped to a MongoDB collection; if you omit the `collection` attribute, Spring Data MongoDB derives the collection name automatically from the (decapitalized) class name.
- How would you map a Java field name to a differently named document field? — Annotate the field with `@Field("document_field_name")` to override the default (which otherwise uses the Java field name as-is) with a custom BSON field name.

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
- How does Spring Data MongoDB decide whether to embed a nested object versus store a reference? — By default, any plain nested POJO field is embedded as a sub-document; it's only stored as a reference if explicitly annotated with `@DBRef`, so embedding is the default behavior and referencing is opt-in.
- What happens when you query using a property path into a nested document (e.g. `address.city`)? — Spring Data MongoDB translates the dotted property path into a MongoDB dot-notation query filter (e.g. `{"address.city": "NYC"}`), which can use an index created on that nested field path.

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
- How would you map a Java `Map` field to a MongoDB document and what are the query implications? — Spring Data MongoDB maps a `Map<String, T>` field directly to a BSON sub-document keyed by the map keys; querying specific entries requires dot notation (e.g. `"attributes.color"`), and dynamic/unknown keys make it harder to build simple indexes compared to a fixed-schema field.
- What are the trade-offs of storing a `List<NestedObject>` versus a separate referenced collection? — An embedded list is read atomically with the parent and avoids extra queries but grows the parent document and can hit size/performance limits at scale, while a separate collection keeps the parent small and allows independent querying/pagination at the cost of an extra lookup.

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
- Why is `@DBRef` generally discouraged in favor of manually storing an ID and querying explicitly? — `@DBRef` incurs an extra query per reference (or per document when eagerly resolving collections, causing N+1 query problems), lacks fine-grained control over fetching, and doesn't support projections, whereas manually storing an ID field and querying the referenced repository directly is more explicit, efficient, and controllable.
- What are the performance implications of lazy `@DBRef` resolution? — Lazy resolution defers the extra query until the referenced field is actually accessed via a proxy, which avoids unnecessary fetches but can trigger a query outside the expected transactional/session context, and still results in one query per reference when accessed, risking N+1 behavior for collections.

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
- How does Spring Data MongoDB convert between `ObjectId` and a Java `String` `@Id` field? — The `MappingMongoConverter` automatically converts a MongoDB `ObjectId` to its 24-character hex string representation when reading into a `String`-typed `@Id` field, and converts it back to an `ObjectId` when writing, as long as the string is valid ObjectId hex.
- What happens if you assign a non-ObjectId-format string to an `@Id` field? — Spring Data MongoDB stores the value as a plain `String` (not converted to `ObjectId`) since it isn't valid 24-character hex, which is how custom/business-key string IDs are supported.

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
- How would you implement an auto-incrementing numeric ID in MongoDB, given it has no native sequence support? — Maintain a separate "counters" collection with one document per sequence name, and use `findAndModify` with `$inc` to atomically increment and retrieve the next value each time a new ID is needed.
- What are the trade-offs of using a business key as `_id` versus a generated `ObjectId`? — A business key avoids a lookup/join to translate between identifiers and can be more meaningful, but risks needing to change if the business key changes (which is difficult since `_id` is immutable), while a generated `ObjectId` is guaranteed unique and stable but meaningless outside the database.

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
- How does `@Version`-based optimistic locking work in Spring Data MongoDB? — A field annotated `@Version` is checked on every update: Spring Data MongoDB includes the current version value in the update's query filter and increments it, so the update only succeeds if no other process has modified (and incremented) the document since it was read.
- What exception is thrown on a version conflict and how should the application handle it? — An `OptimisticLockingFailureException` is thrown when the version check fails; the application should typically catch it, reload the latest document, reapply the intended change, and retry the save.
- How does optimistic locking compare to using MongoDB transactions for concurrency control? — Optimistic locking is lightweight and lock-free, only protecting a single document via version checks, whereas multi-document transactions provide full ACID guarantees across multiple documents/collections at the cost of higher latency and resource overhead from holding locks during the transaction.

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
- What annotation enables automatic auditing support in a Spring Boot application? — `@EnableMongoAuditing` on a `@Configuration` class enables Spring Data MongoDB's auditing infrastructure, allowing `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, and `@LastModifiedBy` fields to be populated automatically.
- How would you populate `@CreatedBy`/`@LastModifiedBy` with the current authenticated user? — Provide a bean implementing `AuditorAware<T>` whose `getCurrentAuditor()` method returns the current user (e.g. from Spring Security's `SecurityContextHolder`), which Spring Data MongoDB uses automatically to populate those fields.

### Lazy vs Eager References (Concept)

When using `@DBRef(lazy = true)`, Spring Data MongoDB returns a proxy object for the referenced entity and only issues the query to fetch it when a property is first accessed, deferring the cost. Eager (default) `@DBRef` resolves the reference immediately when the parent document is loaded.

**Differences:**

| Aspect | Eager `@DBRef` (default) | Lazy `@DBRef(lazy = true)` |
|---|---|---|
| Fetch timing | Immediately with parent | On first access via proxy |
| Extra queries | Always incurred | Only if referenced object is actually used |
| Risk | N+1 queries when loading collections of parents | `LazyInitializationException`-style issues outside a session/proxy context |

**Interview Questions:**
- What is the difference between lazy and eager `@DBRef` resolution? — Eager (default) `@DBRef` fetches the referenced document immediately when the parent is loaded, while `@DBRef(lazy = true)` returns a proxy that only triggers the fetch query when a property on the referenced object is first accessed.
- Why is `@DBRef` (lazy or eager) generally less recommended than manual reference resolution in Spring Data MongoDB? — Both variants incur per-reference queries that can cause N+1 query problems when loading collections of parents, offer no projection/batching control, and lazy proxies can behave unexpectedly outside their originating context, so manually storing an ID and querying the target repository explicitly (optionally batched) is usually more predictable and efficient.

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
- How does Spring Data MongoDB generate query implementations from repository method names? — It parses the method name at startup according to Spring Data's keyword conventions (e.g. `findBy`, `And`, `OrderBy`), builds a corresponding `Criteria`/`Query` object matching those keywords to the entity's fields, and generates a proxy implementation that executes that query at runtime.
- When would you use `@Query` instead of a derived query method? — Use `@Query` when the desired query is too complex to express via method-name derivation (e.g. nested operators, complex `$or`/`$and` combinations, or projections) or when a raw MongoDB query string is clearer and easier to maintain than a long derived method name.

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
- When would you choose `MongoTemplate` over a `MongoRepository` interface? — Choose `MongoTemplate` when you need to build queries dynamically at runtime (unknown combinations of filters), perform bulk operations, run aggregations, or need finer control than derived/`@Query` methods can express.
- How would you build a dynamic query with an unknown number of optional filter criteria using `MongoTemplate`? — Build a `Criteria` list conditionally, adding a criterion only if the corresponding filter parameter is present, then combine them with `new Criteria().andOperator(...)` (or start from an empty `Query` and call `.addCriteria()` for each present filter) before passing the resulting `Query` to `mongoTemplate.find()`.

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
- How do you translate a raw MongoDB aggregation pipeline into Spring Data MongoDB's `Aggregation` builder API? — Map each pipeline stage to its corresponding static builder method (e.g. `$match` → `Aggregation.match()`, `$group` → `Aggregation.group()`, `$sort` → `Aggregation.sort()`) and chain them in order via `Aggregation.newAggregation(...)`, then execute with `mongoTemplate.aggregate()`.
- How would you perform the equivalent of a SQL join using Spring Data MongoDB's aggregation support? — Use `Aggregation.lookup()` to perform a `$lookup` stage joining another collection on a local/foreign field, typically followed by `Aggregation.unwind()` to flatten the resulting array into individual joined documents.

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
- What MongoDB deployment topology is required to use multi-document transactions? — Multi-document transactions require a replica set (available since MongoDB 4.0) or a sharded cluster (since MongoDB 4.2); standalone `mongod` instances do not support transactions.
- How do you configure `@Transactional` support for MongoDB in a Spring Boot application? — Define a `MongoTransactionManager` bean wired to your `MongoDatabaseFactory`, after which standard Spring `@Transactional` annotations on service methods will participate in MongoDB multi-document transactions.
- When should you prefer redesigning a schema to avoid needing a transaction versus actually using one? — Prefer redesigning the schema (e.g. embedding related data so updates are atomic within a single document) when the same logical operation is performed frequently and at scale, since single-document atomic updates are far cheaper than transactions; reserve transactions for genuinely cross-document operations that can't reasonably be modeled that way.

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
- What are the key differences between `MongoRepository` and `ReactiveMongoRepository`? — `MongoRepository` is blocking and returns standard types like `List<T>` and `Optional<T>` using one thread per request, while `ReactiveMongoRepository` is non-blocking, returns `Mono<T>`/`Flux<T>` backed by Project Reactor and the reactive streams driver, and is designed for event-loop-based frameworks like WebFlux.
- When would reactive MongoDB support provide a real throughput benefit versus the blocking API? — It helps most under high concurrency with I/O-bound workloads where many requests are waiting on the database simultaneously, since non-blocking I/O lets a small number of threads handle many concurrent requests without being tied up waiting; it offers little benefit for low-concurrency or CPU-bound workloads.
- How do you handle multi-document transactions in a reactive Spring Data MongoDB application? — Use `ReactiveMongoTransactionManager` combined with Spring's reactive transaction support (`@Transactional` on reactive return types, or `TransactionalOperator`) to wrap a sequence of reactive repository/template calls in a MongoDB transaction, which still requires a replica set or sharded cluster.

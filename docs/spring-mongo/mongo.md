# MongoDB

## MongoDB Fundamentals

- What is MongoDB?
- NoSQL Databases
- Document-Oriented Database
- BSON vs JSON
- MongoDB Architecture
- MongoDB Use Cases
- MongoDB vs Relational Databases
- MongoDB Editions (Community vs Enterprise)

## MongoDB Installation and Tools

- MongoDB Server
- MongoDB Shell (mongosh)
- MongoDB Compass
- MongoDB Atlas (Overview)
- Configuration Basics
- MongoDB Drivers
- MongoDB Atlas Search (Overview)

## Database Structure

- Databases
- Collections
- Documents
- Fields
- Embedded Documents
- Arrays
- Dynamic Schema

## BSON Data Types

- String
- Number Types
- Boolean
- Date
- ObjectId
- Array
- Embedded Document
- Null
- Binary Data
- Timestamp
- Decimal128
- UUID
- MinKey and MaxKey
- Regular Expression

## ObjectId

- Structure of ObjectId
- Automatic ID Generation
- Custom IDs
- ObjectId Advantages

## CRUD Concepts

- Insert Operations
- Read Operations
- Update Operations
- Delete Operations
- Bulk Operations
- Upsert
- Replace Operations

## Data Modeling

- Embedding Documents
- Referencing Documents
- One-to-One Relationships
- One-to-Many Relationships
- Many-to-Many Relationships
- Denormalization
- Schema Design Principles
- Schema Validation
- Polymorphic Documents

## Collections

- Capped Collections
- Time Series Collections
- Collection Validation
- Collection Options
- Views
- Clustered Collections

## Indexing

- Index Fundamentals
- Single Field Index
- Compound Index
- Multikey Index
- Text Index
- Geospatial Index
- Hashed Index
- TTL Index
- Unique Index
- Sparse Index
- Partial Index
- Covered Queries
- Index Selection
- Index Best Practices
- Wildcard Index

## Query Processing

- Query Execution
- Query Planner
- Execution Plans
- Explain Plans
- Query Optimization
- Projection
- Pagination
- Sorting
- Cursors

## Aggregation Framework

- Aggregation Pipeline
- Pipeline Stages
- Match
- Project
- Group
- Sort
- Limit
- Skip
- Unwind
- Lookup
- Facet
- Bucket
- Merge
- Out
- Aggregation Pipeline Optimization
- Window Functions

## Transactions

- Single Document Atomicity
- Multi-Document Transactions
- ACID Properties
- Transaction Lifecycle
- Transaction Limitations
- Retryable Writes

## Concurrency

- Document-Level Locking
- Optimistic Concurrency Concepts
- Atomic Operations
- Write Conflicts

## Replication

- Replica Sets
- Primary
- Secondary
- Automatic Failover
- Elections
- Read Preference
- Write Concern
- Read Concern
- Oplog
- Arbiter Nodes

## Sharding

- Sharding Concepts
- Shard Key
- Config Servers
- Mongos Router
- Chunk Migration
- Balancer
- Choosing a Shard Key
- Zone Sharding

## Consistency and Availability

- CAP Theorem
- Eventual Consistency
- Read Preference
- Read Concern
- Write Concern
- Causal Consistency

## Storage Engine

- WiredTiger
- Compression
- Journaling
- Checkpoints
- Cache Management

## Schema Validation

- JSON Schema Validation
- Validation Rules
- Validation Levels
- Validation Actions

## Security

- Authentication
- Authorization
- Users and Roles
- Role-Based Access Control (RBAC)
- TLS/SSL
- Encryption at Rest
- Auditing (Overview)
- Client-Side Field Level Encryption

## Backup and Recovery

- mongodump
- mongorestore
- Point-in-Time Recovery (Overview)
- Backup Strategies
- Disaster Recovery

## Performance Tuning

- Query Optimization
- Index Optimization
- Connection Pooling
- Bulk Writes
- Batch Processing
- Profiling
- Slow Query Analysis
- Memory Considerations

## Monitoring

- MongoDB Profiler
- Server Status
- Database Statistics
- Collection Statistics
- Index Statistics
- Performance Metrics
- Mongostat and Mongotop

## GridFS

- GridFS Fundamentals
- File Storage
- Buckets
- File Retrieval

## Change Streams

- Change Streams
- Event Notifications
- Resume Tokens
- Real-Time Data Processing

## MongoDB Design Patterns

- Bucket Pattern
- Attribute Pattern
- Subset Pattern
- Extended Reference Pattern
- Computed Pattern
- Outlier Pattern
- Schema Versioning Pattern

## High Availability

- Replica Set Failover
- Heartbeats
- Elections
- Disaster Recovery Concepts

## Best Practices

- Schema Design Best Practices
- Embedding vs Referencing
- Index Design
- Collection Design
- Shard Key Selection
- Performance Optimization
- Security Best Practices

## Concepts for Spring Data MongoDB

- Object-Document Mapping (ODM)
- Entity vs Document
- Mapping Nested Documents
- Mapping Collections
- Embedded Documents vs References
- ObjectId Mapping
- Custom ID Strategies
- Optimistic Locking Concepts
- Auditing Concepts
- Lazy vs Eager References (Concept)
- Repository Pattern (Concept)
- MongoTemplate (Concept)
- Aggregation Pipeline Concepts
- Transactions with Spring Data MongoDB
- Reactive MongoDB Support
# Redis


## Redis Fundamentals

- What is Redis?
- In-Memory Data Store
- Redis Architecture
- Redis Use Cases
- Redis vs Traditional Databases
- Redis vs Memcached
- Redis Persistence Overview
- RESP Protocol (Redis Serialization Protocol)

## Redis Installation and Configuration

- Redis Server
- Redis CLI
- Configuration File (redis.conf)
- Basic Configuration Options
- Memory Configuration
- Security Configuration
- Running Redis with Docker
- Client Connection Configuration

## Redis Data Types

- Strings
- Hashes
- Lists
- Sets
- Sorted Sets (ZSets)
- Bitmaps
- HyperLogLog
- Streams
- Geospatial Data

## Redis Modules and Extensions

- RedisJSON
- RediSearch (Full-Text and Vector Search)
- RedisBloom (Probabilistic Data Structures)
- RedisTimeSeries
- Redis Stack Overview

## Key Management

- Keys
- Namespaces
- Key Naming Conventions
- Key Expiration (TTL)
- Persistent Keys
- Key Eviction
- Key Scanning
- Key Deletion
- Key Renaming

## Redis Persistence

- RDB Snapshots
- AOF (Append Only File)
- Hybrid Persistence
- Persistence Trade-offs
- Backup and Restore
- AOF Rewrite (BGREWRITEAOF)

## Memory Management

- Memory Allocation
- Memory Usage
- Eviction Policies
- LRU
- LFU
- Volatile vs AllKeys Policies
- Memory Fragmentation

## Transactions

- Transaction Basics
- MULTI
- EXEC
- DISCARD
- WATCH
- Optimistic Locking
- Transaction Limitations

## Concurrency and Atomicity

- Single-Threaded Execution Model
- Atomic Operations
- Race Conditions
- Distributed Locks (Concept)
- Redlock (Overview)

## Pub/Sub Messaging

- Publish
- Subscribe
- Pattern Subscriptions
- Pub/Sub Limitations
- Common Use Cases
- Keyspace Notifications

## Redis Streams

- Streams Basics
- Producers
- Consumers
- Consumer Groups
- Message Acknowledgement
- Pending Entries
- Stream Trimming

## Caching Concepts

- Cache Aside Pattern
- Read Through Cache
- Write Through Cache
- Write Behind Cache
- Refresh Ahead Cache
- Cache Warming
- Cache Invalidation
- Cache Stampede
- Cache Penetration
- Cache Avalanche
- Hot Keys
- Client-Side Caching

## Expiration and Eviction

- TTL
- Expire
- Persist
- Passive Expiration
- Active Expiration
- Eviction Strategies

## Replication and High Availability

- Master-Replica Replication
- Replication Process
- Read Replicas
- Failover Concepts
- Partial Resynchronization (PSYNC)

## Redis Sentinel

- Sentinel Architecture
- Automatic Failover
- Monitoring
- Leader Election
- Client Interaction

## Redis Cluster

- Cluster Architecture
- Hash Slots
- Data Partitioning
- Resharding
- Cluster Failover
- Multi-Key Operation Limitations

## Lua Scripting

- Lua Script Basics
- Atomic Execution
- Script Caching
- Common Use Cases
- Redis Functions (Server-Side Functions)

## Redis Security

- Authentication (AUTH)
- ACL (Access Control Lists)
- Protected Mode
- TLS Support
- Command Renaming
- Network Security Best Practices

## Monitoring and Performance

- INFO Command
- Slow Log
- Latency Monitoring
- Memory Statistics
- Performance Tuning
- Benchmarking
- Connection Management
- Metrics Export (Prometheus/Grafana Integration)

## Data Modeling

- Choosing the Right Data Type
- Modeling Relationships
- Denormalization
- Composite Keys
- Secondary Index Patterns
- Time-Series Modeling
- Leaderboard Modeling

## Common Redis Design Patterns

- Session Store
- Distributed Cache
- Rate Limiter
- Leaderboard
- Distributed Lock
- Message Queue
- Delayed Queue
- Job Queue
- Real-Time Analytics
- Notification System
- Counters and Atomic Counters

## Reliability and Recovery

- Backup Strategies
- Disaster Recovery
- Data Consistency Considerations
- Recovery from Failures

## Redis Best Practices

- Key Naming Strategy
- TTL Strategy
- Memory Optimization
- Choosing Appropriate Data Types
- Avoiding Large Keys
- Avoiding Hot Keys
- Pipelining
- Batching
- Monitoring and Alerting

## Concepts for Spring Data Redis

- Serialization vs Deserialization
- JSON vs Binary Serialization
- Object Mapping Concepts
- Connection Pooling
- RedisTemplate (Concept)
- Repository Pattern (Concept)
- Hash Mapping Concepts
- TTL for Objects
- Optimistic Locking Concepts
- Transactions with Redis
- Reactive Redis Concepts
- Lettuce vs Jedis (Client Libraries)
- Cache Abstraction with @Cacheable
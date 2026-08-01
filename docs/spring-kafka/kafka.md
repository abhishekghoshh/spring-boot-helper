# Apache Kafka Topics

## Kafka Fundamentals

- What is Apache Kafka?
- Event Streaming Platform
- Messaging System vs Event Streaming
- Kafka Architecture
- Kafka Use Cases
- Kafka vs RabbitMQ
- Kafka vs ActiveMQ
- Kafka vs Pulsar (Overview)

## Core Kafka Concepts

- Events (Records)
- Topics
- Partitions
- Offsets
- Producers
- Consumers
- Brokers
- Kafka Cluster
- Consumer Groups
- Replication
- Leader and Follower Replicas

## Kafka Architecture

- Cluster Architecture
- Broker Responsibilities
- Topic Partitioning
- Metadata
- Replication Factor
- In-Sync Replicas (ISR)
- Controller
- KRaft Architecture
- ZooKeeper (Legacy)
- KRaft Controller Quorum

## Topic Management

- Topic Creation
- Topic Configuration
- Topic Partitions
- Topic Replication
- Topic Retention
- Topic Compaction
- Internal Topics
- Topic Deletion
- Minimum In-Sync Replicas (min.insync.replicas)

## Partitions and Ordering

- Partitioning Strategy
- Message Ordering
- Partition Keys
- Round Robin Partitioning
- Custom Partitioners
- Partition Rebalancing

## Producers

- Producer Architecture
- Producer Configuration
- Producer Acknowledgements (acks)
- Batching
- Compression
- Retries
- Idempotent Producer
- Transactions
- Message Keys
- Message Headers
- Serialization
- Producer Interceptors

## Consumers

- Consumer Architecture
- Consumer Groups
- Consumer Configuration
- Polling Model
- Offset Management
- Offset Commit
- Auto Commit
- Manual Commit
- Consumer Rebalancing
- Deserialization
- Consumer Interceptors

## Message Delivery Semantics

- At Most Once
- At Least Once
- Exactly Once
- Idempotency
- Transactions
- Duplicate Messages

## Serialization

- String Serialization
- JSON Serialization
- Avro Serialization
- Protobuf Serialization
- Custom Serialization
- Schema Evolution

## Schema Management

- Schema Registry
- Schema Compatibility
- Forward Compatibility
- Backward Compatibility
- Full Compatibility

## Replication and Fault Tolerance

- Replication Factor
- Leader Election
- Preferred Leader
- Leader Failover
- Replica Synchronization
- High Availability
- Broker Failure Recovery
- Unclean Leader Election

## Storage and Retention

- Log Segments
- Log Retention
- Time-Based Retention
- Size-Based Retention
- Log Compaction
- Tombstone Records
- Disk Storage Model
- Tiered Storage

## Consumer Group Rebalancing

- Static Membership
- Dynamic Membership
- Cooperative Rebalancing
- Rebalance Triggers
- Rebalance Listeners

## Transactions

- Producer Transactions
- Transaction Coordinator
- Transaction Lifecycle
- Read Committed
- Read Uncommitted
- Exactly Once Semantics (EOS)

## Error Handling

- Retry Strategies
- Dead Letter Topics (DLT)
- Poison Messages
- Error Recovery
- Backoff Strategies

## Performance Tuning

- Throughput vs Latency
- Batch Size
- Linger Time
- Compression
- Fetch Size
- Poll Size
- Producer Buffer Memory
- Consumer Fetch Settings
- Broker Performance Tuning
- Zero-Copy Transfer

## Security

- Authentication
- Authorization
- ACLs
- SSL/TLS
- SASL
- SCRAM
- OAuth Authentication (Overview)
- Encryption in Transit

## Quotas and Multi-Tenancy

- Client Quotas
- Request Rate Quotas
- Multi-Tenancy

## Monitoring and Operations

- Kafka Metrics
- Consumer Lag
- Broker Metrics
- Topic Metrics
- Partition Metrics
- Health Checks
- Log Monitoring
- Alerting
- Broker Configuration (server.properties)

## Kafka Streams (Concepts)

- Stream Processing
- Stateless Processing
- Stateful Processing
- Stream Topology
- Windowing
- Joins
- Aggregations
- Interactive Queries (Overview)
- KStream and KTable Abstractions
- State Stores
- Exactly-Once Semantics in Kafka Streams

## Kafka Connect (Concepts)

- Source Connectors
- Sink Connectors
- Standalone Mode
- Distributed Mode
- Connector Configuration
- Offset Storage
- Single Message Transforms (SMTs)
- Kafka Connect REST API

## Event-Driven Architecture

- Event-Driven Design
- Event Producers
- Event Consumers
- Event Choreography
- Event Ordering
- Event Versioning
- Event Immutability

## Design Patterns

- Publish-Subscribe
- Event Sourcing (Overview)
- Outbox Pattern
- Request-Reply Pattern
- Competing Consumers
- Retry Pattern
- Dead Letter Queue Pattern
- Transactional Outbox
- Saga Pattern (Overview)

## Scalability

- Horizontal Scaling
- Scaling Producers
- Scaling Consumers
- Partition Scaling
- Broker Scaling

## Reliability

- High Availability
- Disaster Recovery
- Rack Awareness
- Multi-Cluster Replication (Overview)
- MirrorMaker 2 (Overview)

## Best Practices

- Topic Naming Conventions
- Partition Sizing
- Choosing Replication Factor
- Choosing Number of Partitions
- Message Size Best Practices
- Key Design
- Consumer Group Design
- Error Handling Best Practices
- Performance Best Practices

## Concepts for Spring for Apache Kafka

- Producer Factory (Concept)
- Consumer Factory (Concept)
- KafkaTemplate (Concept)
- Listener Containers (Concept)
- @KafkaListener (Concept)
- Message Converters (Concept)
- Acknowledgement Modes
- Error Handlers
- Retry Topics
- Dead Letter Topics
- Batch Listeners
- Record Listeners
- Transactions with Spring Kafka
- JSON Message Conversion
- Avro Integration (Concept)
- Embedded Kafka for Testing (Concept)
- Concurrency Configuration (@KafkaListener)
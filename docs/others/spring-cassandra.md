# Spring boot with Cassandra

> **Primer:** Cassandra is a wide-column NoSQL store built for massive write
> throughput and linear horizontal scale. Data is addressed by
> **partition key** (which node owns it) + **clustering columns** (sort order
> inside the partition), queried with CQL. Consistency is tunable per query
> (`ONE` / `QUORUM` / `ALL`). There are no JOINs — you denormalize into
> query-shaped tables.

## Real-life use case

IoT telemetry: 1M sensors emit a reading every second. Writes are append-only
and keyed by `(sensor_id, timestamp)`; dashboards read "last 24h for sensor X"
— a single-partition range scan, Cassandra's sweet spot.

## Mental model

```text
CQL: SELECT * FROM readings WHERE sensor_id = ? AND ts > ?
                                        │
                    partition key ──────┘  → token ring → ONE node owns it
                    clustering ts ───────── orders rows inside the partition

  Write path: coordinator → commitlog + memtable → SSTable (ALL replicas, hinted handoff if down)
  Read path:  QUORUM replicas → reconcile by timestamp → return newest
```

## Minimal example

```java
package com.example.iot;

import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;
import static org.springframework.data.cassandra.core.cql.PrimaryKeyType.*;
import org.springframework.data.cassandra.repository.CassandraRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table("readings")
public class Reading {
    @org.springframework.data.cassandra.core.mapping.PrimaryKey
    private ReadingKey key;
    private double value;
}

@PrimaryKeyClass
class ReadingKey {
    @PrimaryKeyColumn(name = "sensor_id", type = PARTITIONED)
    private UUID sensorId;
    @PrimaryKeyColumn(name = "ts", type = CLUSTERED)
    private Instant ts;
}

public interface ReadingRepository extends CassandraRepository<Reading, ReadingKey> {
    List<Reading> findByKeySensorIdAndKeyTsGreaterThan(UUID sensorId, Instant since);
}
```

```yaml
spring:
  cassandra:
    contact-points: cass-1:9042,cass-2:9042
    keyspace-name: iot
    schema-action: create_if_not_exists
```

## Pitfalls

- `ALLOW FILTERING` and unbounded `IN (...)` scan the whole cluster — design tables so every query hits one partition.
- Secondary indexes are for low-cardinality columns only; high-cardinality lookups need their own table.
- Lightweight transactions (`IF NOT EXISTS`) use Paxos and are ~4x slower — don't put them on the hot path.

## Interview Q&A

**Q1. Partition key vs clustering key?**
A: The partition key decides *which node* stores the row (via token hash).
Clustering columns decide *sort order within* that partition, enabling range
slices like "last 24h" without scanning other nodes.

**Q2. Why can't you JOIN in Cassandra, and what do you do instead?**
A: JOINs would require cross-node coordination at read time, killing the
single-partition read guarantee. Instead you denormalize: write the same data
into several query-shaped tables (one per access pattern).

---




## Youtube

- [SpringDeveloper](https://www.youtube.com/@SpringSourceDev/playlists)
    - [Build a Todo App on Cassandra with Spring Boot and Spring Data](https://www.youtube.com/watch?v=YutF0WgNtl4)
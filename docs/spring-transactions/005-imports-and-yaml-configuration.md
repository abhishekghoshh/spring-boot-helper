### Imports for RDBMS vs NoSQL and YAML Configuration

**For RDBMS (JPA + Hibernate):**

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Data JPA (includes Hibernate as default JPA provider) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Database Drivers — choose ONE -->
    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- H2 (in-memory for testing) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

```java
// Java imports for RDBMS Transactions
import org.springframework.transaction.annotation.Transactional;       // ← The annotation
import org.springframework.transaction.annotation.Isolation;           // ← Isolation levels
import org.springframework.transaction.annotation.Propagation;         // ← Propagation types

// These come from spring-boot-starter-data-jpa:
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.Entity;           // Jakarta EE 9+ (Spring Boot 3.x)
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
```

```yaml
# application.yml — RDBMS (MySQL example)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

# PostgreSQL example:
# spring:
#   datasource:
#     url: jdbc:postgresql://localhost:5432/mydb
#     username: postgres
#     password: secret
#     driver-class-name: org.postgresql.Driver
#   jpa:
#     properties:
#       hibernate:
#         dialect: org.hibernate.dialect.PostgreSQLDialect
```

**For NoSQL (MongoDB):**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
<!-- MongoDB driver is included transitively -->
```

```java
// Java imports for MongoDB Transactions
import org.springframework.transaction.annotation.Transactional;       // ← Same annotation!
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;       // ← MongoDB-specific TxManager
import org.springframework.data.annotation.Id;                         // ← Not jakarta.persistence!
import org.springframework.data.mongodb.core.mapping.Document;
```

```yaml
# application.yml — MongoDB
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb
      # For transactions, MongoDB requires a Replica Set:
      # uri: mongodb://localhost:27017,localhost:27018,localhost:27019/mydb?replicaSet=rs0
```

```java
// ⚠️ MongoDB requires explicit TransactionManager bean configuration
@Configuration
public class MongoConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
// Note: MongoDB transactions require a Replica Set (not standalone).
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  RDBMS vs NoSQL — Transaction Support Comparison:                                │
│                                                                                  │
│  ┌──────────────────┬──────────────────────────┬────────────────────────────┐     │
│  │                  │ RDBMS (JPA/Hibernate)    │ NoSQL (MongoDB)            │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ Starter          │ spring-boot-starter-     │ spring-boot-starter-       │     │
│  │                  │ data-jpa                 │ data-mongodb               │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ Driver           │ mysql-connector-j /      │ Included in starter        │     │
│  │                  │ postgresql               │                            │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ TxManager        │ JpaTransactionManager    │ MongoTransactionManager    │     │
│  │                  │ (auto-configured)        │ (manual bean required)     │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ @Transactional   │ Same annotation          │ Same annotation            │     │
│  │                  │ (spring-tx)              │ (spring-tx)                │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ Entity Annot.    │ @Entity, @Table          │ @Document                  │     │
│  │                  │ (jakarta.persistence)    │ (spring-data-mongodb)      │     │
│  ├──────────────────┼──────────────────────────┼────────────────────────────┤     │
│  │ Requirement      │ Any RDBMS                │ Replica Set required       │     │
│  │                  │                          │ (standalone won't work)    │     │
│  └──────────────────┴──────────────────────────┴────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---


# Configuration Guide

## Configuration Architecture

LoanSphere uses a hierarchical configuration strategy:

```
Root POM (dependency management)
    └── Service application.yml (service-specific)
        └── Environment variables (Docker/K8s override)
```

All services inherit from `spring-boot-starter-parent:4.0.7` and use the Spring Cloud 2025.1.2 BOM.

---

## Root POM Configuration

### Java & Spring Versions

```xml
<properties>
    <java.version>25</java.version>
    <spring-cloud.version>2025.1.2</spring-cloud.version>
</properties>
```

### Managed Dependencies

The root POM manages versions for:
- Spring Cloud dependencies (imported BOM)
- MapStruct (`1.6.3`)
- Nimbus JOSE JWT (`9.37.3`)
- SpringDoc OpenAPI (`2.8.6`)
- Resilience4j (`2.3.0`)
- Testcontainers (`1.20.6`)

---

## Service Configuration Reference

### Common Properties (All Services)

```yaml
spring:
  application:
    name: <service-name>            # Eureka registration name
  threads:
    virtual:
      enabled: true                  # Virtual threads enabled everywhere

server:
  port: <port>                       # Service port

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}

jwt:
  secret: ${JWT_SECRET:default-key}
  expiration-ms: ${JWT_EXPIRATION_MS:3600000}
```

### MongoDB Configuration

```yaml
spring:
  data:
    mongodb:
      host: ${MONGODB_HOST:localhost}
      port: ${MONGODB_PORT:27017}
      authentication-database: admin
      username: ${MONGODB_USERNAME:root}
      password: ${MONGODB_PASSWORD:rootroot}
      database: <database-name>
```

### Redis Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:rootroot}
```

### Cache Configuration (Redis)

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000          # 5 minutes default TTL
```

### Mail Configuration

```yaml
spring:
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}
```

### Resilience4j Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      serviceName:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
        permitted-number-of-calls-in-half-open-state: 3
  retry:
    instances:
      serviceName:
        max-attempts: 3
        wait-duration: 500ms
```

### OpenAPI Configuration

```yaml
springdoc:
  swagger-ui:
    doc-expansion: none
    operations-sorter: alpha
    tags-sorter: alpha
```

---

## Per-Service Configurations

### Authentication Service

```yaml
spring:
  application:
    name: authentication-service
  data:
    mongodb:
      database: auth_db
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}

jwt:
  secret: ${JWT_SECRET:mylongsecretkeythatisatleast256bitslong}
  expiration-ms: ${JWT_EXPIRATION_MS:3600000}           # 1 hour
  refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION_MS:86400000}  # 24 hours

server:
  port: 8081
```

### Customer Profile Service

```yaml
spring:
  application:
    name: customer-profile-service
  data:
    mongodb:
      database: customer_db

server:
  port: 8082
```

### Loan Offer Service

```yaml
spring:
  application:
    name: loan-offer-service
  data:
    mongodb:
      database: offer_db
  cache:
    type: redis
    redis:
      time-to-live: 300000

server:
  port: 8083

resilience4j:
  circuitbreaker:
    instances:
      customerProfileService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
  retry:
    instances:
      customerProfileService:
        max-attempts: 3
        wait-duration: 500ms
```

### EMI Calculation Service

```yaml
spring:
  application:
    name: emi-calculation-service

server:
  port: 8084
```

### Loan Application Service

```yaml
spring:
  application:
    name: loan-application-service
  data:
    mongodb:
      database: application_db

server:
  port: 8085
```

### Loan Processing Service

```yaml
spring:
  application:
    name: loan-processing-service
  data:
    mongodb:
      database: processing_db

server:
  port: 8086

resilience4j:
  circuitbreaker:
    instances:
      applicationService:
        sliding-window-size: 10
        failure-rate-threshold: 50
  retry:
    instances:
      applicationService:
        max-attempts: 3
        wait-duration: 500ms
```

### Notification Service

```yaml
spring:
  application:
    name: notification-service
  data:
    mongodb:
      database: notification_db
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}

server:
  port: 8087
```

### Document Service

```yaml
spring:
  application:
    name: document-service
  data:
    mongodb:
      database: document_db

server:
  port: 8088
```

---

## API Gateway Configuration

```yaml
spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin
      globalcors:
        cors-configurations:
          "[/**]":
            allowedOrigins: "${CORS_ORIGINS:http://localhost:5173}"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
      routes:
        - id: authentication-service
          uri: lb://authentication-service
          predicates:
            - Path=/api/v1/auth/**
        - id: customer-profile-service
          uri: lb://customer-profile-service
          predicates:
            - Path=/api/v1/customers/**
        # ... other routes

server:
  port: 8080
```

---

## Environment Variables (Quick Reference)

### Infrastructure

| Variable | Default | Used By |
|---|---|---|
| `MONGODB_HOST` | `localhost` | All DB-connected services |
| `MONGODB_PORT` | `27017` | All DB-connected services |
| `MONGODB_USERNAME` | `root` | All DB-connected services |
| `MONGODB_PASSWORD` | `rootroot` | All DB-connected services |
| `REDIS_HOST` | `localhost` | Gateway, Auth, Offer |
| `REDIS_PORT` | `6379` | Gateway, Auth, Offer |
| `REDIS_PASSWORD` | `rootroot` | Gateway, Auth, Offer |

### Security

| Variable | Default | Used By |
|---|---|---|
| `JWT_SECRET` | `loansphere-...` | All services |
| `JWT_EXPIRATION_MS` | `3600000` | Auth service |
| `JWT_REFRESH_EXPIRATION_MS` | `86400000` | Auth service |

### Service Discovery

| Variable | Default | Used By |
|---|---|---|
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | All services |

### Email

| Variable | Default | Used By |
|---|---|---|
| `MAIL_HOST` | `localhost` | Auth, Notification |
| `MAIL_PORT` | `1025` | Auth, Notification |

### CORS

| Variable | Default | Used By |
|---|---|---|
| `CORS_ORIGINS` | `http://localhost:5173` | API Gateway |

---

## Profile-Specific Configuration

### Development Profile

```yaml
# application-dev.yml
spring:
  data:
    mongodb:
      host: localhost
    redis:
      host: localhost

logging:
  level:
    com.loansphere: DEBUG
    org.springframework.security: DEBUG
```

### Production Profile

```yaml
# application-prod.yml
spring:
  data:
    mongodb:
      host: ${MONGODB_HOST}
      username: ${MONGODB_USERNAME}
      password: ${MONGODB_PASSWORD}
    redis:
      host: ${REDIS_HOST}
      password: ${REDIS_PASSWORD}

logging:
  level:
    com.loansphere: INFO
    org.springframework: WARN
```

### Activating Profiles

```bash
# Command line
java -jar app.jar --spring.profiles.active=dev

# Docker / K8s
SPRING_PROFILES_ACTIVE=prod
```

---

## Feature Flags

### Virtual Threads

```yaml
spring:
  threads:
    virtual:
      enabled: true    # Enable on all services
```

### Async Processing

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
        // Uses virtual threads when spring.threads.virtual.enabled=true
        return new SimpleAsyncTaskExecutor();
    }
}
```

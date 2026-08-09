# Database Guide

## MongoDB Overview

LoanSphere uses MongoDB 8 as the primary database. Each microservice owns its own database, following the **Database-per-Service** pattern. No service directly accesses another service's database.

### Databases

| Service | Database | Purpose |
|---|---|---|
| Authentication | `auth_db` | Users, credentials |
| Customer Profile | `customer_db` | Customer biodata, employment |
| Loan Offer | `offer_db` | Loan product definitions |
| Loan Application | `application_db` | Submitted applications |
| Loan Processing | `processing_db` | Review workflows |
| Notification | `notification_db` | Notification records |
| Document | `document_db` | File metadata + GridFS |

## Collections

### `auth_db`

#### `users`

```json
{
  "_id": ObjectId,
  "username": "admin",
  "email": "admin@loansphere.com",
  "password": "$2a$10$...",
  "firstName": "System",
  "lastName": "Administrator",
  "roles": ["SUPER_ADMIN"],
  "emailVerified": true,
  "enabled": true,
  "accountNonLocked": true,
  "createdAt": ISODate("2026-07-25T00:00:00Z"),
  "updatedAt": ISODate("2026-07-25T00:00:00Z"),
  "_version": 1
}
```

**Indexes**:
- `{ "username": 1 }` — unique
- `{ "email": 1 }` — unique

---

### `customer_db`

#### `customer_profiles`

```json
{
  "_id": ObjectId,
  "userId": "64a1b2c3...",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phone": "+91-9876543210",
  "dateOfBirth": "1990-01-15",
  "gender": "Male",
  "panNumber": "ABCDE1234F",
  "aadhaarNumber": "123456789012",
  "annualIncome": 1500000,
  "employmentType": "Salaried",
  "employerName": "Tech Corp",
  "creditScore": 750,
  "createdAt": ISODate("2026-07-25T00:00:00Z"),
  "updatedAt": ISODate("2026-07-25T00:00:00Z"),
  "_version": 1
}
```

**Indexes**:
- `{ "userId": 1 }` — unique

---

### `offer_db`

#### `loan_offers`

```json
{
  "_id": ObjectId,
  "name": "Personal Loan",
  "description": "Unsecured personal loan for any purpose",
  "minAmount": 50000,
  "maxAmount": 5000000,
  "interestRate": 10.5,
  "minTenureMonths": 12,
  "maxTenureMonths": 60,
  "processingFee": 1.0,
  "minCreditScore": 650,
  "minAnnualIncome": 300000,
  "active": true,
  "expiresAt": ISODate("2027-12-31T23:59:59Z"),
  "createdAt": ISODate("2026-07-25T00:00:00Z"),
  "updatedAt": ISODate("2026-07-25T00:00:00Z"),
  "_version": 1
}
```

**Indexes**:
- `{ "active": 1, "expiresAt": 1 }` — compound (active offers query)

---

### `application_db`

#### `loan_applications`

```json
{
  "_id": ObjectId,
  "userId": "64a1b2c3...",
  "offerId": "64b1c2d3...",
  "offerName": "Personal Loan",
  "loanAmount": 500000,
  "tenureMonths": 36,
  "purpose": "Home renovation",
  "status": "SUBMITTED",
  "documentIds": ["64c1d2e3..."],
  "reviewerNotes": "",
  "rejectionReason": "",
  "createdAt": ISODate("2026-07-25T00:00:00Z"),
  "updatedAt": ISODate("2026-07-25T00:00:00Z"),
  "_version": 1
}
```

**Indexes**:
- `{ "userId": 1 }` — user's applications
- `{ "status": 1 }` — review queue lookup

**Status Values**: `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `DISBURSED`, `CLOSED`

---

### `notification_db`

#### `notifications`

```json
{
  "_id": ObjectId,
  "userId": "64a1b2c3...",
  "title": "Application Submitted",
  "message": "Your loan application #APP123 has been submitted.",
  "type": "EMAIL",
  "status": "SENT",
  "recipient": "john@example.com",
  "read": false,
  "createdAt": ISODate("2026-07-25T00:00:00Z"),
  "updatedAt": ISODate("2026-07-25T00:00:00Z"),
  "_version": 1
}
```

**Indexes**:
- `{ "userId": 1, "read": 1 }` — user's unread notifications

---

### `document_db`

#### `documents` (metadata collection)

```json
{
  "_id": ObjectId,
  "userId": "64a1b2c3...",
  "fileName": "passport.pdf",
  "contentType": "application/pdf",
  "fileSize": 245760,
  "fileId": "507f1f77bcf86cd799439011",
  "documentType": "ID_PROOF",
  "version": 1,
  "status": "ACTIVE",
  "createdAt": ISODate("2026-07-25T00:00:00Z"),
  "updatedAt": ISODate("2026-07-25T00:00:00Z"),
  "_version": 1
}
```

**Indexes**:
- `{ "userId": 1 }` — user's documents
- `{ "fileId": 1 }` — unique (GridFS reference)

**File Storage**: Actual files stored via MongoDB GridFS (`fs.files` and `fs.chunks` collections).

## Redis Usage

### Key Patterns

| Pattern | TTL | Purpose |
|---|---|---|
| `refresh:<uuid>` | 24 hours | Refresh token → User ID |
| `blacklist:<jwt>` | Token expiry | Revoked access tokens |
| `pwd-reset:<uuid>` | 15 minutes | Password reset token → User ID |
| `email-verify:<uuid>` | 24 hours | Email verification token → User ID |
| `activeOffers::all` | 5 minutes | Cached active loan offers |
| `sessions:<userId>` | 30 minutes | User session data |
| `otp:<userId>` | 5 minutes | One-time passwords |

### Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

## Data Access Patterns

### Repository Pattern

All MongoDB access through Spring Data MongoDB repositories:

```java
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

### Optimistic Locking

All entities use `@Version` for optimistic concurrency control:

```java
@Version
private Long version;
```

MongoDB throws `OptimisticLockingFailureException` on version mismatch.

### Auditing

`@CreatedDate` and `@LastModifiedDate` automatically populate timestamps:

```java
@CreatedDate
private Instant createdAt;

@LastModifiedDate
private Instant updatedAt;
```

Requires `@EnableMongoAuditing` in configuration.

## Backups

### MongoDB Dump

```bash
# Backup all databases
docker compose exec mongodb mongodump \
  --username root --password rootroot \
  --authenticationDatabase admin \
  --out /data/backup

# Backup specific database
docker compose exec mongodb mongodump \
  --username root --password rootroot \
  --authenticationDatabase admin \
  --db auth_db --out /data/backup/auth
```

### Redis Backup

```bash
docker compose exec redis redis-cli -a rootroot BGSAVE
# Backup file: /data/dump.rdb (mounted to redis_data/)
```

# Development Guide

## Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose)
- Java 21 (Temurin recommended)
- Maven 3.9+
- Node.js 22+
- npm 10+

## Local Development Setup

### 1. Clone the Repository

```bash
git clone <repo-url>
cd commerce-sphere
```

### 2. Start Infrastructure

Start MongoDB and Redis in Docker:

```bash
docker compose up -d mongodb redis mongo-express redis-insight
```

This initializes databases with default data (admin user, roles, categories, brands, warehouse).

### 3. Run a Backend Service

Each service runs independently:

```bash
cd backend/identity-service
./mvnw spring-boot:run
```

Or run all backend services via Docker:

```bash
docker compose up -d --build identity-service catalog-service cart-service inventory-service order-service
```

### 4. Run Frontend

```bash
cd ui/admin-console
npm install
npm run dev
```

```bash
cd ui/storefront
npm install
npm run dev
```

## Running Tests

### Backend Tests

```bash
cd backend/identity-service
./mvnw test
```

### Frontend Tests

```bash
cd ui/admin-console
npm test
```

## Debugging

### View Service Logs

```bash
docker compose logs -f identity-service
docker compose logs -f catalog-service
```

### Access Service Shell

```bash
docker compose exec identity-service sh
docker compose exec mongodb mongosh
```

### Check Service Health

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

## Environment Variables

See `.env` file at the project root for all configurable environment variables.

Key variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `MONGO_ROOT_USER` | MongoDB username | root |
| `MONGO_ROOT_PASSWORD` | MongoDB password | rootroot |
| `REDIS_PASSWORD` | Redis password | rootroot |
| `JWT_SECRET` | JWT signing secret | (see .env) |
| `ADMIN_PASSWORD` | Default admin password | Admin@123 |

## Project Structure Conventions

### Backend Service Structure

```
backend/service-name/
├── pom.xml                    # Maven build (no parent POM)
├── Dockerfile                 # Multi-stage build
├── README.md
├── src/main/java/.../
│   ├── config/                # @Configuration classes
│   ├── controller/            # REST controllers
│   ├── dto/                   # Data transfer objects (Java records)
│   ├── entity|document/       # MongoDB documents
│   ├── repository/            # Spring Data repositories
│   ├── service/               # Business logic
│   ├── mapper/                # MapStruct interfaces
│   ├── security/              # Security filters, entry points
│   ├── exception/             # Custom exceptions
│   ├── advice/                # @ControllerAdvice handlers
│   ├── event/                 # Spring events
│   ├── listener/              # Event listeners
│   └── scheduler/             # Scheduled tasks
└── helm/                      # Kubernetes Helm chart
```

### Frontend Structure

```
ui/app-name/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── nginx.conf
├── Dockerfile
└── src/
    ├── api/                   # Axios API client modules
    ├── hooks/                 # Custom React hooks + context providers
    ├── components/            # Reusable UI components
    └── pages/                 # Route-level page components
```

# Identity Service

Handles user registration, authentication, JWT token management, and role-based access control for the CommerceSphere platform.

## Tech Stack
- Java 21 with Virtual Threads
- Spring Boot 3.3
- Spring Security 6.3 with JWT
- MongoDB (users, roles)
- Redis (refresh tokens, sessions, rate limiting)

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login, get JWT |
| POST | `/api/auth/refresh` | Public | Refresh access token |
| GET | `/api/users/profile` | Bearer | Get current user profile |
| PUT | `/api/users/password` | Bearer | Change password |
| GET | `/api/admin/users` | Admin | List all users |

### Default Admin
- **Username:** admin
- **Password:** Admin@123

## Running Locally

```bash
# Ensure MongoDB and Redis are running
cd backend/identity-service
./mvnw spring-boot:run
```

Swagger UI: http://localhost:8081/swagger-ui.html

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `IDENTITY_SERVICE_PORT` | 8081 | Server port |
| `MONGO_HOST` | localhost | MongoDB host |
| `MONGO_PORT` | 27017 | MongoDB port |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6379 | Redis port |
| `JWT_SECRET` | (see .env) | JWT signing secret |
| `JWT_EXPIRATION_MS` | 3600000 | Access token TTL (1 hour) |
| `JWT_REFRESH_EXPIRATION_MS` | 2592000000 | Refresh token TTL (30 days) |

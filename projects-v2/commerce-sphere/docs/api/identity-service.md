# Identity Service API

Base URL: `http://localhost:8081` (Docker) or `http://localhost:8082` (from host via Docker Compose)

## Authentication Endpoints

### Register New User

```
POST /api/auth/register
```

**Request Body:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securePass123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "uuid.refresh.jwt.token",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "username": "johndoe",
  "email": "john@example.com",
  "fullName": "John Doe"
}
```

**Error Responses:**
- `409 Conflict` — Username or email already exists
- `400 Bad Request` — Validation failed

### Login

```
POST /api/auth/login
```

**Request Body:**
```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

**Response:** `200 OK` — Same as register response

**Error Responses:**
- `401 Unauthorized` — Invalid credentials

### Refresh Token

```
POST /api/auth/refresh
```

**Request Body:**
```json
{
  "refreshToken": "uuid.refresh.jwt.token"
}
```

**Response:** `200 OK` — New token pair

---

## User Endpoints (requires Bearer token)

### Get Profile

```
GET /api/users/profile
Authorization: Bearer <accessToken>
```

**Response:** `200 OK`
```json
{
  "id": "user-id",
  "username": "admin",
  "email": "admin@commercesphere.com",
  "firstName": "Admin",
  "lastName": "User",
  "roles": ["ROLE_ADMIN"]
}
```

### Change Password

```
PUT /api/users/password
Authorization: Bearer <accessToken>
```

**Request Body:**
```json
{
  "currentPassword": "Admin@123",
  "newPassword": "NewPass@456"
}
```

**Response:** `200 OK`

---

## Admin Endpoints (requires ADMIN role)

### List All Users

```
GET /api/admin/users
Authorization: Bearer <accessToken>
```

**Response:** `200 OK` — Array of user profiles

## Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | Admin@123 |

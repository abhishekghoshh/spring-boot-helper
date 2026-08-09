# API Reference

All API endpoints follow RESTful conventions. Base URL: `http://localhost:8080/api/v1`

## Common Patterns

### Response Format

All endpoints return a consistent envelope:

```json
{
  "success": true,
  "message": "Success",
  "data": { },
  "timestamp": "2026-07-25T12:00:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "message": "User not found with id: 'abc123'",
  "data": null,
  "timestamp": "2026-07-25T12:00:00Z"
}
```

### Validation Error

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request parameters",
  "path": "/api/v1/auth/register",
  "timestamp": "2026-07-25T12:00:00Z",
  "errors": [
    { "field": "email", "message": "must be a well-formed email address" }
  ]
}
```

### Authentication

All protected endpoints require a JWT token:

```
Authorization: Bearer <access_token>
```

---

## Authentication API

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response** (200):
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "id": "64a1b2c3...",
    "username": "johndoe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["CUSTOMER"],
    "emailVerified": false
  }
}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123"
}
```

**Response** (200):
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer"
  }
}
```

### Refresh Token

```http
POST /api/v1/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <access_token>
```

### Get Current User

```http
GET /api/v1/auth/me
Authorization: Bearer <access_token>
```

**Response** (200):
```json
{
  "success": true,
  "data": {
    "id": "64a1b2c3...",
    "username": "admin",
    "email": "admin@loansphere.com",
    "firstName": "System",
    "lastName": "Administrator",
    "roles": ["SUPER_ADMIN"],
    "emailVerified": true
  }
}
```

### Forgot Password

```http
POST /api/v1/auth/forgot-password
{
  "email": "admin@loansphere.com"
}
```

### Reset Password

```http
POST /api/v1/auth/reset-password
{
  "token": "uuid-token-from-email",
  "newPassword": "NewSecurePass456!"
}
```

---

## Customer Profile API

### Create Profile

```http
POST /api/v1/customers/profile
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phone": "+91-9876543210",
  "dateOfBirth": "1990-01-15",
  "gender": "Male",
  "panNumber": "ABCDE1234F",
  "aadhaarNumber": "123456789012",
  "annualIncome": 1500000.00,
  "employmentType": "Salaried",
  "employerName": "Tech Corp",
  "creditScore": 750
}
```

### Get Profile

```http
GET /api/v1/customers/profile
Authorization: Bearer <access_token>
```

### Update Profile

```http
PUT /api/v1/customers/profile
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "phone": "+91-9999999999",
  "annualIncome": 1800000.00,
  "creditScore": 780
}
```

---

## Loan Offer API

### List Active Offers

```http
GET /api/v1/offers
Authorization: Bearer <access_token>
```

**Response** (200):
```json
{
  "success": true,
  "data": [
    {
      "id": "64b1...",
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
      "active": true
    }
  ]
}
```

### Get Offer Details

```http
GET /api/v1/offers/{id}
```

### Calculate EMI for Offer

```http
GET /api/v1/offers/{id}/emi?amount=500000&tenureMonths=36
```

### Create Offer (Admin)

```http
POST /api/v1/offers
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "name": "Home Loan",
  "description": "Home purchase and construction loan",
  "minAmount": 500000,
  "maxAmount": 50000000,
  "interestRate": 8.5,
  "minTenureMonths": 60,
  "maxTenureMonths": 360,
  "processingFee": 0.5,
  "minCreditScore": 700,
  "minAnnualIncome": 500000,
  "active": true
}
```

---

## EMI Calculation API

### Calculate EMI

```http
GET /api/v1/emi/calculate?principal=1000000&annualInterestRate=10.5&tenureMonths=36
```

**Response** (200):
```json
{
  "success": true,
  "data": {
    "monthlyEmi": 32500.45,
    "principal": 1000000,
    "totalInterest": 170016.20,
    "totalPayment": 1170016.20
  }
}
```

### Amortization Schedule

```http
GET /api/v1/emi/amortization-schedule?principal=1000000&annualInterestRate=10.5&tenureMonths=36
```

**Response** (200):
```json
{
  "success": true,
  "data": [
    {
      "month": 1,
      "emi": 32500.45,
      "principal": 23750.45,
      "interest": 8750.00,
      "remainingPrincipal": 976249.55
    },
    {
      "month": 2,
      "emi": 32500.45,
      "principal": 23958.25,
      "interest": 8542.20,
      "remainingPrincipal": 952291.30
    }
  ]
}
```

### Prepayment Impact

```http
GET /api/v1/emi/prepayment?principal=1000000&annualInterestRate=10.5&tenureMonths=36&prepaymentAmount=200000
```

**Response** (200):
```json
{
  "success": true,
  "data": {
    "originalMonthlyEmi": 32500.45,
    "newMonthlyEmi": 28750.30,
    "monthsCompleted": 6,
    "remainingMonths": 30,
    "interestSaved": 45600.75
  }
}
```

---

## Loan Application API

### Submit Application

```http
POST /api/v1/applications
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "offerId": "64b1...",
  "offerName": "Personal Loan",
  "loanAmount": 500000,
  "tenureMonths": 36,
  "purpose": "Home renovation"
}
```

### List My Applications

```http
GET /api/v1/applications
Authorization: Bearer <access_token>
```

### Get Application

```http
GET /api/v1/applications/{id}
Authorization: Bearer <access_token>
```

---

## Loan Processing API

### Get Review Queue (Staff)

```http
GET /api/v1/processing/reviews
Authorization: Bearer <staff_token>
```

**Response** (200):
```json
{
  "success": true,
  "data": {
    "pending": 5,
    "inProgress": 2,
    "completedToday": 12,
    "totalQueue": 7
  }
}
```

### Approve Application

```http
POST /api/v1/processing/reviews/{id}/approve
Authorization: Bearer <staff_token>
Content-Type: application/json

{
  "notes": "All documents verified. Credit score meets threshold."
}
```

### Reject Application

```http
POST /api/v1/processing/reviews/{id}/reject
Authorization: Bearer <staff_token>
Content-Type: application/json

{
  "reason": "Credit score below minimum requirement"
}
```

### Request Additional Information

```http
POST /api/v1/processing/reviews/{id}/request-info
Authorization: Bearer <staff_token>
Content-Type: application/json

{
  "message": "Please upload latest salary slip"
}
```

---

## Document API

### Upload Document

```http
POST /api/v1/documents/upload
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

file: <binary>
documentType: ID_PROOF
```

**Response** (200):
```json
{
  "success": true,
  "message": "Document uploaded",
  "data": {
    "id": "64c1...",
    "userId": "64a1...",
    "fileName": "passport.pdf",
    "contentType": "application/pdf",
    "fileSize": 245760,
    "documentType": "ID_PROOF",
    "version": 1,
    "status": "ACTIVE"
  }
}
```

### List Documents

```http
GET /api/v1/documents
Authorization: Bearer <access_token>
```

### Download Document

```http
GET /api/v1/documents/{id}/download
Authorization: Bearer <access_token>
```

### Get Document Metadata

```http
GET /api/v1/documents/{id}
```

### Delete Document

```http
DELETE /api/v1/documents/{id}
Authorization: Bearer <access_token>
```

---

## Notification API

### Get Notifications

```http
GET /api/v1/notifications
Authorization: Bearer <access_token>
```

**Response** (200):
```json
{
  "success": true,
  "data": [
    {
      "id": "64d1...",
      "userId": "64a1...",
      "title": "Application Submitted",
      "message": "Your loan application #APP123 has been submitted.",
      "type": "EMAIL",
      "status": "SENT",
      "recipient": "john@example.com",
      "read": false
    }
  ]
}
```

### Mark Notification as Read

```http
PUT /api/v1/notifications/{id}/read
Authorization: Bearer <access_token>
```

---

## HTTP Status Codes

| Code | Meaning |
|---|---|
| `200` | Success |
| `201` | Created |
| `400` | Bad Request / Validation Error |
| `401` | Unauthorized (missing/invalid token) |
| `403` | Forbidden (insufficient permissions) |
| `404` | Resource Not Found |
| `409` | Conflict (duplicate resource) |
| `429` | Rate Limit Exceeded |
| `500` | Internal Server Error |

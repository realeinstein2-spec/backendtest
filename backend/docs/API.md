# MakersHub REST API Reference

Base URL: `https://api.makershub.gh/v1`

## Authentication

All endpoints except `/auth/**`, `/public/**`, `/webhooks/**`, `/swagger-ui/**`, `/v3/api-docs/**`, and `/actuator/health` require a Bearer JWT token.

```http
Authorization: Bearer <access_token>
```

## Endpoints

### Authentication

#### POST /auth/register
Register a new SME or factory owner.

**Body:**
```json
{
  "phoneNumber": "+233241234567",
  "password": "SecurePass123",
  "fullName": "Kwame Mensah",
  "role": "SME_OWNER",
  "ghanaCardNumber": "GHA-123456789-0",
  "region": "Ashanti",
  "town": "Kumasi"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "phoneNumber": "+233241234567",
  "fullName": "Kwame Mensah",
  "role": "SME_OWNER",
  "isVerified": false,
  "region": "Ashanti"
}
```

#### POST /auth/login
**Body:**
```json
{
  "phoneNumber": "+233241234567",
  "password": "SecurePass123"
}
```

**Response 200:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "accessTokenExpiry": "2024-01-01T00:15:00Z",
  "refreshTokenExpiry": "2024-02-01T00:00:00Z",
  "tokenType": "Bearer"
}
```

#### POST /auth/refresh
**Body:** `{ "refreshToken": "eyJ..." }`

### Jobs

#### POST /jobs
SME posts a job.

**Body:**
```json
{
  "title": "500 custom tote bags",
  "productType": "Tote Bag",
  "sectorTag": "TEXTILES",
  "quantity": 500,
  "specifications": "Canvas, 12x14 inches, black logo",
  "budgetMinGhs": 2000.00,
  "budgetMaxGhs": 3500.00,
  "deadline": "2024-12-31",
  "deliveryAddress": "Adum, Kumasi"
}
```

#### GET /jobs
Factories browse open jobs. Supports `sectorTag`, `minBudget`, `maxBudget`, pagination.

#### GET /jobs/{id}
Retrieve job details.

### Bids

#### POST /jobs/{jobId}/bids
Factory submits a bid.

**Body:**
```json
{
  "pricePerUnitGhs": 6.50,
  "totalPriceGhs": 3250.00,
  "productionDays": 7,
  "deliveryDateEstimate": "2024-12-30",
  "message": "We can start tomorrow."
}
```

#### GET /jobs/{jobId}/bids
SME views bids for their job.

#### PATCH /bids/{bidId}/accept
SME accepts a bid; creates an order.

### Orders

#### PATCH /orders/{id}/status
Factory updates order status.

**Body:**
```json
{
  "newStatus": "IN_PRODUCTION",
  "notes": "Materials procured."
}
```

Allowed transitions:
- `PAYMENT_PENDING` → `IN_ESCROW` / `CANCELLED`
- `IN_ESCROW` → `IN_PRODUCTION` / `REFUNDED`
- `IN_PRODUCTION` → `QUALITY_CHECK`
- `QUALITY_CHECK` → `DELIVERED`
- `DELIVERED` → `COMPLETED` / `DISPUTED`

#### POST /orders/{id}/confirm-delivery
SME confirms quality or rejects (opens dispute).

**Body:**
```json
{
  "qualityAccepted": true,
  "comment": "Great quality, thank you!"
}
```

### Payments

#### POST /payments/initiate
Initiate Paystack escrow payment.

**Body:**
```json
{
  "orderId": "uuid",
  "paymentMethod": "MTN_MOMO"
}
```

**Response:**
```json
{
  "reference": "MKH-...",
  "authorization_url": "https://paystack.com/pay/..."
}
```

### Reviews

#### POST /reviews
Submit a review after order completion.

### Disputes

#### POST /orders/{orderId}/dispute
Raise a dispute within the quality window.

### Messages

#### POST /messages
Send a message on an order thread.

#### GET /messages/orders/{orderId}
Get paginated messages for an order.

### Admin

#### GET /admin/analytics/dashboard
Platform KPIs.

#### GET /admin/factories/verification-queue
#### PATCH /admin/factories/{id}/verify
#### POST /admin/users/{id}/suspend

## Error Responses

```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/jobs",
  "fieldErrors": [
    { "field": "quantity", "message": "must be greater than 0" }
  ]
}
```

Common HTTP codes: `200`, `201`, `400`, `401`, `403`, `404`, `409`, `422`, `500`.

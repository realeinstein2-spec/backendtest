# Frontend Parameter Testing Guide

Purpose: give the frontend developer a beginning-to-end checklist for testing every required request parameter, path parameter, query parameter, auth header, and state transition against Swagger or the live API.

Base URL: `/api/v1`

Auth header for protected endpoints:

```text
Authorization: Bearer <accessToken>
```

## 1. Authentication

### POST `/auth/register`

Required body:

```json
{
  "phoneNumber": "+233240000001",
  "password": "SecurePass123",
  "fullName": "Frontend SME",
  "role": "SME_OWNER",
  "ghanaCardNumber": "GHA-TEST-001",
  "region": "Ashanti",
  "town": "Kumasi"
}
```

Test parameters:

- `phoneNumber`: valid 10-15 digits, optional `+`; invalid letters; too short; duplicate.
- `password`: minimum 8 chars; empty; too short.
- `fullName`: required, max 200.
- `role`: `SME_OWNER`, `FACTORY_OWNER`, `ENTERPRISE`; do not allow frontend self-registration as `ADMIN`.
- Optional fields: max lengths.

Expected result: user summary, usually unverified. In dev, `otpCode` may be present.

### POST `/auth/login`

Body:

```json
{
  "phoneNumber": "+233240000001",
  "password": "SecurePass123"
}
```

Expected result: pending auth response, not tokens.

Frontend must then call `/auth/verify`.

### POST `/auth/verify`

Body:

```json
{
  "phoneNumber": "+233240000001",
  "otp": "1234"
}
```

Test:

- Correct OTP.
- Wrong OTP repeatedly.
- Expired OTP.
- Missing phone.
- OTP length below 4 or above 8.

Expected result: `accessToken`, `refreshToken`, expiries, `tokenType`.

### POST `/auth/refresh`

Body:

```json
{
  "refreshToken": "<refreshToken>"
}
```

Test valid, invalid, expired, and refresh for suspended/unverified users.

## 2. User And Factory Profile

### GET `/users/me`

Requires token. Test each role.

### POST `/users/factory-profile`

Requires `FACTORY_OWNER`.

Body:

```json
{
  "companyName": "Frontend Test Factory",
  "description": "General manufacturing",
  "sectorTags": ["TEXTILES", "PACKAGING"],
  "machineryList": "{\"machines\":[\"CNC\",\"sewing\"]}",
  "minOrderQuantity": 10,
  "maxOrderQuantity": 1000,
  "latitude": 6.6885,
  "longitude": -1.6244,
  "address": "Kumasi"
}
```

Test:

- Empty `sectorTags`.
- Missing `companyName`.
- Latitude without longitude.
- Duplicate factory profile.
- Wrong role token.

## 3. Admin Verification

### GET `/admin/factories/verification-queue`

Requires `ADMIN`.

Query params:

- `status`: `PENDING`, `VERIFIED`, `SUSPENDED`, `REJECTED`
- `page`, `size`, `sort`

### PATCH `/admin/factories/{id}/verify`

Body:

```json
{
  "status": "VERIFIED",
  "notes": "Documents checked"
}
```

Test every status and invalid UUID.

## 4. Jobs

### POST `/jobs`

Requires `SME_OWNER` or `ENTERPRISE`.

Body:

```json
{
  "title": "500 custom tote bags",
  "productType": "Tote Bag",
  "sectorTag": "TEXTILES",
  "quantity": 500,
  "specifications": "Canvas, black logo",
  "budgetMinGhs": 2000.00,
  "budgetMaxGhs": 3500.00,
  "deadline": "2026-08-31",
  "deliveryAddress": "Adum, Kumasi",
  "attachmentUrls": ["https://example.com/spec.pdf"]
}
```

Test:

- Required fields missing.
- `quantity` below 1.
- Deadline in the past.
- `budgetMinGhs` greater than `budgetMaxGhs`.
- Long strings beyond max length.

### GET `/jobs`

Requires `FACTORY_OWNER`.

Query params:

- `sectorTag`
- `minBudget`
- `maxBudget`
- `page`
- `size`
- `sort`

Test invalid decimal strings for `minBudget` and `maxBudget`; current backend may return inconsistent errors.

### GET `/jobs/{id}`

Requires auth. Test valid ID, invalid UUID, not found.

## 5. Bids

### POST `/jobs/{jobId}/bids`

Requires verified `FACTORY_OWNER`.

Body:

```json
{
  "pricePerUnitGhs": 6.50,
  "totalPriceGhs": 3250.00,
  "productionDays": 7,
  "deliveryDateEstimate": "2026-08-20",
  "message": "We can start tomorrow."
}
```

Test:

- Factory unverified.
- Duplicate bid.
- Job not open.
- Negative price.
- Past delivery date.

### GET `/jobs/{jobId}/bids`

Requires `SME_OWNER`.

Test:

- SME owner of job.
- Different SME. Known backend issue: ownership check is missing and must be fixed before production.

### PATCH `/bids/{bidId}/accept`

Requires owning `SME_OWNER`.

Expected result: order with `PAYMENT_PENDING`.

## 6. Payments

### POST `/payments/initiate`

Requires `SME_OWNER` or `ENTERPRISE`.

Body:

```json
{
  "orderId": "<orderId>",
  "paymentMethod": "MTN_MOMO"
}
```

Payment methods:

- `MTN_MOMO`
- `TELECEL`
- `AIRTELTIGO`
- `CARD`
- `BANK_TRANSFER`

Test:

- Wrong owner.
- Wrong order status.
- Duplicate initiate.
- Blank or invalid payment method.

Frontend note: payment initiation alone does not make order `IN_ESCROW`; webhook confirmation does.

## 7. Orders

### GET `/orders`

Requires auth. Test SME, factory, enterprise.

### GET `/orders/{id}`

Requires order party. Test wrong user access.

### PATCH `/orders/{id}/status`

Requires assigned `FACTORY_OWNER`.

Body:

```json
{
  "newStatus": "IN_PRODUCTION",
  "notes": "Materials procured"
}
```

Allowed factory transitions after payment webhook:

- `IN_ESCROW` to `IN_PRODUCTION`
- `IN_PRODUCTION` to `QUALITY_CHECK`
- `QUALITY_CHECK` to `DELIVERED`

Do not build frontend UI that lets factory manually set `IN_ESCROW`.

### POST `/orders/{id}/confirm-delivery`

Requires owning `SME_OWNER`.

Accept body:

```json
{
  "qualityAccepted": true,
  "comment": "Accepted"
}
```

Reject body:

```json
{
  "qualityAccepted": false,
  "comment": "Wrong quantity"
}
```

Rejecting opens dispute state through order service.

### POST `/orders/{id}/cancel`

Requires owning `SME_OWNER` or `ENTERPRISE`; only works while `PAYMENT_PENDING`.

## 8. Reviews

### POST `/reviews`

Requires order party after order is `COMPLETED` or `REFUNDED`.

Body:

```json
{
  "orderId": "<orderId>",
  "overallRating": 5,
  "qualityRating": 5,
  "timelinessRating": 4,
  "communicationRating": 5,
  "comment": "Excellent work"
}
```

Test all ratings from 1 to 5 and invalid 0/6.

Known backend issue: current DB unique constraint may block the second party from reviewing.

## 9. Disputes

### POST `/orders/{orderId}/dispute`

Requires order party.

Body:

```json
{
  "reason": "QUALITY_BELOW_SPEC",
  "description": "The delivered material does not match specifications.",
  "evidenceUrls": ["https://example.com/photo.jpg"]
}
```

Reasons:

- `QUALITY_BELOW_SPEC`
- `WRONG_QUANTITY`
- `LATE_DELIVERY`
- `NOT_DELIVERED`
- `OTHER`

### GET `/admin/disputes`

Requires `ADMIN`.

### PATCH `/admin/disputes/{id}/resolve`

Requires `ADMIN`.

Body:

```json
{
  "resolution": "RESOLVED_BUYER",
  "adminNotes": "Refund approved",
  "refundAmountGhs": 100.00
}
```

Valid terminal resolutions:

- `RESOLVED_BUYER`
- `RESOLVED_SELLER`
- `RESOLVED_SPLIT`
- `CLOSED`

## 10. Messages

### POST `/messages`

Requires order party.

Body:

```json
{
  "orderId": "<orderId>",
  "content": "Can you confirm delivery time?",
  "attachmentUrl": "https://example.com/file.jpg"
}
```

### GET `/messages/orders/{orderId}`

Requires order party.

Query params: `page`, `size`, `sort`.

Test wrong order party and long content.

## 11. Admin And Analytics

### GET `/admin/analytics/dashboard`

Requires `ADMIN`.

### POST `/admin/users/{id}/suspend?reason=<reason>`

Requires `ADMIN`.

### PUT `/admin/users/{id}/unsuspend`

Requires `ADMIN`.

Test login/refresh behavior for suspended users.


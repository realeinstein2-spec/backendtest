# MakersHub — Frontend End-to-End Workflow Testing Guide

This guide maps out the exact sequence of user actions, UI behaviors, and API calls to test the entire MakersHub ecosystem. Follow these workflows in order to verify that your frontend integration matches the backend state machine.

---

## 🧭 Overview of Roles

For testing, you will need **three different accounts**:
1. **SME Owner (Client)** — Posts jobs, accepts bids, makes payments, accepts delivery.
2. **Factory Owner (Maker)** — Submits factory details, places bids, manages production, handles delivery.
3. **Admin (Platform manager)** — Verifies factory accounts, resolves order disputes, suspends/unsuspends users.

---

## 🔄 Workflow 1 — Registration, MFA & Token Storage

### Scenario: A new user registers and logs in securely.

#### 1. Register User (SME / Factory)
* **UI Step**: Open the Registration page. Fill out name, phone number (`+233...`), password, and choose role (`SME_OWNER` or `FACTORY_OWNER`).
* **API Action**: `POST /api/v1/auth/register`
* **Success Check**: 
  * Response contains a `201 Created` code.
  * In **Dev Environment**, the JSON response will include `"otpCode": "123456"`. In **Production**, the code is sent via SMS.
  * UI transitions to the **OTP Entry Page**.

#### 2. Confirm OTP (MFA)
* **UI Step**: Enter the 6-digit OTP code.
* **API Action**: `POST /api/v1/auth/verify`
* **Success Check**:
  * Response contains `accessToken` and `refreshToken`.
  * **Frontend Action**: Save these tokens securely (HTTPOnly cookies or local storage).
  * UI redirects user to the main dashboard.

#### 3. Log In (Existing Account)
* **UI Step**: Open Login page. Enter phone number and password.
* **API Action**: `POST /api/v1/auth/login`
* **Success Check**:
  * Response returns a `PendingAuthResponse` indicating an OTP has been sent. **No JWT tokens are returned yet.**
  * UI transitions to the OTP entry page to finish login.

---

## 🔄 Workflow 2 — Factory Setup & Admin Verification

### Scenario: A factory owner sets up their profile and gets verified by the administrator to enable bidding.

#### 1. Create Factory Profile (Factory Owner)
* **UI Step**: Logged in as `FACTORY_OWNER`. Go to Profile Setup. Enter company name, description, coordinates (latitude/longitude), and sector tags (e.g. `FURNITURE`, `TEXTILE`).
* **API Action**: `POST /api/v1/factories`
* **Success Check**:
  * Profile is created.
  * UI displays a banner: *"Your profile is pending admin verification."*
  * **Verify**: Try placing a bid. The API will return `403 Forbidden` because the profile is not yet `VERIFIED`.

#### 2. Verify Profile (Admin)
* **UI Step**: Log in using an `ADMIN` account. Open the Verification Queue page.
* **API Action**:
  * `GET /api/v1/admin/factories/verification-queue` (Lists pending profiles).
  * `PATCH /api/v1/admin/factories/{id}/verify` with `{"status": "VERIFIED", "notes": "Approved"}`.
* **Success Check**:
  * Factory status updates to `VERIFIED`.
  * The factory owner receives a push/SMS notification.
  * Factory owner's UI banner disappears, and bidding actions become active.

---

## 🔄 Workflow 3 — Job Posting & Bidding

### Scenario: An SME posts a job listing, and a verified factory submits a bid.

#### 1. Post a Job (SME Owner)
* **UI Step**: Logged in as `SME_OWNER`. Open "Create Job" form. Enter title, quantity, budget, specifications, and a future deadline date.
* **API Action**: `POST /api/v1/jobs`
* **Success Check**: Job is created with status `OPEN`.

#### 2. Search Open Jobs (Factory Owner)
* **UI Step**: Logged in as `FACTORY_OWNER`. Open "Find Jobs" board.
* **API Action**: `GET /api/v1/jobs` (filtered by `sectorTag` or budget range).
* **Success Check**: Displays a list of open jobs including the SME's new listing.

#### 3. Submit Bid (Factory Owner)
* **UI Step**: Open the detail view for the SME's job listing. Enter price-per-unit, total price, estimated production days, and click "Submit Bid".
* **API Action**: `POST /api/v1/jobs/{jobId}/bids`
* **Success Check**: 
  * Bid status is `PENDING`.
  * The job listing status transitions to `BIDDING`.
  * The SME owner receives an alert: *"New bid received for your job"*.

---

## 🔄 Workflow 4 — Bid Acceptance & Escrow Payment

### Scenario: The SME chooses the best bid, creating an order, and funds the escrow vault.

#### 1. Accept Bid (SME Owner)
* **UI Step**: Open the bids list for your job. Select a bid and click "Accept Bid".
* **API Action**: `PATCH /api/v1/bids/{bidId}/accept`
* **Success Check**:
  * An order is created with status `PAYMENT_PENDING`.
  * All other bids for this job are auto-rejected (transition to `REJECTED`).
  * The job listing status updates to `AWARDED`.

#### 2. Cancel Order (Awaiting Payment - Optional)
* **UI Step**: Before paying, the SME can click "Cancel Order".
* **API Action**: `POST /api/v1/orders/{orderId}/cancel`
* **Success Check**: Order status transitions to `CANCELLED`.

#### 3. Pay for Order (SME Owner)
* **UI Step**: Open the order details page. Click "Initiate Payment". Select payment method (e.g. `MTN_MOMO`).
* **API Action**: `POST /api/v1/payments/initiate`
* **Success Check**:
  * Returns a Paystack `reference` and `authorization_url`.
  * **Frontend Action**: Redirect the user or load `authorization_url` in a web view.
  * **Note**: In local/dev testing, the payment is simulated. Manually set the database order status to `IN_ESCROW` to proceed.

---

## 🔄 Workflow 5 — Production & Delivery Management

### Scenario: The factory runs the production cycle and delivers the items.

#### 1. Start Production (Factory Owner)
* **UI Step**: Logged in as `FACTORY_OWNER`. Open the order page. Click **"Start Production"**.
* **API Action**: `PATCH /api/v1/orders/{orderId}/status` with `{"newStatus": "IN_PRODUCTION"}`
* **Success Check**: Order status becomes `IN_PRODUCTION`.

#### 2. Request Quality Check (Factory Owner)
* **UI Step**: When manufacturing completes, click **"Ready for Quality Check"**.
* **API Action**: `PATCH /api/v1/orders/{orderId}/status` with `{"newStatus": "QUALITY_CHECK"}`
* **Success Check**: Order status becomes `QUALITY_CHECK`.

#### 3. Shipped & Delivered (Factory Owner)
* **UI Step**: Dispatch the package, then click **"Confirm Shipped/Delivered"**.
* **API Action**: `PATCH /api/v1/orders/{orderId}/status` with `{"newStatus": "DELIVERED"}`
* **Success Check**:
  * Order status becomes `DELIVERED`.
  * A 48-hour countdown is initialized (`qualityCheckDeadline`).

---

## 🔄 Workflow 6 — Quality Check, Disputes & Reviews

### Scenario A: Clean Acceptance (Happy Path)
* **UI Step**: Logged in as `SME_OWNER`. Inspect items, open order page, click **"Accept Delivery"**.
* **API Action**: `POST /api/v1/orders/{orderId}/confirm-delivery` with `{"qualityAccepted": true}`
* **Success Check**:
  * Order status becomes `COMPLETED`.
  * Escrow funds are released to the factory.
  * UI displays rating inputs for reviews.

---

### Scenario B: Reject Quality & File Dispute (Unhappy Path)
* **UI Step**: Logged in as `SME_OWNER`. Inspect items, detect defects, click **"Reject Delivery"**. Fill out the reason and evidence.
* **API Action**: `POST /api/v1/orders/{orderId}/confirm-delivery` with `{"qualityAccepted": false, "comment": "Description of defects"}`
* **Success Check**:
  * Order status becomes `DISPUTED`.
  * Escrow funds remain locked in the vault.
  * Admin is notified.

* **Admin Resolution Action**:
  * Admin logs in. Navigates to Dispute details.
  * Triggers `PATCH /api/v1/admin/disputes/{disputeId}/resolve` with `{"resolution": "RESOLVED_BUYER", "refundAmountGhs": 2000.00}`.
  * **Result**: Order status is set to `REFUNDED`. Escrow status changes to `REFUNDED`. Funds are returned to the SME.

---

### 🌟 Step 7: Submitting Reviews (After COMPLETED / REFUNDED)
* **UI Step**: Open the order rating panel. Rate overall quality, timeliness, and communication (1-5 stars). Add comments.
* **API Action**: `POST /api/v1/reviews`
* **Success Check**: 
  * Review is saved.
  * The counter-party's `ratingAvg` is updated.
  * The rating panel is hidden to prevent duplicate submissions.

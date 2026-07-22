# Frontend Feature Requirements

Purpose: define the frontend features required for the backend system to work fully, especially around the current evaluation findings and release/testing workflow.

## Required Global Frontend Behavior

- Use the backend prefix `/api/v1`.
- Store access token and refresh token securely according to the frontend platform.
- Attach `Authorization: Bearer <accessToken>` to protected requests.
- On `401`, attempt refresh once, then send user back to login.
- On `403`, show an authorization/role message rather than retrying.
- Render backend validation errors from `fieldErrors`.
- Render business errors using `error`, `message`, and `status`.
- Keep role-aware navigation: SME/Enterprise, Factory, Admin.
- Do not expose admin UI to non-admin users.
- Do not let users manually select state transitions the backend does not allow.

## Authentication Features

Must implement:

- Register screen with role selection limited to `SME_OWNER`, `FACTORY_OWNER`, `ENTERPRISE`.
- Login screen that expects pending auth response, not tokens.
- OTP verification screen after register and login.
- Token refresh support.
- Suspended/unverified account messaging.
- Logout that clears local tokens, even before backend refresh-token revocation exists.

Testing-specific:

- In dev/staging Swagger-style testing, support reading `otpCode` if backend returns it.
- In production UI, never display or depend on `otpCode` in response.

## SME / Enterprise Features

Must implement:

- Create job form with validation for title, product type, sector, quantity, budget, deadline, delivery address, and attachments.
- My jobs/order tracking.
- Bid list screen for jobs owned by the SME.
- Accept bid action.
- Payment initiation flow.
- Order cancellation while `PAYMENT_PENDING`.
- Delivery confirmation and rejection flow.
- Dispute creation when quality is rejected or within allowed dispute window.
- Review submission after completion/refund.
- Messaging inside order threads.

Important backend behavior:

- Payment initiation returns `authorization_url`; frontend should redirect or open payment flow.
- Order becomes `IN_ESCROW` only after webhook confirmation, not immediately after clicking pay.
- Current backend has a bid-list ownership issue; frontend should still only call bid listing for jobs owned by current user, but backend must fix this before production.

## Factory Features

Must implement:

- Factory registration and OTP verification.
- Factory profile creation.
- Verification status display: `PENDING`, `VERIFIED`, `SUSPENDED`, `REJECTED`.
- Open job marketplace list with filters.
- Job detail view.
- Submit bid form.
- Assigned orders list.
- Allowed order status update actions:
  - `IN_ESCROW` to `IN_PRODUCTION`
  - `IN_PRODUCTION` to `QUALITY_CHECK`
  - `QUALITY_CHECK` to `DELIVERED`
- Messaging inside order threads.
- Review submission after completion/refund.

Do not implement:

- Manual factory action to set `PAYMENT_PENDING` to `IN_ESCROW`.
- Bidding before factory verification.

## Admin Features

Must implement:

- Factory verification queue with status filter and pagination.
- Factory verification review action with status and notes.
- User suspension with required reason.
- User unsuspension.
- Dispute list.
- Dispute resolution with terminal status and optional refund amount.
- Analytics dashboard.

Admin statuses:

- Factory verification: `PENDING`, `VERIFIED`, `SUSPENDED`, `REJECTED`
- Dispute resolution: `RESOLVED_BUYER`, `RESOLVED_SELLER`, `RESOLVED_SPLIT`, `CLOSED`

## Payment And Escrow UI Requirements

Must implement:

- Payment pending state.
- Payment initiated state while waiting for webhook/backend order status update.
- In escrow state.
- Production state.
- Quality check state.
- Delivered state with confirmation/reject actions.
- Completed, disputed, refunded, cancelled terminal states.

Frontend should poll or refresh order state after payment redirect until backend confirms `IN_ESCROW`, or use WebSocket/order notifications if available.

## Messaging And Notification Features

Must implement:

- Order message thread.
- Send text message.
- Optional attachment URL.
- Paginated loading.
- Unread/read indicators can be deferred because backend currently exposes limited message read-state operations.

Can defer:

- Rich real-time chat UI.
- Push notification settings.
- Email notification preferences.

## Error Handling UX

Must implement:

- Validation error display per field.
- Global business error banner.
- Retry option for transient `5xx`.
- Forbidden state for wrong role.
- Not found state for deleted/missing resources.
- Payment failed or pending states.

Known backend testing issues to handle gracefully:

- Invalid UUID or decimal filters may return generic errors until backend improves exception handling.
- Second review submission by the other party may fail until review schema is fixed.

## Frontend Features That Can Be Deferred During Initial Testing

- Full notification preference center.
- Advanced search sorting beyond backend filters.
- Real-time WebSocket order/chat subscriptions.
- Refresh token rotation UI beyond clearing local tokens.
- Analytics charts beyond displaying returned numbers.
- Offline support.
- File upload UI if Cloudinary integration is not fully enabled; use URL fields for testing.

## Frontend Production Checklist

Before production frontend launch:

- No hardcoded test tokens, phone numbers, OTPs, or API keys.
- API base URL uses production environment variable.
- Role guards are implemented in routing and components.
- Payment redirect/callback UX is tested with Paystack test mode and production-like callback URLs.
- All forms enforce frontend validation matching backend limits.
- All backend errors are visible to the user or operator.
- Sensitive values are never logged in browser console.
- Admin routes are hidden and protected.
- Smoke test every role on the deployed backend.


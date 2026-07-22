# MakersHub — Google & Apple Social Authentication Integration Guide

This guide details the exact steps the frontend and backend developers must take to test and launch the new Google and Apple Sign-In/Sign-Up features.

---

## ⚙️ Backend Developer Next Steps (Railway Configuration)

### 1. Add Environment Variables in Railway
Go to your **Railway Service Variables** dashboard and add the following keys:
* **`GOOGLE_CLIENT_ID`**: The OAuth 2.0 Web Client ID generated from the **Google Cloud Console** under Credentials.
* **`APPLE_CLIENT_ID`**: The Services ID (App ID / Identifier) configured in your **Apple Developer Portal** under Certificates, Identifiers & Profiles.

### 2. Reset the Database (Optional but Recommended)
Since Flyway migration **`V6`** creates a partial unique index on the `email` column, any existing duplicate active emails in your database will fail the migration. 
To clear old test data and apply migrations cleanly:
1. Run this query in pgAdmin:
   ```sql
   DROP SCHEMA public CASCADE;
   CREATE SCHEMA public;
   ```
2. Restart the Railway app. Flyway will automatically run `V1` through `V6` to set up all tables.

---

## 📱 Frontend Developer Next Steps (Integration Flow)

### 1. Client-Side Authentication (Obtaining the `idToken`)
Your frontend app (Web, iOS, or Android) must load the Google Identity Services SDK or Apple Sign-In SDK and prompt the user.
* **Result**: On successful authorization, the SDK returns an **`idToken`** (a signed JWT payload from Google or Apple).

### 2. Submit Token to Backend
The frontend sends this token to the backend using the new generic endpoint:

👉 **`POST /api/v1/auth/social/{provider}`**  *(where `{provider}` is `google` or `apple`)*

#### Request Body Schema (`SocialLoginRequest`):
```json
{
  "idToken": "eyJ...",         // 🔑 Token received from Google/Apple SDK
  "role": "SME_OWNER",         // 👤 Selected Role: 'SME_OWNER' or 'FACTORY_OWNER' (Used on first-time signup)
  "fullName": "Kwame Mensah",  // 📝 Optional. If empty, the backend falls back to Google/Apple profile name
  "phoneNumber": "+233..."     // 📞 Required for first-time sign-up. Leave null or omit for returning login.
}
```

---

## 🔄 The E2E Integration Flowchart

### Path A: Returning User (Instant Login)
1. Frontend submits the Google/Apple `idToken`.
2. Backend verifies the token signature against Google/Apple public keys, extracts the email, and finds the matching active record in the database.
3. Backend returns a `200 OK` response with JWT tokens:
   ```json
   {
     "accessToken": "eyJ...",
     "refreshToken": "eyJ...",
     "tokenType": "Bearer",
     "user": {
       "id": "uuid",
       "phoneNumber": "+233240000000",
       "fullName": "Kofi Mensah",
       "role": "SME_OWNER",
       "isVerified": true
     }
   }
   ```
4. Frontend saves tokens and logs user in immediately.

### Path B: New User (First-Time Registration Flow)
Because our database requires a unique phone number, the frontend must collect it if the user is new.

1. **Step 1**: Frontend submits `idToken` without a phone number:
   ```json
   {
     "idToken": "eyJ...",
     "role": "SME_OWNER"
   }
   ```
2. **Step 2**: Backend verifies the token, finds no matching email in the DB, and realizes this is a registration. Because `phoneNumber` is missing, backend returns:
   * **Status Code**: `400 Bad Request`
   * **Response Body**:
     ```json
     {
       "status": 400,
       "error": "PHONE_REQUIRED",
       "message": "Phone number is required for first-time registration"
     }
     ```
3. **Step 3**: Frontend detects the `"error": "PHONE_REQUIRED"` key.
4. **Step 4**: Frontend opens a modal or onboarding form prompting the user:
   * *"Welcome! Please confirm your phone number and selected role to complete registration."*
5. **Step 5**: The user enters their phone number (`+233...`), and frontend resubmits the request:
   ```json
   {
     "idToken": "eyJ...",
     "role": "SME_OWNER",
     "phoneNumber": "+233241234567"
     // optional: "fullName": "Custom Name"
   }
   ```
6. **Step 6**: Backend registers the user, saves their profile, and returns the JWT tokens. Onboarding complete!

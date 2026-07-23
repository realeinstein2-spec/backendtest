# Developer Integration Instructions (Frontend & Backend)

This document describes how the Refresh Token Whitelist, Refresh Token Rotation (RTR), and the new `/logout` endpoint are integrated, detailing instructions for both frontend and backend developers.

---

## 🚀 Deployment & Database Migrations

### 1. Automatic Schema Migrations
- The new `refresh_tokens` table is managed via Flyway. 
- When the code is pushed to your deployment server (e.g., Railway, Heroku, AWS, Docker), Flyway will automatically scan the classpath, detect [V13__create_refresh_token_table.sql](file:///C:/Users/User/Desktop/backendtest/backend/src/main/resources/db/migration/V13__create_refresh_token_table.sql), and apply it to the database **before** the application boots.
- **No manual SQL execution** is needed on the production database.

### 2. Environment Variables
- Ensure that the `JWT_SECRET` environment variable remains secure and matching in all environments.
- The default token expiration settings configured in [application.yml](file:///C:/Users/User/Desktop/backendtest/backend/src/main/resources/application.yml) will be active unless overridden in your host environment:
  - `makershub.jwt.access-expiry-ms` (Default: 30 mins)
  - `makershub.jwt.refresh-expiry-ms` (Default: 30 days)

---

## 📱 For the Frontend Developer

The authentication flow is now backed by a server-side whitelist. Logging out on the frontend must now notify the backend to invalidate the session.

### 1. Store Access & Refresh Tokens
Ensure your authentication state store (e.g. Zustand, Redux, React Native AsyncStorage) stores:
* `accessToken`
* `refreshToken`
* `accessTokenExpiry`
* `refreshTokenExpiry`

### 2. Auto-Refresh Access Tokens (Axios Interceptor)
When the stateless `accessToken` expires (30 minutes), backend requests will fail with `401 Unauthorized`. You must catch this, refresh the token, and retry the request using this interceptor pattern:

```ts
import axios from 'axios';

const API_BASE = 'https://api.makershub.gh/v1'; // Update this to match your environment url

const api = axios.create({
  baseURL: API_BASE,
});

// Request interceptor injects Bearer access token
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor handles 401 and refreshes
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refresh = useAuthStore.getState().refreshToken;
        
        // Refresh the tokens
        const { data } = await axios.post(`${API_BASE}/auth/refresh`, { 
          refreshToken: refresh 
        });
        
        // Update local store with the rotated tokens
        useAuthStore.getState().setTokens(data.accessToken, data.refreshToken);
        
        // Retry the original request with the new access token
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // If the refresh token was invalid/logged out, log the user out on frontend
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);
```

### 3. Call POST `/auth/logout` on Manual Logout
When the user explicitly logs out (e.g., clicks the "Log Out" button), send the refresh token to the backend before clearing tokens on the client:

```ts
const handleLogout = async () => {
  const refreshToken = useAuthStore.getState().refreshToken;
  
  try {
    // 1. Notify backend to delete the refresh token
    await api.post('/auth/logout', { refreshToken });
  } catch (err) {
    console.warn("Backend logout failed or token already invalid:", err);
  } finally {
    // 2. Clear local storage tokens (Frontend Logout)
    useAuthStore.getState().logout();
  }
};
```

---

## 💻 For the Backend Developer

The database migration, entities, repositories, endpoints, and unit tests are complete and deployment-ready.

### 1. Refresh Token Flow in `AuthService.java`
- **Token Generation**: Calls `buildTokenResponse(...)` which persists the new refresh token:
  ```java
  RefreshToken refreshTokenEntity = RefreshToken.builder()
          .token(refreshTokenString)
          .user(user)
          .expiryTime(refreshExpiry)
          .build();
  refreshTokenRepository.save(refreshTokenEntity);
  ```
- **Token Refresh**: Queries `RefreshTokenRepository` to ensure the token exists in the database and is not expired. Then, deletes the old refresh token (`RTR` rotation) and generates/saves a new one.
- **Logout**: Deletes the token from the database.

### 2. Endpoints Exposed in `AuthController.java`
- `POST /api/v1/auth/refresh` (Body: `{ "refreshToken": "eyJ..." }`) -> Returns `TokenResponse`
- `POST /api/v1/auth/logout` (Body: `{ "refreshToken": "eyJ..." }`) -> Returns `204 No Content`

### 3. Local Verification Command
Always check compilation and tests before committing changes to Git:
```bash
cd backend
mvn clean compile test
```

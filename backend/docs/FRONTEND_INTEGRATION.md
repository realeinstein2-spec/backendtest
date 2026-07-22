# Frontend Integration Guide (React Native)

## Base URL

```ts
const API_BASE = 'https://api.makershub.gh/v1';
```

## Auth Store (Zustand example)

```ts
import { create } from 'zustand';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  setTokens: (access: string, refresh: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  setTokens: (access, refresh) => set({ accessToken: access, refreshToken: refresh }),
  logout: () => set({ accessToken: null, refreshToken: null }),
}));
```

## API Client

```ts
import axios from 'axios';
import { useAuthStore } from './authStore';

const api = axios.create({
  baseURL: 'https://api.makershub.gh/v1',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config;
    if (err.response?.status === 401 && !original._retry) {
      original._retry = true;
      const refresh = useAuthStore.getState().refreshToken;
      const { data } = await axios.post(`${API_BASE}/auth/refresh`, { refreshToken: refresh });
      useAuthStore.getState().setTokens(data.accessToken, data.refreshToken);
      original.headers.Authorization = `Bearer ${data.accessToken}`;
      return api(original);
    }
    return Promise.reject(err);
  }
);
```

## Key Flows

### Register → Login → Post Job

```ts
// Register
await api.post('/auth/register', {
  phoneNumber: '+233241234567',
  password: 'SecurePass123',
  fullName: 'Ama Owusu',
  role: 'SME_OWNER',
  region: 'Ashanti',
  town: 'Kumasi'
});

// Login
const { data } = await api.post('/auth/login', {
  phoneNumber: '+233241234567',
  password: 'SecurePass123'
});
useAuthStore.getState().setTokens(data.accessToken, data.refreshToken);

// Post job
await api.post('/jobs', {
  title: '500 tote bags',
  productType: 'Tote Bag',
  sectorTag: 'TEXTILES',
  quantity: 500,
  budgetMinGhs: 2000,
  budgetMaxGhs: 3500,
  deadline: '2024-12-31'
});
```

### Factory Bids

```ts
const { data } = await api.get('/jobs?sectorTag=TEXTILES&page=0&size=20');
const jobId = data.content[0].id;
await api.post(`/jobs/${jobId}/bids`, {
  pricePerUnitGhs: 6.5,
  totalPriceGhs: 3250,
  productionDays: 7,
  deliveryDateEstimate: '2024-12-30'
});
```

### Accept Bid & Pay

```ts
const order = await api.patch(`/bids/${bidId}/accept`);
const payment = await api.post('/payments/initiate', {
  orderId: order.data.id,
  paymentMethod: 'MTN_MOMO'
});
// Open payment.data.authorization_url in WebView or browser
```

### Track Order

```ts
// Poll every 30s or use push notifications
const { data } = await api.get(`/orders/${orderId}`);
```

## Notifications

1. Initialize Firebase Cloud Messaging.
2. Send the FCM token to a future `/users/fcm-token` endpoint.
3. Listen for notification payloads and route to the correct screen.
4. If push fails, Africa's Talking SMS is used automatically as fallback.

## 2G Optimization

- Use pagination (`size=20`).
- Compress images before upload to Cloudinary.
- Cache order status locally with React Query.
- Poll status only when app is foregrounded.

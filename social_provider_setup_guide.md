# Step-by-Step Developer Account Setup Guide (Google & Apple)

Follow these steps to obtain your production **Google Client ID** and **Apple Client ID (Services ID)**.

---

## 🔑 Part 1: How to Get Your Google Client ID

### Step 1: Open Google Cloud Console
1. Go to the **[Google Cloud Console](https://console.cloud.google.com/)**.
2. Log in with your business Google account.
3. Click the project dropdown in the top-left header and select your project (or click **New Project** to create one).

### Step 2: Configure the OAuth Consent Screen (Required First)
Before Google lets you create keys, you must set up your app's consent screen:
1. In the left navigation menu, click **APIs & Services** -> **OAuth consent screen**.
2. Select **User Type** -> **External** and click **Create**.
3. Fill out the required information:
   * **App name**: `MakersHub`
   * **User support email**: *Your support email address*
   * **Developer contact information**: *Your email address*
4. Click **Save and Continue** (skip Scopes and Test Users for now by clicking save on those screens too).
5. Back on the dashboard, click **Publish App** to push it out of testing mode.

### Step 3: Create the OAuth Client ID
1. In the left menu, click **APIs & Services** -> **Credentials**.
2. Click the **`+ Create Credentials`** button at the top, and select **OAuth client ID**.
3. Under **Application type**, select **Web application**.
4. In the **Name** field, enter: `MakersHub Web Client`
5. Under **Authorized JavaScript origins**, click **+ Add URI** and enter:
   * `http://localhost:3000` *(for local frontend development)*
   * `https://your-frontend-domain.com` *(your production frontend website)*
6. Click **Create** at the bottom.
7. A popup will appear. Copy the long string under **Your Client ID** (ends with `.apps.googleusercontent.com`).
8. Paste this value as **`GOOGLE_CLIENT_ID`** in Railway.

---

## 🍎 Part 2: How to Get Your Apple Client ID

*Note: Accessing the Apple Developer Portal requires a paid Apple Developer Account.*

### Step 1: Create an App ID (The Base Identifier)
Apple requires you to link web logins to a primary app registration:
1. Log in to your **[Apple Developer Account](https://developer.apple.com/account/)**.
2. Click **Certificates, Identifiers & Profiles** -> **Identifiers** in the left menu.
3. Click the blue **`+`** icon next to Identifiers.
4. Select **App IDs** and click **Continue**.
5. Choose **App** and click **Continue**.
6. Fill out:
   * **Description**: `MakersHub Main App`
   * **Bundle ID**: Select **Explicit** and type `com.makershub.app` (or your company's bundle name).
7. Scroll down the list of capabilities and check the box next to **Sign In with Apple**.
8. Click **Continue**, then click **Register**.

### Step 2: Create the Services ID (The Web Client ID)
1. Click the blue **`+`** icon next to Identifiers again.
2. Select **Services IDs** and click **Continue**.
3. Fill out:
   * **Description**: `MakersHub Web Auth`
   * **Identifier**: Type `com.makershub.services` (This string is your **Apple Client ID**!).
4. Click **Continue**, then click **Register**.

### Step 3: Configure Web Authentication
1. Click on the **Services ID** you just created (`com.makershub.services`).
2. Check the box next to **Sign In with Apple**, then click **Configure**.
3. Under **Primary App ID**, select the App ID you created in Step 1 (`com.makershub.app`).
4. Under **Web Domains**, enter your web domains (e.g. `makershub.gh` or your frontend host).
5. Under **Return URLs**, enter your frontend callback landing URL.
6. Click **Save**, then click **Done**, and finally click **Save** in the top right.
7. Paste `com.makershub.services` as **`APPLE_CLIENT_ID`** in Railway.

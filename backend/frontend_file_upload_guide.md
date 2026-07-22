# Frontend Developer Guide — File Upload Integration (Cloudinary)

This guide explains how to connect your frontend file pickers (avatar uploads, job attachments) to the backend to get secure URLs for saving in your database forms.

---

## 📡 The API Endpoint

* **URL**: `https://<your-backend-domain>/api/v1/files/upload`
* **Method**: `POST`
* **Authentication**: Requires a valid Bearer Token in the headers:
  `Authorization: Bearer <ACCESS_TOKEN>`
* **Content-Type**: `multipart/form-data`
* **Request Body Parameter**: `file` (must contain the binary file upload)
* **Response Model**: `FileResponse.UploadResponse` (JSON containing `url` field)

---

## 💻 Integration Examples

### Example 1: Standard Javascript (Axios / Fetch)
Use `FormData` to package the local file before sending:

```javascript
import axios from 'axios';

async function uploadLocalFile(fileObject, userToken) {
  // 1. Create a FormData container
  const formData = new FormData();
  formData.append('file', fileObject); // 'file' must match the backend parameter name

  try {
    // 2. Post to the backend upload endpoint
    const response = await axios.post('https://your-backend.up.railway.app/api/v1/files/upload', formData, {
      headers: {
        'Authorization': `Bearer ${userToken}`,
        'Content-Type': 'multipart/form-data'
      }
    });

    // 3. Get the permanent secure URL
    const fileUrl = response.data.url;
    console.log("Uploaded successfully! URL:", fileUrl);
    return fileUrl; // Send this URL to your database save forms
  } catch (error) {
    console.error("Upload failed:", error.response?.data?.message || error.message);
  }
}
```

---

### Example 2: React Component Usage (HTML File Picker)
Here is how to wire it up to a file input in a React component:

```jsx
import React, { useState } from 'react';

function ProfileAvatarUpload({ token, onUploadComplete }) {
  const [uploading, setUploading] = useState(false);

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);
    const uploadedUrl = await uploadLocalFile(file, token);
    setUploading(false);

    if (uploadedUrl) {
      onUploadComplete(uploadedUrl); // Save this url string to user profile state
    }
  };

  return (
    <div>
      <label>Choose Profile Picture:</label>
      <input type="file" accept="image/*" onChange={handleFileChange} disabled={uploading} />
      {uploading && <p>Uploading to Cloudinary...</p>}
    </div>
  );
}
```

---

## 📥 Expected Responses

### Success Response (`200 OK`)
```json
{
  "url": "https://res.cloudinary.com/makershub/image/upload/v172131234/profile_pics/avatar.jpg"
}
```

### Error Responses
* **`400 Bad Request` — Empty File**:
  If no file was selected or sent in the request:
  ```json
  {
    "status": 400,
    "error": "EMPTY_FILE",
    "message": "Uploaded file cannot be empty"
  }
  ```
* **`400 Bad Request` — Cloudinary Missing**:
  If the backend developer hasn't configured the variables on Railway yet:
  ```json
  {
    "status": 400,
    "error": "CLOUDINARY_NOT_CONFIGURED",
    "message": "Cloudinary is not configured. Please set Cloudinary environment variables."
  }
  ```

# Environment Setup Guide

## Initial Setup

1. **Copy the example configuration file:**
   ```bash
   cp local.properties.example local.properties
   ```

2. **Edit `local.properties` with your values:**
   ```properties
   # Update your Android SDK path (usually auto-generated)
   sdk.dir=/Users/yourname/Library/Android/sdk

   # Add your Google OAuth Client ID
   GOOGLE_CLIENT_ID=your-actual-client-id.apps.googleusercontent.com

   # Add your backend API URL
   API_BASE_URL=http://your-ip:8000/
   API_HOST=your-ip
   ```

3. **Sync Gradle:**
   - In Android Studio: Click "Sync Now" or File → Sync Project with Gradle Files

## Getting Your Google Client ID

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Select your project or create a new one
3. Navigate to: APIs & Services → Credentials
4. Find your **OAuth 2.0 Client IDs** for Android
5. Copy the Client ID and paste it into `local.properties`

## Getting Your SHA-1 Fingerprint (For Team Members)

If Google Sign-In isn't working, you need to register your debug keystore SHA-1:

1. **Get your SHA-1:**
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```

2. **Copy the SHA-1 fingerprint** (looks like: `A1:B2:C3:...`)

3. **Register it in Google Cloud Console:**
   - Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
   - Click on the Android OAuth 2.0 Client ID
   - Add your SHA-1 fingerprint
   - Save changes

## Network Configuration

Update `app/src/main/res/xml/network_security_config.xml` if needed:

```xml
<domain includeSubdomains="true">YOUR_API_HOST</domain>
```

Replace `YOUR_API_HOST` with the same value as `API_HOST` in `local.properties`.

## Troubleshooting

- **"BuildConfig cannot be resolved"**: Rebuild project (Build → Rebuild Project)
- **"Google Sign-In failed (code: 10)"**: Your SHA-1 fingerprint is not registered
- **"Connection failed"**: Check `API_BASE_URL` and `network_security_config.xml`
- **Environment variables not working**: Clean project (Build → Clean Project) and rebuild

## Important Notes

- ⚠️ **Never commit `local.properties`** - it contains sensitive information
- ✅ **Always commit `local.properties.example`** - so teammates know what to configure
- 🔄 When IP changes, only update `local.properties` (no code changes needed)
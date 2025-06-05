# Android 15 Storage Permissions Fix

This document explains how to resolve the "Storage permissions denied" issue on Android 15 devices.

## What Changed in Android 15

Android 15 (API level 35) introduced stricter storage permission requirements:
- **Scoped Storage** is now fully enforced
- **MANAGE_EXTERNAL_STORAGE** permission is required for broad file access
- Legacy external storage access is no longer supported

## Solution Steps

### 1. Rebuild and Install the App
After applying the code changes, rebuild and reinstall the app:

```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Grant Permissions
When you run the app, you'll see:

1. **Initial Permission Dialog**: Grant basic storage permissions
2. **"Grant Storage Permissions" Button**: If you see this button, tap it
3. **System Settings**: You'll be taken to "All files access" settings
4. **Enable Full Access**: Toggle on "Allow access to manage all files"

### 3. Alternative Permission Granting (if automatic doesn't work)

#### Method 1: Through Android Settings
1. Go to **Settings** → **Apps** → **Llama Android**
2. Tap **Permissions**
3. Tap **Files and media** → **Allow access to manage all files**

#### Method 2: Using ADB (for developers)
```bash
adb shell pm grant com.example.llama android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.example.llama android.permission.MANAGE_EXTERNAL_STORAGE
```

## Troubleshooting

### Still seeing "Storage permissions denied"?

1. **Check App Info**: Long-press the app icon → App info → Permissions
2. **Clear App Data**: If permissions are stuck, clear app data and grant permissions again
3. **Restart Device**: Sometimes a reboot helps permissions take effect

### Files not found after granting permissions?

1. Make sure your `.gguf` files are in accessible locations:
   - `/storage/emulated/0/Download/`
   - `/storage/emulated/0/Documents/`
   - Any folder you create in internal storage

2. Use the "Scan for GGUF Files" button after placing files

## Code Changes Made

1. **Enhanced Permission Handling**: Added support for `MANAGE_EXTERNAL_STORAGE`
2. **Android 13+ Compatibility**: Added granular media permissions
3. **Better UI Feedback**: Added permission request button and status messages
4. **Improved File Scanning**: More efficient scanning with proper permission checks

## File Placement Recommendations

For best compatibility with Android 15:

1. **App-specific directory** (always accessible):
   `/Android/data/com.example.llama/files/`

2. **Public Downloads** (with permissions):
   `/storage/emulated/0/Download/`

3. **Documents folder** (with permissions):
   `/storage/emulated/0/Documents/`

The app will now properly request and handle these permissions on Android 15! 
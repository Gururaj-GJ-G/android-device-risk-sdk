# How to Build the Device Risk SDK AAR
## Step-by-step for Android Studio (no coding experience needed)

---

## WHAT YOU ARE BUILDING
An AAR file — this is the packaged SDK you hand to a developer integrating it into their app.
It takes about 10 minutes once Android Studio is set up.

---

## STEP 1 — Open the project in Android Studio

1. Open Android Studio
2. Click "Open" (NOT "New Project")
3. Navigate to this folder: android-device-risk-sdk
4. Click OK
5. Wait for Gradle to sync (progress bar at the bottom — takes 2–3 min first time)
   When you see "BUILD SUCCESSFUL" in the bottom panel, you're ready.

---

## STEP 2 — Build the AAR file

1. In the top menu click: Build → Make Project
   Wait for it to finish (green tick at bottom)

2. Then click: Build → Build Bundle(s) / APK(s) → Build APK(s)
   This generates the AAR.

---

## STEP 3 — Find the AAR file

After building, the AAR file is at this path inside your project folder:

    sdk / build / outputs / aar / sdk-release.aar

Rename it to:
    android-device-risk-sdk-1.0.aar

This is the file you distribute to whoever is integrating the SDK.

---

## STEP 4 — What the developer does with it

They put android-device-risk-sdk-1.0.aar into their app's libs/ folder
and follow the SDK Integration Guide.

---

## BEFORE YOU SHARE THE AAR

Replace the API key placeholder in DeviceRiskSdk.kt if needed.
Each integrator uses their own key — this is handled in DeviceRiskConfig when they call init().

---

## IF YOU SEE ERRORS

Most common error: "SDK location not found"
Fix: File → Project Structure → SDK Location → set your Android SDK path

Second most common: "Gradle sync failed"
Fix: File → Invalidate Caches → Invalidate and Restart

Still stuck? Open an issue on the GitHub repository.

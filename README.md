# RedOS Launcher

Native Android home-screen launcher with an iOS-inspired black/red interface.

## Features
- Android HOME/default-launcher entry point
- Installed-app discovery and launch
- Home screen app shortcuts and dock
- App drawer with live search
- Swipe-up gesture to open the app drawer
- Dark black/red styling

## Build the APK online (no Android Studio required)

1. Create a free account at https://github.com/ if you do not already have one.
2. Create a new **public** repository named `RedOSLauncher`.
3. Upload the contents of this project ZIP into the repository (not the ZIP file itself).
4. Open the repository's **Actions** tab.
5. Select **Build RedOS APK** and press **Run workflow** (or wait for the automatic build after pushing to `main`).
6. When the workflow finishes with a green check, open the run and download the **RedOS-debug-apk** artifact.
7. Extract the downloaded artifact ZIP; it contains `app-debug.apk`. Transfer it to your Android phone and install it.
8. In Android settings, choose **RedOS Launcher** as the default Home app.

The GitHub Actions workflow builds the APK in the cloud. This project ZIP contains source code and the workflow, not a prebuilt APK.

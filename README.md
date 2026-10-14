# manu novel — source project

This project contains two independent editions:

- `android/`: native Android app source using Kotlin + Jetpack Compose.
- `web/`: responsive offline-first browser/PWA edition for Chrome and other modern browsers.

## Native Android build
1. Install Android Studio (current stable) and Android SDK 35.
2. Open the `android` folder in Android Studio and let Gradle sync.
3. Run on an emulator or Android phone with USB debugging enabled.
4. To build an APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**. The debug APK is usually at `android/app/build/outputs/apk/debug/app-debug.apk`.

The source has not been built in this environment, so please treat the first Android Studio Gradle sync/build as the verification step. The native MVP includes a Compose library/reader, original sample stories, persistent local progress/favourites/bookmarks/settings, and PDF import using Android's document picker plus native `PdfRenderer` page display. No account, backend, or paid API is used.

## Web/PWA edition
Open `web/index.html` for a quick local preview, or serve the `web` directory over localhost/HTTPS (needed for service-worker/PWA install). For example, from the `web` directory run `python -m http.server 8000`, then open `http://localhost:8000`. For public hosting, upload the contents of `web/` to any static host (GitHub Pages, Cloudflare Pages, Netlify, etc.). Browser storage is local to that browser/device; it does not sync with Android app data.

## Privacy
Imported books and reading data are stored locally on the device/browser. The app contains no upload or account code. Clearing app/browser storage may erase locally saved data; export/backup is not included in this MVP.

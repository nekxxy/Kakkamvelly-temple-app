# കക്കംവെള്ളി ക്ഷേത്രം — Android & iOS App

Native mobile app for **Kakkamvelly Sreekrishna Temple**, Purameri, Kozhikode, Kerala.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Compose Multiplatform 1.7.3 |
| Architecture | MVVM + Repository pattern |
| Platforms | Android (API 24+) + iOS 15+ |
| HTTP | Ktor 3.0.3 |
| DI | Koin 4.0.0 |
| Images | Coil 3.0.4 |
| Notifications | Firebase Cloud Messaging |
| DateTime | kotlinx.datetime 0.6.2 |

## Features

- 🔴 Live darshan open/closed status (IST timezone, NOAA-precise)
- ⏱️ Real-time countdown to next festival (23 festivals 2026–2029)
- 🌤 Live weather (Open-Meteo API — no key required)
- 🌅 NOAA sunrise/sunset calculator (Kozhikode coordinates)
- 🌙 Moon phase indicator
- 🍛 Annadhanam monthly tracker (next first-Sunday)
- 📅 Full festival calendar 2026–2029
- 🕐 Pooja schedule with live highlight
- 🖼 Photo gallery with lightbox
- 📍 Location, directions, transport info
- 💧 Kulam renovation progress tracker
- 🔔 Push notifications (festivals + Annadhanam)
- 🌐 Malayalam / English language switch

## Setup

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Kotlin 2.0.21
- Xcode 16+ (for iOS)

### 1. Clone
```bash
git clone https://github.com/nekxxy/Kakkamvelly-temple-app
cd Kakkamvelly-temple-app
```

### 2. Firebase Setup (for push notifications)
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create project `KakkamvellyTemple`
3. Add Android app: package `page.kakkamvellytemple.app`
4. Download `google-services.json` → replace `androidApp/google-services.json`

### 3. Build Android
```bash
./gradlew :androidApp:assembleDebug
# APK: androidApp/build/outputs/apk/debug/
```

### 4. Build iOS
```bash
./gradlew :shared:iosArm64Binaries
# Open iosApp/iosApp.xcodeproj in Xcode
```

## Project Structure
```
├── shared/                          # 90% of code — shared across platforms
│   └── src/
│       ├── commonMain/kotlin/
│       │   └── page/kakkamvellytemple/app/
│       │       ├── data/
│       │       │   ├── model/       # Data classes
│       │       │   ├── remote/      # Ktor API clients
│       │       │   └── repository/  # Repositories + StaticData
│       │       ├── presentation/
│       │       │   ├── ui/
│       │       │   │   ├── screen/  # All 6 screens
│       │       │   │   ├── component/ # Reusable Composables
│       │       │   │   ├── theme/   # Material 3 theme
│       │       │   │   └── App.kt   # Root navigation
│       │       │   └── viewmodel/   # MVVM ViewModels
│       │       └── util/            # ISTClock, NOAA, MoonPhase
│       ├── androidMain/             # Android-specific
│       └── iosMain/                 # iOS-specific
├── androidApp/                      # Android app shell
│   └── src/main/
│       ├── kotlin/.../
│       │   ├── MainActivity.kt
│       │   ├── KVTApplication.kt
│       │   └── notification/KVTFirebaseService.kt
│       ├── AndroidManifest.xml
│       └── res/
└── .github/workflows/build.yml      # CI: auto-build on push
```

## CI/CD

GitHub Actions builds automatically on every push to `main`:
- Debug APK → downloadable artifact (30 days)
- Release AAB → downloadable artifact (30 days)

## Website
https://kakkamvellytemple.page

## Play Store
Package: `page.kakkamvellytemple.app`

## License
© Kakkamvelly Sreekrishna Temple, Purameri, Kozhikode, Kerala

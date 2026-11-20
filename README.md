# WalletQ — Personal and Household Money Management

WalletQ is an offline-first, privacy-respecting Android application developed by SuBZeRo. It provides personal and family financial tracking without mandatory accounts, cloud services, advertisements, or telemetry.

## Key Principles
- **Privacy First**: No internet permission (`android.permission.INTERNET` is not declared).
- **Zero Monetization**: Free forever with no ads, tracking, or paid features.
- **Data Integrity**: Monetary values are tracked in integer minor units to eliminate floating-point rounding errors.
- **Offline Storage**: Local SQLite database managed with AndroidX Room.

## Technology Stack
- **Language**: Kotlin 2.0.21 (JVM 17)
- **UI**: Jetpack Compose with Material 3 design tokens
- **Architecture**: MVVM with Clean Architecture principles
- **Persistence**: Room Database (version 1) and DataStore Preferences
- **Dependency Injection**: Dagger Hilt
- **Navigation**: Jetpack Navigation Compose (5 top-level destinations: Home, Activity, Accounts, Reports, More)

## How to Build
```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Debug outputs: `app/build/outputs/apk/debug/app-debug.apk`

## Documentation
- [Architecture](docs/ARCHITECTURE.md)
- [Financial Accounting Rules](docs/FINANCIAL_RULES.md)
- [Privacy Policy](docs/PRIVACY.md)
- [Roadmap](docs/ROADMAP.md)
- [Implementation Checklist](docs/IMPLEMENTATION_CHECKLIST.md)

Developer: SuBZeRo  
License: Apache License 2.0  

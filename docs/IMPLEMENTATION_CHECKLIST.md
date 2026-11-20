# WalletQ Phase 1 Implementation Checklist

## 1. Development Environment Inspection & Baseline
- [x] Inspect workspace and directory contents (`G:\WalletQ` root with `icons/icon.png` and `icons/logo.png`).
- [x] Verify Java toolchain: OpenJDK 17 (Temurin 17.0.20.1+1).
- [x] Verify Android SDK platform: `android-36` and build tools `36.0.0` installed.
- [x] Discover and configure Gradle 8.13 wrapper (`gradlew`, `gradlew.bat`).
- [x] Initialize Git repository (`git init`) and create comprehensive `.gitignore`.

## 2. Project Configuration & Build System
- [x] Establish Gradle Kotlin DSL project (`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`).
- [x] Centralize all dependencies in `gradle/libs.versions.toml`.
- [x] Configure mutually compatible stable versions (AGP 8.8.2, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Hilt 2.51.1, Room 2.6.1, DataStore 1.1.2, Compose BOM 2024.12.01).
- [x] Configure Java 17 toolchain (`jvmToolchain(17)`).
- [x] Configure `minSdk = 26` (Android 8.0 Oreo), `compileSdk = 36`, `targetSdk = 36`.
- [x] Implement memory-optimized compiler configuration (`-Xms128m`, `-Xmx1536m`, SerialGC, in-process compilation) for 8GB development workstation.

## 3. Financial Architecture & Domain Foundation
- [x] Implement `Currency` model supporting ISO-4217 metadata (INR, USD, EUR, GBP, AED, JPY).
- [x] Explicit handling of variable currency decimal places (JPY = 0, INR/USD/EUR/GBP/AED = 2).
- [x] Implement `Money` value object backed by exact 64-bit integer `minorUnits: Long`.
- [x] Forbid cross-currency arithmetic without explicit conversion via runtime assertions.
- [x] Implement controlled `BigDecimal` scaling with Banker's rounding (`RoundingMode.HALF_EVEN`).
- [x] Define `OwnershipType` (PERSONAL, HOUSEHOLD) for local dual ownership.
- [x] Define `AccountType` (CASH, BANK, SAVINGS, CREDIT_CARD, INVESTMENT, OTHER).
- [x] Implement `Profile` and `Account` domain entities.

## 4. Room Database Foundation
- [x] Configure `WalletQDatabase` (version 1, file `walletq.db`).
- [x] Implement `Converters` for `OwnershipType` and `AccountType`.
- [x] Define `CurrencyEntity`, `ProfileEntity`, and `AccountEntity`.
- [x] Enforce SQLite Foreign Key constraints via `DatabaseCallback` (`PRAGMA foreign_keys = ON;`).
- [x] Pre-populate default ISO currencies and default Personal profile on database creation.
- [x] Implement reactive Room DAOs (`ProfileDao`, `AccountDao`, `CurrencyDao`).

## 5. DataStore Preferences Foundation
- [x] Define `PreferencesRepository` interface.
- [x] Implement `PreferencesRepositoryImpl` utilizing AndroidX DataStore Preferences.
- [x] Manage `ThemeMode` (SYSTEM, LIGHT, DARK), dynamic color toggle, default reporting currency, and active profile ID.

## 6. Dependency Injection (Hilt)
- [x] Implement `DatabaseModule` providing `WalletQDatabase` and DAOs.
- [x] Implement `DataStoreModule` binding `PreferencesRepository`.
- [x] Implement `DispatchersModule` with `@IoDispatcher`, `@DefaultDispatcher`, and `@MainDispatcher`.
- [x] Annotate `WalletQApplication` with `@HiltAndroidApp` and `MainActivity` with `@AndroidEntryPoint`.

## 7. Material 3 Design System
- [x] Implement fintech palette: Blue, Teal, and Neutral color tokens in `Color.kt`.
- [x] Configure typography tokens in `Type.kt`.
- [x] Configure shape tokens in `Shape.kt`.
- [x] Configure standardized spacing tokens in `Dimensions.kt`.
- [x] Implement `WalletQTheme` supporting Light mode, Dark mode, system default, and dynamic colors.
- [x] Build reusable components: `WalletQTopBar`, `WalletQCard`, and `WalletQEmptyState`.

## 8. Presentation & Navigation Shell
- [x] Configure Navigation Compose with 5 top-level destinations: Home, Activity, Accounts, Reports, More.
- [x] Build bottom `NavigationBar` with Material 3 icons and indicator highlighting.
- [x] Implement `HomeScreen` with net worth summary card, architecture badges, and empty state.
- [x] Implement clean placeholder screens: `ActivityScreen`, `AccountsScreen`, `ReportsScreen`.
- [x] Implement interactive `MoreScreen` with live theme switching, dynamic color toggle, currency picker, and privacy audit.

## 9. Security & Privacy Guarantees
- [x] Verify zero `android.permission.INTERNET` in `AndroidManifest.xml`.
- [x] Exclude database from automated cloud backups in `backup_rules.xml` and `data_extraction_rules.xml`.
- [x] Zero third-party telemetry, tracking, or advertising SDKs.

## 10. Assets & Documentation
- [x] Generate density mipmap launcher icons (`ic_launcher.png`, `ic_launcher_round.png`) from high-res `icons/icon.png`.
- [x] Create `README.md`.
- [x] Create `LICENSE` (Apache License 2.0).
- [x] Create `CONTRIBUTING.md`.
- [x] Create `docs/ARCHITECTURE.md`.
- [x] Create `docs/FINANCIAL_RULES.md`.
- [x] Create `docs/PRIVACY.md`.
- [x] Create `docs/ROADMAP.md`.
- [x] Create `docs/IMPLEMENTATION_CHECKLIST.md`.

## 11. Testing & Verification
- [x] Compile and build project with memory-safe JVM configuration.
- [x] Run unit test suite (`MoneyTest`, `CurrencyTest`, `NavigationDestinationsTest`, `ThemeTest`, `OwnershipAndAccountTest`).

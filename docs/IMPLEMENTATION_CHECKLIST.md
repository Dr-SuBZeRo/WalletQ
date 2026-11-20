# WalletQ Phase 1 Implementation Checklist

## 1. Development Environment Inspection & Baseline
- [x] Inspect workspace and directory contents (`G:\WalletQ` root with `icons/icon.png` and `icons/logo.png`).
- [x] Verify Java toolchain: OpenJDK 17 (Temurin 17.0.20.1+1).
- [x] Verify Android SDK platform: `android-36` and build tools `36.0.0` installed.
- [x] Discover and configure Gradle 8.13 wrapper (`gradlew`, `gradlew.bat`).
- [x] Initialize Git repository (`git init`) and create comprehensive `.gitignore`.
- [x] Link and verify remote repository: `https://github.com/Dr-SuBZeRo/WalletQ.git`.

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
- [x] Implement overflow-safe operations (`Math.addExact`, `Math.subtractExact`, `Math.negateExact`, `Math.multiplyExact`).
- [x] Implement scalar integer division with Banker's rounding (`Money.div(Long)`).
- [x] Implement precise ratio calculation (`Money.div(Money)`).
- [x] Implement cross-currency conversion with Banker's rounding (`Money.convertTo`).
- [x] Implement `ExchangeRate` domain model.

## 4. Household Relational Architecture
- [x] Define `Household` domain model.
- [x] Define `HouseholdRole` enum (`OWNER`, `ADMIN`, `MEMBER`, `VIEWER`).
- [x] Define `HouseholdMember` junction model supporting multi-household membership.
- [x] Define `OwnershipType` (PERSONAL, HOUSEHOLD).
- [x] Define `AccountType` (CASH, BANK, SAVINGS, CREDIT_CARD, INVESTMENT, OTHER).
- [x] Support personal and household accounts via `householdId` and `ownershipType`.

## 5. Room Database Foundation (Schema V2)
- [x] Configure `WalletQDatabase` (version 2, file `walletq.db`).
- [x] Define `ProfileEntity`, `HouseholdEntity`, `HouseholdMemberEntity`, `AccountEntity`, `CurrencyEntity`, `ExchangeRateEntity`.
- [x] Enforce SQLite Foreign Key constraints via `DatabaseCallback` (`PRAGMA foreign_keys = ON;`).
- [x] Synchronous atomic baseline pre-population within `onCreate` transaction.
- [x] Implement deterministic `MIGRATION_1_2` without destructive fallback.
- [x] Remove `.fallbackToDestructiveMigration()` from `DatabaseModule`.
- [x] Implement reactive Room DAOs (`ProfileDao`, `HouseholdDao`, `AccountDao`, `CurrencyDao`, `ExchangeRateDao`).

## 6. DataStore Preferences Foundation
- [x] Define `PreferencesRepository` interface.
- [x] Implement `PreferencesRepositoryImpl` utilizing AndroidX DataStore Preferences.
- [x] Manage `ThemeMode` (SYSTEM, LIGHT, DARK), dynamic color toggle, default reporting currency, and active profile ID.

## 7. Dependency Injection (Hilt)
- [x] Implement `DatabaseModule` providing `WalletQDatabase`, DAOs (Profile, Household, Account, Currency, ExchangeRate).
- [x] Implement `DataStoreModule` binding `PreferencesRepository`.
- [x] Implement `DispatchersModule` with `@IoDispatcher`, `@DefaultDispatcher`, and `@MainDispatcher`.
- [x] Annotate `WalletQApplication` with `@HiltAndroidApp` and `MainActivity` with `@AndroidEntryPoint`.

## 8. Material 3 Design System
- [x] Implement fintech palette: Blue, Teal, and Neutral color tokens in `Color.kt`.
- [x] Configure typography tokens in `Type.kt`.
- [x] Configure shape tokens in `Shape.kt`.
- [x] Configure standardized spacing tokens in `Dimensions.kt`.
- [x] Implement `WalletQTheme` supporting Light mode, Dark mode, system default, and dynamic colors.
- [x] Build reusable components: `WalletQTopBar`, `WalletQCard`, and `WalletQEmptyState`.

## 9. Presentation & Navigation Shell
- [x] Configure Navigation Compose with 5 top-level destinations: Home, Activity, Accounts, Reports, More.
- [x] Build bottom `NavigationBar` with Material 3 icons and indicator highlighting.
- [x] Implement `HomeScreen` with net worth summary card, architecture badges, and empty state.
- [x] Implement clean placeholder screens: `ActivityScreen`, `AccountsScreen`, `ReportsScreen`.
- [x] Implement interactive `MoreScreen` with live theme switching, dynamic color toggle, currency picker, and privacy audit.

## 10. Security & Privacy Guarantees
- [x] Verify zero `android.permission.INTERNET` in `AndroidManifest.xml`.
- [x] Exclude database from automated cloud backups in `backup_rules.xml` and `data_extraction_rules.xml`.
- [x] Zero third-party telemetry, tracking, or advertising SDKs.

## 11. Testing & Verification
- [x] Unit test suite covering money math, overflow safety, conversions, division, precision, household relations, and themes.
- [x] Execute `testDebugUnitTest` — 100% passing.
- [x] Execute `assembleDebug` — APK generated successfully.

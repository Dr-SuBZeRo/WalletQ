<div align="center">

  <h1>WalletQ</h1>
  <p><strong>Personal finance. Household finance. Complete control.</strong></p>
  <p>A free, open-source Android finance manager built with Kotlin and Jetpack Compose.</p>

  <p>
    <a href="https://github.com/Dr-SuBZeRo/WalletQ">
      <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
    </a>
    <a href="https://kotlinlang.org/">
      <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
    </a>
    <a href="https://developer.android.com/jetpack/compose">
      <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
    </a>
    <a href="https://www.apache.org/licenses/LICENSE-2.0">
      <img src="https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge" alt="Apache 2.0">
    </a>
  </p>
  <p>
    <a href="#-overview">Overview</a> •
    <a href="#-principles">Principles</a> •
    <a href="#-features">Features</a> •
    <a href="#-technology">Technology</a> •
    <a href="#-build">Build</a> •
    <a href="#-roadmap">Roadmap</a> •
    <a href="#-documentation">Documentation</a>
  </p>
</div>

---

## 💼 Overview

**WalletQ** is a privacy-focused personal and household money management application for Android, developed by **SuBZeRo**.

It is being built to help people understand and manage their finances in one place: organize accounts, record income and expenses, track transfers, plan budgets, monitor savings, and review financial activity.

WalletQ is designed around local data ownership and offline-first operation. Essential finance workflows will not depend on an online account, a cloud service, or a bank connection.

> **Our promise:** Your finances belong to you. WalletQ is free to use, keeps core data on your device, and is developed as an open-source project.

## ✨ Principles

<table>
  <tr>
    <td width="50%">
      <h3>🔒 Privacy first</h3>
      No mandatory registration, phone number, analytics, advertising SDKs, or financial tracking.
    </td>
    <td width="50%">
      <h3>📴 Offline first</h3>
      Core finance records and calculations are designed to work locally without an internet connection.
    </td>
  </tr>
  <tr>
    <td>
      <h3>🧮 Financial accuracy</h3>
      Monetary values use integer minor units and controlled decimal arithmetic rather than floating-point money calculations.
    </td>
    <td>
      <h3>🗂️ Data ownership</h3>
      Local storage and user-controlled backup are central design goals. No mandatory cloud synchronization.
    </td>
  </tr>
  <tr>
    <td>
      <h3>👨‍👩‍👧‍👦 Personal + household</h3>
      The data model supports individual profiles and household-owned financial records.
    </td>
    <td>
      <h3>🆓 Free and open source</h3>
      No advertisements, subscriptions, in-app purchases, or paid feature tiers.
    </td>
  </tr>
</table>

## 🚀 Features

WalletQ is under active development. The status below distinguishes implemented foundations from planned application functionality.

### Implemented foundations

- [x] Android project and Jetpack Compose application shell
- [x] Material 3 light, dark, and system theme foundation
- [x] Five top-level destinations: Home, Activity, Accounts, Reports, and More
- [x] Room database foundation with schema version 2
- [x] Personal profile, household, household membership, and account data models
- [x] Currency metadata for INR, USD, EUR, GBP, AED, and JPY
- [x] Historical exchange-rate data model
- [x] Integer-minor-unit money model and precision-aware currency handling
- [x] Pure Kotlin ledger domain engine for income, expenses, and transfers
- [x] Domain-level reversal and replacement transaction models
- [x] Unit tests for money, currency, ownership, navigation, and ledger rules

### Planned finance features

- [ ] Persistent transaction and ledger database tables
- [ ] Add, view, search, and filter income and expenses
- [ ] Transfers between accounts, including cross-currency transfers
- [ ] Account balance history and reconciliation
- [ ] Custom income and expense categories
- [ ] Personal and household finance views
- [ ] Monthly budgets and spending limits
- [ ] Savings goals
- [ ] Recurring transactions and bill reminders
- [ ] Financial reports and spending insights
- [ ] Standard and encrypted local backup and restore

Features will be marked complete as they are implemented and verified.

## 💱 Multi-currency design

WalletQ is being designed for multiple currencies from the beginning.

| Currency | Code | Minor-unit precision |
|---|---:|---:|
| Indian Rupee | INR | 2 |
| US Dollar | USD | 2 |
| Euro | EUR | 2 |
| British Pound | GBP | 2 |
| UAE Dirham | AED | 2 |
| Japanese Yen | JPY | 0 |

- INR is the default reporting currency.
- Accounts retain their own currency.
- Amounts in different currencies are never added directly.
- Conversions use an explicit exchange rate and documented rounding.
- The applied rate is intended to be preserved with the financial event so historical records do not change when rates change.

Live exchange-rate retrieval is not part of the current offline foundation.

## 🧾 Financial integrity

WalletQ uses a ledger-oriented domain design.

- **Income** creates a positive account entry.
- **Expense** creates a negative account entry.
- **Transfer** creates corresponding source and destination entries.
- **Reversal** creates compensating entries rather than deleting posted history.
- **Replacement** preserves the original event, reversal, and corrected event.

For a same-currency transfer, the source and destination movements must conserve value. Cross-currency transfers preserve each side in its own currency and record the conversion relationship.

## 🛠️ Technology

| Area | Technology |
|---|---|
| Platform | Android |
| Language | Kotlin |
| UI | Jetpack Compose |
| Design system | Material 3 |
| Architecture | MVVM + Clean Architecture principles |
| Local database | Room / SQLite |
| Preferences | DataStore |
| Dependency injection | Hilt |
| Asynchronous work | Kotlin Coroutines and Flow |
| Background work | WorkManager |
| Navigation | Navigation Compose |
| Unit testing | JUnit |

### Current project configuration

| Setting | Value |
|---|---|
| Application ID | `com.walletq` |
| Minimum Android SDK | 26 |
| Compile SDK | 36 |
| Target SDK | 36 |
| JDK | 17 |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.8.2 |
| Kotlin | 2.0.21 |
| Room schema | Version 2 |

Dependency versions are managed centrally in `gradle/libs.versions.toml`.

## 🏗️ Build

### Requirements

- Windows, macOS, or Linux development environment
- JDK 17
- Android SDK with the required platform and build tools
- Git

### Clone the repository

```bash
git clone https://github.com/Dr-SuBZeRo/WalletQ.git
cd WalletQ
```

### Build a debug APK

**Windows PowerShell:**

```powershell
.\gradlew.bat assembleDebug
```

**macOS / Linux:**

```bash
./gradlew assembleDebug
```

### Run unit tests

**Windows PowerShell:**

```powershell
.\gradlew.bat testDebugUnitTest
```

**macOS / Linux:**

```bash
./gradlew testDebugUnitTest
```

### Run Android lint

**Windows PowerShell:**

```powershell
.\gradlew.bat lintDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 🗺️ Roadmap

WalletQ is being developed in small, testable milestones.

| Milestone | Scope | Status |
|---|---|---|
| Phase 1 | Project foundation, app shell, initial database | Complete |
| Foundation audit | Household relationships, currency safeguards, migration V2 | Complete |
| Phase 2A | Pure Kotlin financial ledger engine | Complete |
| Phase 2B | Room transaction persistence and repository layer | In progress |
| Phase 2C | Transaction entry and activity UI | Planned |
| Phase 3 | Account management and reconciliation | Planned |
| Phase 4 | Dashboard and financial overview | Planned |
| Phase 5 | Categories and budgets | Planned |
| Phase 6 | Currency conversion management | Planned |
| Phase 7 | Household finance workflows | Planned |
| Phase 8 | Analytics and reports | Planned |
| Phase 9 | Savings goals and recurring transactions | Planned |
| Phase 10 | Standard and encrypted backup / restore | Planned |
| Release | Quality, accessibility, security, and release preparation | Planned |

Roadmap status reflects the project implementation checkpoints; planned features are not represented as available functionality.

## 🛡️ Privacy and permissions

WalletQ's current Android manifest does not declare the `INTERNET` permission. The project currently has no advertising or analytics SDKs.

Financial data is stored locally in the application's database. Backup and restore capabilities are planned as explicit, user-controlled workflows.

Please review [PRIVACY.md](docs/PRIVACY.md) for the project's privacy design and implementation status.

## 📚 Documentation

| Document | Description |
|---|---|
| [Architecture](docs/ARCHITECTURE.md) | Application structure and architectural decisions |
| [Database Schema](docs/DATABASE_SCHEMA.md) | Database design and persistence details |
| [Financial Rules](docs/FINANCIAL_RULES.md) | Money precision, accounting, and ledger rules |
| [Privacy](docs/PRIVACY.md) | Privacy principles and data handling |
| [Roadmap](docs/ROADMAP.md) | Development milestones |
| [Implementation Checklist](docs/IMPLEMENTATION_CHECKLIST.md) | Task-level implementation progress |
| [Contributing](CONTRIBUTING.md) | Development and contribution guidelines |

## 🤝 Contributing

Contributions, bug reports, and practical suggestions are welcome.

Before submitting changes:

1. Read the architecture and financial rules documentation.
2. Keep financial calculations deterministic and independently testable.
3. Add or update tests for behaviour changes.
4. Preserve database records through explicit migrations.
5. Avoid dependencies that introduce tracking, advertisements, or unnecessary network access.
6. Keep pull requests focused and explain the reasoning behind architectural changes.

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## 📄 License

WalletQ is distributed under the **Apache License 2.0**. See [LICENSE](LICENSE) for the full license text.

## 👨‍💻 Developer

<div align="center">
  <h3>SuBZeRo</h3>
  <p>Independent developer • Open-source enthusiast</p>
  <a href="https://github.com/Dr-SuBZeRo">
    <img src="https://img.shields.io/badge/GitHub-Dr--SuBZeRo-181717?style=for-the-badge&logo=github&logoColor=white" alt="SuBZeRo on GitHub">
  </a>
  <p><strong>WalletQ — Your money. Your data. Your control.</strong></p>
</div>

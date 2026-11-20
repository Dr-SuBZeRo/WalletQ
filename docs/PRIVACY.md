# WalletQ Privacy & Data Ownership Policy

**Application:** WalletQ  
**Developer:** SuBZeRo  
**Target:** 100% Privacy by Design  

---

## 1. Zero Network Connectivity

WalletQ does not declare, request, or use the `android.permission.INTERNET` permission in `AndroidManifest.xml`.
- The application is physically unable to communicate with remote servers, APIs, or cloud services.
- Financial data cannot be leaked, exfiltrated, or transmitted across the internet.

---

## 2. No Tracking, Telemetry, or Analytics

WalletQ contains:
- **No Google Analytics / Firebase Analytics**
- **No Crashlytics or remote crash reporters**
- **No Advertising SDKs (No AdMob, Unity, etc.)**
- **No Third-party tracking or attribution beacons**
- **No Telemetry daemons**

---

## 3. No Mandatory Registration or Identity Collection

- Users are never asked to create an account, log in, or authenticate with an email.
- No phone numbers, identities, or contact lists are collected, requested, or read.
- The app operates with complete utility immediately upon installation.

---

## 4. Local Database Storage

- All user data, account records, balances, and preferences reside solely on the local device filesystem within the app-private directory.
- Database file: `walletq.db` (SQLite via AndroidX Room).

---

## 5. Cloud Backup Exclusion & User-Controlled Backups

In `backup_rules.xml` and `data_extraction_rules.xml`, `walletq.db` is explicitly excluded from unencrypted or automated cloud backups to Google Drive.
- Future backup/export functionality will be completely manual, user-initiated, local, and encrypted.
- The user retains 100% ownership and sovereignty over their financial records.

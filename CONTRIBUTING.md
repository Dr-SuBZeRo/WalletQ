# Contributing Guidelines for WalletQ

Thank you for contributing to WalletQ, created by SuBZeRo.

## Core Rules
1. Never introduce network-related permissions or dependencies.
2. Never add tracking, analytics, or advertisement libraries.
3. Financial amounts must use integer minor units (`Long`) and avoid floating-point types (`Float`, `Double`).
4. Keep domain business logic free of Android SDK imports.

## Development Setup
- JDK: OpenJDK 17
- Compile / Target SDK: 36
- Minimum SDK: 26
- Build tool: Gradle Wrapper with memory settings configured in `gradle.properties`

## Workflow
- Create descriptive feature or fix branches.
- Run `.\gradlew.bat testDebugUnitTest` prior to submitting changes.
- Ensure all new financial calculations include comprehensive unit tests.

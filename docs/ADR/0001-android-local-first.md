# ADR 0001: Android-first local-first application

- Status: Accepted
- Date: 2026-10-06

## Context
Trace needs to observe personal app-use behavior and help users review time allocation and learning activity. This data is sensitive and collection feasibility is uncertain, especially for per-video recognition.

## Decision
- Build the first client as a native Android app using Kotlin and Jetpack Compose.
- Keep behavior data on-device by default.
- Begin with app-level usage sessions and manual/share-based content capture.
- Treat AccessibilityService-based content recognition as an optional experiment, isolated behind an adapter.
- Do not introduce a backend, account system, remote AI, telemetry, or platform credentials in the MVP.
- Keep domain calculations independent from Android APIs where practical.

## Consequences
Positive:
- Lower infrastructure cost and reduced data exposure.
- Faster validation of Android-specific collection behavior.
- Core timeline can work without a network connection.

Trade-offs:
- No multi-device sync in the MVP.
- Users must manage local backup/export themselves.
- Device/OEM behavior and accessibility-node availability require real-device testing.
- Some analytics will remain incomplete when optional recognition is disabled.

## Revisit when
- User research demonstrates a clear need for sync or remote backup.
- Data collection accuracy and permission acceptance have been measured.
- A concrete product requirement cannot be satisfied locally.

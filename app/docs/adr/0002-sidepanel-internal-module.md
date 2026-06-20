# ADR-0002: SidePanel as in-app internal module

## Status

Accepted

## Context

The app needs a session browser sidebar and a settings sheet with provider picker and API key management. This feature was added to provide the drawer navigation and credential configuration surface. OpenCore uses a single `:app` Gradle module; feature boundaries are enforced by package structure and Kotlin `internal` visibility, not separate Gradle libraries.

## Decision

1. Place SidePanel under `io.github.bengidev.opencore.sidepanel/` with layered sub-packages: `domain/`, `application/`, `infrastructure/`, `presenter/`, `theme/`.
2. Default types to `internal`; expose only `SidePanelFacade`, `SidePanelDrawer`, and `SidePanelSettingsSheetRoute` to the app shell.
3. Apply GoF patterns: Command (`SidePanelIntent`), Reducer (`SidePanelReducer`), Repository (`CredentialStore`, `SessionRepository`), Facade (`SidePanelFacade`).
4. Reuse `OpenCorePalette` from the onboarding theme via typealias (`SidePanelPalette`); no new color tokens.
5. Persist API keys via `EncryptedSharedPreferences` (androidx.security:security-crypto) — the Android equivalent of iOS Keychain for small secrets. An in-memory double is provided for hermetic tests.
6. TDD: unit tests under `app/src/test/.../sidepanel/`.
7. Wire the drawer into Home via `ModalNavigationDrawer`; the sidebar button opens the drawer (previously a no-op intent).

## Consequences

- No cross-module Gradle coordination; compiler enforces boundaries via `internal`.
- API keys are encrypted at rest; the in-memory double keeps tests hermetic.
- The Home `SidebarTapped` intent remains a no-op in the reducer (drawer open is handled in the Compose layer); it can be removed in a later cleanup.

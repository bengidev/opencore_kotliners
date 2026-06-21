# Home Context

| | |
|---|---|
| **Context** | Home feature — welcome landing screen |
| **Package** | `io.github.bengidev.opencore.home` |
| **Module** | Internal module inside `:app` |

Welcome-state home screen with particle orb hero, message composer, and context rail. Visual design only in this phase — chat, sidebar, and model catalog are not wired yet.

## Visibility

Internal module with `HomeFacade` and `HomeScreen` as the app-shell entry points.

## Language

- **HomeComponent**: Decompose component for local UI state
- **HomeIntent** / **HomeReducer**: Draft message and placeholder actions
- **HomeView**: Root layout (top bar, welcome hero, composer)
- **HomeParticleOrbView**: Pre-rasterized animated pixel orb - **OpenCorePalette**: Shared graphite tokens via onboarding theme

## Keyboard avoidance

The welcome content scrolls inside `WelcomeScrollContainer` (in `HomeView.kt`). The composer sits below the scroll area (like iOS `safeAreaInset`). `WelcomeScrollContainer` applies `imePadding()` so the scroll viewport and composer move up together when the keyboard opens. `restingViewportHeight` is captured while the IME is hidden and used only as a first-frame fallback. While the keyboard is open, layout uses the live measured viewport so the orb and greeting re-center above the composer. `LaunchedEffect(imeVisible)` scrolls back to top when the keyboard hides. Tapping the content area or top bar dismisses the keyboard via `LocalSoftwareKeyboardController`.

## Current scope

| Implemented | Not yet |
|---|---|
| Welcome hero + particle orb | Chat thread |
| Composer prompt panel | Sidebar |
| Model / speed / context rail (static demo) | Model popup |
| Top bar chrome | API key validation |
| Draft text input + send clears field | Real send / streaming |

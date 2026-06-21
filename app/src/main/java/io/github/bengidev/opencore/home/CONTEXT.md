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

The welcome content scrolls inside `WelcomeScrollContainer` (in `HomeView.kt`). Viewport height comes from `BoxWithConstraints` (no zero-height first frame). Hero centering uses the viewport minus `composerClearance` so the orb stays above the overlaid composer. `frozenViewportHeight` is captured while the IME is hidden and reused while the IME is visible so content keeps full height when the viewport shrinks. `LaunchedEffect(imeVisible)` scrolls only on keyboard show/hide transitions (not on first composition). The composer is overlaid at `BottomCenter` with `imePadding()` so it rides above the keyboard. Tapping the content area or top bar dismisses the keyboard via `LocalSoftwareKeyboardController`.

## Current scope

| Implemented | Not yet |
|---|---|
| Welcome hero + particle orb | Chat thread |
| Composer prompt panel | Sidebar |
| Model / speed / context rail (static demo) | Model popup |
| Top bar chrome | API key validation |
| Draft text input + send clears field | Real send / streaming |

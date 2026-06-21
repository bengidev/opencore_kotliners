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

The welcome content scrolls inside `WelcomeScrollContainer` (in `HomeView.kt`). The composer sits below the scroll area (like iOS `safeAreaInset`), so `BoxWithConstraints` measures the true viewport above the composer. `frozenViewportHeight` is captured while the IME is hidden and reused while the IME is visible so hero layout keeps full height when the scroll viewport shrinks. `LaunchedEffect(imeVisible)` scrolls to the bottom on keyboard show and back to top on hide, waiting for `scrollState.maxValue` before animating. The composer uses `imePadding()` so it rides above the keyboard. Tapping the content area or top bar dismisses the keyboard via `LocalSoftwareKeyboardController`.

## Current scope

| Implemented | Not yet |
|---|---|
| Welcome hero + particle orb | Chat thread |
| Composer prompt panel | Sidebar |
| Model / speed / context rail (static demo) | Model popup |
| Top bar chrome | API key validation |
| Draft text input + send clears field | Real send / streaming |

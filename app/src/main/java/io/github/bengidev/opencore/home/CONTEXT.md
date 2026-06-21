# Home Context

| | |
|---|---|
| **Context** | Home feature — welcome landing screen |
| **Package** | `io.github.bengidev.opencore.home` |
| **Module** | Internal module inside `:app` |
| **Source port** | [openzone-swifters](https://github.com/bengidev/openzone-swifters) `Features/Home/` |

Welcome-state home screen with particle orb hero, message composer, and context rail. Visual design only in this phase — chat, sidebar, and model catalog are not wired yet.

## Visibility

Internal module with `HomeFacade` and `HomeScreen` as the app-shell entry points.

## Language

- **HomeComponent**: Decompose component for local UI state
- **HomeIntent** / **HomeReducer**: Draft message and placeholder actions
- **HomeView**: Root layout (`WelcomeScrollContainer`, floating top bar overlay, composer)
- **WelcomeScrollContainer**: IME-aware scroll viewport with composer below the scroll area
- **HomeTopBarOverlay**: Menu and new-chat icons floated above the hero
- **HomeTopBarClearance**: Top inset reserved under the overlay
- **HomeParticleOrbView**: Pre-rasterized animated pixel orb (from openzone-kotliners)
- **OpenCorePalette**: Shared graphite tokens via onboarding theme

## Keyboard avoidance

The welcome content scrolls inside `WelcomeScrollContainer` (`WelcomeScrollContainer.kt`). The composer sits below the scroll area (like iOS `safeAreaInset`). `WelcomeScrollContainer` applies `imePadding()` so the scroll viewport and composer move up together when the keyboard opens. `restingViewportHeight` is captured while the IME is hidden; while the keyboard is open, hero layout metrics stay frozen at that resting height so the orb and greeting keep their resting size and pan up via scroll instead of compacting. `imeNestedScroll()` keeps scroll in sync with the IME animation. `LaunchedEffect(imeBottomPx, …)` maps keyboard inset to scroll offset once a resting viewport has been measured (skipping the first composition). When the keyboard hides, scroll returns to top. `HomeTopBarOverlay` floats above the hero; tapping the content area or top bar dismisses the keyboard via `LocalSoftwareKeyboardController`.

## Current scope

| Implemented | Not yet |
|---|---|
| Welcome hero + particle orb | Chat thread |
| Composer prompt panel | Sidebar |
| Model / speed / context rail (static demo) | Model popup |
| Top bar chrome | API key validation |
| Draft text input + send clears field | Real send / streaming |

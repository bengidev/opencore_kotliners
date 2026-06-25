# Home Context

| | |
|---|---|
| **Context** | Home feature — welcome landing screen |
| **Package** | `io.github.bengidev.opencore.home` |
| **Module** | Internal module inside `:app` |

Welcome-state home screen with particle orb hero, message composer, and context rail. Sidebar opens the side panel overlay. When a chat thread is active, Home composes `ChatView` and keeps the shared composer chatbox.

## Visibility

Internal module with `HomeFacade` and `HomeScreen` as the app-shell entry points. `HomeScreen` composes `SidePanelScreen` as an overlay.

## Design patterns

| Pattern | Location |
|---|---|
| Command | `HomeIntent` |
| Reducer | `HomeReducer` |
| Facade | `HomeFacade`, `ContextWindowTracker` |
| Policy | `ModelSelectionPolicy` |
| Strategy | `ContextWindowEstimator` (character-based token estimate) |

## Language

- **HomeComponent**: Decompose component for local UI state
- **HomeIntent** / **HomeReducer**: Draft message, speed mode, context usage
- **ContextWindowUsage** (`home/models/`): Token budget display model for composer ring
- **ContextWindowEstimator** / **ContextWindowTracker** (`home/utilities/`): Token estimate strategy and facade
- **HomeComposerSpeedMode** (`home/models/`): Standard / fast presets with OpenRouter `provider.sort.by`
- **HomeView**: Root layout (`WelcomeScrollContainer`, floating top bar overlay, composer)
- **WelcomeScrollContainer**: IME-aware scroll viewport with composer below the scroll area
- **HomeTopBarOverlay**: Menu and new-chat icons floated above the hero
- **HomeTopBarClearance**: Top inset reserved under the overlay
- **HomeParticleOrbView**: Pre-rasterized animated pixel orb
- **OpenCorePalette**: Shared graphite tokens via onboarding theme

## Keyboard avoidance

The welcome content scrolls inside `WelcomeScrollContainer` (`WelcomeScrollContainer.kt`). The composer sits below the scroll area. `WelcomeScrollContainer` applies `imePadding()` so the scroll viewport and composer move up together when the keyboard opens. `restingViewportHeight` is captured while the IME is hidden; while the keyboard is open, hero layout metrics stay frozen at that resting height so the orb and greeting keep their resting size and pan up via scroll instead of compacting. `imeNestedScroll()` keeps scroll in sync with the IME animation. `LaunchedEffect(imeBottomPx, …)` maps keyboard inset to scroll offset once a resting viewport has been measured (skipping the first composition). When the keyboard hides, scroll returns to top. `HomeTopBarOverlay` floats above the hero; tapping the content area or top bar dismisses the keyboard via `LocalSoftwareKeyboardController`.

## Current scope

| Implemented | Not yet |
|---|---|
| Welcome hero + particle orb | Live GET /models fetch |
| Composer prompt panel | Attachment / voice capture |
| Model picker sheet (static catalog per provider) | |
| Context window ring + usage popover | |
| Speed mode menu for router models | |
| Model / speed / context rail | |
| Top bar chrome (menu opens side panel) | |
| Draft text input + send clears field | |
| Side panel overlay (via `SidePanelScreen`) | |
| Active thread via `ChatView` (composer + error banner chrome split: banner in Chat, composer here) | |
| API key gating on composer send | |

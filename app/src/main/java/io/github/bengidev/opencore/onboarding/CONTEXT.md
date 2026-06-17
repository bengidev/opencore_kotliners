# Onboarding Context

| | |
|---|---|
| **Context** | Onboarding feature — first-run product tour |
| **Package** | `io.github.bengidev.opencore.onboarding` |
| **Module** | Internal module inside `:app` |
| **Source port** | [openzone-swifters](https://github.com/bengidev/openzone-swifters) (converted to OpenCore branding) |

First-run product tour with interactive visual demos. Persists completion via DataStore, then returns control to the app shell.

## Visibility

The entire onboarding package is an **internal module**: types default to `internal` (Kotlin module visibility). The app shell in `io.github.bengidev.opencore` wires onboarding via `OnboardingFacade` and `OnboardingScreen` in the same `:app` module.

## Language

- **OnboardingComponent**: Decompose component dispatching intents
- **OnboardingIntent**: Command objects (Command pattern)
- **OnboardingReducer**: Pure state transitions
- **Pages**: `EncryptedPairing`, `IdeaStudio`, `PromptQueue`, `ReasoningControl`, `WorkspaceReady`
- **OpenCorePalette**: Graphite monochrome design tokens (OpenCore branding)

## Design patterns

| Pattern | Location |
|---|---|
| Command | `OnboardingIntent` |
| Strategy | `PageDemoDefaultsStrategy` |
| Factory Method | `PageDemoDefaultsStrategyFactory`, `PageVisualFactory` |
| Repository | `OnboardingRepository` |
| Facade | `OnboardingFacade` |

## Flow

```
EncryptedPairing → IdeaStudio → PromptQueue → ReasoningControl → WorkspaceReady
```

## Constraints

- Onboarding must not store provider credentials or model preferences.
- Only completion is persisted; demo state is local UI state.

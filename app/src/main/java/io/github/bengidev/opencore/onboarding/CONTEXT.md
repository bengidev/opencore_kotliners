# Onboarding Context

| | |
|---|---|
| **Context** | Onboarding feature — first-run cinematic intro |
| **Package** | `io.github.bengidev.opencore.onboarding` |
| **Module** | Internal module inside `:app` |

Single-page onboarding with a wireframe cube hero, looping chat feature feed, usage notice, and swipe-to-start CTA. Persists completion via DataStore, then returns control to the app shell.

## Visibility

The entire onboarding package is an **internal module**: types default to `internal` (Kotlin module visibility). The app shell in `io.github.bengidev.opencore` wires onboarding via `OnboardingFacade` and `OnboardingScreen` in the same `:app` module.

## Language

- **OnboardingComponent**: Decompose component dispatching intents
- **OnboardingIntent**: Command objects (Command pattern)
- **OnboardingReducer**: Pure state transitions (`isFinished` only)
- **OnboardingFeature**: Feature catalog for the chat feed
- **ThinkingOrbsKit port** (`thinkingorbs/`): MetalForge thinking-orb procedural animation engine (Canvas)
- **OpenCorePalette**: Graphite monochrome design tokens (OpenCore branding)

## Design patterns

| Pattern | Location |
|---|---|
| Command | `OnboardingIntent` |
| Repository | `OnboardingRepository` |
| Facade | `OnboardingFacade` |

## Flow

```
Cube hero showoff → morph to header → chat feature feed → swipe to start → app shell
```

## Constraints

- Onboarding must not store provider credentials or model preferences.
- Only completion is persisted; animation and chat feed state are local UI state.

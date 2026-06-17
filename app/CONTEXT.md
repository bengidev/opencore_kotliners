# App Context

| | |
|---|---|
| **Context** | OpenCore Android application |
| **Module** | `:app` (single Gradle module) |

The app composes feature packages as siblings under `io.github.bengidev.opencore.*`. Each feature is an **internal module**: its own `domain/`, `application/`, `infrastructure/`, `presenter/`, and `theme/` packages, with types defaulting to `internal` visibility.

## Language

- **MainActivity**: Composition root, Decompose lifecycle owner
- **OnboardingFacade**: Wiring entry for the onboarding internal module
- **HomeFacade**: Wiring entry for the home internal module

## Internal modules (in `:app`)

```text
io.github.bengidev.opencore/
├── MainActivity.kt
├── onboarding/          # Onboarding internal module
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   ├── presenter/
│   └── theme/
├── home/                # Home internal module
│   ├── application/
│   ├── presenter/
│   └── theme/
└── ui/theme/            # App-wide theme
```

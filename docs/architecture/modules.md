# Module Layout

OpenCore uses feature-oriented packages inside the `:app` Gradle module. Packages are intentionally shaped like modules so they can be promoted to Gradle library modules later without rewriting feature boundaries.

Onboarding flow state is owned by `OnboardingComponent` and mutated through explicit intents — not a global store.

## Module map

```text
:app
├── ui/               # App-wide theme + shared Compose primitives
├── onboarding/       # First-run product tour
├── sidepanel/        # Conversation browser + settings (self-contained internal module)
├── chat/             # Live message stream, send/receive, active conversation (ChatView — thread only)
└── home/             # Welcome hero + composer chatbox (wires Chat + SidePanel)
```

## Current layout

```text
app/src/main/java/io/github/bengidev/opencore/
├── MainActivity.kt           # Composition root, Decompose lifecycle owner
├── ui/
│   ├── theme/
│   └── components/
├── onboarding/               # Role-based
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   ├── presenter/
│   └── theme/
├── home/
│   ├── application/
│   ├── models/               # HomeModelOption, ContextWindowUsage, HomeComposerSpeedMode
│   ├── utilities/            # ContextWindowEstimator, ContextWindowTracker, HomeModelCatalogClient
│   ├── infrastructure/
│   ├── presenter/
│   └── theme/
├── chat/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   ├── presenter/
│   ├── utilities/
│   └── theme/
└── sidepanel/
    ├── domain/
    ├── application/
    │   ├── session/
    │   └── setting/
    ├── infrastructure/
    └── presenter/
```

SidePanel is a self-contained internal module combining two scopes (session + setting) with feature-owned infrastructure in `infrastructure/`. Its `application/` package nests `session/` and `setting/` sub-packages, each with its own component, state, intent, and reducer.

Home uses flat role folders only (`application/`, `models/`, `utilities/`, `infrastructure/`, `presenter/`, `theme/`). Context window and speed mode types live alongside other Home models and utilities.

`ChatView` is the Chat module entry view for the active message thread. The composer chatbox (`HomeComposerView`) stays owned and positioned by Home in both welcome and chat states.

## Role-based folders

Each feature organizes files by responsibility:

- `domain/` — domain types and value objects
- `application/` — Decompose components, intents, reducers, state
- `infrastructure/` — persistence clients, HTTP adapters, codecs
- `presenter/` — Compose screens and visual components
- `utilities/` — helpers that are not domain or infrastructure
- `theme/` — feature-scoped tokens and typography

Folder names describe product roles, not design-pattern names.

## Internal module boundaries

All feature packages live in `:app`. Types default to `internal`. Each module exposes a **Facade** (`OnboardingFacade`, `HomeFacade`, `ChatFacade`, `SidePanelFacade`) plus one or two shell entry composables for `MainActivity` wiring. Cross-feature calls go through facades and injected components — not direct presenter imports.

Promote to a Gradle library module only when a boundary needs stronger isolation than package visibility.

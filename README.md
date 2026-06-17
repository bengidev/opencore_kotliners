# opencore_kotliners

OpenCore Android — Kotlin/Compose app with feature-oriented **internal modules** inside `:app`.

## Layout

```text
:app
└── io.github.bengidev.opencore/
    ├── MainActivity.kt
    └── onboarding/     # internal module (package + internal visibility)
```

See [CONTEXT-MAP.md](CONTEXT-MAP.md).

## Run

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

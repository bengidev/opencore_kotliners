# Context Map

Feature packages live inside `:app` as internal modules (package boundaries + `internal` visibility).

| Package / context | Context doc |
| --- | --- |
| `app/` shell | [app/CONTEXT.md](app/CONTEXT.md) |
| `app/.../shared/` | Shared internal modules (credential, persistence, providers) |
| `app/.../tabbar/` | [app/src/main/java/io/github/bengidev/opencore/tabbar/CONTEXT.md](app/src/main/java/io/github/bengidev/opencore/tabbar/CONTEXT.md) |
| `app/.../about/` | [app/src/main/java/io/github/bengidev/opencore/about/CONTEXT.md](app/src/main/java/io/github/bengidev/opencore/about/CONTEXT.md) |
| `app/.../onboarding/` | [app/src/main/java/io/github/bengidev/opencore/onboarding/CONTEXT.md](app/src/main/java/io/github/bengidev/opencore/onboarding/CONTEXT.md) |
| `app/.../home/` | [app/src/main/java/io/github/bengidev/opencore/home/CONTEXT.md](app/src/main/java/io/github/bengidev/opencore/home/CONTEXT.md) |
| `app/.../sidepanel/` | [app/src/main/java/io/github/bengidev/opencore/sidepanel/CONTEXT.md](app/src/main/java/io/github/bengidev/opencore/sidepanel/CONTEXT.md) |
| `app/.../chat/` | [app/src/main/java/io/github/bengidev/opencore/chat/CONTEXT.md](app/src/main/java/io/github/bengidev/opencore/chat/CONTEXT.md) |
| `app/.../speech/` | Speech-to-text dictation for the composer |
| `app/.../vision/` | Composer media intake (photos, videos, files) |

App-scoped ADRs: [app/docs/adr/](app/docs/adr/) (when present).

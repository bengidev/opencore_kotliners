# Tab Bar Context

| | |
| --- | --- |
| **Context** | App shell tab navigation UI |
| **Package** | `io.github.bengidev.opencore.tabbar` |
| **Module** | Internal module inside `:app` |

Bottom tab shell switching between Home, Settings, and About. The app shell
owns selected-tab state; this module renders tab UI and content slots.

## Visibility

Internal module with `TabBarScreen` as the app-shell entry point.

## Language

- **HomeTab**: Closed set of app-shell tabs
- **TabBarScreen** / **TabBarShell**: Stateless slot adapter + Material bottom
  navigation

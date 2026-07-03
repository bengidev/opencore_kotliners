# Tab Bar Context

| | |
| --- | --- |
| **Context** | App shell tab navigation |
| **Package** | `io.github.bengidev.opencore.tabbar` |
| **Module** | Internal module inside `:app` |

Bottom tab shell switching between Home, Settings, and About. Content stays slot-based so the tab module owns navigation state without owning feature internals.

## Visibility

Internal module with `TabBarFacade` and `TabBarScreen` as app-shell entry points.

## Design patterns

| Pattern | Location |
| --- | --- |
| Command | `TabBarIntent` |
| Reducer | `TabBarReducer` |
| Facade | `TabBarFacade` |
| Slot adapter | `TabBarScreen` / `TabBarShell` |

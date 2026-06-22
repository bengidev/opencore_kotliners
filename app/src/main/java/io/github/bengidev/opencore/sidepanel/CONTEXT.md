# Side Panel Context

| | |
|---|---|
| **Context** | Side panel — history drawer + settings sheet |
| **Package** | `io.github.bengidev.opencore.sidepanel` |
| **Module** | Internal module inside `:app` |
| **Source port** | [opencore_swifters](https://github.com/bengidev/opencore_swifters) `Features/SidePanel/` |

Left-sliding history drawer for browsing, pinning, renaming, deleting, and grouping saved conversations, plus a settings sheet for provider selection, API key storage, and reasoning effort.

## Visibility

Internal module with `SidePanelFacade` and `SidePanelScreen` as app-shell entry points. Composed as an overlay on `HomeScreen`.

## Language

- **SidePanelComponent**: Host Decompose component composing session + setting scopes
- **SidePanelSessionComponent** / **SidePanelSettingComponent**: Session drawer and settings sheet state machines
- **SidePanelSessionIntent** / **SidePanelSettingIntent**: Command-style mutations
- **SidePanelSessionDrawer**: Animated left drawer (0.82 width ratio, max 360dp, 280ms)
- **SidePanelSettingSheet**: Modal bottom sheet for credentials and provider prefs
- **SidePanelHistoryRepository**: Conversation persistence boundary (in-memory default; Room upgrade path)
- **EncryptedSidePanelCredentialStore**: API keys in EncryptedSharedPreferences
- **DataStoreSidePanelPreferenceStore**: Provider + reasoning preferences

## Current scope

| Implemented | Not yet |
|---|---|
| History drawer UI + animations | Chat thread resume |
| Search, pin, rename, delete, group | Room-backed history persistence |
| Settings sheet (provider, API key, reasoning) | Delegate wiring to live chat |
| Recency grouping + relative labels | Custom provider URLs |
| Unit tests for grouping + reducers | |

## Integration

`MainActivity` creates `SidePanelComponent` alongside `HomeComponent`. The home menu button calls `SidePanelComponent.toggleSidebar()`. Settings opens from the drawer gear icon.

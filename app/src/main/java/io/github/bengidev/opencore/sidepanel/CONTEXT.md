# SidePanel Context

| | |
|---|---|
| **Context** | SidePanel feature — session browser sidebar + settings sheet |
| **Package** | `io.github.bengidev.opencore.sidepanel` |
| **Module** | Internal module inside `:app` |

Session browser drawer with a gear button that opens a settings sheet (provider picker + API key field). API keys are persisted via EncryptedSharedPreferences (Android Keychain equivalent). The session list is seeded from sample data; rename/delete delegate to SessionRepository.

## Visibility

Internal module with `SidePanelFacade` and the `SidePanelDrawer` / `SidePanelSettingsSheetRoute` composables as app-shell entry points.

## Language

- **SidePanelComponent**: Decompose component for session + settings state
- **SidePanelIntent** / **SidePanelReducer**: Command + pure reducer for session selection, rename, delete, settings visibility, provider selection, API key save/remove
- **CredentialStore**: Port for encrypted API key storage (EncryptedSharedPreferences live adapter, in-memory double for tests)
- **SessionRepository**: Port for session list persistence
- **SessionProvider**: Enum of supported API providers (OpenRouter, Anthropic, OpenAI)
- **SidePanelDrawerContent**: Drawer content (header + gear button + session list)
- **SidePanelSettingsSheet**: ModalBottomSheet with provider picker + API key field + save/remove buttons
- **SidePanelTheme**: Reuses OpenCorePalette from onboarding theme

## Design patterns

| Pattern | Location |
|---|---|
| Command | `SidePanelIntent` |
| Reducer | `SidePanelReducer` |
| Repository | `CredentialStore`, `SessionRepository` |
| Facade | `SidePanelFacade` |

## Gear button wiring

The drawer header's gear IconButton calls `SidePanelComponent.onSettingsTapped()`, which dispatches `SettingsTapped` (flips `isSettingsVisible` true) and invokes `onSettingsTappedCallback`. The host renders `SidePanelSettingsSheetRoute`, which shows the `SidePanelSettingsSheet` ModalBottomSheet when `isSettingsVisible` is true. This is the wired path — the gear button is never a dead no-op.

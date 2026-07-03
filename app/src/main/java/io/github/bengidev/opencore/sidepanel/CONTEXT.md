# Side Panel Context

| | |
| --- | --- |
| **Context** | Side panel — history drawer + settings sheet |
| **Package** | `io.github.bengidev.opencore.sidepanel` |
| **Module** | Internal module inside `:app` |

Left-sliding history drawer for browsing, pinning, renaming, deleting, and
grouping saved conversations, plus reusable settings content for provider
selection, API key storage, and reasoning effort.

## Visibility

Internal module with `SidePanelFacade`, `SidePanelScreen`, and
`SidePanelSettingsScreen` as app-shell entry points. Drawer is composed as an
overlay on `HomeScreen`; settings content can be shown as a modal sheet or tab
content.

## Language

- **SidePanelComponent**: Host Decompose component for session drawer state
- **SidePanelSettingComponent**: Settings state machine owned by app shell
- **SidePanelSessionIntent** / **SidePanelSettingIntent**: Command-style
  mutations
- **SidePanelSessionDrawer**: Animated left drawer (0.82 width ratio, max
  360dp, 280ms)
- **SidePanelSettingContent**: Reusable credentials and provider preferences UI
- **SidePanelSettingSheet**: Modal bottom sheet adapter for settings content
- **SidePanelSettingsScreen**: Tab adapter for settings content
- **SidePanelHistoryRepository**: Conversation persistence seam
- **EncryptedSidePanelCredentialStore**: API keys in EncryptedSharedPreferences
- **DataStoreSidePanelPreferenceStore**: Provider + reasoning preferences

## Current scope

| Implemented | Not yet |
| --- | --- |
| History drawer UI + animations | Room-backed history persistence |
| Search, pin, rename, delete, group | Custom provider URLs |
| Settings tab + modal sheet | |
| Recency grouping + relative labels | |
| Delegate wiring to live chat | |
| Unit tests for grouping + reducers | |

## Integration

`MainActivity` creates `SidePanelComponent`, `SidePanelSettingComponent`, and
`HomeComponent`. The home menu button calls `SidePanelComponent.toggleSidebar()`.
Settings opens from the drawer gear icon or the app shell tab bar.

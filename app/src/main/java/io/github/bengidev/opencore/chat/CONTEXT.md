# Chat Context

| | |
|---|---|
| **Context** | Chat feature — live conversation thread |
| **Package** | `io.github.bengidev.opencore.chat` |
| **Module** | Internal module inside `:app` |

Owns the active conversation thread: loading persisted messages, sending user messages, and streaming assistant replies. Does not own the history drawer (SidePanel) or welcome hero layout (Home).

## Visibility

Internal module with `ChatFacade` as the app-shell wiring entry. `ChatComponent` holds Decompose state; `ChatThreadView` is composed from Home when a thread is active.

## Language

- **ChatComponent**: Decompose component for thread lifecycle and send
- **ChatIntent** / **ChatReducer**: Command-style state mutations
- **ChatStreamingClient**: Strategy seam for provider streaming (`ProviderChatStreamingClient` → OpenAI-compatible SSE HTTP)
- **SidePanelHistoryRepository**: Persistence for conversations and messages (owned by SidePanel infrastructure)
- **SidePanelMessageKind**: Message kind discriminator on persisted history rows

## Integration

| Event | Handler |
|---|---|
| Composer send | `ChatComponent.sendUserMessage` |
| Side panel row tap | `ChatComponent.openConversation` |
| New conversation (+) | `ChatComponent.startNewConversation` |
| Active rename/delete | `ChatComponent` via SidePanel delegates |
| Session highlight | `SidePanelSessionIntent.ActiveConversationIdChanged` |

## Current scope

| Implemented | Not yet |
|---|---|
| Thread message list UI | Live GET /models fetch |
| SSE streaming with thinking + answer merge | Room-backed persistence |
| Send creates conversation + user message | In-flight stream cancel button |
| OpenAI-compatible provider streaming | Token-based context usage ring |
| Resume from history with load guards | |
| Credential gating on composer send | |
| Static model catalog per provider | |

# Chat Context

| | |
|---|---|
| **Context** | Chat feature — live conversation thread |
| **Package** | `io.github.bengidev.opencore.chat` |
| **Module** | Internal module inside `:app` |

Owns the active conversation thread: loading persisted messages, sending user messages, and appending assistant replies via a completion client. Does not own the history drawer (SidePanel) or welcome hero layout (Home).

## Visibility

Internal module with `ChatFacade` as the app-shell wiring entry. `ChatComponent` holds Decompose state; `ChatThreadView` is composed from Home when a thread is active.

## Language

- **ChatComponent**: Decompose component for thread lifecycle and send
- **ChatIntent** / **ChatReducer**: Command-style state mutations
- **ChatCompletionClient**: Strategy seam for provider responses (stub echo today)
- **SidePanelHistoryRepository**: Persistence for conversations and messages (owned by SidePanel infrastructure)

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
| Thread message list UI | Provider streaming / SSE |
| Send creates conversation + user message | Model catalog fetch |
| Echo assistant reply via stub client | Credential gating on send |
| Resume from history | Room-backed persistence |

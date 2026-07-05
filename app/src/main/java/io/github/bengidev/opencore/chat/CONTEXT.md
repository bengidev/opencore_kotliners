# Chat Context

| | |
|---|---|
| **Context** | Chat feature — live conversation thread |
| **Package** | `io.github.bengidev.opencore.chat` |
| **Module** | Internal module inside `:app` |

Owns the active conversation thread: loading persisted messages, sending user messages, and streaming assistant replies. Does not own the history drawer (SidePanel) or welcome hero layout (Home).

## Visibility

Internal module with `ChatFacade` as the app-shell wiring entry. `ChatComponent` holds Decompose state; `ChatView` is composed from Home when a thread is active.

## Design patterns

| Pattern | Location |
|---|---|
| Command | `ChatIntent` |
| Reducer | `ChatReducer` |
| Facade | `ChatFacade` |
| Strategy | `ChatStreamingClient`, `ChatStreamingTextAppendStrategy` |
| Policy | `ChatStreamingCoalescingPolicy`, `ChatStreamingTextAppendPolicy`, `ChatStreamingTextCursorPolicy`, `ChatViewTitlePolicy`, `ChatThreadLayoutPolicy`, `ChatThreadScrollPolicy` |
| Pure utility | `ChatMarkwonRenderer`, `ChatStreamingMarkdownGuard` |
| State | `ChatStreamingCoalescer` |
| Pure merge | `ChatStreamingMerger` |

## Language

- **ChatComponent**: Decompose component for thread lifecycle and send
- **ChatIntent** / **ChatReducer**: Command-style state mutations
- **ChatView**: Entry view for the active thread (title, thread, error banner; composer stays in Home)
- **ChatThreadView**: Bottom-aligned scrollable message list inside `ChatView` (`BoxWithConstraints` + chronological `LazyColumn`; mirrors iOS `defaultScrollAnchor(.bottom)`)
- **ChatErrorBannerView**: Turn-level failure banner colocated in `ChatView`
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
| OpenAI-compatible provider streaming | |
| Assistant content normalization (blocks + safety-only) | |
| Polymorphic `delta.content` (string or array) | |
| Resume from history with load guards | |
| Credential gating on composer send | |
| Static model catalog per provider | |

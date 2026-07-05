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
| Policy | `ChatStreamingCoalescingPolicy`, `ChatStreamingTextAppendPolicy`, `ChatStreamingTextCursorPolicy`, `ChatViewTitlePolicy`, `ChatThreadLayoutPolicy`, `ChatThreadScrollPolicy`, `ChatReasoningCollapsePolicy` |
| Segmenter | `ChatRichContentSegmenter` |
| Presenter orchestrator | `ChatRichContentColumn` |
| Pure utility | `ChatMarkwonRenderer`, `ChatPlainTextRenderer`, `ChatStreamingMarkdownGuard`, `BoundedSpannedCache` |
| Composable embed | `MarkdownEmbedWebView` |
| State | `ChatStreamingCoalescer` |
| Pure merge | `ChatStreamingMerger` |

## Rich markdown rendering

Hybrid Markwon + CDN WebView pipeline for assistant answers, thinking cards, and command output detail.

**Streaming policy:** deferred — plain text while streaming (`ChatStreamingTextView`); switch to full rich rendering (`ChatRichContentColumn`) only when the message completes. Thinking streams use mono-italic typography and a blinking cursor.

**Completed content pipeline:**

1. `ChatAssistantMarkdownPreprocessor.normalize` sanitizes input.
2. `ChatRichContentSegmenter` splits markdown into `ChatRichContentSegment` values (`Prose`, `RawFragment`, `MermaidDiagram`, `MathBlock`). Fence-aware split (not AST). `mermaid`, `latex`/`math`/`katex` fences become embed segments; other fenced languages stay in prose. `ChatStreamingMarkdownGuard.shouldUsePlainFallback` provides all-or-nothing prose fallback when markdown is incomplete.
3. `ChatRichContentColumn` lays out segments in a Compose `Column`.
4. **Prose** → `ChatMarkwonRenderer.spanned` in an `AndroidView` `TextView`. Profiles: `Assistant` (default body) and `Thinking` (monospace + italic). Markwon plugins: core, strikethrough, tables, linkify. Results cached in `BoundedSpannedCache`.
5. **Embeds** → `MarkdownEmbedWebView` loads `assets/markdown-embed/host.html`, renders via jsDelivr CDN (KaTeX 0.16.11, Mermaid 10.9.0), reports height through a JS bridge.

**Surfaces:**

| Surface | Streaming | Complete |
|---|---|---|
| Assistant answer (`ChatAssistantTextView`) | `ChatStreamingTextView` (plain) | `ChatRichContentColumn` (Assistant profile) |
| Thinking card (`ChatReasoningCardView`) | `ChatStreamingTextView` (mono italic + cursor) | `ChatRichContentColumn` (Thinking profile) |
| Command output detail (`ChatOutputStreamCardView`) | — | `ChatRichContentColumn` (Assistant profile) |

Thinking card starts expanded; `ChatReasoningCollapsePolicy` auto-collapses when a competing answer or output stream is active (`hasCompetingStream` from `ChatThreadView`).

## Language

- **ChatComponent**: Decompose component for thread lifecycle and send
- **ChatIntent** / **ChatReducer**: Command-style state mutations
- **ChatView**: Entry view for the active thread (title, thread, error banner; composer stays in Home)
- **ChatThreadView**: Bottom-aligned scrollable message list inside `ChatView` (`BoxWithConstraints` + chronological `LazyColumn`; mirrors iOS `defaultScrollAnchor(.bottom)`)
- **ChatErrorBannerView**: Turn-level failure banner colocated in `ChatView`
- **ChatStreamingClient**: Strategy seam for provider streaming (`ProviderChatStreamingClient` → OpenAI-compatible SSE HTTP)
- **ChatRichContentSegment**: Sealed segment types — `Prose`, `RawFragment`, `MermaidDiagram`, `MathBlock`
- **ChatRichContentSegmenter**: Fence-aware markdown splitter for completed content
- **ChatRichContentColumn**: Compose orchestrator — Markwon `TextView` per prose segment, `MarkdownEmbedWebView` per embed
- **ChatStreamingTextView**: Coalesced plain-text streaming with optional cursor
- **ChatMarkwonRenderer**: Markwon factory with `Assistant` and `Thinking` theme profiles
- **ChatPlainTextRenderer**: Plain `Spanned` styling for raw streaming fragments (no markdown parse)
- **ChatStreamingMarkdownGuard**: Detects incomplete fences/backticks; keeps segmenter on prose-only fallback
- **MarkdownEmbedWebView**: WebView composable for Mermaid diagrams and KaTeX math blocks
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
| Rich markdown rendering (Markwon + WebView embeds) | |
| Deferred streaming (plain while streaming; rich when complete) | |
| Thinking card auto-collapse on competing stream | |

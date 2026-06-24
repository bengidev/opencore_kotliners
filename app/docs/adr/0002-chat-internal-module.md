# ADR-0002: Chat as in-app internal module

## Status

Accepted

## Context

The app needed a live message thread wired to Home (composer send) and SidePanel (resume, rename, delete delegates). SidePanel history and Home composer existed; the active thread was not yet implemented.

## Decision

1. Place chat under `io.github.bengidev.opencore.chat/` with layered sub-packages matching other internal modules.
2. Default types to `internal`; expose `ChatFacade` and `ChatComponent` to the app shell.
3. Apply Command (`ChatIntent`), Reducer, Facade, Repository (reuse `SidePanelHistoryRepository`), and Strategy (`ChatStreamingClient` with `ProviderChatStreamingClient` adapter).
4. Chat owns active-thread state and message send/resume; SidePanel remains the history browser.
5. TDD: unit tests under `app/src/test/.../chat/`.

## Consequences

- Home switches between welcome hero and thread list based on `ChatState.isThreadActive`.
- `MainActivity` wires SidePanel delegates to `ChatComponent` and syncs `activeConversationId` in session state.
- Provider streaming is implemented via `ChatStreamingMerger` and `OpenAiCompatibleStreamingClient`; echo client remains for tests.

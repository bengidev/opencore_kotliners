# ADR-0002: Chat as in-app internal module

## Status

Accepted

## Context

iOS OpenCore documents a future `Features/Chat/` scope for the live message thread, wired to Home (composer send) and SidePanel (resume, rename, delete delegates). Android already ports SidePanel history and Home composer; the live thread was not yet implemented.

## Decision

1. Place chat under `io.github.bengidev.opencore.chat/` with layered sub-packages matching other internal modules.
2. Default types to `internal`; expose `ChatFacade` and `ChatComponent` to the app shell.
3. Apply GoF patterns: Command (`ChatIntent`), Reducer, Facade, Repository (reuse `SidePanelHistoryRepository`), Strategy (`ChatCompletionClient` with stub adapter until streaming lands).
4. Chat owns active-thread state and message send/resume; SidePanel remains the history browser.
5. TDD: unit tests under `app/src/test/.../chat/`.

## Consequences

- Home switches between welcome hero and thread list based on `ChatState.isThreadActive`.
- `MainActivity` wires SidePanel delegates to `ChatComponent` and syncs `activeConversationId` in session state.
- Real provider streaming replaces `EchoChatCompletionClient` without changing reducer or presenter contracts.

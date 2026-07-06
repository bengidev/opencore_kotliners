# Composer Attachment Capabilities Design

**Date:** 2026-07-06  
**Status:** Approved  
**Scope:** Home composer plus-button, model capability fetch, attachment menu filtering

## Problem

The composer plus (+) button is always visible and opens a fixed attachment menu (Photo Library / Import File), regardless of whether the selected AI model supports non-text input. Model capabilities are derived once from the catalog list response using a legacy `architecture.modality` string. Users can attach incompatible media and only discover the mismatch at send time.

## Goals

1. Fetch fresh model input capabilities each time the user selects a model.
2. Hide the plus button when the model only accepts text input.
3. Filter attachment menu options and file-picker MIME types by supported modalities.
4. Auto-remove incompatible draft attachments when switching to a narrower-capability model, with a brief toast.

## Non-goals

- Native multimodal wire for plain-text files (existing text-inlining behavior unchanged).
- Per-model capability caching across sessions (fetch on every selection).
- Attachment support for audio input modality (out of scope for this change).

## Decisions

| Topic | Decision |
| --- | --- |
| Capability source | Fresh API request on each model selection |
| Switch to text-only with attachments | Auto-remove incompatible drafts + brief toast |
| Attachment menu | Filter options by model capabilities |
| Architecture approach | Extend `HomeComponent` + `HomeModelCatalogClient` (Approach 1) |

## Capability Fetch

### OpenRouter

Use the single-model endpoint documented by OpenRouter:

```
GET https://openrouter.ai/api/v1/model/{modelId}
```

Where `modelId` is the catalog id (e.g. `openai/gpt-4o`). Response includes `architecture.input_modalities` (e.g. `["text"]`, `["text", "image", "file"]`).

### Other providers

OpenCode, Command Code, and Ollama Cloud do not expose a single-model details endpoint in the current adapter layer. For these providers, skip the network call and derive capabilities from the already-loaded catalog list entry.

### New infrastructure

- `ProviderAdapting.encodeModelDetailsRequest(modelId, secret): ProviderHttpRequest?` — OpenRouter implements; others return `null`.
- `HomeModelCatalogClient.fetchModelDetails(adapter, secret, modelId): ModelDetailsResult` — HTTP GET + parse.
- Extend `ProviderCatalogParser` to prefer `architecture.input_modalities` array, with fallback to legacy `architecture.modality` string for list responses and older payloads.

### Selection flow

1. User picks a model → optimistically set `selectedModelId` / title; close picker.
2. Set `isLoadingModelCapabilities = true`.
3. If provider supports model details → fetch; else use catalog entry.
4. On success → merge refreshed capabilities into the matching `availableModels` entry; dispatch `ModelCapabilitiesLoaded`.
5. On failure → fall back to catalog-list capabilities for that id; if absent, treat as text-only.
6. Set `isLoadingModelCapabilities = false`.
7. Prune draft attachments incompatible with resolved capabilities; show toast if any removed.

While `isLoadingModelCapabilities` is true, hide the plus button conservatively to avoid flashing wrong options.

## Capability Model

Extend `SidePanelModel`:

```kotlin
val inputModalities: Set<String> = setOf("text")
val supportsFileInput: Boolean  // "file" in inputModalities
// existing: supportsImageInput, supportsVideoInput (derived from inputModalities or legacy modality)
```

Computed property:

```kotlin
val supportsComposerAttachments: Boolean
    get() = supportsImageInput || supportsVideoInput || supportsFileInput
```

Centralize capability rules in `HomeComposerModelCapabilityLogic`:

- `supportsComposerAttachments(model)`
- `attachmentMenuOptions(model)` — which picker actions to expose
- `filePickerMimeTypes(model)` — filtered MIME type array for `OpenDocument`
- `attachmentsToPrune(attachments, model)` — attachments to remove on model switch

### Modality mapping

| `input_modalities` value | Flag |
| --- | --- |
| `image` | `supportsImageInput` |
| `video` | `supportsVideoInput` |
| `file` | `supportsFileInput` |
| `text` | (baseline; does not enable plus button alone) |

Legacy `modality` string (e.g. `text+image`) continues to set image/video flags when `input_modalities` is absent.

## Plus Button Visibility

In `HomeComposerView`, render the plus button only when:

```
selectedModelSupportsComposerAttachments == true
&& !isLoadingModelCapabilities
```

Text-only models (`input_modalities = ["text"]` only): omit the plus button; existing `Spacer` layout keeps mic/send alignment.

## Filtered Attachment Menu

Replace the fixed two-button dialog with a dynamic menu derived from capabilities.

| Model supports | Menu option | Picker |
| --- | --- | --- |
| `image` or `video` | Photo Library | `PickVisualMedia` — `ImageOnly` vs `ImageAndVideo` based on support |
| `file` | Import File (text) | `OpenDocument` with `text/*`, `application/json` |
| `image` (no photo library path needed) | Import File (images) | `image/*` |
| `video` | Import File (videos) | `video/*` |

If exactly one option applies, skip the menu and launch that picker directly on plus tap.

Keep `HomeComposerModelCapabilityLogic.validateNewAttachment` as a safety net at attach time.

## Auto-Remove on Model Switch

When capabilities resolve after model selection:

1. Call `ChatComponent.pruneDraftAttachments { attachment -> shouldKeep(attachment, model) }`.
2. Delete evicted files via `ChatAttachmentStore.remove`.
3. Show snackbar/toast, e.g. *"Attachments removed — {modelName} only supports text input."* (message varies by what was removed).

Runs on every selection after fetch completes, including switches between two multimodal models with different modality sets (e.g. image-only → no video).

## State and Wiring

### `HomeState` additions

- `isLoadingModelCapabilities: Boolean`
- `selectedModelSupportsComposerAttachments: Boolean` (computed from selected model in `availableModels`)
- `transientMessage: String?` for snackbar text

### `HomeIntent` additions

- `ModelCapabilitiesLoadingStarted`
- `ModelCapabilitiesLoaded(model: SidePanelModel)` — carries merged model with fresh capabilities
- `ModelCapabilitiesLoadFailed(modelId: String)` — triggers catalog fallback
- `TransientMessageShown` / `TransientMessageDismissed`

### `HomeScreen`

- Host `SnackbarHost`; observe `transientMessage`, display snackbar, clear on dismiss.
- Build attachment menu from `HomeComposerModelCapabilityLogic.attachmentMenuOptions(selectedModel)`.
- Pass `showAttachmentButton` into `HomeComposerView`.

### `ChatComponent`

- Add `pruneDraftAttachments(shouldKeep: (ChatMessageAttachment) -> Boolean)` via new `ChatIntent`.

## Error Handling

| Case | Behavior |
| --- | --- |
| Fetch 404 / network error | Fall back to catalog list capabilities; toast only if attachments were pruned |
| Fetch in progress | Plus button hidden |
| Plus visible but zero menu options | Should not occur; debug assert |

## Testing

- `ProviderCatalogParser` — `input_modalities` parsing; legacy `modality` fallback
- `HomeComposerModelCapabilityLogic` — menu options, MIME filters, prune logic, `supportsComposerAttachments`
- `HomeModelCatalogClient` — single-model response parsing
- `HomeReducer` — `ModelCapabilitiesLoaded` / failure intents update state correctly
- `HomeComponent` — model selection triggers fetch; prune callback invoked after load

## Files Touched (expected)

| Area | Files |
| --- | --- |
| Provider layer | `ProviderAdapting.kt`, `ProviderOpenRouterAdapter.kt`, `ProviderDescriptor.kt`, `ProviderCatalogParser.kt` |
| Catalog client | `HomeModelCatalogClient.kt` |
| Domain | `SidePanelModel.kt` |
| Home application | `HomeIntent.kt`, `HomeReducer.kt`, `HomeState.kt`, `HomeComponent.kt` |
| Home utilities | `HomeComposerModelCapabilityLogic.kt` |
| Home presenter | `HomeComposerView.kt`, `HomeView.kt` |
| Home screen | `HomeScreen.kt` |
| Chat application | `ChatIntent.kt`, `ChatReducer.kt`, `ChatComponent.kt` |
| Tests | Matching `*Test.kt` files for parser, logic, reducer, client |

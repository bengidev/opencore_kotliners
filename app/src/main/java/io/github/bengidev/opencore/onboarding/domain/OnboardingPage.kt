package io.github.bengidev.opencore.onboarding.domain

internal data class OnboardingPage(
    val type: OnboardingPageType,
    val indexLabel: String,
    val eyebrow: String,
    val headline: String,
    val body: String,
    val metric: String,
    val command: String,
    val shaderIntensity: Double,
    val highlights: List<OnboardingFeatureHighlight>
) {
    val id: String get() = type.name

    companion object {
        val all: List<OnboardingPage> = listOf(
            OnboardingPage(
                type = OnboardingPageType.EncryptedPairing,
                indexLabel = "SEC-01",
                eyebrow = "Encrypted pairing",
                headline = "End-to-end encrypted pairing and chats",
                body = "Pair trusted devices, keep local workspace context private, and open AI chats without leaking the conversation boundary.",
                metric = "E2E CHANNEL",
                command = "pair --device workspace --mode sealed",
                shaderIntensity = 0.78,
                highlights = listOf(
                    OnboardingFeatureHighlight(
                        title = "Local memory",
                        detail = "Persists offline",
                        iconRes = android.R.drawable.ic_menu_save
                    ),
                    OnboardingFeatureHighlight(
                        title = "Secure session",
                        detail = "Rotates keys",
                        iconRes = android.R.drawable.ic_lock_lock
                    )
                )
            ),
            OnboardingPage(
                type = OnboardingPageType.IdeaStudio,
                indexLabel = "AI-02",
                eyebrow = "Model workspace",
                headline = "Ask questions, write, and explore ideas with AI models",
                body = "OpenCore turns prompts into a focused working surface for drafting, refactoring, research, and interface decisions.",
                metric = "MODEL STUDIO",
                command = "ask --model adaptive --context project",
                shaderIntensity = 0.55,
                highlights = listOf(
                    OnboardingFeatureHighlight(
                        title = "Design canvas",
                        detail = "Native cards",
                        iconRes = android.R.drawable.ic_menu_gallery
                    ),
                    OnboardingFeatureHighlight(
                        title = "AI assistance",
                        detail = "Structured sessions",
                        iconRes = android.R.drawable.ic_menu_edit
                    )
                )
            ),
            OnboardingPage(
                type = OnboardingPageType.PromptQueue,
                indexLabel = "RUN-03",
                eyebrow = "Queue control",
                headline = "Queue follow-up prompts while a turn is still running",
                body = "Keep momentum by lining up the next question, test request, or implementation step before the current model turn finishes.",
                metric = "LIVE QUEUE",
                command = "queue append --while-running follow-up",
                shaderIntensity = 0.66,
                highlights = listOf(
                    OnboardingFeatureHighlight(
                        title = "State engine",
                        detail = "Explicit actions",
                        iconRes = android.R.drawable.ic_menu_sort_by_size
                    ),
                    OnboardingFeatureHighlight(
                        title = "Run steering",
                        detail = "Visible follow-ups",
                        iconRes = android.R.drawable.ic_media_play
                    )
                )
            ),
            OnboardingPage(
                type = OnboardingPageType.ReasoningControl,
                indexLabel = "THK-04",
                eyebrow = "Reasoning dial",
                headline = "Tune how much thinking the AI uses",
                body = "Choose faster answers, balanced planning, or deeper reasoning before the AI model commits compute to the task.",
                metric = "THINK BUDGET",
                command = "model set reasoning --level balanced",
                shaderIntensity = 0.82,
                highlights = listOf(
                    OnboardingFeatureHighlight(
                        title = "Model controls",
                        detail = "Adjust thinking",
                        iconRes = android.R.drawable.ic_menu_preferences
                    ),
                    OnboardingFeatureHighlight(
                        title = "Human steering",
                        detail = "User-set compute",
                        iconRes = android.R.drawable.ic_menu_myplaces
                    )
                )
            ),
            OnboardingPage(
                type = OnboardingPageType.WorkspaceReady,
                indexLabel = "RDY-05",
                eyebrow = "Workspace ready",
                headline = "OpenCore",
                body = "Your AI-native command center. Deploy specialized agents to handle code, review, test, and ship — all within your existing workflow without context switching.",
                metric = "WORKSPACE",
                command = "opencore enter --mode production",
                shaderIntensity = 0.45,
                highlights = listOf(
                    OnboardingFeatureHighlight(
                        title = "Agents",
                        detail = "Specialized runners",
                        iconRes = android.R.drawable.ic_menu_manage
                    ),
                    OnboardingFeatureHighlight(
                        title = "Prompts",
                        detail = "Structured input",
                        iconRes = android.R.drawable.ic_menu_send
                    ),
                    OnboardingFeatureHighlight(
                        title = "Models",
                        detail = "Adaptive compute",
                        iconRes = android.R.drawable.ic_menu_view
                    ),
                    OnboardingFeatureHighlight(
                        title = "Review",
                        detail = "Iterative feedback",
                        iconRes = android.R.drawable.ic_menu_revert
                    ),
                    OnboardingFeatureHighlight(
                        title = "Ship",
                        detail = "Deploy ready",
                        iconRes = android.R.drawable.ic_menu_upload
                    )
                )
            )
        )
    }
}

package io.github.bengidev.opencore.home.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageAttachment
import io.github.bengidev.opencore.chat.domain.ChatMessageAttachmentKind
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeComposerModelCapabilityLogicTest {
    private val visionModel = SidePanelModel(
        id = "gpt-4o",
        displayTitle = "GPT-4o",
        supportsImageInput = true,
    )

    private val textModel = SidePanelModel(
        id = "llama",
        displayTitle = "Llama",
    )

    @Test
    fun validateDraft_blocksImageOnTextOnlyModel() {
        val attachments = listOf(
            ChatMessageAttachment(
                kind = ChatMessageAttachmentKind.IMAGE,
                filename = "photo.jpg",
                localPath = "/tmp/photo.jpg",
            ),
        )

        val decision = HomeComposerModelCapabilityLogic.validateDraft(
            attachments = attachments,
            model = textModel,
            modelName = textModel.displayTitle,
        )

        assertTrue(decision is HomeComposerModelCapabilityLogic.VisualAttachmentDecision.Blocked)
    }

    @Test
    fun validateDraft_allowsImageOnVisionModel() {
        val attachments = listOf(
            ChatMessageAttachment(
                kind = ChatMessageAttachmentKind.IMAGE,
                filename = "photo.jpg",
                localPath = "/tmp/photo.jpg",
            ),
        )

        val decision = HomeComposerModelCapabilityLogic.validateDraft(
            attachments = attachments,
            model = visionModel,
            modelName = visionModel.displayTitle,
        )

        assertEquals(HomeComposerModelCapabilityLogic.VisualAttachmentDecision.Allowed, decision)
    }

    @Test
    fun supportsComposerAttachments_falseForTextOnlyModel() {
        assertFalse(HomeComposerModelCapabilityLogic.supportsComposerAttachments(textModel))
    }

    @Test
    fun supportsComposerAttachments_trueWhenModelAcceptsImages() {
        assertTrue(HomeComposerModelCapabilityLogic.supportsComposerAttachments(visionModel))
    }

    @Test
    fun supportsComposerAttachments_trueWhenModelAcceptsFiles() {
        val fileModel = textModel.copy(supportsFileInput = true)
        assertTrue(HomeComposerModelCapabilityLogic.supportsComposerAttachments(fileModel))
    }

    @Test
    fun attachmentMenuOptions_imageOnlyModel_showsPhotoLibraryOnly() {
        val options = HomeComposerModelCapabilityLogic.attachmentMenuOptions(visionModel)
        assertEquals(
            listOf(HomeComposerModelCapabilityLogic.AttachmentMenuOption.PhotoLibrary),
            options,
        )
    }

    @Test
    fun attachmentMenuOptions_fileCapableModel_showsImportFileOnly() {
        val fileModel = textModel.copy(supportsFileInput = true)
        val options = HomeComposerModelCapabilityLogic.attachmentMenuOptions(fileModel)
        assertEquals(
            listOf(HomeComposerModelCapabilityLogic.AttachmentMenuOption.ImportFile),
            options,
        )
    }

    @Test
    fun attachmentMenuOptions_multimodalModel_showsBothOptions() {
        val multimodalModel = visionModel.copy(supportsFileInput = true)
        val options = HomeComposerModelCapabilityLogic.attachmentMenuOptions(multimodalModel)
        assertEquals(
            listOf(
                HomeComposerModelCapabilityLogic.AttachmentMenuOption.PhotoLibrary,
                HomeComposerModelCapabilityLogic.AttachmentMenuOption.ImportFile,
            ),
            options,
        )
    }

    @Test
    fun attachmentMenuOptions_textOnlyModel_isEmpty() {
        assertTrue(HomeComposerModelCapabilityLogic.attachmentMenuOptions(textModel).isEmpty())
    }
}

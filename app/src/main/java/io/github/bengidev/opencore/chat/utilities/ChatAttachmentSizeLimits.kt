package io.github.bengidev.opencore.chat.utilities

internal object ChatAttachmentSizeLimits {
    /** Maximum video payload encoded and sent to the provider. */
    const val MAX_VIDEO_BYTES = 50 * 1024 * 1024

    /** Maximum size for any imported attachment copy. */
    const val MAX_IMPORT_BYTES = 50 * 1024 * 1024

    fun validateImportSize(byteCount: Int) {
        if (byteCount > MAX_IMPORT_BYTES) {
            throw ChatAttachmentError.ImportTooLarge(byteCount, MAX_IMPORT_BYTES)
        }
    }

    fun validateVideoWireSize(byteCount: Int) {
        if (byteCount > MAX_VIDEO_BYTES) {
            throw ChatAttachmentError.FileTooLarge(byteCount, MAX_VIDEO_BYTES)
        }
    }
}

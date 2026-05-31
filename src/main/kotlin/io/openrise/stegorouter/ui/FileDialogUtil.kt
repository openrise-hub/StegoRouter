package io.openrise.stegorouter.ui

object FileDialogUtil {
    fun chooseFile(title: String, multiple: Boolean = false): List<java.io.File>? {
        return try {
            val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
            dialog.isMultipleMode = multiple
            dialog.isVisible = true

            if (multiple) {
                dialog.files?.toList()
            } else if (dialog.file != null) {
                listOf(java.io.File(dialog.directory, dialog.file))
            } else {
                null
            }
        } catch (e: java.awt.HeadlessException) {
            null
        } catch (e: java.lang.UnsatisfiedLinkError) {
            null
        } catch (e: Exception) {
            null
        }
    }
}

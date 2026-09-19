package com.garland0.tikdown

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStream

/**
 * Decides where a downloaded file's bytes go: a user-chosen SAF folder if one
 * has been set in Prefs, otherwise the public Downloads/TikDown folder via
 * MediaStore (requires no extra permission on API 29+, which is our minSdk).
 */
object StorageHelper {

    fun openOutputStream(context: Context, filename: String, mimeType: String): OutputStream? {
        val customUri = Prefs.getCustomFolderUri(context)

        if (customUri != null) {
            val treeDoc = DocumentFile.fromTreeUri(context, customUri)
            val newFile = treeDoc?.createFile(mimeType, filename)
            val targetUri = newFile?.uri ?: return null
            return context.contentResolver.openOutputStream(targetUri)
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/TikDown")
        }

        val uri: Uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        ) ?: return null

        return context.contentResolver.openOutputStream(uri)
    }
}

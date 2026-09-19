package com.garland0.tikdown

import android.content.Context
import android.net.Uri

object Prefs {
    private const val PREFS_NAME = "tikdown_prefs"
    private const val KEY_FOLDER_URI = "custom_folder_uri"

    fun getCustomFolderUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_FOLDER_URI, null) ?: return null
        return Uri.parse(uriString)
    }

    fun setCustomFolderUri(context: Context, uri: Uri?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FOLDER_URI, uri?.toString()).apply()
    }
}

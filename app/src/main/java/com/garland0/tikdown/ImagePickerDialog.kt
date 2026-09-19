package com.garland0.tikdown

import android.app.Activity
import androidx.appcompat.app.AlertDialog

/**
 * A simple numbered checklist ("Image 1", "Image 2", ...) so the person can
 * pick which items to download from a TikTok slideshow post. All items are
 * checked by default. Kept deliberately simple (no thumbnail loading) to
 * minimize moving parts in a share-sheet flow that needs to be fast and reliable.
 */
object ImagePickerDialog {

    fun show(
        activity: Activity,
        images: List<String>,
        onCancel: () -> Unit = {},
        onConfirm: (List<Int>) -> Unit
    ) {
        val labels = images.indices.map { "Image ${it + 1}" }.toTypedArray()
        val checked = BooleanArray(images.size) { true }

        AlertDialog.Builder(activity, R.style.Theme_TikDown_Dialog)
            .setTitle("Choose images (${images.size} found)")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Download") { _, _ ->
                val selected = checked.indices.filter { checked[it] }
                onConfirm(selected)
            }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .setCancelable(false)
            .show()
    }
}

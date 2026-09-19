package com.garland0.tikdown

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Invisible target for Android's share sheet (registered for ACTION_SEND /
 * text/plain in the manifest). Resolves the shared TikTok link, then either
 * kicks off a background download immediately (single video) or shows a
 * lightweight picker first (slideshow post), and finishes itself either way.
 */
class ShareReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val url = sharedText?.let { Regex("https?://\\S+").find(it)?.value }

        if (url == null) {
            Toast.makeText(this, "No TikTok link found in that share", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { TikTokApi.resolve(url) }
            when (result) {
                is TikTokApi.Result.Video -> {
                    DownloadService.startVideo(this@ShareReceiverActivity, result.playUrl)
                    finish()
                }
                is TikTokApi.Result.Images -> {
                    showImagePicker(url, result.imageUrls)
                }
                is TikTokApi.Result.Error -> {
                    Toast.makeText(
                        this@ShareReceiverActivity,
                        "Couldn't read that link: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun showImagePicker(sourceUrl: String, images: List<String>) {
        ImagePickerDialog.show(
            activity = this,
            images = images,
            onCancel = { finish() }
        ) { selectedIndices ->
            DownloadService.startImages(this, sourceUrl, images, selectedIndices)
            finish()
        }
    }
}

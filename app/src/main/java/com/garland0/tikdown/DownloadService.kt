package com.garland0.tikdown

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Foreground service that performs the actual network download and file write,
 * so the download survives even though the share sheet returns the user to
 * TikTok immediately after they pick this app. Started fresh for each request;
 * stops itself once the work is done.
 */
class DownloadService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val CHANNEL_ID = "tikdown_downloads"
        private const val NOTIF_ID = 1001

        private const val EXTRA_MODE = "mode"
        private const val EXTRA_VIDEO_URL = "video_url"
        private const val EXTRA_IMAGE_URLS = "image_urls"
        private const val EXTRA_SELECTED = "selected_indices"

        fun startVideo(context: Context, videoUrl: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_MODE, "video")
                putExtra(EXTRA_VIDEO_URL, videoUrl)
            }
            context.startForegroundService(intent)
        }

        fun startImages(
            context: Context,
            sourceUrl: String,
            imageUrls: List<String>,
            selected: List<Int>
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_MODE, "images")
                putExtra(EXTRA_IMAGE_URLS, imageUrls.toTypedArray())
                putExtra(EXTRA_SELECTED, selected.toIntArray())
            }
            context.startForegroundService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannelIfNeeded()
        startForeground(NOTIF_ID, buildNotification("Downloading…", ongoing = true))

        val safeIntent = intent
        if (safeIntent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val mode = safeIntent.getStringExtra(EXTRA_MODE)
        scope.launch {
            when (mode) {
                "video" -> {
                    val url = safeIntent.getStringExtra(EXTRA_VIDEO_URL)
                    if (url != null) downloadVideo(url)
                }
                "images" -> {
                    val urls = safeIntent.getStringArrayExtra(EXTRA_IMAGE_URLS)?.toList()
                        ?: emptyList()
                    val selected = safeIntent.getIntArrayExtra(EXTRA_SELECTED)?.toList()
                        ?: urls.indices.toList()
                    downloadImages(urls, selected)
                }
            }
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private suspend fun downloadVideo(url: String) {
        val filename = "tikdown_${System.currentTimeMillis()}.mp4"
        val ok = fetchAndSave(url, filename, "video/mp4")
        updateNotification(if (ok) "Video saved" else "Download failed")
    }

    private suspend fun downloadImages(urls: List<String>, selected: List<Int>) {
        val targets = selected.mapNotNull { urls.getOrNull(it) }
        if (targets.isEmpty()) {
            updateNotification("No images selected")
            return
        }

        val successCount = coroutineScope {
            val jobs = targets.mapIndexed { i, imgUrl ->
                async {
                    val filename = "tikdown_img${i + 1}_${System.currentTimeMillis()}.jpg"
                    fetchAndSave(imgUrl, filename, "image/jpeg")
                }
            }
            jobs.awaitAll().count { it }
        }

        updateNotification("Saved $successCount of ${targets.size} images")
    }

    private fun fetchAndSave(url: String, filename: String, mimeType: String): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                val output = StorageHelper.openOutputStream(this, filename, mimeType)
                    ?: return false
                output.use { out -> body.byteStream().copyTo(out) }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "TikDown Downloads", NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(text: String, ongoing: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TikDown")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(ongoing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification(text, ongoing = false))
    }
}

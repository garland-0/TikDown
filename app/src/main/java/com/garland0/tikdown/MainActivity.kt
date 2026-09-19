package com.garland0.tikdown

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.garland0.tikdown.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manual-entry screen: paste a link and download it directly, plus the
 * storage-folder setting. The main day-to-day flow is the share sheet
 * (ShareReceiverActivity), but this screen covers opening the app directly.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            Prefs.setCustomFolderUri(this, uri)
            updateFolderLabel()
            Toast.makeText(this, "Folder updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermissionIfNeeded()
        updateFolderLabel()

        binding.btnChooseFolder.setOnClickListener {
            folderPicker.launch(null)
        }

        binding.btnResetFolder.setOnClickListener {
            Prefs.setCustomFolderUri(this, null)
            updateFolderLabel()
            Toast.makeText(this, "Back to default folder", Toast.LENGTH_SHORT).show()
        }

        binding.btnDownload.setOnClickListener {
            val text = binding.inputLink.text.toString().trim()
            val url = Regex("https?://\\S+").find(text)?.value
            if (url == null) {
                Toast.makeText(this, "Paste a valid TikTok link first", Toast.LENGTH_SHORT).show()
            } else {
                resolveAndDownload(url)
            }
        }
    }

    private fun resolveAndDownload(url: String) {
        binding.btnDownload.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { TikTokApi.resolve(url) }
            binding.btnDownload.isEnabled = true
            when (result) {
                is TikTokApi.Result.Video -> {
                    DownloadService.startVideo(this@MainActivity, result.playUrl)
                    Toast.makeText(this@MainActivity, "Downloading…", Toast.LENGTH_SHORT).show()
                }
                is TikTokApi.Result.Images -> {
                    ImagePickerDialog.show(this@MainActivity, result.imageUrls) { selected ->
                        DownloadService.startImages(
                            this@MainActivity, url, result.imageUrls, selected
                        )
                    }
                }
                is TikTokApi.Result.Error -> {
                    Toast.makeText(
                        this@MainActivity, "Error: ${result.message}", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateFolderLabel() {
        val uri = Prefs.getCustomFolderUri(this)
        binding.folderLabel.text = if (uri != null) {
            "Custom folder selected"
        } else {
            "Default: Downloads/TikDown"
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }
    }
}

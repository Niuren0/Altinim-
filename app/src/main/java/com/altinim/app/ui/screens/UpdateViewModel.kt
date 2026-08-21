package com.altinim.app.ui.screens

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altinim.app.BuildConfig
import com.altinim.app.data.ApkSignatureVerifier
import com.altinim.app.data.repository.UpdateCheckResult
import com.altinim.app.data.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UpdateRepository()

    private val _uiState = MutableStateFlow<UpdateCheckResult?>(null)
    val uiState: StateFlow<UpdateCheckResult?> = _uiState.asStateFlow()

    private var downloadReceiver: BroadcastReceiver? = null

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = null
            _uiState.value = repository.checkForUpdate(BuildConfig.VERSION_NAME)
        }
    }

    fun downloadAndInstall(downloadUrl: String, version: String) {
        val context = getApplication<Application>()

        if (!context.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(
                context,
                "Güncellemeyi kurmak için \"Bilinmeyen kaynaklardan yükleme\" iznini açman gerekiyor.",
                Toast.LENGTH_LONG
            ).show()
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            return
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        unregisterDownloadReceiver(context)

        val apkFileName = "altinim-$version.apk"
        val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkFileName)

        val request = DownloadManager.Request(downloadUrl.toUri())
            .setTitle("Altınım $version")
            .setDescription("Güncelleme indiriliyor…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                apkFileName
            )
            .setMimeType("application/vnd.android.package-archive")

        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (completedId == downloadId) {
                    val uri = downloadManager.getUriForDownloadedFile(completedId)
                    if (uri != null && apkFile.exists() &&
                        ApkSignatureVerifier.matchesInstalledApp(context, apkFile)
                    ) {
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(installIntent)
                    } else {
                        apkFile.delete()
                        Toast.makeText(
                            context,
                            "İndirilen güncelleme doğrulanamadı, kurulum iptal edildi.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    unregisterDownloadReceiver(context)
                }
            }
        }
        downloadReceiver = receiver

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun unregisterDownloadReceiver(context: Context) {
        downloadReceiver?.let {
            runCatching { context.unregisterReceiver(it) }
        }
        downloadReceiver = null
    }

    override fun onCleared() {
        super.onCleared()
        unregisterDownloadReceiver(getApplication())
    }
}
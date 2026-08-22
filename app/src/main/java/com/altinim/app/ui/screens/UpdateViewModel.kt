package com.altinim.app.ui.screens

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altinim.app.AltinimApplication
import com.altinim.app.BuildConfig
import com.altinim.app.data.ApkSignatureVerifier
import com.altinim.app.data.repository.UpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AltinimApplication).container.updateRepository

    private val _uiState = MutableStateFlow<UpdateCheckResult?>(null)
    val uiState: StateFlow<UpdateCheckResult?> = _uiState.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private var downloadReceiver: BroadcastReceiver? = null

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = UpdateCheckResult.Loading
            _uiState.value = repository.checkForUpdate(BuildConfig.VERSION_NAME)
        }
    }

    fun downloadAndInstall(downloadUrl: String, version: String) {
        // Zaten devam eden bir indirme varsa aynı sürümü tekrar tekrar indirmeye
        // başlamayı engelle (kullanıcı butona art arda basarsa).
        if (_isDownloading.value) return

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

        // getExternalFilesDir() depolama erişilemez durumdayken null dönebilir; bunu görmezden
        // gelip File(null, ...) ile devam etmek, DownloadManager'ın gerçekte yazdığı yoldan
        // farklı (ve yanlış) bir dosya yoluna bakmamıza yol açabilir.
        val externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (externalFilesDir == null) {
            Toast.makeText(
                context,
                "İndirme klasörüne erişilemiyor, depolamayı kontrol et.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        unregisterDownloadReceiver(context)

        // Sürüm etiketi (GitHub tag) dosya adında kullanılıyor; "/" gibi karakterler
        // yol ayırıcı olarak yorumlanabileceğinden temizleniyor.
        val safeVersion = version.replace(Regex("[^A-Za-z0-9.\\-]"), "_")
        val apkFileName = "altinim-$safeVersion.apk"
        val apkFile = File(externalFilesDir, apkFileName)

        _isDownloading.value = true

        val downloadId = try {
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
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            _isDownloading.value = false
            Toast.makeText(
                context,
                "İndirme başlatılamadı: ${e.message ?: "geçersiz bağlantı."}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (completedId != downloadId) return

                unregisterDownloadReceiver(context)
                _isDownloading.value = false

                if (!isDownloadSuccessful(downloadManager, completedId)) {
                    apkFile.delete()
                    Toast.makeText(context, "İndirme başarısız oldu, tekrar dene.", Toast.LENGTH_LONG).show()
                    return
                }

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
            }
        }
        downloadReceiver = receiver

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            // ACTION_DOWNLOAD_COMPLETE korumalı (protected) bir sistem yayınıdır; yalnızca
            // sistem gönderebilir, bu yüzden başka uygulamaların bu yayını taklit etmesine
            // izin verecek şekilde exported yapmaya gerek yok.
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /** İndirmenin gerçekten başarıyla tamamlanıp tamamlanmadığını DownloadManager'dan sorgular. */
    private fun isDownloadSuccessful(downloadManager: DownloadManager, downloadId: Long): Boolean {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query)?.use { cursor: Cursor ->
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex != -1) {
                    return cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL
                }
            }
        }
        return false
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
package com.altinim.app.data.repository

import com.altinim.app.data.remote.GitHubNetworkModule
import kotlinx.coroutines.CancellationException

sealed interface UpdateCheckResult {
    data object Loading : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class UpdateAvailable(val version: String, val downloadUrl: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

class UpdateRepository {

    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult {
        return try {
            val release = GitHubNetworkModule.githubApi.getLatestRelease()
            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
            when {
                apkAsset == null -> UpdateCheckResult.Error("Son sürümde APK dosyası bulunamadı.")
                isNewerVersion(release.tag_name, currentVersion) ->
                    UpdateCheckResult.UpdateAvailable(release.tag_name, apkAsset.browser_download_url)
                else -> UpdateCheckResult.UpToDate
            }
        } catch (e: CancellationException) {
            // Coroutine iptalini yutma: yapısal eşzamanlılığın çalışması için tekrar fırlatılmalı.
            throw e
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Güncelleme kontrol edilemedi, internetini kontrol et.")
        }
    }

    /**
     * "v1.2.0", "1.2.0-beta" gibi sürüm etiketlerini sayısal parçalara ayırır.
     * Bir parça tamamen sayı değilse (ör. "0-beta"), baştaki sayısal kısmı alır;
     * hiç sayı yoksa 0 kabul eder. Böylece parça tamamen atılıp diğer parçaların
     * kayması engellenmiş olur (ör. "1.2.0-beta" -> [1, 2, 0], [1,2] değil).
     */
    private fun parseVersion(version: String): List<Int> =
        version.trimStart('v', 'V').split(".").map { part ->
            Regex("""\d+""").find(part)?.value?.toIntOrNull() ?: 0
        }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = parseVersion(latest)
        val currentParts = parseVersion(current)
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l != c) return l > c
        }
        return false
    }
}
package com.altinim.app.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

object ApkSignatureVerifier {

    fun matchesInstalledApp(context: Context, apkFile: File): Boolean {
        val downloaded = fingerprintsOf(archivePath = apkFile.absolutePath, context = context)
        val installed = fingerprintsOf(archivePath = null, context = context)
        if (downloaded.isNullOrEmpty() || installed.isNullOrEmpty()) return false
        return downloaded == installed
    }

    // archivePath == null  -> cihazda kurulu olan Altınım'ın kendi imzası
    // archivePath != null  -> henüz kurulmamış, indirilen .apk dosyasının imzası
    private fun fingerprintsOf(archivePath: String?, context: Context): Set<String>? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val packageInfo: PackageInfo = try {
            if (archivePath != null) {
                context.packageManager.getPackageArchiveInfo(archivePath, flags) ?: return null
            } else {
                context.packageManager.getPackageInfo(context.packageName, flags)
            }
        } catch (e: Exception) {
            return null
        }

        val rawSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return null
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        if (rawSignatures.isNullOrEmpty()) return null
        return rawSignatures.map { sha256(it.toByteArray()) }.toSet()
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
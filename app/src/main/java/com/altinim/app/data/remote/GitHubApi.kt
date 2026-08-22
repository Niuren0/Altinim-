package com.altinim.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)

interface GitHubApi {
    @GET("repos/Niuren0/Altinim/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}
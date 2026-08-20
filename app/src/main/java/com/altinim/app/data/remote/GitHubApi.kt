package com.altinim.app.data.remote

import retrofit2.http.GET

data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    val browser_download_url: String
)

interface GitHubApi {
    @GET("repos/Niuren0/Altinim/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}
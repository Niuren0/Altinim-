package com.altinim.app.data.remote

import okhttp3.Interceptor

object GitHubNetworkModule {

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "Altinim-App")
            .build()
        chain.proceed(request)
    }

    val githubApi: GitHubApi by lazy {
        RetrofitFactory.build(
            baseUrl = "https://api.github.com/",
            userAgentInterceptor
        ).create(GitHubApi::class.java)
    }
}
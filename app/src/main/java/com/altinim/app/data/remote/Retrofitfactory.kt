package com.altinim.app.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitFactory {

    fun build(baseUrl: String, vararg interceptors: Interceptor): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
        interceptors.forEach { clientBuilder.addInterceptor(it) }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
package com.altinim.app.data.remote

import com.altinim.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    // Site tarayıcı olmayan isteklerde bot korumasına takmasın diye
    // normal bir tarayıcı User-Agent'ı ekliyoruz.
    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Altinim-App)"
            )
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val kurpanoApi: KurpanoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://kurpano.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KurpanoApi::class.java)
    }
}
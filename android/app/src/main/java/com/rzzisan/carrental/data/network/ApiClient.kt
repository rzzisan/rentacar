package com.rzzisan.carrental.data.network

import com.rzzisan.carrental.BuildConfig
import com.rzzisan.carrental.data.auth.AuthTokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val httpClient: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        // Bearer token auto-inject
        val authInterceptor = okhttp3.Interceptor { chain ->
            val token = AuthTokenStore.getToken()
            val req = if (token.isNullOrBlank()) chain.request()
                      else chain.request().newBuilder()
                          .addHeader("Authorization", "Bearer $token")
                          .build()
            chain.proceed(req)
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logger)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}

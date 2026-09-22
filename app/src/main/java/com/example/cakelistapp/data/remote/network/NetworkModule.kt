package com.example.cakelistapp.data.remote.network

import com.example.cakelistapp.BuildConfig
import com.example.cakelistapp.data.remote.api.CakeApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://raw.githubusercontent.com/"
    private const val TIMEOUT_SECONDS = 15L
    private const val GITHUB_RAW_HOST = "raw.githubusercontent.com"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(JsonContentTypeInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(debugHttpLogger())
            }
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val cakeApi: CakeApi = retrofit.create(CakeApi::class.java)

    private fun debugHttpLogger(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            CakeApiLog.debug(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }
    }

    /**
     * GitHub raw responses are served as text/plain. Rewrite only that host so Retrofit
     * can decode JSON without accepting arbitrary content types.
     */
    private class JsonContentTypeInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            if (response.request.url.host != GITHUB_RAW_HOST) return response
            return response.newBuilder()
                .header("Content-Type", "application/json; charset=utf-8")
                .build()
        }
    }
}

package com.example.cakelistapp.data.remote.network

import com.example.cakelistapp.BuildConfig
import com.example.cakelistapp.data.remote.api.CakeApi
import kotlinx.serialization.json.Json
import okhttp3.Call
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
    private const val IMAGE_CALL_TIMEOUT_SECONDS = 30L
    private const val GITHUB_RAW_HOST = "raw.githubusercontent.com"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * Single transport policy for all egress. Every outbound client is derived from this one so
     * connection pool, TLS configuration and timeouts stay consistent and any future certificate
     * pinning applies to API and image traffic alike.
     */
    // TODO: Add certificate pinning here if the endpoint ever serves authenticated or sensitive
    //  data; every client derives from baseClient so it applies to API and image traffic at once.
    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val okHttpClient: OkHttpClient = baseClient.newBuilder()
        .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(JsonContentTypeInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(debugHttpLogger(HttpLoggingInterceptor.Level.BODY))
            }
        }
        .build()

    /**
     * Client used by the image loader. Logging is capped at BASIC because image payloads are
     * binary and the JSON content-type rewrite must not apply to them. The longer call timeout
     * accounts for image payloads being an order of magnitude larger than the cake list.
     */
    val imageCallFactory: Call.Factory by lazy {
        baseClient.newBuilder()
            .callTimeout(IMAGE_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(debugHttpLogger(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val cakeApi: CakeApi = retrofit.create(CakeApi::class.java)

    private fun debugHttpLogger(level: HttpLoggingInterceptor.Level): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            CakeApiLog.debug(message)
        }.apply {
            this.level = level
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

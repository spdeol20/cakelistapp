package com.example.cakelistapp.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.example.cakelistapp.data.remote.network.NetworkModule
import com.example.cakelistapp.data.repository.CakeRepositoryImpl
import com.example.cakelistapp.domain.repository.CakeRepository

object AppContainer {
    val cakeRepository: CakeRepository by lazy {
        CakeRepositoryImpl(NetworkModule.cakeApi)
    }

    /**
     * Image loading is routed through the application OkHttp stack rather than Coil's default
     * client so image egress inherits the same transport hardening as API traffic.
     *
     * Crossfade is disabled deliberately: Coil skips the animation on memory cache hits but not
     * on disk cache hits, so leaving it on makes every cold start fade each thumbnail in and
     * reads to the user as the list loading a second time.
     */
    fun imageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { NetworkModule.imageCallFactory }))
            }
            .crossfade(false)
            .build()
    }
}

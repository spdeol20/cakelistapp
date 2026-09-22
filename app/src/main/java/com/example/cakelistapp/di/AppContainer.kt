package com.example.cakelistapp.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.cakelistapp.data.remote.network.NetworkModule
import com.example.cakelistapp.data.repository.CakeRepositoryImpl
import com.example.cakelistapp.domain.repository.CakeRepository

// TODO: Replace manual construction with Hilt if the dependency graph grows beyond these two
//  objects; a DI framework is not yet justified at this size.
object AppContainer {
    val cakeRepository: CakeRepository by lazy {
        CakeRepositoryImpl(NetworkModule.cakeApi)
    }

    /**
     * Image loading is routed through the application OkHttp stack rather than Coil's default
     * client so image egress inherits the same transport hardening as API traffic.
     *
     * Crossfade is left at Coil's default of off. It is skipped on memory cache hits but not on
     * disk cache hits, so enabling it would fade each thumbnail in on every cold start and read
     * to the user as the list loading a second time.
     */
    fun imageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { NetworkModule.imageCallFactory }))
            }
            .build()
    }
}

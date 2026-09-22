package com.example.cakelistapp

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.example.cakelistapp.di.AppContainer

/**
 * Registers the application wide [ImageLoader]. Binding it here rather than at a composable call
 * site means no screen can fall back to Coil's default loader, which would bypass the hardened
 * OkHttp stack in NetworkModule.
 */
class CakeListApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return AppContainer.imageLoader(context)
    }
}

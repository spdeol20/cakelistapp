package com.example.cakelistapp.data.remote.network

import android.util.Log
import com.example.cakelistapp.BuildConfig

internal object CakeApiLog {
    const val TAG = "CakeApi"

    fun debug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
}

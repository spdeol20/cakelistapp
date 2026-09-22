package com.example.cakelistapp.di

import com.example.cakelistapp.data.remote.network.NetworkModule
import com.example.cakelistapp.data.repository.CakeRepositoryImpl
import com.example.cakelistapp.domain.repository.CakeRepository

object AppContainer {
    val cakeRepository: CakeRepository by lazy {
        CakeRepositoryImpl(NetworkModule.cakeApi)
    }
}

package com.example.cakelistapp.domain.repository

import com.example.cakelistapp.domain.model.Cake

interface CakeRepository {
    suspend fun getCakes(): List<Cake>
}

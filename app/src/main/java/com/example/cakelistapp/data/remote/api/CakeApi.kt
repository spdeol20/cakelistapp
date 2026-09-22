package com.example.cakelistapp.data.remote.api

import com.example.cakelistapp.data.remote.dto.CakeDto
import retrofit2.http.GET

interface CakeApi {
    @GET("Waracle/mobile-coding-test-api/refs/heads/main/cakes")
    suspend fun getCakes(): List<CakeDto>
}

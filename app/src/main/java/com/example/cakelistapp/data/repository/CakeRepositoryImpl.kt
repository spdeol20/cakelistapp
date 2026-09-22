package com.example.cakelistapp.data.repository

import com.example.cakelistapp.data.remote.api.CakeApi
import com.example.cakelistapp.data.remote.mapper.toUniqueSortedCakes
import com.example.cakelistapp.data.remote.network.CakeApiLog
import com.example.cakelistapp.domain.model.Cake
import com.example.cakelistapp.domain.repository.CakeRepository

class CakeRepositoryImpl(
    private val api: CakeApi,
) : CakeRepository {
    override suspend fun getCakes(): List<Cake> {
        CakeApiLog.debug("Calling cakes API")
        val cakes = api.getCakes().toUniqueSortedCakes()
        CakeApiLog.debug("Cakes API success: ${cakes.size} items")
        return cakes
    }
}

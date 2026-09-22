package com.example.cakelistapp.data.repository

import com.example.cakelistapp.data.remote.api.CakeApi
import com.example.cakelistapp.data.remote.dto.CakeDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CakeRepositoryImplTest {

    @Test
    fun getCakes_mapsDeduplicatesAndSorts() = runTest {
        val api = FakeCakeApi {
            listOf(
                CakeDto(title = "Rocky Road", desc = "later", image = "https://example.com/r.jpg"),
                CakeDto(title = "Banana Cake", desc = "first", image = "https://example.com/b.jpg"),
                CakeDto(title = "Rocky Road", desc = "duplicate", image = "https://example.com/r2.jpg"),
            )
        }
        val repository = CakeRepositoryImpl(api)

        val cakes = repository.getCakes()

        assertEquals(listOf("Banana Cake", "Rocky Road"), cakes.map { it.title })
    }
}

private class FakeCakeApi(
    private val response: suspend () -> List<CakeDto>,
) : CakeApi {
    override suspend fun getCakes(): List<CakeDto> = response()
}

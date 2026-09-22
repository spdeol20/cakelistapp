package com.example.cakelistapp.ui.cakes

import com.example.cakelistapp.MainDispatcherRule
import com.example.cakelistapp.data.remote.api.CakeApi
import com.example.cakelistapp.data.remote.dto.CakeDto
import com.example.cakelistapp.data.repository.CakeRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CakeListStartupTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun whenAppStarted_andLoadSucceeds_presentsUniqueSortedCakes() = runTest {
        val viewModel = CakeListViewModel(
            repository = CakeRepositoryImpl(
                FakeCakeApi {
                    listOf(
                        CakeDto(
                            title = "Rocky Road",
                            desc = "Eat it, don't drive on it!",
                            image = "https://example.com/r.jpg",
                        ),
                        CakeDto(
                            title = "Banana Cake",
                            desc = "Donkey kongs favourite",
                            image = "https://example.com/b.jpg",
                        ),
                        CakeDto(
                            title = "Rocky Road",
                            desc = "duplicate",
                            image = "https://example.com/r2.jpg",
                        ),
                    )
                },
            ),
            loadErrorMessage = ERROR_MESSAGE,
        )

        val state = viewModel.uiState.value

        assertEquals(listOf("Banana Cake", "Rocky Road"), state.cakes.map { it.title })
        assertEquals("Eat it, don't drive on it!", state.cakes.single { it.title == "Rocky Road" }.description)
        assertFalse(state.isInitialLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
    }

    @Test
    fun whenAppStarted_andLoadFails_presentsError() = runTest {
        val viewModel = CakeListViewModel(
            repository = CakeRepositoryImpl(
                FakeCakeApi { error("network down") },
            ),
            loadErrorMessage = ERROR_MESSAGE,
        )

        val state = viewModel.uiState.value

        assertTrue(state.cakes.isEmpty())
        assertFalse(state.isInitialLoading)
        assertEquals(ERROR_MESSAGE, state.errorMessage)
    }

    private companion object {
        const val ERROR_MESSAGE = "Unable to load cakes. Check your connection and try again."
    }
}

private class FakeCakeApi(
    private val response: suspend () -> List<CakeDto>,
) : CakeApi {
    override suspend fun getCakes(): List<CakeDto> = response()
}

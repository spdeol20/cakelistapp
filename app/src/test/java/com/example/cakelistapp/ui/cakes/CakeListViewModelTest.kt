package com.example.cakelistapp.ui.cakes

import com.example.cakelistapp.MainDispatcherRule
import com.example.cakelistapp.domain.model.Cake
import com.example.cakelistapp.domain.repository.CakeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CakeListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleCakes = listOf(
        Cake("Banana Cake", "Donkey kongs favourite", "https://example.com/b.jpg"),
        Cake("Carrot Cake", "Bugs bunnys favourite", "https://example.com/c.jpg"),
    )

    @Test
    fun loadCakes_emitsSuccessState() = runTest {
        val viewModel = CakeListViewModel(
            repository = FakeCakeRepository { sampleCakes },
            loadErrorMessage = ERROR_MESSAGE,
        )

        val state = viewModel.uiState.value

        assertEquals(sampleCakes, state.cakes)
        assertFalse(state.isInitialLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadCakes_onFailure_emitsUserSafeError() = runTest {
        val viewModel = CakeListViewModel(
            repository = FakeCakeRepository { error("network down") },
            loadErrorMessage = ERROR_MESSAGE,
        )

        val state = viewModel.uiState.value

        assertTrue(state.cakes.isEmpty())
        assertFalse(state.isInitialLoading)
        assertEquals(ERROR_MESSAGE, state.errorMessage)
    }

    @Test
    fun refresh_onFailure_keepsExistingCakes() = runTest {
        var shouldFail = false
        val viewModel = CakeListViewModel(
            repository = FakeCakeRepository {
                if (shouldFail) error("refresh failed") else sampleCakes
            },
            loadErrorMessage = ERROR_MESSAGE,
        )

        shouldFail = true
        viewModel.loadCakes(isRefresh = true)

        val state = viewModel.uiState.value
        assertEquals(sampleCakes, state.cakes)
        assertFalse(state.isRefreshing)
        assertEquals(ERROR_MESSAGE, state.errorMessage)
    }

    @Test
    fun onCakeSelected_andDismiss_updatesDialogState() = runTest {
        val viewModel = CakeListViewModel(
            repository = FakeCakeRepository { sampleCakes },
            loadErrorMessage = ERROR_MESSAGE,
        )
        val selected = sampleCakes.first()

        viewModel.onCakeSelected(selected)
        assertEquals(selected, viewModel.uiState.value.selectedCake)

        viewModel.onDialogDismissed()
        assertNull(viewModel.uiState.value.selectedCake)
    }

    private companion object {
        const val ERROR_MESSAGE = "Unable to load cakes. Check your connection and try again."
    }
}

private class FakeCakeRepository(
    private val result: suspend () -> List<Cake>,
) : CakeRepository {
    override suspend fun getCakes(): List<Cake> = result()
}

package com.example.cakelistapp.ui.cakes

import com.example.cakelistapp.domain.model.Cake

data class CakeListUiState(
    val cakes: List<Cake> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedCake: Cake? = null,
)

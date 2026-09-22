package com.example.cakelistapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CakeDto(
    val title: String? = null,
    val desc: String? = null,
    val image: String? = null,
)

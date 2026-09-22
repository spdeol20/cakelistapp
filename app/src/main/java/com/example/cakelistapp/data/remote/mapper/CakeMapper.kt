package com.example.cakelistapp.data.remote.mapper

import com.example.cakelistapp.data.remote.dto.CakeDto
import com.example.cakelistapp.domain.model.Cake
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun CakeDto.toCakeOrNull(): Cake? {
    val sanitizedTitle = title?.trim().orEmpty()
    if (sanitizedTitle.isEmpty()) return null

    val candidateImageUrl = image?.trim().orEmpty()
    val imageUrl = if (isTrustedHttpsUrl(candidateImageUrl)) candidateImageUrl else ""

    return Cake(
        title = sanitizedTitle,
        description = desc?.trim().orEmpty(),
        imageUrl = imageUrl,
    )
}

fun List<CakeDto>.toUniqueSortedCakes(): List<Cake> {
    return mapNotNull { it.toCakeOrNull() }
        .distinctBy { it.title.lowercase() }
        .sortedBy { it.title.lowercase() }
}

internal fun isTrustedHttpsUrl(value: String): Boolean {
    val url = value.toHttpUrlOrNull() ?: return false
    return url.scheme == "https" && url.host.isNotBlank()
}

package com.example.cakelistapp.data.remote.mapper

import com.example.cakelistapp.data.remote.dto.CakeDto
import com.example.cakelistapp.domain.model.Cake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CakeMapperTest {

    @Test
    fun toCakeOrNull_dropsBlankTitle() {
        val dto = CakeDto(title = "  ", desc = "desc", image = "https://example.com/cake.jpg")

        assertNull(dto.toCakeOrNull())
    }

    @Test
    fun toCakeOrNull_mapsHttpsImageAndTrimsFields() {
        val dto = CakeDto(
            title = "  Victoria Sponge  ",
            desc = "  sponge with jam  ",
            image = "https://example.com/cake.jpg",
        )

        assertEquals(
            Cake(
                title = "Victoria Sponge",
                description = "sponge with jam",
                imageUrl = "https://example.com/cake.jpg",
            ),
            dto.toCakeOrNull(),
        )
    }

    @Test
    fun toCakeOrNull_rejectsNonHttpsImageUrl() {
        val dto = CakeDto(
            title = "Donut",
            desc = "Not technically a cake",
            image = "http://example.com/insecure.jpg",
        )

        assertEquals("", dto.toCakeOrNull()?.imageUrl)
    }

    @Test
    fun toCakeOrNull_rejectsUnsupportedSchemes() {
        val dto = CakeDto(
            title = "Donut",
            desc = "desc",
            image = "file:///data/local/tmp/cake.jpg",
        )

        assertEquals("", dto.toCakeOrNull()?.imageUrl)
    }

    @Test
    fun toUniqueSortedCakes_deduplicatesAndSortsByTitle() {
        val cakes = listOf(
            CakeDto(title = "Carrot Cake", desc = "first", image = "https://example.com/a.jpg"),
            CakeDto(title = "banana cake", desc = "second", image = "https://example.com/b.jpg"),
            CakeDto(title = "Carrot Cake", desc = "duplicate", image = "https://example.com/c.jpg"),
            CakeDto(title = null, desc = "ignored", image = "https://example.com/d.jpg"),
        ).toUniqueSortedCakes()

        assertEquals(listOf("banana cake", "Carrot Cake"), cakes.map(Cake::title))
        assertEquals("first", cakes.single { it.title == "Carrot Cake" }.description)
        assertTrue(cakes.none { it.title.isBlank() })
    }
}

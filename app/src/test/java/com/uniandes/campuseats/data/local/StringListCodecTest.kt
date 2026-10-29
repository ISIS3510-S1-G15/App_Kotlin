package com.uniandes.campuseats.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringListCodecTest {

    @Test
    fun roundTripKeepsOrderAndSpecialCharacters() {
        val values = listOf("Vegan", "Gluten | free", "Café \"bar\"", "")
        assertEquals(values, StringListCodec.decode(StringListCodec.encode(values)))
    }

    @Test
    fun emptyListRoundTrips() {
        assertTrue(StringListCodec.decode(StringListCodec.encode(emptyList())).isEmpty())
    }

    @Test
    fun corruptInputYieldsEmptyList() {
        assertTrue(StringListCodec.decode("not json").isEmpty())
    }
}

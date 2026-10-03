package com.uniandes.campuseats

import com.uniandes.campuseats.data.isOpenAt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningHoursTest {

    private fun minutes(hour: Int, minute: Int = 0) = hour * 60 + minute

    @Test
    fun open_inside_the_schedule() {
        assertTrue(isOpenAt("6:30 AM – 11:00 PM", minutes(12)))
        assertTrue(isOpenAt("6:30 AM – 11:00 PM", minutes(6, 30)))
    }

    @Test
    fun closed_outside_the_schedule() {
        assertFalse(isOpenAt("6:30 AM – 11:00 PM", minutes(6, 29)))
        assertFalse(isOpenAt("6:30 AM – 11:00 PM", minutes(23)))
        assertFalse(isOpenAt("11:00 AM – 8:00 PM", minutes(21)))
    }

    @Test
    fun noon_and_midnight_use_12_hour_clock() {
        assertTrue(isOpenAt("12:00 PM – 8:00 PM", minutes(12)))
        assertFalse(isOpenAt("12:00 PM – 8:00 PM", minutes(11, 59)))
    }

    @Test
    fun schedule_that_crosses_midnight() {
        assertTrue(isOpenAt("6:00 PM – 2:00 AM", minutes(23)))
        assertTrue(isOpenAt("6:00 PM – 2:00 AM", minutes(1)))
        assertFalse(isOpenAt("6:00 PM – 2:00 AM", minutes(12)))
    }

    @Test
    fun unreadable_schedule_is_assumed_open() {
        assertTrue(isOpenAt("Open 24h", minutes(3)))
    }
}

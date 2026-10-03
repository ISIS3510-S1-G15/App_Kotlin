package com.uniandes.campuseats.data

import java.util.Calendar

private val HoursRegex = Regex("""(\d{1,2}):(\d{2})\s*(A\.?M\.?|P\.?M\.?)""", RegexOption.IGNORE_CASE)

/** Convierte "6:30 AM" en minutos desde medianoche. */
private fun toMinutes(match: MatchResult): Int {
    val (h, m, ampm) = match.destructured
    var hour = h.toInt() % 12
    if (ampm.replace(".", "").equals("PM", ignoreCase = true)) hour += 12
    return hour * 60 + m.toInt()
}

/**
 * Indica si un horario como "6:30 AM – 11:00 PM" está abierto en [minutesOfDay]
 * (minutos desde medianoche). Soporta horarios que cruzan la medianoche.
 * Si el texto no se puede interpretar se asume abierto.
 */
fun isOpenAt(hours: String, minutesOfDay: Int): Boolean {
    val times = HoursRegex.findAll(hours).map(::toMinutes).toList()
    if (times.size < 2) return true
    val open = times[0]
    val close = times[1]
    return if (open <= close) minutesOfDay in open until close
    else minutesOfDay >= open || minutesOfDay < close
}

/** Hora actual del teléfono en minutos desde medianoche. */
fun currentMinutesOfDay(): Int {
    val now = Calendar.getInstance()
    return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
}

/** Abierto según la hora actual del teléfono. */
fun isOpenNow(hours: String): Boolean = isOpenAt(hours, currentMinutesOfDay())

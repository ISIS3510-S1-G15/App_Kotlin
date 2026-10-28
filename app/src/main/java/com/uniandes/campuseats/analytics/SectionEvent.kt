package com.uniandes.campuseats.analytics

data class SectionEvent(
    val eventId: String,
    val type: Type,
    val section: AppSection,
    val action: SectionAction?,
    val visitId: String,
    val timestamp: String
) {
    enum class Type(val wireName: String) {
        View("section_view"),
        Interaction("section_interaction")
    }

    fun toBody(): Map<String, Any> {
        val body = mutableMapOf<String, Any>(
            "event_id" to eventId,
            "event_type" to type.wireName,
            "section" to section.wireName,
            "visit_id" to visitId,
            "client_timestamp" to timestamp
        )
        action?.let { body["action"] = it.wireName }
        return body
    }
}

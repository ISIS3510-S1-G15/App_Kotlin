package com.uniandes.campuseats.data

data class CatalogOption(
    val value: String,
    val label: String,
    val detail: String?,
    val emoji: String?,
    val extra: Map<String, Any> = emptyMap()
) {
    fun number(key: String): Double? = (extra[key] as? Number)?.toDouble()

    fun strings(key: String): List<String> = (extra[key] as? List<*>).orEmpty().filterIsInstance<String>()
}

class Catalog(private val groups: Map<String, List<CatalogOption>>) {

    fun options(group: String): List<CatalogOption> = groups[group].orEmpty()

    fun labels(group: String): List<String> = options(group).map { it.label }

    fun label(group: String, value: String): String? = options(group).firstOrNull { it.value == value }?.label

    fun number(group: String, value: String, key: String = "number"): Double? =
        options(group).firstOrNull { it.value == value }?.number(key)

    val isEmpty: Boolean get() = groups.isEmpty()

    companion object {
        val Empty = Catalog(emptyMap())
    }
}

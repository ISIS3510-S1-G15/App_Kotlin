package com.uniandes.campuseats.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/** Qué tan agresivo es el cache respecto a los TTL base de [com.uniandes.campuseats.data.cache.CacheTtl]. */
enum class CacheFreshness(val multiplier: Double) {
    SHORT(0.5),
    NORMAL(1.0),
    LONG(3.0)
}

data class UserPreferences(
    val syncOnlyOnWifi: Boolean = false,
    val backgroundPrefetch: Boolean = true,
    val dataSaver: Boolean = false,
    val memoryCacheMb: Int = DEFAULT_MEMORY_CACHE_MB,
    val persistCacheOnDisk: Boolean = true,
    val cacheFreshness: CacheFreshness = CacheFreshness.NORMAL,
    val maxRecentSearches: Int = DEFAULT_RECENT_SEARCHES,
    val rememberHomeFilters: Boolean = true
) {
    /** Entradas del cache de objetos en memoria: ~8 entradas por MB configurado. */
    val memoryCacheEntries: Int get() = memoryCacheMb * ENTRIES_PER_MB

    companion object {
        const val DEFAULT_MEMORY_CACHE_MB = 16
        const val DEFAULT_RECENT_SEARCHES = 5
        const val ENTRIES_PER_MB = 8
        val MEMORY_CACHE_OPTIONS_MB = listOf(8, 16, 32, 64)
        val RECENT_SEARCH_OPTIONS = listOf(3, 5, 8, 10)
    }
}

class HomeFilterState(
    val category: String = "All",
    val openNow: Boolean = false,
    val savedOnly: Boolean = false,
    val prices: Set<String> = emptySet(),
    val minRating: Double = 0.0
)

/**
 * Preferencias del usuario sobre Preferences DataStore (reemplaza SharedPreferences).
 * [current] es una instantánea síncrona para código que no puede suspender (políticas de cache, Coil...).
 */
class UserPreferencesRepository(private val store: DataStore<Preferences>) {

    private val _current = MutableStateFlow(UserPreferences())
    val current: StateFlow<UserPreferences> = _current.asStateFlow()

    val flow: Flow<UserPreferences> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toUserPreferences() }

    /** Mantiene [current] sincronizado mientras viva [scope]. */
    fun start(scope: CoroutineScope) {
        flow.onEach { _current.value = it }.launchIn(scope)
    }

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(_current.value)
        _current.value = next
        store.edit { prefs ->
            prefs[SYNC_WIFI] = next.syncOnlyOnWifi
            prefs[PREFETCH] = next.backgroundPrefetch
            prefs[DATA_SAVER] = next.dataSaver
            prefs[MEMORY_MB] = next.memoryCacheMb
            prefs[PERSIST_DISK] = next.persistCacheOnDisk
            prefs[FRESHNESS] = next.cacheFreshness.name
            prefs[MAX_RECENT] = next.maxRecentSearches
            prefs[REMEMBER_FILTERS] = next.rememberHomeFilters
        }
    }

    // ---- filtros del Home (antes HomeFilterStore con SharedPreferences) ----

    suspend fun loadHomeFilters(): HomeFilterState {
        val prefs = runCatching { store.data.first() }.getOrDefault(emptyPreferences())
        return HomeFilterState(
            category = prefs[F_CATEGORY] ?: "All",
            openNow = prefs[F_OPEN_NOW] ?: false,
            savedOnly = prefs[F_SAVED_ONLY] ?: false,
            prices = prefs[F_PRICES].orEmpty(),
            minRating = prefs[F_MIN_RATING]?.toDoubleOrNull() ?: 0.0
        )
    }

    suspend fun saveHomeFilters(state: HomeFilterState) {
        store.edit {
            it[F_CATEGORY] = state.category
            it[F_OPEN_NOW] = state.openNow
            it[F_SAVED_ONLY] = state.savedOnly
            it[F_PRICES] = state.prices
            it[F_MIN_RATING] = state.minRating.toString()
        }
    }

    /** Borra solo los filtros (datos de usuario) y conserva los ajustes de la app (tema, cache...). */
    suspend fun clearHomeFilters() {
        store.edit {
            it.remove(F_CATEGORY)
            it.remove(F_OPEN_NOW)
            it.remove(F_SAVED_ONLY)
            it.remove(F_PRICES)
            it.remove(F_MIN_RATING)
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val d = UserPreferences()
        return UserPreferences(
            syncOnlyOnWifi = this[SYNC_WIFI] ?: d.syncOnlyOnWifi,
            backgroundPrefetch = this[PREFETCH] ?: d.backgroundPrefetch,
            dataSaver = this[DATA_SAVER] ?: d.dataSaver,
            memoryCacheMb = this[MEMORY_MB] ?: d.memoryCacheMb,
            persistCacheOnDisk = this[PERSIST_DISK] ?: d.persistCacheOnDisk,
            cacheFreshness = this[FRESHNESS]?.let { runCatching { CacheFreshness.valueOf(it) }.getOrNull() } ?: d.cacheFreshness,
            maxRecentSearches = this[MAX_RECENT] ?: d.maxRecentSearches,
            rememberHomeFilters = this[REMEMBER_FILTERS] ?: d.rememberHomeFilters
        )
    }

    companion object {
        private val SYNC_WIFI = booleanPreferencesKey("pref_sync_only_wifi")
        private val PREFETCH = booleanPreferencesKey("pref_background_prefetch")
        private val DATA_SAVER = booleanPreferencesKey("pref_data_saver")
        private val MEMORY_MB = intPreferencesKey("pref_memory_cache_mb")
        private val PERSIST_DISK = booleanPreferencesKey("pref_persist_cache_disk")
        private val FRESHNESS = stringPreferencesKey("pref_cache_freshness")
        private val MAX_RECENT = intPreferencesKey("pref_max_recent_searches")
        private val REMEMBER_FILTERS = booleanPreferencesKey("pref_remember_home_filters")

        // Mismas llaves que el antiguo SharedPreferences "campus_eats_home_filters" para que la migración las copie tal cual
        private val F_CATEGORY = stringPreferencesKey("category")
        private val F_OPEN_NOW = booleanPreferencesKey("open_now")
        private val F_SAVED_ONLY = booleanPreferencesKey("saved_only")
        private val F_PRICES = stringSetPreferencesKey("prices")
        private val F_MIN_RATING = stringPreferencesKey("min_rating")

        fun create(context: Context, scope: CoroutineScope): UserPreferencesRepository {
            val app = context.applicationContext
            val store = PreferenceDataStoreFactory.create(
                migrations = listOf(SharedPreferencesMigration(app, "campus_eats_home_filters")),
                scope = scope,
                produceFile = { app.preferencesDataStoreFile("user_preferences") }
            )
            return UserPreferencesRepository(store).also { it.start(scope) }
        }
    }
}

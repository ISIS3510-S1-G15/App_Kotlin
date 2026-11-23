package com.uniandes.campuseats.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.uniandes.campuseats.data.prefs.CacheFreshness
import com.uniandes.campuseats.data.prefs.HomeFilterState
import com.uniandes.campuseats.data.prefs.UserPreferences
import com.uniandes.campuseats.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.repository() = UserPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("prefs-${System.nanoTime()}.preferences_pb") }
    )

    @Test
    fun defaultsAreSensible() = runTest(UnconfinedTestDispatcher()) {
        val prefs = repository().flow.first()
        assertEquals(UserPreferences(), prefs)
        assertTrue(prefs.persistCacheOnDisk)
        assertEquals(CacheFreshness.NORMAL, prefs.cacheFreshness)
    }

    @Test
    fun updatesArePersistedAndExposedSynchronously() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.update { it.copy(syncOnlyOnWifi = true, memoryCacheMb = 32, persistCacheOnDisk = false, cacheFreshness = CacheFreshness.LONG) }

        val stored = repo.flow.first()
        assertTrue(stored.syncOnlyOnWifi)
        assertEquals(32, stored.memoryCacheMb)
        assertEquals(32 * UserPreferences.ENTRIES_PER_MB, stored.memoryCacheEntries)
        assertFalse(stored.persistCacheOnDisk)
        assertEquals(CacheFreshness.LONG, stored.cacheFreshness)
        assertEquals(stored, repo.current.value)
    }

    @Test
    fun clearingHomeFiltersKeepsAppSettings() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository()
        repo.update { it.copy(dataSaver = true) }
        repo.saveHomeFilters(HomeFilterState("Cafe", openNow = true, prices = setOf("$", "$$"), minRating = 4.0))

        val loaded = repo.loadHomeFilters()
        assertEquals("Cafe", loaded.category)
        assertTrue(loaded.openNow)
        assertEquals(setOf("$", "$$"), loaded.prices)
        assertEquals(4.0, loaded.minRating, 0.0)

        repo.clearHomeFilters()
        assertEquals("All", repo.loadHomeFilters().category)
        assertTrue(repo.flow.first().dataSaver)
    }
}

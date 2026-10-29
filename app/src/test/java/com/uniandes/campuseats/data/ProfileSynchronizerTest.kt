package com.uniandes.campuseats.data

import com.uniandes.campuseats.data.remote.ProfileLocalSource
import com.uniandes.campuseats.data.remote.ProfileRepository
import com.uniandes.campuseats.data.remote.ProfileSynchronizer
import com.uniandes.campuseats.data.remote.StoredProfile
import com.uniandes.campuseats.net.FakeRemoteApi
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSynchronizerTest {

    private class MemoryProfileSource(var stored: StoredProfile? = null) : ProfileLocalSource {
        override suspend fun loadProfileRecord() = stored

        override suspend fun saveProfile(profile: Profile, pendingSync: Boolean) {
            stored = StoredProfile(profile, pendingSync)
        }

        override suspend fun markProfileSynced(profile: Profile) {
            val current = stored ?: return
            if (current.profile == profile) stored = StoredProfile(profile, false)
        }
    }

    private val local = emptyProfile.copy(name = "Local", budget = "Under 8.000", cuisinePreferences = listOf("Asian"))

    private val remoteJson = """{"name":"Remote","cuisines":["Italian"],"dietary_restrictions":[],
        "meal_frequency":"Daily","meal_times":[],"budget":"","priorities":[],"disliked_foods":"","completed":true}"""

    private fun onlineApi() = FakeRemoteApi().apply { respond("/me/profile", remoteJson) }

    private fun offlineApi() = FakeRemoteApi(failure = IOException("offline"))

    @Test
    fun pendingLocalProfileIsNotOverwrittenByTheRemoteOne() = runTest {
        val source = MemoryProfileSource(StoredProfile(local, pendingSync = true))
        val api = onlineApi()
        val resolved = ProfileSynchronizer(source, ProfileRepository(api)).resolve("Fallback")
        assertEquals(local, resolved)
        assertEquals(local, source.stored?.profile)
        assertFalse(source.stored!!.pendingSync)
        assertEquals(listOf("PUT"), api.calls.map { it.first })
    }

    @Test
    fun pendingLocalProfileStaysPendingWhileOffline() = runTest {
        val source = MemoryProfileSource(StoredProfile(local, pendingSync = true))
        val resolved = ProfileSynchronizer(source, ProfileRepository(offlineApi())).resolve("Fallback")
        assertEquals(local, resolved)
        assertTrue(source.stored!!.pendingSync)
    }

    @Test
    fun syncedLocalProfileIsReplacedByTheRemoteOne() = runTest {
        val source = MemoryProfileSource(StoredProfile(local, pendingSync = false))
        val resolved = ProfileSynchronizer(source, ProfileRepository(onlineApi())).resolve("Fallback")
        assertEquals("Remote", resolved?.name)
        assertEquals(listOf("Italian"), source.stored?.profile?.cuisinePreferences)
        assertFalse(source.stored!!.pendingSync)
    }

    @Test
    fun blankRemoteNameFallsBackToTheAccountName() = runTest {
        val api = FakeRemoteApi().apply {
            respond("/me/profile", remoteJson.replace("\"name\":\"Remote\"", "\"name\":\"\""))
        }
        val resolved = ProfileSynchronizer(MemoryProfileSource(), ProfileRepository(api)).resolve("Account name")
        assertEquals("Account name", resolved?.name)
    }

    @Test
    fun offlineKeepsTheStoredProfile() = runTest {
        val source = MemoryProfileSource(StoredProfile(local, pendingSync = false))
        assertEquals(local, ProfileSynchronizer(source, ProfileRepository(offlineApi())).resolve("Fallback"))
    }

    @Test
    fun offlineWithoutStoredProfileResolvesToNull() = runTest {
        assertNull(ProfileSynchronizer(MemoryProfileSource(), ProfileRepository(offlineApi())).resolve("Fallback"))
    }

    @Test
    fun updateMarksPendingUntilTheServerAcknowledges() = runTest {
        val offlineSource = MemoryProfileSource()
        ProfileSynchronizer(offlineSource, ProfileRepository(offlineApi())).update(local)
        assertEquals(local, offlineSource.stored?.profile)
        assertTrue(offlineSource.stored!!.pendingSync)

        val onlineSource = MemoryProfileSource()
        ProfileSynchronizer(onlineSource, ProfileRepository(onlineApi())).update(local)
        assertFalse(onlineSource.stored!!.pendingSync)
    }

    @Test
    fun aNewerEditIsNotMarkedSyncedByAnOlderPush() = runTest {
        val source = MemoryProfileSource()
        source.saveProfile(local, pendingSync = true)
        val newer = local.copy(budget = "Over 20.000")
        source.saveProfile(newer, pendingSync = true)
        source.markProfileSynced(local)
        assertEquals(newer, source.stored?.profile)
        assertTrue(source.stored!!.pendingSync)
    }
}

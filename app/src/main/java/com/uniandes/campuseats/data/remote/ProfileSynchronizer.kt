package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.Profile
import com.uniandes.campuseats.net.attemptOrNull

class StoredProfile(val profile: Profile, val pendingSync: Boolean)

interface ProfileLocalSource {
    suspend fun loadProfileRecord(): StoredProfile?
    suspend fun saveProfile(profile: Profile, pendingSync: Boolean)
    suspend fun markProfileSynced(profile: Profile)
}

class ProfileSynchronizer(
    private val local: ProfileLocalSource,
    private val remote: ProfileRepository
) {

    suspend fun update(profile: Profile) {
        local.saveProfile(profile, pendingSync = true)
        val pushed = attemptOrNull {
            remote.save(profile)
            true
        } == true
        if (pushed) local.markProfileSynced(profile)
    }

    suspend fun resolve(fallbackName: String): Profile? {
        val stored = local.loadProfileRecord()
        if (stored != null && stored.pendingSync) {
            update(stored.profile)
            return stored.profile
        }
        val fetched = attemptOrNull { remote.fetch() } ?: return stored?.profile
        val restored = fetched.copy(name = fetched.name.ifBlank { fallbackName })
        local.saveProfile(restored, pendingSync = false)
        return restored
    }
}

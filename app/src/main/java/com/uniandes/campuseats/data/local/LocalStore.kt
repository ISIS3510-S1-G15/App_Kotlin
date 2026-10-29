package com.uniandes.campuseats.data.local

import androidx.room.withTransaction
import com.uniandes.campuseats.data.Profile
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings

class LocalStore(private val database: AppDatabase) {

    private val dao = database.dao()

    suspend fun loadReviews(): List<Review> = dao.reviews().map { it.toReview() }

    suspend fun saveReviews(reviews: List<Review>) {
        val rows = reviews.mapIndexed { index, review -> review.toEntity(index) }
        database.withTransaction {
            dao.clearReviews()
            dao.insertReviews(rows)
        }
    }

    suspend fun loadCrowding(): Map<String, List<Int>> =
        dao.crowdingReports().groupBy({ it.restaurantId }, { it.level })

    suspend fun saveCrowding(crowding: Map<String, List<Int>>) {
        val rows = crowding.flatMap { (restaurantId, levels) ->
            levels.map { CrowdingReportEntity(restaurantId = restaurantId, level = it) }
        }
        database.withTransaction {
            dao.clearCrowding()
            dao.insertCrowding(rows)
        }
    }

    suspend fun loadSavedIds(): Set<String> = dao.savedRestaurants().map { it.restaurantId }.toSet()

    suspend fun saveSavedIds(ids: Set<String>) {
        database.withTransaction {
            dao.clearSaved()
            dao.insertSaved(ids.map { SavedRestaurantEntity(it) })
        }
    }

    suspend fun loadRecentSearches(): List<String> = dao.recentSearches().map { it.name }

    suspend fun saveRecentSearches(names: List<String>) {
        database.withTransaction {
            dao.clearRecentSearches()
            dao.insertRecentSearches(names.mapIndexed { index, name -> RecentSearchEntity(index, name) })
        }
    }

    suspend fun loadProfile(): Profile? = dao.profile()?.toProfile()

    suspend fun saveProfile(profile: Profile) {
        dao.upsertProfile(profile.toEntity())
    }

    suspend fun clearUserData() {
        database.withTransaction {
            dao.clearProfile()
            dao.clearSaved()
        }
    }

    suspend fun loadDocument(key: String): CacheDocumentEntity? = dao.document(key)

    suspend fun saveDocument(key: String, payload: String, fetchedAt: Long) {
        dao.upsertDocument(CacheDocumentEntity(key, payload, fetchedAt))
    }
}

private fun Review.toEntity(position: Int) = ReviewEntity(
    id = id,
    position = position,
    restaurantId = restaurantId,
    author = author,
    date = date,
    taste = ratings.taste,
    attention = ratings.attention,
    price = ratings.price,
    options = ratings.options,
    dietaryOptions = StringListCodec.encode(dietaryOptions),
    comment = comment
)

private fun ReviewEntity.toReview() = Review(
    id = id,
    restaurantId = restaurantId,
    author = author,
    date = date,
    ratings = ReviewRatings(taste = taste, attention = attention, price = price, options = options),
    dietaryOptions = StringListCodec.decode(dietaryOptions),
    comment = comment
)

private fun Profile.toEntity() = ProfileEntity(
    name = name,
    frequency = frequency,
    budget = budget,
    dietaryRestrictions = StringListCodec.encode(dietaryRestrictions),
    cuisinePreferences = StringListCodec.encode(cuisinePreferences),
    usualMealTimes = StringListCodec.encode(usualMealTimes),
    topPriorities = StringListCodec.encode(topPriorities),
    foodsToAvoid = StringListCodec.encode(foodsToAvoid)
)

private fun ProfileEntity.toProfile() = Profile(
    name = name,
    frequency = frequency,
    budget = budget,
    dietaryRestrictions = StringListCodec.decode(dietaryRestrictions),
    cuisinePreferences = StringListCodec.decode(cuisinePreferences),
    usualMealTimes = StringListCodec.decode(usualMealTimes),
    topPriorities = StringListCodec.decode(topPriorities),
    foodsToAvoid = StringListCodec.decode(foodsToAvoid)
)

package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.data.cache.CacheTtl
import com.uniandes.campuseats.data.cache.DocumentCache
import com.uniandes.campuseats.net.RemoteApi
import com.uniandes.campuseats.net.attemptOrNull
import org.json.JSONObject

private const val MAX_COMMENT = 300
private const val MAX_DIETARY_OPTIONS = 10
private const val DATE_LENGTH = 10

data class RatingSummary(val restaurantId: String, val rating: Double, val reviewCount: Int)

fun applyRatingSummaries(restaurants: List<Restaurant>, summaries: Map<String, RatingSummary>): List<Restaurant> =
    restaurants.map { restaurant ->
        summaries[restaurant.id]?.let { restaurant.copy(rating = it.rating, reviews = it.reviewCount) } ?: restaurant
    }

fun parseSummaries(json: JSONObject): Map<String, RatingSummary> {
    val items = json.optJSONArray("items") ?: return emptyMap()
    return (0 until items.length())
        .map { items.getJSONObject(it) }
        .filterNot { it.isNull("rating") }
        .associate {
            val id = it.getString("restaurant_id")
            id to RatingSummary(id, it.getDouble("rating"), it.getInt("review_count"))
        }
}

fun reviewFromRemote(json: JSONObject) = Review(
    id = json.get("id").toString(),
    restaurantId = json.getString("restaurant_id"),
    author = json.getString("author"),
    date = json.getString("created_at").take(DATE_LENGTH),
    ratings = ReviewRatings(
        taste = json.getInt("taste"),
        attention = json.getInt("attention"),
        price = json.getInt("price"),
        options = json.getInt("options")
    ),
    dietaryOptions = json.optJSONArray("dietary_options").toStringList(),
    comment = json.optString("comment")
)

class ReviewRepository(
    private val api: RemoteApi,
    private val cache: DocumentCache,
    private val policy: CachePolicy = CachePolicy()
) {

    suspend fun summaries(): Map<String, RatingSummary> {
        val json = api.get("/reviews/summary")
        val parsed = parseSummaries(json)
        cache.save(CacheKeys.REVIEW_SUMMARIES, json.toString(), policy.now())
        return parsed
    }

    suspend fun cachedSummaries(): Map<String, RatingSummary> {
        val raw = attemptOrNull { cache.load(CacheKeys.REVIEW_SUMMARIES) }?.takeIf { it.isNotBlank() }
            ?: return emptyMap()
        val parsed = attemptOrNull { parseSummaries(JSONObject(raw)) }
        if (parsed == null) {
            attemptOrNull { cache.clear(CacheKeys.REVIEW_SUMMARIES) }
            return emptyMap()
        }
        return parsed
    }

    suspend fun revalidateSummaries(force: Boolean = false): Map<String, RatingSummary>? {
        val fresh = !force && policy.isFreshOrFalse(cache, CacheKeys.REVIEW_SUMMARIES, CacheTtl.REVIEW_SUMMARIES)
        return if (fresh) null else summaries()
    }

    suspend fun forRestaurant(restaurantId: String): List<Review> {
        val rows = api.get("/restaurants/$restaurantId/reviews").optJSONArray("reviews")
        val parsed = if (rows == null) emptyList() else (0 until rows.length()).map { reviewFromRemote(rows.getJSONObject(it)) }
        policy.markFetched(cache, CacheKeys.reviews(restaurantId))
        return parsed
    }

    suspend fun revalidate(restaurantId: String, force: Boolean = false): List<Review>? {
        val fresh = !force && policy.isFreshOrFalse(cache, CacheKeys.reviews(restaurantId), CacheTtl.REVIEWS)
        return if (fresh) null else forRestaurant(restaurantId)
    }

    suspend fun invalidate(restaurantId: String) {
        attemptOrNull {
            cache.clear(CacheKeys.reviews(restaurantId))
            cache.clear(CacheKeys.REVIEW_SUMMARIES)
        }
    }

    suspend fun submit(review: Review): Review {
        val created = reviewFromRemote(
            api.post(
                "/restaurants/${review.restaurantId}/reviews",
                mapOf(
                    "taste" to review.ratings.taste,
                    "attention" to review.ratings.attention,
                    "price" to review.ratings.price,
                    "options" to review.ratings.options,
                    "dietary_options" to review.dietaryOptions.take(MAX_DIETARY_OPTIONS),
                    "comment" to review.comment.take(MAX_COMMENT)
                )
            )
        )
        invalidate(review.restaurantId)
        return created
    }
}

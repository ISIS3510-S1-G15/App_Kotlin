package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.net.RemoteApi
import org.json.JSONObject

private const val MAX_COMMENT = 300
private const val MAX_DIETARY_OPTIONS = 10
private const val DATE_LENGTH = 10

data class RatingSummary(val restaurantId: String, val rating: Double, val reviewCount: Int)

fun applyRatingSummaries(restaurants: List<Restaurant>, summaries: Map<String, RatingSummary>): List<Restaurant> =
    restaurants.map { restaurant ->
        summaries[restaurant.id]?.let { restaurant.copy(rating = it.rating, reviews = it.reviewCount) } ?: restaurant
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

class ReviewRepository(private val api: RemoteApi) {

    suspend fun summaries(): Map<String, RatingSummary> {
        val items = api.get("/reviews/summary").optJSONArray("items") ?: return emptyMap()
        return (0 until items.length())
            .map { items.getJSONObject(it) }
            .filterNot { it.isNull("rating") }
            .associate {
                val id = it.getString("restaurant_id")
                id to RatingSummary(id, it.getDouble("rating"), it.getInt("review_count"))
            }
    }

    suspend fun forRestaurant(restaurantId: String): List<Review> {
        val rows = api.get("/restaurants/$restaurantId/reviews").optJSONArray("reviews") ?: return emptyList()
        return (0 until rows.length()).map { reviewFromRemote(rows.getJSONObject(it)) }
    }

    suspend fun submit(review: Review): Review = reviewFromRemote(
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
}

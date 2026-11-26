package com.uniandes.campuseats.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ReviewDraft(
    val taste: Int,
    val attention: Int,
    val price: Int,
    val options: Int,
    val dietaryOptions: List<String>,
    val comment: String
)

class ReviewDraftStore(context: Context) {
    // Aqui definimos que la informacion se guarda en el directorio privado de la app (App-Specific Files)
    private val draftsDir = File(context.applicationContext.filesDir, "review_drafts")

    private fun draftFile(restaurantId: String): File {
        val safeId = restaurantId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(draftsDir, "draft_$safeId.json")
    }

    suspend fun save(restaurantId: String, draft: ReviewDraft) = withContext(Dispatchers.IO) {
        runCatching {
            if (!draftsDir.exists()) draftsDir.mkdirs()
            val json = JSONObject().apply {
                put("taste", draft.taste)
                put("attention", draft.attention)
                put("price", draft.price)
                put("options", draft.options)
                put("dietary_options", JSONArray(draft.dietaryOptions))
                put("comment", draft.comment)
            }
            draftFile(restaurantId).writeText(json.toString())
        }
    }

    suspend fun load(restaurantId: String): ReviewDraft? = withContext(Dispatchers.IO) {
        runCatching {
            val file = draftFile(restaurantId)
            if (!file.exists()) return@withContext null
            val json = JSONObject(file.readText())
            val dietsArray = json.optJSONArray("dietary_options")
            val diets = if (dietsArray == null) {
                emptyList()
            } else {
                (0 until dietsArray.length()).map { dietsArray.getString(it) }
            }
            ReviewDraft(
                taste = json.optInt("taste", 0),
                attention = json.optInt("attention", 0),
                price = json.optInt("price", 0),
                options = json.optInt("options", 0),
                dietaryOptions = diets,
                comment = json.optString("comment", "")
            )
        }.getOrNull()
    }

    suspend fun delete(restaurantId: String) = withContext(Dispatchers.IO) {
        runCatching { draftFile(restaurantId).delete() }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        runCatching { draftsDir.deleteRecursively() }
    }
}
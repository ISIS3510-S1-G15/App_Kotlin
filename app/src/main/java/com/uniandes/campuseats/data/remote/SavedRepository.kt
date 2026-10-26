package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.net.RemoteApi
import org.json.JSONObject

class SavedRepository(private val api: RemoteApi) {

    suspend fun fetch(): Set<String> = parse(api.get("/me/saved"))

    suspend fun add(restaurantId: String): Set<String> = parse(api.put("/me/saved/$restaurantId"))

    suspend fun remove(restaurantId: String): Set<String> = parse(api.delete("/me/saved/$restaurantId"))

    private fun parse(json: JSONObject): Set<String> = json.optJSONArray("restaurant_ids").toStringList().toSet()
}

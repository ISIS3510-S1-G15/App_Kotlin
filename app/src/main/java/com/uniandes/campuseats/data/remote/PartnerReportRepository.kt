package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.net.RemoteApi
import org.json.JSONObject

data class PartnerComplaint(val label: String, val mentions: Int, val sharePct: Double)

data class PartnerReport(
    val restaurantName: String,
    val periodDays: Int,
    val views: Int,
    val uniqueVisitors: Int,
    val goDecisions: Int,
    val viewsChangePct: Double?,
    val reviewCount: Int,
    val avgOverall: Double?,
    val sentimentLabel: String, 
    val positivePct: Double,
    val neutralPct: Double,
    val negativePct: Double,
    val complaints: List<PartnerComplaint>,
    val complaintsHidden: Boolean,
    val highlights: List<String>
)

private fun JSONObject.doubleOrNull(key: String): Double? = if (isNull(key)) null else getDouble(key)

fun parsePartnerReport(json: JSONObject): PartnerReport {
    val visits = json.getJSONObject("visits")
    val sentiment = json.getJSONObject("sentiment")
    val complaintRows = json.optJSONArray("complaints")
    val highlightRows = json.optJSONArray("highlights")
    return PartnerReport(
        restaurantName = json.getString("restaurant_name"),
        periodDays = json.getInt("period_days"),
        views = visits.getInt("views"),
        uniqueVisitors = visits.getInt("unique_visitors"),
        goDecisions = visits.getInt("go_decisions"),
        viewsChangePct = visits.doubleOrNull("views_change_pct"),
        reviewCount = sentiment.getInt("review_count"),
        avgOverall = sentiment.doubleOrNull("avg_overall"),
        sentimentLabel = sentiment.getString("label"),
        positivePct = sentiment.getDouble("positive_pct"),
        neutralPct = sentiment.getDouble("neutral_pct"),
        negativePct = sentiment.getDouble("negative_pct"),
        complaints = (0 until (complaintRows?.length() ?: 0)).map {
            val c = complaintRows!!.getJSONObject(it)
            PartnerComplaint(c.getString("label"), c.getInt("mentions"), c.getDouble("share_pct"))
        },
        complaintsHidden = json.optBoolean("complaints_hidden", false),
        highlights = (0 until (highlightRows?.length() ?: 0)).map { highlightRows!!.getString(it) }
    )
}


class PartnerReportRepository(private val api: RemoteApi) {
    suspend fun report(restaurantId: String, days: Int = 30): PartnerReport =
        parsePartnerReport(api.get("/analytics/partner-report/$restaurantId?days=$days"))
}
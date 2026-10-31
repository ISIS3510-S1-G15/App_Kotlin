package com.uniandes.campuseats.data.remote

import com.uniandes.campuseats.data.cache.CacheKeys
import com.uniandes.campuseats.data.cache.CachePolicy
import com.uniandes.campuseats.data.cache.CacheTtl
import com.uniandes.campuseats.data.cache.DocumentCache
import com.uniandes.campuseats.net.RemoteApi
import com.uniandes.campuseats.net.attemptOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

data class PartnerReportResult(
    val report: PartnerReport,
    val fromCache: Boolean,
    val fetchedAt: Long?,
    val refreshFailed: Boolean = false
)

class PartnerReportRepository(
    private val api: RemoteApi,
    private val cache: DocumentCache,
    private val policy: CachePolicy = CachePolicy()
) {

    suspend fun fetch(restaurantId: String, days: Int, userId: Int): PartnerReport {
        val json = api.get("/analytics/partner-report/$restaurantId?days=$days")
        val report = parsePartnerReport(json)
        attemptOrNull { cache.save(CacheKeys.partnerReport(userId, restaurantId, days), json.toString(), policy.now()) }
        return report
    }

    private suspend fun cached(key: String): PartnerReportResult? {
        val raw = attemptOrNull { cache.load(key) }?.takeIf { it.isNotBlank() } ?: return null
        val report = attemptOrNull { parsePartnerReport(JSONObject(raw)) }
        if (report == null) {
            attemptOrNull { cache.clear(key) }
            return null
        }
        return PartnerReportResult(report, fromCache = true, fetchedAt = attemptOrNull { cache.fetchedAt(key) })
    }

    fun load(restaurantId: String, days: Int, userId: Int, force: Boolean = false): Flow<PartnerReportResult> = flow {
        val key = CacheKeys.partnerReport(userId, restaurantId, days)
        val stored = cached(key)
        if (stored != null) emit(stored)

        val fresh = !force && stored != null &&
            policy.isFreshOrFalse(cache, key, CacheTtl.PARTNER_REPORT)
        if (fresh) return@flow

        try {
            emit(PartnerReportResult(fetch(restaurantId, days, userId), fromCache = false, fetchedAt = policy.now()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (stored == null) throw e
            emit(stored.copy(refreshFailed = true))
        }
    }
}
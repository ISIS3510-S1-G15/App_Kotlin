package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.remote.PartnerReport
import com.uniandes.campuseats.net.ApiException
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import java.util.Locale
import kotlinx.coroutines.CancellationException

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Outline = Color(0xFFE8E0D4)
private val Green = Color(0xFF3A8C4F)
private val Red = Color(0xFFC0392B)
private val Gold = Color(0xFFF4A735)

private val PERIODS = listOf(7, 30, 90)

private sealed interface ReportState {
    data object Loading : ReportState
    data class Error(val message: String) : ReportState
    data class Ready(val report: PartnerReport) : ReportState
}

@Composable
fun PartnerReportView(
    restaurant: Restaurant,
    loadReport: suspend (restaurantId: String, days: Int) -> PartnerReport,
    onBack: () -> Unit
) {
    var days by remember { mutableIntStateOf(30) }
    var state by remember { mutableStateOf<ReportState>(ReportState.Loading) }

    LaunchedEffect(restaurant.id, days) {
        state = ReportState.Loading
        state = try {
            ReportState.Ready(loadReport(restaurant.id, days))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            ReportState.Error(
                if (e.statusCode == 403) "Your account doesn't have access to partner reports."
                else e.detail ?: "Couldn't load the report."
            )
        } catch (e: Exception) {
            ReportState.Error("Couldn't load the report. Check your connection.")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Cream)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Outline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Ink) }
            Column {
                Text("Partner report", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OutfitFontFamily)
                Text(restaurant.name, color = Muted, fontSize = 12.sp, fontFamily = OutfitFontFamily)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PERIODS.forEach { option ->
                val selected = option == days
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Orange else Color.White)
                        .border(1.dp, if (selected) Orange else Outline, RoundedCornerShape(50))
                        .clickable { days = option }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Last $option days",
                        color = if (selected) Color.White else Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFontFamily
                    )
                }
            }
        }

        when (val current = state) {
            ReportState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Loading report…", color = Muted, fontFamily = OutfitFontFamily)
            }
            is ReportState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(current.message, color = Red, fontSize = 14.sp, fontFamily = OutfitFontFamily)
            }
            is ReportState.Ready -> ReportBody(current.report)
        }
    }
}

@Composable
private fun ReportBody(report: PartnerReport) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (report.highlights.isNotEmpty()) {
            Card {
                report.highlights.forEach {
                    Text("• $it", color = Ink, fontSize = 13.sp, fontFamily = OutfitFontFamily)
                }
            }
        }

        SectionTitle("Visit volume")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Views", report.views.toString(), Modifier.weight(1f))
            Stat("Unique visitors", report.uniqueVisitors.toString(), Modifier.weight(1f))
            Stat("Went there", report.goDecisions.toString(), Modifier.weight(1f))
        }
        report.viewsChangePct?.let {
            val up = it >= 0
            Text(
                "${if (up) "▲" else "▼"} ${String.format(Locale.US, "%.1f", kotlin.math.abs(it))}% vs. previous ${report.periodDays} days",
                color = if (up) Green else Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily
            )
        }

        SectionTitle("Overall sentiment")
        Card {
            if (report.avgOverall == null) {
                Text("No reviews in this period.", color = Muted, fontSize = 13.sp, fontFamily = OutfitFontFamily)
            } else {
                Text(
                    "${String.format(Locale.US, "%.1f", report.avgOverall)} / 5 · ${report.sentimentLabel.replaceFirstChar { it.uppercase() }}",
                    color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OutfitFontFamily
                )
                Text("${report.reviewCount} anonymous reviews", color = Muted, fontSize = 12.sp, fontFamily = OutfitFontFamily)
                SplitBar(report.positivePct, report.neutralPct, report.negativePct)
                Text(
                    "${report.positivePct}% positive · ${report.neutralPct}% neutral · ${report.negativePct}% negative",
                    color = Muted, fontSize = 12.sp, fontFamily = OutfitFontFamily
                )
            }
        }

        SectionTitle("Recurring complaints")
        Card {
            when {
                report.complaintsHidden -> Text(
                    "Not enough reviews in this period to show complaints without risking anonymity.",
                    color = Muted, fontSize = 13.sp, fontFamily = OutfitFontFamily
                )
                report.complaints.isEmpty() -> Text(
                    "No complaint repeated in 2 or more reviews. 🎉",
                    color = Green, fontSize = 13.sp, fontFamily = OutfitFontFamily
                )
                else -> report.complaints.forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily)
                        Text("${it.mentions} reviews · ${it.sharePct}%", color = Muted, fontSize = 12.sp, fontFamily = OutfitFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OutfitFontFamily)

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = OutfitFontFamily)
        Text(label, color = Muted, fontSize = 11.sp, fontFamily = OutfitFontFamily)
    }
}

@Composable
private fun SplitBar(positive: Double, neutral: Double, negative: Double) {
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50))) {
        listOf(positive to Green, neutral to Gold, negative to Red).forEach { (pct, color) ->
            if (pct > 0) Box(Modifier.weight(pct.toFloat()).height(10.dp).background(color))
        }
    }
}
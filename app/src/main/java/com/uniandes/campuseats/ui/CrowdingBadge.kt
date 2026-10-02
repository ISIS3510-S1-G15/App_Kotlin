package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.data.avgCrowding
import com.uniandes.campuseats.data.getCrowdingLevel
import kotlin.math.roundToInt

private val BarOff = Color(0xFFD4CAC0)

/** Badge con mini gráfico de barras del nivel de ocupación promedio. size = "sm" | "md". */
@Composable
fun CrowdingBadge(reports: List<Int>, size: String, modifier: Modifier = Modifier) {
    val avg = avgCrowding(reports) ?: return
    val level = getCrowdingLevel(avg)
    val isSm = size == "sm"

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(level.bg)
            .border(1.dp, level.border, CircleShape)
            .padding(
                horizontal = if (isSm) 6.dp else 10.dp,
                vertical = if (isSm) 2.dp else 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CrowdingBars(
            value = avg.roundToInt(),
            activeColor = level.barColor,
            barWidth = if (isSm) 2f else 3f,
            baseHeight = if (isSm) 3f else 4f,
            step = if (isSm) 1.5f else 2f,
            gap = 1.5f
        )
        Text(
            text = level.label,
            color = level.color,
            fontSize = if (isSm) 9.sp else 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** 5 barras crecientes; las primeras [value] se pintan con [activeColor]. */
@Composable
fun CrowdingBars(
    value: Int,
    activeColor: Color,
    barWidth: Float,
    baseHeight: Float,
    step: Float,
    gap: Float,
    inactiveColor: Color = BarOff
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(gap.dp)
    ) {
        (1..5).forEach { bar ->
            Box(
                modifier = Modifier
                    .width(barWidth.dp)
                    .height((baseHeight + bar * step).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (bar <= value) activeColor else inactiveColor)
            )
        }
    }
}

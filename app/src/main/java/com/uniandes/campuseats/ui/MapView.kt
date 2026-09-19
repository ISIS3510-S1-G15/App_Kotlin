package com.uniandes.campuseats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.restaurants
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import kotlin.math.min
import kotlin.math.roundToInt

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Muted = Color(0xFF7A6D5F)
private val BorderColor = Color(0xFFE8E0D4)
private val CardBorder = Color(0xFFF0E8DE)
private val Accent = Color(0xFFE8440A)
private val Green = Color(0xFF3A8C4F)
private val GreenBg = Color(0xFFE8F5EB)
private val ClosedBg = Color(0xFFF0EEEC)
private val StarColor = Color(0xFFF4A735)
private val Disabled = Color(0xFFB8AC9E)

private const val STAR_PATH = "M12 2L15.1 8.3L22 9.3L17 14.2L18.2 21L12 17.8L5.8 21L7 14.2L2 9.3L8.9 8.3L12 2Z"
private const val CHEVRON_PATH = "M9 18L15 12L9 6"

@Composable
private fun PathIcon(
    data: String,
    size: Dp,
    fill: Color? = null,
    stroke: Color? = null,
    strokeWidth: Float = 2f,
    round: Boolean = false
) {
    val path = remember(data) { PathParser().parsePathString(data).toPath() }
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            if (fill != null) {
                drawPath(path, fill)
            }
            if (stroke != null) {
                drawPath(
                    path,
                    stroke,
                    style = Stroke(
                        width = strokeWidth,
                        cap = if (round) StrokeCap.Round else StrokeCap.Butt,
                        join = if (round) StrokeJoin.Round else StrokeJoin.Miter
                    )
                )
            }
        }
    }
}


@Composable
fun MapView(
    onSelect: (Restaurant) -> Unit,
    crowding: Map<String, List<Int>>
) {
    var selected by remember { mutableStateOf<Restaurant?>(null) }
    var filter by remember { mutableStateOf("all") }

    val visible = if (filter == "all") restaurants else restaurants.filter { it.isOpen }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Campus Map",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Ink,
                fontFamily = OutfitFontFamily
            )
            Text(
                text = "Universidad de los Andes — Campus Principal",
                fontSize = 12.sp,
                color = Muted
            )
        }

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allActive = filter == "all"
            Text(
                text = "All Spots",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (allActive) Color.White else Muted,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (allActive) Ink else Color.White)
                    .then(if (!allActive) Modifier.border(1.dp, BorderColor, CircleShape) else Modifier)
                    .clickable { filter = "all" }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            val openActive = filter == "open"
            Text(
                text = "Open Now",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (openActive) Color.White else Muted,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (openActive) Green else Color.White)
                    .then(if (!openActive) Modifier.border(1.dp, BorderColor, CircleShape) else Modifier)
                    .clickable { filter = "open" }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        val mapShape = RoundedCornerShape(24.dp)
        BoxWithConstraints(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(300.dp)
                .shadow(8.dp, mapShape)
                .clip(mapShape)
        ) {
            val boxWidthPx = constraints.maxWidth
            val boxHeightPx = constraints.maxHeight
            val textMeasurer = rememberTextMeasurer()
            val labelFontSize = with(LocalDensity.current) { 6f.toSp() }
            val labelStyle = TextStyle(
                fontSize = labelFontSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5A7A9A),
                fontFamily = FontFamily.SansSerif
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = min(size.width / 350f, size.height / 300f)
                val dx = (size.width - 350f * s) / 2f
                val dy = (size.height - 300f * s) / 2f
                translate(dx, dy) {
                    scale(s, s, pivot = Offset.Zero) {
                        drawRect(Color(0xFFD4E8C2), Offset.Zero, Size(350f, 300f))

                        val pathColor = Color(0xFFC5D9B2)
                        val p1 = Path().apply {
                            moveTo(0f, 150f)
                            quadraticBezierTo(100f, 140f, 175f, 150f)
                            quadraticBezierTo(250f, 160f, 350f, 145f)
                        }
                        drawPath(p1, pathColor, style = Stroke(width = 18f))
                        val p2 = Path().apply {
                            moveTo(175f, 0f)
                            quadraticBezierTo(180f, 80f, 175f, 150f)
                            quadraticBezierTo(170f, 220f, 175f, 300f)
                        }
                        drawPath(p2, pathColor, style = Stroke(width = 14f))
                        val p3 = Path().apply {
                            moveTo(0f, 80f)
                            quadraticBezierTo(80f, 85f, 175f, 80f)
                            quadraticBezierTo(260f, 75f, 350f, 82f)
                        }
                        drawPath(p3, pathColor, style = Stroke(width = 10f))

                        val buildingColor = Color(0xFFA8C4E0)
                        val buildings = listOf(
                            floatArrayOf(30f, 20f, 60f, 45f),
                            floatArrayOf(130f, 30f, 55f, 40f),
                            floatArrayOf(230f, 15f, 70f, 50f),
                            floatArrayOf(40f, 185f, 50f, 55f),
                            floatArrayOf(135f, 200f, 65f, 45f),
                            floatArrayOf(240f, 175f, 75f, 60f)
                        )
                        buildings.forEach { b ->
                            drawRoundRect(
                                color = buildingColor,
                                topLeft = Offset(b[0], b[1]),
                                size = Size(b[2], b[3]),
                                cornerRadius = CornerRadius(4f, 4f),
                                alpha = 0.8f
                            )
                        }

                        drawOval(
                            color = Color(0xFF7AB8D4),
                            topLeft = Offset(157f, 138f),
                            size = Size(36f, 24f),
                            alpha = 0.6f
                        )

                        val labels = listOf(
                            Triple("WEST", 60f, 47f),
                            Triple("UNION", 157f, 52f),
                            Triple("NORTH", 265f, 43f),
                            Triple("ATHLETICS", 65f, 215f),
                            Triple("EAST HALL", 167f, 225f),
                            Triple("FOOD COURT", 277f, 207f)
                        )
                        labels.forEach { (text, x, y) ->
                            val layoutResult = textMeasurer.measure(text, labelStyle)
                            drawText(
                                layoutResult,
                                topLeft = Offset(
                                    x - layoutResult.size.width / 2f,
                                    y - layoutResult.firstBaseline
                                )
                            )
                        }
                    }
                }
            }

            visible.forEach { r ->
                val isSelected = selected?.id == r.id
                val px = boxWidthPx * r.mapX.toFloat() / 100f
                val py = boxHeightPx * r.mapY.toFloat() / 100f
                Column(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                            layout(placeable.width, placeable.height) {
                                placeable.place(
                                    (px - placeable.width / 2f).roundToInt(),
                                    (py - placeable.height).roundToInt()
                                )
                            }
                        }
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { selected = if (isSelected) null else r },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isSelected) {
                        val tipShape = RoundedCornerShape(12.dp)
                        Column(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .shadow(6.dp, tipShape)
                                .clip(tipShape)
                                .background(Color.White)
                                .border(1.dp, CardBorder, tipShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = r.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = r.waitTime,
                                fontSize = 9.sp,
                                color = Muted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val sc = if (isSelected) 1.25f else 1f
                                scaleX = sc
                                scaleY = sc
                            }
                            .size(32.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> Accent
                                    r.isOpen -> Ink
                                    else -> Muted
                                }
                            )
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = categoryEmoji(r.category),
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Ink)
                    )
                    Text(text = "Open", fontSize = 9.sp, color = Muted)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Muted)
                    )
                    Text(text = "Closed", fontSize = 9.sp, color = Muted)
                }
            }
        }

        selected?.let { sel ->
            val cardShape = RoundedCornerShape(16.dp)
            Box(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, cardShape)
                        .clip(cardShape)
                        .background(Color.White)
                        .border(1.dp, CardBorder, cardShape)
                        .clickable { onSelect(sel) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = sel.image,
                        contentDescription = sel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = sel.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                fontFamily = OutfitFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = if (sel.isOpen) "Open" else "Closed",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel.isOpen) Green else Muted,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (sel.isOpen) GreenBg else ClosedBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = sel.location,
                            fontSize = 11.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PathIcon(STAR_PATH, 10.dp, fill = StarColor)
                                Text(
                                    text = "${sel.rating}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                            }
                            Text(text = "• ${sel.waitTime}", fontSize = 11.sp, color = Muted)
                            CrowdingBadge(reports = crowding[sel.id] ?: emptyList(), size = "sm")
                        }
                    }
                    Box(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        PathIcon(CHEVRON_PATH, 16.dp, stroke = Disabled, strokeWidth = 2f, round = true)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = "Nearby — ${visible.size} spots",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontFamily = OutfitFontFamily,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visible.forEach { r ->
                    val itemShape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(itemShape)
                            .background(Color.White)
                            .border(
                                1.dp,
                                if (selected?.id == r.id) Accent else CardBorder,
                                itemShape
                            )
                            .clickable { selected = r }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = categoryEmoji(r.category), fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = r.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                                fontFamily = OutfitFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${r.waitTime} wait",
                                fontSize = 11.sp,
                                color = Muted
                            )
                        }
                        Text(
                            text = if (r.isOpen) "Open" else "Closed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (r.isOpen) Green else Muted,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (r.isOpen) GreenBg else ClosedBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun categoryEmoji(cat: String): String {
    val map = mapOf(
        "Dining Hall" to "🍽",
        "Café" to "☕",
        "Asian" to "🍜",
        "Burgers" to "🍔",
        "Indian" to "🍛",
        "Smoothies" to "🥤"
    )
    return map[cat] ?: "🍴"
}

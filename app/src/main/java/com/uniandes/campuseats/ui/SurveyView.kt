package com.uniandes.campuseats.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniandes.campuseats.data.Profile
import com.uniandes.campuseats.ui.theme.OutfitFontFamily
import kotlinx.coroutines.delay

private val Cream = Color(0xFFFBF5EE)
private val Ink = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Outline = Color(0xFFE8E0D4)
private val LightMuted = Color(0xFFB8AC9E)
private val Green = Color(0xFF3A8C4F)
private val Amber = Color(0xFFF4A735)

private const val TOTAL_STEPS = 5
private const val DONE_STEP = TOTAL_STEPS

private val Cuisines = listOf("Colombian", "Mediterranean", "Asian", "American", "Mexican", "Italian", "Indian", "Middle Eastern", "Vegetarian", "Vegan")
private val Dietary = listOf("None", "Vegetarian", "Vegan", "Gluten-free", "Lactose-free", "Halal", "Kosher", "Nut-free")
private val MealTimes = listOf("Breakfast", "Mid-morning snack", "Lunch", "Afternoon snack", "Dinner", "Late night")
private val Frequencies = listOf("Daily", "4–5x / week", "2–3x / week", "Once a week")
private val Priorities = listOf("Price", "Speed", "Nutrition", "Taste", "Variety", "Proximity", "Sustainability")

private data class BudgetOption(val label: String, val sub: String, val emoji: String)

private val Budgets = listOf(
    BudgetOption("Under $8.000", "Looking for the best deals", "💸"),
    BudgetOption("$8.000 – $15.000", "Mid-range, good value", "💵"),
    BudgetOption("$15.000 – $25.000", "Happy to spend a bit more", "🪙"),
    BudgetOption("Over $25.000", "Quality is top priority", "💎")
)

private enum class ChipColor { Brand, Green, Amber }

/**
 * Encuesta de preferencias de comida en 5 pasos. Se usa como onboarding al abrir la app
 * por primera vez ([isOnboarding] = true) y desde el perfil para rehacerla.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SurveyView(
    onComplete: (Profile) -> Unit,
    isOnboarding: Boolean = false
) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var cuisines by remember { mutableStateOf(listOf<String>()) }
    var dietary by remember { mutableStateOf(listOf<String>()) }
    var mealTimes by remember { mutableStateOf(listOf<String>()) }
    var frequency by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var priorities by remember { mutableStateOf(listOf<String>()) }
    var dislikedFoods by remember { mutableStateOf("") }

    if (step == DONE_STEP) {
        // Igual que el prototipo: muestra "¡Listo!" 1.2 s antes de continuar
        LaunchedEffect(Unit) {
            delay(1200)
            onComplete(
                Profile(
                    name = name.trim(),
                    frequency = frequency,
                    budget = budget,
                    dietaryRestrictions = dietary,
                    cuisinePreferences = cuisines,
                    usualMealTimes = mealTimes,
                    topPriorities = priorities,
                    foodsToAvoid = dislikedFoods.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                )
            )
        }
        DoneScreen(name = name.trim())
        return
    }

    val progress by animateFloatAsState(
        targetValue = (step + 1f) / TOTAL_STEPS,
        animationSpec = tween(500),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Header
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)) {
            if (isOnboarding && step == 0) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "UNIVERSIDAD DE LOS ANDES",
                        color = Orange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.7.sp
                    )
                    Text(
                        text = "Welcome to\nCampus Eats 👋",
                        color = Ink,
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = OutfitFontFamily
                    )
                    Text(
                        text = "Tell us a bit about yourself so we can personalize your experience.",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = "FOOD PREFERENCES",
                        color = Orange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.7.sp
                    )
                    Text(
                        text = "Survey",
                        color = Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = OutfitFontFamily
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Outline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Orange)
                    )
                }
                Text(
                    text = "${step + 1} / $TOTAL_STEPS",
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(TOTAL_STEPS) { i ->
                    Box(
                        modifier = Modifier
                            .then(if (i == step) Modifier.weight(1f) else Modifier.width(16.dp))
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (i <= step) Orange else Outline)
                    )
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            when (step) {
                0 -> StepCard("👤", "What should we call you?", "Just your first name is fine") {
                    SurveyTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Your name...",
                        singleLine = true
                    )
                    SubQuestion("What cuisines do you love?")
                    MultiSelect(Cuisines, cuisines, ChipColor.Brand) { cuisines = cuisines.toggle(it) }
                }

                1 -> StepCard("🌿", "Any dietary restrictions?", "We'll filter out what doesn't work for you") {
                    MultiSelect(Dietary, dietary, ChipColor.Green) { dietary = dietary.toggle(it) }
                }

                2 -> StepCard("🕐", "When do you eat on campus?", "Select your typical meal times") {
                    MultiSelect(MealTimes, mealTimes, ChipColor.Amber) { mealTimes = mealTimes.toggle(it) }
                    SubQuestion("How often?")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Frequencies.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { opt ->
                                    val isSelected = frequency == opt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Ink else Color.White)
                                            .border(1.dp, if (isSelected) Ink else Outline, RoundedCornerShape(12.dp))
                                            .clickable { frequency = opt }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt,
                                            color = if (isSelected) Color.White else Ink,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> StepCard("💰", "What's your typical meal budget?", "Per meal, in Colombian pesos") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Budgets.forEach { opt ->
                            val isSelected = budget == opt.label
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Orange else Color.White)
                                    .border(1.dp, if (isSelected) Orange else Outline, RoundedCornerShape(16.dp))
                                    .clickable { budget = opt.label }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(text = opt.emoji, fontSize = 22.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = opt.label,
                                        color = if (isSelected) Color.White else Ink,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = OutfitFontFamily
                                    )
                                    Text(
                                        text = opt.sub,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Muted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> StepCard("⭐", "What matters most?", "Pick up to 3 priorities when choosing where to eat") {
                    MultiSelect(
                        options = Priorities,
                        selected = priorities,
                        color = ChipColor.Brand,
                        maxLabel = if (priorities.size >= 3) "Max 3 selected" else null
                    ) {
                        if (it in priorities || priorities.size < 3) priorities = priorities.toggle(it)
                    }
                    SubQuestion("Foods you want to avoid?", bottom = 8.dp)
                    SurveyTextField(
                        value = dislikedFoods,
                        onValueChange = { dislikedFoods = it },
                        placeholder = "e.g. spicy food, seafood, mushrooms...",
                        singleLine = false
                    )
                }
            }
        }

        // Nav
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (step > 0) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Outline, RoundedCornerShape(16.dp))
                        .clickable { step -= 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = Ink)
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Orange)
                    .clickable { step += 1 },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (step < TOTAL_STEPS - 1) "Continue" else "Save Preferences",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitFontFamily
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DoneScreen(name: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(Orange),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (name.isNotEmpty()) "¡Listo, $name!" else "¡Listo!",
            color = Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            fontFamily = OutfitFontFamily,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your profile is set up. Discovering the best campus spots for you…",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun StepCard(icon: String, title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Outline, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 22.sp)
            }
            Column {
                Text(
                    text = title,
                    color = Ink,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = OutfitFontFamily
                )
                Text(
                    text = subtitle,
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        content()
    }
}

@Composable
private fun SubQuestion(text: String, bottom: Dp = 12.dp) {
    Text(
        text = text,
        color = Ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = OutfitFontFamily,
        modifier = Modifier.padding(top = 20.dp, bottom = bottom)
    )
}

@Composable
private fun SurveyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        textStyle = TextStyle(color = Ink, fontSize = if (singleLine) 14.sp else 13.sp),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, if (focused) Orange else Outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = LightMuted, fontSize = if (singleLine) 14.sp else 13.sp)
                }
                inner()
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiSelect(
    options: List<String>,
    selected: List<String>,
    color: ChipColor,
    maxLabel: String? = null,
    onToggle: (String) -> Unit
) {
    val (activeBg, activeText) = when (color) {
        ChipColor.Brand -> Orange to Color.White
        ChipColor.Green -> Green to Color.White
        ChipColor.Amber -> Amber to Ink
    }
    Column {
        if (maxLabel != null) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Orange))
                Text(text = maxLabel, color = Orange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { opt ->
                val isSelected = opt in selected
                Text(
                    text = if (isSelected) "✓  $opt" else opt,
                    color = if (isSelected) activeText else Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) activeBg else Color.White)
                        .border(1.dp, if (isSelected) activeBg else Outline, RoundedCornerShape(12.dp))
                        .clickable { onToggle(opt) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun List<String>.toggle(value: String): List<String> =
    if (value in this) this - value else this + value

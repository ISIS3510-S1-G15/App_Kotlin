package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.restaurants

private val Cream = Color(0xFFFBF5EE)
private val Brown = Color(0xFF1A1208)
private val Orange = Color(0xFFE8440A)
private val Muted = Color(0xFF7A6D5F)
private val Border = Color(0xFFE8E0D4)
private val SoftBorder = Color(0xFFF0E8DE)
private val Gold = Color(0xFFF4A735)
private val Green = Color(0xFF3A8C4F)
private val LightMuted = Color(0xFFB8AC9E)
private val Highlight = Color(0x66F4A735)

private val Recent = listOf("Starbucks", "Kai Sushi", "Cosechas")
private val PopularTags = listOf(
    "Vegan options", "Halal", "Open late", "Quick pickup", "Coffee"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchView(
    onSelect: (Restaurant) -> Unit,
    crowding: Map<String, List<Int>>,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val results = remember(query) {
        if (query.trim().isNotEmpty()) {
            val q = query.lowercase()
            restaurants.filter { restaurant ->
                restaurant.name.lowercase().contains(q) ||
                        restaurant.category.lowercase().contains(q) ||
                        restaurant.location.lowercase().contains(q) ||
                        restaurant.tags.any { it.lowercase().contains(q) }
            }
        } else {
            emptyList()
        }
    }

    val showEmpty = query.trim().isNotEmpty() && results.isEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Search",
                color = Brown,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (focused) Orange else Border,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (focused) Orange else Muted,
                        modifier = Modifier.size(18.dp)
                    )

                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focused = it.isFocused },
                        placeholder = {
                            Text(
                                text = "Restaurant name, cuisine, location...",
                                color = LightMuted,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Orange
                        )
                    )

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                query = ""
                                focusManager.clearFocus(force = false)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Muted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            if (showEmpty) {
                EmptySearchState(query)
            }

            if (results.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${results.size} result${if (results.size != 1) "s" else ""}",
                        color = Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    results.forEach { restaurant ->
                        SearchResultRow(
                            restaurant = restaurant,
                            onSelect = onSelect,
                            query = query,
                            crowdingReports = crowding[restaurant.id].orEmpty()
                        )
                    }
                }
            }

            if (query.isEmpty()) {
                RecentSearches(
                    onSelect = onSelect
                )

                PopularTags(
                    onTagClick = { query = it }
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "All Restaurants",
                        color = Brown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        restaurants.forEach { restaurant ->
                            SearchResultRow(
                                restaurant = restaurant,
                                onSelect = onSelect,
                                query = "",
                                crowdingReports = crowding[restaurant.id].orEmpty()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🍽",
            fontSize = 48.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No results for \"$query\"",
            color = Brown,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Try a different name or cuisine",
            color = Muted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun RecentSearches(
    onSelect: (Restaurant) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Searches",
                color = Brown,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Clear",
                color = Orange,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Recent.forEach { name ->
                val restaurant = restaurants.find { it.name == name }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = restaurant != null) {
                            restaurant?.let(onSelect)
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = Color(0xFFF0E8DE)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Muted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        color = Brown,
                        fontSize = 14.sp
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open",
                        tint = LightMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PopularTags(
    onTagClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Browse by Tag",
            color = Brown,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PopularTags.forEach { tag ->
                Surface(
                    modifier = Modifier.clickable { onTagClick(tag) },
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#",
                            color = Orange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tag,
                            color = Brown,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    restaurant: Restaurant,
    onSelect: (Restaurant) -> Unit,
    query: String,
    crowdingReports: List<Int>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, SoftBorder, RoundedCornerShape(16.dp))
            .clickable { onSelect(restaurant) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = restaurant.image,
            contentDescription = restaurant.name,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            HighlightedText(
                text = restaurant.name,
                query = query,
                color = Brown,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            HighlightedText(
                text = restaurant.location,
                query = query,
                color = Muted,
                fontSize = 11.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = restaurant.rating.toString(),
                        color = Brown,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text("·", color = LightMuted, fontSize = 11.sp)

                Text(
                    text = restaurant.category,
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text("·", color = LightMuted, fontSize = 11.sp)

                Text(
                    text = if (restaurant.isOpen) "Open" else "Closed",
                    color = if (restaurant.isOpen) Green else Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                CrowdingBadge(
                    reports = crowdingReports,
                    size = "sm"
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Open restaurant",
            tint = LightMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int
) {
    val annotated = highlight(text, query)

    Text(
        text = annotated,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

private fun highlight(
    text: String,
    query: String
): AnnotatedString {
    if (query.isBlank()) {
        return AnnotatedString(text)
    }

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    val index = lowerText.indexOf(lowerQuery)

    if (index == -1) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        append(text.substring(0, index))

        pushStyle(
            SpanStyle(
                background = Highlight,
                color = Brown
            )
        )
        append(text.substring(index, index + query.length))
        pop()

        append(text.substring(index + query.length))
    }
}


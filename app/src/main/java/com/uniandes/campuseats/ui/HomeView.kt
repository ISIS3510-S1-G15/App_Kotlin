package com.uniandes.campuseats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.uniandes.campuseats.data.Profile
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
private val SoftGreen = Color(0xFFE8F5EB)
private val SoftGray = Color(0xFFF0EEEC)
private val LightMuted = Color(0xFFB8AC9E)

private val Categories = listOf(
    "All", "Dining Hall", "Café", "Asian", "Burgers", "Indian", "Smoothies"
)

@Composable
fun HomeView(
    onSelect: (Restaurant) -> Unit,
    onOpenProfile: () -> Unit,
    profile: Profile,
    crowding: Map<String, List<Int>>,
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf("All") }

    val filtered = remember(search, activeCategory) {
        restaurants.filter { restaurant ->
            val matchCategory =
                activeCategory == "All" || restaurant.category == activeCategory

            val normalizedSearch = search.lowercase()
            val matchSearch =
                restaurant.name.lowercase().contains(normalizedSearch) ||
                        restaurant.location.lowercase().contains(normalizedSearch)

            matchCategory && matchSearch
        }
    }

    val featured = restaurants.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "UNIVERSIDAD DE LOS ANDES",
                        color = Orange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.7.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = profile.name
                            .takeIf { it.isNotBlank() }
                            ?.let { "Hola, $it 👋" }
                            ?: "Campus Eats",
                        color = Brown,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 30.sp
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onOpenProfile),
                    shape = CircleShape,
                    color = Brown
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val name = profile.name.takeIf { it.isNotBlank() }

                        if (name != null) {
                            Text(
                                text = name
                                    .take(2)
                                    .uppercase(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (search.isBlank() && activeCategory == "All" && featured != null) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Pick",
                        color = Brown,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "See all",
                        color = Orange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FeaturedRestaurant(
                    restaurant = featured,
                    onClick = { onSelect(featured) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
            ,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Categories.forEach { category ->
                CategoryChip(
                    text = category,
                    selected = activeCategory == category,
                    onClick = { activeCategory = category }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filtered.size} ${
                        if (activeCategory == "All") "Spots" else activeCategory
                    } Near You",
                    color = Brown,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = Orange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Filter",
                        color = Orange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            filtered.forEach { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    onSelect = onSelect,
                    crowdingReports = crowding[restaurant.id].orEmpty()
                )
            }
        }
    }
}

@Composable
private fun FeaturedRestaurant(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = restaurant.image,
            contentDescription = restaurant.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Brown.copy(alpha = 0.30f),
                            Brown
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    text = "Featured",
                    background = Orange,
                    textColor = Color.White
                )

                StatusPill(
                    text = if (restaurant.isOpen) "Open" else "Closed",
                    background = if (restaurant.isOpen) Green else Muted,
                    textColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = restaurant.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = restaurant.rating.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = restaurant.location,
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "• ${restaurant.waitTime}",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) Brown else Color.White,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) Color.White else Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    background: Color,
    textColor: Color
) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = textColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp
    )
}

@Composable
fun CrowdingBadge(reports: List<Int>, size: String, modifier: Modifier = Modifier) {
    Text(
        text = "👥 ${reports.size}",
        fontSize = if (size == "sm") 10.sp else 12.sp,
        color = Color.Gray,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun RestaurantCard(
    restaurant: Restaurant,
    onSelect: (Restaurant) -> Unit,
    crowdingReports: List<Int>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, SoftBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect(restaurant) }
    ) {
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(135.dp)
        ) {
            AsyncImage(
                model = restaurant.image,
                contentDescription = restaurant.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (restaurant.saved) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.90f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Saved",
                            tint = Orange,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = restaurant.name,
                    modifier = Modifier.weight(1f),
                    color = Brown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (restaurant.isOpen) "OPEN" else "CLOSED",
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (restaurant.isOpen) SoftGreen else SoftGray)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = if (restaurant.isOpen) Green else Muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = restaurant.location,
                color = Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = restaurant.rating.toString(),
                        color = Brown,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "(${restaurant.reviews})",
                    color = LightMuted,
                    fontSize = 11.sp
                )

                Text("·", color = LightMuted, fontSize = 11.sp)

                Text(
                    text = restaurant.price,
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Text("·", color = LightMuted, fontSize = 11.sp)

                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(10.dp)
                )

                Text(
                    text = restaurant.waitTime,
                    color = Muted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                restaurant.tags.take(2).forEach { tag ->
                    Text(
                        text = tag,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Cream)
                            .border(1.dp, Border, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Muted,
                        fontSize = 10.sp
                    )
                }

                CrowdingBadge(
                    reports = crowdingReports,
                    size = "sm"
                )
            }
        }
    }
}


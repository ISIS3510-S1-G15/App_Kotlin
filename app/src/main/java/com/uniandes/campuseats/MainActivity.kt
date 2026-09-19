package com.uniandes.campuseats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.uniandes.campuseats.data.mockProfile
import com.uniandes.campuseats.ui.HomeView
import com.uniandes.campuseats.ui.SearchView
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Person
import com.uniandes.campuseats.ui.MapView
import com.uniandes.campuseats.ui.ProfileView
import com.uniandes.campuseats.ui.SavedView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CampusEatsMainScreen()
            }
        }
    }
}

@Composable
fun CampusEatsMainScreen() {
    var currentTab by remember { mutableStateOf(0) }

    val dummyCrowding = mapOf(
        "1" to listOf(1, 2, 3),
        "2" to listOf(1),
        "3" to emptyList(),
        "4" to listOf(1, 2)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val unselectedColor = Color.Gray
                val selectedColor = Color(0xFFE8440A) // Naranja de Campus Eats

                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, "Discover", tint = if (currentTab == 0) selectedColor else unselectedColor) },
                    label = { Text("Discover", color = if (currentTab == 0) selectedColor else unselectedColor) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Search, "Search", tint = if (currentTab == 1) selectedColor else unselectedColor) },
                    label = { Text("Search", color = if (currentTab == 1) selectedColor else unselectedColor) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Map, "Map", tint = if (currentTab == 2) selectedColor else unselectedColor) },
                    label = { Text("Map", color = if (currentTab == 2) selectedColor else unselectedColor) }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Bookmark, "Saved", tint = if (currentTab == 3) selectedColor else unselectedColor) },
                    label = { Text("Saved", color = if (currentTab == 3) selectedColor else unselectedColor) }
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.Person, "Profile", tint = if (currentTab == 4) selectedColor else unselectedColor) },
                    label = { Text("Profile", color = if (currentTab == 4) selectedColor else unselectedColor) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                0 -> HomeView(
                    onSelect = { },
                    onOpenProfile = { },
                    profile = mockProfile,
                    crowding = dummyCrowding
                )
                1 -> SearchView(
                    onSelect = { },
                    crowding = dummyCrowding
                )
                2 -> MapView( // Vista de mapa conectada
                    onSelect = { },
                    crowding = dummyCrowding
                )
                3 -> SavedView( // Vista de guardados conectada
                    onSelect = { },
                    crowding = dummyCrowding
                )
                4 -> ProfileView(
                    profile = mockProfile,
                    onRetakeSurvey = { },
                    onBack = { currentTab = 0 }
                )
            }
        }
    }
}
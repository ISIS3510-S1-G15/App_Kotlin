package com.uniandes.campuseats.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.uniandes.campuseats.data.Restaurant

val LocalRestaurants = staticCompositionLocalOf<List<Restaurant>> { emptyList() }

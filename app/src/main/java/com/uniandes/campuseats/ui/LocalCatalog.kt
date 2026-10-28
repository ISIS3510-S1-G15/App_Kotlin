package com.uniandes.campuseats.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.uniandes.campuseats.data.Catalog
import com.uniandes.campuseats.data.MealRules

val LocalCatalog = staticCompositionLocalOf { Catalog.Empty }

val LocalMealRules = staticCompositionLocalOf { MealRules(Catalog.Empty) }

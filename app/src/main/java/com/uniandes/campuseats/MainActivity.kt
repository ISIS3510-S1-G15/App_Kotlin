package com.uniandes.campuseats

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.uniandes.campuseats.analytics.AnalyticsScreen
import com.uniandes.campuseats.analytics.AnalyticsViewModel
import com.uniandes.campuseats.auth.AuthUser
import com.uniandes.campuseats.auth.AuthViewModel
import com.uniandes.campuseats.data.AppDataStore
import com.uniandes.campuseats.data.ProfileStore
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.net.attemptOrNull
import com.uniandes.campuseats.sensor.distanceMeters
import com.uniandes.campuseats.sensor.rememberUserLocation
import com.uniandes.campuseats.ui.CrowdingModal
import com.uniandes.campuseats.ui.DetailView
import com.uniandes.campuseats.ui.HomeView
import com.uniandes.campuseats.ui.LoadingView
import com.uniandes.campuseats.ui.LoginView
import com.uniandes.campuseats.ui.MapView
import com.uniandes.campuseats.ui.ProfileView
import com.uniandes.campuseats.ui.ReviewsListView
import com.uniandes.campuseats.ui.SavedView
import com.uniandes.campuseats.ui.SearchView
import com.uniandes.campuseats.ui.SurveyView
import com.uniandes.campuseats.ui.WriteReviewView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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

/** Pantallas que se apilan encima de las tabs (igual que los overlays del prototipo). */
private enum class Overlay { Detail, WriteReview, ReviewsList, Survey }

private const val PROFILE_TAB = 4

@Composable
fun CampusEatsMainScreen() {
    val context = LocalContext.current
    val services = remember { AppServices.get(context) }
    val auth: AuthViewModel = viewModel(factory = AuthViewModel.factory(services.authRepository))
    val user = auth.user

    when {
        auth.restoring -> LoadingView()
        user == null -> LoginView(
            loading = auth.loading,
            error = auth.error,
            onLogin = auth::login,
            onRegister = auth::register,
            onInputChanged = auth::clearError
        )
        else -> key(user.id) {
            CampusEatsApp(
                services = services,
                user = user,
                onLogout = {
                    ProfileStore(context.applicationContext).clear()
                    AppDataStore(context.applicationContext).clearSavedIds()
                    auth.logout()
                }
            )
        }
    }
}

@Composable
private fun CampusEatsApp(services: AppServices, user: AuthUser, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileStore = remember { ProfileStore(context.applicationContext) }
    val dataStore = remember { AppDataStore(context.applicationContext) }
    val analytics: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.factory(services))

    var profileResolved by remember { mutableStateOf(profileStore.isSurveyDone) }
    var surveyDone by remember { mutableStateOf(profileStore.isSurveyDone) }
    var profile by remember { mutableStateOf(profileStore.load()) }
    var currentTab by remember { mutableIntStateOf(0) }

    val overlayStack = remember { mutableStateListOf<Overlay>() }
    val overlay = overlayStack.lastOrNull()
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var pendingRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    // Datos del usuario persistidos (si no hay nada guardado, AppDataStore devuelve la semilla)
    val reviews = remember { mutableStateListOf<Review>().apply { addAll(dataStore.loadReviews()) } }
    val crowding = remember { mutableStateMapOf<String, List<Int>>().apply { putAll(dataStore.loadCrowding()) } }
    var savedIds by remember { mutableStateOf(dataStore.loadSavedIds()) }

    LaunchedEffect(user.id) {
        attemptOrNull { services.profileRepository.fetch() }?.let { remote ->
            val restored = remote.copy(name = remote.name.ifBlank { user.name })
            profileStore.save(restored)
            profile = restored
            surveyDone = true
        }
        profileResolved = true
        attemptOrNull { services.savedRepository.fetch() }?.let {
            savedIds = it
            dataStore.saveSavedIds(it)
        }
    }

    // Sensor GPS: se pide el permiso al terminar el onboarding y se comparte con las pantallas
    val userLocation = rememberUserLocation()
    LaunchedEffect(surveyDone) {
        if (surveyDone && !userLocation.hasPermission) userLocation.requestPermission()
    }

    if (!profileResolved) {
        LoadingView()
        return
    }

    // Onboarding: encuesta a pantalla completa solo la primera vez
    if (!surveyDone) {
        Scaffold { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                SurveyView(
                    isOnboarding = true,
                    initialName = user.name,
                    onComplete = { answers ->
                        val completed = answers.copy(name = answers.name.ifBlank { user.name })
                        profileStore.save(completed)
                        profile = completed
                        surveyDone = true
                        scope.launch { attemptOrNull { services.profileRepository.save(completed) } }
                    }
                )
            }
        }
        return
    }

    // Tocar un restaurante → primero el modal de ocupación, luego el detalle
    val requestDetail: (Restaurant) -> Unit = {
        pendingRestaurant = it
    }

    fun commitDetail(r: Restaurant) {
        analytics.onOccupancyChecked(r.id, crowding[r.id].orEmpty().isNotEmpty())
        selectedRestaurant = r
        pendingRestaurant = null
        overlayStack.clear()
        overlayStack.add(Overlay.Detail)
    }

    fun toggleSaved(id: String) {
        val before = savedIds
        val wasSaved = id in before
        savedIds = if (wasSaved) before - id else before + id
        scope.launch {
            try {
                val confirmed = if (wasSaved) {
                    services.savedRepository.remove(id)
                } else {
                    services.savedRepository.add(id)
                }
                savedIds = confirmed
                dataStore.saveSavedIds(confirmed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                savedIds = before
                Toast.makeText(context, "Could not update your saved spots", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun popOverlay() {
        overlayStack.removeLastOrNull()
    }

    fun closeAll() {
        overlayStack.clear()
    }

    fun selectTab(tab: Int) {
        overlayStack.clear()
        pendingRestaurant = null
        currentTab = tab
    }

    BackHandler(enabled = pendingRestaurant != null || overlayStack.isNotEmpty()) {
        when {
            pendingRestaurant != null -> pendingRestaurant = null
            overlay == Overlay.Detail -> closeAll()
            else -> popOverlay()
        }
    }

    val restaurantReviews = selectedRestaurant
        ?.let { r -> reviews.filter { it.restaurantId == r.id } }
        .orEmpty()

    Scaffold(
        bottomBar = {
            if (overlay != Overlay.Survey) {
                NavigationBar(containerColor = Color.White) {
                    val unselectedColor = Color.Gray
                    val selectedColor = Color(0xFFE8440A) // Naranja de Campus Eats
                    val tabs = listOf(
                        Icons.Default.Home to "Discover",
                        Icons.Default.Search to "Search",
                        Icons.Default.Map to "Map",
                        Icons.Default.Bookmark to "Saved",
                        Icons.Default.Person to "Profile"
                    )
                    tabs.forEachIndexed { index, (icon, label) ->
                        val active = currentTab == index && overlay == null
                        NavigationBarItem(
                            selected = active,
                            onClick = { selectTab(index) },
                            icon = { Icon(icon, label, tint = if (active) selectedColor else unselectedColor) },
                            label = { Text(label, color = if (active) selectedColor else unselectedColor) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            val restaurant = selectedRestaurant
            when {
                overlay == Overlay.Detail && restaurant != null -> DetailView(
                    restaurant = restaurant,
                    reviews = restaurantReviews,
                    crowdingReports = crowding[restaurant.id].orEmpty(),
                    isSaved = restaurant.id in savedIds,
                    distanceMeters = userLocation.location?.let { distanceMeters(it, restaurant) },
                    userLocation = userLocation.location,
                    onToggleSave = { toggleSaved(restaurant.id) },
                    onBack = ::closeAll,
                    onWriteReview = { overlayStack.add(Overlay.WriteReview) },
                    onSeeReviews = { overlayStack.add(Overlay.ReviewsList) },
                    onGetDirections = { analytics.onGoDecision(restaurant.id) }
                )

                overlay == Overlay.WriteReview && restaurant != null -> WriteReviewView(
                    restaurant = restaurant,
                    authorName = profile.name,
                    onSubmit = {
                        reviews.add(0, it)
                        dataStore.saveReviews(reviews)
                    },
                    onBack = ::popOverlay
                )

                overlay == Overlay.ReviewsList && restaurant != null -> ReviewsListView(
                    restaurant = restaurant,
                    reviews = restaurantReviews,
                    onBack = ::popOverlay,
                    onWriteReview = {
                        popOverlay()
                        overlayStack.add(Overlay.WriteReview)
                    }
                )

                overlay == Overlay.Survey -> SurveyView(
                    initialName = profile.name,
                    onComplete = { answers ->
                        val updated = answers.copy(name = answers.name.ifBlank { user.name })
                        analytics.onProfileSaved(profile, updated)
                        profileStore.save(updated)
                        profile = updated
                        scope.launch { attemptOrNull { services.profileRepository.save(updated) } }
                        popOverlay()
                    }
                )

                else -> when (currentTab) {
                    0 -> HomeView(
                        onSelect = requestDetail,
                        onOpenProfile = { selectTab(PROFILE_TAB) },
                        profile = profile,
                        crowding = crowding,
                        savedIds = savedIds,
                        reviews = reviews,
                        userLocation = userLocation.location,
                        onFilterUsed = { analytics.onFilterUsed(it, AnalyticsScreen.Home) }
                    )
                    1 -> SearchView(
                        onSelect = requestDetail,
                        crowding = crowding,
                        userLocation = userLocation.location,
                        onFilterUsed = { analytics.onFilterUsed(it, AnalyticsScreen.Search) }
                    )
                    2 -> MapView(
                        onSelect = requestDetail,
                        crowding = crowding,
                        userLocation = userLocation.location,
                        hasLocationPermission = userLocation.hasPermission,
                        onRetryLocation = userLocation.requestPermission,
                        onFilterUsed = { analytics.onFilterUsed(it, AnalyticsScreen.Map) }
                    )
                    3 -> SavedView(
                        onSelect = requestDetail,
                        crowding = crowding,
                        savedIds = savedIds,
                        userLocation = userLocation.location
                    )
                    PROFILE_TAB -> ProfileView(
                        profile = profile,
                        userEmail = user.email,
                        onRetakeSurvey = { overlayStack.add(Overlay.Survey) },
                        onLogout = onLogout,
                        onBack = { selectTab(0) }
                    )
                }
            }

            // Modal de ocupación: flota sobre la tab antes de abrir el detalle
            pendingRestaurant?.let { pending ->
                if (overlay == null) {
                    CrowdingModal(
                        restaurant = pending,
                        onSubmit = { level ->
                            crowding[pending.id] = crowding[pending.id].orEmpty() + level
                            dataStore.saveCrowding(crowding)
                            commitDetail(pending)
                        },
                        onSkip = { commitDetail(pending) }
                    )
                }
            }
        }
    }
}

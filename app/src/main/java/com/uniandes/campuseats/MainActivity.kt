package com.uniandes.campuseats

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.uniandes.campuseats.analytics.AnalyticsScreen
import com.uniandes.campuseats.analytics.AnalyticsViewModel
import com.uniandes.campuseats.analytics.AppSection
import com.uniandes.campuseats.analytics.SectionAction
import com.uniandes.campuseats.analytics.SectionAnalyticsViewModel
import com.uniandes.campuseats.data.MealRules
import com.uniandes.campuseats.data.remote.applyRatingSummaries
import com.uniandes.campuseats.auth.AuthUser
import com.uniandes.campuseats.auth.AuthViewModel
import com.uniandes.campuseats.data.Catalog
import com.uniandes.campuseats.data.Restaurant
import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.data.emptyProfile
import com.uniandes.campuseats.data.outbox.OutboxKind
import com.uniandes.campuseats.data.outbox.OutboxRequest
import com.uniandes.campuseats.net.attemptOrNull
import com.uniandes.campuseats.sensor.distanceMeters
import com.uniandes.campuseats.sensor.findLifecycleOwner
import com.uniandes.campuseats.sensor.rememberUserLocation
import com.uniandes.campuseats.ui.ConnectionErrorView
import com.uniandes.campuseats.ui.CrowdingModal
import com.uniandes.campuseats.ui.DetailView
import com.uniandes.campuseats.ui.HomeView
import com.uniandes.campuseats.ui.LoadingView
import com.uniandes.campuseats.ui.LocalCatalog
import com.uniandes.campuseats.ui.LocalMealRules
import com.uniandes.campuseats.ui.LocalRestaurants
import com.uniandes.campuseats.ui.LoginView
import com.uniandes.campuseats.ui.MapView
import com.uniandes.campuseats.ui.OfflineBanner
import com.uniandes.campuseats.ui.PendingSyncBanner
import com.uniandes.campuseats.ui.ProfileView
import com.uniandes.campuseats.ui.ReviewsListView
import com.uniandes.campuseats.ui.SavedView
import com.uniandes.campuseats.ui.SearchView
import com.uniandes.campuseats.ui.SurveyView
import com.uniandes.campuseats.ui.WriteReviewView
import com.uniandes.campuseats.ui.PartnerReportView
import com.uniandes.campuseats.net.ConnectivityObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
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
private enum class Overlay { Detail, WriteReview, ReviewsList, Survey, PartnerReport }
private const val MAX_RECENT_SEARCHES = 5
private const val MAP_TAB = 2
private const val FAVORITES_TAB = 3
private const val PROFILE_TAB = 4

@Composable
fun CampusEatsMainScreen() {
    val context = LocalContext.current
    val services = remember { AppServices.get(context) }
    val scope = rememberCoroutineScope()
    val auth: AuthViewModel = viewModel(factory = AuthViewModel.factory(services.authRepository, services.localStore, services.connectivity,  ))
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
                auth = auth
            )
        }
    }
}

@Composable
private fun CampusEatsApp(services: AppServices, user: AuthUser, auth: AuthViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localStore = services.localStore
    val analytics: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.factory(services))
    val sections: SectionAnalyticsViewModel = viewModel(factory = SectionAnalyticsViewModel.factory(services))

    var profileResolved by remember { mutableStateOf(false) }
    var surveyDone by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(emptyProfile) }
    var currentTab by remember { mutableIntStateOf(0) }

    val overlayStack = remember { mutableStateListOf<Overlay>() }
    val overlay = overlayStack.lastOrNull()
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var pendingRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    val reviews = remember { mutableStateListOf<Review>() }
    val crowding = remember { mutableStateMapOf<String, List<Int>>() }
    var savedIds by remember { mutableStateOf(emptySet<String>()) }
    var restaurants by remember { mutableStateOf(emptyList<Restaurant>()) }
    var restaurantsResolved by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf(emptyList<String>()) }
    var catalog by remember { mutableStateOf(Catalog.Empty) }
    var catalogResolved by remember { mutableStateOf(false) }

    var startupDone by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var outboxPending by remember { mutableIntStateOf(0) }
    val online by services.connectivity.online.collectAsState()

    suspend fun refreshRestaurants(force: Boolean = false) {
        val fetched = attemptOrNull { services.restaurantRepository.revalidate(force) }?.takeIf { it.isNotEmpty() }
        val freshSummaries = attemptOrNull { services.reviewRepository.revalidateSummaries(force) }
        val base = fetched ?: restaurants.takeIf { freshSummaries != null && it.isNotEmpty() } ?: return
        val summaries = freshSummaries ?: services.reviewRepository.cachedSummaries()
        restaurants = applyRatingSummaries(base, summaries)
    }

    suspend fun refreshCatalog(force: Boolean = false) {
        attemptOrNull { services.catalogRepository.revalidate(force) }?.takeIf { !it.isEmpty }?.let { catalog = it }
    }

    suspend fun refreshReviews(force: Boolean = false) {
        val current = reviews.toList()
        val ids = restaurants.map { it.id }
        val fetched = services.reviewRepository.revalidateAll(ids, force)
        if (fetched.isEmpty()) return
        val merged = ids.flatMap { id -> fetched[id] ?: current.filter { it.restaurantId == id } }
        reviews.clear()
        reviews.addAll(merged)
        localStore.saveReviews(merged)
    }

    suspend fun refreshCrowding(force: Boolean = false) {
        val fetched = attemptOrNull { services.occupancyRepository.revalidate(force) } ?: return
        crowding.clear()
        crowding.putAll(fetched)
        localStore.saveCrowding(fetched)
    }

    suspend fun flushOutbox() {
        val result = attemptOrNull { services.outboxFlusher.flush() } ?: return
        outboxPending = result.remaining
        if (result.dropped > 0) {
            Toast.makeText(context, "Some changes made offline could not be saved", Toast.LENGTH_SHORT).show()
        }
        if ((OutboxKind.SAVED in result.sentKinds && result.remaining == 0) || result.dropped > 0) {
            attemptOrNull { services.savedRepository.fetch() }?.let {
                savedIds = it
                localStore.saveSavedIds(it)
            }
        }
        if (OutboxKind.OCCUPANCY in result.sentKinds) refreshCrowding(force = true)
        if (OutboxKind.REVIEW in result.sentKinds) {
            refreshReviews(force = true)
            refreshRestaurants(force = true)
        }
    }

    suspend fun enqueueAndFlush(request: OutboxRequest) {
        services.outboxStore.enqueue(request)
        outboxPending = services.outboxStore.count()
        if (services.connectivity.online.value) flushOutbox()
    }

    suspend fun refreshStale() {
        flushOutbox()
        if (refreshing) return
        refreshing = true
        try {
            coroutineScope {
                launch { refreshRestaurants() }
                launch { refreshCatalog() }
            }
            coroutineScope {
                launch { refreshReviews() }
                launch { refreshCrowding() }
            }
        } finally {
            refreshing = false
        }
    }

    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && startupDone) scope.launch { refreshStale() }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    fun recordRecentSearch(name: String) {
        recentSearches = (listOf(name) + recentSearches.filter { it != name }).take(MAX_RECENT_SEARCHES)
        val snapshot = recentSearches
        scope.launch { localStore.saveRecentSearches(snapshot) }
    }

    suspend fun loadRemote() {
        coroutineScope {
            launch {
                services.profileSynchronizer.resolve(user.name)?.let {
                    profile = it
                    surveyDone = true
                }
                profileResolved = true
            }
            launch {
                refreshRestaurants()
                restaurantsResolved = true
            }
            launch {
                refreshCatalog()
                catalogResolved = true
            }
            launch {
                if (services.connectivity.online.value) flushOutbox()
                if (outboxPending == 0) {
                    attemptOrNull { services.savedRepository.fetch() }?.let {
                        savedIds = it
                        localStore.saveSavedIds(it)
                    }
                }
            }
        }
        loadFailed = restaurants.isEmpty() || catalog.isEmpty
        if (loadFailed) return
        coroutineScope {
            launch { refreshReviews() }
            launch { refreshCrowding() }
        }
        startupDone = true
    }

    LaunchedEffect(online) {
        if (online && startupDone) refreshStale()
    }

    LaunchedEffect(user.id) {
        services.prepareLocalData()
        outboxPending = services.outboxStore.count()
        localStore.loadProfileRecord()?.let {
            profile = it.profile
            surveyDone = true
        }
        reviews.addAll(localStore.loadReviews())
        crowding.putAll(localStore.loadCrowding())
        savedIds = localStore.loadSavedIds()
        recentSearches = localStore.loadRecentSearches()
        restaurants = applyRatingSummaries(
            services.restaurantRepository.cached(),
            services.reviewRepository.cachedSummaries()
        )
        catalog = services.catalogRepository.cached()
        profileResolved = surveyDone
        restaurantsResolved = restaurants.isNotEmpty()
        catalogResolved = !catalog.isEmpty
        loadRemote()
    }

    // Sensor GPS: se pide el permiso al terminar el onboarding y se comparte con las pantallas
    val userLocation = rememberUserLocation()
    LaunchedEffect(surveyDone) {
        if (surveyDone && !userLocation.hasPermission) userLocation.requestPermission()
    }

    if (loadFailed) {
        ConnectionErrorView(
            onRetry = {
                loadFailed = false
                restaurantsResolved = false
                catalogResolved = false
                scope.launch { loadRemote() }
            }
        )
        return
    }

    if (!profileResolved || !restaurantsResolved || !catalogResolved) {
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
                        profile = completed
                        surveyDone = true
                        scope.launch { services.profileSynchronizer.update(completed) }
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
        val wasSaved = id in savedIds
        savedIds = if (wasSaved) savedIds - id else savedIds + id
        val snapshot = savedIds
        scope.launch {
            localStore.saveSavedIds(snapshot)
            sections.onInteraction(
                AppSection.Favorites,
                if (wasSaved) SectionAction.FavoriteRemoved else SectionAction.FavoriteAdded,
                visitSection = AppSection.Details
            )
            enqueueAndFlush(
                if (wasSaved) services.savedRepository.removeRequest(id) else services.savedRepository.addRequest(id)
            )
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

    val mapVisible = overlay == null && currentTab == MAP_TAB
    val favoritesVisible = overlay == null && currentTab == FAVORITES_TAB
    val detailsVisible = overlay == Overlay.Detail || overlay == Overlay.WriteReview || overlay == Overlay.ReviewsList || overlay == Overlay.PartnerReport
    SectionVisibilityEffect(AppSection.Map, mapVisible, sections)
    SectionVisibilityEffect(AppSection.Favorites, favoritesVisible, sections)
    SectionVisibilityEffect(AppSection.Details, detailsVisible, sections)

    BackHandler(enabled = pendingRestaurant != null || overlayStack.isNotEmpty()) {
        when {
            pendingRestaurant != null -> pendingRestaurant = null
            overlay == Overlay.Detail -> closeAll()
            else -> popOverlay()
        }
    }

    // Obtenemos las reseñas que estan en cache
    val cachedRestaurantReviews = selectedRestaurant
        ?.let { r -> reviews.filter { it.restaurantId == r.id } }
        .orEmpty()

    // Obtenemos las reseñas que estan en el Outbox
    var pendingReviews by remember { mutableStateOf<List<Review>>(emptyList()) }

    LaunchedEffect(outboxPending, selectedRestaurant) {
        val pendingEntries = services.localStore.pending().filter {
            it.request.kind == OutboxKind.REVIEW &&
                    it.request.path.contains(selectedRestaurant?.id ?: "")
        }

        pendingReviews = pendingEntries.mapNotNull { entry ->
            val body = entry.request.body ?: return@mapNotNull null
            try {
                val ratings = ReviewRatings(
                    taste = (body["taste"] as? Number)?.toInt() ?: 0,
                    attention = (body["attention"] as? Number)?.toInt() ?: 0,
                    price = (body["price"] as? Number)?.toInt() ?: 0,
                    options = (body["options"] as? Number)?.toInt() ?: 0
                )

                val rawDiets = body["dietary_options"] as? List<*>
                val dietaryOptions = rawDiets?.map { it.toString() } ?: emptyList()

                Review(
                    id = "pending_${entry.id}",
                    restaurantId = selectedRestaurant?.id ?: "",
                    author = profile.name.ifEmpty { user.name },
                    date = "Pending sync...",
                    ratings = ratings,
                    dietaryOptions = dietaryOptions,
                    comment = (body["comment"] as? String).orEmpty()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    val restaurantReviews = pendingReviews + cachedRestaurantReviews

    val mealRules = remember(catalog) { MealRules(catalog) }
    CompositionLocalProvider(LocalRestaurants provides restaurants, LocalCatalog provides catalog, LocalMealRules provides mealRules) {
        Scaffold(
            bottomBar = {
                if (overlay != Overlay.Survey) Column {
                    if (!online) OfflineBanner(outboxPending)
                    else if (outboxPending > 0) PendingSyncBanner(outboxPending)
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
                val authViewModel = null
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
                        onWriteReview = {
                            sections.onInteraction(AppSection.Details, SectionAction.ReviewWriteOpened)
                            overlayStack.add(Overlay.WriteReview)
                        },
                        onSeeReviews = {
                            sections.onInteraction(AppSection.Details, SectionAction.ReviewsOpened)
                            overlayStack.add(Overlay.ReviewsList)
                        },
                        onPartnerReport = { overlayStack.add(Overlay.PartnerReport) },
                        onGetDirections = { tags ->
                            sections.onInteraction(AppSection.Details, SectionAction.DirectionsOpened)
                            analytics.onGoDecision(restaurant.id)
                            analytics.onTagClick(tags)
                        }
                    )

                    overlay == Overlay.WriteReview && restaurant != null -> WriteReviewView(
                        restaurant = restaurant,
                        authorName = profile.name,
                        onSubmit = { review ->
                            scope.launch {
                                if (!services.connectivity.online.value) {
                                    Toast.makeText(
                                        context,
                                        "You're offline. Your review will be posted when you reconnect.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                enqueueAndFlush(services.reviewRepository.submitRequest(review))
                            }
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

                    overlay == Overlay.PartnerReport && restaurant != null -> PartnerReportView(
                        restaurant = restaurant,
                        loadReport = { id, days -> services.partnerReportRepository.report(id, days) },
                        onBack = ::popOverlay
                    )

                    overlay == Overlay.Survey -> SurveyView(
                        initialName = profile.name,
                        onComplete = { answers ->
                            val updated = answers.copy(name = answers.name.ifBlank { user.name })
                            analytics.onProfileSaved(profile, updated)
                            profile = updated
                            scope.launch { services.profileSynchronizer.update(updated) }
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
                            onFilterUsed = { analytics.onFilterUsed(it, AnalyticsScreen.Home) },
                            onFilterSheetOpened = { sections.onSectionShown(AppSection.Filters) },
                            onFilterSheetClosed = { sections.onSectionLeft(AppSection.Filters) },
                            onFiltersApplied = {
                                sections.onInteraction(AppSection.Filters, SectionAction.FiltersApplied)
                            },
                            onFiltersCleared = {
                                sections.onInteraction(AppSection.Filters, SectionAction.FiltersCleared)
                            }
                        )
                        1 -> SearchView(
                            onSelect = {
                                recordRecentSearch(it.name)
                                requestDetail(it)
                            },
                            crowding = crowding,
                            userLocation = userLocation.location,
                            onFilterUsed = { analytics.onFilterUsed(it, AnalyticsScreen.Search) },
                            recentSearches = recentSearches,
                            onClearRecent = {
                                recentSearches = emptyList()
                                scope.launch { localStore.saveRecentSearches(emptyList()) }
                            }
                        )
                        MAP_TAB -> MapView(
                            onSelect = {
                                sections.onInteraction(AppSection.Map, SectionAction.RestaurantSelected)
                                requestDetail(it)
                            },
                            crowding = crowding,
                            userLocation = userLocation.location,
                            hasLocationPermission = userLocation.hasPermission,
                            onRetryLocation = userLocation.requestPermission,
                            onFilterUsed = {
                                sections.onInteraction(AppSection.Map, SectionAction.FilterChipChanged)
                                analytics.onFilterUsed(it, AnalyticsScreen.Map)
                            },
                            onMarkerSelected = {
                                sections.onInteraction(AppSection.Map, SectionAction.MarkerSelected)
                            }
                        )
                        FAVORITES_TAB -> SavedView(
                            onSelect = {
                                sections.onInteraction(AppSection.Favorites, SectionAction.SavedItemSelected)
                                requestDetail(it)
                            },
                            crowding = crowding,
                            savedIds = savedIds,
                            userLocation = userLocation.location
                        )
                        PROFILE_TAB -> ProfileView(
                            profile = profile,
                            userEmail = user.email,
                            showLogoutWarning = auth.showLogoutWarning.collectAsState().value,
                            onRetakeSurvey = { overlayStack.add(Overlay.Survey) },
                            onLogoutAttempt = { auth.attemptLogout() },
                            onLogoutConfirm = {
                                scope.launch {
                                    if (services.connectivity.online.value) attemptOrNull { services.outboxFlusher.flush() }
                                    services.localStore.clearUserData()
                                    auth.confirmLogout()
                                }
                            },
                            onLogoutCancel = { auth.cancelLogout() },
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
                                scope.launch {
                                    crowding[pending.id] = crowding[pending.id].orEmpty() + level
                                    localStore.saveCrowding(crowding.toMap())
                                    enqueueAndFlush(services.occupancyRepository.reportRequest(pending.id, level))
                                }
                                commitDetail(pending)
                            },
                            onSkip = { commitDetail(pending) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionVisibilityEffect(section: AppSection, visible: Boolean, sections: SectionAnalyticsViewModel) {
    LaunchedEffect(visible) {
        if (visible) sections.onSectionShown(section) else sections.onSectionLeft(section)
    }
    DisposableEffect(Unit) {
        onDispose { sections.onSectionLeft(section) }
    }
}   
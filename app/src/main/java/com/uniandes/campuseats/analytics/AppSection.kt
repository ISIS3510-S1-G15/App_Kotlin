package com.uniandes.campuseats.analytics

enum class AppSection(val wireName: String) {
    Map("map"),
    Filters("filters"),
    Details("details"),
    Favorites("favorites")
}

enum class SectionAction(val wireName: String) {
    MarkerSelected("marker_selected"),
    RestaurantSelected("restaurant_selected"),
    FilterChipChanged("filter_chip_changed"),
    FiltersApplied("filters_applied"),
    FiltersCleared("filters_cleared"),
    DirectionsOpened("directions_opened"),
    ReviewsOpened("reviews_opened"),
    ReviewWriteOpened("review_write_opened"),
    FavoriteAdded("favorite_added"),
    FavoriteRemoved("favorite_removed"),
    SavedItemSelected("saved_item_selected")
}

package com.uniandes.campuseats.analytics

enum class AnalyticsScreen(val wireName: String) {
    Home("home"),
    Search("search"),
    Map("map"),
    Saved("saved"),
    Profile("profile");

    companion object {
        fun fromTab(index: Int): AnalyticsScreen = entries.getOrElse(index) { Home }
    }
}

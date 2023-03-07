package com.example.nsdshare.Navigation

sealed class Screens(val route: String) {
    object Home: Screens(route = "home_screen")
    object Discover: Screens(route = "discover_screen")
}

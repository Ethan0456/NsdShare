package com.example.nsdshare.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.nsdshare.DiscoverScreen
import com.example.nsdshare.NsdShareViewModel
import com.example.nsdshare.Screens.HomeScreen

@Composable
fun SetupNavGraph(
    nsdShareViewModel: NsdShareViewModel,
    navHostController: NavHostController
) {
    NavHost(
        navController = navHostController,
        startDestination = Screens.Home.route
    ) {
        composable(
            route = Screens.Home.route
        ) {
            HomeScreen(nsdShareViewModel, navHostController)
        }
        composable(
            route = Screens.Discover.route
        ) {
            DiscoverScreen(nsdShareViewModel, navHostController)
        }
    }
}
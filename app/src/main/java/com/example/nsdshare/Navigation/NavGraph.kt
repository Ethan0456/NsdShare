package com.example.nsdshare.Navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.nsdshare.NsdShareViewModel
import com.example.nsdshare.Screens.HomeScreen

@RequiresApi(Build.VERSION_CODES.Q)
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
    }
}
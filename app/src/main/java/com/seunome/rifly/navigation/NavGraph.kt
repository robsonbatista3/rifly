package com.seunome.rifly.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.ui.auth.AuthViewModel
import com.seunome.rifly.ui.auth.LoginScreen
import com.seunome.rifly.ui.auth.RegisterScreen
import com.seunome.rifly.ui.create.CreateRaffleScreen
import com.seunome.rifly.ui.detail.RaffleDetailScreen
import com.seunome.rifly.ui.detail.RaffleDetailViewModel
import com.seunome.rifly.ui.home.HomeScreen
import com.seunome.rifly.ui.home.HomeViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class NavRoute(val route: String) {
    object Login : NavRoute("login")
    object Register : NavRoute("register")
    object Home : NavRoute("home")
    object CreateRaffle : NavRoute("create_raffle")
    object RaffleDetail : NavRoute("raffle_detail/{raffleJson}") {
        fun createRoute(raffle: Raffle): String {
            val json = Uri.encode(Json.encodeToString(raffle))
            return "raffle_detail/$json"
        }
    }
}

@Composable
fun RiflyNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()

    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val raffles by homeViewModel.raffles.collectAsState()
    val isLoadingRaffles by homeViewModel.isLoading.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            navController.navigate(NavRoute.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = if (isAuthenticated) NavRoute.Home.route else NavRoute.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavRoute.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(NavRoute.Register.route)
                },
                onLoginSuccess = {
                    homeViewModel.loadRaffles()
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    homeViewModel.loadRaffles()
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.Home.route) {
            HomeScreen(
                raffles = raffles,
                isLoading = isLoadingRaffles,
                onCreateRaffle = {
                    navController.navigate(NavRoute.CreateRaffle.route)
                },
                onLogout = {
                    authViewModel.logout()
                },
                onRaffleClick = { raffle ->
                    navController.navigate(NavRoute.RaffleDetail.createRoute(raffle))
                }
            )
        }

        composable(NavRoute.CreateRaffle.route) {
            CreateRaffleScreen(
                onBack = {
                    navController.popBackStack()
                },
                onRaffleCreated = {
                    homeViewModel.loadRaffles()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = NavRoute.RaffleDetail.route,
            arguments = listOf(navArgument("raffleJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("raffleJson") ?: ""
            val raffle = Json.decodeFromString<Raffle>(Uri.decode(json))
            val detailViewModel: RaffleDetailViewModel = viewModel()

            LaunchedEffect(raffle) {
                detailViewModel.setup(raffle)
            }

            RaffleDetailScreen(
                raffle = raffle,
                onBack = { navController.popBackStack() },
                viewModel = detailViewModel
            )
        }
    }
}

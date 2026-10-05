package com.seunome.rifly.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.seunome.rifly.ui.home.HomeScreen
import com.seunome.rifly.ui.home.HomeViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object CreateRaffle : Screen("create_raffle")
    object RaffleDetail : Screen("raffle_detail/{raffleJson}") {
        fun createRoute(raffle: Raffle): String {
            val json = Uri.encode(Json.encodeToString(raffle))
            return "raffle_detail/$json"
        }
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()

    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val raffles by homeViewModel.raffles.collectAsState()
    val isLoadingRaffles by homeViewModel.isLoading.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = if (isAuthenticated) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    homeViewModel.loadRaffles()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    homeViewModel.loadRaffles()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                raffles = raffles,
                isLoading = isLoadingRaffles,
                onCreateRaffle = {
                    navController.navigate(Screen.CreateRaffle.route)
                },
                onLogout = {
                    authViewModel.logout()
                },
                onRaffleClick = { raffle ->
                    navController.navigate(Screen.RaffleDetail.createRoute(raffle))
                }
            )
        }

        composable(Screen.CreateRaffle.route) {
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
            route = Screen.RaffleDetail.route,
            arguments = listOf(navArgument("raffleJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("raffleJson") ?: ""
            val raffle = Json.decodeFromString<Raffle>(Uri.decode(json))
            RaffleDetailScreen(
                raffle = raffle,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

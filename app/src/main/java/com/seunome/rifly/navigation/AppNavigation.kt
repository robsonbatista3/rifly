package com.seunome.rifly.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seunome.rifly.data.model.Raffle
import com.seunome.rifly.data.repository.RaffleRepository
import com.seunome.rifly.ui.admin.AdminCreatorsScreen
import com.seunome.rifly.ui.admin.AdminDashboardScreen
import com.seunome.rifly.ui.admin.AdminRafflesScreen
import com.seunome.rifly.ui.admin.AdminReservationsScreen
import com.seunome.rifly.ui.admin.AdminViewModel
import com.seunome.rifly.ui.auth.AuthViewModel
import com.seunome.rifly.ui.auth.LoginScreen
import com.seunome.rifly.ui.auth.RegisterScreen
import com.seunome.rifly.ui.create.CreateRaffleScreen
import com.seunome.rifly.ui.detail.RaffleDetailScreen
import com.seunome.rifly.ui.detail.RaffleDetailViewModel
import com.seunome.rifly.ui.home.HomeScreen
import com.seunome.rifly.ui.home.HomeViewModel
import com.seunome.rifly.ui.notifications.NotificationsScreen
import com.seunome.rifly.ui.notifications.NotificationsViewModel
import com.seunome.rifly.ui.report.ReportScreen
import com.seunome.rifly.ui.report.ReportViewModel
import com.seunome.rifly.util.Utils
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object CreateRaffle : Screen("create_raffle")
    object Notifications : Screen("notifications")
    object Admin : Screen("admin")
    object AdminRaffles : Screen("admin_raffles")
    object AdminCreators : Screen("admin_creators")
    object AdminReservations : Screen("admin_reservations")
    object Report : Screen("report/{raffleJson}") {
        fun createRoute(raffle: Raffle): String {
            val json = Uri.encode(Json.encodeToString(raffle))
            return "report/$json"
        }
    }
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
    val adminViewModel: AdminViewModel = viewModel()
    val raffleRepository = RaffleRepository()
    val scope = rememberCoroutineScope()

    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val raffles by homeViewModel.raffles.collectAsState()
    val isLoadingRaffles by homeViewModel.isLoading.collectAsState()
    val unreadCount by homeViewModel.unreadCount.collectAsState()
    val isAdmin by homeViewModel.isAdmin.collectAsState()

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
            val context = LocalContext.current
            HomeScreen(
                raffles = raffles,
                isLoading = isLoadingRaffles,
                unreadCount = unreadCount,
                isAdmin = isAdmin,
                onCreateRaffle = {
                    navController.navigate(Screen.CreateRaffle.route)
                },
                onLogout = {
                    authViewModel.logout()
                },
                onRaffleClick = { raffle ->
                    navController.navigate(Screen.RaffleDetail.createRoute(raffle))
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Notifications.route)
                },
                onOpenWinners = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Utils.buildWinnersLink()))
                    context.startActivity(intent)
                },
                onOpenAdmin = {
                    navController.navigate(Screen.Admin.route)
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

        composable(Screen.Notifications.route) {
            val notificationsViewModel: NotificationsViewModel = viewModel()
            NotificationsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNotificationClick = { notification ->
                    if (!notification.raffleId.isNullOrBlank()) {
                        scope.launch {
                            val result = raffleRepository.getRaffleById(notification.raffleId)
                            result.getOrNull()?.let { raffle ->
                                navController.navigate(Screen.RaffleDetail.createRoute(raffle))
                            }
                        }
                    }
                },
                viewModel = notificationsViewModel
            )
        }

        composable(Screen.Admin.route) {
            AdminDashboardScreen(
                onBack = { navController.popBackStack() },
                onOpenRaffles = { navController.navigate(Screen.AdminRaffles.route) },
                onOpenCreators = { navController.navigate(Screen.AdminCreators.route) },
                onOpenReservations = { navController.navigate(Screen.AdminReservations.route) },
                viewModel = adminViewModel
            )
        }

        composable(Screen.AdminRaffles.route) {
            AdminRafflesScreen(
                onBack = { navController.popBackStack() },
                viewModel = adminViewModel
            )
        }

        composable(Screen.AdminCreators.route) {
            AdminCreatorsScreen(
                onBack = { navController.popBackStack() },
                viewModel = adminViewModel
            )
        }

        composable(Screen.AdminReservations.route) {
            AdminReservationsScreen(
                onBack = { navController.popBackStack() },
                viewModel = adminViewModel
            )
        }

        composable(
            route = Screen.Report.route,
            arguments = listOf(navArgument("raffleJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("raffleJson") ?: ""
            val raffle = Json.decodeFromString<Raffle>(Uri.decode(json))
            val reportViewModel: ReportViewModel = viewModel()

            LaunchedEffect(raffle) {
                reportViewModel.setup(raffle)
            }

            ReportScreen(
                onBack = { navController.popBackStack() },
                viewModel = reportViewModel
            )
        }

        composable(
            route = Screen.RaffleDetail.route,
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
                onOpenReport = { navController.navigate(Screen.Report.createRoute(raffle)) },
                viewModel = detailViewModel
            )
        }
    }
}

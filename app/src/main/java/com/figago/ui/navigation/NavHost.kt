package com.figago.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.figago.ui.history.DayDetailScreen
import com.figago.ui.history.HistoryScreen
import com.figago.ui.dashboard.DashboardScreen
import com.figago.ui.map.MapScreen
import com.figago.ui.settings.SettingsScreen

import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.figago.ui.main.MainViewModel
import com.figago.R

/**
 * Маршруты навигации приложения FigaGo.
 */
object Routes {
    const val MAP = "map"
    const val HISTORY = "history"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val DAY_DETAIL = "day_detail/{dayId}?showAll={showAll}"
    const val DIAGNOSTICS = "diagnostics"

    fun dayDetail(dayId: Long, showAll: Boolean) = "day_detail/$dayId?showAll=$showAll"
}

/**
 * Корневой NavHost приложения FigaGo.
 *
 * Маршруты:
 * - main       → Главный экран (управление, дистанция, карта)
 * - history    → Список суточных сессий
 * - day_detail → Детали конкретного дня (карта + статистика)
 *
 * Анимации: slide + fade для плавных переходов.
 */
@Composable
fun FigaGoNavHost(startDestination: String = Routes.DASHBOARD) {
    val navController = rememberNavController()
    val animDuration = 300

    // Получаем активный профиль для динамической закладки «Я»
    val mainViewModel: MainViewModel = hiltViewModel()
    val activeProfile by mainViewModel.activeProfile.collectAsStateWithLifecycle()

    val profileLabel = activeProfile?.name ?: stringResource(com.figago.R.string.nav_me)
    val defaultIcon = if (activeProfile?.type == com.figago.data.entity.ProfileType.ELECTRIC) com.figago.R.drawable.ic_notification_preview else com.figago.R.drawable.ic_wheelchair_manual
    val safeIconId = activeProfile?.iconId?.takeIf { it != 0 } ?: defaultIcon

    // Данные навигации: route → (label, iconComposable)
    data class NavItem(val route: String, val label: String, val icon: @Composable () -> Unit)

    val navItems = listOf(
        NavItem(
            route = Routes.MAP,
            label = profileLabel,
            icon = {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(safeIconId),
                    contentDescription = profileLabel,
                    modifier = Modifier.size(24.dp),
                )
            }
        ),
        NavItem(Routes.HISTORY, stringResource(com.figago.R.string.nav_history)) { Icon(Icons.Filled.History, contentDescription = null) },
        NavItem(Routes.DASHBOARD, stringResource(com.figago.R.string.nav_start)) { Icon(Icons.Filled.Dashboard, contentDescription = null) },
        NavItem(Routes.SETTINGS, stringResource(com.figago.R.string.nav_settings)) { Icon(Icons.Filled.Settings, contentDescription = null) },
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route

            if (currentRoute in navItems.map { it.route }) {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label, maxLines = 1) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        // Состояние для диалога смены профиля
        var showSwitchDialog by remember { mutableStateOf(false) }
        var pendingSwitchProfileId by remember { mutableLongStateOf(0L) }

        val profiles by mainViewModel.profiles.collectAsStateWithLifecycle()
        val trackingState by mainViewModel.trackingState.collectAsStateWithLifecycle()
        val dayState by mainViewModel.uiState.collectAsStateWithLifecycle()

        // Определяем, показывать ли карусель (только на основных вкладках)
        val navBackStackForCarousel by navController.currentBackStackEntryAsState()
        val showCarousel = navBackStackForCarousel?.destination?.route in listOf(
            Routes.MAP, Routes.HISTORY, Routes.DASHBOARD, Routes.SETTINGS
        )

        Column(modifier = Modifier.padding(innerPadding)) {

            // ===== Глобальная карусель профилей =====
            if (showCarousel) {

                Spacer(modifier = Modifier.height(8.dp))

                com.figago.ui.dashboard.ProfileCarousel(
                    profiles = profiles,
                    activeProfileId = activeProfile?.id,
                    onProfileClick = { profileId ->
                        // Если идёт запись — показываем диалог подтверждения
                        if (dayState.dayState == com.figago.ui.main.DayState.RECORDING ||
                            dayState.dayState == com.figago.ui.main.DayState.PAUSED) {
                            pendingSwitchProfileId = profileId
                            showSwitchDialog = true
                        } else {
                            mainViewModel.switchActiveProfile(profileId)
                        }
                    },
                    onAddProfileClick = {
                        mainViewModel.createAndSwitchProfile {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    // Клик на активную плитку — переходим в настройки
                    onEditProfileClick = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

            }

            // ===== NavHost с маршрутами =====
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.weight(1f),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) + fadeIn(tween(animDuration))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) + fadeOut(tween(animDuration))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) + fadeIn(tween(animDuration))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) + fadeOut(tween(animDuration))
            },
        ) {

            // Карта (Я)
            composable(Routes.MAP) {
                MapScreen()
            }

            // Дашборд (Старт/Запись)
            composable(
                route = Routes.DASHBOARD,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "figago://command/start_track" },
                    navDeepLink { uriPattern = "https://figago.app/command/start_track" },
                    navDeepLink { uriPattern = "figago://command/stop_track" },
                    navDeepLink { uriPattern = "https://figago.app/command/stop_track" },
                    navDeepLink { uriPattern = "figago://command/led_event" },
                    navDeepLink { uriPattern = "https://figago.app/command/led_event" }
                )
            ) {
                DashboardScreen(
                    onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) }
                )
            }

            // Экран настроек
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Экран истории
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onDaySelected = { dayId, showAll ->
                        navController.navigate(Routes.dayDetail(dayId, showAll))
                    },
                )
            }

            // Детали дня
            composable(
                route = Routes.DAY_DETAIL,
                arguments = listOf(
                    navArgument("dayId") { type = NavType.LongType },
                    navArgument("showAll") { type = NavType.BoolType; defaultValue = false }
                ),
            ) { backStackEntry ->

                val dayId = backStackEntry.arguments?.getLong("dayId") ?: return@composable
                val showAll = backStackEntry.arguments?.getBoolean("showAll") ?: false

                DayDetailScreen(
                    sessionId = dayId,
                    showAllProfiles = showAll,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Экран Диагностики
            composable(Routes.DIAGNOSTICS) {
                com.figago.ui.diagnostics.DiagnosticsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onForceStart = {
                        mainViewModel.startDay()
                        mainViewModel.startTrack()
                        navController.popBackStack()
                    }
                )
            }
        }
        }

        // ===== Диалог подтверждения смены профиля =====
        if (showSwitchDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSwitchDialog = false },
                title = { Text(stringResource(R.string.dialog_switch_profile_title)) },
                text = { Text(stringResource(R.string.dialog_switch_profile_text)) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showSwitchDialog = false
                        mainViewModel.stopTrack()
                        mainViewModel.switchActiveProfile(pendingSwitchProfileId)
                        mainViewModel.startTrack()
                    }) {
                        Text(stringResource(R.string.dialog_switch_confirm))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showSwitchDialog = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
    }
}


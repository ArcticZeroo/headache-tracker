package com.example.headachetracker

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.headachetracker.ui.analysis.AnalysisScreen
import com.example.headachetracker.ui.entry.EditEntryScreen
import com.example.headachetracker.ui.entry.QuickLogScreen
import com.example.headachetracker.ui.history.HistoryScreen
import com.example.headachetracker.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object History : Screen("history", "History", Icons.Default.History)
    data object Analysis : Screen("analysis", "Analysis", Icons.Default.BarChart)
    data object QuickLog : Screen("quick_log", "Log", Icons.Default.Add)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavScreens = listOf(Screen.History, Screen.Analysis, Screen.QuickLog, Screen.Settings)

@Composable
fun MainNavigation(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val showNav = bottomNavScreens.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showNav && !useNavRail) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = { navigateTo(screen.route) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.padding(innerPadding)) {
            if (showNav && useNavRail) {
                NavigationRail {
                    bottomNavScreens.forEach { screen ->
                        NavigationRailItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = { navigateTo(screen.route) }
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = Screen.History.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.History.route) {
                    HistoryScreen(
                        onEditEntry = { entryId ->
                            navController.navigate("edit/$entryId")
                        },
                        isExpanded = isExpanded
                    )
                }
                composable(Screen.Analysis.route) {
                    AnalysisScreen(isExpanded = isExpanded)
                }
                composable(Screen.QuickLog.route) {
                    QuickLogScreen(
                        isExpanded = isExpanded,
                        onEntryLogged = { navigateTo(Screen.History.route) }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
                composable(
                    route = "edit/{entryId}",
                    arguments = listOf(navArgument("entryId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val entryId = backStackEntry.arguments?.getLong("entryId")
                        ?: return@composable
                    EditEntryScreen(
                        entryId = entryId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

package com.localtube.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localtube.R
import com.localtube.ui.screens.AddScreen
import com.localtube.ui.screens.DetailsScreen
import com.localtube.ui.screens.DownloadsScreen
import com.localtube.ui.screens.LibraryScreen
import com.localtube.ui.screens.PlayerScreen
import com.localtube.ui.screens.SettingsScreen

/** Маршруты, где показывается нижняя панель. */
private val topLevelRoutes = setOf(
    Routes.LIBRARY,
    Routes.DOWNLOADS,
    Routes.ADD,
    Routes.SETTINGS,
)

/** Корневой экран: NavHost + нижняя панель (Библиотека/Загрузки/Добавить/Настройки). */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                BottomBar(currentRoute, navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LIBRARY) { LibraryScreen() }
            composable(Routes.DOWNLOADS) { DownloadsScreen() }
            composable(Routes.ADD) { AddScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(
                route = Routes.DETAILS,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                DetailsScreen(id = entry.arguments?.getLong("id") ?: 0L)
            }
            composable(
                route = Routes.PLAYER,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                PlayerScreen(id = entry.arguments?.getLong("id") ?: 0L)
            }
        }
    }
}

@Composable
private fun BottomBar(currentRoute: String?, navController: NavHostController) {
    NavigationBar {
        bottomItem(
            route = Routes.LIBRARY,
            labelRes = R.string.nav_library,
            icon = Icons.Filled.LibraryMusic,
            currentRoute = currentRoute,
            navController = navController,
        )
        bottomItem(
            route = Routes.DOWNLOADS,
            labelRes = R.string.nav_downloads,
            icon = Icons.Filled.Download,
            currentRoute = currentRoute,
            navController = navController,
        )
        bottomItem(
            route = Routes.ADD,
            labelRes = R.string.nav_add,
            icon = Icons.Filled.Add,
            currentRoute = currentRoute,
            navController = navController,
        )
        bottomItem(
            route = Routes.SETTINGS,
            labelRes = R.string.nav_settings,
            icon = Icons.Filled.Settings,
            currentRoute = currentRoute,
            navController = navController,
        )
    }
}

@Composable
private fun RowScope.bottomItem(
    route: String,
    labelRes: Int,
    icon: ImageVector,
    currentRoute: String?,
    navController: NavHostController,
) {
    val label = stringResource(labelRes)
    NavigationBarItem(
        selected = currentRoute == route,
        onClick = {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
    )
}

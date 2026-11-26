package com.pareto.timetracker.ui

import androidx.annotation.StringRes
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pareto.timetracker.R
import com.pareto.timetracker.ui.archive.ArchiveScreen
import com.pareto.timetracker.ui.charts.ChartsScreen
import com.pareto.timetracker.ui.goals.GoalsScreen
import com.pareto.timetracker.ui.home.HomeScreen

private object Routes {
    const val HOME = "home"
    const val CHARTS = "charts"
    const val GOALS = "goals"
    const val ARCHIVE = "archive"
}

private const val SCREEN_FADE_MS = 90

private data class Tab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Outlined.Home),
    Tab(Routes.CHARTS, R.string.nav_charts, Icons.Outlined.BarChart),
    Tab(Routes.GOALS, R.string.nav_goals, Icons.Outlined.Flag),
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            // Varsayılan geçiş 700 ms'lik çapraz solma: sekme değişimi "1 saniye sonra" geliyormuş gibi hissettiriyordu.
            enterTransition = { fadeIn(tween(SCREEN_FADE_MS)) },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { fadeIn(tween(SCREEN_FADE_MS)) },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Routes.HOME) {
                HomeScreen(onOpenArchive = { navController.navigate(Routes.ARCHIVE) })
            }
            composable(Routes.CHARTS) { ChartsScreen() }
            composable(Routes.GOALS) { GoalsScreen() }
            composable(Routes.ARCHIVE) {
                ArchiveScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

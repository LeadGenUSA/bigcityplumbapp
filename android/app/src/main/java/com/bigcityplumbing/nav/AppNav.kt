package com.bigcityplumbing.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bigcityplumbing.screens.HelpGuideDetailScreen
import com.bigcityplumbing.screens.HelpGuidesScreen
import com.bigcityplumbing.screens.HomeScreen
import com.bigcityplumbing.screens.PipePuzzleScreen
import com.bigcityplumbing.screens.ServiceRequestScreen
import com.bigcityplumbing.screens.VideoHubScreen

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Dest("home", "Home", Icons.Outlined.Home)
    data object Game : Dest("game", "Game", Icons.Outlined.Extension)
    data object Videos : Dest("videos", "Videos", Icons.Outlined.PlayArrow)
    data object Guides : Dest("guides", "Guides", Icons.Outlined.Info)
    data object Service : Dest("service", "Service", Icons.Outlined.Build)
    data object Contact : Dest("contact", "Contact", Icons.Outlined.Mail)
    // Detail routes
    data object GuideDetail : Dest("guide/{id}", "Guide", Icons.Outlined.Info) {
        fun build(id: String) = "guide/$id"
    }
}

private val bottomTabs = listOf(Dest.Home, Dest.Game, Dest.Videos, Dest.Guides, Dest.Service)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Hide bottom bar on detail screens for full-bleed reading
            if (currentRoute == null || currentRoute in bottomTabs.map { it.route }) {
                NavigationBar {
                    bottomTabs.forEach { dest ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(dest.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController = nav, startDestination = Dest.Home.route) {
                composable(Dest.Home.route) {
                    HomeScreen(
                        onNavigate = { route -> nav.navigate(route) }
                    )
                }
                composable(Dest.Game.route) { PipePuzzleScreen() }
                composable(Dest.Videos.route) { VideoHubScreen() }
                composable(Dest.Guides.route) {
                    HelpGuidesScreen(
                        onOpen = { id -> nav.navigate(Dest.GuideDetail.build(id)) }
                    )
                }
                composable(Dest.Service.route) { ServiceRequestScreen() }
                composable(Dest.GuideDetail.route) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    HelpGuideDetailScreen(
                        guideId = id,
                        onBack = { nav.popBackStack() },
                    )
                }
            }
        }
    }
}

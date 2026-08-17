package com.rugplayer.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.Composable
import com.rugplayer.app.AppGraph
import com.rugplayer.app.ui.library.LibraryScreen
import com.rugplayer.app.ui.player.PlayerScreen
import com.rugplayer.app.ui.settings.SettingsScreen
import com.rugplayer.app.ui.transfer.TransferScreen

object Routes {
    const val LIBRARY = "library"
    const val PLAYER = "player/{videoId}"
    const val SETTINGS = "settings"
    const val TRANSFER = "transfer"

    fun player(videoId: Long) = "player/$videoId"
}

@Composable
fun RugNavHost(graph: AppGraph) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(150)) },
        popExitTransition = { fadeOut(tween(150)) },
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                graph = graph,
                onOpenVideo = { videoId -> navController.navigate(Routes.player(videoId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenTransfer = { navController.navigate(Routes.TRANSFER) },
            )
        }

        composable(Routes.TRANSFER) {
            TransferScreen(
                graph = graph,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("videoId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong("videoId") ?: return@composable
            PlayerScreen(
                graph = graph,
                initialVideoId = videoId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                graph = graph,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

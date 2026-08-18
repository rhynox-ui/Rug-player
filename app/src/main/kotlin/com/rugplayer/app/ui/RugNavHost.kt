package com.rugplayer.app.ui

import android.net.Uri
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
import com.rugplayer.app.ui.library.FolderVideosScreen
import com.rugplayer.app.ui.library.LibraryScreen
import com.rugplayer.app.ui.player.PlayerScreen
import com.rugplayer.app.ui.settings.SettingsScreen
import com.rugplayer.app.ui.statussaver.StatusSaverScreen
import com.rugplayer.app.ui.transfer.TransferScreen

object Routes {
    const val LIBRARY = "library"
    const val PLAYER = "player/{videoId}"
    const val PLAYER_STREAM = "player_stream/{url}/{title}"
    const val SETTINGS = "settings"
    const val TRANSFER = "transfer"
    const val FOLDER = "folder/{folderName}"
    const val STATUS_SAVER = "status_saver"

    fun player(videoId: Long) = "player/$videoId"
    fun playerStream(url: String, title: String) =
        "player_stream/${Uri.encode(url)}/${Uri.encode(title)}"
    fun folder(name: String) = "folder/${Uri.encode(name)}"
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
                onOpenFolder = { name -> navController.navigate(Routes.folder(name)) },
                onOpenStatusSaver = { navController.navigate(Routes.STATUS_SAVER) },
            )
        }

        composable(
            route = Routes.FOLDER,
            arguments = listOf(navArgument("folderName") { type = NavType.StringType }),
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName")?.let(Uri::decode) ?: return@composable
            FolderVideosScreen(
                graph = graph,
                folderName = folderName,
                onOpenVideo = { videoId -> navController.navigate(Routes.player(videoId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.TRANSFER) {
            TransferScreen(
                graph = graph,
                onBack = { navController.popBackStack() },
                onPlayStream = { url, title ->
                    navController.navigate(Routes.playerStream(url, title))
                },
            )
        }

        composable(Routes.STATUS_SAVER) {
            StatusSaverScreen(
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

        composable(
            route = Routes.PLAYER_STREAM,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url")?.let(Uri::decode) ?: return@composable
            val title = backStackEntry.arguments?.getString("title")?.let(Uri::decode) ?: "Network stream"
            PlayerScreen(
                graph = graph,
                streamUrl = url,
                streamTitle = title,
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

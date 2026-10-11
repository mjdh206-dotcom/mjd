package com.mjd.videoforge.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mjd.videoforge.ui.screens.home.HomeScreen
import com.mjd.videoforge.ui.screens.home.SmartQuestionsScreen
import com.mjd.videoforge.ui.screens.storyboard.StoryboardScreen
import com.mjd.videoforge.ui.screens.pipeline.PipelineScreen
import com.mjd.videoforge.ui.screens.preview.PreviewScreen
import com.mjd.videoforge.ui.screens.providers.ProvidersScreen
import com.mjd.videoforge.ui.screens.providers.ProviderSettingsScreen
import com.mjd.videoforge.ui.screens.settings.SettingsScreen
import com.mjd.videoforge.ui.screens.queue.QueueScreen
import com.mjd.videoforge.ui.screens.brand.BrandKitScreen
import com.mjd.videoforge.ui.screens.analytics.AnalyticsScreen

object Routes {
    const val HOME = "home"
    const val QUESTIONS = "questions/{projectId}"
    const val STORYBOARD = "storyboard/{projectId}"
    const val PIPELINE = "pipeline/{projectId}"
    const val PREVIEW = "preview/{projectId}"
    const val PROVIDERS = "providers"
    const val PROVIDER_SETTINGS = "provider_settings/{providerId}"
    const val SETTINGS = "settings"
    const val QUEUE = "queue"
    const val BRAND = "brand"
    const val ANALYTICS = "analytics"

    fun questions(projectId: String) = "questions/$projectId"
    fun storyboard(projectId: String) = "storyboard/$projectId"
    fun pipeline(projectId: String) = "pipeline/$projectId"
    fun preview(projectId: String) = "preview/$projectId"
    fun providerSettings(providerId: String) = "provider_settings/$providerId"
}

@Composable
fun VideoForgeNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToQuestions = { projectId ->
                    navController.navigate(Routes.questions(projectId))
                },
                onNavigateToQueue = {
                    navController.navigate(Routes.QUEUE)
                },
                onNavigateToProviders = {
                    navController.navigate(Routes.PROVIDERS)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToBrand = {
                    navController.navigate(Routes.BRAND)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Routes.ANALYTICS)
                }
            )
        }

        composable(Routes.QUESTIONS) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            SmartQuestionsScreen(
                projectId = projectId,
                onQuestionsComplete = {
                    navController.navigate(Routes.storyboard(projectId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STORYBOARD) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            StoryboardScreen(
                projectId = projectId,
                onStartProduction = {
                    navController.navigate(Routes.pipeline(projectId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PIPELINE) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            PipelineScreen(
                projectId = projectId,
                onProductionComplete = {
                    navController.navigate(Routes.preview(projectId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PREVIEW) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            PreviewScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onNewVideo = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PROVIDERS) {
            ProvidersScreen(
                onProviderSettings = { providerId ->
                    navController.navigate(Routes.providerSettings(providerId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROVIDER_SETTINGS) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
            ProviderSettingsScreen(
                providerId = providerId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.QUEUE) {
            QueueScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.BRAND) {
            BrandKitScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ANALYTICS) {
            AnalyticsScreen(onBack = { navController.popBackStack() })
        }
    }
}

package com.bodyscope3d.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.AppGraph

@Composable
fun AppRoot(graph: AppGraph) {
    BodyScopeTheme {
        val nav = rememberNavController()
        val explore: ExploreViewModel = viewModel()
        val draft: PainDraftViewModel = viewModel()
        val ms = if (LocalReduceMotion.current) 0 else 180
        val back: () -> Unit = { nav.popBackStack() }
        val go: (String) -> Unit = { nav.navigate(it) }
        Surface(Modifier.fillMaxSize(), color = Palette.Bg) {
            NavHost(
                nav, "splash", Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(ms)) }, exitTransition = { fadeOut(tween(ms)) },
                popEnterTransition = { fadeIn(tween(ms)) }, popExitTransition = { fadeOut(tween(ms)) },
            ) {
                composable("splash") { SplashScreen { nav.navigate("home") { popUpTo("splash") { inclusive = true } } } }
                composable("home") { HomeScreen(graph, explore, go) }
                composable("anatomy") { AnatomyScreen(graph, back) }
                composable("pain") { PainFormScreen(graph, explore, draft, go, back) { nav.navigate("history") { popUpTo("home") } } }
                composable("history") { HistoryScreen(graph, { nav.navigate("record/$it") }, back) }
                composable("record/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    RecordDetailScreen(graph, e.arguments?.getLong("id") ?: -1L, back)
                }
                composable("saved") { SavedRegionsScreen(graph, explore, back) }
                composable("settings") { SettingsScreen(graph, go, back) }
                composable("about") { AboutScreen(graph, back) }
                composable("privacy") { InfoScreen(R.string.privacy, R.string.privacy_text, back) }
                composable("help") { InfoScreen(R.string.help, R.string.help_text, back) }
            }
        }
    }
}

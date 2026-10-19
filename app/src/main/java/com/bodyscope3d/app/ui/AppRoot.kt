package com.bodyscope3d.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.AppGraph

@Composable fun isArabic(): Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(graph: AppGraph) {
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF0E5E6F))) {
        val nav = rememberNavController()
        val exploreVm: ExploreViewModel = viewModel()
        val route = nav.currentBackStackEntryAsState().value?.destination?.route
        Scaffold(topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) }, navigationIcon = {
                if (route != null && route != "home") IconButton({ nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            })
        }) { pad ->
            NavHost(nav, "home", Modifier.padding(pad)) {
                composable("home") { HomeScreen { nav.navigate(it) } }
                composable("explore") { ExploreScreen(graph, exploreVm, onDetails = { nav.navigate("detail") }, onLog = { nav.navigate("log") }) }
                composable("detail") { DetailScreen(graph, exploreVm) }
                composable("anatomy") { AnatomyScreen(graph) }
                composable("log") { PainLogScreen(graph, exploreVm.state.value.selectedId) { nav.navigate("history") { popUpTo("home") } } }
                composable("history") { HistoryScreen(graph) }
                composable("favorites") { FavoritesScreen(graph) }
                composable("settings") { SettingsScreen(graph) { nav.navigate(it) } }
                composable("privacy") { InfoScreen(R.string.privacy, R.string.privacy_text) }
                composable("help") { InfoScreen(R.string.help, R.string.help_text) }
            }
        }
    }
}

@Composable
fun EmergencyBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFDECEA)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.emergency_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.emergency_body), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun HomeScreen(go: (String) -> Unit) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EmergencyBanner()
        Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodyMedium)
        listOf("explore" to R.string.home_explore, "anatomy" to R.string.home_anatomy, "log" to R.string.home_log,
            "history" to R.string.home_history, "favorites" to R.string.home_favorites, "settings" to R.string.settings,
            "privacy" to R.string.privacy, "help" to R.string.help).forEach { (r, s) ->
            Button({ go(r) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(s)) }
        }
    }
}

@Composable
fun InfoScreen(title: Int, body: Int) = Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(body))
    EmergencyBanner()
}

package com.worddrop.app.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.worddrop.app.data.prefs.UserPreferences
import com.worddrop.app.ui.components.Tab
import com.worddrop.app.ui.detail.DetailScreen
import com.worddrop.app.ui.home.TodayScreen
import com.worddrop.app.ui.onboarding.OnboardingScreen
import com.worddrop.app.ui.quiz.QuizScreen
import com.worddrop.app.ui.saved.SavedScreen
import com.worddrop.app.ui.settings.SettingsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import javax.inject.Inject
import androidx.lifecycle.viewModelScope

@Serializable object Onboarding
@Serializable object Today
@Serializable object Saved
@Serializable object Quiz
@Serializable object Settings

/** [fromWidget] makes Back leave the app instead of popping to Today (UI/UX spec §4). */
@Serializable data class Detail(val wordId: String, val fromWidget: Boolean = false)

const val DEEP_LINK_WORD = "worddrop://word"

@HiltViewModel
class RootViewModel @Inject constructor(prefs: UserPreferences) : ViewModel() {
    /** null while preferences load — the host paints paper and waits. */
    val onboardingDone = prefs.settings.map { it.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null as Boolean?)
}

@Composable
fun WordDropNavHost(root: RootViewModel = hiltViewModel()) {
    val onboardingDone by root.onboardingDone.collectAsStateWithLifecycle()
    when (onboardingDone) {
        null -> Box(Modifier.fillMaxSize())
        true -> Graph(startAtOnboarding = false)
        false -> Graph(startAtOnboarding = true)
    }
}

@Composable
private fun Graph(startAtOnboarding: Boolean) {
    val nav = rememberNavController()
    val activity = LocalActivity.current

    NavHost(
        navController = nav,
        startDestination = if (startAtOnboarding) Onboarding else Today,
    ) {
        composable<Onboarding> {
            OnboardingScreen(onDone = {
                nav.navigate(Today) { popUpTo<Onboarding> { inclusive = true } }
            })
        }
        composable<Today> {
            TodayScreen(
                onOpenWord = { id -> nav.navigate(Detail(id)) },
                onOpenSettings = { nav.navigate(Settings) },
                onTab = { nav.switchTab(it) },
            )
        }
        composable<Saved> {
            SavedScreen(onOpenWord = { id -> nav.navigate(Detail(id)) }, onTab = { nav.switchTab(it) })
        }
        composable<Quiz> {
            QuizScreen(onOpenWord = { id -> nav.navigate(Detail(id)) }, onTab = { nav.switchTab(it) })
        }
        composable<Settings> {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable<Detail>(
            deepLinks = listOf(navDeepLink<Detail>(basePath = DEEP_LINK_WORD)),
        ) { entry ->
            val route = entry.toRoute<Detail>()
            DetailScreen(
                wordId = route.wordId,
                onBack = {
                    if (route.fromWidget) activity?.finish() else if (!nav.popBackStack()) activity?.finish()
                },
                onOpenWord = { id -> nav.navigate(Detail(id)) },
            )
        }
    }
}

private fun NavHostController.switchTab(tab: Tab) {
    val route: Any = when (tab) {
        Tab.TODAY -> Today
        Tab.SAVED -> Saved
        Tab.QUIZ -> Quiz
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

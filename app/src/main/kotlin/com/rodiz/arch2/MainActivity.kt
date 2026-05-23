package com.rodiz.arch2

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.chat.nav.ChatRoute
import com.rodiz.arch2.feature.deck.nav.DeckHome
import com.rodiz.arch2.feature.likes.nav.LikesHome
import com.rodiz.arch2.feature.match.nav.MatchesHome
import com.rodiz.arch2.feature.notifications.nav.NotificationRationale
import com.rodiz.arch2.feature.profile.nav.ProfileHome
import com.rodiz.arch2.ui.BottomNavViewModel
import com.rodiz.arch2.ui.FloatingChipNavBar
import com.rodiz.arch2.ui.NavBarItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TinPetTheme {
                val current by remember(navigator) {
                    derivedStateOf { navigator.backStack.lastOrNull() }
                }
                val showBottomBar = TOP_LEVEL_ROUTES.any { it == current }

                val bottomNavViewModel: BottomNavViewModel = hiltViewModel()
                val badges by bottomNavViewModel.badges.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0),
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        ) {
                            FloatingChipNavBar(
                                items = navBarItems(badges.likes, badges.matches),
                                selectedRoute = current,
                                onSelected = { route -> navigator.replaceAll(route) },
                            )
                        }
                    },
                ) { innerPadding ->
                    NavDisplay(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        backStack = navigator.backStack,
                        onBack = { navigator.goBack() },
                        entryProvider = entryProvider {
                            entryProviderInstallers.forEach { install -> install() }
                        },
                    )
                }
            }
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "tinpet") return
        val destination: Any? = when (uri.host) {
            "chat" -> uri.lastPathSegment?.let { ChatRoute(it) }
            "deck" -> DeckHome
            "likes" -> LikesHome
            "matches" -> MatchesHome
            "profile" -> ProfileHome
            "notify" -> NotificationRationale
            else -> null
        }
        destination?.let { navigator.replaceAll(it) }
    }
}

private val TOP_LEVEL_ROUTES: List<Any> = listOf(DeckHome, LikesHome, MatchesHome, ProfileHome)

private fun navBarItems(likes: Int, matches: Int): List<NavBarItem> = listOf(
    NavBarItem(DeckHome, "Deck", Icons.Outlined.Pets, Icons.Filled.Pets),
    NavBarItem(LikesHome, "Likes", Icons.Outlined.Favorite, Icons.Filled.Favorite, badgeCount = likes),
    NavBarItem(MatchesHome, "Matches", Icons.Outlined.Bolt, Icons.Filled.Bolt, badgeCount = matches),
    NavBarItem(ProfileHome, "Profile", Icons.Outlined.Person, Icons.Filled.Person),
)

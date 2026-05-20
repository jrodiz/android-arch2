package com.rodiz.arch2

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
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
                val showBottomBar = BOTTOM_TABS.any { it.route == current }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            DashboardBottomBar(
                                current = current,
                                onTabSelected = { tabRoute ->
                                    if (current != tabRoute) navigator.replaceAll(tabRoute)
                                },
                            )
                        }
                    },
                    contentWindowInsets = WindowInsets(0),
                ) { innerPadding ->
                    NavDisplay(
                        modifier = Modifier.padding(innerPadding),
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

private data class BottomTab(
    val route: Any,
    val label: String,
    val icon: ImageVector,
)

private val BOTTOM_TABS = listOf(
    BottomTab(DeckHome, "Deck", Icons.Outlined.Pets),
    BottomTab(LikesHome, "Likes you", Icons.Outlined.Favorite),
    BottomTab(MatchesHome, "Matches", Icons.Outlined.Bolt),
    BottomTab(ProfileHome, "Profile", Icons.Outlined.Person),
)

@Composable
private fun DashboardBottomBar(
    current: Any?,
    onTabSelected: (Any) -> Unit,
) {
    NavigationBar {
        BOTTOM_TABS.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { onTabSelected(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

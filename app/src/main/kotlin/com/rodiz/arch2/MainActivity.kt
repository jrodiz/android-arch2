package com.rodiz.arch2

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.discover.nav.DiscoverHome
import com.rodiz.arch2.feature.home.nav.HomeHome
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
    }
}

private data class BottomTab(
    val route: Any,
    val label: String,
    val icon: ImageVector,
)

private val BOTTOM_TABS = listOf(
    BottomTab(HomeHome, "Home", Icons.Outlined.Home),
    BottomTab(DiscoverHome, "Discover", Icons.Outlined.Explore),
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

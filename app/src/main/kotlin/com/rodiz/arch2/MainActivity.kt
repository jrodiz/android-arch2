package com.rodiz.arch2

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.rodiz.arch2.core.designsystem.theme.Arch2Theme
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
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
            Arch2Theme {
                Scaffold { padding ->
                    NavDisplay(
                        backStack = navigator.backStack,
                        onBack = { navigator.goBack() },
                        modifier = Modifier.padding(padding),
                        entryProvider = entryProvider {
                            entryProviderInstallers.forEach { install -> install() }
                        },
                    )
                }
            }
        }
    }
}

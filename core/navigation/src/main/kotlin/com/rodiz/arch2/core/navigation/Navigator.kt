package com.rodiz.arch2.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.EntryProviderBuilder
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import javax.inject.Qualifier

typealias EntryProviderInstaller = EntryProviderBuilder<Any>.() -> Unit

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class StartDestination

@ActivityRetainedScoped
class Navigator @Inject constructor(
    @StartDestination start: Any,
) {
    val backStack: SnapshotStateList<Any> = mutableStateListOf(start)

    fun goTo(destination: Any) {
        backStack.add(destination)
    }

    fun goBack() {
        backStack.removeLastOrNull()
    }

    fun replaceAll(destination: Any) {
        backStack.clear()
        backStack.add(destination)
    }
}

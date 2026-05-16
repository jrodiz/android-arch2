# Home Bottom Navigation (Home / Discover / Profile)

## Context

The post-login surface is currently a single `HomeRoute` with a welcome message and a sign-out button. We want to turn it into a tabbed dashboard with a Material 3 `NavigationBar` exposing three peer surfaces: **Home**, **Discover**, and **Profile**. This is the first step toward a multi-surface app (discovery features land next, account/settings move out of Home).

We are creating **dedicated feature modules for Discover and Profile up front** rather than inlining tab content in `:feature:home:presentation`. This respects the scaffold's architectural invariant ("a `:presentation` module may depend on another feature's `:nav` only — never another feature's `:presentation`") and avoids a costly refactor later. Sign-out moves from Home to Profile, which is where users will look for it.

The bottom nav itself is hosted at the **app level** (in `MainActivity`'s `Scaffold`), driven by the global `Navigator.backStack`. Each tab is its own destination; switching tabs calls `navigator.replaceAll(tabRoute)` so the back stack always has exactly one root when on the dashboard. This keeps each feature independent (no cross-feature `:presentation` deps) and matches typical Material 3 bottom-nav behavior (system-back exits the app from any tab root).

Canonical scaffold spec: `ANDROID_APP_SCAFFOLD_PROMPT.md`.

## Approach

### 1. New feature modules

Create four new modules following the existing `:feature:home` shape.

**`:feature:discover:nav`** (pure JVM)
- `build.gradle.kts`: `arch.jvm.library` + `arch.kotlin.serialization` (mirror `feature/home/nav/build.gradle.kts`).
- `Routes.kt`:
  ```kotlin
  @Serializable data object DiscoverHome
  ```

**`:feature:discover:presentation`** (Android feature)
- `build.gradle.kts`: `arch.android.feature`; deps on `:feature:discover:nav`, `:core:designsystem`, `:core:navigation`.
- `DiscoverScreen.kt`: public `DiscoverRoute(viewModel: DiscoverViewModel = hiltViewModel())` rendering a placeholder Material 3 surface (`Icons.Outlined.Explore` header + "Discover content coming soon").
- `DiscoverViewModel.kt`: empty `@HiltViewModel` placeholder for parity with `HomeViewModel`.
- `DiscoverNavModule.kt`: Hilt `@Module @InstallIn(ActivityRetainedComponent::class)` providing `@IntoSet EntryProviderInstaller` that registers `entry<DiscoverHome> { DiscoverRoute() }`.

**`:feature:profile:nav`** (pure JVM)
- `Routes.kt`:
  ```kotlin
  @Serializable data object ProfileHome
  ```

**`:feature:profile:presentation`** (Android feature)
- `build.gradle.kts`: `arch.android.feature`; deps on `:feature:profile:nav`, `:feature:login:nav` (so profile can call `navigator.replaceAll(LoginHome)` on sign-out), `:core:designsystem`, `:core:navigation`, `:core:session:domain` (sign-out use case).
- `ProfileScreen.kt`: public `ProfileRoute(onSignedOut: () -> Unit, viewModel: ProfileViewModel = hiltViewModel())` with a sign-out button. Move the sign-out logic that currently lives in `HomeViewModel` here.
- `ProfileViewModel.kt`: takes the sign-out use case (whatever `HomeViewModel` currently injects from `:core:session:domain`).
- `ProfileNavModule.kt`: `entry<ProfileHome> { ProfileRoute(onSignedOut = { navigator.replaceAll(LoginHome) }) }`.

### 2. Register modules

Add to `settings.gradle.kts`:
```kotlin
include(":feature:discover:nav")
include(":feature:discover:presentation")
include(":feature:profile:nav")
include(":feature:profile:presentation")
```

Add to `app/build.gradle.kts` dependencies:
```kotlin
implementation(projects.feature.discover.presentation)
implementation(projects.feature.profile.presentation)
```
(`:feature:home:presentation` is already included.)

### 3. Slim `:feature:home:presentation`

In `feature/home/presentation/src/main/kotlin/HomeScreen.kt`:
- Drop the `onSignedOut` parameter from `HomeRoute`.
- Remove the sign-out button. The Home tab becomes the "welcome / overview" surface (placeholder content is fine for this PR — a greeting + space for future widgets).

In `HomeViewModel.kt`:
- Remove the sign-out use case injection and any related state. (Moved to `ProfileViewModel`.)

In `HomeNavModule.kt`:
- Update the entry: `entry<HomeHome> { HomeRoute() }` (no callback).

### 4. Bottom navigation in `MainActivity`

Edit `app/src/main/kotlin/MainActivity.kt`:

- Define a small sealed list of tabs at the app level (no new module needed — this is app-shell wiring):
  ```kotlin
  private data class BottomTab(
      val route: Any,
      val label: String,
      val icon: ImageVector,
  )

  private val bottomTabs = listOf(
      BottomTab(HomeHome,     "Home",     Icons.Outlined.Home),
      BottomTab(DiscoverHome, "Discover", Icons.Outlined.Explore),
      BottomTab(ProfileHome,  "Profile",  Icons.Outlined.Person),
  )
  ```
- Wrap the existing `NavDisplay` in a `Scaffold`:
  ```kotlin
  val top = navigator.backStack.lastOrNull()
  val showBottomBar = bottomTabs.any { it.route == top }
  Scaffold(
      bottomBar = {
          if (showBottomBar) {
              NavigationBar {
                  bottomTabs.forEach { tab ->
                      NavigationBarItem(
                          selected = top == tab.route,
                          onClick = { if (top != tab.route) navigator.replaceAll(tab.route) },
                          icon = { Icon(tab.icon, contentDescription = tab.label) },
                          label = { Text(tab.label) },
                      )
                  }
              }
          }
      },
  ) { innerPadding ->
      NavDisplay(
          modifier = Modifier.padding(innerPadding),
          backStack = navigator.backStack,
          onBack = { navigator.goBack() },
          entryProvider = entryProvider { entryProviderInstallers.forEach { it() } },
      )
  }
  ```
- Imports: `androidx.compose.material3.{Scaffold, NavigationBar, NavigationBarItem, Icon, Text}`, `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.outlined.{Home, Explore, Person}`, and the three route data objects.

The bottom bar only renders when the current destination is a tab root, so login and any future detail screens still get full-bleed layout. `replaceAll` keeps the back stack to a single root while on the dashboard — system back from any tab exits the app, which is the standard Material 3 bottom-nav behavior.

### 5. Login → Dashboard entry point (verify, no change expected)

`StartDestinationModule.provideStartDestination` already resolves to `HomeHome` when a session exists, and `LoginRoute`'s success path already calls `navigator.replaceAll(HomeHome)`. Both continue to land on the Home tab automatically — no changes required.

## Critical files

Created:
- `feature/discover/nav/build.gradle.kts`
- `feature/discover/nav/src/main/kotlin/Routes.kt`
- `feature/discover/presentation/build.gradle.kts`
- `feature/discover/presentation/src/main/kotlin/{DiscoverScreen,DiscoverViewModel,DiscoverNavModule}.kt`
- `feature/profile/nav/build.gradle.kts`
- `feature/profile/nav/src/main/kotlin/Routes.kt`
- `feature/profile/presentation/build.gradle.kts`
- `feature/profile/presentation/src/main/kotlin/{ProfileScreen,ProfileViewModel,ProfileNavModule}.kt`

Modified:
- `settings.gradle.kts` — include 4 new modules.
- `app/build.gradle.kts` — add discover/profile presentation deps.
- `app/src/main/kotlin/MainActivity.kt` — Scaffold + NavigationBar wrapping NavDisplay.
- `feature/home/presentation/src/main/kotlin/HomeScreen.kt` — drop `onSignedOut`, slim content.
- `feature/home/presentation/src/main/kotlin/HomeViewModel.kt` — drop sign-out wiring.
- `feature/home/presentation/src/main/kotlin/HomeNavModule.kt` — drop callback from `entry<HomeHome>`.

## Verification

```bash
# Builds against the documented toolchain.
JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug

# JVM isolation for both new :nav modules — must NOT pull in androidx.*, Compose, Hilt.
./gradlew :feature:discover:nav:dependencies --configuration runtimeClasspath
./gradlew :feature:profile:nav:dependencies  --configuration runtimeClasspath

# Unit tests (Home/Profile VM behavior, if any).
JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew test
```

End-to-end on an emulator:
1. Install: `./gradlew :app:installDebug`
2. Launch the app on the connected emulator.
3. Log in with the debug pre-fill credentials.
4. Verify the bottom navigation bar appears with three tabs (Home, Discover, Profile) and correct icons.
5. Tap each tab; confirm the content swaps and the selected indicator follows.
6. From the Profile tab, tap Sign Out → confirm we land back on the login screen and the bottom bar disappears.
7. Capture a screenshot of the dashboard with the bottom nav visible and surface its path.

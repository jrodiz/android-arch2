# Plan: Reshape bottom navigation to the 4-tab TinPet shell

> **Parent spec:** [`plans/tinpet-app.md`](./tinpet-app.md). **Architecture rules:** [`plans/ANDROID_APP_SCAFFOLD_PROMPT.md`](./ANDROID_APP_SCAFFOLD_PROMPT.md).
>
> **Position in the implementation order:** comes **after** [`plans/pet-profile.md`](./pet-profile.md) and [`plans/deck-swipe.md`](./deck-swipe.md). By the time this plan is implemented, `:feature:pet` and `:feature:deck` already exist and are fully wired against Firebase. This PR rebuilds the app shell around them.
>
> **Backend:** none added or changed in this PR. This is pure app-shell + module structure work.
>
> **Revision note (2026-05-19):** an earlier draft used 5 tabs (Deck / Likes you / Matches / Chats / Profile). The Match-and-Chat design (see [`plans/match-and-chat.md`](./match-and-chat.md)) collapses Matches and Chats into a single combined inbox tab, so this plan now describes the 4-tab layout. `:feature:chat` is **no longer created in this PR** — it's created alongside `:feature:match` in the match-and-chat plan.

## 1. Context

The project currently ships a placeholder bottom navigation with three tabs (Home / Discover / Profile), where Home renders a Facebook-style social feed scaffolded for the project's early shape. See [`plans/home-bottom-navigation.md`](./home-bottom-navigation.md) and [`plans/home-feed.md`](./home-feed.md) for the historical state.

That shape no longer fits the product. The TinPet shell needs **four tabs** ([`plans/tinpet-app.md`](./tinpet-app.md) §4.7):

| Order | Tab | Module that owns it |
|---|---|---|
| 1 | **Deck** | `:feature:deck` (built — see deck-swipe.md) |
| 2 | **Likes you** | `:feature:likes` (new skeleton — empty-state only in this PR) |
| 3 | **Matches** | `:feature:match` (new skeleton — empty-state only in this PR; combined inbox built later) |
| 4 | **Profile** | `:feature:profile` (exists; keep mostly as-is) |

This PR is purely app-shell restructuring. The full implementations of Likes / Match (and Chat, which is reached from the Matches tab) arrive in their own plans.

## 2. Goal

Replace the current 3-tab bottom nav with the 4-tab TinPet shell, **without** building out the new features beyond a forward-compatible empty-state placeholder. After this PR:

- The user signs in → lands on the **Deck** tab (which is fully functional).
- All four tabs are visible in the `NavigationBar` with the chosen icons and labels.
- Tapping Likes you / Matches shows the empty-state mockup that the real feature will fall back to when there's no data — so the UI looks finished and credible even before backend wiring lands.
- The Facebook-style feed and the Discover placeholder are gone from the codebase.

## 3. Scope summary

### Delete

- `feature/home/` — entire directory tree (nav, data, domain, presentation). The Facebook feed, fake repository, and all its models go with it.
- `feature/discover/` — entire directory tree (nav, presentation).
- Their `include(...)` entries in `settings.gradle.kts`.
- Their dependencies in `app/build.gradle.kts`.
- Imports of `HomeHome` and `DiscoverHome` route keys throughout the app.

### Create

- `:feature:likes:nav` + `:feature:likes:presentation`
- `:feature:match:nav` + `:feature:match:presentation`

Each new feature in this PR is **`:nav` + `:presentation` only** (no `:domain`, no `:data` — those are added by the feature's own implementation plan). The `:presentation` module renders the forward-compatible empty state.

> `:feature:chat` is **not** created in this PR. It's created alongside the match implementation in [`plans/match-and-chat.md`](./match-and-chat.md), where the chat detail screen is reachable from the Matches tab (not a tab of its own).

### Modify

- `app/src/main/kotlin/.../MainActivity.kt` — reshape the `bottomTabs` list to the four tabs; update icons.
- `app/src/main/kotlin/.../di/StartDestinationModule.kt` — when a session exists, land on `DeckHome` instead of `HomeHome`.
- `feature/login/presentation/.../LoginRoute.kt` — successful login calls `navigator.replaceAll(DeckHome)` instead of `navigator.replaceAll(HomeHome)`.
- `feature/profile/presentation/` — no change in this PR (still just sign-out). Full Profile build-out is `plans/owner-profile-settings.md`.

### Keep

- `:feature:deck:*` — already implemented; this PR just wires its `DeckHome` route into the new bottom nav.
- `:feature:pet:*` — used indirectly (deck depends on it); no change.
- `:feature:profile:*` — sign-out flow stays as-is.
- `:feature:login:*` — unchanged except for the single `replaceAll` target.
- `:core:*` — unchanged.
- Plan files `home-bottom-navigation.md` and `home-feed.md` — retained in `plans/` for historical context (per [`plans/rebrand-tinpet.md`](./rebrand-tinpet.md) guidance on historical plans).

## 4. New module skeletons

Both new features share the same shape, with different route names and empty-state content. Package paths stay `com.rodiz.arch2.feature.<name>.*` (the project does not rename packages — see `plans/rebrand-tinpet.md` §2).

### 4.1 `:feature:likes`

**`feature/likes/nav/build.gradle.kts`** — mirrors `feature/profile/nav/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.tinpet.jvm.library)
    alias(libs.plugins.tinpet.kotlin.serialization)
}
```

**`feature/likes/nav/src/main/kotlin/com/rodiz/arch2/feature/likes/nav/Routes.kt`:**

```kotlin
@Serializable data object LikesHome
```

**`feature/likes/presentation/build.gradle.kts`** — mirrors `feature/profile/presentation/build.gradle.kts`:

```kotlin
plugins { alias(libs.plugins.tinpet.android.feature) }

dependencies {
    implementation(projects.feature.likes.nav)
    implementation(projects.feature.deck.nav)        // for the "Go to Deck" CTA
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
}
```

**`feature/likes/presentation/src/main/kotlin/com/rodiz/arch2/feature/likes/presentation/LikesScreen.kt`** — empty-state composable (see §5).

**`feature/likes/presentation/src/main/kotlin/com/rodiz/arch2/feature/likes/presentation/LikesNavModule.kt`:**

```kotlin
@Module
@InstallIn(ActivityRetainedComponent::class)
object LikesNavModule {
    @IntoSet @Provides
    fun provideLikesEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<LikesHome> { LikesScreen(onGoToDeck = { navigator.replaceAll(DeckHome) }) }
    }
}
```

### 4.2 `:feature:match`

Same shape. Route: `@Serializable data object MatchesHome`. Empty-state CTA "Go to Deck".

(The `MatchDetail(matchId)` route key is **not** added in this PR. It lands with `plans/match-and-chat.md`.)

## 5. Empty-state mockups

A shared composable in `:core:designsystem` makes the two empty states consistent:

```kotlin
@Composable
fun EmptyTabState(
    icon: ImageVector,
    headline: String,
    body: String,
    cta: String? = null,
    onCta: (() -> Unit)? = null,
)
```

Layout: centered column, large outlined icon (96.dp), headline (`MaterialTheme.typography.headlineSmall`), body (`bodyMedium`, `onSurfaceVariant`), optional primary button.

Per-tab content for this PR:

| Tab | Icon | Headline | Body | CTA |
|---|---|---|---|---|
| Likes you | `Icons.Outlined.Favorite` | "No one yet" | "When someone likes one of your pets, they'll appear here. Keep swiping in the deck." | "Go to Deck" → `navigator.replaceAll(DeckHome)` |
| Matches | `Icons.Outlined.Bolt` | "No matches yet" | "When you and another owner both like each other's pets, your match shows up here." | "Go to Deck" |

Each `LikesScreen` / `MatchesScreen` is just a `Surface { EmptyTabState(...) }` in this PR. When the real feature lands, the screen wraps this in a `when` over its UI state: empty → `EmptyTabState(...)`, populated → the actual list.

## 6. MainActivity rewire

Edit `app/src/main/kotlin/com/rodiz/arch2/MainActivity.kt`. The existing `bottomTabs` list becomes:

```kotlin
private val bottomTabs = listOf(
    BottomTab(DeckHome,    "Deck",      Icons.Outlined.Pets),
    BottomTab(LikesHome,   "Likes you", Icons.Outlined.Favorite),
    BottomTab(MatchesHome, "Matches",   Icons.Outlined.Bolt),
    BottomTab(ProfileHome, "Profile",   Icons.Outlined.Person),
)
```

Imports to add:

```kotlin
import com.rodiz.arch2.feature.deck.nav.DeckHome
import com.rodiz.arch2.feature.likes.nav.LikesHome
import com.rodiz.arch2.feature.match.nav.MatchesHome
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Bolt
```

Imports to remove:

```kotlin
import com.rodiz.arch2.feature.home.nav.HomeHome
import com.rodiz.arch2.feature.discover.nav.DiscoverHome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Explore
```

The `BottomTab` data class, the `Scaffold` + `NavigationBar` wrapping, and the `showBottomBar = bottomTabs.any { it.route == top }` gate are unchanged — that mechanism is correct, it just gets a different tab list.

**Tab order** is the declaration order in `bottomTabs`. The four-tab layout works fine on standard phone widths (Material 3 `NavigationBar` is designed for 3–5 destinations and is most balanced at 4).

**No badges in this PR** — per design decision. Each future feature implementation will add its own `Badge` slot via a small extension to `BottomTab` (e.g., a `badgeFlow: Flow<Int>?` field) when that feature has count data to wire.

## 7. Login → Dashboard entry point

Two small edits route the post-login destination to Deck:

**`app/src/main/kotlin/com/rodiz/arch2/di/StartDestinationModule.kt`** (or wherever `provideStartDestination` currently lives):

```kotlin
// Replace:
return if (hasSession) HomeHome else LoginHome
// With:
return if (hasSession) DeckHome else LoginHome
```

**`feature/login/presentation/src/main/kotlin/.../LoginRoute.kt`** — wherever the success branch lives:

```kotlin
// Replace:
navigator.replaceAll(HomeHome)
// With:
navigator.replaceAll(DeckHome)
```

`:app` and `:feature:login:presentation` both gain a dependency on `:feature:deck:nav` (the deck route key). `:feature:login:presentation` drops its dependency on `:feature:home:nav`.

The `runBlocking { ... }` in `StartDestinationModule.provideStartDestination` (the single intentional one called out in [`CLAUDE.md`](../CLAUDE.md)) is preserved — only the returned route changes.

## 8. Module structure summary (after this PR)

```
:feature
  :feature:login                  // unchanged (auth + Google sign-in)
  :feature:pet                    // unchanged (pet profiles)
  :feature:deck                   // unchanged (the swipe deck — fully implemented)
  :feature:likes                  // NEW — nav + presentation skeleton
    :feature:likes:nav
    :feature:likes:presentation
  :feature:match                  // NEW — nav + presentation skeleton (full impl in match-and-chat.md)
    :feature:match:nav
    :feature:match:presentation
  :feature:profile                // unchanged (sign-out only for now)

  // GONE:
  // :feature:home                — deleted
  // :feature:discover            — deleted

  // Not yet created (lands with match-and-chat.md):
  // :feature:chat
```

`settings.gradle.kts` changes:

```kotlin
// remove:
include(":feature:home:nav")
include(":feature:home:data")
include(":feature:home:domain")
include(":feature:home:presentation")
include(":feature:discover:nav")
include(":feature:discover:presentation")

// add:
include(":feature:likes:nav")
include(":feature:likes:presentation")
include(":feature:match:nav")
include(":feature:match:presentation")
```

`app/build.gradle.kts` dependency changes:

```kotlin
// remove:
implementation(projects.feature.home.presentation)
implementation(projects.feature.discover.presentation)

// add (if not already present from the deck PR):
implementation(projects.feature.deck.presentation)
implementation(projects.feature.deck.nav)        // for BottomTab + StartDestination route key
implementation(projects.feature.likes.presentation)
implementation(projects.feature.likes.nav)
implementation(projects.feature.match.presentation)
implementation(projects.feature.match.nav)
```

## 9. Out of scope (deferred)

- Tab badges / unread counts — each future feature plan adds its own count when its data layer lands.
- The full Likes feature (the "Likes you" list with full reveal) — `plans/likes-you.md`.
- The full Match feature (combined inbox, match detail) and the Chat feature (1:1 text chat) — both in `plans/match-and-chat.md`.
- Profile expansion (My Pets section, Settings entry, delete account) — `plans/owner-profile-settings.md`.
- Predictive back / motion polish on tab switches.
- Animated transitions between tabs (default fade is fine for v1).
- Tablet / large-screen `NavigationRail` adaptation.

## 10. Verification

1. Build:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug
   ```
2. JVM isolation for the two new `:nav` modules — none should pull in `androidx.*`, Compose, Room, Retrofit, Hilt, or Firebase SDKs:
   ```bash
   ./gradlew :feature:likes:nav:dependencies --configuration runtimeClasspath
   ./gradlew :feature:match:nav:dependencies --configuration runtimeClasspath
   ```
3. Confirm zero lingering references to the deleted modules:
   ```bash
   grep -rn 'feature\.home\|feature\.discover\|HomeHome\|DiscoverHome' \
     --include='*.kt' --include='*.kts' --include='*.toml' . | grep -v '/build/' | grep -v '/plans/'
   ```
   Expected: empty.
4. Unit tests:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew test
   ```
5. Manual on emulator (install + launch per the project's standard verification):
   - Fresh install → sign in → lands on the **Deck** tab (not Home).
   - Bottom nav shows four icons in order: Deck (paw), Likes you (heart), Matches (bolt), Profile (person).
   - Tap Likes you → see the empty-state mockup with heart icon, "No one yet" headline, and "Go to Deck" CTA. Tap the CTA → returns to Deck tab.
   - Tap Matches → empty state with bolt icon. CTA works.
   - Tap Profile → existing sign-out screen. Sign out → returns to Login; bottom bar hidden.
   - Sign back in → lands on Deck again (StartDestination resolves correctly).
   - Capture a screenshot of the dashboard with the four-tab bottom nav and surface its path.

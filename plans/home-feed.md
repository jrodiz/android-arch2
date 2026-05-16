# Prompt: Implement a Facebook-Style Home Feed on the Home Tab

You are implementing a new feature in the **Arch2.0 / TinPet** Android project. Follow this spec exactly. Read `CLAUDE.md` and `ANDROID_APP_SCAFFOLD_PROMPT.md` at the repo root before you start — every architecture invariant they describe applies here.

This is a **spec-driven design**: build to the spec, not your assumptions. If something below is ambiguous, ask before improvising.

---

## 1. Goal

Replace the placeholder `HomeRoute` in `feature/home/presentation` with a **Facebook-style social feed** — a vertically scrolling list of posts created by other users. The feed is the primary surface of the Home tab in the bottom navigation.

For this iteration, **all data is fake (in-memory).** Do not add Firebase, Retrofit, Room, or any network/persistence code. The data layer must be structured so that a real backend can be swapped in later without changing the presentation layer.

---

## 2. User Stories

1. As a user, when I open the app and land on the Home tab, I see a scrollable feed of posts from other users, ordered newest-first.
2. As a user, I see each post's author (avatar + display name), how long ago it was posted, the text content, and any attached image.
3. As a user, I see how many likes, comments, and shares each post has.
4. As a user, I can tap **Like** to toggle my like on a post; the like count updates immediately and the icon reflects my state.
5. As a user, I can pull down to refresh the feed; new fake posts may appear at the top.
6. As a user, I see a **"Create Post"** composer pinned at the top of the feed (tappable, but tapping it for now just shows a `Snackbar` — no real composer screen yet).
7. As a user, if the feed is loading for the first time, I see a loading state. If it fails (simulate a 10% random failure on refresh), I see an error state with a Retry button. If it is empty, I see an empty state.

---

## 3. Domain Model

Define these as **immutable Kotlin data classes** in `feature/home/domain` (pure JVM module — no Android dependencies).

```kotlin
data class Post(
    val id: PostId,
    val author: Author,
    val createdAt: Instant,        // kotlinx.datetime.Instant
    val text: String?,             // null if image-only
    val imageUrl: String?,         // null if text-only; at least one of text/imageUrl must be non-null
    val reactions: Reactions,
    val commentCount: Int,
    val shareCount: Int,
    val viewerHasLiked: Boolean,
)

@JvmInline value class PostId(val value: String)

data class Author(
    val id: AuthorId,
    val displayName: String,
    val avatarUrl: String?,        // null → render initials placeholder
)

@JvmInline value class AuthorId(val value: String)

data class Reactions(
    val likeCount: Int,
    val loveCount: Int,
    val hahaCount: Int,
    val wowCount: Int,
    val sadCount: Int,
    val angryCount: Int,
) {
    val total: Int get() = likeCount + loveCount + hahaCount + wowCount + sadCount + angryCount
}
```

Notes:
- Reactions mirror Facebook's six reaction types so the model is forward-compatible, but the UI only needs to surface **Like** for this iteration.
- `imageUrl` is a URL string; the UI renders it with Coil. Use stable placeholder URLs from `https://picsum.photos/seed/<seed>/800/600` so images load deterministically.

---

## 4. Data Layer (Fake)

In `feature/home/data` (Android module — fine to depend on `kotlinx.coroutines`):

- Define `interface PostRepository` in `:domain` with:
  - `fun observeFeed(): Flow<List<Post>>`
  - `suspend fun refresh()`  — throws on simulated failure
  - `suspend fun toggleLike(postId: PostId)`
- Provide `FakePostRepository : PostRepository` in `:data`:
  - Seeds with **~15 hand-written posts** mixing text-only, image-only, and text+image. Use 5–6 distinct fake authors so the feed feels populated. Write the seed list in `FakeFeedSeed.kt` as a top-level `val` so it is easy to edit.
  - `refresh()` delays 600–1200 ms (use `kotlinx.coroutines.delay`), then randomly throws `IOException("Simulated network failure")` with `Random.nextFloat() < 0.1f`. On success, prepends 1–3 new fake posts with `createdAt = Clock.System.now()`.
  - `toggleLike()` mutates the in-memory list and re-emits via a `MutableStateFlow<List<Post>>`.
  - Singleton-scoped via Hilt.

---

## 5. Presentation Layer

In `feature/home/presentation`:

### 5.1 `HomeViewModel`
- Injects `PostRepository`.
- Exposes `val uiState: StateFlow<HomeUiState>` where:
  ```kotlin
  sealed interface HomeUiState {
      data object Loading : HomeUiState
      data class Content(
          val posts: List<PostUiModel>,
          val isRefreshing: Boolean,
      ) : HomeUiState
      data object Empty : HomeUiState
      data class Error(val message: String) : HomeUiState
  }
  ```
- `PostUiModel` is a presentation-only mapping of `Post` that pre-formats `relativeTime` (e.g. `"3m"`, `"2h"`, `"Yesterday"`, `"Mar 14"`) and `formattedLikeCount` (e.g. `"1.2K"`).
- Actions: `fun refresh()`, `fun onLikeClicked(postId: PostId)`.

### 5.2 `HomeRoute` + `HomeScreen`
- Keep the `HomeRoute(viewModel: HomeViewModel = hiltViewModel())` entry point — `HomeRoute` only collects state and forwards callbacks to a **stateless** `HomeScreen(state, onRefresh, onLikeClicked, onComposerClicked)`.
- Layout (top to bottom):
  1. **TopAppBar** — title `"TinPet"` left-aligned in brand coral, with a trailing `IconButton` showing `Icons.Outlined.Notifications` (no-op for now).
  2. **Pull-to-refresh container** wrapping a `LazyColumn`.
  3. **First item:** `CreatePostComposerCard` — a `Card` with the viewer's avatar (use a hardcoded fake "You" author for now) and the placeholder text `"What's on your mind?"`. Tappable → triggers `onComposerClicked` (host shows a `Snackbar`).
  4. **Remaining items:** `PostCard` for each post.
- Empty / Loading / Error states replace the LazyColumn body, not the TopAppBar.

### 5.3 `PostCard` composable
A `Card` with `MaterialTheme.shapes.large`, `Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)`. Internal layout:

```
┌─────────────────────────────────────────────┐
│ [avatar 40dp]  Display Name                 │
│                3h · 🌐                       │
│                                             │
│ Post text wraps here, up to 6 lines then    │
│ truncates with "… See more".                │
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │           [image, 16:9, full width]     │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ 👍 1.2K                  23 comments · 5 shares
│ ───────────────────────────────────────────  │
│   👍 Like    💬 Comment    ↗ Share          │
└─────────────────────────────────────────────┘
```

- Avatar: `AsyncImage` (Coil) clipped to a circle; if `avatarUrl == null`, render a coral circle with the author's first initial.
- Image: `AsyncImage` with `ContentScale.Crop`, fixed 16:9 aspect ratio, `MaterialTheme.shapes.medium` corners.
- Action row: three equal-weight `TextButton`s with leading icons. The **Like** button tint and icon swap between outlined/filled based on `viewerHasLiked`; tapping calls `onLikeClicked(post.id)`.
- The **Comment** and **Share** buttons are present but no-op for now (do not gate on a feature flag — just leave the lambda empty at the call site).

### 5.4 Theming
- Use the existing `TinPetTheme` and Material 3 components. Do not introduce a new color palette. The Like state uses `MaterialTheme.colorScheme.primary` when active.

---

## 6. Module Layout

Add these modules (or confirm they already exist) and register them in `settings.gradle.kts`:

```
feature/home/
├── nav/            (already exists — JVM, no changes needed)
├── domain/         (NEW — pure JVM: arch.jvm.library)
├── data/           (NEW — Android library: arch.android.library + Hilt)
└── presentation/   (exists — extend it)
```

Dependency edges:
- `:presentation` → `:domain`, `:nav`
- `:data` → `:domain`
- `:app` → `:data` (for Hilt binding aggregation)
- `:domain` depends only on `kotlinx-coroutines-core` and `kotlinx-datetime`. **No `androidx.*`.** Verify with `./gradlew :feature:home:domain:dependencies --configuration runtimeClasspath`.

Hilt binding: in `:data`, `@Module @InstallIn(SingletonComponent::class) abstract class PostDataModule { @Binds @Singleton abstract fun bindPostRepository(impl: FakePostRepository): PostRepository }`.

---

## 7. Dependencies to Add

In the version catalog (`gradle/libs.versions.toml`):
- `kotlinx-datetime` (latest stable) — for `Instant` / `Clock`.
- `coil-compose` (version 2.7.x) — for `AsyncImage`.
- `androidx-compose-material-pullrefresh` — if not already pulled in by the Material 3 BOM, add the appropriate artifact.

Wire these only into the modules that need them. `:domain` only adds `kotlinx-datetime`.

---

## 8. Acceptance Criteria

The feature is done when **all** of the following are true:

1. `./gradlew assembleDebug` succeeds with `JAVA_HOME` pointing at the JDK 17 in `CLAUDE.md`.
2. `./gradlew :feature:home:domain:dependencies --configuration runtimeClasspath` shows **no `androidx.*`, no Hilt, no Compose, no Retrofit, no Room** transitive deps.
3. Launching the app and tapping the Home tab shows the seeded feed within ~1 second, with avatars and at least one image-bearing post rendered correctly.
4. Tapping **Like** on a post toggles the icon + count instantly and the change persists while scrolling away and back.
5. Pull-to-refresh shows the spinner, then either prepends new posts or shows an error snackbar/state (verify by toggling the random-failure rate to 1.0f temporarily).
6. Rotating the device preserves the feed and like state (ViewModel survives configuration change).
7. The empty state is reachable by editing the seed list to be empty and relaunching; the error state is reachable by forcing the simulated failure.
8. No `runBlocking` is introduced anywhere outside the one existing site documented in `CLAUDE.md`.
9. After the build passes, install + launch the debug APK on a running emulator and capture a screenshot of the feed; surface its path in the final report.

---

## 9. Explicitly Out of Scope

Do **not** implement any of the following in this iteration — they will be separate plans:

- Real backend integration (Firestore, REST, GraphQL, etc.).
- A real "Create Post" screen, image picker, or upload flow.
- Comments screen, share sheet, or reaction picker (long-press on Like).
- Stories tray, Reels, Marketplace, friend suggestions, or any non-feed surface.
- Video posts, link previews, polls, or multi-image carousels.
- Pagination / infinite scroll — the seed list is small enough to render in one page.
- Analytics, A/B flags, or feature gating.

---

## 10. Deliverables

When you're done, report:
1. The list of files created and modified.
2. The result of the two `:dependencies` verifications from §8.
3. The build + install commands you ran and their exit status.
4. The path to the emulator screenshot of the feed.

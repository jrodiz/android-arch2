# Plan — Matches inbox redesign: cream canvas, NEW MATCHES rail, conversation rows

> **Parent plan:** [`plans/match-and-chat.md`](./match-and-chat.md) (data model + flow).
> **Sibling:** [`plans/chat-redesign-conversation.md`](./chat-redesign-conversation.md) (vocabulary + typography).

## 1. Context

The Matches tab (`feature/match/presentation/.../InboxScreen.kt`) currently uses a
default `TopAppBar("Matches")` with two `LazyColumn` sections built from
`ListItem` / `HorizontalDivider` rows that show only the owner's first name. The
mock asks for the same visual language already established in Login / SignUp /
Profile / Chat redesigns:

- A two-line title block at the top: small caps "Your inbox" label over a
  giant serif-feeling **"Matches"** headline, with a search icon on the right.
- A **NEW MATCHES** rail: small uppercase section header with a right-aligned
  "See all" link, then a horizontally-scrolling row of 4 circular pet avatars
  ringed in coral, with each owner / pet name underneath.
- A **CONVERSATIONS** list: uppercase header, then full-width tappable rows
  with a 56dp avatar, a bold two-line title row ("<Owner> & <Pet>"), a single
  line of muted preview text, and a right-aligned timestamp. The first row in
  the mock has a small coral unread dot.
- Cream background (existing `MaterialTheme.colorScheme.background = #FFFBFA`)
  so the floating coral chip nav reads correctly.

**Non-goals (out of scope for this PR):**

- Search functionality — the magnifier is rendered for the visual but is a no-op.
- A real "See all" destination — link is rendered but no-ops with a snackbar
  ("Full new matches list coming soon"). Tapping individual rail avatars *does*
  navigate (same as a row tap → `ChatRoute`).
- Per-conversation unread state — see §2.
- Per-rail "Mochi / Rex / Otto / Cloud" pet names — see §2.
- The bottom nav — already lives in `MainActivity.Scaffold.bottomBar` and feeds
  this screen the right `innerPadding`. Don't add extra bottom padding.
- Empty / loading states beyond what already exists (`EmptyTabState`).

## 2. Confirmed decisions & domain gaps

The mock surfaces information the domain does not currently carry. Each one is
addressed here so the code can ship without new repository wiring.

| Mock element | Domain has it? | Decision |
|---|---|---|
| Avatar in rail + row | `OwnerDisplay.avatarUrl` (owner avatar) | Use the owner avatar. We do not have the matched pet's photo plumbed through `MatchSummary` (would require joining `/pets`). Show the owner avatar with a coral ring in the rail and a plain circle in the row. |
| Rail label ("Mochi") | No pet name on `MatchSummary` | Render `OwnerDisplay.firstName` under each rail avatar (the rail is "people you matched with", not "pets"). Call this out in the report so the user can opt-in to a pet-avatar join later. |
| Row title ("Leah & Mochi") | Owner first name only | Show just the owner's first name in bold (matches the chat header degradation in `plans/chat-redesign-conversation.md` §2). |
| Preview text ("Hi! Wanna meet…") | `Match.lastMessagePreview` exists | Use it. Fallback "Say hello" when null (shouldn't fire since these rows always have `hasMessages = true`). |
| Per-row timestamp ("2m", "5h", "Mon") | `Match.lastMessageAt` exists | Format it inline with a tiny helper (see §6). |
| Unread dot on row 1 | No per-conversation unread flag on `MatchSummary` | **Skip the unread dot for now.** Adding it would require joining chat-message read receipts into the inbox snapshot — out of scope. Document as a deferred follow-up. |
| Status bar styling | n/a | Cream background → keep default dark status-bar icons (the existing app theme already does this). No `LightStatusBarIconsWhileShown`. |
| "See all" link | n/a | Wire to an in-presentation snackbar that says "Coming soon". |
| Search icon | n/a | No-op `IconButton`; snackbar same as above. |

## 3. Visual spec

All dimensions in dp; colors from `BrandColors` + `MaterialTheme.colorScheme`.

### 3.1 Top header (replaces `TopAppBar`)

- Padding: 24 start / 24 end / 16 top / 12 bottom.
- Layout: `Row(verticalAlignment = CenterVertically)`.
  - `Column(weight = 1f)`:
    - Text "Your inbox" — `labelMedium`, `onSurfaceVariant`, letter-spacing default.
    - Text "Matches" — `displaySmall.copy(fontWeight = ExtraBold)`, `onSurface`.
  - `IconButton(onClick = { snack("Coming soon") })` — `Icons.Outlined.Search`,
    tint `onSurface`, size 24, sits in a 40dp circle of `surfaceVariant`.

### 3.2 NEW MATCHES rail

- Section header row: 24 start / 24 end / 16 top / 8 bottom padding.
  - Left: Text "NEW MATCHES" — `labelMedium`, `onSurfaceVariant`,
    `letterSpacing = 1.2.sp`, `fontWeight = SemiBold`.
  - Right: TextButton "See all" — coral (`BrandColors.CoralDeep`), `labelMedium`,
    `fontWeight = SemiBold`, taps trigger snackbar.
- Rail: `LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp))`.
  - Each item is a `Column(horizontalAlignment = CenterHorizontally)`:
    - Outer Box `size = 72dp`, `background = BrandColors.Coral.copy(alpha = 0.18f)` clipped circle (ring).
    - Inner Box `size = 64dp`, centered, clipped to circle, background `surfaceVariant`, contains `AsyncImage` of avatar or fallback `Icon(Person)`.
    - Spacer 8dp.
    - Text — `labelMedium`, `onSurface`, single line, ellipsized, width 72dp.
  - Whole `Column.clickable { onOpenMatch(matchId) }`.
- If `newMatches.isEmpty()`: skip the entire rail (don't render header).

### 3.3 CONVERSATIONS list

- Section header: 24 start / 24 end / 24 top / 8 bottom.
  - Text "CONVERSATIONS" — same style as NEW MATCHES header (no trailing link).
- Each row (`Modifier.fillMaxWidth().clickable(...).padding(horizontal = 24.dp, vertical = 12.dp)`):
  - `Row(verticalAlignment = CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp))`:
    - Avatar `size = 56dp`, circle clip, `surfaceVariant` background, `AsyncImage` or `Icon(Person)`.
    - `Column(weight = 1f)`:
      - Text title — `titleMedium.copy(fontWeight = Bold)`, `onSurface`, single line.
      - Spacer 2dp.
      - Text preview — `bodyMedium`, `onSurfaceVariant`, single line, ellipsized.
    - Trailing `Text` timestamp — `labelSmall`, `onSurfaceVariant`, single line.
- **No `HorizontalDivider`** between rows — the spacing carries the rhythm
  (matches the mock).

### 3.4 LazyColumn wrapper

- `LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp))`.
- Items, in order:
  - `item { Header() }`
  - If new matches: `item { NewMatchesSection(snap.newMatches, ...) }`
  - If conversations: `item { SectionHeader("CONVERSATIONS") }` then
    `items(snap.conversations, key = { ... }) { ConversationRow(...) }`.

### 3.5 Empty state

Unchanged — still `EmptyTabState(...)` when both lists are empty.

## 4. Component changes

- **No new `:core:ui` or `:core:designsystem` components.** Everything is inline
  in `InboxScreen.kt` (small one-off composables: `InboxHeader`,
  `SectionHeader`, `NewMatchesRail`, `RailAvatar`, `ConversationRow`,
  `OwnerAvatar`, `formatInboxTimestamp`).
- **No new `BrandColors` tokens** — `Coral`, `CoralDeep`, `CoralLight` cover
  everything we render.

## 5. State / behavior changes

- `InboxUiState` is unchanged.
- `InboxViewModel` is unchanged.
- `InboxRoute` continues to forward `onOpenMatch(matchId)` to chat (set in
  `MatchNavModule`); a rail avatar tap also calls `onOpenMatch`.
- A new `internal const val NEW_MATCH_DESTINATION = "match"` is *not* added —
  rail avatar simply reuses `onOpenMatch`. Same behavior as a row tap.
- "See all" + search trigger `snackbar.showSnackbar(getString(R.string.match_coming_soon))`.

## 6. Files to add / modify

**Modify:**

- `feature/match/presentation/src/main/kotlin/com/rodiz/arch2/feature/match/presentation/InboxScreen.kt` — full rewrite of the screen body. ViewModel + Route signature unchanged.
- `feature/match/presentation/src/main/res/values/strings.xml` — new file (folder doesn't exist yet; create it). Strings: `match_inbox_eyebrow` ("Your inbox"), `match_inbox_title` ("Matches"), `match_section_new` ("NEW MATCHES"), `match_section_conversations` ("CONVERSATIONS"), `match_see_all` ("See all"), `match_search_cd` ("Search matches"), `match_coming_soon` ("Coming soon").

**Do NOT modify:**

- `:feature:match:domain` — no schema changes.
- `:feature:match:data`.
- `MatchNavModule`.
- `:core:designsystem` / `:core:ui` / shared theme.
- `MainActivity` / `FloatingChipNavBar`.

## 7. Critical recipes

- **`LazyColumn` with a `LazyRow` inside** — keep the row's `Modifier.fillMaxWidth()`
  off; use `contentPadding` for edge insets so the first/last items can be flicked off-screen.
- **Circular ring without a `Border`** — nest a smaller `Box` inside a translucent-coral
  `Box`; using `Modifier.border()` antialiases inconsistently around `clip(CircleShape)`.
- **Time formatting** — use `kotlinx.datetime.Clock.System.now()` and `TimeZone.currentSystemDefault()` (already in deps via `libs.kotlinx.datetime`). Helper:
  - `< 1 min` → "now"
  - `< 60 min` → "<n>m"
  - `< 24h` and same calendar day → "<n>h"
  - `< 7d` → weekday short ("Mon")
  - else → date short ("May 22")
- **Snackbar from inside a lazy list item** — pass the `SnackbarHostState` down (or the lambda); don't recreate per item.

## 8. Verification checklist

- `JAVA_HOME=...jbr-17.0.14... ./gradlew :app:installDebug` clean.
- Launch on `emulator-5556`. Sign in (if not already), tap the Matches chip in the nav.
- Visual check vs `/Users/jrodiz/Desktop/matches.png`:
  - Eyebrow + headline correct.
  - Rail scrolls, avatars are ringed, names visible.
  - Conversation rows have a bold name, lighter preview, right-aligned timestamp.
  - Floating chip nav still floats; no extra bottom padding gap.
- Empty inbox still shows `EmptyTabState`.

## 9. Out of scope

- Per-row unread indicator (needs chat read-receipt join).
- Real "See all" destination.
- Functional search.
- Pet avatar / pet name in row title — needs `:feature:pet:domain` join into
  `MatchSummary`. Track as follow-up; the chat redesign plan has the same gap.

## 10. Risk / rollback

- Pure presentation change. Revert is `git revert <hash>`. No data migration.
- Worst case if the redesign hits an emulator bug: revert restores the prior
  Material `ListItem` rows.

## 11. Implementation order

1. Add `feature/match/presentation/src/main/res/values/strings.xml`.
2. Rewrite `InboxScreen.kt` (one Edit / Write).
3. Build with JBR-17 → `:app:installDebug`.
4. Force-stop, launch, navigate to Matches tab, screencap.
5. Single local commit, no push.

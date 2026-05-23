# Plan — Chat / Conversation redesign: cream canvas, bubble-less messages, match banner

## 1. Context

The conversation screen (`feature/chat/presentation/.../ChatScreen.kt`) currently uses a
default Material `TopAppBar` with a `MoreVert` overflow, a `LazyColumn` of pill-shaped
`Surface` bubbles (primaryContainer for me, surfaceVariant for them) with `✓`/`✓✓` read
ticks, and an `OutlinedTextField` + circular send button composer. It works but is
visually generic — none of the brand vocabulary (cream canvas, coral accents, ghost
typography) from the Login / SignUp / Profile redesigns has landed here yet.

The new mock wants the conversation screen to feel like a quiet "letter on cream
paper" instead of a chat-bubble app:

- **White top app bar** (no elevation) with a back chevron, the other pet's square-rounded
  avatar (with a small coral badge in the bottom-right corner that holds the current
  user's pet avatar mini), a bold title row ("Leah & Mochi"), and a subtitle row
  with a small green dot + "Active now · < 5 km", and a filter/sliders icon on the
  right.
- **Cream/off-white canvas** matching the rest of the redesign (use existing
  `MaterialTheme.colorScheme.background = #FFFBFA`, no new token).
- **Match-context banner** just under the app bar — a soft coral pill with a filled
  coral circle (white heart icon) on the left, a bold coral title "You matched on
  Tuesday", and a coral subline "Biscuit · Mochi · Playdate".
- **Centered "Today" date separator** in muted gray.
- **Bubble-less messages**: incoming = black text on cream, outgoing = ghost-white
  text on cream (almost unreadable on purpose), both with a tiny timestamp underneath
  in muted gray (or muted white for outgoing). Generous vertical spacing — no tight
  grouping.
- **White rounded pill composer** with "Message Leah…" placeholder + smiley icon
  inside the pill, then a coral circular send button (paper plane icon, white) to
  the right of the pill.

**Goal:** match the mock visually, keep all existing chat wiring intact (send /
mark-read / observe-match / observe-chat / unmatch / block / report still work).

**Non-goals:**
- Online-status presence detection (no infra for it yet).
- Real distance computation between owners.
- Pet-name plumbing into the chat header (`OwnerDisplay` only carries owner first
  name + avatar today — see §2 for how the title degrades).
- A "your pet avatar mini" in the header coral badge (we don't have the current
  user's primary pet avatar accessible from chat presentation without new wiring).
- A real "you matched on <weekday>" string — we *do* have `match.createdAt`
  available, so this one we'll wire (see §5.3).
- Re-styling Unmatch / Block / Report dialogs and the report bottom sheet — they
  keep the current Material chrome.

## 2. Confirmed decisions

1. **Title** = `other.firstName` only. The mock shows "Leah & Mochi" (owner + pet).
   We don't have the other owner's primary pet name in `OwnerDisplay`. Adding it
   would mean changing `:core:ownerlookup:domain` + `data` + writing pet name on
   match creation, which is out of scope. The header shows "Leah" until we ship
   the pet-name extension. Flagged as a §11 follow-up.
2. **"Active now" presence row**: there is no presence signal. Render the green dot
   + "Active now" copy as **static visual chrome** only when we have an `other`
   loaded — it is intentional vapor for now so the visual lands. The distance
   ("< 5 km") is also static placeholder text. Both are §11 follow-ups. The mock
   doesn't read right without the row; deleting it would make the header look
   under-built. Tagged in code as `// TODO presence` / `// TODO distance`.
3. **Header coral badge (current user's pet mini)**: also placeholder. Render a
   small coral circle with a white paw icon in the bottom-right of the other-pet
   avatar — no real image — so the visual pattern is present without requiring
   new wiring. §11 follow-up to thread the current user's primary pet avatar.
4. **Match-context banner**: title = "You matched on `<formatted day>`" using
   `match.createdAt` (we already collect `observeMatch(matchId)` in the VM).
   Format: if today → "today", if yesterday → "yesterday", if within the last
   6 days → weekday name ("Tuesday"), else short date ("May 12"). Subline =
   "Pet · Other Pet · Playdate" — but since we don't have either pet name (see
   §2.1), we degrade to just **"Say hi · share a playdate idea"**. Banner is
   shown only when `match != null`. Tappable → no-op (placeholder; future could
   open a match-details bottom sheet).
5. **"Today" date separator**: insert a centered `Text("Today", muted gray)` once
   above the first message of today. We won't fully bucket by day in this pass
   (today's traffic is what dominates). If the conversation starts with old
   messages, the "Today" separator just sits above the first new-day message.
   Single-bucket separator, not a full date-bucketing pass. Tagged in code.
6. **Ghost outgoing text**: the mock explicitly asks for outgoing in white on
   cream. On a real screen, pure white on `#FFFBFA` is technically AA-fail
   (~1.04:1) — but the user explicitly noted this is intentional and asked us
   to flag if we deviate. **We ship pure white** (`Color.White`) as the mock
   asks. Deviation note in §10: if QA pushes back on accessibility, the
   fallback is `BrandColors.Coral.copy(alpha = 0.32f)` (~2.2:1) or a warm gray
   `Color(0xFFE7D9CB)` (~1.4:1) — easy single-line swap.
7. **Read ticks**: drop the `✓`/`✓✓` glyphs. The mock has only timestamps under
   each message, no read state. Keep the read-state in the model (the VM still
   calls `markAllRead`), just don't render anything for it. §11 follow-up if we
   want a "Read" hint back.
8. **Composer placeholder**: hardcoded "Message Leah…" in the mock. We render
   `"Message ${other.firstName}…"` when other is loaded, else `"Message…"`.
9. **Emoji icon inside the composer pill**: tappable but no-op (no emoji
   keyboard wired). Tagged as decoration. Tapping the field already opens the
   system keyboard, which lets users pick emojis from there.
10. **Filter / sliders icon on the right of the app bar**: re-use the existing
    overflow menu (Unmatch / Block / Report) and trigger it from a `Tune` icon
    instead of `MoreVert`. Mock shows what looks like a settings/sliders icon —
    `Icons.Outlined.Tune` is the closest match in `androidx.compose.material.icons`.
11. **Cream background**: `MaterialTheme.colorScheme.background` (`#FFFBFA`).
    Matches the Profile / Login redesigns; do not introduce a new `Cream` token.
12. **Status bar icons**: leave them dark (default). Top bar is white, not coral,
    so we do NOT call `LightStatusBarIconsWhileShown()`.

## 3. Visual spec

### 3.1 Scaffold

- `Scaffold(containerColor = MaterialTheme.colorScheme.background)`.
- `topBar` = `ChatTopBar(...)` — custom row, not `TopAppBar`, so we can stack the
  square-rounded avatar with corner badge + two-line title without fighting
  `TopAppBar`'s height limits. Built as a `Surface(color = White, tonalElevation
  = 0.dp, shadowElevation = 0.dp)` with `Modifier.statusBarsPadding()` and a
  fixed 72dp inner height.
- `bottomBar` = `Composer(...)` — also a custom row inside a `Surface(color =
  Color.Transparent)` with `Modifier.imePadding().navigationBarsPadding()`.

### 3.2 ChatTopBar (white, no shadow)

```
Row( height = 72.dp, padding(horizontal = 8.dp), vAlign = CenterVertically )
  IconButton(onBack)         // ArrowBack chevron, tint = onSurface
  Spacer(width = 4.dp)
  Box(size = 44.dp) {
    Box(size = 44.dp).clip(RoundedCornerShape(14.dp)).background(surfaceVariant)
      AsyncImage(other.avatarUrl, ContentScale.Crop)  // or Person icon fallback
    // coral mini-badge bottom-right
    Box(
      Modifier.size(18.dp)
        .align(BottomEnd)
        .offset(x = 4.dp, y = 4.dp)
        .clip(CircleShape)
        .background(BrandColors.Coral)
        .border(2.dp, Color.White, CircleShape)
    ) { Icon(Icons.Outlined.Pets, size = 10.dp, tint = White) }   // placeholder
  }
  Spacer(width = 12.dp)
  Column(weight = 1f) {
    Text(title, titleMedium.copy(fontWeight = Bold), maxLines = 1)
    Row(vAlign = Center) {
      Box(size = 8.dp).clip(CircleShape).background(Color(0xFF22C55E))
      Spacer(width = 6.dp)
      Text("Active now · < 5 km", labelMedium, color = onSurfaceVariant)
    }
  }
  IconButton(onMenu) { Icon(Icons.Outlined.Tune, contentDescription = "Chat options") }
DropdownMenu(anchored to right icon) { Unmatch / Block / Report — same as today }
```

### 3.3 MatchContextBanner

```
Surface(
  shape = RoundedCornerShape(20.dp),
  color = BrandColors.Coral.copy(alpha = 0.12f),  // light coral wash
  modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
)
  Row(padding = 14.dp, vAlign = Center)
    Box(size = 40.dp).clip(CircleShape).background(BrandColors.Coral) {
      Icon(Icons.Filled.Favorite, size = 20.dp, tint = White)
    }
    Spacer(width = 14.dp)
    Column {
      Text("You matched ${formatMatchDay(match.createdAt)}",
           titleSmall.copy(fontWeight = SemiBold),
           color = BrandColors.CoralDeep)
      Spacer(height = 2.dp)
      Text("Say hi · share a playdate idea",
           labelMedium,
           color = BrandColors.CoralDeep.copy(alpha = 0.85f))
    }
```

`formatMatchDay(Instant)` — local helper inside ChatScreen.kt:
- 0 days ago → "today"
- 1 day ago → "yesterday"
- 2..6 days ago → `DayOfWeek` name lowercased ("on tuesday")
- otherwise → `LocalDate.format("MMM d")` ("on May 12")

(Returns the connector word too so the banner reads naturally.)

### 3.4 Message list

`LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding =
PaddingValues(horizontal = 24.dp, vertical = 16.dp))`.

Single "Today" separator inserted as a `stickyHeader`-less item once, above the
first message in the local day. Render as a centered muted `Text("Today",
labelMedium, color = onSurfaceVariant.copy(alpha = 0.7f))`.

Each message → `MessageRow`:

```
Column(
  Modifier.fillMaxWidth(),
  horizontalAlignment = if (isMine) End else Start
) {
  Text(
    message.text,
    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
    style = bodyLarge,
    modifier = Modifier.widthIn(max = 280.dp),
    textAlign = if (isMine) TextAlign.End else TextAlign.Start
  )
  Spacer(height = 4.dp)
  Text(
    formatTime(message.createdAt),
    color = if (isMine) Color.White.copy(alpha = 0.85f) else onSurfaceVariant.copy(alpha = 0.6f),
    style = labelSmall
  )
}
```

`formatTime(Instant)` → `HH:mm` in local time, e.g. "10:42". Static 24h format
to match the mock ("10:42").

### 3.5 Composer (bottom bar)

```
Row(
  Modifier
    .fillMaxWidth()
    .background(MaterialTheme.colorScheme.background) // cream
    .padding(horizontal = 16.dp, vertical = 12.dp)
    .imePadding()
    .navigationBarsPadding(),
  verticalAlignment = CenterVertically,
)
  // pill
  Surface(
    shape = RoundedCornerShape(28.dp),
    color = Color.White,
    shadowElevation = 0.dp,
    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
  )
    Row(padding(horizontal = 18.dp, vertical = 4.dp), vAlign = Center) {
      BasicTextField(
        value = draft,
        onValueChange = onDraftChange,
        textStyle = bodyLarge.copy(color = onSurface),
        cursorBrush = SolidColor(BrandColors.Coral),
        modifier = Modifier.weight(1f),
        decorationBox = { inner ->
          if (draft.isEmpty()) Text(placeholder, color = onSurfaceVariant.copy(alpha = 0.6f), style = bodyLarge)
          inner()
        }
      )
      IconButton(onClick = { /* decorative */ }, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Outlined.SentimentSatisfied, contentDescription = null, tint = onSurfaceVariant)
      }
    }
  Spacer(width = 10.dp)
  // coral send circle
  Surface(
    shape = CircleShape,
    color = BrandColors.Coral,
    modifier = Modifier.size(52.dp).clickable(enabled = canSend) { onSend() }
  )
    Box(contentAlignment = Center, fillMaxSize) {
      Icon(Icons.AutoMirrored.Outlined.Send, tint = White, size = 22.dp)
    }
```

Where `canSend = draft.isNotBlank() && !isSending`. Disabled state: fade the
coral surface to `BrandColors.Coral.copy(alpha = 0.5f)` and ignore the click.

Use `BasicTextField` (not `OutlinedTextField`) so we can put a single text style
into a white pill without M3's border / supporting-text chrome bleeding out.

## 4. Component changes

No changes to `:core:designsystem` or `:core:ui`. Everything is local to
`ChatScreen.kt`:

- `ChatTopBar(other: OwnerDisplay?, onBack, onMenuTap)` — private composable.
- `MatchContextBanner(createdAt: Instant)` — private composable.
- `MessageRow(message: Message, isMine: Boolean)` — replaces `MessageBubble`.
- `Composer(draft, isSending, placeholder, onDraftChange, onSend)` — rewritten.
- Local helpers `formatMatchDay(Instant): String` and `formatTime(Instant):
  String` using `kotlinx.datetime`.

## 5. State / behavior changes

### 5.1 `ChatUiState`
- Add `val match: Match? = null` to surface `createdAt` for the banner. The VM
  already observes the match via `observeMatch(matchId)` — currently it only
  uses it to detect `unmatched`. We'll store the latest non-null match.

### 5.2 `ChatViewModel`
- In the existing `observeMatch(matchId)` subscription, in addition to
  detecting `null` for unmatch, also `_uiState.update { it.copy(match = match) }`
  on non-null emissions.
- No other VM changes. Send / draftChanged / unmatchAndExit / blockAndExit /
  submitReport stay identical.

### 5.3 Strings
Add `feature/chat/presentation/src/main/res/values/strings.xml`:
- `chat_active_now` = `"Active now · < 5 km"` (intentional vapor — §11)
- `chat_back_cd` = `"Back"`
- `chat_options_cd` = `"Chat options"`
- `chat_options_unmatch` = `"Unmatch"`
- `chat_options_block` = `"Block"`
- `chat_options_report` = `"Report"`
- `chat_banner_title_template` = `"You matched %1$s"` (`%1$s` = "today" / "on
  tuesday" / etc.)
- `chat_banner_subtitle` = `"Say hi · share a playdate idea"`
- `chat_date_today` = `"Today"`
- `chat_composer_placeholder_default` = `"Message…"`
- `chat_composer_placeholder_template` = `"Message %1$s…"`
- `chat_send_cd` = `"Send"`
- `chat_emoji_cd` = `"Emoji"`

All previously-inline copy ("Unmatch?", "Block this person?", report sheet copy)
stays inline in this pass — out of scope.

## 6. Files

### Modify
- `feature/chat/presentation/src/main/kotlin/.../ChatScreen.kt` — rewrite the
  five render functions (`ChatScreen`, `ChatHeaderTitle` → `ChatTopBar`,
  `MessageList`, `MessageBubble` → `MessageRow`, `Composer`) plus add the
  banner + date helpers. Keep the dialogs + report sheet as-is.
- `feature/chat/presentation/src/main/kotlin/.../ChatViewModel.kt` — add
  `match: Match?` to `ChatUiState` and update the existing `observeMatch`
  collector to push it.
- `feature/chat/presentation/build.gradle.kts` — add `implementation(libs.kotlinx.datetime)`.

### Add
- `feature/chat/presentation/src/main/res/values/strings.xml`.

### Do NOT modify
- `:core:designsystem`, `:core:ui`, `:core:ownerlookup:*`, `:feature:match:*`,
  `:feature:chat:nav`, `:feature:chat:domain`, `:feature:chat:data`.

## 7. Critical recipes

- **BasicTextField inside a Surface pill** — use `decorationBox` for the
  placeholder so the cursor sits centered in the pill, and pass
  `Modifier.weight(1f)` to the inner BasicTextField so the smiley icon stays
  pinned to the right edge.
- **Custom top bar instead of TopAppBar** — wrap the custom row in
  `Surface(color = Color.White, shadowElevation = 0.dp)` and add
  `Modifier.statusBarsPadding()`. Don't use `TopAppBar` for two-line subtitles
  with custom leading composition — it auto-clips.
- **No bubble = no Surface around the message text** — just `Text` directly in
  the column. Use `Modifier.widthIn(max = 280.dp)` so long messages wrap nicely
  without filling the gutters edge-to-edge.
- **`imePadding()` + `navigationBarsPadding()` on the composer**, not on the
  list — otherwise the cream background under the system bar looks weird.
- **`LazyColumn` reverseLayout**: do NOT use it. The mock reads top-to-bottom
  (oldest at the top, newest at the bottom). Keep the current `animateScrollToItem(lastIndex)`
  on new message arrival.
- **kotlinx.datetime → local time conversion** — `instant.toLocalDateTime(TimeZone.currentSystemDefault())`
  for both the banner day formatter and the `HH:mm` timestamp.
- **DayOfWeek lowercase** — `dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }`
  → "Tuesday" (then we prefix with " on ").
- **Color.White on cream background is intentionally low-contrast** — see §10
  for the fallback if QA flags it.

## 8. Verification checklist

- [ ] Build green: `JAVA_HOME=.../jbr-17 ./gradlew :app:installDebug`.
- [ ] Launch app, sign in, navigate Match tab → tap an existing conversation →
      chat opens.
- [ ] Top bar is white, no shadow, shows the other pet's avatar with the small
      coral badge, the first-name title, and the green-dot subtitle row.
- [ ] Coral context banner is visible under the bar with "You matched
      today/yesterday/on <weekday>".
- [ ] Incoming messages render in black, outgoing in ghost white, both with
      `HH:mm` timestamps in muted text underneath.
- [ ] Composer is a white pill with placeholder "Message Leah…" + smiley icon,
      flanked by a coral circular send button on the right.
- [ ] Send still works — type a message, tap the send button, message appears
      with a timestamp.
- [ ] Tap the right-side `Tune` icon → dropdown still shows Unmatch / Block /
      Report and each path still works.
- [ ] Back chevron returns to the Match inbox.
- [ ] Screenshot pulled and visually matches the mock.

## 9. Out of scope

- Pet name in the title ("& Mochi"). Requires extending `OwnerDisplay` and the
  Firestore owner doc.
- Real presence ("Active now" / "Away").
- Real distance between owners.
- Current user's pet avatar inside the header coral badge.
- Restyling Unmatch / Block / Report dialogs and the report bottom sheet.
- Full date-bucketing (Today / Yesterday / earlier dates as multiple
  separators).
- Read-state glyphs (✓/✓✓). Removed in this pass.

## 10. Risk / deviations

- **Pure-white outgoing text on cream is intentionally low-contrast** and fails
  WCAG AA. Shipping as-mocked per the user's explicit instruction. If QA
  pushes back, swap line in `MessageRow`:
  ```
  color = if (isMine) Color.White else onSurface
  // → fallback: BrandColors.Coral.copy(alpha = 0.32f)  // ~2.2:1, still soft
  ```
- "Active now" + "< 5 km" + the coral pet-mini badge are decorative placeholders
  with no signal. If a reviewer flags them as misleading, the safe fallback is
  to hide the subtitle row and the badge until real data lands — single
  `if (false)` guard each.
- Removing read ticks: the model still tracks read state, so re-introducing
  them later is one composable away. Documented in §11.

## 11. Follow-ups (not this PR)

- Extend `OwnerDisplay` with `primaryPetName` + `primaryPetAvatarUrl` (and the
  Firestore writes in `pet` / `profile` to populate them) so the title can read
  "Leah & Mochi" and the header badge can show the current user's mini pet
  avatar.
- Presence service for "Active now" / "Last seen 3h ago".
- Distance computation between owners (requires location sharing flag from
  Settings → Discovery).
- Optional "Read" hint under the last outgoing message that the other side has
  read.

## 12. Implementation order

1. Add `kotlinx.datetime` to `feature/chat/presentation/build.gradle.kts`.
2. Create `strings.xml` with all 12 keys from §5.3.
3. Edit `ChatViewModel.kt` — add `match: Match?` field, update `observeMatch`
   collector to push it.
4. Rewrite `ChatScreen.kt` top-to-bottom: imports → `ChatScreen` body →
   `ChatTopBar` → `MatchContextBanner` → `MessageList` (single "Today"
   separator) → `MessageRow` → `Composer` → date/time helpers. Keep dialogs +
   report sheet untouched.
5. Build, fix compile errors.
6. Install on `emulator-5556`, navigate to chat, screenshot.
7. Compare to mock, iterate on spacing / colors if needed.
8. Single commit `feat(chat): bubble-less conversation redesign on cream`.

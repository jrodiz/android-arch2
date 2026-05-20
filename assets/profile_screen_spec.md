# Profile Screen — TinPet Android

## Context
Replace the ProfileScreen with a new **Account / Profile** screen for the TinPet Android app. The screen is reached from the main navigation after the user has authenticated and moved to the profile tab. All user identity information must come from the Google credential already obtained at login for now, since login with user email and password is still fake let's just display non-missing info.

## Visual Reference
Match the layout, proportions, and component hierarchy shown in the attached reference screenshot. Use the **current app theme** (colors, typography, shapes, spacing) — **do not hardcode any colors or font values**. Pull every visual token from the existing theme/design system.

## Screen Composition

### 1. Top app bar
- Circular back button (top-left) returning to the previous destination.
- Centered title: `Account`.
- Background matches the theme's primary surface/background.

### 2. Header zone (two columns)
Two-column row, roughly **1 : 2** width ratio.

**Left column — stacked square tiles:**
- **Profiles** tile — shows the user's avatar/initial inside a circular badge.
- **My List** tile — card-stack icon.

**Right column — profile card:**
- Large rounded card filled with the theme accent color.
- Circular (or rounded-square) profile picture sourced from the Google account `photoUrl`.
- Display name (from Google `displayName`) shown below the photo, underlined.
- Tapping the card opens the detailed profile view.

### 3. Menu list panel
A rounded-top container using a light surface color from the theme. Vertical list of rows, each with a label on the left and a chevron-right indicator on the right:

1. Edit Profile
2. Manage Subscriptions
3. Saved Cards
4. Settings
5. Notifications
6. Privacy Settings

Each row navigates to its respective destination (stub navigation if the destination doesn't yet exist).

### 4. Sign Out
Filled pill-shaped button at the bottom of the panel, using the theme accent color. Tapping it:
1. Shows a confirmation dialog.
2. On confirm, signs the user out of Google, clears local session/state, and navigates back to the login screen with the back stack cleared.

## Data Source — Google Sign-In
All user data displayed on this screen must be read from the `GoogleSignInAccount` (or equivalent credential model) obtained on the previous login screen. Pass it through the navigation graph / shared ViewModel — do not re-trigger sign-in here.

Mapping:
- `displayName` → profile card name + initial in the "Profiles" tile.
- `photoUrl` → profile picture (fallback: initial-letter avatar on the theme accent if the image fails to load).
- `email` → available to downstream screens (Edit Profile, etc.).
- `id` → stable user identifier for backend calls.

## Theming Rules
- Use `MaterialTheme.colorScheme.*` (or the project's existing theme tokens) for every color.
- Use `MaterialTheme.typography.*` for all text styles.
- Use the project's shape tokens for corner radii.
- No literal hex values, no inline color constants.

## Behavior & States
- **Loading:** show a skeleton/shimmer on the profile card while the credential resolves (only relevant on a cold start before the session is restored).
- **Loaded:** full UI as specified.
- **Image failure:** fall back to a circular initial-letter avatar.
- **Back gesture and back button:** both return to the previous screen.
- **Accessibility:** every row is at least 48dp tall and exposes a proper TalkBack label.

## Acceptance Criteria
- Profile name and photo are populated entirely from the Google credential — zero manual input on this screen.
- All colors, fonts, and shapes come from theme tokens.
- Layout proportions match the reference screenshot across common phone widths.
- All six menu rows navigate (or stub-navigate) correctly.
- Sign Out clears the auth state and prevents back-navigation into authenticated screens.

## Out of Scope
- Implementation of the destination screens (Edit Profile, Settings, etc.) — stubs only.
- Any editing of profile data on this screen.
- Backend changes; this screen is read-only with respect to the Google credential.

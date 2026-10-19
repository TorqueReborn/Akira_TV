# Android TV Development Rules & Best Practices

## 1. UI Toolkit & Dependencies
- **Primary Framework**: Jetpack Compose for TV (`androidx.tv:tv-material` and `androidx.tv:tv-foundation`).
- **Strict Prohibition**: Do NOT use mobile `androidx.compose.material3` components (`Button`, `Card`, `LazyRow`, `Surface`, etc.) in TV UI screens. Mixing mobile and TV Material causes theme inconsistencies and breaks D-pad focus indicators.
- **TV Components**:
  - `androidx.tv.material3.Button`
  - `androidx.tv.material3.Card` / `CompactCard` / `ClassicCard` / `StandardCard`
  - `androidx.tv.material3.Carousel`
  - `androidx.tv.material3.ListItem`
  - `androidx.tv.material3.NavigationDrawer` / `ModalNavigationDrawer`
  - `androidx.tv.material3.Surface`
  - `androidx.tv.material3.MaterialTheme`
  - `androidx.tv.foundation.lazy.list.TvLazyRow`
  - `androidx.tv.foundation.lazy.list.TvLazyColumn`

## 2. 10-Foot UI Layout & Navigation Architecture
- **Overscan Margins**: Always apply outer margins/padding of at least **48dp** (`PaddingValues(horizontal = 48.dp, vertical = 27.dp)`) on main screen containers to account for TV screen overscan.
- **Top-Level Navigation**: Use `NavigationDrawer` or `ModalNavigationDrawer` from `androidx.tv.material3`.
  - Limit navigation items to 5–6 primary destinations to avoid vertical overflow.
  - Every navigation item MUST contain both an icon and a text label.
- **Typography & Scale**: Use TV Material typography styles (`displayMedium`, `headlineLarge`, `titleMedium`) designed for high legibility from a 10-foot distance.

## 3. Focus Management & D-Pad Navigation
- **Visible Focus State**: All interactive components MUST exhibit a clear visual indicator when focused (such as scaling, border glow, or color shift).
- **100% D-Pad Reachability**: Every interactive element must be reachable using only directional keys (**Up, Down, Left, Right**). No touch or cursor assumptions.
- **Initial Focus**: On every screen load, request focus on the primary interactive element using `FocusRequester` and `LaunchedEffect` so the user never encounters lost focus:
  ```kotlin
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) { focusRequester.requestFocus() }
  ```
- **Focus Restoration**: Attach `Modifier.focusRestorer()` on horizontal lists (`TvLazyRow` / `LazyRow`) so that when the user scrolls vertically across rows, returning to a row restores the previously focused item.
- **Focus Tracking**: Use `.onFocusChanged { state -> if (state.isFocused) ... }` to update hero banner details, background posters, or active item state when focus moves.
- **Pivot Scrolling**: Use `BringIntoViewSpec` with `LocalBringIntoViewSpec` to define TV row pivot lines (e.g. pivoting active items at 35% from the top edge).

## 4. Text Inputs & Authentication
- **Secondary Device / QR Code Preferred**: Prefer QR code sign-in or activation codes over manual text entry on TV remotes whenever possible.
- **Field Navigation**: For text input screens, route focus smoothly using `KeyboardActions` and `ImeAction.Next` (`focusManager.moveFocus(FocusDirection.Down)`).
- **Escape Key / Back Interception**: Intercept `Key.Back` / `Key.Escape` in `.onPreviewKeyEvent` on text fields so the user can exit text editing mode and return to D-pad screen navigation without getting trapped.

## 5. Media Playback (Media3) & Audio Focus
- **Video Surface**: Use `androidx.media3.ui.compose.PlayerSurface` with `ExoPlayer` for video rendering.
- **No Legacy Wrappers**: Do NOT wrap legacy `PlayerView` in `AndroidView`.
- **D-Pad Controls**: Intercept D-pad key events (`Key.DirectionLeft`, `Key.DirectionRight`, `Key.DirectionCenter`) in `.onPreviewKeyEvent` to control seeking (e.g. +/- 10s) and play/pause state.
- **Audio Focus**: Always manage `AudioFocusRequest` when starting video or audio playback to pause gracefully if another TV app requests audio.

## 6. Manifest & Google Play TV Quality Compliance
- **Leanback Launcher**: Include `<category android:name="android.intent.category.LEANBACK_LAUNCHER" />` in the main activity filter.
- **Hardware Features**:
  - `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />`
  - `<uses-feature android:name="android.software.leanback" android:required="true" />` (or `false` for multi-device apps).
  - `<uses-feature android:name="android.hardware.gamepad" android:required="false" />`
- **TV Banner**: Mandatory `android:banner="@drawable/ic_banner"` on application and main activity tags (320x180 px banner).
- **Orientation**: Fixed landscape mode (`android:screenOrientation="landscape"`) or handling `configChanges="orientation|screenSize|screenLayout|keyboardHidden|keyboard|navigation|uiMode"`.

## 7. Low-End Hardware & Performance Optimization
- **TV Specs**: Many Android TV devices (streaming sticks, dongles) have limited CPU and RAM (1GB-2GB).
- **Image Caching**: Use image loading libraries (e.g., Coil) configured for low memory overhead and avoid high-res images that exceed 1080p rendering targets.
- **Lazy List Keying**: Always provide unique `key` parameters in `items()` inside `TvLazyRow` and `TvLazyColumn` to avoid unnecessary recomposition jank.

## 8. GitHub Releases In-App Auto-Updater Architecture
- **Release Source**: Fetch latest release from `https://api.github.com/repos/{owner}/{repo}/releases/latest`.
- **Version Comparison**: Compare `tag_name` against `BuildConfig.VERSION_NAME` / `BuildConfig.VERSION_CODE`.
- **Manifest Requirements**:
  - Requires `<uses-permission android:name="android.permission.INTERNET" />`
  - Requires `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`
  - Requires `FileProvider` (`${applicationId}.provider`) pointing to `res/xml/file_paths.xml`.
- **TV Update UI**:
  - Display TV Compose dialog (`UpdateAvailableDialog`) showing release notes.
  - Automatically place D-pad focus on the "Update Now" button via `FocusRequester`.
  - Show download progress indicator during APK download to cache directory.
- **Installation Trigger**: Launch `Intent.ACTION_VIEW` with `application/vnd.android.package-archive` and `FLAG_GRANT_READ_URI_PERMISSION` using `FileProvider`. Handle system "Install Unknown Apps" permission request gracefully.

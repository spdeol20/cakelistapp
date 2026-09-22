# Cake List App

An Android app that fetches a list of cakes from a remote JSON endpoint and displays them in a
scrollable list. Tapping a cake opens a dialog with its full description. Built with Jetpack Compose
and Material 3.

The data comes from the Waracle mobile coding test API:

```
https://raw.githubusercontent.com/Waracle/mobile-coding-test-api/refs/heads/main/cakes
```

## Features

- Cake list with thumbnail, title, and truncated description
- Detail dialog on tap showing the full description
- Pull to refresh, plus a refresh action in the top app bar
- Full-screen error state with retry when the list is empty, snackbar when stale data is on screen
- Duplicate titles removed and the list sorted alphabetically before it reaches the UI
- Staggered fade-and-drop entrance animation that plays once per item

## Requirements

| Tool | Version |
| --- | --- |
| Android Gradle Plugin | 9.2.1 |
| Gradle | 9.4.1 (wrapper included) |
| Kotlin | 2.2.10 |
| JDK | 11 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 33 |

`minSdk 33` means Android 13 or newer.

## Build and run

From the project root:

```powershell
# Debug build
.\gradlew.bat assembleDebug

# Install on a connected device or running emulator
.\gradlew.bat installDebug

# Unit tests
.\gradlew.bat testDebugUnitTest

# Instrumented tests (requires a device or emulator)
.\gradlew.bat connectedDebugAndroidTest
```

On macOS or Linux use `./gradlew` instead. In Android Studio, open the project root and run the
`app` configuration; the Gradle wrapper handles the toolchain.

## Architecture

Layered, unidirectional data flow. Dependencies point inward: UI depends on domain, data implements
domain, and domain depends on nothing.

```
ui        CakeListScreen, CakeListViewModel, CakeListUiState, EntranceAnimation
              |
domain    Cake, CakeRepository (interface)
              |
data      CakeRepositoryImpl, CakeApi, CakeDto, CakeMapper, NetworkModule
```

```
app/src/main/java/com/example/cakelistapp/
├─ CakeListApplication.kt        Application class; supplies the singleton Coil ImageLoader
├─ MainActivity.kt               Single activity hosting the Compose tree
├─ di/
│  └─ AppContainer.kt            Manual DI: lazy repository and ImageLoader factory
├─ domain/
│  ├─ model/Cake.kt              UI-facing model; no serialization or framework types
│  └─ repository/CakeRepository.kt
├─ data/
│  ├─ remote/api/CakeApi.kt      Retrofit interface
│  ├─ remote/dto/CakeDto.kt      Wire format, all fields nullable
│  ├─ remote/mapper/CakeMapper.kt  DTO to domain, validation and de-duplication
│  ├─ remote/network/NetworkModule.kt  OkHttp, Retrofit, JSON setup
│  ├─ remote/network/CakeApiLog.kt     Debug-only logging wrapper
│  └─ repository/CakeRepositoryImpl.kt
└─ ui/
   ├─ animation/EntranceAnimation.kt   Reusable entrance animation modifier
   ├─ cakes/CakeListScreen.kt          Composables: route, screen, content, row, thumbnail
   ├─ cakes/CakeListViewModel.kt
   ├─ cakes/CakeListUiState.kt         Single immutable state object
   └─ theme/
```

### State and data flow

`CakeListViewModel` exposes one `StateFlow<CakeListUiState>`. The UI collects it with
`collectAsStateWithLifecycle` and sends events back as lambdas; nothing writes state from the
composition. `CakeListContent` is a stateless composable taking state plus callbacks, so it is
previewable and testable without a ViewModel.

The initial load is triggered from the ViewModel's `init` block. Each load cancels the previous job,
so a refresh during an in-flight request cannot deliver a stale result.

`isInitialLoading` drives the full-screen spinner, `isRefreshing` drives the pull-to-refresh
indicator, and they are kept separate so a refresh never blanks out the list.

### Dependency injection

No DI framework, deliberately. The graph is a repository and an image loader, so Hilt would add an
annotation processor and a layer of indirection without removing any real wiring. `AppContainer`
holds the two as lazily constructed singletons, and `CakeListViewModel` receives everything through
its constructor via `CakeListViewModel.factory`.

The payoff is in the tests: because nothing resolves its own dependencies, every test builds the
ViewModel directly with fakes and the container is never involved. That is also what makes moving to
Hilt or Dagger cheap later — constructor injection is already the norm, so a migration would touch
`AppContainer` and the route that reads it, not the ViewModel or any test. Worth doing once there
are scoped dependencies or several more graph edges; not before.

## Notable implementation decisions

**Thumbnail decode size.** `CakeThumbnail` uses `AsyncImage`, which derives its decode size from the
measured constraints, so the fixed `THUMBNAIL_SIZE` on the wrapper is load-bearing. Given an
unbounded size Coil decodes at source resolution — some of these images are 3000x2000, a 24 MB
bitmap for a 56dp icon, which evicts the memory cache and makes the list flicker while scrolling.
`rememberAsyncImagePainter` was used previously and needed an explicit `ImageRequest.size` for the
same reason, since the painter alone has no layout node to measure.

**Error state drawn as an overlay.** A failed load leaves the `AsyncImage` in composition and draws
the broken-image icon on top, rather than branching it out. Swapping the request out of composition
would cancel it and prevent recovery, and the overlay keeps the icon tinted and inset, which an
`error` painter passed to `AsyncImage` cannot do because it inherits the image's `ContentScale`.

**Shared transport.** Image traffic goes through an OkHttp client derived from the same base client
as the API, so timeouts, connection pool, and TLS configuration stay consistent and any future
certificate pinning covers both. Image logging is capped at `BASIC` since the payloads are binary.

**Content-type rewrite.** GitHub raw serves JSON as `text/plain`. An interceptor rewrites the header
for that host only, rather than configuring the converter to accept arbitrary content types.

**Entrance animation.** `Modifier.entranceAnimation` reads the animated value inside the
`graphicsLayer` lambda, confining each frame to the draw phase. Reading it in composition (via
`Modifier.alpha` or `Modifier.offset`) would recompose and re-layout every visible item every frame.
The stagger is anchored to a shared wave-start timestamp rather than each item's own composition
time, so the sequence holds even when the main thread is busy. Played keys are tracked in
`rememberSaveable` state, so the animation does not replay on scroll-back or after a rotation.

## Security notes

- Cleartext traffic is disabled via `network_security_config.xml`; HTTPS only.
- `CakeMapper` rejects any image URL that is not a well-formed HTTPS URL and drops cakes with no
  title. API responses are treated as untrusted input.
- `INTERNET` is the only permission requested.
- No secrets or credentials are present in the app; the endpoint is public and unauthenticated.
- Logging is gated behind `BuildConfig.DEBUG` through `CakeApiLog`, and the HTTP logger redacts
  `Authorization`, `Cookie`, and `Set-Cookie`.

## Tests

Unit tests live in `app/src/test`:

| Test | Covers |
| --- | --- |
| `CakeListStartupTest` | End-to-end startup through a fake API: success and failure states |
| `CakeListViewModelTest` | Load, refresh, error handling, selection, and dismissal |
| `CakeRepositoryImplTest` | Repository mapping behaviour against a fake API |
| `CakeMapperTest` | DTO validation, HTTPS filtering, de-duplication, sorting |

`MainDispatcherRule` swaps the main dispatcher for a test dispatcher so `viewModelScope` work runs
synchronously.

## Known tooling quirk

Attaching the Compose Layout Inspector tears down and recreates the Compose tree without recreating
the activity or ViewModel. The list therefore renders twice and the entrance animation replays. This
is an inspector artifact, not app behaviour — run without it attached to see the real startup.

## Dependencies

| Library | Purpose |
| --- | --- |
| Jetpack Compose (BOM 2026.02.01) + Material 3 | UI |
| Lifecycle ViewModel / runtime-compose | State holding, lifecycle-aware collection |
| Retrofit 2.11.0 | HTTP client interface |
| OkHttp 4.12.0 | Transport, logging interceptor |
| kotlinx.serialization 1.8.1 | JSON parsing |
| Coil 3.4.0 (`coil-compose`, `coil-network-okhttp`) | Image loading |
| JUnit 4, kotlinx-coroutines-test | Testing |

Coil is pinned to 3.4.0 deliberately. Versions 3.5.0 and later ship Kotlin 2.4 metadata, which the
project's Kotlin 2.2 compiler cannot read. Bump it together with the `kotlin` version in
`gradle/libs.versions.toml`.

## AI assistance

Around half of this codebase was produced with AI assistance (Cursor, using Claude). The brief asks
for that to be disclosed, so here is the honest breakdown.

**AI-assisted, roughly 50%**
- Boilerplate: the Retrofit and kotlinx.serialization wiring, the Compose screen skeleton, theme files
- First drafts of the entrance animation modifier and the thumbnail composable
- This README

**Mine**
- Architecture: the ui/domain/data split, the repository interface living in domain, a single
  immutable UI state object, and keeping all security logic out of the UI layer
- Design calls: a dialog rather than a detail screen, manual DI rather than Hilt at this size,
  routing Coil through the same OkHttp client as the API, HTTPS-only validation in the mapper
- The debugging, which is where most of the real time went: full-resolution image decodes causing
  scroll flicker, an entrance animation staggering from each item's own composition time instead of
  a shared start, and an apparent double render on launch that turned out to be the Compose Layout
  Inspector rebuilding the tree, not the app
- Reviewing and cutting back what the assistant generated. The thumbnail originally hand-built an
  `ImageRequest` with explicit size, precision, and memory cache keys; once I understood why those
  were needed I replaced the whole thing with `AsyncImage`, which derives the decode size from its
  layout constraints and does the same job in a third of the code

Nothing here was accepted because it compiled. Every file has been read, and several were rewritten
or deleted after review.
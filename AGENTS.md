# Pinmap — Agent Instructions

Single-module Android app (`:app`) using Jetpack Compose + Material3, AMap 3D SDK, Room, Navigation Compose, Coil.

## Commands

```bash
# Build / lint / test
./gradlew assembleDebug
./gradlew lint
./gradlew test                       # unit tests only
./gradlew connectedAndroidTest       # instrumentation tests (device/emulator)

# Install
./gradlew installDebug
```

## Key Architecture

- **Entrypoint**: `MainActivity` creates DB + repositories manually (no DI), sets up drawer + NavHost
- **Navigation**: `Screen` sealed class routes in `ui/navigation/Screen.kt`; `PinmapNavGraph` in `NavGraph.kt`
- **Route params**: `PinDetail` / `PinEdit` use `{pinId}` (Long). `PinEdit.createRoute(null)` passes `0` as sentinel.
- **ViewModelFactory** exists but `MapScreen` also creates its own ViewModel via `viewModel()` factory. The factory in `MainActivity` only instantiates `MapViewModel`.
- **Database version**: 2, `fallbackToDestructiveMigration()` — schema changes wipe all data
- **AMap search SDK** is commented out in `app/build.gradle.kts` (duplicate class conflict with 3D SDK)
- **AMap auto-update disabled** in `PinmapApplication`: `amap.sdk.update.enable=false`
- **Drawer gestures disabled on map screen**: `gesturesEnabled = currentRoute != "map"`
- **Search** is embedded on MapScreen (bottom center, not a separate route)

## Database

7 entities: Pin, Category, CustomField, FieldTemplate, FieldValue, Attachment, OfflineMap.
Relations: Pin belongs-to Category (M:1), Pin has-many CustomField/FieldTemplate(via FieldValue)/Attachment.
Field templates: `categoryId != null` = category-wide; `categoryId == null` = standalone.
Field types: TEXT, NUMBER, DATE, SINGLE_CHOICE, MULTI_CHOICE, IMAGE.
DAO methods return Flow<> for reactive queries.
Room annotation processor uses KSP: `ksp(libs.room.compiler)`.

## Dependencies

Managed via `gradle/libs.versions.toml` (Gradle Version Catalog). Always add new deps there.

## Tests

Placeholders only — `app/src/test/java/.../ExampleUnitTest.kt` and `app/src/androidTest/.../ExampleInstrumentedTest.kt`. No real tests exist.

## Style

- `kotlin.code.style=official` in `gradle.properties`
- No ktlint/detekt configured
- ProGuard disabled for release: `isMinifyEnabled = false`
- `renderscriptSupportModeEnabled = true` in defaultConfig

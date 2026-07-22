# TrackerApp

A personal Android app for one-tap logging of custom lifestyle events (Café, Cola,
Sick, Headache, Energy Drink, Sleep quality, …) via home-screen widgets, with an
in-app overview and offline-safe batch upload to a separate backend.

**For the product spec, feature scope, and the non-negotiable timepoint rules**, see
[`APP_PROJECT.md`](APP_PROJECT.md) — that file is the source of truth for *what* this
app does and *why*. This README is about *how the code is put together*, for anyone
(including future-you) opening the project cold.

This is an addition Zepp/Health Connect Dashboard (need to add to repo and share here) 
allow better insights.

## Requirements

- Android Studio (current stable) with an Android SDK platform matching `compileSdk`
  in `app/build.gradle.kts` (currently API 36) installed
- A device or emulator running **API 26+** (`minSdk 26`, chosen specifically so
  `java.time.Instant` is available natively, no desugaring needed)
- JDK 21 (managed automatically via the Gradle toolchain — see
  `gradle/gradle-daemon-jvm.properties`)

## Building and running

```
.\gradlew.bat assembleDebug     # build only
.\gradlew.bat installDebug      # build + install on all connected/running devices
```

Or open the project in Android Studio and hit Run. The first launch seeds a handful
of default event types (see "Adding a default event type" below) — no manual setup
needed to start tapping.

## Project structure

```
app/src/main/java/com/example/trackerapp/
├── TrackerApplication.kt       Application subclass — builds every singleton below
├── MainActivity.kt             Hosts the in-app screens (Overview / AddEntry / Settings)
│
├── data/
│   ├── db/                     Room: entities, DAOs, database, seed data
│   ├── repository/             EventRepository — the one insert path, used by
│   │                           widgets, Add Entry, and nothing else
│   ├── network/                Ktor client + wire-format DTO for POST /events/bulk
│   └── settings/               DataStore-backed settings (backend URL, last sync time)
│
├── sync/                       WorkManager: the background upload job + its scheduler
│
├── widget/                     Jetpack Glance: the home-screen widget and its
│                               placement/configuration flow
│
└── ui/                         Jetpack Compose: the in-app screens
    ├── overview/                Recent entries, sync status, delete
    ├── addentry/                Manual backdated entry
    ├── settings/                Backend URL
    └── theme/                   Material3 theme wrapper
```

## Architecture

### Layering

```
ui/*  ─┐
       ├─▶ data/repository/EventRepository ─▶ data/db/* (Room)
widget/┘         │
                 └─▶ sync/SyncScheduler (enqueues WorkManager)

sync/SyncWorker ─▶ data/db/* (reads unsynced rows)
                ─▶ data/network/ApiClient (POST /events/bulk)
                ─▶ data/settings/SettingsRepository (backend URL, last-sync time)
```

There's no ViewModel layer and no dependency-injection framework (Hilt, etc.) —
`TrackerApplication` builds a handful of plain singletons (`database`, `repository`,
`settingsRepository`, `apiClient`) as `by lazy` properties, and every Activity/Worker/
Composable reaches them via `(context.applicationContext as TrackerApplication)`.
That's a deliberate choice for a single-developer personal app: it keeps the whole
data flow traceable by just reading code, with nothing to configure or generate.
Compose screens observe Room's `Flow`s directly (`collectAsStateWithLifecycle`)
instead of going through a ViewModel — reasonable at this scale; worth revisiting if
the screens grow real presentation logic.

### The one insert path

Every entry — whether from a widget tap or the in-app Add Entry screen — goes
through exactly one function: `EventRepository.logEvent(typeKey, value, epochMillis,
note)`. It mints the `client_event_id`, inserts the Room row, and enqueues a sync.
Widget taps call it with `Instant.now()` and a button-implied value; Add Entry calls
it with whatever date/time and value the user picked. Neither path duplicates the
other's logic — see [`APP_PROJECT.md`](APP_PROJECT.md#widget-action-routing) for why
that matters for the offline-sync guarantees.

### Widgets (Jetpack Glance)

One `GlanceAppWidget` (`EventWidget`), placed as many times as you like. Each
instance is bound to an event type at placement time by `WidgetConfigurationActivity`
and snapshots everything it needs to render and log — `backendKey`, kind, shape,
label, emoji, color — into its own Glance state (`PreferencesGlanceStateDefinition`).
It does **not** do a live Room lookup on every render: editing or deleting a type
later never breaks an already-placed widget's tap (mirrors the `typeKey`
denormalization on `event_entries` — see `APP_PROJECT.md`).

All three shapes (single button, multi-value buttons, fixed-value button) render
through one `LogEventAction : ActionCallback`, parameterized by `typeKey` + an
optional `value`. The widget is size-responsive (`SizeMode.Responsive`) between a
compact 1×1 layout and a wide 4×1 layout — a multi-button score widget that's been
shrunk to 1×1 can't fit one button per value, so it falls back to a single button
logging the scale's middle value, keeping the "every tap logs something" guarantee
even at that size.

### Sync (WorkManager)

`SyncWorker` is triggered three ways, matching `APP_PROJECT.md`'s sync-behavior
section: on every tap/add (`EventRepository` calls `SyncScheduler.scheduleOneTime`
after every insert/delete), on app open (`MainActivity.onCreate`), and manually (the
refresh icon on the Overview screen). It reads unsynced, non-deleted rows, POSTs them
to `<baseUrl>/events/bulk`, and on success marks them synced and records the sync
time. A `422` response (malformed payload — shouldn't happen given rule #2 in
`APP_PROJECT.md`, but backends can always surprise you) is treated as permanent and
does **not** retry; any other failure (backend unreachable, 5xx, timeout) does, via
WorkManager's exponential backoff.

Soft-deleted entries (see below) are deliberately excluded from what gets uploaded —
propagating *deletes* to the backend isn't implemented yet (no endpoint for it), so
they just sit locally, hidden from the UI, until that lands.

### Delete semantics

A not-yet-synced entry is hard-deleted outright — nothing ever reached the backend.
An already-synced entry is soft-deleted: `deleted = true` (hidden from the UI's
query) and `synced` is flipped back to `false`, so it re-enters the same "unsynced"
queue `SyncWorker` already watches. When backend delete support exists, the worker
can reuse that exact queue instead of needing a second sync-tracking mechanism. Full
rationale in `APP_PROJECT.md`'s "Local data model" section.

### Networking

Cleartext HTTP is allowed in **debug builds only** (`app/src/debug/AndroidManifest.xml`
sets `usesCleartextTraffic="true"`), so the app can hit a plain-HTTP backend on your
dev LAN without a network-security-config tied to one IP that breaks the moment your
laptop's address changes. Release builds keep Android's secure default. Set the
backend base URL from the in-app Settings screen (gear icon on Overview) — e.g.
`http://192.168.1.23:8000` for a LAN backend, or `http://10.0.2.2:<port>` from an
emulator to reach a server running on the host machine.

## Adding a default event type

There's no "manage types" UI yet (planned — see below). Until then, defaults live in
`data/db/SeedEventTypes.kt`. `TrackerApplication.onCreate()` inserts that list on
every launch using `INSERT ... IGNORE` against the unique index on `backendKey`, so
existing types (and any future manual edits to them) are left untouched — only
genuinely new entries in the seed list get added. No migration or data wipe needed to
add one.

## Tech stack

| Concern | Library |
|---|---|
| Language | Kotlin, built-in AGP Kotlin support (no `kotlin-android` plugin — see below) |
| In-app UI | Jetpack Compose + Material 3 |
| Widgets | Jetpack Glance (`glance-appwidget`) |
| Local storage | Room |
| Settings storage | DataStore Preferences |
| Background sync | WorkManager |
| Networking | Ktor client (OkHttp engine) + kotlinx.serialization |

A quirk worth knowing if you touch the Gradle setup: this project uses **AGP 9's
built-in Kotlin support**, so there's no `org.jetbrains.kotlin.android` plugin
applied — Kotlin compilation is built into the Android Gradle plugin itself. Room's
KSP-based code generation and the Compose compiler plugin both still apply normally
on top of that. KSP specifically needs a recent-enough release to know about this
pipeline (older versions error out asking you to disable built-in Kotlin instead) —
see the comment above the `kotlin`/`ksp` entries in `gradle/libs.versions.toml`
before bumping either.

## Open TODOs

- Manage event types from the UI (add/edit/delete) — currently only given
- Change Widgets sizes
- Does not show sync worked yet

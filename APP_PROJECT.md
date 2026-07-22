# Event Tracker — Android App (Project Description)

## What this app is

A personal Android app for **one-tap logging of custom lifestyle events**
(e.g. Café, Cola, feeling sick) via home-screen widgets, with an in-app overview
and **offline-safe batch upload** to my existing backend. It's the "easy tracking"
half of a larger personal health-analysis system; the backend (Python/FastAPI) and
a dashboard already exist in a separate repo.

This app lives in **its own repo**, separate from the Python backend/dashboard.

## Role in the wider system

- The app captures **custom events** and queues them locally on the phone.
- It uploads them in batches to the backend's `POST /events/bulk` whenever the
  backend is reachable.
- **The backend is NOT always on** — it runs only when I sit down to analyze.
  So the app must buffer events on the phone indefinitely and flush
  opportunistically. A gap of days or weeks between syncs is normal and **must be
  lossless**.
- Wearable metrics (Zepp via Health Connect) are handled separately for now.
  Reading Health Connect from this app is a possible later addition — **out of
  scope for v1**.

## Core features (v1)

1. **Manage trackable types.** Create / edit / delete event types: name, emoji/icon,
   color, and a **kind** — `occurrence` (tap logs it, no value) or `score` (tap logs
   an integer on a fixed `scaleMin`–`scaleMax` range, e.g. Sleep 1–5). New types can
   be added anytime.
2. **Home-screen widgets (Jetpack Glance).** One widget provider, placed multiple
   times, each instance bound to a type at placement time via a configuration
   Activity. Every tap stays one-tap:
   - `occurrence` type → single button.
   - `score` type → configuration picks either a **multi-button** layout (one button
     per value in `scaleMin..scaleMax`) or a **single fixed-value** button (always
     logs one chosen value, e.g. a widget that always logs "Sleep = 3").

   All taps route through one Glance `ActionCallback` carrying `typeKey` + an
   optional `value`, so there is exactly one code path that creates an entry (see
   "Widget action routing" below).
3. **Custom-time entries.** An **Add Entry** screen in the main app (reachable from
   the Overview screen), separate from the one-tap widget flow. Pick a type; if its
   kind is `score`, pick a value constrained to that type's scale; pick a date/time
   (defaults to now, editable — this is how backdating works); optional note. On
   submit it goes through the **same repository insert path** as a widget tap (mint
   `client_event_id`, insert into `event_entries`, enqueue sync) — the only
   difference from a widget tap is that the timestamp and value are chosen instead
   of implied by which button was pressed.
4. **Overview.** Recent events grouped by day/type, per-type counts, and sync
   status (unsynced count, last sync time, a manual "Sync now" button).
5. **Offline-safe upload** to the backend, following the timepoint rules below.

## The three rules for event timepoints (non-negotiable)

1. **Capture at tap time, upload later.** `Instant.now()` the moment the widget is
   tapped → into the local queue (Room, as epoch millis). The upload time is
   irrelevant; the tap time is the event.
2. **Serialize with explicit offset.** `Instant.ofEpochMilli(t).toString()` →
   `"2026-07-22T15:04:05Z"` — exactly what the backend accepts. Anything without an
   offset is rejected with **422** (our by-design guard against silent local-time
   shifts; it also survives traveling across timezones).
3. **Mint a `client_event_id` (UUID) at tap time.** This is what makes offline sync
   safe: a timeout mid-upload leaves the app unsure whether the event landed — with
   the key, it just retries, and the server stores each key once.

## Recommended app skeleton

```
tap → Room row {uuid, type, epochMillis, synced=false}
    → WorkManager job (network constraint, backoff):
        POST /events/bulk with unsynced rows → on 2xx mark synced
```

## Widget action routing

Every widget tap — regardless of shape — goes through one Glance `ActionCallback`:

```
LogEventAction : ActionCallback
  params: typeKeyParam: ActionParameters.Key<String>
          valueParam:   ActionParameters.Key<Int>   // absent = null = occurrence
```

`onAction` always does the same four things: read `typeKey` + optional `value` →
`Instant.now()` → insert `{uuid, typeKey, epochMillis, value, synced=false}` into
`event_entries` → enqueue the sync worker. This is the single place event-insert
logic lives, shared by every widget shape and by the in-app Add Entry screen (which
calls the same repository method with an explicit timestamp/value instead of
`Instant.now()`/button-implied value).

## Backend contract (what the app expects)

- Endpoint: `POST /events/bulk`, accepting a JSON array of events.
- Each event: `{ client_event_id (UUID), type (string), timestamp (ISO-8601 with
  offset, e.g. "2026-07-22T15:04:05Z"), source, value?, note? }`.
- Server **dedupes on `client_event_id`** (stores each once) → safe retries and
  safe overlapping batches.
- Server **rejects any timestamp without an explicit offset with 422**.
- On 2xx, the app marks those rows `synced = true`.

> NOTE: the existing backend may need a `/events/bulk` endpoint and a
> `client_event_id` unique column added to match this contract.

## Local data model (Room)

- **event_types**: id, name, `backendKey` (the string sent as `type` on upload),
  emoji/icon, color, **`kind`** (`occurrence` | `score`), **`scaleMin`/`scaleMax`**
  (Int, null unless `kind = score`), enabled, sortOrder, createdAt.
- **event_entries** (the queue): `client_event_id` (UUID, primary key), `typeKey`
  (denormalized string captured at tap time), `epochMillis` (tap time), value?,
  note?, source (e.g. "android"), `synced` (bool), createdAt.
  - Storing `typeKey` denormalized means renaming or deleting a type later never
    corrupts already-queued entries.
  - `value` holds the score for `score`-kind entries; stays null for `occurrence`.
  - `deleted` (bool, default false) + `deletedAt` (epoch millis, nullable): local
    deletion. Not-yet-synced rows are hard-deleted outright — nothing reached the
    backend, nothing to reconcile. Already-synced rows are soft-deleted: `deleted =
    true` (hidden from the UI's recent-entries query) and `synced` is flipped back to
    `false`, so the row re-enters the same "unsynced" queue the sync worker already
    watches. A future delete-propagation feature reuses that existing queue rather
    than needing a second sync-tracking column: for `deleted = true` rows it calls a
    (future) delete endpoint instead of `/events/bulk`, then sets `synced = true`
    again meaning "backend now reflects this row's deleted state."
- **Widget instance state** is not a Room table — each placed widget snapshots its
  own `backendKey`, kind, scale/fixed-value choice, and label/emoji/color into
  Glance's per-widget state at placement time. This mirrors the `typeKey`
  denormalization above: editing or deleting a type later never breaks an
  already-placed widget's tap, though a scale change also won't retroactively
  reshape a widget already on the home screen — remove and re-add it to pick up a
  new scale.

## Tech stack (decided)

- **Kotlin**, `minSdk 26` (for `java.time.Instant`), target the latest stable SDK.
- **Jetpack Compose** — in-app UI (overview, type management, manual add).
- **Jetpack Glance** (`glance-appwidget`, ~1.1.x stable) — home-screen widgets.
- **Room** — local types + event queue.
- **WorkManager** — background sync (network constraint + exponential backoff).
- **Ktor client** (or Retrofit) + **kotlinx.serialization** — HTTP + JSON.
- Configurable backend base URL in settings.

## Sync behavior

- **Triggers:** on tap/add (enqueue WorkManager), on app open, and a manual
  "Sync now".
- Because the backend isn't always up, expect POST failures (connection refused)
  when it's down. WorkManager retries with backoff, and **app-open + manual sync
  are the primary catch-up triggers** for long gaps (WorkManager backoff caps out
  over very long offline periods).
- Idempotent uploads via `client_event_id` make re-sends and overlapping batches
  harmless.

## Networking note (local dev)

- Point the app at the laptop's **LAN IP** (same wifi) or a deployed backend.
- Android **blocks cleartext HTTP by default** — add a network security config
  allowing the dev IP, or use HTTPS. This is the most common "won't connect"
  cause.

## Out of scope for v1

- Reading Health Connect / wearable metrics from this app.
- Editing already-synced entries.
- **Propagating deletes to the backend** — deleting an already-synced entry hides it
  locally (soft-delete, see Local data model) but the backend still has it until a
  delete endpoint exists there and the sync worker is taught to call it.
- iOS.

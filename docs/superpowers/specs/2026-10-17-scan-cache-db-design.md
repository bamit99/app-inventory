# Scan + Room cache — Design

**Date:** 2026-10-17
**Status:** approved (conversational design) — awaiting written-spec review
**Supersedes/extends:** `2026-10-17-app-inventory-design.md` (which specified "no database")

## Purpose

The app currently reads the live system APIs on every open. That scan is heavy (~300 apps × synchronous usage + storage Binder calls) and already caused a real ANR on-device. Fix: a **scan button** runs the heavy scan off the main thread and stores the result in a **Room database**. The list always reads from the DB, so opening the app is instant and never blocks on live queries.

## Decisions (user-confirmed)

- **DB purpose:** fast cache of the latest scan (one current row per app; a scan overwrites).
- **Engine:** Room (adds KSP annotation processing).
- **Trigger:** manual scan only. The app never scans on open. Before the first scan, an empty state prompts to scan.
- **Scope:** store the full `AppEntry` inventory. No scan history, no auto-scan, no migrations beyond v1.

## Architecture

The existing live sources stay the **scan source**; the Room DB becomes the **read source**.

- **Scan source (unchanged):** `PackageListSource` / `UsageSource` / `StorageSource` (real Android impls) → `AppInventoryRepository.load()`.
- **Read source (new):** `InventoryStore` interface, `RoomInventoryStore` implementation backed by Room.
- **Flow control:** `AppViewModel.scan()` runs `repo.load()` on `Dispatchers.IO`, saves via the store, then re-reads the store into UI state. On open, the ViewModel loads from the store only.

Pure logic (`Rules`, `Summary`, `SortSearch`, `CsvExport`) is unchanged — it operates on `List<AppEntry>` and does not care where the list came from.

## Components

- **`AppEntryEntity`** — Room `@Entity(tableName = "apps")`. Fields: `packageName` (primary key), `label`, `installTime`, `lastUpdateTime`, `launchCount`, `lastLaunch`, `storageBytes`, `cacheBytes`, `removable`. Mirrors `AppEntry` (`-1` unknown convention preserved as-is).
- **`AppDao`** — `@Query("SELECT * FROM apps")` load; `@Insert(onConflict = REPLACE)`; `@Query("DELETE FROM apps")` clear. A scan clears then inserts (full replace).
- **`AppDatabase`** — Room DB singleton (companion `get(context)`).
- **`InventoryStore`** — interface `{ suspend fun load(): List<AppEntry>; suspend fun save(list: List<AppEntry>) }`. Both are `suspend` so Room's suspend DAO functions run on its IO executor (never the main thread). `RoomInventoryStore(dao)` implements it; a `FakeInventoryStore` (constructor-injected list) lives in test sources for JVM tests.
- **`AppViewModel`** — gains `scan()` (suspend, IO) and a `scanning: Boolean` state flag; on construction/open it loads from the store. `save()` is suspend and does its DB write on IO.
- **`MainActivity`** — wires the real `RoomInventoryStore` into the `AppViewModel`; adds the **Scan** button (top bar) with a scanning state; unchanged uninstall/export.

## Data flow

1. **Open** → `store.load()` → `List<AppEntry>` → existing sort/filter/summary render. If empty, show empty state "Tap Scan".
2. **Tap Scan** → button shows "Scanning…" (disabled) → `repo.load()` on IO → `store.save(all)` (IO) → re-read `store.load()` → list + summary update.
3. All existing behavior (badges, kill rule, storage bar, sort, search, filter, uninstall, CSV export, CSV reads current `state.all`) continues to work unchanged.

## Scan UX

- A **Scan** button in the top app bar.
- While scanning: label "Scanning…", button disabled.
- Empty DB → list shows a "Tap Scan to scan your apps" empty state.
- Errors during scan: surface a toast/snackbar; leave the previous list intact.

## Testing

- Existing 18 JVM unit tests + 6 instrumented tests unchanged and still green.
- New JVM tests (fakes, no Android):
  - ViewModel loads from `FakeInventoryStore` on open (list reflects cached rows).
  - `scan()` saves the scanned list to the store and the list reflects it.
  - Empty store → empty list (empty state path), no crash.
- New thin instrumented smoke test for `RoomInventoryStore` (save then load round-trips). Instrumented runs have the known ColorOS/UTP flakiness; verify via `TestRunner` logcat line.

## Error handling

- DB write/read failures propagate as exceptions from the store; the ViewModel's `scan()` catches them so the UI shows an error and keeps the prior list.
- Unknown values stay `-1`; the DB round-trips them as-is (no `-1`↔NULL conversion needed since the column type is nullable/Int64 sentinel-based).

## Not in scope

Scan history, auto-scan on open/stale cache, per-app time-series, DB schema migrations beyond v1, sync, export of the DB itself.
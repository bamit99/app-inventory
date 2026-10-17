# App Inventory — Design Spec

Date: 2026-10-17
Status: Draft for review

## Purpose

A small Android app that shows every user-installed application with the data that
makes uninstall decisions easy: install date, last launch, total launches, and storage
used. A summary strip at the top surfaces the cleanup signal (never opened, kill
candidates, reclaimable cache). Each row has its own uninstall button.

Primary use: a personal cleanup assistant and usage dashboard with equal weight.

## Scope

- Shows **user-installed (third-party) apps only**. System/OEM packages are out of scope.
- Single screen. Single Activity. No accounts, no network, no analytics.
- Runs entirely on-device. No root required.

## Data sources (all on-device)

| Field | Source | Cost |
|-------|--------|------|
| App list | `PackageManager`, third-party flag off | free |
| Install date | `PackageInfo.firstInstallTime` | free |
| Last update | `PackageInfo.lastUpdateTime` | free |
| Launch count | `UsageStatsManager` (window: install → now) | Usage Access |
| Last launch | `UsageStatsManager` (last event per package) | Usage Access |
| Storage used | `StorageStatsManager` | Usage Access |
| Cache size | `StorageStatsManager` | Usage Access |
| Uninstall | `ACTION_DELETE` system prompt | free |

**Caveat (accepted):** on-device `UsageStats` retention is shorter than what `dumpsys`
exposed. Launch counts read live may be lower than the historical CSV numbers. This is a
system limit and cannot be fixed.

## Permissions

- **Usage Access** (Settings → Security → Usage Access). One grant unlocks launch count,
  last launch, storage, and cache. Without it, the app still lists apps and install dates;
  launch/storage/cache fields are blank.
- No normal runtime permissions needed. Uninstall uses the system prompt, not a permission.

## UI

- **Layout:** summary-first. Summary cards on top, app list below.
- **Summary cards (top):** Total · Never opened · Kill candidates · Reclaimable cache.
  Tapping a card filters the list to that set.
- **Row (card style):** app name, storage bar, install date, last launch, launch count,
  and an Uninstall button.
- **Storage bar:** scaled relative to the largest app on the phone.
- **Badges:** `never` (0 launches since install) and `stale` (kill rule below).
- **Sort/search:** tap any column header to sort. Search box at top. Default sort: kill
  candidates first, then oldest last-launch.

## Kill-candidate rule

An app is a kill candidate if:

- 0 launches since install, **OR**
- fewer than 5 launches AND last launch more than 90 days ago.

## Reclaimable cache

"Reclaimable" counts **cache only** (the safe, always-reclaimable figure). It is the sum of
cache sizes across the current list.

## Behavior

- First launch: if Usage Access not granted, show a "Grant Usage Access" card that opens
  the system Settings page. Re-check on resume.
- Uninstall: tap a row's Uninstall button → system uninstall prompt for that package. The
  app cannot uninstall silently.
- Export: button produces a CSV with the same columns as the list.

## Tech

- Kotlin + Jetpack Compose, single Activity.
- Minimum API 26 (Android 8.0) — required for `StorageStats` and reliable Usage Access.
- No database. All data is live system queries, refreshed on screen entry/resume.
- Target device: the user's OnePlus (ColorOS), recent Android.

## Success criteria

- List count matches `pm list packages -3`.
- Install date exact for every app.
- After grant: launch count, last launch, storage, cache populate.
- Kill-candidate and never-opened counts follow the rules above.
- Uninstall fires the system prompt for the tapped app.
- Export writes a CSV matching the on-screen columns.

## Out of scope (v1)

- System/OEM app management.
- Batch / multi-select uninstall (per-row only).
- Launch history timeline.
- Sorting saved across sessions.
- Themes beyond system light/dark.

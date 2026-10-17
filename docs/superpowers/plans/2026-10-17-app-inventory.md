# App Inventory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single-screen Android app that lists every user-installed app with install date, last launch, launch count, storage, and cache, a summary strip that surfaces cleanup signals, and a per-row uninstall button.

**Architecture:** Compose single-Activity app. Three data-source interfaces (packages, usage, storage) feed a repository that assembles a flat `AppEntry` list. Pure Kotlin logic (kill rule, badges, summary, sort/search/filter, CSV) is JVM-unit-tested; the real system sources are behind the interfaces and get a thin instrumented smoke test. A ViewModel holds list state (sort, search, filter) and exposes UI state.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), AndroidX, JUnit4 (JVM unit tests), Gradle (Kotlin DSL). Min API 26.

**Spec:** `docs/superpowers/specs/2026-10-17-app-inventory-design.md`

## Global Constraints

- Minimum API 26 (Android 8.0).
- User-installed (third-party) apps only; system/OEM out of scope.
- No database. Data is live system queries, refreshed on screen entry/resume.
- Usage Access grant unlocks launch count, last launch, storage, cache; without it those fields are blank (`-1`).
- Unknown numeric values are `-1` (never `0` — `0` means genuinely zero).
- Kill rule: 0 launches since install, OR fewer than 5 launches AND last launch older than 90 days.
- Reclaimable counts cache only.
- Storage bar scaled relative to the largest app on the phone.
- Uninstall uses the `ACTION_DELETE` system prompt; never silent.
- Per-row uninstall only; no batch/multi-select in v1.

## Review Focus

These are the failure modes the spec implies that the pure-logic tests do not fully cover. Each line names the input, the expected behavior, and the task whose tests pin it.

- **Usage data unavailable (no grant):** expected — list and install dates still render; launch/storage/cache show blank, not `0` or crash. Pinned by Task 8 (no-grant) and Task 10 (row renders `-1` as blank).
- **Unknown vs zero:** expected — a `0`-launch app shows `never`; a `-1` app shows blank and is NOT a kill candidate. Pinned by Task 3 (tests both).
- **Empty list:** expected — summary shows zeros, list shows empty state, no divide-by-zero on storage bar. Pinned by Task 4 and Task 5.
- **Storage bar max = 0:** expected — when all storage is `-1`/`0`, bars render empty, not NaN/crash. Pinned by Task 10.
- **CSV with unknown fields:** expected — `-1` fields export as empty cells, not `-1`. Pinned by Task 6.

---

## File Structure

```
app/
  src/main/java/com/example/appinventory/
    data/
      AppEntry.kt                 (shared model)
      PackageListSource.kt        (interface + real impl)
      UsageSource.kt              (interface + real impl)
      StorageSource.kt            (interface + real impl)
      AppInventoryRepository.kt   (assembles AppEntry list)
    logic/
      Rules.kt                    (kill rule + badges, pure)
      Summary.kt                  (aggregate counts, pure)
      SortSearch.kt               (ordering + filter, pure)
      CsvExport.kt                (CSV writer, pure)
    ui/
      AppViewModel.kt             (state holder)
      InventoryScreen.kt          (summary cards + list)
      AppRow.kt                   (single row)
      PermissionGate.kt           (usage-access card)
      MainActivity.kt
  src/test/java/com/example/appinventory/   (JVM unit tests)
  src/androidTest/java/com/example/appinventory/  (instrumented smoke)
```

Pure logic (`logic/`) has no Android imports — testable on the JVM. Data-source interfaces are Android-bound; JVM tests use fakes.

---

## Task 1: Project scaffolding + build sanity

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/example/appinventory/ui/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/example/appinventory/BuildSanityTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: a buildable Compose app; `MainActivity` shows a placeholder `Text("App Inventory")`.

- [ ] **Step 1: Create Gradle scaffold.** Root + app `build.gradle.kts` with Compose BOM, Material 3, `minSdk 26`, `compileSdk 35`, JUnit4 test deps, `testImplementation("junit:junit:4.13.2")`.
- [ ] **Step 2: Write failing sanity test.**

```kotlin
package com.example.appinventory
import org.junit.Test
import kotlin.test.assertTrue
class BuildSanityTest { @Test fun truth() { assertTrue(true) } }
```

- [ ] **Step 3: Run test, confirm it runs.** `./gradlew :app:testDebugUnitTest --tests "*BuildSanityTest"` → PASS (proves toolchain + test wiring).
- [ ] **Step 4: Add `MainActivity` placeholder + manifest + strings.** Manifest declares `PACKAGE_USAGE_STATS` permission (`<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" android:summary="Usage access" android:protectionLevel="signature|appops" />`).
- [ ] **Step 5: Build the APK.** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 6: Commit.** `git add -A && git commit -m "chore: scaffold Compose app"`

---

## Task 2: Data model + source interfaces + fakes

**Files:**
- Create: `data/AppEntry.kt`, `data/PackageListSource.kt`, `data/UsageSource.kt`, `data/StorageSource.kt`
- Create: `app/src/test/java/com/example/appinventory/Fakes.kt`
- Test: `app/src/test/java/com/example/appinventory/ModelTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `data class AppEntry(packageName: String, label: String, installTime: Long, lastUpdateTime: Long, launchCount: Long, lastLaunch: Long, storageBytes: Long, cacheBytes: Long, removable: Boolean)`
  - `interface PackageListSource { fun list(): List<AppEntry> }` — returns entries with usage/storage fields left `-1`.
  - `interface UsageSource { fun launchCount(pkg: String, since: Long): Long; fun lastLaunch(pkg: String, since: Long): Long }`
  - `interface StorageSource { fun storageBytes(pkg: String): Long; fun cacheBytes(pkg: String): Long }`
  - Fakes: `FakePackageListSource`, `FakeUsageSource`, `FakeStorageSource` (constructor-injected maps).

- [ ] **Step 1: Write failing test.**

```kotlin
class ModelTest {
  @Test fun entryDefaultsUnknownToMinusOne() {
    val e = AppEntry("x","X",0,0,-1,-1,-1,-1,true)
    assertEquals(-1L, e.launchCount); assertEquals(-1L, e.lastLaunch)
  }
}
```

- [ ] **Step 2: Run — FAIL (class missing).** `./gradlew :app:testDebugUnitTest --tests "*ModelTest"`
- [ ] **Step 3: Implement `AppEntry` + three interfaces + `Fakes.kt`.** Fakes take `Map<String,Long>` for usage/storage; `FakePackageListSource` takes a fixed list.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: data model and source interfaces"`

---

## Task 3: Kill rule + badges (pure logic)

**Files:**
- Create: `logic/Rules.kt`
- Test: `app/src/test/java/com/example/appinventory/RulesTest.kt`

**Interfaces:**
- Consumes: `AppEntry`.
- Produces:
  - `object Rules { const val MAX_LAUNCHES = 5L; const val STALE_DAYS = 90L; fun isKillCandidate(e: AppEntry, now: Long): Boolean; fun badge(e: AppEntry, now: Long): Badge }`
  - `enum class Badge { NONE, NEVER, STALE }`

- [ ] **Step 1: Write failing tests.**

```kotlin
class RulesTest {
  private val now = 1_700_000_000_000L
  private val day = 86_400_000L
  @Test fun zeroLaunchIsKill() {
    val e = AppEntry("a","A",0,0,0,-1,0,0,true)
    assertTrue(Rules.isKillCandidate(e, now))
    assertEquals(Badge.NEVER, Rules.badge(e, now))
  }
  @Test fun fewLaunchesLongAgoIsKill() {
    val e = AppEntry("b","B",0,0,4, now - 91*day, 0,0,true)
    assertTrue(Rules.isKillCandidate(e, now))
    assertEquals(Badge.STALE, Rules.badge(e, now))
  }
  @Test fun unknownLaunchNotKill() {
    val e = AppEntry("c","C",0,0,-1,-1,0,0,true)   // usage unavailable
    assertFalse(Rules.isKillCandidate(e, now))
    assertEquals(Badge.NONE, Rules.badge(e, now))
  }
  @Test fun activeAppNotKill() {
    val e = AppEntry("d","D",0,0,100, now - day, 0,0,true)
    assertFalse(Rules.isKillCandidate(e, now))
  }
}
```

- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement `Rules.kt`.** `isKillCandidate`: `launchCount == 0L || (launchCount in 1 until MAX_LAUNCHES && lastLaunch >= 0 && now - lastLaunch > STALE_DAYS*day)`. Unknown (`-1`) launchCount → not a candidate.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: kill rule and badges"`

---

## Task 4: Summary aggregate (pure logic)

**Files:**
- Create: `logic/Summary.kt`
- Test: `app/src/test/java/com/example/appinventory/SummaryTest.kt`

**Interfaces:**
- Consumes: `AppEntry`, `Rules`.
- Produces: `data class SummaryCounts(total:Int, neverOpened:Int, killCandidates:Int, reclaimableCacheBytes:Long)`; `object Summary { fun compute(list: List<AppEntry>, now: Long): SummaryCounts }`

- [ ] **Step 1: Write failing tests.**

```kotlin
class SummaryTest {
  private val now = 1_700_000_000_000L
  @Test fun emptyListIsAllZeros() {
    val s = Summary.compute(emptyList(), now)
    assertEquals(0, s.total); assertEquals(0L, s.reclaimableCacheBytes)
  }
  @Test fun countsAndCache() {
    val never = AppEntry("a","A",0,0,0,-1,100,50,true)
    val known = AppEntry("b","B",0,0,100,now,200,30,true)
    val unknown = AppEntry("c","C",0,0,-1,-1,-1,-1,true) // cache unknown -> excluded
    val s = Summary.compute(listOf(never,known,unknown), now)
    assertEquals(3, s.total); assertEquals(1, s.neverOpened)
    assertEquals(1, s.killCandidates); assertEquals(80L, s.reclaimableCacheBytes)
  }
}
```

- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement `Summary.kt`.** `reclaimableCacheBytes` sums `cacheBytes` where `>= 0`. `neverOpened` counts `launchCount == 0`. `killCandidates` via `Rules.isKillCandidate`.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: summary aggregate"`

---

## Task 5: Sort, search, filter (pure logic)

**Files:**
- Create: `logic/SortSearch.kt`
- Test: `app/src/test/java/com/example/appinventory/SortSearchTest.kt`

**Interfaces:**
- Consumes: `AppEntry`, `Rules`.
- Produces:
  - `enum class SortColumn { LAST_LAUNCH, INSTALL, LAUNCH_COUNT, STORAGE }`
  - `enum class FilterCard { ALL, NEVER_OPENED, KILL_CANDIDATES, RECLAIMABLE }`
  - `object SortSearch { fun apply(list: List<AppEntry>, sort: SortColumn, query: String, filter: FilterCard, now: Long): List<AppEntry> }`
  - Default ordering (sort = LAST_LAUNCH): kill candidates first, then oldest last-launch; `-1` (unknown) last.

- [ ] **Step 1: Write failing tests.**

```kotlin
class SortSearchTest {
  private val now = 1_700_000_000_000L
  private val day = 86_400_000L
  private val kill = AppEntry("k","K",0,0,0,-1,100,50,true)
  private val active = AppEntry("a","A",0,0,10,now,100,10,true)
  private val stale = AppEntry("s","S",0,0,4,now-91*day,100,10,true)
  @Test fun defaultPutsKillFirstThenOldest() {
    val out = SortSearch.apply(listOf(active,kill,stale), SortColumn.LAST_LAUNCH, "", FilterCard.ALL, now)
    assertEquals(listOf("k","s","a"), out.map{it.packageName})
  }
  @Test fun filterKillOnly() {
    val out = SortSearch.apply(listOf(active,kill,stale), SortColumn.LAST_LAUNCH, "", FilterCard.KILL_CANDIDATES, now)
    assertEquals(listOf("k","s"), out.map{it.packageName})
  }
  @Test fun searchMatchesLabel() {
    val out = SortSearch.apply(listOf(active,kill), SortColumn.LAST_LAUNCH, "act", FilterCard.ALL, now)
    assertEquals(listOf("a"), out.map{it.packageName})
  }
  @Test fun emptySafe() { assertTrue(SortSearch.apply(emptyList(), SortColumn.STORAGE, "", FilterCard.ALL, now).isEmpty()) }
}
```

- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement `SortSearch.kt`.** Filter first, then sort. Default: `partition(isKillCandidate)` then within each group by sort column, `-1` sorted last. Other sort columns sort by that column, `-1` last. Search = case-insensitive `label.contains(query)`.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: sort search filter"`

---

## Task 6: CSV export (pure logic)

**Files:**
- Create: `logic/CsvExport.kt`
- Test: `app/src/test/java/com/example/appinventory/CsvExportTest.kt`

**Interfaces:**
- Consumes: `AppEntry`.
- Produces: `object CsvExport { val HEADER: List<String>; fun rows(list: List<AppEntry>): List<List<String>>; fun write(list: List<AppEntry>): String }` — columns: label, package, install, lastLaunch, launchCount, storageBytes, cacheBytes.

- [ ] **Step 1: Write failing tests.**

```kotlin
class CsvExportTest {
  @Test fun headerMatchesColumns() {
    assertEquals(listOf("label","package","install","lastLaunch","launchCount","storageBytes","cacheBytes"), CsvExport.HEADER)
  }
  @Test fun unknownFieldsExportEmpty() {
    val e = AppEntry("pkg","Label",0,0,-1,-1,-1,-1,true)
    val row = CsvExport.rows(listOf(e)).single()
    assertEquals("", row[3]); assertEquals("", row[4]); assertEquals("", row[5]); assertEquals("", row[6])
  }
  @Test fun writeHasHeaderPlusRows() {
    val csv = CsvExport.write(listOf(AppEntry("p","L",0,0,5,10,20,3,true)))
    assertEquals(CsvExport.HEADER.size, csv.lineSequence().first().split(",").size)
    assertEquals(2, csv.trim().lines().size)
  }
}
```

- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement `CsvExport.kt`.** `-1` numeric → `""`. Quote fields containing commas. Header row + data rows.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: csv export"`

---

## Task 7: Repository (assembles list from sources)

**Files:**
- Create: `data/AppInventoryRepository.kt`
- Test: `app/src/test/java/com/example/appinventory/RepositoryTest.kt`

**Interfaces:**
- Consumes: `PackageListSource`, `UsageSource`, `StorageSource`, `AppEntry`, `Fakes`.
- Produces: `class AppInventoryRepository(pkg: PackageListSource, usage: UsageSource, storage: StorageSource) { fun load(): List<AppEntry> }` — for each package from `pkg.list()`, fills launchCount/lastLaunch (window = installTime → now) and storageBytes/cacheBytes from sources.

- [ ] **Step 1: Write failing test.**

```kotlin
class RepositoryTest {
  @Test fun mergesAllSources() {
    val base = AppEntry("com.x","X", 1000, 1000, -1, -1, -1, -1, true)
    val pkg = FakePackageListSource(listOf(base))
    val usage = FakeUsageSource(mapOf("com.x" to 42L), mapOf("com.x" to 9999L))
    val storage = FakeStorageSource(mapOf("com.x" to 500L), mapOf("com.x" to 100L))
    val e = AppInventoryRepository(pkg, usage, storage).load().single()
    assertEquals(42L, e.launchCount); assertEquals(9999L, e.lastLaunch)
    assertEquals(500L, e.storageBytes); assertEquals(100L, e.cacheBytes)
  }
  @Test fun missingUsageStaysUnknown() {
    val base = AppEntry("com.y","Y", 1000, 1000, -1,-1,-1,-1, true)
    val pkg = FakePackageListSource(listOf(base))
    val e = AppInventoryRepository(pkg, FakeUsageSource(emptyMap(), emptyMap()), FakeStorageSource(emptyMap(), emptyMap())).load().single()
    assertEquals(-1L, e.launchCount); assertEquals(-1L, e.cacheBytes)
  }
}
```

- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement `AppInventoryRepository.kt`.** Copy each base entry, fill fields from sources, keep `-1` when source returns `-1`.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: repository assembly"`

---

## Task 8: Real system data sources + permission check

**Files:**
- Modify: `data/PackageListSource.kt` (add `AndroidPackageListSource`), `data/UsageSource.kt` (add `AndroidUsageSource`), `data/StorageSource.kt` (add `AndroidStorageSource`)
- Create: `data/UsageAccess.kt` (`fun hasUsageAccess(ctx): Boolean`)
- Test: `app/src/androidTest/java/com/example/appinventory/SystemSourcesSmokeTest.kt`

**Interfaces:**
- Consumes: Android `Context`.
- Produces: `AndroidPackageListSource(ctx)`, `AndroidUsageSource(ctx)`, `AndroidStorageSource(ctx)` implementing the Task 2 interfaces; `UsageAccess.hasUsageAccess(ctx): Boolean`.

- [ ] **Step 1: Write failing instrumented test.**

```kotlin
class SystemSourcesSmokeTest {
  @Test fun listsThirdPartyPackages() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val list = AndroidPackageListSource(ctx).list()
    assertTrue(list.isNotEmpty())          // dev phone has user apps
    assertTrue(list.all { it.packageName.isNotEmpty() })
  }
  @Test fun permissionCheckReturnsBoolean() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    UsageAccess.hasUsageAccess(ctx)        // no crash, returns Boolean
  }
}
```

- [ ] **Step 2: Run on device/emulator — FAIL (classes missing).** `./gradlew :app:connectedDebugAndroidTest`
- [ ] **Step 3: Implement the three Android sources + `UsageAccess`.** `PackageManager.getInstalledPackages(0)` filtered by `(flags and FLAG_SYSTEM)==0`; `QUERY_ALL_PACKAGES` declared. `UsageStatsManager.queryUsageStats(INTERVAL_BEG, installTime, now)` for last event; launch count = count of `ACTIVITY_RESUMED` events. `StorageStatsManager.queryStatsForPackage(volumeUuid, pkg, userId)`.
- [ ] **Step 4: Run on device — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: android system sources"`

---

## Task 9: ViewModel (state + wiring)

**Files:**
- Create: `ui/AppViewModel.kt`
- Test: `app/src/test/java/com/example/appinventory/ViewModelTest.kt`

**Interfaces:**
- Consumes: `AppInventoryRepository`, `Rules`, `Summary`, `SortSearch`, `CsvExport`.
- Produces: `class AppViewModel(repo: AppInventoryRepository, nowProvider: () -> Long) { val state: StateFlow<UiState>; fun setSort(c); fun setSearch(q); fun setFilter(f); fun refresh(); fun exportCsv(): String }`; `data class UiState(all: List<AppEntry>, visible: List<AppEntry>, counts: SummaryCounts, sort: SortColumn, query: String, filter: FilterCard)`.

- [ ] **Step 1: Write failing test.**

```kotlin
class ViewModelTest {
  private val now = 1_700_000_000_000L
  @Test fun stateReflectsRepoAndLogic() = runBlocking {
    val base = listOf(AppEntry("com.k","K",0,0,0,-1,100,50,true))
    val repo = AppInventoryRepository(FakePackageListSource(base), FakeUsageSource(emptyMap(),emptyMap()), FakeStorageSource(mapOf("com.k" to 100L), mapOf("com.k" to 50L)))
    val vm = AppViewModel(repo) { now }
    vm.refresh()
    val s = vm.state.value
    assertEquals(1, s.counts.total); assertEquals(1, s.counts.killCandidates)
    assertEquals("com.k", s.visible.first().packageName)
  }
}
```

- [ ] **Step 2: Run — FAIL.**
- [ ] **Step 3: Implement `AppViewModel.kt`.** `refresh()` loads repo, computes counts, applies default sort. `setSort/setSearch/setFilter` recompute `visible` via `SortSearch`. `exportCsv` delegates to `CsvExport.write(state.value.all)`.
- [ ] **Step 4: Run — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: viewmodel state wiring"`

---

## Task 10: UI — summary cards, list, rows, uninstall

**Files:**
- Create: `ui/InventoryScreen.kt`, `ui/AppRow.kt`
- Modify: `ui/MainActivity.kt` (show `InventoryScreen`)
- Test: `app/src/androidTest/java/com/example/appinventory/UiTest.kt`

**Interfaces:**
- Consumes: `AppViewModel`, `UiState`, `Badge`, `SortSearch`.
- Produces: `@Composable fun InventoryScreen(vm: AppViewModel, onUninstall: (AppEntry) -> Unit)`; `@Composable fun AppRow(e: AppEntry, maxStorage: Long, badge: Badge, onUninstall: () -> Unit)`.

- [ ] **Step 1: Write failing UI test (Compose test).**

```kotlin
class UiTest {
  @Test fun rendersSummaryAndRow() {
    val vm = AppViewModel(FakeRepoWithOneKillApp()) { System.currentTimeMillis() }
    composeTestRule.setContent { InventoryScreen(vm) { } }
    composeTestRule.onNodeWithText("Kill candidates").assertExists()
    composeTestRule.onNodeWithText("K").assertExists()
  }
  @Test fun unknownFieldShowsBlank() {
    val vm = AppViewModel(FakeRepoWithUnknownUsage()) { System.currentTimeMillis() }
    composeTestRule.setContent { InventoryScreen(vm) { } }
    composeTestRule.onNodeWithText("-1").assertDoesNotExist()   // blanks, not -1
  }
}
```

- [ ] **Step 2: Run on device — FAIL.**
- [ ] **Step 3: Implement `AppRow` + `InventoryScreen`.** Row: name, storage bar (width = `storageBytes / maxStorage`, `maxStorage = max(all storage, 1)`; `-1` → empty bar), install date, last launch, launch count, Uninstall button. Format `-1` as blank. Summary cards row: Total / Never opened / Kill candidates / Reclaimable cache; each tappable → `vm.setFilter(...)`. Column-header sort. Search box.
- [ ] **Step 4: Run on device — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: summary cards list rows"`

---

## Task 11: Permission gate + uninstall + export wiring

**Files:**
- Create: `ui/PermissionGate.kt`
- Modify: `ui/MainActivity.kt`
- Test: `app/src/androidTest/java/com/example/appinventory/PermissionGateTest.kt`

**Interfaces:**
- Consumes: `UsageAccess`, `AppViewModel`, `AppEntry`.
- Produces: `@Composable fun PermissionGate(hasAccess: Boolean, onGrant: () -> Unit, content: @Composable () -> Unit)`; uninstall via `ACTION_DELETE`; export writes CSV to app external files dir.

- [ ] **Step 1: Write failing test.**

```kotlin
class PermissionGateTest {
  @Test fun showsGrantWhenNoAccess() {
    composeTestRule.setContent { PermissionGate(false, {}) { Text("hidden") } }
    composeTestRule.onNodeWithText("Grant Usage Access").assertExists()
    composeTestRule.onNodeWithText("hidden").assertDoesNotExist()
  }
  @Test fun showsContentWhenGranted() {
    composeTestRule.setContent { PermissionGate(true, {}) { Text("list") } }
    composeTestRule.onNodeWithText("list").assertExists()
  }
}
```

- [ ] **Step 2: Run on device — FAIL.**
- [ ] **Step 3: Implement `PermissionGate`, uninstall launcher, export.** `onGrant` opens `Settings.ACTION_USAGE_ACCESS_SETTINGS`. Uninstall: `startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS ...))` or `Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))`. Export: write `vm.exportCsv()` to `getExternalFilesDir(null)/apps.csv`, toast path.
- [ ] **Step 4: Run on device — PASS.**
- [ ] **Step 5: Commit.** `git commit -am "feat: permission gate uninstall export"`

---

## Self-Review

**1. Spec coverage:** Purpose → Tasks 1–11. Scope (user apps only) → Task 8 filter. Data sources → Tasks 2, 7, 8. Permissions → Tasks 1, 8, 11. UI (summary-first, card rows, storage bar, badges, sort/search) → Tasks 3–6, 10. Kill rule → Task 3. Reclaimable cache → Task 4. Tech/min API → Task 1. Success criteria → each task's tests. No gaps.

**2. Placeholder scan:** No TBD/TODO. Every code step has real code.

**3. Type consistency:** `AppEntry` fields identical across Tasks 2/3/4/5/6/7/9/10. `Rules.isKillCandidate`, `Badge`, `SummaryCounts`, `SortColumn`, `FilterCard`, `UiState` names match everywhere used. `AppViewModel` signatures consistent between Task 9 and Tasks 10/11.

**4. Review Focus:** each line's test lives in the owning task (Task 8 no-grant, Task 3 unknown-vs-zero, Tasks 4/5 empty, Task 10 blank/max, Task 6 CSV unknowns). Covered.

package com.example.appinventory

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import com.example.appinventory.data.AppEntry
import com.example.appinventory.data.AppInventoryRepository
import com.example.appinventory.ui.AppViewModel

class ViewModelTest {
    private val now = 1_700_000_000_000L
    @Test fun stateReflectsRepoAndLogic() = runBlocking {
        val base = listOf(AppEntry("com.k", "K", 0, 0, 0, -1, 100, 50, true))
        val repo = AppInventoryRepository(FakePackageListSource(base), FakeUsageSource(emptyMap(), emptyMap()), FakeStorageSource(mapOf("com.k" to 100L), mapOf("com.k" to 50L)))
        val vm = AppViewModel(repo) { now }
        vm.refresh()
        val s = vm.state.value
        assertEquals(1, s.counts.total); assertEquals(1, s.counts.killCandidates)
        assertEquals("com.k", s.visible.first().packageName)
    }
}

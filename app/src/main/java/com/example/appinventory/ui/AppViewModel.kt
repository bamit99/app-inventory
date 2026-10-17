package com.example.appinventory.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.appinventory.data.AppEntry
import com.example.appinventory.data.AppInventoryRepository
import com.example.appinventory.logic.CsvExport
import com.example.appinventory.logic.FilterCard
import com.example.appinventory.logic.SortColumn
import com.example.appinventory.logic.SortSearch
import com.example.appinventory.logic.Summary
import com.example.appinventory.logic.SummaryCounts

data class UiState(
    val all: List<AppEntry>,
    val visible: List<AppEntry>,
    val counts: SummaryCounts,
    val sort: SortColumn,
    val query: String,
    val filter: FilterCard,
)

class AppViewModel(
    private val repo: AppInventoryRepository,
    private val nowProvider: () -> Long,
) {
    private val _state = MutableStateFlow(
        UiState(emptyList(), emptyList(), SummaryCounts(0, 0, 0, 0L), SortColumn.LAST_LAUNCH, "", FilterCard.ALL)
    )
    val state: StateFlow<UiState> = _state

    private fun recompute(all: List<AppEntry>, sort: SortColumn, query: String, filter: FilterCard) {
        val now = nowProvider()
        _state.value = UiState(
            all = all,
            visible = SortSearch.apply(all, sort, query, filter, now),
            counts = Summary.compute(all, now),
            sort = sort,
            query = query,
            filter = filter,
        )
    }

    fun refresh() = recompute(repo.load(), _state.value.sort, _state.value.query, _state.value.filter)
    fun setSort(c: SortColumn) = recompute(_state.value.all, c, _state.value.query, _state.value.filter)
    fun setSearch(q: String) = recompute(_state.value.all, _state.value.sort, q, _state.value.filter)
    fun setFilter(f: FilterCard) = recompute(_state.value.all, _state.value.sort, _state.value.query, f)
    fun exportCsv(): String = CsvExport.write(_state.value.all)
}

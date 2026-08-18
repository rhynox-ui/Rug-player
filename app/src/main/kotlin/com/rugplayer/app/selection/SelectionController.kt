package com.rugplayer.app.selection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Multi-select state shared across every screen. Picking a few videos in
 * one folder, backing out, opening a different folder, and picking more
 * there all add up to one selection instead of resetting on navigation —
 * each screen only owns its own remembered UI (scroll position etc.), not
 * which videos are selected.
 */
class SelectionController {
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun toggle(id: Long) {
        _selectedIds.update { if (id in it) it - id else it + id }
    }

    fun select(id: Long) {
        _selectedIds.update { it + id }
    }

    fun remove(ids: Collection<Long>) {
        _selectedIds.update { it - ids.toSet() }
    }

    fun clear() {
        _selectedIds.value = emptySet()
    }
}

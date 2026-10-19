package com.bodyscope3d.app.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.*

data class ExploreState(
    val selectedId: String? = null,
    val isolated: Boolean = false,
    val history: List<String?> = emptyList(),   // للتراجع
)

class ExploreViewModel : ViewModel() {
    private val _s = MutableStateFlow(ExploreState())
    val state = _s.asStateFlow()

    fun select(id: String?) = _s.update { it.copy(selectedId = id, isolated = id != null, history = it.history + it.selectedId) }
    fun showWithBody() = _s.update { it.copy(isolated = false) }
    fun isolate() = _s.update { if (it.selectedId != null) it.copy(isolated = true) else it }
    fun fullBody() = _s.update { it.copy(selectedId = null, isolated = false, history = it.history + it.selectedId) }
    fun deselect() = fullBody()
    fun undo() = _s.update {
        if (it.history.isEmpty()) it else it.copy(selectedId = it.history.last(), isolated = it.history.last() != null, history = it.history.dropLast(1))
    }
}

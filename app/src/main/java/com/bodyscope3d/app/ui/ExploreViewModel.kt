package com.bodyscope3d.app.ui

import androidx.lifecycle.ViewModel
import com.bodyscope3d.app.data.Aspect
import kotlinx.coroutines.flow.*

data class ExploreState(
    val selectedId: String? = null,
    val isolated: Boolean = false,
    val history: List<String?> = emptyList(),   // للتراجع
    val aspect: Aspect? = null,                 // الجهة: أمام/خلف/يمين/يسار
)

class ExploreViewModel : ViewModel() {
    private val _s = MutableStateFlow(ExploreState())
    val state = _s.asStateFlow()

    fun select(id: String?) = _s.update { it.copy(selectedId = id, isolated = id != null, aspect = null, history = it.history + it.selectedId) }
    fun setAspect(a: Aspect?) = _s.update { if (it.selectedId == null) it else it.copy(aspect = if (it.aspect == a) null else a) }
    fun showWithBody() = _s.update { it.copy(isolated = false) }
    fun isolate() = _s.update { if (it.selectedId != null) it.copy(isolated = true) else it }
    fun fullBody() = _s.update { it.copy(selectedId = null, isolated = false, aspect = null, history = it.history + it.selectedId) }
    fun deselect() = fullBody()
    fun undo() = _s.update {
        if (it.history.isEmpty()) it else it.copy(selectedId = it.history.last(), isolated = it.history.last() != null, aspect = null, history = it.history.dropLast(1))
    }
}

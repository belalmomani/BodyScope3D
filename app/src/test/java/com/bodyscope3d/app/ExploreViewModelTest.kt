package com.bodyscope3d.app

import com.bodyscope3d.app.ui.ExploreViewModel
import org.junit.Assert.*
import org.junit.Test

class ExploreViewModelTest {
    @Test fun selectIsolateBackToFullBody() {
        val vm = ExploreViewModel()
        vm.select("head"); assertEquals("head", vm.state.value.selectedId); assertTrue(vm.state.value.isolated)
        vm.showWithBody(); assertFalse(vm.state.value.isolated)
        vm.fullBody(); assertNull(vm.state.value.selectedId)
    }
    @Test fun undoRestoresPrevious() {
        val vm = ExploreViewModel(); vm.select("head"); vm.select("face"); vm.undo()
        assertEquals("head", vm.state.value.selectedId)
    }
}

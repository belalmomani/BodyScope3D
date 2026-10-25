package com.bodyscope3d.app

import com.bodyscope3d.app.data.Aspect
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
    @Test fun aspectSelectToggleAndResetOnRegionChange() {
        val vm = ExploreViewModel(); vm.setAspect(Aspect.BACK)
        assertNull(vm.state.value.aspect)                                  // لا جهة بدون منطقة
        vm.select("head"); vm.setAspect(Aspect.BACK)
        assertEquals(Aspect.BACK, vm.state.value.aspect)
        vm.setAspect(Aspect.BACK); assertNull(vm.state.value.aspect)   // نفس الزر يلغي
        vm.setAspect(Aspect.LEFT); vm.select("neck"); assertNull(vm.state.value.aspect)
    }
    @Test fun aspectYawMapping() {
        assertEquals(0f, Aspect.FRONT.yawDeg); assertEquals(180f, Aspect.BACK.yawDeg)
        assertEquals(-90f, Aspect.RIGHT.yawDeg); assertEquals(90f, Aspect.LEFT.yawDeg)
        assertEquals(Aspect.RIGHT, Aspect.fromKey("right")); assertNull(Aspect.fromKey(""))
    }
    @Test fun undoRestoresPrevious() {
        val vm = ExploreViewModel(); vm.select("head"); vm.select("face"); vm.undo()
        assertEquals("head", vm.state.value.selectedId)
    }
}

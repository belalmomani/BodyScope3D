package com.bodyscope3d.app

import com.bodyscope3d.app.data.Aspect
import com.bodyscope3d.app.ui.PainDraftViewModel
import org.junit.Assert.*
import org.junit.Test

class PainDraftTest {
    @Test fun commitRequiresRegionIntensityAndNature() {
        val d = PainDraftViewModel()
        assertFalse(d.commit("head", null))
        d.intensity = 5; assertFalse(d.commit("head", null))
        d.nature = "dull"; assertFalse(d.commit(null, null)); assertTrue(d.commit("head", Aspect.BACK))
        assertEquals(1, d.entries.size); assertEquals("back", d.entries[0].aspect); assertEquals(-1, d.intensity)   // النموذج صُفّر
    }
    @Test fun draftSurvivesAndClears() {
        val d = PainDraftViewModel(); d.intensity = 0; d.nature = "sharp"; d.commit("neck", null)
        d.intensity = 7; d.nature = "burning"; d.commit("chest", Aspect.FRONT)
        assertEquals(2, d.entries.size); d.entries.removeAt(0); assertEquals("chest", d.entries[0].regionId)
        d.clearAll(); assertTrue(d.entries.isEmpty())
    }
    @Test fun intensityBoundaries() {
        val d = PainDraftViewModel(); d.nature = "dull"
        d.intensity = 10; assertTrue(d.canCommit("head")); d.intensity = 11; assertFalse(d.canCommit("head")); d.intensity = -1; assertFalse(d.canCommit("head"))
    }
    @Test fun severityLabelsCoverRange() {
        val set = (0..10).map { com.bodyscope3d.app.ui.severityRes(it) }.toSet(); assertEquals(4, set.size)
    }
}

package com.bodyscope3d.app

import com.bodyscope3d.app.ui.*
import org.junit.Assert.*
import org.junit.Test

class ExportFormatTest {
    private fun line(region: String, dir: String?, i: Int, notes: String = "") =
        ExportLine(region, dir, i, "Moderate", "Dull", "Onset", "", "Duration", "2 days", "Notes", notes)

    @Test fun formatsNumberedLinesAndOptionalFields() {
        val t = ExportFormat.format("T", "F", listOf(ExportSession("D", listOf(line("Head", "Back", 6, "n1"), line("Neck", null, 2)))))
        assertTrue(t.startsWith("T")); assertTrue(t.endsWith("F"))
        assertTrue("1) Head (Back) — 6/10 (Moderate) — Dull" in t); assertTrue("2) Neck — 2/10" in t)
        assertTrue("Duration: 2 days" in t); assertTrue("Notes: n1" in t); assertFalse("Onset:" in t)   // الحقول الفارغة تُحذف
    }
    @Test fun multipleSessionsAreSeparated() {
        val s = ExportSession("D", listOf(line("Head", null, 1)))
        val t = ExportFormat.format("T", "F", listOf(s, s)); assertTrue("— 1 —" in t && "— 2 —" in t)
    }
}

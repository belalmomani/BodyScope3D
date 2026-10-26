package com.bodyscope3d.app.ui

/** صياغة نص التصدير (دالة نقية قابلة للاختبار): تُرسَل فقط عندما يختار المستخدم المشاركة بنفسه */
data class ExportLine(
    val region: String, val direction: String?, val intensity: Int, val severity: String, val nature: String,
    val onsetLabel: String, val onset: String, val durationLabel: String, val duration: String, val notesLabel: String, val notes: String,
)
data class ExportSession(val date: String, val lines: List<ExportLine>)

object ExportFormat {
    fun format(title: String, footer: String, sessions: List<ExportSession>): String = buildString {
        appendLine(title); appendLine()
        sessions.forEachIndexed { si, s ->
            if (sessions.size > 1) appendLine("— ${si + 1} —")
            appendLine(s.date)
            s.lines.forEachIndexed { i, l ->
                appendLine("${i + 1}) ${l.region}${l.direction?.let { " ($it)" } ?: ""} — ${l.intensity}/10 (${l.severity}) — ${l.nature}")
                if (l.onset.isNotBlank()) appendLine("   ${l.onsetLabel}: ${l.onset}")
                if (l.duration.isNotBlank()) appendLine("   ${l.durationLabel}: ${l.duration}")
                if (l.notes.isNotBlank()) appendLine("   ${l.notesLabel}: ${l.notes}")
            }
            appendLine()
        }
        append(footer)
    }
}

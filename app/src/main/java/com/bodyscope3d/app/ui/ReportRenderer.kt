package com.bodyscope3d.app.ui

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.bodyscope3d.app.R
import com.bodyscope3d.app.data.Region
import kotlin.math.roundToInt

data class ReportEntry(
    val number: Int, val region: String, val direction: String?, val back: Boolean, val nx: Float, val ny: Float,
    val summary: String, val details: List<String>,
)
data class ReportTexts(val title: String, val date: String, val front: String, val back: String, val footer: String, val brand: String)

/** تحويل موضع المنطقة في المجسم إلى إحداثيات (0..1) على صورة الجسم 2D. يجب أن تطابق حدود tools/make_body_maps.py */
object BodyMap {
    const val X0 = -0.5f; const val X1 = 0.5f; const val Z0 = -0.02f; const val Z1 = 1.89f
    private const val S = 1f / 1.855f; private const val Z_CENTER = 0.9375f
    fun point(r: Region, back: Boolean): Pair<Float, Float> {
        val x = r.camera.targetX / S; val z = r.camera.targetY / S + Z_CENTER
        val nx = (x - X0) / (X1 - X0); val ny = (Z1 - z) / (Z1 - Z0)
        return (if (back) 1f - nx else nx).coerceIn(0.02f, 0.98f) to ny.coerceIn(0.02f, 0.98f)
    }
}

/** يرسم صورة تقرير للمشاركة: الجسم من الأمام والخلف مع مواضع الألم مرقّمة ثم التفاصيل. لا يحتوي أي تشخيص. */
object ReportRenderer {
    private const val W = 1080; private const val PAD = 48
    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
    private fun layout(text: String, p: TextPaint, width: Int, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL) =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width).setAlignment(align).setLineSpacing(0f, 1.15f).build()

    fun render(ctx: Context, t: ReportTexts, entries: List<ReportEntry>): Bitmap {
        val ink = 0xFF0A1220.toInt(); val gray = 0xFF51607A.toInt(); val coral = 0xFFFF7A59.toInt()
        val contentW = W - 2 * PAD; val pw = (contentW - 24) / 2; val ph = (pw * 955f / 500f).roundToInt()
        val title = layout(t.title, paint(46f, ink, true), contentW); val date = layout(t.date, paint(30f, gray), contentW)
        val capP = paint(28f, gray, true); val capF = layout(t.front, capP, pw, Layout.Alignment.ALIGN_CENTER); val capB = layout(t.back, capP, pw, Layout.Alignment.ALIGN_CENTER)
        val textW = contentW - 90
        val blocks = entries.map { e ->
            val head = layout(e.region + (e.direction?.let { " ($it)" } ?: ""), paint(36f, ink, true), textW)
            val body = layout((listOf(e.summary) + e.details).joinToString("\n"), paint(30f, 0xFF2B3A55.toInt()), textW)
            Triple(e, head, body)
        }
        val footer = layout(t.footer, paint(26f, gray), contentW); val brand = layout(t.brand, paint(24f, gray), contentW)
        val blocksH = blocks.sumOf { (_, h, b) -> maxOf(64, h.height + b.height + 6) + 30 }
        val H = PAD + title.height + 6 + date.height + 24 + capF.height + 10 + ph + 36 + blocksH + 12 + footer.height + 10 + brand.height + PAD

        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val c = Canvas(bmp); c.drawColor(Color.WHITE)
        var y = PAD.toFloat()
        fun draw(l: StaticLayout, x: Float, yy: Float) { c.save(); c.translate(x, yy); l.draw(c); c.restore() }
        draw(title, PAD.toFloat(), y); y += title.height + 6; draw(date, PAD.toFloat(), y); y += date.height + 24
        draw(capF, PAD.toFloat(), y); draw(capB, (PAD + pw + 24).toFloat(), y); y += capF.height + 10
        val panelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF1F5F9.toInt() }; val bmpP = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val fl = RectF(PAD.toFloat(), y, (PAD + pw).toFloat(), y + ph); val bl = RectF((PAD + pw + 24).toFloat(), y, (PAD + 2 * pw + 24).toFloat(), y + ph)
        for ((rect, res) in listOf(fl to R.drawable.body_front, bl to R.drawable.body_back)) {
            c.drawRoundRect(rect, 24f, 24f, panelP)
            BitmapFactory.decodeResource(ctx.resources, res)?.let { c.drawBitmap(it, null, rect, bmpP); it.recycle() }
        }
        val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = coral }; val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 5f }
        val num = paint(32f, ink, true).apply { textAlign = Paint.Align.CENTER }
        fun marker(cx: Float, cy: Float, n: Int) { c.drawCircle(cx, cy, 28f, circle); c.drawCircle(cx, cy, 28f, ring); c.drawText("$n", cx, cy + 11f, num) }
        val seen = HashMap<String, Int>()
        for (e in entries) {
            val rect = if (e.back) bl else fl
            val key = "${e.back}:${(e.nx * 12).roundToInt()}:${(e.ny * 20).roundToInt()}"; val k = seen.merge(key, 1, Int::plus)!! - 1
            marker(rect.left + e.nx * pw + k * 30f, rect.top + e.ny * ph + k * 30f, e.number)
        }
        y += ph + 36
        for ((e, head, body) in blocks) {
            marker(PAD + 30f, y + 30f, e.number); draw(head, PAD + 80f, y); draw(body, PAD + 80f, y + head.height + 6f)
            y += maxOf(64, head.height + body.height + 6) + 30
        }
        y += 12; draw(footer, PAD.toFloat(), y); y += footer.height + 10; draw(brand, PAD.toFloat(), y)
        return bmp
    }
}

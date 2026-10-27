package com.bodyscope3d.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** مشاركة صورة التقرير. لا يُرسَل شيء من التطبيق نفسه: المستخدم يختار الوجهة، والصورة مؤقتة في مجلد الكاش. */
object ReportShare {
    val WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun writePng(ctx: Context, bmp: Bitmap): File {
        val dir = File(ctx.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }                      // لا نُبقي صور سجلات قديمة
        return File(dir, "bodyscope_${System.currentTimeMillis()}.png").also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    fun uriFor(ctx: Context, f: File): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)

    /** يحاول التطبيقات المحددة بالترتيب (واتساب ثم واتساب للأعمال)، وإن لم يوجد أيٌّ منها يفتح قائمة المشاركة العامة. يعيد true إذا فُتح أحد التطبيقات المحددة */
    fun send(ctx: Context, uri: Uri, packages: List<String>, chooserTitle: String): Boolean {
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        for (p in packages) try { ctx.startActivity(Intent(base).setPackage(p)); return true } catch (_: ActivityNotFoundException) {}
        ctx.startActivity(Intent.createChooser(base, chooserTitle))
        return packages.isEmpty()
    }

    /** حفظ في المعرض (Pictures/BodyScope3D) بدون صلاحيات على Android 10 فما فوق؛ يعيد false على الأقدم */
    fun saveToGallery(ctx: Context, bmp: Bitmap): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "BodyScope3D_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BodyScope3D")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return false
        return true
    }
}

package com.example.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.model.BusinessProfile
import com.example.model.SeoPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PosterBitmapRenderer {

    suspend fun renderBrandedPosterToUri(
        context: Context,
        post: SeoPost,
        profile: BusinessProfile
    ): Uri = withContext(Dispatchers.IO) {
        val bitmap = createPosterBitmap(context, post, profile)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "rankpulse_poster_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun saveBrandedPosterToGallery(
        context: Context,
        post: SeoPost,
        profile: BusinessProfile
    ): String = withContext(Dispatchers.IO) {
        val bitmap = createPosterBitmap(context, post, profile)
        val safeTitle = post.title
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .trim('_')
            .take(28)
            .ifBlank { "poster" }
        val fileName = "RankPulse_${safeTitle}_${System.currentTimeMillis()}.png"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RankPulse")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Unable to create MediaStore entry for poster.")
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)) {
                        throw IllegalStateException("Failed to write PNG poster.")
                    }
                } ?: throw IllegalStateException("Unable to open MediaStore output stream.")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Pictures/RankPulse/$fileName"
            } else {
                val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: File(context.filesDir, "posters")
                val outDir = File(baseDir, "RankPulse").apply { mkdirs() }
                val outFile = File(outDir, fileName)
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(outFile.absolutePath), arrayOf("image/png"), null)
                outFile.name
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun createPosterBitmap(
        context: Context,
        post: SeoPost,
        profile: BusinessProfile
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val accentColor = try {
            Color.parseColor(post.posterAccentHex.ifBlank { "#047857" })
        } catch (_: Exception) {
            Color.parseColor("#047857")
        }

        // 1. Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(Color.parseColor("#06211A"), Color.parseColor("#0B3B2E"), Color.parseColor("#081C17")),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Optional AI scene image as middle artwork banner
        val scenePath = post.posterImagePath
        val sceneRect = RectF(56f, 220f, width - 56f, 670f)
        val sceneClip = Path().apply { addRoundRect(sceneRect, 36f, 36f, Path.Direction.CW) }
        val sceneBitmap = loadScaledBitmap(scenePath, 968, 450)
        if (sceneBitmap != null) {
            canvas.save()
            canvas.clipPath(sceneClip)
            drawCenterCrop(canvas, sceneBitmap, sceneRect)
            val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, sceneRect.top, 0f, sceneRect.bottom,
                    Color.argb(40, 0, 0, 0),
                    Color.argb(190, 6, 33, 26),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(sceneRect, scrim)
            canvas.restore()
            sceneBitmap.recycle()
        } else {
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    sceneRect.left, sceneRect.top, sceneRect.right, sceneRect.bottom,
                    accentColor, Color.parseColor("#0F513E"), Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(sceneRect, 36f, 36f, cardPaint)
            val decorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(35, 255, 255, 255)
            }
            canvas.drawCircle(sceneRect.right - 120f, sceneRect.top + 110f, 150f, decorPaint)
            canvas.drawCircle(sceneRect.left + 140f, sceneRect.bottom - 80f, 110f, decorPaint)
        }

        // 3. Top bar: Service Logo + Business Name
        val logoPath = post.serviceLogoSnapshotPath.ifBlank { profile.serviceLogoPath }
        val logoBitmap = loadScaledBitmap(logoPath, 128, 128)
        val logoRect = RectF(56f, 52f, 176f, 172f)
        if (logoBitmap != null) {
            val logoClip = Path().apply { addRoundRect(logoRect, 28f, 28f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(logoClip)
            drawCenterCrop(canvas, logoBitmap, logoRect)
            canvas.restore()
            logoBitmap.recycle()
        } else {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34D399") }
            canvas.drawRoundRect(logoRect, 28f, 28f, badgePaint)
            val initPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#022C22")
                textSize = 54f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val initial = profile.businessName.firstOrNull()?.uppercaseChar()?.toString() ?: "R"
            canvas.drawText(initial, logoRect.centerX(), logoRect.centerY() + 18f, initPaint)
        }

        val bizPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(profile.businessName.take(26), 200f, 108f, bizPaint)

        val catPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6EE7B7")
            textSize = 28f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(profile.category.take(36), 200f, 152f, catPaint)

        // 4. Mandatory Owner Photo Portrait (ALWAYS rendered regardless of who performed work)
        val ownerPhotoPath = post.ownerPhotoSnapshotPath.ifBlank { profile.ownerPhotoPath }
        val ownerRect = RectF(width - 270f, 430f, width - 86f, 614f)
        val ownerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FBBF24") }
        canvas.drawCircle(ownerRect.centerX(), ownerRect.centerY(), 100f, ownerBgPaint)
        val innerCircle = RectF(ownerRect.left + 6f, ownerRect.top + 6f, ownerRect.right - 6f, ownerRect.bottom - 6f)
        val ownerBitmap = loadScaledBitmap(ownerPhotoPath, 200, 200)
        if (ownerBitmap != null) {
            val circlePath = Path().apply { addOval(innerCircle, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(circlePath)
            drawCenterCrop(canvas, ownerBitmap, innerCircle)
            canvas.restore()
            ownerBitmap.recycle()
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#064E3B") }
            canvas.drawOval(innerCircle, fallbackPaint)
            val ownerInitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 58f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val ownerInit = profile.ownerName.firstOrNull()?.uppercaseChar()?.toString() ?: "O"
            canvas.drawText(ownerInit, innerCircle.centerX(), innerCircle.centerY() + 20f, ownerInitPaint)
        }

        // Owner label pill under portrait
        val ownerPill = RectF(ownerRect.left - 18f, ownerRect.bottom + 8f, ownerRect.right + 18f, ownerRect.bottom + 46f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FBBF24") }
        canvas.drawRoundRect(ownerPill, 18f, 18f, pillPaint)
        val ownerTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#451A03")
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("OWNER: ${profile.ownerName.take(14)}", ownerPill.centerX(), ownerPill.centerY() + 8f, ownerTagPaint)

        // 5. Work Performed By tag inside scene
        val staffPill = RectF(86f, 590f, 540f, 642f)
        val staffBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 2, 44, 34) }
        canvas.drawRoundRect(staffPill, 22f, 22f, staffBg)
        val staffTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A7F3D0")
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Service by: ${post.workPerformedBy.take(26)}", 108f, 624f, staffTextPaint)

        // 6. Post Headline & Description
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            typeface = Typeface.DEFAULT_BOLD
        }
        val titleLayout = StaticLayout.Builder.obtain(
            post.title, 0, post.title.length, titlePaint, width - 112
        ).setMaxLines(2).build()
        canvas.save()
        canvas.translate(56f, 710f)
        titleLayout.draw(canvas)
        canvas.restore()

        val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D1FAE5")
            textSize = 32f
            typeface = Typeface.DEFAULT
        }
        val descLayout = StaticLayout.Builder.obtain(
            post.description, 0, post.description.length, descPaint, width - 112
        ).setMaxLines(5).setLineSpacing(6f, 1.0f).build()
        canvas.save()
        canvas.translate(56f, 848f)
        descLayout.draw(canvas)
        canvas.restore()

        // 7. CTA Pill
        val ctaText = post.ctaLabel.ifBlank { "Call Now" }
        val ctaRect = RectF(56f, 1068f, 440f, 1138f)
        val ctaBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34D399") }
        canvas.drawRoundRect(ctaRect, 32f, 32f, ctaBg)
        val ctaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#022C22")
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(ctaText.uppercase(), ctaRect.centerX(), ctaRect.centerY() + 10f, ctaPaint)

        // 8. Footer strip: Owner Phone Number & Business Address (MANDATORY on every poster)
        val footerRect = RectF(40f, 1166f, width - 40f, height - 36f)
        val footerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#022C22") }
        canvas.drawRoundRect(footerRect, 28f, 28f, footerBg)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#34D399")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(footerRect, 28f, 28f, borderPaint)

        val phoneText = post.ownerPhoneSnapshot.ifBlank { profile.ownerPhone }.ifBlank { "Phone not set" }
        val addressText = post.addressSnapshot.ifBlank { profile.address }.ifBlank { "Address not set" }

        val footerPhonePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FBBF24")
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Call: $phoneText", 72f, 1226f, footerPhonePaint)

        val footerAddressPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 27f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("Address: ${addressText.take(56)}", 72f, 1278f, footerAddressPaint)
        return bitmap
    }

    private fun loadScaledBitmap(path: String, reqW: Int, reqH: Int): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            var sample = 1
            while (opts.outWidth / sample > reqW * 2 && opts.outHeight / sample > reqH * 2) {
                sample *= 2
            }
            BitmapFactory.Options().apply { inSampleSize = sample }.let {
                BitmapFactory.decodeFile(path, it)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, dst: RectF) {
        val scale = maxOf(dst.width() / bitmap.width, dst.height() / bitmap.height)
        val srcW = dst.width() / scale
        val srcH = dst.height() / scale
        val left = ((bitmap.width - srcW) / 2f).toInt().coerceAtLeast(0)
        val top = ((bitmap.height - srcH) / 2f).toInt().coerceAtLeast(0)
        val src = Rect(
            left,
            top,
            (left + srcW).toInt().coerceAtMost(bitmap.width),
            (top + srcH).toInt().coerceAtMost(bitmap.height)
        )
        canvas.drawBitmap(bitmap, src, dst, null)
    }
}

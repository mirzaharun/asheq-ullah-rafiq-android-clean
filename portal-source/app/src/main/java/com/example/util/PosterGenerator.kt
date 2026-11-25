package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.DevelopmentProject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PosterGenerator {

    /**
     * Programmatically creates a high-resolution, professionally branded social media poster (1080x1350, 4:5 aspect ratio)
     * for a development project with real leadership portraits, project image, metrics, and branding.
     * Then saves the bitmap to cache and triggers an image sharing Intent.
     */
    fun generateAndSharePoster(context: Context, project: DevelopmentProject) {
        try {
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Background fill
            val bgPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#F8FAFC")
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // 2. Top Header Bar (Deep Green Gradient)
            val headerHeight = 220f
            val headerPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), headerHeight,
                    Color.parseColor("#004D40"), Color.parseColor("#1B5E20"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)

            // Header Title & Subtitle
            val headerTitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("উন্নয়ন অগ্রযাত্রায় মহেশখালী ও কুতুবদিয়া", 50f, 95f, headerTitlePaint)

            val headerSubPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#DCFCE7")
                textSize = 26f
            }
            canvas.drawText("কক্সবাজার-২ আসনের অভূতপূর্ব মেগা প্রকল্প ও অবকাঠামো", 50f, 150f, headerSubPaint)

            // 3. Project Banner Image (draw between Y: 220 and 720)
            val imgTop = 220f
            val imgBottom = 720f
            val bannerBitmap = loadProjectBitmap(context, project.imageUrl)
            if (bannerBitmap != null) {
                val srcRect = Rect(0, 0, bannerBitmap.width, bannerBitmap.height)
                val destRect = RectF(0f, imgTop, width.toFloat(), imgBottom)
                val imgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bannerBitmap, srcRect, destRect, imgPaint)
            } else {
                // Fallback colored rect
                val placeholderPaint = Paint().apply {
                    color = Color.parseColor("#0284C7")
                }
                canvas.drawRect(0f, imgTop, width.toFloat(), imgBottom, placeholderPaint)
            }

            // Category & Status overlay badge on image
            val badgePaint = Paint().apply {
                color = Color.parseColor("#CC000000")
                isAntiAlias = true
            }
            val badgeRect = RectF(50f, 250f, 320f, 310f)
            canvas.drawRoundRect(badgeRect, 20f, 20f, badgePaint)
            val badgeTextPaint = Paint().apply {
                color = Color.parseColor("#FBBF24")
                textSize = 26f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(project.categoryBn.ifBlank { "মেগা প্রকল্প" }, 75f, 290f, badgeTextPaint)

            // 4. Leadership Portraits (Dual circular portraits in upper middle right overlay)
            val portraitRadius = 60f
            val avatar1 = BitmapFactory.decodeResource(context.resources, R.drawable.asheq_avatar)
            val avatar2 = BitmapFactory.decodeResource(context.resources, R.drawable.asheq_profile_custom)

            if (avatar1 != null) {
                drawCircularBitmap(canvas, avatar1, width - 180f, imgBottom - 50f, portraitRadius, "আশেক উল্লাহ রফিক")
            }
            if (avatar2 != null) {
                drawCircularBitmap(canvas, avatar2, width - 80f, imgBottom - 50f, portraitRadius, null)
            }

            // 5. Project Content Card Area (Y: 730 to 1180)
            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 50f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(project.titleBn, 50f, 790f, titlePaint)

            // Metric Pills: Budget & Completion Progress
            val progressVal = project.completionPercentage.coerceIn(0, 100)
            val statBoxPaint = Paint().apply {
                color = Color.parseColor("#ECFDF5")
                isAntiAlias = true
            }
            val statStrokePaint = Paint().apply {
                color = Color.parseColor("#10B981")
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            val statRect = RectF(50f, 830f, 400f, 920f)
            canvas.drawRoundRect(statRect, 16f, 16f, statBoxPaint)
            canvas.drawRoundRect(statRect, 16f, 16f, statStrokePaint)

            val statTextPaint = Paint().apply {
                color = Color.parseColor("#065F46")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("অগ্রগতি: $progressVal% সম্পন্ন", 75f, 888f, statTextPaint)

            if (project.budgetBn.isNotBlank()) {
                val budgetBoxPaint = Paint().apply {
                    color = Color.parseColor("#EFF6FF")
                    isAntiAlias = true
                }
                val budgetStroke = Paint().apply {
                    color = Color.parseColor("#3B82F6")
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                val budgetRect = RectF(420f, 830f, 780f, 920f)
                canvas.drawRoundRect(budgetRect, 16f, 16f, budgetBoxPaint)
                canvas.drawRoundRect(budgetRect, 16f, 16f, budgetStroke)

                val budgetTextPaint = Paint().apply {
                    color = Color.parseColor("#1E40AF")
                    textSize = 30f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("বাজেট: ${project.budgetBn}", 445f, 888f, budgetTextPaint)
            }

            // Description text (multi-line layout)
            val descPaint = Paint().apply {
                color = Color.parseColor("#334155")
                textSize = 28f
                isAntiAlias = true
            }
            val desc = project.descriptionBn.ifBlank { "কক্সবাজার-২ (মহেশখালী-কুতুবদিয়া) আসনের সার্বিক উন্নয়ন কর্মকাণ্ড।" }
            drawMultilineText(canvas, desc, 50f, 970f, width - 100, descPaint, maxLines = 4)

            // 6. Footer Branding Banner (Y: 1180 to 1350)
            val footerPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f, 1180f, width.toFloat(), 1350f,
                    Color.parseColor("#0F172A"), Color.parseColor("#1E293B"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 1180f, width.toFloat(), 1350f, footerPaint)

            val footerBrandPaint = Paint().apply {
                color = Color.WHITE
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("উন্নয়ন ও জনসেবা পোর্টাল • মহেশখালী ও কুতুবদিয়া", 50f, 1250f, footerBrandPaint)

            val footerSubPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 24f
                isAntiAlias = true
            }
            canvas.drawText("জননেতা আশেক উল্লাহ রফিক এর বলিষ্ঠ নেতৃত্ব ও উন্নয়ন কর্মকাণ্ড", 50f, 1300f, footerSubPaint)

            // 7. Save to cache & launch ACTION_SEND Intent with FileProvider
            val cacheDir = File(context.cacheDir, "posters")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val posterFile = File(cacheDir, "project_poster_${project.id}_${System.currentTimeMillis()}.png")
            val out = FileOutputStream(posterFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                posterFile
            )

            val shareText = "【${project.titleBn}】\nবাজেট: ${project.budgetBn} | অগ্রগতি: ${project.completionPercentage}%\n${project.descriptionBn}\n\n— উন্নয়ন ও জনসেবা পোর্টাল (মহেশখালী ও কুতুবদিয়া)"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "পোস্টার শেয়ার করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "পোস্টার শেয়ার করার উপযুক্ত অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "পোস্টার তৈরিতে ত্রুটি হয়েছে: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProjectBitmap(context: Context, imageUrl: String): Bitmap? {
        return try {
            when {
                imageUrl.startsWith("drawable:") -> {
                    val name = imageUrl.removePrefix("drawable:")
                    val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
                    if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
                }
                imageUrl.startsWith("/") -> BitmapFactory.decodeFile(imageUrl)
                else -> {
                    // Try by standard identifier name
                    val resId = context.resources.getIdentifier(imageUrl, "drawable", context.packageName)
                    if (resId != 0) BitmapFactory.decodeResource(context.resources, resId)
                    else BitmapFactory.decodeResource(context.resources, R.drawable.moheshkhali_banner)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun drawCircularBitmap(canvas: Canvas, bitmap: Bitmap, cx: Float, cy: Float, radius: Float, label: String?) {
        val output = Bitmap.createBitmap((radius * 2).toInt(), (radius * 2).toInt(), Bitmap.Config.ARGB_8888)
        val outCanvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, (radius * 2).toInt(), (radius * 2).toInt())

        outCanvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        outCanvas.drawBitmap(bitmap, null, rect, paint)

        // Draw white border ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(cx, cy, radius, ringPaint)
        canvas.drawBitmap(output, cx - radius, cy - radius, null)
    }

    private fun drawMultilineText(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Int, paint: Paint, maxLines: Int) {
        val words = text.split(" ")
        var currentLine = ""
        var y = startY
        var linesDrawn = 0

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) < maxWidth) {
                currentLine = testLine
            } else {
                canvas.drawText(currentLine, x, y, paint)
                y += paint.textSize + 14f
                currentLine = word
                linesDrawn++
                if (linesDrawn >= maxLines - 1) break
            }
        }
        if (currentLine.isNotEmpty() && linesDrawn < maxLines) {
            canvas.drawText(currentLine, x, y, paint)
        }
    }
}

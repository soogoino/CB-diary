package com.chastity.diary.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.chastity.diary.domain.model.CardData
import com.chastity.diary.domain.model.CardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

/**
 * Renders a [SummaryCardContent] Composable off-screen into a 1080×1350 PNG bitmap
 * and provides helpers to share or save it to the media library.
 */
object CardRenderer {

    const val CARD_W = 1080
    const val CARD_H = 1350

    private val FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd")

    // ── Render ─────────────────────────────────────────────────────────────────

    /**
     * Renders [content] into a software [Bitmap].
     *
     * Must be called on the **main thread** (use [withContext(Dispatchers.Main)]).
     * The view is briefly added off-screen to the activity's decor view so that the
     * Compose lifecycle observers can attach, then removed immediately after drawing.
     *
     * @param activity Required to access the window decor view for measurement.
     * @param content  The Composable tree representing `SummaryCardContent(...)`.
     */
    suspend fun renderToBitmap(
        activity: Activity,
        content: @Composable () -> Unit
    ): Bitmap = withContext(Dispatchers.Main) {
        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity as? androidx.lifecycle.LifecycleOwner)
            setViewTreeSavedStateRegistryOwner(activity as? androidx.savedstate.SavedStateRegistryOwner)
            setContent { content() }
        }

        // Add off-screen (y = -CARD_H * 2) so it is invisible to the user.
        val decor = activity.window.decorView as FrameLayout
        val params = FrameLayout.LayoutParams(CARD_W, CARD_H).also {
            it.topMargin = -(CARD_H * 2)
        }
        decor.addView(composeView, params)

        // Give Compose one frame to compose + measure.
        delay(200)

        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(CARD_W, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(CARD_H, View.MeasureSpec.EXACTLY)
        )
        composeView.layout(0, 0, CARD_W, CARD_H)

        // Force a software layer so Canvas.drawBitmap works on all API levels.
        composeView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        composeView.draw(Canvas(bitmap))
        decor.removeView(composeView)

        bitmap
    }

    // ── Persist ────────────────────────────────────────────────────────────────

    /**
     * Compresses [bitmap] to PNG and stores it in [Context.cacheDir]/cards/.
     * Previous card for the same date is overwritten.
     */
    suspend fun saveToCache(context: Context, bitmap: Bitmap, data: CardData): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "cards").also { it.mkdirs() }
            val file = File(dir, "summary_${data.date.format(FILE_DATE_FMT)}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            file
        }

    // ── Share ─────────────────────────────────────────────────────────────────

    /**
     * Triggers the system share sheet for a previously saved [pngFile].
     */
    fun shareCard(context: Context, pngFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pngFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    // ── Save to gallery ───────────────────────────────────────────────────────

    /**
     * Inserts [pngFile] into the device's media library (Pictures/CB Diary).
     * On API 29+ uses the scoped MediaStore API; on API 28 and below uses the legacy path.
     *
     * @return the [Uri] of the newly inserted image, or null on failure.
     */
    suspend fun saveToGallery(context: Context, pngFile: File, data: CardData): Uri? =
        withContext(Dispatchers.IO) {
            val displayName = "CB_Diary_${data.date.format(FILE_DATE_FMT)}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/CB Diary")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext null
                resolver.openOutputStream(uri)?.use { out ->
                    pngFile.inputStream().copyTo(out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                @Suppress("DEPRECATION")
                val path = MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    pngFile.absolutePath,
                    displayName,
                    "CB Diary summary card"
                )
                path?.let { Uri.parse(it) }
            }
        }
}

package com.chastity.diary.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.chastity.diary.R
import com.chastity.diary.domain.model.BackgroundSource
import com.chastity.diary.domain.model.CardTemplateSpec
import com.chastity.diary.domain.model.CardTheme
import com.chastity.diary.domain.model.TextColorScheme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Imports a user-designed card template from a `.zip` file.
 *
 * Expected zip structure:
 * ```
 * template.png              # 1080×1350 background image
 * card_template_spec.json   # see [CardTemplateSpec]
 * ```
 *
 * Validated templates are stored under `context.filesDir/templates/<uuid>/`.
 * The returned [CardTheme] uses [BackgroundSource.ExternalAsset] pointing to those files.
 *
 * ## Error handling
 * All validation failures return `Result.failure(ImportException(...))` — never throw.
 * The [ImportException.messageResId] should be shown to the user as a Snackbar.
 */
object TemplateImporter {

    const val EXPECTED_WIDTH = CardRenderer.CARD_W
    const val EXPECTED_HEIGHT = CardRenderer.CARD_H

    class ImportException(val messageResId: Int, cause: Throwable? = null) :
        Exception("Import error res=$messageResId", cause)

    private val gson = Gson()

    /**
     * Parse, validate, and persist a template `.zip` selected by the user.
     *
     * @param context Application or Activity context.
     * @param zipUri  SAF URI of the selected `.zip` file.
     * @return [Result] wrapping a ready-to-use [CardTheme] on success, or an
     *         [ImportException] containing a string resource id for the error message.
     */
    suspend fun import(context: Context, zipUri: Uri): Result<CardTheme> =
        withContext(Dispatchers.IO) {
            try {
                // ── 1. Read zip entries ──────────────────────────────────────
                var pngBytes: ByteArray? = null
                var specJson: String? = null

                context.contentResolver.openInputStream(zipUri)?.use { raw ->
                    ZipInputStream(raw).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            when (entry.name) {
                                "template.png" -> pngBytes = zip.readBytes()
                                "card_template_spec.json" -> specJson = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                } ?: return@withContext Result.failure(
                    ImportException(R.string.card_import_error_cannot_read)
                )

                if (pngBytes == null) return@withContext Result.failure(
                    ImportException(R.string.card_import_error_missing_png)
                )
                if (specJson == null) return@withContext Result.failure(
                    ImportException(R.string.card_import_error_missing_spec)
                )

                // ── 2. Validate PNG dimensions ───────────────────────────────
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes!!.size, opts)
                if (opts.outWidth != EXPECTED_WIDTH || opts.outHeight != EXPECTED_HEIGHT) {
                    return@withContext Result.failure(
                        ImportException(R.string.card_import_error_wrong_size)
                    )
                }

                // ── 3. Parse spec ────────────────────────────────────────────
                val spec: CardTemplateSpec = try {
                    gson.fromJson(specJson, CardTemplateSpec::class.java)
                } catch (e: Exception) {
                    return@withContext Result.failure(
                        ImportException(R.string.card_import_error_invalid_spec, e)
                    )
                }
                if (spec.formatVersion != 1) {
                    return@withContext Result.failure(
                        ImportException(R.string.card_import_error_unsupported_version)
                    )
                }

                // ── 4. Persist to filesDir/templates/<uuid>/ ─────────────────
                val uuid = UUID.randomUUID().toString()
                val dir = File(context.filesDir, "templates/$uuid").also { it.mkdirs() }

                val pngFile = File(dir, "template.png")
                FileOutputStream(pngFile).use { it.write(pngBytes) }

                val specFile = File(dir, "card_template_spec.json")
                specFile.writeText(specJson!!)

                // ── 5. Build CardTheme ────────────────────────────────────────
                val pngUri = Uri.fromFile(pngFile)
                val textScheme = if (spec.textColorScheme == "dark")
                    TextColorScheme.DARK else TextColorScheme.LIGHT

                val theme = CardTheme(
                    id = "user_$uuid",
                    nameResId = R.string.card_theme_user_imported,
                    isPremium = false,
                    backgroundSource = BackgroundSource.ExternalAsset(pngUri = pngUri, spec = spec),
                    accentColor = if (textScheme == TextColorScheme.LIGHT)
                        androidx.compose.ui.graphics.Color.White
                    else
                        androidx.compose.ui.graphics.Color.Black,
                    textColorScheme = textScheme,
                    overlayOpacity = spec.overlayOpacity,
                    userTemplateId = uuid
                )

                Result.success(theme)
            } catch (e: Exception) {
                Result.failure(ImportException(R.string.card_import_error_unknown, e))
            }
        }

    /**
     * Deletes a previously imported template from `filesDir/templates/<uuid>/`.
     * Safe to call even if the directory does not exist.
     */
    suspend fun delete(context: Context, uuid: String) = withContext(Dispatchers.IO) {
        File(context.filesDir, "templates/$uuid").deleteRecursively()
    }
}

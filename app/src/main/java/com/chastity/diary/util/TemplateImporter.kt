package com.chastity.diary.util

import android.content.Context
import android.graphics.Bitmap
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
 * template.png              # 1080×1920 background image
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

    /**
     * Scans `filesDir/templates/` and reconstructs all previously-imported [CardTheme]s.
     * Call this in [CardViewModel]'s `init {}` block to restore templates after process death.
     *
     * Directories that are missing either `template.png` or `card_template_spec.json`,
     * or whose spec is malformed, are silently skipped.
     */
    suspend fun loadUserTemplates(context: Context): List<CardTheme> = withContext(Dispatchers.IO) {
        val templatesDir = File(context.filesDir, "templates")
        if (!templatesDir.exists()) return@withContext emptyList()

        templatesDir.listFiles()?.mapNotNull { dir ->
            if (!dir.isDirectory) return@mapNotNull null
            val uuid = dir.name
            val pngFile = File(dir, "template.png")
            val specFile = File(dir, "card_template_spec.json")
            if (!pngFile.exists() || !specFile.exists()) return@mapNotNull null

            try {
                val spec = gson.fromJson(specFile.readText(), CardTemplateSpec::class.java)
                val pngUri = Uri.fromFile(pngFile)
                val textScheme = if (spec.textColorScheme == "dark")
                    TextColorScheme.DARK else TextColorScheme.LIGHT

                CardTheme(
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
                    userTemplateId = uuid,
                    displayName = File(dir, "name.txt").takeIf { it.exists() }?.readText()?.trim()?.ifBlank { null }
                )
            } catch (e: Exception) {
                null // skip corrupted entries
            }
        } ?: emptyList()
    }

    /**
     * Renames a user-imported template by writing (or deleting) `name.txt` inside its directory.
     * A blank [name] clears the stored name so the generic label is used.
     */
    suspend fun renameTemplate(context: Context, uuid: String, name: String) =
        withContext(Dispatchers.IO) {
            val nameFile = File(context.filesDir, "templates/$uuid/name.txt")
            if (name.isBlank()) nameFile.delete() else nameFile.writeText(name.trim())
        }

    /**
     * Imports a single PNG/JPG image as a card background.
     *
     * Unlike [import], no `.zip` or JSON spec is required — the image is automatically
     * scaled to 1080×1920 and [CardTemplateSpec.DEFAULT] is applied so the App controls
     * all text layout. The designer only needs to supply the background artwork.
     *
     * @param context         Application or Activity context.
     * @param imageUri        SAF URI of the selected image (PNG/JPG).
     * @param textColorScheme Whether the content text should render as [TextColorScheme.LIGHT]
     *                        or [TextColorScheme.DARK] over this background.
     * @return [Result] wrapping a ready-to-use [CardTheme] on success.
     */
    suspend fun importSingleImage(
        context: Context,
        imageUri: Uri,
        textColorScheme: TextColorScheme,
    ): Result<CardTheme> = withContext(Dispatchers.IO) {
        try {
            // ── 1. Read image bytes ──────────────────────────────────────────
            val imageBytes = context.contentResolver.openInputStream(imageUri)?.use {
                it.readBytes()
            } ?: return@withContext Result.failure(
                ImportException(R.string.card_import_error_cannot_read)
            )

            // ── 2. Decode and scale to 1080×1920 ────────────────────────────
            val srcBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return@withContext Result.failure(
                    ImportException(R.string.card_import_error_unsupported_format)
                )

            val scaled = if (srcBitmap.width == EXPECTED_WIDTH && srcBitmap.height == EXPECTED_HEIGHT) {
                srcBitmap
            } else {
                Bitmap.createScaledBitmap(srcBitmap, EXPECTED_WIDTH, EXPECTED_HEIGHT, true)
                    .also { if (it != srcBitmap) srcBitmap.recycle() }
            }

            // ── 3. Build spec using DEFAULT (fixed layout) ───────────────────
            val spec = CardTemplateSpec.DEFAULT.copy(
                textColorScheme = if (textColorScheme == TextColorScheme.DARK) "dark" else "light"
            )

            // ── 4. Persist to filesDir/templates/<uuid>/ ─────────────────────
            val uuid = UUID.randomUUID().toString()
            val dir = File(context.filesDir, "templates/$uuid").also { it.mkdirs() }

            val pngFile = File(dir, "template.png")
            FileOutputStream(pngFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            scaled.recycle()

            val specFile = File(dir, "card_template_spec.json")
            specFile.writeText(gson.toJson(spec))

            // ── 5. Build CardTheme ────────────────────────────────────────────
            val pngUri = Uri.fromFile(pngFile)
            val theme = CardTheme(
                id = "user_$uuid",
                nameResId = R.string.card_theme_user_imported,
                isPremium = false,
                backgroundSource = BackgroundSource.ExternalAsset(pngUri = pngUri, spec = spec),
                accentColor = if (textColorScheme == TextColorScheme.LIGHT)
                    androidx.compose.ui.graphics.Color.White
                else
                    androidx.compose.ui.graphics.Color.Black,
                textColorScheme = textColorScheme,
                overlayOpacity = spec.overlayOpacity,
                userTemplateId = uuid
            )

            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(ImportException(R.string.card_import_error_unknown, e))
        }
    }
}

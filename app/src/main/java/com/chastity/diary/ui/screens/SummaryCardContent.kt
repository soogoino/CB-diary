@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.chastity.diary.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.chastity.diary.domain.model.rotatingQuestionTitleRes
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.R
import com.chastity.diary.domain.model.BackgroundSource
import com.chastity.diary.domain.model.CardData
import com.chastity.diary.domain.model.CardTemplateSpec
import com.chastity.diary.domain.model.CardTheme
import com.chastity.diary.domain.model.PatternType
import com.chastity.diary.domain.model.TextColorScheme
import com.chastity.diary.ui.theme.CardThemes
import com.chastity.diary.util.Constants
import com.chastity.diary.viewmodel.CardViewModel
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ── Constants ─────────────────────────────────────────────────────────────────

private val CARD_RATIO_W = 1080f
private val CARD_RATIO_H = 1920f
private val CARD_DATE_FMT = DateTimeFormatter.ofPattern("yyyy / MM / dd")

// ── SummaryCardContent ────────────────────────────────────────────────────────

/**
 * The full-resolution card Composable.
 * Designed at 1080×1920 logical pixels; wrap in a scaled [Box] for previews.
 *
 * Layer structure:
 * ```
 * Box {
 *   Layer 0 — Background (Gradient / CanvasPattern / ExternalAsset)
 *   Layer 1 — Optional translucent scrim (theme.overlayOpacity)
 *   Layer 2 — Content (always on top, text colour from theme.textColorScheme)
 * }
 * ```
 */
@Composable
fun SummaryCardContent(
    data: CardData,
    theme: CardTheme,
    modifier: Modifier = Modifier
) {
    val textColor = if (theme.textColorScheme == TextColorScheme.LIGHT)
        Color.White else Color.Black
    val subTextColor = textColor.copy(alpha = 0.7f)
    val spec = (theme.backgroundSource as? BackgroundSource.ExternalAsset)?.spec
        ?: CardTemplateSpec.DEFAULT

    Box(
        modifier = modifier
            .size(CARD_RATIO_W.dp, CARD_RATIO_H.dp)
    ) {
        // ── Layer 0: Background ──────────────────────────────────────────────
        CardBackground(source = theme.backgroundSource)

        // ── Layer 1: Scrim ───────────────────────────────────────────────────
        if (theme.overlayOpacity > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = theme.overlayOpacity))
            )
        }

        // ── Layer 2: Content ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = spec.safeZones.header.left.dp,
                    end = spec.safeZones.header.right.dp,
                    top = spec.safeZones.header.top.dp,
                    bottom = spec.safeZones.footer.bottom.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            CardHeader(data = data, textColor = textColor, subColor = subTextColor)

            // Streak ring
            StreakSection(
                data = data,
                accentColor = theme.accentColor,
                textColor = textColor,
                subColor = subTextColor
            )

            // Today snapshot
            TodaySection(
                data = data,
                accentColor = theme.accentColor,
                textColor = textColor,
                subColor = subTextColor
            )

            // Today's photo (shown only when user opts in and photo exists)
            PhotoSection(
                data = data,
                textColor = textColor,
                subColor = subTextColor
            )

            // 7-day averages
            AvgSection(
                data = data,
                accentColor = theme.accentColor,
                textColor = textColor,
                subColor = subTextColor
            )

            // Check-ins row + rotating question
            CheckInsSection(
                data = data,
                accentColor = theme.accentColor,
                textColor = textColor,
                subColor = subTextColor
            )

            // Footer
            CardFooter(textColor = subTextColor)
        }
    }
}

// ── Background rendering ──────────────────────────────────────────────────────

@Composable
private fun CardBackground(source: BackgroundSource, modifier: Modifier = Modifier) {
    when (source) {
        is BackgroundSource.Gradient -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = source.colors,
                            start = gradientStart(source.angleDegrees),
                            end = gradientEnd(source.angleDegrees)
                        )
                    )
            )
        }

        is BackgroundSource.CanvasPattern -> {
            Canvas(modifier = modifier.fillMaxSize()) {
                // Base gradient
                drawRect(
                    brush = Brush.linearGradient(
                        colors = source.baseColors,
                        start = gradientStart(source.angleDegrees),
                        end = gradientEnd(source.angleDegrees)
                    )
                )
                // Geometric pattern overlay
                drawPattern(source.type, source.patternColor, source.patternAlpha)
            }
        }

        is BackgroundSource.ExternalAsset -> {
            val bmp = remember(source.pngUri) {
                runCatching {
                    BitmapFactory.decodeFile(source.pngUri.path)
                        ?.asImageBitmap()
                }.getOrNull()
            }
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E))
                )
            }
        }
    }
}

// ── Gradient helpers ──────────────────────────────────────────────────────────

private fun gradientStart(angleDeg: Float): Offset {
    val rad = (angleDeg - 90f) * (PI / 180f).toFloat()
    val cx = 540f; val cy = 675f; val r = 900f
    return Offset(cx - r * cos(rad), cy - r * sin(rad))
}

private fun gradientEnd(angleDeg: Float): Offset {
    val rad = (angleDeg - 90f) * (PI / 180f).toFloat()
    val cx = 540f; val cy = 675f; val r = 900f
    return Offset(cx + r * cos(rad), cy + r * sin(rad))
}

// ── Canvas pattern drawing ────────────────────────────────────────────────────

private fun DrawScope.drawPattern(type: PatternType, color: Color, alpha: Float) {
    when (type) {
        PatternType.DOTS -> {
            val spacing = 60f; val radius = 4f
            var x = spacing / 2f
            while (x < size.width) {
                var y = spacing / 2f
                while (y < size.height) {
                    drawCircle(color.copy(alpha = alpha), radius = radius, center = Offset(x, y))
                    y += spacing
                }
                x += spacing
            }
        }

        PatternType.DIAGONAL_LINES -> {
            val spacing = 48f; val strokeWidth = 1.5f
            var off = -size.height
            while (off < size.width) {
                drawLine(
                    color = color.copy(alpha = alpha),
                    start = Offset(off, 0f),
                    end = Offset(off + size.height, size.height),
                    strokeWidth = strokeWidth
                )
                off += spacing
            }
        }

        PatternType.HEXAGON -> {
            // Simplified: rows of small circles arranged in a hex grid
            val r = 28f; val cols = (size.width / (r * 1.8f)).toInt() + 2
            val rows = (size.height / (r * 1.55f)).toInt() + 2
            for (row in 0..rows) {
                val offsetX = if (row % 2 == 0) 0f else r * 0.9f
                for (col in 0..cols) {
                    val cx = col * r * 1.8f + offsetX
                    val cy = row * r * 1.55f
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = r * 0.85f,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                    )
                }
            }
        }

        PatternType.CROSS_HATCH -> {
            val spacing = 36f; val sw = 0.8f
            var off = 0f
            while (off < size.width + size.height) {
                drawLine(color.copy(alpha = alpha), Offset(off, 0f),
                    Offset(0f, off), sw)
                drawLine(color.copy(alpha = alpha), Offset(size.width - off, size.height),
                    Offset(size.width, size.height - off), sw)
                off += spacing
            }
        }

        PatternType.NONE -> Unit
    }
}

// ── Content sections ──────────────────────────────────────────────────────────

@Composable
private fun CardHeader(data: CardData, textColor: Color, subColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Lock, null, tint = textColor, modifier = Modifier.size((40).dp))
            Text("CB diary", color = textColor, fontWeight = FontWeight.Bold, fontSize = (36).sp)
        }
        Text(data.date.format(CARD_DATE_FMT), color = subColor, fontSize = (28).sp)
    }
}

@Composable
private fun StreakSection(data: CardData, accentColor: Color, textColor: Color, subColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Streak ring
        Box(contentAlignment = Alignment.Center) {
            CircleRing(progress = (data.currentStreak.toFloat() / data.longestStreak.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f),
                accentColor = accentColor, size = 280.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", fontSize = 56.sp)
                Text(
                    data.currentStreak.toString(),
                    color = textColor, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(stringResource(R.string.card_days_streak), color = subColor, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${stringResource(R.string.card_longest)}: ${data.longestStreak} ${stringResource(R.string.card_days)}",
            color = subColor, fontSize = 26.sp
        )
    }
}

@Composable
private fun CircleRing(progress: Float, accentColor: Color, size: Dp) {
    val animP by animateFloatAsState(progress, label = "ring")
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 20f
        val inset = stroke / 2
        // Track
        drawArc(
            color = accentColor.copy(alpha = 0.25f),
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Progress
        drawArc(
            color = accentColor,
            startAngle = -90f, sweepAngle = 360f * animP, useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun TodaySection(data: CardData, accentColor: Color, textColor: Color, subColor: Color) {
    // Map stored mood key (from Constants.MOODS) to the current-locale display label
    val moodKeys = Constants.MOODS
    val moodLabels = stringArrayResource(R.array.moods_array).toList()
    val moodKeyIndex = if (data.morningMood != null) moodKeys.indexOf(data.morningMood) else -1
    val displayMood = if (moodKeyIndex >= 0) moodLabels.getOrNull(moodKeyIndex) ?: data.morningMood
                      else data.morningMood

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.card_today), color = subColor, fontSize = 22.sp,
            fontWeight = FontWeight.Medium)

        // All 6 today-fields in one unified row: mood · energy · desire · comfort · focus · sleep
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                stringResource(R.string.card_mood)           to (displayMood ?: "—"),
                stringResource(R.string.card_morning_energy) to (if (data.morningEnergy != null) "${data.morningEnergy}" else "—"),
                stringResource(R.string.card_avg_desire)     to (if (data.todayDesire  != null) "${data.todayDesire}"  else "—"),
                stringResource(R.string.card_avg_comfort)    to (if (data.todayComfort != null) "${data.todayComfort}" else "—"),
                stringResource(R.string.card_avg_focus)      to (if (data.todayFocus   != null) "${data.todayFocus}"   else "—"),
                stringResource(R.string.card_avg_sleep)      to (if (data.todaySleep   != null) "${data.todaySleep}"   else "—"),
            ).forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, color = accentColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(label, color = subColor, fontSize = 26.sp)
                }
            }
        }

        // Self rating stars — removed from card (selfRating is recorded but not shown on card)
    }
}

@Composable
private fun PhotoSection(data: CardData, textColor: Color, subColor: Color) {
    if (!data.showPhoto || data.photoPath == null) return

    // Load bitmap synchronously with EXIF-corrected rotation (runs on composition thread;
    // remember() only re-runs when photoPath changes, so main-thread cost is a one-off).
    val (imgBitmap, isLandscape) = remember(data.photoPath) {
        runCatching {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 } // limit memory
            val raw = BitmapFactory.decodeFile(data.photoPath, opts)
                ?: return@runCatching null
            val exif = ExifInterface(data.photoPath)
            val degrees = when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else                                 -> 0f
            }
            val rotated = if (degrees == 0f) raw
            else Bitmap.createBitmap(
                raw, 0, 0, raw.width, raw.height,
                Matrix().apply { postRotate(degrees) }, true
            )
            rotated.asImageBitmap() to (rotated.width > rotated.height)
        }.getOrNull()
    } ?: return

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.card_photo_section),
            color = subColor, fontSize = 22.sp, fontWeight = FontWeight.Medium
        )
        Image(
            bitmap = imgBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 440.dp else 760.dp)
                .clip(RoundedCornerShape(24.dp))
        )
    }
}

@Composable
private fun AvgSection(data: CardData, accentColor: Color, textColor: Color, subColor: Color) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(R.string.card_7d_average), color = subColor, fontSize = 22.sp,
            fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                stringResource(R.string.card_avg_desire) to data.avg7Desire,
                stringResource(R.string.card_avg_comfort) to data.avg7Comfort,
                stringResource(R.string.card_avg_focus) to data.avg7Focus,
                stringResource(R.string.card_avg_sleep) to data.avg7Sleep
            ).forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.1f".format(value), color = accentColor,
                        fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(label, color = subColor, fontSize = 26.sp)
                }
            }
        }
    }
}

@Composable
private fun CheckInsSection(data: CardData, accentColor: Color, textColor: Color, subColor: Color) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            CheckChip(
                icon = if (data.exercised) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                label = stringResource(R.string.card_exercised),
                active = data.exercised,
                accentColor = accentColor,
                textColor = textColor
            )
        }
        // Rotating questions — resolve strings here (Composable context = correct locale)
        data.rotatingQuestions.forEach { (key, rawValue) ->
            val titleRes = rotatingQuestionTitleRes(key) ?: return@forEach
            val label  = stringResource(titleRes)
            val answer = when (rawValue) {
                "true"  -> stringResource(R.string.chip_yes)
                "false" -> stringResource(R.string.chip_no)
                else    -> rawValue
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = accentColor,
                        modifier = Modifier.size(28.dp))
                    Text(
                        label,
                        color = subColor,
                        fontSize = 24.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "「${answer}」",
                    color = textColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }
        }
    }
}

@Composable
private fun CheckChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accentColor: Color,
    textColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = if (active) accentColor else textColor.copy(alpha = 0.4f),
            modifier = Modifier.size(32.dp))
        Text(label, color = if (active) textColor else textColor.copy(alpha = 0.4f), fontSize = 22.sp)
    }
}

@Composable
private fun CardFooter(textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("CB Diary  •  chastity.diary", color = textColor, fontSize = 24.sp)
        // QR code / app link placeholder — reserved for a future update
        Canvas(modifier = Modifier.size(112.dp)) {
            val dashOn = 6f; val dashOff = 4f
            drawRoundRect(
                color = textColor.copy(alpha = 0.35f),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(dashOn, dashOff)
                    )
                )
            )
        }
    }
}

// ── CardBottomSheet ───────────────────────────────────────────────────────────

/**
 * Modal bottom sheet that shows a scaled card preview, a horizontal theme picker,
 * and Share / Save buttons.
 *
 * @param onDismiss called when the sheet should close.
 */
@Composable
fun CardBottomSheet(
    onDismiss: () -> Unit,
    viewModel: CardViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity

    val cardData by viewModel.cardData.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val themes by viewModel.availableThemes.collectAsState()
    val isRendering by viewModel.isRendering.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    stringResource(R.string.card_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // ── Card preview ─────────────────────────────────────────────
                if (cardData != null) {
                    // Correct Compose scaling: the `layout` modifier reports the
                    // SCALED dimensions to the layout system so no overflow occurs,
                    // while placeWithLayer applies the visual scale from the top-left.
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .aspectRatio(CARD_RATIO_W / CARD_RATIO_H)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        val scaleFactor = maxWidth.value / CARD_RATIO_W
                        Box(
                            modifier = Modifier.layout { measurable, _ ->
                                val cardWPx = (CARD_RATIO_W * density).toInt()
                                val cardHPx = (CARD_RATIO_H * density).toInt()
                                val placeable = measurable.measure(
                                    androidx.compose.ui.unit.Constraints(
                                        minWidth = cardWPx, maxWidth = cardWPx,
                                        minHeight = cardHPx, maxHeight = cardHPx
                                    )
                                )
                                layout(
                                    (cardWPx * scaleFactor).toInt(),
                                    (cardHPx * scaleFactor).toInt()
                                ) {
                                    placeable.placeWithLayer(0, 0) {
                                        scaleX = scaleFactor
                                        scaleY = scaleFactor
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    }
                                }
                            }
                        ) {
                            SummaryCardContent(
                                data = cardData!!,
                                theme = selectedTheme
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(CARD_RATIO_W / CARD_RATIO_H)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.card_no_entry_today),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── Theme picker ─────────────────────────────────────────────
                Text(
                    stringResource(R.string.card_choose_theme),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(themes, key = { it.id }) { theme ->
                        ThemeChip(
                            theme = theme,
                            selected = selectedTheme.id == theme.id,
                            unlocked = true,
                            onSelect = { viewModel.selectTheme(theme.id) },
                            onDelete = theme.userTemplateId?.let {
                                { viewModel.deleteUserTemplate(it) }
                            }
                        )
                    }
                }

                // ── Photo toggle ─────────────────────────────────────────────
                val showPhoto by viewModel.showPhoto.collectAsState()
                val hasPhoto = cardData?.photoPath != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.card_show_photo),
                            style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.card_show_photo_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showPhoto && hasPhoto,
                        enabled = hasPhoto,
                        onCheckedChange = { viewModel.showPhoto.value = it }
                    )
                }

                // ── Action buttons ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { activity?.let { viewModel.generateAndSave(it) } },
                        enabled = cardData != null && !isRendering,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        if (isRendering) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.card_save))
                        }
                    }
                    Button(
                        onClick = { activity?.let { viewModel.generateAndShare(it) } },
                        enabled = cardData != null && !isRendering,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        if (isRendering) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.card_share))
                        }
                    }
                }
            }
        }
    }

}

// ── ThemeChip ─────────────────────────────────────────────────────────────────

@Composable
private fun ThemeChip(
    theme: CardTheme,
    selected: Boolean,
    unlocked: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val bc = when (val src = theme.backgroundSource) {
        is BackgroundSource.Gradient -> src.colors.first()
        is BackgroundSource.CanvasPattern -> src.baseColors.first()
        is BackgroundSource.ExternalAsset -> Color.Gray
    }

    Box(contentAlignment = Alignment.TopEnd) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onSelect() }
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bc)
                    .then(
                        if (selected) Modifier.border(
                            3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!unlocked) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(22.dp))
                } else if (selected) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Text(
                stringResource(theme.nameResId),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 64.dp)
            )
        }
        // Delete badge for user-imported templates
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .offset(x = 4.dp, y = (-4).dp)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White,
                    modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun ImportThemeChip(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp))
        }
        Text(
            stringResource(R.string.card_import_template),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 64.dp)
        )
    }
}

// ── SponsorCodeDialog ─────────────────────────────────────────────────────────

@Composable
fun SponsorCodeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.sponsor_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.sponsor_dialog_body),
                    style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = false },
                    label = { Text(stringResource(R.string.sponsor_code_hint)) },
                    isError = error,
                    supportingText = if (error) {{ Text(stringResource(R.string.sponsor_code_invalid)) }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                error = !onSubmit(code)
            }) { Text(stringResource(R.string.sponsor_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.onboarding_skip)) }
        }
    )
}

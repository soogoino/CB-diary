@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.chastity.diary.ui.screens

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.chastity.diary.viewmodel.CardViewModel
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ── Constants ─────────────────────────────────────────────────────────────────

private val CARD_RATIO_W = 1080f
private val CARD_RATIO_H = 1350f
private val CARD_DATE_FMT = DateTimeFormatter.ofPattern("yyyy / MM / dd")

// ── SummaryCardContent ────────────────────────────────────────────────────────

/**
 * The full-resolution card Composable.
 * Designed at 1080×1350 logical pixels; wrap in a scaled [Box] for previews.
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
                textColor = textColor
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Lock, null, tint = textColor, modifier = Modifier.size(20.dp))
            Text("CB diary", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text(data.date.format(CARD_DATE_FMT), color = subColor, fontSize = 14.sp)
    }
}

@Composable
private fun StreakSection(data: CardData, accentColor: Color, textColor: Color, subColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Streak ring
        Box(contentAlignment = Alignment.Center) {
            CircleRing(progress = (data.currentStreak.toFloat() / data.longestStreak.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f),
                accentColor = accentColor, size = 140.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", fontSize = 28.sp)
                Text(
                    data.currentStreak.toString(),
                    color = textColor, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(stringResource(R.string.card_days_streak), color = subColor, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${stringResource(R.string.card_longest)}: ${data.longestStreak} ${stringResource(R.string.card_days)}",
            color = subColor, fontSize = 13.sp
        )
    }
}

@Composable
private fun CircleRing(progress: Float, accentColor: Color, size: Dp) {
    val animP by animateFloatAsState(progress, label = "ring")
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 10f
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
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.card_today), color = subColor, fontSize = 11.sp,
            fontWeight = FontWeight.Medium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // Mood
            Column {
                Text(stringResource(R.string.card_mood), color = subColor, fontSize = 10.sp)
                Text(data.morningMood ?: "—", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            // Morning energy
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.card_morning_energy), color = subColor, fontSize = 10.sp)
                Text(
                    if (data.morningEnergy != null) "${data.morningEnergy}/10" else "—",
                    color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        // Self rating stars
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.card_self_rating), color = subColor, fontSize = 10.sp)
            Spacer(Modifier.width(4.dp))
            val stars = data.selfRating ?: 0
            repeat(5) { i ->
                Text(if (i < stars) "★" else "☆", color = accentColor, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun AvgSection(data: CardData, accentColor: Color, textColor: Color, subColor: Color) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.card_7d_average), color = subColor, fontSize = 11.sp,
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
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(label, color = subColor, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun CheckInsSection(data: CardData, accentColor: Color, textColor: Color) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CheckChip(
                icon = if (data.exercised) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                label = stringResource(R.string.card_exercised),
                active = data.exercised,
                accentColor = accentColor,
                textColor = textColor
            )
            if (data.showSensitiveData) {
                CheckChip(
                    icon = if (data.exposedDevice) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    label = stringResource(R.string.card_device_exposed),
                    active = data.exposedDevice,
                    accentColor = accentColor,
                    textColor = textColor
                )
            }
        }
        // Rotating question
        if (data.rotatingQuestionLabel != null && data.rotatingQuestionAnswer != null) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = accentColor,
                    modifier = Modifier.size(14.dp).padding(top = 2.dp))
                Text(
                    "「${data.rotatingQuestionAnswer}」",
                    color = textColor,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = if (active) accentColor else textColor.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp))
        Text(label, color = if (active) textColor else textColor.copy(alpha = 0.4f), fontSize = 11.sp)
    }
}

@Composable
private fun CardFooter(textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("CB Diary  •  chastity.diary", color = textColor, fontSize = 10.sp)
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
    val sponsorUnlocked by viewModel.sponsorUnlocked.collectAsState()
    val isRendering by viewModel.isRendering.collectAsState()

    var showSponsorDialog by remember { mutableStateOf(false) }
    var snackMessage by remember { mutableStateOf<String?>(null) }
    val snackState = remember { SnackbarHostState() }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.importTemplate(uri) { errResId ->
            snackMessage = if (errResId == null)
                context.getString(R.string.card_import_success)
            else
                context.getString(errResId)
        }
    }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackState.showSnackbar(it)
            snackMessage = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Scaffold(snackbarHost = { SnackbarHost(snackState) },
            containerColor = Color.Transparent) { padding ->
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(CARD_RATIO_W / CARD_RATIO_H)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewScale = remember { mutableStateOf(1f) }
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val scale = maxWidth.value / CARD_RATIO_W
                            previewScale.value = scale
                            Box(
                                modifier = Modifier
                                    .size(CARD_RATIO_W.dp, CARD_RATIO_H.dp)
                                    .scale(scale)
                            ) {
                                SummaryCardContent(
                                    data = cardData!!,
                                    theme = selectedTheme
                                )
                            }
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
                            unlocked = sponsorUnlocked || !theme.isPremium,
                            onSelect = {
                                if (sponsorUnlocked || !theme.isPremium) {
                                    viewModel.selectTheme(theme.id)
                                } else {
                                    showSponsorDialog = true
                                }
                            },
                            onDelete = theme.userTemplateId?.let {
                                { viewModel.deleteUserTemplate(it) }
                            }
                        )
                    }
                    // ＋ Import template button
                    item {
                        ImportThemeChip(onClick = { importLauncher.launch("application/zip") })
                    }
                }

                // ── Sensitive data toggle ────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.card_show_sensitive),
                            style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.card_show_sensitive_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val showSensitive by viewModel.showSensitiveData.collectAsState()
                    Switch(
                        checked = showSensitive,
                        onCheckedChange = { viewModel.showSensitiveData.value = it }
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

    // ── Sponsor code dialog ───────────────────────────────────────────────────
    if (showSponsorDialog) {
        SponsorCodeDialog(
            onDismiss = { showSponsorDialog = false },
            onSubmit = { code ->
                val ok = viewModel.submitSponsorCode(code)
                if (ok) showSponsorDialog = false
                ok
            }
        )
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

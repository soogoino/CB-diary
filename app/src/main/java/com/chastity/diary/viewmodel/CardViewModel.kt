package com.chastity.diary.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.chastity.diary.R
import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.data.local.database.AppDatabase
import com.chastity.diary.data.repository.EntryRepository
import com.chastity.diary.data.repository.SettingsRepository
import com.chastity.diary.data.repository.StreakRepository
import com.chastity.diary.domain.model.CardData
import com.chastity.diary.domain.model.CardTheme
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.domain.model.BackgroundSource
import com.chastity.diary.domain.model.CardTemplateSpec
import com.chastity.diary.domain.model.TextColorScheme
import com.chastity.diary.domain.model.rotatingQuestionTitleRes
import com.chastity.diary.ui.screens.SummaryCardContent
import com.chastity.diary.ui.theme.CardThemes
import com.chastity.diary.util.CardRenderer
import com.chastity.diary.util.SponsorManager
import com.chastity.diary.util.TemplateImporter
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * ViewModel backing the summary card generation UI.
 *
 * Aggregates data from [EntryRepository], [StreakRepository] and [SettingsRepository]
 * into [CardData] and exposes theme selection, sponsor-code verification and
 * one-shot render/share/save actions.
 */
class CardViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val settingsRepo = SettingsRepository(preferencesManager)
    private val db = AppDatabase.getInstance(application)
    private val entryRepo = EntryRepository(db.dailyEntryDao(), db.dailyEntryAttributeDao())
    private val streakRepo = StreakRepository(preferencesManager)

    init {
        // Restore user-imported templates that survived process death.
        viewModelScope.launch {
            val persisted = TemplateImporter.loadUserTemplates(getApplication())
            if (persisted.isNotEmpty()) _userTemplates.value = persisted
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    private val userSettings = settingsRepo.userSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val sponsorUnlocked: StateFlow<Boolean> = userSettings
        .map { it?.sponsorUnlocked ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Theme selection ───────────────────────────────────────────────────────

    /** User-imported templates added via [importTemplate]. */
    private val _userTemplates = MutableStateFlow<List<CardTheme>>(emptyList())

    /** All available themes: built-in + user-imported. */
    val availableThemes: StateFlow<List<CardTheme>> = combine(
        _userTemplates,
        sponsorUnlocked
    ) { userTemplates, unlocked ->
        buildList {
            addAll(CardThemes.ALL)
            addAll(userTemplates)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CardThemes.ALL)

    val selectedTheme: StateFlow<CardTheme> = combine(
        userSettings,
        availableThemes
    ) { settings, themes ->
        val id = settings?.selectedCardThemeId ?: "midnight"
        themes.find { it.id == id } ?: CardThemes.MIDNIGHT
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CardThemes.MIDNIGHT)

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            settingsRepo.updateCardThemeId(themeId)
        }
    }

    // ── Photo toggle ────────────────────────────────────────────────────────

    /** Whether to include today’s photo on the generated card (user opt-in). */
    val showPhoto = MutableStateFlow(false)

    // ── Target date ───────────────────────────────────────────────────────────

    /**
     * The date for which the card should be generated.
     * Defaults to today; updated by [setDate] when the user opens the sheet
     * while viewing a non-today entry.
     */
    private val _targetDate = MutableStateFlow(LocalDate.now())

    /** Update the card date; resets [showPhoto] to avoid stale state. */
    fun setDate(date: LocalDate) {
        _targetDate.value = date
        showPhoto.value = false
    }

    // ── Card data ─────────────────────────────────────────────────────────────

    /**
     * Resolved card data for [_targetDate], or null if no entry exists yet.
     * Re-emits automatically whenever the database, streak or target date changes.
     */
    private val _nickname: StateFlow<String?> = userSettings
        .map { it?.nickname?.takeIf { n -> n.isNotBlank() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val cardData: StateFlow<CardData?> = combine(_targetDate, _nickname) { date, nick -> date to nick }
        .flatMapLatest { (targetDate, nickname) ->
        combine(
            entryRepo.getAllEntries(),                                        // for 7-day averages
            entryRepo.getEntryByDateWithAttributesFlow(targetDate),           // target date entry
            streakRepo.currentStreak,
            streakRepo.longestStreak,
            showPhoto
        ) { allEntries, entryOrNull, curStreak, longestStreak, showPhoto ->
            val entry: DailyEntry = entryOrNull ?: return@combine null

            // 7-day rolling window
            val sevenDaysAgo = targetDate.minusDays(6)
            val recentEntries = allEntries.filter { !it.date.isBefore(sevenDaysAgo) }

            fun List<DailyEntry>.avg(selector: (DailyEntry) -> Float?) =
                mapNotNull(selector).average().let { if (it.isNaN()) 0f else it.toFloat() }

            // Rotating questions: collect ALL answered questions for the target date.
            // Store raw (key, rawValue) — string resolution happens in the Composable
            // so it always uses the correct current-locale context.
            val rotatingQuestions = entry.rotatingAnswers.entries
                .filter { (key, _) -> rotatingQuestionTitleRes(key) != null }  // skip unknown keys
                .map { (key, value) -> key to value }

            CardData(
                date = targetDate,
                currentStreak = curStreak,
                longestStreak = longestStreak,
                morningMood = entry.morningMood,
                morningEnergy = entry.morningEnergy,
                exercised = entry.exercised,
                photoPath = entry.photoPath,
                showPhoto = showPhoto,
                rotatingQuestions = rotatingQuestions,
                todayDesire = entry.desireLevel,
                todayComfort = entry.comfortRating,
                todayFocus = entry.focusLevel,
                todaySleep = entry.sleepQuality,
                avg7Desire = recentEntries.avg { it.desireLevel?.toFloat() },
                avg7Comfort = recentEntries.avg { it.comfortRating?.toFloat() },
                avg7Focus = recentEntries.avg { it.focusLevel?.toFloat() },
                avg7Sleep = recentEntries.avg { it.sleepQuality?.toFloat() },
                nickname = nickname,
                totalDays = allEntries.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Generate + share/save ─────────────────────────────────────────────────

    private val _isRendering = MutableStateFlow(false)
    val isRendering: StateFlow<Boolean> = _isRendering.asStateFlow()

    fun generateAndShare(activity: Activity) {
        val data = cardData.value ?: return
        val theme = selectedTheme.value
        viewModelScope.launch {
            _isRendering.value = true
            try {
                val bitmap = CardRenderer.renderToBitmap(activity) {
                    SummaryCardContent(data = data, theme = theme)
                }
                val file = CardRenderer.saveToCache(activity, bitmap, data)
                CardRenderer.shareCard(activity, file)
            } catch (e: Exception) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.card_render_error),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                _isRendering.value = false
            }
        }
    }

    fun generateAndSave(activity: Activity) {
        val data = cardData.value ?: return
        val theme = selectedTheme.value
        viewModelScope.launch {
            _isRendering.value = true
            try {
                val bitmap = CardRenderer.renderToBitmap(activity) {
                    SummaryCardContent(data = data, theme = theme)
                }
                val file = CardRenderer.saveToCache(activity, bitmap, data)
                val uri = CardRenderer.saveToGallery(activity, file, data)
                Toast.makeText(
                    activity,
                    if (uri != null) activity.getString(R.string.card_saved_to_gallery)
                    else activity.getString(R.string.card_render_error),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.card_render_error),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                _isRendering.value = false
            }
        }
    }

    // ── Sponsor code ──────────────────────────────────────────────────────────

    /**
     * Validates [code] and, if correct, persists the unlocked state.
     * @return `true` if the code was valid.
     */
    fun submitSponsorCode(code: String): Boolean {
        if (!SponsorManager.isValidCode(code)) return false
        viewModelScope.launch { settingsRepo.setSponsorUnlocked(true) }
        return true
    }

    // ── User template import ──────────────────────────────────────────────────

    /**
     * Imports a single PNG/JPG image as a card background.
     * The image is scaled to 1080×1920 automatically; text layout uses [CardTemplateSpec.DEFAULT].
     *
     * @param imageUri SAF URI of the selected image.
     * @param scheme   Text colour scheme to overlay on the background.
     * @param onResult Called with null on success, or a string-resource id for the error message.
     */
    fun importSingleImage(imageUri: Uri, scheme: TextColorScheme, onResult: (Int?) -> Unit) {
        viewModelScope.launch {
            val result = TemplateImporter.importSingleImage(getApplication(), imageUri, scheme)
            result.onSuccess { theme ->
                _userTemplates.value = _userTemplates.value + theme
                onResult(null)
            }.onFailure { err ->
                val resId = (err as? TemplateImporter.ImportException)?.messageResId
                    ?: R.string.card_import_error_unknown
                onResult(resId)
            }
        }
    }

    /**
     * Imports a user-designed `.zip` template and appends it to [availableThemes].
     * @return import error string resource id, or null on success.
     */
    fun importTemplate(zipUri: Uri, onResult: (Int?) -> Unit) {
        viewModelScope.launch {
            val result = TemplateImporter.import(getApplication(), zipUri)
            result.onSuccess { theme ->
                _userTemplates.value = _userTemplates.value + theme
                onResult(null)
            }.onFailure { err ->
                val resId = (err as? TemplateImporter.ImportException)?.messageResId
                    ?: R.string.card_import_error_unknown
                onResult(resId)
            }
        }
    }

    /**
     * Deletes a user-imported template by its [userTemplateId].
     * If the deleted theme is currently selected, reverts to "midnight".
     */
    fun deleteUserTemplate(userTemplateId: String) {
        viewModelScope.launch {
            _userTemplates.value = _userTemplates.value.filter {
                it.userTemplateId != userTemplateId
            }
            if (selectedTheme.value.userTemplateId == userTemplateId) {
                settingsRepo.updateCardThemeId("midnight")
            }
            TemplateImporter.delete(getApplication(), userTemplateId)
        }
    }

    /**
     * Swaps the text colour scheme of a user-imported template in-memory and
     * persists the updated spec to disk so the change survives restarts.
     *
     * The `_userTemplates` StateFlow update triggers a recomposition of the
     * card preview immediately (live preview).
     */
    fun updateUserTemplateTextColor(userTemplateId: String, scheme: TextColorScheme) {
        viewModelScope.launch {
            val schemeStr = if (scheme == TextColorScheme.DARK) "dark" else "light"
            val accent = if (scheme == TextColorScheme.LIGHT) Color.White else Color.Black

            _userTemplates.value = _userTemplates.value.map { theme ->
                if (theme.userTemplateId != userTemplateId) return@map theme
                val updatedSrc = (theme.backgroundSource as? BackgroundSource.ExternalAsset)
                    ?.let { it.copy(spec = it.spec.copy(textColorScheme = schemeStr)) }
                    ?: theme.backgroundSource
                theme.copy(
                    backgroundSource = updatedSrc,
                    textColorScheme = scheme,
                    accentColor = accent
                )
            }

            // Persist the new scheme to disk so loadUserTemplates() restores it correctly.
            withContext(Dispatchers.IO) {
                val specFile = File(
                    getApplication<Application>().filesDir,
                    "templates/$userTemplateId/card_template_spec.json"
                )
                if (specFile.exists()) {
                    val spec = Gson().fromJson(specFile.readText(), CardTemplateSpec::class.java)
                    specFile.writeText(Gson().toJson(spec.copy(textColorScheme = schemeStr)))
                }
            }
        }
    }
}

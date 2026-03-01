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
import com.chastity.diary.ui.screens.SummaryCardContent
import com.chastity.diary.ui.theme.CardThemes
import com.chastity.diary.util.CardRenderer
import com.chastity.diary.util.SponsorManager
import com.chastity.diary.util.TemplateImporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    // ── Privacy toggle ─────────────────────────────────────────────────────────

    /** Whether to show sensitive fields (exposed device status) on the card. */
    val showSensitiveData = MutableStateFlow(false)

    // ── Card data ─────────────────────────────────────────────────────────────

    /**
     * Resolved card data for today, or null if no entry exists yet.
     * Re-emits automatically whenever the database or streak changes.
     */
    val cardData: StateFlow<CardData?> = combine(
        entryRepo.getAllEntries(),
        streakRepo.currentStreak,
        streakRepo.longestStreak,
        showSensitiveData
    ) { allEntries, curStreak, longestStreak, showSensitive ->
        val today = LocalDate.now()
        val todayEntry: DailyEntry = allEntries.find { it.date == today }
            ?: return@combine null

        // 7-day rolling window
        val sevenDaysAgo = today.minusDays(6)
        val recentEntries = allEntries.filter { !it.date.isBefore(sevenDaysAgo) }

        fun List<DailyEntry>.avg(selector: (DailyEntry) -> Float?) =
            mapNotNull(selector).average().let { if (it.isNaN()) 0f else it.toFloat() }

        // Rotating question: take the first answer from today's rotatingAnswers map
        val rotatingPair = todayEntry.rotatingAnswers.entries.firstOrNull()

        CardData(
            date = today,
            currentStreak = curStreak,
            longestStreak = longestStreak,
            morningMood = todayEntry.morningMood,
            morningEnergy = todayEntry.morningEnergy,
            selfRating = todayEntry.selfRating,
            exercised = todayEntry.exercised,
            exposedDevice = todayEntry.exposedLock,
            rotatingQuestionLabel = rotatingPair?.key,
            rotatingQuestionAnswer = rotatingPair?.value,
            avg7Desire = recentEntries.avg { it.desireLevel?.toFloat() },
            avg7Comfort = recentEntries.avg { it.comfortRating?.toFloat() },
            avg7Focus = recentEntries.avg { it.focusLevel?.toFloat() },
            avg7Sleep = recentEntries.avg { it.sleepQuality?.toFloat() },
            showSensitiveData = showSensitive
        )
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
}

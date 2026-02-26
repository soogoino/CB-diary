package com.chastity.diary.util

import com.chastity.diary.domain.model.DailyEntry
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Utility for CSV export/import of DailyEntry records.
 *
 * Format:
 *   - Comma-delimited fields, each field double-quoted.
 *   - List<String> fields: elements joined by "|" within the quoted cell.
 *   - Null values: empty quoted cell "".
 *   - Booleans: "true" / "false".
 *   - Dates: ISO-8601 (yyyy-MM-dd / yyyy-MM-ddTHH:mm:ss).
 */
object CsvHelper {

    // --------------- Column headers ---------------
    private val HEADERS = listOf(
        "id", "date",
        "mood",
        "viewedPorn", "pornDuration",
        "hadErection", "erectionCount",
        "exercised", "exerciseTypes", "exerciseDuration",
        "unlocked", "masturbated", "masturbationDuration", "masturbationCount",
        "exposedLock", "exposedLocations",
        "photoPath",
        "desireLevel",
        "comfortRating",
        "hasDiscomfort", "discomfortAreas", "discomfortLevel",
        "cleaningType",
        "hadLeakage", "leakageAmount",
        "hadEdging", "edgingDuration", "edgingMethods",
        "keyholderInteraction", "interactionTypes",
        "sleepQuality", "wokeUpDueToDevice",
        "temporarilyRemoved", "removalDuration", "removalReasons",
        "nightErections", "wokeUpFromErection",
        "focusLevel",
        "completedTasks",
        "emotions",
        "deviceCheckPassed",
        "socialActivities", "socialAnxiety",
        "selfRating",
        "bedtime", "wakeTime", "morningMood", "morningEnergy",
        "morningErection", "morningCheckDone", "hadEroticDream",
        "rotatingAnswers",
        "notes",
        "createdAt", "updatedAt"
    )

    // --------------- Export ---------------

    fun toCsv(entries: List<DailyEntry>): String {
        val sb = StringBuilder()
        sb.appendLine(HEADERS.joinToString(",") { q(it) })
        entries.forEach { e ->
            sb.appendLine(entryToRow(e))
        }
        return sb.toString()
    }

    private fun entryToRow(e: DailyEntry): String {
        val fields = listOf(
            q(e.id.toString()),
            q(e.date.toString()),
            q(e.mood),
            q(e.viewedPorn),
            q(e.pornDuration),
            q(e.hadErection),
            q(e.erectionCount),
            q(e.exercised),
            q(e.exerciseTypes),
            q(e.exerciseDuration),
            q(e.unlocked),
            q(e.masturbated),
            q(e.masturbationDuration),
            q(e.masturbationCount),
            q(e.exposedLock),
            q(e.exposedLocations),
            q(e.photoPath),
            q(e.desireLevel),
            q(e.comfortRating),
            q(e.hasDiscomfort),
            q(e.discomfortAreas),
            q(e.discomfortLevel),
            q(e.cleaningType),
            q(e.hadLeakage),
            q(e.leakageAmount),
            q(e.hadEdging),
            q(e.edgingDuration),
            q(e.edgingMethods),
            q(e.keyholderInteraction),
            q(e.interactionTypes),
            q(e.sleepQuality),
            q(e.wokeUpDueToDevice),
            q(e.temporarilyRemoved),
            q(e.removalDuration),
            q(e.removalReasons),
            q(e.nightErections),
            q(e.wokeUpFromErection),
            q(e.focusLevel),
            q(e.completedTasks),
            q(e.emotions),
            q(e.deviceCheckPassed),
            q(e.socialActivities),
            q(e.socialAnxiety),
            q(e.selfRating),
            q(e.bedtime?.toString()),
            q(e.wakeTime?.toString()),
            q(e.morningMood),
            q(e.morningEnergy),
            q(e.morningErection),
            q(e.morningCheckDone),
            q(e.hadEroticDream),
            q(e.rotatingAnswers.entries.joinToString("|") { "${it.key}=${it.value}" }),
            q(e.notes),
            q(e.createdAt.toString()),
            q(e.updatedAt.toString())
        )
        return fields.joinToString(",")
    }

    // --------------- Import ---------------

    /**
     * Parses CSV content and returns a list of DailyEntry objects.
     * Rows that fail to parse are silently skipped.
     */
    fun fromCsv(csvContent: String): List<DailyEntry> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()
        // Validate header
        val headerCols = parseCsvLine(lines[0])
        if (!headerCols.containsAll(listOf("date", "createdAt"))) return emptyList()
        val colIndex = headerCols.withIndex().associate { (i, v) -> v to i }

        return lines.drop(1).mapNotNull { line ->
            try {
                rowToEntry(parseCsvLine(line), colIndex)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun rowToEntry(cols: List<String>, idx: Map<String, Int>): DailyEntry {
        fun col(name: String): String = cols.getOrElse(idx[name] ?: -1) { "" }
        fun bool(name: String, default: Boolean = false) = col(name).toBooleanStrictOrNull() ?: default
        fun int(name: String): Int? = col(name).toIntOrNull()
        fun list(name: String): List<String> = col(name).let {
            if (it.isBlank()) emptyList() else it.split("|").filter { s -> s.isNotBlank() }
        }

        return DailyEntry(
            id = col("id").toLongOrNull() ?: 0L,
            date = LocalDate.parse(col("date")),
            mood = col("mood").ifBlank { null },
            viewedPorn = bool("viewedPorn"),
            pornDuration = int("pornDuration"),
            hadErection = bool("hadErection"),
            erectionCount = int("erectionCount"),
            exercised = bool("exercised"),
            exerciseTypes = list("exerciseTypes"),
            exerciseDuration = int("exerciseDuration"),
            unlocked = bool("unlocked"),
            masturbated = bool("masturbated"),
            masturbationDuration = int("masturbationDuration"),
            masturbationCount = int("masturbationCount"),
            exposedLock = bool("exposedLock"),
            exposedLocations = list("exposedLocations"),
            photoPath = col("photoPath").ifBlank { null },
            desireLevel = int("desireLevel"),
            comfortRating = int("comfortRating"),
            hasDiscomfort = bool("hasDiscomfort"),
            discomfortAreas = list("discomfortAreas"),
            discomfortLevel = int("discomfortLevel"),
            cleaningType = col("cleaningType").ifBlank { null },
            hadLeakage = bool("hadLeakage"),
            leakageAmount = col("leakageAmount").ifBlank { null },
            hadEdging = bool("hadEdging"),
            edgingDuration = int("edgingDuration"),
            edgingMethods = list("edgingMethods"),
            keyholderInteraction = bool("keyholderInteraction"),
            interactionTypes = list("interactionTypes"),
            sleepQuality = int("sleepQuality"),
            wokeUpDueToDevice = bool("wokeUpDueToDevice"),
            temporarilyRemoved = bool("temporarilyRemoved"),
            removalDuration = int("removalDuration"),
            removalReasons = list("removalReasons"),
            nightErections = int("nightErections"),
            wokeUpFromErection = bool("wokeUpFromErection"),
            focusLevel = int("focusLevel"),
            completedTasks = list("completedTasks"),
            emotions = list("emotions"),
            deviceCheckPassed = bool("deviceCheckPassed", true),
            socialActivities = list("socialActivities"),
            socialAnxiety = int("socialAnxiety"),
            selfRating = int("selfRating"),
            bedtime = runCatching { java.time.LocalTime.parse(col("bedtime")) }.getOrNull(),
            wakeTime = runCatching { java.time.LocalTime.parse(col("wakeTime")) }.getOrNull(),
            morningMood = col("morningMood").ifBlank { null },
            morningEnergy = int("morningEnergy"),
            morningErection = bool("morningErection"),
            morningCheckDone = bool("morningCheckDone"),
            hadEroticDream = bool("hadEroticDream"),
            rotatingAnswers = col("rotatingAnswers").let { raw ->
                if (raw.isBlank()) emptyMap()
                else raw.split("|").mapNotNull { pair ->
                    val eq = pair.indexOf('=')
                    if (eq > 0) pair.substring(0, eq) to pair.substring(eq + 1) else null
                }.toMap()
            },
            notes = col("notes").ifBlank { null },
            createdAt = runCatching { LocalDateTime.parse(col("createdAt")) }.getOrDefault(LocalDateTime.now()),
            updatedAt = runCatching { LocalDateTime.parse(col("updatedAt")) }.getOrDefault(LocalDateTime.now())
        )
    }

    // --------------- CSV helpers ---------------

    /** Quote a value for a CSV cell. Embedded quotes are doubled. */
    private fun q(value: String?): String {
        val v = value ?: ""
        return "\"${v.replace("\"", "\"\"")}\""
    }
    private fun q(value: Boolean): String = q(value.toString())
    private fun q(value: Int?): String = q(value?.toString())
    private fun q(value: List<String>): String = q(value.joinToString("|"))

    /**
     * Simple RFC-4180 CSV line parser.
     * Handles quoted fields with embedded commas and escaped double-quotes.
     */
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++ // escaped quote
                }
                c == '"' && inQuotes -> inQuotes = false
                c == ',' && !inQuotes -> {
                    result.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        // Q3: Detect unclosed quote — row is malformed; caller's try/catch will skip it
        if (inQuotes) throw IllegalArgumentException("Unclosed quote in CSV line: ${line.take(80)}")
        result.add(sb.toString())
        return result
    }
}

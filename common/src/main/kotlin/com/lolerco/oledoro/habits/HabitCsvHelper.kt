package com.lolerco.oledoro.habits

import com.lolerco.oledoro.data.GruvboxColor
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt

/**
 * Secure CSV and ZIP Import and Export for Habidoro and Loop Habit Tracker (uhabits) compatibility.
 *
 * Supports:
 * 1. Habidoro native CSV format:
 *    - Header: id,name,color,createdAt,targetDaysPerWeek,completedDates,skippedDates
 * 2. uhabits (Loop Habit Tracker) exports:
 *    - Checkmarks matrix CSV: Date column (YYYY-MM-DD), followed by habit names, values 2=completed, 1=skipped, 0=off
 *    - Habits definition CSV: name, description, frequency_num, frequency_den, color...
 *    - Exported Archive (.zip): containing both habits.csv and checkmarks.csv, merged seamlessly
 * 3. CSV / Formula injection prevention:
 *    - Neutralizes formula triggers (=, +, -, @, tab, carriage return)
 *    - Strict CSV quoting and RFC-4180 parsing
 */
object HabitCsvHelper {

    private const val HEADER_NATIVE = "id,name,color,createdAt,targetDaysPerWeek,completedDates,skippedDates"

    /**
     * Securely exports habits into standard CSV output stream.
     * Sanitizes values against CSV injection (=, +, -, @ prefix protection) and handles commas/quotes properly.
     */
    fun exportToCsv(habits: List<Habit>, outputStream: OutputStream) {
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine(HEADER_NATIVE)
            for (habit in habits) {
                val safeId = sanitizeCsvCell(habit.id)
                val safeName = sanitizeCsvCell(habit.name)
                val safeColor = sanitizeCsvCell(habit.color.name)
                val safeCreatedAt = sanitizeCsvCell(habit.createdAt)
                val target = habit.targetDaysPerWeek.coerceIn(1, 7)
                val completedJoined = sanitizeCsvCell(habit.completedDates.sorted().joinToString(";"))
                val skippedJoined = sanitizeCsvCell(habit.skippedDates.sorted().joinToString(";"))

                writer.appendLine("$safeId,$safeName,$safeColor,$safeCreatedAt,$target,$completedJoined,$skippedJoined")
            }
            writer.flush()
        }
    }

    /**
     * Imports habits from an input stream (CSV or uhabits ZIP export).
     */
    fun importFromCsv(inputStream: InputStream): List<Habit> {
        val buffered = if (inputStream is BufferedInputStream) inputStream else BufferedInputStream(inputStream)
        buffered.mark(8)
        val magic = ByteArray(4)
        val readBytes = buffered.read(magic, 0, 4)
        buffered.reset()

        // Check if stream is a ZIP archive (starts with PK.. 0x50, 0x4B, 0x03, 0x04)
        val isZip = readBytes == 4 && magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() && magic[2] == 0x03.toByte() && magic[3] == 0x04.toByte()
        return if (isZip) {
            importFromZipStream(buffered)
        } else {
            importFromCsvStream(buffered)
        }
    }

    private fun importFromZipStream(inputStream: InputStream): List<Habit> {
        val zip = ZipInputStream(inputStream)
        var habitsCsvText: String? = null
        var checkmarksCsvText: String? = null

        while (true) {
            val entry = zip.nextEntry ?: break
            val entryName = entry.name.lowercase()
            when {
                entryName.endsWith("habits.csv") -> {
                    habitsCsvText = zip.bufferedReader(Charsets.UTF_8).readText()
                }
                entryName.endsWith("checkmarks.csv") -> {
                    checkmarksCsvText = zip.bufferedReader(Charsets.UTF_8).readText()
                }
            }
            zip.closeEntry()
        }

        val definedHabits = habitsCsvText?.let { importFromCsvStream(it.byteInputStream()) } ?: emptyList()
        val checkmarkedHabits = checkmarksCsvText?.let { importFromCsvStream(it.byteInputStream()) } ?: emptyList()

        if (definedHabits.isEmpty() && checkmarkedHabits.isEmpty()) return emptyList()
        if (definedHabits.isEmpty()) return checkmarkedHabits
        if (checkmarkedHabits.isEmpty()) return definedHabits

        // Merge checkmarks into defined habits matching by habit name
        val checkmarkByName = checkmarkedHabits.associateBy { it.name.trim().lowercase() }
        val merged = mutableListOf<Habit>()
        for (habit in definedHabits) {
            val cm = checkmarkByName[habit.name.trim().lowercase()]
            if (cm != null) {
                merged.add(
                    habit.copy(
                        completedDates = cm.completedDates,
                        skippedDates = cm.skippedDates
                    )
                )
            } else {
                merged.add(habit)
            }
        }
        return merged
    }

    private fun importFromCsvStream(inputStream: InputStream): List<Habit> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val firstLine = lines.first()
        val headerTokens = parseCsvRow(firstLine)

        return when {
            // 1. Habidoro native CSV
            headerTokens.contains("completedDates") || (headerTokens.size >= 5 && headerTokens[0] == "id" && headerTokens[1] == "name") -> {
                importNativeCsv(lines.drop(1), headerTokens)
            }
            // 2. uhabits checkmarks matrix (Header starts with Date, subsequent columns are Habit names)
            headerTokens.isNotEmpty() && headerTokens[0].equals("Date", ignoreCase = true) -> {
                importUhabitsMatrixCsv(lines.drop(1), headerTokens)
            }
            // 3. uhabits habits definition format (name, description, color, freq_num, freq_den...)
            headerTokens.any { it.equals("frequency_num", ignoreCase = true) || it.equals("frequency_den", ignoreCase = true) } -> {
                importUhabitsDefinitionsCsv(lines.drop(1), headerTokens)
            }
            // Fallback: heuristic record-based parser
            else -> {
                importHeuristicCsv(lines)
            }
        }
    }

    private fun importNativeCsv(dataLines: List<String>, headerTokens: List<String>): List<Habit> {
        val idIdx = headerTokens.indexOf("id").takeIf { it >= 0 } ?: 0
        val nameIdx = headerTokens.indexOf("name").takeIf { it >= 0 } ?: 1
        val colorIdx = headerTokens.indexOf("color").takeIf { it >= 0 } ?: 2
        val createdIdx = headerTokens.indexOf("createdAt").takeIf { it >= 0 } ?: 3
        val targetIdx = headerTokens.indexOf("targetDaysPerWeek").takeIf { it >= 0 } ?: 4
        val completedIdx = headerTokens.indexOf("completedDates").takeIf { it >= 0 } ?: 5
        val skippedIdx = headerTokens.indexOf("skippedDates").takeIf { it >= 0 } ?: 6

        val result = mutableListOf<Habit>()
        for (line in dataLines) {
            val row = parseCsvRow(line)
            if (row.size <= maxOf(nameIdx, colorIdx)) continue
            val name = row.getOrNull(nameIdx)?.trim() ?: continue
            if (name.isEmpty()) continue

            val id = row.getOrNull(idIdx)?.trim()?.ifEmpty { UUID.randomUUID().toString() } ?: UUID.randomUUID().toString()
            val colorStr = row.getOrNull(colorIdx)?.trim() ?: ""
            val color = try { GruvboxColor.valueOf(colorStr) } catch (_: Exception) { GruvboxColor.YELLOW }
            val createdAt = row.getOrNull(createdIdx)?.trim()?.ifEmpty { LocalDate.now().toString() } ?: LocalDate.now().toString()
            val target = row.getOrNull(targetIdx)?.trim()?.toIntOrNull()?.coerceIn(1, 7) ?: 7

            val completedDates = row.getOrNull(completedIdx)?.split(";")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
            val skippedDates = row.getOrNull(skippedIdx)?.split(";")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

            result.add(
                Habit(
                    id = id,
                    name = name,
                    color = color,
                    createdAt = createdAt,
                    targetDaysPerWeek = target,
                    completedDates = completedDates,
                    skippedDates = skippedDates
                )
            )
        }
        return result
    }

    /**
     * Parses uhabits matrix export: Date, Habit A, Habit B...
     * Cell values: >= 2 = Checkmark (Completed), 1 = Skip, 0 = Off
     */
    private fun importUhabitsMatrixCsv(dataLines: List<String>, headerTokens: List<String>): List<Habit> {
        if (headerTokens.size <= 1) return emptyList()
        val habitNames = headerTokens.drop(1)
        val gruvColors = GruvboxColor.entries

        val completedMap = mutableMapOf<Int, MutableSet<String>>()
        val skippedMap = mutableMapOf<Int, MutableSet<String>>()
        for (i in habitNames.indices) {
            completedMap[i] = mutableSetOf()
            skippedMap[i] = mutableSetOf()
        }

        var earliestDate: String? = null

        for (line in dataLines) {
            val row = parseCsvRow(line)
            if (row.isEmpty()) continue
            val dateStr = row[0].trim()
            if (dateStr.isNotEmpty() && (earliestDate == null || dateStr < earliestDate)) {
                earliestDate = dateStr
            }

            for (i in habitNames.indices) {
                val cellVal = row.getOrNull(i + 1)?.trim() ?: "0"
                val num = cellVal.toDoubleOrNull() ?: 0.0
                when {
                    num >= 2.0 -> completedMap[i]?.add(dateStr)
                    num == 1.0 -> skippedMap[i]?.add(dateStr)
                }
            }
        }

        return habitNames.mapIndexed { index, name ->
            Habit(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                color = gruvColors[index % gruvColors.size],
                createdAt = earliestDate ?: LocalDate.now().toString(),
                completedDates = completedMap[index] ?: emptySet(),
                skippedDates = skippedMap[index] ?: emptySet(),
                targetDaysPerWeek = 7
            )
        }
    }

    private fun importUhabitsDefinitionsCsv(dataLines: List<String>, headerTokens: List<String>): List<Habit> {
        val nameIdx = headerTokens.indexOfFirst { it.equals("name", ignoreCase = true) }.takeIf { it >= 0 } ?: 0
        val freqNumIdx = headerTokens.indexOfFirst { it.equals("frequency_num", ignoreCase = true) }
        val freqDenIdx = headerTokens.indexOfFirst { it.equals("frequency_den", ignoreCase = true) }
        val gruvColors = GruvboxColor.entries

        return dataLines.mapIndexedNotNull { idx, line ->
            val row = parseCsvRow(line)
            val name = row.getOrNull(nameIdx)?.trim() ?: return@mapIndexedNotNull null
            if (name.isEmpty()) return@mapIndexedNotNull null

            var target = 7
            if (freqNumIdx != null && freqNumIdx >= 0 && freqDenIdx != null && freqDenIdx >= 0) {
                val num = row.getOrNull(freqNumIdx)?.toIntOrNull() ?: 1
                val den = row.getOrNull(freqDenIdx)?.toIntOrNull() ?: 1
                target = if (den == 7) num.coerceIn(1, 7) else if (den == 1) (num * 7).coerceIn(1, 7) else ((num.toDouble() / den) * 7).roundToInt().coerceIn(1, 7)
            }

            Habit(
                id = UUID.randomUUID().toString(),
                name = name,
                color = gruvColors[idx % gruvColors.size],
                createdAt = LocalDate.now().toString(),
                targetDaysPerWeek = target,
                completedDates = emptySet(),
                skippedDates = emptySet()
            )
        }
    }

    private fun importHeuristicCsv(lines: List<String>): List<Habit> {
        val gruvColors = GruvboxColor.entries
        val habits = mutableListOf<Habit>()
        for ((idx, line) in lines.withIndex()) {
            val tokens = parseCsvRow(line)
            if (tokens.isEmpty()) continue
            val name = tokens[0].trim()
            if (name.isNotEmpty() && !name.equals("id", ignoreCase = true) && !name.equals("name", ignoreCase = true)) {
                habits.add(
                    Habit(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        color = gruvColors[idx % gruvColors.size],
                        createdAt = LocalDate.now().toString(),
                        targetDaysPerWeek = 7
                    )
                )
            }
        }
        return habits
    }

    /**
     * Splits a CSV line taking into account double quotes and escaped quotes.
     */
    fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(unescapeCsvCell(sb.toString()))
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(unescapeCsvCell(sb.toString()))
        return result
    }

    /**
     * Sanitizes cell values to prevent CSV Injection / Formula Injection in spreadsheet software.
     * If value starts with =, +, -, @, or tab/CR, it prefixes it with a single quote.
     */
    private fun sanitizeCsvCell(raw: String): String {
        var text = raw
        if (text.isNotEmpty()) {
            val first = text[0]
            if (first == '=' || first == '+' || first == '-' || first == '@' || first.code == 9 || first.code == 13) {
                text = "'" + text
            }
        }
        val needsQuotes = text.any { it == ',' || it == '"' || it.code == 10 || it.code == 13 }
        return if (needsQuotes) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }

    private fun unescapeCsvCell(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("'") && s.length > 1) {
            val next = s[1]
            if (next == '=' || next == '+' || next == '-' || next == '@') {
                s = s.substring(1)
            }
        }
        return s
    }
}
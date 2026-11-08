package com.lolerco.oledoro.habits

import com.lolerco.oledoro.data.GruvboxColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class HabitCsvHelperTest {

    @Test
    fun `exportToCsv neutralizes formula injection`() {
        val habits = listOf(
            Habit(
                id = "1",
                name = "=cmd|' /C calc'!A0",
                color = GruvboxColor.YELLOW,
                createdAt = "2026-11-01",
                targetDaysPerWeek = 7
            ),
            Habit(
                id = "2",
                name = "@SUM(1+1)",
                color = GruvboxColor.GREEN,
                createdAt = "2026-11-01",
                targetDaysPerWeek = 5
            ),
            Habit(
                id = "3",
                name = "Read, \"deeply\"",
                color = GruvboxColor.AQUA,
                createdAt = "2026-11-01",
                targetDaysPerWeek = 3
            )
        )

        val out = ByteArrayOutputStream()
        HabitCsvHelper.exportToCsv(habits, out)
        val csvText = out.toString(Charsets.UTF_8.name())

        // Formula prefixes must be preceded with single quote
        assertTrue(csvText.contains("'=cmd"))
        assertTrue(csvText.contains("'@SUM"))
        // Commas and quotes must be escaped
        assertTrue(csvText.contains(""""Read, ""deeply"""""))
    }

    @Test
    fun `native export and import roundtrip preserves data`() {
        val original = listOf(
            Habit(
                id = "h-123",
                name = "Meditate",
                color = GruvboxColor.BLUE,
                createdAt = "2026-10-01",
                targetDaysPerWeek = 5,
                completedDates = setOf("2026-10-02", "2026-10-03"),
                skippedDates = setOf("2026-10-04")
            )
        )

        val out = ByteArrayOutputStream()
        HabitCsvHelper.exportToCsv(original, out)
        val imported = HabitCsvHelper.importFromCsv(ByteArrayInputStream(out.toByteArray()))

        assertEquals(1, imported.size)
        val h = imported[0]
        assertEquals("h-123", h.id)
        assertEquals("Meditate", h.name)
        assertEquals(GruvboxColor.BLUE, h.color)
        assertEquals(5, h.targetDaysPerWeek)
        assertEquals(setOf("2026-10-02", "2026-10-03"), h.completedDates)
        assertEquals(setOf("2026-10-04"), h.skippedDates)
    }

    @Test
    fun `importFromCsv parses uhabits checkmark matrix`() {
        val csv = """
            Date,Morning Run,Coding,Reading
            2026-11-01,2,0,1
            2026-11-02,2,2,0
            2026-11-03,0,2,2
        """.trimIndent()

        val habits = HabitCsvHelper.importFromCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(3, habits.size)

        val run = habits.first { it.name == "Morning Run" }
        val coding = habits.first { it.name == "Coding" }
        val reading = habits.first { it.name == "Reading" }

        // Morning Run checked on Nov 1 and Nov 2
        assertEquals(setOf("2026-11-01", "2026-11-02"), run.completedDates)
        assertTrue(run.skippedDates.isEmpty())

        // Coding checked on Nov 2 and Nov 3
        assertEquals(setOf("2026-11-02", "2026-11-03"), coding.completedDates)

        // Reading skipped on Nov 1 (val 1) and checked on Nov 3 (val 2)
        assertEquals(setOf("2026-11-03"), reading.completedDates)
        assertEquals(setOf("2026-11-01"), reading.skippedDates)
    }

    @Test
    fun `importFromCsv parses uhabits habits definitions`() {
        val csv = """
            name,description,color,frequency_num,frequency_den,type,target_type,target_value,unit
            Exercise,Cardio,0xFF00FF,3,7,0,0,0,
            Daily Read,Pages,0x00FF00,1,1,0,0,0,
        """.trimIndent()

        val habits = HabitCsvHelper.importFromCsv(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(2, habits.size)

        val exercise = habits.first { it.name == "Exercise" }
        assertEquals(3, exercise.targetDaysPerWeek)

        val dailyRead = habits.first { it.name == "Daily Read" }
        assertEquals(7, dailyRead.targetDaysPerWeek)
    }

    @Test
    fun `importFromCsv parses uhabits ZIP archive with habits and checkmarks`() {
        val habitsCsv = """
            name,description,color,frequency_num,frequency_den
            Workout,Gym session,RED,4,7
        """.trimIndent()

        val checkmarksCsv = """
            Date,Workout
            2026-11-05,2
            2026-11-06,1
        """.trimIndent()

        val zipBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                zos.putNextEntry(ZipEntry("habits.csv"))
                zos.write(habitsCsv.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("checkmarks.csv"))
                zos.write(checkmarksCsv.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            baos.toByteArray()
        }

        val imported = HabitCsvHelper.importFromCsv(ByteArrayInputStream(zipBytes))
        assertEquals(1, imported.size)
        val habit = imported[0]
        assertEquals("Workout", habit.name)
        assertEquals(4, habit.targetDaysPerWeek)
        assertEquals(setOf("2026-11-05"), habit.completedDates)
        assertEquals(setOf("2026-11-06"), habit.skippedDates)
    }
}
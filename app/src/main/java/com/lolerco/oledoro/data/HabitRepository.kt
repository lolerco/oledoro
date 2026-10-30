package com.lolerco.oledoro.data

import android.content.Context
import android.content.SharedPreferences
import com.lolerco.oledoro.habits.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * SharedPreferences-backed repository for Habidoro habits, with in-memory caching
 * and reactive StateFlow updates.
 */
class AndroidHabitRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    init {
        loadHabits()
    }

    @Synchronized
    private fun loadHabits() {
        val raw = prefs.getString(KEY_HABITS_JSON, null)
        if (raw.isNullOrBlank()) {
            // Seed initial defaults so the user immediately has a functional tracker
            val today = LocalDate.now()
            val initial = listOf(
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "deep focus pomodoro",
                    color = GruvboxColor.YELLOW,
                    createdAt = today.minusDays(14).toString(),
                    completedDates = setOf(
                        today.toString(),
                        today.minusDays(1).toString(),
                        today.minusDays(2).toString(),
                        today.minusDays(3).toString(),
                        today.minusDays(4).toString()
                    )
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "read 20 pages",
                    color = GruvboxColor.AQUA,
                    createdAt = today.minusDays(10).toString(),
                    completedDates = setOf(
                        today.minusDays(1).toString(),
                        today.minusDays(2).toString(),
                        today.minusDays(3).toString()
                    )
                ),
                Habit(
                    id = UUID.randomUUID().toString(),
                    name = "morning workout",
                    color = GruvboxColor.GREEN,
                    createdAt = today.minusDays(7).toString(),
                    completedDates = setOf(
                        today.toString(),
                        today.minusDays(2).toString()
                    )
                )
            )
            _habits.value = initial
            saveToPrefs(initial)
        } else {
            try {
                val jsonArray = JSONArray(raw)
                val list = mutableListOf<Habit>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(deserializeHabit(obj))
                }
                _habits.value = list
            } catch (_: Exception) {
                _habits.value = emptyList()
            }
        }
    }

    @Synchronized
    fun addHabit(name: String, color: GruvboxColor = GruvboxColor.YELLOW): Habit {
        val newHabit = Habit(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            color = color,
            createdAt = LocalDate.now().toString(),
            completedDates = emptySet()
        )
        val updated = _habits.value + newHabit
        _habits.value = updated
        saveToPrefs(updated)
        return newHabit
    }

    @Synchronized
    fun deleteHabit(id: String) {
        val updated = _habits.value.filterNot { it.id == id }
        _habits.value = updated
        saveToPrefs(updated)
    }

    @Synchronized
    fun toggleHabitCompletion(id: String, dateString: String = LocalDate.now().toString()): Boolean {
        var nowCompleted = false
        val updated = _habits.value.map { habit ->
            if (habit.id == id) {
                val currentDates = habit.completedDates.toMutableSet()
                if (currentDates.contains(dateString)) {
                    currentDates.remove(dateString)
                    nowCompleted = false
                } else {
                    currentDates.add(dateString)
                    nowCompleted = true
                }
                habit.copy(completedDates = currentDates)
            } else {
                habit
            }
        }
        _habits.value = updated
        saveToPrefs(updated)
        return nowCompleted
    }

    @Synchronized
    fun flush() {
        saveToPrefs(_habits.value)
    }

    private fun saveToPrefs(list: List<Habit>) {
        val jsonArray = JSONArray()
        for (habit in list) {
            jsonArray.put(serializeHabit(habit))
        }
        prefs.edit().putString(KEY_HABITS_JSON, jsonArray.toString()).apply()
    }

    private fun serializeHabit(habit: Habit): JSONObject {
        return JSONObject().apply {
            put("id", habit.id)
            put("name", habit.name)
            put("color", habit.color.name)
            put("createdAt", habit.createdAt)
            put("targetDaysPerWeek", habit.targetDaysPerWeek)
            val datesArray = JSONArray()
            habit.completedDates.forEach { datesArray.put(it) }
            put("completedDates", datesArray)
        }
    }

    private fun deserializeHabit(obj: JSONObject): Habit {
        val id = obj.getString("id")
        val name = obj.getString("name")
        val colorStr = obj.optString("color", GruvboxColor.YELLOW.name)
        val color = try {
            GruvboxColor.valueOf(colorStr)
        } catch (_: Exception) {
            GruvboxColor.YELLOW
        }
        val createdAt = obj.optString("createdAt", LocalDate.now().toString())
        val targetDaysPerWeek = obj.optInt("targetDaysPerWeek", 7)
        val dates = mutableSetOf<String>()
        val datesArr = obj.optJSONArray("completedDates")
        if (datesArr != null) {
            for (i in 0 until datesArr.length()) {
                dates.add(datesArr.getString(i))
            }
        }
        return Habit(
            id = id,
            name = name,
            color = color,
            createdAt = createdAt,
            completedDates = dates,
            targetDaysPerWeek = targetDaysPerWeek
        )
    }

    companion object {
        private const val PREFS_NAME = "habidoro_prefs"
        private const val KEY_HABITS_JSON = "habits_v1"
    }
}

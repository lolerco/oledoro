package com.lolerco.oledoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.data.GruvboxColor
import com.lolerco.oledoro.habits.DayCheck
import com.lolerco.oledoro.habits.Habit
import com.lolerco.oledoro.habits.HabitStreakCalculator
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import java.time.LocalDate

/**
 * A single habit row mirroring Loop Habit Tracker (uhabits):
 * [Streak on Left] -> [Habit Name Center] -> [Past 7-day checks on Right]
 */
@Composable
fun HabitRow(
    habit: Habit,
    today: LocalDate = LocalDate.now(),
    onToggleDay: (dateString: String) -> Unit,
    onSelectHabitForStats: () -> Unit
) {
    val stats = remember(habit.completedDates, today) {
        HabitStreakCalculator.calculateStats(habit, today)
    }
    val pastWeek = remember(habit.completedDates, today) {
        HabitStreakCalculator.getPastWeek(habit, today)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelectHabitForStats
            )
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. STREAK ON THE LEFT (uhabits style)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(42.dp)
        ) {
            Text(
                text = "${stats.currentStreak}",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = if (stats.currentStreak > 0) habit.color.composeColor else AmbientDimGray
            )
            Text(
                text = "days",
                fontFamily = JetBrainsMono,
                fontSize = 9.sp,
                color = AmbientCoolGray.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 2. HABIT NAME IN THE MIDDLE
        Text(
            text = habit.name,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        // 3. STREAK OF THE PAST WEEK (7 days check circles/buttons on the right)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pastWeek.forEach { day ->
                PastWeekDayItem(
                    day = day,
                    accentColor = habit.color.composeColor,
                    onToggle = { onToggleDay(day.dateString) }
                )
            }
        }
    }
}

@Composable
private fun PastWeekDayItem(
    day: DayCheck,
    accentColor: Color,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (day.isCompleted) accentColor else Color.Transparent
            )
            .border(
                width = if (day.isToday) 1.5.dp else 1.dp,
                color = when {
                    day.isCompleted -> accentColor
                    day.isToday -> AmbientCoolGray
                    else -> AmbientDimGray.copy(alpha = 0.4f)
                },
                shape = CircleShape
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.dayInitial,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                day.isCompleted -> Color.Black
                day.isToday -> Color.White
                else -> AmbientDimGray
            }
        )
    }
}

package com.lolerco.oledoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lolerco.oledoro.data.DailyStudyStats
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import com.lolerco.oledoro.ui.theme.OledBlack
import kotlin.math.max

private fun formatMinutesOrHours(ms: Long): String {
    val totalSec = ms / 1000
    val mins = totalSec / 60
    val hours = mins / 60
    val remMins = mins % 60
    return when {
        hours > 0 -> "${hours}h ${remMins}m"
        mins > 0 -> "${mins}m"
        totalSec > 0 -> "${totalSec}s"
        else -> "0m"
    }
}

@Composable
fun StudyStatsDialog(
    dailyStats: List<DailyStudyStats>,
    focusColor: Color,
    breakColor: Color,
    onDismiss: () -> Unit
) {
    val maxTimeMs = max(1L, dailyStats.maxOfOrNull { max(it.focusTimeMs, it.breakTimeMs) } ?: 1L)
    val totalFocusMs = dailyStats.sumOf { it.focusTimeMs }
    val totalBreakMs = dailyStats.sumOf { it.breakTimeMs }
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OledBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, AmbientDimGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STUDY STATS",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = focusColor,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "daily focus & break activity",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = AmbientCoolGray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AmbientCoolGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend & Totals card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(10.dp)
                                    .background(focusColor, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "focus",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 10.sp,
                                    color = AmbientCoolGray
                                )
                                Text(
                                    text = formatMinutesOrHours(totalFocusMs),
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(10.dp)
                                    .background(breakColor, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "break",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 10.sp,
                                    color = AmbientCoolGray
                                )
                                Text(
                                    text = formatMinutesOrHours(totalBreakMs),
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Daily Bar Chart
                Text(
                    text = "ACTIVITY BARS",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = AmbientCoolGray,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    dailyStats.forEach { stat ->
                        val displayDate = if (stat.date.length >= 5) stat.date.substring(5) else stat.date
                        val focusFraction = (stat.focusTimeMs.toFloat() / maxTimeMs.toFloat()).coerceIn(0f, 1f)
                        val breakFraction = (stat.breakTimeMs.toFloat() / maxTimeMs.toFloat()).coerceIn(0f, 1f)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0D0D0D), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF1F1F1F), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stat.date,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "F: ${formatMinutesOrHours(stat.focusTimeMs)} · B: ${formatMinutesOrHours(stat.breakTimeMs)}",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 10.sp,
                                    color = AmbientCoolGray
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Focus bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "F ",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.sp,
                                    color = focusColor,
                                    modifier = Modifier.width(16.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                                ) {
                                    if (focusFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(focusFraction)
                                                .height(8.dp)
                                                .background(focusColor, RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Break bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "B ",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.sp,
                                    color = breakColor,
                                    modifier = Modifier.width(16.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                                ) {
                                    if (breakFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(breakFraction)
                                                .height(8.dp)
                                                .background(breakColor, RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

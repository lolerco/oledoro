package com.lolerco.oledoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lolerco.oledoro.data.DailyStudyStats
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import com.lolerco.oledoro.ui.theme.OledBlack
import kotlin.math.max

enum class StatsMetric(val label: String) {
    FOCUS("focus time"),
    BREAK("break time"),
    TOTAL("total time")
}

private fun formatMinutesOrHours(ms: Long): String {
    val totalSec = ms / 1000
    val mins = totalSec / 60
    val hours = mins / 60
    val remMins = mins % 60
    return when {
        hours > 0 && remMins > 0 -> "${hours}h ${remMins}m"
        hours > 0 -> "${hours}h"
        mins > 0 -> "${mins}m"
        totalSec > 0 -> "${totalSec}s"
        else -> "0m"
    }
}

private fun formatShortDate(date: String): String {
    return if (date.length >= 10) date.substring(5) else date
}

@Composable
fun StudyStatsDialog(
    dailyStats: List<DailyStudyStats>,
    focusColor: Color,
    breakColor: Color,
    accentColor: Color = focusColor,
    onDismiss: () -> Unit
) {
    var selectedMetric by remember { mutableStateOf(StatsMetric.FOCUS) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()

    val trackedDays = remember(dailyStats) {
        val filtered = dailyStats.filter { it.focusTimeMs > 0L || it.breakTimeMs > 0L }
        if (filtered.isEmpty()) dailyStats else filtered
    }

    val targetColor = when (selectedMetric) {
        StatsMetric.FOCUS -> focusColor
        StatsMetric.BREAK -> breakColor
        StatsMetric.TOTAL -> accentColor
    }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "graph_color"
    )

    val targetMaxMs = remember(trackedDays, selectedMetric) {
        val maxVal = trackedDays.maxOfOrNull { stat ->
            when (selectedMetric) {
                StatsMetric.FOCUS -> stat.focusTimeMs
                StatsMetric.BREAK -> stat.breakTimeMs
                StatsMetric.TOTAL -> stat.totalTimeMs
            }
        } ?: 0L
        max(60_000L, maxVal).toFloat()
    }

    val animatedMaxMs by animateFloatAsState(
        targetValue = targetMaxMs,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "graph_max"
    )

    val animatedValues = trackedDays.map { stat ->
        val target = when (selectedMetric) {
            StatsMetric.FOCUS -> stat.focusTimeMs
            StatsMetric.BREAK -> stat.breakTimeMs
            StatsMetric.TOTAL -> stat.totalTimeMs
        }.toFloat()
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            label = "point_${stat.date}"
        ).value
    }

    val currentTotalMs = remember(trackedDays, selectedMetric) {
        trackedDays.sumOf { stat ->
            when (selectedMetric) {
                StatsMetric.FOCUS -> stat.focusTimeMs
                StatsMetric.BREAK -> stat.breakTimeMs
                StatsMetric.TOTAL -> stat.totalTimeMs
            }
        }
    }

    val averageMs = if (trackedDays.isNotEmpty()) currentTotalMs / trackedDays.size else 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OledBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, AmbientDimGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
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
                            color = animatedColor,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "daily activity tracking",
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

                Spacer(modifier = Modifier.height(14.dp))

                // Overview Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F0F), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .height(8.dp)
                                        .background(animatedColor, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedMetric.label.uppercase(),
                                    fontFamily = JetBrainsMono,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = animatedColor,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatMinutesOrHours(currentTotalMs),
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "avg: ${formatMinutesOrHours(averageMs)} / day",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = AmbientCoolGray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${trackedDays.size} days tracked",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = AmbientCoolGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Graph Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIME ACTIVITY GRAPH",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = AmbientCoolGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Y: time  X: day",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        color = AmbientDimGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Single Interactive Graph Area
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1F1F1F), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    val containerWidth = maxWidth
                    val minCanvasWidth = max(containerWidth.value, (trackedDays.size * 56).toFloat()).dp
                    val horizontalGraphScrollState = rememberScrollState()

                    // Auto-scroll to latest days on initial load
                    LaunchedEffect(trackedDays.size) {
                        horizontalGraphScrollState.scrollTo(horizontalGraphScrollState.maxValue)
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Y-axis labels column
                        Column(
                            modifier = Modifier
                                .height(180.dp)
                                .padding(end = 4.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = formatMinutesOrHours(animatedMaxMs.toLong()),
                                fontFamily = JetBrainsMono,
                                fontSize = 8.sp,
                                color = AmbientDimGray
                            )
                            Text(
                                text = formatMinutesOrHours((animatedMaxMs / 2).toLong()),
                                fontFamily = JetBrainsMono,
                                fontSize = 8.sp,
                                color = AmbientDimGray
                            )
                            Text(
                                text = "0m",
                                fontFamily = JetBrainsMono,
                                fontSize = 8.sp,
                                color = AmbientDimGray
                            )
                        }

                        // Canvas area with horizontal scroll if multiple points
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalGraphScrollState)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .width(minCanvasWidth)
                                    .height(180.dp)
                                    .pointerInput(trackedDays, animatedValues) {
                                        detectTapGestures { offset ->
                                            val count = trackedDays.size
                                            if (count > 0) {
                                                val paddingLeft = 24.dp.toPx()
                                                val paddingRight = 24.dp.toPx()
                                                val availableWidth = size.width - paddingLeft - paddingRight
                                                val closestIndex = if (count <= 1) {
                                                    0
                                                } else {
                                                    val step = availableWidth / (count - 1)
                                                    val index = ((offset.x - paddingLeft + step / 2) / step).toInt()
                                                    index.coerceIn(0, count - 1)
                                                }
                                                selectedPointIndex = if (selectedPointIndex == closestIndex) null else closestIndex
                                            }
                                        }
                                    }
                            ) {
                                val w = size.width
                                val h = size.height
                                val paddingTop = 26.dp.toPx()
                                val paddingBottom = 26.dp.toPx()
                                val paddingLeft = 24.dp.toPx()
                                val paddingRight = 24.dp.toPx()
                                val graphHeight = h - paddingTop - paddingBottom
                                val availableWidth = w - paddingLeft - paddingRight

                                // Draw horizontal grid lines
                                val gridLines = listOf(0f, 0.5f, 1f)
                                for (gl in gridLines) {
                                    val y = paddingTop + (1f - gl) * graphHeight
                                    drawLine(
                                        color = AmbientDimGray.copy(alpha = 0.2f),
                                        start = Offset(0f, y),
                                        end = Offset(w, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                val count = trackedDays.size
                                if (count > 0) {
                                    val points = mutableListOf<Offset>()
                                    for (i in 0 until count) {
                                        val x = if (count <= 1) {
                                            w / 2f
                                        } else {
                                            paddingLeft + i.toFloat() * (availableWidth / (count - 1))
                                        }
                                        val frac = (animatedValues[i] / animatedMaxMs).coerceIn(0f, 1f)
                                        val y = paddingTop + (1f - frac) * graphHeight
                                        points.add(Offset(x, y))
                                    }

                                    val baselineY = paddingTop + graphHeight

                                    // Draw line path and gradient fill
                                    if (points.size > 1) {
                                        val linePath = Path().apply {
                                            moveTo(points[0].x, points[0].y)
                                            for (i in 1 until points.size) {
                                                lineTo(points[i].x, points[i].y)
                                            }
                                        }

                                        val fillPath = Path().apply {
                                            addPath(linePath)
                                            lineTo(points.last().x, baselineY)
                                            lineTo(points.first().x, baselineY)
                                            close()
                                        }

                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    animatedColor.copy(alpha = 0.28f),
                                                    animatedColor.copy(alpha = 0.02f)
                                                ),
                                                startY = paddingTop,
                                                endY = baselineY
                                            )
                                        )

                                        drawPath(
                                            path = linePath,
                                            color = animatedColor,
                                            style = Stroke(
                                                width = 2.5.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }

                                    // Draw data points, values, and date labels
                                    for (i in 0 until count) {
                                        val pt = points[i]
                                        val isPointSelected = (selectedPointIndex == i)

                                        // Point circle outer glow
                                        if (isPointSelected) {
                                            drawCircle(
                                                color = animatedColor.copy(alpha = 0.4f),
                                                radius = 9.dp.toPx(),
                                                center = pt
                                            )
                                        }

                                        drawCircle(
                                            color = OledBlack,
                                            radius = 5.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = animatedColor,
                                            radius = 4.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = OledBlack,
                                            radius = 1.8.dp.toPx(),
                                            center = pt
                                        )

                                        // Draw value label above point
                                        val valueStr = formatMinutesOrHours(animatedValues[i].toLong())
                                        val valueLayout = textMeasurer.measure(
                                            text = valueStr,
                                            style = TextStyle(
                                                fontFamily = JetBrainsMono,
                                                fontSize = 8.sp,
                                                fontWeight = if (isPointSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isPointSelected) animatedColor else AmbientCoolGray
                                            )
                                        )
                                        drawText(
                                            textLayoutResult = valueLayout,
                                            topLeft = Offset(
                                                x = pt.x - (valueLayout.size.width / 2f),
                                                y = pt.y - 14.dp.toPx()
                                            )
                                        )

                                        // Draw date label below baseline
                                        val dateStr = formatShortDate(trackedDays[i].date)
                                        val dateLayout = textMeasurer.measure(
                                            text = dateStr,
                                            style = TextStyle(
                                                fontFamily = JetBrainsMono,
                                                fontSize = 8.sp,
                                                fontWeight = if (isPointSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isPointSelected) Color.White else AmbientDimGray
                                            )
                                        )
                                        drawText(
                                            textLayoutResult = dateLayout,
                                            topLeft = Offset(
                                                x = pt.x - (dateLayout.size.width / 2f),
                                                y = baselineY + 6.dp.toPx()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Point Details Card (if tapped)
                if (selectedPointIndex != null && selectedPointIndex in trackedDays.indices) {
                    val stat = trackedDays[selectedPointIndex!!]
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141414), RoundedCornerShape(6.dp))
                            .border(1.dp, animatedColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stat.date,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Text(
                                text = "F: ${formatMinutesOrHours(stat.focusTimeMs)} · B: ${formatMinutesOrHours(stat.breakTimeMs)} · T: ${formatMinutesOrHours(stat.totalTimeMs)}",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = animatedColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle view controls below graph
                Text(
                    text = "TOGGLE GRAPH METRIC",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = AmbientDimGray,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatsMetric.values().forEach { metric ->
                        val isSelected = (selectedMetric == metric)
                        val metricColor = when (metric) {
                            StatsMetric.FOCUS -> focusColor
                            StatsMetric.BREAK -> breakColor
                            StatsMetric.TOTAL -> accentColor
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) metricColor.copy(alpha = 0.16f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) metricColor else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    selectedMetric = metric
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "[ ${metric.label} ]",
                                fontFamily = JetBrainsMono,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp,
                                color = if (isSelected) metricColor else AmbientCoolGray
                            )
                        }
                    }
                }
            }
        }
    }
}

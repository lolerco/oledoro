package com.lolerco.oledoro.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.data.ChangelogProvider
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import com.lolerco.oledoro.ui.utils.HapticHelper

@Composable
fun ChangelogSection(
    themeColor: Color,
    context: Context? = null,
    modifier: Modifier = Modifier
) {
    val actualContext = context ?: LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // "see changelog" extra button at the bottom of settings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isExpanded) themeColor else AmbientDimGray, RoundedCornerShape(8.dp))
                .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                .clickable {
                    HapticHelper.performClick(actualContext)
                    isExpanded = !isExpanded
                }
                .padding(vertical = 10.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "see changelog",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (isExpanded) themeColor else AmbientCoolGray,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = if (isExpanded) themeColor else AmbientDimGray
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                    .border(1.dp, AmbientDimGray, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "CHANGELOG",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = themeColor,
                    letterSpacing = 1.5.sp
                )

                ChangelogProvider.entries.forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v${entry.version}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = entry.date,
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = AmbientCoolGray
                            )
                        }

                        entry.highlights.forEach { highlight ->
                            Row(
                                modifier = Modifier.padding(start = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "·",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = themeColor
                                )
                                Text(
                                    text = highlight,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = AmbientCoolGray,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

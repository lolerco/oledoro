package com.lolerco.oledoro.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.data.GruvboxColor
import com.lolerco.oledoro.habits.Habit
import com.lolerco.oledoro.habits.HabitCsvHelper
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Specific Settings Dialog for Habidoro.
 * Includes:
 * - Secure CSV Export (creates a sanitized CSV file via Android Storage Access Framework)
 * - Secure CSV Import (imports Habidoro or Loop Habit Tracker / uhabits CSV files with formula-injection sanitization)
 * - Informational notes on supported uhabits export formats
 */
@Composable
fun HabidoroSettingsDialog(
    habits: List<Habit>,
    themeColor: GruvboxColor,
    onImportHabits: (List<Habit>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // SAF Create Document for Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        HabitCsvHelper.exportToCsv(habits, os)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Exported ${habits.size} habits successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // SAF Open Document for Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val imported = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        HabitCsvHelper.importFromCsv(inputStream)
                    } ?: emptyList()

                    withContext(Dispatchers.Main) {
                        if (imported.isNotEmpty()) {
                            onImportHabits(imported)
                            Toast.makeText(context, "Imported ${imported.size} habits successfully", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        } else {
                            Toast.makeText(context, "No valid habits found in selected CSV", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Black,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HABIDORO SETTINGS",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = themeColor.color
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AmbientCoolGray
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "DATA BACKUP & TRANSFER",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmbientCoolGray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Export CSV Option
                SettingsActionTile(
                    title = "Export Habits (CSV)",
                    subtitle = "Export all current habits & history to a secure CSV file",
                    icon = Icons.Default.FileUpload,
                    accentColor = themeColor.color,
                    onClick = {
                        val defaultName = "habidoro_backup_${LocalDate.now()}.csv"
                        exportLauncher.launch(defaultName)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Import CSV Option
                SettingsActionTile(
                    title = "Import Habits (CSV)",
                    subtitle = "Import from Habidoro or Loop Habit Tracker (uhabits) CSV export",
                    icon = Icons.Default.FileDownload,
                    accentColor = themeColor.color,
                    onClick = {
                        importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*", "*/*"))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F0F0F))
                        .border(1.dp, AmbientDimGray.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "COMPATIBILITY NOTICE",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor.color
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Supports direct Loop Habit Tracker (uhabits) checkmarks export matrix or habits list. Injection formulas are automatically neutralized.",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = AmbientCoolGray.copy(alpha = 0.8f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "DONE",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = themeColor.color
                )
            }
        }
    )
}

@Composable
private fun SettingsActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141414))
            .border(1.dp, AmbientDimGray.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = AmbientCoolGray.copy(alpha = 0.7f)
            )
        }
    }
}

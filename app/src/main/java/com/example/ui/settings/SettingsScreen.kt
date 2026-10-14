package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.common.AppTopBar
import com.example.ui.common.ConfirmDialog
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningRed
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val autoSpeak by viewModel.autoSpeak.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val ttsReady by viewModel.ttsReady.collectAsState()
    val hasUsVoice by viewModel.hasUsVoice.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nav_settings),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Restore Result Notice
            uiState.restoreSuccess?.let { success ->
                Surface(
                    color = if (success) SuccessGreen.copy(alpha = 0.12f) else WarningRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (success) SuccessGreen else WarningRed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (success) "تمت استعادة النسخة الاحتياطية بنجاح!" else (uiState.errorMessage ?: "فشلت الاستعادة"),
                            color = if (success) SuccessGreen else WarningRed,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // 1. Text-To-Speech Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.tts_status_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // TTS Status Alert Box
                    val isUsReady = ttsReady && hasUsVoice
                    Surface(
                        color = if (isUsReady) SuccessGreen.copy(alpha = 0.12f) else WarningRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isUsReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isUsReady) SuccessGreen else WarningRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(if (isUsReady) R.string.tts_ready else R.string.tts_not_ready),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isUsReady) SuccessGreen else WarningRed
                            )
                        }
                    }

                    // Auto Speak Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.setting_auto_speak),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "نطق الكلمة الإنجليزية تلقائيًا عند ظهورها",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSpeak,
                            onCheckedChange = viewModel::setAutoSpeak,
                            modifier = Modifier.testTag("switch_auto_speak")
                        )
                    }

                    // Test TTS & Open System Settings Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.testTtsSpeech() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_test_tts")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.tts_test_btn))
                        }

                        OutlinedButton(
                            onClick = { viewModel.openSystemTtsSettings() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_open_tts_settings")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.tts_open_settings))
                        }
                    }
                }
            }

            // 2. Countdown Timer Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.setting_timer_duration),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.timer_seconds, timerSeconds),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "المدة الزمنية قبل كشف المعنى العربي تلقائيًا في الاختبار الذكي (1 - 15 ثانية):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = timerSeconds.toFloat(),
                        onValueChange = { viewModel.setTimerSeconds(it.roundToInt()) },
                        valueRange = 1f..15f,
                        steps = 13,
                        modifier = Modifier.testTag("slider_timer_duration")
                    )
                }
            }

            // 3. Theme Mode Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.theme_mode),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    ThemeRadioRow(
                        title = stringResource(R.string.theme_system),
                        selected = themeMode == "SYSTEM",
                        onClick = { viewModel.setThemeMode("SYSTEM") }
                    )
                    ThemeRadioRow(
                        title = stringResource(R.string.theme_light),
                        selected = themeMode == "LIGHT",
                        onClick = { viewModel.setThemeMode("LIGHT") }
                    )
                    ThemeRadioRow(
                        title = stringResource(R.string.theme_dark),
                        selected = themeMode == "DARK",
                        onClick = { viewModel.setThemeMode("DARK") }
                    )
                }
            }

            // 4. Backup & Restore Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "النسخ الاحتياطي والاستعادة",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "يمكنك تصدير قاعدة البيانات كاملة في ملف JSON واستعادتها في أي وقت دون اتصال بالإنترنت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.prepareBackupJson { jsonContent ->
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, jsonContent)
                                        putExtra(Intent.EXTRA_SUBJECT, "LexiVocab_Backup.json")
                                        type = "application/json"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "مشاركة / حفظ النسخة الاحتياطية")
                                    context.startActivity(shareIntent)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_backup_all")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_all))
                        }

                        OutlinedButton(
                            onClick = {
                                restoreFilePicker.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_restore_backup")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.restore_backup))
                        }
                    }
                }
            }

            // 5. About App Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.about_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الإصدار 1.0.0 • تطبيق أندرويد محلي 100% دون حسابات أو خوادم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    // Confirmation Alert before Restoring
    if (showRestoreConfirmDialog) {
        ConfirmDialog(
            title = stringResource(R.string.restore_confirm_title),
            message = stringResource(R.string.restore_confirm_desc),
            confirmButtonText = "استعادة",
            cancelButtonText = stringResource(R.string.cancel),
            isDestructive = true,
            onConfirm = {
                showRestoreConfirmDialog = false
                selectedRestoreUri?.let { uri ->
                    viewModel.restoreBackupFromUri(uri)
                }
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                selectedRestoreUri = null
            }
        )
    }
}

@Composable
fun ThemeRadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

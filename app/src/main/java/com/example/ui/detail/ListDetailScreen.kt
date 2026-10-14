package com.example.ui.detail

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.VocabListWithStats
import com.example.ui.common.AppTopBar
import com.example.ui.common.ConfirmDialog
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningRed

@Composable
fun ListDetailScreen(
    listId: Long,
    viewModel: ListDetailViewModel,
    onBackClick: () -> Unit,
    onStartQuiz: (filterType: String) -> Unit,
    onQuickReview: (filterType: String) -> Unit,
    onOpenBox: (filterType: String) -> Unit,
    onAddWordClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val context = LocalContext.current
    val listWithStats by viewModel.listWithStats.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    var showQuizFilterDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val listName = listWithStats?.name ?: "تفاصيل القائمة"

    Scaffold(
        topBar = {
            AppTopBar(
                title = listName,
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.prepareCsvExport { csvContent ->
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, csvContent)
                                    putExtra(Intent.EXTRA_SUBJECT, "$listName - LexiVocab.csv")
                                    type = "text/csv"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة / حفظ القائمة")
                                context.startActivity(shareIntent)
                            }
                        },
                        modifier = Modifier.testTag("btn_export_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.export_list)
                        )
                    }
                }
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
            val stats = listWithStats

            // 1. Primary Action: Start Quiz Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        if (activeSession != null) {
                            showResumeDialog = true
                        } else {
                            showQuizFilterDialog = true
                        }
                    }
                    .testTag("card_start_quiz"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.start_quiz),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (activeSession != null) {
                                "توجد جلسة غير مكتملة (اضغط للاستكمال)"
                            } else {
                                "مؤقت ذكي مع خوارزمية الحفظ والنسيان"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // 2. Secondary Action: Quick Review Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onQuickReview("ALL") }
                    .testTag("card_quick_review"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.quick_review),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "تصفح المفردات بسلاسة مع النطق ودون اختبار",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3, 4, 5. The Three Word Boxes Grid
            Text(
                text = "صناديق المفردات",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Forgotten Box (صندوق الكلمات المنسية)
                BoxCategoryCard(
                    modifier = Modifier.weight(1f),
                    title = "المنسية",
                    count = stats?.forgottenCount ?: 0,
                    icon = Icons.Default.Warning,
                    color = WarningRed,
                    testTag = "box_forgotten",
                    onClick = { onOpenBox("FORGOTTEN") }
                )

                // Saved Box (صندوق الكلمات المحفوظة)
                BoxCategoryCard(
                    modifier = Modifier.weight(1f),
                    title = "المحفوظة",
                    count = stats?.savedCount ?: 0,
                    icon = Icons.Default.CheckCircle,
                    color = SuccessGreen,
                    testTag = "box_saved",
                    onClick = { onOpenBox("SAVED") }
                )

                // All Words (جميع الكلمات)
                BoxCategoryCard(
                    modifier = Modifier.weight(1f),
                    title = "الكل",
                    count = stats?.totalCount ?: 0,
                    icon = Icons.Default.FormatListBulleted,
                    color = MaterialTheme.colorScheme.primary,
                    testTag = "box_all",
                    onClick = { onOpenBox("ALL") }
                )
            }

            // 6. Add Word and Import Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddWordClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_detail_add_word")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_new_word))
                }

                FilledTonalButton(
                    onClick = onImportClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_detail_import_words")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.import_words))
                }
            }

            // 7. List Statistics Card
            stats?.let { s ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_list_statistics"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.list_stats),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val pct = if (s.totalCount > 0) ((s.savedCount.toFloat() / s.totalCount.toFloat()) * 100f).toInt() else 0
                        Text(
                            text = "نسبة الإتقان: $pct%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { if (s.totalCount > 0) s.savedCount.toFloat() / s.totalCount.toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SuccessGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(label = "إجمالي الكلمات", value = "${s.totalCount}")
                            StatItem(label = "المحفوظة", value = "${s.savedCount}", color = SuccessGreen)
                            StatItem(label = "المنسية", value = "${s.forgottenCount}", color = WarningRed)
                            StatItem(label = "غير مصنفة", value = "${s.uncategorizedCount}")
                        }
                    }
                }
            }

            // 8. Reset Progress Button
            OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_reset_progress"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reset_progress))
            }
        }
    }

    // Dialog: Quiz Word Selection Filter
    if (showQuizFilterDialog) {
        var selectedFilter by remember { mutableStateOf("ALL") }
        val s = listWithStats

        AlertDialog(
            onDismissRequest = { showQuizFilterDialog = false },
            title = { Text(stringResource(R.string.quiz_filter_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterOptionItem(
                        title = "جميع كلمات القائمة (${s?.totalCount ?: 0})",
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" }
                    )
                    FilterOptionItem(
                        title = "الكلمات المنسية فقط (${s?.forgottenCount ?: 0})",
                        selected = selectedFilter == "FORGOTTEN",
                        onClick = { selectedFilter = "FORGOTTEN" }
                    )
                    FilterOptionItem(
                        title = "الكلمات غير المصنفة فقط (${s?.uncategorizedCount ?: 0})",
                        selected = selectedFilter == "UNCATEGORIZED",
                        onClick = { selectedFilter = "UNCATEGORIZED" }
                    )
                    FilterOptionItem(
                        title = "الكلمات المحفوظة فقط (${s?.savedCount ?: 0})",
                        selected = selectedFilter == "SAVED",
                        onClick = { selectedFilter = "SAVED" }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuizFilterDialog = false
                        onStartQuiz(selectedFilter)
                    },
                    modifier = Modifier.testTag("btn_confirm_start_quiz")
                ) {
                    Text(stringResource(R.string.quiz_start_button))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showQuizFilterDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog: Resume Quiz Session
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text(stringResource(R.string.resume_session)) },
            text = { Text(stringResource(R.string.resume_dialog_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResumeDialog = false
                        val filter = activeSession?.filterType ?: "ALL"
                        onStartQuiz(filter)
                    },
                    modifier = Modifier.testTag("btn_resume_session")
                ) {
                    Text("استكمال الجلسة")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteActiveSession()
                        showResumeDialog = false
                        showQuizFilterDialog = true
                    },
                    modifier = Modifier.testTag("btn_new_session")
                ) {
                    Text("جلسة جديدة")
                }
            }
        )
    }

    // Dialog: Reset Progress Confirmation
    if (showResetConfirmDialog) {
        ConfirmDialog(
            title = stringResource(R.string.reset_confirm_title),
            message = stringResource(R.string.reset_confirm_desc),
            confirmButtonText = "إعادة التعيين",
            cancelButtonText = stringResource(R.string.cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.resetProgress()
                showResetConfirmDialog = false
            },
            onDismiss = { showResetConfirmDialog = false }
        )
    }
}

@Composable
fun BoxCategoryCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FilterOptionItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

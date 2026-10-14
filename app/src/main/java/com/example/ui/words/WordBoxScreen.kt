package com.example.ui.words

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Word
import com.example.data.model.WordStatus
import com.example.ui.common.AppTopBar
import com.example.ui.common.ConfirmDialog
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningRed

@Composable
fun WordBoxScreen(
    listId: Long,
    filterType: String,
    viewModel: WordBoxViewModel,
    onBackClick: () -> Unit,
    onAddWordClick: () -> Unit,
    onEditWordClick: (Long) -> Unit,
    onStartQuizClick: (String) -> Unit
) {
    val words by viewModel.words.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var wordToDelete by remember { mutableStateOf<Word?>(null) }

    val boxTitle = when (filterType) {
        "FORGOTTEN" -> stringResource(R.string.box_forgotten)
        "SAVED" -> stringResource(R.string.box_saved)
        "UNCATEGORIZED" -> "الكلمات غير المصنفة"
        else -> stringResource(R.string.box_all)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "$boxTitle (${words.size})",
                onBackClick = onBackClick,
                actions = {
                    if (words.isNotEmpty()) {
                        IconButton(
                            onClick = { onStartQuizClick(filterType) },
                            modifier = Modifier.testTag("btn_quiz_box")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "بدء الاختبار لهذا الصندوق"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddWordClick,
                modifier = Modifier.testTag("fab_add_word"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_new_word))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_words_input"),
                placeholder = { Text(stringResource(R.string.search_words_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (words.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "لا توجد كلمات هنا حاليًا",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onAddWordClick) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.add_new_word))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("words_lazy_column"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = words, key = { it.id }) { word ->
                        WordItemCard(
                            word = word,
                            onSpeakClick = { viewModel.speak(word.english) },
                            onEditClick = { onEditWordClick(word.id) },
                            onDeleteClick = { wordToDelete = word },
                            onStatusChange = { newStatus -> viewModel.updateWordStatus(word, newStatus) }
                        )
                    }
                }
            }
        }
    }

    wordToDelete?.let { w ->
        ConfirmDialog(
            title = "حذف الكلمة؟",
            message = "هل أنت متأكد من حذف الكلمة \"${w.english}\"؟",
            confirmButtonText = stringResource(R.string.delete),
            cancelButtonText = stringResource(R.string.cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteWord(w.id)
                wordToDelete = null
            },
            onDismiss = { wordToDelete = null }
        )
    }
}

@Composable
fun WordItemCard(
    word: Word,
    onSpeakClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_card_${word.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // English word and Speaker Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onSpeakClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_word_speak_${word.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "استماع",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = word.english,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status Badge with Dropdown selector
                Box {
                    val statusText = when (word.status) {
                        WordStatus.SAVED -> "محفوظة"
                        WordStatus.FORGOTTEN -> "منسية"
                        else -> "غير مصنفة"
                    }
                    val badgeColor = when (word.status) {
                        WordStatus.SAVED -> SuccessGreen
                        WordStatus.FORGOTTEN -> WarningRed
                        else -> MaterialTheme.colorScheme.outline
                    }

                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { statusMenuExpanded = true }
                    ) {
                        Text(
                            text = statusText,
                            color = badgeColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("محفوظة") },
                            onClick = {
                                statusMenuExpanded = false
                                onStatusChange(WordStatus.SAVED)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("منسية") },
                            onClick = {
                                statusMenuExpanded = false
                                onStatusChange(WordStatus.FORGOTTEN)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("غير مصنفة") },
                            onClick = {
                                statusMenuExpanded = false
                                onStatusChange(WordStatus.UNCATEGORIZED)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Arabic Translation
            Text(
                text = word.arabic,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            // Example if exists
            if (word.exampleEn.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• ${word.exampleEn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (word.exampleAr.isNotBlank()) {
                    Text(
                        text = "  ${word.exampleAr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Derivative if exists
            if (word.derivedEn.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "مشتق: ${word.derivedEn} (${word.derivedAr})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card bottom actions (Edit & Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

package com.example.ui.importexport

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.common.AppTopBar
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onBackClick: () -> Unit,
    onImportFinished: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val availableLists by viewModel.availableLists.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "imported_file.csv"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            viewModel.onFileSelected(uri, fileName)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.import_words),
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
            // 1. File Picker Button Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_pick_file"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.import_source_choose),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يدعم CSV, TSV, TXT وملفات Lexilize LXF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "text/*",
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "text/tab-separated-values",
                                    "application/zip",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.testTag("btn_select_import_file"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.select_file))
                    }
                }
            }

            // Loading / Analyzing
            if (uiState.isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("جاري فحص محتوى الملف...")
                    }
                }
            }

            // LXF Note if present
            uiState.previewData?.lxfNote?.let { note ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Surface(
                    color = WarningRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = error, color = WarningRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Preview & Column Mapping Section
            uiState.previewData?.let { preview ->
                if (preview.rawRows.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${stringResource(R.string.preview_first_10)} (${preview.totalRowsFound} سطر تم العثور عليها)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal scrollable preview table
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Headers
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        preview.headers.forEachIndexed { idx, h ->
                                            Text(
                                                text = "[$idx] $h",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.width(120.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    // Rows
                                    preview.rawRows.take(8).forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            preview.headers.indices.forEach { colIdx ->
                                                Text(
                                                    text = row.getOrNull(colIdx) ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.width(120.dp),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "تعيين الأعمدة (Column Mapping)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // English Column Selector
                            ColumnSelectorDropdown(
                                label = "عمود الكلمة بالإنجليزية *",
                                options = preview.headers,
                                selectedIndex = uiState.enColIndex,
                                onSelect = viewModel::setEnColIndex
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Arabic Column Selector
                            ColumnSelectorDropdown(
                                label = "عمود المعنى بالعربية *",
                                options = preview.headers,
                                selectedIndex = uiState.arColIndex,
                                onSelect = viewModel::setArColIndex
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Example EN Selector (Optional)
                            ColumnSelectorDropdown(
                                label = "عمود المثال الإنجليزي (اختياري)",
                                options = listOf("(بدون)") + preview.headers,
                                selectedIndex = uiState.exampleEnIndex + 1,
                                onSelect = { viewModel.setExampleEnIndex(it - 1) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Example AR Selector (Optional)
                            ColumnSelectorDropdown(
                                label = "عمود ترجمة المثال (اختياري)",
                                options = listOf("(بدون)") + preview.headers,
                                selectedIndex = uiState.exampleArIndex + 1,
                                onSelect = { viewModel.setExampleArIndex(it - 1) }
                            )
                        }
                    }

                    // Destination List Choice
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.target_list_mode),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Option: Create New List
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setIsCreatingNewList(true) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.isCreatingNewList,
                                    onClick = { viewModel.setIsCreatingNewList(true) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.create_new_list_radio))
                            }

                            if (uiState.isCreatingNewList) {
                                OutlinedTextField(
                                    value = uiState.newListName,
                                    onValueChange = viewModel::setNewListName,
                                    label = { Text("اسم القائمة الجديدة") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
                                        .testTag("input_import_new_list_name"),
                                    singleLine = true
                                )
                            }

                            // Option: Add to Existing List
                            if (availableLists.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setIsCreatingNewList(false) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = !uiState.isCreatingNewList,
                                        onClick = { viewModel.setIsCreatingNewList(false) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.add_to_existing_radio))
                                }

                                if (!uiState.isCreatingNewList) {
                                    ColumnSelectorDropdown(
                                        label = "اختر القائمة الموجودة",
                                        options = availableLists.map { it.name },
                                        selectedIndex = availableLists.indexOfFirst { it.id == uiState.selectedListId }.coerceAtLeast(0),
                                        onSelect = { idx ->
                                            if (idx in availableLists.indices) {
                                                viewModel.setSelectedListId(availableLists[idx].id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Start Import Button
                    Button(
                        onClick = { viewModel.startImport() },
                        enabled = !uiState.isImporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_start_import_action"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isImporting) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جاري استيراد الكلمات...")
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.start_import_btn), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Success Results Card
            uiState.importSuccessCount?.let { count ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_import_success"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.import_success_msg, count),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SuccessGreen
                        )
                        uiState.duplicateCount?.let { dups ->
                            if (dups > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تم تخطي $dups كلمة مكررة تلقائيًا للمحافظة على البيانات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onImportFinished,
                            modifier = Modifier.testTag("btn_import_finish")
                        ) {
                            Text("عرض القوائم")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnSelectorDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.getOrNull(selectedIndex) ?: options.firstOrNull() ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

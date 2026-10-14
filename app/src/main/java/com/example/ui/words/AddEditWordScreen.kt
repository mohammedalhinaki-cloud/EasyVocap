package com.example.ui.words

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.common.AppTopBar
import com.example.ui.theme.WarningRed

@Composable
fun AddEditWordScreen(
    viewModel: AddEditWordViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.shouldClose) {
        if (uiState.shouldClose) {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        // Autofocus English field
        focusRequester.requestFocus()
    }

    LaunchedEffect(uiState.isSavedSuccess) {
        if (uiState.isSavedSuccess) {
            snackbarHostState.showSnackbar("تم حفظ الكلمة بنجاح، المؤشر جاهز للكلمة التالية")
            viewModel.clearSuccessNotice()
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (uiState.isEditing) "تعديل الكلمة" else stringResource(R.string.add_new_word),
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Duplicate Warning Banner
            if (uiState.isDuplicateWarning) {
                Surface(
                    color = WarningRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.duplicate_word_warning),
                            color = WarningRed,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // 1. English Word (Required)
            OutlinedTextField(
                value = uiState.english,
                onValueChange = viewModel::onEnglishChange,
                label = { Text(stringResource(R.string.field_english)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("input_word_english"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )

            // 2. Arabic Meaning (Required)
            OutlinedTextField(
                value = uiState.arabic,
                onValueChange = viewModel::onArabicChange,
                label = { Text(stringResource(R.string.field_arabic)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_word_arabic"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )

            // 3. English Example (Optional)
            OutlinedTextField(
                value = uiState.exampleEn,
                onValueChange = viewModel::onExampleEnChange,
                label = { Text(stringResource(R.string.field_example_en)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_example_en"),
                singleLine = false,
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )

            // 4. Arabic Example Translation (Optional)
            OutlinedTextField(
                value = uiState.exampleAr,
                onValueChange = viewModel::onExampleArChange,
                label = { Text(stringResource(R.string.field_example_ar)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_example_ar"),
                singleLine = false,
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )

            // 5 & 6. Derivative EN & AR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.derivedEn,
                    onValueChange = viewModel::onDerivedEnChange,
                    label = { Text(stringResource(R.string.field_derived_en)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_derived_en"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = uiState.derivedAr,
                    onValueChange = viewModel::onDerivedArChange,
                    label = { Text(stringResource(R.string.field_derived_ar)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_derived_ar"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.saveWord(closeAfterSave = false) }),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            if (!uiState.isEditing) {
                // "حفظ وإضافة التالية" (Save & Add Next)
                Button(
                    onClick = { viewModel.saveWord(closeAfterSave = false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_add_next"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.save_and_add_next),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // "حفظ وإغلاق" (Save & Close)
                OutlinedButton(
                    onClick = { viewModel.saveWord(closeAfterSave = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_and_close"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.save_and_close),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.saveWord(closeAfterSave = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_update_word"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

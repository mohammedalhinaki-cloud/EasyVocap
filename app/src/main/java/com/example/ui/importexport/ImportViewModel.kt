package com.example.ui.importexport

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.importer.ImportPreviewData
import com.example.data.importer.LexiImporter
import com.example.data.model.VocabListWithStats
import com.example.data.model.Word
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportUiState(
    val isAnalyzing: Boolean = false,
    val previewData: ImportPreviewData? = null,
    val enColIndex: Int = 0,
    val arColIndex: Int = 1,
    val exampleEnIndex: Int = -1,
    val exampleArIndex: Int = -1,
    val isCreatingNewList: Boolean = true,
    val newListName: String = "",
    val selectedListId: Long? = null,
    val isImporting: Boolean = false,
    val importSuccessCount: Int? = null,
    val duplicateCount: Int? = null,
    val errorMessage: String? = null
)

class ImportViewModel(
    application: Application,
    private val preselectedListId: Long?
) : AndroidViewModel(application) {

    private val repository = VocabRepository(AppDatabase.getDatabase(application))

    private val _uiState = MutableStateFlow(
        ImportUiState(
            isCreatingNewList = preselectedListId == null || preselectedListId <= 0,
            selectedListId = preselectedListId
        )
    )
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    val availableLists: StateFlow<List<VocabListWithStats>> = repository.listsWithStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onFileSelected(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzing = true,
                errorMessage = null,
                importSuccessCount = null,
                previewData = null
            )
            try {
                val preview = LexiImporter.analyzeFile(getApplication(), uri, fileName)
                val guessedEnCol = guessColumnIndex(preview.headers, listOf("english", "en", "word", "front", "الكلمة", "الإنجليزية"))
                val guessedArCol = guessColumnIndex(preview.headers, listOf("arabic", "ar", "meaning", "translation", "back", "المعنى", "العربية"))
                val guessedExEn = guessColumnIndex(preview.headers, listOf("example en", "example", "مثال"))
                val guessedExAr = guessColumnIndex(preview.headers, listOf("example ar", "ترجمة المثال"))

                val defaultListName = fileName.substringBeforeLast(".").replace("_", " ").trim()

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    previewData = preview,
                    enColIndex = if (guessedEnCol >= 0) guessedEnCol else 0,
                    arColIndex = if (guessedArCol >= 0) guessedArCol else (if (preview.headers.size > 1) 1 else 0),
                    exampleEnIndex = guessedExEn,
                    exampleArIndex = guessedExAr,
                    newListName = if (_uiState.value.newListName.isBlank()) defaultListName else _uiState.value.newListName
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = e.message ?: "حدث خطأ أثناء فحص الملف"
                )
            }
        }
    }

    private fun guessColumnIndex(headers: List<String>, keywords: List<String>): Int {
        for (i in headers.indices) {
            val h = headers[i].lowercase().trim()
            if (keywords.any { h.contains(it) }) {
                return i
            }
        }
        return -1
    }

    fun setEnColIndex(index: Int) {
        _uiState.value = _uiState.value.copy(enColIndex = index)
    }

    fun setArColIndex(index: Int) {
        _uiState.value = _uiState.value.copy(arColIndex = index)
    }

    fun setExampleEnIndex(index: Int) {
        _uiState.value = _uiState.value.copy(exampleEnIndex = index)
    }

    fun setExampleArIndex(index: Int) {
        _uiState.value = _uiState.value.copy(exampleArIndex = index)
    }

    fun setIsCreatingNewList(isNew: Boolean) {
        _uiState.value = _uiState.value.copy(isCreatingNewList = isNew)
    }

    fun setNewListName(name: String) {
        _uiState.value = _uiState.value.copy(newListName = name)
    }

    fun setSelectedListId(listId: Long) {
        _uiState.value = _uiState.value.copy(selectedListId = listId)
    }

    fun startImport() {
        val s = _uiState.value
        val preview = s.previewData ?: return

        viewModelScope.launch {
            _uiState.value = s.copy(isImporting = true, errorMessage = null)
            try {
                val targetListId = if (s.isCreatingNewList) {
                    val listName = if (s.newListName.isNotBlank()) s.newListName.trim() else "قائمة مستوردة"
                    repository.createList(listName)
                } else {
                    s.selectedListId ?: throw IllegalArgumentException("يرجى اختيار القائمة المراد الإضافة إليها")
                }

                val words = LexiImporter.mapRowsToWords(
                    rawRows = preview.rawRows,
                    listId = targetListId,
                    enColIndex = s.enColIndex,
                    arColIndex = s.arColIndex,
                    exampleEnIndex = s.exampleEnIndex,
                    exampleArIndex = s.exampleArIndex
                )

                if (words.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        errorMessage = "لم يتم العثور على كلمات صالحة في الأعمدة المحددة"
                    )
                    return@launch
                }

                val result = repository.batchInsertWords(
                    listId = targetListId,
                    words = words,
                    skipDuplicates = true
                )

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importSuccessCount = result.first,
                    duplicateCount = result.second
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = e.message ?: "فشلت عملية الاستيراد"
                )
            }
        }
    }
}

package com.example.ui.words

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Word
import com.example.data.model.WordStatus
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditWordUiState(
    val english: String = "",
    val arabic: String = "",
    val exampleEn: String = "",
    val exampleAr: String = "",
    val derivedEn: String = "",
    val derivedAr: String = "",
    val isEditing: Boolean = false,
    val wordId: Long? = null,
    val isDuplicateWarning: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val shouldClose: Boolean = false,
    val errorMessage: String? = null
)

class AddEditWordViewModel(
    application: Application,
    private val listId: Long,
    private val initialWordId: Long?
) : AndroidViewModel(application) {

    private val repository = VocabRepository(AppDatabase.getDatabase(application))

    private val _uiState = MutableStateFlow(AddEditWordUiState())
    val uiState: StateFlow<AddEditWordUiState> = _uiState.asStateFlow()

    init {
        if (initialWordId != null && initialWordId > 0) {
            viewModelScope.launch {
                val existingWord = repository.getWordById(initialWordId)
                if (existingWord != null) {
                    _uiState.value = AddEditWordUiState(
                        english = existingWord.english,
                        arabic = existingWord.arabic,
                        exampleEn = existingWord.exampleEn,
                        exampleAr = existingWord.exampleAr,
                        derivedEn = existingWord.derivedEn,
                        derivedAr = existingWord.derivedAr,
                        isEditing = true,
                        wordId = existingWord.id
                    )
                }
            }
        }
    }

    fun onEnglishChange(text: String) {
        _uiState.value = _uiState.value.copy(english = text, isDuplicateWarning = false)
        if (text.isNotBlank() && !_uiState.value.isEditing) {
            viewModelScope.launch {
                val isDup = repository.isWordDuplicate(listId, text)
                _uiState.value = _uiState.value.copy(isDuplicateWarning = isDup)
            }
        }
    }

    fun onArabicChange(text: String) {
        _uiState.value = _uiState.value.copy(arabic = text)
    }

    fun onExampleEnChange(text: String) {
        _uiState.value = _uiState.value.copy(exampleEn = text)
    }

    fun onExampleArChange(text: String) {
        _uiState.value = _uiState.value.copy(exampleAr = text)
    }

    fun onDerivedEnChange(text: String) {
        _uiState.value = _uiState.value.copy(derivedEn = text)
    }

    fun onDerivedArChange(text: String) {
        _uiState.value = _uiState.value.copy(derivedAr = text)
    }

    fun saveWord(closeAfterSave: Boolean) {
        val s = _uiState.value
        val cleanEn = s.english.trim()
        val cleanAr = s.arabic.trim()

        if (cleanEn.isBlank()) {
            _uiState.value = s.copy(errorMessage = "يرجى إدخال الكلمة بالإنجليزية")
            return
        }
        if (cleanAr.isBlank()) {
            _uiState.value = s.copy(errorMessage = "يرجى إدخال المعنى بالعربية")
            return
        }

        viewModelScope.launch {
            if (s.isEditing && s.wordId != null) {
                val current = repository.getWordById(s.wordId)
                if (current != null) {
                    repository.updateWord(
                        current.copy(
                            english = cleanEn,
                            arabic = cleanAr,
                            exampleEn = s.exampleEn.trim(),
                            exampleAr = s.exampleAr.trim(),
                            derivedEn = s.derivedEn.trim(),
                            derivedAr = s.derivedAr.trim()
                        )
                    )
                }
                _uiState.value = s.copy(shouldClose = true)
            } else {
                repository.addWord(
                    Word(
                        listId = listId,
                        english = cleanEn,
                        arabic = cleanAr,
                        exampleEn = s.exampleEn.trim(),
                        exampleAr = s.exampleAr.trim(),
                        derivedEn = s.derivedEn.trim(),
                        derivedAr = s.derivedAr.trim(),
                        status = WordStatus.UNCATEGORIZED
                    )
                )

                if (closeAfterSave) {
                    _uiState.value = s.copy(shouldClose = true)
                } else {
                    // Reset fields for adding next word immediately
                    _uiState.value = AddEditWordUiState(
                        isSavedSuccess = true
                    )
                }
            }
        }
    }

    fun clearSuccessNotice() {
        _uiState.value = _uiState.value.copy(isSavedSuccess = false)
    }
}

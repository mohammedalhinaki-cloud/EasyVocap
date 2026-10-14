package com.example.ui.words

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TtsManager
import com.example.data.db.AppDatabase
import com.example.data.model.Word
import com.example.data.model.WordStatus
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WordBoxViewModel(
    application: Application,
    private val listId: Long,
    private val filterType: String
) : AndroidViewModel(application) {

    private val repository = VocabRepository(AppDatabase.getDatabase(application))
    val ttsManager = TtsManager(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val sourceFlow = when (filterType) {
        "FORGOTTEN" -> repository.getWordsByListIdAndStatus(listId, WordStatus.FORGOTTEN)
        "SAVED" -> repository.getWordsByListIdAndStatus(listId, WordStatus.SAVED)
        "UNCATEGORIZED" -> repository.getWordsByListIdAndStatus(listId, WordStatus.UNCATEGORIZED)
        else -> repository.getWordsByListId(listId)
    }

    val words: StateFlow<List<Word>> = sourceFlow
        .combine(_searchQuery) { wordList, query ->
            if (query.isBlank()) {
                wordList
            } else {
                val q = query.trim().lowercase()
                wordList.filter {
                    it.english.lowercase().contains(q) || it.arabic.contains(q)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun speak(text: String) {
        ttsManager.speak(text)
    }

    fun deleteWord(wordId: Long) {
        viewModelScope.launch {
            repository.deleteWord(wordId)
        }
    }

    fun updateWordStatus(word: Word, newStatus: String) {
        viewModelScope.launch {
            repository.updateWord(word.copy(status = newStatus))
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

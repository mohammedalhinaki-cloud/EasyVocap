package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.VocabListWithStats
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VocabRepository(AppDatabase.getDatabase(application))

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val lists: StateFlow<List<VocabListWithStats>> = repository.listsWithStats
        .combine(_searchQuery) { allLists, query ->
            if (query.isBlank()) {
                allLists
            } else {
                allLists.filter { it.name.contains(query.trim(), ignoreCase = true) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun createList(name: String, onCreated: (Long) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createList(name.trim())
            onCreated(id)
        }
    }

    fun renameList(listId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val list = repository.getListById(listId)
            if (list != null) {
                repository.updateList(list.copy(name = newName.trim()))
            }
        }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch {
            repository.deleteList(listId)
        }
    }
}

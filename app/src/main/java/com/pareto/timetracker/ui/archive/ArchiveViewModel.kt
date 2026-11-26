package com.pareto.timetracker.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.data.repo.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    /** null: henüz yüklenmedi. */
    val archived: StateFlow<List<CategoryEntity>?> = categoryRepository.archivedCategories
        .stateIn<List<CategoryEntity>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun restore(id: Long) {
        viewModelScope.launch { categoryRepository.restore(id) }
    }

    /** Kategori, oturumları ve hedefleri kalıcı olarak silinir. */
    fun deletePermanently(id: Long) {
        viewModelScope.launch { categoryRepository.deletePermanently(id) }
    }
}

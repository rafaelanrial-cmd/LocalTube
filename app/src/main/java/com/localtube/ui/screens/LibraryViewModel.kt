package com.localtube.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localtube.LocalTubeApp
import com.localtube.data.db.VideoEntity
import com.localtube.data.repo.VideoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel экрана «Библиотека» (Этап 3).
 * Данные: Flow из VideoDao.getAll() через VideoRepository.
 */
class LibraryViewModel(private val repository: VideoRepository) : ViewModel() {

    /** Все скачанные видео, сначала новые по дате загрузки. */
    val videos: StateFlow<List<VideoEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        /** Фабрика: репозиторий берём из AppContainer (ручной DI, без Hilt). */
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = LocalTubeApp.instance
                return LibraryViewModel(app.container.videoRepository) as T
            }
        }
    }
}

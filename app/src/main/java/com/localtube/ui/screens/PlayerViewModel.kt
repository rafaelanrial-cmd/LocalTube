package com.localtube.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localtube.LocalTubeApp
import com.localtube.data.db.VideoEntity
import com.localtube.data.repo.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Состояние экрана «Плеер» (Этап 4). */
sealed interface PlayerUiState {
    /** Запись ещё грузится из Room. */
    data object Loading : PlayerUiState

    /** Всё готово: запись есть и файл на диске. */
    data class Ready(val video: VideoEntity, val file: File) : PlayerUiState

    /** Записи с таким id нет или файла на диске больше нет. */
    data object FileNotFound : PlayerUiState
}

/**
 * ViewModel экрана «Плеер» (Этап 4).
 * Грузит запись видео по id из маршрута player/{id}.
 */
class PlayerViewModel(
    private val repository: VideoRepository,
    videoId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val video = repository.getById(videoId.toInt())
            val file = video?.videoFilePath?.takeIf { it.isNotEmpty() }?.let(::File)
            _uiState.value = when {
                video == null || file == null || !file.exists() -> PlayerUiState.FileNotFound
                else -> PlayerUiState.Ready(video, file)
            }
        }
    }

    companion object {
        /** Фабрика: репозиторий берём из AppContainer (ручной DI, без Hilt). */
        fun factory(videoId: Long): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = LocalTubeApp.instance
                return PlayerViewModel(app.container.videoRepository, videoId) as T
            }
        }
    }
}

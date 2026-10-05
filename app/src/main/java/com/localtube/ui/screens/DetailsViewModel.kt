package com.localtube.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localtube.DebugFlags
import com.localtube.LocalTubeApp
import com.localtube.data.db.VideoEntity
import com.localtube.data.repo.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Состояние экрана «Детали видео» (Этап 5). */
sealed interface DetailsUiState {
    /** Запись ещё грузится из Room. */
    data object Loading : DetailsUiState

    /** Записи с таким id в базе больше нет. */
    data object NotFound : DetailsUiState

    /** Запись загружена; deleting — идёт удаление, кнопки блокируются. */
    data class Ready(val video: VideoEntity, val deleting: Boolean = false) : DetailsUiState
}

/**
 * ViewModel экрана «Детали видео» (Этап 5).
 * Грузит запись по id из маршрута details/{id}; удаляет видео с диска и из БД.
 */
class DetailsViewModel(
    private val repository: VideoRepository,
    videoId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val video = repository.getById(videoId)
            _uiState.value = if (video == null) DetailsUiState.NotFound
                             else DetailsUiState.Ready(video)
        }
    }

    /**
     * Удаляет видео: сначала файлы на диске (видео + превью), затем запись из БД —
     * задачи скачивания и комментарии уходят CASCADE. true, если всё удалено.
     */
    suspend fun deleteVideo(): Boolean {
        val state = _uiState.value as? DetailsUiState.Ready ?: return false
        if (state.deleting) return false
        val video = state.video
        setDeleting(true)
        return try {
            debugLog("DELETE_START id=${video.id} sizeBytes=${video.sizeBytes}")
            deleteFile(video.videoFilePath)
            deleteFile(video.thumbnailPath)
            repository.delete(video)
            debugLog("DELETE_DONE id=${video.id}")
            true
        } catch (e: Exception) {
            debugLog("DELETE_ERROR ${e.message}")
            false
        } finally {
            setDeleting(false)
        }
    }

    private fun setDeleting(deleting: Boolean) {
        val state = _uiState.value as? DetailsUiState.Ready ?: return
        _uiState.value = state.copy(deleting = deleting)
    }

    /** Удаляет файл, если путь непустой; отсутствующий файл — не ошибка. */
    private fun deleteFile(path: String) {
        if (path.isBlank()) return
        val file = File(path)
        if (file.exists() && !file.delete()) {
            throw IllegalStateException("Не удалось удалить файл: ${file.name}")
        }
    }

    // TODO(Этап 12): УДАЛИТЬ — лог-маркеры под тегом LocalTubeDebug при включённом флаге.
    private fun debugLog(message: String) {
        val context = LocalTubeApp.instance.container.context
        if (DebugFlags.detailsFlagFile(context).exists()) {
            Log.d(DebugFlags.TAG, message)
        }
    }

    companion object {
        /** Фабрика: репозиторий берём из AppContainer (ручной DI, без Hilt). */
        fun factory(videoId: Long): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = LocalTubeApp.instance
                return DetailsViewModel(app.container.videoRepository, videoId.toInt()) as T
            }
        }
    }
}

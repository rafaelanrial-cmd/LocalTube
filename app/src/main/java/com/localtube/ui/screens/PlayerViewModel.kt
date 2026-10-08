package com.localtube.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.core.net.toUri
import androidx.media3.exoplayer.ExoPlayer
import com.localtube.DebugFlags
import com.localtube.LocalTubeApp
import com.localtube.data.db.VideoEntity
import com.localtube.data.repo.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
 *
 * Память позиции (баг-фикс 2026-10-08): ExoPlayer живёт здесь, а не в
 * композиции — ViewModel переживает поворот экрана (смену конфигурации),
 * поэтому при смене ориентации воспроизведение не сбрасывается. Место
 * остановки пишется в Room (videos.positionMs) и подхватывается при
 * следующем запуске приложения.
 */
class PlayerViewModel(
    private val repository: VideoRepository,
    private val videoId: Long,
) : ViewModel() {

    /** Игрок живёт в ViewModel: переживает поворот экрана (баг-фикс 2026-10-08). */
    val player: ExoPlayer = ExoPlayer.Builder(LocalTubeApp.instance).build()

    /** Пауза при уходе в фон/поворте: true, если на ON_START нужно возобновить. */
    @Volatile
    var shouldResume = false

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val video = repository.getById(videoId.toInt())
            val file = video?.videoFilePath?.takeIf { it.isNotEmpty() }?.let(::File)
            if (video == null || file == null || !file.exists()) {
                _uiState.value = PlayerUiState.FileNotFound
                return@launch
            }
            _uiState.value = PlayerUiState.Ready(video, file)
            player.setMediaItem(MediaItem.Builder().setUri(file.toUri()).build())
            // Память позиции: продолжить с сохранённого места.
            val startPos = PlaybackPosition.startPositionFor(video)
            player.seekTo(startPos)
            // TODO(Этап 12): УДАЛИТЬ — маркер для проверки памяти позиции.
            if (DebugFlags.sessionActive) {
                Log.d(DebugFlags.TAG, "PLAYER_START seekTo=${startPos}ms")
            }
            player.prepare()
            player.play()
        }
    }

    /** Память позиции (баг-фикс 2026-10-08): сохранить место воспроизведения (мс). */
    fun savePosition(positionMs: Long) {
        if (positionMs <= 0L) return
        viewModelScope.launch {
            runCatching { repository.updatePosition(videoId.toInt(), positionMs) }
        }
    }

    override fun onCleared() {
        // Выход с экрана плеера (назад): сохранить место, затем release.
        val pos = player.currentPosition
        if (pos > 0L) {
            runBlocking { repository.updatePosition(videoId.toInt(), pos) }
            // TODO(Этап 12): УДАЛИТЬ — маркер для проверки памяти позиции.
            if (DebugFlags.sessionActive) {
                Log.d(DebugFlags.TAG, "POSITION_SAVED pos=${pos}ms on=closed")
            }
        }
        player.release()
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

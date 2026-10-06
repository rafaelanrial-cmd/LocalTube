package com.localtube.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localtube.DebugFlags
import com.localtube.LocalTubeApp
import com.localtube.data.db.DownloadTaskEntity
import com.localtube.data.db.VideoEntity
import com.localtube.data.downloader.AudioChoice
import com.localtube.data.downloader.VideoInfoData
import com.localtube.data.downloader.YtDlpManager
import com.localtube.data.repo.DownloadRepository
import com.localtube.data.repo.VideoRepository
import com.localtube.service.DownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Состояние экрана «Добавить» (Этап 6). */
sealed interface AddUiState {
    /** Ссылка ещё не проверялась. */
    data object Idle : AddUiState

    /** Идёт получение метаданных через yt-dlp. */
    data object Checking : AddUiState

    /** Метаданные получены; показаны карточка и опции. */
    data class Checked(val info: VideoInfoData) : AddUiState

    /** Не удалось получить метаданные. */
    data object CheckFailed : AddUiState
}

/**
 * ViewModel экрана «Добавить» (Этапы 6–7): валидация ссылки, получение метаданных
 * через yt-dlp, опции скачивания. Кнопка «Скачать» создаёт VideoEntity +
 * DownloadTask(QUEUED) с выбранными опциями и запускает DownloadService.
 */
class AddViewModel(
    private val videoRepository: VideoRepository,
    private val downloadRepository: DownloadRepository,
    private val ytDlpManager: YtDlpManager,
    // TODO(Этап 12): УДАЛИТЬ — отладочная автопроверка ссылки (MIUI блокирует input).
    autoCheckUrl: String? = null,
) : ViewModel() {

    private val _urlText = MutableStateFlow("")
    val urlText: StateFlow<String> = _urlText.asStateFlow()

    private val _uiState = MutableStateFlow<AddUiState>(AddUiState.Idle)
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    private val _audioChoice = MutableStateFlow(AudioChoice.ORIGINAL)
    val audioChoice: StateFlow<AudioChoice> = _audioChoice.asStateFlow()

    /** Качество видео (высота в пикселях): 720 или 1080. */
    private val _qualityHeight = MutableStateFlow(QUALITY_720)
    val qualityHeight: StateFlow<Int> = _qualityHeight.asStateFlow()

    /** Скачивать комментарии (вкл по умолчанию). */
    private val _withComments = MutableStateFlow(true)
    val withComments: StateFlow<Boolean> = _withComments.asStateFlow()

    /** «Скачать» выполняется — защита от двойного клика. */
    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading.asStateFlow()

    // TODO(Этап 12): УДАЛИТЬ — автоскачивание сработало (экран сам уходит на «Загрузки»).
    private val _autoDownloaded = MutableStateFlow(false)
    val autoDownloaded: StateFlow<Boolean> = _autoDownloaded.asStateFlow()

    init {
        // TODO(Этап 12): УДАЛИТЬ — автопереход: подставлена ссылка и сразу проверка.
        if (autoCheckUrl != null) {
            _urlText.value = autoCheckUrl
            onCheckClick()
        }
    }

    fun onUrlChanged(text: String) {
        _urlText.value = text
    }

    /** Кнопка «Проверить»: получить метаданные по ссылке. */
    fun onCheckClick() {
        val url = _urlText.value.trim()
        if (!isValidYouTubeUrl(url)) return
        viewModelScope.launch {
            _uiState.value = AddUiState.Checking
            try {
                val info = ytDlpManager.fetchInfo(url)
                debugLog(
                    "FETCH_OK id=${info.youtubeId} " +
                        "langs=${info.audioLangs.joinToString(",")} hasRu=${info.hasRuTrack}"
                )
                // Сбрасываем опции под новое видео.
                _audioChoice.value = AudioChoice.ORIGINAL
                _qualityHeight.value = QUALITY_720
                _withComments.value = true
                _uiState.value = AddUiState.Checked(info)
                // TODO(Этап 12): УДАЛИТЬ — автоскачивание (автотест Этапа 7).
                val ctx = LocalTubeApp.instance.container.context
                if (DebugFlags.autoDownloadFlagFile(ctx).exists()) {
                    debugLog("AUTO_DOWNLOAD flag present")
                    if (download()) _autoDownloaded.value = true
                }
            } catch (e: Exception) {
                debugLog("FETCH_FAIL url=$url error=${e.message}")
                _uiState.value = AddUiState.CheckFailed
            }
        }
    }

    fun onAudioSelected(choice: AudioChoice) {
        if (choice == AudioChoice.RU && !hasRu()) return // «Русская» видна только если ru есть
        _audioChoice.value = choice
    }

    private fun hasRu(): Boolean =
        (_uiState.value as? AddUiState.Checked)?.info?.hasRuTrack == true

    fun onQualitySelected(height: Int) {
        if (height != QUALITY_720 && height != QUALITY_1080) return
        _qualityHeight.value = height
    }

    fun onCommentsChanged(checked: Boolean) {
        _withComments.value = checked
    }

    /**
     * Кнопка «Скачать»: создаёт VideoEntity + DownloadTask(QUEUED) в базе.
     * @return true, если задача создана (вызывающий переходит на экран Загрузок).
     */
    suspend fun download(): Boolean {
        val state = _uiState.value as? AddUiState.Checked ?: return false
        if (_downloading.value) return false
        _downloading.value = true
        return try {
            val info = state.info
            val videoId = videoRepository.insert(
                VideoEntity(
                    youtubeId = info.youtubeId,
                    title = info.title,
                    description = info.description,
                    uploadDateEpoch = info.uploadDateEpoch,
                    durationSec = info.durationSec,
                    // thumbnailPath/videoFilePath/sizeBytes — заполнит скачивание (Этап 7).
                    audioLangs = info.audioLangs.joinToString(","),
                    hasRuTrack = info.hasRuTrack,
                )
            )
            downloadRepository.insertTask(
                DownloadTaskEntity(
                    videoId = videoId.toInt(),
                    url = _urlText.value.trim(),
                    qualityHeight = _qualityHeight.value,
                    audioChoice = _audioChoice.value,
                    withComments = _withComments.value,
                ),
            )
            // Этап 7: сервис берёт задачу из очереди (FIFO) и скачивает.
            DownloadService.start(LocalTubeApp.instance.container.context)
            debugLog(
                "TASK_QUEUED videoId=$videoId audio=${_audioChoice.value} " +
                    "quality=${_qualityHeight.value} comments=${_withComments.value}"
            )
            true
        } finally {
            _downloading.value = false
        }
    }

    // TODO(Этап 12): УДАЛИТЬ — лог-маркеры под тегом LocalTubeDebug при включённом флаге.
    private fun debugLog(message: String) {
        val context = LocalTubeApp.instance.container.context
        if (DebugFlags.addUrlFlagFile(context).exists()) {
            Log.d(DebugFlags.TAG, message)
        }
    }

    companion object {
        const val QUALITY_720 = 720
        const val QUALITY_1080 = 1080

        /** Валидация ссылки: youtube.com/watch (с v=) или youtu.be. */
        fun isValidYouTubeUrl(url: String): Boolean =
            WATCH_URL.matches(url) || SHORT_URL.matches(url)

        private val WATCH_URL =
            Regex("^https?://(?:www\\.)?youtube\\.com/watch\\?.*v=[A-Za-z0-9_-]{11}")
        private val SHORT_URL =
            Regex("^https?://youtu\\.be/[A-Za-z0-9_-]{11}(?:[/?].*)?$")

        /** Фабрика: репозитории и движок из AppContainer (ручной DI, без Hilt). */
        fun factory(autoCheckUrl: String? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val container = LocalTubeApp.instance.container
                    return AddViewModel(
                        container.videoRepository,
                        container.downloadRepository,
                        container.ytDlpManager,
                        autoCheckUrl,
                    ) as T
                }
            }
    }
}

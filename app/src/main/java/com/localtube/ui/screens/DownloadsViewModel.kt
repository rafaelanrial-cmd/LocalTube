package com.localtube.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localtube.DebugFlags
import com.localtube.LocalTubeApp
import com.localtube.data.db.DownloadStatus
import com.localtube.data.db.DownloadTaskRow
import com.localtube.data.repo.DownloadRepository
import com.localtube.service.DownloadService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel экрана «Загрузки» (Этап 7): список задач из БД + отмена.
 * Данные: Flow из DownloadTaskDao.getRows() через DownloadRepository.
 */
class DownloadsViewModel(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    /** Все задачи с названием видео, сначала новые; разбивка на секции — в UI. */
    val tasks: StateFlow<List<DownloadTaskRow>> = downloadRepository.observeTaskRows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // TODO(Этап 12): УДАЛИТЬ — автотест отмены (Этап 7): если в файле-флаге
        // задан процент, RUNNING-задача отменяется, когда прогресс его достигает.
        val ctx = LocalTubeApp.instance.container.context
        val cancelAt = DebugFlags.readCancelAt(ctx)
        if (cancelAt != null) {
            Log.d(DebugFlags.TAG, "AUTO_CANCEL flag: $cancelAt%")
            viewModelScope.launch {
                tasks.collect { list ->
                    val running = list.firstOrNull { it.status == DownloadStatus.RUNNING } ?: return@collect
                    if (running.progressPercent >= cancelAt) {
                        Log.d(DebugFlags.TAG, "AUTO_CANCEL at ${running.progressPercent}%")
                        cancelTask(running.id)
                    }
                }
            }
        }
    }

    /** Отмена: QUEUED — сразу CANCELED; RUNNING — остановить процесс yt-dlp. */
    fun cancelTask(taskId: Int) {
        viewModelScope.launch {
            val task = downloadRepository.getTask(taskId) ?: return@launch
            when (task.status) {
                DownloadStatus.QUEUED ->
                    downloadRepository.updateTask(task.copy(status = DownloadStatus.CANCELED))
                DownloadStatus.RUNNING ->
                    if (!DownloadService.cancelRunning()) {
                        // Сервис недоступен (например, убит системой) — помечаем
                        // отменённой; процесс умирает вместе с ним.
                        downloadRepository.updateTask(task.copy(status = DownloadStatus.CANCELED))
                    }
                else -> Unit
            }
        }
    }

    companion object {
        /** Фабрика: репозиторий из AppContainer (ручной DI, без Hilt). */
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = LocalTubeApp.instance.container
                return DownloadsViewModel(container.downloadRepository) as T
            }
        }
    }
}

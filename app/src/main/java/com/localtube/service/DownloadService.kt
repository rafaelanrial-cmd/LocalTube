package com.localtube.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.localtube.LocalTubeApp
import com.localtube.MainActivity
import com.localtube.R
import com.localtube.data.db.DownloadStatus
import com.localtube.data.db.DownloadTaskEntity
import com.localtube.data.downloader.VideoDownloader
import com.localtube.util.Formats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground-сервис скачиваний (Этап 7), тип dataSync.
 *
 * Берёт задачи QUEUED по одной (FIFO). Прогресс пишет в БД (~каждые 400 мс),
 * UI читает Flow из Room; уведомление обновляется не чаще раза в секунду.
 */
class DownloadService : Service() {

    private val container get() = LocalTubeApp.instance.container

    private var workerScope: CoroutineScope? = null
    private var currentDownloader: VideoDownloader? = null
    @Volatile private var lastNotifMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        ensureChannel()
        startForeground(NOTIF_ID, buildNotification(null, null))
        if (workerScope == null) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            workerScope = scope
            scope.launch {
                // MIUI может убить сервис, пока процесс приложения жив (Xiaomi):
                // RUNNING-задачи мёртвого экземпляра «осиротевшие» — возвращаем
                // их в очередь. Тот же корутинный поток, что и runQueue, поэтому
                // гонки со сменой статусов нет.
                container.downloadRepository.resetRunningToQueued()
                runQueue(scope)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        workerScope?.cancel()
        workerScope = null
        currentDownloader = null
        if (instance === this) instance = null
    }

    /** Очередь: задачи по одной, пока есть QUEUED; затем сервис останавливается. */
    private suspend fun runQueue(scope: CoroutineScope) {
        while (true) {
            val task = container.downloadRepository.getNextQueued() ?: break
            val video = container.videoRepository.getById(task.videoId)
            if (video == null) {
                // Видео удалили, пока задача ждала — пропускаем.
                container.downloadRepository.updateTask(
                    task.copy(status = DownloadStatus.FAILED, errorMsg = getString(R.string.dl_error_no_video)),
                )
                continue
            }
            container.downloadRepository.updateTask(
                task.copy(status = DownloadStatus.RUNNING, progressPercent = 0, speedBps = 0L, etaSec = 0L),
            )
            val downloader = VideoDownloader(
                container.context,
                container.ytDlpManager,
                container.downloadRepository,
                container.videoRepository,
            )
            currentDownloader = downloader
            val pollJob = scope.launch { pollProgress(downloader, task, video.title) }
            try {
                downloader.run(task, video)
            } finally {
                pollJob.cancel()
                currentDownloader = null
            }
        }
        workerScope?.cancel()
        workerScope = null
        if (instance === this) instance = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Опрос значений колбэка: скорость по дельте (lastBytes/lastTimeMs),
     * запись в БД ~каждые 400 мс, уведомление — не чаще раза в секунду.
     */
    private suspend fun pollProgress(
        downloader: VideoDownloader,
        task: DownloadTaskEntity,
        title: String,
    ) {
        var last = task
        while (true) {
            delay(POLL_MS)
            val h = downloader.holder
            // Свежая копия из БД: не затираем статус/метки, которые пишет
            // VideoDownloader; если задача уже не RUNNING — опрос окончен.
            val current = container.downloadRepository.getTask(task.id) ?: return
            if (current.status != DownloadStatus.RUNNING) return
            val now = System.currentTimeMillis()
            var speedBps = 0L
            if (h.hasBytes && last.lastBytes > 0L && now > last.lastTimeMs) {
                speedBps = ((h.bytes - last.lastBytes).coerceAtLeast(0L) * 1_000L) / (now - last.lastTimeMs)
            }
            last = current.copy(
                progressPercent = h.progress.toInt().coerceIn(0, 100),
                speedBps = speedBps,
                etaSec = if (h.etaSec in 0..3_600L) h.etaSec else 0L,
                lastBytes = if (h.hasBytes) h.bytes else current.lastBytes,
                lastTimeMs = now,
            )
            container.downloadRepository.updateTask(last)
            updateNotification(last, title)
        }
    }

    /** Отмена текущей задачи: останавливает процесс yt-dlp. */
    private fun cancelCurrent(): Boolean {
        val downloader = currentDownloader ?: return false
        downloader.cancel()
        return true
    }

    // --- Уведомление ---

    private fun updateNotification(task: DownloadTaskEntity, title: String) {
        val now = System.currentTimeMillis()
        if (now - lastNotifMs < NOTIF_THROTTLE_MS) return
        lastNotifMs = now
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, buildNotification(task, title))
    }

    private fun buildNotification(task: DownloadTaskEntity?, videoTitle: String?): Notification {
        val content = if (task == null || task.status != DownloadStatus.RUNNING) {
            getString(R.string.dl_notif_starting)
        } else {
            val parts = mutableListOf("${task.progressPercent}%")
            if (task.speedBps > 0L) {
                parts.add(getString(R.string.dl_speed, Formats.speedMb(task.speedBps)))
            }
            if (task.etaSec > 0L) {
                val total = task.etaSec.toInt()
                parts.add(
                    if (total < 60) getString(R.string.dl_eta_sec, total)
                    else getString(R.string.dl_eta_min, total / 60, total % 60),
                )
            }
            parts.joinToString(" · ")
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle(videoTitle ?: getString(R.string.app_name))
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent())
            .build()
    }

    private fun openAppPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.dl_notif_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val NOTIF_ID = 7001
        private const val POLL_MS = 400L
        private const val NOTIF_THROTTLE_MS = 1_000L

        private var instance: DownloadService? = null

        /** Запускает сервис (при создании задачи и после перезапуска приложения). */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, DownloadService::class.java))
        }

        /** Отменяет текущую задачу; false, если активной нет. */
        fun cancelRunning(): Boolean = instance?.cancelCurrent() ?: false
    }
}

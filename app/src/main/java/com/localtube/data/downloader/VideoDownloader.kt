package com.localtube.data.downloader

import android.content.Context
import android.util.Log
import com.localtube.DebugFlags
import com.localtube.data.db.DownloadStatus
import com.localtube.data.db.DownloadTaskEntity
import com.localtube.data.db.VideoEntity
import com.localtube.data.repo.DownloadRepository
import com.localtube.data.repo.VideoRepository
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Исполнитель одной задачи скачивания (Этап 7):
 * 1) превью: `--write-thumbnail --convert-thumbnails jpg` → thumbs/{youtubeId}.jpg;
 * 2) видео+аудио: `-f "bv*[height<=X]+ba/b"`, объединение ffmpeg из состава
 *    библиотеки в videos/{youtubeId}.mp4 (RU-дорожка — Этап 9).
 *
 * Прогресс приходит колбэком библиотеки (проценты, ETA, сырая строка stderr);
 * скорость считается по дельте: прошлые lastBytes/lastTimeMs хранятся в
 * [DownloadTaskEntity], скорость = (байты − прошлые байты) / (сейчас − прошлое время).
 */
class VideoDownloader(
    private val context: Context,
    private val ytDlpManager: YtDlpManager,
    private val taskRepo: DownloadRepository,
    private val videoRepo: VideoRepository,
) {

    /** Последние значения колбэка: пишутся потоком библиотеки, читает цикл сервиса. */
    class ProgressHolder {
        @Volatile var progress: Float = 0f
        @Volatile var etaSec: Long = -1L
        @Volatile var bytes: Long = 0L
        @Volatile var hasBytes: Boolean = false

        /**
         * Принимает nullable-параметры: сигнатура колбэка библиотеки может
         * передавать и null (тип Function3 из AAR), компиляция проходит в любом случае.
         */
        fun onLine(progress: Float?, etaSec: Long?, line: String?) {
            progress?.let { this.progress = it }
            etaSec?.let { this.etaSec = it }
            parseTotalBytes(line.orEmpty())?.let { total ->
                // «of X MiB» — размер ВСЕГО файла, а не скачанное; дельта от него
                // всегда 0 и скорость не считается. Скачанные байты считаем по
                // процентам из той же строки прогресса.
                val p = progress ?: 0f
                bytes = (total * p / 100f).toLong().coerceIn(0L, total)
                hasBytes = true
            }
        }

        /** «of 30.52MiB» / «of ~ 1.2GiB» в строке прогресса yt-dlp → всего байт. */
        private fun parseTotalBytes(line: String): Long? {
            val m = TOTAL_BYTES_REGEX.find(line) ?: return null
            val value = m.groupValues[1].toDoubleOrNull() ?: return null
            val multiplier = UNIT_MULTIPLIERS[m.groupValues[2]] ?: return null
            return (value * multiplier).toLong().coerceAtLeast(0L)
        }

        companion object {
            private val TOTAL_BYTES_REGEX = Regex("of\\s+(?:~\\s*)?([0-9.]+)\\s*([KMGT]?i?B)")
            private val UNIT_MULTIPLIERS = mapOf(
                "B" to 1.0,
                "KiB" to 1024.0,
                "MiB" to 1_048_576.0,
                "GiB" to 1_073_741_824.0,
                "TiB" to 1_099_511_627_776.0,
            )
        }
    }

    /** Свежие значения колбэка — их опрашивает DownloadService. */
    val holder = ProgressHolder()

    @Volatile private var processId: String? = null
    @Volatile private var cancelledFlag = false

    /** Отмена: останавливает процесс yt-dlp (и дочерний ffmpeg). */
    fun cancel() {
        cancelledFlag = true
        processId?.let { YoutubeDL.destroyProcessById(it) }
    }

    /**
     * Выполняет задачу до конца (превью → видео → DONE в базе).
     * Итоговый статус всегда записывает этот метод.
     */
    suspend fun run(task: DownloadTaskEntity, video: VideoEntity): DownloadStatus =
        withContext(Dispatchers.IO) {
            ytDlpManager.ensureInitialized()
            cancelledFlag = false
            val root = context.getExternalFilesDir(null) ?: context.filesDir
            val thumbsDir = File(root, "thumbs").apply { mkdirs() }
            val videosDir = File(root, "videos").apply { mkdirs() }
            try {
                downloadThumbnail(task, video, thumbsDir)
                if (cancelledFlag) return@withContext finishCanceled(task, video, thumbsDir, videosDir)
                downloadVideo(task, video, videosDir)
                if (cancelledFlag) return@withContext finishCanceled(task, video, thumbsDir, videosDir)
                val thumbFile = File(thumbsDir, "${video.youtubeId}.jpg")
                val videoFile = File(videosDir, "${video.youtubeId}.mp4")
                // Этап 7: пути файлов и размер — видео становится доступным офлайн.
                videoRepo.update(
                    video.copy(
                        thumbnailPath = thumbFile.absolutePath,
                        videoFilePath = videoFile.absolutePath,
                        sizeBytes = videoFile.length(),
                    ),
                )
                updateTaskFresh(task) {
                    it.copy(
                        status = DownloadStatus.DONE,
                        progressPercent = 100,
                        speedBps = 0L,
                        etaSec = 0L,
                        lastBytes = 0L,
                        lastTimeMs = 0L,
                        thumbOk = true,
                        videoOk = true,
                    )
                }
                DownloadStatus.DONE
            } catch (e: CancellationException) {
                // Сервис убит во время скачивания: не пишем FAILED — задача
                // вернётся в очередь новым экземпляром сервиса (onStartCommand).
                throw e
            } catch (e: Exception) {
                if (cancelledFlag) {
                    finishCanceled(task, video, thumbsDir, videosDir)
                } else {
                    finishFailed(task, video, thumbsDir, videosDir, e)
                }
            }
        }

    /** Превью: `--skip-download --write-thumbnail --convert-thumbnails jpg`. */
    private suspend fun downloadThumbnail(
        task: DownloadTaskEntity,
        video: VideoEntity,
        thumbsDir: File,
    ) {
        val request = YoutubeDLRequest(task.url)
            .addOption("--no-playlist")
            .addOption("--no-update") // без проверки обновления: тише и быстрее
            .addOption("--skip-download")
            .addOption("--write-thumbnail")
            .addOption("--convert-thumbnails", "jpg")
            // Точка обязательна: %(ext)s подставляется БЕЗ точки (иначе файл
            // назовётся «jNQXAC9IVRwwebm.jpg» вместо «jNQXAC9IVRw.jpg»).
            .addOption("-o", File(thumbsDir, "${video.youtubeId}.%(ext)s").absolutePath)
        val response = executeTracked(request, "thumb-${task.id}")
        dbg("THUMB_EXIT code=${response.exitCode} stderr_tail=${stderrTail(response)}")
        if (response.exitCode != 0) {
            throw YtDlpException("Превью: ${lastStderrLine(response)}")
        }
        if (!File(thumbsDir, "${video.youtubeId}.jpg").exists()) {
            // yt-dlp завершается с кодом 0 и без файла, если превью не скачалось
            // (например, сетевое предупреждение) — stderr в сообщении покажет причину.
            throw YtDlpException(
                "Файл превью не найден после скачивания. stderr: ${stderrTail(response)}",
            )
        }
        updateTaskFresh(task) { it.copy(thumbOk = true) }
    }

    /** Видео+аудио с объединением в mp4; прогресс — колбэком библиотеки. */
    private suspend fun downloadVideo(
        task: DownloadTaskEntity,
        video: VideoEntity,
        videosDir: File,
    ) {
        val request = YoutubeDLRequest(task.url)
            .addOption("--no-playlist")
            .addOption("--no-update") // без проверки обновления: тише и быстрее
            .addOption("--extractor-args", "youtube:player_client=${playerClient()}")
            .addOption("-f", "bv*[height<=${task.qualityHeight}]+ba/b")
            .addOption("--merge-output-format", "mp4")
            // Точка обязательна: %(ext)s подставляется БЕЗ точки.
            .addOption("-o", File(videosDir, "${video.youtubeId}.%(ext)s").absolutePath)
        val response = executeTracked(request, "video-${task.id}") { progress, etaSec, line ->
            holder.onLine(progress, etaSec, line)
        }
        dbg("VIDEO_EXIT code=${response.exitCode} stderr_tail=${stderrTail(response)}")
        if (response.exitCode != 0) {
            throw YtDlpException("Скачивание: ${lastStderrLine(response)}")
        }
        if (!File(videosDir, "${video.youtubeId}.mp4").exists()) {
            throw YtDlpException(
                "Файл видео не найден после скачивания. stderr: ${stderrTail(response)}",
            )
        }
    }

    /**
     * Клиент плеера YouTube для yt-dlp. По умолчанию `android` (2026-10-06):
     * в этой стране YouTube заблокирован, весь трафик идёт через ByeDPI, и
     * именно android стабильно проходит без 403; default/tv/ios/mweb/
     * android_vr/tv_embedded на текущем выходном IP блокируются.
     *
     * TODO(Этап 12): УДАЛИТЬ — файл debug_player_client переопределяет клиент
     * (автотест: какие клиенты проходят без блокировки на текущей сети).
     */
    private fun playerClient(): String {
        val file = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            "debug_player_client",
        )
        if (file.exists()) {
            file.readText().trim().takeIf { it.isNotEmpty() }?.let { return it }
        }
        // 2026-10-06: дефолт tv → android. При работающем ByeDPI клиенты default/tv/ios/
        // mweb/android_vr/tv_embedded дают 403 (анти-абьюз YouTube по выходному IP),
        // а android стабильно скачивает (task 10, Id5RNRX30Gk DONE). Если 403 вернётся —
        // перезапустить ByeDPI (новый IP) и/или перебрать клиентов через debug-файл.
        return "android"
    }

    /** Запускает yt-dlp, запоминая id процесса для отмены. */
    private suspend fun executeTracked(
        request: YoutubeDLRequest,
        processId: String,
        // Nullable-параметры: совместимо и с Function3<Float?, Long?, String?> из AAR.
        onLine: ((Float?, Long?, String?) -> Unit)? = null,
    ): YoutubeDLResponse {
        this.processId = processId
        return try {
            val response = if (onLine == null) {
                YoutubeDL.execute(request, processId)
            } else {
                YoutubeDL.execute(request, processId, onLine)
            }
            dumpDebug(processId, "exitCode=${response.exitCode}", response.out + "\n===== ERR =====\n" + response.err)
            response
        } catch (e: Exception) {
            // TODO(Этап 12): УДАЛИТЬ — при ненулевом exit code библиотека бросает
            // YoutubeDLException, у которого message = полный stderr; сохраняем его.
            dumpDebug(processId, "EXCEPTION ${e::class.java.name}", e.message.orEmpty())
            throw e
        } finally {
            this.processId = null
        }
    }

    /** TODO(Этап 12): УДАЛИТЬ — дамп out/err в файл на телефоне (только при флаге). */
    private fun dumpDebug(processId: String, header: String, body: String) {
        if (!DebugFlags.addUrlFlagFile(context).exists()) return
        val dump = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            "debug_ytdlp_$processId.txt",
        )
        dump.writeText("$header\n===== BODY =====\n$body")
    }

    private suspend fun finishCanceled(
        task: DownloadTaskEntity,
        video: VideoEntity,
        thumbsDir: File,
        videosDir: File,
    ): DownloadStatus {
        dbg("PARTIAL_FILES " + partialListing(video.youtubeId, thumbsDir, videosDir))
        deletePartialFiles(video.youtubeId, thumbsDir, videosDir)
        updateTaskFresh(task) { it.copy(status = DownloadStatus.CANCELED, speedBps = 0L, etaSec = 0L) }
        return DownloadStatus.CANCELED
    }

    private suspend fun finishFailed(
        task: DownloadTaskEntity,
        video: VideoEntity,
        thumbsDir: File,
        videosDir: File,
        error: Exception,
    ): DownloadStatus {
        dbg("PARTIAL_FILES " + partialListing(video.youtubeId, thumbsDir, videosDir))
        deletePartialFiles(video.youtubeId, thumbsDir, videosDir)
        val message = extractErrorMessage(error)
        updateTaskFresh(task) {
            it.copy(status = DownloadStatus.FAILED, errorMsg = message, speedBps = 0L, etaSec = 0L)
        }
        return DownloadStatus.FAILED
    }

    /** Удаляет partial-файлы задачи (расширения .part, .f*, .ytdl) и готовое превью. */
    private fun deletePartialFiles(youtubeId: String, vararg dirs: File) {
        for (dir in dirs) {
            dir.listFiles { file -> file.name.startsWith(youtubeId) }?.forEach { it.delete() }
        }
    }

    /**
     * Обновление задачи по свежей копии из БД: DownloadService параллельно пишет
     * прогресс в тот же ряд, запись по устаревшей копии zатирает его (баг Этапа 7).
     */
    private suspend fun updateTaskFresh(
        task: DownloadTaskEntity,
        transform: (DownloadTaskEntity) -> DownloadTaskEntity,
    ) {
        val fresh = taskRepo.getTask(task.id) ?: task
        taskRepo.updateTask(transform(fresh))
    }

    /** Текст для FAILED: последние строки stderr — именно там причина ошибки. */
    private fun extractErrorMessage(error: Exception): String {
        val raw = error.message ?: "Неизвестная ошибка"
        val lines = raw.trim().lineSequence().filter { it.isNotBlank() }.toList()
        return if (lines.size <= 2) raw.take(300)
        else lines.takeLast(3).joinToString(" | ").take(400)
    }

    /** Последняя непустая строка stderr — туда yt-dlp пишет причину ошибки. */
    private fun lastStderrLine(response: YoutubeDLResponse): String =
        response.err.trim().lineSequence().lastOrNull { it.isNotBlank() }?.take(300)
            ?: "код ${response.exitCode}"

    /** Последние 3 непустые строки stderr — предупреждения yt-dlp тоже туда пишут. */
    private fun stderrTail(response: YoutubeDLResponse): String =
        response.err.trim().lineSequence()
            .filter { it.isNotBlank() }
            .toList()
            .takeLast(3)
            .joinToString(" | ")
            .take(400)

    // TODO(Этап 12): УДАЛИТЬ — маркеры автотеста в logcat (тег LocalTubeDebug).
    private fun dbg(message: String) {
        if (DebugFlags.addUrlFlagFile(context).exists()) {
            Log.d(DebugFlags.TAG, message)
        }
    }

    /** Список файлов задачи до удаления partial — чтобы видеть, что реально создал yt-dlp. */
    private fun partialListing(youtubeId: String, vararg dirs: File): String =
        dirs.joinToString("; ") { dir ->
            dir.listFiles { f -> f.name.startsWith(youtubeId) }?.joinToString(",") {
                "${it.name}(${it.length()})"
            }.orEmpty()
        }
}

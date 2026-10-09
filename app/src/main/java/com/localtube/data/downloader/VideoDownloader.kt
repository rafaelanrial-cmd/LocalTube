package com.localtube.data.downloader

import android.content.Context
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
 * 2) видео+аудио (Этап 9): формат зависит от AudioChoice — ORIGINAL
 *    `bv*[height<=X]+ba/b` и RU `bv*[height<=X]+ba[language=ru]` (mp4), BOTH
 *    `bv*[height<=X]+ba+ba[language=ru]` + --audio-multistreams (mkv);
 * 3) комментарии (Этап 8): при включённых — `--write-comments --write-info-json`
 *    БЕЗ лимита max_comments (2026-10-09: берём ВСЕ, что отдаёт YouTube);
 *    json разбирается в Room и удаляется.
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

    /** Значения колбэка (см. YtDlpProgress.kt): пишутся потоком библиотеки, читает цикл сервиса. */
    val holder = ProgressHolder()

    /** TODO(Этап 12): УДАЛИТЬ — отладочные выводы автотестов (YtDlpDebugLog). */
    private val debugLog = YtDlpDebugLog(context)

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

                // 3. Этап 8: .info.json → Room; файл удаляется внутри функции.
                val commentsOk = if (task.withComments) {
                    // Этап 8: аватары авторов — в avatars/ (дедупликация по каналу).
                    saveCommentsFromInfoJson(
                        taskRepo, video.id, File(videosDir, "${video.youtubeId}.info.json"),
                        File(root, "avatars").apply { mkdirs() },
                        // TODO(Этап 12): УДАЛИТЬ — диагностика: не удалять info.json при флаге.
                        keepJson = DebugFlags.keepJsonFlagFile(context).exists(),
                    ) { debugLog.dbg(it) } >= 0
                } else false

                val thumbFile = File(thumbsDir, "${video.youtubeId}.jpg")
                // Этап 9: контейнер зависит от выбора аудио (BOTH → mkv).
                val videoFile = File(videosDir, "${video.youtubeId}.${containerFor(task.audioChoice)}")
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
                        commentsOk = commentsOk,
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
        debugLog.dbg("THUMB_EXIT code=${response.exitCode} stderr_tail=${stderrTail(response)}")
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

    /** Видео+аудио по AudioChoice (Этап 9); прогресс — колбэком библиотеки. */
    private suspend fun downloadVideo(
        task: DownloadTaskEntity,
        video: VideoEntity,
        videosDir: File,
    ) {
        val request = YoutubeDLRequest(task.url)
            .addOption("--no-playlist")
            .addOption("--no-update") // без проверки обновления: тише и быстрее
            // player_client — ОДИН --extractor-args: повторный опс с тем же ключом
            // youtube в yt-dlp ПЕРЕПИСЫВАЕТ предыдущий. Лимита max_comments НЕТ:
            // в yt-dlp 2026.08.19 пустые слоты = без ограничения (sys.maxsize),
            // поэтому берём все комментарии, которые отдаёт YouTube.
            .addOption(
                "--extractor-args",
                "youtube:player_client=${playerClient()}",
            )
            .addOption("-f", videoFormat(task.qualityHeight, task.audioChoice))
            .addOption("--merge-output-format", containerFor(task.audioChoice))
            // Точка обязательна: %(ext)s подставляется БЕЗ точки.
            .addOption("-o", File(videosDir, "${video.youtubeId}.%(ext)s").absolutePath)
        if (task.withComments) {
            // Этап 8: комментарии в info.json. Лимита нет (решение пользователя,
            // 2026-10-09): у видео с десятками тысяч комментариев извлечение займёт
            // несколько минут (страницы continuations до скачивания файла) и json
            // может весить десятки МБ — это принято как цена за «все комментарии».
            request.addOption("--write-comments")
            request.addOption("--write-info-json")
        }
        if (task.audioChoice == AudioChoice.BOTH) {
            // Этап 9: без флага yt-dlp откажется класть две аудиодорожки в один файл.
            request.addOption("--audio-multistreams")
        }
        val response = executeTracked(request, "video-${task.id}") { progress, etaSec, line ->
            holder.onLine(progress, etaSec, line)
        }
        debugLog.dbg("VIDEO_EXIT code=${response.exitCode} stderr_tail=${stderrTail(response)}")
        if (response.exitCode != 0) {
            throw YtDlpException("Скачивание: ${lastStderrLine(response)}")
        }
        val ext = containerFor(task.audioChoice)
        if (!File(videosDir, "${video.youtubeId}.$ext").exists()) {
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
            debugLog.dumpDebug(processId, "exitCode=${response.exitCode}", response.out + "\n===== ERR =====\n" + response.err)
            response
        } catch (e: Exception) {
            // TODO(Этап 12): УДАЛИТЬ — при ненулевом exit code библиотека бросает
            // YoutubeDLException, у которого message = полный stderr; сохраняем его.
            debugLog.dumpDebug(processId, "EXCEPTION ${e::class.java.name}", e.message.orEmpty())
            throw e
        } finally {
            this.processId = null
        }
    }

    private suspend fun finishCanceled(
        task: DownloadTaskEntity,
        video: VideoEntity,
        thumbsDir: File,
        videosDir: File,
    ): DownloadStatus {
        debugLog.dbg("PARTIAL_FILES " + debugLog.partialListing(video.youtubeId, thumbsDir, videosDir))
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
        debugLog.dbg("PARTIAL_FILES " + debugLog.partialListing(video.youtubeId, thumbsDir, videosDir))
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

}

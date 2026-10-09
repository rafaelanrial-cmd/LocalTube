package com.localtube.data.downloader

import android.content.Context
import com.localtube.DebugFlags
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Ошибка движка скачивания (инициализация или запрос метаданных). */
class YtDlpException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Обёртка над youtubedl-android (yt-dlp): инициализация при старте приложения
 * и получение метаданных видео (`-J --no-playlist`) — Этап 6.
 * Само скачивание появится в Этапе 7.
 */
class YtDlpManager(appContext: Context) {

    private val context = appContext.applicationContext
    /** Фоновая область для одноразовой инициализации движка. */
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initDeferred: CompletableDeferred<Unit>? = null

    /**
     * Инициализирует yt-dlp и ffmpeg (распаковка python/yt-dlp на диск,
     * может занять несколько секунд). Вызывается один раз при старте приложения;
     * повторные вызовы игнорируются.
     */
    fun startInit() {
        if (initDeferred != null) return
        val deferred = CompletableDeferred<Unit>()
        initDeferred = deferred
        initScope.launch {
            try {
                // Этап 8: AAR бандлит yt-dlp 2025.11.12 (нет ответов на комментарии) —
                // до инициализации подставляем более новый из assets, если нужно.
                YtDlpAssets.ensureUpToDate(context)
                FFmpeg.init(context)
                YoutubeDL.init(context)
                deferred.complete(Unit)
            } catch (t: Throwable) {
                deferred.completeExceptionally(t)
            }
        }
    }

    /**
     * Публичная обёртка над [ensureInit] (Этап 7): VideoDownloader вызывает
     * её перед запуском yt-dlp — скачивание стартует только после инициализации.
     */
    suspend fun ensureInitialized() = ensureInit()

    /** Ждёт результат [startInit]; при неудаче бросает [YtDlpException]. */
    private suspend fun ensureInit() {
        val deferred =
            initDeferred ?: throw YtDlpException("Движок скачивания ещё не инициализирован")
        try {
            deferred.await()
        } catch (t: Throwable) {
            throw YtDlpException("Не удалось инициализировать движок скачивания", t)
        }
    }

    /**
     * Получает метаданные видео по ссылке: yt-dlp `-J --no-playlist`, парсинг JSON.
     * @throws YtDlpException при ошибке сети, парсинга или инициализации.
     */
    suspend fun fetchInfo(url: String): VideoInfoData = withContext(Dispatchers.IO) {
        ensureInit()
        val request = YoutubeDLRequest(url)
            .addOption("--no-playlist")
            .addOption("-J")
        // TODO(Этап 12): УДАЛИТЬ — автотест Этапа 9: клиент из файла-флага
        // (подбор рабочего player_client, когда дефолтный блокируется).
        DebugFlags.readPlayerClient(context)?.let { client ->
            request.addOption("--extractor-args", "youtube:player_client=$client")
        }
        try {
            val response = YoutubeDL.execute(request, null, null)
            // TODO(Этап 12): УДАЛИТЬ — дамп сырого -J для разбора formats (Этап 9).
            // Гейт по сессии: флаг-файл add_url одноразовый, удаляется при запуске.
            if (DebugFlags.sessionActive) {
                File(context.getExternalFilesDir(null) ?: context.filesDir, "debug_fetch.json")
                    .writeText(response.out)
            }
            if (response.exitCode != 0) {
                // yt-dlp пишет причину в stderr (например, «Video unavailable»).
                val reason = response.err.trim().lineSequence()
                    .lastOrNull { it.isNotBlank() }?.take(300)
                    ?: "код ${response.exitCode}"
                throw YtDlpException("yt-dlp: $reason")
            }
            parseYtDlpJson(response.out)
        } catch (e: YtDlpException) {
            throw e
        } catch (e: Exception) {
            throw YtDlpException("Не удалось получить информацию о видео", e)
        }
    }
}

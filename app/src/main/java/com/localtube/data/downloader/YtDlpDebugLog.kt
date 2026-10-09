package com.localtube.data.downloader

import android.content.Context
import android.util.Log
import com.localtube.DebugFlags
import java.io.File

/**
 * TODO(Этап 12): УДАЛИТЬ — отладочные выводы автотестов (logcat + дамп-файлы).
 * Вынесено из VideoDownloader, чтобы файл не превышал 300 строк.
 */
class YtDlpDebugLog(private val context: Context) {

    /** Маркеры в logcat под тегом LocalTubeDebug (только в сессии автотеста). */
    fun dbg(message: String) {
        // Гейт по сессии: файлы флагов add_url одноразовые, удаляются при запуске.
        if (DebugFlags.sessionActive) {
            Log.d(DebugFlags.TAG, message)
        }
    }

    /** Дамп out/err в файл на телефоне (только если включён флаг автотеста). */
    fun dumpDebug(processId: String, header: String, body: String) {
        if (!DebugFlags.sessionActive) return
        val dump = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            "debug_ytdlp_$processId.txt",
        )
        dump.writeText("$header\n===== BODY =====\n$body")
    }

    /** Список файлов задачи до частичного удаления — видно, что реально создал yt-dlp. */
    fun partialListing(youtubeId: String, vararg dirs: File): String =
        dirs.joinToString("; ") { dir ->
            dir.listFiles { f -> f.name.startsWith(youtubeId) }?.joinToString(",") {
                "${it.name}(${it.length()})"
            }.orEmpty()
        }
}

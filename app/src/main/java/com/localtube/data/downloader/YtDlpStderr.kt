package com.localtube.data.downloader

import com.yausername.youtubedl_android.YoutubeDLResponse

// Вынесено из VideoDownloader, чтобы файл не превышал 300 строк.

/** Текст для FAILED: последние строки stderr — там и есть причина ошибки. */
internal fun extractErrorMessage(error: Exception): String {
    val raw = error.message ?: "Неизвестная ошибка"
    val lines = raw.trim().lineSequence().filter { it.isNotBlank() }.toList()
    return if (lines.size <= 2) raw.take(300)
    else lines.takeLast(3).joinToString(" | ").take(400)
}

/** Последняя непустая строка stderr — yt-dlp пишет туда причину ошибки. */
internal fun lastStderrLine(response: YoutubeDLResponse): String =
    response.err.trim().lineSequence().lastOrNull { it.isNotBlank() }?.take(300)
        ?: "код ${response.exitCode}"

/** Последние 3 непустые строки stderr — yt-dlp пишет туда и предупреждения. */
internal fun stderrTail(response: YoutubeDLResponse): String =
    response.err.trim().lineSequence()
        .filter { it.isNotBlank() }
        .toList()
        .takeLast(3)
        .joinToString(" | ")
        .take(400)

package com.localtube.data.downloader

import com.yausername.youtubedl_android.YoutubeDLRequest

// Вынесено из VideoDownloader, чтобы файл не превышал 300 строк.

/** Этап 9: селектор формата в зависимости от выбора аудиодорожки. */
internal fun videoFormat(qualityHeight: Int, audioChoice: AudioChoice): String {
    val videoPart = "bv*[height<=$qualityHeight]"
    return when (audioChoice) {
        AudioChoice.ORIGINAL -> "$videoPart+ba/b"
        // Если селектор не сработает — задача FAILED; тогда рабочий селектор
        // подбирается по formats из -J и записывается в PROGRESS.md.
        AudioChoice.RU -> "$videoPart+ba[language=ru]"
        AudioChoice.BOTH -> "$videoPart+ba+ba[language=ru]"
    }
}

/** Этап 9: контейнер на выходе: BOTH → mkv (две аудиодорожки), иначе mp4. */
internal fun containerFor(choice: AudioChoice): String =
    if (choice == AudioChoice.BOTH) "mkv" else "mp4"

/**
 * Этап 9: языковые теги аудиодорожек. Без --embed-metadata ffmpeg НЕ пишет
 * тег `language` на потоки — в плеере обе дорожки были бы «Дорожка 1 / Дорожка 2».
 * Только для неоригинальных выборов: путь ORIGINAL (уже рабочий) не трогаем.
 * --no-embed-info-json: иначе при BOTH+комментариях yt-dlp встроил бы в mkv
 * огромный info.json (десятки МБ; проверено по yt_dlp/__init__.py:607 —
 * --embed-metadata сам включает embed_infojson='if_exists').
 */
internal fun YoutubeDLRequest.addAudioLanguageTags(choice: AudioChoice) {
    if (choice == AudioChoice.ORIGINAL) return
    addOption("--embed-metadata")
    addOption("--no-embed-info-json")
}

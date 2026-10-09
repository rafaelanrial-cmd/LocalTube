package com.localtube.data.downloader

import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse

// Вынесено из VideoDownloader, чтобы файл не превышал 300 строк.

/**
 * Цепочка клиентов по умолчанию (2026-10-09, проверено на телефоне через ByeDPI):
 * visionos ПЕРВЫМ — в SABR-only-сессиях android отдаёт форматы без URL
 * (yt-dlp #12482), mweb требует GVS PO Token, а tv_embedded в бандловом yt-dlp
 * 2026.08.19 отсутствует («Skipping unsupported client»): остаётся только itag 18
 * (360p) и RU/BOTH падают с «Requested format is not available». visionos отдаёт
 * ВСЕ форматы с URL, включая русский дубляж (E2E: cvMJ5oKWwRE → 398+251-5, mp4,
 * code=0). android и mweb — резерв на сети/IP, где visionos блокируется; клиент
 * с 403 не фатален: yt-dlp сливает форматы от остальных.
 */
internal const val DEFAULT_PLAYER_CLIENTS = "visionos,android,mweb"

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

/**
 * Этап 9 (фикс): человекочитаемое сообщение для известного режима отказа —
 * SABR-only-сессия: все дорожки нужного языка без URL → «Requested format is
 * not available». Без расшифровки пользователь видит только английский текст.
 */
internal fun downloadErrorMessage(response: YoutubeDLResponse): String {
    val err = response.err
    if ("Requested format is not available" in err && "missing a URL" in err) {
        return ("YouTube скрыл форматы в этой сессии (эксперимент SABR, yt-dlp #12482). " +
            "Повторите попытку позже или перезапустите ByeDPI (смена IP) и скачайте заново.")
    }
    return "Скачивание: ${lastStderrLine(response)}"
}

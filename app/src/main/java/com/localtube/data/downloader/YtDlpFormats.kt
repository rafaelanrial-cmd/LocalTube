package com.localtube.data.downloader

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

package com.localtube.data.downloader

/**
 * Этап 7: последние значения колбэка прогресса yt-dlp (вынесено из
 * VideoDownloader, чтобы файл не превышал 300 строк). Потоком библиотеки
 * пишется, циклом DownloadService читается.
 */
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

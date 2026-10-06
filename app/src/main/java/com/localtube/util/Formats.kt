package com.localtube.util

/** Числовые форматы для экрана «Загрузки» и уведомления (Этап 7). */
object Formats {

    /** Скорость в МБ/с, одна цифра после запятой: «1,2». */
    fun speedMb(bps: Long): String =
        String.format("%.1f", bps / 1_048_576.0).replace('.', ',')

    /** Оставшееся время в минутах и секундах для строки «~ %1$d:%2$02d». */
    fun etaParts(sec: Long): Pair<Int, Int> {
        val s = sec.coerceIn(0L, 3_599L).toInt()
        return (s / 60) to (s % 60)
    }
}

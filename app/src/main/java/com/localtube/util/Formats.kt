package com.localtube.util

import android.content.Context
import com.localtube.R
import kotlin.math.roundToInt

/** Числовые форматы: экран «Загрузки» и уведомление (Этап 7), даты и лайки комментариев (Этап 8). */
object Formats {

    /** Скорость в МБ/с, одна цифра после запятой: «1,2». */
    fun speedMb(bps: Long): String =
        String.format("%.1f", bps / 1_048_576.0).replace('.', ',')

    /** Оставшееся время в минутах и секундах для строки «~ %1$d:%2$02d». */
    fun etaParts(sec: Long): Pair<Int, Int> {
        val s = sec.coerceIn(0L, 3_599L).toInt()
        return (s / 60) to (s % 60)
    }

    /** Этап 8: число лайков как на YouTube: «999», «1,2 тыс.», «10 тыс.», «1,4 млн». */
    fun compactRu(count: Long): String {
        if (count < 1_000L) return count.toString()
        val isThousand = count < 1_000_000L
        val value = if (isThousand) count / 1_000.0 else count / 1_000_000.0
        val num = if (value >= 10.0) {
            value.roundToInt().toString()
        } else {
            "%.1f".format(value).replace('.', ',').trimEnd('0').trimEnd('.')
        }
        return "$num ${if (isThousand) "тыс." else "млн"}"
    }

    /** Этап 8: относительная дата как на YouTube: «только что», «5 минут назад», … */
    fun relativeDate(context: Context, epochSeconds: Long): String {
        val sec = (System.currentTimeMillis() / 1000L - epochSeconds).coerceAtLeast(0L)
        return when {
            sec < 60 -> context.getString(R.string.rel_now)
            sec < 3_600 -> ago(context, (sec / 60).toInt(), R.string.rel_min_one, R.string.rel_min_few, R.string.rel_min_many)
            sec < 86_400 -> ago(context, (sec / 3_600).toInt(), R.string.rel_hour_one, R.string.rel_hour_few, R.string.rel_hour_many)
            sec < 7 * 86_400L -> ago(context, (sec / 86_400).toInt(), R.string.rel_day_one, R.string.rel_day_few, R.string.rel_day_many)
            sec < 30L * 86_400L -> ago(context, (sec / (7 * 86_400)).toInt(), R.string.rel_week_one, R.string.rel_week_few, R.string.rel_week_many)
            sec < 365L * 86_400L -> ago(context, (sec / (30L * 86_400)).toInt(), R.string.rel_month_one, R.string.rel_month_few, R.string.rel_month_many)
            else -> ago(context, (sec / (365L * 86_400)).toInt(), R.string.rel_year_one, R.string.rel_year_few, R.string.rel_year_many)
        }
    }

    private fun ago(context: Context, count: Int, one: Int, few: Int, many: Int): String =
        context.getString(R.string.rel_ago, count, context.getString(pluralWordRes(count, one, few, many)))

    /** Русские формы множественного числа: 1 / 2–4 / 5+ (с исключением 11–14). */
    fun pluralWordRes(count: Int, one: Int, few: Int, many: Int): Int {
        val mod10 = count % 10
        val mod100 = count % 100
        return when {
            mod10 == 1 && mod100 != 11 -> one
            mod10 in 2..4 && mod100 !in 12..14 -> few
            else -> many
        }
    }
}

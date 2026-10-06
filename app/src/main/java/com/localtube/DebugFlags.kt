package com.localtube

import android.content.Context
import java.io.File

/**
 * TODO(Этап 12): УДАЛИТЬ — временный отладочный режим (введён в Этапе 4).
 *
 * MIUI блокирует input-события через adb (SecurityException INJECT_EVENTS),
 * поэтому проверка на телефоне идёт по файлам-флагам в externalFilesDir:
 * - [FLAG_FILE_NAME] — приложение само заходит в плеер и проигрывает
 *   сценарий play → pause → seek (Этап 4);
 * - [DETAILS_FLAG_FILE_NAME] — автопереход в детали и автотест удаления (Этап 5);
 * - [ADD_URL_FLAG_FILE_NAME] — экран «Добавить»: содержимое файла = ссылка,
 *   приложение само подставляет её и жмёт «Проверить» (Этап 6);
 * - [AUTO_DOWNLOAD_FLAG_FILE_NAME] — после успешной проверки сразу жмёт
 *   «Скачать» (автотест Этапа 7);
 * - [CANCEL_AT_FLAG_FILE_NAME] — содержимое = процент: задача в работе
 *   автоматически отменяется, когда прогресс достигает его (автотест Этапа 7);
 * - [PLAYER_YTID_FLAG_FILE_NAME] — содержимое = youtubeId: автопереход в плеер
 *   скачанного видео по id из базы (автотест оффлайн-воспроизведения, Этап 7).
 * Маркеры — в logcat под тегом [TAG]. Флаги: adb shell touch
 * /sdcard/Android/data/com.localtube/files/<имя флага>.
 */
object DebugFlags {

    /** Тег маркеров автотеста в logcat. */
    const val TAG = "LocalTubeDebug"

    /** Имя файла-флага: автопереход в плеер + сценарий play/pause/seek (Этап 4). */
    const val FLAG_FILE_NAME = "debug_goto_player"

    /** Имя файла-флага: автопереход в детали + автотест удаления (Этап 5). */
    const val DETAILS_FLAG_FILE_NAME = "debug_open_details"

    /** Имя файла-флага: автопроверка ссылки на экране «Добавить» (Этап 6). */
    const val ADD_URL_FLAG_FILE_NAME = "debug_add_url"

    /** Имя файла-флага: автоскачивание после проверки (автотест Этапа 7). */
    const val AUTO_DOWNLOAD_FLAG_FILE_NAME = "debug_auto_download"

    /** Имя файла-флага: отмена задачи по проценту; содержимое — число (Этап 7). */
    const val CANCEL_AT_FLAG_FILE_NAME = "debug_cancel_at"

    /** Имя файла-флага: автопереход в плеер по youtubeId; содержимое — id (Этап 7). */
    const val PLAYER_YTID_FLAG_FILE_NAME = "debug_player_ytid"

    fun flagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, FLAG_FILE_NAME)

    fun detailsFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, DETAILS_FLAG_FILE_NAME)

    /** Файл-флаг автопроверки ссылки (Этап 6); содержимое — URL. */
    fun addUrlFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, ADD_URL_FLAG_FILE_NAME)

    /** URL из файла-флага [ADD_URL_FLAG_FILE_NAME]; null, если файла нет или он пуст. */
    fun readAddUrlFlag(context: Context): String? {
        val file = addUrlFlagFile(context)
        if (!file.exists()) return null
        return file.readText().trim().ifEmpty { null }
    }

    /** Файл-флага автоскачивания (Этап 7); достаточно просто создать его. */
    fun autoDownloadFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, AUTO_DOWNLOAD_FLAG_FILE_NAME)

    /** Файл-флага отмены по проценту (Этап 7); содержимое — число. */
    fun cancelAtFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, CANCEL_AT_FLAG_FILE_NAME)

    /** Процент для автотест-отмены; null, если файла нет или не число. */
    fun readCancelAt(context: Context): Int? {
        val file = cancelAtFlagFile(context)
        if (!file.exists()) return null
        return file.readText().trim().toIntOrNull()
    }

    /** Файл-флага автоперехода в плеер по youtubeId (Этап 7). */
    fun playerYtidFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, PLAYER_YTID_FLAG_FILE_NAME)

    /** youtubeId для автоперехода в плеер; null, если файла нет или он пуст. */
    fun readPlayerYtid(context: Context): String? {
        val file = playerYtidFlagFile(context)
        if (!file.exists()) return null
        return file.readText().trim().ifEmpty { null }
    }
}

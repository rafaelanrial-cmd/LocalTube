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
 * - [DETAILS_FLAG_FILE_NAME] — автопереход в детали и автотест удаления (Этап 5).
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

    fun flagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, FLAG_FILE_NAME)

    fun detailsFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, DETAILS_FLAG_FILE_NAME)
}

package com.localtube

import android.content.Context
import java.io.File

/**
 * TODO(Этап 12): УДАЛИТЬ — временный отладочный режим (введён в Этапе 4).
 *
 * MIUI блокирует input-события через adb (SecurityException INJECT_EVENTS),
 * поэтому проверка плеера на телефоне идёт по файлу-флагу в externalFilesDir:
 * приложение само заходит в плеер и проигрывает сценарий play → pause → seek,
 * маркеры — в logcat под тегом [TAG]. Флаг: adb shell touch
 * /sdcard/Android/data/com.localtube/files/debug_goto_player.
 */
object DebugFlags {

    /** Тег маркеров автотеста в logcat. */
    const val TAG = "LocalTubeDebug"

    /** Имя файла-флага: если он в externalFilesDir — включается автотест. */
    const val FLAG_FILE_NAME = "debug_goto_player"

    fun flagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, FLAG_FILE_NAME)
}

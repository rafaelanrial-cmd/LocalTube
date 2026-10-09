package com.localtube

import android.content.Context
import com.localtube.data.downloader.AudioChoice
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
 *   скачанного видео по id из базы (автотест оффлайн-воспроизведения, Этап 7);
 * - [DETAILS_YTID_FLAG_FILE_NAME] — содержимое = youtubeId: автопереход в детали
 *   скачанного видео по id из базы (автотест комментариев офлайн, Этап 8);
 * - [AUDIO_CHOICE_FLAG_FILE_NAME] — содержимое = ORIGINAL|RU|BOTH: выбор аудио
 *   для автоскачивания (автотест Этапа 9);
 * - [AUDIO_SWITCH_FLAG_FILE_NAME] — в плеере: лог AUDIO-дорожек + переключение
 *   на RU и обратно без перезапуска (автотест Этапа 9);
 * - [NO_COMMENTS_FLAG_FILE_NAME] — автоскачивание идёт без комментариев
 *   (автотест Этапа 9 изолирован от скачивания комментариев);
 * - [FULLSCREEN_FLAG_FILE_NAME] — в плеере сам включает полноэкранный режим
 *   (автотест 2026-10-08); одноразовый — удаляется после срабатывания;
 * - [KEEP_JSON_FLAG_FILE_NAME] — НЕ удалять .info.json после разбора комментариев
 *   (диагностика «нет ответов на комментарии», 2026-10-09).
 * Маркеры — в logcat под тегом [TAG]. Флаги: adb shell touch
 * /sdcard/Android/data/com.localtube/files/<имя флага>.
 *
 * Флаги автоперехода при запуске (details_ytid, player_ytid, add_url) —
 * ОДНОРАЗОВЫЕ: приложение удаляет файл сразу после чтения, поэтому забытый
 * флаг не перехватывает следующий запуск (исправление бага 2026-10-08:
 * приложение само улетало в «Добавить» и грузило ссылку). Маркеры при этом
 * работают через [sessionActive], а не по существованию файла.
 */
object DebugFlags {

    /** Тег маркеров автотеста в logcat. */
    const val TAG = "LocalTubeDebug"

    /**
     * True, пока в этом процессе работает отладочный автосценарий: MainActivity
     * ставит при потреблении launch-флага (сам файл при этом удаляется).
     * По нему гейтятся лог-маркеры и дамп-файлы — они должны жить всё время
     * сценария, хотя флаг-файл уже удалён.
     */
    @Volatile
    var sessionActive = false

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

    /** Имя файла-флага: автопереход в детали по youtubeId; содержимое — id (Этап 8). */
    const val DETAILS_YTID_FLAG_FILE_NAME = "debug_details_ytid"

    /** Файл-флаг автоперехода в детали по youtubeId (Этап 8). */
    fun detailsYtidFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, DETAILS_YTID_FLAG_FILE_NAME)

    /** youtubeId для автоперехода в детали; null, если файла нет или он пуст. */
    fun readDetailsYtid(context: Context): String? {
        val file = detailsYtidFlagFile(context)
        if (!file.exists()) return null
        return file.readText().trim().ifEmpty { null }
    }

    /** Имя файла-флага: выбор аудио для автоскачивания; ORIGINAL|RU|BOTH (Этап 9). */
    const val AUDIO_CHOICE_FLAG_FILE_NAME = "debug_audio_choice"

    /** Файл-флаг выбора аудио (Этап 9). */
    fun audioChoiceFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, AUDIO_CHOICE_FLAG_FILE_NAME)

    /** Выбор аудио из файла-флага; null, если файла нет или значение не распознано. */
    fun readAudioChoice(context: Context): AudioChoice? {
        val file = audioChoiceFlagFile(context)
        if (!file.exists()) return null
        return runCatching { AudioChoice.valueOf(file.readText().trim().uppercase()) }.getOrNull()
    }

    /** Имя файла-флага: автотест переключения аудиодорожек в плеере (Этап 9). */
    const val AUDIO_SWITCH_FLAG_FILE_NAME = "debug_audio_switch"

    /** Файл-флаг автотеста переключения дорожек (Этап 9); достаточно создать. */
    fun audioSwitchFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, AUDIO_SWITCH_FLAG_FILE_NAME)

    /** Имя файла-флага: автоскачивание без комментариев (изоляция автотеста Этапа 9). */
    const val NO_COMMENTS_FLAG_FILE_NAME = "debug_no_comments"

    /** Файл-флага «без комментариев» (автотест Этапа 9); достаточно создать. */
    fun noCommentsFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, NO_COMMENTS_FLAG_FILE_NAME)

    /** Имя файла-флага: клиент плеера YouTube для yt-dlp; содержимое — имя клиента. */
    const val PLAYER_CLIENT_FLAG_FILE_NAME = "debug_player_client"

    /** Файл-флага клиента (автотест Этапа 9: подбор рабочего клиента). */
    fun playerClientFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, PLAYER_CLIENT_FLAG_FILE_NAME)

    /** Клиент из файла-флага; null, если файла нет или он пуст. */
    fun readPlayerClient(context: Context): String? {
        val file = playerClientFlagFile(context)
        if (!file.exists()) return null
        return file.readText().trim().ifEmpty { null }
    }

    /** Имя файла-флага: автотест полноэкранного режима плеера (2026-10-08). */
    const val FULLSCREEN_FLAG_FILE_NAME = "debug_fullscreen"

    /** Файл-флаг автотеста полноэкранного режима; достаточно просто создать. */
    fun fullscreenFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, FULLSCREEN_FLAG_FILE_NAME)

    /** Имя файла-флага: не удалять .info.json после разбора (диагностика, 2026-10-09). */
    const val KEEP_JSON_FLAG_FILE_NAME = "debug_keep_json"

    /** Файл-флага «сохранить info.json» (диагностика ответов); достаточно создать. TODO(Этап 12): УДАЛИТЬ. */
    fun keepJsonFlagFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, KEEP_JSON_FLAG_FILE_NAME)
}

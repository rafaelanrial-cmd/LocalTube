package com.localtube.data.downloader

import android.content.Context
import android.util.Log
import com.localtube.DebugFlags
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Этап 8 (2026-10-09): AAR youtubedl-android бандлит yt-dlp 2025.11.12, который
 * не извлекает ответы на комментарии вообще (в новом формате ответа YouTube токен
 * продолжения лежит в subThreads — его там нет). В assets/ytdlp — проверенная
 * сборка 2026.08.19 (zipapp: __main__.py + yt_dlp/).
 *
 * [ensureUpToDate] вызывается ДО YoutubeDL.init(): если установленного бинарника
 * нет или он старше assets — перезаписываем. Более новую версию (например, через
 * updater в Этапе 10) НЕ откатываем: версия сравнивается как YYYY.MM.DD.
 */
object YtDlpAssets {

    /** Имя файла в assets/ (zipapp). */
    private const val ASSET_NAME = "ytdlp"

    /** Запись с версией внутри zipapp. */
    private const val VERSION_ENTRY = "yt_dlp/version.py"

    /** Путь к бинарнику, который использует YoutubeDL.init() (постоянные из исходников библиотеки). */
    private fun targetFile(context: Context): File =
        File(File(File(context.noBackupFilesDir, YoutubeDL.baseName), YoutubeDL.ytdlpDirName),
            YoutubeDL.ytdlpBin)

    /**
     * Гарантирует, что установленный yt-dlp не старше версии из assets.
     * Никогда не бросает: при любой ошибке остаётся то, что было (init работает
     * с бандловым бинарником AAR).
     */
    fun ensureUpToDate(context: Context) {
        val assetVersion = readAssetVersion(context)
        if (assetVersion == null) {
            log("YTDLP_ASSET_NO_VERSION") // assets биты — не трогаем установленный
            return
        }
        val target = targetFile(context)
        val current = if (target.exists()) readVersionFromFile(target) else null
        if (current != null && current >= assetVersion) {
            log("YTDLP_ASSET_SKIP installed=$current asset=$assetVersion")
            return
        }
        try {
            target.parentFile?.mkdirs()
            context.assets.open(ASSET_NAME).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            log("YTDLP_ASSET_UPDATED from=${current ?: "none"} to=$assetVersion")
        } catch (e: Exception) {
            log("YTDLP_ASSET_FAIL ${e.message}") // фолбэк на бандловый AAR
        }
    }

    /** Версия из assets: копируем zipapp в кэш и читаем yt_dlp/version.py. */
    private fun readAssetVersion(context: Context): String? {
        val temp = File(context.cacheDir, "ytdlp_asset_check.zip")
        return try {
            context.assets.open(ASSET_NAME).use { input ->
                FileOutputStream(temp).use { output -> input.copyTo(output) }
            }
            readVersionFromFile(temp)
        } catch (e: Exception) {
            null
        } finally {
            temp.delete()
        }
    }

    /** Версия из zipapp на диске; null, если файл не читается. */
    private fun readVersionFromFile(file: File): String? {
        // Android'овский ZipEntry не имеет openStream() — читаем через
        // ZipFile.getInputStream(entry) и закрываем явно (без .use).
        var zip: ZipFile? = null
        var stream: InputStream? = null
        return try {
            zip = ZipFile(file)
            val entry: ZipEntry? = zip.getEntry(VERSION_ENTRY) ?: return null
            stream = zip.getInputStream(entry)
            val text: String = stream.bufferedReader().readText()
            // takeBefore/takeAfter в Kotlin stdlib НЕТ — только substringBefore/substringAfter
            // (проверено по официальным jar 1.9.24/2.0.20 и докам: страница take-before → 404).
            for (line in text.lines()) {
                if (!line.startsWith("__version__")) continue
                val version: String = line.substringAfter("'").substringBefore("'")
                if (version.isNotEmpty()) return version
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            try { stream?.close() } catch (_: Exception) { /* ignore */ }
            try { zip?.close() } catch (_: Exception) { /* ignore */ }
        }
    }

    /** Маркеры в logcat — только в сессии автотеста (как в VideoDownloader). */
    private fun log(message: String) {
        if (DebugFlags.sessionActive) Log.d(DebugFlags.TAG, message)
    }
}

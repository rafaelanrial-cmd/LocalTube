package com.localtube.data.downloader

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Этап 8: скачивание аватаров авторов комментариев (дизайн как на YouTube).
 *
 * Блокирующий HTTP-GET (вызывается из IO-корутины DownloadService, до записи
 * в Room). Ошибка одного аватара не фатальна: комментарий сохранится без
 * аватара, UI покажет цветной круг с первой буквой имени.
 */
object CommentAvatars {

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 20_000

    /**
     * Ключ файла аватара: по каналу (ch{channelId}) — дедупликация между
     * комментариями и видео; без channelId — MD5 URL.
     */
    fun keyFor(channelId: String?, thumbnailUrl: String): String {
        val id = channelId?.trim().orEmpty()
        if (id.isNotEmpty()) return "ch$id"
        val md5 = MessageDigest.getInstance("MD5")
            .digest(thumbnailUrl.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "url$md5"
    }

    /**
     * Скачивает [url] в [targetFile], если файла ещё нет (кэш между видео).
     * Запись во временный файл + rename: без полудохлых jpg при обрыве.
     * @return true, если файл доступен ПОСЛЕ вызова (скачан или уже был).
     */
    fun downloadIfAbsent(url: String, targetFile: File, log: (String) -> Unit): Boolean {
        if (targetFile.exists() && targetFile.length() > 0L) return true
        val conn = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            log("AVATAR_FAIL ${targetFile.name} ${e.message}")
            return false
        }
        return try {
            // Android-URLConnection: setRequestProperty (requestProperty — только JVM).
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.instanceFollowRedirects = true
            val code = conn.responseCode
            if (code !in 200..299) {
                log("AVATAR_HTTP_$code ${targetFile.name}")
                return false
            }
            val tmp = File(targetFile.parentFile, targetFile.name + ".tmp")
            conn.inputStream.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            if (tmp.length() <= 0L) {
                tmp.delete()
                return false
            }
            if (!tmp.renameTo(targetFile)) {
                // Редкий сбой rename: файл всё равно читаем, просто не переименован.
                targetFile.delete()
                if (!tmp.renameTo(targetFile)) {
                    tmp.delete()
                    return false
                }
            }
            true
        } catch (e: Exception) {
            log("AVATAR_FAIL ${targetFile.name} ${e.message}")
            false
        } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }
}

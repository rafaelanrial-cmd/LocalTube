package com.localtube.data.downloader

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Метаданные видео из yt-dlp (`-J`) — Этап 6.
 */
data class VideoInfoData(
    /** Идентификатор видео YouTube (v=...) — уникальный ключ библиотеки. */
    val youtubeId: String,
    val title: String,
    val description: String,
    /** Дата загрузки, секунды от epoch; 0 — если неизвестна. */
    val uploadDateEpoch: Long,
    /** Длительность в секундах. */
    val durationSec: Long,
    /** https-ссылка на превью (Coil). */
    val thumbnailUrl: String,
    /** Уникальные языки аудио из formats[].language. */
    val audioLangs: List<String>,
    /** Есть ли русская аудиодорожка. */
    val hasRuTrack: Boolean,
)

/**
 * Часть JSON-ответа yt-dlp (`-J`): только нужные поля.
 * Остальные сотни полей игнорируются (ignoreUnknownKeys).
 */
@Serializable
private data class YtDlpJson(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val upload_date: String? = null,
    val duration: Double? = null,
    val thumbnail: String? = null,
    val formats: List<YtDlpFormat>? = null,
)

@Serializable
private data class YtDlpFormat(
    /** Код языка аудио ("en", "ru"); у чисто видеодорожек обычно отсутствует. */
    val language: String? = null,
)

private val ytDlpJsonParser = Json { ignoreUnknownKeys = true }

/**
 * Парсит JSON yt-dlp в [VideoInfoData].
 * @throws YtDlpException если JSON не распознан или нет id.
 */
internal fun parseYtDlpJson(json: String): VideoInfoData {
    val data = try {
        ytDlpJsonParser.decodeFromString(YtDlpJson.serializer(), json)
    } catch (e: Exception) {
        throw YtDlpException("Не удалось разобрать информацию о видео", e)
    }
    val id = data.id.orEmpty()
    if (id.isEmpty()) throw YtDlpException("В ответе нет идентификатора видео")
    val langs = data.formats.orEmpty().mapNotNull { it.language }.distinct()
    return VideoInfoData(
        youtubeId = id,
        title = data.title.orEmpty(),
        description = data.description.orEmpty(),
        uploadDateEpoch = parseUploadDate(data.upload_date),
        durationSec = (data.duration ?: 0.0).toLong(),
        thumbnailUrl = data.thumbnail.orEmpty(),
        audioLangs = langs,
        hasRuTrack = "ru" in langs,
    )
}

/** Дата yt-dlp «YYYYMMDD» → секунды epoch (UTC); 0 — если неизвестна или не распознана. */
private fun parseUploadDate(uploadDate: String?): Long {
    if (uploadDate.isNullOrBlank()) return 0L
    return try {
        LocalDate.parse(uploadDate, DateTimeFormatter.BASIC_ISO_DATE)
            .atStartOfDay(ZoneOffset.UTC)
            .toEpochSecond()
    } catch (e: Exception) {
        0L
    }
}

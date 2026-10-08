package com.localtube.data.downloader

import com.localtube.data.db.CommentEntity
import com.localtube.data.repo.DownloadRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Этап 8: комментарии из .info.json yt-dlp.
 *
 * Загрузка идёт с --write-comments --write-info-json и лимитом
 * youtube:max_comments=500,50,10 (обязателен — иначе json может весить 50+ МБ).
 * После скачивания [saveCommentsFromInfoJson] разбирает файл, сохраняет
 * комментарии в Room и удаляет json.
 */

/** Комментарий из info.json (имена полей yt-dlp). */
@Serializable
private data class YtDlpComment(
    val id: String? = null,
    /** "root" или YouTube-id родительского комментария. */
    val parent: String? = null,
    val author: String? = null,
    val text: String? = null,
    val like_count: Long? = null,
    val timestamp: Long? = null, // epoch-секунды; может быть null (оценочное время)
)

/** Нужен только раздел комментариев из info.json. */
@Serializable
private data class YtDlpInfoJson(
    /** null — комментарии для видео отключены. */
    val comments: List<YtDlpComment>? = null,
)

/** Разобранный комментарий, готовый к записи в Room. */
internal data class ParsedComment(
    val ytId: String,
    /** YouTube-id родительского комментария; null — топовый. */
    val parentYtId: String?,
    val author: String,
    val text: String,
    val likeCount: Long,
    val publishedAtEpoch: Long,
)

private val infoJsonParser = Json { ignoreUnknownKeys = true }

/** Разбирает раздел комментариев .info.json; пустой список — их нет/отключены. */
internal fun parseCommentsFromInfoJson(jsonText: String): List<ParsedComment> {
    val data = try {
        infoJsonParser.decodeFromString(YtDlpInfoJson.serializer(), jsonText)
    } catch (e: Exception) {
        throw YtDlpException("Не удалось разобрать комментарии из info.json", e)
    }
    return data.comments.orEmpty().mapNotNull { c ->
        val id = c.id ?: return@mapNotNull null
        ParsedComment(
            ytId = id,
            parentYtId = c.parent?.takeIf { it.isNotEmpty() && it != "root" },
            author = c.author.orEmpty(),
            text = c.text.orEmpty(),
            likeCount = (c.like_count ?: 0L).coerceAtLeast(0L),
            publishedAtEpoch = (c.timestamp ?: 0L).coerceAtLeast(0L),
        )
    }
}

/**
 * Сохраняет разобранные комментарии в Room. В списке yt-dlp родительский
 * комментарий всегда идёт раньше своих ответов, поэтому одного прохода
 * по карте id достаточно.
 * @return число сохранённых комментариев.
 */
internal suspend fun saveParsedComments(
    repo: DownloadRepository,
    videoId: Int,
    parsed: List<ParsedComment>,
): Int {
    repo.deleteComments(videoId) // на случай повторной загрузки
    val roomIdByYtId = mutableMapOf<String, Int>()
    for (c in parsed) {
        val parentId = c.parentYtId?.let { roomIdByYtId[it] }
        val id = repo.insertComment(
            CommentEntity(
                videoId = videoId,
                author = c.author,
                text = c.text,
                likeCount = c.likeCount,
                publishedAtEpoch = c.publishedAtEpoch,
                parentId = parentId,
            ),
        )
        roomIdByYtId[c.ytId] = id.toInt()
    }
    return parsed.size
}

/**
 * Этап 8: разбирает .info.json → Room и удаляет файл.
 * Ошибка не фатальна для видео (оно уже скачано): возвращает -1.
 * @return число сохранённых комментариев или -1, если json отсутствует/не читается.
 */
internal suspend fun saveCommentsFromInfoJson(
    repo: DownloadRepository,
    videoId: Int,
    infoJsonFile: File,
    log: (String) -> Unit = {},
): Int {
    if (!infoJsonFile.exists()) {
        log("COMMENTS_JSON_MISSING")
        return -1
    }
    try {
        val parsed = parseCommentsFromInfoJson(infoJsonFile.readText())
        val saved = saveParsedComments(repo, videoId, parsed)
        log("COMMENTS_SAVED count=$saved")
        return saved
    } catch (e: Exception) {
        log("COMMENTS_PARSE_FAIL ${e.message}")
        return -1
    } finally {
        infoJsonFile.delete() // json удаляется после разбора (задача Этапа 8)
    }
}
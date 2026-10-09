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
 * youtube:max_comments (MAX_COMMENTS_ARG в VideoDownloader). После скачивания
 * [saveCommentsFromInfoJson] разбирает файл, скачивает аватары авторов в
 * avatars/ (дедупликация по каналу), сохраняет комментарии в Room и удаляет json.
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
    // Этап 8 (2026-10-08): дизайн как на YouTube.
    val author_thumbnail: String? = null, // URL аватара автора
    val author_id: String? = null,        // id канала — ключ кэша аватаров
    val is_pinned: Boolean? = null,       // закреплён автором/YouTube
    val author_is_uploader: Boolean? = null, // автор комментария = владелец видео
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
    /** URL аватара автора; null/пусто — аватар не будет скачиваться. */
    val authorThumbnailUrl: String?,
    /** id канала автора (ключ кэша аватаров); null — ключ из MD5 URL. */
    val channelId: String?,
    val isPinned: Boolean,
    val authorIsUploader: Boolean,
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
            authorThumbnailUrl = c.author_thumbnail?.takeIf { it.isNotBlank() },
            channelId = c.author_id?.trim()?.takeIf { it.isNotEmpty() },
            isPinned = c.is_pinned == true,
            authorIsUploader = c.author_is_uploader == true,
        )
    }
}

/**
 * Сохраняет разобранные комментарии в Room. В списке yt-dlp родительский
 * комментарий всегда идёт раньше своих ответов, поэтому одного прохода
 * по карте id достаточно. Аватары скачиваются ДО записи (блокирующий вызов —
 * функция вызывается из IO-корутины DownloadService).
 * @return число сохранённых комментариев.
 */
internal suspend fun saveParsedComments(
    repo: DownloadRepository,
    videoId: Int,
    parsed: List<ParsedComment>,
    avatarsDir: File,
    log: (String) -> Unit = {},
): Int {
    repo.deleteComments(videoId) // на случай повторной загрузки

    // Этап 8: уникальные аватары — один файл на канал, кэш между видео.
    val urlByKey = linkedMapOf<String, String>()
    for (c in parsed) {
        val url = c.authorThumbnailUrl ?: continue
        val key = CommentAvatars.keyFor(c.channelId, url)
        if (!urlByKey.containsKey(key)) urlByKey[key] = url
    }
    val pathByKey = mutableMapOf<String, String>()
    var freshAvatars = 0
    for ((key, url) in urlByKey) {
        val file = File(avatarsDir, "$key.jpg")
        val fresh = !file.exists() || file.length() <= 0L
        if (CommentAvatars.downloadIfAbsent(url, file, log) && file.length() > 0L) {
            pathByKey[key] = file.absolutePath
            if (fresh) freshAvatars++
        }
    }
    if (urlByKey.isNotEmpty()) {
        log("AVATARS_SAVED total=${pathByKey.size} fresh=$freshAvatars")
    }

    val roomIdByYtId = mutableMapOf<String, Int>()
    for (c in parsed) {
        val parentId = c.parentYtId?.let { roomIdByYtId[it] }
        val avatarPath = c.authorThumbnailUrl
            ?.let { pathByKey[CommentAvatars.keyFor(c.channelId, it)] }
            .orEmpty()
        val id = repo.insertComment(
            CommentEntity(
                videoId = videoId,
                author = c.author,
                text = c.text,
                likeCount = c.likeCount,
                publishedAtEpoch = c.publishedAtEpoch,
                parentId = parentId,
                authorThumbnailPath = avatarPath,
                isPinned = c.isPinned,
                authorIsUploader = c.authorIsUploader,
            ),
        )
        roomIdByYtId[c.ytId] = id.toInt()
    }
    return parsed.size
}

/**
 * Этап 8: разбирает .info.json → Room (с аватарами в [avatarsDir]) и удаляет файл.
 * Ошибка не фатальна для видео (оно уже скачано): возвращает -1.
 * @param keepJson TODO(Этап 12): УДАЛИТЬ — диагностика «нет ответов»: не удалять json.
 * @return число сохранённых комментариев или -1, если json отсутствует/не читается.
 */
internal suspend fun saveCommentsFromInfoJson(
    repo: DownloadRepository,
    videoId: Int,
    infoJsonFile: File,
    avatarsDir: File,
    keepJson: Boolean = false,
    log: (String) -> Unit = {},
): Int {
    if (!infoJsonFile.exists()) {
        log("COMMENTS_JSON_MISSING")
        return -1
    }
    try {
        val parsed = parseCommentsFromInfoJson(infoJsonFile.readText())
        val saved = saveParsedComments(repo, videoId, parsed, avatarsDir, log)
        log("COMMENTS_SAVED count=$saved")
        return saved
    } catch (e: Exception) {
        log("COMMENTS_PARSE_FAIL ${e.message}")
        return -1
    } finally {
        if (keepJson) {
            log("KEEP_JSON ${infoJsonFile.name}") // TODO(Этап 12): УДАЛИТЬ — диагностика
        } else {
            infoJsonFile.delete() // json удаляется после разбора (задача Этапа 8)
        }
    }
}

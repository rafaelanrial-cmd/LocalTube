package com.localtube.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Комментарий к видео (Этап 8). Удаляется вместе с видео (CASCADE).
 *
 * @param id ключ Room, генерируется автоматически
 * @param videoId видео, к которому относится комментарий
 * @param author автор комментария
 * @param text текст комментария
 * @param likeCount количество «нравится»
 * @param publishedAtEpoch дата публикации, секунды от epoch
 * @param parentId id родительского комментария (ответы), null для верхнего уровня
 * @param authorThumbnailPath путь к файлу аватара автора (каталог avatars); пусто — не скачан
 * @param isPinned закреплённый комментарий (первый в списке, как на YouTube)
 * @param authorIsUploader автор комментария — владелец видео (бейдж «Автор»)
 */
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["videoId"])],
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: Int,
    val author: String,
    val text: String,
    val likeCount: Long = 0L,
    val publishedAtEpoch: Long = 0L,
    val parentId: Int? = null,
    // Этап 8 (2026-10-08): аватары + дизайн как на YouTube.
    val authorThumbnailPath: String = "",
    val isPinned: Boolean = false,
    val authorIsUploader: Boolean = false,
)

package com.localtube.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.localtube.data.downloader.AudioChoice

/**
 * Задача скачивания для [VideoEntity] (Этап 7).
 *
 * Прогресс и статус обновляются только через эту сущность;
 * UI читает Flow из Room (правило MASTER_PLAN).
 * Удаляется вместе с видео (CASCADE).
 */
@Entity(
    tableName = "download_tasks",
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
data class DownloadTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: Int,
    val url: String,
    /** Ограничение высоты для -f "bv*[height<=X]+ba/b" (Этап 7). */
    val qualityHeight: Int = 720,
    /** Выбор аудиодорожки; RU реально подставляется в Этапе 9. */
    val audioChoice: AudioChoice = AudioChoice.ORIGINAL,
    /** Скачивать комментарии (реализуется с Этапа 8). */
    val withComments: Boolean = false,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progressPercent: Int = 0,
    val speedBps: Long = 0L,
    val etaSec: Long = 0L,
    val videoOk: Boolean = false,
    val thumbOk: Boolean = false,
    val commentsOk: Boolean = false,
    /** Прошлые значения колбэка для расчёта скорости по дельте (Этап 7). */
    val lastBytes: Long = 0L,
    val lastTimeMs: Long = 0L,
    val errorMsg: String? = null,
)

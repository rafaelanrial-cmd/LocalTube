package com.localtube.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progressPercent: Int = 0,
    val speedBps: Long = 0L,
    val etaSec: Long = 0L,
    val videoOk: Boolean = false,
    val thumbOk: Boolean = false,
    val commentsOk: Boolean = false,
    val errorMsg: String? = null,
)

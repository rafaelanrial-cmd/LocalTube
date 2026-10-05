package com.localtube.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Скачанное видео — запись библиотеки (Этап 3).
 *
 * @param id ключ Room, генерируется автоматически
 * @param youtubeId идентификатор видео на YouTube (v=...), уникален
 * @param title название видео
 * @param description описание видео
 * @param uploadDateEpoch дата загрузки, секунды от epoch
 * @param durationSec длительность в секундах
 * @param thumbnailPath путь к файлу превью на диске
 * @param videoFilePath путь к файлу видео на диске
 * @param sizeBytes суммарный размер скачанного (видео+аудио), байты
 * @param audioLangs коды доступных аудиодорожек через запятую (например "en,ru")
 * @param hasRuTrack есть ли русская аудиодорожка (Этап 9)
 */
@Entity(
    tableName = "videos",
    indices = [Index(value = ["youtubeId"], unique = true)],
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val youtubeId: String,
    val title: String,
    val description: String = "",
    val uploadDateEpoch: Long = 0L,
    val durationSec: Long = 0L,
    val thumbnailPath: String = "",
    val videoFilePath: String = "",
    val sizeBytes: Long = 0L,
    val audioLangs: String = "",
    val hasRuTrack: Boolean = false,
)

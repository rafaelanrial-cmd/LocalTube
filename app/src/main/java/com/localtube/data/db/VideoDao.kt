package com.localtube.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    /** Все видео, сначала новые по дате загрузки. */
    @Query("SELECT * FROM videos ORDER BY uploadDateEpoch DESC")
    fun getAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: Int): VideoEntity?

    /** Возвращает ключ вставленной строки. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity): Long

    /** Удаляет видео; задачи и комментарии уходят CASCADE. */
    @Delete
    suspend fun delete(video: VideoEntity)
}

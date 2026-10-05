package com.localtube.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    /** Все комментарии видео, сначала с большим числом «нравится». */
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY likeCount DESC")
    fun getByVideoId(videoId: Int): Flow<List<CommentEntity>>

    /** Возвращает ключ вставленной строки. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity): Long

    /** Удаляет все комментарии видео (например, при повторном скачивании). */
    @Query("DELETE FROM comments WHERE videoId = :videoId")
    suspend fun deleteByVideoId(videoId: Int)
}

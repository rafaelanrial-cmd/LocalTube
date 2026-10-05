package com.localtube.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {

    /** Все задачи, сначала новые. */
    @Query("SELECT * FROM download_tasks ORDER BY id DESC")
    fun getAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getById(id: Int): DownloadTaskEntity?

    /** Создаёт задачу (Этап 7); возвращает ключ строки. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DownloadTaskEntity): Long

    /** Обновление прогресса/статуса — замена всей строки через copy(). */
    @Update
    suspend fun update(task: DownloadTaskEntity)
}

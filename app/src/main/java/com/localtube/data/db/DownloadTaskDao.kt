package com.localtube.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.RewriteQueriesToDropUnusedColumns
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

    /** Задачи с названием видео (JOIN), сначала новые — для экрана «Загрузки». */
    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT t.*, v.title AS title
        FROM download_tasks t JOIN videos v ON v.id = t.videoId
        ORDER BY t.id DESC
        """,
    )
    fun getRows(): Flow<List<DownloadTaskRow>>

    /** Первая задача в очереди (FIFO по id) — её берёт DownloadService. */
    @Query("SELECT * FROM download_tasks WHERE status = 'QUEUED' ORDER BY id ASC LIMIT 1")
    suspend fun getNextQueued(): DownloadTaskEntity?

    /** Сброс «осиротевших» RUNNING в QUEUED после перезапуска приложения (Этап 7). */
    @Query(
        """
        UPDATE download_tasks
        SET status = 'QUEUED', progressPercent = 0, speedBps = 0, etaSec = 0,
            lastBytes = 0, lastTimeMs = 0
        WHERE status = 'RUNNING'
        """,
    )
    suspend fun resetRunningToQueued()

    /** Есть ли задачи в очереди? */
    @Query("SELECT COUNT(*) > 0 FROM download_tasks WHERE status = 'QUEUED'")
    suspend fun hasQueued(): Boolean
}

/** Задача + название видео для экрана «Загрузки» (Этап 7). */
data class DownloadTaskRow(
    val id: Int,
    val videoId: Int,
    val url: String,
    val qualityHeight: Int,
    val withComments: Boolean,
    val status: DownloadStatus,
    val progressPercent: Int,
    val speedBps: Long,
    val etaSec: Long,
    val videoOk: Boolean,
    val thumbOk: Boolean,
    val commentsOk: Boolean,
    val lastBytes: Long,
    val lastTimeMs: Long,
    val errorMsg: String?,
    val title: String,
)

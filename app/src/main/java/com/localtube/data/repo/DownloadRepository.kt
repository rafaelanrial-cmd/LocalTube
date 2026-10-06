package com.localtube.data.repo

import com.localtube.data.db.CommentDao
import com.localtube.data.db.CommentEntity
import com.localtube.data.db.DownloadTaskDao
import com.localtube.data.db.DownloadTaskEntity
import com.localtube.data.db.DownloadTaskRow
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий скачиваний: задачи (Этап 7) и комментарии видео (Этап 8).
 *
 * Прогресс и статус обновляются только через [DownloadTaskEntity];
 * UI читает Flow из Room (правило MASTER_PLAN).
 */
class DownloadRepository(
    private val taskDao: DownloadTaskDao,
    private val commentDao: CommentDao,
) {

    // --- Задачи скачивания ---

    /** Все задачи, сначала новые. */
    fun observeTasks(): Flow<List<DownloadTaskEntity>> = taskDao.getAll()

    suspend fun getTask(id: Int): DownloadTaskEntity? = taskDao.getById(id)

    /** Создаёт задачу; возвращает ключ строки. */
    suspend fun insertTask(task: DownloadTaskEntity): Long = taskDao.insert(task)

    /** Обновление прогресса/статуса (замена всей строки). */
    suspend fun updateTask(task: DownloadTaskEntity) = taskDao.update(task)

    /** Задачи с названием видео для экрана «Загрузки» (Этап 7). */
    fun observeTaskRows(): Flow<List<DownloadTaskRow>> = taskDao.getRows()

    /** Первая задача в очереди — её берёт DownloadService. */
    suspend fun getNextQueued(): DownloadTaskEntity? = taskDao.getNextQueued()

    /** Сброс «осиротевших» RUNNING в QUEUED после перезапуска приложения. */
    suspend fun resetRunningToQueued() = taskDao.resetRunningToQueued()

    /** Есть ли задачи в очереди? */
    suspend fun hasQueued(): Boolean = taskDao.hasQueued()

    // --- Комментарии ---

    /** Все комментарии видео, сначала с большим числом «нравится». */
    fun observeComments(videoId: Int): Flow<List<CommentEntity>> = commentDao.getByVideoId(videoId)

    /** Возвращает ключ вставленной строки. */
    suspend fun insertComment(comment: CommentEntity): Long = commentDao.insert(comment)

    /** Удаляет все комментарии видео. */
    suspend fun deleteComments(videoId: Int) = commentDao.deleteByVideoId(videoId)
}

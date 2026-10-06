package com.localtube.data.repo

import com.localtube.data.db.VideoDao
import com.localtube.data.db.VideoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий видео: единственный вход в библиотеку (Этапы 3, 5).
 * Обычный класс без DI-фреймворка; экземпляр хранится в AppContainer.
 */
class VideoRepository(private val dao: VideoDao) {

    /** Все видео, сначала новые по дате загрузки. UI собирает этот Flow. */
    fun observeAll(): Flow<List<VideoEntity>> = dao.getAll()

    suspend fun getById(id: Int): VideoEntity? = dao.getById(id)

    /** По youtubeId; null, если нет. */
    suspend fun getByYoutubeId(youtubeId: String): VideoEntity? = dao.getByYoutubeId(youtubeId)

    /** Возвращает ключ вставленной строки. */
    suspend fun insert(video: VideoEntity): Long = dao.insert(video)

    /** Этап 7: записать пути файлов и размер после скачивания. */
    suspend fun update(video: VideoEntity) = dao.update(video)

    /** Удаляет видео; задачи скачивания и комментарии уходят CASCADE. */
    suspend fun delete(video: VideoEntity) = dao.delete(video)
}

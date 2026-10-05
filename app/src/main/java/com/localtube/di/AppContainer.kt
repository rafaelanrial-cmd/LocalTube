package com.localtube.di

import android.content.Context
import androidx.room.Room
import com.localtube.data.db.AppDatabase
import com.localtube.data.repo.DownloadRepository
import com.localtube.data.repo.VideoRepository

/**
 * Ручной DI-контейнер приложения (без Hilt).
 * Здесь создаются и хранятся:
 *  - Room-база (AppDatabase) и репозитории (data/repo) — Этап 2;
 *  - скачиватель (data/downloader) — Этап 7;
 *  - DataStore-настройки — Этап 10.
 */
class AppContainer(appContext: Context) {

    val context: Context = appContext.applicationContext

    // Этап 2: Room-база и репозитории.
    val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    val videoRepository: VideoRepository by lazy {
        VideoRepository(database.videoDao())
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(database.downloadTaskDao(), database.commentDao())
    }

    // TODO(Этап 7): lateinit var downloader: YtdlpDownloader
}

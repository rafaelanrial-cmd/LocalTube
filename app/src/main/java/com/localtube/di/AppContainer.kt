package com.localtube.di

import android.content.Context
import androidx.room.Room
import com.localtube.data.db.AppDatabase
import com.localtube.data.downloader.YtDlpManager
import com.localtube.data.repo.DownloadRepository
import com.localtube.data.repo.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Ручной DI-контейнер приложения (без Hilt).
 * Здесь создаются и хранятся:
 *  - Room-база (AppDatabase) и репозитории (data/repo) — Этап 2;
 *  - движок yt-dlp (data/downloader) — Этап 6, скачивание — Этап 7;
 *  - DataStore-настройки — Этап 10.
 */
class AppContainer(appContext: Context) {

    val context: Context = appContext.applicationContext

    // Этап 7: фоновая область для одноразовой работы при старте приложения.
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    // Этап 6: движок yt-dlp (инициализация стартует в LocalTubeApp.onCreate).
    val ytDlpManager: YtDlpManager by lazy { YtDlpManager(context) }

    // Этап 7: исполнение задач скачивания — в service.DownloadService
    // (запускается из AddViewModel и при старте приложения, см. LocalTubeApp).
}

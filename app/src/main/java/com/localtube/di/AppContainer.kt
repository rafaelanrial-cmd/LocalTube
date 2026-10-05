package com.localtube.di

import android.content.Context

/**
 * Ручной DI-контейнер приложения (без Hilt).
 * Здесь будут создаваться и храниться:
 *  - Этап 2: Room-база (AppDatabase) и репозитории (data/repo);
 *  - Этап 7: скачиватель (data/downloader);
 *  - Этап 10: DataStore-настройки.
 */
class AppContainer(appContext: Context) {

    val context: Context = appContext.applicationContext

    // TODO(Этап 2): lateinit var database: AppDatabase
    // TODO(Этап 7): lateinit var downloader: YtdlpDownloader
}

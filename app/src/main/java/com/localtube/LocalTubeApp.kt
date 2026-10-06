package com.localtube

import android.app.Application
import com.localtube.di.AppContainer
import com.localtube.service.DownloadService
import kotlinx.coroutines.launch

/** Application-класс LocalTube: создаёт ручной DI-контейнер (без Hilt). */
class LocalTubeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        // Этап 6: инициализация yt-dlp/ffmpeg в фоне (распаковка занимает секунды).
        container.ytDlpManager.startInit()
        // Этап 7: после перезапуска приложения «осиротевшие» RUNNING-задачи
        // возвращаем в очередь (процессы умерли вместе с сервисом) и,
        // если очередь не пуста, заново запускаем сервис скачиваний.
        container.appScope.launch {
            container.downloadRepository.resetRunningToQueued()
            if (container.downloadRepository.hasQueued()) {
                DownloadService.start(container.context)
            }
        }
    }

    companion object {
        lateinit var instance: LocalTubeApp
            private set
    }
}

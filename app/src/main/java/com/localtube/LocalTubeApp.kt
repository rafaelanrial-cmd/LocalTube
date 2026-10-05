package com.localtube

import android.app.Application
import com.localtube.di.AppContainer

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
    }

    companion object {
        lateinit var instance: LocalTubeApp
            private set
    }
}

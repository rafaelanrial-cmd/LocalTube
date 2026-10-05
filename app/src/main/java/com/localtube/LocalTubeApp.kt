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
    }

    companion object {
        lateinit var instance: LocalTubeApp
            private set
    }
}

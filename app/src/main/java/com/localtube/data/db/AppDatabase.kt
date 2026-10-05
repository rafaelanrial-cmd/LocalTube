package com.localtube.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room-база LocalTube, версия 1 (Этап 2).
 *
 * Политика миграций — fallbackToDestructiveMigration (настраивается в AppContainer):
 * при несовместимом изменении схемы база пересоздаётся.
 */
@Database(
    entities = [
        VideoEntity::class,
        DownloadTaskEntity::class,
        CommentEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao

    abstract fun downloadTaskDao(): DownloadTaskDao

    abstract fun commentDao(): CommentDao

    companion object {
        const val NAME = "localtube.db"
    }
}

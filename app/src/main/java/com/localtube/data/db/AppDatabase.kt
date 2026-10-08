package com.localtube.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room-база LocalTube, версия 3 (баг-фикс 2026-10-08: память позиции videos.positionMs).
 *
 * Миграция 2→3 — явно в AppContainer (MIGRATION_2_3), чтобы не стирать базу;
 * fallbackToDestructiveMigration остаётся страховкой для более старых версий.
 */
@Database(
    entities = [
        VideoEntity::class,
        DownloadTaskEntity::class,
        CommentEntity::class,
    ],
    version = 3,
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

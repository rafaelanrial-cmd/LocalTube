package com.localtube.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localtube.LocalTubeApp
import com.localtube.data.db.VideoEntity
import com.localtube.data.repo.VideoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * ViewModel экрана «Библиотека» (Этап 3).
 * Данные: Flow из VideoDao.getAll() через VideoRepository.
 */
class LibraryViewModel(private val repository: VideoRepository) : ViewModel() {

    /** Все скачанные видео, сначала новые по дате загрузки. */
    val videos: StateFlow<List<VideoEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // TODO(Этап 7): УДАЛИТЬ — временное тестовое видео для проверки экрана.
    fun addTestVideo() {
        viewModelScope.launch {
            val context = LocalTubeApp.instance.container.context
            repository.insert(
                VideoEntity(
                    youtubeId = TEST_YOUTUBE_ID,
                    title = "Тестовое видео",
                    description = "Временная запись для проверки экрана «Библиотека» (Этап 3).",
                    uploadDateEpoch = System.currentTimeMillis() / 1000L,
                    durationSec = 254L, // 4:14
                    thumbnailPath = createTestThumbnail(context),
                    videoFilePath = "",
                    sizeBytes = 12_345_678L,
                    audioLangs = "en,ru",
                    hasRuTrack = true,
                ),
            )
        }
    }

    /** Генерирует превью PNG для тестового видео в thumbs/ (если ещё нет). */
    private fun createTestThumbnail(context: Context): String {
        val root = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(root, "thumbs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "test_video.png")
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.parseColor("#1565C0"))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 42f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Тестовое видео", 320f, 205f, paint)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        return file.absolutePath
    }

    companion object {
        /**
         * Фиксированный id тестового видео: повторный клик заменяет запись
         * (OnConflictStrategy.REPLACE по youtubeId), дублей не появляется.
         */
        private const val TEST_YOUTUBE_ID = "localtube-test-video"

        /** Фабрика: репозиторий берём из AppContainer (ручной DI, без Hilt). */
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = LocalTubeApp.instance
                return LibraryViewModel(app.container.videoRepository) as T
            }
        }
    }
}

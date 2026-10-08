package com.localtube.ui.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import com.localtube.DebugFlags
import com.localtube.data.db.VideoEntity
import kotlinx.coroutines.delay

/**
 * Память позиции воспроизведения (баг-фикс 2026-10-08): плеер запоминает,
 * на каком месте остановилось видео, и при следующем запуске продолжает
 * с того же места. Позиция хранится в Room (videos.positionMs) и пишется:
 * - каждые [SAVE_INTERVAL_MS], пока открыт экран плеера;
 * - когда activity уходит в фон (ON_STOP);
 * - при выходе из экрана плеера (сохранение делает ViewModel.onCleared).
 */
object PlaybackPosition {

    /** Как часто сохранять позицию, пока открыт экран. */
    const val SAVE_INTERVAL_MS = 5_000L

    /** Остановились ближе к концу, чем на [NEAR_END_MS] — начинаем сначала. */
    private const val NEAR_END_MS = 10_000L

    /**
     * Точка старта: сохранённое место; 0, если позиций ещё не было или видео
     * досмотрели почти до конца (старт «с начала» — ожидаемое поведение).
     */
    fun startPositionFor(video: VideoEntity?): Long {
        val saved = video?.positionMs ?: 0L
        if (saved <= 0L) return 0L
        val durationMs = (video?.durationSec ?: 0L) * 1_000L
        return if (durationMs > 0L && saved >= durationMs - NEAR_END_MS) 0L else saved
    }
}

/**
 * Сохранение позиции: каждые [PlaybackPosition.SAVE_INTERVAL_MS] и на ON_STOP
 * (уход в фон). Сохранение при выходе из экрана делает ViewModel.onCleared.
 */
@Composable
fun PlaybackPositionMemory(
    player: ExoPlayer?,
    viewModel: PlayerViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            delay(PlaybackPosition.SAVE_INTERVAL_MS)
            viewModel.savePosition(p.currentPosition)
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val p = player
        if (p == null) {
            onDispose {}
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    viewModel.savePosition(p.currentPosition)
                    // TODO(Этап 12): УДАЛИТЬ — маркер для проверки памяти позиции.
                    if (DebugFlags.sessionActive) {
                        Log.d(DebugFlags.TAG, "POSITION_SAVED pos=${p.currentPosition}ms on=onStop")
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }
}

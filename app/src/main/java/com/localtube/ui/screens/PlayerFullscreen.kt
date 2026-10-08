package com.localtube.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.localtube.DebugFlags
import com.localtube.R

/**
 * Полноэкранный режим (2026-10-08): скрывает/показывает системные панели
 * (статус-бар + навигация) на экране плеера. Приложение уже edge-to-edge
 * (enableEdgeToEdge в MainActivity), поэтому достаточно переключить видимость
 * панелей; после свайпа они на мгновение появляются и снова прячутся
 * (immersive sticky). При повороте экрана Activity пересоздаётся, но эффект
 * перезапускается с восстановленным состоянием — панели остаются скрытыми.
 */
@Composable
fun PlayerSystemBarsFullscreen(isFullscreen: Boolean) {
    val view = LocalView.current
    DisposableEffect(isFullscreen) {
        val activity = view.context as? Activity
        if (activity == null) {
            onDispose {}
        } else {
            val controller = WindowInsetsControllerCompat(activity.window, view)
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                // Выход с экрана — вернуть панели (на случай, если ушли в полном экране).
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

/** Кнопка выхода из полноэкранного режима: полупрозрачный кружок поверх видео. */
@Composable
fun FullscreenExitButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.background(Color.Black.copy(alpha = 0.4f)),
    ) {
        Icon(
            imageVector = Icons.Filled.FullscreenExit,
            contentDescription = stringResource(R.string.player_exit_fullscreen),
        )
    }
}

/**
 * TODO(Этап 12): УДАЛИТЬ — автотест полноэкранного режима (MIUI блокирует
 * input-события, тапов нет): при файле-флаге debug_fullscreen сам включает
 * полноэкранный режим. Одноразовый: файл удаляется сразу после срабатывания,
 * поэтому забытый флаг не перехватывает следующий запуск плеера.
 * Маркер — FULLSCREEN_ON в logcat под тегом [DebugFlags.TAG].
 */
@Composable
fun FullscreenDebugAutoTest(context: Context, onEnterFullscreen: () -> Unit) {
    val flag = DebugFlags.fullscreenFlagFile(context)
    if (flag.exists()) {
        LaunchedEffect(Unit) {
            flag.delete()
            Log.d(DebugFlags.TAG, "FULLSCREEN_ON")
            onEnterFullscreen()
        }
    }
}
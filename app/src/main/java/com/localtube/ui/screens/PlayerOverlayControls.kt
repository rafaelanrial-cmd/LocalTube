package com.localtube.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localtube.R

/**
 * Кнопки поверх видео в правом нижнем углу (баг-фикс 2026-10-08):
 * шестерёнка аудиодорожек левее кнопки «На весь экран». Показываются, пока
 * виден интерфейс плеера (синхронно с контроллерами PlayerView).
 */
@Composable
fun VideoOverlayControls(
    onAudioTracksClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    showAudioTracks: Boolean,
    showFullscreenButton: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        // Шестерёнка — открывает диалог выбора аудиодорожки (Этап 9).
        if (showAudioTracks) {
            IconButton(onClick = onAudioTracksClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.player_audio_tracks),
                )
            }
        }
        // «На весь экран»: в полном режиме скрыта (выход — кнопка справа сверху или «назад»).
        if (showFullscreenButton) {
            IconButton(onClick = onFullscreenClick) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = stringResource(R.string.player_fullscreen),
                )
            }
        }
    }
}

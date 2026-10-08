package com.localtube.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.TrackGroup
import com.localtube.R
import java.util.Locale

/**
 * Диалог выбора аудиодорожки (Этап 9): список AUDIO-треков из player.currentTracks.
 * Выбор применяет TrackSelectionOverride — воспроизведение продолжается без перезапуска.
 */
@Composable
fun AudioTrackSelectionDialog(
    groups: List<TrackGroup>,
    current: Pair<TrackGroup, Int>?,
    onSelect: (group: TrackGroup, index: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.player_audio_tracks)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                var position = 0
                groups.forEach { group ->
                    for (i in 0 until group.length) {
                        val format = group.getFormat(i)
                        val isCurrent =
                            current != null && current.first == group && current.second == i
                        Row(modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = isCurrent,
                                onClick = {
                                    onSelect(group, i)
                                    onDismiss()
                                },
                            )
                            Text(text = audioTrackLabel(format.language, position))
                        }
                        position++
                    }
                }
            }
        },
        confirmButton = {},
    )
}

/** Подпись дорожки: название языка на русском ("русский"); иначе «Дорожка N». */
@Composable
private fun audioTrackLabel(language: String?, position: Int): String {
    val code = language?.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
        ?: return stringResource(R.string.audio_track_fallback, position + 1)
    // android.icu.util.DisplayNames не резолвится на compile-класспасе — используем Locale.
    return runCatching { Locale(code).getDisplayLanguage(Locale("ru")) }.getOrDefault(code)
}

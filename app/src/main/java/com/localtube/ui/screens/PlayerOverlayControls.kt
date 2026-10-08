package com.localtube.ui.screens

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.ui.PlayerView
import com.localtube.R

// Серый (баг-фикс 2026-10-08): иконки не сливаются ни с тёмным, ни с светлым видео.
private val OverlayIconTint = Color(0xFF9E9E9E)

/**
 * Кнопки поверх видео в правом нижнем углу (баг-фикс 2026-10-08):
 * шестерёнка аудиодорожек левее с отступом, кнопка «развернуть/свернуть» —
 * в самом углу. Показываются, пока виден интерфейс плеера (синхронно с
 * контроллерами PlayerView).
 */
@Composable
fun VideoOverlayControls(
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Шестерёнка — старое меню media3 (скорость воспроизведения + аудиодорожки).
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.player_settings),
                tint = OverlayIconTint,
            )
        }
        // Кнопка всегда в углу: «развернуть» или «свернуть» (выход из полного режима).
        IconButton(onClick = onToggleFullscreen) {
            Icon(
                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = stringResource(
                    if (isFullscreen) R.string.player_exit_fullscreen else R.string.player_fullscreen,
                ),
                tint = OverlayIconTint,
            )
        }
    }
}

/**
 * Прячет встроенные кнопки PlayerView в правом нижнем углу (баг-фикс 2026-10-08):
 * шестерёнку настроек и fullscreen-кнопки. Без этого они накладываются с нашими
 * кнопками, а встроенная fullscreen перехватывает тап и включает собственный
 * полноэкранный режим media3. Нулевой размер + INVISIBLE: кнопки остаются в
 * дереве (шестерёнка — якорь для старого меню настроек), но не рисуются и не
 * сдвигают раскладку.
 */
fun hideBuiltInCornerButtons(view: PlayerView) {
    val ctx = view.context
    for (name in listOf("exo_settings", "exo_fullscreen", "exo_minimal_fullscreen")) {
        val id = ctx.resources.getIdentifier(name, "id", ctx.packageName)
        if (id == 0) continue
        view.findViewById<View>(id)?.let { btn ->
            btn.layoutParams = btn.layoutParams.also { it.width = 0; it.height = 0 }
            btn.visibility = View.INVISIBLE
        }
    }
}

/**
 * Открывает старое меню настроек media3 (скорость воспроизведения + аудиодорожки):
 * программный клик по скрытой встроенной шестерёнке. false — если кнопка не найдена.
 */
fun openBuiltInSettings(view: PlayerView): Boolean {
    val id = view.context.resources.getIdentifier("exo_settings", "id", view.context.packageName)
    if (id == 0) return false
    val btn = view.findViewById<View>(id) ?: return false
    btn.performClick()
    return true
}

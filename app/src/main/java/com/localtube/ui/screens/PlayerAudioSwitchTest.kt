package com.localtube.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.C
import androidx.media3.common.TrackGroup
import androidx.media3.exoplayer.ExoPlayer
import com.localtube.DebugFlags
import com.localtube.ui.components.selectAudioTrack
import kotlinx.coroutines.delay

/**
 * TODO(Этап 12): УДАЛИТЬ — автотест Этапа 9 (MIUI блокирует input-события,
 * тапов нет): лог AUDIO-дорожек из player.currentTracks, переключение на RU
 * и обратно через TrackSelectionOverride без перезапуска воспроизведения.
 * Маркеры — в logcat под тегом [DebugFlags.TAG]: AUDIO_TRACKS, SWITCH_TO_RU,
 * RU_PLAYING (позиция пошла вперёд ~на 5 с — доказательство смены на лету),
 * SWITCH_BACK. Запускается только при файле-флаге debug_audio_switch.
 */
@Composable
fun ExoPlayer.audioSwitchAutoTest(audioGroups: List<TrackGroup>) {
    // Внутри LaunchedEffect `this` — CoroutineScope, поэтому игрока захватываем заранее.
    val player = this
    val groupsRef = rememberUpdatedState(audioGroups)
    LaunchedEffect(player) {
        var waited = 0L
        while (groupsRef.value.isEmpty() && waited < 20_000) {
            delay(500)
            waited += 500
        }
        val groups = groupsRef.value
        // TrackGroup не имеет .language — язык берём из Format каждого трека группы.
        val langs = groups.joinToString(";") { g ->
            (0 until g.length).joinToString(",") { i -> g.getFormat(i).language ?: "und" }
        }
        Log.d(DebugFlags.TAG, "AUDIO_TRACKS count=${groups.size} langs=$langs")
        if (groups.size < 2) {
            Log.d(DebugFlags.TAG, "AUDIO_SWITCH_SKIP: audio tracks fewer than 2")
            return@LaunchedEffect
        }
        // Media3 1.4.1: публичного player.audioTrackSelection нет — текущая дорожка
        // читается из currentTracks (AUDIO + isSelected + isTrackSelected).
        var original: Pair<TrackGroup, Int>? = null
        for (t in player.currentTracks.groups) {
            if (t.type != C.TRACK_TYPE_AUDIO || !t.isSelected()) continue
            for (i in 0 until t.length) {
                if (t.isTrackSelected(i)) {
                    original = t.mediaTrackGroup to i
                    break
                }
            }
            if (original != null) break
        }
        var ru: Pair<TrackGroup, Int>? = null
        outer@ for (g in groups) {
            for (i in 0 until g.length) {
                val lang = g.getFormat(i).language
                if (!lang.isNullOrBlank() && lang.lowercase().startsWith("ru")) {
                    ru = g to i
                    break@outer
                }
            }
        }
        if (ru == null) {
            Log.d(DebugFlags.TAG, "AUDIO_SWITCH_SKIP: no RU track")
            return@LaunchedEffect
        }
        val posBeforeSwitch = currentPosition
        selectAudioTrack(player, ru.first, ru.second)
        Log.d(
            DebugFlags.TAG,
            "SWITCH_TO_RU pos=${currentPosition}ms playing=${isPlaying}",
        )
        delay(5_000)
        // Если переключение на лету (без рестарта), позиция пошла вперёд ~на 5 с.
        Log.d(
            DebugFlags.TAG,
            "RU_PLAYING pos=${currentPosition}ms delta=${currentPosition - posBeforeSwitch}ms",
        )
        if (original != null) {
            selectAudioTrack(player, original.first, original.second)
            delay(2_000)
            Log.d(
                DebugFlags.TAG,
                "SWITCH_BACK pos=${currentPosition}ms playing=${isPlaying}",
            )
        }
    }
}

/**
 * TODO(Этап 12): УДАЛИТЬ — автотест Этапа 4 без тапов (MIUI блокирует input-события):
 * play → пауза через 4 с → seekTo(20 с); маркеры PAUSE/SEEK_20S в logcat.
 * Не запускается при флаге переключения дорожек (Этап 9): пауза/перемотка
 * исказили бы проверку «позиция пошла вперёд» после смены дорожки.
 * Вызывается из PlayerScreen (перенос, чтобы файл экрана не превышал 300 строк).
 */
@Composable
fun PlayerDebugAutoTest(player: ExoPlayer, audioGroups: List<TrackGroup>, context: Context) {
    val audioSwitchFlag = DebugFlags.audioSwitchFlagFile(context)
    if (audioSwitchFlag.exists()) {
        player.audioSwitchAutoTest(audioGroups)
    }
    if (DebugFlags.flagFile(context).exists() && !audioSwitchFlag.exists()) {
        LaunchedEffect(player) {
            delay(4_000)
            player.pause()
            Log.d(DebugFlags.TAG, "PAUSE pos=${player.currentPosition}ms")
            delay(4_000)
            player.seekTo(20_000)
            player.play()
            Log.d(DebugFlags.TAG, "SEEK_20S pos=${player.currentPosition}ms")
        }
    }
}

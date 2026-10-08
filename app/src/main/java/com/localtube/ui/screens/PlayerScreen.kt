package com.localtube.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.localtube.DebugFlags
import com.localtube.R
import com.localtube.ui.components.AudioTrackSelectionDialog
import kotlinx.coroutines.delay
import java.io.File

/**
 * Экран «Плеер» (Этап 4): Media3 ExoPlayer + PlayerView с контроллерами.
 * Этап 9: выбор аудиодорожки — кнопка в TopAppBar открывает диалог AUDIO-треков
 * из player.currentTracks; переключение идёт через TrackSelectionOverride,
 * то есть на лету, без перезапуска воспроизведения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    id: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory(id)),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Этап 9: игрок живёт на уровне экрана (а не внутри ExoPlayerView) — так
    // TopAppBar может показать кнопку «дорожки» и открыть диалог. Пока нет
    // Ready — null; когда файл появился, remember() создаёт нового игрока.
    val readyFile = (uiState as? PlayerUiState.Ready)?.file
    val player = remember(readyFile) {
        readyFile?.let { ExoPlayer.Builder(context).build() }
    }

    // AUDIO-группы и выбранная дорожка из currentTracks; обновляются в onTracksChanged.
    var audioGroups by remember(player) { mutableStateOf<List<TrackGroup>>(emptyList()) }
    var currentAudio by remember(player) { mutableStateOf<Pair<TrackGroup, Int>?>(null) }

    DisposableEffect(player) {
        // Media3 1.4.1: Tracks не Iterable — группы читаем через tracks.groups;
        // lambda эффекта обязана вернуть DisposableEffectResult, поэтому ветка
        // null закрывается пустым onDispose вместо return Unit.
        val p = player
        if (p == null || readyFile == null) {
            onDispose {}
        } else {
            val file = readyFile!!
        val tracksListener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                // «Выбрано» читается из группы: isSelected()/isTrackSelected(i).
                val audio = ArrayList<TrackGroup>()
                var selected: TrackGroup? = null
                var selectedIndex = -1
                for (t in tracks.groups) {
                    if (t.type != C.TRACK_TYPE_AUDIO) continue
                    val g = t.mediaTrackGroup
                    audio.add(g)
                    if (t.isSelected()) {
                        for (i in 0 until g.length) {
                            if (t.isTrackSelected(i)) {
                                selected = g
                                selectedIndex = i
                                break
                            }
                        }
                    }
                }
                audioGroups = audio
                currentAudio = if (selected != null && selectedIndex >= 0) {
                    selected to selectedIndex
                } else {
                    null
                }
            }
        }
            p.addListener(tracksListener)
            p.setMediaItem(MediaItem.Builder().setUri(file.toUri()).build())
            p.prepare()
            p.play()
            onDispose {
                p.removeListener(tracksListener)
                p.release()
            }
        }
    }

    // Пауза при уходе с экрана в фон (activity ON_STOP), возобновление — при возврате.
    DisposableEffect(lifecycleOwner, player) {
        val p = player
        if (p == null) {
            onDispose {}
        } else {
            var shouldResume = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (p.isPlaying) {
                    p.pause()
                    shouldResume = true
                    // TODO(Этап 12): УДАЛИТЬ — маркер для проверки паузы при уходе с экрана.
                    if (DebugFlags.flagFile(context).exists()) {
                        Log.d(DebugFlags.TAG, "ON_STOP_PAUSE pos=${p.currentPosition}ms")
                    }
                }

                Lifecycle.Event.ON_START -> if (shouldResume) {
                    p.play()
                    shouldResume = false
                    // TODO(Этап 12): УДАЛИТЬ — маркер для проверки возобновления.
                    if (DebugFlags.flagFile(context).exists()) {
                        Log.d(DebugFlags.TAG, "ON_START_RESUME pos=${p.currentPosition}ms")
                    }
                }

                else -> Unit
            }
        }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    // Этап 9: диалог выбора аудиодорожки.
    var showTrackDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (uiState as? PlayerUiState.Ready)?.video?.title
                            ?: stringResource(R.string.title_player),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    // Этап 9: кнопка видна, когда дорожки уже распознаны.
                    if (player != null && audioGroups.isNotEmpty()) {
                        IconButton(onClick = { showTrackDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = stringResource(R.string.player_audio_tracks),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            PlayerUiState.Loading -> Unit // мгновение до готовности записи из Room

            PlayerUiState.FileNotFound -> FileNotFoundContent(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            is PlayerUiState.Ready -> ExoPlayerView(
                player = player!!,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    }

    if (showTrackDialog && player != null) {
        AudioTrackSelectionDialog(
            groups = audioGroups,
            current = currentAudio,
            onSelect = { group, index -> selectAudioTrack(player, group, index) },
            onDismiss = { showTrackDialog = false },
        )
    }

    // TODO(Этап 12): УДАЛИТЬ — автотест без тапов (MIUI блокирует input-события).
    // Этап 4: play → пауза через 4 с → seekTo(20 с); маркеры — в logcat.
    // Не запускается при флаге переключения дорожек (Этап 9): пауза/перемотка
    // исказили бы проверку «позиция пошла вперёд» после смены дорожки.
    val audioSwitchFlag = DebugFlags.audioSwitchFlagFile(context)
    if (player != null && audioSwitchFlag.exists()) {
        player.audioSwitchAutoTest(audioGroups)
    }
    if (player != null && DebugFlags.flagFile(context).exists() && !audioSwitchFlag.exists()) {
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

/** Этап 9: смена аудиодорожки через TrackSelectionOverride — на лету, без рестарта. */
internal fun selectAudioTrack(player: ExoPlayer, group: TrackGroup, index: Int) {
    // Media3 1.4.1: Builder.addOverride принимает TrackSelectionOverride(group, index),
    // старые аудио-оверраиды сначала снимаются (иначе новый не применится).
    val params = player.trackSelectionParameters
        .buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .addOverride(TrackSelectionOverride(group, index))
        .build()
    player.trackSelectionParameters = params
    Log.d(
        DebugFlags.TAG,
        "AUDIO_SELECT lang=${group.getFormat(index).language} pos=${player.currentPosition}ms",
    )
}

/** Экран «Файл не найден»: записи в базе нет или файла на диске больше нет. */
@Composable
private fun FileNotFoundContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(R.string.player_file_not_found),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** PlayerView для готового игрока (Этап 9: игрок создаётся в PlayerScreen). */
@Composable
private fun ExoPlayerView(player: ExoPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                setPlayer(player)
                showController()
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.setPlayer(player)
            }
        },
        modifier = modifier,
    )
}

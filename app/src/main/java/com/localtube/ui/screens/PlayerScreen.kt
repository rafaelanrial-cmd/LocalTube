package com.localtube.ui.screens

import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.localtube.DebugFlags
import com.localtube.R
import com.localtube.ui.components.AudioTrackSelectionDialog
import com.localtube.ui.components.selectAudioTrack
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
    // Ключ по id видео: при смене видео не переиспользуется чужой игрок.
    viewModel: PlayerViewModel = viewModel(key = "player-$id", factory = PlayerViewModel.factory(id)),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Память позиции (баг-фикс 2026-10-08): игрок живёт в ViewModel — переживает
    // поворот экрана, поэтому смена ориентации не сбрасывает воспроизведение.
    val player = viewModel.player

    // AUDIO-группы и выбранная дорожка из currentTracks; обновляются в onTracksChanged.
    var audioGroups by remember(player) { mutableStateOf<List<TrackGroup>>(emptyList()) }
    var currentAudio by remember(player) { mutableStateOf<Pair<TrackGroup, Int>?>(null) }

    // Этап 9: AUDIO-группы из currentTracks. Игрок создаётся и настраивается
    // в ViewModel (setMediaItem/prepare/play); здесь только слушатель для диалога.
    DisposableEffect(player) {
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
        player.addListener(tracksListener)
        onDispose { player.removeListener(tracksListener) }
    }

    // Пауза при уходе в фон/повороте (activity ON_STOP), возобновление — при возврате.
    // shouldResume живёт в ViewModel: переживает поворот экрана, поэтому после
    // смены ориентации воспроизведение продолжается, а не остаётся на паузе.
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (player.isPlaying) {
                    player.pause()
                    viewModel.shouldResume = true
                    // TODO(Этап 12): УДАЛИТЬ — маркер для проверки паузы при уходе с экрана.
                    if (DebugFlags.flagFile(context).exists()) {
                        Log.d(DebugFlags.TAG, "ON_STOP_PAUSE pos=${player.currentPosition}ms")
                    }
                }

                Lifecycle.Event.ON_START -> if (viewModel.shouldResume) {
                    player.play()
                    viewModel.shouldResume = false
                    // TODO(Этап 12): УДАЛИТЬ — маркер для проверки возобновления.
                    if (DebugFlags.flagFile(context).exists()) {
                        Log.d(DebugFlags.TAG, "ON_START_RESUME pos=${player.currentPosition}ms")
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Память позиции (баг-фикс 2026-10-08): периодическое сохранение + ON_STOP.
    PlaybackPositionMemory(player, viewModel, lifecycleOwner)

    // Выход с экрана (назад): пауза; release — в ViewModel.onCleared.
    DisposableEffect(Unit) {
        onDispose { if (player.isPlaying) player.pause() }
    }

    // Этап 9: диалог выбора аудиодорожки.
    var showTrackDialog by remember { mutableStateOf(false) }

    // Полноэкранный режим (2026-10-08): rememberSaveable — при повороте Activity
    // пересоздаётся, обычный remember() потерял бы полноэкранный режим.
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    // Баг-фикс 2026-10-08: кнопки поверх видео прячутся вместе с интерфейсом плеера (тап/авто-скрытие).
    var showController by remember { mutableStateOf(true) }

    // В полном экране «назад» (кнопка или жест) сначала выходит из него, а не с экрана.
    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    // Полноэкранный режим: скрытие/показ системных панелей (статус-бар + навигация).
    PlayerSystemBarsFullscreen(isFullscreen)

    // TODO(Этап 12): УДАЛИТЬ — автотест полноэкранного режима (флаг debug_fullscreen,
    // одноразовый: приложение само удаляет файл после срабатывания).
    FullscreenDebugAutoTest(context) { isFullscreen = true }

    Scaffold(
        modifier = modifier,
        topBar = {
            // Полноэкранный режим (2026-10-08): шапка приложения скрыта — видео на весь экран.
            if (!isFullscreen) {
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
                )
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            PlayerUiState.Loading -> Unit // мгновение до готовности записи из Room

            PlayerUiState.FileNotFound -> FileNotFoundContent(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            is PlayerUiState.Ready -> Box(
                // Полноэкранный режим (2026-10-08): без отступов — видео на весь экран.
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.fillMaxSize().padding(innerPadding)
                },
            ) {
                ExoPlayerView(
                    player = player,
                    modifier = Modifier.fillMaxSize(),
                    onControllerVisibilityChange = { showController = it },
                )
                // Баг-фикс 2026-10-08: кнопки в правом нижнем углу, прячутся вместе с интерфейсом.
                if (showController) {
                    VideoOverlayControls(
                        onAudioTracksClick = { showTrackDialog = true },
                        onFullscreenClick = { isFullscreen = true },
                        showAudioTracks = audioGroups.isNotEmpty(),
                        showFullscreenButton = !isFullscreen,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    )
                    // Полноэкранный режим: кнопка выхода справа сверху.
                    if (isFullscreen) {
                        FullscreenExitButton(
                            onClick = { isFullscreen = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        )
                    }
                }
            }
        }
    }

    if (showTrackDialog) {
        AudioTrackSelectionDialog(
            groups = audioGroups,
            current = currentAudio,
            onSelect = { group, index -> selectAudioTrack(player, group, index) },
            onDismiss = { showTrackDialog = false },
        )
    }

    // TODO(Этап 12): УДАЛИТЬ — автотест без тапов (MIUI блокирует input-события);
    // реализация в PlayerAudioSwitchTest.kt (перенос, чтобы файл не превышал 300 строк).
    PlayerDebugAutoTest(player, audioGroups, context)

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

/** PlayerView для готового игрока; сообщает видимость интерфейса (баг-фикс 2026-10-08). */
@Composable
private fun ExoPlayerView(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    onControllerVisibilityChange: (Boolean) -> Unit,
) {
    val latestCallback = rememberUpdatedState(onControllerVisibilityChange)
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                setPlayer(player)
                showController()
                // Явный интерфейс: у метода два SAM-оверлода, лямбда была бы неоднозначна.
                setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v ->
                    latestCallback.value(v == View.VISIBLE)
                })
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

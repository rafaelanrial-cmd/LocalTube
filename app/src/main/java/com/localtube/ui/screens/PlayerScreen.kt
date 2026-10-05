package com.localtube.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.localtube.DebugFlags
import com.localtube.R
import kotlinx.coroutines.delay
import java.io.File

/** Экран «Плеер» (Этап 4): Media3 ExoPlayer + PlayerView с контроллерами. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    id: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory(id)),
) {
    val uiState by viewModel.uiState.collectAsState()

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
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            PlayerUiState.Loading -> Unit // мгновение до готовности записи из Room

            PlayerUiState.FileNotFound -> FileNotFoundContent(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            is PlayerUiState.Ready -> ExoPlayerView(
                file = state.file,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    }
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

/** ExoPlayer + PlayerView: создание, media item, воспроизведение и lifecycle. */
@Composable
private fun ExoPlayerView(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Игрок живёт, пока экран в композиции; при уходе с экрана — release.
    val player = remember(file) { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        player.setMediaItem(MediaItem.Builder().setUri(file.toUri()).build())
        player.prepare()
        player.play()
        onDispose { player.release() }
    }

    // Пауза при уходе с экрана в фон (activity ON_STOP), возобновление — при возврате.
    DisposableEffect(lifecycleOwner, player) {
        var shouldResume = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (player.isPlaying) {
                    player.pause()
                    shouldResume = true
                    // TODO(Этап 12): УДАЛИТЬ — маркер для проверки паузы при уходе с экрана.
                    if (DebugFlags.flagFile(context).exists()) {
                        Log.d(DebugFlags.TAG, "ON_STOP_PAUSE pos=${player.currentPosition}ms")
                    }
                }

                Lifecycle.Event.ON_START -> if (shouldResume) {
                    player.play()
                    shouldResume = false
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

    // TODO(Этап 12): УДАЛИТЬ — автотест без тапов (MIUI блокирует input-события):
    // play → пауза через 4 с → seekTo(20 с) и play ещё через 4 с; маркеры — в logcat.
    if (DebugFlags.flagFile(context).exists()) {
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

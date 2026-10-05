package com.localtube.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.localtube.R
import com.localtube.ui.components.StubScreen

/** Экран «Плеер» (Этап 1 — заглушка). */
@Composable
fun PlayerScreen(id: Long, modifier: Modifier = Modifier) {
    StubScreen(
        title = stringResource(R.string.title_player),
        message = stringResource(R.string.player_stub, id),
        modifier = modifier,
    )
}
